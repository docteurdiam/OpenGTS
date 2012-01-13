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
//  2011/08/21  Martin D. Flynn
//     -Moved "button-icon" text to below the icon
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

public class IconMenu
{
    
    private static final String BLANK_IMAGE     = "images/blank.png";

    // ------------------------------------------------------------------------

    private static final String PROP_iconMenuCSS_menuItemImage_on_background  = "iconMenuCSS.menuItemImage_on.background";
    private static final String PROP_iconMenuCSS_menuItemImage_off_background = "iconMenuCSS.menuItemImage_off.background";
    private static final String PROP_iconMenuCSS_menuItemText_on_background   = "iconMenuCSS.menuItemText_on.background";
    private static final String PROP_iconMenuCSS_menuItemText_off_background  = "iconMenuCSS.menuItemText_off.background";
    private static final String PROP_iconMenuCSS_menuItemTextW_on_background  = "iconMenuCSS.menuItemTextW_on.background";
    private static final String PROP_iconMenuCSS_menuItemTextW_off_background = "iconMenuCSS.menuItemTextW_off.background";

    // ------------------------------------------------------------------------
    // write Style

    public static void writeStyle(JspWriter out, RequestProperties reqState)
        throws IOException
    {
        IconMenu.writeStyle(new PrintWriter(out, out.isAutoFlush()), reqState);
    }

    public static void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        WebPageAdaptor.writeCssLink(out, reqState, "IconMenu.css", null);
        
        // overrides below
        PrivateLabel pl = reqState.getPrivateLabel();
        String indent = "  ";
        String menuItemImage_on  = pl.getStringProperty(PROP_iconMenuCSS_menuItemImage_on_background, null);
        String menuItemImage_off = pl.getStringProperty(PROP_iconMenuCSS_menuItemImage_off_background, null);
        String menuItemText_on   = pl.getStringProperty(PROP_iconMenuCSS_menuItemText_on_background, null);
        String menuItemText_off  = pl.getStringProperty(PROP_iconMenuCSS_menuItemText_off_background, null);
        String menuItemTextW_on  = pl.getStringProperty(PROP_iconMenuCSS_menuItemTextW_on_background, null);
        String menuItemTextW_off = pl.getStringProperty(PROP_iconMenuCSS_menuItemTextW_off_background, null);
        
        // <style type="text/css">
        boolean writeEndScript = false;
        if ((menuItemImage_on != null) || (menuItemImage_off != null) ||
            (menuItemText_on  != null) || (menuItemText_off  != null) ||
            (menuItemTextW_on != null) || (menuItemTextW_off != null)   ) {
            out.println(indent+"<style type=\"text/css\">");
            writeEndScript = true;
        }
        
        // menuItemImage_on, menuItemImage_off
        if (!StringTools.isBlank(menuItemImage_on)) {
            out.println(indent+"  #iconMenu TD.menuItemImage_on {");
            out.println(indent+"    background: url('"+menuItemImage_on+"') no-repeat;"); // "../images/MenuBtnH.png"
            out.println(indent+"  }");
        }
        if (!StringTools.isBlank(menuItemImage_off)) {
            out.println(indent+"  #iconMenu TD.menuItemImage_off {");
            out.println(indent+"    background: url('"+menuItemImage_off+"') no-repeat;"); // "../images/MenuBtn.png"
            out.println(indent+"  }");
        }
        
        // menuItemText_on, menuItemText_off
        if (!StringTools.isBlank(menuItemText_on)) {
            out.println(indent+"  #iconMenu TD.menuItemText_on {");
            out.println(indent+"    background: url('"+menuItemText_on+"') no-repeat;"); // "../images/MenuBtnH.png"
            out.println(indent+"  }");
        }
        if (!StringTools.isBlank(menuItemText_off)) {
            out.println(indent+"  #iconMenu TD.menuItemText_off {");
            out.println(indent+"    background: url('"+menuItemText_off+"') no-repeat;"); // "../images/MenuBtn.png"
            out.println(indent+"  }");
        }
        
