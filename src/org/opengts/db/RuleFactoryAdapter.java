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
//  2008/02/21  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public abstract class RuleFactoryAdapter
    implements RuleFactory
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* adjust action mask */
    public static int ValidateActionMask(int actionMask)
    {
        int m = actionMask & (int)EnumTools.getValueMask(NotifyAction.class);
        if (m != 0) {
            if (((m & ACTION_NOTIFY_MASK) != 0) && ((m & RuleFactory.ACTION_VIA_MASK) == 0)) {
                // Apparently an action notify recipient was specified 
                // (ie Account/Device/Rule), but no 'via' was specified.
                // Enable ACTION_VIA_EMAIL by default.
                m |= RuleFactory.ACTION_VIA_EMAIL;
            }
        }
        return m;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public RuleFactoryAdapter() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    public abstract String getName();
    
    public String getVersion()
    {
        return "0.0.0";
    }

    // ------------------------------------------------------------------------

}
