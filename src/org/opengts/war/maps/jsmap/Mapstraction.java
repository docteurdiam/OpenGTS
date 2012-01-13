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
//  - http://www.mapstraction.com/
//  - http://mapstraction.com/svn/source/
//  - http://www.koders.com/javascript/fid85B6D94DE9D67A1D5648D53ED9E2BC05FFA5CD42.aspx
// ----------------------------------------------------------------------------
// Required supporting JavaScript:
//  - http://mapstraction.com/svn/source/mapstraction-geocode.js
//  - http://mapstraction.com/svn/source/mapstraction.js
//  - http://www.koders.com/javascript/fid85B6D94DE9D67A1D5648D53ED9E2BC05FFA5CD42.aspx
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Anthony George, Peter Jonas, Martin D. Flynn
//     -Initial release (extracted from "org/opengts/war/maps/ms/Mapstraction.java")
//  2008/08/08  Martin D. Flynn
//     -Included support for OpenSpace.
//     -Added limited Geozone support (OpenLayers only)
// ----------------------------------------------------------------------------
package org.opengts.war.maps.jsmap;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.maps.JSMap;

public class Mapstraction
    extends JSMap
{
    
    // ------------------------------------------------------------------------

    public  static final String PROVIDER_OPENLAYERS     = "openlayers";
    public  static final String PROVIDER_MICROSOFT      = "microsoft";
    public  static final String PROVIDER_GOOGLE         = "google";
    public  static final String PROVIDER_YAHOO          = "yahoo";
    public  static final String PROVIDER_MULTIMAP       = "multimap";
    public  static final String PROVIDER_MAP24          = "map24";
    public  static final String PROVIDER_MAPQUEST       = "mapquest";
    public  static final String PROVIDER_OPENSTREETMAP  = "openstreetmap";
    public  static final String PROVIDER_FREEEARTH      = "freeearth";
    public  static final String PROVIDER_OPENSPACE      = "openspace";
    public  static final String DEFAULT_PROVIDER        = PROVIDER_OPENLAYERS; 

    // ------------------------------------------------------------------------

    public  static final String PROP_PROVIDER[]         = new String[] { "provider" };
    public  static final String PROP_LOCAL_JS[]         = new String[] { "localMapstractionJS" };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private boolean didInitFeatures = false;

    /* Mapstraction instance */ 
    public Mapstraction(String name, String key) 
    {
        super(name, key); 
        this.addSupportedFeature(FEATURE_DETAIL_REPORT);
    }

    // ------------------------------------------------------------------------

    public boolean isFeatureSupported(long feature)
    {
        if (!this.didInitFeatures) {
            // lazy feature support initialization
            String mapProvider = this.getProperties().getString(PROP_PROVIDER, DEFAULT_PROVIDER); 
            boolean isOpenLayers = mapProvider.equals(PROVIDER_OPENLAYERS);
            if (isOpenLayers) {
                this.addSupportedFeature(FEATURE_LATLON_DISPLAY);
                this.addSupportedFeature(FEATURE_DISTANCE_RULER);
            }
            this.didInitFeatures = true;
        }
        return super.isFeatureSupported(feature);
    }

    // ------------------------------------------------------------------------

    /* write mapping support JS to stream */ 
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    {
        super.writeJSVariables(out, reqState);
        String mapProvider = this.getProperties().getString(PROP_PROVIDER, DEFAULT_PROVIDER); 
        out.write("// Mapstraction custom vars\n");
        // custom icons currently only work on OpenLayers
        JavaScriptTools.writeJSVar(out, "SHOW_CUSTOM_ICON", mapProvider.equals(PROVIDER_OPENLAYERS));
        //out.write("\n");
    }
    
    // ------------------------------------------------------------------------
    
    protected String _getAuthKey(String provider, String dftKey)
    {
        String mapKey = this.getAuthorization(); 
        if (StringTools.isBlank(mapKey)) { 
            Print.logError("No '%s' key!", provider); 
            mapKey = (dftKey != null)? dftKey : "";  
        }
        return mapKey;
    }

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    {
        java.util.List<String> jsList = new Vector<String>();
        
        /* main mapping support javascript */
        jsList.add(JavaScriptTools.qualifyJSFileRef("maps/jsmap.js"));
        
        /* specific Mapstraction provider */
        String mapProvider = this.getProperties().getString(PROP_PROVIDER, DEFAULT_PROVIDER); 
        if (mapProvider.equals(PROVIDER_OPENLAYERS)) {
            jsList.add("http://openlayers.org/api/OpenLayers.js");
        } else
        if (mapProvider.equals(PROVIDER_MICROSOFT)) {
            jsList.add("http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=5");
        } else
        if (mapProvider.equals(PROVIDER_GOOGLE)) {
            String key = this._getAuthKey(mapProvider,"INVALID_KEY"); 
            jsList.add("http://maps.google.com/maps?file=api&v=2&key="+key);
            jsList.add(JavaScriptTools.qualifyJSFileRef("mapstraction/labeledmarker.js"));
        } else
        if (mapProvider.equals(PROVIDER_YAHOO)) {
            String key = this._getAuthKey(mapProvider,"MapstractionDemo"); 
            jsList.add("http://api.maps.yahoo.com/ajaxymap?v=3.0&appid="+key);
        } else
        if (mapProvider.equals(PROVIDER_MAPQUEST)) {
            String key = this._getAuthKey(mapProvider,"mjtd%7Clu6t210anh%2Crn%3Do5-labwu"); 
            jsList.add("http://btilelog.access.mapquest.com/tilelog/transaction?transaction=script&key="+key+"&ipr=true&itk=true&v=5.1");
        } else
        if (mapProvider.equals(PROVIDER_MULTIMAP)) {
            String key = this._getAuthKey(mapProvider,"");
            jsList.add("http://developer.multimap.com/API/maps/1.2/"+key);
        } else
        if (mapProvider.equals(PROVIDER_MAP24)) {
            String key = this._getAuthKey(mapProvider,"FJXe1b9e7b896f8cf70534ee0c69ecbfX16");
            jsList.add("http://api.maptp.map24.com/ajax?appkey="+key);
        } else
        if (mapProvider.equals(PROVIDER_FREEEARTH)) {
            jsList.add("http://freeearth.poly9.com/api.js");
        } else
        if (mapProvider.equals(PROVIDER_OPENSPACE)) {
            String key = this._getAuthKey(mapProvider,"INVALID_KEY"); 
            jsList.add("http://openspace.ordnancesurvey.co.uk/osmapapi/openspace.js?key="+key);
        } else {
            Print.logError("Unrecognized map provider specified: %s", mapProvider); 
        }
        
        /* include 'mapstraction.js' */
        if (this.getProperties().getBoolean(PROP_LOCAL_JS,true)) {
            jsList.add(JavaScriptTools.qualifyJSFileRef("mapstraction/mapstraction-geocode.js"));
            jsList.add(JavaScriptTools.qualifyJSFileRef("mapstraction/mapstraction.js"));
        } else {
            jsList.add("http://mapstraction.com/svn/source/mapstraction-geocode.js");
            jsList.add("http://mapstraction.com/svn/source/mapstraction.js");
        }
        
        /* include OpenGTS mapping support for Mapstraction */
        jsList.add(JavaScriptTools.qualifyJSFileRef("maps/Mapstraction.js"));
        
        /* write out script html */
        super.writeJSIncludes(out, reqState, jsList.toArray(new String[jsList.size()]));
        
    }

    // ------------------------------------------------------------------------
    
    public String[] getGeozoneInstructions(int type, Locale loc)
    {
        I18N i18n = I18N.getI18N(Mapstraction.class, loc);
        return new String[] {
            i18n.getString("Mapstraction.geozoneInstructions", "")
        };
    }

    // ------------------------------------------------------------------------

}
