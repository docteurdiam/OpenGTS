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
//  Provides a per-thread Map instance
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** A thread local map container
*** @see java.lang.ThreadLocal
*** @see java.util.Map
**/

public class ThreadLocalMap<K,V>
    extends ThreadLocal<Map<K,V>>
    implements Map<K,V>
{

    // ------------------------------------------------------------------------

    private Class<Map<K,V>>         mapClass = null;
    private ThreadLocal<Map<K,V>>   threadLocal = null;

    /**
    *** Constructor
    **/
    public ThreadLocalMap()
    {
        this(null);
    }

    /**
    *** Constructor
    *** @param mapClass The class to use for the supporting Map
    **/
    public ThreadLocalMap(final Class<Map<K,V>> mapClass) 
    {
        super();
        this.mapClass = mapClass;
        this.threadLocal = new ThreadLocal<Map<K,V>>() {
            protected Map<K,V> initialValue() {
                if (ThreadLocalMap.this.mapClass == null) {
                    return new Hashtable<K,V>();
                } else {
                    try {
                        return ThreadLocalMap.this.mapClass.newInstance(); // throw ClassCastException
                    } catch (Throwable t) {
                        // Give up and try a Hashtable
                        Print.logException("Error instantiating: " + StringTools.className(ThreadLocalMap.this.mapClass), t);
                        return new Hashtable<K,V>();
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the class of the supporting Map
    *** @return The class of the supporting Map
    **/
    public Class<Map<K,V>> getMapClass() 
    {
        return this.mapClass;
    }

    /**
    *** Gets the supporting map
    *** @return The supporting map
    **/
    protected Map<K,V> getMap()
    {
        Map<K,V> map = this.threadLocal.get();
        if (map == null) {
            Print.logError("'<ThreadLocal>.get()' has return null!");
        }
        return map;
    }

    // ------------------------------------------------------------------------
    // Map interface

    /**
    *** Removes all of the mappings from this map 
    **/
    public void clear()
    {
        this.getMap().clear();
    }

    /**
    *** Returns true if this map contains a mapping for the specified key
    *** @param key key whose presence in this map is to be tested
    *** @return True if this map contains a mapping for the specified key
    **/
    public boolean containsKey(Object key)
    {
        return this.getMap().containsKey(key);
    }

    /**
    *** Returns true if this map maps one or more keys to the
    *** specified value
    *** @param value value whose presence in this map is to be tested
    *** @return True if this map maps one or more keys to the
    ***         specified value
    **/
    public boolean containsValue(Object value)
    {
        return this.getMap().containsValue(value);
    }

    /**
    *** Returns a Set view of the mappings contained in this map
    *** @return A Set view of the mappings contained in this map
    **/
    public Set<Map.Entry<K,V>> entrySet()
    {
        return this.getMap().entrySet();
    }

    /**
    *** Compares a specified object with this map for equality.  Returns
    *** true if the given object is a map representing the same mappings.
    *** @param o object to be compared for equality with this map
    *** @return True if the specified object is equal to this map
    **/
    public boolean equals(Object o)
    {
        if (o instanceof ThreadLocalMap) {
            return this.getMap().equals(((ThreadLocalMap)o).getMap());
        } else {
            return false;
        }
    }

    /**
    *** Gets a value to which a specified value is mapped
    *** @param key The key whose associated value is to be returned
    *** @return The value to which the specified key is mapped, or
    ***         null if this map contains no mapping for the key
    **/
    public V get(Object key)
    {
        return (key != null)? this.getMap().get(key) : null;
    }

    /**
    *** Returns the hash code value for this map
    *** @return The hash code value for this map
    **/
    public int hashCode()
    {
        return this.getMap().hashCode();
    }

    /**
    *** Returns true if the map is empty
    *** @return Ture if the map is empty
    **/
    public boolean isEmpty()
    {
        return this.getMap().isEmpty();
    }

    /**
    *** Returns a Set view of the keys contained in this map
    *** @return A Set view of the keys contained in this map
    **/
    public Set<K> keySet()
    {
        return this.getMap().keySet();
    }

    /**
    *** Associates a specied value with a specified key
    *** @param key key with which the specified value is to be associated
    *** @param value value to be associated with the specified key
    *** @return The previous value associated with <code>key</code>, or null
    **/
    public V put(K key, V value)
    {
        if (key == null) {
            Print.logStackTrace("Null key");
            return null;
        } else
        if (value == null) {
            this.getMap().remove(key);
            return null;
        } else {
            return this.getMap().put(key, value);
        }
    }

    /**
    *** Copies all of the mappings from the specified map to this map
    *** @param t map whose mappings will be stored in this map
    **/
    public void putAll(Map<? extends K, ? extends V> t)
    {
        this.getMap().putAll(t);
    }
    
    /**
    *** Removes the mapping for a key from this map if it is present
    *** @param key The key whose value will be removed
    *** @return The previous value associated with <code>key</key>, or null
    **/
    public V remove(Object key)
    {
        return this.getMap().remove(key);
    }

    /**
    *** Returns the number of key-value mappings in this map
    *** @return The number of key-value mappings in this map
    **/
    public int size()
    {
        return this.getMap().size();
    }

    /**
    *** Returns a Collection view of the values contained in this map
    *** @return A Collection view of the values contained in this map
    **/
    public Collection<V> values()
    {
        return this.getMap().values();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
