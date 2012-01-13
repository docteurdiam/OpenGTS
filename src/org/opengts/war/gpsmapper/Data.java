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
// This module supports the server-side component of the 'GPSMapper' project on
// SourceForge at "http://sourceforge.net/projects/gpsmapper/".
// Note:
//  - This module has not yet been fully tested.
// ----------------------------------------------------------------------------
// Build the "gpsmapper.war" file:
//    ant gpsmapper
// Deploy to Tomcat:
//    cp build/gpsmapper.war $CATALINA_HOME/webapps/.
// Configure phone to send data to
//    http://<YourDomain>/gpsmapper/Data?.....
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Change History:
//  2009/04/02  Martin D. Flynn
//     -Initial release. 
// ----------------------------------------------------------------------------
package org.opengts.war.gpsmapper;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.google.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;

public class Data 
    extends CommonServlet
{

    // ------------------------------------------------------------------------

    /* name used to tag logged messaged */
    public static String LOG_NAME                       = "gpsmapper";

    /* error response */
    public static String RESPONSE_INTERNAL_ERROR        = "INTERNAL_ERROR";
    public static String RESPONSE_INVALID_ID            = "INVALID_ID";
    public static String RESPONSE_INVALID_FIX           = "INVALID_FIX";
    public static String RESPONSE_INVALID_IP            = "INVALID_IP";
    public static String RESPONSE_MAP_UNAVAIL           = "MAP_UNAVAILABLE";

    // ------------------------------------------------------------------------

    /* runtime config */
    public static final String  CONFIG_MIN_SPEED        = "gpsmapper.minimumSpeedKPH";
    public static final String  CONFIG_GOOGLE_KEY       = "gpsmapper.googleMapKey";

    // ------------------------------------------------------------------------

    /* unique id prefix */
    // This allows Unigue IDs coming from this server to be unique system-wide
    public static String    UniqueIDPrefix              = "gm_";

    /* minimum required speed */
    // GPS receivers have a tendency to appear 'moving', even when sitting stationary
    // on your desk.  This filter is a 'speed' threshold which is used to force the 
    // reported speed to '0' if it falls below this value.  This value can be overridden
    // in the Servlet 'webapp.conf' file.
    public static double    MinimumReqSpeedKPH          = 2.0;
    
    /* Google Map key */
    public static String    GoogleMapKey                = "";

    /* Default time zone */
    private static TimeZone gmtTimeZone                 = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // common parameter keys (lookups are case insensitive) */
    private static String PARM_LATITUDE                 = "lat";        // [deg] Latitude
    private static String PARM_LONGITUDE                = "lng";        // [deg] Longitude
    private static String PARM_SPEEDMPH                 = "mph";        // [mph] Speed (1609/GeoPoint.KILOMETERS_PER_MILE = kph)
    private static String PARM_AZIMUTH                  = "dir";        // [deg] Azimuth from previous location
    private static String PARM_DISTANCE                 = "dis";        // [Miles] Distance (* 1609/GeoPoint.KILOMETERS_PER_MILE = km)
    private static String PARM_DATE                     = "dt";         // (new Date()).toString()
    private static String PARM_LOCMETH                  = "lm";         // [?] (info only)
    private static String PARM_PHONENUM                 = "pn";         // (?] UniqueID?
    private static String PARM_SESSION                  = "sid";        // [System.currentTimeMillis()] Session ID 
    private static String PARM_HACCURACY                = "acc";        // [feet] Session ID [/3.28 = meters]
    private static String PARM_VALIDFIX                 = "lv";         // [true|false]
    private static String PARM_INFO                     = "info";       // [?] (info only)
    private static String PARM_ZOOM                     = "zm";         // [Google Zoom]
    private static String PARM_HEIGHT                   = "h";          // [Google height]
    private static String PARM_WIDTH                    = "w";          // [Google width]
    private static String PARM_ICON                     = "pp";         // [Google pushpin icon]

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* static initializer */
    // Only initialized once (per JVM)
    static {

        /* initialize DBFactories */
        // should already have been called by 'RTConfigContextListener'
        DBConfig.servletInit(null);

        /* customized properties */
        MinimumReqSpeedKPH = RTConfig.getDouble(CONFIG_MIN_SPEED, MinimumReqSpeedKPH);
        GoogleMapKey       = RTConfig.getString(CONFIG_GOOGLE_KEY, GoogleMapKey);

    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long parseJavaDate(String date)
    {
        // Assumed format: "Sun Mar 01 15:53:42 PST 2009"
        String d[] = StringTools.split(date, ' ');
        if (d.length == 6) {
            String t[] = StringTools.split(d[3], ':');
            if (t.length == 3) {
                TimeZone tmz = DateTime.getTimeZone(d[4]);
                int MM = DateTime.getMonthIndex1(d[1], -1);
                int DD = StringTools.parseInt(d[2], -1);
                int YY = StringTools.parseInt(d[5], -1);
                int hh = StringTools.parseInt(t[0], -1);
                int mm = StringTools.parseInt(t[1], -1);
                int ss = StringTools.parseInt(t[2], -1);
                if ((MM > 0) && (DD > 0) && (YY >= 2008) && (hh >= 0) && (mm >= 0) && (ss >= 0)) {
                    DateTime fixDT = new DateTime(tmz, YY, MM, DD, hh, mm, ss);
                    return fixDT.getTimeSec();
                }
            }
        }
        Print.logWarn("Unable to parse Date: " + date);
        return DateTime.getCurrentTimeSec();
    }

    // ------------------------------------------------------------------------

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this.doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String  ipAddr     = request.getRemoteAddr();
        double  latitude   = AttributeTools.getRequestDouble( request, PARM_LATITUDE  , -999.0);
        double  longitude  = AttributeTools.getRequestDouble( request, PARM_LONGITUDE , -999.0);
        double  speedKPH   = AttributeTools.getRequestDouble( request, PARM_SPEEDMPH  ,    0.0) * (1609.0 / GeoPoint.KILOMETERS_PER_MILE);
        double  azimuth    = AttributeTools.getRequestDouble( request, PARM_AZIMUTH   ,    0.0);
        double  distanceKM = AttributeTools.getRequestDouble( request, PARM_DISTANCE  ,    0.0) * (1609.0 / GeoPoint.KILOMETERS_PER_MILE);
        String  javaDate   = AttributeTools.getRequestString( request, PARM_DATE      ,   null);
        String  locateMeth = AttributeTools.getRequestString( request, PARM_LOCMETH   ,   null);
        String  phoneNum   = AttributeTools.getRequestString( request, PARM_PHONENUM  ,   null); // only opportunity at a unique-id
        String  sessionID  = AttributeTools.getRequestString( request, PARM_SESSION   ,   null); // millisecond time
        double  horzAccM   = AttributeTools.getRequestDouble( request, PARM_HACCURACY ,    0.0) / 3.28;
        boolean isValid    = AttributeTools.getRequestBoolean(request, PARM_VALIDFIX  ,   true);
        String  extraInfo  = AttributeTools.getRequestString( request, PARM_INFO      ,   null);
        int     googleZoom = AttributeTools.getRequestInt(    request, PARM_ZOOM      ,      4);
        int     googleW    = AttributeTools.getRequestInt(    request, PARM_WIDTH     ,    150);
        int     googleH    = AttributeTools.getRequestInt(    request, PARM_HEIGHT    ,    200);
        String  googleIcon = AttributeTools.getRequestString( request, PARM_ICON      ,   null);
        int     statusCode = StatusCodes.STATUS_LOCATION;
        long    fixTime    = this.parseJavaDate(javaDate);
        double  headingDeg = azimuth;    // assume azimuth is heading
        double  altitudeM  = 0.0;

        /* URL */
        String URL = "[" + ipAddr + "] URL: " + request.getRequestURL() + " " + request.getQueryString();

        /* determine what to use for the unique ID */
        // Since a specific uniqueID was not specified in the data, we will try to determine
        // an adequate unique-id.  If the phone number is available, we're home free, however
        // some phones may not allow running Java applications to obtain the local phone number,
        // in which case we must look elsewhere.
        String UniqueIDCadtidates[] = new String[] {
            phoneNum,       // first try the phone number
            ipAddr,         // then try the ip address (will work iff a statis IP address is assigned)
            "default"       // fallback to "default"
        };

        /* unique id? */
        Device device = null;
        for (int u = 0; u < UniqueIDCadtidates.length; u++) {
            String uniqueID = UniqueIDPrefix + UniqueIDCadtidates[u];
            try {
                device = Transport.loadDeviceByUniqueID(uniqueID);
                if (device != null) {
                    break;
                }
            } catch (DBException dbe) {
                Data.logError(URL, "Exception getting Device: " + uniqueID + " [" + dbe + "]");
                this.plainTextResponse(response, RESPONSE_INTERNAL_ERROR);
                return;
            }
        }
        if (device == null) {
            Data.logWarn("UniqueID not found!: " + UniqueIDPrefix + UniqueIDCadtidates[0]); // <== display main key
            this.plainTextResponse(response, RESPONSE_INVALID_ID);
            return;
        }

        /* update actual device/account ids */
        String deviceID  = device.getDeviceID();
        String accountID = device.getAccountID();

        /* validate source IP address */
        // This may be used to prevent rogue hackers from spoofing data coming from the phone
        if (!device.getDataTransport().isValidIPAddress(ipAddr)) {
            // 'ipAddr' does not match allowable device IP addresses
            Data.logError(URL, "Invalid IP Address for device");
            this.plainTextResponse(response, RESPONSE_INVALID_IP);
            return;
        }
        
        /* display URL (debug) */
        Data.logInfo(URL);

        /* reject invalid GPS fixes? */
        if (!isValid && (statusCode == StatusCodes.STATUS_LOCATION)) {
            // ignore invalid GPS fixes that have a simple 'STATUS_LOCATION' status code
            Data.logWarn("Ignoring event with invalid latitude/longitude");
            this.plainTextResponse(response, RESPONSE_INVALID_FIX);
            return;
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

        /* create new event record */
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixTime, statusCode);
        EventData evdb = evKey.getDBRecord();
        evdb.setLatitude(latitude);
        evdb.setLongitude(longitude);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(headingDeg);
        evdb.setAltitude(altitudeM);
        evdb.setHorzAccuracy(horzAccM);
        evdb.setDistanceKM(distanceKM);
        evdb.setOdometerKM(distanceKM);

        /* insert event */
        // this will display an error if it was unable to store the event
        if (device.insertEventData(evdb)) {
            Data.logInfo("Event inserted: "+device.getAccountID()+"/"+device.getDeviceID() +
                " - "+evdb.getGeoPoint());
        }

        /* return Google Map */
        GoogleStaticMap gsm = new GoogleStaticMap(googleW, googleH, GoogleMapKey);
        gsm.setZoom(googleZoom);
        gsm.addPushpin(new GeoPoint(latitude,longitude), googleIcon);
        byte pngMap[] = gsm.getMap();
        if (!ListTools.isEmpty(pngMap)) {
            OutputStream output = null;
            try {
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PNG());
                output = response.getOutputStream();
                output.write(pngMap);
            } catch (IOException ioe) {
                Print.logException("Error writing Map", ioe);
            } finally {
                if (output != null) { try { output.close(); } catch (Throwable th) {/*ignore*/} }
            }
        } else {
            this.plainTextResponse(response, RESPONSE_MAP_UNAVAIL);
        }

    }

    // ------------------------------------------------------------------------

    /* send plain text response */
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
