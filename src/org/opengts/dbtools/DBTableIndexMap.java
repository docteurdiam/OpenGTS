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
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBTableIndexMap</code> is used by DBProvider to hold actual table column 
*** to index, and index to column mappings.
**/

public class DBTableIndexMap
{

    // ------------------------------------------------------------------------

    private String                  utableName          = null;
    
    private int                     count = 0;
    private Set<String>             alternateIndexes    = null;
    private Map<String,Set<String>> mapFieldsToIndexes  = null;
    private Map<String,Set<String>> mapIndexesToFields  = null;
    
    /**
    *** Constructor
    *** @param tableName The untranslated table name for this index map
    **/
    public DBTableIndexMap(String utableName) 
    {
        super();
        this.utableName         = utableName;
        this.count              = 0;
        this.alternateIndexes   = null;
        this.mapFieldsToIndexes = new HashMap<String,Set<String>>();
        this.mapIndexesToFields = new HashMap<String,Set<String>>();
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the index/column name to this index map
    *** @param ndxName  The index name
    *** @param colName  The field/column name
    **/
    public void addIndexColumn(String ndxName, String colName) 
    {
        if ((ndxName == null) || ndxName.equals("")) {
            // index name not specified
        } else
        if ((colName == null) || colName.equals("")) {
            // column name not specified
        } else {
            
            /* count this entry */
            this.count++;
            
            /* alternate index */
            if (!ndxName.equalsIgnoreCase(DBProvider.PRIMARY_INDEX_NAME)) {
                if (this.alternateIndexes == null) { this.alternateIndexes = new HashSet<String>(); }
                this.alternateIndexes.add(ndxName);
            }
            
            /* map columns to indexes */
            Set<String> ndxSet = this.mapFieldsToIndexes.get(colName);
            if (ndxSet == null) {
                ndxSet = new HashSet<String>();
                this.mapFieldsToIndexes.put(colName, ndxSet);
            }
            ndxSet.add(ndxName);
            
            /* map indexes to columns */
            Set<String> colSet = this.mapIndexesToFields.get(ndxName);
            if (colSet == null) {
                colSet = new HashSet<String>();
                this.mapIndexesToFields.put(ndxName, colSet);
            }
            colSet.add(colName);
            
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a set of the alternate indexes ("PRIMARY" has been removed).
    *** @return A set of the alternate indexes.
    **/
    public Set<String> getAlternateIndexes() 
    {
        return this.alternateIndexes;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a set of all defined indexes (including "PRIMARY" if defined)
    *** @return A set of all defined indexes.
    **/
    public Set<String> getIndexes() 
    {
        return this.mapIndexesToFields.keySet();
    }

    /**
    *** Gets a set of all indexes defined for the specified column name
    *** @param colName The column name
    *** @return A set of all indexes defined for the specified column name, or null 
    ***         if the column is not defined within any index.
    **/
    public Set<String> getIndexesForColumn(String colName) 
    {
        return (colName != null)? this.mapFieldsToIndexes.get(colName) : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a set of all column names defined within any index
    *** @return A set of all column names defined within any index
    **/
    public Set<String> getColumns() 
    {
        return this.mapFieldsToIndexes.keySet();
    }
    
    /**
    *** Gets a set of all column names defined for the specified index
    *** @param ndxName The index name
    *** @return A set of all column names defined for the specified index, or null 
    ***         if the index is not defined for this table.
    **/
    public Set<String> getColumnsForIndex(String ndxName) 
    {
        return (ndxName != null)? this.mapIndexesToFields.get(ndxName) : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if no index has been defined for this table
    *** @return True if no index has been defined for this table
    **/
    public boolean isEmpty() 
    {
        return (this.count == 0);
    }

    // ------------------------------------------------------------------------

}
