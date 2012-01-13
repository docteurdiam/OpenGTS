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
//  2007/04/15  Martin D. Flynn
//     -Initial release
//  2007/04/17  Martin D. Flynn
//     -Fixed use of reserved word for column name ("key" now called "propKey")
//  2007/09/16  Martin D. Flynn
//     -Added 'timestamp' and 'binaryValue' columns
//     -Removed 'value' columns.
//  2007/01/10  Martin D. Flynn
//     -Removed 'sequence', 'readOnly' columns.
//     -Removed 'key' attribute from 'timestamp' column.
//  2008/02/17  Martin D. Flynn
//     -Added command-line tool for listing account/device properties
//  2008/03/28  Martin D. Flynn
//     -Incorporate "DBRecord.select(DBSelect,...) method
// ----------------------------------------------------------------------------
package org.opengts.db.dmtp;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

// ----------------------------------------------------------------------------

public class Property
    extends DeviceRecord<Property>
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME               = "Property";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_propKey               = "propKey";
    public static final String FLD_timestamp             = "timestamp";
    public static final String FLD_binaryValue           = "binaryValue";
    private static DBField FieldInfo[] = {
        // Property fields
        newField_accountID(true),
        newField_deviceID(true),
        new DBField(FLD_propKey         , Integer.TYPE  , DBField.TYPE_UINT32      , "Property Key" , "key=true"),
        new DBField(FLD_timestamp       , Long.TYPE     , DBField.TYPE_UINT32      , "Timestamp"    , null),
        new DBField(FLD_binaryValue     , byte[].class  , DBField.TYPE_BLOB        , "Binary Value" , null),
    };

    /* key class */
    public static class Key
        extends DeviceKey<Property>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId, int propKey) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_propKey   , propKey);
        }
        public DBFactory<Property> getFactory() {
            return Property.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Property> factory = null;
    public static DBFactory<Property> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Property.TABLE_NAME(), 
                Property.FieldInfo, 
                DBFactory.KeyType.PRIMARY, 
                Property.class, 
                Property.Key.class,
                false/*editable*/,false/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Property()
    {
        super();
    }

    /* database record */
    public Property(Property.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Property.class, loc);
        return i18n.getString("Property.description", 
            "This table contains " +
            "Device specific property information collected from client devices."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    public int getPropKey()
    {
        Integer v = (Integer)this.getFieldValue(FLD_propKey);
        return (v != null)? v.intValue() : 0x0000;
    }
    
    public String getPropKeyHex()
    {
        return StringTools.toHexString(this.getPropKey(), 16);
    }

    public void setPropKey(int v)
    {
        this.setFieldValue(FLD_propKey, v);
    }
    
    // ------------------------------------------------------------------------

    public long getTimestamp()
    {
        Long v = (Long)this.getFieldValue(FLD_timestamp);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setTimestamp(long v)
    {
        this.setFieldValue(FLD_timestamp, v);
    }

    // ------------------------------------------------------------------------

    public byte[] getBinaryValue()
    {
        byte v[] = (byte[])this.getFieldValue(FLD_binaryValue);
        return (v != null)? v : new byte[0];
    }
    
    private void setBinaryValue(byte[] v)
    {
        this.setFieldValue(FLD_binaryValue, ((v != null)? v : new byte[0]));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* save property entry */
    public static boolean saveProperty(Device device, int propKey, byte propVal[])
        throws DBException
    {
        if (device != null) {
            String acctID = device.getAccountID();
            String devID  = device.getDeviceID();
            return Property.saveProperty(acctID, devID, propKey, propVal);
        } else {
            return false;
        }
    }
    
    /* save property entry */
    public static boolean saveProperty(String acctID, String devID, int propKey, byte propVal[])
        throws DBException
    {
        if ((acctID != null) && !acctID.equals("") && 
            (devID  != null) && !devID.equals("")  && 
            (propKey > 0)    && (propVal != null)    ) {
            Property.Key key = new Property.Key(acctID, devID, propKey);
            Property prop = new Property(key);
            prop.setTimestamp(DateTime.getCurrentTimeSec());
            prop.setBinaryValue(propVal);
            prop.save();
            return true;
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

    /* return string representation */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getAccountID()).append("/").append(this.getDeviceID());
        sb.append(" ");
        PropertyKey pk = this.getPropertyKey();
        if (pk != null) {
            sb.append(pk.getName());
            sb.append(" [").append(pk.getKeyHex()).append("] ");
            sb.append(pk.toStringValue(this.getBinaryValue()));
        } else {
            sb.append("[").append(StringTools.toHexString(this.getPropKey(),16)).append("] ");
            sb.append("0x").append(StringTools.toHexString(this.getBinaryValue()));
        }
        return sb.toString();
    }

    /* get description */
    public String getDescription()
    {
        return PropertyKey.GetKeyDescription(this.getPropKey());
    }

    /* return contained value as a String */
    public String getValueAsString()
    {
        PropertyKey pk = this.getPropertyKey();
        byte     bin[] = this.getBinaryValue();
        if (pk != null) {
            return pk.toStringValue(bin);
        } else {
            Print.logWarn("Invalid Property Key: " + this.getPropKeyHex());
            return "0x" + StringTools.toHexString(bin);
        }
    }

    // ------------------------------------------------------------------------
    
    public PropertyKey getPropertyKey()
    {
        return PropertyKey.GetPropertyKey(this.getPropKey());
    }

    public String getName()
    {
        PropertyKey pk = this.getPropertyKey();
        if (pk != null) {
            return pk.getName();
        } else {
            return "";
        }
    }

    public int getPropertyType()
    {
        PropertyKey pk = this.getPropertyKey();
        if (pk != null) {
            return pk.getType();
        } else {
            return PropertyKey.TYPE_UNKNOWN;
        }
    }

    public boolean isNumeric()
    {
        PropertyKey pk = this.getPropertyKey();
        if (pk != null) {
            return pk.isNumeric();
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the property value as an array of int's (or null if the property is not numeric) */
    public int[] getIntValues()
    {
        PropertyKey pk = this.getPropertyKey();
        if (pk != null) {
            return pk.toIntArray(this.getBinaryValue());
        } else {
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    
    /* return the property value as an array of double's (or null if the property is not numeric) */
    public double[] getDoubleValues()
    {
        PropertyKey pk = this.getPropertyKey();
        if (pk != null) {
            return pk.toDoubleArray(this.getBinaryValue());
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get all property records for specified account/device */
    public static Property[] getProperties(String acctId, String devId)
        throws DBException
    {

        /* invalid account/device? */
        if ((acctId == null) || acctId.equals("")) {
            return null;
        } else
        if ((devId == null) || devId.equals("")) {
            return null;
        }
        
        /* where clause */
        // DBSelect: WHERE ((accountID=='acct')AND(deviceID='dev')) ORDER BY propKey
        DBSelect<Property> dsel = new DBSelect<Property>(Property.getFactory());
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(FLD_accountID,acctId),
                dwh.EQ(FLD_deviceID ,devId)
            )
        ));
        dsel.setOrderByFields(FLD_propKey);
        dsel.setLimit(-1L);

        /* get Properties */
        Property p[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //p = (Property[])DBRecord.select(Property.getFactory(), dsel.toString(false));
            p = DBRecord.select(dsel); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }

        /* no properties */
        if (p == null) {
            // no records
            return null;
        }

        /* return array of properties */
        return p;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below
    
    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct"  };
    private static final String ARG_DEVICE[]    = new String[] { "device" , "dev"   };
    private static final String ARG_LIST[]      = new String[] { "list"             };
    private static final String ARG_DEMO[]      = new String[] { "demo"             };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Property.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>           Acount ID which owns the specified Device");
        Print.logInfo("  -device=<id>            Device ID to read properties");
        Print.logInfo("  -list                   List properties");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String accountID = RTConfig.getString(ARG_ACCOUNT, "");
        String deviceID  = RTConfig.getString(ARG_DEVICE , "");

        /* account-id specified? */
        if ((accountID == null) || accountID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(accountID); // may return DBException
            if (account == null) {
                Print.logError("Account-ID does not exist: " + accountID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + accountID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* get device */
        OrderedSet<String> deviceList = null;
        try {
            if ((deviceID != null) && !deviceID.equals("")) {
                if (Device.exists(accountID, deviceID)) {
                    deviceList = new OrderedSet<String>(new String[] { deviceID });
                } else {
                    Print.logError("Device-ID does not exist: " + deviceID);
                    usage();
                }
            } else {
                deviceList = Device.getDeviceIDsForAccount(accountID, null, true);
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Devices from Account: " + accountID, dbe);
            System.exit(99);
        }
        
        /* option count */
        int opts = 0;

        /* list client properties */
        if (RTConfig.hasProperty(ARG_LIST)) {
            opts++;
            Print.sysPrintln("");
            for (int d = 0; d < deviceList.size(); d++) {
                String devId = deviceList.get(d);
                try {
                    Device device = Device.getDevice(account, devId);
                    if (device != null) {
                        Print.sysPrintln("Properties: " + accountID + "/" + devId + " [\"" + device.getDescription() + "\"]:");
                        Property p[] = Property.getProperties(accountID, devId);
                        if ((p != null) && (p.length > 0)) {
                            for (int i = 0; i < p.length; i++) {
                                PropertyKey pk = p[i].getPropertyKey();
                                StringBuffer sb = new StringBuffer("  ");
                                sb.append("[0x").append(p[i].getPropKeyHex()).append("]").append(p[i].getName());
                                sb.append(" = ").append(p[i].getValueAsString());
                                sb.append("  // ").append((new DateTime(p[i].getTimestamp())).toString());
                                Print.sysPrintln(sb.toString());
                            }
                        } else {
                            Print.sysPrintln("  No properties");
                        }
                    } else {
                        Print.logError("Device-ID does not exist: " + devId);
                    }
                } catch (Throwable th) {
                    Print.logException("Error while getting properties for account/device", th);
                    System.exit(1);
                }
                Print.sysPrintln("");
            }
            System.exit(0);
        }

        /* list client properties */
        if (RTConfig.hasProperty(ARG_DEMO)) {
            opts++;
            try {
                Property.Key key = new Property.Key("demo","demo",PropertyKey.PROP_STATE_DEVICE_ID);
                Property record = key.getDBRecord();
                record.setBinaryValue("demo".getBytes());
                record.save();
            } catch (DBException dbe) {
                Print.logException("Error creating demo Property", dbe);
                System.exit(99);
            }
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }
    
}
