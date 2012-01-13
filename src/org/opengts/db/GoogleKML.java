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
//  - http://www.bowdoin.edu/~disrael/google-earth-icons/
// ----------------------------------------------------------------------------
// Change History:
//  2008/12/01  Martin D. Flynn
//     -Extracted from EventUtil.java
//  2009/08/07  Martin D. Flynn
//     -Updated Icon specification
//  2009/12/16  Martin D. Flynn
//     -Convert odometer KM to account display units.
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.geocoder.*;
import org.opengts.db.tables.*;

public class GoogleKML
{
    
    // ------------------------------------------------------------------------

    public static final String PROP_GoogleKML_lastPushpinID = "googleKML.lastPushpinID";
    
    // ------------------------------------------------------------------------

    private static double MPH(double kph) { return kph * GeoPoint.MILES_PER_KILOMETER; }

    // ------------------------------------------------------------------------

    private static final String STYLE_LAST              = "StyleLast";
    private static final String STYLE_DEFAULT           = "StyleDefault";
    private static final String STYLE_MOVING            = "StyleMoving";
    private static final String STYLE_MOVING_LAST       = "StyleMovingLast";
    private static final String STYLE_STOPPED           = "StyleStopped";
    private static final String STYLE_STOPPED_LAST      = "StyleStoppedLast";
    private static final String STYLE_SLOW              = "StyleSlow";
    private static final String STYLE_SLOW_LAST         = "StyleSlowLast";

    // ------------------------------------------------------------------------
    
    // http://maps.google.com/mapfiles/kml/pal3/icon21.png
    // http://gmapicons.googlepages.com/
    // http://jyotirmaya.blogspot.com/2008/03/google-map-files-kml-icon.html
    private static final String     GOOGLE_ICON_URL     = "http://labs.google.com/ridefinder/images/";
    
    private static final GooglePP   PUSHPIN_BLACK       = new GooglePP("black"      , GOOGLE_ICON_URL, "mm_20_black.png" ,0.8,6,20);
    private static final GooglePP   PUSHPIN_BROWN       = new GooglePP("brown"      , GOOGLE_ICON_URL, "mm_20_brown.png" ,0.8,6,20);
    private static final GooglePP   PUSHPIN_RED         = new GooglePP("red"        , GOOGLE_ICON_URL, "mm_20_red.png"   ,0.8,6,20);
    private static final GooglePP   PUSHPIN_ORANGE      = new GooglePP("orange"     , GOOGLE_ICON_URL, "mm_20_orange.png",0.8,6,20);
    private static final GooglePP   PUSHPIN_YELLOW      = new GooglePP("yellow"     , GOOGLE_ICON_URL, "mm_20_yellow.png",0.8,6,20);
    private static final GooglePP   PUSHPIN_GREEN       = new GooglePP("green"      , GOOGLE_ICON_URL, "mm_20_green.png" ,0.8,6,20);
    private static final GooglePP   PUSHPIN_BLUE        = new GooglePP("blue"       , GOOGLE_ICON_URL, "mm_20_blue.png"  ,0.8,6,20);
    private static final GooglePP   PUSHPIN_PURPLE      = new GooglePP("purple"     , GOOGLE_ICON_URL, "mm_20_purple.png",0.8,6,20);
    private static final GooglePP   PUSHPIN_GRAY        = new GooglePP("gray"       , GOOGLE_ICON_URL, "mm_20_gray.png"  ,0.8,6,20);
    private static final GooglePP   PUSHPIN_WHITE       = new GooglePP("white"      , GOOGLE_ICON_URL, "mm_20_white.png" ,0.8,6,20);
    
