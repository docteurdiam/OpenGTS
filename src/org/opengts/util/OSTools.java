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
//  General OS specific tools
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/06/20  Martin D. Flynn
//     -Added method 'getProcessID()'
//  2010/05/24  Martin D. Flynn
//     -Added "getMemoryUsage", "printMemoryUsage"
//  2011/08/21  Martin D. Flynn
//     -Added "getOSTypeName"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.management.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class OSTools
{

    // ------------------------------------------------------------------------

    private static final Object LockObject          = new Object();

    // ------------------------------------------------------------------------
    // OS and JVM specific tools
    // ------------------------------------------------------------------------

    // Note: these values may change from release to release
    // Reference: http://lopica.sourceforge.net/os.html
    private static final int OS_INITIALIZE          = -1;
    public  static final int OS_TYPE_MASK           = 0x00FFFF00;
    public  static final int OS_SUBTYPE_MASK        = 0x000000FF;

    public  static final int OS_UNKNOWN             = 0x00000000;
    
    public  static final int OS_LINUX               = 0x00010100;
    public  static final int OS_LINUX_FEDORA        = 0x00000001; // not detected
    public  static final int OS_LINUX_CENTOS        = 0x00000002; // not detected
    public  static final int OS_LINUX_UBUNTU        = 0x00000003; // not detected
    public  static final int OS_LINUX_DEBIAN        = 0x00000004; // not detected

    public  static final int OS_UNIX                = 0x00010200;
    public  static final int OS_UNIX_SOLARIS        = 0x00000001;
    public  static final int OS_UNIX_SUNOS          = 0x00000002;
    public  static final int OS_UNIX_AIX            = 0x00000004;
    public  static final int OS_UNIX_DIGITAL        = 0x00000005;
    public  static final int OS_UNIX_HPUX           = 0x00000006;
    public  static final int OS_UNIX_IRIX           = 0x00000007;

    public  static final int OS_BSD                 = 0x00010300;
    public  static final int OS_BSD_FREEBSD         = 0x00000001;

    public  static final int OS_MACOS               = 0x00010500;
    public  static final int OS_MACOS_X             = 0x00000001;

    public  static final int OS_WINDOWS             = 0x00000700;
    public  static final int OS_WINDOWS_9X          = 0x00000001; // 95/98/ME
    public  static final int OS_WINDOWS_XP          = 0x00000002;
    public  static final int OS_WINDOWS_VISTA       = 0x00000003;
    public  static final int OS_WINDOWS_7           = 0x00000004;
    public  static final int OS_WINDOWS_CE          = 0x00000010;
    public  static final int OS_WINDOWS_NT          = 0x00000011;
    public  static final int OS_WINDOWS_2000        = 0x00000012;
    public  static final int OS_WINDOWS_2003        = 0x00000013;
    public  static final int OS_WINDOWS_CYGWIN      = 0x000000C0; // not detected

    private static       int OSType                 = OS_INITIALIZE;

    /**
    *** Returns the known OS type as an integer bitmask
    *** @return The OS type
    **/
    public static int getOSType()
    {
        if (OSType == OS_INITIALIZE) {
            String osName = System.getProperty("os.name").toLowerCase();
            //Print.logInfo("OS: " + osName);
            if (osName.startsWith("windows")) {
                OSType = OS_WINDOWS;
                if (osName.startsWith("windows xp")) {
                    OSType |= OS_WINDOWS_XP;
                } else
                if (osName.startsWith("windows 9") || osName.startsWith("windows m")) {
                    OSType |= OS_WINDOWS_9X;
                } else
                if (osName.startsWith("windows 7")) {
                    OSType |= OS_WINDOWS_7;
                } else
                if (osName.startsWith("windows vista")) {
                    OSType |= OS_WINDOWS_VISTA;
                } else
                if (osName.startsWith("windows nt")) {
                    OSType |= OS_WINDOWS_NT;
                } else
                if (osName.startsWith("windows 2000")) {
                    OSType |= OS_WINDOWS_2000;
                } else
                if (osName.startsWith("windows 2003")) {
                    OSType |= OS_WINDOWS_2003;
                } else
                if (osName.startsWith("windows ce")) {
                    OSType |= OS_WINDOWS_CE;
                }
            } else
            if (osName.startsWith("mac")) {
                // "Max OS X"
                OSType = OS_MACOS;
                if (osName.startsWith("mac os x")) {
                    OSType |= OS_MACOS_X;
                }
            } else
            if (osName.startsWith("linux")) {
                // "Linux"
                OSType = OS_LINUX;
            } else
            if (osName.startsWith("solaris")) {
                // "Solaris"
                OSType = OS_UNIX | OS_UNIX_SOLARIS;
            } else
            if (osName.startsWith("sunos")) {
                // "Solaris"
                OSType = OS_UNIX | OS_UNIX_SUNOS;
            } else
            if (osName.startsWith("hp ux") || osName.startsWith("hp-ux")) {
                // "HP UX"
                OSType = OS_UNIX | OS_UNIX_HPUX;
            } else
            if (osName.startsWith("digital unix")) {
                // "Digital Unix"
                OSType = OS_UNIX | OS_UNIX_DIGITAL;
            } else
            if (osName.startsWith("aix")) {
                // "AIX"
                OSType = OS_UNIX | OS_UNIX_AIX;
            } else
            if (osName.startsWith("irix")) {
                // "Irix"
                OSType = OS_UNIX | OS_UNIX_IRIX;
            } else
            if (osName.startsWith("freebsd")) {
                // "FreeBSD"
                OSType = OS_BSD | OS_BSD_FREEBSD;
            } else
            if (osName.indexOf("unix") >= 0) {
                // "*Unix*"
                OSType = OS_UNIX;
            } else
            if (osName.indexOf("linux") >= 0) {
                // "*Linux*"
                OSType = OS_LINUX;
            } else
            if (File.separatorChar == '/') {
                // "Linux"
                OSType = OS_LINUX;
            } else {
                OSType = OS_UNKNOWN;
            }
        }
        return OSType;
    }

    /**
    *** Returns the String representation of the specified OS type
    *** @param type The OS type
    *** @return The OS type name
    **/
    public static String getOSTypeName(int type, boolean inclSubtype)
    {
        switch (type & OS_TYPE_MASK) {
            case OS_LINUX   : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_LINUX_FEDORA    : return "LINUX_FEDORA";
                        case OS_LINUX_CENTOS    : return "LINUX_CENTOS";
                        case OS_LINUX_UBUNTU    : return "LINUX_CENTOS";
                        case OS_LINUX_DEBIAN    : return "LINUX_DEBIAN";
                        default                 : return "LINUX";
                    }
                } else {
                    return "LINUX";
                }
            case OS_UNIX    : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_UNIX_SOLARIS    : return "UNIX_SOLARIS";
                        case OS_UNIX_SUNOS      : return "UNIX_SUNOS";
                        case OS_UNIX_AIX        : return "UNIX_AIX";
                        case OS_UNIX_DIGITAL    : return "UNIX_DIGITAL";
                        case OS_UNIX_HPUX       : return "UNIX_HPUX";
                        case OS_UNIX_IRIX       : return "UNIX_IRIX";
                        default                 : return "UNIX";
                    }
                } else {
                    return "UNIX";
                }
            case OS_BSD     : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_BSD_FREEBSD     : return "BSD_FREEBSD";
                        default                 : return "BSD";
                    }
                } else {
                    return "BSD";
                }
            case OS_MACOS   : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_MACOS_X         : return "MACOS_X";
                        default                 : return "MACOS";
                    }
                } else {
                    return "MACOS";
                }
            case OS_WINDOWS : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_WINDOWS_9X      : return "WINDOWS_9X";
                        case OS_WINDOWS_XP      : return "WINDOWS_XP";
                        case OS_WINDOWS_VISTA   : return "WINDOWS_VISTA";
                        case OS_WINDOWS_7       : return "WINDOWS_7";
                        case OS_WINDOWS_2000    : return "WINDOWS_2000";
                        case OS_WINDOWS_NT      : return "WINDOWS_NT";
                        default                 : return "WINDOWS";
                    }
                } else {
                    return "WINDOWS";
                }
            default         : 
                return "UNKNOWN";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is the specified type
    *** @return True if the OS is the specified type
    **/
    public static boolean isOSType(int type)
    {
        int osType = getOSType();
        return ((osType & OS_TYPE_MASK) == type);
    }

    /**
    *** Returns true if the OS is the specified type
    *** @return True if the OS is the specified type
    **/
    public static boolean isOSType(int type, int subType)
    {
        int osType = getOSType();
        if ((osType & OS_TYPE_MASK) != type) {
            // type mismatch
            return false;
        } else
        if (subType <= 0) {
            // subtype not specified
            return true;
        } else {
            // test subtype
            return ((osType & OS_SUBTYPE_MASK & subType) != 0);
        }
    }

    /**
    *** Returns true if the OS is unknown
    *** @return True if the OS is unknown
    **/
    public static boolean isUnknown()
    {
        return (getOSType() == OS_UNKNOWN);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is a flavor of Windows
    *** @return True if the OS is a flavor of Windows
    **/
    public static boolean isWindows()
    {
        return isOSType(OS_WINDOWS);
    }

    /**
    *** Returns true if the OS is Windows XP
    *** @return True if the OS is Windows XP
    **/
    public static boolean isWindowsXP()
    {
        return isOSType(OS_WINDOWS, OS_WINDOWS_XP);
    }

    /**
    *** Returns true if the OS is Windows 95/98
    *** @return True if the OS is Windows 95/98
    **/
    public static boolean isWindows9X()
    {
        return isOSType(OS_WINDOWS, OS_WINDOWS_9X);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is Unix/Linux
    *** @return True if the OS is Unix/Linux
    **/
    public static boolean isLinux()
    {
        return isOSType(OS_LINUX) || isOSType(OS_UNIX);
    }

    /**
    *** Returns true if the OS is Apple Mac OS
    *** @return True if the OS is Apple Mac OS
    **/
    public static boolean isMacOS()
    {
        return isOSType(OS_MACOS);
    }

    /**
    *** Returns true if the OS is Apple Mac OS X
    *** @return True if the OS is Apple Mac OS X
    **/
    public static boolean isMacOSX()
    {
        return isOSType(OS_MACOS, OS_MACOS_X);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the current host name
    *** @return The current hostname
    **/
    public static String getHostName()
    {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            //Print.logException("Error", uhe);
            return "";
        }
    }

    /**
    *** Gets the current host IP address
    *** @return The current IP address
    **/
    public static String getHostIP()
    {
        try {
            String ip = StringTools.trim(InetAddress.getByName(InetAddress.getLocalHost().getHostName()));
            int    h  = ip.indexOf("/");
            return (h >= 0)? ip.substring(h+1) : ip;
        } catch (UnknownHostException uhe) {
            //Print.logException("Error", uhe);
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Get the current memory usage (in number of bytes)
    *** @param L  The long array where the memory values will be placed.  If 'null',
    ***           is specified, or if the array has fewer than 3 elements, a new
    ***           long array will be returned.
    ***           then the array must be 
    *** @return The current memory usage as an array of 3 long values indicating
    ***         { MaxMemory, TotalMemory, FreeMemory } (in that order).
    **/
    public static long[] getMemoryUsage(long L[])
    {
        long mem[] = ((L != null) && (L.length >= 3))? L : new long[3];
        Runtime rt = Runtime.getRuntime();
        synchronized (OSTools.LockObject) {
            mem[0] = rt.maxMemory();
            mem[1] = rt.totalMemory();
            mem[2] = rt.freeMemory();
        }
        return mem;
    }
    
    /**
    *** Get the current memory usage String
    **/
    public static String getMemoryUsageStringMb()
    {
        // "Max=4.00, Total=4.00, Free=2.00, Used=2.00"
        double divisor = 1024.0 * 1024.0; // megabytes
        long   mem[]   = OSTools.getMemoryUsage(null);
        double maxK    = (double)mem[0] / divisor;
        double totK    = (double)mem[1] / divisor;
        double freK    = (double)mem[2] / divisor;
        double useK    = totK - freK;
        StringBuffer sb = new StringBuffer();
        //sb.append("[Mb] ");
        sb.append("Max="  ).append(StringTools.format(maxK,"0.0")).append(", ");
        sb.append("Total=").append(StringTools.format(totK,"0.0")).append(", ");
        sb.append("Free=" ).append(StringTools.format(freK,"0.0")).append(", ");
        sb.append("Used=" ).append(StringTools.format(useK,"0.0"));
        return sb.toString();
    }
    
    /**
    *** Prints the current memory usage to the log file
    **/
    public static void printMemoryUsage()
    {
        long mem[] = OSTools.getMemoryUsage(null);
        long maxK  = mem[0] / 1024L;
        long totK  = mem[1] / 1024L;
        long freK  = mem[2] / 1024L;
        Print.logInfo("Memory-K: max=%d, total=%d, free=%d, used=%d", maxK, totK, freK, (totK - freK));
        //OSTools.printMemoryUsageMXBean();
    }
    
    /**
    *** Prints the current memory usage to the log file
    **/
    public static void printMemoryUsageMXBean()
    {
        
        /* Heap/Non-Heap */
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage    = memory.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memory.getNonHeapMemoryUsage();
        Print.logInfo("Heap Memory Usage    : " + formatMemoryUsage(heapUsage   ));
        Print.logInfo("Non-Heap Memory Usage: " + formatMemoryUsage(nonHeapUsage)); 

        /* Pools */
        java.util.List<MemoryPoolMXBean> memPool = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean mp : memPool) {
            String      name      = mp.getName();
            MemoryType  type      = mp.getType();
            MemoryUsage estUsage  = mp.getUsage();
            MemoryUsage peakUsage = mp.getPeakUsage();
            MemoryUsage collUsage = mp.getCollectionUsage();
            Print.logInfo("Pool Usage: " + name + " [" + type + "]");
            Print.logInfo("  Estimate  : "  + formatMemoryUsage(estUsage ));
            Print.logInfo("  Peak      : "  + formatMemoryUsage(peakUsage));
            Print.logInfo("  Collection: "  + formatMemoryUsage(collUsage));
        }

    }
    
    /**
    *** Formats a MemoryUsage instance
    **/
    private static String formatMemoryUsage(MemoryUsage u)
    {
        if (u != null) {
            long comm = u.getCommitted() / 1024L;
            long init = u.getInit()      / 1024L;
            long max  = u.getMax()       / 1024L;
            long used = u.getUsed()      / 1024L;
            StringBuffer sb = new StringBuffer();
            sb.append("[K]");
            sb.append(" Committed=").append(comm);
            sb.append(" Init=").append(init);
            sb.append(" Max=").append(max);
            sb.append(" Used=").append(used);
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Object  memoryCheckLock         = new Object();
    private static long    firstMem_maxB           = 0L;
    private static long    firstMem_usedB          = 0L;
    private static long    firstMem_time           = 0L;
    private static long    memoryCheckCount        = 0L;
    private static long    averMem_usedB           = 0L;
    private static long    lastMem_usedB           = 0L;

    /**
    *** Analyzes/Prints the current memory usage.<br>
    *** (This method only analyzes/prints memory usage if the current usage is less than 
    *** the previous usage, implying that a garbage collection has recently occured)<br>
    *** Useful for determining <b>IF</b> there are memory leaks, and how much it is leaking, 
    *** but useless for determining <b>WHERE</b> the leak is occurring.
    *** @param reset  True to reset the memory growth-rate checks.
    **/
    public static void checkMemoryUsage(boolean reset)
    {
        // http://olex.openlogic.com/wazi/2009/how-to-fix-memory-leaks-in-java/
        // http://java.sun.com/docs/hotspot/gc1.4.2/faq.html
        // http://java.dzone.com/articles/letting-garbage-collector-do-c

        /* memory check enabled? */
        if (!RTConfig.getBoolean(RTKey.OSTOOLS_MEMORY_CHECK_ENABLE)) {
            return;
        }

        /* get current memory usage */
        long nowTime = DateTime.getCurrentTimeSec();
        long maxB, usedB;
        long averUsedB = 0L, firstUsedB = 0L, firstTime = 0L;
        long count = 0L;
        Runtime rt = Runtime.getRuntime();
        synchronized (OSTools.memoryCheckLock) {
            // reset?
            if (reset) {
                // start over
                OSTools.firstMem_maxB    = 0L;
                OSTools.firstMem_usedB   = 0L;
                OSTools.firstMem_time    = 0L;
                OSTools.memoryCheckCount = 0L;
                OSTools.averMem_usedB    = 0L;
                OSTools.lastMem_usedB    = 0L;
            }
            // get memory usage
            maxB  = rt.maxMemory();
            usedB = rt.totalMemory() - rt.freeMemory();
            if (usedB <= 0L) {
                // unlikely, but we need to check anyway
                Print.logWarn("Memory usage <= 0? " + usedB + " bytes");
            } else {
                Print.sysPrintln("UsedB="+usedB +", lastUsedB="+OSTools.lastMem_usedB);
                if (usedB < OSTools.lastMem_usedB) {
                    // garbage collection has occurred
                    if ((OSTools.firstMem_time <= 0L) || (usedB < OSTools.firstMem_usedB)) {
                        // store results after first garbage collection
                        OSTools.firstMem_maxB    = maxB;      // should never change
                        OSTools.firstMem_usedB   = usedB;
                        OSTools.firstMem_time    = nowTime;
                        OSTools.memoryCheckCount = 0L;
                        OSTools.averMem_usedB    = 0L;
                    }
                    firstUsedB = OSTools.firstMem_usedB; // cache for use outside synchronized section
                    firstTime  = OSTools.firstMem_time;  // cache for use outside synchronized section
                    // average "trend"
                    if (OSTools.averMem_usedB <= 0L) {
                        // initialize average
                        OSTools.averMem_usedB = usedB;
                    } else
                    if (usedB <= OSTools.averMem_usedB) {
                        // always reset to minimum used (ie. 100% downward trend)
                        OSTools.averMem_usedB = usedB;
                    } else {
                        // upward "trend" determined by weighting factor
                        double trendWeight = RTConfig.getDouble(RTKey.OSTOOLS_MEMORY_TREND_WEIGHT);
                        OSTools.averMem_usedB = OSTools.averMem_usedB + (long)((double)(usedB - OSTools.averMem_usedB) * trendWeight);
                    }
                    averUsedB = OSTools.averMem_usedB; // cache for use outside synchronized section
                    // count
                    count = ++OSTools.memoryCheckCount; // increment and cache count for use outside synchronized section
                }
                OSTools.lastMem_usedB = usedB; // save last used
            }
        } // synchronized

        /* return if a garbage collection has not just occurred */
        if (count <= 0L) {
            return;
        }

        /* analyze */
        double deltaHours = (double)(nowTime - firstTime) / 3600.0;
        long   deltaUsedB = averUsedB - firstUsedB; // could be <= 0
        long   grwBPH     = (deltaHours > 0.0)? (long)(deltaUsedB / deltaHours) : 0L; // bytes/hour
        long   grwBPC     = deltaUsedB / count; // bytes/hour

        /* message */
        long maxK  = maxB      / 1024;
        long usedK = usedB     / 1024;
        long averK = averUsedB / 1024;
        String s = "["+count+"] Memory-K max "+maxK+ ", used "+usedK+ " (trend "+averK+ " K "+grwBPH+" b/h "+ grwBPC+" b/c)";

        /* display */
        double maxPercent = RTConfig.getDouble(RTKey.OSTOOLS_MEMORY_USAGE_WARN);
        if (usedB >= (long)((double)maxB * maxPercent)) {
            Print.logWarn("**** More than "+(maxPercent*100.0)+"% of max memory has been used!! ****");
            Print.logWarn(s);
        } else {
            Print.logInfo(s);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this implementation has a broken 'toFront' Swing implementation.<br>
    *** (may only be applicable on Java v1.4.2)
    *** @return True if this implementation has a broken 'toFront' Swing implementation.
    **/
    public static boolean isBrokenToFront()
    {
        return isWindows();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String PROPERTY_JAVA_HOME                   = "java.home";
    public static final String PROPERTY_JAVA_VENDOR                 = "java.vendor";
    public static final String PROPERTY_JAVA_SPECIFICATION_VERSION  = "java.specification.version";

    /**
    *** Returns true if executed from a Sun Microsystems JVM.
    *** @return True is executed from a Sun Microsystems JVM.
    **/
    public static boolean isSunJava()
    {
        String propVal = System.getProperty(PROPERTY_JAVA_VENDOR); // "Sun Microsystems Inc."
        if ((propVal == null) || (propVal.indexOf("Sun Microsystems") < 0)) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the class of the caller at the specified frame index
    *** @param frame The frame index
    *** @return The calling class
    **/
    @SuppressWarnings("proprietary")  // <-- does not work to supress the "Sun proprietary API" warning
    private static Class _getCallerClass(int frame)
        throws Throwable
    {
        return sun.reflect.Reflection.getCallerClass(frame + 1); // <== ignore any warnings
    }

    /**
    *** Gets the class of the caller at the specified frame index
    *** @param frame The frame index
    *** @return The calling class
    **/
    public static Class getCallerClass(int frame)
    {
        try {
            // sun.reflect.Reflection.getCallerClass(0) == sun.reflect.Reflection
            // sun.reflect.Reflection.getCallerClass(1) == OSTools
            Class clz = OSTools._getCallerClass(frame + 1);
            //Print._println("" + (frame + 1) + "] class " + StringTools.className(clz));
            return clz;
        } catch (Throwable th) { // ClassNotFoundException
            // This can occur when the code has been compiled with the Sun Microsystems version
            // of Java, but is executed with the GNU version of Java (or other non-Sun version).
            Print.logException("Sun Microsystems version of Java is not in use", th);
            return null;
        }
    }

    /**
    *** Returns true if 'sun.reflect.Reflection' is present in the runtime libraries.<br>
    *** (will return true when running with the Sun Microsystems version of Java)
    *** @return True if 'getCallerClass' is available.
    **/
    public static boolean hasGetCallerClass()
    {
        try {
            OSTools._getCallerClass(0);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    /**
    *** Prints the class of the caller (debug purposes only)
    **/
    public static void printCallerClasses()
    {
        try {
            for (int i = 0;; i++) {
                Class clz = OSTools._getCallerClass(i);
                Print.logInfo("" + i + "] class " + StringTools.className(clz));
                if (clz == null) { break; }
            }
        } catch (Throwable th) { // ClassNotFoundException
            // This can occur when the code has been compiled with the Sun Microsystems version
            // of Java, but is executed with the GNU version of Java.
            Print.logException("Sun Microsystems version of Java is not in use", th);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Process-ID of this JVM invocation.<br>
    *** IMPORTANT: This implementation relies on a "convention", rather that a documented method
    *** of obtaining the process-id of this JVM within the OS.  <b>Caveat Emptor!</b><br>
    *** (On Windows, this returns the 'WINPID' which is probably useless anyway)
    *** @return The Process-ID
    **/
    public static int getProcessID()
    {
        // References:
        //  - http://blog.igorminar.com/2007/03/how-java-application-can-discover-its.html
        if (OSTools.isSunJava()) {
            try {
                // by convention, returns "<PID>@<host>" (until something changes, and it doesn't)
                String n = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                int pid = StringTools.parseInt(n,-1); // parse PID
                return pid;
            } catch (Throwable th) {
                Print.logException("Unable to obtain Process ID", th);
                return -1;
            }
        } else {
            return -1;
        }
    }

    /* this does not work on Windows (and seems to return the wrong parent PID on Linux) */
    private static int _getProcessID()
    {
        try {
            String cmd[] = new String[] { "bash", "-c", "echo $PPID" };
            Process ppidExec = Runtime.getRuntime().exec(cmd);
            BufferedReader ppidReader = new BufferedReader(new InputStreamReader(ppidExec.getInputStream()));
            StringBuffer sb = new StringBuffer();
            for (;;) {
                String line = ppidReader.readLine();
                if (line == null) { break; }
                sb.append(StringTools.trim(line));
            }
            int pid = StringTools.parseInt(sb.toString(),-1);
            int exitVal = ppidExec.waitFor();
            Print.logInfo("Exit value: %d [%s]", exitVal, sb.toString());
            ppidReader.close();
            return pid;
        } catch (Throwable th) {
            Print.logException("Unable to obtain PID", th);
            return -1;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a Java command set up to be executed by Runtime.getRuntime().exec(...)
    *** @param classpath The classpath 
    *** @param className The main Java class name
    *** @param args The command line arguments
    *** @return A command to call and it's arguments
    **/
    public static String[] createJavaCommand(String classpath[], String className, String args[])
    {
        java.util.List<String> execCmd = new Vector<String>();
        execCmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        execCmd.add("-classpath");
        if (ListTools.isEmpty(classpath)) {
            execCmd.add(System.getProperty("java.class.path"));
        } else {
            StringBuffer sb = new StringBuffer();
            for (String p : classpath) {
                if (sb.length() > 0) { sb.append(File.pathSeparator); }
                sb.append(p);
            }
            execCmd.add(sb.toString());
        }
        execCmd.add(className);
        if (!ListTools.isEmpty(args)) {
            for (String a : args) {
                execCmd.add(a);
            }
        }
        return execCmd.toArray(new String[execCmd.size()]);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified object is an instance of (or equal to)
    *** the specified class name.
    **/
    public static boolean instanceOf(Object obj, String className)
    {
        if ((obj == null) || StringTools.isBlank(className)) {
            return false;
        } else {
            return StringTools.className(obj).equals(className);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sleeps for the specified number of milliseconds
    *** @param ms  Number of milliseconds to sleep
    *** @return True if sleep was performed without interruption, false otherwise
    **/
    public static boolean sleepMS(long ms)
    {
        if (ms < 0L) {
            return false;
        } else
        if (ms == 0L) {
            return true;
        } else {
            try {
                Thread.sleep(ms);
                return true;
            } catch (Throwable th) {
                return false;
            }
        }
    }

    /**
    *** Sleeps for the specified number of seconds
    *** @param sec  Number of milliseconds to sleep
    *** @return True if sleep was performed without interruption, false otherwise
    **/
    public static boolean sleepSec(long sec)
    {
        if (sec < 0L) {
            return false;
        } else
        if (sec == 0L) {
            return true;
        } else {
            try {
                Thread.sleep(sec * 1000L);
                return true;
            } catch (Throwable th) {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        RTConfig.setBoolean(RTKey.LOG_FILE_ENABLE,false);
        Print.setAllOutputToStdout(true);

        Print.sysPrintln("");
        Print.sysPrintln("Host ...");
        Print.sysPrintln("Host Name   : " + getHostName());
        Print.sysPrintln("Host IP     : " + getHostIP());

        Print.sysPrintln("");
        Print.sysPrintln("OS Type ...");
        Print.sysPrintln("Is Windows  : " + isWindows());
        Print.sysPrintln("Is Windows9X: " + isWindows9X());
        Print.sysPrintln("Is WindowsXP: " + isWindowsXP());
        Print.sysPrintln("Is Linux    : " + isLinux());
        Print.sysPrintln("Is MacOS    : " + isMacOS());
        Print.sysPrintln("Is MacOSX   : " + isMacOSX());

        Print.sysPrintln("");
        Print.sysPrintln("PID ...");
        Print.sysPrintln("PID #1      : " + getProcessID());
        Print.sysPrintln("PID #2      : " + _getProcessID());
        
        Print.sysPrintln("");
        Print.sysPrintln("Memory ...");
        Runtime rt = Runtime.getRuntime();
        Print.sysPrintln("Total Mem   : " + rt.totalMemory()/(1024.0*1024.0) + " Mb");
        Print.sysPrintln("Max Mem     : " + rt.maxMemory()/(1024.0*1024.0)   + " Mb");
        Print.sysPrintln("Free Mem    : " + rt.freeMemory()/(1024.0*1024.0)  + " Mb");
        
        RTConfig.setBoolean(RTKey.OSTOOLS_MEMORY_CHECK_ENABLE,true);
        OSTools.lastMem_usedB = 99999999L;
        Print.sysPrintln("MemoryCheck : " + RTConfig.getBoolean(RTKey.OSTOOLS_MEMORY_CHECK_ENABLE));
        OSTools.checkMemoryUsage(false);
        Print.sysPrintln("");

    }

}
