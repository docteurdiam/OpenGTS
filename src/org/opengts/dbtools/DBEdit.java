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
//  2007/11/28  Martin D. Flynn
//     -Added ability to display 'all' fields
//  2008/06/20  Martin D. Flynn
//     -When getting/setting field values, the DBRecord 'setValue' methos is used.
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBEdit</code> is used by a command-line tool to allow editing a table record.
**/

public class DBEdit
{

    // ------------------------------------------------------------------------

    private static final String LINE_SEPARATOR = "-----------------------------------------";
    
    private static final int    EDIT_MODE = DBField.EDIT_ADMIN;
    
    // ------------------------------------------------------------------------

    private DBRecordKey recordKey = null;
    private DBField     editableFields[] = null;
    
    /**
    *** Constructor
    *** @param key  The DBrecordKey to edit
    **/
    public DBEdit(DBRecordKey key)
    {
        this.recordKey = key;
        this.recordKey.getDBRecord(true);
    }
    
    /**
    *** Constructor
    *** @param rcd  The DBRecord to edit
    **/
    public DBEdit(DBRecord rcd)
    {
        this(rcd.getRecordKey());
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns all table fields
    *** @return All table fields
    **/
    private DBField[] getAllFields()
    {
        return this.recordKey.getFields();
    }

    /**
    *** Returns editable table fields
    *** @return Editable table fields
    **/
    private DBField[] getEditableFields()
    {
        if (this.editableFields == null) {
            DBField fld[] = this.recordKey.getFields();
            java.util.List<DBField> fldList = new Vector<DBField>();
            for (int i = 0; i < fld.length; i++) {
                if (fld[i].isEditable(EDIT_MODE)) {
                    fldList.add(fld[i]);
                }
            }
            this.editableFields = fldList.toArray(new DBField[fldList.size()]);
        }
        return this.editableFields;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Displays editable fields to the console
    *** @param allFlds  True to display all fields, false to show only editable fields
    **/
    @SuppressWarnings("unchecked")
    public void print(boolean allFlds) 
    {
        Print.sysPrintln("");
        Print.sysPrintln(LINE_SEPARATOR);
        Print.sysPrintln("Key: " + this.recordKey);
        Print.sysPrintln(LINE_SEPARATOR);
        DBField fld[] = allFlds? this.getAllFields() : this.getEditableFields();
        DBRecord dbRcd = this.recordKey.getDBRecord();
        for (int i = 0; i < fld.length; i++) {
            boolean editable   = fld[i].isEditable(EDIT_MODE);
            boolean isPriKey   = fld[i].isPrimaryKey();
            boolean isAltKey   = fld[i].isAlternateKey(); // see also '<DBField>.getAlternateIndexes()'
            OrderedMap<String,Integer> enumMap = fld[i].getEnumValues();
            OrderedMap<String,Long>    maskMap = fld[i].getMaskValues();
            String ndx   = StringTools.rightJustify(String.valueOf(i), 2);
            String title = StringTools.leftJustify(fld[i].getTitle(null), 35);
            Object value = dbRcd.getValue(fld[i].getName());
            StringBuffer sb = new StringBuffer();
            // Field index
            sb.append(ndx);
            if (editable) {
                sb.append(") ");
            } else {
                sb.append("- ");
            }
            // title
            sb.append(title);
            // key info
            if (isPriKey) {
                sb.append("[K]");
            } else
            if (isAltKey) {
                sb.append("[A]");
            } else {
                sb.append("   ");
            }
            // field/value separator
            if (editable) {
                sb.append(": ");
            } else {
                sb.append("- ");
            }
            // value
            if (value != null) {
                sb.append("\"").append(fld[i].formatValue(value)).append("\"");
                if (enumMap != null) {
                    int v = (value instanceof Number)? ((Number)value).intValue() : -1;
                    Object e = EnumTools.getValueOf(fld[i].getEnumClass(), v);
                    sb.append("  [").append(e.toString()).append("]");
                } else
                if (maskMap != null) {
                    long v = (value instanceof Number)? ((Number)value).longValue() : 0L;
                    Enum m[] = EnumTools.getValuesForMask(fld[i].getMaskClass(), v);
                    if (m != null) {
                        sb.append("  [");
                        for (int e = 0; e < m.length; e++) {
                            if (e > 0) { sb.append(","); }
                            sb.append(m[e].toString());
                        }
                        sb.append("]");
                    }
                }
            } else {
                sb.append("null");
            }
            // print
            Print.sysPrintln(sb.toString());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Edit the specified field
    *** @param fld The field to edit
    *** @return True if the field was successfully edited
    *** @throws IOException if an I/O error occurs in input
    **/
    public boolean editField(DBField fld)
        throws IOException
    {
        String name = fld.getName();
        Class<?> type = fld.getTypeClass();
        
        /* enum/mask */
        OrderedMap<String,Integer> enumMap = fld.getEnumValues();
        OrderedMap<String,Long>    maskMap = fld.getMaskValues();
        String extType = (enumMap != null)? " [Enum]" : (maskMap != null)? " [BitMask]" : "";
        
        /* header */
        Print.sysPrintln("");
        Print.sysPrintln(LINE_SEPARATOR);
        Print.sysPrintln("Field: " + name);
        Print.sysPrintln("Title: " + fld.getTitle(null));
        Print.sysPrintln("Type : " + type + extType);
        if (enumMap != null) {
            StringBuffer sb = new StringBuffer();
            for (Iterator<String> e = enumMap.keyIterator(); e.hasNext();) {
                String  k = e.next();
                Integer v = enumMap.get(k);
                sb.append(v).append("=").append(k);
                if (e.hasNext()) { sb.append(", "); }
            }
            Print.sysPrintln("Enum : " + sb);
        } else
        if (maskMap != null) {
            StringBuffer sb = new StringBuffer();
            for (Iterator<String> e = maskMap.keyIterator(); e.hasNext();) {
                String  k = e.next();
                Long    v = maskMap.get(k);
                sb.append(fld.formatValue(v)).append("=").append(k);
                if (e.hasNext()) { sb.append(", "); }
            }
            Print.sysPrintln("Mask : " + sb);
        }
        DBRecord dbRcd = this.recordKey.getDBRecord();
        Print.sysPrintln("Value: " + fld.formatValue(dbRcd.getValue(name)));
        
        /* editable? */
        if (!fld.isEditable(EDIT_MODE)) {
            Print.sysPrintln("(Field is not editable)");
            return false;
        }

        /* new value */
        DBRecord dbrcd = this.recordKey.getDBRecord();
        for (;;) {
            Print.sysPrint("Enter new value: ");
            String line = FileTools.readLine_stdin().trim();
            if (StringTools.isBlank(line)) {
                return false;
            } else
            if (type == String.class) {
                String val = line.equals("\\")? "" : line;
                dbrcd.setValue(name, val);
                return true;
            } else
            if ((type == Integer.class) || (type == Integer.TYPE)) {
                int val = (int)StringTools.parseLong(line, Integer.MIN_VALUE);
                if (val != Integer.MIN_VALUE) {
                    dbrcd.setValue(name, val);
                    return true;
                }
            } else
            if ((type == Long.class) || (type == Long.TYPE)) {
                String fmt = StringTools.trim(fld.getFormat());
                if (fmt.equals("date") && (line.indexOf("/") >= 0)) {
                    try {
                        DateTime.ParsedDateTime pdt = DateTime.parseDateTime(line, null, DateTime.DefaultParsedTime.DayStart);
                        dbrcd.setValue(name, pdt.getDayNumber());
                        return true;
                    } catch (DateTime.DateParseException dpe) {
                        Print.logError("Unable to parse date: " + line + " [" + dpe.getMessage() + "]");
                        return false;
                    }
                } else
                if (fmt.equals("time") && ((line.indexOf("/") >= 0) || (line.indexOf(":") >= 0))) {
                    try {
                        DateTime.ParsedDateTime pdt = DateTime.parseDateTime(line, null, DateTime.DefaultParsedTime.DayStart);
                        dbrcd.setValue(name, pdt.getEpochTime());
                        return true;
                    } catch (DateTime.DateParseException dpe) {
                        Print.logError("Unable to parse date/time: " + line + " [" + dpe.getMessage() + "]");
                        return false;
                    }
                } else {
                    long val = StringTools.parseLong(line, Long.MIN_VALUE);
                    if (val != Long.MIN_VALUE) {
                        dbrcd.setValue(name, val);
                        return true;
                    }
                }
            } else
            if ((type == Float.class) || (type == Float.TYPE)) {
                float val = (float)StringTools.parseDouble(line, Float.MIN_VALUE);
                if (val != Float.MIN_VALUE) {
                    dbrcd.setValue(name, val);
                    return true;
                }
            } else
            if ((type == Double.class) || (type == Double.TYPE)) {
                double val = StringTools.parseDouble(line, Double.MIN_VALUE);
                if (val != Double.MIN_VALUE) {
                    dbrcd.setValue(name, val);
                    return true;
                }
            } else
            if ((type == Boolean.class) || (type == Boolean.TYPE)) {
                if (StringTools.isBoolean(line,true)) {
                    boolean val = StringTools.parseBoolean(line, false);
                    dbrcd.setValue(name, val);
                    return true;
                }
            } else
            if (type == DateTime.class) {
                try {
                    DateTime dt = DateTime.parseArgumentDate(line);
                    dbrcd.setValue(name, dt);
                    return true;
                } catch (DateTime.DateParseException dpe) {
                    Print.logError("Unable to parse date: " + line + " [" + dpe.getMessage() + "]");
                    return false;
                }
            } else
            if (type == DayNumber.class) {
                try {
                    DateTime.ParsedDateTime pdt = DateTime.parseDateTime(line, null, DateTime.DefaultParsedTime.DayStart);
                    dbrcd.setValue(name, pdt.getDayNumber());
                    return true;
                } catch (DateTime.DateParseException dpe) {
                    Print.logError("Unable to parse date: " + line + " [" + dpe.getMessage() + "]");
                    return false;
                }
            } else
            if (DBFieldType.class.isAssignableFrom(type)) {
                try {
                    Constructor<?> typeConst = type.getConstructor(String.class);
                    Object val = typeConst.newInstance(new Object[] { line });
                    dbrcd.setValue(name, val);
                    return true;
                } catch (Throwable t) { // NoSuchMethodException
                    Print.logError("ERROR: Unable to parse this field type");
                    return false;
                }
            } else {
                Print.logError("ERROR: Unable to parse this field type");
                return false;
            }
            Print.logError("Entered value is improper type");
        }
        
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Display edit session
    *** @return True if the edited record was saved
    *** @throws IOException if an I/O error occurs in input
    **/
    public boolean edit()
        throws IOException
    {
        try {
            return this._edit(false);
        } catch (IOException ioe) {
            Print.sysPrintln("");
            throw ioe;
        }
    }

    /**
    *** Display edit session
    *** @param allFlds  True to display/edit all fields, false to display/edit only editable fields
    *** @return True if the edited record was saved
    *** @throws IOException if an I/O error occurs in input
    **/
    public boolean edit(boolean allFlds)
        throws IOException
    {
        try {
            return this._edit(allFlds);
        } catch (IOException ioe) {
            Print.sysPrintln("");
            throw ioe;
        }
    }
    
    /**
    *** Display edit session
    *** @param allFlds  True to display/edit all fields, false to display/edit only editable fields
    *** @return True if the edited record was saved
    *** @throws IOException if an I/O error occurs in input
    **/
    protected boolean _edit(boolean allFlds)
        throws IOException
    {
        Print.sysPrintln("");
        Print.sysPrintln(LINE_SEPARATOR);
        Print.sysPrintln("Editing table: " + this.recordKey.getUntranslatedTableName());
        Print.sysPrintln("Record Key   : " + this.recordKey);
        Print.sysPrintln(LINE_SEPARATOR);
        Print.sysPrintln("Commands:");
        Print.sysPrintln("   ##   - Field number to edit");
        Print.sysPrintln("   save - Save changes and exit");
        Print.sysPrintln("   exit - Exit without saving changes");
        DBField fld[] = allFlds? this.getAllFields() : this.getEditableFields();
        for (;;) {
            
            /* field prompt */
            Print.sysPrintln("");
            this.print(allFlds);
            Print.sysPrint("Enter field number [or 'save','exit']: ");
            String line = FileTools.readLine_stdin().trim();
            if (StringTools.isBlank(line)) {
                continue;
            }
            
            /* commands */
            if (line.equalsIgnoreCase("exit")) {
                Print.sysPrintln("\nExiting, record not saved");
                Print.sysPrintln("");
                return false;
            } else
            if (line.equalsIgnoreCase("save")) {
                DBRecord rcd = this.recordKey.getDBRecord();
                try {
                    rcd.save();
                    Print.sysPrintln("\nRecord saved");
                    Print.sysPrintln("");
                    return true;
                } catch (DBException dbe) {
                    Print.sysPrintln("\nERROR: Unable to save record!");
                    dbe.printException();
                    Print.sysPrintln("");
                    return false;
                }
            }
            
            /* selected field index */
            long fldNdx = StringTools.parseLong(line, -1L);
            if ((fldNdx < 0L) || (fldNdx >= fld.length)) {
                continue;
            }
            
            /* edit field */
            this.editField(fld[(int)fldNdx]);

        }
    }

    // ------------------------------------------------------------------------

}
