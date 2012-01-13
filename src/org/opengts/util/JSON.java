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
//  2011/07/15  Martin D. Flynn
//     -Initial release
//  2011/08/21  Martin D. Flynn
//     -Fixed JSON parsing.
//  2011/10/03  Martin D. Flynn
//     -Added multiple-name lookup support
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.*;

public class JSON
{

    // ------------------------------------------------------------------------

    private static final String  INDENT         = "   ";

    private static final boolean CASE_SENSITIVE = false;

    private static boolean NameEquals(String n1, String n2)
    {
        if ((n1 == null) || (n2 == null)) {
            return false;
        } else
        if (CASE_SENSITIVE) {
            return n1.equals(n2);
        } else {
            return n1.equalsIgnoreCase(n2);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** JSON Parse Exception
    **/
    public static class JSONParsingException
        extends Exception
    {
        private int index = 0;
        public JSONParsingException(String msg, int ndx) {
            super(msg);
            this.index = ndx;
        }
        public int getIndex() {
            return this.index;
        }
        public String toString() {
            String s = super.toString();
            return s + " ["+this.index+"]";
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Object
    
    /**
    *** JSON Object class
    **/
    public static class _Object
        extends Vector<JSON._KeyValue>
    {

        private boolean formatIndent = true;

        /**
        *** Constructor
        **/
        public _Object() {
            super();
        }

        /**
        *** Constructor
        **/
        public _Object(Vector<JSON._KeyValue> list) {
            this();
            this.addAll(list);
        }

        /**
        *** Constructor
        **/
        public _Object(JSON._KeyValue... kv) {
            this();
            if (!ListTools.isEmpty(kv)) {
                for (int i = 0; i < kv.length; i++) {
                    this.add(kv[i]);
                }
            }
        }

        // --------------------------------------

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(JSON._KeyValue kv) {
            return super.add(kv);
        }
        public boolean add(JSON._KeyValue kv) {
            return super.add(kv);
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, String value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, int value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, long value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, double value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, boolean value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Array value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Object value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Value value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        // --------------------------------------

        /**
        *** Gets the number of key/value pairs in this object
        **/
        public int getKeyValueCount() {
            return super.size();
        }

        /**
        *** Gets the key/value pair at the specified index
        **/
        public JSON._KeyValue getKeyValueAt(int ndx) {
            if ((ndx >= 0) && (ndx < this.size())) {
                return this.get(ndx);
            } else {
                return null;
            }
        }

        // --------------------------------------

        /**
        *** Gets the key/value pair for the specified name
        **/
        public JSON._KeyValue getKeyValue(String n) {
            if (n != null) {
                for (JSON._KeyValue kv : this) {
                    String kvn = kv.getKey();
                    if (JSON.NameEquals(n,kvn)) {
                        return kv;
                    }
                }
            }
            return null;
        }

        /**
        *** Gets the JSON._Value for the specified name
        **/
        public JSON._Value getValueForName(String n) {
            JSON._KeyValue kv = this.getKeyValue(n);
            return (kv != null)? kv.getValue() : null;
        }

        /**
        *** Gets the JSON._Value for the specified name
        **/
        public JSON._Value getValueForName(String name[]) {
            if (!ListTools.isEmpty(name)) {
                for (String n : name) {
                    JSON._Value jv = this.getValueForName(n);
                    if (jv != null) {
                        return jv;
                    }
                }
            }
            return null;
        }

        // --------------------------------------

        /**
        *** Gets the JSON._Array for the specified name
        **/
        public JSON._Array getArrayForName(String name, JSON._Array dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getArrayValue(dft) : dft;
        }

        /**
        *** Gets the JSON._Array for the specified name
        **/
        public JSON._Array getArrayForName(String name[], JSON._Array dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getArrayValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the String value for the specified name
        **/
        public String getStringValueForName(String name, String dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getStringValue(dft) : dft;
        }

        /**
        *** Gets the String value for the specified name
        **/
        public String getStringValueForName(String name[], String dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getStringValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the Integer value for the specified name
        **/
        public int getIntValueForName(String name, int dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getIntValue(dft) : dft;
        }

        /**
        *** Gets the Integer value for the specified name
        **/
        public int getIntValueForName(String name[], int dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getIntValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the Long value for the specified name
        **/
        public long getLongValueForName(String name, long dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getLongValue(dft) : dft;
        }

        /**
        *** Gets the Long value for the specified name
        **/
        public long getLongValueForName(String name[], long dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getLongValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the Double value for the specified name
        **/
        public double getDoubleValueForName(String name, double dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getDoubleValue(dft) : dft;
        }

        /**
        *** Gets the Double value for the specified name
        **/
        public double getDoubleValueForName(String name[], double dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getDoubleValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the String value for the specified name
        **/
        public boolean getBooleanValueForName(String name, boolean dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getBooleanValue(dft) : dft;
        }

        /**
        *** Gets the String value for the specified name
        **/
        public boolean getBooleanValueForName(String name[], boolean dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getBooleanValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets a list of all key names in this object
        **/
        public Collection<String> getKeyNames() {
            Collection<String> keyList = new Vector<String>();
            for (JSON._KeyValue kv : this) {
                keyList.add(kv.getKey());
            }
            return keyList;
        }
        
        /**
        *** Print object contents (for debug purposes only)
        **/
        public void debugDisplayObject(int level) {
            String pfx0 = StringTools.replicateString(INDENT,level);
            String pfx1 = StringTools.replicateString(INDENT,level+1);
            for (String key : this.getKeyNames()) {
                JSON._KeyValue kv = this.getKeyValue(key);
                Object val = kv.getValue().getObjectValue();
                Print.sysPrintln(pfx0 + key + " ==> " + StringTools.className(val));
                if (val instanceof JSON._Object) {
                    JSON._Object obj = (JSON._Object)val;
                    obj.debugDisplayObject(level+1);
                } else
                if (val instanceof JSON._Array) {
                    JSON._Array array = (JSON._Array)val;
                    for (JSON._Value jv : array) {
                        Object av = jv.getObjectValue();
                        Print.sysPrintln(pfx1 + " ==> " + StringTools.className(av));
                        if (av instanceof JSON._Object) {
                            JSON._Object obj = (JSON._Object)av;
                            obj.debugDisplayObject(level+2);
                        }
                    }
                }
            }
        }

        // --------------------------------------

        /**
        *** Set format indent state
        **/
        public _Object setFormatIndent(boolean indent) {
            this.formatIndent = indent;
            return this;
        }

        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            boolean fullFormat = this.formatIndent && (prefix >= 0);
            String pfx0 = fullFormat? StringTools.replicateString(INDENT,prefix)   : "";
            String pfx1 = fullFormat? StringTools.replicateString(INDENT,prefix+1) : "";
            sb.append("{");
            if (fullFormat) {
                //Print.logStackTrace("Carriage return");
                sb.append("\n");
            }
            if (!ListTools.isEmpty(this)) {
                int size = this.size();
                for (int i = 0; i < size; i++) {
                    JSON._KeyValue kv = this.get(i);
                    sb.append(pfx1);
                    kv.toStringBuffer((fullFormat?(prefix+1):-1),sb);
                    if ((i + 1) < size) {
                        sb.append(",");
                    }
                    if (fullFormat) {
                        sb.append("\n");
                    }
                }
            }
            sb.append(pfx0).append("}");
            if (fullFormat && (prefix == 0)) {
                //Print.logStackTrace("Carriage return");
                sb.append("\n");
            }
            return sb;
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() {
            return this.toStringBuffer(0,null).toString();
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString(boolean inclPrefix) {
            return this.toStringBuffer((inclPrefix?0:-1),null).toString();
        }

    }

    /**
    *** Parse a JSON Object from the specified String
    **/
    public static _Object parse_Object(String v)
        throws JSONParsingException 
    {
        return JSON.parse_Object(v,new AccumulatorLong(0L));
    }

    /**
    *** Parse a JSON Object from the specified String, starting at the 
    *** specified location
    **/
    public static _Object parse_Object(String v, AccumulatorLong index)
        throws JSONParsingException 
    {
        int          ndx = (int)index.get();
        int          len = v.length();
        JSON._Object obj = null;

        objectParse:
        for (;ndx < len;) {
            char ch = v.charAt(ndx);
            if (Character.isWhitespace(ch)) {
                ndx++; // consume space
                continue; // skip space
            } else
            if (ch == '{') {
                if (obj != null) {
                    throw new JSONParsingException("Object already started", ndx);
                }
                ndx++;
                obj = new JSON._Object();
            } else
            if (ch == '\"') {
                // "key": VALUE
                if (obj == null) {
                    throw new JSONParsingException("No start of Object", ndx);
                }
                index.set(ndx);
                JSON._KeyValue kv = JSON.parse_KeyValue(v, index);
                ndx = (int)index.get();
                if (kv == null) {
                    throw new JSONParsingException("Invalid KeyValue ...", ndx);
                }
                obj.add(kv);
            } else
            if (ch == ',') {
                // ignore
                ndx++;
            } else
            if (ch == '}') {
                ndx++;
                break objectParse;
            } else {
                // invalid character
                throw new JSONParsingException("Invalid JSON syntax ...", ndx);
            }
        }
        index.set(ndx);
        return obj;
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._KeyValue

    /**
    *** JSON Key/Value pair
    **/
    public static class _KeyValue
    {

        private String      key   = null;
        private JSON._Value value = null;

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, JSON._Value value) {
            this.key   = key;
            this.value = value;
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, String value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, long value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, double value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, boolean value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, JSON._Array value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, JSON._Object value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Gets the key of this key/value pair 
        **/
        public String getKey() {
            return this.key;
        }
        
        /**
        *** Gets the value of this key/value pair 
        **/
        public JSON._Value getValue() {
            return this.value;
        }

        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            sb.append("\"").append(this.key).append("\"");
            sb.append(":");
            if (prefix >= 0) {
                sb.append(" ");
            }
            if (this.value != null) {
                this.value.toStringBuffer(prefix,sb);
            } else {
                sb.append("null");
            }
            return sb;
        }
        
        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() {
            return this.toStringBuffer(1,null).toString();
        }

    }
    
    /**
    *** Parse a Key/Value pair from the specified String at the specified location
    **/
    public static JSON._KeyValue parse_KeyValue(String v, AccumulatorLong index)
        throws JSONParsingException 
    {
        int            ndx = (int)index.get();
        int            len = v.length();
        JSON._KeyValue kv  = null;
        
        String key = null;
        boolean colon = false;
        keyvalParse:
        for (;ndx < len;) {
            char ch = v.charAt(ndx);
            if (Character.isWhitespace(ch)) {
                ndx++; // consume space
                continue; // skip space
            } else
            if (!colon && (ch == '\"')) {
                // Key
                index.set(ndx);
                key = JSON.parse_String(v, index);
                ndx = (int)index.get();
                if (key == null) {
                    throw new JSONParsingException("Invalid key String", ndx);
                }
            } else
            if (ch == ':') {
                if (colon) {
                    throw new JSONParsingException("More than one ':'", ndx);
                } else
                if (key == null) {
                    throw new JSONParsingException("Key not defined", ndx);
                }
                ndx++;
                colon = true;
            } else {
                // JSON._Value
                index.set(ndx);
                JSON._Value val = JSON.parse_Value(v, index);
                ndx = (int)index.get();
                if (val == null) {
                    throw new JSONParsingException("Invalid value", ndx);
                }
                kv = new JSON._KeyValue(key,val);
                break keyvalParse;
            }
        }
        index.set(ndx);
        return kv; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Value

    /**
    *** JSON Value
    **/
    public static class _Value
    {

        private Object value = null;

        /**
        *** Constructor 
        **/
        public _Value(String v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(Integer v) {
            this.value = (v != null)? new Long(v.longValue()) : null;
        }

        /**
        *** Constructor 
        **/
        public _Value(int v) {
            this.value = new Long((long)v);
        }

        /**
        *** Constructor 
        **/
        public _Value(Long v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(long v) {
            this.value = new Long(v);
        }

        /**
        *** Constructor 
        **/
        public _Value(Double v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(double v) {
            this.value = new Double(v);
        }

        /**
        *** Constructor 
        **/
        public _Value(Boolean v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(boolean v) {
            this.value = new Boolean(v);
        }

        /**
        *** Constructor 
        **/
        public _Value(JSON._Array v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(JSON._Object v) {
            this.value = v;
        }

        // -----------------------------------------

        /**
        *** Gets the value
        **/
        public Object getObjectValue() {
            return this.value;
        }

        // -----------------------------------------

        /**
        *** Returns true is this value represents a String 
        **/
        public boolean isStringValue() {
            return (this.value instanceof String);
        }
        public String getStringValue(String dft) {
            if (this.value instanceof String) {
                return (String)this.value;
            } else
            if (this.value instanceof Number) {
                return this.value.toString();
            } else
            if (this.value instanceof Boolean) {
                return this.value.toString();
            } else  {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true is this value represents an Integer 
        **/
        public boolean isIntValue() {
            return (this.value instanceof Integer);
        }
        public int getIntValue(int dft) {
            if (this.value instanceof Number) {
                return ((Number)this.value).intValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseInt(this.value,dft);
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1 : 0;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true is this value represents a Long 
        **/
        public boolean isLongValue() {
            return (this.value instanceof Long);
        }
        public long getLongValue(long dft) {
            if (this.value instanceof Number) {
                return ((Number)this.value).longValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseLong(this.value,dft);
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1L : 0L;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true is this value represents a Double 
        **/
        public boolean isDoubleValue() {
            return (this.value instanceof Double);
        }
        public double getDoubleValue(double dft) {
            if (this.value instanceof Number) {
                return ((Number)this.value).doubleValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseDouble(this.value,dft);
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1.0 : 0.0;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true is this value represents a Boolean 
        **/
        public boolean isBooleanValue() {
            return (this.value instanceof Boolean);
        }
        public boolean getBooleanValue(boolean dft) {
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseBoolean(this.value,dft);
            } else
            if (this.value instanceof Number) {
                return (((Number)this.value).longValue() != 0L)? true : false;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true is this value represents a JSON._Array 
        **/
        public boolean isArrayValue() {
            return (this.value instanceof JSON._Array);
        }
        public JSON._Array getArrayValue(JSON._Array dft) {
            if (this.value instanceof JSON._Array) {
                return (JSON._Array)this.value;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true is this value represents a JSON._Object 
        **/
        public boolean isObjectValue() {
            return (this.value instanceof JSON._Object);
        }
        public JSON._Object getObjectValue(JSON._Object dft) {
            if (this.value instanceof JSON._Object) {
                return (JSON._Object)this.value;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            if (this.value instanceof String) {
                sb.append("\"");
                sb.append(StringTools.escapeJSON((String)this.value));
                sb.append("\"");
            } else
            if (this.value instanceof Number) {
                sb.append(this.value.toString());
            } else
            if (this.value instanceof Boolean) {
                sb.append(this.value.toString());
            } else 
            if (this.value instanceof JSON._Object) {
                ((JSON._Object)this.value).toStringBuffer(prefix, sb);
            } else
            if (this.value instanceof JSON._Array) {
                ((JSON._Array)this.value).toStringBuffer(prefix,sb);
            } else {
                // ignore
            }
            return sb;
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() {
            return this.toStringBuffer(0,null).toString();
        }

    }
    
    /**
    *** Parse JSON Value
    **/
    public static JSON._Value parse_Value(String v, AccumulatorLong index)
        throws JSONParsingException 
    {
        int         ndx = (int)index.get();
        int         len = v.length();
        JSON._Value val = null;
        
        valueParse:
        for (;ndx < len;) {
            char ch = v.charAt(ndx);
            if (Character.isWhitespace(ch)) {
                ndx++; // consume space
                continue; // skip space
            } else
            if (ch == '\"') {
                // parse String
                index.set(ndx);
                String sval = JSON.parse_String(v, index);
                ndx = (int)index.get();
                if (sval == null) {
                    throw new JSONParsingException("Invalid String value", ndx);
                } else {
                    val = new JSON._Value(sval);
                }
                break valueParse;
            } else
            if ((ch == '-') || (ch == '+') || Character.isDigit(ch)) {
                // parse Number
                index.set(ndx);
                Number num = JSON.parse_Number(v, index);
                ndx = (int)index.get();
                if (num == null) {
                    throw new JSONParsingException("Invalid Number value", ndx);
                } else
                if (num instanceof Double) {
                    val = new JSON._Value((Double)num);
                } else
                if (num instanceof Integer) {
                    val = new JSON._Value((Integer)num);
                } else
                if (num instanceof Long) {
                    val = new JSON._Value((Long)num);
                } else {
                    throw new JSONParsingException("Unsupported Number type: " + StringTools.className(num), ndx);
                }
                break valueParse;
            } else
            if (ch == 't') { // true
                ndx++;
                if ((ndx + 2) >= len) {
                    throw new JSONParsingException("Overflow", ndx);
                } else
                if ((v.charAt(ndx  ) == 'r') && 
                    (v.charAt(ndx+1) == 'u') && 
                    (v.charAt(ndx+2) == 'e')   ) {
                    ndx += 3;
                    val = new JSON._Value(Boolean.TRUE);
                } else {
                    throw new JSONParsingException("Invalid Boolean 'true'", ndx);
                }
                break valueParse;
            } else
            if (ch == 'f') { // false
                ndx++;
                if ((ndx + 3) >= len) {
                    throw new JSONParsingException("Overflow", ndx);
                } else
                if ((v.charAt(ndx  ) == 'a') && 
                    (v.charAt(ndx+1) == 'l') && 
                    (v.charAt(ndx+2) == 's') &&
                    (v.charAt(ndx+3) == 'e')   ) {
                    ndx += 4;
                    val = new JSON._Value(Boolean.FALSE);
                } else {
                    throw new JSONParsingException("Invalid Boolean 'false'", ndx);
                }
                break valueParse;
            } else
            if (ch == '[') {
                // JSON._Array
                index.set(ndx);
                JSON._Array array = JSON.parse_Array(v, index);
                ndx = (int)index.get();
                if (array == null) {
                    throw new JSONParsingException("Invalid array", ndx);
                }
                val = new JSON._Value(array);
                break valueParse;
            } else
            if (ch == '{') {
                // JSON._Object
                index.set(ndx);
                JSON._Object obj = JSON.parse_Object(v, index);
                ndx = (int)index.get();
                if (obj == null) {
                    throw new JSONParsingException("Invalid object", ndx);
                }
                val = new JSON._Value(obj);
                break valueParse;
            } else {
                throw new JSONParsingException("Invalid character", ndx);
            }
        }
        index.set(ndx);
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Array

    /**
    *** JSON Array 
    **/
    public static class _Array
        extends Vector<JSON._Value>
    {

        private boolean formatIndent = true;

        /**
        *** Constructor 
        **/
        public _Array() {
            super();
        }

        /**
        *** Constructor 
        *** An Array of other Values
        **/
        public _Array(JSON._Value... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.add(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Strings
        **/
        public _Array(String... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Longs
        **/
        public _Array(long... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Doubles
        **/
        public _Array(double... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Booleans
        **/
        public _Array(boolean... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Objects
        **/
        public _Array(JSON._Object... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of other Arrays
        **/
        public _Array(JSON._Array... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        **/
        /*
        private _Array(Collection list) {
            if (list != null) {
                for (Object val : list) {
                    if (val == null) {
                        this.addValue("");
                    } else
                    if (val instanceof JSON._Value) {
                        this.addValue((JSON._Value)val);
                    } else
                    if (val instanceof JSON._Object) {
                        this.addValue((JSON._Object)val);
                    } else
                    if (val instanceof JSON._Array) {
                        this.addValue((JSON._Array)val);
                    } else
                    if (val instanceof String) {
                        this.addValue((String)val);
                    } else
                    if (val instanceof Long) {
                        this.addValue(((Long)val).longValue());
                    } else
                    if (val instanceof Double) {
                        this.addValue(((Double)val).doubleValue());
                    } else
                    if (val instanceof Boolean) {
                        this.addValue(((Boolean)val).booleanValue());
                    } else {
                        Print.logInfo("Unrecognized data type: " + StringTools.className(val));
                        this.addValue(val.toString());
                    }
                }
            }
        }
        */

        // --------------------------------------

        /**
        *** Add a JSON._Value to this JSON._Array 
        **/
        public boolean add(JSON._Value value) {
            //Print.logStackTrace("Adding: " + value);
            return super.add(value);
        }

        /**
        *** Add a JSON._Value to this JSON._Array 
        **/
        public boolean addValue(JSON._Value value) {
            return this.add(value);
        }

        /**
        *** Add a String to this JSON._Array 
        **/
        public boolean addValue(String value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a Long to this JSON._Array 
        **/
        public boolean addValue(long value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a Double to this JSON._Array 
        **/
        public boolean addValue(double value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a Boolean to this JSON._Array 
        **/
        public boolean addValue(boolean value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a JSON._Object to this JSON._Array 
        **/
        public boolean addValue(JSON._Object value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a JSON._Array to this JSON._Array 
        **/
        public boolean addValue(JSON._Array value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Returns the JSON._Value at the specified index
        **/
        public JSON._Value getValueAt(int ndx) {
            if ((ndx >= 0) && (ndx < this.size())) {
                return this.get(ndx);
            } else {
                return null;
            }
        }

        // --------------------------------------

        /**
        *** Set format indent state
        **/
        public _Array setFormatIndent(boolean indent) {
            this.formatIndent = indent;
            return this;
        }

        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            boolean fullFormat = this.formatIndent && (prefix >= 0);
            String pfx0 = fullFormat? StringTools.replicateString(INDENT,prefix)   : "";
            String pfx1 = fullFormat? StringTools.replicateString(INDENT,prefix+1) : "";
            sb.append("[");
            if (fullFormat) {
                sb.append("\n");
            }
            int size = this.size();
            for (int i = 0; i < this.size(); i++) {
                JSON._Value v = this.get(i);
                sb.append(pfx1);
                v.toStringBuffer((fullFormat?(prefix+1):-1), sb);
                if ((i + 1) < size) { 
                    sb.append(","); 
                }
                if (fullFormat) {
                    sb.append("\n");
                }
            }
            sb.append(pfx0).append("]");
            return sb;
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() {
            return this.toStringBuffer(1,null).toString();
        }

    }

    /**
    *** Parse JSON Array
    **/
    public static JSON._Array parse_Array(String v, AccumulatorLong index)
        throws JSONParsingException 
    {
        int         ndx   = (int)index.get();
        int         len   = v.length();
        JSON._Array array = null;
       
        arrayParse:
        for (;ndx < len;) {
            char ch = v.charAt(ndx);
            if (Character.isWhitespace(ch)) {
                ndx++; // consume space
                continue; // skip space
            } else
            if (ch == '[') {
                if (array != null) {
                    throw new JSONParsingException("Array already started", ndx);
                }
                ndx++;
                array = new JSON._Array();
            } else
            if (ch == ',') {
                // ignore
                ndx++;
            } else
            if (ch == ']') {
                ndx++;
                break arrayParse;
            } else {
                index.set(ndx);
                JSON._Value val = JSON.parse_Value(v, index);
                ndx = (int)index.get();
                if (val == null) {
                    throw new JSONParsingException("Invalid Value", ndx);
                }
                array.add(val);
            }
        }
        index.set(ndx);
        return array;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // String

    /**
    *** Parse a JSON String
    **/
    public static String parse_String(String v, AccumulatorLong index)
        throws JSONParsingException 
    {
        int          ndx = (int)index.get();
        int          len = v.length();
        String       val = null;
        
        stringParse:
        for (;ndx < len;) {
            char ch = v.charAt(ndx);
            if (Character.isWhitespace(ch)) {
                ndx++; // consume space
                continue; // skip space
            } else
            if (ch == '\"') {
                // parse String
                ndx++; // consume initial quote
                StringBuffer sb = new StringBuffer();
                quoteParse:
                for (;;) {
                    ch = v.charAt(ndx);
                    if (ch == '\\') {
                        ndx++; // skip '\'
                        if (ndx >= len) {
                            throw new JSONParsingException("Overflow", ndx);
                        }
                        ch = v.charAt(ndx);
                        ndx++; // skip char
                        switch (ch) {
                            case '"' : sb.append('"' ); break;
                            case '\\': sb.append('\\'); break;
                            case '/' : sb.append('/' ); break;
                            case 'b' : sb.append('\b'); break;
                            case 'f' : sb.append('\f'); break;
                            case 'n' : sb.append('\n'); break;
                            case 'r' : sb.append('\r'); break;
                            case 't' : sb.append('\t'); break;
                            case 'u' : {
                                if ((ndx + 4) >= len) {
                                    throw new JSONParsingException("Overflow", ndx);
                                }
                                String hex = v.substring(ndx,ndx+4);
                                ndx += 4;
                                break;
                            }
                            default  : sb.append(ch); break;
                        }
                    } else
                    if (ch == '\"') {
                        ndx++;  // consume final quote
                        break quoteParse; // we're done
                    } else {
                        sb.append(ch);
                        ndx++;
                    }
                } // quoteParse
                val = sb.toString();
                break stringParse;
            } else {
                throw new JSONParsingException("Missing initial String quote", ndx);
            }
        }
        index.set(ndx);
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Number

    /**
    *** Parse a JSON Number
    **/
    public static Number parse_Number(String v, AccumulatorLong index)
        throws JSONParsingException 
    {
        int          ndx = (int)index.get();
        int          len = v.length();
        Number       val = null;

        numberParse:
        for (;ndx < len;) {
            char ch = v.charAt(ndx);
            if (Character.isWhitespace(ch)) {
                ndx++; // consume space
                continue; // skip space
            } else
            if ((ch == '-') || (ch == '+') || Character.isDigit(ch)) {
                StringBuffer num = new StringBuffer();
                num.append(ch);
                ndx++;
                int intDig = Character.isDigit(ch)? 1 : 0;
                int frcDig = 0;
                int expDig = 0;
                boolean frcCh = false; // '.'
                boolean esnCh = false; // '+'/'-'
                boolean expCh = false; // 'e'/'E'
                digitParse:
                for (;;) {
                    char d = v.charAt(ndx);
                    if (Character.isDigit(d)) {
                        if (expCh) {
                            expDig++;
                        } else
                        if (frcCh) {
                            frcDig++;
                        } else {
                            intDig++;
                        }
                        num.append(d);
                        ndx++;
                    } else
                    if (d == '.') {
                        if (frcCh) {
                            // more than one '.'
                            throw new JSONParsingException("Invalid numeric value (multiple '.')", ndx);
                        } else
                        if (intDig == 0) {
                            // no digits before decimal
                            throw new JSONParsingException("Invalid numeric value (no digits before '.')", ndx);
                        }
                        frcCh = true;
                        num.append(d);
                        ndx++;
                    } else
                    if ((d == 'e') || (d == 'E')) {
                        if (frcCh && (frcDig == 0)) {
                            // no digits after decimal
                            throw new JSONParsingException("Invalid numeric value (no digits after '.')", ndx);
                        } else
                        if (expCh) {
                            // more than one 'E'
                            throw new JSONParsingException("Invalid numeric value (multiple 'E')", ndx);
                        }
                        expCh = true;
                        num.append(d);
                        ndx++;
                    } else
                    if ((d == '-') || (d == '+')) {
                        if (!expCh) {
                            // no 'E'
                            throw new JSONParsingException("Invalid numeric value (no 'E')", ndx);
                        } else
                        if (esnCh) {
                            // more than one '-/+'
                            throw new JSONParsingException("Invalid numeric value (more than one '+/-')", ndx);
                        }
                        esnCh = true;
                        num.append(d);
                        ndx++;
                    } else {
                        break digitParse; // first non-numeric character
                    }
                } // digitParse
                String numStr = num.toString();
                if (frcCh || expCh) {
                    val = (Number)(new Double(StringTools.parseDouble(numStr,0.0)));
                } else {
                    val = (Number)(new Long(StringTools.parseLong(numStr,0L)));
                }
                break numberParse;
            } else {
                throw new JSONParsingException("Missing initial Numeric +/-/0", ndx);
            }
        }
        index.set(ndx);
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private JSON._Object object = null;

    /**
    *** Constructor 
    **/
    public JSON()
    {
        super();
    }

    /**
    *** Constructor 
    **/
    public JSON(JSON._Object obj)
    {
        this.object = obj;
    }

    /**
    *** Constructor 
    **/
    public JSON(String json)
        throws JSONParsingException 
    {
        this.object = JSON.parse_Object(json);
    }

    /**
    *** Constructor 
    **/
    public JSON(InputStream input)
        throws JSONParsingException, IOException
    {
        String json = StringTools.toStringValue(FileTools.readStream(input));
        this.object = JSON.parse_Object(json);
    }

    /** 
    *** Returns true if an object is defined
    **/
    public boolean hasObject()
    {
        return (this.object != null);
    }

    /** 
    *** Gets the main JSON._Object
    **/
    public JSON._Object getObject()
    {
        return this.object;
    }

    /** 
    *** Sets the main JSON._Object
    **/
    public void getObject(JSON._Object obj)
    {
        this.object = obj;
    }

    /**
    *** Return a String representation of this instance
    **/
    public String toString()
    {
        if (this.object != null) {
            return this.object.toString();
        } else {
            return "";
        }
    }

    /**
    *** Print object contents (debug purposes only)
    **/
    public void debugDisplayObject()
    {
        if (this.object != null) {
            this.object.debugDisplayObject(0);
        } else {
            Print.sysPrintln("n/a");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
}
