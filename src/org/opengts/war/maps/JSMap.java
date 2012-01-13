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
// Required funtions defined by 'jsmap.js':
//   new JSMap(String mapID)
//   JSClearLayers()
//   JSDrawPushpins(JSMapPushpin pushPins[], recenter)
//   JSDrawRoute(JSMapPoint points[])
//   JSDrawGeozone(int type, boolean editable, double radius, JSMapPoint points[], int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin)
//   JSUnload() 
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/09/19  Martin D. Flynn
//     -Added warning message text used when MAX_PUSH_PINS has been reached.
//     -Set JS var 'jsvLastEventYMD' to null when there is no last event date.
//  2008/12/01  Martin D. Flynn
//     -Internationalized info-balloon header text.
//  2009/01/01  Martin D. Flynn
//     -Added option for showing altitude in map info bubble.
//  2009/09/23  Martin D. Flynn
//     -Added support for customizing the Geozone map width/height
//  2010/04/11  Martin D. Flynn
//     -Added work-around to not impose 'fleet' maxPushpin limit on reports (see 'isReport')
//  2011/10/03  Martin D. Flynn
//     -Fixed default "Address" title text (was blank/null)
// ----------------------------------------------------------------------------
package org.opengts.war.maps;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.war.tools.*;

public class JSMap
    extends MapProviderAdapter
{

    // ------------------------------------------------------------------------

    public  static final String DEFAULT_MAP_ID          = "jsmap";

    private static final int    DEFAULT_ZOOM            = 14;   // when no points are displayed
    private static final int    PUSHPIN_ZOOM            = 8;    // when points are displayed

    private static final String PROP_MAP_ID             = "jsmap.mapID";
    private static final String PROP_MAP_LOADING        = "jsmap.showMapLoading";
    private static final String PROP_MAP_LOADING_IMAGE  = "jsmap.showMapLoading.image";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    /* JSMap instance */ 
    public JSMap(String name, String key) 
    {
        super(name, key); 
    }

    // ------------------------------------------------------------------------ 
 
    /* get map element ID */
    public String getMapID()
    {
        RTProperties rtp = this.getProperties();
        return rtp.getString(PROP_MAP_ID, DEFAULT_MAP_ID);
    }

    // ------------------------------------------------------------------------

    /**
    *** List of property keys which are not to be included in the 'defined propertes' section
    **/
    private static final Object SKIP_PROPS[] = new Object[] {
        PROP_javascript,
        PROP_zone_map_width,
        PROP_zone_map_height,
        PROP_map_width,
        PROP_map_height,
        PROP_map_fillFrame,
        PROP_map_pushpins,
        PROP_map_routeLine,
        PROP_map_routeLine_color,
        PROP_map_routeLine_arrows,
        PROP_scrollWheelZoom,
        PROP_map_view,
        PROP_iconSelector,
        PROP_iconSelector_legend,
        PROP_iconSel_fleet,
        PROP_iconSel_fleet_legend,
        PROP_pushpin_zoom,
        PROP_default_zoom,
        PROP_default_latitude,
        PROP_default_longitude,
    };

    /**
    *** Returns true if the specified key is not to be included in the 'defined propertes' section
    *** @param key  The property key to test
    *** @return true to omit the specified property from the 'defined properties' section
    **/
    protected boolean _skipPropKey(Object key)
    {
        String ks = key.toString();
        for (int i = 0; i < SKIP_PROPS.length; i++) {
            String propKey[] = (String[])SKIP_PROPS[i];
            if (ListTools.contains(propKey,ks)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // write JSMap Style
    
    public void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        WebPageAdaptor.writeCssLink(out, reqState, "JSMap.css", null);
    }

    // ------------------------------------------------------------------------

    /* write mapping support JS to stream */ 
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    {
        // This var initilizations must not use any functions defined in 'jsmap.js'
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(JSMap.class);
        Locale       locale      = reqState.getLocale();
        GeoPoint     dftCenter   = this.getDefaultCenter(null);
        boolean      isFleet     = reqState.isFleet();
        Account      account     = reqState.getCurrentAccount();
        long         maxPushpins = this.getMaxPushpins(reqState);
        out.write("// --- Map support Javascript ["+this.getName()+"]\n");
        JavaScriptTools.writeJSVar(out, "MAP_PROVIDER_NAME", this.getName());

        /* properties */
        boolean wrotePropHeader = false;
        RTProperties rtp = this.getProperties();
        for (Iterator<?> i = rtp.keyIterator(); i.hasNext();) {
            Object key = i.next();
            if (!this._skipPropKey(key)) {
                if (!wrotePropHeader) {
                    //out.write("\n");
                    out.write("// Defined properties\n");
                    wrotePropHeader = true;
                }
                String val[] = StringTools.parseString(rtp.getProperty(key,"").toString(), '\n');
                String propVar = "PROP_" + key.toString().replace('.','_').replace('-','_');
                if (val.length == 1) {
                    if (StringTools.isDouble(val[0],true) || 
                        StringTools.isLong(val[0],true)   || 
                        StringTools.isBoolean(val[0],true)  ) {
                        JavaScriptTools.writeJSVar(out, propVar, val[0], false);
                    } else {
                        JavaScriptTools.writeJSVar(out, propVar, val[0]);
                    }
                } else
                if (val.length > 1) {
                    JavaScriptTools.writeJSVar(out, propVar, StringTools.join(val,"\\n"));
                }
            }
        }

        /* speed units */
        Account.SpeedUnits speedUnits = reqState.getSpeedUnits();
        boolean speedIsKph  = speedUnits.equals(Account.SpeedUnits.KPH);
        double altUnitsMult = speedIsKph? 1.0 : GeoPoint.FEET_PER_METER;
        String altUnitsName = speedIsKph? i18n.getString("JSMap.altitude.meters","Meters") : i18n.getString("JSMap.altitude.feet","Feet");

        /* constants (these do not change during the user session) */
        out.write("// Element IDs\n");
        JavaScriptTools.writeJSVar(out, "MAP_ID"                , this.getMapID());
        JavaScriptTools.writeJSVar(out, "ID_DETAIL_TABLE"       , ID_DETAIL_TABLE);
        JavaScriptTools.writeJSVar(out, "ID_DETAIL_CONTROL"     , ID_DETAIL_CONTROL);
        JavaScriptTools.writeJSVar(out, "ID_LAT_LON_DISPLAY"    , ID_LAT_LON_DISPLAY);
        JavaScriptTools.writeJSVar(out, "ID_DISTANCE_DISPLAY"   , ID_DISTANCE_DISPLAY);
        JavaScriptTools.writeJSVar(out, "ID_LATEST_EVENT_DATE"  , ID_LATEST_EVENT_DATE);
        JavaScriptTools.writeJSVar(out, "ID_LATEST_EVENT_TIME"  , ID_LATEST_EVENT_TIME);
        JavaScriptTools.writeJSVar(out, "ID_LATEST_EVENT_TMZ"   , ID_LATEST_EVENT_TMZ);
        JavaScriptTools.writeJSVar(out, "ID_LATEST_BATTERY"     , ID_LATEST_BATTERY);
        JavaScriptTools.writeJSVar(out, "ID_MESSAGE_TEXT"       , ID_MESSAGE_TEXT);
        out.write("// Geozone IDs\n");
        JavaScriptTools.writeJSVar(out, "ID_ZONE_LATITUDE_"     , ID_ZONE_LATITUDE_);
        JavaScriptTools.writeJSVar(out, "ID_ZONE_LONGITUDE_"    , ID_ZONE_LONGITUDE_);
        JavaScriptTools.writeJSVar(out, "ID_ZONE_RADIUS_M"      , ID_ZONE_RADIUS_M);
        out.write("// Session constants\n");
        JavaScriptTools.writeJSVar(out, "PUSHPINS_SHOW"         , rtp.getBoolean(PROP_map_pushpins,true));
        JavaScriptTools.writeJSVar(out, "MAX_PUSH_PINS"         , maxPushpins);
        JavaScriptTools.writeJSVar(out, "MAP_WIDTH"             , this.getDimension().getWidth());
        JavaScriptTools.writeJSVar(out, "MAP_HEIGHT"            , this.getDimension().getHeight());
        JavaScriptTools.writeJSVar(out, "IS_FLEET"              , isFleet);
        JavaScriptTools.writeJSVar(out, "SHOW_SAT_COUNT"        , rtp.getBoolean(PROP_detail_showSatCount,false));
        JavaScriptTools.writeJSVar(out, "COMBINE_SPEED_HEAD"    , rtp.getBoolean(PROP_combineSpeedHeading,true));
        JavaScriptTools.writeJSVar(out, "SHOW_ALTITUDE"         , rtp.getBoolean(PROP_info_showAltitude,false));
        JavaScriptTools.writeJSVar(out, "SHOW_ADDR"             , reqState.getShowAddress());
        JavaScriptTools.writeJSVar(out, "INCL_BLANK_ADDR"       , false);
        JavaScriptTools.writeJSVar(out, "SHOW_OPT_FIELDS"       , true);
        JavaScriptTools.writeJSVar(out, "LATLON_FORMAT"         , Account.getLatLonFormat(account).getIntValue());
        JavaScriptTools.writeJSVar(out, "DISTANCE_KM_MULT"      , reqState.getDistanceUnits().getMultiplier());
        JavaScriptTools.writeJSVar(out, "SPEED_KPH_MULT"        , speedUnits.getMultiplier());
        JavaScriptTools.writeJSVar(out, "SPEED_UNITS"           , speedUnits.toString(locale));
        JavaScriptTools.writeJSVar(out, "ALTITUDE_METERS_MULT"  , altUnitsMult);
        JavaScriptTools.writeJSVar(out, "ALTITUDE_UNITS"        , altUnitsName);
        JavaScriptTools.writeJSVar(out, "TIME_ZONE"             , reqState.getTimeZoneString(null)); // long
        JavaScriptTools.writeJSVar(out, "DEFAULT_CENTER"        , "{ lat:" + dftCenter.getLatitude() + ", lon:" + dftCenter.getLongitude() + " }", false);
        JavaScriptTools.writeJSVar(out, "DEFAULT_ZOOM"          , this.getDefaultZoom(JSMap.DEFAULT_ZOOM,false));
        JavaScriptTools.writeJSVar(out, "PUSHPIN_ZOOM"          , this.getDefaultZoom(JSMap.PUSHPIN_ZOOM,true));
        JavaScriptTools.writeJSVar(out, "MAP_AUTHORIZATION"     , this.getAuthorization());
        JavaScriptTools.writeJSVar(out, "SCROLL_WHEEL_ZOOM"     , rtp.getBoolean(PROP_scrollWheelZoom,false));
        JavaScriptTools.writeJSVar(out, "DEFAULT_VIEW"          , rtp.getString(PROP_map_view,"").toLowerCase());
        JavaScriptTools.writeJSVar(out, "ROUTE_LINE_SHOW"       , rtp.getBoolean(PROP_map_routeLine,true));
        JavaScriptTools.writeJSVar(out, "ROUTE_LINE_COLOR"      , rtp.getString(PROP_map_routeLine_color,"#FF2222"));
        JavaScriptTools.writeJSVar(out, "ROUTE_LINE_ARROWS"     , rtp.getBoolean(PROP_map_routeLine_arrows,false));
        JavaScriptTools.writeJSVar(out, "REPLAY_INTERVAL"       , this.getReplayInterval());
        JavaScriptTools.writeJSVar(out, "REPLAY_SINGLE"         , this.getReplaySinglePushpin());

        /* address title */
        String adrTitles[] = reqState.getAddressTitles();
        String adrTitle    = ListTools.itemAt(adrTitles,0,null);

        /* device title */
        String devTitles[] = reqState.getDeviceTitles();
        String devTitle    = ListTools.itemAt(devTitles,0,null);

        /* labels */
        out.write("// Localized Text/Labels\n");
        JavaScriptTools.writeJSVar(out, "HEADING", "new Array(" +
            "\"" + GeoPoint.CompassHeading.N .toString(locale) + "\"," +
            "\"" + GeoPoint.CompassHeading.NE.toString(locale) + "\"," +
            "\"" + GeoPoint.CompassHeading.E .toString(locale) + "\"," +
            "\"" + GeoPoint.CompassHeading.SE.toString(locale) + "\"," +
            "\"" + GeoPoint.CompassHeading.S .toString(locale) + "\"," +
            "\"" + GeoPoint.CompassHeading.SW.toString(locale) + "\"," +
            "\"" + GeoPoint.CompassHeading.W .toString(locale) + "\"," +
            "\"" + GeoPoint.CompassHeading.NW.toString(locale) + "\")", false);
        JavaScriptTools.writeJSVar(out, "TEXT_INFO_DATE"            , i18n.getString("JSMap.info.date"      , "Date"));
        JavaScriptTools.writeJSVar(out, "TEXT_INFO_GPS"             , i18n.getString("JSMap.info.gps"       , "GPS"));
        JavaScriptTools.writeJSVar(out, "TEXT_INFO_SATS"            , i18n.getString("JSMap.info.sats"      , "#Sats"));
        JavaScriptTools.writeJSVar(out, "TEXT_INFO_SPEED"           , i18n.getString("JSMap.info.speed"     , "Speed"));
        JavaScriptTools.writeJSVar(out, "TEXT_INFO_HEADING"         , GeoPoint.GetHeadingTitle(locale));
        JavaScriptTools.writeJSVar(out, "TEXT_INFO_ALTITUDE"        , i18n.getString("JSMap.info.altitude"  , "Altitude"));
        JavaScriptTools.writeJSVar(out, "TEXT_INFO_ADDR"            , !StringTools.isBlank(adrTitle)?adrTitle:i18n.getString("JSMap.info.address", "Address"));
        JavaScriptTools.writeJSVar(out, "TEXT_DEVICE"               , !StringTools.isBlank(devTitle)?devTitle:i18n.getString("JSMap.device"      , "Device"));
        JavaScriptTools.writeJSVar(out, "TEXT_DATE"                 , i18n.getString("JSMap.dateTime"       , "Date/Time"));
        JavaScriptTools.writeJSVar(out, "TEXT_CODE"                 , i18n.getString("JSMap.code"           , "Status"));
        JavaScriptTools.writeJSVar(out, "TEXT_LATLON"               , i18n.getString("JSMap.latLon"         , "Lat/Lon"));
        JavaScriptTools.writeJSVar(out, "TEXT_SATCOUNT"             , i18n.getString("JSMap.satCount"       , "#Sats"));
        JavaScriptTools.writeJSVar(out, "TEXT_ADDR"                 , !StringTools.isBlank(adrTitle)?adrTitle:i18n.getString("JSMap.address"     , "Address"));
        JavaScriptTools.writeJSVar(out, "TEXT_SPEED"                , reqState.getSpeedUnits().toString(locale));
        JavaScriptTools.writeJSVar(out, "TEXT_HEADING"              , i18n.getString("JSMap.heading"        , "Heading"));
        JavaScriptTools.writeJSVar(out, "TEXT_DISTANCE"             , reqState.getDistanceUnits().toString(locale));
        JavaScriptTools.writeJSVar(out, "TEXT_TIMEOUT"              , i18n.getString("JSMap.sessionTimeout" , "Your session has timed-out.\nPlease login ..."));
        JavaScriptTools.writeJSVar(out, "TEXT_PING_OK"              , i18n.getString("JSMap.pingDevice.ok"  , "A command request has been sent.\nThe {0} should respond shortly ...",devTitles));
        JavaScriptTools.writeJSVar(out, "TEXT_PING_ERROR"           , i18n.getString("JSMap.pingDevice.err" , "The command request failed.\nThe {0} may not support this feature ...",devTitles));
        JavaScriptTools.writeJSVar(out, "TEXT_MAXPUSHPINS_ALERT"    , i18n.getString("JSMap.maxPushpins.err", 
            "The maximum number of allowed pushpins has been exceeded.\n" + 
            " [max={0}] Not all pushpins may be displayed on this map.\n" +
            "Adjust the 'From' time to see remaining pushpins",
            String.valueOf(maxPushpins)));
        JavaScriptTools.writeJSVar(out, "TEXT_MAXPUSHPINS_MSG"      , i18n.getString("JSMap.maxPushpins.msg",
            "Only partial data displayed.  The maximum allowed pushpins has been reached.<BR>" + 
            "Adjust the Date/Time range accordingly to view the remaining pushpins."));
        JavaScriptTools.writeJSVar(out, "TEXT_UNAVAILABLE"          , i18n.getString("JSMap.unavailable","unavailable"));
        JavaScriptTools.writeJSVar(out, "TEXT_showLocationDetails"  , i18n.getString("JSMap.showLocationDetails","Show Location Details"));
        JavaScriptTools.writeJSVar(out, "TEXT_hideLocationDetails"  , i18n.getString("JSMap.hideLocationDetails","Hide Location Details"));

        /* map "Loading ..." */
        JavaScriptTools.writeJSVar(out, "TEXT_LOADING_MAP_POINTS"   , (rtp.getBoolean(PROP_MAP_LOADING,false)? i18n.getString("JSMap.loadingMapPoints","Loading Map Points ...") : null));
        JavaScriptTools.writeJSVar(out, "MAP_LOADING_IMAGE_URI"     , rtp.getString(PROP_MAP_LOADING_IMAGE,null));

        /* icons/shadows */
        JSMap.writePushpinArray(out, reqState);

        /* constants (these do not change during the user session) */
        out.write("// Geozone support constants\n");
        JavaScriptTools.writeJSVar(out, "jsvGeozoneMode"        , false);
        JavaScriptTools.writeJSVar(out, "MAX_ZONE_RADIUS_M"     , Geozone.MAX_RADIUS_METERS);
        JavaScriptTools.writeJSVar(out, "MIN_ZONE_RADIUS_M"     , Geozone.MIN_RADIUS_METERS);
        JavaScriptTools.writeJSVar(out, "DETAIL_REPORT"         , this.isFeatureSupported(FEATURE_DETAIL_REPORT));
        JavaScriptTools.writeJSVar(out, "DETAIL_INFO_BOX"       , this.isFeatureSupported(FEATURE_DETAIL_INFO_BOX));
        JavaScriptTools.writeJSVar(out, "TEXT_METERS"           , GeoPoint.DistanceUnits.METERS.toString(locale));

        /* variables */
        out.write("// TrackMap Vars\n");
        JavaScriptTools.writeJSVar(out, "jsvPoiPins"            , null);
        JavaScriptTools.writeJSVar(out, "jsvDataSets"           , null);
        JavaScriptTools.writeJSVar(out, "jsvDetailPoints"       , null);
        JavaScriptTools.writeJSVar(out, "jsvDetailVisible"      , false);
        JavaScriptTools.writeJSVar(out, "jsvDetailAscending"    , privLabel.getBooleanProperty(PrivateLabel.PROP_TrackMap_detailAscending,true));
        JavaScriptTools.writeJSVar(out, "jsvDetailCenterPushpin", privLabel.getBooleanProperty(PrivateLabel.PROP_TrackMap_detailCenterPushpin,false));

        /* last update time */
        TimeZone tmz    = reqState.getTimeZone();
        String dateFmt  = (account != null)? account.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt  = (account != null)? account.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();
        DateTime today  = new DateTime(tmz);
        JavaScriptTools.writeJSVar(out, "jsvTodayEpoch"     , today.getTimeSec());
        JavaScriptTools.writeJSVar(out, "jsvTodayYMD"       , "{ YYYY:" + today.getYear(tmz) + ", MM:" + today.getMonth1(tmz) + ", DD:" + today.getDayOfMonth(tmz) + " }", false);
        JavaScriptTools.writeJSVar(out, "jsvTodayDateFmt"   , today.format(dateFmt,tmz));
        JavaScriptTools.writeJSVar(out, "jsvTodayTimeFmt"   , today.format(timeFmt,tmz));
        JavaScriptTools.writeJSVar(out, "jsvTodayTmzFmt"    , today.format("z"    ,tmz));

        /* last event time */
        out.write("// Last event time\n");
        DateTime lastEventTime = reqState.getLastEventTime();
        if (lastEventTime != null) {
            JavaScriptTools.writeJSVar(out, "jsvLastEventEpoch"     , lastEventTime.getTimeSec());
            JavaScriptTools.writeJSVar(out, "jsvLastEventYMD"       , "{ YYYY:"+lastEventTime.getYear(tmz)+", MM:"+lastEventTime.getMonth1(tmz)+", DD:"+lastEventTime.getDayOfMonth(tmz)+" }",false);
            JavaScriptTools.writeJSVar(out, "jsvLastEventDateFmt"   , lastEventTime.format(dateFmt,tmz));
            JavaScriptTools.writeJSVar(out, "jsvLastEventTimeFmt"   , lastEventTime.format(timeFmt,tmz));
            JavaScriptTools.writeJSVar(out, "jsvLastEventTmzFmt"    , lastEventTime.format("z"    ,tmz));
            JavaScriptTools.writeJSVar(out, "jsvLastBatteryLevel"   , 0L);
            JavaScriptTools.writeJSVar(out, "jsvLastSignalStrength" , 0L);
        } else {
            JavaScriptTools.writeJSVar(out, "jsvLastEventEpoch"     , 0L);
            JavaScriptTools.writeJSVar(out, "jsvLastEventYMD"       , null);
            JavaScriptTools.writeJSVar(out, "jsvLastEventDateFmt"   , null);
            JavaScriptTools.writeJSVar(out, "jsvLastEventTimeFmt"   , null);
            JavaScriptTools.writeJSVar(out, "jsvLastEventTmzFmt"    , null);
            JavaScriptTools.writeJSVar(out, "jsvLastBatteryLevel"   , 0.0);
            JavaScriptTools.writeJSVar(out, "jsvLastSignalStrength" , 0.0);
        }

        /* map pointers */
        out.write("// Map vars\n");
        JavaScriptTools.writeJSVar(out, "jsmapElem"             , null);
        JavaScriptTools.writeJSVar(out, "jsmap"                 , null);

    }

    /* write mapping support JS to stream */ 
    public static void writePushpinArray(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    {
        MapProvider mapProv = reqState.getMapProvider();
        out.write("// Icon URLs\n");
        out.write("var jsvPushpinIcon = new Array(\n");
        OrderedMap<String,PushpinIcon> iconMap = mapProv.getPushpinIconMap(reqState);
        for (Iterator<String> k = iconMap.keyIterator(); k.hasNext();) {
            String key = k.next();
            PushpinIcon ppi = iconMap.get(key);
            String  I  = ppi.getIconURL();
            boolean iE = ppi.getIconEval();
            int     iW = ppi.getIconWidth();
            int     iH = ppi.getIconHeight();
            int     iX = ppi.getIconHotspotX();
            int     iY = ppi.getIconHotspotY();
            String  S  = ppi.getShadowURL();
            int     sW = ppi.getShadowWidth();
            int     sH = ppi.getShadowHeight();
            String  B  = ppi.getBackgroundURL();
            int     bW = ppi.getBackgroundWidth();
            int     bH = ppi.getBackgroundHeight();
            int     bX = ppi.getBackgroundOffsetX();
            int     bY = ppi.getBackgroundOffsetY();
            out.write("    {");
            out.write(" key:\"" + key + "\",");
            if (iE) {
            out.write(" iconEval:\"" + I + "\",");
            } else {
            out.write(" iconURL:\"" + I + "\",");
            }
            out.write(" iconSize:["+iW+","+iH+"],");
            out.write(" iconOffset:["+iX+","+iY+"],");
            out.write(" iconHotspot:["+iX+","+iY+"],");
            out.write(" shadowURL:\"" + S + "\",");
            out.write(" shadowSize:["+sW+","+sH+"]");
            if (!StringTools.isBlank(B)) {
            out.write(",");
            out.write(" bgURL:\"" + B + "\",");
            out.write(" bgSize:["+bW+","+bH+"],");
            out.write(" bgOffset:["+bX+","+bY+"]");
            }
            out.write(" }");
            if (k.hasNext()) { out.write(","); }
            out.write("\n");
        }
        out.write("    );\n");
    }

    // ------------------------------------------------------------------------

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState, String jsMapURLs[]) 
        throws IOException 
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        JavaScriptTools.writeJSIncludes(out, jsMapURLs, request);
    }

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    {
        String jsMapURLs[] = StringTools.parseString(this.getProperties().getString(PROP_javascript,""),'\n');
        this.writeJSIncludes(out, reqState, jsMapURLs);
    }

    // ------------------------------------------------------------------------

    /* write JS to stream */ 
    public void writeJavaScript(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    { 

        /* prefetch map "Loading" image */
        if (this.getProperties().getBoolean(PROP_MAP_LOADING,false)) {
            String mapLoadingImageURI = this.getProperties().getString(PROP_MAP_LOADING_IMAGE,null);
            if (!StringTools.isBlank(mapLoadingImageURI)) {
                out.write("<link rel=\"prefetch\" href=\"" + mapLoadingImageURI + "\">\n");
            }
        }

        /* JSMap variables */
        JavaScriptTools.writeStartJavaScript(out);
        this.writeJSVariables(out, reqState); 
        JavaScriptTools.writeEndJavaScript(out); 

        /* JSMap JavaScript includes */
        this.writeJSIncludes(out, reqState); 

        /* event CSV parsing code */
        JavaScriptTools.writeStartJavaScript(out); 
        out.write(EventUtil.getInstance().getParseMapEventJS(reqState.isFleet(),reqState.getLocale()));
        JavaScriptTools.writeEndJavaScript(out);

    } 

    // ------------------------------------------------------------------------ 

    /* update request for map points */
    public void writeMapUpdate(RequestProperties reqState, int statusCodes[])
        throws IOException
    {
        super.writeMapUpdate(reqState, statusCodes); // XML/JSON
    }

    // ------------------------------------------------------------------------ 
 
} 
