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
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/09/16  Martin D. Flynn
//     -Added 'getObject' method
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBFieldType</code> is an abstract superclass for custom field types
**/

public abstract class DBFieldType
{

    // ------------------------------------------------------------------------

    /** 
    *** Constructor
    **/
    public DBFieldType()
    {
        super();
    }

    /** 
    *** Constructor
    *** @param val  A default initialization value
    **/
    public DBFieldType(String val)
    {
        this();
    }

    /** 
    *** Constructor
    *** @param rs  The ResultSet from which this field type will be initialized
    *** @param fldName  The field name within the ResultSet used to initialize this field type
    **/
    public DBFieldType(ResultSet rs, String fldName)
        throws SQLException
    {
        this();
        // override (NOTE: 'rs' may be null!)
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the current value
    *** @return The current value
    **/
    public abstract Object getObject();
    
    /**
    *** Gets the String representation of the current value
    *** @return The String representation of the current value
    **/
    public abstract String toString();
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the 'other' object is equivalent to this field type value
    *** @param other  The 'other' object
    *** @return True if the 'other' object is equivalent to this field type value
    **/
    public boolean equals(Object other)
    {
        if (this == other) {
            // same object
            return true;
        } else
        if (other instanceof DBFieldType) {
            // Warning: may still match if 'other' is not of the same subclass
            DBFieldType dft = (DBFieldType)other;
            return this.toString().equals(other.toString());
        } else {
            // 'other' is not the same class
            return false;
        }
    }

    // ------------------------------------------------------------------------

}
