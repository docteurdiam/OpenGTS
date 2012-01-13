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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public abstract class CommandPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  ARG_ACCOUNT             = DCServerFactory.CMDARG_ACCOUNT;
    public static final String  ARG_DEVICE              = DCServerFactory.CMDARG_DEVICE;

    public static final String  ARG_CMDTYPE             = DCServerFactory.CMDARG_CMDTYPE;
    public static final String  ARG_CMDNAME             = DCServerFactory.CMDARG_CMDNAME;
    public static final String  ARG_ARG0[]              = new String[] { (DCServerFactory.CMDARG_ARG+"0"), DCServerFactory.CMDARG_ARG };
    public static final String  ARG_ARG1                = DCServerFactory.CMDARG_ARG + "1";
    public static final String  ARG_ARG2                = DCServerFactory.CMDARG_ARG + "2";
    public static final String  ARG_ARG3                = DCServerFactory.CMDARG_ARG + "3";
    public static final String  ARG_ARG4                = DCServerFactory.CMDARG_ARG + "4";
    public static final String  ARG_ARG5                = DCServerFactory.CMDARG_ARG + "5";
    public static final String  ARG_ARG6                = DCServerFactory.CMDARG_ARG + "6";
    public static final String  ARG_ARG7                = DCServerFactory.CMDARG_ARG + "7";
    public static final String  ARG_ARG8                = DCServerFactory.CMDARG_ARG + "8";
    public static final String  ARG_ARG9                = DCServerFactory.CMDARG_ARG + "9";

    public static final String  ARG_SERVER              = DCServerFactory.CMDARG_SERVER;
    
    public static final String  ARG_IP                  = "ip";
    public static final String  ARG_PHONE               = "phone";
    public static final String  ARG_LASTCONNECT         = "lastConnect";
    
    public static final String  ARG_RESULT              = DCServerFactory.RESPONSE_RESULT;
    public static final String  ARG_MESSAGE             = DCServerFactory.RESPONSE_MESSAGE;

    // ------------------------------------------------------------------------

    public static RTProperties setResult(RTProperties rtp, DCServerFactory.ResultCode result)
    {
        if ((rtp != null) && (result != null)) {
            rtp.setString(ARG_RESULT , result.getCode());
            rtp.setString(ARG_MESSAGE, result.toString());
            if (!result.equals(DCServerFactory.ResultCode.SUCCESS)) {
                Print.logError("Command Error: " + result.getCode() + " - " + result.getMessage());
            }
        }
        return rtp;
    }

    protected static byte[] RESULT(RTProperties rtp, DCServerFactory.ResultCode result)
    {
        CommandPacketHandler.setResult(rtp,result);
        return (rtp.toString() + "\n").getBytes();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Session 'terminate' indicator */
    private boolean         terminate           = false;

    /* session start time */
    private long            sessionStartTime    = 0L;

    /* session IP address */
    private InetAddress     inetAddress         = null;
    private String          ipAddress           = null;
    
    /* packet handler constructor */
    public CommandPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    public abstract String getServerName();
    public abstract DCServerFactory.ResultCode handleCommand(Device device, String cmdType, String cmdName, String args[]);

    // ------------------------------------------------------------------------

    /* UDP response port */
    public int getResponsePort()
    {
        return super.getLocalPort();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);

        /* init */
        this.sessionStartTime = DateTime.getCurrentTimeSec();
        this.inetAddress      = inetAddr;
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;

        /* debug message */
        Print.logInfo("---- Begin Command Packet Handler: " + this.ipAddress);

    }
    
    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        
        Print.logInfo("---- End Command Packet Handler: " + this.ipAddress);
        try { Thread.sleep(10L); } catch (Throwable t) {}
        
    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        return PACKET_LEN_LINE_TERMINATOR;
    }
            
    // ------------------------------------------------------------------------

    /* indicate that the session should terminate */
    public boolean terminateSession()
    {
        return this.terminate;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if (!ListTools.isEmpty(pktBytes)) {
            String cmd = StringTools.toStringValue(pktBytes);
            this.terminate = true;
            return this.parseCommand(cmd);
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /* parse and insert data record */
    // account=myaccount device=mydevice command="someCommand" arg="someArg"
    private byte[] parseCommand(String cmd)
    {
        RTProperties rtCmd = new RTProperties(cmd);
        Print.logInfo("Command: " + rtCmd);

        /* standard arguments */
        String accountID = rtCmd.getString(ARG_ACCOUNT,null);
        String deviceID  = rtCmd.getString(ARG_DEVICE ,null);
        String cmdType   = rtCmd.getString(ARG_CMDTYPE,null);
        String cmdName   = rtCmd.getString(ARG_CMDNAME,null);
        String cmdArg0   = rtCmd.getString(ARG_ARG0   ,null);
        String cmdArg1   = rtCmd.getString(ARG_ARG1   ,null);
        String cmdArg2   = rtCmd.getString(ARG_ARG2   ,null);
        String cmdArg3   = rtCmd.getString(ARG_ARG3   ,null);
        String cmdArg4   = rtCmd.getString(ARG_ARG4   ,null);
        String cmdArg5   = rtCmd.getString(ARG_ARG5   ,null);
        String cmdArg6   = rtCmd.getString(ARG_ARG6   ,null);
        String cmdArg7   = rtCmd.getString(ARG_ARG7   ,null);
        String cmdArg8   = rtCmd.getString(ARG_ARG8   ,null);
        String cmdArg9   = rtCmd.getString(ARG_ARG9   ,null);

        /* get account record */
        Account account = null;
        if (StringTools.isBlank(accountID)) {
            Print.logDebug("Account not specified");
        } else {
            try {
                account = Account.getAccount(accountID);
            } catch (DBException dbe) {
                account = null;
            }
            if (account == null) {
                Print.logError("Account not found: %s", accountID);
                return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_ACCOUNT);
            } else {
                Print.logDebug("Found Account: [%s] %s", account.getAccountID(), account.getDescription());
            }
        }

        /* get device record */
        Device device = null;
        if (account == null) {
            //
        } else
        if (StringTools.isBlank(deviceID)) {
            Print.logDebug("Device not specified");
        } else {
            try {
                device = Device.getDevice(account, deviceID);
            } catch (DBException dbe) {
                device = null;
            }
            if (device == null) {
                Print.logError("Device not found: %s/%s", accountID, deviceID);
                return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_DEVICE);
            } else {
                Print.logDebug("Found Device: [%s:%s] %s", device.getDeviceID(), device.getUniqueID(), device.getDescription());
            }
        }

        /* no command? */
        if (StringTools.isBlank(cmdType)) {
            return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_COMMAND);
        }

        /* Account/Device required */
        if (device == null) {
            return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_DEVICE);
        }
        
        /* set device properties */
        rtCmd.setString(ARG_SERVER      , device.getDeviceCode());
        //rtCmd.setString(ARG_IP          , StringTools.trim(device.getIpAddressCurrent()));
        //rtCmd.setString(ARG_PHONE       , device.getSimPhoneNumber());
        //rtCmd.setLong(  ARG_LASTCONNECT , device.getLastTotalConnectTime());

        /* account/device command */
        String cmdArgs[] = new String[] { 
            cmdArg0, cmdArg1, cmdArg2, cmdArg3, cmdArg4, cmdArg5, cmdArg6, cmdArg7, cmdArg8, cmdArg9 
            };
        DCServerFactory.ResultCode result = this.handleCommand(device, cmdType, cmdName, cmdArgs);
        if (result != null) {
            return RESULT(rtCmd, result);
        } else {
            return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_COMMAND);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* validate email address */
    public static boolean validateAddress(String addr)
    {
        try {
            return SendMail.validateAddress(addr);
        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
            // this will fail if JavaMail support for SendMail is not available.
            Print.logWarn("SendMail error: " + t);
            return false;
        }
    }

    /* validate the syntax of the specified list of multiple email addresses */
    public static boolean validateAddresses(String addrs)
    {
        try {
            return SendMail.validateAddresses(addrs);
        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
            // this will fail if JavaMail support for SendMail is not available.
            Print.logWarn("SendMail error: " + t);
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

    /* return command 'From' email address */
    public static String getFromEmailCommand(Account account)
    {
        return CommandPacketHandler.getFromEmailCommand(Account.getPrivateLabel(account));
    }
    
    /* return command 'From' email address */
    public static String getFromEmailCommand(BasicPrivateLabel bpl)
    {
        if (bpl != null) {
            String email = bpl.getEMailAddress(BasicPrivateLabel.EMAIL_TYPE_COMMAND);
            if (!StringTools.isBlank(email)) {
                return email;
            }
        }
        return SendMail.getUserFromEmailAddress(null);
    }
    
}
