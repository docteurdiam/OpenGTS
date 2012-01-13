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
//     -Added 'format' attribute
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/02/28  Martin D. Flynn
//     -Added ability to convert SQL types to standard field types.
//  2007/09/16  Martin D. Flynn
//     -Added static "quote" method to provide a single place where SQL values are quoted.
//  2007/11/28  Martin D. Flynn
//     -Added methods 'isUnsigned()', 'isNumeric()', 'isString()'
//  2008/02/27  Martin D. Flynn
//     -Added 'isDecimal', 'getTypeMask' methods
//  2008/03/12  Martin D. Flynn
//     -Added method 'getEditor'
//  2008/05/14  Martin D. Flynn
//     -Added support for multiple alternate index keys
//  2008/05/20  Martin D. Flynn
//     -Added a 'Locale' aregument to the 'getTitle' method (for Localization purposes).
//     -Additional changes to the optional/required field specification.
//  2008/06/20  Martin D. Flynn
//     -Added support for enum/mask classes and values (used for record editing).
//  2009/01/28  Martin D. Flynn
//     -Added changes to support UTF8 character sets
//  2009/05/01  Martin D. Flynn
//     -Added DateTime datatype
//  2011/05/13  Martin D. Flynn
//     -Remove 'isPriKey' check from 'isUniqueALtKey' settings.
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBField</code> represents a specific field in an SQL table.
**/

public class DBField
{

    // ------------------------------------------------------------------------

    /**
    *** The default 'isRequired' field state.<br>
    *** If 'true', then all fields are considered required, unliess tagged with "optional".<br>
    *** If 'false', then all fields are considered optional, unliess tagged with "required".<br>
    *** (Note: primary/alternate key fields are always considered required).
    **/
    private static final boolean DEFAULT_REQUIRED   = true;

    // ------------------------------------------------------------------------

    public static final String TYPE_BOOLEAN         = DBProvider.TYPE_BOOLEAN;
    public static final String TYPE_INT8            = DBProvider.TYPE_INT8;
    public static final String TYPE_UINT8           = DBProvider.TYPE_UINT8;
    public static final String TYPE_INT16           = DBProvider.TYPE_INT16;
    public static final String TYPE_UINT16          = DBProvider.TYPE_UINT16;
    public static final String TYPE_INT32           = DBProvider.TYPE_INT32;
    public static final String TYPE_UINT32          = DBProvider.TYPE_UINT32;
    public static final String TYPE_INT64           = DBProvider.TYPE_INT64;
    public static final String TYPE_UINT64          = DBProvider.TYPE_UINT64;
    public static final String TYPE_FLOAT           = DBProvider.TYPE_FLOAT;
    public static final String TYPE_DOUBLE          = DBProvider.TYPE_DOUBLE;
    public static final String TYPE_SBLOB           = DBProvider.TYPE_SBLOB;
    public static final String TYPE_BLOB            = DBProvider.TYPE_BLOB;
    public static final String TYPE_MBLOB           = DBProvider.TYPE_MBLOB;
    public static final String TYPE_TEXT            = DBProvider.TYPE_TEXT;
    public static final String TYPE_STRING          = DBProvider.TYPE_STRING;
    public static final String TYPE_DATETIME        = DBProvider.TYPE_DATETIME;

    public static String TYPE_STRING(int n)         { return DBProvider.TYPE_STRING(n); }

    public static String TYPE_STRING(String T,int D) { return DBField.TYPE_STRING(RTConfig.getInt((RTKey.DB_TYPESIZE_ + T), D)); }
    // ie. db.typeSize.address=128
    public static String TYPE_ID()                  { return DBField.TYPE_STRING("ID"           , 32); }
    public static String TYPE_ACCT_ID()             { return DBField.TYPE_STRING("accountID"    , 32); }
    public static String TYPE_USER_ID()             { return DBField.TYPE_STRING("userID"       , 32); }
    public static String TYPE_DEV_ID()              { return DBField.TYPE_STRING("deviceID"     , 32); }
    public static String TYPE_XPORT_ID()            { return DBField.TYPE_STRING("transportID"  , 32); }
    public static String TYPE_GROUP_ID()            { return DBField.TYPE_STRING("groupID"      , 32); }
    public static String TYPE_ROLE_ID()             { return DBField.TYPE_STRING("roleID"       , 32); }
    public static String TYPE_RULE_ID()             { return DBField.TYPE_STRING("ruleID"       , 32); }
    public static String TYPE_CORR_ID()             { return DBField.TYPE_STRING("corridorID"   , 32); }
    public static String TYPE_DRIVER_ID()           { return DBField.TYPE_STRING("driverID"     , 32); }
    public static String TYPE_ENTITY_ID()           { return DBField.TYPE_STRING("entityID"     , 32); }
    public static String TYPE_ZONE_ID()             { return DBField.TYPE_STRING("zoneID"       , 32); }
    public static String TYPE_PROP_ID()             { return DBField.TYPE_STRING("propertyID"   , 32); }
    public static String TYPE_ADDRESS()             { return DBField.TYPE_STRING("address"      , 90); }
    public static String TYPE_EMAIL_LIST()          { return DBField.TYPE_STRING("emailList"    ,128); }

    public static String TYPE_UNIQ_ID()             { return DBField.TYPE_STRING("uniqueID"     , 40); }

    public static String TYPE_DESC()                { return DBField.TYPE_STRING("description"  ,128); }

    // ------------------------------------------------------------------------

    public static final String SQL_AUTO_INCREMENT   = "auto_increment";
    public static final String SQL_NOT_NULL         = "NOT NULL";

    // ------------------------------------------------------------------------
    // EDIT_NEVER   : Never editable (maintained by system)
    // EDIT_NEW     : Only editable when new records are created
    // EDIT_ADMIN   : Editable by admin only
    // EDIT_PUBLIC  : Editable by anyone having access to the data

    public static final int    EDIT_NEVER           = -1;
    public static final int    EDIT_NEW             = 0;
    public static final int    EDIT_ADMIN           = 1;
    public static final int    EDIT_PUBLIC          = 2;

    // ------------------------------------------------------------------------
    // field attributes
    
    public static final String ATTR_KEY             = "key";        // primary key [Boolean]
    public static final String ATTR_UNIQUE          = "unique";     // unique key [Boolean]
    public static final String ATTR_ALTKEY          = "altkey";     // alternate index key [String]
    public static final String ATTR_OPTIONAL        = "optional";   // optional field [Boolean]
    public static final String ATTR_REQUIRED        = "required";   // required field [Boolean]
    public static final String ATTR_EDIT            = "edit";       // editable mode [Integer:0,1,2]
    public static final String ATTR_FORMAT          = "format";     // format [String]
    public static final String ATTR_ENUM            = "enum";       // enum [String]
    public static final String ATTR_MASK            = "mask";       // mask [String]
    public static final String ATTR_EDITOR          = "editor";     // editor [String]
    public static final String ATTR_PRESEP          = "presep";     // editor pre-separator
    public static final String ATTR_UTF8            = "utf8";       // UTF8 character set [Boolean]
    public static final String ATTR_AUTO_INCR       = "auto";       // auto-increment [Boolean]
    public static final String ATTR_UNITS           = "units";      // Field unit type [String]

    // ------------------------------------------------------------------------

    public static final char   ENUM_TYPE_SEPARATOR  = '|';
    public static final char   ENUM_VALUE_SEPARATOR = ':';

    public static final char   MASK_TYPE_SEPARATOR  = '|';
    public static final char   MASK_VALUE_SEPARATOR = ':';

    public static final char   ALT_INDEX_SEPARATOR  = ',';

    // ------------------------------------------------------------------------

