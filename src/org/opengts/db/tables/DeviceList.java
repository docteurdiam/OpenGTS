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
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public class DeviceList
    extends DeviceRecord<DeviceList>
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "DeviceList";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_groupID              = DeviceGroup.FLD_groupID;
    private static DBField FieldInfo[] = {
        // DeviceList fields
        newField_accountID(true),
        new DBField(FLD_groupID, String.class, DBField.TYPE_GROUP_ID(), "Device Group ID", "key=true"),
        newField_deviceID(true),
        // Common fields
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DeviceKey<DeviceList>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String groupId, String deviceId) {
            super.setFieldValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setFieldValue(FLD_groupID  , ((groupId   != null)? groupId  .toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID , ((deviceId  != null)? deviceId .toLowerCase() : ""));
        }
        public DBFactory<DeviceList> getFactory() {
            return DeviceList.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<DeviceList> factory = null;
    public static DBFactory<DeviceList> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                DeviceList.TABLE_NAME(), 
                DeviceList.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                DeviceList.class, 
                DeviceList.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(DeviceGroup.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public DeviceList()
    {
        super();
    }

    /* database record */
    public DeviceList(DeviceList.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceList.class, loc);
        return i18n.getString("DeviceList.description", 
            "This table defines " +
            "the membership of a given Device within a DeviceGroup. " +
            "A Device may be defined in more than one DeviceGroup."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
        
    public String getGroupID()
    {
        String v = (String)this.getFieldValue(FLD_groupID);
        return StringTools.trim(v);
    }
    
    private void setGroupID(String v)
    {
        this.setFieldValue(FLD_groupID, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    public String toString()
    {
        return this.getAccountID() + "/" + this.getGroupID() + "/" + this.getDeviceID();
    }

    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String groupID, String devID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (groupID != null) && (devID != null)) {
            DeviceList.Key devListKey = new DeviceList.Key(acctID, groupID, devID);
            return devListKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // The following is an optimization for holding the Account record while
    // processing this User.  Use with caution.

    private DeviceGroup group = null;
    
    public DeviceGroup getGroup()
    {
        if (this.group == null) {
            try {
                this.group = DeviceGroup.getDeviceGroup(this.getAccount(), this.getGroupID(), false);
            } catch (DBException dbe) {
                this.group = null;
            }
        }
        return this.group;
    }
    
    public void setGroup(DeviceGroup group) 
    {
        if ((group != null) && 
            group.getAccountID().equals(this.getAccountID()) && 
            group.getGroupID().equals(this.getGroupID())) {
            this.group = group;
        } else {
            this.group = null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get/create device list */
    public static DeviceList getDeviceList(DeviceGroup group, String deviceID, boolean createOK)
        throws DBException
    {
        // does not return null, if 'createOK' is true

        /* DeviceGroup specified? */
        if (group == null) {
            throw new DBException("DeviceGroup not specified.");
        }
        String accountID = group.getAccountID();
        String groupID   = group.getGroupID();

        /* device exists? */
        if (StringTools.isBlank(deviceID)) {
            throw new DBException("Device ID not specified.");
        } else
        if (!Device.exists(accountID,deviceID)) {
            //throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* create/save record */
        DeviceList.Key devListKey = new DeviceList.Key(accountID, groupID, deviceID);
        if (devListKey.exists()) { // may throw DBException
            // already exists
            DeviceList list = devListKey.getDBRecord(true);
            list.setGroup(group);
            return list;
        } else
        if (createOK) {
            DeviceList list = devListKey.getDBRecord();
            list.setCreationDefaultValues();
            list.setGroup(group);
            return list;
        } else {
            // record doesn't exist, and caller doesn't want us to create it
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        Print.logWarn("No command-line options available for this table");
    }

}
