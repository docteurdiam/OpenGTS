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
//  Main Entry point for I-Care G3300 server
// ----------------------------------------------------------------------------
// Change History:
//  2009/04/02  Martin D. Flynn
//     -Initial release
//  2009/05/01  Martin D. Flynn
//     -Added an "autoregister" feature for use with custom server implementation.
// ----------------------------------------------------------------------------
package org.opengts.servers.icare;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public class Main
{

    // ------------------------------------------------------------------------

    /* command-line argument keys */
    public  static final String ARG_HELP[]      = new String[] { "h", "help" };
    public  static final String ARG_PORT[]      = new String[] { "p", "port" };
    public  static final String ARG_START       = "start";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return server config */
    public static String getServerName()
    {
        return DCServerFactory.ICARE_NAME;
    }

    /* return server config */
    public static DCServerConfig getServerConfig()
    {
        return DCServerFactory.getServerConfig(getServerName());
    }

    /* get server 'listen' ports (first check command-line, then config file) */
    public static int[] getListenPorts()
    {
        DCServerConfig dcs = getServerConfig();
        if (dcs != null) {
            return dcs.getListenPorts();
        } else {
            Print.logError("DCServerConfig not found for server: " + getServerName());
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main entry point

    /* display usage and exit */
    private static void usage(String msg)
    {
        String ports = StringTools.join(Main.getListenPorts(),",");

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
        Print.logInfo("  [-port=<p>[,<p>]]   Server port(s) to listen on [dft="+ports+"]");
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

        /* header */
        Print.logInfo("----------------------------------------------------------------");
        Print.logInfo(Constants.TITLE_NAME + " Server Version " + Constants.VERSION);
        Print.logInfo(Constants.COPYRIGHT);
        Print.logInfo("----------------------------------------------------------------");

        /* explicit help? */
        if (RTConfig.getBoolean(ARG_HELP,false)) {
            Main.usage("Help ...");
            // control doesn't reach here
        }

        /* check server registry */
        String serverName = Main.getServerName();   // current server name
        if (!DCServerFactory.hasServerConfig(serverName)) {
            // This will only occur if this server is used as the starting point for a different device
            // communication server, and "Main.getServerName()" no longer returns "icare".
            Print.logWarn("This server name is not registered with DCServerConfig: %s", serverName);
            if (RTConfig.getBoolean("autoregister",false)) {
                String dftDescr   = "Custom Server";        // default description
                int    listenPort = 31200;                  // default listen port
                String prefix[]   = new String[] { Constants.UNIQUE_ID_PREFIX_IMEI, Constants.UNIQUE_ID_PREFIX_ALT, "" };
                // The following line is a work-around if you do not wish to add this server to DCServerConfig:
                int ports[] = new int[] { listenPort };
                DCServerFactory.addDCS(serverName,dftDescr,ports,ports,0,DCServerConfig.F_STD_PERSONAL,prefix);
            }
        }

        /* validate ports here */
        int listenPorts[] = Main.getListenPorts();
        if (!DCServerFactory.isValidPort(listenPorts)) {
            Main.usage("Invalid ports specified");
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
                TrackServer.startTrackServer(listenPorts);
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
