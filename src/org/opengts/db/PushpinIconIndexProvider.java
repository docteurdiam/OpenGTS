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
//  2010/05/24  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import org.opengts.util.*;
import org.opengts.db.tables.*;

public abstract class PushpinIconIndexProvider
{
    
    public PushpinIconIndexProvider()
    {
        super();
    }

    // ------------------------------------------------------------------------

    public int getPushpinIconIndex(EventData event, 
        String iconSelector, OrderedSet<String> iconKeys,
        boolean isFleet, BasicPrivateLabel bpl)
    {
        // override
        return -1;
    }
        
}

