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
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/30  Martin D. Flynn
//     -Added Calendar style sheets
//  2008/07/08  Martin D. Flynn
//     -Separated js/css sections into separate files.
//  2008/08/15  Martin D. Flynn
//     -Moved Calendar handing (next/previous months) to 'Calendar.js'
//  2008/09/01  Martin D. Flynn
//     -Fixed Locale issues that prevented month abbreviations from being translated.
//  2008/09/12  Martin D. Flynn
//     -Added hour decrement.
//  2008/10/16  Martin D. Flynn
//     -Added ability to enter hour:minute via a standard text field
//     -"formatYMD" renamed to "formatArgDateTime".  Returned value now includes the
//      formated hours:minutes:seconds.
// ----------------------------------------------------------------------------
package org.opengts.war.track;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;

public class Calendar
{

    // ------------------------------------------------------------------------

    public  static final String  CLASS_CAL_DIV              = "calDiv";

    public  static final String  ID_CAL_DIV                 = "calDiv";
    public  static final String  ID_CAL_BOTTOM              = "calBottom";

    public  static final String  PARM_DATE                  = "date";
    public  static final String  PARM_RANGE_FR[]            = new String[] { PARM_DATE + "_fr"  , "fr"  };  // "date_fr"
    public  static final String  PARM_RANGE_FR2[]           = new String[] { PARM_RANGE_FR + "2", "fr2" };  // "date_fr2"
    public  static final String  PARM_RANGE_TO[]            = new String[] { PARM_DATE + "_to"  , "to"  };  // "date_to"
    public  static final String  PARM_RANGE_TO2[]           = new String[] { PARM_RANGE_TO + "2", "tz"  };  // "date_to2"

    public  static final String  PARM_TIMEZONE[]            = new String[] { PARM_DATE + "_tz"  , "tz"  };    // "date_tz"

    // ------------------------------------------------------------------------

    public enum Action implements EnumTools.IntValue {
        FIXED    (0),
        FADE     (1),
        SWITCH   (2),
        POPUP    (3);
        // ---
        private int vv = 0;
        Action(int v)                       { vv = v; }
        public int     getIntValue()        { return vv; }
    };
    
