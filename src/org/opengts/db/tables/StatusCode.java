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
// Description: 
//  This table allow customizing status code descriptions displayed on reports.
//  Using this table, status code descriptions are resolved in the following order:
//    1) Use description matching key Account/*/StatusCode
//    2) Use description matching key Account/Device/StatusCode
//    3) Use default description for status code.
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/25  Martin D. Flynn
//     -Initial release
//  2007/04/01  Martin D. Flynn
//     -Added 'deviceID' column.
//  2007/11/28  Martin D. Flynn
//     -Added command-line administrative tools.
//  2008/04/11  Martin D. Flynn
//     -Added column "FLD_iconSelector".
//  2008/09/19  Martin D. Flynn
//     -Removed check for invalid status codes when using the command-line admin.
//  2011/06/16  Martin D. Flynn
//     -Added FLD_foregroundColor, FLD_backgroundColor
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

public class StatusCode
    extends DeviceRecord<StatusCode>
    implements StatusCodeProvider
{
    
    // ------------------------------------------------------------------------

    public static final String ALL_DEVICES              = "*";
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "StatusCode";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_statusCode           = "statusCode";
    public static final String FLD_statusName           = "statusName";
    public static final String FLD_foregroundColor      = "foregroundColor";
    public static final String FLD_backgroundColor      = "backgroundColor";
    public static final String FLD_iconSelector         = "iconSelector";
    public static final String FLD_iconName             = "iconName";
    private static DBField FieldInfo[] = {
        // StatusCode fields
        newField_accountID(true),
        newField_deviceID(true),
        new DBField(FLD_statusCode      , Integer.TYPE  , DBField.TYPE_UINT32      , "Status Code"     , "key=true editor=statusCode format=X2"),
        new DBField(FLD_statusName      , String.class  , DBField.TYPE_STRING(18)  , "Status Name"     , "edit=2"),
        new DBField(FLD_foregroundColor , String.class  , DBField.TYPE_STRING(10)  , "Foreground Color", "edit=2"),
        new DBField(FLD_backgroundColor , String.class  , DBField.TYPE_STRING(10)  , "Background Color", "edit=2"),
        new DBField(FLD_iconSelector    , String.class  , DBField.TYPE_STRING(128) , "Icon Selector"   , "edit=2 editor=ruleSelector"),
        new DBField(FLD_iconName        , String.class  , DBField.TYPE_STRING(24)  , "Icon Name"       , "edit=2"),
        // Common fields
        newField_description(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DeviceKey<StatusCode>
    {
        public Key() {
            super();
        }
        public Key(String accountId, int statusCode) {
            this(accountId, ALL_DEVICES, statusCode);
        }
        public Key(String accountId, String deviceId, int statusCode) {
            super.setFieldValue(FLD_accountID , ((accountId != null)? accountId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((deviceId  != null)? deviceId.toLowerCase()  : ""));
            super.setFieldValue(FLD_statusCode, statusCode);
        }
        public DBFactory<StatusCode> getFactory() {
            return StatusCode.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<StatusCode> factory = null;
    public static DBFactory<StatusCode> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                StatusCode.TABLE_NAME(), 
                StatusCode.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                StatusCode.class, 
                StatusCode.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
            factory.setFieldDefaultValue(FLD_deviceID, ALL_DEVICES);  
        }
        return factory;
    }

    /* Bean instance */
    public StatusCode()
    {
        super();
    }

    /* database record */
    public StatusCode(StatusCode.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(StatusCode.class, loc);
        return i18n.getString("StatusCode.description", 
            "This table defines " +
            "Device specific StatusCode descriptions."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    public int getStatusCode()
    {
        Integer v = (Integer)this.getFieldValue(FLD_statusCode);
        return (v != null)? v.intValue() : 0;
    }
    
    public void setStatusCode(int v)
    {
        this.setFieldValue(FLD_statusCode, v);
    }

    // ------------------------------------------------------------------------

    public String getStatusName()
    {
        String v = (String)this.getFieldValue(FLD_statusName);
        return StringTools.trim(v);
    }
    
    public void setStatusName(String v)
    {
        this.setFieldValue(FLD_statusName, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getForegroundColor()
    {
        String v = (String)this.getFieldValue(FLD_foregroundColor);
        return StringTools.trim(v);
    }

    public void setForegroundColor(String v)
    {
        this.setFieldValue(FLD_foregroundColor, StringTools.trim(v));
    }

    public void setForegroundColor(ColorTools.RGB rgb)
    {
        this.setForegroundColor((rgb != null)? rgb.toString(true) : (String)null);
    }

    // ------------------------------------------------------------------------

    public String getBackgroundColor()
    {
        String v = (String)this.getFieldValue(FLD_backgroundColor);
        return StringTools.trim(v);
    }
    
    public void setBackgroundColor(String v)
    {
        this.setFieldValue(FLD_backgroundColor, StringTools.trim(v));
    }

    public void setBackgroundColor(ColorTools.RGB rgb)
    {
        this.setBackgroundColor((rgb != null)? rgb.toString(true) : (String)null);
    }

    // ------------------------------------------------------------------------

    public String getIconSelector()
    {
        String v = (String)this.getFieldValue(FLD_iconSelector);
        return StringTools.trim(v);
    }
    
    public void setIconSelector(String v)
    {
        this.setFieldValue(FLD_iconSelector, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getIconName()
    {
        String v = (String)this.getFieldValue(FLD_iconName);
        return StringTools.trim(v);
    }
    
    public void setIconName(String v)
    {
        this.setFieldValue(FLD_iconName, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    // StatusCodeProvider interface
    public String getDescription(Locale locale)
    {
        return this.getDescription();
    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getAccountID());
        sb.append("/");
        sb.append(this.getDeviceID());
        sb.append(" ");
        sb.append(this.getStatusCode());
        sb.append("[");
        sb.append(this.getDescription());
        sb.append("]");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        BasicPrivateLabel privateLabel = Account.getPrivateLabel(this.getAccount());
        StatusCodes.Code code = StatusCodes.GetCode(this.getStatusCode(), privateLabel);
        this.setStatusName( (code != null)? code.getName()            : "");
        this.setDescription((code != null)? code.getDescription(null) : "");
        this.setIconSelector("");
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return StatusCode */
    public static StatusCode findStatusCode(String accountID, String deviceID, int statusCode)
    {

        /* check account status codes */
        if (!StringTools.isBlank(accountID)) {

            // first, try account/device
            if (!StringTools.isBlank(deviceID)) {
                try {
                    StatusCode.Key codeKey = new StatusCode.Key(accountID, deviceID, statusCode);
                    if (codeKey.exists()) { // may throw DBException
                        StatusCode code = codeKey.getDBRecord(true);
                        if (code != null) { // should not be null
                            return code;
                        }
                    }
                } catch (DBException dbe) {
                    // ignore error
                }
            }

            // next, try just the account
            try {
                StatusCode.Key codeKey = new StatusCode.Key(accountID, statusCode);
                if (codeKey.exists()) { // may throw DBException
                    StatusCode code = codeKey.getDBRecord(true);
                    if (code != null) { // should not be null
                        return code;
                    }
                }
            } catch (DBException dbe) {
                // ignore error
            }

        }

        /* check global status codes */
        String sysAdmin = AccountRecord.getSystemAdminAccountID();
        if (!StringTools.isBlank(sysAdmin)) {
            try {
                StatusCode.Key codeKey = new StatusCode.Key(sysAdmin, statusCode);
                if (codeKey.exists()) { // may throw DBException
                    StatusCode code = codeKey.getDBRecord(true);
                    if (code != null) { // should not be null
                        return code;
                    }
                }
            } catch (DBException dbe) {
                // ignore error
            }
        }

        /* icon selector not found */
        return null;

    }

    // ------------------------------------------------------------------------

    /* Return status code attributes */
    public static StatusCodeProvider getStatusCodeProvider(Device device, int statusCode, 
        BasicPrivateLabel bpl,
        StatusCodeProvider dftSCP)
    {

        /* device code */
        if (device != null) {
            StatusCode code = device.getStatusCode(statusCode);
            if (code != null) {
                return code;
            }
        }

        /* default */
        if (dftSCP != null) {
            return dftSCP;
        } else {
            return StatusCodes.GetStatusCodeProvider(statusCode, bpl);
        }

    }

    /* Return status code description (used by RuleInfo, RequestProperties) */
    public static StatusCodeProvider getStatusCodeProvider(String accountID, int statusCode, 
        BasicPrivateLabel bpl, 
        StatusCodeProvider dftSCP)
    {

        /* custom code (record) */
        StatusCode code = StatusCode.findStatusCode(accountID, null, statusCode);
        if (code != null) {
            return code;
        }

        /* default */
        if (dftSCP != null) {
            return dftSCP;
        } else {
            return StatusCodes.GetStatusCodeProvider(statusCode, bpl);
        }

    }

    // ------------------------------------------------------------------------

    /* Return status code description */
    public static String getDescription(Device device, int statusCode, 
        BasicPrivateLabel bpl, 
        String dftDesc)
    {

        /* device code */
        if (device != null) {
            StatusCode code = device.getStatusCode(statusCode);
            if (code != null) {
                return code.getDescription();
            }
        }

        /* default */
        if (!StringTools.isBlank(dftDesc)) {
            return dftDesc;
        } else {
            return StatusCodes.GetDescription(statusCode, bpl);
        }

    }

    /* Return status code description (used by RuleInfo, RequestProperties) */
    public static String getDescription(String accountID, int statusCode, 
        BasicPrivateLabel bpl, 
        String dftDesc)
    {

        /* custom code (record) */
        StatusCode code = StatusCode.findStatusCode(accountID, null, statusCode);
        if (code != null) {
            return code.getDescription();
        }

        /* default */
        if (!StringTools.isBlank(dftDesc)) {
            return dftDesc;
        } else {
            return StatusCodes.GetDescription(statusCode, bpl);
        }

    }

    // ------------------------------------------------------------------------

    /* Return status code foreground color */
    public static String getForegroundColor(Device device, int statusCode, 
        BasicPrivateLabel bpl, 
        String dftColor)
    {

        /* device code */
        if (device != null) {
            StatusCode code = device.getStatusCode(statusCode);
            if (code != null) {
                return code.getForegroundColor();
            }
        }

        /* default */
        if (!StringTools.isBlank(dftColor)) {
            return dftColor;
        } else {
            return null; // inherited CSS
        }

    }

    /* Return status code foreground color */
    public static String getForegroundColor(String accountID, int statusCode, 
        BasicPrivateLabel bpl, 
        String dftColor)
    {

        /* custom code (record) */
        StatusCode code = StatusCode.findStatusCode(accountID, null, statusCode);
        if (code != null) {
            return code.getForegroundColor();
        }

        /* default */
        if (!StringTools.isBlank(dftColor)) {
            return dftColor;
        } else {
            return null; // inherited CSS
        }

    }

    // ------------------------------------------------------------------------

    /* Return status code background color */
    public static String getBackgroundColor(Device device, int statusCode, 
        BasicPrivateLabel bpl, 
        String dftColor)
    {

        /* device code */
        if (device != null) {
            StatusCode code = device.getStatusCode(statusCode);
            if (code != null) {
                return code.getBackgroundColor();
            }
        }

        /* default */
        if (!StringTools.isBlank(dftColor)) {
            return dftColor;
        } else {
            return null; // inherited CSS
        }

    }

    /* Return status code background color */
    public static String getBackgroundColor(String accountID, int statusCode, 
        BasicPrivateLabel bpl, 
        String dftColor)
    {

        /* custom code (record) */
        StatusCode code = StatusCode.findStatusCode(accountID, null, statusCode);
        if (code != null) {
            return code.getBackgroundColor();
        }

        /* default */
        if (!StringTools.isBlank(dftColor)) {
            return dftColor;
        } else {
            return null; // inherited CSS
        }

    }

    // ------------------------------------------------------------------------

    /* return icon selector */
    public static String getIconSelector(Device device, int statusCode, 
        BasicPrivateLabel bpl)
    {

        /* device code */
        if (device != null) {
            StatusCode code = device.getStatusCode(statusCode);
            if (code != null) {
                return code.getIconSelector();
            }
        }

        /* default */
        return "";

    }

    // ------------------------------------------------------------------------

    /* return icon name */
    public static String getIconName(Device device, int statusCode, 
        BasicPrivateLabel bpl)
    {

        /* device code */
        if (device != null) {
            StatusCode code = device.getStatusCode(statusCode);
            if (code != null) {
                return code.getIconName();
            }
        }

        /* default */
        return StatusCodes.GetIconName(statusCode, bpl);

    }

    // ------------------------------------------------------------------------

    /* return true if StatusCode exists */
    public static boolean exists(String accountID, String deviceID, int code)
        throws DBException // if error occurs while testing existance
    {
        if ((accountID != null) && (deviceID != null)) {
            StatusCode.Key scKey = new StatusCode.Key(accountID, deviceID, code);
            return scKey.exists();
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /* Return specified StatusCode (or null if non-existant) */
    public static StatusCode getStatusCode(String accountID, String deviceID, int code)
        throws DBException
    {
        return StatusCode._getStatusCode(accountID, null, deviceID, code, false);
    }

    /* Return specified StatusCode (or null if non-existant) */
    public static StatusCode getStatusCode(Account account, String deviceID, int code)
        throws DBException
    {
        return StatusCode._getStatusCode(null, account, deviceID, code, false);
    }

    /* Return specified StatusCode, create if specified */
    public static StatusCode getStatusCode(Account account, String deviceID, int code, boolean createOK)
        throws DBException
    {
        return StatusCode._getStatusCode(null, account, deviceID, code, createOK);
    }
    
    /* Return specified StatusCode, create if specified */
    private static StatusCode _getStatusCode(String accountID, Account account, String deviceID, int code, boolean createOK)
        throws DBException
    {
        // does not return null if 'createOK' is true

        /* account-id specified? */
        if (StringTools.isBlank(accountID)) {
            if (account == null) {
                throw new DBException("Account not specified.");
            } else {
                accountID = account.getAccountID();
            }
        } else
        if ((account != null) && !account.getAccountID().equals(accountID)) {
            throw new DBException("Account does not match specified AccountID.");
        }

        /* device-id specified? */
        if (StringTools.isBlank(deviceID)) {
            //throw new DBException("Device-ID not specified.");
            deviceID = ALL_DEVICES;
        }

        /* get/create entity */
        StatusCode.Key scKey = new StatusCode.Key(accountID, deviceID, code);
        if (scKey.exists()) { // may throw DBException
            StatusCode sc = scKey.getDBRecord(true);
            if (account != null) {
                sc.setAccount(account);
            }
            return sc;
        } else
        if (createOK) {
            StatusCode sc = scKey.getDBRecord();
            if (account != null) {
                sc.setAccount(account);
            }
            sc.setCreationDefaultValues();
            return sc; // not yet saved!
        } else {
            // record doesn't exist, and caller doesn't want us to create it
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /* create a new StatusCode */
    public static StatusCode createNewStatusCode(Account account, String deviceID, int code)
        throws DBException
    {
        
        /* invalid account */
        if (account == null) {
            throw new DBException("Invalid/Null Account specified");
        }
        
        /* invalid code */
        if ((code < 0) || (code > 0xFFFF)) {
            throw new DBException("Invalid StatusCode specified");
        }
        
        /* default to 'ALL' devices */
        if (StringTools.isBlank(deviceID)) {
            deviceID = ALL_DEVICES;
        }
        
        /* create status code */
        StatusCode sc = StatusCode.getStatusCode(account, deviceID, code, true); // does not return null
        sc.save();
        return sc;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return status codes for account/device */
    public static int[] getStatusCodes(String accountID, String deviceID)
        throws DBException
    {

        /* account-id specified? */
        if (StringTools.isBlank(accountID)) {
            return new int[0];
        }

        /* device-id specified? */
        if (StringTools.isBlank(deviceID)) {
            deviceID = ALL_DEVICES;
        }

        /* select */
        // DBSelect: SELECT statucCode FROM StatusCode WHERE (accountID='acct') AND (deviceID='*') ORDER BY statucCode
        DBSelect<StatusCode> dsel = new DBSelect<StatusCode>(StatusCode.getFactory());
        dsel.setSelectedFields(StatusCode.FLD_statusCode);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(StatusCode.FLD_accountID,accountID),
                dwh.EQ(StatusCode.FLD_deviceID ,deviceID)
            )
        ));
        dsel.setOrderByFields(StatusCode.FLD_statusCode);

        /* get list */
        java.util.List<Integer> codeList = new Vector<Integer>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                int code = rs.getInt(StatusCode.FLD_statusCode);
                codeList.add(new Integer(code));
            }
        } catch (SQLException sqe) {
            throw new DBException("Getting StatusCode List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return array of status codes */
        int codeListInt[] = new int[codeList.size()];
        for (int i = 0; i < codeListInt.length; i++) {
            codeListInt[i] = codeList.get(i).intValue();
        }
        return codeListInt;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below
    
    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "acct"  };
    private static final String ARG_DEVICE[]    = new String[] { "device"  , "dev"   };
    private static final String ARG_CODE[]      = new String[] { "status"  , "code"  , "ecode" };
    private static final String ARG_ECODE[]     = new String[] { "ecode"             };
    private static final String ARG_DELETE[]    = new String[] { "delete"            };
    private static final String ARG_CREATE[]    = new String[] { "create"            };
    private static final String ARG_EDIT[]      = new String[] { "edit"    , "ed"    };
    private static final String ARG_LIST[]      = new String[] { "list"              };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + StatusCode.class.getName() + " {options}");
        Print.logInfo("Options:");
        Print.logInfo("  -account=<id>   Account ID owning StatusCode");
        Print.logInfo("  -device=<id>    Device ID owning StatusCode (use '/' for ALL)");
        Print.logInfo("  -code=<id>      StatusCode to create/delete/edit");
        Print.logInfo("  -create         Create a new StatusCode");
        Print.logInfo("  -edit           To edit an existing StatusCode");
        Print.logInfo("  -delete         Delete specified StatusCode");
        System.exit(1);
    }
    
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        String  accountID  = RTConfig.getString(ARG_ACCOUNT, "");
        String  deviceID   = RTConfig.getString(ARG_DEVICE , "");
        int     statusCode = RTConfig.getInt(ARG_CODE , 0);
        boolean anyCode    = true; // RTConfig.hasProperty(ARG_ECODE);

        /* account-id specified? */
        if (StringTools.isBlank(accountID)) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(accountID); // may throw DBException
            if (account == null) {
                Print.logError("Account-ID does not exist: " + accountID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + accountID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* device-id specified? */
        if (StringTools.isBlank(deviceID) || deviceID.startsWith("/")) {
            deviceID = ALL_DEVICES;
        }

        /* check device existance */
        if (!deviceID.equals(ALL_DEVICES)) {
            try {
                Device device = Device.getDevice(account, deviceID); // may throw DBException
                if (device == null) {
                    Print.logError("Device-ID does not exist: " + accountID + " / " + deviceID);
                    usage();
                }
            } catch (DBException dbe) {
                Print.logException("Error loading Device: " + accountID + " / " + deviceID, dbe);
                System.exit(99);
            }
        }

        /* status-code specified? */
        if ((statusCode > 0) && !anyCode && !StatusCodes.IsValid(statusCode,account.getPrivateLabel())) {
            Print.logError("Invalid Status Code specified.");
            usage();
        }

        /* statusCode specified? */
        if (statusCode <= 0) {
            Print.logError("StatusCode not specified.");
            usage();
        }

        /* statusCode exists? */
        boolean statusCodeExists = false;
        try {
            statusCodeExists = StatusCode.exists(accountID, deviceID, statusCode);
        } catch (DBException dbe) {
            Print.logError("Error determining if StatusCode exists: " + accountID + "/" + deviceID + "/" + statusCode);
            System.exit(99);
        }

        /* option count */
        int opts = 0;

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE,false)) {
            opts++;
            if (!statusCodeExists) {
                Print.logWarn("StatusCode does not exist: " + accountID + "/" + deviceID + "/" + statusCode);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                StatusCode.Key scKey = new StatusCode.Key(accountID, deviceID, statusCode);
                scKey.delete(true); // also delete dependencies (if any)
                Print.logInfo("StatusCode deleted: " + accountID + "/" + deviceID + "/" + statusCode);
                statusCodeExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting StatusCode: " + accountID + "/" + deviceID + "/" + statusCode);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (statusCodeExists) {
                Print.logWarn("StatusCode already exists: " + accountID + "/" + deviceID + "/" + statusCode);
            } else {
                try {
                    StatusCode.createNewStatusCode(account, deviceID, statusCode);
                    Print.logInfo("Created StatusCode: " + accountID + "/" + deviceID + "/" + statusCode);
                    statusCodeExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating StatusCode: " + accountID + "/" + deviceID + "/" + statusCode);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT, false)) {
            opts++;
            if (!statusCodeExists) {
                Print.logError("StatusCode does not exist: " + accountID + "/" + deviceID + "/" + statusCode);
            } else {
                try {
                    StatusCode sc = StatusCode.getStatusCode(account, deviceID, statusCode); // may throw DBException
                    DBEdit editor = new DBEdit(sc);
                    editor.edit(); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing StatusCode: " + accountID + "/" + deviceID + "/" + statusCode);
                    dbe.printException();
                }
            }
            System.exit(0);
        }

        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            opts++;
            String listType = RTConfig.getString(ARG_LIST,null);
            // TODO: complete ...
        }
        
        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }
    
}
