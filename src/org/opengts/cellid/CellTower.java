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
//  2010/05/24  Martin D. Flynn
//      -Initial release
//  2011/06/16  Martin D. Flynn
//      -Added "GetLocation" interface.
//      -Moved from org.opengts.util
//  2011/07/01  Martin D. Flynn
//      -Moved GetLocation interface to MobileLocationProvider
// ----------------------------------------------------------------------------
package org.opengts.cellid;

import java.io.*;
import java.util.*;

import org.opengts.util.*;

import org.opengts.cellid.google.GoogleMobileService;

/**
*** A Container for Cell-Tower information
**/

public class CellTower
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String ARG_MCC              = "mcc";    // Mobile Country Code
    public static final String ARG_MNC              = "mnc";    // Mobile Network Code
    public static final String ARG_LAC              = "lac";    // Location Area Code
    public static final String ARG_CID              = "cid";    // Cell Tower ID
    public static final String ARG_TAV              = "tav";    // Timing Advance
    public static final String ARG_RAT              = "rat";    // Radio Access Technology
    public static final String ARG_RXLEV            = "rxlev";  // Reception Level
    public static final String ARG_ARFCN            = "arfcn";  // ARF Channel Number

    // ------------------------------------------------------------------------

    private String          cellTowerName           = null;
    private MobileLocation  cellLocation            = null;

    private int             cellTowerID             = -1;
    private int             radioAccessTechnology   = -1;
    private int             mobileCountryCode       = -1;
    private int             mobileNetworkCode       = -1;
    private int             locationAreaCode        = -1;
    private int             arfChannelNumber        = -1;
    private int             receptionLevel          = -1;
    private int             timingAdvance           = -1;

    /**
    *** Constructor
    **/
    public CellTower()
    {
        super();
        this._init(
            -1/*RAT*/, 
            -1/*MCC*/, 
            -1/*MNC*/, 
            -1/*TAV*/, 
            -1/*CID*/, 
            -1/*LAC*/, 
            -1/*ARFCN*/, 
            -1/*RXLEV*/
            );
    }

    /**
    *** Copy Constructor
    **/
    public CellTower(CellTower other)
    {
        super();
        if (other != null) {
            this._init(
                other.getRadioAccessTechnology(),
                other.getMobileCountryCode(),
                other.getMobileNetworkCode(),
                other.getTimingAdvance(),
                other.getCellTowerID(),
                other.getLocationAreaCode(),
                other.getAbsoluteRadioFrequencyChannelNumber(),
                other.getReceptionLevel()
                );
            this.setName(other.getName());
            if (other.hasMobileLocation()) {
                this.setMobileLocation(new MobileLocation(other.getMobileLocation()));
            }
        }
    }

    /**
    *** Constructor
    **/
    public CellTower(int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev)
    {
        super();
        this._init(
            -1/*RAT*/,
            mcc,
            mnc,
            tav,
            cid,
            lac,
            arfcn,
            rxlev
            );
    }

    /**
    *** Constructor
    **/
    public CellTower(int rat, int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev)
    {
        super();
        this._init(
            rat,
            mcc,
            mnc,
            tav,
            cid,
            lac,
            arfcn,
            rxlev
            );
    }

    /**
    *** Constructor
    **/
    public CellTower(int cid, int lac, int arfcn, int rxlev)
    {
        super();
        this._init(
            -1/*RAT*/, 
            -1/*MCC*/,
            -1/*MNC*/,
            -1/*TAV*/,
            cid,
            lac,
            arfcn,
            rxlev
            );
    }

    /**
    *** Constructor
    **/
    public CellTower(String cidStr)
    {
        this(new RTProperties(cidStr));
    }

    /**
    *** Constructor
    **/
    public CellTower(RTProperties cidp)
    {
        super();
        if (cidp != null) {
            int rat   = cidp.getInt(ARG_RAT  , -1);
            int mcc   = cidp.getInt(ARG_MCC  , -1);
            int mnc   = cidp.getInt(ARG_MNC  , -1);
            int tav   = cidp.getInt(ARG_TAV  , -1);
            int cid   = cidp.getInt(ARG_CID  , -1);
            int lac   = cidp.getInt(ARG_LAC  , -1);
            int arfcn = cidp.getInt(ARG_ARFCN, -1);
            int rxlev = cidp.getInt(ARG_RXLEV, -1);
            this._init(rat, mcc, mnc, tav, cid, lac, arfcn, rxlev);
        }
    }

    /**
    *** private init
    **/
    private void _init(int rat, int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev)
    {
        // --
        this.radioAccessTechnology  = rat;
        // --
        this.mobileCountryCode      = mcc;
        this.mobileNetworkCode      = mnc;
        this.timingAdvance          = tav;
        // --
        this.cellTowerID            = cid;
        this.locationAreaCode       = lac;
        this.arfChannelNumber       = arfcn;
        this.receptionLevel         = rxlev;
        // --
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this CellTower instance contains valid information
    **/
    public boolean isValid()
    {
        if (this.getCellTowerID() <= 0) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Cell Tower Name (if any)
    *** @param name The Cell Tower name
    **/
    public void setName(String name)
    {
        this.cellTowerName = !StringTools.isBlank(name)? name.trim() : null;
    }

    /** 
    *** Gets the Cell Tower Name (if available)
    *** @return The Cell Tower name (or null if not available)
    **/
    public String getName()
    {
        return this.cellTowerName;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Mobile location
    *** @param mobLoc The Mobile location
    **/
    public void setMobileLocation(MobileLocation mobLoc)
    {
        this.cellLocation = mobLoc;
    }

    /** 
    *** Sets the Mobile location
    *** @param mobLoc The Mobile location
    **/
    public void setMobileLocation(GeoPoint gp, double accuracy)
    {
        if (this.cellLocation != null) {
            this.cellLocation.setGeoPoint(gp);
            this.cellLocation.setAccuracy(accuracy);
        } else {
            this.cellLocation = new MobileLocation(gp, accuracy);
        }
    }

    /** 
    *** Gets the Mobile Location (if available)
    *** @return The Mobile Location (or null if not available)
    **/
    public MobileLocation getMobileLocation()
    {
        return this.cellLocation;
    }

    /** 
    *** Returns true if this instance has a Mobile Location
    *** @return True if this instance has a Mobile Location
    **/
    public boolean hasMobileLocation()
    {
        return (this.cellLocation != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Radio Access Technology code
    *** @param rat The Radio Access Technology code
    **/
    public void setRadioAccessTechnology(int rat)
    {
        this.radioAccessTechnology = rat;
    }

    /**
    *** Gets the Radio Access Technology code
    *** @return The Radio Access Technology code
    **/
    public int getRadioAccessTechnology()
    {
        return this.radioAccessTechnology;
    }

    /**
    *** Returns true if the Radio Access Technology code has been defined
    *** @return True if the Radio Access Technology code has been defined
    **/
    public boolean hasRadioAccessTechnology()
    {
        return (this.radioAccessTechnology >= 0);
    }

    /**
    *** Gets the Radio Access Technology code as a String
    *** @return The Radio Access Technology code as a String
    **/
    public String getRadioAccessTechnologyString()
    {
        return this.hasRadioAccessTechnology()?
            String.valueOf(this.getRadioAccessTechnology()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Mobile Country Code (MCC)
    *** @param mcc The Mobile Country Code (MCC)
    **/
    public void setMobileCountryCode(int mcc)
    {
        this.mobileCountryCode = mcc;
    }

    /**
    *** Gets the Mobile Country Code (MCC)
    *** @return The Mobile Country Code (MCC)
    **/
    public int getMobileCountryCode()
    {
        return this.mobileCountryCode;
    }

    /**
    *** Returns true if the Mobile Country Code has been defined
    *** @return True if the Mobile Country Code has been defined
    **/
    public boolean hasMobileCountryCode()
    {
        return (this.mobileCountryCode >= 0);
    }

    /**
    *** Gets the Mobile Country Code as a String
    *** @return The Mobile Country Code as a String
    **/
    public String getMobileCountryCodeString()
    {
        return this.hasMobileCountryCode()? 
            String.valueOf(this.getMobileCountryCode()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Mobile Network Code (MNC)
    *** @param mnc The Mobile Network Code (MNC)
    **/
    public void setMobileNetworkCode(int mnc)
    {
        this.mobileNetworkCode = mnc;
    }

    /**
    *** Gets the Mobile Network Code (MNC)
    *** @return The Mobile Network Code (MNC)
    **/
    public int getMobileNetworkCode()
    {
        return this.mobileNetworkCode;
    }

    /**
    *** Returns true if the Mobile Network Code has been defined
    *** @return True if the Mobile Network Code has been defined
    **/
    public boolean hasMobileNetworkCode()
    {
        return (this.mobileNetworkCode >= 0);
    }

    /**
    *** Gets the Mobile Network Code as a String
    *** @return The Mobile Network Code as a String
    **/
    public String getMobileNetworkCodeString()
    {
        return this.hasMobileNetworkCode()? 
            String.valueOf(this.getMobileNetworkCode()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Timing Advance
    *** @param tav The Timing Advance
    **/
    public void setTimingAdvance(int tav)
    {
        this.timingAdvance = tav;
    }

    /**
    *** Gets the Timing Advance
    *** @return The Timing Advance
    **/
    public int getTimingAdvance()
    {
        return this.timingAdvance;
    }

    /**
    *** Returns true if the Timing Advance has been defined
    *** @return True if the Timing Advance has been defined
    **/
    public boolean hasTimingAdvance()
    {
        return (this.timingAdvance >= 0);
    }

    /**
    *** Gets the Timing Advance as a String
    *** @return The Timing Advance as a String
    **/
    public String getTimingAdvanceString()
    {
        return this.hasTimingAdvance()? 
            String.valueOf(this.getTimingAdvance()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Cell Tower ID (CID)
    *** @param cid The Cell Tower ID
    **/
    public void setCellTowerID(int cid)
    {
        this.cellTowerID = cid;
    }

    /**
    *** Gets the Cell Tower ID (CID)
    *** @return The Cell Tower ID
    **/
    public int getCellTowerID()
    {
        return this.cellTowerID;
    }

    /**
    *** Returns true if the Cell Tower ID has been defined
    *** @return True if the Cell Tower ID has been defined
    **/
    public boolean hasCellTowerID()
    {
        return (this.cellTowerID >= 0);
    }

    /**
    *** Gets the Cell Tower ID as a String
    *** @return The Cell Tower ID as a String
    **/
    public String getCellTowerIDString()
    {
        return this.hasCellTowerID()? 
            String.valueOf(this.getCellTowerID()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Location Area Code (LAC)
    *** @param lac  The Location Area Code
    **/
    public void setLocationAreaCode(int lac)
    {
        this.locationAreaCode = lac;
    }

    /**
    *** Gets the Location Area Code (LAC)
    *** @return The Location Area Code
    **/
    public int getLocationAreaCode()
    {
        return this.locationAreaCode;
    }

    /**
    *** Returns true if the Location Area Code has been defined
    *** @return True if the Location Area Code has been defined
    **/
    public boolean hasLocationAreaCode()
    {
        return (this.locationAreaCode >= 0);
    }

    /**
    *** Gets the Location Area Code (LAC) as a String
    *** @return The Location Area Code (LAC) as a String
    **/
    public String getLocationAreaCodeString()
    {
        return this.hasLocationAreaCode()? 
            String.valueOf(this.getLocationAreaCode()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Absolute Radio Frequency Channel Number
    *** @param arfcn The Absolute Radio Frequency Channel Number
    **/
    public void setAbsoluteRadioFrequencyChannelNumber(int arfcn)
    {
        this.arfChannelNumber = arfcn;
    }

    /**
    *** Gets the Absolute Radio Frequency Channel Number
    *** @return The Absolute Radio Frequency Channel Number
    **/
    public int getAbsoluteRadioFrequencyChannelNumber()
    {
        return this.arfChannelNumber;
    }

    /**
    *** Returns true if the Absolute Radio Frequency Channel Number has been defined
    *** @return True if the Absolute Radio Frequency Channel Number has been defined
    **/
    public boolean hasAbsoluteRadioFrequencyChannelNumber()
    {
        return (this.arfChannelNumber >= 0);
    }

    /**
    *** Gets the Absolute Radio Frequency Channel Number as a String
    *** @return The Absolute Radio Frequency Channel Number as a String
    **/
    public String getAbsoluteRadioFrequencyChannelNumberString()
    {
        return this.hasAbsoluteRadioFrequencyChannelNumber()? 
            String.valueOf(this.getAbsoluteRadioFrequencyChannelNumber()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Reception Level
    *** @param rxlev The Reception Level
    **/
    public void setReceptionLevel(int rxlev)
    {
        this.receptionLevel = rxlev;
    }

    /**
    *** Gets the Reception Level
    *** @return The Reception Level
    **/
    public int getReceptionLevel()
    {
        return this.receptionLevel;
    }

    /**
    *** Returns true if the Reception Level has been defined
    *** @return True if the Reception Level has been defined
    **/
    public boolean hasReceptionLevel()
    {
        return (this.receptionLevel >= 0);
    }

    /**
    *** Gets the Reception Level as a String
    *** @return The Reception Level as a String
    **/
    public String getReceptionLevelString()
    {
        return this.hasReceptionLevel()? 
            String.valueOf(this.getReceptionLevel()) : 
            "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a RTProperties representation of this instance
    *** @return A RTProperties representation of this instance
    **/
    public RTProperties getRTProperties(RTProperties cidp)
    {
        if (cidp == null) { cidp = new RTProperties(); }
        // RAT
        if (this.hasRadioAccessTechnology()) {
            cidp.setInt(ARG_RAT  , this.getRadioAccessTechnology());
        } else {
            cidp.removeProperty(ARG_RAT);
        }
        // MCC
        if (this.hasMobileCountryCode()) {
            cidp.setInt(ARG_MCC  , this.getMobileCountryCode());
        } else {
            cidp.removeProperty(ARG_MCC);
        }
        // MNC
        if (this.hasMobileNetworkCode()) {
            cidp.setInt(ARG_MNC  , this.getMobileNetworkCode());
        } else {
            cidp.removeProperty(ARG_MNC);
        }
        // TAV
        if (this.hasTimingAdvance()) {
            cidp.setInt(ARG_TAV  , this.getTimingAdvance());
        } else {
            cidp.removeProperty(ARG_TAV);
        }
        // CID
        if (this.hasCellTowerID()) {
            cidp.setInt(ARG_CID  , this.getCellTowerID());
        } else {
            cidp.removeProperty(ARG_CID);
        }
        // LAC
        if (this.hasLocationAreaCode()) {
            cidp.setInt(ARG_LAC  , this.getLocationAreaCode());
        } else {
            cidp.removeProperty(ARG_LAC);
        }
        // ARFCN
        if (this.hasAbsoluteRadioFrequencyChannelNumber()) {
            cidp.setInt(ARG_ARFCN, this.getAbsoluteRadioFrequencyChannelNumber());
        } else {
            cidp.removeProperty(ARG_ARFCN);
        }
        // RXLEV
        if (this.hasReceptionLevel()) {
            cidp.setInt(ARG_RXLEV, this.getReceptionLevel());
        } else {
            cidp.removeProperty(ARG_RXLEV);
        }
        // MobileLocation
        if (this.hasMobileLocation()) {
            this.getMobileLocation().getRTProperties(cidp);
        } else {
            cidp.removeProperty(MobileLocation.ARG_GPS);
            cidp.removeProperty(MobileLocation.ARG_ACC);
        }
        // return
        return cidp;
    }

    /**
    *** Returns the String representation of this instance
    *** @return String representation of this instance
    **/
    public String toString()
    {
        return this.getRTProperties(null).toString();
    }
        
    // ------------------------------------------------------------------------

    /** 
    *** Returns true if the specified instance is equal to this insance
    *** (the MobileLocation is not tested for equality)
    *** @param other  The other instance
    *** @return True if equal, false otherwise
    **/
    public boolean equals(Object other)
    {
        if (other instanceof CellTower) {
            CellTower oct = (CellTower)other;
            if (this.cellTowerID            != oct.cellTowerID          ) { return false; }
            if (this.radioAccessTechnology  != oct.radioAccessTechnology) { return false; }
            if (this.mobileCountryCode      != oct.mobileCountryCode    ) { return false; }
            if (this.mobileNetworkCode      != oct.mobileNetworkCode    ) { return false; }
            if (this.locationAreaCode       != oct.locationAreaCode     ) { return false; }
            if (this.arfChannelNumber       != oct.arfChannelNumber     ) { return false; }
            if (this.receptionLevel         != oct.receptionLevel       ) { return false; }
            if (this.timingAdvance          != oct.timingAdvance        ) { return false; }
            return true;
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

}
