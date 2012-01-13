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
//  Report definition based on generic field definitions
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/25  Martin D. Flynn
//     -Initial release
//  2007/11/28  Martin D. Flynn
//     -Added additional 'stop' fields for motion reporting
//  2007/01/10  Martin D. Flynn
//     -Added several new fields.
//  2008/03/12  Martin D. Flynn
//     -Added additional decimal point options to various fields
//  2008/03/28  Martin D. Flynn
//     -Added property/diagnostic fields
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//     -Added City/State/Country/Subdivision fields
//  2008/06/20  Martin D. Flynn
//     -Added DATA_ACCOUNT_ID
//  2008/10/16  Martin D. Flynn
//     -Added DATA_LAST_EVENT
//  2009/08/23  Martin D. Flynn
//     -Fixed StatusCode description lookup (now first looks up account custom 
//      status codes).
//  2009/09/23  Martin D. Flynn
//     -Added 'startOdometer' column
//  2010/04/11  Martin D. Flynn
//     -Added columns "serviceNotes"/"serviceRemaining"/"accountDesc"/...
//  2010/06/17  Martin D. Flynn
//     -Added columns "stopCount", "engineHours", "ptoHours", "idleHours", etc.
//  2010/09/09  Martin D. Flynn
//     -Added "deviceBattery"
//  2011/05/13  Martin D. Flynn
//     -Added DATA_EVENT_INDEX
//  2011/06/16  Martin D. Flynn
//     -Added status code/description coloring option
//  2011/10/03  Martin D. Flynn
//     -Added DATA_STOP_FUEL, DATA_START_FUEL, DATA_FUEL_DELTA
// ----------------------------------------------------------------------------
package org.opengts.war.report.field;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.RequestProperties;
import org.opengts.war.tools.PushpinIcon;

import org.opengts.war.report.*;

import org.opengts.db.dmtp.PropertyKey;

