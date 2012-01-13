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
// Description:
//  Runtime properties container
// ----------------------------------------------------------------------------
// This class supports including other runtime property files.
// Notes: 
// -- Files will be included at the point they are specified.  Any values specified
//    in the included file which have already been defined will be overwritten.
// -- Included files must be specified in URL form, as in the following examples:
//       %include=file:/home/user/some.conf
//       %include=http:/server:8080/dir/some.conf
//    Optional included files may be specified as follows:
//       %include?=file:/home/user/some.conf
//       %include?=http:/server:8080/dir/some.conf
//    If an include file is required, and the file/url does not exist, an exception will
//    be thrown.  If the include is optional, and the file/url does not exist, then the
//    include will be quietly ignored.
// -- Relative URLs may also be specified.  Relative references will be resolved relative
//    to the URL which included the current file.  The relative URL must include the protocol.  
//    Thus relative file URLs may be specified as:
//       file:dir/file.conf
//    and relative http[s] URLs may be specified as:
//       http:dir/file.conf
// -- Replacement variables may be used, however, since the 'include' file is resolved at
//    the point where it is placed in the config file, the reference property keys must
//    already be preveiously defined, either in the current file, a parent file, on the
//    command-line, in an environment variable, or in a Java system property.
// -- Recursive config file inclusions is allow up to at least up to 3 levels deep.
//    That is, the main file can include a child config file, which can include another
//    child config file.  Beyond that, an error may be generated.   File-based property
//    definitions may include http-based property definitions, however, http-based
//    property definitions may not include file-based property definitions.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/02  Martin D. Flynn
//     -Added ability to separate command-line key/value pairs with a ':'.
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/07/27  Martin D. Flynn
//     -Added support for primitive array types
//  2007/08/09  Martin D. Flynn
//     -Added support for URL and InputStream initializers.
//  2007/09/16  Martin D. Flynn
//     -Added method 'insertKeyValues'
//     -Added support for key/value replace in config-file value strings
//  2008/05/14  Martin D. Flynn
//     -Added 'setProperties(String props, char propSep)' method.
//     -Added 'PropertyChangeListener' support
//  2008/06/20  Martin D. Flynn
//     -Removed 'System.getenv' checking (moved to RTConfig.java)
//  2008/07/08  Martin D. Flynn
//     -Added additional command-line argument parsing.
//     -Added method 'validateKeyAttributes' for command-line argument validation.
//  2008/07/20  Martin D. Flynn
//     -Added 'setKeyValueSeparatorChar'/'getKeyValueSeparatorChar' methods
//  2008/07/27  Martin D. Flynn
//     -Added "StringTools.KeyValueMap" implementation.
//  2009/01/01  Martin D. Flynn
//     -Added ability to 'include' other config files.
//  2009/01/28  Martin D. Flynn
//     -Relative 'include' file/http URL specifications are now resolved relative 
//      to the parent file/url.  Replacement variables specified on 'include'
//      statements may now include variable defined in the current/parent file.
//     - Changed 'include[?]' reservered key to '%include[?]'
//     -Added '%log' reserved key to display the specified value to the log output.
//  2009/02/20  Martin D. Flynn
//     -Added 'getAllowBlankValues' and 'setAllowBlankValues'.
//  2009/04/02  Martin D. Flynn
//     -Fixed loading of relative path "%include" files on Windows.
//  2009/07/01  Martin D. Flynn
//     -Added support for returning BigInteger types
//  2009/09/23  Martin D. Flynn
//     -Added support for ${key=default} replacement in config-file value strings
//  2009/11/29  Martin D. Flynn
//     -Added 'isInt', 'isLong', 'isFloat', 'isDouble', 'isBoolean', 'isBigInteger'
//  2011/05/13  Martin D. Flynn
//     -Fixed name inclusion in 'toString' (was "name:...", should be "[name]...")
//  2011/07/01  Martin D. Flynn
//     -Added "%ifTrue-", "%ifFalse-", "%ifDef-", "%ifNotDef-" keywords.
//       %ifDef-testBool:testDef=true
//       %ifNotDef-testBool:testDef=false
//       %ifTrue-testBool:smith=apple
//       %ifFalse-testBool:granny=apple
//  2011/08/21  Martin D. Flynn
//     -Replaced "%ifDef", etc. with "%if <conditional>" ... "%else" ... "%endif"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.*;
import java.math.*;

/**
*** Runtime properties container. Supports including other runtime property files.
**/

