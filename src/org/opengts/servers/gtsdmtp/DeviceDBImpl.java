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
//     -Integrate DBException
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -'insertEvent' now transfers temperature information as well.
//  2007/02/26  Martin D. Flynn
//     -Added 'FLD_odometerKM' support
//  2007/02/28  Martin D. Flynn
//     -Populate FLD_entity filed in EventData
//  2007/07/14  Martin D. Flynn
//     -Added support for FLD_sensorLow/FLD_sensorHigh
//  2007/07/27  Martin D. Flynn
//     -Repackaged to "org.opengts.servers.gtsdmtp"
//  2007/09/16  Martin D. Flynn
//     -Moved 'FLD_driver' into EventData.INCLUDE_CUSTOM_FIELDS data block
//     -Added support for 'FLD_gpsAge' and 'FLD_horzAccuracy'.
//     -Added handlers for client device errors, diagnostics, and properties.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2009/05/01  Martin D. Flynn
//     -Modified to update only changed DataTransport fields.
//     -Save current IP address
//  2009/05/24  Martin D. Flynn
//     -Added support for optional simulated geozone arrive/depart.
//  2009/08/07  Martin D. Flynn
//     -Changed DEVICE_CODE value from "dmtp" to "gtsdmtp".
//  2009/10/02  Martin D. Flynn
//     -Added call to Device "updateChangedEventFields" method (fixed geozone
//      arrive/depart detection).
//  2010/01/29  Martin D. Flynn
//     -Added oil pressure data transfer to EventData.
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

import org.opendmtp.codes.ServerErrors;
import org.opendmtp.codes.ClientErrors;
import org.opendmtp.codes.CommandErrors;
import org.opendmtp.server.db.DeviceDB;
import org.opendmtp.server.db.PayloadTemplate;
import org.opendmtp.server.base.DMTPGeoEvent;
import org.opendmtp.server.base.Packet;
import org.opendmtp.server.base.PacketList;
import org.opendmtp.server.base.PacketParseException;

