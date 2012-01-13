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
//  2011/08/21  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;

public interface JSONRequestHandler
{

    /**
    *** Handle a JSON request
    *** @param context  The "context" of the JSON request
    *** @param jsonObj  The JSON object containing the request
    *** @return The JSON respons object
    **/
    public JSON handleJSONRequest(String context, RequestProperties reqState, JSON jsonObj);
    
}
