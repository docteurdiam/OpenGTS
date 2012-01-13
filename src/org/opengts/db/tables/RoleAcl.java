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
//  2008/10/16  Martin D. Flynn
//     -Initial release (cloned from UserAcl.java)
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
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

public class RoleAcl
    extends RoleRecord<RoleAcl>
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "RoleAcl";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_aclID                = "aclID";
    public static final String FLD_accessLevel          = "accessLevel";
    private static DBField FieldInfo[] = {
        // RoleAcl fields
        newField_accountID(true),
        newField_roleID(true),
        new DBField(FLD_aclID           , String.class  , DBField.TYPE_STRING(64)  , "ACL ID"       , "key=true"),
        new DBField(FLD_accessLevel     , Integer.TYPE  , DBField.TYPE_UINT16      , "Access Level" , "edit=2 enum=AclEntry$AccessLevel"),
        // Common fields
        newField_description(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends RoleKey<RoleAcl>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String roleId, String aclId) {
            super.setFieldValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_roleID   , ((roleId != null)? roleId.toLowerCase() : ""));
            super.setFieldValue(FLD_aclID    , ((aclId  != null)? aclId .toLowerCase() : ""));
        }
        public DBFactory<RoleAcl> getFactory() {
            return RoleAcl.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<RoleAcl> factory = null;
    public static DBFactory<RoleAcl> getFactory()
    {
        if (factory == null) {
            EnumTools.registerEnumClass(AccessLevel.class);
            factory = DBFactory.createDBFactory(
                RoleAcl.TABLE_NAME(), 
                RoleAcl.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                RoleAcl.class, 
                RoleAcl.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Role.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public RoleAcl()
    {
        super();
    }

    /* database record */
    public RoleAcl(RoleAcl.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(RoleAcl.class, loc);
        return i18n.getString("RoleAcl.description", 
            "This table defines " + 
            "Role specific Access Control permissions."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    public String getAclID()
    {
        String v = (String)this.getFieldValue(FLD_aclID);
        return StringTools.trim(v);
    }
    
    private void setAclID(String v)
    {
        this.setFieldValue(FLD_aclID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public int getAccessLevel()
    {
        Integer v = (Integer)this.getFieldValue(FLD_accessLevel);
        return (v != null)? v.intValue() : 0;
    }

    public void setAccessLevel(int v)
    {
        this.setFieldValue(FLD_accessLevel, EnumTools.getValueOf(AccessLevel.class,v).getIntValue());
    }

    public void setAccessLevel(String v)
    {
        this.setFieldValue(FLD_accessLevel, EnumTools.getValueOf(AccessLevel.class,v).getIntValue());
    }

    public boolean hasReadAccess()
    {
        return AclEntry.okRead(this.getAccessLevel());
    }

    public boolean hasWriteAccess()
    {
        return AclEntry.okWrite(this.getAccessLevel());
    }

    public boolean hasAllAccess()
    {
        // This can be implied to mean 'read all' access if no writing is allowed for this ACL
        return AclEntry.okAll(this.getAccessLevel());
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    public String toString()
    {
        return this.getAccountID() + "/" + this.getRoleID() + "/" + this.getAclID();
    }
    
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // The following is an optimization for holding the Role record while
    // processing this RoleAcl.  Use with caution.

    // ------------------------------------------------------------------------

    /* return true if the specified role ACL exists */
    public static boolean exists(String acctID, String roleID, String aclID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (roleID != null) && (aclID != null)) {
            RoleAcl.Key aclKey = new RoleAcl.Key(acctID, roleID, aclID);
            return aclKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /* return Role access level */
    public static AccessLevel getAccessLevel(RoleAcl ra)
    {
        return (ra != null)? EnumTools.getValueOf(AccessLevel.class,ra.getAccessLevel()) : EnumTools.getDefault(AccessLevel.class);
    }

    /* return Role access level */
    public static AccessLevel getAccessLevel(Role role, String aclId, AccessLevel dftAccess)
    {
        if (role == null) {
            return dftAccess;
        } else
        if (StringTools.isBlank(aclId)) {
            return dftAccess;
        } else {
            try {
                RoleAcl roleAcl = RoleAcl.getRoleAcl(role, aclId); // may throw DBException
                if (roleAcl != null) {
                    return RoleAcl.getAccessLevel(roleAcl);
                } else {
                    return dftAccess;
                }
            } catch (DBException dbe) {
                // error occurred
                return AccessLevel.NONE;
            }
        }
    }

    /* set Role access level */
    public static void setAccessLevel(Role role, String aclId, AccessLevel level)
        throws DBException
    {

        /* role specified? */
        if (role == null) {
            throw new DBException("Role not specified.");
        }
        String acctId = role.getAccountID();
        String roleId = role.getRoleID();

        /* acl-id specified? */
        if (StringTools.isBlank(aclId)) {
            throw new DBException("Acl-ID not specified.");
        }

        /* get/create role */
        RoleAcl roleAcl = null;
        RoleAcl.Key aclKey = new RoleAcl.Key(acctId, roleId, aclId);
        if (aclKey.exists()) { // may throw DBException
            roleAcl = RoleAcl.getRoleAcl(role, aclId); // may throw DBException
        } else {
            roleAcl = aclKey.getDBRecord();
            roleAcl.setRole(role);
        }
        
        /* set access level */
        int levelInt = (level != null)? level.getIntValue() : AccessLevel.NONE.getIntValue();
        roleAcl.setAccessLevel(levelInt);
        
        /* save */
        roleAcl.save(); // may throw DBException

    }

    /* set Role access level */
    public static boolean deleteAccessLevel(Role role, String aclId)
        throws DBException
    {

        /* role specified? */
        if (role == null) {
            return false; // quietly ignore
        }
        String acctId = role.getAccountID();
        String roleId = role.getRoleID();

        /* acl-id specified? */
        if (StringTools.isBlank(aclId)) {
            return false; // quietly ignore
        }

        /* already deleted? */
        boolean aclExists = RoleAcl.exists(acctId, roleId, aclId);
        if (!aclExists) {
            return false;
        }

        /* delete */
        RoleAcl.Key aclKey = new RoleAcl.Key(acctId, roleId, aclId);
        aclKey.delete(true); // also delete dependencies
        return true;

    }

    // ------------------------------------------------------------------------

    /* Return specified role */
    public static RoleAcl getRoleAcl(Role role, String aclId)
        throws DBException
    {
        if ((role != null) && (aclId != null)) {
            RoleAcl.Key aclKey = new RoleAcl.Key(role.getAccountID(), role.getRoleID(), aclId);
            if (aclKey.exists()) {
                RoleAcl roleAcl = aclKey.getDBRecord(true);
                roleAcl.setRole(role);
                return roleAcl;
            } else {
                return null;
            }
        } else {
            throw new DBException("Role or AclID is null");
        }
    }

    /* Return specified role ACL, create if specified */
    public static RoleAcl getRoleAcl(Role role, String aclId, boolean create)
        throws DBException
    {
        // does not return null

        /* role specified? */
        if (role == null) {
            throw new DBNotFoundException("Role not specified.");
        }
        String acctId = role.getAccountID();
        String roleId = role.getRoleID();

        /* acl-id specified? */
        if (StringTools.isBlank(aclId)) {
            throw new DBNotFoundException("Acl-ID not specified.");
        }

        /* get/create role */
        RoleAcl roleAcl = null;
        RoleAcl.Key aclKey = new RoleAcl.Key(acctId, roleId, aclId);
        if (!aclKey.exists()) { // may throw DBException
            if (create) {
                roleAcl = aclKey.getDBRecord();
                roleAcl.setRole(role);
                roleAcl.setCreationDefaultValues();
                return roleAcl; // not yet saved!
            } else {
                throw new DBNotFoundException("Acl-ID does not exists '" + aclKey + "'");
            }
        } else
        if (create) {
            // we've been asked to create the Acl, and it already exists
            throw new DBAlreadyExistsException("Acl-ID already exists '" + aclKey + "'");
        } else {
            roleAcl = RoleAcl.getRoleAcl(role, aclId); // may throw DBException
            if (roleAcl == null) {
                throw new DBException("Unable to read existing Role-ID '" + aclKey + "'");
            }
            return roleAcl;
        }

    }

    /* Create specified role.  Return null if acl already exists */
    public static RoleAcl createNewRoleAcl(Role role, String aclID)
        throws DBException
    {
        RoleAcl roleAcl = RoleAcl.getRoleAcl(role, aclID, true);
        if (roleAcl != null) {
            roleAcl.save();
        }
        return roleAcl;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct" };
    private static final String ARG_ROLE[]      = new String[] { "role"            };
    private static final String ARG_LIST[]      = new String[] { "list"            };
    private static final String ARG_ACL[]       = new String[] { "acl"             };
    private static final String ARG_SET[]       = new String[] { "set"             };
    private static final String ARG_CREATE[]    = new String[] { "create" , "cr"   };
    private static final String ARG_EDIT[]      = new String[] { "edit"   , "ed"   };
    private static final String ARG_DELETE[]    = new String[] { "delete" , "purge"};

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + RoleAcl.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns Role");
        Print.logInfo("  -role=<id>      Role ID which owns RoleAcl");
        Print.logInfo("  -list           List Acls for Role");
        Print.logInfo("  -acl=<id>       Role ID to create/edit");
        Print.logInfo("  -set=<val>      RoleAcl value (create if necessary)");
        Print.logInfo("  -create         Create a new RoleAcl");
        Print.logInfo("  -edit           Edit an existing (or newly created) RoleAcl");
        Print.logInfo("  -delete         Delete specified RoleAcl");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String roleID  = RTConfig.getString(ARG_ROLE   , "");
        String aclID   = RTConfig.getString(ARG_ACL    , "");

        /* account-id specified? */
        if ((acctID == null) || acctID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may return DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* role-id specified? */
        if ((roleID == null) || roleID.equals("")) {
            Print.logError("Role-ID not specified.");
            usage();
        }

        /* get role */
        Role role = null;
        try {
            role = Role.getRole(acct, roleID); // may return DBException
            if (role == null) {
                Print.logError("Role-ID does not exist: " + acctID + "/" + roleID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Role: " + acctID + "/" + roleID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* RoleAcl exists? */
        boolean aclExists = false;
        if ((aclID != null) && !aclID.equals("")) {
            try {
                aclExists = RoleAcl.exists(acctID, roleID, aclID);
            } catch (DBException dbe) {
                Print.logError("Error determining if RoleAcl exists: " + acctID + "/" + roleID + "/" + aclID);
                System.exit(99);
            }
        }
        
        /* option count */
        int opts = 0;
        
        /* list */
        if (RTConfig.getBoolean(ARG_LIST, false)) {
            opts++;
            try {
                String aclList[] = role.getAclsForRole();
                for (int i = 0; i < aclList.length; i++) {
                    AccessLevel level = RoleAcl.getAccessLevel(role, aclList[i], AccessLevel.NONE);
                    Print.sysPrintln("  " + aclList[i] + " ==> " + level);
                }
            } catch (DBException dbe) {
                Print.logError("Error getting Acl list: " + dbe);
                System.exit(99);
            }
            System.exit(0);
        }
        
        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !acctID.equals("") && !roleID.equals("")) {
            opts++;
            if (!aclExists) {
                Print.logWarn("RoleAcl does not exist: " + acctID + "/" + roleID + "/" + aclID);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                RoleAcl.Key aclKey = new RoleAcl.Key(acctID, roleID, aclID);
                aclKey.delete(true); // also delete dependencies
                Print.logInfo("RoleAcl deleted: " + acctID + "/" + roleID + "/" + aclID);
            } catch (DBException dbe) {
                Print.logError("Error deleting RoleAcl: " + acctID + "/" + roleID + "/" + aclID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (aclExists) {
                Print.logWarn("RoleAcl already exists: " + acctID + "/" + roleID + "/" + aclID);
            } else {
                try {
                    RoleAcl.createNewRoleAcl(role, aclID);
                    Print.logInfo("Created RoleAcl: " + acctID + "/" + roleID + "/" + aclID);
                    aclExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating RoleAcl: " + acctID + "/" + roleID + "/" + aclID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }
        
        /* set */
        if (RTConfig.hasProperty(ARG_SET)) {
            opts++;
            AccessLevel aclLevel = EnumTools.getValueOf(AccessLevel.class,RTConfig.getInt(ARG_SET,-1));
            try {
                RoleAcl.setAccessLevel(role, aclID, aclLevel);
                Print.logInfo("Set RoleAcl '" + acctID + "/" + roleID + "/" + aclID + "' to level " + aclLevel);
            } catch (DBException dbe) {
                Print.logError("Error setting RoleAcl: "+ acctID + "/" + roleID + "/" + aclID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT, false)) {
            opts++;
            if (!aclExists) {
                Print.logError("RoleAcl does not exist: " + acctID + "/" + roleID + "/" + aclID);
            } else {
                try {
                    RoleAcl roleAcl = RoleAcl.getRoleAcl(role, aclID, false); // may throw DBException
                    DBEdit editor = new DBEdit(roleAcl);
                    editor.edit(); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing RoleAcl: "+ acctID + "/" + roleID + "/" + aclID);
                    dbe.printException();
                }
            }
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

    // ------------------------------------------------------------------------

}
