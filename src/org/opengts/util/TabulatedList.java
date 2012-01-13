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

public class TabulatedList<KEYTYPE>
{

    // ------------------------------------------------------------------------

    private Map<KEYTYPE,AccumulatorLong>     tabList = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public TabulatedList()
    {
        this.tabList = new HashMap<KEYTYPE,AccumulatorLong>();
    }

    // ------------------------------------------------------------------------
    
    /** 
    *** Returns an Iterator.  Items are in random order
    **/
    public Iterator<KEYTYPE> getIterator()
    {
        return this.tabList.keySet().iterator();
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
        return new Vector<KEYTYPE>(this.tabList.keySet());
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
    public void count(KEYTYPE key)
    {
        AccumulatorLong accum = this.tabList.get(key);
        if (accum == null) {
            accum = new AccumulatorLong();
            this.tabList.put(key, accum);
        }
        accum.increment();
    }
    
    /**
    *** Returns the count for the specified key
    *** @param key  The key for which the count is returned
    *** @return The count
    **/
    public long getCount(KEYTYPE key) 
    {
        AccumulatorLong accum = this.tabList.get(key);
        return (accum != null)? accum.get() : 0L;
    }

    // ------------------------------------------------------------------------

}
