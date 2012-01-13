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
// This module provides support for the Sanav GC-101 device.
//  http://www.sanav.com/gps_tracking/GC-101.htm
// Device record configuration:
//  When creating a new device record this the GC-101, set the UniqueID to the
//  value "imei_<IMEI#>" (or "gc101_<IMEI#>"), where <IMEI#> is the IMEI number 
//  of the GC-101 device.
//  For instance, if the IMEI # of the device is "471623002251245", then set the
//  UniqueID field of the device record to "gc101_471623002251245".
//  Follow the documentation that comes with the device to configure the GC-101
//  to send data to your server.
// ----------------------------------------------------------------------------
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/05/25  Martin D. Flynn
//     -Added static initializer (to support reverse-geocoding)
//  2007/07/14  Martin D. Flynn
//     -Prefix IMEI key with device name.
//  2007/08/09  Martin D. Flynn
//     -Use "imei_" as the primary IMEI # prefix when looking up device unique-ids.
//     -Added configurable "minimum speed"
//     -Invalid GPS fix records are now only rejected for type "AUTO" events.
//  2007/09/16  Martin D. Flynn
//     -The UniqueID Device lookup now occurs in the following order:
//      1-"gc101_<imei>", 2-"imei_<imei>", 3-"<imei>"
//  2007/11/28  Martin D. Flynn
//     -"gc101_<imei>" now works as expected.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2009/08/07  Martin D. Flynn
//     -Added "ALARM1", "ALARM2", ..., support statusCodes
//  2009/08/23  Martin D. Flynn
//     -Changed event type field from "fld[12]" to "fld[fld.length - 1]" to grab
//      the last field in the list.
//     -Remove trailing "-XXXXmv" from event type (some versions of Sanav devices
//      use this area for a battery level indicator).
//  2009/11/01  Martin D. Flynn
//     -Added support for generating an simulated odometer value.
//  2010/04/11  Martin D. Flynn
//     -Added simulated geozone arrive/depart event generation.
// ----------------------------------------------------------------------------
package org.opengts.war.gc101;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.tables.*;
import org.opengts.db.*;

import org.opengts.war.tools.*;

