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
//  2008/03/28  Martin D. Flynn
//     -Extracted from org.opendmtp.codes.ClientErrors
// ----------------------------------------------------------------------------
package org.opengts.db.dmtp;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;

public class ClientErrors
    implements org.opendmtp.codes.ClientErrors
{

    // ------------------------------------------------------------------------

    /**
    *** Gets the code description
    *** @param errCode  The error code
    *** @return The error description
    **/
    public static String getDescription(int errCode)
    {
        return getStringValue(errCode, true, null, null);
    }

    /**
    *** Gets the String representation of the specified data for the specified error code
    *** @param errCode  The error code
    *** @param errData  The binary payload data
    *** @return The value String representation
    **/
    public static String getStringValue(int errCode, byte errData[])
    {
        return getStringValue(errCode, false, errData, null);
    }

    /**
    *** Gets the String representation of the specified data for the specified error code
    *** @param errCode   The error code
    *** @param errData   The binary payload data
    *** @param tz        A TimeZone used convert any dates encountered
    *** @return The value String representation
    **/
    public static String getStringValue(int errCode, byte errData[], TimeZone tz)
    {
        return getStringValue(errCode, false, errData, tz);
    }

    /**
    *** Gets the Description and/or converts the specified binary value to a String
    *** @param errCode   The error code
    *** @param inclDesc  True to include the description, false to omit
    *** @param errData   The binary payload data
    *** @return The value String representation
    **/
    public static String getStringValue(int errCode, boolean inclDesc, byte errData[])
    {
        return getStringValue(errCode, inclDesc, errData, null);
    }
    
    /**
    *** Gets the Description and/or converts the specified binary value to a String
    *** @param errCode   The error code
    *** @param inclDesc  True to include the description, false to omit
    *** @param errData   The binary payload data
    *** @param tz        A TimeZone used convert any dates encountered
    *** @return The Description and/or value String representation
    **/
    public static String getStringValue(int errCode, boolean inclDesc, byte errData[], TimeZone tz)
    {
        Payload payload = ((errData != null) && (errData.length > 0))? new Payload(errData) : null;
        StringBuffer sb = new StringBuffer();
        switch (errCode) {
            case ERROR_PACKET_HEADER: 
                if (inclDesc) {
                    sb.append("Invalid packet header");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PACKET_TYPE:
                if (inclDesc) {
                    sb.append("Invalid packet type");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PACKET_LENGTH:
                if (inclDesc) {
                    sb.append("Invalid packet length");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PACKET_ENCODING:
                if (inclDesc) {
                    sb.append("Unsupported packet encoding");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PACKET_PAYLOAD:          
                if (inclDesc) {
                    sb.append("Invalid packet payload");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PACKET_CHECKSUM:
                if (inclDesc) {
                    sb.append("Invalid checksum");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PACKET_ACK:
                if (inclDesc) {
                    sb.append("Invalid ACL sequence");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PROTOCOL_ERROR:
                if (inclDesc) {
                    sb.append("Protocol error");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String h = StringTools.toHexString(payload.readLong(1,0L),8);
                    String t = StringTools.toHexString(payload.readLong(1,0L),8);
                    sb.append("0x" + h + t);
                }
                return sb.toString();
            case ERROR_PROPERTY_READ_ONLY:
                if (inclDesc) {
                    sb.append("Property is read-only");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String p = StringTools.toHexString(payload.readLong(2,0L),16);
                    sb.append("0x" + p);
                }
                return sb.toString();
            case ERROR_PROPERTY_WRITE_ONLY:
                if (inclDesc) {
                    sb.append("Property is write-only");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String p = StringTools.toHexString(payload.readLong(2,0L),16);
                    sb.append("0x" + p);
                }
                return sb.toString();
            case ERROR_PROPERTY_INVALID_ID:
                if (inclDesc) {
                    sb.append("Invalid/Unrecognized property key");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String p = StringTools.toHexString(payload.readLong(2,0L),16);
                    sb.append("0x" + p);
                }
                return sb.toString();
            case ERROR_PROPERTY_INVALID_VALUE:
                if (inclDesc) {
                    sb.append("Invalid property value");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String p = StringTools.toHexString(payload.readLong(2,0L),16);
                    sb.append("0x" + p);
                }
                return sb.toString();
            case ERROR_PROPERTY_UNKNOWN_ERROR:
                if (inclDesc) {
                    sb.append("Unknown property error");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String p = StringTools.toHexString(payload.readLong(2,0L),16);
                    sb.append("0x" + p);
                }
                return sb.toString();
            case ERROR_COMMAND_INVALID:
                if (inclDesc) {
                    sb.append("Invalid/Unsupported command");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String c = StringTools.toHexString(payload.readLong(2,0L),16);
                    sb.append("0x" + c);
                }
                return sb.toString();
            case ERROR_COMMAND_ERROR:
                if (inclDesc) {
                    sb.append("Command error");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    String c = StringTools.toHexString(payload.readLong(2,0L),16);
                    String e = StringTools.toHexString(payload.readLong(2,0L),16);
                    sb.append("cmd=0x" + c + ", err=0x"+ e);
                    int dlen = payload.getAvailableReadLength();
                    if (dlen > 0) {
                        String d = StringTools.toHexString(payload.readBytes(dlen));
                        sb.append(", data=0x" + d);
                    }
                }
                return sb.toString();
            case ERROR_UPLOAD_TYPE:
                if (inclDesc) {
                    sb.append("Invalid upload type");
                }
                return sb.toString();
            case ERROR_UPLOAD_PACKET:
                if (inclDesc) {
                    sb.append("Invalid upload packet");
                }
                return sb.toString();
            case ERROR_UPLOAD_LENGTH:
                if (inclDesc) {
                    sb.append("Invalid upload length");
                }
                return sb.toString();
            case ERROR_UPLOAD_OFFSET_OVERLAP:
                if (inclDesc) {
                    sb.append("Upload offset overlap");
                }
                return sb.toString();
            case ERROR_UPLOAD_OFFSET_GAP:
                if (inclDesc) {
                    sb.append("Upload offset gap");
                }
                return sb.toString();
            case ERROR_UPLOAD_OFFSET_OVERFLOW:
                if (inclDesc) {
                    sb.append("Upload offset overflow");
                }
                return sb.toString();
            case ERROR_UPLOAD_FILE_NAME:
                if (inclDesc) {
                    sb.append("Invalid uploaded filename");
                }
                return sb.toString();
            case ERROR_UPLOAD_CHECKSUM:
                if (inclDesc) {
                    sb.append("Invalid uploaded checksum");
                }
                return sb.toString();
            case ERROR_UPLOAD_SAVE:
                if (inclDesc) {
                    sb.append("Unable to save uploaded file");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    int dlen = payload.getAvailableReadLength();
                    String d = StringTools.toHexString(payload.readBytes(dlen));
                    sb.append("0x" + d);
                }
                return sb.toString();
            case ERROR_UPLOAD_HOST:
                if (inclDesc) {
                    sb.append("Invalid/unspecified upload host:port");
                }
                return sb.toString();
            case ERROR_UPLOAD_SERVER_ERROR:
                if (inclDesc) {
                    sb.append("Server indicated upload error");
                }
                return sb.toString();
            case ERROR_GPS_EXPIRED:
                if (inclDesc) {
                    sb.append("GPS Expired");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    long lastFix = payload.readLong(4,0L);
                    sb.append("lastFix=");
                    if (lastFix <= 0L) {
                        sb.append("never");     // i18n
                    } else
                    if (tz != null) {
                        DateTime dt = new DateTime(lastFix, tz);
                        sb.append(dt.format("yyyy/MM/dd,HH:mm:ss")); // i18n
                    } else {
                        sb.append(lastFix);
                    }
                }
                return sb.toString();
            case ERROR_GPS_FAILURE:
                if (inclDesc) {
                    sb.append("GPS Failure");
                    if (payload != null) { sb.append(": "); }
                }
                if (payload != null) {
                    long lastFix = payload.readLong(4,0L);
                    sb.append("lastFix=");
                    if (lastFix <= 0L) {
                        sb.append("never");     // i18n
                    } else
                    if (tz != null) {
                        DateTime dt = new DateTime(lastFix, tz);
                        sb.append(dt.format("yyyy/MM/dd,HH:mm:ss")); // i18n
                    } else {
                        sb.append(lastFix);
                    }
                    int dlen = payload.getAvailableReadLength();
                    if (dlen > 0) {
                        String d = StringTools.toHexString(payload.readBytes(dlen));
                        sb.append(" data=0x" + d);
                    }
                }
                return sb.toString();
            case ERROR_OUT_OF_MEMORY:
                if (inclDesc) {
                    sb.append("Out-Of-Memory error");
                }
                return sb.toString();
        }

        /* internal error */
        if ((errCode >= ERROR_INTERNAL_ERROR_00) && (errCode <= ERROR_INTERNAL_ERROR_0F)) {
            if (inclDesc) {
                sb.append("Internal error");
                if (payload != null) { sb.append(": "); }
            }
            if (payload != null) {
                int dlen = payload.getAvailableReadLength();
                String d = StringTools.toHexString(payload.readBytes(dlen));
                sb.append("0x" + d);
            }
            return sb.toString();
        }

        /* unknown */
        if (inclDesc) {
            sb.append("Unknown[0x").append(StringTools.toHexString(errCode,16)).append("]");
            if (payload != null) { sb.append(": "); }
        }
        if (payload != null) {
            int dlen = payload.getAvailableReadLength();
            String d = StringTools.toHexString(payload.readBytes(dlen));
            sb.append("0x" + d);
        }
        return sb.toString();
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private ClientErrors()
    {
        // not instantiated
    }
    
}
