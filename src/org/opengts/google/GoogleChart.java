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
*** Tools for obtaining various charts via Google Chart API
**/

public class GoogleChart
{

    // ------------------------------------------------------------------------

    public   static final String CHART_API_URL = "http://chart.apis.google.com/chart?";
    
    // ------------------------------------------------------------------------

    // encode values betwen 0 and 4095 inclusive (a base-64 number with the following digits):
    protected static final String Encoding[] = {
        "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
        "a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z",
        "0","1","2","3","4","5","6","7","8","9",
        "-",".",
    };
    
    // ------------------------------------------------------------------------

    public static String GetSimpleEncodedValue(int val)
    {
        if ((val >= 0) && (val <= 61)) {
            return Encoding[val];
        } else {
            return "_";
        }
    }
    
    public static String GetScaledSimpleEncodedValue(double val, double minVal, double maxVal)
    {
        int n = (int)Math.round(((val - minVal) / (maxVal - minVal)) * 61.0);
        return GetSimpleEncodedValue(n);
    }

    public static StringBuffer GetScaledSimpleEncodedValue(StringBuffer sb, double val[], double minVal, double maxVal)
    {
        if (sb == null) { sb = new StringBuffer(); }
        for (int i = 0; i < val.length; i++) {
            sb.append(GetScaledSimpleEncodedValue(val[i], minVal, maxVal));
        }
        return sb;
    }

    // ------------------------------------------------------------------------

    public static StringBuffer GetExtendedEncodedValue(StringBuffer sb, int val)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if ((val >= 0) && (val <= 4095)) {
            sb.append(Encoding[val / 64]).append(Encoding[val % 64]);
            return sb;
        } else {
            sb.append("__"); // missing
            return sb;
        }
    }
    
    public static StringBuffer GetScaledExtendedEncodedValue(StringBuffer sb, double val, double minVal, double maxVal)
    {
        int n = (int)Math.round(((val - minVal) / (maxVal - minVal)) * 4095.0);
        return GetExtendedEncodedValue(sb, n);
    }

    public static StringBuffer GetScaledExtendedEncodedValue(StringBuffer sb, double val[], double minVal, double maxVal)
    {
        if (sb == null) { sb = new StringBuffer(); }
        for (int i = 0; i < val.length; i++) {
            GetScaledExtendedEncodedValue(sb, val[i], minVal, maxVal);
        }
        return sb;
    }

    // ------------------------------------------------------------------------

    /* return URL for a circular marker (uses single-dataset venn diragram) */
    // http://chart.apis.google.com/chart?cht=v&ext=.png&chs=15x15&chf=a,s,FFFFFFFF&chco=FF0000FF&chd=t:50
    public static String getCircleMarkerURL(int W, Color color)
    {
        int H = W;
        String C = ColorTools.toHexString(color,false);
        return CHART_API_URL + "cht=v&ext=.png&chs="+W+"x"+H+"&chf=a,s,FFFFFFFF&chco="+C+"FF&chd=t:50";
    }

    /* return URL for a pushpin marker */
    // http://chart.apis.google.com/chart?cht=mm&ext=.png&chs=16x16&chco=FF0000FF,FF0000FF,000000FF
    public static String getPushpinMarkerURL(int W, int H, Color color)
    {
        String C = ColorTools.toHexString(color,false);
        return CHART_API_URL + "cht=mm&ext=.png&chs="+W+"x"+H+"&chco="+C+"FF,"+C+"FF,000000FF";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected String    cht             = "lc";             // chart type
    protected String    chs             = "700x400";        // chart size

    protected String    chg             = "";               // horizontal grid
    
    protected String    chxt            = "y,x";            // axis labels
    protected String    chxl            = "";               // axis labels

    protected String    chts            = "000000,15";      // title color,fontSize
    protected String    chtt            = "Chart";          // title text
    
    protected String    chco            = "";               // data set color
    protected String    chm             = "";               // shape markers
    protected String    chdl            = "";               // data set legend
    
    protected int       dataSetCount    = 0;
    protected String    chd             = "";               // data set data

    public GoogleChart()
    {
        //
    }
        
    // ------------------------------------------------------------------------
    
    public void setType(String type)
    {
        this.cht = type;
    }

    // ------------------------------------------------------------------------
    
    public void setSize(int w, int h)
    {
        this.chs = w + "x" + h;
    }

    // ------------------------------------------------------------------------

    public void setTitle(Color color, int fontSize, String text)
    {
        if (color == null) { color = Color.black; }
        this.chts = ColorTools.toHexString(color,false) + "," + fontSize;
        this.chtt = text.replace(' ','+');
    }
    
    public void appendTitle(String text)
    {
        if (this.chtt == null) { this.chtt = ""; }
        this.chtt += text.replace(' ','+');
    }

    // ------------------------------------------------------------------------

    public void addShapeMarker(String marker)
    {
        if (this.chm == null) { this.chm = ""; }
        if (!StringTools.isBlank(this.chm)) { this.chm += "|"; }
        this.chm += marker;
    }

    // ------------------------------------------------------------------------

    public void addDatasetColor(String hexColor)
    {
        if (this.chco == null) { this.chco = ""; }
        if (!StringTools.isBlank(this.chco)) { this.chco += ","; }
        this.chco += hexColor;
    }

    // ------------------------------------------------------------------------

    public void addDatasetLegend(String legend)
    {
        if (this.chdl == null) { this.chdl = ""; }
        if (!StringTools.isBlank(this.chdl)) { this.chdl += "|"; }
        this.chdl += legend.replace(' ','+');
    }

    // ------------------------------------------------------------------------

    public void setGrid(int xCount, int yCount)
    {
        this.chg = String.valueOf(xCount) + "," + String.valueOf(yCount) + ",5,5";
    }

    // ------------------------------------------------------------------------

    public void setAxisLabels(String type, String value)
    {
        this.chxt = type;
        this.chxl = value;
    }
    
    // ------------------------------------------------------------------------

}
