// ----------------------------------------------------------------------------
// Copyright 2007-2011, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// TK102/TK103 device communication server:
//  -Includes support to overriding default status code
//  -Includes support for simulated geozone arrive/depart events
// ----------------------------------------------------------------------------
// Change History:
//  2011/07/15  Martin D. Flynn
//     -Initial release.
//  2011/08/21  Martin D. Flynn
//     -TK103: The Date column appears to be specified in a timezone local to
//      where the device was configured.  Made some changes to attempt to 
//      determine the actual GMT day, based on the time difference between the 
//      local time, and the GMT time specified elsewhere in the record.
//     -TK103: It appears that most tk103 data packets do not contain a heading
//      value.  If no heading value is present, then an approximate heading will
//      be calcualted based on the previous valid location.
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.servers.tk10x;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    public static       boolean DEBUG_MODE                  = false;
    
    // ------------------------------------------------------------------------

    public static       String  UNIQUEID_PREFIX[]           = null;
    public static       double  MINIMUM_SPEED_KPH           = Constants.MINIMUM_SPEED_KPH;
    public static       boolean ESTIMATE_ODOMETER           = true;
    public static       boolean SIMEVENT_GEOZONES           = false;
    public static       boolean XLATE_LOCATON_INMOTION      = true;
    public static       double  MINIMUM_MOVED_METERS        = 0.0;
    public static       boolean PACKET_LEN_END_OF_STREAM    = false;

    // ------------------------------------------------------------------------

    /* convenience for converting knots to kilometers */
    public static final double  KILOMETERS_PER_KNOT         = 1.85200000;

    // ------------------------------------------------------------------------

    /* GMT/UTC timezone */
    private static final TimeZone gmtTimezone               = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------

    /* packet handler constructor */
    public TrackClientPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);
        super.clearTerminateSession();
    }
    
    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);
    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        if (PACKET_LEN_END_OF_STREAM) {
            return ServerSocketThread.PACKET_LEN_END_OF_STREAM;
        } else {
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
        }
    }

    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if (ListTools.isEmpty(pktBytes)) {
            Print.logWarn("Ignoring empty/null packet");
        } else
        if (pktBytes.length < 11) {
            Print.logError("Unexpected packet length: " + pktBytes.length);
        } else {
            Print.logInfo("Receive: " + StringTools.toStringValue(pktBytes,'.')); // debug message
            String s = StringTools.toStringValue(pktBytes).trim();
            if (s.startsWith("##")) {
                // Likely a TK103 derivitive:
                //   ##,imei:123451042191239,A;
                Print.logInfo("TK103 Header: " + s); // debug message
                //Print.logInfo("Ignoring superfluous initial IMEI TK103 packet ...");
                return null;
            } else
            if (s.startsWith("imei:")) {
                // Could be a TK103 record
                //   imei:123451042191239,tracker ,1107090553,9735551234,F,215314.000,A,4103.7641,N,14244.9450,W,0.08,;
                this.parseInsertRecord_TK103(s);
            } else {
                // default to TK102
                this.parseInsertRecord_TK102(s);
            }
        }
        return null; // no return packets are expected
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* TK103: parse and insert data record */
    private boolean parseInsertRecord_TK103(String s)
    {
        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return false;
        }
        Print.logInfo("Parsing(TK103): " + s);

        /* parse to fields */
        String fld[] = StringTools.parseString(s, ',');
        if (fld == null) {
            // will not occur
            Print.logWarn("Fields are null");
            return false;
        } else
        if (fld.length < 12) {
            Print.logWarn("Invalid number of fields");
            return false;
        }

        /* get "imei:" */
        String mobileID = null;
        if (fld[0].startsWith("imei:")) {
            mobileID = fld[0].substring("imei:".length()).trim();
        }
        if (StringTools.isBlank(mobileID)) {
            Print.logError("'imei:' value is missing");
            return false;
        }

        /* get time */
        long    locYMDhm    = (fld[2].length() >= 10)? StringTools.parseLong(fld[2].substring(0,10),0L) : 0L;
        long    gmtHMS      = StringTools.parseLong(fld[5],0L);
        long    fixtime     = this._getUTCSeconds_YMDhms_HMS(locYMDhm*100L, gmtHMS);
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[2] + "/" + fld[5]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* GPS */
        boolean validGPS    = fld[6].equalsIgnoreCase("A");
        double  latitude    = validGPS? this._parseLatitude( fld[7], fld[ 8])  : 0.0;
        double  longitude   = validGPS? this._parseLongitude(fld[9], fld[10])  : 0.0;
        double  knots       = (validGPS && (fld.length > 11))? StringTools.parseDouble(fld[11], -1.0) : -1.0;
        double  headingDeg  = (validGPS && (fld.length > 12))? StringTools.parseDouble(fld[12], -1.0) : -1.0;
        double  speedKPH    = (knots >= 0.0)? (knots * KILOMETERS_PER_KNOT)   : -1.0;
        double  altitudeM   = 0.0;
        double  odomKM      = 0.0;

        /* get status code */
        String eventCode   = fld[1];
        int    statusCode  = StatusCodes.STATUS_LOCATION;
        DCServerConfig dcs = Main.getServerConfig();
        if ((dcs != null) && !StringTools.isBlank(eventCode)) {
            int code = dcs.translateStatusCode(eventCode, -9999);
            if (code == -9999) {
                // default 'statusCode' is StatusCodes.STATUS_LOCATION
            } else {
                statusCode = code;
            }
        }
        if (statusCode == StatusCodes.STATUS_IGNORE) {
            Print.logError("Ignoring EventCode: " + eventCode);
            return false;
        } else
        if (statusCode == StatusCodes.STATUS_NONE) {
            statusCode = (speedKPH > 0.0)? StatusCodes.STATUS_MOTION_IN_MOTION : StatusCodes.STATUS_LOCATION;
        } else
        if (XLATE_LOCATON_INMOTION && (statusCode == StatusCodes.STATUS_LOCATION) && (speedKPH > 0.0)) {
            statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
        }

        /* valid lat/lon? */
        if (validGPS && !GeoPoint.isValid(latitude,longitude)) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
            validGPS  = false;
        }
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        /* parsed data */
        Print.logInfo("IMEI     : " + mobileID);
        Print.logInfo("Timestamp: " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GPS      : " + geoPoint);

        /* find Device */
        Device device = DCServerFactory.loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, mobileID);
        if (device == null) {
            return false; // errors already displayed
        }
        String accountID = device.getAccountID();
        String deviceID  = device.getDeviceID();
        String uniqueID  = device.getUniqueID();
        Print.logInfo("UniqueID  : " + uniqueID);
        Print.logInfo("DeviceID  : " + accountID + "/" + deviceID);

        /* check IP address */
        DataTransport dataXPort = device.getDataTransport();
        if (this.hasIPAddress() && !dataXPort.isValidIPAddress(this.getIPAddress())) {
            DTIPAddrList validIPAddr = dataXPort.getIpAddressValid(); // may be null
            Print.logError("Invalid IP Address from device: " + this.getIPAddress() + " [expecting " + validIPAddr + "]");
            return false;
        }
        dataXPort.setIpAddressCurrent(this.getIPAddress());    // FLD_ipAddressCurrent
        dataXPort.setRemotePortCurrent(this.getRemotePort());  // FLD_remotePortCurrent
        dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!dataXPort.getDeviceCode().equalsIgnoreCase(Main.getServerName())) {
            dataXPort.setDeviceCode(Main.getServerName()); // FLD_deviceCode
        }

        /* adjust speed, calculate approximate heading if not available in packet */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
            if (validGPS && (device != null)) {
                GeoPoint lastGP = device.getLastValidLocation();
                if (GeoPoint.isValid(lastGP)) {
                    // calculate heading from last point to this point
                    headingDeg = lastGP.headingToPoint(geoPoint);
                }
            }
        }
        Print.logInfo("Speed    : " + StringTools.format(speedKPH,"0.0") + " km/h " + headingDeg);

        /* estimate GPS-based odometer */
        if (odomKM <= 0.0) {
            // calculate odometer
            odomKM = (ESTIMATE_ODOMETER && validGPS)? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            // bounds-check odometer
            odomKM = device.adjustOdometerKM(odomKM);
        }
        Print.logInfo("OdometerKM: " + odomKM);

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && validGPS) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    this.insertEventRecord(device, 
                        z.getTimestamp(), z.getStatusCode(), z.getGeozone(),
                        geoPoint, 0/*gpsAge*/, 0.0/*HDOP*/, 0/*numSats*/,
                        speedKPH, headingDeg, altitudeM, odomKM);
                    Print.logInfo("Geozone    : " + z);
                    if (z.getStatusCode() == statusCode) {
                        // suppress 'statusCode' event if we just added it here
                        Print.logDebug("StatusCode already inserted: 0x" + StatusCodes.GetHex(statusCode));
                        statusCode = StatusCodes.STATUS_IGNORE;
                    }
                }
            }
        }

        /* status code checks */
        if (statusCode < 0) { // StatusCodes.STATUS_IGNORE
            // skip (event ignored)
        } else
        if (statusCode == StatusCodes.STATUS_IGNORE) {
            // skip (event ignored)
        } else
        if ((statusCode == StatusCodes.STATUS_LOCATION) && this.hasSavedEvents()) {
            // skip (already inserted an event)
        } else
        if (statusCode != StatusCodes.STATUS_LOCATION) {
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*geozone*/,
                geoPoint, 0/*gpsAge*/, 0.0/*HDOP*/, 0/*numSats*/,
                speedKPH, headingDeg, altitudeM, odomKM);
        } else
        if (validGPS && !device.isNearLastValidLocation(geoPoint,MINIMUM_MOVED_METERS)) {
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*geozone*/,
                geoPoint, 0/*gpsAge*/, 0.0/*HDOP*/, 0/*numSats*/,
                speedKPH, headingDeg, altitudeM, odomKM);
        }

        /* save device changes */
        if (!DEBUG_MODE && (device != null)) {
            try {
                //DBConnection.pushShowExecutedSQL();
                device.updateChangedEventFields();
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
            } finally {
                //DBConnection.popShowExecutedSQL();
            }
        }

        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* TK102: parse and insert data record */
    private boolean parseInsertRecord_TK102(String s)
    {
        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return false;
        }
        Print.logInfo("Parsing(TK102): " + s);

        /* parse to fields */
        String fld[] = StringTools.parseString(s, ',');
        if (fld == null) {
            // will not occur
            Print.logWarn("Fields are null");
            return false;
        } else
        if (fld.length < 15) {
            Print.logWarn("Invalid number of fields");
            return false;
        }

        /* find "imei:" */
        String mobileID = null;
        int imeiNdx = -1;
        for (int f = 0; f < fld.length; f++) {
            if (fld[f].startsWith("imei:")) {
                mobileID = fld[f].substring("imei:".length()).trim();
                imeiNdx = f;
                break;
            }
        }
        if (StringTools.isBlank(mobileID)) {
            Print.logError("'imei:' value is missing");
            return false;
        }

        /* find "GPRMC" */
        int gpx = 0;
        for (; (gpx < fld.length) && !fld[gpx].equalsIgnoreCase("GPRMC"); gpx++);
        if (gpx >= fld.length) {
            Print.logError("'GPRMC' not found");
            return false;
        } else
        if ((gpx + 12) >= fld.length) {
            Print.logError("Insufficient 'GPRMC' fields");
            return false;
        }

        /* parse data following GPRMC */
        long    hms         = StringTools.parseLong(fld[gpx + 1], 0L);
        long    dmy         = StringTools.parseLong(fld[gpx + 9], 0L);
        long    fixtime     = this._getUTCSeconds_DMY_HMS(dmy, hms);
        boolean validGPS    = fld[gpx + 2].equalsIgnoreCase("A");
        double  latitude    = validGPS? this._parseLatitude( fld[gpx + 3], fld[gpx + 4])  : 0.0;
        double  longitude   = validGPS? this._parseLongitude(fld[gpx + 5], fld[gpx + 6])  : 0.0;
        double  knots       = validGPS? StringTools.parseDouble(fld[gpx + 7], -1.0) : 0.0;
        double  headingDeg  = validGPS? StringTools.parseDouble(fld[gpx + 8], -1.0) : 0.0;
        double  speedKPH    = (knots >= 0.0)? (knots * KILOMETERS_PER_KNOT)   : -1.0;
        double  altitudeM   = 0.0;
        double  odomKM      = 0.0;

        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[gpx + 9] + "/" + fld[gpx + 1]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* status code */
        String eventCode   = ((gpx + 14) < fld.length)? StringTools.trim(fld[gpx + 14]) : "";
        int    statusCode  = StatusCodes.STATUS_LOCATION;
        DCServerConfig dcs = Main.getServerConfig();
        if ((dcs != null) && !StringTools.isBlank(eventCode)) {
            int code = dcs.translateStatusCode(eventCode, -9999);
            if (code == -9999) {
                // default 'statusCode' is StatusCodes.STATUS_LOCATION
            } else {
                statusCode = code;
            }
        }
        if (statusCode == StatusCodes.STATUS_IGNORE) {
            Print.logError("Ignoring EventCode: " + eventCode);
            return false;
        } else
        if (statusCode == StatusCodes.STATUS_NONE) {
            statusCode = (speedKPH > 0.0)? StatusCodes.STATUS_MOTION_IN_MOTION : StatusCodes.STATUS_LOCATION;
        } else
        if (XLATE_LOCATON_INMOTION && (statusCode == StatusCodes.STATUS_LOCATION) && (speedKPH > 0.0)) {
            statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
        }

        /* valid lat/lon? */
        if (validGPS && !GeoPoint.isValid(latitude,longitude)) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
            validGPS  = false;
        }
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        /* adjustments to received values */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* parsed data */
        Print.logInfo("IMEI     : " + mobileID);
        Print.logInfo("Timestamp: " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GPS      : " + geoPoint);
        Print.logInfo("Speed    : " + StringTools.format(speedKPH,"#0.0") + " kph " + headingDeg);

        /* find Device */
        Device device = DCServerFactory.loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, mobileID);
        if (device == null) {
            return false; // errors already displayed
        }
        String accountID = device.getAccountID();
        String deviceID  = device.getDeviceID();
        String uniqueID  = device.getUniqueID();
        Print.logInfo("UniqueID  : " + uniqueID);
        Print.logInfo("DeviceID  : " + accountID + "/" + deviceID);

        /* check IP address */
        DataTransport dataXPort = device.getDataTransport();
        if (this.hasIPAddress() && !dataXPort.isValidIPAddress(this.getIPAddress())) {
            DTIPAddrList validIPAddr = dataXPort.getIpAddressValid(); // may be null
            Print.logError("Invalid IP Address from device: " + this.getIPAddress() + " [expecting " + validIPAddr + "]");
            return false;
        }
        dataXPort.setIpAddressCurrent(this.getIPAddress());    // FLD_ipAddressCurrent
        dataXPort.setRemotePortCurrent(this.getRemotePort());  // FLD_remotePortCurrent
        dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!dataXPort.getDeviceCode().equalsIgnoreCase(Main.getServerName())) {
            dataXPort.setDeviceCode(Main.getServerName()); // FLD_deviceCode
        }

        /* estimate GPS-based odometer */
        if (odomKM <= 0.0) {
            // calculate odometer
            odomKM = (ESTIMATE_ODOMETER && validGPS)? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            // bounds-check odometer
            odomKM = device.adjustOdometerKM(odomKM);
        }
        Print.logInfo("OdometerKM: " + odomKM);

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && validGPS) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    this.insertEventRecord(device, 
                        z.getTimestamp(), z.getStatusCode(), z.getGeozone(),
                        geoPoint, 0/*gpsAge*/, 0.0/*HDOP*/, 0/*numSats*/,
                        speedKPH, headingDeg, altitudeM, odomKM);
                    Print.logInfo("Geozone    : " + z);
                    if (z.getStatusCode() == statusCode) {
                        // suppress 'statusCode' event if we just added it here
                        Print.logDebug("StatusCode already inserted: 0x" + StatusCodes.GetHex(statusCode));
                        statusCode = StatusCodes.STATUS_IGNORE;
                    }
                }
            }
        }

        /* status code checks */
        if (statusCode < 0) { // StatusCodes.STATUS_IGNORE
            // skip (event ignored)
        } else
        if (statusCode == StatusCodes.STATUS_IGNORE) {
            // skip (event ignored)
        } else
        if ((statusCode == StatusCodes.STATUS_LOCATION) && this.hasSavedEvents()) {
            // skip (already inserted an event)
        } else
        if (statusCode != StatusCodes.STATUS_LOCATION) {
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*geozone*/,
                geoPoint, 0/*gpsAge*/, 0.0/*HDOP*/, 0/*numSats*/,
                speedKPH, headingDeg, altitudeM, odomKM);
        } else
        if (validGPS && !device.isNearLastValidLocation(geoPoint,MINIMUM_MOVED_METERS)) {
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*geozone*/,
                geoPoint, 0/*gpsAge*/, 0.0/*HDOP*/, 0/*numSats*/,
                speedKPH, headingDeg, altitudeM, odomKM);
        }

        /* save device changes */
        try {
            //DBConnection.pushShowExecutedSQL();
            device.updateChangedEventFields();
        } catch (DBException dbe) {
            Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
        } finally {
            //DBConnection.popShowExecutedSQL();
        }

        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy    Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***               YY is year.
    *** @param hms    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***               and SS is second.
    *** @return Time in UTC seconds.
    ***/
    private long _getUTCSeconds_DMY_HMS(long dmy, long hms)
    {
    
        /* time of day [TOD] */
        int    HH  = (int)((hms / 10000L) % 100L);
        int    MM  = (int)((hms / 100L) % 100L);
        int    SS  = (int)(hms % 100L);
        long   TOD = (HH * 3600L) + (MM * 60L) + SS;
    
        /* current UTC day */
        long DAY;
        if (dmy > 0L) {
            int    yy  = (int)(dmy % 100L) + 2000;
            int    mm  = (int)((dmy / 100L) % 100L);
            int    dd  = (int)((dmy / 10000L) % 100L);
            long   yr  = ((long)yy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY        = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                         + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                         + (long)dd - 719469L;
        } else {
            // we don't have the day, so we need to figure out as close as we can what it should be.
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= TOD)? (tod - TOD) : (TOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (tod > TOD) {
                    // tod > TOD likely represents the next day
                    DAY++;
                } else {
                    // tod < TOD likely represents the previous day
                    DAY--;
                }
            }
        }
        
        /* return UTC seconds */
        long sec = DateTime.DaySeconds(DAY) + TOD;
        return sec;
        
    }

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param locYMDhms Date received from packet in YYMMDDhhmmss format, where DD is day, MM is month,
    ***                  YY is year, hh is the hour, mm is the minutes, and ss is the seconds.  
    ***                  Unfortunately, the device is allowed to be configured to specify this time 
    ***                  relative to the local timezone, rather than GMT.  This makes determining the
    ***                  actual time of the event more difficult.
    *** @param gmtHMS    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***                  and SS is second.  This time is assumed to be relative to GMT.
    *** @return Time in UTC seconds.
    ***/
    private long _getUTCSeconds_YMDhms_HMS(long locYMDhms, long gmtHMS)
    {

        /* GMT time of day */
        int    gmtHH  = (int)((gmtHMS / 10000L) % 100L);
        int    gmtMM  = (int)((gmtHMS /   100L) % 100L);
        int    gmtSS  = (int)((gmtHMS         ) % 100L);
        long   gmtTOD = (gmtHH * 3600L) + (gmtMM * 60L) + gmtSS; // seconds of day
        //Print.logInfo("GMT HHMMSS: " + gmtHMS);
        //Print.logInfo("GMT HH="+gmtHH + " MM="+gmtMM + " SS="+gmtSS +" TOD="+gmtTOD);

        /* local time of day */
        int    locHH  = (int)((locYMDhms / 10000L) % 100L);
        int    locMM  = (int)((locYMDhms /   100L) % 100L);
        int    locSS  = (int)((locYMDhms         ) % 100L);
        long   locTOD = (locHH * 3600L) + (locMM * 60L) + locSS; // seconds of day
        //Print.logInfo("Loc HHMMSS: " + locYMDhms);
        //Print.logInfo("Loc HH="+locHH + " MM="+locMM + " SS="+locSS +" TOD="+locTOD);

        /* current day */
        long ymd = locYMDhms / 1000000L; // remove hhmmss
        long DAY;
        if (ymd > 0L) {
            int    dd = (int)( ymd           % 100L);
            int    mm = (int)((ymd /   100L) % 100L);
            int    yy = (int)((ymd / 10000L) % 100L) + 2000;
            long   yr = ((long)yy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY       = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                         + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                         + (long)dd - 719469L;
            long  dif = (locTOD >= gmtTOD)? (locTOD - gmtTOD) : (gmtTOD - locTOD); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (locTOD > gmtTOD) {
                    // locTOD > gmtTOD likely represents the next day
                    // ie 2011/07/10 23:00 ==> 01:00 (2011/07/11)
                    DAY++;
                } else {
                    // locTOD < gmtTOD likely represents the previous day
                    // ie 2011/07/10 01:00 ==> 23:00 (2011/07/09)
                    DAY--;
                }
            }
        } else {
            // we don't have the day, so we need to figure out as close as we can what it should be.
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= gmtTOD)? (tod - gmtTOD) : (gmtTOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (tod > gmtTOD) {
                    // tod > TOD likely represents the next day
                    DAY++;
                } else {
                    // tod < TOD likely represents the previous day
                    DAY--;
                }
            }
        }

        /* return UTC seconds */
        long sec = DateTime.DaySeconds(DAY) + gmtTOD;
        return sec;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parses latitude given values from GPS device.
    *** @param  s  Latitude String from GPS device in ddmm.mm format.
    *** @param  d  Latitude hemisphere, "N" for northern, "S" for southern.
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         90.0 if invalid latitude provided.
    **/
    private double _parseLatitude(String s, String d)
    {
        double _lat = StringTools.parseDouble(s, 99999.0);
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
            return d.equals("S")? -lat : lat;
        } else {
            return 90.0; // invalid latitude
        }
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param s Longitude String from GPS device in ddmm.mm format.
    *** @param d Longitude hemisphere, "E" for eastern, "W" for western.
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    *** 180.0 if invalid longitude provided.
    **/
    private double _parseLongitude(String s, String d)
    {
        double _lon = StringTools.parseDouble(s, 99999.0);
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            return d.equals("W")? -lon : lon;
        } else {
            return 180.0; // invalid longitude
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private EventData createEventRecord(Device device, 
        long     gpsTime, int statusCode, Geozone geozone,
        GeoPoint geoPoint, long gpsAge, double HDOP, int numSats,
        double   speedKPH, double heading, double altitude, double odomKM)
    {
        String accountID    = device.getAccountID();
        String deviceID     = device.getDeviceID();
        EventData.Key evKey = new EventData.Key(accountID, deviceID, gpsTime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeozone(geozone);
        evdb.setGeoPoint(geoPoint);
        evdb.setGpsAge(gpsAge);
        evdb.setHDOP(HDOP);
        evdb.setSatelliteCount(numSats);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(heading);
        evdb.setAltitude(altitude);
        evdb.setOdometerKM(odomKM);
        return evdb;
    }

    /* create and insert an event record */
    private void insertEventRecord(Device device, 
        long     gpsTime, int statusCode, Geozone geozone,
        GeoPoint geoPoint, long gpsAge, double HDOP, int numSats,
        double   speedKPH, double heading, double altitude, double odomKM)
    {

        /* create event */
        EventData evdb = createEventRecord(device, 
            gpsTime, statusCode, geozone,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, heading, altitude, odomKM);

        /* insert event */
        // this will display an error if it was unable to store the event
        Print.logInfo("Event: [0x" + StringTools.toHexString(statusCode,16) + "] " + 
            StatusCodes.GetDescription(statusCode,null));
        if (!DEBUG_MODE) {
            device.insertEventData(evdb);
            this.incrementSavedEventCount();
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void configInit() 
    {
        DCServerConfig dcsc = Main.getServerConfig();
        if (dcsc != null) {
    
            /* common */
            UNIQUEID_PREFIX          = dcsc.getUniquePrefix();
            MINIMUM_SPEED_KPH        = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
            ESTIMATE_ODOMETER        = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
            SIMEVENT_GEOZONES        = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
            XLATE_LOCATON_INMOTION   = dcsc.getStatusLocationInMotion(XLATE_LOCATON_INMOTION);
            MINIMUM_MOVED_METERS     = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);

            /* custom */
            PACKET_LEN_END_OF_STREAM = dcsc.getBooleanProperty(Constants.CFG_packetLenEndOfStream, PACKET_LEN_END_OF_STREAM);

        }
        
    }

}
