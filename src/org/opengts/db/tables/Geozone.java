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
//  2007/02/26  Martin D. Flynn
//     -Added ability to create client CSV file
//     -Moved to standard released tables package 'org.opengts.db'.
//     -Removed reference to 'GeozoneFactory' (no longer needed)
//  2007/02/28  Martin D. Flynn
//     -Added command line record editor
//     -Added CSV file load/dump
//  2007/03/25  Martin D. Flynn
//     -Moved to 'org.opengts.db.tables'
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//  2007/11/28  Martin D. Flynn
//     -Added columns FLD_reverseGeocode, FLD_city, FLD_postalCode, FLD_subdivision
//     -Added additional lat/lon columns to support polygons.
//     -Added existance check when editing a record
//     -Added option to list geozones defined for an account
//     -Added Geozone inclusion buffer around zones that are also installed on 
//      the client (point-radius, swept-point-radius, bounded-rectangle).
//     -Added an alternate key to FLD_clientID to allow binding a Geozone name to 
//      a departure event
//     -Added '-editall' command-line option to display all fields.
//  2008/02/21  Martin D. Flynn
//     -Reduced size of MAX_RADIUS_METERS to 8200 meters (setting this too large may
//      cause out-of-memory errors).
//  2008/03/28  Martin D. Flynn
//     -Incorporate "DBRecord.select(DBSelect,...) method
//  2008/05/14  Martin D. Flynn
//     -Added FLD_stateProvince, FLD_country, FLD_streetAddress
//  2008/08/15  Martin D. Flynn
//     -Added FLD_minLatitude, FLD_maxLatitude, FLD_minLongitude, FLD_maxLongitude.
//     -Optimized Geozone lookup via bounding box (see 'USE_BOUNDING_BOX').
//     -Added "-update" arg to "-list" option to update existing Geozone bounding boxes.
//  2008/08/24  Martin D. Flynn
//     -Added alternate key to bounding box fields.
//     -Added FLD_arrivalZone, FLD_departureZone
//  2009/05/01  Martin D. Flynn
//     -Fixed bug in "static boolean containsPoint" that threw an exception, instead of
//      returning "false", when a geozone was not found.
//     -Changed 'exists(...)' to allow checking for zones with any sortID.
//  2009/05/24  Martin D. Flynn
//     -Changed "update(...)" to add bounding-box fields to updated list.
//  2009/05/27  Martin D. Flynn
//     -Fixed bug introduced in last release which caused an NPE when saving.
//     -Set default radius to 3000 meter for new Geozones.
//  2009/07/01  Martin D. Flynn
//     -Added an "-export" command-line option.
//  2009/11/01  Martin D. Flynn
//     -Added boolean field "FLD_zoomRegion"
//  2009/12/16  Martin D. Flynn
//     -Added table column FLD_priority (keyed)
//  2010/04/11  Martin D. Flynn
//     -Added table column FLD_shapeColor
//  2010/09/09  Martin D. Flynn
//     -Increased number of points per Geozone to 8
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;

