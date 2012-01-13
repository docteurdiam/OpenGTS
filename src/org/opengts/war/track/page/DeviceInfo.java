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
//     -Fixed possible exception when notification email has been disabled.
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/06/30  Martin D. Flynn
//     -Added Device table view
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -Fixed NPE when no devices are yet defined for the account
//  2007/01/10  Martin D. Flynn
//     -Added edit fields for 'notes', 'simPhoneNumber'.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/10/16  Martin D. Flynn
//     -Added ability to create/delete devices.
//     -Added ability to edit unique-id and 'active' state.
//     -'setAllowNotify()' now properly set when rule selector has been specified.
//     -Update with new ACL usage
//  2008/12/01  Martin D. Flynn
//     -Added 'Map Pushpin ID' field to specify group map icon for a specific device.
//  2009/01/01  Martin D. Flynn
//     -Added Notification Description fields (if a valid RuleFactory is in place).
//  2009/05/01  Martin D. Flynn
//     -Added "Ignition Input Line" field.
//  2009/08/23  Martin D. Flynn
//     -Convert new entered IDs to lowercase
//  2009/11/10  Martin D. Flynn
//     -Added PushpinChooser support
//  2010/01/29  Martin D. Flynn
//     -Added 'codeVersion' field displayed as "Firmware Version".
//  2010/04/11  Martin D. Flynn
//     -Added Link fields (linkURL/linkDescription)
//     -"Properties" button will not display if no listed device supports the DeviceCmdHandler
//  2010/07/18  Martin D. Flynn
//     -Allow editing Server-id ("deviceCode") if device has not yet connected to server.
//     -Added "Fuel Capacity" field.
//  2010/09/09  Martin D. Flynn
//     -Added "vehicleID" field
//  2011/01/28  Martin D. Flynn
//     -Added "Creation Date" display (read-only) field
//     -Added "Driver ID"
//  2011/10/03  Martin D. Flynn
//     -Added PROP_DeviceInfo_maxIgnitionIndex
//     -Added edit "Serial Number" [device.getSerialNumber()]
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;
import org.opengts.war.maps.JSMap;

import org.opengts.war.track.page.devcmd.*;