        // menuItemTextW_on, menuItemTextW_off
        if (!StringTools.isBlank(menuItemTextW_on)) {
            out.println(indent+"  #iconMenu TD.menuItemTextW_on {");
            out.println(indent+"    background: url('"+menuItemTextW_on+"') no-repeat;"); // "../images/MenuBtnH_lg.png"
            out.println(indent+"  }");
        }
        if (!StringTools.isBlank(menuItemTextW_off)) {
            out.println(indent+"  #iconMenu TD.menuItemTextW_off {");
            out.println(indent+"    background: url('"+menuItemTextW_off+"') no-repeat;"); // "../images/MenuBtn_lg.png"
            out.println(indent+"  }");
        }
        
        // </script>
        if (writeEndScript) {
            out.println(indent+"</style>");
        }

    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeJavaScript(JspWriter out, RequestProperties reqState)
        throws IOException
    {
        IconMenu.writeJavaScript(new PrintWriter(out, out.isAutoFlush()), reqState);
    }

    public static void writeJavaScript(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("IconMenu.js"), request);
    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeMenu(JspWriter out, RequestProperties reqState, String menuID, int maxIconsPerRow, boolean showIcon)
        throws IOException
    {
        IconMenu.writeMenu(new PrintWriter(out, out.isAutoFlush()), reqState, menuID, maxIconsPerRow, showIcon);
    }

    public static void writeMenu(PrintWriter out, RequestProperties reqState, String menuID, int maxIconsPerRow, boolean showIcon)
        throws IOException
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        Locale       locale    = reqState.getLocale();
        String  parentPageName = null;
        Account       account  = reqState.getCurrentAccount();

        /* sub style classes */
        // these are defined in "IconMenu.css"
        String topMenuID         = "iconMenu";
        String menuTableClass    = "iconMenuTable";
        String groupTitleClass   = "menuGroupTitle";
        String groupIconsClass   = "menuGroupIcons";
        String menuItemTable     = "menuItemTable";
        String menuItemRow       = "menuItemRow";
        String menuItemImage_on  = "menuItemImage_on";
        String menuItemImage_off = "menuItemImage_off";
        String menuItemImage     = "menuItemImage";
        String menuItemText_on   = "menuItemText_on";
        String menuItemText_off  = "menuItemText_off";
        String menuItemText      = "menuItemText";
        String menuIconImage     = "menuIconImage";

        /* start menu */
        out.println("<table id='"+topMenuID+"' class='"+menuTableClass+"' cellpadding='0' cellspacing='0' border='0' width='100%'>");