public class FieldLayout
    extends ReportLayout
{

    // ------------------------------------------------------------------------

    private static final long   MIN_REASONABLE_TIMESTAMP = (new DateTime(null,2000,1,1)).getTimeSec();

    // ------------------------------------------------------------------------
    // Data keys
    // - These define what data is available (see 'FieldDataRow') and what columns will be 
    //   displayed in the table.
    // - Column names must only contain <alpha>/<numeric>/'_' characters
    
    public static final String  DATA_INDEX              = "index";
    
    public static final String  DATA_ACCOUNT_ID         = "accountId";          // field
    public static final String  DATA_ACCOUNT_DESC       = "accountDesc";
    
    public static final String  DATA_USER_ID            = "userId";             // field
    public static final String  DATA_USER_DESC          = "userDesc";
    
    public static final String  DATA_DEVICE_ID          = "deviceId";           // field
    public static final String  DATA_DEVICE_DESC        = "deviceDesc";
    public static final String  DATA_DEVICE_BATTERY     = "deviceBattery";

    public static final String  DATA_VEHICLE_ID         = "vehicleId";          // field
    public static final String  DATA_EQUIPMENT_TYPE     = "equipmentType";      // field
    public static final String  DATA_UNIQUE_ID          = "uniqueId";           // field
    public static final String  DATA_GROUP_ID           = "groupId";            // field
    public static final String  DATA_STATUS_CODE        = "statusCode";         // field
    public static final String  DATA_STATUS_DESC        = "statusDesc";
    public static final String  DATA_PUSHPIN            = "pushpin";
    public static final String  DATA_ENTITY_ID          = "entityId";           // field
    public static final String  DATA_ENTITY_DESC        = "entityDesc";         // field
    public static final String  DATA_DRIVER_ID          = "driverId";           // field
    public static final String  DATA_DRIVER_DESC        = "driverDesc";         // field
    public static final String  DATA_DRIVER_BADGEID     = "driverBadge";        // field
    public static final String  DATA_DRIVER_LICENSE     = "driverLicense";      // field
    public static final String  DATA_DRIVER_LICENSE_EXP = "driverLicenseExp";   // field
    public static final String  DATA_DRIVER_BIRTHDATE   = "driverBirthdate";    // field
    public static final String  DATA_LATITUDE           = "latitude";           // field
    public static final String  DATA_LONGITUDE          = "longitude";          // field
    public static final String  DATA_GEOPOINT           = "geoPoint";           // field
    public static final String  DATA_ALTITUDE           = "altitude";           // field
    public static final String  DATA_SPEED_LIMIT        = "speedLimit";         // field
    public static final String  DATA_SPEED              = "speed";              // field
    public static final String  DATA_SPEED_HEADING      = "speedH";
    public static final String  DATA_SPEED_UNITS        = "speedU";
    public static final String  DATA_HEADING            = "heading";            // field
    public static final String  DATA_DISTANCE           = "distance";           // field
    public static final String  DATA_ODOMETER           = "odometer";           // field
    public static final String  DATA_ODOMETER_DELTA     = "odomDelta";          // field
    public static final String  DATA_EVENT_INDEX        = "eventIndex";         //
    public static final String  DATA_LAST_EVENT         = "lastEvent";  
    public static final String  DATA_SERVER_ID          = "serverId";           // field (deviceCode)
    public static final String  DATA_JOB_NUMBER         = "jobNumber";          // field
    
    public static final String  DATA_BEST_LATITUDE      = "bestLatitude";       // field
    public static final String  DATA_BEST_LONGITUDE     = "bestLongitude";      // field
    public static final String  DATA_BEST_GEOPOINT      = "bestGeoPoint";       // field

    public static final String  DATA_DATE               = "date";
    public static final String  DATA_TIME               = "time";
    public static final String  DATA_DATETIME           = "dateTime";
    public static final String  DATA_TIMESTAMP          = "timestamp";          // field

    public static final String  DATA_CREATE_DATE        = "createDate";
    public static final String  DATA_CREATE_TIME        = "createTime";
    public static final String  DATA_CREATE_DATETIME    = "createDateTime";
    public static final String  DATA_CREATE_TIMESTAMP   = "createTimestamp";

    public static final String  DATA_ADDRESS            = "address";            // field
    public static final String  DATA_CITY               = "city";               // field
    public static final String  DATA_STATE              = "state";              // field
    public static final String  DATA_COUNTRY            = "country";            // field
    public static final String  DATA_SUBDIVISION        = "subdivision";        // field
    public static final String  DATA_GEOZONE_ID         = "geozoneId";          // field
    public static final String  DATA_GEOZONE_DESC       = "geozoneDesc";

    public static final String  DATA_PROPERTY_KEY       = "propertyKey";        // field
    public static final String  DATA_PROPERTY_DESC      = "propertyDesc";
    public static final String  DATA_PROPERTY_VALUE     = "propertyValue";      // field

    public static final String  DATA_DIAGNOSTIC_ERROR   = "diagError";          // field
    public static final String  DATA_DIAGNOSTIC_KEY     = "diagKey";            // field
    public static final String  DATA_DIAGNOSTIC_DESC    = "diagDesc";           // field
    public static final String  DATA_DIAGNOSTIC_VALUE   = "diagValue";          // field

    public static final String  DATA_UTILIZATION        = "utilization";        // field
    public static final String  DATA_COUNT              = "count";              // field

    public static final String  DATA_START_DATETIME     = "startDateTime";
    public static final String  DATA_START_TIMESTAMP    = "startTimestamp";     // field
    public static final String  DATA_START_ODOMETER     = "startOdometer";      // field
    public static final String  DATA_START_FUEL         = "startFuel";          // field
    public static final String  DATA_FUEL_DELTA         = "fuelDelta";          // field

    public static final String  DATA_STOP_DATETIME      = "stopDateTime";
    public static final String  DATA_STOP_TIMESTAMP     = "stopTimestamp";      // field
    public static final String  DATA_STOP_LATITUDE      = "stopLatitude";       // field
    public static final String  DATA_STOP_LONGITUDE     = "stopLongitude";      // field
    public static final String  DATA_STOP_GEOPOINT      = "stopGeoPoint";       // field
    public static final String  DATA_STOP_ODOMETER      = "stopOdometer";       // field
    public static final String  DATA_STOP_FUEL          = "stopFuel";           // field
    public static final String  DATA_STOP_ADDRESS       = "stopAddress";        // field
    public static final String  DATA_STOP_ELAPSED       = "stopElapse";         // field
    public static final String  DATA_STOP_COUNT         = "stopCount";          // field
    
    public static final String  DATA_ENTER_DATETIME     = "enterDateTime";      // field
    public static final String  DATA_ENTER_TIMESTAMP    = "enterTimestamp";     // field
    public static final String  DATA_ENTER_GEOZONE_ID   = "enterGeozoneId";     // field
    public static final String  DATA_ENTER_ADDRESS      = "enterAddress";       // field

    public static final String  DATA_EXIT_DATETIME      = "exitDateTime";       // field
    public static final String  DATA_EXIT_TIMESTAMP     = "exitTimestamp";      // field
    public static final String  DATA_EXIT_GEOZONE_ID    = "exitGeozoneId";      // field
    public static final String  DATA_EXIT_ADDRESS       = "exitAddress";        // field

    public static final String  DATA_ELAPSE_SEC         = "elapseSec";          // field
    public static final String  DATA_INSIDE_ELAPSED     = "insideElapse";       // field
    public static final String  DATA_OUTSIDE_ELAPSED    = "outsideElapse";      // field
    public static final String  DATA_DRIVING_ELAPSED    = "drivingElapse";      // field
    public static final String  DATA_IDLE_ELAPSED       = "idleElapse";         // field
    public static final String  DATA_ATTACHED           = "attached";           // field
    
    public static final String  DATA_TCP_CONNECTIONS    = "tcpConnections";     // field
    public static final String  DATA_UDP_CONNECTIONS    = "udpConnections";     // field
    public static final String  DATA_CONNECTIONS        = "connections";        // field
    public static final String  DATA_IPADDRESS          = "ipAddress";          // field
    public static final String  DATA_ISDUPLEX           = "isDuplex";           // field
    public static final String  DATA_BYTES_READ         = "bytesRead";          // field
    public static final String  DATA_BYTES_WRITTEN      = "bytesWritten";       // field
    public static final String  DATA_BYTES_OVERHEAD     = "bytesOverhead";      // field
    public static final String  DATA_BYTES_TOTAL        = "bytesTotal";         // field
    public static final String  DATA_BYTES_ROUNDED      = "bytesRounded";       // field
    public static final String  DATA_EVENTS_RECEIVED    = "eventsReceived";     // field

    public static final String  DATA_ENGINE_HOURS       = "engineHours";        // field
    public static final String  DATA_IDLE_HOURS         = "idleHours";          // field
    public static final String  DATA_WORK_HOURS         = "workHours";          // field
    public static final String  DATA_PTO_HOURS          = "ptoHours";           // field

    public static final String  DATA_FUEL_CAPACITY      = "fuelCapacity";       // field
    public static final String  DATA_FUEL_LEVEL         = "fuelLevel";          // field
    public static final String  DATA_FUEL_TOTAL         = "fuelTotal";          // field
    public static final String  DATA_FUEL_TRIP          = "fuelTrip";           // field
    public static final String  DATA_FUEL_IDLE          = "fuelIdle";           // field
    public static final String  DATA_FUEL_WORK          = "fuelWork";           // field
    public static final String  DATA_FUEL_PTO           = "fuelPTO";            // field
    public static final String  DATA_FUEL_ECONOMY       = "fuelEconomy";        // field

    public static final String  DATA_ENGINE_RPM         = "engineRpm";          // field

    public static final String  DATA_CHECKIN_DATETIME   = "checkinDateTime";    // Device record
    public static final String  DATA_CHECKIN_AGE        = "checkinAge";         // Device record
    public static final String  DATA_LAST_IPADDRESS     = "lastIPAddress";      // Device record
    public static final String  DATA_SERVICE_LAST       = "serviceLast";        // Device record
    public static final String  DATA_SERVICE_INTERVAL   = "serviceInterval";    // Device record
    public static final String  DATA_SERVICE_NEXT       = "serviceNext";        // Device record
    public static final String  DATA_SERVICE_REMAINING  = "serviceRemaining";   // Device record
    public static final String  DATA_SERVICE_NOTES      = "serviceNotes";       // Device record
    public static final String  DATA_CODE_VERSION       = "codeVersion";        // Device record
    public static final String  DATA_CUSTOM_FIELD       = "customField";        // Device record
    
    public static final String  DATA_LOGIN_DATETIME     = "loginDateTime";      // Account record
    public static final String  DATA_LOGIN_AGE          = "loginAge";           // Account record
    public static final String  DATA_ACCOUNT_ACTIVE     = "accountActive";      // Account record
    public static final String  DATA_DEVICE_COUNT       = "deviceCount";        // Account record
    public static final String  DATA_PRIVATE_LABEL      = "privateLabelName";   // Account record

    public static final String  DATA_LEFT_ALIGN_1       = "leftAlign_1";        // left aligned string
    public static final String  DATA_LEFT_ALIGN_2       = "leftAlign_2";        // left aligned string
    public static final String  DATA_RIGHT_ALIGN_1      = "rightAlign_1";       // right aligned string
    public static final String  DATA_RIGHT_ALIGN_2      = "rightAlign_2";       // right aligned string
    
    public static final String  DATA_BLANK_SPACE        = "blankSpace";         // nothing displayed

    // ------------------------------------------------------------------------
    // FieldLayout is a singleton
    
    private static FieldLayout reportDef = null;

    public static ReportLayout getReportLayout()
    {
        if (reportDef == null) {
            reportDef = new FieldLayout();
        }
        return reportDef;
    }
    
    private FieldLayout()
    {
        super();
        this.setDataRowTemplate(new FieldDataRow());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* format double value */
    protected static String formatDouble(double value, String arg, String dftArg)
    {
        String fmt = dftArg;
        if ((arg != null) && (arg.length() > 0)) {
            switch (arg.charAt(0)) {
                case '0': fmt = "#0"       ; break;
                case '1': fmt = "#0.0"     ; break;
                case '2': fmt = "#0.00"    ; break;
                case '3': fmt = "#0.000"   ; break;
                case '4': fmt = "#0.0000"  ; break;
                case '5': fmt = "#0.00000" ; break;
                case '6': fmt = "#0.000000"; break;
            }
        }
        return StringTools.format(value, fmt);
    }
    
    // ------------------------------------------------------------------------

    protected static class FieldDataRow
        extends DataRowTemplate
    {
        public DBDataRow.RowType getRowType(Object obj) {
            return (obj instanceof FieldData)? ((FieldData)obj).getRowType() : DBDataRow.RowType.DETAIL;
        }
        public FieldDataRow() {
            super();

            // Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    if (rowNdx >= 0) {
                        String arg = rc.getArg();
                        int ofs = 1;
                        if ((arg != null) && (arg.length() > 0) && (arg.charAt(0) == '0')) {
                            ofs = 0;
                        }
                        return String.valueOf(rowNdx + ofs);
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return "#";
                }
            });

            // Account-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCOUNT_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String acctID = fd.getAccountID();
                    return fd.filterReturnedValue(DATA_ACCOUNT_ID,acctID);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.accountID","Account-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCOUNT_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String desc = fd.getString(DATA_ACCOUNT_DESC,null);
                    if (desc == null) {
                        Account acct = fd.getAccount();
                        if (acct != null) {
                            desc = acct.getDescription();
                            if (StringTools.isBlank(desc) && acct.isSystemAdmin()) {
                                I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                                desc = i18n.getString("FieldLayout.accountAdmin","Administrator");
                            }
                        }
                    }
                    return fd.filterReturnedValue(DATA_ACCOUNT_DESC,desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.accountDescription","Account\nDescription");
                }
            });

            // User-ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_USER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String userID = fd.getString(FieldLayout.DATA_USER_ID, "");
                    return fd.filterReturnedValue(DATA_USER_ID,userID);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.userID","User-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_USER_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String userID = fd.getString(FieldLayout.DATA_USER_ID  , "");
                    String desc   = fd.getString(FieldLayout.DATA_USER_DESC, "");
                    if (StringTools.isBlank(desc) && User.isAdminUser(userID)) {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        desc = i18n.getString("FieldLayout.userAdmin","Account Administrator");
                    }
                    return fd.filterReturnedValue(DATA_USER_DESC,desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.userDescription","User\nDescription");
                }
            });

            // Device-ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_DEVICE_ID,fd.getDeviceID());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceID","Device-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String desc = fd.getString(DATA_DEVICE_DESC,null);
                    if (desc == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            desc = dev.getDescription();
                        }
                    }
                    return fd.filterReturnedValue(DATA_DEVICE_DESC,desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceDescription","Device\nDescription");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_BATTERY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        double level = dev.getLastBatteryLevel();
                        if (level <= 0.0) {
                            return rc.getBlankFiller();
                        } else
                        if (level <= 1.0) {
                            String p = Math.round(level*100.0) + "%";
                            return fd.filterReturnedValue(DATA_DEVICE_BATTERY, p);  // percent
                        } else {
                            String v = formatDouble(level, arg, "0.0") + "v";
                            return fd.filterReturnedValue(DATA_DEVICE_BATTERY, v);  // volts
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceBattery","Last\nBattery");
                }
            });

            // Vehicle-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_VEHICLE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String vid = fd.getString(DATA_VEHICLE_ID,null);
                    if (vid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            vid = dev.getVehicleID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_VEHICLE_ID,vid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.vehicleID","Vehicle-ID");
                }
            });

            // Equipment Type
            this.addColumnTemplate(new DataColumnTemplate(DATA_EQUIPMENT_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String eqType = fd.getString(DATA_EQUIPMENT_TYPE,null);
                    if (eqType == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            eqType = dev.getEquipmentType();
                        }
                    }
                    return fd.filterReturnedValue(DATA_EQUIPMENT_TYPE,eqType);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.equipmentType","Equipment\nType");
                }
            });

            // Unique-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_UNIQUE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String uid = fd.getString(DATA_UNIQUE_ID,null);
                    if (uid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            uid = dev.getUniqueID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_UNIQUE_ID,uid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.uniqueID","Unique-ID");
                }
            });

            // Server-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String sid = fd.getString(DATA_SERVER_ID,null);
                    if (sid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            sid = dev.getDeviceCode();
                        }
                    }
                    return fd.filterReturnedValue(DATA_SERVER_ID,sid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.serverID","Server-ID");
                }
            });

            // JobNumber
            this.addColumnTemplate(new DataColumnTemplate(DATA_JOB_NUMBER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String job = fd.getString(DATA_JOB_NUMBER,"");
                    return fd.filterReturnedValue(DATA_JOB_NUMBER,job);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.jobNumber","Job#");
                }
            });

            // Group-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GROUP_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String gid = fd.getString(DATA_GROUP_ID,null);
                    if (gid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            gid = dev.getGroupID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_GROUP_ID,gid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.groupID","Group-ID");
                }
            });

            // Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()),tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_DATE, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.date","Date");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz = rd.getTimeZone();
                            DateTime dt = new DateTime(ts);
                            String tmFmt = dt.format(rl.getTimeFormat(rd.getPrivateLabel()),tz);
                            return fd.filterReturnedValue(DATA_TIME, tmFmt);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.time","Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz = rd.getTimeZone();
                            DateTime dt = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()),tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.dateTime","Date/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.timestamp","Timestamp") + "\n(Epoch)";
                }
            });

            // Creation Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()),tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_CREATE_DATE, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createDate","Insert\nDate");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz = rd.getTimeZone();
                            DateTime dt = new DateTime(ts);
                            return fd.filterReturnedValue(DATA_CREATE_TIME,dt.format(rl.getTimeFormat(rd.getPrivateLabel()),tz));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createTime","Insert\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz = rd.getTimeZone();
                            DateTime dt = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()),tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_CREATE_DATETIME,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createDateTime","Insert\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_CREATE_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createTimestamp","Insert\nTimestamp") + "\n(Epoch)";
                }
            });

            // Status Code/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_CODE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STATUS_CODE)) {
                        String arg    = rc.getArg();
                        int    code   = (int)fd.getLong(DATA_STATUS_CODE);
                        String scCode = "0x" + StringTools.toHexString(code,16);
                        if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("color")) {
                            Device dev = fd.getDevice();
                            BasicPrivateLabel bpl = rd.getPrivateLabel();
                            StatusCodeProvider scp = StatusCode.getStatusCodeProvider(dev,code,bpl,null/*dftSCP*/);
                            if (scp == null) {
                                return fd.filterReturnedValue(DATA_STATUS_CODE,scCode);
                            } else {
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(scCode);
                                cv.setForegroundColor(scp.getForegroundColor());
                                cv.setBackgroundColor(scp.getBackgroundColor());
                                return fd.filterReturnedValue(DATA_STATUS_CODE,cv);
                            }
                        } else {
                            return fd.filterReturnedValue(DATA_STATUS_CODE,scCode);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.statusCode","Status#");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg    = rc.getArg();
                    FieldData fd  = (FieldData)obj;
                    String scDesc = fd.getString(DATA_STATUS_DESC);
                    Device dev    = fd.getDevice();
                    BasicPrivateLabel bpl = rd.getPrivateLabel();
                    int    code   = (int)fd.getLong(DATA_STATUS_CODE, StatusCodes.STATUS_NONE);
                    if (StringTools.isBlank(scDesc) && (code != StatusCodes.STATUS_NONE)) {
                        scDesc = StatusCode.getDescription(dev,code,bpl,null);
                    }
                    if (!StringTools.isBlank(scDesc)) {
                        if (code == StatusCodes.STATUS_NONE) {
                            // no code specified to provide coloring
                            return fd.filterReturnedValue(DATA_STATUS_DESC,scDesc);
                        } else
                        if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("color")) {
                            // check for status code coloring
                            StatusCodeProvider scp = StatusCode.getStatusCodeProvider(dev,code,bpl,null/*dftSCP*/);
                            if (scp == null) {
                                return fd.filterReturnedValue(DATA_STATUS_DESC,scDesc);
                            } else {
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(scDesc);
                                cv.setForegroundColor(scp.getForegroundColor());
                                cv.setBackgroundColor(scp.getBackgroundColor());
                                return fd.filterReturnedValue(DATA_STATUS_DESC,cv);
                            }
                        } else {
                            // coloring not wanted
                            return fd.filterReturnedValue(DATA_STATUS_DESC,scDesc);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.statusDescription","Status");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PUSHPIN) {
                // EXPERIMENTAL! (the icons produced by this code section may not exactly match
                // those produced on the actual map by the JavaScript functions.
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    RequestProperties reqState = rd.getRequestProperties();
                    FieldData fd = (FieldData)obj;
                    String iconURL = fd.getString(DATA_PUSHPIN);
                    if (StringTools.isBlank(iconURL)) {
                        return rc.getBlankFiller();
                    } else
                    if (StringTools.isInt(iconURL,true)) {
                        int ppNdx = StringTools.parseInt(iconURL, 0);
                        PushpinIcon ppi = reqState.getPushpinIcon(ppNdx);
                        iconURL = (ppi != null)? ppi.getIconEvalURL(null/*EventData*/,rowNdx) : "";
                        ColumnValue cv = new ColumnValue().setImageURL(iconURL);
                        return fd.filterReturnedValue(DATA_PUSHPIN, cv);
                    } else {
                        ColumnValue cv = new ColumnValue().setImageURL(iconURL);
                        return fd.filterReturnedValue(DATA_PUSHPIN, cv);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.pushpin","Pushpin");
                }
            });

            // Property Key/Description/Value
            this.addColumnTemplate(new DataColumnTemplate(DATA_PROPERTY_KEY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_PROPERTY_KEY)) {
                        long propKey = fd.getLong(DATA_PROPERTY_KEY);
                        return fd.filterReturnedValue(DATA_PROPERTY_KEY,"0x"+StringTools.toHexString(propKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.propertyKey","Property\nKey");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PROPERTY_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String desc = fd.getString(DATA_PROPERTY_DESC);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_PROPERTY_DESC,desc);
                    } else
                    if (fd.hasValue(DATA_PROPERTY_KEY)) {
                        int propKey = (int)fd.getLong(DATA_PROPERTY_KEY);
                        //return PropertyKey.GetKeyDescription(propKey);
                        return fd.filterReturnedValue(DATA_PROPERTY_DESC,"0x"+StringTools.toHexString((long)propKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.propertyDescription","Property\nDescription");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PROPERTY_VALUE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String value = fd.getString(DATA_PROPERTY_VALUE);
                    return fd.filterReturnedValue(DATA_PROPERTY_VALUE,(value!=null)?value:"");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.propertyValue","Property\nValue");
                }
            });

            // Diagnostic Key/Description/Value
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_ERROR) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String isErr = fd.getString(DATA_DIAGNOSTIC_ERROR);
                    if (!StringTools.isBlank(isErr)) {
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_ERROR,isErr); // "true" : "false";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticError","Is Error?");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_KEY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_DIAGNOSTIC_ERROR) || fd.hasValue(DATA_DIAGNOSTIC_KEY)) {
                        boolean isError = fd.getBoolean(DATA_DIAGNOSTIC_ERROR);
                        long    diagKey = fd.getLong(DATA_DIAGNOSTIC_KEY);
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_KEY,(isError?"[E]":"[D]")+"0x"+StringTools.toHexString(diagKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticKey","Diagnostic\nKey");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String desc = fd.getString(DATA_DIAGNOSTIC_DESC);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_DESC,desc);
                    } else
                    if (fd.hasValue(DATA_DIAGNOSTIC_KEY)) {
                        int diagKey = (int)fd.getLong(DATA_DIAGNOSTIC_KEY);
                        //return ClientDiagnostic.GetKeyDescription(diagKey);
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_DESC,"0x"+StringTools.toHexString((long)diagKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticDescription","Diagnostic\nDescription");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_VALUE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String value = fd.getString(DATA_DIAGNOSTIC_VALUE);
                    return (value != null)? fd.filterReturnedValue(DATA_DIAGNOSTIC_VALUE,value) : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticValue","Diagnostic\nValue");
                }
            });

            // Entity-ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String desc = fd.getString(DATA_ENTITY_ID);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_ENTITY_ID,desc);
                    } else {
                        return fd.filterReturnedValue(DATA_ENTITY_ID,fd.getString(DATA_ENTITY_DESC));
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.entityID","Entity-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String desc = fd.getString(DATA_ENTITY_DESC);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_ENTITY_DESC,desc);
                    } else {
                        return fd.filterReturnedValue(DATA_ENTITY_DESC,fd.getString(DATA_ENTITY_ID));
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.entityDescription","Entity\nDescription");
                }
            });

            // Driver
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String drvID = fd.getString(DATA_DRIVER_ID);
                    if (StringTools.isBlank(drvID)) {
                        Device dev = fd.getDevice();
                        drvID = (dev != null)? dev.getDriverID() : "";
                    }
                    return fd.filterReturnedValue(DATA_DRIVER_ID,drvID);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.druiverID","Driver-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String desc = fd.getString(DATA_DRIVER_DESC);
                    if (!StringTools.isBlank(desc)) {
                        // explicitly specified
                        return fd.filterReturnedValue(DATA_DRIVER_DESC,desc);
                    } else {
                        // Driver ID
                        String drvID = fd.getString(DATA_DRIVER_ID);
                        if (StringTools.isBlank(drvID)) {
                            Device dev = fd.getDevice();
                            drvID = (dev != null)? dev.getDriverID() : null;
                        }
                        // Driver Description
                        if (StringTools.isBlank(drvID)) {
                            desc = drvID;
                        } else {
                            try {
                                Driver driver = Driver.getDriver(fd.getAccount(),drvID);
                                desc = (driver != null)? driver.getDescription() : drvID;
                            } catch (DBException dbe) {
                                desc = drvID;
                            }
                        }
                        return fd.filterReturnedValue(DATA_DRIVER_DESC,desc);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverDescription","Driver\nDescription");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_BADGEID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String badge = fd.getString(DATA_DRIVER_BADGEID);
                    if (!StringTools.isBlank(badge)) {
                        // explicitly specified
                        return fd.filterReturnedValue(DATA_DRIVER_BADGEID,badge);
                    } else {
                        // Driver ID
                        String drvID = fd.getString(DATA_DRIVER_ID);
                        if (StringTools.isBlank(drvID)) {
                            Device dev = fd.getDevice();
                            drvID = (dev != null)? dev.getDriverID() : null;
                        }
                        // Driver Description
                        if (StringTools.isBlank(drvID)) {
                            badge = "";
                        } else {
                            try {
                                Driver driver = Driver.getDriver(fd.getAccount(),drvID);
                                badge = (driver != null)? driver.getBadgeID() : "";
                            } catch (DBException dbe) {
                                badge = "";
                            }
                        }
                        return fd.filterReturnedValue(DATA_DRIVER_BADGEID,badge);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverBadge","Driver\nBadge-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_LICENSE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    String lic = fd.getString(DATA_DRIVER_LICENSE);
                    if (!StringTools.isBlank(lic)) {
                        // explicitly specified
                        return fd.filterReturnedValue(DATA_DRIVER_LICENSE,lic);
                    } else {
                        // Driver ID
                        String drvID = fd.getString(DATA_DRIVER_ID);
                        if (StringTools.isBlank(drvID)) {
                            Device dev = fd.getDevice();
                            drvID = (dev != null)? dev.getDriverID() : null;
                        }
                        // Driver License Number
                        if (StringTools.isBlank(drvID)) {
                            lic = "";
                        } else {
                            try {
                                Driver driver = Driver.getDriver(fd.getAccount(),drvID);
                                lic = (driver != null)? driver.getLicenseNumber() : "";
                            } catch (DBException dbe) {
                                lic = "";
                            }
                        }
                        return fd.filterReturnedValue(DATA_DRIVER_LICENSE,lic);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverLicense","Driver\nLicense");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_LICENSE_EXP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    long licExp = 0L;
                    if (fd.hasValue(DATA_DRIVER_LICENSE_EXP)) {
                        // explicitly specified
                        licExp = fd.getLong(DATA_DRIVER_LICENSE_EXP);
                    } else {
                        // Driver ID
                        String drvID = fd.getString(DATA_DRIVER_ID);
                        if (StringTools.isBlank(drvID)) {
                            Device dev = fd.getDevice();
                            drvID = (dev != null)? dev.getDriverID() : null;
                        }
                        // Driver License Expiration
                        if (!StringTools.isBlank(drvID)) {
                            try {
                                Driver driver = Driver.getDriver(fd.getAccount(),drvID);
                                licExp = (driver != null)? driver.getLicenseExpire() : 0L;
                            } catch (DBException dbe) {
                                licExp = 0L;
                            }
                        } else {
                            licExp = 0L;
                        }
                    }
                    if (licExp > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        DayNumber dn = new DayNumber(licExp);
                        String dnFmt = dn.format(rl.getDateFormat(rd.getPrivateLabel()));
                        ColumnValue cv = new ColumnValue(dnFmt).setSortKey(licExp);
                        return fd.filterReturnedValue(DATA_DRIVER_LICENSE_EXP, cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverLicenseExpire","Driver\nLicense Exp");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_BIRTHDATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    long bDay = 0L;
                    if (fd.hasValue(DATA_DRIVER_BIRTHDATE)) {
                        // explicitly specified
                        bDay = fd.getLong(DATA_DRIVER_BIRTHDATE);
                    } else {
                        // Driver ID
                        String drvID = fd.getString(DATA_DRIVER_ID);
                        if (StringTools.isBlank(drvID)) {
                            Device dev = fd.getDevice();
                            drvID = (dev != null)? dev.getDriverID() : null;
                        }
                        // Driver Birthday
                        if (!StringTools.isBlank(drvID)) {
                            try {
                                Driver driver = Driver.getDriver(fd.getAccount(),drvID);
                                bDay = (driver != null)? driver.getBirthdate() : 0L;
                            } catch (DBException dbe) {
                                bDay = 0L;
                            }
                        } else {
                            bDay = 0L;
                        }
                    }
                    if (bDay > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        DayNumber dn = new DayNumber(bDay);
                        String dnFmt = dn.format(rl.getDateFormat(rd.getPrivateLabel()));
                        ColumnValue cv = new ColumnValue(dnFmt).setSortKey(bDay);
                        return fd.filterReturnedValue(DATA_DRIVER_BIRTHDATE, cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverBirthday","Driver\nBirthday");
                }
            });

            // Latitude/Longitude/GeoPoint
            this.addColumnTemplate(new DataColumnTemplate(DATA_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_GEOPOINT) || fd.hasValue(DATA_LATITUDE)) {
                        Locale locale = rd.getLocale();
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude() : fd.getDouble(DATA_LATITUDE);
                        if (GeoPoint.isValid(lat,1.0)) {
                            arg = StringTools.trim(arg);
                            String valStr = "";
                            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                                valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                            } else
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                                valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                            } else {
                                String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                                valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                            }
                            return fd.filterReturnedValue(DATA_LATITUDE,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lat","Lat");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_GEOPOINT) || fd.hasValue(DATA_LONGITUDE)) {
                        Locale locale = rd.getLocale();
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_LONGITUDE);
                        if (GeoPoint.isValid(1.0,lon)) {
                            arg = StringTools.trim(arg);
                            String valStr = "";
                            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                                valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                            } else
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                                valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                            } else {
                                String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                                valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                            }
                            return fd.filterReturnedValue(DATA_LONGITUDE,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lon","Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_GEOPOINT) || fd.hasValue(DATA_LATITUDE) || fd.hasValue(DATA_LONGITUDE)) {
                        Locale locale = rd.getLocale();
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude()  : fd.getDouble(DATA_LATITUDE);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_LONGITUDE);
                        if (GeoPoint.isValid(lat,lon)) {
                            arg = StringTools.trim(arg);
                            String valStr = "";
                            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                                String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DMS, locale);
                                String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                                valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            } else
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM) || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                                String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DM , locale);
                                String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                                valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            } else {
                                String fmt    = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                                String latStr = GeoPoint.formatLatitude( lat, fmt  , locale);
                                String lonStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                                valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            }
                            return fd.filterReturnedValue(DATA_GEOPOINT,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.latLon","Lat/Lon");
                }
            });

            // Best Latitude/Longitude/GeoPoint
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    double lat = 999.0;
                    if (fd.hasValue(DATA_BEST_GEOPOINT)) {
                        lat = GeoPoint.getLatitude(fd.getGeoPoint(DATA_BEST_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_BEST_LATITUDE)) {
                        lat = fd.getDouble(DATA_BEST_LATITUDE);
                    } else
                    if (fd.hasValue(DATA_GEOPOINT)) {
                        lat = GeoPoint.getLatitude(fd.getGeoPoint(DATA_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_LATITUDE)) {
                        lat = fd.getDouble(DATA_LATITUDE);
                    }
                    if (GeoPoint.isValid(lat,1.0)) {
                        Locale locale = rd.getLocale();
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                        } else {
                            String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                        }
                        return fd.filterReturnedValue(DATA_BEST_LATITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bestLat","Lat");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    double lon = 999.0;
                    if (fd.hasValue(DATA_BEST_GEOPOINT)) {
                        lon = GeoPoint.getLongitude(fd.getGeoPoint(DATA_BEST_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_BEST_LONGITUDE)) {
                        lon = fd.getDouble(DATA_BEST_LONGITUDE);
                    } else
                    if (fd.hasValue(DATA_GEOPOINT)) {
                        lon = GeoPoint.getLongitude(fd.getGeoPoint(DATA_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_LONGITUDE)) {
                        lon = fd.getDouble(DATA_LONGITUDE);
                    }
                    if (GeoPoint.isValid(1.0,lon)) {
                        Locale locale = rd.getLocale();
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                        } else {
                            String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                        }
                        return fd.filterReturnedValue(DATA_BEST_LONGITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bestLon","Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    double lat = 999.0;
                    double lon = 999.0;
                    if (fd.hasValue(DATA_BEST_GEOPOINT)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_BEST_GEOPOINT);
                        lat = GeoPoint.getLatitude( gp,999.0);
                        lon = GeoPoint.getLongitude(gp,999.0);
                    } else
                    if (fd.hasValue(DATA_BEST_LATITUDE) && fd.hasValue(DATA_BEST_LONGITUDE)) {
                        lat = fd.getDouble(DATA_BEST_LATITUDE );
                        lon = fd.getDouble(DATA_BEST_LONGITUDE);
                    } else
                    if (fd.hasValue(DATA_GEOPOINT)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        lat = GeoPoint.getLatitude( gp,999.0);
                        lon = GeoPoint.getLongitude(gp,999.0);
                    } else
                    if (fd.hasValue(DATA_LATITUDE) && fd.hasValue(DATA_LONGITUDE)) {
                        lat = fd.getDouble(DATA_LATITUDE );
                        lon = fd.getDouble(DATA_LONGITUDE);
                    }
                    if (GeoPoint.isValid(lat,lon)) {
                        Locale locale = rd.getLocale();
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DMS, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM) || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DM , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else {
                            String fmt    = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            String latStr = GeoPoint.formatLatitude( lat, fmt  , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        }
                        return fd.filterReturnedValue(DATA_BEST_GEOPOINT,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bestLatLon","Lat/Lon");
                }
            });

            // Altitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ALTITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ALTITUDE)) {
                        double alt = fd.getDouble(DATA_ALTITUDE); // meters
                        if (Account.getDistanceUnits(rd.getAccount()).isMiles()) {
                            alt *= GeoPoint.FEET_PER_METER; // convert to feet
                        }
                        return fd.filterReturnedValue(DATA_ALTITUDE,formatDouble(alt,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.altitude","Altitude") + "\n${altitudeUnits}";
                }
            });
            
            // Speed
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_LIMIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_SPEED_LIMIT)) {
                        double kph = fd.getDouble(DATA_SPEED_LIMIT); // KPH
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            return fd.filterReturnedValue(DATA_SPEED_LIMIT,formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph),arg,"0"));
                        } else {
                            return "n/a ";
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speedLimit","Speed Limit") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double kph = fd.getDouble(DATA_SPEED); // KPH
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            return fd.filterReturnedValue(DATA_SPEED,formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph),arg,"0"));
                        } else {
                            return fd.filterReturnedValue(DATA_SPEED,"0   ");
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double kph = fd.getDouble(DATA_SPEED); // KPH
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            String speedStr = formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph), arg, "0");
                            String headStr  = GeoPoint.GetHeadingString(fd.getDouble(DATA_HEADING),rd.getLocale()).toUpperCase();
                            if (headStr.length() == 1) {
                                headStr += " ";
                            }
                            return fd.filterReturnedValue(DATA_SPEED_HEADING,speedStr+" "+headStr);
                        } else {
                            return "0   ";
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_UNITS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double kph = fd.getDouble(DATA_SPEED); // KPH
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            String unitAbbr = Account.getSpeedUnits(a).toString(rd.getLocale());
                            String speedStr = formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph), arg, "0");
                            String headStr  = GeoPoint.GetHeadingString(fd.getDouble(DATA_HEADING),rd.getLocale()).toUpperCase();
                            if (headStr.length() == 1) {
                                headStr += " ";
                            }
                            return fd.filterReturnedValue(DATA_SPEED_UNITS,speedStr+unitAbbr+" "+headStr);
                        } else {
                            return "0    ";
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speed","Speed");
                }
            });

            // Heading
            this.addColumnTemplate(new DataColumnTemplate(DATA_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double speed = fd.getDouble(DATA_SPEED); // KPH
                        if (speed > 0.0) {
                            double heading = fd.getDouble(DATA_HEADING);
                            if (!StringTools.isBlank(arg)) {
                                return fd.filterReturnedValue(DATA_HEADING,formatDouble(heading, arg, "0"));
                            } else {
                                return fd.filterReturnedValue(DATA_HEADING,GeoPoint.GetHeadingString(heading,rd.getLocale()).toUpperCase());
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return GeoPoint.GetHeadingTitle(rd.getLocale());
                }
            });

            // Distance/Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_DISTANCE)) {
                        double dist = fd.getDouble(DATA_DISTANCE); // kilometers
                        if (dist > 0.0) {
                            dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                            return fd.filterReturnedValue(DATA_DISTANCE,formatDouble(dist, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.distance","Distance") + "\n${distanceUnits}";
                }
            });

            // Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    double odom = 0.0;
                    if (fd.hasValue(DATA_ODOMETER)) {
                        odom = fd.getDouble(DATA_ODOMETER); // kilometers
                    } else
                    if (fd.hasValue(DATA_DISTANCE)) {
                        odom = fd.getDouble(DATA_DISTANCE); // kilometers
                    } else {
                        if (dev != null) {
                            odom = dev.getLastOdometerKM();
                        }
                    }
                    if (odom > 0.0) {
                        if (dev != null) {
                            odom += dev.getOdometerOffsetKM(); // ok
                        }
                        odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                        return fd.filterReturnedValue(DATA_ODOMETER,formatDouble(odom,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.odometer","Odometer") + "\n${distanceUnits}";
                }
            });

            // Start Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_START_ODOMETER)) {
                        double odom = fd.getDouble(DATA_START_ODOMETER); // kilometers
                        if (odom > 0.0) {
                            Device dev = fd.getDevice();
                            if (dev != null) {
                                odom += dev.getOdometerOffsetKM();
                            }
                            odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                            return fd.filterReturnedValue(DATA_START_ODOMETER,formatDouble(odom, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startOdometer","Start\nOdometer") + "\n${distanceUnits}";
                }
            });

            // Odometer Delta (ie. "Miles Driven")
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER_DELTA) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ODOMETER_DELTA)) {
                        double deltaOdom = fd.getDouble(DATA_ODOMETER_DELTA); // kilometers
                        if (deltaOdom >= 0.0) {
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_START_ODOMETER) && fd.hasValue(DATA_STOP_ODOMETER)) {
                        double startOdom = fd.getDouble(DATA_START_ODOMETER); // kilometers
                        double stopOdom  = fd.getDouble(DATA_STOP_ODOMETER);  // kilometers
                        double deltaOdom = stopOdom - startOdom;
                        if (deltaOdom >= 0.0) {
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                 }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driven","Driven") + "\n${distanceUnits}";
                }
            });

            // Last Service Odometer km
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_LAST) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device   dev = fd.getDevice();
                    double  dist = 0.0;
                    if (fd.hasValue(DATA_SERVICE_LAST)) {
                        dist = fd.getDouble(DATA_SERVICE_LAST); // kilometers
                    } else
                    if (dev != null) {
                        dist = dev.getMaintOdometerKM0(); // kilometers
                    } else {
                        // not available
                    }
                    if (dist > 0.0) {
                        if (dev != null) {
                            dist += dev.getOdometerOffsetKM(); // ok
                        }
                        dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        return fd.filterReturnedValue(DATA_SERVICE_LAST,formatDouble(dist,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintLastService","Last Service") + "\n${distanceUnits}";
                }
            });

            // Service Period km
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_INTERVAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    double dist = 0.0;
                    if (fd.hasValue(DATA_SERVICE_INTERVAL)) {
                        dist = fd.getDouble(DATA_SERVICE_INTERVAL); // kilometers
                    } else {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            dist = dev.getMaintIntervalKM0(); // kilometers
                        }
                    }
                    if (dist > 0.0) {
                        dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        return fd.filterReturnedValue(DATA_SERVICE_INTERVAL,formatDouble(dist,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintInterval","Service\nInterval");
                }
            });

            // Next Service Odometer km
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_NEXT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    double dist = 0.0;
                    if (fd.hasValue(DATA_SERVICE_NEXT)) {
                        dist = fd.getDouble(DATA_SERVICE_NEXT); // kilometers
                    } else
                    if (dev != null) {
                        dist = dev.getMaintOdometerKM0() + dev.getMaintIntervalKM0(); // km
                    }
                    if (dist > 0.0) {
                        if (dev != null) {
                            dist += dev.getOdometerOffsetKM(); // ok
                        }
                        double distU = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        String distS = formatDouble(distU, arg, "#0");
                        double odom  = dev.getLastOdometerKM(); // DATA_ODOMETER?
                        if (dev != null) {
                            odom += dev.getOdometerOffsetKM(); // ok
                        }
                        if ((odom > 0.0) && (odom >= dist)) {
                            // beyond service time
                            return (new ColumnValue(distS)).setForegroundColor(ColorTools.RED);
                        } else {
                            // have not reached service time yet
                            return fd.filterReturnedValue(DATA_SERVICE_NEXT,distS);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintNextService","Next Service") + "\n${distanceUnits}";
                }
            });

            // Remaining Km until next Service (next - odometer)
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_REMAINING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    double remaining = 0.0;
                    if (fd.hasValue(DATA_SERVICE_REMAINING)) {
                        remaining = fd.getDouble(DATA_SERVICE_REMAINING); // kilometers
                    } else {
                        // next
                        double next = 0.0;
                        if (fd.hasValue(DATA_SERVICE_NEXT)) {
                            next = fd.getDouble(DATA_SERVICE_NEXT); // kilometers
                        } else
                        if (dev != null) {
                            next = dev.getMaintOdometerKM0() + dev.getMaintIntervalKM0(); // km
                        }
                        // odometer
                        double odom = 0.0;
                        if (fd.hasValue(DATA_ODOMETER)) {
                            odom = fd.getDouble(DATA_ODOMETER); // kilometers
                        } else
                        if (fd.hasValue(DATA_DISTANCE)) {
                            odom = fd.getDouble(DATA_DISTANCE); // kilometers
                        } else
                        if (dev != null) {
                            odom = dev.getLastOdometerKM();
                        }
                        remaining = ((next > 0.0) && (odom > 0.0))? (next - odom) : 0.0;
                    }
                    if (remaining != 0.0) { // may be < 0.0
                        double distU = Account.getDistanceUnits(rd.getAccount()).convertFromKM(remaining);
                        String distS = formatDouble(distU, arg, "#0");
                        if (remaining >= 0.0) {
                            return fd.filterReturnedValue(DATA_SERVICE_REMAINING,distS);
                        } else {
                            return (new ColumnValue(distS)).setForegroundColor(ColorTools.RED);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintRemaining","Remaining") + "\n${distanceUnits}";
                }
            });

            // Service Notes
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_NOTES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    String notes = (dev != null)? dev.getMaintNotes() : "";
                    return fd.filterReturnedValue(DATA_SERVICE_NOTES,notes);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintServiceNotes","Service Notes");
                }
            });

            // Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_ADDRESS,fd.getString(DATA_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    RequestProperties reqState = rd.getRequestProperties();
                    String addrTitles[] = (reqState != null)? reqState.getAddressTitles() : null;
                    String addrTitle    = (ListTools.size(addrTitles) > 0)? addrTitles[0] : null;
                    if (!StringTools.isBlank(addrTitle)) {
                        return addrTitle;
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        return i18n.getString("FieldLayout.address","Address");
                    }
                }
            });

            // City
            this.addColumnTemplate(new DataColumnTemplate(DATA_CITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_CITY,fd.getString(DATA_CITY));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.city","City");
                }
            });

            // State/Province
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_STATE,fd.getString(DATA_STATE));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stateProvince","State\nProvince");
                }
            });

            // Country
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNTRY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_COUNTRY,fd.getString(DATA_COUNTRY));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.country","Country");
                }
            });

            // Subdivision
            this.addColumnTemplate(new DataColumnTemplate(DATA_SUBDIVISION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_SUBDIVISION,fd.getString(DATA_SUBDIVISION));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.subdivision","Subdivision");
                }
            });

            // Geozone ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_GEOZONE_ID,fd.getString(DATA_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.geozoneID","Geozone-ID");
                }
            });

            // Geozone Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_GEOZONE_DESC)) {
                        return fd.filterReturnedValue(DATA_GEOZONE_DESC,fd.getString(DATA_GEOZONE_DESC));
                    } else
                    if (fd.hasValue(DATA_GEOZONE_ID)) {
                        String geozoneID = fd.getString(DATA_GEOZONE_ID);
                        try {
                            Geozone gz[] = Geozone.getGeozone(rd.getAccount(), geozoneID);
                            return !ListTools.isEmpty(gz)? gz[0].getDescription() : "";
                        } catch (DBException dbe) {
                            // error
                        }
                    }
                    return rc.getBlankFiller();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.geozoneDescription","Geozone\nDescription");
                }
            });

            // %Utilization
            this.addColumnTemplate(new DataColumnTemplate(DATA_UTILIZATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_UTILIZATION)) {
                        double util = fd.getDouble(DATA_UTILIZATION) * 100.0;
                        return fd.filterReturnedValue(DATA_UTILIZATION,formatDouble(util,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.utilization","%Util");
                }
            });

            // Count
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_COUNT)) {
                        long count = fd.getLong(DATA_COUNT);
                        if (count >= 0L) {
                            return fd.filterReturnedValue(DATA_COUNT,String.valueOf(count));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.count","Count");
                }
            });

            // Start time
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_START_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_START_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_START_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startDateTime","Start\nDate/Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_START_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_START_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_START_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startTimestamp","Start\nTimestamp");
                }
            });

            // Enter Geozone ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_ENTER_GEOZONE_ID,fd.getString(DATA_ENTER_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterGeozoneID","Arrive\nGeozone-ID");
                }
            });

            // Enter Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_ENTER_ADDRESS,fd.getString(DATA_ENTER_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterAddress","Arrive\nAddress");
                }
            });

            // Enter time
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ENTER_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_ENTER_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_ENTER_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterDateTime","Arrive\nDate/Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ENTER_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_ENTER_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_ENTER_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterTimestamp","Arrive\nTimestamp");
                }
            });

            // Stop time
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_STOP_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_STOP_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopDateTime","Stop\nDate/Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_STOP_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_STOP_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopTimestamp","Stop\nTimestamp");
                }
            });

            // Exit Geozone ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_EXIT_GEOZONE_ID,fd.getString(DATA_EXIT_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitGeozoneID","Departure\nGeozone-ID");
                }
            });

            // Exit Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_EXIT_ADDRESS,fd.getString(DATA_EXIT_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitAddress","Departure\nAddress");
                }
            });

            // Exit time
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_EXIT_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_EXIT_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()),tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_EXIT_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitDateTime","Departure\nDate/Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_EXIT_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_EXIT_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_EXIT_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitTimestamp","Departure\nTimestamp");
                }
            });

            // Stop Latitude/Longitude/GeoPoint
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_GEOPOINT) || fd.hasValue(DATA_STOP_LATITUDE)) {
                        Locale locale = rd.getLocale();
                        GeoPoint gp = fd.getGeoPoint(DATA_STOP_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude() : fd.getDouble(DATA_STOP_LATITUDE);
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                        } else {
                            String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                        }
                        return fd.filterReturnedValue(DATA_STOP_LATITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lat","Lat");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_GEOPOINT) || fd.hasValue(DATA_STOP_LONGITUDE)) {
                        Locale locale = rd.getLocale();
                        GeoPoint gp = fd.getGeoPoint(DATA_STOP_GEOPOINT);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_STOP_LONGITUDE);
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                        } else {
                            String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                        }
                        return fd.filterReturnedValue(DATA_STOP_LONGITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lon","Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_GEOPOINT) || fd.hasValue(DATA_STOP_LATITUDE) || fd.hasValue(DATA_STOP_LONGITUDE)) {
                        Locale locale = rd.getLocale();
                        GeoPoint gp = fd.getGeoPoint(DATA_STOP_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude()  : fd.getDouble(DATA_STOP_LATITUDE);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_STOP_LONGITUDE);
                        if (GeoPoint.isValid(lat,lon)) {
                            arg = StringTools.trim(arg);
                            String valStr = "";
                            Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                                String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DMS, locale);
                                String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                                valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            } else
                            if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM) || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                                String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DM , locale);
                                String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                                valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            } else {
                                String fmt    = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                                String latStr = GeoPoint.formatLatitude( lat, fmt  , locale);
                                String lonStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                                valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            }
                            return fd.filterReturnedValue(DATA_STOP_GEOPOINT,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.latLon","Lat/Lon");
                }
            });
            
            // Stop Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_ODOMETER)) {
                        double odom = fd.getDouble(DATA_STOP_ODOMETER); // kilometers
                        if (odom > 0.0) {
                            Device dev = fd.getDevice();
                            if (dev != null) {
                                odom += dev.getOdometerOffsetKM();
                            }
                            odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                            return fd.filterReturnedValue(DATA_STOP_ODOMETER,formatDouble(odom, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.odometer","Odometer") + "\n${distanceUnits}";
                }
            });

            // Stop Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_STOP_ADDRESS,fd.getString(DATA_STOP_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.address","Address");
                }
            });

            // (Generic) Elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_ELAPSE_SEC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ELAPSE_SEC)) {
                        long elapsedSec = fd.getLong(DATA_ELAPSE_SEC,-1L);
                        if (elapsedSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(elapsedSec,fmt)).setSortKey(elapsedSec);
                            return fd.filterReturnedValue(DATA_ELAPSE_SEC, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.elapsedTime","Elapsed\nTime");
                }
            });

            // Inside Elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_INSIDE_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_INSIDE_ELAPSED)) {
                        long elapsedSec = fd.getLong(DATA_INSIDE_ELAPSED,-1L);
                        if (elapsedSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(elapsedSec,fmt)).setSortKey(elapsedSec);
                            return fd.filterReturnedValue(DATA_INSIDE_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.insideElapsed","Inside\nElapsed");
                }
            });

            // Outside Elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_OUTSIDE_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_OUTSIDE_ELAPSED)) {
                        long elapsedSec = fd.getLong(DATA_OUTSIDE_ELAPSED,-1L);
                        if (elapsedSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(elapsedSec,fmt)).setSortKey(elapsedSec);
                            return fd.filterReturnedValue(DATA_OUTSIDE_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.outsideElapsed","Outside\nElapsed");
                }
            });

            // Driving/Moving elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVING_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_DRIVING_ELAPSED)) {
                        long driveSec = fd.getLong(DATA_DRIVING_ELAPSED,-1L);
                        if (driveSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_DRIVING_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.drivingElapsed","Driving\nElapsed");
                }
            });

            // Stopped elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_ELAPSED)) {
                        long stopSec = fd.getLong(DATA_STOP_ELAPSED,-1L);
                        if (stopSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(stopSec,fmt)).setSortKey(stopSec);
                            return fd.filterReturnedValue(DATA_STOP_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stoppedElapsed","Stopped\nElapsed");
                }
            });

            // Stop count (number of stops)
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_COUNT)) {
                        long stopCount = fd.getLong(DATA_STOP_COUNT,-1L);
                        if (stopCount >= 0L) {
                            return fd.filterReturnedValue(DATA_STOP_COUNT, String.valueOf(stopCount));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopCount","Num. of\nStops");
                }
            });

            // Idle elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_IDLE_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_IDLE_ELAPSED)) {
                        long idleSec = fd.getLong(DATA_IDLE_ELAPSED,-1L);
                        if (idleSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(idleSec,fmt)).setSortKey(idleSec);
                            if (idleSec >= DateTime.HourSeconds(2)) {
                                //cv.setForegroundColor(ColorTools.RED);
                            }
                            return fd.filterReturnedValue(DATA_IDLE_ELAPSED,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.idleElapsed","Idle\nElapsed");
                }
            });

            // Attached
            this.addColumnTemplate(new DataColumnTemplate(DATA_ATTACHED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ATTACHED)) {
                        return fd.filterReturnedValue(DATA_ATTACHED,String.valueOf(fd.getBoolean(DATA_ATTACHED)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.attached","Attached");
                }
            });

            // IP address (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_IPADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_IPADDRESS,fd.getString(DATA_IPADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ipAddress","IP Address");
                }
            });

            // Is Duplex (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ISDUPLEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ISDUPLEX)) {
                        return fd.filterReturnedValue(DATA_ISDUPLEX,String.valueOf(fd.getBoolean(DATA_ISDUPLEX)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.isDuplex","Is Duplex?");
                }
            });

            // TCP Connections (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_TCP_CONNECTIONS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_TCP_CONNECTIONS)) {
                        return fd.filterReturnedValue(DATA_TCP_CONNECTIONS,String.valueOf(fd.getLong(DATA_TCP_CONNECTIONS,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.tcpConnections","TCP\nConnects");
                }
            });

            // UDP Connections (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_UDP_CONNECTIONS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_UDP_CONNECTIONS)) {
                        return fd.filterReturnedValue(DATA_UDP_CONNECTIONS,String.valueOf(fd.getLong(DATA_UDP_CONNECTIONS,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.udpConnections","UDP\nConnects");
                }
            });

            // TCP/UDP Connections (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CONNECTIONS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_CONNECTIONS)) {
                        return fd.filterReturnedValue(DATA_CONNECTIONS,String.valueOf(fd.getLong(DATA_CONNECTIONS,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.connections","Connections");
                }
            });

            // Bytes Read (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_READ) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_BYTES_READ)) {
                        return fd.filterReturnedValue(DATA_BYTES_READ,String.valueOf(fd.getLong(DATA_BYTES_READ,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesRead","Bytes\nRead");
                }
            });

            // Bytes Overhead (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_OVERHEAD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_BYTES_OVERHEAD)) {
                        return fd.filterReturnedValue(DATA_BYTES_OVERHEAD,String.valueOf(fd.getLong(DATA_BYTES_OVERHEAD,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesOverhead","Bytes\nOverhead");
                }
            });

            // Bytes Written (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_WRITTEN) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_BYTES_WRITTEN)) {
                        return fd.filterReturnedValue(DATA_BYTES_WRITTEN,String.valueOf(fd.getLong(DATA_BYTES_WRITTEN,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesWritten","Bytes\nWritten");
                }
            });

            // Bytes Total (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_BYTES_TOTAL)) {
                        return fd.filterReturnedValue(DATA_BYTES_TOTAL,String.valueOf(fd.getLong(DATA_BYTES_TOTAL,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesTotal","Bytes\nTotal");
                }
            });

            // Bytes Rounded (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_ROUNDED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_BYTES_ROUNDED)) {
                        return fd.filterReturnedValue(DATA_BYTES_ROUNDED,String.valueOf(fd.getLong(DATA_BYTES_ROUNDED,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesRounded","Bytes\nRounded");
                }
            });

            // Events Received (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_EVENTS_RECEIVED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_EVENTS_RECEIVED)) {
                        return fd.filterReturnedValue(DATA_EVENTS_RECEIVED,String.valueOf(fd.getLong(DATA_EVENTS_RECEIVED,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.eventsReceived","Events\nReceived");
                }
            });

            // Engine RPM (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_RPM) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ENGINE_RPM)) {
                        return fd.filterReturnedValue(DATA_ENGINE_RPM,String.valueOf(fd.getLong(DATA_ENGINE_RPM,0L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.engineRpm","Engine\nRPM");
                }
            });

            // Hours (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_ENGINE_HOURS)) {
                        double hours = fd.getDouble(DATA_ENGINE_HOURS,0.0);
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_ENGINE_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.engineHours","Engine\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_IDLE_HOURS)) {
                        double hours = fd.getDouble(DATA_IDLE_HOURS,0.0);
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_IDLE_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.idleHours","Idle\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_WORK_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_WORK_HOURS)) {
                        double hours = fd.getDouble(DATA_WORK_HOURS,0.0);
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_WORK_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.workHours","Work\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PTO_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_PTO_HOURS)) {
                        double hours = fd.getDouble(DATA_PTO_HOURS,0.0);
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_PTO_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ptoHours","PTO\nHours");
                }
            });

            // Fuel (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_CAPACITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_CAPACITY)) {
                        double vol = fd.getDouble(DATA_FUEL_CAPACITY); // liters
                        if (vol > 0.0) {
                            vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                            return fd.filterReturnedValue(DATA_FUEL_CAPACITY,formatDouble(vol, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelCapacity","Fuel Capacity") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_LEVEL)) {
                        double level = fd.getDouble(DATA_FUEL_LEVEL); // percent
                        if (level <= 0.0) {
                            return rc.getBlankFiller();
                        } else
                        if (level <= 1.0) {
                            String p = Math.round(level*100.0) + "%";
                            return fd.filterReturnedValue(DATA_FUEL_LEVEL, p);  // percent
                        } else {
                            String p = "100%";
                            return fd.filterReturnedValue(DATA_FUEL_LEVEL, p);  // percent
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelLevel","Fuel Level") + "\n%";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_TOTAL)) {
                        double vol = fd.getDouble(DATA_FUEL_TOTAL); // liters
                        if (vol > 0.0) {
                            vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                            return fd.filterReturnedValue(DATA_FUEL_TOTAL,formatDouble(vol, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelTotal","Total Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TRIP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_TRIP)) {
                        double vol = fd.getDouble(DATA_FUEL_TRIP); // liters
                        if (vol > 0.0) {
                            vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                            return fd.filterReturnedValue(DATA_FUEL_TRIP,formatDouble(vol, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelTrip","Trip Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_IDLE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_IDLE)) {
                        double vol = fd.getDouble(DATA_FUEL_IDLE); // liters
                        if (vol > 0.0) {
                            vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                            return fd.filterReturnedValue(DATA_FUEL_IDLE,formatDouble(vol, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelIdle","Idle Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_WORK) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_WORK)) {
                        double vol = fd.getDouble(DATA_FUEL_WORK); // liters
                        if (vol > 0.0) {
                            vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                            return fd.filterReturnedValue(DATA_FUEL_WORK,formatDouble(vol, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_FUEL_TOTAL) && fd.hasValue(DATA_FUEL_IDLE)) {
                        double vol = fd.getDouble(DATA_FUEL_TOTAL) - fd.getDouble(DATA_FUEL_IDLE); // liters
                        if (vol > 0.0) {
                            vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                            return fd.filterReturnedValue(DATA_FUEL_WORK,formatDouble(vol, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelWork","Work Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_PTO) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_PTO)) {
                        double vol = fd.getDouble(DATA_FUEL_PTO); // liters
                        if (vol > 0.0) {
                            vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                            return fd.filterReturnedValue(DATA_FUEL_PTO,formatDouble(vol, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelPTO","PTO Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_ECONOMY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_ECONOMY)) {
                        double econ = fd.getDouble(DATA_FUEL_ECONOMY); // kilometers per liter
                        if (econ > 0.0) {
                            econ = Account.getEconomyUnits(rd.getAccount()).convertFromKPL(econ);
                            return fd.filterReturnedValue(DATA_FUEL_ECONOMY,formatDouble(econ, arg, "#0.0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelEcon","Fuel Econ") + "\n${economyUnits}";
                }
            });

            // Start Fuel
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_FUEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_START_FUEL)) {
                        double fuel = fd.getDouble(DATA_START_FUEL); // Liters
                        if (fuel > 0.0) {
                            Device dev = fd.getDevice();
                            fuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(fuel);
                            return fd.filterReturnedValue(DATA_START_FUEL,formatDouble(fuel, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startFuel","Start\nFuel Total") + "\n${volumeUnits}";
                }
            });

            // Stop Fuel
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_FUEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_STOP_FUEL)) {
                        double fuel = fd.getDouble(DATA_STOP_FUEL); // Liters
                        if (fuel > 0.0) {
                            Device dev = fd.getDevice();
                            fuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(fuel);
                            return fd.filterReturnedValue(DATA_STOP_FUEL,formatDouble(fuel, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startFuel","Start\nFuel Total") + "\n${volumeUnits}";
                }
            });

            // Fuel Delta (ie. "Gallons Consumed")
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_DELTA) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    if (fd.hasValue(DATA_FUEL_DELTA)) {
                        double deltaFuel = fd.getDouble(DATA_FUEL_DELTA); // Liters
                        if (deltaFuel >= 0.0) {
                            deltaFuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(deltaFuel);
                            return fd.filterReturnedValue(DATA_FUEL_DELTA,formatDouble(deltaFuel,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_START_FUEL) && fd.hasValue(DATA_STOP_FUEL)) {
                        double startFuel = fd.getDouble(DATA_START_FUEL); // Liters
                        double stopFuel  = fd.getDouble(DATA_STOP_FUEL);  // Liters
                        double deltaFuel = stopFuel - startFuel;
                        if (deltaFuel >= 0.0) {
                            deltaFuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(deltaFuel);
                            return fd.filterReturnedValue(DATA_FUEL_DELTA,formatDouble(deltaFuel,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                 }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelUsed","Fuel Used") + "\n${volumeUnits}";
                }
            });

            // last connect/checkin date/time (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    long ts = 0L;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        ts = dev.getLastTotalConnectTime();
                        if (ts <= 0L) {
                            try {
                                EventData lastEv = dev.getLastEvent(-1L, false);
                                if (lastEv != null) {
                                    ts = lastEv.getTimestamp();
                                }
                            } catch (DBException dbe) {
                                // error retrieving event record
                            }
                        }
                    }
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        if (ageSec >= DateTime.HourSeconds(24)) {
                            cv.setForegroundColor(ColorTools.RED);
                        }
                        return fd.filterReturnedValue(DATA_CHECKIN_DATETIME, cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastCheckinTime","Last Check-In\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    long ts = 0L;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        ts = dev.getLastTotalConnectTime();
                        if (ts <= 0L) {
                            try {
                                EventData lastEv = dev.getLastEvent(-1L, false);
                                if (lastEv != null) {
                                    ts = lastEv.getTimestamp();
                                }
                            } catch (DBException dbe) {
                                // error retrieving event record
                            }
                        }
                    }
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        long days   = (ageSec / DateTime.DaySeconds(1));
                        long hours  = (ageSec % DateTime.DaySeconds(1)) / DateTime.HourSeconds(1);
                        long min    = (ageSec % DateTime.HourSeconds(1)) / DateTime.MinuteSeconds(1);
                        StringBuffer sb = new StringBuffer();
                        sb.append(days ).append("d ");
                        if (hours < 10) { sb.append("0"); }
                        sb.append(hours).append("h ");
                        if (min   < 10) { sb.append("0"); }
                        sb.append(min  ).append("m");
                        ColumnValue cv = new ColumnValue(sb.toString()).setSortKey(ageSec);
                        if (ageSec >= DateTime.HourSeconds(24)) {
                            cv.setForegroundColor(ColorTools.RED);
                        }
                        return fd.filterReturnedValue(DATA_CHECKIN_AGE,cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastCheckinAge","Since Last\nCheck-In");
                }
            });

            // last IP address (DataTransport record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_IPADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    DTIPAddress ipAddr = (dev != null)? dev.getDataTransport().getIpAddressCurrent() : null;
                    return (ipAddr != null)? fd.filterReturnedValue(DATA_LAST_IPADDRESS,ipAddr.toString()) : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastIPAddress","Last IP\nAddress");
                }
            });

            // Code Version (DataTransport record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CODE_VERSION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    String cv = (dev != null)? dev.getDataTransport().getCodeVersion() : null;
                    return (cv != null)? fd.filterReturnedValue(DATA_CODE_VERSION,cv) : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.codeVersion","Code\nVersion");
                }
            });

            // custom field value (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CUSTOM_FIELD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Device dev = fd.getDevice();
                    String value = dev.getCustomAttribute(arg);
                    return !StringTools.isBlank(value)? value : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg = rc.getArg();
                    String desc = rd.getPrivateLabel().getStringProperty(BasicPrivateLabel.PROP_DeviceInfo_custom_ + arg, null);
                    if (!StringTools.isBlank(desc)) {
                        if (desc.length() > 12) {
                            int p = desc.lastIndexOf(" ");
                            if (p > 0) {
                                desc = desc.substring(0,p) + "\n" + desc.substring(p+1);
                            }
                        }
                        return desc;
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        return i18n.getString("FieldLayout.customAttribute","Custom\nAttribute");
                    }
                }
            });

            // last login date/time (Account record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_LOGIN_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    long ts = 0L;
                    if (fd.hasValue(DATA_LOGIN_DATETIME)) {
                        ts = fd.getLong(DATA_LOGIN_DATETIME);
                    } else {
                        Account acct = fd.getAccount();
                        ts = (acct != null)? acct.getLastLoginTime() : 0L;
                    }
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        if (ageSec >= DateTime.DaySeconds(30)) {
                            cv.setForegroundColor(ColorTools.RED);
                        }
                        return fd.filterReturnedValue(DATA_LOGIN_DATETIME, cv);
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        String never = i18n.getString("FieldLayout.loginNever","never");
                        ColumnValue cv = new ColumnValue(never).setSortKey(0);
                        cv.setForegroundColor(ColorTools.RED);
                        return fd.filterReturnedValue(DATA_LOGIN_DATETIME, cv);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastLoginTime","Last Login\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LOGIN_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    long ts = 0L;
                    if (fd.hasValue(DATA_LOGIN_DATETIME)) {
                        ts = fd.getLong(DATA_LOGIN_DATETIME);
                    } else {
                        Account acct = fd.getAccount();
                        ts = (acct != null)? acct.getLastLoginTime() : 0L;
                    }
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        long days   = (ageSec / DateTime.DaySeconds(1));
                        long hours  = (ageSec % DateTime.DaySeconds(1)) / DateTime.HourSeconds(1);
                        long min    = (ageSec % DateTime.HourSeconds(1)) / DateTime.MinuteSeconds(1);
                        StringBuffer sb = new StringBuffer();
                        sb.append(days ).append("d ");
                        if (hours < 10) { sb.append("0"); }
                        sb.append(hours).append("h ");
                        if (min   < 10) { sb.append("0"); }
                        sb.append(min  ).append("m");
                        ColumnValue cv = new ColumnValue(sb.toString()).setSortKey(ageSec);
                        if (ageSec >= DateTime.DaySeconds(30)) {
                            cv.setForegroundColor(ColorTools.RED);
                        }
                        return fd.filterReturnedValue(DATA_LOGIN_AGE,cv);
                    } else {
                        ColumnValue cv = new ColumnValue(rc.getBlankFiller()).setSortKey(999999999L);
                        cv.setForegroundColor(ColorTools.RED);
                        return fd.filterReturnedValue(DATA_LOGIN_AGE,cv);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastLoginAge","Since Last\nLogin");
                }
            });
            
            // Account active
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCOUNT_ACTIVE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Account acct = fd.getAccount();
                    if (acct != null) {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        boolean isActive = acct.isActive();
                        String value = isActive?
                            i18n.getString("FieldLayout.activeYes","Yes") :
                            i18n.getString("FieldLayout.activeNo" ,"No" );
                        return fd.filterReturnedValue(DATA_ACCOUNT_ACTIVE,value);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.accountActive","Account\nActive");
                }
            });
            
            // Account device Count
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Account acct = fd.getAccount();
                    if (acct != null) {
                        long devCount = acct.getDeviceCount();
                        return fd.filterReturnedValue(DATA_DEVICE_COUNT,String.valueOf(devCount));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceCount","Device\nCount");
                }
            });
            
            // Account PrivateLabel name
            this.addColumnTemplate(new DataColumnTemplate(DATA_PRIVATE_LABEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    Account acct = fd.getAccount();
                    if (acct != null) {
                        String privLabel = acct.getPrivateLabelName();
                        return fd.filterReturnedValue(DATA_PRIVATE_LABEL,privLabel);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.privateLabelName","PrivateLabel\nName");
                }
            });

            // Left-align string #1
            this.addColumnTemplate(new DataColumnTemplate(DATA_LEFT_ALIGN_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_LEFT_ALIGN_1,fd.getString(DATA_LEFT_ALIGN_1));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.leftAlign1","String 1");
                }
            });

            // Left-align string #2
            this.addColumnTemplate(new DataColumnTemplate(DATA_LEFT_ALIGN_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_LEFT_ALIGN_2,fd.getString(DATA_LEFT_ALIGN_2));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.leftAlign1","String 1");
                }
            });

            // Right-align string #1
            this.addColumnTemplate(new DataColumnTemplate(DATA_RIGHT_ALIGN_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_RIGHT_ALIGN_1,fd.getString(DATA_RIGHT_ALIGN_1));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.rightAlign1","String 1");
                }
            });

            // Right-align string #2
            this.addColumnTemplate(new DataColumnTemplate(DATA_RIGHT_ALIGN_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)obj;
                    return fd.filterReturnedValue(DATA_RIGHT_ALIGN_2,fd.getString(DATA_RIGHT_ALIGN_2));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.rightAlign2","String 2");
                }
            });

            // Blank space (this was included per a users request)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BLANK_SPACE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    return rc.getBlankFiller();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return "";
                }
            });

        }
    }
   
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final int ELAPSED_FORMAT_HHMMSS   = 0; // "HH:MM:SS";
    public static final int ELAPSED_FORMAT_HHMM     = 1; // "HH:MM";
    public static final int ELAPSED_FORMAT_HHHhh    = 2; // "HHH.hh";
    public static final int ELAPSED_FORMAT_HHHh     = 3; // "HHH.h";

    protected static int getElapsedFormat(char fmt, int dft)
    {
        switch (fmt) {
            case '0': return ELAPSED_FORMAT_HHMMSS;
            case '1': return ELAPSED_FORMAT_HHMM  ;
            case '2': return ELAPSED_FORMAT_HHHhh ;
            case '3': return ELAPSED_FORMAT_HHHh  ;
            default : return dft;
        }
    }

    protected static int getElapsedFormat(String arg, int dft)
    {
        if ((arg != null) && (arg.length() > 0)) {
            return FieldLayout.getElapsedFormat(arg.charAt(0), dft);
        } else {
            return dft;
        }
    }

    protected static String formatElapsedTime(long elapsedSec, int fmt)
    {
        StringBuffer sb = new StringBuffer();
        switch (fmt) {
            case ELAPSED_FORMAT_HHMMSS : {
                int h = (int)(elapsedSec / (60L * 60L));   // Hours
                int m = (int)((elapsedSec / 60L) % 60);    // Minutes
                int s = (int)(elapsedSec % 60);            // Seconds
                sb.append(StringTools.format(h,"0"));
                sb.append(":");
                sb.append(StringTools.format(m,"00"));
                sb.append(":");
                sb.append(StringTools.format(s,"00"));
            } break;
            case ELAPSED_FORMAT_HHMM : {
                int h = (int)(elapsedSec / (60L * 60L));   // Hours
                int m = (int)((elapsedSec / 60L) % 60);    // Minutes
                int s = (int)(elapsedSec % 60);            // Seconds
                if (s > 30) {
                    if (++m > 59) {
                        h++;
                        m = 0;
                    }
                }
                sb.append(StringTools.format(h,"0"));
                sb.append(":");
                sb.append(StringTools.format(m,"00"));
            } break;
            case ELAPSED_FORMAT_HHHhh : {
                double h = ((double)elapsedSec / (60.0 * 60.0));   // Hours
                sb.append(StringTools.format(h,"0.00"));
            } break;
            case ELAPSED_FORMAT_HHHh : {
                double h = ((double)elapsedSec / (60.0 * 60.0));   // Hours
                sb.append(StringTools.format(h,"0.0"));
            } break;
        }
        return sb.toString();
    }

}
