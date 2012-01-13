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
//  2008/03/12  Martin D. Flynn
//     -Initial release
//  2008/05/20  Martin D. Flynn
//     -Added command line options for updating the 'version' properties.
//  2010/04/11  Martin D. Flynn
//     -Added support for PrivateAdmin users.
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.Version;
import org.opengts.db.*;

public class SystemProps
    extends DBRecord<SystemProps>
{
    
    // ------------------------------------------------------------------------
    
    public static final String GTS_VERSION              = "version.gts";
    public static final String DMTP_VERSION             = "version.dmtp";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    private static final String _TABLE_NAME             = "SystemProps";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_propertyID           = "propertyID";
    public static final String FLD_value                = "value";
    private static DBField FieldInfo[] = {
        // Property fields
        new DBField(FLD_propertyID      , String.class  , DBField.TYPE_PROP_ID()   , "Property ID"  , "key=true"),
        new DBField(FLD_value           , String.class  , DBField.TYPE_TEXT        , "Value"        , "edit=2 utf8=true"),
        // Common fields
        newField_description(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DBRecordKey<SystemProps>
    {
        public Key() {
            super();
        }
        public Key(String versId) {
            super.setFieldValue(FLD_propertyID, ((versId != null)? versId.toLowerCase() : ""));
        }
        public DBFactory<SystemProps> getFactory() {
            return SystemProps.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<SystemProps> factory = null;
    public static DBFactory<SystemProps> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                SystemProps.TABLE_NAME(), 
                SystemProps.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                SystemProps.class, 
                SystemProps.Key.class,
                false/*editable*/,false/*viewable*/);
        }
        return factory;
    }

    /* Bean instance */
    public SystemProps()
    {
        super();
    }

    /* database record */
    public SystemProps(SystemProps.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(SystemProps.class, loc);
        return i18n.getString("SystemProps.description", 
            "This table defines " +
            "system-wide installation property key/values."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
    
    /* return the Property ID for this record */
    public String getPropertyID()
    {
        String v = (String)this.getFieldValue(FLD_propertyID);
        return StringTools.trim(v);
    }
    
    /* set the Property ID for this record */
    private void setPropertyID(String v)
    {
        this.setFieldValue(FLD_propertyID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the property value */
    public String getValue()
    {
        String v = (String)this.getFieldValue(FLD_value);
        return StringTools.trim(v);
    }

    /* set the property value */
    public void setValue(String v)
    {
        this.setFieldValue(FLD_value, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription(this.getPropertyID());
        this.setValue("");
        //super.setRuntimeDefaultValues();
    }
    
    // ------------------------------------------------------------------------

    /* return the property value */
    public String getStringValue()
    {
        return this.getValue();
    }

    /* return the property value as an int */
    public int getIntValue(int dft)
    {
        return StringTools.parseInt(this.getValue(), dft);
    }

    /* return the property value as a long */
    public long getLongValue(long dft)
    {
        return StringTools.parseLong(this.getValue(), dft);
    }

    /* return the property value as a float */
    public float getFloatValue(float dft)
    {
        return StringTools.parseFloat(this.getValue(), dft);
    }

    /* return the property value as a double */
    public double getDoubleValue(double dft)
    {
        return StringTools.parseDouble(this.getValue(), dft);
    }

    // ------------------------------------------------------------------------

    /* return the property value */
    public void setStringValue(String v)
    {
        this.setValue(v);
    }

    /* return the property value as an int */
    public void setIntValue(int v)
    {
        this.setValue(String.valueOf(v));
    }

    /* return the property value as a long */
    public void setLongValue(long v)
    {
        this.setValue(String.valueOf(v));
    }

    /* return the property value as a float */
    public void setFloatValue(float v)
    {
        this.setValue(String.valueOf(v));
    }

    /* return the property value as a double */
    public void setDoubleValue(double v)
    {
        this.setValue(String.valueOf(v));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get string value for specified property */
    public static SystemProps getProperty(String propKey)
        throws DBException
    {
        return SystemProps.getProperty(propKey, false);
    }
    
    /* get string value for specified property */
    public static SystemProps getProperty(String propKey, boolean create)
        throws DBException
    {
        if ((propKey == null) || propKey.equals("")) {
            // invalid key specified
            return null;
        } else {
            SystemProps prop = null;
            SystemProps.Key key = new SystemProps.Key(propKey);
            if (key.exists()) { // may throw DBException
                prop = key.getDBRecord(true);
            } else
            if (create) {
                prop = key.getDBRecord();
                prop.setCreationDefaultValues();
            }
            return prop;
        }
    }

    // ------------------------------------------------------------------------

    /* get string value for specified property */
    public static String getStringValue(String propKey, String dft)
    {
        try {
            SystemProps prop = SystemProps.getProperty(propKey, false);
            return (prop != null)? prop.getValue() : dft;
        } catch (DBException dbe) {
            return dft;
        }
    }

    /* return the property value as an int */
    public static int getIntValue(String propKey, int dft)
    {
        String strVal = SystemProps.getStringValue(propKey, null);
        return StringTools.parseInt(strVal, dft);
    }

    /* return the property value as a long */
    public static long getLongValue(String propKey, long dft)
    {
        String strVal = SystemProps.getStringValue(propKey, null);
        return StringTools.parseLong(strVal, dft);
    }

    /* return the property value as a float */
    public static float getFloatValue(String propKey, float dft)
    {
        String strVal = SystemProps.getStringValue(propKey, null);
        return StringTools.parseFloat(strVal, dft);
    }

    /* return the property value as a double */
    public static double getDoubleValue(String propKey, double dft)
    {
        String strVal = SystemProps.getStringValue(propKey, null);
        return StringTools.parseDouble(strVal, dft);
    }

    // ------------------------------------------------------------------------
    
    /* set string value for specified property */
    public static boolean setStringValue(String propKey, String value)
    {
        try {
            SystemProps prop = SystemProps.getProperty(propKey, true);
            if (prop != null) {
                try {
                    prop.setValue(value);
                    prop.save();
                    return true;
                } catch (DBException dbe) {
                    Print.logException("Setting SystemProps", dbe);
                    return false;
                }
            } else {
                return false;
            }
        } catch (DBException dbe) {
            return false;
        }
    }

    /* return the property value as an int */
    public static boolean setIntValue(String propKey, int v)
    {
        return SystemProps.setStringValue(propKey, String.valueOf(v));
    }

    /* return the property value as a long */
    public static boolean setLongValue(String propKey, long v)
    {
        return SystemProps.setStringValue(propKey, String.valueOf(v));
    }

    /* return the property value as a float */
    public static boolean setFloatValue(String propKey, float v)
    {
        return SystemProps.setStringValue(propKey, String.valueOf(v));
    }

    /* return the property value as a double */
    public static boolean setDoubleValue(String propKey, double v)
    {
        return SystemProps.setStringValue(propKey, String.valueOf(v));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get GTS version */
    public static String getGTSVersion(String dft)
    {
        return SystemProps.getStringValue(SystemProps.GTS_VERSION, dft);
    }

    /* get GTS version */
    public static String getGTSVersion()
    {
        return SystemProps.getGTSVersion("");
    }

    // ------------------------------------------------------------------------

    /* get DMTP version */
    public static String getDMTPVersion(String dft)
    {
        return SystemProps.getStringValue(SystemProps.DMTP_VERSION, dft);
    }

    /* get DMTP version */
    public static String getDMTPVersion()
    {
        return SystemProps.getDMTPVersion("");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* update version */
    public static void updateVersions()
    {

        /* OpenGTS version */
        String gtsCurrVersion = Version.getVersion();
        String gtsPropVersion = SystemProps.getGTSVersion();
        if (!gtsCurrVersion.equals(gtsPropVersion)) {
            Print.logInfo("Updating GTS Version: " + gtsCurrVersion);
            SystemProps.setStringValue(SystemProps.GTS_VERSION, gtsCurrVersion);
        }

        /* OpenDMTP version */
        try { 
            // lazily bind to OpenDMTP Version, in case it is not included in this installation
            MethodAction dmtpVersMeth = new MethodAction("org.opendmtp.server.Version", "getVersion");
            String dmtpCurrVersion = (String)dmtpVersMeth.invoke();
            String dmtpPropVersion = SystemProps.getDMTPVersion();
            if (!dmtpCurrVersion.equals(dmtpPropVersion)) {
                Print.logInfo("Updating DMTP Version: " + dmtpCurrVersion);
                SystemProps.setStringValue(SystemProps.DMTP_VERSION, dmtpCurrVersion);
            }
        } catch (Throwable th) {
            // ignore
        }
            

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        Print.sysPrintln("Property '"+GTS_VERSION +"' value: " + SystemProps.getStringValue(SystemProps.GTS_VERSION ,"undefined"));
        Print.sysPrintln("Property '"+DMTP_VERSION+"' value: " + SystemProps.getStringValue(SystemProps.DMTP_VERSION,"undefined"));
        if (RTConfig.getBoolean("update",false)) {
            SystemProps.updateVersions();
        }
    }
    
}
