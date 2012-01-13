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
// Description:
//  Outbound SMS Gateway support
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/18  Martin D. Flynn
//     -Initial release
//  2010/11/29  Martin D. Flynn
//     -Added "httpURL" format
//  2011/03/08  Martin D. Flynn
//     -Changed "getStringProperty" to first look at the account BasicPrivateLabel
//  2011/05/13  Martin D. Flynn
//     -Look for additional replacement vars "${var}", "%{var}", and "{VAR}",
//      where "var" is "mobile", "message", and "sender".
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

/**
*** Outbound SMS gateway handler
**/
public abstract class SMSOutboundGateway
{

    // ------------------------------------------------------------------------

    public  static final String SMS_Prefix                          = "SMS:";
    public static boolean StartsWithSMS(String val)
    {
        return StringTools.startsWithIgnoreCase(val,SMS_Prefix);
    }

    // ------------------------------------------------------------------------

    public static int getMaximumTextMessageLength()
    {
        return 160;
    }
    
    // ------------------------------------------------------------------------

    public  static final String REPL_mobile[]                       = new String[] { "${mobile}" , "%{mobile}" , "{MOBILE}"  };
    public  static final String REPL_message[]                      = new String[] { "${message}", "%{message}", "{MESSAGE}" };
    public  static final String REPL_sender[]                       = new String[] { "${sender}" , "%{sender}" , "{SENDER}"  };
    
    public static String REPLACE(String s, String r[], String m)
    {
        for (int i = 0; i < r.length; i++) {
            s = StringTools.replace(s, r[i], m);
        }
        return s;
    }

    // ------------------------------------------------------------------------

    private static final String PROP_SmsGatewayHandler_             = "SmsGatewayHandler.";
    public  static final String PROP_defaultName                    = PROP_SmsGatewayHandler_ + "defaultName";

    /* emailBody */
    public  static final String GW_emailBody                        = "emailBody";
    public  static final String PROP_emailBody_smsEmailAddress      = PROP_SmsGatewayHandler_ + "emailBody.smsEmailAddress";

    /* emailSubject */
    public  static final String GW_emailSubject                     = "emailSubject";
    public  static final String PROP_emailSubject_smsEmailAddress   = PROP_SmsGatewayHandler_ + "emailSubject.smsEmailAddress";

    /* httpURL */
    public  static final String GW_httpURL                          = "httpURL";
    public  static final String PROP_httpURL_url                    = PROP_SmsGatewayHandler_ + "httpURL.url";

    /* Click-A-Tell */
    public  static final String GW_clickatell                       = "clickatell";
    public  static final String PROP_clickatell_smsEmailAddress     = PROP_SmsGatewayHandler_ + "clickatell.smsEmailAddress";
    public  static final String PROP_clickatell_user                = PROP_SmsGatewayHandler_ + "clickatell.user";
    public  static final String PROP_clickatell_password            = PROP_SmsGatewayHandler_ + "clickatell.password";
    public  static final String PROP_clickatell_api_id              = PROP_SmsGatewayHandler_ + "clickatell.api_id";

    /* TextAnywhere: mail2txt, mail2txt160, mail2txtid, mail2txt160id */
    public  static final String GW_mail2txt                         = "mail2txt";
    public  static final String PROP_mail2txt_smsEmailAddress       = PROP_SmsGatewayHandler_ + "mail2txt.smsEmailAddress";
    public  static final String GW_mail2txt160                      = "mail2txt160";
    public  static final String PROP_mail2txt160_smsEmailAddress    = PROP_SmsGatewayHandler_ + "mail2txt160.smsEmailAddress";
    public  static final String GW_mail2txtid                       = "mail2txtid";
    public  static final String PROP_mail2txtid_smsEmailAddress     = PROP_SmsGatewayHandler_ + "mail2txtid.smsEmailAddress";
    public  static final String PROP_mail2txtid_from                = PROP_SmsGatewayHandler_ + "mail2txtid.from";
    public  static final String GW_mail2txt160id                    = "mail2txt160id";
    public  static final String PROP_mail2txt160id_smsEmailAddress  = PROP_SmsGatewayHandler_ + "mail2txt160id.smsEmailAddress";
    public  static final String PROP_mail2txt160id_from             = PROP_SmsGatewayHandler_ + "mail2txt160id.from";

    /* ozekisms */
    public  static final String GW_ozekisms                         = "ozekisms";
    public  static final String PROP_ozekisms_hostPort              = PROP_SmsGatewayHandler_ + "ozekisms.hostPort";
    public  static final String PROP_ozekisms_originator            = PROP_SmsGatewayHandler_ + "ozekisms.originator";
    public  static final String PROP_ozekisms_user                  = PROP_SmsGatewayHandler_ + "ozekisms.user";
    public  static final String PROP_ozekisms_password              = PROP_SmsGatewayHandler_ + "ozekisms.password";

