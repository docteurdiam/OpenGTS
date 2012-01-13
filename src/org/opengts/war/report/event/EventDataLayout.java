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
//  Report definition based on EventData table
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/01/10  Martin D. Flynn
//     -Added fields 'checkinDateTime', 'checkinAge'
//  2008/02/04  Martin D. Flynn
//     -Added fields 'engineRpm', 'fuelUsed'
//  2008/03/12  Martin D. Flynn
//     -Added additional decimal point options to various fields
//  2008/04/11  Martin D. Flynn
//     -Added color indicator to lat/lon when gps age is over a given threshold
//     -Added field 'gpsAge'
//  2008/05/14  Martin D. Flynn
//     -Added City/State/Country/Subdivision fields
//  2008/10/16  Martin D. Flynn
//     -Added battery level field.
//     -Added input mask field.
//  2009/01/01  Martin D. Flynn
//     -Added arguments to "heading" to allow displaying in degrees
//  2010/09/09  Martin D. Flynn
//     -Added "ambientTemp", "barometer", "deviceBattery"
//  2011/03/08  Martin D. Flynn
//     -Added "driverStatus"
//  2011/06/16  Martin D. Flynn
//     -Added status code/description coloring option
//     -Added "mapLink" feature to "index" column to support displaying the map
//      with a specific pushpin info-balloon displayed.
//  2011/07/15  Martin D. Flynn
//     -Added "analog#" fields.  Fixed "batteryVolts" column to use "getBatteryVolts()".
//  2011/08/21  Martin D. Flynn
//     -Added "tirePressure", "tireTemp"
//  2011/10/03  Martin D. Flynn
//     -Added "turboPressure", "day*"
// ----------------------------------------------------------------------------
package org.opengts.war.report.event;

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
import org.opengts.war.tools.WebPageAdaptor;
import org.opengts.war.tools.ComboOption;
import org.opengts.war.tools.MapDimension;

import org.opengts.war.report.*;

