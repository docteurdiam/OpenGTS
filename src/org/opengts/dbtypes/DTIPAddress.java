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
//  2006/08/21  Martin D. Flynn
//      Initial release
// ----------------------------------------------------------------------------
package org.opengts.dbtypes;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

public class DTIPAddress
    extends DBFieldType
{

    // ------------------------------------------------------------------------

    private IPTools.IPAddress ipAddr = null;
    
    public DTIPAddress(IPTools.IPAddress ipAddr)
    {
        this.ipAddr = ipAddr;
    }
    
    public DTIPAddress(String ipAddr)
    {
        super(ipAddr);
        this.ipAddr = new IPTools.IPAddress(ipAddr);
    }

    public DTIPAddress(ResultSet rs, String fldName)
        throws SQLException
    {
        super(rs, fldName);
        // set to default value if 'rs' is null
        this.ipAddr = (rs != null)? new IPTools.IPAddress(rs.getString(fldName)) : null;
    }

    // ------------------------------------------------------------------------

    public boolean isMatch(String ipAddr)
    {
        if (this.ipAddr != null) {
            return this.ipAddr.isMatch(ipAddr);
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    public Object getObject()
    {
        return this.toString();
    }

    public String toString()
    {
        return (this.ipAddr != null)? this.ipAddr.toString() : "";
    }

    // ------------------------------------------------------------------------

    public boolean equals(Object other)
    {
        if (this == other) {
            // same object
            return true;
        } else
        if (other instanceof DTIPAddress) {
            DTIPAddress otherList = (DTIPAddress)other;
            if (otherList.ipAddr == this.ipAddr) {
                // will also match if both are null
                return true;
            } else
            if ((this.ipAddr == null) || (otherList.ipAddr == null)) {
                // one is null, the other isn't
                return false;
            } else {
                // IPAddressList match
                return this.ipAddr.equals(otherList.ipAddr);
            }
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

}
