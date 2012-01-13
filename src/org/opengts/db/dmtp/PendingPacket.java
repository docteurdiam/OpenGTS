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
//  This module is used to queue packets which are to be sent to the client the
//  next time the client device checks in with the server.
// ----------------------------------------------------------------------------
// Change History:
//  2006/05/10  Martin D. Flynn
//     -Initial release
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -This class is used specifically for OpenDMTP server support and may not
//      be necessary, nor applicable, for other supported protocols.
//  2007/02/26  Martin D. Flynn
//     -Due to a problem in the OpenDMTP server 'Packet' class ('getPacketLength'
//      may not have returned the proper length of a binary encode packet),  Thus
//      "getPackets()" would not return the last packet found in the packet bytes
//      field.  This was fixed in OpenDMTP server v1.2.5.
//     -Added 'insertSetPropertyPacket'/'insertGetPropertyPacket' method.
//     -Added 'main' entry point to allow inserting set/get property packets.
//     -Added ability to insert pending geozone upload packets.
//     -Changed 'packetBytes' column type to 'TYPE_MBLOB', which allows '2^24-1'
//      byte packet blocks (the previous size was '2^16-1' which may not provide
//      a large enough buffer for upload-type packet sessions).
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect/DBDelete
//  2007/11/28  Martin D. Flynn
//     -Fixed Geozone PendingPacket generation to properly remove existing geozones
//      before reloading new zones.
//     -Added "-reboot" command option
//     -Added FLD_autoDelete to indicate that certaion packet records should be
//      "pre-deleted" before sending to the client.
//  2008/03/28  Martin D. Flynn
//     -Incorporate "DBRecord.select(DBSelect,...) method
// ----------------------------------------------------------------------------
package org.opengts.db.dmtp;

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

// bind to specific OpenDMTP classes
import org.opendmtp.codes.Encoding;
import org.opendmtp.codes.PropCodes;
import org.opendmtp.codes.ServerErrors;
import org.opendmtp.server.base.Packet;
import org.opendmtp.server.base.PacketParseException;

