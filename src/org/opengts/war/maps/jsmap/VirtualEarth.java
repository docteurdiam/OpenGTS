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
// Drawing a circle:
//  - http://forums.microsoft.com/MSDN/ShowPost.aspx?PostID=2738537&SiteID=1
//  - http://pietschsoft.com/post/2008/02/Virtual-Earth-Draw-a-Circle-Radius-Around-a-LatLong-Point.aspx
//  - http://viavirtualearth.com/wiki/(S(ew5ue421jkq3r3yfvpwvf02s))/History.aspx?Page=Drawing-Tool+for+VE+v5&Revision=00001&AspxAutoDetectCookieSupport=1
// Dual Maps:
//  - http://www.mapchannels.com/dualmaps.aspx
// Misc:
//  - http://garzilla.net/vemaps/MovePushPin.aspx
//  - http://garzilla.net/vemaps/MovePushPin2.aspx
//  - http://garzilla.net/vemaps/MovePolygon.aspx
//  - http://garzilla.net/vemaps/SideBar.aspx
// ----------------------------------------------------------------------------
// Note: 
//   When using the Microsoft Virtual Earth mapping service, it is your responsibility 
//   to make sure you comply with all of the Microsoft terms of use for this service:
// ----
// Microsoft Virtual Earth Service API Terms of Use:
//   http://www.microsoft.com/virtualearth/control/terms.mspx
// Microsoft Windows Live Terms of Use:
//   http://tou.live.com/
// Microsoft Online Privacy Statement:
//   http://privacy.microsoft.com/
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/08/08  Martin D. Flynn
//     -Added Geozone support
// ----------------------------------------------------------------------------
package org.opengts.war.maps.jsmap;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.db.tables.Geozone;
import org.opengts.war.tools.*;
import org.opengts.war.maps.JSMap;

public class VirtualEarth
    extends JSMap
{

    // ------------------------------------------------------------------------

    private static final String  CURSOR_CROSSHAIR       = "crosshair";
    private static final String  CURSOR_HAND_OPEN       = "http://maps.live.com/cursors/grab.cur";

    private static final String  VE_MAPCONTROL_URL      = "http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6.2";
        //"http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=5";
        //"http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6";
        //"http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6.1";

    // ------------------------------------------------------------------------

    // the larger the scal value to fewer meters-per-pixel
    private int VALID_ZOOM_VALUES[] = new int[] {
        // --> Greater meters-per-pixel -->
        19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
    };

    private static final double  DEFAULT_ZOOM           = 7.0; // 1..19

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* VirtualEarth instance */ 
    public VirtualEarth(String name, String key) 
    {
        super(name, key); 
        this.addSupportedFeature(FEATURE_LATLON_DISPLAY);
        this.addSupportedFeature(FEATURE_DISTANCE_RULER);
        this.addSupportedFeature(FEATURE_GEOZONES);
        this.addSupportedFeature(FEATURE_DETAIL_REPORT);
        this.addSupportedFeature(FEATURE_DETAIL_INFO_BOX);
        this.addSupportedFeature(FEATURE_REPLAY_POINTS);
        this.addSupportedFeature(FEATURE_CENTER_ON_LAST);
        this.addSupportedFeature(FEATURE_CORRIDORS);
    }

    // ------------------------------------------------------------------------

    /* write mapping support JS to stream */ 
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        super.writeJSVariables(out, reqState);
    }

    // ------------------------------------------------------------------------

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        MapProvider mp = reqState.getMapProvider();
        RTProperties mrtp = (mp != null)? mp.getProperties() : null;
        String mapControlURL = (mrtp != null)? mrtp.getString("ve.mapcontrol", null) : null;
        super.writeJSIncludes(out, reqState, new String[] {
            JavaScriptTools.qualifyJSFileRef("maps/jsmap.js"),
            (!StringTools.isBlank(mapControlURL)? mapControlURL : VE_MAPCONTROL_URL),
            JavaScriptTools.qualifyJSFileRef("maps/VirtualEarth.js")
        });
    }

    // ------------------------------------------------------------------------

    protected String getMapCellStyle(RequestProperties reqState, MapDimension mapDim)
    {
        return "position:relative; " + super.getMapCellStyle(reqState, mapDim);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the number of supported points for the specified Geozone type
    *** @param type  The Geozone type
    *** @return The number of supported points for the specified Geozone type
    **/
    public int getGeozoneSupportedPointCount(int type)
    {

        /* Geozone type supported? */
        Geozone.GeozoneType gzType = Geozone.getGeozoneType(type);
        if (!Geozone.IsGeozoneTypeSupported(gzType)) {
            return 0;
        }

        /* return supported point count */
        RTProperties rtp = this.getProperties();
        switch (gzType) {
            case POINT_RADIUS        : return rtp.getBoolean(PROP_zone_map_multipoint,false)? Geozone.GetGeoPointCount() : 1;
            case BOUNDED_RECT        : return 0; // not yet supported
            case SWEPT_POINT_RADIUS  : return rtp.getBoolean(PROP_zone_map_corridor,false)  ? Geozone.GetGeoPointCount() : 0;
            case POLYGON             : return rtp.getBoolean(PROP_zone_map_polygon,false)   ? Geozone.GetGeoPointCount() : 0;
        }
        return 0;

    }

    public String[] getGeozoneInstructions(int type, Locale loc)
    {
        I18N i18n = I18N.getI18N(VirtualEarth.class, loc);
        if (type == Geozone.GeozoneType.POINT_RADIUS.getIntValue()) {
            return new String[] {
                i18n.getString("VirtualEarth.geozoneNotes.1", "Click to reset center."),
                i18n.getString("VirtualEarth.geozoneNotes.2", "Click-drag Geozone to move."),
                i18n.getString("VirtualEarth.geozoneNotes.3", "Shift-click-drag to resize."),
                i18n.getString("VirtualEarth.geozoneNotes.4", "Ctrl-click-drag for distance.")
            };
        } else
        if (type == Geozone.GeozoneType.POLYGON.getIntValue()) {
            return new String[] {
                i18n.getString("VirtualEarth.geozoneNotes.1", "Click to reset center."),
                i18n.getString("VirtualEarth.geozoneNotes.5", "Click-drag corner to resize."),
                i18n.getString("VirtualEarth.geozoneNotes.4", "Ctrl-click-drag for distance.")
            };
        } else {
            return new String[0];
        }
    }

    // ------------------------------------------------------------------------

}