public class Data 
    extends CommonServlet
{

    // ------------------------------------------------------------------------
    
    /* version */
    public  static final String     VERSION                     = "1.0.1";
    
    /* device code */
    public  static final String     DEVICE_CODE                 = "gc101";

    /* UniqueID prefix */
    public  static final String     UNIQUE_ID_PREFIX_GC101      = "gc101_";
    public  static final String     UNIQUE_ID_PREFIX_IMEI       = "imei_";
    private static final boolean    ALSO_CHECK_IMEI             = false;

    /* parameter keys (lookups are case insensitive) */
    private static final String     PARM_IMEI                   = "imei";
    private static final String     PARM_RMC                    = "rmc";

    // ------------------------------------------------------------------------

    /* runtime config */
    public  static final String CONFIG_MIN_SPEED            = DEVICE_CODE + ".minimumSpeedKPH";
    public  static final String CONFIG_ESTIMATE_ODOMETER    = DEVICE_CODE + ".estimateOdometer";
    public  static final String CONFIG_SIMEVENT_GEOZONES    = DEVICE_CODE + ".simulateGeozones";

    // ------------------------------------------------------------------------

    /* estimate GPS-based odometer */
    public  static       boolean ESTIMATE_ODOMETER          = false;

    /* simulate geozones */
    public  static       boolean SIMEVENT_GEOZONES          = false;

    // ------------------------------------------------------------------------

    /* convenience for converting knot to kilometers */
    public static final double  KILOMETERS_PER_KNOT         = 1.85200000;

    // ------------------------------------------------------------------------

    /* default minimum acceptable speed value */
    // Speeds below this value will be considered 'stopped'
    public static double MinimumReqSpeedKPH                 = 4.0;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* static initializer */
    static {

        /* initialize DBFactories */
        // should already have been called by 'RTConfigContextListener'
        DBConfig.servletInit(null);

        /* version */
        Print.logInfo("Version: v" + VERSION);

        /* minimum speed */
        MinimumReqSpeedKPH = RTConfig.getDouble(CONFIG_MIN_SPEED, MinimumReqSpeedKPH);
        Print.logInfo("Minimum speed: " + MinimumReqSpeedKPH + " kph");

        /* calculate estimated estimate GPS-based odometer */
        ESTIMATE_ODOMETER = RTConfig.getBoolean(CONFIG_ESTIMATE_ODOMETER, ESTIMATE_ODOMETER);
        Print.logInfo("Estimating Odometer: " + ESTIMATE_ODOMETER);

        /* simulate geozone arrive/depart */
        SIMEVENT_GEOZONES = RTConfig.getBoolean(CONFIG_SIMEVENT_GEOZONES, SIMEVENT_GEOZONES);
        Print.logInfo("Simulating Geozone: " + SIMEVENT_GEOZONES);

    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Configure the GC-101 to send to a URL similar to:
    //  http://track.example.com/gc101/Data
    // Returned data format:
    //  ?imei=471923002250245&rmc=$GPRMC,023000.000,A,3130.0577,N,14271.7421,W,0.53,208.37,210507,,*19,AUTO
    // Example:
    //  http://localhost:8080/gc101/Data?imei=471923002250245&rmc=$GPRMC,023000.000,A,3130.0577,N,14271.7421,W,0.53,208.37,210507,,*19,AUTO
    //  http://localhost:8080/gc101/Data?imei=352024025553342&rmc=$GPRMC,124422.000,A,3135.5867,S,14245.3128,W,0.16,100.00,110809,,,A*71,alarm1
    //  http://localhost:8080/gc101/Data?imei=00000&rmc=$GPRMC,023000.000,A,3130.0577,N,14271.7421,W,0.53,208.37,210511,,*19,AUTO

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this.doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String ipAddr = request.getRemoteAddr();
        String imei   = AttributeTools.getRequestString(request, PARM_IMEI, "");
        String gprmc  = AttributeTools.getRequestString(request, PARM_RMC , "");
        Print.logInfo("[" + ipAddr + "] URL: " + request.getRequestURL() + " " + request.getQueryString());
        
        /* parse/insert event */
        try {
            this.parseInsertEvent(ipAddr, imei, gprmc);
        } catch (Throwable t) {
            Print.logException("Unexpected Exception", t);
        }
        
        /* write response */
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
        PrintWriter out = response.getWriter();
        out.println(""); // the client is not expecting any response
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert event */
    private boolean parseInsertEvent(String ipAddr, String imei, String gprmc)
    {

        /* null IMEI? */
        if (StringTools.isBlank(imei)) {
            Print.logWarn("Ignoring packet with blank IMEI#");
            return false;
        }

        /* find Device */
        String        uniqueID  = null;
        Device        device    = null;
        DataTransport dataXPort = null;
        try {
            // first, try the standard uniqueID
            String gc101ID = UNIQUE_ID_PREFIX_GC101 + imei;
            device = Transport.loadDeviceByUniqueID(gc101ID);
            if (device != null) {
                // found a match
                uniqueID = gc101ID;
            } else {
                // second, try the alternate uniqueID
                String imeiID = UNIQUE_ID_PREFIX_IMEI + imei;
                device = Transport.loadDeviceByUniqueID(imeiID);
                if (device != null) {
                    // found a match
                    uniqueID = imeiID;
                } else {
                    // third, try the IMEI# by itself
                    if (ALSO_CHECK_IMEI && (imei.length() >= 15)) { // IMEI numbers are 15 digits long
                        device = Transport.loadDeviceByUniqueID(imei);
                        if (device != null) {
                            // found a match
                            uniqueID = imei;
                        }
                    }
                }
            }
            // final check to see if we found the Device record
            if (device == null) {
                Print.logWarn("GC-101 ID not found!: " + gc101ID); // <== display main key
                return false;
            }
            dataXPort = device.getDataTransport();
        } catch (DBException dbe) {
            Print.logError("Exception getting Device: " + uniqueID + " [" + dbe + "]");
            return false;
        }
        String accountID = device.getAccountID();
        String deviceID  = device.getDeviceID();

        /* validate source IP address */
        if ((ipAddr != null) && !dataXPort.isValidIPAddress(ipAddr)) {
            Print.logError("Invalid IP Address for device: " + ipAddr + 
                " [expecting " + dataXPort.getIpAddressValid() + "]");
            return false;
        }

        /* set transport attributes */
        dataXPort.setIpAddressCurrent(ipAddr);      // FLD_ipAddressCurrent
        dataXPort.setDeviceCode(DEVICE_CODE);       // FLD_deviceCode
        device.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime

        /* parse/insert event */
        EventData evdb = this.parseGPRMC(device, gprmc);
        boolean didInsert = (evdb != null)? device.insertEventData(evdb) : false;
        if (didInsert) {
            Print.logInfo("Event inserted: " + accountID + "/" + deviceID + " - " + evdb.getGeoPoint());
        }

        /* simulate geozones */
        if (SIMEVENT_GEOZONES && (evdb != null) && evdb.isValidGeoPoint()) {
            long    timestamp = evdb.getTimestamp();
            GeoPoint geoPoint = evdb.getGeoPoint();
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(timestamp, geoPoint);
            if (zone != null) {
                double speedKPH  = evdb.getSpeedKPH();
                double heading   = evdb.getHeading();
                double odomKM    = evdb.getOdometerKM();
                for (Device.GeozoneTransition z : zone) {
                    EventData.Key zoneKey = new EventData.Key(accountID, deviceID, z.getTimestamp(), z.getStatusCode());
                    EventData zoneEv = zoneKey.getDBRecord();
                    zoneEv.setGeozone(z.getGeozone());
                    zoneEv.setGeoPoint(geoPoint);
                    zoneEv.setSpeedKPH(speedKPH);
                    zoneEv.setHeading(heading);
                    zoneEv.setOdometerKM(odomKM);
                    if (device.insertEventData(zoneEv)) {
                        Print.logInfo("Geozone    : " + z);
                    }
                }
            }
        }

        /* save device changes */
        try {
            // TODO: check "this.device" vs "this.dataXPort"
            device.updateChangedEventFields();
        } catch (DBException dbe) {
            Print.logException("Unable to update Device: " + 
                device.getAccountID() + "/" + device.getDeviceID(), dbe);
        }
        
        return false;

    }
        
    // ------------------------------------------------------------------------

    /* parse status code */
    private int parseStatusCode(String type)
    {
        
        /* extract code */
        // CODE[-XXXXmv]
        int p = type.indexOf('-');
        String code = (p >= 0)? type.substring(0,p) : type;
        
        /* find code match */
        int statusCode = StatusCodes.STATUS_LOCATION;
        if (code.equalsIgnoreCase("AUTO")) {
            statusCode = StatusCodes.STATUS_LOCATION;
        } else 
        if (code.equalsIgnoreCase("SOS")) {
            statusCode = StatusCodes.STATUS_WAYMARK_0;
        } else
        if (code.equalsIgnoreCase("MOVE")) {
          statusCode = StatusCodes.STATUS_MOTION_MOVING;
        } else 
        if (code.equalsIgnoreCase("POLL")) {
            statusCode = StatusCodes.STATUS_QUERY;
        } else
        if (code.equalsIgnoreCase("GFIN")) {
            statusCode = StatusCodes.STATUS_GEOFENCE_ARRIVE;
        } else
        if (code.equalsIgnoreCase("GFOUT")) {
            statusCode = StatusCodes.STATUS_GEOFENCE_DEPART;
        } else
        if (code.equalsIgnoreCase("PARK")) {
          //statusCode = StatusCodes.STATUS_GEOFENCE_ARRIVE;
        } else
        if (code.equalsIgnoreCase("UNPARK")) {
          //statusCode = StatusCodes.STATUS_GEOFENCE_DEPART;
        } else
        if (code.equalsIgnoreCase("LP")) {
            statusCode = StatusCodes.STATUS_LOW_BATTERY;
        } else
        if (code.equalsIgnoreCase("ALARM1")) {
            statusCode = StatusCodes.STATUS_INPUT_ON_01;
        } else
        if (code.equalsIgnoreCase("ALARM2")) {
            statusCode = StatusCodes.STATUS_INPUT_ON_02;
        } else
        if (code.equalsIgnoreCase("ALARM3")) {
            statusCode = StatusCodes.STATUS_INPUT_ON_03;
        } else
        if (code.equalsIgnoreCase("ALARM4")) {
            statusCode = StatusCodes.STATUS_INPUT_ON_04;
        } else
        if (code.equalsIgnoreCase("ALARM5")) {
            statusCode = StatusCodes.STATUS_INPUT_ON_05;
        } else
        if (code.equalsIgnoreCase("ALARM6")) {
            statusCode = StatusCodes.STATUS_INPUT_ON_06;
        } else
        {
            statusCode = StatusCodes.STATUS_LOCATION;
        }
        return statusCode;

    }
    
    /* parse GPRMC record */
    private EventData parseGPRMC(Device dev, String data)
    {
        String fld[] = StringTools.parseString(data, ',');
        
        /* invalid record? */
        if ((fld == null) || (fld.length < 1) || !fld[0].equals("$GPRMC")) {
            Print.logWarn("Not a $GPRMC record");
            return null;
        } else
        if (fld.length < 10) {
            Print.logWarn("Invalid number of $GPRMC fields");
            return null;
        }
                    
        /* parse */
        long    hms       = StringTools.parseLong(fld[1], 0L);
        long    dmy       = StringTools.parseLong(fld[9], 0L);
        long    fixtime   = this._getUTCSeconds(dmy, hms);
        boolean isValid   = fld[2].equals("A");
        double  latitude  = isValid? this._parseLatitude (fld[3], fld[4]) : 0.0;
        double  longitude = isValid? this._parseLongitude(fld[5], fld[6]) : 0.0;
        double  knots     = isValid? StringTools.parseDouble(fld[7], 0.0) : 0.0;
        double  heading   = isValid? StringTools.parseDouble(fld[8], 0.0) : 0.0;
        double  speedKPH  = (knots > 0.0)? (knots * KILOMETERS_PER_KNOT)  : 0.0;
                
        /* valid lat/lon? */
        if ((latitude  >=  90.0) || (latitude  <=  -90.0) ||
            (longitude >= 180.0) || (longitude <= -180.0)   ) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
            isValid   = false;
        }
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        /* status code */
        // AUTO, <i/o>, MOVE, POLL, GFIN, GFOUT, PARK, UNPARK, <power-cut>, SOS, <power-low>
        //String type = (fld.length > 12)? fld[12] : ""; // "POLL", "SOS", "LP", "AUTO"
        String type = (fld.length > 12)? fld[fld.length - 1] : ""; // last field
        int statusCode = this.parseStatusCode(type);
        if (statusCode == StatusCodes.STATUS_IGNORE) {
            return null;
        } else
        if (statusCode == StatusCodes.STATUS_NONE) {
            return null;
        } else
        if ((statusCode == StatusCodes.STATUS_LOCATION) && !isValid) {
            Print.logWarn("Ignoring event with invalid GPS fix");
            return null;
        }

        /* minimum speed */
        if (speedKPH < MinimumReqSpeedKPH) {
            speedKPH = 0.0;
            heading  = 0.0;
        }

        /* estimate GPS-based odometer */
        double odomKM = (ESTIMATE_ODOMETER && isValid)? 
            dev.getNextOdometerKM(geoPoint) : 
            dev.getLastOdometerKM();

        /* create/return EventData record */
        String acctID = dev.getAccountID();
        String devID  = dev.getDeviceID();
        EventData.Key evKey = new EventData.Key(acctID, devID, fixtime, statusCode);
        EventData evdb = evKey.getDBRecord();
        evdb.setGeoPoint(geoPoint);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(heading);
        evdb.setOdometerKM(odomKM);
        return evdb;

    }
    
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

}
