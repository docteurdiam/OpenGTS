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
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2010/04/11  Martin D. Flynn
//     -Not displayed as an option if the logn "Password" field is hidden
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

public class ChangePassword
    extends WebPageAdaptor
    implements Constants
{
 
    // ------------------------------------------------------------------------

    public  static final String COMMAND_PWD_CHANGE  = "chg";
    
    public  static final String PARM_PWD_SUBMIT     = PARM_PASSWORD + "_subchg";
    public  static final String PARM_OLD_PASSWD     = PARM_PASSWORD + "_old";    
    public  static final String PARM_NEW1_PASSWD    = PARM_PASSWORD + "_nw1";    
    public  static final String PARM_NEW2_PASSWD    = PARM_PASSWORD + "_nw2";    

    // button types
    public  static final String PARM_BUTTON_CANCEL  = "b_cancel";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public ChangePassword()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_PASSWD);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ChangePassword.class);
        return super._getMenuDescription(reqState,i18n.getString("ChangePassword.menuDesc","Change your password"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ChangePassword.class);
        return super._getMenuHelp(reqState,i18n.getString("ChangePassword.menuHelp","Change your login password"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ChangePassword.class);
        return super._getNavigationDescription(reqState,i18n.getString("ChangePassword.navDesc","Change Password"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ChangePassword.class);
        return i18n.getString("ChangePassword.navTab","Change Password");
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(ChangePassword.class);
        final String pageName = this.getPageName();
        String m = "";
        boolean error = false;

        /* change the current password? */
        if (reqState.getCommandName().equals(COMMAND_PWD_CHANGE)) {
            HttpServletRequest request = reqState.getHttpServletRequest();
            String submit = AttributeTools.getRequestString(request, PARM_PWD_SUBMIT, "");
            if (SubmitMatch(submit,i18n.getString("ChangePassword.change","Change"))) {
                String pwdOld  = AttributeTools.getRequestString(request, PARM_OLD_PASSWD , "");
                String pwdNew1 = AttributeTools.getRequestString(request, PARM_NEW1_PASSWD, "");
                String pwdNew2 = AttributeTools.getRequestString(request, PARM_NEW2_PASSWD, "");
                User   user    = reqState.getCurrentUser();
                if (reqState.isDemoAccount()) {
                    m = i18n.getString("ChangePassword.cantChange","Can''t change ''{0}'' password", reqState.getCurrentAccountID());
                    error = true;
                } else
                if (user != null) {
                    if (!user.checkPassword(pwdOld)) {
                        m = i18n.getString("ChangePassword.invalidUserOldPass","Invalid ''Old'' password");
                        error = true;
                    } else
                    if (pwdNew1.equals("") || !pwdNew1.equals(pwdNew2)) { // also prevents blank passwords
                        m = i18n.getString("ChangePassword.unconfirmedUserPass","New password not confirmed");
                        error = true;
                    } else {
                        user.setDecodedPassword(pwdNew1);
                        try {
                            user.save();
                            Track.writeMessageResponse(reqState, i18n.getString("ChangePassword.userChanged","User password was successfully changed"));
                            return;
                        } catch (Throwable t) {
                            Print.logException("Saving User Password", t);
                            m = i18n.getString("ChangePassword.userError","Unable to save new user password");
                            error = true;
                        }
                    }
                } else {
                    Account account = reqState.getCurrentAccount();
                    if (!account.checkPassword(pwdOld)) {
                        m = i18n.getString("ChangePassword.invalidAcctPass","Invalid password");
                        error = true;
                    } else 
                    if (pwdNew1.equals("") || !pwdNew1.equals(pwdNew2)) { // also prevents blank passwords
                        m = i18n.getString("ChangePassword.unconfirmedAcctPass","New password not confirmed");
                        error = true;
                    } else {
                        account.setDecodedPassword(pwdNew1);
                        try {
                            account.save();
                            Track.writeMessageResponse(reqState, i18n.getString("ChangePassword.accountChanged","Account password was successfully changed"));
                            return;
                        } catch (Throwable t) {
                            Print.logException("Saving Account Password", t);
                            m = i18n.getString("ChangePassword.accountError","Unable to save new account password");
                            error = true;
                        }
                    }
                }
            } else {
                m = i18n.getString("ChangePassword.cancelled","Cancelled");
            }
        }
        
        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
            }
        };

        /* write frame */
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = ChangePassword.this.getPageName();
              //String menuURL = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
              //String pwdURL  = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),pageName,COMMAND_PWD_CHANGE);
                String pwdURL  = privLabel.getWebPageURL(reqState, pageName, COMMAND_PWD_CHANGE);
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+i18n.getString("ChangePassword.menuDesc","Change your password")+"</span><br/>\n");
                out.write("<span class='"+CommonServlet.CSS_MENU_INSTRUCTIONS+"'>"+i18n.getString("ChangePassword.enterCurrent","Enter your Current and New Passwords:")+"</span>\n");
                out.write("<hr/>\n");
                out.write("<form name='Passwd' method='post' action='"+pwdURL+"' target='_self'>\n"); // target='_top'
                out.write("  <table>\n");
                out.write("  <tr><td>"+i18n.getString("ChangePassword.oldPass","Old Password:")+"</td><td><input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='password' name='"+PARM_OLD_PASSWD+"' value='' maxlength='20' size='20'></td></tr>\n");
                out.write("  <tr><td>"+i18n.getString("ChangePassword.newPass","New Password:")+"</td><td><input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='password' name='"+PARM_NEW1_PASSWD+"' value='' maxlength='20' size='20'></td></tr>\n");
                out.write("  <tr><td>"+i18n.getString("ChangePassword.confirmNew","Confirm New:")+"</td><td><input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='password' name='"+PARM_NEW2_PASSWD+"' value='' maxlength='20' size='20'></td></tr>\n");
                out.write("  </table>\n");
                out.write("  <input type='submit' name='"+PARM_PWD_SUBMIT+"' value='"+i18n.getString("ChangePassword.change","Change")+"'>\n");
                out.write("  <hr style='margin: 5px 0px 5px 0px;'>\n");
              //out.write("  <a href='"+menuURL+"'>"+i18n.getString("ChangePassword.cancel","Cancel")+"</a>\n");
                out.write("  <input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("ChangePassword.cancel","Cancel")+"' onclick=\"javascript:openURL('"+menuURL+"','_self');\">\n"); // target='_top'
                out.write("</form>\n");
            }
        };

        /* onload alert message? */
        String onload = null;
        if (error && !StringTools.isBlank(m)) {
            // assumed that 'm' does not contain double-quotes
            onload = "javascript:alert(&quot;" + m + "&quot;);";
        }

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTMLOutput.NOOP,            // Style sheets
            HTML_JS,                    // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

}
