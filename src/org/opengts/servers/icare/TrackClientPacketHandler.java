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
// Description:
//  I-Care G3300 data packet 'business' logic.
//  This modules parses client data packets and inserts them into the EventData table.
//  The ICare G3300 may also go my the names MIC318, and G6500
// ----------------------------------------------------------------------------
// References:
//  - http://www.i-care.net.tw/products-g.htm
// ----------------------------------------------------------------------------
// Change History:
//  2009/04/02  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.servers.icare;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{
    
    // ------------------------------------------------------------------------
    
    /* convenience for converting knots to kilometers */
    public static final double KILOMETERS_PER_KNOT      = 1.85200000;

    // ------------------------------------------------------------------------
    
    /* session IP address */
    private String          ipAddress                   = null;
    
    /* count the number of events we've parsed during this session */
    private int             eventCount                  = 0;
    
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
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;

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
        // minimum packet length is 12, so 'packetLen' should be 12 here
        return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR; // Remainder is an ASCII packet
    }

    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if (pktBytes == null) {
            Print.logError("Packet is null");
        } else
        if (pktBytes.length < 12) {
            Print.logError("Unexpected packet length: " + pktBytes.length);
        } else {
            // skip the first 12 bytes of this packet and parse the rest
            int ofs = 12;
            int len = pktBytes.length - ofs;
            String s = StringTools.toStringValue(pktBytes,ofs,len).trim();
            Print.logInfo("Recv: " + s); // debug message
            this.parseInsertRecord(s);
            this.eventCount++;
            // the remainder of the data stream probably can be flushed
            // this.terminate = true;
        }
        return null; // no return packets are expected
    }

    // ------------------------------------------------------------------------

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy    Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***               YY is year.
    *** @param hms    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***               and SS is second.
    *** @return Time in UTC seconds.
    ***/
    private long _getUTCSeconds(long dmy, long hms)
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

    /* parse and insert data record */
    private boolean parseInsertRecord(String s)
    {
        // ----------------------------------------------------------------------------------------------------------------------
        // ----------------------------------------------------------------------------------------------------------------------
        // ASCII Data:
        //      0              1         2    3     4     5      6     7  8    9     A       B  C D    E        F
        // %355510004002126,$GPRMC,162037.512,A,2731.1236,N,14250.0146,W,0.00,0.01,160507,003.1,W,A,4615551493,L18d
        //   0 - IMEI unique ID
        //   1 - "$GPRMC"
        //   2 - UTC time of position HHMMSS
        //   3 - Validity of the fix ("A" = valid, "V" = invalid)
        //   4 - current latitude in ddmm.mm format
        //   5 - latitude hemisphere ("N" = northern, "S" = southern)
        //   6 - current longitude in dddmm.mm format
        //   7 - longitude hemisphere ("E" = eastern, "W" = western)
        //   8 - speed in knots
        //   9 - true course in degrees
        //   A - date in DDMMYY format
        //   B - magnetic variation in degrees
        //   C - direction of magnetic variation ("E" = east, "W" = west)
        //   D - checksum?
        //   E - phone#?
        //   F - unknown?
        // ----------------------------------------------------------------------------------------------------------------------
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return false;
        }

        /* parse to fields */
        String fld[] = StringTools.parseString(s, ',');
        if ((fld == null) || (fld.length < 11) || !fld[1].equals("$GPRMC")) {
            Print.logWarn("Invalid number of fields");
            return false;
        }

        /* valid IMEI? */
        if (!fld[0].startsWith("%") || (fld[0].length() < 10)) {
            return false;
        }

        /* parse individual fields */
        String  imei        = fld[0].substring(1).toLowerCase();
        long    hms         = StringTools.parseLong(fld[ 2], 0L);
        long    dmy         = StringTools.parseLong(fld[10], 0L);
        long    fixtime     = this._getUTCSeconds(dmy, hms);
        boolean validGPS    = fld[3].equalsIgnoreCase("A");
        double  latitude    = validGPS? this._parseLatitude( fld[4], fld[5])  : 0.0;
        double  longitude   = validGPS? this._parseLongitude(fld[6], fld[7])  : 0.0;
        double  knots       = validGPS? StringTools.parseDouble(fld[8], -1.0) : 0.0;
        double  heading     = validGPS? StringTools.parseDouble(fld[9], -1.0) : 0.0;
        double  speedKPH    = (knots >= 0.0)? (knots * KILOMETERS_PER_KNOT)   : -1.0;
        int     statusCode  = StatusCodes.STATUS_LOCATION;

        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[10] + "/" + fld[2]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* valid lat/lon? */
        if (validGPS &&
            ((latitude  >=  90.0) || (latitude  <=  -90.0) ||
             (longitude >= 180.0) || (longitude <= -180.0)   )) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
            validGPS  = false;
        }

        /* minimum speed */
        if (speedKPH < Constants.MINIMUM_SPEED_KPH) {
            speedKPH = 0.0;
            heading  = 0.0;
        }

        /* parsed data */
        Print.logInfo("IMEI     : " + imei);
        Print.logInfo("Timestamp: " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GPS      : " + new GeoPoint(latitude,longitude));
        Print.logInfo("Speed    : " + StringTools.format(speedKPH,"#0.0") + " kph " + heading);

        /* find Device */
        String        uniqueID   = Constants.UNIQUE_ID_PREFIX_IMEI + imei;
        Device        device     = null;
        DataTransport dataXPort  = null;
        String        accountID  = null;
        String        deviceID   = null;
        try {
            device = Transport.loadDeviceByUniqueID(uniqueID);
            if (device == null) {
                String uniqueID_alt = Constants.UNIQUE_ID_PREFIX_ALT + imei;
                device = Transport.loadDeviceByUniqueID(uniqueID_alt);
                if (device == null) {
                    Print.logWarn("IMEI ID not found!: " + uniqueID); // <== display main key
                    return false;
                }
                uniqueID = uniqueID_alt;
            }
            accountID = device.getAccountID();
            deviceID  = device.getDeviceID();
            dataXPort = device.getDataTransport();
            if (!dataXPort.getDeviceCode().equalsIgnoreCase(Main.getServerName())) {
                dataXPort.setDeviceCode(Main.getServerName()); // FLD_deviceCode
            }
        } catch (DBException dbe) {
            Print.logError("Exception getting Device: " + uniqueID + " [" + dbe + "]");
            return false;
        }
        Print.logInfo("UniqueID : " + uniqueID);
        Print.logInfo("DeviceID : " + accountID + "/" + deviceID);

        /* validate source IP address */
        if ((this.ipAddress != null) && !dataXPort.isValidIPAddress(this.ipAddress)) {
            Print.logError("Invalid IP Address for device: " + this.ipAddress + 
                " [expecting " + dataXPort.getIpAddressValid() + "]");
            return false;
        }
        device.setIpAddressCurrent(this.ipAddress); // FLD_ipAddressCurrent

        /* create event */
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb = evKey.getDBRecord();
        evdb.setLatitude(latitude);
        evdb.setLongitude(longitude);
        evdb.setHeading(heading);
        evdb.setSpeedKPH(speedKPH);

        /* insert event */
        // this will display an error if it was unable to store the event
        device.insertEventData(evdb);

        /* save device changes */
        device.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        try {
            //DBConnection.pushShowExecutedSQL();
            device.update(new String[] { 
                Device.FLD_deviceCode, 
                Device.FLD_ipAddressCurrent, 
                Device.FLD_lastTotalConnectTime
            });
        } catch (DBException dbe) {
            Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
        } finally {
            //DBConnection.popShowExecutedSQL();
        }

        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
