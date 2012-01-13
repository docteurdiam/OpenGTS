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
//  TAIP (Trimble ASCII Interface Protocol) packet 'business' logic.
//  Protocol obtained from online sources.
// ----------------------------------------------------------------------------
// Change History:
//  2010/11/29  Martin D. Flynn (1/29)
//     -Initial OpenGTS release
//  2011/10/03  Martin D. Flynn
//     -Added support for "ESTIMATE_ODOMETER", "SIMEVENT_GEOZONES" [B03]
// ----------------------------------------------------------------------------
package org.opengts.servers.taip;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------

    public static       String  UNIQUEID_PREFIX[]           = null;
    public static       double  MINIMUM_SPEED_KPH           = Constants.MINIMUM_SPEED_KPH;
    public static       boolean ESTIMATE_ODOMETER           = true;
    public static       boolean SIMEVENT_GEOZONES           = true;
    public static       double  MINIMUM_MOVED_METERS        = 0.0;

    // ------------------------------------------------------------------------
    
    /* session IP address */
    private String          ipAddress                       = null;
    private int             clientPort                      = 0;

    /* current device */
    private Device          device                          = null;
    private DataTransport   dataXPort                       = null;
    
    /* session start time */
    private long            sessionStartTime                = 0L;

    /* count the number of events we've parsed during this session */
    private int             eventTotalCount                 = 0;

    /* packet handler constructor */
    public TrackClientPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);
        super.clearTerminateSession();

        /* init */
        this.sessionStartTime = DateTime.getCurrentTimeSec();
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;
        this.clientPort       = this.getSessionInfo().getRemotePort();
        this.eventTotalCount  = 0;

    }
    
    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);
    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        
        /* parse */
        if (pktBytes == null) {
            Print.logError("Packet is null");
        } else
        if (pktBytes.length < 2) {
            Print.logError("Unexpected packet length: " + pktBytes.length);
        } else {
            String s = StringTools.toStringValue(pktBytes).trim();
            Print.logInfo("Recv: " + s); // debug message
            Print.logInfo("Hex: 0x" + StringTools.toHexString(pktBytes)); // debug message
            this.parseInsertRecord(s);
        }
        
        /* return ACK */
        return null;
        
    }

    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private boolean parseInsertRecord(String s)
    {
        // >ABB{C}[;ID=DDDD][;*FF]<
        //   >    = Start of new message
        //   A    = Message qualifier
        //   BB   = A two character message identifier
        //   C    = Data string
        //   DDDD = Optional 4 character vehicle ID
        //   FF   = Optional 2 character checksum
        //   <    = Delimiting character
        //   {x}  = Signifies that x can occur any number of times
        //   [x]  = Signifies that x may optionally occur once
        // -------
        // Message Qualifier:
        //   Q = Query for a single sentence (sent to GPS sensor)
        //   R = Response to a query or a scheduled report (from the sensor)
        //   F = Schedule reporting frequency interval in seconds
        //   S = Enables equipment to be initialized, and sets various message types
        //   D = Specify a minimum distance traveled and a minimum and maximum time interval for the next report
        // -------
        // Message Identifier:
        //   PR = protocol
        //   VR = version number
        //   PV = Position/Velocity Solution
        // Example:
        // -------
        // >RPV21305+3958635-1424085300000012;ID=0011<
        // >RPV15714+3739438-1420384601512612;ID=1234;*7F<
        //   R          = [ 0, 1] Response Query
        //   PV         = [ 1, 3] Position/Velocity
        //   15714      = [ 3, 8] GPS time-of-day
        //   +3739438   = [ 8,16] Latitude
        //   -14203846  = [16,25] Longitude
        //   015        = [25,28] Speed (mph)
        //   126        = [28,31] Heading (degrees)
        //   1          = [31,32] GPS source [0=2D-GPS, 1=3D-GPS, 2=2D-DGPS, 3=3D-DGPS, 6=DR, 8=Degraded-DR, 9=Unknown]
        //   2          = [32,33] Age [0=n/a, 1=old, 2=fresh]
        //   ;ID=1234   = VehicleID
        //   ;*7F       = Checksum
        //   <          = End of message

        /* pre-validate */
        if (StringTools.isBlank(s)) {
            Print.logError("String is null/blank");
            return false;
        } else
        if (s.length() < 5) {
            Print.logError("String is invalid length");
            return false;
        } else
        if (!s.startsWith(">")) {
            Print.logError("String does not start with '>'");
            return false;
        }

        /* ends with "<"? */
        int se = s.endsWith("<")? (s.length() - 1) : s.length();
        s = s.substring(1,se);

        /* split */
        String T[] = StringTools.split(s,';');

        /* handle ">RPV" records only */
        if (!T[0].startsWith("RPV")) {
            Print.logWarn("Only 'RPV' record types are currently supported");
            return false;
        }

        /* RPV record */
        if (T[0].length() < 33) {
            Print.logError("Invalid 'RPV' data length");
            return false;
        }

        /* mobile id */
        String mobileID = null;
        for (int i = 1; i < T.length; i++) {
            if (T[i].startsWith("ID=")) {
                mobileID = T[i].substring(3);
                break;
            }
        }

        /* parse */
        //   R          = [ 0, 1] Response Query
        //   PV         = [ 1, 3] Position/Velocity
        //   15714      = [ 3, 8] GPS time-of-day
        //   +3739438   = [ 8,16] Latitude
        //   -14203846  = [16,25] Longitude
        //   015        = [25,28] Speed (mph)
        //   126        = [28,31] Heading (degrees)
        //   1          = [31,32] GPS source [0=2D-GPS, 1=3D-GPS, 2=2D-DGPS, 3=3D-DGPS, 6=DR, 8=Degraded-DR, 9=Unknown]
        //   2          = [32,33] Age [0=n/a, 1=old, 2=fresh]
        int    statusCode = StatusCodes.STATUS_LOCATION;
        int    gpsTOD     = StringTools.parseInt(T[0].substring( 3, 8), 0);
        double latitude   = (double)StringTools.parseLong(T[0].substring( 8,16),0L) / 100000.0;
        double longitude  = (double)StringTools.parseLong(T[0].substring(16,25),0L) / 100000.0;
        double speedKPH   = StringTools.parseDouble(T[0].substring(25,28), 0.0) * GeoPoint.KILOMETERS_PER_MILE;
        double headingDeg = StringTools.parseDouble(T[0].substring(28,31), 0.0);
        String srcStr     = T[0].substring(31,32);
        String ageStr     = T[0].substring(32,33);
        double altitudeM  = 0.0;
        double odomKM     = 0.0;
        long   gpioInput  = 0L;

        /* Fix time */
        long fixtime = (new DateTime(DateTime.getGMTTimeZone())).getDayStart() + gpsTOD;
        if ((fixtime - DateTime.MinuteSeconds(15)) > DateTime.getCurrentTimeSec()) {
            fixtime -= DateTime.DaySeconds(1);
        }

        /* lat/lon valid? */
        boolean validGPS = true;
        if (!GeoPoint.isValid(latitude,longitude)) {
            Print.logWarn("Invalid lat/lon: " + latitude + "/" + longitude);
            validGPS   = false;
            latitude   = 0.0;
            longitude  = 0.0;
            speedKPH   = 0.0;
            headingDeg = 0.0;
        }
        GeoPoint geoPoint = new GeoPoint(latitude,longitude);

        /* adjustments to received values */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* debug */
        Print.logInfo("MobileID  : " + mobileID);
        Print.logInfo("Timestamp : " + new DateTime(fixtime));
        Print.logInfo("GeoPoint  : " + geoPoint);
        Print.logInfo("Speed km/h: " + speedKPH + " [" + headingDeg + "]");

        /* mobile-id */
        if (StringTools.isBlank(mobileID)) {
            Print.logError("Missing MobileID");
            return false;
        }

        /* find Device */
        String accountID = "";
        String deviceID  = "";
        String uniqueID  = "";
        Device device = DCServerFactory.loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, mobileID);
        if (device == null) {
            return false; // errors already displayed
        } else {
            accountID = device.getAccountID();
            deviceID  = device.getDeviceID();
            uniqueID  = device.getUniqueID();
            Print.logInfo("UniqueID  : " + uniqueID);
            Print.logInfo("DeviceID  : " + accountID + "/" + deviceID);
        }
        
        /* check IP address */
        DataTransport dataXPort = device.getDataTransport();
        if ((this.ipAddress != null) && !dataXPort.isValidIPAddress(this.ipAddress)) {
            DTIPAddrList validIPAddr = dataXPort.getIpAddressValid(); // may be null
            Print.logError("Invalid IP Address from device: " + this.ipAddress + " [expecting " + validIPAddr + "]");
            return false;
        }
        dataXPort.setIpAddressCurrent(this.ipAddress);    // FLD_ipAddressCurrent
        dataXPort.setRemotePortCurrent(this.clientPort);  // FLD_remotePortCurrent
        dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!dataXPort.getDeviceCode().equalsIgnoreCase(Constants.DEVICE_CODE)) {
            dataXPort.setDeviceCode(Constants.DEVICE_CODE); // FLD_deviceCode
        }

        /* reject invalid GPS fixes? */
        if (!validGPS && (statusCode == StatusCodes.STATUS_LOCATION)) {
            // ignore invalid GPS fixes that have a simple 'STATUS_LOCATION' status code
            Print.logWarn("Ignoring event with invalid latitude/longitude");
            return false;
        }

        /* estimate GPS-based odometer */
        if (odomKM <= 0.0) {
            // calculate odometer
            odomKM = (ESTIMATE_ODOMETER && validGPS)? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            // bounds-check odometer
            odomKM = device.adjustOdometerKM(odomKM);
        }
        Print.logInfo("OdometerKM: " + odomKM);

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && validGPS) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    this.insertEventRecord(device, 
                        z.getTimestamp(), z.getStatusCode(), z.getGeozone(),
                        geoPoint, gpioInput, speedKPH, headingDeg, altitudeM, odomKM);
                    Print.logInfo("Geozone    : " + z);
                    if (z.getStatusCode() == statusCode) {
                        // suppress 'statusCode' event if we just added it here
                        Print.logDebug("StatusCode already inserted: 0x" + StatusCodes.GetHex(statusCode));
                        statusCode = StatusCodes.STATUS_IGNORE;
                    }
                }
            }
        }

        /* insert event */
        if (statusCode == StatusCodes.STATUS_NONE) {
            // ignore this event
        } else
        if ((statusCode != StatusCodes.STATUS_LOCATION) || !validGPS) {
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*GeoZone*/,
                geoPoint, gpioInput, speedKPH, headingDeg, altitudeM, odomKM);
        } else
        if (!device.isNearLastValidLocation(geoPoint,MINIMUM_MOVED_METERS)) {
            if ((statusCode == StatusCodes.STATUS_LOCATION) && (speedKPH > 0.0)) {
                statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
            }
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*GeoZone*/,
                geoPoint, gpioInput, speedKPH, headingDeg, altitudeM, odomKM);
        }

        /* save device changes */
        try {
            // TODO: check "this.device" vs "this.dataXPort"
            device.updateChangedEventFields();
        } catch (DBException dbe) {
            Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
        } finally {
            //
        }

        return true;

    }

    // ------------------------------------------------------------------------

    private EventData createEventRecord(Device device, 
        long     fixtime,
        int      statusCode,
        GeoPoint geoPoint, 
        long     gpioInput,
        double   speedKPH, double heading, 
        double   altitude,
        double   odomKM)
    {
        String accountID    = (device != null)? device.getAccountID() : "";
        String deviceID     = (device != null)? device.getDeviceID()  : "";
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeoPoint(geoPoint);
        evdb.setInputMask(gpioInput);
        evdb.setHeading(heading);
        evdb.setSpeedKPH(speedKPH);
        evdb.setAltitude(altitude);
        evdb.setOdometerKM(odomKM);
        return evdb;
    }

    /* create and insert an event record */
    private void insertEventRecord(Device device, 
        long     fixtime, int statusCode, Geozone geozone,
        GeoPoint geoPoint, 
        long     gpioInput,
        double   speedKPH, double heading, 
        double   altitude,
        double   odomKM)
    {

        /* create event */
        EventData evdb = createEventRecord(device, fixtime, statusCode, geoPoint, gpioInput, speedKPH, heading, altitude, odomKM);

        /* insert event */
        // this will display an error if it was unable to store the event
        Print.logInfo("Event     : [0x" + StringTools.toHexString(statusCode,16) + "] " + StatusCodes.GetDescription(statusCode,null));
        if (device != null) {
            device.insertEventData(evdb);
        }
        this.eventTotalCount++;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void configInit() 
    {
        DCServerConfig dcsc     = Main.getServerConfig();

        // common
        UNIQUEID_PREFIX         = dcsc.getUniquePrefix();
        MINIMUM_SPEED_KPH       = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
        ESTIMATE_ODOMETER       = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
        SIMEVENT_GEOZONES       = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
        MINIMUM_MOVED_METERS    = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);

    }

    /* debug entry point */
    // Debug purposes only, not used for production
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,false);  // main
        TrackClientPacketHandler.configInit();
        TrackServer.configInit();

        /* create client packet handler */
        TrackClientPacketHandler tcph = new TrackClientPacketHandler();

        /* parse sample data */
        String r[] = new String[] {
            ">RPV21305+3958635-1414085300000012;ID=0011<",
            ">RPV15714+3739438-1420384601512612;ID=1234;*7F<"
        };
        for (int i = 0; i < r.length; i++) {
            tcph.getHandlePacket(r[i].getBytes());
        }

    }

}
