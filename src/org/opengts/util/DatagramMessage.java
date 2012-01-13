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
//  2007/11/28  Martin D. Flynn
//     -Initial release
//  2009/01/28  Martin D. Flynn
//     -Improved command-line interface
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
*** A class for sending and recieving datagram messages [CHECK]
**/

public class DatagramMessage
{

    // ------------------------------------------------------------------------

    protected DatagramSocket datagramSocket  = null;
    protected DatagramPacket sendPacket = null;
    protected DatagramPacket recvPacket = null;

    /**
    *** For subclassing only
    **/
    protected DatagramMessage()
    {
    }

    /**
    *** Constructor for receiving messages 
    *** @param port The port to use
    *** @throws IOException if a socket error occurs
    *** @throws UnknownHostException if the IP adress of the host could not be
    ***     determined
    **/
    public DatagramMessage(int port)
        throws IOException, UnknownHostException
    {
        this.datagramSocket = new DatagramSocket(port);
    }

    /**
    *** Constructor for sending messages 
    *** @param destHost The remote(destination) host address
    *** @param destPort The remote(destination) port to use
    *** @throws IOException if a socket error occurs
    *** @throws UnknownHostException if the IP adress of the host could not be
    ***     determined
    **/
    public DatagramMessage(String destHost, int destPort)
        throws IOException, UnknownHostException
    {
        this(InetAddress.getByName(destHost), destPort);
    }

    /**
    *** Constructor for sending messages 
    *** @param destHost The remote(destination) host address
    *** @param destPort The remote(destination) port to use
    *** @throws IOException if a socket error occurs
    **/
    public DatagramMessage(InetAddress destHost, int destPort)
        throws IOException
    {
        this.datagramSocket = new DatagramSocket();
        this.setRemoteHost(destHost, destPort);
    }

    /**
    *** Constructor for sending messages 
    *** @param destHost The remote(destination) host address
    *** @param destPort The remote(destination) port to use
    *** @param bindPort The local port to bind
    *** @throws IOException if a socket error occurs
    *** @throws UnknownHostException if the IP adress of the host could not be
    ***     determined
    **/
    public DatagramMessage(String destHost, int destPort, int bindPort)
        throws IOException, UnknownHostException
    {
        this(InetAddress.getByName(destHost), destPort, 
            bindPort, null);
    }

    /**
    *** Constructor for sending messages 
    *** @param destHost The remote(destination) host address
    *** @param destPort The remote(destination) port to use
    *** @param bindPort The local port to bind
    *** @param bindAddr The local address to bind
    *** @throws IOException if a socket error occurs
    *** @throws UnknownHostException if the IP adress of the host could not be
    ***     determined
    **/
    public DatagramMessage(String destHost, int destPort, int bindPort, String bindAddr)
        throws IOException, UnknownHostException
    {
        this(InetAddress.getByName(destHost), destPort, 
            bindPort, (!StringTools.isBlank(bindAddr)? InetAddress.getByName(bindAddr) : null));
    }

    /**
    *** Constructor for sending messages 
    *** @param destHost The remote(destination) host address
    *** @param destPort The remote(destination) port to use
    *** @param bindPort The local port to bind
    *** @throws IOException if a socket error occurs
    **/
    public DatagramMessage(InetAddress destHost, int destPort, int bindPort)
        throws IOException
    {
        this(destHost, destPort, 
            bindPort, null);
    }
    
    /**
    *** Constructor for sending messages 
    *** @param destHost The remote(destination) host address
    *** @param destPort The remote(destination) port to use
    *** @param bindPort The local port to bind
    *** @param bindAddr The local address to bind
    *** @throws IOException if a socket error occurs
    **/
    public DatagramMessage(InetAddress destHost, int destPort, int bindPort, InetAddress bindAddr)
        throws IOException
    {
        if (bindPort <= 0) {
            this.datagramSocket = new DatagramSocket();
        } else
        if (bindAddr == null) {
            this.datagramSocket = new DatagramSocket(bindPort);
        } else {
            this.datagramSocket = new DatagramSocket(bindPort, bindAddr);
        }
        this.setRemoteHost(destHost, destPort);
    }

    // ------------------------------------------------------------------------

    /**
    *** Closes the datagram socket
    **/
    public void close()
        throws IOException
    {
        this.datagramSocket.close();
    }

    // ------------------------------------------------------------------------

