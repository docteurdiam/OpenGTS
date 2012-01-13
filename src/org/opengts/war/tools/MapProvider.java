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
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/05/06  Martin D. Flynn
//     -Added methods "isAttributeSupported" & "writeMapUpdate"
//  2008/04/11  Martin D. Flynn
//     -Added/modified map provider property keys
//     -Added auto-update methods.
//     -Added name and authorization (service provider key) methods
//  2008/08/20  Martin D. Flynn
//     -Added 'isFeatureSupported', removed 'isAttributeSupported'
//  2008/08/24  Martin D. Flynn
//     -Added 'getReplayEnabled()' and 'getReplayInterval()' methods.
//  2008/09/19  Martin D. Flynn
//     -Added 'getAutoUpdateOnLoad()' method.
//  2009/02/20  Martin D. Flynn
//     -Added "map.minProximity" property.  This is used to trim redundant events
//      (those closely located to each other) from being display on the map.
//  2009/09/23  Martin D. Flynn
//     -Added support for customizing the Geozone map width/height
//  2009/11/01  Martin D. Flynn
//     -Added 'isFleet' argument to "getMaxPushpins"
//  2009/04/11  Martin D. Flynn
//     -Changed "getMaxPushpins" argument to "RequestProperties"
//  2011/10/03  Martin D. Flynn
//     -Added "map.showPushpins" property.
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;

public interface MapProvider
{

    // ------------------------------------------------------------------------

    /* these attributes are used during runtime only, they are not cached */
    public static final long    FEATURE_GEOZONES            = 0x00000001L;
    public static final long    FEATURE_LATLON_DISPLAY      = 0x00000002L;
    public static final long    FEATURE_DISTANCE_RULER      = 0x00000004L;
    public static final long    FEATURE_DETAIL_REPORT       = 0x00000008L;
    public static final long    FEATURE_DETAIL_INFO_BOX     = 0x00000010L;
    public static final long    FEATURE_REPLAY_POINTS       = 0x00000020L;
    public static final long    FEATURE_CENTER_ON_LAST      = 0x00000040L;
    public static final long    FEATURE_CORRIDORS           = 0x00000080L;

    // ------------------------------------------------------------------------

    public static final String  ID_DETAIL_TABLE             = "trackMapDataTable";
    public static final String  ID_DETAIL_CONTROL           = "trackMapDataControl";

    public static final String  ID_LAT_LON_DISPLAY          = "trackMapLatLonDisplay";
    public static final String  ID_DISTANCE_DISPLAY         = "trackMapDistanceDisplay";

    public static final String  ID_LATEST_EVENT_DATE        = "lastEventDate";
    public static final String  ID_LATEST_EVENT_TIME        = "lastEventTime";
    public static final String  ID_LATEST_EVENT_TMZ         = "lastEventTmz";

    public static final String  ID_LATEST_BATTERY           = "lastBatteryLevel";

    public static final String  ID_MESSAGE_TEXT             = CommonServlet.ID_CONTENT_MESSAGE;

    // ------------------------------------------------------------------------

    public static final String  ID_ZONE_RADIUS_M            = "trackMapZoneRadiusM";
    public static final String  ID_ZONE_LATITUDE_           = "trackMapZoneLatitude_";
    public static final String  ID_ZONE_LONGITUDE_          = "trackMapZoneLongitude_";

    // ------------------------------------------------------------------------
    // Preferred/Default map width/height
    // Note: 'contentTableFrame' in 'private.xml' should have dimensions based on the map size,
    // roughly as follows:
    //    width : MAP_WIDTH  + 164;  [680 + 164 = 844]
    //    height: MAP_HEIGHT +  80;  [420 +  80 = 500]

    public static final int     MAP_WIDTH                   = 680;
    public static final int     MAP_HEIGHT                  = 470;

    public static final int     ZONE_WIDTH                  = 630;
    public static final int     ZONE_HEIGHT                 = 630; // 535;

    // ------------------------------------------------------------------------

    /* geozone properties */
    public static final String  PROP_zone_map_width[]       = new String[] { "zone.map.width"                                    };  // int     (zone map width)
    public static final String  PROP_zone_map_height[]      = new String[] { "zone.map.height"                                   };  // int     (zone map height)
    public static final String  PROP_zone_map_multipoint[]  = new String[] { "zone.map.multipoint",   "geozone.multipoint"       };  // boolean (supports multiple point-radii)
    public static final String  PROP_zone_map_polygon[]     = new String[] { "zone.map.polygon"                                  };  // boolean (supports polygons)
    public static final String  PROP_zone_map_corridor[]    = new String[] { "zone.map.corridor"                                 };  // boolean (supports swept-point-radius)

