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
//  2008/02/04  Martin D. Flynn
//     -Initial release
//  2008/02/11  Martin D. Flynn
//     -Added key ID_DEVICE_NEW_DESCRIPTION
//  2008/07/21  Martin D. Flynn
//     -Added key ID_ACCOUNT
//  2008/08/24  Martin D. Flynn
//     -Added key ID_PING_DEVICE
//  2009/05/24  Martin D. Flynn
//     -Added key ID_USER
//  2011/10/03  Martin D. Flynn
//     -Added key ID_ADDRESS
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

public class AccountString
    extends AccountRecord<AccountString>
{

    // ------------------------------------------------------------------------

    public static final String ID_ACCOUNT                   = "account";     // "Account", "Company", ... 
    public static final String ID_USER                      = "user";        // "User", "Associate", ... 
    public static final String ID_DEVICE                    = "device";      // "Device", "Tractor", "Taxi", ... 
    public static final String ID_DEVICE_NEW_DESCRIPTION    = "deviceDesc";  // "New Device"
    public static final String ID_DEVICE_GROUP              = "deviceGroup"; // "Device Group", "Group", "Fleet", "Crowd", ...
    public static final String ID_ENTITY                    = "entity";      // "Entity", "Trailer", ...
    public static final String ID_PING_DEVICE               = "pingDevice";  // "Ping Device", "Locate {0}", "Locate Now", ...
    public static final String ID_ADDRESS                   = "address";     // "Address", "Landmark", ...

    public static final String ID_KEYS[] = new String[] {
        ID_ACCOUNT,
        ID_USER,
        ID_DEVICE,
        ID_DEVICE_NEW_DESCRIPTION,
        ID_DEVICE_GROUP,
        ID_ENTITY,
        ID_PING_DEVICE,
        ID_ADDRESS
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    private static final String _TABLE_NAME                 = "AccountString";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_stringID                 = "stringID";
    public static final String FLD_singularTitle            = "singularTitle";
    public static final String FLD_pluralTitle              = "pluralTitle";
    private static DBField FieldInfo[] = {
        // String fields:
        newField_accountID(true),
        new DBField(FLD_stringID        , String.class  , DBField.TYPE_ID()        , "String ID"        , "key=true editor=accountString"),
        new DBField(FLD_singularTitle   , String.class  , DBField.TYPE_STRING(64)  , "Singular Title"   , "edit=2"),
        new DBField(FLD_pluralTitle     , String.class  , DBField.TYPE_STRING(64)  , "Plural Title"     , "edit=2"),
        // Common fields
        newField_description(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends AccountKey<AccountString>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String strKey) {
            super.setFieldValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_stringID , ((strKey != null)? strKey.toLowerCase() : ""));
        }
        public DBFactory<AccountString> getFactory() {
            return AccountString.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<AccountString> factory = null;
    public static DBFactory<AccountString> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                AccountString.TABLE_NAME(), 
                AccountString.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                AccountString.class, 
                AccountString.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public AccountString()
    {
        super();
    }

    /* database record */
    public AccountString(AccountString.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(AccountString.class, loc);
        return i18n.getString("AccountString.description", 
            "This table defines " +
            "Account specific customized String key/values."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the String ID for this record */
    public String getStringID()
    {
        String v = (String)this.getFieldValue(FLD_stringID);
        return StringTools.trim(v);
    }
    
    /* set the String ID for this record */
    private void setStringID(String v)
    {
        this.setFieldValue(FLD_stringID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the singular title */
    public String getSingularTitle()
    {
        String v = (String)this.getFieldValue(FLD_singularTitle);
        return StringTools.trim(v);
    }
    
    /* return true if singular title is defined */
    public boolean hasSingularTitle()
    {
        return !this.getSingularTitle().equals("");
    }

    /* set the singular title */
    public void setSingularTitle(String v)
    {
        this.setFieldValue(FLD_singularTitle, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the plural title */
    public String getPluralTitle()
    {
        String v = (String)this.getFieldValue(FLD_pluralTitle);
        return StringTools.trim(v);
    }
    
    /* return true if plural title is defined */
    public boolean hasPluralTitle()
    {
        return !this.getPluralTitle().equals("");
    }

    /* set the plural title */
    public void setPluralTitle(String v)
    {
        this.setFieldValue(FLD_pluralTitle, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription("String");
        this.setSingularTitle("singular");
        this.setPluralTitle("plural");
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /* return the AccountID/StringID */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getStringID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String strID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (strID != null)) {
            AccountString.Key strKey = new AccountString.Key(acctID, strID);
            return strKey.exists();
        }
        return false;
    }
    
    // ------------------------------------------------------------------------
    
    /* update title string */
    public static void updateAccountString(Account account, String stringID, 
        String description, 
        String singular, String plural)
        throws DBException
    {

        /* valid account? */
        if (account == null) {
            throw new DBException("Account not specified.");
        }

        /* delete? */
        // delete if both singular/plural values are empty/null
        if (((singular == null) || singular.equals("")) &&
            ((plural   == null) || plural.equals("")  )   ) {
            String acctID = account.getAccountID();
            AccountString.Key key = new AccountString.Key(acctID, stringID);
            key.delete(true); // also delete dependencies (if any)
            return;
        }

        /* get/create AccountString */
        AccountString str = AccountString.getAccountString(account, stringID);
        if (str == null) {
            str = AccountString.getAccountString(account, stringID, true);
        }

        /* insert/update */
        str.setDescription(description);
        str.setSingularTitle(singular);
        str.setPluralTitle((plural != null)? plural : singular);
        str.save();

    }

    // ------------------------------------------------------------------------

    /* get string */
    public static AccountString getAccountString(Account account, String strID)
        throws DBException
    {
        if ((account != null) && (strID != null)) {
            String acctID = account.getAccountID();
            AccountString.Key key = new AccountString.Key(acctID, strID);
            if (key.exists()) {
                AccountString str = key.getDBRecord(true);
                str.setAccount(account);
                return str;
            } else {
                // AccountString does not exist
                return null;
            }
        } else {
            return null; // just say it doesn't exist
        }
    }

    /* get the singular/plural strings in an array that can be used directly by "i18n.getStr...(...)" */
    public static String[] getStringsArray(Account account, String strID, String dft[])
    {

        /* get values */
        try {
            AccountString str = AccountString.getAccountString(account, strID);
            if (str != null) {
                String s = str.getSingularTitle();
                String p = str.getPluralTitle();
                if ((dft != null) && (dft.length >= 2)) {
                    if (s.equals("")) { s = dft[0]; }
                    if (p.equals("")) { p = dft[1]; }
                }
                return new String[] { 
                    s,  // singular
                    p   // plural
                };
            }
        } catch (DBException dbe) {
            // ignore
        }

        /* return default */
        return dft; // not found

    }

    // ------------------------------------------------------------------------
    
    /* get account string */
    // Note: does NOT return null
    public static AccountString getAccountString(Account account, String strID, boolean create)
        throws DBException
    {
        
        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctID = account.getAccountID();
        
        /* string-id specified? */
        if ((strID == null) || strID.equals("")) {
            throw new DBNotFoundException("String-ID not specified for account: " + acctID);
        }

        /* get/create */
        AccountString str = null;
        AccountString.Key strKey = new AccountString.Key(acctID, strID);
        if (!strKey.exists()) {
            if (create) {
                str = strKey.getDBRecord();
                str.setAccount(account);
                str.setCreationDefaultValues();
                return str; // not yet saved!
            } else {
                throw new DBNotFoundException("String-ID does not exists: " + strKey);
            }
        } else
        if (create) {
            // we've been asked to create the AccountString, and it already exists
            throw new DBAlreadyExistsException("String-ID already exists '" + strKey + "'");
        } else {
            str = AccountString.getAccountString(account, strID);
            if (str == null) {
                throw new DBException("Unable to read existing String-ID: " + strKey);
            }
            return str;
        }
        
    }

    /* create string */
    public static AccountString createNewAccountString(Account account, String strID)
        throws DBException
    {
        if ((account != null) && (strID != null) && !strID.equals("")) {
            AccountString str = AccountString.getAccountString(account, strID, true); // does not return null
            str.save();
            return str;
        } else {
            throw new DBException("Invalid Account/StringID specified");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below
    
    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "acct"  };
    private static final String ARG_STRING[]    = new String[] { "string"  , "str"   };
    private static final String ARG_CREATE[]    = new String[] { "create"            };
    private static final String ARG_EDIT[]      = new String[] { "edit"    , "ed"    };
    private static final String ARG_DELETE[]    = new String[] { "delete"            };

    private static String _fmtStrID(String acctID, String strID)
    {
        return acctID + "/" + strID;
    }

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + AccountString.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns AccountString");
        Print.logInfo("  -string=<id>    String ID to create/edit");
        Print.logInfo("  -create         Create a new AccountString");
        Print.logInfo("  -edit           Edit an existing (or newly created) AccountString");
        Print.logInfo("  -delete         Delete specified AccountString");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String strID   = RTConfig.getString(ARG_STRING , "");

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may throw DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* string-id specified? */
        if ((strID == null) || strID.equals("")) {
            Print.logError("String-ID not specified.");
            usage();
        }

        /* string exists? */
        boolean stringExists = false;
        try {
            stringExists = AccountString.exists(acctID, strID);
        } catch (DBException dbe) {
            Print.logError("Error determining if AccountString exists: " + _fmtStrID(acctID,strID));
            System.exit(99);
        }

        /* option count */
        int opts = 0;
        
        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !acctID.equals("") && !strID.equals("")) {
            opts++;
            if (!stringExists) {
                Print.logWarn("AccountString does not exist: " + _fmtStrID(acctID,strID));
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                AccountString.Key strKey = new AccountString.Key(acctID, strID);
                strKey.delete(true); // also delete dependencies
                Print.logInfo("AccountString deleted: " + _fmtStrID(acctID,strID));
                stringExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting AccountString: " + _fmtStrID(acctID,strID));
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (stringExists) {
                Print.logWarn("AccountString already exists: " + _fmtStrID(acctID,strID));
            } else {
                try {
                    AccountString.createNewAccountString(acct, strID);
                    Print.logInfo("Created AccountString: " + _fmtStrID(acctID,strID));
                    stringExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating AccountString: " + _fmtStrID(acctID,strID));
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false)) {
            opts++;
            if (!stringExists) {
                Print.logError("AccountString does not exist: " + _fmtStrID(acctID,strID));
            } else {
                try {
                    AccountString str = AccountString.getAccountString(acct, strID, false); // may throw DBException
                    DBEdit editor = new DBEdit(str);
                    editor.edit(true); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing AccountString: " + _fmtStrID(acctID,strID));
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
