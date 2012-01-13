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
//  Server configuration constants
// ----------------------------------------------------------------------------
// Change History:
//  2006/07/27  Martin D. Flynn
//     -Initial release
//  2007/08/09  Martin D. Flynn
//     -Additional comments added
//     -Use "imei_" as the primary IMEI prefix when looking up device unique-id
//  2008/03/12  Martin D. Flynn
//     -Added DEVICE_CODE var for specifying the device type
//  2009/08/07  Martin D. Flynn
//     -Saved older version of "template" server 
//      (will be removed in a future release)
// ----------------------------------------------------------------------------
package org.opengts.servers.template_old;

public class Constants
{

    // ------------------------------------------------------------------------

    /* title */
    // Displayed at server startup
    public static final String  TITLE_NAME              = "Template Example Device Parsing Module";
    public static final String  VERSION                 = "0.1.6";
    public static final String  COPYRIGHT               = org.opengts.Version.COPYRIGHT;

    // ------------------------------------------------------------------------

    /* device code */
    public static final String  DEVICE_CODE             = "template";

    // ------------------------------------------------------------------------

    /* runtime config */
    // This is the property key used to find the port override in the runtime config file
    // To listen on more that one port, this property may contain a comma-serated list of ports.
    public static final String  CONFIG_SERVER_PORT      = DEVICE_CODE + ".port";

    // ------------------------------------------------------------------------

    /* Device UniqueID' key prefix */
    // Used when looking up Devices in the "Device" table
    public static final String  UNIQUE_ID_PREFIX_IMEI   = "imei_";
    public static final String  UNIQUE_ID_PREFIX_ALT    = "template_";

    // ------------------------------------------------------------------------

    /* default port(s) */
    // This is the default "listen" port for this server
    public static final int     DEFAULT_PORT            = 31200;
    // This server can "listen" on a maximum of MAX_PORTS ports.
    public static final int     MAX_PORTS               = 2;
    
    // ------------------------------------------------------------------------

    /* all ASCII packets? */
    // Notes:
    // - Set to 'true' if *ALL* packets are expected to contain only ASCII data.
    //   If 'true', then "<TrackCLientPacketHandler>.getActualPacketLength(...)" will *NOT* 
    //   be called to determine the actual client packet length.
    // - Set to 'false' if the client device sends *ANY* binary packet data.
    //   If 'false', then "<TrackCLientPacketHandler>.getActualPacketLength(...)" *WILL*
    //   be called to allow the parser to determine the actual length of the client
    //   packet based on the first few bytes of the data packet (see 'MIN_PACKET_LENGTH').
    public static final boolean ASCII_PACKETS           = false;
    public static final int     ASCII_LINE_TERMINATOR[] = new int[] { '\r', '\n' };
    
    /* packet length */
    // The minimum expected packet length
    // When starting to read a new packet from the client device, the framework
    // will read 'MIN_PACKET_LENGTH' bytes from the client, then call the method
    // <TrackClientPacketHandler>.getActualPacketLength(...) with the these read
    // bytes.  'getActualPacketLength' should then use these bytes to determine
    // how many total bytes represent the actual packet length.
    public static final int     MIN_PACKET_LENGTH       = 1;
    // The maximum expected packet length
    // This value simply provide an upper limit for the maximum length of any single  
    // packet that is expected to be received from the client device.
    public static final int     MAX_PACKET_LENGTH       = 600;
    
    /* terminate flags */
    // Set to 'true' to close the session on a read timeout
    public static final boolean TERMINATE_ON_TIMEOUT    = true;

    // ------------------------------------------------------------------------

    /* TCP Timeouts (milliseconds) */
    // The time to wait to receive the 1st byte after the session has started
    public static final long    TIMEOUT_TCP_IDLE        = 10000L;
    // After the 1st byte, the remainder of a packet must be read in this timeframe
    public static final long    TIMEOUT_TCP_PACKET      = 4000L;
    // The entire session must complete within this timeframe
    public static final long    TIMEOUT_TCP_SESSION     = 15000L;

    /* UDP Timeouts (milliseconds) */
    // The time to wait to receive the 1st byte after the session has started
    public static final long    TIMEOUT_UDP_IDLE        = 5000L;
    // After the 1st byte, the remainder of a packet must be read in this timeframe
    public static final long    TIMEOUT_UDP_PACKET      = 4000L;
    // The entire session must complete within this timeframe
    public static final long    TIMEOUT_UDP_SESSION     = 60000L;

    // ------------------------------------------------------------------------

    /* minimum acceptable speed value */
    // Speeds below this value should be considered 'stopped'
    public static final double  MINIMUM_SPEED_KPH       = 3.0;

    // ------------------------------------------------------------------------

}
