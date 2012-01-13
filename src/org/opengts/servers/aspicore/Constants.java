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
// Description:
//  Server Initialization
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/18  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.servers.aspicore;

import org.opengts.*;
import org.opengts.util.*;
import org.opengts.db.*;

public class Constants
{

    // ------------------------------------------------------------------------

    /* title/version/copyright */
    public static final String  TITLE_NAME                  = "Aspicore Server";
    public static final String  VERSION                     = "0.1.0";
    public static final String  COPYRIGHT                   = org.opengts.Version.COPYRIGHT;

    // ------------------------------------------------------------------------

    /* device code */
    public static final String  DEVICE_CODE                 = DCServerFactory.ASPICORE_NAME;

    // ------------------------------------------------------------------------

    /* ASCII packets */
    public static final boolean ASCII_PACKETS               = false;
    public static final int     ASCII_LINE_TERMINATOR[]     = new int[] { '\r', '\n' };
    public static final int     ASCII_IGNORE_CHARS[]        = null;

    /* packet length */
    public static final int     MIN_PACKET_LENGTH           = 1;
    public static final int     MAX_PACKET_LENGTH           = 800;
    
    /* terminate flags */
    public static final boolean TERMINATE_ON_TIMEOUT        = true;

    // ------------------------------------------------------------------------

    /* TCP Timeouts */
    public static final long    TIMEOUT_TCP_IDLE            =    -1L;
    public static final long    TIMEOUT_TCP_PACKET          =    -1L;
    public static final long    TIMEOUT_TCP_SESSION         =    -1L;

    /* UDP Timeouts */
    public static final long    TIMEOUT_UDP_IDLE            =  2000L;
    public static final long    TIMEOUT_UDP_PACKET          =  3000L;
    public static final long    TIMEOUT_UDP_SESSION         = 10000L;
    
    /* linger on close */
    public static final int     LINGER_ON_CLOSE_SEC         = 1;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        Print.sysPrintln(VERSION);
    }

}
