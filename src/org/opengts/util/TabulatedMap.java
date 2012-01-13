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
//  2010/09/09  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

public class TabulatedMap<KEYTYPE,VALTYPE>
{

    // ------------------------------------------------------------------------

    private Map<KEYTYPE,Vector<VALTYPE>>    tabMap = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public TabulatedMap()
    {
        this.tabMap = new HashMap<KEYTYPE,Vector<VALTYPE>>();
    }

    // ------------------------------------------------------------------------
    
    /** 
    *** Returns an Iterator.  Items are in random order
    **/
    public Iterator<KEYTYPE> getIterator()
    {
        return this.tabMap.keySet().iterator();
    }
    
    /**
    *** Returns an Iterator.  Items are sorted by specified comparator.
    *** @param sortBy  The Comparator by which the returned iterator is sorted
    *** @return The Iterator
    **/
    public Iterator<KEYTYPE> getIterator(Comparator<KEYTYPE> sortBy)
    {
        return this.getKeyList(sortBy).iterator();
    }

    // ------------------------------------------------------------------------

    /**
    *** Return a list of keys
    *** @return A list of keys
    **/
    public java.util.List<KEYTYPE> getKeyList()
    {
        return new Vector<KEYTYPE>(this.tabMap.keySet());
    }

    /**
    *** Return a sorted list of keys
    *** @return A sorted list of keys
    **/
    public java.util.List<KEYTYPE> getKeyList(Comparator<KEYTYPE> sortBy)
    {
        return ListTools.sort(this.getKeyList(),sortBy);
    }

    // ------------------------------------------------------------------------

    /**
    *** Counts the occurrance of the specified key
    *** @param key  The key which will be counted
    **/
    public void add(KEYTYPE key, VALTYPE val)
    {
        Vector<VALTYPE> list = this.tabMap.get(key);
        if (list == null) {
            list = new Vector<VALTYPE>();
            this.tabMap.put(key, list);
        }
        list.add(val);
    }
    
    /**
    *** Returns the count for the specified key
    *** @param key  The key for which the count is returned
    *** @return The count
    **/
    public long getCount(KEYTYPE key) 
    {
        Vector<VALTYPE> list = this.tabMap.get(key);
        return (list != null)? list.size() : 0L;
    }

    // ------------------------------------------------------------------------

}
