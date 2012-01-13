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
//  2007/05/06  Martin D. Flynn
//     -Added note about leaving the userID blank.
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -The current "<PrivateLabel>.getDomainName()" name is now used to set the 
//      temporary Account 'privateLabelName' field (previously it was left blank).
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

public class NewAccount
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    /* set this value to 'true' to always display the "New accounts are offline" message */
    public  static      boolean OFFLINE                     = false;

    // ------------------------------------------------------------------------
    
    public  static final String COMMAND_EMAIL_SUBMIT        = "e_submit";

    public  static final String PARM_EMAIL_SUBMIT           = "e_submit";

    public  static final String PARM_CONTACT_EMAIL          = "e_addr";
    public  static final String PARM_CONTACT_NAME           = "e_name";
    public  static final String PARM_AUTH_CODE              = "e_auth";

    public  static final String CSS_NEW_ACCOUNT[]           = new String[] { "newAccountTable", "newAccountCell" };
    public  static final String CSS_NEW_ACCOUNT_TITLE       = "newAccountTitle";
    public  static final String CSS_NEW_ACCOUNT_OFFLINE     = "newAccountOffline";
    public  static final String CSS_NEW_ACCOUNT_INSTRUCT    = "newAccountInstructions";
    public  static final String CSS_NEW_ACCOUNT_EXPIRE      = "newAccountExpire";

    // ------------------------------------------------------------------------

    public NewAccount()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_ACCOUNT_NEW);
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
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getMenuDescription(reqState,"");
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getMenuHelp(reqState,"");
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getNavigationDescription(reqState,i18n.getString("NewAccount.navDesc","New Account"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getNavigationTab(reqState,i18n.getString("NewAccount.navTab","New Account"));
    }

    // ------------------------------------------------------------------------
    
    private void offline(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(NewAccount.class);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                WebPageAdaptor.writeCssLink(out, reqState, "NewAccount.css", null);
            }
        };

        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_NEW_ACCOUNT, pageMsg) {
            public void write(PrintWriter out) throws IOException {
                out.println("<span class='"+CSS_NEW_ACCOUNT_TITLE+"'>"+i18n.getString("NewAccount.newTempAccountOffline","New Temporary Account")+"</span>");
                out.println("<hr>");
                out.println("<span class='"+CSS_NEW_ACCOUNT_OFFLINE+"'>"+i18n.getString("NewAccount.offline1","New account registration is temporarily offline,")+"<br>"+i18n.getString("NewAccount.offline2","Please check back soon.")+"</span>");
                out.println("<hr>");
                String baseURL = Track.GetBaseURL(reqState); // EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI());
                out.println("<a href='"+baseURL+"'>Back</a>");
            }
        };

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            null,null,                      // onLoad/onUnload
            HTML_CSS,                       // Style sheets
            HTMLOutput.NOOP,                // JavaScript
            null,                           // Navigation
            HTML_CONTENT);                  // Content

    }

    // ------------------------------------------------------------------------

    private boolean isAuthCodeRequired(PrivateLabel privLabel)
    {
        String acm = privLabel.getStringProperty(PrivateLabel.PROP_NewAccount_authCodeMask,null);
        return !StringTools.isBlank(acm);
    }
    
    private Tuple.Pair<Integer,String> checkAuthorization(PrivateLabel privLabel, 
        String name, String email, String auth)
    {

        /* authorization required? */
        byte acm[] = null;
        try {
            acm = Base64.decode(privLabel.getStringProperty(PrivateLabel.PROP_NewAccount_authCodeMask,null));
            if (ListTools.isEmpty(acm)) {
                Print.logInfo("No authorization code required: " + name);
                return new Tuple.Pair<Integer,String>(0,null); // no auth code required
            }
        } catch (Base64.Base64DecodeException bde) {
            Print.logException("Invalid encoded authCodeMask characters", bde);
            return null; // error
        }

        /* user entered authorization */
        byte uac[] = null;
        try {
            uac = Base64.decode(auth);
            if (ListTools.isEmpty(uac) || (uac.length < 8)) {
                Print.logInfo("Authorization code not specified: " + name);
                return null; // invalid specified auth code
            }
        } catch (Base64.Base64DecodeException bde) {
            Print.logException("Invalid encoded userAuth characters", bde);
            return null; // error
        }

        /* de-obfuscate */
        int a = 0;
        for (int u = 0; u < uac.length; u++) {
            uac[u] = (byte)(uac[u] ^ acm[a++ % acm.length]);
        }

        /* checksum */
        int csSpec = (((int)uac[uac.length - 2] & 0xFF) << 8) | ((int)uac[uac.length - 1] & 0xFF);
        int csCalc = Checksum.calcCrcCCITT(uac, uac.length - 2);
        if (csSpec != csCalc) {
            Print.logInfo("Invalid checksum: " + name);
            return null; // invalid checksum (invalid specified auth code)
        }

        /* extract */
        Payload p = new Payload(uac);
        p.readSkip(2);
        int expDays = p.readInt(1,-1);
        p.readSkip(2);
        String acctID = p.readString(20,true);
        Print.logInfo("New Account authorized: " + name + " ("+expDays + ",\"" + acctID + "\")");
        return new Tuple.Pair<Integer,String>(expDays,acctID); // success

    }

    // ------------------------------------------------------------------------

    private void createNewAccount(
        String tempAccountID, int tempExpireDays,
        String contactEmail, String contactName,
        RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(NewAccount.class);
        
        Print.logInfo("EMail address submitted: '" + contactName + "' : " + contactEmail);
        Account account = null;
        Device device = null;
        try {

            /* already assigned? */
            java.util.List<String> existingAcct = Account.getAccountIDsForContactEmail(contactEmail);
            if (!ListTools.isEmpty(existingAcct)) {
                Track.writeErrorResponse(reqState, i18n.getString("NewAccount.alreadyAccount",
                    "An account is already assigned to this Contact EMail Address."));
                return;
            }

            /* specified account already exists? */
            if (!StringTools.isBlank(tempAccountID)) {
                Account.Key acctKey = new Account.Key(tempAccountID);
                if (acctKey.exists()) { // may throw DBException
                    Track.writeErrorResponse(reqState, i18n.getString("NewAccount.accountExists",
                        "An account has already been created for this authorization code."));
                    return;
                }
            }

            /* create account */
            String tempPrivateLabelName = privLabel.getDomainName();
            account = Account.createTemporaryAccount(
                tempAccountID, tempExpireDays,
                contactName, contactEmail, 
                tempPrivateLabelName);
            if (account == null) {
                // unable to assign an account
                this.offline(reqState, pageMsg);
                return;
            }

            /* create device */
            String deviceID = "mobile";
            device = Device.getDevice(account, deviceID, true);
            device.setIsActive(true);
            device.setDescription("Mobile Device");
            device.save();

        } catch (DBException dbe) {

            Print.logException("Creating Account", dbe);
            Track.writeErrorResponse(reqState, i18n.getString("NewAccount.accountError","Internal error occurred while creating account.  Try again later."));
            return;

        }

        /* send new account info */
        // Hello Mr User,
        //
        // Your new temporary account has been created.
        // Your access information is as follows:
        //    AccountID: T999999
        //    Password : vJcKFSbM
        //    DeviceID : mobile
        //
        // Please note that this is a temporary account to be used only for testing and
        // debug purposes.   This account is due to expire on 2006/06/28 08:50:15 GMT,
        // after which time the account and data will no longer be available.   Also note
        // that this free service may become unavailable from time to time and may be
        // discontinued at any time without advance notice.
        // 
        // You must login within the next 6 hours to confirm your new account registration.
        // You will then be able to change your password, and other account information.
        // 
        // Thank you.
        //
        String expd = reqState.formatDateTime(account.getExpirationTime());
        if (StringTools.isBlank(expd)) { expd = "n/a"; }

        String subj = i18n.getString("NewAccount.newAccount", "New Account");
        String body = i18n.getString("NewAccount.emailBody",
            "Hello {0},\n" +
            "\n" +
            "Your new temporary account has been created.\n" +
            "Your access information is as follows:\n" +
            "   AccountID: {1}\n" +
            "   UserID   : (leave blank)\n" +
            "   Password : {2}\n" +
            "   DeviceID : {3}\n" +
            "\n" +
            "Please note that this is a temporary account to be used only for testing and\n" +
            "debug purposes.   This account is due to expire on {4},\n" +
            "after which time the account and data will no longer be available.   Also note\n" +
            "that this free service may become unavailable from time to time and may be\n" +
            "discontinued at any time without advance notice.\n" +
            "\n" +
            "You must login within the next 6 hours to confirm your new account registration.\n" +
            "You will then be able to change your password, and other account information.\n" +
            "\n" +
            "Thank you.\n",
            new Object[] {
                /*{0}*/ contactName,
                /*{1}*/ account.getAccountID(),
                /*{2}*/ account.getDecodedPassword(),
                /*{3}*/ device.getDeviceID(),
                /*{4}*/ expd
            });
        //Print.logInfo("EMail body:\n" + body);
        String from = privLabel.getEMailAddress(PrivateLabel.EMAIL_TYPE_ACCOUNTS);
        String to   = account.getContactEmail();
        if (!StringTools.isBlank(from) && !StringTools.isBlank(to)) {
            String cc   = null;
            String bcc  = null;
            EMail.send(from, to, cc, bcc, subj, body);
            Track.writeMessageResponse(reqState, 
                i18n.getString("NewAccount.emailSent","An email was sent to the specified email address with your new account information."));
        } else {
            Track.writeMessageResponse(reqState, 
                i18n.getString("NewAccount.emailError","Due to an internal error, we were unable to email your new account information."));
        }

    }
    
    // ------------------------------------------------------------------------
        
    public void writePage(
        final RequestProperties reqState, 
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(NewAccount.class);
        String m = pageMsg;
        
        /* offline */
        if (OFFLINE) {
            this.offline(reqState, pageMsg);
            return;
        }

        /* submitted? */
        String email = "";
        String name  = "";
        String auth  = "";
        if (reqState.getCommandName().equals(COMMAND_EMAIL_SUBMIT)) {
            HttpServletRequest request = reqState.getHttpServletRequest();
            String submitSend = AttributeTools.getRequestString(request, PARM_EMAIL_SUBMIT, "");
            if (SubmitMatch(submitSend,i18n.getString("NewAccount.submit","Submit"))) {
                name  = AttributeTools.getRequestString(request, PARM_CONTACT_NAME ,"").trim();
                email = AttributeTools.getRequestString(request, PARM_CONTACT_EMAIL,"").trim();
                auth  = AttributeTools.getRequestString(request, PARM_AUTH_CODE    ,"").trim();
                if (StringTools.isBlank(name)) {
                    m = i18n.getString("NewAccount.pleaseEnterName","Please enter a valid name");
                    name = "";
                } else
                if (StringTools.isBlank(email) || !EMail.validateAddress(email)) {
                    m = i18n.getString("NewAccount.pleaseEnterEMail","Please enter a valid email address");
                    email = "";
                } else {
                    Tuple.Pair<Integer,String> a = this.checkAuthorization(privLabel,name,email,auth);
                    if (a == null) {
                        m = i18n.getString("NewAccount.pleaseEnterAuth","Please enter a valid authorization code");
                        auth = "";
                    } else {
                        int    tempExpDays = a.a.intValue();
                        String tempAcctID  = a.b;
                        this.createNewAccount(
                            tempAcctID, tempExpDays,
                            EMail.getEMailAddress(email), name, 
                            reqState, 
                            pageMsg);
                        return;
                    }
                }
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = NewAccount.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "NewAccount.css", cssDir);
            }
        };

        /* write frame */
        final String cn  = name;
        final String ce  = email;
        final String ac  = "";
        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_NEW_ACCOUNT, m) {
            public void write(PrintWriter out) throws IOException {
              //String menuURL    = EncodeMakeURL(reqState, RequestProperties.TRACK_BASE_URI(), PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String emailURL   = NewAccount.this.encodePageURL(reqState, COMMAND_EMAIL_SUBMIT);
                //String expireDate = reqState.formatDateTime(DateTime.getCurrentTimeSec() + Account.DFT_EXPIRATION_SEC);
                //if (StringTools.isBlank(expireDate)) { expireDate = "n/a"; }
                out.println("<form name='AccountInfo' method='post' action='"+emailURL+"' target='_self'>"); // target='_top'
                out.println("  <span class='"+CSS_NEW_ACCOUNT_TITLE+"'>"+i18n.getString("NewAccount.newTempAccount","New Temporary Account")+"</span>");
                out.println("  <hr>");
                out.println("  <span class='"+CSS_NEW_ACCOUNT_INSTRUCT+"'>");
                out.println(StringTools.replace(i18n.getString("NewAccount.instructions",
                    "To create a temporary account, enter your contact information below\n" + 
                    "(and authorization code if required).\n" +
                    "Login information will be sent to you via email."),"\n","<br>"));
                out.println("  </span>");
                out.println("  <hr>");

                out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                out.println(FormRow_TextField(PARM_CONTACT_NAME , true, i18n.getString("NewAccount.enterYourName" ,"Your Name")+":"         , cn, 30, 30));
                out.println(FormRow_TextField(PARM_CONTACT_EMAIL, true, i18n.getString("NewAccount.enterYourEMail","Your Email Address")+":", ce, 36, 36));
                if (NewAccount.this.isAuthCodeRequired(privLabel)) {
                out.println(FormRow_TextField(PARM_AUTH_CODE    , true, i18n.getString("NewAccount.enterAuthCode" ,"Authorization Code (required)")+":", ac, 40, 40));
                }
                out.println("</table>");

                /*
                out.println("  <span style='font-size:9pt'>"+i18n.getString("NewAccount.enterYourName","Your Name")+":</span><br>");
                out.println("    <input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_CONTACT_NAME +"' value='"+cn+"' maxlength='30' size='30'><br>");
                out.println("  <span style='font-size:9pt'>"+i18n.getString("NewAccount.enterYourEMail","Your Email Address")+":</span><br>");
                out.println("    <input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_CONTACT_EMAIL+"' value='"+ce+"' maxlength='36' size='36'><br>");
                if (NewAccount.this.isAuthCodeRequired(privLabel)) {
                out.println("  <span style='font-size:9pt'>"+i18n.getString("NewAccount.enterAuthCode","Authorization Code (required)")+":</span><br>");
                out.println("    <input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_AUTH_CODE+"' value='"+ac+"' maxlength='40' size='40'><br>");
                }
                */

                out.println("  <input type='submit' name='"+PARM_EMAIL_SUBMIT+"' value='"+i18n.getString("NewAccount.submit","Submit")+"'><br>");
                out.println("</form>");
                out.println("<hr>");
                out.println("<span class='"+CSS_NEW_ACCOUNT_EXPIRE+"'>"+i18n.getString("NewAccount.willExpire","Temporary accounts are temporary, and do have an expiry date.")+"<br>");
                //out.println(i18n.getString("NewAccount.expireOnDate","Accounts created now will expire {0}",expireDate)+"</span>");
                out.println("<hr>");
                out.println("<a href='"+menuURL+"'>"+i18n.getString("NewAccount.back","Back")+"</a>");
            }
        };

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            null,null,                  // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTMLOutput.NOOP,            // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------
    
    private static final String ARG_PRIVLABEL[]     = new String[] { "bpl", "pl" };
    private static final String ARG_ACCOUNT[]       = new String[] { "account", "acct", "a" };
    private static final String ARG_EXPIRE[]        = new String[] { "expire", "exp", "e" };
    private static final String ARG_DECODE[]        = new String[] { "decode" };

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main

        /* PrivateLabel */
        String bplName = RTConfig.getString(ARG_PRIVLABEL,null);
        BasicPrivateLabel bpl = BasicPrivateLabelLoader.getPrivateLabel(bplName);
        if (bpl == null) {
            Print.sysPrintln("ERROR: PrivateLabel name not found: " + bplName);
            System.exit(99);
        }

        /* auth mask */
        byte acm[] = null;
        try {
            acm = Base64.decode(bpl.getStringProperty(PrivateLabel.PROP_NewAccount_authCodeMask,null));
            if (ListTools.isEmpty(acm)) {
                Print.sysPrintln("No authorization code required");
                System.exit(0);
            }
        } catch (Base64.Base64DecodeException bde) {
            Print.logException("Invalid authorization code characters", bde);
            System.exit(99);
        }

        /* decode? */
        if (RTConfig.hasProperty(ARG_DECODE)) {

            /* get auth code bytes */
            String auth = RTConfig.getString(ARG_DECODE,"");
            byte uac[] = null;
            try {
                uac = Base64.decode(auth);
                if (ListTools.isEmpty(uac) || (uac.length < 8)) {
                    Print.sysPrintln("Invalid authorization code");
                    System.exit(99);
                }
            } catch (Base64.Base64DecodeException bde) {
                Print.logException("Invalid decode code characters", bde);
                System.exit(99);
            }

            /* de-obfuscate */
            //Print.sysPrintln("UAC before = " + StringTools.toStringValue(uac,'.'));
            int a = 0;
            for (int u = 0; u < uac.length; u++) {
                uac[u] = (byte)(uac[u] ^ acm[a++ % acm.length]);
            }
            //Print.sysPrintln("UAC after  = " + StringTools.toStringValue(uac,'.'));

            /* checksum */
            int csSpec = (((int)uac[uac.length - 2] & 0xFF) << 8) | ((int)uac[uac.length - 1] & 0xFF);
            int csCalc = Checksum.calcCrcCCITT(uac, uac.length - 2);
            if (csSpec != csCalc) {
                Print.sysPrintln("Invalid checksum");
                System.exit(99);
            }

            /* extract */
            Payload p = new Payload(uac);
            p.readSkip(2);
            int expDays = p.readInt(1,-1);
            p.readSkip(2);
            String acctID = p.readString(20,true);
            Print.sysPrintln("Auth Contents: expireDays="+expDays + " account=" + acctID + "");

        } else {

            /* expire/account */
            int   expireDays = RTConfig.getInt(ARG_EXPIRE, -1);
            String accountID = RTConfig.getString(ARG_ACCOUNT, "");

            /* encode payload */
            Random ran = new Random();
            Payload p = new Payload();
            p.writeInt(ran.nextInt(), 2);
            p.writeInt(expireDays   , 1);
            p.writeInt(ran.nextInt(), 2);
            p.writeString(accountID, 20, true);
            while (p.getSize() < 18) { p.writeLong(ran.nextInt(), 1); }
            int csCalc = Checksum.calcCrcCCITT(p.getBytes(), p.getSize());
            p.writeULong(csCalc, 2);
            byte uac[] = p.getBytes();

            /* obfuscate */
            int a = 0;
            for (int u = 0; u < uac.length; u++) {
                uac[u] = (byte)(uac[u] ^ acm[a++ % acm.length]);
            }
            String uac64 = StringTools.stripChars(Base64.encode(uac),'=');

            /* print */
            Print.sysPrintln(uac64);

        }

    }
    
}
