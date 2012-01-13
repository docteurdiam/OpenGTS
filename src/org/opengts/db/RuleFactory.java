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
//  2007/07/14  Martin D. Flynn
//     -Changes to facilitate easier rule checking and action execution.
//  2007/07/27  Martin D. Flynn
//     -Changed 'executeStatusCodeRules' to 'executeRules'
//     -Added 'executeSelector'
//  2009/10/02  Martin D. Flynn
//     -Added ACTION_SAVE_LAST action
//     -"executeSelector" and "executeRules" now return the executed action-mask,
//      instead of just true/false.
//  2010/01/29  Martin D. Flynn
//     -Added "getRuleSelector" method.
//     -Added "isSelectorMatch(..., Account)" method
//     -Added "evaluateSelector(..., Account)" method
//  2010/11/29  Martin D. Flynn
//     -Removed 'ACTION_SAVE_LAST' from the default action
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public interface RuleFactory
{
 
    // ------------------------------------------------------------------------

    public static final String PROP_rule_workHours_         = "rule.workHours.";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Notification Action

    public static final int    ACTION_NONE                  = 0x00000000; // no action

    public static final int    ACTION_NOTIFY_MASK           = 0x000000FF; // 
    public static final int    ACTION_NOTIFY_ACCOUNT        = 0x00000001; // send email to Account 'notifyEmail'
    public static final int    ACTION_NOTIFY_DEVICE         = 0x00000002; // send email to Device 'notifyEmail'
    public static final int    ACTION_NOTIFY_RULE           = 0x00000004; // send email to Rule 'notifyEmail'

    public static final int    ACTION_VIA_MASK              = 0x0000FF00; // 
    public static final int    ACTION_VIA_EMAIL             = 0x00000100; // notify via SendMail (default)
    public static final int    ACTION_VIA_QUEUE             = 0x00000200; // notify via Notify Queue
    public static final int    ACTION_VIA_LISTENER          = 0x00000400; // notify via callback listener

    public static final int    ACTION_SAVE_LAST             = 0x00010000; // save last notification (in Device record)

    public static final int    ACTION_NOTIFY_ALL =  // 0x00000007
          ACTION_NOTIFY_ACCOUNT
        | ACTION_NOTIFY_DEVICE
        | ACTION_NOTIFY_RULE
        ;

    public static final int    ACTION_EMAIL_ALL =   // 0x00000107
          ACTION_VIA_EMAIL        
        | ACTION_NOTIFY_ALL
        ;

    public static final int    ACTION_DEFAULT =     // 0x00010507
          ACTION_EMAIL_ALL                          // 0x00000107
        | ACTION_VIA_LISTENER                       // 0x00000400
     // | ACTION_SAVE_LAST;                         // 0x00010000
        ;

    public enum NotifyAction implements EnumTools.BitMask, EnumTools.StringLocale {
        NONE         ((long)ACTION_NONE          ,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.none"         ,"None"      )),
        // ---
        NOTIFY_ACCT  ((long)ACTION_NOTIFY_ACCOUNT,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.notifyAccount","Account"   )),
        NOTIFY_DEV   ((long)ACTION_NOTIFY_DEVICE ,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.notifyDevice" ,"Device"    )),
        NOTIFY_RULE  ((long)ACTION_NOTIFY_RULE   ,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.notifyRule"   ,"Rule"      )),
        // ---
        VIA_EMAIL    ((long)ACTION_VIA_EMAIL     ,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.viaEMail"     ,"EMail"     )),
        VIA_QUEUE    ((long)ACTION_VIA_QUEUE     ,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.viaQueue"     ,"Queue"     )),
        VIA_LISTENER ((long)ACTION_VIA_LISTENER  ,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.viaListener"  ,"Listener"  )),
        // ---
        SAVE_LAST    ((long)ACTION_SAVE_LAST     ,I18N.getString(RuleFactory.class,"RuleFactory.notifyAction.saveLast"     ,"SaveLast"  ));
        // ---
        private long        vv = 0L;
        private I18N.Text   aa = null;
        NotifyAction(long v, I18N.Text a)           { vv=v; aa=a; }
        public long    getLongValue()               { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final int     PRIORITY_UNDEFINED          = 0;
    public static final int     PRIORITY_HIGH               = 1;
    public static final int     PRIORITY_MEDIUM             = 5;
    public static final int     PRIORITY_LOW                = 9;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return this RuleFactory name 
    *** @return This RuleFactory name
    **/
    public String getName();

    /**
    *** Return this RuleFactory version String
    *** @return This RuleFactory version String
    **/
    public String getVersion();

    // ------------------------------------------------------------------------

    /**
    *** Returns a list of all available selector identifiers
    *** @return A list of all available selector identifiers
    **/
    public java.util.List<String> getIdentifierNames();

    /**
    *** Returns the description for the specified identifier
    *** @param idName  The name of the identifier for which the description is returned.
    *** @return The description for the specified identifier
    **/
    public String getIdentifierDescription(String idName);

    // ------------------------------------------------------------------------

    /**
    *** Returns a list of all available selector function names
    *** @return A list of all available selector function names
    **/
    public java.util.List<String> getFunctionNames();

    /**
    *** Returns the function "usage" description for the specified function name
    *** @param ftnName  The name of the function for which the "usage" description is returned.
    *** @return The "usage" description for the specified function name
    **/
    public String getFunctionUsage(String ftnName);

    /**
    *** Returns the description for the specified function name
    *** @param ftnName  The name of the function for which the description is returned.
    *** @return The description for the specified function name
    **/
    public String getFunctionDescription(String ftnName);

    // ------------------------------------------------------------------------

    /**
    *** Returns the rule selector for the specified rule-id, in the specified Account
    *** @param account  The account which owns the rule-id from which the selector is returned
    *** @param ruleID   The Rule-ID which contains the selector to return
    *** @return The rule selector for the specified account and rule-id
    **/
    public String getRuleSelector(Account account, String ruleID);

    /**
    *** Return true if the specified selector is syntactically correct
    *** @param selector  The rule selector to syntax check
    *** @return True if the selector is syntactically correct, false otherwise
    **/
    public boolean checkSelectorSyntax(String selector); 

    /**
    *** Return true if the specified 'event' matches the specified 'selector'
    *** @param selector  The rule selector to check against the specified event
    *** @param event     The event that contains the criteria used by the selector
    *** @return True if the events matches the specified selector, false otherwise
    **/
    public boolean isSelectorMatch(String selector, EventData event);

    /**
    *** Return true if the specified Account matches the specified 'selector'
    *** @param selector  The rule selector to check against the specified event
    *** @param account   The Account that contains the criteria used by the selector
    *** @return True if the events matches the specified selector, false otherwise
    ** /
    public boolean isSelectorMatch(String selector, Account account);
    (specification not required) */

    // ------------------------------------------------------------------------

    /**
    *** Exectutes the specified selector against the criteria contained win the
    *** event record, and return a mask containing the actions performed.
    *** @param selector  The selector to execute.
    *** @param event     The event that contains the criteria used by the selector
    *** @return The mask containing the 'actions' performed.
    **/
    public int executeSelector(String selector, EventData event);

    /**
    *** Exectutes all rules which apply to the specified EventData record, and return
    *** a mask containing the actions performed.
    *** @param event     The event that contains the criteria used by the rules
    *** @return The mask containing the 'actions' performed.
    **/
    public int executeRules(EventData event);

    // ------------------------------------------------------------------------

    /**
    *** Evaluates a selector against the specified event and returns the result
    *** @param selector  The selector to execute.
    *** @param event     The event that contains the criteria used by the selector
    *** @return An object containing the result of the executed selector.
    **/
    public Object evaluateSelector(String selector, EventData event)
        throws RuleParseException;

    /**
    *** Evaluates a selector against the specified Account and returns the result
    *** @param selector  The selector to execute.
    *** @param account   The Account that contains the criteria used by the selector
    *** @return An object containing the result of the executed selector.
    **/
    public Object evaluateSelector(String selector, Account account)
        throws RuleParseException;

    // ------------------------------------------------------------------------

}
