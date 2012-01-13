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
// References:
//  - http://www.aspicore.com
// ----------------------------------------------------------------------------
// Sample record: TCP/UDP
//  IMEI 123456789012345
//  $GPRMC,144858.159,A,4009.0358,N,14253.3223,W,0.00,006.40,191104,,*05
//  $GPGGA,092916.000,,,,,0,,,,,,,,*7D
//  OutCell 38091 LAC 30464 Name SAT-C MCC 510 MNC 1
//  In Cell 34747 LAC 2161 Name SONERA MCC 244 MNC 91
//  CurCell 11353 LAC 4431 Name TELCEL GSM MCC 334 MNC 020 MODE 2 SSI 69
//  Label UserLabel
//  *DE6279AE
//
// If HTTP-mode: (not yet supported)
//  http://UDL/Data?imei=123456789012345&lat=40.172740&lon=142.761930&status=A&speed=000.0&course=189.8&time=081055.668&date=250907 
//
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/18  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.servers.aspicore;

import java.lang.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.cellid.CellTower;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------

    public static       String  UNIQUEID_PREFIX[]           = new String[] { "imei_" };
    public static       double  MINIMUM_SPEED_KPH           = 3.0;
    public static       boolean ESTIMATE_ODOMETER           = false;
    public static       boolean SIMEVENT_GEOZONES           = false;
    public static       boolean XLATE_LOCATON_INMOTION      = false;
    public static       double  MINIMUM_MOVED_METERS        = 0.0;

    public static       boolean DEBUG_MODE                  = false;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* session IP address */
    private String          ipAddress                       = null;
    private int             clientPort                      = 0;

    /* current device */
    private Device          device                          = null;
    private DataTransport   dataXPort                       = null;
    private String          mobileID                        = null;
    private Nmea0183        gprmc                           = null;
    private CellTower       outboundCell                    = null;
    private CellTower       inboundCell                     = null;
    private CellTower       currentCell                     = null;
    private String          label                           = null;

    /* session start time */
    private long            sessionStartTime                = 0L;

    /* count the number of events we've parsed during this session */
    private int             eventTotalCount                 = 0;

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

        /* init */
        this.sessionStartTime = DateTime.getCurrentTimeSec();
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;
        this.clientPort       = this.getSessionInfo().getRemotePort();
        this.eventTotalCount  = 0;
        this.mobileID         = null;
        this.device           = null;
        this.gprmc            = new Nmea0183();

    }
    
    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
    }

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        // Sample record:
        //  IMEI 123456789012345
        //  $GPRMC,144858.159,A,6009.0358,N,14253.3223,W,0.00,006.40,191104,,*05
        //  OutCell 38091 LAC 30464 Name SAT-C MCC 510 MNC 1
        //  In Cell 34747 LAC 2161 Name SONERA MCC 244 MNC 91
        //  Label Aspicore Ltd
        //  *DE6279AE

        /* check packet length */
        if ((pktBytes == null) || (pktBytes.length == 0)) {
            //Print.logDebug("Packet is null/empty");
            return null;
        }
        String s = StringTools.toStringValue(pktBytes).trim();
        Print.logInfo("Recv: " + s); // debug message

        /* "IMEI" */
        if (s.startsWith("IMEI")) {
            this.mobileID       = s.substring(4).trim();
            this.gprmc          = null; // should already be null
            this.outboundCell   = null;
            this.inboundCell    = null;
            this.currentCell    = null;
            this.label          = null;
            return null;
        }

        /* "$GPRMC" / "$GPGGA" */
        if (s.startsWith("$GP")) {
            if (this.gprmc == null) { this.gprmc = new Nmea0183(); }
            this.gprmc.parse(s); // with checksum
            return null;
        } else
        if (s.startsWith("No ")) { // "No GPS info"
            return null;
        }

        /* "Label" (Current location entered by user) */
        if (s.startsWith("Label")) {
            this.label = s.substring(5).trim();
            return null;
        }

        /* "OutCell" */
        if (s.startsWith("OutCell")) {
            Pattern pat = Pattern.compile("\\bOutCell (\\d+) LAC (\\d+) Name ([\\S ]*) MCC (\\d+) MNC (\\d+) MODE (\\d+)\\b");
            Matcher mat = pat.matcher(s);
            if (mat.find()) {
                CellTower ct = new CellTower();
                ct.setCellTowerID      (StringTools.parseInt(mat.group(1),0));
                ct.setLocationAreaCode (StringTools.parseInt(mat.group(2),0));
                ct.setName             (                     mat.group(3)   );
                ct.setMobileCountryCode(StringTools.parseInt(mat.group(4),0));
                ct.setMobileNetworkCode(StringTools.parseInt(mat.group(5),0));
                this.outboundCell = ct;
            } else {
                Print.logError("Unable to parse 'OutCell' pattern");
            }
            return null;
        }

        /* "In Cell" */
        if (s.startsWith("In Cell")) {
            Pattern pat = Pattern.compile("\\bIn Cell (\\d+) LAC (\\d+) Name ([\\S ]*) MCC (\\d+) MNC (\\d+) MODE (\\d+)\\b");
            Matcher mat = pat.matcher(s);
            if (mat.find()) {
                CellTower ct = new CellTower();
                ct.setCellTowerID      (StringTools.parseInt(mat.group(1),0));
                ct.setLocationAreaCode (StringTools.parseInt(mat.group(2),0));
                ct.setName             (                     mat.group(3)   );
                ct.setMobileCountryCode(StringTools.parseInt(mat.group(4),0));
                ct.setMobileNetworkCode(StringTools.parseInt(mat.group(5),0));
                this.inboundCell = ct;
            } else {
                Print.logError("Unable to parse 'In Cell' pattern");
            }
            return null;
        }

        /* "CurCell" */
        if (s.startsWith("In Cell")) {
            // uses the same parsing example provided by Aspicore example
            Pattern pat = Pattern.compile("\\bCurCell (\\d+) LAC (\\d+) Name ([\\S ]*) MCC (\\d+) MNC (\\d+) MODE (\\d+)\\b");
            Matcher mat = pat.matcher(s);
            if (mat.find()) {
                CellTower ct = new CellTower();
                ct.setCellTowerID      (StringTools.parseInt(mat.group(1),0));
                ct.setLocationAreaCode (StringTools.parseInt(mat.group(2),0));
                ct.setName             (                     mat.group(3)   );
                ct.setMobileCountryCode(StringTools.parseInt(mat.group(4),0));
                ct.setMobileNetworkCode(StringTools.parseInt(mat.group(5),0));
                this.currentCell = ct;
            } else {
                Print.logError("Unable to parse 'CurCell' pattern");
            }
            return null;
        }

        /* checksum */
        if (s.startsWith("*")) {
            // checksum is ignored
            this.insertData();
            this.gprmc          = null;
            this.outboundCell   = null;
            this.inboundCell    = null;
            this.currentCell    = null;
            this.label          = null;
            return null;
        }

        /* record not handled */
        return null;

    }

    /* intercept the final packet to insert the parsed record */
    public byte[] getFinalPacket(boolean hasError) 
        throws Exception
    {
        this.insertData();
        return null;
    }

    // ------------------------------------------------------------------------

    private boolean insertData()
    {

        /* no data? */
        if ((this.mobileID == null) || (this.gprmc == null)) {
            // no mobile ID
            return false;
        }

        /* MobileID */
        String imei = StringTools.trim(this.mobileID);
        Print.logInfo("Mobile ID    : " + imei);
        this.device = DCServerFactory.loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, imei);
        if (this.device == null) {
            Print.logError("Device not found: " + imei);
            DCServerFactory.addUnassignedDevice(Constants.DEVICE_CODE, imei, 
                this.ipAddress, this.isDuplex(), null);
            return false;
        }
        this.dataXPort   = this.device.getDataTransport();
        String accountID = this.device.getAccountID();
        String deviceID  = this.device.getDeviceID();

        /* validate source IP address */
        if ((this.ipAddress != null) && 
            !this.dataXPort.isValidIPAddress(this.ipAddress)) {
            Print.logError("Invalid IP Address for device: " + this.ipAddress + 
                " [expecting " + this.dataXPort.getIpAddressValid() + "]");
            return false;
        }

        /* updated Device attributes */
        // TODO: change "this.device" to "this.dataXPort"
        this.device.setIpAddressCurrent(this.ipAddress);   // FLD_ipAddressCurrent
        this.device.setRemotePortCurrent(this.clientPort); // FLD_remotePortCurrent
        this.device.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!this.dataXPort.getDeviceCode().equalsIgnoreCase(Constants.DEVICE_CODE)) {
            this.dataXPort.setDeviceCode(Constants.DEVICE_CODE); // FLD_deviceCode
        }
        Print.logInfo("DeviceID     : ["+this.device.getUniqueID()+"] " + accountID + "/" + deviceID + " [" + this.device.getDescription() + "]");

        /* data */
        int      statusCode = StatusCodes.STATUS_LOCATION;
        long     fixtime    = (this.gprmc != null)? this.gprmc.getFixtime()        : 0L;
        double   latitude   = (this.gprmc != null)? this.gprmc.getLatitude()       : 0.0;
        double   longitude  = (this.gprmc != null)? this.gprmc.getLongitude()      : 0.0;
        boolean  validGPS   = (this.gprmc != null)? this.gprmc.isValidGPS()        : false;
        double   speedKPH   = (this.gprmc != null)? this.gprmc.getSpeedKPH()       : 0.0;
        double   headingDeg = (this.gprmc != null)? this.gprmc.getHeading()        : 0.0;
        double   altitudeM  = (this.gprmc != null)? this.gprmc.getAltitudeMeters() : 0.0;
        double   odomKM     = 0.0; // set to available odometer from event record
        GeoPoint geoPoint   = new GeoPoint(latitude,longitude);

        /* adjustments to speed/heading */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* STATUS_LOCATION / STATUS_MOTION_IN_MOTION */
        if (statusCode == StatusCodes.STATUS_NONE) {
            statusCode = (speedKPH > 0.0)? StatusCodes.STATUS_MOTION_IN_MOTION : StatusCodes.STATUS_LOCATION;
        } else
        if (XLATE_LOCATON_INMOTION && (statusCode == StatusCodes.STATUS_LOCATION) && (speedKPH > 0.0)) {
            statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
        }

        /* debug */
        Print.logInfo("Fixtime  : " + new DateTime(fixtime));
        Print.logInfo("GeoPoint : " + geoPoint);
        Print.logInfo("Speed    : " + speedKPH + " km/h [heading " + headingDeg + "]");

        /* reject invalid GPS fixes? */
        if (!validGPS && (statusCode == StatusCodes.STATUS_LOCATION)) {
            Print.logWarn("Ignoring event with invalid latitude/longitude");
            return true; // no error, we're just ignoring the record
        }

        /* ignore status code event */
        if ((statusCode < 0) || (statusCode == StatusCodes.STATUS_IGNORE)) {
            Print.logWarn("Ignoring event with IGNORE status code");
            return true; // no error, we're just ignoring the record
        }

        /* minimum proximity */
        if (validGPS && (MINIMUM_MOVED_METERS > 0.0) && (statusCode == StatusCodes.STATUS_LOCATION)) {
            GeoPoint prevGPS = this.device.getLastValidLocation();
            if (prevGPS != null) {
                double deltaM = geoPoint.metersToPoint(prevGPS);
                if (deltaM < MINIMUM_MOVED_METERS) {
                    // inside minimum zone, skip event
                    Print.logWarn("Ignoring event within close proximity to previous event");
                    return true; // no error, we're just ignoring the record
                }
            }
        }

        /* insert */
        if ((statusCode != StatusCodes.STATUS_LOCATION) || (this.eventTotalCount <= 0)) {
            this.insertEventRecord(this.device, 
                fixtime, statusCode, null,
                geoPoint, speedKPH, headingDeg, altitudeM, odomKM);
        }

        /* save device changes */
        if (!DEBUG_MODE) {
            try {
                // TODO: check "this.device" vs "this.dataXPort"
                this.device.updateChangedEventFields();
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: "+this.device.getAccountID()+"/"+this.device.getDeviceID(),dbe);
            } finally {
                //
            }
        }

        /* return */
        return true;

    }

    // ------------------------------------------------------------------------

    private EventData createEventRecord(Device device, 
        long     fixtime,
        int      statusCode,
        Geozone  geozone,
        GeoPoint geoPoint, 
        double   speedKPH, double heading, 
        double   altitude,
        double   odomKM)
    {
        String accountID    = (device != null)? device.getAccountID() : "";
        String deviceID     = (device != null)? device.getDeviceID()  : "";
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeozone(geozone);
        evdb.setGeoPoint(geoPoint);
        evdb.setHeading(heading);
        evdb.setSpeedKPH(speedKPH);
        evdb.setAltitude(altitude);
        evdb.setOdometerKM(odomKM);
        return evdb;
    }

    /* create and insert an event record */
    private void insertEventRecord(Device device, 
        long     fixtime,
        int      statusCode,
        Geozone  geozone,
        GeoPoint geoPoint, 
        double   speedKPH, double heading, 
        double   altitude,
        double   odomKM)
    {
        if (DEBUG_MODE || (this.device == null)) { return; }

        /* create event */
        EventData evdb = createEventRecord(device, 
            fixtime, statusCode, geozone,
            geoPoint, speedKPH, heading, altitude, odomKM);

        /* insert event */
        // this will display an error if it was unable to store the event
        Print.logInfo("Event     : [0x" + StringTools.toHexString(statusCode,16) + "] " + 
            StatusCodes.GetDescription(statusCode,null));
        device.insertEventData(evdb);
        this.eventTotalCount++;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void configInit() 
    {
        DCServerConfig dcsc = Main.getServerConfig();
        if (dcsc != null) {
            UNIQUEID_PREFIX         = dcsc.getUniquePrefix();
            MINIMUM_SPEED_KPH       = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
            ESTIMATE_ODOMETER       = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
            SIMEVENT_GEOZONES       = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
            XLATE_LOCATON_INMOTION  = dcsc.getStatusLocationInMotion(XLATE_LOCATON_INMOTION);
            MINIMUM_MOVED_METERS    = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);
        }
    }

    // ------------------------------------------------------------------------

}
