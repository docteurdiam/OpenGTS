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
// NOTE: This table is not currently used!
// ----------------------------------------------------------------------------
// Change History:
//  2008/10/16  Martin D. Flynn
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

public class UserDevice
    extends UserRecord<UserDevice>
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "UserDevice";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_deviceID             = Device.FLD_deviceID;
    private static DBField FieldInfo[] = {
        // UserDevice fields
        newField_accountID(true),
        newField_userID(true),
        new DBField(FLD_deviceID, String.class, DBField.TYPE_DEV_ID(), "Device ID", "key=true"),
        // Common fields
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends UserKey<UserDevice>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String userId, String deviceId) {
            super.setFieldValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setFieldValue(FLD_userID   , ((userId    != null)? userId   .toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID , ((deviceId  != null)? deviceId .toLowerCase() : ""));
        }
        public DBFactory<UserDevice> getFactory() {
            return UserDevice.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<UserDevice> factory = null;
    public static DBFactory<UserDevice> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                UserDevice.TABLE_NAME(), 
                UserDevice.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                UserDevice.class, 
                UserDevice.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(User.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public UserDevice()
    {
        super();
    }

    /* database record */
    public UserDevice(UserDevice.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(UserDevice.class, loc);
        return i18n.getString("UserDevice.description", 
            "This table defines " +
            "the list of authorized devices for a User."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
        
    public String getDeviceID()
    {
        String v = (String)this.getFieldValue(FLD_deviceID);
        return StringTools.trim(v);
    }
    
    private void setDeviceID(String v)
    {
        this.setFieldValue(FLD_deviceID, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    public String toString()
    {
        return this.getAccountID() + "/" + this.getUserID() + "/" + this.getDeviceID();
    }

    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String userID, String deviceID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (userID != null) && (deviceID != null)) {
            UserDevice.Key usrDevKey = new UserDevice.Key(acctID, userID, deviceID);
            return usrDevKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean rebuildUserDeviceTable(User user)
    {

        /* User specified? */
        if (user == null) {
            //throw new DBException("User not specified.");
            return false;
        }
        String accountID = user.getAccountID();
        String userID    = user.getUserID();

        // for (Iterator g = user.groupIterator(); g.hasNext();) {
        //     DeviceGroup group = g.next();
        //     for (Iterator d = group.deviceIterator(); d.hasNext();) {
        //          Device device = d.next();
        //          UserDevice.createUserDevice(user, deviceID);
        //     }
        // }
        
        return true;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get/create user/device entry */
    public static UserDevice getUserDevice(User user, String deviceID, boolean createOK)
        throws DBException
    {
        // does not return null, if 'createOK' is true

        /* User specified? */
        if (user == null) {
            throw new DBException("User not specified.");
        }
        String accountID = user.getAccountID();
        String userID    = user.getUserID();

        /* device exists? */
        if (StringTools.isBlank(deviceID)) {
            throw new DBException("Device ID not specified.");
        } else
        if (!Device.exists(accountID, deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* create/save record */
        UserDevice.Key usrDevKey = new UserDevice.Key(accountID, userID, deviceID);
        if (usrDevKey.exists()) { // may throw DBException
            // already exists
            UserDevice listItem = usrDevKey.getDBRecord(true);
            listItem.setUser(user);
            return listItem;
        } else
        if (createOK) {
            UserDevice listItem = usrDevKey.getDBRecord();
            listItem.setCreationDefaultValues();
            listItem.setUser(user);
            return listItem;
        } else {
            // record doesn't exist, and caller doesn't want us to create it
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the DBSelect statement for the specified account/user */
    protected static DBSelect _getDeviceSelect(String acctId, String userId)
    {

        /* valid account/user? */
        if (StringTools.isBlank(acctId)) {
            return null;
        } else
        if (StringTools.isBlank(userId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT * FROM UserDevice WHERE ((accountID='acct') and (userID='user')) ORDER BY deviceID
        DBSelect<UserDevice> dsel = new DBSelect<UserDevice>(UserDevice.getFactory());
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(UserDevice.FLD_accountID,acctId),
                    dwh.EQ(UserDevice.FLD_userID   ,userId)
                )
            )
        );
        dsel.setOrderByFields(UserDevice.FLD_deviceID);
        return dsel;

    }

    /* return list of all Devices within the specified User (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public static java.util.List<String> getDeviceIDsForUser(String acctId, String userId)
        throws DBException
    {

        /* valid account/user? */
        if (StringTools.isBlank(acctId)) {
            return null;
        } else
        if (StringTools.isBlank(userId)) {
            return null;
        }

        /* get db selector */
        DBSelect dsel = UserDevice._getDeviceSelect(acctId, userId);
        if (dsel == null) {
            return null;
        }

        /* read device for user */
        java.util.List<String> devList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(UserDevice.FLD_deviceID);
                devList.add(devId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get User Devices", sqe);
        } finally {
            DBConnection.release(dbc, stmt, rs);
        }

        /* return list */
        return devList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
