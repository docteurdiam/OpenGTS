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
//  2007/06/13  Martin D. Flynn
//     -Moved to package "org.opengts.war.report"
//     -Renamed 'DeviceList' to 'ReportDeviceList'
//  2009/09/23  Martin D. Flynn
//     -Fixed bug that could cause an "ConcurrentModificationException"
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public class ReportDeviceList
    extends DBRecord<ReportDeviceList> // not really a database table
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class DeviceHolder
    {
        private Account account    = null;
        private String  deviceID   = null;
        private String  deviceDesc = null;
        private Device  device     = null;
        public DeviceHolder(Account acct, String devID) {
            super();
            this.account   = acct;
            this.deviceID  = devID;
            this.device    = null;
        }
        public DeviceHolder(Device dev) {
            this(dev.getAccount(), dev.getDeviceID());
            this.device = dev;
        }
        public void setDevice(Device dev) {
            if ((dev != null) &&
                this.account.getAccountID().equals(dev.getAccountID()) &&
                this.deviceID.equals(dev.getDeviceID())) {
                this.device = dev;
            }
        }
        public String getDeviceID() {
            return this.deviceID;
        }
        public boolean hasDevice() {
            return (this.device != null);
        }
        public String getDeviceDescription() {
            if (this.deviceDesc == null) {
                try {
                    Device device = this.getDevice();
                    if (device != null) {
                        this.deviceDesc = device.getDescription();
                    } else {
                        this.deviceDesc = "";
                    }
                } catch (DBException dbe) {
                    this.deviceDesc = "";
                }
            }
            return this.deviceDesc;
        }
        public Device getDevice() throws DBException {
            if ((this.device == null) && (this.account != null) && (this.deviceID != null)) {
                this.device = Device.getDevice(this.account, this.deviceID); // may still be null
                if (this.device == null) {
                    // so we don't try again
                    this.account  = null;
                    this.deviceID = null;
                }
            }
            return this.device;
        }
    }
    
    private static class DeviceHolderComparator
        implements Comparator<DeviceHolder>
    {
        private boolean ascending = true;
        public DeviceHolderComparator() {
            this(true);
        }
        public DeviceHolderComparator(boolean ascending) {
            this.ascending  = ascending;
        }
        public int compare(DeviceHolder dh1, DeviceHolder dh2) {
            // assume we are comparing DeviceHolder records
            if (dh1 == dh2) {
                return 0; // exact same object (or both null)
            } else 
            if (dh1 == null) {
                return this.ascending? -1 :  1; // null < non-null
            } else
            if (dh2 == null) {
                return this.ascending?  1 : -1; // non-null > null
            } else {
                String D1 = dh1.getDeviceDescription().toLowerCase(); // dh1.getDeviceID();
                String D2 = dh2.getDeviceDescription().toLowerCase(); // dh2.getDeviceID();
                return this.ascending? D1.compareTo(D2) : D2.compareTo(D1);
            }
        }
        public boolean equals(Object other) {
            if (other instanceof DeviceHolderComparator) {
                DeviceHolderComparator dhc = (DeviceHolderComparator)other;
                return (this.ascending == dhc.ascending);
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Account                     account  = null;
    private User                        user     = null;
    private DeviceGroup                 devGroup = null;
    private boolean                     isGroup  = false;
    
    private Map<String,DeviceHolder>    devMap   = null;

    // ------------------------------------------------------------------------

    /* generic instance (devices will be added later) */
    public ReportDeviceList(Account acct, User user)
    {
        super();
        this.account = acct;
        this.user    = user;
        this.devMap  = null;
    }

    /* constuctor with specific device */
    public ReportDeviceList(Account acct, User user, Device device)
    {
        this(acct, user);
        this.add(device);
    }

    /* constuctor with a list of devices */
    public ReportDeviceList(Account acct, User user, String devID[])
    {
        this(acct, user);
        this.add(devID);
    }

    /* constuctor with a specific group */
    public ReportDeviceList(Account acct, User user, DeviceGroup group)
    {
        this(acct, user);
        this.devGroup = group;
        this.isGroup  = true;
        this.add(group);
    }

    // ------------------------------------------------------------------------

    /* return account id */
    public String getAccountID()
    {
        return (this.account != null)? this.account.getAccountID() : "";
    }

    /* return account db instance */
    public Account getAccount()
    {
        return this.account;
    }

    // ------------------------------------------------------------------------

    /* return user id */
    public String getUserID()
    {
        return (this.user != null)? this.user.getUserID() : "";
    }

    /* return user db instance */
    public User getUser()
    {
        return this.user;
    }

    // ------------------------------------------------------------------------

    /* return device group id */
    public String getDeviceGroupID()
    {
        return (this.devGroup != null)? this.devGroup.getGroupID() : "";
    }

    /* return device group db instance */
    public DeviceGroup getDeviceGroup()
    {
        return this.devGroup;
    }
    
    /* return ture if group */
    public boolean isDeviceGroup()
    {
        return this.isGroup || (this.size() > 1);
    }
    
    // ------------------------------------------------------------------------

    /* return device for named id (must already exist in the device map) */
    public Device getDevice(String devID)
        throws DBException
    {

        /* invalid device-id specified? */
        if (StringTools.isBlank(devID)) {
            return null;
        }

        /* return device */
        DeviceHolder dh = this.getDeviceMap().get(devID);
        return (dh != null)? dh.getDevice() : null;

    }

    // ------------------------------------------------------------------------

    /* clear the device map */
    public void clear()
    {
        if (this.devMap != null) {
            this.devMap.clear(); // set Map to empty
        }
    }

    /* return the internal device map */
    protected Map<String,DeviceHolder> getDeviceMap()
    {
        if (this.devMap == null) {
            this.devMap = new HashMap<String,DeviceHolder>(10);
        }
        return this.devMap;
    }

    // ------------------------------------------------------------------------

    /* set the single DeviceHolder object */
    public boolean setDevice(User user, DeviceHolder dh)
    {
        this.clear();
        try {
            this._addDevice(user, dh);
            return true;
        } catch (DBException dbe) {
            return false;
        }
    }

    /* add DeviceHolder if absent from list */
    protected void _addDevice(User user, DeviceHolder dh)
        throws DBException
    {
        String devID = dh.getDeviceID();
        if ((user == null) || user.isAuthorizedDevice(devID)) {
            Map<String,DeviceHolder> dm = this.getDeviceMap();
            if (dm.containsKey(devID)) {
                // already present, try updating device
                if (dh.hasDevice()) { // probably will be false
                    DeviceHolder dmdh = dm.get(devID);
                    dmdh.setDevice(dh.getDevice());
                }
            } else {
                // new entry, add DeviceHolder
                dm.put(devID, dh);
            }
        }
    }
    
    /* add DeviceHolder if absent from list */
    protected void _addDevice(User user, Device device)
        throws DBException
    {
        String devID = device.getDeviceID();
        if ((user == null) || user.isAuthorizedDevice(devID)) {
            Map<String,DeviceHolder> dm = this.getDeviceMap();
            if (dm.containsKey(devID)) {
                // already present, update device
                DeviceHolder dmdh = dm.get(devID);
                dmdh.setDevice(device);
            } else {
                // new entry, add device
                DeviceHolder dh = new DeviceHolder(device);
                dm.put(devID, dh);
            }
        }
    }

    // ------------------------------------------------------------------------

    /* add device to map */
    public boolean add(Device device)
    {
        
        /* invalid device */
        if (device == null) {
            return false;
        }
        
        /* add device */
        //Print.logStackTrace("Adding device: " + device.getDeviceID());
        User user = this.getUser();
        try {
            this._addDevice(user, device);
            return true;
        } catch (DBException dbe) {
            return false;
        }
            
    }

    /* add list of devices to map */
    public boolean add(String devID[])
    {
        
        /* empty list */
        if (ListTools.isEmpty(devID)) {
            return false;
        }
        
        /* add devices from list */
        //Print.logStackTrace("Adding devices ...");
        Account acct = this.getAccount();
        User    user = this.getUser();
        try {
            for (int i = 0; i < devID.length; i++) {
                this._addDevice(user, new DeviceHolder(acct, devID[i]));
            }
            return true;
        } catch (DBException dbe) {
            return false;
        }

    }

    /* add list of devices to map */
    public boolean add(java.util.List<String> devIDList)
    {
        
        /* empty list */
        if (ListTools.isEmpty(devIDList)) {
            return false;
        }
        
        /* add devices from list */
        //Print.logStackTrace("Adding devices ...");
        Account acct = this.getAccount();
        User    user = this.getUser();
        try {
            for (String devID : devIDList) {
                this._addDevice(user, new DeviceHolder(acct, devID));
            }
            return true;
        } catch (DBException dbe) {
            return false;
        }

    }

    /* add device to map */
    public boolean add(String devID)
    {
        
        /* invalid Device id? */
        if (StringTools.isBlank(devID)) {
            return false;
        }
        
        /* add device id */
        //Print.logStackTrace("Adding device: " + devID);
        Account acct = this.getAccount();
        User    user = this.getUser();
        try {
            this._addDevice(user, new DeviceHolder(acct, devID));
            return true;
        } catch (DBException dbe) {
            return false;
        }

    }

    /* add device-group to map */
    public boolean add(DeviceGroup group)
    {
        
        /* invalid group */
        if (group == null) {
            return false;
        }
        
        /* AccountID mismatch? */
        String  acctID = this.getAccountID();
        if (!acctID.equals(group.getAccountID())) {
            return false;
        }
        
        /* add devices from group */
        Account acct = this.getAccount();
        User    user = this.getUser();
        try {
            OrderedSet<String> devIDSet = DeviceGroup.getDeviceIDsForGroup(acctID, group.getGroupID(), null/*User*/, false);
            for (int i = 0; i < devIDSet.size(); i++) {
                this._addDevice(user, new DeviceHolder(acct, devIDSet.get(i)));
            }
            this.isGroup = true;
            return true;
        } catch (DBException dbe) {
            Print.logException("Unable to add DeviceGroup", dbe);
            return false;
        }

    }

    // ------------------------------------------------------------------------

    /* add all user authorized devices to the internal device map */
    public void addAllAuthorizedDevices()
    {
        try {
            User usr = this.getUser();
            Account acct = this.getAccount();
            OrderedSet<String> list = User.getAuthorizedDeviceIDs(usr, acct, false);
            //Print.logInfo("Authorized devices: " + list.size());
            this.add(list);
        } catch (DBException dbe) {
            Print.logException("Unable to add all User devices", dbe);
        }
    }
    
    // ------------------------------------------------------------------------

    /* return number of devices currently in the map */
    public int size()
    {
        return this.getDeviceMap().size();
    }

    // ------------------------------------------------------------------------
    
    /* return a device map iterator */
    public Iterator<String> iterator()
    {
        return this.getDeviceMap().keySet().iterator();
    }

    // ------------------------------------------------------------------------
    
    /* return the device map values */
    public java.util.List<DeviceHolder> getDeviceHolderList(boolean sort)
    {
        java.util.List<DeviceHolder> dhList = new Vector<DeviceHolder>(this.getDeviceMap().values());
        if (sort) {
            Collections.sort(dhList, new DeviceHolderComparator());
        }
        return dhList;
    }

    // ------------------------------------------------------------------------

    /* return the first deviceID in the map */
    public String getFirstDeviceID()
    {
        Iterator i = this.iterator();
        if (i.hasNext()) {
            return (String)i.next();
        } else {
            return "";
        }
    }

    /* return the first device in the map */
    public Device getFirstDevice()
    {
        String devID = this.getFirstDeviceID();
        if (!devID.equals("")) {
            try {
                return this.getDevice(devID);
            } catch (DBException dbe) {
                return null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /* return a string representation of this ReportDeviceList */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("ReportDeviceList:");
        sb.append(" Account=").append(this.getAccountID());
        sb.append(" User=").append(this.getUserID());
        sb.append(" Group=").append(this.getDeviceGroupID());
        sb.append(" Size=").append(this.size());
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

}
