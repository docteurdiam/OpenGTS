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
//  2009/01/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;

import org.opengts.util.*;

public class IDDescription
{

    // ------------------------------------------------------------------------

    /**
    *** "SortBy" Enumeration
    **/
    public enum SortBy implements EnumTools.IntValue {
        ID          ( 0 ),
        DESCRIPTION ( 1 ),
        NAME        ( 2 );
        // ---
        private int vv = 0;
        SortBy(int v)               { vv = v; }
        public  int getIntValue()   { return vv; }
    };

    /**
    *** Returns the 'SortBy' Enumeration for the specified sortBy string value
    *** @param sortBy  Text representation of 'SortBy'
    *** @return The 'SortBy' value (is never null)
    **/
    public static IDDescription.SortBy GetSortBy(String sortBy)
    {
        // does not return null
        if (StringTools.isBlank(sortBy) || sortBy.equalsIgnoreCase("id")) {
            return IDDescription.SortBy.ID;
        } else
        if (sortBy.equalsIgnoreCase("name")) {
            return IDDescription.SortBy.NAME;
        } else 
        if (StringTools.startsWithIgnoreCase(sortBy,"desc")) {
            return IDDescription.SortBy.DESCRIPTION;
        } else {
            return IDDescription.SortBy.ID;
        }
    }

    /**
    *** Returns the specified 'SortBy' Enumeration, or the default 'SortBy' if null.
    *** @param sortBy  'SortBy' Enumeration (may be null)
    *** @return The 'SortBy' Enumeration (is never null)
    **/
    public static IDDescription.SortBy GetSortBy(IDDescription.SortBy sortBy)
    {
        return (sortBy != null)? sortBy : IDDescription.SortBy.DESCRIPTION;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** SortComparator class
    **/
    private static class SortComparator
        implements Comparator<IDDescription>
    {
        private IDDescription.SortBy sortBy = IDDescription.SortBy.ID;
        public SortComparator(IDDescription.SortBy sortBy) {
            this.sortBy = (sortBy != null)? sortBy : IDDescription.SortBy.ID;
        }
        public int compare(IDDescription id1, IDDescription id2) {
            String s1 = (id1 != null)? id1.getSortByString(this.sortBy) : "";
            String s2 = (id1 != null)? id2.getSortByString(this.sortBy) : "";
            return s1.compareTo(s2);
        }
        public boolean equals(Object other) {
            if (other instanceof SortComparator) {
                SortComparator sc = (SortComparator)other;
                return this.sortBy.equals(sc.sortBy);
            }
            return false;
        }
    }

    /**
    *** Sorts the specified IDDescription list by the specified field
    *** @param idList The IDDescription list to sort (in place)
    *** @param sortBy The name of the field by which to sort
    *** @return The argment idList
    **/
    public static java.util.List<IDDescription> SortList(java.util.List<IDDescription> idList, String sortBy)
    {
        return IDDescription.SortList(idList, GetSortBy(sortBy));
    }

    /**
    *** Sorts the specified IDDescription list by the specified field
    *** @param idList The IDDescription list to sort (in place)
    *** @param sortBy The name of the field by which to sort
    *** @return The argment idList
    **/
    public static java.util.List<IDDescription> SortList(java.util.List<IDDescription> idList, IDDescription.SortBy sortBy)
    {
        return ListTools.sort(idList, new SortComparator(sortBy));
    }
    
    // ------------------------------------------------------------------------

    private String id           = null;
    private String description  = null;
    private String name         = null;

    /**
    *** Constructor
    *** @param id    The object ID
    *** @param desc  The object description
    **/
    public IDDescription(String id, String desc) 
    {
        this(id, desc, null);
    }

    /**
    *** Constructor
    *** @param id    The object ID
    *** @param desc  The object description
    *** @param name  The object name
    **/
    public IDDescription(String id, String desc, String name) 
    {
        this.id          = id;
        this.description = desc;
        this.name        = name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the object ID
    *** @return The Object ID
    **/
    public String getID() 
    {
        return this.id;
    }

    /**
    *** Returns the object Description
    *** @return The Object Description
    **/
    public String getDescription() 
    {
        return this.description;
    }
    
    /**
    *** Returns the object Name
    *** @return The Object Description
    **/
    public String getName() 
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the object Description/Name/ID (first item which isn't null)
    *** @return The Object Description/Name/ID
    **/
    public String getSortByString(IDDescription.SortBy sortBy)
    {
        String val = this.getID();
        if (sortBy != null) {
            switch (sortBy) {
                case ID:            val = this.getID();             break;
                case DESCRIPTION:   val = this.getDescription();    break;
                case NAME:          val = this.getName();           break;
            }
        }
        return (val != null)? val.toLowerCase() : "";
    }

    /**
    *** Returns the object String representation
    *** @return The object String representation
    **/
    public String toString() 
    {
        if (this.description != null) {
            return this.description;
        } else
        if (this.name != null) {
            return this.name;
        } else {
            return this.id;
        }
    }

    // ------------------------------------------------------------------------

}
