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
//  2008/10/16  Martin D. Flynn
//     -Initial release
//  2009/10/02  Martin D. Flynn
//     -Added table sorting/searching feature.
//  2009/11/01  Martin D. Flynn
//     -Escape quotes when creating device/description list
//  2009/11/10  Martin D. Flynn
//     -Overhauled display mechanism to attempt to improve performance on IE.
//  2011/08/21  Martin D. Flynn
//     - Fix table 'div'
// ----------------------------------------------------------------------------
package org.opengts.war.track;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;

public class DeviceChooser
{

    // ------------------------------------------------------------------------

    public static boolean showSingleItemTextField(PrivateLabel privLabel)
    {
        if (privLabel == null) {
            return false;
        } else {
            return privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceChooser_singleItemTextField,false);
        }
    }
    
    // ------------------------------------------------------------------------

    public static IDDescription.SortBy getSortBy(PrivateLabel privLabel)
    {
        IDDescription.SortBy dft = IDDescription.SortBy.ID;
        if (privLabel == null) {
            return dft;
        } else
        if (privLabel.hasProperty(PrivateLabel.PROP_DeviceChooser_sortBy)) {
            String sortBy = privLabel.getStringProperty(PrivateLabel.PROP_DeviceChooser_sortBy,null);
            return IDDescription.GetSortBy(sortBy);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public static boolean isDeviceChooserUseTable(PrivateLabel privLabel)
    {
        boolean dft = false;
        if (privLabel == null) {
            return dft;
        } else {
            return privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceChooser_useTable,dft);
        }
    }

    // ------------------------------------------------------------------------

    public static boolean isSearchEnabled(PrivateLabel privLabel)
    {
        boolean dft = false;
        if (privLabel == null) {
            return dft;
        } else {
            return privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceChooser_search,dft);
        }
    }

    // ------------------------------------------------------------------------

    public static boolean matchUsingContains(PrivateLabel privLabel)
    {
        boolean dft = true;
        if (privLabel == null) {
            return dft;
        } else
        if (privLabel.hasProperty(PrivateLabel.PROP_DeviceChooser_matchContains)) {
            String mc = privLabel.getStringProperty(PrivateLabel.PROP_DeviceChooser_matchContains,null);
            if (StringTools.isBlank(mc)) {
                return dft;
            } else
            if (mc.equalsIgnoreCase("contains")) {
                return true;
            } else
            if (mc.equalsIgnoreCase("startsWith")) {
                return false;
            } else {
                return StringTools.parseBoolean(mc,dft);
            }
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public static int getIDPosition(PrivateLabel privLabel)
    {
        // 0=none, 1=first, 2=last
        int dft = 1;
        if (privLabel == null) {
            return dft;
        } else
        if (privLabel.hasProperty(PrivateLabel.PROP_DeviceChooser_idPosition)) {
            String idPos = privLabel.getStringProperty(PrivateLabel.PROP_DeviceChooser_idPosition,null);
            if (StringTools.isBlank(idPos)) {
                return dft;
            } else
            if (idPos.equalsIgnoreCase("first")) {
                return 1;
            } else
            if (idPos.equalsIgnoreCase("last")) {
                return 2;
            } else
            if (idPos.equalsIgnoreCase("none")) {
                return 0;
            } else {
                return dft;
            }
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // write Style
    
    public static void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        WebPageAdaptor.writeCssLink(out, reqState, "DeviceChooser.css", null);
    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeJavaScript(PrintWriter out, Locale locale, RequestProperties reqState, String deviceListURL)
        throws IOException
    {
        I18N               i18n      = I18N.getI18N(DeviceChooser.class, locale);
        HttpServletRequest request   = reqState.getHttpServletRequest();
        PrivateLabel       privLabel = reqState.getPrivateLabel();

        /* start JavaScript */
        JavaScriptTools.writeStartJavaScript(out);

        /* vars */
        out.write("// DeviceChooser vars\n");
        JavaScriptTools.writeJSVar(out, "DEVICE_LIST_URL"           , deviceListURL);
        JavaScriptTools.writeJSVar(out, "DeviceChooserIDPosition"   , DeviceChooser.getIDPosition(privLabel)); // 0=false, 1=first, 2=last
        JavaScriptTools.writeJSVar(out, "DeviceChooserEnableSearch" , DeviceChooser.isSearchEnabled(privLabel));
        JavaScriptTools.writeJSVar(out, "DeviceChooserMatchContains", DeviceChooser.matchUsingContains(privLabel));

        /* Localized text */
        out.write("// DeviceChooser localized text\n");
        JavaScriptTools.writeJSVar(out, "DEVICE_TEXT_ID"            , i18n.getString("DeviceChooser.ID","ID"));
        JavaScriptTools.writeJSVar(out, "DEVICE_TEXT_Description"   , i18n.getString("DeviceChooser.description","Description"));
        JavaScriptTools.writeJSVar(out, "DEVICE_TEXT_Search"        , i18n.getString("DeviceChooser.search","Search"));

        /* end JavaScript */
        JavaScriptTools.writeEndJavaScript(out);

        /* DeviceChooser.js */
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("DeviceChooser.js"), request);

    }

    public static void writeDeviceList(PrintWriter out, RequestProperties reqState, String varName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        boolean      isFleet   = reqState.isFleet();
        Locale       locale    = reqState.getLocale();

        /* begin list var */
        out.write("// "+(isFleet?"Group":"Device")+" list\n");
        out.write("var "+varName+" = new Array(\n");

        /* Device/Group list */
        IDDescription.SortBy sortBy = DeviceChooser.getSortBy(privLabel);
        java.util.List<IDDescription> idList = reqState.createIDDescriptionList(isFleet, sortBy);
        if (!ListTools.isEmpty(idList)) {
            // The 'deviceID'/'groupID' is unique, but the description may not be.
            for (Iterator<IDDescription> i = idList.iterator(); i.hasNext();) { 
                IDDescription dd = i.next();
                String dgn  = _escapeText(dd.getID());            // won't contain quotes anyway
                String desc = _escapeText(dd.getDescription());   //
                String name = _escapeText(dd.getName());          //
                out.write("   { id:\""+dgn+"\", desc:\""+desc+"\", name:\""+name+"\" }");
                if (i.hasNext()) { out.write(","); }
                out.write("\n");
            }
        }
        
        /* debug extra entries? */
        int extraCount = (int)privLabel.getLongProperty(PrivateLabel.PROP_DeviceChooser_extraDebugEntries,0L);
        if (extraCount > 0) {
            if (extraCount > 5000) { extraCount = 5000; } // arbitrary limit
            if (!ListTools.isEmpty(idList)) { out.write(",\n"); }
            int ofs = 1;
            for (int i = ofs; i < ofs + extraCount; i++) {
                String dgn  = (isFleet? "group_" : "device_") + i;
                String desc = i + (isFleet? " Group" : " Device");
                out.write("   { id:\""+dgn+"\", desc:\""+desc+"\" }");
                if ((i + 1) < (ofs + extraCount)) { out.write(","); }
                out.write("\n");
            }
        }

        /* end list var */
        out.write(");\n");

    }

    // ------------------------------------------------------------------------
    
    private static String ID_CHOOSER_VIEW             = "devChooserView";
    private static String ID_SEARCH_FORM              = "devSearchForm";
    public  static String ID_SEARCH_TEXT              = "devSearchText";
    private static String ID_DIV_TABLE                = "DeviceTableList";
    private static String ID_DEVSELECT_TABLE          = "devSelectIDTable";
    private static String CLASS_TABLE_COLUMN_SORTABLE = "sortableX"; // requires 'sorttable.js'
    private static String CLASS_DEVSELECT_DIV_VISIBLE = "devSelectorDiv";
    private static String CLASS_DEVSELECT_DIV_HIDDEN  = "devSelectorDiv_hidden";
    private static String CLASS_DEVSELECT_DIV_TABLE   = "devSelectorTableList";
    private static String CLASS_DEVSELECT_ROW_HEADER  = "devSelectorRowHeader";
    private static String CLASS_DEVSELECT_COL_HEADER  = "devSelectorColHeader";
    private static String CLASS_DEVSELECT_ROW_DATA    = "devSelectorRowData";
    private static String CLASS_DEVSELECT_COL_DATA    = "devSelectorColData";
    private static String CLASS_DEVSELECT_ROW_HIDDEN  = "devSelectorRow_hidden";
    private static String CLASS_SEARCH_INPUT          = "deviceChooserInput";

    private static int    IDPOS_NONE                  = 0;
    private static int    IDPOS_FIRST                 = 1;
    private static int    IDPOS_LAST                  = 2;

    private static int    WIDTH_ID                    = 80;
    private static int    WIDTH_DESC                  = 180;

    private static int    SEARCH_TEXT_SIZE            = 18;

    private static StringBuffer append(StringBuffer sb, String s)
    {
        s = StringTools.replace(s,"\n","\\n");
        s = StringTools.replace(s,"\"","\\\"");
        sb.append("  \"" + s + "\" +\n");
        return sb;
    }

    private static String deviceGetTableHTML(RequestProperties reqState, IDDescription list[], int DeviceChooserIDPosition, String searchVal)
    {
        PrivateLabel privLabel          = reqState.getPrivateLabel();
        Locale      locale              = reqState.getLocale();
        int         idWidth             = WIDTH_ID;
        int         dsWidth             = WIDTH_DESC;

        /* localized text */
        I18N   i18n                     = I18N.getI18N(DeviceChooser.class, locale);
        String DEVICE_TEXT_ID           = i18n.getString("DeviceChooser.ID","ID");
        String DEVICE_TEXT_Description  = i18n.getString("DeviceChooser.description","Description");
        String DEVICE_TEXT_Search       = i18n.getString("DeviceChooser.search","Search");

        /* begin table HTML */
        StringBuffer html = new StringBuffer();
        append(html,"<table id='"+ID_DEVSELECT_TABLE+"' class='"+CLASS_TABLE_COLUMN_SORTABLE+"' cellspacing='0' cellpadding='0' border='1'>\n");
    
        // table header
        append(html,"<thead>\n");
        append(html,"<tr class='"+CLASS_DEVSELECT_ROW_HEADER+"'>");
        if (DeviceChooserIDPosition == IDPOS_NONE) {
            append(html,"<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>");
        } else 
        if (DeviceChooserIDPosition == IDPOS_LAST) {
            append(html,"<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>");
            append(html,"<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+DEVICE_TEXT_ID+"</th>");
        } else {
            append(html,"<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+DEVICE_TEXT_ID+"</th>");
            append(html,"<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>");
        }
        append(html,"</tr>\n");
        append(html,"</thead>\n");
    
        // table body
        int extraCount = (int)privLabel.getLongProperty(PrivateLabel.PROP_DeviceChooser_extraDebugEntries,0L);
        append(html,"<tbody>\n");
        for (int d = 0; d < list.length + extraCount; d++) {
            String idVal = (d < list.length)? list[d].getID()          : ("v" + String.valueOf(d - list.length + 1));
            String desc  = (d < list.length)? list[d].getDescription() : (String.valueOf(d - list.length + 1) + " asset");

            /* omit items not matched */
            //if (!StringTools.isBlank(searchVal) && !desc.toLowerCase().startsWith(searchVal.toLowerCase())) { 
            //    continue; 
            //}
            String dsTxt = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.TEXT);
            String dsVal = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.VALUE).toLowerCase();

            /* save matched item */
            int selNdx = d;

            /* write html */
            append(html,"<tr idVal='"+idVal+"' dsVal='"+dsVal+"' selNdx='"+selNdx+"' class='"+CLASS_DEVSELECT_ROW_DATA+"'>");
            if (DeviceChooserIDPosition == IDPOS_NONE) {
                append(html,"<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
            } else 
            if (DeviceChooserIDPosition == IDPOS_LAST) {
                append(html,"<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
                append(html,"<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ idVal +"</td>");
            } else {
                append(html,"<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ idVal +"</td>");
                append(html,"<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
            }
            append(html,"</tr>\n");
    
        }
        append(html,"</tbody>\n");
    
        append(html,"</table>\n");
        return html.toString();
    
    }

    public static void writeChooserDIV(PrintWriter out, RequestProperties reqState, IDDescription list[], String searchVal)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        int         idPos               = DeviceChooser.getIDPosition(privLabel);
        Locale      locale              = reqState.getLocale();
        int         idWidth             = WIDTH_ID;
        int         dsWidth             = WIDTH_DESC;

        /* localized text */
        I18N   i18n                     = I18N.getI18N(DeviceChooser.class, locale);
        String DEVICE_TEXT_ID           = i18n.getString("DeviceChooser.ID","ID");
        String DEVICE_TEXT_Description  = i18n.getString("DeviceChooser.description","Description");
        String DEVICE_TEXT_Search       = i18n.getString("DeviceChooser.search","Search");

        /* top DIV */
        out.write("\n");
        out.write("<!-- begin DeviceChooser DIV -->\n");
        out.write("<div id='"+ID_CHOOSER_VIEW+"' class='"+CLASS_DEVSELECT_DIV_HIDDEN+"'>\n");

        /* form */   
        if (DeviceChooser.isSearchEnabled(privLabel)) {
            out.write("<form id='"+ID_SEARCH_FORM+"' name='"+ID_SEARCH_FORM+"' method='GET' action=\"javascript:true;\" target='_self' style='padding-left:5px; background-color:#dddddd;'>\n"); // target='_top'
            out.write("<b>"+DEVICE_TEXT_Search+": </b>\n");
            out.write("<input id='"+ID_SEARCH_TEXT+"' name='"+ID_SEARCH_TEXT+"' class='"+CLASS_SEARCH_INPUT+"' type='text' value='' size='"+SEARCH_TEXT_SIZE+"' onkeypress=\"return searchKeyPressed(event);\" onkeyup=\"return deviceSearch();\"/>\n");
            out.write("</form>\n");
        }

        /* begin table */
        out.write("<div id='"+ID_DIV_TABLE+"' class='"+CLASS_DEVSELECT_DIV_TABLE+"'>\n"); // FIX
        out.write("<table id='"+ID_DEVSELECT_TABLE+"' class='"+CLASS_TABLE_COLUMN_SORTABLE+"' cellspacing='0' cellpadding='0' border='1'>\n");
    
        // table header
        out.write("<thead>\n");
        out.write("<tr class='"+CLASS_DEVSELECT_ROW_HEADER+"'>");
        if (idPos == IDPOS_NONE) {
            out.write("<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>");
        } else 
        if (idPos == IDPOS_LAST) {
            out.write("<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>");
            out.write("<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+DEVICE_TEXT_ID+"</th>");
        } else {
            out.write("<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+DEVICE_TEXT_ID+"</th>");
            out.write("<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>");
        }
        out.write("</tr>\n");
        out.write("</thead>\n");
    
        // table body
        int extraCount = (int)privLabel.getLongProperty(PrivateLabel.PROP_DeviceChooser_extraDebugEntries,0L);
        out.write("<tbody>\n");
        for (int d = 0; d < list.length + extraCount; d++) {
            String idVal = (d < list.length)? list[d].getID()          : ("v" + String.valueOf(d - list.length + 1));
            String desc  = (d < list.length)? list[d].getDescription() : (String.valueOf(d - list.length + 1) + " asset");

            /* omit items not matched */
            //if (!StringTools.isBlank(searchVal) && !desc.toLowerCase().startsWith(searchVal.toLowerCase())) { 
            //    continue; 
            //}
            String dsTxt = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.TEXT);
            String dsVal = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.VALUE).toLowerCase();

            /* save matched item */
            int selNdx = d;

            /* write html */
            out.write("<tr idVal='"+idVal+"' dsVal='"+dsVal+"' selNdx='"+selNdx+"' class='"+CLASS_DEVSELECT_ROW_DATA+"'>");
            if (idPos == IDPOS_NONE) {
                out.write("<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
            } else 
            if (idPos == IDPOS_LAST) {
                out.write("<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
                out.write("<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ idVal +"</td>");
            } else {
                out.write("<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ idVal +"</td>");
                out.write("<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
            }
            out.write("</tr>\n");
    
        }
        out.write("</tbody>\n");
    
        /* end table */
        out.write("</table>\n");
        out.write("</div>\n");
   
        /* end DIV */
        out.write("</div>\n");
        if (DeviceChooser.isSearchEnabled(privLabel)) {
            out.write("<script type=\"text/javascript\">\n");
            out.write("var devChooserSearchTextElem = document.getElementById('"+ID_SEARCH_TEXT+"');\n");
            out.write("</script>\n");
        }
        out.write("<!-- end DeviceChooser DIV -->\n");
        out.write("\n");

    }

    // ------------------------------------------------------------------------

    private static String _escapeText(String s)
    {
        s = StringTools.trim(s);
      //s = StringTools.htmlFilterValue(s);
        s = StringTools.replace(s, "\\", "\\\\");   // must be first
        s = StringTools.replace(s, "\"", "\\\"");   // double-quotes
        s = StringTools.replace(s, "'", "\\'");     // single-quotes
        return s;
    }

    // ------------------------------------------------------------------------

}
