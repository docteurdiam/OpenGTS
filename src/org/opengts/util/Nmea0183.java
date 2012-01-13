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
//  Parse NMEA-0183 records, currently the following types:
//      - $GPRMC: Recommended Minimum Specific GPS/TRANSIT Data
//      - $GPGGA: Global Positioning System Fix Data
//      - $GPVTG: Track Made Good and Ground Speed
//      - $GPZDA: UTC Date/Time and Local Time Zone Offset
// References:
//  http://www.scientificcomponent.com/nmea0183.htm
//  http://home.mira.net/~gnb/gps/nmea.html
// ----------------------------------------------------------------------------
// Change History:
//  2007/07/27  Martin D. Flynn
//     -Initial release
//  2007/09/16  Martin D. Flynn
//     -Added 'getExtraData' method to return data following checksum.
//  2008/02/10  Martin D. Flynn
//     -Added handling of $GPVTG and $GPZDA record types
//     -Support parsing and combining multiple record types
//  2008/08/07  Martin D. Flynn
//     -Changed private '_calcChecksum' to public static 'calcXORChecksum'
//  2010/09/09  Martin D. Flynn
//     -Added ability to specify an array of records for parsing.
//  2010/10/21  Martin D. Flynn
//     -Added specific field checks: hasLatitude, hasLongitude, hasSpeed, etc.
//     -"getExtraData" now returns a String array.
//  2011/06/16  Martin D. Flynn
//     -Added "getIgnoreInvalidGpsFlag"/"setIgnoreInvalidGpsFlag" methods to
//      allow ignoring the A|V (valid|invalid) flag (yes, some actually want this)
//  2011/10/03  Martin D. Flynn
//     -Added check for valid data/time on GMRMC record.
// ----------------------------------------------------------------------------
package org.opengts.util;

/**
*** A container for a NMEA-0183 record
**/

public class Nmea0183
{
    
    // ------------------------------------------------------------------------

    /* type names */
    public static final String  NAME_NONE               = "NONE";
    public static final String  NAME_GPRMC              = "GPRMC";
    public static final String  NAME_GPGGA              = "GPGGA";
    public static final String  NAME_GPVTG              = "GPVTG";
    public static final String  NAME_GPZDA              = "GPZDA";

    /* $type names */
    public static final String  DNAME_GPRMC             = "$" + NAME_GPRMC;
    public static final String  DNAME_GPGGA             = "$" + NAME_GPGGA;
    public static final String  DNAME_GPVTG             = "$" + NAME_GPVTG;
    public static final String  DNAME_GPZDA             = "$" + NAME_GPZDA;

    // Note: these values can change between releases!
    public static final long    TYPE_NONE               = 0L;
    public static final long    TYPE_GPRMC              = 0x0000000000000001L;
    public static final long    TYPE_GPGGA              = 0x0000000000000002L;
    public static final long    TYPE_GPVTG              = 0x0000000000000004L;
    public static final long    TYPE_GPZDA              = 0x0000000000000008L;

