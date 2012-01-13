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
// ----------------------------------------------------------------------------
package org.opengts.war.track;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.JspWriter;

import org.opengts.util.*;

import org.opengts.db.tables.*;
import org.opengts.war.tools.*;

public class ExpandMenu
{

    // ------------------------------------------------------------------------

    public static final int DESC_NONE       = 0;
    public static final int DESC_SHORT      = 1;
    public static final int DESC_LONG       = 2;
    
    // ------------------------------------------------------------------------
    // write Style

    public static void writeStyle(JspWriter out, RequestProperties reqState)
        throws IOException
    {
        ExpandMenu.writeStyle(new PrintWriter(out, out.isAutoFlush()), reqState);
    }

    public static void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        WebPageAdaptor.writeCssLink(out, reqState, "ExpandMenu.css", null);
    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeJavaScript(JspWriter out, RequestProperties reqState)
        throws IOException
    {
        ExpandMenu.writeJavaScript(new PrintWriter(out, out.isAutoFlush()), reqState);
    }

    public static void writeJavaScript(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("ExpandMenu.js"), request);
    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeMenu(JspWriter out, RequestProperties reqState,
        String menuID, boolean expandableMenu,
        boolean showIcon, int descriptionType, boolean showMenuHelp)
        throws IOException
    {
        ExpandMenu.writeMenu(new PrintWriter(out, out.isAutoFlush()), reqState,
            menuID, expandableMenu,
            showIcon, descriptionType, showMenuHelp);
    }

    public static void writeMenu(PrintWriter out, RequestProperties reqState,
        String menuID, boolean expandableMenu,
        boolean showIcon, int descriptionType, boolean showMenuHelp)
        throws IOException
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        Locale       locale    = reqState.getLocale();
        String  parentPageName = null;
        Account        account = reqState.getCurrentAccount();
        
        /* disable menu help if menu description is not displayed */
        boolean showInline = false;
        if (descriptionType == ExpandMenu.DESC_NONE) {
            showMenuHelp = false;
            showInline = true;
        }

        /* sub style classes */
        String topMenuID        = !StringTools.isBlank(menuID)?menuID:(expandableMenu?"expandMenu":"fixedMenu");
        String groupClass       = "menuGroup";
        String leafClass        = "itemLeaf";
        String leafDescClass    = "itemLeafDesc";
        String helpClass        = "itemLeafHelp";
        String helpPadClass     = "itemLeafHelpPad";
        String leafIconClass    = "itemLeafIcon";

        /* start menu */
        out.println("<ul id='"+topMenuID+"'>");
        
