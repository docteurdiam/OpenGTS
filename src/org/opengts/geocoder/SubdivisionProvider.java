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
//  2007/08/09  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import org.opengts.util.*;

public interface SubdivisionProvider
{

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this operation will take less than about 20ms to complete
    *** the returned value is used to determine whether the 'getReverseGeocode' operation
    *** should be performed immediately, or lazily (ie. in a separate thread).
    *** @return True if this is a fast (ie. local) operation
    **/
    public boolean isFastOperation();

    /**
    *** Return the subdivision of the specified point
    *** (in the US, this is "US/<stateCode>" as in "US/CA")
    **/
    public String getSubdivision(GeoPoint gp);

}
