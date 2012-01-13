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
// Database specific 'SELECT' handler.
// ----------------------------------------------------------------------------
// Change History:
//  2007/09/16  Martin D. Flynn
//     -Initial release
//     -NOTE: This module is not thread safe (this is typically not an issue, since
//      the use of this class is limited to the creation for a specific 'SELECT'
//      statement within a given thread).
//  2008/03/12  Martin D. Flynn
//     -Added 'OFFSET' support for MySQL
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.text.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBSelect</code> represents an SQL select statement.
**/

public class DBSelect<gDBR extends DBRecord>
{

    // ------------------------------------------------------------------------
    
    private DBFactory<gDBR> factory         = null;
    
    private String          selectFields[]  = null;
    private String          utableName      = null;
    private String          where           = null;
    private String          orderByFields[] = null;
    private boolean         ascending       = true; // default ascending
    private long            limit           = 0L;   // no limit
    private long            offset          = 0L;   // beginning of list

    /**
    *** Constructor
    *** @param fact  The table DBFactory instance
    **/
    public DBSelect(DBFactory<gDBR> fact)
    {
        this.factory = fact;
    }
    
    /**
    *** Constructor
    *** @param fact  The table DBFactory instance
    *** @param where The initial selection SQL 'WHERE' clause
    **/
    public DBSelect(DBFactory<gDBR> fact, String where)
    {
        this(fact);
        this.setWhere(where);
    }
    
    /**
    *** Constructor
    *** @param fact  The table DBFactory instance
    *** @param where The initial selection SQL 'WHERE' clause
    **/
    public DBSelect(DBFactory<gDBR> fact, DBWhere where)
    {
        this(fact);
        this.setWhere(where);
    }

    // ------------------------------------------------------------------------
    // DBFactory methods
    
    /**
    *** Returns true if a DBFactory instance has been properly defined for this instance
    *** @return True if this DBSelect instance has a defined table DBFactory
    **/
    public boolean hasFactory()
    {
        return (this.factory != null);
    }
    
    /**
    *** Gets the table DBFactory instance for this DBSelect
    *** @return The table DBFactory instance
    **/
    public DBFactory<gDBR> getFactory()
    {
        return this.factory;
    }
    
    // ------------------------------------------------------------------------
    // Selected fields

    /**
    *** Sets a list of selected fields
    *** @param sf  An Set of field names to select
    **/
    public void setSelectedFields(Set<String> sf)
    {
        if (ListTools.isEmpty(sf)) {
            this.selectFields = null;
        } else {
            this.setSelectedFields(sf.toArray(new String[sf.size()]));
        }
    }

    /**
    *** Sets an array of selected fields
    *** @param sf  An array of field names to select
    **/
    public void setSelectedFields(String... sf)
    {
        if (ListTools.isEmpty(sf)) {
            this.selectFields = null;
        } else {
            DBFactory<gDBR> fact = this.getFactory();
            if (fact != null) {
                for (int i = 0; i < sf.length; i++) {
                    if (sf[i] == null) {
                        Print.logError("DBFactory field is null: %s.%s", fact.getUntranslatedTableName(), sf[i]);
                    } else
                    if (fact.hasField(sf[i])) {
                        // ok, DBFactory contains field
                    } else
                    if (sf[i].equalsIgnoreCase(DBProvider.FLD_COUNT())) {
                        // ok, "COUNT(*)" allowed
                    } else {
                        Print.logError("DBFactory field does not exist: %s.%s", fact.getUntranslatedTableName(), sf[i]);
                    }
                }
            }
            this.selectFields = sf;
        }
    }

    /**
    *** Returns true if this DBSelect has selected fields defined
    *** @return True if this DBSelect has selected fields defined
    **/
    public boolean hasSelectedFields()
    {
        return (this.selectFields != null);
    }
    
    /**
    *** Gets the selected fields
    *** @return The selected fields, or null if no fields have been defined
    **/
    public String[] getSelectedFields()
    {
        return ((this.selectFields != null) && (this.selectFields.length > 0))? this.selectFields : null;
    }

    // ------------------------------------------------------------------------
    // Table name

    /**
    *** Sets the untranslated table name for this DBSelect (if not set, the table 
    *** name of the defined DBFactory will be used).
    *** @param tableName  The untranslated table name
    **/
    public void setUntranslatedTableName(String utableName)
    {
        this.utableName = !StringTools.isBlank(utableName)? utableName : null;
    }

    /**
    *** Gets the table name for this DBSelect
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
    // Where

    /**
    *** Creates a new DBWhere instance (calling 'setWhere(...)' is still required in order
    *** to used the created DBWhere instance for this DBSelect).
    *** @return The new DBWhere instance
    **/
    public DBWhere createDBWhere()
    {
        return new DBWhere(this.getFactory());
    }
    
