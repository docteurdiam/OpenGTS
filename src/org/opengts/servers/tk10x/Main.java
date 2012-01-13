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
//  2011/07/15  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.servers.tk10x;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.Version;
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
    public  static final String ARG_START[]     = new String[] { "start"  };

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
        return DCServerFactory.getServerConfig(Main.getServerName());
    }

    // ------------------------------------------------------------------------

    /* get server TCP ports (first check command-line, then config file) */
    public static int[] getTcpPorts()
    {
        DCServerConfig dcs = Main.getServerConfig();
        if (dcs != null) {
            return dcs.getTcpPorts();
        } else {
            Print.logError("DCServerConfig not found for server: " + Main.getServerName());
            return null;
        }
    }

    /* get server UDP ports (first check command-line, then config file) */
    public static int[] getUdpPorts()
    {
        DCServerConfig dcs = Main.getServerConfig();
        if (dcs != null) {
            return dcs.getUdpPorts();
        } else {
            Print.logError("DCServerConfig not found for server: " + Main.getServerName());
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
            return 0;
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
        Print.logInfo("  java ... " + StringTools.className(Main.class) + " {options}");
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

        /* init configuration constants */
        TrackClientPacketHandler.configInit();
        TrackServer.configInit();

        /* header */
        Print.logInfo("----------------------------------------------------------------");
        Print.logInfo(Constants.TITLE_NAME);
        Print.logInfo("Version: " + Constants.VERSION + " [" + Version.getVersion() +  "]");
        Print.logInfo(Constants.COPYRIGHT);
        Print.logInfo("----------------------------------------------------------------");
        Print.logInfo("Minimum speed      : " + TrackClientPacketHandler.MINIMUM_SPEED_KPH + " km/h");
        Print.logInfo("Estimating Odometer: " + TrackClientPacketHandler.ESTIMATE_ODOMETER);
        Print.logInfo("Simulating Geozone : " + TrackClientPacketHandler.SIMEVENT_GEOZONES);
        Print.logInfo("Packet Length      : " + (TrackClientPacketHandler.PACKET_LEN_END_OF_STREAM?"End-Of-Stream":"End-Of-Line Character"));
        Print.logInfo("----------------------------------------------------------------");

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
            
            /* wait here forever while the server is running in a thread */
            while (true) { 
                try { Thread.sleep(60L * 60L * 1000L); } catch (Throwable t) {} 
            }
            // control never reaches here
            
        }

        /* display usage */
        Main.usage("Missing '-start' ...");

    }

}
