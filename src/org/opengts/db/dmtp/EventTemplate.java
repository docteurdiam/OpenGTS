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
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -This class is used specifically for OpenDMTP server support and may not
//      be necessary, nor applicable, for other supported protocols.
//     -Fixed NPE when creating a new template.
//  2009/09/23  Martin D. Flynn
//     -Added support for loading custom event types from the runtime config.
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
import org.opendmtp.server.db.PayloadTemplate;
import org.opendmtp.server.base.Packet;

public class EventTemplate
    extends DeviceRecord<EventTemplate>
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class FieldType
    {
        private Integer code    = null;
        private String  desc    = "";
        public FieldType(int code, String desc) {
            this.code = new Integer(code);
            this.desc = desc;
        }
        public Integer getCode() {
            return this.code;
        }
        public int getCodeInt() {
            return this.code.intValue();
        }
        public String getDescription() {
            return this.desc;
        }
    }

    private static Map fieldMap = ListTools.toMap("getCode", new FieldType[] {
        new FieldType(PayloadTemplate.FIELD_STATUS_CODE         , "statuscode"       ),
        new FieldType(PayloadTemplate.FIELD_TIMESTAMP           , "timestamp"        ),
        new FieldType(PayloadTemplate.FIELD_INDEX               , "index"            ),
        new FieldType(PayloadTemplate.FIELD_SEQUENCE            , "sequence"         ),
        new FieldType(PayloadTemplate.FIELD_GPS_POINT           , "gps.point"        ),
        new FieldType(PayloadTemplate.FIELD_GPS_AGE             , "gps.age"          ),
        new FieldType(PayloadTemplate.FIELD_SPEED               , "speed"            ),
        new FieldType(PayloadTemplate.FIELD_HEADING             , "heading"          ),
        new FieldType(PayloadTemplate.FIELD_ALTITUDE            , "altitude"         ),
        new FieldType(PayloadTemplate.FIELD_DISTANCE            , "distance"         ),
        new FieldType(PayloadTemplate.FIELD_ODOMETER            , "odometer"         ),
        new FieldType(PayloadTemplate.FIELD_GEOFENCE_ID         , "geofence"         ),
        new FieldType(PayloadTemplate.FIELD_TOP_SPEED           , "speed.top"        ),
        new FieldType(PayloadTemplate.FIELD_BRAKE_G_FORCE       , "brake.gforce"     ),
        new FieldType(PayloadTemplate.FIELD_STRING              , "string"           ),
        new FieldType(PayloadTemplate.FIELD_STRING_PAD          , "string.pad"       ),
        new FieldType(PayloadTemplate.FIELD_ENTITY              , "entity"           ),
        new FieldType(PayloadTemplate.FIELD_ENTITY_PAD          , "entity.pad"       ),
        new FieldType(PayloadTemplate.FIELD_BINARY              , "binary"           ),
        new FieldType(PayloadTemplate.FIELD_INPUT_ID            , "inputID"          ),
        new FieldType(PayloadTemplate.FIELD_INPUT_STATE         , "inputState"       ),
        new FieldType(PayloadTemplate.FIELD_OUTPUT_ID           , "output.id"        ),
        new FieldType(PayloadTemplate.FIELD_OUTPUT_STATE        , "output.state"     ),
        new FieldType(PayloadTemplate.FIELD_ELAPSED_TIME        , "elapsed.time"     ),
        new FieldType(PayloadTemplate.FIELD_COUNTER             , "counter"          ),
        new FieldType(PayloadTemplate.FIELD_SENSOR32_LOW        , "sensor32.low"     ),
        new FieldType(PayloadTemplate.FIELD_SENSOR32_HIGH       , "sensor32.high"    ),
        new FieldType(PayloadTemplate.FIELD_SENSOR32_AVER       , "sensor32.aver"    ),
        new FieldType(PayloadTemplate.FIELD_TEMP_LOW            , "temperature.low"  ),
        new FieldType(PayloadTemplate.FIELD_TEMP_HIGH           , "temperature.high" ),
        new FieldType(PayloadTemplate.FIELD_TEMP_AVER           , "temperature.aver" ),
        new FieldType(PayloadTemplate.FIELD_GPS_DGPS_UPDATE     , "gps.dgps.update"  ),
        new FieldType(PayloadTemplate.FIELD_GPS_HORZ_ACCURACY   , "gps.accuracy.horz"),
        new FieldType(PayloadTemplate.FIELD_GPS_VERT_ACCURACY   , "gps.accuracy.vert"),
        new FieldType(PayloadTemplate.FIELD_GPS_SATELLITES      , "gps.satellites"   ),
        new FieldType(PayloadTemplate.FIELD_GPS_MAG_VARIATION   , "gps.magvariation" ),
        new FieldType(PayloadTemplate.FIELD_GPS_QUALITY         , "gps.quality"      ),
        new FieldType(PayloadTemplate.FIELD_GPS_TYPE            , "gps.type"         ),
        new FieldType(PayloadTemplate.FIELD_GPS_GEOID_HEIGHT    , "gps.geoid.height" ),
        new FieldType(PayloadTemplate.FIELD_GPS_PDOP            , "gps.pdop"         ),
        new FieldType(PayloadTemplate.FIELD_GPS_HDOP            , "gps.hdop"         ),
        new FieldType(PayloadTemplate.FIELD_GPS_VDOP            , "gps.vdop"         ),
        new FieldType(PayloadTemplate.FIELD_OBC_VALUE           , "obc.value"        ),
        new FieldType(PayloadTemplate.FIELD_OBC_GENERIC         , "obc.generic"      ),
        new FieldType(PayloadTemplate.FIELD_OBC_J1708_FAULT     , "obc.j1708fault"   ),
        new FieldType(PayloadTemplate.FIELD_OBC_DISTANCE        , "obc.distance"     ),
        new FieldType(PayloadTemplate.FIELD_OBC_ENGINE_HOURS    , "obc.engine.hours" ),
        new FieldType(PayloadTemplate.FIELD_OBC_ENGINE_RPM      , "obc.engine.rpm"   ),
        new FieldType(PayloadTemplate.FIELD_OBC_COOLANT_TEMP    , "obc.coolant.temp" ),
        new FieldType(PayloadTemplate.FIELD_OBC_COOLANT_LEVEL   , "obc.coolant.level"),
        new FieldType(PayloadTemplate.FIELD_OBC_OIL_LEVEL       , "obc.oil.level"    ),
        new FieldType(PayloadTemplate.FIELD_OBC_OIL_PRESSURE    , "obc.oil.pressure" ),
        new FieldType(PayloadTemplate.FIELD_OBC_FUEL_LEVEL      , "obc.fuel.level"   ),
        new FieldType(PayloadTemplate.FIELD_OBC_FUEL_ECONOMY    , "obc.fuel.economy" ),
        new FieldType(PayloadTemplate.FIELD_OBC_FUEL_TOTAL      , "obc.fuel.total"   ),
        new FieldType(PayloadTemplate.FIELD_OBC_FUEL_IDLE       , "obc.fuel.idle"    ),
    });
    
    public static FieldType getFieldType(int code)
    {
        return (FieldType)fieldMap.get(new Integer(code));
    }
    
    public static String getFieldTypeDescription(int code)
    {
        FieldType ft = getFieldType(code);
        return (ft != null)? ft.getDescription() : null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* table name */
    public static final String _TABLE_NAME              = "EventTemplate";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_customType           = "customType";
    public static final String FLD_repeatLast           = "repeatLast";
    public static final String FLD_template             = "template";
    private static DBField FieldInfo[] = {
        // EventTemplate fields
        newField_accountID(true),
        newField_deviceID(true),
        new DBField(FLD_customType   , Integer.TYPE      , DBField.TYPE_UINT8      , "Custom Type"  , "key=true format=X1"),
        new DBField(FLD_repeatLast   , Boolean.TYPE      , DBField.TYPE_BOOLEAN    , "Repeat Last"  , null),
        new DBField(FLD_template     , DTTemplate.class  , DBField.TYPE_TEXT       , "Template"     , "editor=eventTemplate"),
    };

    /* key class */
    public static class Key
        extends DeviceKey<EventTemplate>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId, int customType) {
            super.setFieldValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_customType, customType);
        }
        public DBFactory<EventTemplate> getFactory() {
            return EventTemplate.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<EventTemplate> factory = null;
    public static DBFactory<EventTemplate> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                EventTemplate.TABLE_NAME(), 
                EventTemplate.FieldInfo, 
                DBFactory.KeyType.PRIMARY, 
                EventTemplate.class, 
                EventTemplate.Key.class,
                false/*editable*/, true/*viewable*/); // not editable, but viewable
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public EventTemplate()
    {
        super();
    }

    /* database record */
    public EventTemplate(EventTemplate.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(EventTemplate.class, loc);
        return i18n.getString("EventTemplate.description", 
            "This table contains " +
            "DMTP event packet 'template's (Custom Event Packet Negotiation parse templates) " +
            "which have been received from client devices."
            );
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static Map<Integer,PayloadTemplate> PayloadTemplateMap = new HashMap<Integer,PayloadTemplate>();
    
    public static PayloadTemplate GetPayloadTemplate(String acctId, String devId, int custType)
    {
        EventTemplate.Key tmpKey = new EventTemplate.Key(acctId, devId, custType);
        EventTemplate et = tmpKey.getDBRecord();
        if (et.reload() != null) {
            // Event template exists
            return et.createPayloadTemplate();
        } else {
            // Event template does not exist, check runtime config
            Integer custTypeInt = new Integer(custType);
            PayloadTemplate pt = PayloadTemplateMap.get(custTypeInt);
            if (pt == null) {
                DCServerConfig dcs = DCServerFactory.getServerConfig(DCServerFactory.OPENDMTP_NAME);
                if (dcs != null) {
                    String key = DCServerFactory.OPENDMTP_NAME + ".customEvent." + StringTools.toHexString(custType,8);
                    String customTemplate = dcs.getStringProperty(key, null);
                    if (!StringTools.isBlank(customTemplate)) {
                        String fldStr[] = StringTools.split(customTemplate,' ');
                        pt = new PayloadTemplate(custType, fldStr, true);
                        PayloadTemplateMap.put(custTypeInt, pt);
                    }
                }
            }
            return pt;
        }
    }
    
    public static boolean SetPayloadTemplate(String acctId, String devId, PayloadTemplate pt)
    {
        if (pt != null) {
            EventTemplate.Key etKey = new EventTemplate.Key(acctId, devId, pt.getPacketType());
            EventTemplate et = etKey.getDBRecord();
            et.initFromPayloadTemplate(pt);
            try {
                et.save();
                return true;
            } catch (DBException dbe) {
                return false;
            }
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
    
    public int getCustomType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_customType);
        return (v != null)? v.intValue() : 0;
    }
   
    public void setCustomType(int v)
    {
        this.setFieldValue(FLD_customType, v);
    }

    // ------------------------------------------------------------------------

    public boolean getRepeatLast()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_repeatLast);
        return (v != null)? v.booleanValue() : false;
    }
   
    public void setRepeatLast(boolean v)
    {
        this.setFieldValue(FLD_repeatLast, v);
    }

    // ------------------------------------------------------------------------

    public DTTemplate getTemplate()
    {
        DTTemplate v = (DTTemplate)this.getFieldValue(FLD_template);
        return (v != null)? v : null;
    }
   
    public void setTemplate(DTTemplate v)
    {
        this.setFieldValue(FLD_template, v);
    }
   
    public void setTemplate(String v)
    {
        this.setTemplate(new DTTemplate(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void initFromPayloadTemplate(PayloadTemplate pt)
    {
        if (pt != null) {
            this.setFieldValue(FLD_repeatLast, pt.getRepeatLast());
            PayloadTemplate.Field flds[] = pt.getFields();
            
            /* get current template (create new if necessary) */
            DTTemplate template = this.getTemplate();
            if (template == null) {
                template = new DTTemplate();
            }
            
            /* re-init template */
            template.clearFields();
            for (int i = 0; i < flds.length; i++) {
                int type = flds[i].getType();
                boolean isHi = flds[i].isHiRes();
                int ndx  = flds[i].getIndex();
                int len  = flds[i].getLength();
                DTTemplate.Field dtf = new DTTemplate.Field(type, isHi, ndx, len);
                template.setField(i, dtf);
            }
            
            /* set new template */
            // put the value back in case in the future this is a clone of the actual template
            this.setFieldValue(FLD_template, template);
            
        }
    }
    
    public PayloadTemplate createPayloadTemplate()
    {
        
        /* assemble PayloadTemplate.Field array */
        java.util.List<PayloadTemplate.Field> pfl = new Vector<PayloadTemplate.Field>();
        DTTemplate t = this.getTemplate();
        if (t != null) {
            for (int i = 0;; i++) {
                DTTemplate.Field tf = t.getField(i);
                if (tf == null) { break; }
                int     type = tf.getType();
                boolean isHi = tf.isHiRes();
                int     ndx  = tf.getIndex();
                int     len  = tf.getLength();
                PayloadTemplate.Field pf = new PayloadTemplate.Field(type, isHi, ndx, len);
                pfl.add(pf);
            }
        }
        PayloadTemplate.Field pfa[] = pfl.toArray(new PayloadTemplate.Field[pfl.size()]);

        /* create/return new PayloadTemplate */
        return new PayloadTemplate(this.getCustomType(), pfa, this.getRepeatLast());
        
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct"  };
    private static final String ARG_DEVICE[]    = new String[] { "device" , "dev"   };
    private static final String ARG_LOAD[]      = new String[] { "load"             };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + EventTemplate.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>           Acount ID which owns the specified Device");
        Print.logInfo("  -device=<id>            Device ID to apply event templates");
        Print.logInfo("  -load=<template>        An XML file containing an event template");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE , "");

        /* account-id specified? */
        if ((acctID == null) || acctID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(acctID); // may return DBException
            if (account == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* device-id specified? */
        if ((devID == null) || devID.equals("")) {
            Print.logError("Device-ID not specified.");
            usage();
        }

        /* get device */
        Device device = null;
        try {
            device = Device.getDevice(account, devID, false);
            if (device == null) {
                Print.logError("Device-ID does not exist: " + devID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Device: " + devID, dbe);
            //dbe.printException();
            System.exit(99);
        }
        
        /* option count */
        int opts = 0;

        /* load EventTemplate from file */
        // ie. "-load=file.xml"
        if (RTConfig.hasProperty(ARG_LOAD)) {
            // EventTemplate.customType=0x73
            // EventTemplate.repeatLast=true
            // EventTemplate.template.0=H|1|0|2 
            // EventTemplate.template.1=H|2|0|4 
            // EventTemplate.template.2=H|7|0|2 
            // EventTemplate.template.3=H|6|0|8 
            // EventTemplate.template.4=H|8|0|2
            // EventTemplate.template.5=H|9|0|2
            // EventTemplate.template.6=L|10|0|2
            // EventTemplate.template.7=H|14|0|2
            // EventTemplate.template.8=H|12|0|4
            // EventTemplate.template.9=L|93|0|1
            // EventTemplate.template.10=H|94|0|2
            // EventTemplate.template.11=L|82|0|4
            // EventTemplate.template.12=H|21|0|20
            // EventTemplate.template.13=H|4|0|1
            opts++;
            Print.logInfo("");
            // load propert file
            File file = RTConfig.getFile(ARG_LOAD,null);
            if ((file == null) || !file.exists()) {
                Print.logError("File does not exist: " + file);
                System.exit(99);
            }
            RTProperties tp = new RTProperties(file);
            // custom type
            int customType = tp.getInt(_TABLE_NAME + "." + FLD_customType, 0);
            if ((customType & ~0x00FF) != 0) { // packet header is optional
                // if specified, the packet header value must equal the BASIC packet header
                if ((customType >> 8) != Packet.HEADER_BASIC) {
                    Print.logError("Invalid custom packet header: 0x" + StringTools.toHexString((long)customType,16));
                    System.exit(99);
                }
                customType &= 0x00FF; // strip off type
            }
            if ((customType < Packet.PKT_CLIENT_CUSTOM_FORMAT_0) || 
                (customType > Packet.PKT_CLIENT_CUSTOM_FORMAT_F)) {
                Print.logError("Invalid custom packet type: 0x" + StringTools.toHexString((long)customType,8));
                System.exit(99);
            }
            // repeat last
            boolean repeatLast = tp.getBoolean(_TABLE_NAME + "." + FLD_repeatLast, true);
            // field definition
            StringBuffer fields = new StringBuffer();
            int length = 0;
            for (int i = 0; i < 99; i++) {
                String k = _TABLE_NAME + "." + FLD_template + "." + i;
                if (!tp.hasProperty(k)) { 
                    if (fields.length() == 0) {
                        Print.logError("No fields defined");
                        System.exit(99);
                    } else {
                        break; 
                    }
                }
                DTTemplate.Field f = new DTTemplate.Field(tp.getString(k,""));
                if (!f.isValid()) {
                    Print.logError("Field error: " + k + " - " + tp.getString(k,""));
                    System.exit(99);
                }
                length += f.getLength();
                if (length > Packet.MAX_PAYLOAD_LENGTH) {
                    Print.logError("Maximum length exceeded: " + length);
                    System.exit(99);
                }
                fields.append(String.valueOf(i)).append("=").append(f.toString()).append(" ");
            }
            // create template
            try {
                Print.sysPrintln("AccountID  : " + acctID);
                Print.sysPrintln("DeviceID   : " + devID);
                Print.sysPrintln("Packet Type: 0x" + StringTools.toHexString(customType,8));
                Print.sysPrintln("Fields     : " + fields);
                EventTemplate.Key key = new EventTemplate.Key(acctID, devID, customType);
                EventTemplate evt = key.getDBRecord();
                evt.setTemplate(fields.toString());
                evt.save();
            } catch (DBException dbe) {
                Print.logError("");
                System.exit(99);
            }
            // done
            System.exit(0);
        }
        
    }
    
}
