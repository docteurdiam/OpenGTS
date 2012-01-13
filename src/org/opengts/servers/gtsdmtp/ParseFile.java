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

import org.opendmtp.codes.ServerErrors;
import org.opendmtp.server.base.DMTPServer;
import org.opendmtp.server.base.AccountID;
import org.opendmtp.server.base.DeviceID;
import org.opendmtp.server.base.Packet;
import org.opendmtp.server.base.PacketParseException;
import org.opendmtp.server.base.Event;

public class ParseFile
{

    // ------------------------------------------------------------------------

    private static String ARG_ACCOUNT[] = new String[] { "account", "acct", "a" };
    private static String ARG_DEVICE[]  = new String[] { "device" , "dev" , "d" };
    private static String ARG_FILE[]    = new String[] { "file"   ,         "f" };

    // ------------------------------------------------------------------------

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + ParseFile.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns Device");
        Print.logInfo("  -device=<id>    Device ID to which parsed events will be inserted");
        Print.logInfo("  -file=<file>    The OpenDMTP event file to parse");
        System.exit(1);
    }

    // ------------------------------------------------------------------------
    // This class will read event packets from a file and insert them into the
    // EventData table for the specified Device.
    
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        DMTPServer.setDBFactory(new DMTPDBFactory());
        String accountID  = RTConfig.getString(ARG_ACCOUNT, null);
        String deviceID   = RTConfig.getString(ARG_DEVICE , null);
        File   evFile     = RTConfig.getFile(ARG_FILE,null);

        /* account/device/file specified? */
        if (StringTools.isBlank(accountID) || StringTools.isBlank(deviceID) || (evFile == null)) {
            usage();
        }

        /* file exists? */
        if (!evFile.isFile()) {
            Print.sysPrintln("ERROR: File does not exist - " + evFile);
            System.exit(99);
        }

        /* make sure the DB is properly initialized */
        if (!DBAdmin.verifyTablesExist()) {
            Print.sysPrintln("ERROR: MySQL database has not yet been properly initialized");
            System.exit(99);
        }

        /* load account/device */
        AccountID account = null;
        DeviceID  device  = null;
        try {
            account = AccountID.loadAccountID(accountID);
            if (account == null) {
                Print.sysPrintln("ERROR: Unable to load Account - " + accountID);
                System.exit(99);
            }
            device = DeviceID.loadDeviceID(account, deviceID);
            if (device == null) {
                Print.sysPrintln("ERROR: Unable to load Device - " + accountID + "/" + deviceID);
                System.exit(99);
            }
        } catch (PacketParseException ppe) {
            Print.logException("Unable to load DeviceID: " + accountID + "/" + deviceID, ppe);
            System.exit(99);
        }
        
        /* read file */
        byte pktData[] = FileTools.readFile(evFile);
        if (ListTools.isEmpty(pktData)) {
            Print.sysPrintln("ERROR: Unable to read packet file: " + evFile);
            System.exit(99);
        }
        
        /* parse data */
        int pktOfs = 0;
        for (;pktOfs < pktData.length;) {
            
            /* extract packet */
            int len = Packet.getPacketLength(pktData, pktOfs);
            if (len < 0) {
                Print.sysPrintln("ERROR: Found invalid packet at offset " + pktOfs);
                System.exit(99);
            }
            byte pkt[] = new byte[len];
            System.arraycopy(pktData, pktOfs, pkt, 0, len);

            /* parse and insert */
            try {
                Packet packet = new Packet(device, true/*isClient*/, pkt); // client packet
                if (packet.isEventType()) {
                    Event evData = new Event(null, packet);
                    int err = device.saveEvent(evData);
                    if (err != ServerErrors.NAK_OK) {
                        Print.sysPrintln("ERROR: Event insertion error: " + err);
                    } else
                    if (RTConfig.isDebugMode()) {
                        Print.sysPrintln("Saved event: " + evData);
                    }
                } else {
                    // not an event packet - ignore
                }
            } catch (PacketParseException ppe) {
                Print.logException("Unable to parse packet", ppe);
                System.exit(1);
            }

            /* advance to next packet */
            pktOfs += len;

        }
        Print.sysPrintln("");
        Print.sysPrintln("... Done.");

    }

}
