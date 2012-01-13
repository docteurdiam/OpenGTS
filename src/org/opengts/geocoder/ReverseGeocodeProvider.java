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
// Some [reverse]geocoder options/references:
//   http://www.johnsample.com/articles/GeocodeWithSqlServer.aspx   (incl reverse)
//   http://www.extendthereach.com/products/OSGeocoder.srct
//   http://datamining.anu.edu.au/student/honours-proj2005-geocoding.html
//   http://geocoder.us/
//   http://www.nacgeo.com/reversegeocode.asp
//   http://wsfinder.jot.com/WikiHome/Maps+and+Geography
/// ----------------------------------------------------------------------------
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2010/07/04  Martin D. Flynn
//     -Added "isEnabled" method
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import org.opengts.util.*;

public interface ReverseGeocodeProvider
{
    
    /**
    *** Returns the name of this ReverseGeocodeProvider 
    *** @return The name of this ReverseGeocodeProvider
    **/
    public String getName();

    /**
    *** Returns true if this ReverseGeocodeProvider is enabled
    *** @return True if this ReverseGeocodeProvider is enabled, false otherwise
    **/
    public boolean isEnabled();

    /**
    *** Returns true if this operation will take less than about 20ms to complete
    *** the returned value is used to determine whether the 'getReverseGeocode' operation
    *** should be performed immediately, or lazily (ie. in a separate thread).
    *** @return True if this is a fast (ie. local) operation
    **/
    public boolean isFastOperation();

    /**
    *** Returns the best address for the specified GeoPoint 
    *** @return The reverse-geocoded adress
    **/
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr);

}