    private static final char  ESCAPE_CHAR          = '\\'; // may be DBProvider dependent!
    private static final char  QUOTE_CHAR           = '\''; // may be DBProvider dependent!
    
    /**
    *** Quotes and returns the specified String
    *** @param s  The String to quote
    *** @return The quoted String
    **/
    public static String quote(String s)
    {
        return DBField.quote(s, QUOTE_CHAR, true);
    }

    /**
    *** Quotes and returns the specified String
    *** @param s  The String to quote
    *** @param q  The quote character (either ' or ")
    *** @param escapeQuote  True to include embedded quotes with \", false to include embedded quotes with ""
    *** @return The quoted String
    **/
    public static String quote(String s, char q, boolean escapeQuote)
    {
        if (s == null) { s = ""; }
        char ch[] = s.toCharArray();
        int c = 0, len = ch.length;
        StringBuffer qsb = new StringBuffer();
        qsb.append(q);
        for (;c < len; c++) {
            if (ch[c] == q) {
                if (escapeQuote) {
                    qsb.append(ESCAPE_CHAR).append(q);  // \"
                } else {
                    qsb.append(q).append(q);            // ""
                }
            } else
            if (ch[c] == ESCAPE_CHAR) {
                qsb.append(ESCAPE_CHAR).append(ESCAPE_CHAR);
            } else
            if (ch[c] == '\n') {
                qsb.append(ESCAPE_CHAR).append('n');
            } else
            if (ch[c] == '\r') {
                qsb.append(ESCAPE_CHAR).append('r');
            } else
            if (ch[c] == '\t') {
                qsb.append(ESCAPE_CHAR).append('t');
            //} else
            //if (ch[c] == 0x26) {
            //    qsb.append(ESCAPE_CHAR).append('Z'); // MySQL: special case for ^Z
            } else {
                qsb.append(ch[c]);
            }
        }
        qsb.append(q);
        return qsb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          name            = "";
    private Class           javaClass       = null;
    
    private String          sqlType         = null;
    private String          charSet         = "";
    private String          dataType        = "";
    private boolean         isTypeMatch     = false;
    
    private int             stringLength    = -1;

    private boolean         isRequired      = DEFAULT_REQUIRED;
    
    private boolean         isAutoIncr      = false;

    private boolean         isPriKey        = false;
    private boolean         isUniqueAltKey  = false;
    private String          altIndexNames[] = null;

    private int             typeMask        = DBProvider.DATATYPE_NONE;

    private I18N.Text       i18nTitle       = null;

    private RTProperties    attr            = null;
    private int             editMode        = 0;
    
    private boolean         enumInit        = false;
    private Class<? extends Enum> enumClass = null;
    
    private boolean         maskInit        = false;
    private Class<? extends Enum> maskClass = null;

    private DBFactory       factory         = null;
    
    private Object          defaultValue    = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor.  Used only by 'DBFactory.getExistingColumns' to load actual tables columns.
    *** @param utableName The untranslated name of the table containing this field
    *** @param colName    The name of this field column
    *** @param sqlType    The SQL field type
    *** @param indexNames A set of index names for this field (may include "PRIMARY")
    **/
    public DBField(String utableName, String colName, 
        String sqlType, boolean autoIncr,
        String charSet, Set<String> indexNames)
    {
        // Used for existing column definitions
        this.name        = (colName != null)? colName : "";
        this.javaClass   = null;
        this.sqlType     = (sqlType != null)? sqlType.toUpperCase() : "";
        this.charSet     = (charSet != null)? charSet.toLowerCase() : "";
        this.dataType    = null;    // init below
        this.isTypeMatch = false;   // init below
        this.typeMask    = 0;       // init below
        this.attr        = new RTProperties("");
        this.editMode    = EDIT_NEVER;
        this.isRequired  = true;
        this.i18nTitle   = null;
        this.isAutoIncr  = autoIncr;

        /* primary key */
        if ((indexNames != null) && indexNames.contains(DBProvider.PRIMARY_INDEX_NAME)) {
            this.isPriKey = true;
            indexNames.remove(DBProvider.PRIMARY_INDEX_NAME);
        } else {
            this.isPriKey = false;
        }

        /* unique key */
        if ((indexNames != null) && indexNames.contains(DBProvider.UNIQUE_INDEX_NAME)) {
            this.isUniqueAltKey = true;
            indexNames.remove(DBProvider.UNIQUE_INDEX_NAME);
        } else {
            this.isUniqueAltKey = false;
        }

        /* alternate index */
        if ((indexNames != null) && !indexNames.isEmpty()) {
            this.altIndexNames = indexNames.toArray(new String[indexNames.size()]);
        } else {
            this.altIndexNames = null;
        }

        /* check the preferred data type */
        // Different defined data-types may map to the same sql-type.
        // This initialization attempts to set the preferred data-type (as defined by the table wrapper).
        // If the preferred data-type produces an sql-type that matches the incoming sql-type, then set 
        // this field's data-type to the preferred data-type
        DBFactory fact = DBFactory.getFactoryByName(utableName);
        if (fact != null) {
            this.setFactory(fact);
            DBField fld = fact.getField(colName);
            if (fld != null) {
                String prefDT = fld.getDataType();      // preferred DataType
                String prefST = fld.getSqlType(false);  // preferred SQLType
                if (prefST.equalsIgnoreCase(this.sqlType)) {
                    // The resulting sql-types are exactly equal, use the preferred data-type
                    this.dataType    = prefDT;
                    this.isTypeMatch = true;
                    this.typeMask    = DBProvider.getDataTypeMask(this.dataType);
                } else {
                    String dbpDT = DBProvider.getDataTypeFromSqlType(this.sqlType);
                    if (DBProvider.areTypesEquivalent(prefDT,dbpDT)) {
                        // The resulting data-types are equivalent, use the preferred data-type
                        this.dataType    = prefDT;
                        this.isTypeMatch = true;
                        this.typeMask    = DBProvider.getDataTypeMask(this.dataType);
                    } else {
                        // preferred/actual types are different
                        this.dataType    = dbpDT;
                        Print.logInfo("["+colName+"] Type mismatch - expected:" + prefST + "[" + prefDT + "] ==> found:" + this.sqlType + "[" + dbpDT + "]");
                        this.isTypeMatch = false;
                        this.typeMask    = DBProvider.getDataTypeMask(this.dataType);
                    }
                }
            } else {
                // this column doesn't exist in our table definition
                //Print.logInfo("This column is not used in the defined table: " + this._getName());
                this.dataType    = DBProvider.getDataTypeFromSqlType(this.sqlType);
                this.isTypeMatch = false;
                this.typeMask    = DBProvider.getDataTypeMask(this.dataType);
            }
        } else {
            // This table doesn't exist in our known list of tables
            this.dataType    = DBProvider.getDataTypeFromSqlType(this.sqlType);
            this.isTypeMatch = false;
            this.typeMask    = DBProvider.getDataTypeMask(this.dataType);
        }

        /* cache string lengths (from 'this.dataType') */
        this.getStringLength();

    }

    /**
    *** Constructor.  Used within table definition modules to define fileds/columns.<br>
    *** The file attribute list should specify a String containing a list of "key=value" properties.
    *** The following attribute/property keys may be specified:
    *** <ul>
    ***  <li>"key"    - "true" if this field is a primary key (may be omitted otherwise) [ie. "key=true"]</li>
    ***  <li>"altkey" - An alternate index name [ie. "altkey=myIndex"]</li>
    ***  <li>"edit"   - A field edit mode (-1=never, 0=new, 1=administrator, 2=public) [ie. "edit=2"]</li>
    ***  <li>"format" - The default display field format used for reports, etc. [ie. "format=0.000"]</li>
    ***  <li>"enum"   - A reference to the enumerated type for this field, used by a table/field editor [ie. "enum=SpeedUnits"]</li>
    ***  <li>"mask"   - A reference to a bitmasked type for this field, used by a table/field editor [ie. "enum=1:Binary|2:Base64|4:Hex"]</li>
    ***  <li>"editor" - The name of a field editor used by a table/field editor UI [ie. "editor=statusCode"]</li>
    ***  <li>"presep" - Used by the table/field editor UI to insert a separator before this field [ie. "presep=true"]</li>
    *** </ul>
    *** @param fldName   The field name
    *** @param javaClass The Java class representation of this field
    *** @param dataType  The field SQL data type
    *** @param title     The displayed field title
    *** @param attr      The field attribute list
    **/
    public DBField(String fldName, Class javaClass, String dataType, String title, String attr)
    {
        this(fldName, javaClass, dataType, new I18N.Text(title), attr);
    }
    
    /**
    *** Constructor.  Used within table definition modules to define fileds/columns.<br>
    *** The file attribute list should specify a String containing a list of "key=value" properties.
    *** The following attribute/property keys may be specified:
    *** <ul>
    ***  <li>"key"    - "true" if this field is a primary key (may be omitted otherwise) [ie. "key=true"]</li>
    ***  <li>"altkey" - An alternate index name [ie. "altkey=myIndex"]</li>
    ***  <li>"edit"   - A field edit mode (-1=never, 0=new, 1=administrator, 2=public) [ie. "edit=2"]</li>
    ***  <li>"format" - The default display field format used for reports, etc. [ie. "format=0.000"]</li>
    ***  <li>"enum"   - A reference to the enumerated type for this field, used by a table/field editor [ie. "enum=SpeedUnits"]</li>
    ***  <li>"mask"   - A reference to a bitmasked type for this field, used by a table/field editor [ie. "enum=1:Binary|2:Base64|4:Hex"]</li>
    ***  <li>"editor" - The name of a field editor used by a table/field editor UI [ie. "editor=statusCode"]</li>
    ***  <li>"presep" - Used by the table/field editor UI to insert a separator before this field [ie. "presep=true"]</li>
    *** </ul>
    *** @param fldName   The field name
    *** @param javaClass The Java class representation of this field
    *** @param dataType  The field SQL data type
    *** @param title     The displayed field title
    *** @param attr      The field attribute list
    **/
    public DBField(String fldName, Class javaClass, String dataType, I18N.Text title, String attr)
    {
        // Used by table definitions
        this.name        = (fldName != null)? fldName : "";
        this.javaClass   = javaClass;
        this.sqlType     = null; // always null for 'defined' fields
        this.charSet     = "";   // init below
        this.dataType    = (dataType != null)? dataType.toUpperCase() : "";
        this.isTypeMatch = true; // this field always matches itself
        this.typeMask    = DBProvider.getDataTypeMask(this.dataType);
        this.attr        = new RTProperties((attr != null)? attr : "");
        this.editMode    = this.getIntAttribute(ATTR_EDIT, EDIT_NEVER);
        this.isRequired  = DEFAULT_REQUIRED;
        this.i18nTitle   = title;

        /* character set */
        this.charSet     = (RTConfig.getBoolean(RTKey.DB_UTF8) && this.getBooleanAttribute(ATTR_UTF8,false))? "utf8" : "";

        /* primary key */
        this.isPriKey    = this.getBooleanAttribute(ATTR_KEY,false);

        /* unique key */
        this.isUniqueAltKey = /* this.isPriKey || */ this.getBooleanAttribute(ATTR_UNIQUE,false);

        /* alternate index keys */
        this.altIndexNames = null;
        String akn[] = StringTools.parseString(this.getStringAttribute(ATTR_ALTKEY,null), ALT_INDEX_SEPARATOR); // alt1,alt2,etc
        if ((akn != null) && (akn.length > 0)) {
            // if we're here, then 'altkey' has been specified with a non-empty value
            java.util.List<String> altKeyList = new Vector<String>();
            for (int i = 0; i < akn.length; i++) {
                if (StringTools.isBlank(akn[i])) {
                    // null/empty 'altkey' value
                    Print.logStackTrace("Invalid alternate key specification for field: " + fldName);
                } else
                if (akn[i].equalsIgnoreCase("true") || akn[i].equalsIgnoreCase("yes") || akn[i].equals("1")) {
                    // default 'altkey' value
                    //Print.logInfo("Alternate Index: " + this._getName() + " ==> " + DBProvider.DEFAULT_ALT_INDEX_NAME);
                    altKeyList.add(DBProvider.DEFAULT_ALT_INDEX_NAME);
                } else 
                if (akn[i].equals(DBProvider.DEFAULT_ALT_INDEX_NAME)) {
                    // explicit 'altIndex' default value
                    //Print.logInfo("Alternate Index: " + this._getName() + " ==> " + akn[i]);
                    altKeyList.add(akn[i]);
                } else {
                    // other alternate index name
                    //Print.logInfo("Alternate Index: " + this._getName() + " ==> " + akn[i]);
                    altKeyList.add(akn[i]);
                }
            }
            if (!altKeyList.isEmpty()) {
                this.altIndexNames = altKeyList.toArray(new String[altKeyList.size()]);
            } else {
                // should not be here, since we've prequalified the 'altkey' value list
                // (this is possible if something similar to the following is specified: "altkey=,,,")
            }
        }

        /* required/optional? */
        if (this.isPriKey || this.isUniqueAltKey || (this.altIndexNames != null)) {
            if (this.getBooleanAttribute(ATTR_OPTIONAL,false)) {
                Print.logWarn("'Optional' specification ignored for key field!");
            }
            this.isRequired = true;
        } else
        if (this.getBooleanAttribute(ATTR_REQUIRED,false)) {
            this.isRequired = true;
        } else
        if (this.getBooleanAttribute(ATTR_OPTIONAL,false)) {
            this.isRequired = false;
        }

        /* auto_increment */
        this.isAutoIncr = this.getBooleanAttribute(ATTR_AUTO_INCR,false);

        /* cache string lengths (from 'this.dataType') */
        this.getStringLength();

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the parent table DBFactory for this field
    *** @param factory  The parent DBFactory instance
    **/
    public void setFactory(DBFactory factory)
    {
        this.factory = factory;
    }
    
    /**
    *** Gets the parent table DBFactory for this field
    *** @return  The parent DBFactory instance
    **/
    public DBFactory getFactory()
    {
        return this.factory;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Java class for this field
    *** @return  The Java class for this field
    **/
    public Class getTypeClass()
    {
        return this.javaClass;
    }

    /** 
    *** Returns true if this field is a String type.
    *** @return True if this field is a String type.
    **/
    public boolean isTypeString()
    {
        return (this.javaClass == String.class);
    }

    /** 
    *** Returns true if this field is a Integer type.
    *** @return True if this field is a Integer type.
    **/
    public boolean isTypeInteger()
    {
        return ((this.javaClass == Integer.class) || (this.javaClass == Integer.TYPE));
    }

    /** 
    *** Returns true if this field is a Long type.
    *** @return True if this field is a Long type.
    **/
    public boolean isTypeLong()
    {
        return ((this.javaClass == Long.class) || (this.javaClass == Long.TYPE));
    }

    /** 
    *** Returns true if this field is a Float type.
    *** @return True if this field is a Float type.
    **/
    public boolean isTypeFloat()
    {
        return ((this.javaClass == Float.class) || (this.javaClass == Float.TYPE));
    }

    /** 
    *** Returns true if this field is a Double type.
    *** @return True if this field is a Double type.
    **/
    public boolean isTypeDouble()
    {
        return ((this.javaClass == Double.class) || (this.javaClass == Double.TYPE));
    }

    /** 
    *** Returns true if this field is a Boolean type.
    *** @return True if this field is a Boolean type.
    **/
    public boolean isTypeBoolean()
    {
        return ((this.javaClass == Boolean.class) || (this.javaClass == Boolean.TYPE));
    }

    /** 
    *** Returns true if this field is a Byte[] type.
    *** @return True if this field is a Byte[] type.
    **/
    public boolean isTypeBLOB()
    {
        return ((this.javaClass == Byte[].class) || (this.javaClass == byte[].class));
    }

    /** 
    *** Returns true if this field is a DateTime type.
    *** @return True if this field is a DateTime type.
    **/
    public boolean isTypeDateTime()
    {
        return (this.javaClass == DateTime.class);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this field is "auto_increment"
    *** @return True if this field is "auto_increment"
    **/
    public boolean isAutoIncrement()
    {
        return this.isAutoIncr;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this field is required
    *** @return True if this field is required
    **/
    public boolean isRequired()
    {
        return this.isRequired;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the column character set
    *** @return The column character set
    **/
    public String getCharacterSet()
    {
        return this.charSet;
    }
    
    /**
    *** Returns true if this field is defined as character set UTF8
    *** @return True if this field is defined as character set UTF8
    **/
    public boolean isUTF8()
    {
        return (this.charSet != null) && this.charSet.startsWith("utf8");
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this field is a primary key
    *** @return True if this field is a primary key
    **/
    public boolean isPrimaryKey()
    {
        return this.isPriKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this field is a unique key
    *** @return True if this field is a unique key
    **/
    public boolean isUniqueAltKey()
    {
        return this.isUniqueAltKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this field is an alternate index key
    *** @return True if this field is an alternate index key
    **/
    public boolean isAlternateKey()
    {
        return ((this.altIndexNames != null) && (this.altIndexNames.length > 0));
    }
    
    /**
    *** Gets an array of alternate key index names
    *** @return An array of alternate key index names, or null if this field has not alternate keys
    **/
    public String[] getAlternateIndexes()
    {
        return this.isAlternateKey()? this.altIndexNames : null;
    }
    
    /**
    *** Returns true if this field defines the specified alternate indexes
    *** @param altIndexes  The list of alternate indexes to check
    *** @return True if this field defines the specified alternate indexes
    **/
    public boolean equalsAlternateIndexes(String altIndexes[])
    {
        String altNdx[] = this.getAlternateIndexes();
        if ((altIndexes == null) || (altIndexes.length == 0)) {
            return ((altNdx == null) || (altNdx.length == 0));
        } else
        if ((altNdx == null) || (altNdx.length == 0)) {
            return false;
        } else
        if (altNdx.length != altIndexes.length) {
            return false;
        } else {
            // TODO: probably could stand some optimization
            for (int i = 0; i < altNdx.length; i++) {
                boolean found = false;
                for (int j = 0; j < altIndexes.length; j++) {
                    if (altNdx[i].equals(altIndexes[j])) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
    *** Returns true if this is a key field (either primary or alternate)
    *** @return True if this is a key field
    **/
    public boolean isKeyField()
    {
        return this.isPrimaryKey() || this.isAlternateKey();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this field was loaded from the actual table columns
    *** @return True if this field was loaded from the actual table columns
    **/
    public boolean isSqlField()
    {
        return !StringTools.isBlank(this.sqlType);
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Gets the type mask for this field
    *** @return The type mask for this field
    **/
    public int getTypeMask()
    {
        return this.typeMask;
    }

    /**
    *** Returns true if this is a boolean field
    *** @return True if this is a boolean field
    **/
    public boolean isBoolean()
    {
        return DBProvider.isDataTypeBoolean(this.typeMask);
    }

    /**
    *** Returns true if this is a numeric field (ie. decimal, unsigned, etc)
    *** @return True if this is a numeric field
    **/
    public boolean isNumeric()
    {
        return DBProvider.isDataTypeNumeric(this.typeMask);
    }

    /**
    *** Returns true if this is a decimal (floating point) field
    *** @return True if this is a decimal field
    **/
    public boolean isDecimal()
    {
        return DBProvider.isDataTypeDecimal(this.typeMask);
    }

    /**
    *** Returns true if this is an unsigned integer field
    *** @return True if this is an unsigned integer field
    **/
    public boolean isUnsigned()
    {
        return DBProvider.isDataTypeUnsigned(this.typeMask);
    }

    /**
    *** Returns true if this is a String field
    *** @return True if this is a String field
    **/
    public boolean isString()
    {
        return DBProvider.isDataTypeString(this.typeMask);
    }

    /**
    *** Returns true if this is a binary (blob) field
    *** @return True if this is a binary field
    **/
    public boolean isBinary()
    {
        return DBProvider.isDataTypeBinary(this.typeMask);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the data type for this field.<br>
    *** (Allowed only for 'defined' fields)
    *** @param dataType The field data type
    **/
    public void setDataType(String dataType)
    {
        if (!this.isSqlField()) {
            this.dataType = (dataType != null)? dataType.toUpperCase() : "";
            this.typeMask = DBProvider.getDataTypeMask(this.dataType);
            // cache string lengths (from 'this.dataType')
            this.stringLength = -1;
            this.getStringLength();
        }
    }

    /**
    *** Gets the data type for this field
    *** @return The field data type
    **/
    public String getDataType()
    {
        return this.dataType;
    }
    
    /** 
    *** Returns true if this field matches the defined table field<br>
    *** (only meaningful for fields read from the actual table columns)
    *** @return True if this field matches the defined table field
    **/
    public boolean isTypeMatch()
    {
        return this.isTypeMatch;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the SQL type for this field
    *** @return The SQL type for this field
    **/
    public String getSqlType()
    {
        return this.getSqlType(true);
    }

    /**
    *** Gets the SQL type for this field
    *** @param inclNotNull  True to include "NOT NULL" specification, false to strip this specification
    *** @return The SQL type for this field
    **/
    public String getSqlType(boolean inclNotNull)
    {
        if (this.isSqlField()) {
            if (inclNotNull) {
                // return as-is
                return this.sqlType;
            } else {
                // remove "NOT NULL"
                return DBField._removeFieldType(this.sqlType,SQL_NOT_NULL);
            }
        } else {
            String st = DBProvider.getSqlTypeFromDataType(this.getDataType());
            if (this.isPrimaryKey()) {
                if (inclNotNull) {
                    if (DBField._hasFieldType(st,SQL_NOT_NULL)) {
                        // Primary key field already has "NOT NULL"
                        return st;
                    } else {
                        // append "NOT NULL"
                        return st + " " + SQL_NOT_NULL;
                    }
                } else {
                    // remove existing "NOT NULL" specification
                    return DBField._removeFieldType(st,SQL_NOT_NULL);
                }
            } else {
                if (inclNotNull) {
                    // return string as-is
                    return st;
                } else {
                    // remove existing "NOT NULL" specification
                    return DBField._removeFieldType(st,SQL_NOT_NULL);
                }
            }
        }
    }

    /**
    *** Returns true if 'target' contains 'match'
    **/
    private static boolean _hasFieldType(String target, String match)
    {
        int p = target.toUpperCase().indexOf(match.toUpperCase());
        return (p >= 0);
    }

    /**
    *** Removes 'match' String from 'target' String
    **/
    private static String _removeFieldType(String target, String match)
    {
        int p = target.toUpperCase().indexOf(match.toUpperCase());
        if (p >= 0) {
            return target.substring(0,p).trim() + target.substring(p + match.length());
        } else {
            return target;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the String length of this field, or '0' if the field is not a String
    *** @return The length of the String field
    **/
    public int getStringLength()
    {
        if (this.stringLength < 0) {
            String dt = this.getDataType(); // already uppercase
            if (dt.startsWith(TYPE_STRING + "[")) {
                String x = dt.substring(TYPE_STRING.length() + 1);
                this.stringLength = StringTools.parseInt(x, 0);
            } else {
                this.stringLength = 0;
            }
        }
        return this.stringLength;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns true if this field type is TEXT/CLOB
    *** @return True if this field type is TEXT/CLOB
    **/
    public boolean isCLOB()
    {
        String dt = this.getDataType(); // already uppercase
        return 
            dt.equalsIgnoreCase(TYPE_TEXT);
    }
    
    /** 
    *** Returns true if this field type is binary/BLOB
    *** @return True if this field type is binary/BLOB
    **/
    public boolean isBLOB()
    {
        String dt = this.getDataType(); // already uppercase
        return 
            dt.equalsIgnoreCase(TYPE_SBLOB) || 
            dt.equalsIgnoreCase(TYPE_BLOB)  || 
            dt.equalsIgnoreCase(TYPE_MBLOB);
    }
    
    private static boolean ALLOW_ESCAPE_CHARACTERS_IN_ASCII_BLOB = false;
    private static byte EMPTY_BLOB[] = new byte[0];
    
    /**
    *** Parses and returns a BLOB (byte[]) representation of the String value
    *** @param val  The String value to parse
    *** @return The BLOB/byte[] representation
    **/
    public static byte[] parseBlobString(String val)
    {
        return DBField.parseBlobString(val, EMPTY_BLOB);
    }

    /**
    *** Parses and returns a BLOB (byte[]) representation of the String value
    *** @param val  The String value to parse
    *** @param dftBlob  The default value returned if the String cannot be parsed
    *** @return The BLOB/byte[] representation
    **/
    public static byte[] parseBlobString(String val, byte dftBlob[])
    {
        
        /* null/empty value? */
        if (val == null) {
            return dftBlob;
        }
        
        /* standard Hex representation */
        if (val.startsWith("0x") || val.startsWith("0X")) {
            return StringTools.parseHex(val, dftBlob);
        }
        
        /* parse as escaped string */
        // parse as a string of characters
        //Print.logInfo("Parsing BLOB String: " + val);
        char ch[] = val.toCharArray();
        byte ba[] = new byte[ch.length];
        int b = 0;
        for (int i = 0; i < ch.length; i++) {
            if (ALLOW_ESCAPE_CHARACTERS_IN_ASCII_BLOB) {
                if ((ch[i] == '\\') && ((i + 1) < ch.length)) {
                    i++; // skip past '\\'
                    if (ch[i] == '0') { // "\0"
                        ba[b++] = (byte)0; 
                    } else
                    if (ch[i] == 'r') { // "\r"
                        ba[b++] = '\r';
                    } else
                    if (ch[i] == 'n') { // "\n"
                        ba[b++] = '\n';
                    } else
                    if (ch[i] == 't') { // "\t"
                        ba[b++] = '\t';
                    } else {
                        ba[b++] = (byte)ch[i];
                    }
                } else {
                    ba[b++] = (byte)ch[i];
                }
            } else {
                ba[b++] = (byte)ch[i];
            }
        }
        if (b < ba.length) {
            byte bb[] = new byte[b];
            System.arraycopy(ba, 0, bb, 0, b);
            ba = bb;
        }
        //Print.logInfo("Returned BLOB length = " + ba.length);
        return ba;

    }

    // ------------------------------------------------------------------------

    /**
    *** Extracts and returns the value for this field from the specified ResultSet
    *** @param rs  The ResultSet from which the value is extracted
    *** @return The extracted value for this field
    **/
    @SuppressWarnings("deprecation")
    public Object getResultSetValue(ResultSet rs)
        throws SQLException
    {
        String n = this.getName();
        Class<?> jvc = this.getTypeClass();
        if (jvc == String.class) {
            return (rs != null)? rs.getString(n) : "";
        } else
        if ((jvc == Integer.class) || (jvc == Integer.TYPE)) {
            return new Integer((rs != null)? rs.getInt(n) : 0);
        } else
        if ((jvc == Long.class) || (jvc == Long.TYPE)) {
            return new Long((rs != null)? rs.getLong(n) : 0L);
        } else
        if ((jvc == Float.class) || (jvc == Float.TYPE)) {
            return new Float((rs != null)? rs.getFloat(n) : 0.0F);
        } else
        if ((jvc == Double.class) || (jvc == Double.TYPE)) {
            return new Double((rs != null)? rs.getDouble(n) : 0.0);
        } else
        if ((jvc == Boolean.class) || (jvc == Boolean.TYPE)) {
            return new Boolean((rs != null)? (rs.getInt(n) != 0) : false);
        } else
        if ((jvc == Byte[].class) || (jvc == byte[].class)) {
            return (rs != null)? rs.getBytes(n) : new byte[0];
        } else
        if (jvc == DateTime.class) {
            // Note: the retrieved date should be in the UTC timezone.  The following
            // extracts the YYYY-MM-DD, hh:mm:ss, then explicitly creates a DateTime
            // instance with the UTC/GMT timezone.
            java.sql.Timestamp ts = (rs != null)? rs.getTimestamp(n) : null;
            if (ts != null) {
                int YY = ts.getYear() + 1900;
                int MM = ts.getMonth() + 1;
                int DD = ts.getDate();
                int hh = ts.getHours();
                int mm = ts.getMinutes();
                int ss = ts.getSeconds();
                return new DateTime(DateTime.getGMTTimeZone(), YY,MM,DD, hh,mm,ss);
            } else {
                return new DateTime(0L, DateTime.getGMTTimeZone());
            }
        } else
        if (DBFieldType.class.isAssignableFrom(jvc)) {
            try {
                Constructor<?> dbftConst = jvc.getConstructor(ResultSet.class, String.class);
                return dbftConst.newInstance(new Object[] { rs, n });
            } catch (Throwable t) { // NPE, NoSuchMethodException, InstantiationException, InvocationTargetException, etc.
                if (t instanceof SQLException) {
                    // this will not occur, but we check anyway 
                    throw (SQLException)t; // re-throw SQLExceptions
                } else
                if (t.getCause() instanceof SQLException) {
                    // This will typically be wrapped inside a 'InvocationTargetException'
                    throw (SQLException)t.getCause(); // re-throw SQLExceptions
                }
                Print.logException("Unable to instantiate", t);
                return null;
            }
        } else {
            Print.logError("Unsupported Java class: " + StringTools.className(jvc));
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the field value from the specified DBRecord
    *** @param rcd  The DBRecord from which the value is returned
    *** @return The returned value for this field
    **/
    public Object getFieldValue(DBRecord rcd)
    {
        return (rcd != null)? rcd.getFieldValue(this.getName()) : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses the specified String and returns an Object appropriate for this field data type
    *** @param val  The String value to parse
    *** @return An Object value appropriate for this field data type
    **/
    public Object parseStringValue(String val)
    {
        Class<?> jvc = this.getTypeClass();
        if (jvc == String.class) {
            return (val != null)? val : "";
        } else
        if ((jvc == Integer.class) || (jvc == Integer.TYPE)) {
            return new Integer(StringTools.parseInt(val,0));
        } else
        if ((jvc == Long.class) || (jvc == Long.TYPE)) {
            return new Long(StringTools.parseLong(val,0L));
        } else
        if ((jvc == Float.class) || (jvc == Float.TYPE)) {
            return new Float(StringTools.parseFloat(val,0.0F));
        } else
        if ((jvc == Double.class) || (jvc == Double.TYPE)) {
            return new Double(StringTools.parseDouble(val,0.0));
        } else
        if ((jvc == Boolean.class) || (jvc == Boolean.TYPE)) {
            return new Boolean(StringTools.parseBoolean(val,false));
        } else
        if ((jvc == Byte[].class) || (jvc == byte[].class)) {
            return DBField.parseBlobString(val);
        } else
        if (jvc == DateTime.class) {
            try { // "2009-12-12 12:12:00 TMZ"
                DateTime.ParsedDateTime pdt = DateTime.parseDateTime(val, DateTime.getGMTTimeZone(), DateTime.DefaultParsedTime.DayStart);
                return pdt.createDateTime();
            } catch (DateTime.DateParseException dpe) {
                return new DateTime(0L);
            }
        } else
        if (DBFieldType.class.isAssignableFrom(jvc)) {
            //Print.logInfo("Parsing DBFieldType: " + StringTools.className(jvc) + " >" + val + " [" + val.length() + "]");
            try {
                Constructor<?> dbftConst = jvc.getConstructor(String.class);
                return dbftConst.newInstance(new Object[] { val });
            } catch (Throwable t) { 
                // NPE, NoSuchMethodException, InstantiationException, InvocationTargetException, etc.
                Print.logError("Unable to obtain proper constructor: " + t);
                return null;
            }
        } else {
            Print.logError("Unsupported Java class: " + StringTools.className(jvc));
            return null;
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets a default value for this field
    *** Note: The data type of the specified default value is assumed to be
    *** correct for this field type.
    *** @param val  The default value
    **/
    public void setDefaultValue(Object val)
    {
        this.defaultValue = val;
    }
    
    /**
    *** Returns true if an explicit default value has been defined for this field
    *** @return True if an explicit default value has been defined for this field
    **/
    public boolean _hasDefaultValue()
    {
        return (this.defaultValue != null);
    }

    /**
    *** Gets the explicitly defined default value for this field
    *** @return The explicitly defined default field value
    **/
    public Object _getDefaultValue()
    {
        return this.defaultValue;
    }

    /**
    *** Gets a default value for this field
    *** @return A default field value
    **/
    public Object getDefaultValue()
    {
        if (this.defaultValue != null) {
            return this.defaultValue;
        } else {
            try {
                return this.getResultSetValue(null);
            } catch (SQLException sqe) {
                // this will(should) never occur
                return null;
            }
        }
    }
    
    /**
    *** Returns true if the specified value matches a default value
    *** (currently, there can be only one default value)
    *** @return True if the specified value matches a default value
    **/
    public boolean isDefaultValue(Object val)
    {
        if (val == null) {
            return false;
        } else
        if (this._hasDefaultValue()) {
            return val.equals(this._getDefaultValue());
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if values represented by this field data type must be quoted
    *** @return True if values represented by this field data type must be quoted
    **/
    public boolean quoteValue()
    {
        if (this.isCLOB()) {
            return true; // TYPE_TEXT
        } else
        if (this.isBLOB()) {
            // This assumes that the value is presented in raw hex form.
            // If the value is presented in hex, quoting may produce invalid results.
            return true; // TYPE_SBLOB || TYPE_BLOB || TYPE_MBLOB
        } else
        if (this.isTypeDateTime()) {
            return true; // TYPE_DATETIME
        } else {
            String dt = this.getDataType(); // already uppercase
            return dt.startsWith(TYPE_STRING);
        }
    }

    /**
    *** Returns a quoted String value for the specified object
    *** @param v  The Object to quote
    *** @return The quoted String value
    **/
    public String getQValue(Object v)
    {
        String vs = DBFieldValues.toStringValue(v);
        if (this.isBLOB()) {
            int dbid = DBProvider.getProvider().getID();
            if (dbid == DBProvider.DB_DERBY) {
                // Derby: CAST(x'FFFF' AS BLOB)
                String hex = vs.startsWith("0x")? vs.substring(2) : vs;
                // TODO: make sure 'hex' contains only valid hex characters
                // (StringTools.hexLength(hex) == hex.length())
                return "CAST(x'" + hex + "' AS BLOB)";
            } else {
                // DBProvider.DB_DERBY
                // MySQL: 0xFFFF
                if (vs.equals("") || vs.equalsIgnoreCase("0x")) {
                    return DBField.quote("");
                } else {
                    String hex = vs.startsWith("0x")? vs : ("0x" + vs);
                    // TODO: make sure 'hex' contains only valid hex characters
                    // (StringTools.hexLength(hex) == hex.length())
                    return hex;
                }
            }
        } else {
            return (this.quoteValue() || vs.equals(""))? DBField.quote(vs) : vs;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this field contains the specified attribute
    *** @param key  The attribute key
    *** @return True if this field contains the specified attribute
    **/
    public boolean hasAttribute(String key)
    {
        return this.attr.hasProperty(key);
    }

    /**
    *** Returns the boolean value of the specified attribute key
    *** @param key  The attribute key
    *** @param dft  The default boolean value if the attribute key does not exist
    *** @return The boolean value of the specified attribute key
    **/
    public boolean getBooleanAttribute(String key, boolean dft)
    {
        return this.hasAttribute(key)? this.attr.getBoolean(key,true) : dft;
    }

    /**
    *** Returns the integer value of the specified attribute key
    *** @param key  The attribute key
    *** @param dft  The default integer value if the attribute key does not exist
    *** @return The integer value of the specified attribute key
    **/
    public int getIntAttribute(String key, int dft)
    {
        return this.attr.getInt(key, dft);
    }

    /**
    *** Returns the String value of the specified attribute key
    *** @param key  The attribute key
    *** @param dft  The default 'String' if the attribute key is not defined
    *** @return The String value of the specified attribute key
    **/
    public String getStringAttribute(String key, String dft)
    {
        return this.attr.getString(key, dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the 'format' String attribute
    *** @param format The 'format' String attribute
    **/
    public void setFormat(String format)
    {
        this.attr.setString(ATTR_FORMAT, format);
    }

    /**
    *** Returns the 'format' String attribute
    *** @return The 'format' String attribute
    **/
    public String getFormat()
    {
        return this.getFormat(null);
    }

    /**
    *** Returns the 'format' String attribute
    *** @param dft  The default 'format' returned if undefined
    *** @return The 'format' String attribute
    **/
    public String getFormat(String dft)
    {
        String fmt = this.getStringAttribute(ATTR_FORMAT, dft);
        if ((fmt != null) && fmt.startsWith("X")) {
            DBProvider dbp = DBProvider.getProvider();
            if (dbp.getID() == DBProvider.DB_DERBY) { return null; }
        }
        return fmt;
    }

    /**
    *** Returns the specified value as a String formatted appropriately for this field type
    *** @param val  The value to format
    *** @return The formated value
    **/
    public String formatValue(Object val)
    {

        /* null value? */
        if (val == null) {
            return null;
        }

        /* type */
        Class typeClass = this.getTypeClass();

        /* String? */
        if (typeClass == String.class) {
            return val.toString();
        }

        /* format */
        String fmt = this.getFormat();
        if (StringTools.isBlank(fmt)) {
            fmt = null;
        }

        /* numeric */
        if (val instanceof Number) {
            if ((typeClass == Double.class) || (typeClass == Double.TYPE)) {
                double d = ((Number)val).doubleValue();
                return (fmt == null)? String.valueOf(d) : StringTools.format(d,fmt);
            } else
            if ((typeClass == Float.class) || (typeClass == Float.TYPE)) {
                float d = ((Number)val).floatValue();
                return (fmt == null)? String.valueOf(d) : StringTools.format(d,fmt);
            } else
            if ((typeClass == Long.class) || (typeClass == Long.TYPE)) {
                long d = ((Number)val).longValue();
                return (fmt == null)? String.valueOf(d) : StringTools.format(d,fmt);
            } else
            if ((typeClass == Integer.class) || (typeClass == Integer.TYPE)) {
                int d = ((Number)val).intValue();
                return (fmt == null)? String.valueOf(d) : StringTools.format(d,fmt);
            }
            if ((typeClass == Short.class) || (typeClass == Short.TYPE)) {
                int d = ((Number)val).shortValue();
                return (fmt == null)? String.valueOf(d) : StringTools.format(d,fmt);
            }
        }

        /* byte array */
        if (val instanceof byte[]) {
            String hex = StringTools.toHexString((byte[])val);
            return "0x" + hex;
        }

        /* DateTime */
        if (val instanceof DateTime) {
            DateTime dt = (DateTime)val;
            return dt.format("yyyy-MM-dd HH:mm:ss", DateTime.getGMTTimeZone());
        }

        /* default value */
        return val.toString();

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified object is equivalent to this DBField
    *** @return True if the specified object is equivalent to this DBField
    **/
    public boolean equals(Object other)
    {
        if (!(other instanceof DBField)) { 
            return false; 
        }
        DBField fld = (DBField)other;

        /* check field name */
        if (!this.getName().equals(fld.getName())) {
            return false;
        }

        /* check keys */
        if (this.isPrimaryKey() != fld.isPrimaryKey()) {
            return false;
        } else
        if (this.isAlternateKey() != fld.isAlternateKey()) {
            return false;
        } else
        if (!this.equalsAlternateIndexes(fld.getAlternateIndexes())) {
            return false;
        }

        /* characer set */
        if (this.isUTF8() != fld.isUTF8()) {
            return false;
        }

        /* check data type */
        String thisDT = this.getDataType();
        String othrDT = fld.getDataType();
        if (!thisDT.equals(othrDT)) {
            /* special case for checking fields retrieved directly from the table */
            // if either field is from the SQL table, then TYPE_INT8 is the same as TYPE_BOOLEAN.
            boolean thisBool = (this.isSqlField() && thisDT.equals(TYPE_INT8)) || thisDT.equals(TYPE_BOOLEAN);
            boolean othrBool = ( fld.isSqlField() && othrDT.equals(TYPE_INT8)) || othrDT.equals(TYPE_BOOLEAN);
            if (!thisBool || !othrBool) {
                return false;
            }
        }

        /* match */
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the untranslated field name
    *** @return  The field name
    **/
    public String _getName()
    {
        return this.name;
    }

    /**
    *** Gets the translated field name (as translated to the actual DB column name)
    *** @return  The field name
    **/
    public String getName()
    {
        return DBProvider.translateColumnName(this._getName());
    }

    /**
    *** Gets the SQL definition of this field
    *** (NOTE: does not contain character set information!)
    *** @return The SQL definition of this field
    **/
    public String getFieldDefinition()
    {
        // This is used by "DBFactory._createTable()"
        return this.getName() + " " + this.getSqlType();
    }

    /**
    *** Returns a String representation of this field (for display purposes only)
    *** @return A String representation of this field
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getFieldDefinition());
        this._getIndexNames(sb);
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String list of index names for this field (for display purposes only)
    *** @return A String list of index names for this field
    **/
    public String getIndexNames()
    {
        StringBuffer sb = this._getIndexNames(null);
        return (sb != null)? sb.toString() : "";
    }
    
    /**
    *** Returns a String list of index names for this field (for display purposes only)
    *** @param sb  The StringBuffer where the index names will be placed
    *** @return The StringBuffer where the index names were placed
    **/
    protected StringBuffer _getIndexNames(StringBuffer sb)
    {
        boolean priKey = this.isPrimaryKey();
        boolean altKey = this.isAlternateKey();
        if (!priKey && !altKey) {
            return sb;
        } else {
            if (sb == null) { sb = new StringBuffer(); }
            if (sb.length() > 0) { sb.append(" "); }
            if (priKey) {
                sb.append(DBProvider.PRIMARY_INDEX_NAME);
            }
            if (altKey) {
                if (priKey) { sb.append(","); }
                sb.append(StringTools.join(this.getAlternateIndexes(),","));
            }
            return sb;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the I18N title for this field
    *** @param title  The I18N title for this field
    **/
    public void setTitle(I18N.Text title)
    {
        this.i18nTitle = title;
    }

    /**
    *** Gets the I18N title for this field
    *** @return  The I18N title for this field
    **/
    public I18N.Text getTitle()
    {
        return this.i18nTitle;
    }

    /**
    *** Returns the 'title' String attribute
    *** @param locale The Locale used for Localization purposes
    *** @return The 'title' String attribute
    **/
    public String getTitle(Locale locale)
    {
        return (this.i18nTitle != null)? this.i18nTitle.toString(locale) : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the edit mode for this field
    *** @param mode  The edit mode for this field
    **/
    public void setEditMode(int mode)
    {
        switch (mode) {
            case EDIT_NEVER : // -1;
            case EDIT_NEW   : // 0;
            case EDIT_ADMIN : // 1;
            case EDIT_PUBLIC: // 2;
                this.editMode = mode;
                break;
            default         :
                this.editMode = EDIT_NEVER;
                break;
        }
    }

    /**
    *** Gets the edit mode for this field
    *** @return  The edit mode for this field
    **/
    public int getEditMode()
    {
        return this.editMode;
    }

    /**
    *** Returns true if this field is editable for the specified mode
    *** @param mode  The edit mode
    *** @return True if this field is editable for the specified mode
    **/
    public boolean isEditable(int mode)
    {
        DBFactory dbf = this.getFactory();
        if ((dbf != null) && !dbf.isEditable()) {
            return false;
        } else {
            return (mode <= this.editMode);
        }
    }
    
    /**
    *** Gets the field editor name
    *** @return The field editor name
    **/
    public String getEditor()
    {
        return this.getStringAttribute(ATTR_EDITOR, null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the defined Enum class type for this field
    *** @return The Enum class defined for this field
    **/
    public Class<? extends Enum> getEnumClass()
    {
        if (!this.enumInit) {
            this.enumInit = true;
            String enumValue = this.getStringAttribute(ATTR_ENUM, null);
            if (!StringTools.isBlank(enumValue)) {
                if (enumValue.indexOf("$") < 0) {
                    DBFactory fact = this.getFactory();
                    if (fact != null) {
                        this.enumClass = EnumTools.getEnumClass(fact.getRecordClass(), enumValue);
                    }
                }
                if (this.enumClass == null) {
                    this.enumClass = EnumTools.getEnumClass(enumValue);
                }
                if (this.enumClass != null) {
                    Enum e[] = this.enumClass.getEnumConstants();
                    if ((e == null) || (e.length == 0)) {
                        this.enumClass = null;
                    }
                }
                if (this.enumClass == null) {
                    Print.logWarn("Enum class not found: [%s] %s", this.getName(), enumValue);
                }
            }
        }
        return this.enumClass; // may be null
    }

    /**
    *** Gets the map of the valid enumerated values for this field
    *** @return The map of the valid enumerated values for this field, or null if undefined
    **/
    public OrderedMap<String,Integer> getEnumValues()
    {

        /* look up the registered Enum name */
        Class<? extends Enum> eclz = this.getEnumClass();
        if (eclz != null) {
            Enum e[] = eclz.getEnumConstants();
            OrderedMap<String,Integer> map = new OrderedMap<String,Integer>();
            for (int n = 0; n < e.length; n++) {
                if (e[n] instanceof EnumTools.IntValue) {
                    map.put(e[n].toString(), ((EnumTools.IntValue)e[n]).getIntValue());
                }
            }
            if (!map.isEmpty()) {
                return map;
            } else {
                Print.logError("No emunerated values found for field: " + this.getName());
                return null;
            }
        }
        
        /* custom Enum values */
        String enumValue = this.getStringAttribute(ATTR_ENUM, null);
        if (!StringTools.isBlank(enumValue) && (enumValue.indexOf(ENUM_VALUE_SEPARATOR) >= 0)) {
            String ev[] = StringTools.parseString(enumValue, ENUM_TYPE_SEPARATOR);
            if (ev.length > 0) {
                OrderedMap<String,Integer> map = new OrderedMap<String,Integer>();
                for (int i = 0; i < ev.length; i++) {
                    String v[] = StringTools.parseString(ev[i], ENUM_VALUE_SEPARATOR);
                    if ((v.length >= 2) && !StringTools.isBlank(v[1])) {
                        int n = StringTools.parseInt(v[0],0);
                        map.put(v[1], new Integer(n));
                    }
                }
                return !map.isEmpty()? map : null;
            } else {
                return null;
            }
        }

        /* not found */
        return null;

    }

    // ------------------------------------------------------------------------

    /**
    *** Return the defined Enum bitmask class type for this field
    *** @return The Enum bitmask class defined for this field
    **/
    public Class<? extends Enum> getMaskClass()
    {
        if (!this.maskInit) {
            this.maskInit = true;
            String maskValue = this.getStringAttribute(ATTR_MASK, null);
            if (!StringTools.isBlank(maskValue)) {
                if (maskValue.indexOf("$") < 0) {
                    DBFactory fact = this.getFactory();
                    if (fact != null) {
                        this.maskClass = EnumTools.getEnumClass(fact.getRecordClass(), maskValue);
                    }
                }
                if (this.maskClass == null) {
                    this.maskClass = EnumTools.getEnumClass(maskValue);
                }
                if (this.maskClass != null) {
                    Enum e[] = this.maskClass.getEnumConstants();
                    if ((e == null) || (e.length == 0)) {
                        this.maskClass = null;
                    } else
                    if (!EnumTools.BitMask.class.isAssignableFrom(this.maskClass)) {
                        this.maskClass = null;
                    }
                }
                if (this.maskClass == null) {
                    Print.logWarn("Mask class not found: [%s] %s", this.getName(), maskValue);
                }
            }
        }
        return this.maskClass; // may be null
    }

    /**
    *** Gets the map of the valid enumerated values for this field
    *** @return The map of the valid enumerated values for this field, or null if undefined
    **/
    public OrderedMap<String,Long> getMaskValues()
    {

        /* look up the registered Enum name */
        Class<? extends Enum> eclz = this.getMaskClass();
        if ((eclz != null) && EnumTools.BitMask.class.isAssignableFrom(eclz)) {
            Enum e[] = eclz.getEnumConstants();
            OrderedMap<String,Long> map = new OrderedMap<String,Long>();
            for (int n = 0; n < e.length; n++) {
                map.put(e[n].toString(), ((EnumTools.BitMask)e[n]).getLongValue());
            }
            if (!map.isEmpty()) {
                return map;
            } else {
                Print.logError("No emunerated values found for field: " + this.getName());
                return null;
            }
        }
        
        /* custom Enum values */
        String maskValue = this.getStringAttribute(ATTR_MASK, null);
        if (!StringTools.isBlank(maskValue) && (maskValue.indexOf(MASK_VALUE_SEPARATOR) >= 0)) {
            String ev[] = StringTools.parseString(maskValue, MASK_TYPE_SEPARATOR);
            if (ev.length > 0) {
                OrderedMap<String,Long> map = new OrderedMap<String,Long>();
                for (int i = 0; i < ev.length; i++) {
                    String v[] = StringTools.parseString(ev[i], MASK_VALUE_SEPARATOR);
                    if ((v.length >= 2) && !StringTools.isBlank(v[1])) {
                        long n = StringTools.parseLong(v[0],0L);
                        map.put(v[1], n);
                    }
                }
                return !map.isEmpty()? map : null;
            } else {
                return null;
            }
        }

        /* not found */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /*
    public static final String HIB_BOOLEAN      = "boolean";           //  8bit          Java 'boolean'
    public static final String HIB_INT8         = "byte";              //  8bit (signed) Java 'byte'
    public static final String HIB_UINT8        = "unsigned byte";     //  8bit          Java 'byte'
    public static final String HIB_INT16        = "short";             // 16bit (signed)
    public static final String HIB_UINT16       = "unsigned short";    // 16bit (signed)
    public static final String HIB_INT32        = "integer";           // 32bit (signed) Java 'int'
    public static final String HIB_UINT32       = "unsigned integer";  // 32bit          Java 'int'
    public static final String HIB_INT64        = "long";              // 64bit (signed) Java 'long'
    public static final String HIB_UINT64       = "unsigned long";     // 64bit          Java 'long'
    public static final String HIB_FLOAT        = "float";
    public static final String HIB_DOUBLE       = "double";
    public static final String HIB_BINARY       = "binary";            // max (2^16 - 1) bytes
    public static final String HIB_MBLOB        = "bin24";             // max (2^24 - 1) bytes
    public static final String HIB_TEXT         = "text";              // max (2^16 - 1) bytes
    public static final String HIB_STRING       = "string";
    public static String HIB_STRING(int size) { return HIB_STRING + "(" + size + ")"; }

    private String _getHibernateType()
    {
        String dt = this.getDataType();
        if (dt.equals(TYPE_BOOLEAN)) {
            return HIB_BOOLEAN;
        } else
        if (dt.equals(TYPE_INT8)) {
            return HIB_INT8;
        } else
        if (dt.equals(TYPE_UINT8)) {
            return HIB_UINT8;
        } else
        if (dt.equals(TYPE_INT16)) {
            return HIB_INT16;
        } else
        if (dt.equals(TYPE_UINT16)) {
            return HIB_UINT16;
        } else
        if (dt.equals(TYPE_INT32)){
            return HIB_INT32;
        } else
        if (dt.equals(TYPE_UINT32)) {
            return HIB_UINT32;
        } else
        if (dt.equals(TYPE_INT64)) {
            return HIB_INT64;
        } else
        if (dt.equals(TYPE_UINT64)) {
            return HIB_UINT64;
        } else
        if (dt.equals(TYPE_FLOAT)) {
            return HIB_FLOAT;
        } else
        if (dt.equals(TYPE_DOUBLE)) {
            return HIB_DOUBLE;
        } else
        if (dt.equals(TYPE_BLOB)) {
            return HIB_BINARY;
        } else
        if (dt.equals(TYPE_MBLOB)) {
            return HIB_MBLOB;
        } else
        if (dt.equals(TYPE_TEXT)) {
            return HIB_TEXT;
        } else
        if (dt.startsWith(TYPE_STRING + "[")) {
            //String x = dt.substring(TYPE_STRING.length() + 1);
            //int len = StringTools.parseInt(x, 32);
            //return HIB_STRING(len);
            return HIB_STRING;
        } else {
            Print.logError("Unrecognized type: " + dt);
            return HIB_STRING(32);
        }
    }
    
    public String getHibernateType()
    {
        String hibType = _getHibernateType();
        return hibType;
    }
    */

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        String s = "This is a ("+SQL_NOT_NULL+") test";
        Print.sysPrintln("%s", s);
        if (DBField._hasFieldType(s,SQL_NOT_NULL)) {
            Print.sysPrintln("==> %s", DBField._removeFieldType(s,SQL_NOT_NULL));
        }

    }
    
}
