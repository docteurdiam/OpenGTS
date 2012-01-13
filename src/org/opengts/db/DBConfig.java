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
//  2006/04/09  Martin D. Flynn
//     -Integrated 'DBException'
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//  2007/05/25  Martin D. Flynn
//     -Added already-initialized check to "initDBFactories()"
//     -Check for 'NoClassDefFoundError' when creating optional DBFactories.
//  2007/06/30  Martin D. Flynn
//     -Added optional "-account" argument for use by "-dump=<table>" command.
//  2007/09/16  Martin D. Flynn
//     -Changed logging level on "Optional DBFactory" warnings to debug level.
//     -Added BasicPrivateLabel initialization in 'init' method
//  2007/11/28  Martin D. Flynn
//     -Added factory entry for table "org.opengts.db.tables.StatusCode"
//     -Added runtime property "startup.initializerClass" to provide custom 
//      statup initialization.
//  2008/02/27  Martin D. Flynn
//     -Added TRACK_JAVASCRIPT_DIR, TRACK_JS_MENUBAR, TRACK_JS_UTILS property keys
//  2008/05/14  Martin D. Flynn
//     -Added TRACK_ENABLE_COOKIES, DB_TRANSPORT_ENABLE_QUERY, DB_UNIQUE_ENABLE_QUERY 
//      properties
//     -Added factory entry for table "org.opengts.db.tables.Transport"
//     -Added factory entry for table "org.opengts.db.tables.UniqueXID"
//  2008/06/20  Martin D. Flynn
//     -Added 'DBInitialization' interface for startup initialization support.
//     -Moved custom DBFactory initialization to optional StartupInit.
//     -Added command-line 'Usage' display.
//  2008/07/08  Martin D. Flynn
//     -Removed TRACK_JS_MENUBAR, TRACK_JS_UTILS property keys.
//  2008/10/16  Martin D. Flynn
//     -Added lookup for default device authorization
//  2008/12/01  Martin D. Flynn
//     -Override DBAdmin '-schema' command (calling 'DBAdmin.printTableSchema'
//      directly, and include header.
//  2009/01/28  Martin D. Flynn
//     -Added TRACK_OFFLINE_FILE
//     -Renamed 'warInit' to 'servletInit' and separated from 'cmdLineInit'.
//     -Renamed 'DB_TRANSPORT_ENABLE_QUERY' to 'TRANSPORT_QUERY_ENABLED'
//     -Renamed 'DB_UNIQUE_ENABLE_QUERY' to 'UNIQUEXID_QUERY_ENABLED'
//  2009/02/20  Martin D. Flynn
//     -Renamed 'track.enableCookies' to 'track.requireCookies'
//  2009/12/16  Martin D. Flynn
//     -Added method for GTS_HOME validation check [check_GTS_HOME()]
//  2010/10/25  Martin D. Flynn
//     -Updated startup initialization to add any missing DCS Command ACLs to 
//      the various BasicPrivateLabel instances.
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtools.DBAdmin.DBAdminExec;

import org.opengts.Version;
import org.opengts.StartupInit;
import org.opengts.dbtypes.*;
import org.opengts.db.tables.*;

public class DBConfig
{

    // ------------------------------------------------------------------------
    // default SQL database name

    private static final String DEFAULT_DB_NAME                 = "gts";

    // ------------------------------------------------------------------------
    // Runtime referenced package names

    private static final String PACKAGE_OPENGTS_                = "org.opengts.";

    private static final String PACKAGE_TABLES_                 = PACKAGE_OPENGTS_ + "db.tables."; // "org.opengts.db.tables."

    private static final String PACKAGE_DMTP_                   = PACKAGE_OPENGTS_ + "db.dmtp.";   // "org.opengts.db.dmtp."

    public  static final String PACKAGE_OPT                     = PACKAGE_OPENGTS_ + "opt";        // "org.opengts.opt"
    public  static final String PACKAGE_OPT_                    = PACKAGE_OPT      + ".";          // "org.opengts.opt."
    public  static final String PACKAGE_OPT_AUDIT_              = PACKAGE_OPT_     + "audit.";     // "org.opengts.opt.audit."
    public  static final String PACKAGE_OPT_WAR_                = PACKAGE_OPT_     + "war.";       // "org.opengts.opt.war."

    public  static final String PACKAGE_EXTRA_                  = PACKAGE_OPENGTS_ + "extra.";     // "org.opengts.extra."
    private static final String PACKAGE_EXTRA_TABLES            = PACKAGE_EXTRA_   + "tables";     // "org.opengts.extra.tables"
    public  static final String PACKAGE_EXTRA_TABLES_           = PACKAGE_EXTRA_   + "tables.";    // "org.opengts.extra.tables."
    public  static final String PACKAGE_EXTRA_DBTOOLS_          = PACKAGE_EXTRA_   + "dbtools.";   // "org.opengts.extra.dbtools."
    public  static final String PACKAGE_EXTRA_WAR_              = PACKAGE_EXTRA_   + "war.";       // "org.opengts.extra.war."

    public  static final String PACKAGE_RULE_                   = PACKAGE_OPENGTS_ + "rule.";      // "org.opengts.rule."
    public  static final String PACKAGE_RULE_UTIL_              = PACKAGE_RULE_    + "util.";      // "org.opengts.rule.util."
    private static final String PACKAGE_RULE_TABLES             = PACKAGE_RULE_    + "tables";     // "org.opengts.rule.tables"
    public  static final String PACKAGE_RULE_TABLES_            = PACKAGE_RULE_    + "tables.";    // "org.opengts.rule.tables."
    public  static final String CLASS_RULE_EventRuleFactory     = PACKAGE_RULE_    + "EventRuleFactory";

    public  static final String PACKAGE_BCROSS_                 = PACKAGE_OPENGTS_ + "bcross.";    // "org.opengts.bcross."
    public  static final String PACKAGE_BCROSS_TABLES_          = PACKAGE_BCROSS_  + "tables.";    // "org.opengts.bcross.tables."

    public  static final String PACKAGE_WAR_                    = PACKAGE_OPENGTS_ + "war.";       // "org.opengts.war."
    public  static final String PACKAGE_WAR_TRACK_              = PACKAGE_WAR_     + "track.";     // "org.opengts.war.track."

