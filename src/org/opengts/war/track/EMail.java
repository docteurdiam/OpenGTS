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
//  2007/11/28  Martin D. Flynn
//     -Added method for validating a list of multiple email addresses
// ----------------------------------------------------------------------------
package org.opengts.war.track;

import java.util.*;

import org.opengts.util.*;
import org.opengts.db.*;

public class EMail
{

    /* send an email */
    public static boolean send(String from, String to, String cc, String bcc, String subject, String msgBody)
    {
        try {
            return SendMail.send(from, to, cc, bcc, subject, msgBody, null);
        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
            // this will fail if JavaMail support for SendMail is not available.
            Print.logWarn("SendMail error: " + t);
            return false;
        }
    }

    /* validate the syntax of the specified single email address */
    public static boolean validateAddress(String addr)
    {
        try {
            return SendMail.validateAddress(addr);
        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
            // this will fail if JavaMail support for SendMail is not available.
            Print.logWarn("SendMail error: " + t);
            return false;
        }
    }

    /* validate the syntax of the specified list of multiple email addresses */
    public static boolean validateAddresses(String addrs, boolean acceptSMS)
    {
        if (StringTools.isBlank(addrs)) {
            // blank is ok
            return true;
        } else
        if (acceptSMS) {
            // allow "sms:123456789" format
            String addrArry[] = StringTools.parseString(addrs, ',');
            if (addrArry.length == 0) { return false; }
            for (int i = 0; i < addrArry.length; i++) {
                String em = addrArry[i].trim();
                if (StringTools.isBlank(em)) { return false; }
                if (SMSOutboundGateway.StartsWithSMS(em)) {
                    // TODO: for now, accept as-is
                } else
                if (!validateAddress(em)) { 
                    return false; 
                }
            }
            return true;
        } else {
            // true email addresses only
            try {
                return SendMail.validateAddresses(addrs);
            } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                // this will fail if JavaMail support for SendMail is not available.
                Print.logWarn("SendMail error: " + t);
                return false;
            }
        }
    }

    /* extract email address from the specified string */
    public static String getEMailAddress(String addr)
    {
        // extract/normalize email address
        try {
            return SendMail.getEMailAddress(addr);
        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
            // this will fail if JavaMail support for SendMail is not available.
            Print.logWarn("SendMail error: " + t);
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /*
    public static void main(String argv[])
    {
        if (argv.length > 0) {
            Print.sysPrintln(" Address: " + EMail.getEMailAddress(argv[0]));
        } else {
            String email = "Joe Schmidt <joeshmidt@example.com>";
            Print.sysPrintln(" Address: " + EMail.getEMailAddress(email));
        }
    }
    */

}
