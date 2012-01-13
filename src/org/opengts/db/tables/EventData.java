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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/02  Martin D. Flynn
//     -Added field formatting support for CSV output
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -Added new 'FLD_address' field
//     -Added 'FLD_thermoAverage#' fields.
//  2007/02/26  Martin D. Flynn
//     -Added 'FLD_odometerKM' table column ('FLD_distanceKM' is used for
//      'tripometer' purposes).
//  2007/02/28  Martin D. Flynn
//     -Added column 'FLD_horzAccuracy' (meters)
//     -Removed columns FLD_geofenceID2, FLD_thermoAverage#, & FLD_topSpeedKPH.  
//      For specific custom solutions these can easily be added back, but for a  
//      general solution they are not necessary.
//  2007/03/11  Martin D. Flynn
//     -Added convenience methods 'getSpeedString' and 'getHeadingString'.
//     -Added 'statusCodes[]' and 'additionalSelect' arguments to 'getRangeEvents'
//      method.
//  2007/03/25  Martin D. Flynn
//     -Changed FLD_geofenceID1 to FLD_geozoneIndex, and added FLD_geozoneID
//     -Moved to 'org.opengts.db.tables'
//  2007/05/06  Martin D. Flynn
//     -Added 'FLD_creationTime' column support.
//  2007/06/13  Martin D. Flynn
//     -Added 'FLD_subdivision' column support (state/province/etc).
//  2007/07/14  Martin D. Flynn
//     -Added various optional fields/columns
//  2007/07/27  Martin D. Flynn
//     -Added custom/optional column 'FLD_driver'
//  2007/09/16  Martin D. Flynn
//     -Added 'getFieldValueString' method to return a formatted String 
//      representation of the specified field.
//     -Integrated DBSelect
//  2007/11/28  Martin D. Flynn
//     -Added columns FLD_brakeGForce, FLD_city, FLD_postalCode
//     -"getTimestampString()" now returns a time based on the Account TimeZone.
//     -Apply 'Departed' geozone description for STATUS_GEOFENCE_DEPART events.
//  2007/01/10  Martin D. Flynn
//     -Added method 'countRangeEvents(...)' to return the number of events matching  
//      the specified criteria.
//  2008/02/04  Martin D. Flynn
//     -Added custom/optional column 'FLD_fuelTotal', 'FLD_fuelIdle', 'FLD_engineRpm'
//  2008/02/17  Martin D. Flynn
//     -Added column 'FLD_inputMask'
//  2008/02/21  Martin D. Flynn
//     -Moved J1708/J1587 encoding/decoding to 'org.opengts.dbtools.DTOBDFault'
//  2008/03/12  Martin D. Flynn
//     -Added additional date/time key values for 'getFieldValueString' method.
//  2008/03/28  Martin D. Flynn
//     -Incorporate "DBRecord.select(DBSelect,...) method
//  2008/04/11  Martin D. Flynn
//     -Added status code icon index lookup to "getMapIconIndex(...)"
//  2008/05/14  Martin D. Flynn
//     -Added FLD_country, FLD_stateProvince, FLD_streetAddress
//  2008/05/20  Martin D. Flynn
//     -Added message to assist in determining reason for lack of ReverseGeocoding
//  2008/06/20  Martin D. Flynn
//     -Moved custom field initialization to StartupInit.
//     -EventData record now ignores invalid field references (ie. no displayed errors).
//  2008/07/08  Martin D. Flynn
//     -Added field FLD_costCenter to 'CustomFieldInfo' group.
//     -Rearranged fields/columns to reduce the size of the basic record structure.
//  2008/09/12  Martin D. Flynn
//     -Added field/column FLD_satelliteCount, FLD_batteryLevel
//  2008/10/16  Martin D. Flynn
//     -Modified "getDefaultMapIconIndex" to use the 'iconKeys' table to look up the
//      custom icon index.
//  2008/12/01  Martin D. Flynn
//     -'getDefaultMapIconIndex' now returns Device pushpinID for fleet maps.
//     -Added KEY_HEADING to 'getFieldValueString(...)' support.
//  2009/02/20  Martin D. Flynn
//     -Added field FLD_vertAccuracy
//  2009/05/01  Martin D. Flynn
//     -Added fields FLD_speedLimitKPH, FLD_isTollRoad
//  2009/07/01  Martin D. Flynn
//     -Renamed "getMapIconIndex(...)" to "getPushpinIconIndex(...)"
//  2009/11/01  Martin D. Flynn
//     -Changed 'FLD_driver' to 'FLD_driverID', and 'FLD_entity' to 'FLD_entityID'
//  2009/12/16  Martin D. Flynn
//     -Added field FLD_driverMessage, FLD_jobNumber to 'CustomFieldInfo' group.
//  2010/01/29  Martin D. Flynn
//     -Added field FLD_oilPressure, FLD_ptoEngaged to 'CANBUSFieldInfo' group.
//  2010/04/11  Martin D. Flynn
//     -Modified "DeviceDescriptionComparator" to a case-insensitive sort
//     -Added FLD_fuelPTO
//  2010/09/09  Martin D. Flynn
//     -Custom 'Device' pushpins now override statusCode custom pushpins
//     -Removed FLD_topSpeedKPH
//     -Added FLD_ambientTemp, FLD_barometer, FLD_rfidTag, FLD_oilTemp
//  2010/10/25  Martin D. Flynn
//     -Added FLD_appliedPressure, FLD_sampleIndex, FLD_sampleID
//  2010/11/29  Martin D. Flynn
//     -Moved FLD_appliedPressure to WorkOrderSample
//  2011/01/28  Martin D. Flynn
//     -Fixed "getNextEventData" (bug filed on SourceForge)
//     -Fixed "setInputMask" to mask value to 32-bits
//     -Added FLD_intakeTemp, FLD_throttlePos, FLD_airPressure
//  2011/03/08  Martin D. Flynn
//     -Added FLD_driverStatus
//     -Added optional "GarminFieldInfo" fields
//  2011/04/01  Martin D. Flynn
//     -Truncate "address" data length to specified table column length
//  2011/05/13  Martin D. Flynn
//     -Added FLD_cabinTemp, FLD_airFilterPressure, FLD_engineTorque
//  2011/06/16  Martin D. Flynn
//     -Truncate "streetAddress", "stateProvince" to table column length
//     -Added FLD_fuelPressure, FLD_cellTowerID, FLD_locationAreaCode
//     -Added method "reloadAddress" to reload address columns
//  2011/07/01  Martin D. Flynn
//     -Added the field/column descriptions to the LocalStrings tables.
//     -Minor changes to the order of checking criteria for choosing a pushpin.
//     -Added FLD_batteryVolts
//     -Bounds check "gpsAge" (make sure it is >= 0)
//  2011/08/21  Martin D. Flynn
//     -Added FLD_tirePressure, FLD_tireTemp to "CANBUSFieldInfo" fields
//  2011/10/03  Martin D. Flynn
//     -Added FLD_turboPressure
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.geocoder.*;
import org.opengts.cellid.*;

import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;

import org.opengts.cellid.CellTower;

