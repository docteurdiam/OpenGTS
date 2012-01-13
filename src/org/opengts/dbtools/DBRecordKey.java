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
//  2006/04/02  Martin D. Flynn
//     -Added "getField(String)"
//  2006/04/09  Martin D. Flynn
//     -Integrate DBException
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/09/16  Martin D. Flynn
//     -Integrated DBWhere, DBSelect, DBDelete
//     -Added argument to 'getWhereClause' to allow enforcing a full-key lookup.
//  2008/02/04  Martin D. Flynn
//     -Change DBRecordKey constructor to set this DBRecord in the record key.
//  2008/02/27  Martin D. Flynn
//     -Added 'hasFieldValue' method
//  2008/03/28  Martin D. Flynn
//     -Added existance and delete methods that operate on the alternate key index
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
//  2009/01/01  Martin D. Flynn
//     -Changed 'getWhereClause' to accept an 'int' (rather than boolean) to allow
//      for 2 types of partial key specifications.  This also fixes a dependency 
//      delete issue that would potentially delete improper partial keys.
//  2009/09/23  Clifton Flynn / Martin D. Flynn
//     -Added 'soapXML' argument to various methods.
//  2009/11/01  Martin D. Flynn
//     -Added support for 'autoIndex' field
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

/**
*** <code>DBRecordKey</code> represents the SQL table key for a DBRecord.
**/

public abstract class DBRecordKey<gDBR extends DBRecord>
{

    // ------------------------------------------------------------------------

    //public  static boolean WHERE_FULL_KEY_REQUIRED      = true;
    //public  static boolean WHERE_PARTIAL_KEY_OK         = !WHERE_FULL_KEY_REQUIRED;
    
    public static final String  FLD_autoIndex   = "autoIndex";

    // ------------------------------------------------------------------------

    private DBFieldValues       fieldValues     = null;
    private gDBR                record          = null;
    private Set<String>         taggedFields    = null;

    // ------------------------------------------------------------------------

    /**
    *** Default Constructor
    **/
    protected DBRecordKey()
    {
        super();
    }

    /**
    *** Gets the DBFactory for this DBRecoedKey
    **/
    public abstract DBFactory<gDBR> getFactory();

    // ------------------------------------------------------------------------
    // DBFactory convience methods
    
    /**
    *** @see DBFactory#getUntranslatedTableName()
    **/
    public String getUntranslatedTableName()
    {
        return this.getFactory().getUntranslatedTableName();
    }

    /**
    *** Gets the table name for this DBSelect
    *** @return The defined table name
    **/
    public String getTranslatedTableName()
    {
        return this.getFactory().getTranslatedTableName();
    }

    /**
    *** @see DBFactory#getFields()
    **/
    public DBField[] getFields()
    {
        return this.getFactory().getFields();
    }
    
    /**
    *** @see DBFactory#getField(String)
    **/
    public DBField getField(String fldName)
    {
        return this.getFactory().getField(fldName);
    }

    // ------------------------------------------------------------------------

    /**
    *** @see DBFactory#getKeyFields()
    **/
    public DBField[] getKeyFields()
    {
        //if (this.hasFieldValue(FLD_autoIndex)) {
        //    return new DBField[] { this.getField(FLD_autoIndex); }
        //} else {
        return this.getFactory().getKeyFields();
        //}
    }

