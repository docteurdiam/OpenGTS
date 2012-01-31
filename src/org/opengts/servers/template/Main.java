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
//  Main Entry point for a Template example server
// ----------------------------------------------------------------------------
// Change History:
//  2006/06/30  Martin D. Flynn
//     -Initial release
//  2006/07/27  Martin D. Flynn
//     -Moved constant information to 'Constants.java'
//  2009/08/07  Martin D. Flynn
//     -Updated to use DCServerConfig
// ----------------------------------------------------------------------------
package org.opengts.servers.template;

import java.lang.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;

public class Main
{
    
    // ------------------------------------------------------------------------

    /* command-line argument keys */
  //public  static final String ARG_DEVCODE[]   = new String[] { "devcode", "dcs" , "serverid" };
    public  static final String ARG_PARSEFILE[] = new String[] { "parse"  , "parseFile" };
    public  static final String ARG_HELP[]      = new String[] { "help"   , "h"   };
    public  static final String ARG_TCP_PORT[]  = new String[] { "tcp"    , "p"   , "port" };
    public  static final String ARG_UDP_PORT[]  = new String[] { "udp"    , "p"   , "port" };
    public  static final String ARG_CMD_PORT[]  = new String[] { "command", "cmd" };
    public  static final String ARG_START[]     = new String[] { "start"  };
    public  static final String ARG_DEBUG[]     = new String[] { "debug"  };
    public  static final String ARG_FORMAT[]    = new String[] { "format" , "parseFormat" };
    public  static final String ARG_INSERT[]    = new String[] { "insert" };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* return server config */
    public static String getServerName()
    {
        return Constants.DEVICE_CODE;
    }

    /* return server config */
    public static DCServerConfig getServerConfig()
    {
        return DCServerFactory.getServerConfig(getServerName());
    }

    // ------------------------------------------------------------------------

    /* get server TCP ports (first check command-line, then config file) */
    public static int[] getTcpPorts()
    {
        DCServerConfig dcs = getServerConfig();
        if (dcs != null) {
            return dcs.getTcpPorts();
        } else {
            Print.logError("DCServerConfig not found for server: " + getServerName());
            return null;
        }
    }

    /* get server UDP ports (first check command-line, then config file) */
    public static int[] getUdpPorts()
    {
        DCServerConfig dcs = getServerConfig();
        if (dcs != null) {
            return dcs.getUdpPorts();
        } else {
            Print.logError("DCServerConfig not found for server: " + getServerName());
            return null;
        }
    }

    /* get server ports (first check command-line, then config file, then default) */
    public static int getCommandDispatcherPort()
    {
        DCServerConfig dcs = getServerConfig();
        if (dcs != null) {
            return dcs.getCommandDispatcherPort();
        } else {
            return RTConfig.getInt(ARG_CMD_PORT,0);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main entry point

    /* display usage and exit */
    private static void usage(String msg)
    {
        String tcp = StringTools.join(getTcpPorts(),",");
        String udp = StringTools.join(getUdpPorts(),",");
        String cmd = String.valueOf(getCommandDispatcherPort());

        /* print message */
        if (msg != null) {
            Print.logInfo(msg);
        }

        /* print usage */
        String className = Main.class.getName();
        Print.logInfo("");
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + className + " -h[elp]");
        Print.logInfo(" or");
        Print.logInfo("  java ... " + className + " -parseFile=<filePath>");
        Print.logInfo(" or");
        Print.logInfo("  java ... " + className + " [-port=<port>[,<port>]] -start");
        Print.logInfo("Options:");
        Print.logInfo("  -help               This help");
        Print.logInfo("  [-port=<p>[,<p>]]   Server TCP/UDP port(s) to listen");
        Print.logInfo("  [-tcp=<p>[,<p>]]    Server TCP port(s) to listen on [dft="+tcp+"]");
        Print.logInfo("  [-udp=<p>[,<p>]]    Server UDP port(s) to listen on [dft="+udp+"]");
        Print.logInfo("  [-command=<p>]      Command port to listen on [dft="+cmd+"]");
        Print.logInfo("  [-dcs=<serverId>]   DCServer ID [dft="+Constants.DEVICE_CODE+"]");
        Print.logInfo("  [-format=<parser#>] Parser Format #");
        Print.logInfo("  -start              Start server on the specified port.");
        Print.logInfo("  -parseFile=<file>   File from which data will be parsed.");
        Print.logInfo("");

        /* exit */
        System.exit(1);

    }

    /* main entry point */
    public static void main(String argv[])
    {
        /* init configuration constants */
        TrackClientPacketHandler.configInit();
        TrackServer.configInit();

        /* header */
        String SEP = "--------------------------------------------------------------------------";
        Print.logInfo(SEP);
        Print.logInfo(Constants.TITLE_NAME + " Server Version " + Constants.VERSION);
        Print.logInfo("DeviceCode           : " + Constants.DEVICE_CODE);
        Print.logInfo("ParseFormat          : " + TrackClientPacketHandler.DATA_FORMAT_OPTION);
        Print.logInfo("MinimumSpeed         : " + TrackClientPacketHandler.MINIMUM_SPEED_KPH);
        Print.logInfo("EstimateOdom         : " + TrackClientPacketHandler.ESTIMATE_ODOMETER);
        Print.logInfo("TCP Idle Timeout     : " + TrackServer.getTcpIdleTimeout()    + " ms");
        Print.logInfo("TCP Packet Timeout   : " + TrackServer.getTcpPacketTimeout()  + " ms");
        Print.logInfo("TCP Session Timeout  : " + TrackServer.getTcpSessionTimeout() + " ms");
        Print.logInfo("UDP Idle Timeout     : " + TrackServer.getUdpIdleTimeout()    + " ms");
        Print.logInfo("UDP Packet Timeout   : " + TrackServer.getUdpPacketTimeout()  + " ms");
        Print.logInfo("UDP Session Timeout  : " + TrackServer.getUdpSessionTimeout() + " ms");
        Print.logInfo(Constants.COPYRIGHT);
        Print.logInfo(SEP);

        /* explicit help? */
        if (RTConfig.getBoolean(ARG_HELP,false)) {
            Main.usage("Help ...");
            // control doesn't reach here
            System.exit(0);
        }

        /* make sure the DB is properly initialized */
        if (!DBAdmin.verifyTablesExist()) {
            Print.logFatal("MySQL database has not yet been properly initialized");
            System.exit(1);
        }

        /* 'parseFile'? */
        if (RTConfig.hasProperty(ARG_PARSEFILE)) {
            Print.sysPrintln("Attempting to parse data from file: " + RTConfig.getString(ARG_PARSEFILE));
            RTConfig.setString("parseFile", RTConfig.getString(ARG_PARSEFILE));
            int exit = TrackClientPacketHandler._main(true);
            System.exit(exit);
        }

        /* start server */
        if (RTConfig.getBoolean(ARG_START,false)) {
            
            /* start port listeners */
            try {
                int tcpPorts[]  = getTcpPorts();
                int udpPorts[]  = getUdpPorts();
                int commandPort = getCommandDispatcherPort();
                TrackServer.startTrackServer(tcpPorts, udpPorts, commandPort);
            } catch (Throwable t) { // trap any server exception
                Print.logError("Error: " + t);
            }
            
            /* wait here forever while the server is running in a thread */
            while (true) { 
                try { Thread.sleep(60L * 60L * 1000L); } catch (Throwable t) {} 
            }
            // control never reaches here
            
        }

        /* display usage */
        Main.usage("Missing '-start' ...");
        // control doesn't reach here
        System.exit(1);

    }

}
