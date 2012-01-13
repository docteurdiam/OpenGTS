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
//  2010/05/24  ZhongShan SIPGEAR Technology Co, Ltd. (updated by Martin D. Flynn)
//     -Initial release
//  2011/01/28  Martin D. Flynn
//     -Fixed "imei:" parsing (previously it parsed "imei" as the literal MobileID).
//  2011/06/16  Martin D. Flynn
//     -Added packet terminating character ')'
// ----------------------------------------------------------------------------
// Note:
//  - This device communication server module has been provided by the company
//    ZhongShan SIPGEAR Technology Co, Ltd (through modificiation of other available
//    DCS modules within the OpenGTS.  It has been heavily modified to support some 
//    of the lateted features of OpenGTS.
//  - Geotelematic Solutions, Inc. and OpenGTS are not affiliated with ZhongShan 
//    SIPGEAR Technology Co, Ltd.
// ----------------------------------------------------------------------------
package org.opengts.servers.sipgear;

import org.opengts.*;
import org.opengts.util.*;
import org.opengts.db.DCServerFactory;
import org.opengts.db.DCServerConfig;

public class Constants
{

    // ------------------------------------------------------------------------

    /* title */
    public static final String  TITLE_NAME                  = "SipGear DCS";
    public static final String  VERSION                     = "0.3.6";
    public static final String  COPYRIGHT                   = Version.COPYRIGHT;

    // ------------------------------------------------------------------------

    /* device code */
    public static final String  DEVICE_CODE                 = DCServerFactory.SIPGEAR_NAME;

    /* device code */
    public static final String  CFG_packetLenEndOfStream    = DEVICE_CODE + ".packetLenEndOfStream";

    // ------------------------------------------------------------------------

    /* ASCII packets*/
    public static final boolean ASCII_PACKETS               = false;
    public static final int     ASCII_LINE_TERMINATOR[]     = new int[] { 
        // this list has been construction by observation of various data packets
        0x00, 0xFF, 0xCE, '\n', '\r', ')'
    };
    public static final int     ASCII_IGNORE_CHARS[]        = null; // new int[] { 0x00 };

    /* packet length */
    public static final int     MIN_PACKET_LENGTH           = 1;
    public static final int     MAX_PACKET_LENGTH           = 600;
    
    /* terminate flags */
    public static final boolean TERMINATE_ON_TIMEOUT        = true;

    // ------------------------------------------------------------------------

    /* TCP Timeouts */
    public static final long    TIMEOUT_TCP_IDLE            = 20000L;
    public static final long    TIMEOUT_TCP_PACKET          = 4000L;
    public static final long    TIMEOUT_TCP_SESSION         = 60000L;

    /* UDP Timeouts */
    public static final long    TIMEOUT_UDP_IDLE            = 5000L;
    public static final long    TIMEOUT_UDP_PACKET          = 4000L;
    public static final long    TIMEOUT_UDP_SESSION         = 60000L;
    
    /* linger on close */
    public static final int     LINGER_ON_CLOSE_SEC         = 2;
    
    // ------------------------------------------------------------------------
    
    /* minimum acceptable speed */
    public static final double  MINIMUM_SPEED_KPH           = 3.0;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        Print.sysPrintln(VERSION);
    }

}
