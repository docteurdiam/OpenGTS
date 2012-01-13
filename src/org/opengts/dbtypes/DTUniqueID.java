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
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/05  Martin D. Flynn
//      Moved to "OpenGTS"
// ----------------------------------------------------------------------------
package org.opengts.dbtypes;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

public class DTUniqueID
    extends DBFieldType
{

    // ------------------------------------------------------------------------

    private long uniqueID = 0L;
    
    public DTUniqueID(long uniqueID)
    {
        this.uniqueID = uniqueID;
    }
    
    public DTUniqueID(String uniqueIDHex)
    {
        super(uniqueIDHex);
        // If 'uniqueIDHex' represents a 'HEX' value, then it must begin with '0x'.
        // Otherwise it will be parsed as a 'Long'
        this.uniqueID = StringTools.parseLong(uniqueIDHex, 0L);
    }

    public DTUniqueID(ResultSet rs, String fldName)
        throws SQLException
    {
        super(rs, fldName);
        // set to default value if 'rs' is null
        this.uniqueID = (rs != null)? rs.getLong(fldName) : 0L;
    }

    // ------------------------------------------------------------------------

    public Object getObject()
    {
        return new Long(this.uniqueID);
    }

    public String toString()
    {
        return String.valueOf(this.uniqueID);
    }

    // ------------------------------------------------------------------------

}
