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
//  2006/06/05  Martin D. Flynn
//     -Moved to "OpenGTS"
//  2008/03/28  Martin D. Flynn
//     -Changed 'Field' innner class to accept field definitions in either of the
//      following formats: [H/L]|<type>|<ndx>|<len> or  <type>|[H/L]|<ndx>|<len>
// ----------------------------------------------------------------------------
package org.opengts.dbtypes;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

public class DTTemplate
    extends DBFieldType
{

    // ------------------------------------------------------------------------
    //  boolean hiRes   = false; // 0..1
    //  int     fldType = -1;    // 0..128
    //  int     fldNdx  = 0;     // 0..255
    //  int     fldLen  = 0;     // 0..255
    // "0=H|23|0|3"

    // ------------------------------------------------------------------------
    
    private RTProperties templateProps = null;
    private int          fieldCount    = -1;
    
    /**
    *** Default constructor
    **/
    public DTTemplate()
    {
        super();
        this.templateProps = new RTProperties("");
        this.fieldCount    = -1;
    }
    
    /** 
    *** Constructor
    *** @param template The template String representation
    **/
    public DTTemplate(String template)
    {
        super(template);
        this.templateProps = new RTProperties((template != null)? template : "");
        this.fieldCount    = -1;
    }

    /** 
    *** Constructor
    *** @param rs       SQL ResultSet
    *** @param fldName  Field name in ResultSet containing the field template
    **/
    public DTTemplate(ResultSet rs, String fldName)
        throws SQLException
    {
        super(rs, fldName);
        // set to default value if 'rs' is null
        this.templateProps = new RTProperties((rs != null)? rs.getString(fldName) : "");
        this.fieldCount    = -1;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the String representation of this DTTemplate
    *** @return The String representation of this DTTemplate
    **/
    public Object getObject()
    {
        return this.toString();
    }

    /**
    *** Returns the String representation of this DTTemplate
    *** @return The String representation of this DTTemplate
    **/
    public String toString()
    {
        return this.templateProps.toString();
    }

    // ------------------------------------------------------------------------
    // "#=H/L|<type>|<index>|<length>" or
    // "#=<type>|H/L|<index>|<length>"
    
    private static final char FIELD_VALUE_SEPARATOR = '|';
    
    /**
    *** Clears all fields defined by this DTTemplate
    **/
    public void clearFields()
    {
        this.templateProps.clearProperties();
    }
    
    /** Returns the field at the specified index
    *** @param ndx  The index of the field to return
    *** @return The field at the specified index, or null if the field does not exist
    **/
    public Field getField(int ndx)
    {
        String ftmp = this.templateProps.getString(String.valueOf(ndx), null);
        return ((ftmp != null) && !ftmp.equals(""))? new Field(ftmp) : null;
    }
    
    /**
    *** Gets the field count
    *** @return The cusrrent number of fields
    **/
    public int getFieldCount()
    {
        if (this.fieldCount < 0) {
            for (this.fieldCount = 0; this.getField(this.fieldCount) != null; this.fieldCount++);
        }
        return this.fieldCount;
    }
    
    /**
    *** Sets the field at the specified index
    *** @param ndx  The index of the field to set
    *** @param fld  The field to set
    ***/
    public void setField(int ndx, Field fld)
    {
        this.templateProps.setString(String.valueOf(ndx), fld.toString());
        this.fieldCount = -1;
    }
    
    /**
    *** DTTemplate Field inner class
    **/
    public static class Field
    {
        private boolean isHiRes = false;
        private int     type    = -1;
        private int     index   = 0;
        private int     length  = 0;
        private boolean isValid = true;
        public Field(int type, boolean hiRes, int ndx, int len) {
            this.isHiRes = hiRes;
            this.type    = type;
            this.index   = ndx;
            this.length  = len;
            this.isValid = (this.type >= 0) && (this.index >= 0) && (this.length > 0);
        }
        public Field(String s) {
            String f[] = StringTools.parseString(s,FIELD_VALUE_SEPARATOR);
            if ((f.length > 0) && (f[0].length() > 0) && Character.isLetter(f[0].charAt(0))) {
                this.isHiRes = (f.length > 0)? f[0].equalsIgnoreCase("H") : false;
                this.type    = (f.length > 1)? StringTools.parseInt(f[1],-1) : -1;
            } else {
                this.type    = (f.length > 0)? StringTools.parseInt(f[0],-1) : -1;
                this.isHiRes = (f.length > 1)? f[1].equalsIgnoreCase("H") : false;
            }
            this.index   = (f.length > 2)? StringTools.parseInt(f[2], 0) :  0;
            this.length  = (f.length > 3)? StringTools.parseInt(f[3], 0) :  0;
            this.isValid = (f.length == 4) && (this.type >= 0) && (this.index >= 0) && (this.length > 0);
        }
        public boolean isValid() {
            return this.isValid;
        }
        public boolean isHiRes() {
            return this.isHiRes;
        }
        public int getType() {
            return this.type;
        }
        public int getIndex() {
            return this.index;
        }
        public int getLength() {
            return this.length;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.isHiRes()?"H":"L");
            sb.append(FIELD_VALUE_SEPARATOR);
            sb.append(this.getType());
            sb.append(FIELD_VALUE_SEPARATOR);
            sb.append(this.getIndex());
            sb.append(FIELD_VALUE_SEPARATOR);
            sb.append(this.getLength());
            return sb.toString();
        }
        public boolean equals(Object other) {
            if (other instanceof Field) {
                return this.toString().equals(other.toString());
            } else {
                return false;
            }
        }
    }
    
    // ------------------------------------------------------------------------

}
