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
//  2007/03/30  Martin D. Flynn
//     -Added access control
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/09/16  Martin D. Flynn
//     -Changed localization key "TopMenu.selectIem" to "TopMenu.selectItem"
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class TopMenu
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    public static final String MENU_FIXED           = "fixed";
    public static final String MENU_FIXED_ICON      = "fixed-icon";
    public static final String MENU_EXPAND          = "expand";
    public static final String MENU_EXPAND_ICON     = "expand-icon";
    public static final String MENU_BUTTON          = "button";
    public static final String MENU_BUTTON_ICON     = "button-icon";

    public enum MenuType implements EnumTools.IntValue, EnumTools.StringValue {
        FIXED        ( 0, MENU_FIXED       ), // default
        FIXED_ICON   ( 1, MENU_FIXED_ICON  ),
        EXPAND       ( 2, MENU_EXPAND      ),
        EXPAND_ICON  ( 3, MENU_EXPAND_ICON ),
        BUTTON       ( 4, MENU_BUTTON      ),
        BUTTON_ICON  ( 5, MENU_BUTTON_ICON );
        // ---
        private int      vv = 0;
        private String   aa = null;
        MenuType(int v, String a)       { vv = v; aa = a; }
        public boolean isFixed()        { return this.equals(FIXED ) || this.equals(FIXED_ICON ); }
        public boolean isExpandable()   { return this.equals(EXPAND) || this.equals(EXPAND_ICON); }
        public boolean isButton()       { return this.equals(BUTTON) || this.equals(BUTTON_ICON); }
        public boolean isIcon()         { return this.equals(FIXED_ICON) || this.equals(EXPAND_ICON) || this.equals(BUTTON_ICON); }
        public int     getIntValue()    { return vv; }
        public String  getStringValue() { return this.toString(); }
        public String  toString()       { return aa; }
    }

    // ------------------------------------------------------------------------

    public TopMenu()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_MENU_TOP);
        this.setPageNavigation(new String[] { PAGE_LOGIN });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------
    
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_MAIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(TopMenu.class);
        return super._getMenuDescription(reqState,"");
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(TopMenu.class);
        return super._getMenuHelp(reqState,i18n.getString("TopMenu.menuHelp","Main Menu"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(TopMenu.class);
        return super._getNavigationDescription(reqState,i18n.getString("TopMenu.navDesc","Main Menu"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(TopMenu.class);
        return super._getNavigationTab(reqState,i18n.getString("TopMenu.navTab","Main Menu"));
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel  = reqState.getPrivateLabel();
        final I18N         i18n       = privLabel.getI18N(TopMenu.class);
        final Locale       locale     = reqState.getLocale();
        final String       pageName   = TopMenu.this.getPageName();

        /* menu type */
        final MenuType menuType = EnumTools.getValueOf(MenuType.class, 
            privLabel.getStringProperty(PrivateLabel.PROP_TopMenu_menuType,null),
            MenuType.FIXED);

        /* show frame header: ie "Main Menu" */
        final boolean showHeader;
        String showHeaderStr = privLabel.getStringProperty(PrivateLabel.PROP_TopMenu_showHeader,"default");
        if (StringTools.isBlank(showHeaderStr) || showHeaderStr.equalsIgnoreCase("default")) {
            showHeader = menuType.isButton()? false : true;
        } else {
            showHeader = StringTools.parseBoolean(showHeaderStr,true);
        }

        /* show menu description: none/icon, short, long */
        final int descriptionType;
        String descTypeSte = privLabel.getStringProperty(PrivateLabel.PROP_TopMenu_showMenuDescription,"long");
        if (descTypeSte.equalsIgnoreCase("none") || descTypeSte.equalsIgnoreCase("icon")) {
            descriptionType = ExpandMenu.DESC_NONE;
        } else
        if (descTypeSte.equalsIgnoreCase("short")) {
            descriptionType = ExpandMenu.DESC_SHORT;
        } else {
            descriptionType = ExpandMenu.DESC_LONG;
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                switch (menuType) {
                    case FIXED:
                    case FIXED_ICON:
                        //
                        break;
                    case EXPAND:
                    case EXPAND_ICON:
                        ExpandMenu.writeStyle(out, reqState);
                        break;
                    case BUTTON:
                    case BUTTON_ICON:
                        IconMenu.writeStyle(out, reqState);
                        break;
                }
                WebPageAdaptor.writeCssLink(out, reqState, "TopMenu.css", TopMenu.this.getCssDirectory());
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                switch (menuType) {
                    case FIXED:
                    case FIXED_ICON:
                        //
                        break;
                    case EXPAND:
                    case EXPAND_ICON:
                        ExpandMenu.writeJavaScript(out, reqState);
                        break;
                    case BUTTON:
                    case BUTTON_ICON:
                        IconMenu.writeJavaScript(out, reqState);
                        break;
                }
            }
        };

        /* write frame */
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_MENU, pageMsg) {
            public void write(PrintWriter out) throws IOException {

                Account account = reqState.getCurrentAccount();
                String acctDesc = account.getDescription();

                /* frame header */
                if (showHeader) {
                    out.println("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+
                        i18n.getString("TopMenu.mainMenu","Main Menu")+
                        "</span><br/>");
                    out.println("<span class='"+CommonServlet.CSS_MENU_INSTRUCTIONS+"'>"+
                        i18n.getString("TopMenu.selectItem","Please select an item from the following menu:")+
                        "</span><br/>");
                    out.println("<hr/>");
                }

                /* display menu */
                boolean showIcon = menuType.isIcon();
                switch (menuType) {
                    case FIXED: 
                    case FIXED_ICON: {
                        boolean menuHelp = privLabel.getBooleanProperty(PrivateLabel.PROP_TopMenu_showMenuHelp,true);
                        ExpandMenu.writeMenu(out, reqState, "topMenuFixed", false, showIcon, descriptionType, menuHelp);
                    } break;
                    case EXPAND:
                    case EXPAND_ICON: {
                        boolean menuHelp = privLabel.getBooleanProperty(PrivateLabel.PROP_TopMenu_showMenuHelp,true);
                        ExpandMenu.writeMenu(out, reqState, "topMenuExpand", true, showIcon, descriptionType, menuHelp);
                    } break;
                    case BUTTON:
                    case BUTTON_ICON: {
                        int maxIPR = privLabel.getIntProperty(PrivateLabel.PROP_TopMenu_maximumIconsPerRow,-1);
                        IconMenu.writeMenu(out, reqState, "topMenuIcon", maxIPR, showIcon);
                    } break;
                }

            }
        };

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            null,null,                      // onLoad/onUnload
            HTML_CSS,                       // Style sheets
            HTML_JS,                        // JavaScript
            null,                           // Navigation
            HTML_CONTENT);                  // Content

    }

}
