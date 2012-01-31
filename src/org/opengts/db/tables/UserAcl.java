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
//  2007/03/30  Martin D. Flynn
//     -Initial release
//  2007/06/30  Martin D. Flynn
//     -Added convenience method for User ACLs
//     -Added ability to set ACL levels from command line
//  2008/02/07  Martin D. Flynn
//     -Removed 'GetInternalAclEntries' (all ACLs should now be defined 'private.xml')
//  2008/05/20  Martin D. Flynn
//     -Integrated 'AccessLevel' enumerated type.
//  2008/10/16  Martin D. Flynn
//     -Simplified ACL usage.
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;

public class UserAcl
    extends UserRecord<UserAcl>
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "UserAcl";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_aclID                = "aclID";
    public static final String FLD_accessLevel          = "accessLevel";
    private static DBField FieldInfo[] = {
        // UserAcl fields
        newField_accountID(true),
        newField_userID(true),
        new DBField(FLD_aclID           , String.class  , DBField.TYPE_STRING(64)  , "ACL ID"       , "key=true"),
        new DBField(FLD_accessLevel     , Integer.TYPE  , DBField.TYPE_UINT16      , "Access Level" , "edit=2 enum=AclEntry$AccessLevel"),
        // Common fields
        newField_description(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends UserKey<UserAcl>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String userId, String aclId) {
            super.setFieldValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_userID   , ((userId != null)? userId.toLowerCase() : ""));
            super.setFieldValue(FLD_aclID    , ((aclId  != null)? aclId .toLowerCase() : ""));
        }
        public DBFactory<UserAcl> getFactory() {
            return UserAcl.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<UserAcl> factory = null;
    public static DBFactory<UserAcl> getFactory()
    {
        if (factory == null) {
            EnumTools.registerEnumClass(AccessLevel.class);
            factory = DBFactory.createDBFactory(
                UserAcl.TABLE_NAME(), 
                UserAcl.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                UserAcl.class, 
                UserAcl.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(User.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public UserAcl()
    {
        super();
    }

    /* database record */
    public UserAcl(UserAcl.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(UserAcl.class, loc);
        return i18n.getString("UserAcl.description", 
            "This table defines " + 
            "User specific Access Control permissions."
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
        return this.getAccountID() + "/" + this.getUserID() + "/" + this.getAclID();
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
    // The following is an optimization for holding the User record while
    // processing this UserAcl.  Use with caution.

    // ------------------------------------------------------------------------

    /* return true if the specified user ACL exists */
    public static boolean exists(String acctID, String userID, String aclID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (userID != null) && (aclID != null)) {
            UserAcl.Key aclKey = new UserAcl.Key(acctID, userID, aclID);
            return aclKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /* return User access level */
    public static AccessLevel getAccessLevel(UserAcl ua)
    {
        return (ua != null)? 
            EnumTools.getValueOf(AccessLevel.class,ua.getAccessLevel()) : 
            EnumTools.getDefault(AccessLevel.class);
    }

    /* return User access level */
    public static AccessLevel getAccessLevel(User user, String aclId, AccessLevel dftAccess)
    {

        /* sysadmin user (no ACL restrictions) */
        if (User.isAdminUser(user)) { // returns true if "(user == null)"
            return AccessLevel.ALL; // 'admin' has all rights
        }

        /* no user (should not occur, but handle anyway) */
        if (user == null) {
            // if we are here, then a 'null' user is not a SysAdmin user, return NONE
            return AccessLevel.NONE;
        }

        /* set default access level */
        int maxAccessLevel = AccessLevel.ALL.getIntValue(); // user.getMaxAccessLevel();
        if ((dftAccess != null) && (dftAccess.getIntValue() > maxAccessLevel)) {
            // limit to max access-level
            dftAccess = AclEntry.getAccessLevel(maxAccessLevel);
        }

        /* no ACL specified? */
        if (StringTools.isBlank(aclId)) {
            return dftAccess;
        }

        /* get ACL for user */
        try {
            UserAcl userAcl = UserAcl.getUserAcl(user, aclId); // may throw DBException
            if (userAcl != null) {
                AccessLevel accLvl = UserAcl.getAccessLevel(userAcl); //not null
                if (accLvl.getIntValue() > maxAccessLevel) {
                    return AclEntry.getAccessLevel(maxAccessLevel);
                } else {
                    return accLvl;
                }
            } else {
                return dftAccess;
            }
        } catch (DBException dbe) {
            // error occurred (unlikely)
            return AccessLevel.NONE;
        }
        
    }

    /* set User access level */
    public static void setAccessLevel(User user, String aclId, AccessLevel level)
        throws DBException
    {

        /* user specified? */
        if (user == null) {
            throw new DBException("User not specified.");
        }
        String acctId = user.getAccountID();
        String userId = user.getUserID();

        /* acl-id specified? */
        if (StringTools.isBlank(aclId)) {
            throw new DBException("Acl-ID not specified.");
        }

        /* get/create user */
        UserAcl userAcl = null;
        UserAcl.Key aclKey = new UserAcl.Key(acctId, userId, aclId);
        if (aclKey.exists()) { // may throw DBException
            userAcl = UserAcl.getUserAcl(user, aclId); // may throw DBException
        } else {
            userAcl = aclKey.getDBRecord();
            userAcl.setUser(user);
        }
        
        /* set access level */
        int levelInt = (level != null)? level.getIntValue() : AccessLevel.NONE.getIntValue();
        userAcl.setAccessLevel(levelInt);
        
        /* save */
        userAcl.save(); // may throw DBException

    }

    /* set User access level */
    public static boolean deleteAccessLevel(User user, String aclId)
        throws DBException
    {

        /* user specified? */
        if (user == null) {
            return false; // quietly ignore
        }
        String acctId = user.getAccountID();
        String userId = user.getUserID();

        /* acl-id specified? */
        if (StringTools.isBlank(aclId)) {
            return false; // quietly ignore
        }

        /* already deleted? */
        boolean aclExists = UserAcl.exists(acctId, userId, aclId);
        if (!aclExists) {
            return false;
        }

        /* delete */
        UserAcl.Key aclKey = new UserAcl.Key(acctId, userId, aclId);
        aclKey.delete(true); // also delete dependencies
        return true;

    }

    // ------------------------------------------------------------------------

    /* Return specified user */
    public static UserAcl getUserAcl(User user, String aclId)
        throws DBException
    {
        if ((user != null) && (aclId != null)) {
            UserAcl.Key aclKey = new UserAcl.Key(user.getAccountID(), user.getUserID(), aclId);
            if (aclKey.exists()) {
                UserAcl userAcl = aclKey.getDBRecord(true);
                userAcl.setUser(user);
                return userAcl;
            } else
            if (user.isAdminUser()) {
                //UserAcl userAcl = (UserAcl)aclKey.getDBRecord(false);
                //userAcl.setUser(user);
                //userAcl.setAccessLevel(AccessLevel.ALL.getIntValue());
                //return userAcl;
                return null;
            } else {
                return null;
            }
        } else {
            throw new DBException("User or AclID is null");
        }
    }

    /* Return specified user ACL, create if specified */
    public static UserAcl getUserAcl(User user, String aclId, boolean create)
        throws DBException
    {
        // does not return null

        /* user specified? */
        if (user == null) {
            throw new DBNotFoundException("User not specified.");
        }
        String acctId = user.getAccountID();
        String userId = user.getUserID();

        /* acl-id specified? */
        if (StringTools.isBlank(aclId)) {
            throw new DBNotFoundException("Acl-ID not specified.");
        }

        /* get/create user */
        UserAcl userAcl = null;
        UserAcl.Key aclKey = new UserAcl.Key(acctId, userId, aclId);
        if (!aclKey.exists()) { // may throw DBException
            if (create) {
                userAcl = aclKey.getDBRecord();
                userAcl.setUser(user);
                userAcl.setCreationDefaultValues();
                return userAcl; // not yet saved!
            } else {
                throw new DBNotFoundException("Acl-ID does not exists '" + aclKey + "'");
            }
        } else
        if (create) {
            // we've been asked to create the Acl, and it already exists
            throw new DBAlreadyExistsException("Acl-ID already exists '" + aclKey + "'");
        } else {
            userAcl = UserAcl.getUserAcl(user, aclId); // may throw DBException
            if (userAcl == null) {
                throw new DBException("Unable to read existing User-ID '" + aclKey + "'");
            }
            return userAcl;
        }

    }

    /* Create specified user.  Return null if acl already exists */
    public static UserAcl createNewUserAcl(User user, String aclID)
        throws DBException
    {
        UserAcl userAcl = UserAcl.getUserAcl(user, aclID, true);
        if (userAcl != null) {
            userAcl.save();
        }
        return userAcl;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct" };
    private static final String ARG_USER[]      = new String[] { "user"   , "usr"  };
    private static final String ARG_LIST[]      = new String[] { "list"            };
    private static final String ARG_ACL[]       = new String[] { "acl"             };
    private static final String ARG_SET[]       = new String[] { "set"             };
    private static final String ARG_CREATE[]    = new String[] { "create" , "cr"   };
    private static final String ARG_EDIT[]      = new String[] { "edit"   , "ed"   };
    private static final String ARG_DELETE[]    = new String[] { "delete" , "purge"};

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + UserAcl.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns User");
        Print.logInfo("  -user=<id>      User ID which owns UserAcl");
        Print.logInfo("  -list           List Acls for User");
        Print.logInfo("  -acl=<id>       User ID to create/edit");
        Print.logInfo("  -set=<val>      UserAcl value (create if necessary)");
        Print.logInfo("  -create         Create a new UserAcl");
        Print.logInfo("  -edit           Edit an existing (or newly created) UserAcl");
        Print.logInfo("  -delete         Delete specified UserAcl");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String userID  = RTConfig.getString(ARG_USER   , "");
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

        /* user-id specified? */
        if ((userID == null) || userID.equals("")) {
            Print.logError("User-ID not specified.");
            usage();
        }

        /* get user */
        User user = null;
        try {
            user = User.getUser(acct, userID); // may return DBException
            if (user == null) {
                Print.logError("User-ID does not exist: " + acctID + "/" + userID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading User: " + acctID + "/" + userID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* userAcl exists? */
        boolean aclExists = false;
        if ((aclID != null) && !aclID.equals("")) {
            try {
                aclExists = UserAcl.exists(acctID, userID, aclID);
            } catch (DBException dbe) {
                Print.logError("Error determining if UserAcl exists: " + acctID + "/" + userID + "/" + aclID);
                System.exit(99);
            }
        }
        
        /* option count */
        int opts = 0;
        
        /* list */
        if (RTConfig.getBoolean(ARG_LIST, false)) {
            opts++;
            try {
                String aclList[] = user.getAclsForUser();
                for (int i = 0; i < aclList.length; i++) {
                    AccessLevel level = UserAcl.getAccessLevel(user, aclList[i], AccessLevel.NONE);
                    Print.sysPrintln("  " + aclList[i] + " ==> " + level);
                }
            } catch (DBException dbe) {
                Print.logError("Error getting Acl list: " + dbe);
                System.exit(99);
            }
            System.exit(0);
        }
        
        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !acctID.equals("") && !userID.equals("")) {
            opts++;
            if (!aclExists) {
                Print.logWarn("UserAcl does not exist: " + acctID + "/" + userID + "/" + aclID);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                UserAcl.Key aclKey = new UserAcl.Key(acctID, userID, aclID);
                aclKey.delete(true); // also delete dependencies
                Print.logInfo("UserAcl deleted: " + acctID + "/" + userID + "/" + aclID);
            } catch (DBException dbe) {
                Print.logError("Error deleting UserAcl: " + acctID + "/" + userID + "/" + aclID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (aclExists) {
                Print.logWarn("UserAcl already exists: " + acctID + "/" + userID + "/" + aclID);
            } else {
                try {
                    UserAcl.createNewUserAcl(user, aclID);
                    Print.logInfo("Created UserAcl: " + acctID + "/" + userID + "/" + aclID);
                    aclExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating UserAcl: " + acctID + "/" + userID + "/" + aclID);
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
                UserAcl.setAccessLevel(user, aclID, aclLevel);
                Print.logInfo("Set UserAcl '" + acctID + "/" + userID + "/" + aclID + "' to level " + aclLevel);
            } catch (DBException dbe) {
                Print.logError("Error setting UserAcl: "+ acctID + "/" + userID + "/" + aclID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT, false)) {
            opts++;
            if (!aclExists) {
                Print.logError("UserAcl does not exist: " + acctID + "/" + userID + "/" + aclID);
            } else {
                try {
                    UserAcl userAcl = UserAcl.getUserAcl(user, aclID, false); // may throw DBException
                    DBEdit editor = new DBEdit(userAcl);
                    editor.edit(); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing UserAcl: "+ acctID + "/" + userID + "/" + aclID);
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
