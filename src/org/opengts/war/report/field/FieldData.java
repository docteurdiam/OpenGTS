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
//  2007/01/10  Martin D. Flynn
//     -Added methods to sort FieldData lists by the device description
// ----------------------------------------------------------------------------
package org.opengts.war.report.field;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.report.*;

public class FieldData
    implements CSSRowClass
{

    // ------------------------------------------------------------------------

    public static final String KEY_ACCOUNT      = "$account";
    public static final String KEY_DEVICE       = "$device";
    public static final String KEY_DEVICE_DESC  = "$deviceDesc";
    public static final String KEY_DEVICE_VIN   = "$deviceVIN";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                  cssClass    = null;
    private DBDataRow.RowType       rowType     = DBDataRow.RowType.DETAIL;
    private HashMap<String,Object>  fieldValues = null;

    public FieldData()
    {
        super();
        this.fieldValues = new HashMap<String,Object>();
    }

    // ------------------------------------------------------------------------
    
    public void setRowType(DBDataRow.RowType rt)
    {
        this.rowType = (rt != null)? rt : DBDataRow.RowType.DETAIL;
    }
    
    public DBDataRow.RowType getRowType()
    {
        return (this.rowType != null)? this.rowType : DBDataRow.RowType.DETAIL;
    }

    // ------------------------------------------------------------------------

    public boolean hasCssClass()
    {
        return !StringTools.isBlank(this.getCssClass());
    }

    public String getCssClass()
    {
        return this.cssClass;
    }

    public void setCssClass(String rowClass)
    {
        this.cssClass = rowClass;
    }

    // ------------------------------------------------------------------------

    public void setValue(String key, Object val)
    {
        this.fieldValues.put(key, val);
    }

    public Object getValue(String key, Object dft)
    {
        Object val = this.fieldValues.get(key);
        return (val != null)? val : dft;
    }

    public Object getValue(String key)
    {
        return this.getValue(key, null);
    }
    
    public boolean hasValue(String key)
    {
        return this.fieldValues.containsKey(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** This method provide this instance to tweak (colorise, etc) the returned 
    *** value from the FieldLayout class.
    *** @param key     The field key that the value represents
    *** @param rtnVal  The value to filter (is either a String or ColumnValue instance)
    *** @return The returned value (default is to return the specified value unfiltered)
    **/
    public Object filterReturnedValue(String key, Object rtnVal)
    {
        return rtnVal;
    }
    
    // ------------------------------------------------------------------------

    /* return the account id associated with this data record */
    public String getAccountID()
    {
        // may return "" if undefined
        String acctID = this.getString(FieldLayout.DATA_ACCOUNT_ID, null);
        if (acctID != null) {
            return acctID;
        } else {
            Account acct = this.getAccount(null);
            if (acct != null) {
                return acct.getAccountID();
            } else {
                return "";
            }
        }
    }

    /* set the account associated with this record */
    public void setAccount(Account account)
    {
        this.setValue(KEY_ACCOUNT, account);
        if (account != null) {
            this.setValue(FieldLayout.DATA_ACCOUNT_ID, account.getAccountID());
        }
    }

    /* get the account associated with this record (or the default if not defined) */
    public Account getAccount(Account dft)
    {
        Object val = this.getValue(KEY_ACCOUNT,null); // this.fieldValues.get(KEY_ACCOUNT);
        if (val instanceof Account) {
            // we have an account
            return (Account)val;
        } else
        if (dft != null) {
            // a default account has been specified
            return dft;
        } else {
            // no account, and no default
            Device dev = this.getDevice();
            if (dev != null) {
                // obtain the account from the device
                Account acct = dev.getAccount();
                this.setAccount(acct);
                return acct;
            } else {
                // no account, no device, no default, return null
                return null;
            }
        }
    }

    /* get the account associated with this record (or null if not defined) */
    public Account getAccount()
    {
        return this.getAccount(null);
    }
    
    // ------------------------------------------------------------------------

    /* return the device id associated with this data record */
    public String getDeviceID()
    {
        // may return "" if undefined
        String devID = this.getString(FieldLayout.DATA_DEVICE_ID, null);
        if (devID != null) {
            return devID;
        } else {
            Device dev = this.getDevice(null);
            if (dev != null) {
                return dev.getDeviceID();
            } else {
                return "";
            }
        }
    }

    /* set the device associated with this data record */
    public void setDevice(Device device)
    {
        this.setValue(KEY_DEVICE, device);
        if (device != null) {
            this.setValue(FieldLayout.DATA_DEVICE_ID, device.getDeviceID());
        }
    }
    
    /* return the cached device (or the default device if not defined) */
    public Device getDevice(Device dft)
    {
        Object val = this.getValue(KEY_DEVICE,null); // this.fieldValues.get(KEY_DEVICE);
        return (val instanceof Device)? (Device)val : dft;
    }

    /* return the cached device (or null if not defined) */
    public Device getDevice()
    {
        return this.getDevice(null);
    }
    
    /* return the device description */
    public String getDeviceDescription()
    {
        Object devDesc = this.getValue(KEY_DEVICE_DESC,null); // this.fieldValues.get(KEY_DEVICE_DESC);
        if (devDesc != null) {
            return (String)devDesc;
        } else {
            Device dev = this.getDevice(null);
            if (dev != null) {
                String desc = dev.getDescription();
                this.setValue(KEY_DEVICE_DESC, desc);
                return desc;
            } else {
                // default to returning the device ID (if defined)
                return this.getString(FieldLayout.DATA_DEVICE_ID, "");
            }
        } 
    }
    
    /* return the device description */
    public String getDeviceVIN()
    {
        Object devVIN = this.getValue(KEY_DEVICE_VIN,null); // this.fieldValues.get(KEY_DEVICE_VIN);
        if (devVIN != null) {
            return (String)devVIN;
        } else {
            Device dev = this.getDevice(null);
            if (dev != null) {
                String vin = dev.getDeviceVIN();
                this.setValue(KEY_DEVICE_VIN, vin);
                return vin;
            } else {
                // default to returning the device ID (if defined)
                return this.getString(FieldLayout.DATA_DEVICE_ID, "");
            }
        } 
    }

    // ------------------------------------------------------------------------

    public void setGeoPoint(String key, GeoPoint gp)
    {
        this.setValue(key, gp);
    }

    public GeoPoint getGeoPoint(String key, GeoPoint dft)
    {
        Object val = this.getValue(key,null);
        return (val instanceof GeoPoint)? (GeoPoint)val : dft;
    }

    public GeoPoint getGeoPoint(String key)
    {
        return this.getGeoPoint(key, null);
    }

    // ------------------------------------------------------------------------

    public void setValue(String key, boolean val)
    {
        this.setValue(key, new Boolean(val));
    }
    
    public void setBoolean(String key, boolean val)
    {
        this.setValue(key, new Boolean(val));
    }

    public boolean getBoolean(String key, boolean dft)
    {
        Object val = this.getValue(key,null);
        return StringTools.parseBoolean(val, dft);
    }

    public boolean getBoolean(String key)
    {
        return this.getBoolean(key, false);
    }

    // ------------------------------------------------------------------------

    public void setString(String key, String val)
    {
        this.setValue(key, val);
    }

    public String getString(String key, String dft)
    {
        Object val = this.getValue(key, null);
        return (val != null)? val.toString() : dft;
    }

    public String getString(String key)
    {
        return this.getString(key, "");
    }

    // ------------------------------------------------------------------------

    public void setValue(String key, int val)
    {
        this.setValue(key, new Integer(val));
    }

    public void setInt(String key, int val)
    {
        this.setValue(key, new Integer(val));
    }

    public int getInt(String key, int dft)
    {
        Object val = this.getValue(key,null);
        return (val instanceof Number)? ((Number)val).intValue() : dft;
    }

    public int getInt(String key)
    {
        return this.getInt(key, 0);
    }

    // ------------------------------------------------------------------------

    public void setValue(String key, long val)
    {
        this.setValue(key, new Long(val));
    }

    public void setLong(String key, long val)
    {
        this.setValue(key, new Long(val));
    }

    public long getLong(String key, long dft)
    {
        Object val = this.getValue(key,null); // this.fieldValues.get(key);
        return (val instanceof Number)? ((Number)val).longValue() : dft;
    }

    public long getLong(String key)
    {
        return this.getLong(key, 0L);
    }

    // ------------------------------------------------------------------------

    public void setValue(String key, double val)
    {
        this.setValue(key, new Double(val));
    }

    public void setDouble(String key, double val)
    {
        this.setValue(key, new Double(val));
    }

    public double getDouble(String key, double dft)
    {
        Object val = this.getValue(key,null); // this.fieldValues.get(key);
        return (val instanceof Number)? ((Number)val).doubleValue() : dft;
    }

    public double getDouble(String key)
    {
        return this.getDouble(key, 0.0);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* singleton instance of DeviceDescriptionComparator */
    private static Comparator<FieldData> devDescComparator = null;
    public static Comparator<FieldData> getDeviceDescriptionComparator()
    {
        if (devDescComparator == null) {
            devDescComparator = new DeviceDescriptionComparator(); // ascending
        }
        return devDescComparator;
    }

    /* sort by device description ascending */
    public static void sortByDeviceDescription(java.util.List<FieldData> fieldDataList) 
    {
        if (fieldDataList != null) {
            try {
                Collections.sort(fieldDataList, FieldData.getDeviceDescriptionComparator());
            } catch (Throwable th) { // ClassCastException, etc
                Print.logException("Invalid FieldData list", th);
            }
        }
    }

    /* Comparator for FieldData device descriptions */
    public static class DeviceDescriptionComparator
        implements Comparator<FieldData>
    {
        private boolean ascending = true;
        public DeviceDescriptionComparator() {
            this(true);
        }
        public DeviceDescriptionComparator(boolean ascending) {
            this.ascending  = ascending;
        }
        public int compare(FieldData o1, FieldData o2) {
            // assume we are comparing FieldData instances
            String D1 = o1.getDeviceDescription();
            String D2 = o2.getDeviceDescription();
            return this.ascending? D1.compareTo(D2) : D2.compareTo(D1);
        }
        public boolean equals(Object other) {
            if (other instanceof DeviceDescriptionComparator) {
                DeviceDescriptionComparator ddc = (DeviceDescriptionComparator)other;
                return (this.ascending == ddc.ascending);
            }
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

}
