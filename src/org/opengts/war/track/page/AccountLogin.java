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
//  2007/12/13  Martin D. Flynn
//     -Changed form target to '_self' for "ContentOnly" display
//  2008/12/01  Martin D. Flynn
//     -Increased maxsize for account/user/password fields to match length specified
//      in their respective tables.
//  2009/01/01  Martin D. Flynn
//     -Added popup 'alert' for login errors
//  2009/04/11  Martin D. Flynn
//     -Added focus on login field
//     -Added support for hiding the "Password" field on the login page
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

public class AccountLogin
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Tomcat conf/server.xml
    //   emptySessionPath="true"
    //   <SessionId cookiesFirst="true" noCookies="true"/>
    // HttpServletResponse.encodeURL() 
    // ------------------------------------------------------------------------

    private static       String FORM_LOGIN                  = "Login";
    private static       String FORM_DEMO                   = "Demo";

    // ------------------------------------------------------------------------

    private static final String CSS_ACCOUNT_LOGIN_BORDER[]  = new String[] { "accountLoginTable", "accountLoginCell" };
    private static final String CSS_ACCOUNT_LOGIN_NOBORD[]  = new String[] { "accountLoginTable_nobord", "accountLoginCell_nobord" };

    private static final String CSS_LOGIN_CONTENT_CENTER    = "accountLoginContentTable";
    private static final String CSS_LOGIN_CONTENT_LEFT      = "accountLoginContentTable";

    public  static final String CSS_LOGIN_VSEP_CELL         = "accountLoginVertSepCell";
    public  static final String CSS_LOGIN_TEXT_CELL_CENTER  = "accountLoginTextCell";
    public  static final String CSS_LOGIN_TEXT_CELL_LEFT    = "accountLoginTextCell";
    public  static final String CSS_LOGIN_FORM_PAD          = "accountLoginFormTable";
    public  static final String CSS_LOGIN_FORM_NOPAD        = "accountLoginFormTable";

    // ------------------------------------------------------------------------
    // Properties

    public  static final String PROP_customLoginUrl         = "customLoginUrl";
    public  static final String PROP_VSeparatorImage        = "VSeparatorImage.path";
    public  static final String PROP_VSeparatorImage_W      = "VSeparatorImage.width";
    public  static final String PROP_VSeparatorImage_H      = "VSeparatorImage.height";
  //public  static final String PROP_legacyLAF              = "legacyLAF";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public AccountLogin()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        super.setPageName(PAGE_LOGIN); // 'super' required here
        this.setPageNavigation(new String[] { PAGE_LOGIN });
        this.setLoginRequired(false);
    }

    // ------------------------------------------------------------------------

    public void setPageName(String pageName)
    {
        // ignore (changing the PAGE_LOGIN name is not allowed)
    }

    // ------------------------------------------------------------------------
    
    
    public URIArg getPageURI(String command, String cmdArg)
    {
        String loginURL = this.getProperties().getString(PROP_customLoginUrl,null);
        if (!StringTools.isBlank(loginURL)) {
            Print.logInfo("Login custom URL: " + loginURL);
            return new URIArg(loginURL);
        } else {
            return super.getPageURI(command, cmdArg);
        }
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        return super._getMenuDescription(reqState,i18n.getString("AccountLogin.menuDesc","Logout"));
    }

    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        return super._getMenuHelp(reqState,i18n.getString("AccountLogin.menuHelp","Logout"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        if (reqState.isLoggedIn()) {
            return i18n.getString("AccountLogin.navDesc","Logout");
        } else
        if (privLabel.getBooleanProperty(PrivateLabel.PROP_AccountLogin_showLoginLink,true)) {
            return i18n.getString("AccountLogin.navDesc.login","Login");
        } else {
            return super._getNavigationDescription(reqState,"");
        }
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        return i18n.getString("AccountLogin.navTab","Logout");
    }

    // ------------------------------------------------------------------------

    public boolean isOkToDisplay(RequestProperties reqState)
    {
        PrivateLabel privLabel = (reqState != null)? reqState.getPrivateLabel() : null;
        if (privLabel == null) {
            // no PrivateLabel?
            return false;
        } else
        if (!privLabel.getShowPassword()) {
            // Password is hidden, don't show the "Change Password" page
            return false;
        } else {
            // show "Change Password"
            return true;
        }
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(AccountLogin.class);
      //final boolean legacy = this.getProperties().getBoolean(PROP_legacyLAF,false);
        final boolean legacy = privLabel.getBooleanProperty(PrivateLabel.PROP_AccountLogin_legacyLAF,false);
        final boolean borderedCss = legacy; // this.getProperties().getBoolean(PROP_boarderedLogin,true);
        final String HR = legacy? "<hr>" : "<hr style='height: 5px;'/>";

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = AccountLogin.this.getCssDirectory(); 
                WebPageAdaptor.writeCssLink(out, reqState, "AccountLogin.css", cssDir);
            }
        };

        /* write frame */
        String cssAccountLogin[] = borderedCss? CSS_ACCOUNT_LOGIN_BORDER : CSS_ACCOUNT_LOGIN_NOBORD;
        HTMLOutput HTML_CONTENT = new HTMLOutput(cssAccountLogin, pageMsg) {
            public void write(PrintWriter out) throws IOException {
                // baseURL
                URIArg  baseURI    = MakeURL(RequestProperties.TRACK_BASE_URI(),null,null,null);
                HttpServletRequest req = reqState.getHttpServletRequest();
                String rtpArg      = (req != null)? req.getParameter(AttributeTools.ATTR_RTP) : null;
                if (!StringTools.isBlank(rtpArg)) { baseURI.addArg(AttributeTools.ATTR_RTP,rtpArg); }
                String  baseURL    = EncodeURL(reqState, baseURI);
                String  accountID  = StringTools.trim(AccountRecord.getFilteredID(AttributeTools.getRequestString(req,Constants.PARM_ACCOUNT,"")));
                String  userID     = StringTools.trim(AccountRecord.getFilteredID(AttributeTools.getRequestString(req,Constants.PARM_USER   ,"")));
                // other args
                String  newURL     = reqState.getPrivateLabel().hasWebPage(PAGE_ACCOUNT_NEW )? 
                    //EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_ACCOUNT_NEW ) : null;
                    privLabel.getWebPageURL(reqState,PAGE_ACCOUNT_NEW) : null;
                String  forgotURL  = reqState.getPrivateLabel().hasWebPage(PAGE_PASSWD_EMAIL)? 
                    //EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_PASSWD_EMAIL) : null;
                    privLabel.getWebPageURL(reqState,PAGE_PASSWD_EMAIL) : null;
                boolean acctLogin  = reqState.getPrivateLabel().getAccountLogin();
                boolean userLogin  = reqState.getPrivateLabel().getUserLogin();
                boolean emailLogin = reqState.getPrivateLabel().getAllowEmailLogin();
                boolean showPasswd = reqState.getShowPassword();
                boolean showLocale = privLabel.getBooleanProperty(PrivateLabel.PROP_AccountLogin_showLocaleSelection, false);
                boolean showDemo   = reqState.getEnableDemo();
                String  target     = "_self"; // reqState.getPageFrameContentOnly()? "_self" : "_top";  // target='_top'
                boolean loginOK    = privLabel.getBooleanProperty(BasicPrivateLabelLoader.ATTR_allowLogin, true);
                String  ro         = loginOK? "" : "readonly";
                // ----------------------------------
                // Basic login input form:
                //  <form name="login" method="post" action="http://track.example.com:8080/track/Track" target="_top">
                //      Account:  <input name="account"  type="text"     size='20' maxlength='32'> <br>
                //      User:     <input name="user"     type="text"     size='20' maxlength='32'> <br>
                //      Password: <input name="password" type="password" size='20' maxlength='32'> <br>
                //      <input type="submit" name="submit" value="Login">
                //  </form>
                String cssLoginContent = borderedCss? CSS_LOGIN_CONTENT_CENTER : CSS_LOGIN_CONTENT_LEFT;
                out.println("<table class='"+cssLoginContent+"' width='100%' cellpadding='0' cellspacing='0' border='0'>");
                out.println("<tr>");
                
                String vsepImg = legacy? null : AccountLogin.this.getProperties().getString(PROP_VSeparatorImage,null);
                if (!StringTools.isBlank(vsepImg)) {
                    String W = AccountLogin.this.getProperties().getString(PROP_VSeparatorImage_W,null);
                    String H = AccountLogin.this.getProperties().getString(PROP_VSeparatorImage_H,null);
                    out.print("<td class='"+CSS_LOGIN_VSEP_CELL+"'>");
                    out.print("<img ");
                    if (!StringTools.isBlank(W)) { out.print(" width='"+W+"'"); }
                    if (!StringTools.isBlank(H)) { out.print(" height='"+H+"'"); }
                    out.print(" src='"+vsepImg+"'/>");
                    out.print("</td>");
                }

                String cssLoginText = borderedCss? CSS_LOGIN_TEXT_CELL_CENTER : CSS_LOGIN_TEXT_CELL_LEFT;
                out.println("<td class='"+cssLoginText+"'>");
                String enterLoginText = showPasswd?
                    i18n.getString("AccountLogin.enterLogin","Enter your Login ID and Password") :
                    i18n.getString("AccountLogin.enterLoginNoPass","Enter Login ID (No Password Required)");
                out.println("<span style='font-size:11pt'>"+enterLoginText+"</span>");
                out.println(HR);
                //out.println("<center>"); // necessary because "text-align:center" doesn't center the following form/table
                out.println("<form name='"+FORM_LOGIN+"' method='post' action='"+baseURL+"' target='"+target+"'>");
                String cssLoginFormTable = borderedCss? CSS_LOGIN_FORM_PAD : CSS_LOGIN_FORM_NOPAD;
                out.println("  <table class='"+cssLoginFormTable+"' cellpadding='0' cellspacing='0' border='0'>");
                String focusFieldID = "";
                // account login field
                if (acctLogin) {
                    String fldID = "accountLoginField";
                    out.print("  <tr><td class='accountLoginFieldLabel'>"+i18n.getString("AccountLogin.account","Account:")+"</td>");
                    out.print(      "<td class='accountLoginFieldValue'><input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' "+ro+" name='"+Constants.PARM_ACCOUNT+"' value='"+accountID+"' size='24' maxlength='32'></td></tr>");
                    out.print("\n");
                    focusFieldID = fldID;
                }
                // user login field
                if (userLogin && emailLogin) {
                    String fldID = "userLoginField";
                    out.print("  <tr><td class='accountLoginFieldLabel'>"+i18n.getString("AccountLogin.userEmail","User/EMail:")+"</td>");
                    out.print(      "<td class='accountLoginFieldValue'><input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' "+ro+" name='"+Constants.PARM_USER+"' value='"+userID+"' size='30' maxlength='40'></td></tr>");
                    out.print("\n");
                    if (StringTools.isBlank(focusFieldID)) { focusFieldID = fldID; }
                } else
                if (userLogin) {
                    String fldID = "userLoginField";
                    out.print("  <tr><td class='accountLoginFieldLabel'>"+i18n.getString("AccountLogin.user","User:")+"</td>");
                    out.print(      "<td class='accountLoginFieldValue'><input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' "+ro+" name='"+Constants.PARM_USER+"' value='"+userID+"' size='24' maxlength='32'></td></tr>");
                    out.print("\n");
                    if (StringTools.isBlank(focusFieldID)) { focusFieldID = fldID; }
                } else
                if (emailLogin) {
                    String fldID = "emailLoginField";
                    out.print("  <tr><td class='accountLoginFieldLabel'>"+i18n.getString("AccountLogin.email","EMail:")+"</td>");
                    out.print(      "<td class='accountLoginFieldValue'><input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' "+ro+" name='"+Constants.PARM_USEREMAIL+"' value='"+userID+"' size='30' maxlength='40'></td></tr>");
                    out.print("\n");
                    if (StringTools.isBlank(focusFieldID)) { focusFieldID = fldID; }
                }
                // password field
                if (showPasswd) {
                    out.print("  <tr>");
                    out.print(    "<td class='accountLoginFieldLabel'>"+i18n.getString("AccountLogin.password","Password:")+"</td>");
                    out.print(    "<td class='accountLoginFieldValue'><input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='password' "+ro+" name='"+Constants.PARM_PASSWORD+"' value='' size='24' maxlength='32'></td>");
                    out.print(  "</tr>\n");
                }
                // language selection
                if (showLocale) {
                    String dftLocale = privLabel.getLocaleString();
                    Map<String,String> localeMap = BasicPrivateLabel.GetSupportedLocaleMap(privLabel.getLocale());
                    ComboMap comboLocaleMap = new ComboMap(localeMap);
                    out.print("  <tr>");
                    out.print(    "<td class='accountLoginFieldLabel'>"+i18n.getString("AccountLogin.language","Language:")+"</td>");
                    out.print(    "<td class='accountLoginFieldValue'>");
                    out.write(      Form_ComboBox(CommonServlet.PARM_LOCALE, CommonServlet.PARM_LOCALE, true, comboLocaleMap, dftLocale, null/*onchange*/));
                    out.print(    "</td>");
                    out.print(  "</tr>\n");
                }
                // end table
                out.print("</table>\n");
                // Login
                out.print("<br>");
                out.print("<input type='submit' name='submit' value='"+i18n.getString("AccountLogin.login","Login")+"'>\n");
                // forgot password
                if (showPasswd && (forgotURL != null)) {
                   if (legacy) { out.println("<br>"); }
                   out.println("  <span style='font-size:8pt;padding-left:10px;'><i><a href='"+forgotURL+"'>"+i18n.getString("AccountLogin.forgotPassword","Forgot your password?")+"</a></i></span>");
                }
                // end forn
                out.println("</form>");
                // "Cookies/JavaScript must be enabled"
                out.println("<br/>");
                out.println("<span style='font-size:8pt'><i>"+i18n.getString("AccountLogin.cookiesJavaScript","(Cookies and JavaScript must be enabled)")+"</i></span>");
                //out.println("<br/>");
                out.println("<br/>");
                // demo
                if (showDemo) {
                    out.println(HR);
                    //out.println("<br/>");
                    out.println("<form name='"+FORM_DEMO+"' method='post' action='"+baseURL+"' target='"+target+"'>");
                    out.println("  <input type='hidden' name='"+Constants.PARM_ACCOUNT  +"' value='"+reqState.getDemoAccountID()+"'/>");
                    out.println("  <input type='hidden' name='"+Constants.PARM_USER     +"' value=''/>");
                    out.println("  <input type='hidden' name='"+Constants.PARM_PASSWORD +"' value=''/>");
                    out.println("  <span style='font-size:9pt;padding-right:5px;'>"+i18n.getString("AccountLogin.freeDemo","Or click here for a demonstration")+"</span>");
                    out.println("  <input type='submit' name='submit' value='"+i18n.getString("AccountLogin.demo","Demo")+"'>");
                    out.println("</form>");
                    //out.println("<br/>");
                }
                // New Account
                if (newURL != null) {
                    out.println(HR);
                    out.println("<span style='font-size:8pt'><i><a href='"+newURL+"'>"+i18n.getString("AccountLogin.freeAccount","Sign up for a free account")+"</a></i></span>");
                }
                out.println("</td>");
                out.println("</tr>");
                out.println("</table>");
                
                /* set focus */
                if (!StringTools.isBlank(focusFieldID)) {
                    out.write("<script type=\"text/javascript\">\n");
                    out.write("var loginFocusField = document.getElementById('"+focusFieldID+"');\n");
                    out.write("if (loginFocusField) {\n");
                    out.write("    loginFocusField.focus();\n");
                    out.write("    loginFocusField.select();\n");
                    out.write("}\n");
                    out.write("</script>\n");
                }

                
            }
        };

        /* write frame */
        String onload = (!StringTools.isBlank(pageMsg) && reqState._isLoginErrorAlert())? JS_alert(true,pageMsg) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTMLOutput.NOOP,            // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------

}