public class DeviceDBImpl
    implements DeviceDB
{

    // ------------------------------------------------------------------------

    public static final String  DEVICE_CODE         = DCServerFactory.OPENDMTP_NAME;

    // ------------------------------------------------------------------------

    public static       boolean SIMEVENT_GEOZONES   = false;

    public static void configInit()
    {
        DCServerConfig dcs = Main.getServerConfig();
        SIMEVENT_GEOZONES = dcs.getSimulateGeozones(SIMEVENT_GEOZONES);
        Print.logWarn("Simulated Geozone detection enabled == " + SIMEVENT_GEOZONES);
    }

    // ------------------------------------------------------------------------

    private Device              device              = null;
    private DataTransport       dataXPort           = null;
    private Set<String>         devFields           = null;
    
    public DeviceDBImpl(Device dev) 
    {
        this.device    = dev; // never null
        this.dataXPort = this.device.getDataTransport(); // never null
        // make sure that the device indicates that it supports DMTP
        if (!this.dataXPort.getSupportsDMTP()) {
            this.dataXPort.setSupportsDMTP(true); // will be saved later
            this._addUpdateField(Device.FLD_supportsDMTP);
        }
        if (!this.dataXPort.getDeviceCode().equalsIgnoreCase(DEVICE_CODE)) {
            this.dataXPort.setDeviceCode(DEVICE_CODE); // will be saved later
            this._addUpdateField(Device.FLD_deviceCode);
        }
    }

    // ------------------------------------------------------------------------

    private void _addUpdateField(String fldName)
    {
        if (this.devFields == null) {
            this.devFields = new HashSet<String>();
        }
        this.devFields.add(fldName);
    }

    // ------------------------------------------------------------------------

    /* not part of the DeviceDB interface! */
    public Device getDevice()
    {
        return this.device;
    }
    
    /* not part of the DeviceDB interface! */
    public Account getAccount()
    {
        return this.getDevice().getAccount();
    }
    
    // ------------------------------------------------------------------------

    public String getAccountName() 
    {
        return this.device.getAccountID();
    }
        
    public String getDeviceName() 
    {
        return this.device.getDeviceID();
    }
    
    public String getDescription() 
    {
        return this.device.getDescription();
    }
    
    public boolean isActive() 
    {
        return this.device.getIsActive();
    }
    
    public boolean isValidIpAddress(String ipAddr) 
    {
        if (this.dataXPort.isValidIPAddress(ipAddr)) {
            this.dataXPort.setIpAddressCurrent(ipAddr);
            this._addUpdateField(Device.FLD_ipAddressCurrent);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    public int getMaxAllowedEvents()
    {
        return this.dataXPort.getMaxAllowedEvents();
    }
    
    public long getEventCount(long timeStart, long timeEnd)
    {
        try {
            return this.device.getEventCount(timeStart, timeEnd);
        } catch (DBException dbe) {
            dbe.printException();
            return -1L;
        }
    }
    
    // ------------------------------------------------------------------------

    public int getLimitTimeIntervalMinutes()
    {
        return this.dataXPort.getUnitLimitInterval(); // Minutes
    }
    
    // ------------------------------------------------------------------------

    public int getMaxTotalConnections() 
    {
        return this.dataXPort.getTotalMaxConn();
    }

    public int getMaxTotalConnectionsPerMinute() 
    {
        return this.dataXPort.getTotalMaxConnPerMin();
    }

    public byte[] getTotalConnectionProfile()
    {
        DTProfileMask v = this.dataXPort.getTotalProfileMask();
        return (v != null)? v.getByteMask() : new byte[0];
    }

    public void setTotalConnectionProfile(byte[] profile) 
    {
        DTProfileMask mask = new DTProfileMask(profile);
        mask.setLimitTimeInterval(this.getLimitTimeIntervalMinutes());
        this.dataXPort.setTotalProfileMask(mask);
        this._addUpdateField(Device.FLD_totalProfileMask);
    }

    public long getLastTotalConnectionTime()
    {
        return this.dataXPort.getLastTotalConnectTime();
    }

    public void setLastTotalConnectionTime(long time)
    {
        this.dataXPort.setLastTotalConnectTime(time);
        this._addUpdateField(Device.FLD_lastTotalConnectTime);
    }

    // ------------------------------------------------------------------------

    /* return the maximum number of allowed duplex connections per interval */
    public int getMaxDuplexConnections() 
    {
        return this.dataXPort.getDuplexMaxConn();
    }
    
    /* return the maximum number of allowed duplex connections per minute */
    public int getMaxDuplexConnectionsPerMinute()
    {
        return this.dataXPort.getDuplexMaxConnPerMin();
    }
    
    /* return the duplex connection profile */
    public byte[] getDuplexConnectionProfile()
    {
        DTProfileMask v = this.dataXPort.getDuplexProfileMask();
        return (v != null)? v.getByteMask() : new byte[0];
    }
    
    /* set the duplex connection profile */
    public void setDuplexConnectionProfile(byte[] profile)
    {
        DTProfileMask mask = new DTProfileMask(profile);
        mask.setLimitTimeInterval(this.getLimitTimeIntervalMinutes());
        this.dataXPort.setDuplexProfileMask(mask);
        this._addUpdateField(Device.FLD_duplexProfileMask);
    }

    /* return the last duplex connection time */
    public long getLastDuplexConnectionTime()
    {
        return this.dataXPort.getLastDuplexConnectTime();
    }

    /* set the last duplex connection time */
    public void setLastDuplexConnectionTime(long time)
    {
        this.dataXPort.setLastDuplexConnectTime(time);
        this._addUpdateField(Device.FLD_lastDuplexConnectTime);
    }

    // ------------------------------------------------------------------------

    /* return true if the specified encoding is supported */
    public boolean supportsEncoding(int encoding)
    {
        // 'encoding' is a mask containing one (or more) of the following:
        //    Encoding.SUPPORTED_ENCODING_BINARY
        //    Encoding.SUPPORTED_ENCODING_BASE64
        //    Encoding.SUPPORTED_ENCODING_HEX
        //    Encoding.SUPPORTED_ENCODING_CSV
        int vi = this.dataXPort.getSupportedEncodings();
        return ((vi & encoding) != 0);
    }
    
    /* remove the specified encoding from the list of supported encodings */
    public void removeEncoding(int encoding) 
    {
        int vi = this.dataXPort.getSupportedEncodings();
        if ((vi & encoding) != 0) {
            vi &= ~encoding;
            this.dataXPort.setSupportedEncodings(vi);
            this._addUpdateField(Device.FLD_supportedEncodings);
        }
    }
    
    // ------------------------------------------------------------------------

    /* add a new payload template for this device */
    public boolean addClientPayloadTemplate(PayloadTemplate template)
    {
        return EventTemplate.SetPayloadTemplate(
            this.getAccountName(),
            this.getDeviceName(),
            template);
    }

    /* return the specified payload template for this device, or null if no template exists */
    public PayloadTemplate getClientPayloadTemplate(int custType) 
    {
        return EventTemplate.GetPayloadTemplate(
            this.getAccountName(),
            this.getDeviceName(),
            custType);
    }
    
    // ------------------------------------------------------------------------

    /* insert DMTP event into EventData table for this device */
    public int insertEvent(DMTPGeoEvent geoEvent) 
    {
        long   timestamp  = geoEvent.getTimestamp();
        int    statusCode = geoEvent.getStatusCode();
        String accountID  = this.getAccountName();
        String deviceID   = this.getDeviceName();

        /* create key */
        EventData.Key evKey = new EventData.Key(
            accountID,
            deviceID,
            timestamp,
            statusCode);

        /* populate record */
        EventData evdb = evKey.getDBRecord();
        
        // Standard fields
        evdb.setFieldValue(EventData.FLD_dataSource     , geoEvent.getDataSource());
        evdb.setFieldValue(EventData.FLD_rawData        , geoEvent.getRawData());
        evdb.setFieldValue(EventData.FLD_latitude       , geoEvent.getLatitude(0));
        evdb.setFieldValue(EventData.FLD_longitude      , geoEvent.getLongitude(0));
        evdb.setFieldValue(EventData.FLD_gpsAge         , geoEvent.getGpsAge());
        evdb.setFieldValue(EventData.FLD_horzAccuracy   , geoEvent.getHorizontalAccuracy());
        evdb.setFieldValue(EventData.FLD_speedKPH       , geoEvent.getSpeed());
        evdb.setFieldValue(EventData.FLD_heading        , geoEvent.getHeading());
        evdb.setFieldValue(EventData.FLD_altitude       , geoEvent.getAltitude());
        evdb.setFieldValue(EventData.FLD_geozoneIndex   , geoEvent.getGeofence(0));

        // Odometer/Distance
        double distanceKM = geoEvent.getDistance();
        double odometerKM = geoEvent.getOdometer();
        evdb.setFieldValue(EventData.FLD_distanceKM     , distanceKM);
        evdb.setFieldValue(EventData.FLD_odometerKM     , ((odometerKM != 0.0)? odometerKM : distanceKM));

        // Misc fields
        evdb.setFieldValue(EventData.FLD_driverID       , geoEvent.getEntity(1));
        evdb.setFieldValue(EventData.FLD_entityID       , geoEvent.getEntity(0));
      //evdb.setFieldValue(EventData.FLD_topSpeedKPH    , geoEvent.getTopSpeed());
        evdb.setFieldValue(EventData.FLD_sensorLow      , geoEvent.getSensorLow(0));
        evdb.setFieldValue(EventData.FLD_sensorHigh     , geoEvent.getSensorHigh(0));
        evdb.setFieldValue(EventData.FLD_brakeGForce    , geoEvent.getBrakeGForce());
        
        // Temperature fields
        evdb.setFieldValue(EventData.FLD_thermoAverage0 , geoEvent.getTemeratureAverage(0));
        evdb.setFieldValue(EventData.FLD_thermoAverage1 , geoEvent.getTemeratureAverage(1));
        evdb.setFieldValue(EventData.FLD_thermoAverage2 , geoEvent.getTemeratureAverage(2));
        evdb.setFieldValue(EventData.FLD_thermoAverage3 , geoEvent.getTemeratureAverage(3));
        evdb.setFieldValue(EventData.FLD_thermoAverage4 , geoEvent.getTemeratureAverage(4));
        evdb.setFieldValue(EventData.FLD_thermoAverage5 , geoEvent.getTemeratureAverage(5));
        evdb.setFieldValue(EventData.FLD_thermoAverage6 , geoEvent.getTemeratureAverage(6));
        evdb.setFieldValue(EventData.FLD_thermoAverage7 , geoEvent.getTemeratureAverage(7));
        
        // J1708 fields
        evdb.setFieldValue(EventData.FLD_fuelLevel      , geoEvent.getObcFuelLevel());
        evdb.setFieldValue(EventData.FLD_fuelEconomy    , geoEvent.getObcFuelEconomy());
        evdb.setFieldValue(EventData.FLD_fuelTotal      , geoEvent.getObcFuelTotal());
        evdb.setFieldValue(EventData.FLD_fuelIdle       , geoEvent.getObcFuelIdle());
        evdb.setFieldValue(EventData.FLD_engineRpm      , geoEvent.getObcEngineRPM());
        evdb.setFieldValue(EventData.FLD_coolantLevel   , geoEvent.getObcCoolantLevel());
        evdb.setFieldValue(EventData.FLD_coolantTemp    , geoEvent.getObcCoolantTemperature());
        evdb.setFieldValue(EventData.FLD_oilPressure    , geoEvent.getObcOilPressure());
        evdb.setFieldValue(EventData.FLD_j1708Fault     , geoEvent.getObcJ1708Fault(0));

        /* insert event */
        Device dev = this.getDevice();
        boolean didInsert = dev.insertEventData(evdb);
        if (!didInsert) {
            return ServerErrors.NAK_EVENT_ERROR;
        }

        /* simulate Geozones? */
        if (SIMEVENT_GEOZONES && evdb.isValidGeoPoint() && 
            (statusCode != StatusCodes.STATUS_GEOFENCE_ARRIVE) && 
            (statusCode != StatusCodes.STATUS_GEOFENCE_DEPART)   ) {
            EventData prevEv = DCServerFactory.getPreviousEventData(dev, timestamp);
            GeoPoint prevGP  = ((prevEv != null) && prevEv.isValidGeoPoint())? prevEv.getGeoPoint() : null;
            Geozone prevZone = (prevGP != null)? Geozone.getGeozone(accountID, null, prevGP, false) : null;
            GeoPoint thisGP  = evdb.getGeoPoint();
            Geozone thisZone = Geozone.getGeozone(accountID, null, thisGP, false);
            long zoneFixtime = timestamp - 1L; // subtract 1 second from arrive/depart
            if ((prevZone == null) && (thisZone != null)) {
                evdb.setTimestamp(zoneFixtime);
                evdb.setStatusCode(StatusCodes.STATUS_GEOFENCE_ARRIVE);
                dev.insertEventData(evdb); // ignore any errors
            } else
            if ((prevZone != null) && (thisZone == null)) {
                evdb.setTimestamp(zoneFixtime);
                evdb.setStatusCode(StatusCodes.STATUS_GEOFENCE_DEPART);
                dev.insertEventData(evdb); // ignore any errors
            }
        }

        /* success */
        return ServerErrors.NAK_OK;

    }

    // ------------------------------------------------------------------------

    /* save connection statistics */
    public void sessionStatistics(long startTime, String ipAddr, boolean isDuplex, long bytesRead, long bytesWritten, long evtsRecv)
    {
        this.getDevice().insertSessionStatistic(startTime,ipAddr,isDuplex,bytesRead,bytesWritten,evtsRecv);
    }

    // ------------------------------------------------------------------------

    /* return any pending packets to be sent to the client device */
    public PacketList getPendingPackets()
    {
        return this.getPendingPackets(true); // enable autodelete
    }
    
    /* return any pending packets to be sent to the client device */
    public PacketList getPendingPackets(boolean allowAutoDelete)
    {
        long limit = 1L; // only 1 pending record at a time
        try {
            String acctId = this.getAccountName();
            String devId  = this.getDeviceName();
            PendingPacket pp[] = PendingPacket.getPendingPackets(acctId, devId, limit);
            if ((pp != null) && (pp.length > 0)) {
                try {
                    
                    /* autodelete/predelete PendingPacket? */
                    if (allowAutoDelete) {
                        for (int i = 0; i < pp.length; i++) {
                            if (pp[i].isAutoDelete()) {
                                // pre-delete this PendingPacket now
                            }
                        }
                    }
                    
                    /* extract packets contained in all specified PendingPacket records */
                    Packet p[] = PendingPacket.extractPackets(pp);
                    if ((p != null) && (p.length > 0)) {
                        // mark PendingPacket record
                        long lastQueueTime = pp[pp.length - 1].getQueueTime();
                        PacketList plist = new PacketList(acctId, devId, p, lastQueueTime);
                        // return what packets we've retrieved
                        return plist;
                    } else {
                        return null;
                    }
                    
                } catch (PacketParseException ppe) {
                    
                    //PendingPacket.deletePendingPackets(acctId, devId, -1L);
                    Print.logException("Unable to parse pending packets", ppe);
                    return null;
                    
                }
            } else {
                
                // no pending packets
                return null;
                
            }
        } catch (DBException dbe) {
            
            Print.logError("PendingPacket retrieval: " + dbe);
            return null;
            
        }
    }

    public void clearPendingPackets(PacketList pktList)
    {
        if (pktList != null) {
            try {
                String accountID   = pktList.getAccountName();
                String deviceID    = pktList.getDeviceName();
                long lastQueueTime = pktList.getTimestamp();
                PendingPacket.deletePendingPackets(accountID, deviceID, lastQueueTime);
            } catch (DBException dbe) {
                Print.logError("PendingPacket delete: " + dbe);
            }
        }
    }

    // ------------------------------------------------------------------------

    /* save changes to the Device record */
    public int saveChanges()
    {
        try {
            if (this.devFields != null) {
                //this.device.save();
                if (this.dataXPort == this.device) {
                    this.device.updateChangedEventFields(this.devFields);
                } else {
                    this.dataXPort.update(this.devFields);
                    this.device.updateChangedEventFields();
                }
            }
            return ServerErrors.NAK_OK;
        } catch (DBException dbe) {
            return ServerErrors.NAK_DEVICE_ERROR;
        }
    }

    // ------------------------------------------------------------------------

    /* handle (DMTP) client device errors */
    public void handleError(int errCode, byte errData[])
    {

        /* display error info */
        // The value of 'errCode' is defined in OpenDMTP 'ClientErrors.java'
        // TODO: should delegate to an OpenDMTP server module (ie ClientErrors.java) 
        // to display the full error description.
        String ec = StringTools.toHexString(errCode,16);
        String ed = StringTools.toHexString(errData);
        if (errCode == ClientErrors.ERROR_COMMAND_INVALID) {
            Print.logError("Client Command Unsupported: 0x" + ec + " - " + ed);
        } else
        if (errCode == ClientErrors.ERROR_COMMAND_ERROR) {
            Payload ped  = new Payload(errData);
            long propKey = ped.readULong(2,-1L);
            long cmdErr  = ped.readULong(2,-1L);
            if (cmdErr == CommandErrors.COMMAND_OK) {
                Print.logError("Client Command OK: 0x" + ec + " - " + ed);
            } else
            if (cmdErr == CommandErrors.COMMAND_OK_ACK) {
                Print.logError("Client Command ACK: 0x" + ec + " - " + ed);
            } else {
                Print.logError("Client Command Error: 0x" + ec + " - " + ed);
            }
        } else {
            Print.logError("Client Error: 0x" + ec + " - " + ed);
        }

        /* save error information */
        try {
            Diagnostic.saveError(this.device, errCode, errData);
        } catch (DBException dbe) {
            Print.logException("Saving Property value", dbe);
        }

    }

    /* handle (DMTP) client device diagnostic values */
    public void handleDiagnostic(int diagCode, byte diagData[])
    {

        /* display diagnostic info */
        String dc = StringTools.toHexString(diagCode,16);
        String dd = StringTools.toHexString(diagData);
        Print.logWarn("Client Diagnostic: 0x" + dc + " - " + dd);

        /* save diagnostic information */
        try {
            Diagnostic.saveDiagnostic(this.device, diagCode, diagData);
        } catch (DBException dbe) {
            Print.logException("Saving Property value", dbe);
        }

    }

    /* handle (DMTP) client device property values */
    public void handleProperty(int propKey, byte propVal[])
    {

        /* display property info */
        PropertyKey pk = PropertyKey.GetPropertyKey(propKey);
        if (pk != null) {
            Print.logInfo("Client Property: " + pk.toString(propVal));
        } else {
            String pkx = StringTools.toHexString(propKey,16);
            String pvx = StringTools.toHexString(propVal);
            Print.logInfo("Client Property: 0x" + pkx + " - " + pvx);
        }

        /* save property information */
        try {
            Property.saveProperty(this.device, propKey, propVal);
        } catch (DBException dbe) {
            Print.logException("Saving Property value", dbe);
        }

    }

    // ------------------------------------------------------------------------

    /* String value */
    public String toString()
    {
        return (this.device != null)? this.device.toString() : "";
    }

    // ------------------------------------------------------------------------

}
