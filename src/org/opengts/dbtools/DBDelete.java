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
// Database specific 'DELETE' handler.
// ----------------------------------------------------------------------------
// Change History:
//  2007/09/16  Martin D. Flynn
//     -Initial release
//     -NOTE: This module is not thread safe (this is typically not an issue, since
//      the use of this class is limited to the creation for a specific 'DELETE'
//      statement within a given thread).
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.text.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBDelete</code> provides the creation of the SQL provider specific 
*** DELETE statement.
**/

public class DBDelete
{
    
    // ------------------------------------------------------------------------
    
    private DBFactory factory         = null;
    
    private String    utableName      = null;
    private String    where           = null;

    /**
    *** Constructor
    *** @param fact The table DBFactory
    **/
    public DBDelete(DBFactory fact)
    {
        this.factory = fact;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if a DBFactory has been defined
    *** @return True if a DBFactory has been defined
    **/
    public boolean hasFactory()
    {
        return (this.factory != null);
    }
    
    /**
    *** Gets the DBFactory defined for this DBDelete
    *** @return The defined DBFactory
    **/
    public DBFactory getFactory()
    {
        return this.factory;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the untranslated table name for this DBDelete (if not set, the table 
    *** name of the defined DBFactory will be used).
    *** @param utableName  The table name
    **/
    public void setUntranslatedTableName(String utableName)
    {
        this.utableName = !StringTools.isBlank(utableName)? utableName : null;
    }
    
    /**
    *** Gets the untranslated table name for this DBDelete
    *** @return The defined table name
    **/
    public String getUntranslatedTableName()
    {
        if (this.utableName != null) {
            return this.utableName;
        } else
        if (this.hasFactory()) {
            return this.getFactory().getUntranslatedTableName();
        } else {
            return "UNKNOWN";
        }
    }

    /**
    *** Gets the table name for this DBSelect
    *** @return The defined table name
    **/
    public String getTranslatedTableName()
    {
        return DBProvider.translateTableName(this.getUntranslatedTableName());
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a new DBWhere instance (calling 'setWhere(...)' is still required in order
    *** to used the created DBWhere instance for this DBDelete).
    *** @return The new DBWhere instance
    **/
    public DBWhere createDBWhere()
    {
        return new DBWhere(this.getFactory());
    }

    /**
    *** Sets the DBWhere instance used for this DBDelete
    *** @param wh  The DBWhere instance used for this DBDelete
    **/
    public void setWhere(String wh)
    {
        if (StringTools.isBlank(wh)) {
            this.where = null;
        } else {
            wh = wh.trim();
            if (StringTools.startsWithIgnoreCase(wh,"WHERE ")) {
                this.where = wh;
            } else {
                this.where = "WHERE ( " + wh + " )";
            }
        }
    }
    
    /**
    *** Returns true if this DBDelete has a defined where clause
    *** @return True if this DBDelete has a defined where clause
    **/
    public boolean hasWhere()
    {
        return (this.where != null);
    }

    /**
    *** Gets the where clause for this DBDelete
    *** @return The where clause for this DBDelete
    **/
    public String getWhere()
    {
        return this.where;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the DELETE statement for this DBDelete
    *** @return The DELETE statement for this DBDelete
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        /* DELETE FROM */
        sb.append("DELETE FROM ");
        sb.append(this.getTranslatedTableName());

        /* WHERE */
        if (this.hasWhere()) {
            sb.append(" ");
            sb.append(this.getWhere());
        }
        
        return sb.toString();
    }

    // ------------------------------------------------------------------------

}
