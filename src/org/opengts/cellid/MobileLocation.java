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

public class MobileLocation
    implements GeoPointProvider
{

    // ------------------------------------------------------------------------
    
    public static final String ARG_GPS              = "gps";    // GeoPoint
    public static final String ARG_ACC              = "acc";    // accuracy

    // ------------------------------------------------------------------------

    private GeoPoint    geoPoint    = null;
    private double      accuracyM   = 0.0;

    /**
    *** Private empty constructor
    **/
    private MobileLocation()
    {
        super();
    }

    /**
    *** Copy constructor
    **/
    public MobileLocation(MobileLocation other)
    {
        super();
        if (other != null) {
            this.setGeoPoint(other.getGeoPoint()); // shallow copy
            this.setAccuracy(other.getAccuracy());
        }
    }

    /**
    *** Constructor
    *** @param lat  The latitude
    *** @param lon  The longitude
    **/
    public MobileLocation(double lat, double lon)
    {
        this(lat, lon, 0.0/*accuracy*/);
    }

    /**
    *** Constructor
    *** @param lat       The latitude
    *** @param lon       The longitude
    *** @param accuracy  The uncertainty radius (meters)
    **/
    public MobileLocation(double lat, double lon, double accuracy)
    {
        this(GeoPoint.isValid(lat,lon)?new GeoPoint(lat,lon):GeoPoint.INVALID_GEOPOINT, accuracy);
    }

    /**
    *** Constructor
    *** @param geoPoint  The latitude/longitude
    **/
    public MobileLocation(GeoPoint geoPoint)
    {
        this(geoPoint, 0.0/*accuracy*/);
    }

    /**
    *** Constructor
    *** @param geoPoint  The latitude/longitude
    *** @param accuracy  The uncertainty radius (meters)
    **/
    public MobileLocation(GeoPoint geoPoint, double accuracy)
    {
        this.geoPoint  = GeoPoint.isValid(geoPoint)? geoPoint : GeoPoint.INVALID_GEOPOINT;
        this.accuracyM = accuracy;
    }

    // ------------------------------------------------------------------------
   
    /** 
    *** Sets the Mobile location
    *** @param gps The Cell Tower location
    **/
    public void setGeoPoint(GeoPoint gps)
    {
        this.geoPoint = gps;
    }

    /**
    *** Returns true if a GeoPoint has been defined
    *** @return True if a GeoPoint has been defined
    **/
    public boolean hasGeoPoint()
    {
        return GeoPoint.isValid(this.geoPoint);
    }

    /** 
    *** Gets the Mobile location (if available)
    *** @return The Mobile location (or null if not available)
    **/
    public GeoPoint getGeoPoint()
    {
        return this.geoPoint;
    }
     
    /**
    *** Returns true if the GeoPoint represented by this instance is valid
    *** @return True if the GeoPoint represented by this instance is valid
    **/
    public boolean isValid()
    {
        return GeoPoint.isValid(this.geoPoint);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the accuracy (uncertanty radius) for this Cell Tower location in meters
    *** @param meters  The accuracy (uncertanty radius)
    **/
    public void setAccuracy(double meters)
    {
        this.accuracyM = (meters > 0.0)? meters : 0.0;
    }

    /**
    *** Returns true if a GeoPoint has been defined
    *** @return True if a GeoPoint has been defined
    **/
    public boolean hasAccuracy()
    {
        return (this.accuracyM > 0.0);
    }

    /**
    *** Gets the accuracy (uncertanty radius) for this Cell Tower location in meters
    *** @return  The accuracy (uncertanty radius)
    **/
    public double getAccuracy()
    {
        return this.accuracyM;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Populates the specified RTProperties instance with the contents of this 
    *** MobileLocation
    *** @param rtp  The RTProperties instance to populate
    *** @return The RTProperties instance
    **/
    public RTProperties getRTProperties(RTProperties rtp)
    {
        if (rtp == null) { rtp = new RTProperties(); }
        rtp.setString(ARG_GPS, this.getGeoPoint().toString());
        rtp.setLong(  ARG_ACC, Math.round(this.getAccuracy()));
        return rtp;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation of this instance
    **/
    public String toString()
    {
        return this.getRTProperties(null).toString();
    }
    
    // ------------------------------------------------------------------------

}
