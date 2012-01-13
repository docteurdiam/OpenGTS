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
//  Template for general server socket support
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/05/01  David Cowan
//     -Added support for gracefully shutting down the server
//  2007/07/13  Martin D. Flynn
//     -End Of Stream errors on UDP connections simply returns the bytes we're 
//      just read (previously it threw an exception, and ignored the received data).
//  2008/07/08  Martin D. Flynn
//     -Removed (commented) default socket timeout.
//  2008/08/07  Martin D. Flynn
//     -If client handler returns 'PACKET_LEN_ASCII_LINE_TERMINATOR', check to see if the
//      last read byte is already a line terminator.
//  2009/01/28  Martin D. Flynn
//     -Added UDP support to send 'getFinalPacket' to client.
//     -Fixed UDP 'getLocalPort'
//  2009/02/20  Martin D. Flynn
//     -Moved check for 'getMinimumPacketLength' into 'ServerSessionThread'.
//  2009/04/02  Martin D. Flynn
//     -Changed 'ServerSessionThread' to allow the 'ClientPacketHandler' to override
//      the minimum/maximum packet lengths.
//     -Added support for incremental packet lengths
//     -Added method 'isValidPort'
//  2009/05/01  Martin D. Flynn
//     -Returned UDP ACKs now use the same DatagramSocket which the server 'listens' on.
//  2009/05/24  Martin D. Flynn
//     -Added "getRemotePort()" to SessionInfo interface.
//  2009/07/01  Martin D. Flynn
//     -Client may now immediately terminate a session after 'sessionStarted'.
//  2009/08/07  Martin D. Flynn
//     -Added ability to set the local bound interface
//  2009/09/23  Martin D. Flynn
//     -Fixed: now counts bytes ('writeByteCount') when writing via UDP
//  2010/09/09  Martin D. Flynn
//     -Fixed EOS during TCP session when EOS should be end of packet.
//  2011/05/13  Martin D. Flynn
//     -Added option to include line terminator character in returned packets.
//      [see "includePacketLineTerminator()"]
//     -Increased "PACKET_LEN_INCREMENTAL_" mask to 20 bits
//  2011/07/01  Martin D. Flynn
//     -Changed to allow timeouts for "PACKET_LEN_END_OF_STREAM" termination.
//     -Fixed "PACKET_LEN_END_OF_STREAM" to attempt reading only whats left in the stream.
//  2011/08/21  Martin D. Flynn
//     -Renamed "PACKET_LEN_ASCII_LINE_TERMINATOR" to "PACKET_LEN_LINE_TERMINATOR"
//     -Check for ASCII packets before removing non-printable chars.
//     -Added support for incremental line-termination char packet length.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;
import javax.net.*;

import javax.net.ssl.SSLServerSocketFactory;

