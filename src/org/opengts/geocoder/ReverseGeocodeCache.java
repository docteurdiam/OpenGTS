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
//  2009/12/16  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import java.util.*;

import org.opengts.util.*;

public class ReverseGeocodeCache
{

    // ------------------------------------------------------------------------

    private static final long   DEFAULT_MAX_AGE_SEC     = DateTime.MinuteSeconds(60);
    private static final int    DEFAULT_MAX_SIZE        = 1000;
    private static final String GEOPOINT_DECIMAL        = "4";
    
    private static  boolean     DEBUG                   = false;

    // ------------------------------------------------------------------------
    
    private static String formatGeoPoint(GeoPoint gp)
    {
        StringBuffer sb = new StringBuffer();
        String fmt = GEOPOINT_DECIMAL;
        sb.append(GeoPoint.formatLatitude( gp.getLatitude() , fmt, null));
        sb.append(GeoPoint.PointSeparator);
        sb.append(GeoPoint.formatLongitude(gp.getLongitude(), fmt, null));
        return sb.toString();
    }
    
    private static long currentTimeSec()
    {
        if (DEBUG) {
            return System.currentTimeMillis();
        } else {
            return System.currentTimeMillis() / 1000L;
        }
    }

    // ------------------------------------------------------------------------

    public static class RGItem
    {
        private long            timestamp = 0L;
        private ReverseGeocode  revGeocode = null;
        public RGItem(ReverseGeocode rg) {
            this.revGeocode = rg;
            this.updateTimestamp();
        }
        public long getTimestamp() {
            return this.timestamp;
        }
        public void updateTimestamp() {
            this.timestamp = currentTimeSec();
        }
        public ReverseGeocode getReverseGeocode() {
            return this.revGeocode;
        }
    }
    
    // ------------------------------------------------------------------------
    
    private Map<String,RGItem>  rgCacheMap       = new HashMap<String,RGItem>();
    private int                 maxCacheSize     = DEFAULT_MAX_SIZE;
    private long                maxAgeSec        = DEFAULT_MAX_AGE_SEC;

    private Object              mapLock          = new Object();
    private int                 readLockCount    = 0;
    private int                 writeLockCount   = 0;

    public ReverseGeocodeCache()
    {
        this(DEFAULT_MAX_SIZE, DEFAULT_MAX_AGE_SEC);
    }

    public ReverseGeocodeCache(int maxSize, long maxAge)
    {
        super();
        this.setMaxSize(maxSize);
        this.setMaxAgeSec(maxAge);
    }

    // ------------------------------------------------------------------------

    public void setMaxSize(int maxSize)
    {
        if (maxSize <= 0) {
            this.maxCacheSize = DEFAULT_MAX_SIZE;
        } else {
            this.maxCacheSize = (maxSize < 100)? 100 : maxSize;
        }
    }
    
    public int getMaxSize()
    {
        return this.maxCacheSize;
    }

    // ------------------------------------------------------------------------

    public void setMaxAgeSec(long maxAge)
    {
        this.maxAgeSec = (maxAge > 0L)? maxAge : DEFAULT_MAX_AGE_SEC;
    }
    
    public long getMaxAgeSec()
    {
        return this.maxAgeSec;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean getReadLock()
    {
        boolean rtn = false;
        synchronized (this.mapLock) {
            if (this.writeLockCount == 0) {
                this.readLockCount++;
                rtn = true;
            }
        }
        return rtn;
    }

    private boolean releaseReadLock()
    {
        boolean rtn = false;
        synchronized (this.mapLock) {
            if (this.readLockCount > 0) {
                this.readLockCount--;
                rtn = true;
            } else {
                Print.logStackTrace("Read lock released, with no active lock");
            }
        }
        return rtn;
    }

    public ReverseGeocode getReverseGeocode(GeoPoint gp)
    {
        ReverseGeocode rg = null;

        /* get ReverseGeocode */
        if ((gp != null) && gp.isValid()) {
            boolean readLocked = false;
            try {
                readLocked = this.getReadLock();
                if (readLocked) {
                    RGItem rgi = this.rgCacheMap.get(formatGeoPoint(gp));
                    if (rgi != null) {
                        rgi.updateTimestamp();
                        rg = rgi.getReverseGeocode();
                    }
                }
            } finally {
                if (readLocked) {
                    this.releaseReadLock();
                }
            }
        }

        /* return */
        return rg;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean getWriteLock()
    {
        boolean rtn = false;
        synchronized (this.mapLock) {
            if ((this.writeLockCount == 0) && (this.readLockCount == 0)) {
                this.writeLockCount++;
                rtn = true;
            }
        }
        return rtn;
    }

    private boolean releaseWriteLock()
    {
        boolean rtn = false;
        synchronized (this.mapLock) {
            if (this.writeLockCount > 0) {
                this.writeLockCount--;
                rtn = true;
            } else {
                Print.logStackTrace("Write lock released, with no active lock");
            }
        }
        return rtn;
    }

    public boolean addReverseGeocode(GeoPoint gp, ReverseGeocode rg)
    {
        boolean rtn = false;
        if ((gp != null) && gp.isValid() && (rg != null)) {
            boolean writeLocked = false;
            try {
                writeLocked = this.getWriteLock();
                if (writeLocked) {
                    this.rgCacheMap.put(formatGeoPoint(gp), new RGItem(rg));
                    rtn = true;
                    int rgSize = this.rgCacheMap.size();
                    if (rgSize > this.maxCacheSize) {
                        if (DEBUG) { Print.logInfo("\n\nTrimming cache ..."); }
                        long maxTime = currentTimeSec() - this.maxAgeSec;
                        for (Iterator<String> i = this.rgCacheMap.keySet().iterator(); i.hasNext();) {
                            String key = i.next();
                            RGItem rgi = this.rgCacheMap.get(key);
                            if (rgi.getTimestamp() <= maxTime) {
                                i.remove();
                            }
                        }
                        if (DEBUG) { Print.logInfo("Old size="+rgSize +" New size="+this.rgCacheMap.size()); }
                        if (this.rgCacheMap.size() > this.maxCacheSize) {
                            Print.logWarn("Unable to trim ReverseGeocodeCache entries: " + this.rgCacheMap.size());
                            long minSec = DateTime.MinuteSeconds(15);
                            if (this.maxAgeSec > (2L * minSec)) {
                                this.maxAgeSec -= minSec;
                            } else {
                                this.maxAgeSec = this.maxAgeSec / 2L;
                            }
                        }
                    }
                } else {
                    Print.logInfo("Unable to obtain write lock");
                }
            } finally {
                if (writeLocked) {
                    this.releaseWriteLock();
                }
            }
        }
        return rtn;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        
        ReverseGeocodeCache rgc = new ReverseGeocodeCache();
        DEBUG = true;
        
        double baseLat = 39.0000;
        double baseLon = -142.0000;
        Random rand = new Random();
        
        for (;;) {
            double lat = baseLat + ((double)rand.nextInt(100) / 100.0);
            double lon = baseLon + ((double)rand.nextInt(100) / 100.0);
            GeoPoint gp = new GeoPoint(lat, lon);
            
            ReverseGeocode rg = rgc.getReverseGeocode(gp);
            if (rg != null) {
                Print.sysPrintln("Found RG: " + gp);
            } else {
                rgc.addReverseGeocode(gp, new ReverseGeocode());
                //Print.sysPrintln("Added RG: " + gp);
            }
            
        }
        
    }
    
}
