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
//  2007/11/28  Martin D. Flynn
//     -Initial release
//  2008/05/14  Martin D. Flynn
//     -Reordered argument list to 'containsPoint'
// ----------------------------------------------------------------------------
package org.opengts.util;

import org.opengts.util.*;

/**
*** Inteface for functions finding if a point is inside of a polygon on a spherical surface
**/

public interface GeozoneChecker
{

    /**
    *** Checks if a specified point is inside the specified polygon on the surface of a sphere
    *** @param gpTest The point to check if is inside the polygon
    *** @param gpList The array of GeoPoints forming the polygon
    *** @param radiusKM The radius of the sphere that the points lie on
    *** @return True if the specified point is inside the polygon
    **/
    public boolean containsPoint(GeoPoint gpTest, GeoPoint gpList[], double radiusKM);
    
}