    // ------------------------------------------------------------------------
    // Schema header

    /* schema header */
    private static final String SCHEMA_HEADER[] = new String[] {
        "This document contains database schema information for the tables defined within the OpenGTS " +
            "system.  Optional tables (if any) will be indicated by the term \"[optional]\" next to the " +
            "table name.",
        "",
        "Additional information may be obtained by examining the source module for the specified class.",
        "",
        "The schema listing below should match the installed configuration, however, there may still be " +
            "minor differences depending on the specific version installed, or changes that have been made " +
            "to the configuration.  The current schema configuration can be generated from the actual " +
            "database configuration by executing the following command: ",
        "(executed from within the OpenGTS directory)",
        "",
        "   bin/dbAdmin.pl -schema",
        "",
        "Or, on Windows:",
        "",
        "   bin\\dbConfig.bat -schema",
    };

    // ------------------------------------------------------------------------
    // Version

    /**
    *** Returns true if the "extra" package is present
    *** @return True if the "extra" package is present
    **/
    public static boolean hasExtraPackage()
    {
        // TODO: optimize
        return (Package.getPackage(PACKAGE_EXTRA_TABLES) != null);
    }

    /**
    *** Returns true if the "rule" package is present
    *** @return True if the "rule" package is present
    **/
    public static boolean hasRulePackage()
    {
        // TODO: optimize
        return (Package.getPackage(PACKAGE_RULE_TABLES) != null);
    }

    /**
    *** Returns true if the "opt" package is present
    *** @return True if the "opt" package is present
    **/
    public static boolean hasOptPackage()
    {
        // TODO: optimize
        return (Package.getPackage(PACKAGE_OPT) != null);
    }

    /**
    *** Return the GTS build version
    *** @return The GTS build version
    **/
    public static String getVersion() 
    {
        return org.opengts.Version.getVersion(); 
    }

    // ------------------------------------------------------------------------
    // default device authorization
    
    private static final boolean DEFAULT_DEVICE_AUTHORIZATION       = true;

    // ------------------------------------------------------------------------
    // custom property keys

    // -------

    /**
    *** Runtime Configuration Property<br>
    *** The customer "Service Account" ID<br>
    *** Type: String
    **/
    public static final String PROP_ServiceAccount_ID                   = "ServiceAccount.ID";
    
    /**
    *** Runtime Configuration Property<br>
    *** The customer "Service Account" Name/Description<br>
    *** Type: String
    **/
    public static final String PROP_ServiceAccount_Name                 = "ServiceAccount.Name";
    
    /**
    *** Runtime Configuration Property<br>
    *** The customer "Service Account" Attributes<br>
    *** (currently not used)<br>
    *** Type: RTProperties
    **/
    public static final String PROP_ServiceAccount_Attr                 = "ServiceAccount.Attr";
    
    /**
    *** Runtime Configuration Property<br>
    *** The customer "Service Account" Authorization Key<br>
    *** Type: String
    **/
    public static final String PROP_ServiceAccount_Key                  = "ServiceAccount.Key";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** The name of the statup-initialization Class used to initialize custom features<br>
    *** Type: Class
    **/
    public static final String PROP_StartupInit_class                   = "StartupInit.class";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** Action to perform when an Event date/time is in the future<br>
    *** Type: Enum[disabled|ignore|truncate]
    **/
    public static final String PROP_Device_futureDate_action            = "Device.futureDate.action";
    
    /**
    *** Runtime Configuration Property<br>
    *** Maximum number of seconds that an Event date/time may be in the future before the action is performed<br>
    *** Type: Integer
    **/
    public static final String PROP_Device_futureDate_maximumSec        = "Device.futureDate.maximumSec";
    
    /**
    *** Runtime Configuration Property<br>
    *** Action to perform when an Event speed is invalid (ie. < '0', or > max km/h)<br>
    *** (not currently used)<br>
    *** Type: Enum[disabled|ignore|truncate]
    **/
    public static final String PROP_Device_invalidSpeed_action          = "Device.invalidSpeed.action";

    /**
    *** Runtime Configuration Property<br>
    *** Maximum speed that an Event may have, beyond which ithe action is performed<br>
    *** (not currently used)<br>
    *** Type: Double
    **/
    public static final String PROP_Device_invalidSpeed_maximumKPH      = "Device.invalidSpeed.maximumKPH";

    /**
    *** Runtime Configuration Property<br>
    *** Maximum odometer value<br>
    *** Type: Double
    **/
    public static final String PROP_Device_maximumOdometerKM            = "Device.maximumOdometerKM";

    /**
    *** Runtime Configuration Property<br>
    *** Maximum engine-hours value<br>
    *** Type: Double
    **/
    public static final String PROP_Device_maximumRuntimeHours          = "Device.maximumRuntimeHours";

    /**
    *** Runtime Configuration Property<br>
    *** True to create an alternate key/index for column Device.simPhoneNumber<br>
    *** (can be used for "SIM Phone Number" lookups)<br>
    *** Type: Boolean
    **/
    public static final String PROP_Device_keyedSimPhoneNumber          = "Device.keyedSimPhoneNumber";

    /**
    *** Runtime Configuration Property<br>
    *** True to create an alternate key/index for column Device.lastNotifyTime<br>
    *** (can be used for global "Last Notify Time" lookups)<br>
    *** Type: Boolean
    **/
    public static final String PROP_Device_keyedLastNotifyTime          = "Device.keyedLastNotifyTime";

    /**
    *** Runtime Configuration Property<br>
    *** True to test the rule selector in column Device.notifySelector<br>
    *** Type: Boolean
    **/
    public static final String PROP_Device_checkNotifySelector          = "Device.checkNotifySelector";

    // -------

    /**
    *** Runtime Configuration Property<br>
    *** Prefix for looking up DeviceInfo.DeviceCmd alternate server IDs<br>
    *** Type: String array
    **/
    public static final String PROP_DeviceInfo_DeviceCmdAlternate_      = "DeviceInfo.DeviceCmdAlternate.";

    // -------

    /**
    *** Runtime Configuration Property<br>
    *** Show the "Event-Per-Second" field<br>
    *** Type: Boolean
    **/
    public static final String PROP_sysAdminInfo_showEventsPerSecond    = "sysAdminInfo.showEventsPerSecond";

