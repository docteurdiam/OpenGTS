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
//  2007/03/25  Martin D. Flynn
//     -Initial release
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
//  2007/11/28  Martin D. Flynn
//     -Added start 'address' to go with start geoPoint
//     -Added stop geopoint/address to available report fields
//  2008/03/28  Martin D. Flynn
//     -Added limited reporting support for devices that do not support OpenDMTP.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/06/20  Martin D. Flynn
//     -Added support for displaying a report 'map'.
//  2009/01/01  Martin D. Flynn
//     -Added totals for drive/idle time and distance driven.
//     -Added 'minimumStoppedTime' property (for simulated start/stop events only).
//     -Added 'hasStartStopCodes' property to force simulated start/stop events.
//  2009/05/01  Martin D. Flynn
//     -Added support for "idle" elapsed time (ignition on and not moving).
//  2009/08/07  Martin D. Flynn
//     -Changed 'hasStartStopCode' to 'tripStartType'
//  2009/11/01  Martin D. Flynn
//     -Added property 'stopOnIgnitionOff'
//  2010/05/24  Martin D. Flynn
//     -Added idle accumulation to TRIP_ON_SPEED
// ----------------------------------------------------------------------------
package org.opengts.war.report.field;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class MotionReport
    extends ReportData
    implements DBRecordHandler<EventData>
{

    // ------------------------------------------------------------------------
    // Detail report
    // Multiple FieldData records per device
    // 'From'/'To' date
    // ------------------------------------------------------------------------
    // Columns:
    //   index startDateTime movingElapse stopDateTime idleElapse
    // ------------------------------------------------------------------------
    // It would be helpful if the following items were available from the device:
    //  - "minimumStoppedTime"
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Properties

    private static final String PROP_tripStartType          = "tripStartType";
    private static final String PROP_minimumStoppedTime     = "minimumStoppedTime";
    private static final String PROP_minimumSpeedKPH        = "minimumSpeedKPH";
    private static final String PROP_stopOnIgnitionOff      = "stopOnIgnitionOff";

    // ------------------------------------------------------------------------
    // Trip start types
    
    private static final String MOTION_DEFAULT[]            = new String[] { "default" };
    private static final String MOTION_SPEED[]              = new String[] { "speed", "motion" };
    private static final String MOTION_IGNITION[]           = new String[] { "ignition" };
    private static final String MOTION_STARTSTOP[]          = new String[] { "start", "startstop" };

    private static final int    TRIP_ON_SPEED               = 0; // idle time if ignition present
    private static final int    TRIP_ON_IGNITION            = 1; // no idle time
    private static final int    TRIP_ON_START               = 2; // idle time if ignition present
    
    private static String TripTypeName(int type)
    {
        switch (type) {
            case TRIP_ON_SPEED      : return "Speed";
            case TRIP_ON_IGNITION   : return "Ignition";
            case TRIP_ON_START      : return "Start/Stop";
            default                 : return "Unknown";
        }
    }

    // ------------------------------------------------------------------------

    /** TRIP_ON_SPEED only
    *** Minimum speed used for determining in-motion when the device does not
    *** support start/stop events
    **/
    private static final double MIN_SPEED_KPH               = 5.0;

    /** TRIP_ON_SPEED only
    *** Default mimimum stopped elapsed time to be considered stopped
    **/
    private static final long   MIN_STOPPED_TIME_SEC        = DateTime.MinuteSeconds(5);

    /**
    *** Default to delimit stop with ignition off (if this occurs before the minimum stopped time)
    **/
    private static final boolean STOP_ON_IGNITION_OFF       = false;
    
    // ------------------------------------------------------------------------

    // During TRIP_ON_SPEED trip delimiters, set this value to 'true' to reset the
    // elapsed stop time accumulation to start at the point of the defined 'stop'
    // which is after the minimum elapsed stopped time has passed.  This does cause
    // some user confustion, so if the above is unclear, leave this value 'false'.
    private static final boolean SPEED_RESET_STOP_TIME      = false;

    // ------------------------------------------------------------------------

    private static final int    STATE_UNKNOWN               = 0;
    private static final int    STATE_START                 = 1;
    private static final int    STATE_STOP                  = 2;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private int                         deviceCount         = 0;

    private int                         tripStartType       = TRIP_ON_SPEED;
    private boolean                     tripTypeDefault     = true;

    private int                         ignitionCodes[]     = null;
    private boolean                     isIgnitionOn        = false;
    private EventData                   lastIgnitionEvent   = null;
    
    private EventData                   idleStartEvent      = null;
    private EventData                   idleStopEvent       = null;
    private long                        idleAccumulator     = 0L;       // seconds

    private boolean                     isInMotion          = false;
    private EventData                   lastMotionEvent     = null;
    private EventData                   pendingStopEvent    = null;                 // TRIP_ON_SPEED only

    private double                      minSpeedKPH         = MIN_SPEED_KPH;        // TRIP_ON_SPEED only
    private long                        minStoppedTimeSec   = MIN_STOPPED_TIME_SEC; // TRIP_ON_SPEED only
    private boolean                     stopOnIgnitionOff   = STOP_ON_IGNITION_OFF;

    private int                         lastStateChange     = STATE_UNKNOWN;

    private long                        lastStartTime       = 0L;
    private GeoPoint                    lastStartPoint      = null;
    private String                      lastStartAddress    = "";
    private double                      lastStartOdometer   = 0.0;
    private double                      lastStartFuelUsed   = 0.0;

    private long                        lastStopTime        = 0L;
    private GeoPoint                    lastStopPoint       = null;
    private String                      lastStopAddress     = "";
    private double                      lastStopOdometer    = 0.0;
    private double                      lastStopFuelUsed    = 0.0;

    private java.util.List<FieldData>   deviceDetailData    = null;
    private java.util.List<FieldData>   deviceTotalData     = null;
    private java.util.List<FieldData>   fleetTotalData      = null;

    private double                      totalOdomKM         = 0.0;
    private long                        totalDriveSec       = 0L;
    private double                      totalDriveFuel      = 0.0;
    private int                         totalStopCount      = 0;
    private long                        totalStopSec        = 0L;
    private long                        totalIdleSec        = 0L;
    private double                      totalIdleFuel       = 0.0;

    // ------------------------------------------------------------------------

    /**
    *** Motion Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public MotionReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);

        /* Account check */
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }

        /* Device check */
        this.deviceCount = this.getDeviceCount();
        if (this.deviceCount < 1) {
            throw new ReportException("At least 1 Device must be specified");
        }
        // Detail report if device count == 1
        // Summary report is device count > 1

    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {

        /* TRIP_ON_SPEED vars */
        this.minSpeedKPH       = this.getProperties().getDouble( PROP_minimumSpeedKPH   , MIN_SPEED_KPH);
        this.minStoppedTimeSec = this.getProperties().getLong(   PROP_minimumStoppedTime, MIN_STOPPED_TIME_SEC);
        this.stopOnIgnitionOff = this.getProperties().getBoolean(PROP_stopOnIgnitionOff , STOP_ON_IGNITION_OFF);

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Override 'getEventData' to reset selected status codes
    *** @param device       The Device for which EventData records will be selected
    *** @param rcdHandler   The DBRecordHandler
    *** @return An array of EventData records for the device
    **/
    protected EventData[] getEventData(Device device, DBRecordHandler<EventData> rcdHandler)
    {

        /* Device */
        if (device == null) {
            return EventData.EMPTY_ARRAY;
        }

        /* adjust report constraints */
        ReportConstraints rc = this.getReportConstraints();
        if (this.tripStartType == TRIP_ON_START) {
            // return only start/stop events
            if (this.ignitionCodes != null) {
                rc.setStatusCodes(new int[] {
                    StatusCodes.STATUS_MOTION_START,
                    StatusCodes.STATUS_MOTION_STOP,
                    this.ignitionCodes[0],              // ignition OFF
                    this.ignitionCodes[1]               // ignition ON
                });
            } else {
                rc.setStatusCodes(new int[] {
                    StatusCodes.STATUS_MOTION_START,
                    StatusCodes.STATUS_MOTION_STOP
                });
            }
            rc.setValidGPSRequired(false); // don't need just valid gps events
        } else
        if (this.tripStartType == TRIP_ON_IGNITION) {
            // return only IgnitionOn/IgnitionOff events (this.ignitionCodes is non-null)
            rc.setStatusCodes(new int[] {
                this.ignitionCodes[0],                  // ignition OFF
                this.ignitionCodes[1]                   // ignition ON
            });
            rc.setValidGPSRequired(false); // don't need just valid gps events
        } else {
            // TRIP_ON_SPEED
            // return all status codes
            rc.setStatusCodes(null);
            if (this.ignitionCodes != null) {
                // read all events to make sure we get ignition events
                rc.setValidGPSRequired(false);
            } else {
                // no ignition code, we only need GPS events
                rc.setValidGPSRequired(true);
            }
            long rptLimit = rc.getReportLimit();
            if (rptLimit > 0L) {
                rc.setSelectionLimit(Math.max(rc.getSelectionLimit(), (rptLimit * 4L)));
            }
        }

        /* get data */
        return super.getEventData(device, rcdHandler);

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report supports displaying a map
    *** @return True if this report supports displaying a map, false otherwise
    **/
    public boolean getSupportsMapDisplay()
    {
        return true; // this.hasReportColumn(FieldLayout.DATA_STOP_GEOPOINT);
    }

    /**
    *** Returns true if this report supports displaying KML
    *** @return True if this report supports displaying KML, false otherwise
    **/
    public boolean getSupportsKmlDisplay()
    {
        return this.hasReportColumn(FieldLayout.DATA_STOP_GEOPOINT);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return FieldLayout.getReportLayout();
    }

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {

        
        /* total totals */
        double totalTotalOdomKM    = 0.0;
        long   totalTotalDriveSec  = 0L;
        double totalTotalDriveFuel = 0.0;
        int    totalTotalStopCount = 0;
        long   totalTotalStopSec   = 0L;
        long   totalTotalIdleSec   = 0L;
        double totalTotalIdleFuel  = 0.0;

        /* device total data list */
        this.deviceTotalData = new Vector<FieldData>();

        /* device list */
        Account account   = this.getAccount();
        String  accountID = account.getAccountID();
        ReportDeviceList devList = this.getReportDeviceList();

        /* loop through devices */
        for (Iterator i = devList.iterator(); i.hasNext();) {
            String devID = (String)i.next();
            
            /* init detail data iterator */
            this.deviceDetailData   = new Vector<FieldData>();

            /* reset device totals */
            this.totalOdomKM         = 0.0;
            this.totalDriveSec       = 0L ;
            this.totalDriveFuel      = 0.0;
            this.totalStopCount      = 0;
            this.totalStopSec        = 0L ;
            this.totalIdleSec        = 0L ;
            this.totalIdleFuel       = 0.0;

            // reset ignition state
            this.isIgnitionOn        = false;
            this.lastIgnitionEvent   = null;
            this.ignitionCodes       = null;
            // reset idle state
            this.idleStartEvent      = null;
            this.idleStopEvent       = null;
            this.idleAccumulator     = 0L;
            // reset motion
            this.isInMotion          = false;
            this.lastMotionEvent     = null;
            // reset start
            this.lastStartTime       = 0L;
            this.lastStartPoint      = null;
            this.lastStartAddress    = "";
            this.lastStartOdometer   = 0.0;
            this.lastStartFuelUsed   = 0.0;
            // reset stop
            this.lastStopTime        = 0L;
            this.lastStopPoint       = null;
            this.lastStopAddress     = "";
            this.lastStopOdometer    = 0.0;
            this.lastStopFuelUsed    = 0.0;
            // reset state
            this.lastStateChange     = STATE_UNKNOWN;

            try {

                /* get device */
                Device device = devList.getDevice(devID);
                if (device == null) {
                    continue;
                }

                // Device ignition statusCodes
                this.ignitionCodes = device.getIgnitionStatusCodes();
                boolean hasIgnition = (this.ignitionCodes != null);

                // trip start/stop type
                String tt = this.getProperties().getString(PROP_tripStartType,MOTION_SPEED[0]).toLowerCase();
                //Print.logInfo("Trip type: " + tt);
                if (ListTools.contains(MOTION_DEFAULT,tt)) {
                    // "default"
                    String devCode = device.getDeviceCode();
                    DCServerConfig dcs = DCServerFactory.getServerConfig(devCode);
                    if ((dcs == null) && StringTools.isBlank(devCode) && Account.IsDemoAccount(accountID)) {
                        // special case for "demo" account when 'deviceCode' is blank
                        dcs = DCServerFactory.getServerConfig(DCServerFactory.OPENDMTP_NAME);
                        if (dcs == null) {
                            Print.logWarn("Account 'demo' DCServerConfig not found: " + DCServerFactory.OPENDMTP_NAME);
                        }
                    }
                    if (dcs != null) {
                        // DCServerConfig found
                        if (dcs.getStartStopSupported(false)) {
                            // Device supports start/stop
                            this.tripStartType = TRIP_ON_START;
                        } else
                        if (hasIgnition) {
                            // Device supports ignition state
                            this.tripStartType = TRIP_ON_IGNITION;
                        } else {
                            // Default to speed
                            this.tripStartType = TRIP_ON_SPEED;
                        }
                    } else {
                        // DCServerConfig not found ('deviceCode' is either blank or invalid)
                        if (hasIgnition) {
                            // Device supports ignition state
                            this.tripStartType = TRIP_ON_IGNITION;
                        } else {
                            // Default
                            this.tripStartType = TRIP_ON_SPEED;
                        }
                    }
                    this.tripTypeDefault = true;
                } else
                if (ListTools.contains(MOTION_STARTSTOP,tt)) {
                    // "startstop"
                    this.tripStartType = TRIP_ON_START;
                    this.tripTypeDefault = false;
                } else
                if (ListTools.contains(MOTION_IGNITION,tt)/* && hasIgnition */) {
                    // "ignition"
                    this.tripStartType   = TRIP_ON_IGNITION;
                    this.tripTypeDefault = false;
                    if (!hasIgnition) {
                        this.ignitionCodes = new int[] { StatusCodes.STATUS_IGNITION_OFF, StatusCodes.STATUS_IGNITION_ON };
                        hasIgnition = true;
                    }
                } else {
                    // "speed", "motion"
                    this.tripStartType   = TRIP_ON_SPEED;
                    this.tripTypeDefault = true;
                }
                
                /* debug */
                Print.logInfo("Trip Start Type: [" + this.tripStartType + "] " + TripTypeName(this.tripStartType));
                if (hasIgnition) {
                    String ignOff = StatusCodes.GetHex(this.ignitionCodes[0]);
                    String ignOn  = StatusCodes.GetHex(this.ignitionCodes[1]);
                    Print.logInfo("Device Ignition Codes "+ignOff+":"+ignOn+" [" + accountID + "/" + devID + "]");
                } else {
                    Print.logInfo("No defined Device ignition codes [" + accountID + "/" + devID + "]");
                }

                // get events
                this.getEventData(device, this); // <== callback to 'handleDBRecord'

                // handle final record here
                if (this.lastStopTime > 0) {
                    // we are stopped
                    long   driveTime = (this.lastStartTime > 0L)? (this.lastStopTime     - this.lastStartTime    ) : -1L;
                    double driveDist = (this.lastStartTime > 0L)? (this.lastStopOdometer - this.lastStartOdometer) : -1.0; // kilometers
                    double driveFuel = (this.lastStartTime > 0L)? (this.lastStopFuelUsed - this.lastStartFuelUsed) : -1.0; // liter
                    double driveEcon = (driveFuel > 0.0)? (driveDist / driveFuel) : 0.0; // kilometers per liter
                    long   stopElaps = -1L;
                    long   idleElaps = (this.idleAccumulator > 0L)? this.idleAccumulator : -1L;
                    double idleFuel  = -1.0;
                    this._addRecord(accountID, devID, 
                        this.lastStartTime, this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartFuelUsed,
                        this.lastStopTime , this.lastStopPoint , this.lastStopAddress , this.lastStopOdometer , this.lastStopFuelUsed ,
                        driveTime, driveDist, driveFuel, driveEcon,
                        stopElaps, idleElaps, idleFuel);
                } else
                if (this.lastStartTime > 0) {
                    // we haven't stopped during the range of this report
                    long   driveTime = -1L;
                    double driveDist = -1.0; // kilometers
                    double driveFuel = -1.0; // liters
                    double driveEcon = -1.0; // kilometers per liter
                    long   stopElaps = -1L;
                    long   idleElaps = -1L;
                    double idleFuel  = -1.0;
                    this._addRecord(accountID, devID, 
                        this.lastStartTime, this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartFuelUsed,
                        -1L               , null               , ""                   , -1.0                  , -1.0                  ,
                        driveTime, driveDist, driveFuel, driveEcon,
                        stopElaps, idleElaps, idleFuel);
                }

                /* total record */
                FieldData fd = new FieldData();
                fd.setRowType(DBDataRow.RowType.TOTAL);
                double driveEcon = (this.totalDriveFuel > 0.0)? (this.totalOdomKM / this.totalDriveFuel) : 0.0;
                fd.setAccount(account);
                fd.setDevice(device);
                fd.setString(FieldLayout.DATA_ACCOUNT_ID      , this.getAccountID());
                fd.setString(FieldLayout.DATA_DEVICE_ID       , devID);
                fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA  , this.totalOdomKM);
                fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED , this.totalDriveSec);
                fd.setDouble(FieldLayout.DATA_FUEL_TRIP       , this.totalDriveFuel);
                fd.setDouble(FieldLayout.DATA_FUEL_ECONOMY    , driveEcon);
                fd.setLong(  FieldLayout.DATA_STOP_COUNT      , this.totalStopCount);
                fd.setLong(  FieldLayout.DATA_STOP_ELAPSED    , this.totalStopSec);
                fd.setLong(  FieldLayout.DATA_IDLE_ELAPSED    , this.totalIdleSec);
                fd.setDouble(FieldLayout.DATA_FUEL_IDLE       , this.totalIdleFuel);
                this.deviceTotalData.add(fd);

                /* total totals */
                totalTotalOdomKM    += this.totalOdomKM;
                totalTotalDriveSec  += this.totalDriveSec;
                totalTotalDriveFuel += this.totalDriveFuel;
                totalTotalStopCount += this.totalStopCount;
                totalTotalStopSec   += this.totalStopSec;
                totalTotalIdleSec   += this.totalIdleSec;
                totalTotalIdleFuel  += this.totalIdleFuel;

            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData for Device: " + devID);
            }

        } // Device list iterator

        /* return row iterator */
        if (this.deviceCount > 1) {
            // prepare fleet-total date
            double avgEcon = (totalTotalDriveFuel > 0.0)? (totalTotalOdomKM / totalTotalDriveFuel) : 0.0;
            FieldData fd = new FieldData();
            fd.setRowType(DBDataRow.RowType.TOTAL);
            fd.setAccount(account);
            fd.setString(FieldLayout.DATA_ACCOUNT_ID      , this.getAccountID());
            fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA  , totalTotalOdomKM);
            fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED , totalTotalDriveSec);
            fd.setDouble(FieldLayout.DATA_FUEL_TRIP       , totalTotalDriveFuel);
          //fd.setDouble(FieldLayout.DATA_FUEL_ECONOMY    , avgEcon);
            fd.setLong(  FieldLayout.DATA_STOP_COUNT      , totalTotalStopCount);
            fd.setLong(  FieldLayout.DATA_STOP_ELAPSED    , totalTotalStopSec);
            fd.setLong(  FieldLayout.DATA_IDLE_ELAPSED    , totalTotalIdleSec);
            fd.setDouble(FieldLayout.DATA_FUEL_IDLE       , totalTotalIdleFuel);
            this.fleetTotalData = new Vector<FieldData>();
            this.fleetTotalData.add(fd);
            // return device-total data
            return new ListDataIterator(this.deviceTotalData);
        } else {
            // return device-detail data
            return new ListDataIterator(this.deviceDetailData);
        }
        
    }

    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        if (this.deviceCount > 1) {
            if (this.fleetTotalData != null) {
                return new ListDataIterator(this.fleetTotalData);
            } else {
                return null;
            }
        } else {
            return new ListDataIterator(this.deviceTotalData);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Adds a record to the body database iterator
    *** @param startTime    The motion start time
    *** @param startGP      The motion start GeoPoint
    *** @param startAddress The motion start Address
    *** @param startOdom    The motion start Odometer
    *** @param startFuel    The motion start Fuel Usage
    *** @param stopTime     The motion stop time
    *** @param stopGP       The motion stop GeoPoint
    *** @param stopAddress  The motion stop Address
    *** @param stopOdom     The motion stop Odometer
    *** @param stopFuel     The motion stop Fuel Usage
    *** @param driveTime    The driving elapsed time
    *** @param driveDist    The distance driven
    *** @param driveFuel    The fuel used
    *** @param driveEcon    The fuel economy
    *** @param stopElapse   The elapsed stop time
    *** @param idleElapse   The elapsed idle time (ignition-on, not moving)
    *** @param idleFuel     The fuel used while idling (<='0.0' if unavailable)
    **/
    private void _addRecord(String acctID, String devID, 
        long startTime , GeoPoint startGP, String startAddress, double startOdom, double startFuel,
        long stopTime  , GeoPoint stopGP , String stopAddress , double stopOdom , double stopFuel ,
        long driveTime , double driveDist, double driveFuel   , double driveEcon,
        long stopElapse, long idleElapse , double idleFuel
        )
    {
        FieldData fd = new MotionFieldData();
        fd.setString(  FieldLayout.DATA_ACCOUNT_ID      , acctID);
        fd.setString(  FieldLayout.DATA_DEVICE_ID       , devID);
        fd.setGeoPoint(FieldLayout.DATA_GEOPOINT        , startGP);         // may be null
        fd.setString(  FieldLayout.DATA_ADDRESS         , startAddress);    // may be null/blank
        fd.setLong(    FieldLayout.DATA_START_TIMESTAMP , startTime);       // may be 0L
        fd.setLong(    FieldLayout.DATA_DRIVING_ELAPSED , driveTime);
        fd.setDouble(  FieldLayout.DATA_ODOMETER        , startOdom);
        fd.setDouble(  FieldLayout.DATA_ODOMETER_DELTA  , driveDist);
        fd.setLong(    FieldLayout.DATA_STOP_TIMESTAMP  , stopTime);
        fd.setGeoPoint(FieldLayout.DATA_STOP_GEOPOINT   , stopGP);          // may be null
        fd.setString(  FieldLayout.DATA_STOP_ADDRESS    , stopAddress);     // may be null/blank
        fd.setDouble(  FieldLayout.DATA_STOP_ODOMETER   , stopOdom);
        fd.setDouble(  FieldLayout.DATA_FUEL_TOTAL      , startFuel);
        fd.setDouble(  FieldLayout.DATA_FUEL_TRIP       , driveFuel);       // stopFuel - startFuel
        fd.setDouble(  FieldLayout.DATA_FUEL_ECONOMY    , driveEcon);       // driveDist / driveFuel
        fd.setLong(    FieldLayout.DATA_STOP_ELAPSED    , stopElapse);
        fd.setLong(    FieldLayout.DATA_IDLE_ELAPSED    , idleElapse);
        fd.setDouble(  FieldLayout.DATA_FUEL_IDLE       , idleFuel);
        this.deviceDetailData.add(fd);
        if (driveTime  >  0L) { this.totalDriveSec  += driveTime  ; }
        if (driveDist  > 0.0) { this.totalOdomKM    += driveDist  ; }
        if (driveFuel  > 0.0) { this.totalDriveFuel += driveFuel  ; }
        if (stopTime   >  0L) { this.totalStopCount += 1          ; }
        if (stopElapse >  0L) { this.totalStopSec   += stopElapse ; }
        if (idleElapse >  0L) { this.totalIdleSec   += idleElapse ; }
        if (idleFuel   > 0.0) { this.totalIdleFuel  += idleFuel   ; }
    }

    /**
    *** Custom DBRecord callback handler class
    *** @param rcd  The EventData record
    *** @return The returned status indicating whether to continue, or stop
    **/
    public int handleDBRecord(EventData rcd)
        throws DBException
    {
        EventData evRcd = rcd;
        Device device   = evRcd.getDevice();
        int statusCode  = evRcd.getStatusCode();
        //Print.logInfo("EventData: " + evRcd.getTimestamp() + " 0x" + StringTools.toHexString(evRcd.getStatusCode(),16));

        /* ignition state change */
        boolean ignitionChange = false;
        if (this.tripStartType != TRIP_ON_IGNITION) {
            if (this.ignitionCodes != null) {
                // has ignition codes
                if (this.isIgnitionOff(statusCode)) {
                    // ignition OFF
                    if ((this.lastIgnitionEvent == null) || this.isIgnitionOn) {
                        ignitionChange         = true;
                        this.isIgnitionOn      = false;
                      //this.lastIgnOffEvent   = evRcd;
                        this.lastIgnitionEvent = evRcd;
                    } else {
                        // ignition is already off
                    }
                } else
                if (this.isIgnitionOn(statusCode)) {
                    // ignition ON
                    if ((this.lastIgnitionEvent == null) || !this.isIgnitionOn) {
                        ignitionChange         = true;
                        this.isIgnitionOn      = true;
                      //this.lastIgnOnEvent    = evRcd;
                        this.lastIgnitionEvent = evRcd;
                    } else {
                        // ignition is already on
                    }
                } else {
                    // leave ignition state as-is
                }
            } else {
                // no ignition codes
            }
        }

        /* trip delimiter */
        boolean isMotionStart = false;
        boolean isMotionStop  = false;
        boolean isIdleStart   = false;
        boolean isIdleStop    = false;
        if (this.tripStartType == TRIP_ON_IGNITION) {
            // TRIP_ON_IGNITION
            if (this.isIgnitionOn(statusCode)) {
                // I've started moving
                if ((this.lastIgnitionEvent == null) || !this.isIgnitionOn) {
                    // ignition state changed to on
                    ignitionChange              = true;
                    this.isIgnitionOn           = true;
                  //this.lastIgnOnEvent         = evRcd;
                    this.lastIgnitionEvent      = evRcd;
                    isMotionStart               = true;
                    this.isInMotion             = true;
                  //this.lastStartEvent         = evRcd;
                    this.lastMotionEvent        = evRcd;
                  //isIdleStop                  = true; <== no idle for TRIP_ON_IGNITION
                    this.idleStopEvent          = null;
                } else {
                    // ignition is already on
                }
            } else
            if (this.isIgnitionOff(statusCode)) {
                // I've stopped moving
                if ((this.lastIgnitionEvent == null) || this.isIgnitionOn) {
                    ignitionChange              = true;
                    this.isIgnitionOn           = false;
                  //this.lastIgnOffEvent        = evRcd;
                    this.lastIgnitionEvent      = evRcd;
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                  //isIdleStart                 = true;  <== no idle for TRIP_ON_IGNITION
                    this.idleStartEvent         = null;
                } else {
                    // ignition is already off
                }
            } else {
                // not a motion state change event
            }
        } else
        if (this.tripStartType == TRIP_ON_START) {
            // TRIP_ON_START
            if (this.isMotionStart(statusCode)) {
                if (!this.isInMotion) {
                    // I was stopped, I've now started moving (stop idle clock)
                    isMotionStart                   = true;
                    this.isInMotion                 = true;
                  //this.lastStartEvent             = evRcd;
                    this.lastMotionEvent            = evRcd;
                    if (!this.isIgnitionOn) {
                        // force ignition ON when moving
                        this.isIgnitionOn           = true; 
                      //this.lastIgnOnEvent         = evRcd;
                        this.lastIgnitionEvent      = evRcd;
                    }
                    isIdleStop                      = true;     // in TRIP_ON_START
                    this.idleStopEvent              = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm already moving
                }
            } else
            if (this.isMotionStop(statusCode)) {
                if (this.isInMotion) {
                    // I've stopped moving (start idle clock)
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                    if (this.isIgnitionOn && (this.ignitionCodes == null)) {
                        // force ignition off if device does not have ignition codes
                        this.isIgnitionOn       = false;
                      //this.lastIgnOffEvent    = evRcd
                        this.lastIgnitionEvent  = evRcd;
                    }
                    isIdleStart                 = true;     // in TRIP_ON_START
                    this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm already stopped
                }
            } else
            if (this.isIgnitionOff(statusCode) && this.stopOnIgnitionOff) {
                if (this.isInMotion) {
                    // I've stopped moving (start idle clock)
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                    isIdleStart                 = true;     // in TRIP_ON_START
                    this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm already stopped
                }
            } else {
                // not a motion state change event
                // check for idle change events while not moving
                if (!this.isInMotion) {
                    if (this.isIgnitionOn(statusCode)) {
                        // ignition on while not moving, start idle clock
                        isIdleStart             = true;     // in TRIP_ON_START
                        this.idleStartEvent     = (this.ignitionCodes != null)? evRcd : null;
                    } else
                    if (this.isIgnitionOff(statusCode)) {
                        isIdleStop              = true;     // in TRIP_ON_START
                        this.idleStopEvent      = (this.ignitionCodes != null)? evRcd : null;
                    }
                }
            }
        } else
        if (this.tripStartType == TRIP_ON_SPEED) {
            if (evRcd.getSpeedKPH() >= this.minSpeedKPH) {
                // I am moving
                this.pendingStopEvent           = null; // always reset (for min stop time below)
                if (!this.isInMotion) {
                    // I wasn't moving before, now I've started moving
                    isMotionStart               = true;
                    this.isInMotion             = true;
                  //this.lastStartEvent         = evRcd;
                    this.lastMotionEvent        = evRcd; // start of motion
                    if (this.isIgnitionOn) {
                        // ignition is already on.
                        //Print.logInfo("Start of motion (ignition is ON)");
                    } else {
                        // force ignition on (since were now moving)
                        //Print.logInfo("Start of motion (force ignition ON)");
                        this.isIgnitionOn       = true; 
                      //this.lastIgnOnEvent     = evRcd;
                        this.lastIgnitionEvent  = evRcd;
                    }
                    isIdleStop                  = true;     // in TRIP_ON_SPEED
                    this.idleStopEvent          = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm still moving
                    if (ignitionChange) {
                        // ignition on/off while moving?
                    }
                }
            } else {
                // I am not moving
                if (this.isInMotion) {
                    // I was moving, now I've stopped moving - maybe
                    if (this.minStoppedTimeSec <= 0L) {
                        // no minimum stopped-time, and we haven't already stopped
                        //Print.logInfo("Stopped motion (no minimum stopped time)");
                        isMotionStop                = true;
                        this.isInMotion             = false;
                      //this.lastStopEvent          = evRcd;
                        this.lastMotionEvent        = evRcd; // stop motion
                        this.pendingStopEvent       = null;
                        isIdleStart                 = true;     // in TRIP_ON_SPEED
                        this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                    } else
                    if (ignitionChange && !this.isIgnitionOn && this.stopOnIgnitionOff) {
                        // ignition turned off while not moving, and we want to consider this as a stop
                        //Print.logInfo("Stopped motion (forced by ignition OFF)");
                        isMotionStop                = true;
                        this.isInMotion             = false;
                      //this.lastStopEvent          = evRcd;
                        this.lastMotionEvent        = (this.pendingStopEvent != null)? this.pendingStopEvent : evRcd; // stop motion
                        this.pendingStopEvent       = null;
                        isIdleStart                 = true;     // in TRIP_ON_SPEED
                        this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                    } else {
                        // minimum stopped time in effect
                        if (this.pendingStopEvent == null) {
                            // start the stopped-time clock
                            this.pendingStopEvent   = evRcd;
                        } else {
                            // check to see if we've met the minimum stopped time
                            long deltaTimeSec = evRcd.getTimestamp() - this.pendingStopEvent.getTimestamp();
                            if (deltaTimeSec >= this.minStoppedTimeSec) {
                                // elapsed stop time exceeded limit
                                //Print.logInfo("Stopped motion (elapsed minimum stop time)");
                                isMotionStop         = true;
                                this.isInMotion      = false;
                              //this.lastStopEvent   = evRcd;
                                if (SPEED_RESET_STOP_TIME) {
                                    // if we reset the stop event here, then the minimum stopped time will
                                    // not be counted. (this does cause some user confusion, so this reset
                                    // should not occur).
                                    this.lastMotionEvent = evRcd; // stop motion
                                } else {
                                    this.lastMotionEvent = this.pendingStopEvent;
                                }
                                this.pendingStopEvent    = null;
                                isIdleStart              = true;     // in TRIP_ON_SPEED
                                this.idleStartEvent      = (this.ignitionCodes != null)? evRcd : null;
                            } else {
                                // assume I'm still moving (ie. temporarily stopped)
                            }
                        }
                    }
                } else {
                    // I'm still not moving
                    // check for idle change events while not moving
                    if (this.isIgnitionOn(statusCode)) {
                        // ignition on while not moving, start idle clock
                        isIdleStart             = true;     // in TRIP_ON_SPEED
                        this.idleStartEvent     = (this.ignitionCodes != null)? evRcd : null;
                    } else
                    if (this.isIgnitionOff(statusCode)) {
                        isIdleStop              = true;     // in TRIP_ON_SPEED
                        this.idleStopEvent      = (this.ignitionCodes != null)? evRcd : null;
                    }
                }
            }
        }
        // isMotionStart            - true if motion changed from stop==>start
        // isMotionStop             - true if motion changed from start==>stop
        // this.isInMotion          - current motion state
        // this.lastMotionEvent     - last motion delimiter event
        // this.lastIgnitionEvent   - last ignition delimiter event
        // isIdleStart              - true if idle changed from stop==>start
        // isIdleStop               - true if idle changed from start==>stop
        // this.idleStartEvent      - last idle start event
        // this.idleStopEvent       - last idle stop event
        // ignitionChange           - true if ignition changed state
        // this.isIgnitionOn        - current ignition state

        /* accrue idle time */
        if (this.ignitionCodes != null) {
            // 'idle' only valid if we have ignition codes
            if (isIdleStart) {
                // just wait for 'stop'
            } else
            if (isIdleStop) {
                // 'this.idleStopEvent' is non-null
                if (this.idleStartEvent != null) {
                    this.idleAccumulator += (this.idleStopEvent.getTimestamp() - this.idleStartEvent.getTimestamp());
                } else {
                    // 'this.idleStartEvent' not yet initialized (likely first occurance in report)
                }
                //Print.logInfo("Accumulated Idle time: " + this.idleAccumulator);
                this.idleStartEvent = null;
                this.idleStopEvent  = null;
            }
        }

        // lastStart -> lastStop -> start
        if (isMotionStart) {
            EventData ev = this.lastMotionEvent; // start of motion
            // 'this.isIgnitionOn' is 'true'

            if (this.lastStateChange == STATE_START) {
                // abnormal start ==> start
                // we already have a 'start', we're missing an interleaving  'stop'
                // the driving-time is not valid
                // ('this.lastStopTime' will already be '0' here, since we didn't get an interleaving 'stop')
                // ('this.lastStartTime' will be > 0 here, since we did get a previous 'start')
                // We treat this START event as a STOP event
                long     stopTime  = ev.getTimestamp();
                GeoPoint stopPoint = ev.getGeoPoint();
                String   stopAddr  = ev.getAddress();
                double   stopOdom  = ev.getOdometerKM();
                if (stopOdom <= 0.0) { stopOdom = ev.getDistanceKM(); }
                double   stopFuel  = ev.getFieldValue(EventData.FLD_fuelTotal, 0.0);
                long     driveTime = (this.lastStartTime > 0L)? (stopTime  - this.lastStartTime)     : 0L;
                double   driveDist = (this.lastStartTime > 0L)? (stopOdom  - this.lastStartOdometer) : 0.0; // kilometers
                double   driveFuel = (this.lastStartTime > 0L)? (stopFuel  - this.lastStartFuelUsed) : 0.0; // liters
                double   driveEcon = (driveFuel > 0.0)? (driveDist / driveFuel) : -1.0; // kilometers per liter
                long     stopElaps = 0L;
                long     idleElaps = 0L;
                double   idleFuel  = -1.0;
                this._addRecord(ev.getAccountID(), ev.getDeviceID(), 
                    this.lastStartTime, this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartFuelUsed,
                    stopTime          , stopPoint          , stopAddr             , stopOdom              , stopFuel              ,
                    driveTime, driveDist, driveFuel, driveEcon,
                    stopElaps, idleElaps, idleFuel);
                // continue with 'START'
            } else
            if (this.lastStopTime > 0) {
                // normal start --> stop ==> start
                long     driveTime = (this.lastStartTime > 0L)? (this.lastStopTime     - this.lastStartTime)     : 0L;
                double   driveDist = (this.lastStartTime > 0L)? (this.lastStopOdometer - this.lastStartOdometer) : -1.0; // kilometers
                double   driveFuel = (this.lastStartTime > 0L)? (this.lastStopFuelUsed - this.lastStartFuelUsed) : -1.0; // liters
                double   driveEcon = (driveFuel > 0.0)? (driveDist / driveFuel) : 0.0; // kilometers per liter
                long     stopElaps = ev.getTimestamp() - this.lastStopTime;
                long     idleElaps = (this.idleAccumulator > 0L)? this.idleAccumulator : -1L;
                double   idleFuel  = -1.0;
                this._addRecord(ev.getAccountID(), ev.getDeviceID(), 
                    this.lastStartTime, this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartFuelUsed,
                    this.lastStopTime , this.lastStopPoint , this.lastStopAddress , this.lastStopOdometer , this.lastStopFuelUsed ,
                    driveTime, driveDist, driveFuel, driveEcon,
                    stopElaps, idleElaps, idleFuel);
            }

            this.lastStartTime      = ev.getTimestamp();
            this.lastStartPoint     = ev.getGeoPoint();
            this.lastStartAddress   = ev.getAddress();
            this.lastStartOdometer  = ev.getOdometerKM();
            if (this.lastStartOdometer <= 0.0) { this.lastStartOdometer = ev.getDistanceKM(); }
            this.lastStartFuelUsed  = ev.getFieldValue(EventData.FLD_fuelTotal, 0.0);

            this.lastStopTime       = 0L;
            this.lastStopPoint      = null;
            this.lastStopAddress    = null;
            this.lastStopOdometer   = 0.0;
            this.lastStopFuelUsed   = 0.0;
            this.lastStateChange    = STATE_START;

            /* clear idle accrual */
            this.idleAccumulator    = 0L;

        } else
        if (isMotionStop) {
            EventData ev = this.lastMotionEvent; // stop motion

            if (this.lastStateChange == STATE_STOP) {
                // abnormal start --> stop ==> stop
                // we already have a 'stop', we're missing a 'start'.
                // this condition can only occur for TRIP_ON_START or TRIP_ON_IGNITION
                if ((this.lastStopTime > 0) && (this.lastIgnitionEvent != null) && (this.lastIgnitionEvent.getTimestamp() > this.lastStopTime)) {
                    // inject a START at the last ignition event (no additional idle accural calculations)
                    long     startTime  = this.lastIgnitionEvent.getTimestamp();
                    GeoPoint startPoint = this.lastIgnitionEvent.getGeoPoint();
                    String   startAddr  = this.lastIgnitionEvent.getAddress();
                    double   startOdom  = this.lastIgnitionEvent.getOdometerKM();
                    if (startOdom <= 0.0) { startOdom = this.lastIgnitionEvent.getDistanceKM(); }
                    double   startFuel  = this.lastIgnitionEvent.getFieldValue(EventData.FLD_fuelTotal, 0.0);
                    long     driveTime  = this.lastStopTime     - startTime;
                    double   driveDist  = this.lastStopOdometer - startOdom; // kilometers
                    double   driveFuel  = this.lastStopFuelUsed - startFuel; // liters
                    double   driveEcon  = (driveFuel > 0.0)? (driveDist / driveFuel) : 0.0; // kilometers per liter
                    long     stopElaps  = this.lastIgnitionEvent.getTimestamp() - this.lastStopTime;
                    long     idleElaps  = (this.idleAccumulator > 0L)? this.idleAccumulator : -1L;
                    double   idleFuel   = -1.0;
                    this._addRecord(ev.getAccountID(), ev.getDeviceID(), 
                        startTime         , startPoint         , startAddr            , startOdom             , startFuel             ,
                        this.lastStopTime , this.lastStopPoint , this.lastStopAddress , this.lastStopOdometer , this.lastStopFuelUsed ,
                        driveTime, driveDist, driveFuel, driveEcon,
                        stopElaps, idleElaps, idleFuel);
                    this.isIgnitionOn = true; // force to true, since we simulated a 'START'
                    // 'this.lastIgnitionEvent' stays as-is
                    // Continue with STOP
                } else {
                    // no interleaving ignition events
                    // ignore the previous 'STOP'
                }
            }

            this.lastStopTime       = ev.getTimestamp();
            this.lastStopPoint      = ev.getGeoPoint();
            this.lastStopAddress    = ev.getAddress();
            this.lastStopOdometer   = ev.getOdometerKM();
            if (this.lastStopOdometer <= 0.0) { this.lastStopOdometer = ev.getDistanceKM(); }
            this.lastStopFuelUsed   = ev.getFieldValue(EventData.FLD_fuelTotal, 0.0);
            this.lastStateChange    = STATE_STOP;

            /* start idle accrual */
            this.idleAccumulator    = 0L;

        }

        /* return record limit status */
        return (this.deviceDetailData.size() < this.getReportLimit())? DBRH_SKIP : DBRH_STOP;
        
    }

    // ------------------------------------------------------------------------

    private boolean isIgnitionOn(int statusCode)
    {
        if (this.ignitionCodes != null) {
            return (statusCode == this.ignitionCodes[1]);
        } else {
            return false;
        }
    }
    
    private boolean isIgnitionOff(int statusCode)
    {
        if (this.ignitionCodes != null) {
            return (statusCode == this.ignitionCodes[0]);
        } else {
            return false;
        }
    }
    
    private boolean isMotionStart(int statusCode)
    {
        return (statusCode == StatusCodes.STATUS_MOTION_START);
    }

    private boolean isMotionStop(int statusCode)
    {
        return (statusCode == StatusCodes.STATUS_MOTION_STOP);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom MotionFieldData class
    **/
    private static class MotionFieldData
        extends FieldData
        implements EventDataProvider
    {
        // Available fields:
        //   FieldLayout.DATA_ACCOUNT_ID       
        //   FieldLayout.DATA_DEVICE_ID        
        //   FieldLayout.DATA_GEOPOINT        
        //   FieldLayout.DATA_ADDRESS         
        //   FieldLayout.DATA_START_TIMESTAMP 
        //   FieldLayout.DATA_DRIVING_ELAPSED 
        //   FieldLayout.DATA_ODOMETER        
        //   FieldLayout.DATA_ODOMETER_DELTA  
        //   FieldLayout.DATA_STOP_TIMESTAMP  
        //   FieldLayout.DATA_STOP_GEOPOINT   
        //   FieldLayout.DATA_STOP_ADDRESS    
        //   FieldLayout.DATA_STOP_ODOMETER   
        //   FieldLayout.DATA_FUEL_TOTAL      
        //   FieldLayout.DATA_FUEL_TRIP       
        //   FieldLayout.DATA_FUEL_ECONOMY    
        //   FieldLayout.DATA_IDLE_ELAPSED    
        public MotionFieldData() {
            super();
        }
        public String getAccountID() {
            return super.getString(FieldLayout.DATA_ACCOUNT_ID,"");
        }
        public String getDeviceID() {
            return super.getDeviceID();
        }
        public String getDeviceDescription() {
            return super.getDeviceDescription();
        }
        public String getDeviceVIN() {
            return super.getDeviceVIN();
        }
        public long getTimestamp() {
            return super.getLong(FieldLayout.DATA_STOP_TIMESTAMP, 0L);
        }
        public int getStatusCode() {
            return StatusCodes.STATUS_MOTION_STOP;
        }
        public String getStatusCodeDescription(BasicPrivateLabel bpl) {
            return "Stop";
        }
        public int getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys, 
            boolean isFleet, BasicPrivateLabel bpl) {
            return EventData.ICON_PUSHPIN_RED;
        }
        public boolean isValidGeoPoint() {
            return GeoPoint.isValid(this.getLatitude(), this.getLongitude());
        }
        public double getLatitude() {
            GeoPoint gp = super.getGeoPoint(FieldLayout.DATA_STOP_GEOPOINT, null);
            return (gp != null)? gp.getLatitude() : 0.0;
        }
        public double getLongitude() {
            GeoPoint gp = super.getGeoPoint(FieldLayout.DATA_STOP_GEOPOINT, null);
            return (gp != null)? gp.getLongitude() : 0.0;
        }
        public GeoPoint getGeoPoint() {
            return new GeoPoint(this.getLatitude(), this.getLongitude());
        }
        public double getHorzAccuracy() {
            return -1.0; // not available
        }
        public GeoPoint getBestGeoPoint() {
            return this.getGeoPoint();
        }
        public double getBestAccuracy() {
            return this.getHorzAccuracy();
        }
        public int getSatelliteCount() {
            return 0;
        }
        public double getBatteryLevel() {
            return 0.0;
        }
        public double getSpeedKPH() {
            return 0.0;
        }
        public double getHeading() {
            return 0.0;
        }
        public double getAltitude() {
            return 0.0;
        }
        public double getOdometerKM() {
            return 0.0;
        }
        public String getGeozoneID() {
            return "";
        }
        public String getAddress() {
            return super.getString(FieldLayout.DATA_STOP_ADDRESS, "");
        }
        public long getInputMask() {
            return 0L;
        }
        public void setEventIndex(int ndx)
        {
            super.setInt(FieldLayout.DATA_EVENT_INDEX,ndx);
        }
        public int getEventIndex()
        {
            return super.getInt(FieldLayout.DATA_EVENT_INDEX,-1);
        }
        public boolean getIsFirstEvent()
        {
            return (this.getEventIndex() == 0);
        }
        public void setIsLastEvent(boolean isLast) {
            super.setBoolean(FieldLayout.DATA_LAST_EVENT,isLast);
        }
        public boolean getIsLastEvent() {
            return super.getBoolean(FieldLayout.DATA_LAST_EVENT,false);
        }
    }

}
