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
//  This class provides many String based utilities.
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/04  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

/**
*** Provides various String parsing/format utilities
**/

public enum TriState
    implements EnumTools.IntValue
{

    // ------------------------------------------------------------------------

    /* Enum value definition */
    UNKNOWN     ( -1, "unknown"  ), // default
    FALSE       (  0, "false"    ),
    TRUE        (  1, "true"     );

    // ------------------------------------------------------------------------

    private int         value = 0;
    private String      text  = null;

    // ------------------------------------------------------------------------

    /**
    *** Enum Constructor
    **/
    TriState(int v, String t) 
    { 
        this.value = v; 
        this.text  = t; 
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the integer value of this instance
    *** @return The integer value of this instance
    **/
    public int getIntValue()
    { 
        return this.value; 
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the String representation of this instance
    *** @return The String representation of this instance
    **/
    public String toString()
    { 
        return text.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this is the default value (UNKNOWN) or this TriState
    *** @return True if this is the default value (UNKNOWN) or this TriState
    **/
    public boolean isDefault()
    { 
        return this.equals(UNKNOWN);
    }

    /**
    *** Returns true if the value is "Unknown"
    *** @return True if the value is "Unknown"
    **/
    public boolean isUnknown()
    { 
        return this.equals(UNKNOWN); 
    }

    /**
    *** Returns true if the value is "True"
    *** @return True if the value is "True"
    **/
    public boolean isTrue()
    { 
        return this.equals(TRUE); 
    }

    /**
    *** Returns true if the value is "False"
    *** @return True if the value is "False"
    **/
    public boolean isFalse()
    { 
        return this.equals(FALSE); 
    }

}