    /**
    *** @see DBFactory#getAlternateIndex(String)
    **/
    protected DBField[] getAltKeyFields(String indexName)
    {
        DBAlternateIndex altKey = this.getFactory().getAlternateIndex(indexName);
        return (altKey != null)? altKey.getFields() : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a set of tagged field names, or null if no tagged fields have been set.
    *** 'Tagged' fields may be used by the record handler to indicate that certain
    *** data field/columns may be of interest.
    *** @return A set of 'tagged' fields, or null if not set
    **/
    public Set<String> getTaggedFieldNames()
    {
        return this.taggedFields;
    }
    
    /**
    *** Sets a list of 'tagged' data fields.  This set should contain only
    *** non-primary-key field names defined by the DBFactory of this key.
    *** @param taggedFields  A set of 'tagged' data fields.
    **/
    public void setTaggedFieldNames(Set<String> taggedFields)
    {
        this.taggedFields = taggedFields;
    }
    
    /**
    *** Returns true if any tagged field names have been defined
    *** @return True if any tagged field names have been defined
    **/
    public boolean hasTaggedFields()
    {
        return !ListTools.isEmpty(this.taggedFields);
    }
    
    /**
    *** Returns true if the specified field name is in the 'tagged' list, false otherwise.
    *** @param fldName  The field name
    *** @return True if the specified field name is in the 'tagged' list.
    **/
    public boolean isTaggedFieldName(String fldName)
    {
        return (this.taggedFields != null) && this.taggedFields.contains(fldName);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this key fully defines all key fields
    *** @return True if this key fully defines all key fields
    **/
    public boolean isFullKey()
    {
        DBField kfld[] = this.getKeyFields();
        if (ListTools.isEmpty(kfld)) {
            return false;
        } else {
            DBFieldValues fldVals = this.getFieldValues(); // hasPartialKey
            for (int i = 0; i < kfld.length; i++) {
                String fldName = kfld[i].getName();
                if (!fldVals.hasFieldValue(fldName)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
    *** Returns !isFullKey()
    *** @return !isFullKey()
    *** @see #isFullKey
    **/
    public boolean isPartialKey()
    {
        return !this.isFullKey();
    }

    // ------------------------------------------------------------------------

    /**
    *** Return a DBFieldValues instance for thei DBRecordKey
    *** @return The DBFieldValues instance
    **/
    public DBFieldValues getFieldValues()
    {
        if (this.fieldValues == null) {
            this.fieldValues = new DBFieldValues(this);
        }
        return this.fieldValues;
    }
    
    /**
    *** @see DBFieldValues#hasFieldValue(String)
    **/
    public boolean hasFieldValue(String fldName)
    {
        return this.getFieldValues().hasFieldValue(fldName);
    }
    
    /**
    *** @see DBFieldValues#getFieldValue
    **/
    public Object getFieldValue(String fldName)
    {
        return this.getFieldValues().getFieldValue(fldName);
    }
    
    /**
    *** See DBFieldValues#getFieldValueAsString(String)
    **/
    public String getFieldValueAsString(String fldName)
    {
        return this.getFieldValues().getFieldValueAsString(fldName);
    }

    /**
    *** @see DBFieldValues#setFieldValue(String,Object)
    **/
    public boolean setFieldValue(String fldName, Object val)
    {
        return this.getFieldValues().setFieldValue(fldName, val);
    }

    /**
    *** @see DBFieldValues#setFieldValue(String,boolean)
    **/
    public boolean setFieldValue(String fldName, boolean val)
    {
        return this.getFieldValues().setFieldValue(fldName, val);
    }

    /**
    *** @see DBFieldValues#setFieldValue(String,int)
    **/
    public boolean setFieldValue(String fldName, int val)
    {
        return this.getFieldValues().setFieldValue(fldName, val);
    }

    /**
    *** @see DBFieldValues#setFieldValue(String,long)
    **/
    public boolean setFieldValue(String fldName, long val)
    {
        return this.getFieldValues().setFieldValue(fldName, val);
    }

    /**
    *** @see DBFieldValues#setFieldValue(String,double)
    **/
    public boolean setFieldValue(String fldName, double val)
    {
        return this.getFieldValues().setFieldValue(fldName, val);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the full Primary Key exists in the table
    *** @return True if the full Primary key exists in the table, false otherwise
    **/
    public boolean exists()
        throws DBException
    {
        try {
            return this._exists(null, DBWhere.KEY_FULL); // full primary key lookup
        } catch (SQLException sqe) {
            String tn = this.getUntranslatedTableName();
            throw new DBException("Record existance " + tn + "='" + this + "'", sqe);
        }
    }

    /**
    *** Returns true if the partial Primary Key exists in the table
    *** @return True if the partial Primary key exists in the table, false otherwise
    **/
    public boolean exists(int whereKeyType)
        throws DBException
    {
        try {
            return this._exists(null, whereKeyType); // primary key lookup
        } catch (SQLException sqe) {
            String tn = this.getUntranslatedTableName();
            throw new DBException("Record existance " + tn + "='" + this + "'", sqe);
        }
    }

    /**
    *** Returns true if the full (or partial) Primary Key exists in the table
    *** @param fullKeyOnly  True to test for existence of the full Primary Key, false to
    ***                     allow testing for existance of a partial primary key.
    *** @return True if the full/partial primary key exists in the table, false otherwise
    **/
    public boolean exists(boolean fullKeyOnly)
        throws DBException
    {
        try {
            int whereKeyType = fullKeyOnly? DBWhere.KEY_FULL : DBWhere.KEY_PARTIAL_FIRST;
            return this._exists(null, whereKeyType); // primary key lookup
        } catch (SQLException sqe) {
            String tn = this.getUntranslatedTableName();
            throw new DBException("Record existance " + tn + "='" + this + "'", sqe);
        }
    }

    /**
    *** Returns true if the full Alternate Key exists in the table
    *** @return True if the full Alternate key exists in the table, false otherwise
    **/
    public boolean altIndexExists(String indexName)
        throws DBException
    {
        try {
            if (indexName == null) { indexName = DBProvider.DEFAULT_ALT_INDEX_NAME; }
            return this._exists(indexName, DBWhere.KEY_FULL);  // alternate key lookup
        } catch (SQLException sqe) {
            String tn = this.getUntranslatedTableName();
            throw new DBException("Record existance " + tn + "='" + this + "'", sqe);
        }
    }
    
    /**
    *** Returns true if the specified key attribute exists in the table
    *** @param altIndexName   The alternate index name, or null to use the primary index
    *** @param whereKeyType   The partial key match type
    *** @return True if the specified key attribute exists in the table, false otherwise
    **/
    protected boolean _exists(String altIndexName, int whereKeyType)
        throws SQLException, DBException
    {

        /* key fields */
        boolean usePrimaryKey = StringTools.isBlank(altIndexName);
        DBField kfld[] = usePrimaryKey? this.getKeyFields() : this.getAltKeyFields(altIndexName);
        if (ListTools.isEmpty(kfld)) {
            throw new DBException("No keys found!"); 
        }
        
        /* check last key for "auto_increment" */
        if (whereKeyType == DBWhere.KEY_FULL) {
            DBField lastField = kfld[kfld.length - 1];
            if (lastField.isAutoIncrement() && !this.getFieldValues().hasFieldValue(lastField.getName())) {
                // full key requested and last key is auto_increment, which is missing
                return false;
            }
        }

        // DBSelect: SELECT <Keys> FROM <TableName> <KeyWhere>
        String firstKey = kfld[0].getName();
        DBSelect<gDBR> dsel = new DBSelect<gDBR>(this.getFactory());
        dsel.setSelectedFields(firstKey);
        dsel.setWhere(this._getWhereClause(altIndexName, whereKeyType));

        /* get keyed record */
        DBConnection dbc    = null;
        Statement    stmt   = null;
        ResultSet    rs     = null;
        boolean      exists = false;
        try {
            dbc    = DBConnection.getDefaultConnection();
            stmt   = dbc.execute(dsel.toString()); // may throw DBException
            rs     = stmt.getResultSet();
            exists = rs.next();
        } catch (SQLException sqe) {
            if (sqe.getErrorCode() == DBFactory.SQLERR_TABLE_NOTLOCKED) {
                // MySQL: This case has been seen on rare occasions.  Not sure what causes it.
                Print.logError("SQL Lock Error: " + sqe);
                Print.logError("Hackery! Forcing lock on table: " + this.getUntranslatedTableName());
                if (DBProvider.lockTableForRead(this.getUntranslatedTableName(),true)) { // may throw DBException
                    stmt   = dbc.execute(dsel.toString()); // may throw SQLException, DBException
                    rs     = stmt.getResultSet();   // SQLException
                    exists = rs.next();         // SQLException
                    DBProvider.unlockTables();  // DBException
                }
            } else {
                throw sqe;
            }
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }
        
        return exists;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the parent records in their respective parent tables exist.
    *** @return True if the parent records exist.
    **/
    public boolean parentsExist()
        throws DBException
    {
        DBFactory<gDBR> dbFact = this.getFactory();
        DBFieldValues myFldVals = this.getFieldValues();
        java.util.List<String> parentList = dbFact.getParentTables();
        for (String parentTable : parentList) {

            /* get parent table DBFactory */
            Print.logInfo("[%s] Parent table: %s", this.getUntranslatedTableName(), parentTable);
            DBFactory parentFact = DBFactory.getFactoryByName(parentTable);
            if (parentFact == null) {
                Print.logError("Unexpected error finding parent table: " + parentTable);
                return false;
            }

            /* create parent record key with fields from this key */
            DBRecordKey parentKey = parentFact.createKey(); // an empty key
            DBField parentKeyFlds[] = parentFact.getKeyFields();
            for (DBField pkf : parentKeyFlds) {
                String pfn = pkf.getName();
                
                /* get this DBField */
                DBField myKeyFld = this.getField(pfn);
                if (myKeyFld == null) {
                    Print.logError("Unexpected error finding field: [" + this.getUntranslatedTableName() + "] " + pfn);
                    return false;
                }
                
                /* get parent key field value */
                Object pkv = myFldVals.getFieldValue(pfn);
                if (pkv == null) {
                    Print.logError("Unexpected error finding parent field: [" + parentTable + "] " + pfn);
                    return false;
                }
                if (myKeyFld.isDefaultValue(pkv)) {
                    Print.logInfo("This key contains a global value, skipping parent check: " + parentTable);
                    parentKey = null;
                    break;
                }
                parentKey.setFieldValue(pfn, pkv);

            }

            /* check parent existence */
            if ((parentKey != null) && !parentKey.exists()) {
                Print.logError("Parent record does not exist: [" + parentTable + "] " + parentKey);
                return false;
            }

        }
        return true;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Deletes the record corresponding to the Primary Key of thie DBRecordKey.
    *** (Warning: Dependent children records are not deleted!)
    **/
    public void delete()
        throws DBException
    {
        this.delete(false);
    }

    /**
    *** Deletes the record corresponding to the Primary Key of thie DBRecordKey.
    *** @param delDeps  True to also delete dependent children records
    **/
    public void delete(boolean delDeps)
        throws DBException
    {
        try {
            if (delDeps) {
                this._deleteDependencies();
            }
            int whereKeyType = DBWhere.KEY_FULL; // DBRecordKey.WHERE_FULL_KEY_REQUIRED
            this._delete(null, whereKeyType); // primary key delete
        } catch (SQLException sqe) {
            throw new DBException("Record deletion", sqe);
        }
    }

    /**
    *** Deletes the record corresponding to the Alternate Key of thie DBRecordKey.<br>
    *** <b>WARNING: If the alternate key is not unique, calling this method when the alternate
    *** key fields are blank may cause more records to be deleted than is intended.<b>
    *** @param indexName  The alternate index name (defaults to <code>DBProvider.DEFAULT_ALT_INDEX_NAME</code>)
    **/
    public void altIndexDelete(String indexName)
        throws DBException
    {
        try {
            if (indexName == null) { indexName = DBProvider.DEFAULT_ALT_INDEX_NAME; }
            int whereKeyType = DBWhere.KEY_FULL; // DBRecordKey.WHERE_FULL_KEY_REQUIRED
            this._delete(indexName, whereKeyType); // alternate key delete
        } catch (SQLException sqe) {
            throw new DBException("Record deletion", sqe);
        }
    }

    /**
    *** Deletes the record corresponding to the Primary Key of thie DBRecordKey.
    *** @param altIndexName The alternate index name, or null to delete the primary index
    *** @param whereKeyType WHERE key type: Full, PartialFirst, PartialAll
    **/
    protected void _delete(String altIndexName, int whereKeyType) // boolean fullKeyReq)
        throws SQLException, DBException
    {
        // DBDelete: DELETE FROM <table> WHERE <where>
        DBDelete ddel = new DBDelete(this.getFactory());
        ddel.setWhere(this._getWhereClause(altIndexName, whereKeyType));
        //Print.logInfo("DBDelete: " + ddel);
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(ddel.toString());
        } finally {
            DBConnection.release(dbc);
        }
    }
    
    protected void _deleteDependencies()
        throws DBException
    {
        DBField[] keyFlds = this.getKeyFields();
        DBFieldValues fldVals = this.getFieldValues();
        DBFactory fact = this.getFactory();
        DBFactory childFact[] = fact.getChildFactories();
        for (int i = 0; i < childFact.length; i++) {
            DBRecordKey key = childFact[i].createKey(); // an empty key
            for (int k = 0; k < keyFlds.length; k++) {
                String fldName = keyFlds[k].getName();
                if (fldVals.hasFieldValue(fldName)) {
                    Object fldValue = fldVals.getFieldValue(fldName);
                    key.setFieldValue(fldName, fldValue);
                } else {
                    throw new DBException("Missing dependent key fields!");
                }
            }
            // Do not perform recursive dependency deletion!
            // - 'key' is an incomplete (partial key only), and dependency deletion would fail
            // - all dependent children should already be specified by "getChildFactories()"
            try {
                int whereKeyType = DBWhere.KEY_PARTIAL_ALL; // Should use ALL available partial keys.
                key._delete(null, whereKeyType); // primary key delete
            } catch (SQLException sqe) {
                throw new DBException("Record deletion", sqe);
            } 
        }
    }

    // ------------------------------------------------------------------------

    /* return the 'WHERE' clause for this key */
    //public String getWhereClause(boolean fullKeyReq)
    //    throws DBException
    //{
    //    int whereKeyType = fullKeyReq? DBWhere.KEY_FULL : DBWhere.KEY_PARTIAL_FIRST;
    //    return this._getWhereClause(null, whereKeyType); // primary key 'where'
    //}

    /**
    *** Gets the 'WHERE' clause for this key
    *** @param whereKeyType The where key type. One of the constants from DBWhere
    *** @return The 'WHERE' clause for this key
    *** @throws DBException If a database exception occurs
    **/
    public String getWhereClause(int whereKeyType)
        throws DBException
    {
        return this._getWhereClause(null, whereKeyType); // primary key 'where'
    }

    /**
    *** Return the 'WHERE' clause for this key [CHECK]
    *** @param altIndexName The alternate index name. If null or blank, uses 
    ***        primary keys instead
    *** @param whereKeyType The where key type. One of the constants from DBWhere
    *** @return The 'WHERE' clause for this key
    **/
    protected String _getWhereClause(String altIndexName, int whereKeyType) // boolean fullKeyRequired)
        throws DBException
    {

        /* key fields */
        boolean usePrimaryKey = StringTools.isBlank(altIndexName);
        DBField keyFlds[] = usePrimaryKey? this.getKeyFields() : this.getAltKeyFields(altIndexName);
        if (ListTools.isEmpty(keyFlds)) { 
            throw new DBException("No keys found!"); 
        }

        /* WHERE */
        DBWhere dwh = new DBWhere(this.getFactory());
        DBFieldValues fldVals = this.getFieldValues();
        int keyCnt = 0;
        boolean hasPartialKey = false;
        for (int i = 0; i < keyFlds.length; i++) {
            String fldName = keyFlds[i].getName();
            if (fldVals.hasFieldValue(fldName)) {
                if (!hasPartialKey || (whereKeyType == DBWhere.KEY_PARTIAL_ALL)) {
                    String fev = dwh.EQ(fldName,fldVals.getFieldValueAsString(fldName));
                    if (keyCnt > 0) {
                        dwh.append(dwh.AND_(fev));
                    } else {
                        dwh.append(fev);
                    }
                    keyCnt++;
                } else {
                    // whereKeyType == DBWhere.KEY_PARTIAL_FIRST, and we found a subsequent key
                    String m = "Additional partial key in 'WHERE' clause! [" + this.getUntranslatedTableName() + "." + fldName + "]";
                    //throw new DBException(m); // TODO: 
                    Print.logWarn("******************************************************************");
                    Print.logWarn(m);
                    //Print.logWarn(StringTools.join(keyFlds,","));
                    //Print.logStackTrace(m);
                    Print.logWarn("******************************************************************");
                }
            } else 
            if ((i == 0) && (whereKeyType != DBWhere.KEY_PARTIAL_ALL_EMPTY)) { // 
                // missing first key 
                if (keyFlds[i].isAutoIncrement()) {
                    // first key is an "auto_increment" and it is not present
                    // assume that we are expecting the DB server to create this value for us, thus the key dow not exist
                    // However, there is nothing we can do about this here.
                    String m = "First key field for 'WHERE' clause is 'auto_increment' and field is not present [" + this.getUntranslatedTableName() + "." + fldName + "]";
                    throw new DBException(m);
                } else {
                    String m = "Missing first key field for 'WHERE' clause! [" + this.getUntranslatedTableName() + "." + fldName + "]";
                    throw new DBException(m);
                }
            } else
            if (whereKeyType == DBWhere.KEY_FULL) {
                // missing a key when all keys are required
                String m = "Missing key for 'WHERE' clause! [" + this.getUntranslatedTableName() + "." + fldName + "]";
                throw new DBException(m);
            } else {
                // only a portion of the key has been specified.
                // This is a common occurance deleting an Account/Device with sub-dependencies
                //Print.logWarn("Key field not specified: " + this.getUntranslatedTableName() + "." + fldName);
                hasPartialKey = true;
            }
        }

        return (keyCnt > 1)? dwh.WHERE(dwh.toString()) : dwh.WHERE_(dwh.toString());
        
    }

    // ------------------------------------------------------------------------

    /* package */ gDBR _getDBRecord()
    {
        return this.record; // may be null
    }

    @SuppressWarnings("unchecked")
    /* package */ void _setDBRecord(DBRecord<gDBR> rcd)
    {
        this.record = (gDBR)rcd; // unchecked cast
    }

    /**
    *** Gets the DBRecord associated with this key
    *** @return The DBRecord associated with this key
    **/
    public gDBR getDBRecord()
    {
        return this.getDBRecord(false);
    }

    /**
    *** Gets the DBRecord associated with this key
    *** @param reload If the record should be reloaded before it is returned
    *** @return The DBRecord associated with this key
    **/
    public gDBR getDBRecord(boolean reload)
    {
        // returns null if there is an error

        /* create record */
        if (this.record == null) {
            try {
                this.record = DBRecord._createDBRecord(this);
            } catch (DBException dbe) {
                // Implementation error (this should never occur)
                // an NPE will likely follow
                Print.logStackTrace("Implementation error - cant' create DB record", dbe);
                return null;
            }
        }

        /* reload */
        if (reload) {
            // 'reload' is ignored if key does not exist
            this.record.reload();
        }

        /* return record (never null) */
        return this.record;

    }
    
    /**
    *** Gets a virtual DBRecord from the specified remote service
    *** @param servReq  The remote web service
    *** @return The virtual DBRecord (cannot be saved or reloaded)
    **/
    @SuppressWarnings("unchecked")
    public gDBR getVirtualDBRecord(final ServiceRequest servReq)
        throws DBException
    {
        String CMD_dbget       = DBFactory.CMD_dbget;
        String TAG_Response    = servReq.getTagResponse();
        String TAG_Record      = DBFactory.TAG_Record;
        String ATTR_command    = servReq.getAttrCommand();
        String ATTR_result     = servReq.getAttrResult();

        /* send request / get response */
        Document xmlDoc = null;
        try {
            xmlDoc = servReq.sendRequest(CMD_dbget, new ServiceRequest.RequestBody() {
                public StringBuffer appendRequestBody(StringBuffer sb, int indent) {
                    return DBRecordKey.this.toRequestXML(sb, indent);
                }
            });
        } catch (IOException ioe) {
            Print.logException("Error", ioe);
            throw new DBException("Request read error", ioe);
        }

        /* parse 'GTSResponse' */
        Element gtsResponse = xmlDoc.getDocumentElement();
        if (!gtsResponse.getTagName().equalsIgnoreCase(TAG_Response)) {
            Print.logError("Request XML does not start with '%s'", TAG_Response);
            throw new DBException("Response XML does not begin eith '"+TAG_Response+"'");
        }

        /* request command/argument */
        String cmd    = StringTools.trim(gtsResponse.getAttribute(ATTR_command));
        String result = StringTools.trim(gtsResponse.getAttribute(ATTR_result));
        if (StringTools.isBlank(result)) { result = StringTools.trim(gtsResponse.getAttribute("type")); }
        if (!result.equalsIgnoreCase("success")) {
            Print.logError("Response indicates failure");
            throw new DBException("Response does not indicate 'success'");
        }

        /* Record */
        NodeList rcdList = XMLTools.getChildElements(gtsResponse,TAG_Record);
        if (rcdList.getLength() <= 0) {
            Print.logError("No 'Record' tags");
            throw new DBException("GTSResponse does not contain any 'Record' tags");
        }
        Element rcdElem = (Element)rcdList.item(0);

        /* return DBRecord */
        gDBR dbr = (gDBR)DBFactory.parseXML_DBRecord(rcdElem);
        dbr.setVirtual(true);
        return dbr;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this object is equivilent to the specified object
    *** @param other The other object
    *** @return True if <code>other</code> is the same class and all fields and
    ***         field values are the same
    **/
    public boolean equals(Object other) 
    {
        if (other == null) {
            
            return false;
            
        } else
        if (this.getClass().equals(other.getClass())) {

            /* get key fields */
            DBField thisKfld[] = this.getKeyFields();
            DBField othrKfld[] = ((DBRecordKey)other).getKeyFields();
            if (thisKfld.length != othrKfld.length) { return false; }

            /* compare field values */
            DBFieldValues thisFval = this.getFieldValues();
            DBFieldValues othrFval = ((DBRecordKey)other).getFieldValues();
            for (int i = 0; (i < thisKfld.length); i++) {
                if (!thisKfld[i].equals(othrKfld[i])) { 
                    return false; 
                }
                Object thisKey = thisFval.getFieldValue(thisKfld[i].getName());
                Object othrKey = othrFval.getFieldValue(othrKfld[i].getName());
                if ((thisKey == null) || (othrKey == null)) {
                    if (thisKey != othrKey) { 
                        return false; 
                    }
                } else
                if (!thisKey.equals(othrKey)) { 
                    return false; 
                }
            }
            
            /* equals */
            return true;
        }
        
        return false;
    }

    /**
    *** Returns a string representation of this object
    *** @return The string representation of this object
    **/
    public String toString() 
    {
        DBField kf[] = this.getKeyFields();
        if (kf.length == 0) { 
            return "<null>"; 
        } else {
            DBFieldValues fv = this.getFieldValues();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < kf.length; i++) {
                if (i > 0) { sb.append(","); }
                sb.append(fv.getFieldValueAsString(kf[i].getName()));
            }
            return sb.toString();
        }
    }

    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Encodes the specified DBRecordKyes into XML and writes it to
    *** a specified PrintStream
    *** @param out The PrintStream 
    *** @param dbrk The list of DBRecordKeys
    **/
    public static void printXML(PrintStream out, DBRecordKey... dbrk)
    {
        if (out != null) {
            DBRecordKey.printXML(new PrintWriter(out), dbrk);
            out.flush();
        }
    }

    /**
    *** Encodes the specified DBRecordKyes into XML and writes it to
    *** a specified PrintWriter
    *** @param out The PrintWriter 
    *** @param dbrk The list of DBRecordKeys
    **/
    public static void printXML(PrintWriter out, DBRecordKey... dbrk)
    {
        if (out != null) {
            out.write("<"+DBFactory.TAG_RecordKeys+">\n");
            for (int i = 0; i < dbrk.length; i++) {
                dbrk[i].printXML(out, 4);
            }
            out.write("</"+DBFactory.TAG_RecordKeys+">\n");
            out.flush();
        }
    }
    
    /**
    *** Encodes this DBRecordKey into XML and writes it to a specified PrintWriter
    *** @param out    The PrintWriter 
    *** @param indent The number of spaces to indent
    **/
    public void printXML(PrintWriter out, int indent)
    {
        this.printXML(out, indent, -1, false);
    }

    /**
    *** Encodes this DBRecordKey into XML and writes it to a specified PrintWriter
    *** @param out      The PrintWriter 
    *** @param indent   The number of spaces to indent
    *** @param sequence Optional sequence value
    **/
    public void printXML(PrintWriter out, int indent, int sequence)
    {
        this.printXML(out, indent, sequence, false);
    }

    /**
    *** Encodes this DBRecordKey into XML and writes it to a specified PrintWriter
    *** @param out      The PrintWriter 
    *** @param indent   The number of spaces to indent
    *** @param sequence Optional sequence value
    *** @param soapXML  True for SOAP XML
    **/
    public void printXML(PrintWriter out, int indent, int sequence, boolean soapXML)
    {
        if (out != null) {
            out.write(this.toXML(null,indent,sequence,soapXML).toString());
            out.flush();
        }
    }

    /**
    *** Encodes this DBRecordKey into XML
    *** @param sb     The StringBuffer to which the DBRecord XML is writen
    *** @param indent The number of spaces to indent
    *** @return The StringBuffer
    **/
    public StringBuffer toXML(StringBuffer sb, int indent)
    {
        return this.toXML(sb, indent, -1, false);
    }

    /**
    *** Encodes this DBRecordKey into XML
    *** @param sb     The StringBuffer to which the DBRecord XML is writen
    *** @param indent The number of spaces to indent
    *** @return The StringBuffer
    *** @param sequence An optional record sequence number
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, int sequence)
    {
        return this.toXML(sb, indent, sequence, false);
    }

    /**
    *** Encodes this DBRecordKey into XML
    *** @param sb       The StringBuffer to which the DBRecord XML is writen
    *** @param indent   The number of spaces to indent
    *** @param sequence An optional record sequence number
    *** @param soapXML  True for SOAP XML
    *** @return The StringBuffer
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, int sequence, boolean soapXML)
    {
        if (sb == null) { sb = new StringBuffer(); }
        String            prefix     = StringTools.replicateString(" ", indent);
        DBRecordKey<gDBR> recKey     = this;
        String            utableName = recKey.getUntranslatedTableName();
        DBField           fld[]      = recKey.getKeyFields();    // KEY fields
        DBFieldValues     fldVals    = recKey.getFieldValues();
        String            PFX1       = XMLTools.PREFIX(soapXML,indent);
        sb.append(PFX1);
        sb.append(XMLTools.startTAG(soapXML,DBFactory.TAG_RecordKey,
            XMLTools.ATTR(DBFactory.ATTR_table,utableName) +
            ((sequence > 0)?XMLTools.ATTR(DBFactory.ATTR_sequence,sequence):""),
            false,true));
        DBFactory.writeXML_DBFields(sb, 2*indent, fld, fldVals, soapXML);
        sb.append(PFX1);
        sb.append(XMLTools.endTAG(soapXML,DBFactory.TAG_RecordKey,true));
        return sb;
    }

    /**
    *** Encodes this DBRecordKey into XML for "GTSRequest' purposes
    **/
    private StringBuffer toRequestXML(StringBuffer sb, int indent)
    {
        boolean isSoapReq = false;
        if (sb == null) { sb = new StringBuffer(); }
        DBRecordKey<gDBR> recKey     = this;
        String            utableName = recKey.getUntranslatedTableName();
        DBField           fld[]      = recKey.getKeyFields();    // KEY fields
        DBFieldValues     fldVals    = recKey.getFieldValues();
        String            PFX1       = XMLTools.PREFIX(isSoapReq,indent);
        sb.append(PFX1);
        sb.append(XMLTools.startTAG(isSoapReq,DBFactory.TAG_Record,
            XMLTools.ATTR(DBFactory.ATTR_table,utableName),
            false,true));
        DBFactory.writeXML_DBFields(sb, 2*indent, fld, fldVals, isSoapReq);
        sb.append(PFX1);
        sb.append(XMLTools.endTAG(isSoapReq, DBFactory.TAG_Record, true));
        return sb;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified character is a valid character to use in 
    *** an ID
    *** @param ch The character
    *** @return True if the specified character is a valid character to use in
    ***        an ID
    **/
    public static boolean isValidIDChar(char ch)
    {
        // At a minimum, avoid the following special chars: 
        //   $   - substitution character
        //   {}  - have had problems using this character in MySQL
        //   %   - MySQL wildcard character
        //   *   - generic wildcard character
        //   \   - escape character
        //   ?   - just don't use it
        //   ,   - will get confused as a field separator
        //   |   - will get confused as a field separator
        //   /   - will get confused as a field separator
        //   =   - will get confused as a key=value separator
        //   "'` - quotation characters
        //   #   - possible beginning of comment
        //   ~   - just don't use it
        //   ?   - just don't use it
        //   ^   - just don't use it
        // Pending possibles:
        //   !   - Looks like '|'?
        //   -   - ?
        //   +   - ?
        // @abc,#abc,_abc,.abc,&abc
        if (Character.isLetterOrDigit(ch)) {
            return true;
        } else
        if ((ch == '.') || (ch == '_')) {
            // definately accept these
            return true;
        } else
        if ((ch == '@') || (ch == '&') || (ch == '-')) {
            // we'll consider these
            return true;
        } else {
            return false;
        }
    }
    
    /**
    *** Filters an ID String, convertering all letters to lowercase and 
    *** removing invalid characters
    *** @param text The ID String to filter
    *** @return The filtered ID String
    **/
    public static String FilterID(String text)
    {
        // ie. "sky.12", "acme@123"
        if (text != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < text.length(); i++) {
                char ch = Character.toLowerCase(text.charAt(i));
                if (DBRecordKey.isValidIDChar(ch)) {
                    sb.append(ch);
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------

}