    public static Action getCalendarAction(String action)
    {
        String act = StringTools.trim(action).toLowerCase();
        if (StringTools.isBlank(act) || act.equals("default")) {
            return Action.FIXED;
        } else
        if (act.equals("fixed")) {
            return Action.FIXED;
        } else
        if (act.equals("fade") || act.equals("transition")) {
            return Action.FADE;
        } else
        if (act.equals("switch")) {
            return Action.SWITCH;
        } else
        if (act.equals("popup")) {
            return Action.POPUP;
        } else {
            return Action.FIXED;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String[] _GetDayNames(I18N i18n)
    {
        return new String[] {
            i18n.getString("Calendar.Su","Su"),
            i18n.getString("Calendar.Mo","Mo"),
            i18n.getString("Calendar.Tu","Tu"),
            i18n.getString("Calendar.We","We"),
            i18n.getString("Calendar.Th","Th"),
            i18n.getString("Calendar.Fr","Fr"),
            i18n.getString("Calendar.Sa","Sa"),
        };
    }

    private static String _GetMonthName(I18N i18n, int ndx1)
    {
        switch (ndx1) {
            case  1:    return i18n.getString("Calendar.Jan","Jan");
            case  2:    return i18n.getString("Calendar.Feb","Feb");
            case  3:    return i18n.getString("Calendar.Mar","Mar");
            case  4:    return i18n.getString("Calendar.Apr","Apr");
            case  5:    return i18n.getString("Calendar.May","May");
            case  6:    return i18n.getString("Calendar.Jun","Jun");
            case  7:    return i18n.getString("Calendar.Jul","Jul");
            case  8:    return i18n.getString("Calendar.Aug","Aug");
            case  9:    return i18n.getString("Calendar.Sep","Sep");
            case 10:    return i18n.getString("Calendar.Oct","Oct");
            case 11:    return i18n.getString("Calendar.Nov","Nov");
            case 12:    return i18n.getString("Calendar.Dec","Dec");
            default:    return "?";
        }
    }

    // ------------------------------------------------------------------------

    public static DateTime getCurrentDayStart(TimeZone tz)
    {
        DateTime dt = new DateTime(tz);
        dt.setTimeSec(dt.getDayStart());
        return dt;
    }

    public static DateTime getCurrentDayEnd(TimeZone tz)
    {
        DateTime dt = new DateTime(tz);
        dt.setTimeSec(dt.getDayEnd());
        return dt;
    }

    // ------------------------------------------------------------------------
    // date formatting

    private static String _formatYMD(int yr, int mo1, int dy, int hh, int mm, int ss)
    {
        // YYYY/MM/DD
        StringBuffer sb = new StringBuffer();
        if (yr > 0) {
            sb.append(StringTools.format(yr,"0000"));
        }
        if (mo1 > 0) {
            sb.append("/");
            sb.append(StringTools.format(mo1,"00"));
            if (dy > 0) {
                sb.append("/");
                sb.append(StringTools.format(dy,"00"));
                if (hh >= 0) {
                    sb.append("/");
                    sb.append(StringTools.format(hh,"00"));
                    if (mm >= 0) {
                        sb.append(":");
                        sb.append(StringTools.format(mm,"00"));
                        if (ss >= 0) {
                            sb.append(":");
                            sb.append(StringTools.format(ss,"00"));
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String formatArgYMD(DateTime dt)
    {
        if (dt != null) {
            return _formatYMD(dt.getYear(), dt.getMonth1(), dt.getDayOfMonth(), -1, -1, -1);
        } else {
            return "";
        }
    }

    public static String formatArgDateTime(DateTime dt)
    {
        if (dt != null) {
            return _formatYMD(dt.getYear(), dt.getMonth1(), dt.getDayOfMonth(), dt.getHour24(), dt.getMinute(), dt.getSecond());
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // parse date/time

    /* parse date (may return null) */
    public static DateTime parseDate(String dateStr, TimeZone tz, boolean isToDate)
    {
        // YYYY/MM[/DD[/hh[:mm[:ss]]]]  (ie. YYYY/MM/DD/hh:mm:ss)
        try {
            return DateTime.parseArgumentDate(dateStr, tz, isToDate);
        } catch (DateTime.DateParseException dpe) {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // write calendar Style
    
    public static void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        WebPageAdaptor.writeCssLink(out, reqState, "Calendar.css", null);
    }

    // ------------------------------------------------------------------------
    // write calendar JavaScript

    public static void writeNewCalendar(PrintWriter out, String calIDVar, String calFormID, String calTitle, DateTime calDate)
        throws IOException
    {
        int year = calDate.getYear();
        int mon1 = calDate.getMonth1();
        int day  = calDate.getDayOfMonth();
        int hour = calDate.getHour24();
        int min  = calDate.getMinute();
        String form = !StringTools.isBlank(calFormID)? ("'"+calFormID+"'") : "null";
        out.write("var "+calIDVar+" = new Calendar('"+calIDVar+"',"+form+",'"+calTitle+"',"+year+","+mon1+","+day+","+hour+","+min+");\n");
    }

    // ------------------------------------------------------------------------
    // write calendar JavaScript

    public static void writeJavaScript(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        PrivateLabel       privLabel  = reqState.getPrivateLabel();
        Locale             locale     = reqState.getLocale();
        String             timeZone   = reqState.getTimeZoneString(null);
        String             dateFormat = privLabel.getDateFormat();
        I18N               i18n       = I18N.getI18N(Calendar.class, locale);
        boolean            timeTxtFld = privLabel.getBooleanProperty(PrivateLabel.PROP_Calendar_timeTextField,false);
        HttpServletRequest request    = reqState.getHttpServletRequest();

        /* first day of week */
        String             fdowStr    = privLabel.getStringProperty(PrivateLabel.PROP_Calendar_firstDayOfWeek, null);
        int                fdowNdx    = DateTime.getDayIndex(fdowStr, 0);

        /* start JavaScript */
        JavaScriptTools.writeStartJavaScript(out);

        /* vars */
        JavaScriptTools.writeJSVar(out, "calTextTimeEntry"  , timeTxtFld);
        JavaScriptTools.writeJSVar(out, "calSelectedTMZ"    , timeZone);            // selected  TimeZone
        JavaScriptTools.writeJSVar(out, "calFirstDOW"       , fdowNdx);             // First DOW (Sun/Mon)
        JavaScriptTools.writeJSVar(out, "ID_CAL_DIV"        , ID_CAL_DIV);
        JavaScriptTools.writeJSVar(out, "ID_CAL_BOTTOM"     , ID_CAL_BOTTOM);

        /* I18N */
        if (timeTxtFld) {
        JavaScriptTools.writeJSVar(out, "TOOLTIP_SET_HOUR"  , i18n.getString("Calendar.inputHour.tooltip"  ,"Double-click to enter hour")); 
        JavaScriptTools.writeJSVar(out, "TOOLTIP_SET_MINUTE", i18n.getString("Calendar.inputMinute.tooltip","Double-click to enter minute")); 
        } else {
        JavaScriptTools.writeJSVar(out, "TOOLTIP_SET_HOUR"  , i18n.getString("Calendar.incrementHour.tooltip","Click to increment hour")); 
        JavaScriptTools.writeJSVar(out, "TOOLTIP_SET_MINUTE", i18n.getString("Calendar.incrementMinute.tooltip","Click to increment minute")); 
        }
        JavaScriptTools.writeJSVar(out, "TOOLTIP_EXPAND"    , i18n.getString("Calendar.expand.tooltip", "Click to expand calendar"));
        JavaScriptTools.writeJSVar(out, "TOOLTIP_COLLAPSE"  , i18n.getString("Calendar.collapse.tooltip", "Click to collapse calendar"));
        JavaScriptTools.writeJSVar(out, "TOOLTIP_PREV_MONTH", i18n.getString("Calendar.previous.tooltip", "Click to go to previous month"));
        JavaScriptTools.writeJSVar(out, "TOOLTIP_NEXT_MONTH", i18n.getString("Calendar.next.tooltip", "Click to go to next month"));

        /* format date JavaScript */
        char dateSep[] = DateTime.GetDateSeparatorChars(dateFormat);
        int  yyPos     = dateFormat.indexOf("y");
        int  mmPos     = dateFormat.indexOf("M");
        int  ddPos     = dateFormat.indexOf("d");
        out.write("function calFormatDisplayDate(YYYY,MM,DD) { ");
        if ((yyPos < mmPos) && (mmPos < ddPos)) {
            out.write("return YYYY+'"+dateSep[0]+"'+MM+'"+dateSep[1]+"'+DD;");
        } else
        if ((yyPos < ddPos) && (ddPos < mmPos)) {
            out.write("return YYYY+'"+dateSep[0]+"'+DD+'"+dateSep[1]+"'+MM;");
        } else
        if ((mmPos < ddPos) && (ddPos < yyPos)) {
            out.write("return MM+'"  +dateSep[0]+"'+DD+'"+dateSep[1]+"'+YYYY;");
        } else
        if ((ddPos < mmPos) && (mmPos < yyPos)) {
            out.write("return DD+'"  +dateSep[0]+"'+MM+'"+dateSep[1]+"'+YYYY;");
        } else {
            out.write("return YYYY+'"+dateSep[0]+"'+MM+'"+dateSep[1]+"'+DD;");
        }
        out.write(" }\n");

        /* New Calendar Javascript */
        out.write("var CalMonthNames = [ ");
        for (int m = 1; m <= 12; m++) {
            if (m > 1) { out.write(", "); }
            String mn = _GetMonthName(i18n, m);
            out.write("'" + mn + "'");
        }
        out.write(" ];\n");
        out.write("var CalDayNames = [ ");
        String DAY_NAMES[] = _GetDayNames(i18n);
        for (int d = 0; d < DAY_NAMES.length; d++) {
            if (d > 0) { out.write(", "); }
            out.write("'" + DAY_NAMES[d] + "'");
        }
        out.write(" ];\n");

        /* end JavaScript */
        JavaScriptTools.writeEndJavaScript(out);

        /* Calendar.js */
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("Calendar.js"), request);

    }

    // ------------------------------------------------------------------------

}
