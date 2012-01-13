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
// Reverse-Geocoding possibilities:
//  - http://blog.programmableweb.com/2008/10/24/google-maps-api-gets-reverse-geocoding/
//  - http://gmaps-samples.googlecode.com/svn/trunk/geocoder/reverse.html
//  - http://groups.google.com/group/Google-Maps-API/web/resources-non-google-geocoders
//  - http://www.freereversegeo.com/
//  - http://mapperz.blogspot.com/2007/08/exclusive-reverse-geocoding-using.html
//  - http://nicogoeminne.googlepages.com/reversegeocode.html
// Dual Maps:
//  - http://www.mapchannels.com/dualmaps.aspx
// Register for Google Map keys:
//  - http://www.google.com/apis/maps/signup.html
// Usage examples:
//  - http://mapstraction.com/demo-filters.php
//  - http://econym.org.uk/gmap/
// Scale/Zoom/Meters-per-pixel
//  - http://slappy.cs.uiuc.edu/fall06/cs492/Group2/example.html
// Misc (many useful examples)
//  - http://www.bdcc.co.uk/Gmaps/BdccGmapBits.htm
//  - http://groups.google.com/group/Google-Maps-API/web/examples-tutorials-gpolygon-gpolyline
//  - http://code.nosvamosdetapas.com/googlemaps/test5.html
//  - http://maps.huge.info/examples.htm
//      - http://maps.huge.info/dragcircle2.htm
//      - http://maps.huge.info/dragpoly.htm
//  - http://wolfpil.googlepages.com/polygon.html 
//  - http://maps.forum.nu/gm_plot.html
//  - http://wolfpil.googlepages.com/switch-polies.html
// Google Pushpins:
//  - http://labs.google.com/ridefinder/images/mm_20_${color}.png
//  - http://labs.google.com/ridefinder/images/mm_20_shadow.png
//  - http://gmaps-utility-library.googlecode.com/svn/trunk/mapiconmaker/1.1/docs/examples.html
//  - http://www.powerhut.co.uk/googlemaps/custom_markers.php
//  - http://groups.google.com/group/google-maps-api/web/examples-tutorials-custom-icons-for-markers
//  - http://thydzik.com/dynamic-google-maps-markersicons-with-php/
// Google Search Bar:
//  - http://code.google.com/apis/maps/documentation/services.html#LocalSearch
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/08/08  Martin D. Flynn
//     -Added Geozone support
//  2009/08/07  Martin D. Flynn
//     -Added "google.mapcontrol" and "google.sensor" properties.
//  2009/12/16  Martin D. Flynn
//     -Added support for client-id (ie. "&client=gme-...")
//  2011/06/16  Martin D. Flynn
//     -Initial support for Google API v3 (requires supporting JS as well)
// ----------------------------------------------------------------------------
package org.opengts.war.maps.jsmap;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.db.DBConfig;
import org.opengts.db.tables.Geozone;
import org.opengts.war.tools.*;
import org.opengts.war.maps.JSMap;

