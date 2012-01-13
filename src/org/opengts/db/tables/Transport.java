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
//  2008/05/14  Martin D. Flynn
//     -Initial release
//  2008/10/16  Martin D. Flynn
//     -Added FLD_lastPingTime, FLD_totalPingCount
//  2009/09/23  Martin D. Flynn
//     -Added FLD_maxPingCount
//  2009/11/01  Martin D. Flynn
//     -Added FLD_expectAck, FLD_lastAckCommand, FLD_lastAckTime
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.db.*;

/**
*** This class represents a single data transport for a tracking/telematic hardware device.
*** In the case where a single hardware tracking/telematic device supports multiple data 
*** transports (such as GPRS and Satellite), a hardware device would have more than one
*** 'Transport' instance.
**/

public class Transport
    extends AccountRecord<Transport>
    implements DataTransport
{
    
    // ------------------------------------------------------------------------

    // set to true when it is desireable to query the Transport table entries for 
    // a specific Device/Asset reference.
    public static boolean isTransportQueryEnabled()
    {
        return RTConfig.getBoolean(DBConfig.PROP_Transport_queryEnabled);
    }

    // set to true when it is desireable to create a default Transport from the Device record
    public static boolean allowCreateDefaultTransport()
    {
        return false;
    }

    // ------------------------------------------------------------------------
    // OpenDMTP Protocol Definition v0.1.0 Conformance:
    // These encoding value constants are defined by the OpenDMTP protocol specification
    // and must remain as specified here.  For more information, see the following source
    // file in the OpenDMTP-Server project: "src/org/opendmtp/codes/Encoding.java"

    public  static final int    SUPPORTED_ENCODING_BINARY               = 0x01;
    public  static final int    SUPPORTED_ENCODING_BASE64               = 0x02;
    public  static final int    SUPPORTED_ENCODING_HEX                  = 0x04;
    public  static final int    SUPPORTED_ENCODING_CSV                  = 0x08;

    // ------------------------------------------------------------------------
    // new Transport defaults

    public  static final String DEFAULT_XPORT_NAME                      = "New Data Transport";
    public  static final int    DEFAULT_ENCODING = 
        Transport.SUPPORTED_ENCODING_BINARY | 
        Transport.SUPPORTED_ENCODING_BASE64 | 
        Transport.SUPPORTED_ENCODING_HEX;
    public  static final int    DEFAULT_UNIT_LIMIT_INTERVAL_MIN         =  0; // 30] (minutes)
    public  static final int    DEFAULT_MAX_ALLOWED_EVENTS              =  0; // 30] '0' == unlimited
    public  static final int    DEFAULT_TOTAL_MAX_CONNECTIONS           =  0; // 30] '0' == unlimited
    public  static final int    DEFAULT_TOTAL_MAX_CONNECTIONS_PER_MIN   =  0; //  2] '0' == unlimited
    public  static final int    DEFAULT_DUPLEX_MAX_CONNECTIONS          =  0; // 30] '0' == unlimited
    public  static final int    DEFAULT_DUPLEX_MAX_CONNECTIONS_PER_MIN  =  0; //  2]s '0' == unlimited

    // ------------------------------------------------------------------------
    // OpenDMTP Protocol Definition v0.1.1 Conformance:
    // These property value constants are defined by the OpenDMTP protocol specification
    // and must remain as specified here.  For more information, see the following source
    // file in the OpenDMTP-J2ME-Client project: "org/opendmtp/j2me/codes/DMTPProps.java"

    public  static final int    PROP_COMM_MAX_CONNECTIONS               = 0xF311;           // PropertyKey.PROP_COMM_MAX_CONNECTIONS
    public  static final String PROP_COMM_MAX_CONNECTIONS_STR           = "com.maxconn";    // PropertyKey.PROP_COMM_MAX_CONNECTIONS
    
    public  static final int    PROP_COMM_MIN_XMIT_DELAY                = 0xF312;           // PropertyKey.PROP_COMM_MIN_XMIT_DELAY
    public  static final String PROP_COMM_MIN_XMIT_DELAY_STR            = "com.mindelay";   // PropertyKey.PROP_COMM_MIN_XMIT_DELAY
    
    public  static final int    PROP_COMM_MIN_XMIT_RATE                 = 0xF313;           // PropertyKey.PROP_COMM_MIN_XMIT_RATE
    public  static final String PROP_COMM_MIN_XMIT_RATE_STR             = "com.minrate";    // PropertyKey.PROP_COMM_MIN_XMIT_RATE
    
    public  static final int    PROP_COMM_MAX_XMIT_RATE                 = 0xF315;           // PropertyKey.PROP_COMM_MAX_XMIT_RATE
    public  static final String PROP_COMM_MAX_XMIT_RATE_STR             = "com.maxrate";    // PropertyKey.PROP_COMM_MAX_XMIT_RATE
    
    public  static final int    PROP_COMM_MAX_DUP_EVENTS                = 0xF317;           // PropertyKey.PROP_COMM_MAX_DUP_EVENTS
    public  static final String PROP_COMM_MAX_DUP_EVENTS_STR            = "com.maxduplex";  // PropertyKey.PROP_COMM_MAX_DUP_EVENTS
    
    public  static final int    PROP_COMM_MAX_SIM_EVENTS                = 0xF318;           // PropertyKey.PROP_COMM_MAX_SIM_EVENTS
    public  static final String PROP_COMM_MAX_SIM_EVENTS_STR            = "com.maxsimplex"; // PropertyKey.PROP_COMM_MAX_SIM_EVENTS

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DMTP Encodings
    
    public enum Encodings implements EnumTools.BitMask, EnumTools.StringLocale {
        NONE        (0L,I18N.getString(Transport.class,"Transport.encoding.none"  ,"None")),
        BINARY      (1L,I18N.getString(Transport.class,"Transport.encoding.binary","Binary")),
        BASE64      (2L,I18N.getString(Transport.class,"Transport.encoding.base64","Base64")),
        HEX         (4L,I18N.getString(Transport.class,"Transport.encoding.hex"   ,"Hex"   ));
        // ---
        private long        vv = 0;
        private I18N.Text   aa = null;
        Encodings(long v, I18N.Text a)              { vv=v; aa=a; }
        public long    getLongValue()               { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME               = "Transport";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_transportID           = "transportID";           // 
    public static final String FLD_uniqueID              = "uniqueID";              // unique ID
    public static final String FLD_assocAccountID        = "assocAccountID";        // associated account id
    public static final String FLD_assocDeviceID         = "assocDeviceID";         // associated device id
    public static final String FLD_deviceCode            = "deviceCode";            // manufacturer, etc (config)
    public static final String FLD_deviceType            = "deviceType";            // reserved
    public static final String FLD_serialNumber          = "serialNumber";          // device hardware serial#.
    public static final String FLD_simPhoneNumber        = "simPhoneNumber";        // SIM phone number
    public static final String FLD_smsEmail              = "smsEmail";              // SMS email address
    public static final String FLD_imeiNumber            = "imeiNumber";            // IMEI number
    public static final String FLD_lastInputState        = "lastInputState";        // last known digital input state
    public static final String FLD_ignitionIndex         = "ignitionIndex";         // hardware ignition I/O index
    public static final String FLD_codeVersion           = "codeVersion";           // code version installed on device
    public static final String FLD_featureSet            = "featureSet";            // device features
    public static final String FLD_ipAddressValid        = "ipAddressValid";        // valid IP address block
    // Ping/Command
    public static final String FLD_pendingPingCommand    = "pendingPingCommand";    // pending ping command
    public static final String FLD_lastPingTime          = "lastPingTime";          // last ping time
    public static final String FLD_totalPingCount        = "totalPingCount";        // total ping count
    public static final String FLD_maxPingCount          = "maxPingCount";          // maximum ping count
    public static final String FLD_expectAck             = "expectAck";             // expecting a returned ACK
    public static final String FLD_lastAckCommand        = "lastAckCommand";        // last command expecting an ACK
    public static final String FLD_lastAckTime           = "lastAckTime";           // last received ACK time
    // DMTP
    public static final String FLD_supportsDMTP          = "supportsDMTP";          // DMTP
    public static final String FLD_supportedEncodings    = "supportedEncodings";    // DMTP
    public static final String FLD_unitLimitInterval     = "unitLimitInterval";     // DMTP
    public static final String FLD_maxAllowedEvents      = "maxAllowedEvents";      // DMTP
    public static final String FLD_totalProfileMask      = "totalProfileMask";      // DMTP
    public static final String FLD_totalMaxConn          = "totalMaxConn";          // DMTP
    public static final String FLD_totalMaxConnPerMin    = "totalMaxConnPerMin";    // DMTP
    public static final String FLD_duplexProfileMask     = "duplexProfileMask";     // DMTP
    public static final String FLD_duplexMaxConn         = "duplexMaxConn";         // DMTP
    public static final String FLD_duplexMaxConnPerMin   = "duplexMaxConnPerMin";   // DMTP
    // Last Event
    public static final String FLD_ipAddressCurrent      = "ipAddressCurrent";      // current(last) IP address
    public static final String FLD_remotePortCurrent     = "remotePortCurrent";     // current(last) remote port
    public static final String FLD_listenPortCurrent     = "listenPortCurrent";     // current(last) listen port
    // Last Event
    public static final String FLD_lastTotalConnectTime  = "lastTotalConnectTime";  // last connect time
    public static final String FLD_lastDuplexConnectTime = "lastDuplexConnectTime"; // last TCP connect time
    //
    private static DBField FieldInfo[] = {
        // Transport fields
        newField_accountID(true),   // "key=true"
        new DBField(FLD_transportID          , String.class        , DBField.TYPE_XPORT_ID()  , "Transport ID"                  , "key=true"),
        new DBField(FLD_assocAccountID       , String.class        , DBField.TYPE_ACCT_ID()   , "Associated Account ID"         , "edit=2 altkey=device"),
        new DBField(FLD_assocDeviceID        , String.class        , DBField.TYPE_DEV_ID()    , "Associated Device ID"          , "edit=2 altkey=device"),
        // DataTransport fields
        new DBField(FLD_uniqueID             , String.class        , DBField.TYPE_UNIQ_ID()   , "Unique ID"                     , "edit=2 altkey=altIndex"),
        new DBField(FLD_deviceCode           , String.class        , DBField.TYPE_STRING(24)  , "Device Code"                   , "edit=2"),
        new DBField(FLD_deviceType           , String.class        , DBField.TYPE_STRING(24)  , "Device Type"                   , "edit=2"),
        new DBField(FLD_serialNumber         , String.class        , DBField.TYPE_STRING(24)  , "Serial Number"                 , "edit=2"),
        new DBField(FLD_simPhoneNumber       , String.class        , DBField.TYPE_STRING(24)  , "SIM Phone Number"              , "edit=2"),
        new DBField(FLD_smsEmail             , String.class        , DBField.TYPE_STRING(64)  , "SMS EMail Address"             , "edit=2"),
        new DBField(FLD_imeiNumber           , String.class        , DBField.TYPE_STRING(24)  , "IMEI Number"                   , "edit=2"),
        new DBField(FLD_lastInputState       , Long.TYPE           , DBField.TYPE_UINT32      , "Last Input State"              , ""),
        new DBField(FLD_ignitionIndex        , Integer.TYPE        , DBField.TYPE_UINT16      , "Ignition I/O Index"            , "edit=2"),
        new DBField(FLD_codeVersion          , String.class        , DBField.TYPE_STRING(32)  , "Code Version"                  , ""),
        new DBField(FLD_featureSet           , String.class        , DBField.TYPE_STRING(64)  , "Feature Set"                   , ""),
        new DBField(FLD_ipAddressValid       , DTIPAddrList.class  , DBField.TYPE_STRING(128) , "Valid IP Addresses"            , "edit=2"),
        new DBField(FLD_ipAddressCurrent     , DTIPAddress.class   , DBField.TYPE_STRING(32)  , "Current IP Address"            , ""),
        new DBField(FLD_remotePortCurrent    , Integer.TYPE        , DBField.TYPE_UINT16      , "Current Remote Port"           , ""),
        new DBField(FLD_listenPortCurrent    , Integer.TYPE        , DBField.TYPE_UINT16      , "Current Listen Port"           , ""),
        // Ping/Command
        new DBField(FLD_pendingPingCommand   , String.class        , DBField.TYPE_TEXT        , "Pending Ping Command"          , "edit=2"),
        new DBField(FLD_lastPingTime         , Long.TYPE           , DBField.TYPE_UINT32      , "Last 'Ping' Time"              , "format=time"),
        new DBField(FLD_totalPingCount       , Integer.TYPE        , DBField.TYPE_UINT16      , "Total 'Ping' Count"            , ""),
        new DBField(FLD_maxPingCount         , Integer.TYPE        , DBField.TYPE_UINT16      , "Maximum 'Ping' Count"          , "edit=2"),
        new DBField(FLD_expectAck            , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , "Expecting an ACK"            , "edit=2"),
        new DBField(FLD_lastAckCommand       , String.class        , DBField.TYPE_TEXT        , "Last Command Expecting an ACK", ""),
        new DBField(FLD_lastAckTime          , Long.TYPE           , DBField.TYPE_UINT32      , "Last Received 'ACK' Time"    , "format=time"),
        // DMTP
        new DBField(FLD_supportsDMTP         , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , "Supports DMTP"                 , "edit=2"),
        new DBField(FLD_supportedEncodings   , Integer.TYPE        , DBField.TYPE_UINT8       , "Supported Encodings"           , "edit=2 format=X1 editor=encodings mask=Transport$Encodings"),
        new DBField(FLD_unitLimitInterval    , Integer.TYPE        , DBField.TYPE_UINT16      , "Accounting Time Interval Min"  , "edit=2"),
        new DBField(FLD_maxAllowedEvents     , Integer.TYPE        , DBField.TYPE_UINT16      , "Max Events per Interval"       , "edit=2"),
        new DBField(FLD_totalProfileMask     , DTProfileMask.class , DBField.TYPE_BLOB        , "Total Profile Mask"            , ""),
        new DBField(FLD_totalMaxConn         , Integer.TYPE        , DBField.TYPE_UINT16      , "Max Total Conn per Interval"   , "edit=2"),
        new DBField(FLD_totalMaxConnPerMin   , Integer.TYPE        , DBField.TYPE_UINT16      , "Max Total Conn per Minute"     , "edit=2"),
        new DBField(FLD_duplexProfileMask    , DTProfileMask.class , DBField.TYPE_BLOB        , "Duplex Profile Mask"           , ""),
        new DBField(FLD_duplexMaxConn        , Integer.TYPE        , DBField.TYPE_UINT16      , "Max Duplex Conn per Interval"  , "edit=2"),
        new DBField(FLD_duplexMaxConnPerMin  , Integer.TYPE        , DBField.TYPE_UINT16      , "Max Duplex Conn per Minute"    , "edit=2"),
        new DBField(FLD_lastTotalConnectTime , Long.TYPE           , DBField.TYPE_UINT32      , "Last Total Connect Time"       , "format=time"),
        new DBField(FLD_lastDuplexConnectTime, Long.TYPE           , DBField.TYPE_UINT32      , "Last Duplex Connect Time"      , "format=time"),
        // Common fields
        newField_displayName(),
        newField_description(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends AccountKey<Transport>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String xportId) {
            super.setFieldValue(FLD_accountID  , ((acctId  != null)? acctId.toLowerCase()  : ""));
            super.setFieldValue(FLD_transportID, ((xportId != null)? xportId.toLowerCase() : ""));
        }
        public DBFactory<Transport> getFactory() {
            return Transport.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Transport> factory = null;
    public static DBFactory<Transport> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Transport.TABLE_NAME(),
                Transport.FieldInfo, 
                DBFactory.KeyType.PRIMARY, 
                Transport.class, 
                Transport.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Transport()
    {
        super();
    }

    /* database record */
    public Transport(Transport.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Transport.class, loc);
        return i18n.getString("Transport.description", 
            "This table defines " +
            "the data transport specific information for an Asset/Device.  A 'Transport' " +
            "represents the datapath used to send data to a server.  In some cases a single 'Device' " +
            "can have more than one such datapath to the server, such as a device that incorporates " +
            "both GPRS and satellite communications."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
    // ------------------------------------------------------------------------

    /* Transport ID */
    public String getTransportID()
    {
        String v = (String)this.getFieldValue(FLD_transportID);
        return StringTools.trim(v);
    }
    
    private void setTransportID(String v)
    {
        this.setFieldValue(FLD_transportID, StringTools.trim(v));
    }
        
    // ------------------------------------------------------------------------

    /* Assigned Unique ID */
    public String getUniqueID()
    {
        String v = (String)this.getFieldValue(FLD_uniqueID);
        return StringTools.trim(v);
    }
    
    public void setUniqueID(String v)
    {
        this.setFieldValue(FLD_uniqueID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the target Account ID
    *** @return v  The target Account ID
    **/
    public String getTargetAccountID()
    {
        String assocAccountID = this.getAssocAccountID();
        if (!StringTools.isBlank(assocAccountID)) {
            return assocAccountID;
        } else {
            return this.getAccountID();
        }
    }

    /**
    *** Returns the AccountID associated with this Transport<br>
    *** This method should return non-blank only if not equal to 'getAccountID()' and Account
    *** level indirection is desired.
    *** @return The AccountID associated with this Transport
    **/
    public String getAssocAccountID()
    {
        String v = (String)this.getFieldValue(FLD_assocAccountID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the associated account ID
    *** @param v  The associated Account ID
    **/
    public void setAssocAccountID(String v)
    {
        v = StringTools.trim(v);
        if (v.equals("") || v.equals(this.getAccountID())) {
            // do not set Associated AccountID if same as this Transport AccountID
            this.setFieldValue(FLD_assocAccountID, "");
        } else {
            this.setFieldValue(FLD_assocAccountID, v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the target Device ID
    *** @return v  The target Device ID
    **/
    public String getTargetDeviceID()
    {
        String assocDeviceID = this.getAssocDeviceID();
        if (!StringTools.isBlank(assocDeviceID)) {
            return assocDeviceID;
        } else {
            return this.getTransportID();
        }
    }

    /**
    *** Returns the DeviceID associated with this Transport<br>
    *** This method should return non-blank only if not equal to 'getTransportID()' and Device
    *** level indirection is desired.
    *** @return The DeviceID associated with this Transport
    **/
    public String getAssocDeviceID()
    {
        String v = (String)this.getFieldValue(FLD_assocDeviceID);
        return StringTools.trim(v);
    }
    
    /**
    *** Sets the Associated Device ID
    *** @param v The Associated Device ID
    */
    public void setAssocDeviceID(String v)
    {
        v = StringTools.trim(v);
        if (v.equals("") || v.equals(this.getTransportID())) {
            // do not set Associated DeviceID if same as this Transport TransportID
            this.setFieldValue(FLD_assocDeviceID, "");
        } else {
            this.setFieldValue(FLD_assocDeviceID, v);
        }
    }

    /**
    *** Returns true if the specified Device is the target of this Transport.
    *** @param device    The Device
    *** @return True if Device is the target of this Transport
    **/
    public boolean isAssocTargetDevice(Device device)
    {
        if (device == null) {
            return false;
        } else {
            return this.isAssocTargetDevice(device.getAccountID(), device.getDeviceID());
        }
    }

    /**
    *** Returns true if the specified Device is the target of this Transport.
    *** @param accountID  The Account ID
    *** @param deviceID   The Device ID
    *** @return True if Device is the target of this Transport
    **/
    public boolean isAssocTargetDevice(String accountID, String deviceID)
    {

        /* account match? */
        if (StringTools.isBlank(accountID) || !this.getTargetAccountID().equalsIgnoreCase(accountID)) {
            // Account not specified, no match
            return false;
        }

        /* device match */
        if (StringTools.isBlank(deviceID) || !this.getTargetDeviceID().equalsIgnoreCase(deviceID)) {
            // Device not specified, no match
            return false;
        }

        /* match */
        return true;
        
    }

    // ------------------------------------------------------------------------

    public String getDeviceCode()
    {
        String v = (String)this.getFieldValue(FLD_deviceCode);
        return StringTools.trim(v);
    }

    public void setDeviceCode(String v)
    {
        this.setFieldValue(FLD_deviceCode, StringTools.trim(v));
    }
    // ------------------------------------------------------------------------

    public String getSerialNumber()
    {
        String v = (String)this.getFieldValue(FLD_serialNumber);
        return StringTools.trim(v);
    }

    public void setSerialNumber(String v)
    {
        this.setFieldValue(FLD_serialNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getSimPhoneNumber()
    {
        String v = (String)this.getFieldValue(FLD_simPhoneNumber);
        return StringTools.trim(v);
    }

    public void setSimPhoneNumber(String v)
    {
        this.setFieldValue(FLD_simPhoneNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getSmsEmail()
    {
        String v = (String)this.getFieldValue(FLD_smsEmail);
        return StringTools.trim(v);
    }

    public void setSmsEmail(String v)
    {
        this.setFieldValue(FLD_smsEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getImeiNumber()
    {
        String v = (String)this.getFieldValue(FLD_imeiNumber);
        return StringTools.trim(v);
    }

    public void setImeiNumber(String v)
    {
        this.setFieldValue(FLD_imeiNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getDeviceType()
    {
        String v = (String)this.getFieldValue(FLD_deviceType);
        return StringTools.trim(v);
    }
    
    public void setDeviceType(String v)
    {
        this.setFieldValue(FLD_deviceType, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public long getLastInputState()
    {
        Long v = (Long)this.getFieldValue(FLD_lastInputState);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setLastInputState(long v)
    {
        this.setFieldValue(FLD_lastInputState, v & 0xFFFFFFFFL);
    }

    // ------------------------------------------------------------------------

    public int getIgnitionIndex()
    {
        Integer v = (Integer)this.getFieldValue(FLD_ignitionIndex);
        return (v != null)? v.intValue() : -1;
    }

    public void setIgnitionIndex(int v)
    {
        this.setFieldValue(FLD_ignitionIndex, v);
    }

    public int[] getIgnitionStatusCodes()
    {
        int ndx = this.getIgnitionIndex();
        if (ndx >= 0) {
            int scOFF = StatusCodes.GetDigitalInputStatusCode(ndx, false);
            int scON  = StatusCodes.GetDigitalInputStatusCode(ndx, true );
            if (scOFF != StatusCodes.STATUS_NONE) {
                return new int[] { scOFF, scON };
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    public String getCodeVersion()
    {
        String v = (String)this.getFieldValue(FLD_codeVersion);
        return StringTools.trim(v);
    }
    
    public void setCodeVersion(String v)
    {
        this.setFieldValue(FLD_codeVersion, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getFeatureSet()
    {
        String v = (String)this.getFieldValue(FLD_featureSet);
        return StringTools.trim(v);
    }
    
    public void setFeatureSet(String v)
    {
        this.setFieldValue(FLD_featureSet, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public DTIPAddrList getIpAddressValid()
    {
        DTIPAddrList v = (DTIPAddrList)this.getFieldValue(FLD_ipAddressValid);
        return v; // May return null!!
    }
    
    public void setIpAddressValid(DTIPAddrList v)
    {
        this.setFieldValue(FLD_ipAddressValid, v);
    }
    
    public void setIpAddressValid(String v)
    {
        this.setIpAddressValid((v != null)? new DTIPAddrList(v) : null);
    }

    public boolean isValidIPAddress(String ipAddr)
    {
        DTIPAddrList ipList = this.getIpAddressValid();
        if ((ipList == null) || ipList.isEmpty()) {
            return true;
        } else
        if (!ipList.isMatch(ipAddr)) {
            return false;
        } else {
            return true;
        }
    }
    
    // ------------------------------------------------------------------------

    public DTIPAddress getIpAddressCurrent()
    {
        DTIPAddress v = (DTIPAddress)this.getFieldValue(FLD_ipAddressCurrent);
        return v; // May return null!!
    }

    public void setIpAddressCurrent(DTIPAddress v)
    {
        this.setFieldValue(FLD_ipAddressCurrent, v);
    }

    public void setIpAddressCurrent(String v)
    {
        this.setIpAddressCurrent((v != null)? new DTIPAddress(v) : null);
    }

    // ------------------------------------------------------------------------

    public int getRemotePortCurrent()
    {
        Integer v = (Integer)this.getFieldValue(FLD_remotePortCurrent);
        return (v != null)? v.intValue() : 0;
    }

    public void setRemotePortCurrent(int v)
    {
        this.setFieldValue(FLD_remotePortCurrent, ((v > 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    public int getListenPortCurrent()
    {
        Integer v = (Integer)this.getFieldValue(FLD_listenPortCurrent);
        return (v != null)? v.intValue() : 0;
    }

    public void setListenPortCurrent(int v)
    {
        this.setFieldValue(FLD_listenPortCurrent, ((v > 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    public String getPendingPingCommand()
    {
        String v = (String)this.getFieldValue(FLD_pendingPingCommand);
        return StringTools.trim(v);
    }

    public void setPendingPingCommand(String v)
    {
        this.setFieldValue(FLD_pendingPingCommand, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public long getLastPingTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastPingTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastPingTime(long v)
    {
        this.setFieldValue(FLD_lastPingTime, v);
    }

    public void setLastPingTime(long v)
    {
        this._setLastPingTime(v);
        if (this.assocDevice != null) {
            this.assocDevice._setLastPingTime(v);
        }
    }

    // ------------------------------------------------------------------------

    public int getTotalPingCount()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalPingCount);
        return (v != null)? v.intValue() : 0;
    }

    public void _setTotalPingCount(int v)
    {
        this.setFieldValue(FLD_totalPingCount, v);
    }

    public void setTotalPingCount(int v)
    {
        this._setTotalPingCount(v);
        if (this.assocDevice != null) {
            this.assocDevice._setLastPingTime(v);
        }
    }

    // ------------------------------------------------------------------------

    public int getMaxPingCount()
    {
        Integer v = (Integer)this.getFieldValue(FLD_maxPingCount);
        return (v != null)? v.intValue() : 0;
    }

    public void _setMaxPingCount(int v)
    {
        this.setFieldValue(FLD_maxPingCount, v);
    }

    public void setMaxPingCount(int v)
    {
        this._setMaxPingCount(v);
        if (this.assocDevice != null) {
            this.assocDevice._setMaxPingCount(v);
        }
    }

    // ------------------------------------------------------------------------

    public boolean getExpectAck()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_expectAck);
        return (v != null)? v.booleanValue() : true;
    }

    public void _setExpectAck(boolean v)
    {
        this.setFieldValue(FLD_expectAck, v);
    }

    public void setExpectAck(boolean v)
    {
        this._setExpectAck(v);
        if (this.assocDevice != null) {
            this.assocDevice._setExpectAck(v);
        }
    }

    // ------------------------------------------------------------------------

    public String getLastAckCommand()
    {
        String v = (String)this.getFieldValue(FLD_lastAckCommand);
        return StringTools.trim(v);
    }

    public void setLastAckCommand(String v)
    {
        this.setFieldValue(FLD_lastAckCommand, StringTools.trim(v));
    }

    public boolean isExpectingCommandAck()
    {
        return this.getExpectAck() && (this.getLastAckTime() <= 0L);
    }

    // ------------------------------------------------------------------------

    public long getLastAckTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastAckTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastAckTime(long v)
    {
        this.setFieldValue(FLD_lastAckTime, v);
    }

    public void setLastAckTime(long v)
    {
        this._setLastAckTime(v);
        if (this.assocDevice != null) {
            this.assocDevice._setLastAckTime(v);
        }
    }

    // ------------------------------------------------------------------------

    public boolean getSupportsDMTP()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_supportsDMTP);
        return (v != null)? v.booleanValue() : true;
    }

    public void setSupportsDMTP(boolean v)
    {
        this.setFieldValue(FLD_supportsDMTP, v);
    }
    
    public boolean supportsDMTP()
    {
        return this.getSupportsDMTP();
    }

    // ------------------------------------------------------------------------

    public int getSupportedEncodings()
    {
        Integer v = (Integer)this.getFieldValue(FLD_supportedEncodings);
        return (v != null)? v.intValue() : (int)Encodings.BINARY.getLongValue();
    }
    
    public void setSupportedEncodings(int v)
    {
        v &= (int)EnumTools.getValueMask(Encodings.class);
        if (v == 0) { v = (int)Encodings.BINARY.getLongValue(); }
        this.setFieldValue(FLD_supportedEncodings, v);
    }

    // ------------------------------------------------------------------------

    public int getUnitLimitInterval() // Minutes
    {
        Integer v = (Integer)this.getFieldValue(FLD_unitLimitInterval);
        return (v != null)? v.intValue() : 0;
    }

    public void setUnitLimitInterval(int v) // Minutes
    {
        this.setFieldValue(FLD_unitLimitInterval, v);
    }

    // ------------------------------------------------------------------------

    public int getMaxAllowedEvents()
    {
        Integer v = (Integer)this.getFieldValue(FLD_maxAllowedEvents);
        return (v != null)? v.intValue() : 1;
    }

    public void setMaxAllowedEvents(int max)
    {
        this.setFieldValue(FLD_maxAllowedEvents, max);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: */
    public DTProfileMask getTotalProfileMask()
    {
        DTProfileMask v = (DTProfileMask)this.getFieldValue(FLD_totalProfileMask);
        return v;
    }

    public void setTotalProfileMask(DTProfileMask v)
    {
        this.setFieldValue(FLD_totalProfileMask, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Total Connections per Interval */
    // The effective maximum value for this field is defined by the following:
    //   (org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK * this.getUnitLimitIntervalMinutes())
    public int getTotalMaxConn()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalMaxConn);
        return (v != null)? v.intValue() : 0;
    }

    public void setTotalMaxConn(int v)
    {
        this.setFieldValue(FLD_totalMaxConn, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Total Connections per Minute */
    // The effective maximum value for this field is defined by the constant:
    //   "org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK"
    public int getTotalMaxConnPerMin()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalMaxConnPerMin);
        return (v != null)? v.intValue() : 0;
    }

    public void setTotalMaxConnPerMin(int v)
    {
        this.setFieldValue(FLD_totalMaxConnPerMin, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: */
    public DTProfileMask getDuplexProfileMask()
    {
        DTProfileMask v = (DTProfileMask)this.getFieldValue(FLD_duplexProfileMask);
        return v;
    }

    public void setDuplexProfileMask(DTProfileMask v)
    {
        this.setFieldValue(FLD_duplexProfileMask, v);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Duplex Connections per Interval */
    // The effective maximum value for this field is defined by the following:
    //   (org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK * this.getUnitLimitIntervalMinutes())
    public int getDuplexMaxConn()
    {
        Integer v = (Integer)this.getFieldValue(FLD_duplexMaxConn);
        return (v != null)? v.intValue() : 0;
    }

    public void setDuplexMaxConn(int max)
    {
        this.setFieldValue(FLD_duplexMaxConn, max);
    }

    // ------------------------------------------------------------------------

    /* OpenDMTP: Maximum Duplex Connections per Minute */
    // The effective maximum value for this field is defined by the constant:
    //   "org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK"
    public int getDuplexMaxConnPerMin()
    {
        Integer v = (Integer)this.getFieldValue(FLD_duplexMaxConnPerMin);
        return (v != null)? v.intValue() : 0;
    }

    public void setDuplexMaxConnPerMin(int max)
    {
        this.setFieldValue(FLD_duplexMaxConnPerMin, max);
    }

    // ------------------------------------------------------------------------

    public long getLastTotalConnectTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastTotalConnectTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastTotalConnectTime(long v)
    {
        this.setFieldValue(FLD_lastTotalConnectTime, v);
    }

    public void setLastTotalConnectTime(long v)
    {
        this._setLastTotalConnectTime(v);
        if (this.assocDevice != null) {
            this.assocDevice._setLastTotalConnectTime(v);
        }
    }

    // ------------------------------------------------------------------------

    public long getLastDuplexConnectTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastDuplexConnectTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void _setLastDuplexConnectTime(long v)
    {
        this.setFieldValue(FLD_lastDuplexConnectTime, v);
    }

    public void setLastDuplexConnectTime(long v)
    {
        this._setLastDuplexConnectTime(v);
        if (this.assocDevice != null) {
            this.assocDevice._setLastDuplexConnectTime(v);
        }
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription(DEFAULT_XPORT_NAME);
        this.setSupportedEncodings(DEFAULT_ENCODING);
        this.setTotalMaxConn(DEFAULT_TOTAL_MAX_CONNECTIONS);
        this.setDuplexMaxConn(DEFAULT_DUPLEX_MAX_CONNECTIONS);
        this.setUnitLimitInterval(DEFAULT_UNIT_LIMIT_INTERVAL_MIN); // Minutes
        this.setTotalMaxConnPerMin(DEFAULT_TOTAL_MAX_CONNECTIONS_PER_MIN);
        this.setDuplexMaxConnPerMin(DEFAULT_DUPLEX_MAX_CONNECTIONS_PER_MIN);
        this.setMaxAllowedEvents(DEFAULT_MAX_ALLOWED_EVENTS);
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    private Account assocAccount = null;
    private Device  assocDevice  = null;

    protected Account getAssocAccount()
    {
        if (this.assocAccount == null) {
            String acctID = this.getAssocAccountID();
            if (StringTools.isBlank(acctID) || acctID.equals(this.getAccountID())) {
                // most likely to occur for most applications
                this.assocAccount = this.getAccount();
            } else {
                // get custom Associated Account
                try {
                    this.assocAccount = Account.getAccount(acctID);
                } catch (DBException dbe) {
                    // may be caused by "java.net.ConnectException: Connection refused: connect"
                    Print.logError("Associated Account not found: " + acctID);
                    this.assocAccount = null;
                }
            }
        }
        return this.assocAccount;
    }

    public Device getAssocDevice()
    {
        if (this.assocDevice == null) {
            String devID = this.getAssocDeviceID(); // associated device id
            try {
                if (!StringTools.isBlank(devID)) {
                    this.assocDevice = Device.getDevice(this.getAssocAccount(), devID);
                    if (this.assocDevice != null) {
                        this.assocDevice.setTransport(this);
                    } else {
                        Print.logError("**** Transport Device not found: " + this + " ==> " + devID);
                    }
                } else {
                    devID = this.getTransportID();
                    this.assocDevice = Device.getDevice(this.getAssocAccount(), devID);
                    if (this.assocDevice != null) {
                        Print.logWarn("TransportID used for DeviceID: " + devID);
                        this.assocDevice.setTransport(this);
                    } else {
                        Print.logWarn("**** Device not defined for Transport: " + this.getAccountID() + "/" + this.getTransportID());
                    }
                }
            } catch (DBException dbe) {
                // This Transport doesn't have an account?
                Print.logError("**** Error reading Device for Transport: " + this.getAccountID() + "/" + this.getTransportID() + "/" + devID);
                this.assocDevice = null;
            }
        }
        return this.assocDevice;
    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        return this.getAccountID() + "/" + this.getTransportID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String xportID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (xportID != null)) {
            Transport.Key xportKey = new Transport.Key(acctID, xportID);
            return xportKey.exists();
        }
        return false;
    }

    public static boolean exists(String uniqID)
        throws DBException // if error occurs while testing existance
    {
        if (!StringTools.isBlank(uniqID)) {
            Transport xport = Transport.getTransportByUniqueID(uniqID);
            return (xport != null);
        }
        return false;
    }

    public static boolean exists(String prefix[], String mobileID)
        throws DBException // if error occurs while testing existance
    {
        if (StringTools.isBlank(mobileID)) {
            return false;
        } else
        if (ListTools.isEmpty(prefix)) {
            return Transport.exists(mobileID);
        } else {
            for (int i = 0; i < prefix.length; i++) {
                String uniqueID = prefix[i] + mobileID;
                if (Transport.exists(uniqueID)) {
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /* get Transport by UniqueID */
    // may return null
    public static Transport getTransportByUniqueID(String uniqId)
        throws DBException
    {
        
        /* invalid id? */
        if (StringTools.isBlank(uniqId)) {
            return null; // just say it doesn't exist
        }
        
        /* read Transport for unique-id */
        Transport  xport = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
        
            /* select */
            // DBSelect: SELECT * FROM Transport WHERE (uniqueID='unique')
            DBSelect<Transport> dsel = new DBSelect<Transport>(Transport.getFactory());
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(dwh.EQ(Transport.FLD_uniqueID,uniqId)));
            dsel.setLimit(2);
            // Note: The index on the column FLD_uniqueID does not enforce uniqueness
            // (since null/empty values are allowed and needed)

            /* get record */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String acctId  = rs.getString(FLD_accountID);
                String xportId = rs.getString(FLD_transportID);
                xport = new Transport(new Transport.Key(acctId,xportId));
                xport.setAllFieldValues(rs);
                if (rs.next()) {
                    Print.logError("Found multiple occurances of this unique-id: " + uniqId);
                }
                break; // only one record
            }
            // it's possible at this point that we haven't even read 1 device

        } catch (SQLException sqe) {
            throw new DBException("Getting Transport for UniqueID", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return Transport */
        // Note: 'xport' may be null if it wasn't found
        return xport;

    }

    // ------------------------------------------------------------------------

    /* get Transport for Account/Transport ID */
    // may return null
    public static Transport getTransport(Account account, String xportID)
        throws DBException
    {
        if ((account != null) && (xportID != null)) {
            String acctID = account.getAccountID();
            Transport.Key key = new Transport.Key(acctID, xportID);
            if (key.exists()) {
                Transport xport = key.getDBRecord(true);
                xport.setAccount(account);
                return xport;
            } else {
                // Transport does not exist
                return null;
            }
        } else {
            return null; // just say it doesn't exist
        }
    }

    /* get Transport */
    // Note: does NOT return null
    public static Transport getTransport(Account account, String xportID, boolean create)
        throws DBException
    {
        
        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctID = account.getAccountID();
        
        /* transport-id specified? */
        if (StringTools.isBlank(xportID)) {
            throw new DBNotFoundException("Transport-ID not specified for account: " + acctID);
        }

        /* get/create */
        Transport xport = null;
        Transport.Key xportKey = new Transport.Key(acctID, xportID);
        if (!xportKey.exists()) {
            if (create) {
                xport = xportKey.getDBRecord();
                xport.setAccount(account);
                xport.setCreationDefaultValues();
                return xport; // not yet saved!
            } else {
                throw new DBNotFoundException("Transport-ID does not exists: " + xportKey);
            }
        } else
        if (create) {
            // we've been asked to create the Transport, and it already exists
            throw new DBAlreadyExistsException("Transport-ID already exists '" + xportKey + "'");
        } else {
            xport = Transport.getTransport(account, xportID);
            if (xport == null) {
                throw new DBException("Unable to read existing Transport: " + xportKey);
            }
            return xport;
        }
        
    }
    
    // ------------------------------------------------------------------------

    /* create Transport */
    public static Transport createNewTransport(Account account, String xportID, String uniqueID)
        throws DBException
    {
        if ((account != null) && (xportID != null) && !xportID.equals("")) {
            Transport xport = Transport.getTransport(account, xportID, true); // does not return null
            if ((uniqueID != null) && !uniqueID.equals("")) {
                xport.setUniqueID(uniqueID);
            }
            xport.save();
            return xport;
        } else {
            throw new DBException("Invalid Account/TransportID specified");
        }
    }
    
    /* create Transport from Device */
    public static Transport createNewTransport(Device device)
        throws DBException
    {
        
        /* invalid device */
        if (device == null) {
            throw new DBNotFoundException("Invalid Device specified");
        }
        
        /* Transport already defined for Device? */
        DataTransport dt = device.getDataTransport();
        if (dt instanceof Transport) {
            throw new DBAlreadyExistsException("Device already has a defined Transport");
        }
        
        /* create Transport from Device default DataTransport */
        Account account  = device.getAccount();
        String xportID   = device.getDeviceID();
        Transport xport  = Transport.getTransport(account, xportID, true);   // does not return null
        xport.setUniqueID(              dt.getUniqueID()          );
        xport.setAssocAccountID(        dt.getAssocAccountID()    );
        xport.setAssocDeviceID(         dt.getAssocDeviceID()     );
        xport.setDescription(           dt.getDescription()       );
        xport.setDeviceCode(            dt.getDeviceCode()        );
        xport.setSerialNumber(          dt.getSerialNumber()      );
        xport.setSimPhoneNumber(        dt.getSimPhoneNumber()    );
        xport.setImeiNumber(            dt.getImeiNumber()        );
        xport.setDeviceType(            dt.getDeviceType()        );
        xport.setLastInputState(        dt.getLastInputState()    );
        xport.setCodeVersion(           dt.getCodeVersion()       );
        xport.setFeatureSet(            dt.getFeatureSet()        );
        xport.setIpAddressValid(        dt.getIpAddressValid()    );
        xport.setIpAddressCurrent(      dt.getIpAddressCurrent()  );
        xport.setSupportsDMTP(          dt.getSupportsDMTP()      );
        xport.setSupportedEncodings(    dt.getSupportedEncodings());
        xport.setUnitLimitInterval(     dt.getUnitLimitInterval() );
        xport.setMaxAllowedEvents(      dt.getMaxAllowedEvents()  );
        xport.setTotalProfileMask(      dt.getTotalProfileMask()  );
        xport.setTotalMaxConn(          dt.getTotalMaxConn()      );
        xport.setTotalMaxConnPerMin(    dt.getTotalMaxConnPerMin());
        xport.setDuplexProfileMask(     dt.getDuplexProfileMask() );
        xport.setDuplexMaxConn(         dt.getDuplexMaxConn()     );
        xport.setDuplexMaxConnPerMin(   dt.getDuplexMaxConnPerMin());
        xport.setLastTotalConnectTime(  dt.getLastTotalConnectTime());
        xport.setLastDuplexConnectTime( dt.getLastDuplexConnectTime());
        xport.save();
        device.setTransport(xport);
        return xport;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return list of all Transports owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getTransportsForAccount(String acctId)
        throws DBException
    {
        
        /* no account specified? */
        if (StringTools.isBlank(acctId)) {
            Print.logError("Account not specified!");
            return new String[0];
        }

        /* select */
        // DBSelect: SELECT * FROM Transport WHERE (accountID='acct') ORDER BY transportID
        DBSelect<Transport> dsel = new DBSelect<Transport>(Transport.getFactory());
        dsel.setSelectedFields(Transport.FLD_transportID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(dwh.WHERE(dwh.EQ(Transport.FLD_accountID,acctId)));
        dsel.setOrderByFields(Transport.FLD_transportID);

        /* return list */
        return Transport.getTransports(dsel);

    }

    /* return list of all Transports owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getTransports(DBSelect<Transport> dsel)
        throws DBException
    {

        /* invalid DBSelect */
        if (dsel == null) {
            return new String[0];
        }

        /* read Transports for account */
        java.util.List<String> xportList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String xportId = rs.getString(Transport.FLD_transportID);
                xportList.add(xportId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account Transport List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return xportList.toArray(new String[xportList.size()]);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** This method is used by Device Communication Servers to load a Device record based
    *** on a Unique-ID.  Returns null if the Device is found, but the Account or Device are
    *** inactive.
    *** @param prefix   An array of Unique-ID prefixes.
    *** @param modemID  The Unique-ID of the device (ie. IMEI, ESN, Serial#, etc)
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceUniqueID(String prefix[], String modemID)
    {
        Device device = null;

        /* find Device */
        String uniqueID = "";
        try {

            /* load device record */
            if (ListTools.isEmpty(prefix)) {
                uniqueID = modemID;
                //Print.logDebug("Looking for UniqueID: " + uniqueID);
                device = Transport.loadDeviceByUniqueID(uniqueID);
            } else {
                uniqueID = prefix[0] + modemID;
                for (int u = 0; u < prefix.length; u++) {
                    String pfxid = prefix[u] + modemID;
                    //Print.logDebug("Looking for UniqueID: " + pfxid);
                    device = Transport.loadDeviceByUniqueID(pfxid);
                    if (device != null) {
                        uniqueID = pfxid;
                        break;
                    }
                }
            }

            /* not found? */
            if (device == null) {
                Print.logWarn("!!!UniqueID not found!: " + uniqueID);
                return null;
            }

            /* inactive? */
            if (!device.getAccount().isActive() || !device.isActive()) {
                String a = device.getAccountID();
                String d = device.getDeviceID();
                Print.logWarn("Account/Device is inactive: " + a + "/" + d + " [" + uniqueID + "]");
                return null;
            }

            /* return device */
            device.setModemID(modemID);
            return device;

        } catch (Throwable dbe) { // DBException
            Print.logError("Exception getting Device: " + uniqueID + " [" + dbe + "]");
            return null;
        }

    }

    /**
    *** This method is used by Device Communication Servers to load a Device record based
    *** on a Unique-ID.  The caller must confirm that the Device and Account are active.
    *** @param uniqId  The Unique-ID of the device (ie. IMEI, ESN, Serial#, etc)
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByUniqueID(String uniqId)
        throws DBException
    {

        /* invalid id? */
        if (StringTools.isBlank(uniqId)) {
            // not likely to occur
            Print.logError("Unique-ID is null!");
            return null; // just say it doesn't exist
        }

        /* lookup UniqueXID entry? */
        if (UniqueXID.isUniqueQueryEnabled()) {
            UniqueXID uniqXp = null;
            try {
                uniqXp = UniqueXID.getUniqueXID(uniqId);
            } catch (DBException dbe) {
                // ignore this error
            }
            if (uniqXp != null) {
                String a = uniqXp.getAccountID();
                String t = uniqXp.getTransportID();
                Print.logDebug("Located Transport '"+a+"/"+t+"' via UniqueXID '"+uniqXp+"'");
                return Transport.loadDeviceByTransportID(a, t);
            } else {
                // continue below
            }
        }

        /* lookup Transport entry */
        if (Transport.isTransportQueryEnabled()) {
            try {
                Transport xport = Transport.getTransportByUniqueID(uniqId);
                if (xport != null) {
                    // a Transport entry was found
                    Print.logDebug("Located Transport '"+xport+"' via UniqueID '"+uniqId+"'");

                    /* update the Transport connect time */
                    try {
                        xport.setLastTotalConnectTime(DateTime.getCurrentTimeSec());
                        xport.update(Transport.FLD_lastTotalConnectTime);
                    } catch (DBException dbe) {
                        Print.logError("Error updating connect time: " + dbe);
                        // otherwise ignore this error
                    }

                    /* get the associated Device record */
                    Device dev = xport.getAssocDevice();
                    if (dev != null) {
                        // this Transport Device was also found
                        Print.logDebug("Located Device '"+dev+"' (via Transport '"+xport+"')");
                        return dev;
                    } else {
                        // this Transport record does not reference a device (error already displayed)
                        // we probably should return null here, but instead we will try again using the Device below
                        //return null;
                    }

                }
            } catch (DBException dbe) {
                // ignore this error
            }
        }

        /* return device */
        Device device = Device.loadDeviceByUniqueID(uniqId);
        if (device != null) {
            // if we are here, then the Device exists and no Transport record references this device
            Print.logDebug("Located Device '"+device+"' (via Device record)");
            return device;
        }

        /* transport/device not found */
        //Print.logWarn("Device not found for UniqueID '"+uniqId+"'");
        return null;

    }

    /**
    *** This method is used by Device Communication Servers to load a Device record based
    *** on a Unique-ID.
    *** @param uniqID  The Unique-ID of the device (ie. IMEI, ESN, Serial#, etc)
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByUniqueID(byte uniqID[])
        throws DBException
    {
        if (uniqID != null) {
            // first try ASCII
            if (StringTools.isPrintableASCII(uniqID,false)) {
                Device dev = Transport.loadDeviceByUniqueID(StringTools.toStringValue(uniqID));
                if (dev != null) {
                    return dev;
                }
            }
            // then try HEX
            String hexId = StringTools.toHexString(uniqID);
            return Transport.loadDeviceByUniqueID(hexId);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** This method is used by Device Communication Servers to load a Device record based
    *** on the Account and Transport/Device IDs.
    *** @param accountID  The Account ID of the owning account.
    *** @param xportID    The Transport-ID (or Device-ID in some cases).
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByTransportID(String accountID, String xportID)
        throws DBException
    {

        /* no account/transport specified? */
        if (accountID == null) {
            Print.logError("Account-ID is null!");
            return null; // just say it doesn't exist
        } else
        if (xportID == null) {
            // not likely to occur
            Print.logError("Device/Transport-ID is null!");
            return null; // just say it doesn't exist
        }

        /* get account */
        Account account = Account.getAccount(accountID); // may throw DBException
        if (account == null) {
            Print.logError("Account-ID does not exist: " + accountID);
            return null;
        }
        
        /* load Transport from device */
        return Transport.loadDeviceByTransportID(account, xportID);

    }

    /**
    *** This method is used by Device Communication Servers to load a Device record based
    *** on the Account and Transport/Device IDs.
    *** @param account  The Account instance representing the owning account.
    *** @param xportID  The Transport-ID (or Device-ID in some cases).
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByTransportID(Account account, String xportID)
        throws DBException
    {

        /* no account/transport specified? */
        if (account == null) {
            Print.logError("Account is null (not found/defined?)!");
            return null; // just say it doesn't exist
        } else 
        if (xportID == null) {
            Print.logError("Device/Transport-ID is null!");
            return null; // just say it doesn't exist
        }

        /* lookup Transport entry */
        if (Transport.isTransportQueryEnabled()) {
            try {
                Transport xport = Transport.getTransport(account, xportID);
                if (xport != null) {
                    // a Transport entry was found
                    Device dev = xport.getAssocDevice();
                    if (dev != null) {
                        // this Transport Device was also found
                        Print.logInfo("Located Device '"+dev+"' via Transport '"+xport+"'");
                        return dev;
                    } else {
                        // this Transport record does not reference a device (error already displayed)
                        // we probably should return null here, but instead we will try again using the Device below
                        //return null;
                    }
                }
            } catch (DBException dbe) {
                // ignore this error
            }
        }

        /* return device */
        Device dev = Device.loadDeviceByName(account, xportID);
        if (dev != null) {
            // if we are here, then the Device exists and no Transport record references this device
            Print.logInfo("Located Device '"+dev+"' (using default Device transport)");
            return dev;
        }

        /* Auto-Add Devices? */
        if (account.getAutoAddDevices()) {
            // Create a 'Device'
            dev = Device.getDevice(account, xportID, true); // create
            if (dev != null) {
                dev.setDescription(account.getNewDeviceDescription(null,"!"+xportID));
                dev.save();
                Print.logInfo("Created new Device '"+dev+"'");
                return dev;
            }
        }
        
        /* transport/device not found */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // return suggested OpenDMTP communication property configuration value
    public static String getSuggestedDMTPConnectionAttribute(int prop, DataTransport dt)
    {
        switch (prop) {
            case PROP_COMM_MAX_CONNECTIONS: {
                // com.maxconn=20,12,60
                int totmax = dt.getTotalMaxConn();
                int dupmax = dt.getDuplexMaxConn();
                int interv = dt.getUnitLimitInterval(); // Minutes
                return PROP_COMM_MAX_CONNECTIONS_STR+"="+totmax+","+dupmax+","+interv;
            }
            case PROP_COMM_MIN_XMIT_DELAY: { // standard minimum (seconds)
                // com.mindelay=300
                int interv = (int)DateTime.MinuteSeconds(dt.getUnitLimitInterval());
                int dupmax = dt.getDuplexMaxConn();
                int delmin = (int)Math.round((double)interv / (double)dupmax);
                if (delmin < 60) { delmin = 60; }
                return PROP_COMM_MIN_XMIT_DELAY_STR+"="+delmin;
            }
            case PROP_COMM_MIN_XMIT_RATE: { // absolute minimum (seconds)
                // com.minrate=60
                int maxTotalConnPerMin  = dt.getTotalMaxConnPerMin();
                int maxDuplexConnPerMin = dt.getDuplexMaxConnPerMin();
                int totminrate = (int)Math.round(60.0 / (double)maxTotalConnPerMin);
                int dupminrate = (int)Math.round(60.0 / (double)maxDuplexConnPerMin);
                int minrate    = (dupminrate > totminrate)? dupminrate : totminrate;
                return PROP_COMM_MIN_XMIT_RATE_STR+"="+minrate;
            }
            case PROP_COMM_MAX_XMIT_RATE: {
                // com.maxrate=3600
                return PROP_COMM_MAX_XMIT_RATE_STR+"=3600";
            }
            case PROP_COMM_MAX_DUP_EVENTS: {
                // com.maxduplex=10
                return PROP_COMM_MAX_DUP_EVENTS_STR+"=10";
            }
            case PROP_COMM_MAX_SIM_EVENTS: {
                // com.maxsimplex=2
                return PROP_COMM_MAX_SIM_EVENTS_STR+"=2";
            }
            default: {
                return "";
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account"  , "acct"    };
    private static final String ARG_TRANSPORT[] = new String[] { "transport", "xport"   };
    private static final String ARG_UNIQID[]    = new String[] { "uniqueid" , "unique", "uid" };
    private static final String ARG_DEVICE[]    = new String[] { "device"   , "dev"     };
    private static final String ARG_CREATE[]    = new String[] { "create"               };
    private static final String ARG_EDIT[]      = new String[] { "edit"     , "ed"      };
    private static final String ARG_EDITALL[]   = new String[] { "editall"  , "eda"     };
    private static final String ARG_DELETE[]    = new String[] { "delete"               };
    private static final String ARG_ORPHANS[]   = new String[] { "orphan"   , "orphans" };

    private static String _fmtXPortID(String acctID, String xportID)
    {
        return acctID + "/" + xportID;
    }

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Transport.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns the Transport");
        Print.logInfo("  -transport=<id> Transport ID to create/edit");
        Print.logInfo("  -uniqueid=<id>  Unique ID to create/edit");
        Print.logInfo("  -device=<id>    Device ID from which the Transport is created");
        Print.logInfo("  -create         Create a new Transport");
        Print.logInfo("  -edit[all]      Edit an existing (or newly created) Transport");
        Print.logInfo("  -delete         Delete specified Transport");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT  , "");
        String xportID = RTConfig.getString(ARG_TRANSPORT, "");
        String uniqID  = RTConfig.getString(ARG_UNIQID   , "");
        String devID   = RTConfig.getString(ARG_DEVICE   , "");

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may throw DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* transport-id specified? */
        //if (StringTools.isBlank(xportID)) {
        //    Print.logError("Transport-ID not specified.");
        //    usage();
        //}

        /* device transport exists? */
        boolean xportExists = false;
        try {
            xportExists = StringTools.isBlank(xportID)? false : Transport.exists(acctID, xportID);
        } catch (DBException dbe) {
            Print.logError("Error determining if Transport exists: " + _fmtXPortID(acctID,xportID));
            System.exit(99);
        }

        /* option count */
        int opts = 0;

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !StringTools.isBlank(acctID) && !StringTools.isBlank(xportID)) {
            opts++;
            if (!xportExists) {
                Print.logWarn("Transport does not exist: " + _fmtXPortID(acctID,xportID));
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                Transport.Key xportKey = new Transport.Key(acctID, xportID);
                xportKey.delete(true); // also delete dependencies
                Print.logInfo("Transport deleted: " + _fmtXPortID(acctID,xportID));
                xportExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting Transport: " + _fmtXPortID(acctID,xportID));
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (xportExists) {
                Print.logWarn("Transport already exists: " + _fmtXPortID(acctID,xportID));
            } else
            if (!StringTools.isBlank(devID)) {
                if (!StringTools.isBlank(xportID)) {
                    Print.logError("Transport-ID must not be specified when Device-ID is specified.");
                } else {
                    try {
                        Device dev = Device.getDevice(acct, devID);
                        if (dev != null) {
                            Transport.createNewTransport(dev);
                            Print.logInfo("Created Transport (from device): " + _fmtXPortID(acctID,devID));
                            xportExists = true;
                        } else {
                            Print.logError("Device-ID does not exist: " + devID);
                            System.exit(99);
                        }
                    } catch (DBException dbe) {
                        Print.logError("Error creating Transport: " + _fmtXPortID(acctID,devID));
                        dbe.printException();
                        System.exit(99);
                    }
                }
            } else
            if (StringTools.isBlank(xportID)) {
                Print.logError("Transport-ID not specified.");
            } else {
                try {
                    Transport.createNewTransport(acct, xportID, uniqID);
                    Print.logInfo("Created Transport: " + _fmtXPortID(acctID,xportID));
                    xportExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating Transport: " + _fmtXPortID(acctID,xportID));
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!xportExists) {
                Print.logError("Transport does not exist: " + _fmtXPortID(acctID,xportID));
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    Transport transport = Transport.getTransport(acct, xportID, false); // may throw DBException
                    DBEdit editor = new DBEdit(transport);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing Transport: " + _fmtXPortID(acctID,xportID));
                    dbe.printException();
                }
            }
            System.exit(0);
        }

        /* list orphans (Transport records exists, but Device record does not) */
        if (RTConfig.getBoolean(ARG_ORPHANS,false)) {
            opts++;
            // selection
            DBSelect<Transport> dsel = new DBSelect<Transport>(Transport.getFactory());
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(dwh.EQ(Transport.FLD_accountID,acctID)));
            // iterate through records
            DBConnection dbc = null;
            Statement   stmt = null;
            ResultSet     rs = null;
            try {
                long rcdCount = 0L, orphanCount = 0L;
                Print.sysPrintln("Listing orphaned Transport records:");
                dbc  = DBConnection.getDefaultConnection();
                stmt = dbc.execute(dsel.toString());
                rs   = stmt.getResultSet();
                while (rs.next()) {
                    rcdCount++;
                    String transportID = rs.getString(Transport.FLD_transportID);
                    String uniqueID    = rs.getString(Transport.FLD_uniqueID);
                    String assocAcctID = rs.getString(Transport.FLD_assocAccountID);
                    String assocDevID  = rs.getString(Transport.FLD_assocDeviceID);
                    if (StringTools.isBlank(assocAcctID) || assocAcctID.equalsIgnoreCase(acctID)) {
                        assocAcctID = null;
                    }
                    if (StringTools.isBlank(assocDevID) || assocDevID.equalsIgnoreCase(transportID)) {
                        assocDevID = null;
                    }
                    boolean isAssoc  = StringTools.isBlank(assocAcctID) || StringTools.isBlank(assocDevID);
                    String accountID = !StringTools.isBlank(assocAcctID)? assocAcctID : acctID;
                    String deviceID  = !StringTools.isBlank(assocDevID)?  assocDevID  : transportID;
                    if (!Device.exists(accountID,deviceID)) {
                        Print.sysPrintln("   Device does not exist ["+transportID+"]: " + accountID + "/" + deviceID);
                        orphanCount++;
                    }
                }
                Print.sysPrintln("   Found %d orphaned records [out of %d]", orphanCount, rcdCount);
            } catch (SQLException sqe) {
                Print.logException("Error checking orphaned TransportIDs", sqe);
            } catch (DBException dbe) {
                Print.logException("Error checking orphaned TransportIDs", dbe);
            } finally {
                if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
                if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
                DBConnection.release(dbc);
            }
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }
    
}
