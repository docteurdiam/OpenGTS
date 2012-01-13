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
// Database specific 'WHERE' handler.
// ----------------------------------------------------------------------------
// Change History:
//  2007/09/16  Martin D. Flynn
//     -Initial release
//     -NOTE: This module is not thread safe (this is typically not an issue, since
//      the use of this class is limited to the creation for a specific 'WHERE'
//      clause within a given thread).
//  2008/01/10  Martin D. Flynn
//     -Added 'AND' method with 5 arguments
//  2008/04/11  Martin D. Flynn
//     -Fixed 'LIKE(...)' to use the '*' to '%' translated value.
//  2009/11/01  Martin D. Flynn
//     -Added KEY_AUTO_INDEX
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.text.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBWhere</code> represents an SQL WHERE clause.
**/

public class DBWhere
{
    // ------------------------------------------------------------------------

    public  static int     KEY_FULL                 = 0; // all keys
    public  static int     KEY_PARTIAL_FIRST        = 1; // at least 1st key, stop at next missing
    public  static int     KEY_PARTIAL_ALL          = 2; // at least 1st key, remaining keys optional
    public  static int     KEY_PARTIAL_ALL_EMPTY    = 3; // all keys optional
    public  static int     KEY_AUTO_INDEX           = 4; // only "autoIndex" should be specified

    // ------------------------------------------------------------------------
    // Examples:
    //   DBWhere wh= new DBWhere();
    //   : WHERE ((this='v') AND (that='v'))
    //     wh.WHERE(wh.AND( 
    //       wh.EQ(FLD_this,val), 
    //       wh.EQ(FLD_that,val)
    //     ))
    //   : WHERE ((this='v') OR (that='v'))
    //     wh.WHERE(wh.OR( 
    //       wh.EQ(FLD_this,val), 
    //       wh.EQ(FLD_that,val)
    //     ))
    //   : WHERE (((this='v') OR (that='v')) AND ((this='v') OR (that='v')))
    //     wh.WHERE(wh.AND(
    //       wh.OR(
    //         wh.EQ(FLD_this,val), 
    //         wh.EQ(FLD_that,val)
    //       ),
    //       wh.OR(
    //         wh.EQ(FLD_this,val), 
    //         wh.EQ(FLD_that,val)
    //       ),
    //     ))
    // ------------------------------------------------------------------------

    private DBFactory factory    = null;
    private StringBuffer tempSB  = null;
    private StringBuffer accumSB = null;

