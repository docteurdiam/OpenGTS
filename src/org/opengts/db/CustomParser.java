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
//  2010/10/25  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.db.tables.*;

public interface CustomParser
{

    // ------------------------------------------------------------------------

    public static final String  ACCOUNT     = "$account";   // Account
    public static final String  DEVICE      = "$device";    // Device
    public static final String  DATA        = "$data";      // byte[]
    public static final String  DUPLEX      = "$duplex";    // Boolean

    // ------------------------------------------------------------------------

    /**
    *** Callback to parse raw data received from a remote tracking device through its
    *** device communication server.
    *** @param account  The assigned device Account instance
    *** @param device   The addigned Device instance
    *** @param data     The byte array containing the raw data
    *** @param props    A map where parsed data should be placed (to be inserted into the EventData record)
    *** @return The response which will be sent back to the device
    **/
    public byte[] parseData(Account account, Device device, byte data[], Map<String,Object> props);

}
