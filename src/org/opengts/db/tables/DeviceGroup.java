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
//  2007/03/18  Martin D. Flynn
//     -Initial release
//  2007/06/13  Martin D. Flynn
//     -Moved to "org.opengts.db.tables"
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//  2010/04/25  Martin D. Flynn
//     -Fix trimming of 'inactive' Devices
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;

public class DeviceGroup
    extends GroupRecord<DeviceGroup>
{
    
    // ------------------------------------------------------------------------

    /* reserved name for the group containing "ALL" authorized devices */
    public static final String DEVICE_GROUP_ALL             = "all";
    public static final String DEVICE_GROUP_NONE            = "none";

    public static final OrderedSet<String> GROUP_LIST_ALL   = new OrderedSet<String>(new String[] { DEVICE_GROUP_ALL });
    public static final OrderedSet<String> GROUP_LIST_EMPTY = new OrderedSet<String>();

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String  OPTCOLS_WorkOrderInfo       = "startupInit.DeviceGroup.WorkOrderInfo";

    // ------------------------------------------------------------------------

    /* "DeviceGroup" title (ie. "Group", "Fleet", etc) */
    public static String[] GetTitles(Locale loc) 
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return new String[] {
            i18n.getString("DeviceGroup.title.singular", "Group"),
            i18n.getString("DeviceGroup.title.plural"  , "Groups"),
        };
    }
    
    /* Group "All" description */
    public static String GetDeviceGroupAll(Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return i18n.getString("DeviceGroup.allDescription", "All");
    }
        
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "DeviceGroup";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_groupType            = "groupType";
    private static DBField FieldInfo[] = {
        // Group fields
        newField_accountID(true),
        newField_groupID(true),
      //new DBField(FLD_groupType       , Integer.TYPE  , DBField.TYPE_UINT16    , "Device Group Type", "edit=2"),
        // Home GeozoneID?
        // Common fields
        newField_displayName(),
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    // WorkOrder fields
    // startupInit.DeviceGroup.WorkOrderInfo=true
    public static final String FLD_workOrderID          = "workOrderID";           // WorkOrder ID (may not be used)
    public static final DBField WorkOrderInfo[]         = {
        new DBField(FLD_workOrderID , String.class , DBField.TYPE_STRING(512) , "Work Order IDs" , "edit=2"),
    };

    /* key class */
    public static class Key
        extends GroupKey<DeviceGroup>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String groupId) {
            super.setFieldValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setFieldValue(FLD_groupID  , ((groupId   != null)? groupId  .toLowerCase() : ""));
        }
        public DBFactory<DeviceGroup> getFactory() {
            return DeviceGroup.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<DeviceGroup> factory = null;
    public static DBFactory<DeviceGroup> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                DeviceGroup.TABLE_NAME(), 
                DeviceGroup.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                DeviceGroup.class, 
                DeviceGroup.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public DeviceGroup()
    {
        super();
    }

    /* database record */
    public DeviceGroup(DeviceGroup.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return i18n.getString("DeviceGroup.description", 
            "This table defines " +
            "Account specific Device Groups."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the group type */
    public int getGroupType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_groupType);
        return (v != null)? v.intValue() : 0;
    }

    /* set the group type */
    public void setGroupType(int v)
    {
        this.setFieldValue(FLD_groupType, v);
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

    public void setMapLegend(String legend)
    {
        //
    }

    public String getMapLegend()
    {
        return "";
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    /* return string representation of instance */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getGroupID();
    }
    
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription("");
        //super.setRuntimeDefaultValues();
    }
    
    // ------------------------------------------------------------------------

    /* return true if the specified account/group/device exists */
    public boolean isDeviceInDeviceGroup(String deviceID)
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            return DeviceGroup.isDeviceInDeviceGroup(accountID, deviceID, groupID);
        } else {
            return false;
        }
    }

    /* return true if the specified account/group/device exists */
    public boolean isDeviceInDeviceGroup(Device device)
    {
        if (device != null) {
            return this.isDeviceInDeviceGroup(device.getDeviceID());
        } else {
            return false;
        }
    }
    
    /* add device to this group */
    public void addDeviceToDeviceGroup(String deviceID)
        throws DBException
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            DeviceGroup.addDeviceToDeviceGroup(accountID, groupID, deviceID);
        }
    }

    /* add device to this group */
    public void addDeviceToDeviceGroup(Device device)
        throws DBException
    {
        if (device != null) {
            this.addDeviceToDeviceGroup(device.getDeviceID());
        }
    }
    
    /* remove device from this group */
    public void removeDeviceFromDeviceGroup(String deviceID)
        throws DBException
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            DeviceGroup.removeDeviceFromDeviceGroup(accountID, deviceID, groupID);
        }
    }
    
    /* remove device from this group */
    public void removeDeviceFromDeviceGroup(Device device)
        throws DBException
    {
        if (device != null) {
            this.removeDeviceFromDeviceGroup(device.getDeviceID());
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if specified account/group exists */
    public static boolean exists(String acctID, String groupID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID == null) || acctID.equals("")) {
            // invalid account
            return false;
        } else
        if ((groupID == null) || groupID.equals("")) {
            // invalid group
            return false;
        } else
        if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            // 'all' always exists
            return true;
        } else {
            DeviceGroup.Key groupKey = new DeviceGroup.Key(acctID, groupID);
            return groupKey.exists();
        }
    }

    /* return true if specified account/group/device exists */
    public static boolean exists(String acctID, String groupID, String deviceID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (groupID != null) && (deviceID != null)) {
            DeviceList.Key deviceListKey = new DeviceList.Key(acctID, groupID, deviceID);
            return deviceListKey.exists();
        }
        return false;
    }

    /* return true if the specified account/group/device exists */
    public static boolean isDeviceInDeviceGroup(String acctID, String groupID, String deviceID)
    {
        if ((acctID == null) || (groupID == null) || (deviceID == null)) {
            return false;
        } else
        if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            return true;
        } else {
            try {
                return DeviceGroup.exists(acctID, groupID, deviceID);
            } catch (DBException dbe) {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------

    /* add device to device group */
    public static void addDeviceToDeviceGroup(String accountID, String groupID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(accountID,deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* group exists? */
        if (!DeviceGroup.exists(accountID,groupID)) {
            throw new DBException("DeviceGroup does not exist: " + accountID + "/" + groupID);
        }

        /* create/save record */
        DeviceList.Key devListKey = new DeviceList.Key(accountID, groupID, deviceID);
        if (devListKey.exists()) {
            // already exists
        } else {
            DeviceList devListEntry = devListKey.getDBRecord();
            // no other data fields/columns required
            devListEntry.save();
        }

    }

    /* remove device from device group */
    public static void removeDeviceFromDeviceGroup(String accountID, String groupID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(accountID,deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* delete record */
        DeviceList.Key devListKey = new DeviceList.Key(accountID, groupID, deviceID);
        devListKey.delete(false); // no dependencies
        
    }

    // ------------------------------------------------------------------------

    /* Return specified group */
    public static DeviceGroup getDeviceGroup(Account account, String groupId)
        throws DBException
    {
        if (groupId == null) {
            return null;
        } else {
            return DeviceGroup.getDeviceGroup(account, groupId, false);
        }
    }

    /* Return specified group, create if specified */
    public static DeviceGroup getDeviceGroup(Account account, String groupId, boolean createOK)
        throws DBException
    {
        // does not return null, if 'createOK' is true

        /* account-id specified? */
        if (account == null) {
            throw new DBException("Account not specified.");
        }

        /* group-id specified? */
        if (StringTools.isBlank(groupId)) {
            throw new DBException("Device Group-ID not specified.");
        }

        /* get/create group */
        DeviceGroup.Key groupKey = new DeviceGroup.Key(account.getAccountID(), groupId);
        if (groupKey.exists()) { // may throw DBException
            DeviceGroup group = groupKey.getDBRecord(true);
            group.setAccount(account);
            return group;
        } else
        if (createOK) {
            DeviceGroup group = groupKey.getDBRecord();
            group.setAccount(account);
            group.setCreationDefaultValues();
            return group; // not yet saved!
        } else {
            // record doesn't exist, and caller doesn't want us to create it
            return null;
        }

    }

    /* create device group */
    public static DeviceGroup createNewDeviceGroup(Account account, String groupID)
        throws DBException
    {
        if ((account != null) && (groupID != null) && !groupID.equals("")) {
            DeviceGroup group = DeviceGroup.getDeviceGroup(account, groupID, true); // does not return null
            group.save();
            return group;
        } else {
            throw new DBException("Invalid Account/GroupID specified");
        }
    }

    // ------------------------------------------------------------------------

    /* return the DBSelect statement for the specified account/group */
    protected static DBSelect _getDeviceListSelect(String acctId, String groupId, long limit)
    {

        /* empty/null account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* empty/null group */
        if (StringTools.isBlank(groupId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT * FROM DeviceList WHERE ((accountID='acct') and (groupID='group')) ORDER BY deviceID
        DBSelect<DeviceList> dsel = new DBSelect<DeviceList>(DeviceList.getFactory());
        dsel.setSelectedFields(DeviceList.FLD_deviceID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(DeviceList.FLD_accountID,acctId ),
                    dwh.EQ(DeviceList.FLD_groupID  ,groupId)
                )
            )
        );
        dsel.setOrderByFields(DeviceList.FLD_deviceID);
        dsel.setLimit(limit);
        return dsel;

    }

    /* return the number of devices in this group */
    public long getDeviceCount()
    {
        
        /* get db selector */
        String acctId  = this.getAccountID();
        String groupId = this.getGroupID();
        DBSelect dsel = DeviceGroup._getDeviceListSelect(acctId, groupId, -1L);
        if (dsel == null) {
            return 0;
        }

        /* return count */
        try {
            //Print.logInfo("Retrieving count: " + dsel);
            return DBRecord.getRecordCount(DeviceList.getFactory(), dsel.getWhere());
        } catch (DBException dbe) {
            Print.logException("Unable to retrieve DeviceList count", dbe);
            return 0L;
        }
        
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public OrderedSet<String> getDevices(User userAuth, boolean inclInactv)
        throws DBException
    {
        String acctId  = this.getAccountID(); // TODO: matches "userAuth.getAccountID()"?
        String groupId = this.getGroupID();
        return DeviceGroup.getDeviceIDsForGroup(acctId, groupId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public static OrderedSet<String> getDeviceIDsForGroup(
        String acctId, String groupId, User userAuth, 
        boolean inclInactv)
        throws DBException
    {
        return DeviceGroup.getDeviceIDsForGroup(acctId, groupId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public static OrderedSet<String> getDeviceIDsForGroup(
        String acctId, String groupId, User userAuth, 
        boolean inclInactv, long limit)
        throws DBException
    {

        /* valid accountId/groupId? */
        if (StringTools.isBlank(acctId)) {
            return new OrderedSet<String>();
        } else
        if (StringTools.isBlank(groupId)) {
            return new OrderedSet<String>();
        }

        /* "All"? */
        if (groupId.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            return Device.getDeviceIDsForAccount(acctId, userAuth, inclInactv);
        }

        /* get db selector */
        DBSelect dsel = DeviceGroup._getDeviceListSelect(acctId, groupId, limit);
        if (dsel == null) {
            return new OrderedSet<String>();
        }

        /* read Account? */
        Account account = null;
        if (!inclInactv) {
            // We need the Account, to read the Devices, to determine if they are active/inactive
            // There is a chance that the User already has a handle to the Account
            account = (userAuth != null)? userAuth.getAccount() : Account.getAccount(acctId);
            if (account == null) {
                // account not found?
                Print.logWarn("Account not found? " + acctId);
                return new OrderedSet<String>();
            }
        }
        
        /* read devices for account */
        OrderedSet<String> devList = new OrderedSet<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(DeviceList.FLD_deviceID);
                // trim inactive?
                if (!inclInactv) {
                    Device device = (account != null)? Device.getDevice(account, devId) : null;
                    if ((device == null) || !device.isActive()) {
                        continue;
                    }
                }
                // trim unauthorized?
                if ((userAuth != null) && !userAuth.isAuthorizedDevice(devId)) {
                    continue;
                }
                // device ok
                devList.add(devId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group DeviceList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return devList;

    }

    // ------------------------------------------------------------------------

    /* return list of all DeviceGroups owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceGroupsForAccount(String acctId, boolean includeAll)
        throws DBException
    {

        /* select */
        // DBSelect: SELECT * FROM DeviceGroup WHERE (accountID='acct') ORDER BY groupID
        DBSelect<DeviceGroup> dsel = new DBSelect<DeviceGroup>(DeviceGroup.getFactory());
        dsel.setSelectedFields(DeviceGroup.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE(
                dwh.EQ(DeviceGroup.FLD_accountID,acctId)
            )
        );
        dsel.setOrderByFields(DeviceGroup.FLD_groupID);

        /* return list */
        return DeviceGroup.getDeviceGroups(dsel, includeAll);

    }

    /* return list of all DeviceGroups owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceGroups(DBSelect<DeviceGroup> dsel, boolean includeAll)
        throws DBException
    {

        /* group ID list, always add 'All' */
        OrderedSet<String> groupList = new OrderedSet<String>(true);
        
        /* include 'All'? */
        if (includeAll) {
            groupList.add(DeviceGroup.DEVICE_GROUP_ALL);
        }

        /* invalid account */
        if (dsel == null) {
            return groupList;
        }

        /* read device groups for account */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String groupId = rs.getString(DeviceGroup.FLD_groupID);
                //Print.logInfo("Adding DeviceGroup: " + groupId);
                groupList.add(groupId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account DeviceGroup List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return groupList;

    }

    // ------------------------------------------------------------------------

    /* return list of all DeviceGroups in which the specified device is a member */
    public static Collection<String> getDeviceGroupsForDevice(String acctId, String deviceId)
        throws DBException
    {
        return DeviceGroup.getDeviceGroupsForDevice(acctId, deviceId, true);
    }
    
    /* return list of all DeviceGroups in which the specified device is a member */
    public static Collection<String> getDeviceGroupsForDevice(String acctId, String deviceId, boolean inclAll)
        throws DBException
    {

        /* valid Account/Device? */
        try {
            if ((acctId == null) || (deviceId == null) || !Device.exists(acctId,deviceId)) {
                return null;
            }
        } catch (DBException dbe) {
            // error attempting to text device existance
            return null;
        }

        /* group ids */
        java.util.List<String> groupList = new Vector<String>();

        /* include all? */
        if (inclAll) {
            groupList.add(DeviceGroup.DEVICE_GROUP_ALL);
        }

        /* get select */
        // DBSelect: SELECT * FROM DeviceList WHERE ((accountID='acct') and (deviceID='dev')) ORDER BY groupID
        DBSelect<DeviceList> dsel = new DBSelect<DeviceList>(DeviceList.getFactory());
        dsel.setSelectedFields(DeviceList.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(DeviceList.FLD_accountID,acctId  ),
                    dwh.EQ(DeviceList.FLD_deviceID ,deviceId)
                )
            )
        );
        dsel.setOrderByFields(DeviceList.FLD_groupID);

        /* read devices for DeviceGroup */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(DeviceList.FLD_groupID);
                groupList.add(devId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group DeviceList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return groupList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct" };
    private static final String ARG_GROUP[]     = new String[] { "group"  , "grp"  };
    private static final String ARG_CREATE[]    = new String[] { "create" , "cr"   };
    private static final String ARG_EDIT[]      = new String[] { "edit"   , "ed"   };
    private static final String ARG_DELETE[]    = new String[] { "delete"          };
    private static final String ARG_ADD[]       = new String[] { "add"             };
    private static final String ARG_REMOVE[]    = new String[] { "remove"          };
    private static final String ARG_LIST[]      = new String[] { "list"            };

}