    /**
    *** Constructor
    *** @param fact  The table DBFactory
    **/
    public DBWhere(DBFactory fact)
    {
        this.factory = fact;
        this.tempSB = new StringBuffer();
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Clear and return the internal StringBuffer
    *** @return The internal StringBuffer
    **/
    private StringBuffer _getTempSB()
    {
        this.tempSB.setLength(0);
        return this.tempSB;
    }
    
    /**
    *** Quotes the specified value per the field requirements
    *** @param fldName  The field name
    *** @param value    The value to quote
    *** @return The quoted value
    **/
    private String _quoteValue(String fldName, Object value)
    {
        DBField fld = (this.factory != null)? this.factory.getField(fldName) : null;
        if (fld == null) {
            return DBField.quote((value != null)? value.toString() : "");
        } else {
            return fld.getQValue(value);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Return a temporary StringBuffer used for value accumulation
    *** @return An internal temporary StringBuffer instance
    **/
    private StringBuffer _getAccumSB()
    {
        if (this.accumSB == null) {
            this.accumSB = new StringBuffer();
        }
        return this.accumSB;
    }

    /**
    *** Append the specified String to the internal accumulator StringBuffer
    *** @param s  The String to append
    *** @return  The accumulator StringBuffer instance
    **/
    public StringBuffer append(String s)
    {
        StringBuffer sb = this._getAccumSB();
        if (sb.length() > 0) {
            return sb.append(s);
        } else {
            // TODO: check for prefixing " AND " or " OR "
            return sb.append(s);
        }
    }
    
    /** 
    *** Returns a String representation of this DBWhere instance
    *** @return A String representation of this DBWhere instance
    **/
    public String toString()
    {
        return this._getAccumSB().toString();
    }
    
    // ------------------------------------------------------------------------

    /**
    *** "AND operand" 
    *** @param op  The operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String AND_(String op)
    {
        if (op != null) {
            StringBuffer sb = this._getTempSB();
            sb.append(" AND ");
            sb.append(op);
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** "operand1 AND operand2" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String _AND_(String op1, String op2)
    {
        StringBuffer sb = this._getTempSB();
        sb.append(op1); // MUST NOT BE NULL
        if (op2 != null) {
            sb.append(" AND ");
            sb.append(op2);
        }
        return sb.toString();
    }

    /**
    *** "(operand1 AND operand2)" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String AND(String op1, String op2)
    {
        return this.AND(op1, op2, null, null);
    }

    /**
    *** "(operand1 AND operand2 AND operand3)" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @param op3  The third operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String AND(String op1, String op2, String op3)
    {
        return this.AND(op1, op2, op3, null);
    }

    /**
    *** "(operand1 AND operand2 AND operand3 AND operand4)" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @param op3  The third operand
    *** @param op4  The forth operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String AND(String op1, String op2, String op3, String op4)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("( ");
        sb.append(op1); // MUST NOT BE NULL
        if (op2 != null) {
            sb.append(" AND ");
            sb.append(op2);
        }
        if (op3 != null) {
            sb.append(" AND ");
            sb.append(op3);
        }
        if (op4 != null) {
            sb.append(" AND ");
            sb.append(op4);
        }
        sb.append(" )");
        return sb.toString();
    }

    /**
    *** "(operand1 AND operand2 AND operand3 AND operand4 AND operand5)" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @param op3  The third operand
    *** @param op4  The forth operand
    *** @param op5  The fifth operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String AND(String op1, String op2, String op3, String op4, String op5)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("( ");
        sb.append(op1); // MUST NOT BE NULL
        if (op2 != null) {
            sb.append(" AND ");
            sb.append(op2);
        }
        if (op3 != null) {
            sb.append(" AND ");
            sb.append(op3);
        }
        if (op4 != null) {
            sb.append(" AND ");
            sb.append(op4);
        }
        if (op5 != null) {
            sb.append(" AND ");
            sb.append(op5);
        }
        sb.append(" )");
        return sb.toString();
    }

    /**
    *** "(operand AND ...)" 
    *** @param op   An array of operands
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String AND(String op[])
    {
        StringBuffer sb = this._getTempSB();
        sb.append("( ");
        for (int i = 0; i < op.length; i++) {
            if (i > 0) { sb.append(" AND "); }
            sb.append(op[i]);
        }
        sb.append(" )");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "OR operand" 
    *** @param op  The operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String OR_(String op)
    {
        if (op != null) {
            StringBuffer sb = this._getTempSB();
            sb.append(" OR ");
            sb.append(op);
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** "operand1 OR operand2" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String _OR_(String op1, String op2)
    {
        StringBuffer sb = this._getTempSB();
        sb.append(op1);
        if (op2 != null) {
            sb.append(" OR ");
            sb.append(op2);
        }
        return sb.toString();
    }

    /**
    *** "(operand1 OR operand2)" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String OR(String op1, String op2)
    {
        return this.OR(op1, op2, null, null);
    }

    /**
    *** "(operand1 OR operand2 OR operand3)" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @param op3  The third operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String OR(String op1, String op2, String op3)
    {
        return this.OR(op1, op2, op3, null);
    }

    /**
    *** "(operand1 OR operand2 OR operand3 OR operand4)" 
    *** @param op1  The first operand
    *** @param op2  The second operand
    *** @param op3  The third operand
    *** @param op4  The forth operand
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String OR(String op1, String op2, String op3, String op4)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("( ");
        sb.append(op1);
        if (op2 != null) {
            sb.append(" OR ");
            sb.append(op2);
        }
        if (op3 != null) {
            sb.append(" OR ");
            sb.append(op3);
        }
        if (op4 != null) {
            sb.append(" OR ");
            sb.append(op4);
        }
        sb.append(" )");
        return sb.toString();
    }

    /**
    *** "(operand OR ...)" 
    *** @param op   An array of operands
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String OR(String op[])
    {
        StringBuffer sb = this._getTempSB();
        sb.append("( ");
        for (int i = 0; i < op.length; i++) {
            if (i > 0) { sb.append(" OR "); }
            sb.append(op[i]);
        }
        sb.append(" )");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "(field = value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String EQ(String fld, Object value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("=").append(this._quoteValue(fld,value));
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field = value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String EQ(String fld, int value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field = value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String EQ(String fld, long value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field = value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String EQ(String fld, double value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field = value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String EQ(String fld, boolean value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        if (value) {
            // test for true
            sb.append(fld).append("!=0");
        } else {
            // test for false
            sb.append(fld).append("=0");
        }
        sb.append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "(field != value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String NE(String fld, Object value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("!=").append(this._quoteValue(fld,value));
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field != value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String NE(String fld, int value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("!=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field != value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String NE(String fld, long value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("!=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field != value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String NE(String fld, double value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("!=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field != value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String NE(String fld, boolean value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        if (value) {
            // test for NOT true
            sb.append(fld).append("=0");
        } else {
            // test for NOT false
            sb.append(fld).append("!=0");
        }
        sb.append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "(field > value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GT(String fld, Object value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">").append(this._quoteValue(fld,value));
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field > value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GT(String fld, int value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field > value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GT(String fld, long value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field > value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GT(String fld, double value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">").append(value);
        sb.append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "(field >= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GE(String fld, Object value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">=").append(this._quoteValue(fld,value));
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field >= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GE(String fld, int value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field >= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GE(String fld, long value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field >= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String GE(String fld, double value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append(">=").append(value);
        sb.append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "(field < value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LT(String fld, Object value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<").append(this._quoteValue(fld,value));
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field < value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LT(String fld, int value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field < value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LT(String fld, long value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field < value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LT(String fld, double value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<").append(value);
        sb.append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "(field <= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LE(String fld, Object value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<=").append(this._quoteValue(fld,value));
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field <= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LE(String fld, int value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field <= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LE(String fld, long value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<=").append(value);
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field <= value)"
    *** @param fld   The table field
    *** @param value The value 
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LE(String fld, double value)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        sb.append(fld).append("<=").append(value);
        sb.append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "field LIKE '...%...'"
    *** @param fld   The table field
    *** @param value The value (containing wildcards)
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String LIKE(String fld, String value)
    {
        if (fld != null) {
            String likeVal = StringTools.replace(((value!=null)?value:""), "*", DBProvider.LIKE_WILDCARD);
            if (likeVal.indexOf(DBProvider.LIKE_WILDCARD) < 0) {
                likeVal += DBProvider.LIKE_WILDCARD;
            }
            StringBuffer sb = this._getTempSB();
            sb.append("(");
            sb.append(fld).append(" like ").append(this._quoteValue(fld,likeVal));
            sb.append(")");
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** "field LIKE '...%...'"
    *** @param fld   The table field
    *** @param value The 'startsWith' value (no wildcards!)
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String STARTSWITH(String fld, String value)
    {
        return this.LIKE(fld, value + "*");
    }

    // ------------------------------------------------------------------------

    /**
    *** "(field=value0 OR field=value1 ...)"
    *** @param fld   The table field
    *** @param list  An array of values
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String INLIST(String fld, Iterable<?> list)
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        int i = 0;
        for (Iterator v = list.iterator(); v.hasNext();) {
            Object val = v.next();
            if (i++ > 0) {
                sb.append(" OR ");
            }
            sb.append(fld).append("=").append(this._quoteValue(fld,val));
        }
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field=value0 OR field=value1 ...)"
    *** @param fld   The table field
    *** @param list  An array of values
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String INLIST(String fld, Object list[])
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        for (int i = 0; i < list.length; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(fld).append("=").append(this._quoteValue(fld,list[i]));
        }
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field=value0 OR field=value1 ...)"
    *** @param fld   The table field
    *** @param list  An array of values
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String INLIST(String fld, int list[])
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        for (int i = 0; i < list.length; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(fld).append("=").append(list[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
    *** "(field=value0 OR field=value1 ...)"
    *** @param fld   The table field
    *** @param list  An array of values
    *** @return A String representation of the internal temporary StringBuffer
    **/
    public String INLIST(String fld, long list[])
    {
        StringBuffer sb = this._getTempSB();
        sb.append("(");
        for (int i = 0; i < list.length; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(fld).append("=").append(list[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** "WHERE conditions"
    *** @param conditions  The where conditions
    **/
    public String WHERE_(String conditions)
    {
        StringBuffer sb = this._getTempSB();
        if (!StringTools.isBlank(conditions)) {
            sb.append(" WHERE ");
            sb.append(conditions);
        }
        return sb.toString();
    }

    /**
    *** "WHERE (conditions)"
    *** @param conditions  The where conditions
    **/
    public String WHERE(String conditions)
    {
        StringBuffer sb = this._getTempSB();
        if (!StringTools.isBlank(conditions)) {
            // remove prefixing " AND ", " OR "
            String c = conditions.trim();
            if (c.startsWith("AND ")) {
                c = c.substring(4);
            } else
            if (c.startsWith("OR ")) {
                c = c.substring(3);
            }
            sb.append(" WHERE ( ");
            sb.append(c);
            sb.append(" )");
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv  The command-line arguments
    **/
    public static void main(String argv[]) 
    {
        RTConfig.setCommandLineArgs(argv);
        DBFactory fact = null;
        
        String FLD_this = "this";
        String FLD_that = "that";
        String val = "value";
        
        // WHERE (((this='v') OR (that='v')) AND ((this='v') OR (that='v')))
        DBWhere wh = new DBWhere(fact);
        String where_1 = wh.WHERE(
            wh.AND(
                wh.OR(
                    wh.EQ(FLD_this,val), 
                    wh.EQ(FLD_that,val)
                ),
                wh.OR(
                    wh.EQ(FLD_this,val), 
                    wh.EQ(FLD_that,val)
                )
            )
        );
        Print.sysPrintln(where_1);
        
        // WHERE (((this>'v') AND (that<='v')) OR ((this>='v') AND (that<'v')))
        String where_2 = wh.WHERE(
            wh.OR(
                wh.AND(
                    wh.GT(FLD_this,val), 
                    wh.LE(FLD_that,val)
                ),
                wh.AND(
                    wh.GE(FLD_this,val), 
                    wh.LT(FLD_that,val)
                )
            )
        );
        Print.sysPrintln(where_2);
 
        String where_3 = wh.WHERE(wh.EQ(FLD_this,val));
        Print.sysPrintln(where_3);

    }
    
}