public class DeviceInfo
    extends WebPageAdaptor
    implements Constants
{

    
    // ------------------------------------------------------------------------

    private static final boolean EDIT_SERVER_ID             = false;
    private static final boolean SHOW_LAST_CONNECT          = false;
    private static final boolean SHOW_NOTES                 = false;
    private static final boolean SHOW_DATA_KEY              = false;
    private static final boolean SHOW_PUSHPIN_ID            = true;
    private static final boolean SHOW_IGNITION_NDX          = true;
    private static final boolean SHOW_DISPLAY_COLOR         = true;
    private static final boolean SHOW_SPEED_LIMIT           = false;
    private static final boolean SHOW_MAINTENANCE_NOTES     = true;  // still requires maintenance support
    private static final boolean SHOW_FIXED_LOCATION        = false;
    private static final boolean SHOW_NOTIFY_SELECTOR       = false; // only if RuleFactory defined

    // ------------------------------------------------------------------------

    public  static final String _ACL_RULES                  = "rules";
    public  static final String _ACL_UNIQUEID               = "uniqueID";
    public  static final String _ACL_COMMANDS               = "commands";
    public  static final String _ACL_SMS                    = "sms";
    private static final String _ACL_LIST[]                 = new String[] { _ACL_RULES, _ACL_UNIQUEID, _ACL_COMMANDS, _ACL_SMS };

    // ------------------------------------------------------------------------
    // allow new device modes

    public  static final int    NEWDEV_DENY                 = 0;
    public  static final int    NEWDEV_ALLOW                = 1;
    public  static final int    NEWDEV_SYSADMIN             = 2;

    // ------------------------------------------------------------------------
    // Parameters

    // forms
    public  static final String FORM_DEVICE_SELECT          = "DeviceInfoSelect";
    public  static final String FORM_DEVICE_EDIT            = "DeviceInfoEdit";
    public  static final String FORM_DEVICE_NEW             = "DeviceInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPD_DEVICE     = "updateDev";
    public  static final String COMMAND_INFO_UPD_PROPS      = "updateProps"; // see DeviceCmdHandler
    public  static final String COMMAND_INFO_UPD_SMS        = "updateSms";   // see DeviceCmd_SMS
  //public  static final String COMMAND_INFO_REFRESH        = "refreshList";
    public  static final String COMMAND_INFO_SEL_DEVICE     = "selectDev";
    public  static final String COMMAND_INFO_NEW_DEVICE     = "newDev";

    // submit
    public  static final String PARM_SUBMIT_EDIT            = "d_subedit";
    public  static final String PARM_SUBMIT_VIEW            = "d_subview";
    public  static final String PARM_SUBMIT_CHG             = "d_subchg";
    public  static final String PARM_SUBMIT_DEL             = "d_subdel";
    public  static final String PARM_SUBMIT_NEW             = "d_subnew";
    public  static final String PARM_SUBMIT_QUE             = "d_subque";
    public  static final String PARM_SUBMIT_PROP            = "d_subprop";
    public  static final String PARM_SUBMIT_SMS             = "d_subsms";

    // buttons
    public  static final String PARM_BUTTON_CANCEL          = "d_btncan";
    public  static final String PARM_BUTTON_BACK            = "d_btnbak";

    // device table fields
    public  static final String PARM_NEW_NAME               = "d_newname";
    public  static final String PARM_CREATE_DATE            = "d_creation";
    public  static final String PARM_SERVER_ID              = "d_servid";
    public  static final String PARM_CODE_VERS              = "d_codevers";
    public  static final String PARM_DEV_UNIQ               = "d_uniq";
    public  static final String PARM_DEV_DESC               = "d_desc";
    public  static final String PARM_DEV_NAME               = "d_name";
    public  static final String PARM_VEHICLE_ID             = "d_vehicid";
    public  static final String PARM_DEV_EQUIP_TYPE         = "d_equipt";
    public  static final String PARM_DEV_FUEL_CAP           = "d_fuelcap";
    public  static final String PARM_DEV_FUEL_ECON          = "d_fuelecon";
    public  static final String PARM_DEV_SPEED_LIMIT        = "d_speedLim";
    public  static final String PARM_DEV_IMEI               = "d_imei";
    public  static final String PARM_DEV_SERIAL_NO          = "d_sernum";
    public  static final String PARM_DATA_KEY               = "d_datakey";
    public  static final String PARM_DEV_SIMPHONE           = "d_simph";
    public  static final String PARM_SMS_EMAIL              = "d_smsemail";
    public  static final String PARM_ICON_ID                = "d_iconid";
    public  static final String PARM_DISPLAY_COLOR          = "d_dcolor";
    public  static final String PARM_DEV_ACTIVE             = "d_actv";
    public  static final String PARM_DEV_SERIAL             = "d_ser";
    public  static final String PARM_DEV_LAST_CONNECT       = "d_lconn";
    public  static final String PARM_DEV_LAST_EVENT         = "d_levnt";
    public  static final String PARM_DEV_NOTES              = "d_notes";
    public  static final String PARM_LINK_URL               = "d_linkurl";
    public  static final String PARM_LINK_DESC              = "d_linkdesc";
    public  static final String PARM_FIXED_LAT              = "d_fixlat";
    public  static final String PARM_FIXED_LON              = "d_fixlon";
    public  static final String PARM_IGNITION_INDEX         = "d_ignndx";
    public  static final String PARM_DRIVER_ID              = "d_driver";
    public  static final String PARM_DEV_GROUP_             = "d_grp_";

    public  static final String PARM_REPORT_ODOM            = "d_rptodom";
    public  static final String PARM_REPORT_HOURS           = "d_rpthours";
    public  static final String PARM_MAINT_INTERVKM_        = "d_mntintrkm";
    public  static final String PARM_MAINT_LASTKM_          = "d_mntlastkm";    // read-only
    public  static final String PARM_MAINT_NEXTKM_          = "d_mntnextkm";    // read-only
    public  static final String PARM_MAINT_RESETKM_         = "d_mntreskm";      // checkbox
    public  static final String PARM_MAINT_INTERVKM_0       = PARM_MAINT_INTERVKM_+"0";
    public  static final String PARM_MAINT_LASTKM_0         = PARM_MAINT_LASTKM_+"0";
    public  static final String PARM_MAINT_NEXTKM_0         = PARM_MAINT_NEXTKM_+"0";
    public  static final String PARM_MAINT_RESETKM_0        = PARM_MAINT_RESETKM_+"0";
    public  static final String PARM_MAINT_INTERVKM_1       = PARM_MAINT_INTERVKM_+"1";
    public  static final String PARM_MAINT_LASTKM_1         = PARM_MAINT_LASTKM_+"1";
    public  static final String PARM_MAINT_NEXTKM_1         = PARM_MAINT_NEXTKM_+"1";
    public  static final String PARM_MAINT_RESETKM_1        = PARM_MAINT_RESETKM_+"1";
    public  static final String PARM_MAINT_INTERVHR_        = "d_mntintrhr";
    public  static final String PARM_MAINT_LASTHR_          = "d_mntlasthr";    // read-only
    public  static final String PARM_MAINT_RESETHR_         = "d_mntreshr";      // checkbox
    public  static final String PARM_MAINT_INTERVHR_0       = PARM_MAINT_INTERVHR_+"0";
    public  static final String PARM_MAINT_LASTHR_0         = PARM_MAINT_LASTHR_+"0";
    public  static final String PARM_MAINT_RESETHR_0        = PARM_MAINT_RESETHR_+"0";
    public  static final String PARM_MAINT_NOTES            = "d_mntnotes";

    public  static final String PARM_DEV_RULE_ALLOW         = "d_ruleallw";
    public  static final String PARM_DEV_RULE_EMAIL         = "d_rulemail";
    public  static final String PARM_DEV_RULE_SEL           = "d_rulesel";
    public  static final String PARM_DEV_RULE_DESC          = "d_ruledesc";
    public  static final String PARM_DEV_RULE_SUBJ          = "d_rulesubj";
    public  static final String PARM_DEV_RULE_TEXT          = "d_ruletext";
  //public  static final String PARM_DEV_RULE_WRAP          = "d_rulewrap";
    public  static final String PARM_LAST_ALERT_TIME        = "d_alertTime";
    public  static final String PARM_LAST_ALERT_RESET       = "d_alertReset";

    public  static final String PARM_ACTIVE_CORRIDOR        = "d_actvcorr";

  //public  static final String PARM_WORKORDER_ID           = "d_workid";

    public  static final String PARM_DEV_CUSTOM_            = "d_c_";

    // ------------------------------------------------------------------------
    // Device command handlers

    private static Map<String,DeviceCmdHandler> DCMap = new HashMap<String,DeviceCmdHandler>();
    private static DeviceCmd_SMS                SMSCommandHandler = null;

    static {
        _initDeviceCommandHandlers();
    };

    private static void _addDeviceCommandHandler(DeviceCmdHandler dch)
    {
        String servID = dch.getServerID();
        if (!DCMap.containsKey(servID)) {
            Print.logDebug("Installing DeviceCmdHandler: %s", servID);
            DCMap.put(servID, dch);
        } else {
            Print.logStackTrace("Duplicate ServerID found: " + servID + " ["+StringTools.className(dch)+"]");
        }
    }

    private static void _initDeviceCommandHandlers()
    {

        /* list of possible DeviceCmdHandler UI pages */
        // NOTE: Not all of these DeviceCmd_<DCS> pages may be available.
        String optDevCmdHandlerClasses[] = new String[] {
            "DeviceCmd_antares",
            "DeviceCmd_calamp",     // "DeviceCmd_calamp:1,2,3"
            "DeviceCmd_eloc",
            "DeviceCmd_gosafe",
            "DeviceCmd_qgl200",
            "DeviceCmd_qgv100",
            "DeviceCmd_qgv200",
            "DeviceCmd_qgv300",
            "DeviceCmd_enfora",
            "DeviceCmd_gtsdmtp",    // always available
            "DeviceCmd_pgt3000",
            "DeviceCmd_teltonika",
            "DeviceCmd_telgh3000",
            "DeviceCmd_xirgo",
        };
        String devCmdPackage = DeviceCmd_gtsdmtp.class.getPackage().getName(); // always present

        /* add available Device command pages */
        for (String _dchn : optDevCmdHandlerClasses) {

            /* separate any class:args */
            // DeviceInfo.DeviceCmdAlternate.calamp=1,2,3
            int p = _dchn.indexOf(":");
            String dcn = (p >= 0)? _dchn.substring(0,p).trim() : _dchn;
            String dca = (p >= 0)? _dchn.substring(p+1).trim() : null;
            String dchArgs[] = RTConfig.getStringArray(DBConfig.PROP_DeviceInfo_DeviceCmdAlternate_+dcn, null);
            if (ListTools.isEmpty(dchArgs) && !StringTools.isBlank(dca)) {
                dchArgs = StringTools.split(dca,',');
            }

            /* get class */
            String dchClassName = devCmdPackage + "." + dcn;
            Class  dchClass     = null;
            try {
                //Print.logInfo("Getting Class.forName: " + dchClassName);
                dchClass = Class.forName(dchClassName);
            } catch (Throwable th) { // ClassNotFoundException
                // quietly ignore
                //Print.logWarn("DeviceCmdHandler UI class not found: " + dchClassName);
                continue;
            }

            /* load command handler UI modules */
            try {

                // add standard DCS name
                DeviceCmdHandler stdDCH = (DeviceCmdHandler)dchClass.newInstance();
                _addDeviceCommandHandler(stdDCH);
                //Print.logInfo("Added standard command handler: " + dcn);

                // check for additional DCS name args
                if (!ListTools.isEmpty(dchArgs)) {
                    for (int a = 0; a < dchArgs.length; a++) {
                        String arg = StringTools.trim(dchArgs[a]);
                        if (!StringTools.isBlank(arg)) {
                            DeviceCmdHandler argDCH = (DeviceCmdHandler)dchClass.newInstance();
                            argDCH.setServerIDArg(arg);
                            _addDeviceCommandHandler(argDCH);
                            Print.logInfo("Added custom command handler: " + dcn + " [" + arg + "]");
                        }
                    }
                }

            } catch (Throwable th) { // ClassNotFoundException, ...
                Print.logException("DeviceCmdHandler UI instantiation error: " + dchClassName, th);
            }

        }

        /* SMS */
        SMSCommandHandler = new DeviceCmd_SMS();

    }
    
    private DeviceCmdHandler getDeviceCommandHandler(String dcid)
    {
        DCServerConfig dcs = DCServerFactory.getServerConfig(dcid);
        if (dcs != null) {
            // found a DCS configuration
            return DCMap.get(dcid); // may still be null
        } else {
            // no DCS configuration found
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // WebPage interface
    
    public DeviceInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_DEVICE_INFO);
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
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getMenuDescription(reqState,i18n.getString("DeviceInfo.editMenuDesc","View/Edit {0} Information", devTitles));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getMenuHelp(reqState,i18n.getString("DeviceInfo.editMenuHelp","View and Edit {0} information", devTitles));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getNavigationDescription(reqState,i18n.getString("DeviceInfo.navDesc","{0}", devTitles));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return i18n.getString("DeviceInfo.navTab","{0} Admin", devTitles);
    }

    // ------------------------------------------------------------------------
    
    public String[] getChildAclList()
    {
        return _ACL_LIST;
    }

    // ------------------------------------------------------------------------

    private String _filterPhoneNum(String simPhone)
    {
        if (StringTools.isBlank(simPhone)) {
            return "";
        } else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < simPhone.length(); i++) {
                char ch = simPhone.charAt(i);
                if (Character.isDigit(ch)) {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
    }

    private boolean _showNotificationFields(PrivateLabel privLabel)
    {
        String propShowNotify = PrivateLabel.PROP_DeviceInfo_showNotificationFields;
        String snf = (privLabel != null)? privLabel.getStringProperty(propShowNotify,null) : null;

        /* has notification fields? */
        if (!Device.supportsNotification()) {
            if ((snf != null) && snf.equalsIgnoreCase(StringTools.STRING_TRUE)) {
                Print.logWarn("Property "+propShowNotify+" is 'true', but Device notification fields are not supported");
            }
            return false;
        }

        /* show notification fields? */
        if (StringTools.isBlank(snf) || snf.equalsIgnoreCase("default")) {
            return Device.hasRuleFactory(); // default value
        } else {
            return StringTools.parseBoolean(snf,false);
        }
        
    }

    /*
    private boolean _showWorkOrderID(PrivateLabel privLabel)
    {
        String propShowWorkOrder = PrivateLabel.PROP_DeviceInfo_showWorkOrderField;
        String swo = (privLabel != null)? privLabel.getStringProperty(propShowWorkOrder,null) : null;

        //* has workOrderID field?
        if (!Device.getFactory().hasField(Device.FLD_workOrderID)) {
            if ((swo != null) && swo.equalsIgnoreCase(StringTools.STRING_TRUE)) {
                Print.logWarn("Property "+propShowWorkOrder+" is 'true', but Device workOrderID not supported");
            }
            return false;
        }

        //* show workOrder field?
        if (StringTools.isBlank(swo) || swo.equalsIgnoreCase("default")) {
            return true; // default value
        } else {
            return StringTools.parseBoolean(swo,false);
        }

    }
    */

    private int _allowNewDevice(Account currAcct, PrivateLabel privLabel)
    {
        if (currAcct == null) {
            // always deny if no account
            Print.logDebug("Allow New Device? Deny - no account");
            return NEWDEV_DENY;
        } else
        if (Account.isSystemAdmin(currAcct)) {
            // always allow if "sysadmin"
            Print.logDebug("Allow New Device? Allow - SystemAdmin");
            return NEWDEV_ALLOW;
        } else {
            String globAND = privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_allowNewDevice,"");
            if (globAND.equalsIgnoreCase("sysadmin")) {
                // only allow if logged-in from "sysadmin"
                Print.logDebug("Allow New Device? Allow - if SystemAdmin");
                return NEWDEV_SYSADMIN;
            } else
            if (StringTools.parseBoolean(globAND,false) == false) {
                // explicit deny
                Print.logDebug("Allow New Device? Deny - disallowed");
                return NEWDEV_DENY;
            } else
            if (currAcct.exceedsMaximumDevices(currAcct.getDeviceCount()+1,true)) {
                // deny if over limit
                Print.logDebug("Allow New Device? Deny - over limit");
                return NEWDEV_DENY;
            } else {
                // otherwise allow
                Print.logDebug("Allow New Device? Allow - as specified");
                return NEWDEV_ALLOW;
            }
        }
    }

    /* update Device table with user entered information */
    private String _updateDeviceTable(RequestProperties reqState)
    {
        final Account      currAcct      = reqState.getCurrentAccount();
        final User         currUser      = reqState.getCurrentUser();
        final PrivateLabel privLabel     = reqState.getPrivateLabel();
        final Device       selDev        = reqState.getSelectedDevice(); // 'selDev' is non-null
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final I18N         i18n          = privLabel.getI18N(DeviceInfo.class);
        final Locale       locale        = reqState.getLocale();
        final String       devTitles[]   = reqState.getDeviceTitles();
        final boolean      showFuelEcon  = DBConfig.hasExtraPackage();
        String  msg       = null;
        boolean groupsChg = false;
        String  serverID  = AttributeTools.getRequestString(request, PARM_SERVER_ID         , "");
        String  codeVers  = AttributeTools.getRequestString(request, PARM_CODE_VERS         , "");
        String  uniqueID  = AttributeTools.getRequestString(request, PARM_DEV_UNIQ          , "");
        String  vehicleID = AttributeTools.getRequestString(request, PARM_VEHICLE_ID        , null);
        String  devActive = AttributeTools.getRequestString(request, PARM_DEV_ACTIVE        , "");
        String  devDesc   = AttributeTools.getRequestString(request, PARM_DEV_DESC          , "");
        String  devName   = AttributeTools.getRequestString(request, PARM_DEV_NAME          , "");
        String  pushpinID = AttributeTools.getRequestString(request, PARM_ICON_ID           , "");
        String  dispColor = AttributeTools.getRequestString(request, PARM_DISPLAY_COLOR     , "");
        String  equipType = AttributeTools.getRequestString(request, PARM_DEV_EQUIP_TYPE    , "");
        double  fuelCap   = AttributeTools.getRequestDouble(request, PARM_DEV_FUEL_CAP      , 0.0);
        double  fuelEcon  = AttributeTools.getRequestDouble(request, PARM_DEV_FUEL_ECON     , 0.0);
        double  speedLimU = AttributeTools.getRequestDouble(request, PARM_DEV_SPEED_LIMIT   , 0.0);
        String  driverID  = AttributeTools.getRequestString(request, PARM_DRIVER_ID         , "");
        String  imeiNum   = AttributeTools.getRequestString(request, PARM_DEV_IMEI          , "");
        String  serialNum = AttributeTools.getRequestString(request, PARM_DEV_SERIAL_NO     , "");
        String  dataKey   = AttributeTools.getRequestString(request, PARM_DATA_KEY          , "");
        String  simPhone  = this._filterPhoneNum(AttributeTools.getRequestString(request,PARM_DEV_SIMPHONE,""));
        String  smsEmail  = AttributeTools.getRequestString(request, PARM_SMS_EMAIL         , "");
        String  noteText  = AttributeTools.getRequestString(request, PARM_DEV_NOTES         , "");
        String  linkURL   = AttributeTools.getRequestString(request, PARM_LINK_URL          , "");
        String  linkDesc  = AttributeTools.getRequestString(request, PARM_LINK_DESC         , "");
        double  fixedLat  = AttributeTools.getRequestDouble(request, PARM_FIXED_LAT         , 0.0);
        double  fixedLon  = AttributeTools.getRequestDouble(request, PARM_FIXED_LON         , 0.0);
        String  ignition  = AttributeTools.getRequestString(request, PARM_IGNITION_INDEX    , "");
        String  grpKeys[] = AttributeTools.getMatchingKeys( request, PARM_DEV_GROUP_);
        String  cstKeys[] = AttributeTools.getMatchingKeys( request, PARM_DEV_CUSTOM_);
        double  rptOdom   = AttributeTools.getRequestDouble(request, PARM_REPORT_ODOM       , 0.0);
        double  rptHours  = AttributeTools.getRequestDouble(request, PARM_REPORT_HOURS      , 0.0);
        String  actvCorr  = AttributeTools.getRequestString(request, PARM_ACTIVE_CORRIDOR   , "");
      //String  worderID  = AttributeTools.getRequestString(request, PARM_WORKORDER_ID      , "");
        try {
            // unique id
            boolean editUniqID = privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_UNIQUEID));
            if (editUniqID && !selDev.getUniqueID().equals(uniqueID)) {
                if (StringTools.isBlank(uniqueID)) {
                    selDev.setUniqueID("");
                } else {
                    try {
                        Device dev = Transport.loadDeviceByUniqueID(uniqueID);
                        if (dev == null) {
                            selDev.setUniqueID(uniqueID);
                        } else {
                            String devAcctID = dev.getAccountID();
                            String devDevID  = dev.getDeviceID();
                            if (devAcctID.equals(reqState.getCurrentAccountID())) {
                                // same account, this user can fix this himself
                                msg = i18n.getString("DeviceInfo.uniqueIdAlreadyAssignedToDevice",
                                    "UniqueID is already assigned to {0}: {1}", devTitles[0], devDevID);
                                selDev.setError(msg);
                            } else {
                                // different account, this user cannot fix this himself
                                Print.logWarn("UniqueID '%s' already assigned: %s/%s", uniqueID, devAcctID, devDevID);
                                msg = i18n.getString("DeviceInfo.uniqueIdAlreadyAssigned",
                                    "UniqueID is already assigned to another Account");
                                selDev.setError(msg);
                            }
                        }
                    } catch (DBException dbe) {
                        msg = i18n.getString("DeviceInfo.errorReadingUniqueID",
                            "Error while looking for other matching UniqueIDs");
                        selDev.setError(msg);
                    }
                }
            }
            // server id
            boolean editServID = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_allowEditServerID,EDIT_SERVER_ID) && (selDev.getLastConnectTime() <= 0L);
            if (editServID && !selDev.getDeviceCode().equals(serverID) && DCServerFactory.hasServerConfig(serverID)) {
                selDev.setDeviceCode(serverID);
            }
            // active
            boolean devActv = ComboOption.parseYesNoText(locale, devActive, true);
            if (selDev.getIsActive() != devActv) { 
                selDev.setIsActive(devActv); 
            }
            // description
            if (!selDev.getDescription().equals(devDesc)) {
                selDev.setDescription(devDesc);
            }
            // display name
            if (!selDev.getDisplayName().equals(devName)) {
                selDev.setDisplayName(devName);
            }
            // vehicle ID (VIN/License)
            if ((vehicleID != null) && !selDev.getVehicleID().equals(vehicleID)) {
                selDev.setVehicleID(vehicleID);
            }
            // equipment type
            if (!selDev.getEquipmentType().equals(equipType)) {
                selDev.setEquipmentType(equipType);
            }
            // fuel capacity
            double fuelCapLiters = Account.getVolumeUnits(currAcct).convertToLiters(fuelCap);
            if (selDev.getFuelCapacity() != fuelCapLiters) {
                selDev.setFuelCapacity(fuelCapLiters);
            }
            // fuel economy (km/L)
            if (showFuelEcon) {
                double fuelEconomy = Account.getEconomyUnits(currAcct).convertToKPL(fuelEcon);
                if (selDev.getFuelEconomy() != fuelEconomy) {
                    selDev.setFuelEconomy(fuelEconomy);
                }
            }
            // Driver ID
            if (!selDev.getDriverID().equals(driverID)) {
                selDev.setDriverID(driverID);
            }
            // IMEI number
            if (!selDev.getImeiNumber().equals(imeiNum)) {
                selDev.setImeiNumber(imeiNum);
            }
            // Serial number
            if (!selDev.getSerialNumber().equals(serialNum)) {
                selDev.setSerialNumber(serialNum);
            }
            // SIM phone number
            if (!selDev.getSimPhoneNumber().equals(simPhone)) {
                selDev.setSimPhoneNumber(simPhone);
            }
            // SMS EMail address
            if (!selDev.getSmsEmail().equals(smsEmail)) { // EMail.validateAddress(smsEmail)
                selDev.setSmsEmail(smsEmail);
            }
            // Notes
            boolean notesOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showNotes,SHOW_NOTES);
            if (notesOK && !selDev.getNotes().equals(noteText)) {
                selDev.setNotes(noteText);
            }
            // Link URL/Description
            if (Device.supportsLinkURL()) {
                if (!selDev.getLinkURL().equals(linkURL)) {
                    selDev.setLinkURL(linkURL);
                }
                if (!selDev.getLinkDescription().equals(linkDesc)) {
                    selDev.setLinkDescription(linkDesc);
                }
            }
            // Fixed Latitude/Longitude
            if (Device.supportsFixedLocation()) {
                if ((fixedLat != 0.0) || (fixedLon != 0.0)) {
                    selDev.setFixedLatitude(fixedLat);
                    selDev.setFixedLongitude(fixedLon);
                }
            }
            // Ignition index
            boolean ignOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showIgnitionIndex,SHOW_IGNITION_NDX);
            if (ignOK && !StringTools.isBlank(ignition)) {
                String ign = ignition.toLowerCase();
                int ignNdx = -1;
                if (ign.equals("n/a")) {
                    ignNdx = -1;
                } else
                if (ign.startsWith("ign")) {
                    ignNdx = StatusCodes.IGNITION_INPUT_INDEX;
                } else {
                    ignNdx = StringTools.parseInt(ignition,-1);
                }
                selDev.setIgnitionIndex(ignNdx);
            }
            // speed limit
            boolean speedLimOK = Device.hasRuleFactory(); // || privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSpeedLimit,SHOW_SPEED_LIMIT);
            double speedLimKPH = Account.getSpeedUnits(currAcct).convertToKPH(speedLimU);
            if (speedLimOK && (selDev.getSpeedLimitKPH() != speedLimKPH)) {
                selDev.setSpeedLimitKPH(speedLimKPH);
            }
            // Data Key
            boolean dataKeyOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDataKey,SHOW_DATA_KEY);
            if (dataKeyOK && !selDev.getDataKey().equals(dataKey)) {
                selDev.setDataKey(pushpinID);
            }
            // Pushpin ID
            boolean ppidOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPushpinID,SHOW_PUSHPIN_ID);
            if (ppidOK && !selDev.getPushpinID().equals(pushpinID)) {
                selDev.setPushpinID(pushpinID);
            }
            // Display Color
            boolean dcolorOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDisplayColor,SHOW_DISPLAY_COLOR);
            if (dcolorOK && !selDev.getDisplayColor().equals(dispColor)) {
                selDev.setDisplayColor(dispColor);
            }
            // Reported Odometer
            if (rptOdom >= 0.0) {
                Account.DistanceUnits distUnits = Account.getDistanceUnits(currAcct);
                double rptOdomKM  = distUnits.convertToKM(rptOdom);
                double lastOdomKM = selDev.getLastOdometerKM();
                double offsetKM   = rptOdomKM - lastOdomKM;
                if (Math.abs(offsetKM - selDev.getOdometerOffsetKM()) >= 0.1) {
                    selDev.setOdometerOffsetKM(offsetKM);
                }
            }
            // Maintenance Interval
            if (Device.supportsPeriodicMaintenance()) {
                Account.DistanceUnits distUnits = Account.getDistanceUnits(currAcct);
                // Odometer Maintenance #0
                long    mIntrvKM0 = AttributeTools.getRequestLong(request, PARM_MAINT_INTERVKM_0, 0L);
                double  intrvKM0  = distUnits.convertToKM((double)mIntrvKM0);
                boolean mResetKM0 = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_MAINT_RESETKM_0,null));
                selDev.setMaintIntervalKM0(intrvKM0);
                if (mResetKM0) {
                    selDev.setMaintOdometerKM0(selDev.getLastOdometerKM());
                }
                // Odometer Maintenane #1
                long    mIntrvKM1 = AttributeTools.getRequestLong(request, PARM_MAINT_INTERVKM_1, 0L);
                double  intrvKM1  = distUnits.convertToKM((double)mIntrvKM1);
                boolean mResetKM1 = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_MAINT_RESETKM_1,null));
                selDev.setMaintIntervalKM1(intrvKM1);
                if (mResetKM1) {
                    selDev.setMaintOdometerKM1(selDev.getLastOdometerKM());
                }
                // EngineHours Maintenane #0
                double  mIntrvHR0 = AttributeTools.getRequestDouble(request, PARM_MAINT_INTERVHR_0, 0.0);
                double  intrvHR0  = mIntrvHR0;
                boolean mResetHR0 = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_MAINT_RESETHR_0,null));
                selDev.setMaintIntervalHR0(intrvHR0);
                if (mResetHR0) {
                    selDev.setMaintEngHoursHR0(selDev.getLastEngineHours());
                }
                // maintenance notes
                boolean maintNotesOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceNotes,SHOW_MAINTENANCE_NOTES);
                String  maintText = AttributeTools.getRequestString(request, PARM_MAINT_NOTES   , "");
                if (maintNotesOK && !selDev.getMaintNotes().equals(maintText)) {
                    selDev.setMaintNotes(maintText);
                }
            }
            // Rule Engine Notification
            if (_showNotificationFields(privLabel)) {
                String  ruleAllow   = AttributeTools.getRequestString( request, PARM_DEV_RULE_ALLOW  , null);
                String  notifyEmail = AttributeTools.getRequestString( request, PARM_DEV_RULE_EMAIL  , null);
                boolean alertReset  = AttributeTools.getRequestBoolean(request, PARM_LAST_ALERT_RESET, false);
                String  ruleSel     = AttributeTools.getRequestString( request, PARM_DEV_RULE_SEL    ,  null);
                String  ruleDesc    = AttributeTools.getRequestString( request, PARM_DEV_RULE_DESC   ,  null);
                String  ruleSubj    = AttributeTools.getRequestString( request, PARM_DEV_RULE_SUBJ   ,  null);
                String  ruleText    = AttributeTools.getRequestString( request, PARM_DEV_RULE_TEXT   ,  null);
              //String  ruleWrap    = AttributeTools.getRequestString( request, PARM_DEV_RULE_WRAP   ,  null);
                 // Allow Notification
                boolean allowNtfy = ComboOption.parseYesNoText(locale, ruleAllow, true);
                if (selDev.getAllowNotify() != allowNtfy) { 
                    selDev.setAllowNotify(allowNtfy); 
                }
                // Notification email
                if (notifyEmail != null) {
                    if (StringTools.isBlank(notifyEmail)) {
                        if (!selDev.getNotifyEmail().equals(notifyEmail)) {
                            selDev.setNotifyEmail(notifyEmail);
                        }
                    } else
                    if (EMail.validateAddresses(notifyEmail,true)) {
                        if (!selDev.getNotifyEmail().equals(notifyEmail)) {
                            selDev.setNotifyEmail(notifyEmail);
                        }
                    } else {
                        msg = i18n.getString("DeviceInfo.enterEMail","Please enter a valid notification email/sms address");
                        selDev.setError(msg);
                    }
                }
               // notification selector
                if (ruleSel != null) {
                    if (selDev.checkSelectorSyntax(ruleSel)) {
                        // update rule selector (if changed)
                        if (!selDev.getNotifySelector().equals(ruleSel)) {
                            selDev.setNotifySelector(ruleSel);
                        }
                        //selDev.setAllowNotify(!StringTools.isBlank(ruleSel));
                    } else {
                        Print.logInfo("Notification selector has a syntax error: " + ruleSel);
                        msg = i18n.getString("DeviceInfo.ruleError","Notification rule contains a syntax error");
                        selDev.setError(msg);
                    }
                }
                // notification description
                if (ruleDesc != null) {
                    if (!selDev.getNotifyDescription().equals(ruleDesc)) {
                        selDev.setNotifyDescription(ruleDesc);
                    }
                }
                // notification subject
                if (ruleSubj != null) {
                    if (!selDev.getNotifySubject().equals(ruleSubj)) {
                        selDev.setNotifySubject(ruleSubj);
                    }
                }
                // notification message
                if (ruleText != null) {
                    if (!selDev.getNotifyText().equals(ruleText)) {
                        selDev.setNotifyText(ruleText);
                    }
                }
                // notify wrapper
                boolean ntfyWrap = false; // ComboOption.parseYesNoText(locale, ruleWrap, true);
                if (selDev.getNotifyUseWrapper() != ntfyWrap) { 
                    selDev.setNotifyUseWrapper(ntfyWrap); 
                }
                // last-notify-time reset
                if ((selDev.getLastNotifyTime() != 0L) && alertReset) {
                    selDev.setLastNotifyTime(0L);
                    selDev.setLastNotifyCode(StatusCodes.STATUS_NONE);
                }
            }
            // Active Corridor
            if (Device.supportsActiveCorridor()) {
                if (!selDev.getActiveCorridor().equals(actvCorr)) {
                    selDev.setActiveCorridor(actvCorr);
                }
            }
            // WorkOrder ID
            /*
            if (_showWorkOrderID(privLabel)) {
                if (!selDev.getWorkOrderID().equals(worderID)) {
                    selDev.setWorkOrderID(worderID);
                }
            }
            */
            // Custom Attributes
            if (!ListTools.isEmpty(cstKeys)) {
                String oldCustAttr = selDev.getCustomAttributes();
                RTProperties rtp = selDev.getCustomAttributesRTP();
                for (int i = 0; i < cstKeys.length; i++) {
                    String cstKey = cstKeys[i];
                    String rtpVal = AttributeTools.getRequestString(request, cstKey, "");
                    String rtpKey = cstKey.substring(PARM_DEV_CUSTOM_.length());
                    rtp.setString(rtpKey, rtpVal);
                }
                String rtpStr = rtp.toString();
                if (!rtpStr.equals(oldCustAttr)) {
                    //Print.logInfo("Setting custom attributes: " + rtpStr);
                    selDev.setCustomAttributes(rtpStr);
                }
            }
            // DeviceGroups
            if (!selDev.hasError()) {
                String accountID = selDev.getAccountID();
                String deviceID  = selDev.getDeviceID();
                // 'grpKey' may only contain 'checked' items!
                OrderedSet<String> fullGroupSet = reqState.getDeviceGroupIDList(true);
                // add checked groups
                if (!ListTools.isEmpty(grpKeys)) {
                    for (int i = 0; i < grpKeys.length; i++) {
                        String grpID = grpKeys[i].substring(PARM_DEV_GROUP_.length());
                        if (!grpID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                            String chkStr = AttributeTools.getRequestString(request,grpKeys[i],"");
                            boolean chked = chkStr.equalsIgnoreCase("on");
                            boolean exists = DeviceGroup.isDeviceInDeviceGroup(accountID, grpID, deviceID);
                            //Print.logInfo("Checking group : " + grpID + " [checked=" + chked + "]");
                            if (chked) {
                                if (!exists) {
                                    DeviceGroup.addDeviceToDeviceGroup(accountID, grpID, deviceID);
                                    groupsChg = true;
                                }
                            } else {
                                if (exists) {
                                    DeviceGroup.removeDeviceFromDeviceGroup(accountID, grpID, deviceID);
                                    groupsChg = true;
                                }
                            }
                            fullGroupSet.remove(grpID);
                        }
                    }
                }
                // delete remaining (unchecked) groups
                for (Iterator i = fullGroupSet.iterator(); i.hasNext();) {
                    String grpID = (String)i.next();
                    if (!grpID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                        boolean exists = DeviceGroup.isDeviceInDeviceGroup(accountID, grpID, deviceID);
                        //Print.logInfo("Removing group: " + grpID + " [" + exists + "]");
                        if (exists) {
                            DeviceGroup.removeDeviceFromDeviceGroup(accountID, grpID, deviceID);
                            groupsChg = true;
                        }
                    }
                }
            }
            // save
            if (selDev.hasError()) {
                // should stay on same page
                Print.logInfo("An error occured during Edit ...");
            } else
            if (selDev.hasChanged()) {
                selDev.save();
                msg = i18n.getString("DeviceInfo.updatedDevice","{0} information updated", devTitles);
            } else
            if (groupsChg) {
                String grpTitles[] = reqState.getDeviceGroupTitles();
                msg = i18n.getString("DeviceInfo.updatedDeviceGroups","{0} membership updated", grpTitles);
            } else {
                // nothing changed
                Print.logInfo("Nothing has changed for this Device ...");
            }
        } catch (Throwable t) {
            Print.logException("Updating Device", t);
            msg = i18n.getString("DeviceInfo.errorUpdate","Internal error updating {0}", devTitles);
            selDev.setError(msg);
        }
        return msg;
    }

    // ------------------------------------------------------------------------

    /* write html */
    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel   = reqState.getPrivateLabel();
        final I18N         i18n        = privLabel.getI18N(DeviceInfo.class);
        final Locale       locale      = reqState.getLocale();
        final String       devTitles[] = reqState.getDeviceTitles();
        final String       grpTitles[] = reqState.getDeviceGroupTitles();
        final Account      currAcct    = reqState.getCurrentAccount();
        final User         currUser    = reqState.getCurrentUser(); // may be null
        final String       pageName    = this.getPageName();
        String m = pageMsg;
        boolean error = false;
        String cmdBtnTitle = i18n.getString("DeviceInfo.commands","Commands"); // included here to maintain I18N string

        /* device */
        OrderedSet<String> devList = reqState.getDeviceIDList(true/*inclInactv*/);
        if (devList == null) { devList = new OrderedSet<String>(); }
        Device selDev   = reqState.getSelectedDevice();
        String selDevID = (selDev != null)? selDev.getDeviceID() : "";

        /* new device creation */
        final int allowNewDevMode = this._allowNewDevice(currAcct, privLabel);
        boolean allowNew = privLabel.hasAllAccess(currUser,this.getAclName()) && (allowNewDevMode != NEWDEV_DENY);
        if (allowNew && (allowNewDevMode == NEWDEV_SYSADMIN) && !reqState.isLoggedInFromSysAdmin()) {
            // disallow new device creation if the runtime config indicates this is only allowed for "sysadmin" login transfer
            allowNew = false;
        }

        /* ACL allow edit/view */
        boolean allowDelete  = allowNew;
        boolean allowEdit    = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView    = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* "Commands"("Properties") */
        boolean allowCommand = allowView && privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_COMMANDS)) &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPropertiesButton,true);
        //Print.logDebug("Allow 'Commands' = " + allowCommand);

        /* "SMS" */
        boolean allowSms     = allowView && privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_SMS)) &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSmsButton,false) && 
            (SMSCommandHandler != null) && SMSCommandHandler.hasSmsCommands(reqState);

        /* submit buttons */
        String  submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String  submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String  submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String  submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String  submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");
        String  submitQueue  = AttributeTools.getRequestString(request, PARM_SUBMIT_QUE , "");
        String  submitProps  = AttributeTools.getRequestString(request, PARM_SUBMIT_PROP, "");
        String  submitSms    = AttributeTools.getRequestString(request, PARM_SUBMIT_SMS , "");

        /* ACL view/edit rules/notification */
        boolean editRules    = privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_RULES));
        boolean viewRules    = editRules || privLabel.hasReadAccess(currUser, this.getAclName(_ACL_RULES));

        /* command */
        String  deviceCmd    = reqState.getCommandName();
      //boolean refreshList  = deviceCmd.equals(COMMAND_INFO_REFRESH);
        boolean selectDevice = deviceCmd.equals(COMMAND_INFO_SEL_DEVICE);
        boolean newDevice    = deviceCmd.equals(COMMAND_INFO_NEW_DEVICE);
        boolean updateDevice = deviceCmd.equals(COMMAND_INFO_UPD_DEVICE);
        boolean sendCommand  = deviceCmd.equals(COMMAND_INFO_UPD_PROPS);
        boolean updateSms    = deviceCmd.equals(COMMAND_INFO_UPD_SMS);
        boolean deleteDevice = false;

        /* ui display */
        boolean uiList       = false;
        boolean uiEdit       = false;
        boolean uiView       = false;
        boolean uiCmd        = false;
        boolean uiSms        = false;

        /* DeviceCmdHandler */
        DeviceCmdHandler dcHandler = null;

        /* pre-qualify commands */
        String newDeviceID = null;
        if (newDevice) {
            if (!allowNew) {
                newDevice = false; // not authorized
            } else {
                if (allowNewDevMode == NEWDEV_SYSADMIN) {
                    // TODO: need to check some "Create Device Password" field
                }
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newDeviceID = AttributeTools.getRequestString(httpReq,PARM_NEW_NAME,"").trim();
                newDeviceID = newDeviceID.toLowerCase();
                if (StringTools.isBlank(newDeviceID)) {
                    m = i18n.getString("DeviceInfo.enterNewDevice","Please enter a new {0} ID.", devTitles);
                    error = true;
                    newDevice = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_DeviceInfo_validateNewIDs,*/newDeviceID)) {
                    m = i18n.getString("DeviceInfo.invalidIDChar","ID contains invalid characters");
                    error = true;
                    newDevice = false;
                }
            }
        } else
        if (updateDevice) {
            if (!allowEdit) {
                updateDevice = false; // not authorized
            } else
            if (!SubmitMatch(submitChange,i18n.getString("DeviceInfo.change","Change"))) {
                updateDevice = false;
            } else
            if (selDev == null) {
                // should not occur
                m = i18n.getString("DeviceInfo.unableToUpdate","Unable to update Device, ID not found");
                error = true;
                updateDevice = false;
            }
        } else
        if (sendCommand) {
            if (!allowCommand) {
                sendCommand = false; // not authorized
            } else
            if (!SubmitMatch(submitQueue,i18n.getString("DeviceInfo.queue","Queue"))) {
                sendCommand = false; // button not pressed
            } else
            if (selDev == null) {
                m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles);
                error = true;
                sendCommand = false; // no device selected
            } else
            if (StringTools.isBlank(selDev.getDeviceCode())) {
                Print.logInfo("DeviceCode/ServerID is blank");
                sendCommand = false;
            } else {
                dcHandler = this.getDeviceCommandHandler(selDev.getDeviceCode());
                if (dcHandler == null) {
                    Print.logWarn("DeviceCmdHandler not found: " + selDev.getDeviceCode());
                    sendCommand = false; // not found
                } else
                if (!dcHandler.deviceSupportsCommands(selDev)) {
                    Print.logWarn("DeviceCode/ServerID not supported by handler: " + selDev.getDeviceCode());
                    sendCommand = false; // not supported
                }
            }
        } else
        if (updateSms) {
            if (!allowSms) {
                updateSms = false; // not authorized
            } else
            if (!SubmitMatch(submitQueue,i18n.getString("DeviceInfo.queue","Queue"))) {
                updateSms = false; // button not pressed
            } else
            if (selDev == null) {
                m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles);
                error = true;
                updateSms = false; // no device selected
            //} else
            //if (StringTools.isBlank(selDev.getDeviceCode())) {
            //    Print.logInfo("DeviceCode/ServerID is blank");
            //    updateSms = false;
            } else {
                dcHandler = SMSCommandHandler;
                if (dcHandler == null) {
                    Print.logWarn("SMS DeviceCmdHandler not found");
                    updateSms = false; // not found
                } else
                if (!dcHandler.deviceSupportsCommands(selDev)) {
                    Print.logWarn("DeviceCode/ServerID not supported by handler: " + selDev.getDeviceCode());
                    updateSms = false; // not supported
                }
            }
        } else
        if (selectDevice) {
            if (SubmitMatch(submitDelete,i18n.getString("DeviceInfo.delete","Delete"))) {
                if (!allowDelete) {
                    deleteDevice = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles);
                    error = true;
                    deleteDevice = false; // not selected
                } else {
                    deleteDevice = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("DeviceInfo.edit","Edit"))) {
                if (!allowEdit) {
                    uiEdit = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles);
                    error = true;
                    uiEdit = false; // not selected
                } else {
                    uiEdit = true;
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("DeviceInfo.view","View"))) {
                if (!allowView) {
                    uiView = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles);
                    error = true;
                    uiView = false; // not selected
                } else {
                    uiView = true;
                }
            } else
            if (SubmitMatch(submitProps,i18n.getString("DeviceInfo.properties","Commands"))) {
                if (!allowCommand) {
                    uiCmd = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles);
                    error = true;
                    uiCmd = false; // not selected
                } else {
                    String dcsName = selDev.getDeviceCode();
                    dcHandler = this.getDeviceCommandHandler(dcsName);
                    if (dcHandler == null) {
                        if (StringTools.isBlank(dcsName)) {
                            Print.logWarn("ServerID is blank");
                            m = i18n.getString("DeviceInfo.deviceCommandsNotDefined","Unknown {0} type (blank)", devTitles);
                        } else {
                            Print.logWarn("DeviceCmdHandler not found: " + dcsName);
                            m = i18n.getString("DeviceInfo.deviceCommandsNotFound","{0} not supported", devTitles);
                        }
                        error = true;
                        uiCmd = false; // not supported / blank
                    } else
                    if (!dcHandler.deviceSupportsCommands(selDev)) {
                        Print.logWarn("ServerID does not support Device commands: " + dcsName);
                        m = i18n.getString("DeviceInfo.deviceCommandsNotSupported","{0} Commands not supported", devTitles);
                        error = true;
                        uiCmd = false; // not supported
                    } else {
                        uiCmd = true;
                    }
                }
            } else
            if (SubmitMatch(submitSms,i18n.getString("DeviceInfo.sms","SMS"))) {
                if (!allowSms) {
                    uiSms = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles);
                    error = true;
                    uiSms = false; // not selected
                } else {
                    dcHandler = SMSCommandHandler;
                    if (dcHandler == null) {
                        Print.logWarn("SMS DeviceCmdHandler not found.");
                        m = i18n.getString("DeviceInfo.deviceSmsNotSupported","{0} SMS not supported", devTitles);
                        error = true;
                        uiSms = false; // not supported
                    } else
                    if (!dcHandler.deviceSupportsCommands(selDev)) {
                        Print.logWarn("SMS not supported by device");
                        m = i18n.getString("DeviceInfo.deviceSmsNotSupported","{0} SMS not supported", devTitles);
                        error = true;
                        uiSms = false; // not supported
                    } else {
                        uiSms = true;
                    }
                }
            }
        }

        /* delete device? */
        if (deleteDevice) {
            // 'selDev' guaranteed non-null here
            try {
                // delete device
                Device.Key devKey = (Device.Key)selDev.getRecordKey();
                Print.logWarn("Deleting Device: " + devKey);
                devKey.delete(true); // will also delete dependencies
                selDevID = "";
                selDev = null;
                reqState.clearDeviceList();
                // select another device
                devList = reqState.getDeviceIDList(true/*inclInactv*/);
                if (!ListTools.isEmpty(devList)) {
                    selDevID = devList.get(0);
                    try {
                        selDev = !selDevID.equals("")? Device.getDevice(currAcct, selDevID) : null; // may still be null
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Deleting Device", dbe);
                m = i18n.getString("DeviceInfo.errorDelete","Internal error deleting {0}", devTitles);
                error = true;
            }
            uiList = true;
        }

        /* new device? */
        if (newDevice) {
            boolean createDeviceOK = true;
            for (int d = 0; d < devList.size(); d++) {
                if (newDeviceID.equalsIgnoreCase(devList.get(d))) {
                    m = i18n.getString("DeviceInfo.alreadyExists","This {0} already exists", devTitles);
                    error = true;
                    createDeviceOK = false;
                    break;
                }
            }
            if (createDeviceOK) {
                try {
                    Device device = Device.createNewDevice(currAcct, newDeviceID, null); // already saved
                    reqState.clearDeviceList();
                    devList  = reqState.getDeviceIDList(true/*inclInactv*/);
                    selDev   = device;
                    selDevID = device.getDeviceID();
                    m = i18n.getString("DeviceInfo.createdUser","New {0} has been created", devTitles);
                } catch (DBException dbe) {
                    Print.logException("Deleting Creating", dbe);
                    m = i18n.getString("DeviceInfo.errorCreate","Internal error creating {0}", devTitles);
                    error = true;
                }
            }
            uiList = true;
        }

        /* update the device info? */
        if (updateDevice) {
            // 'selDev' guaranteed non-null here
            selDev.clearChanged();
            m = _updateDeviceTable(reqState);
            if (selDev.hasError()) {
                // stay on this page
                uiEdit = true;
            } else {
                uiList = true;
            }
        }

        /* send command */
        if (sendCommand) {
            // 'selDev' and 'dcHandler' guaranteed non-null here
            m = dcHandler.handleDeviceCommands(reqState, selDev);
            Print.logInfo("Returned Message: " + m);
            error = true;
            uiList = true;
        }

        /* update SMS */
        if (updateSms) {
            // 'selDev' and 'dcHandler' guaranteed non-null here
            m = dcHandler.handleDeviceCommands(reqState, selDev);
            Print.logInfo("Returned Message: " + m);
            error = true;
            uiList = true;
        }

        /* last event from device */
        try {
            EventData evd[] = (selDev != null)? selDev.getLatestEvents(1L,false) : null;
            if ((evd != null) && (evd.length > 0)) {
                reqState.setLastEventTime(new DateTime(evd[0].getTimestamp()));
            }
        } catch (DBException dbe) {
            // ignore
        }

        /* PushpinChooser */
        final boolean showPushpinChooser = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPushpinChooser,false);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = DeviceInfo.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "DeviceInfo.css", cssDir);
                if (showPushpinChooser) {
                    WebPageAdaptor.writeCssLink(out, reqState, "PushpinChooser.css", cssDir);
                }
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("DeviceInfo.js"), request);
                if (showPushpinChooser) {
                    PushpinIcon.writePushpinChooserJS(out, reqState, true);
                }
            }
        };

        /* Content */
        final Device _selDev        = selDev; // may be null !!!
        final OrderedSet<String> _deviceList = devList;
        final String _selDevID      = selDevID ;
        final boolean _allowEdit    = allowEdit;
        final boolean _allowView    = allowView;
        final boolean _allowCommand = allowCommand;
        final boolean _allowSms     = allowSms ;
        final boolean _allowNew     = allowNew ;
        final boolean _allowDelete  = allowDelete;
        final boolean _uiCmd        = _allowCommand && uiCmd ;
        final boolean _uiSms        = _allowSms     && uiSms ;
        final boolean _uiEdit       = _allowEdit    && uiEdit;
        final boolean _uiView       = _uiEdit || uiView;
        final boolean _uiList       = uiList || (!_uiEdit && !_uiView && !_uiCmd && !_uiSms);
        HTMLOutput HTML_CONTENT     = null;
        if (_uiList) {

            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String selectURL  = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String refreshURL = DeviceInfo.this.encodePageURL(reqState); // TODO: add "refresh" page command
                    //String editURL    = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    
                    /* show expected ACKs */
                    boolean showAcks = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showExpectedAcks,false);

                    // frame header
                    String frameTitle = _allowEdit? 
                        i18n.getString("DeviceInfo.viewEditDevice","View/Edit {0} Information", devTitles) : 
                        i18n.getString("DeviceInfo.viewDevice","View {0} Information", devTitles);
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span>&nbsp;\n");
                    out.write("<a href='"+refreshURL+"'><span class=''>"+i18n.getString("DeviceInfo.refresh","Refresh")+"</a>\n");
                    out.write("<br/>\n");
                    out.write("<hr>\n");

                    // device selection table (Select, Device ID, Description, ...)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("DeviceInfo.selectDevice","Select a {0}",devTitles)+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_DEVICE_SELECT+"' method='post' action='"+selectURL+"' target='_self'>"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SEL_DEVICE+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing='0' cellpadding='0' border='0'>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='" +CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+FilterText(i18n.getString("DeviceInfo.select","Select"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.deviceID","{0} ID",devTitles))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.uniqueID","Unique ID"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.decription","Description",devTitles))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.devEquipType","Equipment\nType"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.simPhoneNumber","SIM Phone#"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.devServerID","Server ID"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.ignitionState","Ignition\nState"))+"</th>\n");
                    if (showAcks) {
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.ackExpected","Expecting\nACK"))+"</th>\n");
                    }
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.active","Active"))+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    Device deviceRcd[] = new Device[_deviceList.size()];
                    int hasCommandHandlers = 0;
                    for (int d = 0; d < _deviceList.size(); d++) {
                        String rowClass = ((d & 1) == 0)? 
                            CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD : CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN;
                        out.write("  <tr class='"+rowClass+"'>\n");
                        try {
                            Device dev = Device.getDevice(currAcct, _deviceList.get(d));
                            if (dev != null) {
                                String deviceID     = FilterText(dev.getDeviceID());
                                String uniqueID     = FilterText(dev.getUniqueID());
                                String deviceDesc   = FilterText(dev.getDescription());
                                String equipType    = FilterText(dev.getEquipmentType());
                                String imeiNum      = FilterText(dev.getImeiNumber());
                                String simPhone     = FilterText(dev.getSimPhoneNumber());
                                String devCode      = FilterText(DCServerFactory.getServerConfigDescription(dev.getDeviceCode()));
                                int    ignState     = dev.getIgnitionState();
                                String ignDesc      = "";
                                String ignColor     = "black";
                                switch (ignState) {
                                    case  0: 
                                        ignDesc  = FilterText(i18n.getString("DeviceInfo.ignitionOff"    , "Off"));
                                        ignColor = ColorTools.BLACK.toString(true);
                                        break;
                                    case  1: 
                                        ignDesc  = FilterText(i18n.getString("DeviceInfo.ignitionOn"     , "On"));
                                        ignColor = ColorTools.GREEN.toString(true);
                                        break;
                                    default: 
                                        ignDesc  = FilterText(i18n.getString("DeviceInfo.ignitionUnknown", "Unknown"));
                                        ignColor = ColorTools.DARK_YELLOW.toString(true);
                                        break;
                                }
                                String pendingACK   = FilterText(dev.isExpectingCommandAck()?ComboOption.getYesNoText(locale,true):"");
                                String active       = FilterText(ComboOption.getYesNoText(locale,dev.isActive()));
                                String checked      = _selDevID.equals(dev.getDeviceID())? "checked" : "";
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+d+"'><input type='radio' name='"+PARM_DEVICE+"' id='"+deviceID+"' value='"+deviceID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+deviceID+"'>"+deviceID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+uniqueID+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+deviceDesc+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+equipType+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+simPhone+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+devCode+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap style='color:"+ignColor+"'>"+ignDesc+"</td>\n");
                                if (showAcks) {
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap style='color:red'>"+pendingACK+"</td>\n");
                                }
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+active+"</td>\n");
                                String serverID = dev.getDeviceCode();
                                if (StringTools.isBlank(serverID)) {
                                    // ignore
                                } else
                                if (DeviceInfo.this.getDeviceCommandHandler(serverID) != null) {
                                    Print.logInfo("Found DeviceCommandHandler: " + serverID);
                                    hasCommandHandlers++;
                                } else {
                                    Print.logInfo("DeviceCommandHandler not found: " + serverID);
                                }
                            }
                        } catch (DBException dbe) {
                            deviceRcd[d] = null;
                        }
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("DeviceInfo.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("DeviceInfo.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowCommand) { 
                        if (hasCommandHandlers > 0) {
                            out.write("<td style='padding-left:5px;'>");
                            out.write("<input type='submit' name='"+PARM_SUBMIT_PROP+"' value='"+i18n.getString("DeviceInfo.properties","Commands")+"'>");
                            out.write("</td>\n"); 
                        } else {
                            Print.logWarn("No matching DeviceCommandHandlers found"); 
                        }
                    }
                    if (_allowSms) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_SMS +"' value='"+i18n.getString("DeviceInfo.sms","SMS")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL +"' value='"+i18n.getString("DeviceInfo.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;");
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");

                    /* new device */
                    if (_allowNew) {
                    String createText = i18n.getString("DeviceInfo.createNewDevice","Create a new device");
                    if (allowNewDevMode == NEWDEV_SYSADMIN) {
                        createText += " " + i18n.getString("DeviceInfo.sysadminOnly","(System Admin only)");
                    }
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+createText+":</h1>\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form name='"+FORM_DEVICE_NEW+"' method='post' action='"+newURL+"' target='_self'>"); // target='_top'
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW_DEVICE+"'/>");
                    out.write(i18n.getString("DeviceInfo.deviceID","{0} ID",devTitles)+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                    out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("DeviceInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    }

                }
            };

        } else
        if (_uiEdit || _uiView) {

            final boolean _editUniqID  = _uiEdit && privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_UNIQUEID));
            final boolean _viewUniqID  = _editUniqID || privLabel.hasReadAccess(currUser, this.getAclName(_ACL_UNIQUEID));
            final boolean _editRules   = _uiEdit && editRules;
            final boolean _viewRules   = _uiView && viewRules;
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String editURL       = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    boolean ntfyOK       = _viewRules && _showNotificationFields(privLabel);
                    boolean ntfyEdit     = ntfyOK && _editRules;
                    boolean ignOK        = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showIgnitionIndex,SHOW_IGNITION_NDX);
                    boolean ppidOK       = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPushpinID,SHOW_PUSHPIN_ID);
                    boolean dcolorOK     = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDisplayColor,SHOW_DISPLAY_COLOR);
                    boolean notesOK      = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showNotes,SHOW_NOTES);
                    boolean maintNotesOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceNotes,SHOW_MAINTENANCE_NOTES);
                    boolean dataKeyOK    = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDataKey,SHOW_DATA_KEY);
                    boolean speedLimOK   = Device.hasRuleFactory(); // || privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSpeedLimit,SHOW_SPEED_LIMIT);
                    boolean showFuelEcon = DBConfig.hasExtraPackage();
                    boolean fixLocOK     = (_selDev != null) && Device.supportsFixedLocation() && 
                        privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showFixedLocation,SHOW_FIXED_LOCATION);
                    boolean editServID   = ((_selDev == null) || (_selDev.getLastConnectTime() <= 0L)) &&
                        privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_allowEditServerID,EDIT_SERVER_ID);

                    /* distance units description */
                    Account.DistanceUnits distUnits = Account.getDistanceUnits(currAcct);
                    String distUnitsStr = distUnits.toString(locale);

                    /* speed units description */
                    Account.SpeedUnits speedUnits = Account.getSpeedUnits(currAcct);
                    String speedUnitsStr = speedUnits.toString(locale);

                    /* volume units description */
                    Account.VolumeUnits volmUnits = Account.getVolumeUnits(currAcct);
                    String volmUnitsStr = volmUnits.toString(locale);

                    /* volume units description */
                    Account.EconomyUnits econUnits = Account.getEconomyUnits(currAcct);
                    String econUnitsStr = econUnits.toString(locale);

                    /* custom attributes */
                    Collection<String> customKeys = new OrderedSet<String>();
                    Collection<String> ppKeys = privLabel.getPropertyKeys(PrivateLabel.PROP_DeviceInfo_custom_);
                    for (String ppKey : ppKeys) {
                        customKeys.add(ppKey.substring(PrivateLabel.PROP_DeviceInfo_custom_.length()));
                    }
                    if (_selDev != null) {
                        customKeys.addAll(_selDev.getCustomAttributeKeys());
                    }

                    /* last connect times */
                    String lastConnectTime = (_selDev != null)? reqState.formatDateTime(_selDev.getLastTotalConnectTime()) : "";
                    if (StringTools.isBlank(lastConnectTime)) { lastConnectTime = i18n.getString("DeviceInfo.neverConnected","never"); }
                    String lastEventTime   = reqState.formatDateTime(reqState.getLastEventTime());
                    if (StringTools.isBlank(lastEventTime  )) { lastEventTime   = i18n.getString("DeviceInfo.noLastEvent"   ,"none" ); }

                    // frame header
                    String frameTitle = _allowEdit? 
                        i18n.getString("DeviceInfo.viewEditDevice","View/Edit {0} Information", devTitles) : 
                        i18n.getString("DeviceInfo.viewDevice","View {0} Information", devTitles);
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");

                    /* start of form */
                    out.write("<form name='"+FORM_DEVICE_EDIT+"' method='post' action='"+editURL+"' target='_self'>\n"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPD_DEVICE+"'/>\n");

                    /* Device fields */
                    ComboOption devActive = ComboOption.getYesNoOption(locale, ((_selDev != null) && _selDev.isActive()));
                    String firmVers  = (_selDev!=null)? _selDev.getCodeVersion() : "";
                    long   createTS  = (_selDev!=null)? _selDev.getCreationTime() : 0L;
                    String createStr = reqState.formatDateTime(createTS, "--");
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_DEVICE           , false      , i18n.getString("DeviceInfo.deviceID","{0} ID",devTitles)+":"        , _selDevID, 32, 32));
                    out.println(FormRow_TextField(PARM_CREATE_DATE      , false      , i18n.getString("DeviceInfo.creationDate","Creation Date")+":"       , createStr, 30, 30));
                    String deviceCode = (_selDev != null)? DCServerFactory.getServerConfigDescription(_selDev.getDeviceCode()) : "";
                    String autoFill = null;
                    if (StringTools.isBlank(deviceCode)) {
                        autoFill = i18n.getString("DeviceInfo.serverIdAutoFill","(automatically entered by the DCS)");
                    }
                    out.println(FormRow_TextField(PARM_SERVER_ID        , editServID , i18n.getString("DeviceInfo.serverID","Server ID")+":"               , deviceCode, 18, 20, autoFill));
                    //if (!StringTools.isBlank(firmVers)) {
                    out.println(FormRow_TextField(PARM_CODE_VERS        , false      , i18n.getString("DeviceInfo.firmwareVers","Firmware Version")+":"    , firmVers, 28, 28));
                    //}
                    if (_viewUniqID) {
                    out.println(FormRow_TextField(PARM_DEV_UNIQ         , _editUniqID, i18n.getString("DeviceInfo.uniqueID","Unique ID")+":"               , (_selDev!=null)?_selDev.getUniqueID():""       , 30, 30));
                    }
                    out.println(FormRow_ComboBox (PARM_DEV_ACTIVE       , _uiEdit    , i18n.getString("DeviceInfo.active","Active")+":"                    , devActive, ComboMap.getYesNoMap(locale), ""    , -1));
                    out.println(FormRow_TextField(PARM_DEV_DESC         , _uiEdit    , i18n.getString("DeviceInfo.deviceDesc","{0} Description",devTitles) +":", (_selDev!=null)?_selDev.getDescription():"", 40, 64));
                    out.println(FormRow_TextField(PARM_DEV_NAME         , _uiEdit    , i18n.getString("DeviceInfo.displayName","Short Name") +":"          , (_selDev!=null)?_selDev.getDisplayName():""    , 16, 64));
                    out.println(FormRow_TextField(PARM_VEHICLE_ID       , _uiEdit    , i18n.getString("DeviceInfo.vehicleID","Vehicle ID") +":"            , (_selDev!=null)?_selDev.getVehicleID():""      , 24, 24));
                    out.println(FormRow_TextField(PARM_DEV_EQUIP_TYPE   , _uiEdit    , i18n.getString("DeviceInfo.equipmentType","Equipment Type") +":"    , (_selDev!=null)?_selDev.getEquipmentType():""  , 30, 40));
                    out.println(FormRow_TextField(PARM_DEV_IMEI         , _uiEdit    , i18n.getString("DeviceInfo.imeiNumber","IMEI/ESN Number") +":"      , (_selDev!=null)?_selDev.getImeiNumber():""     , 16, 18));
                    out.println(FormRow_TextField(PARM_DEV_SERIAL_NO    , _uiEdit    , i18n.getString("DeviceInfo.serialNumber","Serial Number") +":"      , (_selDev!=null)?_selDev.getSerialNumber():""   , 20, 24));
                    if (dataKeyOK) {
                    out.println(FormRow_TextField(PARM_DATA_KEY         , _uiEdit    , i18n.getString("DeviceInfo.dataKey","Data Key") +":"                , (_selDev!=null)?_selDev.getDataKey():""        , 60, 200));
                    }
                    out.println(FormRow_TextField(PARM_DEV_SIMPHONE     , _uiEdit    , i18n.getString("DeviceInfo.simPhoneNumber","SIM Phone#") +":"       , (_selDev!=null)?_selDev.getSimPhoneNumber():"" , 14, 18));
                    out.println(FormRow_TextField(PARM_SMS_EMAIL        , _uiEdit    , i18n.getString("DeviceInfo.smsEmail","SMS Email Address") +":"      , (_selDev!=null)?_selDev.getSmsEmail():""       , 60, 60));
                    if (ppidOK) {
                        String ppDesc = i18n.getString("DeviceInfo.mapPushpinID","{0} Pushpin ID",grpTitles)+":";
                        String ppid = (_selDev != null)? _selDev.getPushpinID() : "";
                        if (showPushpinChooser) {
                            String ID_ICONSEL = "PushpinChooser";
                            String onclick    = _uiEdit? "javascript:ppcShowPushpinChooser('"+ID_ICONSEL+"')" : null;
                            out.println(FormRow_TextField(ID_ICONSEL, PARM_ICON_ID, _uiEdit, ppDesc, ppid, onclick, 25, 32, null));
                        } else {
                            ComboMap ppList = new ComboMap(reqState.getMapProviderPushpinIDs());
                            ppList.insert(""); // insert a blank as the first entry
                            out.println(FormRow_ComboBox(PARM_ICON_ID, _uiEdit, ppDesc, ppid, ppList, "", -1));
                        }
                    }
                    if (dcolorOK) {
                        String dcDesc = i18n.getString("DeviceInfo.mapRouteColor","Map Route Color")+":";
                        String dcolor = (_selDev != null)? _selDev.getDisplayColor() : "";
                        double P = 0.30;
                        ComboMap dcCombo = new ComboMap();
                        dcCombo.add(""                                        ,i18n.getString("DeviceInfo.color.default","Default"));
                        dcCombo.add(ColorTools.BLACK.toString(true)           ,i18n.getString("DeviceInfo.color.black"  ,"Black"  ));
                        dcCombo.add(ColorTools.BROWN.toString(true)           ,i18n.getString("DeviceInfo.color.brown"  ,"Brown"  ));
                        dcCombo.add(ColorTools.RED.toString(true)             ,i18n.getString("DeviceInfo.color.red"    ,"Red"    ));
                        dcCombo.add(ColorTools.ORANGE.darker(P).toString(true),i18n.getString("DeviceInfo.color.orange" ,"Orange" ));
                      //dcCombo.add(ColorTools.YELLOW.darker(P).toString(true),i18n.getString("DeviceInfo.color.yellow" ,"Yellow" ));
                        dcCombo.add(ColorTools.GREEN.darker(P).toString(true) ,i18n.getString("DeviceInfo.color.green"  ,"Green"  ));
                        dcCombo.add(ColorTools.BLUE.toString(true)            ,i18n.getString("DeviceInfo.color.blue"   ,"Blue"   ));
                        dcCombo.add(ColorTools.PURPLE.toString(true)          ,i18n.getString("DeviceInfo.color.purple" ,"Purple" ));
                        dcCombo.add(ColorTools.DARK_GRAY.toString(true)       ,i18n.getString("DeviceInfo.color.gray"   ,"Gray"   ));
                      //dcCombo.add(ColorTools.WHITE.toString(true)           ,i18n.getString("DeviceInfo.color.white"  ,"White"  ));
                        dcCombo.add(ColorTools.CYAN.darker(P).toString(true)  ,i18n.getString("DeviceInfo.color.cyan"   ,"Cyan"   ));
                        dcCombo.add(ColorTools.PINK.toString(true)            ,i18n.getString("DeviceInfo.color.pink"   ,"Pink"   ));
                        dcCombo.add("none"                                    ,i18n.getString("DeviceInfo.color.none"   ,"None"   ));
                        out.println(FormRow_ComboBox(PARM_DISPLAY_COLOR, _uiEdit, dcDesc, dcolor, dcCombo, "", -1));
                    }
                    if (ignOK) {
                        ComboMap ignList = new ComboMap(new String[] { "n/a", "ign" }); //,"0","1","2","3","4","5","6","7" });
                        int maxIgnNdx = privLabel.getIntProperty(PrivateLabel.PROP_DeviceInfo_maximumIgnitionIndex,7);
                        for (int igx = 0; igx <= maxIgnNdx; igx++) {
                            String igs = String.valueOf(igx);
                            ignList.add(igs, igs);
                        }
                        int ignNdx = (_selDev != null)? _selDev.getIgnitionIndex() : -1;
                        String ignSel = "";
                        if (ignNdx < 0) {
                            ignSel = "n/a";
                        } else
                        if (ignNdx == StatusCodes.IGNITION_INPUT_INDEX) {
                            ignSel = "ign";
                        } else {
                            ignSel = String.valueOf(ignNdx);
                        }
                        out.println(FormRow_ComboBox( PARM_IGNITION_INDEX, _uiEdit   , i18n.getString("DeviceInfo.ignitionIndex","Ignition Input") +":" , ignSel, ignList, "", -1, i18n.getString("DeviceInfo.ignitionIndexDesc","(ignition input line, if applicable)")));
                    }
                    // fuel capacity
                    double fuelCapUnits = (_selDev!=null)? volmUnits.convertFromLiters(_selDev.getFuelCapacity()) : 0.0;
                    String fuelCapStr   = StringTools.format(fuelCapUnits, "0.0");
                    out.println(FormRow_TextField(PARM_DEV_FUEL_CAP     , _uiEdit    , i18n.getString("DeviceInfo.fuelCapacity","Fuel Capacity") +":"      , fuelCapStr                                     , 10, 10, volmUnitsStr));
                    // fuel economy
                    if (showFuelEcon) {
                    double fuelEconUnits = (_selDev!=null)? econUnits.convertFromKPL(_selDev.getFuelEconomy()) : 0.0;
                    String fuelEconStr   = StringTools.format(fuelEconUnits, "0.0");
                    out.println(FormRow_TextField(PARM_DEV_FUEL_ECON    , _uiEdit    , i18n.getString("DeviceInfo.fuelEconomy","Fuel Economy") +":"        , fuelEconStr                                    , 10, 10, econUnitsStr));
                    }
                    // Speed Limit
                    if (speedLimOK) {
                        double speedLimUnits = (_selDev!=null)? Account.getSpeedUnits(currAcct).convertFromKPH(_selDev.getSpeedLimitKPH()) : 0.0;
                        String speedLimStr   = StringTools.format(speedLimUnits, "0.0");
                        out.println(FormRow_TextField(PARM_DEV_SPEED_LIMIT, _uiEdit  , i18n.getString("DeviceInfo.speedLimit","Maximum Speed") +":"        , speedLimStr                                    , 10, 10, speedUnitsStr));
                    }
                    // Driver ID
                    out.println(FormRow_TextField(PARM_DRIVER_ID        , _uiEdit    , i18n.getString("DeviceInfo.driverID","Driver ID")+":"               , (_selDev!=null)?_selDev.getDriverID():""       , 24, 24));
                    // Maintenance Section
                    if (Device.supportsPeriodicMaintenance()) {
                        // add separator before odometer if maintenance is supported
                        out.println(FormRow_Separator());
                    }
                    if (Device.supportsLastOdometer()) {
                        double odomKM   = (_selDev != null)? _selDev.getLastOdometerKM() : 0.0;
                        double offsetKM = (_selDev != null)? _selDev.getOdometerOffsetKM() : 0.0;
                        double rptOdom  = distUnits.convertFromKM(odomKM + offsetKM);
                        String odomStr  = StringTools.format(rptOdom, "0.0");
                        out.println(FormRow_TextField(PARM_REPORT_ODOM, _uiEdit, i18n.getString("DeviceInfo.reportOdometer","Reported Odometer") +":" , odomStr, 10, 11, distUnitsStr));
                    }
                    if (Device.supportsLastEngineHours()) {
                        double engHR   = (_selDev != null)? _selDev.getLastEngineHours() : 0.0;
                        String hourStr = StringTools.format(engHR, "0.00");
                        out.println(FormRow_TextField(PARM_REPORT_HOURS, false, i18n.getString("DeviceInfo.lastEngineHours","Last Engine Hours") +":" , hourStr, 10, 11, null));
                    }
                    if (Device.supportsPeriodicMaintenance()) {
                        double offsetKM = (_selDev != null)? _selDev.getOdometerOffsetKM() : 0.0;
                        double offsetHR = 0.0;
                        // Maintenance Notes
                        if (maintNotesOK) {
                            String noteText = (_selDev!=null)? StringTools.decodeNewline(_selDev.getMaintNotes()) : "";
                            out.println(FormRow_TextArea(PARM_MAINT_NOTES, _uiEdit, i18n.getString("DeviceInfo.maintNotes" ,"Maintenance Notes")+":", noteText, 3, 70));
                        }
                        // Odometer Maintenance / Interval
                        for (int ndx = 0; ndx < Device.getPeriodicMaintOdometerCount(); ndx++) {
                            String ndxStr = String.valueOf(ndx + 1);
                            out.println(FormRow_SubSeparator());
                            double lastMaintKM = (_selDev != null)? _selDev.getMaintOdometerKM(ndx) : 0.0;
                            double lastMaint   = distUnits.convertFromKM(lastMaintKM + offsetKM);
                            String lastMaintSt = StringTools.format(lastMaint, "0.0");
                            out.println(FormRow_TextField(PARM_MAINT_LASTKM_+ndx, false, i18n.getString("DeviceInfo.mainLast","Last Maintenance #{0}",ndxStr) +":" , lastMaintSt, 10, 11, distUnitsStr));
                            double intrvKM = (_selDev != null)? _selDev.getMaintIntervalKM(ndx) : 0.0;
                            double intrv   = distUnits.convertFromKM(intrvKM);
                            out.print("<tr>");
                            out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("DeviceInfo.maintInterval","Maintenance Interval #{0}",ndxStr)+":</td>");
                            out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                            out.print(Form_TextField(PARM_MAINT_INTERVKM_+ndx, _uiEdit, String.valueOf((long)intrv), 10, 11));
                            out.print("&nbsp;" + distUnitsStr);
                            if (_uiEdit) {
                                out.print(" &nbsp;&nbsp;(" + i18n.getString("DeviceInfo.maintReset","Check to Reset Service") + " ");
                                out.print(Form_CheckBox(PARM_MAINT_RESETKM_+ndx, PARM_MAINT_RESETKM_+ndx, _uiEdit, false, null, null));
                                out.print(")");
                            }
                            out.print("</td>");
                            out.print("</tr>\n");
                        }
                        // EngineHours Maintenance / Interval
                        for (int ndx = 0; ndx < Device.getPeriodicMaintEngHoursCount(); ndx++) {
                            String ndxStr = String.valueOf(ndx + 1);
                            out.println(FormRow_SubSeparator());
                            double lastMaintHR = (_selDev != null)? _selDev.getMaintEngHoursHR(ndx) : 0.0;
                            double lastMaint   = lastMaintHR + offsetHR;
                            String lastMaintSt = StringTools.format(lastMaint, "0.00");
                            out.println(FormRow_TextField(PARM_MAINT_LASTHR_+ndx, false, i18n.getString("DeviceInfo.maintEngHours","Last Eng Hours Maint",ndxStr) +":" , lastMaintSt, 10, 11, null));
                            double intrvHR = (_selDev != null)? _selDev.getMaintIntervalHR(ndx) : 0.0;
                            double intrv   = intrvHR;
                            out.print("<tr>");
                            out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("DeviceInfo.maintIntervalHR","Eng Hours Maint Interval",ndxStr)+":</td>");
                            out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                            out.print(Form_TextField(PARM_MAINT_INTERVHR_+ndx, _uiEdit, String.valueOf((long)intrv), 10, 11));
                            out.print("&nbsp;");
                            if (_uiEdit) {
                                out.print(" &nbsp;&nbsp;(" + i18n.getString("DeviceInfo.maintResetHR","Check to Reset Service") + " ");
                                out.print(Form_CheckBox(PARM_MAINT_RESETHR_+ndx, PARM_MAINT_RESETHR_+ndx, _uiEdit, false, null, null));
                                out.print(")");
                            }
                            out.print("</td>");
                            out.print("</tr>\n");
                        }
                    }
                    // Notification Section
                    if (ntfyOK) {
                        ComboOption allowNotfy = ComboOption.getYesNoOption(locale, ((_selDev!=null) && _selDev.getAllowNotify()));
                        out.println(FormRow_Separator());
                        out.println(FormRow_ComboBox (PARM_DEV_RULE_ALLOW, ntfyEdit  , i18n.getString("DeviceInfo.notifyAllow","Notify Enable")+":"     , allowNotfy, ComboMap.getYesNoMap(locale), "", -1));
                        out.println(FormRow_TextField(PARM_DEV_RULE_EMAIL, ntfyEdit  , i18n.getString("DeviceInfo.notifyEMail","Notify Email")+":"      , (_selDev!=null)?_selDev.getNotifyEmail():"", 95, 125));
                        out.print("<tr>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("DeviceInfo.lastAlertTime","Last Alert Time")+":</td>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                        long lastNotifyTime = (_selDev != null)? _selDev.getLastNotifyTime() : 0L;
                        if (lastNotifyTime <= 0L) {
                            String na = i18n.getString("DeviceInfo.noAlert","n/a");
                            out.print(Form_TextField(PARM_LAST_ALERT_TIME, false, na, 10, 10));
                        } else {
                            String lastAlertTime = reqState.formatDateTime(lastNotifyTime,"--");
                            out.print(Form_TextField(PARM_LAST_ALERT_TIME, false, lastAlertTime, 24, 24));
                            if (ntfyEdit) {
                                out.print(" &nbsp;&nbsp;(" + i18n.getString("DeviceInfo.lastAlertReset","Check to Reset Alert") + " ");
                                out.print(Form_CheckBox(PARM_LAST_ALERT_RESET, PARM_LAST_ALERT_RESET, ntfyEdit, false, null, null));
                                out.print(")");
                            }
                        }
                        out.print("</td>");
                        out.print("</tr>\n");
                        String ntfySel  = (_selDev!=null)?_selDev.getNotifySelector():"";
                        String ntfyDesc = (_selDev!=null)?_selDev.getNotifyDescription():"";
                        String ntfySubj = (_selDev!=null)?_selDev.getNotifySubject():"";
                        String ntfyText = (_selDev!=null)? StringTools.decodeNewline(_selDev.getNotifyText()) : "";
                        if (SHOW_NOTIFY_SELECTOR           ||
                            !Device.hasENRE()              || 
                            !StringTools.isBlank(ntfySel)  || 
                            !StringTools.isBlank(ntfySubj) || 
                            !StringTools.isBlank(ntfyText)   ) {
                            out.println(FormRow_SubSeparator());
                            String notifyRuleTitle = i18n.getString("DeviceInfo.notifyRule","Notify Rule");
                            out.println(FormRow_TextField(PARM_DEV_RULE_SEL  , ntfyEdit  , i18n.getString("DeviceInfo.ruleSelector","Rule Selector")+":"     , ntfySel  , 75, 90));
                          //out.println(FormRow_TextField(PARM_DEV_RULE_DESC , ntfyEdit  , i18n.getString("DeviceInfo.notifyDesc"  ,"Notify Description")+":", ntfyDesc , 75, 90));
                            out.println(FormRow_TextField(PARM_DEV_RULE_SUBJ , ntfyEdit  , i18n.getString("DeviceInfo.notifySubj"  ,"Notify Subject")+":"    , ntfySubj , 75, 90));
                            out.println(FormRow_TextArea( PARM_DEV_RULE_TEXT , ntfyEdit  , i18n.getString("DeviceInfo.notifyText"  ,"Notify Message")+":"    , ntfyText ,  5, 70));
                            //if (privLabel.hasEventNotificationEMail()) {
                            //    boolean editWrap = ntfyEdit; // && privLabel.hasEventNotificationEMail()
                            //    boolean useEMailWrapper = (_selDev!=null) && _selDev.getNotifyUseWrapper();
                            //    ComboOption wrapEmail = ComboOption.getYesNoOption(locale, useEMailWrapper);
                            //    out.println(FormRow_ComboBox(PARM_DEV_RULE_WRAP, editWrap, i18n.getString("DeviceInfo.notifyWrap" ,"Notify Use Wrapper")+":", wrapEmail, ComboMap.getYesNoMap(locale), "", -1, i18n.getString("DeviceInfo.seeEventNotificationEMail","(See 'EventNotificationEMail' tag in 'private.xml')")));
                            //} else {
                            //    Print.logInfo("PrivateLabel EventNotificationEMail Subject/Body not defined");
                            //}
                        }

                    }
                    if (Device.supportsActiveCorridor()) {
                        String actvGC   = (_selDev != null)? _selDev.getActiveCorridor() : "";
                        String gcTitle  = i18n.getString("DeviceInfo.activeCorridor","Active Corridor") + ":";
                        String gcList[] = Device.getCorridorIDsForAccount(reqState.getCurrentAccountID());
                        out.println(FormRow_Separator());
                        if (gcList != null) {
                            ComboMap gcCombo = new ComboMap(gcList);
                            gcCombo.insert("", i18n.getString("DeviceInfo.corridorNone","(none)"));
                            out.println(FormRow_ComboBox( PARM_ACTIVE_CORRIDOR, _uiEdit, gcTitle, actvGC, gcCombo, "", -1));
                        } else {
                            out.println(FormRow_TextField(PARM_ACTIVE_CORRIDOR, _uiEdit, gcTitle, actvGC, 20, 20));
                        }
                    }
                    /*
                    if (_showWorkOrderID(privLabel)) {
                        String actvWO = (_selDev != null)? _selDev.getWorkOrderID() : "";
                        String woTitle = i18n.getString("DeviceInfo.workOrderID","Work Order ID") + ":";
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_WORKORDER_ID     , _uiEdit, woTitle, actvWO, 20, 20));
                    }
                    */
                    if (Device.supportsLinkURL()) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_LINK_URL         , _uiEdit, i18n.getString("DeviceInfo.linkURL"        ,"Link URL")         +":" , (_selDev!=null)?String.valueOf(_selDev.getLinkURL()):""        , 60, 70));
                        out.println(FormRow_TextField(PARM_LINK_DESC        , _uiEdit, i18n.getString("DeviceInfo.linkDescription","Link Description") +":" , (_selDev!=null)?String.valueOf(_selDev.getLinkDescription()):"", 10, 24));
                    }
                    if (fixLocOK) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_FIXED_LAT        , _uiEdit, i18n.getString("DeviceInfo.fixedLatitude" ,"Fixed Latitude")  +":"  , (_selDev!=null)?String.valueOf(_selDev.getFixedLatitude()) :"0.0",  9, 10));
                        out.println(FormRow_TextField(PARM_FIXED_LON        , _uiEdit, i18n.getString("DeviceInfo.fixedLongitude","Fixed Longitude") +":"  , (_selDev!=null)?String.valueOf(_selDev.getFixedLongitude()):"0.0", 10, 11));
                    }
                    if (SHOW_LAST_CONNECT) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_DEV_LAST_CONNECT , false  , i18n.getString("DeviceInfo.lastConnect","Last Connect")+":" , lastConnectTime, 30, 30, i18n.getString("DeviceInfo.serverTime","(Server time)"))); // read-only
                        out.println(FormRow_TextField(PARM_DEV_LAST_EVENT   , false  , i18n.getString("DeviceInfo.lastEvent"  ,"Last Event"  )+":" , lastEventTime  , 30, 30, i18n.getString("DeviceInfo.deviceTime","(Device time)"))); // read-only
                    }
                    if (notesOK) {
                        String noteText = (_selDev!=null)? StringTools.decodeNewline(_selDev.getNotes()) : "";
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextArea(PARM_DEV_NOTES, _uiEdit, i18n.getString("DeviceInfo.notes" ,"General Notes")+":", noteText, 5, 70));
                    }
                    if (!ListTools.isEmpty(customKeys)) {
                        out.println(FormRow_Separator());
                        for (String key : customKeys) {
                            String desc  = privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_custom_ + key, key);
                            String value = (_selDev != null)? _selDev.getCustomAttribute(key) : "";
                            out.println(FormRow_TextField(PARM_DEV_CUSTOM_ + key, _uiEdit, desc + ":", value, 40, 50));
                        }
                    }
                    out.println(FormRow_Separator());
                    out.println("</table>");

                    /* DeviceGroup membership */
                    out.write("<span style='margin-left:4px; margin-top:10px; font-weight:bold;'>");
                    out.write(  i18n.getString("DeviceInfo.groupMembership","{0} Membership:",grpTitles));
                    out.write(  "</span>\n");
                    out.write("<div style='border: 1px solid black; margin: 2px 20px 5px 10px; height:80px; width:400px; overflow-x: hidden; overflow-y: scroll;'>\n");
                    out.write("<table>\n");
                    final OrderedSet<String> grpList = reqState.getDeviceGroupIDList(true);
                    for (int g = 0; g < grpList.size(); g++) {
                        String grp  = grpList.get(g);
                        String name = PARM_DEV_GROUP_ + grp;
                        String desc = reqState.getDeviceGroupDescription(grp,false/*!rtnDispName*/);
                        desc += desc.equals(grp)? ":" : (" ["+grp+"]:");
                        out.write("<tr><td>"+desc+"</td><td>");
                        if (grp.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                            out.write(Form_CheckBox(null,name,false,true,null,null));
                        } else {
                            boolean devInGroup = (_selDev != null)? 
                                DeviceGroup.isDeviceInDeviceGroup(_selDev.getAccountID(), grp, _selDevID) :
                                false;
                            out.write(Form_CheckBox(null,name,_uiEdit,devInGroup,null,null));
                        }
                        out.write("</td></tr>\n");
                    }
                    out.write("</table>\n");
                    out.write("</div>\n");

                    /* end of form */
                    out.write("<hr style='margin-bottom:5px;'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_uiEdit) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("DeviceInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("DeviceInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("DeviceInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    }
                    out.write("</form>\n");
                    
                }
            };

        } else
        if (_uiCmd) {

            final boolean _editProps = _allowCommand && (selDev != null);
            final DeviceCmdHandler _dcHandler = dcHandler; // non-null here
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {

                    /* frame title */
                    String frameTitle = i18n.getString("DeviceInfo.setDeviceProperties","({0}) Set {1} Properties", 
                        _dcHandler.getServerDescription(), devTitles[0]);
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");

                    /* Device Command/Properties content */
                    String editURL = DeviceInfo.this.encodePageURL(reqState);//, RequestProperties.TRACK_BASE_URI());
                    _dcHandler.writeCommandForm(out, reqState, _selDev, editURL, _editProps);

                }
            };
        } else
        if (_uiSms) {

            final boolean _editSms = _allowSms && (selDev != null);
            final DeviceCmdHandler _dcHandler = dcHandler; // non-null here
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {

                    /* frame title */
                    String frameTitle = i18n.getString("DeviceInfo.setDeviceSMS","({0}) Send {1} SMS", 
                        _dcHandler.getServerDescription(), devTitles[0]);
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");

                    /* Device Command/Properties content */
                    String editURL = DeviceInfo.this.encodePageURL(reqState);//, RequestProperties.TRACK_BASE_URI());
                    _dcHandler.writeCommandForm(out, reqState, _selDev, editURL, _editSms);

                }
            };

        }

        /* write frame */
        String onloadAlert = error? JS_alert(true,m) : null;
        String onload = (_uiCmd || _uiSms)? "javascript:devCommandOnLoad();" : onloadAlert;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }
    
    // ------------------------------------------------------------------------
}
