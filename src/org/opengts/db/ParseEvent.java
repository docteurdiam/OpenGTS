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
//  2010/07/18  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public class ParseEvent
    implements GeoEvent.GeoEventHandler
{

    public static boolean   DEBUG_MODE      = false;
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static interface ParseEventHandler
    {
        public boolean parseStream(InputStream stream, GeoEvent.GeoEventHandler gevHandler)
            throws IOException;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private ParseEventHandler   parseHandler   = null;
    private String              accountID      = null;
    private String              deviceID       = null;

    private DCServerConfig      dcserver       = null;
    private String              uniquePrefix[] = new String[] { "" };
    private double              minSpeedKPH    = 0.0;
    private boolean             estimateOdom   = false;
    private boolean             simGeozones    = false;
    private double              minMovedMeters = 0.0;

    private Account             account        = null;
    private Device              device         = null;

    /**
    *** Consgtructor 
    **/
    public ParseEvent(
        DCServerConfig dcserver, 
        ParseEventHandler parseHandler, 
        String accountID, String deviceID)
    {
        super();

        this.parseHandler = parseHandler;

        this.accountID    = accountID;
        this.account      = null;

        this.deviceID     = !StringTools.isBlank(this.accountID)? deviceID : null;
        this.device       = null;

        this.dcserver     = dcserver;
        if (this.dcserver != null) {
            this.uniquePrefix   = this.dcserver.getUniquePrefix();
            this.minSpeedKPH    = this.dcserver.getMinimumSpeedKPH(0.0);
            this.estimateOdom   = this.dcserver.getEstimateOdometer(false);
            this.simGeozones    = this.dcserver.getSimulateGeozones(false);
            this.minMovedMeters = this.dcserver.getMinimumMovedMeters(0.0);
        }
        
    }

    /**
    *** Parse file
    **/
    public boolean parse(File parseFile)
    {
        if (this.parseHandler == null) {
            return false;
        } else
        if (parseFile != null) {
            boolean rtn = false;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(parseFile);
                rtn = this.parseHandler.parseStream(fis, this);
            } catch (IOException ioe) {
                Print.logException("IO Error", ioe);
            } finally {
                if (fis != null) { try { fis.close(); } catch (Throwable th) {} }
            }
            return rtn;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Callback to handle event
    **/
    public int handleGeoEvent(GeoEvent gev)
    {
        int eventCount = 0;

        /* validate record identification */
        // We either have both AccountID/DeviceID, or neither
        if (gev.hasAccountID() != gev.hasDeviceID()) {
            Print.logError("Missing either Account or Device ID");
            return eventCount;
        }

        /* Account ID */
        if (gev.hasAccountID()) {
            if (!StringTools.isBlank(this.accountID) && !this.accountID.equals(gev.getAccountID())) {
                Print.logError("Mismatched AccountID!");
                return eventCount;
            }
        } else {
            if (!StringTools.isBlank(this.accountID)) {
                gev.setAccountID(this.accountID);
            }
        }
        String gevAcctID = gev.getAccountID(); // may be blank

        /* Device ID */
        if (gev.hasDeviceID()) {
            if (!StringTools.isBlank(this.deviceID) && !this.deviceID.equals(gev.getDeviceID())) {
                Print.logError("Mismatched DeviceID!");
                return eventCount;
            }
        } else {
            if (!StringTools.isBlank(this.deviceID)) {
                gev.setDeviceID(this.deviceID);
            }
        }
        String gevDevID = gev.getDeviceID(); // may be blank
        
        /* load device */
        String mobileID = gev.getMobileID();
        boolean validateMobileID = true;
        if (this.device == null) {
            if (!StringTools.isBlank(gevDevID)) {
                // load account record
               if (this.account == null) {
                    try {
                        this.account = Account.getAccount(gevAcctID); // may throw DBException
                        if (this.account == null) {
                            Print.logError("Account-ID does not exist: " + gevAcctID);
                            return eventCount;
                        }
                    } catch (DBException dbe) {
                        Print.logException("Error loading Account: " + gevAcctID, dbe);
                        return eventCount;
                    }
                }
                // load device record
                try {
                    this.device = Device.getDevice(this.account, gevDevID, false); // may throw DBException
                    if (this.device == null) {
                        Print.logError("Device-ID does not exist: " + gevAcctID + "/" + gevDevID);
                        return eventCount;
                    }
                } catch (DBException dbe) {
                    Print.logException("Error loading Device: " + gevAcctID + "/" + gevDevID, dbe);
                    return eventCount;
                }
            } else
            if (!StringTools.isBlank(mobileID)) {
                this.device = DCServerFactory.loadDeviceByPrefixedModemID(this.uniquePrefix, mobileID);
                if (this.device == null) {
                    // error messages already displayed
                    return eventCount;
                }
                if (this.account == null) {
                    this.account = this.device.getAccount();
                } else
                if (this.account.getAccountID().equals(this.device.getAccountID())) {
                    this.device.setAccount(this.account);
                } else {
                    Print.logError("Device AccountID does not match defined Account: " + this.device.getAccountID());
                    return eventCount;
                }
                // no need to validate mobile ID
                validateMobileID = false;
            } else {
                Print.logError("No Device/Mobile ID defined");
                return eventCount;
            }
        }

        /* validate MobileID */
        if (validateMobileID && !StringTools.isBlank(mobileID)) {
            boolean match = false;
            String uniqueID = this.device.getUniqueID();
            if (!StringTools.isBlank(uniqueID)) {
                for (String pfx : this.uniquePrefix) {
                    if (uniqueID.equals(pfx + mobileID)) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                Print.logError("Unique-ID does not match Device: " + uniqueID);
                return eventCount;
            }
        }

        /* Account check */
        if (this.account == null) {
            this.account = this.device.getAccount();
        } else
        if (this.account.getAccountID().equals(this.device.getAccountID())) {
            this.device.setAccount(this.account);
        } else {
            Print.logError("Device AccountID does not match defined Account: " + this.device.getAccountID());
            return eventCount;
        }
        gev.setAccount(this.account);
        gev.setDevice(this.device);

        /* timestamp */
        long timestamp = gev.getTimestamp();
        if (timestamp <= 0L) {
            Print.logInfo("No valid Timestamp!");
            return eventCount;
        }
        
        /* GeoPoint */
        GeoPoint geoPoint = gev.getGeoPoint();
        boolean validGPS  = gev.isGeoPointValid();

        /* status code */
        int statusCode;
        if (!gev.hasStatusCode()) {
            statusCode = StatusCodes.STATUS_LOCATION;
            gev.setStatusCode(statusCode);
        } else {
            statusCode = gev.getStatusCode();
            if (statusCode <= 0) {
                Print.logInfo("No valid StatusCode!");
                return eventCount;
            }
        }

        /* minimum speed adjustment */
        if (!validGPS || (gev.getSpeedKPH() < this.minSpeedKPH)) {
            gev.setSpeedKPH(0.0);
            gev.setHeading(0.0);
        }

        /* odometer */
        double odometerKM = gev.hasOdometerKM()? gev.getOdometerKM() : 0.0;
        if (this.device != null) {
            if (odometerKM <= 0.0) {
                odometerKM = (this.estimateOdom && validGPS)? 
                    this.device.getNextOdometerKM(geoPoint) : 
                    this.device.getLastOdometerKM();
            } else {
                odometerKM = this.device.adjustOdometerKM(odometerKM);
            }
            gev.setOdometerKM(odometerKM);
        }

        /* simulate Geozone arrival/departure */
        if (this.simGeozones && validGPS && (this.device != null)) {
            java.util.List<Device.GeozoneTransition> zone = this.device.checkGeozoneTransitions(timestamp, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    GeoEvent zoneEv = new GeoEvent(gev); // clone
                    zoneEv.setTimestamp(z.getTimestamp());
                    zoneEv.setStatusCode(z.getStatusCode());
                    zoneEv.setGeozoneID(z.getGeozoneID());
                    zoneEv.setGeozone(z.getGeozone());
                    if (this.insertEventRecord(zoneEv)) {
                        eventCount++;
                    }
                    Print.logInfo("Geozone    : " + z);
                }
            }
        }

        /* previous event checks */
        if ((statusCode != StatusCodes.STATUS_LOCATION) || !validGPS) {
            if (this.insertEventRecord(gev)) {
                eventCount++;
            }
        } else
        if ((this.device == null) || !this.device.isNearLastValidLocation(geoPoint,this.minMovedMeters)) {
            if (this.insertEventRecord(gev)) {
                eventCount++;
            }
        }

        /* update device date */
        if (!DEBUG_MODE) {
            // TODO: optimize
            try {
                //DBConnection.pushShowExecutedSQL();
                if (this.device != null) {
                    this.device.updateChangedEventFields();
                }
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + gevAcctID + "/" + gevDevID, dbe);
            } finally {
                //DBConnection.popShowExecutedSQL();
            }
        } else {
            // TODO: reset any changes made to device record
        }

        /* return success */
        return eventCount;

    }
    
    protected boolean insertEventRecord(GeoEvent gev)
    {
        Print.logInfo("GeoEvent: " + gev);
        if (DEBUG_MODE) { return false; }
        
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String ARG_ACCOUNT[] = new String[] { "account" , "acct", "a" };
    private static String ARG_DEVICE[]  = new String[] { "device"  , "dev" , "d" };
    private static String ARG_FORMAT[]  = new String[] { "format"  , "fmt"       };
    private static String ARG_DCS[]     = new String[] { "dcserver", "dcs"       };
    private static String ARG_FILE[]    = new String[] { "file"                  };

    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + ParseEvent.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -account=<id>   Acount ID which owns Device");
        Print.sysPrintln("  -device=<id>    Device ID to which parsed events will be inserted");
        Print.sysPrintln("  -format=<class> The format handler class");
        Print.sysPrintln("  -dcs=<name>     The format handler class");
        Print.sysPrintln("  -file=<file>    The file to parse");
        System.exit(1);
    }

}
