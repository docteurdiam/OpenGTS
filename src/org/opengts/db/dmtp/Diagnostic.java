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
//  2007/01/10  Martin D. Flynn
//     -Initial release
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

public class Diagnostic
    extends DeviceRecord<Diagnostic>
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME               = "Diagnostic";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_isError               = "isError";
    public static final String FLD_codeKey               = "codeKey";
    public static final String FLD_timestamp             = "timestamp";
    public static final String FLD_binaryValue           = "binaryValue";
    private static DBField FieldInfo[] = {
        // Diagnostic fields
        newField_accountID(true),
        newField_deviceID(true),
        new DBField(FLD_isError         , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , "Is Error"     , "key=true"),
        new DBField(FLD_codeKey         , Integer.TYPE  , DBField.TYPE_UINT32      , "Code Key"     , "key=true"),
        new DBField(FLD_timestamp       , Long.TYPE     , DBField.TYPE_UINT32      , "Timestamp"    , "key=true"),
        new DBField(FLD_binaryValue     , byte[].class  , DBField.TYPE_BLOB        , "Binary Value" , null),
    };

    /* key class */
    public static class Key
        extends DeviceKey<Diagnostic>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId, boolean isError, int codeKey, long timestamp) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_isError   , isError);
            super.setFieldValue(FLD_codeKey   , codeKey);
            super.setFieldValue(FLD_timestamp , timestamp);
        }
        public DBFactory<Diagnostic> getFactory() {
            return Diagnostic.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Diagnostic> factory = null;
    public static DBFactory<Diagnostic> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Diagnostic.TABLE_NAME(), 
                Diagnostic.FieldInfo, 
                DBFactory.KeyType.PRIMARY, 
                Diagnostic.class, 
                Diagnostic.Key.class,
                false/*editable*/,false/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Diagnostic()
    {
        super();
    }

    /* database record */
    public Diagnostic(Diagnostic.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Diagnostic.class, loc);
        return i18n.getString("Diagnostic.description", 
            "This table contains " +
            "Device specific diagnostic information collected from client devices."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    public boolean getIsError()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_isError);
        return (v != null)? v.booleanValue() : false;
    }

    public void setIsError(boolean v)
    {
        this.setFieldValue(FLD_isError, v);
    }
    
    public boolean isError()
    {
        return this.getIsError();
    }

    // ------------------------------------------------------------------------

    public int getCodeKey()
    {
        Integer v = (Integer)this.getFieldValue(FLD_codeKey);
        return (v != null)? v.intValue() : 0x0000;
    }
    
    public String getCodeKeyHex()
    {
        return StringTools.toHexString(this.getCodeKey(), 16);
    }

    public void setCodeKey(int v)
    {
        this.setFieldValue(FLD_codeKey, v);
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

    /* String representation */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getAccountID()).append("/").append(this.getDeviceID()).append("/");
        sb.append(this.isError()? "E:" : "D:");
        sb.append(StringTools.toHexString(this.getCodeKey(),16));
        return sb.toString();
    }

    /* get description */
    public String getDescription()
    {
        if (this.isError()) {
            return ClientErrors.getDescription(this.getCodeKey());
        } else {
            return ClientDiagnostics.getDescription(this.getCodeKey());
        }
    }

    /* return contained value as a String */
    public String getValueAsString()
    {
        return this.getValueAsString(null);
    }
    
    /* return contained value as a String */
    public String getValueAsString(TimeZone tz)
    {
        if (this.isError()) {
            return ClientErrors.getStringValue(this.getCodeKey(), this.getBinaryValue(), tz);
        } else {
            return ClientDiagnostics.getStringValue(this.getCodeKey(), this.getBinaryValue());
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* save property entry */
    public static boolean saveDiagnostic(Device device, int codeKey, byte codeVal[])
        throws DBException
    {
        if (device != null) {
            String acctID = device.getAccountID();
            String devID  = device.getDeviceID();
            return Diagnostic.saveDiagnostic(acctID, devID, codeKey, codeVal);
        } else {
            Print.logError("Diagnostic 'Device' is null");
            return false;
        }
    }
    
    /* save property entry */
    public static boolean saveDiagnostic(String acctID, String devID, int codeKey, byte codeVal[])
        throws DBException
    {
        if ((acctID == null) || acctID.equals("") ||
            (devID  == null) || devID.equals("")    ) {
            Print.logError("Diagnostic 'AccountID'/'DeviceID' is null");
            return false;
        } else
        if ((codeKey <= 0) || (codeVal == null)) {
            Print.logError("Diagnostic code/data is null");
            return false;
        } else {
            Diagnostic.Key key = new Diagnostic.Key(acctID, devID, false, codeKey, DateTime.getCurrentTimeSec());
            Diagnostic diag = new Diagnostic(key);
            diag.setBinaryValue(codeVal);
            diag.save();
            Print.logInfo("Saved Diagnostic code");
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /* save property entry */
    public static boolean saveError(Device device, int codeKey, byte codeVal[])
        throws DBException
    {
        if (device != null) {
            String acctID = device.getAccountID();
            String devID  = device.getDeviceID();
            return Diagnostic.saveError(acctID, devID, codeKey, codeVal);
        } else {
            Print.logError("Error 'Device' is null");
            return false;
        }
    }
    
    /* save property entry */
    public static boolean saveError(String acctID, String devID, int codeKey, byte codeVal[])
        throws DBException
    {
        if ((acctID == null) || acctID.equals("") ||
            (devID  == null) || devID.equals("")    ) {
            Print.logError("Error 'AccountID'/'DeviceID' is null");
            return false;
        } else
        if ((codeKey <= 0) || (codeVal == null)) {
            Print.logError("Error code/data is null");
            return false;
        } else {
            Diagnostic.Key key = new Diagnostic.Key(acctID, devID, true, codeKey, DateTime.getCurrentTimeSec());
            Diagnostic diag = new Diagnostic(key);
            diag.setBinaryValue(codeVal);
            diag.save();
            Print.logInfo("Saved Error code");
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get all property records for specified account/device */
    public static Diagnostic[] getDiagnostics(String acctId, String devId, 
        long timeStart, long timeEnd)
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
        // DBSelect: WHERE (((accountID=='acct')AND(deviceID='dev'))AND(timestamp>=123436789)AND(timestamp<=123436789) ORDER BY isError,codeKey
        DBSelect<Diagnostic> dsel = new DBSelect<Diagnostic>(Diagnostic.getFactory());
        DBWhere dwh = dsel.createDBWhere();
        dwh.append(dwh.AND(
            dwh.EQ(Diagnostic.FLD_accountID,acctId),
            dwh.EQ(Diagnostic.FLD_deviceID ,devId)
        ));
        if (timeStart >= 0L) {
            // AND (timestamp>=123436789)
            dwh.append(dwh.AND_(dwh.GE(Diagnostic.FLD_timestamp,timeStart)));
        }
        if ((timeEnd >= 0L) && (timeEnd >= timeStart)) {
            // AND (timestamp<=123456789)
            dwh.append(dwh.AND_(dwh.LE(Diagnostic.FLD_timestamp,timeEnd)));
        }
        dsel.setWhere(dwh.WHERE(dwh.toString()));
        dsel.setOrderByFields(FLD_isError, FLD_codeKey);
        dsel.setLimit(-1L);

        /* get Properties */
        Diagnostic d[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //d = (Diagnostic[])DBRecord.select(Diagnostic.getFactory(), dsel.toString(false));
            d = DBRecord.select(dsel); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }

        /* no properties */
        if (d == null) {
            // no records
            return null;
        }

        /* return array of properties */
        return d;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
