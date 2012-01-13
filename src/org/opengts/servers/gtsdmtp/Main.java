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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//  2007/02/18  Martin D. Flynn
//     -Added support for listening on multiple ports
//     -Added command-line option to display help
//  2007/02/26  Martin D. Flynn
//     -No longer maintains a 'static final' copy of the OpenDMTP version.
//      This allows the OpenDMTP version display to properly reflect the version
//      of the loaded 'dmtpserv.jar' file.
//  2007/06/30  Martin D. Flynn
//     -Added ability to set tcp/udp timeouts from the config file.
//  2007/07/27  Martin D. Flynn
//     -Repackaged to "org.opengts.servers.gtsdmtp"
// ----------------------------------------------------------------------------
package org.opengts.servers.gtsdmtp;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.dmtp.*;
import org.opengts.db.tables.*;

import org.opendmtp.server.base.DMTPServer;
import org.opendmtp.server.base.DMTPClientPacketHandler;

public class Main
{
    
    // ----------------------------------------------------------------------------

    /* command-line argument keys */
    public  static final String ARG_HELP[]                  = new String[] { "h", "help" };
    public  static final String ARG_PORT[]                  = new String[] { "p", "port" };
    public  static final String ARG_START[]                 = new String[] { "start" };

    // ------------------------------------------------------------------------

    public  static final String COPYRIGHT                   = org.opengts.Version.COPYRIGHT;

    // ------------------------------------------------------------------------
    
    public  static final String DEVICE_CODE                 = DCServerFactory.OPENDMTP_NAME;
    
    /* runtime config file key */
    public  static final String CFG_firstSessionNegotiation = DEVICE_CODE + ".firstSessionNegotiation";
    public  static final String CFG_udpReturnResponse       = DEVICE_CODE + ".udpReturnResponse";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return server config */
    public static String getServerName()
    {
        return DCServerFactory.OPENDMTP_NAME;
    }

    /* return server config */
    public static DCServerConfig getServerConfig()
    {
        return DCServerFactory.getServerConfig(getServerName());
    }

