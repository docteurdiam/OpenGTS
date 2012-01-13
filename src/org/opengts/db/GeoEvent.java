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
//  A container for a generic GPS event
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/18  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

/**
*** A container for a single generic GPS event
**/

public class GeoEvent
    implements Cloneable, GeoPointProvider
{

    // ------------------------------------------------------------------------

    /**
    *** Interface for GeoEvent handler call-backs
    **/
    public static interface GeoEventHandler
    {
        public int handleGeoEvent(GeoEvent gev);
    }
    
    // ------------------------------------------------------------------------

    /* standard event field types */
    public  static final String KEY_mobileID    = "mobileID";
    
    public  static final String KEY_accountID   = EventData.FLD_accountID;
    public  static final String KEY_account     = "account";
    
    public  static final String KEY_deviceID    = EventData.FLD_deviceID;
    public  static final String KEY_device      = "device";

    public  static final String KEY_geozoneID   = EventData.FLD_geozoneID;
    public  static final String KEY_geozone     = "geozone";

    public  static final String KEY_timestamp   = EventData.FLD_timestamp;
    public  static final String KEY_statusCode  = EventData.FLD_statusCode;
    public  static final String KEY_latitude    = EventData.FLD_latitude;
    public  static final String KEY_longitude   = EventData.FLD_longitude;
    public  static final String KEY_geoPoint    = "geoPoint";
    public  static final String KEY_speedKPH    = EventData.FLD_speedKPH;
    public  static final String KEY_heading     = EventData.FLD_heading;
    public  static final String KEY_altitude    = EventData.FLD_altitude;
    public  static final String KEY_odometerKM  = EventData.FLD_odometerKM;

    // ------------------------------------------------------------------------

    private Map<String,Object>  fieldValues     = null;

    /**
    *** Constructor
    **/
    public GeoEvent()
    {
        this.fieldValues = new HashMap<String,Object>();
    }

    /**
    *** Copy Constructor
    **/
    public GeoEvent(GeoEvent other)
    {
        this();
        if (other != null) {
            this.fieldValues.putAll(other.getFieldValues());
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Retruns a clone of this GeoEvent instance
    *** @return A clone of this GeoEvent
    **/
    public Object clone()
    {
        return new GeoEvent(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[" + new DateTime(this.getTimestamp()) + "] ");
        sb.append(StringTools.format(this.getLatitude(),"0.00000"));
        sb.append("/");
        sb.append(StringTools.format(this.getLongitude(),"0.00000"));
        sb.append("  ");
        sb.append(StringTools.format(this.getAltitudeMeters(),"0") + " m");
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Gets the field keys
    **/
    public Set<String> getFieldKeys()
    {
        return this.fieldValues.keySet();
    }

    /**
    *** Gets the field values
    **/
    public Map<String,Object> getFieldValues()
    {
        return this.fieldValues;
    }
 
    /**
    *** Sets a field value
    **/
    public void setFieldValue(String key, Object val)
    {
        if (!StringTools.isBlank(key)) {
            Map<String,Object> fv = this.getFieldValues();
            if (val != null) {
                fv.put(key, val);
            } else {
                fv.remove(key);
            }
        }
    }
    
    /**
    *** Gets a Object field value
    **/
    public Object getFieldValue(String key, Object dft)
    {
        Object fv = (key != null)? this.getFieldValues().get(key) : null;
        if (fv != null) {
            return (dft instanceof String)? fv.toString() : fv;
        } else {
            return dft;
        }
    }

    /**
    *** Returns true if the field value has been defined
    **/
    public boolean hasFieldValue(String key)
    {
        return this.fieldValues.containsKey(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets a Integer field value
    **/
    public void setFieldValue(String key, int val)
    {
        this.setFieldValue(key, (Object)(new Integer(val)));
    }

    /**
    *** Gets a Integer field value
    **/
    public int getFieldValue(String key, int dft)
    {
        Object fv = this.getFieldValue(key,null);
        return (fv instanceof Number)? ((Number)fv).intValue() : dft;
    }

    /**
    *** Sets a Long field value
    **/
    public void setFieldValue(String key, long val)
    {
        this.setFieldValue(key, (Object)(new Long(val)));
    }

    /**
    *** Gets a Long field value
    **/
    public long getFieldValue(String key, long dft)
    {
        Object fv = this.getFieldValue(key,null);
        return (fv instanceof Number)? ((Number)fv).longValue() : dft;
    }

    /**
    *** Sets a Double field value
    **/
    public void setFieldValue(String key, double val)
    {
        this.setFieldValue(key, (Object)(new Double(val)));
    }

    /**
    *** Gets a Double field value
    **/
    public double getFieldValue(String key, double dft)
    {
        Object fv = this.getFieldValue(key,null);
        return (fv instanceof Number)? ((Number)fv).doubleValue() : dft;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Sets the MobileID
    **/
    public void setMobileID(String mobileID)
    {
        this.setFieldValue(KEY_mobileID, mobileID);
    }
    
    /** 
    *** Gets the MobileID
    **/
    public String getMobileID()
    {
        return (String)this.getFieldValue(KEY_mobileID, null);
    }

    /**
    *** Returns true if an MobileID has been defined
    **/
    public boolean hasMobileID()
    {
        return this.hasFieldValue(KEY_mobileID);
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the AccountID
    **/
    public void setAccountID(String accountID)
    {
        this.setFieldValue(KEY_accountID, accountID);
        if (StringTools.isBlank(accountID)) {
            this.setAccount(null);
            this.setDevice(null);
            this.setGeozone(null);
        } else {
            Account acct = this.getAccount();
            if ((acct != null) && !accountID.equals(acct.getAccountID())) {
                this.setAccount(null);
                this.setDevice(null);
                this.setGeozone(null);
            }
        }
    }
    
    /** 
    *** Gets the AccountID
    **/
    public String getAccountID()
    {
        return (String)this.getFieldValue(KEY_accountID, null);
    }

    /**
    *** Returns true if an AccountID has been defined
    **/
    public boolean hasAccountID()
    {
        return this.hasFieldValue(KEY_accountID);
    }
    
    /**
    *** Sets the Account
    **/
    public void setAccount(Account account)
    {
        this.setFieldValue(KEY_account, account);
        if (account != null) {
            this.setFieldValue(KEY_accountID, account.getAccountID());
        }
    }
    
    /**
    *** Gets the Account
    **/
    public Account getAccount()
    {
        return (Account)this.getFieldValue(KEY_account, null);
    }

    /**
    *** Returns true if an Account has been defined
    **/
    public boolean hasAccount()
    {
        return this.hasFieldValue(KEY_account);
    }

    // ------------------------------------------------------------------------
    
    /** 
    *** Sets the DeviceID
    **/
    public void setDeviceID(String deviceID)
    {
        this.setFieldValue(KEY_deviceID, deviceID);
        if (StringTools.isBlank(deviceID)) {
            this.setDevice(null);
        } else {
            Device dev = this.getDevice();
            if ((dev != null) && !deviceID.equals(dev.getDeviceID())) {
                this.setDevice(null);
            }
        }
    }
    
    /** 
    *** Gets the DeviceID
    **/
    public String getDeviceID()
    {
        return (String)this.getFieldValue(KEY_deviceID, null);
    }

    /**
    *** Returns true if a DeviceID has been defined
    **/
    public boolean hasDeviceID()
    {
        return this.hasFieldValue(KEY_deviceID);
    }
    
    /**
    *** Sets the Device
    **/
    public void setDevice(Device device)
    {
        this.setFieldValue(KEY_device, device);
        if (device != null) {
            this.setFieldValue(KEY_accountID, device.getAccountID());
            this.setFieldValue(KEY_deviceID , device.getDeviceID());
        }
    }
    
    /**
    *** Gets the Device
    **/
    public Device getDevice()
    {
        return (Device)this.getFieldValue(KEY_device, null);
    }

    /**
    *** Returns true if a Device has been defined
    **/
    public boolean hasDevice()
    {
        return this.hasFieldValue(KEY_device);
    }

    // ------------------------------------------------------------------------
    
    /** 
    *** Sets the GeozoneID
    **/
    public void setGeozoneID(String geozoneID)
    {
        this.setFieldValue(KEY_geozoneID, geozoneID);
        if (StringTools.isBlank(geozoneID)) {
            this.setGeozone(null);
        } else {
            Geozone geoz = this.getGeozone();
            if ((geoz != null) && !geozoneID.equals(geoz.getGeozoneID())) {
                this.setGeozone(null);
            }
        }
    }
    
    /** 
    *** Gets the GeozoneID
    **/
    public String getGeozoneID()
    {
        return (String)this.getFieldValue(KEY_geozoneID, null);
    }

    /**
    *** Returns true if a GeozoneID has been defined
    **/
    public boolean hasGeozoneID()
    {
        return this.hasFieldValue(KEY_geozoneID);
    }
    
    /**
    *** Sets the Geozone
    **/
    public void setGeozone(Geozone geozone)
    {
        this.setFieldValue(KEY_geozone, geozone);
        if (geozone != null) {
            this.setFieldValue(KEY_geozoneID, geozone.getGeozoneID());
        }
    }
    
    /**
    *** Gets the Geozone
    **/
    public Geozone getGeozone()
    {
        return (Geozone)this.getFieldValue(KEY_geozone, null);
    }

    /**
    *** Returns true if a Geozone has been defined
    **/
    public boolean hasGeozone()
    {
        return this.hasFieldValue(KEY_geozone);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the timestamp
    **/
    public void setTimestamp(long ts)
    {
        this.setFieldValue(KEY_timestamp, ((ts > 0L)? ts : 0L));
    }
    
    /**
    *** Gets the timestamp
    **/
    public long getTimestamp()
    {
        return this.getFieldValue(KEY_timestamp, 0L);
    }

    /**
    *** Returns true if a timestamp has been defined
    **/
    public boolean hasTimestamp()
    {
        return this.hasFieldValue(KEY_timestamp);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the StatusCode
    **/
    public void setStatusCode(int sc)
    {
        this.setFieldValue(KEY_statusCode, ((sc > 0)? sc : 0));
    }
   
    /**
    *** Gets the StatusCode
    **/
    public int getStatusCode()
    {
        return this.getFieldValue(KEY_statusCode, 0);
    }

    /**
    *** Returns true if a StatusCode has been defined
    **/
    public boolean hasStatusCode()
    {
        return this.hasFieldValue(KEY_statusCode);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the GeoPoint
    **/
    public void setGeoPoint(double lat, double lon)
    {
        this.setFieldValue(KEY_latitude , lat);
        this.setFieldValue(KEY_longitude, lon);
        this.setFieldValue(KEY_geoPoint , null);
    }

    /**
    *** Sets the GeoPoint
    **/
    public void setGeoPoint(GeoPoint gp)
    {
        if (gp != null) {
            this.setFieldValue(KEY_latitude , gp.getLatitude());
            this.setFieldValue(KEY_longitude, gp.getLongitude());
            this.setFieldValue(KEY_geoPoint , gp);
        } else {
            this.setFieldValue(KEY_latitude , null);
            this.setFieldValue(KEY_longitude, null);
            this.setFieldValue(KEY_geoPoint , null);
        }
    }
    
    /**
    *** Returns true if the current GeoPoint is valie
    **/
    public boolean isGeoPointValid()
    {
        if (this.hasGeoPoint()) {
            return GeoPoint.isValid(this.getLatitude(),this.getLongitude());
        } else {
            return false;
        }
    }

    /**
    *** Sets the latitude
    **/
    public void setLatitude(double lat)
    {
        this.setFieldValue(KEY_latitude , lat);
        this.setFieldValue(KEY_geoPoint , null);
    }

    /**
    *** Gets the latitude
    **/
    public double getLatitude()
    {
        return this.getFieldValue(KEY_latitude, 0.0);
    }

    /**
    *** Returns true if a latitude has been defined
    **/
    public boolean hasLatitude()
    {
        return this.hasFieldValue(KEY_latitude);
    }

    /**
    *** Sets the longitude
    **/
    public void setLongitude(double lon)
    {
        this.setFieldValue(KEY_longitude, lon);
        this.setFieldValue(KEY_geoPoint , null);
    }

    /**
    *** Gets the longitude
    **/
    public double getLongitude()
    {
        return this.getFieldValue(KEY_longitude, 0.0);
    }

    /**
    *** Returns true if a longitude has been defined
    **/
    public boolean hasLongitude()
    {
        return this.hasFieldValue(KEY_longitude);
    }

    /**
    *** Gets the GeoPoint
    **/
    public GeoPoint getGeoPoint()
    {
        GeoPoint gp = (GeoPoint)this.getFieldValue(KEY_geoPoint, null);
        if (gp == null) {
            gp = new GeoPoint(this.getLatitude(),this.getLongitude());
            this.setFieldValue(KEY_geoPoint, gp);
        }
        return gp;
    }

    /**
    *** Returns true if a latitude/longitude has been defined
    **/
    public boolean hasGeoPoint()
    {
        return this.hasLatitude() && this.hasLongitude();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Speed
    **/
    public void setSpeedKPH(double kph)
    {
        this.setFieldValue(KEY_speedKPH, ((kph >= 0.0)? kph : 0.0));
    }
    
    /**
    *** Gets the Speed
    **/
    public double getSpeedKPH()
    {
        return this.getFieldValue(KEY_speedKPH, 0.0);
    }

    /**
    *** Returns true if a speed has been defined
    **/
    public boolean hasSpeedKPH()
    {
        return this.hasFieldValue(KEY_speedKPH);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Heading
    **/
    public void setHeading(double deg)
    {
        this.setFieldValue(KEY_heading, ((deg >= 0.0)? deg : 0.0));
    }
    
    /**
    *** Gets the Heading
    **/
    public double getHeading()
    {
        return this.getFieldValue(KEY_heading, 0.0);
    }

    /**
    *** Returns true if a speed has been defined
    **/
    public boolean hasHeading()
    {
        return this.hasFieldValue(KEY_heading);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Altitude
    **/
    public void setAltitudeMeters(double altM)
    {
        this.setFieldValue(KEY_altitude, altM);
    }
    
    /**
    *** Gets the Altitude
    **/
    public double getAltitudeMeters()
    {
        return this.getFieldValue(KEY_altitude, 0.0);
    }

    /**
    *** Returns true if a speed has been defined
    **/
    public boolean hasAltitudeMeters()
    {
        return this.hasFieldValue(KEY_altitude);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Odometer
    **/
    public void setOdometerKM(double km)
    {
        this.setFieldValue(KEY_odometerKM, km);
    }
    
    /**
    *** Gets the Odometer
    **/
    public double getOdometerKM()
    {
        return this.getFieldValue(KEY_odometerKM, 0.0);
    }

    /**
    *** Returns true if an Odometer has been defined
    **/
    public boolean hasOdometerKM()
    {
        return this.hasFieldValue(KEY_odometerKM);
    }

    // ------------------------------------------------------------------------

}
