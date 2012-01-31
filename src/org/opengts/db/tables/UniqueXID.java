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
//  2008/05/14  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;

public class UniqueXID
    extends DBRecord<UniqueXID>
{

    // ------------------------------------------------------------------------

    // set to true when it is desireable to query the UniqueXID table entries for 
    // a specific Device/Asset reference.
    public static boolean isUniqueQueryEnabled()
    {
        return RTConfig.getBoolean(DBConfig.PROP_UniqueXID_queryEnabled);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    private static final String _TABLE_NAME             = "UniqueXID";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_uniqueID             = "uniqueID";
    public static final String FLD_accountID            = Account.FLD_accountID;
    public static final String FLD_transportID          = Transport.FLD_transportID;
    private static DBField FieldInfo[] = {
        new DBField(FLD_uniqueID        , String.class  , DBField.TYPE_UNIQ_ID()  , "Unique ID"    , "key=true"),
        new DBField(FLD_accountID       , String.class  , DBField.TYPE_ACCT_ID()  , "Account ID"   , "edit=2"),
        new DBField(FLD_transportID     , String.class  , DBField.TYPE_XPORT_ID() , "Transport ID" , "edit=2"),
    };

    /* key class */
    public static class Key
        extends DBRecordKey<UniqueXID>
    {
        public Key() {
            super();
        }
        public Key(String uniqueId) {
            super.setFieldValue(FLD_uniqueID, ((uniqueId != null)? uniqueId.toLowerCase() : ""));
        }
        public DBFactory<UniqueXID> getFactory() {
            return UniqueXID.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<UniqueXID> factory = null;
    public static DBFactory<UniqueXID> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                UniqueXID.TABLE_NAME(), 
                UniqueXID.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                UniqueXID.class, 
                UniqueXID.Key.class,
                false/*editable*/,false/*viewable*/);
        }
        return factory;
    }

    /* Bean instance */
    public UniqueXID()
    {
        super();
    }

    /* database record */
    public UniqueXID(UniqueXID.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(UniqueXID.class, loc);
        return i18n.getString("UniqueXID.description", 
            "This table defines " +
            "system-wide mapping of Transport Unique-IDs to a specific Account/Transport."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
    
    /* return the Unique ID for this record */
    public String getUniqueID()
    {
        String v = (String)this.getFieldValue(FLD_uniqueID);
        return StringTools.trim(v);
    }
    
    /* set the Unique ID for this record */
    private void setUniqueID(String v)
    {
        this.setFieldValue(FLD_uniqueID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the Account ID for this record */
    public String getAccountID()
    {
        String v = (String)this.getFieldValue(FLD_accountID);
        return StringTools.trim(v);
    }
    
    /* set the Account ID for this record */
    private void setAccountID(String v)
    {
        this.setFieldValue(FLD_accountID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the Transport ID for this record */
    public String getTransportID()
    {
        String v = (String)this.getFieldValue(FLD_transportID);
        return StringTools.trim(v);
    }
    
    /* set the Transport ID for this record */
    private void setTransportID(String v)
    {
        this.setFieldValue(FLD_transportID, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* get string value for specified property */
    public static UniqueXID getUniqueXID(String uniqueID)
        throws DBException
    {
        return UniqueXID.getUniqueXID(uniqueID, false);
    }
    
    /* get string value for specified property */
    public static UniqueXID getUniqueXID(String uniqueID, boolean create)
        throws DBException
    {
        if ((uniqueID == null) || uniqueID.equals("")) {
            // invalid key specified
            return null;
        } else {
            UniqueXID.Key key = new UniqueXID.Key(uniqueID);
            if (key.exists()) { // may throw DBException
                // return existing UniqueXID
                return key.getDBRecord(true);
            } else
            if (create) {
                // create a new UniqueXID (not yet saved)
                UniqueXID uniqXport = key.getDBRecord();
                uniqXport.setCreationDefaultValues();
                return uniqXport;
            } else {
                // not found, and not created
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
    }
    
}
