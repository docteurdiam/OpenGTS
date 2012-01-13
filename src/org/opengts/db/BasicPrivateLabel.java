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
//  2007/03/11  Martin D. Flynn
//     -Changed XML to place PageHeader/PageFooter/PageLeft/PageRight inside a
//      PageDecorations tag.
//     -Implemented two flavors of page decorations, one which is displayed when
//      no user is logged in, another which is displayed when a user is logged in.
//     -Added ReportFactory support
//  2007/03/30  Martin D. Flynn
//     -Added 'User' login support
//     -Added access control support
//  2007/05/06  Martin D. Flynn
//     -Added support for 'Page' tags 'menuText', 'menuHelp', and 'navText'
//  2007/05/20  Martin D. Flynn
//     -Added 'properties' attribute to 'MapProvider tag.
//     -Removed 'Geocoder' tag (use GeocodeProvider/ReverseGeocodeProvider instead)
//  2007/05/25  Martin D. Flynn
//     -Added 'restricted' attribute
//  2007/06/03  Martin D. Flynn
//     -Added 'locale' attribute (for I18N support)
//     -Removed 'menuText', 'menuHelp', 'navText' attributes (replaced by i18n)
//  2007/06/30  Martin D. Flynn
//     -Added host 'alias' support method 'addHostAlias(...)'
//     -Added support for overriding the default map dimensions.
//  2007/07/27  Martin D. Flynn
//     -'MapProvider' properties now supports ';' property separator.
//  2007/09/16  Martin D. Flynn
//     -Moved to package 'org.opengts.db', renamed to BasicPrivateLabel.jar
//     -Components specific to WAR property left in 'org.opengts.war.tools.PrivateLabel'
//     -XML loading moved to 'BasicPrivateLabelLoader.java'
//     -Added method 'setEventNotificationEMail'
//  2008/08/24  Martin D. Flynn
//     -Added 'setDefaultLoginUser' and 'getDefaultLoginUser' methods.
//  2009/02/20  Martin D. Flynn
//     -Added 'setDefaultLoginAccount' and 'getDefaultLoginAccount' methods.
//  2009/05/24  Martin D. Flynn
//     -Moved all property definitions from 'PrivateLabel.java' to here
//  2009/07/01  Martin D. Flynn
//     -"SendMail.getUserFromEmailAddress" is used to override 'From' email.
//  2009/09/23  Martin D. Flynn
//     -Added "getIntProperty".  Added property "topMenu.maximumIconsPerRow".
//  2010/04/11  Martin D. Flynn
//     -Added support for hiding the "Password" field on the login page
//  2011/01/28  Martin D. Flynn
//     -Added property "trackMap.showUpdateAll"
//  2011/07/01  Martin D. Flynn
//     -Added support for MobileLocationProvider
// ----------------------------------------------------------------------------
// The features this class provides are highly configurable through the external
// XML file 'private.xml'.  However, this code may also be modified to provide
// special custom features for the GPS tracking page.
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.Color;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
                        
import org.opengts.util.*;

import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;
import org.opengts.geocoder.*;
import org.opengts.cellid.*;

