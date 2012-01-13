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
//  2007/04/01  Martin D. Flynn
//     -Added "Distance Units" field
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/09/16  Martin D. Flynn
//     -Fixed GeocoderMode field to display the proper value from the table
//  2007/11/28  Martin D. Flynn
//     -Added 'Notify EMail' address field
//     -Invalid entries are now indicated on the page (previously they were
//      quietly ignored).
//  2008/10/16  Martin D. Flynn
//     -Update with new ACL usage
//  2008/12/01  Martin D. Flynn
//     -Added temperature units
//  2009/01/01  Martin D. Flynn
//     -Added 'Plural' field for Device/Group titles.
//  2010/04/11  Martin D. Flynn
//     -Added "Enable Border Crossing"
//     -Added "Pressure Units" selection
//  2011/03/08  Martin D. Flynn
//     -Moved GeocoderMode and isBorderCrossing to SysAdminAccounts admin.
//  2011/07/01  Martin D. Flynn
//     -Updated call to getDeviceTitles/getDeviceGroupTitles to not return the
//      standard default titles.
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.Locale;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.Calendar;
import org.opengts.war.track.*;

public class AccountInfo
    extends WebPageAdaptor
    implements Constants
{
    
    // ------------------------------------------------------------------------
    // Parameters

    // commands
    public  static final String COMMAND_INFO_UPDATE     = "update";

    // submit types
    public  static final String PARM_SUBMIT_CHANGE      = "a_subchg";

    // button types
    public  static final String PARM_BUTTON_CANCEL      = "a_btncan";
    public  static final String PARM_BUTTON_BACK        = "a_btnbak";

    // parameters
    public  static final String PARM_ACCT_ID            = "a_id";
    public  static final String PARM_ACCT_DESC          = "a_desc";
    public  static final String PARM_CONTACT_NAME       = "a_contact";
    public  static final String PARM_CONTACT_PHONE      = "a_phone";
    public  static final String PARM_CONTACT_EMAIL      = "a_email";
    public  static final String PARM_NOTIFY_EMAIL       = "a_notify";
    public  static final String PARM_TIMEZONE           = "a_tmz";
    public  static final String PARM_SPEED_UNITS        = "a_spdun";
    public  static final String PARM_DIST_UNITS         = "a_dstun";
    public  static final String PARM_VOLM_UNITS         = "a_volun";
    public  static final String PARM_ECON_UNITS         = "a_ecoun";
    public  static final String PARM_PRESS_UNITS        = "a_presun";
    public  static final String PARM_TEMP_UNITS         = "a_tempun";
    public  static final String PARM_LATLON_FORMAT      = "a_latlon";
    public  static final String PARM_DEVICE_TITLE       = "a_devtitle";
    public  static final String PARM_DEVICES_TITLE      = "a_devstitle";
    public  static final String PARM_GROUP_TITLE        = "a_grptitle";
    public  static final String PARM_GROUPS_TITLE       = "a_grpstitle";
    public  static final String PARM_ADDRESS_TITLE      = "a_adrtitle";
    public  static final String PARM_ADDRESSES_TITLE    = "a_adrstitle";
    public  static final String PARM_ACCT_EXPIRE        = "a_expire";
    public  static final String PARM_MAX_PINGS          = "a_maxPing";
    public  static final String PARM_TOT_PINGS          = "a_totPing";
    public  static final String PARM_DEFAULT_USER       = "a_dftuser";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public AccountInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_ACCOUNT_INFO);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }
    
    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("AccountInfo.editMenuDesc","View/Edit Account Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("AccountInfo.editMenuHelp","View and Edit the current Account information"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("AccountInfo.navDesc","Account"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountInfo.class);
        return i18n.getString("AccountInfo.navTab","Account Admin");
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N    i18n     = privLabel.getI18N(AccountInfo.class);
        final Locale  locale   = reqState.getLocale();
        final Account currAcct = reqState.getCurrentAccount();
        final User    currUser = reqState.getCurrentUser();
        final String  pageName = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* ACL allow edit/view */
        boolean allowEdit = privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* command */
        String accountCmd = reqState.getCommandName();
        boolean updateAccount = accountCmd.equals(COMMAND_INFO_UPDATE);

        /* change the account info? */
        if (updateAccount) {
            HttpServletRequest request = reqState.getHttpServletRequest();
            String submit = AttributeTools.getRequestString(request, PARM_SUBMIT_CHANGE, "");
            if (SubmitMatch(submit,i18n.getString("AccountInfo.change","Change"))) {
                String acctName     = AttributeTools.getRequestString(request, PARM_ACCT_DESC,"");
                String contactName  = AttributeTools.getRequestString(request, PARM_CONTACT_NAME,"");
                String contactPhone = AttributeTools.getRequestString(request, PARM_CONTACT_PHONE,"");
                String contactEmail = AttributeTools.getRequestString(request, PARM_CONTACT_EMAIL,"");
                String notifyEmail  = AttributeTools.getRequestString(request, PARM_NOTIFY_EMAIL,"");
                String timeZone     = AttributeTools.getRequestString(request, PARM_TIMEZONE,"");
                String speedUnits   = AttributeTools.getRequestString(request, PARM_SPEED_UNITS,"");
                String distUnits    = AttributeTools.getRequestString(request, PARM_DIST_UNITS,"");
                String volUnits     = AttributeTools.getRequestString(request, PARM_VOLM_UNITS,"");
                String econUnits    = AttributeTools.getRequestString(request, PARM_ECON_UNITS,"");
                String pressUnits   = AttributeTools.getRequestString(request, PARM_PRESS_UNITS,"");
                String tempUnits    = AttributeTools.getRequestString(request, PARM_TEMP_UNITS,"");
                String latLonFormat = AttributeTools.getRequestString(request, PARM_LATLON_FORMAT,"");
                String deviceTitle  = AttributeTools.getRequestString(request, PARM_DEVICE_TITLE,"");
                String devicesTitle = AttributeTools.getRequestString(request, PARM_DEVICES_TITLE,"");
                String groupTitle   = AttributeTools.getRequestString(request, PARM_GROUP_TITLE,"");
                String groupsTitle  = AttributeTools.getRequestString(request, PARM_GROUPS_TITLE,"");
                String addrTitle    = AttributeTools.getRequestString(request, PARM_ADDRESS_TITLE,"");
                String addrsTitle   = AttributeTools.getRequestString(request, PARM_ADDRESSES_TITLE,"");
                String defaultUser  = AttributeTools.getRequestString(request, PARM_DEFAULT_USER,"");
                try {
                    boolean saveOK = true;
                    // description
                    if (!StringTools.isBlank(acctName)) {
                        currAcct.setDescription(acctName);
                    } else {
                        currAcct.setDescription(currAcct.getAccountID());
                    }
                    // contact name
                    currAcct.setContactName(contactName);
                    // contact phone
                    currAcct.setContactPhone(contactPhone);
                    // contact email
                    if (StringTools.isBlank(contactEmail) || EMail.validateAddress(contactEmail)) {
                        currAcct.setContactEmail(contactEmail);
                    } else {
                        m = i18n.getString("AccountInfo.pleaseEnterContactEMail","Please enter a valid contact email address");
                        error = true;
                        saveOK = false;
                    }
                    // notify email
                    if (StringTools.isBlank(notifyEmail)) {
                        if (!currAcct.getNotifyEmail().equals(notifyEmail)) {
                            currAcct.setNotifyEmail(notifyEmail);
                        }
                    } else
                    if (EMail.validateAddresses(notifyEmail,true/*acceptSMS*/)) {
                        if (!currAcct.getNotifyEmail().equals(notifyEmail)) {
                            currAcct.setNotifyEmail(notifyEmail);
                        }
                    } else {
                        m = i18n.getString("AccountInfo.pleaseEnterNotifyEMail","Please enter a valid notify email/sms address");
                        error = true;
                        saveOK = false;
                    }
                    // timezone
                    currAcct.setTimeZone(timeZone);
                    // speed units
                    currAcct.setSpeedUnits(speedUnits, locale);
                    // distance units
                    currAcct.setDistanceUnits(distUnits, locale);
                    // volume units
                    currAcct.setVolumeUnits(volUnits, locale);
                    // economy units
                    currAcct.setEconomyUnits(econUnits, locale);
                    // pressure units
                    currAcct.setPressureUnits(pressUnits, locale);
                    // temperature units
                    currAcct.setTemperatureUnits(tempUnits, locale);
                    // latitude/longitude format
                    currAcct.setLatLonFormat(latLonFormat, locale);
                    // reverse-geocoder mode
                  //currAcct.setGeocoderMode(revGeoMode, locale);
                    // 'Device' title
                    String devSingTitle = deviceTitle;
                    String devPlurTitle = devicesTitle;
                    currAcct.setDeviceTitle(devSingTitle, devPlurTitle);
                    // 'DeviceGroup' title
                    String grpSingTitle = groupTitle;
                    String grpPlurTitle = groupsTitle;
                    currAcct.setDeviceGroupTitle(grpSingTitle, grpPlurTitle);
                    // 'Address' title
                    String adrSingTitle = addrTitle; 
                    String adrPlurTitle = addrsTitle;
                    currAcct.setAddressTitle(adrSingTitle, adrPlurTitle);
                    // default user
                    currAcct.setDefaultUser(defaultUser);
                    // save
                    if (saveOK) {
                        /* exclude fields that only the SysAdmin/AccountManager should change */
                        currAcct.addExcludedUpdateFields(
                            Account.FLD_isActive,
                            Account.FLD_isAccountManager,
                            Account.FLD_managerID,
                            Account.FLD_privateLabelName,
                            Account.FLD_isBorderCrossing,
                            Account.FLD_geocoderMode
                            );
                        currAcct.save();
                        AttributeTools.setSessionAttribute(request, Calendar.PARM_TIMEZONE[0], timeZone);
                        //Track.writeMessageResponse(reqState, i18n.getString("AccountInfo.updatedAcct","Account information updated"));
                        m = i18n.getString("AccountInfo.updatedAcct","Account information updated");
                    }
                } catch (Throwable t) {
                    Print.logException("Updating Account", t);
                    m = i18n.getString("AccountInfo.errorUpdate","Internal error updating Account");
                    error = true;
                    return;
                }
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = AccountInfo.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "AccountInfo.css", cssDir);
            }
        };

        /* javascript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
            }
        };

        /* Content */
        final boolean _allowEdit = allowEdit;
        final ComboMap _tzList   = privLabel.getTimeZoneComboMap();
        final ComboMap _suList   = privLabel.getEnumComboMap(Account.SpeedUnits.class      );
        final ComboMap _duList   = privLabel.getEnumComboMap(Account.DistanceUnits.class   );
        final ComboMap _vuList   = privLabel.getEnumComboMap(Account.VolumeUnits.class     );
        final ComboMap _ecList   = privLabel.getEnumComboMap(Account.EconomyUnits.class    );
        final ComboMap _puList   = privLabel.getEnumComboMap(Account.PressureUnits.class   );
        final ComboMap _tuList   = privLabel.getEnumComboMap(Account.TemperatureUnits.class);
        final ComboMap _llList   = privLabel.getEnumComboMap(Account.LatLonFormat.class    );
      //final ComboMap _rgList   = privLabel.getEnumComboMap(Account.GeocoderMode.class    );
        final ComboMap _ynList   = ComboMap.getYesNoMap(locale);
        HTMLOutput HTML_CONTENT  = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                //Print.logStackTrace("here");

              //String menuURL = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
              //String chgURL  = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),pageName,COMMAND_INFO_UPDATE);
                String chgURL  = privLabel.getWebPageURL(reqState, pageName, COMMAND_INFO_UPDATE);
                String frameTitle = _allowEdit? 
                    i18n.getString("AccountInfo.editAccount","Edit Account Information") : 
                    i18n.getString("AccountInfo.viewAccount","View Account Information");

                // frame content
                ComboOption speedUnits       = privLabel.getEnumComboOption(Account.getSpeedUnits(currAcct)      );
                ComboOption distanceUnits    = privLabel.getEnumComboOption(Account.getDistanceUnits(currAcct)   );
                ComboOption volumeUnits      = privLabel.getEnumComboOption(Account.getVolumeUnits(currAcct)     );
                ComboOption economyUnits     = privLabel.getEnumComboOption(Account.getEconomyUnits(currAcct)    );
                ComboOption pressureUnits    = privLabel.getEnumComboOption(Account.getPressureUnits(currAcct)   );
                ComboOption temperatureUnits = privLabel.getEnumComboOption(Account.getTemperatureUnits(currAcct));
                ComboOption latLonFormat     = privLabel.getEnumComboOption(Account.getLatLonFormat(currAcct)    );
              //ComboOption geocoderMode     = privLabel.getEnumComboOption(Account.getGeocoderMode(currAcct)    );
                String      devTitles[]      = currAcct.getDeviceTitles(locale, new String[]{"",""});
                String      grpTitles[]      = currAcct.getDeviceGroupTitles(locale, new String[]{"",""});
                String      adrTitles[]      = currAcct.getAddressTitles(locale, new String[]{"",""});
                out.println("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>");
                out.println("<hr/>");
                out.println("<form name='AccountInfo' method='post' action='"+chgURL+"' target='_self'>");
                out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                out.println(FormRow_TextField(PARM_ACCT_ID      , false     , i18n.getString("AccountInfo.accountID","Account ID:")                  , currAcct.getAccountID()       , 32, 32)); // read-only
                out.println(FormRow_TextField(PARM_ACCT_DESC    , _allowEdit, i18n.getString("AccountInfo.accountName","Account Description:")       , currAcct.getDescription()     , 40, 40));
                out.println(FormRow_TextField(PARM_CONTACT_NAME , _allowEdit, i18n.getString("AccountInfo.contactName","Contact Name:")              , currAcct.getContactName()     , 40, 40));
                out.println(FormRow_TextField(PARM_CONTACT_PHONE, _allowEdit, i18n.getString("AccountInfo.contactPhone","Contact Phone:")            , currAcct.getContactPhone()    , 20, 20));
                out.println(FormRow_TextField(PARM_CONTACT_EMAIL, _allowEdit, i18n.getString("AccountInfo.contactEMail","Contact Email:")            , currAcct.getContactEmail()    , 60, 100));
                out.println(FormRow_TextField(PARM_NOTIFY_EMAIL , _allowEdit, i18n.getString("AccountInfo.notifyEMail","Notify Email:")              , currAcct.getNotifyEmail()     , 95, 125));
                out.println(FormRow_ComboBox (PARM_TIMEZONE     , _allowEdit, i18n.getString("AccountInfo.timeZone","Time Zone:")                    , currAcct.getTimeZone()        , _tzList, null, 20));
                out.println(FormRow_ComboBox (PARM_SPEED_UNITS  , _allowEdit, i18n.getString("AccountInfo.speedUnits","Speed Units:")                , speedUnits       , _suList, null, 10));
                out.println(FormRow_ComboBox (PARM_DIST_UNITS   , _allowEdit, i18n.getString("AccountInfo.distanceUnits","Distance Units:")          , distanceUnits    , _duList, null, 10));
                out.println(FormRow_ComboBox (PARM_VOLM_UNITS   , _allowEdit, i18n.getString("AccountInfo.volumeUnits","Volume Units:")              , volumeUnits      , _vuList, null, 10));
                out.println(FormRow_ComboBox (PARM_ECON_UNITS   , _allowEdit, i18n.getString("AccountInfo.economyUnits","Economy Units:")            , economyUnits     , _ecList, null, 10));
                out.println(FormRow_ComboBox (PARM_PRESS_UNITS  , _allowEdit, i18n.getString("AccountInfo.pressureUnits","Pressure Units:")          , pressureUnits    , _puList, null, 10));
                out.println(FormRow_ComboBox (PARM_TEMP_UNITS   , _allowEdit, i18n.getString("AccountInfo.temperatureUnits","Temperature Units:")    , temperatureUnits , _tuList, null,  5));
                out.println(FormRow_ComboBox (PARM_LATLON_FORMAT, _allowEdit, i18n.getString("AccountInfo.latLonFormat","Latitude/Longitude Format:"), latLonFormat     , _llList, null, 15));

                /* "Device" title */
                out.print  ("<tr>");
                out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"'>"+i18n.getString("AccountInfo.deviceTitle","'Device' Title:")+"</td>");
                out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                out.print  (Form_TextField(PARM_DEVICE_TITLE , _allowEdit, devTitles[0], 20, 40));
                out.print  ("<span style='margin-left: 10px;margin-right:5px;'>"+i18n.getString("AccountInfo.plural","Plural:")+"</span>");
                out.print  (Form_TextField(PARM_DEVICES_TITLE, _allowEdit, devTitles[1], 20, 40));
                out.print  ("</td>");
                out.println("</tr>");
 
                /* "Fleet" title */
                out.print  ("<tr>");
                out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"'>"+i18n.getString("AccountInfo.groupTitle","'DeviceGroup' Title:")+"</td>");
                out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                out.print  (Form_TextField(PARM_GROUP_TITLE , _allowEdit, grpTitles[0], 20, 40));
                out.print  ("<span style='margin-left: 10px;margin-right:5px;'>"+i18n.getString("AccountInfo.plural","Plural:")+"</span>");
                out.print  (Form_TextField(PARM_GROUPS_TITLE, _allowEdit, grpTitles[1], 20, 40));
                out.print  ("</td>");
                out.println("</tr>");
 
                /* "Address" title */
                out.print  ("<tr>");
                out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"'>"+i18n.getString("AccountInfo.addressTitle","'Address' Title:")+"</td>");
                out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                out.print  (Form_TextField(PARM_ADDRESS_TITLE  , _allowEdit, adrTitles[0], 20, 40));
                out.print  ("<span style='margin-left: 10px;margin-right:5px;'>"+i18n.getString("AccountInfo.plural","Plural:")+"</span>");
                out.print  (Form_TextField(PARM_ADDRESSES_TITLE, _allowEdit, adrTitles[1], 20, 40));
                out.print  ("</td>");
                out.println("</tr>");

                /* default user */
                out.println(FormRow_TextField(PARM_DEFAULT_USER , _allowEdit, i18n.getString("AccountInfo.defaultUser","Default Login UserID:")      , currAcct.getDefaultUser()     , 20, 32));

                /* expiration */
                long expireTime = currAcct.getExpirationTime();
                if (expireTime > 0L) {
                    String  expireTimeStr = reqState.formatDateTime(expireTime);
                    if (StringTools.isBlank(expireTimeStr)) { expireTimeStr = "n/a"; }
                    out.println(FormRow_TextField(PARM_ACCT_EXPIRE  , false     , i18n.getString("AccountInfo.expiration","Expiration:")                 , expireTimeStr                 , 30, 30)); // read-only
                }

                /* max pings / total pings */
                int maxPingCnt = currAcct.getMaxPingCount();
                if (maxPingCnt > 0) {
                    int totPingCnt = currAcct.getTotalPingCount();
                    int remaining  = (maxPingCnt > totPingCnt)? (maxPingCnt - totPingCnt) : 0;
                    out.print  ("<tr>");
                    out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"'>"+i18n.getString("AccountInfo.maxCommandCount","Max Allowed Commands")+":</td>");
                    out.print  ("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                    out.print  (Form_TextField(PARM_MAX_PINGS, false, String.valueOf(maxPingCnt), 5, 5));
                        out.print  ("<span style='margin-left: 10px;margin-right:5px;'>"+i18n.getString("AccountInfo.remainingCommands","Remaining Commands")+":</span>");
                    out.print  (Form_TextField(PARM_TOT_PINGS, false, String.valueOf(remaining) , 5, 5));
                    out.print  ("</td>");
                    out.println("</tr>");
                }

                out.println("</table>");

                /* end of form */
                out.write("<hr style='margin-bottom:5px;'>\n");
                out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                if (_allowEdit) {
                    out.write("<input type='submit' name='"+PARM_SUBMIT_CHANGE+"' value='"+i18n.getString("AccountInfo.change","Change")+"'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("AccountInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+menuURL+"','_self');\">\n"); // target='_top'
                } else {
                    out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("AccountInfo.back","Back")+"' onclick=\"javascript:openURL('"+menuURL+"','_self');\">\n"); // target='_top'
                }
                out.write("</form>\n");

            }
        };

        /* write frame */
        String onload = error? JS_alert(true,m) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }
    
    // ------------------------------------------------------------------------
}