    /* standard properties */
    public static final String  PROP_map_width[]            = new String[] { "map.width"                                         };  // int     (map width)
    public static final String  PROP_map_height[]           = new String[] { "map.height"                                        };  // int     (map height)
    public static final String  PROP_map_fillFrame[]        = new String[] { "map.fillFrame"                                     };  // boolean (map fillFrame)
    public static final String  PROP_maxPushpins_device[]   = new String[] { "map.maxPushpins.device" , "map.maxPushpins"        };  // int     (maximum pushpins)
    public static final String  PROP_maxPushpins_fleet[]    = new String[] { "map.maxPushpins.fleet"  , "map.maxPushpins"        };  // int     (maximum pushpins)
    public static final String  PROP_maxPushpins_report[]   = new String[] { "map.maxPushpins.report" , "map.maxPushpins"        };  // int     (maximum pushpins)
    public static final String  PROP_map_pushpins[]         = new String[] { "map.showPushpins"       , "map.pushpins"           };  // boolean (include pushpins)
    public static final String  PROP_map_routeLine[]        = new String[] { "map.routeLine"                                     };  // boolean (include route line)
    public static final String  PROP_map_routeLine_color[]  = new String[] { "map.routeLine.color"                               };  // boolean (include route line arrows)
    public static final String  PROP_map_routeLine_arrows[] = new String[] { "map.routeLine.arrows"                              };  // boolean (include route line arrows)
    public static final String  PROP_map_view[]             = new String[] { "map.view"                                          };  // String  (road|satellite|hybrid)
    public static final String  PROP_map_minProximity[]     = new String[] { "map.minProximity" /*meters*/                       };  // double  (mim meters between events)
    public static final String  PROP_map_includeGeozones[]  = new String[] { "map.includeGeozones"    , "includeGeozones"        };  // boolean (include traversed Geozones)
    public static final String  PROP_pushpin_zoom[]         = new String[] { "pushpin.zoom"                                      };  // dbl/int (default zoom with points)
    public static final String  PROP_default_zoom[]         = new String[] { "default.zoom"                                      };  // dbl/int (default zoom without points)
    public static final String  PROP_default_latitude[]     = new String[] { "default.lat"            , "default.latitude"       };  // double  (default latitude)
    public static final String  PROP_default_longitude[]    = new String[] { "default.lon"            , "default.longitude"      };  // double  (default longitude)
    public static final String  PROP_info_showAltitude[]    = new String[] { "info.showAltitude"                                 };  // boolean (show altitude in info bubble)
    public static final String  PROP_detail_showSatCount[]  = new String[] { "detail.showSatCount"                               };  // boolean (show altitude in info bubble)

    /* auto update properties */
    public static final String  PROP_auto_enable_device[]   = new String[] { "auto.enable"            , "auto.enable.device"     };  // boolean (auto update)
    public static final String  PROP_auto_onload_device[]   = new String[] { "auto.onload"            , "auto.onload.device"     };  // boolean (auto update onload)
    public static final String  PROP_auto_interval_device[] = new String[] { "auto.interval"          , "auto.interval.device"   };  // int     (update interval seconds)
    public static final String  PROP_auto_count_device[]    = new String[] { "auto.count"             , "auto.count.device"      };  // int     (update count)
    public static final String  PROP_auto_enable_fleet[]    = new String[] { "auto.enable"            , "auto.enable.fleet"      };  // boolean (auto update)
    public static final String  PROP_auto_onload_fleet[]    = new String[] { "auto.onload"            , "auto.onload.fleet"      };  // boolean (auto update onload)
    public static final String  PROP_auto_interval_fleet[]  = new String[] { "auto.interval"          , "auto.interval.fleet"    };  // int     (update interval seconds)
    public static final String  PROP_auto_count_fleet[]     = new String[] { "auto.count"             , "auto.count.fleet"       };  // int     (update count)

    /* replay properties (device map only) */
    public static final String  PROP_replay_enable[]        = new String[] { "replay.enable"                                     };  // boolean (replay)
    public static final String  PROP_replay_interval[]      = new String[] { "replay.interval"                                   };  // int     (replay interval milliseconds)
    public static final String  PROP_replay_singlePushpin[] = new String[] { "replay.singlePushpin"                              };  // boolean (show single pushpin)

