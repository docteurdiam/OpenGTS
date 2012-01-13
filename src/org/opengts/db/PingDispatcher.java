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
//  2008/08/17  Martin D. Flynn
//     -Initial release
//  2009/04/02  Martin D. Flynn
//     -Added 'arg' parameter to 'sendDevicePing'
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public interface PingDispatcher
{

    // ------------------------------------------------------------------------

    /**
    *** Returns true if 'ping' is supported for the specified device
    *** @param device The device
    *** @return True if 'ping' is supported for the specified device
    **/
    public boolean isPingSupported(Device device);
    
    /**
    *** Sends a 'ping' notification to the specified Device
    *** @param device  The device to which the command is to be sent.
    *** @param cmdType The command type (DCServerConfig.COMMAND_*)
    *** @param cmdName The command name
    *** @param cmdArgs The argument to the "ping" command sent to the device.
    *** @return True if the 'ping' was sent successfully.
    **/
    public boolean sendDeviceCommand(Device device, String cmdType, String cmdName, String cmdArgs[]);
    
    // ------------------------------------------------------------------------

}
