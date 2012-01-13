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
//  2006/04/09  Martin D. Flynn
//     -Integrate DBException
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/03/04  Martin D. Flynn
//     -Added support for DBRecordHandler to 'select(...)'.
//  2007/09/16  Martin D. Flynn
//     -Added "Row-by-Row" option on the 'execute' and 'getStatement' methods.
//      [see DBConnection.createStatement(...) for more information]
//     -Integrated DBSelect
//     -Removed 'getStatement' methods
//  2007/11/28  Martin D. Flynn
//     -Added table name to duplicate key error message.
//  2007/01/10  Martin D. Flynn
//     -In 'getRecordCount', wrap SQLException in DBException
//  2008/03/28  Martin D. Flynn
//     -Added new 'select' and 'getRecordCount' methods to accept a DBSelect argument.
//     -Removed obsolete "DBRecord.select(...)" methods
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
//  2008/06/20  Martin D. Flynn
//     -Added 'getValue'/'setValue' methods which attempt to locate the proper
//      getter/setter methods before defaulting to the generic 'get|setFieldValue'.
//  2009/05/01  Martin D. Flynn
//     -Added DateTime datatype
//  2009/05/27  Martin D. Flynn
//     -Added XML output
//  2009/08/07  Martin D. Flynn
//     -Added 'virtual' flag to allow disabling save/reload.
//  2009/09/23  Clifton Flynn / Martin D. Flynn
//     -Added 'soapXML' argument to various methods.
//  2011/01/28  Martin D. Flynn
//     -Added ability to exclude certain columns on next update.
//  2011/05/13  Martin D. Flynn
//     -Modified "reload" to all specifying specific columns to reload.
//  2011/06/16  Martin D. Flynn
//     -Added "inclBlank" argument on "toXML(...)"
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.io.*;
import java.text.*;
import java.sql.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

public abstract class DBRecord<gDBR extends DBRecord>
{

    // ------------------------------------------------------------------------
    // See DBFactory.java for list of MySQL error codes

    // ------------------------------------------------------------------------

    public  static final int    NOTIFY_GROUP            = 1;
    
    public  static final int    ID_SIZE                 = 32; // default size
    
    // ------------------------------------------------------------------------

    public  static final String PSEUDO_FIELD_CHAR       = "$";
    
    // ------------------------------------------------------------------------

    public  static final String FLD_description         = "description";
    public  static final String FLD_creationTime        = "creationTime";
    public  static final String FLD_creationMillis      = "creationMillis"; // millisecond creation time
    public  static final String FLD_lastUpdateTime      = "lastUpdateTime";
    public  static final String FLD_lastUpdateUser      = "lastUpdateUser";

    // ------------------------------------------------------------------------

    private   DBRecordKey<gDBR>         recordKey               = null;

  //private   DBFieldValues             fieldVals               = null;
    private   boolean                   changed                 = false;
    private   Vector<DBChangeListener>  changeNotification      = null;
    
    private   boolean                   isVirtual               = false;

    protected boolean                   isValidating            = false;
    protected SQLException              lastSQLException        = null;

    protected boolean                   hasError                = false;
    protected String                    errorDescription        = null;
    
    protected Set<String>               excludedUpdateFields    = null;

    /**
    *** Default Constructor
    **/
    public DBRecord()
    {
        super();
    }

