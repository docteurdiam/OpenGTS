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
//  2007/02/21  Martin D. Flynn
//      -Initial release
//  2010/06/17  Martin D. Flynn
//      -Added support for J1939, OBDII
//  2010/09/09  Martin D. Flynn
//      -Modified method used for obtaining J1587 MID/PID/SID/FMI descriptions
// ----------------------------------------------------------------------------
package org.opengts.dbtypes;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.DBConfig;

public class DTOBDFault
    extends DBFieldType
{

    // ------------------------------------------------------------------------
    
    public static final String PROP_TYPE[]      = new String[] { "type"  , "TYPE"   };
    public static final String PROP_MID[]       = new String[] { "mid"   , "MID"    };
    public static final String PROP_SID[]       = new String[] { "sid"   , "SID"    };
    public static final String PROP_PID[]       = new String[] { "pid"   , "PID"    };
    public static final String PROP_FMI[]       = new String[] { "fmi"   , "FMI"    };
    public static final String PROP_SPN[]       = new String[] { "spn"   , "SPN"    };
    public static final String PROP_DTC_0[]     = new String[] { "dtc"   , "DTC"    , "dtc0", "DTC0" };
    public static final String PROP_COUNT[]     = new String[] { "count" , "COUNT"  };
    public static final String PROP_ACTIVE[]    = new String[] { "active", "ACTIVE" };

    public static final String NAME_J1708       = "J1708";
    public static final String NAME_J1939       = "J1939";
    public static final String NAME_OBDII       = "OBDII";

    public static final String NAME_MID         = "MID";
    public static final String NAME_MID_DESC    = NAME_MID + ".desc";
    public static final String NAME_PID         = "PID";
    public static final String NAME_PID_DESC    = NAME_PID + ".desc";
    public static final String NAME_SID         = "SID";
    public static final String NAME_SID_DESC    = NAME_SID + ".desc";
    public static final String NAME_SPN         = "SPN";
    public static final String NAME_FMI         = "FMI";
    public static final String NAME_FMI_DESC    = NAME_FMI + ".desc";
    public static final String NAME_DTC         = "DTC";

    public static final long   TYPE_MASK        = 0x7000000000000000L;
    public static final int    TYPE_SHIFT       = 60;
    public static final long   TYPE_J1708       = 0x0000000000000000L;
    public static final long   TYPE_J1939       = 0x1000000000000000L;
    public static final long   TYPE_OBDII       = 0x2000000000000000L;

    public static final long   ACTIVE_MASK      = 0x0100000000000000L;
    public static final int    ACTIVE_SHIFT     = 56;

    public static final long   MID_MASK         = 0x00FFFFFF00000000L;
    public static final int    MID_SHIFT        = 32;
    
    public static final long   SPID_MASK        = 0x00000000FFFF0000L;
    public static final int    SPID_SHIFT       = 16;
    public static final long   SID_MASK         = 0x0000000080000000L;

    public static final long   FMI_MASK         = 0x000000000000FF00L;
    public static final int    FMI_SHIFT        =  8;
    
    public static final long   COUNT_MASK       = 0x00000000000000FFL;
    public static final int    COUNT_SHIFT      =  0;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static long EncodeActive(boolean active)
    {
        return active? ACTIVE_MASK : 0L;
    }
    
    public static boolean DecodeActive(long fault)
    {
        if (DTOBDFault.IsJ1708(fault)) {
            return ((fault & ACTIVE_MASK) != 0L);
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    public static long EncodeSystem(int sys)
    {
        return ((long)sys << MID_SHIFT) & MID_MASK;
    }
    
    public static int DecodeSystem(long fault)
    {
        return (int)((fault & MID_MASK) >> MID_SHIFT);
    }
    
    // ------------------------------------------------------------------------

    public static long EncodeSPID(int sub)
    {
        return ((long)sub << SPID_SHIFT) & SPID_MASK;
    }
    
    public static int DecodeSPID(long fault)
    {
        return (int)((fault & SPID_MASK) >> SPID_SHIFT);
    }

    public static int DecodePidSid(long fault)
    {
        return DecodeSPID(fault) & 0x0FFF;
    }

    // ------------------------------------------------------------------------

    public static long EncodeFMI(int fmi)
    {
        return ((long)fmi << FMI_SHIFT) & FMI_MASK;
    }
    
    public static int DecodeFMI(long fault)
    {
        return (int)((fault & FMI_MASK) >> FMI_SHIFT);
    }

    // ------------------------------------------------------------------------

    public static long EncodeCount(int count)
    {
        return ((long)count << COUNT_SHIFT) & COUNT_MASK;
    }
    
    public static int DecodeCount(long fault)
    {
        return (int)((fault & COUNT_MASK) >> COUNT_SHIFT);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    //  J1708: type=j1708 mid=123 pid=123 fmi=1 count=1 active=false
    //  J1939: type=i1939 spn=1234 fmi=12 count=1 active=false
    //  OBDII: type=obdii dtc=024C

    public static String GetPropertyString(long fault)
    {
        StringBuffer sb = new StringBuffer();
        if ((fault & TYPE_MASK) == TYPE_J1708) {
            sb.append(PROP_TYPE[0]).append("=").append(NAME_J1708);
            boolean active = DecodeActive(fault);
            int     mid    = DecodeSystem(fault);
            int     fmi    = DecodeFMI(fault);
            int     count  = DecodeCount(fault);
            sb.append(" ").append(PROP_MID[0]).append("=").append(mid);
            if (DTOBDFault.IsJ1708_SID(fault)) {
                int sid    = DecodePidSid(fault);
                sb.append(" ").append(PROP_SID[0]).append("=").append(sid);
            } else {
                int pid    = DecodePidSid(fault);
                sb.append(" ").append(PROP_PID[0]).append("=").append(pid);
            }
            sb.append(" ").append(PROP_FMI[0]).append("=").append(fmi);
            if (count > 1) {
                sb.append(" ").append(PROP_COUNT[0]).append("=" + count);
            }
            if (!active) {
                sb.append(" ").append(PROP_ACTIVE[0]).append("=false");
            }
        } else
        if ((fault & TYPE_MASK) == TYPE_J1939) {
            sb.append(PROP_TYPE[0]).append("=").append(NAME_J1939);
            boolean active = true;
            int     spn    = DecodeSystem(fault);
            int     fmi    = DecodeFMI(fault);
            int     count  = DecodeCount(fault);
            sb.append(" ").append(PROP_SPN[0]).append("=").append(spn);
            sb.append(" ").append(PROP_FMI[0]).append("=").append(DecodeFMI(fault));
            if (count > 1) {
                sb.append(" ").append(PROP_COUNT[0]).append("=" + count);
            }
            if (!active) {
                sb.append(" ").append(PROP_ACTIVE[0]).append("=false");
            }
        } else
        if ((fault & TYPE_MASK) == TYPE_OBDII) {
            sb.append(PROP_TYPE[0]).append("=").append(NAME_OBDII);
            boolean active = true;
            int     dtc    = DecodeSystem(fault);
            sb.append(" ").append(PROP_DTC_0[0]).append("=").append(dtc);
        } else {
            // unrecognized
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /* return string representation of fault code */
    public static String GetFaultString(long fault)
    {
        if (fault > 0L) {
            StringBuffer sb = new StringBuffer();
            if ((fault & TYPE_MASK) == TYPE_J1708) {
                // SID: "128/s123/1"
                // PID: "128/123/1"
                boolean active = DTOBDFault.DecodeActive(fault);
                int     mid    = DTOBDFault.DecodeSystem(fault);
                int     fmi    = DTOBDFault.DecodeFMI(fault);
                if (!active) {
                    sb.append("[");
                }
                sb.append(mid);                     // MID
                sb.append("/");
                if (DTOBDFault.IsJ1708_SID(fault)) {
                    int sid = DTOBDFault.DecodePidSid(fault);
                    sb.append("s").append(sid);     // SID "128/s123/1"
                } else {
                    int pid = DTOBDFault.DecodePidSid(fault);
                    sb.append(pid);                 // PID "128/123/1"
                }
                sb.append("/");
                sb.append(fmi);                     // FMI
                if (!active) {
                    sb.append("]");
                }
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_J1939) {
                // SPN: "128/1"
                boolean active = DTOBDFault.DecodeActive(fault);
                int     spn    = DTOBDFault.DecodeSystem(fault);
                int     fmi    = DTOBDFault.DecodeFMI(fault);
                sb.append(spn);                    // SPN
                sb.append("/");
                sb.append(fmi);                    // FMI
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_OBDII) {
                // DTC: "024C"
                boolean active = DTOBDFault.DecodeActive(fault);
                int     dtc    = DTOBDFault.DecodeSystem(fault);
                sb.append(StringTools.toHexString(dtc,16)); // DTC
                return sb.toString();
            } else {
                // unrecognized
            }
        }
        return "---";
    }

    /* return fault header */
    public static String GetFaultHeader(long fault)
    {
        if ((fault & TYPE_MASK) == TYPE_J1708) {
            if (DTOBDFault.IsJ1708_SID(fault)) {
                return NAME_MID + "/" + NAME_SID + "/" + NAME_FMI;
            } else {
                return NAME_MID + "/" + NAME_PID + "/" + NAME_FMI;
            }
        } else
        if ((fault & TYPE_MASK) == TYPE_J1939) {
            return NAME_SPN + "/" + NAME_FMI;
        } else
        if ((fault & TYPE_MASK) == TYPE_OBDII) {
            return NAME_DTC;
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* encode "type=<type> ..." into long value */
    public static long EncodeFault(String faultProps)
    {
        if (!StringTools.isBlank(faultProps)) {
            return DTOBDFault.EncodeFault(new RTProperties(faultProps));
        } else {
            return 0L;
        }
    }

    /* encode "type=<type> ..." into long value */
    public static long EncodeFault(RTProperties rtp)
    {
        String type = rtp.getString(PROP_TYPE,null);
        if (type.equalsIgnoreCase(NAME_J1708)) {
            int     mid    = rtp.getInt(PROP_MID,0);
            int     sid    = rtp.getInt(PROP_SID,-1);
            int     pid    = rtp.getInt(PROP_PID,-1);
            int     pidSid = (sid >= 0)? sid : pid;
            int     fmi    = rtp.getInt(PROP_FMI,0);
            int     count  = rtp.getInt(PROP_COUNT,0);
            boolean active = rtp.getBoolean(PROP_ACTIVE,true);
            return EncodeFault_J1708(mid, (sid >= 0), pidSid, fmi, count, active);
        } else
        if (type.equalsIgnoreCase(NAME_J1939)) {
            int     spn    = rtp.getInt(PROP_SPN,0);
            int     fmi    = rtp.getInt(PROP_FMI,0);
            int     count  = rtp.getInt(PROP_COUNT,0);
            boolean active = rtp.getBoolean(PROP_ACTIVE,true);
            return EncodeFault_J1939(spn, fmi, count);
        } else
        if (type.equalsIgnoreCase(NAME_OBDII)) {
            int     dtc    = rtp.getInt(PROP_DTC_0,0);
            return EncodeFault_OBDII(dtc);
        } else {
            return 0L;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* encode J1708 MID,PID/SID,FMI into long value */
    // 0x0100000000000000
    public static long EncodeFault_J1708(int mid, boolean isSID, int pidSid, int fmi, int count, boolean active)
    {
        int spid = isSID? (pidSid | 0x8000) : pidSid;
        long faultCode = TYPE_J1708;
        faultCode |= EncodeActive(active);      // [ACTIVE_MASK]    0x0100000000000000
        faultCode |= EncodeSystem(mid);         // [MID_MASK]       0x00FFFFFF00000000
        faultCode |= EncodeSPID(spid);          // [SPID_MASK]      0x00000000FFFF0000
        faultCode |= EncodeFMI(fmi);            // [FMI_MASK]       0x000000000000FF00
        faultCode |= EncodeCount(count);        // [COUNT_MASK]     0x00000000000000FF
        return faultCode;
    }

    /* return true if J1708 */
    public static boolean IsJ1708(long fault)
    {
        return ((fault & TYPE_MASK) == TYPE_J1708);
    }
    
    /* return true if long value represents a SID */
    public static boolean IsJ1708_SID(long fault)
    {
        return DTOBDFault.IsJ1708(fault) && ((fault & SID_MASK) != 0L);
    }
    
    /* return true if long value represents a SID */
    public static boolean IsJ1708_PID(long fault)
    {
        return DTOBDFault.IsJ1708(fault) && ((fault & SID_MASK) == 0L);
    }

    // ------------------------------------------------------------------------

    /* encode J1939 SPN,FMI into long value */
    public static long EncodeFault_J1939(int spn, int fmi, int count)
    {
        long faultCode = TYPE_J1939;
        faultCode |= EncodeActive(true);        // [ACTIVE_MASK]    0x0100000000000000
        faultCode |= EncodeSystem(spn);         // [MID_MASK]       0x00FFFFFF00000000
        faultCode |= EncodeFMI(fmi);            // [FMI_MASK]       0x000000000000FF00
        faultCode |= EncodeCount(count);        // [COUNT_MASK]     0x00000000000000FF
        return faultCode;
    }

    /* return true if J1939 */
    public static boolean IsJ1939(long fault)
    {
        return ((fault & TYPE_MASK) == TYPE_J1939);
    }

    // ------------------------------------------------------------------------

    /* encode OBDII DTC into long value */
    public static long EncodeFault_OBDII(int dtc)
    {
        long faultCode = TYPE_OBDII;
        faultCode |= EncodeActive(true);        // [ACTIVE_MASK]    0x0100000000000000
        faultCode |= EncodeSystem(dtc);         // [MID_MASK]       0x00FFFFFF00000000
        return faultCode;
    }

    /* return true if OBDII */
    public static boolean IsOBDII(long fault)
    {
        return ((fault & TYPE_MASK) == TYPE_OBDII);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean                  j1587DidInit        = false;
    private static J1587DescriptionProvider j1587DescProvider   = null;
    private static MethodAction             j1587GetDescription = null;

    public interface J1587DescriptionProvider
    {
        public Properties getJ1587Descriptions(long fault);
    }

    public static boolean InitJ1587DescriptionProvider()
    {
        if (!j1587DidInit) {
            j1587DidInit = true;
            try {
                j1587GetDescription = new MethodAction(DBConfig.PACKAGE_EXTRA_DBTOOLS_ + "J1587", "GetJ1587Description", Properties.class);
                j1587DescProvider   = new DTOBDFault.J1587DescriptionProvider() {
                    public Properties getJ1587Descriptions(long fault) {
                        if (DTOBDFault.IsJ1708(fault)) {
                            int     mid    = DTOBDFault.DecodeSystem(fault);    // MID
                            boolean isSid  = DTOBDFault.IsJ1708_SID(fault);
                            int     pidSid = DTOBDFault.DecodePidSid(fault);    // PID|SID "128/[s]123/1"
                            int     fmi    = DTOBDFault.DecodeFMI(fault);       // FMI
                            Properties p = new Properties();
                            p.setProperty("MID", String.valueOf(mid));
                            p.setProperty((isSid?"SID":"PID"), String.valueOf(pidSid));
                            p.setProperty("FMI", String.valueOf(fmi));
                            try {
                                return (Properties)j1587GetDescription.invoke(p);
                            } catch (Throwable th) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                };
                Print.logDebug("J1587 Description Provider installed ...");
            } catch (Throwable th) {
                //Print.logException("J1587 Description Provider NOT installed!", th);
            }
        }
        return (j1587DescProvider != null);
    }
    
    public static boolean HasDescriptionProvider()
    {
        return (j1587DescProvider != null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String GetFaultDescription(long fault, Locale locale)
    {
        if (fault != 0L) {
            String fmt = "000";
            StringBuffer sb = new StringBuffer();
            if ((fault & TYPE_MASK) == TYPE_J1708) {
                int     mid    = DTOBDFault.DecodeSystem(fault);    // MID
                boolean isSid  = DTOBDFault.IsJ1708_SID(fault);
                int     pidSid = DTOBDFault.DecodePidSid(fault);    // PID|SID "128/[s]123/1"
                int     fmi    = DTOBDFault.DecodeFMI(fault);       // FMI
                Properties p   = (j1587DescProvider != null)? j1587DescProvider.getJ1587Descriptions(fault) : new Properties();
                // MID
                sb.append(NAME_MID + "(" + StringTools.format(mid,fmt) + ") " + p.getProperty(NAME_MID_DESC,"") + "\n");
                // PID/SID
                if (isSid) {
                    sb.append(NAME_SID + "(" + StringTools.format(pidSid,fmt) + ") " + p.getProperty(NAME_SID_DESC,"") + "\n");
                } else {
                    sb.append(NAME_PID + "(" + StringTools.format(pidSid,fmt) + ") " + p.getProperty(NAME_PID_DESC,"") + "\n");
                }
                // FMI
                sb.append(NAME_FMI + "(" + StringTools.format(fmi,fmt) + ") " + p.getProperty(NAME_FMI_DESC,""));
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_J1939) {
                int spn = DTOBDFault.DecodeSystem(fault);          // SPN
                int fmi = DTOBDFault.DecodeFMI(fault);             // FMI
                Properties p = new Properties();
                // SPN
                sb.append(NAME_SPN + "(" + StringTools.format(spn,fmt) + ") " + p.getProperty(NAME_SPN,"") + "\n");
                // FMI
                sb.append(NAME_FMI + "(" + StringTools.format(fmi,fmt) + ") " + p.getProperty(NAME_FMI,""));
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_OBDII) {
                int dtc = DTOBDFault.DecodeSystem(fault);          // DTC
                Properties p = new Properties();
                // DTC
                sb.append(NAME_DTC + "(" + StringTools.format(dtc,fmt) + ") " + p.getProperty(NAME_DTC,""));
                return sb.toString();
            }
        }
        return "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long    faultCode   = 0L;

    public DTOBDFault(int mid, boolean isSid, int pidSid, int fmi, int count, boolean active)
    {
        this.faultCode = DTOBDFault.EncodeFault_J1708(mid, isSid, pidSid, fmi, count, active);
    }

    public DTOBDFault(int spn, int fmi, int count)
    {
        this.faultCode = DTOBDFault.EncodeFault_J1939(spn, fmi, count);
    }

    public DTOBDFault(int dtc)
    {
        this.faultCode = DTOBDFault.EncodeFault_OBDII(dtc);
    }

    public DTOBDFault(long faultCode)
    {
        this.faultCode = faultCode;
    }

    public DTOBDFault(ResultSet rs, String fldName)
        throws SQLException
    {
        super(rs, fldName);
        // set to default value if 'rs' is null
        this.faultCode = (rs != null)? rs.getLong(fldName) : 0L;
    }

    // ------------------------------------------------------------------------

    /* return fault code */
    public long getFaultCode()
    {
        return this.faultCode;
    }

    /* return multi-line description */
    public String getDescription()
    {
        return DTOBDFault.GetFaultDescription(this.getFaultCode(),null);
    }
    
    // ------------------------------------------------------------------------

    public boolean isJ1708()
    {
        return DTOBDFault.IsJ1708(this.getFaultCode());
    }

    public boolean isJ1939()
    {
        return DTOBDFault.IsJ1939(this.getFaultCode());
    }

    public boolean isOBDII()
    {
        return DTOBDFault.IsOBDII(this.getFaultCode());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public Object getObject()
    {
        return new Long(this.getFaultCode());
    }

    public String toString()
    {
        return "0x" + StringTools.toHexString(this.getFaultCode());
    }

    public boolean equals(Object other)
    {
        if (other instanceof DTOBDFault) {
            DTOBDFault jf = (DTOBDFault)other;
            return (this.getFaultCode() == jf.getFaultCode());
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        InitJ1587DescriptionProvider();
        RTProperties cmdLineProps = RTConfig.getCommandLineProperties();
        long fault = EncodeFault(cmdLineProps);
        Print.sysPrintln("Fault : " + fault + " [0x" + StringTools.toHexString(fault) + "]");
        Print.sysPrintln("String: " + GetPropertyString(fault)); 
        Print.sysPrintln("Desc  : " + GetFaultDescription(fault,null));
    }
    
}
