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
//  06/15/2006  Martin D. Flynn
//      Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.*;

/**
*** A set of tools for handling IP Adresses
**/

public class IPTools
{
    
    // ------------------------------------------------------------------------
    
    public    static final String HOST_UNKNOWN = "UNKNOWN";
    
    public    static final String IP_ADDR_ALL  = "*";
    public    static final char   IPAddressSeparatorChar = ',';
    public    static final String IPAddressSeparator = String.valueOf(IPAddressSeparatorChar);

    protected static final int    IP_ADDR_SIZE = 32;
    protected static final long   IP_ADDR_MASK = 0xFFFFFFFFL;
    
    // ------------------------------------------------------------------------
    
    private static String localhostName = null;
    
    /**
    *** Retruns name of the local host
    *** @return The local host name
    **/
    public static String getHostName()
    {
        /* host name */
        if (IPTools.localhostName == null) {
            try {
                String hd = InetAddress.getLocalHost().getHostName();
                int p = hd.indexOf(".");
                IPTools.localhostName = (p >= 0)? hd.substring(0,p) : hd;
            } catch (UnknownHostException uhe) {
                IPTools.localhostName = HOST_UNKNOWN;
            }
        }
        return IPTools.localhostName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the IP adress of a specified host name though a DNS lookup
    *** @param hostName The specified host name
    *** @return the IP adress of the specified host, or an empty string
    **/
    public static String getIPAddress(String hostName)
    {
        try {
            InetAddress addr = InetAddress.getByName(hostName);
            return addr.getHostAddress();
        } catch (Throwable t) {
            Print.logException("DNS lookup", t);
            return "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified IP address is valid
    *** @param ipAddr  The String representation of an IP address
    *** @return True if the specified IP address is valid, false otherwise.
    **/
    public static boolean isValidIPAddress(String ipAddr) 
    {
        if (StringTools.isBlank(ipAddr)) {
            return false; // blank
        } else 
        if (ipAddr.indexOf(" ") >= 0) {
            return false; // contains embedded spaces
        } else {
            String ipList[] = StringTools.parseString(ipAddr, '.');
            if (ipList.length != 4) {
                return false;
            } else {
                for (int i = 0; i < ipList.length; i++) {
                    if (!StringTools.isInt(ipList[i],true)) {
                        return false;
                    } else {
                        int x = StringTools.parseInt(ipList[i],-1);
                        if ((x < 0) || (x > 255)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses the specified IP address into it's binary form
    *** @param ipAddr The IP adress to parse as a string
    *** @return The parsed IP adress
    **/
    protected static long _parseIPAddress(String ipAddr) 
    {
        if (!StringTools.isBlank(ipAddr)) {
            int p = ipAddr.indexOf("/");
            if (p >= 0) { ipAddr = ipAddr.substring(0, p); }
            String ipList[] = StringTools.parseString(ipAddr, '.');
            long ip = 0L;
            for (int i = 0; i < 4; i++) {
                ip <<= 8;
                if (i < ipList.length) { ip |= StringTools.parseInt(ipList[i],0) & 0xFF; }
            }
            return ip;
        } else {
            return 0L;
        }
    }
    
    /**
    *** Parses an IP subnet mask [CHECK](better wording?)
    *** @param m The length of the adress section
    *** @return The parsed IP subnet mask
    **/
    protected static long _parseIPMask(int m) 
    {
        if (m > 32) { m = IP_ADDR_SIZE; }
        long x = (long)Math.pow(2.0,(double)m) - 1L; // (2**m) - 1;
        long mask = x << (IP_ADDR_SIZE - m);
        return mask;
    }

    /**
    *** Parses the specified IPAddress/Mask and returns the result in a 2-element array
    *** @param ipAddr  The specified IPAddress/Mask
    *** @return A 2-element array containing the parsed IP address and mask
    **/
    public static long[] parseIPAddress(String ipAddr) 
    {
        if (StringTools.isBlank(ipAddr)) {
            return null;
        } else
        if (ipAddr.equals(IP_ADDR_ALL)) {
            return new long[] { 0L, 0L };
        } else {
            int p = ipAddr.indexOf("/");
            if (p >= 0) {
                long ipm[] = new long[] { IPTools._parseIPAddress(ipAddr.substring(0,p)), IP_ADDR_MASK };
                String mask = ipAddr.substring(p + 1);
                ipm[1] = (mask.indexOf(".") >= 0)?
                    IPTools._parseIPAddress(mask) :
                    IPTools._parseIPMask(StringTools.parseInt(mask, IP_ADDR_SIZE));
                return ipm;
            } else {
                return new long[] { IPTools._parseIPAddress(ipAddr), IP_ADDR_MASK };
            }
        }
    }

    /**
    *** IPAddress class
    **/
    public static class IPAddress
    {
        private long ip = 0L;
        private long mask = 0L;
        public IPAddress(long ip[]) {
            this.mask = ((ip != null) && (ip.length > 1))? (ip[1] & IP_ADDR_MASK) : IP_ADDR_MASK;
            this.ip   = ((ip != null) && (ip.length > 0))? (ip[0] & this.mask) : 0L;
        }
        public IPAddress(long ip, long mask) {
            this(new long[] { ip, mask });
        }
        public IPAddress(String ipAddr, int mask) {
            this(IPTools._parseIPAddress(ipAddr), _parseIPMask(mask));
        }
        public IPAddress(String ipAddr, long mask) {
            this(IPTools._parseIPAddress(ipAddr), mask);
        }
        public IPAddress(String ipAddr) {
            this(IPTools.parseIPAddress(ipAddr));
        }
        public long getMask() {
            return this.mask;
        }
        public long getIP() {
            return this.ip;
        }
        public boolean isZero() {
            return (this.getIP() == 0L);
        }
        public boolean isMatch(String ipAddr) {
            long ip1 = IPTools._parseIPAddress(ipAddr) & this.mask;
            long ip2 = this.ip & this.mask;
            return (ip1 == ip2);
        }
        protected String _ipToString(long p) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i <= 3; i++) {
                if (i > 0) { sb.append("."); }
                int b = (int)(p >>> ((3 - i) * 8)) & 0xFF;
                sb.append(b);
            }
            return sb.toString();
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this._ipToString(this.ip));
            if (this.mask != IP_ADDR_MASK) {
                sb.append("/");
                long m = this.mask;
                int i, j;
                for (i = 0; (i < IP_ADDR_SIZE) && ((m & 1) == 0); i++, m >>>= 1);
                for (j = i; (j < IP_ADDR_SIZE) && ((m & 1) == 1); j++, m >>>= 1);
                if (j == IP_ADDR_SIZE) {
                    sb.append(IP_ADDR_SIZE - i);
                } else {
                    sb.append(this._ipToString(this.mask));
                }
            }
            return sb.toString();
        }
        public boolean equals(Object other) {
            if (other instanceof IPAddress) {
                IPAddress ipa = (IPAddress)other;
                return ((ipa.ip == this.ip) && (ipa.mask == this.mask));
            } else {
                return false;
            }
        }
    }
        
    // ------------------------------------------------------------------------

    /**
    *** Manages a list of IP address blocks
    *** <p>Can read IP addresses in the following format:
    *** "63.196.107.82/29,209.79.220.20,192.168.1.0/24"</p>
    **/
    public static class IPAddressList
    {
        private Vector<IPAddress> ipList = null;
        
        public IPAddressList() {
        }
        
        public IPAddressList(IPAddress list[]) {
            ListTools.toList(list, this.getIPList());
        }
        
        public IPAddressList(String ipListStr) {
            this.addIPAddress(ipListStr);
        }
        
        public java.util.List<IPAddress> getIPList() {
            if (this.ipList == null) { 
                this.ipList = new Vector<IPAddress>(); 
            }
            return this.ipList;
        }
        
        public void addIPAddress(IPAddress ipAddr) {
            java.util.List<IPAddress> v = this.getIPList();
            if ((ipAddr != null) && !v.contains(ipAddr)) {
                v.add(ipAddr);
            }
        }
        
        public void addIPAddress(IPAddress ipAddr[]) {
            if (ipAddr != null) {
                for (int i = 0; i < ipAddr.length; i++) {
                    this.addIPAddress(ipAddr[i]);
                }
            }
        }
        
        public void addIPAddress(String ipAddr) {
            String ipArray[] = StringTools.parseString(ipAddr, IPAddressSeparatorChar);
            for (int i = 0; i < ipArray.length; i++) {
                String ipm = ipArray[i].trim();
                if (!ipm.equals("")) {
                    this.addIPAddress(new IPTools.IPAddress(ipm));
                }
            }
        }
        
        public boolean contains(String ipAddr) {
            for (Iterator<IPAddress> i = this.getIPList().iterator(); i.hasNext();) {
                IPTools.IPAddress ipa = i.next();
                if (ipa.isMatch(ipAddr)) { 
                    //Print.logDebug(ipAddr + " Matches " + ipa);
                    return true; 
                }
            }
            return false;
        }
        
        public boolean isMatch(String ipAddr) {
            return this.contains(ipAddr);
        }
        
        public boolean isEmpty() {
            return ((this.ipList == null) || (this.ipList.size() <= 0));
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            java.util.List<IPAddress> v = this.getIPList();
            for (Iterator<IPAddress> i = v.iterator(); i.hasNext();) {
                if (sb.length() > 0) { sb.append(IPAddressSeparatorChar); }
                sb.append(i.next().toString());
            }
            return sb.toString();
        }
        
        public boolean equals(Object other) {
            if (other instanceof IPAddressList) {
                return this.toString().equals(other.toString());
            } else {
                return false;
            }
        }
    }
        
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.logInfo("Hostname: " + getHostName());
        String ipMask = RTConfig.getString("ipMask","10.0.0.0/13,10.8.0.0/16,10.9.0.0/16");
        String ipTarg = RTConfig.getString("ip","");
        if (StringTools.isBlank(ipTarg)) {
            Print.sysPrintln("Usage: %s -ipMask=X.X.X.X/X[,X.X.X.X/X] -ip=X.X.X.X");
            System.exit(99);
        }

        if (ipMask.indexOf(",") >= 0) {
            IPTools.IPAddressList ipList = new IPTools.IPAddressList(ipMask);
            Print.sysPrintln("'"+ipTarg+"' Match = " + ipList.contains(ipTarg));
            System.exit(0);
        } else {
            IPTools.IPAddress ipAddr = new IPTools.IPAddress(ipMask);
            Print.sysPrintln("'"+ipTarg+"' Match = " + ipAddr.isMatch(ipTarg));
            System.exit(0);
        }

    }
    
}
