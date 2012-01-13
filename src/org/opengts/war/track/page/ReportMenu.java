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
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/03/25  Martin D. Flynn
//     -Added CSV output format
//     -Added report category support
//  2007/03/30  Martin D. Flynn
//     -Added access control
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -Changes made to allow subclassing
//  2009/11/01  Martin D. Flynn
//     -Added ReportOption support
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.Locale;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.TimeZone;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.track.*;

public class ReportMenu
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    private static final String  ID_DEVICE_ID                   = "deviceSelector";
    private static final String  ID_DEVICE_DESCR                = "deviceDescDisp";

    // ------------------------------------------------------------------------
    // Forms:

    public  static final String  FORM_GET_REPORT                = "GetReport";
    public  static final String  FORM_SELECT_REPORT             = "SelectReport";
    public  static final String  FORM_DEVICE_GROUP              = "DeviceGroup";

    // ------------------------------------------------------------------------
    // Report type classifications:

    public  static final String  REPORT_TYPE_ALL                = "all";

    // ------------------------------------------------------------------------

    public  static final String  CSS_REPORT_RADIO_BUTTON        = "reportRadioButton";
    public  static final String  CSS_REPORT_RADIO_OPTION        = "reportRadioOption";

    public  static final String  COMMAND_REPORT_SELECT          = "rptsel";     // arg=<reportName>

    public  static final String  PARM_DEVICE_ID                 = PARM_DEVICE;
    public  static final String  PARM_GROUP_ID                  = PARM_GROUP;
    
    public  static final String  PARM_REPORT_SUBMIT             = "r_submit";

    public  static final String  PARM_REPORT[]                  = ReportURL.RPTARG_REPORT;
    public  static final String  PARM_REPORT_OPT_               = "r_opt_";
    public  static final String  PARM_REPORT_OPT                = "r_option";
    public  static final String  PARM_REPORT_TEXT_              = "r_txt_";
    public  static final String  PARM_REPORT_TEXT               = "r_text";
    public  static final String  PARM_LIMIT[]                   = ReportURL.RPTARG_LIMIT;
    public  static final String  PARM_LIMIT_TYPE[]              = ReportURL.RPTARG_LIMIT_TYPE; // not used
    public  static final String  PARM_FORMAT[]                  = ReportURL.RPTARG_FORMAT;

    public  static final String  PARM_EMAIL_ADDR[]              = ReportURL.RPTARG_EMAIL;

    public  static final String  PARM_MENU                      = "r_menu";

    // ------------------------------------------------------------------------

    /* Calendar IDs */
    public  static final String  CALENDAR_FROM                  = "rptCal_fr";
    public  static final String  CALENDAR_TO                    = "rptCal_to";
    
    // ------------------------------------------------------------------------

    protected static void writeJS_MenuUpdate(PrintWriter out, RequestProperties reqState, ReportMenu rptMenu,
        String parm_RANGE_FR[], String parm_RANGE_TO[], String parm_TIMEZONE[])
        throws IOException
    {
        HttpServletRequest request   = reqState.getHttpServletRequest();
        PrivateLabel       privLabel = reqState.getPrivateLabel();
        I18N               i18n      = privLabel.getI18N(ReportMenu.class);
        boolean            isGroup   = rptMenu.isReportTypeDeviceGroup(); // isFleet
        reqState.setFleet(isGroup); // not used (or really shouldn't be)

        /* HTML URL */
        String htmlURL   = Track.GetBaseURL(reqState); // EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI());
        String customURL = privLabel.getStringProperty(PrivateLabel.PROP_ReportMenu_customFormatURL,"");

        /* start JavaScript */
        JavaScriptTools.writeStartJavaScript(out);

        /* Group/Device list */
        if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
            //DeviceChooser.writeDeviceList(out, reqState, "ReportSelectorList");
        }

        /* Calendar */
        Calendar.writeNewCalendar(out, CALENDAR_FROM, null, i18n.getString("ReportMenu.dateFrom","From"), reqState.getEventDateFrom());
        Calendar.writeNewCalendar(out, CALENDAR_TO  , null, i18n.getString("ReportMenu.dateTo"  ,"To"  ), reqState.getEventDateTo());

        /* vars */
        out.write("// Report vars \n");
        JavaScriptTools.writeJSVar(out, "ReportIsGroup"   , isGroup);
        JavaScriptTools.writeJSVar(out, "ReportPageName"  , rptMenu.getPageName());
        JavaScriptTools.writeJSVar(out, "ReportHtmlURL"   , htmlURL);
        JavaScriptTools.writeJSVar(out, "ReportCustomURL" , customURL);

        out.write("// Onload \n");
        out.write("function rptmOnLoad() {\n");
        out.write("    "+CALENDAR_FROM+".setCollapsible(false, false, false);\n");
        out.write("    "+CALENDAR_TO  +".setCollapsible(false, false, false);\n");
        out.write("    calWriteCalendars("+CALENDAR_FROM+","+CALENDAR_TO+");\n");
        out.write("    rptmReportRadioChanged();\n");
        out.write("}\n");

        if (isGroup) {
        out.write("// device group ID \n");
        out.write("function rptmGetDeviceGroup() {\n");
        out.write("   return document."+FORM_DEVICE_GROUP+"."+PARM_GROUP_ID+".value;\n");
        out.write("}\n");
        } else {
        out.write("// device ID \n");
        out.write("function rptmGetDevice() {\n");
        out.write("   return document."+FORM_DEVICE_GROUP+"."+PARM_DEVICE_ID+".value;\n");
        out.write("}\n");
        }

        out.write("// selected report \n");
        out.write("function rptmGetReport() {\n");
        out.write("   if (document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length) {\n");
        out.write("     var rc = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length;\n");
        out.write("     for (var i = 0; i < rc; i++) {\n");
        out.write("       if (document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].checked) {\n");
        out.write("           return document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].value;\n");
        out.write("       }\n");
        out.write("     }\n");
        out.write("     return '?';\n");
        out.write("   } else {\n"); // assume that there is at least 1 report in the list
        out.write("     return document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".value;\n");
        out.write("   }\n");
        out.write("}\n");

        out.write("// selected report option\n");
        out.write("function rptmGetReportOption() {\n");
        out.write("   var rptName = rptmGetReport();\n");
        out.write("   var rptOptID = '" + PARM_REPORT_OPT_ + "' + rptName;\n");
        out.write("   var rptOpt = document.getElementById(rptOptID);\n");
        out.write("   if (rptOpt) {\n");
        out.write("       return rptOpt.value;\n");
        out.write("   }\n");
        out.write("   return '';\n");
        out.write("}\n");

        out.write("// selected report text\n");
        out.write("function rptmGetReportText() {\n");
        out.write("   var rptName = rptmGetReport();\n");
        out.write("   var rptTxtID = '" + PARM_REPORT_TEXT_ + "' + rptName;\n");
        out.write("   var rptTxt = document.getElementById(rptTxtID);\n");
        out.write("   if (rptTxt) {\n");
        out.write("       return rptTxt.value;\n");
        out.write("   }\n");
        out.write("   return '';\n");
        out.write("}\n");

        out.write("// record limit/type \n");
        out.write("function rptmGetLimit() {\n");
        out.write("   return '';\n");
        out.write("}\n");
        out.write("function rptmGetLimitType() {\n");
        out.write("   return '';\n");
        out.write("}\n");

        out.write("// report format \n");
        out.write("function rptmGetFormat() {\n");
        out.write("   return document."+FORM_GET_REPORT+"."+PARM_FORMAT[0]+".value;\n");
        out.write("}\n");

        out.write("// report format \n");
        out.write("function rptGetToEMailAddress() {\n");
        out.write("   try { return document."+FORM_GET_REPORT+"."+PARM_EMAIL_ADDR[0]+".value; } catch(e) { return ''; }\n");
        out.write("}\n");

        out.write("// submit command \n");
        out.write("function rptmSubmitCmd(page, cmd, arg) {\n");
        out.write("   var outFmt    = rptmGetFormat();\n");
        out.write("   var rptName   = rptmGetReport();\n");
        out.write("   var rptOption = rptmGetReportOption();\n");
        out.write("   var rptText   = rptmGetReportText();\n");
      //out.write("   alert('Report Option: ' + rptOption);\n");
        out.write("   document."+FORM_COMMAND+".method = 'post';\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_CSV+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.csv';\n"); // POST
        out.write("   } else\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_XLS+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.xls';\n"); // POST
        out.write("   } else\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_XLSX+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.xlsx';\n"); // POST
        out.write("   } else\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_TXT+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.txt';\n"); // POST
        out.write("   } else\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_XML+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.xml';\n"); // POST
        out.write("   } else\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_CUSTOM+"') {\n");
        out.write("       document."+FORM_COMMAND+".action = ReportCustomURL;\n");  // GET
        out.write("       document."+FORM_COMMAND+".method = 'get';\n");            // GET
        out.write("       document."+FORM_COMMAND+".target = '_blank';\n");         // new page
        out.write("   } else {\n");
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL;\n");    // POST
        out.write("   }\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_PAGE         +".value = page;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_COMMAND      +".value = cmd;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_ARGUMENT     +".value = arg;\n");
        if (isGroup) {
        out.write("   document."+FORM_COMMAND+"."+PARM_GROUP_ID     +".value = rptmGetDeviceGroup();\n");
        } else {
        out.write("   document."+FORM_COMMAND+"."+PARM_DEVICE_ID    +".value = rptmGetDevice();\n");
        }
        out.write("   document."+FORM_COMMAND+"."+parm_RANGE_FR[0]  +".value = "+CALENDAR_FROM+".getArgDateTime();\n");
        out.write("   document."+FORM_COMMAND+"."+parm_RANGE_TO[0]  +".value = "+CALENDAR_TO+".getArgDateTime();\n");
        out.write("   document."+FORM_COMMAND+"."+parm_TIMEZONE[0]  +".value = calGetTimeZone();\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_LIMIT[0]     +".value = rptmGetLimit();\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_LIMIT_TYPE[0]+".value = rptmGetLimitType();\n"); // not used
        out.write("   document."+FORM_COMMAND+"."+PARM_FORMAT[0]    +".value = outFmt;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_EMAIL_ADDR[0]+".value = rptGetToEMailAddress();\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_REPORT[0]    +".value = rptName;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_REPORT_OPT   +".value = rptOption;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_REPORT_TEXT  +".value = rptText;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_MENU         +".value = ReportPageName;\n");
        out.write("   document."+FORM_COMMAND+".submit();\n");
        out.write("}\n");

        out.write("// 'Get Report' \n");
        out.write("function rptmSubmitReport() {\n");
        out.write("   rptmSubmitCmd('"+PAGE_REPORT_SHOW+"','"+COMMAND_REPORT_SELECT+"','');\n");
        out.write("}\n");

        String csvMsg   = i18n.getString("ReportMenu.csvFormat"  ,"Report will be returned as a comma-separated-value file");
        String xlsMsg   = i18n.getString("ReportMenu.xlsFormat"  ,"Report will be returned as an XLS spreadsheet file");
        String xlsxMsg  = i18n.getString("ReportMenu.xlsxFormat" ,"Report will be returned as an XLSX spreadsheet file");
        String xmlMsg   = i18n.getString("ReportMenu.xmlFormat"  ,"Report will be returned as an XML formatted file");
        String emailMsg = i18n.getString("ReportMenu.emailFormat","Report will be emailed to the following list of comma-separated email address(es):");
        String schedMsg = i18n.getString("ReportMenu.schedFormat","Report will be scheduled for periodic email reportig:"); // EXPERIMENTAL
        out.write("// Format selection changed \n");
        out.write("function rptmFormatChanged() {\n");
        out.write("   var toEmailElem = document.getElementById('"+PARM_EMAIL_ADDR[0]+"');\n");
        out.write("   if (toEmailElem) { toEmailElem.style.visibility = 'hidden'; }\n");
        out.write("   var fmtSelElem = document.getElementById('"+PARM_FORMAT[0]+"');\n");
        out.write("   var selVal = fmtSelElem? fmtSelElem.value : '';\n");
        out.write("   var formatMsgElem = document.getElementById('formatMsgElem');\n");
        out.write("   if (formatMsgElem) {\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_CSV+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+csvMsg+"\";\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_TXT+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+csvMsg+"\";\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_XLS+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+xlsMsg+"\";\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_XLSX+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+xlsxMsg+"\";\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_XML+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+xmlMsg+"\";\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_EHTML+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+emailMsg+"\";\n");
        out.write("           if (toEmailElem) { toEmailElem.style.visibility = 'visible'; }\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_SCHEDULE+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+schedMsg+"\";\n");
        out.write("       } else {\n");
        out.write("           formatMsgElem.innerHTML = '';\n");
        out.write("       }\n");
        out.write("   }\n");
        out.write("}\n");
        
        out.write("// Report radio button selection changed \n");
        out.write("function rptmReportRadioChanged() {\n");
        out.write("   try {\n");
        out.write("      if (document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length) {\n");
        out.write("         var rc = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length;\n");
        out.write("         for (var i = 0; i < rc; i++) {\n");
        out.write("            var rptName = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].value;\n");
        out.write("            var rptChkd = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].checked;\n");
        out.write("            var rptOptn = document.getElementById('" + PARM_REPORT_OPT_ + "' + rptName);\n");
        out.write("            if (rptOptn) {\n");
        out.write("               rptOptn.disabled = rptChkd? false : true;\n");
        out.write("            }\n");
        out.write("            var rptText = document.getElementById('" + PARM_REPORT_TEXT_ + "' + rptName);\n");
        out.write("            if (rptText) {\n");
        out.write("               rptText.disabled = rptChkd? false : true;\n");
        out.write("            }\n");
        out.write("         }\n");
        out.write("      }\n");
        out.write("   } catch (e) {\n");
        out.write("      //\n");
        out.write("   }\n");
        out.write("}\n");

        if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
            out.write("// Device/Group selector \n");
            out.write("function rptmShowSelector() {\n");
            out.write("   if (deviceShowChooserList) {\n");
            out.write("       var list = (typeof ReportSelectorList != 'undefined')? ReportSelectorList : null;\n");
            out.write("       deviceShowChooserList('"+ID_DEVICE_ID+"','"+ID_DEVICE_DESCR+"',list);\n");
            out.write("   }\n");
            out.write("}\n");
            out.write("function deviceDeviceChanged() {\n");
                // NO-OP
            out.write("}\n");
        }

        /* end JavaScript */
        JavaScriptTools.writeEndJavaScript(out);

        /* sorttable.js */
        if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
            JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(ReportPresentation.SORTTABLE_JS), request);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String reportType = REPORT_TYPE_ALL;
    
    public ReportMenu()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_MENU_REPORT);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public void setReportType(String rptType)
    {
        this.reportType = rptType;
    }
    
    public String getReportType()
    {
        return this.reportType;
    }
    
    public boolean isReportTypeAll()
    {
        String rt = this.getReportType();
        if ((rt == null) || rt.equals("")) {
            // this is the default, if not specified
            return true;
        } else
        if (rt.equalsIgnoreCase(REPORT_TYPE_ALL)) {
            // explicitly 'ALL'
            return true;
        } else {
            // otherwise not 'all'
            return false;
        }
    }
    
    /* return true if this report type should display device groups */
    public boolean isReportTypeDeviceGroup()
    {
        return ReportFactory.getReportTypeIsGroup(this.getReportType());
    }
    
    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_REPORTS;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getMenuDescription(reqState,i18n.getString("ReportMenu.menuDesc","GPS tracking reports"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getMenuHelp(reqState,i18n.getString("ReportMenu.menuHelp","Display various historical GPS detail and summary reports"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getNavigationDescription(reqState,i18n.getString("ReportMenu.navDesc","Reports"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getNavigationTab(reqState,i18n.getString("ReportMenu.navTab","Reports"));
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n       = privLabel.getI18N(ReportMenu.class);
        final Locale locale   = reqState.getLocale();
        Account account       = reqState.getCurrentAccount();
        User    user          = reqState.getCurrentUser();    // may be null
        String  cmdName       = reqState.getCommandName();    // not used?
        final boolean isGroup = this.isReportTypeDeviceGroup();
        reqState.setFleet(isGroup);

        /* error */
        String m = pageMsg;
        boolean error = !StringTools.isBlank(m);
        
        /* date parameters */
        boolean useMapDates = privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_useMapDates,true);
        final String parm_RANGE_FR[] = useMapDates? Calendar.PARM_RANGE_FR : Calendar.PARM_RANGE_FR2;
        final String parm_RANGE_TO[] = useMapDates? Calendar.PARM_RANGE_TO : Calendar.PARM_RANGE_TO2;
        final String parm_TIMEZONE[] = Calendar.PARM_TIMEZONE;

        /* date args */
        String rangeFr   = (String)AttributeTools.getRequestAttribute(request, parm_RANGE_FR  , "");
        String rangeTo   = (String)AttributeTools.getRequestAttribute(request, parm_RANGE_TO  , "");
        String tzStr     = (String)AttributeTools.getRequestAttribute(request, parm_TIMEZONE  , "");
        
        /* other args */
        String limitStr  = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT     , "");
        String limitType = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT_TYPE, ""); // not used
        String format    = (String)AttributeTools.getRequestAttribute(request, PARM_FORMAT    , ReportURL.FORMAT_HTML);

        /* 'demo' date range? */
        if (reqState.isDemoAccount()) {
            // Special case for the device with demo data.
            String firstRptKey = "FirstDemoReport_" + reqState.getSelectedDeviceID();
            String firstRpt    = (String)AttributeTools.getSessionAttribute(request, firstRptKey, null); // from session only
            if (firstRpt == null) {
                String dateRange[] = reqState.getDemoDateRange();
                if ((dateRange != null) && (dateRange.length >= 2)) {
                    rangeFr = dateRange[0];
                    rangeTo = dateRange[1];
                }
                AttributeTools.setSessionAttribute(request, firstRptKey, "true");
            }
        }

        /* TimeZone */
        if (StringTools.isBlank(tzStr)) {
            if (user != null) {
                // try User timezone
                tzStr = user.getTimeZone(); // may be blank
                if (StringTools.isBlank(tzStr) || tzStr.equals(User.DEFAULT_TIMEZONE)) {
                    // override with Account timezone
                    tzStr = account.getTimeZone();
                }
            } else {
                // get Account timezone
                tzStr = account.getTimeZone();
            }
            if (StringTools.isBlank(tzStr)) {
                // make sure we have a timezone 
                // (unecessary, since Account/User will return a timezone)
                tzStr = Account.DEFAULT_TIMEZONE;
            }
        }
        final TimeZone tz = DateTime.getTimeZone(tzStr); // will be GMT if invalid
        AttributeTools.setSessionAttribute(request, parm_TIMEZONE[0], tzStr);
        reqState.setTimeZone(tz, tzStr);

        /* Event date range */
        DateTime dateFr = Calendar.parseDate(rangeFr,tz,false);
        DateTime dateTo = Calendar.parseDate(rangeTo,tz,true );
        if (dateFr == null) { dateFr = Calendar.getCurrentDayStart(tz); }
        if (dateTo == null) { dateTo = Calendar.getCurrentDayEnd(tz); }
        if (dateFr.after(dateTo)) { dateFr = dateTo; }
        reqState.setEventDateFrom(dateFr);
        reqState.setEventDateTo(  dateTo);
        AttributeTools.setSessionAttribute(request, parm_RANGE_FR[0], Calendar.formatArgDateTime(dateFr));
        AttributeTools.setSessionAttribute(request, parm_RANGE_TO[0], Calendar.formatArgDateTime(dateTo));

        /* reset previous 'reportID' */
        // TODO: should reset this iff this invocation is a different instance of ReportMenu
        String reportID = ""; // for now, always reset the previous report-id
        AttributeTools.setSessionAttribute(request, PARM_REPORT[0]  , reportID);
        AttributeTools.setSessionAttribute(request, PARM_REPORT_OPT , "");
        AttributeTools.setSessionAttribute(request, PARM_REPORT_TEXT, "");

        /* group/device */
        String deviceID = "";
        String groupID  = "";
        if (isGroup) {
            String rptGrp = (String)AttributeTools.getRequestAttribute(request, PARM_GROUP_ID , "");
            groupID = StringTools.isBlank(rptGrp)? reqState.getSelectedDeviceGroupID() : rptGrp;
            AttributeTools.setSessionAttribute(request, PARM_GROUP_ID , groupID);
        } else {
            String rptDev = (String)AttributeTools.getRequestAttribute(request, PARM_DEVICE_ID, "");
            deviceID = StringTools.isBlank(rptDev)? reqState.getSelectedDeviceID() : rptDev;
            AttributeTools.setSessionAttribute(request, PARM_DEVICE_ID, deviceID);
        }

        /* store vars as session attributes */
        AttributeTools.setSessionAttribute(request, PARM_LIMIT[0]     , limitStr);
        AttributeTools.setSessionAttribute(request, PARM_LIMIT_TYPE[0], limitType); // not used
        AttributeTools.setSessionAttribute(request, PARM_FORMAT[0]    , format);

        /* Style Sheets */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = ReportMenu.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "ReportMenu.css", cssDir);
                Calendar.writeStyle(out, reqState);
                if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
                    DeviceChooser.writeStyle(out, reqState);
                }
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String pageName = ReportMenu.this.getPageName();
                MenuBar.writeJavaScript(out, pageName, reqState);
                Calendar.writeJavaScript(out, reqState);
                if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
                    DeviceChooser.writeJavaScript(out, locale, reqState,
                        privLabel.getWebPageURL(reqState, pageName, Track.COMMAND_DEVICE_LIST));
                }
                ReportMenu.writeJS_MenuUpdate(out, reqState, ReportMenu.this,
                    parm_RANGE_FR, parm_RANGE_TO, parm_TIMEZONE);
            }
        };

        /* has XLS/XLSX support */
        final boolean hasXLSSupport = ReportSpreadsheet.IsExcelSpreadsheetSupported();

        /* has notification email address */
        final String toEmailAddress = StringTools.trim(Account.getReportEmailAddress(account,user));
        final boolean hasToEmail; // see also "ReportURL.FORMAT_EHTML"
        if (!privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_enableReportEmail,true)) {
            // "EMail" option quietly disabled
            hasToEmail = false;
        } else
        if (StringTools.isBlank(privLabel.getEventNotificationFrom())) {
            // no "From" email address
            Print.logWarn("No valid 'From' notification email address defined ('EMail' option disabled)"); 
            hasToEmail = false;
        } else {
            hasToEmail = true; // StringTools.isBlank(toEmailAddress);
        }

        /* has notification email address */
        final boolean hasSchedule; // see also "ReportURL.FORMAT_SCHEDULE"
        if (!privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_enableReportSchedule,false)) {
            hasSchedule = false;
        } else {
            hasSchedule = true;
        }

        /* Misc attributes */
        final boolean showTimezoneSelect = privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_showTimezoneSelection,true);
        final boolean hasCustomURL       = !StringTools.isBlank(privLabel.getStringProperty(PrivateLabel.PROP_ReportMenu_customFormatURL,""));

        /* write frame */
        final String _reportID = reportID;
        final String _deviceID = deviceID;
        final String _groupID  = groupID;
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName  = ReportMenu.this.getPageName();
                String reportURL = Track.GetBaseURL(reqState); // EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI());

                /* Command Form */
                // This entire form is 'hidden'.  It's used by JS functions to submit specific commands 
                String pageTarget = "_self"; // change to "_blank" to open reports in a separate page  // target='_top'
                out.write("\n");
                out.write("<form id='"+FORM_COMMAND+"' name='"+FORM_COMMAND+"' method='post' action=\""+reportURL+"\" target='"+pageTarget+"'>\n");
                out.write(" <input type='hidden' name='"+PARM_PAGE              +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_COMMAND           +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_ARGUMENT          +"' value=''/>\n");
                if (isGroup)  { 
                out.write(" <input type='hidden' name='"+PARM_GROUP_ID          +"' value=''/>\n"); 
                } else {
                out.write(" <input type='hidden' name='"+PARM_DEVICE_ID         +"' value=''/>\n"); 
                }
                out.write(" <input type='hidden' name='"+PARM_REPORT[0]         +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_REPORT_OPT        +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_REPORT_TEXT       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+parm_RANGE_FR[0]       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+parm_RANGE_TO[0]       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+parm_TIMEZONE[0]       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_LIMIT[0]          +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_LIMIT_TYPE[0]     +"' value=''/>\n"); // not used
                out.write(" <input type='hidden' name='"+PARM_FORMAT[0]         +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_EMAIL_ADDR[0]     +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_MENU              +"' value=''/>\n");
                out.write("</form>\n");
                out.write("\n");

                // frame header
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+i18n.getString("ReportMenu.gpsReports","GPS Tracking Reports")+"</span><br/>\n");
                out.write("<span class='"+CommonServlet.CSS_MENU_INSTRUCTIONS+"'>"+i18n.getString("ReportMenu.selectReport","Please select a report from the following menu:")+"</span><br/>\n");
                out.write("<hr/>\n");

                /* begin calendar/report-selection table */
                out.write("<table height='90%' border='0' cellspacing='0' cellpadding='0'>\n"); // {
                out.write("<tr>\n");
                out.write("<td valign='top' height='100%' style='padding-right:3px; border-right: 3px double black;'>\n");

                /* device[group] list */
                out.write("<form id='"+FORM_DEVICE_GROUP+"' name='"+FORM_DEVICE_GROUP+"' method='post' action=\"javascript:rptmSubmitReport();\" target='_self''>\n"); // target='_top'
                IDDescription.SortBy sortBy = DeviceChooser.getSortBy(privLabel);
                if (isGroup) {
                    // fleet group selection
                    String grpAllDesc  = DeviceGroup.GetDeviceGroupAll(locale);
                    String grpTitles[] = reqState.getDeviceGroupTitles();
                    out.write("<b>"+i18n.getString("ReportMenu.deviceGroup","{0}:",grpTitles)+"</b><br>\n");
                    if (DeviceChooser.isDeviceChooserUseTable(privLabel)) { // Fleet
                        out.write("<table cellspacing='0' cellpadding='0' border='0'><tr>");
                        out.write("<td>");
                        String chooserStyle   = "height:17px; padding:0px 0px 0px 3px; margin:0px 0px 0px 3px; cursor:pointer; border:1px solid gray;";
                        String chooserOnclick = "javascript:rptmShowSelector()";
                        String chooserLen     = "16";
                        switch (sortBy) {
                            case DESCRIPTION : {
                                String grDesc = FilterValue(reqState.getDeviceGroupDescription(_groupID,false));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+_groupID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' type='text' value='"+grDesc+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            case NAME : {
                                String grName = FilterValue(reqState.getDeviceGroupDescription(_groupID,true));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+_groupID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' type='text' value='"+grName+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            default : {
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='text' value='"+_groupID+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                        }
                        out.write("</td>");
                        out.write("<td><img src='images/Pulldown.png' height='17' style='cursor:pointer;' onclick='"+chooserOnclick+"'></td>");
                        out.write("</tr></table>\n");
                    } else {
                        OrderedSet<String> groupList = reqState.getDeviceGroupIDList(true); // non-null, length > 0
                        if (ListTools.isEmpty(groupList)) {
                            // will not occur
                            String id   = DeviceGroup.DEVICE_GROUP_NONE;
                            String desc = FilterValue("?");
                            out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+id+"'>");
                            out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+"' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                        } else
                        if (DeviceChooser.showSingleItemTextField(privLabel) && (groupList.size() == 1)) {
                            String id   = groupList.get(0);
                            if (sortBy.equals(IDDescription.SortBy.ID)) {
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' class='"+CommonServlet.CSS_TEXT_READONLY+"' type='text' readonly size='14' maxlength='32' value='"+id+"'>");
                            } else {
                                boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                                String desc = FilterValue(reqState.getDeviceGroupDescription(id,rtnDispName));
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+id+"'>");
                                out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+"' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                            }
                        } else {
                            java.util.List<IDDescription> sortList = new Vector<IDDescription>();
                            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                            for (String id : groupList) {
                                String desc = reqState.getDeviceGroupDescription(id,rtnDispName);
                                sortList.add(new IDDescription(id,desc));
                            }
                            IDDescription.SortList(sortList, rtnDispName? IDDescription.SortBy.DESCRIPTION : sortBy);
                            out.write("<select id='"+ID_DEVICE_ID+"' name='"+PARM_GROUP_ID+"'>\n");
                            for (IDDescription dd : sortList) {
                                String id   = dd.getID();
                                String desc = dd.getDescription();
                                String sel  = id.equals(_groupID)? "selected" : "";
                                String disp = FilterValue((sortBy.equals(IDDescription.SortBy.ID)?id:desc));
                                out.write("<option value='"+id+"' "+sel+">"+disp+"</option>\n");
                            }
                            out.write("</select>\n");
                        }
                    }
                } else {
                    // device selection
                    String devTitles[] = reqState.getDeviceTitles();
                    out.write("<b>"+i18n.getString("ReportMenu.device","{0}:",devTitles)+"</b><br>\n");
                    if (DeviceChooser.isDeviceChooserUseTable(privLabel)) { // Device
                        out.write("<table cellspacing='0' cellpadding='0' border='0'><tr>");
                        out.write("<td>");
                        String chooserStyle   = "height:17px; padding:0px 0px 0px 3px; margin:0px 0px 0px 3px; cursor:pointer; border:1px solid gray;";
                        String chooserOnclick = "javascript:rptmShowSelector()";
                        String chooserLen     = "16";
                        switch (sortBy) {
                            case DESCRIPTION : {
                                String dvDesc = FilterValue(reqState.getDeviceDescription(_deviceID,false));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' type='hidden' value='"+_deviceID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' type='text' value='"+dvDesc   +"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            case NAME : {
                                String dvName = FilterValue(reqState.getDeviceDescription(_deviceID,true));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' type='hidden' value='"+_deviceID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' type='text' value='"+dvName   +"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            default : {
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' type='text' value='"+_deviceID+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                        }
                        out.write("</td>");
                        out.write("<td><img src='images/Pulldown.png' height='17' style='cursor:pointer;' onclick='"+chooserOnclick+"'></td>");
                        out.write("</tr></table>\n");
                    } else {
                        OrderedSet<String> devList = reqState.getDeviceIDList(false/*inclInactv*/);
                        if (ListTools.isEmpty(devList)) {
                            String id   = DeviceGroup.DEVICE_GROUP_NONE;
                            String desc = FilterValue("?");
                            out.println("<input id='"+ID_DEVICE_ID+"' name='"+PARM_DEVICE_ID+"' type='hidden' value='"+id+"'>");
                            out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+"' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                        } else
                        if (DeviceChooser.showSingleItemTextField(privLabel) && (devList.size() == 1)) {
                            String id   = devList.get(0);
                            if (sortBy.equals(IDDescription.SortBy.ID)) {
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' class='"+CommonServlet.CSS_TEXT_READONLY+"' type='text' readonly size='14' maxlength='32' value='"+id+"'>");
                            } else {
                                boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                                String desc = FilterValue(reqState.getDeviceDescription(id,rtnDispName));
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' type='hidden' value='"+id+"'>");
                                out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+"' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                            }
                        } else {
                            java.util.List<IDDescription> sortList = new Vector<IDDescription>();
                            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                            for (String id : devList) {
                                String desc = reqState.getDeviceDescription(id,rtnDispName);
                                sortList.add(new IDDescription(id,desc));
                            }
                            IDDescription.SortList(sortList, rtnDispName? IDDescription.SortBy.DESCRIPTION : sortBy);
                            out.write("<select id='"+ID_DEVICE_ID+"' name='"+PARM_DEVICE_ID+"'>\n");
                            for (IDDescription dd : sortList) {
                                String id   = dd.getID();
                                String desc = dd.getDescription();
                                String sel  = id.equals(_deviceID)? "selected" : "";
                                String disp = FilterValue((sortBy.equals(IDDescription.SortBy.ID)?id:desc));
                                out.write("<option value='"+id+"' "+sel+">"+disp+"</option>\n");
                            }
                            out.write("</select>\n");
                        }
                    }
                }
                out.write("</form>\n");

                /* From/To Calendars */
                DateTime fr = reqState.getEventDateFrom();
                if (fr == null) { fr = new DateTime(tz); }
                DateTime to = reqState.getEventDateTo();
                if (to == null) { to = new DateTime(tz); }
                boolean sameMonth = (fr.getYear() == to.getYear()) && (fr.getMonth1() == to.getMonth1());
                out.write("\n");
                out.println("<!-- Calendars -->");
                out.write("<div style='height: 100%; margin-top: 8px;'>\n");
                out.write(  "<b>"+i18n.getString("ReportMenu.selectDate","Select Date Range:")+"</b>\n");
                out.write(  "<div class='"+Calendar.CLASS_CAL_DIV+"' id='"+Calendar.ID_CAL_DIV+"' style='text-align: center; padding: 4px 5px 0px 5px;'>\n");
                out.write(    "<div id='"+CALENDAR_FROM+"'></div>\n");
                out.write(    "<div id='"+CALENDAR_TO+  "' style='padding-top: 8px;'></div>\n");
                if (showTimezoneSelect) {
                    out.println("<!-- Timezone select -->");
                    out.println("<div style='padding-top: 5px; text-align: left;'>");
                    out.println(  "<form id='TimeZoneSelect' name='TimeZoneSelect' method='get' action=\"javascript:true;\" target='_self'>"); // target='_top'
                    out.println(  "<span style='font-size:8pt;'><b>"+i18n.getString("ReportMenu.timeZone","TimeZone:")+"</b></span><br>");
                    out.println(  "<select name='"+parm_TIMEZONE[0]+"' onchange=\"javascript:calSelectTimeZone(document.TimeZoneSelect."+parm_TIMEZONE[0]+".value)\">");
                    String timeZone = reqState.getTimeZoneString(null);
                    java.util.List _tzList = reqState.getTimeZonesList();
                    for (Iterator i = _tzList.iterator(); i.hasNext();) {
                        String tmz = (String)i.next();
                        String sel = tmz.equals(timeZone)? "selected" : "";
                        out.println("  <option value='"+tmz+"' "+sel+">"+tmz+"</option>");
                    }
                    out.println(  "</select>");
                    out.println(  "</form>");
                    out.println("</div>");
                    out.println("");
                }
                
                // the following pushes the calendars to the top
                // (however, it also pushes the footer to the bottom of the frame, and leaves
                // a bunch of space below the footer)
                //out.write("<div style='height:100%'>&nbsp;</div>\n"); 
                
                out.write(  "</div>\n");                
                out.write("</div>\n");
                out.write("\n");

                out.write("</td>\n");

                out.write("<td nowrap width='100%' height='100%' valign='top' style='margin-left:10px;'>\n");

                /* reports */
                out.write("<!-- Begin Reports -->\n");
                out.write("<table width='100%'>\n"); // {
                out.write("<tr>\n");
                out.write("<td valign='top' style='margin-top: 8px; padding-left: 5px;'>\n");
                out.write("<form id='"+FORM_SELECT_REPORT+"' name='"+FORM_SELECT_REPORT+"' method='post' action=\"javascript:rptmSubmitReport();\" target='_self'>\n"); // target='_top'
                boolean checked = false;
                int rptSel = 0;
                for (int t = 0; t < ReportFactory.REPORT_TYPES.length; t++) {
                    // check report type
                    String type = ReportFactory.REPORT_TYPES[t];
                    if (!ReportMenu.this.isReportTypeAll() && !ReportMenu.this.getReportType().equalsIgnoreCase(type)) {
                        // report type does not match what this report menu supports
                        continue;
                    }
                    // include report type
                    String desc = ReportFactory.getReportTypeDescription(reqState, type);
                    java.util.List<ReportEntry> reportItems = ReportMenu.this.getReportItems(reqState, type);
                    if (reportItems.size() > 0) {
                        // at least one report with this type
                        out.write("  <b>"+desc+"</b><br/>\n");
                        for (Iterator<ReportEntry> i = reportItems.iterator(); i.hasNext();) {
                            ReportEntry   re = i.next();
                            ReportFactory rf = re.getReportFactory();
                            String        rn = rf.getReportName();
                            out.write("<span class='"+CSS_REPORT_RADIO_BUTTON+"'>");
                            out.write("<input class='"+CSS_REPORT_RADIO_BUTTON+"' type='radio' name='"+PARM_REPORT[0]+"' id='" + rn + "' value='" + FilterValue(rn) + "' onchange=\"javascript:rptmReportRadioChanged();\"");
                            if (!checked && (StringTools.isBlank(_reportID) || _reportID.equals(rn))) {
                                out.write(" checked");
                                checked = true;
                            }
                            out.write(">");
                            String rmd = StringTools.replaceKeys(rf.getMenuDescription(locale),reqState,null);
                            out.write("<label for='"+rn+"'>" + rmd + "</label>");
                            out.write("</span>\n");
                            if (rf.hasReportOptions(reqState)) {
                                OrderedMap<String,String> optMap = rf.getReportOptionDescriptionMap(reqState);
                                String optId = PARM_REPORT_OPT_ + rn;
                                if (optMap.size() == 1) {
                                    String k = optMap.getFirstKey();
                                    String d = optMap.get(k);
                                    out.write("<input id='"+optId+"' name='"+optId+"' type='hidden' value='"+k+"'>");
                                    out.write("<span class='"+CSS_REPORT_RADIO_OPTION+"'>["+d+"]</span>\n");
                                } else {
                                    ComboMap comboOptMap = new ComboMap(optMap);
                                    out.write(Form_ComboBox(optId, optId, true, comboOptMap, (String)null/*selected*/, null/*onchange*/));
                                }
                            } else
                            if (rf.hasReportTextInput()) {
                                String textId = PARM_REPORT_TEXT_ + rn;
                                out.write(Form_TextField(textId, textId, true, "", 40, 60));
                            }
                            out.write("<br/>\n");
                            rptSel++;
                        }
                    }
                }
                out.write("</form>\n");
                out.write("</td>\n");
                out.write("</tr>\n");
                
                out.write(" <!-- Begin Report Submit -->\n");
                out.write(" <tr>\n");
                out.write("  <td valign='bottom' style='text-align: left;'>\n");
                out.write("    <hr>\n");
                out.write("    <form id='"+FORM_GET_REPORT+"' name='"+FORM_GET_REPORT+"' method='post' action=\"javascript:rptmSubmitReport();\" target='_self'>\n"); // target='_top'
                out.write("    <span style='padding-left: 5px;'><b>"+i18n.getString("ReportMenu.format","Format:")+"</b></span>\n");
                out.write("    <select id='"+PARM_FORMAT[0]+"' name='"+PARM_FORMAT[0]+"' onchange=\"javascript:rptmFormatChanged();\">\n");
                out.write("      <option value='"+ReportURL.FORMAT_HTML +"' selected>HTML</option>\n");
                out.write("      <option value='"+ReportURL.FORMAT_CSV  +"'>CSV</option>\n");
              //out.write("      <option value='"+ReportURL.FORMAT_TXT  +"'>TXT</option>\n");
                out.write("      <option value='"+ReportURL.FORMAT_XML  +"'>XML</option>\n");
                if (hasXLSSupport) {
                    out.write("      <option value='"+ReportURL.FORMAT_XLS +"'>XLS</option>\n");
                }
                if (hasToEmail) {
                    out.write("      <option value='"+ReportURL.FORMAT_EHTML+"'>EMail</option>\n");
                }
                if (hasSchedule) { // EXPERIMENTAL
                    out.write("      <option value='"+ReportURL.FORMAT_SCHEDULE+"'>Schedule</option>\n");
                }
                if (hasCustomURL) {
                    out.write("      <option value='"+ReportURL.FORMAT_CUSTOM+"'>Custom</option>\n");
                }
                out.write("    </select>\n");
                out.write("    <span style='margin-left:40px;'><input type='submit' name='"+PARM_REPORT_SUBMIT+"' value='"+i18n.getString("ReportMenu.getReport","Get Report")+"'></span>\n");
                out.write("    <br>\n");
                out.write("    <br>\n");
                out.write("    <span id='formatMsgElem' style='margin-top:10px; margin-left:5px;'></span>\n");
                out.write("    <br>\n");
                if (hasToEmail) {
                    boolean emailEditable = !reqState.isDemoAccount();
                    String emailRO = emailEditable? "" : "readonly";
                    String emailClass = emailEditable? CommonServlet.CSS_TEXT_INPUT : CommonServlet.CSS_TEXT_READONLY;
                    out.write("    <input class='"+emailClass+"' id='"+PARM_EMAIL_ADDR[0]+"' name='"+PARM_EMAIL_ADDR[0]+"' style='margin-top:5px; margin-left:10px;; visibility:hidden' type='text' "+emailRO+" value='"+toEmailAddress+"' size='76'>");
                } else {
                    out.write("    <input id='"+PARM_EMAIL_ADDR[0]+"' name='"+PARM_EMAIL_ADDR[0]+"' type='hidden' value=''/>\n");
                }
                out.write("    </form>\n");
                out.write("  </td>\n");
                out.write(" </tr>\n");
                out.write(" <!-- End Report Submit -->\n");
                
                out.write("</table>\n"); // }
                out.write("<!-- End Reports -->\n");
                
                /* end table */
                out.write("</td>\n");
                out.write("</tr>\n");
                out.write("</table>\n");  // }

                /* write DeviceChooser DIV */
                if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
                    java.util.List<IDDescription> idList = reqState.createIDDescriptionList(isGroup, sortBy);
                    IDDescription list[] = idList.toArray(new IDDescription[idList.size()]);
                    DeviceChooser.writeChooserDIV(out, reqState, list, null);
                }

            }
        };

        /* write frame */
        String rptOnLoad = "javascript:rptmOnLoad();";
        String onload = (error && !StringTools.isBlank(m))? (rptOnLoad + JS_alert(false,m)) : rptOnLoad;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------

    protected java.util.List<ReportEntry> getReportItems(RequestProperties reqState, String rptType)
    {
        java.util.List<ReportEntry> list = new Vector<ReportEntry>();
        if (reqState != null) {
            PrivateLabel pl = reqState.getPrivateLabel();
            Map reportMap = pl.getReportMap();
            if (reportMap != null) {
                Account account = reqState.getCurrentAccount();
                User user = reqState.getCurrentUser();
                for (Iterator i = reportMap.values().iterator(); i.hasNext();) {
                    ReportEntry   re = (ReportEntry)i.next();
                    ReportFactory rf = re.getReportFactory();
                    if (StringTools.isBlank(rptType) || rptType.equalsIgnoreCase(rf.getReportType())) {
                        if (rf.isSysAdminOnly() && ((account == null) || !account.isSystemAdmin())) {
                            // skip this report
                        } else
                        if (pl.hasReadAccess(user,re.getAclName())) {
                            list.add(re);
                        }
                    }
                }
            }
        } else {
            Print.logStackTrace("RequestProperties is null!");
        }
        return list;
    }

    // ------------------------------------------------------------------------

}
