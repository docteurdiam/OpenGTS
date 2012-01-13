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
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2008/02/11  Martin D. Flynn
//     -Added support for displaying a map of locations on a report.
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Collection;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.track.Calendar;
import org.opengts.war.track.*;

public class ReportDisplay
    extends WebPageAdaptor
    implements Constants
{
    
    // ------------------------------------------------------------------------
    
    /* DEBUG: save emailed report output to "/tmp/" (should be false for production) */
    private static final boolean DEBUG_SAVE_EHTML_TO_TMP    = false;
    
    // ------------------------------------------------------------------------

    public  static final String CSS_REPORT_DISPLAY[]        = new String[] { "reportDisplayTable", "reportDisplayCell" };

    // ------------------------------------------------------------------------
    
    public  static final String COMMAND_REPORT_SELECT       = ReportMenu.COMMAND_REPORT_SELECT;

    public  static final String PARM_DEVICE_ID              = ReportMenu.PARM_DEVICE_ID;
    public  static final String PARM_GROUP_ID               = ReportMenu.PARM_GROUP_ID;
    
    public  static final String PARM_REPORT[]               = ReportMenu.PARM_REPORT;
    public  static final String PARM_REPORT_OPT             = ReportMenu.PARM_REPORT_OPT;
    public  static final String PARM_REPORT_TEXT            = ReportMenu.PARM_REPORT_TEXT;
    public  static final String PARM_LIMIT[]                = ReportMenu.PARM_LIMIT;
    public  static final String PARM_LIMIT_TYPE[]           = ReportMenu.PARM_LIMIT_TYPE; // not used
    public  static final String PARM_FORMAT[]               = ReportMenu.PARM_FORMAT;
    public  static final String PARM_REPORT_SUBMIT          = ReportMenu.PARM_REPORT_SUBMIT;

    public  static final String PARM_MENU                   = ReportMenu.PARM_MENU;
    public  static final String PARM_EMAIL_ADDR[]           = ReportMenu.PARM_EMAIL_ADDR;

    // ------------------------------------------------------------------------

    public ReportDisplay()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_REPORT_SHOW);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP, PAGE_MENU_REPORT });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public String getPageNavigationHTML(RequestProperties reqState)
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        String rptMenu = AttributeTools.getRequestString(request, PARM_MENU, PAGE_MENU_REPORT);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP, rptMenu });
        return super.getPageNavigationHTML(reqState,true);
    }
    
    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getMenuDescription(reqState,"");
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getMenuHelp(reqState,"");
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getNavigationDescription(reqState,"");
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getNavigationTab(reqState,"");
    }

    // ------------------------------------------------------------------------

    private String _formatMapEvent(EventDataProvider edp, RequestProperties reqState, ReportData report)
    {
        MapProvider mapProvider = reqState.getMapProvider(); // not null
        PrivateLabel privLabel = reqState.getPrivateLabel();

        /* date/time format */
        Account acct = reqState.getCurrentAccount();
        String dateFmt = (acct != null)? acct.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (acct != null)? acct.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();

        /* format */
        boolean isFleet = reqState.isFleet();
        TimeZone tmz    = reqState.getTimeZone();
        String iconSel  = report.getMapIconSelector(); // may be null
        OrderedSet<String> iconKeys = (OrderedSet<String>)mapProvider.getPushpinIconMap(reqState).keySet();
        return EventUtil.getInstance().formatMapEvent(privLabel, edp, iconSel, iconKeys, isFleet, tmz, dateFmt, timeFmt);

    }

    private void _writeReportMap(HttpServletResponse response, final RequestProperties reqState, final ReportData report, final I18N i18n)
        throws ReportException, IOException
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        int showPPBox = StringTools.parseInt(AttributeTools.getRequestAttribute(request,"showpp",""),-1);
        int zoomPP    = StringTools.parseInt(AttributeTools.getRequestAttribute(request,"zoompp",""),-1);

        /* map provider */
        MapProvider mapProvider = reqState.getMapProvider();
        if (mapProvider == null) {
            throw new ReportException(i18n.getString("ReportDisplay.noMapProvider","No Map Provider defined for this URL"));
        }

        /* write frame */
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
        PrintWriter pw = response.getWriter();
        PrivateLabel privLabel = reqState.getPrivateLabel();
        
        /* map dataset type */
        final boolean isFleet = reqState.isFleet();
        final String type = isFleet? EventUtil.DSTYPE_group : EventUtil.DSTYPE_device; // "poi"

        // HTML start
        pw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
        pw.write("<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>\n");

        // HTML head
        pw.write("\n");
        pw.write("<head>\n");
        pw.write("  <meta http-equiv='content-type' content='text/html; charset=UTF-8'/>\n");
        pw.write("  <meta http-equiv='cache-control' content='no-cache'/>\n");
        pw.write("  <meta http-equiv='expires' content='0'/>\n"); // expires 'now'
        pw.write("  <meta name='copyright' content='"+privLabel.getCopyright()+"'/>\n");
        pw.write("  <meta name='robots' content='none'/>\n");
        pw.write("  <title>" + privLabel.getPageTitle() + "</title>\n");

        // JavaScript tools
        JavaScriptTools.writeUtilsJS(pw, request);
        mapProvider.writeJavaScript(pw, reqState);

        // JavaScript map points
        // see also "writeMapEvents_json", "writeMapEvents_xml"
        JavaScriptTools.writeStartJavaScript(pw);
        pw.write("function trackMapOnLoad() {\n");
        pw.write("   var mapPts =\n");
        TimeZone tz = reqState.getTimeZone();
        if (EventUtil.IsXMLMapDataFormat()) {
            // XML
            pw.write("\"<"+EventUtil.TAG_MapData+">\\n\" +\n");
            pw.write("\"<"+EventUtil.TAG_DataSet+" type=\\\""+type+"\\\" route=\\\""+!isFleet+"\\\">\\n\" +\n");
            int evNdx = 0;
            for (DBDataIterator dbi = report.getBodyDataIterator(); dbi.hasNext();) {
                Object ev = dbi.next().getRowObject();
                if (ev instanceof EventDataProvider) {
                    EventDataProvider edp = (EventDataProvider)ev;
                    edp.setEventIndex(evNdx++);
                    if (!dbi.hasNext()) { edp.setIsLastEvent(true); }
                    String rcd = StringTools.replace(this._formatMapEvent(edp,reqState,report),"\"","\\\"");
                    pw.write("\"<"+EventUtil.TAG_Point+"><![CDATA[" + rcd + "]]></"+EventUtil.TAG_Point+">\\n\" +\n");
                    //pw.write("\"" + rcd + "\\n\" +\n");
                } else {
                    Print.logWarn("Not an EventDataProvider: " + StringTools.className(ev));
                }
            }
            pw.write("\"</"+EventUtil.TAG_DataSet+">\\n\" +\n");
            if (showPPBox > 0) {
            pw.write("\"<"+EventUtil.TAG_Action+" command=\\\"showpp\\\">"+showPPBox+"</Action>\\n\" +\n");
            }
            if (zoomPP > 0) {
            pw.write("\"<"+EventUtil.TAG_Action+" command=\\\"zoompp\\\">"+zoomPP+"</Action>\\n\" +\n");
            }
            pw.write("\"</"+EventUtil.TAG_MapData+">\\n\" +\n");
            pw.write("\"\";\n");
        } else {
            // JSON
            JSON._Object jsonObj = new JSON._Object();
            JSON._Object JMapData = new JSON._Object();
            jsonObj.addKeyValue(EventUtil.JSON_JMapData,JMapData);
            JSON._Array DataSets = new JSON._Array();
            JMapData.addKeyValue(EventUtil.JSON_DataSets,DataSets);
            JSON._Object dataSetObj = new JSON._Object();
            DataSets.addValue(dataSetObj);
            dataSetObj.addKeyValue(EventUtil.JSON_type,type);
            dataSetObj.addKeyValue(EventUtil.JSON_route,!isFleet);
            JSON._Array pointArray = new JSON._Array();
            dataSetObj.addKeyValue(EventUtil.JSON_Points,pointArray);
            int evNdx = 0;
            for (DBDataIterator dbi = report.getBodyDataIterator(); dbi.hasNext();) {
                Object ev = dbi.next().getRowObject();
                if (ev instanceof EventDataProvider) {
                    EventDataProvider edp = (EventDataProvider)ev;
                    edp.setEventIndex(evNdx++);
                    if (!dbi.hasNext()) { edp.setIsLastEvent(true); }
                    String rcd = this._formatMapEvent(edp,reqState,report);
                    pointArray.addValue(rcd);
                } else {
                    Print.logWarn("Not an EventDataProvider: " + StringTools.className(ev));
                }
            }
            if ((showPPBox > 0) || (zoomPP > 0)) {
                JSON._Array Actions = new JSON._Array();
                JMapData.addKeyValue(EventUtil.JSON_Actions,Actions);
                if (showPPBox > 0) {
                    JSON._Object showpp = new JSON._Object();
                    Actions.addValue(showpp);
                    showpp.addKeyValue(EventUtil.JSON_cmd,"showpp");
                    showpp.addKeyValue(EventUtil.JSON_arg,String.valueOf(showPPBox));
                }
                if (zoomPP > 0) {
                    JSON._Object zoompp = new JSON._Object();
                    Actions.addValue(zoompp);
                    zoompp.addKeyValue(EventUtil.JSON_cmd,"zoompp");
                    zoompp.addKeyValue(EventUtil.JSON_arg,String.valueOf(zoomPP));
                }
            }
            pw.write("\"");
            String jsonStr = jsonObj.toString(false);
            jsonStr = StringTools.replace(jsonStr,"\\","\\\\");
            jsonStr = StringTools.replace(jsonStr,"\"","\\\"");
            pw.write(jsonStr);
            pw.write("\";\n");
        }
        pw.write("   mapProviderParseXML(mapPts);\n");
        pw.write("}\n");
        pw.write("function trackMapOnUnload() {\n");
        pw.write("   mapProviderUnload();\n");
        pw.write("}\n");
        JavaScriptTools.writeEndJavaScript(pw);

        pw.write("</head>\n");
        pw.write("\n");
        
        // HTML Body
        pw.write("<body onload=\"javascript:trackMapOnLoad();\" onunload=\"javascript:trackMapOnUnload();\">\n"); 
        //pw.write(" leftmargin='0' rightmargin='0' topmargin='0' bottommargin='0'>\n");
        pw.write("<div>\n"); //  style='align:center; width:99%; height:99%;'>\n");
        mapProvider.writeMapCell(pw, reqState, new MapDimension());
        pw.write("</div>\n");
        pw.write("</body>\n");
        
        // HTML end
        pw.write("</html>\n");
        pw.close();

    }

    // ------------------------------------------------------------------------

    private void _writeReportKML(HttpServletResponse response, RequestProperties reqState, ReportData report)
        throws ReportException, IOException
    {
        PrintWriter pw = response.getWriter();
        PrivateLabel privLabel = reqState.getPrivateLabel();
        Account account = reqState.getCurrentAccount();

        /* events */
        OrderedMap<Device,java.util.List<EventData>> devMap = new OrderedMap<Device,java.util.List<EventData>>();
        for (DBDataIterator dbi = report.getBodyDataIterator(); dbi.hasNext();) {

            Object ev = dbi.next().getRowObject();
            if (!(ev instanceof EventData)) {
                Print.logWarn("Not an EventData: " + StringTools.className(ev));
                continue;
            }
            EventData ed = (EventData)ev;

            /* add to Device */
            Device dev = ed.getDevice();
            java.util.List<EventData> edList = devMap.get(dev);
            if (edList == null) {
                edList = new Vector<EventData>();
                devMap.put(dev,edList);
            }
            edList.add(ed);

        }

        /* iterate through captured devices */
        Collection<Device> devList = new Vector<Device>();
        for (Device dev : devMap.keySet()) {
            java.util.List<EventData> edList = devMap.get(dev);
            EventData edArray[] = edList.toArray(new EventData[edList.size()]);
            for (int e = 0; e < edArray.length; e++) {
                edArray[e].setEventIndex(e);
                if (e == (edArray.length - 1)) {
                    edArray[e].setIsLastEvent(true);
                }
            }
            dev.setSavedRangeEvents(edArray);
            devList.add(dev);
        }

        /* KML output */
        GoogleKML.getInstance().writeEvents(pw, 
            account, devList, 
            privLabel);

    }

    // ------------------------------------------------------------------------

    private void _writeReportGraph(HttpServletResponse response, final RequestProperties reqState, final ReportData report, final I18N i18n)
        throws ReportException, IOException
    {
        Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.graphError","Error generating map: {0}",report.getReportName()));

        /* write frame */
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
        PrintWriter pw = response.getWriter();
        PrivateLabel privLabel = reqState.getPrivateLabel();
        
        // HTML start
        pw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
        pw.write("<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>\n");

        // HTML head
        pw.write("\n");
        pw.write("<head>\n");
        pw.write("  <meta http-equiv='content-type' content='text/html; charset=UTF-8'/>\n");
        pw.write("  <meta http-equiv='cache-control' content='no-cache'/>\n");
        pw.write("  <meta http-equiv='expires' content='0'/>\n"); // expires 'now'
        pw.write("  <meta name='copyright' content='"+privLabel.getCopyright()+"'/>\n");
        pw.write("  <meta name='robots' content='none'/>\n");
        pw.write("  <title>" + privLabel.getPageTitle() + "</title>\n");
        pw.write("</head>\n");
        pw.write("\n");
        
        // HTML body
        pw.write("<body>\n"); 
        String graphURL = EncodeURL(reqState, report.getGraphURL());
        if (!StringTools.isBlank(graphURL)) {
            pw.write("<img src='"+graphURL+"'/>\n");
        }
        pw.write("</body>\n");
        
        // HTML end
        pw.write("</html>\n");
        pw.close();

    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel        privLabel = reqState.getPrivateLabel();
        final I18N                i18n      = privLabel.getI18N(ReportDisplay.class);
        final Locale              locale    = reqState.getLocale();
        final HttpServletRequest  request   = reqState.getHttpServletRequest();
        final HttpServletResponse response  = reqState.getHttpServletResponse();
        String m = pageMsg;
        boolean error = false;

        /* report constraints */
        // account=<account> user=<user>
        // r_report=<report> 
        // device=<device> | group=<group>
        // date_fr=<ts> date_to=<ts> date_tz=<tz>
        // r_limit=<limit> r_limType=last|first
        // format=html|csv|xls|xml
        String reportID   = (String)AttributeTools.getRequestAttribute(request, PARM_REPORT     , "");
        String rptOption  = (String)AttributeTools.getRequestAttribute(request, PARM_REPORT_OPT , "");
        String rptText    = (String)AttributeTools.getRequestAttribute(request, PARM_REPORT_TEXT, ""); // not used
        String deviceID   = (String)AttributeTools.getRequestAttribute(request, PARM_DEVICE_ID  , "");
        String groupID    = (String)AttributeTools.getRequestAttribute(request, PARM_GROUP_ID   , ""); 
        String rangeFr    = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_RANGE_FR, "");
        String rangeTo    = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_RANGE_TO, "");
        String tzStr      = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_TIMEZONE, "");
        String limitStr   = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT      , "");
        String limTypStr  = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT_TYPE , ""); // not used
        String emailAddr  = (String)AttributeTools.getRequestAttribute(request, PARM_EMAIL_ADDR , ""); // not used

        /* report format */
        String rptFormat  = (String)AttributeTools.getRequestAttribute(request, PARM_FORMAT     , ReportURL.FORMAT_HTML);

        /* report menu */
        String rptMenu    = AttributeTools.getRequestString(request, PARM_MENU, PAGE_MENU_REPORT);
      //String rptMenuURL = EncodeMakeURL(reqState, RequestProperties.TRACK_BASE_URI(), rptMenu);
        String rptMenuURL = privLabel.getWebPageURL(reqState, rptMenu);
        //Print.logDebug("ReportMenu: %s => %s", rptMenu, rptMenuURL);

        /* get report */
        //String cmdName = reqState.getCommandName(); // should be COMMAND_REPORT_SELECT (but ignored)
        if (StringTools.isBlank(reportID)) {
            if (!StringTools.isBlank(deviceID)) {
                Print.logInfo("Assuming default Device report 'EventDetail'");
                reportID = "EventDetail";
            } else {
                Print.logError("No report specified");
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.noReport","No report requested"));
                return;
            }
        }
        ReportEntry reportEntry = privLabel.getReportEntry(reportID);
        if (reportEntry == null) {
            Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.notFound","Report not found: {0}",reportID));
            return;
        }
        ReportFactory reportFactory = reportEntry.getReportFactory();

        /* set "fleet" request type */
        boolean isGroup = reportFactory.getReportTypeIsGroup();
        reqState.setFleet(isGroup);
        reqState.setReport(true);
        
        /* Account/User/Device record */
        final Account account   = reqState.getCurrentAccount(); // should never be null
      //final String  accountID = reqState.getCurrentAccountID();
        final User    user      = reqState.getCurrentUser(); // may be null
        final Device  device;
        if (!isGroup && !StringTools.isBlank(deviceID)) {
            try {
                device = Device.getDevice(account, deviceID);
                if (device == null) {
                    Print.logError("Device does not exist: "  + deviceID);
                    Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.deviceNotFound","Report: {0}\\nDevice ''{1}'' not found.", reportID, deviceID));
                    return;
                }
            } catch (DBException dbe) {
                // TODO: change this to a different error message
                Print.logException("Device read error: "  + deviceID, dbe);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.deviceNotFound","Report: {0}\\nDevice ''{1}'' not found.", reportID, deviceID));
                return;
            }
        } else {
            device = null;
        }

        /* create report */
        final ReportData report;
        try {
            if (isGroup) {
                // Group/Summary report
                //Print.logInfo("Group/Summary report ...");
                if (!StringTools.isBlank(groupID)) {
                    if (groupID.equals(DeviceGroup.DEVICE_GROUP_ALL)) {
                        ReportDeviceList rdl = new ReportDeviceList(account, user);
                        rdl.addAllAuthorizedDevices();
                        report = reportFactory.createReport(reportEntry, rptOption, reqState, rdl);
                    } else {
                        DeviceGroup group = DeviceGroup.getDeviceGroup(account, groupID);
                        if (group == null) {
                            Print.logError("Group does not exist: "  + groupID);
                            Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.groupNotFound","Report: {0}\\nGroup ''{1}'' not found.", reportID, groupID));
                            return;
                        }
                        report = reportFactory.createReport(reportEntry, rptOption, reqState, group);
                    }
                } else {
                    Print.logError("Group not specified: "  + reportID);
                    Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.noGroupSelection","Report: {0}\\nNo Group selected.", reportID));
                    return;
                }
            } else {
                // Device/Detail report
                //Print.logInfo("Device/Detail report ...");
                if (device == null) {
                    Print.logError("Device not specified: "  + reportID);
                    Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.noDeviceSelection","Report: {0}\\nNo Device selected.", reportID));
                    return;
                }
                report = reportFactory.createReport(reportEntry, rptOption, reqState, device);
            }
        } catch (Throwable t) {
            Print.logException("Error generating report: "  + reportID, t);
            Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.reportError","Report: {0}\\nError generating report", reportID));
            return;
        }
        final ReportLayout reportLayout = report.getReportLayout();
        ReportConstraints rc = report.getReportConstraints();

        /* set refresh/map URL (FORMAT_HTML only) */
        if (rptFormat.equalsIgnoreCase(ReportURL.FORMAT_HTML)) {
            // - refresh URL
            URIArg refreshURL = reqState.getHttpServletRequestURIArg(false);
            if (refreshURL != null) {
                report.setRefreshURL(refreshURL.removeBlankValues());
            }
            // - automatic report URL
            URIArg autoReportURL = reqState.getHttpServletRequestURIArg(false);
            if (autoReportURL != null) {
                // timezone
                TimeZone tz      = DateTime.getTimeZone(autoReportURL.getArgValue(Calendar.PARM_TIMEZONE)); // will be GMT if invalid
                // now
                DateTime dtm_NW  = new DateTime(tz);
                long     day_NW  = DateTime.getDayNumberFromDate(dtm_NW);
                // convert "from" date (ie. "-1d,00:00")
                String   arg_FR  = URIArg.decodeArg(null,autoReportURL.getArgValue(Calendar.PARM_RANGE_FR)).toString();
                DateTime dtm_FR  = Calendar.parseDate(arg_FR, tz, false);
                if (dtm_FR == null) { 
                    Print.logWarn("Unable to parse 'from' date: " + arg_FR);
                    dtm_FR = Calendar.getCurrentDayStart(tz);
                }
                long     day_FR  = DateTime.getDayNumberFromDate(dtm_FR);
                long     delt_FR = day_FR - day_NW;
                String   new_FR  = ((delt_FR >= 0)? ("+"+delt_FR) : delt_FR) + "d," + dtm_FR.getHour24() + ":" + dtm_FR.getMinute();
                autoReportURL.removeArg(Calendar.PARM_RANGE_FR);
                autoReportURL.setArgValue(Calendar.PARM_RANGE_FR[0], new_FR);
                // convert "to" date (ie. +0d,23:59")
                String   arg_TO  = URIArg.decodeArg(null,autoReportURL.getArgValue(Calendar.PARM_RANGE_TO)).toString();
                DateTime dtm_TO  = Calendar.parseDate(arg_TO, tz, true );
                if (dtm_TO == null) { 
                    Print.logWarn("Unable to parse 'to' date: " + arg_TO);
                    dtm_TO = new DateTime(dtm_FR.getDayEnd(),tz); 
                }
                long     day_TO  = DateTime.getDayNumberFromDate(dtm_TO);
                long     delt_TO = day_TO - day_NW;
                String   new_TO  = ((delt_TO >= 0)? ("+"+delt_TO) : delt_TO) + "d," + dtm_TO.getHour24() + ":" + dtm_TO.getMinute();
                autoReportURL.removeArg(Calendar.PARM_RANGE_TO);
                autoReportURL.setArgValue(Calendar.PARM_RANGE_TO[0], new_TO);
                // save
                //Print.logInfo("Auto-Report URL: " + autoReportURL);
                report.setAutoReportURL(autoReportURL);
            }
            // - graph URL
            URIArg graphURL = reqState.getHttpServletRequestURIArg(false);
            if (graphURL != null) {
                graphURL.removeArg(PARM_FORMAT);
                graphURL.addArg(PARM_FORMAT[1], ReportData.FORMAT_GRAPH);
                //graphURL.setArgValue(PARM_DEVICE_ID, deviceID);
                report.setGraphURL(graphURL);
            }
            // - map URL
            URIArg mapURL = reqState.getHttpServletRequestURIArg(false);
            if (mapURL != null) {
                mapURL.removeArg(PARM_FORMAT);
                mapURL.addArg(PARM_FORMAT[1], ReportData.FORMAT_MAP);
                //graphURL.setArgValue(PARM_DEVICE_ID, deviceID);
                report.setMapURL(mapURL);
                //Print.logInfo("1) Map URL: " + mapURL);
                //Print.logInfo("2) Map URL: " + report.getMapURL());
            }
            // - kml URL
            URIArg kmlURL = reqState.getHttpServletRequestURIArg(false);
            if (kmlURL != null) {
                kmlURL.addExtension(".kml");
                kmlURL.removeArg(PARM_FORMAT);
                kmlURL.addArg(PARM_FORMAT[1], ReportData.FORMAT_KML);
                //graphURL.setArgValue(PARM_DEVICE_ID, deviceID);
                report.setKmlURL(kmlURL);
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
        TimeZone tz = DateTime.getTimeZone(tzStr); // will be GMT if invalid
        AttributeTools.setSessionAttribute(request, Calendar.PARM_TIMEZONE[0], tzStr);
        reqState.setTimeZone(tz, tzStr);

        /* Event from/to date range */
        DateTime dateFr = null;
        DateTime dateTo = null;
        // "From" date
        if (rangeFr.equalsIgnoreCase("last")) {
            if (device != null) {
                // "last" event time, start of day
                try {
                    EventData lastEv = device.getLastEvent(-1L/*endTime*/, false/*validGPS*/);
                    if (lastEv != null) {
                        dateFr = new DateTime((new DateTime(lastEv.getTimestamp(),tz)).getDayStart(),tz);
                        if (rangeTo.equals("last")) {
                            dateTo = new DateTime((new DateTime(lastEv.getTimestamp(),tz)).getDayEnd(),tz);
                        }
                    } else {
                        // no last event
                    }
                } catch (DBException dbe) {
                    Print.logException("Reading Device last event: " + reqState.getCurrentAccountID() + "/" + deviceID, dbe);
                }
            }
            if (dateFr == null) { 
                // default to current day start
                dateFr = Calendar.getCurrentDayStart(tz); 
            }
        } else
        if (!rangeFr.equals("")) {
            dateFr = Calendar.parseDate(rangeFr, tz, false);
            if (dateFr == null) { 
                Print.logWarn("Unable to parse 'From' date: " + rangeFr);
                dateFr = Calendar.getCurrentDayStart(tz); 
            }
        } else {
            dateFr = Calendar.getCurrentDayStart(tz); // does not return null
        }
        // "To" date
        if (dateTo != null) {
            // skip, already set
        } else
        if (rangeTo.equalsIgnoreCase("last")) {
            if (device != null) {
                // "last" event time, end of day
                try {
                    EventData lastEv = device.getLastEvent(-1L/*endTime*/, false/*validGPS*/);
                    if (lastEv != null) {
                        dateTo = new DateTime((new DateTime(lastEv.getTimestamp(),tz)).getDayEnd(),tz);
                    } else {
                        // no last event
                    }
                } catch (DBException dbe) {
                    Print.logException("Reading Device last event: " + reqState.getCurrentAccountID() + "/" + deviceID, dbe);
                }
            }
            if (dateTo == null) { 
                // default to current day end
                dateTo = Calendar.getCurrentDayEnd(tz);  // does not return null
            }
        } else
        if (rangeTo.equalsIgnoreCase("from")) {
            // end of day, relative to "From" date
            dateTo = new DateTime(dateFr.getDayEnd(), tz);
        } else
        if (!rangeTo.equals("")) {
            dateTo = Calendar.parseDate(rangeTo, tz, false);
            if (dateTo == null) { 
                Print.logWarn("Unable to parse 'To' date: " + rangeTo);
                dateTo = Calendar.getCurrentDayEnd(tz); 
            }
        } else {
            dateTo = Calendar.getCurrentDayEnd(tz);  // does not return null
        }
        // set calculated dates
        reqState.setEventDateFrom(dateFr);
        reqState.setEventDateTo(  dateTo);
        long timeStart  = (dateFr != null)? dateFr.getTimeSec() : -1L;
        long timeEnd    = (dateTo != null)? dateTo.getTimeSec() : -1L;
        rc.setTimeRange(timeStart, timeEnd);
        AttributeTools.setSessionAttribute(request, Calendar.PARM_RANGE_FR[0], Calendar.formatArgDateTime(dateFr));
        AttributeTools.setSessionAttribute(request, Calendar.PARM_RANGE_TO[0], Calendar.formatArgDateTime(dateTo));

        /* limit */
        //if (!StringTools.isBlank(limitStr)) {
        //    long limit = StringTools.parseLong(limitStr, -1L);
        //    if (limit > 0L) {
        //        if (limit > MAX_LIMIT) { limit = MAX_LIMIT; }
        //        rc.setSelectionLimit(EventData.LimitType.LAST, limit);
        //    }
        //} else {
        //    if (!rc.hasSelectionLimit()) {
        //        // set a limit if no limit has been set
        //        rc.setSelectionLimit(EventData.LimitType.LAST, MAX_LIMIT);
        //    }
        //}

        /* valid gps? */
        //rc.setValidGPSRequired(false);
        
        /* store vars as session attributes */
        AttributeTools.setSessionAttribute(request, PARM_REPORT[0]    , reportID);
        AttributeTools.setSessionAttribute(request, PARM_DEVICE_ID    , deviceID);
        AttributeTools.setSessionAttribute(request, PARM_GROUP_ID     , groupID);
        AttributeTools.setSessionAttribute(request, PARM_LIMIT[0]     , limitStr);
        AttributeTools.setSessionAttribute(request, PARM_LIMIT_TYPE[0], limTypStr); // not used

        /* report post initialization */
        // After all external configuration and constraints have been set
        report.postInitialize();

        /* XML output? */
        // output as XML to browser
        if (rptFormat.equalsIgnoreCase(ReportURL.FORMAT_XML)) {
            try {
                //CommonServlet.setResponseContentType(response, HTMLTools.MIME_XML());
                // (See "org.opengts.war.report.ReportTable:writeXML")
                OutputProvider op = new OutputProvider(response);
                int count = report.writeReport(ReportURL.FORMAT_XML, op);
                if (count > 0) {
                    // all is ok (XML report written)
                    return;
                } else {
                    Print.logWarn("XML Date/Time range contains no data: "  + reportID);
                    m = i18n.getString("ReportDisplay.xmlNoData","The selected Date/Time range contains no data.\\nReport: {0}",reportFactory.getReportTitle(locale));
                    //Track.writeErrorResponse(reqState, m);
                    error = true;
                    // TODO: what we really want to do here is redisplay the ReportMenu with the appropriate alert dialog
                    WebPage rptPage = privLabel.getWebPage(rptMenu);
                    if (rptPage != null) {
                        rptPage.writePage(reqState, m);
                    }
                    return;
                }
            } catch (ReportException re) {
                Print.logException("Error generating XML: " + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.xmlError","Error generating XML: {0}",reportID));
            }
            return;
        }

        /* CSV output? */
        // output as CSV/TXT to browser
        if (rptFormat.equalsIgnoreCase(ReportURL.FORMAT_CSV) ||
            rptFormat.equalsIgnoreCase(ReportURL.FORMAT_TXT)) {
            try {
                //CommonServlet.setResponseContentType(response, HTMLTools.MIME_CSV());
                // (See "org.opengts.war.report.ReportTable:writeCSV")
                OutputProvider op = new OutputProvider(response);
                int count = report.writeReport(rptFormat.toUpperCase(), op);
                if (count > 0) {
                    // all is ok
                    return;
                } else {
                    Print.logWarn("CSV Date/Time range contains no data: "  + reportID);
                    m = i18n.getString("ReportDisplay.csvNoData","The selected Date/Time range contains no data.\\nReport: {0}",reportFactory.getReportTitle(locale));
                    //Track.writeErrorResponse(reqState, m);
                    error = true;
                    // TODO: what we really want to do here is redisplay the ReportMenu with the appropriate alert dialog
                    WebPage rptPage = privLabel.getWebPage(rptMenu);
                    if (rptPage != null) {
                        rptPage.writePage(reqState, m);
                    }
                    return;
                }
            } catch (ReportException re) {
                Print.logException("Error generating CSV: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.csvError","Error generating CSV: {0}",reportID));
            }
            return;
        }

        /* XLS/XLSX output? */
        // output as XLS/XLSX to browser
        if (rptFormat.equalsIgnoreCase(ReportURL.FORMAT_XLS) ||
            rptFormat.equalsIgnoreCase(ReportURL.FORMAT_XLSX)) {
            try {
                //CommonServlet.setResponseContentType(response, HTMLTools.MIME_XLS());
                // (See "org.opengts.war.report.ReportTable:writeXLS")
                OutputProvider op = new OutputProvider(response);
                int count = report.writeReport(rptFormat.toUpperCase(), op);
                if (count > 0) {
                    // all is ok
                    return;
                } else
                if (op.hasOutputStream()) {
                    // the "response" has already been used as an "OutputStream"
                    return;
                } else {
                    Print.logWarn("XLS Date/Time range contains no data: "  + reportID);
                    m = i18n.getString("ReportDisplay.xlsNoData","The selected Date/Time range contains no data.\\nReport: {0}",reportFactory.getReportTitle(locale));
                    //Track.writeErrorResponse(reqState, m);
                    error = true;
                    // TODO: what we really want to do here is redisplay the ReportMenu with the appropriate alert dialog
                    WebPage rptPage = privLabel.getWebPage(rptMenu);
                    if (rptPage != null) {
                        rptPage.writePage(reqState, m);
                    }
                    return;
                }
            } catch (ReportException re) {
                Print.logException("Error generating XLS: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.xlsError","Error generating XLS[X]: {0}",reportID));
            }
            return;
        }

        /* graph? */
        // output as graph image to browser
        if (rptFormat.equalsIgnoreCase(ReportData.FORMAT_GRAPH)) {
            try {
                this._writeReportGraph(response, reqState, report, i18n);
            } catch (ReportException re) {
                Print.logException("Error generating Map: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.mapError","Error generating map: {0}",reportID));
            }
            return;
        }

        /* map? */
        // output as map to browser
        if (rptFormat.equalsIgnoreCase(ReportData.FORMAT_MAP)) {
            try {
                this._writeReportMap(response, reqState, report, i18n);
            } catch (ReportException re) {
                Print.logException("Error generating Map: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.mapError","Error generating map: {0}",reportID));
            }
            return;
        }

        /* KML? */
        // output as KML to browser
        if (rptFormat.equalsIgnoreCase(ReportData.FORMAT_KML)) {
            try {
                this._writeReportKML(response, reqState, report);
            } catch (ReportException re) {
                Print.logException("Error generating KML: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.kmlError","Error generating KML: {0}",reportID));
            }
            return;
        }

        /* style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter pw) throws IOException {
                try {
                    pw.write("\n");
                    pw.write("<!-- Begin Report Style -->\n");
                    String cssDir = ReportDisplay.this.getCssDirectory(); 
                    WebPageAdaptor.writeCssLink(pw, reqState, "ReportDisplay.css", cssDir);
                    if (reportLayout.hasCSSFiles()) {
                        for (String file : reportLayout.getCSSFiles(true)) {
                            WebPageAdaptor.writeCssLink(pw, reqState, file, cssDir);
                        }
                    }
                    report.writeReportStyle(ReportURL.FORMAT_HTML, new OutputProvider(pw));
                    pw.write("<!-- End Report Style -->\n");
                    pw.write("\n");
                } catch (ReportException re) {
                    throw new IOException(re.getMessage());
                }
            }
        };

        /* JavaScript */
        final boolean isTableSortable = reportFactory.isTableSortable();
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter pw) throws IOException {
                if (isTableSortable) {
                    JavaScriptTools.writeJSInclude(pw, JavaScriptTools.qualifyJSFileRef(ReportPresentation.SORTTABLE_JS), request);
                }
            }
        };

        /* report */
        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_REPORT_DISPLAY, m) {
            public void write(PrintWriter pw) throws IOException {
                try {
                    report.writeReport(ReportURL.FORMAT_HTML, new OutputProvider(pw));
                } catch (ReportException re) {
                    throw new IOException(re.getMessage());
                }
            }
        };

        /* Tag as periodic report */
        // save report paramters
        if (rptFormat.equalsIgnoreCase(ReportURL.FORMAT_SCHEDULE)) {
            URIArg reportURL = reqState.getHttpServletRequestURIArg(false/*inclUserPass*/);
            reportURL.removeArg(Constants.PARM_ENCPASS); // remove password, if present
            reportURL.removeBlankValues(); // remove unused arguments
            // now
            TimeZone _tz    = DateTime.getTimeZone(reportURL.getArgValue(Calendar.PARM_TIMEZONE));
            DateTime _dtNow = new DateTime(_tz);
            long     _dyNow = DateTime.getDayNumberFromDate(_dtNow);
            // from
            String   _dtFrS = HTMLTools.decodeParameter(reportURL.getArgValue(Calendar.PARM_RANGE_FR));
            DateTime _dtFr  = Calendar.parseDate(_dtFrS, _tz, false);
            long     _dyFr  = DateTime.getDayNumberFromDate(_dtFr);
            Print.logInfo("'From' Date: " + _dtFrS + " ==> " + _dtFr + " [Day# " + _dyFr);
            // To
            String   _dtToS = HTMLTools.decodeParameter(reportURL.getArgValue(Calendar.PARM_RANGE_TO));
            DateTime _dtTo  = Calendar.parseDate(_dtToS, _tz, true );
            long     _dyTo  = DateTime.getDayNumberFromDate(_dtTo);
            Print.logInfo("'To' Date  : " + _dtToS + " ==> " + _dtTo + " [Day# " + _dyTo);
            // format
            reportURL.setArgValue(ReportURL.RPTARG_FORMAT, ReportURL.FORMAT_HTML);
            // Range
            if ((_dtFr == null) || (_dtTo == null)) {
                Print.logError("Unable to parse from/to dates");
            } else {
                String _ranFr = ((_dyFr<_dyNow)?"":"+") + String.valueOf(_dyFr-_dyNow) + "d," + _dtFr.format("HH:mm:ss");
                String _ranTo = ((_dyTo<_dyNow)?"":"+") + String.valueOf(_dyTo-_dyNow) + "d," + _dtTo.format("HH:mm:ss");
                reportURL.setArgValue(Calendar.PARM_RANGE_FR, _ranFr);
                reportURL.setArgValue(Calendar.PARM_RANGE_TO, _ranTo);
                // save this URL "?.....&date_fr=-3d,00:00:00&date_to=+0,23:59:59&..."
                Print.logInfo("Report URL: " + reportURL);
            }
            // redisplay the Report Menu
            WebPage rptPage = privLabel.getWebPage(rptMenu);
            if (rptPage != null) {
                rptPage.writePage(reqState, m);
            }
            return;
        }

        /* EMail report */
        // output as EMail to SMTP server
        if (rptFormat.equalsIgnoreCase(ReportURL.FORMAT_EHTML)) {
            // set Report JSP
            String uri = privLabel.getJSPFile("emailReport", false);
            //Print.logInfo("Embedded Report JSP: " + uri);
            reqState.setWebPageURI(uri);
            reqState.setEncodeEMailHTML(true);
            URIArg emailURL = reqState.getHttpServletRequestURIArg(true);
            RTProperties emailLinkProps = null;
            if (emailURL != null) {
                emailURL.removeArg(ReportURL.RPTARG_FORMAT);
                emailURL.addArg(ReportURL.RPTARG_FORMAT[1],ReportURL.FORMAT_HTML);
                if (privLabel.hasDefaultBaseURL()) {
                    emailURL.setURI(privLabel.getDefaultBaseURL());
                }
                Print.logInfo("EMail URL(1): " + emailURL);
                // ---
                String rtpVal = URIArg.encodeRTP(emailURL.getArgProperties());
                emailURL = new URIArg(emailURL.getURI(),true);
                emailURL.addArg(AttributeTools.ATTR_RTP, rtpVal);
                //Print.logInfo("EMail URL(2): " + emailURL);
                emailLinkProps = new RTProperties();
                emailLinkProps.setString("EMailReport.url" , emailURL.toString());
                emailLinkProps.setString("EMailReport.desc", i18n.getString("ReportDisplay.webBrowserLink", "Web Link"));
            }
            // write report byte array
            //Print.logInfo("Report JSP: " + reqState.getJspURI());
            HttpServletResponse httpResp = reqState.getHttpServletResponse();
            BufferedHttpServletResponse bhsp = new BufferedHttpServletResponse(httpResp);
            reqState.setHttpServletResponse(bhsp);
            try {
                if (emailLinkProps != null) {
                    RTConfig.pushTemporaryProperties(emailLinkProps);
                }
                CommonServlet.writePageFrame(
                    reqState,
                    null,null,                  // onLoad/onUnload
                    HTML_CSS,                   // Style sheets
                    HTML_JS,                    // JavaScript
                    null,                       // Navigation
                    HTML_CONTENT);              // Content
            } finally {
                if (emailLinkProps != null) {
                    RTConfig.popTemporaryProperties(emailLinkProps);
                }
            }
            // Debug, display report html to stdout
            //String s = bhsp.toString();
            //Print.logInfo("Report HTML:\n" + s);
            if (emailAddr.equals("INLINE")) {
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
                PrintWriter out = response.getWriter();
                out.write(bhsp.toString());
                out.close();
                return;
            } else {
                // email report
                int logLevel = Print.getLogLevel();
                Print.setLogLevel(Print.LOG_ALL); // all debug logging
                try {
                    String frEmail = (privLabel != null)? privLabel.getEventNotificationFrom() : null;
                    String toEmail = emailAddr; // Account.getReportEmailAddress(account,user);
                    if (StringTools.isBlank(frEmail)) {
                        Print.logWarn("'From' email address has not been configured");
                        m = i18n.getString("ReportDisplay.missingFromEmail","The 'From' email address has not been configured");
                    } else
                    if (StringTools.isBlank(toEmail)) {
                        Print.logWarn("No email recipients have been specified");
                        m = i18n.getString("ReportDisplay.missingToEmail","No recipient email address has been specified");
                    } else {
                        String subj = i18n.getString("ReportDisplay.reportTitle","Report") + ": " + 
                            report.getReportTitle();
                        String body = subj; //  + "\n" + StringTools.trim(emailURL.toString());
                        byte rptAttach[] = bhsp.toByteArray();
                        SendMail.Attachment attach = new SendMail.Attachment(
                            rptAttach, 
                            reportID + ".html", 
                            HTMLTools.MIME_HTML());
                        SendMail.send(frEmail, toEmail, subj, body, attach);
                        Print.logInfo("Email sent to: " + toEmail);
                        m = i18n.getString("ReportDisplay.reportEmailed","The selected report has been emailed");
                        if (DEBUG_SAVE_EHTML_TO_TMP) {
                            // debug purposes only
                            File rptFile = new File("/tmp/" + reportID + ".html");
                            FileTools.writeFile(rptAttach, rptFile);
                        }
                    }
                } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                    // this will fail if JavaMail support for SendMail is not available.
                    Print.logWarn("SendMail error: " + t);
                    m = i18n.getString("ReportDisplay.sendMailError","An error occurred while attempting to send email");
                } finally {
                    Print.setLogLevel(logLevel);
                }
            }
            // redisplay the Report Menu
            reqState.setHttpServletResponse(httpResp);
            reqState.setWebPageURI(null);
            reqState.setEncodeEMailHTML(false);
            WebPage rptPage = privLabel.getWebPage(rptMenu);
            if (rptPage != null) {
                rptPage.writePage(reqState, m);
            }
            return;
        }
        
        /* write report to client browser output stream */
        String onload = error? JS_alert(true,m) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------

}