        /* iterate through menu groups */
        Map<String,MenuGroup> menuMap = privLabel.getMenuGroupMap();
        for (String mgn : menuMap.keySet()) {
            MenuGroup mg = menuMap.get(mgn);
            if (!mg.showInTopMenu()) {
                // skip this group
                //Print.logInfo("Skipping menu group: %s", mgn);
                continue;
            }
            
            boolean didDisplayGroup = false;
            for (WebPage wp : mg.getWebPageList(reqState)) {
                String menuName = wp.getPageName();
                String iconURI  = showIcon? wp.getMenuIconImage() : null; // may be blank/null
                String menuHelp = wp.getMenuHelp(reqState, parentPageName);
                String url      = wp.encodePageURL(reqState);//, RequestProperties.TRACK_BASE_URI());

                /* skip login page */
                if (menuName.equalsIgnoreCase(Constants.PAGE_LOGIN)) { 
                    // omit login
                    //Print.logInfo("Skipping login page: %s", menuName);
                    continue;
                }

                /* skip sysAdmin pages */
                if (wp.systemAdminOnly() && !Account.isSystemAdmin(account)) {
                    continue;
                }

                /* skip pages that are not ok to display */
                if (!wp.isOkToDisplay(reqState)) {
                    continue; 
                }

                /* menu description */
                String menuDesc = null;
                switch (descriptionType) {
                    case DESC_NONE:
                        menuDesc = null;
                        break;
                    case DESC_SHORT:
                        menuDesc = wp.getNavigationDescription(reqState);
                        break;
                    case DESC_LONG:
                    default:
                        menuDesc = wp.getMenuDescription(reqState, parentPageName);
                        break;
                }

                /* skip this menu item? */
                if (StringTools.isBlank(menuDesc) && StringTools.isBlank(iconURI)) {
                    //Print.logWarn("Menu name has no description: %s", menuName);
                    continue;
                }

                /* start menu group */
                if (!didDisplayGroup) {
                    // open Menu Group
                    didDisplayGroup = true;
                    out.write("<li class='"+groupClass+"'>" + mg.getTitle(locale) + "\n");
                    if (showInline) {
                        out.write("<br><table cellpadding='0' cellspacing='0' border='0'><tr>\n");
                    } else {
                        out.write("<ul>\n"); // <-- start menu sub group
                    }
                }

                /* menu anchor/link */
                String anchorStart = "<a";
                if (!StringTools.isBlank(menuHelp)) {
                    anchorStart += " title=\""+menuHelp+"\"";
                }
                String target = StringTools.blankDefault(wp.getTarget(),"_self"); // ((WebPageURL)wp).getTarget();
                if (target.startsWith("_")) {
                    anchorStart += " href=\""+url+"\"";
                    anchorStart += " target=\""+target+"\"";
                } else {
                    PixelDimension pixDim = wp.getWindowDimension();
                    if (pixDim != null) {
                        int W = pixDim.getWidth();
                        int H = pixDim.getHeight();
                        anchorStart += " onclick=\"javascript:openFixedWindow('"+url+"','"+target+"',"+W+","+H+")\"";
                        anchorStart += " style=\"text-decoration: underline; color: blue; cursor: pointer;\"";
                    } else {
                        anchorStart += " href=\""+url+"\"";
                        anchorStart += " target=\""+target+"\"";
                    }
                }
                anchorStart += ">";

                /* inline? */
                if (showInline) {

                    /* menu icon (will not be blank here) */
                    out.write("<td class='"+leafIconClass+"'>");
                    if (!StringTools.isBlank(iconURI)) {
                        out.write(anchorStart + "<img class='"+leafIconClass+"' src='"+iconURI+"'/></a>");
                    } else {
                        out.write("&nbsp;");
                    }
                    out.write("</td>");

                } else {

                    /* start menu list item */
                    out.write("<li class='"+leafClass+"'>");

                    /* special case for non-icons */
                    if (StringTools.isBlank(iconURI)) {

                        /* menu description/help */
                        if (!StringTools.isBlank(menuDesc)) {
                            out.write("<span class='"+leafDescClass+ "'>" + anchorStart + menuDesc + "</a></span>");
                            if (showMenuHelp && !StringTools.isBlank(menuHelp)) {
                                out.write("<br>");
                                out.write("<span class='"+helpPadClass+"'>"+menuHelp+"</span>");
                            }
                        }

                    } else {
                        // this section may not appear as expected on IE
    
                        /* start table */
                        out.write("<table class='"+leafClass+"' cellpadding='0' cellspacing='0'>");
                        out.write("<tr>");
    
                        /* menu icon */
                        if (!StringTools.isBlank(iconURI)) {
                            out.write("<td class='"+leafIconClass+"'>");
                            out.write(anchorStart + "<img class='"+leafIconClass+"' src='"+iconURI+"'/></a>");
                            out.write("</td>");
                        }
                    
                        /* menu description/help */
                        if (!StringTools.isBlank(menuDesc)) {
                            out.write("<td class='"+leafDescClass+"'>");
                            out.write("<span class='"+leafDescClass+ "'>" + anchorStart + menuDesc + "</a></span>");
                            if (showMenuHelp && !StringTools.isBlank(menuHelp)) {
                                out.write("<br>");
                                out.write("<span class='"+helpClass+"'>"+menuHelp+"</span>");
                            }
                            out.write("</td>");
                        }
    
                        /* end table */
                        out.write("</tr>");
                        out.write("</table>");
                    
                    }
    
                    /* end menu list item */
                    out.write("</li>\n");
                    
                }
 
            }
                
            /* end menu group */
            if (didDisplayGroup) {
                if (showInline) {
                    out.write("</tr></table>\n");
                } else {
                    out.write("</ul>\n");
                }
                out.write("</li>\n");
            }
                
        }
        
        /* end of menu */
        out.write("</ul>\n");
        
        /* init menu if expandable */
        if (expandableMenu) {
            out.write("<script type=\"text/javascript\"> new ExpandMenu('"+topMenuID+"'); </script>\n");
        }

    }

}