    /**
    *** Set the remote(destination) host 
    *** @param host The remote host address
    *** @param port The remote host port
    *** @throws IOException if an error occurs
    **/
    public void setRemoteHost(String host, int port)
        throws IOException
    {
        this.setRemoteHost(InetAddress.getByName(host), port);
    }

    /**
    *** Set the remote(destination) host 
    *** @param host The remote host address
    *** @param port The remote host port
    *** @throws IOException if an error occurs
    **/
    public void setRemoteHost(InetAddress host, int port)
        throws IOException
    {
        if (this.sendPacket != null) {
            this.sendPacket.setAddress(host);
            this.sendPacket.setPort(port);
        } else {
            this.sendPacket = new DatagramPacket(new byte[0], 0, host, port);
        }
    }
    
    /**
    *** Gets the datagram packet to be sent
    *** @return The datagram packet to be sent
    **/
    public DatagramPacket getSendPacket()
    {
        return this.sendPacket;
    }

    // ------------------------------------------------------------------------

    /**
    *** Send a String to the remote host
    *** @param msg The String to send to the remote host
    *** @throws IOException if the string is null or a socket error occurs
    **/
    public void send(String msg)
        throws IOException
    {
        this.send(StringTools.getBytes(msg));
    }

    /**
    *** Send an array of bytes to the remote host
    *** @param data The array of bytes to send to the remote host
    *** @throws IOException if the string is null or a socket error occurs
    **/
    public void send(byte data[])
        throws IOException
    {
        if (data != null) {
            this.send(data, data.length);
        } else {
            throw new IOException("Nothing to send");
        }
    }

    /**
    *** Send an array of bytes to the remote host
    *** @param data The array of bytes to send to the remote host
    *** @param len The length of the data
    *** @throws IOException if the string is null or a socket error occurs
    **/
    public void send(byte data[], int len)
        throws IOException
    {
        this.send(data, len, 1);
    }

    /**
    *** Send an array of bytes to the remote host 
    *** @param data The array of bytes to send to the remote host
    *** @param len The length of the data
    *** @param count The number of times to send the message
    *** @throws IOException if the string is null or a socket error occurs
    **/
    public void send(byte data[], int len, int count)
        throws IOException
    {
        if (this.sendPacket == null) {
            throw new IOException("'setRemoteHost' not specified");
        } else
        if ((data == null) || (len <= 0) || (count <= 0)) {
            throw new IOException("Nothing to send");
        } else {
            this.sendPacket.setData(data);
            this.sendPacket.setLength(len);
            for (; count > 0; count--) {
                this.datagramSocket.send(this.sendPacket);
            }
        }
    }

    // ------------------------------------------------------------------------

    private static final int DEFAULT_PACKET_SIZE = 1024;

    /**
    *** Receive an array of bytes 
    *** @param maxBuffSize The maximum buffer size
    *** @return The recieved packet as a byte array
    *** @throws IOException if a socket error occurs
    **/
    public byte[] receive(int maxBuffSize)
        throws IOException
    {

        /* receive data */
        byte dbuff[] = new byte[(maxBuffSize > 0)? maxBuffSize : DEFAULT_PACKET_SIZE];
        this.recvPacket = new DatagramPacket(dbuff, dbuff.length);
        this.datagramSocket.receive(this.recvPacket);
        byte newBuff[] = new byte[this.recvPacket.getLength()];
        System.arraycopy(this.recvPacket.getData(), 0, newBuff, 0, this.recvPacket.getLength());

        /* return received data */
        return newBuff;

    }

