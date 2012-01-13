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
//  2009/11/10  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class WorkHours
{
    
    // ------------------------------------------------------------------------
    
    /**
    *** Day
    **/
    public static class Day
    {
        private byte todSeg[] = new byte[96];
        // default constructor
        public Day() {
            for (int i = 0; i < this.todSeg.length; i++) {
                this.todSeg[i] = (byte)0; // not a workday
            }
        }
        // time range constructor
        public Day(int todStart, int todEnd) {
            int tsNdx = todStart / 15;
            if (tsNdx < 0) { tsNdx = 0; }
            int teNdx = (todEnd - 1) / 15;
            if (teNdx >= this.todSeg.length) { teNdx = this.todSeg.length - 1; }
            for (int i = 0; i < this.todSeg.length; i++) {
                this.todSeg[i] = ((i >= tsNdx) && (i <= teNdx))? (byte)1 : (byte)0;
            }
        }
        // copy constructor
        public Day(Day day) {
            for (int i = 0; i < this.todSeg.length; i++) {
                this.todSeg[i] = (day != null)? day.todSeg[i] : (byte)1;
            }
        }
        // check for work-hour match
        public boolean isMatch(int tod) {
            int todNdx = tod / 15;
            if (todNdx < 0) { todNdx = 0; }
            if (todNdx >= this.todSeg.length) { todNdx = this.todSeg.length - 1; }
            return (this.todSeg[todNdx] != (byte)0);
        }
        // string representation
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < this.todSeg.length; i++) {
                sb.append((this.todSeg[i] != (byte)0)? "1" : "-");
            }
            return sb.toString();
        }
    }
   
    // ------------------------------------------------------------------------
    
    private Day dowDay[] = new Day[7];

    /**
    *** Constructor
    **/
    public WorkHours(Day day[])
    {
        Day last = !ListTools.isEmpty(day)? day[day.length - 1] : null;
        int dlen = !ListTools.isEmpty(day)? day.length : 0;
        for (int i = 0; i < this.dowDay.length; i++) {
            this.dowDay[i] = new Day((i < dlen)? day[i] : last);
        }
    }

    /**
    *** Constructor
    **/
    public WorkHours(String keyPrefix)
    {
        this(null, keyPrefix);
    }

    /**
    *** Constructor
    **/
    public WorkHours(RTConfig.PropertyGetter dayRTP)
    {
        this(dayRTP, null);
    }

    /**
    *** Constructor
    **/
    public WorkHours(RTConfig.PropertyGetter dayRTP, String keyPrefix)
    {
        
        /* init */
        keyPrefix = StringTools.trim(keyPrefix);
        if (dayRTP == null) { dayRTP = RTConfig.getPropertyGetter(); }
        
        /* init days */
        for (int i = 0; i < this.dowDay.length; i++) {
            // sun=08:00-17:00
            String dowKey = keyPrefix + DateTime.getDayName(i,1).toLowerCase();
            String timeRange = StringTools.trim(dayRTP.getProperty(dowKey,""));
            if (!StringTools.isBlank(timeRange)) {
                String tm[] = StringTools.split(timeRange,'-');
                if (tm.length == 2) {
                    //Print.logInfo("Range: " + tm[0] + "," + tm[1]);
                    String frTm[] = StringTools.split(tm[0],':');
                    int frTod = (frTm.length >= 2)? ((StringTools.parseInt(frTm[0],0)*60)+StringTools.parseInt(frTm[1],0)) : 0;
                    //Print.logInfo("From : " + frTod);
                    String toTm[] = StringTools.split(tm[1],':');
                    int toTod = (toTm.length >= 2)? ((StringTools.parseInt(toTm[0],0)*60)+StringTools.parseInt(toTm[1],0)) : 1440;
                    //Print.logInfo("To   : " + toTod);
                    this.dowDay[i] = new Day(frTod, toTod);
                } else {
                    // invalid time specification
                    this.dowDay[i] = new Day();
                }
            } else {
                this.dowDay[i] = new Day();
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** True if the specified DateTime is within the current 'WorkingHours'
    **/
    public boolean isMatch(DateTime dateTime)
    {
        return this.isMatch(dateTime, null);
    }

    /**
    *** True if the specified DateTime is within the current 'WorkingHours'
    **/
    public boolean isMatch(DateTime dateTime, TimeZone tz)
    {
        if (dateTime != null) {
            int dow = dateTime.getDayOfWeek(tz);
            Day day = this.dowDay[dow];
            int tod = (dateTime.getHour24(tz) * 60) + dateTime.getMinute(tz);
            return day.isMatch(tod);
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String repreentation of this instance
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15  16  17  18  19  20  21  22  23  \n");
        sb.append("---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+\n");
        for (int i = 0; i < this.dowDay.length; i++) {
            sb.append(this.dowDay[i].toString());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_TIME[]   = new String[] { "time" };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String time = RTConfig.getString(ARG_TIME,"");
        try {
            DateTime dt = !StringTools.isBlank(time)? new DateTime(time) : new DateTime();
            WorkHours wh = new WorkHours(RuleFactory.PROP_rule_workHours_);
            Print.sysPrintln(dt.toString());
            Print.sysPrintln("IsMatch " + wh.isMatch(dt));
            Print.sysPrintln("WorkHours:");
            Print.sysPrintln(wh.toString());
        } catch (DateTime.DateParseException dpe) {
            Print.sysPrintln("Error: Unable to parse time - " + time);
        }
    }
    
}