    /* get server 'listen' ports (first check command-line, then config file) */
    public static int[] getListenPorts()
    {
        DCServerConfig dcs = Main.getServerConfig(); // should not be null
        if (dcs != null) {
            return dcs.getListenPorts();
        } else {
            Print.logError("DCServerConfig not found for server: " + getServerName());
            return new int[0];
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* display usage and exit */
    private static void usage(String msg)
    {

        /* get default ports */
        StringBuffer ps = new StringBuffer();
        int ports[] = Main.getListenPorts();
        for (int i = 0; i < ports.length; i++) {
            if (i > 0) { ps.append(","); }
            ps.append(ports[i]);
        }
    
        /* print message */
        if (msg != null) {
            Print.logInfo(msg);
        }
        
        /* print usage */
        Print.logInfo("");
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Main.class.getName() + " {options}");
        Print.logInfo("Options:");
        Print.logInfo("  [-h[elp]]         Print this help");
        Print.logInfo("  [-port=<p>[,<p>]] Server port(s) to listen on [dft="+ps+"]");
        Print.logInfo("  -start            Start server on the specified port");
        Print.logInfo("");
        
        /* exit */
        System.exit(1);
        
    }

    /* main entry point */
    public static void main(String argv[])
    {

        /* runtime default properties */
        RTKey.addRuntimeEntry(new RTKey.Entry(CFG_firstSessionNegotiation, true, "DMTP allow 1st session custom event negotiation"));
        RTKey.addRuntimeEntry(new RTKey.Entry(CFG_udpReturnResponse      , true, "DMTP return UDP response/ack"));

        /* load runtime config */
        DBConfig.cmdLineInit(argv,false);

        /* register OpenDMTP protocol DB interface */
        DMTPServer.setDBFactory(new DMTPDBFactory());

        /* header */
        Print.logInfo("----------------------------------------------------------------");
        Print.logInfo("OpenDMTP (GTS) Java Server");
        Print.logInfo(COPYRIGHT);
        Print.logInfo("DMTP Version     : " + org.opendmtp.server.Version.getVersion());
        Print.logInfo("GTS  Version     : " + org.opengts.Version.getVersion());
        Print.logInfo("SQL Database Name: " + RTConfig.getString(RTKey.DB_NAME));
        Print.logInfo("Transport Enabled: " + Transport.isTransportQueryEnabled());
        Print.logInfo("Message Log Level: " + Print.getLogLevelString(Print.getLogLevel()));
      //Print.logInfo("Process ID       : " + OSTools.getProcessID()); // useless on Windows
        Print.logInfo("----------------------------------------------------------------");

        /* explicit help? */
        if (RTConfig.getBoolean(ARG_HELP,false)) {
            Main.usage("Help ...");
            // control doesn't reach here
        }

        /* validate ports here */
        int listenPorts[] = Main.getListenPorts();

        /* make sure the DB is properly initialized */
        if (!DBAdmin.verifyTablesExist()) {
            Print.logFatal("MySQL database has not yet been properly initialized");
            System.exit(1);
        }

        /* alow first session custom event packet negotiation */
        if (RTConfig.hasProperty(CFG_firstSessionNegotiation)) {
            // This section requires OpenDMTP v1.2.14.1+
            // If you receive a compile error here, simply comment the following lines
            if (RTConfig.getBoolean(CFG_firstSessionNegotiation,true)) {
                DMTPServer.setAllowFirstSessionNegotiation(true);
            } else {
                Print.logInfo("First session custom event packet negotiation DISABLED ...");
                DMTPServer.setAllowFirstSessionNegotiation(false);
            }
        }

        /* return UDP response/ack */
        if (RTConfig.hasProperty(CFG_udpReturnResponse)) {
            if (RTConfig.getBoolean(CFG_udpReturnResponse,true)) {
                Print.logInfo("UDP responses/acks will be returned to UDP packets ...");
                DMTPClientPacketHandler.setUdpReturnResponse(true);
            } else {
                Print.logInfo("No UDP responses will be returned ...");
                DMTPClientPacketHandler.setUdpReturnResponse(false);
            }
        }

        /* init DeviceDMImpl */
        DeviceDBImpl.configInit();
        DCServerConfig dcs = Main.getServerConfig();
        String dcsName = dcs.getName();

        /* TCP timeouts */
        long tcpIdleTimeout = dcs.getTcpIdleTimeoutMS(-1L);
        if (tcpIdleTimeout > 0L) { DMTPServer.setTcpIdleTimeout(tcpIdleTimeout); }
        long tcpPcktTimeout = dcs.getTcpPacketTimeoutMS(-1L);
        if (tcpPcktTimeout > 0L) { DMTPServer.setTcpPacketTimeout(tcpPcktTimeout); }
        long tcpSessTimeout = dcs.getTcpSessionTimeoutMS(-1L);
        if (tcpSessTimeout > 0L) { DMTPServer.setTcpSessionTimeout(tcpSessTimeout); }

        /* UDP timeouts */
        long udpIdleTimeout = dcs.getUdpIdleTimeoutMS(-1L);
        if (udpIdleTimeout > 0L) { DMTPServer.setUdpIdleTimeout(udpIdleTimeout); }
        long udpPcktTimeout = dcs.getUdpPacketTimeoutMS(-1L);
        if (udpPcktTimeout > 0L) { DMTPServer.setUdpPacketTimeout(udpPcktTimeout); }
        long udpSessTimeout = dcs.getUdpSessionTimeoutMS(-1L);
        if (udpSessTimeout > 0L) { DMTPServer.setUdpSessionTimeout(udpSessTimeout); }

        /* start server */
        if (RTConfig.getBoolean(ARG_START,false)) {

            /* start port listeners */
            try {
                DMTPServer.createTrackSocketHandler(listenPorts);
            } catch (Throwable t) { 
                // trap any server exception
                Print.logError("Error: " + t);
            }

            /* wait here forever while the server is running in a thread */
            while (true) { 
                try { Thread.sleep(60L * 60L * 1000L); } catch (Throwable t) {}
            }
            // control never reaches here

        }
        
        /* display usage */
        Main.usage("Please specify an option ...");
        
    }

}
