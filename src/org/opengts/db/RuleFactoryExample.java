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
//  2007/11/28  Martin D. Flynn
//     -Initial release
//     -Note: this is a sample implementation of a RuleFactory instance.  This
//      sample implementation is only an example, and has not been fully tested.
//  2008/02/21  Martin D. Flynn
//     -Additional examples provided
//  2009/04/02  Martin D. Flynn
//     -Additional additional help/comments
//     -Set the default 'actionMask' to ACTION_DEFAULT
//  2009/10/02  Martin D. Flynn
//     -"executeSelector" and "executeRules" now return the executed action-mask,
//      instead of just true/false.
//  2010/01/29  Martin D. Flynn
//     -Added additional methods per RuleFactory interface
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public class RuleFactoryExample
    extends RuleFactoryAdapter
{

    // ------------------------------------------------------------------------
    // This "RuleFactoryExmaple" is only intended to be an example for creating your
    // own customized rule 'selector' parser and notification system.  It is not
    // intended to be a complete implementation.
    // 
    // To enable this 'RuleFactoryExample' will require a few configuration steps:
    // 1) Modify the parsing of the Rule "selectors' to fit your own requirements.
    //    The code below provide some examples for creating a very simple type of 
    //    rule selector.
    // 2) At startup (system initialization) an instance of this RuleFactory must be
    //    passed to the method "Device.setRuleFactory".  This can be done in startup
    //    initialization class "StartupInit.java".  Examine this source module for
    //    more information.  Once this change has been made, rebuild the project.
    // 3) Add the new 'notify...' fields to the Device record by adding the following
    //    line to the 'config.conf' or 'custom.conf' runtime configuration files:
    //      startupInit.Device.NotificationFieldInfo=true
    //    Then update the table columns with the following command (Linux example):
    //      bin/dbAdmin.pl -tables=ca
    //    Once this is configured properly, the "Notification" fields should then be 
    //    visible on the Device admin page.    
    // 4) Set the appropriate Rule "selector" value in the "Notification" selector
    //    field.  Since this RuleFactory implementation will parse the specified
    //    selector value, the syntax and appropriate values depend on how this
    //    RuleFactory is implemented.
    // 5) Set the appropriate Notification EMail in the various Account and Device
    //    records that you want to have notified when a rule selector is triggered.
    // 6) Make sure that the outbound EMail (SMTP) service is set up and working
    //    properly.  This is configured in the 'common.conf' file in the OpenGTS
    //    installation directory.
    //
    // We do have a fully functional "Event Notification Rules Engine" module
    // which can be used for commercial tracking and notification systems.
    // Please contact us for more details.
    // ------------------------------------------------------------------------

    /* debug testing */
    private static boolean SEND_NOTIFICATION = false; // set to 'true' for production
    public static void DebugSetSendNotification(boolean send)
    {
        SEND_NOTIFICATION = send;
    }

    // ------------------------------------------------------------------------
    // Example "Selector" syntax:
    //
    // The syntax of a rule "selector" is defined by a specific RuleFactory
    // implementation (that is, it can be whatever you want it to be). It could be 
    // defined as a simple identifier that must be looked up in a table to find a 
    // matching function (as in the example provided here), or it can be a more 
    // complex grammar (such as an SQL 'where' clause) that has it's own lexer/parser.
    //
    // The example provided here defines a selector as a simple identifier that
    // is used to look up a local function (also defined here) that is used to 
    // evaluate the selector.  It also allows a parameter/argument to be specified,
    // using one of the following formats:
    //      overSpeed(50)
    //      overSpeed:50
    // 
    // The email sending 'action' has been commented in this example.  You will 
    // add your own appropriate 'actions' in the event of a selector match, include
    // any email notification if desired.

    private static String SEL_IS_STOPPED        = "isStopped";
    private static String SEL_OVER_100_KPH      = "over100kph";
    private static String SEL_OVER_SPEED        = "overSpeed";
    private static String SEL_PANIC             = "panic";
    private static String SEL_CODE              = "code";
    private static String RULE_FUNCTIONS[] = new String[] {
        SEL_IS_STOPPED,
        SEL_OVER_100_KPH,
        SEL_OVER_SPEED,
        SEL_PANIC,
        SEL_CODE,
    };

    // ------------------------------------------------------------------------

    /* these define the possible argument separators */
    private static char   ARG_BEGIN_CHAR[]      = new char[] { '(', ':' };
    private static String ARG_END               = ")";

    private static int indexOfArgSeparator(String s)
    {
        if (s != null) {
            for (int i = 0; i < ARG_BEGIN_CHAR.length; i++) {
                int p = s.indexOf(ARG_BEGIN_CHAR[i]);
                if (p >= 0) {
                    return p;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------

    /* interface for rule evaluation function */
    private static interface RuleFunction
    {
        public Object evaluate(EventData ev, String arg);
        public String usage();
        public String description();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Map<String,RuleFunction> ftnMap  = null;

    /* instance of RuleFactory */
    public RuleFactoryExample() 
    {
        super();
        this.ftnMap = new HashMap<String,RuleFunction>();

        /* panic */
        this.ftnMap.put(SEL_PANIC, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                return (ev.getStatusCode() == StatusCodes.STATUS_PANIC_ON);
            }
            public String usage() {
                return SEL_PANIC;
            }
            public String description() {
                return "True if Event status code is STATUS_PANIC_ON";
            }
        });

        /* status code */
        this.ftnMap.put(SEL_CODE, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                int code = StringTools.parseInt(arg,StatusCodes.STATUS_NONE);
                return (ev.getStatusCode() == code);
            }
            public String usage() {
                return SEL_CODE;
            }
            public String description() {
                return "True if Event status code matches specified code";
            }
        });

        /* vehicle speeds over 100 kph */
        this.ftnMap.put(SEL_OVER_100_KPH, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                return new Boolean(ev.getSpeedKPH() > 100.0);
            }
            public String usage() {
                return SEL_OVER_100_KPH;
            }
            public String description() {
                return "True if Event speed is over 100 km/h";
            }
        });

        /* vehicle stopped */
        this.ftnMap.put(SEL_IS_STOPPED, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                return new Boolean(ev.getSpeedKPH() <= 0.0);
            }
            public String usage() {
                return SEL_IS_STOPPED;
            }
            public String description() {
                return "True if Event speed is 0.0";
            }
        });

        /* vehicle overspeed (with argument) */
        this.ftnMap.put(SEL_OVER_SPEED, new RuleFunction() {
            public Object evaluate(EventData ev, String arg) {
                double maxSpeed = StringTools.parseDouble(arg,99999.9);
                return new Boolean(ev.getSpeedKPH() > maxSpeed);
            }
            public String usage() {
                return SEL_OVER_SPEED;
            }
            public String description() {
                return "True if Event speed is over specified limit";
            }
        });

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the RuleFactory name */
    public String getName()
    {
        return "RuleExample";
    }

    /* return the RuleFactory version */
    public String getVersion()
    {
        return "0.0.3";
    }

    /* consitancy check */
    public boolean checkRuntime()
    {
        return true;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return an array of available identifiers */
    public java.util.List<String> getIdentifierNames()
    {
        return new Vector<String>();
    }

    /* return the function description */
    public String getIdentifierDescription(String idName)
    {
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return an array of available functions */
    public java.util.List<String> getFunctionNames()
    {
        return ListTools.toList(RULE_FUNCTIONS);
    }

    /* return the function 'usage' String */
    public String getFunctionUsage(String ftnName)
    {
        RuleFunction ruleFtn = this.getFunction(ftnName);
        return (ruleFtn != null)? ruleFtn.usage() : "";
    }

    /* return the function description */
    public String getFunctionDescription(String ftnName)
    {
        RuleFunction ruleFtn = this.getFunction(ftnName);
        return (ruleFtn != null)? ruleFtn.description() : "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return RuleFunction for specified selector */
    private RuleFunction getFunction(String selector)
    {
        if (StringTools.isBlank(selector)) {
            return null;
        } else {
            int p = indexOfArgSeparator(selector);
            String sel = (p >= 0)? selector.substring(0,p) : selector;
            String arg = (p >= 0)? selector.substring(p+1) : null; // <-- ignored here
            RuleFunction ftn = this.ftnMap.get(sel); // case sensitive
            if (ftn == null) {
                Print.logWarn("Function for selector not found: " + selector);
            }
            return ftn;
        }
    }

    /* extract the argument from the specified selector */
    private String getArgument(String selector)
    {
        if (StringTools.isBlank(selector)) {
            return null;
        } else {
            int p = indexOfArgSeparator(selector);
            if (p < 0) {
                return null;
            } else {
                String a = selector.substring(p+1);
                if (a.endsWith(ARG_END)) {
                    a = a.substring(0, a.length() - 1);
                }
                return a;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return the selector for the specified rule-id
    *** @param account the Account
    *** @param ruleID  the rule-id
    *** @return The rule selector, or null if the rule-id is not defined
    **/
    public String getRuleSelector(Account account, String ruleID)
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /* check selector syntax */
    public boolean checkSelectorSyntax(String selector) 
    {
        // perfrom syntax checking of the 'selector' value
        if (StringTools.isBlank(selector)) {
            // assume that an empty selector is valid
            // (however, note that an empty/null selector is not a 'match')
            return true;
        } else {
            // true if our function map contains the specified key
            String selList[] = StringTools.split(selector,',');
            for (int r = 0; r < selList.length; r++) {
                RuleFunction rf = this.getFunction(selList[r]);
                if (rf == null) {
                    // function not found
                    return false; 
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /* return true if the specified selector matches the specified event record */
    public boolean isSelectorMatch(String selector, Account account) 
    {
        return false;
    }

    // ------------------------------------------------------------------------

    /* return true if the specified selector matches the specified event record */
    public boolean isSelectorMatch(String selector, EventData event) 
    {
        if ((event != null) && !StringTools.isBlank(selector)) {
            String selList[] = StringTools.split(selector,',');
            for (int i = 0; i < selList.length; i++) {
                if (this._isSelectorMatch(selList[i],event)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* return true if the specified selector matches the specified event record */
    private boolean _isSelectorMatch(String selector, EventData event) 
    {
        
        /* valid selector */
        if (StringTools.isBlank(selector)) {
            return false;
        }

        /* get function */
        RuleFunction ftn = this.getFunction(selector);
        if (ftn == null) {
            // no function, no match
            return false;
        }

        /* evaluate function */
        String arg = this.getArgument(selector);
        Object eval = ftn.evaluate(event,arg);
        if (eval == null) {
            // no valid result, return false
            return false;
        }
        
        /* return boolean results */
        if (eval instanceof Boolean) {
            // return Boolean value
            return ((Boolean)eval).booleanValue();
        } else
        if (eval instanceof Number) {
            // return true if Number is non-zero
            return (((Number)eval).longValue() != 0L);
        } else {
            // return true for everything else
            return true;
        }

    }

    // ------------------------------------------------------------------------

    /* check rule selector and perform action */
    public Object evaluateSelector(String selector, Account account) 
    {
        return Boolean.FALSE;
    }

    // ------------------------------------------------------------------------

    /* return the result of the specified selector */
    public Object evaluateSelector(String selector, EventData event) 
    {
        String selList[] = StringTools.split(selector,',');
        Object eval = null;
        for (int i = 0; i < selList.length; i++) {
            eval = this._evaluateSelector(selList[i],event);
            if ((eval instanceof Boolean) && ((Boolean)eval).booleanValue()) {
                // return Boolean value
                return eval;
            } else
            if ((eval instanceof Number) && (((Number)eval).longValue() != 0L)) {
                // return Number value
                return eval;
            }
        }
        return eval; // return last value
    }

    /* return the result of the specified selector */
    private Object _evaluateSelector(String selector, EventData event) 
    {

        /* get function */
        RuleFunction ftn = this.getFunction(selector);
        if (ftn == null) {
            // no function
            return null;
        }
        
        /* evaluate and return */
        String arg = this.getArgument(selector);
        return ftn.evaluate(event,arg);

    }

    // ------------------------------------------------------------------------

    /* check rule selector and perform action */
    public int executeSelector(String selector, EventData event) 
    {
        if (this.isSelectorMatch(selector,event)) {
            Print.logInfo("Selector matched: " + selector);
            int actionMask = RuleFactory.ACTION_DEFAULT;
            // perform appropriate action, such as sending email
            // RuleFactoryExample._sendNotification(event, RuleFactory.ACTION_DEFAULT);
            return actionMask;
        } else {
            Print.logDebug("Selector match returned false: " + selector);
            return -1;
        }
    }

    // ------------------------------------------------------------------------

    public int executeRules(EventData event) 
    {
        
        /* no event */
        if (event == null) {
            return -1;
        }

        /* get device */
        String accountID = event.getAccountID();
        Device device = event.getDevice();
        if (device == null) {
            Print.logError("EventData Device not found: " + accountID + "/" + event.getDeviceID());
            return -1;
        }
            
        /* execute rules, and perform appropriate action */
        String notifySel = device.getNotifySelector();
        if (this.isSelectorMatch(notifySel,event)) {
            int actionMask = device.getNotifyAction();
            RuleFactoryExample._sendNotification(event, actionMask);
            return actionMask;
        }
            
        // other rules/selectors can be evaluated here
        // IE. loop through other selectors: this.executeSelector(ruleSel[i], event);

        /* no triggered rules */
        return -1;

    }

    // ------------------------------------------------------------------------

    /* example notification action */
    protected static boolean _sendNotification(EventData evdb, int actionMask)
    {

        /* default action */
        if (actionMask == 0) {
            actionMask = ACTION_DEFAULT;
        }

        /* account/device */
        Account account = evdb.getAccount();
        Device  device  = evdb.getDevice();
        if ((account == null) || (device == null)) {
            return false;
        }
        
        /* private label */
        BasicPrivateLabel privLabel = account.getPrivateLabel();
        if (privLabel == null) {
            Print.logWarn("No PrivateLabel found for Account");
        }

        /* "To:" email address */
        HashSet<String> emailTo = new HashSet<String>();
        // Account recipients
        if ((actionMask & RuleFactory.ACTION_NOTIFY_ACCOUNT) != 0) {
            String email = account.getNotifyEmail();
            if (!email.equals("")) {
                emailTo.add(email);
            }
        }
        // Device recipients
        if ((actionMask & RuleFactory.ACTION_NOTIFY_DEVICE) != 0) {
            String email = device.getNotifyEmail();
            if (!email.equals("")) {
                emailTo.add(email);
            }
        }
        // Accumulate recipients into a single string
        StringBuffer toEmailSB = new StringBuffer();
        for (Iterator i = emailTo.iterator(); i.hasNext();) {
            String emailAddr = (String)i.next();
            if (toEmailSB.length() > 0) { toEmailSB.append(","); }
            toEmailSB.append(emailAddr);
        }
        String toEmail = toEmailSB.toString();

        /* "From:" email address */
        String frEmail = (privLabel != null)? privLabel.getEventNotificationFrom() : null;
        if ((frEmail == null) || frEmail.equals("")) {
            frEmail = (privLabel != null)? privLabel.getEMailAddress(BasicPrivateLabel.EMAIL_TYPE_NOTIFY) : null;
            if ((frEmail == null) || frEmail.equals("")) {
                frEmail = account.getContactEmail();
            }
        }

        /* subject/body */
        String subj = "Vehicle " + evdb.getDeviceID() + ": Code = " + evdb.getStatusCode();
        String body = 
            "Account   : " + evdb.getAccountID() + "\n" +
            "Tractor   : " + evdb.getDeviceID() + "\n" +
            "Date/Time : " + evdb.getTimestamp() + "\n" +
            "Status    : " + evdb.getStatusCode() + "\n" +
            "Location  : " + evdb.getGeoPoint() + "\n" +
            "Speed     : " + evdb.getSpeedKPH() + " KPH  " + evdb.getHeading() + "\n";
        if (evdb.hasAddress()) { 
            body += "Address   : " + evdb.getAddress() + "\n";
        }
        body += "\n";

        /* debug logging */
        Print.logInfo("From: "    + frEmail);
        Print.logInfo("To: "      + toEmail);
        Print.logInfo("Subject: " + subj);
        Print.logInfo("Body:\n"   + body);
        
        /* action results */
        boolean success = true;

        /* skip notification */
        if (!SEND_NOTIFICATION) {
            // typically used for debugging only
            Print.logWarn("Debug mode enabled, email will not be sent ...");
            return success;
        }

        /* send email */
        if ((actionMask & RuleFactory.ACTION_VIA_EMAIL) != 0) {
            if (emailTo.size() <= 0) {
                // no-one to which to send email
                Print.logWarn("EMail requested, but no recipients specified");
                success = false;
            } else
            if ((frEmail == null) || frEmail.equals("")) {
                // PROBLEM HERE: We haven't set a valid 'From' address in the config file.
                // Issue a stack-trace to make sure we get noticed.
                Print.logWarn("Cannot determine 'From' email address!!!");
                success = false;
            } else {
                try {
                    SendMail.send(frEmail, toEmail, null, null, subj, body, null);
                } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                    // this will fail if JavaMail support for SendMail is not available.
                    Print.logWarn("SendMail error: " + t);
                    success = false;
                }
            }
        }

        /* callback listener */
        if ((actionMask & RuleFactory.ACTION_VIA_LISTENER) != 0) {
            // TODO: implement callback listener
        }

        /* queue notification message */
        if ((actionMask & RuleFactory.ACTION_VIA_QUEUE) != 0) {
            // TODO: queue notofication message
        }

        /* return success */
        return success;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static void printMatch(RuleFactory ruleFact, EventData event, String sel)
    {
        Print.logInfo("Match: " + sel + " ==> " + ruleFact.isSelectorMatch(sel, event));
    }
    
    /* test example RuleFactory */
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        RuleFactory ruleFact = new RuleFactoryExample();
        
        /* EventData */
        EventData.Key evKey = new EventData.Key("demo","demo",DateTime.getCurrentTimeSec(),StatusCodes.STATUS_LOCATION);
        EventData evRcd = evKey.getDBRecord();
        evRcd.setAddress("1234 Somewhere Ln, Somewhere CA, 98765");
        evRcd.setGeoPoint(new GeoPoint(35.12345, -142.12345));
        evRcd.setSpeedKPH(105.0);
        evRcd.setHeading(123.0);
        evRcd.setAltitude(457.0);
        evRcd.setOdometerKM(123456.0);
        evRcd.setDistanceKM(3456.0);
        
        /* test match */
        printMatch(ruleFact, evRcd, SEL_PANIC);
        printMatch(ruleFact, evRcd, SEL_OVER_100_KPH);
        printMatch(ruleFact, evRcd, SEL_IS_STOPPED);
        printMatch(ruleFact, evRcd, SEL_OVER_SPEED + ":" + 99.0);
        printMatch(ruleFact, evRcd, SEL_OVER_SPEED + ":" + 106.0);

    }
    
}