    /* detail report */
    public static final String  PROP_combineSpeedHeading[]  = new String[] { "details.combineSpeedHeading"                       };  // boolean (combine speed/heading columns)

    /* icon selector */
    public static final String  PROP_iconSelector[]         = new String[] { "iconSelector"       , "iconselector.device"        };  // String  (default icon selector)
    public static final String  PROP_iconSelector_legend[]  = new String[] { "iconSelector.legend", "iconSelector.device.legend" };  // String  (icon selector legend)
    public static final String  PROP_iconSel_fleet[]        = new String[] { "iconSelector.fleet"                                };  // String  (fleet icon selector)
    public static final String  PROP_iconSel_fleet_legend[] = new String[] { "iconSelector.fleet.legend"                         };  // String  (fleet icon selector legend)

    /* JSMap properties */
    public static final String  PROP_javascript[]           = new String[] { "javascript.include", "javascript"                  };  // String  (JSMap provider JS)

    /* optional properties */
    public  static final String PROP_scrollWheelZoom[]      = new String[] { "scrollWheelZoom"                                   };  // boolean (scroll wheel zoom)

    // ------------------------------------------------------------------------

    public static final double  DEFAULT_LATITUDE            = 39.0000;
    public static final double  DEFAULT_LONGITUDE           = -96.5000;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the MapProvider name
    *** @return The MapProvider name
    **/
    public String getName();
    
    /**
    *** Returns the MapProvider authorization String/Key (passed to the map service provider)
    *** @return The MapProvider authorization String/Key.
    **/
    public String getAuthorization();

    // ------------------------------------------------------------------------

    /**
    *** Sets the properties for this MapProvider.
    *** @param props  A String representation of the properties to set in this
    ***               MapProvider.  The String must be in the form "key=value key=value ...".
    **/
    public void setProperties(String props);
    
    /**
    *** Returns the properties for this MapProvider
    *** @return The properties for this MapProvider
    **/
    public RTProperties getProperties();

    // ------------------------------------------------------------------------

    /**
    *** Sets the zoom regions
    *** @param The zoon regions 
    **/
    /* public void setZoomRegions(Map<String,String> map); */

    /**
    *** Gets the zoom regions
    *** @return The zoon regions 
    **/
    /* public Map<String,String> getZoomRegions(); */

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum number of allowed pushpins on the map at one time
    *** @param reqState The session RequestProperties instance
    *** @return The maximum number of allowed pushpins on the map
    **/
    public long getMaxPushpins(RequestProperties reqState);

    /**
    *** Gets the pushpin icon map
    *** @param reqState  The RequestProperties for the current session
    *** @return The PushPinIcon map
    **/
    public OrderedMap<String,PushpinIcon> getPushpinIconMap(RequestProperties reqState);

    // ------------------------------------------------------------------------

    /**
    *** Gets the icon selector for the current map
    *** @param reqState  The RequestProperties for the current session
    *** @return The icon selector String
    **/
    public String getIconSelector(RequestProperties reqState);
    
    /**
    *** Gets the IconSelector legend displayed on the map page to indicate the
    *** type of pushpins displayed on the map.
    *** @param reqState  The RequestProperties for the current session
    *** @return The IconSelector legend (in html format)
    **/
    public String getIconSelectorLegend(RequestProperties reqState);

    // ------------------------------------------------------------------------

    /**
    *** Returns the MapDimension for this MapProvider
    *** @return The MapDimension
    **/
    public MapDimension getDimension();
    
    /**
    *** Returns the Width from the MapDimension
    *** @return The MapDimension width
    **/
    public int getWidth();
    
    /**
    *** Returns the Height from the MapDimension
    *** @return The MapDimension height
    **/
    public int getHeight();

    // ------------------------------------------------------------------------

    /**
    *** Returns the Geozone MapDimension for this MapProvider
    *** @return The Geozone MapDimension
    **/
    public MapDimension getZoneDimension();
    
    /**
    *** Returns the Geozone Width from the MapDimension
    *** @return The Geozone MapDimension width
    **/
    public int getZoneWidth();
    
    /**
    *** Returns the Geozone Height from the MapDimension
    *** @return The Geozone MapDimension height
    **/
    public int getZoneHeight();

    // ------------------------------------------------------------------------