public class EventDataLayout
    extends ReportLayout
{

    // ------------------------------------------------------------------------

    // TODO: these colors/thresholds should be configurable at runtime
    private static final long   GPS_AGE_THRESHOLD_1     = DateTime.MinuteSeconds(60);
    private static final String GPS_AGE_COLOR_1         = "#BB0000";  // lighter red
    private static final long   GPS_AGE_THRESHOLD_2     = DateTime.MinuteSeconds(20);
    private static final String GPS_AGE_COLOR_2         = "#550000";  // darker red

    // ------------------------------------------------------------------------

    private static final long  MINIMUM_REASONABLE_TIMESTAMP = (new DateTime(null,2000,1,1)).getTimeSec();

    // ------------------------------------------------------------------------
    // Data keys
    // - These define what data is available (see 'EventDataRow') and what columns will be 
    //   displayed in the table.
    // - Column names must only contain <alpha>/<numeric>/'_' characters
    
    public static final String  DATA_INDEX              = "index";
    
    public static final String  DATA_DEVICE_ID          = "deviceId";
    public static final String  DATA_DEVICE_DESC        = "deviceDesc";             // Device record
    public static final String  DATA_DEVICE_BATTERY     = "deviceBattery";          // Device record
    public static final String  DATA_DEVICE_CREATION    = "deviceCreateDateTime";   // Device record

    public static final String  DATA_DATE               = "date";
    public static final String  DATA_TIME               = "time";
    public static final String  DATA_DATETIME           = "dateTime";
    public static final String  DATA_TIMESTAMP          = "timestamp";

    public static final String  DATA_GROUP_ID           = "groupId";
    public static final String  DATA_STATUS_CODE        = "statusCode";
    public static final String  DATA_STATUS_DESC        = "statusDesc";
    public static final String  DATA_PUSHPIN            = "pushpin";
    public static final String  DATA_GPS_AGE            = "gpsAge";
    public static final String  DATA_LATITUDE           = "latitude";
    public static final String  DATA_LONGITUDE          = "longitude";
    public static final String  DATA_ACCURACY           = "accuracy";
    public static final String  DATA_GEOPOINT           = "geoPoint";
    public static final String  DATA_ALTITUDE           = "altitude";
    public static final String  DATA_SATELLITES         = "satellites";
    public static final String  DATA_SPEED_LIMIT        = "speedLimit";
    public static final String  DATA_SPEED              = "speed";
    public static final String  DATA_SPEED_HEADING      = "speedH";
    public static final String  DATA_SPEED_UNITS        = "speedU";
    public static final String  DATA_HEADING            = "heading";
    public static final String  DATA_DISTANCE           = "distance";
    public static final String  DATA_ODOMETER           = "odometer";
    
    public static final String  DATA_CELL_LATITUDE      = "cellLatitude";       
    public static final String  DATA_CELL_LONGITUDE     = "cellLongitude";      
    public static final String  DATA_CELL_GEOPOINT      = "cellGeoPoint";       
    public static final String  DATA_CELL_ACCURACY      = "cellAccuracy";
    
    public static final String  DATA_BEST_LATITUDE      = "bestLatitude";       
    public static final String  DATA_BEST_LONGITUDE     = "bestLongitude";      
    public static final String  DATA_BEST_GEOPOINT      = "bestGeoPoint";       
    public static final String  DATA_BEST_ACCURACY      = "bestAccuracy";

    public static final String  DATA_BATTERY            = "battery";
    public static final String  DATA_BATTERY_VOLTS      = "batteryVolts";
    public static final String  DATA_BATTERY_PERCENT    = "batteryPercent";

    public static final String  DATA_INPUT_STATE        = "inputState";
    
    public static final String  DATA_ANALOG_0           = "analog0";
    public static final String  DATA_ANALOG_1           = "analog1";
    public static final String  DATA_ANALOG_2           = "analog2";
    public static final String  DATA_ANALOG_3           = "analog3";

    public static final String  DATA_ADDRESS            = "address";
    public static final String  DATA_CITY               = "city";
    public static final String  DATA_STATE              = "state";
    public static final String  DATA_COUNTRY            = "country";
    public static final String  DATA_SUBDIVISION        = "subdivision";
    
    public static final String  DATA_GEOZONE_ID         = "geozoneId";
    public static final String  DATA_GEOZONE_DESC       = "geozoneDesc";

    public static final String  DATA_ENTITY_ID          = "entityId";
    public static final String  DATA_ENTITY_DESC        = "entityDesc";
    
    public static final String  DATA_DRIVER_ID          = "driverId";
    public static final String  DATA_DRIVER_DESC        = "driverDesc";
    public static final String  DATA_DRIVER_STATUS      = "driverStatus";
    public static final String  DATA_DRIVER_MESSAGE     = "driverMessage";
    
    public static final String  DATA_JOB_NUMBER         = "jobNumber";
    
    public static final String  DATA_RFID_TAG           = "rfidTag";
    
    public static final String  DATA_SAMPLE_INDEX       = "sampleIndex";
    public static final String  DATA_SAMPLE_ID          = "sampleId";
  //public static final String  DATA_APPLIED_PRESSURE   = "appliedPressure";

    public static final String  DATA_ETA_DATETIME       = "etaDateTime";
    public static final String  DATA_ETA_TIMESTAMP      = "etaTimestamp";
    public static final String  DATA_ETA_UNIQUE_ID      = "etaUniqueID";            // ETA ID
    public static final String  DATA_ETA_DISTANCE       = "etaDistance";            // ETA distance
    public static final String  DATA_ETA_LATITUDE       = "etaLatitude";            // ETA latitude
    public static final String  DATA_ETA_LONGITUDE      = "etaLongitude";           // ETA longitude
    public static final String  DATA_ETA_GEOPOINT       = "etaGeoPoint";            // ETA lat/lon
    
    public static final String  DATA_STOP_UNIQUE_ID     = "stopUniqueID";           // STOP ID
    public static final String  DATA_STOP_STATUS        = "stopStatus";             // STOP Status
    public static final String  DATA_STOP_STATUS_DESC   = "stopStatusDesc";         // STOP Status Description
    public static final String  DATA_STOP_INDEX         = "stopIndex";              // STOP Index

    public static final String  DATA_BAROMETER          = "barometer";
    public static final String  DATA_AMBIENT_TEMP       = "ambientTemp";

    public static final String  DATA_THERMO_1           = "thermo1";
    public static final String  DATA_THERMO_2           = "thermo2";
    public static final String  DATA_THERMO_3           = "thermo3";
    public static final String  DATA_THERMO_4           = "thermo4";
    public static final String  DATA_THERMO_5           = "thermo5";
    public static final String  DATA_THERMO_6           = "thermo6";
    public static final String  DATA_THERMO_7           = "thermo7";
    public static final String  DATA_THERMO_8           = "thermo8";

    public static final String  DATA_FUEL_LEVEL         = "fuelLevel";
    public static final String  DATA_FUEL_LEVEL_VOL     = "fuelLevelVolume";
    public static final String  DATA_FUEL_ECONOMY       = "fuelEconomy";
    public static final String  DATA_FUEL_TOTAL         = "fuelTotal";
    public static final String  DATA_FUEL_IDLE          = "fuelIdle";
    public static final String  DATA_FUEL_PRESSURE      = "fuelPressure";

    public static final String  DATA_FAULT_CODE         = "faultCode";
    public static final String  DATA_MALFUNCTION_LAMP   = "malfunctionLamp";
    public static final String  DATA_OIL_LEVEL          = "oilLevel";
    public static final String  DATA_OIL_PRESSURE       = "oilPressure";
    public static final String  DATA_OIL_TEMP           = "oilTemp";
    public static final String  DATA_ENGINE_RPM         = "engineRpm";
    public static final String  DATA_ENGINE_HOURS       = "engineHours";
    public static final String  DATA_ENGINE_LOAD        = "engineLoad";             // %
    public static final String  DATA_IDLE_HOURS         = "idleHours";              // hours
    public static final String  DATA_TRANS_OIL_TEMP     = "transOilTemp";           // C
    public static final String  DATA_COOLANT_LEVEL      = "coolantLevel";           // %
    public static final String  DATA_COOLANT_TEMP       = "coolantTemp";            // C
    public static final String  DATA_BRAKE_G_FORCE      = "brakeGForce";            // G
    public static final String  DATA_BRAKE_FORCE        = "brakeForce";
    public static final String  DATA_BRAKE_PRESSURE     = "brakePressure";          // kPa
    public static final String  DATA_BRAKE_POSITION     = "brakePos";               // %
    public static final String  DATA_ACCELERATION       = "acceleration";           // 
    public static final String  DATA_PTO_ENGAGED        = "ptoEngaged";
    public static final String  DATA_PTO_HOURS          = "ptoHours";
    public static final String  DATA_VEH_BATTERY_VOLTS  = "vBatteryVolts";
    public static final String  DATA_THROTTLE_POSITION  = "throttlePos";            // %
    public static final String  DATA_INTAKE_TEMP        = "intakeTemp";             // C
    public static final String  DATA_AIR_PRESSURE       = "airPressure";            // kPa
    public static final String  DATA_AIR_FILTER_PRESS   = "airFilterPressure";      // kPa
    public static final String  DATA_TURBO_PRESS        = "turboPressure";          // kPa
    public static final String  DATA_TIRE_PRESSURE      = "tirePressure";           // kPa
    public static final String  DATA_TIRE_TEMPERATURE   = "tireTemp";               // C

    public static final String  DATA_EVENT_FIELD        = "eventField";

    public static final String  DATA_CHECKIN_DATETIME   = "checkinDateTime";        // Device record
    public static final String  DATA_CHECKIN_AGE        = "checkinAge";             // Device record
    public static final String  DATA_CUSTOM_FIELD       = "customField";            // Device record
    public static final String  DATA_LAST_BATTERY_PCT   = "lastBatteryPercent";     // Device record
    public static final String  DATA_FUEL_CAPACITY      = "fuelCapacity";           // Device record

    public static final String  DATA_RAW_DATA           = "rawData";

    public static final String  DATA_CREATE_DATE        = "createDate";
    public static final String  DATA_CREATE_TIME        = "createTime";
    public static final String  DATA_CREATE_DATETIME    = "createDateTime";
    public static final String  DATA_CREATE_TIMESTAMP   = "createTimestamp";

    public static final String  DATA_DAY_ENGINE_STARTS  = "dayEngineStarts";
    public static final String  DATA_DAY_IDLE_HOURS     = "dayIdleHours";
    public static final String  DATA_DAY_FUEL_IDLE      = "dayFuelIdle";
    public static final String  DATA_DAY_WORK_HOURS     = "dayWorkHours";
    public static final String  DATA_DAY_FUEL_WORK      = "dayFuelWork";
    public static final String  DATA_DAY_FUEL_PTO       = "dayFuelPTO";
    public static final String  DATA_DAY_FUEL_TOTAL     = "dayFuelTotal";
    public static final String  DATA_DAY_DISTANCE       = "dayDistance";

    // ------------------------------------------------------------------------
    // EventDataLayout is a singleton

    private static EventDataLayout reportDef = null;

    /**
    *** Gets the EventDataLayout singleton instance
    *** @return The EventDataLayout singleton instance
    **/
    public static ReportLayout getReportLayout()
    {
        if (reportDef == null) {
            reportDef = new EventDataLayout();
        }
        return reportDef;
    }
    
    /**
    *** Standard singleton constructor
    **/
    private EventDataLayout()
    {
        super();
        this.setDataRowTemplate(new EventDataRow());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* format double value */
    protected static String formatDouble(double value, String arg, String dftFmt)
    {
        String fmt = dftFmt;
        if (!StringTools.isBlank(arg)) {
            switch (arg.charAt(0)) {
                case '0': fmt = "0"        ; break;
                case '1': fmt = "0.0"      ; break;
                case '2': fmt = "0.00"     ; break;
                case '3': fmt = "0.000"    ; break;
                case '4': fmt = "0.0000"   ; break;
                case '5': fmt = "0.00000"  ; break;
                case '6': fmt = "0.000000" ; break;
                case '7': fmt = "0.0000000"; break;
            }
        }
        return StringTools.format(value, fmt);
    }
    
    // ------------------------------------------------------------------------

    /* format temperatures */
    protected static String formatTemperature(double thermoC, String arg, ReportData rd, String dft)
    {
        if (EventData.isValidTemperature(thermoC)) {
            Account a       = rd.getAccount();
            double thermo   = Account.getTemperatureUnits(a).convertFromC(thermoC);
            String unitAbbr = Account.getTemperatureUnits(a).toString(rd.getLocale());
            return EventDataLayout.formatDouble(thermo, arg, "0.0") + unitAbbr;
        } else
        if (dft != null) {
            return dft;
        } else {
            I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
            String na = i18n.getString("EventDataLayout.notAvailable","n/a");
            return "--";
        }
    }

    /* format kilometer distance */
    protected static String formatKM(double dist, String arg, ReportData rd)
    {
        if (dist > 0.0) {
            dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
            return EventDataLayout.formatDouble(dist, arg, "0");
        } else {
            return "";
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static class EventDataRow
        extends DataRowTemplate
    {
        public EventDataRow() {
            super();

            // Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    if (rowNdx >= 0) {
                        String arg = rc.getArg();
                        if (StringTools.isBlank(arg)) {
                            return String.valueOf(rowNdx + 1);
                        } else
                        if (arg.equalsIgnoreCase("map")    ||
                            arg.equalsIgnoreCase("mapLink")  ) {
                            //Print.logWarn("** Index Map-Link feature not yet fully supported! **");
                            String ndxVal = String.valueOf(rowNdx + 1);
                            URIArg mapURL = rd.getMapURL();
                            if (mapURL != null) {
                                mapURL.addArg("showpp",rowNdx+1);
                                mapURL.addArg("zoompp",rowNdx+1);
                                RequestProperties reqState = rd.getRequestProperties();
                                MapDimension sz = rd.getMapWindowSize();
                                int W = sz.getWidth();
                                int H = sz.getHeight();
                                String encMapURL = WebPageAdaptor.EncodeURL(reqState,mapURL);
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(" "+ndxVal+" ");
                                cv.setLinkURL("javascript:openResizableWindow('"+encMapURL+"','ReportMap',"+W+","+H+");",null);
                                return cv;
                            }
                            return ndxVal;
                        } else 
                        if (arg.charAt(0) == '0') {
                            return String.valueOf(rowNdx);
                        } else {
                            return String.valueOf(rowNdx + 1);
                        }
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return "#";
                }
            });

            // Device-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getDeviceID();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceID","Device-ID");
                }
            });

            // Device Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    if (dev != null) {
                        return dev.getDescription();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceDescription","Device\nDescription");
                }
            });

            // Device Battery-Level
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_BATTERY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    if (dev != null) {
                        double level = dev.getLastBatteryLevel();
                        if (level <= 0.0) {
                            return rc.getBlankFiller();
                        } else
                        if (level <= 1.0) {
                            return Math.round(level*100.0) + "%";           // percent
                        } else {
                            return EventDataLayout.formatDouble(level, arg, "0.0") + "v";   // volts
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceBattery","Last\nBattery");
                }
            });

            // Device creation date/time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_CREATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    long ts = dev.getCreationTime();
                    if (ts > MINIMUM_REASONABLE_TIMESTAMP) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                        return cv;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceCrationTime","Device Create\nTime");
                }
            });

            // Group-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GROUP_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    if (dev != null) {
                        return dev.getGroupID();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.groupID","Group-ID");
                }
            });

            // Event timestamp Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.date","Date");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        return dt.format(rl.getTimeFormat(rd.getPrivateLabel()), tz);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.time","Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dateTime","Date/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.timestamp","Timestamp") + "\n(Epoch)";
                }
            });

            // Event creation Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createDate","Insert\nDate");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        return dt.format(rl.getTimeFormat(rd.getPrivateLabel()), tz);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createTime","insert\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createDateTime","Insert\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createTimestamp","Insert\nTimestamp") + "\n(Epoch)";
                }
            });

            // Status Code/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_CODE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    String scCode = "0x" + StringTools.toHexString((long)ed.getStatusCode(),16);
                    if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("color")) {
                        StatusCodeProvider scp = ed.getStatusCodeProvider(rd.getPrivateLabel());
                        if (scp == null) {
                            return scCode;
                        } else
                        if (StringTools.isBlank(scp.getForegroundColor()) && 
                            StringTools.isBlank(scp.getBackgroundColor())   ) {
                            return scCode;
                        } else {
                            ColumnValue cv = new ColumnValue();
                            cv.setValue(scCode);
                            cv.setForegroundColor(scp.getForegroundColor());
                            cv.setBackgroundColor(scp.getBackgroundColor());
                            return cv;
                        }
                    } else {
                        // arg.equalsIgnoreCase("noColor")
                        return scCode;
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.statusCode","Status#");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("color")) {
                        StatusCodeProvider scp = ed.getStatusCodeProvider(rd.getPrivateLabel());
                        if (scp == null) {
                            String scDesc = "0x" + StringTools.toHexString((long)ed.getStatusCode(),16);
                            return scDesc;
                        } else {
                            String scDesc = scp.getDescription(rd.getLocale());
                            if (StringTools.isBlank(scp.getForegroundColor()) && 
                                StringTools.isBlank(scp.getBackgroundColor())   ) {
                                return scDesc;
                            } else {
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(scDesc);
                                cv.setForegroundColor(scp.getForegroundColor());
                                cv.setBackgroundColor(scp.getBackgroundColor());
                                return cv;
                            }
                        }
                    } else { 
                        // arg.equalsIgnoreCase("noColor")
                        String scDesc = ed.getStatusCodeDescription(rd.getPrivateLabel());
                        return scDesc;
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.statusDescription","Status");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PUSHPIN) {
                // EXPERIMENTAL! (the icons produced by this code section may not exactly match
                // those produced on the actual map by the JavaScript functions.
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    RequestProperties  reqState = rd.getRequestProperties();
                    BasicPrivateLabel  bpl      = rd.getPrivateLabel();
                    OrderedSet<String> iconKeys = reqState.getMapProviderIconKeys();
                    int       ppNdx = ed.getPushpinIconIndex(null/*iconSelector*/, iconKeys, false/*isFleet*/, bpl);
                    PushpinIcon ppi = reqState.getPushpinIcon(ppNdx);
                    String  iconURL = (ppi != null)? ppi.getIconEvalURL(ed,rowNdx) : "";
                    ColumnValue  cv = new ColumnValue().setImageURL(iconURL);
                    return cv;
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.pushpin","Pushpin");
                }
            });

            // Entity ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getEntityID();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.entityID","Entity-ID");
                }
            });

            // Entity Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    String aid = ed.getAccountID();
                    String eid = ed.getEntityID();
                    return Device.getEntityDescription(aid, eid, EntityManager.EntityType.TRAILER.getIntValue());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.entityDescription","Entity\nDescription");
                }
            });

            // Driver
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getDriverID();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverID","Driver-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    // Driver ID
                    String drvID = ed.getDriverID();
                    if (StringTools.isBlank(drvID)) {
                        Device dev = ed.getDevice();
                        drvID = (dev != null)? dev.getDriverID() : null;
                    }
                    // Driver Description
                    String desc = drvID;
                    if (!StringTools.isBlank(drvID)) {
                        try {
                            Driver driver = Driver.getDriver(ed.getAccount(),drvID);
                            desc = (driver != null)? driver.getDescription() : drvID;
                        } catch (DBException dbe) {
                            desc = drvID;
                        }
                    }
                    return desc;
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverDescription","Driver\nDescription");
                }
            });

            // Driver Status
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_STATUS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return String.valueOf(ed.getDriverStatus());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverStatus","Driver\nStatus");
                }
            });

            // Driver Message
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_MESSAGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getDriverMessage(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverMessage","Driver\nMessage");
                }
            });

            // JobNumber
            this.addColumnTemplate(new DataColumnTemplate(DATA_JOB_NUMBER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getJobNumber(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.jobNumber","Job\nNumber");
                }
            });

            // RFID Tag (Bar Code)
            this.addColumnTemplate(new DataColumnTemplate(DATA_RFID_TAG) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getRfidTag(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.rfidTag","RFID/Bar\nCode");
                }
            });
            
            // Sample ID/Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_SAMPLE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getSampleID(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.sampleID","Sample\nID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SAMPLE_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return String.valueOf(ed.getSampleIndex());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.sampleIndex","Sample\nIndex");
                }
            });

            // ETA date/time
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getEtaTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaDateTime","ETA\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getEtaTimestamp();
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaTimestamp","ETA\nTimestamp") + "\n(Epoch)";
                }
            });

            // ETA Unique ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_UNIQUE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getEtaUniqueID();
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaUniqueID","ETA\nUniqueID");
                }
            });

            // ETA Distance
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getEtaDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaDistance","ETA\nDistance") + "\n${distanceUnits}";
                }
            });

            // ETA Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getEtaLatitude();
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
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaLat","ETA\nLat");
                }
            });
            
            // ETA Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getEtaLongitude();
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
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaLon","ETA Lon");
                }
            });
            
            // ETA Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getEtaLatitude();
                    double lon = ed.getEtaLongitude();
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
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaLatLon","ETA Lat/Lon");
                }
            });

            // Stop Unique ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_UNIQUE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long id = ed.getStopID();
                    return String.valueOf(id);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopUniqueID","Stop\nUniqueID");
                }
            });

            // Stop Status
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_STATUS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long sst = ed.getStopStatus();
                    return String.valueOf(sst);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopStatus","Stop\nStatus");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_STATUS_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    int sst = ed.getStopStatus();
                    return DCServerFactory.Garmin_getStopStatusDescription(locale, sst);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopStatusDesc","Stop\nStatus");
                }
            });

            // Stop Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int ndx = ed.getStopIndex();
                    return String.valueOf(ndx);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopIndex","Stop\nIndex");
                }
            });

            // General applied pressure
            /*
            this.addColumnTemplate(new DataColumnTemplate(DATA_APPLIED_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getAppliedPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa > 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return StringTools.format(pressure, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.applyPressure","Appled Press.") + "\n${pressureUnits}";
                }
            });
            */

            // GPS Age
            this.addColumnTemplate(new DataColumnTemplate(DATA_GPS_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long gpsAge = ed.getGpsAge();
                    if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                        return (new ColumnValue(gpsAge)).setForegroundColor(GPS_AGE_COLOR_1).setFontStyleItalic();
                    } else
                    if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                        return (new ColumnValue(gpsAge)).setForegroundColor(GPS_AGE_COLOR_2);
                    } else {
                        return String.valueOf(gpsAge);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.gpsAge","GPS\nAge");
                }
            });

            // Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getLatitude();
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
                    long gpsAge = ed.getGpsAge();
                    if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                        return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_1).setFontStyleItalic();
                    } else
                    if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                        return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_2);
                    } else
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lat","Lat");
                }
            });
            
            // Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getLongitude();
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
                    long gpsAge = ed.getGpsAge();
                    if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                        return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_1).setFontStyleItalic();
                    } else
                    if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                        return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_2);
                    } else
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lon","Lon");
                }
            });
            
            // Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getLatitude();
                    double lon = ed.getLongitude();
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
                        long gpsAge = ed.getGpsAge();
                        if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                            return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_1).setFontStyleItalic();
                        } else
                        if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                            return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_2);
                        } else
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.latLon","Lat/Lon");
                }
            });

            // Accuracy
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCURACY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double acc = ed.getHorzAccuracy(); // meters
                    if (acc > 0) {
                        if (Account.getDistanceUnits(rd.getAccount()).isMiles()) {
                            acc *= GeoPoint.FEET_PER_METER; // convert to feet
                        }
                        return EventDataLayout.formatDouble(acc, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.accuracy","Accuracy") + "\n${accuracyUnits}";
                }
            });

            // Altitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ALTITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double alt = ed.getAltitude(); // meters
                    if (Account.getDistanceUnits(rd.getAccount()).isMiles()) {
                        alt *= GeoPoint.FEET_PER_METER; // convert to feet
                    }
                    return EventDataLayout.formatDouble(alt, arg, "0");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.altitude","Altitude") + "\n${altitudeUnits}";
                }
            });

            // Speed limit (posted speed)
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_LIMIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedLimitKPH(); // KPH
                    if (kph > 0.0) {
                        Account a = rd.getAccount();
                        double speed = Account.getSpeedUnits(a).convertFromKPH(kph);
                        return EventDataLayout.formatDouble(speed, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speedLimit","Speed Limit") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        Account a = rd.getAccount();
                        double speed = Account.getSpeedUnits(a).convertFromKPH(kph);
                        return EventDataLayout.formatDouble(speed, arg, "0");
                    } else {
                        return "0   ";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        Account a       = rd.getAccount();
                        double speed    = Account.getSpeedUnits(a).convertFromKPH(kph);
                        String speedStr = EventDataLayout.formatDouble(speed, arg, "0");
                        String headStr  = GeoPoint.GetHeadingString(ed.getHeading(),rd.getLocale()).toUpperCase();
                        if (headStr.length() == 1) {
                            headStr += " ";
                        }
                        return speedStr + " " + headStr;
                    } else {
                        return "0   ";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_UNITS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        Account a = rd.getAccount();
                        String unitAbbr = Account.getSpeedUnits(a).toString(rd.getLocale());
                        double speed    = Account.getSpeedUnits(a).convertFromKPH(kph);
                        String speedStr = EventDataLayout.formatDouble(speed, arg, "0");
                        String headStr  = GeoPoint.GetHeadingString(ed.getHeading(),rd.getLocale()).toUpperCase();
                        if (headStr.length() == 1) {
                            headStr += " ";
                        }
                        return speedStr + unitAbbr + " " + headStr;
                    } else {
                        return "0    ";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speed","Speed");
                }
            });

            // Heading
            this.addColumnTemplate(new DataColumnTemplate(DATA_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        double heading = ed.getHeading();
                        if (!StringTools.isBlank(arg)) {
                            return EventDataLayout.formatDouble(heading, arg, "0");
                        } else {
                            return GeoPoint.GetHeadingString(heading,rd.getLocale()).toUpperCase();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return GeoPoint.GetHeadingTitle(rd.getLocale());
                }
            });

            // #Satellites
            this.addColumnTemplate(new DataColumnTemplate(DATA_SATELLITES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int satCount = ed.getSatelliteCount();
                    if (satCount > 0) {
                        return String.valueOf(satCount);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.satelliteCount","Sat\nCount");
                }
            });

            // Distance
            this.addColumnTemplate(new DataColumnTemplate(DATA_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.distance","Distance") + "\n${distanceUnits}";
                }
            });

            // Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    double odom = ed.getOdometerKM(); // kilometers
                    if (odom <= 0.0) { odom = ed.getDistanceKM(); }
                    odom += dev.getOdometerOffsetKM(); // ok
                    if (odom > 0) {
                        return EventDataLayout.formatKM(odom, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(odom, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.odometer","Odometer") + "\n${distanceUnits}";
                }
            });

            // Cell Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getCellLatitude();
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
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellLat","Cell\nLat");
                }
            });
            
            // Cell Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getCellLongitude();
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
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellLon","Cell\nLon");
                }
            });

            // Cell Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getCellLatitude();
                    double lon = ed.getCellLongitude();
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
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellLatLon","Cell\nLat/Lon");
                }
            });

            // Cell Accuracy
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_ACCURACY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double acc = ed.getCellAccuracy(); // meters
                    if (acc > 0) {
                        if (Account.getDistanceUnits(rd.getAccount()).isMiles()) {
                            acc *= GeoPoint.FEET_PER_METER; // convert to feet
                        }
                        return EventDataLayout.formatDouble(acc, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellAccuracy","Cell Accuracy") + "\n${accuracyUnits}";
                }
            });

            // Best Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getBestLatitude();
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
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestLat","Lat");
                }
            });
            
            // Best Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getBestLongitude();
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
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestLon","Lon");
                }
            });

            // Best Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getBestLatitude();
                    double lon = ed.getBestLongitude();
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
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestLatLon","Lat/Lon");
                }
            });

            // Best Accuracy
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_ACCURACY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double acc = ed.getBestAccuracy(); // meters
                    if (acc > 0) {
                        if (Account.getDistanceUnits(rd.getAccount()).isMiles()) {
                            acc *= GeoPoint.FEET_PER_METER; // convert to feet
                        }
                        return EventDataLayout.formatDouble(acc, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestAccuracy","Accuracy") + "\n${accuracyUnits}";
                }
            });

            // Input Mask
            this.addColumnTemplate(new DataColumnTemplate(DATA_INPUT_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int input = (int)ed.getInputMask(); // bit mask
                    String s = StringTools.toBinaryString(input);
                    int slen = s.length();
                    int blen = StringTools.parseInt(arg,8);
                    return s.substring(slen - blen, slen);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.inputBitMask","Inputs\n(BitMask)");
                }
            });

            // Geozone-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getGeozoneID();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.geozoneID","Geozone-ID");
                }
            });

            // Geozone Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getGeozoneDescription();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.geozoneDescription","Geozone\nDescription");
                }
            });

            // Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getAddress();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    RequestProperties reqState = rd.getRequestProperties();
                    String addrTitles[] = (reqState != null)? reqState.getAddressTitles() : null;
                    String addrTitle    = (ListTools.size(addrTitles) > 0)? addrTitles[0] : null;
                    if (!StringTools.isBlank(addrTitle)) {
                        return addrTitle;
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.address","Address");
                    }
                }
            });
            
            // City
            this.addColumnTemplate(new DataColumnTemplate(DATA_CITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getCity();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.city","City");
                }
            });
            
            // State/Province
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getStateProvince();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stateProvince","State\nProvince");
                }
            });

            // Country
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNTRY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getCountry();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.country","Country");
                }
            });

            // Subdivision
            this.addColumnTemplate(new DataColumnTemplate(DATA_SUBDIVISION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getSubdivision();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.subdivision","Subdivision");
                }
            });
            
            // Atmosphere
            this.addColumnTemplate(new DataColumnTemplate(DATA_BAROMETER) {
                // Barometric pressure
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getBarometer(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa > 0.0) {
                        //double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        double pressure = Account.PressureUnits.MMHG.convertFromKPa(kPa); // always convert to mmHg
                        return StringTools.format(pressure, "#0.00");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.barometer","Barometer") + "\nmmHg";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_AMBIENT_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double thermo = ed.getAmbientTemp(); // degrees 'C'
                    return EventDataLayout.formatTemperature(thermo, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.ambientTemp","Ambient\nTemp");
                }
            });

            // Temperature (report index starts at '1')
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(0); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#1";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(1); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#2";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_3) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(2); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#3";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_4) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(3); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#4";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_5) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(4); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#5";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_6) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(5); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#6";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_7) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(6); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#7";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_8) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(7); // degrees 'C'
                    return EventDataLayout.formatTemperature(tempC, arg, rd, null);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#8";
                }
            });

            // Battery level (% or volts?)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BATTERY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getBatteryLevel();
                    double volts = ed.getBatteryVolts();
                    if (level > 0.0) {
                        if (level <= 1.0) {
                            return Math.round(level*100.0) + "%";           // percent
                        } else {
                            return EventDataLayout.formatDouble(level, arg, "0.0") + "v";   // volts
                        }
                    } else
                    if (volts > 0.0) {
                        return EventDataLayout.formatDouble(volts, arg, "0.0") + "v";   // volts
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.BatteryLevel","Battery\nLevel");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BATTERY_VOLTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getBatteryVolts();
                    if (level > 0.0) {
                        return EventDataLayout.formatDouble(level, arg, "0.0");  // volts
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.BatteryVolts","Battery\nVolts");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BATTERY_PERCENT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getBatteryLevel();
                    if (level > 0.0) {
                        double pct100 = (level <= 1.0)? (level*100.0) : level;
                        return Math.round(pct100) + "%";    // integer percent
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.BatteryPercent","Battery\n%");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_BATTERY_PCT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    double level = dev.getLastBatteryLevel();
                    if (level > 0.0) {
                        double pct100 = (level <= 1.0)? (level*100.0) : level;
                        return Math.round(pct100) + "%";    // integer percent
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.latestBatteryPercent","Latest\nBatt %");
                }
            });

            // Analog
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog0();
                    return EventDataLayout.formatDouble(level, arg, "0.0");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog0","Analog 0");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog1();
                    return EventDataLayout.formatDouble(level, arg, "0.0");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog1","Analog 1");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog2();
                    return EventDataLayout.formatDouble(level, arg, "0.0");  // volts
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog2","Analog 2");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_3) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog3();
                    return EventDataLayout.formatDouble(level, arg, "0.0");  // volts
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog3","Analog 3");
                }
            });

            // Fuel Capacity (device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_CAPACITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    double vol = dev.getFuelCapacity(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return StringTools.format(vol, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelCapacity","Fuel Capacity") + "\n${volumeUnits}";
                }
            });

            // Fuel
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double fuelLevel = ed.getFuelLevel();
                    if (fuelLevel > 0.0) {
                        return Math.round(fuelLevel*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelPercent","Fuel%");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL_VOL) { // see also DATA_FUEL_CAPACITY
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    double capacity = dev.getFuelCapacity(); // liters
                    double percent  = ed.getFuelLevel();
                    if ((percent <= 0.0) && (capacity <= 0.0)) {
                        return rc.getBlankFiller();
                    } else
                    if (capacity <= 0.0) {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.notAvailable","n/a");
                    } else {
                        double liters = capacity * percent; // liters
                        double vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(liters);
                        return StringTools.format(vol, "#0.0");
                    } 
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelLevelVolume","Fuel Level") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double vol = ed.getFuelTotal(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return StringTools.format(vol, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelTotal","Total Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_IDLE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double vol = ed.getFuelIdle(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return StringTools.format(vol, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelIdle","Idle Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_ECONOMY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double econ = ed.getFuelEconomy(); // kilometers per liter
                    if (econ > 0.0) {
                        econ = Account.getEconomyUnits(rd.getAccount()).convertFromKPL(econ);
                        return StringTools.format(econ, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelEcon","Fuel Econ") + "\n${economyUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getFuelPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return StringTools.format(pressure, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelPressure","Fuel Press.") + "\n${pressureUnits}";
                }
            });

            // PTO
            this.addColumnTemplate(new DataColumnTemplate(DATA_PTO_ENGAGED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    boolean pto = ed.getPtoEngaged();
                    return ComboOption.getYesNoText(rd.getLocale(), pto);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.ptoEngaged","PTO\nEngaged");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PTO_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getPtoHours();
                    if (hours > 0.0) {
                        return StringTools.format(hours,"#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.ptoHours","PTO\nHours");
                }
            });

            // Vehicle Battery Volts
            this.addColumnTemplate(new DataColumnTemplate(DATA_VEH_BATTERY_VOLTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double volts = ed.getVBatteryVolts();
                    if (volts > 0.0) {
                        // return EventDataLayout.formatDouble(volts, arg, "0");
                        return StringTools.format(volts,"#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.vBatteryVolts","Veh Batt.\nVolts");
                }
            });

            // Throttle Position
            this.addColumnTemplate(new DataColumnTemplate(DATA_THROTTLE_POSITION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double throttlePos = ed.getThrottlePos();
                    if (throttlePos > 0.0) {
                        return Math.round(throttlePos*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.throttlePosition","Throttle\nPosition");
                }
            });

            // Air
            this.addColumnTemplate(new DataColumnTemplate(DATA_INTAKE_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getIntakeTemp(); // degrees 'C'
                    if (C > 0.0) {
                        return EventDataLayout.formatTemperature(C, arg, rd, null);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.intakeTemp","Intake\nTemp.");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_AIR_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getAirPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return StringTools.format(pressure, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.airPressure","Air Press.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_AIR_FILTER_PRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getAirFilterPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return StringTools.format(pressure, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.airFilterPressure","Air Filt\nPress.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TURBO_PRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getTurboPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return StringTools.format(pressure, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.turboPressure","Turbo\nPress.") + "\n${pressureUnits}";
                }
            });

            // Tire pressure
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIRE_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa[] = ed.getTirePressure_kPa();
                    if (!ListTools.isEmpty(kPa)) {
                        Account.PressureUnits pu = Account.getPressureUnits(rd.getAccount());
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < kPa.length; i++) {
                            double pressure = pu.convertFromKPa(kPa[i]);
                            if (sb.length() > 0) { sb.append(","); }
                            sb.append(EventDataLayout.formatDouble(pressure, arg, "0"));
                        }
                        return sb.toString();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tirePressure","Tire Press.") + "\n${pressureUnits}";
                }
            });

            // Tire temperature
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIRE_TEMPERATURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C[] = ed.getTireTemp_C();
                    if (!ListTools.isEmpty(C)) {
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < C.length; i++) {
                            if (sb.length() > 0) { sb.append(","); }
                            String tempFmt = EventDataLayout.formatTemperature(C[i], arg, rd, "?");
                            sb.append(tempFmt);
                        }
                        return sb.toString();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tireTemperature","Tire Temp.");
                }
            });

            // Fault Code
            this.addColumnTemplate(new DataColumnTemplate(DATA_FAULT_CODE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = StringTools.trim(rc.getArg());
                    EventData ed = (EventData)obj;
                    long fault = ed.getOBDFault();
                    if (fault == 0L) {
                        return rc.getBlankFiller();
                    } else
                    if (!DTOBDFault.IsJ1708(fault) || !DTOBDFault.HasDescriptionProvider()) {
                        return DTOBDFault.GetFaultString(fault);
                    } else {
                        Locale locale = rd.getLocale();
                        if (arg.equalsIgnoreCase("link")) {
                            ColumnValue cv = new ColumnValue(DTOBDFault.GetFaultString(fault));
                            RequestProperties reqState = rd.getRequestProperties();
                            URIArg j1587URL = WebPageAdaptor.MakeURL(reqState.getBaseURI(),"j1587.show"); // Constants.PAGE_J1587_SHOW);
                            j1587URL.addArg("mid" , DTOBDFault.DecodeSystem(fault));
                            j1587URL.addArg("spid", DTOBDFault.DecodeSPID(fault));
                            j1587URL.addArg("fmi" , DTOBDFault.DecodeFMI(fault));
                            cv.setLinkURL("javascript:openResizableWindow('"+j1587URL+"','J1587Desc',320,100);",null);
                            return cv;
                        } else 
                        if (arg.equalsIgnoreCase("desc")) {
                            String desc = DTOBDFault.GetFaultDescription(fault, locale);
                            return desc;
                        } else {
                            return DTOBDFault.GetFaultString(fault);
                        }
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.j1708Fault","OBD Fault"); // + "\nMID/PID/FMI";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_MALFUNCTION_LAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    boolean mil = ed.getMalfunctionLamp();
                    return ComboOption.getOnOffText(rd.getLocale(), mil);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.malfunctionLamp","Malfunction\nLamp");
                }
            });

            // Engine oil level
            this.addColumnTemplate(new DataColumnTemplate(DATA_OIL_LEVEL) {
                // Oil pressure (http://en.wikipedia.org/wiki/KPa)
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double oilLvl = ed.getOilLevel(); // %
                    if (oilLvl > 0.0) {
                        return Math.round(oilLvl*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.oilLevel","Oil Level");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_OIL_PRESSURE) {
                // Oil pressure (http://en.wikipedia.org/wiki/KPa)
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getOilPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa > 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return StringTools.format(pressure, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.oilPressure","Oil Press.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_OIL_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getOilTemp(); // degrees 'C'
                    if (C > 0.0) {
                        return EventDataLayout.formatTemperature(C, arg, rd, null); 
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.oilTemp","Oil\nTemp.");
                }
            });

            // Engine
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_RPM) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long rpm = ed.getEngineRpm();
                    if (rpm >= 0L) {
                        return String.valueOf(rpm);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.engineRpm","Engine\nRPM");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getEngineHours();
                    if (hours > 0) {
                        return StringTools.format(hours,"#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.engineHours","Engine\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_LOAD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double engineLoad = ed.getEngineLoad();
                    if (engineLoad > 0.0) {
                        return Math.round(engineLoad*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.engineLoad","Engine\nLoad %");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getIdleHours();
                    if (hours > 0.0) {
                        return StringTools.format(hours,"#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.idleHours","Idle\nHours");
                }
            });

            // Transmission Oil
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRANS_OIL_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getTransOilTemp(); // degrees 'C'
                    if (C > 0.0) {
                        return EventDataLayout.formatTemperature(C, arg, rd, null); 
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.transOilTemp","Trans Oil\nTemp.");
                }
            });

            // Coolant
            this.addColumnTemplate(new DataColumnTemplate(DATA_COOLANT_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double coolantLevel = ed.getCoolantLevel();
                    if (coolantLevel > 0.0) {
                        return Math.round(coolantLevel*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.coolantLevel","Coolant\nLevel");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_COOLANT_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getCoolantTemp(); // degrees 'C'
                    if (C > 0.0) {
                        return EventDataLayout.formatTemperature(C, arg, rd, null);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.coolantTemp","Coolant\nTemp.");
                }
            });

            // Brake
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_G_FORCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg   = rc.getArg();
                    EventData ed = (EventData)obj;
                    double bgf   = ed.getBrakeGForce();
                    if (bgf != 0.0) {
                        return StringTools.format(bgf, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakeGForce","Braking\nG-force");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_FORCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg   = rc.getArg();
                    EventData ed = (EventData)obj;
                    double bgf   = ed.getBrakeGForce();
                    double kphs  = ((Account.MPSS_PER_G_FORCE * bgf) / 1000.0) * 3600.0; // km/hr/sec
                    if (kphs != 0.0) {
                        return EventDataLayout.formatKM(kphs, arg, rd);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakeForce","Braking\n${distanceUnits}/hr/sec");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa = ed.getBrakePressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return StringTools.format(pressure, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakePressure","Brake Press.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_POSITION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double throttlePos = ed.getBrakePos();
                    if (throttlePos > 0.0) {
                        return Math.round(throttlePos*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakePosition","Brake\nPosition");
                }
            });

            // Acceleration/Deceleration (meters per seconds-squared)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCELERATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg   = rc.getArg();
                    EventData ed = (EventData)obj;
                    double mss   = ed.getAcceleration();
                    if (mss != 0.0) {
                        return StringTools.format(mss, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.acceleration","Accel\nm/s/s");
                }
            });

            // last connect/checkin date/time (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    long ts = dev.getLastTotalConnectTime();
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
                    if (ts > MINIMUM_REASONABLE_TIMESTAMP) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        if (ageSec >= DateTime.HourSeconds(24)) {
                            cv.setForegroundColor("red");
                        }
                        return cv;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lastCheckinTime","Last Check-In\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    long ts = dev.getLastTotalConnectTime();
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
                    if (ts > MINIMUM_REASONABLE_TIMESTAMP) {
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
                            cv.setForegroundColor("red");
                        }
                        return cv;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lastCheckinAge","Since Last\nCheck-In");
                }
            });

            // custom field value (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CUSTOM_FIELD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev   = ed.getDevice();
                    String value = (dev != null)? dev.getCustomAttribute(arg) : "";
                    if (!StringTools.isBlank(value)) {
                        return value;
                    } else {
                        return rc.getBlankFiller();
                    }
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
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.customAttribute","Custom\nAttribute");
                    }
                }
            });

            // Raw data (unparsed event packet)
            this.addColumnTemplate(new DataColumnTemplate(DATA_RAW_DATA) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getRawData();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.rawData","Raw Data");
                }
            });

            // "Day" values
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_ENGINE_STARTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int engStarts = ed.getDayEngineStarts();
                    return String.valueOf(engStarts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayEngineStarts","Day Engine\nStarts");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getDayIdleHours();
                    if (hours > 0.0) {
                        return StringTools.format(hours,"#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayIdleHours","Day Idle\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_IDLE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double vol = ed.getDayFuelIdle(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return StringTools.format(vol, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelIdle","Day Idle\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_WORK_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getDayWorkHours();
                    if (hours > 0.0) {
                        return StringTools.format(hours,"#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayWorkHours","Day Work\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_WORK) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double vol = ed.getDayFuelWork(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return StringTools.format(vol, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelWork","Day Work\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_PTO) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double vol = ed.getDayFuelPTO(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return StringTools.format(vol, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelPTO","Day PTO\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double vol = ed.getDayFuelTotal(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return StringTools.format(vol, "#0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelTotal","Day Total\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getDayDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayDistance","Day\nDistance") + "\n${distanceUnits}";
                }
            });

            // General Event field
            this.addColumnTemplate(new DataColumnTemplate(DATA_EVENT_FIELD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    EventData ed = (EventData)obj;
                    String fldName = rc.getArg();
                    DBField edFld = EventData.getFactory().getField(fldName);
                    if (edFld != null) {
                        Object val = ed.getFieldValue(fldName);
                        return StringTools.trim(edFld.formatValue(val));
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale  = rd.getLocale();
                    String fldName = rc.getArg();
                    DBField  edFld = EventData.getFactory().getField(fldName);
                    if (edFld != null) {
                        String title = edFld.getTitle(locale);
                        return rc.getTitle(locale, title);
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        String title = i18n.getString("EventDataLayout.eventField","Event\nField");
                        return rc.getTitle(locale, title);
                    }
                }
            });

        }
    }
   
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
