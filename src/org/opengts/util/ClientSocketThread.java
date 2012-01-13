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
//  2006/02/19  Martin D. Flynn
//     - Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;

import javax.net.ssl.SSLSocketFactory;

import org.opengts.util.*;

/**
*** Threaded messaging socket client
**/

public class ClientSocketThread
    extends Thread
{

    // ------------------------------------------------------------------------
    // References:
    //   http://tvilda.stilius.net/java/java_ssl.php

    // ------------------------------------------------------------------------
    // SSL:
    //    keytool -genkey -keystore mySrvKeystore -keyalg RSA
    // Required Properties:
    //   -Djavax.net.ssl.trustStore=<mySrvKeystore>
    //   -Djavax.net.ssl.trustStorePassword=<123456>
    // For debug, also add:
    //   -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol 
    //   -Djavax.net.debug=ssl
    // ------------------------------------------------------------------------

    private static final int THREAD_STOPPED  = -1;
    private static final int THREAD_CHANGING = 0;
    private static final int THREAD_RUNNING  = 1;

    // ------------------------------------------------------------------------

    private boolean                 useSSL          = false;
    private String                  host            = null;
    private int                     port            = 0;
    private long                    readTimeout     = 5000L; // msec
    private Socket                  socket          = null;
    private InputThread             inputThread     = null;
    private OutputThread            outputThread    = null;
    private Object                  ioThreadLock    = null;
    private Vector<ActionListener>  actionListeners = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param host The host name to connect to
    *** @param port The port number to use
    **/
    public ClientSocketThread(String host, int port) 
    {
        this(host, port, false);
    }
    
    /**
    *** Constructor
    *** @param host The host name to connect to
    *** @param port The port number to use
    *** @param useSSL True if SSL is to be used
    **/
    public ClientSocketThread(String host, int port, boolean useSSL) 
    {
        this.host   = (host != null)? host : "localhost";
        this.port   = port;
        this.useSSL = useSSL;
        this.ioThreadLock = new Object();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the read timeout in milliseconds
    *** @return The read timeout in milliseconds
    **/
    public long getReadTimeout()
    {
        return this.readTimeout;
    }
    
    /**
    *** Sets the read timeout in milliseconds
    *** @param ms The read timeout in milliseconds
    **/
    public void setReadTimeout(long ms)
    {
        this.readTimeout = ms;
    }

    /**
    *** Sets the read timeout on the currently open socket to the previous specified value
    **/
    public void setSocketReadTimeout()
        throws SocketException
    {
        if ((this.socket != null) && (this.readTimeout > 0L)) {
            this.socket.setSoTimeout((int)this.readTimeout);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Opens the socket connection
    *** @throws IOException if an error occurs while creating the socket
    **/
    public void openSocket()
        throws IOException
    {
        this.openSocket(-1L);
    }
    
    /**
    *** Opens the socket connection
    *** @param timeoutMS  open timeout in milliseconds
    *** @throws IOException if an error occurs while creating the socket
    **/
    public void openSocket(long timeoutMS)
        throws IOException
    {
        //Print.logInfo("Openning socket - " + this.host + ":" + this.port);
        if (this.useSSL) {
            if (timeoutMS > 0L) {
                this.socket = SSLSocketFactory.getDefault().createSocket(this.host, this.port);
                //this.socket = SSLSocketFactory.getDefault().createSocket();
                //this.socket.connect(new InetSocketAddress(this.host, this.port), timeoutMS);
            } else {
                this.socket = SSLSocketFactory.getDefault().createSocket(this.host, this.port);
            }
        } else {
            if (timeoutMS > 0) {
                this.socket = new Socket(); 
                this.socket.connect(new InetSocketAddress(this.host, this.port), (int)timeoutMS);
            } else {
                // new Socket(InetAddress.getByName(host),port)
                this.socket = new Socket(this.host, this.port); 
            }
        }
    }
    
    /**
    *** Closes the socket connection
    **/
    public void closeSocket()
    {
        if (this.socket != null) {
            try { this.socket.close(); } catch (Throwable t) {/*ignore*/}
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Writes the specified byte array to the socket output stream
    *** @param b The byte array to write to the socket output stream
    *** @throws IOException if an error occurs
    **/
    public void socketWriteBytes(byte b[])
        throws IOException
    {
        ClientSocketThread.socketWriteBytes(this.socket, b, 0, -1);
    }

    /**
    *** Writes <code>length</code> bytes from the specified byte array 
    *** starting at <code>offset</code> to the socket output stream.
    *** @param b The byte array to write to the socket output stream
    *** @param offset The start offset in the data to begin writing at
    *** @param length The length of the data. Normally <code>b.length</code>
    *** @throws IOException if an error occurs
    **/
    public void socketWriteBytes(byte b[], int offset, int length)
        throws IOException
    {
        ClientSocketThread.socketWriteBytes(this.socket, b, offset, length);
    }

    /**
    *** Writes <code>length</code> bytes from the specified byte array 
    *** starting at <code>offset</code> to the specified socket's output stream.
    *** @param socket The socket which's output stream to write to
    *** @param b The byte array to write to the socket output stream
    *** @param offset The start offset in the data to begin writing at
    *** @param length The length of the data. Normally <code>b.length</code>
    *** @throws IOException if an error occurs
    **/
    protected static void socketWriteBytes(Socket socket, byte b[], int offset, int length)
        throws IOException
    {
        if ((socket != null) && (b != null)) {
            int bofs = offset;
            int blen = (length >= 0)? length : b.length;
            OutputStream output = socket.getOutputStream();
            output.write(b, bofs, blen);
            output.flush();
        }
    }

    /**
    *** Writes the specified byte array to the specified socket's output stream
    *** @param socket The socket which's output stream to write to
    *** @param b The byte array to write to the socket output stream
    *** @throws IOException if an error occurs
    **/
    protected static void socketWriteBytes(Socket socket, byte b[])
        throws IOException
    {
        if ((socket != null) && (b != null)) {
            OutputStream output = socket.getOutputStream();
            output.write(b);
            output.flush();
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Writes the specified StringBuffer to the socket output stream
    *** @param val The StringBuffer to write to the socket output stream. 
    *** @throws IOException if an error occurs
    **/
    public void socketWriteString(StringBuffer val)
        throws IOException
    {
        ClientSocketThread.socketWriteString(this.socket, val);
    }

    /**
    *** Writes the specified StringBuffer to the specified socket's output stream
    *** @param socket The socket which's output stream to write to
    *** @param val The StringBuffer to write to the socket output stream. 
    *** @throws IOException if an error occurs
    **/
    protected static void socketWriteString(Socket socket, StringBuffer val)
        throws IOException
    {
        if (val != null) {
            ClientSocketThread.socketWriteBytes(socket, StringTools.getBytes(val), 0, -1);
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Writes the specified String to the socket output stream
    *** @param val The String to write to the socket output stream. 
    *** @throws IOException if an error occurs
    **/
    public void socketWriteString(String val)
        throws IOException
    {
        ClientSocketThread.socketWriteString(this.socket, val);
    }

    /**
    *** Writes the specified String to the specified socket's output stream
    *** @param socket The socket which's output stream to write to
    *** @param val The String to write to the socket output stream. 
    *** @throws IOException if an error occurs
    **/
    protected static void socketWriteString(Socket socket, String val)
        throws IOException
    {
        if (val != null) {
            ClientSocketThread.socketWriteBytes(socket, StringTools.getBytes(val), 0, -1);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes the specified line to the socket output stream
    *** @param val The line to write to the socket output stream. A newline is
    ***   appended if it does not already end with one.
    *** @throws IOException if an error occurs
    **/
    public void socketWriteLine(String val)
        throws IOException
    {
        ClientSocketThread.socketWriteLine(this.socket, val);
    }

    /**
    *** Writes the specified line to the specified socket's output stream
    *** @param socket The socket which's output stream to write to
    *** @param val The line to write to the socket output stream. A newline is
    ***   appended if it does not already end with one.
    *** @throws IOException if an error occurs
    **/
    protected static void socketWriteLine(Socket socket, String val)
        throws IOException
    {
        if (val != null) {
            String v = val.endsWith("\n")? val : (val + "\n");
            ClientSocketThread.socketWriteBytes(socket, StringTools.getBytes(v), 0, -1);
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Reads the specified number of bytes from the socket
    *** @param length The number of bytes to read from the socket
    *** @throws IOException if an error occured or the server has stopped
    **/
    public byte[] socketReadBytes(int length)
        throws IOException
    {
        return this.socketReadBytes(this.socket, length);
    }

    /**
    *** Reads the specified number of bytes from the specifed socket
    *** @param socket    The socket from which bytes are read
    *** @param length The number of bytes to read from the socket
    *** @throws IOException if an error occured or the server has stopped
    **/
    protected static byte[] socketReadBytes(Socket socket, int length)
        throws IOException
    {
        if (socket == null) {
            return null;
        } else
        if (length <= 0) {
            return new byte[0];
        } else {
            int dataLen = 0;
            byte data[] = new byte[length];
            InputStream input = socket.getInputStream();
            while (dataLen < length) {
                int ch = input.read();
                if (ch < 0) {
                    // this means that the server has stopped
                    throw new IOException("End of input");
                } else {
                    data[dataLen] = (byte)ch;
                    dataLen++;
                }
            }
            return data;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Reads the bytes from the specifed socket until an eod-of-stream error occurs, or
    *** until the maximum number of bytes has bee read.
    *** @param baos      The ByteArrayOutputStream to which the bytes are written
    *** @param maxLength The number of bytes to read from the socket
    *** @return The number of bytes read if no exception has occurred
    *** @throws IOException if an error occured or the server has stopped.  ByteArrayOutputStream
    ***     will still contain all bytes read from the stream up until it failed.
    **/
    public int socketReadBytes(ByteArrayOutputStream baos, int maxLength)
        throws IOException
    {
        return this.socketReadBytes(this.socket, baos, maxLength);
    }

    /**
    *** Reads the bytes from the specifed socket until an eod-of-stream error occurs, or
    *** until the maximum number of bytes has bee read.
    *** @param socket    The socket from which bytes are read
    *** @param baos      The ByteArrayOutputStream to which the bytes are written
    *** @param maxLength The number of bytes to read from the socket
    *** @return The number of bytes read if no exception has occurred
    *** @throws IOException if an error occured or the server has stopped
    **/
    protected static int socketReadBytes(Socket socket, ByteArrayOutputStream baos, int maxLength)
        throws IOException
    {
        if (socket == null) {
            return 0;
        } else
        if (maxLength == 0) {
            return 0;
        } else {
            int dataLen = 0;
            InputStream input = socket.getInputStream();
            while ((maxLength < 0) || (dataLen < maxLength)) {
                int ch = input.read();
                if (ch < 0) {
                    // we've reached the end of input
                    return dataLen;
                } else {
                    if (baos != null) {
                        baos.write(ch);
                    }
                    dataLen++;
                }
            }
            return dataLen;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Reads a line from the socket input stream
    *** @throws IOException if an error occurs or the server has stopped
    **/
    public String socketReadLine()
        throws IOException
    {
        return this.socketReadLine(-1);
    }

    /** 
    *** Reads a line from the socket input stream
    *** @param maxLen The maximum length of the line to read
    *** @throws IOException if an error occurs or the server has stopped
    **/
    public String socketReadLine(int maxLen)
        throws IOException
    {
        return ClientSocketThread.socketReadLine(this.socket, maxLen, null);
    }

    /** 
    *** Reads a line from the socket input strea
    *** @param sb The string buffer to use
    *** @throws IOException if an error occurs or the server has stopped
    **/
    public String socketReadLine(StringBuffer sb)
        throws IOException
    {
        return this.socketReadLine(-1, sb);
    }

    /**
    *** Reads a line from the socket input stream
    *** @param maxLen The maximum length of of the line to read
    *** @param sb The string buffer to use
    *** @throws IOException if an error occurs or the server has stopped
    **/
    public String socketReadLine(int maxLen, StringBuffer sb)
        throws IOException
    {
        return ClientSocketThread.socketReadLine(this.socket, maxLen, sb);
    }

    /**
    *** Reads a line from the specified socket's input stream
    *** @param socket The socket to read a line from
    *** @param maxLen The maximum length of of the line to read
    *** @param sb The string buffer to use
    *** @throws IOException if an error occurs or the server has stopped
    **/
    protected static String socketReadLine(Socket socket, int maxLen, StringBuffer sb)
        throws IOException
    {
        if (socket != null) {
            int dataLen = 0;
            StringBuffer data = (sb != null)? sb : new StringBuffer();
            InputStream input = socket.getInputStream();
            while ((maxLen < 0) || (maxLen > dataLen)) {
                int ch = input.read();
                if (ch < 0) {
                    // this means that the server has stopped
                    throw new IOException("End of input");
                } else
                if (ch == '\n') {
                    break;
                } else {
                    data.append((char)ch);
                    dataLen++;
                }
            }
            return data.toString();
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // Main client thread loop
    
    /**
    *** Main client thread loop
    **/
    public void run() 
    {
        this.setRunStatus(THREAD_RUNNING);
        this.threadStarted();
        try {
            this.openSocket();
            this.inputThread = new InputThread(this.socket, this.readTimeout, this.ioThreadLock);
            this.outputThread = new OutputThread(this.socket, this.ioThreadLock);
            this.inputThread.start();
            this.outputThread.start();
            synchronized (this.ioThreadLock) {
                while (this.inputThread.isRunning() || this.outputThread.isRunning()) {
                    try { this.ioThreadLock.wait(); } catch (Throwable t) {}
                }
            }
        } catch (Throwable t) {
            Print.logInfo("Client:ControlThread - " + t);
            t.printStackTrace();
        } finally {
            this.closeSocket();
        }
        this.setRunStatus(THREAD_STOPPED);
        this.threadStopped();
    }
    
    /**
    *** Called when the thread is started
    **/
    protected void threadStarted()
    {
        Print.logDebug("Client:ControlThread started ...");
    }
    
    /**
    *** Called when the thread has stopped
    **/
    protected void threadStopped()
    {
        Print.logDebug("Client:ControlThread stopped ...");
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Attemps to stop the thread [CHECK]
    **/
    public void stopThread()
    {
        setRunStatus(THREAD_CHANGING);
    }

    /**
    *** Causes this thread to begin execution
    **/
    public void startThread()
    {
        this.start();
    }
    
    // ------------------------------------------------------------------------

    private int runStatus = THREAD_STOPPED;

    private int getRunStatus()
    {
        return this.runStatus;
    }

    private void setRunStatus(int state)
    {
        synchronized (this.ioThreadLock) {
            this.runStatus = state;
        }
    }

    /**
    *** Returns true if the thread is currently running
    *** @return True if the thread is currently running
    **/
    public boolean isRunning()
    {
        return (this.getRunStatus() == THREAD_RUNNING);
    }

    // ------------------------------------------------------------------------
    // Output thread
    
    /**
    *** Queues a message to be sent
    **/
    public void sendMessage(String msg)
    {
        if ((this.outputThread != null) && this.isRunning()) {
            this.outputThread.addCommand(msg);
        } else {
            throw new RuntimeException("ClientSocketThread must be running to send queued message");
        }
    }
    
    private class OutputThread
        extends Thread
    {

        private boolean                 isRunning = false;
        private Socket                  socket = null;
        private Object                  threadLock = null;
        private java.util.List<String>  cmdList = null;
        
        public OutputThread(Socket sock, Object threadLock) {
            this.socket = sock;
            this.threadLock = threadLock;
            this.cmdList = new Vector<String>();
        }
        
        public void start() {
            this.isRunning = true;
            super.start();
        }

        public boolean isRunning() {
            return this.isRunning;
        }
        
        public void addCommand(String cmd) {
            synchronized (this.cmdList) {
                this.cmdList.add(cmd);
                this.cmdList.notify();
            }
        }

        public void run() {
            String command = null;
            Print.logInfo("Client:OutputThread started");

            while (true) {
                
                /* wait for commands */
                synchronized (this.cmdList) {
                    while ((this.cmdList.size() <= 0) && (getRunStatus() == THREAD_RUNNING)) {
                        try { this.cmdList.wait(5000L); } catch (Throwable t) {/*ignore*/}
                    }
                    if (getRunStatus() != THREAD_RUNNING) { break; }
                    command = this.cmdList.remove(0).toString();
                }

                /* send commands */
                try {
                    ClientSocketThread.socketWriteLine(this.socket, command);
                } catch (Throwable t) {
                    Print.logError("Client:OutputThread - " + t);
                    t.printStackTrace();
                    break;
                }
            
            }
            
            if (getRunStatus() == THREAD_RUNNING) {
                Print.logWarn("Client:OutputThread stopped due to error");
            } else {
                Print.logInfo("Client:OutputThread stopped");
            }
            
            synchronized (this.threadLock) {
                this.isRunning = false;
                Print.logInfo("Client:OutputThread stopped");
                this.threadLock.notify();
            }
        }
        
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Adds an action listener
    *** @param al The action listener to add
    **/
    public void addActionListener(ActionListener al)
    {
        if (al != null) {
            if (this.actionListeners == null) {
                this.actionListeners = new Vector<ActionListener>();
            }
            if (!this.actionListeners.contains(al)) {
                this.actionListeners.add(al);
            }
        }
    }

    /**
    *** Removes an action listener
    *** @param al The action listener to remove
    **/
    public void removeActionListener(ActionListener al)
    {
        if (this.actionListeners != null) {
            this.actionListeners.remove(al);
        }
    }

    /**
    *** Invokes all listeners with an assciated command
    *** @param r a string that may specify a command (possibly one 
    ***     of several) associated with the event
    **/
    protected void invokeListeners(String r)
    {
        if (this.actionListeners != null) {
            for (Iterator i = this.actionListeners.iterator(); i.hasNext();) {
                ActionListener al = (ActionListener)i.next();
                ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, r);
                try {
                    al.actionPerformed(ae);
                } catch (Throwable t) {
                    Print.logError("Exception: " + t.getMessage());
                }
            }
        }
    }

    /**
    *** Invokes all listeners with an assciated command. Same as calling invokeListeners 
    *** @param msg a string that may specify a command (possibly one 
    ***     of several) associated with the event
    *** @see #invokeListeners
    **/
    protected void handleMessage(String msg)
    {
        this.invokeListeners(msg);
    }

    // ------------------------------------------------------------------------
    // input thread
    
    private class InputThread
        extends Thread
    {
        private boolean isRunning = false;
        private Socket  socket = null;
        private long    readTimeout = 5000L;
        private Object  threadLock = null;
        
        public InputThread(Socket sock, long timeout, Object threadLock) 
        {
            this.socket = sock;
            this.readTimeout = timeout;
            this.threadLock = threadLock;
        }
        
        public void start() {
            this.isRunning = true;
            super.start();
        }

        public boolean isRunning() {
            return this.isRunning;
        }

        public void run() {
            StringBuffer data = new StringBuffer();
            Print.logDebug("Client:InputThread started");
            
            while (true) {
                data.setLength(0);
                try {
                    if (this.readTimeout > 0L) {
                        this.socket.setSoTimeout((int)this.readTimeout);
                    }
                    ClientSocketThread.socketReadLine(this.socket, -1, data);
                } catch (InterruptedIOException ee) { // SocketTimeoutException ee) {
                    //error("Read interrupted (timeout) ...");
                    if (getRunStatus() != THREAD_RUNNING) { break; }
                    continue;
                } catch (Throwable t) {
                    Print.logError("Client:InputThread - " + t);
                    t.printStackTrace();
                    break;
                }
                ClientSocketThread.this.handleMessage(data.toString());
            }
            
            synchronized (this.threadLock) {
                this.isRunning = false;
                Print.logDebug("Client:InputThread stopped");
                this.threadLock.notify();
            }
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_HOST[]      = new String[] { "host" , "h"       };
    private static final String ARG_PORT[]      = new String[] { "port" , "p"       };
    private static final String ARG_SEND[]      = new String[] { "send"             };
    private static final String ARG_RECEIVE[]   = new String[] { "recv", "receive"  };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + ClientSocketThread.class.getName() + " {options}");
        Print.logInfo("'Send' Options:");
        Print.logInfo("  -host=<host>    The destination host");
        Print.logInfo("  -port=<port>    The destination port");
        Print.logInfo("  -send=<data>    The data to send (prefix with '0x' for hex data)");
        Print.logInfo("'Receive' Options (not yet implemented):");
        Print.logInfo("  -port=<port>    The port on which to listen for incoming data");
        Print.logInfo("  -recv           Set to 'receive' mode");
        System.exit(1);
    }

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String host = RTConfig.getString(ARG_HOST, null);
        int    port = RTConfig.getInt(ARG_PORT, 0);

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
            String dataStr = RTConfig.getString(ARG_SEND,"hello");
            byte data[] = dataStr.startsWith("0x")? StringTools.parseHex(dataStr,null) : dataStr.getBytes();
            ClientSocketThread cst = new ClientSocketThread(host, port);
            try {
                cst.openSocket();
                cst.socketWriteBytes(data);
            } catch (Throwable t) {
                Print.logException("Error", t);
            } finally {
                cst.closeSocket();
            }
            System.exit(0);
        }

        /* receive data */
        if (RTConfig.hasProperty(ARG_RECEIVE)) {
            if (port <= 0) {
                Print.logError("Target port not specified");
                usage();
            }
            if (!StringTools.isBlank(host)) {
                Print.logWarn("Specified 'host' will be ignored");
            }
            Print.logError("Receive not yet implemented ...");
            System.exit(99);
        }
        
        /* show usage */
        usage();

    }
    
}
