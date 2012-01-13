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
//  2007/06/03  Martin D. Flynn
//     -Initial release
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Menu bar descriptions changed to use 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -Added support for pull-down tab menus
//  2008/02/21  Martin D. Flynn
//     -Moved menubar javascript tools to 'MenuBar.js'
//  2008/09/12  Martin D. Flynn
//     -Move "menuBar.usePullDownMenus" property definition to PrivateLabel.java
//  2008/09/19  Martin D. Flynn
//     -Tabs with no sub-options will no longer be displayed.
//  2011/03/08  Martin D. Flynn
//     -Fixed dynamic HTML/JavaScript issue when writing menu options that call
//      "openFixedWindow" and "menuBar.includeTextAnchor" is true.
// ----------------------------------------------------------------------------
// Helpful references:
//   http://www.scriptforest.com/javascript_cascading_menu.html
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.JspWriter;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.track.Track;

public class MenuBar
    implements org.opengts.war.track.Constants
{

    // ------------------------------------------------------------------------

    public static final boolean DFT_USE_PULL_DOWN_MENUS     = true;

    public static final int     MENU_TIMEOUT_MS             = 3700;

    // ------------------------------------------------------------------------

    public static final String  CSS_MENU_TAB_UNSEL_WIDE     = "menuBarUnsW";    // TD
    public static final String  CSS_MENU_TAB_SEL_WIDE       = "menuBarSelW";
    public static final String  CSS_MENU_TAB_UNSEL          = "menuBarUns";
    public static final String  CSS_MENU_TAB_SEL            = "menuBarSel";
    public static final String  CSS_MENU_BAR                = "menuBar";        // TD
    public static final String  CSS_MENU_TABLE              = "menuSubFrame";
    public static final String  CSS_MENU_TABLE_ROW          = "menuSubItemRow";
    public static final String  CSS_MENU_CELL               = "menuSubItemCol";
    public static final String  CSS_MENU_LINK               = "menuSubItemLink";

    // ------------------------------------------------------------------------

    public static final String MENU_MAIN                    = "menu.main";
        // Main Menu

    public static final String MENU_ADMIN                   = "menu.admin";
        // Account[X]
        // User[X]
        // Device[X]/Configuration
        // DeviceGroups
        // Entity/Trailer
        // GeoZones

    public static final String MENU_TRACK_DEVICE            = "menu.track.device";
        // Device Map[X]

    public static final String MENU_TRACK_FLEET             = "menu.track.fleet";
        // DeviceGroup Map

    public static final String MENU_REPORTS                 = "menu.rpts";
    public static final String MENU_REPORTS_DEVDETAIL       = "menu.rpts.devDetail";
        // Device Detail[X]
        //      Event Detail[X]
        //      Temperature Monitoring[X]
        //      J1708 Fault codes[X]
    public static final String MENU_REPORTS_GRPDETAIL       = "menu.rpts.grpDetail";
        // Group Detail[X]
        //      Trip Report[X]
        //      Geozone Report[X]
    public static final String MENU_REPORTS_GRPSUMMARY      = "menu.rpts.grpSummary";
        // Device Summary[X]
        //      Last known Device location[X]
        //      Last known Entity location[X]
    public static final String MENU_REPORTS_PERFORM         = "menu.rpts.performance";
        // Driver Performance
        //      Excess Speed[X]
        //      Hard Braking
        //      Driving/Stop Time Report[X]
    public static final String MENU_REPORTS_IFTA            = "menu.rpts.ifta";
        // IFTA
        //      Stateline Crossing Detail[X]
        //      State Mileage Summary[X]
        //      Fueling Detail
        //      Fueling Summary
    public static final String MENU_REPORTS_SYSADMIN        = "menu.rpts.sysadmin";
        // SysAdmin
        //      Unassigned Devices

    // ------------------------------------------------------------------------

    public static void writeJavaScript(JspWriter out, String pageName, RequestProperties reqState)
        throws IOException
    {
        MenuBar.writeJavaScript(new PrintWriter(out, out.isAutoFlush()), pageName, reqState);
    }

    public static void writeJavaScript(PrintWriter out, String pageName, RequestProperties reqState)
        throws IOException
    {
        PrivateLabel       privLabel = reqState.getPrivateLabel();
        HttpServletRequest request   = reqState.getHttpServletRequest();

        /* need JavaScript? */
        if (!privLabel.getBooleanProperty(PrivateLabel.PROP_MenuBar_usePullDownMenus,DFT_USE_PULL_DOWN_MENUS)) {
            // don't bother with JavaScript
            return;
        }

        /* MenuBar.js */
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("MenuBar.js"), request);
        
        /* include anchor link */
        boolean inclAnchor = privLabel.getBooleanProperty(PrivateLabel.PROP_MenuBar_includeTextAnchor,false);

        /* specific menu items */
        int itemHeight = 13;
        JavaScriptTools.writeStartJavaScript(out);
        out.println("function mnubarCreateSubMenu(mainObj) {");
        out.println("  var itemHeight = "+itemHeight+";");
        out.println("  var id = mainObj.id;");
        out.println("  var absLoc = getElementPosition(mainObj);");
        out.println("  var absSiz = getElementSize(mainObj);");
        out.println("  for (;;) {");
        Map<String,MenuGroup> menuMap = privLabel.getMenuGroupMap();
        for (String mgn : menuMap.keySet()) {
            MenuGroup mg = menuMap.get(mgn);
            if (mg.showInMenuBar()) {
                java.util.List<WebPage> pageList = mg.getWebPageList(reqState);
                if (!ListTools.isEmpty(pageList)) {
                    out.println("    if (id == '"+mgn+"') {");
                    out.println("      mbSubMenuObj = mnubarCreateMenuFrame(absLoc,absSiz,(("+pageList.size()+"*itemHeight)+6));");
                    out.println("      mbSubMenuObj.innerHTML = ");
                    out.println("        \"<table class='"+CSS_MENU_TABLE+"' cellspacing='0' cellpadding='0'>\" +");
                    for (WebPage wp : pageList) {
                        String url  = wp.encodePageURL(reqState);
                        String desc = wp.getNavigationTab(reqState);
                        String help = wp.getMenuHelp(reqState, null);
                        out.write("        ");
                        out.write("\"<tr class='"+CSS_MENU_TABLE_ROW+"'>");
                        String target  = StringTools.blankDefault(wp.getTarget(),"_self"); // (wp instanceof WebPageURL)? ((WebPageURL)wp).getTarget() : "_self";
                        String onclick = "openURL('"+url+"','"+target+"')";
                        if (!target.startsWith("_")) {
                            PixelDimension pixDim = wp.getWindowDimension();
                            if (pixDim != null) {
                                int W = pixDim.getWidth();
                                int H = pixDim.getHeight();
                                onclick = "openFixedWindow('"+url+"','"+target+"',"+W+","+H+")";
                            }
                        }
                        out.write("<td class='"+CSS_MENU_CELL+"' height='\"+itemHeight+\"' onclick=\\\"javascript:"+onclick+";\\\" title=\\\""+help+"\\\">");
                        if (inclAnchor) { 
                            // "menuBar.includeTextAnchor"
                            out.write("<a class='"+CSS_MENU_LINK+"'");
                            if (target.startsWith("_")) {
                                out.write(" href='"+url+"' target='"+target+"'");
                            } else {
                                PixelDimension pixDim = wp.getWindowDimension();
                                if (pixDim == null) {
                                    out.write(" href='"+url+"' target='"+target+"'");
                                }
                            }
                            out.write(">");
                            out.write(desc);
                            out.write("</a>");
                        } else {
                            out.write(desc);
                        }
                        out.write("</td>");
                        out.write("</tr>\" +\n");
                    }
                    out.println("        \"</table>\";");
                    out.println("      break;");
                    out.println("    }");
                }
            }
        }
        out.println("    break; // error");
        out.println("  }");	
        out.println("  if (mbSubMenuObj) { document.body.appendChild(mbSubMenuObj); }");	
        out.println("  return mbSubMenuObj;");	
        out.println("}");	
        JavaScriptTools.writeEndJavaScript(out);

    }
    
    // ------------------------------------------------------------------------

    public static void writeTableRow(JspWriter out, String pageName, RequestProperties reqState)
        throws IOException
    {
        MenuBar.writeTableRow(new PrintWriter(out, out.isAutoFlush()), pageName, reqState);
    }

    /* write out the menu bar */
    public static void writeTableRow(PrintWriter out, String pageName, RequestProperties reqState)
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();

        /* start menu bar row */
        out.write("\n");
        out.write("<!-- Begin Menu Bar -->\n");
        out.write("<td nowrap class='"+CSS_MENU_BAR+"'>\n");
        out.write("<table border='0' cellpadding='0' cellspacing='0'>");
        out.write("<tr style='margin-bottom:0px; padding-bottom:0px'>\n");

        if (privLabel.getBooleanProperty(PrivateLabel.PROP_MenuBar_usePullDownMenus,DFT_USE_PULL_DOWN_MENUS)) {
            
            Map<String,MenuGroup> menuMap = privLabel.getMenuGroupMap();
            boolean openOnMouseOver = privLabel.getBooleanProperty(PrivateLabel.PROP_MenuBar_openOnMouseOver,false);
            for (String mgn : menuMap.keySet()) {
                MenuGroup mg = menuMap.get(mgn);
                if (mg.showInMenuBar() && !ListTools.isEmpty(mg.getWebPageList(reqState))) {
                    String desc = mg.getTitle(reqState.getLocale());
                    out.write(" <td id='"+mgn+"' class='"+CSS_MENU_TAB_UNSEL_WIDE+"'");
                    out.write(" onmouseover=\"mnubarMouseOverTab('"+mgn+"',"+openOnMouseOver+")\"");
                    out.write(" onmouseout=\"mnubarMouseOutTab('"+mgn+"')\"");
                    out.write(" onclick=\"mnubarToggleMenu('"+mgn+"')\">");
                    out.write(desc);
                    out.write("</td>\n");
                }
            }
        
        } else {

            /* explicitly add main menu */
            Map pageMap = privLabel.getWebPageMap();
            WebPage mainMenu = (WebPage)pageMap.get(Track.PAGE_MENU_TOP);
            if (mainMenu != null) {
                String desc = mainMenu.getNavigationTab(reqState);
                if ((pageName == null) || !pageName.equals(Track.PAGE_MENU_TOP)) {
                    //String url = WebPageAdaptor.EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),Track.PAGE_MENU_TOP);
                    String url = mainMenu.encodePageURL(reqState);
                    out.write(" <td class='"+CSS_MENU_TAB_UNSEL+"' onclick=\"javascript:window.open('"+url+"','_top')\">"+desc+"</td>\n");
                } else {
                    out.write(" <td class='"+CSS_MENU_TAB_SEL+"'>"+desc+"</td>\n");
                }
            }
        
            /* add all other menu items (except logout) */
            Map<String,MenuGroup> menuMap = privLabel.getMenuGroupMap();
            for (String mgn : menuMap.keySet()) {
                MenuGroup mg = menuMap.get(mgn);
                if (mg.showInMenuBar()) {
                    java.util.List<WebPage> menuItems = mg.getWebPageList(reqState);
                    for (WebPage wp : menuItems) {
                        String wpname = wp.getPageName();

                        // skip these pages if they show up
                        if (wpname.equals(Track.PAGE_LOGIN)   ) { continue; }
                        if (wpname.equals(Track.PAGE_MENU_TOP)) { continue; }
                        if (wpname.equals(Track.PAGE_PASSWD)  ) { continue; }

                        // add menu bar tab
                        String desc = wp.getNavigationTab(reqState);
                        if ((pageName == null) || !pageName.equals(wpname)) {
                            String url = wp.encodePageURL(reqState);
                            out.write(" <td class='"+CSS_MENU_TAB_UNSEL+"' onclick=\"javascript:window.open('"+url+"','_top')\">"+desc+"</td>\n");
                        } else {
                            out.write(" <td class='"+CSS_MENU_TAB_SEL+"'>"+desc+"</td>\n");
                        }

                    }
                }
            }
            
        }
        
        out.write("</tr>\n");
        out.write("</table>\n");
        out.write("</td>\n");
        out.write("<!-- End Menu Bar -->\n");
    }

    // ------------------------------------------------------------------------

}
