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
// Notes:
//  - "TrackStick" is a licensed trademark of "TrackStick", a division of
//    Telespial Systems.  OpenGTS and GeoTelematic Solutions are not affiliated
//    with either TrackStick or Telespial System in any way.
//  - This module is designed for support of the CSV download format provided by
//    the "TrackStick Mini" (see "http://www.trackstick.com")
//  - Loading the same CSV file more than one may skew the reported estimated
//    odometer value.  This module does attempt to detect such a reload, but it
//    is not guaranteed.  Reloading a CSV file is not recommended.
// ----------------------------------------------------------------------------
// Change History:
//  2011/04/01  Martin D. Flynn
//     -Initial release
//  2011/05/13  Martin D. Flynn
//     -Added option to change "displayed" timezone
//     -Reverse-Geocoding is turned off by default (tends to overload the active
//      reverse-geocoder).
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public class TrackStick
{

    // ------------------------------------------------------------------------

    private static       boolean DEBUG_MODE                 = true;
    private static       boolean PrintAllEvents             = false;
    
    private static final String  COMMENT_TIMEZONE           = "TimeZone";
    private static final String  COMMENT_STATUSCODE         = "StatusCode";

    // ------------------------------------------------------------------------
    // trackStick.defaultTimeZone=US/Pacific
    // trackStick.minimumSpeedKPH=0.0
    // trackStick.minimumHeadingChange=15.0
    // trackStick.minimumDormantSeconds=1800
    // trackStick.minimumMovingSeconds=120
    // trackStick.estimateOdometer=true

    public  static final String  PROP_defaultTimeZone       = "trackStick.defaultTimeZone";
    public  static final String  PROP_minimumSpeedKPH       = "trackStick.minimumSpeedKPH";
    public  static final String  PROP_minimumHeadingChange  = "trackStick.minimumHeadingChange";
    public  static final String  PROP_minimumDormantSeconds = "trackStick.minimumDormantSeconds";
    public  static final String  PROP_minimumMovingSeconds  = "trackStick.minimumMovingSeconds";
    public  static final String  PROP_estimateOdometer      = "trackStick.estimateOdometer";
    public  static final String  PROP_addIgnitionState      = "trackStick.addIgnitionState";
    public  static final String  PROP_preClearEvents        = "trackStick.preClearEvents";
    public  static final String  PROP_reverseGeocode        = "trackStick.reverseGeocode";

    private static       String  DFT_TIMEZONE               = "GMT";
    private static       double  MIN_SPEEDKPH               = 0.0;
    private static       double  MIN_HEADING_CHANGE         = 15.0;
    private static       long    MIN_DORMANT_SEC            = DateTime.MinuteSeconds(30);
    private static       long    MIN_MOVING_SEC             = DateTime.MinuteSeconds( 2);
    private static       boolean ESTIMATE_ODOMETER          = true;
    private static       boolean ADD_IGNITION_STATE         = false;
    private static       long    MIN_IGNITION_STOP_TIME     = DateTime.MinuteSeconds(6);
    private static       boolean PRE_CLEAR_EVENTS           = false;
    private static       boolean PRE_CLEAR_ONLY             = false;
    private static       boolean REVERSE_GEOCODE            = false;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static double temp_C2F(double C) { return (C * 9.0/5.0) + 32.0; }
    private static double temp_F2C(double F) { return (F - 32.0) * 5.0/9.0; }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Account  account            = null;
    private Device   device             = null;
    
    private TimeZone displayTimeZone    = null;
    
    private int      eventTotalCount    = 0;
    
    private String   dftTimeZone        = DFT_TIMEZONE;
    private double   minSpeedKPH        = MIN_SPEEDKPH;
    private double   minHeadingChange   = MIN_HEADING_CHANGE;
    private long     minDormantSec      = MIN_DORMANT_SEC;
    private long     minMovingSec       = MIN_MOVING_SEC;
    private boolean  estimateOdometer   = ESTIMATE_ODOMETER;
    private boolean  addIgnitionState   = ADD_IGNITION_STATE;
    private boolean  preClearEvents     = PRE_CLEAR_EVENTS;
    private boolean  reverseGeocode     = REVERSE_GEOCODE;

    public TrackStick(Account acct, Device dev)
    {

        /* account/device */
        this.device  = dev;
        this.account = (acct != null)? acct : (dev != null)? dev.getAccount() : null;

        /* default parse properties */
        this.dftTimeZone      = RTConfig.getString( PROP_defaultTimeZone      , DFT_TIMEZONE);
        this.minSpeedKPH      = RTConfig.getDouble( PROP_minimumSpeedKPH      , MIN_SPEEDKPH);
        this.minHeadingChange = RTConfig.getDouble( PROP_minimumHeadingChange , MIN_HEADING_CHANGE);
        this.minDormantSec    = RTConfig.getLong(   PROP_minimumDormantSeconds, MIN_DORMANT_SEC);
        this.minMovingSec     = RTConfig.getLong(   PROP_minimumMovingSeconds , MIN_MOVING_SEC);
        this.estimateOdometer = RTConfig.getBoolean(PROP_estimateOdometer     , ESTIMATE_ODOMETER);
        this.addIgnitionState = RTConfig.getBoolean(PROP_addIgnitionState     , ADD_IGNITION_STATE);
        this.preClearEvents   = RTConfig.getBoolean(PROP_preClearEvents       , PRE_CLEAR_EVENTS);
        this.reverseGeocode   = RTConfig.getBoolean(PROP_reverseGeocode       , REVERSE_GEOCODE);

    }

    // ------------------------------------------------------------------------

    public Account getAccount()
    {
        return this.account;
    }
    
    public String getAccountID()
    {
        return (this.account != null)? this.account.getAccountID() : "";
    }

    // ------------------------------------------------------------------------

    public Device getDevice()
    {
        return this.device;
    }
    
    public String getDeviceID()
    {
        return (this.device != null)? this.device.getDeviceID() : "";
    }

    // ------------------------------------------------------------------------

    public void setDisplayTimeZone(TimeZone dispTmz)
    {
        this.displayTimeZone = dispTmz;
    }
    
    // ------------------------------------------------------------------------

    /** 
    *** Get Default TimeZone
    *** @return The default Timezone
    **/
    private TimeZone getDefaultTimeZone()
    {
        // no timezone, check Account
        TimeZone tmz = Account.getTimeZone(this.account, null);
        if (tmz == null) {
            // still null, try property specified default
            tmz = DateTime.getTimeZone(this.dftTimeZone,null);
            if (tmz == null) {
                // still null, set to GMT
                tmz = DateTime.getGMTTimeZone();
            }
        }
        return tmz;
    }

    // ------------------------------------------------------------------------

    /**
    *** Print Header 
    **/
    public void printHeader()
    {
        Print.logInfo("---------------------------------------------");
        Print.logInfo("Account           : " + this.getAccountID());
        Print.logInfo("Device            : " + this.getDeviceID());
        Print.logInfo("Debug Mode        : " + DEBUG_MODE);
        Print.logInfo("Default Timezone  : " + this.dftTimeZone);
        Print.logInfo("Pre-Clear Events  : " + this.preClearEvents);
        Print.logInfo("Min Speed Km/H    : " + this.minSpeedKPH);
        Print.logInfo("Min Heading Chg   : " + this.minHeadingChange);
        Print.logInfo("Min Moving Sec    : " + this.minMovingSec);
        Print.logInfo("Min Dormant Sec   : " + this.minDormantSec);
        Print.logInfo("Estimate Odometer : " + this.estimateOdometer);
        Print.logInfo("Add Ignition State: " + this.addIgnitionState);
        Print.logInfo("Reverse-Geocoding : " + Device.GetAllowSlowReverseGeocoding());
        Print.logInfo("---------------------------------------------");
        Print.logInfo("");
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Parse Date and Time
    **/
    private static long parseDateTime(String date, String time, TimeZone tmz)
    {

        /* extract time */
        if (StringTools.isBlank(time)) {
            int p = date.indexOf(":");
            if (p >= 2) {
                time = date.substring(p-2).trim();
                date = date.substring(0,p-2).trim();
            }
        }

        /* accumulators */
        int YEAR    = 0;    // 2011
        int MON1    = 0;    // 03
        int DAY     = 0;    // 24
        int HOUR    = 0;    // 17
        int MIN     = 0;    // 34
        int SEC     = 0;    // 27

        /* Date */
        if (StringTools.isBlank(date)) {
            // skip
        } else
        if (date.indexOf("/") >= 0) {
            // "03/24/2011", "03/24/11"
            String d[] = StringTools.split(date,'/');
            if (d.length >= 3) {
                YEAR = StringTools.parseInt(d[2],0);      // YYYY
                if (YEAR < 100) { YEAR += 2000; }
                MON1 = StringTools.parseInt(d[0],0);      // MM
                DAY  = StringTools.parseInt(d[1],0);      // DD
            }
        } else
        if (date.indexOf("-") >= 0) {
            // "24-Mar-11", "24-March-2011"
            String d[] = StringTools.split(date,'-');
            if (d.length >= 3) {
                YEAR = StringTools.parseInt(d[2],0);      // YYYY
                if (YEAR < 100) { YEAR += 2000; }
                MON1 = DateTime.getMonthIndex1(d[1],0);   // MM
                DAY  = StringTools.parseInt(d[0],0);      // DD
            }
        } else
        if (date.indexOf(" ") >= 0) {
            // "Thursday, March 24, 2011"
            String d[] = StringTools.split(date,','); // first split on comma
            if (d.length >= 3) {
                YEAR = StringTools.parseInt(d[2],0);      // YYYY
                if (YEAR < 100) { YEAR += 2000; }
                int m = d[1].indexOf(" ");
                String mmStr = (m >= 0)? d[1].substring(0,m).trim() : "";
                String ddStr = (m >= 0)? d[1].substring(m+1).trim() : "";
                MON1 = DateTime.getMonthIndex1(mmStr,0);  // MM
                DAY  = StringTools.parseInt(ddStr,0);     // DD
            }
        }

        /* Time */
        if ((YEAR > 2000) && !StringTools.isBlank(time)) {
            String t[] = StringTools.split(time,':');
            if (t.length >= 3) {
                // "17:18:00", "05:18:00 pm"
                HOUR = StringTools.parseInt(t[0],0); // hh
                MIN  = StringTools.parseInt(t[1],0); // mm
                SEC  = StringTools.parseInt(t[2],0); // ss
                String apm = t[2].toLowerCase();
                if (apm.endsWith("am")) {
                    // "12:59:59 am" ==> "00:59:59"
                    if (HOUR == 12) {
                        HOUR = 0;
                    }
                } else
                if (apm.endsWith("pm")) {
                    // "12:59:59 pm" ==> "12:59:59"
                    // "11:59:59 pm" ==> "23:59:59"
                    // "01:59:59 pm" ==> "13:59:59"
                    if (HOUR < 12) {
                        HOUR += 12;
                    }
                }
            }
        }
        
        /*convert to timestamp */
        DateTime dt = new DateTime(tmz, YEAR,MON1,DAY, HOUR,MIN,SEC);
        long timestamp = dt.getTimeSec();

        return timestamp;
    }
    
    // ------------------------------------------------------------------------
    
    private static final int STATE_UNDEFINED  = 0;
    private static final int STATE_STOPPED    = 1;
    private static final int STATE_MOVING     = 2;

    /**
    *** Parse date from speified file 
    **/
    public boolean parseFile(File csvFile, TimeZone csvTMZ)
        throws IOException
    {

        /* have account/device? */
        if (this.device == null) {
            Print.logError("No Device specified");
            return false;
        }

        /* CSV File? */
        if (csvFile == null) {
            Print.logError("CSV File not specified");
            System.exit(99);
        } else
        if (!csvFile.isFile()) {
            Print.logError("CSV File does not exist - " + csvFile);
            System.exit(99);
        }
        
        /* get file stream */
        boolean ok = false;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(csvFile);
            ok = this.parseStream(fis, csvTMZ);
        } finally {
            if (fis != null) { try { fis.close(); } catch (Throwable th) {} }
        }
        return ok;

    }

    /**
    *** Parse data from specified stream
    **/
    public boolean parseStream(InputStream csvStream, TimeZone csvTMZ)
        throws IOException
    {

        /* have account/device? */
        if (this.device == null) {
            Print.logError("No Device specified");
            return false;
        }
        String accountID   = this.device.getAccountID();
        String deviceID    = this.device.getDeviceID();
        double startOdomKM = this.device.getLastOdometerKM();
        
        /* account BasicPrivateLabel */
        BasicPrivateLabel privLabel = Account.getPrivateLabel(this.getAccount());

        /* have input stream? */
        if (csvStream == null) {
            Print.logError("No CSV stream specified");
            return false;
        }

        /* check timezone */
        if (csvTMZ == null) {
            csvTMZ = this.getDefaultTimeZone();
        }

        /* read CSV stream */
        //Record,Date      ,Time    ,Latitude,Longitude   ,Altitude,Temp,Status       ,Course,GPS Fix,Signal,MapLink                                                      ,Name                             ,DeviceName
        //1     ,03/24/2011,17:18:00,         ,           ,        ,    ,Power On     ,      ,       ,      ,                                                             ,03/24/2011 17:18:00 Power On
        //1     ,03/24/2011,17:18:00,39.151540,-142.123456,562.7   ,28.4,4 kph        ,337.6 ,Y      ,7     ,http://maps.google.com/maps?q=39.151540+-121.132085&h=en&t=h ,03/24/2011 17:18:00 4 kph N      ,TrackStick
        //3     ,03/24/2011,17:22:00,39.151523,-142.123456,564.5   ,28.9,Stopped 8 min,62    ,Y      ,7     ,http://maps.google.com/maps?q=39.151523+-121.132283&h=en&t=h ,03/24/2011 17:22:00 Stopped 8 min,TrackStick
        //3     ,03/24/2011,17:30:00,         ,           ,        ,    ,Power Off    ,      ,       ,      ,                                                             ,03/24/2011 17:30:00 Power Off
        String   header[]        = null;
        int      lineCount       = 0;
        int      totalCount      = 0;
        int      saveCount       = 0;
        long     lastKeepTime    = 0L;
        double   lastKeepHeading = 0.0;
        int      lastStatusCode  = StatusCodes.STATUS_NONE;
        int      lastMotionState = STATE_UNDEFINED;
        GeoPoint lastValidPoint  = null;
        double   lastOdometerKM  = startOdomKM;
        boolean  checkedReload   = false;
        boolean  isReload        = false;
        boolean  isIgnitionOn    = false;
        long     stoppedTimeSec  = 0L;
        boolean  isFirstEvent    = true;
        int      extStatusCode   = StatusCodes.STATUS_NONE;
        long     timeOffsetSec   = 0L;
        for (int r = 0;; r++) {

            /* read line */
            String line = null;
            try {
                line = FileTools.readLine(csvStream);
                if (line == null) { break; }
            } catch (EOFException eof) {
                break;
            }
            lineCount++;
            
            /* blank line */
            if (StringTools.isBlank(line)) {
                // skip any blank lines
                continue;
            }
            line = line.trim();

            /* comment */
            if (line.startsWith("#")) {
                // skip comment lines
                String cFld[] = StringTools.split(line.substring(1).trim(),',');
                if (ListTools.size(cFld) > 0) {
                    if (cFld[0].equalsIgnoreCase(COMMENT_TIMEZONE)) {
                        // #TimeZone,PST,-8
                        if (cFld.length >= 2) {
                            TimeZone tz = DateTime.getTimeZone(cFld[1].toUpperCase(),null);
                            if (tz != null) {
                                csvTMZ = tz;
                            }
                            if (cFld.length >= 3) {
                                int HH = StringTools.parseInt(cFld[2],0);
                                timeOffsetSec = DateTime.HourSeconds(HH);
                            }
                        }
                    } else 
                    if (cFld[0].equalsIgnoreCase(COMMENT_STATUSCODE)) {
                        // #StatusCode,0xF020
                        // #StatusCode,WAYMARK.0
                        if (cFld.length >= 2) {
                            extStatusCode = StatusCodes.ParseCode(cFld[1],privLabel,StatusCodes.STATUS_NONE);
                        }
                    }
                }
                continue;
            }

            /* parse into fields */
            String fld[] = StringTools.split(line,',');
            if (fld.length < 10) {
                // invalid number of fields
                Print.logError("[" + lineCount + "] " + line);
                Print.logError("Invalid number of data fields: " + fld.length);
                continue;
            }

            /* first non-blank line: header */
            if ((header == null) && fld[0].equalsIgnoreCase("Record")) {
                header = fld;
                for (int h = 0; h < header.length; h++) {
                    header[h] = header[h].toLowerCase();
                }
                continue;
            }

            /* date record */
            String  date        = null;
            String  time        = null;
            long    timestamp   = 0L;
            double  latitude    = 0.0;
            double  longitude   = 0.0;
            double  altitudeM   = 0.0;
            double  tempC       = 0.0;
            double  speedKPH    = 0.0;
            double  headingDeg  = 0.0;
            int     statusCode  = StatusCodes.STATUS_LOCATION;
            boolean validGPS    = false;
            int     satCount    = 0;
            double  odomKM      = 0.0;
            int     motionState = lastMotionState;
            boolean saveEvent   = false;
            String  saveReason  = "";
            for (int i = 0; i < header.length; i++) {

                /* invalid number of fields? */
                if (i >= fld.length) {
                    continue;
                }

                /* parse field */
                String H = header[i];
                String F = fld[i].toLowerCase();
                if (H.equalsIgnoreCase("Record")) {
                    // ignore
                } else
                if (H.equalsIgnoreCase("Date")) {
                    // "03/24/2011", "03/24/11"
                    // "24-Mar-11", "24-March-2011"
                    // "Thursday, March 24, 2011"
                    date = F;
                } else
                if (H.equalsIgnoreCase("Time")) {
                    // "17:18:00", "05:18:00 pm"
                    time = F;
                } else
                if (H.equalsIgnoreCase("Latitude")) {
                    // "39.123456", "39^ 12' 34""
                    latitude = GeoPoint.parseLatitude(F,0.0);
                } else
                if (H.equalsIgnoreCase("Longitude")) {
                    // -142.123456
                    longitude = GeoPoint.parseLongitude(F,0.0);
                } else
                if (H.equalsIgnoreCase("Altitude")) {
                    // 564.5 (meters)
                    altitudeM = StringTools.parseDouble(F,0.0);
                } else
                if (H.equalsIgnoreCase("Temp")) {
                    // 28.9 (C)
                    tempC = StringTools.parseDouble(F,0.0);
                } else
                if (H.equalsIgnoreCase("Status")) {
                    // "Power On", "Power Off", "Stopped X min", "4 kph", ...
                    if (F.equalsIgnoreCase("Power On")) {
                        statusCode  = StatusCodes.STATUS_POWER_ON;
                        motionState = STATE_UNDEFINED;
                        saveEvent   = true;
                        saveReason  = "Power On Event";
                    } else
                    if (F.equalsIgnoreCase("Power Off")) {
                        if (motionState == STATE_MOVING) {
                            // TODO: insert Stopped event?
                        }
                        statusCode  = StatusCodes.STATUS_POWER_OFF;
                        motionState = STATE_UNDEFINED;
                        saveEvent   = true;
                        saveReason  = "Power Off Event";
                    } else
                    if (StringTools.startsWithIgnoreCase(F,"Stopped")) {
                        // "Stopped XX min"
                        // "Stopped XX hour XX min"
                        if (motionState != STATE_STOPPED) {
                            statusCode  = StatusCodes.STATUS_MOTION_STOP;
                            motionState = STATE_STOPPED;
                            saveEvent   = true;
                            saveReason  = "Stopped Event";
                        } else {
                            // already stopped
                            statusCode  = StatusCodes.STATUS_MOTION_DORMANT;
                        }
                        String stopTimeStr = F.substring("Stopped".length()).trim();
                        if (lastStatusCode == StatusCodes.STATUS_POWER_ON) {
                            // previous event was a power-on, ignore this 'Stopped' time
                            stoppedTimeSec = 0L;
                        } else {
                            long stopTime = StringTools.parseLong(stopTimeStr,0L);
                            if (StringTools.endsWithIgnoreCase(stopTimeStr,"min")) {
                                stoppedTimeSec = stopTime * 60L; // min ==> sec
                            } else 
                            if (StringTools.endsWithIgnoreCase(stopTimeStr,"sec")) {
                                stoppedTimeSec = stopTime; // handle "sec" (if present)
                            } else {
                                Print.logWarn("Unable to determine amount of stopped time: " + F);
                                stoppedTimeSec = 0L;
                            }
                        }
                    } else
                    if (StringTools.endsWithIgnoreCase(F,"kph")) {
                        speedKPH = StringTools.parseDouble(F,0.0);
                        if (motionState != STATE_MOVING) {
                            // was not previously moving
                            statusCode  = StatusCodes.STATUS_MOTION_START;
                            motionState = STATE_MOVING;
                            saveEvent   = true;
                            saveReason  = "Start Event (kph)";
                        } else {
                            // already moving
                            statusCode  = StatusCodes.STATUS_MOTION_IN_MOTION;
                        }
                    } else
                    if (StringTools.endsWithIgnoreCase(F,"mph")) {
                        speedKPH = StringTools.parseDouble(F,0.0) * GeoPoint.KILOMETERS_PER_MILE;
                        if (motionState != STATE_MOVING) {
                            // was not previously moving
                            statusCode  = StatusCodes.STATUS_MOTION_START;
                            motionState = STATE_MOVING;
                            saveEvent   = true;
                            saveReason  = "Start Event (mph)";
                        } else {
                            // already moving
                            statusCode  = StatusCodes.STATUS_MOTION_IN_MOTION;
                        }
                    } else
                    if (StringTools.endsWithIgnoreCase(F,"kts")) {
                        speedKPH = StringTools.parseDouble(F,0.0) * GeoPoint.KILOMETERS_PER_NAUTICAL_MILE;
                        if (motionState != STATE_MOVING) {
                            // was not previously moving
                            statusCode  = StatusCodes.STATUS_MOTION_START;
                            motionState = STATE_MOVING;
                            saveEvent   = true;
                            saveReason  = "Start Event (kts)";
                        } else {
                            // already moving
                            statusCode  = StatusCodes.STATUS_MOTION_IN_MOTION;
                        }
                    } else {
                        statusCode = StatusCodes.STATUS_LOCATION;
                    }
                } else
                if (H.equalsIgnoreCase("Course")) {
                    // "337.6", "N", "SE", ...
                    if (F.equalsIgnoreCase("N")) {
                        headingDeg =   0.0;
                    } else
                    if (F.equalsIgnoreCase("NE")) {
                        headingDeg =  45.0;
                    } else
                    if (F.equalsIgnoreCase("E")) {
                        headingDeg =  90.0;
                    } else
                    if (F.equalsIgnoreCase("SE")) {
                        headingDeg = 135.0;
                    } else
                    if (F.equalsIgnoreCase("S")) {
                        headingDeg = 180.0;
                    } else
                    if (F.equalsIgnoreCase("SW")) {
                        headingDeg = 225.0;
                    } else
                    if (F.equalsIgnoreCase("W")) {
                        headingDeg = 270.0;
                    } else
                    if (F.equalsIgnoreCase("NW")) {
                        headingDeg = 315.0;
                    } else {
                        headingDeg = StringTools.parseDouble(F,0.0);
                    }
                } else
                if (H.equalsIgnoreCase("GPS Fix")) {
                    // "Y", "N"
                    validGPS = F.equalsIgnoreCase("Y");
                } else
                if (H.equalsIgnoreCase("Signal")) {
                    // 7 (Sat#?)
                    satCount = StringTools.parseInt(F,0);
                } else
                if (H.equalsIgnoreCase("MapLink")) {
                    // http://maps.google.com/maps?q=39.123456+-142.123456&h=en&t=h
                    // ignore
                } else
                if (H.equalsIgnoreCase("Name")) {
                    // "03/24/2011 17:22:00 Stopped 8 min"
                    // ignore
                } else
                if (H.equalsIgnoreCase("DeviceName")) {
                    // "TrackStick"
                    // ignore
                }

            } // parsed all record fields

            /* get timestamp */
            timestamp = parseDateTime(date, time, csvTMZ) + timeOffsetSec;
            if (timestamp <= 0L) {
                Print.logError("[" + lineCount + "] " + line);
                Print.logError("Skipping record with invalid timestamp: " + timestamp);
                continue;
            }
            // 'timestamp' valid after this point

            /* first-record? */
            if (isFirstEvent) {

                /* get previous odometer value */
                try {
                    EventData prevEv = EventData.getPreviousEventData(
                        accountID, deviceID,
                        timestamp, null/*statusCodes*/,
                        true/*validGPS*/);
                    if (prevEv != null) {
                        // reset device 'last' values
                        lastOdometerKM = prevEv.getOdometerKM();
                        device.setLastOdometerKM(prevEv.getOdometerKM());
                        device.setLastValidLatitude(prevEv.getLatitude());
                        device.setLastValidLongitude(prevEv.getLongitude());
                        device.setLastValidHeading(prevEv.getHeading());
                        device.setLastGPSTimestamp(prevEv.getTimestamp());
                    }
                } catch (DBException dbe) {
                    Print.logException("Unable obtain previous event: " + accountID + "/" + deviceID, dbe);
                }

                /* pre-clear events */
                if (!DEBUG_MODE && this.preClearEvents) {
                    Print.logInfo("Pre-Clearing Events ...");
                    try {
                        long delCount = EventData.deleteEventsAfterTimestamp(
                            accountID, deviceID, 
                            timestamp, true/*inclusive*/);
                        if (delCount > 0L) {
                            isReload = true;
                            Print.logWarn("Found/Deleted "+delCount+" existing events from '"+
                                (new DateTime(timestamp))+"' ["+timestamp+"]");
                        }
                        if (PRE_CLEAR_ONLY) {
                            Print.logInfo("Exiting due to 'PreClearOnly' ...");
                            System.exit(0);
                        }
                    } catch (DBException dbe) {
                        Print.logException("Unable delete existing events: " + accountID + "/" + deviceID, dbe);
                    }
                }

                /* first event handled */
                isFirstEvent = false;

            }

            /* geoPoint */
            GeoPoint geoPoint = null;
            if (validGPS && GeoPoint.isValid(latitude,longitude)) {
                geoPoint = new GeoPoint(latitude,longitude);
            } else
            if ((lastValidPoint != null)                    && 
                (statusCode == StatusCodes.STATUS_POWER_OFF)  ) {
                validGPS  = true;
                geoPoint  = lastValidPoint;
                latitude  = lastValidPoint.getLatitude();
                longitude = lastValidPoint.getLongitude();
            } else {
                validGPS  = false;
                geoPoint  = GeoPoint.INVALID_GEOPOINT;
                latitude  = 0.0;
                longitude = 0.0;
            }

            /* speed/heading */
            if (speedKPH < this.minSpeedKPH) {
                speedKPH   = 0.0;
                headingDeg = 0.0;
            }

            /* calc next GPS-based odometer */
            odomKM = lastOdometerKM;
            if (validGPS && (lastValidPoint != null)) {
                double deltaKM = lastValidPoint.kilometersToPoint(geoPoint);
                odomKM += deltaKM;
            }

            /* count */
            totalCount++;

            /* record info */
            long   deltaKeepTimeSec = timestamp - lastKeepTime;
            double deltaKeepHeading = Math.abs(headingDeg - lastKeepHeading);
            if (deltaKeepHeading > 180.0) { deltaKeepHeading = 360.0 - deltaKeepHeading; }

            /* keep this record? */
            boolean keepEvent  = false;
            String  keepReason = "";
            if (saveEvent) {
                // we've already decided to keep this event
                keepEvent  = true;
                keepReason = saveReason;
            } else
            if (lastMotionState != motionState) {
                // motion state change (should already be handled above)
                keepEvent  = true;
                keepReason = "Moving state change Event";
            } else
            if ((motionState == STATE_STOPPED) && (deltaKeepTimeSec >= this.minDormantSec)) {
                // not moving, but beyond minimum dormant time since last 'keep'
                keepEvent  = true;
                keepReason = "Periodic Dormant Event";
            } else
            if ((motionState == STATE_MOVING) && (deltaKeepTimeSec >= this.minMovingSec)) {
                // moving, but beyond minimum moving time since last 'keep'
                keepEvent  = true;
                keepReason = "Periodic Moving Event";
            } else
            if ((motionState == STATE_MOVING) && (deltaKeepHeading > this.minHeadingChange)) {
                // moving, less than moving minimum, but beyond delta heading change since last 'keep'
                keepEvent  = true;
                keepReason = "Heading Change Event";
            } else { 
                keepEvent  = false;
            }

            /* keep/save count */
            if (keepEvent) {
                // this event will be saved below, check for 'reload' of CSV file
                if (!checkedReload) {
                    EventData ed = null;
                    try {
                        ed = EventData.getEventData(accountID, deviceID, timestamp, statusCode);
                        if (ed != null) {
                            isReload = true;
                            // reload, force odometer to the previous event odometer
                            Print.logWarn("Possible CSV 'reload' detected!!! : " + ed);
                            Print.logWarn("(Using previous event odometer starting value)");
                            odomKM = ed.getOdometerKM();
                        }
                    } catch (DBException dbe) {
                        Print.logError("Unable to read EventData record: " + dbe);
                    }
                    checkedReload = true;
                }
                /* count this saved record */
                saveCount++;
            }

            /* debug */
            if (TrackStick.PrintAllEvents || keepEvent) {
                TimeZone dtz = (this.displayTimeZone != null)? this.displayTimeZone : csvTMZ;
                Print.logInfo("---------------------------------------------");
                Print.logInfo("Include Event: " + (keepEvent?"true":"false") + " - " + keepReason);
                Print.logInfo("Count        : " + totalCount + (keepEvent?" ("+saveCount+")":""));
                Print.logInfo("Timestamp    : " + "["+timestamp+"] " + (new DateTime(timestamp,dtz)));
                Print.logInfo("Status       : " + StatusCodes.GetDescription(statusCode,null));
                Print.logInfo("GeoPoint     : " + geoPoint);
                Print.logInfo("Altitude     : " + StringTools.format(altitudeM,"0.0") + " meters" +
                    " ("+StringTools.format(altitudeM*GeoPoint.FEET_PER_METER,"0.0") + " feet)");
                Print.logInfo("Speed        : " + StringTools.format(speedKPH,"0.0") + " km/h" +
                    " ("+StringTools.format(speedKPH*GeoPoint.MILES_PER_KILOMETER,"0.0") + " mph)" +
                    " heading " + headingDeg + " (" + GeoPoint.GetHeadingDescription(headingDeg,null) + ")");
                Print.logInfo("Temp         : " + StringTools.format(tempC,"0.0") + " C" +
                    " ("+StringTools.format(temp_C2F(tempC),"0.0")+" F)");
                Print.logInfo("Odometer     : " + StringTools.format(odomKM,"0.0") + " km" +
                    " ("+StringTools.format(odomKM*GeoPoint.MILES_PER_KILOMETER,"0.0") + " miles) " +
                    ""); //(this.estimateOdometer? "saved" : "not saved"));
            }

            /* save last */
            lastStatusCode  = statusCode;
            lastMotionState = motionState;
            if (validGPS) {
                lastValidPoint = geoPoint;
            }
            lastOdometerKM  = odomKM;

            /* insert record */
            if (keepEvent) {
                lastKeepTime    = timestamp;
                lastKeepHeading = headingDeg;
                // pre-event: ignition on
                if (this.addIgnitionState && !isIgnitionOn && (statusCode == StatusCodes.STATUS_MOTION_START)) {
                    // Add ignition-on?
                    long ts = timestamp - 1L;
                    int  sc = StatusCodes.STATUS_IGNITION_ON;
                    this.insertEventRecord(device,
                        ts, sc, null/*GeoZone*/,
                        geoPoint, 0L/*gpsAge*/, 0.0/*HDOP*/, satCount,
                        speedKPH, headingDeg, altitudeM, odomKM,
                        tempC);
                    isIgnitionOn  = true;
                }
                // event
                if ((statusCode != StatusCodes.STATUS_NONE  ) && 
                    (statusCode != StatusCodes.STATUS_IGNORE)   ) {
                    this.insertEventRecord(device,
                        timestamp, statusCode, null/*GeoZone*/,
                        geoPoint, 0L/*gpsAge*/, 0.0/*HDOP*/, satCount,
                        speedKPH, headingDeg, altitudeM, odomKM,
                        tempC);
                }
                if ((extStatusCode != StatusCodes.STATUS_NONE  ) && 
                    (extStatusCode != StatusCodes.STATUS_IGNORE) &&
                    (extStatusCode != statusCode               )   ) {
                    this.insertEventRecord(device,
                        timestamp, extStatusCode, null/*GeoZone*/,
                        geoPoint, 0L/*gpsAge*/, 0.0/*HDOP*/, satCount,
                        speedKPH, headingDeg, altitudeM, odomKM,
                        tempC);
                }
                // post-event: ignition on
                if (this.addIgnitionState && isIgnitionOn && (statusCode == StatusCodes.STATUS_MOTION_STOP) &&
                    ((stoppedTimeSec <= 0L) || (stoppedTimeSec > MIN_IGNITION_STOP_TIME))) {
                    // Add ignition-off?
                    long ts = timestamp + 1L;
                    int  sc = StatusCodes.STATUS_IGNITION_OFF;
                    this.insertEventRecord(device,
                        ts, sc, null/*GeoZone*/,
                        geoPoint, 0L/*gpsAge*/, 0.0/*HDOP*/, satCount,
                        speedKPH, headingDeg, altitudeM, odomKM,
                        tempC);
                    isIgnitionOn  = false;
                }
            }

        } // CSV record loop

        /* update device info */
        if (!DEBUG_MODE && (this.device != null)) {
            try {
                //DBConnection.pushShowExecutedSQL();
                this.device.updateChangedEventFields();
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
            } finally {
                //DBConnection.popShowExecutedSQL();
            }
        }

        /* final counts */
        Print.logInfo("---------------------------------------------");
        Print.logInfo("");
        Print.logInfo("Event Counts:");
        Print.logInfo("  Total    = " + totalCount);
        Print.logInfo("  Omitted  = " + (totalCount - saveCount));
        Print.logInfo("  Included = " + saveCount);
        Print.logInfo("  Stored   = " + this.eventTotalCount);
        Print.logInfo("");

        return (saveCount > 0);

    }

    // ------------------------------------------------------------------------

    /* create event record */
    private EventData createEventRecord(Device device, 
        long     timestamp, int statusCode, Geozone geozone,
        GeoPoint geoPoint, long gpsAge, double HDOP, int numSats,
        double   speedKPH, double heading, double altitude, double odomKM,
        double   tempC)
    {
        String accountID    = device.getAccountID();
        String deviceID     = device.getDeviceID();
        EventData.Key evKey = new EventData.Key(accountID, deviceID, timestamp, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeozone(geozone);
        evdb.setGeoPoint(geoPoint);
        evdb.setGpsAge(gpsAge);
        evdb.setHDOP(HDOP);
        evdb.setSatelliteCount(numSats);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(heading);
        evdb.setAltitude(altitude);
        evdb.setOdometerKM(odomKM);
        evdb.setThermoAverage0(tempC);
        //evdb.setThermoAverage1(tempC);
        return evdb;
    }

    /* create and insert an event record */
    private void insertEventRecord(Device device, 
        long     timestamp, int statusCode, Geozone geozone,
        GeoPoint geoPoint, long gpsAge, double HDOP, int numSats,
        double   speedKPH, double heading, double altitude, double odomKM,
        double   tempC)
    {

        /* debug mode? */
        if (device == null) {
            return;
        } else
        if (DEBUG_MODE) {
            Print.logInfo("Non-stored Event : [0x" + StringTools.toHexString(statusCode,16) + "] " + 
                StatusCodes.GetDescription(statusCode,null));
            return;
        }

        /* include odometer? */
        if (!this.estimateOdometer) {
            odomKM = 0.0;
        }

        /* create event */
        EventData evdb = createEventRecord(device, 
            timestamp, statusCode, geozone,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, heading, altitude, odomKM,
            tempC);

        /* insert event */
        // this will display an error if it was unable to store the event
        device.insertEventData(evdb);
        this.eventTotalCount++;
        Print.logInfo("Stored Event : [0x" + StringTools.toHexString(statusCode,16) + "] " + 
            StatusCodes.GetDescription(statusCode,null));

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String ARG_HELP[]          = new String[] { "help"    ,            "h"           };
    private static String ARG_ACCOUNT[]       = new String[] { "account" , "acct"   , "a"           };
    private static String ARG_DEVICE[]        = new String[] { "device"  , "dev"    , "d"           };
    private static String ARG_CSV_FILE[]      = new String[] { "csvFile" , "csv"                    };
    private static String ARG_CSV_TMZ[]       = new String[] { "timezone", "tmz"    , "cvsTmz", "tz"};
    private static String ARG_DISP_TMZ[]      = new String[] { "dispTmz" ,                          };
    private static String ARG_SAVEODOM[]      = new String[] { "saveOdom", "estOdom"                };
    private static String ARG_IGNITION[]      = new String[] { "addIgn"  , "ign"                    };
    private static String ARG_SHOWALL[]       = new String[] { "printAll", "showAll"                };
    private static String ARG_NOSAVE[]        = new String[] { "nosave"  ,                          };
    private static String ARG_PRECLEAR[]      = new String[] { "preClear", "clear", "preClearOnly"  };
    private static String ARG_PRECLEAR_ONLY[] = new String[] { "preClearOnly"                       };
    private static String ARG_REVGEO[]        = new String[] { "revgeo"  ,                          };

    private static void usage(int exit)
    {
        Print.sysPrintln("");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + TrackStick.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -account=<id>    Acount ID which owns Device");
        Print.sysPrintln("  -device=<id>     Device ID to which parsed events will be inserted");
        Print.sysPrintln("  -tmz=<timeZone>  The TimeZone of the times represented in the CSV file");
        Print.sysPrintln("  -csv=<file>      The CSV file to parse");
        Print.sysPrintln("  -estOdom         Include GPS-based estimated odometer in events");
        Print.sysPrintln("  -noSave          Do not save events, nor update Device record");
        Print.sysPrintln("  -preClear        Delete any existing events after the starting timestamp");
        Print.sysPrintln("  -help            Display this help");
        Print.sysPrintln("Notes:");
        Print.sysPrintln(" * Input file must be in CSV format from a TrackStick device");
        Print.sysPrintln(" * The first CSV record must be the column header information");
        Print.sysPrintln(" * Altitude is expected to be in meters");
        Print.sysPrintln(" * Temperature is expected to be in Celsius");
        Print.sysPrintln(" * Loading a CSV file more than once may skew the estimated odometer value");
        Print.sysPrintln("");
        System.exit(exit);
    }

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv, true);  // main
        
        /* help only */
        if (RTConfig.getBoolean(ARG_HELP,false)) {
            TrackStick.usage(0);
            System.exit(0); // control does not reach here
        }

        /* options */
        DEBUG_MODE         = RTConfig.isDebugMode() || RTConfig.getBoolean(ARG_NOSAVE,false);
        String  accountID  = RTConfig.getString(ARG_ACCOUNT , null);
        String  deviceID   = RTConfig.getString(ARG_DEVICE  , null);
        File    csvFile    = RTConfig.getFile(  ARG_CSV_FILE, null);
        String  tmzStr     = RTConfig.getString(ARG_CSV_TMZ , null);
        String  dispTmzStr = RTConfig.getString(ARG_DISP_TMZ, tmzStr);

        /* Print/Show all events */
        TrackStick.PrintAllEvents = RTConfig.getBoolean(ARG_SHOWALL,false);

        /* save/estimate odometer */
        if (RTConfig.hasProperty(ARG_SAVEODOM)) {
            // override PROP_estimateOdometer property
            boolean saveOdom = RTConfig.getBoolean(ARG_SAVEODOM, false);
            RTConfig.setBoolean(PROP_estimateOdometer, saveOdom);
        }

        /* add ignition state */
        if (RTConfig.hasProperty(ARG_IGNITION)) {
            // override PROP_addIgnitionState property
            boolean addIgn = RTConfig.getBoolean(ARG_IGNITION, false);
            RTConfig.setBoolean(PROP_addIgnitionState, addIgn);
        }

        /* pre-clear events */
        if (RTConfig.hasProperty(ARG_PRECLEAR)) {
            // override PROP_preClearEvents property
            boolean preClear = RTConfig.getBoolean(ARG_PRECLEAR, false);
            RTConfig.setBoolean(PROP_preClearEvents, preClear);
            if (RTConfig.getBoolean(ARG_PRECLEAR_ONLY,false)) {
                PRE_CLEAR_ONLY = true;
            }
        }

        /* enable/disable reverse-geocoding */
        if (RTConfig.hasProperty(ARG_REVGEO)) {
            boolean revGeo = RTConfig.getBoolean(ARG_REVGEO, false);
            RTConfig.setBoolean(PROP_reverseGeocode, revGeo);
        }

        /* get Account/Device */
        Account account = null;
        Device  device  = null;
        try {
            account = Account.getAccount(accountID);
            if (account == null) {
                Print.logError("Account not found: " + accountID);
                TrackStick.usage(99);
                System.exit(99); // control does not reach here
            }
            device  = Device.getDevice(account,deviceID);
            if (device == null) {
                Print.logError("Device not found: " + deviceID);
                TrackStick.usage(99);
                System.exit(99); // control does not reach here
            }
        } catch (DBException dbe) {
            Print.sysPrintln("Unable to load Account/Device");
            Print.sysPrintln("AccountID : " + accountID);
            Print.sysPrintln("DeviceID  : " + deviceID);
            Print.sysPrintln("-------------------------------------------------------------------------------");
            dbe.printStackTrace();
            Print.sysPrintln("-------------------------------------------------------------------------------");
            System.exit(99);
        }

        /* csv timezone */
        TimeZone csvTmz = null;
        if (!StringTools.isBlank(tmzStr)) {
            csvTmz = DateTime.getTimeZone(tmzStr,null);
            if (csvTmz == null) {
                Print.sysPrintln("Invalid CSV TimeZone: " + tmzStr);
                TrackStick.usage(99);
                System.exit(99); // control does not reach here
            }
        }

        /* display timezone */
        TimeZone dspTmz = null;
        if (!StringTools.isBlank(dispTmzStr)) {
            dspTmz = DateTime.getTimeZone(dispTmzStr,null);
            if (dspTmz == null) {
                Print.sysPrintln("Invalid Display TimeZone: " + tmzStr);
                TrackStick.usage(99);
                System.exit(99); // control does not reach here
            }
        }

        /* CSV File? */
        if (csvFile == null) {
            Print.sysPrintln("ERROR: CSV File not specified");
            TrackStick.usage(99);
            System.exit(99); // control does not reach here
        } else
        if (!csvFile.isFile()) {
            Print.sysPrintln("ERROR: CSV File does not exist - " + csvFile);
            TrackStick.usage(99);
            System.exit(99); // control does not reach here
        }

        /* allow/disable slow reverse-geocoding */
        Device.SetAllowSlowReverseGeocoding(RTConfig.getBoolean(ARG_REVGEO,REVERSE_GEOCODE));

        /* parse */
        try {
            TrackStick ts = new TrackStick(account, device);
            ts.printHeader();
            ts.setDisplayTimeZone(dspTmz);
            ts.parseFile(csvFile, csvTmz);
        } catch (IOException ioe) {
            Print.logException("IO Error", ioe);
            System.exit(99);
        }
        
    }
    
}
