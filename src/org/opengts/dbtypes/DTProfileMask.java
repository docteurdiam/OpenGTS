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

public class DTProfileMask
    extends DBFieldType
{
    
    // ------------------------------------------------------------------------

    private byte profileMask[] = null;
    
    public DTProfileMask(byte profileMask[])
    {
        this.profileMask = (profileMask != null)? profileMask : new byte[0];
    }
    
    public DTProfileMask(String val)
    {
        super(val);
        //Print.logInfo("Creating new Mask from string: " + val + " [" + val.length() + "]");
        this.profileMask = DBField.parseBlobString(val);
    }

    public DTProfileMask(ResultSet rs, String fldName)
        throws SQLException
    {
        super(rs, fldName);
        // set to default value if 'rs' is null
        this.profileMask = (rs != null)? rs.getBytes(fldName) : new byte[0];
    }

    // ------------------------------------------------------------------------

    public Object getObject()
    {
        //Print.logWarn("ProfileMask length = " + this.profileMask.length);
        return this.profileMask;
    }
    
    public String toString()
    {
        return "0x" + StringTools.toHexString(this.profileMask);
    }

    // ------------------------------------------------------------------------

    public void setLimitTimeInterval(int minutes)
    {
        int byteLen = (minutes + 7) / 8;
        if (this.profileMask.length != byteLen) {
            byte newMask[] = new byte[byteLen];
            if (newMask.length > 0) {
                int len = (this.profileMask.length < byteLen)? this.profileMask.length : byteLen;
                System.arraycopy(this.profileMask, 0, newMask, 0, len);
            }
            this.profileMask = newMask;
        }
    }

    // ------------------------------------------------------------------------

    public byte[] getByteMask()
    {
        return this.profileMask;
    }
    
}