public class BasicPrivateLabel
    implements RTConfig.PropertySetter
{

    // ------------------------------------------------------------------------

    public  static final String EMAIL_TYPE_DEFAULT                          = "default";
    public  static final String EMAIL_TYPE_PASSWORD                         = "password";
    public  static final String EMAIL_TYPE_ACCOUNTS                         = "accounts";
    public  static final String EMAIL_TYPE_SUPPORT                          = "support";
    public  static final String EMAIL_TYPE_NOTIFY                           = "notify";
    public  static final String EMAIL_TYPE_COMMAND                          = "command";

    // ------------------------------------------------------------------------

    public  static final String TIMEZONE_CONF                               = "timezones.conf";

    // ------------------------------------------------------------------------

    public  static final String ALL_HOSTS                                   = "*";
    public  static final String DEFAULT_HOST                                = ALL_HOSTS;
    private static final String DEFAULT_TITLE                               = "Example GPS Tracking";
    private static final String DEFAULT_EMAIL_ADDRESS                       = null;

    // ------------------------------------------------------------------------
    // LAF properties
    
    public  static final String LAF_PageTitle                               = "PageTitle";
    public  static final String LAF_Copyright                               = "Copyright";
    
    /* "JSPEntry." is resolved in the 'private.xml' file tag "JSPEntry" */
    public  static final String LAF_JSPEntry_                               = "JSPEntry.";
    public  static final String LAF_JSPEntry_Default                        = LAF_JSPEntry_ + "default";
    
    public  static final String LAF_Banner_Width                            = "banner.width";
    public  static final String LAF_Banner_Style                            = "banner.style";
    public  static final String LAF_Banner_ImageSource                      = "banner.imageSource";
    public  static final String LAF_Banner_ImageWidth                       = "banner.imageWidth";
    public  static final String LAF_Banner_ImageHeight                      = "banner.imageHeight";
    public  static final String LAF_Banner_AnchorLink                       = "banner.anchorLink";
    
    public  static final String LAF_Background_Color                        = "Background.color";
    public  static final String LAF_Background_Image                        = "Background.image";
    public  static final String LAF_Background_Position                     = "Background.position";
    public  static final String LAF_Background_Repeat                       = "Background.repeat";
    
    public  static final String LAF_ContentCell_Color                       = "ContentCell.color";
    public  static final String LAF_IconMenu_GroupTitle_Color               = "IconMenu.groupTitle.color";

    // ------------------------------------------------------------------------
    // "Domain" level property definitions

    /* top-level Track properties */
    public  static final String PROP_Track_ValidateIDs                      = "track.validateIDs";
    public  static final String PROP_Track_editAfterNew                     = "track.editAfterNew";
    public  static final String PROP_Track_enableAuthenticationService      = "track.enableAuthenticationService";
    public  static final String PROP_Track_forwardToSecureAccess            = "track.forwardToSecureAccess";
    
    /* NewAccount properties */
    public  static final String PROP_NewAccount_authCodeMask                = "newAccount.authCodeMask";

    /* AccountLogin properties */
    public  static final String PROP_AccountLogin_showLoginLink             = "accountLogin.showLoginLink";
    public  static final String PROP_AccountLogin_showLocaleSelection       = "accountLogin.showLocaleSelection";
    public  static final String PROP_AccountLogin_legacyLAF                 = "accountLogin.legacyLAF";

    /* MenuBar properties */
    public  static final String PROP_MenuBar_openOnMouseOver                = "menuBar.openOnMouseOver";
    public  static final String PROP_MenuBar_usePullDownMenus               = "menuBar.usePullDownMenus";
    public  static final String PROP_MenuBar_includeTextAnchor              = "menuBar.includeTextAnchor";

    /* TopMenu properties */
    public  static final String PROP_TopMenu_menuType                       = "topMenu.menuType";
    public  static final String PROP_TopMenu_showHeader                     = "topMenu.showHeader";
    public  static final String PROP_TopMenu_maximumIconsPerRow             = "topMenu.maximumIconsPerRow";
    public  static final String PROP_TopMenu_showMenuDescription            = "topMenu.showMenuDescription";
    public  static final String PROP_TopMenu_showMenuHelp                   = "topMenu.showMenuHelp";

    /* DriverInfo properties */
    public  static final String PROP_DriverInfo_showDeviceID                = "driverInfo.showDeviceID";            // true|false

    /* DeviceInfo properties */
    public  static final String PROP_DeviceInfo_allowEditServerID           = "deviceInfo.allowEditServerID";       // true|false
    public  static final String PROP_DeviceInfo_showNotificationFields      = "deviceInfo.showNotificationFields";  // true|false
  //public  static final String PROP_DeviceInfo_showWorkOrderField          = "deviceInfo.showWorkOrderField";      // true|false
    public  static final String PROP_DeviceInfo_showPropertiesButton        = "deviceInfo.showPropertiesButton";    // true|false
    public  static final String PROP_DeviceInfo_showSmsButton               = "deviceInfo.showSmsButton";           // true|false
    public  static final String PROP_DeviceInfo_showNotes                   = "deviceInfo.showNotes";               // true|false
    public  static final String PROP_DeviceInfo_showFixedLocation           = "deviceInfo.showFixedLocation";       // true|false
    public  static final String PROP_DeviceInfo_showExpectedAcks            = "deviceInfo.showExpectedAcks";        // true|false
    public  static final String PROP_DeviceInfo_showMaintenanceNotes        = "deviceInfo.showMaintenanceNotes";    // true|false
    public  static final String PROP_DeviceInfo_showIgnitionIndex           = "deviceInfo.showIgnitionIndex";       // true|false
    public  static final String PROP_DeviceInfo_maximumIgnitionIndex        = "deviceInfo.maximumIgnitionIndex";    // 0..15
    public  static final String PROP_DeviceInfo_showPushpinID               = "deviceInfo.showPushpinID";           // true|false
    public  static final String PROP_DeviceInfo_showPushpinChooser          = "deviceInfo.showPushpinChooser";      // true|false
    public  static final String PROP_DeviceInfo_showDisplayColor            = "deviceInfo.showDisplayColor";        // true|false
    public  static final String PROP_DeviceInfo_showDataKey                 = "deviceInfo.showDataKey";             // true|false
    public  static final String PROP_DeviceInfo_allowNewDevice              = "deviceInfo.allowNewDevice";          // true|false|pass
    public  static final String PROP_DeviceInfo_SMS_useDeviceSMSCommands    = "deviceInfo.sms.useDeviceSMSCommands";// true|false|pass

    /* DeviceInfo custom fields */
    public  static final String PROP_DeviceInfo_custom_                     = "deviceInfo.custom.";                 // custom attr

    /* GroupInfo properties */
    public  static final String PROP_GroupInfo_showPropertiesButton         = "groupInfo.showPropertiesButton";     // true|false

    /* TrackMap properties */
    // calendar properties
    public  static final String PROP_TrackMap_calendarAction                = "trackMap.calendarAction";
    public  static final String PROP_TrackMap_calendarDateOnLoad            = "trackMap.calendarDateOnLoad";        // last|current
    public  static final String PROP_TrackMap_showTimezoneSelection         = "trackMap.showTimezoneSelection";     // true|false
    // map update properties
    public  static final String PROP_TrackMap_mapUpdateOnLoad               = "trackMap.mapUpdateOnLoad";           // all|last
    public  static final String PROP_TrackMap_autoUpdateRecenter            = "trackMap.autoUpdateRecenter";        // no|last|zoom
    public  static final String PROP_TrackMap_showUpdateAll                 = "trackMap.showUpdateAll";             // true|false
    public  static final String PROP_TrackMap_showUpdateLast                = "trackMap.showUpdateLast";            // true|false
    public  static final String PROP_TrackMap_showPushpinReplay             = "trackMap.showPushpinReplay";         // true|false
    // detail report properties
    public  static final String PROP_TrackMap_detailAscending               = "trackMap.detailAscending";           // true|false
    public  static final String PROP_TrackMap_detailCenterPushpin           = "trackMap.detailCenterPushpin";       // true|false
    public  static final String PROP_TrackMap_useRouteDisplayColor          = "trackMap.useRouteDisplayColor";      // true|false
    // overflow limit type
    public  static final String PROP_TrackMap_limitType                     = "trackMap.limitType";                 // first|last
    // misc properties
    public  static final String PROP_TrackMap_fleetDeviceEventCount         = "trackMap.fleetDeviceEventCount";     // 1
    public  static final String PROP_TrackMap_showFleetMapDevicePushpin     = "trackMap.showFleetMapDevicePushpin"; // default|true|false
    public  static final String PROP_TrackMap_showCursorLocation            = "trackMap.showCursorLocation";        // true|false
    public  static final String PROP_TrackMap_showDistanceRuler             = "trackMap.showDistanceRuler";         // true|false
    public  static final String PROP_TrackMap_showLocateNow                 = "trackMap.showLocateNow";             // true|false|device
    public  static final String PROP_TrackMap_showDeviceLink                = "trackMap.showDeviceLink";
    public  static final String PROP_TrackMap_showLegend                    = "trackMap.showLegend";                // true|false
    public  static final String PROP_TrackMap_pageLinks                     = "trackMap.pageLinks";                 // <pageIDs>
    public  static final String PROP_TrackMap_showGoogleKML                 = "trackMap.showGoogleKML";             // true|false
    public  static final String PROP_TrackMap_mapControlLocation            = "trackMap.mapControlLocation";        // left|right|true|false
    public  static final String PROP_TrackMap_mapControlCollapsible         = "trackMap.mapControlCollapsible";     // true|false
    public  static final String PROP_TrackMap_enableGeocode                 = "trackMap.enableGeocode";             // true|false
    public  static final String PROP_TrackMap_showBatteryLevel              = "trackMap.showBatteryLevel";          // true|false
    public  static final String PROP_TrackMap_showAllContainedGeozones      = "trackMap.showAllContainedGeozones";  // true|false
    public  static final String PROP_TrackMap_lastDevicePushpin_device      = "trackMap.lastDevicePushpin.device";  // true|false
    public  static final String PROP_TrackMap_lastDevicePushpin_fleet       = "trackMap.lastDevicePushpin.fleet";   // true|false

    /* FullMap properties */
    // calendar properties
    public  static final String PROP_FullMap_calendarAction                 = "fullMap.calendarAction";
    public  static final String PROP_FullMap_calendarDateOnLoad             = "fullMap.calendarDateOnLoad";         // last|current
    public  static final String PROP_FullMap_showTimezoneSelection          = "fullMap.showTimezoneSelection";      // true|false
    // map update properties
    public  static final String PROP_FullMap_mapUpdateOnLoad                = "fullMap.mapUpdateOnLoad";            // all|last
    public  static final String PROP_FullMap_autoUpdateRecenter             = "fullMap.autoUpdateRecenter";         // no|last|zoom
    public  static final String PROP_FullMap_showUpdateAll                  = "fullMap.showUpdateAll";              // true|false
    public  static final String PROP_FullMap_showUpdateLast                 = "fullMap.showUpdateLast";             // true|false
    public  static final String PROP_FullMap_showPushpinReplay              = "fullMap.showPushpinReplay";          // true|false
    // detail report properties
    public  static final String PROP_FullMap_detailAscending                = "fullMap.detailAscending";            // true|false
    public  static final String PROP_FullMap_detailCenterPushpin            = "fullMap.detailCenterPushpin";        // true|false
    public  static final String PROP_FullMap_useRouteDisplayColor           = "fullMap.useRouteDisplayColor";       // true|false
    // overflow limit type
    public  static final String PROP_FullMap_limitType                      = "fullMap.limitType";                  // first|last
    // misc properties
    public  static final String PROP_FullMap_fleetDeviceEventCount          = "fullMap.fleetDeviceEventCount";      // 1
    public  static final String PROP_FullMap_showFleetMapDevicePushpin      = "fullMap.showFleetMapDevicePushpin";  // default|true|false
    public  static final String PROP_FullMap_showCursorLocation             = "fullMap.showCursorLocation";         // true|false
    public  static final String PROP_FullMap_showDistanceRuler              = "fullMap.showDistanceRuler";          // true|false
    public  static final String PROP_FullMap_showLocateNow                  = "fullMap.showLocateNow";              // true|false|device
    public  static final String PROP_FullMap_showDeviceLink                 = "fullMap.showDeviceLink";
    public  static final String PROP_FullMap_showLegend                     = "fullMap.showLegend";                 // true|false
    public  static final String PROP_FullMap_pageLinks                      = "fullMap.pageLinks";                  // <pageIDs>
    public  static final String PROP_FullMap_showGoogleKML                  = "fullMap.showGoogleKML";              // true|false
    public  static final String PROP_FullMap_mapControlLocation             = "fullMap.mapControlLocation";         // left|right|true|false
    public  static final String PROP_FullMap_mapControlCollapsible          = "fullMap.mapControlCollapsible";      // true|false
    public  static final String PROP_FullMap_enableGeocode                  = "fullMap.enableGeocode";              // true|false
    public  static final String PROP_FullMap_showBatteryLevel               = "fullMap.showBatteryLevel";           // true|false
    public  static final String PROP_FullMap_showAllContainedGeozones       = "fullMap.showAllContainedGeozones";   // true|false
    public  static final String PROP_FullMap_lastDevicePushpin_device       = "fullMap.lastDevicePushpin.device";   // true|false
    public  static final String PROP_FullMap_lastDevicePushpin_fleet        = "fullMap.lastDevicePushpin.fleet";    // true|false

    /* ReportMenu properties */
    public  static final String PROP_ReportMenu_useMapDates                 = "reportMenu.useMapDates";             // true|false
    public  static final String PROP_ReportMenu_showTimezoneSelection       = "reportMenu.showTimezoneSelection";   // true|false
    public  static final String PROP_ReportMenu_enableReportEmail           = "reportMenu.enableReportEmail";       // true|false
    public  static final String PROP_ReportMenu_enableReportSchedule        = "reportMenu.enableReportSchedule";    // true|false
    public  static final String PROP_ReportMenu_customFormatURL             = "reportMenu.customFormatURL";         // <URL>

    /* ReportDisplay properties */
    public  static final String PROP_ReportDisplay_showGoogleKML            = "reportDisplay.showGoogleKML";        // true|false

    /* UserInfo properties */
    public  static final String PROP_UserInfo_showPreferredDeviceID         = "userInfo.showPreferredDeviceID";     // true|false
    public  static final String PROP_UserInfo_showPassword                  = "userInfo.showPassword";              // true|false
    public  static final String PROP_UserInfo_authorizedGroupCount          = "userInfo.authorizedGroupCount";      // <int>

    /* ZoneInfo properties */
    public  static final String PROP_ZoneInfo_mapControlLocation            = "zoneInfo.mapControlLocation";        // left|right|true|false
    public  static final String PROP_ZoneInfo_enableGeocode                 = "zoneInfo.enableGeocode";             // true|false
    public  static final String PROP_ZoneInfo_showOverlapPriority           = "zoneInfo.showOverlapPriority";       // true|false
    public  static final String PROP_ZoneInfo_showArriveDepartZone          = "zoneInfo.showArriveDepartZone";      // true|false
    public  static final String PROP_ZoneInfo_showAutoNotify                = "zoneInfo.showAutoNotify";            // true|false
    public  static final String PROP_ZoneInfo_showClientUploadZone          = "zoneInfo.showClientUploadZone";      // true|false
    public  static final String PROP_ZoneInfo_showShapeColor                = "zoneInfo.showShapeColor";            // true|false
    public  static final String PROP_ZoneInfo_showSpeedLimit                = "zoneInfo.showSpeedLimit";            // true|false
    public  static final String PROP_ZoneInfo_maximumDisplayedVertices      = "zoneInfo.maximumDisplayedVertices";  // <int> 1..8

    /* CorridorInfo properties */
    public  static final String PROP_CorridorInfo_mapControlLocation        = "corridorInfo.mapControlLocation";    // left|right|true|false
    public  static final String PROP_CorridorInfo_showShapeColor            = "corridorInfo.showShapeColor";        // true|false
    public  static final String PROP_CorridorInfo_enableGeocode             = "corridorInfo.enableGeocode";         // true|false
    public  static final String PROP_CorridorInfo_pointCount                = "corridorInfo.pointCount";            // <int>

    /* RuleInfo properties */
    public  static final String PROP_RuleInfo_showEMailWrapper              = "ruleInfo.showEMailWrapper";          // true|false
    public  static final String PROP_RuleInfo_showSysRulesOnly              = "ruleInfo.showSysRulesOnly";          // true|false
    public  static final String PROP_RuleInfo_showCronRules                 = "ruleInfo.showCronRules";             // true|false
    public  static final String PROP_RuleInfo_ruleTagList                   = "ruleInfo.ruleTagList";               // tag,tag,...
    public  static final String PROP_RuleInfo_showPredefinedActions         = "ruleInfo.showPredefinedActions";

    /* DeviceChooser misc properties */
    public  static final String PROP_DeviceChooser_sortBy                   = "deviceChooser.sortBy";               // id|name|description
    public  static final String PROP_DeviceChooser_useTable                 = "deviceChooser.useTable";             // true|false
    public  static final String PROP_DeviceChooser_idPosition               = "deviceChooser.idPosition";           // none|first|last (table only)
    public  static final String PROP_DeviceChooser_search                   = "deviceChooser.search";               // true|false (table only)
    public  static final String PROP_DeviceChooser_matchContains            = "deviceChooser.matchContains";        // true|false 
    public  static final String PROP_DeviceChooser_singleItemTextField      = "deviceChooser.singleItemTextField";  // true|false (hint)
    public  static final String PROP_DeviceChooser_includeListHtml          = "deviceChooser.includeListHtml";      // include iniitial HTML
    // ---
    public  static final String PROP_DeviceChooser_extraDebugEntries        = "deviceChooser.extraDebugEntries";    // int

    /* WorkZoneInfo properties */
    public  static final String PROP_WorkZoneInfo_mapControlLocation        = "WorkZoneInfo.mapControlLocation";    // left|right|true|false

    /* StatusCodeInfo misc properties */
    public  static final String PROP_StatusCodeInfo_showIconSelector        = "statusCodeInfo.showIconSelector";    // true|false "Rule" selector
    public  static final String PROP_StatusCodeInfo_showPushpinChooser      = "statusCodeInfo.showPushpinChooser";  // true|false

    /* DeviceAlerts misc properties */
    public  static final String PROP_DeviceAlerts_refreshInterval           = "deviceAlerts.refreshInterval";       // #seconds
    public  static final String PROP_DeviceAlerts_mapPageName               = "deviceAlerts.mapPageName";           // map page name
    public  static final String PROP_DeviceAlerts_showAllDevices            = "deviceAlerts.showAllDevices";        // true|false
    public  static final String PROP_DeviceAlerts_maxActiveAlertAge         = "deviceAlerts.maxActiveAlertAge";     // #seconds

    /* EntityAdmin misc properties */
    public  static final String PROP_EntityAdmin_entityType                 = "entityAdmin.entityType";             // 

    /* SysAdminAccounts misc properties */
    public  static final String PROP_SysAdminAccounts_showPasswords         = "sysAdminAccounts.showPasswords";     // true|false
    public  static final String PROP_SysAdminAccounts_allowAccountLogin     = "sysAdminAccounts.allowAccountLogin"; // true|false
    public  static final String PROP_SysAdminAccounts_accountProperties     = "sysAdminAccounts.accountProperties"; // true|false
    public  static final String PROP_SysAdminAccounts_showAccountManager    = "sysAdminAccounts.showAccountManager"; // true|false
    public  static final String PROP_SysAdminAccounts_reloginPasscode       = "sysAdminAccounts.reloginPasscode";   // String
    public  static final String PROP_SysAdminAccounts_showNotes             = "sysAdminAccounts.showNotes";         // true|false

    /* Calendar properties */
    public  static final String PROP_Calendar_firstDayOfWeek                = "calendar.firstDayOfWeek";            // 0..6 (Sun..Sat)
    public  static final String PROP_Calendar_timeTextField                 = "calendar.timeTextField";             // true|false
  //public  static final String PROP_Calendar_timeTextField_hourInc         = "calendar.timeTextField.hourInc";     // int
  //public  static final String PROP_Calendar_timeTextField_minuteInc       = "calendar.timeTextField.minuteInc";   // int

    /* "loginSession_banner.jsp" properties */
    public  static final String PROP_Banner_width                           = "banner.width";
    public  static final String PROP_Banner_style                           = "banner.style";
    public  static final String PROP_Banner_imageSource                     = "banner.imageSource";
    public  static final String PROP_Banner_imageWidth                      = "banner.imageWidth";
    public  static final String PROP_Banner_imageHeight                     = "banner.imageHeight";
    public  static final String PROP_Banner_imageLink                       = "banner.imageLink";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    protected static boolean _isTrackServlet = false;

    public static void setTrackServlet_loaderOnly()
    {
        BasicPrivateLabel._isTrackServlet = true;
    }

    public static boolean isTrackServlet()
    {
        return BasicPrivateLabel._isTrackServlet;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // host name key
    private String                      primaryHostName         = DEFAULT_HOST;
    private java.util.List<String>      hostAliasList           = null;
  //private Map<String,String>          hostAliasMap            = null;
    private String                      domainName              = null;

    // base URL
    private String                      defaultBaseURL          = null;

    // account/user login (display account/user name on login screen)
    private boolean                     acctLogin               = true;  // if true, Account displayed
    private boolean                     userLogin               = false; // if true, User    displayed
    private boolean                     allowEmailLogin         = false; // if true, contact-email login allowed
    private String                      defaultLoginAccount     = null;
    private String                      defaultLoginUser        = null;

    // restricted (requires account to explicitly specify this private label name)
    private boolean                     isRestricted            = false;
    
    // locale (IE. "en_US")
    private String                      localeStr               = null;
    private String                      dateFormat              = null;
    private String                      timeFormat              = null;

    // show password field on login page
    private boolean                     showPassword            = true;

    // enable demo (display 'Demo' button on login screen)
    private boolean                     enableDemo              = true;

    // Properties
    private OrderedMap<String,Object>   rtPropMap               = new OrderedMap<String,Object>();
    private RTProperties                rtProps                 = new RTProperties(this.rtPropMap);

    // TimeZones
    private OrderedSet<String>          timeZonesList           = null;
    private String                      timeZonesArray[]        = null;

    // (Reverse)GeocodeProviders
    private OrderedMap<String,ReverseGeocodeProvider>   revgeoProvider  = null;
    private OrderedMap<String,GeocodeProvider>          geocodeProvider = null;
    
    // MobileLocationProvider
    private OrderedMap<String,MobileLocationProvider>   mobLocProvider  = null;

    // map of PrivateLabel ACLs
    private AccessLevel                 dftAccLevel             = null;
    private OrderedMap<String,AclEntry> privateAclMap           = null;
    private AclEntry                    allAclEntries[]         = null;

    // Event Notification EMail
    private String                      eventNotifyFrom         = null;
    private I18N.Text                   eventNotifySubj         = null;
    private I18N.Text                   eventNotifyBody         = null;
    private boolean                     eventNotifyDefault      = false;

    // StatusCode description overrides
    private boolean                     statusCodeOnly          = false;
    private Map<Integer,StatusCodes.Code> statusCodes           = null;

    /**
    *** Constructor 
    **/
    protected BasicPrivateLabel()
    {
        super();
    }

    /**
    *** Constructor 
    *** @param host  The primary host name associated with this BasicPrivateLabel
    **/
    public BasicPrivateLabel(String host)
    {
        this();
        this.setHostName(host);
    }

    //public BasicPrivateLabel(String host, String title)
    //{
    //    this();
    //    this.setHostName(host);
    //    this.setPageTitle(title);
    //}

    // ------------------------------------------------------------------------

    /**
    *** Sets the default BaseURL (ie. "http://localhost:8080/track/Track")
    *** @param baseURL  The base URL
    **/
    public void setDefaultBaseURL(String baseURL)
    {
        this.defaultBaseURL = !StringTools.isBlank(baseURL)? baseURL : null;
    }
    
    /**
    *** Returns true if a base URL has been defined
    *** @return True if a base URL has been defined
    **/
    public boolean hasDefaultBaseURL()
    {
        return !StringTools.isBlank(this.defaultBaseURL);
    }

    /**
    *** Gets the default BaseURL (or null if no base URL is defined)
    *** @return The default BaseURL
    **/
    public String getDefaultBaseURL()
    {
        return this.defaultBaseURL;
    }

    // ------------------------------------------------------------------------

    /**
    *** Return String representation of this instance
    *** @return String representation of this instance
    **/
    public String toString()
    {
        return this.getHostName();
    }

    /**
    *** Sets the primary host name associated with this BasicPrivateLabel
    *** @param host  The primary host name to associate with this BasicPrivateLabel
    **/
    public void setHostName(String host)
    {
        if (StringTools.isBlank(host)) {
            this.primaryHostName = null;
        } else {
            this.primaryHostName = host.trim();
        }
    }
    
    /**
    *** Gets the primary host name associated with this BasicPrivateLabel
    *** @return The primary host name associated with this BasicPrivateLabel
    **/
    public String getHostName()
    {
        return (this.primaryHostName != null)? this.primaryHostName : DEFAULT_HOST;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds a host alias to this BasicPrivateLabel
    *** @param host The host alias to add
    *** @param desc The host alias description (currently not used)
    **/
    public void addHostAlias(String host, String desc)
    {
        if (!StringTools.isBlank(host)) {
            if (this.hostAliasList == null) {
                this.hostAliasList = new Vector<String>();
                //this.hostAliasMap  = new HashMap<String,String>();
            }
            String h = host.trim();
            this.hostAliasList.add(h);
            //this.hostAliasMap.put(h, desc);
        }
    }
    
    /**
    *** Gets the list of host name aliases
    *** @return The list of host nmae aliases
    **/
    public java.util.List<String> getHostAliasNames()
    {
        return this.hostAliasList;
    }
    
    /**
    *** Gets the default description for the specified alias name
    *** @param name  The host alias for which the description is returned
    *** @return The description of the specified alias
    **/
    /*
    public String getHostAliasDescription(String name)
    {
        if (StringTools.isBlank(name)) {
            return "";
        } else {
            String desc = (this.hostAliasMap != null)? this.hostAliasMap.get(name) : null;
            if (desc != null) {
                return desc;
            } else
            if (name.equals(StringTools.trim(this.getHostName()) {
                return "";
            } else
            if (name.equals(StringTools.trim(this.getDomainName())) {
                return "";
            } else {
                return "";
            }
        }
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Sets the host alias name
    *** @param name  The host alias name
    **/
    public void setDomainName(String name)
    {
        if (StringTools.isBlank(name)) {
            this.domainName = null;
        } else {
            this.domainName = name.trim();
            this.setProperty(RTKey.SESSION_NAME, this.domainName);
            //this.printProperties();
        }
    }
    
    /**
    *** Gets the host alias name
    *** @return The host alias name
    **/
    public String getDomainName()
    {
        return this.domainName; // should not be null, but not guaranteed
    }

    /**
    *** Gets the name of this BasicPrivateLabel.  
    *** @return If specified, the alias name will be returned, otherwise the host name will be reutrned
    **/
    public String getName()
    {
        String name = this.getDomainName();
        if (!StringTools.isBlank(name)) {
            return name;
        } else {
            return this.getHostName();
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the account login display state
    *** @param acctLogin True to display account login, false otherwise
    **/
    public void setAccountLogin(boolean acctLogin)
    {
        this.acctLogin = acctLogin;
    }
    
    /**
    *** Gets the account login display state 
    *** @return True to display the account login, false to hide account login (if implemented)
    **/
    public boolean getAccountLogin()
    {
        return this.acctLogin;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the user login display state
    *** @param userLogin True to display user login, false otherwise
    **/
    public void setUserLogin(boolean userLogin)
    {
        this.userLogin = userLogin;
    }
    
    /**
    *** Gets the user login display state 
    *** @return True to display the user login, false to hide user login
    **/
    public boolean getUserLogin()
    {
        return this.userLogin;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Enabled/Disabled contact-email login state
    *** @param emailLogin True to enable contact-email login
    **/
    public void setAllowEmailLogin(boolean emailLogin)
    {
        this.allowEmailLogin = emailLogin;
    }
    
    /**
    *** Gets the Enabled/Disabled contact-email login state
    *** @return True if contact-email login is enabled
    **/
    public boolean getAllowEmailLogin()
    {
        return this.allowEmailLogin;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default login account name
    *** @param defaultAccount The default login account
    **/
    public void setDefaultLoginAccount(String defaultAccount)
    {
        this.defaultLoginAccount = defaultAccount;
    }
    
    /**
    *** Gets the default login account name
    *** @return The default login account name
    **/
    public String getDefaultLoginAccount()
    {
        if (!StringTools.isBlank(this.defaultLoginAccount)) {
            return this.defaultLoginAccount;
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default login user name
    *** @param defaultUser The default login user
    **/
    public void setDefaultLoginUser(String defaultUser)
    {
        this.defaultLoginUser = defaultUser;
    }
    
    /**
    *** Gets the default login user name
    *** @return The default login user name
    **/
    public String getDefaultLoginUser()
    {
        if (!StringTools.isBlank(this.defaultLoginUser)) {
            return this.defaultLoginUser;
        } else {
            return User.getAdminUserID();
        }
    }

    // ------------------------------------------------------------------------

    private static OrderedMap<String,I18N.Text> SupportedLocales = null;

    /**
    *** Returns true if a set of supported Locales is defined
    *** @param localeMap  The supported Locale map
    **/
    public static boolean HasSupportedLocalesMap()
    {
        return !ListTools.isEmpty(BasicPrivateLabel.SupportedLocales);
    }

    /**
    *** Sets the supported Locales
    *** @param localeMap  The supported Locale map
    **/
    public static void SetSupportedLocalesMap(OrderedMap<String,I18N.Text> localeMap)
    {
        if (!ListTools.isEmpty(localeMap)) {
            if (ListTools.isEmpty(BasicPrivateLabel.SupportedLocales)) {
                BasicPrivateLabel.SupportedLocales = localeMap;
            } else {
                //BasicPrivateLabel.SupportedLocales.putAll(localeMap);
            }
        }
    }

    /**
    *** Gets a map of supported locales 
    *** @param locale  The preferred Locale
    *** @return The Supported Locale map
    **/
    public static Map<String,String> GetSupportedLocaleMap(Locale locale)
    {
        OrderedMap<String,String> map = new OrderedMap<String,String>();
        if (!ListTools.isEmpty(BasicPrivateLabel.SupportedLocales)) {
            for (String locID : BasicPrivateLabel.SupportedLocales.keySet()) {
                String locDesc = BasicPrivateLabel.SupportedLocales.get(locID).toString(locale);
                map.put(locID, locDesc);
                //Print.logInfo("Locale: " + locID + " - " + locDesc);
            }
        } else {
            Print.logWarn("No 'SupportedLocales' defined in 'private.xml'");
        }
        return map;
    }

    /**
    *** Sets the locale String code for this BasicPrivateLabel
    *** @param localeStr  The locale String associated with this BasicPrivateLabel
    **/
    public void setLocaleString(String localeStr)
    {
        this.localeStr = localeStr;
        this.setProperty(RTKey.SESSION_LOCALE, this.localeStr);
        this.setProperty(RTKey.LOCALE        , this.localeStr);
    }
    
    /**
    *** Gets the locale String code for this BasicPrivateLabel
    *** @return The locale String code for this BasicPrivateLabel
    **/
    public String getLocaleString()
    {
        String ls = RTConfig.getString(RTKey.SESSION_LOCALE, this.localeStr);
        //Print.logInfo("Returning Locale: " + ls);
        return (ls != null)? ls : "";
        //return (this.localeStr != null)? this.localeStr : "";
    }
    
    /**
    *** Gets the Locale for the current locale String code
    *** @return The Locale associated with this BasicPrivateLabel
    **/
    public Locale getLocale()
    {
        return I18N.getLocale(this.getLocaleString());
    }
    
    /**
    *** Gets the I18N instance for the specified class using the Locale associated with this BasicPrivateLabel
    *** @param clazz  The class for which the I18N instance will be returned
    *** @return The I18N instance for the specified class
    **/
    public I18N getI18N(Class clazz)
    {
        return I18N.getI18N(clazz, this.getLocale());
    }
    
    /**
    *** Gets the I18N instance for the specified package using the Locale associated with this BasicPrivateLabel
    *** @param pkg  The package for which the I18N instance will be returned
    *** @return The I18N instance for the specified package
    **/
    public I18N getI18N(Package pkg)
    {
        return I18N.getI18N(pkg, this.getLocale());
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the date format for the current BasicPrivateLabel
    *** @param dateFmt  The date format
    **/
    public void setDateFormat(String dateFmt)
    {
        this.dateFormat = !StringTools.isBlank(dateFmt)? dateFmt : null;
    }

    /**
    *** Gets the date format for this BasicPrivateLabel
    *** @return The date format
    **/
    public String getDateFormat()
    {
        if (this.dateFormat == null) {
            this.dateFormat = BasicPrivateLabel.getDefaultDateFormat();
        }
        return this.dateFormat;
    }
    
    /**
    *** Gets the default date format
    *** @return The default date format
    **/
    public static String getDefaultDateFormat()
    {
        // ie. "yyyy/MM/dd"
        String fmt = RTConfig.getString(RTKey.LOCALE_DATEFORMAT,RTKey.DEFAULT_DATEFORMAT);
        return ((fmt != null) && !fmt.equals(""))? fmt : DateTime.DEFAULT_DATE_FORMAT;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the time format for this BasicPrivateLabel
    *** @param timeFmt  The time format
    **/
    public void setTimeFormat(String timeFmt)
    {
        this.timeFormat = !StringTools.isBlank(timeFmt)? timeFmt : null;
    }

    /**
    *** Gets the time format for this BasicPrivateLabel
    *** @return The time format
    **/
    public String getTimeFormat()
    {
        if (this.timeFormat == null) {
            this.timeFormat = BasicPrivateLabel.getDefaultTimeFormat();
        }
        return this.timeFormat;
    }
    
    /**
    *** Gets the default time format
    *** @return The default time format
    **/
    public static String getDefaultTimeFormat()
    {
        // ie. "HH:mm:ss"
        String fmt = RTConfig.getString(RTKey.LOCALE_TIMEFORMAT,RTKey.DEFAULT_TIMEFORMAT);
        return ((fmt != null) && !fmt.equals(""))? fmt : DateTime.DEFAULT_TIME_FORMAT;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the 'restricted mode' for this BasicPrivateLabel.  This means that only Account
    *** that reference this particular BasicPrivalLable name have access to the resources
    *** defined by this BasicPrivateLabel.
    *** @param restricted  True to enforce restricted access, false otherwise
    **/
    public void setRestricted(boolean restricted)
    {
        this.isRestricted = restricted;
    }
    
    /**
    *** Returns true this is BasicPrivateLabel has restricted access
    *** @return True if this BasicPrivateLabel has restricted access
    **/
    public boolean isRestricted()
    {
        return this.isRestricted;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets whether to show the password, or not
    *** @param showPass  True to show password, false to hide the password field.
    **/
    public void setShowPassword(boolean showPass)
    {
        this.showPassword = showPass;
    }
    
    /**
    *** Returns true if the password field is to be made visible
    *** @return True if the password field is to be made visible, false otherwise
    **/
    public boolean getShowPassword()
    {
        return this.showPassword;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the 'demo' mode for this BasicPrivateLabel
    *** @param isDemo  True to enable 'demo' support, false to disable.
    **/
    public void setEnableDemo(boolean isDemo)
    {
        this.enableDemo = isDemo;
    }
    
    /**
    *** Returns true if this BasicPrivateLabel supports a 'demo' mode
    *** @return True if 'demo' mode is supported, false otherwise
    **/
    public boolean getEnableDemo()
    {
        return this.enableDemo;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Clears TimeZone cache
    **/
    public void clearTimeZones()
    {
        this.timeZonesList  = null;
        this.timeZonesArray = null;
    }

    /**
    *** Sets the TimeZones supported by this BasicPrivateLabel
    *** @param tmz  The list of supported TimeZones
    **/
    public void setTimeZones(OrderedSet<String> tmz)
    {
        this.clearTimeZones();
        this.timeZonesList = new OrderedSet<String>(tmz); // clone
    }

    /**
    *** Sets the TimeZones supported by this BasicPrivateLabel
    *** @param tmz  The list of supported TimeZones
    **/
    public void setTimeZones(java.util.List<String> tmz)
    {
        this.clearTimeZones();
        this.timeZonesList = new OrderedSet<String>(tmz); // clone
    }

    /**
    *** Sets the TimeZones supported by this BasicPrivateLabel
    *** @param tmz  The array of supported TimeZones
    **/
    public void setTimeZones(String tmz[])
    {
        this.clearTimeZones();
        this.timeZonesList = new OrderedSet<String>();
        if (tmz != null) {
            for (int i = 0; i < tmz.length; i++) {
                String tz = tmz[i].trim();
                if (!tz.equals("")) {
                    this.timeZonesList.add(tz);
                }
            }
        }
    }

    /**
    *** Gets the list of supported TimeZones
    *** @return The list of supported TimeZones
    **/
    public java.util.List<String> getTimeZonesList()
    {
        if (this.timeZonesList == null) { 
            // 'this.timeZonesList' is initialized at startup
            this.clearTimeZones();
            this.timeZonesList = new OrderedSet<String>();
        }
        return this.timeZonesList;
    }
   /**
    *** Gets the list of supported TimeZones
    *** @return The list of supported TimeZones
    **/
    public String[] getTimeZonesArray()
    {
        java.util.List<String> tmzl = this.getTimeZonesList();
        return tmzl.toArray(new String[tmzl.size()]);
    }

    /**
    *** Gets an array of supported TimeZones
    *** @return The array of supported TimeZones
    **/
    public String[] getTimeZones()
    {
        if (this.timeZonesArray == null) {
            java.util.List<String> tzList = this.getTimeZonesList();
            synchronized (tzList) {
                if (this.timeZonesArray == null) {
                    // reconstruct TimeZone Array from TimeZone List
                    this.timeZonesArray = tzList.toArray(new String[tzList.size()]);
                }
            }
        }
        return this.timeZonesArray;
    }

    /* array of all time-zones */
    private static boolean didInitAllTimeZones = false;
    private static String  allTimeZones[] = null;
    
    /**
    *** Returns an array of all possible TimeZone names
    *** @return An array of all possible TimeZone names
    **/
    public static String[] getAllTimeZones()
    {
        if (!BasicPrivateLabel.didInitAllTimeZones) {
            BasicPrivateLabel.didInitAllTimeZones = true;
            File cfgFile = RTConfig.getLoadedConfigFile();
            if (cfgFile != null) {
                File tmzFile = new File(cfgFile.getParentFile(), TIMEZONE_CONF);
                BasicPrivateLabel.allTimeZones = DateTime.readTimeZones(tmzFile); // may still be null
            }
        }
        return BasicPrivateLabel.allTimeZones;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the title used on HTML pages displayed for this BasicPrivateLabel
    *** @param text  The title of the HTML page
    **/
    public void setPageTitle(I18N.Text text)
    {
        this.setI18NTextProperty(BasicPrivateLabelLoader.TAG_PageTitle, text);
    }

    /**
    *** Gets the HTML page title for this BasicPrivateLabel
    *** @return The HTML page title
    **/
    public String getPageTitle()
    {
        return this.getI18NTextString(BasicPrivateLabelLoader.TAG_PageTitle, DEFAULT_TITLE);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the ReverseGeocodeProvider for this BasicPrivateLabel
    *** @param rgp  The ReverseGeocodeProvider
    **/
    public void addReverseGeocodeProvider(ReverseGeocodeProvider rgp)
    {
        if (rgp != null) {
            if (this.revgeoProvider == null) {
                this.revgeoProvider = new OrderedMap<String,ReverseGeocodeProvider>();
            }
            this.revgeoProvider.put(rgp.getName().toLowerCase(), rgp);
            //Print.logInfo("Added ReverseGeocodeProvider: [%s] %s", this, rgp.getName());
        }
    }

    /**
    *** Returns the active ReverseGeocodeProvider for this BasicPrivatelabel
    *** @return The active ReverseGeocodeProvider for this BasicPrivatelabel
    **/
    public ReverseGeocodeProvider getReverseGeocodeProvider()
    {
        if ((this.revgeoProvider != null) && (this.revgeoProvider.size() > 0)) {
            return this.revgeoProvider.getValue(0);
        } else {
            // TODO: return a default?
            return null;
        }
    }

    /**
    *** Returns the named ReverseGeocodeProvider for this BasicPrivatelabel
    *** @param name  The named ReverseGeocodeProvider to return
    *** @return The named ReverseGeocodeProvider for this BasicPrivatelabel
    **/
    public ReverseGeocodeProvider getReverseGeocodeProvider(String name)
    {
        if ((name != null) && (this.revgeoProvider != null)) {
            return this.revgeoProvider.get(name.toLowerCase());
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the GeocodeProvider for this BasicPrivateLabel
    *** @param gp The GeocodeProvider
    **/
    public void addGeocodeProvider(GeocodeProvider gp)
    {
        if (gp != null) {
            if (this.geocodeProvider == null) {
                this.geocodeProvider = new OrderedMap<String,GeocodeProvider>();
            }
            this.geocodeProvider.put(gp.getName().toLowerCase(), gp);
        }
    }

    /**
    *** Returns the active GeocodeProvider for this BasicPrivatelabel
    *** @return The active GeocodeProvider for this BasicPrivatelabel
    **/
    public GeocodeProvider getGeocodeProvider()
    {
        if ((this.geocodeProvider != null) && (this.geocodeProvider.size() > 0)) {
            return this.geocodeProvider.getValue(0);
        } else {
            // TODO: return a default?
            return null;
        }
    }

    /**
    *** Returns the named GeocodeProvider for this BasicPrivatelabel
    *** @param name  The named GeocodeProvider to return
    *** @return The named GeocodeProvider for this BasicPrivatelabel
    **/
    public GeocodeProvider getGeocodeProvider(String name)
    {
        if ((name != null) && (this.geocodeProvider != null)) {
            return this.geocodeProvider.get(name.toLowerCase());
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the MobileLocationProvider for this BasicPrivateLabel
    *** @param mp The MobileLocationProvider
    **/
    public void addMobileLocationProvider(MobileLocationProvider mp)
    {
        if (mp != null) {
            if (this.mobLocProvider == null) {
                this.mobLocProvider = new OrderedMap<String,MobileLocationProvider>();
            }
            //Print.logInfo("Adding MobileLocationProvider: ["+this.getName()+"] " + mp.getName());
            this.mobLocProvider.put(mp.getName().toLowerCase(), mp);
        }
    }

    /**
    *** Returns the active MobileLocationProvider for this BasicPrivatelabel
    *** @return The active MobileLocationProvider for this BasicPrivatelabel
    **/
    public MobileLocationProvider getMobileLocationProvider()
    {
        if ((this.mobLocProvider != null) && (this.mobLocProvider.size() > 0)) {
            return this.mobLocProvider.getValue(0);
        } else {
            // TODO: return a default?
            return null;
        }
    }

    /**
    *** Returns the named MobileLocationProvider for this BasicPrivatelabel
    *** @param name  The named MobileLocationProvider to return
    *** @return The named MobileLocationProvider for this BasicPrivatelabel
    **/
    public MobileLocationProvider getMobileLocationProvider(String name)
    {
        if ((name != null) && (this.mobLocProvider != null)) {
            return this.mobLocProvider.get(name.toLowerCase());
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the MapProvider's Pushpin index for the specified pushpin ID
    *** @param pushpinID      The pushpin ID
    *** @param dftIndex       The default index value (0..9 are always defined)
    *** @return The pushpin icon index
    **/
    public int getPushpinIconIndex(String pushpinID, int dftIndex)
    {
        return this.getPushpinIconIndex(null, pushpinID, dftIndex);
    }

    /**
    *** Return the MapProvider's Pushpin index for the specified pushpin ID
    *** @param mapProviderID  The MapProvider ID (may be null)
    *** @param pushpinID      The pushpin ID
    *** @param dftIndex       The default index value (0..9 are always defined)
    *** @return The pushpin icon index
    **/
    public int getPushpinIconIndex(String mapProviderID, String pushpinID, int dftIndex)
    {
        // PrivateLabel overrides this method to provide the specific MapProvider index
        return dftIndex;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the copyright notice for this BasicPrivateLabel
    *** @param copyright  The copyright notice
    **/
    public void setCopyright(String copyright)
    {
        String c = (copyright != null)? copyright.trim() : null;
        this.setStringProperty(BasicPrivateLabelLoader.TAG_Copyright, c);
    }

    /**
    *** Gets the copyright notice for this BasicPrivateLabel
    *** @return The copyright notice
    **/
    public String getCopyright()
    {
        String copyright = this.getStringProperty(BasicPrivateLabelLoader.TAG_Copyright, null);
        if (copyright != null) {
            return copyright;
        } else {
            return "Copyright (C) " + this.getPageTitle();
        }
    }

    // ------------------------------------------------------------------------
    private static boolean MERGE_ACL_ENTRIES = true;
    
    /**
    *** Sets the user Access-Control for this BasicPrivateLabel
    *** @param dftAccess  The defalt access level
    *** @param aclMap     The access-control map
    **/
    public void addAclMap(AccessLevel dftAccess, Map<String,AclEntry> aclMap)
    {
        
        /* default access (only if not already set) */
        if ((this.dftAccLevel == null) && (dftAccess != null)) {
            this.dftAccLevel = dftAccess;
        }
        
        /* ACL map */
        if (this.privateAclMap == null) {
            this.privateAclMap = new OrderedMap<String,AclEntry>(aclMap);
        } else
        if (MERGE_ACL_ENTRIES) {
            // if an ACL map already exists, then try to place any new entries 
            // next to similar existing entries
            for (String aclName : aclMap.keySet()) {
                AclEntry aclEntry = aclMap.get(aclName);
                int matchNdx = -1;
                int matchLen = 0;
                String matchName = null;
                for (int i = 0; i < this.privateAclMap.size(); i++) {
                    String pan = this.privateAclMap.getKey(i);
                    int diffLen = StringTools.diff(pan,aclName);
                    if (diffLen > matchLen) {
                        matchLen = diffLen;
                        matchNdx = i;
                        matchName = aclName.substring(0,diffLen);
                    }
                }
                if (matchNdx >= 0) {
                    int insertNdx = matchNdx;
                    for (insertNdx = matchNdx; insertNdx < this.privateAclMap.size(); insertNdx++) {
                        String pan = this.privateAclMap.getKey(insertNdx);
                        if (!pan.startsWith(matchName)) {
                            break;
                        }
                    }
                    this.privateAclMap.put(insertNdx,aclName,aclEntry);
                } else {
                    this.privateAclMap.put(aclName,aclEntry);
                }
            }
        } else {
            this.privateAclMap.putAll(aclMap);
        }
        
        /* clean ALL Acl entries (reloaded later) */
        this.allAclEntries = null;
        
    }

    /**
    *** Gets the maximum access-control level for this BasicPrivateLabel
    *** @return The maximum acces-control level
    **/
    public AccessLevel getMaximumAccessLevel(String aclName)
    {
        if (this.privateAclMap != null) {
            AclEntry acl = this.privateAclMap.get(aclName);
            return (acl != null)? acl.getMaximumAccessLevel() : AccessLevel.ALL;
        } else {
            return AccessLevel.ALL;
        }
    }

    /**
    *** Gets the global default access-control level for this BasicPrivateLabel
    *** @return The default access-control level (does not reutrn null)
    **/
    public AccessLevel getDefaultAccessLevel()
    {
        return (this.dftAccLevel != null)? this.dftAccLevel : AccessLevel.ALL;
    }

    /**
    *** Gets the default access-control level for this BasicPrivateLabel
    *** @param aclName  The ACL key
    *** @return The default acces-control level (does not reutrn null)
    **/
    public AccessLevel getDefaultAccessLevel(String aclName)
    {
        if (this.privateAclMap != null) {
            AclEntry acl = this.privateAclMap.get(aclName);
            return (acl != null)? acl.getDefaultAccessLevel() : this.getDefaultAccessLevel();
        } else {
            return this.getDefaultAccessLevel();
        }
    }
 
    /**
    *** Returns the AclEntry for the specified key
    *** @return The AclEntry, or null if the key does not exist
    **/
    public AclEntry getAclEntry(String aclName)
    {
        if ((this.privateAclMap != null) && !StringTools.isBlank(aclName)) {
            return this.privateAclMap.get(aclName);
        } else {
            return null;
        }
    }

    /**
    *** Returns true if the AclEntry key is defined
    *** @return True if the AclEntry key is defined
    **/
    public boolean hasAclEntry(String aclName)
    {
        return (this.getAclEntry(aclName) != null);
    }

    /**
    *** Returns all defined AclEntries
    *** @return An array of AclEntry items
    **/
    public AclEntry[] getAllAclEntries()
    {
        // TODO: postInit
        if (this.allAclEntries == null) {
            if (this.privateAclMap != null) {
                this.allAclEntries = new AclEntry[this.privateAclMap.size()];
                int a = 0;
                for (Iterator i = this.privateAclMap.values().iterator(); i.hasNext();) {
                    this.allAclEntries[a++] = (AclEntry)i.next();
                }
            } else {
                this.allAclEntries = new AclEntry[0];
            }
        }
        return this.allAclEntries;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the AccessLevel for the specified ACL key
    *** @param role     The Role
    *** @param aclName  The ACL key
    *** @return The AccessLevel for the specified ACL key
    **/
    public AccessLevel getAccessLevel(Role role, String aclName)
    {
        AclEntry aclEntry = this.getAclEntry(aclName);

        /* default access level */
        AccessLevel dft;
        if (aclEntry != null) {
            dft = aclEntry.getDefaultAccessLevel();
        } else {
            dft = this.getDefaultAccessLevel();
        }
        
        /* actual access level */
        AccessLevel acl;
        if (role != null) {
            acl = RoleAcl.getAccessLevel(role,aclName,dft);
        } else {
            acl = dft;
        }
        
        /* return result */
        return acl;
        
    }

    /**
    *** Returns true is user has 'ALL' access rights for the specified ACL key
    *** @param user     The User
    *** @param aclName  The ACL key
    *** @return True is user has 'ALL' access rights for the specified ACL key
    **/
    public AccessLevel getAccessLevel(User user, String aclName)
    {
        
        /* sysadmin user (no ACL restrictions) */
        if (User.isAdminUser(user)) { // returns true if "(user == null)"
            AclEntry aclEntry = this.getAclEntry(aclName);
            return (aclEntry != null)? aclEntry.getMaximumAccessLevel() : AccessLevel.ALL; // 'admin' has all rights
        }

        /* no user (should not occur, but handle anyway) */
        if (user == null) {
            // if we are here, then a 'null' user is not a SysAdmin user, return NONE
            return AccessLevel.NONE;
        }

        /* normal user */
        AccessLevel acl = UserAcl.getAccessLevel(user, aclName, null);
        if (acl == null) {
            acl = this.getAccessLevel(user.getRole(), aclName);
        }

        /* over maximum for user? */
        int maxAccessLevel = user.getMaxAccessLevel();
        if ((acl != null) && (acl.getIntValue() > maxAccessLevel)) {
            // limit to max access-level
            acl = AclEntry.getAccessLevel(maxAccessLevel);
        }

        /* return ACL */
        return acl;

    }

    /**
    *** Returns true is user has 'ALL' access rights for the specified ACL key
    *** @param user     The User
    *** @param aclName  The ACL key
    *** @return True is user has 'ALL' access rights for the specified ACL key
    **/
    public boolean hasAllAccess(User user, String aclName)
    {
        return AclEntry.okAll(this.getAccessLevel(user,aclName));
    }

    /**
    *** Returns true is user has 'WRITE' access rights for the specified ACL key
    *** @param user     The User
    *** @param aclName  The ACL key
    *** @return True is user has 'WRITE' access rights for the specified ACL key
    **/
    public boolean hasWriteAccess(User user, String aclName)
    {
        return AclEntry.okWrite(this.getAccessLevel(user,aclName));
    }

    /**
    *** Returns true is user has 'READ' access rights for the specified ACL key
    *** @param user     The User
    *** @param aclName  The ACL key
    *** @return True is user has 'READ' access rights for the specified ACL key
    **/
    public boolean hasReadAccess(User user, String aclName)
    {
        return AclEntry.okRead(this.getAccessLevel(user,aclName));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the event notification Email attributes
    *** @param emailFrom     The EMail 'From' address
    *** @param emailSubj     The EMail 'Subject'
    *** @param emailBody     The EMail 'Body'
    *** @param useAsDefault  True to use this subj/body as the default entry for Rule notifications
    **/
    public void setEventNotificationEMail(String emailFrom, 
        I18N.Text emailSubj, I18N.Text emailBody, boolean useAsDefault)
    {
        this.eventNotifyFrom    = SendMail.getUserFromEmailAddress(StringTools.trim(emailFrom));
        this.eventNotifySubj    = emailSubj;   // may be null
        this.eventNotifyBody    = emailBody;   // may be null
        this.eventNotifyDefault = useAsDefault && ((emailSubj != null) || (emailBody != null));
        this.setEMailAddress(EMAIL_TYPE_NOTIFY, this.eventNotifyFrom);
    }
    
    /**
    *** Gets the EMail notification 'From' address
    *** @return The Email notification 'From' address
    **/
    public String getEventNotificationFrom()
    {
        return SendMail.getUserFromEmailAddress(this.eventNotifyFrom);
    }
    
    /**
    *** Return true if the notification subject and/or message is defined
    *** @return True if the notification subject and/or message is defined
    **/
    public boolean hasEventNotificationEMail()
    {
        return (this.eventNotifySubj != null) || (this.eventNotifyBody != null);
    }

    /**
    *** Gets the EMail notification 'Subject'
    *** @return The Email notification 'Subject'
    **/
    public String getEventNotificationSubject()
    {
        if (this.eventNotifySubj != null) {
            return this.eventNotifySubj.toString(this.getLocale());
        } else {
            return null;
        }
    }

    /**
    *** Gets the EMail notification message 'Body'
    *** @return The Email notification message 'Body'
    **/
    public String getEventNotificationBody()
    {
        if (this.eventNotifyBody != null) {
            return this.eventNotifyBody.toString(this.getLocale());
        } else {
            return null;
        }
    }

    /**
    *** Returns true if the email notification subject/body is to be used as the
    *** default entry for new created Rule definitions.
    *** @return True if the event notification subject/body is to be used as the default.
    **/
    public boolean getEventNotificationDefault()
    {
        return this.eventNotifyDefault;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default EMail 'From' addresses
    *** @param type  The 'type' of default EMail address
    *** @param emailAddr  The EMail address
    **/
    public void setEMailAddress(String type, String emailAddr)
    {
        if (!StringTools.isBlank(emailAddr)) {
            emailAddr = SendMail.getUserFromEmailAddress(emailAddr.trim());
            //if (emailAddr.endsWith("example.com")) {
            //    String t = (StringTools.isBlank(type) || type.equals(EMAIL_TYPE_DEFAULT))? "<default>" : type;
            //    Print.logWarn("EMail address not yet customized ["+t+"] '"+emailAddr+"'");
            //}
            if (StringTools.isBlank(type) || type.equals(EMAIL_TYPE_DEFAULT)) {
                // explicitly set default email address
                this.setProperty(BasicPrivateLabelLoader.TAG_EMailAddress, emailAddr);
            } else {
                this.setProperty(BasicPrivateLabelLoader.TAG_EMailAddress + "_" + type, emailAddr);
                if (!this.hasProperty(BasicPrivateLabelLoader.TAG_EMailAddress)) {
                    // set default email address, if not already defined
                    this.setProperty(BasicPrivateLabelLoader.TAG_EMailAddress, emailAddr);
                }
            }
        }
    }

    /**
    *** Gets the 'From' EMail address for the specified type
    *** @param type  The 'type' of EMail address to return
    *** @return The 'From' EMail address for the specified type
    **/
    public String getEMailAddress(String type)
    {
        String email = null;
        if (StringTools.isBlank(type) || type.equals(EMAIL_TYPE_DEFAULT)) {
            email = this.getStringProperty(BasicPrivateLabelLoader.TAG_EMailAddress, null);
            if (email == null) { email = DEFAULT_EMAIL_ADDRESS; }
        } else {
            email = this.getStringProperty(BasicPrivateLabelLoader.TAG_EMailAddress + "_" + type, null);
            if (email == null) {
                email = this.getStringProperty(BasicPrivateLabelLoader.TAG_EMailAddress, null);
                if (email == null) { email = DEFAULT_EMAIL_ADDRESS; }
            }
        }
        return SendMail.getUserFromEmailAddress(email);
    }
    
    /**
    *** Returns an array of all defined EMail addresses (used by CHeckInstall)
    *** @return An array of defined EMail addresses
    **/
    public String[] getEMailAddresses()
    {
        java.util.List<String> list = new Vector<String>();
        for (Iterator i = this.rtPropMap.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            if (key.startsWith(BasicPrivateLabelLoader.TAG_EMailAddress)) {
                list.add(this.getStringProperty(key,"?"));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if all IDs should be validated for proper characters
    **/
    public boolean globalValidateIDs()
    {
        return this.getBooleanProperty(PROP_Track_ValidateIDs, true);
    }

    /**
    *** Returns true if all IDs should be validated for proper characters
    **/
    public boolean globalEditAfterNew()
    {
        return this.getBooleanProperty(PROP_Track_editAfterNew, true);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets (appends) the the specified properties to this BasicPrivateLabel
    **/
    public void setRTProperties(RTProperties rtp)
    {
        if (rtp != null) {
            this.rtProps.setProperties(rtp);
        }
    }

    /**
    *** Gets the properties of this BasicPrivateLabel
    **/
    public RTProperties getRTProperties()
    {
        return this.rtProps;
    }

    /**
    *** Pushes the properties of this BasicPrivateLabel on the temporary RTConfig properties stack
    **/
    public void pushRTProperties()
    {
        RTConfig.pushTemporaryProperties(this.rtProps);
    }

    /**
    *** Pops the properties of this BasicPrivateLabel from the temporary RTConfig properties stack
    **/
    public void popRTProperties()
    {
        RTConfig.popTemporaryProperties(this.rtProps);
    }
    
    /**
    *** Prints the current properties to stdout
    **/
    public void printProperties()
    {
        this.printProperties(null);
    }
    
    /**
    *** Prints the current properties to stdout
    **/
    public void printProperties(String msg)
    {
        String m = !StringTools.isBlank(msg)?
            ("[" + this.getName() + "] " + msg) :
            ("PrivateLabel Properties: " + this.getName());
        this.rtProps.printProperties(m);
    }

    /**
    *** Sets the property value for the specified key
    *** @param key  The property key
    *** @param value  The property value
    **/
    public void setProperty(Object key, Object value)
    {
        String k = StringTools.trim(key);
        if (value != null) {
            this.rtPropMap.put(k, value);
        } else {
            this.rtPropMap.remove(k);
        }
    }

    /**
    *** Sets the property String value for the specified key
    *** @param key    The property key
    *** @param value  The property String value
    **/
    public void setStringProperty(String key, String value)
    {
        this.setProperty(key, value);
    }

    /**
    *** Sets the property I18N value for the specified key
    *** @param key    The property key
    *** @param value  The property I18N value
    **/
    public void setI18NTextProperty(String key, I18N.Text value)
    {
        this.setProperty(key, value);
    }

    /**
    *** Gets the property value for the specified key
    *** @param key  The property key
    *** @return The property value
    **/
    public Object getProperty(String key)
    {

        /* look for property in local property map */
        String k = (key != null)? key : "";
        Object obj = this.rtPropMap.get(k);
        return obj; // may return null

    }

    /**
    *** Gets the property keys matching the specified key prefix
    *** @param keyPrefix  The property key prefix
    *** @return A collection of property keys (a show copy)
    **/
    public Collection<String> getPropertyKeys(String keyPrefix)
    {
        return this.rtProps.getPropertyKeys(keyPrefix);
    }

    /**
    *** Returns true if the property key is defined by thie BasicPrivateLabel
    *** @param key  The property key
    *** @return True if the specified property key is defined by this BasicPrivateLabel
    **/
    public boolean hasProperty(String key)
    {
        return (key != null)? this.rtPropMap.containsKey(key) : false;
    }

    /**
    *** Gets the String property value for the specified key
    *** @param key  The property key
    *** @param dft  The default value returned if the property key is not defined
    *** @return The property String value
    **/
    public String getStringProperty(String key, String dft)
    {

        /* try temporary properties */
        RTProperties rtp = RTConfig.getTemporaryProperties(key);
        if (rtp != null) {
            Object v = rtp.getProperty(key,null);
            if (v instanceof String) {
                return v.toString();
            }
        }

        /* try locally defined properties */
        Object obj = this.getProperty(key);
        return (obj != null)? obj.toString() : dft;

    }

    /**
    *** Gets the I18N property value for the specified key
    *** @param key  The property key
    *** @param dft  The default value returned if the property key is not defined
    *** @return The property I18N value
    **/
    public I18N.Text getI18NTextProperty(String key, I18N.Text dft)
    {
        Object obj = this.getProperty(key);
        return (obj instanceof I18N.Text)? (I18N.Text)obj : dft;
    }

    /**
    *** Gets the Localized text for the specified String key
    *** @return The HTML page title
    **/
    public String getI18NTextString(String key, String dft)
    {

        /* try temporary properties */
        RTProperties rtp = RTConfig.getTemporaryProperties(key);
        if (rtp != null) {
            Object v = rtp.getProperty(key,null);
            if (v instanceof String) {
                return v.toString();
            }
        }

        /* try locally defined properties */
        I18N.Text text = this.getI18NTextProperty(key, null);
        return (text != null)? text.toString(this.getLocale()) : dft;
        
    }

    /**
    *** Gets the double property value for the specified key
    *** @param key  The property key
    *** @param dft  The default value returned if the property key is not defined
    *** @return The property double value
    **/
    public double getDoubleProperty(String key, double dft)
    {
        Object obj = this.getProperty(key);
        return (obj != null)? StringTools.parseDouble(obj,dft) : dft;
    }

    /**
    *** Gets the long property value for the specified key
    *** @param key  The property key
    *** @param dft  The default value returned if the property key is not defined
    *** @return The property long value
    **/
    public long getLongProperty(String key, long dft)
    {
        Object obj = this.getProperty(key);
        return (obj != null)? StringTools.parseLong(obj,dft) : dft;
    }

    /**
    *** Gets the int property value for the specified key
    *** @param key  The property key
    *** @param dft  The default value returned if the property key is not defined
    *** @return The property int value
    **/
    public int getIntProperty(String key, int dft)
    {
        Object obj = this.getProperty(key);
        return (obj != null)? StringTools.parseInt(obj,dft) : dft;
    }

    /**
    *** Gets the boolean property value for the specified key
    *** @param key  The property key
    *** @param dft  The default value returned if the property key is not defined
    *** @return The property boolean value
    **/
    public boolean getBooleanProperty(String key, boolean dft)
    {
        Object obj = this.getProperty(key);
        return (obj != null)? StringTools.parseBoolean(obj.toString(),dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the exclusive status codes state
    *** @param only  If true, only status codes set by this PrivateLabel will be visible
    **/
    public void setStatusCodeOnly(boolean only)
    {
        this.statusCodeOnly = only;
    }

    /**
    *** Gets the exclusive status codes state
    *** @return True if only status codes set by this PrivateLabel will be visible
    **/
    public boolean getStatusCodeOnly()
    {
        return this.statusCodeOnly && (this.statusCodes != null);
    }

    /**
    *** Adds a customized status code description override
    *** @param code  The StatusCode to add
    **/
    public void addStatusCode(StatusCodes.Code code)
    {
        if (code != null) {
            if (this.statusCodes == null) {
                this.statusCodes = new HashMap<Integer,StatusCodes.Code>();
            }
            this.statusCodes.put(new Integer(code.getCode()), code);
            //Print.logInfo("Added Code: " + code);
        }
    }

    /**
    *** Returns a Map of custom status codes
    *** @return A Map of custom status codes (or null if there are no custom status codes)
    **/
    public Map<Integer,StatusCodes.Code> getCustomStatusCodeMap()
    {
        return this.statusCodes;
    }

    /**
    *** Returns a Map of StatusCodes to their desriptions
    *** @return a Map of StatusCodes to their desriptions
    **/
    public Map<Integer,String> getStatusCodeDescriptionMap()
    {
        Locale locale = this.getLocale();
        Map<Integer,String> descMap = this.getStatusCodeOnly()?
            new OrderedMap<Integer,String>() :
            StatusCodes.GetDescriptionMap(locale);
        Map<Integer,StatusCodes.Code> csc = this.getCustomStatusCodeMap();
        if (csc != null) {
            for (Integer sci : csc.keySet()) {
                descMap.put(sci, csc.get(sci).getDescription(locale));
            }
        }
        return descMap;
    }

    /**
    *** Return specific code (from statusCode)
    *** @param code The status code
    *** @return The StatusCode.Code instance
    **/
    public StatusCodes.Code getStatusCode(Integer code)
    {
        //Print.logInfo("Looking up Code: " + code);
        if (this.statusCodes != null) {
            return this.statusCodes.get(code);
        } else {
            return null;
        }
    }

    /**
    *** Return specific code (from statusCode)
    *** @param code The status code
    *** @return The StatusCode.Code instance
    **/
    public StatusCodes.Code getStatusCode(int code)
    {
        if (this.statusCodes != null) {
            return this.statusCodes.get(new Integer(code));
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the global PointsOfInterest (overridden)
    *** @return The PointsOfInterest list
    **/
    public java.util.List<PoiProvider> getPointsOfInterest()
    {
        return null;
    }

    // ------------------------------------------------------------------------

}