    /**
    *** Gets the message type String from a type mask
    *** @param type The message type
    *** @return The type as a string
    **/
    public static String GetTypeNames(long type)
    {
        String sep = ",";
        StringBuffer sb = new StringBuffer();
        if ((type & TYPE_GPRMC) != 0) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPRMC);
        }
        if ((type & TYPE_GPGGA) != 0) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPGGA);
        }
        if ((type & TYPE_GPVTG) != 0) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPVTG);
        }
        if ((type & TYPE_GPZDA) != 0) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPZDA);
        }
        return (sb.length() > 0)? sb.toString() : NAME_NONE;
    }

    // ------------------------------------------------------------------------

    public static final long    FIELD_RECORD_TYPE       = 0x0000000000000001L;
    public static final long    FIELD_VALID_FIX         = 0x0000000000000002L;
    public static final long    FIELD_DDMMYY            = 0x0000000000000004L;
    public static final long    FIELD_HHMMSS            = 0x0000000000000008L;
    public static final long    FIELD_LATITUDE          = 0x0000000000000010L;
    public static final long    FIELD_LONGITUDE         = 0x0000000000000020L;
    public static final long    FIELD_SPEED             = 0x0000000000000040L;
    public static final long    FIELD_HEADING           = 0x0000000000000080L;
    public static final long    FIELD_HDOP              = 0x0000000000000100L;
    public static final long    FIELD_NUMBER_SATS       = 0x0000000000000200L;
    public static final long    FIELD_ALTITUDE          = 0x0000000000000400L;
    public static final long    FIELD_FIX_TYPE          = 0x0000000000000800L;

    // ------------------------------------------------------------------------

    public static final double  KILOMETERS_PER_KNOT     = 1.85200000;
    public static final double  KNOTS_PER_KILOMETER     = 1.0 / KILOMETERS_PER_KNOT;

    // ------------------------------------------------------------------------

    private boolean     validChecksum       = false;
    private long        parsedRcdTypes      = TYPE_NONE;
    private String      lastRcdType         = "";
    private long        fieldMask           = 0L;
    
    private long        ddmmyy              = 0L;
    private long        hhmmss              = 0L;
    private long        fixtime             = 0L;
    
    private boolean     ignoreGpsFlag       = false;
    private boolean     ignoredInvalidGPS   = false;
    private boolean     isValidGPS          = false;
    private double      latitude            = 0.0;
    private double      longitude           = 0.0;
    private GeoPoint    geoPoint            = null;
    
    private double      speedKnots          = 0.0;
    private double      heading             = 0.0;
    
    private double      hdop                = 0.0;
    private int         numSats             = 0;
    private double      altitudeM           = 0.0;
    private int         fixType             = 0;
    
    private double      magVariation        = 0.0;
    
    private String      extraData[]         = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public Nmea0183()
    {
        super();
    }
    
    /**
    *** Constructor
    *** @param rcd The NMEA-0183 record
    **/
    public Nmea0183(String rcd)
    {
        this();
        this.parse(rcd, false);
    }
    
    /**
    *** Constructor
    *** @param rcds An array of NMEA-0183 records
    **/
    public Nmea0183(String rcds[])
    {
        this();
        this.parse(rcds, false);
    }

    /**
    *** Constructor
    *** @param rcd The NMEA-0183 record
    *** @param ignoreChecksum True if the record's checksum is to be ignored
    **/
    public Nmea0183(String rcd, boolean ignoreChecksum)
    {
        this();
        this.parse(rcd, ignoreChecksum);
    }

    /**
    *** Constructor
    *** @param rcds An array of NMEA-0183 records
    *** @param ignoreChecksum True if the record's checksum is to be ignored
    **/
    public Nmea0183(String rcds[], boolean ignoreChecksum)
    {
        this();
        this.parse(rcds, ignoreChecksum);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the mask of available fields
    *** @return The mask of available fields
    **/
    public long getFieldMask()
    {
        return this.fieldMask;
    }
    
    /**
    *** Returns true if specified field is available
    *** @return True if specified field is available
    **/
    public boolean hasField(long fld)
    {
        return ((this.fieldMask & fld) != 0);
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets all parsed record types (mask)
    *** @return all parsed record types (mask)
    **/
    public long getParsedRecordTypes()
    {
        return this.parsedRcdTypes;
    }

    /**
    *** Gets the last record type
    *** @return The last record type
    **/
    public String getLastRecordType()
    {
        return this.lastRcdType;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the checksum is valid
    *** @return True if the checksum is valid
    **/
    public boolean isValidChecksum()
    {
        return this.validChecksum;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the day/month/year (for "$GPGGA" records)
    *** @param ddmmyy The day/month/year [CHECK](as what?)
    **/
    public void setDDMMYY(long ddmmyy)
    {
        if ((ddmmyy >=  10100L) && (ddmmyy <= 311299L)) { // day/month must be specified
            this.ddmmyy = ddmmyy;
            this.fieldMask |= FIELD_DDMMYY;
        } else {
            this.ddmmyy = 0L;
            this.fieldMask &= ~FIELD_DDMMYY;
        }
    }

    /**
    *** Gets the day/month/year of the fix
    *** @return The day/month/year of the fix
    **/
    public long getDDMMYY()
    {
        // this.hasField(FIELD_DDMMYY)
        return this.ddmmyy;
    }

    /**
    *** Gets the hour/minute/seconds of the fix
    *** @return The hour/minute/seconds of the fix 
    **/
    public long getHHMMSS()
    {
        // this.hasField(FIELD_HHMMSS)
        return this.hhmmss;
    }

    /**
    *** Returns true if the fixtime has been defined
    *** @return True if the fixtime has been defined
    **/
    public boolean hasFixtime()
    {
        return this.hasField(FIELD_DDMMYY) && this.hasField(FIELD_HHMMSS);
    }

    /**
    *** Gets the epoch fix time
    *** @return the epoch fix time 
    **/
    public long getFixtime()
    {
        return this.getFixtime(false);
    }

    /**
    *** Gets the epoch fix time
    *** @return the epoch fix time 
    **/
    public long getFixtime(boolean dftToCurrentTOD)
    {
        if (this.fixtime <= 0L) {
            // fix time not yet set
            boolean hasDMY = this.hasField(FIELD_DDMMYY);
            boolean hasHMS = this.hasField(FIELD_HHMMSS);
            if (hasDMY && hasHMS) {
                // both DMY and HMS defined
                this.fixtime = this._getUTCSeconds(this.ddmmyy, this.hhmmss);
            } else
            if (!hasHMS) {
                // no HMS, DMY will default to current day if not defined
                long DMY = this.ddmmyy; // will default to current day if undefined
                if (dftToCurrentTOD) {
                    // default to current time-of-day
                    DateTime nowDT = new DateTime(DateTime.GMT);
                    int  HH  = nowDT.getHour24(DateTime.GMT);
                    int  MM  = nowDT.getMinute(DateTime.GMT);
                    int  SS  = nowDT.getSecond(DateTime.GMT);
                    long HMS = (HH * 10000L) + (MM * 100L) + SS;
                    this.fixtime = this._getUTCSeconds(DMY, HMS);
                    // Warning: 
                    // If DMY is defined and the time is near midnight, this may generate
                    // an HMS just after midnight, when a time just before midnight is
                    // more accurate.
                } else {
                    // will default to 00:00:00 GMT on current day if DMY not defined
                    this.fixtime = this._getUTCSeconds(DMY, 0L);
                }
            } else {
                // HSM defined, DMY will default to current day if not defined
                long DMY = hasDMY? this.ddmmyy : 0L;
                this.fixtime = this._getUTCSeconds(DMY, this.hhmmss);
            }
        }
        return this.fixtime;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets whether the A|V (valid|invalid) GPS location flag should be ignored.<br>
    *** Setting to "true" is not recommended as invalid GPS locations may be returned.
    **/
    public void setIgnoreInvalidGpsFlag(boolean ignore)
    {
        this.ignoreGpsFlag = ignore;
        this.ignoredInvalidGPS = false;
    }
    
    /**
    *** gets whether the A|V (valid|invalid) GPS location flag should be ignored.<br>
    *** @return True if the GPS A|V flag should be ignored, false otherwise.
    **/
    public boolean getIgnoreInvalidGpsFlag()
    {
        return this.ignoreGpsFlag;
    }
    
    /** 
    *** Returns true if ignoring invalid GPS flags, and the flag indicator was not "A"
    *** @return True if ignoring invalid GPS flags, and the flag indicator was not "A"
    **/
    public boolean didIgnoreInvalidGPS()
    {
        return this.isValidGPS() && this.ignoredInvalidGPS;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the GPS fix is valid
    *** @return True if the GPS fix is valid
    **/
    public boolean isValidGPS()
    {
        return this.isValidGPS;
    }
   
    /**
    *** Returns true if the latitude has been defined
    *** @return Ttrue if the latitude has have been defined
    **/
    public boolean hasLatitude()
    {
        return this.hasField(FIELD_LATITUDE);
    }

    /**
    *** Gets the latitude
    *** @return The latitude
    **/
    public double getLatitude()
    {
        return this.latitude;
    }
  
    /**
    *** Returns true if the longitude has been defined
    *** @return Ttrue if the longitude has have been defined
    **/
    public boolean hasLongitude()
    {
        return this.hasField(FIELD_LONGITUDE);
    }

    /**
    *** Gets the longitude
    *** @return The longitude
    **/
    public double getLongitude()
    {
        return this.longitude;
    }
    
    /**
    *** Returns true if the latitude/longitude have been defined
    *** @return Ttrue if the latitude/longitude have been defined
    **/
    public boolean hasGeoPoint()
    {
        return this.hasField(FIELD_LATITUDE) && this.hasField(FIELD_LONGITUDE);
    }

    /**
    *** Gets the lat/lon as a GeoPoint
    *** @return the lat/lon as a GeoPoint
    **/
    public GeoPoint getGeoPoint()
    {
        if (this.geoPoint == null) {
            this.geoPoint = new GeoPoint(this.getLatitude(),this.getLongitude());
        }
        return this.geoPoint;
    }

    /**
    *** Sets the lat/lon as a GeoPoint
    *** @param gp  The GeoPoint
    **/
    public void setGeoPoint(GeoPoint gp)
    {
        if ((gp != null) && gp.isValid()) {
            this.latitude   = gp.getLatitude();
            this.longitude  = gp.getLongitude();
            this.geoPoint   = gp;
            this.isValidGPS = true;
        } else {
            this.latitude   = 0.0;
            this.longitude  = 0.0;
            this.geoPoint   = null;
            this.isValidGPS = false;
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns true if the speed has been defined
    *** @return True if the speed has been defined
    **/
    public boolean hasSpeed()
    {
        return this.hasField(FIELD_SPEED);
    }

    /**
    *** Returns the speed in knots 
    *** @return The speed in knots
    **/
    public double getSpeedKnots()
    {
        return this.speedKnots;
    }

    /**
    *** Gets the speed in KPH
    *** @return The speed in KPH
    **/
    public double getSpeedKPH()
    {
        return this.speedKnots * KILOMETERS_PER_KNOT;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the heading has been defined
    *** @return True if the heading has been defined
    **/
    public boolean hasHeading()
    {
        return this.hasField(FIELD_HEADING);
    }

    /**
    *** Gets the heading/course in degrees
    *** @return The heading/course in degrees
    **/
    public double getHeading()
    {
        return this.heading;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the "$GPGGA" fix type (0=no fix, 1=GPS, 2=DGPS, 3=PPS?, 6=dead-reckoning)
    *** @return The "$GPGGA fix type
    **/
    public int getFixType()
    {
        return this.fixType;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the number of satellites used in fix
    *** @return The number of satellites used in fix
    **/
    public int getNumberOfSatellites()
    {
        return this.numSats;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the horizontal-dilution-of-precision
    *** @return The horizontal-dilution-of-precision
    **/
    public double getHDOP()
    {
        return this.hdop;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the altitude has been defined
    *** @return True if the altitude has been defined
    **/
    public boolean hasAltitude()
    {
        return this.hasField(FIELD_ALTITUDE);
    }

    /**
    *** Gets the altitude in meters
    *** @return The altitude in meters
    **/
    public double getAltitudeMeters()
    {
        return this.altitudeM;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the magnetic variation in degrees
    *** @return The magnetic variation in degrees
    **/
    public double getMagneticVariation()
    {
        return this.magVariation;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets any data that may follow the checksum
    *** @return Any data that may follow the checksum (may be null)
    **/
    public String[] getExtraData()
    {
        return this.extraData;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns a string representation of this object
    *** @return A string representation of this object
    **/
    public String toString()
    {
        long types = this.getParsedRecordTypes();
        StringBuffer sb = new StringBuffer();
        sb.append("RcdType  : ").append(GetTypeNames(types)).append(" [0x").append(StringTools.toHexString(types,16)).append("]\n");
        sb.append("Checksum : ").append(this.isValidChecksum()?"ok":"failed").append("\n");
        sb.append("Fixtime  : ").append(this.getFixtime()).append(" [").append(new DateTime(this.getFixtime()).toString()).append("]\n");
        sb.append("GPS      : ").append(this.isValidGPS()?"valid ":"invalid ").append(this.getGeoPoint().toString()).append("\n");
        sb.append("SpeedKPH : ").append(this.getSpeedKPH()).append(" kph, heading ").append(this.getHeading()).append("\n");
        sb.append("Altitude : ").append(this.getAltitudeMeters()).append(" meters\n");
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Return a formatted $GPRMC record from the values in this instance
    *** @return A formatted $GPRMC record
    **/
    public String toGPRMC()
    {
        // $GPRMC,080701.00,A,3128.7540,N,14257.6714,W,000.0,000.0,180707,,,A*1C
        DateTime ft = new DateTime(this.getFixtime(), DateTime.GMT);
        GeoPoint gp = this.getGeoPoint();
        StringBuffer sb = new StringBuffer();
        sb.append("$").append(NAME_GPRMC);
        sb.append(",");
        sb.append(ft.format("HHmmss")).append(".00");
        sb.append(",");
        sb.append(this.isValidGPS()? "A" : "V");
        sb.append(",");
        sb.append(gp.getLatitudeString(GeoPoint.SFORMAT_NMEA,null));
        sb.append(",");
        sb.append(gp.getLongitudeString(GeoPoint.SFORMAT_NMEA,null));
        sb.append(",");
        sb.append(StringTools.format(this.getSpeedKnots(),"0.0"));
        sb.append(",");
        sb.append(StringTools.format(this.getHeading(),"0.0"));
        sb.append(",");
        sb.append(ft.format("ddMMyy"));
        sb.append(",");
        double magDeg = this.getMagneticVariation();
        if (magDeg != 0.0) {
            sb.append(StringTools.format(Math.abs(magDeg),"0.0")).append((magDeg >= 0.0)? ",E" : ",W");
        } else {
            // blank mag variation/direction
            sb.append(",");
        }
        sb.append(",A");
        int cksum = Nmea0183.calcXORChecksum(sb.toString(),false);
        sb.append("*");
        sb.append(StringTools.toHexString(cksum,8));
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parses a NMEA-0183 record
    *** @param rcds An array of NMEA-0183 records to parse
    *** @return True if this record was successfully parsed
    **/
    public boolean parse(String rcds[])
    {
        return this.parse(rcds, false);
    }

    /**
    *** Parses a NMEA-0183 record
    *** @param rcd the NMEA-0183 record to parse
    *** @return True if this record was successfully parsed
    **/
    public boolean parse(String rcd)
    {
        return this.parse(rcd, false);
    }

    /**
    *** Parses a NMEA-0183 record
    *** @param rcds An array of NMEA-0183 records to parse
    *** @param ignoreChecksum True to ignore the terminating checksum
    *** @return True if this record was successfully parsed
    **/
    public boolean parse(String rcds[], boolean ignoreChecksum)
    {
        if (!ListTools.isEmpty(rcds)) {
            boolean rtn = true;
            for (int i = 0; i < rcds.length; i++) {
                if (!this.parse(rcds[i], ignoreChecksum)) {
                    rtn = false;
                }
            }
            return rtn;
        } else {
            return false;
        }
    }

    /**
    *** Parses a NMEA-0183 record
    *** @param rcd the NMEA-0183 record to parse
    *** @param ignoreChecksum True to ignore the terminating checksum
    *** @return True if this record was successfully parsed
    **/
    public boolean parse(String rcd, boolean ignoreChecksum)
    {
        
        /* pre-validate */
        if (rcd == null) {
            Print.logError("Null record specified");
            return false;
        } else
        if (!rcd.startsWith("$")) {
            Print.logError("Invalid record (must begin with '$'): " + rcd);
            return false;
        }
        
        /* valid checksum? */
        if (ignoreChecksum) {
            this.validChecksum = true;
        } else {
            this.validChecksum = this._hasValidChecksum(rcd);
            if (!this.validChecksum) {
                Print.logError("Invalid Checksum: " + rcd);
                return false;
            }
        }
        
        /* parse into fields */
        String fld[] = StringTools.parseString(rcd, ',');
        if ((fld == null) || (fld.length < 1)) {
            Print.logError("Insufficient fields: " + rcd);
            return false;
        }
        
        /* parse record type */
        this.fieldMask = 0L;
        if (fld[0].equals(DNAME_GPRMC)) {
            this.parsedRcdTypes |= TYPE_GPRMC;
            this.lastRcdType = fld[0];
            this.fieldMask |= FIELD_RECORD_TYPE;
            return this._parse_GPRMC(fld);
        } else
        if (fld[0].equals(DNAME_GPGGA)) {
            this.parsedRcdTypes |= TYPE_GPGGA;
            this.lastRcdType = fld[0];
            this.fieldMask |= FIELD_RECORD_TYPE;
            return this._parse_GPGGA(fld);
        } else
        if (fld[0].equals(DNAME_GPVTG)) {
            this.parsedRcdTypes |= TYPE_GPVTG;
            this.lastRcdType = fld[0];
            this.fieldMask |= FIELD_RECORD_TYPE;
            return this._parse_GPVTG(fld); // speed/heading
        } else
        if (fld[0].equals(DNAME_GPZDA)) {
            this.parsedRcdTypes |= TYPE_GPZDA;
            this.lastRcdType = fld[0];
            this.fieldMask |= FIELD_RECORD_TYPE;
            return this._parse_GPZDA(fld);
        } else {
            Print.logError("Record not supported: " + rcd);
            return false;
        }
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a $GPRMC record has been parsed
    **/
    public boolean hasGPRMC()
    {
        return ((this.parsedRcdTypes | TYPE_GPRMC) != 0L);
    }
    
    /* parse "$GPRMC" */
    private boolean _parse_GPRMC(String fld[])
    {
        // $GPRMC - Recommended Minimum Specific GPS/TRANSIT Data
        // Format 1:
        // $GPRMC,025423.494,A,3709.0642,N,14207.8315,W,7.094,108.52,200505,13.1,E*12,
        // $GPRMC,    1     ,2,    3    ,4,     5    ,6,  7  ,  8   ,  9   ,  A ,B*MM,E
        //      1   UTC time of position HHMMSS
        //      2   validity of the fix ("A" = valid, "V" = invalid)
        //      3   current latitude in ddmm.mm format
        //      4   latitude hemisphere ("N" = northern, "S" = southern)
        //      5   current longitude in dddmm.mm format
        //      6   longitude hemisphere ("E" = eastern, "W" = western)
        //      7   speed in knots
        //      8   true course in degrees
        //      9   date in DDMMYY format
        //      A   magnetic variation in degrees
        //      B   direction of magnetic variation ("E" = east, "W" = west)
        //      MM  checksum
        //      E   extra data (may not be present)
        // Format 2:
        // $GPRMC,025423.494,A,3709.0642,N,14207.8315,W,7.094,108.52,200505,13.1,E,A*71
        // $GPRMC,    1     ,2,    3    ,4,     5    ,6,  7  ,  8   ,  9   ,  A ,B,C*MM,E
        //      C   Mode indicator, (A=Autonomous, D=Diff, E=Est, N=Not valid) 

        /* valid number of fields? */
        if (fld.length < 10) {
            return false;
        }
        
        /* valid GPS? */
        boolean validGPS = false;
        if (fld[2].equals("A")) {
            // vAlid
            validGPS = true;
        } else
        if (this.getIgnoreInvalidGpsFlag()) {
            // forced valid
            this.ignoredInvalidGPS = true;
            validGPS = true;
        } else
        if (fld[2].equals("V")) {
            // inValid
            validGPS = false;
        } else
        if (fld[2].equals("L")) {
            // staLe?
            Print.logWarn("Unexpected valid GPS fix indicator: " + fld[2]);
            validGPS = true;
        } else {
            // unknown
            Print.logWarn("Unexpected valid GPS fix indicator: " + fld[2]);
            validGPS = true;
        }
        this.fieldMask |= FIELD_VALID_FIX;

        /* date */
        this.fixtime = 0L; // calculated later
        boolean hasDate = false;
        if (!fld[9].equals("000000")) {
            this.ddmmyy = StringTools.parseLong(fld[9], 0L);
            this.fieldMask |= FIELD_DDMMYY;
            hasDate = true;
        }

        /* time */
        if (hasDate || !fld[1].equals("000000.000")) {
            // either we have a date, or the time is not "000000"
            this.hhmmss = StringTools.parseLong(fld[1], 0L);
            this.fieldMask |= FIELD_HHMMSS;
        }

        /* latitude, longitude, speed, heading */
        if (validGPS) {
            this.latitude  = Nmea0183.ParseLatitude (fld[3], fld[4],  90.0);
            this.longitude = Nmea0183.ParseLongitude(fld[5], fld[6], 180.0);
            if (!GeoPoint.isValid(this.latitude,this.longitude)) {
                validGPS        = false;
                this.latitude   = 0.0;
                this.longitude  = 0.0;
                this.ignoredInvalidGPS = false; // in case it was set true above
            } else {
                this.fieldMask |= FIELD_LATITUDE | FIELD_LONGITUDE;
                this.speedKnots = StringTools.parseDouble(fld[7], -1.0);
                this.heading    = StringTools.parseDouble(fld[8], -1.0);
                this.fieldMask |= FIELD_SPEED | FIELD_HEADING;
            }
        } else {
            this.latitude   = 0.0;
            this.longitude  = 0.0;
            this.speedKnots = 0.0;
            this.heading    = 0.0;
        }
        this.isValidGPS = validGPS;

        /* magnetic variation */
        if (fld.length > 11) {
            double magDeg = StringTools.parseDouble(fld[10], 0.0);
            this.magVariation = fld[11].equalsIgnoreCase("W")? -magDeg : magDeg;
        }

        /* extra data? */
        // Note: We've split the data record on commas, the "Extra Data" is re-assembled
        // by joining the remaining/unused fields together, separated again by commas.
        this.extraData = null;
        if (fld.length > 12) {
            int ePos = (fld[11].indexOf('*') >= 0)? 12 : ((fld.length > 13) && (fld[12].indexOf('*') >= 0))? 13 : 12;
            this.extraData = new String[fld.length - ePos];
            System.arraycopy(fld, ePos, this.extraData, 0, this.extraData.length);
            /*
            int eNdx = ePos + 1;
            if (fld.length == eNdx) {
                this.extraData = fld[ePos];
            } else {
                StringBuffer ed = new StringBuffer(fld[ePos]);
                for (int e = eNdx; e < fld.length; e++) {
                    ed.append(",").append(fld[e]);
                }
                this.extraData = ed.toString();
            }
            */
        }

        /* return valid GPS state */
        return validGPS;

    }
    
    // ----------------------------------------------------------------------------

    /* parse "$GPGGA" */
    private boolean _parse_GPGGA(String fld[])
    {
        // $GPGGA - Global Positioning System Fix Data
        // $GPGGA,015402.240,0000.0000,N,00000.0000,E,0,00,50.0,0.0,M,18.0,M,0.0,0000*4B
        // $GPGGA,025425.494,3509.0743,N,14207.6314,W,1,04,2.3,530.3,M,-21.9,M,0.0,0000*4D,
        // $GPGGA,    1     ,    2    ,3,     4    ,5,6,7 , 8 ,  9  ,A,  B  ,C, D , E  *MM,F
        //      1   UTC time of position HHMMSS
        //      2   current latitude in ddmm.mm format
        //      3   latitude hemisphere ("N" = northern, "S" = southern)
        //      4   current longitude in dddmm.mm format
        //      5   longitude hemisphere ("E" = eastern, "W" = western)
        //      6   (0=no fix, 1=GPS, 2=DGPS, 3=PPS?, 6=dead-reckoning)
        //      7   number of satellites (00-12)
        //      8   Horizontal Dilution of Precision
        //      9   Height above/below mean geoid (above mean sea level, not WGS-84 ellipsoid height)
        //      A   Unit of height, always 'M' meters
        //      B   Geoidal separation (add to #9 to get WGS-84 ellipsoid height)
        //      C   Unit of Geoidal separation (meters)
        //      D   Age of differential GPS
        //      E   Differential reference station ID (always '0000')
        //      F   Extra data (may not be present)

        /* valid number of fields? */
        if (fld.length < 14) {
            return false;
        }

        /* valid GPS? */
        boolean validGPS = !fld[6].equals("0");
        this.fieldMask |= FIELD_VALID_FIX;

        /* date */
        this.fixtime = 0L; // calculated later
        this.ddmmyy  = 0L; // we don't know the day

        /* time */
        this.hhmmss  = StringTools.parseLong(fld[1], 0L);
        this.fieldMask |= FIELD_HHMMSS;

        /* latitude, longitude, altitude */
        if (validGPS) {
            this.latitude  = Nmea0183.ParseLatitude (fld[2], fld[3],  90.0);
            this.longitude = Nmea0183.ParseLongitude(fld[4], fld[5], 180.0);
            if (!GeoPoint.isValid(this.latitude,this.longitude)) {
                validGPS        = false;
                this.latitude   = 0.0;
                this.longitude  = 0.0;
            } else {
                this.fieldMask |= FIELD_LATITUDE | FIELD_LONGITUDE;
                this.fixType    = StringTools.parseInt(fld[6], 1); // 1=GPS, 2=DGPS, 3=PPS?, ...
                this.numSats    = StringTools.parseInt(fld[7], 0);
                this.hdop       = StringTools.parseDouble(fld[8], 0.0);
                this.altitudeM  = StringTools.parseDouble(fld[9], 0.0); // meters
                this.fieldMask |= FIELD_FIX_TYPE | FIELD_NUMBER_SATS | FIELD_HDOP | FIELD_ALTITUDE;
            }
        } else {
            this.latitude   = 0.0;
            this.longitude  = 0.0;
            this.fixType    = 0;
            this.numSats    = 0;
            this.hdop       = 0.0;
            this.altitudeM  = 0.0;
        }
        this.isValidGPS = validGPS;

        /* extra data? */
        // Note: We've split the data record on commas, the "Extra Data" is re-assembled
        // by joining the remaining/unused fields together, separated again by commas.
        if (fld.length > 15) {
            int ePos = 15;
            this.extraData = new String[fld.length - ePos];
            System.arraycopy(fld, ePos, this.extraData, 0, this.extraData.length);
            /*
            if (fld.length == 16) {
                this.extraData = fld[15];
            } else {
                StringBuffer ed = new StringBuffer(fld[15]);
                for (int e = 16; e < fld.length; e++) {
                    ed.append(",").append(fld[e]);
                }
                this.extraData = ed.toString();
            }
            */
        } else {
            this.extraData = null;
        }

        /* return valid GPS state */
        return validGPS;
        
    }

    // ------------------------------------------------------------------------

    /* parse "$GPVTG" (speed/heading) */
    private boolean _parse_GPVTG(String fld[])
    {
        // $GPVTG - Track Made Good and Ground Speed
        // $GPVTG,229.86,T, ,M,0.00,N,0.0046,K*55
        // $GPVTG,   1  ,2,3,4, 5  ,6,  7   ,8*MM
        //      1   True course over ground, degrees
        //      2   "T" ("True" course)
        //      3   Magnetic course over ground, degrees
        //      4   "M" ("Magnetic" course)
        //      5   Speed over ground in Knots
        //      6   "N" ("Knots")
        //      7   Speed over ground in KM/H
        //      8   "K" ("KM/H")

        /* valid number of fields? */
        if (fld.length < 3) {
            return false;
        }
        
        /* loop through values */
        for (int i = 1; (i + 1) < fld.length; i += 2) {
            if (fld[i+1].equals("T")) { // True course
                this.heading = StringTools.parseDouble(fld[i], -1.0);
                this.fieldMask |= FIELD_HEADING;
            } else
            if (fld[i+1].equals("N")) { // Knots
                this.speedKnots = StringTools.parseDouble(fld[i], -1.0);
                this.fieldMask |= FIELD_SPEED;
            } else
            if (fld[i+1].equals("K")) { // KPH
                double kph = StringTools.parseDouble(fld[i], -1.0);
                this.speedKnots = (kph >= 0.0)? (kph * KNOTS_PER_KILOMETER) : -1.0;
                this.fieldMask |= FIELD_SPEED;
            }
        }

        /* success */
        return true;
        
    }

    // ------------------------------------------------------------------------

    /* parse "$GPZDA" */
    private boolean _parse_GPZDA(String fld[])
    {
        // $GPZDA - UTC Date/Time and Local Time Zone Offset
        // $GPZDA,125653.00,13,09,2007,00,00*6E 
        // $GPZDA,   1     , 2, 3,  4 , 5, 6*MM
        //      1   UTC hhmmss.ss
        //      2   Day: 01..31
        //      3   Month: 01..12
        //      4   Year
        //      5   Local zone hours description: -13..00..+13 hours
        //      6   Local zone minutes description (same sign as hours)

        /* valid number of fields? */
        if (fld.length < 5) {
            return false;
        }

        /* parse date */
        this.fixtime = 0L; // calculated later
        long day     = StringTools.parseLong(fld[2], 0L) % 100L;
        long month   = StringTools.parseLong(fld[3], 0L) % 100L;
        long year    = StringTools.parseLong(fld[4], 0L) % 10000L;
        this.ddmmyy  = (day * 10000L) + (month * 100L) + (year % 100L);
        this.fieldMask |= FIELD_DDMMYY;

        /* parse time */
        this.hhmmss  = StringTools.parseLong(fld[1], 0L);
        this.fieldMask |= FIELD_HHMMSS;
        
        /* success */
        return true;
        
    }


    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***     YY is year.
    *** @param hms Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***     and SS is second.
    *** @return Time in UTC seconds.
    **/
    private long _getUTCSeconds(long dmy, long hms)
    {
    
        /* time of day [TOD] */
        int    HH  = (int)((hms / 10000L) % 100L);
        int    MM  = (int)((hms /   100L) % 100L);
        int    SS  = (int)((hms /     1L) % 100L);
        long   TOD = (HH * 3600L) + (MM * 60L) + SS;
    
        /* current UTC day */
        long DAY;
        if (dmy > 0L) {
            // we have a valid date
            int    dd  = (int)((dmy / 10000L) % 100L);
            int    mm  = (int)((dmy /   100L) % 100L);
            int    yy  = (int)((dmy /     1L) % 100L) + 2000;
            long   yr  = ((long)yy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY        = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                         + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                         + (long)dd - 719469L;
        } else {
            // we don't have the day, so we need to figure out as close as we can what it should be.
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= TOD)? (tod - TOD) : (TOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (tod > TOD) {
                    // tod > TOD likely represents the next day
                    DAY++;
                } else {
                    // tod < TOD likely represents the previous day
                    DAY--;
                }
            }
        }

        /* return UTC seconds */
        long sec = DateTime.DaySeconds(DAY) + TOD;
        return sec;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses latitude given values from GPS device.
    *** @param s   Latitude String from GPS device in ddmm.mm format.
    *** @param d   Latitude hemisphere, "N" for northern, "S" for southern.
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         '90.0' if invalid latitude provided.
    **/
    public static double ParseLatitude(String s, String d)
    {
        return Nmea0183.ParseLatitude(s,d,90.0);
    }

    /**
    *** Parses latitude given values from GPS device.
    *** @param s   Latitude String from GPS device in ddmm.mm format.
    *** @param d   Latitude hemisphere, "N" for northern, "S" for southern.
    *** @param dft The default latitude, if the specified latitude cannot be parsed
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         'dft' if invalid latitude provided.
    **/
    public static double ParseLatitude(String s, String d, double dft)
    {
        double _lat = StringTools.parseDouble(s, 99999.0);
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
            return d.equalsIgnoreCase("S")? -lat : lat;
        } else {
            return 90.0; // invalid latitude
        }
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param s Longitude String from GPS device in ddmm.mm format.
    *** @param d Longitude hemisphere, "E" for eastern, "W" for western.
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         '180.0' if invalid longitude provided.
    **/
    public static double ParseLongitude(String s, String d)
    {
        return Nmea0183.ParseLongitude(s,d,180.0);
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param s Longitude String from GPS device in ddmm.mm format.
    *** @param d Longitude hemisphere, "E" for eastern, "W" for western.
    *** @param dft The default latitude, if the specified latitude cannot be parsed
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         'dft' if invalid longitude provided.
    **/
    public static double ParseLongitude(String s, String d, double dft)
    {
        double _lon = StringTools.parseDouble(s, 99999.0);
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            return d.equalsIgnoreCase("W")? -lon : lon;
        } else {
            return dft; // invalid longitude
        }
    }

    // ------------------------------------------------------------------------

    /**
    * Checks if NMEA-0183 formatted String has valid checksum by calculating the
    * checksum of the payload and comparing that to the received checksum.
    * @param str NMEA-0183 formatted String to be checked.
    * @return true if checksum is valid, false otherwise.
    */
    private boolean _hasValidChecksum(String str)
    {
        int c = str.indexOf("*");
        if (c < 0) {
            // does not contain a checksum char
            return false;
        }
        String chkSum = str.substring(c + 1);
        byte cs[] = StringTools.parseHex(chkSum,null);
        if ((cs == null) || (cs.length != 1)) {
            // invalid checksum hex length
            return false;
        }
        int calcSum = Nmea0183.calcXORChecksum(str,false);
        boolean isValid = (calcSum == ((int)cs[0] & 0xFF));
        if (!isValid) { Print.logWarn("Expected checksum: 0x" + StringTools.toHexString(calcSum,8)); }
        return isValid;
    }

    /**
    *** Calculates/Returns the checksum for a NMEA-0183 formatted String
    *** @param str NMEA-0183 formatted String to be checksummed.
    *** @return Checksum computed from input.
    **/
    public static int calcXORChecksum(String str, boolean includeAll)
    {
        byte b[] = StringTools.getBytes(str);
        if (b == null) {

            /* no bytes */
            return -1;

        } else {

            int cksum = 0, s = 0;

            /* skip leading '$' */
            if (!includeAll && (b.length > 0) && (b[0] == '$')) { 
                s++; 
            }

            /* calc checksum */
            for (; s < b.length; s++) {
                if (!includeAll && (b[s] == '*')) { break; }
                if ((b[s] == '\r') || (b[s] == '\n')) { break; }
                cksum = (cksum ^ b[s]) & 0xFF;
            }

            /* return checksum */
            return cksum;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        
        /* parse record */
        if (RTConfig.hasProperty("parse")) {
            String gprmc = RTConfig.getString("parse","");
            if (!gprmc.startsWith("$")) {
                gprmc = "$" + gprmc;
            }
            Nmea0183 n = new Nmea0183(gprmc, true); // ignore checksum
            Print.sysPrintln("NMEA-0183: \n" + n);
            System.exit(0);
        }
        
        /* calculate checksum */
        if (RTConfig.hasProperty("xor")) {
            String cksumStr = RTConfig.getString("xor","");
            int cksum = Nmea0183.calcXORChecksum(cksumStr,true);
            Print.sysPrintln("Checksum: " + StringTools.toHexString(cksum,8));
            System.exit(0);
        }
        
        /* test */
        if (RTConfig.hasProperty("test")) {
            Nmea0183 n;

            String gprmc = "$GPRMC,080701.00,A,3128.7540,N,14257.6714,W,27.6,107.5,180607,13.1,E,A*2D";
            n = new Nmea0183(gprmc);
            Print.sysPrintln("$GPRMC   : " + gprmc);
            Print.sysPrintln("      ==>: " + n.toGPRMC());
            Print.sysPrintln("NMEA-0183: \n" + n);
            
            String gpgga = "$GPGGA,025425.494,3509.0743,N,14207.6314,W,1,04,2.3,530.3,M,-21.9,M,0.0,0000*45";
            n = new Nmea0183(gpgga);
            Print.sysPrintln("NMEA-0183: \n" + n);
            
            n = new Nmea0183("$GPGGA,125653.00,3845.165,N,14228.961,W,1,05,,102.1331,M,,M,,*75");
            n.parse("$GPVTG,229.86,T,,M,0.00,N,0.0046,K*55");   // speed/heading
            n.parse("$GPZDA,125653.00,13,09,2007,00,00*6E");    // date/time
            Print.sysPrintln("NMEA-0183: \n" + n);
            
        }
        
    }
    
}
