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
// ----------------------------------------------------------------------------
// Map alternatives:
//   http://mapserver.gis.umn.edu
//   http://www.cartoweb.org
//   http://tiger.census.gov/instruct.html
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;

public class MapProviderFactory
{
    
    // ------------------------------------------------------------------------

    private static final String MAP_PROVIDER_PACKAGE = DBConfig.PACKAGE_WAR_ + "maps";
    
    // ------------------------------------------------------------------------

    private static MapProviderFactory mapFactory = null;
    
    public static MapProviderFactory getInstance()
    {
        if (mapFactory == null) {
            mapFactory = new MapProviderFactory();
        }
        return mapFactory;
    }
    
    // ------------------------------------------------------------------------

    public static MapProvider getMapProviderForName(String providerClassName)
    {
        return getInstance().getMapProvider(providerClassName);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private HashMap<String,MapProvider> mapProviderMap = new HashMap<String,MapProvider>();
    
    private MapProviderFactory()
    {
        super();
    }

    // ------------------------------------------------------------------------

    private MapProvider getMapProvider(String providerClassName)
    {
        MapProvider mp = null;
        if (this.mapProviderMap.containsKey(providerClassName)) {

            /* already initialized */
            mp = this.mapProviderMap.get(providerClassName);

        } else {

            /* construct class name */
            String clzName = null;
            if (providerClassName.indexOf(".") >= 0) {
                clzName = providerClassName;
            } else {
                clzName = MAP_PROVIDER_PACKAGE + "." + providerClassName;
            }
            
            /* get instance of MapProvider */
            try {
                Class providerClass = Class.forName(clzName);
                mp = (MapProvider)providerClass.newInstance();
                this.mapProviderMap.put(providerClassName, mp);
                //Print.logInfo("Found MapProvider: " + clzName);
            } catch (Throwable t) { // ClassNotFoundException, ClassCastException, etc.
                Print.logError("MapProvider creation error: " + clzName + " [" + t);
                mp = null;
            }
            
        }
        return mp;
    }
    
}