        /* iterate through menu groups */
        Map<String,MenuGroup> menuMap = privLabel.getMenuGroupMap();
        for (String mgn : menuMap.keySet()) {
            MenuGroup mg = menuMap.get(mgn);
            if (!mg.showInTopMenu()) {
                continue; // skip this group
            }

            int rowIconCount = 0;
            boolean didDisplayGroup = false;
            for (WebPage wp : mg.getWebPageList(reqState)) {
                String menuName  = wp.getPageName();
                String iconImg   = showIcon? wp.getMenuIconImage() : null;
                String buttonImg = wp.getMenuButtonImage();
                String buttonAlt = wp.getMenuButtonAltImage();
                String url       = wp.encodePageURL(reqState);//, RequestProperties.TRACK_BASE_URI());

                /* skip login page */
                if (menuName.equalsIgnoreCase(Constants.PAGE_LOGIN)) { 
                    //Print.logInfo("Skipping Login ...");
                    continue; // omit login
                }

                /* skip sysAdmin pages */
                if (wp.systemAdminOnly() && !Account.isSystemAdmin(account)) {
                    //Print.logInfo("Skipping SysAdmin item ...");
                    continue;
                }

                /* skip pages that are not ok to display */
                if (!wp.isOkToDisplay(reqState)) {
                    continue; 
                }

                /* start menu group */
                if (!didDisplayGroup) {
                    didDisplayGroup = true;
                    out.write("\n");
                    out.write("<!-- "+mg.getTitle(null)+" -->\n");
                    out.write("<tr class='"+groupTitleClass+"'><td class='"+groupTitleClass+"' width='100%'>"+mg.getTitle(locale)+"</td></tr>\n");
                    out.write("<tr class='"+groupIconsClass+"'><td class='"+groupIconsClass+"' width='100%'>\n");
                    out.write("<table class='"+menuItemTable+"' border='0'>\n"); //cellspacing='0' cellpadding='0' 
                    out.write("<tr class='"+menuItemRow+"'>\n");
                }

                /* wrap to next line? */
                if ((maxIconsPerRow > 0) && (rowIconCount >= maxIconsPerRow)) {
                    out.write("</tr>\n");
                    out.write("<tr class='"+menuItemRow+"'>\n");
                    rowIconCount = 0;
                }

                /* menu description */
                // replace all spaces with a newline "<BR>"
                String menuDesc = StringTools.trim(wp.getNavigationDescription(reqState)); // short
                menuDesc = filterButtonMenuDescription(menuDesc);

                /* menu help */
                String menuHelp = StringTools.trim(wp.getMenuHelp(reqState, parentPageName));

                /* icon */
                String classOff = !StringTools.isBlank(buttonImg)? menuItemImage_off : menuItemText_off;
                String classOn  = !StringTools.isBlank(buttonImg)? menuItemImage_on  : menuItemText_on;
                String target   = StringTools.blankDefault(wp.getTarget(),"_self"); // (wp instanceof WebPageURL)? ((WebPageURL)wp).getTarget() : "_self";
                String onclick  = "javascript:openURL('"+url+"','"+target+"')";
                if (!target.startsWith("_")) {
                    PixelDimension pixDim = wp.getWindowDimension();
                    if (pixDim != null) {
                        int W = pixDim.getWidth();
                        int H = pixDim.getHeight();
                        onclick = "javascript:openFixedWindow('"+url+"','"+target+"',"+W+","+H+")";
                    }
                }
                out.write("<td class='"+classOff+"' title=\""+menuHelp+"\""+
                    " onmouseover=\"this.className='"+classOn+"'\""+
                    " onmouseout=\"this.className='"+classOff+"'\""+
                    " onclick=\""+onclick+"\""+
                    ">");
                if (StringTools.isBlank(buttonImg)) {
                    // draw text over background image, include icon if specified
                    if (!StringTools.isBlank(iconImg)) {
                        out.write("<img class='"+menuIconImage+"' border='0' src='"+iconImg+"'/>");
                        out.write("<br>");
                    }
                    out.write("<span class='"+menuItemText+"'>"+menuDesc+"</span>");
                } else {
                    // draw the main button image itself (no text)
                    out.write("<img class='"+menuItemImage+"' border='0' src='"+buttonImg+"'");
                    if (!StringTools.isBlank(buttonAlt)) {
                        out.write(" onmouseover=\"this.src='"+buttonAlt+"'\"");
                        out.write(" onmouseout=\"this.src='" +buttonImg+"'\"");
                    }
                    out.write("/>");
                }
                out.write("</td>\n");
                rowIconCount++;

            }
                
            /* end menu group */
            if (didDisplayGroup) {
                out.write("</tr>\n");
                out.write("</table>\n");
                out.write("</td></tr>\n");
                out.write("\n");
            }

        }
        
        /* end of menu */
        out.write("</table>\n");

    }

    /* break menu description text into reasonable length lines */
    private static String filterButtonMenuDescription(String str)
    {
        String s[] = StringTools.split(str, ' ');
        StringBuffer sb = new StringBuffer();
        int len = 0;
        for (int i = 0; i < s.length; i++) {
            String x = s[i].trim();
            int xlen = x.length();
            if (xlen == 0) {
                continue;
            } else
            if ((len + 1 + xlen) > 13) {
                sb.append("<BR>");
                len = 0;
            } else {
                sb.append(" ");
                len += 1;
            }
            sb.append(x);
            len += xlen;
        }
        return sb.toString(); // StringTools.replace(s, " ", "<BR>");
    }
    
    // ------------------------------------------------------------------------

}