    /**
    *** Returns the default map center (when no pushpins are displayed)
    *** @param dft  The GeoPoint center to return if not otherwised overridden
    *** @return The default map center
    **/
    public GeoPoint getDefaultCenter(GeoPoint dft);
    
    /**
    *** Returns the default zoom level
    *** @param dft  The default zoom level to return
    *** @param withPushpins  If true, return the default zoom level is at least
    ***                      one pushpin is displayed.
    *** @return The default zoom level
    **/
    public double getDefaultZoom(double dft, boolean withPushpins);

    // ------------------------------------------------------------------------

    /** 
    *** Returns true if auto-update is enabled
    *** @param isFleet True for fleet map
    *** @return True if auto-updated is enabled
    **/
    public boolean getAutoUpdateEnabled(boolean isFleet);

    /** 
    *** Returns true if auto-update on-load is enabled
    *** @param isFleet True for fleet map
    *** @return True if auto-updated on-load is enabled
    **/
    public boolean getAutoUpdateOnLoad(boolean isFleet);

    /** 
    *** Returns the auto-update interval in seconds
    *** @param isFleet True for fleet map
    *** @return The auto-update interval in seconds
    **/
    public long getAutoUpdateInterval(boolean isFleet);

    /** 
    *** Returns the auto-update count
    *** @param isFleet True for fleet map
    *** @return The auto-update count (-1 for indefinate)
    **/
    public long getAutoUpdateCount(boolean isFleet);

    // ------------------------------------------------------------------------

    /** 
    *** Returns true if replay is enabled
    *** @return True if replay is enabled
    **/
    public boolean getReplayEnabled();

    /** 
    *** Returns the replay interval in seconds
    *** @return The replay interval in seconds
    **/
    public long getReplayInterval();

    /** 
    *** Returns true if only a single pushpin is to be displayed at a time during replay
    *** @return True if only a single pushpin is to be displayed at a time during replay
    **/
    public boolean getReplaySinglePushpin();

    // ------------------------------------------------------------------------

    /**
    *** Writes any required CSS to the specified PrintWriter.  This method is 
    *** intended to be overridden to provide the required behavior.
    *** @param out  The PrintWriter
    *** @param reqState The session RequestProperties
    **/
    public void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException;

    /**
    *** Writes any required JavaScript to the specified PrintWriter.  This method is 
    *** intended to be overridden to provide the required behavior.
    *** @param out  The PrintWriter
    *** @param reqState The session RequestProperties
    **/
    public void writeJavaScript(PrintWriter out, RequestProperties reqState)
        throws IOException;
    
    /**
    *** Writes map cell to the specified PrintWriter.  This method is intended
    *** to be overridden to provide the required behavior for the specific MapProvider
    *** @param out  The PrintWriter
    *** @param reqState The session RequestProperties
    *** @param mapDim  The MapDimension
    **/
    public void writeMapCell(PrintWriter out, RequestProperties reqState, MapDimension mapDim)
        throws IOException;

    // ------------------------------------------------------------------------

    /**
    *** Updates the points to the current displayed map
    *** @param reqState The session RequestProperties
    **/
    public void writeMapUpdate(RequestProperties reqState,
        int statusCodes[])
        throws IOException;

    /**
    *** Updates the points to the current displayed map
    *** @param out       The output PrintWriter
    *** @param indentLvl The indentation level (0 for no indentation)
    *** @param reqState  The session RequestProperties
    **/
    public void writeMapUpdate(PrintWriter out, int indentLvl, RequestProperties reqState,
        int statusCodes[])
        throws IOException;

    // ------------------------------------------------------------------------

    /**
    *** Gets the number of supported Geozone points
    *** @param type  The Geozone type
    *** @return The number of supported points.
    **/
    public int getGeozoneSupportedPointCount(int type);

    /**
    *** Returns the localized Geozone instructions
    *** @param type The Geozone type
    *** @param loc  The current Locale
    *** @return An array of instruction line items
    **/
    public String[] getGeozoneInstructions(int type, Locale loc);

    // ------------------------------------------------------------------------

    /**
    *** Returns the localized GeoCorridor instructions
    *** @param loc  The current Locale
    *** @return An array of instruction line items
    **/
    public String[] getCorridorInstructions(Locale loc);

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified feature is supported
    *** @param featureMask  The feature mask to test
    *** @return True if the specified feature is supported
    **/
    public boolean isFeatureSupported(long featureMask);

}
