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
//  2008/07/21  Martin D. Flynn
//     -Added additional CSS class types
//     -Added support for forwarding requests to a JSP
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.db.*;
import org.opengts.util.*;

public class CommonServlet
    extends HttpServlet
{

    // ------------------------------------------------------------------------
    // global parameter definition

    public  static final String PARM_PAGE                       = "page";
    public  static final String PARM_COMMAND                    = PARM_PAGE + "_cmd";
    public  static final String PARM_ARGUMENT                   = PARM_COMMAND + "_arg";
    public  static final String PARM_CONTENT                    = "co";
    public  static final String PARM_LOCALE                     = "locale";

    // ------------------------------------------------------------------------
    // overriding host properties keys

    public  static final String HOST_PROPERTIES_ID              = "lfid";               // String
    public  static final String HOST_PROPERTIES                 = "hostProperties";     // RTProperties
    public  static final String DEFAULT_HOST_PROPERTIES_ID      = "default";            // default id

    // ------------------------------------------------------------------------
    // TagLib sections

    public  static final String SECTION_REQUESTPROPS            = "requestproperties";
    public  static final String SECTION_STYLESHEET              = "stylesheet";         // custom stylesheets
    public  static final String SECTION_JAVASCRIPT              = "javascript";         // custom javascript

    public  static final String SECTION_CSSFILE                 = "cssfile";

    public  static final String SECTION_BODY_ONLOAD             = "body.onload";
    public  static final String SECTION_BODY_ONUNLOAD           = "body.onunload";

    public  static final String SECTION_BANNER_WIDTH            = "banner.width";
    public  static final String SECTION_BANNER_STYLE            = "banner.style";
    public  static final String SECTION_BANNER_IMAGE            = "banner.image";
    public  static final String SECTION_BANNER_IMAGE_SOURCE     = "banner.image.source";
    public  static final String SECTION_BANNER_IMAGE_WIDTH      = "banner.image.width";
    public  static final String SECTION_BANNER_IMAGE_HEIGHT     = "banner.image.height";

    public  static final String SECTION_NAVIGATION              = "navigation";

    public  static final String SECTION_CONTENT_CLASS_TABLE     = "content.class.table";
    public  static final String SECTION_CONTENT_CLASS_CELL      = "content.class.cell";
    public  static final String SECTION_CONTENT_CLASS_MESSAGE   = "content.class.message";
    public  static final String SECTION_CONTENT_ID_MESSAGE      = "content.id.message";
    public  static final String SECTION_CONTENT_MENUBAR         = "content.menubar";
    public  static final String SECTION_CONTENT_BODY            = "content.body";
    public  static final String SECTION_CONTENT_MESSAGE         = "content.message";
    
    public  static final String SECTION_CONTENT_HTML            = "content.html";

    // ------------------------------------------------------------------------
    // CSS class definitions

    public static final String CSS_TEXT_INPUT                   = "textInput";
    public static final String CSS_TEXT_READONLY                = "textReadOnly";
    public static final String CSS_TEXT_ONCLICK                 = "textOnClick";
    public static final String CSS_TEXTAREA_INPUT               = "textAreaInput";
    public static final String CSS_TEXTAREA_READONLY            = "textAreaReadOnly";

    public static final String CSS_MENU_TITLE                   = "menuTitle";
    public static final String CSS_MENU_INSTRUCTIONS            = "menuInstructions";
    public static final String CSS_MENU_DESCRIPTION             = "menuDescription";

    public static final String CSS_CONTENT_FRAME[]              = new String[] { "contentTable"         , "contentCell" };
    public static final String CSS_CONTENT_MENU[]               = new String[] { "contentTable"         , "contentTopMenuCell" };
    public static final String CSS_CONTENT_MAP[]                = new String[] { "contentMapTable"      , "contentTrackMapCell" };
    public static final String CSS_CONTENT_MAP_FULL[]           = new String[] { "contentMapTableFull"  , "contentTrackMapCellFull" };
    public static final String CSS_MENUBAR_OK[]                 = new String[] { "contentTable" , "contentMapTable", "contentMapTableFull" };

    public static final String CSS_MESSAGE[]                    = new String[] { "messageResponseTable" , "messageResponseCell" };

    public static final String CSS_CONTENT_MESSAGE              = "contentMessage";
    
    public static final String CSS_ADMIN_SELECT_TITLE           = "adminSelectTitle";
    public static final String CSS_ADMIN_SELECT_TABLE           = "adminSelectTable_sortable";   // ReportPresentation.SORTTABLE_CSS_CLASS;
    
    public static final String CSS_ADMIN_TABLE_HEADER_ROW       = "adminTableHeaderRow";
    public static final String CSS_ADMIN_TABLE_HEADER_COL       = "adminTableHeaderCol_sort";
    public static final String CSS_ADMIN_TABLE_HEADER_COL_NS    = "adminTableHeaderCol_nosort";  // ReportPresentation.SORTTABLE_CSS_NOSORT;
    public static final String CSS_ADMIN_TABLE_HEADER_COL_SEL   = CSS_ADMIN_TABLE_HEADER_COL_NS;

    public static final String CSS_ADMIN_TABLE_BODY_ROW_ODD     = "adminTableBodyRowOdd";
    public static final String CSS_ADMIN_TABLE_BODY_ROW_EVEN    = "adminTableBodyRowEven";
    public static final String CSS_ADMIN_TABLE_BODY_COL         = "adminTableBodyCol";
    public static final String CSS_ADMIN_TABLE_BODY_COL_SEL     = CSS_ADMIN_TABLE_BODY_COL;

    public static final String CSS_ADMIN_VIEW_TABLE             = "adminViewTable";
    public static final String CSS_ADMIN_VIEW_TABLE_HEADER      = "adminViewTableHeader";
    public static final String CSS_ADMIN_VIEW_TABLE_DATA        = "adminViewTableData";
    public static final String CSS_ADMIN_VIEW_TABLE_TEXTAREA    = "adminViewTableTextArea";
    public static final String CSS_ADMIN_COMBO_BOX              = "adminComboBox";

    // ------------------------------------------------------------------------
    // ID definitions
    
    public static final String ID_CONTENT_MESSAGE               = "contentMessage";

    // ------------------------------------------------------------------------
    // helper to write out html lines

    public static void println(PrintWriter out, String html)
        throws IOException
    {
        if (html != null) {
            out.println(html);
        }
    }

    public static void writeHTML(PrintWriter out, String html[])
        throws IOException
    {
        if (html != null) {
            for (int i = 0; i < html.length; i++) {
                CommonServlet.println(out, html[i]);
            }
        }  
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the content type for the specified 'response'.  This method appends the
    *** default character set to the mim content type before calling 'response.setContentType(...)'
    *** @param response  The HttpServletResponse instance
    *** @param mimeType  The mime type
    **/
    public static void setResponseContentType(HttpServletResponse response, String mimeType)
    {
        CommonServlet.setResponseContentType(response, mimeType, null);
    }

    /**
    *** Sets the content type for the specified 'response'.  This method appends the
    *** default character set to the mim content type before calling 'response.setContentType(...)'
    *** @param response  The HttpServletResponse instance
    *** @param mimeType  The mime type
    *** @param charSet   The character encoding
    **/
    public static void setResponseContentType(HttpServletResponse response, String mimeType, String charSet)
    {
        if (response != null) {
            if (StringTools.isBlank(charSet)) {
                charSet = StringTools.getCharacterEncoding();
            }
            String contentType = HTMLTools.getMimeTypeCharset(mimeType,charSet);
            //Print.logInfo("Setting 'content-type': %s", contentType);
            response.setContentType(contentType);
            response.setCharacterEncoding(charSet);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public CommonServlet() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    // Page Layout
    public static void writePageFrame(
        RequestProperties reqState,
        HTMLOutput html)
        throws IOException
    { 
        HttpServletResponse response = reqState.getHttpServletResponse();

        /* dispatch to JSP */
        String jspURI = reqState.getJspURI();
        if (!StringTools.isBlank(jspURI)) {
            ServletRequest request = reqState.getHttpServletRequest();
            request.setAttribute(SECTION_REQUESTPROPS   , reqState);
            request.setAttribute(SECTION_BODY_ONLOAD    , null);
            request.setAttribute(SECTION_BODY_ONUNLOAD  , null);
            request.setAttribute(SECTION_STYLESHEET     , HTMLOutput.NOOP);
            request.setAttribute(SECTION_JAVASCRIPT     , HTMLOutput.NOOP);
            request.setAttribute(SECTION_NAVIGATION     , HTMLOutput.NOOP);
            request.setAttribute(SECTION_CONTENT_BODY   , HTMLOutput.NOOP);
            request.setAttribute(SECTION_CONTENT_HTML   , html);
            //Print.logInfo("Dispatching to JSP: " + jspURI);
            RequestDispatcher rd = request.getRequestDispatcher(jspURI);
            if (rd != null) {
                try {
                    rd.forward(request, response);
                    return;
                } catch (ServletException se) {
                    Print.logException("JSP error: " + jspURI, se);
                    // continue below
                }
            } else {
                Print.logError("******* RequestDispatcher not found for URI: " + jspURI);
            }
        }
 
        /* default if not handled by the above */
        // OBSOLETE: will be removed in the next release
        Print.logError("This page rendering section is OBSOLETE (should not be used)");
        CommonServlet._writePageFrame(
            reqState,
            null, // bodyOnLoad,
            null, // bodyOnUnload,
            null, // styleSheet,
            null, // javascript,
            null, // navigation,
            null, // contents,
            html);


    }
    
    // ------------------------------------------------------------------------

    // Page Layout
    public static void writePageFrame(
        RequestProperties reqState,
        String bodyOnLoad, String bodyOnUnload,
        HTMLOutput styleSheet,
        HTMLOutput javascript,
        HTMLOutput navigation,
        HTMLOutput contents)
        throws IOException
    { 
        HttpServletResponse response = reqState.getHttpServletResponse();

        /* dispatch to JSP */
        String jspURI = reqState.getJspURI();
        if (!StringTools.isBlank(jspURI)) {
            ServletRequest request = reqState.getHttpServletRequest();
            request.setAttribute(SECTION_REQUESTPROPS   , reqState);
            request.setAttribute(SECTION_BODY_ONLOAD    , StringTools.isBlank(bodyOnLoad  )? null : bodyOnLoad  );
            request.setAttribute(SECTION_BODY_ONUNLOAD  , StringTools.isBlank(bodyOnUnload)? null : bodyOnUnload);
            request.setAttribute(SECTION_STYLESHEET     , styleSheet);
            request.setAttribute(SECTION_JAVASCRIPT     , javascript);
            request.setAttribute(SECTION_NAVIGATION     , navigation);
            request.setAttribute(SECTION_CONTENT_BODY   , contents);
            //Print.logInfo("Dispatching to JSP: " + jspURI);
            RequestDispatcher rd = request.getRequestDispatcher(jspURI);
            if (rd != null) {
                try {
                    rd.forward(request, response);
                    return;
                } catch (ServletException se) {
                    Print.logException("JSP error: " + jspURI, se);
                    // continue below
                }
            } else {
                Print.logError("******* RequestDispatcher not found for URI: " + jspURI);
            }
        }
 
        /* default if not handled by the above */
        // OBSOLETE: will be removed in the next release
        Print.logWarn("This page rendering section is OBSOLETE (should not be used)");
        CommonServlet._writePageFrame(
            reqState,
            bodyOnLoad,
            bodyOnUnload,
            styleSheet,
            javascript,
            navigation,
            contents,
            null/*html*/);

    }

    // ------------------------------------------------------------------------
    // Page Layout (OBSOLETE! Will be removed in the next release.)
    // Layout:
    //  +----------------------------------+
    //  |             header               |
    //  +----------------------------------+
    //  |           navigation             |
    //  +---+--------------------------+---+
    //  | L |                          | R |
    //  |   |        contents          |   |
    //  |   |                          |   |
    //  |   |                          |   |
    //  +---+--------------------------+---+
    //  |             footer               |
    //  +----------------------------------+
    //

    private static void _writePageFrame(
        RequestProperties   reqState,
        String              bodyOnLoad,
        String              bodyOnUnload,
        HTMLOutput          styleSheet,
        HTMLOutput          javascript,
        HTMLOutput          navigation,
        HTMLOutput          contents,
        HTMLOutput          html)
        throws IOException
    {
        Print.logWarn("PageDecorations are now obsolete and may be removed from the next release!");
        HttpServletRequest  request   = reqState.getHttpServletRequest();
        HttpServletResponse response  = reqState.getHttpServletResponse();
        PrivateLabel        privLabel = reqState.getPrivateLabel();
        PageDecorations     pageDecor = reqState.isLoggedIn()? privLabel.getUserPageDecorations() : privLabel.getDefaultPageDecorations();
        String              pageName  = reqState.getPageName();

        /* write */
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
        PrintWriter out = response.getWriter();

        /* html specified? */
        if (html != null) {
            html.write(out); 
            out.close();
            return;
        }

        // HTML start
        out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
        out.write("<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>\n");
        
        /* indicate this is an obsolete format */
        String jspName = reqState.getJspName();
        out.write("<!-- Default PageDecorations (Obsolete!) -->\n");
        out.write("<!-- JSPEntries tag JSP ("+jspName+") does not specify a file value! -->\n");

        // HTML head
        out.write("\n");
        out.write("<head>\n");
        out.write("  <meta http-equiv='content-type' content='text/html; charset=UTF-8'/>\n");
        out.write("  <meta http-equiv='cache-control' content='no-cache'/>\n");
      //out.write("  <meta http-equiv='pragma' content='no-cache'/>\n");
        out.write("  <meta http-equiv='expires' content='0'/>\n"); // expires 'now'
        out.write("  <meta name='copyright' content='"+privLabel.getCopyright()+"'/>\n");
        out.write("  <meta name='robots' content='none'/>\n");
        out.write("  <title>" + privLabel.getPageTitle() + "</title>\n");

        // Page style
        out.write("\n");
        out.write("<!-- Begin Page Style -->\n");
        pageDecor.writeStyle(out, reqState);
        if (styleSheet != null) { 
            styleSheet.write(out); 
        }
        out.write("<!-- End Page Style -->\n");
        out.write("\n");

        // JavaScript
        out.write("<!-- Begin Page JavaScript -->\n");
        JavaScriptTools.writeUtilsJS(out, request);
        if (javascript != null) { 
            javascript.write(out); 
        }
        out.write("<!-- End Page JavaScript -->\n");
        out.write("\n");

        out.write("</head>\n");
        out.write("\n");

        // HTML Body start
        out.write("<body ");
        if (!StringTools.isBlank(bodyOnLoad)) {
            out.write("onload='" + bodyOnLoad + "' "); 
        }
        if (!StringTools.isBlank(bodyOnUnload)) {
            out.write("onunload='" + bodyOnUnload + "' "); 
        }
        out.write(" leftmargin='0' rightmargin='0' topmargin='0' bottommargin='0'>\n");
        
        // Framing table start
        out.write("<table cellspacing='0' cellpadding='0' border='0' width='100%' height='100%'>\n");
        out.write("<tbody>\n");

        // Top row
        if (reqState.writePageFrameSection(RequestProperties.PAGE_FRAME_HEADER)) {
            out.write("\n");
            out.write("<!-- Begin Page header -->\n");
            out.write("<tr>\n");
            out.write("<td colspan='3'>\n");
            //***** header here *******
            pageDecor.writeHeader(out, reqState);
            //*************************
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<!-- End Page header -->\n");
            out.write("\n");
        }

        // Navigation row
        if (reqState.writePageFrameSection(RequestProperties.PAGE_FRAME_NAVIGATION)) {
            out.write("\n");
            out.write("<!-- Begin Page navigation -->\n");
            out.write("<tr>\n");
            out.write("<td colspan='3'>\n");
            //***** navigation here *******
            if (navigation != null) { 
                navigation.write(out); 
            } else {
                pageDecor.writeNavigation(out, reqState);
            }        
            //*************************
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<!-- End Page navigation -->\n");
            out.write("\n");
        }

        // Middle row
        out.write("<tr>\n");
        
        // left wing
        if (reqState.writePageFrameSection(RequestProperties.PAGE_FRAME_LEFT)) {
            out.write("\n");
            out.write("<!-- Begin Page left -->\n");
            out.write("<td valign='top' align='left'>\n");
            //***** left panel here ***
            pageDecor.writeLeft(out, reqState);
            //*************************
            out.write("</td>\n");
            out.write("<!-- End Page left -->\n");
            out.write("\n");
        }

        // content (should always be enabled)
        if (reqState.writePageFrameSection(RequestProperties.PAGE_FRAME_CONTENT)) {
            out.write("\n");
            out.write("<!-- Begin Page Contents -->\n");
            out.write("<td valign='top' align='center' height='100%'>\n");
            //***** contents here ************************
            if (contents != null) {
                String tableClass = contents.getTableClass();
                if (!StringTools.isBlank(tableClass)) {
                    out.write("<table class='"+tableClass+"' cellspacing='0' cellpadding='0' border='0'>\n");
                    if (CommonServlet.CSS_CONTENT_FRAME[0].equals(tableClass)) {
                        out.write("<tr>\n");
                        MenuBar.writeTableRow(out, pageName, reqState);
                        out.write("</tr>\n");
                    }
                    out.write("<tr>\n");
                    out.write("<td class=" + contents.getCellClass() + " align='center' valign='top'>");
                    contents.write(out); 
                    out.write("</td>");
                    out.write("</tr>\n");
                    String frameMessage = contents.getTableMessage();
                    if (!StringTools.isBlank(frameMessage)) {
                        out.write("<tr><td id='"+CommonServlet.ID_CONTENT_MESSAGE+"' class='"+CommonServlet.CSS_CONTENT_MESSAGE+"'>"+StringTools.trim(frameMessage)+"</td></tr>\n");
                    }
                    out.write("</table>\n");
                } else {
                    contents.write(out); 
                }
            }
            //********************************************
            out.write("</td>\n");
            out.write("<!-- End Page contents -->\n");
            out.write("\n");
        }

        // right wing
        if (reqState.writePageFrameSection(RequestProperties.PAGE_FRAME_RIGHT)) {
            out.write("\n");
            out.write("<!-- Begin Page right -->\n");
            out.write("<td valign='top' align='right'>\n");
            //***** right panel here ***
            pageDecor.writeRight(out, reqState);
            //**************************
            out.write("</td>\n");
            out.write("<!-- End Page right -->\n");
            out.write("\n");
        }
        
        // end of middle row
        out.write("</tr>\n");

        // Bottom row
        if (reqState.writePageFrameSection(RequestProperties.PAGE_FRAME_FOOTER)) {
            out.write("\n");
            out.write("<!-- Begin Page footer -->\n");
            out.write("<tr>\n");
            out.write("<td colspan='3'>\n");
            //***** footer here *******
            pageDecor.writeFooter(out, reqState);
            //*************************
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<!-- End Page footer -->\n");
            out.write("\n");
        }

        // Framing table end
        out.write("</tbody>\n");
        out.write("</table>\n");

        // Body end
        out.write("</body>\n");
        // HTML end
        out.write("</html>\n");
        out.close();

    }

    // ------------------------------------------------------------------------

}
