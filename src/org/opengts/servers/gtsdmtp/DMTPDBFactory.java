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
//  2006/04/09  Martin D. Flynn
//     -Integrated 'DBException'
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//  2007/07/27  Martin D. Flynn
//     -Repackaged to "org.opengts.servers.gtsdmtp"
//  2008/02/11  Martin D. Flynn
//     -Added support for "<Account>.autoAddDevice()"
// ----------------------------------------------------------------------------
package org.opengts.servers.gtsdmtp;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.db.dmtp.*;

import org.opendmtp.server.base.DMTPServer;
import org.opendmtp.server.db.AccountDB;
import org.opendmtp.server.db.DeviceDB;

// ----------------------------------------------------------------------------

public class DMTPDBFactory
    implements DMTPServer.DBFactory
{
    
    // ----------------------------------------------------------------------------

    public static final String DEFAULT_UNIQUE_PREFIX    = "gtc";
    
    // ----------------------------------------------------------------------------

    /* return an AccountDB wrapper for the specified Account table entry */
    public AccountDB getAccountDB(String acctName) 
    {
        if (acctName != null) {
            try {
                Account acct = Account.getAccount(acctName);
                if (acct != null) {
                    return new AccountDBImpl(acct);
                } else {
                    return new AccountDBImpl(acctName);
                }
            } catch (DBException dbe) {
                dbe.printException();
            }
        }
        return null;
    }

    // ----------------------------------------------------------------------------

    /* return a DeviceDB wrapper for the specified Device table entry */
    public DeviceDB getDeviceDB(AccountDB acctDB, String devName)
    {

        /* null AccountDB or DeviceID? */
        if ((acctDB == null) || StringTools.isBlank(devName)) {
            return null;
        }
        AccountDBImpl acctDBImpl = (AccountDBImpl)acctDB;
        Account account = acctDBImpl.getAccount(); // may be null

        /* get Device */
        try {
            if (account != null) {
                // By Account/DeviceID
                Device dev = Transport.loadDeviceByTransportID(account, devName);
                if (dev != null) {
                    return new DeviceDBImpl(dev);
                }
            } else {
                // By UniqueID
                String prefixID = StringTools.blankDefault(acctDBImpl.getPrefixID(),DEFAULT_UNIQUE_PREFIX);
                String uniqueID = prefixID + "_" + devName;
                Device dev = Transport.loadDeviceByUniqueID(uniqueID);
                if (dev != null) {
                    acctDBImpl.setAccountName(dev.getAccountID());
                    return new DeviceDBImpl(dev);
                }
            }
        } catch (DBException dbe) {
            dbe.printException();
        }
        
        /* not found */
        return null;
    }

    // ----------------------------------------------------------------------------

    /* return a DeviceDB wrapper for the specified Device table entry */
    public DeviceDB getDeviceDB(byte uniqId[]) 
    {
        try {
            Device db = Transport.loadDeviceByUniqueID(uniqId);
            if (db != null) {
                return new DeviceDBImpl(db);
            }
        } catch (DBException dbe) {
            dbe.printException();
        }
        return null;
    }

    // ----------------------------------------------------------------------------

}