    // -------

    /**
    *** Runtime Configuration Property<br>
    *** Create alternate keyed "creationTime" in EventData<br>
    *** Type: Boolean
    **/
    public static final String PROP_EventData_keyedCreationTime         = "EventData.keyedCreationTime";

    /**
    *** Runtime Configuration Property<br>
    *** Log missing columns on insert/update (defaults to 'true')<br>
    *** Type: Boolean
    **/
    public static final String PROP_EventData_logMissingColumns         = "EventData.logMissingColumns";

    // -------

    /**
    *** Runtime Configuration Property<br>
    *** Default Radius (in meters) for PointRadius Geozones<br>
    *** Type: Integer
    **/
    public static final String PROP_Geozone_dftRadius_pointRadius       = "Geozone.dftRadius.pointRadius";
    
    /**
    *** Runtime Configuration Property<br>
    *** Default Radius (in meters) for Polygon Geozones<br>
    *** Type: Integer
    **/
    public static final String PROP_Geozone_dftRadius_polygon           = "Geozone.dftRadius.polygon";
    
    /**
    *** Runtime Configuration Property<br>
    *** Default Radius (in meters) for SweptPointRadius Geozones<br>
    *** Type: Integer
    **/
    public static final String PROP_Geozone_dftRadius_sweptPointRadius  = "Geozone.dftRadius.sweptPointRadius";

    // -------
    
    /**
    *** Runtime Configuration Property (optional)<br>
    *** True to enable checking Device "Group" lookup when triggering rules.<br>
    *** Type: Boolean
    **/
    public static final String PROP_RuleList_includeGroupRules          = "RuleList.includeGroupRules";

    // -------

    /**
    *** Runtime Configuration Property (optional)<br>
    *** Enable FuelManager checking (requires FuelRegister installed).<br>
    *** Type: Boolean
    **/
    public static final String PROP_FuelRegister_installFuelManager     = "FuelRegister.installFuelManager";

    /**
    *** Runtime Configuration Property (optional)<br>
    *** The FuelLevel "Increase" percent threshold (between 0.0 and 1.0 inclusive).<br>
    *** Type: Double
    **/
    public static final String PROP_FuelRegister_levelIncreaseThreshold = "FuelRegister.levelIncreaseThreshold";

    /**
    *** Runtime Configuration Property (optional)<br>
    *** The FuelLevel "Decrease" percent threshold (between 0.0 and 1.0 inclusive).<br>
    *** Type: Double
    **/
    public static final String PROP_FuelRegister_levelDecreaseThreshold = "FuelRegister.levelDecreaseThreshold";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** Base URI used for accesing the GTS login (must match web.xml configuration)<br>
    *** Type: String
    **/
    public static final String PROP_track_baseURI                       = "track.baseURI";
    
    /**
    *** Runtime Configuration Property<br>
    *** True to require that cookies be enabled.<br>
    *** If false, session information will be encoded in the URL<br>
    *** Type: Boolean
    **/
    public static final String PROP_track_requireCookies                = "track.requireCookies";
    
    /**
    *** Runtime Configuration Property<br>
    *** The overriding directory, used to find the JavaScript code.<br>
    *** Type: String
    **/
    public static final String PROP_track_js_directory                  = "track.js.directory";
    
    /**
    *** Runtime Configuration Property<br>
    *** The file which contains the text to display when the system is 'Offline'.<br>
    *** Type: File
    **/
    public static final String PROP_track_offlineFile                   = "track.offlineFile";
    
    /**
    *** Runtime Configuration Property<br>
    *** True to enable web-service access.<br>
    *** Type: Boolean
    **/
    public static final String PROP_track_enableService                 = "track.enableService";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** The name of the overriding Class used to provide latitude/longitude subdivision data.<br>
    *** Used for Border-Crossing detection.<br>
    *** Type: Class
    **/
    public static final String PROP_SubdivisionProvider_class           = "SubdivisionProvider.class";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** The name of the overriding Class used to provide the Rule handling EventFunctionMapFactory.<br>
    *** Type: Class
    **/
    public static final String PROP_EventFunctionMapFactory_class       = "EventFunctionMapFactory.class";
    
    /**
    *** Runtime Configuration Property<br>
    *** The name of the overriding Class used to provide the Rule handling EventIdentifierMapFactory.<br>
    *** Type: Class
    **/
    public static final String PROP_EventIdentifierMapFactory_class     = "EventIdentifierMapFactory.class";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** True to enable Transport table unique-id lookup.<br>
    *** (only required when the Device is separate from the Transport)<br>
    *** Type: Boolean
    **/
    public static final String PROP_Transport_queryEnabled              = "Transport.queryEnabled";
    
    /**
    *** Runtime Configuration Property<br>
    *** True to enable UniqueXID table unique-id lookup.<br>
    *** (only required when the UniqueID is separate from the Device)<br>
    *** Type: Boolean
    **/
    public static final String PROP_UniqueXID_queryEnabled              = "UniqueXID.queryEnabled";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** True to allow account/user access to "ALL" device group, if no groups are defined.<br>
    *** Type: Boolean
    **/
    public static final String PROP_db_defaultDeviceAuthorization       = "db.defaultDeviceAuthorization";
    
    /**
    *** Runtime Configuration Property<br>
    *** True to allow account/user access to "ALL" device group, if no groups are defined.<br>
    *** (allows specifying specific Account IDs)<br>
    *** Type: Boolean
    **/
    public static final String PROP_db_defaultDeviceAuthorization_      = PROP_db_defaultDeviceAuthorization + ".";

    // -------
    
    /**
    *** Runtime Configuration Property<br>
    *** The name of the SystemAdmin Account ID.<br>
    *** Type: String
    **/
    public static final String PROP_sysAdmin_account                    = "sysAdmin.account";

    // -------

