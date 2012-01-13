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
//     -Saved older version of "template" server 
//      (will be removed in a future release)
// ----------------------------------------------------------------------------
package org.opengts.servers.template_old;

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
    public  static final String ARG_PARSEFILE[] = new String[] { "parse", "parseFile" };
    public  static final String ARG_HELP[]      = new String[] { "h", "help" };
    public  static final String ARG_PORT[]      = new String[] { "p", "port", Constants.CONFIG_SERVER_PORT };
    public  static final String ARG_START[]     = new String[] { "start" };
    public  static final String ARG_DEBUG[]     = new String[] { "debug" };
    public  static final String ARG_FORMAT[]    = new String[] { "format" };
    public  static final String ARG_INSERT[]    = new String[] { "insert" };

    // ------------------------------------------------------------------------

    /* get server ports (first check command-line, then config file, then default) */
    private static int[] _serverPorts()
        throws Throwable
    {

        /* get specified ports */
        String portStr[] = RTConfig.getStringArray(ARG_PORT,null);
        if ((portStr == null) || (portStr.length <= 0)) {
            // return default
            return new int[] { Constants.DEFAULT_PORT };
        }

        /* too many ports? */
        if (portStr.length > Constants.MAX_PORTS) {
            throw new Exception("Too many ports specified");
        }

        /* parse/return port numbers */
        int ports[] = new int[portStr.length];
        for (int i = 0; i < portStr.length; i++) {
            int p = StringTools.parseInt(portStr[i], 0);
            if ((p <= 0) || (p > 65535)) {
                throw new Exception("Invalid port: " + portStr[i]);
            }
            ports[i] = p;
        }
        return ports;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main entry point

    /* display usage and exit */
    private static void usage(String msg)
    {

        /* get default ports */
        StringBuffer ps = new StringBuffer();
        try {
            int ports[] = Main._serverPorts();
            for (int i = 0; i < ports.length; i++) {
                if (i > 0) { ps.append(","); }
                ps.append(ports[i]);
            }
        } catch (Throwable t) {
            ps.append(Constants.DEFAULT_PORT);
        }

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
        Print.logInfo("  -help              This help");
        Print.logInfo("  -port              Server port(s) to listen on [dft="+ps+"].");
        Print.logInfo("  -start             Start server on the specified port.");
        Print.logInfo("  -parseFile=<file>  File from which data will be parsed.");
        Print.logInfo("");

        /* exit */
        System.exit(1);

    }

    /* main entry point */
    public static void main(String argv[])
    {

        /* additional runtime default properties */
        RTKey.addRuntimeEntry(new RTKey.Entry(Constants.CONFIG_SERVER_PORT, Constants.DEFAULT_PORT, "Server 'listen' port"));

        /* configure server for MySQL data store */
        DBConfig.cmdLineInit(argv,false);  // main
        //String badArgs[] = RTConfig.validateCommandLineArgs(new Object[] { 
        //    ARG_HELP, ARG_PORT, ARG_START, ARG_PARSEFILE
        //});
        //if (badArgs != null) {
        //    Main.usage("Invalid argument specified: " + badArgs[0]);
        //    // control doesn't reach here
        //    return;
        //}

        /* header */
        Print.logInfo("----------------------------------------------------------------");
        Print.logInfo(Constants.TITLE_NAME + " Server Version " + Constants.VERSION);
        Print.logInfo(Constants.COPYRIGHT);
        Print.logInfo("----------------------------------------------------------------");

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

        /* validate ports here */
        int listenPorts[] = null;
        try {
            listenPorts = Main._serverPorts();
        } catch (Throwable th) {
            //Print.logError(th.getMessage());
            Main.usage("Error: " + th.getMessage());
            // control doesn't reach here
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
        // control doesn't reach here
        System.exit(1);

    }

}
