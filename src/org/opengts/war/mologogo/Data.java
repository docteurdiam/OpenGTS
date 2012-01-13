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
// This module provides support for Mologogo capable devices (www.mologogo.com)
//  http://www.mologogo.com
//  http://mologogo.wikispaces.com/Alternate+URL+feature
// ----------------------------------------------------------------------------
// Change History:
//  2007/02/25  Martin D. Flynn
//     -Initial release
//  2007/02/28  Martin D. Flynn
//     -Added MinimumReqSpeedKPH
//     -Horizontal-Accuracy is now saved in EventData
//  2007/05/06  Martin D. Flynn
//     -Extension: Added additional key "utctime" for specifying time.
//  2007/05/25  Martin D. Flynn
//     -Added static initializer (to support reverse-geocoding)
//  2007/11/28  Martin D. Flynn
//     -Added additional runtime properties.
//  2008/02/21  Martin D. Flynn
//     -Fixed bug in 'logWarn' method which caused and endless recursion and
//      out-of-memory error.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
// ----------------------------------------------------------------------------
package org.opengts.war.mologogo;

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
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;

public class Data 
    extends CommonServlet
{
    
    // ------------------------------------------------------------------------
    // Sample Data:
    //   http://track.example.com/mologogo/data?
    //     [acct=<AccountID>&]
    //     [dev=<DeviceID>&]
    //     [id=<UniqueID>&]
    //     [utctime=<EpochTime>&]
    //      lat=<Latitude>&
    //      lon=<Longitude>&
    //     [accuracy=<Accuracy>&]
    //      direction=<Heading>&
    //      speed=<KPH>&
    //     [speedUncertainty=0&]            - not currently used
    //      altitude=<Meters>&
    //     [altitudeUncertainty=49390&]     - not currently used
    //      pointType=GPS
    // ------------------------------------------------------------------------

    /* name used to tag logged messaged */
    public static String  LOG_NAME                      = "mologogo";

    // ------------------------------------------------------------------------
    
    /* unique id prefix */
    public static String  UniqueIDPrefix                = LOG_NAME;

    /* default account id */
    // The URL variable "?ID=<id>" is used to allow this module the ability to uniquely
    // identify a reporting phone.  The reported "<id>" provided by the phone is used
    // as the "DeviceID" for the following AccountID.  This value can be overridden in
    // the Servlet 'webapp.conf' file.
    public static String  DefaultAccountID              = "mologogo";

    /* allow using "?ACCT=<account>&DEV=<device>" as an alternative form of identification */
    // If you do not want to use a default AccountID for identification, setting this
    // variable to 'true' will allow using the alternative "?ACCT=<account>&DEV=<device>"
    // for to identify a specific phone.  "<account>" would be replaced with the actual 
    // AccountID, and "<device>" would be replaced with the actual DeviceID.  This value
    // can be overridden in the Servlet 'webapp.conf' file.  Use this feature with care, 
    // as it may enable others (ie. hackers) to be able to insert data records for other
    // accounts in the database.
    public static boolean AllowAccountDeviceID          = false;

    /* minimum required speed */
    // GPS receivers have a tendency to appear 'moving', even when sitting stationary
    // on your desk.  This filter is a 'speed' threshold which is used to force the 
    // reported speed to '0' if it falls below this value.  This value can be overridden
    // in the Servlet 'webapp.conf' file.
    public static double  MinimumReqSpeedKPH            = 4.0;
    
    /* Default time zone */
    private static TimeZone gmtTimeZone                 = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------

    public static final String POINT_TYPE_GPS           = "GPS";
    public static final String POINT_TYPE_CELL          = "CELL";
    public static final String POINT_TYPE_MANUAL        = "MANUAL";     // waymark
    public static final String POINT_TYPE_MOTION        = "MOTION";     // in-motion
    public static final String POINT_TYPE_DORMANT       = "DORMANT";    // not moving

    // ------------------------------------------------------------------------

    /* configuration name (TODO: update based on specific servlet configuration) */
    public static final String  CONFIG_NAME             = "mologogo";
    
    /* runtime config */
    public static final String  CONFIG_LOG_NAME         = CONFIG_NAME + ".logName";
    public static final String  CONFIG_UNIQUE_PREFIX    = CONFIG_NAME + ".uniquePrefix";
    public static final String  CONFIG_ALLOW_ACCTDEV    = CONFIG_NAME + ".allowAcctDevID";
    public static final String  CONFIG_DFT_ACCOUNT      = CONFIG_NAME + ".defaultAccountID";
    public static final String  CONFIG_MIN_SPEED        = CONFIG_NAME + ".minimumSpeedKPH";

    // ------------------------------------------------------------------------
    
    // parameter keys (lookups are case insensitive)
    private static final String PARM_ACCOUNT            = "acct";       // account
    private static final String PARM_DEVICE             = "dev";        // device
    private static final String PARM_ID                 = "id";         // unique id
    private static final String PARM_TIMESTAMP          = "utctime";    // UTC time
    private static final String PARM_LATITUDE           = "lat";        // latitude
    private static final String PARM_LONGITUDE          = "lon";        // longitude
    private static final String PARM_ACCURACY           = "accuracy";   // +/- millimeters
    private static final String PARM_HEADING            = "direction";  // heading
    private static final String PARM_SPEED              = "speed";      // speedKPH
    private static final String PARM_SPEED_UNC          = "speedUncertainty"; // +/- kph
    private static final String PARM_ALTITUDE           = "altitude";   // altitude meters
    private static final String PARM_ALT_UNC            = "altitudeUncertainty"; // +/- meters
    private static final String PARM_POINT_TYPE         = "pointType";  // GPS/CELL/MANUAL

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* static initializer */
    static {

        /* initialize DBFactories */
        // should already have been called by 'RTConfigContextListener'
        DBConfig.servletInit(null);

        /* set configuration */
        LOG_NAME             = RTConfig.getString( CONFIG_LOG_NAME      , LOG_NAME);
        UniqueIDPrefix       = RTConfig.getString( CONFIG_UNIQUE_PREFIX , LOG_NAME);
        AllowAccountDeviceID = RTConfig.getBoolean(CONFIG_ALLOW_ACCTDEV , AllowAccountDeviceID);
        DefaultAccountID     = RTConfig.getString( CONFIG_DFT_ACCOUNT   , DefaultAccountID);
        MinimumReqSpeedKPH   = RTConfig.getDouble( CONFIG_MIN_SPEED     , MinimumReqSpeedKPH);
        if ((DefaultAccountID != null) && !DefaultAccountID.equals("")) {
            Data.logInfo("'ID' lookup using AccountID[" + DefaultAccountID + "]");
        } else {
            Data.logInfo("'ID' lookup using UniqueID");
        }
        Data.logInfo("Allow 'ACCT=<id>&DEV=<id>'?: " + AllowAccountDeviceID);
        Data.logInfo("Minimum speed: " + MinimumReqSpeedKPH + " kph");
        
    };

    // ------------------------------------------------------------------------

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this.doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String ipAddr     = request.getRemoteAddr();
        String accountID  = AttributeTools.getRequestString(request, PARM_ACCOUNT   , "");  // extension (not part of Mologogo)
        String deviceID   = AttributeTools.getRequestString(request, PARM_DEVICE    , "");  // extension (not part of Mologogo)
        String uniqueID   = AttributeTools.getRequestString(request, PARM_ID        , "");
        String timeStr    = AttributeTools.getRequestString(request, PARM_TIMESTAMP , "");  // extension (not part of Mologogo)
        double latitude   = AttributeTools.getRequestDouble(request, PARM_LATITUDE  , 0.0);
        double longitude  = AttributeTools.getRequestDouble(request, PARM_LONGITUDE , 0.0);
        double accuracyMM = AttributeTools.getRequestDouble(request, PARM_ACCURACY  , 0.0); // millimeters
        double headingDeg = AttributeTools.getRequestDouble(request, PARM_HEADING   , 0.0); // degrees
        double speedKPH   = AttributeTools.getRequestDouble(request, PARM_SPEED     , 0.0); // kph
      //double speedUnc   = AttributeTools.getRequestDouble(request, PARM_SPEED_UNC , 0.0); // kph (not yet saved in EventData table)
        double altitudeM  = AttributeTools.getRequestDouble(request, PARM_ALTITUDE  , 0.0); // meters
      //double altUnc     = AttributeTools.getRequestDouble(request, PARM_ALT_UNC   , 0.0); // millimeters (not yet saved in EventData table)
        String pointType  = AttributeTools.getRequestString(request, PARM_POINT_TYPE, "");  // GPS/CELL/MANUAL
        
        /* URL */
        String URL = "[" + ipAddr + "] URL: " + request.getRequestURL() + " " + request.getQueryString();

        /* timestamp */
        long fixtime = DateTime.getCurrentTimeSec(); // assume 'now' is the fix time
        if (!timeStr.equals("")) {
            if (timeStr.indexOf("/") > 0) {
                // "utctime=YYYY/MM/DD/hh/mm/ss" or "utctime=YYYY/MM/DD,hh:mm:ss"
                String d[] = StringTools.parseString(timeStr, "/,: ");
                if (d.length >= 6) {
                    int YYYY = StringTools.parseInt(d[0], 0);
                    if (YYYY < 100) { YYYY += (YYYY > 40)? 1900 : 2000; }
                    int MM = StringTools.parseInt(d[1], 0);
                    int DD = StringTools.parseInt(d[2], 0);
                    int hh = StringTools.parseInt(d[3], 0);
                    int mm = StringTools.parseInt(d[4], 0);
                    int ss = StringTools.parseInt(d[5], 0);
                    DateTime dt = new DateTime(gmtTimeZone, YYYY, MM, DD, hh, mm, ss);
                    fixtime = dt.getTimeSec();
                }
            } else {
                // "utctime=1177383424"
                long t = StringTools.parseLong(timeStr,0L);
                if (t > 0L) {
                    fixtime = t;
                }
            }
        }

        /* do we have an id? */
        if (!uniqueID.equals("")) {
            // "?ID=<id>"
            if (!accountID.equals("") || !deviceID.equals("")) {
                // 'accountID/deviceID' and 'uniqueID' are mutually exclusive
                Data.logError(URL, "UniqueID and AccountID/DeviceID are mutually exclusive");
                this.plainTextResponse(response, "");
                return;
            }
        } else {
            // "?ACCT=<acct>&DEV=<dev>"
            if (accountID.equals("") || deviceID.equals("")) {
                // if 'uniqueID' not specified, both 'accountID/deviceID' must be specified
                Data.logError(URL, "Unable to identify Device (either ID or ACCT/DEV must be specified)");
                this.plainTextResponse(response, "");
                return;
            }
        }

        /* read the device */
        Device device = null;
        try {
            String msgDevKey = "";
            if (!uniqueID.equals("")) {
                // "?ID=<id>"
                if ((DefaultAccountID != null) && !DefaultAccountID.equals("")) {
                    // try using <id> as DeviceID for Account specified by DefaultAccountID
                    msgDevKey = "AccountID/DeviceID: " + DefaultAccountID + "/" + uniqueID;
                    device = Transport.loadDeviceByTransportID(Account.getAccount(DefaultAccountID), uniqueID);
                } else {
                    // try using <ad> as UniqueID
                    String uid   = UniqueIDPrefix + "_" + uniqueID; // ie: "mologogo_123456789012345"
                    msgDevKey = "UniqueID: " + uid;
                    device = Transport.loadDeviceByUniqueID(uid); // may return null
                }
            } else
            if (AllowAccountDeviceID) {
                // try the AccountID/DeviceID (if allowed)
                msgDevKey = "AccountID/DeviceID: " + accountID + "/" + deviceID;
                device = Transport.loadDeviceByTransportID(Account.getAccount(accountID), deviceID);
            }
            if (device == null) {
                // Device was not found
                Data.logError(URL, "Device not found " + msgDevKey);
                this.plainTextResponse(response, "");
                return;
            }
        } catch (DBException dbe) {
            // Error while reading Device
            Data.logException(URL, "Error reading Device", dbe);
            this.plainTextResponse(response, "");
            return;
        }

        /* update actual device/account ids */
        deviceID  = device.getDeviceID();
        accountID = device.getAccountID();

        /* validate source IP address */
        // This may be used to prevent rogue hackers from spoofing data coming from the phone
        if (!device.getDataTransport().isValidIPAddress(ipAddr)) {
            // 'ipAddr' does not match allowable device IP addresses
            Data.logError(URL, "Invalid IP Address for device");
            this.plainTextResponse(response, "");
            return;
        }
        
        /* display URL (debug) */
        Data.logInfo(URL);

        /* validate latitude/longitude */
        if ((latitude  >=  90.0) || (latitude  <=  -90.0) ||
            (longitude >= 180.0) || (longitude <= -180.0)   ) {
            Data.logWarn("Invalid lat/lon: " + latitude + "/" + longitude);
            // reject?
        } else
        if ((latitude == 0.0) && (longitude == 0.0)) {
            Data.logWarn("Invalid lat/lon: " + latitude + "/" + longitude);
        }
        
        /* adjustments to received values */
        if (speedKPH < MinimumReqSpeedKPH) {
            // Say we're not moving if the value is <= our desired threshold
            speedKPH = 0.0;
        }
        if ((speedKPH <= 0.0) || (headingDeg < 0.0)) {
            // We're either not moving, or the GPS receiver doesn't know the heading
            headingDeg = 0.0; // to be consistent, set the heading to North
        }

        /* status code */
        int statusCode = StatusCodes.STATUS_LOCATION;
        if (pointType.equalsIgnoreCase(POINT_TYPE_GPS)) {
            // standard GPS point
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (pointType.equalsIgnoreCase(POINT_TYPE_CELL)) {
            // location of cell tower?
            statusCode = StatusCodes.STATUS_LOCATION;
            // does 'accuracyMM' reflect the uncertainty in this GPS fix?
        } else
        if (pointType.equalsIgnoreCase(POINT_TYPE_MANUAL)) {
            // manually entered GPS point?
            statusCode = StatusCodes.STATUS_WAYMARK_0;
        } else
        if (pointType.equalsIgnoreCase(POINT_TYPE_MOTION)) {
            // moving (not a Mologogo code)
            statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
        } else
        if (pointType.equalsIgnoreCase(POINT_TYPE_DORMANT)) {
            // not moving (not a Mologogo code)
            statusCode = StatusCodes.STATUS_MOTION_DORMANT;
        } else {
            statusCode = StatusCodes.STATUS_LOCATION;
        }

        /* create and insert event */
        try {
    
            /* create event */
            EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
            EventData evdb = evKey.getDBRecord();
            evdb.setDataSource(pointType);
            evdb.setLatitude(latitude);
            evdb.setLongitude(longitude);
            evdb.setHorzAccuracy(accuracyMM / 1000.0); // convert to meters
            evdb.setSpeedKPH(speedKPH);
            evdb.setHeading(headingDeg);
            evdb.setAltitude(altitudeM);
    
            /* insert event */
            // this will display an error if it was unable to store the event
            device.insertEventData(evdb);

        } catch (Throwable th) {
            
            /* display error */
            Print.logException("Error creating/inserting event", th);
            
        }

        /* write success response */
        this.plainTextResponse(response, "");

    }
    
    // ------------------------------------------------------------------------

    private void plainTextResponse(HttpServletResponse response, String errMsg)
        throws ServletException, IOException
    {
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
        PrintWriter out = response.getWriter();
        out.println(errMsg);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // message logging
    
    private static void logInfo(String msg)
    {
        Print.logInfo(LOG_NAME + ": " + msg);
    }

    private static void logWarn(String msg)
    {
        Print.logWarn(LOG_NAME + ": " + msg);
    }

    private static void logError(String URL, String msg)
    {
        if (URL != null) {
            Print.logError(LOG_NAME + ": " + URL);
        }
        Print.logError(LOG_NAME + ": " + msg);
    }

    private static void logException(String URL, String msg, Throwable th)
    {
        if (URL != null) {
            Print.logError(LOG_NAME + ": " + URL);
        }
        Print.logException(LOG_NAME + ": " + msg, th);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