    protected static RTKey.Entry runtimeKeys[] = {
        new RTKey.Entry("Custom GTS Properties"),
        new RTKey.Entry(PROP_ServiceAccount_ID                  , "opengts"                     , "ServiceAccount ID"),
        new RTKey.Entry(PROP_ServiceAccount_Name                , "OpenGTS"                     , "ServiceAccount Name"),
        new RTKey.Entry(PROP_ServiceAccount_Attr                , ""                            , "ServiceAccount Attributes"),
        new RTKey.Entry(PROP_ServiceAccount_Key                 , ""                            , "ServiceAccount Key"),
        new RTKey.Entry(PROP_StartupInit_class                  , null                          , "Startup Initialization class"),
        new RTKey.Entry(PROP_Device_futureDate_action           , ""                            , "Future Date Action"),
        new RTKey.Entry(PROP_Device_futureDate_maximumSec       , -1L                           , "Future Date Maximm Seconds"),
        new RTKey.Entry(PROP_Device_invalidSpeed_action         , ""                            , "Invalid Speed Action"),
        new RTKey.Entry(PROP_Device_invalidSpeed_maximumKPH     , 0.0                           , "Future Date Maximm Seconds"),
        new RTKey.Entry(PROP_Device_maximumOdometerKM           , 1000000.0                     , "Maximum Odometer value"),
        new RTKey.Entry(PROP_EventData_keyedCreationTime        , false                         , "Keyed 'EventData.creationTime'"),
        new RTKey.Entry(PROP_Geozone_dftRadius_pointRadius      , 3000                          , "Default Point Radius"),
        new RTKey.Entry(PROP_Geozone_dftRadius_polygon          , 500                           , "Default Polygon Radius"),
        new RTKey.Entry(PROP_Geozone_dftRadius_sweptPointRadius , 1000                          , "Default SweptPoint Radius"),
        new RTKey.Entry(PROP_RuleList_includeGroupRules         , false                         , "Include DeviceGroup rules"),
        new RTKey.Entry(PROP_FuelRegister_installFuelManager    , false                         , "Install FuelRegister/FuelManager"),
        new RTKey.Entry(PROP_FuelRegister_levelIncreaseThreshold, 0.03                          , "FuelLevel 'increase' threshold"),
        new RTKey.Entry(PROP_FuelRegister_levelDecreaseThreshold, 0.03                          , "FuelLevel 'decrease' threshold"),
        new RTKey.Entry(PROP_track_baseURI                      , null                          , "'Track' Base URI"),
        new RTKey.Entry(PROP_track_requireCookies               , true                          , "'Track' Require Enabled Cookies"),
        new RTKey.Entry(PROP_track_js_directory                 , "./js"                        , "'Track' JavaScript Directory"),
        new RTKey.Entry(PROP_track_offlineFile                  , null                          , "'Track' Offline File"),
        new RTKey.Entry(PROP_track_enableService                , false                         , "'Track' Enable 'Service'"),
        new RTKey.Entry(PROP_SubdivisionProvider_class          , null                          , "SubdivisionProvider class"),
        new RTKey.Entry(PROP_EventFunctionMapFactory_class      , null                          , "EventFunctionMapFactory subclass"),
        new RTKey.Entry(PROP_EventIdentifierMapFactory_class    , null                          , "EventIdentifierMapFactory subclass"),
        new RTKey.Entry(PROP_Transport_queryEnabled             , false                         , "Enable DB Transport query"),
        new RTKey.Entry(PROP_UniqueXID_queryEnabled             , false                         , "Enable DB UniqueXID query"),
        new RTKey.Entry(PROP_db_defaultDeviceAuthorization      , DEFAULT_DEVICE_AUTHORIZATION  , "Default Device Authoirization"),
        new RTKey.Entry(PROP_sysAdmin_account                   , ""                            , "System Admin Account ID"),
    };

    // ------------------------------------------------------------------------
    // Default User device/group authorization when no groups are defined
    