    private static final GooglePP   PUSHPIN_BLACK_LAST  = new GooglePP("black.last" , GOOGLE_ICON_URL, "mm_20_black.png" ,0.8,6,20);
    private static final GooglePP   PUSHPIN_BROWN_LAST  = new GooglePP("brown.last" , GOOGLE_ICON_URL, "mm_20_brown.png" ,0.8,6,20);
    private static final GooglePP   PUSHPIN_RED_LAST    = new GooglePP("red.last"   , GOOGLE_ICON_URL, "mm_20_red.png"   ,0.8,6,20);
    private static final GooglePP   PUSHPIN_ORANGE_LAST = new GooglePP("orange.last", GOOGLE_ICON_URL, "mm_20_orange.png",0.8,6,20);
    private static final GooglePP   PUSHPIN_YELLOW_LAST = new GooglePP("yellow.last", GOOGLE_ICON_URL, "mm_20_yellow.png",0.8,6,20);
    private static final GooglePP   PUSHPIN_GREEN_LAST  = new GooglePP("green.last" , GOOGLE_ICON_URL, "mm_20_green.png" ,0.8,6,20);
    private static final GooglePP   PUSHPIN_BLUE_LAST   = new GooglePP("blue.last"  , GOOGLE_ICON_URL, "mm_20_blue.png"  ,0.8,6,20);
    private static final GooglePP   PUSHPIN_PURPLE_LAST = new GooglePP("purple.last", GOOGLE_ICON_URL, "mm_20_purple.png",0.8,6,20);
    private static final GooglePP   PUSHPIN_GRAY_LAST   = new GooglePP("gray.last"  , GOOGLE_ICON_URL, "mm_20_gray.png"  ,0.8,6,20);
    private static final GooglePP   PUSHPIN_WHITE_LAST  = new GooglePP("white.last" , GOOGLE_ICON_URL, "mm_20_white.png" ,0.8,6,20);
    
    private static final GooglePP   GooglePushpins[] = new GooglePP[] {
        PUSHPIN_BLACK       ,
        PUSHPIN_BROWN       ,
        PUSHPIN_RED         ,
        PUSHPIN_ORANGE      ,
        PUSHPIN_YELLOW      ,
        PUSHPIN_GREEN       ,
        PUSHPIN_BLUE        ,
        PUSHPIN_PURPLE      ,
        PUSHPIN_GRAY        ,
        PUSHPIN_WHITE       ,
        PUSHPIN_BLACK_LAST  ,
        PUSHPIN_BROWN_LAST  ,
        PUSHPIN_RED_LAST    ,
        PUSHPIN_ORANGE_LAST ,
        PUSHPIN_YELLOW_LAST ,
        PUSHPIN_GREEN_LAST  ,
        PUSHPIN_BLUE_LAST   ,
        PUSHPIN_PURPLE_LAST ,
        PUSHPIN_GRAY_LAST   ,
        PUSHPIN_WHITE_LAST  ,
    };
    
    private static final Map<String,GooglePP> GooglePushpinMap = new HashMap<String,GooglePP>();
    static {
        for (GooglePP gpp : GooglePushpins) {
            GooglePushpinMap.put(gpp.getName(), gpp);
        }
    };
    
    public static GooglePP getGooglePushpin(String name)
    {
        GooglePP gpp = GooglePushpinMap.get(name);
        return (gpp != null)? gpp : PUSHPIN_GREEN;
    }