public class EventData
    extends DeviceRecord<EventData>
    implements EventDataProvider, GeoPointProvider
{

    // ------------------------------------------------------------------------

    /* static initializers */
    static {
        EventData.getDeviceDescriptionComparator();
    }

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String OPTCOLS_AddressFieldInfo             = "startupInit.EventData.AddressFieldInfo";
    public static final String OPTCOLS_GPSFieldInfo                 = "startupInit.EventData.GPSFieldInfo";
    public static final String OPTCOLS_CustomFieldInfo              = "startupInit.EventData.CustomFieldInfo";
    public static final String OPTCOLS_GarminFieldInfo              = "startupInit.EventData.GarminFieldInfo";
    public static final String OPTCOLS_CANBUSFieldInfo              = "startupInit.EventData.CANBUSFieldInfo";
    public static final String OPTCOLS_AtmosphereFieldInfo          = "startupInit.EventData.AtmosphereFieldInfo";
    public static final String OPTCOLS_ThermoFieldInfo              = "startupInit.EventData.ThermoFieldInfo";
    public static final String OPTCOLS_AnalogFieldInfo              = "startupInit.EventData.AnalogFieldInfo";
    public static final String OPTCOLS_AutoIncrementIndex           = "startupInit.EventData.AutoIncrementIndex";
    public static final String OPTCOLS_EndOfDaySummary              = "startupInit.EventData.EndOfDaySummary";
    public static final String OPTCOLS_ServingCellTowerData         = "startupInit.EventData.ServingCellTowerData";
    public static final String OPTCOLS_NeighborCellTowerData        = "startupInit.EventData.NeighborCellTowerData";
    public static final String OPTCOLS_WorkZoneGridData             = "startupInit.EventData.WorkZoneGridData";
    public static final String OPTCOLS_CreationTimeMillisecond      = "startupInit.EventData.CreationTimeMillisecond";

    // ------------------------------------------------------------------------

    // "pushkey"
    public  static final String  KEY_pushkey            = "pushkey";
    public  static final String  ALTKEY_eq_pushkey      = "altkey=" + KEY_pushkey;
    public  static boolean ENABLE_DATA_PUSH()
    {
        return RTConfig.getBoolean(OPTCOLS_CreationTimeMillisecond, DBConfig.hasOptPackage());
    }
    
    // ------------------------------------------------------------------------

    public enum LimitType {
        FIRST,
        LAST
    };

    // ------------------------------------------------------------------------

    public  static final double  INVALID_TEMPERATURE    = -9999.0;
    public  static final double  TEMPERATURE_LIMIT      = 126.0;    // degrees C

    // ------------------------------------------------------------------------
    // standard map icons (see "getPushpinIconIndex")

    /* color pushpins */
    // This represents the standard color "position" for the first 10 pushpin icons
    // The ordering is established in the method "PushpinIcon._DefaultPushpinIconMap()"
    public  static final int     ICON_PUSHPIN_BLACK     = 0;
    public  static final int     ICON_PUSHPIN_BROWN     = 1;
    public  static final int     ICON_PUSHPIN_RED       = 2;
    public  static final int     ICON_PUSHPIN_ORANGE    = 3;
    public  static final int     ICON_PUSHPIN_YELLOW    = 4;
    public  static final int     ICON_PUSHPIN_GREEN     = 5;
    public  static final int     ICON_PUSHPIN_BLUE      = 6;
    public  static final int     ICON_PUSHPIN_PURPLE    = 7;
    public  static final int     ICON_PUSHPIN_GRAY      = 8;
    public  static final int     ICON_PUSHPIN_WHITE     = 9;
 
    public static int _getPushpinIconIndex(String val, OrderedSet<String> iconKeys, int dft)
    {

        if (val == null) { 
            // skip to default below
        } else
        if (iconKeys != null) {
            int ndx = iconKeys.indexOf(val);
            if (ndx >= 0) {
                return ndx;
            }
            // skip to default below
        } else {
            // 'iconKeys' should not be null, however, if it is, this will return the index
            // of the standard colors.
            if (val.equalsIgnoreCase("black" )) { return ICON_PUSHPIN_BLACK ; }
            if (val.equalsIgnoreCase("brown" )) { return ICON_PUSHPIN_BROWN ; }
            if (val.equalsIgnoreCase("red"   )) { return ICON_PUSHPIN_RED   ; }
            if (val.equalsIgnoreCase("orange")) { return ICON_PUSHPIN_ORANGE; }
            if (val.equalsIgnoreCase("yellow")) { return ICON_PUSHPIN_YELLOW; }
            if (val.equalsIgnoreCase("green" )) { return ICON_PUSHPIN_GREEN ; }
            if (val.equalsIgnoreCase("blue"  )) { return ICON_PUSHPIN_BLUE  ; }
            if (val.equalsIgnoreCase("purple")) { return ICON_PUSHPIN_PURPLE; }
            if (val.equalsIgnoreCase("gray"  )) { return ICON_PUSHPIN_GRAY  ; }
            if (val.equalsIgnoreCase("white" )) { return ICON_PUSHPIN_WHITE ; }
            // skip to default below
        }
        
        /* return default */
        //Print.logInfo("Returning default pushpin: " + dft);
        return dft;
        
    }

    // ------------------------------------------------------------------------

    public static final EventData[] EMPTY_ARRAY         = new EventData[0];

    public static       int         AddressColumnLength = -1;
    public static       int         StreetColumnLength  = -1;
    public static       int         CityColumnLength    = -1;
    public static       int         StateColumnLength   = -1;

    // ------------------------------------------------------------------------
    // GPS fix type

    public enum GPSFixType implements EnumTools.StringLocale, EnumTools.IntValue {
        UNKNOWN             (0, I18N.getString(EventData.class,"EventData.gpsFix.unknown", "Unknown")),
        NONE                (1, I18N.getString(EventData.class,"EventData.gpsFix.none"   , "None"   )),
        n2D                 (2, I18N.getString(EventData.class,"EventData.gpsFix.2D"     , "2D"     )),
        n3D                 (3, I18N.getString(EventData.class,"EventData.gpsFix.3D"     , "3D"     ));
        private int         vv = 0;
        private I18N.Text   aa = null;
        GPSFixType(int v, I18N.Text a)          { vv=v; aa=a; }
        public int     getIntValue()            { return vv; }
        public String  toString()               { return aa.toString(); }
        public String  toString(Locale loc)     { return aa.toString(loc); }
    }

    public static GPSFixType getGPSFixType(EventData e)
    {
        return (e != null)? EnumTools.getValueOf(GPSFixType.class,e.getGpsFixType()) : EnumTools.getDefault(GPSFixType.class);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "EventData";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* pseudo field definition */
    // currently only used by EventUtil
    public static final String PFLD_deviceDesc          = DBRecord.PSEUDO_FIELD_CHAR + "deviceDesc";

    /* field definition */
    // Standard fields
    public static final String FLD_timestamp            = "timestamp";              // Unix Epoch time
    public static final String FLD_statusCode           = "statusCode";
    public static final String FLD_latitude             = "latitude";
    public static final String FLD_longitude            = "longitude";
    public static final String FLD_gpsAge               = "gpsAge";                 // fix age (seconds)
    public static final String FLD_speedKPH             = "speedKPH";
    public static final String FLD_heading              = "heading";
    public static final String FLD_altitude             = "altitude";               // meters
    public static final String FLD_transportID          = Transport.FLD_transportID;
    public static final String FLD_inputMask            = "inputMask";              // bitmask
    // Address fields
    public static final String FLD_address              = "address";                // custom or reverse-geocoded address
    // Misc fields
    public static final String FLD_dataSource           = "dataSource";             // gprs, satellite, etc.
    public static final String FLD_rawData              = "rawData";                // optional
    public static final String FLD_distanceKM           = "distanceKM";             // tripometer
    public static final String FLD_odometerKM           = "odometerKM";             // vehicle odometer
    public static final String FLD_geozoneIndex         = "geozoneIndex";           // Geozone Index
    public static final String FLD_geozoneID            = Geozone.FLD_geozoneID;    // Geozone ID
    private static final DBField StandardFieldInfo[] = {
        // Key fields
        newField_accountID(true,(ENABLE_DATA_PUSH()?ALTKEY_eq_pushkey:"")),
        newField_deviceID(true,(ENABLE_DATA_PUSH()?ALTKEY_eq_pushkey:"")),
        new DBField(FLD_timestamp      , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.timestamp"          , "Timestamp"             ), "key=true"),
        new DBField(FLD_statusCode     , Integer.TYPE  , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.statusCode"         , "Status Code"           ), "key=true editor=statusCode format=X2"),
        // Standard fields
        new DBField(FLD_latitude       , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.latitude"           , "Latitude"              ), "format=#0.00000"),
        new DBField(FLD_longitude      , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.longitude"          , "Longitude"             ), "format=#0.00000"),
        new DBField(FLD_gpsAge         , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.gpsAge"             , "GPS Fix Age"           ), ""),
        new DBField(FLD_speedKPH       , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.speedKPH"           , "Speed"                 ), "format=#0.0 units=speed"),
        new DBField(FLD_heading        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.heading"            , "Heading"               ), "format=#0.0"),
        new DBField(FLD_altitude       , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.altitude"           , "Altitude"              ), "format=#0.0"),
        new DBField(FLD_transportID    , String.class  , DBField.TYPE_XPORT_ID()  , I18N.getString(EventData.class,"EventData.fld.transportID"        , "Transport ID"          ), ""),
        new DBField(FLD_inputMask      , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.inputMask"          , "Input Mask"            ), "format=X4"),
        // Address fields
        new DBField(FLD_address        , String.class  , DBField.TYPE_ADDRESS()   , I18N.getString(EventData.class,"EventData.fld.address"            , "Full Address"          ), "utf8=true"),
        // Misc fields
        new DBField(FLD_dataSource     , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.dataSource"         , "Data Source"           ), ""),
        new DBField(FLD_rawData        , String.class  , DBField.TYPE_TEXT        , I18N.getString(EventData.class,"EventData.fld.rawData"            , "Raw Data"              ), ""),
        new DBField(FLD_distanceKM     , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.distanceKM"         , "Distance KM"           ), "format=#0.0 units=distance"),
        new DBField(FLD_odometerKM     , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.odometerKM"         , "Odometer KM"           ), "format=#0.0 units=distance"),
        new DBField(FLD_geozoneIndex   , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.geozoneIndex"       , "Geozone Index"         ), ""),
        new DBField(FLD_geozoneID      , String.class  , DBField.TYPE_ZONE_ID()   , I18N.getString(EventData.class,"EventData.fld.geozoneID"          , "Geozone ID"            ), ""),
        // Common fields
        newField_creationTime(RTConfig.getBoolean(DBConfig.PROP_EventData_keyedCreationTime,false)),
    };
    
    // Extra Address fields 
    // startupInit.EventData.AddressFieldInfo=true
    public static final String FLD_streetAddress        = "streetAddress";          // reverse-geocoded street address
    public static final String FLD_city                 = "city";                   // reverse-geocoded city
    public static final String FLD_stateProvince        = "stateProvince";          // reverse-geocoded state
    public static final String FLD_postalCode           = "postalCode";             // reverse-geocoded postal code
    public static final String FLD_country              = "country";                // reverse-geocoded country
    public static final String FLD_subdivision          = "subdivision";            // reverse-geocoded subdivision (ie "US/CA")
    public static final String FLD_speedLimitKPH        = "speedLimitKPH";          // reverse-geocoded speed-limit ('0' for unavailable)
    public static final String FLD_isTollRoad           = "isTollRoad";             // reverse-geocoded toll-road indicator
    public static final DBField AddressFieldInfo[] = {
        new DBField(FLD_streetAddress  , String.class  , DBField.TYPE_STRING(90)  , I18N.getString(EventData.class,"EventData.fld.streetAddress"      , "Street Address"        ), "utf8=true"),
        new DBField(FLD_city           , String.class  , DBField.TYPE_STRING(40)  , I18N.getString(EventData.class,"EventData.fld.city"               , "City"                  ), "utf8=true"),
        new DBField(FLD_stateProvince  , String.class  , DBField.TYPE_STRING(40)  , I18N.getString(EventData.class,"EventData.fld.stateProvince"      , "State/Privince"        ), "utf8=true"), 
        new DBField(FLD_postalCode     , String.class  , DBField.TYPE_STRING(16)  , I18N.getString(EventData.class,"EventData.fld.postalCode"         , "Postal Code"           ), "utf8=true"),
        new DBField(FLD_country        , String.class  , DBField.TYPE_STRING(40)  , I18N.getString(EventData.class,"EventData.fld.country"            , "Country"               ), "utf8=true"), 
        new DBField(FLD_subdivision    , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.subdivision"        , "Subdivision"           ), "utf8=true"),
        new DBField(FLD_speedLimitKPH  , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.speedLimitKPH"      , "Speed Limit"           ), "format=#0.0 units=speed"),
        new DBField(FLD_isTollRoad     , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , I18N.getString(EventData.class,"EventData.fld.isTollRoad"         , "Toll Road"             ), ""),
    };

    // Device/Modem/GPS fields
    // startupInit.EventData.GPSFieldInfo=true
    public static final String FLD_gpsFixType           = "gpsFixType";             // fix type (0/1=None, 2=2D, 3=3D)
    public static final String FLD_horzAccuracy         = "horzAccuracy";           // horizontal accuracy (meters)
    public static final String FLD_vertAccuracy         = "vertAccuracy";           // vertical accuracy (meters)
    public static final String FLD_HDOP                 = "HDOP";                   // HDOP
    public static final String FLD_satelliteCount       = "satelliteCount";         // number of satellites
    public static final String FLD_batteryLevel         = "batteryLevel";           // battery level %
    public static final String FLD_batteryVolts         = "batteryVolts";           // battery volts
    public static final String FLD_signalStrength       = "signalStrength";         // signal strength (RSSI)
    public static final DBField GPSFieldInfo[] = {
        new DBField(FLD_gpsFixType     , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.gpsFixType"         , "GPS Fix Type"          ), "enum=EventData$GPSFixType"),
        new DBField(FLD_horzAccuracy   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.horzAccuracy"       , "Horizontal Accuracy"   ), "format=#0.0"),
        new DBField(FLD_vertAccuracy   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.vertAccuracy"       , "Vertical Accuracy"     ), "format=#0.0"),
        new DBField(FLD_HDOP           , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.HDOP"               , "HDOP"                  ), "format=#0.0"),
        new DBField(FLD_satelliteCount , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.satelliteCount"     , "Number of Satellites"  ), ""),
        new DBField(FLD_batteryLevel   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.batteryLevel"       , "Battery Level %"       ), "format=#0.0 units=percent"),
        new DBField(FLD_batteryVolts   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.batteryVolts"       , "Battery Volts"         ), "format=#0.0"),
        new DBField(FLD_signalStrength , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.signalStrength"     , "Signal Strength (RSSI)"), "format=#0.0"),
    };

    // Misc custom fields
    // startupInit.EventData.CustomFieldInfo=true
    public static final String FLD_entityID             = "entityID";               // trailer/package
    public static final String FLD_driverID             = "driverID";               // user/driver
    public static final String FLD_driverStatus         = "driverStatus";           // driver status
    public static final String FLD_driverMessage        = "driverMessage";          // driver message
    public static final String FLD_emailRecipient       = "emailRecipient";         // recipient email address(es)
    public static final String FLD_sensorLow            = "sensorLow";              // digital analog
    public static final String FLD_sensorHigh           = "sensorHigh";             // digital analog
    public static final String FLD_costCenter           = "costCenter";             // associated cost center
    public static final String FLD_jobNumber            = "jobNumber";              // associated job number
    public static final String FLD_rfidTag              = "rfidTag";                // RFID/BarCode Tag
    public static final String FLD_attachType           = "attachType";             // event attachment type (image:jpeg,gif,png,etc)
    public static final String FLD_attachData           = "attachData";             // event attachment data (image, etc)
    public static final DBField CustomFieldInfo[] = {
        // (may be externally accessed by DBConfig.DBInitialization)
        // Custom fields (may also need to be supported by "org.opengts.servers.gtsdmtp.DeviceDBImpl")
        new DBField(FLD_entityID       , String.class  , DBField.TYPE_ENTITY_ID() , I18N.getString(EventData.class,"EventData.fld.entityID"           , "Trailer/Entity"        ), "utf8=true"),
        new DBField(FLD_driverID       , String.class  , DBField.TYPE_DRIVER_ID() , I18N.getString(EventData.class,"EventData.fld.driverID"           , "Driver/User"           ), "utf8=true"),
        new DBField(FLD_driverStatus   , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.driverStatus"       , "Driver Status"         ), ""),
        new DBField(FLD_driverMessage  , String.class  , DBField.TYPE_STRING(200) , I18N.getString(EventData.class,"EventData.fld.driverMessage"      , "Driver Message"        ), "utf8=true"),
      //new DBField(FLD_emailRecipient , String.class  , DBField.TYPE_STRING(200) , I18N.getString(EventData.class,"EventData.fld.emailRecipient"     , "EMail Recipients"      ), ""),
        new DBField(FLD_sensorLow      , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.sensorLow"          , "Sensor Low"            ), "format=X4"),
        new DBField(FLD_sensorHigh     , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.sensorHigh"         , "Sensor High"           ), "format=X4"),
        new DBField(FLD_costCenter     , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.costCenter"         , "Cost Center"           ), ""),
        new DBField(FLD_jobNumber      , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.jobNumber"          , "Job Number"            ), ""),
        new DBField(FLD_rfidTag        , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.rfidTag"            , "RFID/BarCode Tag"      ), ""),
        new DBField(FLD_attachType     , String.class  , DBField.TYPE_STRING(64)  , I18N.getString(EventData.class,"EventData.fld.attachType"         , "Attachment MIME Type"  ), ""),
        new DBField(FLD_attachData     , byte[].class  , DBField.TYPE_BLOB        , I18N.getString(EventData.class,"EventData.fld.attachData"         , "Attachment Data"       ), ""),
    };

    // Garmin ETA fields
    // startupInit.EventData.GarminFieldInfo=true
    public static final String FLD_etaTimestamp         = "etaTimestamp";           // ETA time
    public static final String FLD_etaUniqueID          = "etaUniqueID";            // ETA ID
    public static final String FLD_etaDistanceKM        = "etaDistanceKM";          // ETA distance
    public static final String FLD_etaLatitude          = "etaLatitude";            // ETA latitude
    public static final String FLD_etaLongitude         = "etaLongitude";           // ETA longitude
    public static final String FLD_stopID               = "stopID";                 // STOP ID
    public static final String FLD_stopStatus           = "stopStatus";             // STOP Status
    public static final String FLD_stopIndex            = "stopIndex";              // STOP Index
    public static final DBField GarminFieldInfo[] = {
        new DBField(FLD_etaTimestamp   , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.etaTimestamp"       , "ETA Time"              ), "format=time"),
        new DBField(FLD_etaUniqueID    , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.etaUniqueID"        , "ETA Unique ID"         ), ""),
        new DBField(FLD_etaDistanceKM  , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.etaDistanceKM"      , "ETA Distance KM"       ), "format=#0.0 units=distance"),
        new DBField(FLD_etaLatitude    , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.etaLatitude"        , "ETA Latitude"          ), "format=#0.00000"),
        new DBField(FLD_etaLongitude   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.etaLongitude"       , "ETA Longitude"         ), "format=#0.00000"),
        new DBField(FLD_stopID         , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.stopID"             , "STOP ID"               ), ""),
        new DBField(FLD_stopStatus     , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.stopStatus"         , "STOP Status"           ), ""),
        new DBField(FLD_stopIndex      , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.stopIndex"          , "STOP Index"            ), ""),
    };

    // OBD fields
    // startupInit.EventData.J1708FieldInfo=true
    // startupInit.EventData.CANBUSFieldInfo=true
  //public static final String FLD_obdType              = "obdType";                // OBD type [0,1=J1708, 2=J1939, 3=OBDII]
    public static final String FLD_fuelTotal            = "fuelTotal";              // liters
    public static final String FLD_engineRpm            = "engineRpm";              // rpm
    public static final String FLD_engineHours          = "engineHours";            // hours
    public static final String FLD_engineLoad           = "engineLoad";             // %
    public static final String FLD_engineTorque         = "engineTorque";           // %
    public static final String FLD_idleHours            = "idleHours";              // hours
    public static final String FLD_workHours            = "workHours";              // hours
    public static final String FLD_transOilTemp         = "transOilTemp";           // C
    public static final String FLD_coolantLevel         = "coolantLevel";           // %
    public static final String FLD_coolantTemp          = "coolantTemp";            // C
    public static final String FLD_intakeTemp           = "intakeTemp";             // C
    public static final String FLD_brakeGForce          = "brakeGForce";            // G (9.80665 m/s/s)
    public static final String FLD_acceleration         = "acceleration";           // m/s/s
    public static final String FLD_oilPressure          = "oilPressure";            // kPa
    public static final String FLD_oilLevel             = "oilLevel";               // %
    public static final String FLD_oilTemp              = "oilTemp";                // C
    public static final String FLD_airPressure          = "airPressure";            // kPa
    public static final String FLD_airFilterPressure    = "airFilterPressure";      // kPa
    public static final String FLD_turboPressure        = "turboPressure";          // kPa
    public static final String FLD_ptoEngaged           = "ptoEngaged";             // boolean
    public static final String FLD_ptoHours             = "ptoHours";               // hours
    public static final String FLD_throttlePos          = "throttlePos";            // %
    public static final String FLD_brakePos             = "brakePos";               // %
    public static final String FLD_j1708Fault           = "j1708Fault";             // 
    public static final String FLD_faultCode            = "faultCode";              // J1708/J1939/OBDII fault code
    public static final String FLD_malfunctionLamp      = "malfunctionLamp";        // J1939/OBDII 
    // --- less useful OBD fields (not always available)
    public static final String FLD_fuelLevel            = "fuelLevel";              // %
    public static final String FLD_fuelIdle             = "fuelIdle";               // liters
    public static final String FLD_fuelPTO              = "fuelPTO";                // liters
    public static final String FLD_vBatteryVolts        = "vBatteryVolts";          // vehicle battery voltage
    // --- other OBD fields
    public static final String FLD_fuelPressure         = "fuelPressure";           // kPa
    public static final String FLD_fuelUsage            = "fuelUsage";              // liters per hour
    public static final String FLD_fuelTemp             = "fuelTemp";               // C
    public static final String FLD_fuelEconomy          = "fuelEconomy";            // kilometer per liter (average)
    public static final String FLD_brakePressure        = "brakePressure";          // kPa
    // --- tire (tyre) temperature/pressure
    public static final String FLD_tirePressure         = "tirePressure";           // kPa
    public static final String FLD_tireTemp             = "tireTemp";               // C
    // ---
    public static final DBField CANBUSFieldInfo[] = { 
        // (may be externally accessed by DBConfig.DBInitialization)
        // Custom fields (may also need to be supported by "org.opengts.servers.gtsdmtp.DeviceDBImpl")
      //new DBField(FLD_obdType          , Integer.TYPE  , DBField.TYPE_UINT16     , I18N.getString(EventData.class,"EventData.fld.obdType"            , "OBD Type"              ), ""),
        new DBField(FLD_fuelPressure     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelPressure"       , "Fuel Pressure"         ), "format=#0.00 units=pressure"),
        new DBField(FLD_fuelUsage        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelUsage"          , "Fuel Usage"            ), "format=#0.00"),
        new DBField(FLD_fuelTemp         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelTemp"           , "Fuel Temp"             ), "format=#0.00 units=temp"),
        new DBField(FLD_fuelLevel        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelLevel"          , "Fuel Level"            ), "format=#0.0 units=percent"),
        new DBField(FLD_fuelEconomy      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelEconomy"        , "Fuel Economy"          ), "format=#0.0 units=econ"),
        new DBField(FLD_fuelTotal        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelTotal"          , "Total Fuel Used"       ), "format=#0.0 units=volume"),
        new DBField(FLD_fuelIdle         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelIdle"           , "Idle Fuel Used"        ), "format=#0.0 units=volume"),
        new DBField(FLD_fuelPTO          , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelPTO"            , "PTO Fuel Used"         ), "format=#0.0 units=volume"),
        new DBField(FLD_engineRpm        , Long.TYPE     , DBField.TYPE_UINT32     , I18N.getString(EventData.class,"EventData.fld.engineRpm"          , "Engine RPM"            ), ""),
        new DBField(FLD_engineHours      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.engineHours"        , "Engine Hours"          ), "format=#0.0"),
        new DBField(FLD_engineLoad       , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.engineLoad"         , "Engine Load"           ), "format=#0.00 units=percent"),
        new DBField(FLD_engineTorque     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.engineTorque"       , "Engine Torque %"       ), "format=#0.00 units=percent"),
        new DBField(FLD_idleHours        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.idleHours"          , "Idle Hours"            ), "format=#0.0"),
        new DBField(FLD_workHours        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.workHours"          , "Work Hours"            ), "format=#0.0"),
        new DBField(FLD_transOilTemp     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.transOilTemp"       , "Transmission Oil Temp" ), "format=#0.00 units=temp"),
        new DBField(FLD_coolantLevel     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.coolantLevel"       , "Coolant Level"         ), "format=#0.00 units=percent"),
        new DBField(FLD_coolantTemp      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.coolantTemp"        , "Coolant Temperature"   ), "format=#0.00 units=temp"),
        new DBField(FLD_intakeTemp       , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.intakeTemp"         , "Intake Temperature"    ), "format=#0.00 units=temp"),
        new DBField(FLD_brakeGForce      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.brakeGForce"        , "Brake G Force"         ), "format=#0.00"),
        new DBField(FLD_acceleration     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.acceleration"       , "Acceleration"          ), "format=#0.00"),
        new DBField(FLD_brakePressure    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.brakePressure"      , "Brake Pressure"        ), "format=#0.00 units=pressure"),
        new DBField(FLD_oilPressure      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.oilPressure"        , "Oil Pressure"          ), "format=#0.00 units=pressure"),
        new DBField(FLD_oilLevel         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.oilLevel"           , "Oil Level"             ), "format=#0.00"),
        new DBField(FLD_oilTemp          , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.oilTemp"            , "Oil Temperature"       ), "format=#0.00 units=temp"),
        new DBField(FLD_airPressure      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.airPressure"        , "Air Supply Pressure"   ), "format=#0.00 units=pressure"),
        new DBField(FLD_airFilterPressure, Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.airFilterPressure"  , "Air Filter Pressure"   ), "format=#0.00 units=pressure"),
        new DBField(FLD_turboPressure    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.turboPressure"      , "Turbo Pressure"        ), "format=#0.00 units=pressure"),
        new DBField(FLD_ptoEngaged       , Boolean.TYPE  , DBField.TYPE_BOOLEAN    , I18N.getString(EventData.class,"EventData.fld.ptoEngaged"         , "PTO Engaged"           ), ""),
        new DBField(FLD_ptoHours         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.ptoHours"           , "PTO Hours"             ), "format=#0.0"),
        new DBField(FLD_throttlePos      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.throttlePos"        , "Throttle Position"     ), "format=#0.0 units=percent"),
        new DBField(FLD_brakePos         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.brakePos"           , "Brake Position"        ), "format=#0.0 units=percent"),
        new DBField(FLD_vBatteryVolts    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.vBatteryVolts"      , "Vehicle Battery Volts" ), "format=#0.0"),
        new DBField(FLD_j1708Fault       , Long.TYPE     , DBField.TYPE_UINT64     , I18N.getString(EventData.class,"EventData.fld.j1708Fault"         , "Fault Code"            ), ""),
        new DBField(FLD_faultCode        , String.class  , DBField.TYPE_STRING(96) , I18N.getString(EventData.class,"EventData.fld.faultCode"          , "Fault String"          ), ""),
        new DBField(FLD_malfunctionLamp  , Boolean.TYPE  , DBField.TYPE_BOOLEAN    , I18N.getString(EventData.class,"EventData.fld.malfunctionLamp"    , "Malfuntion Lamp"       ), ""),
        new DBField(FLD_tirePressure     , String.class  , DBField.TYPE_STRING(140), I18N.getString(EventData.class,"EventData.fld.tirePressure"       , "Tire Pressure"         ), "format=#0.00 units=pressure"),
        new DBField(FLD_tireTemp         , String.class  , DBField.TYPE_STRING(140), I18N.getString(EventData.class,"EventData.fld.tireTemp"           , "Tire Temperature"      ), "format=#0.00 units=temp"),
    };

    // Atmosphere fields
    // startupInit.EventData.AtmosphereFieldInfo=true
    public static final String FLD_barometer            = "barometer";              // kPa
    public static final String FLD_ambientTemp          = "ambientTemp";            // C
    public static final String FLD_cabinTemp            = "cabinTemp";              // C
    public static final DBField AtmosphereFieldInfo[] = {
        new DBField(FLD_barometer      , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.barometer"          , "Barometric Pressure"   ), "format=#0.00 units=pressure"),
        new DBField(FLD_ambientTemp    , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.ambientTemp"        , "Ambient Temperature"   ), "format=#0.0 units=temp"),
        new DBField(FLD_cabinTemp      , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.cabinTemp"          , "Cabin Temperature"     ), "format=#0.0 units=temp"),
    };

    // Temperature fields
    // startupInit.EventData.ThermoFieldInfo=true
    public static final String FLD_thermoAverage0       = "thermoAverage0";         // C
    public static final String FLD_thermoAverage1       = "thermoAverage1";         // C
    public static final String FLD_thermoAverage2       = "thermoAverage2";         // C
    public static final String FLD_thermoAverage3       = "thermoAverage3";         // C
    public static final String FLD_thermoAverage4       = "thermoAverage4";         // C
    public static final String FLD_thermoAverage5       = "thermoAverage5";         // C
    public static final String FLD_thermoAverage6       = "thermoAverage6";         // C
    public static final String FLD_thermoAverage7       = "thermoAverage7";         // C
    public static final DBField ThermoFieldInfo[] = {
        new DBField(FLD_thermoAverage0 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage0"     , "Temperature Average 0" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage1 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage1"     , "Temperature Average 1" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage2 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage2"     , "Temperature Average 2" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage3 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage3"     , "Temperature Average 3" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage4 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage4"     , "Temperature Average 4" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage5 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage5"     , "Temperature Average 5" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage6 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage6"     , "Temperature Average 6" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage7 , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage7"     , "Temperature Average 7" ), "format=#0.0 units=temp"),
    };

    // Analog fields
    // startupInit.EventData.AnalogFieldInfo=true
    public static final String FLD_analog0              = "analog0";                // 
    public static final String FLD_analog1              = "analog1";                // 
    public static final String FLD_analog2              = "analog2";                // 
    public static final String FLD_analog3              = "analog3";                // 
    public static final DBField AnalogFieldInfo[] = {
        new DBField(FLD_analog0        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog0"            , "Analog 0"              ), "format=#0.0"),
        new DBField(FLD_analog1        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog1"            , "Analog 1"              ), "format=#0.0"),
        new DBField(FLD_analog2        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog2"            , "Analog 2"              ), "format=#0.0"),
        new DBField(FLD_analog3        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog3"            , "Analog 3"              ), "format=#0.0"),
    };

    // Auto increment [
    // startupInit.EventData.AutoIncrementIndex=true
    //  - setting this to 'true' will require rebuilding the entire EventData table
    public static final String FLD_autoIndex            = DBRecordKey.FLD_autoIndex;
    public static final DBField AutoIncrementIndex[] = {
        new DBField(FLD_autoIndex      , Long.TYPE     , DBField.TYPE_INT64       , I18N.getString(EventData.class,"EventData.fld.autoIndex"          , "Auto Increment Index"  ), "key=true auto=true"),
    };

    // End-Of-Day summary (Antx)
    // startupInit.EventData.EndOfDaySummary=true
    public static final String FLD_dayEngineStarts      = "dayEngineStarts";    
    public static final String FLD_dayIdleHours         = "dayIdleHours";
    public static final String FLD_dayFuelIdle          = "dayFuelIdle";
    public static final String FLD_dayWorkHours         = "dayWorkHours";
    public static final String FLD_dayFuelWork          = "dayFuelWork";
    public static final String FLD_dayFuelPTO           = "dayFuelPTO";
    public static final String FLD_dayDistanceKM        = "dayDistanceKM";
    public static final String FLD_dayFuelTotal         = "dayFuelTotal";
    public static final DBField EndOfDaySummary[] = {
        new DBField(FLD_dayEngineStarts , Integer.TYPE , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.dayEngineStarts"    , "# Engine Starts"       ), ""),
        new DBField(FLD_dayIdleHours    , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayIdleHours"       , "Day Idle Hours"        ), "format=#0.0"),
        new DBField(FLD_dayFuelIdle     , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelIdle"        , "Day Idle Fuel"         ), "format=#0.0 units=volume"),
        new DBField(FLD_dayWorkHours    , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayWorkHours"       , "Day Work Hours"        ), "format=#0.0"),
        new DBField(FLD_dayFuelWork     , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelWork"        , "Day Work Fuel"         ), "format=#0.0 units=volume"),
        new DBField(FLD_dayFuelPTO      , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelPTO"         , "Day PTO Fuel"          ), "format=#0.0 units=volume"),
        new DBField(FLD_dayDistanceKM   , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayDistanceKM"      , "Day Distance KM"       ), "format=#0.0 units=distance"),
        new DBField(FLD_dayFuelTotal    , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelTotal"       , "Day Total Fuel"        ), "format=#0.0 units=volume"),
    };

    // GPRS PCell data (still under development)
    // startupInit.EventData.ServingCellTowerData=true
    public static final String FLD_cellTowerID          = "cellTowerID";        // Integer
    public static final String FLD_mobileCountryCode    = "mobileCountryCode";  // Integer
    public static final String FLD_mobileNetworkCode    = "mobileNetworkCode";  // Integer
    public static final String FLD_cellTimingAdvance    = "cellTimingAdvance";  // Integer
    public static final String FLD_locationAreaCode     = "locationAreaCode";   // Integer
    public static final String FLD_cellServingInfo      = "cellServingInfo";    // CellTower "-cid=123 -lac=1341 -arfcn=123 -rxlev=123"
    public static final String FLD_cellLatitude         = "cellLatitude";       // Double
    public static final String FLD_cellLongitude        = "cellLongitude";      // Double
    public static final String FLD_cellAccuracy         = "cellAccuracy";       // Double (meters)
    public static final DBField ServingCellTowerData[] = {
        new DBField(FLD_cellTowerID       , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.cellTowerID"        , "Cell Tower ID"         ), ""),
        new DBField(FLD_mobileCountryCode , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.mobileCountryCode"  , "Mobile Country Code"   ), ""),
        new DBField(FLD_mobileNetworkCode , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.mobileNetworkCode"  , "Mobile Network Code"   ), ""),
        new DBField(FLD_cellTimingAdvance , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.cellTimingAdvance"  , "Cell Timing Advance"   ), ""),
        new DBField(FLD_locationAreaCode  , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.locationAreaCode"   , "Location Area Code"    ), ""),
        new DBField(FLD_cellServingInfo   , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellServingInfo"    , "Serving Cell Info"     ), ""),
        new DBField(FLD_cellLatitude      , Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.cellLatitude"       , "Cell Tower Latitude"   ), "format=#0.00000"),
        new DBField(FLD_cellLongitude     , Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.cellLongitude"      , "Cell Tower Longitude"  ), "format=#0.00000"),
        new DBField(FLD_cellAccuracy      , Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.cellAccuracy"       , "Cell GPS Accuracy M"   ), "format=#0.0"),
    };
    // arfcn, tav, lac, cid
    // startupInit.EventData.NeighborCellTowerData=true
    public static final String FLD_cellNeighborInfo0    = "cellNeighborInfo0";  // CellTower
    public static final String FLD_cellNeighborInfo1    = "cellNeighborInfo1";  // CellTower
    public static final String FLD_cellNeighborInfo2    = "cellNeighborInfo2";  // CellTower
    public static final String FLD_cellNeighborInfo3    = "cellNeighborInfo3";  // CellTower
    public static final String FLD_cellNeighborInfo4    = "cellNeighborInfo4";  // CellTower
    public static final String FLD_cellNeighborInfo5    = "cellNeighborInfo5";  // CellTower
    public static final DBField NeighborCellTowerData[] = { 
        new DBField(FLD_cellNeighborInfo0 , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo0"  , "Neighbor Cell Info #0" ), ""),
        new DBField(FLD_cellNeighborInfo1 , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo1"  , "Neighbor Cell Info #1" ), ""),
        new DBField(FLD_cellNeighborInfo2 , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo2"  , "Neighbor Cell Info #2" ), ""),
        new DBField(FLD_cellNeighborInfo3 , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo3"  , "Neighbor Cell Info #3" ), ""),
        new DBField(FLD_cellNeighborInfo4 , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo4"  , "Neighbor Cell Info #4" ), ""),
        new DBField(FLD_cellNeighborInfo5 , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo5"  , "Neighbor Cell Info #5" ), ""),
    };
    private static final int COUNT_cellNeighborInfo = 6;

    // WorkZone Grid data (still under development)
    // startupInit.EventData.WorkZoneGridData=true
    public static final String FLD_sampleIndex          = "sampleIndex";            // #
    public static final String FLD_sampleID             = "sampleID";               // #
  //public static final String FLD_appliedPressure      = "appliedPressure";        // kPa
    public static final DBField WorkZoneGridData[] = {
        new DBField(FLD_sampleIndex       , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.sampleIndex"        , "Sample Index"          ), ""),
        new DBField(FLD_sampleID          , String.class , DBField.TYPE_STRING(32), I18N.getString(EventData.class,"EventData.fld.sampleID"           , "Sample ID"             ), ""),
      //new DBField(FLD_appliedPressure   , Double.TYPE  , DBField.TYPE_DOUBLE    , "Applied Pressure"          , "format=#0.00 units=pressure"),
    };

    // Keyed creation time with millisecond resolution  
    // startupInit.EventData.EventPushData=true
    // startupInit.EventData.CreationTimeMillisecond=true
    public static final String FLD_dataPush             = "dataPush";               //
    public static final DBField CreationTimeMillisecond[] = {
        newField_creationMillis(ALTKEY_eq_pushkey),
        new DBField(FLD_dataPush          , Boolean.TYPE , DBField.TYPE_BOOLEAN   , I18N.getString(EventData.class,"EventData.fld.dataPush"           , "Data Push Indicator"   ), ALTKEY_eq_pushkey),
    };

    /* key class */
    public static class Key
        extends DeviceKey<EventData>
    {
        public Key() {
            this.getFieldValues().setIgnoreInvalidFields(true);
        }
        public Key(String acctId, String devId, long timestamp, int statusCode) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_timestamp , timestamp);
            super.setFieldValue(FLD_statusCode, statusCode);
            this.getFieldValues().setIgnoreInvalidFields(true);
        }
        public Key(String acctId, String devId, long timestamp, int statusCode, String entity) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_timestamp , timestamp);
            super.setFieldValue(FLD_statusCode, statusCode);
            super.setFieldValue(FLD_entityID  , ((entity != null)? entity.toLowerCase() : ""));
            this.getFieldValues().setIgnoreInvalidFields(true);
        }
        public DBFactory<EventData> getFactory() {
            return EventData.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<EventData> factory = null;
    public static DBFactory<EventData> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                EventData.TABLE_NAME(),
                EventData.StandardFieldInfo,
                DBFactory.KeyType.PRIMARY,
                EventData.class, 
                EventData.Key.class,
                false/*editable*/,false/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
            factory.setLogMissingColumnWarnings(RTConfig.getBoolean(DBConfig.PROP_EventData_logMissingColumns,true));
            // FLD_address max length
            DBField addrFld = factory.getField(FLD_address);
            EventData.AddressColumnLength = (addrFld   != null)? addrFld.getStringLength()   : 0;
            // FLD_streetAddress max length
            DBField streetFld = factory.getField(FLD_streetAddress);
            EventData.StreetColumnLength  = (streetFld != null)? streetFld.getStringLength() : 0;
            // FLD_city max length
            DBField cityFld = factory.getField(FLD_city);
            EventData.CityColumnLength    = (cityFld   != null)? cityFld.getStringLength()   : 0;
            // FLD_stateProvince max length
            DBField stateFld = factory.getField(FLD_stateProvince);
            EventData.StateColumnLength   = (stateFld  != null)? stateFld.getStringLength()  : 0;
        }
        return factory;
    }

    /* Bean instance */
    public EventData()
    {
        super();
    }

    /* database record */
    public EventData(EventData.Key key)
    {
        super(key);
        // init?
    }

    // ------------------------------------------------------------------------

    /* copy specified EventData record with new statusCode */
    public static EventData copySynthesizedEvent(EventData evdb, int sc)
    {
        return EventData.copySynthesizedEvent(evdb, sc, 0L);
    }

    /* copy specified EventData record with new statusCode and timestamp */
    public static EventData copySynthesizedEvent(EventData evdb, int sc, long ts)
    {

        /* no event? */
        if ((evdb == null) || (sc <= 0)) { // omit STATUS_NONE, STATUS_IGNORE
            return null;
        }

        /* copy event */
        try {
            String acctID     = evdb.getAccountID();
            String devID      = evdb.getDeviceID();
            long   timestamp  = (ts > 0L)? ts : evdb.getTimestamp();
            int    statCode   = sc;
            EventData.Key evk = new EventData.Key(acctID, devID, timestamp, statCode);
            EventData copyEv  = evk.getDBRecord();
            copyEv.setAllFieldValues(evdb); // copy all non-key fields from original event
            copyEv.setSynthesizedEvent(true);
            return copyEv; // not yet saved
        } catch (DBException dbe) {
            Print.logException("Unable to copy EventData record", dbe);
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(EventData.class, loc);
        return i18n.getString("EventData.description", 
            "This table contains " +
            "events which have been generated by all client devices."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Common Bean access fields below
    
    // EventDataProvider interface. (defined in DeviceRecord.java)
    // public final String getDeviceVIN() { return super.getDeviceVIN(); }

    // ------------------------------------------------------------------------

    /**
    *** Gets the timestamp of this event in Unix/Epoch time
    *** @return The timestamp of this event
    **/
    public long getTimestamp()
    {
        return this.getFieldValue(FLD_timestamp, 0L);
    }

    /**
    *** Sets the timestamp of this event in Unix/Epoch time
    *** @param v The timestamp of this event
    **/
    public void setTimestamp(long v)
    {
        this.setFieldValue(FLD_timestamp, v);
    }

    // --------------------------

    /**
    *** Gets the String representation of the timestamp of this event
    *** @param timestamp  The timestamp
    *** @param account    The account
    *** @param bpl        The BasicPrivateLabel instance
    *** @return The String representation of the timestamp of this event
    **/
    public static String getTimestampString(long timestamp, Account account, BasicPrivateLabel bpl)
    {
        Account a      = account;
        String dateFmt = (a != null)? a.getDateFormat()   : ((bpl != null)? bpl.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat());
        String timeFmt = (a != null)? a.getTimeFormat()   : ((bpl != null)? bpl.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat());
        TimeZone tmz   = (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        //return dt.gmtFormat(dateFmt + " " + timeFmt + " z");
        return dt.format(dateFmt + " " + timeFmt + " z");
    }

    /**
    *** Gets the String representation of the timestamp time-of-day of this event
    *** @param timestamp  The timestamp
    *** @param account    The account
    *** @param bpl        The BasicPrivateLabel instance
    *** @return The String representation of the timestamp time-of-day of this event
    **/
    public static String getTimestampTime(long timestamp, Account account, BasicPrivateLabel bpl)
    {
        Account a      = account;
        String timeFmt = (a != null)? a.getTimeFormat()   : ((bpl != null)? bpl.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat());
        TimeZone tmz   = (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return dt.format(timeFmt);
    }

    /**
    *** Gets the String representation of the timestamp year of this event
    *** @param timestamp  The timestamp
    *** @param account    The account
    *** @return The String representation of the timestamp year of this event
    **/
    public static String getTimestampYear(long timestamp, Account account)
    {
        Account a      = account;
        TimeZone tmz   = (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return String.valueOf(dt.getYear());
    }

    /**
    *** Gets the String representation of the timestamp month of this event
    *** @param timestamp  The timestamp
    *** @param abbrev     True to return the month abbreviation, false to return the full month name
    *** @param account    The account
    *** @param locale     The locale
    *** @return The String representation of the timestamp month of this event
    **/
    public static String getTimestampMonth(long timestamp, boolean abbrev, Account account, Locale locale)
    {
        Account a      = account;
        TimeZone tmz   = (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return DateTime.getMonthName(dt.getMonth1(), abbrev);
    }

    /**
    *** Gets the String representation of the timestamp day-of-month of this event
    *** @param timestamp  The timestamp
    *** @param account    The account
    *** @return The String representation of the timestamp day-of-month of this event
    **/
    public static String getTimestampDayOfMonth(long timestamp, Account account)
    {
        Account a      = account;
        TimeZone tmz   = (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return String.valueOf(dt.getDayOfMonth());
    }

    /**
    *** Gets the String representation of the timestamp day-of-week of this event
    *** @param timestamp  The timestamp
    *** @param abbrev     True to return the day abbreviation, false to return the full day name
    *** @param account    The account
    *** @param locale     The locale
    *** @return The String representation of the timestamp day-of-week of this event
    **/
    public static String getTimestampDayOfWeek(long timestamp, boolean abbrev, Account account, Locale locale)
    {
        Account a      = account;
        TimeZone tmz   = (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return DateTime.getDayName(dt.getDayOfWeek(), abbrev);
    }

    // --------------------------

    /**
    *** Gets the String representation of the timestamp of this event
    *** @param bpl     The BasicPrivateLabel instance
    *** @return The String representation of the timestamp of this event
    **/
    public String getTimestampString(BasicPrivateLabel bpl)
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        return EventData.getTimestampString(ts, acct, bpl);
    }

    /**
    *** Gets the String representation of the timestamp of this event
    *** @return The String representation of the timestamp of this event
    **/
    public String getTimestampString()
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        return EventData.getTimestampString(ts, acct, null);
    }

    /**
    *** Gets the String representation of the timestamp time-of-day of this event
    *** @return The String representation of the timestamp time-of-day of this event
    **/
    public String getTimestampTime()
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        //return EventData.getTimestampString(ts, acct, null);
        return EventData.getTimestampTime(ts, acct, null);
    }

    /**
    *** Gets the String representation of the timestamp year of this event
    *** @return The String representation of the timestamp year of this event
    **/
    public String getTimestampYear()
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        return EventData.getTimestampYear(ts, acct);
    }

    /**
    *** Gets the String representation of the timestamp month of this event
    *** @param abbrev  True to return the month abbreviation, false to return the full month name
    *** @param locale  The locale
    *** @return The String representation of the timestamp month of this event
    **/
    public String getTimestampMonth(boolean abbrev, Locale locale)
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        return EventData.getTimestampMonth(ts, abbrev, acct, locale);
    }

    /**
    *** Gets the String representation of the timestamp day-of-month of this event
    *** @return The String representation of the timestamp day-of-month of this event
    **/
    public String getTimestampDayOfMonth()
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        return EventData.getTimestampDayOfMonth(ts, acct);
    }

    /**
    *** Gets the String representation of the timestamp day-of-week of this event
    *** @param abbrev  True to return the day abbreviation, false to return the full day name
    *** @param locale  The locale
    *** @return The String representation of the timestamp day-of-week of this event
    **/
    public String getTimestampDayOfWeek(boolean abbrev, Locale locale)
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        return EventData.getTimestampDayOfWeek(ts, abbrev, acct, locale);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the status code of this event
    *** @return The status code of this event
    **/
    public int getStatusCode()
    {
        return this.getFieldValue(FLD_statusCode, 0);
    }

    /**
    *** Gets the String representation of the status code foregound color<br>
    *** (may return null if this event status code is not pre-defined).
    *** @return The String representation of the status code foregound color
    **/
    public StatusCodeProvider getStatusCodeProvider(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getStatusCodeProvider(dev, code, bpl, null/*dftSCP*/);
    }

    /**
    *** Gets the String representation of the status code foregound color
    *** @return The String representation of the status code foregound color
    **/
    public String getStatusCodeForegroundColor(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getForegroundColor(dev, code, bpl, null/*dftColor*/);
    }

    /**
    *** Gets the String representation of the status code backgound color
    *** @return The String representation of the status code backgound color
    **/
    public String getStatusCodeBackgroundColor(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getBackgroundColor(dev, code, bpl, null/*dftColor*/);
    }

    /**
    *** Gets the Hex String representation of the status code of this event
    *** @return The Hex String representation of the status code of this event
    **/
    public String getStatusCodeHex()
    {
        return StatusCodes.GetHex(this.getStatusCode());
    }

    /**
    *** Gets the String representation of the status code of this event
    *** @return The String representation of the status code of this event
    **/
    public String getStatusCodeDescription(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getDescription(dev, code, bpl, null/*dftDesc*/);
    }

    /**
    *** Gets the map icon-selector for the status code of this event
    *** @return The map icon-selector for the status code of this event
    **/
    public String getStatusCodeIconSelector(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getIconSelector(dev, code, bpl);
    }

    /**
    *** Gets the icon-name for the status code of this event
    *** @param bpl  The domain BasicPrivateLabel
    *** @return The icon-name for the status code of this event
    **/
    public String getStatusCodeIconName(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getIconName(dev, code, bpl);
    }

    /**
    *** Sets the status code of this event
    *** @param v The status code of this event
    **/
    public void setStatusCode(int v)
    {
        this.setFieldValue(FLD_statusCode, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the data source for this event.  The data source is an optional field defined by the 
    *** remote client tracking device.  
    *** @return The event data source
    **/
    public String getDataSource()
    {
        return this.getFieldValue(FLD_dataSource, "");
    }
    
    /**
    *** Sets the data source for this event.
    *** @param v  The data source
    **/
    public void setDataSource(String v)
    {
        this.setFieldValue(FLD_dataSource, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the transport-id for this event.  This is the 'transportID' from the Transport 
    *** record used to identify this Device.
    *** @return The transport-id used to identify this device.
    **/
    public String getTransportID()
    {
        return this.getFieldValue(FLD_transportID, "");
    }
    
    /**
    *** Sets the transport-id for this event.
    *** @param v  The transport-id used to identify this device.
    **/
    public void setTransportID(String v)
    {
        this.setFieldValue(FLD_transportID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getRawData()
    {
        return this.getFieldValue(FLD_rawData, "");
    }
    
    public void setRawData(String v)
    {
        this.setFieldValue(FLD_rawData, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the best Latitude for this event
    *** @return The best Latitude for this event
    **/
    public double getBestLatitude()
    {
        if (this.isValidGeoPoint()) {
            return this.getLatitude();
        } else {
            return this.getCellLatitude();
        }
    }

    /**
    *** Gets the best Longitude for this event
    *** @return The best Longitude for this event
    **/
    public double getBestLongitude()
    {
        if (this.isValidGeoPoint()) {
            return this.getLongitude();
        } else {
            return this.getCellLongitude();
        }
    }

    /**
    *** Gets the best GeoPoint for this event
    *** (does not return null)
    *** @return The best GeoPoint for this event
    **/
    public GeoPoint getBestGeoPoint()
    {
        if (this.isValidGeoPoint()) {
            return this.getGeoPoint();
        } else {
            return this.getCellGeoPoint();
        }
    }
    
    /**
    *** Gets the accuracy radius, in meters
    *** @return The Accuracy radius, in meters
    **/
    public double getBestAccuracy()
    {
        if (this.isValidGeoPoint()) {
            return this.getHorzAccuracy();
        } else {
            return this.getCellAccuracy();
        }
    }

    // ------------------------------------------------------------------------

    // GeoPoint optimization
    private GeoPoint geoPoint = null;
    
    /**
    *** Gets the GeoPoint for this event
    *** @return The GeoPoint for this event
    **/
    public GeoPoint getGeoPoint()
    {
        if (this.geoPoint == null) {
            if (this.isValidGeoPoint()) {
                this.geoPoint = new GeoPoint(this.getLatitude(), this.getLongitude());
            } else {
                this.geoPoint = GeoPoint.INVALID_GEOPOINT;
            }
        }
        return this.geoPoint;
    }

    /**
    *** Sets the latitude/longitude for this event
    *** @param lat The latitude
    *** @param lat The longitude
    **/
    public void setGeoPoint(double lat, double lng)
    {
        this.setLatitude(lat);
        this.setLongitude(lng);
    }

    /**
    *** Sets the latitude/longitude for this event instance
    *** @param gp The latitude/longitude
    **/
    public void setGeoPoint(GeoPoint gp)
    {
        if (gp == null) {
            // assume called expected 0/0
            this.setLatitude(0.0);
            this.setLongitude(0.0);
        } else
        if (!gp.isValid()) {
            if (!GeoPoint.isOrigin(gp)) {
                // not at origin, display invalid point
                Print.logInfo("GeoPoint is invalid: " + gp);
            }
            this.setLatitude(0.0);
            this.setLongitude(0.0);
        } else {
            // set point
            this.setLatitude(gp.getLatitude());
            this.setLongitude(gp.getLongitude());
        }
    }
    
    /** 
    *** Returns true if the GeoPoint represented by this event is valid
    *** @return True if the GeoPoint represented by this event is valid
    **/
    public boolean isValidGeoPoint()
    {
        return GeoPoint.isValid(this.getLatitude(), this.getLongitude());
    }

    // ------------------------------------------------------------------------

    public double getLatitude()
    {
        return this.getFieldValue(FLD_latitude, 0.0);
    }
    
    public void setLatitude(double v)
    {
        this.setFieldValue(FLD_latitude, v);
        this.geoPoint = null;
    }

    // ------------------------------------------------------------------------

    public double getLongitude()
    {
        return this.getFieldValue(FLD_longitude, 0.0);
    }
    
    public void setLongitude(double v)
    {
        this.setFieldValue(FLD_longitude, v);
        this.geoPoint = null;
    }
    
    // ------------------------------------------------------------------------

    public long getGpsAge()
    {
        return this.getFieldValue(FLD_gpsAge, 0L);
    }
    
    public void setGpsAge(long v)
    {
        this.setFieldValue(FLD_gpsAge, ((v >= 0L)? v : 0L));
    }

    // ------------------------------------------------------------------------

    public double getSpeedKPH()
    {
        return this.getFieldValue(FLD_speedKPH, 0.0);
    }
    
    public void setSpeedKPH(double v)
    {
        this.setFieldValue(FLD_speedKPH, v);
    }

    public double getSpeedMPH()
    {
        return this.getSpeedKPH() * GeoPoint.MILES_PER_KILOMETER;
    }

    // ------------------------------------------------------------------------

    public double getHeading()
    {
        return this.getFieldValue(FLD_heading, 0.0);
    }

    public void setHeading(double v)
    {
        this.setFieldValue(FLD_heading, v);
    }
    
    // ------------------------------------------------------------------------

    public double getAltitude() // meters
    {
        return this.getFieldValue(FLD_altitude, 0.0);
    }

    public String getAltitudeString(boolean inclUnits, Locale locale)
    {
        I18N i18n = I18N.getI18N(EventData.class, locale);
        double alt = this.getAltitude(); // meters
        String distUnitsStr = "?";
        if (Account.getDistanceUnits(this.getAccount()).isMiles()) {
            alt *= GeoPoint.FEET_PER_METER; // convert to feet
            distUnitsStr = i18n.getString("EventData.units.feet", "feet");
        } else {
            distUnitsStr = i18n.getString("EventData.units.meters", "meters");
        }
        String altStr = StringTools.format(alt,"0");
        return inclUnits? (altStr + " " + distUnitsStr) : altStr;
    }

    public void setAltitude(double v) // meters
    {
        this.setFieldValue(FLD_altitude, v);
    }

    // ------------------------------------------------------------------------

    public double getDistanceKM()
    {
        return this.getFieldValue(FLD_distanceKM, 0.0);
    }

    public void setDistanceKM(double v)
    {
        this.setFieldValue(FLD_distanceKM, v);
    }

    // ------------------------------------------------------------------------
    
    private boolean isActualOdometer = false; // is actual vehicle odometer

    public double getOdometerKM()
    {
        return this.getFieldValue(FLD_odometerKM, 0.0);
    }

    public void setOdometerKM(double v)
    {
        this.setFieldValue(FLD_odometerKM, v);
    }

    public void setOdometerKM(double v, boolean actualOdom)
    {
        this.setFieldValue(FLD_odometerKM, v);
        this.isActualOdometer = actualOdom;
    }

    public boolean isActualOdometer()
    {
        return this.isActualOdometer;
    }

    // ------------------------------------------------------------------------

    public long getGeozoneIndex()
    {
        return this.getFieldValue(FLD_geozoneIndex, 0L);
    }

    public void setGeozoneIndex(long v)
    {
        this.setFieldValue(FLD_geozoneIndex, v);
    }

    // ------------------------------------------------------------------------

    public String getGeozoneID()
    {
        return this.getFieldValue(FLD_geozoneID, "");
    }

    public void setGeozoneID(String v)
    {
        this.setFieldValue(FLD_geozoneID, StringTools.trim(v));
    }

    private Geozone geozone = null;  // Geozone cache optimization
    public void setGeozone(Geozone zone)
    {
        this.geozone = zone;
        this.setGeozoneID((zone != null)? zone.getGeozoneID() : "");
    }

    public boolean hasGeozone()
    {
        return (this.geozone != null);
    }

    public Geozone getGeozone()
    {
        if (this.geozone == null) {
            String gid = this.getGeozoneID();
            if (!StringTools.isBlank(gid)) {
                try {
                    Geozone gz[] = Geozone.getGeozone(this.getAccount(), gid);
                    this.geozone = !ListTools.isEmpty(gz)? gz[0] : null;
                } catch (DBException dbe) {
                    this.geozone = null;
                }
            }
        } 
        return this.geozone;
    }

    /* get geozone description */
    public String getGeozoneDescription()
    {
        Geozone zone = this.getGeozone();
        return (zone != null)? zone.getDescription() : "";
    }

    // ------------------------------------------------------------------------

    public String getEntityID()
    {
        return this.getFieldValue(FLD_entityID, "");
    }
    
    public void setEntityID(String v)
    {
        this.setFieldValue(FLD_entityID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    
    private static final EntityManager.EntityType DEFAULT_ENTITY_TYPE = EntityManager.EntityType.TRAILER;
    
    private int entityTypeInt = DEFAULT_ENTITY_TYPE.getIntValue();

    public int getEntityType()
    {
        return this.entityTypeInt;
    }

    public void setEntityType(int v)
    {
        this.entityTypeInt = v;
    }

    public EntityManager.EntityType _getEntityType()
    {
        // does not return null
        int type = this.getEntityType();
        return EnumTools.getValueOf(EntityManager.EntityType.class, type, DEFAULT_ENTITY_TYPE);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the GPS fix type
    *** @return The GPS fix type
    **/
    public int getGpsFixType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_gpsFixType);
        return (v != null)? v.intValue() : EnumTools.getDefault(GPSFixType.class).getIntValue();
    }

    /**
    *** Sets the GPS fix type
    *** @param v The GPS fix type
    **/
    public void setGpsFixType(int v)
    {
        this.setFieldValue(FLD_gpsFixType, EnumTools.getValueOf(GPSFixType.class,v).getIntValue());
    }

    /**
    *** Sets the GPS fix type
    *** @param v The GPS fix type
    **/
    public void setGpsFixType(GPSFixType v)
    {
        this.setFieldValue(FLD_gpsFixType, EnumTools.getValueOf(GPSFixType.class,v).getIntValue());
    }

    /**
    *** Sets the GPS fix type
    *** @param v The GPS fix type
    **/
    public void setGpsFixType(String v, Locale locale)
    {
        this.setFieldValue(FLD_gpsFixType, EnumTools.getValueOf(GPSFixType.class,v,locale).getIntValue());
    }

    public String getGpsFixTypeDescription(Locale loc)
    {
        return EventData.getGPSFixType(this).toString(loc);
    }

    // ------------------------------------------------------------------------

    /* horizontal accuracy (meters) */
    public double getHorzAccuracy()
    {
        return this.getFieldValue(FLD_horzAccuracy, 0.0);
    }
    
    public void setHorzAccuracy(double v)
    {
        this.setFieldValue(FLD_horzAccuracy, v);
    }

    // ------------------------------------------------------------------------

    /* vertical accuracy (meters) */
    public double getVertAccuracy()
    {
        return this.getFieldValue(FLD_vertAccuracy, 0.0);
    }
    
    public void setVertAccuracy(double v)
    {
        this.setFieldValue(FLD_vertAccuracy, v);
    }

    // ------------------------------------------------------------------------

    public double getHDOP()
    {
        return this.getFieldValue(FLD_HDOP, 0.0);
    }
    
    public void setHDOP(double v)
    {
        this.setFieldValue(FLD_HDOP, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Satellite count
    *** @return The Satellite count
    **/
    public int getSatelliteCount()
    {
        return this.getFieldValue(FLD_satelliteCount, 0);
    }

    /**
    *** Sets the Satellite count
    *** @param v The Satellite count
    **/
    public void setSatelliteCount(int v)
    {
        this.setFieldValue(FLD_satelliteCount, ((v < 0)? 0 : v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current battery level
    *** @return The current battery level
    **/
    public double getBatteryLevel()
    {
        // Note: Battery Voltage can be converted to Battery Level using the following example algorithm:
        //   double BATTERY_VOLTS_MAX;
        //   double BATTERY_VOLTS_MIN;
        //   double currentBattVolts;
        //   double battLevelPercent;
        //   battLevelPercent = (currentBattVolts - BATTERY_VOLTS_MIN) / (BATTERY_VOLTS_MAX - BATTERY_VOLTS_MIN);
        //   if (battLevelPercent > 1.0) { battLevelPercent = 1.0; }
        //   if (battLevelPercent < 0.0) { battLevelPercent = 0.0; }
        return this.getFieldValue(FLD_batteryLevel, 0.0);
    }
    
    /**
    *** Sets the current battery level
    *** @param v The current battery level
    **/
    public void setBatteryLevel(double v)
    {
        this.setFieldValue(FLD_batteryLevel, v);
    }

    // ------------------------------------------------------------------------


    /**
    *** Gets the current battery voltage
    *** @return The current battery voltage
    **/
    public double getBatteryVolts()
    {
        return this.getFieldValue(FLD_batteryVolts, 0.0);
    }
    
    /**
    *** Sets the current battery voltage
    *** @param v The current battery voltage
    **/
    public void setBatteryVolts(double v)
    {
        this.setFieldValue(FLD_batteryVolts, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current signal strength
    *** @return The current signal strength
    **/
    public double getSignalStrength()
    {
        return this.getFieldValue(FLD_signalStrength, 0.0);
    }
    
    /**
    *** Sets the current signal strength
    *** @param v The current signal strength
    **/
    public void setSignalStrength(double v)
    {
        this.setFieldValue(FLD_signalStrength, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if an address has been defined
    *** @return True if an address has been defined
    **/
    public boolean hasAddress()
    {
        return !this.getAddress().equals("");
    }

    public String getAddress()
    {
        String v = (String)this.getFieldValue(FLD_address);
        return StringTools.trim(v);
    }

    public String getAddress(boolean lazyUpdate)
    {
        String v = this.getAddress();
        if (lazyUpdate && v.equals("")) {
            try {
                Set<String> updFlds = this.updateAddress(true/*fastOnly*/, false/*force*/);
                if (!ListTools.isEmpty(updFlds)) {
                    this.update(updFlds);
                }
            } catch (SlowOperationException soe) {
                // will occur if reverse-geocoder is not a fast operation
                // leave 'v' as-is
            } catch (DBException dbe) {
                dbe.printException();
            }
            v = this.getAddress();
        }
        return v;
    }

    public void setAddress(String v)
    {
        String addr = StringTools.trim(v);
        if ((EventData.AddressColumnLength > 0)             &&
            (addr.length() >= EventData.AddressColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.AddressColumnLength - 1; 
            addr = addr.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_address, addr);
    }

    // ------------------------------------------------------------------------

    /**
    *** Reload all reverse-geocoded address fields
    **/
    public void reloadAddress()
    {
        this.reload(
            FLD_address,
            FLD_streetAddress,
            FLD_city,
            FLD_stateProvince,
            FLD_postalCode,
            FLD_country,
            FLD_subdivision,
            FLD_speedLimitKPH,
            FLD_isTollRoad
            );
    }
    
    // ------------------------------------------------------------------------

    public Set<String> updateAddress(boolean fastOnly)
        throws SlowOperationException
    {
        return this.updateAddress(fastOnly, false/*force*/);
    }

    public Set<String> updateAddress(boolean fastOnly, boolean force)
        throws SlowOperationException
    {
        // If the caller does not want to wait for a time-consuming operation, specifying 
        // 'fastOnly==true' will cause this method to throw a 'SlowOperationException' if 
        // it determines that the reverse-geocoding will take too long.  The reason that 
        // reverse-geocoding  might take a while is because it might be using an outside 
        // service (ie. linking to a remote web-based service) to perform it's function.
        // (SlowOperationException is not thrown if 'fastOnly' is false.)
        
        /* already have an address? */
        if (!force && this.hasAddress()) {
            // we already have an address 
            // (and 'force' did not indicate we should update the address)
            return null;
        }

        /* invalid GeoPoint? */
        if (!this.isValidGeoPoint()) {
            // Can't reverse-geocode an invalid point
            return null;
        }

        /* get Account */
        Account acct = this.getAccount();
        if (acct == null) {
            // no account, not reverse-geocoding
            return null;
        }

        /* get geocoder mode */
        Account.GeocoderMode geocoderMode = Account.getGeocoderMode(acct);
        if (geocoderMode.isNone()) {
            // no geocoding is performed for this account
            return null;
        }

        /* set "Departed" geozone description for STATUS_GEOFENCE_DEPART events */
        int statusCode = this.getStatusCode();
        if (statusCode == StatusCodes.STATUS_GEOFENCE_DEPART) {
            // On departure events, get the departed Geozone description
            Geozone gz[] = null;
            // first try clientID
            if ((gz == null) || (gz.length == 0)) {
                long clientId = this.getGeozoneIndex(); // ClientID of departed geozone
                if (clientId > 0L) {
                    gz = Geozone.getClientIDZones(acct.getAccountID(), clientId);
                }
            }
            // next try geozoneID
            if ((gz == null) || (gz.length == 0)) {
                String geozoneID = this.getGeozoneID();
                if (!StringTools.isBlank(geozoneID)) {
                    try {
                        gz = Geozone.getGeozone(acct, geozoneID);
                    } catch (DBException dbe) {
                        Print.logException("Error reading Geozone: " + acct.getAccountID() + "/" + geozoneID, dbe);
                    }
                }
            }
            // update and return if we found the geozone
            if ((gz != null) && (gz.length > 0) && gz[0].isReverseGeocode()) {
                Set<String> updFields = new HashSet<String>();
                if (gz[0].isClientUpload()) {
                    this.setGeozoneIndex(gz[0].getClientID());      // FLD_geozoneIndex
                    updFields.add(EventData.FLD_geozoneIndex);
                }
                this.setGeozoneID(gz[0].getGeozoneID());            // FLD_geozoneID
                updFields.add(EventData.FLD_geozoneID);
                this.setAddress(gz[0].getDescription());            // FLD_address
                updFields.add(EventData.FLD_address);
                this.setStreetAddress(gz[0].getStreetAddress());    // FLD_streetAddress
                updFields.add(EventData.FLD_streetAddress);
                this.setCity(gz[0].getCity());                      // FLD_city
                updFields.add(EventData.FLD_city);
                this.setStateProvince(gz[0].getStateProvince());    // FLD_stateProvince
                updFields.add(EventData.FLD_stateProvince);
                this.setPostalCode(gz[0].getPostalCode());          // FLD_postalCode
                updFields.add(EventData.FLD_postalCode);
                this.setCountry(gz[0].getCountry());                // FLD_country
                updFields.add(EventData.FLD_country);
                this.setSubdivision(gz[0].getSubdivision());        // FLD_subdivision
                updFields.add(EventData.FLD_subdivision);
                return updFields;
            }
        } else
        if (statusCode == StatusCodes.STATUS_GEOFENCE_ARRIVE) {
            // On arrival events, get the arrival Geozone description
            // (due to rounding error, the server may think we are not yet within a zone)
            Geozone gz[] = null;
            // first try clientID
            if ((gz == null) || (gz.length == 0)) {
                long clientId = this.getGeozoneIndex(); // ClientID of arrival geozone
                if (clientId > 0L) {
                    gz = Geozone.getClientIDZones(acct.getAccountID(), clientId);
                }
            }
            // next try geozoneID
            if ((gz == null) || (gz.length == 0)) {
                String geozoneID = this.getGeozoneID();
                if (!StringTools.isBlank(geozoneID)) {
                    try {
                        gz = Geozone.getGeozone(acct, geozoneID);
                    } catch (DBException dbe) {
                        Print.logException("Error reading Geozone: " + acct.getAccountID() + "/" + geozoneID, dbe);
                    }
                }
            }
            // update and return if we found the geozone
            if ((gz != null) && (gz.length > 0) && gz[0].isReverseGeocode()) {
                Set<String> updFields = new HashSet<String>();
                if (gz[0].isClientUpload()) {
                    this.setGeozoneIndex(gz[0].getClientID());      // FLD_geozoneIndex
                    updFields.add(EventData.FLD_geozoneIndex);
                }
                this.setGeozoneID(gz[0].getGeozoneID());            // FLD_geozoneID
                updFields.add(EventData.FLD_geozoneID);
                this.setAddress(gz[0].getDescription());            // FLD_address
                updFields.add(EventData.FLD_address);
                this.setStreetAddress(gz[0].getStreetAddress());    // FLD_streetAddress
                updFields.add(EventData.FLD_streetAddress);
                this.setCity(gz[0].getCity());                      // FLD_city
                updFields.add(EventData.FLD_city);
                this.setStateProvince(gz[0].getStateProvince());    // FLD_stateProvince
                updFields.add(EventData.FLD_stateProvince);
                this.setPostalCode(gz[0].getPostalCode());          // FLD_postalCode
                updFields.add(EventData.FLD_postalCode);
                this.setCountry(gz[0].getCountry());                // FLD_country
                updFields.add(EventData.FLD_country);
                this.setSubdivision(gz[0].getSubdivision());        // FLD_subdivision
                updFields.add(EventData.FLD_subdivision);
                return updFields;
            }
        }

        /* (at least GeocoderMode.GEOZONE) get address from Geozone */
        GeoPoint gp = this.getGeoPoint();
        Geozone gzone = Geozone.getGeozone(acct.getAccountID(), null, gp, true); // <Geozone>.getReverseGeocode() == true
        if (gzone != null) {
            Set<String> updFields = new HashSet<String>();
            Print.logInfo("Found Geozone : " + gzone.getGeozoneID() + " - " + gzone.getDescription());
            if (gzone.isClientUpload() && (this.getGeozoneIndex() <= 0L)) {
                this.setGeozoneIndex(gzone.getClientID());              // FLD_geozoneIndex
                updFields.add(EventData.FLD_geozoneIndex);
            }
            this.setGeozoneID(gzone.getGeozoneID());                    // FLD_geozoneID
            updFields.add(EventData.FLD_geozoneID);
            this.setAddress(gzone.getDescription());                    // FLD_address
            updFields.add(EventData.FLD_address);
            this.setStreetAddress(gzone.getStreetAddress());            // FLD_streetAddress
            updFields.add(EventData.FLD_streetAddress);
            this.setCity(gzone.getCity());                              // FLD_city
            updFields.add(EventData.FLD_city);
            this.setStateProvince(gzone.getStateProvince());            // FLD_stateProvince
            updFields.add(EventData.FLD_stateProvince);
            this.setPostalCode(gzone.getPostalCode());                  // FLD_postalCode
            updFields.add(EventData.FLD_postalCode);
            this.setCountry(gzone.getCountry());                        // FLD_country
            updFields.add(EventData.FLD_country);
            this.setSubdivision(gzone.getSubdivision());                // FLD_subdivision
            updFields.add(EventData.FLD_subdivision);
            return updFields;
        }

        /* reverse-geocoding iff FULL, or PARTIAL with high-priority status code */
        BasicPrivateLabel privLabel = acct.getPrivateLabel();
        if (!geocoderMode.okFull() && !StatusCodes.IsHighPriority(statusCode,privLabel)) {
            // PARTIAL reverse-geocoding requested and this is not a high-pri status code
            Print.logDebug("Skipping reverse-geocode per Account geocoderMode: " + acct.getAccountID());
            return null;
        }

        /* get reverse-geocoder */
        ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
        if (rgp == null) {
            // no ReverseGeocodeProvider, no reverse-geocoding
            String acctID = this.getAccountID();
            if (acct.hasPrivateLabel()) {
                Print.logInfo("[Account '%s'] PrivateLabel '%s' does not define a ReverseGeocodeProvider", acctID, privLabel); 
            } else {
                Print.logInfo("No PrivateLabel (thus no ReverseGeocodeProvider) for Account '%s'", acctID); 
            }
            return null;
        } else
        if (!rgp.isEnabled()) {
            Print.logInfo("ReverseGeocodeProvider disabled: " + rgp.getName());
            return null;
        }

        /* fast operations only? */
        if (fastOnly && !rgp.isFastOperation()) {
            // We've requested a fast operation only, and this operation is slow.
            // It's up to the caller to see that this operation is queued in a background thread.
            throw new SlowOperationException("'fast' requested, and this operation is 'slow'");
        }

        /* finally, get the address for this point */
        ReverseGeocode rg = null;
        try {
            // make sure the Domain properties are available to RTConfig
            privLabel.pushRTProperties();   // stack properties (may be redundant in servlet environment)
            privLabel.getLocaleString();
            rg = rgp.getReverseGeocode(gp, privLabel.getLocaleString()); // get the reverse-geocode
        } catch (Throwable th) {
            // ignore
        } finally {
            privLabel.popRTProperties();    // remove from stack
        }
        if (rg != null) {
            Set<String> updFields = new HashSet<String>();
            if (rg.hasFullAddress()) {
                this.setAddress(rg.getFullAddress());                   // FLD_address
                updFields.add(EventData.FLD_address);
            }
            if (rg.hasStreetAddress()) {
                this.setStreetAddress(rg.getStreetAddress());           // FLD_streetAddress
                updFields.add(EventData.FLD_streetAddress);
            }
            if (rg.hasCity()) {
                this.setCity(rg.getCity());                             // FLD_city
                updFields.add(EventData.FLD_city);
            }
            if (rg.hasStateProvince()) {
                this.setStateProvince(rg.getStateProvince());           // FLD_stateProvince
                updFields.add(EventData.FLD_stateProvince);
            }
            if (rg.hasPostalCode()) {
                this.setPostalCode(rg.getPostalCode());                 // FLD_postalCode
                updFields.add(EventData.FLD_postalCode);
            }
            if (rg.hasCountryCode()) {
                this.setCountry(rg.getCountryCode());                   // FLD_country
                updFields.add(EventData.FLD_country);
            }
            if (rg.hasSubdivision()) {
                this.setSubdivision(rg.getSubdivision());               // FLD_subdivision
                updFields.add(EventData.FLD_subdivision);
            }
            if (rg.hasSpeedLimitKPH()) {
                this.setSpeedLimitKPH(rg.getSpeedLimitKPH());           // FLD_speedLimitKPH
                updFields.add(EventData.FLD_speedLimitKPH);
            }
            if (rg.hasIsTollRoad()) {
                this.setIsTollRoad(rg.getIsTollRoad());                 // FLD_isTollRoad
                updFields.add(EventData.FLD_isTollRoad);
            }
            return !updFields.isEmpty()? updFields : null;
        }

        /* still no address after all of this */
        Print.logInfo("No RG Address found ["+rgp.getName()+"]: " + gp);
        return null;
        
    }

    // ------------------------------------------------------------------------

    public String getStreetAddress()
    {
        String v = (String)this.getFieldValue(FLD_streetAddress);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setStreetAddress(String v)
    {
        String street = StringTools.trim(v);
        if ((EventData.StreetColumnLength > 0)              &&
            (street.length() >= EventData.StreetColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.StreetColumnLength - 1; 
            street = street.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_streetAddress, street);
    }

    // ------------------------------------------------------------------------

    public String getCity()
    {
        String v = (String)this.getFieldValue(FLD_city);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }

    public void setCity(String v)
    {
        String city = StringTools.trim(v);
        if ((EventData.CityColumnLength > 0)              &&
            (city.length() >= EventData.CityColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.CityColumnLength - 1; 
            city = city.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_city, city);
    }

    // ------------------------------------------------------------------------

    public String getStateProvince()
    {
        String v = (String)this.getFieldValue(FLD_stateProvince);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setStateProvince(String v)
    {
        String state = StringTools.trim(v); 
        if ((EventData.StateColumnLength > 0)              &&
            (state.length() >= EventData.StateColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.StateColumnLength - 1; 
            state = state.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_stateProvince, state);
    }

    // ------------------------------------------------------------------------

    public String getPostalCode()
    {
        String v = (String)this.getFieldValue(FLD_postalCode);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setPostalCode(String v)
    {
        this.setFieldValue(FLD_postalCode, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getCountry()
    {
        String v = (String)this.getFieldValue(FLD_country);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setCountry(String v)
    {
        this.setFieldValue(FLD_country, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return subdivision */
    public String getSubdivision()
    {
        String v = (String)this.getFieldValue(FLD_subdivision);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    /* set subdivision */
    public void setSubdivision(String v)
    {
        this.setFieldValue(FLD_subdivision, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

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

    public boolean getIsTollRoad()
    {
        return this.getFieldValue(FLD_isTollRoad, false);
    }

    public void setIsTollRoad(boolean v)
    {
        this.setFieldValue(FLD_isTollRoad, v);
    }

    public boolean isTollRoad()
    {
        return this.getIsTollRoad();
    }

    // ------------------------------------------------------------------------

    /* get digital input mask */
    public long getInputMask()
    {
        Long v = (Long)this.getFieldValue(FLD_inputMask);
        return (v != null)? v.intValue() : 0L;
    }
    
    /* return state of input mask bit */
    public boolean getInputMaskBitState(int bit)
    {
        long m = this.getInputMask();
        return (((1L << bit) & m) != 0L);
    }
    
    /* set digital input mask */
    public void setInputMask(long v)
    {
        if (v < 0L) {
            // FLD_inputMask is unsigned
            this.setFieldValue(FLD_inputMask, 0L);
        } else {
            // FLD_inputMask is currently 32-bit max 
            this.setFieldValue(FLD_inputMask, (v & 0xFFFFFFFFL)); // 32-bit
        }
    }

    // Common Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Temerature Bean access fields below

    public double getBarometer()
    {
        return this.getFieldValue(FLD_barometer, 0.0); // kPa
    }
    
    public void setBarometer(double v)
    {
        this.setFieldValue(FLD_barometer, v);
    }

    // ------------------------------------------------------------------------

    public double getAmbientTemp()
    {
        return this.getFieldValue(FLD_ambientTemp, INVALID_TEMPERATURE);
    }
    public void setAmbientTemp(double v)
    {
        this.setFieldValue(FLD_ambientTemp, v);
    }

    // ------------------------------------------------------------------------

    public double getCabinTemp()
    {
        return this.getFieldValue(FLD_cabinTemp, INVALID_TEMPERATURE);
    }
    public void setCabinTemp(double v)
    {
        this.setFieldValue(FLD_cabinTemp, v);
    }

    // ------------------------------------------------------------------------

    public static boolean isValidTemperature(double t)
    {
        // IE. ((T >= -126) && (T <= 126))
        return ((t >= -TEMPERATURE_LIMIT) && (t <= TEMPERATURE_LIMIT));
    }

    public double getThermoAverage(int ndx)
    {
        switch (ndx) {
            case 0: return this.getThermoAverage0();
            case 1: return this.getThermoAverage1();
            case 2: return this.getThermoAverage2();
            case 3: return this.getThermoAverage3();
            case 4: return this.getThermoAverage4();
            case 5: return this.getThermoAverage5();
            case 6: return this.getThermoAverage6();
            case 7: return this.getThermoAverage7();
        }
        return INVALID_TEMPERATURE;
    }

    public void setThermoAverage(int ndx, double v)
    {
        switch (ndx) {
            case 0: this.setThermoAverage0(v); break;
            case 1: this.setThermoAverage1(v); break;
            case 2: this.setThermoAverage2(v); break;
            case 3: this.setThermoAverage3(v); break;
            case 4: this.setThermoAverage4(v); break;
            case 5: this.setThermoAverage5(v); break;
            case 6: this.setThermoAverage6(v); break;
            case 7: this.setThermoAverage7(v); break;
        }
    }

    public void clearThermoAverage()
    {
        for (int n = 0; n <= 7; n++) {
            this.setThermoAverage(n, INVALID_TEMPERATURE);
        }
    }

    public double getThermoAverage0()
    {
        return this.getFieldValue(FLD_thermoAverage0, INVALID_TEMPERATURE);
    }
    public void setThermoAverage0(double v)
    {
        this.setFieldValue(FLD_thermoAverage0, v);
    }

    public double getThermoAverage1()
    {
        return this.getFieldValue(FLD_thermoAverage1, INVALID_TEMPERATURE);
    }
    public void setThermoAverage1(double v)
    {
        this.setFieldValue(FLD_thermoAverage1, v);
    }

    public double getThermoAverage2()
    {
        return this.getFieldValue(FLD_thermoAverage2, INVALID_TEMPERATURE);
    }
    public void setThermoAverage2(double v)
    {
        this.setFieldValue(FLD_thermoAverage2, v);
    }

    public double getThermoAverage3()
    {
        return this.getFieldValue(FLD_thermoAverage3, INVALID_TEMPERATURE);
    }
    public void setThermoAverage3(double v)
    {
        this.setFieldValue(FLD_thermoAverage3, v);
    }

    public double getThermoAverage4()
    {
        return this.getFieldValue(FLD_thermoAverage4, INVALID_TEMPERATURE);
    }
    public void setThermoAverage4(double v)
    {
        this.setFieldValue(FLD_thermoAverage4, v);
    }

    public double getThermoAverage5()
    {
        return this.getFieldValue(FLD_thermoAverage5, INVALID_TEMPERATURE);
    }
    public void setThermoAverage5(double v)
    {
        this.setFieldValue(FLD_thermoAverage5, v);
    }

    public double getThermoAverage6()
    {
        return this.getFieldValue(FLD_thermoAverage6, INVALID_TEMPERATURE);
    }
    public void setThermoAverage6(double v)
    {
        this.setFieldValue(FLD_thermoAverage6, v);
    }

    public double getThermoAverage7()
    {
        return this.getFieldValue(FLD_thermoAverage7, INVALID_TEMPERATURE);
    }
    public void setThermoAverage7(double v)
    {
        this.setFieldValue(FLD_thermoAverage7, v);
    }

    // Temerature Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Analog fields below

    public double getAnalog(int ndx)
    {
        switch (ndx) {
            case 0: return this.getAnalog0();
            case 1: return this.getAnalog1();
            case 2: return this.getAnalog2();
            case 3: return this.getAnalog3();
        }
        return 0.0;
    }

    public void setAnalog(int ndx, double v)
    {
        switch (ndx) {
            case 0: this.setAnalog0(v); break;
            case 1: this.setAnalog1(v); break;
            case 2: this.setAnalog2(v); break;
            case 3: this.setAnalog3(v); break;
        }
    }

    public double getAnalog0()
    {
        return this.getFieldValue(FLD_analog0, 0.0);
    }
    public void setAnalog0(double v)
    {
        this.setFieldValue(FLD_analog0, v);
    }

    public double getAnalog1()
    {
        return this.getFieldValue(FLD_analog1, 0.0);
    }
    public void setAnalog1(double v)
    {
        this.setFieldValue(FLD_analog1, v);
    }

    public double getAnalog2()
    {
        return this.getFieldValue(FLD_analog2, 0.0);
    }
    public void setAnalog2(double v)
    {
        this.setFieldValue(FLD_analog2, v);
    }

    public double getAnalog3()
    {
        return this.getFieldValue(FLD_analog3, 0.0);
    }
    public void setAnalog3(double v)
    {
        this.setFieldValue(FLD_analog3, v);
    }

    // Analog fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Day Summary fields below

    /* EOD number of engines start */
    public int getDayEngineStarts()
    {
        return this.getFieldValue(FLD_dayEngineStarts, 0);
    }
    public void setDayEngineStarts(int v)
    {
        this.setFieldValue(FLD_dayEngineStarts, v);
    }

    /* EOD idle hours */
    public double getDayIdleHours()
    {
        return this.getFieldValue(FLD_dayIdleHours, 0.0);
    }
    public void setDayIdleHours(double v)
    {
        this.setFieldValue(FLD_dayIdleHours, v);
    }

    /* EOD idle fuel */
    public double getDayFuelIdle()
    {
        return this.getFieldValue(FLD_dayFuelIdle, 0.0);
    }
    public void setDayFuelIdle(double v)
    {
        this.setFieldValue(FLD_dayFuelIdle, v);
    }

    /* EOD work hours */
    public double getDayWorkHours()
    {
        return this.getFieldValue(FLD_dayWorkHours, 0.0);
    }
    public void setDayWorkHours(double v)
    {
        this.setFieldValue(FLD_dayWorkHours, v);
    }

    /* EOD work fuel */
    public double getDayFuelWork()
    {
        return this.getFieldValue(FLD_dayFuelWork, 0.0);
    }
    public void setDayFuelWork(double v)
    {
        this.setFieldValue(FLD_dayFuelWork, v);
    }

    /* EOD PTO fuel */
    public double getDayFuelPTO()
    {
        return this.getFieldValue(FLD_dayFuelPTO, 0.0);
    }
    public void setDayFuelPTO(double v)
    {
        this.setFieldValue(FLD_dayFuelPTO, v);
    }

    /* EOD distance travelled */
    public double getDayDistanceKM()
    {
        return this.getFieldValue(FLD_dayDistanceKM, 0.0);
    }
    public void setDayDistanceKM(double v)
    {
        this.setFieldValue(FLD_dayDistanceKM, v);
    }

    /* EOD total fuel */
    public double getDayFuelTotal()
    {
        return this.getFieldValue(FLD_dayFuelTotal, 0.0);
    }
    public void setDayFuelTotal(double v)
    {
        this.setFieldValue(FLD_dayFuelTotal, v);
    }

    // Day Summary fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // PCell data fields below

    public boolean canUpdateCellTowerLocation()
    {

        /* CellID info check */
        if (this.getCellTowerID() <= 0) {
            return false;
        } else
        if (this.getMobileCountryCode() < 0) { // could be 0
            return false;
        } else
        if (this.getMobileNetworkCode() < 0) {
            return false;
        }

        /* account check */
        Account acct = this.getAccount();
        if (acct == null) {
            return false;
        }

        /* MobileLocationProvider check */
        MobileLocationProvider mlp = acct.getPrivateLabel().getMobileLocationProvider(); // test for existance
        if ((mlp == null) || !mlp.isEnabled()) {
            return false;
        }

        /* passed */
        return true;

    }
    
    /* update cell tower location ("updateAddress") */
    public Set<String> updateCellTowerLocation()
    {

        /* get Account */
        Account acct = this.getAccount();
        if (acct == null) {
            return null;
        }
        String acctID = this.getAccountID();

        /* MobileLocationProvider */
        BasicPrivateLabel privLabel = acct.getPrivateLabel(); // never null
        MobileLocationProvider mlp = privLabel.getMobileLocationProvider();
        if (mlp == null) {
            if (acct.hasPrivateLabel()) {
                Print.logInfo("[Account '%s'] PrivateLabel '%s' does not define a MobileLocationProvider", acctID, privLabel); 
            } else {
                Print.logInfo("No PrivateLabel (thus no MobileLocationProvider) for Account '%s'", acctID); 
            }
            return null;
        } else
        if (!mlp.isEnabled()) {
            Print.logInfo("MobileLocationProvider disabled: " + mlp.getName());
            return null;
        }

        /* get cell tower location */
        Set<String> updFields = new HashSet<String>();
        CellTower servCT   = this.getServingCellTower();
        CellTower nborCT[] = this.getNeighborCellTowers();
        Print.logInfo("Getting CellTower location: " + mlp.getName());
        MobileLocation ml = mlp.getMobileLocation(servCT, nborCT); // may return null
        if ((ml != null) && ml.hasGeoPoint()) {
            GeoPoint gp = ml.getGeoPoint();
            this.setCellLatitude( gp.getLatitude());
            updFields.add(EventData.FLD_cellLatitude);
            this.setCellLongitude(gp.getLongitude());
            updFields.add(EventData.FLD_cellLongitude);
            if (ml.hasAccuracy()) {
                double accuracyM = ml.getAccuracy();
                this.setCellAccuracy(accuracyM);
                updFields.add(EventData.FLD_cellAccuracy);
                Print.logInfo("CellTower location ["+acctID+"]: " + gp + " [+/- " + accuracyM + " meters]");
            } else {
                Print.logInfo("CellTower location ["+acctID+"]: " + gp);
            }
        }

        /* success (but MobileLocationProvider may still not have a valid location) */
        return updFields;

    }
    
    // -------------------------------

    /* Cell Tower Latitude */
    public double getCellLatitude()
    {
        return this.getFieldValue(FLD_cellLatitude, 0.0);
    }
    public void setCellLatitude(double v)
    {
        this.setFieldValue(FLD_cellLatitude, v);
    }

    /* Cell Tower Longitude */
    public double getCellLongitude()
    {
        return this.getFieldValue(FLD_cellLongitude, 0.0);
    }
    public void setCellLongitude(double v)
    {
        this.setFieldValue(FLD_cellLongitude, v);
    }
    
    /* Set Cell Tower GeoPoint */
    public void setCellGeoPoint(GeoPoint gp)
    {
        if (GeoPoint.isValid(gp)) {
            this.setCellLatitude( gp.getLatitude());
            this.setCellLongitude(gp.getLongitude());
        } else {
            this.setCellLatitude( 0.0);
            this.setCellLongitude(0.0);
        }
    }

    /* Get Cell Tower GeoPoint */
    public GeoPoint getCellGeoPoint()
    {
        double lat = this.getCellLatitude();
        double lon = this.getCellLongitude();
        if (GeoPoint.isValid(lat,lon)) {
            return new GeoPoint(lat,lon);
        } else {
            return GeoPoint.INVALID_GEOPOINT;
        }
    }

    /* has cell tower location */
    public boolean hasCellLocation()
    {
        double lat = this.getCellLatitude();
        double lon = this.getCellLongitude();
        return GeoPoint.isValid(lat,lon);
    }

    // -------------------------------

    /* Cell Tower GPS Location Accuracy (meters) */
    public double getCellAccuracy()
    {
        return this.getFieldValue(FLD_cellAccuracy, 0.0);
    }
    public void setCellAccuracy(double v)
    {
        this.setFieldValue(FLD_cellAccuracy, ((v >= 0.0)? v : 0.0));
    }

    // -------------------------------

    /* Gets the Mobile Country Code */
    public int getMobileCountryCode()
    {
        return this.getFieldValue(FLD_mobileCountryCode, 0);
    }
    public void setMobileCountryCode(int v)
    {
        this.setFieldValue(FLD_mobileCountryCode, v);
    }

    /* Mobile Network Code */
    public int getMobileNetworkCode()
    {
        return this.getFieldValue(FLD_mobileNetworkCode, 0);
    }
    public void setMobileNetworkCode(int v)
    {
        this.setFieldValue(FLD_mobileNetworkCode, v);
    }

    /* Cell Timing Advance */
    public int getCellTimingAdvance()
    {
        return this.getFieldValue(FLD_cellTimingAdvance, 0);
    }
    public void setCellTimingAdvance(int v)
    {
        this.setFieldValue(FLD_cellTimingAdvance, v);
    }

    /* Location Area Code */
    public int getLocationAreaCode()
    {
        return this.getFieldValue(FLD_locationAreaCode, 0);
    }
    public void setLocationAreaCode(int v)
    {
        this.setFieldValue(FLD_locationAreaCode, v);
    }

    /* Cell Tower ID */
    public int getCellTowerID()
    {
        return this.getFieldValue(FLD_cellTowerID, 0);
    }
    public void setCellTowerID(int v)
    {
        this.setFieldValue(FLD_cellTowerID, v);
    }

    /* Serving cell proterty information */
    public String getCellServingInfo()
    {
        return this.getFieldValue(FLD_cellServingInfo, "");
    }
    public void setCellServingInfo(String v)
    {
        this.setFieldValue(FLD_cellServingInfo, StringTools.trim(v));
    }

    /* get Serving CellTower instance */
    public CellTower getServingCellTower()
    {
        RTProperties ctp = new RTProperties(this.getCellServingInfo());
        ctp.setInt(CellTower.ARG_MCC, this.getMobileCountryCode());
        ctp.setInt(CellTower.ARG_MNC, this.getMobileNetworkCode());
        ctp.setInt(CellTower.ARG_TAV, this.getCellTimingAdvance());
        ctp.setInt(CellTower.ARG_LAC, this.getLocationAreaCode());
        ctp.setInt(CellTower.ARG_CID, this.getCellTowerID());
        if (ctp.getInt(CellTower.ARG_CID) <= 0) {
            return null;
        } else {
            CellTower ct = new CellTower(ctp);
            if (this.hasCellLocation()) {
                ct.setMobileLocation(this.getCellGeoPoint(), this.getCellAccuracy());
            }
            return ct;
        }
    }

    /* get Serving CellTower instance */
    public void setServingCellTower(CellTower cti)
    {
        if (cti != null) {
            this.setMobileCountryCode(cti.getMobileCountryCode());
            this.setMobileNetworkCode(cti.getMobileNetworkCode());
            this.setCellTimingAdvance(cti.getTimingAdvance()    );
            this.setLocationAreaCode( cti.getLocationAreaCode() );
            this.setCellTowerID(      cti.getCellTowerID()      );
            this.setCellServingInfo(  cti.toString()            );
        } else {
            this.setMobileCountryCode(0);
            this.setMobileNetworkCode(0);
            this.setCellTimingAdvance(0);
            this.setLocationAreaCode( 0);
            this.setCellTowerID(      0);
            this.setCellServingInfo(  null);
        }
    }
    
    // -------------------------------
    
    /* Nehghbor #0 cell proterty information */
    public String getCellNeighborInfo0()
    {
        return this.getFieldValue(FLD_cellNeighborInfo0, "");
    }
    public void setCellNeighborInfo0(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo0, StringTools.trim(v));
    }

    /* Nehghbor #1 cell proterty information */
    public String getCellNeighborInfo1()
    {
        return this.getFieldValue(FLD_cellNeighborInfo1, "");
    }
    public void setCellNeighborInfo1(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo1, StringTools.trim(v));
    }

    /* Nehghbor #2 cell proterty information */
    public String getCellNeighborInfo2()
    {
        return this.getFieldValue(FLD_cellNeighborInfo2, "");
    }
    public void setCellNeighborInfo2(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo2, StringTools.trim(v));
    }

    /* Nehghbor #3 cell proterty information */
    public String getCellNeighborInfo3()
    {
        return this.getFieldValue(FLD_cellNeighborInfo3, "");
    }
    public void setCellNeighborInfo3(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo3, StringTools.trim(v));
    }

    /* Nehghbor #4 cell proterty information */
    public String getCellNeighborInfo4()
    {
        return this.getFieldValue(FLD_cellNeighborInfo4, "");
    }
    public void setCellNeighborInfo4(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo4, StringTools.trim(v));
    }

    /* Nehghbor #5 cell proterty information */
    public String getCellNeighborInfo5()
    {
        return this.getFieldValue(FLD_cellNeighborInfo5, "");
    }
    public void setCellNeighborInfo5(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo5, StringTools.trim(v));
    }

    /* get Neighbor CellTower instance */
    public CellTower getNeighborCellTower(int ndx)
    {
        String cts = null;
        switch (ndx) {
            case 0: cts = this.getCellNeighborInfo0();  break;
            case 1: cts = this.getCellNeighborInfo1();  break;
            case 2: cts = this.getCellNeighborInfo2();  break;
            case 3: cts = this.getCellNeighborInfo3();  break;
            case 4: cts = this.getCellNeighborInfo4();  break;
            case 5: cts = this.getCellNeighborInfo5();  break;
        }
        if (!StringTools.isBlank(cts)) {
            return new CellTower(new RTProperties(cts));
        } else {
            return null;
        }
    }
    public void setNeighborCellTower(int ndx, CellTower cti)
    {
        String cts = (cti != null)? cti.toString() : null;
        switch (ndx) {
            case 0: this.setCellNeighborInfo0(cts);  break;
            case 1: this.setCellNeighborInfo1(cts);  break;
            case 2: this.setCellNeighborInfo2(cts);  break;
            case 3: this.setCellNeighborInfo3(cts);  break;
            case 4: this.setCellNeighborInfo4(cts);  break;
            case 5: this.setCellNeighborInfo5(cts);  break;
        }
    }

    /* get all Neighbor CellTower instances (if any) */
    public CellTower[] getNeighborCellTowers()
    {
        Collection<CellTower> nctList = null;
        for (int n = 0; n < COUNT_cellNeighborInfo; n++) {
            CellTower ct = this.getNeighborCellTower(n);
            if (ct != null) {
                if (nctList == null) { nctList = new Vector<CellTower>(); }
                nctList.add(ct);
            }
        }
        return (nctList != null)? nctList.toArray(new CellTower[nctList.size()]) : null;
    }
    public void setNeighborCellTowers(CellTower nct[])
    {
        int nctLen = ListTools.size(nct);
        for (int n = 0; n < COUNT_cellNeighborInfo; n++) {
            CellTower ct = (nctLen > n)? nct[n] : null;
            this.setNeighborCellTower(n, ct);
        }
    }
    public void setNeighborCellTowers(java.util.List<CellTower> nct)
    {
        int nctLen = ListTools.size(nct);
        for (int n = 0; n < COUNT_cellNeighborInfo; n++) {
            CellTower ct = (nctLen > n)? nct.get(n) : null;
            this.setNeighborCellTower(n, ct);
        }
    }

    // PCell data fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Custom Bean access fields below

    public String getDriverID()
    {
        return this.getFieldValue(FLD_driverID, "");
    }
    
    public void setDriverID(String v)
    {
        this.setFieldValue(FLD_driverID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public long getDriverStatus()
    {
        return this.getFieldValue(FLD_driverStatus, 0L);
    }
    
    public void setDriverStatus(long v)
    {
        this.setFieldValue(FLD_driverStatus, v);
    }

    // ------------------------------------------------------------------------

    public String getDriverMessage()
    {
        return this.getFieldValue(FLD_driverMessage, "");
    }
    
    public void setDriverMessage(String v)
    {
        this.setFieldValue(FLD_driverMessage, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getEmailRecipient()
    {
        String v = (String)this.getFieldValue(FLD_emailRecipient);
        return StringTools.trim(v);
    }

    public void setEmailRecipient(String v)
    {
        this.setFieldValue(FLD_emailRecipient, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public long getSensorLow()
    {
        return this.getFieldValue(FLD_sensorLow, 0L);
    }
    
    public void setSensorLow(long v)
    {
        this.setFieldValue(FLD_sensorLow, v);
    }

    // ------------------------------------------------------------------------

    public long getSensorHigh()
    {
        return this.getFieldValue(FLD_sensorHigh, 0L);
    }

    public void setSensorHigh(long v)
    {
        this.setFieldValue(FLD_sensorHigh, v);
    }

    // ------------------------------------------------------------------------

    /* Gets the Sample Index */
    public int getSampleIndex()
    {
        return this.getFieldValue(FLD_sampleIndex, 0);
    }
    
    public void setSampleIndex(int v)
    {
        this.setFieldValue(FLD_sampleIndex, v);
    }

    // ------------------------------------------------------------------------

    /* Gets the Sample Index */
    public String getSampleID()
    {
        String v = (String)this.getFieldValue(FLD_sampleID);
        return StringTools.trim(v);
    }
    
    public void setSampleID(String v)
    {
        this.setFieldValue(FLD_sampleID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* Gets the Sample Index */
    /*
    public double getAppliedPressure()
    {
        return this.getFieldValue(FLD_appliedPressure, 0.0); // kPa
    }
    
    public void setAppliedPressure(double v)
    {
        this.setFieldValue(FLD_appliedPressure, v);
    }
    */

    // ------------------------------------------------------------------------

    public double getBrakeGForce()
    {
        return this.getFieldValue(FLD_brakeGForce, 0.0);
    }
    
    public void setBrakeGForce(double v)
    {
        this.setFieldValue(FLD_brakeGForce, v);
    }

    // ------------------------------------------------------------------------

    public double getAcceleration()
    {
        return this.getFieldValue(FLD_acceleration, 0.0);
    }
    
    public void setAcceleration(double v)
    {
        this.setFieldValue(FLD_acceleration, v);
    }

    // ------------------------------------------------------------------------

    public double getBrakePressure()
    {
        return this.getFieldValue(FLD_brakePressure, 0.0); // kPa
    }

    public void setBrakePressure(double v)
    {
        this.setFieldValue(FLD_brakePressure, v);
    }

    // ------------------------------------------------------------------------

    public String getTirePressure()
    {
        return this.getFieldValue(FLD_tirePressure, ""); // kPa
    }

    public double[] getTirePressure_kPa()
    {
        String kPaStrArr = this.getTirePressure();
        if (StringTools.isBlank(kPaStrArr)) {
            return new double[0];
        } else {
            String kPaStr[] = StringTools.split(kPaStrArr,',');
            double kPaArr[] = new double[kPaStr.length];
            for (int i = 0; i < kPaStr.length; i++) {
                kPaArr[i] = StringTools.parseDouble(kPaStr[i],0.0);
            }
            return kPaArr;
        }
    }

    public double[] getTirePressure_psi()
    {
        String kPaStrArr = this.getTirePressure();
        if (StringTools.isBlank(kPaStrArr)) {
            return new double[0];
        } else {
            String kPaStr[] = StringTools.split(kPaStrArr,',');
            double psiArr[] = new double[kPaStr.length];
            for (int i = 0; i < kPaStr.length; i++) {
                double kPa = StringTools.parseDouble(kPaStr[i],0.0);
                psiArr[i] = kPa * Account.PSI_PER_KPA;
            }
            return psiArr;
        }
    }

    public double[] getTirePressure_units(Account.PressureUnits pu)
    {
        String kPaStrArr = this.getTirePressure();
        if (StringTools.isBlank(kPaStrArr)) {
            return new double[0];
        } else {
            if (pu == null) { pu = EnumTools.getDefault(Account.PressureUnits.class); }
            String kPaStr[] = StringTools.split(kPaStrArr,',');
            double uniArr[] = new double[kPaStr.length];
            for (int i = 0; i < kPaStr.length; i++) {
                double kPa = StringTools.parseDouble(kPaStr[i],0.0);
                uniArr[i] = pu.convertFromKPa(kPa);
            }
            return uniArr;
        }
    }

    public void setTirePressure(String v)
    {
        this.setFieldValue(FLD_tirePressure, v);
    }

    public void setTirePressure_kPa(double v[])
    {
        StringBuffer sb = new StringBuffer();
        if (!ListTools.isEmpty(v)) {
            for (int i = 0; i < v.length; i++) {
                double kPa = v[i];  // kPa ==> kPa
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(kPa,"0.0"));
            }
        }
        this.setTirePressure(sb.toString());
    }

    public void setTirePressure_psi(double v[])
    {
        StringBuffer sb = new StringBuffer();
        if (!ListTools.isEmpty(v)) {
            for (int i = 0; i < v.length; i++) {
                double kPa = v[i] / Account.PSI_PER_KPA; // psi * kPa/psi ==> kPa
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(kPa,"0.0"));
            }
        }
        this.setTirePressure(sb.toString());
    }

    // ------------------------------------------------------------------------

    public String getTireTemp()
    {
        return this.getFieldValue(FLD_tireTemp, ""); // C
    }

    public double[] getTireTemp_C()
    {
        String cStrArr = this.getTireTemp();
        if (StringTools.isBlank(cStrArr)) {
            return new double[0];
        } else {
            String cStr[] = StringTools.split(cStrArr, ',');
            double cArr[] = new double[cStr.length];
            for (int i = 0; i < cStr.length; i++) {
                double C = StringTools.parseDouble(cStr[i],0.0);
                cArr[i] = C;
            }
            return cArr;
        }
    }

    public double[] getTireTemp_units(Account.TemperatureUnits tu)
    {
        String cStrArr = this.getTireTemp();
        if (StringTools.isBlank(cStrArr)) {
            return new double[0];
        } else {
            if (tu == null) { tu = EnumTools.getDefault(Account.TemperatureUnits.class); }
            String cStr[] = StringTools.split(cStrArr,',');
            double uArr[] = new double[cStr.length];
            for (int i = 0; i < cStr.length; i++) {
                double C = StringTools.parseDouble(cStr[i],0.0);
                uArr[i] = tu.convertFromC(C);
            }
            return uArr;
        }
    }

    public void setTireTemp(String v)
    {
        this.setFieldValue(FLD_tireTemp, v);
    }

    public void setTireTemp_C(double v[])
    {
        StringBuffer sb = new StringBuffer();
        if (!ListTools.isEmpty(v)) {
            for (int i = 0; i < v.length; i++) {
                double C = v[i];  // C ==> C
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(C,"0.0"));
            }
        }
        this.setTireTemp(sb.toString());
    }

    // ------------------------------------------------------------------------

    public boolean getDataPush()
    {
        return this.getFieldValue(FLD_dataPush, true);
    }

    public void setDataPush(boolean v)
    {
        this.setFieldValue(FLD_dataPush, v);
    }

    // ------------------------------------------------------------------------

    public long getCostCenter()
    {
        return this.getFieldValue(FLD_costCenter, 0L);
    }

    public void setCostCenter(long v)
    {
        this.setFieldValue(FLD_costCenter, v);
    }

    // ------------------------------------------------------------------------

    public String getJobNumber()
    {
        return this.getFieldValue(FLD_jobNumber, "");
    }

    public void setJobNumber(String v)
    {
        this.setFieldValue(FLD_jobNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getRfidTag()
    {
        return this.getFieldValue(FLD_rfidTag, "");
    }

    public void setRfidTag(String v)
    {
        this.setFieldValue(FLD_rfidTag, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getAttachType()
    {
        return this.getFieldValue(FLD_attachType, ""); // image: jpeg, gif, png, etc
    }

    public void setAttachType(String v)
    {
        this.setFieldValue(FLD_attachType, StringTools.trim(v));
    }

    public byte[] getAttachData()
    {
        byte v[] = (byte[])this.getFieldValue(FLD_attachData); // image bytes, etc
        return (v != null)? v : new byte[0];
    }

    public void setAttachData(byte[] v)
    {
        this.setFieldValue(FLD_attachData, ((v != null)? v : new byte[0]));
    }

    public void setAttachment(String type, byte data[])
    {
        if (ListTools.isEmpty(data)) {
            this.setAttachType(null);
            this.setAttachData(null);
        } else {
            this.setAttachType(StringTools.isBlank(type)? 
                type : HTMLTools.getMimeTypeFromData(data,null));
            this.setAttachData(data);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA timestamp in Unix/Epoch time
    *** @return The ETA timestamp of this event
    **/
    public long getEtaTimestamp()
    {
        return this.getFieldValue(FLD_etaTimestamp, 0L);
    }

    /**
    *** Sets the ETA timestamp in Unix/Epoch time
    *** @param v The ETA timestamp
    **/
    public void setEtaTimestamp(long v)
    {
        this.setFieldValue(FLD_etaTimestamp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA unique-id 
    *** @return The ETA unique-id 
    **/
    public long getEtaUniqueID()
    {
        return this.getFieldValue(FLD_etaUniqueID, 0L);
    }

    /**
    *** Sets the ETA unique-id
    *** @param v The ETA unique-id
    **/
    public void setEtaUniqueID(long v)
    {
        this.setFieldValue(FLD_etaUniqueID, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA distance in kilometers
    *** @return The ETA distance in kilometers
    **/
    public double getEtaDistanceKM()
    {
        return this.getFieldValue(FLD_etaDistanceKM, 0.0);
    }

    /**
    *** Sets the ETA distance in kilometers
    *** @param v The ETA distance in kilometers
    **/
    public void setEtaDistanceKM(double v)
    {
        this.setFieldValue(FLD_etaDistanceKM, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA Latitude
    *** @return The ETA Latitude
    **/
    public double getEtaLatitude()
    {
        return this.getFieldValue(FLD_etaLatitude, 0.0);
    }
    
    /**
    *** Sets the ETA Latitude
    *** @param v The ETA Latitude
    **/
    public void setEtaLatitude(double v)
    {
        this.setFieldValue(FLD_etaLatitude, v);
    }
    
    /**
    *** Sets the ETA GeoPoint
    *** @param gp The ETA GeoPoint
    **/
    public void setEtaGeoPoint(GeoPoint gp)
    {
        if ((gp != null) && gp.isValid()) {
            this.setEtaLatitude( gp.getLatitude());
            this.setEtaLongitude(gp.getLongitude());
        } else {
            this.setEtaLatitude( 0.0);
            this.setEtaLongitude(0.0);
        }
    }

    /**
    *** Gets the ETA GeoPoint
    *** @return The ETA GeoPoint
    **/
    public GeoPoint getEtaGeoPoint()
    {
        return new GeoPoint(this.getEtaLatitude(), this.getEtaLongitude());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA Longitude
    *** @return The ETA Longitude
    **/
    public double getEtaLongitude()
    {
        return this.getFieldValue(FLD_etaLongitude, 0.0);
    }
    
    /**
    *** Sets the ETA Longitude
    *** @param v The ETA Longitude
    **/
    public void setEtaLongitude(double v)
    {
        this.setFieldValue(FLD_etaLongitude, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the STOP id 
    *** @return The STOP id 
    **/
    public long getStopID()
    {
        return this.getFieldValue(FLD_stopID, 0L);
    }

    /**
    *** Sets the STOP id
    *** @param v The STOP id
    **/
    public void setStopID(long v)
    {
        this.setFieldValue(FLD_stopID, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the STOP Status 
    *** @return The STOP Status 
    **/
    public int getStopStatus()
    {
        return this.getFieldValue(FLD_stopStatus, 0);
    }

    /**
    *** Sets the STOP Status
    *** @param v The STOP Status
    **/
    public void setStopStatus(int v)
    {
        this.setFieldValue(FLD_stopStatus, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the STOP Index 
    *** @return The STOP Index 
    **/
    public int getStopIndex()
    {
        return this.getFieldValue(FLD_stopIndex, 0);
    }

    /**
    *** Sets the STOP Index
    *** @param v The STOP Index
    **/
    public void setStopIndex(int v)
    {
        this.setFieldValue(FLD_stopIndex, v);
    }

    // Common Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // J1708 Bean access fields below

    //public int getObdType()
    //{
    //    return this.getFieldValue(FLD_obdType, 0);
    //}
    
    //public void setObdType(int v)
    //{
    //    this.setFieldValue(FLD_obdType, v);
    //}

    // ------------------------------------------------------------------------

    public double getFuelPressure()
    {
        return this.getFieldValue(FLD_fuelPressure, 0.0); // kPa
    }

    public void setFuelPressure(double v)
    {
        this.setFieldValue(FLD_fuelPressure, v);
    }

    // ------------------------------------------------------------------------

    public double getFuelUsage()
    {
        return this.getFieldValue(FLD_fuelUsage, 0.0);
    }
    
    public void setFuelUsage(double v)
    {
        this.setFieldValue(FLD_fuelUsage, v);
    }

    // ------------------------------------------------------------------------

    public double getFuelTemp()
    {
        return this.getFieldValue(FLD_fuelTemp, 0.0);
    }
    
    public void setFuelTemp(double v)
    {
        this.setFieldValue(FLD_fuelTemp, v);
    }

    // ------------------------------------------------------------------------

    public double getFuelLevel()
    {
        return this.getFieldValue(FLD_fuelLevel, 0.0);
    }
    
    public void setFuelLevel(double v)
    {
        this.setFieldValue(FLD_fuelLevel, v);
    }

    public double getFuelLevelVolume_Liters()
    {
        Device device = this.getDevice();
        if (device != null) {
            return this.getFuelLevel() * device.getFuelCapacity();
        } else {
            return 0.0;
        }
    }

    public double getFuelLevelVolume_Units()
    {
        Account.VolumeUnits vu = Account.getVolumeUnits(this.getAccount());
        return vu.convertFromLiters(this.getFuelLevelVolume_Liters());
    }

    //public String getFuelLevelVolume_Title(Locale locale)
    //{
    //    I18N i18n = I18N.getI18N(EventData.class, locale);
    //    return i18n.getString("EventData.fuelLevelVolume", "Fuel Volume");
    //}

    // ------------------------------------------------------------------------

    public double getFuelEconomy()
    {
        return this.getFieldValue(FLD_fuelEconomy, 0.0);
    }
    
    public void setFuelEconomy(double v)
    {
        this.setFieldValue(FLD_fuelEconomy, v);
    }

    // ------------------------------------------------------------------------

    public double getFuelTotal()
    {
        return this.getFieldValue(FLD_fuelTotal, 0.0);
    }

    public void setFuelTotal(double v)
    {
        this.setFieldValue(FLD_fuelTotal, v);
    }

    // ------------------------------------------------------------------------

    public double getFuelIdle()
    {
        return this.getFieldValue(FLD_fuelIdle, 0.0);
    }
    
    public void setFuelIdle(double v)
    {
        this.setFieldValue(FLD_fuelIdle, v);
    }

    // ------------------------------------------------------------------------

    public double getFuelPTO()
    {
        return this.getFieldValue(FLD_fuelPTO, 0.0);
    }
    
    public void setFuelPTO(double v)
    {
        this.setFieldValue(FLD_fuelPTO, v);
    }

    // ------------------------------------------------------------------------

    public long getEngineRpm()
    {
        return this.getFieldValue(FLD_engineRpm, 0L);
    }
    
    public void setEngineRpm(long v)
    {
        this.setFieldValue(FLD_engineRpm, v);
    }

    // ------------------------------------------------------------------------

    public double getEngineHours()
    {
        return this.getFieldValue(FLD_engineHours, 0.0);
    }
    
    public void setEngineHours(double v)
    {
        this.setFieldValue(FLD_engineHours, v);
    }

    // ------------------------------------------------------------------------

    public double getEngineLoad()
    {
        return this.getFieldValue(FLD_engineLoad, 0.0);
    }
    
    public void setEngineLoad(double v)
    {
        this.setFieldValue(FLD_engineLoad, v);
    }

    // ------------------------------------------------------------------------

    public double getEngineTorque()
    {
        return this.getFieldValue(FLD_engineTorque, 0.0);
    }
    
    public void setEngineTorque(double v)
    {
        this.setFieldValue(FLD_engineTorque, v);
    }

    // ------------------------------------------------------------------------

    public double getIdleHours()
    {
        return this.getFieldValue(FLD_idleHours, 0.0);
    }
    
    public void setIdleHours(double v)
    {
        this.setFieldValue(FLD_idleHours, v);
    }

    // ------------------------------------------------------------------------

    public double getWorkHours()
    {
        return this.getFieldValue(FLD_workHours, 0.0);
    }
    
    public void setWorkHours(double v)
    {
        this.setFieldValue(FLD_workHours, v);
    }

    // ------------------------------------------------------------------------

    public double getTransOilTemp()
    {
        return this.getFieldValue(FLD_transOilTemp, 0.0);
    }
    
    public void setTransOilTemp(double v)
    {
        this.setFieldValue(FLD_transOilTemp, v);
    }

    // ------------------------------------------------------------------------

    public double getCoolantLevel()
    {
        return this.getFieldValue(FLD_coolantLevel, 0.0);
    }
    
    public void setCoolantLevel(double v)
    {
        this.setFieldValue(FLD_coolantLevel, v);
    }

    // ------------------------------------------------------------------------

    public double getCoolantTemp()
    {
        return this.getFieldValue(FLD_coolantTemp, 0.0);
    }
    
    public void setCoolantTemp(double v)
    {
        this.setFieldValue(FLD_coolantTemp, v);
    }

    // ------------------------------------------------------------------------

    public double getIntakeTemp()
    {
        return this.getFieldValue(FLD_intakeTemp, 0.0);
    }
    
    public void setIntakeTemp(double v)
    {
        this.setFieldValue(FLD_intakeTemp, v);
    }

    // ------------------------------------------------------------------------

    public double getOilPressure()
    {
        return this.getFieldValue(FLD_oilPressure, 0.0); // kPa
    }
    
    public void setOilPressure(double v)
    {
        this.setFieldValue(FLD_oilPressure, v);
    }

    // ------------------------------------------------------------------------

    public double getOilLevel()
    {
        return this.getFieldValue(FLD_oilLevel, 0.0);
    }
    
    public void setOilLevel(double v)
    {
        this.setFieldValue(FLD_oilLevel, v);
    }

    // ------------------------------------------------------------------------

    public double getOilTemp()
    {
        return this.getFieldValue(FLD_oilTemp, 0.0);
    }
    
    public void setOilTemp(double v)
    {
        this.setFieldValue(FLD_oilTemp, v);
    }

    // ------------------------------------------------------------------------

    public double getAirPressure()
    {
        return this.getFieldValue(FLD_airPressure, 0.0); // kPa
    }
    
    public void setAirPressure(double v)
    {
        this.setFieldValue(FLD_airPressure, v);
    }

    // ------------------------------------------------------------------------

    public double getAirFilterPressure()
    {
        return this.getFieldValue(FLD_airFilterPressure, 0.0); // kPa
    }
    
    public void setAirFilterPressure(double v)
    {
        this.setFieldValue(FLD_airFilterPressure, v);
    }

    // ------------------------------------------------------------------------

    public double getTurboPressure()
    {
        return this.getFieldValue(FLD_turboPressure, 0.0); // kPa
    }
    
    public void setTurboPressure(double v)
    {
        this.setFieldValue(FLD_turboPressure, v);
    }

    // ------------------------------------------------------------------------

    public boolean getPtoEngaged()
    {
        return this.getFieldValue(FLD_ptoEngaged, true);
    }

    public void setPtoEngaged(boolean v)
    {
        this.setFieldValue(FLD_ptoEngaged, v);
    }

    // ------------------------------------------------------------------------

    public double getPtoHours()
    {
        return this.getFieldValue(FLD_ptoHours, 0.0);
    }

    public void setPtoHours(double v)
    {
        this.setFieldValue(FLD_ptoHours, v);
    }

    // ------------------------------------------------------------------------

    public double getThrottlePos()
    {
        return this.getFieldValue(FLD_throttlePos, 0.0);
    }

    public void setThrottlePos(double v)
    {
        this.setFieldValue(FLD_throttlePos, v);
    }

    // ------------------------------------------------------------------------

    public double getBrakePos()
    {
        return this.getFieldValue(FLD_brakePos, 0.0);
    }

    public void setBrakePos(double v)
    {
        this.setFieldValue(FLD_brakePos, v);
    }

    // ------------------------------------------------------------------------

    public double getVBatteryVolts()
    {
        return this.getFieldValue(FLD_vBatteryVolts, 0.0);
    }
    
    public void setVBatteryVolts(double v)
    {
        this.setFieldValue(FLD_vBatteryVolts, v);
    }

    // ------------------------------------------------------------------------

    public long getJ1708Fault()
    {
        return this.getFieldValue(FLD_j1708Fault, 0L);
    }
    
    public void setJ1708Fault(long v)
    {
        this.setFieldValue(FLD_j1708Fault, v);
    }

    public long getOBDFault()
    {
        // see DTOBDFault
        return this.getJ1708Fault();
    }
    
    public void setOBDFault(long v)
    {
        // see DTOBDFault
        this.setJ1708Fault(v);
    }

    // ------------------------------------------------------------------------

    public String getFaultCode()
    {
        return this.getFieldValue(FLD_faultCode, "");
    }
    
    public void setFaultCode(String v)
    {
        this.setFieldValue(FLD_faultCode, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return true if the Malfunction-Indicator-Lamp is on */
    public boolean getMalfunctionLamp()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_malfunctionLamp);
        return (v != null)? v.booleanValue() : false;
    }

    /* sets the Malfunction-Indicator-Lamp state */
    public void setMalfunctionLamp(boolean v)
    {
        this.setFieldValue(FLD_malfunctionLamp, v);
    }

    // J1708/J1939 Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /* event index */
    private int eventIndex = -1;
    public void setEventIndex(int ndx) 
    {
        this.eventIndex = ndx;
    }
    
    public int getEventIndex()
    {
        return this.eventIndex;
    }
    
    public boolean getIsFirstEvent()
    {
        return (this.getEventIndex() == 0);
    }

    // ------------------------------------------------------------------------

    /* last event in list */
    private boolean isLastEventInList = false;
    public void setIsLastEvent(boolean isLast)
    {
        this.isLastEventInList = isLast;
    }
    
    public boolean getIsLastEvent()
    {
        return this.isLastEventInList;
    }

    public boolean showLastEventDevicePushpin(boolean isFleet, BasicPrivateLabel bpl)
    {
        if (isFleet) {
            boolean dft = false;
            if (bpl != null) {
                return bpl.getRTProperties().getBoolean(BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_fleet,dft);
            } else {
                return dft;
            }
        } else {
            boolean dft = false;
            if (bpl != null) {
                return bpl.getRTProperties().getBoolean(BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_device,dft);
            } else {
                return dft;
            }
        }
    }

    // ------------------------------------------------------------------------

    /* rule trigger event */
    private boolean isSynthesizedEvent = false;
    public void setSynthesizedEvent(boolean isSynthesized)
    {
        this.isSynthesizedEvent = isSynthesized;
    }
    
    public boolean getIsSynthesizedEvent()
    {
        return this.isSynthesizedEvent;
    }

    // ------------------------------------------------------------------------
    
    /* PushpinIconIndexProvider */
    private PushpinIconIndexProvider iconIndexProvider = null;
    
    /**
    *** Sets the Pushpin Icon Index Provider
    *** @param piip  The PushpinIconIndexProvider instance
    **/
    public void setPushpinIconIndexProvider(PushpinIconIndexProvider piip)
    {
        this.iconIndexProvider = piip;
    }

    // ------------------------------------------------------------------------
    
    /* preset explicit icon index */
    private int explicitPushpinIconIndex = -1;
    
    /**
    *** Sets the explicit Pushpin Icon Index
    *** @param epii  The PushpinIconIndexProvider instance
    **/
    public void setPushpinIconIndex(int epii)
    {
        this.explicitPushpinIconIndex = epii;
    }
    
    /**
    *** Sets the explicit Pushpin Icon Index
    *** @param iconName  The icon name
    *** @param iconKeys  The list of icon keys from which the index is derived, based on the position of
    ***                  icon name in this list.
    **/
    public void setPushpinIconIndex(String iconName, OrderedSet<String> iconKeys)
    {
        this.setPushpinIconIndex(EventData._getPushpinIconIndex(iconName, iconKeys, ICON_PUSHPIN_GREEN));
    }

    // ------------------------------------------------------------------------
    
    private static boolean DebugPushpins = RTConfig.getBoolean("DebugPushpins",false);
    
    /**
    *** Gets the default map icon index
    *** @param iconSelector  An icon 'selector' to be analyzed by the installed 'RuleFactory' to
    ***         determine the icon index. (may be blank/null)
    *** @param iconKeys The defined icon keys (the returned index must be within the
    ***                 the range of this list).
    *** @param isFleet  True if obtaining an icon index for a 'fleet' map
    *** @return An icon index
    **/
    public int getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys,
        boolean isFleet, BasicPrivateLabel bpl)
    {

        // ---------------------------------------
        // explicit pushpin chooser

        /* ExplicitIndex: do we have an explicit icon index? */
        if (this.explicitPushpinIconIndex >= 0) {
            if (DebugPushpins) Print.logInfo("Pushpin - explicit index: " + this.explicitPushpinIconIndex);
            return this.explicitPushpinIconIndex;
        }

        /* ExplicitProvider: do we have an explicit icon index provider? */
        if (this.iconIndexProvider != null) {
            int iconNdx = this.iconIndexProvider.getPushpinIconIndex(this, iconSelector, iconKeys, isFleet, bpl);
            if (iconNdx >= 0) {
                if (DebugPushpins) Print.logInfo("Pushpin - IconIndexProvider index: " + iconNdx);
                return iconNdx;
            }
        }

        // ---------------------------------------
        // MapProvider ==> IconSelector pushpin chooser

        /* IconSelector: check rule factory */
        RuleFactory ruleFact = Device.getRuleFactory();
        if ((ruleFact != null) && !StringTools.isBlank(iconSelector)) {
            int iconNdx = this._getPushpinIndexFromRuleSelector(ruleFact, "IconSelector", iconSelector, iconKeys);
            if (iconNdx >= 0) {
                return iconNdx;
            }
        }

        // ---------------------------------------

        /* FleetPushpin: fleet map - custom Device pushpin? */
        if (isFleet) {

            /* Device: fleet map? - custom Device icon */
            if (this.showLastEventDevicePushpin(isFleet,bpl)) {
                Device dev = this.getDevice();
                if ((dev != null) && dev.hasPushpinID()) {
                    String devIcon = dev.getPushpinID();
                    int iconNdx = EventData._getPushpinIconIndex(devIcon, iconKeys, -1);
                    if (iconNdx >= 0) {
                        if (DebugPushpins) Print.logInfo("Pushpin - Device ["+dev.getDeviceID()+"] index: " + iconNdx);
                        return iconNdx;
                    }
                }
            }

            /* FleetName: fleet map - custom Device pushpin? */
            int fletIconNdx = EventData._getPushpinIconIndex("fleet", iconKeys, -1/*ICON_PUSHPIN_BLUE*/);
            if (fletIconNdx >= 0) {
                if (DebugPushpins) Print.logInfo("Pushpin - 'fleet' index: " + fletIconNdx);
                return fletIconNdx;
            }

        }

        /* AllPushpin, "all" pushpins? */
        int allIconNdx = EventData._getPushpinIconIndex("all", iconKeys, -1);
        if (allIconNdx >= 0) { // '0' is a valid index
            if (DebugPushpins) Print.logInfo("Pushpin - 'all' index: " + allIconNdx);
            return allIconNdx;
        }

        // ---------------------------------------

        /* StatusCode */
        int scIconNdx = this._getStatusCodePushpinIndex(iconKeys, bpl);
        if (scIconNdx >= 0) {
            return scIconNdx;
        }

        /* LastPushpin: last event? */
        if (!isFleet && this.getIsLastEvent()) {

            /* last event with device pushpin */
            if (this.showLastEventDevicePushpin(!isFleet,bpl)) {
                Device dev = this.getDevice();
                if ((dev != null) && dev.hasPushpinID()) {
                    String devIcon = dev.getPushpinID();
                    int devIconNdx = EventData._getPushpinIconIndex(devIcon, iconKeys, -1);
                    if (devIconNdx >= 0) {
                        if (DebugPushpins) Print.logInfo("Pushpin - Device ["+dev.getDeviceID()+"] index: " + devIconNdx);
                        return devIconNdx;
                    }
                }
            }

            /* standard "last" event */
            int lastIconNdx = EventData._getPushpinIconIndex("last", iconKeys, -1);
            if (lastIconNdx >= 0) { // '0' is a valid index
                if (DebugPushpins) Print.logInfo("Pushpin - 'last' index: " + lastIconNdx);
                return lastIconNdx;
            }

        }

        /* DefaultPushpin: default icon index */
        String dftIconName = RTConfig.getString("EventData.defaultPushpinName","heading");
        int dftIconNdx = EventData._getPushpinIconIndex(dftIconName, iconKeys, ICON_PUSHPIN_GREEN);
        if (dftIconNdx >= 0) {
            if (DebugPushpins) Print.logInfo("Pushpin - default index: " + dftIconNdx);
            return dftIconNdx;
        }

        /* NoPushpin: no pushpin found! */
        if (DebugPushpins) Print.logInfo("Pushpin - No Pushpin Found!");
        return ICON_PUSHPIN_BLACK;

    }
    
    private int _getPushpinIndexFromRuleSelector(RuleFactory ruleFact, 
        String type, String iconSel, OrderedSet<String> iconKeys)
    {
        if ((ruleFact != null) && !StringTools.isBlank(iconSel)) {
            try {
                //Print.logInfo("iconSel: " + iconSel);
                Object result = ruleFact.evaluateSelector(iconSel,this);
                if (result instanceof Number) {
                    int iconNdx = ((Number)result).intValue();
                    if (iconNdx >= iconKeys.size()) {
                        Print.logWarn("Pushpin index invalid: " + iconNdx);
                        if (DebugPushpins) Print.logInfo("Pushpin - "+type+" rule ["+iconSel+"] index: " + iconNdx + " (INVALID)");
                        return iconNdx;
                    } else
                    if (iconNdx >= 0) { // '0' is a valid index
                        if (DebugPushpins) Print.logInfo("Pushpin - "+type+" rule ["+iconSel+"] index: " + iconNdx);
                        return iconNdx;
                    } else {
                        // no pushpin chosen
                        return -1;
                    }
                } else
                if (result instanceof String) {
                    String iconName = (String)result;
                    if (!StringTools.isBlank(iconName)) {
                        int iconNdx = EventData._getPushpinIconIndex(iconName, iconKeys, -1);
                        if (iconNdx >= 0) {
                            if (DebugPushpins) Print.logInfo("Pushpin - "+type+" rule ["+iconSel+"] index: " + iconNdx);
                            return iconNdx;
                        } else {
                            Print.logWarn("Pushpin not found: " + iconName);
                            iconNdx = ICON_PUSHPIN_BLACK;
                            if (DebugPushpins) Print.logInfo("Pushpin - "+type+" rule ["+iconSel+"] index: " + iconNdx + " (INVALID)");
                            return iconNdx;
                        }
                    } else {
                        // no pushpin chosen
                        return -1;
                    }
                } else {
                    Print.logError("Pushpin selector invalid result type: " + StringTools.className(result));
                    int iconNdx = ICON_PUSHPIN_BLACK;
                    if (DebugPushpins) Print.logInfo("Pushpin - "+type+" rule ["+iconSel+"] index: " + iconNdx + " (INVALID)");
                    return iconNdx;
                }
            } catch (RuleParseException rpe) {
                Print.logError("Pushpin selector parse error: " + rpe.getMessage());
                int iconNdx = ICON_PUSHPIN_BLACK;
                if (DebugPushpins) Print.logInfo("Pushpin - "+type+" rule ["+iconSel+"] index: " + iconNdx + " (ERROR)");
                return iconNdx;
            }
        } else {
            // no pushpin chosen
            return -1;
        }
    }

    private int _getStatusCodePushpinIndex(OrderedSet<String> iconKeys, BasicPrivateLabel bpl)
    {

        /* StatusPushpin: device map? - statusCode icon name */
        String scIconName = this.getStatusCodeIconName(bpl);
        if (!StringTools.isBlank(scIconName)) {
            int iconNdx = EventData._getPushpinIconIndex(scIconName, iconKeys, -1);
            if (iconNdx >= 0) {
                if (DebugPushpins) Print.logInfo("Pushpin - StatusCode name index: " + iconNdx);
                return iconNdx;
            }
        }

        /* StatusRulePushpin: status code icon selector */
        RuleFactory ruleFact = Device.getRuleFactory();
        if (ruleFact != null) {
            String scIconSel = this.getStatusCodeIconSelector(bpl);
            int iconNdx = this._getPushpinIndexFromRuleSelector(ruleFact, "StatusCode", scIconSel, iconKeys);
            if (iconNdx >= 0) {
                return iconNdx;
            }
        }
        
        /* no pushpin chosen */
        return -1;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* format this EventData record in CSV format according to the specified fields */
    public String formatAsCSVRecord(String fields[])
    {
        String csvSep = ",";
        StringBuffer sb = new StringBuffer();
        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) { sb.append(csvSep); }
                DBField dbFld = this.getRecordKey().getField(fields[i]);
                Object val = (dbFld != null)? this.getFieldValue(fields[i]) : null;
                if (val != null) {
                    Class typeClass = dbFld.getTypeClass();
                    if (fields[i].equals(FLD_statusCode)) {
                        int code = ((Integer)val).intValue();
                        StatusCodes.Code c = StatusCodes.GetCode(code,Account.getPrivateLabel(this.getAccount()));
                        if (c != null) {
                            sb.append("\"" + c.getDescription(null) + "\"");
                        } else {
                            sb.append("\"0x" + StringTools.toHexString(code,16) + "\"");
                        }
                    } else 
                    if ((typeClass == Double.class) || (typeClass == Double.TYPE)) {
                        double d = ((Double)val).doubleValue();
                        String fmt = dbFld.getFormat();
                        if ((fmt != null) && !fmt.equals("")) {
                            sb.append("\"" + StringTools.format(d,fmt) + "\"");
                        } else {
                            sb.append("\"" + String.valueOf(d) + "\"");
                        }
                    } else 
                    if ((typeClass == Float.class) || (typeClass == Float.TYPE)) {
                        float d = ((Float)val).floatValue();
                        String fmt = dbFld.getFormat();
                        if ((fmt != null) && !fmt.equals("")) {
                            sb.append("\"" + StringTools.format(d,fmt) + "\"");
                        } else {
                            sb.append("\"" + String.valueOf(d) + "\"");
                        }
                    } else {
                        sb.append(StringTools.quoteCSVString(val.toString()));
                    }
                }
            }
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

    private EventData previousEventData          = null;
    private EventData previousEventData_validGPS = null;

    public void setPreviousEventData(EventData ev)
    {
        if (ev != null) {
            this.previousEventData = ev;
            if (this.previousEventData.isValidGeoPoint()) {
                this.previousEventData_validGPS = this.previousEventData;
            }
        }
    }
    
    public EventData getPreviousEventData(boolean validGPS)
        throws DBException
    {
        return this.getPreviousEventData(null, validGPS);
    }

    public EventData getPreviousEventData(int statusCodes[], boolean validGPS)
        throws DBException
    {

        /* check previous event cache */
        if (statusCodes == null) {
            // check for cached previous event
            if (!validGPS && (this.previousEventData != null)) {
                return this.previousEventData;
            } else
            if (validGPS && (this.previousEventData_validGPS != null)) {
                return this.previousEventData_validGPS;
            }
        }

        /* get previous event */
        // 'endTime' should be this events timestamp, 
        // and 'additionalSelect' should be (statusCode != this.getStatusCode())
        long startTime = -1L; // start of time
        long endTime   = this.getTimestamp() - 1L; // previous to this event
        EventData ed[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            startTime, endTime,
            statusCodes,
            validGPS,
            EventData.LimitType.LAST, 1/*limit*/, true/*ascending*/,
            null/*additionalSelect*/);
        if (!ListTools.isEmpty(ed)) {
            EventData ev = ed[0];
            if (statusCodes == null) {
                // cache event
                if (validGPS) {
                    this.previousEventData_validGPS = ev;
                } else {
                    this.previousEventData = ev;
                    if (this.previousEventData.isValidGeoPoint()) {
                        this.previousEventData_validGPS = this.previousEventData;
                    }
                }
            }
            return ev;
        } else {
            return null;
        }
        
    }

    public static EventData getPreviousEventData(
        String accountID, String deviceID,
        long timestamp, int statusCodes[], 
        boolean validGPS)
        throws DBException
    {
        EventData ed[] = EventData.getRangeEvents(
            accountID, deviceID,
            -1L/*startTime*/, (timestamp - 1L),
            statusCodes,
            validGPS,
            EventData.LimitType.LAST, 1/*limit*/, true/*ascending*/,
            null/*additionalSelect*/);
        return !ListTools.isEmpty(ed)? ed[0] : null;
    }
    
    // ------------------------------------------------------------------------

    private EventData nextEventData          = null;
    private EventData nextEventData_validGPS = null;
    
    public EventData getNextEventData(boolean validGPS)
        throws DBException
    {

        if ((!validGPS && (this.nextEventData == null)) ||
            ( validGPS && (this.nextEventData_validGPS == null))) {
            // 'startTime' should be this events timestamp, 
            // and 'additionalSelect' should be (statusCode != this.getStatusCode())
            long startTime   = this.getTimestamp() + 1L;
            long endTime     = -1L;
            EventData ed[] = EventData.getRangeEvents(
                this.getAccountID(), this.getDeviceID(),
                startTime, endTime,
                null/*statusCodes[]*/,
                validGPS,
                EventData.LimitType.FIRST, 1/*limit*/, true/*ascending*/,
                null/*additionalSelect*/);
            if ((ed != null) && (ed.length > 0)) {
                if (validGPS) {
                    this.nextEventData_validGPS = ed[0];
                } else {
                    this.nextEventData = ed[0];
                    if (this.nextEventData.isValidGeoPoint()) {
                        this.nextEventData_validGPS = this.nextEventData;
                    }
                }
            }
        }

        return validGPS? this.nextEventData_validGPS : this.nextEventData;
    }
    
    // ------------------------------------------------------------------------

    /* override DBRecord.getFieldValue(...) */
    public Object getFieldValue(String fldName)
    {
        //if ((fldName != null) && fldName.startsWith(DBRecord.PSEUDO_FIELD_CHAR)) {
        //    if (fldName.equals(EventData.PFLD_deviceDesc)) {
        //        return this.getDeviceDescription();
        //    } else {
        //        return null;
        //    }
        //} else {
            return super.getFieldValue(fldName);
        //}
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback when record is about to be inserted into the table
    **/
    protected void recordWillInsert()
    {
        // overriden to optimize 
        // (DBRecordListnener not allowed, to prevent excessive backlogging)
    }

    /**
    *** Callback after record has been be inserted into the table
    **/
    protected void recordDidInsert()
    {
        // overriden to optimize
        // (DBRecordListnener not allowed, to prevent excessive backlogging)
        // ----
        // TODO: Queue JMS EventData message?
    }

    /**
    *** Callback when record is about to be updated in the table
    **/
    protected void recordWillUpdate()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    /**
    *** Callback after record has been be updated in the table
    **/
    protected void recordDidUpdate()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates an EventData record from the specified GeoEvent
    *** @param gev  The GeoEvent
    **/
    public static EventData createEventDataRecord(GeoEvent gev)
    {

        /* invalid event */
        if (gev == null) {
            Print.logError("GeoEvent is null");
            return null;
        }
                
        /* create key */
        String acctID = gev.getAccountID();
        String devID  = gev.getDeviceID();
        long   time   = gev.getTimestamp();
        int    stCode = gev.getStatusCode();
        if (StringTools.isBlank(acctID) || StringTools.isBlank(devID) || (time <= 0L) || (stCode <= 0)) {
            Print.logError("Invalid key specification");
            return null;
        }
        EventData.Key evKey = new EventData.Key(acctID, devID, time, stCode);
        
        /* fill record */
        EventData evdb = evKey.getDBRecord();
        for (String fldn : gev.getFieldKeys()) {
            Object val = gev.getFieldValue(fldn,null);
            evdb.setFieldValue(fldn, val);
        }
        
        /* return event */
        return evdb;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // [DB]WHERE ( <Condition...> )
    public static String getWhereClause(long autoIndex)
    {
        DBWhere dwh = new DBWhere(EventData.getFactory());
        dwh.append(dwh.EQ(EventData.FLD_autoIndex,autoIndex));
        return dwh.WHERE(dwh.toString());
    }
    
    // [DB]WHERE ( <Condition...> )
    public static String getWhereClause(
        String acctId, String devId,
        long timeStart, long timeEnd, 
        int statCode[], 
        boolean gpsRequired, 
        String andSelect)
    {
        DBFactory<EventData> dbFact = EventData.getFactory();
        DBWhere dwh = new DBWhere(EventData.getFactory());

        /* Account/Device */
        // ( (accountID='acct') AND (deviceID='dev') )
        if (!StringTools.isBlank(acctId)) {
            dwh.append(dwh.EQ(EventData.FLD_accountID, acctId));
            if (!StringTools.isBlank(devId) && !devId.equals("*")) {
                dwh.append(dwh.AND_(dwh.EQ(EventData.FLD_deviceID , devId)));
            }
        }

        /* status code(s) */
        // AND ( (statusCode=2) OR (statusCode=3) [OR ...] )
        if ((statCode != null) && (statCode.length > 0)) {
            dwh.append(dwh.AND_(dwh.INLIST(EventData.FLD_statusCode,statCode)));
        }

        /* gps required */
        if (gpsRequired) {
            // AND ( (latitude!=0) OR (longitude!=0) )
            // This section states that if both the latitude/longitude are '0',
            // then do not include the record in the select.
            if (!dbFact.hasField(EventData.FLD_cellLatitude)) {
                // no celltower location
                dwh.append(dwh.AND_(
                    dwh.OR(
                        dwh.NE(EventData.FLD_latitude     ,0L),
                        dwh.NE(EventData.FLD_longitude    ,0L)
                    )
                ));
            } else {
                // also check cell tower location
                dwh.append(dwh.AND_(
                    dwh.OR(
                        dwh.NE(EventData.FLD_latitude     ,0L),
                        dwh.NE(EventData.FLD_longitude    ,0L),
                        dwh.NE(EventData.FLD_cellLatitude ,0L),
                        dwh.NE(EventData.FLD_cellLongitude,0L)
                    )
                ));
            }
        }

        /* event time */
        if (timeStart >= 0L) {
            // AND (timestamp>=123436789)
            dwh.append(dwh.AND_(dwh.GE(EventData.FLD_timestamp,timeStart)));
        }
        if ((timeEnd >= 0L) && (timeEnd >= timeStart)) {
            // AND (timestamp<=123456789)
            dwh.append(dwh.AND_(dwh.LE(EventData.FLD_timestamp,timeEnd)));
        }
        
        /* additional selection */
        if (!StringTools.isBlank(andSelect)) {
            // AND ( ... )
            dwh.append(dwh.AND_(andSelect));
        }
        
        /* end of where */
        return dwh.WHERE(dwh.toString());
        
    }

    // ------------------------------------------------------------------------

    /* return the EventData record for the specified 'autoIndex' value */
    public static EventData getAutoIndexEvent(long autoIndex)
        throws DBException
    {
        DBFactory<EventData> dbFact = EventData.getFactory();

        /* has FLD_autoIndex? */
        if (!dbFact.hasField(EventData.FLD_autoIndex)) {
            return null;
        }
        
        /* create key */
        //DBFactory dbFact = EventData.getFactory();
        //DBRecordKey<EventData> evKey = dbFact.createKey();
        //evKey.setFieldValue(EventData.FLD_autoIndex, autoIndex);

        /* create selector */
        DBSelect<EventData> dsel = new DBSelect<EventData>(dbFact);
        dsel.setWhere(EventData.getWhereClause(autoIndex));

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            ed = DBRecord.select(dsel, null); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }

        /* return result */
        return !ListTools.isEmpty(ed)? ed[0] : null;

    }

    // ------------------------------------------------------------------------

    /* return the EventData record for the specified 'autoIndex' value */
    public static EventData[] getSelectedEvents(DBSelect<EventData> dsel, DBRecordHandler<EventData> rcdHandler)
        throws DBException
    {

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            ed = DBRecord.select(dsel, rcdHandler);
        } finally {
            DBProvider.unlockTables();
        }

        /* return result */
        return !ListTools.isEmpty(ed)? ed : null;

    }

    // ------------------------------------------------------------------------

    /* create range event selector */
    private static DBSelect<EventData> _createRangeEventSelector(
        String acctId, String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit, boolean ascending,
        String addtnlSelect)
    {

        /* invalid account/device */
        if (StringTools.isBlank(acctId)) {
            //Print.logWarn("No AccountID specified ...");
            return null;
        } else
        if (StringTools.isBlank(devId)) {
            //Print.logWarn("No DeviceID specified ...");
            return null;
        }

        /* invalid time range */
        if ((timeStart > 0L) && (timeEnd > 0L) && (timeStart > timeEnd)) {
            //Print.logWarn("Invalid time range specified ...");
            return null;
        }

        /* ascending/descending */
        boolean isAscending = ascending;
        if ((limit > 0L) && ((limitType == null) || EventData.LimitType.LAST.equals(limitType))) {
            // NOTE: records will be in descending order (will need to reorder)
            isAscending = false;
        }

        /* create/return DBSelect */
        // DBSelect: [SELECT * FROM EventData] <Where> ORDER BY <FLD_timestamp> [DESC] LIMIT <Limit>
        DBSelect<EventData> dsel = new DBSelect<EventData>(EventData.getFactory());
        dsel.setWhere(EventData.getWhereClause(
            acctId, devId,
            timeStart, timeEnd,
            statCode,
            validGPS,
            addtnlSelect));
        dsel.setOrderByFields(FLD_timestamp);
        dsel.setOrderAscending(isAscending);
        dsel.setLimit(limit);
        return dsel;
        
    }

    /* get a specific EventData record */
    public static EventData getEventData(
        String acctId, String devId,
        long timestamp, int statusCode)
        throws DBException
    {
        EventData ed[] = EventData.getRangeEvents(
            acctId, devId,
            timestamp, timestamp,
            new int[] { statusCode },
            false/*validGPS*/,
            EventData.LimitType.LAST, 1L/*limit*/, true/*ascending*/,
            null/*addtnlSelect*/,
            null/*rcdHandler*/);
        return (ed.length > 0)? ed[0] : null;
    }

    /* get range of EventData records (does not return null) */
    public static EventData[] getRangeEvents(
        String acctId, String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit, boolean ascending,
        String addtnlSelect)
        throws DBException
    {
        return EventData.getRangeEvents(
            acctId, devId, 
            timeStart, timeEnd,
            statCode,
            validGPS, 
            limitType, limit, ascending,
            addtnlSelect,
            null/*rcdHandler*/);
    }

    /* get range of EventData records (does not return null) */
    public static EventData[] getRangeEvents(
        String acctId, 
        String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit, boolean ascending,
        String addtnlSelect,
        DBRecordHandler<EventData> rcdHandler)
        throws DBException
    {

        /* get record selector */
        DBSelect<EventData> dsel = EventData._createRangeEventSelector(
            acctId, devId, 
            timeStart, timeEnd,
            statCode,
            validGPS, 
            limitType, limit, ascending,
            addtnlSelect);

        /* invalid arguments? */
        if (dsel == null) {
            return EMPTY_ARRAY;
        }

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //ed = (EventData[])DBRecord.select(EventData.getFactory(), dsel.toString(false), rcdHandler);
            ed = DBRecord.select(dsel, rcdHandler); // select:DBSelect
            // 'ed' _may_ be empty if (rcdHandler != null)
        } finally {
            DBProvider.unlockTables();
        }
        if (ed == null) {
            // no records
            return EMPTY_ARRAY;
        } else
        if (dsel.isOrderAscending() == ascending) {
            // records are in requested order, return as-is
            return ed;
        } else {
            // records are in descending order
            // reorder to ascending order
            int lastNdx = ed.length - 1;
            for (int i = 0; i < ed.length / 2; i++) {
                EventData edrcd = ed[i];
                ed[i] = ed[lastNdx - i];
                ed[lastNdx - i] = edrcd;
            }
            return ed;
        }

    }

    /* return count in range of EventData records */
    public static long countRangeEvents(
        String acctId, String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit,
        String addtnlSelect)
        throws DBException
    {
        
        /* get record selector */
        DBSelect<EventData> dsel = EventData._createRangeEventSelector(
            acctId, devId, 
            timeStart, timeEnd,
            statCode,
            validGPS, 
            limitType, limit, true,
            addtnlSelect);

        /* invalid arguements? */
        if (dsel == null) {
            return 0L;
        }

        /* count events */
        long recordCount = 0L;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            recordCount = DBRecord.getRecordCount(dsel);
        } finally {
            DBProvider.unlockTables();
        }
        return recordCount;

    }

    /** 
    *** Gets the number of EventData records for the specified Account/Device
    *** within the specified range.
    *** @param acctId     The Account ID
    *** @param devId      The Device ID
    *** @param timeStart  The starting time range (inclusive)
    *** @param timeEnd    The ending time range (inclusive)
    *** @return The number of records within the specified range
    **/
    public static long getRecordCount(
        String acctId, String devId,
        long timeStart, long timeEnd)
        throws DBException
    {
        StringBuffer wh = new StringBuffer();
        wh.append(EventData.getWhereClause(
            acctId, devId,
            timeStart, timeEnd,
            null  /*statCode[]*/ ,
            false /*gpsRequired*/,
            null  /*andSelect*/  ));
        return DBRecord.getRecordCount(EventData.getFactory(), wh);
    }

    // ------------------------------------------------------------------------

    /* get EventData records by "creationMillis" (does not return null) */
    public static EventData[] getEventsByCreationMillis(
        String acctId, 
        String devId,
        long createStartMS, long createEndMS,
        long limit)
        throws DBException
    {
        DBFactory<EventData> dbFact = EventData.getFactory();

        /* invalid account/device */
        if (StringTools.isBlank(acctId)) {
            return EMPTY_ARRAY;
        } else
        if (StringTools.isBlank(devId)) {
            return EMPTY_ARRAY;
        }

        /* invalid time range */
        if ((createStartMS > 0L) && (createEndMS > 0L) && (createStartMS > createEndMS)) {
            return EMPTY_ARRAY;
        }

        /* does "creationMillis" exist? */
        if (!dbFact.hasField(EventData.FLD_creationMillis)) {
            Print.logError("EventData table does not contain field '"+EventData.FLD_creationMillis+"'");
            return EMPTY_ARRAY;
        }

        /* create/return DBSelect */
        // DBSelect: [SELECT * FROM EventData] <Where> ORDER BY <FLD_creationMillis> LIMIT <Limit>
        DBSelect<EventData> dsel = new DBSelect<EventData>(dbFact);
        DBWhere dwh = new DBWhere(dbFact);
        dwh.append(dwh.EQ(EventData.FLD_accountID, acctId));
        if (!StringTools.isBlank(devId) && !devId.equals("*")) {
            dwh.append(dwh.AND_(dwh.EQ(EventData.FLD_deviceID, devId)));
        }
        if (createStartMS >= 0L) {
            // AND (creationMillis>=123436789000)
            dwh.append(dwh.AND_(dwh.GE(EventData.FLD_creationMillis,createStartMS)));
        }
        if ((createEndMS >= 0L) && (createEndMS >= createStartMS)) {
            // AND (creationMillis<=123456789000)
            dwh.append(dwh.AND_(dwh.LE(EventData.FLD_creationMillis,createEndMS)));
        }
        dsel.setWhere(dwh.WHERE(dwh.toString()));
        dsel.setOrderByFields(FLD_creationMillis,FLD_timestamp);
        dsel.setOrderAscending(true);
        dsel.setLimit(limit);

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            ed = DBRecord.select(dsel, null/*rcdHandler*/); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }
        if (ed == null) {
            // no records
            return EMPTY_ARRAY;
        } else {
            return ed;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Delete events which are in the future
    *** @param device      The Device record for which EventData records will be deleted
    *** @param futureTime  The time in the future after which events will be deleted.  
    ***                    This time must be more than 60 seconds beyond the current system clock time.
    *** @return The number of events deleted.
    **/
    public static long deleteFutureEvents(
        Device device,
        long futureTime)
        throws DBException
    {

        /* valid Device */
        if (device == null) {
            throw new DBException("Device not specified");
        }
        String acctID = device.getAccountID();
        String devID  = device.getDeviceID();

        /* delete future events */
        return EventData.deleteFutureEvents(acctID, devID, futureTime);

    }

    /**
    *** Delete events which are in the future
    *** @param acctID      The Account ID
    *** @param devID       The Device ID
    *** @param futureTime  The time in the future after which events will be deleted.  
    ***                    This time must be more than 60 seconds beyond the current system clock time.
    *** @return The number of events deleted.
    **/
    public static long deleteFutureEvents(
        String acctID, String devID,
        long futureTime)
        throws DBException
    {

        /* valid Device */
        if (StringTools.isBlank(acctID) || StringTools.isBlank(devID)) {
            throw new DBException("AccountID/DeviceID not specified");
        }

        /* validate specified time */
        // protection against deleting previous events
        long minTime = DateTime.getCurrentTimeSec() + 60L;
        if (futureTime <= minTime) {
            throw new DBException("Invalid future time specified");
        }

        /* delete */
        return EventData.deleteEventsAfterTimestamp(acctID, devID, futureTime, true);

    }

    /**
    *** Delete events which are after the specified timestamp (exclusive)
    *** @param acctID      The Account ID
    *** @param devID       The Device ID
    *** @param timestamp   The time after which all events will be deleted.  
    *** @param inclusive   True to include 'timestamp', false to exclude
    *** @return The number of events deleted.
    **/
    public static long deleteEventsAfterTimestamp(
        String acctID, String devID,
        long timestamp, boolean inclusive)
        throws DBException
    {
        long delFromTime = inclusive? timestamp : (timestamp + 1L);

        /* valid Device */
        if (StringTools.isBlank(acctID) || StringTools.isBlank(devID)) {
            throw new DBException("AccountID/DeviceID not specified");
        }

        /* count events in range */
        long count = EventData.getRecordCount(acctID,devID,delFromTime,-1L);
        if (count <= 0L) {
            // already empty range
            return 0L;
        }

        /* SQL statement */
        // DBDelete: DELETE FROM EventData WHERE ((accountID='acct) AND (deviceID='dev') AND (timestamp>delFromTime))
        DBDelete ddel = new DBDelete(EventData.getFactory());
        DBWhere dwh = ddel.createDBWhere();
        ddel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(EventData.FLD_accountID,acctID),
                dwh.EQ(EventData.FLD_deviceID ,devID),
                dwh.GE(EventData.FLD_timestamp,delFromTime)  // greater-than-or-equals-to
            )
        ));

        /* delete */
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(ddel.toString());
        } catch (SQLException sqe) {
            throw new DBException("Deleting future EventData records", sqe);
        } finally {
            DBConnection.release(dbc);
        }

        /* return count */
        return count;

    }

    // ------------------------------------------------------------------------

    /**
    *** Delete old events
    *** @param device      The Device record for which EventData records will be deleted
    *** @param oldTime     The time in the past before which events will be deleted.  
    *** @return The number of events deleted.
    **/
    public static long deleteOldEvents(
        Device device,
        long oldTime)
        throws DBException
    {

        /* valid Device */
        if (device == null) {
            throw new DBException("Device not specified");
        }
        String acctID = device.getAccountID();
        String devID  = device.getDeviceID();

        /* delete old events */
        return EventData.deleteOldEvents(acctID, devID, oldTime);
        
    }
    
    /**
    *** Delete events which are in the future
    *** @param acctID      The Account ID
    *** @param devID       The Device ID
    *** @param oldTime     The time in the past before which events will be deleted.  
    *** @return The number of events deleted.
    **/
    public static long deleteOldEvents(
        String acctID, String devID,
        long oldTime)
        throws DBException
    {

        /* valid Device */
        if (StringTools.isBlank(acctID) || StringTools.isBlank(devID)) {
            throw new DBException("AccountID/DeviceID not specified");
        }

        /* count events in range */
        long count = EventData.getRecordCount(acctID,devID,-1L,oldTime);
        if (count <= 0L) {
            // already empty range
            return 0L;
        }

        /* SQL statement */
        // DBDelete: DELETE FROM EventData WHERE ((accountID='acct) AND (deviceID='dev') AND (timestamp<oldTime))
        DBDelete ddel = new DBDelete(EventData.getFactory());
        DBWhere dwh = ddel.createDBWhere();
        ddel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(EventData.FLD_accountID,acctID),
                dwh.EQ(EventData.FLD_deviceID ,devID),
                dwh.LT(EventData.FLD_timestamp,oldTime)
            )
        ));

        /* delete */
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(ddel.toString());
        } catch (SQLException sqe) {
            throw new DBException("Deleting old EventData records", sqe);
        } finally {
            DBConnection.release(dbc);
        }

        /* return count */
        return count;

    }

    // ------------------------------------------------------------------------

    private static class GPSDistanceAccumulator
        implements DBRecordHandler<EventData>
    {
        private double accumKM = 0.0;
        private GeoPoint startingGP = null;
        private EventData lastEvent = null;
        public GPSDistanceAccumulator() {
        }
        public GPSDistanceAccumulator(GeoPoint startingGP, double startingOdomKM) {
            this();
            this.startingGP = startingGP;
            this.accumKM = startingOdomKM;
        }
        public int handleDBRecord(EventData rcd) throws DBException 
        {
            EventData ev = rcd;
            if (this.lastEvent != null) {
                GeoPoint lastGP = this.lastEvent.getGeoPoint();
                GeoPoint thisGP = ev.getGeoPoint();
                double distKM = lastGP.kilometersToPoint(thisGP);
                this.accumKM += distKM;
            } else
            if (this.startingGP != null) {
                GeoPoint thisGP = ev.getGeoPoint();
                double distKM = this.startingGP.kilometersToPoint(thisGP);
                this.accumKM += distKM;
            }
            this.lastEvent = ev;
            return DBRH_SKIP;
        }
        public void clearGPSDistanceTraveled() {
            this.accumKM = 0.0;
        }
        public double getGPSDistanceTraveledKM() {
            return this.accumKM;
        }
    }
    
    public static double getGPSDistanceTraveledKM(String acctId, String devId,
        long timeStart, long timeEnd,
        GeoPoint startingGP, double startingOdomKM)
    {
        
        /* record handler */
        GPSDistanceAccumulator rcdHandler = new GPSDistanceAccumulator(startingGP, startingOdomKM);
        
        /* look through events */
        try {
            EventData.getRangeEvents(
                acctId, devId,
                timeStart, timeEnd,
                null/*StatusCodes*/,
                true/*validGPS*/,
                EventData.LimitType.LAST, -1L/*limit*/, true/*ascending*/,
                null/*addtnlSelect*/,
                rcdHandler);
        } catch (DBException dbe) {
            Print.logException("Calculating GPS distance traveled", dbe);
        }

        /* return distance */
        return rcdHandler.getGPSDistanceTraveledKM();
        
    }
 
    // ------------------------------------------------------------------------

    public static DateTime parseDate(String dateStr, TimeZone tz)
    {
        // Formats:
        //   YYYY/MM[/DD[/hh[:mm[:ss]]]]
        //   eeeeeeeeeee
        String dateFld[] = StringTools.parseString(dateStr, "/:");
        if ((dateFld == null) || (dateFld.length == 0)) {
            return null; // no date specified
        } else
        if (dateFld.length == 1) {
            // parse as 'Epoch' time
            long epoch = StringTools.parseLong(dateFld[0], -1L);
            return (epoch > 0L)? new DateTime(epoch,tz) : null;
        } else {
            // (dateFld.length >= 2)
            int YY = StringTools.parseInt(dateFld[0], -1); // 1900..2007+
            int MM = StringTools.parseInt(dateFld[1], -1); // 1..12
            if ((YY < 1900) || (MM < 1) || (MM > 12)) {
                return null;
            } else {
                int DD = 1;
                int hh = 0, mm = 0, ss = 0;    // default to beginning of day
                if (dateFld.length >= 3) {
                    // at least YYYY/MM/DD provided
                    DD = StringTools.parseInt(dateFld[2], -1);
                    if (DD < 1) {
                        DD = 1;
                    } else
                    if (DD > DateTime.getDaysInMonth(tz,MM,YY)) {
                        DD = DateTime.getDaysInMonth(tz,MM,YY);
                    } else {
                        if (dateFld.length >= 4) { hh = StringTools.parseInt(dateFld[3], 0); }
                        if (dateFld.length >= 5) { mm = StringTools.parseInt(dateFld[4], 0); }
                        if (dateFld.length >= 6) { ss = StringTools.parseInt(dateFld[5], 0); }
                    }
                } else {
                    // only YYYY/MM provided
                    DD = 1; // first day of month
                }
                return new DateTime(tz, YY, MM, DD, hh, mm, ss);
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This section support a method for obtaining human readable information from
    // the EventData record for reporting, or email purposes. (currently this is
    // used by the 'rules' engine when generating notification emails).

    public  static final String KEY_ACCOUNT[]       = new String[] { "account"        , "accountDesc"    };  // "opendmtp"
    public  static final String KEY_DEVICE_COUNT[]  = new String[] { "deviceCount"    , "devCount"       };  // "123"
    public  static final String KEY_DEVICE[]        = new String[] { "device"         , "deviceDesc"     };  // "mobile"
    public  static final String KEY_EVENT_COUNT24[] = new String[] { "eventCount24"                      };  // "1017" (arg <statusCode>)
    public  static final String KEY_DEVICE_LINK[]   = new String[] { "deviceLink"     , "devLink"        };  // "<a href='http://...'>Description</a>"
    public  static final String KEY_DEV_CONN_AGE[]  = new String[] { "devConnectAge"  , "connectAge"     };  // "00:13:45"
    public  static final String KEY_DEV_TRAILERS[]  = new String[] { "deviceEntities" , "deviceTrailers" };  // "Trailer 1234, Trailer 4321"

    public  static final String KEY_DATETIME[]      = new String[] { "dateTime"       , "date"           };  // "2007/08/09 03:02:51 GMT"
    public  static final String KEY_DATE_YEAR[]     = new String[] { "dateYear"       , "year"           };  // "2007"
    public  static final String KEY_DATE_MONTH[]    = new String[] { "dateMonth"      , "month"          };  // "January"
    public  static final String KEY_DATE_DAY[]      = new String[] { "dateDay"        , "day"            };  // "23"
    public  static final String KEY_DATE_DOW[]      = new String[] { "dateDow"        , "dayOfWeek"      };  // "Monday"
    public  static final String KEY_TIME[]          = new String[] { "time"                              };  // "03:02:51"

    private static final String KEY_STATUSDESC[]    = new String[] { "status"                            };  // "Location"
    private static final String KEY_GEOPOINT[]      = new String[] { "geopoint"                          };  // "39.12345,-142.12345"
    private static final String KEY_LATITUDE[]      = new String[] { "latitude"                          };  // "39.12345"
    private static final String KEY_LONGITUDE[]     = new String[] { "longitude"                         };  // "-142.12345"
    private static final String KEY_GPS_AGE[]       = new String[] { "gpsAge"                            };  // "00:13:45"
    private static final String KEY_SPEED[]         = new String[] { "speed"                             };  // "34.9 mph"
    private static final String KEY_SPEED_LIMIT[]   = new String[] { "speedLimit"                        };  // "45.0 mph"
    private static final String KEY_DIRECTION[]     = new String[] { "direction"      , "compass"        };  // "SE"
    private static final String KEY_HEADING[]       = new String[] { "heading"        , "bearing", "course" };  // "123.4"
    private static final String KEY_ODOMETER[]      = new String[] { "odometer"                          };  // "1234 Miles"
    private static final String KEY_DISTANCE[]      = new String[] { "distance"                          };  // "1234 Miles"
    private static final String KEY_ALTITUDE[]      = new String[] { "alt"            , "altitude"       };  // "12345 feet"

    private static final String KEY_BATTERY_LEVEL[] = new String[] { "batteryLevel"                      };  // "0.75"
    private static final String KEY_FUEL_LEVEL[]    = new String[] { "fuelLevel"                         };  // "25.0 %"
    private static final String KEY_FUEL_VOLUME[]   = new String[] { "fuelLevelVolume", "fuelVolume"     };  // "12 gal"
    private static final String KEY_TIRE_TEMP[]     = new String[] { "tireTemperature", "tireTemp"       };  // "32.0,31.3,37.2,30.0 C"
    private static final String KEY_TIRE_PRESSURE[] = new String[] { "tirePressure"   , "tirePress"      };  // "32.0,31.3,37.2,30.0 psi"

    private static final String KEY_ADDRESS[]       = new String[] { "fullAddress"    , "address"        };  // "1234 Somewhere Lane, Smithsville, CA 99999"
    private static final String KEY_STREETADDR[]    = new String[] { "streetAddress"  , "street"         };  // "1234 Somewhere Lane"
    private static final String KEY_CITY[]          = new String[] { "city"                              };  // "Smithsville"
    private static final String KEY_STATE[]         = new String[] { "state"          , "province"       };  // "CA"
    private static final String KEY_POSTALCODE[]    = new String[] { "postalCode"     , "zipCode"        };  // "98765"
    private static final String KEY_SUBDIVISION[]   = new String[] { "subdivision"    , "subdiv"         };  // "US/CA"

    private static final String KEY_FAULT_CODE[]    = new String[] { "faultCode"                         };  // 
    private static final String KEY_FAULT_HEADER[]  = new String[] { "faultHeader"                       };  // 
    private static final String KEY_FAULT_DESC[]    = new String[] { "faultDesc"                         };  // 

    private static final String KEY_GEOZONEID[]     = new String[] { "geozoneID"                         };  // "home"
    private static final String KEY_GEOZONE[]       = new String[] { "geozone"                           };  // "Home Base"
    private static final String KEY_ENTITYID[]      = new String[] { "entityID"                          };  // "t1234"
    private static final String KEY_ENTITY[]        = new String[] { "entity"         , "entityDesc"     };  // "Trailer 1234"
    private static final String KEY_DRIVERID[]      = new String[] { "driverID"                          };  // "smith"
    private static final String KEY_DRIVER[]        = new String[] { "driver"         , "driverDesc"     };  // "Joe Smith"
    private static final String KEY_SERVICE_NOTES[] = new String[] { "serviceNotes"   ,                  };  // (Device field)
    private static final String KEY_MAPLINK[]       = new String[] { "maplink"        , "mapurl"         };  // "http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q=39.12345,-142.12345"

    private static final String KEY_ETA_DATETIME[]  = new String[] { "etaDateTime"                       };  // 
    private static final String KEY_ETA_UNIQUE_ID[] = new String[] { "etaUniqueID"    , "etaID"          };  // 
    private static final String KEY_ETA_DISTANCE[]  = new String[] { "etaDistanceKM"                     };  // 
    private static final String KEY_ETA_GEOPOINT[]  = new String[] { "etaGeoPoint"                       };  // 
    private static final String KEY_STOP_ID[]       = new String[] { "stopUniqueID"   , "stopID"         };  // 
    private static final String KEY_STOP_STATUS[]   = new String[] { "stopStatus"                        };  // 
    private static final String KEY_STOP_INDEX[]    = new String[] { "stopIndex"                         };  // 

    public static boolean _keyMatch(String key, String keyList[])
    {
        for (int i = 0; i < keyList.length; i++) {
            if (key.equalsIgnoreCase(keyList[i])) {
                return true;
            }
        }
        return false;
    }

    public String getFieldValueString(String key, String arg, BasicPrivateLabel bpl)
    {

        /* check for valid field name */
        if (key == null) {
            return null;
        }
        long now = DateTime.getCurrentTimeSec();
        Locale locale = (bpl != null)? bpl.getLocale() : null;

        /* Account/Device values */
        if (EventData._keyMatch(key,EventData.KEY_ACCOUNT)) {
            Account account = this.getAccount();
            return (account != null)? account.getDescription() : this.getAccountID();
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE_COUNT)) {
            Account account = this.getAccount();
            return (account != null)? String.valueOf(account.getDeviceCount()) : "?";
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE)) {
            Device device = this.getDevice();
            return (device != null)? device.getDescription() : this.getDeviceID();
        } else
        if (EventData._keyMatch(key,EventData.KEY_EVENT_COUNT24)) {
            String a[]       = StringTools.split(arg,',');
            int sinceHH      = (a.length > 1)? StringTools.parseInt(a[0],24) : 24;
            int statCodes[]  = ((a.length > 2) && !StringTools.isBlank(a[1]))? 
                new int[] { StringTools.parseInt(a[1],StatusCodes.STATUS_NONE) } : 
                null;
                long timeStart   = now - DateTime.HourSeconds((sinceHH > 0)? sinceHH : 24);
            long timeEnd     = -1L;
            long recordCount = -1L;
            try {
                recordCount = EventData.countRangeEvents(
                    this.getAccountID(), this.getDeviceID(),
                    timeStart, timeEnd,
                    statCodes,
                    false/*validGPS*/,
                    EventData.LimitType.LAST/*limitType*/, -1L/*limit*/, // no limit
                    null/*where*/);
            } catch (DBException dbe) {
                Print.logError("Unable to obtain EventData record count [" + dbe);
            }
            return String.valueOf(recordCount);
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE_LINK)) {
            Device device = this.getDevice();
            if (device != null) {
                String url = device.getLinkURL();
                String dsc = StringTools.blankDefault(device.getLinkDescription(),"Link");
                return "<a href='"+url+"' target='_blank'>"+dsc+"</a>";
            } else {
                return "";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEV_CONN_AGE)) {
            Device device = this.getDevice();
            if (device != null) {
                long lastConnectTime = device.getLastTotalConnectTime();
                if (lastConnectTime <= 0L) {
                    return "--:--:--";
                }
                long ageSec = DateTime.getCurrentTimeSec() - lastConnectTime;
                if (ageSec < 0L) { ageSec = 0L; }
                long hours  = (ageSec        ) / 3600L;
                long min    = (ageSec % 3600L) /   60L;
                long sec    = (ageSec %   60L);
                StringBuffer sb = new StringBuffer();
                sb.append(hours).append(":");
                if (min   < 10) { sb.append("0"); }
                sb.append(min  ).append(":");
                if (sec   < 10) { sb.append("0"); }
                sb.append(sec  );
                return sb.toString();
            } else {
                return "--:--:--";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEV_TRAILERS)) {
            Device device = this.getDevice();
            if (device != null) {
                String e[] = device.getAttachedEntityDescriptions(EntityManager.EntityType.TRAILER);
                if ((e != null) && (e.length > 0)) {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < e.length; i++) { 
                        if (i > 0) { sb.append(","); }
                        sb.append(e[i]);
                    }
                    return sb.toString();
                } else {
                    return "";
                }
            } else {
                return "";
            }
        }
        
        /* Date/Time values */
        if (EventData._keyMatch(key,EventData.KEY_DATETIME)) {
            return this.getTimestampString();
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_YEAR)) {
            return this.getTimestampYear();
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_MONTH)) {
            return this.getTimestampMonth(false, locale);
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_DAY)) {
            return this.getTimestampDayOfMonth();
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_DOW)) {
            return this.getTimestampDayOfWeek(false, locale);
        } else
        if (EventData._keyMatch(key,EventData.KEY_TIME)) {
            return this.getTimestampTime();
        }

        /* Event/GPS Values */
        if (EventData._keyMatch(key,EventData.KEY_STATUSDESC)) {
            return this.getStatusCodeDescription(bpl);
        } else
        if (EventData._keyMatch(key,EventData.KEY_GEOPOINT)) {
            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(this.getAccount());
            double lat = this.getLatitude();
            double lon = this.getLongitude();
            String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
            String latStr = GeoPoint.formatLatitude( lat, fmt, locale);
            String lonStr = GeoPoint.formatLongitude(lon, fmt, locale);
            return latStr + GeoPoint.PointSeparator + lonStr;
        } else
        if (EventData._keyMatch(key,EventData.KEY_LATITUDE)) {
            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(this.getAccount());
            double lat = this.getLatitude();
            String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
            return GeoPoint.formatLatitude(lat, fmt, locale);
        } else
        if (EventData._keyMatch(key,EventData.KEY_LONGITUDE)) {
            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(this.getAccount());
            double lon = this.getLongitude();
            String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
            return GeoPoint.formatLongitude(lon, fmt, locale);
        } else
        if (EventData._keyMatch(key,EventData.KEY_GPS_AGE)) {
            long ageSec = this.getGpsAge(); // gps fix age relative to the event timestamp
            if (ageSec < 0L) { ageSec = 0L; }
            long hours  = (ageSec        ) / 3600L;
            long min    = (ageSec % 3600L) /   60L;
            long sec    = (ageSec %   60L);
            StringBuffer sb = new StringBuffer();
            sb.append(hours).append(":");
            if (min   < 10) { sb.append("0"); }
            sb.append(min  ).append(":");
            if (sec   < 10) { sb.append("0"); }
            sb.append(sec  );
            return sb.toString();
        } else
        if (EventData._keyMatch(key,EventData.KEY_SPEED)) {
            double kph = this.getSpeedKPH();
            Account account = this.getAccount();
            if (account != null) {
                return account.getSpeedString(kph,true,locale);
            } else {
                return StringTools.format(kph,"0") + " " + Account.SpeedUnits.KPH.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_SPEED_LIMIT)) {
            double kph = this.getSpeedLimitKPH();
            Account account = this.getAccount();
            if (account != null) {
                return account.getSpeedString(kph,true,locale);
            } else {
                return StringTools.format(kph,"0") + " " + Account.SpeedUnits.KPH.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DIRECTION)) {
            return GeoPoint.GetHeadingString(this.getHeading(),locale);
        } else
        if (EventData._keyMatch(key,EventData.KEY_HEADING)) {
            return StringTools.format(this.getHeading(),"0.0");
        } else
        if (EventData._keyMatch(key,EventData.KEY_ODOMETER)) {
            double  odomKM  = this.getOdometerKM();
            Device  device  = this.getDevice();
            if (device != null) {
                odomKM += device.getOdometerOffsetKM(); // ok
            }
            Account account = this.getAccount();
            if (account != null) {
                return account.getDistanceString(odomKM, true, locale);
            } else {
                return StringTools.format(odomKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DISTANCE)) {
            double distKM = this.getDistanceKM();
            Account account = this.getAccount();
            if (account != null) {
                return account.getDistanceString(distKM, true, locale);
            } else {
                return StringTools.format(distKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ALTITUDE)) {
            return this.getAltitudeString(true, locale);
        }

        /* OBD, etc */
        if (EventData._keyMatch(key,EventData.KEY_BATTERY_LEVEL)) {
            //Device device = this.getDevice();
            //return (device != null)? ((device.getLastBatteryLevel()*100.0)+"%") : "";
            long pct = Math.round(this.getBatteryLevel() * 100.0);
            return pct + "%";
        } else
        if (EventData._keyMatch(key,EventData.KEY_FUEL_LEVEL)) {
            long pct = Math.round(this.getFuelLevel() * 100.0);
            return pct + "%";
        } else
        if (EventData._keyMatch(key,EventData.KEY_FUEL_VOLUME)) {
            Device device = this.getDevice();
            if (device == null) {
                return "";
            } else {
                Account.VolumeUnits vu = Account.getVolumeUnits(this.getAccount());
                double L = device.getFuelCapacity() * this.getFuelLevel();
                double V = vu.convertFromLiters(L);
                return StringTools.format(V,"0.0") + " " + vu.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_TIRE_TEMP)) {
            Account.TemperatureUnits tu = Account.getTemperatureUnits(this.getAccount());
            double T[] = this.getTireTemp_units(tu);
            StringBuffer sb = new StringBuffer();
            for (int t = 0; t < T.length; t++) {
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(T[t],"0.0"));
            }
            return sb + " " + tu.toString(locale);
        } else
        if (EventData._keyMatch(key,EventData.KEY_TIRE_PRESSURE)) {
            Account.PressureUnits pu = Account.getPressureUnits(this.getAccount());
            double P[] = this.getTirePressure_units(pu);
            StringBuffer sb = new StringBuffer();
            for (int p = 0; p < P.length; p++) {
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(P[p],"0.0"));
            }
            return sb + " " + pu.toString(locale);
        } 

        /* Address values */
        if (EventData._keyMatch(key,EventData.KEY_ADDRESS)) {
            return this.getAddress();
        } else
        if (EventData._keyMatch(key,EventData.KEY_STREETADDR)) {
            return this.getStreetAddress();
        } else
        if (EventData._keyMatch(key,EventData.KEY_CITY)) {
            return this.getCity();
        } else
        if (EventData._keyMatch(key,EventData.KEY_STATE)) {
            return this.getStateProvince();
        } else
        if (EventData._keyMatch(key,EventData.KEY_POSTALCODE)) {
            return this.getPostalCode();
        } else
        if (EventData._keyMatch(key,EventData.KEY_SUBDIVISION)) {
            return this.getSubdivision();
        } 

        /* OBD fault values */
        if (EventData._keyMatch(key,EventData.KEY_FAULT_CODE)) {
            long fault = this.getFieldValue(EventData.FLD_j1708Fault, 0L);
            return DTOBDFault.GetFaultString(fault);
        } else
        if (EventData._keyMatch(key,EventData.KEY_FAULT_HEADER)) {
            long fault = this.getFieldValue(EventData.FLD_j1708Fault, 0L);
            return DTOBDFault.GetFaultHeader(fault);
        } else
        if (EventData._keyMatch(key,EventData.KEY_FAULT_DESC)) {
            long fault = this.getFieldValue(EventData.FLD_j1708Fault, 0L);
            return DTOBDFault.GetFaultDescription(fault, locale);
        }

        /* Misc */
        if (EventData._keyMatch(key,EventData.KEY_GEOZONEID)) {
            return this.getGeozoneID();
        } else
        if (EventData._keyMatch(key,EventData.KEY_GEOZONE)) {
            return this.getGeozoneDescription();
        } else
        if (EventData._keyMatch(key,EventData.KEY_ENTITYID)) {
            return this.getEntityID();
        } else
        if (EventData._keyMatch(key,EventData.KEY_ENTITY)) {
            String aid  = this.getAccountID();
            String eid  = this.getEntityID();
            int    type = this.getEntityType();
            return Device.getEntityDescription(aid, eid, type);
        } else
        if (EventData._keyMatch(key,EventData.KEY_DRIVERID)) {
            return this.getDriverID();
        } else
        if (EventData._keyMatch(key,EventData.KEY_DRIVER)) {
            String did = this.getEntityID(); // getDriverID
            return did; // TODO: get driver description
        } else
        if (EventData._keyMatch(key,EventData.KEY_SERVICE_NOTES)) {
            Device device = this.getDevice();
            return (device != null)? device.getMaintNotes() : "";
        } else
        if (EventData._keyMatch(key,EventData.KEY_MAPLINK)) {
            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(this.getAccount());
            double lat = this.getLatitude();
            double lon = this.getLongitude();
            String fmt = "5";
            String latStr = GeoPoint.formatLatitude( lat, fmt, locale);
            String lonStr = GeoPoint.formatLongitude(lon, fmt, locale);
            StringBuffer url = new StringBuffer();
            url.append("http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q=");
            url.append(latStr).append(",").append(lonStr);
            return url.toString();
        }
        
        /* Garmin values */
        if (EventData._keyMatch(key,EventData.KEY_ETA_DATETIME)) {
            long    ts   = this.getEtaTimestamp();
            Account acct = this.getAccount();
            return EventData.getTimestampString(ts, acct, null);
        } else
        if (EventData._keyMatch(key,EventData.KEY_ETA_UNIQUE_ID)) {
            long id = this.getEtaUniqueID();
            return String.valueOf(id);
        } else
        if (EventData._keyMatch(key,EventData.KEY_ETA_DISTANCE)) {
            double distKM = this.getEtaDistanceKM();
            Account account = this.getAccount();
            if (account != null) {
                return account.getDistanceString(distKM, true, locale);
            } else {
                return StringTools.format(distKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ETA_GEOPOINT)) {
            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(this.getAccount());
            double lat = this.getEtaLatitude();
            double lon = this.getEtaLongitude();
            String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
            String latStr = GeoPoint.formatLatitude( lat, fmt, locale);
            String lonStr = GeoPoint.formatLongitude(lon, fmt, locale);
            return latStr + GeoPoint.PointSeparator + lonStr;
        } else
        if (EventData._keyMatch(key,EventData.KEY_STOP_ID)) {
            long id = this.getStopID();
            return String.valueOf(id);
        } else
        if (EventData._keyMatch(key,EventData.KEY_STOP_STATUS)) {
            int status = this.getStopStatus();
            return String.valueOf(status);
        } else
        if (EventData._keyMatch(key,EventData.KEY_STOP_INDEX)) {
            int ndx = this.getStopIndex();
            return String.valueOf(ndx);
        }

        /* EventData fields */
        String fldName = this.getFieldName(key); // this gets the field name with proper case
        DBField dbFld = (fldName != null)? this.getRecordKey().getField(fldName) : null;
        if (dbFld != null) {
            Object val = this.getFieldValue(fldName);
            if (val != null) {
                return dbFld.formatValue(val);
            } else {
                return dbFld.formatValue(dbFld.getDefaultValue());
            }
        }
        // EventData field not found

        /* try device */
        Device device = this.getDevice();
        if (device != null) {
            return device.getFieldValueString(key,arg,bpl);
        }

        /* try account */
        Account account = this.getAccount();
        if (account != null) {
            return account.getFieldValueString(key,arg,bpl);
        }

        /* not found */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* singleton instance of DeviceDescriptionComparator */
    private static Comparator<EventData> devDescComparator = null;
    public static Comparator<EventData> getDeviceDescriptionComparator()
    {
        if (devDescComparator == null) {
            devDescComparator = new DeviceDescriptionComparator(); // ascending
        }
        return devDescComparator;
    }
    
    /* Comparator optimized for EventData device description */
    public static class DeviceDescriptionComparator
        implements Comparator<EventData>
    {
        private boolean ascending = true;
        public DeviceDescriptionComparator() {
            this(true);
        }
        public DeviceDescriptionComparator(boolean ascending) {
            this.ascending  = ascending;
        }
        public int compare(EventData ev1, EventData ev2) {
            // assume we are comparing EventData records
            if (ev1 == ev2) {
                return 0; // exact same object (or both null)
            } else 
            if (ev1 == null) {
                return this.ascending? -1 :  1; // null < non-null
            } else
            if (ev2 == null) {
                return this.ascending?  1 : -1; // non-null > null
            } else {
                String D1 = ev1.getDeviceDescription().toLowerCase(); // ev1.getDeviceID();
                String D2 = ev2.getDeviceDescription().toLowerCase(); // ev2.getDeviceID();
                return this.ascending? D1.compareTo(D2) : D2.compareTo(D1);
            }
        }
        public boolean equals(Object other) {
            if (other instanceof DeviceDescriptionComparator) {
                DeviceDescriptionComparator ddc = (DeviceDescriptionComparator)other;
                return (this.ascending == ddc.ascending);
            }
            return false;
        }
    }

    /* generic field comparator */
    // Note: This comparator has not been tested yet
    public static class FieldComparator
        implements Comparator<EventData>
    {
        private boolean ascending = true;
        private String  fieldName = "";
        public FieldComparator(String fldName) {
            super();
            this.ascending = true;
            this.fieldName = (fldName != null)? fldName : "";
        }
        public int compare(EventData o1, EventData o2) {
            EventData ed1 = o1;
            EventData ed2 = o2;
            if (ed1 == ed2) {
                return 0;
            } else
            if (ed1 == null) {
                return this.ascending? -1 : 1;
            } else
            if (ed2 == null) {
                return this.ascending? 1 : -1;
            }
            Object v1 = ed1.getFieldValue(this.fieldName);
            Object v2 = ed2.getFieldValue(this.fieldName);
            if (v1 == v2) {
                return 0;
            } else
            if (v1 == null) {
                return this.ascending? -1 : 1;
            } else 
            if (v2 == null) {
                return this.ascending? 1 : -1;
            } else 
            if (v1.equals(v2)) {
                return 0;
            } else
            if ((v1 instanceof Number) && (v2 instanceof Number)) {
                double d = ((Number)v2).doubleValue() - ((Number)v1).doubleValue();
                if (d > 0.0) {
                    return this.ascending? 1 : -1;
                } else
                if (d < 0.0) {
                    return this.ascending? -1 : 1;
                } else {
                    return 0;
                }
            } else {
                String s1 = v1.toString();
                String s2 = v2.toString();
                return this.ascending? s1.compareTo(s2) : s2.compareTo(s1);
            }
        }
        public boolean equals(Object other) {
            if (other instanceof FieldComparator) {
                FieldComparator edc = (FieldComparator)other;
                if (this.ascending != edc.ascending) {
                    return false;
                } else
                if (!this.fieldName.equals(edc.fieldName)) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_ACCOUNT[]   = new String[] { "account", "a" };
    private static final String ARG_DEVICE[]    = new String[] { "device" , "d" };

    // DEBUG TESTING ONLY!!
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        String accountID = RTConfig.getString(ARG_ACCOUNT,"");
        String deviceID  = RTConfig.getString(ARG_DEVICE ,"");

        /* address length */
        String addrSizeKey = "db.typeSize.address";
        Print.logInfo(addrSizeKey + " = " + RTConfig.getInt(addrSizeKey,-1) + " [expected]");
        DBField addrFld = EventData.getFactory().getField(FLD_address);
        if (addrFld != null) {
            Print.logInfo("EventData address field length = " + addrFld.getStringLength() + " [found]");
        }

        /* EventData query */
        DBSelect<EventData> dsel = EventData._createRangeEventSelector(
            accountID, deviceID, 
            -1L, -1L,
            null,
            false, 
            EventData.LimitType.FIRST, 0L, true,
            null);
        try {
            DBRecordIterator<EventData> dbi = new DBRecordIterator<EventData>(dsel);
            for (int rc = 1; dbi.hasNext(); rc++) {
                EventData ed = dbi.next();
                StringBuffer sb = new StringBuffer();
                sb.append(rc).append(") ");
                sb.append(ed.getTimestamp()).append("  ");
                sb.append((new DateTime(ed.getTimestamp())).toString()).append("  ");
                sb.append(ed.getGeoPoint().toString());
                Print.sysPrintln(sb.toString());
            }
        } catch (DBException dbe) {
            Print.logException("", dbe);
        }

    }

}
