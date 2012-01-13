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
//  2006/04/11  Martin D. Flynn
//     -'toString(<FieldName>)' now returns a default value consistent with the
//      field type if the field has not yet been assigned an actual value.
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/09/16  Martin D. Flynn
//     -Fixed case where default Boolean valus were not properly converted to a 
//      String value in "toString(fldName)".
//  2008/02/04  Martin D. Flynn
//     -Fixed 'setChanged' method to properly pass the old field value.
//  2009/05/01  Martin D. Flynn
//     -Added DateTime datatype
//  2009/05/24  Martin D. Flynn
//     -Made "_setFieldValue(DBField fld, Object newVal)" public to allow direct
//      access to other modules.
//  2011/05/13  Martin D. Flynn
//     -Modified "setAllFieldValues" to accept a list of specific fields to set.
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.text.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBFieldValues</code> is a container class for field/column values for
*** a DBRecord.
**/

public class DBFieldValues
{
    
    // ------------------------------------------------------------------------

    /* validate field values */
    private static boolean VALIDATE_FIELD_VALUES = false;
    
    /**
    *** Sets the global state for validating field values
    *** @param validate True to validate, false otherwise
    **/
    public static void setValidateFieldValues(boolean validate)
    {
        VALIDATE_FIELD_VALUES = validate;
    }
    
    // ------------------------------------------------------------------------
    
    private DBRecordKey                 recordKey = null;
    private OrderedMap<String,Object>   valueMap  = null;
    private OrderedMap<String,DBField>  fieldMap  = null;
    private Map<String,String>          caseMap   = null; // order is not important
    
    private boolean                     mustExist = true;

    /**
    *** Constructor
    **/
    private DBFieldValues()
    {
        this.valueMap  = new OrderedMap<String,Object>();
        this.fieldMap  = new OrderedMap<String,DBField>();
        this.caseMap   = new HashMap<String,String>();
    }

    /**
    *** Constructor
    *** @param rcdKey  The DBRecordKey associated with this field value container
    **/
    public DBFieldValues(DBRecordKey rcdKey)
    {
        this();
        this.recordKey = rcdKey;
        DBField fld[]  = rcdKey.getFields();
        for (int i = 0; i < fld.length; i++) {
            String fldName = DBProvider.translateColumnName(fld[i].getName());
            this.fieldMap.put(fldName, fld[i]);
            this.caseMap.put(fldName.toLowerCase(),fldName);
        }
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
        this.mustExist = !state;
    }
    