    /** 
    *** Returns the Device group "ALL" authorization for the specified Account
    *** @param acctID  The account ID for which the Device group "ALL" authorizatio is returned
    *** @return True is Device group "ALL" is authoriized
    **/
    public static boolean GetDefaultDeviceAuthorization(String acctID)
    {
        if (!StringTools.isBlank(acctID)) {
            String idKey = PROP_db_defaultDeviceAuthorization_ + acctID;
            if (RTConfig.hasProperty(idKey)) {
                return RTConfig.getBoolean(idKey, DEFAULT_DEVICE_AUTHORIZATION);
            }
        }
        return RTConfig.getBoolean(PROP_db_defaultDeviceAuthorization, DEFAULT_DEVICE_AUTHORIZATION);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** The GTS_HOME environment variable name
    **/
    public static final String env_GTS_HOME     = "GTS_HOME";
    
    /**
    *** Compares the GTS_HOME environment variable to the current directory.
    *** @return True if GTS_HOME matches current directory, false otherwise
    **/
    public static File get_GTS_HOME()
    {
        String gtsStr = System.getenv(env_GTS_HOME);
        if (!StringTools.isBlank(gtsStr)) {
            try {
                File gtsDir = new File(gtsStr);
                return gtsDir.getCanonicalFile();
            } catch (Throwable th) {
                Print.logWarn("Error attempting to obtain GTS_HOME environment varable");
            }
        }
        return null;

    }

    /**
    *** Compares the GTS_HOME environment variable to the current directory.
    *** @return True if GTS_HOME matches current directory, false otherwise
    **/
    public static boolean check_GTS_HOME()
    {
        File gtsDir  = null;
        File curDir = null;

        /* GS_HOME environment variable */
        try {
            String gtsStr = System.getenv(env_GTS_HOME);
            if (StringTools.isBlank(gtsStr)) {
                Print.logWarn("GTS_HOME environment variable is not defined");
                return false;
            }
            gtsDir = new File(gtsStr);
            try {
                File dir = gtsDir.getCanonicalFile();
                gtsDir = dir;
            } catch (Throwable th) {
                //
            }
        } catch (Throwable th) {
            Print.logWarn("Error attempting to obtain GTS_HOME environment varable");
            return false;
        }

        /* current directory */
        try {
            String curStr = System.getProperty("user.dir","");
            if (StringTools.isBlank(curStr)) {
                Print.logWarn("'user.dir' system property is not defined");
                return false;
            }
            curDir = new File(curStr);
            try {
                File dir = curDir.getCanonicalFile();
                curDir = dir;
            } catch (Throwable th) {
                //
            }
        } catch (Throwable th) {
            Print.logWarn("Error attempting to determine current directory");
            return false;
        }

        /* match? */
        if (!curDir.equals(gtsDir)) {
            Print.logWarn("Warning: GTS_HOME directory does not match current directory!");
            Print.logWarn("GTS_HOME   : " + gtsDir);
            Print.logWarn("Current Dir: " + curDir);
            return false;
        }
        
        /* match */
        return true;
        
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** DBInitialization interface
    **/
    public interface DBInitialization
    {

        /**
        *** Called prior to DB initialization
        **/
        public void preInitialization();

        /**
        *** Called after the standard table factories have been initialized
        **/
        public void addTableFactories();

        /**
        *** Called after DB initialization
        **/
        public void postInitialization();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Add the standard DBFactories as-is
    **/
    public static void addTableFactories()
    {

        /* Standard tables */
        String standardTables[] = new String[] {
            PACKAGE_TABLES_ + "Account"      ,
            PACKAGE_TABLES_ + "AccountString",
            PACKAGE_TABLES_ + "User"         ,
            PACKAGE_TABLES_ + "UserAcl"      ,
            PACKAGE_TABLES_ + "GroupList"    ,
            PACKAGE_TABLES_ + "Device"       ,
            PACKAGE_TABLES_ + "Transport"    ,
            PACKAGE_TABLES_ + "UniqueXID"    ,
            PACKAGE_TABLES_ + "DeviceGroup"  ,
            PACKAGE_TABLES_ + "DeviceList"   ,
            PACKAGE_TABLES_ + "Driver"       ,
            PACKAGE_TABLES_ + "EventData"    ,
            PACKAGE_TABLES_ + "Geozone"      ,
            PACKAGE_TABLES_ + "Resource"     ,
            PACKAGE_TABLES_ + "Role"         ,
            PACKAGE_TABLES_ + "RoleAcl"      ,
            PACKAGE_TABLES_ + "StatusCode"   ,
            PACKAGE_TABLES_ + "SystemProps"  ,
        };
        for (String tableClassName : standardTables) {
            DBAdmin.addTableFactory(tableClassName, true/*required*/);
        }

        /* Extra tables (optional) */
        String extraTables[] = new String[] {
            PACKAGE_EXTRA_TABLES_ + "Antx",
            PACKAGE_EXTRA_TABLES_ + "Entity",
            PACKAGE_EXTRA_TABLES_ + "SessionStats",
            PACKAGE_EXTRA_TABLES_ + "UnassignedDevices",
            PACKAGE_EXTRA_TABLES_ + "PendingCommands",
            PACKAGE_EXTRA_TABLES_ + "SystemAudit"
        };
        for (String tableClassName : extraTables) {
            DBAdmin.addTableFactory(tableClassName, false/*optional*/);
        }

        /* OpenDMTP protocol tables (optional) */
        String dmtpTables[] = new String[] {
            PACKAGE_DMTP_   + "EventTemplate",
            PACKAGE_DMTP_   + "PendingPacket",
            PACKAGE_DMTP_   + "Property"     ,
            PACKAGE_DMTP_   + "Diagnostic"   ,
        };
        for (String tableClassName : dmtpTables) {
            DBAdmin.addTableFactory(tableClassName, false/*optional*/);
        }

        /* Rule/GeoCorridor tables (optional) */
        String ruleTables[] = new String[] {
            PACKAGE_RULE_TABLES_ + "Rule",
            PACKAGE_RULE_TABLES_ + "RuleList",
            PACKAGE_RULE_TABLES_ + "GeoCorridor",
            PACKAGE_RULE_TABLES_ + "GeoCorridorList",
            PACKAGE_RULE_TABLES_ + "NotifyQueue",
            PACKAGE_RULE_TABLES_ + "FuelRegister",
        };
        for (String tableClassName : ruleTables) {
            DBAdmin.addTableFactory(tableClassName, false/*optional*/);
        }

        /* BorderCrossing tables (optional) */
        String bcrossTables[] = new String[] {
            PACKAGE_BCROSS_TABLES_ + "BorderCrossing",
        };
        for (String tableClassName : bcrossTables) {
            DBAdmin.addTableFactory(tableClassName, false/*optional*/);
        }
        
        /* WorkZone tables (optional) */
        String workZoneTables[] = new String[] {
            PACKAGE_EXTRA_TABLES_ + "WorkOrder",
            PACKAGE_EXTRA_TABLES_ + "WorkOrderSample",
            PACKAGE_EXTRA_TABLES_ + "WorkZone",
            PACKAGE_EXTRA_TABLES_ + "WorkZoneList"
        };
        for (String tableClassName : workZoneTables) {
            DBAdmin.addTableFactory(tableClassName, false/*optional*/);
        }

    }

    /**
    *** Initializes all DBFactory classes
    **/
    private static void _initDBFactories(Object startupInit)
    {

        /* set DBFactory CustomFactoryHandler */
        if (startupInit instanceof DBFactory.CustomFactoryHandler) {
            DBFactory.setCustomFactoryHandler((DBFactory.CustomFactoryHandler)startupInit);
        }

        /* register DBFactory classes */
        if (startupInit instanceof DBConfig.DBInitialization) {
            try {
                ((DBConfig.DBInitialization)startupInit).addTableFactories();
            } catch (Throwable th) {
                Print.logException("'<DBConfig.DBInitialization>.addTableFactories' failed!", th);
            }
        } else {
            DBConfig.addTableFactories();
        }
        if (DBAdmin.getTableFactoryCount() <= 0) {
            Print.logStackTrace("No DBFactory classes have been registered!!");
        }

        /* clear DBFactory CustomFactoryHandler (for garbage collection) */
        DBFactory.setCustomFactoryHandler(null);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean       _didPreInit  = false;
    private static boolean       _didPostInit = false;

    /**
    *** Initializes runtime keys and 'version' RTConfig constant property
    *** @return True if this is the first time this method was called, false otherwise
    **/
    private static boolean _dbPreInit()
    {

        /* skip if already initialized */
        if (DBConfig._didPreInit) {
            return false; // already initialized
        }
        DBConfig._didPreInit = true;

        /* add our custom default properties */
        RTKey.addRuntimeEntries(DBConfig.runtimeKeys);

        /* init version RTConfig constant property */
        Version.initVersionProperty();

        /* continue with init */
        return true;

    }

    /**
    *** Initializes the DBFactories
    **/
    private static boolean _dbPostInit()
    {

        /* skip if already initialized */
        if (DBConfig._didPostInit) {
            return false; // already initialized
        }
        DBConfig._didPostInit = true;

        /* default database name */
        String dbName = RTConfig.getString(RTKey.DB_NAME, null);
        if ((dbName == null) || dbName.equals("") || dbName.equals("?")) {
            RTConfig.setString(RTKey.DB_NAME, DBConfig.DEFAULT_DB_NAME);
        }

        /* load external startup initialization class */
        Object startupInit = null;
        String initClassName = RTConfig.getString(DBConfig.PROP_StartupInit_class);
        if (!StringTools.isBlank(initClassName)) {
            try {
                //Print.logInfo("Loading custom Startup initializer: " + initClassName);
                Class cfgClass = Class.forName(initClassName);
                startupInit = cfgClass.newInstance();
            } catch (Throwable th) { // ClassNotFoundException, NoSuchMethodException, etc
                Print.logException("Unable to load Startup initializer: " + initClassName, th);
                startupInit = new StartupInit();
            }
        } else {
            //Print.logInfo("Loading standard/default Startup initializer");
            startupInit = new StartupInit();
        }

        /* custom pre-initialization */
        if (startupInit instanceof DBConfig.DBInitialization) {
            ((DBConfig.DBInitialization)startupInit).preInitialization();
        }

        /* register tables */
        DBConfig._initDBFactories(startupInit);

        /* load 'private.xml' (also loads 'reports.xml' if this is a 'Track' servlet) */
        BasicPrivateLabelLoader.loadPrivateLabelXML();

        /* custom post-initialization */
        if (startupInit instanceof DBConfig.DBInitialization) {
            ((DBConfig.DBInitialization)startupInit).postInitialization();
        }

        /* DCServerFactory init */
        DCServerFactory.init();

        /* get DCServerConfig Command ACL map */
        java.util.List<AclEntry> dcsAclList = null;
        java.util.List<DCServerConfig> dcsList = DCServerFactory.getServerConfigList(false/*inclAll*/);
        if (!ListTools.isEmpty(dcsList)) {
            AclEntry.AccessLevel accLvlValues[] = new AclEntry.AccessLevel[] { AclEntry.AccessLevel.NONE, AclEntry.AccessLevel.WRITE };
            // loop through all loaded DCServerConfig instances
            for (DCServerConfig dcs : dcsList) {
                String cmdNames[] = dcs.getCommandList();
                if (!ListTools.isEmpty(cmdNames)) {
                    // loop through all DCS Commands
                    for (String cn : cmdNames) { 
                        DCServerConfig.Command cmd = dcs.getCommand(cn);
                        String               aclName  = cmd.getAclName();
                        I18N.Text            aclDesc  = new I18N.Text("${dcsDesc} ("+cmd.getDescription()+")");
                        AclEntry.AccessLevel aclDft   = cmd.getAclAccessLevelDefault();
                        AclEntry             aclEntry = new AclEntry(aclName, aclDesc, accLvlValues, aclDft);
                        if (dcsAclList == null) { dcsAclList = new Vector<AclEntry>(); }
                        dcsAclList.add(aclEntry);
                    }
                }
            }
        }

        /* Add command ACLs to all BasicPrivateLabel instances */
        if (!ListTools.isEmpty(dcsAclList)) {
            Collection<String> bplList = BasicPrivateLabelLoader.getPrivateLabelNames(true);
            for (String bplName : bplList) { // loop through all BasicPrivateLabel instances
                BasicPrivateLabel bpl = BasicPrivateLabelLoader.getPrivateLabel(bplName);
                Map<String,AclEntry> aclMap = null;
                for (AclEntry aclEntry : dcsAclList) {
                    String aclName = aclEntry.getName();
                    if (!bpl.hasAclEntry(aclName)) {
                        if (aclMap == null) { aclMap = new OrderedMap<String,AclEntry>(); }
                        aclMap.put(aclName, aclEntry);
                    }
                }
                if (!ListTools.isEmpty(aclMap)) {
                    bpl.addAclMap(null, aclMap);
                }
            }
        }
        
        /* initialized */
        return true;

    }

    /**
    *** Entry point for Servlet WAR programs, which initializes the DBFactories
    **/
    private static boolean _didServletInit = false;
    public static void servletInit(Properties srvCtxProps)
    {

        /* prevent recursive initialization */
        if (DBConfig._didServletInit) {
            return;
        }
        DBConfig._didServletInit = true;

        // servlet runtime initialization
        RTConfig.setWebApp(true); // force isWebapp=true

        /* pre-init */
        DBConfig._dbPreInit();

        /* Display warning if 'srvCtxProps' is null */
        // This means that this is not being called from 'RTConfigContextListener'
        if (srvCtxProps == null) {
            Print.logWarn("*** Servlet RTConfigContextListener was not configured propertly");
        }

        /* Servlet context properties */
        RTConfig.setServletContextProperties(srvCtxProps); // also loads run-time properties
        if (RTConfig.isDebugMode()) {
            Print.setLogLevel(Print.LOG_ALL);                       // log everything
            Print.setLogHeaderLevel(Print.LOG_ALL);                 // include log header on everything
        }

        /* post-init */
        DBConfig._dbPostInit();

    }

    /**
    *** Entry point for various programs/tools which initializes the DBFactories
    *** @param argv  The Command-Line arguments, if any
    *** @param interactive  True if this is invoked from a user interactive command-line tool,
    ***                     False if this is invoked from a server non-interactive command-line tool.
    **/
    public static int cmdLineInit(String argv[], boolean interactive)
    {
        // command-line initialization

        /* skip if already initialized */
        boolean didPreInit = DBConfig._dbPreInit();

        /* command-line args */
        int nextArg = RTConfig.setCommandLineArgs(argv);

        /* logging */
        if (interactive) {
            Print.setLogFile(null);
            if (RTConfig.isDebugMode()) {
                Print.setLogLevel(Print.LOG_ALL);                   // log everything
                Print.setLogHeaderLevel(Print.LOG_ALL);             // include log header on everything
            } else {
                Print.setLogHeaderLevel(Print.LOG_WARN);            // include log header on WARN/ERROR/FATAL
            }
            RTConfig.setBoolean(RTKey.LOG_INCL_DATE, false);        // exclude date
            RTConfig.setBoolean(RTKey.LOG_INCL_STACKFRAME, true);   // include stackframe
        } else {
            if (RTConfig.isDebugMode()) {
                Print.setLogLevel(Print.LOG_ALL);                   // log everything
                Print.setLogHeaderLevel(Print.LOG_ALL);             // include log header on everything
            }
            //RTConfig.setBoolean(RTKey.LOG_INCL_DATE, true);
            //RTConfig.setBoolean(RTKey.LOG_INCL_STACKFRAME, false);
        }

        /* db init */
        if (didPreInit) {
            DBConfig._dbPostInit();
        }

        /* return pointer to next command line arg */
        return nextArg;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return the ServiceAccount ID
    *** @param dft  The default value to return if the ServiceAccount ID is not available
    *** @return The ServiceAccount ID
    **/
    public static String getServiceAccountID(String dft)
    {
        return RTConfig.getString(DBConfig.PROP_ServiceAccount_ID, dft);
    }

    /**
    *** Return the ServiceAccount Name
    *** @param dft  The default value to return if the ServiceAccount Name is not available
    *** @return The ServiceAccount Name
    **/
    public static String getServiceAccountName(String dft)
    {
        return RTConfig.getString(DBConfig.PROP_ServiceAccount_Name, dft);
    }

    /**
    *** Return the ServiceAccount Attributes
    *** @param dft  The default value to return if the ServiceAccount Attributes are not available
    *** @return The ServiceAccount Attributes
    **/
    public static String getServiceAccountAttributes(String dft)
    {
        return RTConfig.getString(DBConfig.PROP_ServiceAccount_Attr, dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_INIT_TABLES[]       = new String[] { "initTables"            };
    private static final String ARG_ACCOUNT[]           = new String[] { "account"               };
    private static final String ARG_NEW_ACCOUNT[]       = new String[] { "newAccount", "account" };
    private static final String ARG_NEW_DEVICE[]        = new String[] { "newDevice"             };
    private static final String ARG_TREE[]              = new String[] { "tree"                  };
    public  static final String ARG_SCHEMA[]            = new String[] { "schema"                };
    private static final String ARG_CREATE_SYSADMIN[]   = new String[] { "createSysAdmin"        };

    /**
    *** Displays command 'usage', then exists
    *** @param msg  The message to display before printing 'usage'
    **/
    private static void usage(String msg, boolean exit)
    {
        if (!StringTools.isBlank(msg)) { 
            Print.sysPrintln(msg);
        }
        Print.sysPrintln("");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + DBConfig.class.getName() + " [-rootUser=<user> [-rootPass=<password>]] {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  Validating tables/columns:");
        Print.sysPrintln("     -tables[=<flags(below)>]");
        Print.sysPrintln("         't' - create missing tables [default]");
        Print.sysPrintln("         'c' - add missing columns");
        Print.sysPrintln("         'a' - alter columns with changed types");
        Print.sysPrintln("         'w' - display warnings");
        Print.sysPrintln("         'u' - check column character-encoding (must be used with 'a' or 'w')");
        Print.sysPrintln("         's' - show columns");
        Print.sysPrintln("  Loading tables from a CSV file:");
        Print.sysPrintln("     -load=<TableName>[.csv] -dir=<Source_Dir> [-overwrite]");
        Print.sysPrintln("  Displaying the DB schema:");
        Print.sysPrintln("     -schema[=<TableName>]");
      //Print.sysPrintln("  Dropping a table (WARNING: cannot be undone!):");
      //Print.sysPrintln("     -drop=<TableName>");
        Print.sysPrintln("");
        if (exit) {
            System.exit(1);
        }
    }

    /**
    *** Displays the DBFactory dependency tree
    *** @param level  The current tree level (used for indentation purposes)
    *** @param fact   The current factory node
    **/
    private static void _displayDependencyTree(int level, DBFactory fact)
    {
        if (fact != null) {
            String lvlStr = StringTools.replicateString("  ", level);
            Print.sysPrintln(lvlStr + "- " + fact.getUntranslatedTableName());
            DBFactory children[] = fact.getChildFactories();
            for (int i = 0; i < children.length; i++) {
                DBFactory child = children[i];
                _displayDependencyTree(level + 1, children[i]);
            }
        }
    }

    /**
    *** Main entry point for providing command-line DB administration tools
    *** @param argv  The command-line arguments
    **/
    public static int _main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main

        /* check minimum logging level */
        if (Print.getLogLevel() < Print.LOG_WARN) {
            Print.setLogLevel(Print.LOG_WARN);
        }

        /* Version header */
        Print.sysPrintln("Version: " + Version.getVersion());

        /* default 'rootUser'/'rootPass' */
        // The following may be required for some of the following operations
        //  -rootUser=<root>
        //  -rootPass=<pass>
        if (!RTConfig.hasProperty(DBAdmin.ARG_ROOT_USER)) {
            // set default root user/pass
            RTConfig.setString(DBAdmin.ARG_ROOT_USER[0], "root");
            RTConfig.setString(DBAdmin.ARG_ROOT_PASS[0], "");
        } else
        if (!RTConfig.hasProperty(DBAdmin.ARG_ROOT_PASS)) {
            // 'rootUser' has been specified, but 'rootPass' is missing.
            RTConfig.setString(DBAdmin.ARG_ROOT_PASS[0], "");
        }

        /* create tables */
        if (RTConfig.hasProperty(ARG_INIT_TABLES)) {
            RTConfig.setBoolean(DBAdmin.ARG_CREATE_DB[0], true);
            RTConfig.setBoolean(DBAdmin.ARG_GRANT[0]    , true);
            if (RTConfig.hasProperty(DBAdmin.ARG_TABLES)) {
                // the command-line overrides anything we would put here
                //String tableOptions = RTConfig.getString(DBAdmin.ARG_TABLES,"");
                //if (tableOptions.indexOf("t") < 0) {
                //    RTConfig.setString(DBAdmin.ARG_TABLES[0], "t" + tableOptions);
                //}
            } else {
                String tableOptions = RTConfig.getString(ARG_INIT_TABLES,"");
                RTConfig.setString(DBAdmin.ARG_TABLES[0], tableOptions);
            }
            // The following are required:
            //   -rootUser=<root>
            //   -rootPass=<pass>
            // These should be available in the config file
            //   db.sql.user=<Grant_User>       - GRANT only
            //   db.sql.pass=<Grant_Pass>       - GRANT only
            //   db.sql.name=<DataBase_Name>    - GRANT only
        }

        /* dump a specific account? (only used by "-dump=<table>") */
        if (RTConfig.hasProperty(ARG_ACCOUNT)) {
            // Warning: this will work only for tables that have an 'accountID' column defined
            String accountID = RTConfig.getString(ARG_ACCOUNT,"");
            // [DB]WHERE (accountID=<account>)
            DBWhere dwh = new DBWhere(Account.getFactory());
            RTConfig.setString(DBAdmin.ARG_WHERE[0],dwh.WHERE_(dwh.EQ(Account.FLD_accountID,accountID)));
            //Print.logInfo("Set WHERE: " + RTConfig.getString("where"));
        }
 
        /* intercept DBAdmin schema: print table schema */
        if (RTConfig.hasProperty(ARG_SCHEMA)) {
            // -schema[=<table>]
            String schemaTable = RTConfig.getString(ARG_SCHEMA,null);
            DBAdmin.printTableSchema(95, SCHEMA_HEADER, schemaTable);
            return 0;
        }

        /* execute commands present in run-time config */
        DBAdminExec dbExecStatus = DBAdmin.execCommands();
        if ((dbExecStatus == null) || dbExecStatus.equals(DBAdminExec.ERROR)) {
            // command found, but an error occurred
            return 1;
        } else 
        if (dbExecStatus.equals(DBAdminExec.WARN)) {
            // command found, but a warning occurred
            // continue ...
        } else 
        if (dbExecStatus.equals(DBAdminExec.EXIT)) {
            // command found, executed successfully, but indicated that this command should terminate
            // (see "reload")
            return 0;
        }
        //int execCmd = dbExecStatus.equals(DBAdminExec.OK)? 1 : 0;
        int execCmd = dbExecStatus.equals(DBAdminExec.NONE)? 0 : 1;

        /* final tables update check */
        if (RTConfig.hasProperty(DBAdmin.ARG_TABLES)) {
            execCmd++;
            // update SystemProps versions
            SystemProps.updateVersions();
        }

        /* create 'sysadmin' account */
        if (RTConfig.hasProperty(ARG_CREATE_SYSADMIN)) {
            execCmd++;
            String sysAdminAcct = AccountRecord.getSystemAdminAccountID();
            String sysAdminPass = StringTools.blankDefault(RTConfig.getString(ARG_CREATE_SYSADMIN,null),"syspass");
            Print.sysPrintln("Creating '"+sysAdminAcct+"' Account ...");
            try {
                if (StringTools.isBlank(sysAdminAcct)) {
                    Print.sysPrintln("  WARNING: No SysAdmin Account-ID defined!");
                } else
                if (Account.exists(sysAdminAcct)) {
                    Print.sysPrintln("  WARNING: SysAdmin account already exists: " + sysAdminAcct);
                } else {
                    Account.createNewAccount(null, sysAdminAcct, sysAdminPass);
                    Print.sysPrintln("  Created 'sysadmin' Account with password: " + sysAdminPass);
                }
            } catch (Throwable th) {
                Print.logException("Error creating 'sysadmin' account", th);
            }
        }

        /* create a default account */
        if (RTConfig.hasProperty(ARG_NEW_ACCOUNT)) {
            execCmd++;
            String acctID = RTConfig.getString(ARG_NEW_ACCOUNT, null);
            if (!StringTools.isBlank(acctID)) {
                try {
                    Account.createNewAccount(null, acctID, Account.BLANK_PASSWORD);
                    Print.sysPrintln("Created account:" + acctID);
                } catch (DBException dbe) {
                    Print.logException("Error creating account: " + acctID, dbe);
                }
            } else {
                Print.sysPrintln("WARNING: New Account name not specified. Account creation ignored.");
            }
        }

        /* create a default device */
        if (RTConfig.hasProperty(ARG_NEW_DEVICE)) {
            execCmd++;
            String acctID = RTConfig.getString(ARG_NEW_ACCOUNT, null);
            if ((acctID != null) && !acctID.equals("")) {
                String devID  = RTConfig.getString(ARG_NEW_DEVICE, null);
                if ((devID != null) && !devID.equals("")) {
                    try {
                        Account account = Account.getAccount(acctID);
                        if (account == null) {
                            throw new DBException("Account not found");
                        }
                        Device.createNewDevice(account, devID, null);
                        Print.sysPrintln("Created device: " + acctID + "," + devID);
                    } catch (DBException dbe) {
                        Print.logException("Error creating account:device: " + acctID + "," + devID, dbe);
                    }
                } else {
                    Print.sysPrintln("WARNING: New Device name not specified. Device creation ignored.");
                }
            } else {
                Print.sysPrintln("WARNING: New Account name not specified. Device creation ignored.");
            }
        }

        /* show dependency tree */
        if (RTConfig.hasProperty(ARG_TREE)) {
            execCmd++;
            Print.sysPrintln("Table dependency tree:");
            _displayDependencyTree(0, DBAdmin.getTableFactory(Account.TABLE_NAME()));
            //try {
            //    (new Account.Key("xtest")).delete(true);
            //    (new Device.Key("xtest","car")).delete(true);
            //} catch (Throwable t) {
            //    Print.logException("",t);
            //}
        }

        /* usage? */
        if (execCmd <= 0) {
            DBConfig.usage("",false);
            return 1;
        } else {
            return 0;
        }

    }

    /**
    *** Main entry point for providing command-line DB administration tools
    *** @param argv  The command-line arguments
    **/
    public static void main(String argv[])
    {
        System.exit(DBConfig._main(argv));
    }

}