    /**
    *** Sets the DBWhere instance used for this DBSelect
    *** @param wh  The DBWhere instance used for this DBSelect
    **/
    public void setWhere(DBWhere wh)
    {
        this.setWhere((wh != null)? wh.toString() : null);
    }
    
    /**
    *** Sets the where clause used for this DBSelect
    *** @param wh  The where clause used for this DBSelect
    **/
    public void setWhere(String wh)
    {
        if (StringTools.isBlank(wh)) {
            this.where = null;
        } else {
            wh = wh.trim();
            // MySQL:      WHERE ...
            // SQLServer:  WHERE ...
            // Derby:      WHERE ...
            // PostgreSQL: WHERE ...
            if (StringTools.startsWithIgnoreCase(wh,"WHERE ")) {
                this.where = wh;
            } else {
                this.where = "WHERE ( " + wh + " )";
            }
        }
    }
    
    /**
    *** Returns true if this DBSelect has a defined where clause
    *** @return True if this DBSelect has a defined where clause
    **/
    public boolean hasWhere()
    {
        return (this.where != null);
    }

    /**
    *** Gets the where clause for this DBSelect
    *** @return The where clause for this DBSelect
    **/
    public String getWhere()
    {
        return this.where;
    }
    
    // ------------------------------------------------------------------------
    // ORDER BY

    /**
    *** Sets the order-by fields
    *** @param obf The field names by which the returned results set will be sorted
    **/
    public void setOrderByFields(String... obf)
    {
        if ((obf == null) || (obf.length == 0)) {
            this.orderByFields = null;
        } else {
            DBFactory<gDBR> fact = this.getFactory();
            if (fact != null) {
                for (int i = 0; i < obf.length; i++) {
                    if (obf[i] == null) {
                        Print.logError("DBFactory field is null: %s.%s", fact.getUntranslatedTableName(), obf[i]);
                    } else
                    if (fact.hasField(obf[i])) {
                        // ok
                    } else {
                        Print.logError("DBFactory field does not exist: %s.%s", fact.getUntranslatedTableName(), obf[i]);
                    }
                }
            }
            this.orderByFields = obf;
        }
    }

    /**
    *** Returns true if this DBSelect has order-by fields defined
    *** @return True if this DBSelect has order-by fields defined
    **/
    public boolean hasOrderByFields()
    {
        return (this.orderByFields != null);
    }
    
    /**
    *** Set ascending/descending sort order
    *** @param ascending True to sort ascending, false to sort descending
    **/
    public void setOrderAscending(boolean ascending)
    {
        this.ascending = ascending;
    }

    /**
    *** Returns true if the ording is ascending, false if descending
    *** @return True if the ording is ascending, false if descending
    **/
    public boolean isOrderAscending()
    {
        return this.ascending;
    }
    
    /**
    *** Gets the order-byte fields
    *** @return An array of order-by fields, or null if no order-by fields have been defined
    **/
    public String[] getOrderByFields()
    {
        return ((this.orderByFields != null) && (this.orderByFields.length > 0))? this.orderByFields : null;
    }

    // ------------------------------------------------------------------------
    // LIMIT

    /** 
    *** Returns true if the DBProvider supports a LIMIT clause
    *** @return True if the DBProvider supports a LIMIT clause
    **/
    public boolean supportsLimit()
    {
        // Derby does not support LIMIT
        return DBProvider.getProvider().supportsLimit();
    }

    /**
    *** Sets the LIMIT for this DBSelect
    *** @param limit  The record limit
    **/
    public void setLimit(long limit)
    {
        this.limit = (limit > 0L)? limit : 0L;
        if ((this.limit > 0) && !this.supportsLimit()) {
            // Warn when LIMIT is specified, but not supported by the DBProvider
            Print.logStackTrace("Warning: LIMIT not supported by DBProvider: " + this.limit);
        }
    }

    /**
    *** Returns true if a limit has been defined for this DBSelect
    *** @return True if a limit has been defined for this DBSelect
    **/
    public boolean hasLimit()
    {
        if (this.limit <= 0L) {
            return false;
        } else {
            return true;
        }
    }
 
    /**
    *** Gets the defined limit
    *** @return The defined limit, or -1 if no limit has been defined
    **/
    public long getLimit()
    {
        return this.limit;
    }

    // ------------------------------------------------------------------------
    // OFFSET

    /** 
    *** Returns true if the DBProvider supports a OFFSET clause
    *** @return True if the DBProvider supports a OFFSET clause
    **/
    public boolean supportsOffset()
    {
        // Derby does not support LIMIT/OFFSET
        return DBProvider.getProvider().supportsOffset();
    }