    /**
    *** Constructor specifying the DBRecord Key
    *** @param key  The DBRecordKey for this DBRecord
    **/
    protected DBRecord(DBRecordKey<gDBR> key)
    {
        this();
        this.recordKey = key;
        if (this.recordKey != null) {
            this.recordKey._setDBRecord(this); // "unchecked cast"
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the table description
    *** @param loc  The Locale
    *** @return The table description
    **/
    public static String getTableDescription(Locale loc)
    {
        return "";
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Return the DBRecordKey instance for this record
    *** @return THe DBRecordKey instance
    **/
    public DBRecordKey<gDBR> getRecordKey()
    {
        if (this.recordKey == null) {
            // we don't have a record key, so we need to get the table factory to
            // create a record key for us.
            try {
                this.recordKey = this.getFactory(true).createKey(); // may throw DBException
                this.recordKey._setDBRecord(this); // "unchecked cast"
            } catch (DBException dbe) {
                // This should never occur, if it does, it's an implementation error
                dbe.printException();
                return null;
            }
        }
        return this.recordKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the DBFactory instance for the specified DBRecord
    *** @param dbr  The DBRecord instance
    *** @return The DBFactory for the specified DBRecord
    **/
    public static <T extends DBRecord> DBFactory<T> getFactory(DBRecord<T> dbr)
    {
        return (dbr != null)? dbr._getFactory() : null;
    }
    
    // NOTE:
    // "getFactory()" is a static method reserved for subclasses
    // public DBFactory getFactory()  <-- do not create/implemente this method here

    /**
    *** Gets the DBFactory instance for this DBRecord
    *** @param required  True if the DBFactory is required to be defined, in which case
    ***                  this method will throw a DBException if the DBFactory is null.
    *** @return The DBFactory for this DBRecord
    *** @throws DBException if 'required' is true and the DBFactory isn't defined
    **/
    public DBFactory<gDBR> getFactory(boolean required)
        throws DBException
    {
        DBFactory<gDBR> fact = this._getFactory();
        if (required && (fact == null)) {
            throw new DBException("No DBFactory defined for this DBRecord");
        }
        return fact;
    }

    /**
    *** Gets the DBFactory for this DBRecord
    *** @return The DBFactory for this DBRecord
    **/
    @SuppressWarnings("unchecked")
    protected DBFactory<gDBR> _getFactory()
    {
        if (this.recordKey != null) {
            return this.recordKey.getFactory();
        } else {
            try {
                MethodAction methGetFactory = new MethodAction(this.getClass(), "getFactory");
                DBFactory<gDBR> fact = (DBFactory<gDBR>)methGetFactory.invoke(); // "unchecked cast"
                return fact;
            } catch (Throwable t) { // MethodNotFoundException, ...
                Print.logException("Getting table factory [via reflection]", t);
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Example:
    //  Statement stmt = Account.getStatement(Owner.getFactory(), "WHERE (accountID LIKE '%smith%')");
    //  ResultSet rs = stmt.getResultSet();
    //  while (true) {
    //     Account list[] = (Account[])Account.getNextGroup(Account.getFactory(), rs, 10);
    //     if (list.length == 0) { break; }
    //     // do something with 'list'
    //  }
    //  rs.close();
    //  stmt.close();

    /**
    *** This method returns the next 'max' DBRecords from the specified ResultSet
    *** @param fact The DBFactory
    *** @param rs   The ResultSet
    *** @param max  The number of DBRecords to return
    *** @return An array of DBRecord instances
    *** @throws DBException If a database error occurs
    **/
    @SuppressWarnings("unchecked")
    public static <T extends DBRecord> T[] getNextGroup(DBFactory<T> fact, ResultSet rs, int max)
        throws DBException
    {
        java.util.List<T> rcdList = new Vector<T>();

        /* get result set */
        try {
            int cnt = 0;                                   
            while (((max < 0) || (cnt++ < max)) && rs.next()) {
                DBRecordKey<T> rcdKey = fact.createKey(rs); // may throw DBException
                if (rcdKey != null) {
                    T rcd = rcdKey.getDBRecord();
                    rcd.setAllFieldValues(rs);
                    rcdList.add(rcd);
                } else {
                    Print.logError("Unable to create key: " + fact.getUntranslatedTableName());
                }
            }
        } catch (SQLException sqe) {
            //this.setLastCaughtSQLException(sqe);
            throw new DBException("Read next record group", sqe);
        }

        /* convert to array */
        try {
            Class<T> rcdClass = fact.getRecordClass();
            T ra[] = (T[])java.lang.reflect.Array.newInstance(rcdClass, rcdList.size()); // unchecked cast
            return rcdList.toArray(ra); // unchecked cast
        } catch (Throwable t) { // MethodNotFoundException, ...
            throw new DBException("Array conversion", t);
        }

    }
 
    // ------------------------------------------------------------------------

    /**
    *** Gets an array of DBRecords based on the specified 'where' clause
    *** @param fact       The DBFactory
    *** @param where      The Where clause
    *** @param orderBy    The select 'Order By' statement
    *** @param ascending  True to return the records in ascending order
    *** @throws DBException If a DB access error occurs
    **/
    public static <T extends DBRecord> T[] getRecords(DBFactory<T> fact, 
        String where,
        String orderBy[], boolean ascending)
        throws DBException
    {
        return DBRecord.select(fact, where, null, orderBy, ascending, -1L, -1L, null);
    }

    /**
    *** Gets an array of DBRecords based on the specified 'where' clause
    *** @param fact       The DBFactory
    *** @param where      The Where clause
    *** @param addtnlSel  Additional selection criteria
    *** @param orderBy    The select 'Order By' statement
    *** @param ascending  True to return the records in ascending order
    *** @param limit      The maximum number of records to return
    *** @param offset     The the offset within the selected DB records
    *** @return The returned array of DBRecords
    *** @throws DBException If a DB access error occurs
    **/
    public static <T extends DBRecord> T[] getRecords(DBFactory<T> fact, 
        String where, String addtnlSel, 
        String orderBy[], boolean ascending,
        long limit, long offset)
        throws DBException
    {
        return DBRecord.select(fact, where, addtnlSel, orderBy, ascending, limit, offset, null);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets an array of DBRecords based on the specified 'where' clause
    *** @param fact       The DBFactory
    *** @param where      The Where clause
    *** @param addtnlSel  Additional selection criteria
    *** @param orderBy    The select 'Order By' statement
    *** @param ascending  True to return the records in ascending order
    *** @param limit      The maximum number of records to return
    *** @param offset     The the offset within the selected DB records
    *** @param rcdHandler The optional DBRecordHandler
    *** @return The returned array of DBRecords
    *** @throws DBException If a DB access error occurs
    **/
    protected static <T extends DBRecord> T[] select(DBFactory<T> fact, 
        String where, String addtnlSel, 
        String orderBy[], boolean ascending,
        long limit, long offset,
        DBRecordHandler<T> rcdHandler)
        throws DBException
    {

        /* select statement */
        // DBSelect: SELECT * FROM <TableName> <SQLWhere> <AndSQLWhere>
        DBSelect<T> dsel = new DBSelect<T>(fact);
        if (addtnlSel != null) {
            dsel.setWhere(where + " " + addtnlSel);
        } else {
            dsel.setWhere(where);
        }

        /* order by */
        if ((orderBy != null) && (orderBy.length > 0)) {
            dsel.setOrderByFields(orderBy);
            dsel.setOrderAscending(ascending);
        }

        /* limit/offset */
        if (limit > 0L) {
            dsel.setLimit(limit);
        }
        if (offset >= 0L) {
            dsel.setOffset(offset);
        }

        /* return selected records */
        return DBRecord.select(dsel, rcdHandler);

    }

    /**
    *** Gets an array of DBRecords based on the specified 'where' clause
    *** @param dsel       The DBSelect selection criteria
    *** @return The returned array of DBRecords
    *** @throws DBException If a DB access error occurs
    **/
    protected static <T extends DBRecord> T[] select(DBSelect<T> dsel)
        throws DBException
    {
        return DBRecord.select(dsel, null);
    }

    /**
    *** Gets an array of DBRecords based on the specified 'where' clause
    *** @param dsel       The DBSelect selection criteria
    *** @param rcdHandler The optional DBRecordHandler
    *** @return The returned array of DBRecords
    *** @throws DBException If a DB access error occurs
    **/
    protected static <T extends DBRecord> T[] select(DBSelect<T> dsel, 
        DBRecordHandler<T> rcdHandler)
        throws DBException
    {

        /* get result set */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet   rs   = null;
        java.util.List<T> rcdList = new Vector<T>();
        DBFactory<T> fact = dsel.getFactory();

        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            /* extract records from result set */
            while (rs.next()) {
                DBRecordKey<T> rcdKey = fact.createKey(rs); // may throw DBException
                if (rcdKey != null) {
                    T rcd = rcdKey.getDBRecord();
                    rcd.setAllFieldValues(rs);
                    if (rcdHandler != null) {
                        int rcdStatus = rcdHandler.handleDBRecord(rcd);
                        if (rcdStatus == DBRecordHandler.DBRH_STOP) {
                            break;
                        } else 
                        if (rcdStatus == DBRecordHandler.DBRH_SAVE) {
                            rcdList.add(rcd);
                        } else {
                            // skip this record and continue;
                        }
                    } else {
                        rcdList.add(rcd);
                    }
                }
            }
        } catch (SQLException sqe) {
            //this.setLastCaughtSQLException(sqe);
            throw new DBException("Record Selection", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* convert to array */
        if (rcdList != null) {
            try {
                Class<T> rcdClass = fact.getRecordClass();
                //T ra[] = (T[])java.lang.reflect.Array.newInstance(rcdClass, rcdList.size());
                //return (T[])rcdList.toArray(ra);
                return ListTools.toArray(rcdList, rcdClass);
            } catch (Throwable t) { // MethodNotFoundException, ...
                // Implementation error (should never occur)
                throw new DBException("Array conversion", t);
            }
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the number of records contained in the table represented by the specified DBFactory
    *** @param fact The DBFactory instance
    *** @return The number of records contained in the SQL table
    *** @throws DBException If a general DB error occurs
    **/
    public static <T extends DBRecord> long getRecordCount(DBFactory<T> fact)
        throws DBException
    {
        return DBRecord.getRecordCount(fact, "");
    }

    /**
    *** Returns the number of records  contained in the table represented by the specified DBFactory
    *** and based on the specified 'where' clause.
    *** @param fact The DBFactory instance
    *** @param where The 'where' selection clause
    *** @return The number of records contained in the SQL table
    *** @throws DBException If a general DB error occurs
    **/
    public static <T extends DBRecord> long getRecordCount(DBFactory<T> fact, StringBuffer where)
        throws DBException
    {
        return DBRecord.getRecordCount(fact, ((where!=null)?where.toString():null));
    }
    
    /**
    *** Returns the number of records  contained in the table represented by the specified DBFactory
    *** and based on the specified 'where' clause.
    *** @param fact The DBFactory instance
    *** @param where The 'where' selection clause
    *** @return The number of records contained in the SQL table
    *** @throws DBException If a general DB error occurs
    **/
    public static <T extends DBRecord> long getRecordCount(DBFactory<T> fact, String where)
        throws DBException
    {
        
        /* invalid arguments */
        if ((fact == null) || (where == null)) {
            return 0L;
        }

        /* return count */
        return DBRecord.getRecordCount(new DBSelect<T>(fact, where)); // "unchecked call"
        
    }

    /**
    *** Returns the number of records  contained in the table represented by the specified DBFactory
    *** and based on the specified 'where' clause.
    *** @param fact The DBFactory instance
    *** @param where The 'where' selection clause
    *** @return The number of records contained in the SQL table
    *** @throws DBException If a general DB error occurs
    **/
    public static <T extends DBRecord> long getRecordCount(DBFactory<T> fact, DBWhere where)
        throws DBException
    {

        /* invalid arguments */
        if ((fact == null) || (where == null)) {
            return 0L;
        }

        /* return count */
        return DBRecord.getRecordCount(new DBSelect<T>(fact, where)); // "unchecked call"

    }

    /**
    *** Returns the number of records  contained in the table represented by the specified DBSelect 
    *** (which specifies a DBFactory).
    *** @param dsel The DBSelect instance.
    *** @return The number of records contained in the SQL table
    *** @throws DBException If a general DB error occurs
    **/
    public static <T extends DBRecord> long getRecordCount(DBSelect<T> dsel)
        throws DBException
    {
        
        /* valid select? */
        if (dsel == null) {
            return 0L;
        }
        
        /* valid factory? */
        DBFactory<T> fact = dsel.getFactory();
        if (fact == null) {
            return 0L;
        }
        //Print.logInfo("Where: ["+fact.getUntranslatedTableName()+"] " + dsel.getWhere());

        /* we're only interested in 'COUNT(*)' */
        // DBSelect: SELECT COUNT(*) FROM <TableName> <SQLWhere>
        dsel.setSelectedFields(DBProvider.FLD_COUNT());

        /* get result set */
        DBConnection dbc  = null;
        Statement   stmt  = null;
        ResultSet   rs    = null;
        long        count = 0L;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            if (rs.next()) {
                // this only has 1 column
                //count = rs.getLong(DBProvider.FLD_COUNT());
                count = rs.getLong(1); // indexes start at '1'
            }
        } catch (SQLException sqe) {
            // Apache Derby may complain that column DBProvider.FLD_COUNT() doesn't exist
            //this.setLastCaughtSQLException(sqe);
            throw new DBException("Record Count", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return count */
        return count;

    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns true if this record has changed
    *** @return True if this record has changed
    **/
    public boolean hasChanged()
    {
        return this.changed;
    }

    /**
    *** Sets the 'changed' state of this record, and sends a change notification to
    *** any registered listeners.
    *** @param fieldName  The changed field
    **/
    public void setChanged(String fieldName)
    {
        this.changed = true;
        this.fireChangeNotification(fieldName);
    }

    /**
    *** Sets the 'changed' state of this record, and sends a change notification to
    *** any registered listeners.
    *** @param fieldName  The changed field
    *** @param oldVal     The previous value of the field
    *** @param newVal     The new value of the field
    **/
    public void setChanged(String fieldName, Object oldVal, Object newVal)
    {
        // called by "<DBFieldValues>.setFieldValue(...)"
        if (oldVal == newVal) {
            // ignore (has not changed)
        } else
        if ((oldVal == null) || (newVal == null)) {
            this.setChanged(fieldName);
        } else
        if (!oldVal.equals(newVal)) {
            this.setChanged(fieldName);
        }  else {
            //DBRecordKey recKey = this.getRecordKey();
            //Print.logDebug("Field did not change: " + recKey.getUntranslatedTableName() + "." + fieldName);
        }
    }

    /**
    *** Clears the changed state for this record.
    **/
    public void clearChanged()
    {
        this.changed = false;
    }

    /**
    *** Adds a change notification listener to this record
    *** @param cl  The change notification listener to add
    **/
    public void addChangedNotification(DBChangeListener cl)
    {
        if (this.changeNotification == null) { this.changeNotification = new Vector<DBChangeListener>(); }
        if ((cl != null) && !this.changeNotification.contains(cl)) {
            this.changeNotification.add(cl);
        }
    }

    /**
    *** Removes a change notification listener from this record
    *** @param cl  The change notification listener to remove
    **/
    public void removeChangedNotification(DBChangeListener cl)
    {
        if (this.changeNotification != null) {
            this.changeNotification.remove(cl);
        }
    }

    /**
    *** Fires a change notification
    *** @param fieldName  The changed field
    **/
    public void fireChangeNotification(String fieldName)
    {
        if (this.changeNotification != null) {
            for (Iterator<DBChangeListener> i = this.changeNotification.iterator(); i.hasNext();) {
                DBChangeListener dbcr = i.next();
                dbcr.fieldChanged(this, fieldName);
            }
        }
    }

    /**
    *** The change listener interface
    **/
    public static interface DBChangeListener
    {
        public void fieldChanged(DBRecord rcd, String fieldName);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this record has encountered an error
    *** @return True if thid record has encountered an error
    **/
    public boolean hasError()
    {
        return this.hasError;
    }
    
    /**
    *** Clears the error state
    **/
    public void clearError()
    {
        this.hasError = false;
        this.errorDescription = null;
    }
    
    /**
    *** Sets the error state
    **/
    public void setError()
    {
        this.hasError = true;
    }

    /**
    *** Sets the error state with a description
    *** @param desc The description of the error
    **/
    public void setError(String desc)
    {
        this.hasError = true;
        this.errorDescription = desc;
    }
    
    /**
    *** Gets the error state
    *** @return The error state
    **/
    public String getErrorDescription()
    {
        return this.hasError? this.errorDescription : null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Reload the contents of this record from the DB
    *** @return The DBRecord
    **/
    public gDBR reload()
    {
        // returns 'null' if key does not exist, or if a DB error occurred
        try {
            return this._reload(/*null*/);
        } catch (DBException dbe) {
            dbe.printException();
            return null;
        }
    }

    /**
    *** Reload the contents of this record from the DB
    *** @param fldNames  The list of field names to reload (null for all fields)
    *** @return The DBRecord
    **/
    public gDBR reload(String... fldNames)
    {
        // returns 'null' if key does not exist, or if a DB error occurred
        try {
            return this._reload(fldNames);
        } catch (DBException dbe) {
            dbe.printException();
            return null;
        }
    }

    /**
    *** Reload the contents of this record from the DB
    *** @return The DBRecord
    *** @throws DBException If a general DB error occurs
    **/
    @SuppressWarnings("unchecked")
    protected gDBR _reload(String... fldNames)
        throws DBException
    {
        
        /* ok to reload */
        if (!this.isOkToReload()) {
            throw new DBException("Reload not allowed");
        }
        
        /* reload from DB */
        DBConnection dbc  = null;
        Statement    stmt = null;
        ResultSet    rs   = null;
        try {
            DBRecordKey<gDBR> recKey = this.getRecordKey();
            // DBSelect: SELECT * FROM <table> <where>
            DBSelect<gDBR> dsel = new DBSelect<gDBR>(recKey.getFactory());
            if (!ListTools.isEmpty(fldNames)) {
                dsel.setSelectedFields(fldNames);
            }
            String wh = recKey.getWhereClause(DBWhere.KEY_FULL);
            dsel.setWhere(wh);
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            if (rs.next()) {
                if (!ListTools.isEmpty(fldNames)) {
                    this.setAllFieldValues(rs, fldNames); // exclude primary keys
                } else {
                    this.setAllFieldValues(rs); // exclude primary keys
                    this.clearChanged();
                }
                return (gDBR)this;    // "unchecked cast"
            } else {
                // not a fatal error
                Print.logWarn("Key not found: [" + recKey.getUntranslatedTableName() + "] " + wh);
                return null;
            }
        } catch (SQLException sqe) {
            this.setLastCaughtSQLException(sqe);
            throw new DBException("Reload", sqe);
        } finally {
            if (rs   != null) { try{ rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try{ stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Override to set default values
    **/
    public void setCreationDefaultValues()
    {
        // override
    }
    
    /**
    *** Gets the default value keys for current table
    *** @param fieldName The field name
    *** @return The default keys
    **/
    public String[] getDefaultFieldValueKey(String fieldName)
    {
        DBRecordKey<gDBR> recKey = this.getRecordKey();
        String  utableName = recKey.getUntranslatedTableName();
        return new String[] {
            (utableName + ".default." + fieldName),
            (utableName + "." + fieldName)
        };
    }
    
    /**
    *** Override to set default values
    **/
    public void setRuntimeDefaultValues()
    {
        DBRecordKey<gDBR> recKey = this.getRecordKey();
        String  utableName = recKey.getUntranslatedTableName();
        DBField fld[]      = recKey.getFields();
        for (int i = 0; i < fld.length; i++) {
            String fn   = fld[i].getName();
            String dk[] = this.getDefaultFieldValueKey(fn);
            String val  = RTConfig.getString(dk, null);
            if (val != null) {
                if (!fld[i].isPrimaryKey()) { // cannot change primary key
                    this.setFieldValue(fn, fld[i].parseStringValue(val));
                } else {
                    Print.logError("Refusing to set a default value for a primary key field: " + fn);
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the most recent update time for a specified table
    *** @param factory The DBFactory to get the upate time of
    *** @return The field update time, or -1
    *** @throws DBException If a general DB error occurs
    **/
    public static <T extends DBRecord> long getLastUpdateTime(DBFactory<T> factory)
        throws DBException
    {
        
        /* invalid factory? */
        if (factory == null) {
            Print.logStackTrace("NULL DBFactory specified");
            return -1L;
        }
        
        /* check for field "lastUpdateTime" in this factory */
        String fldUpdTime = FLD_lastUpdateTime;
        if (factory.getField(fldUpdTime) == null) {
            //Print.logWarn("Table doesn't contain field: " + factory.getUntranslatedTableName() + "." + fldUpdTime);
            return -1L;
        }
        
        /* order by lastUpdateTime descending */
        // DBSelect: SELECT * FROM <table> ORDER BY lastUpdateTime DESC LIMIT 1
        DBSelect<T> dsel = new DBSelect<T>(factory);
        dsel.setOrderByFields(fldUpdTime);
        dsel.setOrderAscending(false); // descending
        dsel.setLimit(1);
        
        /* read last record */
        DBConnection dbc  = null;
        Statement    stmt = null;
        ResultSet    rs   = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            if (rs.next()) {
                return rs.getLong(fldUpdTime);
            }
        } catch (SQLException sqe) {
            //this.setLastCaughtSQLException(sqe);
            Print.logSQLError("Unable to get '" + fldUpdTime + "': " + factory.getUntranslatedTableName(), sqe);
            return -1L;
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }
        
        return 0L; // no records

    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a new "description" key field definition
    *** @return The "description key field definition
    **/
    protected static DBField newField_description()
    {
        return DBRecord.newField_description(null);
    }

    /**
    *** Creates a new "description" key field definition
    *** @return The "description key field definition
    **/
    protected static DBField newField_description(String xAttr)
    {
        String attr = "edit=2 utf8=true" + (StringTools.isBlank(xAttr)?"":(" " + xAttr));
        return new DBField(FLD_description, String.class, DBField.TYPE_STRING(128), "Description", attr);
    }

    // ----

    /**
    *** Gets the description field value
    *** @return The record description
    **/
    public String getDescription()
    {
        String v = (String)this.getFieldValue(FLD_description);
        return (v != null)? v : "";
    }

    /**
    *** Sets the description field value
    *** @param v The record description
    **/
    public void setDescription(String v)
    {
        this.setFieldValue(FLD_description, ((v != null)? v : ""));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a new creation time field
    *** @return The new creation time field
    **/
    public static DBField newField_creationTime(boolean isAltKey)
    {
        String attr = isAltKey?
            "format=time altkey=createtime" :
            "format=time";
        return new DBField(FLD_creationTime, Long.TYPE, DBField.TYPE_UINT32, "Creation Time", attr);
    }

    /**
    *** Returns a new creation time field
    *** @return The new creation time field
    **/
    public static DBField newField_creationTime()
    {
        return DBRecord.newField_creationTime(false);
    }

    /**
    *** Gets the formatted creation date/time of this record
    *** @return The formatted creation date/time of this record
    **/
    public String getCreationDateTime(TimeZone tz, String fmt)
    {
        long ts = this.getCreationTime();
        if (ts <= 0L) {
            return "";
        } else {
            DateTime dt = new DateTime(ts);
            String dtFmt = dt.format(fmt, tz);
            return dtFmt;
        }
    }

    /**
    *** Gets the creation time of this record
    *** @return The creation time of this record
    **/
    public long getCreationTime()
    {
        Long v = (Long)this.getFieldValue(FLD_creationTime);
        return (v != null)? v.longValue() : -1L;
    }

    /**
    *** Sets the creation time of this record
    *** @param time The creation time
    *** @return True if this record contained a creaton time field to set
    **/
    protected boolean setCreationTime(long time)
    {
        // not all tables will have this field
        if (this.hasField(FLD_creationTime)) {
            long t = (time >= 0L)? time : 0L; // DateTime.getCurrentTimeSec();
            this.setFieldValue(FLD_creationTime, t);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a new millisecond creation time field
    *** @return The new creation time field
    **/
    public static DBField newField_creationMillis(String xAttr)
    {
        String attr = !StringTools.isBlank(xAttr)? xAttr : "";
        return new DBField(FLD_creationMillis, Long.TYPE, DBField.TYPE_INT64, "Creation Time (millis)", attr);
    }

    /**
    *** Gets the millisecond creation time of this record
    *** @return The millisecond creation time of this record
    **/
    public long getCreationMillis()
    {
        Long v = (Long)this.getFieldValue(FLD_creationMillis);
        return (v != null)? v.longValue() : -1L;
    }

    /**
    *** Sets the millisecond creation time of this record
    *** @param millis The millisecond creation time
    *** @return True if this record contained a creaton time field to set
    **/
    protected boolean setCreationMillis(long millis)
    {
        // not all tables will have this field
        if (this.hasField(FLD_creationMillis)) {
            long tms = (millis >= 0L)? millis : 0L; // DateTime.getCurrentTimeMillis();
            this.setFieldValue(FLD_creationMillis, tms);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a new last update time field
    *** @return The last update time field
    **/
    public static DBField newField_lastUpdateTime()
    {
        return new DBField(FLD_lastUpdateTime, Long.TYPE, DBField.TYPE_UINT32, "Last Update Time", "format=time");
    }

    /**
    *** Gets the last update time
    *** @return The last update time
    **/
    public long getLastUpdateTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastUpdateTime);
        return (v != null)? v.longValue() : -1L;
    }

    /**
    *** Sets the last update time of this record
    *** @param time The last update time
    *** @return True if this record contained a last update time field to set
    **/
    protected boolean setLastUpdateTime(long time)
    {
        // not all tables will have this field
        if (this.hasField(FLD_lastUpdateTime)) {
            long t = (time >= 0L)? time : 0L;
            this.setFieldValue(FLD_lastUpdateTime, t);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a new DBField instance for adding a 'lastUpdateUser' field to the DBFactory 
    *** instance at startup initialization time.
    *** @return The DBField instance representing a 'lastUpdateUser' field.
    **/
    public static DBField newField_lastUpdateUser()
    {
        return new DBField(FLD_lastUpdateUser, String.class, DBField.TYPE_STRING(32), "Last Update User", null);
    }

    /**
    *** Returns the last update user, if the 'lastUpdateUser' field has been defined for this DBRecord,
    *** otherwise this method will return null.
    *** @return The last update user, or null if the 'lastUpdateUser' field has not been defined for this DBRecord.
    **/
    public String getLastUpdateUser()
    {
        String v = (String)this.getFieldValue(FLD_lastUpdateUser);
        return (v != null)? v : "";
    }

    /**
    *** Sets the last update user if the 'lastUpdateUser' field has been defined for this DBRecord,
    *** otherwise this method will have no effect.
    *** @param user The last update user
    **/
    public boolean setLastUpdateUser(String user)
    {
        // not all tables will have this field
        if (this.hasField(FLD_lastUpdateUser)) {
            String u = (user != null)? user : "";
            this.setFieldValue(FLD_lastUpdateUser, u);
            return true;
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

    /** 
    *** Insert this DBRecord in the database.<br>
    *** An exception will be throw if the record already exists
    *** @throws DBException if a database error occurs.
    **/
    public void insert()
        throws DBException
    {

        /* save allowed? */
        if (!this.isOkToSave()) {
            throw new DBException("Update not allowed");
        }

        /* insert */
        try {

            /* creation time/user */
            long nowTimeMS = DateTime.getCurrentTimeMillis();
            long nowTime   = nowTimeMS / 1000L;
            this.setCreationMillis(nowTimeMS);
            this.setCreationTime(nowTime);

            /* last update time */
            this.setLastUpdateTime(nowTime);
            this.setLastUpdateUser(DBRecord.GetCurrentUser());

            /* insert */
            this.recordWillInsert();
            DBProvider.insertRecordIntoTable(this);
            this.recordDidInsert();

            /* clear changes */
            this.clearChanged();

        } catch (SQLException sqe) {
            this.setLastCaughtSQLException(sqe);
            DBRecordKey<gDBR> dbKey = this.getRecordKey();
            if (this.isLastCaughtSQLExceptionErrorCode(DBFactory.SQLERR_DUPLICATE_KEY)) {
                //throw new DBException("Duplicate Key '" + dbKey + "'", sqe);
                Print.logInfo("Duplicate Key Skipped: [" + dbKey.getUntranslatedTableName() + "] " + dbKey);
            } else {
                throw new DBException("Unable to insert record  [" + dbKey.getUntranslatedTableName() + "] '" + dbKey + "'", sqe);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Updates all the fields in this DBRecord.
    *** @throws DBException if a database error occurs.
    **/
    public void update()
        throws DBException
    {
        this.update((Set<String>)null);
    }

    /**
    *** Updates the specified fields in this DBRecord.
    *** @param updFldArray  An array of fields to update.
    *** @throws DBException if a database error occurs.
    **/
    public void update(String... updFldArray)
        throws DBException
    {
        if (updFldArray == null) {
            this.update((Set<String>)null);
        } else {
            this.update(ListTools.toSet(updFldArray,null));
        }
    }

    /**
    *** Updates the specified fields in this DBRecord.
    *** @param updFldSet  A Set of fields to update.
    *** @throws DBException if a database error occurs.
    **/
    public void update(Set<String> updFldSet)
        throws DBException
    {

        /* save allowed? */
        if (!this.isOkToSave()) {
            throw new DBException("Update not allowed");
        }
        
        /* update */
        try {

            /* update time/user */
            boolean updTime = this.setLastUpdateTime(DateTime.getCurrentTimeSec());
            boolean updUser = this.setLastUpdateUser(DBRecord.GetCurrentUser());
            if (updFldSet != null) {
                if (updTime) { updFldSet.add(FLD_lastUpdateTime); }
                if (updUser) { updFldSet.add(FLD_lastUpdateUser); }
            }

            /* update */
            this.recordWillUpdate();
            DBProvider.updateRecordInTable(this, updFldSet);
            this.recordDidUpdate();
            
            /* clear changed fields */
            // NOTE: this clears ALL changes, regardless of 'updFldArray'
            this.clearChanged();

        } catch (SQLException sqe) {
            this.setLastCaughtSQLException(sqe);
            DBRecordKey<gDBR> dbKey = this.getRecordKey();
            throw new DBException("Update record '" + dbKey + "'", sqe);
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Clear excluded fields
    **/
    public void clearExcludedUpdateFields()
    {
        this.excludedUpdateFields = null;
    }

    /**
    *** Adds excluded field to list
    *** @param fldNames The field names which are to be excluded on the next update
    **/
    public void addExcludedUpdateFields(String... fldNames)
    {
        if (!ListTools.isEmpty(fldNames)) {
            if (this.excludedUpdateFields == null) {
                this.excludedUpdateFields = new HashSet<String>();
            }
            for (int i = 0; i < fldNames.length; i++) {
                if (!StringTools.isBlank(fldNames[i])) {
                    this.excludedUpdateFields.add(fldNames[i]);
                }
            }
        }
    }

    /**
    *** Returns true if the specified field should be excluded from the next
    *** update.
    *** @param fld  The Field to check
    *** @return True to exclude, False to not-exclude
    **/
    public boolean excludeFieldFromUpdate(DBField fld)
    {
        if (fld == null) {
            return true;
        } else {
            return this.excludeFieldFromUpdate(fld.getName());
        }
    }

    /**
    *** Returns true if the specified field should be excluded from the next
    *** update.
    *** @param fld  The Field to check
    *** @return True to exclude, False to not-exclude
    **/
    public boolean excludeFieldFromUpdate(String fldName)
    {
        if (StringTools.isBlank(fldName)) {
            return true;
        } else 
        if (this.excludedUpdateFields == null) {
            return false;
        } else {
            return this.excludedUpdateFields.contains(fldName);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the "virtual" state
    **/
    public void setVirtual(boolean isVirtual)
    {
        this.isVirtual = isVirtual;
    }

    /**
    *** Gets the "virtual" state
    **/
    public boolean getVirtual()
    {
        return this.isVirtual;
    }

    /**
    *** Gets the "virtual" state
    **/
    public boolean isVirtual()
    {
        return this.isVirtual;
    }

    /**
    *** Returns true if this DBRecord is ok to save
    **/
    public boolean isOkToSave()
    {
        return !this.isVirtual();
    }

    /**
    *** Returns true if this DBRecord is ok to reload
    **/
    public boolean isOkToReload()
    {
        return !this.isVirtual();
    }

    // ------------------------------------------------------------------------

    /** 
    *** Saves (ie. update an existing record, or inserts a new record) all fields in this DBRecord
    *** @throws DBException If a database error occurs
    **/
    public void save()
        throws DBException
    {
        DBRecordKey<gDBR> dbKey = this.getRecordKey();
        if (dbKey.exists()) {   // may throw DBException
            this.update();      // may throw DBException
        } else {
            this.insert();      // may throw DBException
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Lock table associated with this DBRecord for writing
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    *** @see DBProvider#lockTables
    **/
    public boolean lockWrite()
        throws DBException
    {
        return this.lock(new String[] { this.getFactory(true).getUntranslatedTableName() }, null);
    }

    /**
    *** Lock table associated with this DBRecord for reading
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    *** @see DBProvider#lockTables
    **/
    public boolean lockRead()
        throws DBException
    {
        // write locks are always included
        return this.lock(null, new String[] { this.getFactory(true).getUntranslatedTableName() });
    }

    /**
    *** Lock specified tables for write/read
    *** @param writeTables The array of tables to lock for writing. If null,
    ***        defaults to the table assocated with this DBRecord
    *** @param readTables The array of tables to lock for reading
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    *** @see DBProvider#lockTables
    **/
    public boolean lock(String writeTables[], String readTables[])
        throws DBException
    {
        if (writeTables == null) {
            writeTables = new String[] { this.getFactory(true).getUntranslatedTableName() };
        }
        return DBProvider.lockTables(writeTables, readTables);
    }

    /**
    *** Unlock locked tables
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    *** @see DBProvider#unlockTables
    **/
    public boolean unlock()
        throws DBException
    {
        return DBProvider.unlockTables();
    }

    // ------------------------------------------------------------------------

    //protected static Statement getStatement(DBFactory fact, String where)
    //    throws SQLException, DBException
    //{
    //    return DBRecord.getStatement(fact, where, false);
    //}

    //protected static Statement getStatement(DBFactory fact, String where, boolean rowByRow)
    //    throws SQLException, DBException
    //{
    //    // DBSelect: SELECT * FROM <TableName> <SQLWhere>
    //    DBSelect dsel = new DBSelect(fact);
    //    dsel.setWhere(where);
    //    return DBRecord.execute(dsel.toString(), rowByRow);
    //    // Note: this returned Statement must be closed when finished
    //}

    // ------------------------------------------------------------------------

    /**
    *** Executes a sql query
    *** @param sql The query to execute
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    //protected static Statement execute(String sql)
    //    throws SQLException, DBException
    //{
    //    Statement stmt = null;
    //    DBConnection dbc = null;
    //    try {
    //        dbc  = DBConnection.getDefaultConnection();
    //        stmt = dbc.execute(sql);
    //    } finally {
    //        DBConnection.release(dbc);
    //    }
    //    // WARNING: This DBConnection may not be available until the statement is closed!
    //    return stmt;
    //}

    /**
    *** Executes a sql query
    *** @param sql The query to execute
    *** @param rowByRow  True to create a new Statement in row-by-row mode
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    //protected static Statement execute(String sql, boolean rowByRow)
    //    throws SQLException, DBException
    //{
    //    Statement stmt = null;
    //    DBConnection dbc = null;
    //    try {
    //        dbc  = DBConnection.getDefaultConnection();
    //        stmt = dbc.execute(sql, rowByRow);
    //    } finally {
    //        DBConnection.release(dbc);
    //    }
    //    // WARNING: This DBConnection may not be available until the statement is closed!
    //    return stmt;
    //}

    /**
    *** Execute the specified SQL update
    *** @param sql  The String SQL statement to execute
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    //protected static void executeUpdate(String sql)
    //    throws SQLException, DBException
    //{
    //    DBConnection dbc = null;
    //    try {
    //        dbc  = DBConnection.getDefaultConnection();
    //        dbc.executeUpdate(sql);
    //    } finally {
    //        DBConnection.release(dbc);
    //    }
    //}

    //protected static long executeUpdate(String sql, boolean rtnAutoIncrVal)
    //    throws SQLException, DBException
    //{
    //    long autoIncr = -1L;
    //    DBConnection dbc = null;
    //    try {
    //        dbc  = DBConnection.getDefaultConnection();
    //        autoIncr = dbc.executeUpdate(sql, rtnAutoIncrVal);
    //    } finally {
    //        DBConnection.release(dbc);
    //    }
    //    return autoIncr;
    //}

    // ------------------------------------------------------------------------

    /**
    *** Returns a case sensitive field name for the specified case insensitive field name.
    *** @param fldName  A case-insensitive field name
    *** @return A case sensitive field name
    **/
    public String getFieldName(String fldName)
    {
        return this.getRecordKey().getFieldValues().getFieldName(fldName);
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Sets the state for ignoring invalid field names.  True to ignore errors when
    *** setting/getting a field name that does not exist, False to emit any invalid
    *** field errors.
    *** @param state  True to ignore invalid field names, false to emit errors.
    **/
    public void setIgnoreInvalidFields(boolean state)
    {
        this.getRecordKey().getFieldValues().setIgnoreInvalidFields(state);
    }

    /**
    *** Gets the DBField with the specified name, or null if the specified field name
    *** does not exist.
    *** @param fldName The field name
    *** @return The DBField.
    **/
    public DBField getField(String fldName)
    {
        return this.getRecordKey().getField(fldName); // may return null if field doesn't exist
    }

    /**
    *** Returns true if a defined field with the specified name exists for this DBRecord
    *** @return True if a defined field with the specified name exists, false otherwise.
    **/
    public boolean hasField(String fldName)
    {
        // if true, the field is defined
        return this.getRecordKey().getFieldValues().hasField(fldName);
    }

    /**
    *** Returns true if the specified field name has a defined value
    *** @return True if the specified field name has a defined value.
    **/
    public boolean hasFieldValue(String fldName)
    {
        // if true, the field, and its value, are defined
        return this.getRecordKey().getFieldValues().hasFieldValue(fldName);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field name.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The field value
    **/
    public Object getOptionalFieldValue(String fldName)
    {
        return this.getRecordKey().getFieldValues().getOptionalFieldValue(fldName);
    }

    /**
    *** Gets the value for the specified optional field name.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The field value
    **/
    public Object getOptionalFieldValue(String fldName, Object dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        return (obj != null)? obj : dft;
    }

    /**
    *** Sets the value for the specified optional field name.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setOptionalFieldValue(String fldName, Object value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The field value
    **/
    public Object getFieldValue(String fldName)
    {
        return this.getRecordKey().getFieldValues().getFieldValue(fldName);
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public Object getFieldValue(String fldName, Object dft)
    {
        Object obj = this.getFieldValue(fldName);
        return (obj != null)? obj : dft;
    }

    /**
    *** Sets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    *** @return True if the field exists and was successfully set.
    **/
    public boolean setFieldValue(String fldName, Object value)
    {
        return this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    }

    /**
    *** Gets the value for the specified field.<br>
    *** @param fldName  The field name to retrieve
    *** @return The field value
    **/
    public Object getValue(String fldName)
    {
        DBField fld = this.getField(fldName);
        if (fld != null) {
            String meth = MethodAction.getterMethodName(fldName);
            // try DBRecord 'getter'
            try {
                return (new MethodAction(this,meth,(Class[])null)).invoke();
            } catch (Throwable th) {
                // main record does not define a 'getter'
            }
            // try DBRecord extension
            /*
            DBFactory<gDBR> fact = this._getFactory();
            Object ext = (fact != null)? fact.getRecordExtension() : null;
            if (ext != null) {
                try {
                    return (new MethodAction(ext,meth,fact.getRecordClass())).invoke();
                } catch (Throwable th) {
                    // extension does not define a 'getter'
                }
            }
            */
            // default to generic 'getFieldName'
            return this.getFieldValue(fldName);
        } else {
            // field not found
            return null;
        }
    }

    /**
    *** Sets the value for the specified field.<br>
    *** This method attempts to use the field 'setter' method for setting the field value.
    *** If the field 'setter' method does not exist, then the generic 'setFieldValue' method
    *** will be used.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setValue(String fldName, Object value)
    {
        DBField fld = this.getField(fldName);
        if (fld != null) {
            String meth = MethodAction.setterMethodName(fldName);
            // try DBRecord 'setter'
            try {
                MethodAction m = new MethodAction(this,meth,fld.getTypeClass());
                m.invoke(value);
                return;
            } catch (Throwable th) {
                // main record does not define a 'setter'
            }
            // try DBRecord extension
            /*
            DBFactory<gDBR> fact = this._getFactory();
            Object ext = (fact != null)? fact.getRecordExtension() : null;
            if (ext != null) {
                try {
                    MethodAction m = new MethodAction(ext,meth,fact.getRecordClass(),fld.getTypeClass());
                    m.invoke(this, value);
                    return;
                } catch (Throwable th) {
                    // extension does not define a 'setter'
                }
            }
            */
            // default to generic 'setFieldName'
            this.setFieldValue(fldName, value);
        } else {
            // field not found
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public String getOptionalFieldValue(String fldName, String dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        return StringTools.trim((obj != null)? obj.toString() : dft);
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public String getFieldValue(String fldName, String dft)
    {
        Object obj = this.getFieldValue(fldName);
        return StringTools.trim((obj != null)? obj.toString() : dft);
    }

    /**
    *** Gets the String value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The String field value
    **/
    public String getFieldString(String fldName)
    {
        return this.getFieldValue(fldName, "");
    }

    /**
    *** Sets the String value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setOptionalFieldValue(String fldName, String value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    /**
    *** Sets the String value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setFieldValue(String fldName, String value)
    {
        this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    }

    /**
    *** Sets the String value for the specified field.<br>
    *** This method attempts to use the field 'setter' method for setting the field value.
    *** If the field 'setter' method does not exist, then the generic 'setFieldValue' method
    *** will be used.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setValue(String fldName, String value)
    {
        this.setValue(fldName, (Object)((value != null)? value : ""));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public boolean getOptionalFieldValue(String fldName, boolean dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        if (obj instanceof Boolean) {
            return ((Boolean)obj).booleanValue();
        } else 
        if (obj instanceof Number) {
            return (((Number)obj).intValue() != 0)? true : false;
        } else {
            return dft;
        }
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public boolean getFieldValue(String fldName, boolean dft)
    {
        Object obj = this.getFieldValue(fldName);
        if (obj instanceof Boolean) {
            return ((Boolean)obj).booleanValue();
        } else 
        if (obj instanceof Number) {
            return (((Number)obj).intValue() != 0)? true : false;
        } else {
            return dft;
        }
    }

    /**
    *** Gets the boolean value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The boolean field value
    **/
    public boolean getFieldBoolean(String fldName)
    {
        return this.getFieldValue(fldName, false);
    }

    /**
    *** Sets the boolean value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setOptionalFieldValue(String fldName, boolean value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    /**
    *** Sets the boolean value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setFieldValue(String fldName, boolean value)
    {
        this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    }

    /**
    *** Sets the value for the specified field.<br>
    *** This method attempts to use the field 'setter' method for setting the field value.
    *** If the field 'setter' method does not exist, then the generic 'setFieldValue' method
    *** will be used.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setValue(String fldName, boolean value)
    {
        this.setValue(fldName, new Boolean(value));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public int getOptionalFieldValue(String fldName, int dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).intValue() : dft;
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public int getFieldValue(String fldName, int dft)
    {
        Object obj = this.getFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).intValue() : dft;
    }

    /**
    *** Gets the int value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The int field value
    **/
    public int getFieldInt(String fldName)
    {
        return this.getFieldValue(fldName, 0);
    }

    /**
    *** Sets the int value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setOptionalFieldValue(String fldName, int value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    /**
    *** Sets the int value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setFieldValue(String fldName, int value)
    {
        this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    }

    /**
    *** Sets the value for the specified field.<br>
    *** This method attempts to use the field 'setter' method for setting the field value.
    *** If the field 'setter' method does not exist, then the generic 'setFieldValue' method
    *** will be used.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setValue(String fldName, int value)
    {
        this.setValue(fldName, new Integer(value));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public long getOptionalFieldValue(String fldName, long dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).longValue() : dft;
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public long getFieldValue(String fldName, long dft)
    {
        Object obj = this.getFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).longValue() : dft;
    }

    /**
    *** Gets the long value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The long field value
    **/
    public long getFieldLong(String fldName)
    {
        return this.getFieldValue(fldName, 0L);
    }

    /**
    *** Sets the long value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setOptionalFieldValue(String fldName, long value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    /**
    *** Sets the long value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setFieldValue(String fldName, long value)
    {
        this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    }

    /**
    *** Sets the value for the specified field.<br>
    *** This method attempts to use the field 'setter' method for setting the field value.
    *** If the field 'setter' method does not exist, then the generic 'setFieldValue' method
    *** will be used.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setValue(String fldName, long value)
    {
        this.setValue(fldName, new Long(value));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public float getOptionalFieldValue(String fldName, float dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).floatValue() : dft;
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public float getFieldValue(String fldName, float dft)
    {
        Object obj = this.getFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).floatValue() : dft;
    }

    /**
    *** Gets the float value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The float field value
    **/
    public float getFieldFloat(String fldName)
    {
        return this.getFieldValue(fldName, 0.0F);
    }
    
    /**
    *** Sets the float value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setOptionalFieldValue(String fldName, float value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    /**
    *** Sets the float value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setFieldValue(String fldName, float value)
    {
        this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    }

    /**
    *** Sets the value for the specified field.<br>
    *** This method attempts to use the field 'setter' method for setting the field value.
    *** If the field 'setter' method does not exist, then the generic 'setFieldValue' method
    *** will be used.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setValue(String fldName, float value)
    {
        this.setValue(fldName, new Float(value));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public double getOptionalFieldValue(String fldName, double dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).doubleValue() : dft;
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public double getFieldValue(String fldName, double dft)
    {
        Object obj = this.getFieldValue(fldName);
        return (obj instanceof Number)? ((Number)obj).doubleValue() : dft;
    }

    /**
    *** Gets the double value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The double field value
    **/
    public double getFieldDouble(String fldName)
    {
        return this.getFieldValue(fldName, 0.0);
    }
    
    /**
    *** Sets the double value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setOptionalFieldValue(String fldName, double value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    /**
    *** Sets the double value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setFieldValue(String fldName, double value)
    {
        this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    }

    /**
    *** Sets the value for the specified field.<br>
    *** This method attempts to use the field 'setter' method for setting the field value.
    *** If the field 'setter' method does not exist, then the generic 'setFieldValue' method
    *** will be used.
    *** @param fldName  The field name to set
    *** @param value    The value to set.
    **/
    public void setValue(String fldName, double value)
    {
        this.setValue(fldName, new Double(value));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public DateTime getOptionalFieldValue(String fldName, DateTime dft)
    {
        Object obj = this.getOptionalFieldValue(fldName);
        if (obj instanceof DateTime) {
            return (DateTime)obj;
        } else
        if (obj instanceof Long) {
            TimeZone tmz = (dft != null)? dft.getTimeZone() : DateTime.getGMTTimeZone();
            return new DateTime(((Long)obj).longValue(),tmz);
        } else {
            return dft;
        }
    }

    /**
    *** Gets the value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @param dft      The default value returned if the field does not exist, or has not been initialized.
    *** @return The field value
    **/
    public DateTime getFieldValue(String fldName, DateTime dft)
    {
        Object obj = this.getFieldValue(fldName);
        if (obj instanceof DateTime) {
            return (DateTime)obj;
        } else
        if (obj instanceof Long) {
            TimeZone tmz = (dft != null)? dft.getTimeZone() : DateTime.getGMTTimeZone();
            return new DateTime(((Long)obj).longValue(),tmz);
        } else {
            return dft;
        }
    }

    /**
    *** Gets the DateTime value for the specified field.<br>
    *** Note: This function bypasses the normal 'getter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to retrieve
    *** @return The DateTime field value
    **/
    public DateTime getFieldDateTime(String fldName)
    {
        return this.getFieldValue(fldName, (DateTime)null);
    }
    
    /**
    *** Sets the DateTime value for the specified optional field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    public void setOptionalFieldValue(String fldName, DateTime value)
    {
        this.getRecordKey().getFieldValues().setOptionalFieldValue(fldName, value);
    }

    /**
    *** Sets the DateTime value for the specified field.<br>
    *** Note: This function bypasses the normal 'setter' methods for the specific DBRecord subclass,
    *** and thus may not perform some of the bounds checking for the specific field.
    *** @param fldName  The field name to set
    *** @param value    The value to set
    **/
    //public void setFieldValue(String fldName, DateTime value)
    //{
    //    this.getRecordKey().getFieldValues().setFieldValue(fldName, value);
    //}

    // ------------------------------------------------------------------------

    /**
    *** Sets the field values for this DBRecord from the specified DBRecord <br>
    *** (primary keys are not copied)
    *** @param rcd The other DBRecord instance from which field values are copied
    *** @throws DBException   If a database error occurs
    **/
    public void setAllFieldValues(DBRecord<gDBR> rcd) 
        throws DBException
    {
        if (rcd != null) {
            DBFieldValues thisFldVals = this.getRecordKey().getFieldValues();
            DBFieldValues rcdFldVals  = rcd.getRecordKey().getFieldValues();
            thisFldVals.clearFieldValues(); // does not clear the primary key
            thisFldVals.setFieldValues(rcdFldVals,false/*noPrimaryKey*/,false/*notRequired*/);
        } else {
            // quietly ignore
        }
    }

    /**
    *** Sets the field values for this DBRecord from the specified SQL ResultSet <br>
    *** (primary keys are not copied)
    *** @param rs The SQL ResultSet
    *** @throws DBException   If a database error occurs
    **/
    public void setAllFieldValues(ResultSet rs) 
        throws DBException
    {
        if (rs != null) {
            try {
                DBFieldValues fldVals = this.getRecordKey().getFieldValues();
                fldVals.clearFieldValues(); // does not clear the primary key
                fldVals.setAllFieldValues(rs, false); // should not reset the primary key fields
            } catch (SQLException sqe) {
                this.setLastCaughtSQLException(sqe);
                throw new DBException("Setting field values", sqe);
            }
        } else {
            // quietly ignore
        }
    }

    /**
    *** Sets the field values for this DBRecord from the specified SQL ResultSet <br>
    *** (primary keys are not copied)
    *** @param rs   The SQL ResultSet
    *** @param fldNames  The list of field names to set (null for all fields)
    *** @throws DBException   If a database error occurs
    **/
    public void setAllFieldValues(ResultSet rs, String... fldNames) 
        throws DBException
    {
        if (rs != null) {
            try {
                DBFieldValues fldVals = this.getRecordKey().getFieldValues();
                if (!ListTools.isEmpty(fldNames)) {
                    DBField fldList[] = this._getFactory().getFields(fldNames);
                    fldVals.clearFieldValues(fldList);      // does not clear the primary key
                    fldVals.setAllFieldValues(rs, false, fldList); // should not reset the primary key fields
                } else {
                    fldVals.clearFieldValues();             // does not clear the primary key
                    fldVals.setAllFieldValues(rs, false);   // should not reset the primary key fields
                }
            } catch (SQLException sqe) {
                this.setLastCaughtSQLException(sqe);
                throw new DBException("Setting field values", sqe);
            }
        } else {
            // quietly ignore
        }
    }

    /**
    *** Sets the field values for this DBRecord from the specified Map.<br>
    *** Field values are converted to their proper type from the specified Map values.<br>
    *** All current field values are cleared (except primary key values).
    *** @param valMap  The Field==>Value map
    *** @throws DBException   If a database error occurs
    **/
    public void setAllFieldValues(Map<String,String> valMap) 
        throws DBException
    {
        if (valMap != null) {
            DBFieldValues fldVals = this.getRecordKey().getFieldValues();
            fldVals.clearFieldValues(); // does not clear the primary key
            fldVals.setAllFieldValues(valMap, false/*setKeyFields*/);
        } else {
            // quietly ignore
        }
    }

    /**
    *** Appends the field values for this DBRecord from the specified Map.<br>
    *** Field values are converted to their proper type from the specified Map values.<br>
    *** Similar to "setAllFieldValues", however the current field values are not cleared.
    *** @param valMap  The Field==>Value map
    *** @throws DBException   If a database error occurs
    **/
    public void appendFieldValues(Map<String,String> valMap) 
        throws DBException
    {
        if (valMap != null) {
            DBFieldValues fldVals = this.getRecordKey().getFieldValues();
            fldVals.setFieldValues(valMap, false/*setKeyFields*/, false/*requireAllFields*/);
        } else {
            // quietly ignore
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the last caught SQLException
    *** @param sqe The last caught SQL Exception
    **/
    protected void setLastCaughtSQLException(SQLException sqe)
    {
        this.lastSQLException = sqe;
    }
    
    /**
    *** Clears the last caught SQLException
    **/
    public void clearLastCaughtSQLException()
    {
        this.setLastCaughtSQLException(null);
    }
    
    /**
    *** Gets the last caught SQLException
    *** @return The last caught SQLException
    **/
    public SQLException getLastCaughtSQLException()
    {
        return this.lastSQLException;
    }
    
    /**
    *** Returns true if the specified <code>code</code> matches the error code 
    *** of the last caught SQLException
    *** @param code The code to compare with the last SQLException error code
    *** @return True if <code>code</code> matches the error code of the last
    ***         caught SQLException
    **/
    public boolean isLastCaughtSQLExceptionErrorCode(int code)
    {
        SQLException sqe = this.getLastCaughtSQLException();
        if (sqe == null) {
            return false;
        } else
        if (sqe.getErrorCode() == code) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets if this record id validating [CHECK]
    *** @param validate If this record is validating
    **/
    protected void setValidating(boolean validate)
    {
        this.isValidating = validate;
    }
    
    /**
    *** Returns true if this record is validating
    *** @return True if this record is validating
    **/
    protected boolean isValidating()
    {
        return this.isValidating;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified DBRecord key is equivalent to this record key
    *** @param obj The specified DBRecord key
    *** @return True if <code>obj</code> is equivilent to this record key
    **/
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBRecord)) {
            return false;
        } else
        if (this.getClass().isAssignableFrom(obj.getClass())) {
            return ((DBRecord)obj).getRecordKey().equals(this.getRecordKey());
        } else {
            return false;
        }
    }

    /**
    *** Returns a String representation of the DBRecordKey for this DBRecord
    *** @return A String representation of the DBRecordKey for this DBRecord
    **/
    public String toString() // what the user sees
    {
        return this.getRecordKey().toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Encodes all field of the specified DBRecords into XML and writes it to
    *** a specified PrintWriter
    *** @param out The PrintWriter 
    *** @param dbr The list of DBRecords
    **/
    public static void printXML(PrintWriter out, 
        DBRecord<?>... dbr)
    {
        DBRecord.printXML(out, 0, null, dbr);
    }

    /**
    *** Encodes the specified DBRecords into XML and writes it to a specified 
    *** PrintWriter
    *** @param out The PrintWriter 
    *** @param indent The number of spaces to indent
    *** @param fldNames The set of field names to include
    *** @param dbr The list of DBRecords
    **/
    public static void printXML(PrintWriter out, int indent, 
        Set<String> fldNames, DBRecord<?>... dbr)
    {
        if (out != null) {
            out.write("<"+DBFactory.TAG_Records+">\n");
            if (dbr != null) {
                for (int i = 0; i < dbr.length; i++) {
                    dbr[i].printXML(out, indent, fldNames, i+1);
                }
            }
            out.write("</"+DBFactory.TAG_Records+">\n");
            //out.flush();
        }
    }

    /**
    *** Encodes this DBRecord into XML and writes it to a specified PrintWriter
    *** @param out The PrintWriter 
    *** @param indent The number of spaces to indent
    *** @param fldNames The set of field names to include
    **/
    public void printXML(PrintWriter out, int indent, 
        Set<String> fldNames)
    {
        this.printXML(out, indent, fldNames, -1, false);
    }

    /**
     * Contains boolean value used to encode xml that will be embedded within a SOAP envelope.
     * @param out
     * @param indent
     * @param fldNames
     * @param soapXml
     */    
    public void printXML(PrintWriter out, int indent, 
        Set<String> fldNames, boolean soapXml)
    {
        this.printXML(out, indent, fldNames, -1, soapXml);
    }

    /**
    *** Encodes this DBRecord into XML and writes it to a specified PrintWriter
    *** @param out      The PrintWriter 
    *** @param indent   The number of spaces to indent
    *** @param fldNames The set of field names to include
    *** @param sequence Optional sequence value
    **/
    public void printXML(PrintWriter out, int indent, 
        Set<String> fldNames, int sequence)
    {
        if (out != null) {
            out.write(this.toXML(null,indent,fldNames,sequence).toString());
            //out.flush();
        }
    }

    /**
    *** Encodes this DBRecord into XML and writes it to a specified PrintWriter
    *** @param out      The PrintWriter 
    *** @param indent   The number of spaces to indent
    *** @param fldNames The set of field names to include
    *** @param sequence Optional sequence value
    *** @param soapXML  True is SOAP XML
    **/
    public void printXML(PrintWriter out, int indent, 
        Set<String> fldNames, int sequence, 
        boolean soapXML)
    {
        if (out != null) {
            out.write(this.toXML(null,indent,fldNames,sequence,soapXML).toString());
            //out.flush();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Encodes this DBRecord into XML
    *** @param sb       The StringBuffer to write the DBRecord XML to
    *** @param indent   The number of spaces to indent
    *** @param fldNames The set of field names to include
    *** @return The StringBuffer
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, 
        Set<String> fldNames)
    {
        return this.toXML(sb, indent, 
            fldNames, -1/*sequence*/);
    }

    /**
    *** Encodes this DBRecord into XML
    *** @param sb        The StringBuffer to write the DBRecord XML to
    *** @param indent    The number of spaces to indent
    *** @param fldNames  The set of field names to include
    *** @param soapXML   True if SOAP XML
    *** @return The StringBuffer containing the XML
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, 
        Set<String> fldNames, boolean soapXML)
    {
        return this.toXML(sb, indent, 
            fldNames, -1/*sequence*/, true/*inclBlank*/,
            soapXML);
    }

    /**
    *** Encodes this DBRecord into XML
    *** @param sb        The StringBuffer to write the DBRecord XML to
    *** @param indent    The number of spaces to indent
    *** @param fldNames  The set of field names to include
    *** @param sequence  Optional sequence value
    *** @return The StringBuffer containing the XML
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, 
        Set<String> fldNames, int sequence)
    {
        return this.toXML(sb, indent, 
            fldNames, sequence, true/*inclBlank*/,
            false/*soapXML*/);
    }

    /**
    *** Encodes this DBRecord into XML
    *** @param sb        The StringBuffer to write the DBRecord XML to
    *** @param indent    The number of spaces to indent
    *** @param fldNames  The set of field names to include
    *** @param sequence  Optional sequence value
    *** @param soapXML   True if SOAP XML
    *** @return The StringBuffer containing the XML
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, 
        Set<String> fldNames, int sequence, 
        boolean soapXML)
    {
        return this.toXML(sb, indent, 
            fldNames, sequence, true/*inclBlank*/,
            false/*soapXML*/);
    }
    
    /**
    *** Encodes this DBRecord into XML
    *** @param sb        The StringBuffer to write the DBRecord XML to
    *** @param indent    The number of spaces to indent
    *** @param fldNames  The set of field names to include
    *** @param sequence  Optional sequence value
    *** @param inclBlank Include blank fields
    *** @param soapXML   True if SOAP XML
    *** @return The StringBuffer containing the XML
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, 
        Set<String> fldNames, int sequence, boolean inclBlank,
        boolean soapXML)
    {
        if (sb == null) { sb = new StringBuffer(); }
        DBRecordKey<gDBR> recKey     = this.getRecordKey();
        String            utableName = recKey.getUntranslatedTableName();
        DBField           fld[]      = recKey.getFields();       // ALL fields
        DBFieldValues     fldVals    = recKey.getFieldValues();
        String            PFX1       = XMLTools.PREFIX(soapXML,indent);

        /* begin Record tag */
        sb.append(PFX1);
        sb.append(XMLTools.startTAG(soapXML, DBFactory.TAG_Record,
            XMLTools.ATTR(DBFactory.ATTR_table,utableName) +
            (!ListTools.isEmpty(fldNames)? XMLTools.ATTR(DBFactory.ATTR_partial,"true") : "") +
            ((sequence > 0)? XMLTools.ATTR(DBFactory.ATTR_sequence,sequence) : ""),
            false,true));

        /* first all primary keys */
        for (int i = 0; i < fld.length; i++) {
            if (fld[i].isPrimaryKey()) {
                String value = fldVals.getFieldValueAsString(fld[i].getName());
                if (inclBlank || !StringTools.isBlank(value)) {
                    DBFactory.writeXML_DBField(sb, 2*indent, fld[i], false/*inclInfo*/, value, soapXML);
                }
            }
        }

        /* then all non-primary key fields */            
        for (int i = 0; i < fld.length; i++) {
            if (!fld[i].isPrimaryKey()) {
                String name = fld[i].getName();
                if ((fldNames == null) || fldNames.contains(name)) {
                    String value = fldVals.getFieldValueAsString(name);
                    DBFactory.writeXML_DBField(sb, 2*indent, fld[i], false/*inclInfo*/, value, soapXML);
                }
            }
        }

        /* end Record tag */
        sb.append(PFX1);
        sb.append(XMLTools.endTAG(soapXML,DBFactory.TAG_Record,true));
        return sb;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback when record is about to be inserted into the table
    **/
    @SuppressWarnings("unchecked")
    protected void recordWillInsert()
    {
        DBFactory<gDBR> fact = this._getFactory();
        if (fact != null) {
            fact.recordWillInsert((gDBR)this); // unchecked cast
        }
    }

    /**
    *** Callback after record has been be inserted into the table
    **/
    @SuppressWarnings("unchecked")
    protected void recordDidInsert()
    {
        DBFactory<gDBR> fact = this._getFactory();
        if (fact != null) {
            fact.recordDidInsert((gDBR)this); // unchecked cast
        }
    }

    /**
    *** Callback when record is about to be updated in the table
    **/
    @SuppressWarnings("unchecked")
    protected void recordWillUpdate()
    {
        DBFactory<gDBR> fact = this._getFactory();
        if (fact != null) {
            fact.recordWillUpdate((gDBR)this); // unchecked cast
        }
    }

    /**
    *** Callback after record has been be updated in the table
    **/
    @SuppressWarnings("unchecked")
    protected void recordDidUpdate()
    {
        DBFactory<gDBR> fact = this._getFactory();
        if (fact != null) {
            fact.recordDidUpdate((gDBR)this); // unchecked cast
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String currentUser = "";

    /**
    *** Gets the current user
    *** @return The current user
    **/
    public static String GetCurrentUser()
    {
        if (currentUser == null) { currentUser = ""; }
        return currentUser;
    }

    /**
    *** Sets the current user
    *** @param user The current user
    **/
    public static void SetCurrentUser(String user)
    {
        currentUser = (user != null)? user : "";
    }

    // ------------------------------------------------------------------------

    /* package */ static <T extends DBRecord> T _createDBRecord(DBRecordKey<T> rcdKey)
        throws DBException
    {
        DBFactory<T> factory = rcdKey.getFactory();
        Class<T> dbrClass = factory.getRecordClass();
        Class dbkClass = factory.getKeyClass();
        try {
            Constructor<T> con = dbrClass.getConstructor(new Class[] { dbkClass });
            return con.newInstance(new Object[] { rcdKey }); // "unchecked cast"
        } catch (Throwable t) { // NoSuchMethodException, ...
            // Implementation error (this should never occur)
            throw new DBException("Unable to create DBRecord", t);
        }
    }
    
    // ------------------------------------------------------------------------

}
