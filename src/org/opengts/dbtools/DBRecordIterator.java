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
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBRecordIterator</code> is used to iterate through a DBRecord selection.<br>
*** This DBRecordIterator currently uses the SQL DB provider OFFSET/LIMIT keywords to
*** iterate through a selection, and thus is only supported by DB providers that support
*** these keywords.  This method has the disadvantage that record insertions/deletions 
*** occurring while this DBRecordIterator is in use may cause this iterator to possibly 
*** miss some records, or produce duplicate records.
**/

public class DBRecordIterator<DBR extends DBRecord>
{

    // ------------------------------------------------------------------------

    public static final long    DEFAULT_LIMIT  = 50L;

    // ------------------------------------------------------------------------

    private Iterator<DBR>       iterator        = null;
    
    private DBSelect<DBR>       dbSelector      = null;
    private long                offset          = 0L;
    private long                limit           = DEFAULT_LIMIT;
    
    private DBRecordKey<DBR>    lastRecordKey   = null;
    
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param records An array of DBRecord
    **/
    public DBRecordIterator(DBR records[])
    {
        super();
        this.iterator   = ListTools.toIterator(records);
        this.dbSelector = null;
    }

    /**
    *** Constructor
    *** @param iterable An Iterable
    **/
    public DBRecordIterator(Iterable<DBR> iterable)
    {
        super();
        this.iterator   = ListTools.toIterator(iterable);
        this.dbSelector = null;
    }

    /**
    *** Constructor
    *** @param iterator An iterator
    **/
    public DBRecordIterator(Iterator<DBR> iterator)
    {
        super();
        this.iterator   = iterator;
        this.dbSelector = null;
    }

    /**
    *** Constructor
    *** @param dbSel The DBSelect instance
    *** @throws DBException if the DBProvider does not support offset/limit.
    **/
    public DBRecordIterator(DBSelect<DBR> dbSel)
        throws DBException
    {
        super();
        this.iterator   = null;
        this.dbSelector = dbSel;
        if (this.dbSelector != null) {
            if (!this.dbSelector.supportsLimit()) {
                throw new DBException("DB provider does not support LIMIT");
            } else
            if (!this.dbSelector.supportsOffset()) {
                throw new DBException("DB provider does not support OFFSET");
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the block size limit
    *** @return The block size limit
    **/
    public long getLimit()
    {
        return this.limit;
    }
    
    /**
    *** Sets the limit block size
    *** @param limit  The block size limit
    **/
    public void setLimit(long limit)
    {
        this.limit = (limit > 0L)? limit : DEFAULT_LIMIT;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if another DBRecord is available from the iterator
    *** @return True if another DBRecord is available from the iterator
    *** @throws DBException if a DB access error occurs.
    **/
    public boolean hasNext()
        throws DBException
    {
        try {
            return this.fetch().hasNext();
        } catch (DBNotFoundException nfe) {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the next object from the iterator
    *** @return The next object
    *** @throws DBException if a DB access error occurs.
    *** @throws DBNotFoundException if there ore no more DBRecords to return
    **/
    public DBR next()
        throws DBException, DBNotFoundException
    {
        try {
            return this.fetch().next(); 
        } catch (NoSuchElementException nse) {
            // not likely to occur
            throw new DBNotFoundException("", nse);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    **/
    protected DBSelect<DBR> getDBSelect()
    {
        return this.dbSelector;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Preloads the next block of records to return.
    *** @return The DBRecord iterator
    **/
    @SuppressWarnings("unchecked")
    protected Iterator<DBR> fetch()
        throws DBException, DBNotFoundException
    {

        /* data already available? */
        if ((this.iterator != null) && this.iterator.hasNext()) {
            // data already available
            return this.iterator;
        }
        
        /* exit now if we don't have the means to retrieve more records */
        if (this.dbSelector == null) {
            // unable to fetch data
            throw new DBNotFoundException("No db selector");
        }
        
        /* offset/limit */
        this.dbSelector.setOffset(this.offset);
        this.dbSelector.setLimit(this.limit);
        this.dbSelector.setLastRecordKey(this.lastRecordKey);

        /* get records */
        DBR rcdArry[] = null;
        try {
            //Print.logInfo("Fetch ... " + this.offset + ":" + this.limit);
            //DBProvider.lockTables(new String[] { this.dbSelector.getTableName() }, null);
            rcdArry = DBRecord.select(this.dbSelector, null); // "unchecked cast"
        } finally {
            //DBProvider.unlockTables();
        }

        /* end of data? */
        if ((rcdArry == null) || (rcdArry.length == 0)) {
            // no more records, this record iterator is done
            this.dbSelector = null;
            this.iterator = null;
            this.lastRecordKey = null;
            throw new DBNotFoundException("No more records");
        }
        
        /* last record retrieved */
        this.lastRecordKey = rcdArry[rcdArry.length - 1].getRecordKey(); // "unchecked cast"

        /* advance offset */
        this.offset += rcdArry.length;

        /* reset/return iterator */
        this.iterator = ListTools.toIterator(rcdArry);
        return this.iterator;
        
    }

    // ------------------------------------------------------------------------

}
