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
//  2009/01/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;

import org.opengts.war.tools.*;

public class ComboMap
    extends OrderedMap<String,String>
{

    // ------------------------------------------------------------------------

    /**
    *** Returns a ComboMap containing a mapping of "no"/"yes" to the localized text
    *** @param locale  The Locale
    *** @return The ComboMap
    **/
    public static ComboMap getYesNoMap(Locale locale)
    {
        I18N i18n = I18N.getI18N(ComboMap.class, locale);
        ComboMap yesNo = new ComboMap();
        yesNo.add(ComboOption.BOOLEAN_NAME_FALSE, ComboOption.getYesNoText(locale, false));
        yesNo.add(ComboOption.BOOLEAN_NAME_TRUE , ComboOption.getYesNoText(locale, true ));
        return yesNo;
    }

    /**
    *** Returns a ComboMap containing a mapping of "false"/"true" to the localized text
    *** @param locale  The Locale
    *** @return The ComboMap
    **/
    public static ComboMap getTrueFalseMap(Locale locale)
    {
        I18N i18n = I18N.getI18N(ComboMap.class, locale);
        ComboMap trueFalse = new ComboMap();
        trueFalse.add(ComboOption.BOOLEAN_NAME_FALSE, ComboOption.getTrueFalseText(locale, false));
        trueFalse.add(ComboOption.BOOLEAN_NAME_TRUE , ComboOption.getTrueFalseText(locale, true ));
        return trueFalse;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private ComboOption     defaultSelection    = null;
    
    /**
    *** Constructor
    **/
    public ComboMap()
    {
        super();
    }

    /**
    *** Constructor
    *** @param map  The map copied to this ComboMap
    **/
    public ComboMap(Map<String,String> map)
    {
        super(map);
    }

    /**
    *** Constructor
    *** @param list  The list copied to this ComboMap (descriptions will be the same as the corresponding key)
    **/
    public ComboMap(Collection<String> list)
    {
        super();
        if (list != null) {
            for (String k : list) {
                this.put(k,k);
            }
        }
    }

    /**
    *** Constructor
    *** @param list  The list copied to this ComboMap (descriptions will be the same as the corresponding key)
    **/
    public ComboMap(String list[])
    {
        super();
        if (list != null) {
            for (String k : list) {
                this.put(k,k);
            }
        }
    }

    /**
    *** Constructor
    *** @param key  A single key copied to this ComboMap (the description will be the same as the key)
    **/
    public ComboMap(String key)
    {
        super();
        String k = StringTools.trim(key);
        this.put(k,k);
    }

    /**
    *** Constructor
    *** @param key  A single key copied to this ComboMap
    *** @param desc The description associated with the key
    **/
    public ComboMap(String key, String desc)
    {
        super();
        String k = StringTools.trim(key);
        String d = StringTools.trim(desc);
        this.put(k,d);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default selected option
    *** @param option  The default selected option
    **/
    public void setDefaultSelection(ComboOption option)
    {
        this.defaultSelection = option;
    }
    
    /**
    *** Gets the default selected option
    *** @return The default selected option (may be null)
    **/
    public ComboOption getDefaultSelection()
    {
        return this.defaultSelection;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Inserts the specified key/description as the first entry in the map
    *** @param key  The key
    *** @param desc The description
    **/
    public void insert(String key, String desc)
    {
        String k = StringTools.trim(key);
        String d = StringTools.isBlank(desc)? k : StringTools.trim(desc);
        this.put(0, k, d);
    }

    /**
    *** Inserts the specified key as the first entry in the map
    *** @param key  The key (the description will be the same as the key)
    **/
    public void insert(String key)
    {
        this.insert(key, key);
    }

    /**
    *** Inserts the specified ComboOption as the first entry in the map
    *** @param option  The ComboOption
    **/
    public void insert(ComboOption option)
    {
        if (option != null) {
            this.insert(option.getKey(), option.getDescription());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified key/description to the end (last entry) of the map
    *** @param key  The key
    *** @param desc The description
    **/
    public void add(String key, String desc)
    {
        String k = StringTools.trim(key);
        String d = StringTools.isBlank(desc)? k : StringTools.trim(desc);
        this.put(k, d);
    }

    /**
    *** Adds the specified key to the end (last entry) of the map
    *** @param key  The key (the description will be the same as the key)
    **/
    public void add(String key)
    {
        this.add(key, key);
    }

    /**
    *** Adds the specified ComboOption to the end (last entry) of the map
    *** @param option  The ComboOption
    **/
    public void add(ComboOption option)
    {
        if (option != null) {
            this.add(option.getKey(), option.getDescription());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns this first ComboOption in the list
    *** @return The first ComboOption
    **/
    public ComboOption getFirstComboOption()
    {
        String key  = this.getFirstKey(); // may be null
        String desc = this.getFirstValue(); // may be null
        return new ComboOption(key, desc);
    }

    /**
    *** Returns a ComboOption for the specified key
    *** @param key  The key
    *** @return A ComboOption
    **/
    public ComboOption getComboOption(String key)
    {
        String desc = this.get(key); // may be null
        return new ComboOption(key, desc);
    }

    /**
    *** Returns a ComboOption for the specified key
    *** @param key  The key
    *** @return A ComboOption
    **/
    public static ComboOption getComboOption(ComboMap map, String key)
    {
        return (map != null)? map.getComboOption(key) : new ComboOption(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the String representation of this ComboMap
    *** @return The String representation of this ComboMap
    **/
    public String toString()
    {
        StringBuffer sb= new StringBuffer();
        for (String k : this.keySet()) {
            String d = this.get(k);
            sb.append(k);
            sb.append("|");
            sb.append(d);
            sb.append("\n");
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

}