    // ------------------------------------------------------------------------

    public static String GetDefaultGatewayName()
    {
        return RTConfig.getString(PROP_defaultName,GW_emailBody);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Map<String,SMSOutboundGateway> SmsGatewayHandlerMap = null;

    /**
    *** Add SMS Gateway support provider
    **/
    public static void AddSMSGateway(String name, SMSOutboundGateway smsGW)
    {

        /* validate name */
        if (StringTools.isBlank(name)) {
            Print.logWarn("SMS Gateway name is blank");
            return;
        } else
        if (smsGW == null) {
            Print.logWarn("SMS Gateway handler is null");
            return;
        }
        
        /* initialize map? */
        if (SmsGatewayHandlerMap == null) { 
            SmsGatewayHandlerMap = new HashMap<String,SMSOutboundGateway>(); 
        }

        /* save handler */
        SmsGatewayHandlerMap.put(name.toLowerCase(), smsGW);
        Print.logDebug("Added SMS Gateway Handler: " + name);

    }

    /**
    *** Gets the SMSoutboubdGateway for the specified name
    **/
    public static SMSOutboundGateway GetSMSGateway(String name)
    {

        /* get handler */
        if (StringTools.isBlank(name)) {
            return null;
        } else {
            if (name.equalsIgnoreCase("body")) { 
                name = GW_emailBody; 
            } else
            if (name.equalsIgnoreCase("subject")) { 
                name = GW_emailSubject; 
            }
            return SmsGatewayHandlerMap.get(name.toLowerCase());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initialize outbound SMS gateway handlers
    **/
    public static void _startupInit()
    {
        Print.logDebug("SMSOutboundGateway initializing ...");

        /* already initialized? */
        if (SmsGatewayHandlerMap != null) {
            return;
        }

        // -----------------------------------------------
        // The following shows several example of outbound SMS gateway support.
        // The only method that needs to be overridden and implemented is
        //   public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr)
        // The "device" is the Device record instance to which the SMS message should be sent,
        // and "commandStr" is the SMS text (device command) which is to be sent to the device.
        // -----------------------------------------------

        /* standard "Body" command */
        // Property:
        //   SmsGatewayHandler.emailBody.smsEmailAddress=@example.com
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message body to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailBody.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailBody, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                String frEmail = this.getFromEmailAddress(device);
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getStringProperty(device,PROP_emailBody_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, commandStr);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String frEmail  = this.getFromEmailAddress(account);
                String smsEmail = RTConfig.getString(PROP_emailBody_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, smsMessage);
            }
        });

        /* standard "Subject" command */
        // Property:
        //   SmsGatewayHandler.emailSubject.smsEmailAddress=
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message subject to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailSubject.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailSubject, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                String frEmail = this.getFromEmailAddress(device);
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getStringProperty(device,PROP_emailSubject_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                return this.sendEmail(frEmail, toEmail, commandStr, ""/*body*/);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String frEmail  = this.getFromEmailAddress(account);
                String smsEmail = RTConfig.getString(PROP_emailSubject_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                return this.sendEmail(frEmail, toEmail, smsMessage, ""/*body*/);
            }
        });

