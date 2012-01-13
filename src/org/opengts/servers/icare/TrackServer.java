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
//  2009/04/02  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.servers.icare;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackServer
{

    // ------------------------------------------------------------------------
    // Start TrackServer (TrackServer is a singleton)

    private static TrackServer trackTcpInstance = null;

    /* start TrackServer on single port */
    public static TrackServer startTrackServer(int port)
        throws Throwable
    {
        return TrackServer.startTrackServer(new int[] { port });
    }
        
    /* start TrackServer on array of ports */
    public static TrackServer startTrackServer(int port[])
        throws Throwable
    {
        if (trackTcpInstance == null) {
            trackTcpInstance = new TrackServer(port);
        }
        return trackTcpInstance;
    }

    // ------------------------------------------------------------------------
    // TCP Session timeouts

    /* idle timeout */
    private static long tcpTimeout_idle     = Constants.TIMEOUT_TCP_IDLE;
    public static void setTcpIdleTimeout(long timeout)
    {
        TrackServer.tcpTimeout_idle = timeout;
    }
    public static long getTcpIdleTimeout()
    {
        return TrackServer.tcpTimeout_idle;
    }
    
    /* inter-packet timeout */
    private static long tcpTimeout_packet   = Constants.TIMEOUT_TCP_PACKET;
    public static void setTcpPacketTimeout(long timeout)
    {
        TrackServer.tcpTimeout_packet = timeout;
    }
    public static long getTcpPacketTimeout()
    {
        return TrackServer.tcpTimeout_packet;
    }

    /* total session timeout */
    private static long tcpTimeout_session  = Constants.TIMEOUT_TCP_SESSION;
    public static void setTcpSessionTimeout(long timeout)
    {
        TrackServer.tcpTimeout_session = timeout;
    }
    public static long getTcpSessionTimeout()
    {
        return TrackServer.tcpTimeout_session;
    }

    // ------------------------------------------------------------------------
    // UDP Session timeouts

    /* idle timeout */
    private static long udpTimeout_idle     = Constants.TIMEOUT_UDP_IDLE;
    public static void setUdpIdleTimeout(long timeout)
    {
        TrackServer.udpTimeout_idle = timeout;
    }
    public static long getUdpIdleTimeout()
    {
        return TrackServer.udpTimeout_idle;
    }

    /* inter-packet timeout */
    private static long udpTimeout_packet   = Constants.TIMEOUT_UDP_PACKET;
    public static void setUdpPacketTimeout(long timeout)
    {
        TrackServer.udpTimeout_packet = timeout;
    }
    public static long getUdpPacketTimeout()
    {
        return TrackServer.udpTimeout_packet;
    }

    /* total session timeout */
    private static long udpTimeout_session  = Constants.TIMEOUT_UDP_SESSION;
    public static void setUdpSessionTimeout(long timeout)
    {
        TrackServer.udpTimeout_session = timeout;
    }
    public static long getUdpSessionTimeout()
    {
        return TrackServer.udpTimeout_session;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // TCP port listener threads
    private java.util.List<ServerSocketThread> tcpThread = new Vector<ServerSocketThread>();

    // UDP port listener threads
    private java.util.List<ServerSocketThread> udpThread = new Vector<ServerSocketThread>();

    // ------------------------------------------------------------------------

    /* private constructor */
    private TrackServer(int port[])
        throws Throwable
    {
        if (!ListTools.isEmpty(port)) {
            for (int i = 0; i < port.length; i++) {
                this.startPortListeners(port[i]);
            }
        } else {
            throw new Exception("No ports specified");
        }
    }

    // ------------------------------------------------------------------------

    /* start TCP/UDP listeners on specified port */
    private void startPortListeners(int port)
        throws Throwable
    {
        if (DCServerFactory.isValidPort(port)) {
            this._startTCP(port);
            this._startUDP(port);
        } else {
            throw new Exception("Invalid port number: " + port);
        }
    }

    // ------------------------------------------------------------------------

    /* start TCP listener */
    private void _startTCP(int port)
        throws Throwable
    {
        ServerSocketThread sst = null;

        /* create server socket */
        try {
            sst = new ServerSocketThread(port);
        } catch (Throwable t) { // trap any server exception
            Print.logException("ServerSocket error", t);
            throw t;
        }
        
        /* initialize */
        sst.setTextPackets(Constants.ASCII_PACKETS);
        sst.setBackspaceChar(null); // no backspaces allowed
        sst.setLineTerminatorChar(Constants.ASCII_LINE_TERMINATOR);
        sst.setIgnoreChar(Constants.ASCII_IGNORE_CHARS);
        sst.setMaximumPacketLength(Constants.MAX_PACKET_LENGTH);
        sst.setMinimumPacketLength(Constants.MIN_PACKET_LENGTH);
        sst.setIdleTimeout(TrackServer.tcpTimeout_idle);         // time between packets
        sst.setPacketTimeout(TrackServer.tcpTimeout_packet);     // time from start of packet to packet completion
        sst.setSessionTimeout(TrackServer.tcpTimeout_session);   // time for entire session
        sst.setLingerTimeoutSec(Constants.LINGER_ON_CLOSE_SEC);
        sst.setTerminateOnTimeout(Constants.TERMINATE_ON_TIMEOUT);
        sst.setClientPacketHandlerClass(TrackClientPacketHandler.class);

        /* start thread */
        Print.logInfo("Starting TCP listener thread on port " + port + " [timeout=" + sst.getSessionTimeout() + "ms] ...");
        sst.start();
        this.tcpThread.add(sst);

    }

    // ------------------------------------------------------------------------

    /* start UDP listener */
    private void _startUDP(int port)
        throws Throwable
    {
        ServerSocketThread sst = null;

        /* create server socket */
        try {
            sst = new ServerSocketThread(ServerSocketThread.createDatagramSocket(port));
        } catch (Throwable t) { // trap any server exception
            Print.logException("ServerSocket error", t);
            throw t;
        }
        
        /* initialize */
        sst.setTextPackets(true);
        sst.setBackspaceChar(null); // no backspaces allowed
        sst.setLineTerminatorChar(new int[] { '\r' });
        sst.setMaximumPacketLength(Constants.MAX_PACKET_LENGTH);
        sst.setMinimumPacketLength(Constants.MIN_PACKET_LENGTH);
        sst.setIdleTimeout(TrackServer.udpTimeout_idle);
        sst.setPacketTimeout(TrackServer.udpTimeout_packet);
        sst.setSessionTimeout(TrackServer.udpTimeout_session);
        sst.setTerminateOnTimeout(Constants.TERMINATE_ON_TIMEOUT);
        sst.setClientPacketHandlerClass(TrackClientPacketHandler.class);

        /* start thread */
        Print.logInfo("Starting UDP listener thread on port " + port + " [timeout=" + sst.getSessionTimeout() + "ms] ...");
        sst.start();
        this.udpThread.add(sst);

    }
    
    // ------------------------------------------------------------------------
        
}
