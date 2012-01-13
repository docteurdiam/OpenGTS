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
//  2007/02/25  Martin D. Flynn
//     -Included in standard OpenGTS release
//  2007/03/30  Martin D. Flynn
//     -Added 'User' support
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2009/01/01  Martin D. Flynn
//     -Added additional instructional text.
//  2009/12/16  Martin D. Flynn
//     -Fixed creation of "Submit" button URL.
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

public class ForgotPassword
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    // setting to 'false' may allow a would-be hacker to glean information such as valid 
    // account and user ids.  While they may not be able to obtain the account/user password,
    // knowing a valid account/user id may provide the hacker additional critical information
    // for hacking in to the system.  It is definately recommended that this value remain 'true'
    // to increase the security of forgotten password requests.
    private static final boolean SECURE_RESPONSE            = true;  // more secure when 'true'
  //private static final boolean SECURE_RESPONSE            = false; // definately not secure when false

    // ------------------------------------------------------------------------

    // Settng this value to 'true' allows sending a list of all account owned by a particular
    // contact email address to the contact person.
    private static final boolean SEND_ACCOUNT_LIST          = false;
  //private static final boolean SEND_ACCOUNT_LIST          = true;

    // ------------------------------------------------------------------------

    private static final long    MIN_PASS_QUERY_DELTA_SEC   = DateTime.MinuteSeconds(20L);
    
    public  static final String  COMMAND_EMAIL              = "email";
    
    public  static final String  PARM_EMAIL_SUBMIT          = "email_submit";
    public  static final String  PARM_EMAIL_ACCOUNT         = "email_acct";
    public  static final String  PARM_EMAIL_USER            = "email_user";
    public  static final String  PARM_EMAIL_ADDRESS         = "email_addr";

    public  static final String  CSS_FORGOT_PASSWORD[]      = new String[] { "forgotPasswordTable", "forgotPasswordCell" };

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public ForgotPassword()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_PASSWD_EMAIL);
        this.setPageNavigation(new String[] { PAGE_LOGIN });
        this.setLoginRequired(false);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ForgotPassword.class);
        return super._getMenuDescription(reqState,i18n.getString("ForgotPassword.menuDesc","Forgot your Password?"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ForgotPassword.class);
        return super._getMenuHelp(reqState,i18n.getString("ForgotPassword.menuHelp","Forgot Password"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ForgotPassword.class);
        return super._getNavigationDescription(reqState,"");
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ForgotPassword.class);
        return "";
    }

    // ------------------------------------------------------------------------
    
    private static String sendAccountsForContactEMail(
        PrivateLabel privLabel,
        String contactEmail)
    {
        final I18N i18n = privLabel.getI18N(ForgotPassword.class);
        String invalidError = i18n.getString("ForgotPassword.unableToSendEmail","Invalid Password Request.");
        String internError  = i18n.getString("ForgotPassword.internalError","Internal Error (contact system administrator).");

        /* invalid ContactEmail? */
        if (StringTools.isBlank(contactEmail)) {
            Print.logWarn("No Contact Email specified");
            return SECURE_RESPONSE? invalidError : i18n.getString("ForgotPassword.noContactEmailSpecified","No contact email specified.");
        }
        
        /* list of owned accounts */
        java.util.List<String> acctID = null;
        try {
            acctID = Account.getAccountIDsForContactEmail(contactEmail);
            if (ListTools.isEmpty(acctID)) {
                Print.logWarn("No Accounts owned by specified Contact Email");
                return SECURE_RESPONSE? invalidError : i18n.getString("ForgotPassword.noAccountsForContactEmail","No Account listed for this contact email.");
            }
        } catch (DBException dbe) {
            Print.logException("Error reading Account", dbe);
            return SECURE_RESPONSE? internError : i18n.getString("ForgotPassword.errorReadingAccount","Internal error reading Account.");
        }

        /* Subject/Body */
        String subj = i18n.getString("ForgotPassword.accountNameSubject","Account Name ...");
        StringBuffer body = new StringBuffer();
        if (ListTools.size(acctID) == 1) {
            body.append(i18n.getString("ForgotPassword.accountNameText","Here is the account name managed by your contact email address:"));
            body.append("\n");
        } else {
            body.append(i18n.getString("ForgotPassword.accountNamesText","Here are the account names managed by your contact email address:"));
            body.append("\n");
        }
        for (String A : acctID) { 
            body.append("   "+i18n.getString("ForgotPassword.account","Account:")+" ");
            body.append(A);
            body.append("\n");
        }
        body.append("\n");
        body.append(i18n.getString("ForgotPassword.doNotRespond","Please do not respond to this email."));
        body.append("\n");
        body.append(i18n.getString("ForgotPassword.pleaseDisregard","If you are not the intended recipient, please disregard this email."));
        body.append("\n");
        body.append("\n");
        body.append(i18n.getString("ForgotPassword.thankYou","Thank you."));
        body.append("\n");

        /* send */
        String from = privLabel.getEMailAddress(PrivateLabel.EMAIL_TYPE_PASSWORD);
        String to   = contactEmail;
        if (StringTools.isBlank(from)) {
            Print.logError("No 'From' email address specified");
            return SECURE_RESPONSE? internError : i18n.getString("ForgotPassword.missingFromAddress","Internal email configuration error ['From'].");
        } else
        if (StringTools.isBlank(to)) {
            Print.logError("No 'To' email address specified");
            return SECURE_RESPONSE? internError : i18n.getString("ForgotPassword.missingToAddress","Internal email configuration error ['To'].");
        } else {
            String cc   = null;
            String bcc  = null;
            boolean sent = EMail.send(from, to, cc, bcc, subj, body.toString());
            if (sent) {
                Print.logInfo("Password email sent");
                return null; // success
            } else {
                Print.logInfo("Password email not sent");
                return internError;
            }
        }

    }

    // ------------------------------------------------------------------------

    /* return the contact email for account/user */
    private static String getContactEMailAddress(String accountID, String userID)
    {

        /* invalid accountID? */
        if (StringTools.isBlank(accountID)) {
            Print.logWarn("No Account specified");
            return null;
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(accountID.trim());
            if (account == null) {
                Print.logWarn("Account doesn't exist: " + accountID);
                return null; // account does not exist
            }
        } catch (Throwable t) {
            Print.logWarn("Error reading Account: " + accountID);
            return null; // error retrieving account
        }

        /* get user */
        User user = null;
        if (!StringTools.isBlank(userID)) {
            try {
                user = User.getUser(account, userID.trim());
                if (user == null) {
                    Print.logWarn("User doesn't exist: " + userID);
                    return null; // user does not exist
                }
            } catch (Throwable t) {
                Print.logWarn("Error reading User: " + userID);
                return null; // error retrieving user
            }
        }

        /* return contact email address */
        return (user != null)? user.getContactEmail() : account.getContactEmail();

    }

    /* send password to contact email on file */
    private static String sendAccountPassword(
        PrivateLabel privLabel,
        String accountID,
        String userID,
        String contactEmail)
    {
        final I18N i18n = privLabel.getI18N(ForgotPassword.class);
        String invalidError = i18n.getString("ForgotPassword.unableToSendEmail","Invalid Password Request.");
        String internError  = i18n.getString("ForgotPassword.internalError","Internal Error (contact system administrator).");

        /* invalid ContactEmail? */
        if (StringTools.isBlank(contactEmail)) {
            Print.logWarn("No Contact Email specified");
            return i18n.getString("ForgotPassword.noContactEmailSpecified","No contact email specified.");
        }

        /* invalid accountID? */
        if (StringTools.isBlank(accountID)) {
            Print.logWarn("No Account specified");
            return i18n.getString("ForgotPassword.noAccountSPecified","No Account specified.");
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(accountID.trim());
            if (account == null) {
                Print.logWarn("Account doesn't exist: " + accountID);
                return SECURE_RESPONSE? invalidError : i18n.getString("ForgotPassword.accountNotExist","Specified Account does not exist.");
            }
        } catch (Throwable t) {
            Print.logWarn("Error reading Account: " + accountID);
            return SECURE_RESPONSE? internError : i18n.getString("ForgotPassword.errorReadingAccount","Internal error reading Account.");
        }

        /* get user */
        boolean hasUser = false;
        User user = null;
        if (!StringTools.isBlank(userID)) {
            try {
                user = User.getUser(account, userID.trim());
                if (user == null) {
                    Print.logWarn("User doesn't exist: " + userID);
                    return SECURE_RESPONSE? invalidError : i18n.getString("ForgotPassword.userNotExist","Specified User does not exist.");
                }
                hasUser = true;
            } catch (Throwable t) {
                Print.logWarn("Error reading User: " + userID);
                return SECURE_RESPONSE? internError : i18n.getString("ForgotPassword.errorReadingUser","Internal error reading User.");
            }
        }

        /* re-querying for password too soon since last query? */
        long passwdQueryTime = hasUser? user.getPasswdQueryTime() : account.getPasswdQueryTime();
        long deltaSinceLastQuery = DateTime.getCurrentTimeSec() - passwdQueryTime;
        if (deltaSinceLastQuery < MIN_PASS_QUERY_DELTA_SEC) {
            Print.logWarn("Too soon since last password query: " + accountID);
            return SECURE_RESPONSE? invalidError : i18n.getString("ForgotPassword.requestTooSoon","Too soon since last password request.");
        }

        /* contact email matches? */
        String emailAddress = hasUser? user.getContactEmail() : account.getContactEmail();
        if (StringTools.isBlank(emailAddress)) {
            Print.logWarn("No contact email address on file");
            return SECURE_RESPONSE? invalidError : i18n.getString("ForgotPassword.noContactEmailOnFile","No contact email address on file for this account.\\nPlease contact the system administrator for assistance");
        } else
        if (!contactEmail.equals(emailAddress)) {
            Print.logWarn("Invalid contact email address: " + contactEmail + " [" + emailAddress + "]");
            return SECURE_RESPONSE? invalidError : i18n.getString("ForgotPassword.invalidContactEmail","Specified contact email does not match email on file.");
        }

        /* get password */
        String decodedPass = hasUser? user.getDecodedPassword() : account.getDecodedPassword();
        if (decodedPass == null) {
            // password cannot be decoded, instead reset
            try {
                if (hasUser) {
                    decodedPass = user.resetPassword();
                    user.update(User.FLD_password);
                } else {
                    decodedPass = account.resetPassword();
                    account.update(Account.FLD_password);
                }
            } catch (DBException dbe) {
                // unable to save reset password
                decodedPass = "?"; 
            }
        }

        /* send password */
        String subj = hasUser? 
            i18n.getString("ForgotPassword.userLogin","User Login ...") : 
            i18n.getString("ForgotPassword.accountLogin","Account Login ...");
        String body = i18n.getString("ForgotPassword.emailBody",
            "Here is your requested password, please keep it in a safe place:\n"+
            "  Password: {0}\n"+
            "\n"+
            "Please do not respond to this email.\n"+
            "If you are not the intended recipient, Please disregard this email.\n"+
            "\n"+
            "Thank you.\n", decodedPass);
        //Print.logInfo("EMail body:\n" + body);
        String from = privLabel.getEMailAddress(PrivateLabel.EMAIL_TYPE_PASSWORD);
        String to   = emailAddress;
        if (StringTools.isBlank(from)) {
            Print.logError("No 'From' email address specified");
            return SECURE_RESPONSE? internError : i18n.getString("ForgotPassword.missingFromAddress","Internal email configuration error ['From'].");
        } else
        if (StringTools.isBlank(to)) {
            Print.logError("No 'To' email address specified");
            return SECURE_RESPONSE? internError : i18n.getString("ForgotPassword.missingToAddress","Internal email configuration error ['To'].");
        } else {
            String cc   = null;
            String bcc  = null;
            boolean sent = EMail.send(from, to, cc, bcc, subj, body);
            if (sent) {
                Print.logInfo("Password email sent");
                // reset password query time
                try {
                    if (hasUser) {
                        user.setPasswdQueryTime(DateTime.getCurrentTimeSec());
                        user.update(new String[] { User.FLD_passwdQueryTime });
                    } else {
                        account.setPasswdQueryTime(DateTime.getCurrentTimeSec());
                        account.update(new String[] { Account.FLD_passwdQueryTime });
                    }
                } catch (Throwable t) {
                    Print.logException("Updating LastPasswordQueryTime", t);
                }
                return null; // success
            } else {
                Print.logInfo("Password email not sent");
                return internError;
            }
        }


    }
        
    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(ForgotPassword.class);
        String m = "";
        boolean error = false;

        /* send account/password? */
        if (reqState.getCommandName().equals(COMMAND_EMAIL)) {
            HttpServletRequest request = reqState.getHttpServletRequest();
            String submit = AttributeTools.getRequestString(request, PARM_EMAIL_SUBMIT, "");
            if (SubmitMatch(submit,i18n.getString("ForgotPassword.submit","Submit"))) {
                String accountID = AttributeTools.getRequestString(request, PARM_EMAIL_ACCOUNT,"");
                String userID    = AttributeTools.getRequestString(request, PARM_EMAIL_USER   ,"");
                String emailAddr = AttributeTools.getRequestString(request, PARM_EMAIL_ADDRESS,"");
                if (StringTools.isBlank(emailAddr)) {
                    m = i18n.getString("ForgotPassword.enterEMail","Please enter your contact email address");
                    error = true;
                } else 
                if (!EMail.validateAddress(emailAddr)) {
                    m = i18n.getString("ForgotPassword.invalidEMail","An invalid email address was entered");
                    error = true;
                } else
                if (StringTools.isBlank(accountID)) {
                    if (SEND_ACCOUNT_LIST) {
                        String errMsg = sendAccountsForContactEMail(privLabel, EMail.getEMailAddress(emailAddr));
                        if (!StringTools.isBlank(errMsg)) {
                            //Track.writeMessageResponse(reqState, 
                            m = i18n.getString("ForgotPassword.unableToSendAccountEmail","Unable to send account list email for the following reason:") + "\n" + errMsg;
                            error = true;
                        } else {
                            Track.writeMessageResponse(reqState, 
                                i18n.getString("ForgotPassword.sentAccounts","The Account list has been sent to the contact email on file."));
                            return;
                        }
                    } else {
                        m = i18n.getString("ForgotPassword.enterAccount","Please enter an Account ID");
                        error = true;
                    }
                 } else {
                    String errMsg = sendAccountPassword(privLabel, accountID, userID, EMail.getEMailAddress(emailAddr));
                    if (!StringTools.isBlank(errMsg)) {
                        //Track.writeMessageResponse(reqState, 
                        m = i18n.getString("ForgotPassword.unableToSendPasswordEmail","Unable to send password email for the following reason:") + "\n" + errMsg;
                        error = true;
                    } else {
                        Track.writeMessageResponse(reqState, 
                            i18n.getString("ForgotPassword.sentPassword","The password has been sent to the contact email on file."));
                        return;
                    }
                }
            } else {
                m = i18n.getString("ForgotPassword.cancelled","Cancelled");
            }
        }
        Print.logWarn(m);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = ForgotPassword.this.getCssDirectory(); 
                WebPageAdaptor.writeCssLink(out, reqState, "ForgotPassword.css", cssDir);
            }
        };

        /* write frame */
        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_FORGOT_PASSWORD, m) {
            public void write(PrintWriter out) throws IOException {
                String menuURL   = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String emailURL  = privLabel.getWebPageURL(reqState, PAGE_PASSWD_EMAIL, COMMAND_EMAIL);
                boolean usrLogin = privLabel.getUserLogin();
                out.println("<span style='font-size:10pt; margin-bottom:7px;'>"+i18n.getString("ForgotPassword.instructions",
                              "To have your password sent to you, please enter your "+
                              "Login information and Contact Email Address "+
                              "(must match email address on file):") +
                            "</span>");
                out.println("<hr/>");
                out.println("<form name='Passwd_Email' method='post' action='"+emailURL+"' target='_top'>");
                out.println("<table>");
                out.println("  <tr><td>"+i18n.getString("ForgotPassword.accountID","Account ID:")+"</td><td><input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_EMAIL_ACCOUNT+"' value='' size='24' maxlength='32'></td></tr>");
                if (usrLogin) {
                    out.println("  <tr><td>"+i18n.getString("ForgotPassword.userID","User ID:")+"</td><td><input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_EMAIL_USER+"' value='' size='24' maxlength='32'></td></tr>");
                }
                out.println("  <tr><td>"+i18n.getString("ForgotPassword.contactEMail","Contact Email:")+"</td><td><input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_EMAIL_ADDRESS+"' value='' size='40' maxlength='64'></td></tr>");
                out.println("</table>");
                out.println("<input type='submit' name='"+PARM_EMAIL_SUBMIT+"' value='"+i18n.getString("ForgotPassword.submit","Submit")+"'>");
                out.println("</form>");
                out.println("<hr>");
                if (SEND_ACCOUNT_LIST) {
                    out.println("<span style='font-size:8pt'>"+i18n.getString("ForgotPassword.accountHelp1","To have a list of your managed accounts emailed to you,")+"<br>");
                    out.println(i18n.getString("ForgotPassword.accountHelp2","leave the 'Account ID' field blank.")+"</span>");
                    out.println("<hr>");
                }
                out.println("<a href='"+menuURL+"'>"+i18n.getString("ForgotPassword.cancel","Cancel")+"</a>");
            }
        };

        /* write frame */
        String onload = error? JS_alert(true,m) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTMLOutput.NOOP,            // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

}
