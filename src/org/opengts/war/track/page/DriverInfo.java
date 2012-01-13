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
//  2010/04/11  Martin D. Flynn
//     -Initial release (cloned from StatusCodeInfo.java)
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

public class DriverInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_DRIVER_SELECT      = "DriverInfoSelect";
    public  static final String FORM_DRIVER_EDIT        = "DriverInfoEdit";
    public  static final String FORM_DRIVER_NEW         = "DriverInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE     = "update";
    public  static final String COMMAND_INFO_SELECT     = "select";
    public  static final String COMMAND_INFO_NEW        = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT        = "c_subedit";
    public  static final String PARM_SUBMIT_VIEW        = "c_subview";
    public  static final String PARM_SUBMIT_CHG         = "c_subchg";
    public  static final String PARM_SUBMIT_DEL         = "c_subdel";
    public  static final String PARM_SUBMIT_NEW         = "c_subnew";

    // buttons
    public  static final String PARM_BUTTON_CANCEL      = "u_btncan";
    public  static final String PARM_BUTTON_BACK        = "u_btnbak";

    // parameters
    public  static final String PARM_NEW_NAME           = "d_newid";
    public  static final String PARM_DRIVER_SELECT      = "d_driver";
    public  static final String PARM_DRIVER_NAME        = "d_fullnm";
    public  static final String PARM_INFORMAL_NAME      = "d_nicknm";
    public  static final String PARM_CONTACT_PHONE      = "d_phone";
    public  static final String PARM_CONTACT_EMAIL      = "d_email";
    public  static final String PARM_LICENSE_TYPE       = "d_lictype";
    public  static final String PARM_LICENSE_NUMBER     = "d_license";
    public  static final String PARM_LICENSE_EXPIRE     = "d_licexp";
    public  static final String PARM_BADGE_ID           = "d_badge";
    public  static final String PARM_FULL_ADDRESS       = "d_address";
    public  static final String PARM_BIRTHDATE          = "d_birthdt";  // TODO
    public  static final String PARM_DEVICE_ID          = "d_devid";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public DriverInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_DRIVER_INFO);
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
        I18N i18n = privLabel.getI18N(DriverInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("DriverInfo.editMenuDesc","View/Edit Driver Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DriverInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("DriverInfo.editMenuHelp","View and Edit Driver information"));
    }
    
    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DriverInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("DriverInfo.navDesc","Driver"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DriverInfo.class);
        return i18n.getString("DriverInfo.navTab","Driver Admin");
    }

    // ------------------------------------------------------------------------
    
    private static long GetDayNumber(String ymd)
    {
        if (StringTools.isBlank(ymd)) {
            return 0L;
        } else {
            DayNumber dn = DayNumber.parseDayNumber(ymd);
            return (dn != null)? dn.getDayNumber() : -1L;
        }
    }

    private static String FormatDayNumber(long dn)
    {
        if (dn < 0L) {
            return "";
        } else {
            return (new DayNumber(dn)).format(DayNumber.DATE_FORMAT_1);
        }
    }
    
    // ------------------------------------------------------------------------

    private static String Filter(String s)
    {
        if (StringTools.isBlank(s)) {
            return "&nbsp;";
        } else {
            return s;
        }
    }
    
    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final I18N    i18n       = privLabel.getI18N(DriverInfo.class);
        final Locale  locale     = reqState.getLocale();
        final Account currAcct   = reqState.getCurrentAccount(); // never null
        final String  currAcctID = currAcct.getAccountID(); // never null
        final User    currUser   = reqState.getCurrentUser(); // may be null
        final String  pageName   = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* List of drivers */
        OrderedSet<String> driverList = null;
        try {
            driverList = Driver.getDriverIDsForAccount(currAcctID);
        } catch (DBException dbe) {
            driverList = new OrderedSet<String>();
        }

        /* selected geozone */
        String selDriverID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_DRIVER_SELECT, "");
        if (!StringTools.isBlank(selDriverID) && !driverList.contains(selDriverID)) {
            selDriverID = "";
        }

        /* driver db */
        Driver selDriver = null;
        try {
            selDriver = !selDriverID.equals("")? Driver.getDriver(currAcct, selDriverID) : null; // may still be null
        } catch (DBException dbe) {
            // ignore
        }

        /* ACL allow edit/view */
        boolean allowNew     = privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete  = allowNew;
        boolean allowEdit    = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView    = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* submit buttons */
        String submitEdit    = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String submitView    = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String submitChange  = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String submitNew     = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String submitDelete  = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* command */
        String  driverCmd    = reqState.getCommandName();
        boolean selectDriver = driverCmd.equals(COMMAND_INFO_SELECT);
        boolean newDriver    = driverCmd.equals(COMMAND_INFO_NEW);
        boolean updateDriver = driverCmd.equals(COMMAND_INFO_UPDATE);
        boolean deleteDriver = false;

        /* ui display */
        boolean uiList       = false;
        boolean uiEdit       = false;
        boolean uiView       = false;
        
        /* config */
        final boolean showDeviceID = privLabel.getBooleanProperty(PrivateLabel.PROP_DriverInfo_showDeviceID,false);

        /* sub-command */
        String newDriverID = null;
        if (newDriver) {
            if (!allowNew) {
               newDriver = false; // not authorized
            } else {
                newDriverID = AttributeTools.getRequestString(request,PARM_NEW_NAME,"").trim();
                newDriverID = newDriverID.toLowerCase();
                if (StringTools.isBlank(newDriverID)) {
                    m = i18n.getString("DriverInfo.enterNewID","Please enter a valid new Driver ID.");
                    error = true;
                    newDriver = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_DriverInfo_validateNewIDs,*/newDriverID)) {
                    m = i18n.getString("DriverInfo.invalidIDChar","ID contains invalid characters");
                    error = true;
                    newDriver = false;
                }
            }
        } else
        if (updateDriver) {
            if (!allowEdit) {
                // not authorized to update drivers
                updateDriver = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("DriverInfo.change","Change"))) {
                updateDriver = false;
            } else
            if (selDriver == null) {
                // should not occur
                m = i18n.getString("DriverInfo.unableToUpdate","Unable to update Driver, ID not found");
                error = true;
                updateDriver = false;
            }
        } else
        if (selectDriver) {
            if (SubmitMatch(submitDelete,i18n.getString("DriverInfo.delete","Delete"))) {
                if (!allowDelete) {
                    deleteDriver = false;
                } else
                if (selDriver == null) {
                    m = i18n.getString("DriverInfo.pleaseSelectDriver","Please select a Driver");
                    error = true;
                    deleteDriver = false; // not selected
                } else {
                    deleteDriver = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("DriverInfo.edit","Edit"))) {
                if (!allowEdit) {
                    uiEdit = false; // not authorized
                } else
                if (selDriver == null) {
                    m = i18n.getString("DriverInfo.pleaseSelectDriver","Please select a Driver");
                    error = true;
                    uiEdit = false; // not selected
                } else {
                    uiEdit = true;
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("DriverInfo.view","View"))) {
                if (!allowView) {
                    uiView = false; // not authorized
                } else
                if (selDriver == null) {
                    m = i18n.getString("DriverInfo.pleaseSelectDriver","Please select a Driver");
                    error = true;
                    uiView = false; // not selected
                } else {
                    uiView = true;
                }
            } else {
                uiList = true;
            }
        } else {
            uiList = true;
        }

        /* delete Driver? */
        if (deleteDriver) {
            try {
                Driver.Key driverKey = (Driver.Key)selDriver.getRecordKey();
                Print.logWarn("Deleting Driver: " + driverKey);
                driverKey.delete(true); // will also delete dependencies
                selDriverID = "";
                selDriver = null;
                // select another driver
                driverList = Driver.getDriverIDsForAccount(currAcctID);
                if (!ListTools.isEmpty(driverList)) {
                    selDriverID = driverList.get(0);
                    try {
                        selDriver = !selDriverID.equals("")? Driver.getDriver(currAcct,selDriverID) : null; // may still be null
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Deleting Driver", dbe);
                m = i18n.getString("DriverInfo.errorDelete","Internal error deleting Driver");
                error = true;
            }
            uiList = true;
        }

        /* new Driver? */
        if (newDriver) {
            boolean createDriverOK = true;
            for (int u = 0; u < driverList.size(); u++) {
                if (newDriverID.equalsIgnoreCase(driverList.get(u))) {
                    m = i18n.getString("DriverInfo.alreadyExists","This Driver already exists");
                    error = true;
                    createDriverOK = false;
                    break;
                }
            }
            if (createDriverOK) {
                try {
                    Driver driver = Driver.createNewDriver(currAcct, newDriverID); // already saved
                    driverList = Driver.getDriverIDsForAccount(currAcctID);
                    selDriver = driver;
                    selDriverID = driver.getDriverID();
                    Print.logInfo("Created driver '%s'", selDriverID);
                    m = i18n.getString("DriverInfo.createdDriver","New Driver has been created");
                } catch (DBException dbe) {
                    Print.logException("Creating Driver", dbe);
                    m = i18n.getString("DriverInfo.errorCreate","Internal error creating Driver");
                    error = true;
                }
            }
            uiList = true;
        }

        /* change/update the Driver info? */
        if (updateDriver) {
            selDriver.clearChanged();
            String driverDesc    = AttributeTools.getRequestString(request, PARM_DRIVER_NAME   ,"");
            String nickName      = AttributeTools.getRequestString(request, PARM_INFORMAL_NAME ,"");
            String contactPhone  = AttributeTools.getRequestString(request, PARM_CONTACT_PHONE ,"");
            String contactEmail  = AttributeTools.getRequestString(request, PARM_CONTACT_EMAIL ,"");
            String licenseType   = AttributeTools.getRequestString(request, PARM_LICENSE_TYPE  ,"");
            String licenseNumber = AttributeTools.getRequestString(request, PARM_LICENSE_NUMBER,"");
            String licenseExpire = AttributeTools.getRequestString(request, PARM_LICENSE_EXPIRE,"");
            String badgeID       = AttributeTools.getRequestString(request, PARM_BADGE_ID      ,"");
            String fullAddress   = AttributeTools.getRequestString(request, PARM_FULL_ADDRESS  ,"");
            String deviceID      = showDeviceID? AttributeTools.getRequestString(request, PARM_DEVICE_ID, null) : null;
          //String birthDate     = AttributeTools.getRequestString(request, PARM_BIRTHDATE     ,"");
            try {
                boolean saveOK = true;
                // description
                if (!driverDesc.equals(selDriver.getDescription())) {
                    selDriver.setDescription(driverDesc);
                }
                // mickname
                if (!nickName.equals(selDriver.getDisplayName())) {
                    selDriver.setDisplayName(nickName);
                }
                // contact
                if (!contactPhone.equals(selDriver.getContactPhone())) {
                    selDriver.setContactPhone(contactPhone);
                }
                if (!contactEmail.equals(selDriver.getContactEmail())) {
                    selDriver.setContactEmail(contactEmail);
                }
                // license
                if (!licenseType.equals(selDriver.getLicenseType())) {
                    selDriver.setLicenseType(licenseType);
                }
                if (!licenseNumber.equals(selDriver.getLicenseNumber())) {
                    selDriver.setLicenseNumber(licenseNumber);
                }
                long licenseExpDN = GetDayNumber(licenseExpire);
                if ((licenseExpDN >= 0L) && (licenseExpDN != selDriver.getLicenseExpire())) {
                    selDriver.setLicenseExpire(licenseExpDN);
                }
                // badge
                if (!badgeID.equals(selDriver.getBadgeID())) {
                    selDriver.setBadgeID(badgeID);
                }
                // address
                if (!fullAddress.equals(selDriver.getAddress())) {
                    selDriver.setAddress(fullAddress);
                }
                // deviceID
                if (showDeviceID && (deviceID != null) && !deviceID.equals(selDriver.getDeviceID())) {
                    selDriver.setDeviceID(deviceID);
                }
                // save
                if (saveOK) {
                    selDriver.save();
                    m = i18n.getString("DriverInfo.driverUpdated","Driver information updated");
                } else {
                    // should stay on this page
                }
            } catch (Throwable t) {
                Print.logException("Updating Driver", t);
                m = i18n.getString("DriverInfo.errorUpdate","Internal error updating Driver");
                error = true;
                //return;
            }
            uiList = true;
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = DriverInfo.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "DriverInfo.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
            }
        };

        /* Content */
        final OrderedSet<String> _driverList = driverList;
        final Driver     _selDriver     = selDriver;
        final boolean    _allowEdit     = allowEdit;
        final boolean    _allowView     = allowView;
        final boolean    _allowDelete   = allowDelete;
        final boolean    _allowNew      = allowNew;
        final boolean    _uiEdit        = _allowEdit && uiEdit;
        final boolean    _uiView        = _uiEdit || uiView;
        final boolean    _uiList        = uiList || (!_uiEdit && !_uiView);
        HTMLOutput HTML_CONTENT = null;
        if (_uiList) {
            final String _selDriverID = (selDriverID.equals("") && (driverList.size() > 0))? driverList.get(0) : selDriverID;

            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String pageName = DriverInfo.this.getPageName();

                    // frame header
                  //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                    String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                    String editURL    = DriverInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String selectURL  = DriverInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = DriverInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String frameTitle = _allowEdit? 
                        i18n.getString("DriverInfo.viewEditDriver","View/Edit Driver Information") : 
                        i18n.getString("DriverInfo.viewDriver","View Driver Information");
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");
                    
                    // DriverInfo selection table (Select, DriverInfo ID, DriverInfo Name)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("DriverInfo.selectDriver","Select a Driver")+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_DRIVER_SELECT+"' method='post' action='"+selectURL+"' target='_top'>");
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+i18n.getString("DriverInfo.select","Select")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("DriverInfo.driverID","Driver ID")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("DriverInfo.description","Description")+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    for (int u = 0; u < _driverList.size(); u++) {
                        String drid = _driverList.get(u);
                        if ((u & 1) == 0) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        try {
                            Driver driver = Driver.getDriver(currAcct, drid);
                            if (driver != null) {
                                String driverID   = driver.getDriverID();
                                String driverDesc = Filter(driver.getDescription());
                                String checked    = _selDriverID.equals(driver.getDriverID())? "checked" : "";
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"'><input type='radio' name='"+PARM_DRIVER_SELECT+"' id='"+driverID+"' value='"+driverID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+driverID+"'>"+driverID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+driverDesc+"</td>\n");
                            }
                        } catch (DBException dbe) {
                            // 
                        }
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("DriverInfo.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("DriverInfo.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("DriverInfo.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    
                    /* new Driver */
                    if (_allowNew) {
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("DriverInfo.createNewDriver","Create a new Driver")+":</h1>\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form name='"+FORM_DRIVER_NEW+"' method='post' action='"+newURL+"' target='_self'>"); // target='_top'
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                    out.write(i18n.getString("DriverInfo.driverID","Driver ID")+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                    out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("DriverInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    }
                    
                }
            };

        } else
        if (_uiEdit || _uiView) {
            final String _selDriverID = selDriverID;

            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String pageName = DriverInfo.this.getPageName();
    
                    // frame header
                  //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                    String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                    String editURL    = DriverInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String selectURL  = DriverInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = DriverInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String frameTitle = _allowEdit? 
                        i18n.getString("DriverInfo.viewEditDriver","View/Edit Driver Information") : 
                        i18n.getString("DriverInfo.viewDriver","View Driver Information");
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");

                    /* start of form */
                    out.write("<form name='"+FORM_DRIVER_EDIT+"' method='post' action='"+editURL+"' target='_top'>\n");
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* Driver fields */
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_DRIVER_SELECT  , false  , i18n.getString("DriverInfo.driverID","Driver ID")+":"              , _selDriverID, 8, 8));
                    out.println(FormRow_TextField(PARM_DRIVER_NAME    , _uiEdit, i18n.getString("DriverInfo.driverName","Driver Name")+":"          , (_selDriver!=null)?_selDriver.getDescription()  :"", 50, 80));
                    out.println(FormRow_TextField(PARM_INFORMAL_NAME  , _uiEdit, i18n.getString("DriverInfo.informalName","Nickname")+":"           , (_selDriver!=null)?_selDriver.getDisplayName()  :"", 15, 40));
                    out.println(FormRow_TextField(PARM_CONTACT_PHONE  , _uiEdit, i18n.getString("DriverInfo.contactPhone","Contact Phone")+":"      , (_selDriver!=null)?_selDriver.getContactPhone() :"", 24, 32));
                    out.println(FormRow_TextField(PARM_CONTACT_EMAIL  , _uiEdit, i18n.getString("DriverInfo.contactEmail","Contact EMail")+":"      , (_selDriver!=null)?_selDriver.getContactEmail() :"", 40, 80));
                    out.println(FormRow_TextField(PARM_BADGE_ID       , _uiEdit, i18n.getString("DriverInfo.badgeID","Badge ID")+":"                , (_selDriver!=null)?_selDriver.getBadgeID()      :"", 32, 32));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextField(PARM_LICENSE_TYPE   , _uiEdit, i18n.getString("DriverInfo.licenseType","License Type")+":"        , (_selDriver!=null)?_selDriver.getLicenseType()  :"", 24, 24));
                    out.println(FormRow_TextField(PARM_LICENSE_NUMBER , _uiEdit, i18n.getString("DriverInfo.licenseNumber","License Number")+":"    , (_selDriver!=null)?_selDriver.getLicenseNumber():"", 32, 32));
                    out.println(FormRow_TextField(PARM_LICENSE_EXPIRE , _uiEdit, i18n.getString("DriverInfo.licenseExpire","License Expiration")+":", (_selDriver!=null)?FormatDayNumber(_selDriver.getLicenseExpire()):"",13,13, i18n.getString("DriverInfo.dateYMD","(yyyy/mm/dd)")));
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextField(PARM_FULL_ADDRESS   , _uiEdit, i18n.getString("DriverInfo.fullAddress","Address")+":"             , (_selDriver!=null)?_selDriver.getAddress()      :"", 60, 90));
                    if (showDeviceID) {
                        String devTitles[] = reqState.getDeviceTitles();
                        String selDevID    = (_selDriver != null)? _selDriver.getDeviceID() : "";
                        ComboMap devMap    = new ComboMap(reqState.createDeviceDescriptionMap(true/*includeID*/)); 
                        devMap.insert("", i18n.getString("DriverInfo.noDevice","None",devTitles));
                        out.println(FormRow_Separator());
                        out.println(FormRow_ComboBox (PARM_DEVICE_ID, _uiEdit, i18n.getString("DriverInfo.deviceID","{0} ID",devTitles)+":", selDevID, devMap, "", -1));
                    }
                    out.println("</table>");

                    /* end of form */
                    out.write("<hr style='margin-bottom:5px;'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_uiEdit) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("DriverInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("DriverInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("DriverInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    }
                    out.write("</form>\n");

                }
            };

        }

        /* write frame */
        String onload = error? JS_alert(true,m) : null;
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
