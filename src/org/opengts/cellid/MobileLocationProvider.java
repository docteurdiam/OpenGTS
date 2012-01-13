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
//  2011/07/01  Martin D. Flynn
//      -Initial release
// ----------------------------------------------------------------------------
package org.opengts.cellid;

import org.opengts.util.*;

/**
*** Interface for obtaining the location of a Cell Tower
**/
public interface MobileLocationProvider
{

    /** 
    *** Returns the name of this MobileLocationProvider 
    **/
    public String getName();

    /**
    *** Returns true if this MobileLocationProvider is enabled
    *** @return True if this MobileLocationProvider is enabled, false otherwise
    **/
    public boolean isEnabled();

    /**
    *** Returns the location of Cell Tower indicated by the attributes
    *** specified in the CellTower instance.
    *** @param servCT  The serving Cell Tower information
    *** @param nborCT  Neightbor Cell Tower information
    *** @return The Mobile location of the Cell Tower, or null if no
    ***     location could be determined.
    **/
    public MobileLocation getMobileLocation(CellTower servCT, CellTower nborCT[]);

}
