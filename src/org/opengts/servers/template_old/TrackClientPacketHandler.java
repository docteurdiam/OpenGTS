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
//  Template data packet 'business' logic.
//  This module is an *example* of how client data packets can be parsed and 
//  inserted into the EventData table.  Since every device protocol is different,
//  significant changes will likely be necessary to support the protocol used by
//  your chosen device.
// ----------------------------------------------------------------------------
// Notes:
// - See the OpenGTS_Config.pdf document for additional information regarding the
//   implementation of a device communication server.
// - Implementing a device communication server for your chosen device may take a 
//   signigicant and substantial amount of programming work to accomplish, depending 
//   on the device protocol.  To implement a server, you will likely need an in-depth 
//   understanding of TCP/UDP based communication, and a good understanding of Java 
//   programming techniques, including socket communication and multi-threading. 
// - The first and most important step when starting to implement a device 
//   communication server for your chosen device is to obtain and fully understand  
//   the protocol documentation from the manufacturer of the device.  Attempting to 
//   reverse-engineer a raw-socket base protocol can prove extremely difficult, if  
//   not impossible, without proper protocol documentation.
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Change History:
//  2006/06/30  Martin D. Flynn
//     -Initial release
//  2007/07/27  Martin D. Flynn
//     -Moved constant information to 'Constants.java'
//  2007/08/09  Martin D. Flynn
//     -Added additional help/comments.
//     -Now uses "imei_" as the primary IMEI prefix for the unique-id when
//      looking up the Device record (for data format example #1)
//     -Added a second data format example (#2) which includes the parsing of a
//      standard $GPRMC NMEA-0183 record.
//  2008/02/17  Martin D. Flynn
//     -Added additional help/comments.
//  2008/03/12  Martin D. Flynn
//     -Added ability to compute a GPS-based odometer value
//     -Added ability to update the Device record with IP-address and last connect time.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/06/20  Martin D. Flynn
//     -Added some additional comments regarding the use of 'terminate' and 'terminateSession()'.
//  2008/12/01  Martin D. Flynn
//     -Added entry point for parsing GPS packet data store in a flat file.
//  2009/04/02  Martin D. Flynn
//     -Changed default for 'INSERT_EVENT' to true
//  2009/05/27  Martin D. Flynn
//     -Added changes for estimated odometer calculations, and simulated geozones
//  2009/06/01  Martin D. Flynn
//     -Updated to utilize Device gerozone checks
//  2009/08/07  Martin D. Flynn
//     -Saved older version of "template" server 
//      (will be removed in a future release)
// ----------------------------------------------------------------------------
package org.opengts.servers.template_old;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{
   
    // ------------------------------------------------------------------------
    // This data parsing template contains *examples* of 2 different ASCII data formats:
    // Example #1: (see "parseInsertRecord_ASCII_1")
    //   123456789012345,2006/09/05,07:47:26,35.3640,-141.2958,27.0,224.8
    // Example #2: (see "parseInsertRecord_ASCII_2")
    //   account/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
    //
    // These are only *examples* of an ASCII encoded data protocol.  Since this 'template'
    // cannot anticipate every possible ASCII/Binary protocol that may be encounted, this
    // module should only be used as an *example* of how a device communication server might
    // be implemented.  The implementation of a device communication server for your chosen
    // device may take a signigicant and substantial amount of programming work to accomplish, 
    // depending on the device protocol.

    private static int DATA_FORMAT_OPTION               = 1;
    
    // ------------------------------------------------------------------------

    /* estimate GPS-based odometer */
    // (enable to include estimated GPS-based odometer values on EventData records)
    // Note:
    //  - Enabling this feature may cause an extra query to the EventData table to obtain
    //    the previous EventData record, from which it will calculate the distance between
    //    this prior point and the current point.  This means that the GPS "dithering"
    //    thich can occur when a vehicle is stopped will cause the calculated odometer 
    //    value to increase even when the vehicle is not moving.  You may wish to add some
    //    additional logic to mitigate this particular behavior.  
    //  - The accuracy of a GPS-based odometer calculation varies greatly depending on 
    //    factors such as the accuracy of the GPS receiver (ie. WAAS, DGPS, etc), the time
    //    interval between generated "in-motion" events, and how straight or curved the
    //    road is.  Typically, a GPS-based odometer tends to under-estimate the actual
    //    vehicle value.
    private static final boolean ESTIMATE_ODOMETER      = false;
    
    /* simulate geozone arrival/departure */
    // (enable to insert simulated Geozone arrival/departure EventData records)
    public  static final boolean SIMEVENT_GEOZONES      = false;

    /* flag indicating whether data should be inserted into the DB */
    // should be set to 'true' for production.
    private static       boolean DFT_INSERT_EVENT       = true;
    private static       boolean INSERT_EVENT           = DFT_INSERT_EVENT;

    /* update Device record */
    // (enable to update Device record with current IP address and last connect time)
    private static final boolean UPDATE_DEVICE          = false;

    // ------------------------------------------------------------------------

    /* Ingore $GPRMC checksum? */
    // (only applicable for data formats that include NMEA-0183 formatted event records)
    private static       boolean IGNORE_NMEA_CHECKSUM   = false;

    // ------------------------------------------------------------------------

    /* GMT/UTC timezone */
    private static final TimeZone gmtTimezone           = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* session IP address */
    // These values will be set for you by the incoming session to indicate the 
    // originating IP address.
    private String          ipAddress                   = null;
    private int             clientPort                  = 0;

    /* packet handler constructor */
    public TrackClientPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    // this method is called at the beginning of a communication session
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);
        super.clearTerminateSession();

        /* init */
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;
        this.clientPort       = this.getSessionInfo().getRemotePort();

    }
    
    /* callback when session is terminating */
    // this method is called at the end of a communication session
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);
    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        // (This method is only called if "Constants.ASCII_PACKETS" is false!)
        //
        // This method is possibly the most important part of a server protocol implementation.
        // The length of the incoming client packet must be correctly identified in order to 
        // know how many incoming packet bytes should be read.
        //
        // 'packetLen' will be the value specified by Constants.MIN_PACKET_LENGTH, and should
        // be the minimum number of bytes required (but not more) to accurately determine what
        // the total length of the incoming client packet will be.  After analyzing the initial 
        // bytes of the packet, this method should return what it beleives to be the full length 
        // of the client data packet, including the length of these initial bytes.
        //
        // For example:
        //   Assume that all client packets have the following binary format:
        //      Byte  0    - packet type
        //      Byte  1    - payload length (ie packet data)
        //      Bytes 2..X - payload data (as determined by the payload length byte)
        // In this case 'Constants.ASCII_PACKETS' should be set to 'false', and 
        // 'Constants.MIN_PACKET_LENGTH' should be set to '2' (the minimum number of bytes
        // required to determine the actual packet length).  This method should then return
        // the following:
        //      return 2 + ((int)packet[1] & 0xFF);
        // Which is the packet header length (2 bytes) plus the remaining length of the data
        // payload found in the second byte of the packet header. 
        // 
        // Note that the integer cast and 0xFF mask is very important.  'byte' values in
        // Java are signed, thus the byte 0xFF actually represents a '-1'.  So if the packet
        // payload length is 128, then without the (int) cast and mask, the returned value
        // would end up being -126.
        // IE:
        //    byte b = (byte)128;  // this is actually a signed '-128'
        //    System.out.println("1: " + (2+b)); // this casts -128 to an int, and adds 2
        //    System.out.println("2: " + (2+((int)b&0xFF)));
        // The above would print the following:
        //    1: -126
        //    2: 130
        //
        // Once the full client packet is read, it will be delivered to the 'getHandlePacket'
        // method below.
        //
        // WARNING: If a packet length value is returned here that is greater than what the
        // client device will actually be sending, then the server will receive a read timeout,
        // and this error may cause the socket connection to be closed.  If you happen to see
        // read timeouts occuring during testing/debugging, then it is likely that this method
        // needs to be adjusted to properly identify the client packet length.
        //
        if (Constants.ASCII_PACKETS) {
            // (this actually won't be called if 'Constants.ASCII_PACKETS' is true).
            // ASCII packets - look for line terminator [see Constants.ASCII_LINE_TERMINATOR)]
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;  // read until line termination character
            //return ServerSocketThread.PACKET_LEN_END_OF_STREAM;  // read until end of stream, or maxlen
        } else {
            // BINARY packet - need to analyze 'packet[]' and determine actual packet length
            return -1; // <-- change this for binary packets
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the initial packet sent to the device after session is open */
    public byte[] getInitialPacket() 
        throws Exception
    {
        // At this point a connection from the client to the server has just been
        // initiated, and we have not yet received any data from the client.
        // If the client is expecting to receive an initial packet from the server at
        // the time that the client connects, then this is where the server can return
        // a byte array that will be transmitted to the client device.
        return null;
        // Note: any returned response for "getInitialPacket()" is ignored for simplex/udp connections.
        // Returned UDP packets may be sent from "getHandlePacket" or "getFinalPacket".
    }

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        // After determining the length of a client packet (see method 'getActualPacketLength'),
        // this method is called with the single packet which has been read from the client.
        // It is the responsibility of this method to determine what type of packet was received
        // from the client, parse/insert any event data into the tables, and return any expected 
        // response that the client may be expected in the form of a byte array.
        if ((pktBytes != null) && (pktBytes.length > 0)) {
            Print.logInfo("Recv[HEX]: " + StringTools.toHexString(pktBytes));
            String s = StringTools.toStringValue(pktBytes).trim(); // remove leading/trailing spaces
            Print.logInfo("Recv[TXT]: " + s); // debug message
            switch (DATA_FORMAT_OPTION) {
                case 1 : this.parseInsertRecord_ASCII_1(s); break;
                case 2 : this.parseInsertRecord_ASCII_2(s); break;
                default: Print.logError("Unspecified data format"); break;
            }
            // If the client is expecting to receive a response from the server (such as an
            // acknowledgement), this is where the server should compose a returned response
            // in the form of an array of bytes which should be returned here.  This byte array
            // will then be transmitted back to the client.
            return null; // no return packets are expected
        } else {
            Print.logInfo("Empty packet received ...");
            return null; // no return packets are expected
        }
        // when this method returns, the server framework then starts the process over again
        // attempting to read another packet from the client device (see method 'getActualPacketLength').
        // If this server determines that communicqtion with the client device has completed, then
        // the "terminateSession" method should return true.
    }

    /* final packet sent to device before session is closed */
    public byte[] getFinalPacket(boolean hasError) 
        throws Exception
    {
        // If the server wishes to send a final packet to the client just before the connection
        // is closed, then this is where the server should compose the final packet, and return
        // this packet in the form of a byte array.  This byte array will then be transmitted
        // to the client device before the session is closed.
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private void _insertEventData(Device device, 
        long fixtime, 
        int statusCode, 
        GeoPoint geoPoint,
        double speedKPH, double heading,
        double altitudeM, 
        double odomKM)
        {
        if (INSERT_EVENT) {
            String accountID = device.getAccountID();
            String deviceID  = device.getDeviceID();
            EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
            EventData evdb = evKey.getDBRecord();
            evdb.setGeoPoint(geoPoint);
            evdb.setHeading(heading);
            evdb.setSpeedKPH(speedKPH);
            evdb.setOdometerKM(odomKM);
            evdb.setAltitude(altitudeM);
            device.insertEventData(evdb); // displays error if unable to insert event
        }
    }

    private void _updateDevice(Device device, DataTransport dataXPort)
    {
        if (UPDATE_DEVICE) {
            device.setIpAddressCurrent(this.ipAddress); // FLD_ipAddressCurrent
            device.setRemotePortCurrent(this.clientPort); // FLD_remotePortCurrent
            device.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
            if (!dataXPort.getDeviceCode().equalsIgnoreCase(Constants.DEVICE_CODE)) {
                dataXPort.setDeviceCode(Constants.DEVICE_CODE); // FLD_deviceCode
            }
            try {
                device.update(
                    Device.FLD_deviceCode,
                    Device.FLD_ipAddressCurrent,
                    Device.FLD_remotePortCurrent,
                    Device.FLD_lastValidLatitude,
                    Device.FLD_lastValidLongitude,
                    Device.FLD_lastGPSTimestamp,
                    Device.FLD_lastOdometerKM,
                    Device.FLD_lastTotalConnectTime
                );
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + device.getAccountID() + "/" + device.getDeviceID(), dbe);
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private boolean parseInsertRecord_ASCII_1(String s)
    {
        // This is an example showing how the server might parse one type of ASCII encoded data.
        // Since every device utilizes a different data format, this will likely not match the
        // format coming from your chosen device and may need some significant changes to support
        // the format provided by your device (assuming that the format is even ASCII).
        
        // This parsing method assumes the data format appears as follows:
        //      0             1        2       3         4       5     6  
        // <IMEI number>,2006/09/05,07:47:26,35.3640,-141.2958,27.0,224.8
        //   0 - IMEI unique ID
        //   1 - Date [GMT]
        //   2 - Time [GMT]
        //   3 - Latitude
        //   4 - Longitude
        //   5 - Speed (kph)
        //   6 - Heading (degrees)
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return false;
        }

        /* parse to fields */
        String fld[] = StringTools.parseString(s, ',');
        if ((fld == null) || (fld.length < 7)) {
            Print.logWarn("Invalid number of fields");
            return false;
        }

        /* parse individual fields */
        String imei       = fld[0].toLowerCase();
        long   fixtime    = this._parseDate(fld[1],fld[2]);
        int    statusCode = StatusCodes.STATUS_LOCATION;
        double latitude   = StringTools.parseDouble(fld[3],0.0);
        double longitude  = StringTools.parseDouble(fld[4],0.0);
        double speedKPH   = StringTools.parseDouble(fld[5],0.0);
        double heading    = StringTools.parseDouble(fld[6],0.0);
        double altitudeM  = 0.0;  // 
        
        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[1] + "/" + fld[2]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }
                
        /* valid lat/lon? */
        if (!GeoPoint.isValid(latitude, longitude)) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
        }

        /* GeoPoint */
        GeoPoint geoPoint = new GeoPoint(latitude, longitude); // may be invalid

        /* minimum speed */
        if (speedKPH < Constants.MINIMUM_SPEED_KPH) {
            speedKPH = 0.0;
            heading  = 0.0;
        }

        /* display parsed data */
        Print.logInfo("IMEI     : " + imei);
        Print.logInfo("Fixtime  : " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GPS      : " + geoPoint);
        Print.logInfo("Speed    : " + StringTools.format(speedKPH,"#0.0") + " kph " + heading);

        /* find Device */
        Device device    = null;
        String accountID = null;
        String deviceID  = null;
        String uniqueID  = Constants.UNIQUE_ID_PREFIX_IMEI + imei;
        try {
            device = Transport.loadDeviceByUniqueID(uniqueID);
            if (device == null) {
                String uniqueID_alt = Constants.UNIQUE_ID_PREFIX_ALT + imei;
                device = Transport.loadDeviceByUniqueID(uniqueID_alt);
                if (device == null) {
                    Print.logWarn("IMEI ID not found!: " + uniqueID); // <== display main key
                    return false;
                }
                uniqueID = uniqueID_alt;
            }
            accountID = device.getAccountID();
            deviceID  = device.getDeviceID();
        } catch (DBException dbe) {
            Print.logError("Exception getting Device: " + uniqueID + " [" + dbe + "]");
            return false;
        }
        DataTransport dataXPort = device.getDataTransport();
        Print.logInfo("DeviceID : " + accountID + "/" + deviceID);

        /* validate source IP address */
        if ((this.ipAddress != null) && !dataXPort.isValidIPAddress(this.ipAddress)) {
            Print.logError("Invalid IP Address for device: " + this.ipAddress + 
                " [expecting " + dataXPort.getIpAddressValid() + "]");
            return false;
        }

        /* estimate GPS-based odometer */
        double odomKM = 0.0; // set to available odometer from event record
        if (odomKM <= 0.0) {
            odomKM = (ESTIMATE_ODOMETER && geoPoint.isValid())? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            odomKM = device.adjustOdometerKM(odomKM);
        }
        Print.logInfo("OdomKM    : " + odomKM);

        /* count generated events */
        int eventCount = 0;

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && geoPoint.isValid()) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    this._insertEventData(device, 
                        z.getTimestamp(), z.getStatusCode(), 
                        geoPoint, speedKPH, heading, altitudeM, odomKM);
                    Print.logInfo("Geozone    : " + z);
                    eventCount++;
                }
            }
        }

        /* create/insert event */
        this._insertEventData(device, 
            fixtime, statusCode, 
            geoPoint, speedKPH, heading, altitudeM, odomKM);
        eventCount++;

        /* save device changes */
        this._updateDevice(device, dataXPort);

        /* return success */
        return true;
        
    }

    /* parse the specified date into unix 'epoch' time */
    private long _parseDate(String ymd, String hms)
    {
        // "YYYY/MM/DD", "hh:mm:ss"
        String d[] = StringTools.parseString(ymd,"/");
        String t[] = StringTools.parseString(hms,":");
        if ((d.length != 3) && (t.length != 3)) {
            //Print.logError("Invalid date: " + ymd + ", " + hms);
            return 0L;
        } else {
            int YY = StringTools.parseInt(d[0],0); // 07 year
            int MM = StringTools.parseInt(d[1],0); // 04 month
            int DD = StringTools.parseInt(d[2],0); // 18 day
            int hh = StringTools.parseInt(t[0],0); // 01 hour
            int mm = StringTools.parseInt(t[1],0); // 48 minute
            int ss = StringTools.parseInt(t[2],0); // 04 second
            if (YY < 100) { YY += 2000; }
            DateTime dt = new DateTime(gmtTimezone,YY,MM,DD,hh,mm,ss);
            return dt.getTimeSec();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private boolean parseInsertRecord_ASCII_2(String s)
    {
        // This is an example showing how the server might parse one type of ASCII encoded data.
        // Since every device utilizes a different data format, this will likely not match the
        // format coming from your chosen device and may need some significant changes to support
        // the format provided by your device (assuming that the format is even ASCII).

        // This parsing method assumes the data format appears as follows:
        //      0          1      2 ...
        // <AccountID>/<DeviceID>/$GPRMC,025423.494,A,3709.0642,N,11907.8315,W,0.094824,108.52,200505,,*12
        //   0 - Account ID
        //   1 - Device ID
        //   2 - $GPRMC record ...
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return false;
        }

        /* parse to fields */
        String fld[] = StringTools.parseString(s, '/');
        if ((fld == null) || (fld.length < 3)) {
            Print.logWarn("Invalid number of fields");
            return false;
        }

        /* parse individual fields */
        String   accountID  = fld[0].toLowerCase();
        String   deviceID   = fld[1].toLowerCase();
        Nmea0183 gprmc      = new Nmea0183(fld[2], IGNORE_NMEA_CHECKSUM);
        long     fixtime    = gprmc.getFixtime();
        int      statusCode = StatusCodes.STATUS_LOCATION;
        double   latitude   = gprmc.getLatitude();
        double   longitude  = gprmc.getLongitude();
        double   speedKPH   = gprmc.getSpeedKPH();
        double   heading    = gprmc.getHeading();
        GeoPoint geoPoint   = new GeoPoint(latitude, longitude);
        double   altitudeM  = 0.0;  // 

        /* no deviceID? */
        if (deviceID.equals("")) {
            Print.logWarn("DeviceID not specified!");
            return false;
        }

        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date/time");
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* minimum speed */
        if (speedKPH < Constants.MINIMUM_SPEED_KPH) {
            speedKPH = 0.0;
            heading  = 0.0;
        }

        /* display parsed data */
        Print.logInfo("Fixtime  : " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GPS      : " + new GeoPoint(latitude,longitude));
        Print.logInfo("Speed    : " + StringTools.format(speedKPH,"#0.0") + " kph " + heading);

        /* get Device */
        Device device = null;
        try {
            if (StringTools.isBlank(accountID)) {
                String uniqueID = Constants.UNIQUE_ID_PREFIX_IMEI + deviceID;
                device = Transport.loadDeviceByUniqueID(uniqueID);
                if (device == null) {
                    String uniqueID_alt = Constants.UNIQUE_ID_PREFIX_ALT + deviceID;
                    device = Transport.loadDeviceByUniqueID(uniqueID_alt);
                    if (device == null) {
                        Print.logWarn("UniqueID not found!: " + uniqueID); // <== display main key
                        return false;
                    }
                    uniqueID = uniqueID_alt;
                }
                accountID = device.getAccountID();
                deviceID  = device.getDeviceID();
            } else {
                device = Transport.loadDeviceByTransportID(Account.getAccount(accountID), deviceID);
                if (device == null) {
                    Print.logWarn("Device not found!: " + accountID + "/" + deviceID);
                    return false;
                }
            }
        } catch (DBException dbe) {
            Print.logError("Exception getting Device: " + accountID + "/" + deviceID + " [" + dbe + "]");
            return false;
        }
        DataTransport dataXPort = device.getDataTransport();
        Print.logInfo("DeviceID : " + accountID + "/" + deviceID);

        /* validate source IP address */
        if ((this.ipAddress != null) && !dataXPort.isValidIPAddress(this.ipAddress)) {
            Print.logError("Invalid IP Address for device: " + this.ipAddress + 
                " [expecting " + dataXPort.getIpAddressValid() + "]");
            return false;
        }

        /* estimate GPS-based odometer */
        double odomKM = 0.0; // set to available odometer from event record
        if (odomKM <= 0.0) {
            odomKM = (ESTIMATE_ODOMETER && geoPoint.isValid())? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            odomKM = device.adjustOdometerKM(odomKM);
        }
        Print.logInfo("OdomKM    : " + odomKM);

        /* count generated events */
        int eventCount = 0;

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && geoPoint.isValid()) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    this._insertEventData(device, 
                        z.getTimestamp(), z.getStatusCode(), 
                        geoPoint, speedKPH, heading, altitudeM, odomKM);
                    Print.logInfo("Geozone    : " + z);
                    eventCount++;
                }
            }
        }

        /* create/insert event */
        this._insertEventData(device, 
            fixtime, statusCode, 
            geoPoint, speedKPH, heading, altitudeM, odomKM);
        eventCount++;

        /* save device changes */
        this._updateDevice(device, dataXPort);

        /* return success */
        return true;
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Once you have modified this example 'template' server to parse your particular
    // device packets, you can also use this source module to load GPS data packets
    // which have been saved in a file.  To run this module to load your save GPS data
    // packets, start this command as follows:
    //   java -cp <classpath> org.opengts.servers.template.TrackClientPacketHandler {options}
    // Where your options are one or more of 
    //   -insert=[true|false]    Insert parse records into EventData
    //   -format=[1|2]           Data format
    //   -debug                  Parse internal sample data
    //   -parseFile=<file>       Parse data from specified file

    private static int _usage()
    {
        String cn = StringTools.className(TrackClientPacketHandler.class);
        Print.sysPrintln("Test/Load Device Communication Server");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  $JAVA_HOME/bin/java -classpath <classpath> %s {options}", cn);
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -insert=[true|false]    Insert parse records into EventData");
        Print.sysPrintln("  -format=[1|2]           Data format");
        Print.sysPrintln("  -debug                  Parse internal sample/debug data (if any)");
        Print.sysPrintln("  -parseFile=<file>       Parse data from specified file");
        return 1;
    }

    /* debug entry point (does not return) */
    public static int _main(boolean fromMain)
    {

        /* default options */
        INSERT_EVENT = RTConfig.getBoolean(Main.ARG_INSERT, DFT_INSERT_EVENT);
        if (!INSERT_EVENT) {
            Print.sysPrintln("Warning: Data will NOT be inserted into the database");
        }

        /* data format type */
        DATA_FORMAT_OPTION = RTConfig.getInt(Main.ARG_FORMAT, 1);

        /* create client packet handler */
        TrackClientPacketHandler tcph = new TrackClientPacketHandler();

        /* DEBUG sample data */
        if (RTConfig.getBoolean(Main.ARG_DEBUG,false)) {
            String data[] = null;
            switch (DATA_FORMAT_OPTION) {
                case  1: data = new String[] {
                    "123456789012345,2006/09/05,07:47:26,35.3640,-142.2958,27.0,224.8",
                }; break;
                case  2: data = new String[] {
                    "account/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,12.09,108.52,200505,,*2E",
                    "/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,12.09,108.52,200505,,*2E",
                }; break;
                default:
                    Print.sysPrintln("Unrecognized Data Format: %d", DATA_FORMAT_OPTION);
                    return _usage();
            }
            for (int i = 0; i < data.length; i++) {
                tcph.getHandlePacket(data[i].getBytes());
            }
            return 0;
        }

        /* 'parseFile' specified? */
        if (RTConfig.hasProperty(Main.ARG_PARSEFILE)) {

            /* get input file */
            File parseFile = RTConfig.getFile(Main.ARG_PARSEFILE,null);
            if ((parseFile == null) || !parseFile.isFile()) {
                Print.sysPrintln("Data source file not specified, or does not exist.");
                return _usage();
            }

            /* open file */
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(parseFile);
            } catch (IOException ioe) {
                Print.logException("Error openning input file: " + parseFile, ioe);
                return 2;
            }

            /* loop through file */
            try {
                // records are assumed to be terminated by CR/NL 
                for (;;) {
                    String data = FileTools.readLine(fis);
                    if (!StringTools.isBlank(data)) {
                        tcph.getHandlePacket(data.getBytes());
                    }
                }
            } catch (EOFException eof) {
                Print.sysPrintln("");
                Print.sysPrintln("***** End-Of-File *****");
            } catch (IOException ioe) {
                Print.logException("Error reaading input file: " + parseFile, ioe);
            } finally {
                try { fis.close(); } catch (Throwable th) {/* ignore */}
            }

            /* done */
            return 0;

        }

        /* no options? */
        return _usage();

    }
    
    /* debug entry point */
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,false);
        System.exit(TrackClientPacketHandler._main(false));
    }
    
}