public class PendingPacket
    extends DeviceRecord<PendingPacket>
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME               = "PendingPacket";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_queueTime             = "queueTime";
    public static final String FLD_sequence              = "sequence";
    public static final String FLD_packetBytes           = "packetBytes";
    public static final String FLD_autoDelete            = "autoDelete";
    private static DBField FieldInfo[] = {
        // PendingPacket fields
        newField_accountID(true),
        newField_deviceID(true),
        new DBField(FLD_queueTime   , Long.TYPE     , DBField.TYPE_UINT32     , "Packet Queue Time"     , "key=true"),
        new DBField(FLD_sequence    , Integer.TYPE  , DBField.TYPE_UINT16     , "Sequence"              , "key=true"),
        new DBField(FLD_packetBytes , byte[].class  , DBField.TYPE_MBLOB      , "Packet Bytes"          , null), // at least 1Mb
        new DBField(FLD_autoDelete  , Boolean.TYPE  , DBField.TYPE_BOOLEAN    , "Delete after sending"  , "edit=2"),
    };

    /* key class */
    public static class Key
        extends DeviceKey<PendingPacket>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId, long queueTime, int seq) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_queueTime , queueTime);
            super.setFieldValue(FLD_sequence  , seq);
        }
        public DBFactory<PendingPacket> getFactory() {
            return PendingPacket.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<PendingPacket> factory = null;
    public static DBFactory<PendingPacket> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                PendingPacket.TABLE_NAME(), 
                PendingPacket.FieldInfo, 
                DBFactory.KeyType.PRIMARY, 
                PendingPacket.class, 
                PendingPacket.Key.class,
                false/*editable*/,false/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public PendingPacket()
    {
        super();
    }

    /* database record */
    public PendingPacket(PendingPacket.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(PendingPacket.class, loc);
        return i18n.getString("PendingPacket.description", 
            "This table contains " +
            "configuration packets which are to be sent to the " +
            "DMTP client device the next time it 'checks-in' with the server."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    // ------------------------------------------------------------------------

    public long getQueueTime()
    {
        Long v = (Long)this.getFieldValue(FLD_queueTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setQueueTime(long v)
    {
        this.setFieldValue(FLD_queueTime, v);
    }

    // ------------------------------------------------------------------------

    public int getSequence()
    {
        Integer v = (Integer)this.getFieldValue(FLD_sequence);
        return (v != null)? v.intValue() : 0;
    }

    public void setSequence(int v)
    {
        this.setFieldValue(FLD_sequence, v);
    }

    // ------------------------------------------------------------------------

    public byte[] getPacketBytes()
    {
        byte v[] = (byte[])this.getFieldValue(FLD_packetBytes);
        return (v != null)? v : new byte[0];
    }

    public void setPacketBytes(byte[] v)
    {
        this.setFieldValue(FLD_packetBytes, ((v != null)? v : new byte[0]));
    }

    public void setPacketBytes(byte[] v, int vlen)
    {
        if ((v == null) || (vlen == 0)) {
            // no bytes specified
            this.setFieldValue(FLD_packetBytes, new byte[0]);
        } else
        if ((vlen > 0) && (vlen < v.length)) {
            // write less than the number of bytes available
            byte vc[] = new byte[vlen];
            System.arraycopy(v, 0, vc, 0, vlen);
            this.setFieldValue(FLD_packetBytes, vc);
        } else {
            // write all specified bytes
            // ((vlen < 0) || (vlen >= v.length))
            this.setFieldValue(FLD_packetBytes, v);
        }
    }

    // ------------------------------------------------------------------------

    /* return true if this PendingPacket is to be automatically deleted after sending to the client */
    public boolean getAutoDelete()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_autoDelete);
        return (v != null)? v.booleanValue() : false;
    }

    /* set the 'autoDelete' state of this PendingPacket */
    public void setAutoDelete(boolean v)
    {
        this.setFieldValue(FLD_autoDelete, v);
    }

    /* return true if this PendingPacket is to be automatically deleted after sending to the client */
    public boolean isAutoDelete()
    {
        return this.getAutoDelete();
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String toString()
    {
        return this.getAccountID() + "/" + this.getDeviceID() + 
            "/" + this.getQueueTime() + "/" + this.getSequence();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static Object timestampLock = new Object();
    private static long   lastTimestamp = 0L;
    private static int    lastSequence  = 0;
    
    private static PendingPacket.Key _CreatePendingPacketKey(String accountID, String deviceID)
    {
        long timestamp;
        int  sequence;
        synchronized (PendingPacket.timestampLock) {
            long now = DateTime.getCurrentTimeSec();
            if (now == PendingPacket.lastTimestamp) {
                // time has not changed (rapid succession of PendingPacket records), bump sequence
                PendingPacket.lastSequence++;
            } else {
                // new timestamp
                PendingPacket.lastTimestamp = now;
                PendingPacket.lastSequence  = 0;
            }
            timestamp = PendingPacket.lastTimestamp;
            sequence  = PendingPacket.lastSequence;
        }
        return new PendingPacket.Key(accountID, deviceID, timestamp, sequence);
    }
    
    // ------------------------------------------------------------------------

    public Packet[] getPackets()
        throws PacketParseException
    {
        
        /* get bytes representing packets */
        byte b[] = this.getPacketBytes();
        if ((b == null) || (b.length == 0)) {
            Print.logWarn("No packet bytes ...");
            return null;
        }
        
        /* parse packets */
        int ofs = 0;
        java.util.List<Packet> pkts = new Vector<Packet>();
        while (ofs < b.length) {
            int len = Packet.getPacketLength(b, ofs);
            if (len > 0) {
                try {
                    byte p[] = new byte[len];
                    System.arraycopy(b, ofs, p, 0, len);
                    Packet pkt = new Packet(p);
                    pkts.add(pkt);
                    ofs += len;
                } catch (PacketParseException ppe) {
                    // an error occurred while parsing
                    // (the data does not represent a valid packet)
                    Print.logException("Invalid PendingPacket found!", ppe);
                    throw ppe;
                }
            } else {
                Print.logInfo("Premature end of packet bytes: ofs=" + ofs);
                throw new PacketParseException(ServerErrors.NAK_PACKET_LENGTH);
            }
        }
        
        /* return parsed packets */
        int pktCnt = pkts.size();
        if (pktCnt <= 0) {
            Print.logWarn("No packet data found in this pending packet! ...");
            return null;
        } else {
            return pkts.toArray(new Packet[pktCnt]);
        }
        
    }
    
    public void setPackets(Packet pkt[])
    {
        if (pkt != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < pkt.length; i++) {
                try {
                    baos.write(pkt[i].encode(Encoding.ENCODING_BINARY));
                } catch (IOException ioe) {
                    // ignored (will never occur)
                }
            }
            this.setPacketBytes(baos.toByteArray());
        } else {
            this.setPacketBytes(null);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* delete pending-packet records for specified account/device */
    public static void deletePendingPackets(String acctId, String devId, long queueTime)
        throws DBException
    {
        // 'queueTime' may be '-1L' to delete ALL pending packets

        /* invalid account/device? */
        if (StringTools.isBlank(acctId)) {
            return;
        } else
        if (StringTools.isBlank(devId)) {
            return;
        }

        /* delete statement */
        DBConnection dbc = null;
        try {
            // DBDelete: DELETE FROM PendingPacket WHERE ( accountID='acct' AND deviceID='dev' [AND queueTime<=123456789] )
            DBDelete ddel = new DBDelete(PendingPacket.getFactory());
            DBWhere dwh = ddel.createDBWhere();
            ddel.setWhere(dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(FLD_accountID,acctId),
                    dwh.EQ(FLD_deviceID ,devId),
                    ((queueTime > 0L)? dwh.LE(FLD_queueTime,queueTime) : null)
                )
            ));
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(ddel.toString());
        } catch (SQLException sqe) {
            throw new DBException("PendingPacket deletion", sqe);
        } finally {
            DBConnection.release(dbc);
        }

    }

    /* get all pending-packet records for specified account/device */
    public static PendingPacket[] getPendingPackets(String acctId, String devId)
        throws DBException
    {
        return PendingPacket.getPendingPackets(acctId, devId, -1);
    }

    /* get <limit> pending-packet records for specified account/device */
    public static PendingPacket[] getPendingPackets(String acctId, String devId, long limit)
        throws DBException
    {
        // If 'limit' is '-1L', then ALL records will be retrieved

        /* invalid account/device? */
        if (StringTools.isBlank(acctId)) {
            return null;
        } else
        if (StringTools.isBlank(devId)) {
            return null;
        }
        
        /* where clause */
        // DBSelect: WHERE ((accountID=='acct')AND(deviceID='dev')) ORDER BY queueTime,sequence LIMIT 1
        DBSelect<PendingPacket> dsel = new DBSelect<PendingPacket>(PendingPacket.getFactory());
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(FLD_accountID,acctId),
                dwh.EQ(FLD_deviceID ,devId)
            )
        ));
        dsel.setOrderByFields(FLD_queueTime,FLD_sequence);
        dsel.setLimit(limit);

        /* get PendingPackets */
        PendingPacket pp[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //pp = (PendingPacket[])DBRecord.select(PendingPacket.getFactory(), dsel.toString(false));
            pp = DBRecord.select(dsel); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }

        /* no packets? */
        if (pp == null) {
            // no records
            return null;
        }

        /* filter */
        if ((limit > 0L) && (pp.length > limit)) {
            // This will only occur if the DBProvider does not support 'limit'
            PendingPacket pp2[] = new PendingPacket[(int)limit];
            System.arraycopy(pp, 0, pp2, 0, (int)limit);
            pp = pp2;
        }

        /* return array of pending packets */
        return pp;

    }

    /* extract all packets from specified pending-packet records */
    public static Packet[] extractPackets(PendingPacket pp[])
        throws PacketParseException
    {
        if (ListTools.isEmpty(pp)) {
            Print.logWarn("No PendingPacket list specified");
            return null;
        } else
        if (pp.length == 1) {
            return pp[0].getPackets();
        } else {
            java.util.List<Packet> v = new Vector<Packet>();
            for (int i = 0; i < pp.length; i++) {
                Packet p[] = pp[i].getPackets();
                ListTools.toList(p, v);
            }
            return v.toArray(new Packet[v.size()]);
        }
    }

    //* get all pending packets for specified account/device */
    //public static Packet[] getPackets(String acctId, String devId)
    //    throws DBException, PacketParseException
    //{
    //    PendingPacket pp[] = PendingPacket.getPendingPackets(acctId, devId);
    //    if ((pp != null) && (pp.length > 0)) {
    //        return PendingPacket.extractPackets(pp);
    //    } else {
    //        return null;
    //    }
    //}

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* insert packets */
    public static boolean insertPackets(Device dev, Packet pkt[])
        throws DBException
    {
        if ((dev != null) && !ListTools.isEmpty(pkt)) {
            String acctId = dev.getAccountID();
            String devId  = dev.getDeviceID();
            return PendingPacket.insertPackets(acctId, devId, pkt);
        }
        return false;
    }
   
    /* insert packets */
    public static boolean insertPackets(String acctId, String devId, Packet pkt[])
        throws DBException
    {
        if (!StringTools.isBlank(acctId) && !StringTools.isBlank(devId) && !ListTools.isEmpty(pkt)) {
            PendingPacket pp = new PendingPacket(_CreatePendingPacketKey(acctId,devId));
            pp.setPackets(pkt);
            pp.save();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    
    public static class SetPropertyPacket
    {
        private int  propCode   = 0;
        private byte propData[] = null;
        public SetPropertyPacket(int code, String dataStr) throws PacketParseException {
            this.propCode = code;
            Object propArgs[] = PropCodes.parsePropertyValue(code, dataStr);
            if (ListTools.isEmpty(propArgs)) {
                Print.logError("Invalid property value(s) specified: " + dataStr);
                throw new PacketParseException(ServerErrors.NAK_PACKET_PAYLOAD);
            }
            this.propData = PropCodes.encodePropertyData(code, propArgs);
            if (ListTools.isEmpty(this.propData)) {
                Print.logError("Unable to encode property data: " + dataStr);
                throw new PacketParseException(ServerErrors.NAK_PACKET_PAYLOAD);
            }
        }
        public SetPropertyPacket(int code, byte data[]) {
            this.propCode = code;
            this.propData = data;
        }
        public int getCode() {
            return this.propCode;
        }
        public byte[] getData() {
            return this.propData;
        }
        public int getDataLength() {
            return (this.propData != null)? this.propData.length : 0;
        }
        public Packet getPacket() {
            return Packet.createServerSetPropertyPacket(this.getCode(), this.getData(), this.getDataLength());
        }
    }
    
    /* return a set-property packet wrapper */
    public static SetPropertyPacket createSetPropertyPacket(int propCode, String propDataStr)
    {
        try {
            return new SetPropertyPacket(propCode, propDataStr);
        } catch (PacketParseException ppe) {
            return null;
        }
    }

    /* insert a set-property update packet */
    public static boolean insertSetPropertyPacket(Device device, SetPropertyPacket setPropPacket)
        throws DBException
    {

        /* valid device/property? */
        if ((device == null) || (setPropPacket == null)) {
            return false;
        }
        
        /* insert */
        int  code    = setPropPacket.getCode();
        byte data[]  = setPropPacket.getData();
        int  dataLen = setPropPacket.getDataLength();
        return insertSetPropertyPacket(device, code, data, dataLen);

    }
    
    /* insert a set-property update packet */
    // the data in the String is parsed based on the data type of the property
    public static boolean insertSetPropertyPacket(Device device, int propCode, String propDataStr)
        throws DBException
    {
        
        /* valid device? */
        if (device == null) {
            return false;
        }
        //Print.logInfo("["+device+"] Inserting SetProperty code=0x" + StringTools.toHexString(propCode,16) + " value=" + propDataStr);
        
        // parse string arguments
        Object propArgs[] = PropCodes.parsePropertyValue(propCode, propDataStr);
        if (ListTools.isEmpty(propArgs)) {
            Print.logError("Invalid property value(s) specified: " + propDataStr);
            return false;
        }

        // encode arguments
        byte propData[] = PropCodes.encodePropertyData(propCode, propArgs);
        if (ListTools.isEmpty(propData)) {
            Print.logError("Unable to encode property data: " + propDataStr);
            return false;
        }

        // insert property change packet
        int pdLen = (propData != null)? propData.length : 0; // will not be '0'
        return PendingPacket.insertSetPropertyPacket(device, propCode, propData, pdLen);
        
    }

    /* insert a set-property update packet */
    public static boolean insertSetPropertyPacket(Device device, int propCode, byte propData[], int propDataLen)
        throws DBException
    {
        
        /* valid device? */
        if (device == null) {
            Print.logError("Device is null");
            return false;
        }
        String acctId = device.getAccountID();
        String devId  = device.getDeviceID();
        if (acctId.equals("") || devId.equals("")) {
            Print.logError("Account/Device is empty/null");
            return false;
        }

        // Check for, and remove, any existing SetProperty PendingPacket for the specified Property Code
        PendingPacket ppa[] = PendingPacket.getPendingPackets(acctId, devId, -1L);
        if (!ListTools.isEmpty(ppa)) {
            // we do have some existing Pending Packets
            for (int i = 0; i < ppa.length; i++) {
                try {
                    Packet pkt[] = PendingPacket.extractPackets(new PendingPacket[] { ppa[i] });
                    if (!ListTools.isEmpty(pkt)) {
                        for (int p = 0; p < pkt.length; p++) {
                            if (pkt[p].isBasicPacketHeader() && (pkt[p].getPacketType() == Packet.PKT_SERVER_SET_PROPERTY)) {
                                // we found a SetProperty packet
                                int pktPropCode = (int)pkt[p].getPayload(true).readULong(2,0L);
                                if (pktPropCode == propCode) {
                                    // we've found an existing SetProperty with the same code
                                    if (pkt.length == 1) {
                                        // only one packet in this PendingPacket, delete this PendingPacket
                                        try {
                                            Print.logInfo("Deleting existing SetProperty for Code: 0x" + StringTools.toHexString(propCode,16));
                                            PendingPacket.Key key = (PendingPacket.Key)ppa[i].getRecordKey();
                                            key.delete(true); // will also delete dependencies (which there are none)
                                        } catch (DBException dbe) {
                                            Print.logException("Unable to delete existing PendingPacket", dbe);
                                            return false;
                                        }
                                    } else {
                                        // more than one packet found
                                        Print.logError("A SetProperty for this code already exists!");
                                        return false;
                                    }
                                }
                            }
                        }
                    } else {
                        // Odd, we found a PendingPacket that contains no Packets (not likely to occur)
                        Print.logWarn("PendingPacket found which does not contain any Packets: " + ppa[i]);
                    }
                } catch (PacketParseException ppe) {
                    long qt = ppa[i].getQueueTime();
                    int seq = ppa[i].getSequence();
                    Print.logError("Error parsing packets: queueTime="+qt+ ", sequence="+seq);
                }
             }
        }

        // Queue packet
        PendingPacket pp = new PendingPacket(_CreatePendingPacketKey(acctId,devId));
        Packet propsPkt = Packet.createServerSetPropertyPacket(propCode, propData, propDataLen);
        Print.logInfo("["+acctId+"/"+devId+"] Inserting " + propsPkt);
        pp.setPackets(new Packet[] { propsPkt });
        pp.save();
                
        /* success */
        return true;

    }
    
    /* insert a get-property packet */
    public static boolean insertGetPropertyPacket(Device device, int propCode, byte propData[], int propDataLen)
        throws DBException
    {
        if (device != null) {
            String acctId = device.getAccountID();
            String devId  = device.getDeviceID();
            if (!acctId.equals("") && !devId.equals("")) {
                PendingPacket pp = new PendingPacket(_CreatePendingPacketKey(acctId,devId));
                Packet propsPkt = Packet.createServerGetPropertyPacket(propCode, propData, propDataLen);
                Print.logInfo("Inserting " + propsPkt);
                pp.setPackets(new Packet[] { propsPkt });
                pp.save();
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /* insert command packet */
    public static boolean insertGetGpsDiagnostic(Device device)
        throws DBException
    {
        if (device != null) {
            String acctId = device.getAccountID();
            String devId  = device.getDeviceID();
            if (!acctId.equals("") && !devId.equals("")) {
                PendingPacket pp = new PendingPacket(_CreatePendingPacketKey(acctId,devId));
                Packet propsPkt = Packet.createServerGetPropertyPacket(PropCodes.PROP_STATE_GPS_DIAGNOSTIC, null, 0);
                Print.logInfo("Inserting " + propsPkt);
                pp.setPackets(new Packet[] { propsPkt });
                pp.save();
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /* insert a set-property update packet */
    public static boolean insertUploadFilePacket(Device device, String uplFileName, long uplFileSize)
        throws DBException
    {
        if ((device != null) && (uplFileName != null) && (uplFileSize >= 0L)) {
            String acctId = device.getAccountID();
            String devId  = device.getDeviceID();
            if (!acctId.equals("") && !devId.equals("")) {
                PendingPacket pp = new PendingPacket(_CreatePendingPacketKey(acctId,devId));
                Packet propsPkt = Packet.createServerGetFilePacket(uplFileName, uplFileSize);
                Print.logInfo("Inserting " + propsPkt);
                pp.setPackets(new Packet[] { propsPkt });
                pp.save();
            }
        }
        return false;
    }
    

    // ------------------------------------------------------------------------

    public static Packet[] createGeozoneUploadPackets(String acctId, String version)
    {

        /* get zones */
        Geozone gz[] = Geozone.getClientUploadZones(acctId);
        if (gz == null) {
            return null;
        }

        /* init */
        java.util.List<Packet> packets = new Vector<Packet>();
        Payload p = new Payload();

        /* remove existing geozones */
        p.clear();
        p.writeULong((long)PropCodes.PROP_CMD_GEOF_ADMIN, 2);
        p.writeULong((long)PropCodes.GEOF_CMD_REMOVE    , 1);
        packets.add(Packet.createServerPacket(Packet.PKT_SERVER_SET_PROPERTY, p.getBytes()));

        /* encode zones (dual point) */
        int gzNdx = 0;
        for (;(gzNdx < gz.length);) {
            p.clear();
            p.writeULong((long)PropCodes.PROP_CMD_GEOF_ADMIN, 2);
            p.writeULong((long)PropCodes.GEOF_CMD_ADD_STD_2 , 1);
            for (;(gzNdx < gz.length) && gz[gzNdx].encodeDMTPZone(p,2,false); gzNdx++);
            packets.add(Packet.createServerPacket(Packet.PKT_SERVER_SET_PROPERTY, p.getBytes()));
        }

        /* save geozones */
        p.clear();
        p.writeULong((long)PropCodes.PROP_CMD_GEOF_ADMIN, 2);
        p.writeULong((long)PropCodes.GEOF_CMD_SAVE, 1);
        packets.add(Packet.createServerPacket(Packet.PKT_SERVER_SET_PROPERTY, p.getBytes()));

        /* set version */
        p.clear();
        p.writeULong((long)PropCodes.PROP_GEOF_VERSION, 2);
        p.writeString(((version!=null)?version:""), 16);
        packets.add(Packet.createServerPacket(Packet.PKT_SERVER_SET_PROPERTY, p.getBytes()));

        /* return packets */
        return packets.toArray(new Packet[packets.size()]);
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct"  };
    private static final String ARG_DEVICE[]    = new String[] { "device" , "dev"   };
    private static final String ARG_SETPROP[]   = new String[] { "setprop"          };
    private static final String ARG_GETPROP[]   = new String[] { "getprop"          };
    private static final String ARG_GETGPS[]    = new String[] { "getgps"           };
    private static final String ARG_UPLOAD[]    = new String[] { "upload"           };
    private static final String ARG_LIST[]      = new String[] { "list"             };
    private static final String ARG_DELETE[]    = new String[] { "delete"           };
    private static final String ARG_GEOZ[]      = new String[] { "geoz"             };
    private static final String ARG_REBOOT[]    = new String[] { "reboot"           };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + PendingPacket.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>           Acount ID which owns the specified Device");
        Print.logInfo("  -device=<id>            Device ID to apply pending packets");
        Print.logInfo("  -setprop=<key>:<data>   Set the specified property key to this value");
        Print.logInfo("  -getprop=<key>[:<arg>]  Get the value of the specified property key");
        Print.logInfo("  -geoz[=<version>]       Upload all client geozones for this device");
        Print.logInfo("  -list                   List all pending packets for Device");
        Print.logInfo("  -delete=<time>          Delete all pending packets for Device up to specified time (-1 for ALL)");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE , "");

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.sysPrintln("ERROR: Account-ID not specified.");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(acctID); // may return DBException
            if (account == null) {
                Print.sysPrintln("ERROR: Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* device-id specified? */
        if (StringTools.isBlank(devID)) {
            Print.sysPrintln("ERROR: Device-ID not specified.");
            usage();
        }

        /* get device */
        Device device = null;
        try {
            device = Device.getDevice(account, devID, false);
            if (device == null) {
                Print.sysPrintln("ERROR: Device-ID does not exist: " + devID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Device: " + devID, dbe);
            //dbe.printException();
            System.exit(99);
        }
        
        /* option count */
        int opts = 0;

        /* get client property */
        // ie. "-getprop=F721[:<args>]"
        if (RTConfig.hasProperty(ARG_GETPROP)) {
            opts++;
            Print.sysPrintln("");
            String getprop     = RTConfig.getString(ARG_GETPROP,"");
            int    p           = getprop.indexOf(':');
            String propCodeStr = (p >= 0)? getprop.substring(0,p) : getprop;
            String propDataStr = (p >= 0)? getprop.substring(p+1) : null;
            int    propCode    = StringTools.parseHexInt(propCodeStr,0);
            if (!PropCodes.isValidPropertyCode(propCode)) {
                Print.logError("Invalid property code specified: " + propCodeStr + " [" + propCode + "]");
                usage();
            }
            byte propData[] = null;
            if ((propDataStr != null) && !propDataStr.equals("")) {
                Object propArgs[] = PropCodes.parsePropertyValue(propCode, propDataStr);
                if ((propArgs == null) || (propArgs.length == 0)) {
                    Print.logError("Invalid property value(s) specified");
                    usage();
                }
                propData = PropCodes.encodePropertyData(propCode, propArgs);
                if ((propData == null) || (propData.length == 0)) {
                    Print.logError("Unable to encode property data");
                    usage();
                }
            }
            try {
                int pdLen = (propData != null)? propData.length : 0;
                PendingPacket.insertGetPropertyPacket(device, propCode, propData, pdLen);
            } catch (DBException dbe) {
                Print.logException("Error while inserting pending packet", dbe);
                System.exit(1);
            }
            System.exit(0);
        }

        /* set client property */
        // ie. "-setprop=F721:123.0"
        if (RTConfig.hasProperty(ARG_SETPROP)) {
            opts++;
            Print.sysPrintln("");
            String setprop     = RTConfig.getString(ARG_SETPROP,"");
            int    p           = setprop.indexOf(':');
            String propCodeStr = (p >= 0)? setprop.substring(0,p) : "";
            String propDataStr = (p >= 0)? setprop.substring(p+1) : "";
            int    propCode    = StringTools.parseHexInt(propCodeStr,0);
            // validate property
            if (!PropCodes.isValidPropertyCode(propCode)) {
                Print.logError("Invalid property code specified: " + propCodeStr);
                usage();
            }
            // insert property change packet
            try {
                Print.sysPrintln("Inserting SetProperty for Code: " + PropCodes.getPropertyDescription(propCode));
                if (!PendingPacket.insertSetPropertyPacket(device, propCode, propDataStr)) {
                    usage();
                }
            } catch (DBException dbe) {
                Print.logException("Error while inserting pending packet", dbe);
                System.exit(1);
            }
            // done
            System.exit(0);
        }

        /* get gps property */
        // ie. "-getgps"
        if (RTConfig.hasProperty(ARG_GETGPS)) {
            opts++;
            Print.sysPrintln("");
            // insert upload file packet
            try {
                PendingPacket.insertGetGpsDiagnostic(device);
            } catch (DBException dbe) {
                Print.logException("Error while inserting GPS diagnostic request", dbe);
                System.exit(1);
            }
            // done
            System.exit(0);
        }
        
        /* OOB upload */
        // ie. "-upload=FileName"
        if (RTConfig.hasProperty(ARG_UPLOAD)) {
            opts++;
            Print.sysPrintln("");
            String uplFile = RTConfig.getString(ARG_UPLOAD,"");
            // validate filename
            if (uplFile.equals("")) {
                Print.logError("Invalid upload file specified");
                usage();
            }
            // insert upload file packet
            try {
                PendingPacket.insertUploadFilePacket(device, uplFile, 0L);
            } catch (DBException dbe) {
                Print.logException("Error while inserting upload packet", dbe);
                System.exit(1);
            }
            // done
            System.exit(0);
        }

        /* send client reboot command */
        // ie. "-reboot=now"
        if (RTConfig.hasProperty(ARG_REBOOT)) {
            // WARNING: In the current configuration, this 'reboot' packet will never be removed from the
            // 'PendingPacket' table.  This is because the client never acknowledged receipt of the packet
            // and thus the server does not remove the packet from the PendingPacket queue.  The result is
            // that the client will continually be sent the 'reboot' command on each connection to the
            // server.  'PendingPacket' needs to be modified to be able to remove this packet automatically
            // after sending it to the client.
            opts++;
            Print.sysPrintln("");
            String reboot = RTConfig.getString(ARG_REBOOT,"");
            if ((reboot != null) && !reboot.equals("")) {
                // insert reboot command property packet
                try {
                    // create reboot argument payload
                    byte rebootArg[] = reboot.getBytes();
                    byte propData[]  = new byte[1 + rebootArg.length];
                    propData[0] = (byte)0; // cold boot
                    System.arraycopy(rebootArg, 0, propData, 1, rebootArg.length);
                    // insert packet
                    PendingPacket.insertSetPropertyPacket(device, PropCodes.PROP_CMD_RESET, propData, propData.length);
                } catch (DBException dbe) {
                    Print.logException("Error while inserting pending packet", dbe);
                    System.exit(1);
                }
            } else {
                Print.logError("Missing 'reboot' argument");
            }
            // done
            System.exit(0);
        }

        /* upload Geozones */
        if (RTConfig.hasProperty(ARG_GEOZ)) {
            opts++;
            String version = RTConfig.getString(ARG_GEOZ, "GEOZONE_1.0");
            Packet pkt[] = PendingPacket.createGeozoneUploadPackets(acctID, version);
            if ((pkt != null) && (pkt.length > 0)) {
                for (int i = 0; i < pkt.length; i++) {
                    Print.sysPrintln(" Upload: " + pkt[i]);
                }
                try {
                    PendingPacket.insertPackets(device, pkt);
                } catch (DBException dbe) {
                    Print.logException("Unable to insert pending Geozone-Upload packets", dbe);
                    System.exit(1);
                }
            } else {
                Print.logInfo("No Geozones defined");
            }
            System.exit(0);
        }

        /* list current packets */
        // ie. "-list"
        if (RTConfig.hasProperty(ARG_LIST)) {
            opts++;
            Print.sysPrintln("");
            try {
                PendingPacket pp[] = PendingPacket.getPendingPackets(acctID, devID, -1L);
                if (!ListTools.isEmpty(pp)) {
                    //Print.logInfo("PendingPacket record count = " + pp.length);
                    for (int i = 0; i < pp.length; i++) {
                        long qt = pp[i].getQueueTime();
                        int  sq = pp[i].getSequence();
                        long sz = pp[i].getPacketBytes().length;
                        Print.sysPrintln(i + ") PendingPacket " + qt + ":" + sq + " (" + sz + " bytes):");
                        try {
                            // Duplicate the method used by 'DeviceDBImpl' for testing purposes:
                            //Packet pkt[] = pp[i].getPackets();
                            Packet pkt[] = PendingPacket.extractPackets(new PendingPacket[] { pp[i] });
                            if ((pkt != null) && (pkt.length > 0)) {
                                for (int p = 0; p < pkt.length; p++) {
                                    Print.sysPrintln("   Packet: " + pkt[p]);
                                }
                            }
                        } catch (PacketParseException ppe) {
                            Print.logError("Error parsing packets at QueueTime " + qt);
                        }
                        Print.logInfo("");
                    }
                } else {
                    Print.logInfo("No pending packets for this account/device");
                }
            } catch (Throwable th) {
                Print.logException("Error while getting packets for account/device", th);
                System.exit(1);
            }
            System.exit(0);
        }

        /* delete existing pending packets */
        // ie. "-delete=<time>"
        if (RTConfig.hasProperty(ARG_DELETE)) {
            opts++;
            Print.sysPrintln("");
            long queTime = RTConfig.getLong(ARG_DELETE, -2L);
            if ((queTime == -1L) || (queTime > 0L)) {
                try {
                    PendingPacket.deletePendingPackets(acctID, devID, queTime);
                    if (queTime == -1L) {
                        Print.sysPrintln("Deleted all pending packets");
                    } else {
                        Print.sysPrintln("Deleted pending packets up to, and including " + queTime);
                    }
                } catch (Throwable th) {
                    Print.logException("Error while deleting packets for account/device", th);
                    System.exit(1);
                }
                System.exit(0);
            } else {
                Print.logError("Invalid time specified: " + RTConfig.getString(ARG_DELETE,""));
                usage();
            }
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

}