    private static class GooglePP
        implements StringTools.KeyValueMap
    {
        private String name  = null;
        private String url   = null;
        private double scale = 1.0;
        private int    xOfs  = 0;
        private int    yOfs  = 0;
        public GooglePP(String name, String baseURL, String url, double scale, int xOfs, int yOfs) {
            this.name  = name;
            this.url   = baseURL + url;
            this.scale = scale;
            this.xOfs  = xOfs;
            this.yOfs  = yOfs;
        }
        public String getName() {
            return this.name;
        }
        public String getURL() {
            return !StringTools.isBlank(this.url)? this.url : GOOGLE_ICON_URL;
        }
        public double getScale() {
            return this.scale;
        }
        public int getOffset_X() {
            return this.xOfs;
        }
        public int getOffset_Y() {
            return this.yOfs;
        }
        public String getKeyValue(String key, String arg, String dft) {
            if (key.equalsIgnoreCase("iconOfsX"))  { return String.valueOf(this.getOffset_X()); }
            if (key.equalsIgnoreCase("iconOfsY"))  { return String.valueOf(this.getOffset_Y()); }
            if (key.equalsIgnoreCase("iconScale")) { return String.valueOf(this.getScale()); }
            if (key.equalsIgnoreCase("iconURL"))   { return this.getURL(); }
            return dft;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String XML_Header = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<kml xmlns=\"http://earth.google.com/kml/2.0\">\n" +
        "<Document>\n" +
        "";
    
    private static final String XML_Footer = 
        "</Document>\n" +
        "</kml>\n" +
        "";

    private static final String XML_RouteStyle = 
        "<Style id=\"StyleRoute\">\n" +
        "  <LineStyle id=\"StyleRouteLine\">\n" +
        "    <color>880000FF</color>\n" +
        "    <width>4</width>\n" +
        "  </LineStyle>\n" +
        "  <PolyStyle id=\"StyleRoutePoly\">\n" +
        "    <color>880000FF</color>\n" +
        "  </PolyStyle>\n" +
        "</Style>\n" +
        "";

    private static final String XML_IconStyle = 
        "<Style id=\"${styleName}\">\n" +
        "  <BalloonStyle id=\"${styleName}Balloon\">\n" +     // DefaultBalloonStyle
        "    <text><![CDATA[<b>$[name]</b><br/><br/>$[description]]]></text>\n" +
        "  </BalloonStyle>\n" +
        "  <IconStyle id=\"${styleName}Icon\">\n" +           // DefaultIconStyle
        "    <color>FFFFFFFF</color>\n" +
        "    <scale>${iconScale}</scale>\n" +
        "    <Icon><href>${iconURL}</href></Icon>\n" +
        "    <hotSpot x=\"${iconOfsX}\" y=\"${iconOfsY}\" xunits=\"pixels\" yunits=\"insetPixels\"/>\n" +
        "  </IconStyle>\n" +
        "  <LabelStyle id=\"${styleName}Label\">\n" +         // DefaultLabelStyle
        "    <color>FFFFFFFF</color>\n" +
        "    <scale>0.80</scale>\n" +
        "  </LabelStyle>\n" +
      //"  <LineStyle id=\"${styleName}Line\">\n" +           // DefaultLineStyle
      //"    <color>FF0000FF</color>\n" +
      //"    <width>15</width>\n" +
      //"  </LineStyle>\n" +
      //"  <PolyStyle id=\"${styleName}Poly\">\n" +           // DefaultPolyStyle
      //"    <color>7F7FAAAA</color>\n" +
      //"    <colorMode>normal</colorMode>\n" +
      //"  </PolyStyle>\n" +
        "</Style>\n" +
        "";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static GoogleKML instance = null;
    public static GoogleKML getInstance()
    {
        if (GoogleKML.instance == null) {
            GoogleKML.instance = new GoogleKML();
        }
        return GoogleKML.instance;
    }
    
    static {
        GoogleKML.getInstance();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private GoogleKML() 
    {
        super();
    }

    // ------------------------------------------------------------------------
    
    private String _createStyle(final String name, final GooglePP icon)
    {
        return StringTools.replaceKeys(XML_IconStyle, new StringTools.KeyValueMap() {
            public String getKeyValue(String key, String arg, String dft) {
                if (key.equalsIgnoreCase("styleName")) { return name; }
                return icon.getKeyValue(key, arg, dft);
            }
        });
    }

    // ------------------------------------------------------------------------

    private void _writeRoute(PrintWriter out, 
        I18N i18n, BasicPrivateLabel privLabel, 
        String deviceDesc, java.util.List<String> route, 
        int indent)
        throws IOException
    {
        String tab = StringTools.replicateString(" ", indent);
        out.write(tab + "<Placemark>\n");
        out.write(tab + "   <name>Route</name>\n");
        out.write(tab + "   <description><![CDATA["+deviceDesc+"]]></description>\n");
        out.write(tab + "   <styleUrl>#StyleRoute</styleUrl>\n");
        out.write(tab + "   <LineString>\n");
        out.write(tab + "      <!-- <extrude>1</extrude> -->\n");
        out.write(tab + "      <!-- <tessellate>1</tessellate> -->\n");
        out.write(tab + "      <!-- <altitudeMode>absolute</altitudeMode> -->\n");
        out.write(tab + "      <coordinates>\n");
        for (String c : route) {
        out.write(tab + "         " + c + "\n");
        }
        out.write(tab + "      </coordinates>\n");
        out.write(tab + "   </LineString>\n");
        out.write(tab + "</Placemark>\n");
    }

    // ------------------------------------------------------------------------

    private String _writePlacemark(PrintWriter out, 
        I18N i18n, BasicPrivateLabel privLabel, EventData ev, 
        boolean isLast, boolean useLastPP,
        int indent)
        throws IOException
    {
        Account account  = ev.getAccount();
        Device  device   = ev.getDevice();
        TimeZone tz      = account.getTimeZone(null);
        String tab       = StringTools.replicateString(" ", indent);
        Locale locale    = i18n.getLocale();

        String datStr    = ev.getTimestampString(privLabel);
        int    code      = ev.getStatusCode();
        String codStr    = ev.getStatusCodeDescription(privLabel);
        GeoPoint gp      = ev.getGeoPoint();
        String deviceID  = ev.getDeviceID();
        String address   = ev.getAddress();
        String latStr    = gp.getLatitudeString( GeoPoint.SFORMAT_DEC_5,null);
        String lonStr    = gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null);
        
        /* odometer */
        double odomKM    = ev.getOdometerKM() + device.getOdometerOffsetKM(); // ok
        String odomStr   = account.getDistanceString(odomKM, true, locale);

        /* speed/heading */
        double speedKPH  = ev.getSpeedKPH();
        String spdStr    = account.getSpeedString(speedKPH,"#0.0",true,locale);
        double heading   = ev.getHeading();
        String headStr   = GeoPoint.GetHeadingString(heading,locale);

        /* altitude */
        double altM      = ev.getAltitude();
        String altStr    = Account.getDistanceUnits(account).equals(Account.DistanceUnits.MILES)?
            (Math.round(altM * GeoPoint.FEET_PER_METER) + " Feet") :
            (Math.round(altM) + " Meters");

        out.write(tab + "<Placemark>\n");
        out.write(tab + "  <name>" + deviceID + "</name>\n");
        out.write(tab + "  <description><![CDATA[");
        out.write(           "Status : <strong>" + codStr  + "</strong><br>");
        out.write(           "Date : <strong>" + datStr  + "</strong><br>");
        if (!StringTools.isBlank(address)) {
        out.write(           "Address : <strong>" + address + "</strong><br>");
        }
        out.write(           "Speed : <strong>" + spdStr + " " + headStr + "</strong><br>");
        out.write(           "GPS : <strong>" + latStr + " " + lonStr + "</strong><br>");
        out.write(           "Altitude : <strong>" + altStr  + "</strong><br>");
        if (odomKM > 0.0) {
        out.write(           "Odometer : <strong>" + odomStr  + "</strong>");
        }
        out.write(           "]]></description>\n");
        
        // Pushpin
        String style = STYLE_DEFAULT;
        if (isLast && useLastPP) {
            style = STYLE_LAST;
        } else {
            switch (code) {
                case StatusCodes.STATUS_MOTION_START:
                case StatusCodes.STATUS_MOTION_STOP:
                case StatusCodes.STATUS_MOTION_IN_MOTION:
                case StatusCodes.STATUS_MOTION_MOVING:
                    if (MPH(speedKPH) > 20.0) {
                        style = isLast? STYLE_MOVING_LAST  : STYLE_MOVING;
                    } else
                    if (MPH(speedKPH) > 5.0) {
                        style = isLast? STYLE_SLOW_LAST    : STYLE_SLOW;
                    } else {
                        style = isLast? STYLE_STOPPED_LAST : STYLE_STOPPED;
                    }
                    break;
                default:
                    style = STYLE_DEFAULT;
                    break;
            }
        }
        out.write(tab + "  <styleUrl>#" + style + "</styleUrl>\n");
        
        // GPS location
        String gpsPoint = lonStr + "," + latStr + "," + Math.round(altM);
        out.write(tab + "  <Point><coordinates>" + gpsPoint + "</coordinates></Point>\n");
        
        out.write(tab + "</Placemark>\n");
        
        /* return coordinate (for route) */
        return gpsPoint;
        
    }

    // ------------------------------------------------------------------------

    public boolean writeEvents(PrintWriter out, 
        Account account, Collection<Device> devList, 
        BasicPrivateLabel privLabel)
        throws IOException
    {
        // This does assume that all events belong to the same "Account"

        /* account required */
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();
        TimeZone tz = account.getTimeZone(null);

        /* Localization */
        I18N i18n = (privLabel != null)? privLabel.getI18N(GoogleKML.class) : I18N.getI18N(GoogleKML.class,null);

        /* header */
        out.write(XML_Header);
        
        /* Route Style */
        out.write(XML_RouteStyle);

        /* standard styles */
        out.write(_createStyle(STYLE_DEFAULT      , getGooglePushpin("green")));
        out.write(_createStyle(STYLE_MOVING       , getGooglePushpin("green")));
        out.write(_createStyle(STYLE_MOVING_LAST  , getGooglePushpin("green.last")));
        out.write(_createStyle(STYLE_STOPPED      , getGooglePushpin("red")));
        out.write(_createStyle(STYLE_STOPPED_LAST , getGooglePushpin("red.last")));
        out.write(_createStyle(STYLE_SLOW         , getGooglePushpin("yellow")));
        out.write(_createStyle(STYLE_SLOW_LAST    , getGooglePushpin("yellow.last")));

        /* last icon */
        boolean useLastPP = false;
        String lastPP = (privLabel != null)? privLabel.getStringProperty(PROP_GoogleKML_lastPushpinID,null) : null;
        if (!StringTools.isBlank(lastPP) && GooglePushpinMap.containsKey(lastPP)) {
            out.write(_createStyle(STYLE_LAST, getGooglePushpin(lastPP)));
            useLastPP = true;
        }

        /* placemarks */
        if (!ListTools.isEmpty(devList)) {
            java.util.List<String> routeList = new Vector<String>();
            for (Device dev : devList) {
                String deviceID = dev.getDeviceID();
                routeList.clear();
    
                /* check account ID */
                if (!dev.getAccountID().equals(accountID)) {
                    // mismatched AccountID
                    continue;
                }
    
                /* write event placemarks */
                EventData evList[] = dev.getSavedRangeEvents();
                if (!ListTools.isEmpty(evList)) {
                    for (int e = 0; e < evList.length; e++) {
                        EventData ev = evList[e];
                        boolean isLast = (e == (evList.length - 1));
        
                        /* same account? */
                        if (!ev.getAccountID().equals(accountID)) {
                            // mismatched AccountID
                            continue;
                        }
                        ev.setAccount(account); // redundant
        
                        /* write marker */
                        String pc = this._writePlacemark(out, i18n, privLabel, ev, isLast, useLastPP, 4);
                        routeList.add(pc);
        
                    }
                }
    
                /* draw device route */
                if (routeList.size() > 1) {
                    this._writeRoute(out, i18n, privLabel, deviceID, routeList, 4);
                }
    
            }
        }

        /* trailer */
        out.write(XML_Footer);

        /* flush (output may not occur until the PrintWriter is flushed) */
        out.flush();
        return true;
        
    }

    // ------------------------------------------------------------------------

}
