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
//  2007/06/13  Martin D. Flynn
//     -Initial release
//  2007/11/28  Martin D. Flynn
//     -Added Street#/City/PostalCode getter/setter methods.
//  2008/03/28  Martin D. Flynn
//     -Added CountryCode methods.
//  2008/05/14  Martin D. Flynn
//     -Added StateProvince methods
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import org.opengts.util.*;

public class ReverseGeocode
{
 
    // ------------------------------------------------------------------------

    public static final String COUNTRY_US               = "US";
    public static final String SUBDIVISION_SEPARATOR    = "/";
    public static final String COUNTRY_US_              = COUNTRY_US + SUBDIVISION_SEPARATOR;

    // ------------------------------------------------------------------------

    private String  fullAddress     = null;
    private String  streetAddr      = null;
    private String  city            = null;
    private String  stateProvince   = null;
    private String  postalCode      = null;
    private String  countryCode     = null;
    private String  subdivision     = null;
    private double  speedLimitKPH   = 0.0;
    private int     isTollRoad      = -1;

    public ReverseGeocode()
    {
        super();
    }

    // ------------------------------------------------------------------------
    // Full address

    public void setFullAddress(String address)
    {
        this.fullAddress = (address != null)? address.trim() : null;
    }

    public String getFullAddress()
    {
        return this.fullAddress;
    }

    public boolean hasFullAddress()
    {
        return !StringTools.isBlank(this.fullAddress);
    }
    
    // ------------------------------------------------------------------------
    // Street address

    public void setStreetAddress(String address)
    {
        this.streetAddr = (address != null)? address.trim() : null;
    }

    public String getStreetAddress()
    {
        return this.streetAddr;
    }

    public boolean hasStreetAddress()
    {
        return !StringTools.isBlank(this.streetAddr);
    }

    // ------------------------------------------------------------------------
    // City

    public void setCity(String city)
    {
        this.city = (city != null)? city.trim() : null;
    }
    
    public String getCity()
    {
        return this.city;
    }
    
    public boolean hasCity()
    {
        return !StringTools.isBlank(this.city);
    }

    // ------------------------------------------------------------------------
    // State/Province

    public void setStateProvince(String state)
    {
        this.stateProvince = (state != null)? state.trim() : null;
    }
    
    public String getStateProvince()
    {
        return this.stateProvince;
    }
    
    public boolean hasStateProvince()
    {
        return !StringTools.isBlank(this.stateProvince);
    }

    // ------------------------------------------------------------------------
    // Postal code

    public void setPostalCode(String zip)
    {
        this.postalCode = (zip != null)? zip.trim() : null;
    }
    
    public String getPostalCode()
    {
        return this.postalCode;
    }
    
    public boolean hasPostalCode()
    {
        return !StringTools.isBlank(this.postalCode);
    }

    // ------------------------------------------------------------------------
    // Country
    
    public void setCountryCode(String countryCode)
    {
        this.countryCode = (countryCode != null)? countryCode.trim() : null;
    }
    
    public String getCountryCode()
    {
        return this.hasCountryCode()? this.countryCode : null;
    }
    
    public boolean hasCountryCode()
    {
        return !StringTools.isBlank(this.countryCode);
    }

    // ------------------------------------------------------------------------
    // Subdivision

    public void setSubdivision(String subdiv)
    {
        this.subdivision = (subdiv != null)? subdiv.trim() : null;
    }
    
    public String getSubdivision()
    {
        return this.hasSubdivision()? this.subdivision : null;
    }
    
    public boolean hasSubdivision()
    {
        return !StringTools.isBlank(this.subdivision);
    }

    // ------------------------------------------------------------------------
    // Speed Limit

    public void setSpeedLimitKPH(double limitKPH)
    {
        //Print.logInfo("Set Speed Limit %f", limitKPH);
        this.speedLimitKPH = limitKPH;
    }
    
    public double getSpeedLimitKPH()
    {
        return this.speedLimitKPH;
    }
    
    public boolean hasSpeedLimitKPH()
    {
        return (this.speedLimitKPH > 0.0);
    }

    // ------------------------------------------------------------------------
    // Toll-Road

    public void setIsTollRoad(boolean tollRoad)
    {
        this.isTollRoad = tollRoad? 1 : 0;
    }
    
    public boolean getIsTollRoad()
    {
        return (this.isTollRoad == 1);
    }
    
    public boolean hasIsTollRoad()
    {
        return (this.isTollRoad >= 0);
    }

    // ------------------------------------------------------------------------

    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        if (this.hasFullAddress()) {
            sb.append(this.getFullAddress());
        }
        if (this.hasSubdivision()) {
            if (sb.length() > 0) { 
                sb.append(" ["); 
                sb.append(this.getSubdivision());
                sb.append("]"); 
            } else {
                sb.append(this.getSubdivision());
            }
        }
        if (this.hasSpeedLimitKPH()) {
            double limitKPH = this.getSpeedLimitKPH();
            if (limitKPH >= 900.0) {
                sb.append(" (unlimited speed)");
            } else {
                sb.append(" (limit ");
                sb.append(StringTools.format(limitKPH,"0.0"));
                sb.append(" km/h, ");
                sb.append(StringTools.format(limitKPH*GeoPoint.MILES_PER_KILOMETER,"0.0"));
                sb.append(" mph)");
            }
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

}
