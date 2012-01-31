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
// Notes:
//  This 'Device' table currently assumes a 1-1 relationship between the device hardware
//  used to perform the tracking and communication, and the Vehicle being tracked.
//  However, it is possible to have more than one device on a given vehicle, or a single
//  hardware device may be moved between vehicles.  Ideally, this table should be split
//  into 2 separate tables: The Device table, and the MobileAsset table.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/09  Martin D. Flynn
//     -Integrate DBException
//  2006/05/23  Martin D. Flynn
//     -Changed column 'uniqueID' to a 'VARCHAR(40)'
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -Various new fields added
//  2007/03/25  Martin D. Flynn
//     -Added 'equipmentType', 'groupID'
//     -Moved to 'org.opengts.db.tables'
//  2007/04/15  Martin D. Flynn
//     -Added 'borderCrossing' column.
//  2007/06/30  Martin D. Flynn
//     -Added 'getFirstEvent', 'getLastEvent'
//  2007/07/14  Martin D. Flynn
//     -Added '-uniqueid' command-line option.
//  2007/07/27  Martin D. Flynn
//     -Added 'notifyAction' column
//  2007/08/09  Martin D. Flynn
//     -Renamed command-line option "uniqid" to "uniqueid"
//     -Set 'deviceExists' to true when creating a new device.
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//     -Added handlers for client device errors, diagnostics, and properties.
//     -Added device lookup for the specified unique-id.
//  2007/11/28  Martin D. Flynn
//     -Added columns 'lastBorderCrossTime', 'simPhoneNumber', 'lastInputState'.
//     -Added additional 'Entity' methods
//     -Added OpenDMTP 'CommandErrors' definition section.
//     -Added '-editall' command-line option to display all fields.
//  2007/12/13  Martin D. Flynn
//     -Added an EventData filter to check for invalid odometer values.
//  2007/01/10  Martin D. Flynn
//     -Added column 'notes', 'imeiNumber'
//     -Removed handlers for client device errors, diagnostics, and properties
//      (these handlers have been implemented in 'DeviceDBImpl.java')
//  2008/02/11  Martin D. Flynn
//     -Added columns 'FLD_deviceCode', 'FLD_vehicleID'
//  2008/03/12  Martin D. Flynn
//     -Added column 'FLD_notifyPriority'
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/05/20  Martin D. Flynn
//     -Fixed 'UniqueID" to again make it visible to the CLI record editor.
//  2008/06/20  Martin D. Flynn
//     -Added column 'FLD_notifyDescription'
//  2008/07/21  Martin D. Flynn
//     -Added column 'FLD_linkURL'
//  2008/08/24  Martin D. Flynn
//     -Added 'validGPS' argument to 'getRangeEvents' and 'getLatestEvents'
//  2008/09/01  Martin D. Flynn
//     -Added optional field list "FixedLocationFieldInfo"
//     -Added field/column "FLD_smsEmail"
//  2008/10/16  Martin D. Flynn
//     -Added FLD_lastPingTime, FLD_totalPingCount
//  2008/12/01  Martin D. Flynn
//     -Added FLD_linkDescription, FLD_pushpinID
//     -Added optional field list 'GeoCorridorFieldInfo'
//  2009/05/24  Martin D. Flynn
//     -Added FLD_pendingPingCommand, FLD_remotePortCurrent
//     -Added FLD_lastValidLatitude/Longitude to optimize Geozone calculations.
//     -Added FLS_lastOdometerKM to optimize GPS odometer calculations.
//  2009/06/01  Martin D. Flynn
//     -Increased background thread pool size/limit to 25.
//  2009/09/23  Martin D. Flynn
//     -Added support for ignoring/truncating events with future timestamps
//     -Added FLD_maxPingCount
//  2009/10/02  Martin D. Flynn
//     -Changed "getGeozoneTransition" to return an array of Geozone transitions,
//      fixing the case where 2 adjacent events occur in 2 different geozones.
//  2009/11/01  Martin D. Flynn
//     -Added FLD_expectAck, FLD_lastAckCommand, FLD_lastAckTime
//  2009/12/16  Martin D. Flynn
//     -Added command-line check for "Periodic Maintenance/Service Due" (-maintkm=email)
//  2010/01/29  Martin D. Flynn
//     -Added FLD_listenPortCurrent
//  2010/04/11  Martin D. Flynn
//     -Added FLD_dataKey, FLD_displayColor, FLD_licensePlate
//     -Added 'deleteEventDataPriorTo' to delete old historical EventData records.
//  2010/07/04  Martin D. Flynn
//     -Added FLD_expirationTime, FLD_maintIntervalKM1, FLD_maintOdometerKM1
//  2010/07/18  Martin D. Flynn
//     -Added FLD_lastBatteryLevel, FLD_fuelCapacity
//  2010/09/09  Martin D. Flynn
//     -Added "deleteOldEvents" option
//  2010/11/29  Martin D. Flynn
//     -Added FLD_lastFuelLevel
//     -Added configurable "maximum odometer km"
//  2011/01/28  Martin D. Flynn
//     -Added FLD_lastOilLevel
//  2011/03/08  Martin D. Flynn
//     -Added "getFieldValueString"
//     -Added alternate key "simphone" to field FLD_simPhoneNumber.
//     -Added "loadDeviceBySimPhoneNumber(...)"
//     -Added column FLD_speedLimitKPH
//  2011/04/01  Martin D. Flynn
//     -Added FuelManager module support (requires installed FuelManager)
//     -If "ALLOW_USE_EMAIL_WRAPPER" is false, "getNotifyUseWrapper()" returns false.
//  2011/05/13  Martin D. Flynn
//     -Change to invalid speed maximum checking.
//  2011/06/16  Martin D. Flynn
//     -"lastNotifyTime"/"lastNotifyCode" now only changed if modified.
//      (ie. removed from "DefaultUpdatedFieldsList")
//     -Added FLD_fuelEconomy (approximate fuel economy), FLD_lastEngineHours
//  2011/07/01  Martin D. Flynn
//     -Added "CheckNotifySelector()"
//     -Added FLD_lastValidHeading
//  2011/08/21  Martin D. Flynn
//     -Added convenience setting check for geozone arrive/depart auto-notify
//  2011/10/03  Martin D. Flynn
//     -Added FLD_parkedLatitude, FLD_parkedLongitude, FLD_parkedRadius, FLD_lastFuelTotal
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;

import org.opengts.geocoder.*;
import org.opengts.cellid.*;

import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.RuleFactory.NotifyAction;
import org.opengts.db.tables.Transport.Encodings;

/**
*** This class represents a tracked asset (ie. something that is being tracked).
*** Currently, this DBRecord also represents the tracking hardware device as well.
**/