public class GoogleMaps
    extends JSMap
{

    // ------------------------------------------------------------------------

    private static final String  PROP_version       = "google.version";
    private static final String  PROP_mapcontrol    = "google.mapcontrol";
    private static final String  PROP_useSSL        = "google.useSSL";
    private static final String  PROP_sensor        = "google.sensor";
    private static final String  PROP_channel       = "google.channel";
    private static final String  PROP_enableRuler   = "google.enableRuler"; // V3 only

    private static final String  PremierPrefix_     = "gme-";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int apiVersion      = 0;

    /* GoogleMaps instance */ 
    public GoogleMaps(String name, String key) 
    {
        super(name, key);
        this.addSupportedFeature(FEATURE_LATLON_DISPLAY);
        this.addSupportedFeature(FEATURE_GEOZONES);
        this.addSupportedFeature(FEATURE_DETAIL_REPORT);
        this.addSupportedFeature(FEATURE_DETAIL_INFO_BOX);
        this.addSupportedFeature(FEATURE_REPLAY_POINTS);
        this.addSupportedFeature(FEATURE_CENTER_ON_LAST);
        this.addSupportedFeature(FEATURE_CORRIDORS);
    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if using Google API v3
    **/
    public boolean isVersion3()
    {
        if (this.apiVersion <= 0) {
            // "maps/GoogleMapsV2.js"
            // "maps/GoogleMapsV3.js" - may not be present
            String vers = this.getProperties().getString(PROP_version, "");
            if (StringTools.isBlank(vers)) {
                this.apiVersion = 2; // assume V2
            } else {
                this.apiVersion = vers.equals("3")? 3 : 2; // "3" must be explicit
            }
        }
        return (this.apiVersion == 3);
    }

    // ------------------------------------------------------------------------

    /**
    *** Called after initialization of this MapProvider.  This allows the MapProvider
    *** to perform any required initialization after all attributes have been set 
    **/
    public void postInit()
    {
        if (this.isVersion3()) {
            if (this.getProperties().getBoolean(PROP_enableRuler,false)) {
                this.addSupportedFeature(FEATURE_DISTANCE_RULER); // V3 only (maybe)
            }
        }
    }

    // ------------------------------------------------------------------------

    /* validate */
    public boolean validate()
    {

        /* check authorization key */
        if (!this.isVersion3()) { // V2 only
            String authKey = this.getAuthorization();
            if ((authKey == null) || authKey.startsWith("*")) {
                Print.logError("Google Map key not specified");
                return false;
            } else
            if (!authKey.startsWith(PremierPrefix_) && (authKey.length() < 30)) {
                Print.logError("Invalid Google Map key specified");
                return false;
            }
        }

        /* valid */
        return true;

    }

    // ------------------------------------------------------------------------

    /* write mapping support JS to stream */ 
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        JavaScriptTools.writeJSVar(out, "GOOGLE_API_V2", !this.isVersion3());
        super.writeJSVariables(out, reqState);
    }

    // ------------------------------------------------------------------------

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {

        /* map provider properties */
        MapProvider  mp   = reqState.getMapProvider(); // "(mp == this)" should be true
        RTProperties mrtp = (mp != null)? mp.getProperties() : null;

        /* authorization key */
        String mapCtlURL = (mrtp != null)? mrtp.getString(PROP_mapcontrol,null) : null;
        if (StringTools.isBlank(mapCtlURL)) {
            StringBuffer sb = new StringBuffer();
            // initial URL
            boolean useSSL = mrtp.getBoolean(PROP_useSSL, false);
            sb.append(useSSL? "https://" : "http://");
            if (this.isVersion3()) {
                sb.append("maps.google.com/maps/api/js?v=3");
            } else {
                sb.append("maps.google.com/maps?file=api&v=2");
            }
            // "key="
            String channelVal = (mrtp != null)? mrtp.getString(PROP_channel,"") : "";
            String authKey    = this.getAuthorization();
            if (!StringTools.isBlank(authKey) && !authKey.startsWith("*")) {
                // a Google API key has been specified
                if (authKey.startsWith(PremierPrefix_)) {
                    sb.append("&client=").append(authKey);
                    if (StringTools.isBlank(channelVal)) {
                        channelVal = DBConfig.getServiceAccountID(null);
                    }
                } else
                if (!this.isVersion3() && (authKey.length() < 30)) {
                    Print.logError("Invalid Google Map key specified");
                } else {
                    sb.append("&key=").append(authKey);
                }
            } else {
                // no Google API key specified
                if (!this.isVersion3()) {
                    Print.logError("Google Map key not specified");
                }
            }
            // "&channel="
            if (!StringTools.isBlank(channelVal)) {
                sb.append("&channel=").append(channelVal);
            }
            // "&sensor="
            String sensorVal = (mrtp != null)? mrtp.getString(PROP_sensor,"true") : "true";
            if (!StringTools.isBlank(sensorVal)) {
                sb.append("&sensor=").append(sensorVal);
            }
            // "&oe=" character encoding
            sb.append("&oe=").append("utf-8");
            // "&hl=" localization
            String localStr = reqState.getPrivateLabel().getLocaleString();
            if (!StringTools.isBlank(localStr)) {
                sb.append("&hl=").append(localStr);
            }
            // URL
            mapCtlURL = sb.toString();
        }

        /* display Javascript */
        super.writeJSIncludes(out, reqState, new String[] {
            JavaScriptTools.qualifyJSFileRef("maps/jsmap.js"),
            mapCtlURL,
            (this.isVersion3()?
                JavaScriptTools.qualifyJSFileRef("maps/GoogleMapsV3.js"):
                JavaScriptTools.qualifyJSFileRef("maps/GoogleMapsV2.js"))
        });
        
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
            case SWEPT_POINT_RADIUS  : return rtp.getBoolean(PROP_zone_map_corridor,false)? Geozone.GetGeoPointCount() : 0;
            case POLYGON             : return rtp.getBoolean(PROP_zone_map_polygon,false)? Geozone.GetGeoPointCount() : 0;
        }
        return 0;

    }

    public String[] getGeozoneInstructions(int type, Locale loc)
    {
        I18N i18n = I18N.getI18N(GoogleMaps.class, loc);
        if (type == Geozone.GeozoneType.POINT_RADIUS.getIntValue()) {
            return new String[] { 
                i18n.getString("GoogleMaps.geozoneNotes.1", "Click to reset center."),
                i18n.getString("GoogleMaps.geozoneNotes.2", "Click-drag center to move."),
                i18n.getString("GoogleMaps.geozoneNotes.3", "Click-drag radius to resize."),
            };
        } else
        if (type == Geozone.GeozoneType.POLYGON.getIntValue()) {
            return new String[] { 
                i18n.getString("GoogleMaps.geozoneNotes.1", "Click to reset center."),
                i18n.getString("GoogleMaps.geozoneNotes.2", "Click-drag center to move."),
                i18n.getString("GoogleMaps.geozoneNotes.4", "Click-drag corner to resize."),
            };
        } else {
            return new String[0];
        }
    }

    // ------------------------------------------------------------------------

}