public class ServerSocketThread
    extends Thread
{

    // ------------------------------------------------------------------------
    // References:
    //   http://tvilda.stilius.net/java/java_ssl.php
    //   http://www.jguru.com/faq/view.jsp?EID=993651

    // ------------------------------------------------------------------------
    // SSL:
    //    keytool -genkey -keystore <mySrvKeystore> -keyalg RSA
    // Required Properties:
    //   -Djavax.net.ssl.keyStore=<mySrvKeystore>
    //   -Djavax.net.ssl.keyStorePassword=<123456>
    // For debug, also add:
    //   -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol 
    //   -Djavax.net.debug=ssl
    // ------------------------------------------------------------------------

    /* read additional byte length designated by mask and again call "getActualPacketLength" */
    public static final int         PACKET_LEN_INCREMENTAL_             = 0x01000000;
    public static final int         PACKET_LEN_INCREMENTAL_MASK         = 0x00FFFFFF;

    /* read to line terminator char and send to packet handler */
    public static final int         PACKET_LEN_LINE_TERMINATOR          = -1;

    /* read to end-of-stream and send to packet handler */
    public static final int         PACKET_LEN_END_OF_STREAM            = -2;

    /* read to matched pattern and send to packet handler */
    // NOT YET SUPPORTED!
    public static final int         PACKET_LEN_MATCH_PATTERN            = -3;

    /* read additional bytes up until line-terminator, and again call "getActualPacketLength" */
    public static final int         PACKET_LEN_INCREMENT_EOL            = PACKET_LEN_INCREMENTAL_ | PACKET_LEN_INCREMENTAL_MASK;

    // ------------------------------------------------------------------------

    public static final boolean     ACK_FROM_LISTEN_PORT                = true;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static      int         ListenBacklog                       = 50;
    private static      InetAddress LocalBindAddress                    = null;

    /**
    *** Sets the listen backlog for all created ServerSocket's
    *** @param backlog  The listen backlog
    **/
    public static void setListenBacklog(int backlog)
    {
        ListenBacklog = backlog;
    }

    /**
    *** Sets the local bind address for all created ServerSocket's
    *** @param bindAddr  The local bind address
    **/
    public static void setBindAddress(InetAddress bindAddr)
    {
        if ((bindAddr != null) && !ServerSocketThread.isLocalInterfaceAddress(bindAddr)) {
            Print.logWarn("BindAddress not found in NetworkInterface: " + bindAddr);
        }
        LocalBindAddress = bindAddr;
    }

    /**
    *** Returns true if a local bind address has been defined, otherwise false
    *** @return True if a local bind address has been defined, otherwise false
    **/
    public static boolean hasBindAddress()
    {
        return (LocalBindAddress != null);
    }

    /**
    *** Gets the local bind address for all created ServerSocket's, or null if no specific 
    *** bind address has been set
    *** @return The local bind address
    **/
    public static InetAddress getDefaultBindAddress()
    {
        return LocalBindAddress;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of all local network interface addresses (excluding loopback)
    *** @return An array of all local network interface addresses
    **/
    public static InetAddress[] getNetworkInterfaceAddresses()
        throws SocketException
    {
        java.util.List<InetAddress> ialist = new Vector<InetAddress>();
        for (Enumeration<NetworkInterface> ne = NetworkInterface.getNetworkInterfaces(); ne.hasMoreElements();) {
            NetworkInterface ni = ne.nextElement();
            if (!ni.isLoopback()) {
                //System.out.println("NetworkInterface: " + ni.getName());
                for (Enumeration<InetAddress> ie = ni.getInetAddresses(); ie.hasMoreElements();) {
                    InetAddress ia = ie.nextElement();
                    //System.out.println("  InetAddress : " + ia.getHostAddress());
                    ialist.add(ia);
                }
            }
        }
        return ialist.toArray(new InetAddress[ialist.size()]);
    }
    
    /**
    *** Returns true if the specified InetAddress is a local bound interface (including loopback)
    *** @return True if the specified InetAddress is a local bound interface
    **/
    public static boolean isLocalInterfaceAddress(InetAddress addr)
    {
        if (addr == null) {
            return false;
        } else
        if (addr.isLoopbackAddress()) {
            return true;
        } else {
            try {
                Set<InetAddress> ias = ListTools.toSet(ServerSocketThread.getNetworkInterfaceAddresses());
                return ias.contains(addr);
            } catch (Throwable th) {
                Print.logException("Getting NetworkInterface addresses", th);
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a DatagramSocket bound to the default local interface
    *** @return The created DatagramSocket
    **/
    public static DatagramSocket createDatagramSocket(InetAddress bindAddr, int port)
        throws SocketException
    {
        InetAddress bind = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        if (bind != null) {
            return new DatagramSocket(new InetSocketAddress(bind,port));
        } else {
            return new DatagramSocket(port);
        }
    }
    
    /**
    *** Creates a DatagramSocket bound to the default local interface
    *** @return The created DatagramSocket
    **/
    public static DatagramSocket createDatagramSocket(int port)
        throws SocketException
    {
        return ServerSocketThread.createDatagramSocket((InetAddress)null, port);
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a ServerSocket bound to the default local interface
    *** @return The created ServerSocket
    **/
    public static ServerSocket createServerSocket(InetAddress bindAddr, int port)
        throws IOException
    {
        InetAddress bind = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        return new ServerSocket(port, ListenBacklog, bind);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified port is valid
    *** @param port  The port to test
    *** @return True if the specified port is valid
    **/
    public static boolean isValidPort(int port)
    {
        return ((port > 0) && (port <= 65535));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int                                 listenPort              = 0;
    private int                                 clientPort              = -1;  // use for UDP response connections only
    
    private InetAddress                         bindAddress             = null;

    private DatagramSocket                      datagramSocket          = null;
    private ServerSocket                        serverSocket            = null;
    
    private java.util.List<ServerSessionThread> clientThreadPool        = null;
    private java.util.List<ClientPacketHandler> activeSessionList       = null;

    private ClientPacketHandler                 clientPacketHandler     = null;
    private Class                               clientPacketHandlerClass = null;

    private long                                sessionTimeoutMS        = -1L;
    private long                                idleTimeoutMS           = -1L;
    private long                                packetTimeoutMS         = -1L;
    
    private int                                 lingerTimeoutSec        = 4;    // SO_LINGER timeout is in *Seconds*

    private int                                 maxReadLength           = -1;   // safety net only
    private int                                 minReadLength           = -1;

    private boolean                             terminateOnTimeout      = true;
    
    private boolean                             isTextPackets           = true;
    private int                                 lineTerminatorChar[]    = new int[] { '\n' };
    private int                                 backspaceChar[]         = new int[] { '\b' };
    private int                                 ignoreChar[]            = new int[] { '\r' };
    private boolean                             includePacketLineTerm   = false;
    
    private byte                                packetTermPattern[]     = null;

    private byte                                prompt[]                = null;
    private int                                 promptIndex             = -1;
    private boolean                             autoPrompt              = false;
    
    private java.util.List<ActionListener>      actionListeners         = null;
    
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    private ServerSocketThread()
    {
        this.bindAddress       = ServerSocketThread.getDefaultBindAddress();
        this.clientThreadPool  = new Vector<ServerSessionThread>();
        this.activeSessionList = new Vector<ClientPacketHandler>();
        this.actionListeners   = new Vector<ActionListener>();
    }
    
    /**
    *** Constructor for UDP connections
    **/
    public ServerSocketThread(DatagramSocket ds) 
    {
        this();
        this.datagramSocket = ds;
        this.bindAddress    = (ds != null)? ds.getLocalAddress() : ServerSocketThread.getDefaultBindAddress();
        this.listenPort     = (ds != null)? ds.getLocalPort() : -1;
    }

    /**
    *** Constructor for TCP connections
    *** @param ss  The ServerSocket containing the 'listen' port information
    **/
    public ServerSocketThread(ServerSocket ss) 
    {
        this();
        this.serverSocket = ss;
        this.bindAddress  = (ss != null)? ss.getInetAddress() : ServerSocketThread.getDefaultBindAddress();
        this.listenPort   = (ss != null)? ss.getLocalPort() : -1;
    }

    /**
    *** Constructor for TCP connections
    *** @param port  The port on which to listen for incoming connections
    **/
    public ServerSocketThread(InetAddress bindAddr, int port)
        throws IOException 
    {
        this();
        this.bindAddress  = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        this.serverSocket = ServerSocketThread.createServerSocket(this.bindAddress, port);
        this.listenPort   = port;
    }

    /**
    *** Constructor for TCP connections
    *** @param port  The port on which to listen for incoming connections
    **/
    public ServerSocketThread(int port)
        throws IOException 
    {
        this((InetAddress)null, port);
    }

    /**
    *** Constructor for TCP connections
    *** @param port  The port on which to listen for incoming connections
    *** @param useSSL  True to enable an SSL
    **/
    public ServerSocketThread(InetAddress bindAddr, int port, boolean useSSL)
        throws IOException 
    {
        this();
        this.bindAddress  = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        this.serverSocket = useSSL?
            SSLServerSocketFactory.getDefault().createServerSocket(port, ListenBacklog, this.bindAddress) :
            ServerSocketFactory   .getDefault().createServerSocket(port, ListenBacklog, this.bindAddress);
        this.listenPort = port;
    }

    /**
    *** Constructor for TCP connections
    *** @param port  The port on which to listen for incoming connections
    *** @param useSSL  True to enable an SSL
    **/
    public ServerSocketThread(int port, boolean useSSL)
        throws IOException 
    {
        this((InetAddress)null, port, useSSL);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the bound UDP DatagramSocket for this server handler.  Will 
    *** return null if this server handler does not handle UDP connections.
    *** @return The DatagramSocket handle
    **/
    public DatagramSocket getDatagramSocket()
    {
        return this.datagramSocket;
    }

    /**
    *** Gets the bound TCP ServerSocket for this server handler.  Will 
    *** return null if this server handler does not handle TCP connections.
    *** @return The DatagramSocket handle
    **/
    public ServerSocket getServerSocket()
    {
        return this.serverSocket;
    }

    /**
    *** Gets the local port to which this socket is bound
    *** @return the local port to which this socket is bound
    **/
    public int getLocalPort()
    {
        return this.listenPort;
    }
    
    /**
    *** Gets the local bind address
    **/
    public InetAddress getBindAddress()
    {
        return this.bindAddress;
    }

    // ------------------------------------------------------------------------

    /**
    *** Run a test session from the specified input data array
    *** @param data  The test input data array
    **/
    public void testSession(byte data[])
    {
        if (data != null) {
            this.testSession(new ByteArrayInputStream(data));
        } else {
            Print.logError("'data' byte array is null");
        }
    }
    
    /**
    *** Run a test session from the specified input stream
    *** @param dataInput  The test input stream
    **/
    public void testSession(InputStream dataInput)
    {
        ClientSocket clientSocket = new ClientSocket(dataInput);
        ServerSessionThread sst = new ServerSessionThread(clientSocket, false);
        sst.run(); // run thread task here
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Listens for incoming connections and dispatches them to a handler thread
    **/
    public void run() 
    {
        while (true) {
            ClientSocket clientSocket = null;

            /* wait for client session */
            try {
                if (this.serverSocket != null) {
                    clientSocket = new ClientSocket(this.serverSocket.accept()); // block until connetion
                } else
                if (this.datagramSocket != null) {
                    byte b[] = new byte[ServerSocketThread.this.getMaximumPacketLength()];
                    DatagramPacket dp = new DatagramPacket(b, b.length);
                    this.datagramSocket.receive(dp); // block until connection
                    clientSocket = new ClientSocket(dp);
                } else {
                    Print.logStackTrace("ServerSocketThread has not been properly initialized");
                    break;
                }
            } catch (SocketException se) {
                // shutdown support
                if (this.serverSocket != null) {
                    int port = this.serverSocket.getLocalPort(); // should be same ad 'this.listenPort'
                    if (port <= 0) { port = this.getLocalPort(); }
                    String portStr = (port <= 0)? "?" : String.valueOf(port);
                    Print.logInfo("Shutdown TCP server on port " + portStr);
                } else
                if (this.datagramSocket != null) {
                    int port = this.datagramSocket.getLocalPort(); // should be same ad 'this.listenPort'
                    if (port <= 0) { port = this.getLocalPort(); }
                    String portStr = (port <= 0)? "?" : String.valueOf(port);
                    Print.logInfo("Shutdown UDP server on port " + portStr);
                } else {
                    Print.logInfo("Shutdown must have been called");
                }
            	break;
            } catch (IOException ioe) {
                Print.logError("Connection - " + ioe);
                continue; // go back and wait again
            }

            /* ip address : port */
            //String clientIPAddress;
            //try {
            //    InetAddress inetAddr = clientSocket.getInetAddress();
            //    clientIPAddress = (inetAddr != null)? inetAddr.getHostAddress() : "?";
            //} catch (Throwable t) {
            //    clientIPAddress = "?";
            //}
            //int clientRemotePort = clientSocket.getPort();

            /* find an available client thread */
            boolean foundThread = false;
            for (Iterator i = this.clientThreadPool.iterator(); i.hasNext() && !foundThread;) {
                ServerSessionThread sst = (ServerSessionThread)i.next();
                foundThread = sst.setClientIfAvailable(clientSocket);
            }
            if (!foundThread) { // add new thread to pool
                //Print.logInfo("New thread for ip ["+clientIPAddress+"] ...");
                ServerSessionThread sst = new ServerSessionThread(clientSocket);
                this.clientThreadPool.add(sst);
            } else {
                //Print.logDebug("Reuse existing thread for ip ["+clientIPAddress+"] ...");
            }

        }
    }
    
    /**
    *** Shuts down the server 
    **/
    public void shutdown() 
    {
    	try {

            /* shutdown TCP listener */
	    	if (this.serverSocket != null) {
	    		this.serverSocket.close();
	    	}

            /* shutdown UDP listener */
	    	if (this.datagramSocket != null) {
	    		this.datagramSocket.close();
	    	}
            
            /* loop through, and close all server threads */
	    	Iterator it = this.clientThreadPool.iterator();
	    	while (it.hasNext()) {
	    		ServerSessionThread sst = (ServerSessionThread)it.next();
	    		if (sst != null) {
	    			sst.close();
	    		}
	    	}

    	} catch (Exception e) {

    		Print.logError("Error shutting down ServerSocketThread " + e);

    	}
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the remote UDP response port
    *** @param remotePort The remote UDP respose port
    **/
    public void setRemotePort(int remotePort)
    {
        this.clientPort = remotePort;
    }

    /**
    *** Gets the remote UDP response port
    *** @return The remote UDP respose port
    **/
    public int getRemotePort()
    {
        return this.clientPort;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this object has action listeners
    *** @return True if this object has action listeners
    **/
    public boolean hasListeners()
    {
        return (this.actionListeners.size() > 0);
    }

    /**
    *** Adds an action listener
    *** @param al The action listener to add
    **/
    public void addActionListener(ActionListener al)
    {
        // used for simple one way messaging
        if (!this.actionListeners.contains(al)) {
            this.actionListeners.add(al);
        }
    }

    /**
    *** Removes an action listener
    *** @param al The action listener to remove
    **/
    public void removeActionListener(ActionListener al)
    {
        this.actionListeners.remove(al);
    }

    /**
    *** Invokes action listener with the specified message
    *** @param msgBytes The message to invoke the listeners with as a byte array
    *** @return True if succesful
    **/
    protected boolean invokeListeners(byte msgBytes[])
        throws Exception
    {
        if (msgBytes != null) {
            String msg = StringTools.toStringValue(msgBytes);
            for (Iterator i = this.actionListeners.iterator(); i.hasNext();) {
                Object alObj = i.next();
                if (alObj instanceof ActionListener) {
                    ActionListener al = (ActionListener)alObj;
                    ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, msg);
                    al.actionPerformed(ae);
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Sets the client packet handler [CHECK]
    *** @param cph The client packet handler
    **/
    public void setClientPacketHandler(ClientPacketHandler cph)
    {
        this.clientPacketHandler = cph;
    }
    
    /**
    *** Sets the client packet handler class [CHECK]
    *** @param cphc The client packet handler class
    **/
    public void setClientPacketHandlerClass(Class cphc)
    {
        if ((cphc == null) || ClientPacketHandler.class.isAssignableFrom(cphc)) {
            this.clientPacketHandlerClass = cphc;
            this.clientPacketHandler = null;
        } else {
            throw new ClassCastException("Invalid ClientPacketHandler class");
        }
    }

    /**
    *** Gets the current client packet handler
    *** @return The current client packet handler
    **/
    public ClientPacketHandler getClientPacketHandler()
    {
        if (this.clientPacketHandler != null) {
            // single instance
            return this.clientPacketHandler;
        } else
        if (this.clientPacketHandlerClass != null) {
            // new instance
            try {
                return (ClientPacketHandler)this.clientPacketHandlerClass.newInstance();
            } catch (Throwable t) {
                Print.logException("ClientPacketHandler", t);
                return null;
            }
        } else {
            // not defined
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the session timeout in milliseconds
    *** @param timeoutMS The session timeout in milliseconds
    **/
    public void setSessionTimeout(long timeoutMS)
    {
        this.sessionTimeoutMS = timeoutMS;
    }

    /**
    *** Gets the session timeout in milliseconds
    *** @return The session timeout in milliseconds
    **/
    public long getSessionTimeout()
    {
        return this.sessionTimeoutMS;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the idle timeout in milliseconds
    *** @param timeoutMS The idle timeout in milliseconds
    **/
    public void setIdleTimeout(long timeoutMS)
    {
        this.idleTimeoutMS = timeoutMS;
    }
    
    /**
    *** Gets the idle timeout in milliseconds
    *** @return The idle timeout in milliseconds
    **/
    public long getIdleTimeout()
    {
        // the timeout for waiting for something to appear on the socket
        return this.idleTimeoutMS;
    }

    /**
    *** Sets the packet timeout in milliseconds
    *** @param timeoutMS The packet timeout in milliseconds
    **/
    public void setPacketTimeout(long timeoutMS)
    {
        // once a byte is finally read, the timeout for waiting until the 
        // entire packet is finished
        this.packetTimeoutMS = timeoutMS;
    }
    
    /**
    *** Gets the packet timeout in milliseconds
    *** @return The packet timeout in milliseconds
    **/
    public long getPacketTimeout()
    {
        return this.packetTimeoutMS;
    }

    /**
    *** Sets if the thread should be terminated after a timeout [CHECK]
    *** @param timeoutQuit True if the thread should be terminated after a timeout
    **/
    public void setTerminateOnTimeout(boolean timeoutQuit)
    {
        this.terminateOnTimeout = timeoutQuit;
    }

    /**
    *** Gets if the thread should be terminated after a timeout [CHECK]
    *** @return True if the thread should be terminated after a timeout
    **/
    public boolean getTerminateOnTimeout()
    {
        return this.terminateOnTimeout;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the linger timeout in seconds
    *** @param timeoutSec The linger timeout in seconds
    **/
    public void setLingerTimeoutSec(int timeoutSec)
    {
        this.lingerTimeoutSec = timeoutSec;
    }

    /**
    *** Gets the linger timeout in seconds
    *** @return The linger timeout in seconds
    **/
    public int getLingerTimeoutSec()
    {
        return this.lingerTimeoutSec;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets if the packets to be sent are text
    *** @param isText True if the packets are text
    **/
    public void setTextPackets(boolean isText)
    {
        this.isTextPackets = isText;
        if (!this.isTextPackets()) {
            this.setBackspaceChar(null);
            this.setIgnoreChar(null);
            //this.setLineTerminatorChar(null);
        }
    }

    /**
    *** Returns true if the packets are text
    *** @return Ture if the packets are text
    **/
    public boolean isTextPackets()
    {
        return this.isTextPackets;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum packet length
    *** @param len  The maximum packet length
    **/
    public void setMaximumPacketLength(int len)
    {
        this.maxReadLength = len;
    }
    
    /**
    *** Gets the maximum packet length
    *** @return  The maximum packet length
    **/
    public int getMaximumPacketLength()
    {
        if (this.maxReadLength > 0) {
            return this.maxReadLength;
        } else
        if (this.isTextPackets()) {
            return 2048; // default for text packets
        } else {
            return 1024; // default for binary packets
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the minimum packet length
    *** @param len  The minimum packet length
    **/
    public void setMinimumPacketLength(int len)
    {
        this.minReadLength = len;
    }

    /**
    *** Gets the minimum packet length
    *** @return  The minimum packet length
    **/
    public int getMinimumPacketLength()
    {
        if (this.minReadLength > 0) {
            return this.minReadLength;
        } else
        if (this.isTextPackets()) {
            return 1; // at least '\r' (however, this isn't used for text packets)
        } else {
            return this.getMaximumPacketLength();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the line terminator character
    *** @param term The line terminator character
    **/
    public void setLineTerminatorChar(int term)
    {
        this.setLineTerminatorChar(new int[] { term });
    }

    /**
    *** Sets the line terminator characters
    *** @param term The line terminator characters
    **/
    public void setLineTerminatorChar(int term[])
    {
        this.lineTerminatorChar = term;
    }
    
    /**
    *** Gets the line terminator characters
    *** @return The line terminator characters
    **/
    public int[] getLineTerminatorChar()
    {
        return this.lineTerminatorChar;
    }
    
    /**
    *** Returns true if <code>ch</code> is a line terminator
    *** @return True if <code>ch</code> is a line terminator
    **/
    public boolean isLineTerminatorChar(int ch)
    {
        int termChar[] = this.getLineTerminatorChar();
        if ((termChar != null) && (ch >= 0)) {
            for (int i = 0; i < termChar.length; i++) {
                if (termChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets whether the line terminator character should be included in the returned packets
    *** @param rtnTerm  True to include the line terminator character in returned packets
    **/
    public void setIncludePacketLineTerminator(boolean rtnTerm)
    {
        this.includePacketLineTerm = rtnTerm;
    }
   
    /**
    *** Returns True if the line terminator character should be included in the returned packet
    *** @return True if the line terminator character should be included in the returned packet
    **/
    public boolean getIncludePacketLineTerminator()
    {
        return this.includePacketLineTerm;
    }

    /**
    *** Returns True if the line terminator character should be included in the returned packet
    *** @return True if the line terminator character should be included in the returned packet
    **/
    public boolean includePacketLineTerminator()
    {
        return this.includePacketLineTerm;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the packet terminator pattern
    *** @param pktTerm The packet terminator pattern
    **/
    public void setPacketTerminatorPattern(byte pktTerm[])
    {
        // "getActualPacketLength" is not called when this is set
        this.packetTermPattern = !ListTools.isEmpty(pktTerm)? pktTerm : null;
        this.setTextPackets(false);
    }

    /**
    *** Returns the packet terminator pattern
    *** @return The packet terminator pattern
    **/
    public byte[] getPacketTerminatorPattern()
    {
        return !ListTools.isEmpty(this.packetTermPattern)? this.packetTermPattern : null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the backspace character
    *** @param bs The backspace character
    **/
    public void setBackspaceChar(int bs)
    {
        this.setBackspaceChar(new int[] { bs });
    }

    /**
    *** Sets the backspace characters
    *** @param bs The backspace characters
    **/
    public void setBackspaceChar(int bs[])
    {
        this.backspaceChar = bs;
    }

    /**
    *** Gets the backspace characters
    *** @return The backspace characters
    **/
    public int[] getBackspaceChar()
    {
        return this.backspaceChar;
    }

    /**
    *** Returns true if <code>ch</code> is a backspace character
    *** @return True if <code>ch</code> is a backspace character
    **/
    public boolean isBackspaceChar(int ch)
    {
        if (this.hasPrompt() && (this.backspaceChar != null) && (ch >= 0)) {
            for (int i = 0; i < this.backspaceChar.length; i++) {
                if (this.backspaceChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the characters to ignore
    *** @param bs The characters to ignore
    **/
    public void setIgnoreChar(int bs[])
    {
        this.ignoreChar = bs;
    }

    /**
    *** Gets the characters to ignore
    *** @return The characters to ignore
    **/
    public int[] getIgnoreChar()
    {
        return this.ignoreChar;
    }
    
    /**
    *** Returns true if <code>ch</code> is a character to ignore
    *** @return True if <code>ch</code> is a character to ignore
    **/
    public boolean isIgnoreChar(int ch)
    {
        if ((this.ignoreChar != null) && (ch >= 0)) {
            for (int i = 0; i < this.ignoreChar.length; i++) {
                if (this.ignoreChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }
   
    // ------------------------------------------------------------------------
    
    /**
    *** If a default automatically generated prompt should be used [CHECK](all prompt related below)
    *** @param auto Ture if default automatic prompt should be used
    **/
    public void setAutoPrompt(boolean auto)
    {
        if (auto) {
            this.prompt = null;
            this.autoPrompt = true;
        } else {
            this.autoPrompt = false;
        }
    }
    
    /**
    *** Sets the prompt for TCP connections
    *** @param prompt The prompt
    **/
    public void setPrompt(byte prompt[])
    {
        this.prompt = prompt;
        this.autoPrompt = false;
    }
    
    /**
    *** Sets the prompt for TCP connections
    *** @param prompt The prompt
    **/
    public void setPrompt(String prompt)
    {
        this.setPrompt(StringTools.getBytes(prompt));
    }
    
    /**
    *** Gets the prompt for a specified index
    *** @param ndx The index (used for auto prompt)
    **/
    protected byte[] getPrompt(int ndx)
    {
        this.promptIndex = ndx;
        if (this.prompt != null) {
            return this.prompt;
        } else
        if (this.autoPrompt && this.isTextPackets()) {
            return StringTools.getBytes("" + (this.promptIndex+1) + "> ");
        } else {
            return null;
        }
    }
    
    /**
    *** If this server has a valid prompt
    *** @return True if this server has a valid prompt
    **/
    public boolean hasPrompt()
    {
        return (this.prompt != null) || (this.autoPrompt && this.isTextPackets());
    }

    /**
    *** Gets the current prompt index (used for auto prompt)
    *** @return The current prompt index
    **/
    protected int getPromptIndex()
    {
        return this.promptIndex;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Find the named TCP session and write the specified bytes TCP output stream
    *** @param sessionID  The session ID
    *** @param data       The bytes to write
    *** @return True if the bytes were written, false otherwise
    **/
    public boolean tcpWriteToSessionID(String sessionID, byte data[])
    {

        /* no SessionID specified? */
        if (StringTools.isBlank(sessionID)) {
            Print.logError("No TCP SessionID specified");
            return false;
        }

        /* no data to write? */
        if (ListTools.isEmpty(data)) {
            Print.logWarn("No data to write to TCP session: " + sessionID);
            return false;
        }

        /* find matching TCP SessionID and send packet */
        //Print.logInfo("Looking for SessionID: " + sessionID);
        boolean foundSID = false;
        boolean rtnOK    = false;
        synchronized (this.activeSessionList) {
            for (ClientPacketHandler cph : this.activeSessionList) {
                if (cph.equalsSessionID(sessionID)) {
                    foundSID = true;
                    rtnOK = cph.tcpWrite(data);
                    break;
                }
            }
        }
        if (!foundSID) {
            Print.logWarn("TCP SessionID not found: " + sessionID);
        }
        return rtnOK;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** ClientSocket
    **/
    private class ClientSocket
    {
        private Socket tcpClient = null;
        private DatagramPacket udpClient = null;
        private InputStream bais = null;
        public ClientSocket(Socket tcpClient) {
            this.tcpClient = tcpClient;
        }
        public ClientSocket(DatagramPacket udpClient) {
            this.udpClient = udpClient;
        }
        public ClientSocket(InputStream bais) { // debug/testing only
            this.udpClient = new DatagramPacket(new byte[1], 1);
            this.bais = bais;
        }
        public boolean isTCP() {
            if (this.tcpClient != null) {
                return true;
            } else {
                // test session?
                return false;
            }
        }
        public boolean isUDP() {
            if (this.udpClient != null) {
                return true;
            } else {
                // test session?
                return false;
            }
        }
        public int available() {
            try {
                return this.getInputStream().available();
            } catch (Throwable t) {
                return 0;
            }
        }
        public InetAddress getInetAddress() {
            // return client remote address
            if (this.tcpClient != null) {
                return this.tcpClient.getInetAddress();
            } else 
            if (this.udpClient != null) {
                try {
                    SocketAddress sa = this.udpClient.getSocketAddress();
                    if (sa instanceof InetSocketAddress) {
                        return ((InetSocketAddress)sa).getAddress();
                    } else {
                        return null;
                    }
                } catch (Throwable th) { // IllegalArgumentException
                    return null;
                }
            } else {
                return null;
            }
        }
        public int getPort() {
            // get client remote port
            if (this.tcpClient != null) {
                return this.tcpClient.getPort();
            } else 
            if (this.udpClient != null) {
                return this.udpClient.getPort();
            } else {
                return -1;
            }
        }
        public int getLocalPort() {
            return ServerSocketThread.this.getLocalPort();
            /*
            if (this.tcpClient != null) {
                return this.tcpClient.getLocalPort();
            } else 
            if (this.udpClient != null) {
                // This does not return the proper port
                SocketAddress sa = this.udpClient.getSocketAddress();
                if (sa instanceof InetSocketAddress) {
                    return ((InetSocketAddress)sa).getPort();
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
            */
        }
        public OutputStream getOutputStream() throws IOException {
            if (this.tcpClient != null) {
                // TCP
                return this.tcpClient.getOutputStream();
            } else {
                // UDP
                return null;
            }
        }
        public InputStream getInputStream() throws IOException {
            if (this.tcpClient != null) {
                return this.tcpClient.getInputStream();
            } else 
            if (this.udpClient != null) {
                if (this.bais == null) {
                    this.bais = new ByteArrayInputStream(this.udpClient.getData(), 0, this.udpClient.getLength());
                } 
                return this.bais;
            } else {
                return null;
            }
        }
        public void setSoTimeout(int timeoutSec) throws SocketException {
            if (this.tcpClient != null) {
                this.tcpClient.setSoTimeout(timeoutSec);
            }
        }
        public void setSoLinger(int timeoutSec) throws SocketException {
            if (this.tcpClient != null) {
                if (timeoutSec <= 0) {
                    this.tcpClient.setSoLinger(false, 0); // no linger
                } else {
                    this.tcpClient.setSoLinger(true, timeoutSec);
                }
            }
        }
        public void setSoLinger(boolean on, int timeoutSec) throws SocketException {
            if (this.tcpClient != null) {
                if (timeoutSec <= 0) { on = false; }
                this.tcpClient.setSoLinger(on, timeoutSec);
            }
        }
        public void close() throws IOException {
            if (this.tcpClient != null) {
                this.tcpClient.close();
            }
        }
    }
              
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** SessionInfo
    **/
    public interface SessionInfo
    {

        public int          getLocalPort();         // local bound port

        public boolean      isTCP();                // true if TCP session
        public boolean      isUDP();                // true if UDP session

        public int          getAvailableBytes();    // remaining available read bytes
        public InetAddress  getInetAddress();       // remote client IP address
        public int          getRemotePort();        // remote client port

        public boolean      tcpWrite(byte data[]);  // write bytes asynchronously to TCP output stream

        public long         getReadByteCount();     // how many bytes we've read so far
        public long         getWriteByteCount();    // how many bytes we've written so far

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** ServerSessionThread
    **/
    public class ServerSessionThread
        extends Thread
        implements SessionInfo
    {

        private Object runLock      = new Object();
        private Object tcpWriteLock = new Object(); // TCP write: synchronous/asynchronous

        private ClientSocket client = null;
        
        private long readByteCount  = 0L;
        private long writeByteCount = 0L;

        //public ServerSessionThread(Socket client) {
        //    super("ClientSession");
        //    this.client = new ClientSocket(client);
        //    this.start();
        //}

        /* create new ClientSocket handler thread */
        public ServerSessionThread(ClientSocket client) {
            this(client, true);
        }

        /* create new ClientSocket handler thread */
        public ServerSessionThread(ClientSocket client, boolean startThread) {
            super("ClientSession");
            this.client = client; // new ClientSocket thread
            if (startThread) {
                this.start();
            }
        }

        /* find an existing/unused ClientSocket handler thread */
        public boolean setClientIfAvailable(ClientSocket clientSocket) {
            boolean rtn = false;
            synchronized (this.runLock) {
                if (this.client != null) {
                    rtn = false; // not available
                } else {
                    this.client = clientSocket;
                    this.runLock.notify();
                    rtn = true;
                }
            }
            return rtn;
        }

        public int getLocalPort() {
            return ServerSocketThread.this.getLocalPort();
        }

        private int _getRemotePort(int overridePort) {
            return (overridePort > 0)? overridePort : this.getRemotePort();
        }

        public int getRemotePort() {
            int rPort = ServerSocketThread.this.getRemotePort(); // likely always '0'
            if ((rPort <= 0) && (this.client != null)) { 
                rPort = this.client.getPort(); // preferred port#
            }
            return rPort;
        }

        public boolean isTCP() {
            return (this.client != null)? this.client.isTCP() : false;
        }

        public boolean isUDP() {
            return (this.client != null)? this.client.isUDP() : false;
        }

        public int getMinimumPacketLength(ClientPacketHandler clientHandler) {
            if (clientHandler != null) {
                int len = clientHandler.getMinimumPacketLength();
                if (len > 0) {
                    return len;
                }
            }
            return ServerSocketThread.this.getMinimumPacketLength();
        }

        public int getMaximumPacketLength(ClientPacketHandler clientHandler) {
            if (clientHandler != null) {
                int len = clientHandler.getMaximumPacketLength();
                if (len > 0) {
                    return len;
                }
            }
            return ServerSocketThread.this.getMaximumPacketLength();
        }

        public InetAddress getInetAddress() {
            return (this.client != null)? this.client.getInetAddress() : null;
        }

        public int getAvailableBytes() {
            return (this.client != null)? this.client.available() : 0;
        }

        public long getReadByteCount() {
            return this.readByteCount;
        }

        public long getWriteByteCount() {
            return this.writeByteCount;
        }

        public void run() {

            /* loop forever */
            while (true) {

                /* wait for client (if necessary) */
                synchronized (this.runLock) {
                    while (this.client == null) {
                        try { this.runLock.wait(); } catch (InterruptedException ie) {}
                    }
                }
                // this ServerSessionThread is now active in a session

                /* reset byte counts */
                this.readByteCount  = 0L;
                this.writeByteCount = 0L;

                /* remote client IP address/port */
                InetAddress inetAddr = this.client.getInetAddress();
                int       remotePort = this.client.getPort();
                Print.logInfo("Remote client port: " + inetAddr + ":" + remotePort + "[" + this.client.getLocalPort() + "]");

                /* session timeout */
                long sessionStartTime = DateTime.getCurrentTimeMillis();
                long sessionTimeoutMS = ServerSocketThread.this.getSessionTimeout();
                long sessionTimeoutAt = (sessionTimeoutMS > 0L)? (sessionStartTime + sessionTimeoutMS) : -1L;

                /* client session handler */
                ClientPacketHandler clientHandler = ServerSocketThread.this.getClientPacketHandler();
                if (clientHandler != null) {
                    // set a handle to this session thread
                    clientHandler.setSessionInfo(this);
                    synchronized (ServerSocketThread.this.activeSessionList) {
                        ServerSocketThread.this.activeSessionList.add(clientHandler);
                    }
                    clientHandler.sessionStarted(inetAddr, this.client.isTCP(), ServerSocketThread.this.isTextPackets());
                }

                /* process client requests */
                Throwable termError = null;
                OutputStream output = null;
                try {

                    /* get output stream */
                    output = this.client.getOutputStream(); // null for UDP

                    /* check for client termination request */
                    if ((clientHandler == null) || !clientHandler.terminateSession()) {

                        /* write initial packet from server */
                        if (clientHandler != null) {
                            byte initialPacket[] = clientHandler.getInitialPacket(); // may be null
                            if ((initialPacket != null) && (initialPacket.length > 0)) {
                                if (this.client.isTCP()) {
                                    this._tcpWrite(output, initialPacket);   // TCP write: synchronous
                                } else {
                                    // ignore
                                }
                            }
                        }

                        /* loop until timeout, error, client terminate */
                        for (int i = 0;; i++) {

                            /* session timeout? */
                            if (sessionTimeoutAt > 0L) {
                                long currentTimeMS = DateTime.getCurrentTimeMillis();
                                if (currentTimeMS >= sessionTimeoutAt) {
                                    throw new SSSessionTimeoutException("Session timeout");
                                }
                            }

                            /* display prompt */
                            if (this.client.isTCP()) {
                                byte prompt[] = ServerSocketThread.this.getPrompt(i); // may be null
                                if ((prompt != null) && (prompt.length > 0)) {
                                    this._tcpWrite(output, prompt);   // TCP write: synchronous
                                }
                            }

                            /* read packet */
                            byte line[] = null;
                            if (ServerSocketThread.this.isTextPackets()) {
                                // ASCII: read until packet EOL
                                line = this.readLine(this.client, clientHandler);
                                // "getTerminateOnTimeout()" called on timeout (SSReadTimeoutException)
                            } else {
                                // Binary: read until packet length or timeout
                                line = this.readPacket(this.client, clientHandler);
                                // "getTerminateOnTimeout()" called on timeout (SSReadTimeoutException)
                            }
                            // timeout occurred?

                            /* check for requested terminate */
                            if (clientHandler.terminateSession()) {
                                break;
                            }

                            /* send packet to listeners */
                            if ((line != null) && ServerSocketThread.this.hasListeners()) {
                                try {
                                    ServerSocketThread.this.invokeListeners(line);
                                } catch (Throwable t) {
                                    // a listener can terminate this session
                                    break; 
                                }
                            }

                            /* handle packet, and get response */
                            if ((line != null) && (clientHandler != null)) {
                                try {
                                    byte response[] = clientHandler.getHandlePacket(line);
                                    if ((response != null) && (response.length > 0)) {
                                        if (this.client.isTCP()) {
                                            // TCP: Send response over socket connection
                                            Print.logDebug("TCP Response 0x%s", StringTools.toHexString(response));
                                            this._tcpWrite(output, response);   // TCP write: synchronous
                                        } else {
                                            // UDP: Send response via datagram ('ServerSocketThread.this.datagramSocket' is non-null)
                                            int rp = this._getRemotePort(clientHandler.getResponsePort());
                                            this.sendUDPResponse(inetAddr, rp, response);
                                        }
                                    } else {
                                        //Print.logInfo("No response requested");
                                    }
                                    if (clientHandler.terminateSession()) {
                                        break;
                                    }
                                } catch (Throwable t) {
                                    // the ClientPacketHandler can terminate this session
                                    Print.logException("Unexpected exception: ", t);
                                    break;
                                }
                            }

                            /* terminate now if we're reading a Datagram and we're out of data */
                            if (this.client.isUDP()) {
                                int avail = this.client.available();
                                if (avail <= 0) {
                                    // Normal end of UDP connection
                                    break; // break socket read loop
                                } else {
                                    // Still have more UDP packet data
                                    Print.logDebug("UDP: bytes remaining - %d", avail);
                                }
                            }
    
                        } // socket read loop
                        
                    }

                } catch (SSSessionTimeoutException ste) {
                    Print.logWarn(ste.getMessage());
                    termError = ste;
                } catch (SSReadTimeoutException rte) {
                    if (rte.getByteIndex() <= 0) {
                        // end of stream at packet boundry
                        Print.logInfo(rte.getMessage());
                    } else {
                        // end of stream within expected packet
                        Print.logWarn(rte.getMessage());
                        termError = rte;
                    }
                } catch (SSEndOfStreamException eos) {
                    if (this.client.isTCP()) { // run
                        if (eos.getByteIndex() <= 0) {
                            // end of stream at packet boundry
                            Print.logInfo(eos.getMessage());
                        } else {
                            // end of stream within expected packet
                            Print.logWarn(eos.getMessage());
                            termError = eos;
                        }
                    } else {
                        // We're at the end of the UDP datastream
                    }
                } catch (SocketException se) {
                    Print.logError("Connection closed");
                    termError = se;
                } catch (Throwable t) {
                    Print.logException("?", t);
                    termError = t;
                }
                Print.logInfo("End of session ...");

                /* client session terminated */
                if (clientHandler != null) {
                    try {
                        byte finalPacket[] = clientHandler.getFinalPacket(termError != null);
                        if ((finalPacket != null) && (finalPacket.length > 0)) {
                            if (this.client.isTCP()) {
                                // TCP: Send response over socket connection
                                this._tcpWrite(output, finalPacket);   // TCP write: synchronous
                            } else {
                                // UDP: Send response via datagram ('ServerSocketThread.this.datagramSocket' is non-null)
                                int rp = this._getRemotePort(clientHandler.getResponsePort());
                                this.sendUDPResponse(inetAddr, rp, finalPacket);
                            }
                        }
                    } catch (Throwable t) {
                        Print.logException("Final packet transmission", t);
                    }
                    clientHandler.sessionTerminated(termError, this.readByteCount, this.writeByteCount);
                    synchronized (ServerSocketThread.this.activeSessionList) {
                        ServerSocketThread.this.activeSessionList.remove(clientHandler);
                    }
                    // clear the session so that it doesn't hold on to an instance of this class
                    clientHandler.setSessionInfo(null);
                }

                /* flush output before closing */
                if (output != null) { // TCP
                    try {
                        output.flush();
                    } catch (IOException ioe) {
                        Print.logException("Flush", ioe);
                    } catch (Throwable t) {
                        Print.logException("?", t);
                    }
                }
                
                /* linger on close */
                try {
                    this.client.setSoLinger(ServerSocketThread.this.getLingerTimeoutSec()); // (seconds)
                } catch (SocketException se) {
                    Print.logException("setSoLinger", se);
                } catch (Throwable t) {
                    Print.logException("?", t);
                }

                /* close socket */
                try { 
                    this.client.close(); 
                } catch (IOException ioe) {
                    /* unable to close? */
                }
    
                /* clear for next requestor */
                synchronized (this.runLock) {
                    this.client = null;
                }

            } // while (true)

        } // run()
        
        // ----------------------------

        /*
        public void sendBytes(byte resp[]) throws IOException {
            // not currently used
            if (this.client == null) {
                // ignore
            } else
            if (this.client.isTCP()) {
                // TCP
                this._tcpWrite(this.client.getOutputStream(), resp);
            } else {
                // UDP: Send response via datagram 
                InetAddress inetAddr = this.client.getInetAddress();
                int rp = this.getRemotePort();
                this.sendUDPResponse(inetAddr, rp, resp);
            }
        }
        */

        private void sendUDPResponse(InetAddress clientAddr, int clientPort, byte pkt[]) throws IOException {
            // "ServerSocketThread.this.datagramSocket" is non-null for UDP sessions
            if ((pkt == null) || (pkt.length == 0)) {
                //Print.logInfo("No response requested");
            } else
            if (clientPort <= 0) {
                Print.logWarn("Unable to send final packet Datagram: unknown port");
            } else {
                // get datagram socket
                boolean closeSocket = false;
                DatagramSocket dgSocket = null;
                if (ACK_FROM_LISTEN_PORT) {
                    dgSocket = ServerSocketThread.this.datagramSocket; // preferred (non-null for UDP)
                    closeSocket = false;
                } else {
                    // WARN: may not be routed properly
                    dgSocket = ServerSocketThread.createDatagramSocket(0);
                    closeSocket = true;
                }
                // construct datagram packet
                DatagramPacket respPkt = new DatagramPacket(pkt, pkt.length, clientAddr, clientPort);
                // send
                int retry = 1;
                for (;retry > 0; retry--) {
                    Print.logDebug("UDP Response (from %d to %s:%d) 0x%s", dgSocket.getLocalPort(), clientAddr.toString(), clientPort, StringTools.toHexString(pkt));
                    dgSocket.send(respPkt);
                    this.writeByteCount += pkt.length;
                }
                // close
                if (closeSocket) {
                    dgSocket.close();
                }
            }
        }

        private boolean _tcpWrite(OutputStream output, byte data[]) throws IOException {
            // should only be called for TCP ('output' will be null for UDP)
            boolean rtn = false;
            if ((output != null) && (data != null) && (data.length > 0)) {
                synchronized (this.tcpWriteLock) { // locked to allow for asynchronous writing
                    try {
                        //String c = StringTools.toStringValue(cmd);
                        //Print.logDebug("<-- [" + c.length() + "] " + c);
                        output.write(data);
                        output.flush();
                        this.writeByteCount += data.length;
                        rtn = true;
                    } catch (IOException t) {
                        Print.logError("writeBytes error - " + t);
                        throw t;
                    }
                }
            }
            return rtn;
        }

        public boolean tcpWrite(byte data[]) {
            // this is intended to be called by a external thread
            boolean rtn = false;
            synchronized (this.runLock) {
                if ((this.client != null) && this.client.isTCP()) {
                    try {
                        OutputStream output = this.client.getOutputStream();
                        rtn = this._tcpWrite(output, data);
                    } catch (Throwable th) {
                        rtn = false;
                    }
                }
            }
            return rtn;
        }

        private int readByte(ClientSocket client, ClientPacketHandler clientHandler, long timeoutAt, int byteNdx) throws IOException {
            // Read until:
            //  - Timeout
            //  - IO error
            //  - Read byte
            int ch;
            InputStream input = client.getInputStream();
            while (true) {
                if (timeoutAt > 0L) {
                    long currentTimeMS = DateTime.getCurrentTimeMillis();
                    if (currentTimeMS >= timeoutAt) {
                        if (byteNdx <= 0) {
                            throw new SSReadTimeoutException("Read timeout [empty packet]", byteNdx);
                        } else {
                            throw new SSReadTimeoutException("Read timeout [@ " + byteNdx + "]", byteNdx);
                        }
                    }
                    //if (input.available() <= 0) {
                    int timeout = (int)(timeoutAt - currentTimeMS);
                    client.setSoTimeout(timeout);
                    //}
                }
                try {
                    // this read is expected to time-out if no data is available
                    ch = input.read();
                    if (ch < 0) {
                        // socket likely closed by client
                        if (byteNdx <= 0) {
                            throw new SSEndOfStreamException("End of stream [empty packet]", byteNdx);
                        } else {
                            throw new SSEndOfStreamException("End of stream [@ " + byteNdx + "]", byteNdx);
                        }
                    }
                    this.readByteCount++;
                    return ch; // <-- valid character returned
                } catch (InterruptedIOException ie) {
                    // timeout
                    continue;
                } catch (SocketException se) {
                    // rethrow IO error
                    throw se;
                } catch (IOException ioe) {
                    // rethrow IO error
                    throw ioe;
                }
            }
        }

        private byte[] readLine(ClientSocket client, ClientPacketHandler clientHandler) 
            throws IOException { // SSReadTimeoutException, SSEndOfStreamException, 
            // Read until:
            //  - EOL
            //  - Timeout
            //  - IO error
            //  - Read 'maxLen' characters

            /* timeouts */
            long idleTimeoutMS = ServerSocketThread.this.getIdleTimeout();
            long pcktTimeoutMS = ServerSocketThread.this.getPacketTimeout();
            long pcktTimeoutAt = (idleTimeoutMS > 0L)? (DateTime.getCurrentTimeMillis() + idleTimeoutMS) : -1L;

            /* max read length */
            int maxLen = this.getMaximumPacketLength(clientHandler); // safety net only
            // no minimum
            
            /* set default socket timeout */
            //client.setSoTimeout(10000);

            /* packet */
            byte buff[]  = new byte[maxLen];
            int  buffLen = 0;
            boolean isIdle = true;
            long readStartTime = DateTime.getCurrentTimeMillis();
            try {
                while (true) {

                    /* read byte */
                    int ch = this.readByte(client, clientHandler, pcktTimeoutAt, buffLen);
                    // valid character returned

                    /* reset idle timeout */
                    if (isIdle) {
                        isIdle = false;
                        if (pcktTimeoutMS > 0L) {
                            // reset timeout
                            pcktTimeoutAt = DateTime.getCurrentTimeMillis() + pcktTimeoutMS;
                        }
                    }

                    /* check special characters */
                    if (ServerSocketThread.this.isLineTerminatorChar(ch)) {
                        // end of line/packet
                        if (ServerSocketThread.this.includePacketLineTerminator()) {
                            if (buffLen >= buff.length) { // overflow?
                                byte newBuff[] = new byte[buff.length + 1];
                                System.arraycopy(buff, 0, newBuff, 0, buff.length);
                                buff = newBuff;
                            }
                            buff[buffLen++] = (byte)ch;
                        }
                        break;
                    } else
                    if (ServerSocketThread.this.isTextPackets()) {
                        if (ServerSocketThread.this.isIgnoreChar(ch)) {
                            // ignore this character (typically '\r')
                            continue;
                        } else
                        if (ServerSocketThread.this.isBackspaceChar(ch)) {
                            if (buffLen > 0) {
                                buffLen--;
                            }
                            continue;
                        } else
                        if (ch < ' ') {
                            // ignore non-printable characters
                            if (ch != '\t') { // keep tab chars
                                continue;
                            }
                        }
                    }

                    /* save byte */
                    if (buffLen >= buff.length) { // overflow?
                        byte newBuff[] = new byte[buff.length * 2];
                        System.arraycopy(buff, 0, newBuff, 0, buff.length);
                        buff = newBuff;
                    }
                    buff[buffLen++] = (byte)ch;

                    /* check lengths */
                    if ((maxLen > 0) && (buffLen >= maxLen)) {
                        // we've read all the bytes we can
                        break;
                    }

                }
            } catch (SSReadTimeoutException te) {
                // This could mean a protocol error
                if (buffLen > 0) {
                    Print.logWarn("Timeout: " + StringTools.toStringValue(buff, 0, buffLen));
                }
                if (ServerSocketThread.this.getTerminateOnTimeout()) {
                    throw te;
                }
           } catch (SSEndOfStreamException eos) {
                if (client.isTCP()) { // readLine
                    // This could mean a protocol error
                    if (buffLen > 0) {
                        Print.logWarn("EOS: (ASCII) " + StringTools.toStringValue(buff, 0, buffLen));
                    }
                    Print.logError(eos.getMessage());
                    throw eos;
                } else {
                    // We're at the end of the UDP datastream (may be an expected condition)
                    // (just fall through to return what bytes we've already read.)
                }
            } catch (IOException ioe) {
                Print.logError("ReadLine error - " + ioe);
                throw ioe;
            }
            long readEndTime = DateTime.getCurrentTimeMillis();

            /* return packet */
            if (buff.length == buffLen) {
                // highly unlikely
                return buff;
            } else {
                // resize buffer
                byte newBuff[] = new byte[buffLen];
                System.arraycopy(buff, 0, newBuff, 0, buffLen);
                return newBuff;
            }

        }

        private byte[] readPacket(ClientSocket client, ClientPacketHandler clientHandler) 
            throws IOException { // SSReadTimeoutException, SSEndOfStreamException, SocketException
            // Read until:
            //  - Timeout
            //  - IO error
            //  - Read 'maxLen' characters
            //  - Read 'actualLen' characters

            /* timeouts */
            long idleTimeoutMS = ServerSocketThread.this.getIdleTimeout();
            long pcktTimeoutMS = ServerSocketThread.this.getPacketTimeout();
            long pcktTimeoutAt = (idleTimeoutMS > 0L)? (DateTime.getCurrentTimeMillis() + idleTimeoutMS) : -1L;

            /* packet/read length */
            int maxLen = this.getMaximumPacketLength(clientHandler); // safety net only
            int minLen = this.getMinimumPacketLength(clientHandler); // tcp/udp dependent

            /* set default socket timeout */
            //client.setSoTimeout(10000);

            /* packet termination pattern */
            byte pktTerm[] = ServerSocketThread.this.getPacketTerminatorPattern();
            int  pktState  = 0;

            /* read packet */
            byte packet[] = new byte[maxLen];
            int  packetLen = 0;
            boolean isIdle = true;
            boolean breakOnLineTerm = false;
            boolean incrementOnLineTerm = false;
            boolean failOnEOS = client.isTCP();
            try {
                int actualLen = 0;
                while (true) {

                    /* read byte */
                    // hangs until byte read or timeout
                    int lastByte = this.readByte(client, clientHandler, pcktTimeoutAt, packetLen);
                    // valid byte returned 

                    /* reset idle timeout */
                    if (isIdle) {
                        isIdle = false;
                        if (pcktTimeoutMS > 0L) {
                            // reset packet timeout
                            pcktTimeoutAt = DateTime.getCurrentTimeMillis() + pcktTimeoutMS;
                        }
                    }

                    /* look for line terminator? */
                    if (breakOnLineTerm) {
                        if (ServerSocketThread.this.isLineTerminatorChar(lastByte)) {
                            // end of line (typically '\n')
                            if (ServerSocketThread.this.includePacketLineTerminator()) {
                                // TODO: check for overflow?
                                packet[packetLen++] = (byte)lastByte;
                            }
                            break;
                        } else
                        if (ServerSocketThread.this.isIgnoreChar(lastByte)) {
                            // ignore this character (typically '\r')
                            continue;
                        } else {
                            // save byte
                            // TODO: check for overflow?
                            packet[packetLen++] = (byte)lastByte;
                        }
                    } else {
                        // save byte
                        // TODO: check for overflow?
                        packet[packetLen++] = (byte)lastByte;
                    }

                    /* lready read maximum allowed bytes? */
                    if (packetLen >= maxLen) {
                        // we've read the maximum number of bytes allowed
                        // ignore any incremental state that may be in effect
                        break;
                    }

                    /* do we have a specified packet length? */
                    if (actualLen > 0) {
                        if (packetLen >= actualLen) {
                            // we've read the bytes we expected to read
                            break;
                        }
                        // continue reading until we achieve the requested length
                        continue;
                    }

                    // ---------------------------------------------------------
                    // at this point we do not yet have the actual packet length

                    /* check pattern matching */
                    // EXPERIMENTAL
                    if (pktTerm != null) {
                        // check packet termination pattern
                        if (pktTerm[pktState] == (byte)lastByte) {
                            pktState++;
                            if (pktState >= pktTerm.length) {
                                // we've matched the packet terminating pattern
                                break;
                            }
                        } else {
                            // back to initial state
                            // TODO: should do a proper state reset
                            // Consider the pattern "#!", and the input string "##!".
                            // In this case the 'pktState' should be set to 1 instead of 0.
                            pktState = 0;
                        }
                    }

                    /* scan for incremental line-terminator? */
                    if (incrementOnLineTerm && ServerSocketThread.this.isLineTerminatorChar(lastByte)) {
                        // INCREMENTAL: found EOL
                        incrementOnLineTerm = false;
                        minLen = packetLen; // reset minLen to what we've read so far
                    }

                    /* have we met the minimum-daily-requirements? */
                    if (packetLen < minLen) {
                        // continue reading
                        continue;
                    }

                    // ---------------------------------------------------------
                    // at this point we've read the minimum required packet length

                    /* no clientHandler? */
                    if (clientHandler == null) {
                        // continue reading
                        continue;
                    }

                    // ---------------------------------------------------------
                    // at this point the client handler determines packet length

                    /* get the actual/next expected packet length */
                    int     newPktLen  = clientHandler.getActualPacketLength(packet, packetLen);
                    boolean haveActual = ((newPktLen >= 0) && (newPktLen < PACKET_LEN_INCREMENTAL_MASK));
                    int     nextLen    = (newPktLen < 0)? newPktLen : (newPktLen & PACKET_LEN_INCREMENTAL_MASK);

                    /* has the client indicated that session should be terminated? */
                    if (clientHandler.terminateSession()) {
                        // done reading, do not continue
                        break;
                    }

                    /* actual packet length specified? */
                    if (haveActual) {
                        // (nextLen >= 0) guaranteed
                        if (nextLen == packetLen) {
                            // already have exactly what we need
                            actualLen = packetLen;
                            // done reading, we have a packet
                            break; 
                        } else
                        if (nextLen < packetLen) {
                            // ERROR: "getActualPacketLength" returned a value less than the current length
                            Print.logStackTrace("Actual length ["+nextLen+"] < Packet length ["+packetLen+"]");
                            actualLen = packetLen;
                            // done reading, we have a packet
                            break; 
                        } else
                        if (nextLen > maxLen) {
                            Print.logStackTrace("Actual length ["+nextLen+"] > Maximum length ["+maxLen+"]");
                            actualLen = maxLen;
                            // continue reading until 'maxLen'
                            continue;
                        } else {
                            actualLen = nextLen;
                            // continue reading until packet
                            continue;
                        }
                    }

                    /* check for special case packet termination */
                    if (nextLen == PACKET_LEN_LINE_TERMINATOR) { // "-1"
                        // look for line terminator character
                        //Print.logInfo("Last Byte Read: %s [%s]", StringTools.toHexString(lastByte,8), StringTools.toHexString(packet[packetLen-1]));
                        if (ServerSocketThread.this.isLineTerminatorChar(lastByte)) {
                            // last byte was already a line terminator
                            if (!ServerSocketThread.this.includePacketLineTerminator()) {
                                packetLen--; // remove terminator
                            }
                            actualLen = packetLen;
                            // done reading, we have a packet
                            break;
                        } else {
                            breakOnLineTerm = true;
                            actualLen = maxLen; // continue until line-term
                            // continue reading until EOL
                            continue;
                        }
                    } else
                    if (nextLen == PACKET_LEN_END_OF_STREAM) { // "-2"
                        // read the rest of the stream
                        int avail = client.available();
                        //Print.logDebug("Reading remaining stream bytes: " + avail);
                        actualLen = packetLen + avail; // what we've already read, plus any remaining
                        if (actualLen > maxLen) {
                            // more available bytes than the allowed maximum
                            actualLen = maxLen;
                        }
                        failOnEOS = false;
                        // continue reading until 'maxLen' or EOS
                        continue;
                    } else
                    if (nextLen < PACKET_LEN_END_OF_STREAM) { // <= "-3"
                        // DEFAULT TO END-OF-STREAM
                        // read the rest of the stream
                        int avail = client.available();
                        //Print.logDebug("Reading remaining stream bytes: " + avail);
                        actualLen = packetLen + avail; // what we've already read, plus any remaining
                        if (actualLen > maxLen) {
                            // more available bytes than the allowed maximum
                            actualLen = maxLen;
                        }
                        failOnEOS = false;
                        // continue reading until 'maxLen' or EOS
                        continue;
                    }

                    /* INCREMENTAL read */
                    if (nextLen == PACKET_LEN_INCREMENTAL_MASK) {
                        // should scan for EOL char
                        incrementOnLineTerm = true;
                        minLen = maxLen;
                        // continue reading until next EOL char
                        continue;
                    } else
                    if (nextLen > maxLen) {
                        // specified incremental length is greater that the maximum
                        Print.logStackTrace("Incremental length ["+nextLen+"] > Maximum length ["+maxLen+"]");
                        minLen = maxLen;
                        // continue reading until 'maxLen'
                        continue;
                    } else {
                        // reset minimum to next length (at least one more byte)
                        minLen = (nextLen > packetLen)? nextLen : (packetLen + 1);
                        // continue reading until next 'minLen'
                        continue;
                    }

                } // while (true)
            } catch (SSReadTimeoutException rte) {
                if (failOnEOS) { // PACKET_LEN_END_OF_STREAM
                    // This could mean a protocol error
                    if (packetLen > 0) {
                        Print.logWarn("Timeout: 0x" + StringTools.toHexString(packet, 0, packetLen));
                    }
                    if (ServerSocketThread.this.getTerminateOnTimeout()) {
                        throw rte;
                    }
                } else {
                    // We've received a Timeout during a TCP session and the Timeout was expected
                    // (just fall through to return what bytes we've already read.)
                }
            } catch (SSEndOfStreamException eos) {
                if (failOnEOS) { // PACKET_LEN_END_OF_STREAM
                    // This could mean a protocol error
                    if (packetLen > 0) {
                        Print.logWarn("EOS: 0x" + StringTools.toHexString(packet, 0, packetLen));
                    }
                    Print.logError(eos.getMessage());
                    throw eos;
                } else {
                    // We've received a EOS during a TCP session and the EOS was expected, or
                    // We're at the end of the UDP datastream (may be an expected condition)
                    // (just fall through to return what bytes we've already read.)
                }
            } catch (SocketException se) {
                Print.logError("ReadPacket error - " + se);
                throw se;
            } catch (IOException ioe) {
                Print.logError("ReadPacket error - " + ioe);
                throw ioe;
            }

            /* return packet */
            if (packet.length == packetLen) {
                // highly unlikely
                return packet;
            } else {
                // resize buffer
                byte newPacket[] = new byte[packetLen];
                System.arraycopy(packet, 0, newPacket, 0, packetLen);
                return newPacket;
            }

        }
        
        public void close() throws IOException {
            IOException rethrowIOE = null;
            synchronized (this.runLock) {
                if (this.client != null) {
                    try { 
                        this.client.close(); 
                    } catch (IOException ioe) {
                        /* unable to close? */
                        rethrowIOE = ioe;
                    }
                    this.client = null;
                }
            }
            if (rethrowIOE != null) {
                throw rethrowIOE;
            }
        }

    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** SSSessionTimeoutException
    **/
    public static class SSSessionTimeoutException
        extends IOException
    {
        public SSSessionTimeoutException(String msg) {
            super(msg);
        }
    }

    /**
    *** SSReadTimeoutException
    **/
    public static class SSReadTimeoutException
        extends IOException
    {
        private int byteIndex = 0;
        private byte dataPacket[]  = null;
        private int  dataPacketLen = 0;
        public SSReadTimeoutException(String msg, int byteNdx) {
            super(msg);
            this.byteIndex = byteNdx;
        }
        public int getByteIndex() {
            return this.byteIndex;
        }
        public void setDataPacket(byte data[], int len) {
            this.dataPacket    = data;
            this.dataPacketLen = len;
        }
    }
    
    /**
    *** SSEndOfStreamException
    **/
    public static class SSEndOfStreamException
        extends IOException
    {
        private int byteIndex = 0;
        public SSEndOfStreamException(String msg, int byteNdx) {
            super(msg);
            this.byteIndex = byteNdx;
        }
        public int getByteIndex() {
            return this.byteIndex;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sends a datagram to the specified host:port
    *** @param host  The destination host
    *** @param port  The destination port
    *** @param data  The data to send
    *** @throws IOException  if an IO error occurs
    **/
    public static void sendDatagram(InetAddress host, int port, byte data[])
        throws IOException
    {
        if (host == null) {
            throw new IOException("Invalid destination host");
        } else
        if (data == null) {
            throw new IOException("Data buffer is null");
        } else {
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, host, port);
            DatagramSocket datagramSocket = ServerSocketThread.createDatagramSocket(0);
            datagramSocket.send(sendPacket);
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
