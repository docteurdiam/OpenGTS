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

public class GroupList
    extends UserRecord<GroupList>
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "GroupList";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_userID               = User.FLD_userID;
    public static final String FLD_groupID              = DeviceGroup.FLD_groupID;
    private static DBField FieldInfo[] = {
        // GroupList fields
        newField_accountID(true),
        newField_userID(true),
        new DBField(FLD_groupID, String.class, DBField.TYPE_GROUP_ID(), "Device Group ID", "key=true"),
        // Common fields
        //newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends UserKey<GroupList>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String userId, String groupId) {
            super.setFieldValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setFieldValue(FLD_userID   , ((userId    != null)? userId   .toLowerCase() : ""));
            super.setFieldValue(FLD_groupID  , ((groupId   != null)? groupId  .toLowerCase() : ""));
        }
        public DBFactory<GroupList> getFactory() {
            return GroupList.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<GroupList> factory = null;
    public static DBFactory<GroupList> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                GroupList.TABLE_NAME(), 
                GroupList.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                GroupList.class, 
                GroupList.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(User.TABLE_NAME());
            factory.addParentTable(DeviceGroup.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public GroupList()
    {
        super();
    }

    /* database record */
    public GroupList(GroupList.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(GroupList.class, loc);
        return i18n.getString("GroupList.description", 
            "This table defines " +
            "the authorized Groups that can be accessed by a given User."
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
        return this.getAccountID() + "/" + this.getUserID() + "/" + this.getGroupID();
    }

    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String userID, String groupID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (userID != null) && (groupID != null)) {
            GroupList.Key grpListKey = new GroupList.Key(acctID, userID, groupID);
            return grpListKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get/create device list entry */
    public static GroupList getGroupList(User user, String groupID, boolean createOK)
        throws DBException
    {
        // does not return null, if 'createOK' is true

        /* User specified? */
        if (user == null) {
            throw new DBException("User not specified.");
        }
        String accountID = user.getAccountID();
        String userID    = user.getUserID();

        /* group exists? */
        if (StringTools.isBlank(groupID)) {
            throw new DBException("DeviceGroup ID not specified.");
        } else
        if (!DeviceGroup.exists(accountID, groupID)) {
            throw new DBException("DeviceGroup does not exist: " + accountID + "/" + groupID);
        }

        /* create/save record */
        GroupList.Key grpListKey = new GroupList.Key(accountID, userID, groupID);
        if (grpListKey.exists()) { // may throw DBException
            // already exists
            GroupList listItem = grpListKey.getDBRecord(true);
            listItem.setUser(user);
            return listItem;
        } else
        if (createOK) {
            GroupList listItem = grpListKey.getDBRecord();
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

    /* return the DBSelect statement for the specified account/group */
    protected static DBSelect _getUserListSelect(String acctId, String groupId)
    {

        /* empty/null account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* empty/null user */
        if (StringTools.isBlank(groupId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT * FROM GroupList WHERE ((accountID='acct') and (groupID='group')) ORDER BY userID
        DBSelect<GroupList> dsel = new DBSelect<GroupList>(GroupList.getFactory());
        dsel.setSelectedFields(GroupList.FLD_userID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(GroupList.FLD_accountID,acctId),
                    dwh.EQ(GroupList.FLD_groupID  ,groupId)
                )
            )
        );
        dsel.setOrderByFields(GroupList.FLD_userID);
        return dsel;

    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED GROUPS) */
    public static java.util.List<String> getUsersForGroup(String acctId, String groupId)
        throws DBException
    {

        /* valid account/groupId? */
        if (StringTools.isBlank(acctId)) {
            return null;
        } else
        if (StringTools.isBlank(groupId)) {
            return null;
        }

        /* get db selector */
        DBSelect dsel = GroupList._getUserListSelect(acctId, groupId);
        if (dsel == null) {
            return null;
        }

        /* read users for group */
        java.util.List<String> usrList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String usrId = rs.getString(GroupList.FLD_userID);
                usrList.add(usrId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group GroupeList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return usrList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