public class RTProperties
    implements Cloneable, StringTools.KeyValueMap, 
    RTConfig.PropertySetter, RTConfig.PropertyGetter
{

    // ------------------------------------------------------------------------

    // set to false to enable "%if" ... "%else" ... "%endif"
    private static       boolean USE_PROPERTIES_LOADER      = false;

    // ------------------------------------------------------------------------

    private static final String  INCLUDE_PROTOCOL_FILE      = "file";
    private static final String  INCLUDE_PROTOCOL_HTTP      = "http";
    private static final String  INCLUDE_PROTOCOL_HTTPS     = "https";

    // ------------------------------------------------------------------------

  //public  static final char    NameSeparatorChar          = ':';
    public  static final String  NameStart                  = "[";
    public  static final String  NameEnd                    = "]";

    public  static final char    KeyValSeparatorChars[]     = StringTools.KeyValSeparatorChars;
    public  static final char    PropertySeparatorChar      = StringTools.PropertySeparatorChar;

    public  static final char    ARRAY_DELIM                = StringTools.ARRAY_DELIM;

    // ------------------------------------------------------------------------

    public  static final String  KEY_START_DELIMITER        = "${";
    public  static final String  KEY_END_DELIMITER          = "}";
    public  static final String  KEY_DFT_DELIMITER          = "=";
    public  static final int     KEY_MAX_RECURSION          = 6;

    public  static final int     KEY_REPLACEMENT_NONE       = 0;
    public  static final int     KEY_REPLACEMENT_LOCAL      = 1;
    public  static final int     KEY_REPLACEMENT_GLOBAL     = 2;

    // ------------------------------------------------------------------------
    // This constant controls whether boolean properties with unspecified values
    // will return true, or false.  Example:
    //   ""              - getBoolean("bool", dft) returns dft.
    //   "bool=true"     - getBoolean("bool", dft) returns 'true'.
    //   "bool=false"    - getBoolean("bool", dft) returns 'false'.
    //   "bool=badvalue" - getBoolean("bool", dft) returns dft.
    //   "bool"          - getBoolean("bool", dft) returns DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY.

    private static final boolean DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY   = true;

    // ------------------------------------------------------------------------
    
    public  static final String KEYVAL_PREFIX           = "-";
    public  static final char   KEYVAL_PREFIX_CHAR      = '-';
    public  static final char   KEYVAL_SEPARATOR_CHAR_1 = '=';
    public  static final char   KEYVAL_SEPARATOR_CHAR_2 = ':';

    /**
    *** Returns the index of the key/value separator (either '=' or ':').
    *** @param kv  The String parsed for the key/value separator
    *** @return The index of the key/value separator
    **/
    private int _indexOfKeyValSeparator(String kv)
    {
        //return kv.indexOf('=');
        for (int i = 0; i < kv.length(); i++) {
            char ch = kv.charAt(i);
            if ((ch == KEYVAL_SEPARATOR_CHAR_1) || (ch == KEYVAL_SEPARATOR_CHAR_2)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------

    public  boolean             DEBUG                   = false;

    private String              cfgDirRoot              = null;

    private Map<Object,Object>  cfgProperties           = null;
    private boolean             ignoreCase              = false;
    private boolean             allowBlankValues        = true;

    private char                propertySeparator       = PropertySeparatorChar;
    private char                keyValueSeparators[]    = KeyValSeparatorChars;
    private int                 keyReplacementMode      = KEY_REPLACEMENT_NONE;

    private int                 nextCmdLineArg          = -1;

    private boolean             enableConfigLogMessages = true;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param map  The Object key/value map used to initialize this instance
    **/
    public RTProperties(Map<?,?> map)
    {
        this.setBackingProperties(map);
    }

    /**
    *** Constructor
    **/
    public RTProperties()
    {
        this((Map<Object,Object>)null);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    **/
    public RTProperties(String props)
    {
        this();
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    *** @param inclName True to parse and set the name of this instance.
    **/
    public RTProperties(String props, boolean inclName)
    {
        this();
        this.setProperties(props, inclName);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    *** @param propSep The separator character between one "key=value" pair and the next.
    ***                (ie. in "key=value;key=value", ';' is the property separator)
    **/
    public RTProperties(String props, char propSep)
    {
        this();
        this.setPropertySeparatorChar(propSep);
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param props     A String containing "key=value key=value ..." specifications used to
    ***                  initialize this instance.
    *** @param propSep   The separator character between one "key=value" pair and the next.
    ***                  (ie. in "key=value;key=value", ';' is the property separator)
    *** @param keyValSep The separator character between the property "key" and "value".
    ***                  (ie. in "key=value", ':' is the key/value separator)
    **/
    public RTProperties(String props, char propSep, char keyValSep[])
    {
        this();
        this.setPropertySeparatorChar(propSep);
        this.setKeyValueSeparatorChars(keyValSep);
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param argv    An array of "key=value" specifications.
    **/
    public RTProperties(String argv[])
    {
        this();
        if (argv != null) {
            for (int i = 0; i < argv.length; i++) {

                /* ignore blank arguments */
                if (StringTools.isBlank(argv[i])) {
                    continue;
                }

                /* key/value */
                String kv = argv[i];
                if (kv.startsWith("'") && kv.endsWith("'")) {
                    kv = kv.substring(1, kv.length() - 1);
                } else
                if (kv.startsWith("\"") && kv.endsWith("\"")) {
                    kv = kv.substring(1, kv.length() - 1);
                }

                /* key/val */
                int p = this._indexOfKeyValSeparator(kv); // kv.indexOf("=");
                String key = (p >= 0)? kv.substring(0,p).trim() : kv;
                String val = (p >= 0)? kv.substring(p+1).trim() : "";

                /* remove prefixing "-" from key */
                if (key.startsWith(KEYVAL_PREFIX)) {

                    /* remove prefixing "-" from key */
                    while (key.startsWith(KEYVAL_PREFIX)) { 
                        key = key.substring(1); 
                    }

                    /* special case when separator not specified after key */
                    if (p < 0) {
                        // no separator specified

                        /* end of parameter check? */
                        if (key.equals("")) {
                            // stop at first "-","--",... without a key specifiation
                            // (ie. "-arg1=a -arg2=b -- this is not parsed")
                            if (i < (argv.length + 1)) {
                                this.nextCmdLineArg = i + 1;
                            }
                            break;
                        }

                        /* "-key" was specified without a "=" separator */
                        // check following argument for prefixing "-"
                        if (((i + 1) < argv.length) && !argv[i+1].startsWith(KEYVAL_PREFIX)) {
                            // next argument doesn't have a prefixing "-" (ie. "-file /tmp/myFile")
                            // assume this should be the value for the key
                            // (ie. "-arg1 val1 -arg2 val2")
                            i++; // advance argument pointer
                            val = kv;
                        }
                        
                    }

                }

                /* store key/value */
                if (key.equals("")) {
                    // skip "=value", "-=value", etc.
                    Print.logWarn("Ignoring invalid key argument: '%s'", kv);
                } else {
                    this.setString(key, val);
                }

            }
        }
    }

    /**
    *** Constructor
    *** @param cfgFile A file specification from which the key=value properties are loaded.
    **/
    public RTProperties(File cfgFile)
    {
        this(CreateDefaultMap());
        if ((cfgFile == null) || cfgFile.equals("")) {
            // ignore this case
        } else
        if (cfgFile.isFile()) {
            if (!RTConfig.getBoolean(RTKey.RT_QUIET,true)) {
                Print.logInfo("Loading config file: " + cfgFile);
            }
            try {
                this.setProperties(cfgFile, true);
            } catch (IOException ioe) {
                Print.logError("Unable to load config file: " + cfgFile + " [" + ioe + "]");
            }
        } else {
            Print.logError("Config file doesn't exist: " + cfgFile);
        }
    }

    /**
    *** Constructor
    *** @param cfgURL A URL specification from which the key=value properties are loaded.
    **/
    public RTProperties(URL cfgURL)
    {
        this(CreateDefaultMap());
        if (cfgURL == null) {
            // ignore this case
        } else {
            if (!RTConfig.getBoolean(RTKey.RT_QUIET,true)) {
                Print.logInfo("Loading config file: " + cfgURL);
            }
            try {
                this.setProperties(cfgURL, true);
            } catch (IOException ioe) {
                Print.logError("Unable to load config file: " + cfgURL + " [" + ioe + "]");
            }
        }
    }

    /**
    *** Copy Constructor
    *** @param rtp A RTProperties instance from this this instance is initialized
    **/
    public RTProperties(RTProperties rtp)
    {
        this();
        this.setProperties(rtp, true);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a clone of this RTProperties instance
    *** @return A clone of this RTProperties instance
    **/
    public Object clone()
    {
        return new RTProperties(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the key case on lookups is to be ignored
    *** @return True if the key case on lookups is to be ignored
    **/
    public boolean getIgnoreKeyCase()
    {
        return this.ignoreCase;
    }

    /**
    *** Sets whether key-case is to be ignored on propery lookups.  Only valid if the backing Map
    *** is an <code>OrderedMap</code>.
    *** @param ignCase True ignore key-case on lookups, false otherwise
    **/
    public void setIgnoreKeyCase(boolean ignCase)
    {
        this.ignoreCase = ignCase;
        Map props = this.getProperties();
        if (props instanceof OrderedMap) {
            ((OrderedMap)props).setIgnoreCase(this.ignoreCase);
        } else
        if (ignCase) {
            Print.logWarn("Backing map is not an 'OrderedMap', case insensitive keys not in effect");
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if empty String values are allowed
    *** @return True if empty String values are allowed
    **/
    public boolean getAllowBlankValues()
    {
        return this.allowBlankValues;
    }

    /**
    *** Sets whether empty String values are allowed
    *** @param allowBlank True to allow blank String values
    **/
    public void setAllowBlankValues(boolean allowBlank)
    {
        this.allowBlankValues = allowBlank;
        if (!allowBlank) {
            // TODO: remove existing blank values?
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if configuration log messages (ie. "%log=:) are enabled
    *** @return True if configuration log messages (ie. "%log=:) are enabled
    **/
    public boolean getConfigLogMessagesEnabled()
    {
        return this.enableConfigLogMessages;
    }

    /**
    *** Sets Configuration log messages (ie. "%log=") enabled/disabled
    *** @param enable True to enable, false to disable
    **/
    public void setConfigLogMessagesEnabled(boolean enable)
    {
        this.enableConfigLogMessages = enable;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of this instance.
    *** @return The name of this instance
    **/
    public String getName()
    {
        return this.getString(RTKey.NAME, "");
    }

    /**
    *** Sets the name of this instance
    *** @param name  The name of this instance to set
    **/
    public void setName(String name)
    {
        this.setString(RTKey.NAME, name);
    }

    // ------------------------------------------------------------------------

    /**
    *** List all defined property keys which do not have a registered default value.<br>
    *** Used for diagnostice purposes.
    **/
    public void checkDefaults()
    {
        // This produces a list of keys in the properties list for which RTKey has not 
        // default value.  This is typically for listing unregistered, and possibly 
        // obsolete, properties found in a config file.
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            String key = i.next().toString();
            if (!RTKey.hasDefault(key)) {
                Print.logDebug("No default for key: " + key);
            }
        }
    }

    // ------------------------------------------------------------------------

    protected static Class<OrderedMap> DefaultMapClass = OrderedMap.class;

    /**
    *** Creates a default Map object container
    *** @return A default Map object container
    **/
    protected static Map<Object,Object> CreateDefaultMap()
    {
        /*
        try {
            Map<Object,Object> map = (Map<Object,Object>)DefaultMapClass.newInstance();  // "unchecked cast"
            return map;
        } catch (Throwable t) {
            // (Do not use 'Print' here!!!)
            System.out.println("[RTProperties] Error instantiating: " + DefaultMapClass); // 
            return new OrderedMap<Object,Object>();
        }
        */
        return new OrderedMap<Object,Object>();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the next command-line argument following the last argument
    *** processed by the command-line argument RTProperties constructor.
    *** @return The next command-line argument, or '-1' if there are no additional
    ***         command-line arguments.
    **/
    public int getNextCommandLineArgumentIndex()
    {
        return this.nextCmdLineArg;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Validates the key/values against the expected set of keys and value types.
    *** @param keyAttr  A list of expected keys and attributes
    *** @return The index of the first invalid key
    **/
    public boolean validateKeyAttributes(String keyAttr[], boolean printErrors)
    {
        // key[=|:][o,m][s|i|f|b]
        
        /* nothing to check? */
        if (ListTools.isEmpty(keyAttr)) {
            return true; // all is ok
        }
        
        /* loop through key attributes */
        int error = 0;
        Set<?> argKeys = new HashSet<Object>(this.getPropertyKeys());
        for (int i = 0; i < keyAttr.length; i++) {
            String aKey[] = null;
            boolean mandatory = false;
            int valType = 0; // 0=s,1=i,2=f|d,3=b
            
            int p = this._indexOfKeyValSeparator(keyAttr[i]);
            if (p == 0) {
                // ignore this invalid keyAttr entry
            } else
            if (p < 0) {
                // optional key
                aKey = StringTools.split(keyAttr[i],',');
                mandatory = false;
                valType = 0;
            } else {
                aKey = StringTools.split(keyAttr[i].substring(0,p),',');
                mandatory = (keyAttr[i].charAt(p) == '=')? true : false;
                String attr[] = StringTools.split(keyAttr[i].substring(p+1),',');
                for (int a = 0; a < attr.length; a++) {
                    if (attr[a].equals("m")) { mandatory = true;  } else
                    if (attr[a].equals("o")) { mandatory = false; } else
                    if (attr[a].equals("s")) { valType   = 0;     } else
                    if (attr[a].equals("i")) { valType   = 1;     } else
                    if (attr[a].equals("f")) { valType   = 2;     } else
                    if (attr[a].equals("d")) { valType   = 2;     } else
                    if (attr[a].equals("b")) { valType   = 3;     }
                }
            }

            /* remove keys */
            boolean keyFound = false;
            String keyStr = StringTools.join(aKey,',');
            if (ListTools.isEmpty(aKey)) {
                // invalid keyAttr entry
                continue;
            } else {
                int found = 0;
                for (int k = 0; k < aKey.length; k++) {
                    if (this.hasProperty(aKey[k])) { found++; }
                    argKeys.remove(aKey[k]);
                }
                if (found > 1) {
                    if (printErrors) { Print.sysPrintln("ERROR: Multiple values found for keys: " + keyStr); }
                    error++;
                }
                keyFound = (found > 0);
            }

            /* get value */
            String keyValue = this.getString(aKey, null);

            /* blank value? */
            if (StringTools.isBlank(keyValue)) {
                if (mandatory && (!keyFound || (valType != 3))) {
                    // mandatory argument/value not specified
                    if (printErrors) { Print.sysPrintln("ERROR: Mandatory key not specified: " + keyStr); }
                    error++;
                }
                continue;
            }

            /* check value against type */
            String firstKey = this.getFirstDefinedKey(aKey);
            switch (valType) {
                case 0: // String
                    break;
                case 1: // Integer/Long
                    if (!StringTools.isLong(keyValue,true)) {
                        if (printErrors) { Print.sysPrintln("ERROR: Invalid value for key (i): " + firstKey); }
                        error++;
                    }
                    break;
                case 2: // Float/Double
                    if (!StringTools.isDouble(keyValue,true)) {
                        if (printErrors) { Print.sysPrintln("ERROR: Invalid value for key (f): " + firstKey); }
                        error++;
                    }
                    break;
                case 3: // Boolean
                    if (!StringTools.isBoolean(keyValue,true)) {
                        if (printErrors) { Print.sysPrintln("ERROR: Invalid value for key (b): " + firstKey); }
                        error++;
                    }
                    break;
            }

        }

        /* check for remaining unrecognized keys */
        if (!argKeys.isEmpty()) {
            boolean UNRECOGNIZED_ARGUMENT_ERROR = false;
            for (Object key : argKeys) {
                String ks = key.toString();
                if (ks.startsWith("$")) { continue; }
                if (UNRECOGNIZED_ARGUMENT_ERROR) {
                    if (printErrors) { Print.sysPrintln("ERROR: Unrecognized argument specified: " + ks); }
                    error++;
                } else {
                    if (printErrors) { Print.sysPrintln("WARNING: Unrecognized argument specified: " + ks); }
                }
            }
        }

        /* return validation result */
        return (error == 0);

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** PropertyChangeListener interface
    **/
    public interface PropertyChangeListener
    {
        void propertyChange(RTProperties.PropertyChangeEvent pce);
    }

    /**
    *** PropertyChangeEvent class 
    **/
    public class PropertyChangeEvent
    {
        private Object keyObj = null;
        private Object oldVal = null;
        private Object newVal = null;
        public PropertyChangeEvent(Object key, Object oldValue, Object newValue) {
            this.keyObj = key;      // may be null
            this.oldVal = oldValue; // may be null
            this.newVal = newValue; // may be null
        }
        public RTProperties getSource() {
            return RTProperties.this;
        }
        public Object getKey() {
            return this.keyObj; // may be null
        }
        public Object getOldValue() {
            return this.oldVal; // may be null
        }
        public Object getNewValue() {
            return this.newVal; // may be null
        }
    }

    private java.util.List<PropertyChangeListener> changeListeners = null;

    /** 
    *** Adds a PropertyChangeListener to this instance
    *** @param pcl  A PropertyChangeListener to add to this instance
    **/
    public void addChangeListener(PropertyChangeListener pcl)
    {
        if (this.changeListeners == null) { 
            this.changeListeners = new Vector<PropertyChangeListener>();
        }
        this.changeListeners.add(pcl);
    }

    /** 
    *** Removes a PropertyChangeListener from this instance
    *** @param pcl  A PropertyChangeListener to remove from this instance
    **/
    public void removeChangeListener(PropertyChangeListener pcl)
    {
        if (this.changeListeners != null) {
            this.changeListeners.remove(pcl);
        }
    }

    /**
    *** Fires a PropertyChange event
    *** @param key  The property key which changed
    *** @param oldVal  The old value of the property key which changed
    **/
    protected void firePropertyChanged(Object key, Object oldVal)
    {
        if (this.changeListeners != null) {
            Object newVal = this.getProperties().get(key);
            RTProperties.PropertyChangeEvent pce = new RTProperties.PropertyChangeEvent(key,oldVal,newVal);
            for (Iterator i = this.changeListeners.iterator(); i.hasNext();) {
                ((RTProperties.PropertyChangeListener)i.next()).propertyChange(pce);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the backing properties Map for this instance
    *** @param map  The backing properties Map to set for this instance
    **/
    @SuppressWarnings("unchecked")
    public void setBackingProperties(Map<?,?> map)
    {
        this.cfgProperties = (Map<Object,Object>)map;
        /*
        if (this.cfgProperties != null) {
            for (Object k : this.cfgProperties.keySet()) {
                Object v = this.cfgProperties.get(k);
                Print.sysPrintln(k + " ==> " + v);
            }
        }
        */
    }

    /**
    *** Gets the backing properties Map for this instance
    *** @return  The backing properties Map for this instance
    **/
    public Map<Object,Object> getProperties()
    {
        if (this.cfgProperties == null) { 
            this.cfgProperties = CreateDefaultMap();
            if (this.cfgProperties instanceof OrderedMap) {
                ((OrderedMap)this.cfgProperties).setIgnoreCase(this.ignoreCase);
            }
        }
        return this.cfgProperties;
    }

    /**
    *** Returns the backing properties Map for this instance, cast to indicate String 
    *** keys and values. <br>
    *** WARNING: Use the Map returned by this method with caution.  May cause either
    *** a ClassCastException or other Exception to be thrown if the backing Map contains
    *** any non-String keys or values.
    *** @return  A Map wrapper around the backing properties Map for this instance
    **/
    @SuppressWarnings("unchecked")
    protected Map<String,String> getStringProperties()
    {
        Object objProps = this.getProperties();
        return (Map<String,String>)objProps;
    }

    /**
    *** Returns a immutable wrapper around the backing properties Map for this instance.
    *** (not yet fully tested)
    *** @return  A Map wrapper around the backing properties Map for this instance
    **/
    protected Map<Object,Object> getImmutableProperties()
    {
        // Alot of work just to make this immutable
        final Map<Object,Object> props = this.getProperties();
        return new Map<Object,Object>() {
            public boolean containsKey(Object K) {
                return props.containsKey(K);
            }
            public boolean containsValue(Object K) {
                return props.containsValue(K);
            }
            public Object get(Object K) {
                return props.get(K);
            }
            public int hasCode() {
                return props.hashCode();
            }
            public boolean isEmpty() {
                return props.isEmpty();
            }
            public int size() {
                return props.size();
            }
            public Set<Object> keySet() { 
                return new AbstractSet<Object>() {
                    public int size() {
                        return props.size();
                    }
                    public Iterator<Object> iterator() {
                        final Iterator<Object> i = props.keySet().iterator();
                        return new Iterator<Object>() {
                            public boolean hasNext() {
                                return i.hasNext();
                            }
                            public Object next() {
                                return i.next();
                            }
                            public void remove() { throw new UnsupportedOperationException(); }
                        };
                    }
                };
            }
            public Set<Map.Entry<Object,Object>> entrySet() {
                //throw new UnsupportedOperationException(); 
                return new AbstractSet<Map.Entry<Object,Object>>() {
                    public int size() {
                        return props.entrySet().size();
                    }
                    public Iterator<Map.Entry<Object,Object>> iterator() {
                        final Iterator<Map.Entry<Object,Object>> i = props.entrySet().iterator();
                        return new Iterator<Map.Entry<Object,Object>>() {
                            public boolean hasNext() {
                                return i.hasNext();
                            }
                            public Map.Entry<Object,Object> next() {
                                final Map.Entry<Object,Object> me = i.next();
                                return new Map.Entry<Object,Object>() {
                                    public boolean equals(Object o) {
                                        return me.equals(o);
                                    }
                                    public Object getKey() {
                                        return me.getKey();
                                    }
                                    public Object getValue() {
                                        return me.getValue();
                                    }
                                    public int hashCode() {
                                        return me.hashCode();
                                    }
                                    public Object setValue(Object value) { throw new UnsupportedOperationException(); }
                                };
                            }
                            public void remove() { throw new UnsupportedOperationException(); }
                        };
                    }
                };
            }
            public Collection<Object> values() {
                final Collection<Object> c = props.values();
                return new AbstractCollection<Object>() {
                    public int size() {
                        return c.size();
                    }
                    public Iterator<Object> iterator() {
                        final Iterator<Object> i = c.iterator();
                        return new Iterator<Object>() {
                            public boolean hasNext() {
                                return i.hasNext();
                            }
                            public Object next() {
                                return i.next();
                            }
                            public void remove() { throw new UnsupportedOperationException(); }
                        };
                    }
                };
            }
            public void clear() { throw new UnsupportedOperationException(); }
            public String remove(Object K) { throw new UnsupportedOperationException(); }
            public String put(Object K, Object V) { throw new UnsupportedOperationException(); }
            public void putAll(Map<? extends Object,? extends Object> m) { throw new UnsupportedOperationException(); }
        };
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this RTProperties instance is empty (ie. contains no properties)
    *** @return  True if empty
    **/
    public boolean isEmpty()
    {
        if (this.cfgProperties == null) {
            return true;
        } else {
            return (this.cfgProperties.size() <= 0);
        }
    }

    /**
    *** Returns an Iterator over the property keys defined in this RTProperties instance
    *** @return An Iterator over the property keys defined in this RTProperties instance
    **/ 
    public Iterator<?> keyIterator()
    {
        return this.getPropertyKeys().iterator();
    }

    /**
    *** Gets a set of property keys defined by this RTProperties instance
    *** @return A set of property keys defined by this RTProperties instance
    **/
    public Set<?> getPropertyKeys()
    {
        return this.getProperties().keySet();
    }

    /**
    *** Returns a set of property keys defined in this RTProperties instance which start with the specified String
    *** @return A set of property keys defined in this RTProperties instance which start with the specified String
    **/ 
    public Set<String> getPropertyKeys(String startsWith)
    {
        OrderedSet<String> keys = new OrderedSet<String>();
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            String k = i.next().toString();
            if (startsWith == null) {
                // add everything if 'startsWith' is null
                keys.add(k);
            } else
            if (this.getIgnoreKeyCase()) {
                if (StringTools.startsWithIgnoreCase(k, startsWith)) {
                    // case-insensitive match
                    keys.add(k);
                }
            } else {
                if (k.startsWith(startsWith)) {
                    // match
                    keys.add(k);
                }
            }
        }
        return keys;
    }

    /**
    *** Returns a subset of this RTProperties instance containing key/value pairs which match the
    *** specified partial key.
    *** @param keyStartsWith  The partial key used to match keys in this instance
    *** @return The RTProperties subset
    **/
    public RTProperties getSubset(String keyStartsWith)
    {
        RTProperties rtp = new RTProperties();
        rtp.setIgnoreKeyCase(this.getIgnoreKeyCase());
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            Object k = i.next();
            if (k instanceof String) {
                String ks = (String)k;
                if (this.getIgnoreKeyCase()) {
                    if (StringTools.startsWithIgnoreCase(ks,keyStartsWith)) {
                        String v = this.getString(ks, null);
                        rtp.setProperty(ks, v);
                    }
                } else {
                    if (ks.startsWith(keyStartsWith)) {
                        String v = this.getString(ks, null);
                        rtp.setProperty(ks, v);
                    }
                }
            }
        }
        return rtp;
    }

    /* Extract a Map containing a group of key/values from the runtime config */
    /*
    public Map<String,String> extractMap(String keyEnd, String valEnd)
    {
        Map<String,String> m = new OrderedMap<String,String>();
        for (Iterator<?> i = this.keyIterator(); i.hasNext();) {
            String mkKey = i.next().toString();
            if (mkKey.endsWith(keyEnd)) {
                String key = getString(mkKey, null);
                if (key != null) { // <-- will never be null anyway
                    String mvKey = mkKey.substring(0, mkKey.length() - keyEnd.length()) + valEnd;
                    String val = this.getString(mvKey, "");
                    m.put(key, val);
                }
            }
        }
        return m;
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified property key is defined
    *** @param key  A property key
    *** @return True if the specified property key is defined
    **/
    public static boolean containsKey(Map<Object,Object> map, Object key, boolean blankOK)
    {

        /* quick false checks */
        if ((map == null) || (key == null)) {
            return false;
        }

        /* check for contains */
        if (blankOK) {
            // blank values are ok
            return map.containsKey(key);
        } else {
            // blank String values are considered 'null'
            Object val = map.get(key);
            if (val instanceof String) {
                return !StringTools.isBlank((String)val);
            } else {
                return (val != null);
            }
        }

    }

    /**
    *** Returns true if the specified property key is defined
    *** @param keyList  A list of acceptable property keys
    *** @return True if any of the specified property keys are defined
    **/
    public boolean hasProperty(Object keyList[])
    {
        if (keyList != null) {
            Map<Object,Object> props = this.getProperties();
            boolean allowBlanks = this.getAllowBlankValues();
            for (Object key : keyList) {
                if (RTProperties.containsKey(props, key, allowBlanks)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /**
    *** Returns true if the specified property key is defined
    *** @param key  A property key
    *** @return True if the specified property key is defined
    **/
    public boolean hasProperty(Object key)
    {
        if (key != null) {
            Map<Object,Object> props = this.getProperties();
            boolean allowBlanks = this.getAllowBlankValues();
            return RTProperties.containsKey(props, key, allowBlanks);
        } else {
            return false;
        }
    }

    /**
    *** Returns the first defined property key in the list 
    *** @param key  An array of property keys
    *** @return the first defined property key in the list
    **/
    public String getFirstDefinedKey(String key[])
    {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                if (this.hasProperty(key[i])) {
                    return key[i];
                }
            }
        }
        return null;
    }

    /**
    *** Returns the specified key, if defined
    *** @param key  The propery key
    *** @return The property key if defined, or null otherwise
    **/
    public String getFirstDefinedKey(String key)
    {
        return this.hasProperty(key)? key : null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the value for the specified key
    *** @param key  The property key
    *** @param value The value to associate with the specified key
    **/
    public void setProperty(Object key, Object value)
    {
        if (key != null) {

            /* properties */
            Map<Object,Object> props = this.getProperties();

            /* disallow blank values? */
            if (!this.getAllowBlankValues() && (value instanceof String) && StringTools.isBlank((String)value)) {
                value = null; // will be removed below
            }

            /* "!<key>" implies removable of <key> from Map (value is ignored) */
            String k = (key instanceof String)? (String)key : null;
            if (!StringTools.isBlank(k) && ("|!^".indexOf(k.charAt(0)) >= 0)) {
                key   = k.substring(1);
                value = null;
            }

            /* encode arrays? */
            if ((value != null) && value.getClass().isArray()) {
                Class arrayClass = value.getClass();
                if (arrayClass.getComponentType().isPrimitive()) {
                    value = StringTools.encodeArray(value, ARRAY_DELIM, false);
                } else {
                    Object a[] = (Object[])value;
                    boolean quote = (a instanceof Number[])? false : true;
                    value = StringTools.encodeArray(a, ARRAY_DELIM, quote);
                }
            } else {
                //
            }

            /* add/remove key/value */
            if (!(props instanceof Properties) || (key instanceof String)) {
                Object oldVal = props.get(key);
                if (value == null) {
                    //Print._println("Removing key: " + key);
                    props.remove(key);
                } else
                if ((props instanceof OrderedMap) && key.equals(RTKey.NAME)) {
                    //Print._println("Setting name: " + value);
                    ((OrderedMap<Object,Object>)props).put(0, key, value);
                } else {
                    //Print._println("Setting key: " + key + "=" + value);
                    props.put(key, value);
                }
                this.firePropertyChanged(key, oldVal);
            } else {
                // Non-String are not supported in the 'Properties' class
            }

        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the properties in the specified RTProperties instance to this instance
    *** @param rtp  The RTProperties instance from which properties will be copied to this instance
    *** @return The name of this RTProperties instance
    **/ 
    public String setProperties(RTProperties rtp)
    {
        return this.setProperties(rtp, false);
    }

    /**
    *** Adds the properties in the specified RTProperties instance to this instance
    *** @param rtp  The RTProperties instance from which properties will be copied to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @return The name of this RTProperties instance
    **/ 
    public String setProperties(RTProperties rtp, boolean inclName)
    {
        if (rtp != null) {
            return this.setProperties(rtp.getProperties(), inclName);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the properties in the saved properties at the specified URL to
    *** this instance
    *** @param url  The URL from which properties will be loaded to this instance
    *** @return The name of this RTProperties instance
    *** @throws IOException If an I/O error occurs
    **/ 
    public String setProperties(URL url)
        throws IOException
    {
        return this.setProperties(url, false);
    }
    
    /**
    *** Adds the properties in the saved properties at the specified URL to
    *** this instance
    *** @param url  The URL from which properties will be loaded to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @return The name of this RTProperties instance
    *** @throws IOException If an I/O error occurs
    **/ 
    public String setProperties(URL url, boolean inclName)
        throws IOException
    {
        String name = null;
        if (url != null) {
            InputStream uis = url.openStream(); // may throw IOException
            try {
                name = this._setProperties(uis, inclName, url);
            } finally {
                try { uis.close(); } catch (IOException ioe) {/*ignore*/}
            }
        }
        return name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the properties in the saved properties in the specified file to
    *** this instance
    *** @param file  The file from which properties will be loaded to this instance
    *** @return The name of this RTProperties instance
    *** @throws IOException If an I/O error occurs
    **/
    public String setProperties(File file)
        throws IOException
    {
        return this.setProperties(file, false);
    }

    /**
    *** Adds the properties in the saved properties in the specified file to
    *** this instance
    *** @param file  The file from which properties will be loaded to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @return The name of this RTProperties instance
    *** @throws IOException If an I/O error occurs
    **/
    public String setProperties(File file, boolean inclName)
        throws IOException
    {
        String name = null;
        if (file != null) {
            File absFile = file.getAbsoluteFile();
            FileInputStream fis = new FileInputStream(absFile); // may throw IOException
            try {
                name = this._setProperties(fis, inclName, FileTools.toURL(absFile));
            } finally {
                try { fis.close(); } catch (IOException ioe) {/*ignore*/}
            }
        }
        return name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the properties in the saved properties in the specified input 
    *** stream to this instance
    *** @param in  The input stream from which properties will be loaded to this instance
    *** @return The name of this RTProperties instance
    *** @throws IOException If an I/O error occurs
    **/
    public String setProperties(InputStream in)
        throws IOException
    {
        return this._setProperties(in, false, null);
    }

    /**
    *** Adds the properties in the saved properties in the specified input 
    *** stream to this instance
    *** @param in  The input stream from which properties will be loaded to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @return The name of this RTProperties instance
    *** @throws IOException If an I/O error occurs
    **/
    public String setProperties(InputStream in, boolean inclName)
        throws IOException
    {
        return this._setProperties(in, false, null);
    }

    /**
    *** Adds the properties in the saved properties in the specified input 
    *** stream to this instance
    *** @param in  The input stream from which properties will be loaded to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @param inputURL The input URL. Will be added as a property ({@link RTKey#CONFIG_URL})
    *** @return The name of this RTProperties instance
    *** @throws IOException If an I/O error occurs
    **/
    private String _setProperties(InputStream in, boolean inclName, URL inputURL)
        throws IOException
    {

        /* create temporary Properties holder */
        OrderedProperties props = new OrderedProperties(inputURL);

        /* set property for this loaded URL */
        if (inputURL != null) {
            props.put(RTKey.CONFIG_URL, inputURL.toString());
        }

        /* load the properties from the specified input-stream */
        props.loadProperties(props, in);

        /* convert these loaded properties to an internal format */
        return this.setProperties(props.getOrderedMap(), inclName);

    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the properties in the specified <code>Map</code> to this instance
    *** @param props  The map from which properties will be loaded to this instance
    *** @return The name of this RTProperties instance
    **/
    public String setProperties(Map props)
    {
        return this.setProperties(props, false);
    }

    /**
    *** Adds the properties in the specified <code>Map</code> to this instance
    *** @param props  The map from which properties will be loaded to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @return The name of this RTProperties instance
    **/
    public String setProperties(Map props, boolean inclName)
    {
        // Note: Does NOT remove old properties (by design)
        if (props != null) {
            String n = null;
            for (Iterator i = props.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                Object val = props.get(key);
                if (RTKey.NAME.equals(key)) {
                    n = (val != null)? val.toString() : null;
                    if (inclName) {
                        this.setName(n);
                    }
                } else {
                    this.setProperty(key, val);
                }
            }
            return n;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the character used to seperate properties. Used in storing and 
    *** retriving multiple properties from a string
    *** @param propSep The character to use to seperate properties
    **/
    public void setPropertySeparatorChar(char propSep)
    {
        this.propertySeparator = propSep;
    }
    
    /**
    *** Gets the character used to seperate properties. Used in storing and 
    *** retriving multiple properties from a string
    *** @return The character to use to seperate properties
    **/
    public char getPropertySeparatorChar()
    {
        return this.propertySeparator;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the characters used to seperate key/value pairs. Used in storing 
    *** and retriving multiple properties from a string
    *** @param keyValSep The array of key/value sperator characters
    **/
    public void setKeyValueSeparatorChars(char keyValSep[])
    {
        this.keyValueSeparators = !ListTools.isEmpty(keyValSep)? keyValSep : KeyValSeparatorChars;
    }

    /**
    *** Sets the character used to seperate key/value pairs. Used in storing 
    *** and retriving multiple properties from a string
    *** @param keyValSep The key/value sperator character
    **/
    public void setKeyValueSeparatorChar(char keyValSep)
    {
        this.keyValueSeparators = new char[] { keyValSep };
    }

    /**
    *** Gets the characters used to seperate key/value pairs. Used in storing 
    *** and retriving multiple properties from a string
    *** @return The array of key/value sperator characters
    **/
    public char[] getKeyValueSeparatorChars()
    {
        return this.keyValueSeparators;
    }

    /**
    *** Gets the character used to seperate key/value pairs. Used in storing 
    *** and retriving multiple properties from a string
    *** @return The key/value sperator character
    **/
    public char getKeyValueSeparatorChar()
    {
        return this.keyValueSeparators[0];
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the properties in the specified string to this instance
    *** @param props The string from which the properties will be added
    *** @return The name of this RTProperties instance
    **/
    public String setProperties(String props)
    {
        return this.setProperties(props, false);
    }

    /**
    *** Adds the properties in the specified string to this instance
    *** @param props The string from which the properties will be added
    *** @param propSep The character to set as the property seperator. Calls 
    ***        {@link #setPropertySeparatorChar}, which can affect futher 
    ***        opperations
    *** @return The name of this RTProperties instance
    **/
    public String setProperties(String props, char propSep)
    {
        this.setPropertySeparatorChar(propSep);
        return this.setProperties(props, false);
    }

    /**
    *** Adds the properties in the specified string to this instance
    *** @param props The string from which the properties will be added
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance
    *** @return The name of this RTProperties instance
    **/
    public String setProperties(String props, boolean inclName)
    {
        if (props != null) {
            char propSep     = this.getPropertySeparatorChar();
            char keyValSep[] = this.getKeyValueSeparatorChars();

            /* check for prefixing name in string (ie. "[name] key=value") */
            String n = null;
            String p = props.trim();
            if (p.startsWith(NameStart)) {
                int x = p.indexOf(NameEnd);
                if (x > 0) {
                    // found "[name]"
                    n = p.substring(1,x).trim();
                    p = p.substring(x+1).trim();
                } else {
                    // missing name terminating ']'
                    p = p.substring(1).trim(); // just skip first '['
                }
            }

            /* parse and set properties */
            Map<String,String> propMap = StringTools.parseProperties(p, propSep, keyValSep);
            if (n == null) {
                n = this.setProperties(propMap, inclName);
            } else {
                this.setProperties(propMap, false);
                if (inclName) {
                    this.setName(n);
                }
            }

            /* return name, if any */
            return n;

        } else {

            return null;

        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Removes a property with the specified key from this instance
    *** @param key The key of the property to remove
    **/
    public void removeProperty(Object key)
    {
        if (key != null) {
            Map props = this.getProperties();
            if (!(props instanceof Properties) || (key instanceof String)) {
                Object oldVal = props.get(key);
                props.remove(key);
                this.firePropertyChanged(key, oldVal);
            }
        }
    }

    /**
    *** Removes a property with the specified key from this instance
    *** @param key The key of the property to remove
    *** @see #removeProperty(Object key)
    **/
    public void removeProperties(Object key)
    {
        this.removeProperty(key);
    }

    /**
    *** Removes the specified property keys from this instance
    *** @param keyArry The array of property keys to remove
    **/
    public void removeProperties(String keyArry[])
    {
        if (!ListTools.isEmpty(keyArry)) {
            for (String key : keyArry) {
                this.removeProperty(key);
            }
        }
    }

    /**
    *** Removes all property keys in the specified RTProperties instance from this instance
    *** @param rtp The RTProperties instance containing the keys to remove
    **/
    public void removeProperties(RTProperties rtp)
    {
        if (rtp != null) {
            for (Iterator i = rtp.keyIterator(); i.hasNext();) {
                Object key = i.next();
                this.removeProperty(key);
            }
        }
    }

    /**
    *** Clears all the properties in this instance
    **/
    public void clearProperties()
    {
        this.getProperties().clear();
        this.firePropertyChanged(null, null);
    }

    /**
    *** Clears all the properties in this instance and resets them with the 
    *** properties in the specified map
    *** @param props The properties to set this instance with
    **/
    public void resetProperties(Map props)
    {
        this.clearProperties();
        this.setProperties(props, true);
    }

    // ------------------------------------------------------------------------

    /**
    *** Replaces references to other keys with the values of those keys
    *** @param text  The target String
    *** @return The String containing the replaced key variables
    *** @see StringTools#insertKeyValues
    **/
    public String insertKeyValues(String text)   
    {
        return this._insertKeyValues(null, text, KEY_START_DELIMITER, KEY_END_DELIMITER, KEY_DFT_DELIMITER);
    }

    /**
    *** Replaces references to other keys with the values of those keys
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @return The String containing the replaced key variables
    *** @see StringTools#insertKeyValues
    **/
    public String insertKeyValues(String text, String startDelim, String endDelim)
    {
        return this._insertKeyValues(null, text, startDelim, endDelim, KEY_DFT_DELIMITER);
    }

    /**
    *** Replaces references to other keys with the values of those keys
    *** @param key The main key
    *** @param text  The target String
    *** @return The String containing the replaced key variables
    *** @see StringTools#insertKeyValues
    **/
    public String _insertKeyValues(Object key, String text)   
    {
        return this._insertKeyValues(key, text, KEY_START_DELIMITER, KEY_END_DELIMITER, KEY_DFT_DELIMITER);
    }

    /**
    *** Replaces references to other keys with the values of those keys
    *** @param mainKey The main key
    *** @param text  The target String
    *** @param startDelim  The pattern used to determine the start of a 'key' variable
    *** @param endDelim    The pattern used to determine the end of a key variable
    *** @return The String containing the replaced key variables
    *** @see StringTools#insertKeyValues
    **/
    public String _insertKeyValues(final Object mainKey, String text, 
        String startDelim, String endDelim, final String dftDelim)
    {
        if (text != null) {
            //if (DEBUG) Print.logError("Inserting local keyvalues: " + text);
            // replacment call-back 
            StringTools.KeyValueMap rm = new StringTools.KeyValueMap() { // ReplacementMap
                private Set<Object> thisKeySet = new HashSet<Object>();
                private Set<Object> fullKeySet = new HashSet<Object>();
                public String getKeyValue(String k, String argNotUsed, String dft) {
                    // reset?
                    if (k == null) {
                        // a bit of a hack here to tell this map to reset the cached keys
                        //if (DEBUG) Print.logError("Reset map ...");
                        fullKeySet.addAll(thisKeySet);
                        if (mainKey != null) { fullKeySet.add(mainKey); }
                        thisKeySet.clear();
                        return null;
                    }
                    // parse key/default
                    String key = k; // null;
                    //String dft = null;
                    //int dftNdx = StringTools.isBlank(dftDelim)? -1 : k.indexOf(dftDelim); // k.lastIndexOf(dftDelim); <-- last index?
                    //if (dftNdx >= 0) {
                    //    dft = k.substring(dftNdx + dftDelim.length()); // leave default as-is (untrimmed)
                    //    key = k.substring(0,dftNdx).trim();  // trim key
                    //} else {
                    //    key = k.trim();  // trim key
                    //}
                    // return value
                    if (fullKeySet.contains(key)) {
                        if (DEBUG) Print.logError("Key already processed: " + key);
                        return null;
                    } else {
                        //if (DEBUG) Print.logError("Processing key: " + key);
                        thisKeySet.add(key);
                        Object obj = RTProperties.this._getProperty(key, dft);
                        return (obj != null)? obj.toString() : dft;
                    }
                }
            };
            // iterate until the string doesn't change
            String s_old = text;
            for (int i = 0; i < RTProperties.KEY_MAX_RECURSION; i++) {
                rm.getKeyValue(null,null,null); // hack to reset the cached keys
                String s_new = StringTools.insertKeyValues(s_old, 
                    startDelim, endDelim, dftDelim,
                    rm, false);
                //if (DEBUG) Print.logError("New String: " + s_new);
                if (s_new.equals(s_old)) {
                    return s_new;
                }
                s_old = s_new;
            }
            return s_old;
        } else {
            return text; // return null
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the key replacement mode
    *** @param mode The key replacement mode. One of the 
    ***        <code>KEY_REPLACEMENT_</code> constants
    **/
    public void setKeyReplacementMode(int mode)
    {
        this.keyReplacementMode = mode;
    }

    /**
    *** Replaces any key refences in <code>obj</code>, if <code>obj</code> is a
    *** string, with the values of those keys according to the current 
    *** replacement mode
    *** @param key The main key for <code>obj</code>
    *** @param obj If a string, key refences are resolved, otherwise returned as is
    *** @return If <code>obj</code> is a string, <code>obj</code> with key 
    ***         references resolved, else <code>obj</code>
    *** @see #_insertKeyValues(final Object mainKey, String text, String startDelim, String endDelim, String dftDelim)
    *** @see StringTools#insertKeyValues
    **/
    private Object _replaceKeyValues(Object key, Object obj)   
    {
        if (this.keyReplacementMode == KEY_REPLACEMENT_NONE) {
            //if (DEBUG) System.out.println("No replacement to be performed: " + obj);
            return obj;
        } else
        if ((obj == null) || !(obj instanceof String)) {
            //if (DEBUG) System.out.println("Returning non-String object as-is: " + obj);
            return obj;
        } else
        if (this.keyReplacementMode == KEY_REPLACEMENT_LOCAL) {
            //if (DEBUG) System.out.println("Replacing local keys: " + obj);
            return this._insertKeyValues(key,(String)obj);
        } else {
            //if (DEBUG) System.out.println("Replacing global keys: " + obj);
            return RTConfig._insertKeyValues(key,(String)obj);
        }
    }

    /**
    *** Gets the property value of a key
    *** @param key The key to get the property value of
    *** @param dft The default value to return if none found. If specified, but
    ***        <code>dftClass</code> is not, an attempt will be made to convert
    ***        the value to the class of <code>dft</code>
    *** @param dftClass If specified, attempts to convert the value to this
    ***        specified class (using {@link #convertToType)
    *** @param replaceKeys True if key refernces in the value should be resolved
    ***        if the value is a string
    *** @return The property value of the key
    **/
    private Object _getProperty(Object key, Object dft, Class dftClass, boolean replaceKeys)
    {
        Object value = this.getProperties().get(key);
        if (value == null) {
            return replaceKeys? this._replaceKeyValues(key,dft) : dft; // no value, return default
        } else
        if ((dft == null) && (dftClass == null)) {
            return replaceKeys? this._replaceKeyValues(key,value) : value; // return as-is
        } else {
            // convert 'value' to same type (class) as 'dft' (if specified)
            Class c = (dftClass != null)? dftClass : dft.getClass();
            try {
                return convertToType(replaceKeys? this._replaceKeyValues(key,value) : value, c);
            } catch (Throwable t) {
                return replaceKeys? this._replaceKeyValues(key,dft) : dft; // inconvertable, return as-is
            }
        }
    }

    /**
    *** Gets the property value of a key
    *** @param key The key to get the property value of
    *** @param dft The default value to return if none found. If specified, an 
    ***        attempt will be made to convert the value to the class of 
    ***        <code>dft</code> (using {@link #convertToType})
    *** @return The property value of the key
    **/
    public Object _getProperty(Object key, Object dft)
    {
        return this._getProperty(key, dft, null/*dftClass*/, false/*replaceKeys*/);
    }

    /**
    *** Gets the property value of a key
    *** @param key The key to get the property value of
    *** @param dft The default value to return if none found. If specified, an 
    ***        attempt will be made to convert the value to the class of 
    ***        <code>dft</code> (using {@link #convertToType})
    *** @return The property value of the key
    **/
    public Object getProperty(Object key, Object dft)
    {
        return this._getProperty(key, dft, null/*dftClass*/, true/*replaceKeys*/);
    }

    /**
    *** Attempts to convert <code>val</code> to the specified class
    *** @param val The object to attempt to convert
    *** @param type The type to attempt to convert <code>val</code> to
    *** @return <code>val</code> converted to <code>type</code>
    *** @throws Throwble If type conversion fails
    **/
    protected static Object convertToType(Object val, Class<?> type)
        throws Throwable
    {
        if ((type == null) || (val == null)) {
            // not converted
            return val;
        } else
        if (type.isAssignableFrom(val.getClass())) {
            // already converted
            return val;
        } else
        if (type == String.class) {
            // convert to String
            return val.toString();
        } else {
            // ie:
            //   new File(String.class)
            //   new Long(String.class)
            try {
                Constructor meth = type.getConstructor(new Class[] { type });
                return meth.newInstance(new Object[] { val });
            } catch (Throwable t1) {
                try {
                    Constructor meth = type.getConstructor(new Class[] { String.class });
                    return meth.newInstance(new Object[] { val.toString() });
                } catch (Throwable t2) {
                    Print.logError("Can't convert value to " + type.getName() + ": " + val);
                    throw t2; // inconvertable
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // String properties

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key
    *** @return The String value, or null if the key is not found
    **/
    public String getString(String key)
    {
        return this.getString(key, null);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft  The default value return if the key is not found
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key[], String dft)
    {
        return this.getString(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key.
    *** @param dft  The default value return if the key is not found
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key, String dft)
    {
        return this.getString(key, dft, true);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key.
    *** @param dft  The default value return if the key is not found
    *** @param replaceKeys  True to perform ${...} key replace, false to return raw String
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key, String dft, boolean replaceKeys)
    {
        Object val = this._getProperty(key, dft, String.class, replaceKeys);
        if (val == null) {
            return null;
        } else
        if (val.equals(RTKey.NULL_VALUE)) {
            return null;
        } else {
            return val.toString();
        }
    }

    /**
    *** Sets the property value for the specified key
    *** @param key    The property key
    *** @param value  The property value to set.
    **/
    public void setString(String key, String value)
    {
        this.setProperty(key, value);
    }
    
    /**
    *** "StringTools.KeyValueMap" interface
    *** @param key  The property key
    *** @param arg  The property argument (not used here)
    *** @param dft  The default value
    *** @return The property value
    **/
    public String getKeyValue(String key, String arg, String dft)
    {
        return this.getString(key, dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a string array property at the specified key
    *** @param key The key of the property
    *** @return The string array value of the property
    *** @see StringTools#parseArray(String s, char arrayDelim)
    **/
    public String[] getStringArray(String key)
    {
        return this.getStringArray(key, null);
    }

    /**
    *** Gets a string array property from a specified array of keys
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft The default value to return if an entry was not found
    *** @return The string array value of the property
    *** @see StringTools#parseArray(String s, char arrayDelim)
    **/
    public String[] getStringArray(String key[], String dft[])
    {
        return this.getStringArray(this.getFirstDefinedKey(key), dft);
    }
    
    /**
    *** Gets a string array property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The string array value of the property
    *** @see StringTools#parseArray(String s, char arrayDelim)
    **/
    public String[] getStringArray(String key, String dft[])
    {
        String val = this.getString(key, null);
        if (val == null) {
            return dft;
        } else {
            String va[] = StringTools.parseArray(val);
            // TODO: check for RTKey.NULL_VALUE in string array
            return va;
        }
    }

    /**
    *** Sets the value of the specified property to the specified string array
    *** @param key The key of the property
    *** @param val The value to set the property to
    *** @see StringTools#encodeArray(Object list[], char delim, boolean alwaysQuote)
    *** @see StringTools#parseArray(String s, char arrayDelim)
    **/
    public void setStringArray(String key, String val[])
    {
        this.setStringArray(key, val, true);
    }

    /**
    *** Sets the value of the specified property to the specified string array
    *** @param key The key of the property
    *** @param val The value to set the property to
    *** @param alwaysQuote True if the strings in the encoded array should 
    ***        always be quoted as literal values. Usually true
    *** @see StringTools#encodeArray(Object list[], char delim, boolean alwaysQuote)
    *** @see StringTools#parseArray(String s, char arrayDelim)
    **/
    public void setStringArray(String key, String val[], boolean alwaysQuote)
    {
        String valStr = StringTools.encodeArray(val, ARRAY_DELIM, alwaysQuote);
        this.setString(key, valStr);
    }

    /**
    *** Sets the value of the specified property to the specified string array
    *** @param key The key of the property
    *** @param val The value to set the property to
    *** @see StringTools#encodeArray(Object list[], char delim, boolean alwaysQuote)
    *** @see StringTools#parseArray(String s, char arrayDelim)
    **/
    public void setProperty(String key, String val[])
    {
        this.setStringArray(key, val, true);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Class properties

    /**
    *** Gets a <code>Class</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>Class</code> value of the property
    **/
    public Class getClass(String key)
    {
        return this.getClass(key, null);
    }

    /**
    *** Gets a <code>Class</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>Class</code> value of the property
    **/
    public Class getClass(String key, Class dft)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Class) {
            return (Class)val;
        } else {
            try {
                return Class.forName(val.toString());
            } catch (Throwable th) {
                return dft;
            }
        }
    }

    /**
    *** Sets the value of the specified property to the specified <code>Class</code>
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setClass(String key, Class value)
    {
        this.setProperty(key, value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // File properties

    /**
    *** Gets a <code>File</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>File</code> value of the property
    **/
    public File getFile(String key)
    {
        return this.getFile(key, null);
    }

    // do not include the following method, otherwise "getFile(file, null)" would be ambiguous
    //public File getFile(String key, String dft)

    /**
    *** Gets a <code>File</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>File</code> value of the property
    **/
    public File getFile(String key, File dft)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof File) {
            return (File)val;
        } else {
            return new File(val.toString());
        }
    }

    /**
    *** Sets the value of the specified property to the specified <code>File</code>
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setFile(String key, File value)
    {
        this.setProperty(key, value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Double properties

    /**
    *** Returns true if the value if the specified key can be converted to a <code>double</code>
    *** @param key    The key of the property
    *** @param strict True to test for a strict <code>double</code> value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid <code>double</code> value
    **/
    public boolean isDouble(String key, boolean strict)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        return StringTools.isDouble(val, strict);
    }

    /**
    *** Gets a <code>double</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>double</code> value of the property
    **/
    public double getDouble(String key)
    {
        return this.getDouble(key, 0.0);
    }

    /**
    *** Gets a <code>dobule</code> property at the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>double</code> value of the property
    **/
    public double getDouble(String key[], double dft)
    {
        return this.getDouble(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets a <code>double</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>double</code> value of the property
    **/
    public double getDouble(String key, double dft)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).doubleValue();
        } else {
            return StringTools.parseDouble(val.toString(), dft);
        }
    }

    /**
    *** Gets a <code>double</code> array property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>double</code> array value of the property
    **/
    public double[] getDoubleArray(String key, double dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            double n[] = new double[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseDouble(val[i], 0.0);
            }
            return n;
        }
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>double</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setDouble(String key, double value)
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>double</code> array value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setDoubleArray(String key, double value[])
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>double</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setProperty(String key, double value)
    {
        this.setProperty(key, new Double(value));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Float properties

    /**
    *** Returns true if the value if the specified key can be converted to a <code>float</code>
    *** @param key    The key of the property
    *** @param strict True to test for a strict <code>float</code> value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid <code>float</code> value
    **/
    public boolean isFloat(String key, boolean strict)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        return StringTools.isFloat(val, strict);
    }

    /**
    *** Gets a <code>float</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>float</code> value of the property
    **/
    public float getFloat(String key)
    {
        return this.getFloat(key, 0.0F);
    }

    /**
    *** Gets a <code>float</code> property at the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>float</code> value of the property
    **/
    public float getFloat(String key[], float dft)
    {
        return this.getFloat(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets a <code>float</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>float</code> value of the property
    **/
    public float getFloat(String key, float dft)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).floatValue();
        } else {
            return StringTools.parseFloat(val.toString(), dft);
        }
    }

    /**
    *** Gets a <code>float</code> array property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>float</code> array value of the property
    **/
    public float[] getFloatArray(String key, float dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            float n[] = new float[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseFloat(val[i], 0.0F);
            }
            return n;
        }
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>float</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setFloat(String key, float value)
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>float</code> array value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setFloatArray(String key, float value[])
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>float</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setProperty(String key, float value)
    {
        this.setProperty(key, new Float(value));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // BigInteger properties

    /**
    *** Returns true if the value if the specified key can be converted to a <code>BigInteger</code>
    *** @param key    The key of the property
    *** @param strict True to test for a strict <code>BigInteger</code> value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid <code>BigInteger</code> value
    **/
    public boolean isBigInteger(String key, boolean strict)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        return StringTools.isBigInteger(val, strict);
    }

    /**
    *** Gets a <code>BigInteger</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>BigInteger</code> value of the property
    **/
    public BigInteger getBigInteger(String key)
    {
        return this.getBigInteger(key, BigInteger.ZERO);
    }

    /**
    *** Gets a <code>BigInteger</code> property at the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>BigInteger</code> value of the property
    **/
    public BigInteger getBigInteger(String key[], BigInteger dft)
    {
        return this.getBigInteger(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets a <code>BigInteger</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>BigInteger</code> value of the property
    **/
    public BigInteger getBigInteger(String key, BigInteger dft)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof BigInteger) {
            return (BigInteger)val;
        } else
        if (val instanceof Number) {
            return BigInteger.valueOf(((Number)val).longValue());
        } else {
            return StringTools.parseBigInteger(val.toString(), dft);
        }
    }

    /**
    *** Gets a <code>BigInteger</code> array property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>BigInteger</code> array value of the property
    **/
    public BigInteger[] getBigIntegerArray(String key, BigInteger dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            BigInteger n[] = new BigInteger[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseBigInteger(val[i], BigInteger.ZERO);
            }
            return n;
        }
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>BigInteger</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setBigInteger(String key, BigInteger value)
    {
        this.setProperty(key, (Object)value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>BigInteger</code> array value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setBigIntegerArray(String key, BigInteger value[])
    {
        this.setProperty(key, (Object[])value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>BigInteger</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setProperty(String key, BigInteger value)
    {
        this.setProperty(key, (Object)value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Long properties

    /**
    *** Returns true if the value if the specified key can be converted to a <code>long</code>
    *** @param key    The key of the property
    *** @param strict True to test for a strict <code>long</code> value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid <code>long</code> value
    **/
    public boolean isLong(String key, boolean strict)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        return StringTools.isLong(val, strict);
    }

    /**
    *** Gets a <code>long</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>long</code> value of the property
    **/
    public long getLong(String key)
    {
        return this.getLong(key, 0L);
    }

    /**
    *** Gets a <code>long</code> property at the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>long</code> value of the property
    **/
    public long getLong(String key[], long dft)
    {
        return this.getLong(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets a <code>long</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>long</code> value of the property
    **/
    public long getLong(String key, long dft)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).longValue();
        } else {
            return StringTools.parseLong(val.toString(), dft);
        }
    }

    /**
    *** Gets a <code>long</code> array property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>long</code> array value of the property
    **/
    public long[] getLongArray(String key, long dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            long n[] = new long[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseLong(val[i], 0L);
            }
            return n;
        }
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>long</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setLong(String key, long value)
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>long</code> array value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setLongArray(String key, long value[])
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>long</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setProperty(String key, long value)
    {
        this.setProperty(key, new Long(value));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Integer properties

    /**
    *** Returns true if the value if the specified key can be converted to an <code>int</code>
    *** @param key    The key of the property
    *** @param strict True to test for a strict <code>int</code> value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid <code>int</code> value
    **/
    public boolean isInt(String key, boolean strict)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        return StringTools.isInt(val, strict);
    }

    /**
    *** Gets a <code>int</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>int</code> value of the property
    **/
    public int getInt(String key)
    {
        return this.getInt(key, 0);
    }

    /**
    *** Gets a <code>int</code> property at the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>int</code> value of the property
    **/
    public int getInt(String key[], int dft)
    {
        return this.getInt(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets a <code>int</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>int</code> value of the property
    **/
    public int getInt(String key, int dft)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Number) {
            return ((Number)val).intValue();
        } else {
            return StringTools.parseInt(val.toString(), dft);
        }
    }

    /**
    *** Gets a <code>int</code> array property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>int</code> array value of the property
    **/
    public int[] getIntArray(String key, int dft[])
    {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            int n[] = new int[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseInt(val[i], 0);
            }
            return n;
        }
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>int</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setInt(String key, int value)
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>int</code> array value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setIntArray(String key, int value[])
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>int</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setProperty(String key, int value)
    {
        this.setProperty(key, new Integer(value));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Boolean properties

    /**
    *** Returns true if the value if the specified key can be converted to a <code>boolean</code>
    *** @param key    The key of the property
    *** @param strict True to test for a strict <code>boolean</code> value (ie. does not contain
    ***               any other superfluous trailing characters), false to allow for 
    ***               other non-critical trailing characters.
    *** @return True if the specified String contains a valid <code>boolean</code> value
    **/
    public boolean isBoolean(String key, boolean strict)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        return StringTools.isBoolean(val, strict);
    }

    /**
    *** Gets a <code>boolean</code> property at the specified key
    *** @param key The key of the property
    *** @return The <code>boolean</code> value of the property
    **/
    public boolean getBoolean(String key)
    {
        boolean dft = false;
        return this._getBoolean_dft(key, dft, true);
    }

    /**
    *** Gets a <code>boolean</code> property at the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>boolean</code> value of the property
    **/
    public boolean getBoolean(String key[], boolean dft)
    {
        return this.getBoolean(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets a <code>boolean</code> property at the specified key
    *** @param key The key of the property
    *** @param dft The default value to return if an entry was not found
    *** @return The <code>boolean</code> value of the property
    **/
    public boolean getBoolean(String key, boolean dft)
    {
        return this._getBoolean_dft(key, dft, DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY);
    }

    private boolean _getBoolean_dft(String key, boolean dft, boolean dftTrueIfEmpty)
    {
        Object val = this._getProperty(key, null/*dft*/, null/*dftClass*/, true/*replaceKeys*/);
        if (val == null) {
            return dft;
        } else
        if (val instanceof Boolean) {
            return ((Boolean)val).booleanValue();
        } else
        if (val.toString().equals("")) {
            return dftTrueIfEmpty? true : dft;
        } else {
            return StringTools.parseBoolean(val.toString(), dft);
        }
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>boolean</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setBoolean(String key, boolean value)
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>boolean</code> array value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setBooleanArray(String key, boolean value[])
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the value of the specified property to the specified 
    *** <code>boolean</code> value
    *** @param key The key of the property
    *** @param value The value to set the property to
    **/
    public void setProperty(String key, boolean value)
    {
        this.setProperty(key, new Boolean(value));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Prints the properties contained in this instance to stdout. The properties
    *** are printed as returned by {@link #toString(RTProperties, Collection, String)}
    *** using {@link Print#sysPrintln}
    *** @param msg The first line printed, as a header for the properties list
    **/
    public void printProperties(String msg)
    {
        this.printProperties(msg, null, null);
    }

    /**
    *** Prints the properties contained in this instance to stdout. The properties
    *** are printed as returned by {@link #toString(RTProperties, Collection, String)}
    *** using {@link Print#sysPrintln}
    *** @param msg The first line printed, as a header for the properties list
    *** @param exclProps An <code>RTProperties</code> containing items to exclude
    **/
    public void printProperties(String msg, RTProperties exclProps)
    {
        this.printProperties(msg, exclProps, null);
    }

    /**
    *** Prints the properties contained in this instance to stdout. The properties
    *** are printed as returned by {@link #toString(RTProperties, Collection, String)}
    *** using {@link Print#sysPrintln}
    *** @param msg The first line printed, as a header for the properties list
    *** @param orderBy A <code>Collection</code> of keys to order the properties by
    **/
    public void printProperties(String msg, Collection<?> orderBy)
    {
        this.printProperties(msg, null, orderBy);
    }

    /**
    *** Prints the properties contained in this instance to stdout. The properties
    *** are printed as returned by {@link #toString(RTProperties, Collection, String)}
    *** using {@link Print#sysPrintln}
    *** @param msg The first line printed, as a header for the properties list
    *** @param exclProps An <code>RTProperties</code> containing items to exclude
    *** @param orderBy A <code>Collection</code> of keys to order the properties by
    **/
    public void printProperties(String msg, RTProperties exclProps, Collection<?> orderBy)
    {
        if (!StringTools.isBlank(msg)) {
            Print.sysPrintln(msg);
        }
        String prefix = "   ";
        if (this.isEmpty()) {
            Print.sysPrintln(prefix + "<empty>\n");
        } else {
            if (orderBy == null) {
                orderBy = new Vector<Object>(this.getPropertyKeys());
                ListTools.sort((java.util.List<?>)orderBy, null);
            }
            Print.sysPrintln(this.toString(exclProps, orderBy, prefix));
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Prints the properties contained in this instance to the log output. The properties
    *** are printed as returned by {@link #toString(RTProperties, Collection, String)}
    *** using {@link Print#sysPrintln}
    *** @param msg The first line printed, as a header for the properties list
    **/
    public void logProperties(String msg)
    {
        this.logProperties(msg, null, null);
    }

    /**
    *** Prints the properties contained in this instance to the log output. The properties
    *** are printed as returned by {@link #toString(RTProperties, Collection, String)}
    *** using {@link Print#sysPrintln}
    *** @param msg The first line printed, as a header for the properties list
    *** @param exclProps An <code>RTProperties</code> containing items to exclude
    *** @param orderBy A <code>Collection</code> of keys to order the properties by
    **/
    public void logProperties(String msg, RTProperties exclProps, Collection<?> orderBy)
    {
        String m = (msg != null)? (msg+"\n") : "\n"; 
        String prefix = "   ";
        if (this.isEmpty()) {
            Print.logInfo(m + prefix + "<empty>\n");
        } else {
            if (orderBy == null) {
                orderBy = new Vector<Object>(this.getPropertyKeys());
                ListTools.sort((java.util.List<?>)orderBy, null);
            }
            Print.logInfo(m + this.toString(exclProps, orderBy, prefix));
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this object is equivilent to the specified object
    *** @param other The other object
    *** @return True if this object is equivilent to the specified object
    **/
    public boolean equals(Object other)
    {
        if (other instanceof RTProperties) {
            // We need to perform our own 'equals' checking here:
            // Two RTProperties are equal if they contain the same properties irrespective of ordering.
            // [All property values are compared as Strings]
            RTProperties rtp = (RTProperties)other;
            Map M1 = this.getProperties();
            Map M2 = rtp.getProperties();
            if (M1.size() == M2.size()) {
                for (Iterator i = M1.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    if (M2.containsKey(key)) {
                        Object m1Val = M1.get(key);
                        Object m2Val = M2.get(key);
                        String m1ValStr = (m1Val != null)? m1Val.toString() : null;
                        String m2ValStr = (m2Val != null)? m2Val.toString() : null;
                        if (m1Val == m2Val) {
                            continue; // they are the same object (or both null)
                        } else
                        if ((m1ValStr != null) && m1ValStr.equals(m2ValStr)) {
                            continue; // the values are equals
                        } else {
                            //Print.logInfo("Values not equal: " + m1ValStr + " <==> " + m2ValStr);
                            return false; // values are not equal
                        }
                    } else {
                        //Print.logInfo("Key doesn't exist in M2");
                        return false; // key doesn't exist in M2
                    }
                }
                return true; // all key/vals matched
            } else {
                //Print.logInfo("Sizes don't match");
                return false;
            }
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Saves the properties contained in this <code>RTProperties</code> 
    *** instance to a file
    *** @param cfgFile The file to save the properties contained in this 
    ***        instance to
    *** @throws IOException If an I/O error occurs
    **/
    public void saveProperties(File cfgFile)
        throws IOException
    {

        /* property maps */
        Map propMap = this.getProperties();

        /* encode properties */
        StringBuffer strProps = new StringBuffer();
        for (Iterator i = propMap.keySet().iterator(); i.hasNext();) {
            Object keyObj = i.next();
            Object valObj = propMap.get(keyObj);
            strProps.append(keyObj.toString());
            strProps.append(this.getKeyValueSeparatorChar());
            if (valObj != null) {
                strProps.append(valObj.toString());
            }
            strProps.append("\n");
        }
        
        /* save to file */
        FileTools.writeFile(strProps.toString().getBytes(), cfgFile);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns an array of strings representing the key/value pairs in this 
    *** <code>RTProperties</code>
    *** @param dashPrefix True if the individual properties should be prefixed 
    ***        with a '-'
    *** @return The string array representation of this<code>RTProperties</code>
    **/
    public String[] toStringArray(boolean dashPrefix)
    {
        java.util.List<String> list = new Vector<String>();
        Map<Object,Object> propMap = this.getProperties();
        for (Object keyObj : propMap.keySet()) {
            Object valObj = propMap.get(keyObj);
            StringBuffer sb = new StringBuffer();
            if (dashPrefix) {
                sb.append("-");
            }
            sb.append(keyObj.toString()).append(this.getKeyValueSeparatorChar());
            String v = StringTools.trim(valObj);
            if ((v.indexOf(" ") >= 0) || (v.indexOf("\t") >= 0) || (v.indexOf("\"") >= 0)) {
                sb.append(StringTools.quoteString(v));
            } else {
                sb.append(v);
            }
            list.add(sb.toString());
        }
        return list.toArray(new String[list.size()]);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns a string representation of this <code>RTProperties</code>
    *** @return A string reperesentation of this <code>RTProperties</code>
    **/
    public String toString()
    {
        return this.toString(null, null, null);
    }

    /**
    *** Returns a string representation of this <code>RTProperties</code>
    *** @param exclProps An <code>RTProperties</code> containing items to exclude
    *** @return A string reperesentation of this <code>RTProperties</code>
    **/
    public String toString(RTProperties exclProps)
    {
        return this.toString(exclProps, null, null);
    }

    /**
    *** Returns a string representation of this <code>RTProperties</code>
    *** @param orderBy A <code>Collection</code> of keys to order the properties by
    *** @return A string reperesentation of this <code>RTProperties</code>
    **/
    public String toString(Collection<?> orderBy)
    {
        return this.toString(null, orderBy, null);
    }

    /**
    *** Returns a string representation of this <code>RTProperties</code>
    *** @param exclProps An <code>RTProperties</code> containing items to exclude
    *** @param orderBy A <code>Collection</code> of keys to order the properties by
    *** @return A string reperesentation of this <code>RTProperties</code>
    **/
    public String toString(RTProperties exclProps, Collection<?> orderBy)
    {
        return this.toString(null, orderBy, null);
    }

    /**
    *** Returns a string representation of this <code>RTProperties</code>
    *** @param exclProps An <code>RTProperties</code> containing items to exclude
    *** @param orderBy A <code>Collection</code> of keys to order the properties by
    *** @param newLinePrefix A string to prefix new property lines with, for 
    ***        example, some spaces for indentation
    *** @return A string reperesentation of this <code>RTProperties</code>
    **/
    public String toString(RTProperties exclProps, Collection<?> orderBy, String newLinePrefix)
    {
        StringBuffer sb = new StringBuffer();
        boolean inclNewLine = (newLinePrefix != null);

        /* append name */
        String n = this.getName();
        if (!n.equals("")) {
            if (inclNewLine) {
                sb.append(newLinePrefix);
            }
            sb.append(NameStart).append(n).append(NameEnd);
            if (inclNewLine) {
                sb.append("\n");
            } else {
                sb.append(this.getPropertySeparatorChar());
            }
        }

        /* property maps */
        Map<Object,Object> propMap = this.getProperties();
        Map<Object,Object> exclMap = (exclProps != null)? exclProps.getProperties() : null;

        /* order by */
        Set<Object> orderSet = null;
        if (orderBy != null) {
            orderSet = new OrderedSet<Object>(orderBy, true);
            orderSet.addAll(propMap.keySet());
            // 'orderSet' now contains the union of keys from 'orderBy' and 'propMap.keySet()'
        } else {
            orderSet = propMap.keySet();
        }

        /* encode properties */
        for (Iterator<Object> i = orderSet.iterator(); i.hasNext();) {
            Object keyObj = i.next(); // possible this key doesn't exist in 'propMap' if 'orderBy' used.
            if (!RTKey.NAME.equals(keyObj) && RTProperties.containsKey(propMap,keyObj,this.getAllowBlankValues())) {

                Object valObj = propMap.get(keyObj); // key guaranteed here to be in 'propMap'
                if ((exclMap == null) || !RTProperties.compareMapValues(valObj, exclMap.get(keyObj))) {

                    /* prefix? */
                    if (inclNewLine) {
                        sb.append(newLinePrefix);
                    }

                    /* key/value */
                    if (keyObj instanceof String) {
                        sb.append((String)keyObj);
                        //sb.append("[").append(StringTools.className(keyObj)).append("]");
                        //sb.append("(len=").append(((String)keyObj).length()).append(")");
                    } else {
                        sb.append(keyObj.toString());
                        sb.append("[").append(StringTools.className(keyObj)).append("]");
                    }
                    sb.append(this.getKeyValueSeparatorChar());
                    String valStr = (valObj != null)? valObj.toString() : "";
                    if ((valStr.indexOf(" ") >= 0) || (valStr.indexOf("\t") >= 0) || (valStr.indexOf("\"") >= 0)) {
                        sb.append(StringTools.quoteString(valStr));
                    } else {
                        sb.append(valStr);
                    }

                    /* property separator */
                    if (inclNewLine) {
                        sb.append("\n");
                    } else
                    if (i.hasNext()) {
                        sb.append(this.getPropertySeparatorChar());
                    }

                } else {
                    //Print.logDebug("Key hasn't changed: " + key);
                }
            }
        }
        return inclNewLine? sb.toString() : sb.toString().trim();

    }

    private static boolean compareMapValues(Object value, Object target)
    {
        if ((value == null) && (target == null)) {
            return true;
        } else
        if ((value == null) || (target == null)) {
            return false;
        } else
        if (value.equals(target)) {
            return true;
        } else {
            return value.toString().equals(target.toString());
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static boolean isEOL(byte b)     { return ((b == '\n') || (b == '\r')); }
    private static boolean isEOL(char b)     { return ((b == '\n') || (b == '\r')); }
    private static boolean isCOMMENT(byte b) { return ((b == '#')  || (b == '!') ); }
    private static boolean isCOMMENT(char b) { return ((b == '#')  || (b == '!') ); }
    private static boolean isSEP(byte b)     { return ((b == '=')  || (b == ':') ); }
    private static boolean isSEP(char b)     { return ((b == '=')  || (b == ':') ); }

    /* config file 'include' */
    private static final String KEY_INCLUDE_URL         = RTKey.INCLUDE;      // ("%include") file _MUST_ exist
    private static final String KEY_INCLUDE_URL_OPT     = RTKey.INCLUDE_OPT;  // ("%include?") file _may_ exist
    private static final String KEY_LOG                 = RTKey.LOG;          // ("%log")
    private static final String KEY_DEBUGMODE           = "%debugMode";       // 
    private static final String KEY_IF                  = "%if";              // 
    private static final String KEY_ELSE                = "%else";            // 
    private static final String KEY_ENDIF               = "%endif";           // 
    //private static final String KEY_IFTRUE_             = "%ifTrue-";         // 
    //private static final String KEY_IFFALSE_            = "%ifFalse-";        // 
    //private static final String KEY_IFDEF_              = "%ifDef-";          // 
    //private static final String KEY_IFNOTDEF_           = "%ifNotDef-";       // 
    private static final int    MAX_INCLUDE_RECURSION   = 3; // reasonable max recursion (including 'main ' file)

    /**
    *** OrderedProperties class
    **/
    public class OrderedProperties
        extends Properties
    {

        private boolean debugMode = false;
        private int recursionLevel = 0;
        private OrderedMap<String,String> orderedMap = null;
        private URL inputURL = null;

        public OrderedProperties(URL inputURL) {
            this(1, inputURL); // arbitrarily call the starting level, the 'first' recursion level
        }
        private OrderedProperties(int recursion, URL inputURL) {
            super();
            this.recursionLevel = recursion;
            this.orderedMap     = new OrderedMap<String,String>();
            this.inputURL       = inputURL;
        }

        public Object put(Object key, Object value) {
            if ((key == null) || (value == null)) {
                return value;
            }
            String ks = key.toString();
            String vs = StringTools.trimTrailing(value); // trim trailing
            if (ks.startsWith(RTKey.CONSTANT_PREFIX)) {
                if (this.debugMode) {
                    Print.logInfo("(DEBUG) Found Constant key: " + ks);
                }
                if (ks.equalsIgnoreCase(KEY_DEBUGMODE)) {
                    this.debugMode = StringTools.parseBoolean(vs,false);
                    if (this.debugMode) {
                        Print.logInfo("(DEBUG) 'debugMode' set to " + this.debugMode);
                    }
                    return value;
                } else
                if (ks.equalsIgnoreCase(KEY_INCLUDE_URL) || ks.equalsIgnoreCase(KEY_INCLUDE_URL_OPT)) {
                    String v = RTConfig.insertKeyValues(vs, this.orderedMap); // replace any reference variables
                    if (StringTools.isBlank(v)) {
                        Print.logError("Invalid/blank 'include' URL: " + vs);
                    } else
                    if (this.recursionLevel >= MAX_INCLUDE_RECURSION) { 
                        Print.logWarn("Excessive 'include' recursion [%s] ...", v);
                    } else {
                        InputStream uis = null;
                        URL url = null;
                        try {
                            if (this.debugMode) {
                                Print.logInfo("(DEBUG) Including: " + v);
                            }
                            url = new URL(v);
                            String parent   = (this.inputURL != null)? this.inputURL.toString() : "";
                            String parProto = (this.inputURL != null)? this.inputURL.getProtocol().toLowerCase() : "";
                            String urlProto = url.getProtocol().toLowerCase();
                            String urlPath  = url.getPath();
                            //Print.logInfo("Protocol '%s' Path '%s'", urlProto, urlPath);
                            if (StringTools.isBlank(parProto)) {
                                // no parent URL, leave this URL as-is
                            } else
                            if (parProto.equals(INCLUDE_PROTOCOL_FILE)) {
                                // parent URL is "file:/...."
                                if (urlProto.equals(INCLUDE_PROTOCOL_FILE) && !(new File(urlPath)).isAbsolute()) {
                                    // included URL is "file:..." with relative path.  construct absolute URL
                                    int ls = parent.lastIndexOf("/");
                                    if (ls > 0) {
                                        url = new URL(parent.substring(0,ls+1) + urlPath);
                                    }
                                }
                            } else
                            if (parProto.startsWith(INCLUDE_PROTOCOL_HTTP)) { // http, https
                                // parent URL is "http[s]://...."
                                if (urlProto.equals(INCLUDE_PROTOCOL_FILE)) {
                                    // cannot specify included "file:/..." from "http[s]://..."
                                    Print.logError("Invalid 'include' URL protocol: " + url);
                                    url = null;
                                } else
                                if (urlProto.equals(parProto) && !urlPath.startsWith("/")) {
                                    // included URL is "http[s]:..." with relative path.  construct absolute URL
                                    int cs = parent.indexOf("://");
                                    int ls = parent.lastIndexOf("/");
                                    if ((cs > 0) && (ls >= (cs + 3))) {
                                        url = new URL(parent.substring(0,ls+1) + urlPath);
                                    }
                                }
                            } else {
                                // unrecognized URL, leave as-is
                            }
                            if (url != null) {
                                if (this.debugMode) {
                                    Print.logInfo("(DEBUG) Including URL: ["+vs+"] " + url);
                                }
                                uis = url.openStream(); // may throw MalformedURLException
                                OrderedProperties props = new OrderedProperties(this.recursionLevel + 1, url);
                                props.put(RTKey.CONFIG_URL, url.toString());  // save CONFIG_URL for internal referencing
                                this.loadProperties(props, uis);
                                props.remove(RTKey.CONFIG_URL);               // remove CONFIG_URL before saving to parent properties
                                this.orderedMap.putAll(props.getOrderedMap());
                            }
                        } catch (MalformedURLException mue) {
                            Print.logException("Invalid URL: " + url, mue);
                        } catch (IllegalArgumentException iae) {
                            Print.logException("Invalid URL arguments: " + url, iae);
                        } catch (Throwable th) { // IOException, UnknownHostException
                            if (!ks.equalsIgnoreCase(KEY_INCLUDE_URL_OPT)) {
                                Print.logException("Error including properties: " + url, th);
                            } else {
                                //Print.logWarn("Unable to include URL: " + v);
                            }
                        } finally {
                            if (uis != null) { try { uis.close(); } catch (IOException ioe) {/*ignore*/} }
                        }
                    }
                    return value;
                } else
                if (ks.equalsIgnoreCase(KEY_LOG)) {
                    if (RTProperties.this.getConfigLogMessagesEnabled()) {
                        // not very efficient, but this doesn't need to be efficient since config files are seldom loaded.
                        StringBuffer sb = new StringBuffer();
                        if (this.inputURL != null) {
                            String filePath = this.inputURL.getPath();
                            int p = filePath.lastIndexOf("/");
                            String fileName = (p >= 0)? filePath.substring(p+1) : filePath;
                            sb.append("[").append(fileName).append("] ");
                        }
                        RTProperties tempProps = new RTProperties(this);
                        RTConfig.pushTemporaryProperties(tempProps);
                        Print.resetVars();
                        sb.append(RTConfig.insertKeyValues(vs,this.orderedMap)).append("\n");
                        Print._writeLog(Print.LOG_INFO, sb.toString());
                        RTConfig.popTemporaryProperties(tempProps);
                    }
                    return value;
                } else
                if (ks.startsWith(KEY_IF) || ks.startsWith(KEY_ELSE) || ks.startsWith(KEY_ENDIF)) {
                    Print.logError("'%if..%else..%endif' NOT SUPPORTED !!!");
                    return "";
                } else
                /*
                if (ks.startsWith(KEY_IFTRUE_)) {
                    // EXPERIMENTAL - note: boolean must be defined within this same context
                    String  ifKey  = ks.substring(KEY_IFTRUE_.length());
                    Object  ifVal  = this.orderedMap.get(ifKey);
                    boolean isTrue = StringTools.parseBoolean(ifVal,false);
                    if (isTrue) {
                        int eq = vs.indexOf("=");
                        if (eq >= 0) {
                            String ifk = vs.substring(0,eq);
                            String ifv = vs.substring(eq+1);
                            return this.put(ifk,ifv);
                        }
                    }
                    return value;
                } else
                if (ks.startsWith(KEY_IFFALSE_)) {
                    // EXPERIMENTAL - note: boolean must be defined within this same context
                    String  ifKey   = ks.substring(KEY_IFFALSE_.length());
                    Object  ifVal   = this.orderedMap.get(ifKey);
                    boolean isFalse = !StringTools.parseBoolean(ifVal,false);
                    if (isFalse) {
                        int eq = vs.indexOf("=");
                        if (eq >= 0) {
                            String ifk = vs.substring(0,eq);
                            String ifv = vs.substring(eq+1);
                            return this.put(ifk,ifv);
                        }
                    }
                    return value;
                } else
                if (ks.startsWith(KEY_IFDEF_)) {
                    // EXPERIMENTAL - note: var must be defined within this same context
                    String  ifKey   = ks.substring(KEY_IFDEF_.length());
                    boolean isDef   = this.orderedMap.containsKey(ifKey);
                    if (isDef) {
                        int eq = vs.indexOf("=");
                        if (eq >= 0) {
                            String ifk = vs.substring(0,eq);
                            String ifv = vs.substring(eq+1);
                            return this.put(ifk,ifv);
                        }
                    }
                    return value;
                } else
                if (ks.startsWith(KEY_IFNOTDEF_)) {
                    // EXPERIMENTAL - note: var must be defined within this same context
                    String  ifKey   = ks.substring(KEY_IFNOTDEF_.length());
                    boolean isNotDef= !this.orderedMap.containsKey(ifKey);
                    if (isNotDef) {
                        int eq = vs.indexOf("=");
                        if (eq >= 0) {
                            String ifk = vs.substring(0,eq);
                            String ifv = vs.substring(eq+1);
                            return this.put(ifk,ifv);
                        }
                    }
                    return value;
                } else
                */
                if (ks.equalsIgnoreCase(RTKey.CONFIG_URL)) {
                    // special case assignment because the constant '%configURL' key is placed in
                    // the Properties map that is currently being loaded
                    Object rtn = super.put(key, value);
                    this.orderedMap.put(ks, vs);
                    return rtn;
                } else {
                    // invalid key reference
                    Print.logError("Invalid/unrecognized key specified: " + ks);
                    return value;
                }
            } else {
                Object rtn = super.put(key, value);
                this.orderedMap.put(ks, vs);
                return rtn;
            }
        }

        public Object remove(Object key) {
            if (key != null) {
                Object rtn = super.remove(key);
                this.orderedMap.remove(key.toString());
                return rtn;
            } else {
                return null;
            }
        }
        public OrderedMap<String,String> getOrderedMap() {
            return this.orderedMap;
        }

        public void load(Reader r) throws IOException {
            throw new UnsupportedOperationException("load(Reader) not supported");
        }
        public void load(InputStream in) throws IOException {
            super.load(in);
        }
        public Properties loadProperties(Properties props, InputStream in) throws IOException {
            /* invalid arguments? */
            if ((props == null) || (in == null)) {
                return null;
            }
            /* load using standard Properties "load(...)" */
            if (RTConfig.getBoolean("RTConfig.usePropertiesLoad",USE_PROPERTIES_LOADER)) {
                // Warning! '<Properties>.load' requires character encoding "ISO-8859-1"
                // ("props.put(key,value)" is used for insertion)
                props.load(in);
                return props;
            }
            /* parse input stream */
            //Print.logWarn("Non-standard Properties file loading ...");
            byte data[] = FileTools.readStream(in);
            /* read stream into String */
            String dataStr = StringTools.toStringValue(data);
            String ds[] = StringTools.split(dataStr,'\n');
            /* loop through lines */
            int ifLevel = 0;
            boolean inclDef = true;
            for (int i = 0; i < ds.length; i++) {
                int line = i + 1;
                /* trim string */
                String d = ds[i].trim();
                int dlen = d.length();
                /* skip blank lines and comments */
                if (d.equals("") || isCOMMENT(d.charAt(0))) { 
                    continue; 
                }
                /* check for conditionals */
                // %if ! abc=123
                // %else
                // %endif
                if (d.startsWith(KEY_IF)) {
                    int c = KEY_IF.length();
                    if ((d.length() <= c) || (d.charAt(c) != ' ')) {
                        // error
                        Print.logError("*** ["+line+"] Invalid '%if' specification: " + d);
                        continue;
                    } else
                    if (ifLevel > 0) {
                        // error
                        Print.logError("*** ["+line+"] Nested '%if' not supported");
                        continue;
                    }
                    ifLevel++; // next level
                    // skip "%if", and interleaving spaces
                    while ((c < dlen) && Character.isWhitespace(d.charAt(c))) { c++; }
                    if (c >= dlen) {
                        // error
                        Print.logError("*** ["+line+"] Missing conditional after '%if'");
                        continue; 
                    }
                    // extract key
                    int k = c;
                    while ((c < dlen) && ((d.charAt(c) != '!') && (d.charAt(c) != '='))) { c++; }
                    String ifKey = (c < dlen)? d.substring(k,c).trim() : d.substring(k).trim();
                    // extract compare operator: "=", "==", "!="
                    boolean not = false, hasComp = false;
                    int t = c;
                    while ((c < dlen) && ((d.charAt(c) == '!') || (d.charAt(c) == '='))) { c++; }
                    String comp = (c > t)? d.substring(t,c) : "";
                    if (comp.equals("")) {
                        hasComp = false;
                        not = false;
                    } else 
                    if (comp.equals("=") || comp.equals("==")) {
                        hasComp = true;
                        not = false;
                    } else
                    if (comp.equals("!=")) {
                        hasComp = true;
                        not = true;
                    } else {
                        // error
                        Print.logError("*** ["+line+"] Invalid condition operator: " + comp);
                        continue; 
                    }
                    // extract value
                    String ifVal = (c < dlen)? d.substring(c).trim() : hasComp? "" : null;
                    // get actual value
                    String pkVal = null;
                    if (props.containsKey(ifKey)) {
                        pkVal = props.getProperty(ifKey);
                        //Print.logInfo("Found '"+ifKey+"' in local properties : " + pkVal);
                    } else
                    if (this.orderedMap.containsKey(ifKey)) {
                        pkVal = this.orderedMap.get(ifKey);
                        //Print.logInfo("Found '"+ifKey+"' in OrderedProperties : " + pkVal);
                    } else {
                        pkVal = RTConfig.getString(ifKey,null);
                        //if (pkVal != null) { Print.logInfo("Found '"+ifKey+"' in OrderedProperties : " + pkVal); }
                    }
                    // compare
                    if (ifVal != null) {
                        // a comparison value has been specified, true if equal, false otherwise
                        if (pkVal != null) {
                            // key is defined, compare
                            boolean eq = ifVal.equalsIgnoreCase(pkVal);
                            inclDef = not? !eq : eq;
                        } else {
                            // key is not defined, assume blank value
                            boolean eq = StringTools.isBlank(ifVal);
                            inclDef = not? !eq : eq;
                        }
                    } else {
                        // no comparison value specified, true if key defined, false otherwise
                        boolean eq = (pkVal != null);
                        inclDef = not? !eq : eq;
                    }
                    continue; 
                } else
                if (d.startsWith(KEY_ELSE)) {
                    if (ifLevel <= 0) {
                        // error
                        Print.logError("*** ["+line+"] '%else' without previous '%if'");
                        ifLevel = 0;
                        inclDef = true;
                        continue;
                    }
                    inclDef = !inclDef;
                    if (d.length() > KEY_ELSE.length()) {
                        Print.logWarn("*** ["+line+"] Invalid characters following '%else'");
                    }
                    continue; 
                } else
                if (d.startsWith(KEY_ENDIF)) {
                    ifLevel--;
                    if (ifLevel < 0) {
                        // error
                        Print.logError("*** ["+line+"] '%endif' without previous '%if'");
                        ifLevel = 0;
                        inclDef = true;
                        continue;
                    } else
                    if (ifLevel == 0) {
                        inclDef = true;
                    }
                    if (d.length() > KEY_ENDIF.length()) {
                        Print.logWarn("*** ["+line+"] Invalid characters following '%endif'");
                    }
                    continue; 
                } else
                if (d.startsWith(KEY_LOG)) {
                    //Print.logInfo("Parsing %log ...");
                    if (inclDef && RTProperties.this.getConfigLogMessagesEnabled()) {
                        // not very efficient, but this doesn't need to be efficient since config files are seldom loaded.
                        String msg = d.substring(KEY_LOG.length()).trim();
                        if ((msg.length() > 0) && isSEP(msg.charAt(0))) { msg = msg.substring(1).trim(); }
                        StringBuffer sb = new StringBuffer();
                        if (this.inputURL != null) {
                            String filePath = this.inputURL.getPath();
                            int p = filePath.lastIndexOf("/");
                            String fileName = (p >= 0)? filePath.substring(p+1) : filePath;
                            sb.append("[").append(fileName).append("] ");
                        }
                        RTProperties tempProps = new RTProperties(this);
                        RTConfig.pushTemporaryProperties(tempProps);
                        Print.resetVars();
                        sb.append(RTConfig.insertKeyValues(msg,this.orderedMap).trim()).append("\n");
                        Print._writeLog(Print.LOG_INFO, sb.toString());
                        RTConfig.popTemporaryProperties(tempProps);
                    }
                    continue; 
                }
                /* omit this line? */
                if (!inclDef) {
                    //Print.logInfo("Skipping config rcd ["+line+"]: " + d);
                    continue;
                }
                /* find key/value separator */
                int p = d.indexOf("=");
                if (p < 0) { p = d.indexOf(":"); }
                /* parse key/value */
                String key = (p >= 0)? d.substring(0,p) : d;
                String val = (p >= 0)? d.substring(p+1) : "";
                if (!key.equals("")) {
                    //Print.logInfo("S)Prop: " + key + " ==> " + val);
                    //props.setProperty(key, val);
                    props.put(key, val);
                }
            }
            if (!inclDef) {
                // error
                Print.logError("*** Missing '%endif'");
            }
            return props;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        /*
        RTConfig.setCommandLineArgs(argv, new String[] { // validateKeyAttributes
            "s:",
            "b,bb:m,b",
            "f,ff,fff:f",
            "d,dd,ddd,dddd:d",
            "i=i",
            "g=o",
        });
        */
        
        RTProperties rtp = new RTProperties("-test=\"Hello World\" -another=test hello= world=");
        rtp.printProperties("Test RTProperties:");
        
    }

}
