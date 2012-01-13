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
//  2008/06/20  Martin D. Flynn
//     -Initial release
//  2009/05/24  Martin D. Flynn
//     -Changed "addDBFields" method 'alwaysAdd' to 'defaultAdd' to only add fields 
//      if not explicitly specified in the runtime conf file.
//  2010/09/09  Martin D. Flynn
//     -Added unit conversion for "getOptionalEventField".
//  2011/08/21  Martin D. Flynn
//     -Modified optional group/device map field specification.
// ----------------------------------------------------------------------------
package org.opengts;

import java.util.Vector;
import java.util.Locale;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.geocoder.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.db.dmtp.*;

/**
*** Provides startup initialization.<br>
*** This class is loaded by <code>DBConfig.java</code> at startup initialization time, and 
*** various methods are called within this class to allow custom DB initialization.<br>
*** The actual class loaded and executed by <code>DBConfig</code> can be overridden by placing 
*** the following line in the system 'default.conf' and 'webapp.conf' files:
*** <pre>
***   startup.initClass=org.opengts.StartupInit
*** </pre>
*** Where 'org.opengts.opt.StartupInit' is the name of the class you wish to have loaded in
*** place of this class file.
**/

public class StartupInit
    // standard/parent StartupInit class
    implements DBConfig.DBInitialization, DBFactory.CustomFactoryHandler
{

    // ------------------------------------------------------------------------

    /* local property keys */
    private String      PROP_RuleFactory_class                  = "RuleFactory.class";
    private String      PROP_PasswordHandler_class              = "PasswordHandler.class";

    /* extra map data fields */
    private String      PROP_OptionalEventFields_FleetMap[]     = {
        "OptionalEventFields.GroupMap",
        "OptionalEventFields.Device"
    };
    private String      PROP_OptionalEventFields_DeviceMap[]    = {
        "OptionalEventFields.DeviceMap",
        "OptionalEventFields.EventData"
    };

    // ------------------------------------------------------------------------

    private boolean     didInitRuleFactory                      = false;
    private RuleFactory ruleFactoryInstance                     = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor.<br>
    *** (Created with the DBConfig db startup initialization)
    **/
    public StartupInit()
    {
        super(); // <-- Object

        /* set a default "User-Agent" in the config file properties (if not already present) */
        RTProperties cfgFileProps = RTConfig.getConfigFileProperties();
        String userAgent = cfgFileProps.getString(RTKey.HTTP_USER_AGENT, null, false);
        if (StringTools.isBlank(userAgent)) {
            // no default "http.userAgent" defined in the config-file properties
            cfgFileProps.setString(RTKey.HTTP_USER_AGENT, "OpenGTS/" + org.opengts.Version.getVersion());
        }
        //Print.logInfo("HTTP User-Agent set to '%s'", HTMLTools.getHttpUserAgent());

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBConfig.DBInitialization interface

    /**
    *** Pre-DBInitialization.<br>
    *** This method is called just before the standard database factory classes are initialized/added.
    **/
    public void preInitialization()
    {
        if (RTConfig.isWebApp()) {
            OSTools.printMemoryUsage();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Opportunity to add custom DBFactory classes.<br>
    *** This method is called just after all standard database factory classes have been intialized/added.
    *** Additional database factories that are needed for the custom installation may be added here.
    **/
    public void addTableFactories()
    {

        /* MUST add standard DBFactories */
        DBConfig.addTableFactories();

        /* add custom DBFactories here */
        //DBAdmin.addTableFactory("com.example.db.tables.MyCustomTable", true);

        /* add custom RuleFactory */
        // See "RuleFactoryExample.java" for more information
        if (!Device.hasRuleFactory()) {
            // To add the RuleFactoryExample module:
            //   Device.setRuleFactory(new RuleFactoryExample());
            // To add a different customized RuleFactory implementation:
            //   Device.setRuleFactory(new org.opengts.extra.rule.RuleFactoryLite());
            RuleFactory rf = this._getRuleFactoryInstance();
            if (rf != null) {
                Device.setRuleFactory(rf);
                Print.logInfo("RuleFactory installed: " + StringTools.className(rf));
            }
        }

        /* add custom map event data handler */
        EventUtil.OptionalEventFields optEvFlds = this.createOptionalEventFieldsHandler();
        EventUtil.setOptionalEventFieldHandler(optEvFlds);
        Print.logDebug("Installed OptionalEventFieldHandler: " + StringTools.className(optEvFlds));

    }
    
    private RuleFactory _getRuleFactoryInstance()
    {
        
        /* already initialized? */
        if (this.ruleFactoryInstance != null) {
            return this.ruleFactoryInstance;
        } else
        if (this.didInitRuleFactory) {
            return null;
        }
        this.didInitRuleFactory = true;

        /* get RuleFactory class */
        Class rfClass      = null;
        String rfClassName = RTConfig.getString(PROP_RuleFactory_class,null);
        try {
            String rfcName = !StringTools.isBlank(rfClassName)?
                rfClassName : 
                (DBConfig.PACKAGE_EXTRA_ + "rule.RuleFactoryLite");
            rfClass     = Class.forName(rfcName);
            rfClassName = rfcName;
        } catch (Throwable th) {
            if (!StringTools.isBlank(rfClassName)) {
                Print.logException("Unable to locate RuleFactory class: " + rfClassName, th);
            }
            return null;
        }

        /* instantiate RuleFactory */
        try {
            this.ruleFactoryInstance = (RuleFactory)rfClass.newInstance();
            return this.ruleFactoryInstance;
        } catch (Throwable th) {
            Print.logException("Unable to instantiate RuleFactory: " + rfClassName, th);
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static class EVField
    {
        private String  name  = "";
        private DBField field = null;
        public EVField(String name) {
            this.name  = StringTools.trim(name);
            this.field = null;
        }
        public EVField(DBField field) {
            this.name  = (field != null)? field._getName() : "";
            this.field = field;
        }
        public String getName() {
            return this.name; // not null
        }
        public boolean hasDBField() {
            return (this.field != null);
        }
        public DBField getDBField() {
            return this.field;
        }
    }

    protected EVField[] parseFields(DBFactory factory, String flda[])
    {
        if (factory == null) {
            return null;
        } else
        if (ListTools.isEmpty(flda)) {
            // no defined field names, return nothing
            return null;
        } else {
            //return factory.getFields(flda); 
            java.util.List<EVField> fldList = new Vector<EVField>();
            for (int i = 0; i < flda.length; i++) {
                String n = StringTools.trim(flda[i]);
                if (!StringTools.isBlank(n)) {
                    DBField dfld = factory.getField(n);
                    fldList.add((dfld != null)? new EVField(dfld) : new EVField(n));
                }
            }
            return !ListTools.isEmpty(fldList)? fldList.toArray(new EVField[fldList.size()]) : null;
        }
    }

    // ------------------------------------------------------------------------
    
    private static final String KEY_fuelLevel           = "fuelLevel";
    private static final String KEY_fuelLevelVolume     = "fuelLevelVolume";
    private static final String KEY_lastFuelLevel       = "lastFuelLevel";
    private static final String KEY_lastFuelLevelVolume = "lastFuelLevelVolume";

    /**
    *** Creates a generic custom EventUtil.OptionalEventFields instance
    *** @return An EventUtil.OptionalEventFields instance
    **/
    protected EventUtil.OptionalEventFields createOptionalEventFieldsHandler()
    {

        /* Group/Fleet map fields */
        final EVField optFleetFields[] = this.parseFields(Device.getFactory()   ,
            RTConfig.getStringArray(PROP_OptionalEventFields_FleetMap , null));

        /* Device/Vehicle map fields */
        final EVField optVehicFields[] = this.parseFields(EventData.getFactory(),
            RTConfig.getStringArray(PROP_OptionalEventFields_DeviceMap, null));

        /* return OptionalEventFields instance */
        return new EventUtil.OptionalEventFields() {

            // return number of 'optional' fields
            public int getOptionalEventFieldCount(boolean isFleet) {
                if (isFleet) {
                    // Group/Fleet map count
                    return ListTools.size(optFleetFields);
                } else {
                    // Device/Vehicle map count
                    return ListTools.size(optVehicFields);
                }
            }

            // return the title for a specific 'optional' field
            public String getOptionalEventFieldTitle(int ndx, boolean isFleetMap, Locale locale) {
                // invalid argument checks
                if (ndx < 0) { 
                    return ""; 
                }
                // default vars
                I18N i18n = I18N.getI18N(StartupInit.class, locale);
                // check map type
                if (isFleetMap) {
                    // "Fleet" map title
                    if (ndx < ListTools.size(optFleetFields)) {
                        String  name  = optFleetFields[ndx].getName();
                        DBField dbfld = optFleetFields[ndx].getDBField();
                        if (dbfld != null) {
                            // KEY_lastFuelLevel, etc.
                            return dbfld.getTitle(locale);
                        } else
                        if (name.equalsIgnoreCase(KEY_fuelLevel)) {
                            return i18n.getString("StartupInit.fuelLevel", "Fuel Level");
                        } else
                        if (name.equalsIgnoreCase(KEY_fuelLevelVolume)) {
                            return i18n.getString("StartupInit.fuelLevelVolume", "Fuel Volume");
                        } else
                        if (name.equalsIgnoreCase(KEY_lastFuelLevelVolume)) {
                            return i18n.getString("StartupInit.lastFuelLevelVolume", "Last Fuel Vol");
                        } else {
                            return name;
                        }
                    }
                    return "";
                } else {
                    // "Device" map title
                    if (ndx < ListTools.size(optVehicFields)) {
                        String  name  = optVehicFields[ndx].getName();
                        DBField dbfld = optVehicFields[ndx].getDBField();
                        if (dbfld != null) {
                            // KEY_fuelLevel, etc
                            return dbfld.getTitle(locale);
                        } else
                        if (name.equalsIgnoreCase(KEY_fuelLevelVolume)) {
                            return i18n.getString("StartupInit.fuelLevelVolume", "Fuel Volume");
                        } else {
                            return name;
                        }
                    }
                    return "";
                    // i18n.getString("StartupInit.info.digInput", "Digital Input");
                }
            }

            // return the value for a specific 'optional' field
            public String getOptionalEventFieldValue(int ndx, boolean isFleetMap, Locale locale, EventDataProvider edp) {
                // invalid argument checks
                if (ndx < 0) { 
                    return ""; 
                } else
                if (!(edp instanceof EventData)) {
                    return "";
                }
                // default vars
                EventData event = (EventData)edp;
                Account account = event.getAccount();
                Device  device  = event.getDevice();
                if ((account == null) || (device == null)) {
                    return "";
                }
                // check map type
                if (isFleetMap) {
                    // Group/Fleet map value
                    if (ndx >= ListTools.size(optFleetFields)) {
                        return "";
                    }
                    String  name  = optFleetFields[ndx].getName();
                    DBField dbfld = optFleetFields[ndx].getDBField();
                    if (name.equalsIgnoreCase(Device.FLD_linkURL)) {
                        // NOTE! Enabling 'getLinkURL' and 'getLinkDescrption' requires
                        // that the following property be specified a '.conf' file:
                        //   startupInit.Device.LinkFieldInfo=true
                        String url = device.getLinkURL();
                        if (!StringTools.isBlank(url)) {
                            String desc = device.getLinkDescription();
                            if (StringTools.isBlank(desc)) {
                                BasicPrivateLabel bpl = Account.getPrivateLabel(account);
                                I18N i18n = I18N.getI18N(StartupInit.class, bpl.getLocale());
                                desc = i18n.getString("StartupInit.info.link", "Link");
                            }
                            return "<a href='"+url+"' target='_blank'>"+desc+"</a>";
                        }
                        return "";
                    } else
                    if (dbfld != null) {
                        Object val = dbfld.getFieldValue(device);
                        val = account.convertFieldUnits(dbfld, val, true/*inclUnits*/, locale);
                        return StringTools.trim(val);
                    } else {
                        BasicPrivateLabel bpl = Account.getPrivateLabel(account);
                        if (name.equalsIgnoreCase(KEY_fuelLevel) ||
                            name.equalsIgnoreCase(KEY_fuelLevelVolume)) {
                            // get from event
                            return event.getFieldValueString(name, "", bpl);
                        } else {
                            // KEY_lastFuelLevel, KEY_lastFuelLevelVolume, etc
                            return device.getFieldValueString(name, "", bpl);
                        }
                    }
                } else {
                    // "Device" map value
                    if (ndx >= ListTools.size(optVehicFields)) {
                        return "";
                    }
                    String  name  = optVehicFields[ndx].getName();
                    DBField dbfld = optVehicFields[ndx].getDBField();
                    if (dbfld != null) {
                        Object val = dbfld.getFieldValue(event);
                        val = account.convertFieldUnits(dbfld, val, true/*inclUnits*/, locale);
                        return StringTools.trim(val);
                    } else {
                        BasicPrivateLabel bpl = Account.getPrivateLabel(account);
                        // KEY_fuelLevel, KEY_fuelLevelVolume, etc
                        Object val = event.getFieldValueString(name, "", bpl);
                        return StringTools.trim(val);
                    }
                }
            }

        };
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Post-DBInitialization.<br>
    *** This method is called after all startup initialization has completed.
    **/
    public void postInitialization()
    {

        /* init StatusCode descriptions */
        StatusCodes.initStatusCodes(null); // include all status codes
        /* //The following specifies the list of specific status codes to include:
        StatusCodes.initStatusCodes(new int[] {
            StatusCodes.STATUS_LOCATION,
            StatusCodes.STATUS_MOTION_START,
            StatusCodes.STATUS_MOTION_IN_MOTION,
            StatusCodes.STATUS_MOTION_STOP,
            StatusCodes.STATUS_MOTION_DORMANT,
            ... include other StatusCodes here ...
        });
        */

        /* This sets the description for all accounts, all 'private.xml' domains, and all Localizations. */
        //StatusCodes.SetDescription(StatusCodes.STATUS_LOCATION      , "Marker");
        //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_START  , "Start Point");
        //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_STOP   , "Stop Point");
        
        /* Install custom PasswordHandler */
        String phClassName = RTConfig.getString(PROP_PasswordHandler_class,null);
        if (StringTools.isBlank(phClassName)) {
            // ignore
        } else
        if (phClassName.equalsIgnoreCase("md5")) {
            Account.setPasswordHandler(Account.MD5PasswordHandler);
        } else
        if (phClassName.equalsIgnoreCase("default")) {
            Account.setPasswordHandler(Account.DefaultPasswordHandler);
        } else {
            try {
                Class phClass = Class.forName(phClassName);
                PasswordHandler ph = (PasswordHandler)phClass.newInstance();
                Account.setPasswordHandler(ph);
            } catch (Throwable th) { // ClassCastException, ClassNotFoundException, ...
                Print.logException("Unable to instantiate PasswordHandler: " + phClassName, th);
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBFactory.CustomFactoryHandler interface

    /**
    *** Create a DBFactory instance.  The DBFactory initialization process will call this method
    *** when creating a DBFactory for a given table, allowing this class to override/customize
    *** any specific table attributes.  If this method returns null, the default table DBFactory
    *** will be created.
    *** @param tableName  The name of the table
    *** @param field      The DBFields in the table
    *** @param keyType    The table key type
    *** @param rcdClass   The DBRecord subclass representing the table
    *** @param keyClass   The DBRecordKey subclass representing the table key
    *** @param editable   True if this table should be editable, false otherwise.  
    ***                   This value is used by the GTSAdmin application.
    *** @param viewable   True if this table should be viewable, false otherwise.  
    ***                   An 'editable' table is automatically considered viewable.
    ***                   This value is used by the GTSAdmin application.
    *** @return The DBFactory instance (or null to indicate that the default DBFactory should be created).
    ***/
    public <T extends DBRecord<T>> DBFactory<T> createDBFactory(
        String tableName, 
        DBField field[], 
        DBFactory.KeyType keyType, 
        Class<T> rcdClass, 
        Class<? extends DBRecordKey<T>> keyClass, 
        boolean editable, boolean viewable)
    {
        //Print.logInfo("Intercept creation of DBFactory: %s", tableName);
        return null; // returning null indicates default behavior
    }

    /**
    *** Augment DBFactory fields.  This method is called before fields have been added to any
    *** given DBFactory.  This method may alter the list of DBFields by adding new fields, or 
    *** altering/deleting existing fields.  However, deleting/altering fields that have other
    *** significant systems dependencies may cause unpredictable behavior.
    *** @param factory  The DBFactory
    *** @param fields   The list of fields scheduled to be added to the DBFactory
    *** @return The list of fields which will be added to the DBFactory
    **/
    public java.util.List<DBField> selectFields(DBFactory factory, java.util.List<DBField> fields)
    {
        String tblName = factory.getUntranslatedTableName();
        // These additional fields can be enabled by placing the appropriate/specified 
        // property "<key>=true" in a 'custom.conf' file.

        /* Account */
        if (tblName.equalsIgnoreCase(Account.TABLE_NAME())) {
            // startupInit.Account.AddressFieldInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_AddressFieldInfo               , false, Account.AddressFieldInfo);
            // startupInit.Account.MapLegendFieldInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_MapLegendFieldInfo             , false, Account.MapLegendFieldInfo);
            // startupInit.Account.AccountManagerInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_AccountManagerInfo             , false, Account.AccountManagerInfo);
            // startupInit.Account.DataPushInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_DataPushInfo                   , false, Account.DataPushInfo);
            return fields;
        }

        /* User */
        if (tblName.equalsIgnoreCase(User.TABLE_NAME())) {
            // startupInit.User.AddressFieldInfo=true
            addDBFields(tblName, fields, User.OPTCOLS_AddressFieldInfo                  , false, User.AddressFieldInfo);
            return fields;
        }

        /* Device */
        if (tblName.equalsIgnoreCase(Device.TABLE_NAME())) {
            // startupInit.Device.NotificationFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_NotificationFieldInfo           , false, Device.NotificationFieldInfo);
            // startupInit.Device.GeoCorridorFieldInfo=true
            boolean devGC = DBConfig.hasRulePackage();
            addDBFields(tblName, fields, Device.OPTCOLS_GeoCorridorFieldInfo            , devGC, Device.GeoCorridorFieldInfo);
            // startupInit.Device.FixedLocationFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_FixedLocationFieldInfo          , false, Device.FixedLocationFieldInfo);
            // startupInit.Device.LinkFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_LinkFieldInfo                   , false, Device.LinkFieldInfo);
            // startupInit.Device.BorderCrossingFieldInfo=true
            boolean devBC = Account.SupportsBorderCrossing();
            addDBFields(tblName, fields, Device.OPTCOLS_BorderCrossingFieldInfo         , devBC, Device.BorderCrossingFieldInfo);
            // startupInit.Device.MaintOdometerFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_MaintOdometerFieldInfo          , false, Device.MaintOdometerFieldInfo);
            // startupInit.Device.WorkOrderInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_WorkOrderInfo                   , false, Device.WorkOrderInfo);
            // startupInit.Device.DataPushInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_DataPushInfo                    , false, Device.DataPushInfo);
            return fields;
        }

        /* DeviceGroup */
        if (tblName.equalsIgnoreCase(DeviceGroup.TABLE_NAME())) {
            // startupInit.DeviceGroup.WorkOrderInfo=true
            addDBFields(tblName, fields, DeviceGroup.OPTCOLS_WorkOrderInfo              , false, DeviceGroup.WorkOrderInfo);
            return fields;
        }

        /* EventData */
        if (tblName.equalsIgnoreCase(EventData.TABLE_NAME())) {
            // startupInit.EventData.AutoIncrementIndex=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AutoIncrementIndex           , false, EventData.AutoIncrementIndex);
            // startupInit.EventData.CreationTimeMillisecond=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CreationTimeMillisecond      , false, EventData.CreationTimeMillisecond);
            // startupInit.EventData.AddressFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AddressFieldInfo             , false, EventData.AddressFieldInfo);
            // startupInit.EventData.GPSFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_GPSFieldInfo                 , false, EventData.GPSFieldInfo);
            // startupInit.EventData.CustomFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CustomFieldInfo              , false, EventData.CustomFieldInfo);
            // startupInit.EventData.GarminFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_GarminFieldInfo              , false, EventData.GarminFieldInfo);
            // startupInit.EventData.CANBUSFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CANBUSFieldInfo              , false, EventData.CANBUSFieldInfo);
            // startupInit.EventData.AtmosphereFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AtmosphereFieldInfo          , false, EventData.AtmosphereFieldInfo);
            // startupInit.EventData.ThermoFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_ThermoFieldInfo              , false, EventData.ThermoFieldInfo, 4);
            // startupInit.EventData.AnalogFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AnalogFieldInfo              , false, EventData.AnalogFieldInfo);
            // startupInit.EventData.EndOfDaySummary=true
            addDBFields(tblName, fields, EventData.OPTCOLS_EndOfDaySummary              , false, EventData.EndOfDaySummary);
            // startupInit.EventData.ServingCellTowerData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_ServingCellTowerData         , false, EventData.ServingCellTowerData);
            // startupInit.EventData.NeighborCellTowerData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_NeighborCellTowerData        , false, EventData.NeighborCellTowerData);
            // startupInit.EventData.WorkZoneGridData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_WorkZoneGridData             , false, EventData.WorkZoneGridData);
            return fields;
        }

        /* Geozone */
        if (tblName.equalsIgnoreCase(Geozone.TABLE_NAME())) {
            // startupInit.Geozone.PriorityFieldInfo
            addDBFields(tblName, fields, "startupInit.Geozone.PriorityFieldInfo"        , false, Geozone.PriorityFieldInfo);
            // startupInit.Geozone.CorridorFieldInfo
            addDBFields(tblName, fields, "startupInit.Geozone.CorridorFieldInfo"        , false, Geozone.CorridorFieldInfo);
            return fields;
        }

        /* leave as-is */
        return fields;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Add the specified fields to the table
    *** @param tblName      The table name
    *** @param tblFields    The list of table fields
    *** @param key          The boolean key used to check for permission to add these fields
    *** @param defaultAdd   The default if the property is not explicitly specfified
    *** @param customFields The fields to add, assuming the boolean key returns true.
    **/
    protected void addDBFields(String tblName, java.util.List<DBField> tblFields, String key, boolean defaultAdd, DBField customFields[])
    {
        this.addDBFields(tblName, tblFields, key, defaultAdd, customFields, -1);
    }

    /**
    *** Add the specified fields to the table
    *** @param tblName      The table name
    *** @param tblFields    The list of table fields
    *** @param key          The boolean key used to check for permission to add these fields
    *** @param defaultAdd   The default if the property is not explicitly specfified
    *** @param customFields The fields to add, assuming the boolean key returns true.
    *** @param maxCount     The maximum number of fields to add from the customFields array
    **/
    protected void addDBFields(String tblName, java.util.List<DBField> tblFields, String key, boolean defaultAdd, DBField customFields[], int maxCount)
    {
        if (StringTools.isBlank(key) || RTConfig.getBoolean(key,defaultAdd)) {
            int cnt = ((maxCount >= 0) && (maxCount <= customFields.length))? maxCount : customFields.length;
            for (int i = 0; i < cnt; i++) {
                tblFields.add(customFields[i]);
            }
        }
    }

    // ------------------------------------------------------------------------

}
