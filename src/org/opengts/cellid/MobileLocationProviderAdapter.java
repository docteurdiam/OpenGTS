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
//  2011/07/01  Martin D. Flynn
//      -Initial release
// ----------------------------------------------------------------------------
package org.opengts.cellid;

import org.opengts.util.*;

/**
*** Adapter class for obtaining the location of a Cell Tower
**/
public abstract class MobileLocationProviderAdapter
{

    // ------------------------------------------------------------------------

    public static final String PROP_MobileLocationProvider_ = "MobileLocationProvider.";
    public static final String _PROP_isEnabled              = ".isEnabled";

    // ------------------------------------------------------------------------

    private String       name           = null;
    private TriState     isEnabled      = TriState.UNKNOWN;
    
    private String       accessKey      = null;
    private RTProperties properties     = null;

    /**
    *** Constructor
    *** @param name  The name of this reverse-geocode provider
    *** @param key     The access key (may be null)
    *** @param rtProps The properties (may be null)
    **/
    public MobileLocationProviderAdapter(String name, String key, RTProperties rtProps)
    {
        super();
        this.setName(name);
        this.setAuthorization(key);
        this.setProperties(rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this MobileLocationProvider
    *** @param name  The name of this MobileLocationProvider
    **/
    public void setName(String name)
    {
        this.name = (name != null)? name : "";
    }

    /**
    *** Gets the name of this MobileLocationProvider
    *** @return The name of this MobileLocationProvider
    **/
    public String getName()
    {
        return (this.name != null)? this.name : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this MobileLocationProvider is enabled
    *** @return True if this MobileLocationProvider is enabled, false otherwise
    **/
    public boolean isEnabled()
    {
        if (this.isEnabled.isUnknown()) {
            String key = PROP_MobileLocationProvider_ + this.getName() + _PROP_isEnabled;
            if (RTConfig.getBoolean(key,true)) {
                this.isEnabled = TriState.TRUE;
            } else {
                this.isEnabled = TriState.FALSE;
                Print.logWarn("MobileLocationProvider disabled: " + this.getName());
            }
        }
        return this.isEnabled.isTrue();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the authorization key of this MobileLocationProvider
    *** @param key  The key of this MobileLocationProvider
    **/
    public void setAuthorization(String key)
    {
        this.accessKey = key;
    }
    
    /**
    *** Gets the authorization key of this MobileLocationProvider
    *** @return The access key of this MobileLocationProvider
    **/
    public String getAuthorization()
    {
        return this.accessKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the properties for this ReverseGeocodeProvider
    *** @param rtProps  The properties for this reverse-geocode provider
    **/
    public void setProperties(RTProperties rtProps)
    {
        this.properties = rtProps;
    }

    /**
    *** Gets the properties for this ReverseGeocodeProvider
    *** @return The properties for this reverse-geocode provider
    **/
    public RTProperties getProperties()
    {
        if (this.properties == null) {
            this.properties = new RTProperties();
        }
        return this.properties;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb= new StringBuffer();
        sb.append(this.getName());
        String auth = this.getAuthorization();
        if (!StringTools.isBlank(auth)) {
            sb.append(" [");
            sb.append(auth);
            sb.append("]");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the location of Cell Tower indicated by the attributes
    *** specified in the CellTower instance.
    *** @param servCT  The serving Cell Tower information
    *** @param nborCT  Neightbor Cell Tower information
    *** @return The Mobile location of the Cell Tower, or null if no
    ***     location could be determined.
    **/
    public abstract MobileLocation getMobileLocation(CellTower servCT, CellTower nborCT[]);

    // ------------------------------------------------------------------------

}
