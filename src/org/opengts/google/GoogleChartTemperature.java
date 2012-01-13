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
// References:
// http://www.collegeathome.com/blog/2008/06/05/50-cool-things-you-can-do-with-google-charts-api/
// http://psychopyko.com/tutorial/how-to-use-google-charts/
// ----------------------------------------------------------------------------
// Temperature Examples:
// http://chart.apis.google.com/chart?cht=lc&chs=708x200&chxt=y,x,x&chts=000000,15&chco=008000,FFA500,FF0000&chdl=Dew+Point+(F)|Apparent+Temperature+(F)|Temperature+(F)&chd=e:oaoaeUeUa8XlXlQ2NeNeNeNeNeKHKHDYAA,....ryhra8XlXlry8n8nhrXlUNQ2Nehryh,....ryhra8XlXlry8n8nhrXlUNQ2Nehryh&chxl=0:|47F|49F|52F|54F|57F|59F|61F|64F|66F|1:|2PM|5PM|8PM|11PM|2AM|4AM|7AM|10AM|1PM|4PM|7PM|10PM|1AM|4AM|7AM|10AM|1PM|2:|Sat|+|+|+|Sun|+|+|+|+|+|+|+|Mon|+|+|+|+&chtt=Temperature,+Dew+Point+and+Apparent+Temperature
// http://chart.apis.google.com/chart?chxt=y%2Cr%2Cx&chs=250x100&cht=lc&chxp=0%2C10%2C32%2C50%2C70|1%2C49%2C25|2%2C0%2C3%2C6%2C9&chxl=0%3A|10|32|50|70|1%3A|Sunnyvale|Chicago|2%3A|Jan|Apr|Jul|Sep&chxr=0%2C0%2C80|1%2C0%2C80|2%2C0%2C12&chd=s%3Aloqsvy00yvpll%2CTYemu154yqgXT&chls=1%2C1%2C0|1%2C8%2C4&chco=0000ff%2Cff0000&wiki=foo.png
// ----------------------------------------------------------------------------
// Change History:
//  2008/12/01  Martin D. Flynn
//     -Initial release
//  2009/04/02  Martin D. Flynn
//     -Repackaged
// ----------------------------------------------------------------------------
package org.opengts.google;

import java.util.*;
import java.io.*;
import java.awt.*;

import org.opengts.util.*;

/**
*** Tools for obtaining Temperature charts via Google Chart API
**/