    /**
    *** Gets the DatagramPacket last recieved [CHECK]
    *** @return The DatagramPacket last recieved
    **/
    public DatagramPacket getReceivePacket()
    {
        return this.recvPacket;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Example receiver:
    //   bin/exeJava org.opengts.util.DatagramMessage -port=39000 -recv -echo 
    // Example transmitter:
    //   bin/exeJava org.opengts.util.DatagramMessage -host=localhost -port=39000 -send=hello -recv

    private static final String ARG_HOST[]      = new String[] { "host" , "h"       };
    private static final String ARG_PORT[]      = new String[] { "port" , "p"       };
    private static final String ARG_BINDADDR[]  = new String[] { "bindAddr"         };
    private static final String ARG_BINDPORT[]  = new String[] { "bindPort"         };
    private static final String ARG_SEND[]      = new String[] { "send"             };
    private static final String ARG_RECEIVE[]   = new String[] { "recv", "receive"  };
    private static final String ARG_ECHO[]      = new String[] { "echo",            };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + DatagramMessage.class.getName() + " {options}");
        Print.logInfo("'Send' Options:");
        Print.logInfo("  -bindAddr=<ip>    The local bind address");
        Print.logInfo("  -bindPort=<port>  The local bind port");
        Print.logInfo("  -host=<host>      The destination host");
        Print.logInfo("  -port=<port>      The destination port");
        Print.logInfo("  -send=<data>      The data to send (prefix with '0x' for hex data)");
        Print.logInfo("  -recv             Set to 'receive' mode after sending");
        Print.logInfo("'Receive' Options:");
        Print.logInfo("  -port=<port>      The port on which to listen for incoming data");
        Print.logInfo("  -recv             Set to 'receive' mode");
        Print.logInfo("  -echo             Echo received packet back to sender (implies '-recv')");
        System.exit(1);
    }

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String  host    = RTConfig.getString(ARG_HOST, null);
        int     port    = RTConfig.getInt(ARG_PORT, 0);
        boolean cmdEcho = RTConfig.hasProperty(ARG_ECHO);
        boolean cmdRecv = cmdEcho || RTConfig.hasProperty(ARG_RECEIVE);

        /* send data */
        if (RTConfig.hasProperty(ARG_SEND)) {
            if (StringTools.isBlank(host)) {
                Print.logError("Target host not specified");
                usage();
            }
            if (port <= 0) {
                Print.logError("Target port not specified");
                usage();
            }
            try {
                int    bindPort = RTConfig.getInt(ARG_BINDPORT, -1);
                String bindAddr = RTConfig.getString(ARG_BINDADDR, null);
                DatagramMessage dgm = new DatagramMessage(host, port, bindPort, bindAddr);
                String dataStr = RTConfig.getString(ARG_SEND,"Hello World");
                byte send[] = dataStr.startsWith("0x")? StringTools.parseHex(dataStr,null) : dataStr.getBytes();
                dgm.send(send);
                Print.logInfo("Datagram sent to %s:%d", host, port);
                if (!cmdRecv) {
                    // skip attempting to receive message
                } else
                if (bindPort <= 0) {
                    Print.logWarn("'-recv' requires '-bindPort', receive ignored.");
                } else {
                    Print.sysPrintln("Waiting for incoming data on port %d ...", bindPort);
                    byte recv[] = dgm.receive(1000); // timeout?
                    SocketAddress sa = dgm.getReceivePacket().getSocketAddress();
                    if (sa instanceof InetSocketAddress) {
                        int recvPort = dgm.getReceivePacket().getPort();
                        InetAddress hostAddr = ((InetSocketAddress)sa).getAddress();
                        Print.logInfo("Received from '" + hostAddr + ":" + recvPort + "' - 0x" + StringTools.toHexString(recv));
                    }
                }
                dgm.close();
                System.exit(0);
            } catch (Throwable th) {
                Print.logException("Error", th);
                System.exit(99);
            }
        }

        /* receive data */
        if (cmdRecv) {
            if (port <= 0) {
                Print.logError("Target port not specified");
                usage();
            }
            if (!StringTools.isBlank(host)) {
                Print.logWarn("Specified 'host' will be ignored");
            }
            try {
                DatagramMessage dgm = new DatagramMessage(port);
                Print.sysPrintln("Waiting for incoming data on port %d ...", port);
                byte recv[] = dgm.receive(1000);
                SocketAddress sa = dgm.getReceivePacket().getSocketAddress();
                if (sa instanceof InetSocketAddress) {
                    InetAddress hostAddr = ((InetSocketAddress)sa).getAddress();
                    int recvPort = dgm.getReceivePacket().getPort();
                    Print.logInfo("Received from host "+hostAddr+"["+recvPort+"]: 0x" + StringTools.toHexString(recv));
                    if (cmdEcho) {
                        try { Thread.sleep(500L); } catch (Throwable th) { /* ignore */ }
                        Print.sysPrintln("Echoing packet back to sender ...");
                        dgm.setRemoteHost(hostAddr, recvPort);
                        dgm.send(recv);
                    }
                }
                dgm.close();
                System.exit(0);
            } catch (Throwable th) {
                Print.logException("Error", th);
                System.exit(99);
            }
        }
        
        /* show usage */
        usage();
        
    }
    
}