public class Device // Asset
    extends DeviceRecord<Device>
    implements DataTransport
{

    // ------------------------------------------------------------------------

    private static int LogEventDataInsertion = Print.LOG_UNDEFINED;
    public static void SetLogEventDataInsertion(int logLevel)
    {
        if (logLevel >= Print.LOG_WARN) {
            Device.LogEventDataInsertion = logLevel;
        } else {
            Print.logWarn("'SetLogEventDataInsertion' ignoring excessive log level: " + 
                Print.getLogLevelString(logLevel) + " (using LOG_WARN instead)");
            Device.LogEventDataInsertion = Print.LOG_WARN;
        }
    }

    // ------------------------------------------------------------------------

    /* optimization for caching status code descriptions */
    public  static      boolean CACHE_STATUS_CODE_DESCRIPTIONS      = true;

    /* ReverseGeocodeProvider required on command-line "-insertGP" */
    public  static      boolean INSERT_REVERSEGEOCODE_REQUIRED      = false;

    /* allow Device record specified "notifyUseWrapper" value */
    public  static      boolean ALLOW_USE_EMAIL_WRAPPER             = false;

    // ------------------------------------------------------------------------

    public  static final Device EMPTY_ARRAY[]                       = new Device[0];

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String  OPTCOLS_NotificationFieldInfo       = "startupInit.Device.NotificationFieldInfo";
    public static final String  OPTCOLS_BorderCrossingFieldInfo     = "startupInit.Device.BorderCrossingFieldInfo";
    public static final String  OPTCOLS_LinkFieldInfo               = "startupInit.Device.LinkFieldInfo";
    public static final String  OPTCOLS_FixedLocationFieldInfo      = "startupInit.Device.FixedLocationFieldInfo";
    public static final String  OPTCOLS_GeoCorridorFieldInfo        = "startupInit.Device.GeoCorridorFieldInfo";
    public static final String  OPTCOLS_MaintOdometerFieldInfo      = "startupInit.Device.MaintOdometerFieldInfo";
    public static final String  OPTCOLS_WorkOrderInfo               = "startupInit.Device.WorkOrderInfo";
    public static final String  OPTCOLS_DataPushInfo                = "startupInit.Device.DataPushInfo";

    // ------------------------------------------------------------------------

    /* Event update background thread */
    private static final int BACKGROUND_THREAD_POOL_SIZE = 30;
    private static ThreadPool BackgroundThreadPool = new ThreadPool("DeviceEventUpdate", BACKGROUND_THREAD_POOL_SIZE);

    // ------------------------------------------------------------------------
    // new asset defaults

    private static final String NEW_DEVICE_NAME_                    = "New Device";

    // ------------------------------------------------------------------------

    private static final int    EXT_UPDATE_MASK                     = 0xFFFF;
    private static final int    EXT_UPDATE_NONE                     = 0x0000;
    private static final int    EXT_UPDATE_CELLGPS                  = 0x0001;
    private static final int    EXT_UPDATE_ADDRESS                  = 0x0002;
    private static final int    EXT_UPDATE_BORDER                   = 0x0004;
    private static final int    EXT_UPDATE_JMS                      = 0x0008;

    // ------------------------------------------------------------------------

    /* "Device" title (ie. "Taxi", "Tractor", "Vehicle", etc) */
    public static String[] GetTitles(Locale loc)
    {
        I18N i18n = I18N.getI18N(Device.class, loc);
        return new String[] {
            i18n.getString("Device.title.singular", "Vehicle"),
            i18n.getString("Device.title.plural"  , "Vehicles"),
        };
    }

    // ------------------------------------------------------------------------
    // border crossing flags (see 'borderCrossing' column)

    public enum BorderCrossingState implements EnumTools.StringLocale, EnumTools.IntValue {
        OFF         ( 0, I18N.getString(Device.class,"Device.boarderCrossing.off","off")),
        ON          ( 1, I18N.getString(Device.class,"Device.boarderCrossing.on" ,"on" ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        BorderCrossingState(int v, I18N.Text a)     { vv = v; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
    };

    // ------------------------------------------------------------------------
    // maximum reasonable odometer value for a vehicle

    // TODO: this should be device dependent
    public  static final double MAX_DEVICE_ODOM_KM          = 1000000.0 * GeoPoint.KILOMETERS_PER_MILE;
    
    /**
    *** Get configured maximum allowed odometer value
    *** @return Maximum configured allowed odometer value
    **/
    public static double GetMaximumOdometerKM()
    {
        return RTConfig.getDouble(DBConfig.PROP_Device_maximumOdometerKM, MAX_DEVICE_ODOM_KM);
    }

    // ------------------------------------------------------------------------
    // maximum reasonable odometer value for a vehicle

    // TODO: this should be device dependent
    private static final double MAX_DEVICE_RUNTIME_HOURS    = DateTime.DaySeconds(365*30)/3600.0;
    
    /**
    *** Get configured maximum allowed engine-hours value
    *** @return Maximum configured allowed engine-hours value
    **/
    public static double GetMaximumRuntimeHours()
    {
        return RTConfig.getDouble(DBConfig.PROP_Device_maximumRuntimeHours, MAX_DEVICE_RUNTIME_HOURS);
    }

    // ------------------------------------------------------------------------
    // check notify rule selector
    
    /* check device rule selector */
    private static final boolean CHECK_NOTIFY_SELECTOR      = true;

    /**
    *** True to test notify rule selector, false to ignore
    *** @return True to test notify rule selector, false to ignore
    **/
    public static boolean CheckNotifySelector()
    {
        return RTConfig.getBoolean(DBConfig.PROP_Device_checkNotifySelector, CHECK_NOTIFY_SELECTOR);
    }

    // ------------------------------------------------------------------------
    // CellTower Location API

    private static boolean                cellTower_initDefault = false;
    private static MobileLocationProvider cellTower_GetLocation = null;

    /* set the RuleFactory for event notification */
    public static void setCellTowerGetLocation(MobileLocationProvider ctgl)
    {
        Device.cellTower_initDefault = true;
        if (ctgl != null) {
            Device.cellTower_GetLocation = ctgl;
            Print.logDebug("Device CellTower.GetLocation installed: " + StringTools.className(ctgl));
        } else
        if (Device.cellTower_GetLocation != null) {
            Device.cellTower_GetLocation = null;
            Print.logDebug("Device CellTower.GetLocation removed.");
        }
    }

    /* return ture if a CellTower.GetLocation has been defined */
    public static boolean hasCellTowerGetLocation()
    {
        return (Device.cellTower_GetLocation != null);
    }

    /* get the MobileLocationProvider instance */
    public static MobileLocationProvider getMobileLocationProvider()
    {
        if (!Device.cellTower_initDefault) {
            Device.cellTower_initDefault = true;
            if (Device.cellTower_GetLocation == null) {
                Device.cellTower_GetLocation = null; // CellTower.GetDefaultCellTowerLocationInterface(); 
                // may still be null
            }
        }
        return Device.cellTower_GetLocation;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Rule factory

    private static RuleFactory ruleFactory = null;

    /* set the RuleFactory for event notification */
    public static void setRuleFactory(RuleFactory rf)
    {
        if (rf != null) {
            Device.ruleFactory = rf;
            Print.logDebug("Device RuleFactory installed: " + StringTools.className(rf));
        } else
        if (Device.ruleFactory != null) {
            Device.ruleFactory = null;
            Print.logDebug("Device RuleFactory removed.");
        }
    }

    /* return ture if a RuleFactory has been defined */
    public static boolean hasRuleFactory()
    {
        return (Device.ruleFactory != null);
    }

    /* return ture if the ENRE is present */
    public static boolean hasENRE()
    {
        if (Device.ruleFactory != null) {
            //return Device.ruleFactory.getName().equals("GTSRulesEngine");
            //return DBConfig.hasRulePackage();
            return OSTools.instanceOf(Device.ruleFactory, DBConfig.CLASS_RULE_EventRuleFactory);
        } else {
            return false;
        }
    }

    /* get the event notification RuleFactory */
    public static RuleFactory getRuleFactory()
    {
        return Device.ruleFactory;
    }

    // ------------------------------------------------------------------------
    // (Device) Session statistics

    private static SessionStatsFactory statsFactory = null;

    /* set the SessionStatsFactory */
    public static void setSessionStatsFactory(SessionStatsFactory rf)
    {
        if (rf != null) {
            Device.statsFactory = rf;
            Print.logDebug("Device SessionStatsFactory installed: " + StringTools.className(Device.statsFactory));
        } else
        if (Device.statsFactory != null) {
            Device.statsFactory = null;
            Print.logDebug("Device SessionStatsFactory removed.");
        }
    }

    /* return ture if a SessionStatsFactory has been defined */
    public static boolean hasSessionStatsFactory()
    {
        return (Device.statsFactory != null);
    }

    /* get the event notification SessionStatsFactory */
    public static SessionStatsFactory getSessionStatsFactory()
    {
        return Device.statsFactory;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Entity manager

    private static EntityManager entityManager = null;

    /* set the connect/disconnect EntityManager */
    public static void setEntityManager(EntityManager ef)
    {
        if (ef != null) {
            Device.entityManager = ef;
            //Print.logDebug("Device EntityManager installed: " + StringTools.className(Device.entityManager));
        } else
        if (Device.entityManager != null) {
            Device.entityManager = null;
            //Print.logDebug("Device EntityManager removed.");
        }
    }

    /* return true if an EntityManager has been defined */
    public static boolean hasEntityManager()
    {
        return (Device.entityManager != null);
    }

    /* return the EntityManager (or null if not defined) */
    public static EntityManager getEntityManager()
    {
        return Device.entityManager;
    }

    public static String getEntityDescription(String accountID, String entityID, int entityType)
    {
        String eid = StringTools.trim(entityID);
        if (!eid.equals("") && Device.hasEntityManager()) {
            eid = Device.getEntityManager().getEntityDescription(accountID, eid, entityType);
        }
        return eid;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Fuel manager

    private static FuelManager fuelManager = null;

    /* set the FuelManager */
    public static void setFuelManager(FuelManager fm)
    {
        if (fm != null) {
            Device.fuelManager = fm;
            //Print.logDebug("Device FuelManager installed: " + StringTools.className(Device.fuelManager));
        } else
        if (Device.fuelManager != null) {
            Device.fuelManager = null;
            //Print.logDebug("Device FuelManager removed.");
        }
    }

    /* return true if an FuelManager has been defined */
    public static boolean hasFuelManager()
    {
        return (Device.fuelManager != null);
    }

    /* return the FuelManager (or null if not defined) */
    public static FuelManager getFuelManager()
    {
        return Device.fuelManager;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) "Ping" dispatcher

    private static PingDispatcher pingDispatcher = null;

    /* set the PingDispatcher */
    public static void setPingDispatcher(PingDispatcher pd)
    {
        if (pd != null) {
            Device.pingDispatcher = pd;
            Print.logDebug("Device PingDispatcher installed: " + StringTools.className(Device.pingDispatcher));
        } else
        if (Device.pingDispatcher != null) {
            Device.pingDispatcher = null;
            Print.logDebug("Device PingDispatcher removed.");
        }
    }

    /* return true if an PingDispatcher has been defined */
    public static boolean hasPingDispatcher()
    {
        return (Device.pingDispatcher != null);
    }

    /* return the PingDispatcher (or null if not defined) */
    public static PingDispatcher getPingDispatcher()
    {
        return Device.pingDispatcher;
    }

    // ------------------------------------------------------------------------
    // Future EventDate timestamp check

    public static final int FUTURE_DATE_UNDEFINED   = -999;
    public static final int FUTURE_DATE_IGNORE      = -1;
    public static final int FUTURE_DATE_DISABLED    = 0;
    public static final int FUTURE_DATE_TRUNCATE    = 1;

    private static int  FutureEventDateAction = FUTURE_DATE_UNDEFINED;
    public static int futureEventDateAction()
    {
        // TODO: synchronize?
        if (FutureEventDateAction == FUTURE_DATE_UNDEFINED) {
            // "Device.futureDate.action="
            String act = RTConfig.getString(DBConfig.PROP_Device_futureDate_action,"");
            if (act.equalsIgnoreCase("ignore")   ||
                act.equalsIgnoreCase("skip")     ||
                act.equalsIgnoreCase("-1")         ) {
                FutureEventDateAction = FUTURE_DATE_IGNORE;
            } else
            if (act.equalsIgnoreCase("truncate") ||
                act.equalsIgnoreCase("1")          ) {
                FutureEventDateAction = FUTURE_DATE_TRUNCATE;
            } else
            if (StringTools.isBlank(act)         ||
                act.equalsIgnoreCase("disabled") ||
                act.equalsIgnoreCase("disable")  ||
                act.equalsIgnoreCase("0")          ) {
                FutureEventDateAction = FUTURE_DATE_DISABLED;
            } else {
                Print.logError("Invalid property value %s => %s", DBConfig.PROP_Device_futureDate_action, act);
                FutureEventDateAction = FUTURE_DATE_DISABLED;
            }
        }
        return FutureEventDateAction;
    }

    private static long FutureEventDateMaxSec = -999L;
    public static long futureEventDateMaximumSec()
    {
        // TODO: synchronize?
        if (FutureEventDateMaxSec == -999L) {
            FutureEventDateMaxSec = RTConfig.getLong(DBConfig.PROP_Device_futureDate_maximumSec,0L);
        }
        return FutureEventDateMaxSec;
    }

    // ------------------------------------------------------------------------
    // Invalid speed check

    public static final int INVALID_SPEED_UNDEFINED   = -999;
    public static final int INVALID_SPEED_IGNORE      = -1;
    public static final int INVALID_SPEED_DISABLED    = 0;
    public static final int INVALID_SPEED_TRUNCATE    = 1;

    private static int  InvalidSpeedAction = INVALID_SPEED_UNDEFINED;
    public static int invalidSpeedAction()
    {
        // TODO: synchronize?
        if (InvalidSpeedAction == INVALID_SPEED_UNDEFINED) {
            // "Device.invalidSpeed.action="
            String act = RTConfig.getString(DBConfig.PROP_Device_invalidSpeed_action,"");
            if (act.equalsIgnoreCase("ignore")   ||
                act.equalsIgnoreCase("skip")     ||
                act.equalsIgnoreCase("-1")         ) {
                // events with invalid speed will be ignored
                InvalidSpeedAction = INVALID_SPEED_IGNORE;
            } else
            if (act.equalsIgnoreCase("truncate") ||
                act.equalsIgnoreCase("1")          ) {
                // event speeds exceeding the max allowed speed will be set to the max allowed speed
                InvalidSpeedAction = INVALID_SPEED_TRUNCATE;
            } else
            if (StringTools.isBlank(act)         ||
                act.equalsIgnoreCase("disabled") ||
                act.equalsIgnoreCase("disable")  ||
                act.equalsIgnoreCase("0")          ) {
                // maximum allowed speed will not be checked
                InvalidSpeedAction = INVALID_SPEED_DISABLED;
            } else {
                Print.logError("Invalid property value %s => %s", DBConfig.PROP_Device_invalidSpeed_action, act);
                InvalidSpeedAction = INVALID_SPEED_DISABLED;
            }
        }
        return InvalidSpeedAction;
    }

    private static double InvalidSpeedMaxKPH = -999.0;
    public static double invalidSpeedMaximumKPH()
    {
        // TODO: synchronize?
        if (InvalidSpeedMaxKPH <= -999.0) {
            String spdMaxProp = DBConfig.PROP_Device_invalidSpeed_maximumKPH;
            InvalidSpeedMaxKPH = RTConfig.getDouble(spdMaxProp, 0.0);
            if (InvalidSpeedMaxKPH <= 0.0) {
                // essentially "disabled"
                InvalidSpeedMaxKPH = 0.0;
            } else
            if (InvalidSpeedMaxKPH <= 100.0) {
                // a low maximum speed warning
                Print.logWarn("**** \""+spdMaxProp+"\" set to " + InvalidSpeedMaxKPH + " km/h ****");
            }
        }
        return InvalidSpeedMaxKPH;
    }

    // ------------------------------------------------------------------------
    // DCS Properties ID
    
    public static String GetDcsPropertiesID(Device device)
    {

        /* no device */
        if (device == null) {
            return "";
        }

        /* Device defined? */
        String dcsPropsID = device.getDcsPropertiesID();
        if (!StringTools.isBlank(dcsPropsID)) {
            return dcsPropsID;
        }

        /* Account defined? */
        Account account = device.getAccount();
        if (account != null) {
            dcsPropsID = account.getDcsPropertiesID();
            if (!StringTools.isBlank(dcsPropsID)) {
                return dcsPropsID;
            }
        }

        /* not defined */
        return "";

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* keyed FLD_simPhoneNumber? */
    private static String _simPhoneNumber_attr()
    {
        // FLD_simPhoneNumber
        String commonAttr = "edit=2";
        if (RTConfig.getBoolean(DBConfig.PROP_Device_keyedSimPhoneNumber,false)) {
            return commonAttr + " altkey=simphone";
        } else {
            return commonAttr;
        }
    }

    /* keyed FLD_lastNotifyTime? */
    private static String _lastNotifyTime_attr()
    {
        // FLD_lastNotifyTime
        String commonAttr = "format=time";
        if (RTConfig.getBoolean(DBConfig.PROP_Device_keyedLastNotifyTime,false)) {
            return commonAttr + " altkey=notifyTime";
        } else {
            return commonAttr;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below
    // Note: The following fields should be updated upon each connection from the client device:
    //  - FLD_lastInputState
    //  - FLD_ipAddressCurrent
    //  - FLD_remotePortCurrent
    //  - FLD_lastTotalConnectTime
    //  - FLD_lastDuplexConnectTime (OpenDMTP clients, otherwise optional)
    //  - FLD_totalProfileMask (OpenDMTP clients)
    //  - FLD_duplexProfileMask (OpenDMTP clients)

    /* table name */
    public static final String _TABLE_NAME               = "Device"; // "Asset"
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    // Device/Asset specific information:
    public static final String FLD_groupID               = "groupID";               // vehicle group (user informational only)
    public static final String FLD_equipmentType         = "equipmentType";         // equipment/vehicle type
    public static final String FLD_vehicleID             = "vehicleID";             // vehicle id number (ie VIN)
    public static final String FLD_licensePlate          = "licensePlate";          // licensePlate / registration id
    public static final String FLD_driverID              = "driverID";              // driver id
    public static final String FLD_fuelCapacity          = "fuelCapacity";          // fuel capacity liters
    public static final String FLD_fuelEconomy           = "fuelEconomy";           // approximate fuel economy km/L
    public static final String FLD_speedLimitKPH         = "speedLimitKPH";         // Maximum speed km/h
    public static final String FLD_expirationTime        = "expirationTime";        // expiration time
    // DataTransport specific attributes (see also Transport.java)
    // (These fields contain the default DataTransport attributes)
    public static final String FLD_uniqueID              = "uniqueID";              // unique device ID
    public static final String FLD_deviceCode            = "deviceCode";            // DCServerConfig ID ("serverID")
    public static final String FLD_deviceType            = "deviceType";            // reserved
    public static final String FLD_dcsPropertiesID       = "dcsPropertiesID";
    public static final String FLD_pushpinID             = "pushpinID";             // map pushpin ID
    public static final String FLD_displayColor          = "displayColor";          // display color (maps, reports, etc).
    public static final String FLD_serialNumber          = "serialNumber";          // device hardware serial#.
    public static final String FLD_simPhoneNumber        = "simPhoneNumber";        // SIM phone number
    public static final String FLD_smsEmail              = "smsEmail";              // SMS email address (to the device itself)
  //public static final String FLD_smsGatewayProps       = "smsGatewayProps";       // SMS gateway properties
    public static final String FLD_imeiNumber            = "imeiNumber";            // IMEI number (or moblie ID)
    public static final String FLD_dataKey               = "dataKey";               // Data key
    public static final String FLD_ignitionIndex         = "ignitionIndex";         // hardware ignition I/O index
    public static final String FLD_codeVersion           = "codeVersion";           // code version installed on device
    public static final String FLD_featureSet            = "featureSet";            // device features
    public static final String FLD_ipAddressValid        = "ipAddressValid";        // valid IP address block
    // Last Device IP Address:Port
    public static final String FLD_ipAddressCurrent      = "ipAddressCurrent";      // current(last) IP address
    public static final String FLD_remotePortCurrent     = "remotePortCurrent";     // current(last) remote port
    public static final String FLD_listenPortCurrent     = "listenPortCurrent";     // current(last) local/listen port
    // Ping/Command
    public static final String FLD_pingCommandURI        = "pingCommandURI";        // ping command URL
    public static final String FLD_pendingPingCommand    = "pendingPingCommand";    // pending ping command (should just be 'pendingCommand')
    public static final String FLD_lastPingTime          = "lastPingTime";          // last ping time
    public static final String FLD_totalPingCount        = "totalPingCount";        // total ping count
    public static final String FLD_maxPingCount          = "maxPingCount";          // maximum allowed ping count
    public static final String FLD_expectAck             = "expectAck";             // expecting a returned ACK
    public static final String FLD_lastAckCommand        = "lastAckCommand";        // last command expecting an ACK
    public static final String FLD_lastAckTime           = "lastAckTime";           // last received ACK time
    // Device Communication Server Configuration
    public static final String FLD_dcsConfigMask         = "dcsConfigMask";         // DCS Config Mask
    public static final String FLD_dcsConfigString       = "dcsConfigString";       // DCS Config String
    // DMTP
    public static final String FLD_supportsDMTP          = "supportsDMTP";          // DMTP
    public static final String FLD_supportedEncodings    = "supportedEncodings";    // DMTP
    public static final String FLD_unitLimitInterval     = "unitLimitInterval";     // DMTP
    public static final String FLD_maxAllowedEvents      = "maxAllowedEvents";      // DMTP
    public static final String FLD_totalProfileMask      = "totalProfileMask";      // DMTP
    public static final String FLD_totalMaxConn          = "totalMaxConn";          // DMTP
    public static final String FLD_totalMaxConnPerMin    = "totalMaxConnPerMin";    // DMTP
    public static final String FLD_duplexProfileMask     = "duplexProfileMask";     // DMTP
    public static final String FLD_duplexMaxConn         = "duplexMaxConn";         // DMTP
    public static final String FLD_duplexMaxConnPerMin   = "duplexMaxConnPerMin";   // DMTP
    // Last Event
    public static final String FLD_lastTotalConnectTime  = "lastTotalConnectTime";  // last connect time
    public static final String FLD_lastDuplexConnectTime = "lastDuplexConnectTime"; // last TCP connect time
    public static final String FLD_lastInputState        = "lastInputState";        // last known digital input state
    public static final String FLD_lastBatteryLevel      = "lastBatteryLevel";      // last known battery level (%)
    public static final String FLD_lastFuelLevel         = "lastFuelLevel";         // last fuelLevel value
    public static final String FLD_lastFuelTotal         = "lastFuelTotal";         // last fuelTotal value
    public static final String FLD_lastOilLevel          = "lastOilLevel";          // last oilLevel value
    public static final String FLD_lastValidLatitude     = "lastValidLatitude";     // last known valid latitude
    public static final String FLD_lastValidLongitude    = "lastValidLongitude";    // last known valid longitude
    public static final String FLD_lastValidHeading      = "lastValidHeading";      // last known valid heading
    public static final String FLD_lastGPSTimestamp      = "lastGPSTimestamp";      // timestamp of last valid GPS Location
    public static final String FLD_lastCellServingInfo   = "lastCellServingInfo";   // last Serving CellTower info
    public static final String FLD_lastOdometerKM        = "lastOdometerKM";        // last odometer value (may be simulated)
    public static final String FLD_odometerOffsetKM      = "odometerOffsetKM";      // offset to reported odometer
    public static final String FLD_lastEngineHours       = "lastEngineHours";       // last engine-hours value (may be simulated)
    //
    private static DBField FieldInfo[] = {
        // Asset/Vehicle specific fields
        newField_accountID(true),
        newField_deviceID(true),
        new DBField(FLD_groupID              , String.class        , DBField.TYPE_GROUP_ID()  , I18N.getString(Device.class,"Device.fld.groupID"              , "Group ID"                    ), "edit=2"),
        new DBField(FLD_equipmentType        , String.class        , DBField.TYPE_STRING(40)  , I18N.getString(Device.class,"Device.fld.equipmentType"        , "Equipment Type"              ), "edit=2"),
        new DBField(FLD_vehicleID            , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.vehicleID"            , "VIN"                         ), "edit=2"),
        new DBField(FLD_licensePlate         , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.licensePlate"         , "License Plate"               ), "edit=2"),
        new DBField(FLD_driverID             , String.class        , DBField.TYPE_DRIVER_ID() , I18N.getString(Device.class,"Device.fld.driverID"             , "Driver ID"                   ), "edit=2"),
        new DBField(FLD_fuelCapacity         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fuelCapacity"         , "Fuel Capacity"               ), "edit=2 format=#0.0"),
        new DBField(FLD_fuelEconomy          , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fuelEconomy"          , "Approx. Fuel Economy"        ), "edit=2 format=#0.0"),
        new DBField(FLD_speedLimitKPH        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.speedLimitKPH"        , "Max Speed km/h"              ), "edit=2 format=#0.0"),
        new DBField(FLD_expirationTime       , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.expirationTime"       , "Expiration Time"             ), "edit=2 format=time"),
        // DataTransport fields (These fields contain the default DataTransport attributes)
        new DBField(FLD_uniqueID             , String.class        , DBField.TYPE_UNIQ_ID()   , I18N.getString(Device.class,"Device.fld.uniqueID"             , "Unique ID"                   ), "edit=2 altkey=true presep"),
        new DBField(FLD_deviceCode           , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.deviceCode"           , "Server ID"                   ), "edit=2"),
        new DBField(FLD_deviceType           , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.deviceType"           , "Device Type"                 ), "edit=2"),
        new DBField(FLD_dcsPropertiesID      , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.dcsPropertiesID"      , "DCS Properties ID"           ), "edit=2"),
        new DBField(FLD_pushpinID            , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.pushpinID"            , "Pushpin ID"                  ), "edit=2"),
        new DBField(FLD_displayColor         , String.class        , DBField.TYPE_STRING(16)  , I18N.getString(Device.class,"Device.fld.displayColor"         , "Display Color"               ), "edit=2"),
        new DBField(FLD_serialNumber         , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.serialNumber"         , "Serial Number"               ), "edit=2"),
        new DBField(FLD_simPhoneNumber       , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.simPhoneNumber"       , "SIM Phone Number"            ), Device._simPhoneNumber_attr()),
        new DBField(FLD_smsEmail             , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.smsEmail"             , "SMS EMail Address"           ), "edit=2"),
        new DBField(FLD_imeiNumber           , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.imeiNumber"           , "IMEI Number"                 ), "edit=2"),
        new DBField(FLD_dataKey              , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.dataKey"              , "Data Key"                    ), "edit=2"),
        new DBField(FLD_ignitionIndex        , Integer.TYPE        , DBField.TYPE_INT16       , I18N.getString(Device.class,"Device.fld.ignitionIndex"        , "Ignition I/O Index"          ), "edit=2"),
        new DBField(FLD_codeVersion          , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.codeVersion"          , "Code Version"                ), ""),
        new DBField(FLD_featureSet           , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.featureSet"           , "Feature Set"                 ), ""),
        new DBField(FLD_ipAddressValid       , DTIPAddrList.class  , DBField.TYPE_STRING(128) , I18N.getString(Device.class,"Device.fld.ipAddressValid"       , "Valid IP Addresses"          ), "edit=2"),
        new DBField(FLD_lastTotalConnectTime , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastTotalConnectTime" , "Last Total Connect Time"     ), "format=time"),
        new DBField(FLD_lastDuplexConnectTime, Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastDuplexConnectTime", "Last Duplex Connect Time"    ), "format=time"),
        // Ping/Command
      //new DBField(FLD_pingCommandURI       , String.class        , DBField.TYPE_STRING(128) , I18N.getString(Device.class,"Device.fld.pingCommandURI"  , "Ping Command URL"            , "edit=2"),
        new DBField(FLD_pendingPingCommand   , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.pendingPingCommand"   , "Pending Ping Command"        ), "edit=2"),
        new DBField(FLD_lastPingTime         , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastPingTime"         , "Last 'Ping' Time"            ), "format=time"),
        new DBField(FLD_totalPingCount       , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.totalPingCount"       , "Total 'Ping' Count"          ), ""),
        new DBField(FLD_maxPingCount         , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.maxPingCount"         , "Maximum 'Ping' Count"        ), "edit=2"),
        new DBField(FLD_expectAck            , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.expectAck"            , "Expecting an ACK"            ), "edit=2"),
        new DBField(FLD_lastAckCommand       , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.lastAckCommand"       , "Last Command Expecting ACK"  ), ""),
        new DBField(FLD_lastAckTime          , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastAckTime"          , "Last Received 'ACK' Time"    ), "format=time"),
        // Device Communication Server Configuration
        new DBField(FLD_dcsConfigMask        , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.dcsConfigMask"        , "DCS Configuration Mask"      ), "edit=2"),
      //new DBField(FLD_dcsConfigString      , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.dcsConfigString"      , "DCS Configuration String"    ), "edit=2"),
        // DMTP
        new DBField(FLD_supportsDMTP         , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.supportsDMTP"         , "Supports DMTP"               ), "edit=2"),
        new DBField(FLD_supportedEncodings   , Integer.TYPE        , DBField.TYPE_UINT8       , I18N.getString(Device.class,"Device.fld.supportedEncodings"   , "Supported Encodings"         ), "edit=2 format=X1 editor=encodings mask=Transport$Encodings"),
        new DBField(FLD_unitLimitInterval    , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.unitLimitInterval"    , "Accounting Time Interval Min"), "edit=2"),
        new DBField(FLD_maxAllowedEvents     , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.maxAllowedEvents"     , "Max Events per Interval"     ), "edit=2"),
        new DBField(FLD_totalProfileMask     , DTProfileMask.class , DBField.TYPE_BLOB        , I18N.getString(Device.class,"Device.fld.totalProfileMask"     , "Total Profile Mask"          ), ""),
        new DBField(FLD_totalMaxConn         , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.totalMaxConn"         , "Max Total Conn per Interval" ), "edit=2"),
        new DBField(FLD_totalMaxConnPerMin   , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.totalMaxConnPerMin"   , "Max Total Conn per Minute"   ), "edit=2"),
        new DBField(FLD_duplexProfileMask    , DTProfileMask.class , DBField.TYPE_BLOB        , I18N.getString(Device.class,"Device.fld.duplexProfileMask"    , "Duplex Profile Mask"         ), ""),
        new DBField(FLD_duplexMaxConn        , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.duplexMaxConn"        , "Max Duplex Conn per Interval"), "edit=2"),
        new DBField(FLD_duplexMaxConnPerMin  , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.duplexMaxConnPerMin"  , "Max Duplex Conn per Minute"  ), "edit=2"),
        // Last Event
        new DBField(FLD_ipAddressCurrent     , DTIPAddress.class   , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.ipAddressCurrent"     , "Current IP Address"          ), ""),
        new DBField(FLD_remotePortCurrent    , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.remotePortCurrent"    , "Current Remote Port"         ), ""),
        new DBField(FLD_listenPortCurrent    , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.listenPortCurrent"    , "Current Listen Port"         ), ""),
        new DBField(FLD_lastInputState       , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastInputState"       , "Last Input State"            ), ""),
        new DBField(FLD_lastBatteryLevel     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastBatteryLevel"     , "Last Battery Level"          ), "format=#0.0 units=percent"),
        new DBField(FLD_lastFuelLevel        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastFuelLevel"        , "Last Fuel Level"             ), "format=#0.0 units=percent"),
        new DBField(FLD_lastFuelTotal        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastFuelTotal"        , "Last Fuel Total Liters"      ), "format=#0.0 units=volume"),
        new DBField(FLD_lastOilLevel         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastOilLevel"         , "Last Oil Level"              ), "format=#0.0"),
        new DBField(FLD_lastValidLatitude    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastValidLatitude"    , "Last Valid Latitude"         ), "format=#0.00000"),
        new DBField(FLD_lastValidLongitude   , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastValidLongitude"   , "Last Valid Longitude"        ), "format=#0.00000"),
        new DBField(FLD_lastValidHeading     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastValidHeading"     , "Last Valid Heading"          ), "format=#0.00000"),
        new DBField(FLD_lastGPSTimestamp     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastGPSTimestamp"     , "Last Valid GPS Timestamp"    ), "format=time"),
        new DBField(FLD_lastCellServingInfo  , String.class        , DBField.TYPE_STRING(100) , I18N.getString(Device.class,"Device.fld.lastCellServingInfo"  , "Last Serving Cell Info"      ), ""),
        new DBField(FLD_lastOdometerKM       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastOdometerKM"       , "Last Odometer km"            ), "format=#0.0 units=distance"),
        new DBField(FLD_odometerOffsetKM     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.odometerOffsetKM"     , "Odometer Offset km"          ), "format=#0.0 units=distance"),
        new DBField(FLD_lastEngineHours      , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastEngineHours"      , "Last Engine Hours"           ), "format=#0.0"),
        // Common fields
        newField_isActive(),
        newField_displayName(),
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    // Default Notification (RulesEngine support)
    // startupInit.Device.NotificationFieldInfo=true
    public static final String FLD_allowNotify           = "allowNotify";           // allow notification
    public static final String FLD_lastNotifyTime        = "lastNotifyTime";        // last notification time
    public static final String FLD_lastNotifyCode        = "lastNotifyCode";        // last notification status code
    public static final String FLD_notifyEmail           = "notifyEmail";           // notification email address
    public static final String FLD_notifySelector        = "notifySelector";        // notification rule
    public static final String FLD_notifyAction          = "notifyAction";          // notification action
    public static final String FLD_notifyDescription     = "notifyDescription";     // notification description
    public static final String FLD_notifySubject         = "notifySubject";         // notification subject
    public static final String FLD_notifyText            = "notifyText";            // notification message
    public static final String FLD_notifyUseWrapper      = "notifyUseWrapper";      // notification email wrapper
    public static final String FLD_notifyPriority        = "notifyPriority";        // notification priority
    public static final String FLD_parkedLatitude        = "parkedLatitude";        // parked latitude
    public static final String FLD_parkedLongitude       = "parkedLongitude";       // parked longitude
    public static final String FLD_parkedRadius          = "parkedRadius";          // parked radius meters
    public static final DBField NotificationFieldInfo[] = {
        new DBField(FLD_allowNotify          , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.allowNotify"          , "Allow Notification"          ), "edit=2"),
        new DBField(FLD_lastNotifyTime       , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastNotifyTime"       , "Last Notify Time"            ), _lastNotifyTime_attr()),
        new DBField(FLD_lastNotifyCode       , Integer.TYPE        , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastNotifyCode"       , "Last Notify Status Code"     ), "format=X2"),
        new DBField(FLD_notifyEmail          , String.class        , DBField.TYPE_EMAIL_LIST(), I18N.getString(Device.class,"Device.fld.notifyEmail"          , "Notification EMail Address"  ), "edit=2"),
        new DBField(FLD_notifySelector       , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.notifySelector"       , "Notification Selector"       ), "edit=2 editor=ruleSelector"),
        new DBField(FLD_notifyAction         , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.notifyAction"         , "Notification Action"         ), "edit=2 format=X2 editor=ruleAction mask=RuleFactory$NotifyAction"),
        new DBField(FLD_notifyDescription    , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.notifyDescription"    , "Notification Description"    ), "edit=2 utf8=true"),
        new DBField(FLD_notifySubject        , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.notifySubject"        , "Notification Subject"        ), "edit=2 utf8=true"),
        new DBField(FLD_notifyText           , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.notifyText"           , "Notification Message"        ), "edit=2 editor=textArea utf8=true"),
        new DBField(FLD_notifyUseWrapper     , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.notifyUseWrapper"     , "Notification Use Wrapper"    ), "edit=2"),
        new DBField(FLD_notifyPriority       , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.notifyPriority"       , "Notification Priority"       ), "edit=2"),
        new DBField(FLD_parkedLatitude       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.parkedLatitude"       , "Parked Latitude"             ), "format=#0.00000 edit=2"),
        new DBField(FLD_parkedLongitude      , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.parkedLongitude"      , "Parked Longitude"            ), "format=#0.00000 edit=2"),
        new DBField(FLD_parkedRadius         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.parkedRadius"         , "Parked Radius"               ), "format=#0.00000 edit=2"),
    };
    
    // Border Crossing
    // startupInit.Device.BorderCrossingFieldInfo=true
    public static final String FLD_borderCrossing        = "borderCrossing";        // border crossing flags
    public static final String FLD_lastBorderCrossTime   = "lastBorderCrossTime";   // timestamp of last border crossing calcs
    public static final DBField BorderCrossingFieldInfo[] = {
        new DBField(FLD_borderCrossing       , Integer.TYPE        , DBField.TYPE_UINT8       , I18N.getString(Device.class,"Device.fld.borderCrossing"       , "Border Crossing Flags"       ), "edit=2 enum=Device$BorderCrossingState"),
        new DBField(FLD_lastBorderCrossTime  , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastBorderCrossTime"  , "Last Border Crossing Time"   ), "format=time"),
    };
    
    // Device/Asset Link information
    // startupInit.Device.LinkFieldInfo=true
    public static final String FLD_linkURL               = "linkURL";               // Link URL
    public static final String FLD_linkDescription       = "linkDescription";       // Link Description
    public static final DBField LinkFieldInfo[] = {
        new DBField(FLD_linkURL              , String.class        , DBField.TYPE_STRING(128) , I18N.getString(Device.class,"Device.fld.linkURL"              , "Link URL"                    ), "edit=2"),
        new DBField(FLD_linkDescription      , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.linkDescription"      , "Link Description"            ), "edit=2"),
    };
    
    // Fixed device location fields
    // startupInit.Device.FixedLocationFieldInfo=true
    public static final String FLD_fixedLatitude         = "fixedLatitude";         // fixed latitude
    public static final String FLD_fixedLongitude        = "fixedLongitude";        // fixed longitude
    public static final String FLD_fixedAddress          = "fixedAddress";          // fixed address
    public static final String FLD_fixedContactPhone     = "fixedContactPhone";     // fixed contact phone#
    public static final String FLD_fixedServiceTime      = "fixedServiceTime";      // timestamp of last service
    public static final DBField FixedLocationFieldInfo[] = {
        new DBField(FLD_fixedLatitude        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fixedLatitude"        , "Fixed Latitude"              ), "format=#0.00000 edit=2"),
        new DBField(FLD_fixedLongitude       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fixedLongitude"       , "Fixed Longitude"             ), "format=#0.00000 edit=2"),
        new DBField(FLD_fixedAddress         , String.class        , DBField.TYPE_STRING(90)  , I18N.getString(Device.class,"Device.fld.fixedAddress"         , "Fixed Address (Physical)"    ), "utf8=true"),
        new DBField(FLD_fixedContactPhone    , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.fixedContactPhone"    , "Fixed Contact Phone"         ), "utf8=true"),
        new DBField(FLD_fixedServiceTime     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.fixedServiceTime"     , "Last Service Time"           ), "format=time edit=2"),
    };
    
    // GeoCorridor fields
    // startupInit.Device.GeoCorridorFieldInfo=true
    public static final String FLD_activeCorridor        = "activeCorridor";        // active GeoCorridor
    public static final DBField GeoCorridorFieldInfo[]   = {
        new DBField(FLD_activeCorridor       , String.class        , DBField.TYPE_CORR_ID()   , "Active GeoCorridor"          , ""),
    };
    
    // Maintenance odometer fields
    // startupInit.Device.MaintOdometerFieldInfo=true
    public static final String FLD_maintIntervalKM0      = "maintIntervalKM0";      // odometer maint #0 interval distance to next
    public static final String FLD_maintOdometerKM0      = "maintOdometerKM0";      // odometer maint #0 last Odometer 
    public static final String FLD_maintIntervalKM1      = "maintIntervalKM1";      // odometer maint #1 interval distance to next
    public static final String FLD_maintOdometerKM1      = "maintOdometerKM1";      // odometer maint #1 last Odometer
    public static final String FLD_maintIntervalHR0      = "maintIntervalHR0";      // hours maint #0 interval hours to next
    public static final String FLD_maintEngHoursHR0      = "maintEngHoursHR0";      // hours maint #0 last EngineHours
    public static final String FLD_maintNotes            = "maintNotes";
    public static final DBField MaintOdometerFieldInfo[] = {
        new DBField(FLD_maintIntervalKM0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintIntervalKM0"     , "#0 Maint Distance"           ), "format=#0.0 edit=2"),
        new DBField(FLD_maintOdometerKM0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintOdometerKM0"     , "#0 Maint Last Odom"          ), "format=#0.0"),
        new DBField(FLD_maintIntervalKM1     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintIntervalKM1"     , "#1 Maint Distance "          ), "format=#0.0 edit=2"),
        new DBField(FLD_maintOdometerKM1     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintOdometerKM1"     , "#1 Maint Last Odom"          ), "format=#0.0"),
        new DBField(FLD_maintIntervalHR0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintIntervalHR0"     , "#0 Maint ElapsedHours"       ), "format=#0.0 edit=2"),
        new DBField(FLD_maintEngHoursHR0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintEngHoursHR0"     , "#0 Maint Last EngineHours"   ), "format=#0.0"),
        new DBField(FLD_maintNotes           , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.maintNotes"           , "Maint Notes"                 ), "edit=2 editor=textArea utf8=true"),
    };

    // Misc fields
    // startupInit.Device.WorkOrderInfo=true
    public static final String FLD_workOrderID           = "workOrderID";           // WorkOrder ID
    public static final String FLD_customAttributes      = "customAttributes";      // custom attributes
    public static final DBField WorkOrderInfo[]          = {
        new DBField(FLD_workOrderID          , String.class        , DBField.TYPE_STRING(512) , "Work Order ID"               , "edit=2"),
        new DBField(FLD_customAttributes     , String.class        , DBField.TYPE_TEXT        , "Custom Fields"               , "edit=2"),
    };

    // Data "Push" fields
    // startupInit.Device.DataPushInfo=true
    public static final String FLD_lastDataPushTime      = "lastDataPushTime";      // timestamp of last data push
    public static final String FLD_lastEventCreateMillis = "lastEventCreateMillis"; // timestamp of last data push event creation time
    public static final DBField DataPushInfo[]           = {
        new DBField(FLD_lastDataPushTime     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastDataPushTime"     , "Last Data Push Time (sec)"   ), "format=time"),
        new DBField(FLD_lastEventCreateMillis, Long.TYPE           , DBField.TYPE_INT64       , I18N.getString(Device.class,"Device.fld.lastEventCreateMillis", "Last Event Create Time (MS)" ), "format=time"),
    };

    /* key class */
    public static class Key
        extends DeviceKey<Device>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId) {
            super.setFieldValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID , ((devId  != null)? devId.toLowerCase()  : ""));
        }
        public DBFactory<Device> getFactory() {
            return Device.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Device> factory = null;
    public static DBFactory<Device> getFactory()
    {
        if (factory == null) {
            EnumTools.registerEnumClass(NotifyAction.class);
            factory = DBFactory.createDBFactory(
                Device.TABLE_NAME(),
                Device.FieldInfo,
                DBFactory.KeyType.PRIMARY,
                Device.class,
                Device.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Device()
    {
        super();
    }

    /* database record */
    public Device(Device.Key key)
    {
        super(key);
    }

    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Device.class, loc);
        return i18n.getString("Device.description",
            "This table defines " +
            "Device/Vehicle specific information for an Account. " +
            "A 'Device' record typically represents something that is being 'tracked', such as a Vehicle."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
    // ------------------------------------------------------------------------

    /* Group ID (user informational only, not used by DeviceGroup) */
    // currently used in various ReportLayout subclasses
    public String getGroupID()
    {
        String v = (String)this.getFieldValue(FLD_groupID);
        return StringTools.trim(v);
    }

    public void setGroupID(String v)
    {
        this.setFieldValue(FLD_groupID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getEquipmentType()
    {
        String v = (String)this.getFieldValue(FLD_equipmentType);
        return StringTools.trim(v);
    }

    public void setEquipmentType(String v)
    {
        this.setFieldValue(FLD_equipmentType, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getVehicleID()
    {
        String v = (String)this.getFieldValue(FLD_vehicleID);
        return StringTools.trim(v);
    }

    public void setVehicleID(String v)
    {
        this.setFieldValue(FLD_vehicleID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getLicensePlate()
    {
        String v = (String)this.getFieldValue(FLD_licensePlate);
        return StringTools.trim(v);
    }

    public void setLicensePlate(String v)
    {
        this.setFieldValue(FLD_licensePlate, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getDriverID()
    {
        String v = (String)this.getFieldValue(FLD_driverID);
        return StringTools.trim(v);
    }

    public void setDriverID(String v)
    {
        this.setFieldValue(FLD_driverID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public double getFuelCapacity()
    {
        Double v = (Double)this.getFieldValue(FLD_fuelCapacity);
        return (v != null)? v.doubleValue() : 0.0;
    }

    public void setFuelCapacity(double v)
    {
        this.setFieldValue(FLD_fuelCapacity, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------

    public double getFuelEconomy()
    {
        Double v = (Double)this.getFieldValue(FLD_fuelEconomy);
        return (v != null)? v.doubleValue() : 0.0;
    }

    public void setFuelEconomy(double v)
    {
        this.setFieldValue(FLD_fuelEconomy, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------

    public double getSpeedLimitKPH()
    {
        Double v = (Double)this.getFieldValue(FLD_speedLimitKPH);
        return (v != null)? v.doubleValue() : 0.0;
    }

    public void setSpeedLimitKPH(double v)
    {
        this.setFieldValue(FLD_speedLimitKPH, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------

    /* return the time this account expires */
    public long getExpirationTime()
    {
        Long v = (Long)this.getFieldValue(FLD_expirationTime);
        return (v != null)? v.longValue() : 0L;
    }

    /* set the time this account expires */
    public void setExpirationTime(long v)
    {
        this.setFieldValue(FLD_expirationTime, v);
    }
    
    /* return true if this account has expired */
    public boolean isExpired()
    {
        // account expired?
        long expireTime = this.getExpirationTime();
        if (expireTime > 0L) {
            return (expireTime < DateTime.getCurrentTimeSec());
        } else {
            return false;
        }
    }
    
    /* return true if this account has an expiry date */
    public boolean doesExpire()
    {
        long expireTime = this.getExpirationTime();
        return (expireTime > 0L);
    }

    /* return true if this account will expire within the specified # of seconds */
    public boolean willExpire(long withinSec)
    {
        // will this account be expired 'withinSec' seconds into the future?
        long expireTime = this.getExpirationTime();
        if (expireTime > 0L) {
            if (withinSec >= 0L) {
                return (expireTime < (DateTime.getCurrentTimeSec() + withinSec));
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    public static boolean supportsLinkURL()
    {
        return Device.getFactory().hasField(FLD_linkURL);
    }

    public String getLinkURL()
    {
        String v = (String)this.getOptionalFieldValue(FLD_linkURL);
        return StringTools.trim(v);
    }

    public boolean hasLink()
    {
        return !StringTools.isBlank(this.getLinkURL());
    }

    public void setLinkURL(String v)
    {
        this.setOptionalFieldValue(FLD_linkURL, StringTools.trim(v));
    }
 
    // ------------------------------------------------------------------------

    public String getLinkDescription()
    {
        String v = (String)this.getOptionalFieldValue(FLD_linkDescription);
        return StringTools.trim(v);
    }

    public void setLinkDescription(String v)
    {
        this.setOptionalFieldValue(FLD_linkDescription, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public static boolean supportsNotification()
    {
        return Device.getFactory().hasField(FLD_allowNotify);
    }
    
    public boolean getAllowNotify()
    {
        Boolean v = (Boolean)this.getOptionalFieldValue(FLD_allowNotify);
        return (v != null)? v.booleanValue() : false;
    }

    public void setAllowNotify(boolean v)
    {
        this.setOptionalFieldValue(FLD_allowNotify, v);
    }

    public boolean allowNotify()
    {
        return this.getAllowNotify();
    }

    // ---

    public long getLastNotifyTime()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_lastNotifyTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setLastNotifyTime(long v)
    {
        this.setOptionalFieldValue(FLD_lastNotifyTime, v);
    }

    // ---

    public int getLastNotifyCode()
    {
        return this.getFieldValue(FLD_lastNotifyCode, 0);
    }

    public void setLastNotifyCode(int v)
    {
        this.setFieldValue(FLD_lastNotifyCode, v);
    }

    // ---

    public String getNotifyEmail()
    {
        String v = (String)this.getOptionalFieldValue(FLD_notifyEmail);
        return StringTools.trim(v);
    }

    public void setNotifyEmail(String v)
    {
        this.setOptionalFieldValue(FLD_notifyEmail, StringTools.trim(v));
    }

    public String getNotifyEmail(boolean inclAccount)
    {
        String devEmail = this.getNotifyEmail();
        if (inclAccount) {
            Account acct = this.getAccount();
            if (acct != null) {
                String acctEmail = acct.getNotifyEmail();
                if (!acctEmail.equals("")) {
                    return !devEmail.equals("")? (devEmail + "," + acctEmail) : acctEmail;
                }
            }
        }
        return devEmail;
    }

    // ---

    /* get rule selector which be evaluated by the installed RuleFactory */
    public String getNotifySelector()
    {
        // see CHECK_NOTIFY_SELECTOR
        String v = (String)this.getOptionalFieldValue(FLD_notifySelector);
        return StringTools.trim(v);
    }

    /* set the notification rule selector */
    public void setNotifySelector(String v)
    {
        this.setOptionalFieldValue(FLD_notifySelector, StringTools.trim(v));
    }

    // ---

    public int getNotifyAction()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_notifyAction);
        return (v != null)? RuleFactoryAdapter.ValidateActionMask(v.intValue()) : RuleFactory.ACTION_DEFAULT;
    }

    public void setNotifyAction(int v)
    {
        this.setOptionalFieldValue(FLD_notifyAction, RuleFactoryAdapter.ValidateActionMask(v));
    }

    // ---

    public String getNotifyDescription()
    {
        String v = (String)this.getOptionalFieldValue(FLD_notifyDescription);
        return StringTools.trim(v);
    }

    public void setNotifyDescription(String v)
    {
        this.setOptionalFieldValue(FLD_notifyDescription, StringTools.trim(v));
    }

    // ---

    public String getNotifySubject()
    {
        String v = (String)this.getFieldValue(FLD_notifySubject);
        return (v != null)? v : "";
    }

    public void setNotifySubject(String v)
    {
        this.setFieldValue(FLD_notifySubject, ((v != null)? v : ""));
    }

    // ---

    public String getNotifyText()
    {
        String v = (String)this.getFieldValue(FLD_notifyText);
        return (v != null)? v : "";
    }

    public void setNotifyText(String v)
    {
        String s = (v != null)? StringTools.encodeNewline(v) : "";
        this.setFieldValue(FLD_notifyText, s);
    }

    // ---

    public boolean getNotifyUseWrapper()
    {
        if (ALLOW_USE_EMAIL_WRAPPER) {
            Boolean v = (Boolean)this.getFieldValue(FLD_notifyUseWrapper);
            return (v != null)? v.booleanValue() : true;
        } else {
            return false;
        }
    }

    public void setNotifyUseWrapper(boolean v)
    {
        this.setFieldValue(FLD_notifyUseWrapper, v);
    }

    // ---

    public int getNotifyPriority()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_notifyPriority);
        return (v != null)? v.intValue() : 0;
    }

    public void setNotifyPriority(int v)
    {
        this.setOptionalFieldValue(FLD_notifyPriority, ((v < 0)? 0 : v));
    }

    // ------------------------------------------------------------------------

    public void setParkedLocation(GeoPoint parkLoc, double parkRadM)
    {
        if (!GeoPoint.isValid(parkLoc) || (parkRadM <= 0.0)) {
            //Print.logInfo("Clearing parked location");
            this.setParkedLatitude(0.0);
            this.setParkedLongitude(0.0);
            this.setParkedRadius(0.0);
        } else {
            //Print.logInfo("Setting parked location: " + parkLoc + " " + parkRadM + " m");
            this.setParkedLatitude(parkLoc.getLatitude());
            this.setParkedLongitude(parkLoc.getLongitude());
            this.setParkedRadius(parkRadM);
        }
        this.addOtherChangedFieldNames(Device.FLD_parkedLatitude,Device.FLD_parkedLongitude,Device.FLD_parkedRadius);
    }

    public boolean isParked()
    {
        if (this.getParkedRadius() <= 0.0) {
            return false;
        } else
        if (!GeoPoint.isValid(this.getParkedLatitude(),this.getParkedLongitude())) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isParkedViolation(GeoPoint gp)
    {

        /* no point specified */
        if (!GeoPoint.isValid(gp)) {
            return false; // invalid point, no violation
        }

        /* get parked location */
        double parkLat = this.getParkedLatitude();
        double parkLon = this.getParkedLongitude();
        double parkRad = this.getParkedRadius();
        if (!GeoPoint.isValid(parkLat,parkLon) || (parkRad <= 0.0)) {
            return false; // not parked, no violation
        }
        GeoPoint parkLoc = new GeoPoint(parkLat,parkLon);

        /* outside of parked zone? */
        double distM = parkLoc.metersToPoint(gp);
        //Print.logInfo("Comparing ParkRadius '"+parkRad+"' to distance '"+distM+"' m");
        return (distM > parkRad)? true : false;

    }

    public double getParkedLatitude()
    {
        return this.getOptionalFieldValue(FLD_parkedLatitude, 0.0);
    }

    public void setParkedLatitude(double v)
    {
        this.setOptionalFieldValue(FLD_parkedLatitude, v);
    }

    public double getParkedLongitude()
    {
        return this.getOptionalFieldValue(FLD_parkedLongitude, 0.0);
    }

    public void setParkedLongitude(double v)
    {
        this.setOptionalFieldValue(FLD_parkedLongitude, v);
    }

    public double getParkedRadius()
    {
        return this.getOptionalFieldValue(FLD_parkedRadius, 0.0);
    }

    public void setParkedRadius(double v)
    {
        this.setOptionalFieldValue(FLD_parkedRadius, v);
    }

    // ------------------------------------------------------------------------

    /* return true if the Device record support the BorderCrossing fields */
    public static boolean supportsBorderCrossing()
    {
        return Device.getFactory().hasField(FLD_borderCrossing);
    }

    public int getBorderCrossing()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_borderCrossing);
        return (v != null)? v.intValue() : 0;
        // Note the returned value of this flag may be ignored by 'BorderCrossing'
    }

    public void setBorderCrossing(int flags)
    {
        this.setOptionalFieldValue(FLD_borderCrossing, flags);
    }
    
    public void setBorderCrossing(Device.BorderCrossingState bcs)
    {
        int bcf = (bcs != null)? bcs.getIntValue() : Device.BorderCrossingState.OFF.getIntValue();
        this.setBorderCrossing(bcf);
    }

    // ---

    public long getLastBorderCrossTime()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_lastBorderCrossTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setLastBorderCrossTime(long v)
    {
        this.setOptionalFieldValue(FLD_lastBorderCrossTime, v);
    }

    // Device/Asset specific data above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DataTransport specific data below
    
    private String modemID = "";
    
    public String getModemID()
    {
        return this.modemID;
    }

    public void setModemID(String mid)
    {
        // NOT stored in the Device table.  Only used by the caller
        this.modemID = StringTools.trim(mid);
    }

    // --------
    
    public String getUniqueID()
    {
        String v = (String)this.getFieldValue(FLD_uniqueID);
        return StringTools.trim(v);
    }

    public void setUniqueID(String v)
    {
        this.setFieldValue(FLD_uniqueID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getDeviceCode()
    {
        String v = (String)this.getFieldValue(FLD_deviceCode);  // serverID
        return StringTools.trim(v);
    }

    public void setDeviceCode(String v)
    {
        this.setFieldValue(FLD_deviceCode, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getDeviceType()
    {
        String v = (String)this.getFieldValue(FLD_deviceType);
        return StringTools.trim(v);
    }

    public void setDeviceType(String v)
    {
        this.setFieldValue(FLD_deviceType, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get the DCS Properties ID assigned to this account */
    public String getDcsPropertiesID()
    {
        String v = (String)this.getFieldValue(FLD_dcsPropertiesID);
        return StringTools.trim(v);
    }

    /* set the DCS Properties ID assigned to this account */
    public void setDcsPropertiesID(String v)
    {
        this.setFieldValue(FLD_dcsPropertiesID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public boolean hasPushpinID()
    {
        return !StringTools.isBlank(this.getPushpinID());
    }

    public String getPushpinID()
    {
        String v = (String)this.getFieldValue(FLD_pushpinID);
        return StringTools.trim(v);
    }

    public void setPushpinID(String v)
    {
        this.setFieldValue(FLD_pushpinID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public boolean hasDisplayColor()
    {
        return !StringTools.isBlank(this.getDisplayColor());
    }

    public String getDisplayColor()
    {
        String v = (String)this.getFieldValue(FLD_displayColor);
        return StringTools.trim(v);
    }

    public ColorTools.RGB getDisplayColor(ColorTools.RGB dft)
    {
        return ColorTools.parseColor(this.getDisplayColor(),dft);
    }

    public void setDisplayColor(ColorTools.RGB v)
    {
        this.setDisplayColor((v != null)? v.toString(true) : null);
    }

    public void setDisplayColor(String v)
    {
        this.setFieldValue(FLD_displayColor, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public void setMapLegend(String legend)
    {
        //
    }

    public String getMapLegend()
    {
        return "";
    }

    // ------------------------------------------------------------------------

    public String getSerialNumber()
    {
        String v = (String)this.getFieldValue(FLD_serialNumber);
        return StringTools.trim(v);
    }

    public void setSerialNumber(String v)
    {
        this.setFieldValue(FLD_serialNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getSimPhoneNumber()
    {
        String v = (String)this.getFieldValue(FLD_simPhoneNumber);
        return StringTools.trim(v);
    }

    public void setSimPhoneNumber(String v)
    {
        this.setFieldValue(FLD_simPhoneNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getSmsEmail()
    {
        String v = (String)this.getFieldValue(FLD_smsEmail);
        return StringTools.trim(v);
    }

    public void setSmsEmail(String v)
    {
        this.setFieldValue(FLD_smsEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getImeiNumber()
    {
        String v = (String)this.getFieldValue(FLD_imeiNumber);
        return StringTools.trim(v);
    }

    public void setImeiNumber(String v)
    {
        this.setFieldValue(FLD_imeiNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public boolean validateDataKey(String key)
    {
        // check for a valid key/pin here
        String dkey = this.getDataKey();
        return !StringTools.isBlank(dkey)? dkey.equals(key) : true;
    }

    public String getDataKey()
    {
        String v = (String)this.getFieldValue(FLD_dataKey);
        return StringTools.trim(v);
    }

    public byte[] getDataKeyAsByteArray()
    {
        return StringTools.parseHex(this.getDataKey(),null);
    }

    public void setDataKey(String v)
    {
        this.setFieldValue(FLD_dataKey, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public boolean getLastInputState(int bit)
    {
        long mask = this.getLastInputState();
        DCServerConfig dcs = this.getDCServerConfig();
        if (dcs != null) {
            return dcs.getDigitalInputState(mask, bit);
        } else {
            return ((mask & (1L << bit)) != 0L);
        }
    }

    public long getLastInputState()
    {
        Long v = (Long)this.getFieldValue(FLD_lastInputState);
        return (v != null)? v.longValue() : 0L;
    }

    public void setLastInputState(long v)
    {
        this.setFieldValue(FLD_lastInputState, v & 0xFFFFFFFFL);
    }

    // ------------------------------------------------------------------------

    public double getLastBatteryLevel()
    {
        Double v = (Double)this.getFieldValue(FLD_lastBatteryLevel);
        return (v != null)? v.doubleValue() : 0.0;
    }

    public void setLastBatteryLevel(double v)
    {
        this.setFieldValue(FLD_lastBatteryLevel, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    public double getLastFuelLevel()
    {
        Double v = (Double)this.getFieldValue(FLD_lastFuelLevel);
        return (v != null)? v.doubleValue() : 0.0;
    }

    public void setLastFuelLevel(double v)
    {
        this.setFieldValue(FLD_lastFuelLevel, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    public double getLastFuelTotal()
    {
        Double v = (Double)this.getFieldValue(FLD_lastFuelTotal);
        return (v != null)? v.doubleValue() : 0.0;
    }

    public void setLastFuelTotal(double v)
    {
        this.setFieldValue(FLD_lastFuelTotal, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    public double getLastOilLevel()
    {
        Double v = (Double)this.getFieldValue(FLD_lastOilLevel);
        return (v != null)? v.doubleValue() : 0.0;
    }

    public void setLastOilLevel(double v)
    {
        this.setFieldValue(FLD_lastOilLevel, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    public int getIgnitionIndex()
    {
        Integer v = (Integer)this.getFieldValue(FLD_ignitionIndex);
        return (v != null)? v.intValue() : -1;
    }

    public void setIgnitionIndex(int v)
    {
        this.setFieldValue(FLD_ignitionIndex, ((v >= 0)? v : -1));
    }

    public int[] getIgnitionStatusCodes()
    {
        int ndx = this.getIgnitionIndex();
        if (ndx >= 0) {
            int scOFF = StatusCodes.GetDigitalInputStatusCode(ndx, false);
            int scON  = StatusCodes.GetDigitalInputStatusCode(ndx, true );
            if (scOFF != StatusCodes.STATUS_NONE) {
                return new int[] { scOFF, scON };
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
    *** Returns the current ignition state
    *** -1 = unknown
    ***  0 = off
    ***  1 = on
    **/
    private int ignitionState = -2;
    public int getIgnitionState()
    {

        /* already determined? */
        if (this.ignitionState >= -1) {
            // already initialized
            return this.ignitionState;
        }

        /* get ignition state index */
        int ignNdx = this.getIgnitionIndex();
        if (ignNdx < 0) {
            // no ignition bit specified
            this.ignitionState = -1; //  unknwon
            return this.ignitionState;
        }

        /* look for StatusCodes.IGNITION_[ON|OFF]? */
        if (ignNdx >= StatusCodes.IGNITION_INPUT_INDEX) {
            int sc[] = this.getIgnitionStatusCodes();
            if (sc == null) {
                this.ignitionState = -1;
            } else {
                try {
                    EventData ev = this.getLastEvent(sc);
                    if (ev == null) {
                        this.ignitionState = -1;
                    } else {
                        this.ignitionState = (ev.getStatusCode() == sc[1])? 1 : 0;
                    }
                } catch (DBException dbe) {
                    this.ignitionState = -1;
                }
            }
            return this.ignitionState;
        }

        /* check input mask */
        this.ignitionState = this.getLastInputState(ignNdx)? 1 : 0;
        return this.ignitionState;

    }

    /**
    *** Returns the ignition state at the specified Event
    *** -1 = unknown
    ***  0 = off
    ***  1 = on
    **/
    public int getIgnitionState(EventData ev)
    {
        
        /* not event? */
        if (ev == null) {
            // event is null
            return this.getIgnitionState();
        }

        /* get ignition state index */
        int ignNdx = this.getIgnitionIndex();
        if (ignNdx < 0) {
            // no ignition bit specified
            return -1;
        }

        /* look for StatusCodes.IGNITION_[ON|OFF]? */
        if (ignNdx >= StatusCodes.IGNITION_INPUT_INDEX) {
            int sc[] = this.getIgnitionStatusCodes();
            if (sc == null) {
                return -1;
            }
            if (ev.getStatusCode() == sc[0]) {
                return 0;
            } else
            if (ev.getStatusCode() == sc[1]) {
                return 1;
            }
            try {
                EventData priorEV = this.getLastEvent(sc, ev.getTimestamp(), false);
                if (priorEV == null) {
                    return -1;
                } else {
                    return (priorEV.getStatusCode() == sc[1])? 1 : 0;
                }
            } catch (DBException dbe) {
                return -1;
            }
        }

        /* check input mask */
        long mask = ev.getInputMask();
        DCServerConfig dcs = this.getDCServerConfig();
        if (dcs != null) {
            return dcs.getDigitalInputState(mask, ignNdx)? 1 : 0;
        } else {
            return ((mask & (1L << ignNdx)) != 0L)? 1 : 0;
        }

    }

    // ------------------------------------------------------------------------

    public String getCodeVersion()
    {
        String v = (String)this.getFieldValue(FLD_codeVersion);
        return StringTools.trim(v);
    }

    public void setCodeVersion(String v)
    {
        this.setFieldValue(FLD_codeVersion, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getFeatureSet()
    {
        String v = (String)this.getFieldValue(FLD_featureSet);
        return StringTools.trim(v);
    }

    public void setFeatureSet(String v)
    {
        this.setFieldValue(FLD_featureSet, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public DTIPAddrList getIpAddressValid()
    {
        DTIPAddrList v = (DTIPAddrList)this.getFieldValue(FLD_ipAddressValid);
        return v; // May return null!!
    }

    public void setIpAddressValid(DTIPAddrList v)
    {
        this.setFieldValue(FLD_ipAddressValid, v);
    }

    public void setIpAddressValid(String v)
    {
        this.setIpAddressValid((v != null)? new DTIPAddrList(v) : null);
    }

    public boolean isValidIPAddress(String ipAddr)
    {
        DTIPAddrList ipList = this.getIpAddressValid();
        if ((ipList == null) || ipList.isEmpty()) {
            return true;
        } else
        if (!ipList.isMatch(ipAddr)) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    public DTIPAddress getIpAddressCurrent()
    {
        DTIPAddress v = (DTIPAddress)this.getFieldValue(FLD_ipAddressCurrent);
        return v; // May return null!!
    }

    public void setIpAddressCurrent(DTIPAddress v)
    {
        this.setFieldValue(FLD_ipAddressCurrent, v);
    }

    public void setIpAddressCurrent(String v)
    {
        this.setIpAddressCurrent((v != null)? new DTIPAddress(v) : null);
    }

    // ------------------------------------------------------------------------

    public int getRemotePortCurrent()
    {
        Integer v = (Integer)this.getFieldValue(FLD_remotePortCurrent);
        return (v != null)? v.intValue() : 0;
    }

    public void setRemotePortCurrent(int v)
    {
        this.setFieldValue(FLD_remotePortCurrent, ((v > 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    public int getListenPortCurrent()
    {
        Integer v = (Integer)this.getFieldValue(FLD_listenPortCurrent);
        return (v != null)? v.intValue() : 0;
    }

    public void setListenPortCurrent(int v)
    {
        this.setFieldValue(FLD_listenPortCurrent, ((v > 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    public double getLastValidLatitude()
    {
        return this.getOptionalFieldValue(FLD_lastValidLatitude, 0.0);
    }

    public void setLastValidLatitude(double v)
    {
        this.setOptionalFieldValue(FLD_lastValidLatitude, v);
    }

    public double getLastValidLongitude()
    {
        return this.getOptionalFieldValue(FLD_lastValidLongitude, 0.0);
    }

    public void setLastValidLongitude(double v)
    {
        this.setOptionalFieldValue(FLD_lastValidLongitude, v);
    }

    public GeoPoint getLastValidLocation()
    {
        // returns null if invalid
        double lat = this.getLastValidLatitude();
        double lon = this.getLastValidLongitude();
        return GeoPoint.isValid(lat,lon)? new GeoPoint(lat,lon) : null;
    }

    public GeoPoint getLastValidLocation(boolean tryLastEvent)
    {
        GeoPoint gp = this.getLastValidLocation();
        if ((gp == null) && tryLastEvent) {
            try {
                EventData lastEv = this.getLastEvent(true); // valid GPS only
                if ((lastEv != null) && lastEv.isValidGeoPoint()) {
                    gp = lastEv.getGeoPoint();
                    this.setLastValidLocation(
                        lastEv.getTimestamp(),      // FLD_lastGPSTimestamp
                        lastEv.getGeoPoint(),       // FLD_lastValidLatitude/FLD_lastValidLongitude
                        lastEv.getHeading());       // FLD_lastValidHeading
                    if (this.getLastOdometerKM() <= 0.0) {
                        this.setLastOdometerKM(lastEv.getOdometerKM()); // may still be '0.0'
                    }
                }
            } catch (DBException dbe) {
                // ignore error
            }
        }
        return gp;
    }

    private void setLastValidLocation(long timestamp, GeoPoint gp, double heading)
    {
        if ((gp != null) && gp.isValid()) {
            this.setLastGPSTimestamp(timestamp);            // FLD_lastGPSTimestamp
            this.setLastValidLatitude(gp.getLatitude());    // FLD_lastValidLatitude
            this.setLastValidLongitude(gp.getLongitude());  // FLD_lastValidLongitude
            if (heading >= 0.0) {
                this.setLastValidHeading(heading);          // FLD_lastValidHeading
            }
        } else {
            this.setLastGPSTimestamp(0L);                   // FLD_lastGPSTimestamp
            this.setLastValidLatitude(0.0);                 // FLD_lastValidLatitude
            this.setLastValidLongitude(0.0);                // FLD_lastValidLongitude
            this.setLastValidHeading(0.0);                  // FLD_lastValidHeading
        }
    }

    public double getMetersToLastValidLocation(GeoPoint gp)
    {
        if (GeoPoint.isValid(gp)) {
            GeoPoint lastValidLoc = this.getLastValidLocation(true);
            if (lastValidLoc != null) {
                return gp.metersToPoint(lastValidLoc);
            }
        }
        return -1.0;
    }

    public boolean isNearLastValidLocation(GeoPoint gp, double meters)
    {
        if (meters > 0.0) {
            double deltaM = this.getMetersToLastValidLocation(gp); // '-1' if 'gp' is invalid
            return ((deltaM >= 0.0) && (deltaM < meters)); // false if gp is invalid
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    public double getLastValidHeading()
    {
        return this.getOptionalFieldValue(FLD_lastValidHeading, 0.0);
    }

    public void setLastValidHeading(double v)
    {
        this.setOptionalFieldValue(FLD_lastValidHeading, v);
    }

    // ------------------------------------------------------------------------

    public long getLastGPSTimestamp()
    {
        Long v = (Long)this.getFieldValue(FLD_lastGPSTimestamp);
        return (v != null)? v.longValue() : 0L;
    }

    public void setLastGPSTimestamp(long v)
    {
        this.setFieldValue(FLD_lastGPSTimestamp, v);
    }

    // ------------------------------------------------------------------------

    public String getLastCellServingInfo()
    {
        String v = (String)this.getFieldValue(FLD_lastCellServingInfo);
        return StringTools.trim(v);
    }

    public void setLastCellServingInfo(String v)
    {
        this.setFieldValue(FLD_lastCellServingInfo, StringTools.trim(v));
    }
    
    public void setLastServingCellTower(CellTower sct)
    {
        if (sct != null) {
            this.setLastCellServingInfo(sct.toString());
        } else {
            this.setLastCellServingInfo(null);
        }
    }

    public CellTower getLastServingCellTower()
    {
        String csi = this.getLastCellServingInfo();
        if (!StringTools.isBlank(csi)) {
            return new CellTower(csi);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    private static final Integer GEOZONE_ARRIVE     = new Integer(StatusCodes.STATUS_GEOFENCE_ARRIVE);
    private static final Integer GEOZONE_DEPART     = new Integer(StatusCodes.STATUS_GEOFENCE_DEPART);
    private static final Integer CORRIDOR_ENABLE    = new Integer(StatusCodes.STATUS_CORRIDOR_ACTIVE);
    private static final Integer CORRIDOR_DISABLE   = new Integer(StatusCodes.STATUS_CORRIDOR_INACTIVE);

    public static class GeozoneTransition
    {
        private long    time = 0L;
        private Integer code = null;
        private Geozone zone = null;
        public GeozoneTransition(long timestamp, Integer code, Geozone zone) {
            this.time = timestamp;
            this.code = code;
            this.zone = zone;
        }
        public long getTimestamp() {
            return this.time;
        }
        public int getStatusCode() {
            return this.code.intValue();
        }
        public Geozone getGeozone() {
            return this.zone;
        }
        public String getGeozoneID() {
            return this.zone.getGeozoneID();
        }
        public String getGeozoneDescription() {
            return this.zone.getDescription();
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[").append(StatusCodes.GetDescription(this.code,null)).append("] ");
            sb.append(this.getGeozoneID()).append(" - ");
            sb.append(this.getGeozoneDescription());
            return sb.toString();
        }
    }

    public java.util.List<GeozoneTransition> checkGeozoneTransitions(long eventTime, GeoPoint eventGP)
    {

        /* invalid point? */
        if (!GeoPoint.isValid(eventGP)) {
            return null;
        }

        /* look for geozone transition */
        String accountID = this.getAccountID();
        GeoPoint prevGP  = this.getLastValidLocation(true);
        Geozone prevZone = (prevGP != null)? Geozone.getGeozone(accountID, null, prevGP, false) : null;
        Geozone thisZone = Geozone.getGeozone(accountID, null, eventGP, false);
        //Print.logInfo("Previous Location: " + prevGP);
        //Print.logInfo("Current  Location: " + eventGP);

        /* GeozoneTransision list */
        java.util.List<GeozoneTransition> geoTrans = null;

        /* depart only */
        if ((prevZone != null) && (thisZone == null)) {
            boolean isDepart = prevZone.isDepartureZone();
            if (isDepart) {
                geoTrans = new Vector<GeozoneTransition>();
                geoTrans.add(new GeozoneTransition(eventTime - 2L, GEOZONE_DEPART, prevZone));
            }
            return geoTrans;
        }

        /* arrive only */
        if ((prevZone == null) && (thisZone != null)) {
            boolean isArrive = thisZone.isArrivalZone();
            if (isArrive) {
                geoTrans = new Vector<GeozoneTransition>();
                geoTrans.add(new GeozoneTransition(eventTime - 1L, GEOZONE_ARRIVE, thisZone));
            }
            return geoTrans;
        }

        /* depart, then arrive */
        if ((prevZone != null) && (thisZone != null) && !prevZone.getGeozoneID().equals(thisZone.getGeozoneID())) {
            boolean isDepart = prevZone.isDepartureZone();
            boolean isArrive = thisZone.isArrivalZone();
            if (isDepart || isArrive) {
                geoTrans = new Vector<GeozoneTransition>();
                if (isDepart) {
                    geoTrans.add(new GeozoneTransition(eventTime - 2L, GEOZONE_DEPART, prevZone));
                }
                if (isArrive) {
                    geoTrans.add(new GeozoneTransition(eventTime - 1L, GEOZONE_ARRIVE, thisZone));
                }
            }
            return geoTrans;
        }

        return null;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum allowed odometer value
    *** @return The maximum allowed odometer value
    **/
    public double getMaxOdometerKM()
    {
        // TODO: should be device dependent
        return Device.GetMaximumOdometerKM();
    }

    /* return true if the Device record support the last odometer fields */
    public static boolean supportsLastOdometer()
    {
        // now alway true
        return Device.getFactory().hasField(FLD_lastOdometerKM);
    }

    public double getLastOdometerKM()
    {
        return this.getOptionalFieldValue(FLD_lastOdometerKM, 0.0);
    }

    /*
    public double getLastOdometerKM(boolean tryLastEvent)
    {
        double odomKM = this.getLastOdometerKM();
        if (odomKM > 0.0) {
            return odomKM;
        } else
        if (tryLastEvent) {
            try {
                EventData lastEv = this.getLastEvent(true);
                if ((lastEv != null) && lastEv.isValidGeoPoint()) {
                    odomKM = lastEv.getOdometerKM(); // may be 0
                    this.setLastOdometerKM(odomKM);                         // FLD_lastOdometerKM
                    if (this.getLastValidLocation() == null) {
                        this.setLastValidLocation(
                            lastEv.getTimestamp(),      // FLD_lastGPSTimestamp
                            lastEv.getGeoPoint(),       // FLD_lastValidLatitude/FLD_lastValidLongitude
                            lastEv.getHeading());       // FLD_lastValidHeading
                        this.setLastGPSTimestamp();    
                    }
                    return odomKM;
                } else {
                    return 0.0;
                }
            } catch (DBException dbe) {
                // ignore error
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }
    */

    public void setLastOdometerKM(double v)
    {
        if (v < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_lastOdometerKM, v);
        }
    }

    public double getNextOdometerKM(GeoPoint geoPoint)
    {
        GeoPoint lastValidLoc = this.getLastValidLocation(true); // try last event
        double odomKM = this.getLastOdometerKM(); // only try cached value
        if (GeoPoint.isValid(geoPoint) && (lastValidLoc != null)) {
            odomKM += geoPoint.kilometersToPoint(lastValidLoc);
        }
        return odomKM;
    }

    public double adjustOdometerKM(double v)
    {
        double lastOdomKM = this.getLastOdometerKM();
        if (v < lastOdomKM) {
            return lastOdomKM;
        } else
        if (v >= this.getMaxOdometerKM()) {
            return lastOdomKM;
        } else {
            return v;
        }
    }

    // ------------------------------------------------------------------------

    public double getOdometerOffsetKM()
    {
        return this.getOptionalFieldValue(FLD_odometerOffsetKM, 0.0);
    }

    public void setOdometerOffsetKM(double v)
    {
        if (v < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_odometerOffsetKM, v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum allowed engine-hours value
    *** @return The maximum allowed engine-hours value
    **/
    public double getMaxRuntimeHours()
    {
        // TODO: should be device dependent
        return Device.GetMaximumRuntimeHours();
    }

    /* return true if the Device record support the last engine-hours fields */
    public static boolean supportsLastEngineHours()
    {
        // alway true
        return Device.getFactory().hasField(FLD_lastEngineHours);
    }

    public double getLastEngineHours()
    {
        return this.getOptionalFieldValue(FLD_lastEngineHours, 0.0);
    }

    public void setLastEngineHours(double v)
    {
        if (v < this.getMaxRuntimeHours()) {
            this.setOptionalFieldValue(FLD_lastEngineHours, v);
        }
    }

    // ------------------------------------------------------------------------

    /* not yet fully implemented */
    public String getPingCommandURI()
    {
        String v = (String)this.getFieldValue(FLD_pingCommandURI);
        return StringTools.trim(v);
    }

    /* not yet fully implemented */
    public void setPingCommandURI(String v)
    {
        // valid options:
        //   tcp://192.168.11.11:21500
        //   udp://192.168.11.11:31400
        //   sms://9165551212
        //   smtp://9165551212@example.com
        this.setFieldValue(FLD_pingCommandURI, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getPendingPingCommand()
    {
        String v = (String)this.getFieldValue(FLD_pendingPingCommand);
        return StringTools.trim(v);
    }

    public void setPendingPingCommand(String v)
    {
        this.setFieldValue(FLD_pendingPingCommand, StringTools.trim(v));
    }

    public boolean hasPendingPingCommand()
    {
        return !StringTools.isBlank(this.getPendingPingCommand());
    }

    // ------------------------------------------------------------------------

    public long getLastPingTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastPingTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastPingTime(long v)
    {
        this.setFieldValue(FLD_lastPingTime, v);
    }

    public void setLastPingTime(long v)
    {
        this._setLastPingTime(v);
        if (this.transport != null) {
            this.transport._setLastPingTime(v);
        }
    }

    // ------------------------------------------------------------------------

    public int getTotalPingCount()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalPingCount);
        return (v != null)? v.intValue() : 0;
    }

    public void _setTotalPingCount(int v)
    {
        this.setFieldValue(FLD_totalPingCount, v);
    }

    public void setTotalPingCount(int v)
    {
        this._setTotalPingCount(v);
        if (this.transport != null) {
            this.transport._setTotalPingCount(v);
        }
    }

    public boolean incrementPingCount(long pingTime, boolean reload, boolean update)
    {

        /* refresh current value */
        if (reload) {
            // in case another Device 'ping' has changed this value already
            this.reload(Device.FLD_totalPingCount);
        }

        /* increment ping count */
        this.setTotalPingCount(this.getTotalPingCount() + 1);
        if (pingTime > 0L) {
            this.setLastPingTime(pingTime);
        }

        /* update Device record */
        if (update) {
            try {
                this.update( // may throw DBException
                    Device.FLD_lastPingTime,
                    Device.FLD_totalPingCount);
            } catch (DBException dbe) {
                Print.logException("Unable to update 'ping' count", dbe);
                return false;
            }
        }

        /* update Account */
        Account account = this.getAccount();
        if (account != null) {
            account.incrementPingCount(pingTime, reload, update);
        }

        return true;
    }

    // ------------------------------------------------------------------------

    public int getMaxPingCount()
    {
        Integer v = (Integer)this.getFieldValue(FLD_maxPingCount);
        return (v != null)? v.intValue() : 0;
    }

    public void _setMaxPingCount(int v)
    {
        this.setFieldValue(FLD_maxPingCount, v);
    }

    public void setMaxPingCount(int v)
    {
        this._setMaxPingCount(v);
        if (this.transport != null) {
            this.transport._setMaxPingCount(v);
        }
    }
    
    public boolean exceedsMaxPingCount() 
    {

        /* check device */
        {
            int totPings = this.getTotalPingCount();
            int maxPings = this.getMaxPingCount();
            if ((maxPings > 0) && (totPings >= maxPings)) {
                Print.logInfo("Device exceeded maximum allowed pings: %d >= %d", totPings, maxPings);
                return true;
            }
        }

        /* check account */
        Account account = this.getAccount();
        if (account != null) {
            int totPings = account.getTotalPingCount();
            int maxPings = account.getMaxPingCount();
            if ((maxPings > 0) && (totPings >= maxPings)) {
                Print.logInfo("Account exceeded maximum allowed pings: %d >= %d", totPings, maxPings);
                return true;
            }
        }

        /* not over limit */
        return false;
        
    }

    // ------------------------------------------------------------------------

    public boolean getExpectAck()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_expectAck);
        return (v != null)? v.booleanValue() : true;
    }

    public void _setExpectAck(boolean v)
    {
        this.setFieldValue(FLD_expectAck, v);
    }

    public void setExpectAck(boolean v)
    {
        this._setExpectAck(v);
        if (this.transport != null) {
            this.transport._setExpectAck(v);
        }
    }

    // ------------------------------------------------------------------------

    public String getLastAckCommand()
    {
        String v = (String)this.getFieldValue(FLD_lastAckCommand);
        return StringTools.trim(v);
    }

    public void setLastAckCommand(String v)
    {
        this.setFieldValue(FLD_lastAckCommand, StringTools.trim(v));
    }

    public boolean isExpectingCommandAck()
    {
        return this.getExpectAck() && (this.getLastAckTime() <= 0L);
    }

    public boolean setExpectCommandAck(DCServerConfig.Command command, String cmdStr)
    {

        /* no command */
        if ((command == null) && StringTools.isBlank(cmdStr)) {
            // both 'command' and 'cmdStr' are null/blank
            return false;
        }

        /* no command specified? */
        if ((command != null) && !command.getExpectAck()) {
            Print.logWarn("Not expecting an ACK for Command: " + command.getName());
            return false;
        }

        /* already waiting for an ACK? */
        if (this.isExpectingCommandAck()) {
            // we are already expecting an ACK
            Print.logWarn("Already expecting an ACK for: " + this.getLastAckCommand());
        }

        /* save ACK command */
        try {
            String cs = !StringTools.isBlank(cmdStr)? cmdStr : command.getCommandString();
            this.setExpectAck(true);
            this.setLastAckCommand(cs);
            this.setLastAckTime(0L);
            this.update(FLD_expectAck, FLD_lastAckCommand, FLD_lastAckTime);
            Print.logInfo("ACK expected for command: " + cs);
            return true;
        } catch (DBException dbe) {
            Print.logException("Unable to set Device.lastAck...", dbe);
            return false;
        }

    }

    public boolean clearExpectCommandAck(DCServerConfig.Command command)
    {

        /* not expecting an ACK? */
        if (!this.isExpectingCommandAck()) {
            Print.logInfo("Device is not expecting an ACK");
            return false;
        }

        /* clear ACK command */
        try {
            String lastAckCmd = this.getLastAckCommand();
            this.setExpectAck(false);
            //this.setLastAckCommand("");
            this.setLastAckTime(DateTime.getCurrentTimeSec());
            this.update(FLD_expectAck, FLD_lastAckTime);
            Print.logInfo("ACK received for command: " + lastAckCmd);
            return true;
        } catch (DBException dbe) {
            Print.logException("Unable to set Device.lastAck...", dbe);
            return false;
        }

    }

    // ------------------------------------------------------------------------

    public long getLastAckTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastAckTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastAckTime(long v)
    {
        this.setFieldValue(FLD_lastAckTime, v);
    }

    public void setLastAckTime(long v)
    {
        this._setLastAckTime(v);
        if (this.transport != null) {
            this.transport._setLastAckTime(v);
        }
    }

    // ------------------------------------------------------------------------

    /* DCS: General Config Mask (usage defined by specific DCS */
    public long getDcsConfigMask()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_dcsConfigMask);
        return (v != null)? v.longValue() : 0L;
    }

    public void setDcsConfigMask(long v)
    {
        this.setOptionalFieldValue(FLD_dcsConfigMask, v);
    }

    public String getDcsConfigString()
    {
        String v = (String)this.getOptionalFieldValue(FLD_dcsConfigString);
        return StringTools.trim(v);
    }

    public void setDcsConfigString(String v)
    {
        this.setOptionalFieldValue(FLD_dcsConfigString, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public boolean getSupportsDMTP()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_supportsDMTP);
        return (v != null)? v.booleanValue() : true;
    }

    public void setSupportsDMTP(boolean v)
    {
        this.setFieldValue(FLD_supportsDMTP, v);
    }

    public boolean supportsDMTP()
    {
        return this.getSupportsDMTP();
    }

    // ------------------------------------------------------------------------

    public int getSupportedEncodings()
    {
        Integer v = (Integer)this.getFieldValue(FLD_supportedEncodings);
        return (v != null)? v.intValue() : (int)Encodings.BINARY.getLongValue();
    }

    public void setSupportedEncodings(int v)
    {
        v &= (int)EnumTools.getValueMask(Encodings.class);
        if (v == 0) { v = (int)Encodings.BINARY.getLongValue(); }
        this.setFieldValue(FLD_supportedEncodings, v);
    }

    // ------------------------------------------------------------------------

    public int getUnitLimitInterval() // Minutes
    {
        Integer v = (Integer)this.getFieldValue(FLD_unitLimitInterval);
        return (v != null)? v.intValue() : 0;
    }

    public void setUnitLimitInterval(int v) // Minutes
    {
        this.setFieldValue(FLD_unitLimitInterval, v);
    }

    // ------------------------------------------------------------------------

    public int getMaxAllowedEvents()
    {
        Integer v = (Integer)this.getFieldValue(FLD_maxAllowedEvents);
        return (v != null)? v.intValue() : 1;
    }

    public void setMaxAllowedEvents(int v)
    {
        this.setFieldValue(FLD_maxAllowedEvents, v);
    }

    // ------------------------------------------------------------------------

    public DTProfileMask getTotalProfileMask()
    {
        DTProfileMask v = (DTProfileMask)this.getFieldValue(FLD_totalProfileMask);
        return v;
    }

    public void setTotalProfileMask(DTProfileMask v)
    {
        this.setFieldValue(FLD_totalProfileMask, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Total Connections per Interval */
    // The effective maximum value for this field is defined by the following:
    //   (org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK * this.getUnitLimitIntervalMinutes())
    public int getTotalMaxConn()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalMaxConn);
        return (v != null)? v.intValue() : 0;
    }

    public void setTotalMaxConn(int v)
    {
        this.setFieldValue(FLD_totalMaxConn, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Total Connections per Minute */
    // The effective maximum value for this field is defined by the constant:
    //   "org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK"
    public int getTotalMaxConnPerMin()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalMaxConnPerMin);
        return (v != null)? v.intValue() : 0;
    }

    public void setTotalMaxConnPerMin(int v)
    {
        this.setFieldValue(FLD_totalMaxConnPerMin, v);
    }

    // ------------------------------------------------------------------------

    public DTProfileMask getDuplexProfileMask()
    {
        DTProfileMask v = (DTProfileMask)this.getFieldValue(FLD_duplexProfileMask);
        return v;
    }

    public void setDuplexProfileMask(DTProfileMask v)
    {
        this.setFieldValue(FLD_duplexProfileMask, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Duplex Connections per Interval */
    // The effective maximum value for this field is defined by the following:
    //   (org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK * this.getUnitLimitIntervalMinutes())
    public int getDuplexMaxConn()
    {
        Integer v = (Integer)this.getFieldValue(FLD_duplexMaxConn);
        return (v != null)? v.intValue() : 0;
    }

    public void setDuplexMaxConn(int v)
    {
        this.setFieldValue(FLD_duplexMaxConn, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Duplex Connections per Minute */
    // The effective maximum value for this field is defined by the constant:
    //   "org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK"
    public int getDuplexMaxConnPerMin()
    {
        Integer v = (Integer)this.getFieldValue(FLD_duplexMaxConnPerMin);
        return (v != null)? v.intValue() : 0;
    }

    public void setDuplexMaxConnPerMin(int v)
    {
        this.setFieldValue(FLD_duplexMaxConnPerMin, v);
    }

    // ------------------------------------------------------------------------

    public long getLastDuplexConnectTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastDuplexConnectTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastDuplexConnectTime(long v)
    {
        this.setFieldValue(FLD_lastDuplexConnectTime, v);
    }

    public void setLastDuplexConnectTime(long v)
    {
        this._setLastDuplexConnectTime(v);
        if (this.transport != null) {
            this.transport._setLastDuplexConnectTime(v);
        }
    }

    // ------------------------------------------------------------------------

    public long getLastTotalConnectTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastTotalConnectTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastTotalConnectTime(long v)
    {
        this.setFieldValue(FLD_lastTotalConnectTime, v);
    }

    public void setLastTotalConnectTime(long v)
    {
        this._setLastTotalConnectTime(v);
        if (this.transport != null) {
            this.transport._setLastTotalConnectTime(v);
        }
    }

    public long getLastConnectTime()
    {
        return this.getLastTotalConnectTime();
    }

    public void setLastConnectTime(long v)
    {
        this.setLastTotalConnectTime(v);
    }

    // ------------------------------------------------------------------------

    public static boolean supportsFixedLocation()
    {
        return Device.getFactory().hasField(FLD_fixedLatitude);
    }
    
    public double getFixedLatitude()
    {
        return this.getOptionalFieldValue(FLD_fixedLatitude, 0.0);
    }

    public void setFixedLatitude(double v)
    {
        this.setOptionalFieldValue(FLD_fixedLatitude, v);
    }

    public double getFixedLongitude()
    {
        return this.getOptionalFieldValue(FLD_fixedLongitude, 0.0);
    }

    public void setFixedLongitude(double v)
    {
        this.setOptionalFieldValue(FLD_fixedLongitude, v);
    }

    public boolean hasFixedLocation()
    {
        // we assume FLD_fixedLongitude exists if FLD_fixedLatitude exists
        return this.hasField(FLD_fixedLatitude); // && this.isValidFixedLocation();
    }

    public boolean isValidFixedLocation()
    {
        return GeoPoint.isValid(this.getFixedLatitude(), this.getFixedLongitude());
    }

    public GeoPoint getFixedLocation()
    {
        return new GeoPoint(this.getFixedLatitude(), this.getFixedLongitude());
    }

    // ------------------------------------------------------------------------

    public String getFixedAddress()
    {
        String v = StringTools.trim((String)this.getFieldValue(FLD_fixedAddress));
        return v;
    }

    public void setFixedAddress(String v)
    {
        this.setFieldValue(FLD_fixedAddress, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getFixedContactPhone()
    {
        String v = StringTools.trim((String)this.getFieldValue(FLD_fixedContactPhone));
        return v;
    }

    public void setFixedContactPhone(String v)
    {
        this.setFieldValue(FLD_fixedContactPhone, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public long getFixedServiceTime()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_fixedServiceTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setFixedServiceTime(long v)
    {
        this.setOptionalFieldValue(FLD_fixedServiceTime, v);
    }

    // ------------------------------------------------------------------------

    /* returns true if FLD_activeCorridor field exists */
    public static boolean supportsActiveCorridor()
    {
        return Device.getFactory().hasField(FLD_activeCorridor);
    }

    /* get the current active GeoCorridor */
    public String getActiveCorridor()
    {
        String v = (String)this.getOptionalFieldValue(FLD_activeCorridor);
        return StringTools.trim(v);
    }

    /* get the current active GeoCorridor */
    public boolean hasActiveCorridor()
    {
        return !StringTools.isBlank(this.getActiveCorridor());
    }

    /* set the current active GeoCorridor */
    public void setActiveCorridor(String v)
    {
        this.setOptionalFieldValue(FLD_activeCorridor, StringTools.trim(v));
    }
    
    /* return list of GeoCorridors */
    public static String[] getCorridorIDsForAccount(String acctId)
    {

        /* GeoCorridor Class */
        Class gcClass = null;
        try {
            gcClass = Class.forName(DBConfig.PACKAGE_RULE_TABLES_ + "GeoCorridor");
        } catch (Throwable th) { // ClassNotFoundException
            return null;
        }

        /* Method action */
        MethodAction gcListMeth = null;
        try {
            gcListMeth = new MethodAction(gcClass, "getCorridorIDsForAccount", String.class);
        } catch (Throwable th) { // NoSuchMethodException, ClassNotFoundException
            return null;
        }

        /* get list */
        try {
            return (String[])gcListMeth.invoke(acctId);
        } catch (DBException dbe) {
            Print.logError("DBException: " + dbe);
            return null;
        } catch (Throwable th) {
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /* return true if the Device record support the Periodic Maintenance fields */
    public static boolean supportsPeriodicMaintenance()
    {
        return Device.getFactory().hasField(FLD_maintOdometerKM0);
    }

    // ---------------

    public static int getPeriodicMaintOdometerCount()
    {
        return 2;
    }

    public double getMaintOdometerKM(int ndx)
    {
        switch (ndx) {
            case 0 : return this.getMaintOdometerKM0();
            case 1 : return this.getMaintOdometerKM1();
            default: return 0.0;
        }
    }

    public void resetMaintOdometerKM(int ndx)
    {
        switch (ndx) {
            case 0 : this.resetMaintOdometerKM0(); break;
            case 1 : this.resetMaintOdometerKM1(); break;
        }
    }

    public double getMaintIntervalKM(int ndx)
    {
        switch (ndx) {
            case 0 : return this.getMaintIntervalKM0();
            case 1 : return this.getMaintIntervalKM1();
            default: return 0.0;
        }
    }

    public boolean isMaintenanceDueKM(int ndx, double deltaKM)
    {
        if (Device.supportsPeriodicMaintenance()) {
            double odomKM = this.getLastOdometerKM();
            if (odomKM > 0.0) {
                double lastKM = this.getMaintOdometerKM(ndx);
                double intvKM = this.getMaintIntervalKM(ndx);
                if ((odomKM + deltaKM) >= (lastKM + intvKM)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------

    /* get the maintenance distance limit */
    public double getMaintIntervalKM0()
    {
        return this.getOptionalFieldValue(FLD_maintIntervalKM0, 0.0);
    }

    /* set the maintenance distance limit */
    public void setMaintIntervalKM0(double v)
    {
        this.setOptionalFieldValue(FLD_maintIntervalKM0, v);
    }

    /* get the maintenance service odometer value */
    public double getMaintOdometerKM0()
    {
        return this.getOptionalFieldValue(FLD_maintOdometerKM0, 0.0);
    }

    /* set the maintenance service odometer value */
    public void setMaintOdometerKM0(double v)
    {
        if (v < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_maintOdometerKM0, ((v >= 0.0)? v : 0.0));
        }
    }

    /* reset the maintenance service odometer value */
    public void resetMaintOdometerKM0()
    {
        this.setMaintOdometerKM0(this.getLastOdometerKM());
        this.addOtherChangedFieldNames(Device.FLD_maintOdometerKM0);
    }

    // ---------------

    /* get the maintenance distance limit */
    public double getMaintIntervalKM1()
    {
        return this.getOptionalFieldValue(FLD_maintIntervalKM1, 0.0);
    }

    /* set the maintenance distance limit */
    public void setMaintIntervalKM1(double v)
    {
        this.setOptionalFieldValue(FLD_maintIntervalKM1, v);
    }

    /* get the maintenance service odometer value */
    public double getMaintOdometerKM1()
    {
        return this.getOptionalFieldValue(FLD_maintOdometerKM1, 0.0);
    }

    /* set the maintenance service odometer value */
    public void setMaintOdometerKM1(double v)
    {
        if (v < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_maintOdometerKM1, ((v >= 0.0)? v : 0.0));
        }
    }

    /* reset the maintenance service odometer value */
    public void resetMaintOdometerKM1()
    {
        this.setMaintOdometerKM1(this.getLastOdometerKM());
        this.addOtherChangedFieldNames(Device.FLD_maintOdometerKM0);
    }

    // ---------------

    public static int getPeriodicMaintEngHoursCount()
    {
        return 1;
    }

    public double getMaintEngHoursHR(int ndx)
    {
        switch (ndx) {
            case 0 : return this.getMaintEngHoursHR0();
            default: return 0.0;
        }
    }

    public void resetMaintEngHoursHR(int ndx)
    {
        switch (ndx) {
            case 0 : this.resetMaintEngHoursHR0(); break;
        }
    }

    public double getMaintIntervalHR(int ndx)
    {
        switch (ndx) {
            case 0 : return this.getMaintIntervalHR0();
            default: return 0.0;
        }
    }

    public boolean isMaintenanceDueHR(int ndx, double deltaHR)
    {
        if (Device.supportsPeriodicMaintenance()) {
            double engHrs = this.getLastEngineHours();
            if (engHrs > 0.0) {
                double lastHR = this.getMaintEngHoursHR(ndx);
                double intvHR = this.getMaintIntervalHR(ndx);
                if ((engHrs + deltaHR) >= (lastHR + intvHR)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------

    /* get the maintenance hours limit */
    public double getMaintIntervalHR0()
    {
        return this.getOptionalFieldValue(FLD_maintIntervalHR0, 0.0);
    }

    /* set the maintenance hours limit */
    public void setMaintIntervalHR0(double v)
    {
        this.setOptionalFieldValue(FLD_maintIntervalHR0, v);
    }

    /* get the maintenance service hours value */
    public double getMaintEngHoursHR0()
    {
        return this.getOptionalFieldValue(FLD_maintEngHoursHR0, 0.0);
    }

    /* set the maintenance service hours value */
    public void setMaintEngHoursHR0(double v)
    {
        if (v < this.getMaxRuntimeHours()) {
            this.setOptionalFieldValue(FLD_maintEngHoursHR0, ((v >= 0.0)? v : 0.0));
        }
    }

    /* reset the maintenance service hours value */
    public void resetMaintEngHoursHR0()
    {
        this.setMaintEngHoursHR0(this.getLastEngineHours());
        this.addOtherChangedFieldNames(Device.FLD_maintEngHoursHR0);
    }

    // ---------------

    /* get maintenance notes */
    public String getMaintNotes()
    {
        String v = (String)this.getOptionalFieldValue(FLD_maintNotes);
        return StringTools.trim(v);
    }

    /* set maintenance notes */
    public void setMaintNotes(String v)
    {
        this.setOptionalFieldValue(FLD_maintNotes, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    private RTProperties customAttrRTP = null;
    private Collection<String> customAttrKeys = null;

    /* get the custom attributes as a String */
    public String getCustomAttributes()
    {
        String v = (String)this.getOptionalFieldValue(FLD_customAttributes);
        return StringTools.trim(v);
    }

    /* set the custom attributes as a String */
    public void setCustomAttributes(String v)
    {
        this.setOptionalFieldValue(FLD_customAttributes, StringTools.trim(v));
        this.customAttrRTP  = null;
        this.customAttrKeys = null;
    }

    /* get custom attributes a an RTProperties */
    public RTProperties getCustomAttributesRTP()
    {
        if (this.customAttrRTP == null) {
            this.customAttrRTP = new RTProperties(this.getCustomAttributes());
        }
        return this.customAttrRTP;
    }

    /* get the custom attributes keys */
    public Collection<String> getCustomAttributeKeys()
    {
        if (this.customAttrKeys == null) {
            this.customAttrKeys = this.getCustomAttributesRTP().getPropertyKeys(null);
        }
        return this.customAttrKeys;
    }

    /* get the custom attributes as a String */
    public String getCustomAttribute(String key)
    {
        return this.getCustomAttributesRTP().getString(key,null);
    }

    /* get the custom attributes as a String */
    public String setCustomAttribute(String key, String value)
    {
        return this.getCustomAttributesRTP().getString(key,value);
    }

    // ------------------------------------------------------------------------

    /* get WorkOrder ID */
    public String getWorkOrderID()
    {
        String v = (String)this.getOptionalFieldValue(FLD_workOrderID);
        return StringTools.trim(v);
    }

    /* set WorkOrder ID */
    public void setWorkOrderID(String v)
    {
        this.setOptionalFieldValue(FLD_workOrderID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    
    public boolean supportsDataPushTime()
    {
        return Device.getFactory().hasField(FLD_lastDataPushTime);
    }

    public long getLastDataPushTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastDataPushTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setLastDataPushTime(long v)
    {
        this.setFieldValue(FLD_lastTotalConnectTime, v);
    }

    // ------------------------------------------------------------------------

    public long getLastEventCreateMillis()
    {
        Long v = (Long)this.getFieldValue(FLD_lastEventCreateMillis);
        return (v != null)? v.longValue() : 0L;
    }

    public void setLastEventCreateMillis(long v)
    {
        this.setFieldValue(FLD_lastEventCreateMillis, v);
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setIsActive(true);
        this.setDescription(NEW_DEVICE_NAME_ + " [" + this.getDeviceID() + "]");
        this.setIgnitionIndex(-1);
        // Rules-Engine Allow Notification
        if (Device.hasRuleFactory()) {
            this.setAllowNotify(true);
        }
        this.setNotifyAction(RuleFactory.ACTION_DEFAULT);
        // BorderCrossing 
        if (Device.supportsBorderCrossing()) {
            this.setBorderCrossing(Device.BorderCrossingState.ON);
        }
        // DataTransport attributes below
        this.setSupportedEncodings(Transport.DEFAULT_ENCODING);
        this.setTotalMaxConn(Transport.DEFAULT_TOTAL_MAX_CONNECTIONS);
        this.setDuplexMaxConn(Transport.DEFAULT_DUPLEX_MAX_CONNECTIONS);
        this.setUnitLimitInterval(Transport.DEFAULT_UNIT_LIMIT_INTERVAL_MIN); // Minutes
        this.setTotalMaxConnPerMin(Transport.DEFAULT_TOTAL_MAX_CONNECTIONS_PER_MIN);
        this.setDuplexMaxConnPerMin(Transport.DEFAULT_DUPLEX_MAX_CONNECTIONS_PER_MIN);
        this.setMaxAllowedEvents(Transport.DEFAULT_MAX_ALLOWED_EVENTS);
        // other defaults
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* DataTransport interface */
    public String getAssocAccountID()
    {
        return this.getAccountID();
    }

    /* DataTransport interface */
    public String getAssocDeviceID()
    {
        return this.getDeviceID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return a list of supported commands */
    public DCServerConfig getDCServerConfig()
    {
        return DCServerFactory.getServerConfig(this.getDeviceCode());
    }

    /**
    *** Return a list of supported commands
    *** @param privLabel  The current PrivateLabel instance
    *** @param user       The current user instance
    *** @param type       The command location type (ie. "map", "admin", ...)
    *** @return A map of the specified commands
    **/
    public Map<String,String> getSupportedCommands(BasicPrivateLabel privLabel, User user, 
        String type)
    {
        DCServerConfig dcs = this.getDCServerConfig();
        return (dcs != null)? dcs.getCommandDescriptionMap(privLabel,user,type) : null;
    }

    // ------------------------------------------------------------------------

    /* return true if 'ping' is supported */
    public boolean isPingSupported(BasicPrivateLabel privLabel, User user)
    {
        
        /* check ACL */
        DCServerConfig dcs = this.getDCServerConfig();
        if ((privLabel != null) && (dcs != null) && !privLabel.hasWriteAccess(user, dcs.getCommandsAclName())) {
            Print.logDebug("User does not have access to device command handler");
            return false;
        }

        /* PingDispatcher */
        if (Device.hasPingDispatcher()) {
            boolean supported = Device.getPingDispatcher().isPingSupported(this);
            Print.logDebug("Device "+this.getDeviceID()+" isPingSupported = " + supported);
            return supported;
        } else {
            Print.logDebug("Device "+this.getDeviceID()+" does not have a command-handler");
            return false;
        }
        
    }

    /* dispatch device command */ // "sendCommand"
    public boolean sendDeviceCommand(String cmdType, String cmdName, String cmdArgs[])
    {
        String ct = !StringTools.isBlank(cmdType)? cmdType : DCServerConfig.COMMAND_CONFIG;

        /* DCServerConfig */
        DCServerConfig dcs = this.getDCServerConfig();
        if (dcs != null) {
            // a DCServerConfig is defined
            RTProperties resp = DCServerFactory.sendServerCommand(this, ct, cmdName, cmdArgs);
            Print.logInfo("Ping Response: " + resp);
            boolean sentOK = DCServerFactory.isCommandResultOK(resp);
            return sentOK;
        }

        /* PingDispatcher */
        if (Device.hasPingDispatcher()) {
            boolean sentOK = Device.getPingDispatcher().sendDeviceCommand(this, ct, cmdName, cmdArgs);
            return sentOK;
        } else {
            return false;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean  allowSlowReverseGeocode = true;

    /**
    *** Enabled/Disabled slow reverse-geocoding (default is to allow)
    *** @param allow  True to allow, false to dis-allow
    **/
    public static void SetAllowSlowReverseGeocoding(boolean allow)
    {
        Device.allowSlowReverseGeocode = allow;
    }

    /**
    *** Returns true is slow reverse-geocoding is allowed
    *** @return  True if allowed, false otherwise
    **/
    public static boolean GetAllowSlowReverseGeocoding()
    {
        return Device.allowSlowReverseGeocode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean  ENABLE_LOAD_TESTING     = true;
    private static Object   loadTestingLock         = new Object();
    private static DateTime loadTestingTime         = null;
    private static long     loadTestingCount        = 0L;

    /**
    *** Gets the number of events between the specified timestamps (inclusive)
    *** @param timeStart  The starting timestamp
    *** @param timeEnd    The ending timestamp
    *** @return The number of events between the specified timestamps (inclusive)
    **/
    public long getEventCount(long timeStart, long timeEnd)
        throws DBException
    {
        long count = EventData.getRecordCount(
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd);
        return count;
    }

    /**
    *** Gets the total number of events for this Device/Vehicle
    *** @return The total number of EventData records for this Device
    **/
    public long getEventCount()
        throws DBException
    {
        return this.getEventCount(-1L, -1L);
    }
    
    /**
    *** Prints the event information to the log file
    *** @param ev  The event to log
    **/
    public void log_EventData(int logLevel, EventData ev)
    {

        /* assemble log info */
        StringBuffer sb = new StringBuffer();
        sb.append("Event: ");
        if (ev != null) {
            // Event: 2011/07/11|12:34:56|PST, account|device, 0xF020|Location|geozone, latitude|longitude,
            int sc = ev.getStatusCode();
            DateTime dt = new DateTime(ev.getTimestamp());
            String zone = ev.getGeozoneID();

            /* date|time */
            sb.append(dt.format("yyyy/MM/dd|HH:mm:ss|zzz")).append(", ");

            /* account|device */
            sb.append(ev.getAccountID()).append("|").append(ev.getDeviceID()).append(", ");

            /* status code */
            sb.append(StatusCodes.GetHex(sc)).append("|").append(StatusCodes.GetDescription(sc,null));
            if (!StringTools.isBlank(zone)) {
                sb.append("|").append(zone);
            } else
            if ((sc == StatusCodes.STATUS_GEOFENCE_ARRIVE) ||
                (sc == StatusCodes.STATUS_GEOFENCE_ARRIVE)   ) {
                sb.append("|?");
            }
            sb.append(", ");

            /* lat|lon */
            sb.append(ev.getGeoPoint().toString('|'));

        } else {
            // Event: null

            // create log message
            sb.append("null");

        }

        /* log */
        Print.log(logLevel, sb.toString());

    }

    /**
    *** Insert event into EventData table
    *** @param evdb  The EventData record to insert
    *** @return True if successful, false otherwise
    **/
    public boolean insertEventData(EventData evdb)
    {

        /* log event insertion */
        if (Device.LogEventDataInsertion >= Print.LOG_WARN) {
            // LOG_WARN, LOG_INFO, LOG_DEBUG
            this.log_EventData(Device.LogEventDataInsertion, evdb);
        }

        /* insert event */
        if (!this._insertEventData(evdb)) {
            // event was ignored
            return false;
        }

        /* check for synthesized GeoCorridor events */
        int sc = evdb.getStatusCode();
        if (sc == StatusCodes.STATUS_GEOFENCE_ARRIVE) {
            // check for GeoCorridor deactivation
            Geozone zone = evdb.getGeozone();
            if (this.hasActiveCorridor() && (zone != null) && zone.isCorridorEnd(evdb)) {
                 this.setActiveCorridor("");            // FLD_activeCorridor
                 // TODO: insert STATUS_CORRIDOR_INACTIVE
                 // evdb.setStatusCode(StatusCodes.CORRIDOR_INACTIVE);
                 // this._insertEventData(evdb);
                 // Print.logInfo("Synthesized Corridor deactivation event");
            }
        } else
        if (sc == StatusCodes.STATUS_GEOFENCE_DEPART) {
            // check for GeoCorridor activation
            Geozone zone = evdb.getGeozone();
            if ((zone != null) && zone.isCorridorStart(evdb)) {
                String corridorID = zone.getCorridorID();
                this.setActiveCorridor(corridorID);     // FLD_activeCorridor
                 // TODO: insert STATUS_CORRIDOR_ACTIVE
                 // evdb.setStatusCode(StatusCodes.STATUS_CORRIDOR_ACTIVE);
                 // this._insertEventData(evdb);
                 // Print.logInfo("Synthesized Corridor activation event");
            }
        }

        return true;
    }

    /**
    *** Insert event into EventData table
    *** @param evdb  The EventData record to insert
    *** @return True if successful, false otherwise
    **/
    protected boolean _insertEventData(final EventData evdb)
    {
        // Notes:
        // 1) This incoming EventData record is populated, but hasn't been saved
        // 2) This Device record at this point _should_ still contain old/last field values

        /* invalid EventData? */
        if (evdb == null) {
            //Print.logError("EventData is null");
            return false;
        }

        /* set device */
        evdb.setDevice(this);

        /* Transport ID */
        evdb.setTransportID(this.getTransportID());
        
        /* Event time check */
        long eventTime = evdb.getTimestamp();
        if (eventTime > 5000000000L) {
            // Event time might be specified in milliseconds
            Print.logWarn("EventData time is invalid (too large): "+eventTime+" [ignoring record]");
            return false;
        } else
        if (eventTime <= 0L) {
            // Event time is invalid
            Print.logWarn("EventData time is invalid (<=0): "+eventTime+" [ignoring record]");
            return false;
        }

        /* check for future timestamp */
        int futureDateAction = Device.futureEventDateAction();
        if (futureDateAction != FUTURE_DATE_DISABLED) {
            long maxFutureSec = Device.futureEventDateMaximumSec();
            if (maxFutureSec > 0L) { // must be greater-than 0
                long nowTime = DateTime.getCurrentTimeSec();
                long maxTime = nowTime + maxFutureSec;
                long evTime  = eventTime;
                if (evTime > maxTime) {
                    if (futureDateAction == FUTURE_DATE_IGNORE) {
                        // ignore this record
                        Print.logWarn("Invalid EventData future time: "+new DateTime(evTime)+" [ignoring record per configuration]");
                        return false;
                    } else
                    if (futureDateAction == FUTURE_DATE_TRUNCATE) {
                        // truncate date/time
                        long truncTime = nowTime; // maxTime;
                        Print.logWarn("Invalid EventData future time: "+new DateTime(evTime)+" [set/truncate to "+new DateTime(truncTime)+"]");
                        evdb.setTimestamp(truncTime);
                    } else {
                        // should not occur (just continue)
                        Print.logWarn("Invalid EventData future time: "+new DateTime(evTime)+" [unexpected action "+futureDateAction+"]");
                    }
                }
            }
        }

        /* check for invalid speed (beyond reasonable maximum) */
        int invalidSpeedAction = Device.invalidSpeedAction();
        if (invalidSpeedAction != INVALID_SPEED_DISABLED) {
            double maxSpeedKPH = Device.invalidSpeedMaximumKPH();
            if (maxSpeedKPH > 0.0) { // must be greater-than 0.0
                double evSpeedKPH = evdb.getSpeedKPH();
                if (evSpeedKPH > maxSpeedKPH) {
                    if (invalidSpeedAction == INVALID_SPEED_IGNORE) {
                        // ignore this record
                        Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [ignoring record per configuration]");
                        return false;
                    } else
                    if (invalidSpeedAction == INVALID_SPEED_TRUNCATE) {
                        // truncate speed to maximum
                        Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [set/truncate to "+maxSpeedKPH+"]");
                        evdb.setSpeedKPH(maxSpeedKPH);
                    } else {
                        // should not occur (just continue)
                        Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [unexpected action "+invalidSpeedAction+"]");
                    }
                }
            }
        }

        /* no status code? */
        if (evdb.getStatusCode() == StatusCodes.STATUS_NONE) {
            // '0' status codes are quietly consumed/ignored.
            if (ENABLE_LOAD_TESTING) {
                // This section is for load testing.
                if (loadTestingTime == null) {
                    synchronized (loadTestingLock) {
                        if (loadTestingTime == null) { loadTestingTime = new DateTime(/*tz*/); }
                    }
                }
                long deltaSec = DateTime.getCurrentTimeSec() - loadTestingTime.getTimeSec();
                if (deltaSec > 60) {
                    // reset every minute
                    synchronized (loadTestingLock) {
                        loadTestingTime = new DateTime(/*tz*/);
                        loadTestingCount = 0L;
                        deltaSec = 0L;
                    }
                }
                loadTestingCount++;
                double eps = (deltaSec > 0L)? ((double)loadTestingCount / (double)deltaSec) : loadTestingCount;
                if ((loadTestingCount % 50) == 0) {
                    System.err.println("EventData LoadTest (" + eps + " ev/sec)");
                }
            }
            return true;
        }

        /* extended EventData record update */
        int extUpdate = EXT_UPDATE_NONE;

        /* PCell Tower GPS location */
        if (!evdb.isValidGeoPoint() && evdb.canUpdateCellTowerLocation()) {
            boolean ALWAYS_UPDATE_CELLGPS = true;
            CellTower dct = !ALWAYS_UPDATE_CELLGPS? this.getLastServingCellTower() : null;
            if (dct == null) {
                // No last CellID
                extUpdate |= EXT_UPDATE_CELLGPS;
            } else
            if (!dct.equals(evdb.getServingCellTower())) {
                // Last CellID does not match current CellID
                extUpdate |= EXT_UPDATE_CELLGPS;
            } else {
                // We have a last cellID
                MobileLocation ml = dct.getMobileLocation();
                if ((ml != null) && ml.isValid()) {
                    GeoPoint mgp = ml.getGeoPoint();
                    double   acc = ml.getAccuracy();
                    evdb.setCellGeoPoint(mgp);
                    evdb.setCellAccuracy(acc);
                    Print.logInfo("Using cached CellTower location: " + mgp + " [+/- " + acc + " meters]");
                } else {
                    // No MobileLocation means that the CellID did not have a known location
                    Print.logInfo("Using cached CellTower location: no location");
                }
            }
        }

        /* set geozone / reverse-geocode */
        try {
            Set<String> updFields = evdb.updateAddress(true/*fastOnly*/);
            if (updFields != null) {
                BasicPrivateLabel privLabel = this.getAccount().getPrivateLabel();
                ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
                String rgName = (rgp != null)? rgp.getName() : "???";
                Print.logInfo("EventData address: [%s/%s:%s] %s: %s",
                    this.getAccountID(), this.getDeviceID(), rgName,
                    evdb.getGeoPoint().toString(), evdb.getAddress());
                // we don't care about the names of the fields updated, since all fields will be saved below
            }
        } catch (SlowOperationException soe) {
            // The address update has not been performed because the operation would have
            // taken too long [per 'isFastOperation()' method in ReverseGeocodeProvider instance].
            // This address update will need to be queued for background processing.
            if (Device.allowSlowReverseGeocode) {
                extUpdate |= EXT_UPDATE_ADDRESS;
            }
        } catch (Throwable th) {
            Print.logException("Address update error", th);
        }

        /* stateline border-crossing check */
        //if (this.getBorderCrossing() == Device.BorderCrossingState.ON.getIntValue()) {
        //   // border-crossing is always considered a slow operation
        //   //extUpdate |= EXT_UPDATE_BORDER;
        //}

        /* save EventData record */
        try {
            evdb.save(); // insert();
            // may be re-saved after deferred reverse-geocode
        } catch (DBException dbe) {
            // save failed
            Print.logError("EventData save failed: " + dbe);
            return false;
        }

        /* background processes */
        if (extUpdate != EXT_UPDATE_NONE) {
            // queue for background processing
            final int extUpd = extUpdate;
            Runnable job = new Runnable() {
                public void run() {
                    Device.this._postEventInsertionProcessing(evdb, extUpd);
                }
            };
            BackgroundThreadPool.run(job);
            Print.logDebug("Address update queued for background operation");
        } else {
            // check event rules now and perform appropriate action if necessary
            //this.checkEventRules(evdb);
        }

        /* check rules */
        // "checkEventRules" may recursively call "_insertEventData"
        if (this.checkEventRules(evdb)) { 
            // Fields may have changed: (NOTE: not yet saved)
            //   FLD_lastNotifyTime
            //   FLD_lastNotifyCode
        }

        /* update fields to reflect this event */
        // NOTE: not yet saved!
        if (evdb.isValidGeoPoint()) {
            // update last valid location
            this.setLastValidLatitude(evdb.getLatitude());      // FLD_lastValidLatitude
            this.setLastValidLongitude(evdb.getLongitude());    // FLD_lastValidLongitude
            this.setLastValidHeading(evdb.getHeading());        // FLD_lastValidHeading
            this.setLastGPSTimestamp(evdb.getTimestamp());      // FLD_lastGPSTimestamp
        }
        if (evdb.getOdometerKM() > 0.0) {
            this.setLastOdometerKM(evdb.getOdometerKM());       // FLD_lastOdometerKM
        }
        if (evdb.getFuelTotal() > 0.0) {
            this.setLastFuelTotal(evdb.getFuelTotal());         // FLD_lastFuelTotal
        }
        if (evdb.getEngineHours() > 0.0) {
            this.setLastEngineHours(evdb.getEngineHours());     // FLD_lastEngineHours
        }
        if (evdb.getBatteryLevel() > 0.0) {
            this.setLastBatteryLevel(evdb.getBatteryLevel());   // FLD_lastBatteryLevel
        }
        if (evdb.getFuelLevel() > 0.0) { // EventData may not support fuelLevel
            this.setLastFuelLevel(evdb.getFuelLevel());         // FLD_lastFuelLevel
        }
        if (evdb.getOilLevel() > 0.0) { // EventData may not support oilLevel
            this.setLastOilLevel(evdb.getOilLevel());         // FLD_lastOilLevel
        }

        /* return success */
        return true;

    }

    /* background post-processing: address reverse-geocoding, and rule checking */
    private void _postEventInsertionProcessing(EventData evdb, int extUpdate)
    {
        Set<String> updatedEvFields = null;

        /* cell tower GPS location */
        if ((extUpdate & EXT_UPDATE_CELLGPS) != 0) {
            Set<String> updf = evdb.updateCellTowerLocation();
            if (updf != null) {
                // MobileLocation was successful (but may not have returned a valid location)
                if (updatedEvFields == null) { updatedEvFields = new HashSet<String>(); }
                updatedEvFields.addAll(updf);
                CellTower sct = evdb.getServingCellTower();
                if (sct != null) {
                    this.setLastServingCellTower(sct); // FLD_lastCellServingInfo
                    try {
                        this.update(Device.FLD_lastCellServingInfo);
                    } catch (DBException dbe) {
                        Print.logError("Unable to update Device: " + dbe);
                    }
                }
            }
        }

        /* address */
        if ((extUpdate & EXT_UPDATE_ADDRESS) != 0) {
            try {
                Set<String> updf = evdb.updateAddress(false/*!fastOnly*/);
                if (updf != null) {
                    if (updatedEvFields == null) { updatedEvFields = new HashSet<String>(); }
                    updatedEvFields.addAll(updf);
                }
            } catch (SlowOperationException soe) {
                // this will not occur ('fastOnly' is false)
            }
        }

        /* stateline border-crossing check here */
        // check border-crossing in nightly cron

        /* update */
        if (!ListTools.isEmpty(updatedEvFields)) {
            try {
                evdb.update(updatedEvFields);
                Print.logInfo("EventData address: [%s/%s] %s: %s",
                    this.getAccountID(), this.getDeviceID(),
                    evdb.getGeoPoint().toString(), evdb.getAddress());
            } catch (DBException dbe) {
                Print.logError("EventData update error: " + dbe);
            }
        }

        /* rule check */
        //Cannot defer rule check to here!!!
        //Rule triggers may be based on values which may be changing in the Device record,
        //which will have already changed by the time we get here!
        //this.checkEventRules(evdb);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Delete old events from EventData table.
    *** @param priorToTime  EventData records up to (but excluding) this timestamp will be deleted.
    *** @return The number of records deleted
    **/
    public long deleteEventsPriorTo(long priorToTime)
        throws DBException
    {

        /* valid timestamp? */
        if (priorToTime <= 0L) {
            throw new DBException("Invalid 'priorTo' timestamp specified: " + priorToTime);
        }

        /* starting event count */
        long delEventCount = this.getEventCount(-1L, priorToTime - 1L);

        /* delete all EventData entries prior to the specified date */
        // [DELETE FROM EventData WHERE accountID='account' and deviceID='device' and timestamp<priorToTime]
        DBConnection dbc = null;
        try {
            DBDelete edel = new DBDelete(EventData.getFactory());
            DBWhere  ewh  = edel.createDBWhere();
            edel.setWhere(ewh.WHERE_(
                ewh.AND(
                    ewh.EQ(EventData.FLD_accountID, this.getAccountID()),
                    ewh.EQ(EventData.FLD_deviceID , this.getDeviceID()),
                    ewh.LT(EventData.FLD_timestamp, priorToTime)
                )
            ));
            Print.logInfo("EventData delete command: " + edel);
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(edel.toString());
        } catch (SQLException sqe) {
            throw new DBException("Deleting EventData records", sqe);
        } finally {
            DBConnection.release(dbc);
        }

        /* number of records deleted (or supposed to have been deleted) */
        return delEventCount;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String[] DefaultUpdatedFieldsList = new String[] {
        Device.FLD_deviceCode,              // serverID
        Device.FLD_imeiNumber,
        Device.FLD_ipAddressCurrent,
        Device.FLD_remotePortCurrent,
        Device.FLD_listenPortCurrent,
        Device.FLD_lastInputState,
        Device.FLD_lastBatteryLevel,
        Device.FLD_lastFuelLevel,           
        Device.FLD_lastFuelTotal,
        Device.FLD_lastOilLevel,           
        Device.FLD_lastValidLatitude,
        Device.FLD_lastValidLongitude,
        Device.FLD_lastValidHeading,
        Device.FLD_lastGPSTimestamp,
        Device.FLD_lastOdometerKM,
        Device.FLD_lastEngineHours,
        Device.FLD_lastTotalConnectTime,
      //Device.FLD_driverID,                // CalAmp, ...
      //Device.FLD_lastNotifyTime,          // optional field (should only be updated if changed)
      //Device.FLD_lastNotifyCode,          // optional field (should only be updated if changed)
      //Device.FLD_activeCorridor           // optional field (should only be updated if changed)
    };
    
    private static final Set<String> DefaultUpdatedFieldsSet = 
        ListTools.toSet(DefaultUpdatedFieldsList);

    public Set<String> _createChangedFieldsSet(Set<String> flds)
        throws DBException
    {
        Set<String> otherSet = this.getOtherChangedFieldNames();
        if ((flds == null) && (otherSet == null)) {
            return DefaultUpdatedFieldsSet; // minor optimization
        } else {
            // make a new copy and add the additional fields
            Set<String> updFields = ListTools.toSet(DefaultUpdatedFieldsList);
            if (flds != null) {
                ListTools.toSet(flds/*from*/, updFields/*to*/);
            }
            if (otherSet != null) {
                ListTools.toSet(otherSet/*from*/, updFields/*to*/);
            }
            return updFields;
        }
    }

    public Set<String> _createChangedFieldsSet(String... flds)
        throws DBException
    {
        Set<String> otherSet = this.getOtherChangedFieldNames();
        if ((flds == null) && (otherSet == null)) {
            return DefaultUpdatedFieldsSet; // minor optimization
        } else {
            Set<String> updFields = ListTools.toSet(DefaultUpdatedFieldsList);
            if (flds != null) {
                ListTools.toSet(flds, updFields);
            }
            if (otherSet != null) {
                ListTools.toSet(otherSet, updFields);
            }
            return updFields;
        }
    }

    // --------------------------------

    private Set<String> otherChangedFieldsSet = null;
    
    private Set<String> _createOtherChangedFieldsSet()
    {
        if (this.otherChangedFieldsSet == null) {
            this.otherChangedFieldsSet = new HashSet<String>();
        }
        return this.otherChangedFieldsSet;
    }
    
    public boolean hasChangedFieldNames()
    {
        return (this.otherChangedFieldsSet != null);
    }

    public Set<String> getOtherChangedFieldNames()
    {
        return this.otherChangedFieldsSet; // may be null
    }

    public void addOtherChangedFieldNames(Set<String> flds)
    {
        if (flds != null) {
            ListTools.toSet(flds, this._createOtherChangedFieldsSet());
        }
    }
    
    public void addOtherChangedFieldNames(String... flds)
    {
        if (flds != null) {
            ListTools.toSet(flds, this._createOtherChangedFieldsSet());
        }
    }

    public void updateOtherChangedEventFields()
        throws DBException
    {
        if (this.otherChangedFieldsSet != null) {
            this.update(this.otherChangedFieldsSet);
        }
    }

    // --------------------------------

    public void updateChangedEventFields()
        throws DBException
    {
        this.update(_createChangedFieldsSet((String[])null));
    }

    public void updateChangedEventFields(Set<String> flds)
        throws DBException
    {
        this.update(_createChangedFieldsSet(flds));
    }

    public void updateChangedEventFields(String... flds)
        throws DBException
    {
        this.update(_createChangedFieldsSet(flds));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* save connection statistics */
    public void insertSessionStatistic(long startTime, String ipAddr, boolean isDuplex, long bytesRead, long bytesWritten, long evtsRecv)
    {
        // save session statistics
        SessionStatsFactory csf = Device.getSessionStatsFactory();
        if (csf != null) {
            try {
                csf.addSessionStatistic(this,startTime,ipAddr,isDuplex,bytesRead,bytesWritten,evtsRecv);
            } catch (DBException dbe) {
                Print.logError("Session statistic: " + dbe);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* validate rule selector syntax */
    public boolean checkSelectorSyntax(String selector)
    {
        if (StringTools.isBlank(selector)) {
            // a blank selector should always be valid
            return true;
        } else {
            RuleFactory ruleFact = Device.getRuleFactory();
            if (ruleFact != null) {
                return ruleFact.checkSelectorSyntax(selector);
            } else {
                Print.logWarn("No RuleFactory defined");
                return false;
            }
        }
    }

    /* check new event for specific rule triggers */
    // Any special event rules checking should go here.
    protected boolean checkEventRules(EventData event)
    {

        /* no event? */
        if (event == null) {
            // we have no event, don't bother with the rest
            //Print.logDebug("No EventData record specified: " + this.getAccountID() + "/" + this.getDeviceID());
            return false;
        }
        int statusCode = event.getStatusCode();

        /* skip rule checks for rule-trigger events */
        // to prevent any potential for recursive rule triggers and event insertion
        if (StatusCodes.IsRuleTrigger(statusCode)) {
            return false;
        }

        /* synthesized events */
        boolean isSynthesizedEvent = event.getIsSynthesizedEvent();
        //if (isSynthesizedEvent) {
        //    return false;
        //}

        /* set device */
        // This provides an optimization so that any Account/Device requests on the EventData
        // record won't have to explicitly query the database to retrieve the Account/Device.
        event.setDevice(this);

        /* Entity attach/detach (if installed) */
        if (!isSynthesizedEvent && Device.hasEntityManager()) {
            Device.getEntityManager().insertEntityChange(event);
        }

        /* Fuel Manager (if installed) */
        if (!isSynthesizedEvent && Device.hasFuelManager()) {
            FuelManager fm = Device.getFuelManager();
            FuelManager.LevelChangeType lvlType = fm.insertFuelLevelChange(event);
            switch (lvlType) {
                case INCREASE:
                    if (statusCode != StatusCodes.STATUS_FUEL_REFILL) {
                        // insert synthesized fuel-level change event
                        int fuelCode = StatusCodes.STATUS_FUEL_REFILL;
                        EventData fuelEv = EventData.copySynthesizedEvent(event, fuelCode);
                        if (this.insertEventData(fuelEv)) { // recursive call
                            Print.logWarn("FuelManager: Added new Fuel 'REFILL' Event - " + fuelEv);
                        } else {
                            Print.logError("FuelManager: New Fuel 'REFILL' Event failed!");
                        }
                    }
                    break;
                case DECREASE:
                    if (statusCode != StatusCodes.STATUS_FUEL_THEFT) {
                        // insert synthesized fuel-level change event
                        int fuelCode = StatusCodes.STATUS_FUEL_THEFT;
                        EventData fuelEv = EventData.copySynthesizedEvent(event, fuelCode);
                        if (this.insertEventData(fuelEv)) { // recursive call
                            Print.logWarn("FuelManager: Added new Fuel 'THEFT' Event - " + fuelEv);
                        } else {
                            Print.logError("FuelManager: New Fuel 'THEFT' Event failed!");
                        }
                    }
                    break;
            }
        }

        /* device rule selector */
        String ruleSelector = (Device.CheckNotifySelector() && !Device.hasENRE())? 
            this.getNotifySelector() : null;

        /* check for rule factory */
        RuleFactory ruleFact = Device.getRuleFactory();
        if (ruleFact == null) {
            /* display message if a rule-selector has been specified */
            //if (!StringTools.isBlank(ruleSelector)) {
            //    Print.logWarn("No RuleFactory to process rule: " + ruleSelector);
            //} else {
            //    //Print.logDebug("RuleFactory not installed: " + this);
            //}
            return false;
        }

        /* notification not allowed for this device? */
        if (!this.allowNotify()) {
            /* display message if a rule-selector has been specified */
            //if (!StringTools.isBlank(ruleSelector)) {
            //    Print.logWarn("Notification disallowed [selector = " + ruleSelector + "] " + this);
            //} else {
            //    //Print.logDebug("Notification disallowed: " + this);
            //}
            return false;
        }
        
        /* accumulated action mask */
        int accumActionMask = RuleFactory.ACTION_NONE;

        /* check local email notification selector */
        // This executes a single selector-based rule.
        boolean didTrigger = false;
        if (!StringTools.isBlank(ruleSelector)) {
            Print.logDebug("Processing Device rule [selector = " + ruleSelector + "] " + this);
            int actionMask = ruleFact.executeSelector(ruleSelector, event);
            if (this._setDeviceAction(actionMask, event)) {
                didTrigger = true;
                accumActionMask |= actionMask;
            }
        }

        /* test statusCode rule/action list */
        // This method allows for a complete check of multiple rules
        {
            //Print.logDebug("Executing rules for event: " + this);
            int actionMask = ruleFact.executeRules(event);
            if (this._setDeviceAction(actionMask, event)) {
                didTrigger = true;
                accumActionMask |= actionMask;
            }
        }

        /* Geozone AutoNotify: convenience geozone arrive/depart notification */
        if (StatusCodes.IsGeozoneTransition(statusCode)) {
            Account account = this.getAccount();
            BasicPrivateLabel bpl = Account.getPrivateLabel(account);
            Geozone zone = event.getGeozone();
            if (account == null) {
                // unlikely
                Print.logError("Unable to determine account for Geozone email: " + this.getAccountID());
            } else
            if (bpl == null) {
                // unlikely
                Print.logError("Unable to determine Account PrivateLabel for Geozone email: " + this.getAccountID());
            } else
            if (zone == null) {
                // unlikely
                Print.logError("Geozone status code, but Geozone not found: " + this.getAccountID());
            } else
            if (zone.isAutoNotify()) {
                // IE: "VehicleName" arrived "DeviceDescription"
                I18N i18n = I18N.getI18N(Device.class, account.getLocale());

                /* time format */
                String timeFmt  = bpl.getDateFormat() + " " + bpl.getTimeFormat();
                TimeZone tmz    = Account.getTimeZone(account,DateTime.getGMTTimeZone()); 
                String timeStr  = new DateTime(event.getTimestamp()).format(timeFmt,tmz);

                /* device/geozone description */
                String devDesc  = this.getDescription();
                String zoneDesc = zone.getDescription();

                /* message subject/body */
                String subj = null;
                String body = null;
                if (statusCode == StatusCodes.STATUS_GEOFENCE_ARRIVE) {
                    subj = i18n.getString("Device.autoArriveMessage", "{0}: \"{1}\" arrived \"{2}\"",
                        new String[] { timeStr, devDesc, zoneDesc });
                    body = subj;
                } else
                if (statusCode == StatusCodes.STATUS_GEOFENCE_DEPART) {
                    subj = i18n.getString("Device.autoDepartMessage", "{0}: \"{1}\" departed \"{2}\"",
                        new String[] { timeStr, devDesc, zoneDesc });
                    body = subj;
                } else {
                    // unlikely
                    subj = i18n.getString("Device.autoGeozoneMessage", "{0}: \"{1}\" arrived/departed \"{2}\"",
                        new String[] { timeStr, devDesc, zoneDesc });
                    body = subj;
                }

                /* assemble recipient list */
                HashSet<String> recipients = new HashSet<String>();
                String acctEmail = account.getNotifyEmail();
                if (!StringTools.isBlank(acctEmail)) {
                    ListTools.toSet(StringTools.split(acctEmail,','),recipients);
                }
                String devEmail = this.getNotifyEmail();
                if (!StringTools.isBlank(devEmail)) {
                    ListTools.toSet(StringTools.split(devEmail,','),recipients);
                }
                StringBuffer toSMS   = new StringBuffer();
                StringBuffer toEmail = new StringBuffer();
                for (String R : recipients) {
                    if (SMSOutboundGateway.StartsWithSMS(R)) {
                        if (toSMS.length() > 0) { toSMS.append(","); }
                        toSMS.append(R.substring(SMSOutboundGateway.SMS_Prefix.length()));
                    } else {
                        if (toEmail.length() > 0) { toEmail.append(","); }
                        toEmail.append(R);
                    }
                }

                /* Send email */
                String frEmail = bpl.getEventNotificationFrom();
                if (StringTools.isBlank(frEmail)) {
                    frEmail = bpl.getEMailAddress(BasicPrivateLabel.EMAIL_TYPE_NOTIFY);
                    if (StringTools.isBlank(frEmail)) {
                        frEmail = SendMail.getUserFromEmailAddress();
                    }
                }
                if (StringTools.isBlank(toEmail)) {
                    Print.logInfo("No email recipients, skipping email ...");
                } else
                if (StringTools.isBlank(frEmail)) {
                    Print.logWarn("No 'From:' email address, skipping email ...");
                } else {
                    Print.logInfo("From     : "  + frEmail);
                    Print.logInfo("To(email): "  + toEmail);
                    Print.logInfo("Subject  : "  + subj);
                    Print.logInfo("Body     :\n" + body);
                    try {
                        Print.logInfo("Sending auto notify email ...");
                        SendMail.send(frEmail,toEmail.toString(),null,null,subj,body,null);
                    } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                        // this will fail if JavaMail support for SendMail is not available.
                        Print.logWarn("SendMail error: " + t);
                    }
                }

                /* send SMS */
                if (!StringTools.isBlank(toSMS)) {
                    String smsMsg = subj;
                    Print.logInfo("To(SMS): " + toSMS);
                    Print.logInfo("Message: " + smsMsg);
                    // SMS gateway */
                    String    smsGatewayName = SMSOutboundGateway.GetDefaultGatewayName();
                    SMSOutboundGateway smsGW = SMSOutboundGateway.GetSMSGateway(smsGatewayName);
                    if (smsGW != null) {
                        Print.logInfo("Sending SMS via gateway: " + smsGatewayName);
                        // list of SMS recipients
                        String smsPhoneList[] = StringTools.split(toSMS,',');
                        for (String smsPhone : smsPhoneList) {
                            if (SMSOutboundGateway.StartsWithSMS(smsPhone)) {
                                smsPhone = smsPhone.substring(SMSOutboundGateway.SMS_Prefix.length());
                            }
                            if (!StringTools.isBlank(smsPhone)) {
                                Print.logInfo("SMS: " + smsPhone + " --> " + smsMsg);
                                DCServerFactory.ResultCode result = smsGW.sendSMSMessage(account, smsMsg, smsPhone);
                                if (!result.equals(DCServerFactory.ResultCode.SUCCESS)) {
                                    Print.logWarn("SMS error: " + result);
                                }
                            }
                        }
                    } else {
                        Print.logWarn("SMS Gateway not found: " + smsGatewayName);
                    }
                }

            }
        }

        /* return trigger state */
        return didTrigger;

    }

    private boolean _setDeviceAction(int actionMask, EventData event)
    {

        /* no action? */
        if ((actionMask < 0) || (actionMask == RuleFactory.ACTION_NONE)) {
            return false;
        }

        /* save last triggered notification */
        if ((actionMask & RuleFactory.ACTION_SAVE_LAST) != 0) {
            this.setLastNotifyTime(event.getTimestamp());       // FLD_lastNotifyTime
            this.setLastNotifyCode(event.getStatusCode());      // FLD_lastNotifyCode
            this.addOtherChangedFieldNames(Device.FLD_lastNotifyTime, Device.FLD_lastNotifyCode);
            // TODO: add "lastNotifyTime" and "lastNotifyCode" to updated Device columns
        } else {
            // "lastNotifyTime" and "lastNotifyCode" should be left as-is
            // NOTE: An external DB trigger may have changed these values, and updating
            // them may end up resetting these back to '0'
        }

        /* disable active corridor */
        /*
        if ((actionMask & RuleFactory.ACTION_DISABLE_CORRIDOR) != 0) {
            if (this.hasActiveCorridor()) {
                this.setActiveCorridor("");                     // FLD_activeCorridor
            } else {
                // no active corridor
            }
        }
        */

        /* enable new corridor */
        /*
        if ((actionMask & RuleFactory.ACTION_ENABLE_CORRIDOR) != 0) {
            Geozone zone = event.getGeozone();
            if ((zone != null) && zone.hasCorridorID()) {
                this.setActiveCorridor(zone.getCorridorID());   // FLD_activeCorridor
            } else {
                // leave as-is
            }
        }
        */

        // changes not yet saved
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Optimization for StatusCode description lookup (typically for map display)
    // This is a temporary cache of StatusCodes that are used for Events which
    // are either displayed on a map, or in a report.  Access to this cache does not need
    // to be synchronized since all status code lookups will occur within the same thread.
    // This cache is temporary and is garbage collected along with this Device record.

    private Map<Integer,StatusCode> statusCodeMap = null;

    /* get cached status code */
    public StatusCode getStatusCode(int code)
    {

        /* create map */
        if (this.statusCodeMap == null) {
            this.statusCodeMap = new HashMap<Integer,StatusCode>();
        }

        /* already in cache */
        Integer codeKey = new Integer(code);
        if (this.statusCodeMap.containsKey(codeKey)) {
            return this.statusCodeMap.get(codeKey); // may return null;
        }

        /* add to cache */
        String accountID = this.getAccountID();
        String deviceID  = this.getDeviceID();
        StatusCode sc = StatusCode.findStatusCode(accountID, deviceID, code);
        this.statusCodeMap.put(new Integer(code), sc);
        return sc; // may be null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get entities currently attached to deviceID */
    public String[] getAttachedEntityDescriptions(EntityManager.EntityType etype)
    {
        EntityManager.EntityType et = EntityManager.getEntityType(etype);
        return this.getAttachedEntityDescriptions(et.getIntValue());
    }

    /* get entities currently attached to deviceID */
    public String[] getAttachedEntityDescriptions(int entityType)
    {
        if (Device.hasEntityManager()) {
            String attEnt[] = null;
            try {
                String acctID = this.getAccountID();
                String devID  = this.getDeviceID();
                attEnt = Device.getEntityManager().getAttachedEntityDescriptions(acctID, devID, entityType);
            } catch (DBException dbe) {
                Print.logException("Error reading Device Entities", dbe);
            }
            return attEnt;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private EventData   savedRangeEvents[] = null;
    
    public void setSavedRangeEvents(EventData events[])
    {
        this.savedRangeEvents = events;
    }
    
    public EventData[] getSavedRangeEvents()
    {
        return this.savedRangeEvents;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return events in specified time range */
    public EventData[] getRangeEvents(
        long timeStart, long timeEnd,
        int statusCodes[],
        boolean validGPS,
        EventData.LimitType limitType, long limit)
        throws DBException
    {

        /* get data */
        EventData ev[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd,
            statusCodes,
            validGPS,
            limitType, limit, true,
            null/*additionalSelect*/);

        /* apply current Device to all EventData records */
        if (ev != null) {
            for (int i = 0; i < ev.length; i++) {
                ev[i].setDevice(this);
            }
        }
        return ev;

    }

    /* return events in specified time range */
    public EventData[] getRangeEvents(
        long timeStart, long timeEnd,
        boolean validGPS,
        EventData.LimitType limitType, long limit)
        throws DBException
    {
        return this.getRangeEvents(timeStart, timeEnd, null, validGPS, limitType, limit);
    }

    /* return the most recent 'limit' events */
    public EventData[] getLatestEvents(long limit, boolean validGPS)
        throws DBException
    {
        long timeStart = -1L;
        long timeEnd   = -1L;
        return this.getRangeEvents(timeStart, timeEnd, null, validGPS, EventData.LimitType.LAST, limit);
    }

    // ------------------------------------------------------------------------

    /* return the most recent 'limit' events */
    public EventData getFirstEvent(long startTime, boolean validGPS)
        throws DBException
    {
        long endTime = -1L;
        EventData ev[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            startTime, endTime,
            null/*statusCodes[]*/,
            validGPS,
            EventData.LimitType.FIRST, 1, true,
            null/*additionalSelect*/);
        if ((ev == null) || (ev.length <= 0)) {
            return null;
        } else {
            ev[0].setDevice(this);
            return ev[0];
        }
    }

    // ------------------------------------------------------------------------

    /* return the last valid GPS event */
    public EventData getLastEvent(boolean validGPS)
        throws DBException
    {
        // TODO: cache this event?
        return this.getLastEvent(null, -1L, validGPS);
    }

    /* return the last event <= specified endTime */
    public EventData getLastEvent(long endTime, boolean validGPS)
        throws DBException
    {
        return this.getLastEvent(null, endTime, validGPS);
    }

    /* return the last event <= specified endTime */
    public EventData getLastEvent(int statusCodes[])
        throws DBException
    {
        return this.getLastEvent(statusCodes, -1L, false);
    }

    /* return the last event <= specified endTime */
    public EventData getLastEvent(int statusCodes[], long endTime, boolean validGPS)
        throws DBException
    {
        long startTime = -1L;
        EventData ev[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            startTime, endTime,
            statusCodes,
            validGPS,
            EventData.LimitType.LAST, 1, true,
            null/*additionalSelect*/);
        if ((ev == null) || (ev.length <= 0)) {
            return null;
        } else {
            ev[0].setDevice(this);
            return ev[0];
        }
    }

    // ------------------------------------------------------------------------

    public interface EventDataHandler
    {
        public void handleEventDataRecord(EventData ev);
    }
    
    public void reprocessEventDataRecords(long timeStart, long timeEnd, final EventDataHandler edh)
        throws DBException
    {
        EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd,
            null/*statusCodes*/,
            false/*validGPS*/,
            EventData.LimitType.LAST, -1L/*limit*/, true/*ascending*/,
            null/*additionalSelect*/,
            new DBRecordHandler<EventData>() {
                public int handleDBRecord(EventData rcd) throws DBException {
                    edh.handleEventDataRecord(rcd);
                    return DBRecordHandler.DBRH_SKIP;
                }
            });
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Save this Device to db storage
    **/
    public void save()
        throws DBException
    {

        /* save */
        super.save();
        if (this.transport != null) { this.transport.save(); }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return a String representation of this Device
    *** @return The String representation
    **/
    public String toString()
    {
        return this.getAccountID() + "/" + this.getDeviceID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Transport transport = null;

    /**
    *** Sets the Transport for this Device
    *** @param xport  The Transport instance
    **/
    public void setTransport(Transport xport)
    {
        this.transport = xport;
    }

    /**
    *** Gets the Transport-ID for this Device (if any)
    *** @return The Transport-ID for this Device, or an empty string is not defined
    **/
    public String getTransportID()
    {
        return (this.transport != null)? this.transport.getTransportID() : "";
    }

    /**
    *** Gets the DataTransport for this Device
    *** @return The DataTransport for this Device
    **/
    public DataTransport getDataTransport()
    {
        return (this.transport != null)? (DataTransport)this.transport : (DataTransport)this;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String devID)
        throws DBException // if error occurs while testing existence
    {
        if ((acctID != null) && (devID != null)) {
            Device.Key devKey = new Device.Key(acctID, devID);
            return devKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** This method is used to load a Device record based on the SIM phone number. 
    *** Intended for use by an incoming SMS message handler.
    *** It is up to the caller to check whether this Device or Account are inactive.
    *** @param simPhone  The SIM phone number of the device
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceBySimPhoneNumber(String simPhone)
        throws DBException
    {

        /* invalid id? */
        if ((simPhone == null) || simPhone.equals("")) {
            return null; // just say it doesn't exist
        }

        /* read device for simPhone */
        Device       dev = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (simPhoneNumber='<phone>')
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(dwh.EQ(Device.FLD_simPhoneNumber,simPhone)));
            dsel.setLimit(2);
            // Note: The index on the column FLD_simPhoneNumber does not enforce uniqueness
            // (since null/empty values are allowed and needed)

            /* get record */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String acctId = rs.getString(FLD_accountID);
                String devId  = rs.getString(FLD_deviceID);
                dev = new Device(new Device.Key(acctId,devId));
                dev.setAllFieldValues(rs);
                if (rs.next()) {
                    Print.logError("Found multiple occurances of this SIM phone number: " + simPhone);
                }
                break; // only one record
            }
            // it's possible at this point that we haven't even read 1 device

        } catch (SQLException sqe) {
            throw new DBException("Getting Device SIM phone number: " + simPhone, sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return device */
        // Note: 'dev' may be null if it wasn't found
        return dev;

    }

    // ------------------------------------------------------------------------

    /**
    *** This method is called by "Transport.loadDeviceByUniqueID(String)" to load a Device
    *** within a Device Communication Server, based on a Unique-ID.  It is up to the caller
    *** to check whether this Device or Account are inactive.
    *** @param uniqId  The Unique-ID of the device (ie. IMEI, ESN, Serial#, etc)
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByUniqueID(String uniqId)
        throws DBException
    {

        /* invalid id? */
        if ((uniqId == null) || uniqId.equals("")) {
            return null; // just say it doesn't exist
        }

        /* read device for unique-id */
        Device       dev = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (uniqueID='unique')
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(dwh.EQ(Device.FLD_uniqueID,uniqId)));
            dsel.setLimit(2);
            // Note: The index on the column FLD_uniqueID does not enforce uniqueness
            // (since null/empty values are allowed and needed)

            /* get record */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String acctId = rs.getString(FLD_accountID);
                String devId  = rs.getString(FLD_deviceID);
                dev = new Device(new Device.Key(acctId,devId));
                dev.setAllFieldValues(rs);
                if (rs.next()) {
                    Print.logError("Found multiple occurances of this unique-id: " + uniqId);
                }
                break; // only one record
            }
            // it's possible at this point that we haven't even read 1 device

        } catch (SQLException sqe) {
            throw new DBException("Getting Device unique-id: " + uniqId, sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return device */
        // Note: 'dev' may be null if it wasn't found
        return dev;

    }

    // ------------------------------------------------------------------------

    /**
    *** This method is called by "Transport.loadDeviceByTransportID(...)" to load a Device
    *** within a Device Communication Server, based on the Account and Device IDs.
    *** @param account  The Account instance representing the owning account
    *** @param devID    The Device-ID
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByName(Account account, String devID)
        throws DBException
    {
        Device dev = Device.getDevice(account, devID);
        return dev;
    }

    // ------------------------------------------------------------------------

    /* get device (may return null) */
    public static Device getDevice(Account account, String devID)
        throws DBException
    {
        if ((account != null) && (devID != null)) {
            String acctID = account.getAccountID();
            Device.Key key = new Device.Key(acctID, devID);
            if (key.exists()) {
                Device dev = key.getDBRecord(true);
                dev.setAccount(account);
                return dev;
            } else {
                // device does not exist
                return null;
            }
        } else {
            return null; // just say it doesn't exist
        }
    }

    /* get/create device (not yet save if created) */
    // Note: does NOT return null (throws exception if not found)
    public static Device getDevice(Account account, String devID, boolean create)
        throws DBException
    {

        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctID = account.getAccountID();

        /* device-id specified? */
        if (StringTools.isBlank(devID)) {
            throw new DBNotFoundException("Device-ID not specified for account: " + acctID);
        }

        /* get/create */
        Device dev = null;
        Device.Key devKey = new Device.Key(acctID, devID);
        if (!devKey.exists()) {
            if (create) {
                dev = devKey.getDBRecord();
                dev.setAccount(account);
                dev.setCreationDefaultValues();
                return dev; // not yet saved!
            } else {
                throw new DBNotFoundException("Device-ID does not exists: " + devKey);
            }
        } else
        if (create) {
            // we've been asked to create the device, and it already exists
            throw new DBAlreadyExistsException("Device-ID already exists '" + devKey + "'");
        } else {
            dev = Device.getDevice(account, devID);
            if (dev == null) {
                throw new DBException("Unable to read existing Device-ID: " + devKey);
            }
            return dev;
        }

    }

    // ------------------------------------------------------------------------

    /** 
    *** Create/Save Device record
    **/
    public static Device createNewDevice(Account account, String devID, String uniqueID)
        throws DBException
    {
        if ((account != null) && !StringTools.isBlank(devID)) {
            Device dev = Device.getDevice(account, devID, true); // does not return null
            if (!StringTools.isBlank(uniqueID)) {
                dev.setUniqueID(uniqueID);
            }
            dev.save();
            return dev;
        } else {
            throw new DBException("Invalid Account/DeviceID specified");
        }
    }
    
    /* create a virtual device record (used for testing purposes) */
    public static Device createVirtualDevice(String acctID, String devID)
    {

        /* get/create */
        Device.Key devKey = new Device.Key(acctID, devID);
        Device dev = devKey.getDBRecord();
        dev.setCreationDefaultValues();
        dev.setVirtual(true);
        return dev;

    }

    // ------------------------------------------------------------------------

    /* return list of all Devices owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceIDsForAccount(String acctId, User userAuth, boolean inclInactv)
        throws DBException
    {
        return Device.getDeviceIDsForAccount(acctId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceIDsForAccount(String acctId, User userAuth, boolean inclInactv, long limit)
        throws DBException
    {

        /* no account specified? */
        if (StringTools.isBlank(acctId)) {
            if (userAuth != null) {
                acctId = userAuth.getAccountID();
            } else {
                Print.logError("Account not specified!");
                return new OrderedSet<String>();
            }
        }

        /* read devices for account */
        OrderedSet<String> devList = new OrderedSet<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (accountID='acct') ORDER BY deviceID
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            dsel.setSelectedFields(Device.FLD_deviceID);
            DBWhere dwh = dsel.createDBWhere();
            if (inclInactv) {
                dsel.setWhere(dwh.WHERE(
                    dwh.EQ(Device.FLD_accountID,acctId)
                ));
            } else {
                dsel.setWhere(dwh.WHERE_(
                    dwh.AND(
                        dwh.EQ(Device.FLD_accountID,acctId),
                        dwh.NE(Device.FLD_isActive,0)
                    )
                ));
            }
            dsel.setOrderByFields(Device.FLD_deviceID);
            dsel.setLimit(limit);

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(Device.FLD_deviceID);
                if ((userAuth == null) || userAuth.isAuthorizedDevice(devId)) {
                    devList.add(devId);
                }
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account Device List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return devList;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This section support a method for obtaining human readable information from
    // the Device record for reporting, or email purposes. (currently this is
    // used by the 'rules' engine when generating notification emails).
    
    private static final String KEY_FUEL_LEVEL[]    = new String[] { "fuelLevel"                             };  // "25.0 %"
    private static final String KEY_LAST_FUEL_LEV[] = new String[] { "lastFuelLevel"                         };  // "25.0 %"
    private static final String KEY_FUEL_VOLUME[]   = new String[] { "fuelLevelVolume"    , "fuelVolume"     };  // "12 gal"
    private static final String KEY_LAST_FUEL_VOL[] = new String[] { "lastFuelLevelVolume", "lastFuelVolume" };  // "12 gal"

    public String getFieldValueString(String key, String arg, BasicPrivateLabel bpl)
    {

        /* check for valid field name */
        if (key == null) {
            return null;
        }

        /* return key value */
        long now = DateTime.getCurrentTimeSec();
        Locale locale = (bpl != null)? bpl.getLocale() : null;
        if (EventData._keyMatch(key,EventData.KEY_DEVICE)) {
            return this.getDescription();
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE_LINK)) {
            String url = this.getLinkURL();
            String dsc = StringTools.blankDefault(this.getLinkDescription(),"Link");
            return "<a href='"+url+"' target='_blank'>"+dsc+"</a>";
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEV_CONN_AGE)) {
            // HHH:MM:SS
            long lastConnectTime = this.getLastTotalConnectTime();
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
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEV_TRAILERS)) {
            String e[] = this.getAttachedEntityDescriptions(EntityManager.EntityType.TRAILER);
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
        if (EventData._keyMatch(key,Device.KEY_FUEL_LEVEL)) {
            long pct = Math.round(this.getLastFuelLevel() * 100.0);
            return pct+"%";
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_FUEL_LEV)) {
            long pct = Math.round(this.getLastFuelLevel() * 100.0);
            return pct+"%";
        } else
        if (EventData._keyMatch(key,Device.KEY_FUEL_VOLUME)) {
            Account.VolumeUnits vu = Account.getVolumeUnits(this.getAccount());
            double L = this.getFuelCapacity() * this.getLastFuelLevel();
            double V = vu.convertFromLiters(L);
            return StringTools.format(V,"0.0") + " " + vu.toString(locale);
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_FUEL_VOL)) {
            Account.VolumeUnits vu = Account.getVolumeUnits(this.getAccount());
            double L = this.getFuelCapacity() * this.getLastFuelLevel();
            double V = vu.convertFromLiters(L);
            return StringTools.format(V,"0.0") + " " + vu.toString(locale);
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
    private static Comparator<Device> devDescComparator = null;
    public static Comparator<Device> getDeviceDescriptionComparator()
    {
        if (devDescComparator == null) {
            devDescComparator = new DeviceDescriptionComparator(); // ascending
        }
        return devDescComparator;
    }
    
    /* Comparator optimized for EventData device description */
    public static class DeviceDescriptionComparator
        implements Comparator<Device>
    {
        private boolean ascending = true;
        public DeviceDescriptionComparator() {
            this(true);
        }
        public DeviceDescriptionComparator(boolean ascending) {
            this.ascending  = ascending;
        }
        public int compare(Device dv1, Device dv2) {
            // assume we are comparing Device records
            if (dv1 == dv2) {
                return 0; // exact same object (or both null)
            } else 
            if (dv1 == null) {
                return this.ascending? -1 :  1; // null < non-null
            } else
            if (dv2 == null) {
                return this.ascending?  1 : -1; // non-null > null
            } else {
                String D1 = dv1.getDescription().toLowerCase();
                String D2 = dv2.getDescription().toLowerCase();
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

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]       = new String[] { "account"   , "acct"  , "a" };
    private static final String ARG_DEVICE[]        = new String[] { "device"    , "dev"   , "d" };
    private static final String ARG_UNIQID[]        = new String[] { "uniqueid"  , "unique", "uniq", "uid", "u" };
    private static final String ARG_CREATE[]        = new String[] { "create"               };
    private static final String ARG_EDIT[]          = new String[] { "edit"      , "ed"     };
    private static final String ARG_EDITALL[]       = new String[] { "editall"   , "eda"    }; 
    private static final String ARG_DELETE[]        = new String[] { "delete"               };
    private static final String ARG_EVENTS[]        = new String[] { "events"    , "ev"     };
    private static final String ARG_FORMAT[]        = new String[] { "format"    , "fmt"    };
  //private static final String ARG_SETPROP[]       = new String[] { "setprop"              };
    private static final String ARG_INSERT[]        = new String[] { "insertGP"             };
    private static final String ARG_CLEARACK[]      = new String[] { "clearAck"             };
    private static final String ARG_MAINTKM[]       = new String[] { "maint"     , "maintkm"};
    private static final String ARG_CHECKRULES[]    = new String[] { "ckRules"              };
    private static final String ARG_RESET_ODOM[]    = new String[] { "resetOdom"            };
    private static final String ARG_SEND_COMMAND[]  = new String[] { "sendCmd"              };
    private static final String ARG_CNT_FUTURE_EV[] = new String[] { "countFutureEvents"    };
    private static final String ARG_DEL_FUTURE_EV[] = new String[] { "deleteFutureEvents"   };
    private static final String ARG_CNT_OLD_EV[]    = new String[] { "countOldEvents"       };
    private static final String ARG_DEL_OLD_EV[]    = new String[] { "deleteOldEvents"      };
    private static final String ARG_ZONECHECK[]     = new String[] { "zoneCheck"            };

    private static String _fmtDevID(String acctID, String devID)
    {
        return acctID + "/" + devID;
    }

    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + Device.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -account=<id>               Acount ID which owns Device");
        Print.sysPrintln("  -device=<id>                Device ID to create/edit");
        Print.sysPrintln("  -uniqueid=<id>              Unique ID to create/edit");
        Print.sysPrintln("");
        Print.sysPrintln("  -create                     Create a new Device");
        Print.sysPrintln("  -edit                       Edit an existing (or newly created) Device");
        Print.sysPrintln("  -delete                     Delete specified Device");
        Print.sysPrintln("");
        Print.sysPrintln("  -events=<limit>             Retrieve the last <limit> events");
        Print.sysPrintln("  -ckRules=<lat>/<lon>,<sc>   Check rule (may change db!)");
        Print.sysPrintln("");
        Print.sysPrintln("  -countFutureEvents=<sec>    Count events beyond (now + sec) into the future");
        Print.sysPrintln("  -deleteFutureEvents=<sec>   Delete events beyond (now + sec) into the future");
        Print.sysPrintln("");
        Print.sysPrintln("  -countOldEvents=<time>      Count events before specified time");
        Print.sysPrintln("  -deleteOldEvents=<time>     Delete events ibefore specified time");
        Print.sysPrintln("");
        Print.sysPrintln("  -zoneCheck=<GP1>/<GP2>      Geozone transition check");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE , "");
        String uniqID  = RTConfig.getString(ARG_UNIQID , "");

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may throw DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }
        BasicPrivateLabel privLabel = acct.getPrivateLabel();

        /* device-id specified? */
        if (StringTools.isBlank(devID)) {
            Print.logError("Device-ID not specified.");
            usage();
        }

        /* device exists? */
        boolean deviceExists = false;
        try {
            deviceExists = Device.exists(acctID, devID);
        } catch (DBException dbe) {
            Print.logError("Error determining if Device exists: " + _fmtDevID(acctID,devID));
            System.exit(99);
        }

        /* get device if it exists */
        Device deviceRcd = null;
        if (deviceExists) {
            try {
                deviceRcd = Device.getDevice(acct, devID, false); // may throw DBException
            } catch (DBException dbe) {
                Print.logError("Error getting Device: " + _fmtDevID(acctID,devID));
                dbe.printException();
                System.exit(99);
            }
        }

        /* option count */
        int opts = 0;

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !StringTools.isBlank(acctID) && !StringTools.isBlank(devID)) {
            opts++;
            if (!deviceExists) {
                Print.logWarn("Device does not exist: " + _fmtDevID(acctID,devID));
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                Device.Key devKey = new Device.Key(acctID, devID);
                devKey.delete(true); // also delete dependencies
                Print.logInfo("Device deleted: " + _fmtDevID(acctID,devID));
                deviceExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting Device: " + _fmtDevID(acctID,devID));
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (deviceExists) {
                Print.logWarn("Device already exists: " + _fmtDevID(acctID,devID));
            } else {
                try {
                    Device.createNewDevice(acct, devID, uniqID);
                    Print.logInfo("Created Device: " + _fmtDevID(acctID,devID));
                    deviceExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating Device: " + _fmtDevID(acctID,devID));
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    DBEdit editor = new DBEdit(deviceRcd);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                }
            }
            System.exit(0);
        }

        /* events */
        if (RTConfig.hasProperty(ARG_EVENTS)) {
            opts++;
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
            } else {
                int limit = RTConfig.getInt(ARG_EVENTS, 10);
                int fmt   = EventUtil.parseOutputFormat(RTConfig.getString(ARG_FORMAT,null),EventUtil.FORMAT_CSV);
                try {
                    EventData evList[] = deviceRcd.getLatestEvents(limit,false);
                    deviceRcd.setSavedRangeEvents(evList);
                    java.util.List<Device> devList = new Vector<Device>();
                    devList.add(deviceRcd);
                    EventUtil evUtil = EventUtil.getInstance();
                    evUtil.writeEvents((PrintWriter)null, 
                        acct, devList, 
                        fmt, true/*allTags*/, null/*timezone*/,
                        privLabel);
                } catch (IOException ioe) {
                    Print.logError("IO Error");
                } catch (DBException dbe) {
                    Print.logError("Error getting events for Device: " + _fmtDevID(acctID,devID));
                    dbe.printException();
                }
            }
            System.exit(0);
        }

        /* zone check */
        if (RTConfig.hasProperty(ARG_ZONECHECK)) {
            opts++;
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
            } else {
                String gpStr[] = StringTools.split(RTConfig.getString(ARG_ZONECHECK,""),',');
                GeoPoint gp1 = (gpStr.length > 0)? new GeoPoint(gpStr[0]) : GeoPoint.INVALID_GEOPOINT;
                GeoPoint gp2 = (gpStr.length > 1)? new GeoPoint(gpStr[1]) : GeoPoint.INVALID_GEOPOINT;
                long fixtime = DateTime.getCurrentTimeSec();
                deviceRcd.setLastValidLocation(fixtime, gp1, 0.0); // NOT SAVED!
                java.util.List<Device.GeozoneTransition> zone = deviceRcd.checkGeozoneTransitions(fixtime, gp2);
                if (ListTools.size(zone) > 0) {
                    for (Device.GeozoneTransition z : zone) {
                        Print.sysPrintln("Zone Transition: " + z);
                    }
                } else {
                    Print.sysPrintln("Not in a Geozone ...");
                }
            }
            System.exit(0);
        }

        /* insert GeoPoint */
        if (RTConfig.hasProperty(ARG_INSERT)) {
            opts++;
            GeoPoint gp = new GeoPoint(RTConfig.getString(ARG_INSERT,""));
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(1);
            } else
            if (!gp.isValid()) {
                Print.logError("Invalid GeoPoint: " + gp);
                System.exit(1);
            } else {
                SendMail.SetThreadModel(SendMail.THREAD_DEBUG);
                Print.sysPrintln("Account PrivateLabel: " + privLabel.getName());
                ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
                if (INSERT_REVERSEGEOCODE_REQUIRED && (rgp == null)) {
                    Print.sysPrintln("Account has no ReverseGeocodeProvider (record not inserted)");
                    System.exit(1);
                }
                Print.sysPrintln("Account ReverseGeocodeProvider: " + ((rgp!=null)?rgp.getName():"<none>"));
                if (INSERT_REVERSEGEOCODE_REQUIRED && !Account.getGeocoderMode(acct).equals(Account.GeocoderMode.FULL)) {
                    Print.sysPrintln("Overriding Account GeocoderMode to 'FULL'");
                    acct.setGeocoderMode(Account.GeocoderMode.FULL);
                }
                // insert
                long timestamp = DateTime.getCurrentTimeSec();
                int statusCode = StatusCodes.STATUS_WAYMARK_0;
                EventData.Key evKey = new EventData.Key(acctID,devID,timestamp,statusCode);
                EventData evRcd = evKey.getDBRecord();
                evRcd.setGeoPoint(gp);
                evRcd.setAddress(null); // updated later
                if (deviceRcd.insertEventData(evRcd)) {
                    Print.sysPrintln("EventData record inserted ...");
                } else {
                    Print.logError("*** Unable to insert EventData record!!!");
                }
                BackgroundThreadPool.stopThreads();
                if (BackgroundThreadPool.getSize() > 0) {
                    do {
                        Print.sysPrintln("Waiting for background threads to complete ...");
                        try { Thread.sleep(3000L); } catch (Throwable t) {}
                    } while (BackgroundThreadPool.getSize() > 0);
                }
                Print.sysPrintln("... done");
                System.exit(0);
            }
        }

        /* clear any pending ACK */
        if (RTConfig.hasProperty(ARG_CLEARACK)) {
            opts++;
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // clear ack
            boolean didClear = deviceRcd.clearExpectCommandAck(null);
            Print.logInfo("Cleared Device ACK: " + didClear);
            System.exit(0);
        }

        /* periodic maintenance check */
        if (RTConfig.hasProperty(ARG_MAINTKM)) {
            opts++;
            // device exists?
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // odometer/interval
            double odomKM   = deviceRcd.getLastOdometerKM();
            double intervKM = deviceRcd.getMaintIntervalKM0();
            if ((odomKM <= 0.0) || (intervKM <= 0.0)) {
                System.exit(2); // no odometer/interval
            }
            // check service interval
            double maintKM = deviceRcd.getMaintOdometerKM0();
            if (odomKM >= (maintKM + intervKM)) {
                // send email
                Print.logInfo("Service Interval due for " + deviceRcd.getDescription());
                String frEmail = SendMail.getUserFromEmailAddress();
                String toEmail = RTConfig.getString(ARG_MAINTKM, "");
                if (!StringTools.isBlank(frEmail) && !StringTools.isBlank(toEmail)) {
                    I18N   i18n = I18N.getI18N(Device.class, acct.getLocale());
                    String text = i18n.getString("Device.serviceMaint.dueFor","Periodic Maintenance due for {0}",deviceRcd.getDescription());
                    String odom = i18n.getString("Device.serviceMaint.odometer","Odometer");
                    String subj = text;
                    String body = text + "\n" +
                        odom + ": " + odomKM + "\n" +
                        "\n";
                    try {
                        Print.logInfo("From:"     + frEmail);
                        Print.logInfo("To:"       + toEmail);
                        Print.logInfo("Subject: " + subj);
                        Print.logInfo("Body:\n"   + body);
                        Print.logInfo("Sending email ...");
                        SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
                        SendMail.send(frEmail,toEmail,null,null,subj,body,null);
                        // SystemAudit.sentRuleNotification(acctID, null, devID, subj);
                        System.exit(0); // success
                    } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                        // this will fail if JavaMail support for SendMail is not available.
                        Print.logWarn("SendMail error: " + t);
                        System.exit(97);
                    }
                }
                System.exit(1);
            } else {
                System.exit(2); // no interval
            }
        }

        /* periodic maintenance check */
        if (RTConfig.hasProperty(ARG_CHECKRULES)) {
            opts++;
            // args "<lat>/<lon>[,<code>]"
            String crArgs[] = StringTools.split(RTConfig.getString(ARG_CHECKRULES,""),',');
            if (crArgs.length < 1) {
                Print.logError("Invalid 'checkRules' arguments ['lat/lon,code']");
                System.exit(99);
            }
            GeoPoint gp = new GeoPoint(crArgs[0]);
            int    code = StatusCodes.ParseCode(ListTools.itemAt(crArgs,1,""),null,StatusCodes.STATUS_WAYMARK_0);
            // device exists?
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            } else
            if (!gp.isValid()) {
                Print.logError("Invalid GeoPoint: " + gp);
                System.exit(98);
            }
            // sample event
            long timestamp = DateTime.getCurrentTimeSec();
            EventData.Key evKey = new EventData.Key(acctID, devID, timestamp, code);
            EventData evRcd = evKey.getDBRecord();
            evRcd.setGeoPoint(gp);
            evRcd.setAddress(null);
            Print.logInfo("Created Event: " + _fmtDevID(acctID,devID) + " " + gp + 
                " [" + StatusCodes.GetHex(code) + ":" + StatusCodes.GetDescription(code,null) + "]");
            // check rules
            Print.logInfo("Checking Rules ...");
            if (!deviceRcd.checkEventRules(evRcd)) {
                Print.logWarn("No rules triggered ...");
            }
            // stop (email, etc)
            BackgroundThreadPool.stopThreads();
            if (BackgroundThreadPool.getSize() > 0) {
                do {
                    Print.sysPrintln("Waiting for background threads to complete ...");
                    try { Thread.sleep(3000L); } catch (Throwable t) {}
                } while (BackgroundThreadPool.getSize() > 0);
            }
            Print.sysPrintln("... done");
            System.exit(0);
        }

        /* count future events */
        if (RTConfig.hasProperty(ARG_CNT_FUTURE_EV)) {
            opts++;
            // Device must exist
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // arg seconds
            long sec = RTConfig.getLong(ARG_CNT_FUTURE_EV,0L);
            // count future events
            long nowTime    = DateTime.getCurrentTimeSec();
            long futureTime = nowTime + sec;
            Print.sysPrintln("Counting events after \"" + (new DateTime(futureTime)) + "\" ...");
            try {
                long rcdCount = EventData.getRecordCount(acctID,devID,futureTime,-1L);
                if (rcdCount <= 0L) {
                    Print.sysPrintln("No future events found");
                } else {
                    Print.sysPrintln("Found "+rcdCount+" future events");
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error counting future events: " + dbe);
                System.exit(99);
            }
         }

        /* delete future events */
        if (RTConfig.hasProperty(ARG_DEL_FUTURE_EV)) {
            opts++;
            // Device must exist
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // arg seconds
            long sec = RTConfig.getLong(ARG_DEL_FUTURE_EV,0L);
            if (sec < 60L) {
                Print.logError("Specified seconds must be >= 60");
                System.exit(99);
            }
            // delete future events
            long nowTime    = DateTime.getCurrentTimeSec();
            long futureTime = nowTime + sec;
            Print.sysPrintln("Deleting events after \"" + (new DateTime(futureTime)) + "\" ...");
            try {
                long delCount = EventData.deleteFutureEvents(acctID, devID, futureTime);
                if (delCount <= 0L) {
                    Print.sysPrintln("No future events found");
                } else {
                    Print.sysPrintln("Deleted "+delCount+" future events");
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error deleting future events: " + dbe);
                System.exit(99);
            }
        }

        /* count old events */
        if (RTConfig.hasProperty(ARG_CNT_OLD_EV)) {
            opts++;
            String argTime = RTConfig.getString(ARG_CNT_OLD_EV, "");
            TimeZone acctTMZ = acct.getTimeZone(null);
            // Device must exist
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // arg time
            DateTime oldTime = null;
            if (StringTools.isBlank(argTime)) {
                Print.logError("Invalid time specification: " + argTime);
                System.exit(98);
            } else
            if (argTime.equalsIgnoreCase("current")) {
                oldTime = new DateTime(acctTMZ);
            } else {
                try {
                    oldTime = DateTime.parseArgumentDate(argTime,acctTMZ,true); // end of day time
                } catch (DateTime.DateParseException dpe) {
                    oldTime =null;
                }
                if (oldTime == null) {
                    Print.sysPrintln("Invalid Time specification: " + argTime);
                    System.exit(98);
                } else
                if (oldTime.getTimeSec() > DateTime.getCurrentTimeSec()) {
                    Print.sysPrintln("Delete future events not allowed");
                    System.exit(98);
                }
            }
            // count future events
            Print.sysPrintln("Counting events before \"" + oldTime + "\" ...");
            try {
                long rcdCount = EventData.getRecordCount(acctID,devID,-1L,oldTime.getTimeSec());
                if (rcdCount <= 0L) {
                    Print.sysPrintln("No old events found");
                } else {
                    Print.sysPrintln("Found "+rcdCount+" old events");
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error counting old events: " + dbe);
                System.exit(99);
            }
         }

        /* delete old events */
        if (RTConfig.hasProperty(ARG_DEL_OLD_EV)) {
            opts++;
            String argTime = RTConfig.getString(ARG_DEL_OLD_EV, "");
            TimeZone acctTMZ = acct.getTimeZone(null);
            // Device must exist
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // arg time
            DateTime oldTime = null;
            if (StringTools.isBlank(argTime)) {
                Print.logError("Invalid time specification: " + argTime);
                System.exit(98);
            } else
            if (argTime.equalsIgnoreCase("current")) {
                oldTime = new DateTime(acctTMZ);
            } else {
                try {
                    oldTime = DateTime.parseArgumentDate(argTime,acctTMZ,true);
                } catch (DateTime.DateParseException dpe) {
                    oldTime =null;
                }
                if (oldTime == null) {
                    Print.sysPrintln("Invalid Time specification: " + argTime);
                    System.exit(98);
                } else 
                if (oldTime.getTimeSec() > DateTime.getCurrentTimeSec()) {
                    Print.sysPrintln("Cannot specify a time in the future: " + argTime);
                    System.exit(98);
                }
            }
            // Delete
            Print.sysPrintln("Deleting events before \"" + oldTime + "\" ...");
            try {
                long delCount = EventData.deleteOldEvents(acctID, devID, oldTime.getTimeSec());
                if (delCount <= 0L) {
                    Print.sysPrintln("No old events found");
                } else {
                    Print.sysPrintln("Deleted "+delCount+" old events");
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error deleting old events: " + dbe);
                System.exit(99);
            }
        }

        /* reset odometer */
        if (RTConfig.hasProperty(ARG_RESET_ODOM)) {
            opts++;
            // args "timestamp"
            long resetTime = RTConfig.getLong(ARG_RESET_ODOM,0L);
            // device exists?
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // reset odometer for device events
            final AccumulatorLong count = new AccumulatorLong();
            final Device device = deviceRcd;
            double lastOdomKM = device.getLastOdometerKM();
            device.setLastOdometerKM(0.0);
            DBRecordHandler<EventData> odomResetHandler = new DBRecordHandler<EventData>() {
                private boolean   firstZeroOdom  = false;
                private EventData lastEvent      = null;
                private double    lastOdomKM     = 0.0;
                public int handleDBRecord(EventData rcd) throws DBException {
                    EventData ev = rcd;
                    ev.setDevice(device);
                    ev.setPreviousEventData(this.lastEvent);
                    double evOdomKM = ev.getOdometerKM();
                    long   evTime   = ev.getTimestamp();
                    double evLat    = ev.getLatitude();
                    double evLon    = ev.getLongitude();
                    double evHead   = ev.getHeading();
                    // found first zero odometer?
                    if (!firstZeroOdom) {
                        // still looking
                        if (evOdomKM > 0.0) {
                            this.lastOdomKM = evOdomKM;
                            this.lastEvent  = ev;
                            device.setLastOdometerKM(this.lastOdomKM);
                            device.setLastValidLatitude(evLat);
                            device.setLastValidLongitude(evLon);
                            device.setLastValidHeading(evHead);
                            device.setLastGPSTimestamp(evTime);
                            return DBRH_SKIP;
                        }
                        // found it
                        firstZeroOdom = true;
                        if (this.lastEvent == null) {
                            // we've never found a non-zero odometer
                            this.lastOdomKM = evOdomKM; // which is '0.0'
                            this.lastEvent  = ev;
                            device.setLastOdometerKM(this.lastOdomKM); // "0.0"
                            device.setLastValidLatitude(evLat);
                            device.setLastValidLongitude(evLon);
                            device.setLastValidHeading(evHead);
                            device.setLastGPSTimestamp(evTime);
                        }
                    }
                    // reset event odometer
                    this.lastOdomKM += ev.getGeoPoint().kilometersToPoint(this.lastEvent.getGeoPoint());
                    if ((count.get() % 100L) == 0L) {
                        Print.sysPrintln("Updating Event "+evTime+" (" + (new DateTime(evTime)) + ") ==> " + this.lastOdomKM);
                    }
                    ev.setOdometerKM(this.lastOdomKM);
                    ev.update(EventData.FLD_odometerKM); // may throw DBException
                    this.lastEvent = ev;
                    device.setLastOdometerKM(this.lastOdomKM);
                    device.setLastValidLatitude(evLat);
                    device.setLastValidLongitude(evLon);
                    device.setLastValidHeading(evHead);
                    device.setLastGPSTimestamp(evTime);
                    count.increment();
                    return DBRH_SKIP;
                }
            };
            try {
                // update events
                // (it's possible that this could run out of memory if this range is too large)
                EventData.getRangeEvents(
                    acctID, devID,
                    resetTime, -1L/*toDateSec*/,
                    null/*statusCodes*/,                            // all status codes
                    true/*validGPS*/,                               // valid GPS only
                    EventData.LimitType.FIRST, -1/*limit*/, true,   // no limit, ascending
                    null/*additionalSelect*/,
                    odomResetHandler);
                // update device record
                device.update(
                    Device.FLD_lastValidLatitude,
                    Device.FLD_lastValidLongitude,
                    Device.FLD_lastValidHeading,
                    Device.FLD_lastGPSTimestamp,
                    Device.FLD_lastOdometerKM
                    );
                // return number of records updated
                long lastGPSTime = device.getLastGPSTimestamp();
                Print.sysPrintln("Timestamp of last event processed: " + lastGPSTime + " ("+(new DateTime(lastGPSTime))+")");
                long c = count.get();
                if (c == 0L) {
                    Print.sysPrintln("... done (no events updated)");
                } else {
                    Print.sysPrintln("... done (updated "+c+" events)");
                }
            } catch (DBException dbe) {
                Print.logException("Error reading event records: " + acctID + "/" + devID, dbe);
                System.exit(99);
            }
            System.exit(0);
        }

        /* send command */
        if (RTConfig.hasProperty(ARG_SEND_COMMAND)) {
            opts++;
            // device exists?
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // send command
            String cmdType   = DCServerConfig.COMMAND_CONFIG;
            String cmdName   = RTConfig.getString(ARG_SEND_COMMAND,"");
            String cmdArgs[] = null;
            boolean success  = deviceRcd.sendDeviceCommand(null, cmdName, cmdArgs);
            Print.sysPrintln("Command Sent: " + success);
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

}