public class GoogleChartTemperature
    extends GoogleChart
{

    // ------------------------------------------------------------------------

    protected static final long   MIN_DATE      = 1L;

    // ------------------------------------------------------------------------
    
    public    static final int    TEMP_F        = 0;
    public    static final int    TEMP_C        = 1;

    private static double F2C(double F)
    {
        return (F - 32.0) * 5 / 9;
    }

    private static double C2F(double C)
    {
        return (C * 9 / 5) + 32.0;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static class Data
    {
        private long    timestamp = 0L;
        private double  tempC = -999.0;
        private String  stringVal = null;
        public Data(long ts, double C) {
            this.timestamp = ts;
            this.tempC = C;
        }
        public double getTempC() {
            return this.tempC;
        }
        public long getTimestamp() {
            return this.timestamp;
        }
        public String toString() {
            if (this.stringVal == null) {
                this.stringVal = String.valueOf(this.getTimestamp()) + "," + this.getTempC();
            }
            return this.stringVal;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int         dispUnits       = TEMP_F;           // 0 == F, 1 == C
    private double      minTempC        = -17.77777;        // ==   0.00000F
    private double      maxTempC        =  54.44444;        // == 130.00000F
    private int         yTickCount      = 10;
    
    private long        minDateTS       = 0L;
    private long        maxDateTS       = 0L;
    private TimeZone    timeZone        = null;
    private String      dateFormat      = null;
    private String      timeFormat      = null;
    private int         xTickCount      = 10;
    
    private boolean     didInitChart    = false;

    public GoogleChartTemperature()
    {
        this.setType("lxy");
    }

    // ------------------------------------------------------------------------
    
    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }
    
    public String getDateFormat()
    {
        return StringTools.isBlank(this.dateFormat)? this.dateFormat : "MM/dd";
    }

    // ------------------------------------------------------------------------
    
    public void setTimeFormat(String timeFormat)
    {
        this.timeFormat = timeFormat;
    }
    
    public String getTimeFormat()
    {
        return StringTools.isBlank(this.timeFormat)? this.timeFormat : "HH:mm:ss";
    }

    // ------------------------------------------------------------------------

    public void setDateRange(
        DateTime minDate, DateTime maxDate,
        int tickCount)
        throws Exception
    {
        
        /* validate dates */
        long minDateTS = (minDate != null)? minDate.getTimeSec() : 0L;
        long maxDateTS = (maxDate != null)? maxDate.getTimeSec() : 0L;
        if ((minDate == null)               ||
            (minDateTS < MIN_DATE)          ||
            (maxDate == null)               ||
            (maxDateTS < MIN_DATE)          ||
            (minDateTS >= maxDateTS)        ||
            ((maxDateTS - minDateTS) <= 60L)    ) {
            throw new Exception("Invalid Date range specification");
        }
        
        /* adjust 'maxDateTS' to make sure (maxDateTS - minDateTS) is a multiple of 'tickCount' */
        maxDateTS += (maxDateTS - minDateTS) % tickCount;

        /* vars */
        this.minDateTS  = minDateTS;
        this.maxDateTS  = maxDateTS;
        this.timeZone   = minDate.getTimeZone();
        this.xTickCount = (tickCount > 0)? tickCount : 6;

    }
    
    public int getDateTickCount()
    {
        return this.xTickCount;
    }

    // ------------------------------------------------------------------------

    public void setTemperatureRange(int units,
        double minTempC, double maxTempC,
        int tickCount)
        throws Exception
    {

        /* validate temperature range */
        if ((minTempC < -40.0) || (minTempC > 125.0) ||
            (maxTempC < -40.0) || (maxTempC > 125.0) ||
            (minTempC > maxTempC)                       ) {
            throw new Exception("Invalid Temperature range specification");
        } else
        if (minTempC == maxTempC) {
            minTempC -= 1.0;
            maxTempC += 1.0;
        }

        /* vars */
        this.dispUnits  = (units == TEMP_C)? TEMP_C : TEMP_F;
        this.minTempC   = minTempC;
        this.maxTempC   = maxTempC;
        this.yTickCount = (tickCount > 0)? tickCount : 10;
        
        /* suplement chart title */
        this.appendTitle((this.dispUnits == TEMP_C)? " (C)" : " (F)");

    }
    
    public int getTemperatureTickCount()
    {
        return this.yTickCount;
    }

    // ------------------------------------------------------------------------
    
    protected void _initChart()
        throws Exception
    {

        /* already initialized? */
        if (this.didInitChart) {
            return;
        }

        /* axis tick counts */
        int yTickCnt = this.getTemperatureTickCount();
        int xTickCnt = this.getDateTickCount();

        /* horizontal grid */
        this.setGrid(0, yTickCnt);

        /* Y-axis labels */
        StringBuffer ya = new StringBuffer();
        double deltaC = this.maxTempC - this.minTempC;
        for (int y = 0; y <= yTickCnt; y++) {
            double C = this.minTempC + (deltaC * ((double)y / (double)yTickCnt));
            double v = (this.dispUnits == TEMP_C)? C : C2F(C);
            ya.append("|").append(StringTools.format(v,"0.0"));
        }
        if ((this.maxTempC > 0.0) && (this.minTempC < 0.0)) {
            double sep = Math.abs(this.minTempC) / (this.maxTempC - this.minTempC);
            this.addShapeMarker("r,AA4444,0," + StringTools.format(sep,"0.000") + "," + StringTools.format(sep+0.002,"0.000"));
        }

        /* X-axis labels */
        StringBuffer xat = new StringBuffer();
        StringBuffer xad = new StringBuffer();
        double deltaTS = (double)(this.maxDateTS - this.minDateTS);
        long lastDN = 0L;
        for (int x = 0; x <= xTickCnt; x++) {
            long ts = this.minDateTS + Math.round(deltaTS * ((double)x / (double)xTickCnt));
            DateTime dt = new DateTime(ts, this.timeZone);
            long dn = DateTime.getDayNumberFromDate(dt);
            xat.append("|").append(dt.format(this.getTimeFormat()));
            xad.append("|").append(dt.format(this.getDateFormat()));
            if (dn != lastDN) {
                long ds = dt.getDayStart();
                if (ds > this.minDateTS) {
                    double sep = (double)(ds - this.minDateTS) / deltaTS;
                    this.addShapeMarker("R,444444,0," + StringTools.format(sep,"0.000") + "," + StringTools.format(sep+0.001,"0.000"));
                }
                lastDN = dn;
            }
        }

        /* axis labels */
        this.setAxisLabels("y,x,x", "0:"+ya.toString()+"|1:"+xad.toString()+"|2:"+xat.toString());

        /* did init */
        this.didInitChart = true;

    }

    // ------------------------------------------------------------------------

    public void addDataSet(Color color, String legend, Data data[])
        throws Exception
    {
        
        /* init */
        this._initChart();
        
        /* dataset color/legend/markers */
        String hexColor = ColorTools.toHexString(color,false);
        this.addDatasetColor(hexColor);
        this.addDatasetLegend(legend);
        this.addShapeMarker("d," + hexColor + "," + this.dataSetCount + ",-1,7,1");

        /* data */
        StringBuffer xv = new StringBuffer();
        StringBuffer yv = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            GetScaledExtendedEncodedValue(yv,data[i].getTempC(),this.minTempC,this.maxTempC);
            GetScaledExtendedEncodedValue(xv,data[i].getTimestamp(),this.minDateTS,this.maxDateTS);
        }
        if (StringTools.isBlank(this.chd)) { this.chd = "e:"; } else { this.chd += ","; }
        this.chd += xv.toString() + "," + yv.toString();
        
        /* count data set */
        this.dataSetCount++;

    }

    // ------------------------------------------------------------------------
    
    private static Color TEMP_COLOR[] = new Color[] { Color.red, Color.green, Color.blue, Color.cyan, Color.gray };

    @SuppressWarnings("unchecked")
    public void _addRandomSampleData(int setCount, int tempCount)
        throws Exception
    {
        long   sts = this.minDateTS;
        long   ets = this.maxDateTS;
        Random ran = new Random(sts);

        /* init datasets */
        if (setCount <= 0) { setCount = 1; }
        java.util.List<Data> dataSet[] = new java.util.List[setCount];
        for (int d = 0; d < dataSet.length; d++) {
            dataSet[d] = new Vector<Data>();
        }

        /* populate random temperature data */
        double rangeC = this.maxTempC - this.minTempC;
        long deltaSize = (ets - sts) / (long)tempCount;
        long deltaRangeTS = DateTime.HourSeconds(3);
        long ts = sts + (deltaSize / 2L);
        double Cs = (ran.nextDouble() * rangeC * 0.10) + (rangeC * 0.05);
        for (int t = 0; t < tempCount; t++) {
            double C[] = new double[dataSet.length];
            for (int d = 0; d < dataSet.length; d++) {
                C[d] = (ran.nextDouble() * 7.0) + ((d==0)?Cs:C[d-1]) - 2.5;
                if (C[d] < this.minTempC) { C[d] = this.minTempC; }
                if (C[d] > this.maxTempC) { C[d] = this.maxTempC; }
                dataSet[d].add(new Data(ts,C[d]));
            }
            ts = sts + ((long)(t+1) * deltaSize) + (long)ran.nextInt((int)deltaRangeTS) + (deltaRangeTS / 2L);
            if (ts > ets) { ts = ets - 1L; }
            //ts = sts + ((t==0)?DateTime.HourSeconds(1):(long)ran.nextInt((int)(ets - sts)));
            Cs = C[0];
        }

        /* add datasets */
        for (int d = 0; d < dataSet.length; d++) {
            ListTools.sort(dataSet[d],null);
            Color color = TEMP_COLOR[d % TEMP_COLOR.length];
            this.addDataSet(color, "Temp " + (d+1), dataSet[d].toArray(new Data[dataSet[d].size()]));
        }

    }
    
    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(CHART_API_URL);
        sb.append("uniq=").append(DateTime.getCurrentTimeSec()).append("&");
        sb.append("cht=" ).append(this.cht ).append("&");
        sb.append("chs=" ).append(this.chs ).append("&");
        sb.append("chxt=").append(this.chxt).append("&");
        sb.append("chg=" ).append(this.chg ).append("&");
        sb.append("chxl=").append(this.chxl).append("&");
        sb.append("chts=").append(this.chts).append("&");
        sb.append("chtt=").append(this.chtt).append("&");
        sb.append("chco=").append(this.chco).append("&");
        sb.append("chdl=").append(this.chdl).append("&");
        sb.append("chd=" ).append(this.chd ).append("&");
        sb.append("chm=" ).append(this.chm );
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        long now = DateTime.getCurrentTimeSec();
        long sts = now - DateTime.DaySeconds(3);
        long ets = now;
        
        GoogleChartTemperature gct = new GoogleChartTemperature();
        try {
            
            gct.setSize(700, 400);
            gct.setTitle(Color.black, 16, "Temperature");
            gct.setTemperatureRange(1, F2C(0.0), F2C(130.0), 10);
            gct.setDateRange(new DateTime(sts), new DateTime(ets), 8);
            gct.setDateFormat("MM/dd");
            gct.setTimeFormat("HH:mm:ss");
            
            int setCount  = 3;
            int tempCount = 15;
            gct._addRandomSampleData(setCount,tempCount);
            System.out.println(gct.toString());

        } catch (Throwable th) {
            Print.logException("Error", th);
            System.exit(1);
        }
        
    }

}