    /**
    *** Gets the state of reporting invalid field names.
    *** @return False to report invalid field names, true to suppress/ignore errors.
    **/
    public boolean getIgnoreInvalidFields()
    {
        return !this.mustExist;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the table name for this DBFieldValue instance
    *** @return The table name
    **/
    public String getUntranslatedTableName()
    {
        if (this.recordKey != null) {
            return this.recordKey.getUntranslatedTableName();
        } else {
            return "";
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Clears all field values
    **/
    public void clearFieldValues()
    {
        this.clearFieldValues(null);
    }

    /**
    *** Clears field values
    **/
    public void clearFieldValues(DBField fldList[])
    {
        if (this.recordKey != null) {
            DBField fld[] = (fldList != null)? fldList : this.recordKey.getFields();
            for (int i = 0; i < fld.length; i++) {
                if (!fld[i].isPrimaryKey()) {
                    this._setFieldValue(fld[i], (Object)null);
                }
            }
        } else {
            Print.logStackTrace("DBRecordKey has not been set!");
        }
    }

    /**
    *** Gets the DBField for the specified field name
    *** @param fldName  The field name of the DBField to return
    *** @return  The returned DBField
    **/
    public DBField getField(String fldName)
    {
        if (this.recordKey != null) {
            return this.recordKey.getField(fldName);
        } else {
            Print.logStackTrace("DBRecordKey has not been set!");
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the value for the specified field name
    *** @param fldName   The field name to set
    *** @param requiredField  True to indicate that this field is required (warnings displayed if field is not found)
    *** @param newVal    The 'Object' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    protected boolean _setFieldValue(String fldName, boolean requiredField, Object newVal) 
    {

        /* get/validate field */
        DBField fld = this.getField(fldName);
        if (fld == null) {
            if (requiredField && !this.getIgnoreInvalidFields()) {
                String tn = this.getUntranslatedTableName();
                Print.logError("Field does not exist: " + tn + "." + fldName);
            }
            return false;
        }

        /* set field value */
        return this._setFieldValue(fld, newVal);

    }

    /**
    *** Sets the value for the specified field name
    *** @param fld       The DBField to set
    *** @param newVal    The 'Object' value to set for the field
    *** @return True if 'newVal' is proper field type, false otherwise
    **/
    public boolean _setFieldValue(DBField fld, Object newVal) 
    {

        /* validate Java type */
        if (newVal != null) {
            /* check Java types */
            if (newVal.getClass() == fld.getTypeClass()) {
                // ok
            } else
            if ((newVal instanceof Boolean) && fld.isTypeBoolean()) {
                // ok
            } else
            if ((newVal instanceof Integer) && fld.isTypeInteger()) {
                // ok
            } else
            if ((newVal instanceof Long) && fld.isTypeLong()) {
                // ok
            } else
            if ((newVal instanceof Float) && fld.isTypeFloat()) {
                // ok
            } else
            if ((newVal instanceof Double) && fld.isTypeDouble()) {
                // ok
            } else
            if ((newVal instanceof byte[]) && fld.isTypeBLOB()) {
                // ok
            } else
            if ((newVal instanceof DateTime) && fld.isTypeDateTime()) {
                // ok
            } else {
                Print.logStackTrace("Invalid type["+fld.getName()+"]:" + 
                    " found '" + StringTools.className(newVal) + "'" +
                    ", expected '"+StringTools.className(fld.getTypeClass())+"'");
                return false;
            }
        }

        /* store value */
        String fldName = fld.getName();
        Object oldVal = this._getFieldValue(fldName, true);
        this.valueMap.put(fldName, newVal);
        DBRecord rcd = (this.recordKey != null)? this.recordKey._getDBRecord() : null;
        if (rcd != null) {
            rcd.setChanged(fldName, oldVal, newVal);
        } else
        if (!fld.isKeyField()) {
            // should not be setting a non-key field if there is no associated DBRecord
            Print.logStackTrace("DBRecordKey does not point to a DBRecord! ...");
        }

        /* ok */
        return true;
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName   The field name to set
    *** @param newVal    The 'Object' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, Object newVal) 
    {
        return this._setFieldValue(fldName, false, newVal);
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName   The field name to set
    *** @param newVal    The 'Object' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, Object newVal) 
    {
        return this._setFieldValue(fldName, true, newVal);
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'String' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, String val) 
    {
        return this._setFieldValue(fldName, false, (Object)StringTools.trim(val));
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'String' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, String val) 
    {
        return this._setFieldValue(fldName, true, (Object)StringTools.trim(val));
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'int' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, int val) 
    {
        return this._setFieldValue(fldName, false, (Object)(new Integer(val)));
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'int' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, int val) 
    {
        return this._setFieldValue(fldName, true, (Object)(new Integer(val)));
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'long' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, long val) 
    {
        return this._setFieldValue(fldName, false, (Object)(new Long(val)));
    }
          /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'long' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, long val) 
    {
        return this._setFieldValue(fldName, true, (Object)(new Long(val)));
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'float' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, float val) 
    {
        return this._setFieldValue(fldName, false, (Object)(new Float(val)));
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'float' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, float val) 
    {
        return this._setFieldValue(fldName, true, (Object)(new Float(val)));
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'double' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, double val) 
    {
        return this._setFieldValue(fldName, false, (Object)(new Double(val)));
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'double' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, double val) 
    {
        return this._setFieldValue(fldName, true, (Object)(new Double(val)));
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'boolean' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, boolean val) 
    {
        return this._setFieldValue(fldName, false, (Object)(new Boolean(val)));
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'boolean' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, boolean val) 
    {
        return this._setFieldValue(fldName, true, (Object)(new Boolean(val)));
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'byte[]' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, byte val[])
    {
        return this._setFieldValue(fldName, false, (Object)((val != null)? val : new byte[0]));
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'byte[]' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, byte val[])
    {
        return this._setFieldValue(fldName, true, (Object)((val != null)? val : new byte[0]));
    }

    /**
    *** Sets the value for the specified optional field name
    *** @param fldName  The field name to set
    *** @param val      The 'DateTime' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setOptionalFieldValue(String fldName, DateTime val) 
    {
        return this._setFieldValue(fldName, false, (Object)val);
    }

    /**
    *** Sets the value for the specified field name
    *** @param fldName  The field name to set
    *** @param val      The 'boolean' value to set for the field
    *** @return True if the field exists, false otherwise
    **/
    public boolean setFieldValue(String fldName, DateTime val) 
    {
        return this._setFieldValue(fldName, true, (Object)val);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets all field values from the specified ResultSet
    *** @param rs  The ResultSet from which field values are retrieved
    *** @throws SQLException If field does not exist
    **/
    public void setAllFieldValues(ResultSet rs) 
        throws SQLException
    {
        this.setAllFieldValues(rs, true, null);
    }
    
    /**
    *** Sets all field values from the specified ResultSet
    *** @param rs  The ResultSet from which field values are retrieved
    *** @param setPrimaryKey  True to set primay key fields
    *** @throws SQLException If field does not exist
    **/
    public void setAllFieldValues(ResultSet rs, boolean setPrimaryKey) 
        throws SQLException
    {
        this.setAllFieldValues(rs, setPrimaryKey, null);
    }

    /**
    *** Sets all field values from the specified ResultSet
    *** @param rs  The ResultSet from which field values are retrieved
    *** @param setPrimaryKey  True to set primay key fields
    *** @throws SQLException If field does not exist
    **/
    public void setAllFieldValues(ResultSet rs, boolean setPrimaryKey, DBField fldList[]) 
        throws SQLException
    {
        if (rs == null) {
            // quietly ignore
        } else
        if (this.recordKey != null) {
            String utableName = this.getUntranslatedTableName();
            DBField fld[] = (fldList != null)? fldList : this.recordKey.getFields();
            for (int i = 0; i < fld.length; i++) {
                if (setPrimaryKey || !fld[i].isPrimaryKey()) {
                    try {
                        Object val = fld[i].getResultSetValue(rs); // may throw exception if field does not exist
                        this._setFieldValue(fld[i], val);
                    } catch (SQLException sqe) {
                        // we want to ignore "Column 'xxxx' not found" errors [found: SQLState:S0022;ErrorCode:0]
                        int errCode = sqe.getErrorCode(); // in the test we performed, this was '0' (thus useless)
                        if (errCode == DBFactory.SQLERR_UNKNOWN_COLUMN) {
                            // this is the errorCode that is supposed to be returned
                            Print.logException("Unknown Column: '" + utableName + "." + fld[i].getName() + "'", sqe);
                        } else
                        if (sqe.getMessage().indexOf("Column") >= 0) {
                            // if it says anything about the "Column"
                            if (RTConfig.isDebugMode()) {
                                Print.logException("Column '" + utableName + "." + fld[i].getName() + "'?", sqe);
                            } else {
                                Print.logError("Column '" + utableName + "." + fld[i].getName() + "'? " + sqe);
                            }
                        } else {
                            throw sqe;
                        }
                    }
                }
            }
        } else {
            Print.logStackTrace("DBRecordKey has not been set!");
        }
    }

    /**
    *** Sets all field values from the specified value map (all fields required)
    *** @param valMap  The Field==>Value map
    *** @throws DBException If field does not exist
    **/
    public void setAllFieldValues(Map<String,String> valMap) 
        throws DBException
    {
        this.setAllFieldValues(valMap, true/*setPrimaryKey*/);
    }

    /**
    *** Sets all field values from the specified value map (all fields required)
    *** @param valMap  The Field==>Value map
    *** @param setPrimaryKey True if key fields should also be set
    *** @throws DBException If field does not exist
    **/
    public void setAllFieldValues(Map<String,String> valMap, boolean setPrimaryKey) 
        throws DBException
    {
        this.setFieldValues(valMap, setPrimaryKey, true/*requireAllFields*/);
    }

    /**
    *** Sets the specified field values from the specified ResultSet.
    *** (NOTE: field names specified in the value map, which do not exist in the 
    *** actual field list, are quietly ignored).
    *** @param valMap  The Field==>Value map
    *** @param setPrimaryKey True if key fields should also be set
    *** @throws DBException If field does not exist
    **/
    public void setFieldValues(Map<String,String> valMap, boolean setPrimaryKey, boolean requireAllFields) 
        throws DBException
    {
        if (this.recordKey != null) {
            String utableName = this.getUntranslatedTableName();
            DBField fld[] = this.recordKey.getFields();
            for (int i = 0; i < fld.length; i++) {
                String  name  = fld[i].getName();
                String  val   = valMap.get(name); // may be defined, but null
                if (fld[i].isPrimaryKey()) {
                    if (setPrimaryKey) {
                        if (val != null) {
                            //Print.logInfo("Setting Key Field: " + name + " ==> " + val);
                            Object v = fld[i].parseStringValue(val);
                            this._setFieldValue(fld[i], v);
                        } else {
                            throw new DBException("Setting Key Field Values: value not defined for field - " + name);
                        }
                    }
                } else {
                    if (val != null) {
                        //Print.logInfo("Setting Field: " + name + " ==> " + val);
                        Object v = fld[i].parseStringValue(val);
                        this._setFieldValue(fld[i], v);
                    } else
                    if (requireAllFields) {
                        //throw new DBException("Setting Field Values: value not defined for field - " + name);
                        Print.logError("Column '" + utableName + "." + name + "' value not specified.");
                    }
                }
            }
        } else {
            Print.logStackTrace("DBRecordKey has not been set!");
        }
    }

    /**
    *** Sets the specified field values from the specified ResultSet.
    *** (NOTE: field names specified in the value map, which do not exist in the 
    *** actual field list, are quietly ignored).
    *** @param fldVals  The Field==>Value map
    *** @param setPrimaryKey True if primary key fields should also be set
    *** @throws DBException If field does not exist
    **/
    public void setFieldValues(DBFieldValues fldVals, boolean setPrimaryKey, boolean requireAllFields) 
        throws DBException
    {
        if ((this.recordKey != null) && (fldVals != null)) {
            String utableName = this.getUntranslatedTableName();
            DBField fld[] = this.recordKey.getFields();
            for (int i = 0; i < fld.length; i++) {
                String  name  = fld[i].getName();
                Object  val   = fldVals.getOptionalFieldValue(name);
                if (fld[i].isPrimaryKey()) {
                    if (setPrimaryKey) {
                        if (val != null) {
                            //Print.logInfo("Setting Key Field: " + name + " ==> " + val);
                            this._setFieldValue(fld[i], val);
                        } else {
                            throw new DBException("Setting Key Field Values: value not defined for field - " + name);
                        }
                    }
                } else {
                    if (val != null) {
                        //Print.logInfo("Setting Field: " + name + " ==> " + val);
                        this._setFieldValue(fld[i], val);
                    } else
                    if (requireAllFields) {
                        //throw new DBException("Setting Field Values: value not defined for field - " + name);
                        Print.logError("Column '" + utableName + "." + name + "' value not specified.");
                    }
                }
            }
        } else {
            Print.logStackTrace("DBRecordKey has not been set!");
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the field name to the proper case
    *** @param fldName  The case-insensitive field name
    *** @return  The field name in proper case.
    **/
    public String getFieldName(String fldName)
    {
        if (fldName != null) {
            return this.caseMap.get(fldName.toLowerCase());
        } else {
            return null;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified field name exists in this DBFieldValues instance
    *** @param fldName  The field name to test
    *** @return True if the specified field name exists in this DBFieldValues instance
    **/
    public boolean hasField(String fldName)
    {
        // if true, the field is defined
        if (fldName == null) {
            return false;
        } else {
            String fn = DBProvider.translateColumnName(fldName);
            return this.fieldMap.containsKey(fn);
        }
    }

    /**
    *** Returns true if a value has been set for the specified field name
    *** @param fldName  The field name to test for a set value
    *** @return True if ta value has been set for the specified field name
    **/
    public boolean hasFieldValue(String fldName)
    {
        // if true, the field, and its value, are defined
        return (fldName != null)? this.valueMap.containsKey(fldName) : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified field name
    *** @param fldName  The field name for the value retrieved
    *** @param requiredField  True to indicate that this field is required (warnings displayed if field is not found)
    *** @return The field value
    **/
    protected Object _getFieldValue(String fldName, boolean requiredField) 
    {
        Object val = (fldName != null)? this.valueMap.get(fldName) : null;
        if (val != null) {
            // field value found (or undefined)
            return val;
        } else
        if (this.hasField(fldName)) {
            // field name found, but value is null (which may be the case if the value was undefined)
            //Print.logError("Field value is null: " + fldName);
            return null;
        } else
        if (requiredField && !this.getIgnoreInvalidFields()) {
            Print.logStackTrace("Field not found: " + fldName);
            return null;
        } else {
            return null;
        }
    }

    /**
    *** Gets the value for the specified field name
    *** @param fldName        The field name for the value retrieved
    *** @param requiredField  True to indicate that this field is required (warnings displayed if field is not found)
    *** @param rtnDft         True to return a default value if a value has not been set
    *** @return The field value
    **/
    protected Object _getFieldValue(String fldName, boolean requiredField, boolean rtnDft) 
    {

        /* get field value */
        Object obj = this._getFieldValue(fldName, requiredField);
        
        /* create default? */
        if ((obj == null) && rtnDft) {
            // return a default value consistent with the field type
            DBField fld = this.getField(fldName);
            if (fld != null) {
                obj = fld.getDefaultValue();
                if (obj == null) {
                    // Implementation error, this should never occur
                    Print.logStackTrace("Field doesn't support a default value: " + fldName);
                    return null;
                }
            } else {
                // Implementation error, this should never occur
                // If we're here, the field doesn't exist.
                return null;
            }
        }
        
        /* return object */
        return obj;
        
    }

    /**
    *** Gets the value for the specified optional field name
    *** @param fldName  The field name for the value retrieved
    *** @return The optional field value, or null if the field has not been set, or does not exist
    **/
    public Object getOptionalFieldValue(String fldName) 
    {
        return this._getFieldValue(fldName, false, false);
    }

    /**
    *** Gets the value for the specified optional field name
    *** @param fldName  The field name for the value retrieved
    *** @return The optional field value, or null if the field has not been set, or does not exist
    **/
    public Object getOptionalFieldValue(String fldName, boolean rtnDft) 
    {
        return this._getFieldValue(fldName, false, rtnDft);
    }

    /**
    *** Gets the value for the specified field name
    *** @param fldName  The field name for the value retrieved
    *** @return The field value
    **/
    public Object getFieldValue(String fldName) 
    {
        return this._getFieldValue(fldName, true, false);
    }

    /**
    *** Gets the value for the specified field name
    *** @param fldName  The field name for the value retrieved
    *** @param rtnDft   True to return a default value if a value has not been set
    *** @return The field value
    **/
    public Object getFieldValue(String fldName, boolean rtnDft) 
    {
        return this._getFieldValue(fldName, true, rtnDft);
    }

    /**
    *** Gets the String representation of the field value
    *** @param fldName  The field name for the value retrieved
    *** @return The String representation of the field value
    **/
    public String getFieldValueAsString(String fldName) 
    {
        Object val = this.getFieldValue(fldName, true);
        if (val instanceof Number) {
            DBField fld = this.getField(fldName);
            if (fld != null) {
                String fmt = fld.getFormat();
                if ((fmt != null) && fmt.startsWith("X")) { // hex
                    return fld.formatValue(val);
                }
            }
        }
        return DBFieldValues.toStringValue(val);
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the specified object to a String representation
    *** @param obj The Object to converts to a String representation
    *** @return The String representation of the specified object
    **/
    public static String toStringValue(Object obj) 
    {

        /* null? */
        if (obj == null) {
            return "";
        }

        /* DBFieldType? */
        if (obj instanceof DBFieldType) {
            obj = ((DBFieldType)obj).getObject();
        }

        /* convert to String */
        if (obj instanceof String) {
            return ((String)obj).trim();
        } else
        if (obj instanceof Number) {
            return obj.toString();
        } else
        if (obj instanceof Boolean) {
            return ((Boolean)obj).booleanValue()? "1" : "0";
        } else
        if (obj instanceof byte[]) {
            String hex = StringTools.toHexString((byte[])obj);
            return "0x" + hex;
        } else
        if (obj instanceof DateTime) {
            DateTime dt = (DateTime)obj;
            return dt.format("yyyy-MM-dd HH:mm:ss", DateTime.getGMTTimeZone());
        } else {
            Print.logWarn("Converting object to string: " + StringTools.className(obj));
            return obj.toString();
        }

    }

}