        /* HTTP SMS */
        // Property:
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/smsredirector/sendsms?flash=0&acctuser=user&tracking_Pwd=pass&source=5551212&destination=${mobile}&message=${message}
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/sendsms?user=user&pass=pass&source=${sender}&dest=${mobile}&text=${message}
        // Notes:
        //   This outbound SMS method sends the SMS text in an HTTP "GET" request to the URL 
        //   specified on the property "SmsGatewayHandler.httpURL.url".  The following replacement
        //   variables may be specified in the URL string:
        //      ${sender}  - replaced with the Account "contactPhone" field contents
        //      ${mobile}  - replaced with the Device "simPhoneNumber" field contents
        //      ${message} - replaced with the SMS text/command to be sent to the device.
        //   It is expected that the server handling the request understands how to parse and
        //   interpret the various fields in the URL.
        SMSOutboundGateway.AddSMSGateway(GW_httpURL, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                Account account = device.getAccount();
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                //Print.logDebug("SMS httpURL 1) ["+device.getAccountID()+"/"+device.getDeviceID()+"]" +
                //    " mobile=" + device.getSimPhoneNumber() +
                //    " message=" + commandStr +
                //    " sender=" + account.getContactPhone());
                String mobile   = URIArg.encodeArg(device.getSimPhoneNumber());
                String message  = URIArg.encodeArg(commandStr);
                String sender   = URIArg.encodeArg(account.getContactPhone());
                //Print.logDebug("SMS httpURL 2) ["+device.getAccountID()+"/"+device.getDeviceID()+"]" +
                //    " mobile=" + mobile +
                //    " message=" + message +
                //    " sender=" + sender);
                String httpURL  = this.getStringProperty(device, PROP_httpURL_url, "");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                httpURL = REPLACE(httpURL, REPL_mobile , mobile );
                httpURL = REPLACE(httpURL, REPL_message, message);
                httpURL = REPLACE(httpURL, REPL_sender , sender );
                try {
                    Print.logInfo("SMS Gateway URL: " + httpURL);
                    byte response[] = HTMLTools.readPage_GET(httpURL, 10000);
                    // TODO: check response?
                    return DCServerFactory.ResultCode.SUCCESS;
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                String mobile   = URIArg.encodeArg(smsPhone);
                String message  = URIArg.encodeArg(smsMessage);
                String sender   = URIArg.encodeArg(account.getContactPhone());
                String httpURL  = RTConfig.getString(PROP_httpURL_url, "");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                httpURL = REPLACE(httpURL, REPL_mobile , mobile );
                httpURL = REPLACE(httpURL, REPL_message, message);
                httpURL = REPLACE(httpURL, REPL_sender , sender );
                try {
                    Print.logInfo("SMS Gateway URL: " + httpURL);
                    byte response[] = HTMLTools.readPage_GET(httpURL, 10000);
                    // TODO: check response?
                    return DCServerFactory.ResultCode.SUCCESS;
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
        });

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public SMSOutboundGateway()
    {
        // override
    }

    // ------------------------------------------------------------------------

    public abstract DCServerFactory.ResultCode sendSMSCommand(Device device, String command);
    public abstract DCServerFactory.ResultCode sendSMSMessage(Account account, String smsMessage, String smsPhone);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected String getFromEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(device.getAccount());
    }

    protected String getFromEmailAddress(Account account) 
    {
        if (account == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(account);
    }
    
    // ------------------------------------------------------------------------

    protected String getSmsEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        String toEmail = device.getSmsEmail();
        return toEmail;
    }

    protected String getSmsPhoneNumber(Device device) 
    {
        if (device == null) { return null; }
        String smsPhone = device.getSimPhoneNumber();
        return smsPhone;
    }

    // ------------------------------------------------------------------------

    protected String getStringProperty(Device device, String key, String dft) 
    {

        /* 1) check the BasicPrivateLabel properties */
        Account account = (device != null)? device.getAccount() : null;
        BasicPrivateLabel bpl = Account.getPrivateLabel(account);
        if (bpl != null) {
            String prop = bpl.getStringProperty(key,null);
            if (prop != null) {
                return prop;
            }
        }

        /* 2) check the DCServerConfig properties */
        DCServerConfig dcs = (device != null)? DCServerFactory.getServerConfig(device.getDeviceCode()) : null;
        if (dcs != null) {
            String prop = dcs.getStringProperty(key, dft); // will already check RTConfig properties
            Print.logInfo("DCServerConfig property '"+key+"' ==> " + prop);
            if (StringTools.isBlank(prop) && RTConfig.hasProperty(key)) {
                Print.logInfo("(RTConfig property '"+key+"' ==> " + RTConfig.getString(key,"") + ")");
            }
            return prop;
        }

        /* 3) check RTConfig properties */
        String prop = RTConfig.getString(key, dft);
        Print.logInfo("RTConfig property '"+key+"' ==> " + prop);
        return prop;

    }

    // ------------------------------------------------------------------------

    protected DCServerFactory.ResultCode sendEmail(String frEmail, String toEmail, String subj, String body) 
    {
        if (StringTools.isBlank(frEmail)) {
            Print.logError("'From' SMS Email address not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_FR;
        } else
        if (StringTools.isBlank(toEmail) || !CommandPacketHandler.validateAddress(toEmail)) {
            Print.logError("'To' SMS Email address invalid, or not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_TO;
        } else
        if (StringTools.isBlank(subj) && StringTools.isBlank(body)) {
            Print.logError("SMS Subject/Body string not specified");
            return DCServerFactory.ResultCode.INVALID_ARG;
        } else {
            try {
                Print.logInfo ("SMS email: to <" + toEmail + ">");
                Print.logDebug("  From   : " + frEmail);
                Print.logDebug("  To     : " + toEmail);
                Print.logDebug("  Subject: " + subj);
                Print.logDebug("  Message: " + body);
                SendMail.send(frEmail, toEmail, null, null, subj, body, null);
                return DCServerFactory.ResultCode.SUCCESS;
            } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                // this will fail if JavaMail support for SendMail is not available.
                Print.logWarn("SendMail error: " + t);
                return DCServerFactory.ResultCode.TRANSMIT_FAIL;
            }
        }
    }
    
    // ------------------------------------------------------------------------

}
