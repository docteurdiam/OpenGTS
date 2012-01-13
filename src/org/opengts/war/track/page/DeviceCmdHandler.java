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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;

public interface DeviceCmdHandler
{

    // ------------------------------------------------------------------------

    public void setServerIDArg(String arg);

    public String getServerID();

    public String getServerDescription();

    // ------------------------------------------------------------------------
    
    public boolean deviceSupportsCommands(Device dev);

    public boolean writeCommandForm(PrintWriter out, RequestProperties reqState, Device selDev,
        String actionURL, boolean editProps) throws IOException;
        
    public String handleDeviceCommands(RequestProperties reqState, Device selDev);

    // ------------------------------------------------------------------------

}
