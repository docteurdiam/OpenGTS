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
//     -Initial release (cloned from User.java)
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

public class Role
    extends RoleRecord<Role>
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME                  = "Role";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    private static DBField FieldInfo[] = {
        // Key fields
        newField_accountID(true),
        newField_roleID(true),
        // Common fields
        newField_displayName(),
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends RoleKey<Role>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String roleId) {
            super.setFieldValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_roleID   , ((roleId != null)? roleId.toLowerCase() : ""));
        }
        public DBFactory<Role> getFactory() {
            return Role.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Role> factory = null;
    public static DBFactory<Role> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Role.TABLE_NAME(), 
                Role.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                Role.class, 
                Role.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Role()
    {
        super();
    }

    /* database record */
    public Role(Role.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Role.class, loc);
        return i18n.getString("Role.description", 
            "This table defines " +
            "Account specific Roles."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription("New Role");
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if this is a System Admin role */
    public boolean isSystemAdminRole()
    {
        return AccountRecord.isSystemAdminAccountID(this.getAccountID());
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return list of all currently defined ACLs for this Role */
    // does not return null
    public String[] getAclsForRole()
        throws DBException
    {
        String acctID = this.getAccountID();
        
        /* read ACLs for role */
        java.util.List<String> aclList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
        
            /* select */
            // DBSelect: SELECT aclID FROM RoleAcl WHERE (accountID='acct') AND (roleID='role') ORDER BY aclID
            DBSelect<RoleAcl> dsel = new DBSelect<RoleAcl>(RoleAcl.getFactory());
            dsel.setSelectedFields(RoleAcl.FLD_aclID);
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(RoleAcl.FLD_accountID, this.getAccountID()),
                    dwh.EQ(RoleAcl.FLD_roleID   , this.getRoleID())
                )
            ));
            dsel.setOrderByFields(RoleAcl.FLD_aclID);
    
            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
                String aclId = rs.getString(RoleAcl.FLD_aclID);
                aclList.add(aclId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Role ACL List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return aclList.toArray(new String[aclList.size()]);

    }

    // ------------------------------------------------------------------------

    /* to String value */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getRoleID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if the specified role exists */
    public static boolean exists(String acctID, String roleID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (roleID != null)) {
            Role.Key roleKey = new Role.Key(acctID, roleID);
            return roleKey.exists();
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /* Return specified role (may return null) */
    public static Role getRole(String accountId, String roleId)
        throws DBException
    {

        /* validate RoleID */
        if (StringTools.isBlank(roleId)) {
            throw new DBException("RoleID is blank/null");
        }

        /* validate AccountID */
        String acctId = RoleRecord.isSystemAdminRoleID(roleId)? AccountRecord.getSystemAdminAccountID() : accountId;
        if (StringTools.isBlank(acctId)) {
            throw new DBException("AccountID is blank/null");
        }

        /* get Role */
        Role.Key roleKey = new Role.Key(acctId, roleId);
        if (roleKey.exists()) {
            Role role = roleKey.getDBRecord(true);
            return role;
        } else {
            return null;
        }

    }

    /* Return specified role (may return null) */
    public static Role getRole(Account account, String roleId)
        throws DBException
    {
        if (account != null) {
            String acctId = account.getAccountID();
            Role role = Role.getRole(acctId, roleId);
            if (role != null) {
                if (acctId.equals(role.getAccountID()) ){
                    role.setAccount(account);
                }
                return role;
            } else {
                return null;
            }
        } else {
            throw new DBException("Account is null");
        }
    }

    /* Return specified role, create if specified (does not return null) */
    public static Role getRole(Account account, String roleId, boolean create)
        throws DBException
    {

        /* account specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctId = account.getAccountID();

        /* role-id specified? */
        if (StringTools.isBlank(roleId)) {
            throw new DBNotFoundException("RoleID not specified.");
        } else
        if (Role.isSystemAdminRoleID(roleId) && !account.isSystemAdmin()) {
            throw new DBNotFoundException("RoleID not allow for non system admin.");
        }

        /* get/create role */
        Role role = null;
        Role.Key roleKey = new Role.Key(acctId, roleId);
        if (!roleKey.exists()) { // may throw DBException
            if (create) {
                role = roleKey.getDBRecord();
                role.setAccount(account); // new record
                role.setCreationDefaultValues();
                return role; // not yet saved!
            } else {
                throw new DBNotFoundException("Role-ID does not exists '" + roleKey + "'");
            }
        } else
        if (create) {
            // we've been asked to create the role, and it already exists
            throw new DBAlreadyExistsException("Role-ID already exists '" + roleKey + "'");
        } else {
            role = Role.getRole(account, roleId); // may throw DBException
            if (role == null) {
                throw new DBException("Unable to read existing Role-ID '" + roleKey + "'");
            }
            return role;
        }

    }

    /* Create specified role.  throw DBException if role already exists */
    public static Role createNewRole(Account account, String roleID)
        throws DBException
    {
        if ((account != null) && !StringTools.isBlank(roleID)) {
            Role role = Role.getRole(account, roleID, true); // does not return null
            role.save();
            return role;
        } else {
            throw new DBNotFoundException("Invalid Account/RoleID specified");
        }
    }

    // ------------------------------------------------------------------------

    /* return list of all Roles owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getRolesForAccount(String acctId)
        throws DBException
    {
        return Role.getRolesForAccount(acctId, false);
    }

    /* return list of all Roles owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getRolesForAccount(String acctId, boolean inclSysRoles)
        throws DBException
    {

        /* invalid account */
        if (StringTools.isBlank(acctId)) {
            return new String[0];
        }

        /* select */
        // DBSelect: SELECT roleID FROM Role WHERE (accountID='acct') ORDER BY roleID
        DBSelect<Role> dsel = new DBSelect<Role>(Role.getFactory());
        dsel.setSelectedFields(Role.FLD_roleID);
        dsel.setOrderByFields(Role.FLD_roleID);
        DBWhere dwh = dsel.createDBWhere();
        dwh.append(dwh.EQ(Role.FLD_accountID, acctId));
        if (inclSysRoles && !StringTools.isBlank(RoleRecord.SYSTEM_ROLE_PREFIX) && 
            !AccountRecord.isSystemAdminAccountID(acctId)) {
            String sysAdminID = Account.getSystemAdminAccountID();
            if (!StringTools.isBlank(sysAdminID)) {
                dwh.append(dwh.OR_(
                    dwh.AND(
                        dwh.EQ(Role.FLD_accountID, sysAdminID),
                        dwh.STARTSWITH(Role.FLD_roleID, RoleRecord.SYSTEM_ROLE_PREFIX)
                    )
                ));
            }
        }
        dsel.setWhere(dwh.WHERE(dwh.toString()));

        /* select */
        return Role.getRoles(dsel);

    }
    
    /* return list of all Roles owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getRoles(DBSelect<Role> dsel)
        throws DBException
    {

        /* invalid selection */
        if (dsel == null) {
            return new String[0];
        }
        dsel.setSelectedFields(Role.FLD_roleID);

        /* read roles for account */
        java.util.List<String> roleList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String roleId = rs.getString(Role.FLD_roleID);
                roleList.add(roleId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account Role List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return roleList.toArray(new String[roleList.size()]);

    }

    // ------------------------------------------------------------------------
    
    /* return true if there are any users which reference the specified role */
    public static boolean hasUsers(Role role)
    {
        if (role != null) {
            String acctID = role.getAccountID();
            String roleID = role.getRoleID();
            try {
                return User.hasUserIDsForRole(acctID, roleID);
            } catch (DBException dbe) {
                Print.logException("Checking for users referencing role: " + acctID + "/" + roleID, dbe);
                return false;
            }
        } else {
            return false;
        }
    }

    /* return true if there are any users which reference the specified role */
    public static long getUserCount(Role role)
    {
        if (role != null) {
            String acctID = role.getAccountID();
            String roleID = role.getRoleID();
            try {
                return User.countUserIDsForRole(acctID, roleID);
            } catch (DBException dbe) {
                Print.logException("Checking for users referencing role: " + acctID + "/" + roleID, dbe);
                return -1L;
            }
        } else {
            return 0L;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "acct"  };
    private static final String ARG_ROLE[]      = new String[] { "role"              };
    private static final String ARG_CREATE[]    = new String[] { "create"            };
    private static final String ARG_EDIT[]      = new String[] { "edit"    , "ed"    };
    private static final String ARG_EDITALL[]   = new String[] { "editall" , "eda"   };
    private static final String ARG_DELETE[]    = new String[] { "delete"  , "purge" };
    private static final String ARG_LIST[]      = new String[] { "list"              };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Role.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns Role");
        Print.logInfo("  -role=<id>      Role ID to create/edit");
        Print.logInfo("  -create         Create a new Role");
        Print.logInfo("  -edit           Edit an existing (or newly created) Role");
        Print.logInfo("  -delete         Delete specified Role");
        Print.logInfo("  -list           List Roles for Account");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID     = RTConfig.getString(ARG_ACCOUNT, "");
        String roleID     = RTConfig.getString(ARG_ROLE , "");
        boolean listRoles = RTConfig.getBoolean(ARG_LIST, false);
        
        /* option count */
        int opts = 0;

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
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
            System.exit(99);
        }

        /* list */
        if (listRoles) {
            opts++;
            try {
                Print.logInfo("Account: " + acctID);
                String roleList[] = Role.getRolesForAccount(acctID,false);
                for (int i = 0; i < roleList.length; i++) {
                    Print.logInfo("  Role: " + roleList[i]);
                }
            } catch (DBException dbe) {
                Print.logException("Error listing Roles: " + acctID, dbe);
                System.exit(99);
            }
            System.exit(0);
        }
        
        // the following require a "-role" specification

        /* role-id specified? */
        if (StringTools.isBlank(roleID)) {
            Print.logError("Role-ID not specified.");
            usage();
        }

        /* role exists? */
        boolean roleExists = false;
        try {
            roleExists = Role.exists(acctID, roleID);
        } catch (DBException dbe) {
            Print.logError("Error determining if Role exists: " + acctID + "," + roleID);
            System.exit(99);
        }
        
        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !acctID.equals("") && !roleID.equals("")) {
            opts++;
            if (!roleExists) {
                Print.logWarn("Role does not exist: " + acctID + "/" + roleID);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                Role.Key roleKey = new Role.Key(acctID, roleID);
                roleKey.delete(true); // also delete dependencies
                Print.logInfo("Role deleted: " + acctID + "/" + roleID);
            } catch (DBException dbe) {
                Print.logError("Error deleting Role: " + acctID + "/" + roleID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (roleExists) {
                Print.logWarn("Role already exists: " + acctID + "/" + roleID);
            } else {
                try {
                    Role.createNewRole(acct, roleID);
                    Print.logInfo("Created Role-ID: " + acctID + "/" + roleID);
                } catch (DBException dbe) {
                    Print.logError("Error creating Role: " + acctID + "/" + roleID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!roleExists) {
                Print.logError("Role does not exist: " + acctID + "/" + roleID);
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    Role role = Role.getRole(acct, roleID, false); // may throw DBException
                    DBEdit editor = new DBEdit(role);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing Role: " + acctID + "/" + roleID);
                    dbe.printException();
                    System.exit(99);
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
