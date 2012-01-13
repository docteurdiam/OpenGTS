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
//  Main Entry point
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/18  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.servers.aspicore;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public class Main
{

    // ------------------------------------------------------------------------

    /* command-line argument keys */
    public  static final String ARG_HELP[]      = new String[] { "help"   , "h"   };
    public  static final String ARG_CMD_PORT[]  = new String[] { "command", "cmd" };
    public  static final String ARG_START[]     = new String[] { "start"  };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String DCServerFactory_LoadName() { return Constants.DEVICE_CODE; }

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

    public static String getUniqueIDPrefixList()
    {
        DCServerConfig dcsc = Main.getServerConfig();
        if (dcsc != null) {
            return DCServerFactory.getUniquePrefixString(dcsc.getUniquePrefix());
        } else {
            return "";
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

        /* print message */
        if (msg != null) {
            Print.logInfo(msg);
        }

        /* print usage */
        Print.logInfo("");
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Main.class.getName() + " {options}");
        Print.logInfo("Options:");
        Print.logInfo("  [-h[elp]]           Print this help");
        Print.logInfo("  [-port=<p>[,<p>]]   Server TCP/UDP port(s) to listen");
        Print.logInfo("  [-tcp=<p>[,<p>]]    Server TCP port(s) to listen on [dft="+tcp+"]");
        Print.logInfo("  [-udp=<p>[,<p>]]    Server UDP port(s) to listen on [dft="+udp+"]");
        Print.logInfo("  -start              Start server on the specified port");
        Print.logInfo("");

        /* exit */
        System.exit(1);

    }

    /* main entry point */
    public static void main(String argv[])
    {

        /* configure server for MySQL data store */
        DBConfig.cmdLineInit(argv,false);  // main
        DBConfig.check_GTS_HOME();

        /* init configuration constants */
        TrackClientPacketHandler.configInit();
        TrackServer.configInit();

        /* header */
        String SEP = "--------------------------------------------------------------------------";
        Print.logInfo(SEP);
        Print.logInfo(Constants.TITLE_NAME);
        Print.logInfo(Constants.COPYRIGHT);
        Print.logInfo("Version: " + 
            Constants.VERSION + 
            "  [" + org.opengts.Version.getCompileTime() + "]"
            );
        Print.logInfo("Unique-ID Prefix(s)   : " + Main.getUniqueIDPrefixList());
        Print.logInfo("Minimum Speed (KPH)   : " + TrackClientPacketHandler.MINIMUM_SPEED_KPH);
        Print.logInfo("Estimate GPS Odometer : " + TrackClientPacketHandler.ESTIMATE_ODOMETER);
        Print.logInfo("Simulate Arrive/Depart: " + TrackClientPacketHandler.SIMEVENT_GEOZONES);
        Print.logInfo("TCP Idle Timeout      : " + TrackServer.getTcpIdleTimeout()    + " ms");
        Print.logInfo("TCP Packet Timeout    : " + TrackServer.getTcpPacketTimeout()  + " ms");
        Print.logInfo("TCP Session Timeout   : " + TrackServer.getTcpSessionTimeout() + " ms");
        Print.logInfo("UDP Idle Timeout      : " + TrackServer.getUdpIdleTimeout()    + " ms");
        Print.logInfo("UDP Packet Timeout    : " + TrackServer.getUdpPacketTimeout()  + " ms");
        Print.logInfo("UDP Session Timeout   : " + TrackServer.getUdpSessionTimeout() + " ms");
        Print.logInfo(SEP);

        /* explicit help? */
        if (RTConfig.getBoolean(ARG_HELP,false)) {
            Main.usage("Help ...");
            // control doesn't reach here
        }

        /* make sure the DB is properly initialized */
        if (!DBAdmin.verifyTablesExist()) {
            Print.logFatal("MySQL database has not yet been properly initialized");
            System.exit(1);
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
            Print.logInfo(SEP);

            /* wait here forever while the server is running in a thread */
            while (true) { 
                try { Thread.sleep(60L * 60L * 1000L); } catch (Throwable t) {} 
            }
            // control never reaches here
            
        }

        /* display usage */
        Main.usage("Missing '-start' ...");
        // control doesn't reach here
        System.exit(99);

    }

}
