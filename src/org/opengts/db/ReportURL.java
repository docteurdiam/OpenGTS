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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public class ReportURL
{

    // ------------------------------------------------------------------------

    public  static final String RPTARG_ACCOUNT      = "account";    // Constants.PARM_ACCOUNT
    public  static final String RPTARG_USER         = "user";       // Constants.PARM_USER
    public  static final String RPTARG_ENCPASS      = "encpass";    // Constants.PARM_ENCPASS

    public  static final String RPTARG_DEVICE       = "device";     // Constants.PARM_DEVICE
    public  static final String RPTARG_GROUP        = "group";      // Constants.PARM_GROUP
    
    public  static final String RPTARG_DATE_FR[]    = new String[] { "date_fr"      , "fr"    };    // Calendar.PARM_RANGE_FR
    public  static final String RPTARG_DATE_TO[]    = new String[] { "date_to"      , "to"    };    // Calendar.PARM_RANGE_TO
    public  static final String RPTARG_DATE_TZ[]    = new String[] { "date_tz"      , "tz"    };    // Calendar.PARM_TIMEZONE
    
    public  static final String RPTARG_REPORT[]     = new String[] { "r_report"     , "rpt"   };
    public  static final String RPTARG_LIMIT[]      = new String[] { "r_limit"      , "lim"   };
    public  static final String RPTARG_LIMIT_TYPE[] = new String[] { "r_limType"    , "ltp"   };
    
    public  static final String RPTARG_FORMAT[]     = new String[] { "r_format"     , "fmt"   };
    public  static final String RPTARG_EMAIL[]      = new String[] { "r_emailAddr"  , "email" };

    public  static final String URLARG_RTP          = "rtp_";

    // ------------------------------------------------------------------------

    public static final String FORMAT_HTML          = "html";
    public static final String FORMAT_XML           = "xml";
    public static final String FORMAT_CSV           = "csv";
    public static final String FORMAT_XLS           = "xls";
    public static final String FORMAT_XLSX          = "xlsx";
    public static final String FORMAT_TXT           = "txt";
    public static final String FORMAT_SOAPXML       = "soapxml";
    public static final String FORMAT_EHTML         = "ehtml";      // embedded HTML (no external links)
    public static final String FORMAT_CUSTOM        = "custom";
    public static final String FORMAT_SCHEDULE      = "sched";
    public static final String FORMAT_URL           = "url";
    public static final String FORMAT_EMAIL         = "email";
    public static final String FORMAT_CALLBACK      = "callback";

    public enum Format implements EnumTools.IntValue, EnumTools.StringValue {
        HTML     (  0, FORMAT_HTML     ),    // MIME: "text/html" (default)
        XML      (  1, FORMAT_XML      ),    // MIME: "text/xml"
        CSV      (  2, FORMAT_CSV      ),    // MIME: "text/csv"
        XLS      (  3, FORMAT_XLS      ),    // MIME: "application/vnd.ms-excel"
        XLSX     (  4, FORMAT_XLSX     ),    // MIME: "application/vnd.ms-excel"
        TXT      (  5, FORMAT_TXT      ),    // MIME: "text/plain" (csv format)
        SOAP     (  6, FORMAT_SOAPXML  ),    // 
        EHTML    (  7, FORMAT_EHTML    ),    // 
        CUSTOM   (  8, FORMAT_CUSTOM   ),    // 
        SCHEDULE (  9, FORMAT_SCHEDULE ),    // 
        URL      ( 10, FORMAT_URL      ),    // 
        EMAIL    ( 11, FORMAT_EMAIL    ),    // 
        CALLBACK ( 12, FORMAT_CALLBACK );    // 
        // ---
        private int      vv = 0;
        private String   aa = null;
        Format(int v, String a)         { vv = v; aa = a; }
        public int     getIntValue()    { return vv; }
        public String  getStringValue() { return this.toString(); }
        public String  toString()       { return aa; }
    }

    // ------------------------------------------------------------------------
    
    public  static final String PARM_PAGE           = "page";        // org.opengts.war.tools.CommonServlet.PARM_PAGE;

    public  static final String PAGE_REPORT_SHOW    = "report.show"; // org.opengts.war.track.Constants.PAGE_REPORT_SHOW;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Return a URL for the EventDetail report, for the specified Device
    **/
    public static URIArg createEventDetailReportURL(
        String userID, boolean inclPass,
        Device dev, long timestamp,
        String baseURL)
    {

        /* no Device? */
        if (dev == null) { 
            return null; 
        }
        String deviceID  = dev.getDeviceID();
        String groupID   = null;

        /* get Account */
        String  accountID = dev.getAccountID();
        Account account   = dev.getAccount();
        TimeZone acctTZ   = account.getTimeZone(null/*default*/);

        /* get User */
        User user = null;
        try {
            if (StringTools.isBlank(userID)) {
                String uid = account.getDefaultUser();
                if (!StringTools.isBlank(uid)) {
                    userID = uid;
                } else {
                    userID = User.getAdminUserID();
                }
            }
            user = User.getUser(account, userID); // may return null
        } catch (DBException dbe) {
            Print.logException("Reading User: " + accountID + "/" + userID, dbe);
            user = null;
        }
        TimeZone TZ = (user != null)? user.getTimeZone(acctTZ) : acctTZ;

        /* get password */
        String encPass = null;
        if (inclPass) {
            if (!StringTools.isBlank(userID) && (user != null)) {
                encPass = user.getDecodedPassword();
            } else {
                encPass = account.getDecodedPassword();
            }
        }

        /* Event dates */
        String date_tz = ""; // (user != null)? user.getTimeZone() : account.getTimeZone(); // String name
        String date_fr = null;
        String date_to = null;
        if (timestamp > 0L) {
            date_fr = String.valueOf(timestamp - 2L);
            date_to = String.valueOf(timestamp + 2L);
        } else {
            date_fr = "last";
            date_to = "from";
        }

        /* report */
        String r_report  = ""; // default ("EventDetail")
        String r_limit   = ""; // default (1000)
        String r_limType = ""; // default ("last")
        String r_format  = ""; // default ("http")

        /* create URL */
        boolean rtpEncode = true;
        return  ReportURL.createReportURL(
            baseURL, rtpEncode,
            accountID, userID, encPass,
            deviceID, groupID,
            date_fr, date_to, date_tz,
            r_report,
            r_limit, r_limType,
            r_format);

    }

    // ------------------------------------------------------------------------
    // account=<account> user=<user>
    // r_report=<report> 
    // device=<device> | group=<group>
    // date_fr=<ts> date_to=<ts> date_tz=<tz>
    // r_limit=<limit> r_limType=last|first
    // format=html|csv|xml

    public static URIArg createReportURL(URIArg rptURL, boolean rtpEncode)
    {
        if (rptURL == null) { return null; }
        String  baseURL   = rptURL.getURI();
        RTProperties rtp  = rptURL.getArgProperties();
        // authorization
        String  accountID = rtp.getString(RPTARG_ACCOUNT     , "");
        String  userID    = rtp.getString(RPTARG_USER        , "");
        String  encPass   = rtp.getString(RPTARG_ENCPASS     , "");
        // device/group report
        String  deviceID  = rtp.getString(RPTARG_DEVICE      , "");
        String  groupID   = rtp.getString(RPTARG_GROUP       , "");
        // date range
        String  date_fr   = rtp.getString(RPTARG_DATE_FR     , "");
        String  date_to   = rtp.getString(RPTARG_DATE_TO     , "");
        String  date_tz   = rtp.getString(RPTARG_DATE_TZ     , "");
        // report attributes
        String  r_report  = rtp.getString(RPTARG_REPORT      , "");
        String  r_limit   = rtp.getString(RPTARG_LIMIT       , "");
        String  r_limType = rtp.getString(RPTARG_LIMIT_TYPE  , "");
        String  r_format  = rtp.getString(RPTARG_FORMAT      , "");
        // create report url
        return  ReportURL.createReportURL(
            baseURL, rtpEncode,
            accountID, userID, encPass,
            deviceID, groupID,
            date_fr, date_to, date_tz,
            r_report,
            r_limit, r_limType,
            r_format);
    }

    public static URIArg createReportURL(
        String baseURL, boolean rtpEncode,
        String accountID, String userID, String encPass,
        String deviceID, String groupID,
        String date_fr, String date_to, String date_tz,
        String r_report,
        // remaining args are optional
        String r_limit, String r_limType,
        String r_format)
    {

        /* URL */
        URIArg url = new URIArg(baseURL);
        if (!StringTools.isBlank(accountID)) {
            url.addArg(RPTARG_ACCOUNT, accountID);
        }
        if (!StringTools.isBlank(userID)) {
            url.addArg(RPTARG_USER, userID);
        }
        if (!StringTools.isBlank(encPass) && !encPass.equals(Account.BLANK_PASSWORD)) {
            url.addArg(RPTARG_ENCPASS, encPass);
        }

        /* create RTP */
        RTProperties rtp = new RTProperties();
        rtp.setString(PARM_PAGE, PAGE_REPORT_SHOW);
        // device=
        if (!StringTools.isBlank(deviceID)) {
            rtp.removeProperties(RPTARG_DEVICE);
            rtp.setString(RPTARG_DEVICE, deviceID);
        }
        // group=
        if (!StringTools.isBlank(groupID)) {
            rtp.removeProperties(RPTARG_GROUP);
            rtp.setString(RPTARG_GROUP, groupID);
        }
        // date_fr=
        if (!StringTools.isBlank(date_fr)) {
            rtp.removeProperties(RPTARG_DATE_FR);
            rtp.setString(RPTARG_DATE_FR[1], date_fr);
        }
        // date_to=
        if (!StringTools.isBlank(date_to)) {
            rtp.removeProperties(RPTARG_DATE_TO);
            rtp.setString(RPTARG_DATE_TO[1], date_to);
        }
        // date_tz=
        if (!StringTools.isBlank(date_tz)) {
            rtp.removeProperties(RPTARG_DATE_TO);
            rtp.setString(RPTARG_DATE_TZ[1], date_tz);
        }
        // r_report=
        if (!StringTools.isBlank(r_report)) {
            rtp.removeProperties(RPTARG_REPORT);
            rtp.setString(RPTARG_REPORT[1], r_report);
        }
        // r_limit=
        if (!StringTools.isBlank(r_limit)) {
            rtp.removeProperties(RPTARG_LIMIT);
            rtp.setString(RPTARG_LIMIT[1], r_limit);
        }
        // r_limTyp=
        if (!StringTools.isBlank(r_limType)) {
            rtp.removeProperties(RPTARG_LIMIT_TYPE);
            rtp.setString(RPTARG_LIMIT_TYPE[1], r_limType);
        }
        // r_format=
        if (!StringTools.isBlank(r_format)) {
            rtp.removeProperties(RPTARG_FORMAT);
            rtp.setString(RPTARG_FORMAT[1], r_format);
        }
        Print.logInfo("Report RPT: " + rtp);

        /* remaining arguments */
        if (rtpEncode) {
            url.addArg(URLARG_RTP, rtp);
        } else {
            Map<Object,Object> props = rtp.getProperties();
            for (Object rtk : props.keySet()) {
                Object rtv = props.get(rtk);
                url.addArg((String)rtk, StringTools.trim(rtv));
            }
        }

        /* URL */
        return url;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String args[])
    {
        RTConfig.setCommandLineArgs(args);
        
        if (RTConfig.hasProperty("urld")) {
            String urld = RTConfig.getString("urld","");
            URIArg rtpUrl = new URIArg(urld);
            URIArg decUrl = rtpUrl.rtpDecode(URLARG_RTP);
            Print.sysPrintln("Decoded URL: " + decUrl.toString());
            System.exit(0);
        }

        if (RTConfig.hasProperty("urle")) {
            String urle = RTConfig.getString("urle","");
            URIArg decUrl = new URIArg(urle);
            URIArg rtpUrl = decUrl.rtpEncode(URLARG_RTP, RPTARG_ACCOUNT, RPTARG_USER);
            Print.sysPrintln("Encoded URL: " + rtpUrl.toString());
            System.exit(0);
        }
        
        if (RTConfig.hasProperty("rpturl")) {
            String  baseURL   = StringTools.blankDefault(RTConfig.getString("rpturl",null), ".");
            String  accountID = RTConfig.getString("account","demo");
            String  userID    = RTConfig.getString("user"   ,null);
            String  deviceID  = RTConfig.getString("device" ,"demo");
            Account account   = null;
            Device  device    = null;
            try {
                account = Account.getAccount(accountID); // may throw DBException
                device  = Device.getDevice(account, deviceID, false); // may throw DBException
                if (device == null) {
                    Print.logError("Account/Device does not exist: " + accountID + "/" + deviceID);
                    System.exit(99);
                }
            } catch (DBException dbe) {
                Print.logError("Error getting Device: " + accountID + "/" + deviceID);
                dbe.printException();
                System.exit(99);
            }
            URIArg url = ReportURL.createEventDetailReportURL(
                userID, true/*inclPass*/,
                device, 0L/*timestamp*/, 
                baseURL);
            Print.sysPrintln("Report URL : " + url);
            URIArg decUrl = url.rtpDecode(URLARG_RTP);
            Print.sysPrintln("Decoded URL: " + decUrl.toString());

            System.exit(0);
        }

        String url       = RTConfig.getString("url"             ,"");
        String account   = RTConfig.getString(RPTARG_ACCOUNT    ,"");
        String user      = RTConfig.getString(RPTARG_USER       ,"");
        String encPass   = RTConfig.getString(RPTARG_ENCPASS    ,"");
        String device    = RTConfig.getString(RPTARG_DEVICE     ,"");
        String group     = RTConfig.getString(RPTARG_GROUP      ,"");
        String date_fr   = RTConfig.getString(RPTARG_DATE_FR    ,"");
        String date_to   = RTConfig.getString(RPTARG_DATE_TO    ,"");
        String date_tz   = RTConfig.getString(RPTARG_DATE_TZ    ,"");
        String r_report  = RTConfig.getString(RPTARG_REPORT     ,"");
        String r_limit   = RTConfig.getString(RPTARG_LIMIT      ,"");
        String r_limType = RTConfig.getString(RPTARG_LIMIT_TYPE ,"");
        String format    = RTConfig.getString(RPTARG_FORMAT     ,"");

        /* URL */
        URIArg rptURL = ReportURL.createReportURL(
            url, false,
            account, user, "",
            device, group,
            date_fr, date_to, date_tz,
            r_report, r_limit, r_limType,
            format);
        Print.logInfo("URL: " + rptURL);

    }

}