    /**
    *** Sets the OFFSET for this DBSelect
    *** @param offset  The record offset
    **/
    public void setOffset(long offset)
    {
        this.offset = (offset > 0L)? offset : -1L;
        if ((this.offset > 0) && !this.supportsOffset()) {
            // Warn when OFFSET is specified, but not supported by the DBProvider
            Print.logStackTrace("Warning: OFFSET not supported by DBProvider: " + this.offset);
        }
    }

    /**
    *** Returns true if a offset has been defined for this DBSelect
    *** @return True if a offset has been defined for this DBSelect
    **/
    public boolean hasOffset()
    {
        if (this.offset <= 0L) {
            return false;
        } else {
            return true;
        }
    }

    /**
    *** Gets the defined offset
    *** @return The defined offset, or -1 if no offset has been defined
    **/
    public long getOffset()
    {
        return this.offset;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the DBRecordKey of the last record retrieved by this DBSelect.<br>
    *** Called by DBRecordIterator to allow subclasses of this DBSelect to adjust
    *** the selection criteria if necessary.
    *** @param rcdKey  The DBRecordKey of the last record retrieved by this DBSelect.
    **/
    public void setLastRecordKey(DBRecordKey<gDBR> rcdKey)
    {
        // managed by subclasses of DBSelect
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the DBSelect statement as a String representation
    *** @return The DBSelect statement as a String representation
    **/
    public String toString()
    {
        return this._toString(true);
    }
    
    /**
    *** Returns the DBSelect statement as a String representation
    *** @param inclSelect  True to include the full DBSelect, false to only include the WHERE clause
    *** @return The DBSelect statement as a String representation
    **/
    private String _toString(boolean inclSelect)
    {
        int dbid = DBProvider.getProvider().getID();
        StringBuffer sb = new StringBuffer();

        /* include "SELECT" in string? */
        if (inclSelect) {
            // TODO: check on select statements that do not include 'SELECT', but
            // do have a limit. (only an issue with SQLServer).
            
            /* SELECT fields */
            // MySQL:      SELECT field,... FROM ... WHERE ... ORDER BY ... [DESC] LIMIT <limit> OFFSET <offset>
            // SQLServer:  SELECT [TOP <n>] field,... FROM ... WHERE ... ORDER BY ... [DESC]
            // Derby:      SELECT field,... FROM ... WHERE ... ORDER BY ... 
            // PostgreSQL: SELECT field,... FROM ... WHERE ... ORDER BY ... [DESC] LIMIT <limit> OFFSET <offset>
            sb.append("SELECT ");

            /* SQLServer: TOP <n> */
            if ((dbid == DBProvider.DB_SQLSERVER) && this.hasLimit()) {
                sb.append(" TOP ").append(this.getLimit()); // and offset?
            }

            /* selected fields */
            if (this.hasSelectedFields()) {
                String fld[] = this.getSelectedFields();
                for (int i = 0; i < fld.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(fld[i]);
                }
            } else {
                sb.append("*");
            }
        
            /* FROM table */
            sb.append(" FROM ").append(this.getTranslatedTableName());
            
        }
        
        /* WHERE */
        if (this.hasWhere()) {
            sb.append(" ").append(this.getWhere());
        }
        
        /* ORDER BY */
        if (this.hasOrderByFields()) {
            sb.append(" ORDER BY ");
            String fld[] = this.getOrderByFields();
            for (int i = 0; i < fld.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(fld[i]);
            }
            if (!this.isOrderAscending()) {
                sb.append(" DESC");
            }
        }
        
        /* LIMIT */
        if (this.hasLimit()) {
            // TODO: warn if "ORDER BY" has not been specified?
            switch (dbid) {
                case DBProvider.DB_MYSQL:
                    sb.append(" LIMIT ").append(this.getLimit());
                    if (this.hasOffset()) {
                        sb.append(" OFFSET ").append(this.getOffset());
                    }
                    break;
                case DBProvider.DB_POSTGRESQL:
                    sb.append(" LIMIT ").append(this.getLimit());
                    if (this.hasOffset()) {
                        sb.append(" OFFSET ").append(this.getOffset());
                    }
                    break;
                case DBProvider.DB_SQLSERVER:
                    // already applied above in "TOP" 
                    // (yes, it should be here instead, but it isn't.  Deal with it.)
                    break;
                case DBProvider.DB_DERBY:
                    // Derby doesn't support any form of 'LIMIT'
                    break;
            }
        }
        
        return sb.toString();
    }

    // ------------------------------------------------------------------------

}
