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
//  2009/04/02  Martin D. Flynn
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
import org.opengts.db.tables.DeviceGroup;

public class GroupRecord<RT extends DBRecord>
    extends AccountRecord<RT>
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* common Asset/Group field definition */
    public static final String FLD_groupID = "groupID";

    /* create a new "groupID" key field definition */
    public static DBField newField_groupID(boolean key)
    {
        return GroupRecord.newField_groupID(key, "Device Group ID");
    }

    /* create a new "groupID" key field definition */
    public static DBField newField_groupID(boolean key, String title)
    {
        return new DBField(FLD_groupID, String.class, DBField.TYPE_GROUP_ID(), title, (key?"key=true":"edit=2"));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static abstract class GroupKey<RT extends DBRecord>
        extends AccountKey<RT>
    {
        public GroupKey() {
            super();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Bean instance */
    public GroupRecord()
    {
        super();
    }

    /* database record */
    public GroupRecord(GroupKey<RT> key)
    {
        super(key);
    }
         
    // ------------------------------------------------------------------------

    /* Group ID */
    public final String getGroupID()
    {
        String v = (String)this.getFieldValue(FLD_groupID);
        return StringTools.trim(v);
    }
    
    public /*final*/ void setGroupID(String v)
    {
        this.setFieldValue(FLD_groupID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    // The following is an optimization for holding the Account record while
    // processing this GroupRecord.  Use with caution.
    
    private DeviceGroup group = null;

    public final boolean hasGroup()
    {
        return (this.group != null);
    }

    /* get the device for this event */
    public final DeviceGroup getDeviceGroup()
    {
        if (this.group == null) {
            String groupID = this.getGroupID();
            Print.logDebug("[Optimize] Retrieving Device Group record: " + groupID);
            try {
                this.group = DeviceGroup.getDeviceGroup(this.getAccount(), groupID);
                // 'this.device' may still be null if the asset was not found
            } catch (DBException dbe) {
                // may be caused by "java.net.ConnectException: Connection refused: connect"
                Print.logError("Group not found: " + this.getAccountID() + "/" + groupID);
                this.group = null;
            }
        }
        return this.group;
    }

    /* set thegroup for this event */
    public final void setDeviceGroup(DeviceGroup grp) 
    {
        if ((grp != null) && 
            grp.getAccountID().equals(this.getAccountID()) &&
            grp.getGroupID().equals(this.getGroupID()  )   ) {
            this.setAccount(grp.getAccount());
            this.group = grp;
        } else {
            this.group = null;
        }
    }

    // ------------------------------------------------------------------------

    private String  groupDesc = null;

    /**
    *** Return the description for this DBRecord's Device
    *** @return The Device description
    **/
    public final String getGroupDescription()
    {
        if (this.groupDesc == null) {
            DeviceGroup grp = this.getDeviceGroup();
            this.groupDesc = (grp != null)? grp.getDescription() : this.getGroupID();
        } 
        return this.groupDesc;
    }

    // ------------------------------------------------------------------------

}
