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
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.Account;
import org.opengts.db.tables.User;

public class UserRecord<RT extends DBRecord>
    extends AccountRecord<RT>
{

    // ------------------------------------------------------------------------

    /* common Asset/Device field definition */
    public static final String FLD_userID = "userID";

    /* create a new "accountID" key field definition */
    protected static DBField newField_userID(boolean key)
    {
        return new DBField(FLD_userID, String.class, DBField.TYPE_USER_ID(), "User ID", (key?"key=true":"edit=2"));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static abstract class UserKey<RT extends DBRecord>
        extends AccountKey<RT>
    {
        public UserKey() {
            super();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Bean instance */
    public UserRecord()
    {
        super();
    }

    /* database record */
    public UserRecord(UserKey<RT> key)
    {
        super(key);
    }
         
    // ------------------------------------------------------------------------

    /* User ID */
    public String getUserID()
    {
        String v = (String)this.getFieldValue(FLD_userID);
        return (v != null)? v : "";
    }
    
    private void setUserID(String v)
    {
        this.setFieldValue(FLD_userID, ((v != null)? v : ""));
    }

    // ------------------------------------------------------------------------
    // The following is an optimization for holding the Account record while
    // processing this DeviceRecord.  Use with caution.
    
    private User user = null;

    public final User getUser()
    {
        if (this.user == null) {
            String userID = this.getUserID();
            Print.logDebug("[Optimize] Retrieving User record: " + userID);
            try {
                this.user = User.getUser(this.getAccount(), userID);
                // 'this.asset' may still be null if the asset was not found
            } catch (DBException dbe) {
                // may be caused by "java.net.ConnectException: Connection refused: connect"
                Print.logError("User not found: " + this.getAccountID() + "/" + userID);
                this.user = null;
            }
        }
        return this.user;
    }

    public final void setUser(User user) 
    {
        if ((user != null) && 
            user.getAccountID().equals(this.getAccountID()) &&
            user.getUserID().equals(this.getUserID()      )   ) {
            this.setAccount(user.getAccount());
            this.user = user;
        } else {
            this.user = null;
        }
    }

    // ------------------------------------------------------------------------
    
    private String  userDesc = null;

    public final String getUserDescription()
    {
        if (this.userDesc == null) {
            User user = this.getUser();
            this.userDesc = (user != null)? user.getDescription() : this.getUserID();
        } 
        return this.userDesc;
    }

    // ------------------------------------------------------------------------

}
