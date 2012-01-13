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
//  2007/12/13  Martin D. Flynn
//     -Add indication of partial displayed data if the record limit was reached.
//  2008/03/12  Martin D. Flynn
//     -Changed the partial displayed data message to span 3 table columns.
//  2009/09/23  Clifton Flynn, Martin D. Flynn
//     -Added SOAP xml support
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class ReportTable
    implements ReportPresentation
{

    // ------------------------------------------------------------------------

    public static final String CSS_CLASS_TABLE      = ReportLayout.CSS_CLASS_TABLE;
    public static final String CSS_CLASS_TABLE_SORT = ReportLayout.CSS_CLASS_TABLE_SORT;

    public static final int    INDENT               = ReportPresentation.INDENT;

    // ------------------------------------------------------------------------

    public static final String TAG_Account          = "Account";
    public static final String TAG_TimeFrom         = "TimeFrom";
    public static final String TAG_TimeTo           = "TimeTo";
    public static final String TAG_ValidGPSRequired = "ValidGPSRequired";
    public static final String TAG_SelectionLimit   = "SelectionLimit";
    public static final String TAG_Ascending        = "Ascending";
    public static final String TAG_ReportLimit      = "ReportLimit";
    public static final String TAG_Where            = "Where";
    public static final String TAG_RuleSelector     = "RuleSelector";
    public static final String TAG_Title            = "Title";
    public static final String TAG_Subtitle         = "Subtitle";
    public static final String TAG_Partial          = "Partial";
    public static final String TAG_Report           = "Report";
    public static final String TAG_ReportUrl        = "ReportUrl";
    public static final String TAG_ReportHtml       = "ReportHtml";
    public static final String TAG_ReportEmail      = "ReportEmail";
    public static final String TAG_Message          = "Message";

    public static final String ATTR_timestamp       = "timestamp";
    public static final String ATTR_timezone        = "timezone";
    public static final String ATTR_name            = "name";
    public static final String ATTR_type            = "type";
    public static final String ATTR_format          = "format";
    public static final String ATTR_encoding        = "encoding";
    public static final String ATTR_sent            = "sent";

    // ------------------------------------------------------------------------

    private ReportHeader rptHeader = null;
    private ReportBody   rptBody   = null;
    
    private Map<String,HeaderColumnTemplate> headerColumnMap = null;
    private Map<String,BodyColumnTemplate>   bodyColumnMap   = null;

    // ------------------------------------------------------------------------

    public ReportTable()
    {
        this(null, null);
    }
    
    protected ReportTable(ReportHeader rh, ReportBody rb)
    {
        this.rptHeader       = (rh != null)? rh : new ReportHeader(this);
        this.rptBody         = (rb != null)? rb : new ReportBody(this);
        this.headerColumnMap = new HashMap<String,HeaderColumnTemplate>();
        this.bodyColumnMap   = new HashMap<String,BodyColumnTemplate>();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public int writeReport(String format, ReportData rd, OutputProvider out, int indentLevel)
        throws ReportException
    {
        ReportURL.Format rptFormat = EnumTools.getValueOf(ReportURL.Format.class, format);
        return this.writeReport(rptFormat, rd, out, indentLevel);
    }

    public int writeReport(ReportURL.Format rptFormat, ReportData rd, OutputProvider out, int indentLevel)
        throws ReportException
    {
        if (rptFormat == null) {
            rptFormat = ReportURL.Format.HTML;
        }
        switch (rptFormat) {
            case XML     :
            case SOAP    :
            case URL     :
            case EMAIL   :
            case EHTML   :
                return this.writeXML( out, indentLevel, rd, rptFormat);
            case CSV     :
                return this.writeCSV( out, indentLevel, rd, true /*mimeCSV*/);  // csv
            case XLS     :
                return this.writeXLS( out, indentLevel, rd, false/*xlsx*/   );  // xls
            case XLSX    :
                return this.writeXLS( out, indentLevel, rd, true /*xlsx*/   );  // xlsx
            case TXT     :
                return this.writeCSV( out, indentLevel, rd, false/*mimeCSV*/);  // text/plain
            case CALLBACK:
                return this.writeCallback(out, indentLevel, rd);
            case HTML    :
            default:
                return this.writeHTML(out, indentLevel, rd);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeHTML(OutputProvider out, int level, ReportData rd) 
        throws ReportException
    {
        PrintWriter pw = null;
        try {
            pw = out.getWriter();
        } catch (IOException ioe) {
            throw new ReportException("PrintWriter error", ioe);
        }

        /* no ReportData */
        if (rd == null) {
            return 0;
        }

        /* simple report */
        if (!rd.isSingleDeviceOnly() || (rd.getDeviceCount() <= 1)) {
            return this._writeHTML(pw, level, rd, -1);
        }

        /* multiple per-device reports */
        ReportDeviceList rdl = rd.getReportDeviceList();
        java.util.List<ReportDeviceList.DeviceHolder> dhList = rdl.getDeviceHolderList(true);
        rdl.clear();
        int rcdCount = 0;
        int devCount = dhList.size();
        for (int i = 0; i < devCount; i++) {
            if (i > 0) {
                pw.print("<br>\n");
            }
            rdl.setDevice(null,dhList.get(i));
            rcdCount += this._writeHTML(pw, level, rd, i);
        }
        return rcdCount;

    }

    private int _writeHTML(PrintWriter out, int level, ReportData rd, int ndx) 
        throws ReportException
    {

        /* invalid PrintWriter? */
        if (out == null) {
            throw new ReportException("Invalid PrintWriter (null)");
        }

        /* attributes */
        RequestProperties reqState = rd.getRequestProperties();
        boolean isEMail = reqState.getEncodeEMailHTML();
        PrivateLabel privLabel = rd.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportTable.class);

        out.print("<center>\n");
        out.print("<table cellspacing='0' cellpadding='0' border='0'>\n");

        /* report title row */
        {
            String rptTitle = rd.getReportTitle();
            out.print("<tr><td colSpan='3'><H1 class=\"rptTitle\">" + FilterText(rptTitle) + "</H1></td></tr>\n");
        }
        
        /* report subtitle row */
        {
            out.print("<tr>\n");

            // "Refresh"
            out.print("<td>");
            StringBuffer linkSB_L = new StringBuffer();
            String refreshURL = (!isEMail && (ndx <= 0))? EncodeURL(reqState, rd.getRefreshURL()) : null;
            if (!StringTools.isBlank(refreshURL)) {
                String refreshDesc = i18n.getString("ReportTable.refreshReport","Refresh");
                if (linkSB_L.length() > 0) { linkSB_L.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_L.append("<a href='"+refreshURL+"' target='_self'>"+refreshDesc+"</a>"); // target='_top'
            }
            // ...
            if (linkSB_L.length() > 0) {
                out.print(linkSB_L.toString());
            } else {
                out.print("&nbsp;");
            }
            out.print("</td>\n");

            // Report subtitle
            out.print("<td width='100%'>");
            String rptSubtt = rd.getReportSubtitle();
            if (!StringTools.isBlank(rptSubtt)) {
                out.print("<H2 class=\"rptSubtitle\">" + FilterText(rptSubtt) + "</H2>");
            } else {
                out.print("&nbsp;");
            }
            out.print("</td>\n");

            // "Graph", "Map" links
            out.print("<td>");
            StringBuffer linkSB_R = new StringBuffer();
            String graphURL = (!isEMail && (ndx < 0) && rd.getSupportsGraphDisplay())? EncodeURL(reqState,rd.getGraphURL()) : null;
            if (!StringTools.isBlank(graphURL)) {
                MapDimension sz = rd.getGraphWindowSize();
                String desc = rd.getGraphLinkDescription();
                if (StringTools.isBlank(desc)) { desc = i18n.getString("ReportTable.displayGraph","Graph"); }
                if (linkSB_R.length() > 0) { linkSB_R.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_R.append("<span class='spanLink' onclick=\"javascript:openResizableWindow('"+graphURL+"','ReportGraph',"+sz.getWidth()+","+sz.getHeight()+");\">"+desc+"</span>");
            }
            String mapURL = (!isEMail && (ndx < 0) && rd.getSupportsMapDisplay())? EncodeURL(reqState,rd.getMapURL()) : null;
            if (!StringTools.isBlank(mapURL)) {
                MapDimension sz = rd.getMapWindowSize();
                String desc = rd.getMapLinkDescription();
                if (StringTools.isBlank(desc)) { desc = i18n.getString("ReportTable.displayMap","Map"); }
                if (linkSB_R.length() > 0) { linkSB_R.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_R.append("<span class='spanLink' onclick=\"javascript:openResizableWindow('"+mapURL+"','ReportMap',"+sz.getWidth()+","+sz.getHeight()+");\">"+desc+"</span>");
            }
            String kmlURL = (!isEMail && (ndx < 0) && rd.getSupportsKmlDisplay() && privLabel.getBooleanProperty(PrivateLabel.PROP_ReportDisplay_showGoogleKML,false))? EncodeURL(reqState,rd.getKmlURL()) : null;
            if (!StringTools.isBlank(kmlURL)) {
                String desc = rd.getKmlLinkDescription();
                if (StringTools.isBlank(desc)) { desc = i18n.getString("ReportTable.displayKML","KML"); }
                if (linkSB_R.length() > 0) { linkSB_R.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_R.append("<a href='"+kmlURL+"' target='_blank'>"+desc+"</a>");
            }
            // ...
            if (linkSB_R.length() > 0) {
                out.print(linkSB_R.toString());
            } else {
                out.print("&nbsp;");
            }
            out.print("</td>\n");

            out.print("</tr>\n");
        }

        /* start report */
        out.print("<tr>\n");
        out.print("<td colSpan='3'>\n");
        String tableClass = rd.getReportFactory().isTableSortable()? CSS_CLASS_TABLE_SORT : CSS_CLASS_TABLE;
        out.print("<table class='"+tableClass+"' width='100%' cellspacing='0' cellpadding='0' border='0'>\n");
        out.print("<!-- Report Header -->\n");
        this.rptHeader.writeHTML(out, level+1, rd);
        out.print("<!-- Report Data -->\n");
        this.rptBody.writeHTML(out, level+1, rd);
        out.print("</table>\n");
        out.print("</td>\n");
        out.print("</tr>\n");

        /* no/partial data indication */
        if (this.rptBody.getRecordCount() <= 0) {
            out.print("<tr>\n");
            out.print("<td colSpan='3'><H2 class=\"rptNoData\">");
            String t = i18n.getString("ReportTable.noData","This report contains no data");
            out.print(FilterText(t));
            out.print("</H2></td>\n");
            out.print("</tr>\n");
        } else
        if (this.rptBody.isPartial()) {
            out.print("<tr>\n");
            out.print("<td colSpan='3'><H2 class=\"rptPartial\">");
            String t = i18n.getString("ReportTable.partialData","This report has reached it's record display limit and may only contain a portion of the possible data");
            out.print(FilterText(t));
            out.print("</H2></td>\n");
            out.print("</tr>\n");
        }

        out.print("</table>\n");
        out.print("</center>\n");
        return this.rptBody.getRecordCount();

    }

    // ------------------------------------------------------------------------
    
    public static String EncodeURL(RequestProperties reqState, URIArg url)
    {
        return WebPageAdaptor.EncodeURL(reqState, url);
    }

    public static String FilterText(String s)
    {
        return WebPageAdaptor.FilterText(s);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeXML(OutputProvider out, int level, ReportData rd, ReportURL.Format rptFormat) 
        throws ReportException
    {
        int rcdCount = 0;
        rcdCount += this._writeXML(out, level, rd, rptFormat);
        return rcdCount;
    }

    private int _writeXML(OutputProvider out, int level, ReportData rd, ReportURL.Format rptFormat) 
        throws ReportException
    {

        /* PrintWriter */
        PrintWriter pw = null;
        try {
            pw = out.getWriter();
        } catch (IOException ioe) {
            throw new ReportException("PrintWriter error", ioe);
        }

        /* general properties */
        boolean isSoapRequest = rd.isSoapRequest();
        RequestProperties reqState = rd.getRequestProperties();
        PrivateLabel privLabel = rd.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportTable.class);
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);
        String PFX2 = XMLTools.PREFIX(isSoapRequest, (level + 1) * ReportTable.INDENT);

        /* begin */
        pw.print(PFX1);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_Report,    // TAG_Report
            XMLTools.ATTR(ATTR_name,rd.getReportName()) +       // ATTR_name
            XMLTools.ATTR(ATTR_type,rd.getReportType()) +       // ATTR_type
            XMLTools.ATTR(ATTR_format,rptFormat.toString()),    // ATTR_format
            false,true));

        /* constraints */
        ReportConstraints rc = rd.getReportConstraints();
        String   dtFmt = DateTime.DEFAULT_DATE_FORMAT + "," + DateTime.DEFAULT_TIME_FORMAT;
        TimeZone tzone = rd.getTimeZone();
        String   tzStr = rd.getTimeZoneString();
        long     tmBeg = rc.getTimeStart();
        long     tmEnd = rc.getTimeEnd();
        DateTime dtStr = new DateTime(tmBeg,tzone);
        DateTime dtEnd = new DateTime(tmEnd,tzone);

        /* Account */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_Account,"",false,false));           // TAG_Account
        pw.print(XmlFilter(isSoapRequest,rd.getAccountID()));
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_Account,true));                       // TAG_Account

        /* TimeFrom */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_TimeFrom,                           // TAG_TimeFrom
            XMLTools.ATTR(ATTR_timestamp,String.valueOf(tmBeg)) +                        // ATTR_timestamp
            XMLTools.ATTR(ATTR_timezone,tzStr),                                          // ATTR_timezone
            false,false));
        pw.print((tmBeg>0L)? XmlFilter(isSoapRequest,dtStr.format(dtFmt)) : "");
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_TimeFrom,true));                      // TAG_TimeFrom

        /* TimeTo */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_TimeTo,                             // TAG_TimeTo
            XMLTools.ATTR(ATTR_timestamp,String.valueOf(tmEnd)) +                        // ATTR_timestamp
            XMLTools.ATTR(ATTR_timezone,tzStr),                                          // ATTR_timezone
            false,false));
        pw.print((tmEnd>0L)? XmlFilter(isSoapRequest,dtEnd.format(dtFmt)) : "");
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_TimeTo,true));                        // TAG_TimeTo

        /* ValidGPSRequired */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_ValidGPSRequired,"",false,false));  // TAG_ValidGPSRequired
        pw.print(XmlFilter(isSoapRequest,rc.getValidGPSRequired()));
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_ValidGPSRequired,true));              // TAG_ValidGPSRequired

        /* SelectionLimit */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_SelectionLimit,                     // TAG_SelectionLimit
            XMLTools.ATTR("type",rc.getSelectionLimitType()),
            false,false));
        pw.print(XmlFilter(isSoapRequest,rc.getSelectionLimit()));                     
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_SelectionLimit,true));                // TAG_SelectionLimit

        /* Ascending */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_Ascending,"",false,false));         // TAG_Ascending
        pw.print(XmlFilter(isSoapRequest,rc.getOrderAscending()));
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_Ascending,true));                     // TAG_Ascending

        /* ReportLimit */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_ReportLimit,"",false,false));       // TAG_ReportLimit
        pw.print(XmlFilter(isSoapRequest,rc.getReportLimit()));
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_ReportLimit,true));                   // TAG_ReportLimit

        /* Where */
        if (rc.hasWhere()) {
            pw.print(PFX2);
            pw.print(XMLTools.startTAG(isSoapRequest,TAG_Where,"",false,false));         // TAG_Where
            pw.print(XmlFilter(isSoapRequest,rc.getWhere()));
            pw.print(XMLTools.endTAG(isSoapRequest,TAG_Where,true));                     // TAG_Where
        }
        
        /* RuleSelector */
        if (rc.hasRuleSelector()) {
            pw.print(PFX2);
            pw.print(XMLTools.startTAG(isSoapRequest,TAG_RuleSelector,"",false,false));  // TAG_RuleSelector
            pw.print(XmlFilter(isSoapRequest,rc.getRuleSelector()));
            pw.print(XMLTools.endTAG(isSoapRequest,TAG_RuleSelector,true));              // TAG_RuleSelector
        }

        /* Title */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_Title,"",false,false));             // TAG_Title
        pw.print(XmlFilter(isSoapRequest,rd.getReportTitle()));
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_Title,true));                         // TAG_Title

        /* Subtitle */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_Subtitle,"",false,false));          // TAG_Subtitle
        pw.print(XmlFilter(isSoapRequest,rd.getReportSubtitle()));
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_Subtitle,true));                      // TAG_Subtitle

        /* Generate report */
        // JSP "emailReport" (see ReportDisplay.java)
        if (rptFormat.equals(ReportURL.Format.URL)) {
            // Web-URL only
            HttpServletRequest request = reqState.getHttpServletRequest();
            ReportDeviceList devList = rd.getReportDeviceList();
            String deviceID = devList.isDeviceGroup()? null : devList.getFirstDeviceID();
            String groupID  = devList.isDeviceGroup()? devList.getDeviceGroupID() : null;
            // base URL
            //Print.logInfo("PrivateLabel: " + privLabel.getName());
            String baseURL  = privLabel.hasDefaultBaseURL()?
                privLabel.getDefaultBaseURL() :
                ((request != null)? request.getRequestURL().toString() : "");
            // construct report URL
            URIArg rptURL = ReportURL.createReportURL(
                baseURL, false,
                rd.getAccountID(), rd.getUserID(), "",
                deviceID, groupID,
                String.valueOf(rc.getTimeStart()), String.valueOf(rc.getTimeEnd()), rd.getTimeZoneString(),
                rd.getReportName(),
                String.valueOf(rc.getReportLimit()), rc.getSelectionLimitType().toString(),
                ReportURL.FORMAT_EHTML);
            // Print XML with URL
            pw.print(PFX2);
            pw.print(XMLTools.startTAG(isSoapRequest,TAG_ReportUrl,"",false,false));
            pw.print(XmlFilter(isSoapRequest,rptURL.toString()));
            pw.print(XMLTools.endTAG(isSoapRequest,TAG_ReportUrl,true));
        } else
        if (rptFormat.equals(ReportURL.Format.EMAIL)) {
            String rptMsg = "";
            boolean sent  = false;
            Print.logInfo("Generate and email HTML report ...");
            String htmlStr = ReportTable._writeHTMLReport(reqState, rd, false);
            // email report
            int logLevel = Print.getLogLevel();
            Print.setLogLevel(Print.LOG_ALL); // all debug logging
            try {
                Account account = rd.getAccount();
                User    user    = rd.getUser();
                String frEmail  = (privLabel != null)? privLabel.getEventNotificationFrom() : null;
                String toEmail  = rc.hasEmailAddresses()?
                    rc.getEmailAddresses() :
                    Account.getReportEmailAddress(account,user);
                if (StringTools.isBlank(frEmail)) {
                    Print.logWarn("'From' email address has not been configured");
                    rptMsg = "'From' email address not specified";
                    sent   = false;
                } else
                if (StringTools.isBlank(toEmail)) {
                    Print.logWarn("No email recipients have been specified");
                    rptMsg = "No email recipients specified";
                    sent   = false;
                } else {
                    String reportID = rd.getReportName();
                    String subj = i18n.getString("ReportDisplay.reportTitle","Report") + ": " + 
                        rd.getReportTitle();
                    String body = subj; //  + "\n" + StringTools.trim(emailURL.toString());
                    byte rptAttach[] = StringTools.getBytes(htmlStr);
                    SendMail.Attachment attach = new SendMail.Attachment(
                        rptAttach, 
                        reportID + ".html", 
                        HTMLTools.MIME_HTML());
                    SendMail.send(frEmail, toEmail, subj, body, attach);
                    Print.logInfo("Email sent to: " + toEmail);
                    rptMsg = "EMail sent: " + toEmail;
                    sent   = true;
                }
            } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                // this will fail if JavaMail support for SendMail is not available.
                Print.logException("SendMail error", t);
                rptMsg = "Internal SendMail error";
                sent   = false;
            } finally {
                Print.setLogLevel(logLevel);
            }
            // Print XML with URL
            pw.print(PFX2);
            pw.print(XMLTools.startTAG(isSoapRequest,TAG_ReportEmail,        // TAG_ReportEmail
                XMLTools.ATTR(ATTR_sent,(sent?"true":"false")),              // ATTR_sent
                false,false));
            pw.print(XmlFilter(isSoapRequest,rptMsg));
            pw.print(XMLTools.endTAG(isSoapRequest,TAG_ReportEmail,true));
        } else
        if (rptFormat.equals(ReportURL.Format.EHTML) || rptFormat.equals(ReportURL.Format.HTML)) {
            //Print.logInfo("Generating Base64 encoded HTML report ...");
            String htmlB64 = ReportTable._writeHTMLReport(reqState, rd, true);
            pw.print(PFX2);
            pw.print(XMLTools.startTAG(isSoapRequest,TAG_ReportHtml,        // TAG_ReportHtml
                XMLTools.ATTR(ATTR_encoding,"base64"),                      // ATTR_encoding
                false,false));
            pw.print(XMLTools.CDATA(isSoapRequest,htmlB64));
            pw.print(XMLTools.endTAG(isSoapRequest,TAG_ReportHtml,true));
        } else {
            // XML Report header/body
            this.rptHeader.writeXML(pw, level+1, rd);
            this.rptBody  .writeXML(pw, level+1, rd);
        }

        /* Partial */
        pw.print(PFX2);
        pw.print(XMLTools.startTAG(isSoapRequest,TAG_Partial,"",false,false));           // TAG_Partial
        pw.print(XmlFilter(isSoapRequest,this.rptBody.isPartial()));
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_Partial,true));                       // TAG_Partial

        /* end of report */
        pw.print(PFX1);
        pw.print(XMLTools.endTAG(isSoapRequest,TAG_Report,true));                        // TAG_Report
        return this.rptBody.getRecordCount();

    }

    /* write "emailReport" JSP */
    protected static String _writeHTMLReport(
        final RequestProperties reqState,
        final ReportData report,
        final boolean base64Encode)
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();

        /* style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter _pw) throws IOException {
                PrintWriter pw = _pw;
                try {
                    pw.write("\n");
                    pw.write("<!-- Begin Report Style -->\n");
                    String cssDir = privLabel.getCssDirectory(); 
                    WebPageAdaptor.writeCssLink(pw, reqState, "ReportDisplay.css", cssDir);
                    ReportLayout reportLayout = report.getReportLayout();
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

        /* report */
        final String CSS_REPORT_DISPLAY[] = new String[] { "reportDisplayTable", "reportDisplayCell" };
        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_REPORT_DISPLAY, "") {
            public void write(PrintWriter _pw) throws IOException {
                PrintWriter pw = _pw;
                try {
                    report.writeReport(ReportURL.FORMAT_HTML, new OutputProvider(pw));
                } catch (ReportException re) {
                    throw new IOException(re.getMessage());
                }
            }
        };

        /* set Report JSP */
        String uri = privLabel.getJSPFile("emailReport", false);
        reqState.setWebPageURI(uri);
        Print.logInfo("Embedded Report JSP: " + uri);
        Print.logInfo("Report JSP: " + reqState.getJspURI());

        /* email report properties */
        RTProperties emailLinkProps = null;
        reqState.setEncodeEMailHTML(true);
        URIArg emailURL = reqState.getHttpServletRequestURIArg(true);
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
            emailLinkProps.setString("EMailReport.desc", "Web Link"); // I18N
        }

        /* write report byte array */
        HttpServletResponse httpResp = reqState.getHttpServletResponse();
        BufferedHttpServletResponse bhsp = new BufferedHttpServletResponse(httpResp);
        reqState.setHttpServletResponse(bhsp);
        boolean success = true;
        try {
            if (emailLinkProps != null) {
                RTConfig.pushTemporaryProperties(emailLinkProps);
            }
            CommonServlet.writePageFrame(
                reqState,
                null,null,                  // onLoad/onUnload
                HTML_CSS,                   // Style sheets
                null,                       // JavaScript
                null,                       // Navigation
                HTML_CONTENT);              // Content
            success = true;
            if (!success) throw new IOException("Stupid hack to get around Java brain-dead requirements");
        } catch (IOException ioe) {
            success = false;
        } finally {
            if (emailLinkProps != null) {
                RTConfig.popTemporaryProperties(emailLinkProps);
            }
        }

        /* restore? */
        reqState.setHttpServletResponse(httpResp);
        reqState.setWebPageURI(null);
        reqState.setEncodeEMailHTML(false);

        /* split into reasonable sized text lines */
        if (success) {
            String htmlStr = bhsp.toString();
            if (base64Encode) {
                String       htmlB64 = Base64.encode(htmlStr);
                StringBuffer htmlSB  = new StringBuffer("\n");
                int          lineLen = 100;
                while (htmlB64.length() > lineLen) {
                    htmlSB.append(htmlB64.substring(0,lineLen)).append("\n");
                    htmlB64 = htmlB64.substring(lineLen);
                }
                htmlSB.append(htmlB64).append("\n");
                return htmlSB.toString();
            } else {
                return htmlStr;
            }
        } else {
            return "";
        }
        
    }

    // ------------------------------------------------------------------------

    private static final char XML_CHARS[] = new char[] { '_', '-', '.', ',', '/', '+', ':', '|', '=', ' ' };
    public static String XmlFilter(boolean isSoapReq, String value)
    {
        if ((value == null) || value.equals("")) { // do not use StringTools.isBlank (spaces are significant)
            return "";
        } else
        if (StringTools.isAlphaNumeric(value,XML_CHARS)) {
            return value; // return all significant spaces
        } else {
            String v = StringTools.replace(value,"\n","\\n");
            return XMLTools.CDATA(isSoapReq, v);
        }
    }

    public static String XmlFilter(boolean isSoapReq, long value)
    {
        return String.valueOf(value);
    }

    public static String XmlFilter(boolean isSoapReq, int value)
    {
        return String.valueOf(value);
    }

    public static String XmlFilter(boolean isSoapReq, boolean value)
    {
        return String.valueOf(value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeCSV(OutputProvider out, int level, ReportData rd, boolean mimeCSV) 
        throws ReportException
    {
        PrintWriter pw = null;
        try {
            pw = out.getWriter();
        } catch (IOException ioe) {
            throw new ReportException("PrintWriter error", ioe);
        }

        /* MIME type */
        // (See "org.opengts.war.track.page.ReportDisplay:writePage")
        HttpServletResponse response = rd.getRequestProperties().getHttpServletResponse();
        if (mimeCSV) {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_CSV());
        } else {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
        }

        /* header row */
        this.rptHeader.writeCSV(pw, level+1, rd);

        /* body */
        this.rptBody.writeCSV(pw, level+1, rd);
        return this.rptBody.getRecordCount();

    }

    public static String csvFilter(String value)
    {
        return StringTools.quoteCSVString(value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeXLS(OutputProvider out, int level, ReportData rd, boolean xlsx) 
        throws ReportException
    {

        /* MIME type */
        // (See "org.opengts.war.track.page.ReportDisplay:writePage")
        HttpServletResponse response = rd.getRequestProperties().getHttpServletResponse();
        if (xlsx) {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_XLSX());
        } else {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_XLS());
        }

        /* ReportSpreadsheet */
        ReportSpreadsheet rptSS = new ReportSpreadsheet(xlsx, rd);
        rptSS.setHeaderTitle(rd.getReportTitle());
        rptSS.setHeaderSubtitle(rd.getReportSubtitle());

        /* header row */
        // ==> ReportHeader
        //     ==> HeaderRowTemplate
        //         ==> HeaderColumnTemplate
        this.rptHeader.writeXLS(rptSS, level+1, rd);

        /* body */
        // ==> ReportBody
        //     ==> BodyRowTemplate
        //         ==> BodyColumnTemplate
        this.rptBody.writeXLS(rptSS, level+1, rd);
        
        /* write to output */
        OutputStream os = null;
        try {
            os = out.getOutputStream();
        } catch (IOException ioe) {
            throw new ReportException("'OutputStream' error", ioe);
        }
        boolean ok = rptSS.write(os);
        
        /* return record count */
        return ok? this.rptBody.getRecordCount() : 0;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeCallback(OutputProvider out, int level, ReportData rd) 
        throws ReportException
    {

        /* get callback method */
        ReportCallback rptCB = rd.getReportCallback();
        if (rptCB == null) {
            return 0;
        }

        /* start report */
        rptCB.reportStart(out, level);

        /* header row */
        this.rptHeader.writeCallback(out, level+1, rd);

        /* body */
        this.rptBody.writeCallback(out, level+1, rd);

        /* start report */
        rptCB.reportEnd(out, level);

        /* return body records written */
        return this.rptBody.getRecordCount();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected HeaderColumnTemplate _createHeaderColumnTemplate(DataColumnTemplate dct)
    {
        return new HeaderColumnTemplate(dct);
    }

    public HeaderColumnTemplate getHeaderColumnTemplate(DataColumnTemplate dct)
    {
        if (dct != null) {
            String keyName = dct.getKeyName();
            if (this.headerColumnMap.containsKey(keyName)) {
                return this.headerColumnMap.get(keyName);
            } else {
                HeaderColumnTemplate hct = this._createHeaderColumnTemplate(dct);
                this.headerColumnMap.put(keyName, hct);
                return hct;
            }
        } else {
            Print.logStackTrace("DataColumnTemplate is null!");
            return null;
        }
    }

    // ------------------------------------------------------------------------

    protected BodyColumnTemplate _createBodyColumnTemplate(DataColumnTemplate dct)
    {
        return new BodyColumnTemplate(dct.getKeyName());
    }

    public BodyColumnTemplate getBodyColumnTemplate(DataColumnTemplate dct)
    {
        if (dct != null) {
            String keyName = dct.getKeyName();
            if (this.bodyColumnMap.containsKey(keyName)) {
                return this.bodyColumnMap.get(keyName);
            } else {
                BodyColumnTemplate bct = this._createBodyColumnTemplate(dct);
                this.bodyColumnMap.put(keyName, bct);
                return bct;
            }
        } else {
            Print.logStackTrace("DataColumnTemplate is null!");
            return null;
        }
    }

    // ------------------------------------------------------------------------

}