public class Geozone
    extends AccountRecord<Geozone>
{

    // ------------------------------------------------------------------------

    /* USE_BOUNDING_BOX should be true when the min/max Latitude/Longitude fields are in use */
    private static final boolean USE_BOUNDING_BOX           = true;
    private static final boolean ALWAYS_UPDATE_BOUNDS       = true;

    /* When 'USE_BOUNDING_BOX' is false, 'MAX_RADIUS_METERS' is used to optimize DB lookups and 
    ** should be set to a large but reasonable value.
    ** ie. Setting it to cover the entire US is NOT reasonable (you will have memory issues).
    */
    public static final double  MIN_RADIUS_METERS           = 5.0; //
    public static final double  MAX_RADIUS_METERS           = USE_BOUNDING_BOX? 30000.0 : 8191.0; 
    // 8191 meters == ~5 miles (roughly the max DMTP client radius)
    // WARNING: Setting this value too large could result in excessive memory consumption, and 
    // possibly out-of-memory errors.

    // ------------------------------------------------------------------------

    // This value is added to the specified radius when checking for 'client' uploaded geozones.
    // This ensures that the server calculated zone fully encompasses the client zone, making 
    // sure there is no 'grey' areas at the edges due to rounding errors.
    public static final double CLIENT_RADIUS_DELTA_METERS   = 7.0;
    public static final double CLIENT_GEOPOINT_DELTA        = 0.00007;

    // ------------------------------------------------------------------------

    // GeozoneType.POINT_RADIUS: The target point will be tested to see if it is within 'radius' meters 
    //    from each of the 6 points.  If it is within the radius, then this geofence is a match.
    // GeozoneType.BOUNDED_RECT: The largest and smallest lat/lon values will be used to define a 
    //    "bounding rectangle".  If the target point is within this bounding rectangle, then this 
    //    geofence is a match.
    // GeozoneType.BOUNDED_RECT: A circle of 'radius' meters is swept between all available points.  
    //    If the target point is within the area swept out by this circle (ie. the "geocorridor"),  
    //    then this geofence is a match.
    // GeozoneType.POLYGON: For this type of geofence, all points represent a vertex in a polygon.   
    //    If the target point is within this polygon, then this geofence is a match.

    public enum GeozoneType implements EnumTools.StringLocale, EnumTools.IntValue {
        POINT_RADIUS        (0, I18N.getString(Geozone.class,"Geozone.type.pointRadius"     , "PointRadius"     ), true ),
        BOUNDED_RECT        (1, I18N.getString(Geozone.class,"Geozone.type.boundedRectangle", "BoundedRectangle"), false),
        SWEPT_POINT_RADIUS  (2, I18N.getString(Geozone.class,"Geozone.type.sweptPointRadius", "SweptPointRadius"), true ),
        POLYGON             (3, I18N.getString(Geozone.class,"Geozone.type.polygon"         , "Polygon"         ), false);
        private int         vv = 0;
        private I18N.Text   aa = null;
        private boolean     rr = false; // has radius
        GeozoneType(int v, I18N.Text a, boolean r) { vv=v; aa=a; rr=r; }
        public int     getIntValue()            { return vv; }
        public String  toString()               { return aa.toString(); }
        public String  toString(Locale loc)     { return aa.toString(loc); }
        public boolean hasRadius()              { return rr; }
    }

    public static GeozoneType getGeozoneType(Geozone z)
    {
        return (z != null)? EnumTools.getValueOf(GeozoneType.class,z.getZoneType()) : EnumTools.getDefault(GeozoneType.class);
    }

    public static GeozoneType getGeozoneType(int zt)
    {
        return EnumTools.getValueOf(GeozoneType.class,zt);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME                  = "Geozone";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_geozoneID                = "geozoneID";      // 
    public static final String FLD_sortID                   = "sortID";         // ording of this point within the geozone-id
    // bounding box
    public static final String FLD_minLatitude              = "minLatitude";    // min latitude bounding area
    public static final String FLD_maxLatitude              = "maxLatitude";    // max latitude bounding area
    public static final String FLD_minLongitude             = "minLongitude";   // min longitude bounding area
    public static final String FLD_maxLongitude             = "maxLongitude";   // max longitude bounding area
    // geozone flags
    public static final String FLD_reverseGeocode           = "reverseGeocode"; // apply this description as a custom reverse geocode?
    public static final String FLD_arrivalZone              = "arrivalZone";
    public static final String FLD_departureZone            = "departureZone";
    public static final String FLD_autoNotify               = "autoNotify";
    public static final String FLD_zoomRegion               = "zoomRegion";
    public static final String FLD_shapeColor               = "shapeColor";     // shape color
    // geozone definition
    public static final String FLD_zoneType                 = "zoneType";       // POINT_RADIUS, BOUNDED_RECT, ...
    public static final String FLD_radius                   = "radius";         // radius in meters
    public static final String FLD_latitude1                = "latitude1";
    public static final String FLD_longitude1               = "longitude1";
    public static final String FLD_latitude2                = "latitude2";
    public static final String FLD_longitude2               = "longitude2";
    public static final String FLD_latitude3                = "latitude3";
    public static final String FLD_longitude3               = "longitude3";
    public static final String FLD_latitude4                = "latitude4";
    public static final String FLD_longitude4               = "longitude4";
    public static final String FLD_latitude5                = "latitude5";
    public static final String FLD_longitude5               = "longitude5";
    public static final String FLD_latitude6                = "latitude6";
    public static final String FLD_longitude6               = "longitude6";
    public static final String FLD_latitude7                = "latitude7";
    public static final String FLD_longitude7               = "longitude7";
    public static final String FLD_latitude8                = "latitude8";
    public static final String FLD_longitude8               = "longitude8";
  //public static final String FLD_latitude9                = "latitude9";
  //public static final String FLD_longitude9               = "longitude9";
  //public static final String FLD_latitude10               = "latitude10";
  //public static final String FLD_longitude10              = "longitude10";
    public static final String FLD_clientUpload             = "clientUpload";   // upload this geozone to the client device?
    public static final String FLD_clientID                 = "clientID";       // unique numeric ID for this geozone 
    // custom address
    public static final String FLD_streetAddress            = EventData.FLD_streetAddress;
    public static final String FLD_city                     = EventData.FLD_city;
    public static final String FLD_stateProvince            = EventData.FLD_stateProvince;
    public static final String FLD_postalCode               = EventData.FLD_postalCode;
    public static final String FLD_country                  = EventData.FLD_country;
    public static final String FLD_subdivision              = EventData.FLD_subdivision;
    private static DBField FieldInfo[] = {
        // Geozone fields
        AccountRecord.newField_accountID(true,"export=true"),
        new DBField(FLD_geozoneID           , String.class  , DBField.TYPE_ZONE_ID()   , "Geozone ID"       , "key=true export=true"),
        new DBField(FLD_sortID              , Integer.TYPE  , DBField.TYPE_UINT32      , "Sort ID"          , "key=true export=true"),
        // bounding box
        new DBField(FLD_minLatitude         , Double.TYPE   , DBField.TYPE_DOUBLE      , "Min Latitude"     , "edit=2 altkey=bounds format=#0.00000"),
        new DBField(FLD_maxLatitude         , Double.TYPE   , DBField.TYPE_DOUBLE      , "Max Latitude"     , "edit=2 altkey=bounds format=#0.00000"),
        new DBField(FLD_minLongitude        , Double.TYPE   , DBField.TYPE_DOUBLE      , "Min Longitude"    , "edit=2 altkey=bounds format=#0.00000"),
        new DBField(FLD_maxLongitude        , Double.TYPE   , DBField.TYPE_DOUBLE      , "Max Longitude"    , "edit=2 altkey=bounds format=#0.00000"),
        // geozone flags
        new DBField(FLD_reverseGeocode      , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , "Reverse geocode"  , "edit=2 export=true"),
        new DBField(FLD_arrivalZone         , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , "Arrival Zone"     , "edit=2 export=true"),
        new DBField(FLD_departureZone       , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , "Departure Zone"   , "edit=2 export=true"),
        new DBField(FLD_autoNotify          , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , "Auto Notify"      , "edit=2 export=true"),
        new DBField(FLD_zoomRegion          , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , "Zoom Region"      , "edit=2 export=true"),
        new DBField(FLD_shapeColor          , String.class  , DBField.TYPE_STRING(12)  , "Shape Color"      , "edit=2 export=true"),
        // geozone definition
        new DBField(FLD_zoneType            , Integer.TYPE  , DBField.TYPE_UINT8       , "Zone Type"        , "edit=2 enum=Geozone$GeozoneType export=true"),
        new DBField(FLD_radius              , Integer.TYPE  , DBField.TYPE_UINT32      , "Radius Meters"    , "edit=2 export=true"),
        new DBField(FLD_latitude1           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 1"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude1          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 1"      , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_latitude2           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 2"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude2          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 2"      , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_latitude3           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 3"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude3          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 3"      , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_latitude4           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 4"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude4          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 4"      , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_latitude5           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 5"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude5          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 5"      , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_latitude6           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 6"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude6          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 6"      , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_latitude7           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 7"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude7          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 7"      , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_latitude8           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 8"       , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_longitude8          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 8"      , "edit=2 format=#0.00000 export=true"),
      //new DBField(FLD_latitude9           , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 9"       , "edit=2 format=#0.00000 export=true"),
      //new DBField(FLD_longitude9          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 9"      , "edit=2 format=#0.00000 export=true"),
      //new DBField(FLD_latitude10          , Double.TYPE   , DBField.TYPE_DOUBLE      , "Latitude 10"      , "edit=2 format=#0.00000 export=true"),
      //new DBField(FLD_longitude10         , Double.TYPE   , DBField.TYPE_DOUBLE      , "Longitude 10"     , "edit=2 format=#0.00000 export=true"),
        new DBField(FLD_clientUpload        , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , "Client Upload"    , "edit=2"),
        new DBField(FLD_clientID            , Integer.TYPE  , DBField.TYPE_UINT32      , "Client ID"        , "edit=2 altkey=true"), // "ORDER BY"
        // Address fields
        new DBField(FLD_streetAddress       , String.class  , DBField.TYPE_STRING(90)  , "Street Address"   , "edit=2 utf8=true export=true"),
        new DBField(FLD_city                , String.class  , DBField.TYPE_STRING(40)  , "City"             , "edit=2 utf8=true export=true"),
        new DBField(FLD_stateProvince       , String.class  , DBField.TYPE_STRING(40)  , "State/Province"   , "edit=2 utf8=true export=true"),
        new DBField(FLD_postalCode          , String.class  , DBField.TYPE_STRING(16)  , "Postal Code"      , "edit=2 utf8=true export=true"),
        new DBField(FLD_country             , String.class  , DBField.TYPE_STRING(40)  , "Country"          , "edit=2 utf8=true export=true"),
        new DBField(FLD_subdivision         , String.class  , DBField.TYPE_STRING(32)  , "Subdivision"      , "edit=2 utf8=true export=true"),
        // Common fields
        newField_displayName("export=true"),
        newField_description("export=true"),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };
    // Optional fields
    public static final String FLD_priority                 = "priority";
    public static final String FLD_speedLimitKPH            = "speedLimitKPH"; // in-zone speed limit ('0' for unavailable)
    public static final DBField PriorityFieldInfo[] = {
        // Overlap Priority
        new DBField(FLD_priority            , Integer.TYPE  , DBField.TYPE_UINT32      , "Priority"             , "edit=2 altkey=priority export=true"),
        // In-zone speed limit
        new DBField(FLD_speedLimitKPH       , Double.TYPE   , DBField.TYPE_DOUBLE      , "Speed Limit"          , "format=#0.0 units=speed"),
    };
    // Corridor fields
    public static final String FLD_corrStartSelector        = "corrStartSelector";
    public static final String FLD_corrEndSelector          = "corrEndSelector";
    public static final String FLD_corridorID               = "corridorID";
    public static final DBField CorridorFieldInfo[] = {
       // GeoCorridor ID
        new DBField(FLD_corridorID          , String.class  , DBField.TYPE_CORR_ID()   , "Corridor ID"          , "edit=2"),
        new DBField(FLD_corrStartSelector   , String.class  , DBField.TYPE_TEXT        , "Corridor Start Select", "edit=2"),
        new DBField(FLD_corrEndSelector     , String.class  , DBField.TYPE_TEXT        , "Corridor End Select"  , "edit=2"),
    };

    /* key class */
    public static class Key
        extends AccountKey<Geozone>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String zoneId, int sortId) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_geozoneID , ((zoneId != null)? zoneId.toLowerCase() : ""));
            if (sortId >= 0) {
                super.setFieldValue(FLD_sortID, sortId);
            }
        }
        public DBFactory<Geozone> getFactory() {
            return Geozone.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Geozone> factory = null;
    public static DBFactory<Geozone> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Geozone.TABLE_NAME(),
                Geozone.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                Geozone.class, 
                Geozone.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            Geozone._initZoneTypes();
        }
        return factory;
    }

    /* Bean instance */
    public Geozone()
    {
        super();
    }

    /* database record */
    public Geozone(Geozone.Key key)
    {
        super(key);
        // init?
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Geozone.class, loc);
        return i18n.getString("Geozone.description", 
            "This table defines " +
            "Account specific geozones/geofences."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    public String getGeozoneID()
    {
        String v = (String)this.getFieldValue(FLD_geozoneID);
        return StringTools.trim(v);
    }
    
    public void setGeozoneID(String v)
    {
        this.setFieldValue(FLD_geozoneID, StringTools.trim(v));
    }
    
    // ------------------------------------------------------------------------

    public int getSortID()
    {
        Integer v = (Integer)this.getFieldValue(FLD_sortID);
        return (v != null)? v.intValue() : 0;
    }
    
    public void setSortID(int v)
    {
        this.setFieldValue(FLD_sortID, v);
    }
    
    // ------------------------------------------------------------------------

    public static boolean supportsPriority()
    {
        return Geozone.getFactory().hasField(FLD_priority);
    }
    
    public int getPriority()
    {
        Integer v = (Integer)this.getFieldValue(FLD_priority);
        return (v != null)? v.intValue() : 0;
    }

    public void setPriority(int v)
    {
        this.setFieldValue(FLD_priority, v);
    }

    // ------------------------------------------------------------------------

    public static boolean supportsSpeedLimitKPH()
    {
        return Geozone.getFactory().hasField(FLD_speedLimitKPH);
    }

    /* get speed limit */
    public double getSpeedLimitKPH()
    {
        return this.getFieldValue(FLD_speedLimitKPH, 0.0);
    }
    
    /* set speed limit */
    public void setSpeedLimitKPH(double v)
    {
        this.setFieldValue(FLD_speedLimitKPH, ((v > 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    /* get min latitude */
    public double getMinLatitude()
    {
        Double v = (Double)this.getFieldValue(FLD_minLatitude);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set min latitude */
    public void setMinLatitude(double v)
    {
        this.setFieldValue(FLD_minLatitude, v);
    }

    // ------------------------------------------------------------------------

    /* get max latitude */
    public double getMaxLatitude()
    {
        Double v = (Double)this.getFieldValue(FLD_maxLatitude);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set max latitude */
    public void setMaxLatitude(double v)
    {
        this.setFieldValue(FLD_maxLatitude, v);
    }

    // ------------------------------------------------------------------------

    /* get min longitude */
    public double getMinLongitude()
    {
        Double v = (Double)this.getFieldValue(FLD_minLongitude);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set min longitude */
    public void setMinLongitude(double v)
    {
        this.setFieldValue(FLD_minLongitude, v);
    }

    // ------------------------------------------------------------------------

    /* get max longitude */
    public double getMaxLongitude()
    {
        Double v = (Double)this.getFieldValue(FLD_maxLongitude);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set max longitude */
    public void setMaxLongitude(double v)
    {
        this.setFieldValue(FLD_maxLongitude, v);
    }

    // ------------------------------------------------------------------------

    public int getZoneType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_zoneType);
        return (v != null)? v.intValue() : EnumTools.getDefault(GeozoneType.class).getIntValue();
    }
    
    public void setZoneType(int v)
    {
        this.setFieldValue(FLD_zoneType, EnumTools.getValueOf(GeozoneType.class,v).getIntValue());
    }

    public void setZoneType(GeozoneType v)
    {
        this.setFieldValue(FLD_zoneType, EnumTools.getValueOf(GeozoneType.class,v).getIntValue());
    }

    public void setZoneType(String v, Locale locale)
    {
        this.setFieldValue(FLD_zoneType, EnumTools.getValueOf(GeozoneType.class,v,locale).getIntValue());
    }

    public String getZoneTypeDescription(Locale loc)
    {
        return Geozone.getGeozoneType(this).toString(loc);
    }

    // ------------------------------------------------------------------------

    /* return true if the radius will be used for this Geozone */
    public boolean hasRadius()
    {
        return Geozone.getGeozoneType(this).hasRadius();
    }

    /* return the radius in meters */
    public double getRadiusMeters()
    {
        return (double)this.getRadius();
    }

    /* return the radius in meters */
    public double getRadiusMeters(double minValue, double maxValue)
    {
        double radM = (double)this.getRadius();
        if ((minValue >= 0.0) && (radM < minValue)) {
            return minValue;
        } else
        if ((maxValue >= 0.0) && (radM > maxValue)) {
            return maxValue;
        } else {
            return radM;
        }
    }

    /* return the radius in kilometers */
    public double getRadiusKilometers()
    {
        return (double)this.getRadius() / 1000.0;
    }

    /* return the radius in meters */
    public int getRadius()
    {
        Integer v = (Integer)this.getFieldValue(FLD_radius);
        return (v != null)? v.intValue() : 0;
    }
    
    /* set the radius in meters */
    public void setRadius(int v)
    {
        this.setFieldValue(FLD_radius, v);
        this.setZoneChanged();
    }
    
    /* sets the default radius for the geozone type */
    public void setDefaultRadius()
    {
        int gzType = this.getZoneType();
        if (gzType == GeozoneType.POINT_RADIUS.getIntValue()) {
            int radM = RTConfig.getInt(DBConfig.PROP_Geozone_dftRadius_pointRadius, 3000);
            this.setRadius(radM);
        } else
        if (gzType == GeozoneType.POLYGON.getIntValue()) {
            int radM = RTConfig.getInt(DBConfig.PROP_Geozone_dftRadius_polygon, 500);
            this.setRadius(radM);    // this is ignored for polygons anyway
        } else
        if (gzType == GeozoneType.SWEPT_POINT_RADIUS.getIntValue()) {
            int radM = RTConfig.getInt(DBConfig.PROP_Geozone_dftRadius_sweptPointRadius, 1000);
            this.setRadius(1000);
        } else {
            int radM = RTConfig.getInt(DBConfig.PROP_Geozone_dftRadius_pointRadius, 3000);
            this.setRadius(radM);
        }
    }

    // ------------------------------------------------------------------------

    /* gets the color of this Geozone when drawn as a shape on the map */
    public String getShapeColor(String dftColor)
    {
        // Geozone color
        String color = this.getShapeColor();
        if (!StringTools.isBlank(color)) {
            if (!ColorTools.isColor(color)) {
                Print.logError("Invalid Color value: " + color);
            }
            return color;
        }
        // Default color
        if (!StringTools.isBlank(dftColor)) {
            if (!ColorTools.isColor(dftColor)) {
                Print.logError("Invalid Default Color value: " + dftColor);
            }
        }
        return dftColor;
    }

    /* gets the color of this Geozone when drawn as a shape on the map */
    public String getShapeColor()
    {
        String v = (String)this.getFieldValue(FLD_shapeColor);
        return StringTools.trim(v);
    }

    /* sets the color of this Geozone when drawn as a shape on the map */
    public void setShapeColor(String v)
    {
        this.setFieldValue(FLD_shapeColor, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get the Northern most latitude of the first 2 points */
    public double getNorthLatitude()
    {
        // for BoundingRectangle use only
        double lat1 = this.getLatitude1();
        double lat2 = this.getLatitude2();
        return (lat1 >= lat2)? lat1 : lat2;
    }

    /* get the Southern most latitude of the first 2 points */
    public double getSouthLatitude()
    {
        // for BoundingRectangle use only
        double lat1 = this.getLatitude1();
        double lat2 = this.getLatitude2();
        return (lat1 <= lat2)? lat1 : lat2;
    }

    /* get the Western most longitude of the first 2 points */
    public double getWestLongitude()
    {
        // for BoundingRectangle use only
        double lon1 = this.getLongitude1();
        double lon2 = this.getLongitude2();
        return (lon1 <= lon2)? lon1 : lon2;
    }

    /* get the Eastern most longitude of the first 2 points */
    public double getEastLongitude()
    {
        // for BoundingRectangle use only
        double lon1 = this.getLongitude1();
        double lon2 = this.getLongitude2();
        return (lon1 >= lon2)? lon1 : lon2;
    }

    /* get latitude #1 */
    public double getLatitude1()
    {
        // BoundingRect: North point
        Double v = (Double)this.getFieldValue(FLD_latitude1);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #1 */
    public void setLatitude1(double v)
    {
        // BoundingRect: North point
        this.setFieldValue(FLD_latitude1, v);
        this.setZoneChanged();
    }

    /* get longitude #1 */
    public double getLongitude1()
    {
        // BoundingRect: West point
        Double v = (Double)this.getFieldValue(FLD_longitude1);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #1 */
    public void setLongitude1(double v)
    {
        // BoundingRect: West point
        this.setFieldValue(FLD_longitude1, v);
        this.setZoneChanged();
    }

    /* get geopoint #1 */
    public GeoPoint getGeoPoint1()
    {
        // BoundingRect: NorthWest point
        return new GeoPoint(this.getLatitude1(), this.getLongitude1());
    }

    /* set geopoint #1 */
    public void setGeoPoint1(GeoPoint gp)
    {
        // BoundingRect: NorthWest point
        if (gp != null) {
            this.setLatitude1(gp.getLatitude());
            this.setLongitude1(gp.getLongitude());
        } else {
            this.setLatitude1(0.0);
            this.setLongitude1(0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* get latitude #2 */
    public double getLatitude2()
    {
        // BoundingRect: South point
        Double v = (Double)this.getFieldValue(FLD_latitude2);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #2 */
    public void setLatitude2(double v)
    {
        // BoundingRect: South point
        this.setFieldValue(FLD_latitude2, v);
        this.setZoneChanged();
    }

    /* get longitude #2 */
    public double getLongitude2()
    {
        // BoundingRect: East point
        Double v = (Double)this.getFieldValue(FLD_longitude2);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #2 */
    public void setLongitude2(double v)
    {
        // BoundingRect: East point
        this.setFieldValue(FLD_longitude2, v);
        this.setZoneChanged();
    }

    /* get geopoint #2 */
    public GeoPoint getGeoPoint2()
    {
        // BoundingRect: SouthEast point
        return new GeoPoint(this.getLatitude2(), this.getLongitude2());
    }

    /* set geopoint #2 */
    public void setGeoPoint2(GeoPoint gp)
    {
        // BoundingRect: SouthEast point
        if (gp != null) {
            this.setLatitude2(gp.getLatitude());
            this.setLongitude2(gp.getLongitude());
        } else {
            this.setLatitude2(0.0);
            this.setLongitude2(0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* get latitude #3 */
    public double getLatitude3()
    {
        Double v = (Double)this.getFieldValue(FLD_latitude3);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #3 */
    public void setLatitude3(double v)
    {
        this.setFieldValue(FLD_latitude3, v);
        this.setZoneChanged();
    }

    /* get longitude #3 */
    public double getLongitude3()
    {
        Double v = (Double)this.getFieldValue(FLD_longitude3);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #3 */
    public void setLongitude3(double v)
    {
        this.setFieldValue(FLD_longitude3, v);
        this.setZoneChanged();
    }

    /* get geopoint #3 */
    public GeoPoint getGeoPoint3()
    {
        return new GeoPoint(this.getLatitude3(), this.getLongitude3());
    }

    /* set geopoint #3 */
    public void setGeoPoint3(GeoPoint gp)
    {
        if (gp != null) {
            this.setLatitude3(gp.getLatitude());
            this.setLongitude3(gp.getLongitude());
        } else {
            this.setLatitude3(0.0);
            this.setLongitude3(0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* get latitude #4 */
    public double getLatitude4()
    {
        Double v = (Double)this.getFieldValue(FLD_latitude4);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #4 */
    public void setLatitude4(double v)
    {
        this.setFieldValue(FLD_latitude4, v);
        this.setZoneChanged();
    }

    /* get longitude #4 */
    public double getLongitude4()
    {
        Double v = (Double)this.getFieldValue(FLD_longitude4);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #4 */
    public void setLongitude4(double v)
    {
        this.setFieldValue(FLD_longitude4, v);
        this.setZoneChanged();
    }

    /* get geopoint #4 */
    public GeoPoint getGeoPoint4()
    {
        return new GeoPoint(this.getLatitude4(), this.getLongitude4());
    }

    /* set geopoint #4 */
    public void setGeoPoint4(GeoPoint gp)
    {
        if (gp != null) {
            this.setLatitude4(gp.getLatitude());
            this.setLongitude4(gp.getLongitude());
        } else {
            this.setLatitude4(0.0);
            this.setLongitude4(0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* get latitude #5 */
    public double getLatitude5()
    {
        Double v = (Double)this.getFieldValue(FLD_latitude5);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #5 */
    public void setLatitude5(double v)
    {
        this.setFieldValue(FLD_latitude5, v);
        this.setZoneChanged();
    }

    /* get longitude #5 */
    public double getLongitude5()
    {
        Double v = (Double)this.getFieldValue(FLD_longitude5);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #5 */
    public void setLongitude5(double v)
    {
        this.setFieldValue(FLD_longitude5, v);
        this.setZoneChanged();
    }

    /* get geopoint #5 */
    public GeoPoint getGeoPoint5()
    {
        return new GeoPoint(this.getLatitude5(), this.getLongitude5());
    }

    /* set geopoint #5 */
    public void setGeoPoint5(GeoPoint gp)
    {
        if (gp != null) {
            this.setLatitude5(gp.getLatitude());
            this.setLongitude5(gp.getLongitude());
        } else {
            this.setLatitude5(0.0);
            this.setLongitude5(0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* get latitude #6 */
    public double getLatitude6()
    {
        Double v = (Double)this.getFieldValue(FLD_latitude6);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #6 */
    public void setLatitude6(double v)
    {
        this.setFieldValue(FLD_latitude6, v);
        this.setZoneChanged();
    }

    /* get longitude #6 */
    public double getLongitude6()
    {
        Double v = (Double)this.getFieldValue(FLD_longitude6);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #6 */
    public void setLongitude6(double v)
    {
        this.setFieldValue(FLD_longitude6, v);
        this.setZoneChanged();
    }

    /* get geopoint #6 */
    public GeoPoint getGeoPoint6()
    {
        return new GeoPoint(this.getLatitude6(), this.getLongitude6());
    }

    /* set geopoint #6 */
    public void setGeoPoint6(GeoPoint gp)
    {
        if (gp != null) {
            this.setLatitude6(gp.getLatitude());
            this.setLongitude6(gp.getLongitude());
        } else {
            this.setLatitude6(0.0);
            this.setLongitude6(0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* get latitude #7 */
    public double getLatitude7()
    {
        Double v = (Double)this.getFieldValue(FLD_latitude7);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #7 */
    public void setLatitude7(double v)
    {
        this.setFieldValue(FLD_latitude7, v);
        this.setZoneChanged();
    }

    /* get longitude #7 */
    public double getLongitude7()
    {
        Double v = (Double)this.getFieldValue(FLD_longitude7);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #7 */
    public void setLongitude7(double v)
    {
        this.setFieldValue(FLD_longitude7, v);
        this.setZoneChanged();
    }

    /* get geopoint #7 */
    public GeoPoint getGeoPoint7()
    {
        return new GeoPoint(this.getLatitude7(), this.getLongitude7());
    }

    /* set geopoint #7 */
    public void setGeoPoint7(GeoPoint gp)
    {
        if (gp != null) {
            this.setLatitude7(gp.getLatitude());
            this.setLongitude7(gp.getLongitude());
        } else {
            this.setLatitude7(0.0);
            this.setLongitude7(0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* get latitude #8 */
    public double getLatitude8()
    {
        Double v = (Double)this.getFieldValue(FLD_latitude8);
        return (v != null)? v.doubleValue() : 0.0;
    }
    
    /* set latitude #8 */
    public void setLatitude8(double v)
    {
        this.setFieldValue(FLD_latitude8, v);
        this.setZoneChanged();
    }

    /* get longitude #8 */
    public double getLongitude8()
    {
        Double v = (Double)this.getFieldValue(FLD_longitude8);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /* set longitude #8 */
    public void setLongitude8(double v)
    {
        this.setFieldValue(FLD_longitude8, v);
        this.setZoneChanged();
    }

    /* get geopoint #8 */
    public GeoPoint getGeoPoint8()
    {
        return new GeoPoint(this.getLatitude8(), this.getLongitude8());
    }

    /* set geopoint #8 */
    public void setGeoPoint8(GeoPoint gp)
    {
        if (gp != null) {
            this.setLatitude8(gp.getLatitude());
            this.setLongitude8(gp.getLongitude());
        } else {
            this.setLatitude8(0.0);
            this.setLongitude8(0.0);
        }
    }

    // ------------------------------------------------------------------------

    private static String GeoPointFields[][] = new String[][] {
        { FLD_latitude1 , FLD_longitude1  },
        { FLD_latitude2 , FLD_longitude2  },
        { FLD_latitude3 , FLD_longitude3  },
        { FLD_latitude4 , FLD_longitude4  },
        { FLD_latitude5 , FLD_longitude5  },
        { FLD_latitude6 , FLD_longitude6  },
        { FLD_latitude7 , FLD_longitude7  },
        { FLD_latitude8 , FLD_longitude8  },
      //{ FLD_latitude9 , FLD_longitude9  },
      //{ FLD_latitude10, FLD_longitude10 },
    };
    
    /* get number of GeoPoints */
    public static int GetGeoPointCount()
    {
        return GeoPointFields.length;
    }

    /* get latitude */
    public double getLatitude(int ndx)
    {
        if ((ndx >= 0) && (ndx < Geozone.GetGeoPointCount())) {
            Double v = (Double)this.getFieldValue(GeoPointFields[ndx][0]);
            return (v != null)? v.doubleValue() : 0.0;
        } else {
            return 0.0;
        }
    }
    
    /* set latitude */
    public void setLatitude(int ndx, double v)
    {
        if ((ndx >= 0) && (ndx < Geozone.GetGeoPointCount())) {
            this.setFieldValue(GeoPointFields[ndx][0], v); // FLD_latitude#
            this.setZoneChanged();
        }
    }

    /* get longitude */
    public double getLongitude(int ndx)
    {
        if ((ndx >= 0) && (ndx < Geozone.GetGeoPointCount())) {
            Double v = (Double)this.getFieldValue(GeoPointFields[ndx][1]);
            return (v != null)? v.doubleValue() : 0.0;
        } else {
            return 0.0;
        }
    }

    /* set longitude */
    public void setLongitude(int ndx, double v)
    {
        if ((ndx >= 0) && (ndx < Geozone.GetGeoPointCount())) {
            this.setFieldValue(GeoPointFields[ndx][1], v); // FLD_longitude#
            this.setZoneChanged();
        }
    }

    /* return specified GeoPoint */
    public GeoPoint getGeoPoint(int ndx)
    {
        return this.getGeoPoint(ndx,null);
    }

    /* return specified GeoPoint */
    public GeoPoint getGeoPoint(int ndx, GeoPoint dft)
    {
        if ((ndx >= 0) && (ndx < Geozone.GetGeoPointCount())) {
            double lat = this.getLatitude( ndx);
            double lon = this.getLongitude(ndx);
            return GeoPoint.isValid(lat,lon)? new GeoPoint(lat,lon) : dft;
        } else {
            return dft;
        }
    }

    /* get all valid GeoPoints */
    public GeoPoint[] getGeoPoints()
    {
        java.util.List<GeoPoint> gpList = new Vector<GeoPoint>();
        int geoCnt = Geozone.GetGeoPointCount();
        for (int i = 0; i < geoCnt; i++) {
            double lat = this.getLatitude( i);
            double lon = this.getLongitude(i);
            if (GeoPoint.isValid(lat,lon)) {
                gpList.add(new GeoPoint(lat,lon));
            }
        }
        return gpList.toArray(new GeoPoint[gpList.size()]);
    }
    
    /* set latitude/longitude */
    public void setGeoPoint(int ndx, GeoPoint gp)
    {
        if ((gp != null) && gp.isValid()) {
            this.setLatitude( ndx, gp.getLatitude());
            this.setLongitude(ndx, gp.getLongitude());
        } else {
            this.setLatitude( ndx, 0.0);
            this.setLongitude(ndx, 0.0);
        }
    }
    
    /* set latitude/longitude */
    public void setGeoPoint(int ndx, double lat, double lon)
    {
        if (GeoPoint.isValid(lat,lon)) {
            this.setLatitude( ndx, lat);
            this.setLongitude(ndx, lon);
        } else {
            this.setLatitude( ndx, 0.0);
            this.setLongitude(ndx, 0.0);
        }
    }

    /* set GeoPoints */
    public void setGeoPoints(GeoPoint gp[])
    {
        int geoCnt = Geozone.GetGeoPointCount();
        for (int i = 0; i < geoCnt; i++) {
            if ((gp != null) && (i < gp.length)) {
                this.setLatitude( i, gp[i].getLatitude() );
                this.setLongitude(i, gp[i].getLongitude());
            } else {
                this.setLatitude( i, 0.0);
                this.setLongitude(i, 0.0);
            }
        }
    }

    /* clear GeoPoints */
    public void clearGeoPoints()
    {
        int geoCnt = Geozone.GetGeoPointCount();
        for (int i = 0; i < geoCnt; i++) {
            this.setLatitude( i, 0.0);
            this.setLongitude(i, 0.0);
        }
    }

    // ------------------------------------------------------------------------

    /* return true if this Geozone should be uploaded to the client */
    public boolean getClientUpload()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_clientUpload);
        return (v != null)? v.booleanValue() : false;
    }

    public void setClientUpload(boolean v)
    {
        this.setFieldValue(FLD_clientUpload, v);
    }
    
    public boolean isClientUpload()
    {
        return this.getClientUpload();
    }
    
    // ------------------------------------------------------------------------

    public int getClientID()
    {
        Integer v = (Integer)this.getFieldValue(FLD_clientID);
        return (v != null)? v.intValue() : 0;
    }
    
    public void setClientID(int v)
    {
        this.setFieldValue(FLD_clientID, v);
    }

    // ------------------------------------------------------------------------

    public boolean getReverseGeocode()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_reverseGeocode);
        return (v != null)? v.booleanValue() : false;
    }

    public void setReverseGeocode(boolean v)
    {
        this.setFieldValue(FLD_reverseGeocode, v);
    }
    
    public boolean isReverseGeocode()
    {
        return this.getReverseGeocode();
    }

    // ------------------------------------------------------------------------

    public boolean getArrivalZone()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_arrivalZone);
        return (v != null)? v.booleanValue() : false;
    }

    public void setArrivalZone(boolean v)
    {
        this.setFieldValue(FLD_arrivalZone, v);
    }
    
    public boolean isArrivalZone()
    {
        return this.getArrivalZone();
    }

    // ------------------------------------------------------------------------

    public boolean getDepartureZone()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_departureZone);
        return (v != null)? v.booleanValue() : false;
    }

    public void setDepartureZone(boolean v)
    {
        this.setFieldValue(FLD_departureZone, v);
    }
    
    public boolean isDepartureZone()
    {
        return this.getDepartureZone();
    }

    // ------------------------------------------------------------------------

    public boolean getAutoNotify()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_autoNotify);
        return (v != null)? v.booleanValue() : false;
    }

    public void setAutoNotify(boolean v)
    {
        this.setFieldValue(FLD_autoNotify, v);
    }
    
    public boolean isAutoNotify()
    {
        return this.getAutoNotify();
    }

    // ------------------------------------------------------------------------

    public boolean getZoomRegion()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_zoomRegion);
        return (v != null)? v.booleanValue() : false;
    }

    public void setZoomRegion(boolean v)
    {
        this.setFieldValue(FLD_zoomRegion, v);
    }
    
    public boolean isZoomRegion()
    {
        return this.getZoomRegion();
    }

    // ------------------------------------------------------------------------

    public String getStreetAddress()
    {
        String v = (String)this.getFieldValue(FLD_streetAddress);
        return StringTools.trim(v);
    }
    
    public void setStreetAddress(String v)
    {
        this.setFieldValue(FLD_streetAddress, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getCity()
    {
        String v = (String)this.getFieldValue(FLD_city);
        return StringTools.trim(v);
    }
    
    public void setCity(String v)
    {
        this.setFieldValue(FLD_city, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getStateProvince()
    {
        String v = (String)this.getFieldValue(FLD_stateProvince);
        return StringTools.trim(v);
    }
    
    public void setStateProvince(String v)
    {
        this.setFieldValue(FLD_stateProvince, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getPostalCode()
    {
        String v = (String)this.getFieldValue(FLD_postalCode);
        return StringTools.trim(v);
    }
    
    public void setPostalCode(String v)
    {
        this.setFieldValue(FLD_postalCode, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getCountry()
    {
        String v = (String)this.getFieldValue(FLD_country);
        return StringTools.trim(v);
    }
    
    public void setCountry(String v)
    {
        this.setFieldValue(FLD_country, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getSubdivision()
    {
        String v = (String)this.getFieldValue(FLD_subdivision);
        return StringTools.trim(v);
    }
    
    public void setSubdivision(String v)
    {
        this.setFieldValue(FLD_subdivision, StringTools.trim(v));
    }
   
    // ------------------------------------------------------------------------

    public static boolean supportsCorridor()
    {
        return Geozone.getFactory().hasField(FLD_corridorID);
    }

    public String getCorridorID()
    {
        String v = (String)this.getFieldValue(FLD_corridorID);
        return StringTools.trim(v);
    }
    
    public void setCorridorID(String v)
    {
        this.setFieldValue(FLD_corridorID, StringTools.trim(v));
    }

    public boolean hasCorridorID()
    {
        return !StringTools.isBlank(this.getCorridorID());
    }

    // ------------------------------------------------------------------------

    public String getCorrStartSelector()
    {
        String v = (String)this.getFieldValue(FLD_corrStartSelector);
        return StringTools.trim(v);
    }

    public void setCorrStartSelector(boolean v)
    {
        this.setFieldValue(FLD_corrStartSelector, v);
    }

    public boolean isCorridorStart(EventData ev)
    {
        if (!this.supportsCorridor()) {
            // corridors are not supported in this version
            return false;
        } else
        if ((ev == null) || (ev.getStatusCode() != StatusCodes.STATUS_GEOFENCE_DEPART)) {
            // not a geozone depart event
            return false;
        } else
        if (!this.hasCorridorID()) {
            // no corridor-id defined
            return false;
        } else
        if (!Device.hasRuleFactory()) {
            // no RuleFactory, simply assume "true" if the selector is non-blank
            String sel = this.getCorrStartSelector();
            return !StringTools.isBlank(sel);
        } else {
            // evaluate selector
            String sel = this.getCorrStartSelector();
            if (StringTools.isBlank(sel)) {
                // no selector
                return false;
            } else {
                RuleFactory rf = Device.getRuleFactory(); // not null
                return rf.isSelectorMatch(sel, ev);
            }
        }
    }

    // ------------------------------------------------------------------------

    public String getCorrEndSelector()
    {
        String v = (String)this.getFieldValue(FLD_corrEndSelector);
        return StringTools.trim(v);
    }

    public void setCorrEndSelector(boolean v)
    {
        this.setFieldValue(FLD_corrEndSelector, v);
    }

    public boolean isCorridorEnd(EventData ev)
    {
        if (!this.supportsCorridor()) {
            // corridors are not supported in this version
            return false;
        } else
        if ((ev == null) || (ev.getStatusCode() != StatusCodes.STATUS_GEOFENCE_ARRIVE)) {
            // not a geozone arrive event
            return false;
        } else
        if (!Device.hasRuleFactory()) {
            // no RuleFactory, simply assume "true" if the selector is non-blank
            String sel = this.getCorrEndSelector();
            return !StringTools.isBlank(sel);
        } else {
            // evaluate selector
            String sel = this.getCorrEndSelector();
            if (StringTools.isBlank(sel)) {
                // no selector
                return false;
            } else {
                RuleFactory rf = Device.getRuleFactory(); // not null
                return rf.isSelectorMatch(sel, ev);
            }
        }
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription("Custom Zone");
        this.setReverseGeocode(true);
        this.setArrivalZone(true);
        this.setDepartureZone(true);
        this.setRadius(3000); // should be 1000 for corridors
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    private static GeozoneChecker geozoneMultiPointRadius = null;
    private static GeozoneChecker geozoneSweptPointRadius = null;
    private static GeozoneChecker geozoneBoundedRectangle = null;
    private static GeozoneChecker geozonePolygon          = null;

    private static void _initZoneTypes()
    {
        StringBuffer sb = new StringBuffer();
        
        /* GeozoneType.POINT_RADIUS: standard point radius */
        geozoneMultiPointRadius = GeoPoint.getGeozoneChecker();
        sb.append("PointRadius");
        
        /* GeozoneType.POLYGON: optional polygon */
        geozonePolygon = GeoPolygon.getGeozoneChecker();
        sb.append(",Polygon");
        //try {
        //    MethodAction ma = new MethodAction(DBConfig.PACKAGE_RULE_UTIL_ + "GeoPolygon", "getGeozoneChecker");
        //    geozonePolygon = (GeozoneChecker)ma.invoke();
        //    sb.append(",Polygon");
        //} catch (Throwable th) {
        //    geozonePolygon = null;
        //}

        /* GeozoneType.BOUNDED_RECT: standard bounded rectangle */
        geozoneBoundedRectangle = new GeozoneChecker() {
            public boolean containsPoint(GeoPoint gpTest, GeoPoint gpList[], double radiusKM) {
                if (gpList.length < 2) { return false; }
                double latN =  -90.0, latS =   90.0;
                double lonW =  180.0, lonE = -180.0;
                for (int i = 0; i < gpList.length; i++) {
                    double lat = gpList[i].getLatitude();
                    if (lat > latN) { latN = lat; }
                    if (lat < latS) { latS = lat; }
                    double lon = gpList[i].getLongitude();
                    if (lon > lonE ) { lonE  = lon; }
                    if (lon < lonW ) { lonW  = lon; }
                }
                double lat = gpTest.getLatitude();
                double lon = gpTest.getLongitude();
                if (lat > (latN + CLIENT_GEOPOINT_DELTA)) { return false; } // North/Top
                if (lat < (latS - CLIENT_GEOPOINT_DELTA)) { return false; } // South/Bottom
                if (lon < (lonW - CLIENT_GEOPOINT_DELTA)) { return false; } // West/Left  (fails if zone spans +/-180 deg)
                if (lon > (lonE + CLIENT_GEOPOINT_DELTA)) { return false; } // East/Right (fails if zone spans +/-180 deg)
                return true; // success
            }
        };
        sb.append(",Rectangle");
        
        /* GeozoneType.SWEPT_POINT_RADIUS: optional swept point radius */
        try {
            MethodAction ma = new MethodAction(DBConfig.PACKAGE_RULE_UTIL_ + "GeoSegment", "getGeozoneChecker");
            geozoneSweptPointRadius = (GeozoneChecker)ma.invoke();
            sb.append(",SweptPointRadius");
        } catch (Throwable th) {
            geozoneSweptPointRadius = null;
        }
        
        /* display supported Geozone types */
        //Print.logDebug("Supported Geozone types: " + sb);

    }
    
    public static boolean IsGeozoneTypeSupported(int type)
    {
        return IsGeozoneTypeSupported(Geozone.getGeozoneType(type));
    }
    
    public static boolean IsGeozoneTypeSupported(GeozoneType type)
    {
        switch (type) {
            case POINT_RADIUS        : return (geozoneMultiPointRadius != null);
            case BOUNDED_RECT        : return (geozoneBoundedRectangle != null);
            case SWEPT_POINT_RADIUS  : return (geozoneSweptPointRadius != null);
            case POLYGON             : return (geozonePolygon          != null);
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /* return true if this geozone contains the specified point */
    public boolean containsPoint(GeoPoint gp)
    {

        /* null/invalid GeoPoint? */
        if ((gp == null) || !gp.isValid()) {
            return false;
        }

        /* determine inclusion in Geozone based on zone type */
        GeoPoint gzPts[] = this.getGeoPoints();
        switch (Geozone.getGeozoneType(this)) {
            case POINT_RADIUS: {
                double radiusKM  = this.getRadiusKilometers();
                if (this.isClientUpload()) {
                    radiusKM += CLIENT_RADIUS_DELTA_METERS / 1000.0;
                }
                if (geozoneMultiPointRadius != null) {
                    return geozoneMultiPointRadius.containsPoint(gp,gzPts,radiusKM);
                } else {
                    return false;
                }
            }
            case BOUNDED_RECT: {
                if (geozoneBoundedRectangle != null) {
                    return geozoneBoundedRectangle.containsPoint(gp,gzPts,0.0);
                } else {
                    return false;
                }
            }
            case POLYGON: {
                if (geozonePolygon != null) {
                    return geozonePolygon.containsPoint(gp,gzPts,0.0);
                } else {
                    return false;
                }
            }
            case SWEPT_POINT_RADIUS:  {
                double radiusKM  = this.getRadiusKilometers();
                if (this.isClientUpload()) {
                    // we make the radius slightly bigger if this was used for a client detected geozone
                    radiusKM += CLIENT_RADIUS_DELTA_METERS / 1000.0;
                }
                if (geozoneSweptPointRadius != null) {
                    return geozoneSweptPointRadius.containsPoint(gp,gzPts,radiusKM);
                } else 
                if (geozoneMultiPointRadius != null) {
                    Print.logWarn("GeoSegment not installed, testing with PointRadius ...");
                    return geozoneMultiPointRadius.containsPoint(gp,gzPts,radiusKM);
                } else {
                    return false;
                }
            }
            default: {
                Print.logError("Unrecognized Geozone type: " + this.getZoneType());
            }
        }

        return false;
    }

    // ------------------------------------------------------------------------

    /* write Geozone to Payload (in DMTP format) */
    public boolean encodeDMTPZone(Payload payload, int ptCnt, boolean hiRes)
    {
        int writeLen = hiRes? 22 : 16; // TODO: ((hiRes?4:2) + 2 + (ptCnt * (hiRes?8:6)))
        if ((payload != null) && payload.isValidWriteLength(writeLen)) {
            long clntID   = (long)this.getClientID();
            int  zoneType = this.getZoneType();
            int  radiusM  = this.getRadius();
            long typeRad  = (long)(((zoneType << 13) & 0xE000) | (radiusM & 0x1FFF));
            Print.logInfo("ClientID:"+clntID + " zoneType:"+zoneType + " radius:"+radiusM + " typeRad:0x"+StringTools.toHexString(typeRad,16));
            //GeoPoint gp[] = this.getGeoPoints();
            if (hiRes) {
                // PropCodes.GEOF_CMD_ADD_HIGH_2, PropCodes.GEOF_CMD_ADD_HIGH_N
                payload.writeULong(clntID , 4);
                payload.writeULong(typeRad, 2);
                payload.writeGPS(this.getLatitude1(), this.getLongitude1(), 8);
                payload.writeGPS(this.getLatitude2(), this.getLongitude2(), 8);
                // TODO: remaining points (when OpenDMTP can handle it)
                // for (int i = 0; i < ptCnt; i++) {
                //    if (i < gp.length) {
                //        payload.writeGPS(gp[i], 8);
                //    } else {
                //        payload.writeGPS(0.0, 0.0, 8);
                //    }
                // }
            } else {
                // PropCodes.GEOF_CMD_ADD_STD_2, PropCodes.GEOF_CMD_ADD_STD_N
                payload.writeULong(clntID , 2);
                payload.writeULong(typeRad, 2);
                payload.writeGPS(this.getLatitude1(), this.getLongitude1(), 6);
                payload.writeGPS(this.getLatitude2(), this.getLongitude2(), 6);
                // TODO: remaining points (when OpenDMTP can handle it)
                // for (int i = 0; i < ptCnt; i++) {
                //    if (i < gp.length) {
                //        payload.writeGPS(gp[i], 6);
                //    } else {
                //        payload.writeGPS(0.0, 0.0, 6);
                //    }
                // }
            }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    private boolean zoneChanged = false;
    
    /**
    *** Sets the zone changed flag 
    **/
    protected void setZoneChanged()
    {
        this.zoneChanged = true;
    }

    /* return true if a bounding box has been defined for this Geozone */
    public boolean hasBoundingBox()
    {
        if ((this.getMinLatitude()  != 0.0) && (this.getMaxLatitude()  != 0.0) &&
            (this.getMinLongitude() != 0.0) && (this.getMaxLongitude() != 0.0)   ) {
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Gets the bounding box for the specified zone
    **/
    public static GeoBounds getBoundingBox(GeozoneType zoneType, GeoPoint gp[], double radiusM)
    {
        GeoBounds bounds = new GeoBounds();
        switch (zoneType) {
            case POINT_RADIUS: {
                // bounded by the radius of all circles
                bounds.extendByCircle(radiusM, gp);
            } break;
            case BOUNDED_RECT: {
                // bounded by all points
                bounds.extendByPoint(gp);
            } break;
            case SWEPT_POINT_RADIUS:  {
                // bounded by the radius of all circles
                bounds.extendByCircle(radiusM, gp);
            } break;
            case POLYGON: {
                // bounded by all points
                bounds.extendByPoint(gp);
            } break;
        }
        return bounds;
    }
    
    /**
    *** Resets the bounding area for this GeoZone
    *** @return True if the bounding area has changed
    **/
    public boolean resetBoundingBox()
    {
        GeoPoint    gp[] = this.getGeoPoints();
        double      radM = this.getRadiusMeters();
        GeozoneType type = Geozone.getGeozoneType(this);
        
        /* bounding box */
        GeoBounds bounds = null;
        if (ListTools.isEmpty(gp)) {
            bounds = new GeoBounds();
            bounds.setMaxLatitude( 0.0);
            bounds.setMinLatitude( 0.0);
            bounds.setMaxLongitude(0.0);
            bounds.setMinLongitude(0.0);
        } else {
            // 'gp' contains at least 1 point
            switch (type) {
                case SWEPT_POINT_RADIUS:
                case POINT_RADIUS      :
                    if (radM <= 0.0) {
                        bounds = new GeoBounds();
                        bounds.setMaxLatitude( gp[0].getLatitude());
                        bounds.setMinLatitude( gp[0].getLatitude());
                        bounds.setMaxLongitude(gp[0].getLongitude());
                        bounds.setMinLongitude(gp[0].getLongitude());
                    }
                    break;
                case BOUNDED_RECT      :
                    if (gp.length < 2) {
                        bounds = new GeoBounds();
                        bounds.setMaxLatitude( gp[0].getLatitude());
                        bounds.setMinLatitude( gp[0].getLatitude());
                        bounds.setMaxLongitude(gp[0].getLongitude());
                        bounds.setMinLongitude(gp[0].getLongitude());
                    }
                    break;
                case POLYGON           :
                    if (gp.length < 3) {
                        bounds = new GeoBounds();
                        bounds.setMaxLatitude( gp[0].getLatitude());
                        bounds.setMinLatitude( gp[0].getLatitude());
                        bounds.setMaxLongitude(gp[0].getLongitude());
                        bounds.setMinLongitude(gp[0].getLongitude());
                    }
                    break;
            }
        }
        if (bounds == null) {
            bounds = Geozone.getBoundingBox(type,gp,radM);
        }

        /* only set those that changed */
        boolean changed = false;
        if (Math.abs(this.getMinLatitude() - bounds.getMinLatitude()) > GeoPoint.EPSILON) {
            //Print.logInfo("Bound MinLatitude changed: %f != %f [%f]", this.getMinLatitude(), bounds.getMinLatitude());
            this.setMinLatitude( bounds.getMinLatitude());
            changed = true;
        }
        if (Math.abs(this.getMaxLatitude() - bounds.getMaxLatitude()) > GeoPoint.EPSILON) {
            //Print.logInfo("Bound MaxLatitude changed: %f != %f [%f]", this.getMaxLatitude(), bounds.getMaxLatitude());
            this.setMaxLatitude( bounds.getMaxLatitude());
            changed = true;
        }
        if (Math.abs(this.getMinLongitude() - bounds.getMinLongitude()) > GeoPoint.EPSILON) {
            //Print.logInfo("Bound MinLongitude changed: %f != %f [%f]", this.getMinLongitude(), bounds.getMinLongitude());
            this.setMinLongitude(bounds.getMinLongitude());
            changed = true;
        }
        if (Math.abs(this.getMaxLongitude() - bounds.getMaxLongitude()) > GeoPoint.EPSILON) {
            //Print.logInfo("Bound MaxLongitude changed: %f != %f [%f]", this.getMaxLongitude(), bounds.getMaxLongitude());
            this.setMaxLongitude(bounds.getMaxLongitude());
            changed = true;
        }

        /* return */
        return changed;

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets Geozone bounding-box and inserts the GeoZone into the table
    **/
    public void insert()
        throws DBException
    {
        this.resetBoundingBox();
        super.insert();
        this.zoneChanged = false;
    }
    
    /**
    *** Sets Geozone bounding-box and inserts the GeoZone into the table
    **/
    public void update(String... updFldArray)
        throws DBException
    {
        if (ALWAYS_UPDATE_BOUNDS || this.zoneChanged) {
            this.resetBoundingBox();
            if (updFldArray != null) {
                Set<String> fldSet = ListTools.toSet(updFldArray, new HashSet<String>());
                fldSet.add(FLD_minLatitude);
                fldSet.add(FLD_maxLatitude);
                fldSet.add(FLD_minLongitude);
                fldSet.add(FLD_maxLongitude);
                super.update(fldSet);
            } else {
                super.update((String[])null);
            }
        } else {
            super.update(updFldArray);
        }
        this.zoneChanged = false;
    }
    
    /**
    *** Sets Geozone bounding-box and inserts the GeoZone into the table
    **/
    public void update(Set<String> updFldSet)
        throws DBException
    {
        if (ALWAYS_UPDATE_BOUNDS || this.zoneChanged) {
            this.resetBoundingBox();
            if (updFldSet != null) {
                Set<String> fldSet = new HashSet<String>(updFldSet);
                fldSet.add(FLD_minLatitude);
                fldSet.add(FLD_maxLatitude);
                fldSet.add(FLD_minLongitude);
                fldSet.add(FLD_maxLongitude);
                super.update(fldSet);
            } else {
                super.update((Set<String>)null);
            }
        } else {
            super.update(updFldSet);
        }
        this.zoneChanged = false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // [DB]WHERE ( accountID='account' AND geozoneID='id' AND sortID='id' AND clientUpload!=0 )
    public static String getWhereClause(
        String acctId,
        String zoneId, int sortId,
        GeoPoint centerGP,
        boolean clientUploadOnly,
        boolean reverseGeocodeOnly)
    {
        DBWhere dwh = new DBWhere(Geozone.getFactory());

        /* Account */
        dwh.append(dwh.EQ(FLD_accountID,acctId));

        /* specific zoneID */
        if (!StringTools.isBlank(zoneId)) {
            dwh.append(dwh.AND_(dwh.EQ(FLD_geozoneID,zoneId)));
            // specific sortID
            if (sortId >= 0) {
                dwh.append(dwh.AND_(dwh.EQ(FLD_sortID,sortId)));
            }
        }
        
        /* client upload geozones only? */
        if (clientUploadOnly) {
            dwh.append(dwh.AND_(dwh.NE(FLD_clientUpload,0)));
            dwh.append(dwh.AND_(dwh.GT(FLD_clientID,0)));
        }
        
        /* reverseGeocodeOnly geozones only? */
        if (reverseGeocodeOnly) {
            dwh.append(dwh.AND_(dwh.NE(FLD_reverseGeocode,0)));
        }

        /* constrain to points nearby a lan/lon? */
        if (centerGP != null) {
            double lat = centerGP.getLatitude();
            double lon = centerGP.getLongitude();
            if (USE_BOUNDING_BOX) {
                dwh.append(dwh.AND_(
                    dwh.AND(
                        dwh.LE(FLD_minLatitude , lat),
                        dwh.GE(FLD_maxLatitude , lat),
                        dwh.LE(FLD_minLongitude, lon),
                        dwh.GE(FLD_maxLongitude, lon)
                    )
                ));
            } else {
                GeoOffset ofsGP = centerGP.getRadiusDeltaPoint(MAX_RADIUS_METERS);
                dwh.append(dwh.AND_(
                    dwh.AND(
                        dwh.GE(FLD_latitude1 , lat - ofsGP.getOffsetLatitude() ),
                        dwh.LE(FLD_latitude1 , lat + ofsGP.getOffsetLatitude() ),
                        dwh.GE(FLD_longitude1, lon - ofsGP.getOffsetLongitude()),
                        dwh.LE(FLD_longitude1, lon + ofsGP.getOffsetLongitude())
                    )
                ));
            }
        }

        /* end of where */
        String wh = dwh.WHERE(dwh.toString());
        //Print.logInfo("Where: " + wh);
        return wh;
        
    }

    // [DB]WHERE ( accountID='account' AND clientID=id AND clientUpload!=0 )
    public static String getWhereClause(
        String acctId,
        long clientId)
    {
        DBWhere dwh = new DBWhere(Geozone.getFactory());

        /* Account */
        dwh.append(dwh.EQ(FLD_accountID,acctId));

        /* client upload geozones only? */
        dwh.append(dwh.AND_(dwh.EQ(FLD_clientID,clientId)));
        dwh.append(dwh.AND_(dwh.NE(FLD_clientUpload,0)));

        /* end of where */
        String wh = dwh.WHERE(dwh.toString());
        //Print.logInfo("Where: " + wh);
        return wh;

    }

    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String zoneID, int sortID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (zoneID != null)) {
            if (sortID >= 0) {
                // test for specific zone
                Geozone.Key zoneKey = new Geozone.Key(acctID,zoneID,sortID);
                return zoneKey.exists();
            } else {
                // test for any zone with specified zoneID
                Geozone.Key zoneKey = new Geozone.Key(acctID,zoneID,-1);
                return zoneKey.exists(false);
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    public static String getDescription(Account account, GeoPoint gp)
    {
        if (account != null) {
            return Geozone.getDescription(account.toString(), gp);
        } else {
            return null;
        }
    }

    public static String getDescription(String acctId, GeoPoint gp)
    {
        Geozone gz = Geozone.getGeozone(acctId, null, gp, true);
        return (gz != null)? gz.getDescription() : null;
    }

    // ------------------------------------------------------------------------
    // return the Geozone in which the specified point resides

    public static Geozone getGeozone(Account account, String zoneID, GeoPoint gp, boolean reverseGeocodeOnly)
    {
        if (account != null) {
            return Geozone.getGeozone(account.getAccountID(), zoneID, gp, reverseGeocodeOnly);
        } else {
            return null;
        }
    }
    
    public static Geozone getGeozone(String acctId, String zoneID, GeoPoint gp, boolean reverseGeocodeOnly)
    {

        /* invalid account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* invalid GeoPoint */
        if ((gp == null) || !gp.isValid()) {
            return null;
        }

        /* where clause */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY geozoneID,sortID
        DBSelect<Geozone> dsel = new DBSelect<Geozone>(Geozone.getFactory());
        dsel.setWhere(Geozone.getWhereClause(
            acctId,                     // accountID
            zoneID,                     // geozoneID
            -1,                         // sortID
            gp,                         // GeoPoint
            false,                      // clientUpload
            reverseGeocodeOnly          // reverseGeocode
            ));
        if (Geozone.supportsPriority()) {
            dsel.setOrderByFields(FLD_priority, FLD_sortID);
        } else {
            dsel.setOrderByFields(FLD_sortID);
        }

        /* get Geozones */
        Geozone gz[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //gz = (Geozone[])DBRecord.select(Geozone.getFactory(), dsel.toString(false));
            gz = DBRecord.select(dsel); // select:DBSelect
        } catch (DBException dbe) {
            Print.logError("Geozone error: " + dbe);
            return null;
        } finally {
            try {
                DBProvider.unlockTables();
            } catch (DBException dbe) {
                // ignore
            }
        }

        /* no records? */
        if (ListTools.isEmpty(gz)) {
            return null;
        }

        /* find closest Geozone to specified point */
        //Print.logDebug("Found Geozone count: %d", gz.length);
        String lastDesc = "";
        String lastZoneId = null;
        for (int g = 0; g < gz.length; g++) {
            //Print.logDebug("Testing Geozone: %s", gz[g].getDescription());

            /* reset the cached last description if we've changed zones */
            String zoneId = gz[g].getGeozoneID();
            if ((lastZoneId == null) || !lastZoneId.equals(zoneId)) {
                lastZoneId = zoneId;
                lastDesc = "";
            }
            String thisDesc = gz[g].getDescription();

            /* return found Geozone */
            if (gz[g].containsPoint(gp)) {
                if (thisDesc.equals("") && !lastDesc.equals("")) {
                    // make sure the returned description is valid (if possible)
                    gz[g].setDescription(lastDesc);
                }
                return gz[g];
            }

            /* save last description */
            if (!thisDesc.equals("")) {
                lastDesc = thisDesc;
            }

        }
        return null;

    }

    // ------------------------------------------------------------------------

    /* Get all Geozones in which the GeoPoint resides (sorted by priority) */
    public static Geozone[] getGeozones(String acctID, GeoPoint gp)
        throws DBException
    {

        /* invalid account */
        if (StringTools.isBlank(acctID)) {
            return null;
        }

        /* invalid GeoPoint */
        if ((gp == null) || !gp.isValid()) {
            return null;
        }

        /* where clause */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY geozoneID,sortID
        DBSelect<Geozone> dsel = new DBSelect<Geozone>(Geozone.getFactory());
        dsel.setWhere(Geozone.getWhereClause(
            acctID,                     // accountID
            null,                       // geozoneID
            -1,                         // sortID
            gp,                         // GeoPoint
            false,                      // clientUpload
            false                       // reverseGeocode
            ));
        if (Geozone.supportsPriority()) {
            dsel.setOrderByFields(FLD_priority, FLD_sortID);
        } else {
            dsel.setOrderByFields(FLD_sortID);
        }

        /* get Geozones */
        Geozone gz[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            gz = DBRecord.select(dsel); // select:DBSelect
        } catch (DBException dbe) {
            Print.logError("Geozone error: " + dbe);
            return null;
        } finally {
            try {
                DBProvider.unlockTables();
            } catch (DBException dbe) {
                // ignore
            }
        }

        /* no records? */
        if (ListTools.isEmpty(gz)) {
            return null;
        }

        /* qualify that geozones contain point */
        int x = 0;
        for (int g = 0; g < gz.length; g++) {
            if (gz[g].containsPoint(gp)) {
                if (x != g) { gz[x] = gz[g]; }
                x++;
            } else {
                gz[g] = null;
            }
        }

        /* return geozones */
        if (x == 0) {
            return null;
        } else
        if (x == gz.length) {
            return gz;
        } else {
            Geozone nz[] = new Geozone[x];
            System.arraycopy(gz,0, nz,0, x);
            return nz;
        }

    }

    // ------------------------------------------------------------------------

    /* Get/Create specific Geozone */
    public static Geozone[] getGeozone(Account account, String geozoneID)
        throws DBException
    {
        // TODO: modify to return all 'sortID's
        Geozone zone = Geozone.getGeozone(account, geozoneID, 0, false);
        if (zone == null) {
            return null;
        } else {
            return new Geozone[] { zone };
        }
    }

    /* Get/Create specific Geozone */
    public static Geozone getGeozone(Account account, String geozoneID, int sortID, boolean create)
        throws DBException
    {
        
        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctId = account.getAccountID();

        /* geozone-id specified? */
        if (StringTools.isBlank(geozoneID)) {
            throw new DBNotFoundException("Geozone-ID not specified.");
        }

        /* get/create geozone */
        Geozone geozone = null;
        Geozone.Key zoneKey = new Geozone.Key(acctId, geozoneID, sortID);
        if (!zoneKey.exists()) { // may throw DBException
            if (create) {
                geozone = zoneKey.getDBRecord();
                geozone.setAccount(account);
                geozone.setCreationDefaultValues();
                return geozone; // not yet saved!
            } else {
                throw new DBNotFoundException("Geozone-ID does not exists '" + zoneKey + "'");
            }
        } else
        if (create) {
            // we've been asked to create the geozone, and it already exists
            throw new DBAlreadyExistsException("Geozone-ID already exists '" + zoneKey + "'");
        } else {
            geozone = zoneKey.getDBRecord(true); // may throw DBException
            if (geozone == null) {
                throw new DBException("Unable to read existing Geozone-ID '" + zoneKey + "'");
            }
            return geozone;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean containsPoint(Account account, String zoneId, GeoPoint gp)
        throws DBNotFoundException
    {
        if (account != null) {
            return Geozone.containsPoint(account.getAccountID(), zoneId, gp);
        } else {
            return false;
        }
    }

    public static boolean containsPoint(String acctId, String zoneId, GeoPoint gp)
        throws DBNotFoundException
    {

        /* invalid account */
        if (StringTools.isBlank(acctId)) {
            throw new DBNotFoundException("Account ID is blank/null");
        }

        /* invalid geozone */
        //if (StringTools.isBlank(zoneId)) {
        //    throw new DBNotFoundException("Zone ID is blank/null");
        //}

        /* invalid point? */
        if ((gp == null) || !gp.isValid()) {
            return false;
        }

        /* selection point */
        GeoPoint selGP = gp; // non-null

        /* where clause */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY geozoneID,sortID
        DBSelect<Geozone> dsel = new DBSelect<Geozone>(Geozone.getFactory());
        dsel.setWhere(Geozone.getWhereClause(
            acctId,                     // accountID
            zoneId,                     // geozoneID    - may be null
            -1,                         // sortID
            selGP,                      // GeoPoint
            false,                      // clientUpload
            false                       // reverseGeocode
            ));
        //dsel.setOrderByFields(FLD_sortID);  <-- ordering not necessary

        /* get Geozones */
        Geozone gz[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //gz = (Geozone[])DBRecord.select(Geozone.getFactory(), dsel.toString(false));
            gz = DBRecord.select(dsel); // select:DBSelect
        } catch (DBException dbe) {
            throw new DBNotFoundException("Geozone error: " + dbe);
        } finally {
            try {
                DBProvider.unlockTables();
            } catch (DBException dbe) {
                // ignore
            }
        }

        /* no records? */
        if (ListTools.isEmpty(gz)) {
            if (selGP == null) {
                //Print.logStackTrace("Invalid Geozone? "  + acctId + "/" + zoneId);
                throw new DBNotFoundException("Geozone not found: " + acctId + "/" + zoneId);
            } else {
                return false;
            }
        }

        /* see if the specified point is inside this geozoneID */
        for (int g = 0; g < gz.length; g++) {
            if (gz[g].containsPoint(gp)) {
                return true;
            }
        }
        return false;

    }

    // ------------------------------------------------------------------------

    /* return all Geozones matching specified clientID (should be at most one match, since this came from the client) */
    public static Geozone[] getClientIDZones(String acctId, long clientId)
    {

        /* invalid account? */
        if ((acctId == null) || acctId.equals("")) {
            Print.logError("AccountID not specified");
            return null;
        }

        /* invalid clientID? */
        if (clientId <= 0L) {
            Print.logError("ClientID not specified");
            return null;
        }

        /* where clause */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY sortID
        DBSelect<Geozone> dsel = new DBSelect<Geozone>(Geozone.getFactory());
        dsel.setWhere(Geozone.getWhereClause(
            acctId,             // accountID
            clientId            // clientID
            ));
        dsel.setOrderByFields(FLD_sortID);

        /* get clientID Geozones */
        Geozone gz[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //gz = (Geozone[])DBRecord.select(Geozone.getFactory(), dsel.toString(false));
            gz = DBRecord.select(dsel); // select:DBSelect
        } catch (DBException dbe) {
            Print.logError("Geozone error: " + dbe);
            return null;
        } finally {
            try {
                DBProvider.unlockTables();
            } catch (DBException dbe) {
                // ignore
            }
        }

        /* return Geozones */
        if ((gz == null) || (gz.length == 0)) {
            return null;
        } else {
            return gz;
        }

    }

    public static Geozone[] getClientUploadZones(String acctId)
    {

        /* invalid account? */
        if ((acctId == null) || acctId.equals("")) {
            Print.logError("AccountID not specified");
            return null;
        }
        
        /* where clause */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY clientID,sortID
        DBSelect<Geozone> dsel = new DBSelect<Geozone>(Geozone.getFactory());
        dsel.setWhere(Geozone.getWhereClause(
            acctId,             // accountID
            null,               // geozoneID (all ids)
            -1,                 // sortID (all ids)
            null,               // GeoPoint (all GeoPoints)
            true,               // clientUpload (only)
            false               // reverseGeocode (only)
            ));
        dsel.setOrderByFields(FLD_clientID, FLD_sortID);
        
        /* get Geozones */
        Geozone gz[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //gz = (Geozone[])DBRecord.select(Geozone.getFactory(), dsel.toString(false));
            gz = DBRecord.select(dsel); // select:DBSelect
        } catch (DBException dbe) {
            Print.logError("Geozone error: " + dbe);
            return null;
        } finally {
            try {
                DBProvider.unlockTables();
            } catch (DBException dbe) {
                // ignore
            }
        }
        
        /* return Geozones */
        if ((gz == null) || (gz.length == 0)) {
            return null;
        } else {
            return gz;
        }

    }

    // ------------------------------------------------------------------------

    /* return list of all Geozone IDs owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getGeozoneIDsForAccount(String acctId)
        throws DBException
    {

        /* invalid account */
        if (StringTools.isBlank(acctId)) {
            return new String[0];
        }

        /* select */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY clientID,sortID
        DBSelect<Geozone> dsel = new DBSelect<Geozone>(Geozone.getFactory());
        dsel.setWhere(Geozone.getWhereClause(
            acctId,             // accountID
            null,               // geozoneID (all ids)
            -1,                 // sortID (all ids) [we only need the first)
            null,               // GeoPoint (all GeoPoints)
            false,              // clientUpload (only)
            false               // reverseGeocode (only)
            ));
        dsel.setOrderByFields(FLD_geozoneID, FLD_sortID);

        /* return list */
        return Geozone.getGeozoneIDs(dsel);

    }

    /* return list of all Geozones owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getGeozoneIDs(DBSelect<Geozone> dsel)
        throws DBException
    {

        /* invalid DBSelect */
        if (dsel == null) {
            return new String[0];
        }

        /* get record ids */
        OrderedSet<String> zoneList = new OrderedSet<String>(true);
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get record ids */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String zoneId = rs.getString(Geozone.FLD_geozoneID);
                zoneList.add(zoneId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Geozone ID List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return zoneList.toArray(new String[zoneList.size()]);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* re-format CSV */
    // This creates a CSV file that can be loaded onto the client device
    private static void printClientCSV(File csvFile, String accountID)
    {
        FileOutputStream fos = null;
        try {

            /* open file */
            fos = (csvFile != null)? new FileOutputStream(csvFile) : null;

            /* header */
            String header = "#zoneID,type,rad,lat0,lon0,lat1,lon1\n";
            if (fos != null) {
                FileTools.writeStream(fos, header);
            } else {
                Print.sysPrint(header);
            }

            /* print zones */
            Geozone gzList[] = Geozone.getClientUploadZones(accountID);
            if ((gzList != null) && (gzList.length > 0)) {
                for (int i = 0; i < gzList.length; i++) {
                    Geozone gz = gzList[i];
                    int zoneType = gz.getZoneType();
                    StringBuffer sb = new StringBuffer();
                    sb.append(gz.getClientID()).append(",");
                    sb.append(zoneType).append(",");
                    if (zoneType == GeozoneType.BOUNDED_RECT.getIntValue()) {
                        sb.append(gz.getRadius()).append(",");
                        sb.append(gz.getNorthLatitude()).append(",");
                        sb.append(gz.getWestLongitude()).append(",");
                        sb.append(gz.getSouthLatitude()).append(",");
                        sb.append(gz.getEastLongitude());
                    } else {
                        sb.append(gz.getRadius()).append(",");
                        sb.append(gz.getLatitude1()).append(",");
                        sb.append(gz.getLongitude1()).append(",");
                        sb.append(gz.getLatitude2()).append(",");
                        sb.append(gz.getLongitude2());
                    }
                    sb.append("\n");
                    if (fos != null) {
                        FileTools.writeStream(fos, sb.toString());
                    } else {
                        Print.sysPrint(sb.toString());
                    }
                }
            }

        } catch (IOException ioe) {
            Print.logException("Unable to create CSV file", ioe);
        } finally {
            if (fos != null) { FileTools.closeStream(fos); }
        }
    }

    // ------------------------------------------------------------------------

    public static void listZones(String acctId, boolean update)
    {

        /* invalid account */
        if (StringTools.isBlank(acctId)) {
            Print.logError("AccountID not specified");
            return;
        }

        /* where clause */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY clientID,sortID
        DBSelect<Geozone> dsel = new DBSelect<Geozone>(Geozone.getFactory());
        dsel.setWhere(Geozone.getWhereClause(
            acctId,             // accountID
            null,               // geozoneID (all ids)
            -1,                 // sortID (all ids)
            null,               // GeoPoint (all GeoPoints)
            false,              // clientUpload (only)
            false               // reverseGeocode (only)
            ));
        dsel.setOrderByFields(FLD_geozoneID, FLD_sortID);

        /* get Geozones */
        Geozone gz[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //gz = (Geozone[])DBRecord.select(Geozone.getFactory(), dsel.toString(false));
            gz = DBRecord.select(dsel); // select:DBSelect
        } catch (DBException dbe) {
            Print.logError("Geozone error: " + dbe);
            return;
        } finally {
            try {
                DBProvider.unlockTables();
            } catch (DBException dbe) {
                // ignore
            }
        }

        /* list Geozones */
        int invalidBoundingBoxes = 0;
        if ((gz != null) && (gz.length > 0)) {
            Print.sysPrintln("");
            Print.sysPrintln("Account: " + acctId);
            for (int i = 0; i < gz.length; i++) {
                int ptCnt = Geozone.GetGeoPointCount();
                StringBuffer sb = new StringBuffer();
                sb.append(" ---------------------------------------------------------\n");
                sb.append("  Geozone  : " + gz[i].getGeozoneID() + ":" + gz[i].getSortID() + " - " + gz[i].getDescription()+ "\n");
                String city   = gz[i].getCity();
                String postal = gz[i].getPostalCode();
                String subDiv = gz[i].getSubdivision();
                if (StringTools.isBlank(city) && StringTools.isBlank(postal) && StringTools.isBlank(subDiv)) {
                    sb.append("    Address: \n");
                } else {
                    sb.append("    Address: " + city + ", " + postal + ", " + subDiv + "\n");
                }
                sb.append("    Type   : " + gz[i].getZoneType() + " - " + gz[i].getZoneTypeDescription(null)+ "\n");
                int radiusM = gz[i].getRadius();
                if (gz[i].hasRadius()) {
                    sb.append("    Radius : " + gz[i].getRadius() + " meters\n");
                } else
                if (radiusM != 0) {
                    sb.append("    Radius : [" + gz[i].getRadius() + " meters] not used\n");
                }
                for (int p = 0; p < ptCnt; p++) {
                    // Points  : 1(39.12345/-142.12345) 2(39.12345/-142.12345) 3(39.12345/-142.12345)
                    if ((p % 3) == 0) {
                        if (p > 0) { sb.append("\n"); }
                        sb.append("    Points : ");
                    } else {
                        sb.append(" "); 
                    }
                    GeoPoint gp = gz[i].getGeoPoint(p);
                    if (gp == null) { gp = GeoPoint.INVALID_GEOPOINT; }
                    sb.append(p+1).append("(" + gp + ")");
                }
                sb.append("\n");
                boolean hasBounds = gz[i].hasBoundingBox();
                boolean boundsChanged = gz[i].resetBoundingBox();
                if (boundsChanged) { invalidBoundingBoxes++; }
                sb.append("    Bounds : " + hasBounds + " [" + (boundsChanged?"INVALID":"valid") + "]\n");
                sb.append("    Upload : " + gz[i].getClientUpload() + " [id=" + gz[i].getClientID()+ "]\n");
                sb.append("    RevGeo : " + gz[i].getReverseGeocode()+ "\n");
                if (update) {
                    try {
                        gz[i].save();
                        sb.append("    Updated: true\n");
                    } catch (DBException dbe) {
                        sb.append("    Updated: false [ERROR: " + dbe.getMessage() + "]\n");
                    }
                }
                Print.sysPrintln(sb.toString());
            }
            if (invalidBoundingBoxes > 0) {
                if (update) {
                    Print.sysPrintln("Note:");
                    Print.sysPrintln("Invalid Bounding-Box field values have been updated.");
                    Print.sysPrintln("(Run command again without '-update' to verify)");
                } else {
                    Print.sysPrintln("WARNING:");
                    Print.sysPrintln("Geozones contained invalid Bounding-Box field values.");
                    Print.sysPrintln("Run command again with '-update' option to update Bounding-Box fields.");
                }
                Print.sysPrintln("\n");
            }
        }

    }

    // ------------------------------------------------------------------------
    
    private static class ZoneLoadValidator
        implements DBFactory.InsertionValidator
    {
        private String  accountID = null;
        private String  fields[]  = null;
        private int     acctNdx   = -1;
        private int     zoneNdx   = -1;
        private int     zoneType  = -1;
        private int     count     = 0;
        private boolean forceInvalid = false;
        public ZoneLoadValidator(String acctID, boolean forceInvalid) {
            this.accountID    = (acctID != null)? acctID : "";
            this.forceInvalid = forceInvalid;
        }
        public boolean setFields(String f[]) throws DBException { 
            this.count = 0;
            this.fields = f;
            if (this.fields == null) {
                throw new DBException("No fields specified");
            }
            for (int i = 0; i < this.fields.length; i++) {
                if ((this.acctNdx < 0) && this.fields[i].equals(FLD_accountID)) {
                    this.acctNdx = i;
                }
                if ((this.zoneNdx < 0) && this.fields[i].equals(FLD_geozoneID)) {
                    this.zoneNdx = i;
                }
                if ((this.zoneType < 0) && this.fields[i].equals(FLD_zoneType)) {
                    this.zoneType = i;
                }
            }
            if (this.acctNdx < 0) {
                throw new DBException("Load file is missing '" + FLD_accountID + "'");
            } else
            if (this.zoneNdx < 0) {
                throw new DBException("Load file is missing '" + FLD_geozoneID + "'");
            }
            return true;
        }
        public boolean validate(String v[]) throws DBException {
            this.count++;
            // proper field length
            if (this.fields == null) {
                throw new DBException("No fields specified");
            } else
            if (v == null) {
                throw new DBException("No field values specified");
            } else
            if (v.length != this.fields.length) {
                int vlen = v.length;
                int flen = this.fields.length;
                throw new DBException("Invalid # of fields (found=" + vlen + ", expected=" + flen + ") [" + this.count + "]");
            }
            // valid accountID
            String a = (this.acctNdx >= 0)? v[this.acctNdx] : null;
            if ((a == null) || a.equals("") || !a.equals(this.accountID)) {
                Print.logError("Invalid Account ID found: " + a + " [cnt=" + this.count + "]");
                return false;
            }
            // valid geozoneID
            String z = (this.zoneNdx >= 0)? v[this.zoneNdx] : null;
            if ((z == null) || z.equals("")) {
                Print.logError("Invalid Geozone ID found: " + z + " [cnt=" + this.count + "]");
                return false;
            }
            // force Invalid
            if (this.forceInvalid) {
                return false;
            }
            // ok
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String CSV_EXT        = ".csv";
    
    private static final String ARG_ACCOUNT[]  = new String[] { "account" , "acct" }; // -acct=<id>
    private static final String ARG_DEVICE[]   = new String[] { "device"  , "dev"  }; // -dev=<id>
    private static final String ARG_GEOZONE[]  = new String[] { "geozone" , "zone" }; // -zone=<id> (used by create/edit)
    private static final String ARG_GEOPOINT[] = new String[] { "geopoint", "gp"   }; // -gp=<lat/lon>
    private static final String ARG_SORT[]     = new String[] { "sort"             }; // -sort=<id> (used by create/edit)
    private static final String ARG_CREATE[]   = new String[] { "create"           }; // -create
    private static final String ARG_EDIT[]     = new String[] { "edit"    , "ed"   }; // -edit
    private static final String ARG_EDITALL[]  = new String[] { "editall" , "eda"  };
    private static final String ARG_LIST[]     = new String[] { "list"             }; // -list
    private static final String ARG_DMTPCSV[]  = new String[] { "dmtpcsv"          }; // -dmtpcsv=<file>
    private static final String ARG_EXPORT[]   = new String[] { "export"           }; // -export=<file>
    private static final String ARG_DUMP[]     = new String[] { "dump"             }; // -dump=<file>
    private static final String ARG_LOAD[]     = new String[] { "load"             }; // -load=<file>
    private static final String ARG_VALIDATE[] = new String[] { "validate"         }; // -validate=<file>
    private static final String ARG_TEST[]     = new String[] { "test"             }; // -test=<lat>/<lon>
    private static final String ARG_EVTEST[]   = new String[] { "evtest"           }; // -evtest
    private static final String ARG_UPDATE[]   = new String[] { "update"           }; // -update

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Geozone.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>     Acount ID which owns the specified Geozone");
        Print.logInfo("  -zone=<id>        Geozone ID");
        Print.logInfo("  -sort=<index>     Unique sort index for Geozone");
        Print.logInfo("  -create           Create Geozone (requires '-zone=<zoneId> -sort=<sortId>')");
        Print.logInfo("  -edit             Edit Geozone (requires '-zone=<zoneId> -sort=<sortId>')");
        Print.logInfo("  -list             List Account Geozones");
        Print.logInfo("  -gp=<lat/lon>     Display all Geozones containing point");
        Print.logInfo("  -dmtpcsv=<file>   Create CSV file for client devices");
        Print.logInfo("  -export=<csvFile> Export Account Geozones to CSV file (selected columns)");
        Print.logInfo("  -dump=<csvFile>   Dump Account Geozones to CSV file (all columns)");
        Print.logInfo("  -load=<csvFile>   Load Account Geozones from CSV file");
        Print.logInfo("  -test=<lat>/<lon> Test Geozone: find specified point");
        System.exit(1);
    }

    /* utility main entry point */
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        String acctID = RTConfig.getString(ARG_ACCOUNT, "");
        String devID  = RTConfig.getString(ARG_DEVICE , "");
        String zoneID = RTConfig.getString(ARG_GEOZONE, "");
        int    sortID = RTConfig.getInt(ARG_SORT,-1);

        /* account-id specified? */
        if ((acctID == null) || acctID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(acctID); // may return DBException
            if (account == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            System.exit(99);
        }

        /* geozoneId/sortId specified? */
        boolean zoneSpecified = ((zoneID != null) && !zoneID.equals(""));
        boolean sortSpecified = (sortID >= 0);

        /* 'sortID' special case for 'edit'/'editall' */
        boolean isArgEdit = (RTConfig.hasProperty(ARG_EDIT) || RTConfig.hasProperty(ARG_EDITALL));
        if (isArgEdit && !sortSpecified) {
            sortID = 0;
            sortSpecified = true;
            Print.logInfo("Defaulting to '-" + ARG_SORT[0] + "=0'");
        }

        /* geozone exists? */
        boolean zoneExists = false;
        try {
            zoneExists = Geozone.exists(acctID, zoneID, sortID);
        } catch (DBException dbe) {
            Print.logError("Error determining if Device exists: '" + acctID + "," + zoneID+ "," + sortID + "'");
            System.exit(99);
        }

        /* option count */
        int opts = 0;

        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            opts++;
            boolean update = RTConfig.getBoolean(ARG_UPDATE, false);
            Geozone.listZones(acctID, update);
            System.exit(0);
        }

        /* GeoPoint */
        if (RTConfig.hasProperty(ARG_GEOPOINT)) {
            String gpStr = RTConfig.getString(ARG_GEOPOINT,null);
            GeoPoint gp = new GeoPoint(gpStr);
            if (!gp.isValid()) {
                Print.sysPrintln("ERROR: Invalid GeoPoint - " + gpStr);
                System.exit(99);
            }
            try {
                Geozone gz[] = Geozone.getGeozones(acctID, gp);
                if (ListTools.size(gz) > 0) {
                    Print.sysPrintln("Found Zones:");
                    for (Geozone z : gz) {
                        Print.sysPrintln("  " + z.getGeozoneID());
                    }
                } else {
                    Print.sysPrintln("No Geozones found.");
                }
            } catch (DBException dbe) {
                Print.logException("Unable to read Geozones", dbe);
                System.exit(99);
            }
            System.exit(0);
        }

        /* write DMTP client CSV */
        if (RTConfig.hasProperty(ARG_DMTPCSV)) {
            opts++;
            String csvFileName = RTConfig.getString(ARG_DMTPCSV,"");
            File csvFile = !csvFileName.equals("")? new File(csvFileName) : null;
            Geozone.printClientCSV(csvFile, acctID);
            System.exit(0);
        }
        
        /* create record */
        if (RTConfig.hasProperty(ARG_CREATE)) {
            opts++;
            if (!zoneSpecified) {
                Print.logError("Invalid or missing geozoneId");
                usage();
            } else
            if (!sortSpecified) {
                Print.logError("Invalid or missing sortId");
                usage();
            } else
            if (zoneExists) {
                Print.logError("Geozone already exists: '" + acctID + "," + zoneID+ "," + sortID + "'");
                usage();
            } else {
                try {
                    Geozone.Key key = new Geozone.Key(acctID,zoneID,sortID);
                    Geozone gz = key.getDBRecord(false);
                    gz.setCreationDefaultValues();
                    gz.save();
                } catch (DBException dbe) {
                    Print.logException("Creating Geozone", dbe);
                    System.exit(99);
                }
            }
        }
        
        /* edit record */
        if (isArgEdit) {
            opts++;
            if (!zoneSpecified) {
                Print.logError("Invalid or missing geozoneId");
                usage();
            } else
            if (!sortSpecified) {
                Print.logError("Invalid or missing sortId");
                usage();
            } else
            if (!zoneExists) {
                Print.logError("Geozone does not exist exist: '" + acctID + "," + zoneID+ "," + sortID + "'");
                usage();
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    Geozone.Key key = new Geozone.Key(acctID,zoneID,sortID);
                    Geozone gz = key.getDBRecord(true);
                    DBEdit editor = new DBEdit(gz);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                //} catch (DBException dbe) {
                //    Print.logError("Error editing Geozone '" + acctID + "," + zoneID+ "," + sortID + "'");
                //    dbe.printException();
                }
                System.exit(0);
            }
        }

        /* dump */
        if (RTConfig.hasProperty(ARG_DUMP)) {
            opts++;
            String dumpFileName = RTConfig.getString(ARG_DUMP,"");
            File dumpFile = !StringTools.isBlank(dumpFileName)? new File(dumpFileName) : null;
            if ((dumpFile == null) || !dumpFile.getName().toLowerCase().endsWith(CSV_EXT)) {
                Print.logError("Invalid file specified: " + dumpFile);
                usage();
            }
            DBFactory<Geozone> fact = Geozone.getFactory();
            try {
                // [DBSelect] WHERE (accountID='account') ORDER BY geozoneID,sortID
                DBSelect<Geozone> dsel = new DBSelect<Geozone>(fact);
                DBWhere dwh = dsel.createDBWhere();
                dsel.setWhere(dwh.WHERE_(dwh.EQ(Geozone.FLD_accountID,acctID)));
                dsel.setOrderByFields(FLD_geozoneID, FLD_sortID);
                fact.dumpTable(dumpFile, dsel);
            } catch (DBException dbe) {
                Print.logException("Error dumping table: " + TABLE_NAME(), dbe);
                System.exit(99);
            }
            System.exit(0);
        }

        /* export (similar to 'dump', but only selected columns) */
        if (RTConfig.hasProperty(ARG_EXPORT)) {
            opts++;
            String dumpFileName = RTConfig.getString(ARG_EXPORT,"");
            File dumpFile = !StringTools.isBlank(dumpFileName)? new File(dumpFileName) : new File("stdout"+CSV_EXT);
            if (!dumpFile.getName().toLowerCase().endsWith(CSV_EXT)) {
                Print.logError("Invalid file specified: " + dumpFile);
                usage();
            }
            DBFactory<Geozone> fact = Geozone.getFactory();
            try {
                // [DBSelect] WHERE (accountID='account') ORDER BY geozoneID,sortID
                DBSelect<Geozone> dsel = new DBSelect<Geozone>(fact);
                DBWhere dwh = dsel.createDBWhere();
                dsel.setWhere(dwh.WHERE_(dwh.EQ(Geozone.FLD_accountID,acctID)));
                dsel.setOrderByFields(FLD_geozoneID, FLD_sortID);
                String exportFlds[] = DBFactory.getFieldNames(fact.getFieldsWithBoolean("export",true));
                fact.dumpTable(dumpFile, dsel, exportFlds);
            } catch (DBException dbe) {
                Print.logException("Error exporting table: " + TABLE_NAME(), dbe);
                System.exit(99);
            }
            System.exit(0);
        }

        /* load */
        if (RTConfig.hasProperty(ARG_LOAD) || RTConfig.hasProperty(ARG_VALIDATE)) {
            opts++;
            boolean validateOnly = RTConfig.hasProperty(ARG_VALIDATE);
            String loadFileName = validateOnly? RTConfig.getString(ARG_VALIDATE,"") : RTConfig.getString(ARG_LOAD,"");
            File loadFile = !loadFileName.equals("")? new File(loadFileName) : null;
            if ((loadFile == null) || !loadFile.exists() || !loadFile.getName().toLowerCase().endsWith(CSV_EXT)) {
                Print.logError("Invalid or non-existent file specified: " + loadFile);
                usage();
            }
            DBFactory<Geozone> fact = Geozone.getFactory();
            try {
                if (fact.tableExists()) {
                    if (validateOnly) {
                        Print.logInfo("Validating '" + TABLE_NAME() + "' file: " + loadFile);
                    } else {
                        Print.logInfo("Loading '" + TABLE_NAME() + "' from file: " + loadFile);
                    }
                    fact.loadTable(loadFile, new ZoneLoadValidator(acctID,validateOnly));
                    Print.logInfo("Successfully validated/loaded '" + TABLE_NAME() + "' file: " + loadFile);
                } else {
                    Print.logError("Table does not exist: " + TABLE_NAME());
                    System.exit(99);
                }
            } catch (DBException dbe) {
                Print.logException("Error loading/validating table: " + TABLE_NAME(), dbe);
            }
            System.exit(0);
        }
        
        /* test */
        if (RTConfig.hasProperty(ARG_TEST)) {
            opts++;
            GeoPoint gp = new GeoPoint(RTConfig.getString(ARG_TEST,""));
            Geozone gz = Geozone.getGeozone(account, null, gp, false);
            if (gz == null) {
                Print.logError("Point not found: " + gp);
            } else {
                Print.logInfo("Found description: " + gz.getDescription());
            }
            System.exit(0);
        }

        /* EventData test */
        if (RTConfig.hasProperty(ARG_EVTEST)) {
            opts++;
            String dtStr = RTConfig.getString(ARG_EVTEST,null);
            try {
                DateTime dt = DateTime.parseArgumentDate(dtStr, null, false);
                long dte = dt.getTimeSec();
                EventData ed[] = EventData.getRangeEvents(
                    acctID, devID, 
                    dte-1, dte+1, 
                    null/*statusCodes*/, 
                    true/*validGPS*/, 
                    null/*limitType*/, 0L/*limit*/, true/*ascending*/, 
                    null/*addtnlSelect*/);
                if ((ed == null) || (ed.length == 0)) {
                    throw new DBException("No EvenData records found near the specified date/time");
                }
                EventData prevED = ed[0].getPreviousEventData(true);
                if (prevED != null) { // may be null
                    GeoPoint gp = prevED.getGeoPoint();
                    Geozone  gz = Geozone.getGeozone(account, null, gp, false);
                    if (gz == null) {
                        Print.logInfo("Prev point: No Geozone: " + gp);
                    } else {
                        Print.logInfo("Prev point: Found Geozone: " + gz.getDescription());
                    }
                }
                EventData thisED = ed[0];
                if (thisED != null) { // is NEVER null
                    GeoPoint gp = thisED.getGeoPoint();
                    Geozone  gz = Geozone.getGeozone(account, null, gp, false);
                    if (gz == null) {
                        Print.logInfo("This point: No Geozone: " + gp);
                    } else {
                        Print.logInfo("This point: Found Geozone: " + gz.getDescription());
                    }
                }
                EventData nextED = ed[0].getPreviousEventData(true);
                if (nextED != null) { // may be null
                    GeoPoint gp = nextED.getGeoPoint();
                    Geozone  gz = Geozone.getGeozone(account, null, gp, false);
                    if (gz == null) {
                        Print.logInfo("Next point: No Geozone: " + gp);
                    } else {
                        Print.logInfo("Next point: Found Geozone: " + gz.getDescription());
                    }
                }
            } catch (DateTime.DateParseException dpe) {
                Print.logException("Invalid event date: " + dtStr, dpe);
            } catch (DBException dbe) {
                Print.logException("DB error", dbe);
            }
            System.exit(0);
        }

        /* display usage if we reach here */
        if (opts <= 0) {
            usage();
        }

    }

}
