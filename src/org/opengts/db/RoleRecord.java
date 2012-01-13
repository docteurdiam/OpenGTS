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
//     -Initial release (cloned from UserRecord.java)
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
import org.opengts.db.tables.Role;

public class RoleRecord<RT extends DBRecord>
    extends AccountRecord<RT>
{

    // ------------------------------------------------------------------------

    public static final String SYSTEM_ROLE_PREFIX   = "!";

    // ------------------------------------------------------------------------

    /* common Asset/Device field definition */
    public static final String FLD_roleID = "roleID";

    /* create a new "accountID" key field definition */
    protected static DBField newField_roleID(boolean key)
    {
        return new DBField(FLD_roleID, String.class, DBField.TYPE_ROLE_ID(), "Role ID", (key?"key=true":"edit=2"));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static abstract class RoleKey<RT extends DBRecord>
        extends AccountKey<RT>
    {
        public RoleKey() {
            super();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Bean instance */
    public RoleRecord()
    {
        super();
    }

    /* database record */
    public RoleRecord(RoleKey<RT> key)
    {
        super(key);
    }

    // ------------------------------------------------------------------------

    /* Role ID */
    public String getRoleID()
    {
        String v = (String)this.getFieldValue(FLD_roleID);
        return (v != null)? v : "";
    }

    private void setRoleID(String v)
    {
        this.setFieldValue(FLD_roleID, ((v != null)? v : ""));
    }

    /* return true if the specified role is a system-wide/syste-admin role */
    public static boolean isSystemAdminRoleID(String roleID)
    {
        if (StringTools.isBlank(roleID)) {
            return false;
        } else {
            return roleID.startsWith(SYSTEM_ROLE_PREFIX);
        }
    }

    /* return the displayed role ID */
    public static String getDisplayRoleID(String roleID)
    {
        if (StringTools.isBlank(roleID)) {
            return "";
        } else 
        if (roleID.startsWith(RoleRecord.SYSTEM_ROLE_PREFIX)) {
            return roleID.substring(RoleRecord.SYSTEM_ROLE_PREFIX.length()) + " *";
        } else {
            return roleID;
        }
    }

    // ------------------------------------------------------------------------
    // The following is an optimization for holding the Role record while
    // processing this RoleRecord.  Use with caution.
    
    private Role role = null;

    public final Role getRole()
    {
        if (this.role == null) {
            String roleID = this.getRoleID();
            Print.logDebug("[Optimize] Retrieving Role record: " + roleID);
            try {
                this.role = Role.getRole(this.getAccount(), roleID);
                // may still be null if the role was not found
            } catch (DBException dbe) {
                // may be caused by "java.net.ConnectException: Connection refused: connect"
                Print.logError("Role not found: " + this.getAccountID() + "/" + roleID);
                this.role = null;
            }
        }
        return this.role;
    }

    public final void setRole(Role role) 
    {
        if ((role != null) && 
            role.getAccountID().equals(this.getAccountID()) &&
            role.getRoleID().equals(this.getRoleID()      )   ) {
            this.setAccount(role.getAccount());
            this.role = role;
        } else {
            this.role = null;
        }
    }

    // ------------------------------------------------------------------------
    
    private String  roleDesc = null;

    public final String getRoleDescription()
    {
        if (this.roleDesc == null) {
            Role role = this.getRole();
            this.roleDesc = (role != null)? role.getDescription() : this.getRoleID();
        } 
        return this.roleDesc;
    }

    // ------------------------------------------------------------------------

}
