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
//  2007/01/25  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import org.opengts.util.*;

public interface GeocodeProvider
{

    /** 
    *** Returns the name of this GeocodeProvider 
    **/
    public String getName();

    /**
    *** Returns true if this GeocodeProvider is enabled
    *** @return True if this GeocodeProvider is enabled, false otherwise
    **/
    public boolean isEnabled();

    /** 
    *** Return true if this operation will take less than 20ms to complete 
    *** (The returned value is used to determine whether the 'getGeocode' operation
    *** should be performed immediately, or lazily.)
    **/
    public boolean isFastOperation();

    /**
    *** Returns GeoPoint of specified address
    **/
    public GeoPoint getGeocode(String address, String country);

}
