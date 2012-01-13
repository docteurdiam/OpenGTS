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
//  2008/08/08  Martin D. Flynn
//     -Initial release
//  2008/08/17  Martin D. Flynn
//     -Added "Distance" title line (below "Cursor Location")
//     -Fix display of View/Edit buttons on creation of first user.
//  2008/09/01  Martin D. Flynn
//     -Added delete confirmation
//  2008/10/16  Martin D. Flynn
//     -Update with new ACL usage
//  2008/12/01  Martin D. Flynn
//     -Added ability to display multiple points
//  2009/08/23  Martin D. Flynn
//     -Convert new entered IDs to lowercase
//  2010/04/11  Martin D. Flynn
//     -Added support for drawing polygons and corridors, however, MapProvider
//      support for these features is also required (and in the case of corridors,
//      may also require add-on module support to use the corridor geozones
//      properly).
//  2011/05/13  Martin D. Flynn
//     -Added support for limiting the number of displayed vertices.
//      (see property "zoneInfo.maximumDisplayedVertices")
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
import org.opengts.geocoder.GeocodeProvider;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class ZoneInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // 'private.xml' properties

    // PrivateLabel.PROP_ZoneInfo_mapControlLocation
    private static final String CONTROLS_ON_LEFT[]              = new String[] { "left", "true" };

    // ------------------------------------------------------------------------

    private static final double MIN_RADIUS_METERS               = Geozone.MIN_RADIUS_METERS;
    private static final double MAX_RADIUS_METERS               = Geozone.MAX_RADIUS_METERS;
    
    private static final double DEFAULT_ZONE_RADIUS             = 20000.0;

    // ------------------------------------------------------------------------

    private static final String OVERLAP_PRIORITY[]              = new String[] { "0", "1", "2", "3", "4", "5" };

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_ZONE_SELECT                = "ZoneInfoSelect";
    public  static final String FORM_ZONE_EDIT                  = "ZoneInfoEdit";
    public  static final String FORM_ZONE_NEW                   = "ZoneInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE             = "update";
    public  static final String COMMAND_INFO_SELECT             = "select";
    public  static final String COMMAND_INFO_NEW                = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT                = "z_subedit";
    public  static final String PARM_SUBMIT_VIEW                = "z_subview";
    public  static final String PARM_SUBMIT_CHG                 = "z_subchg";
    public  static final String PARM_SUBMIT_DEL                 = "z_subdel";
    public  static final String PARM_SUBMIT_NEW                 = "z_subnew";

    // buttons
    public  static final String PARM_BUTTON_CANCEL              = "u_btncan";
    public  static final String PARM_BUTTON_BACK                = "u_btnbak";

    // parameters
    public  static final String PARM_NEW_ID                     = "z_newid";
    public  static final String PARM_NEW_TYPE                   = "z_newtype";

    // parameters
    public  static final String PARM_ZONE_SELECT                = "z_zone";
    public  static final String PARM_PRIORITY                   = "z_priority";
    public  static final String PARM_REV_GEOCODE                = "z_revgeo";
    public  static final String PARM_ARRIVE_NOTIFY              = "z_arrive";
    public  static final String PARM_DEPART_NOTIFY              = "z_depart";
    public  static final String PARM_AUTO_NOTIFY                = "z_autontfy";
    public  static final String PARM_CLIENT_UPLOAD              = "z_upload";
    public  static final String PARM_CLIENT_ID                  = "z_clntid";
    public  static final String PARM_SPEED_LIMIT                = "z_spdlim";
    
    public  static final String PARM_ZONE_DESC                  = "z_desc";
    public  static final String PARM_ZONE_RADIUS                = "z_radius";
    public  static final String PARM_ZONE_INDEX                 = "z_index";
    public  static final String PARM_ZONE_COLOR                 = "z_color";

    public  static final String PARM_ZONE_LATITUDE_             = "z_lat";
    private static String PARM_ZONE_LATITUDE(int ndx)
    {
        return PARM_ZONE_LATITUDE_ + ndx;
    }
    public  static final String PARM_ZONE_LONGITUDE_            = "z_lon";
    public static final String  PARM_ZONE_LONGITUDE(int ndx)
    {
        return PARM_ZONE_LONGITUDE_ + ndx;
    }

    // sort ID
    private static final int    DEFAULT_SORT_ID                 = 0;
    
    // point index
    private static final int    DEFAULT_POINT_INDEX             = 0;

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public ZoneInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_ZONE_INFO);
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
        I18N i18n = privLabel.getI18N(ZoneInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("ZoneInfo.editMenuDesc","View/Edit Geozone Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ZoneInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("ZoneInfo.editMenuHelp","View and Edit Geozone information"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ZoneInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("ZoneInfo.navDesc","Geozone"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ZoneInfo.class);
        return super._getNavigationTab(reqState,i18n.getString("ZoneInfo.navTab","Geozone Admin"));
    }

    // ------------------------------------------------------------------------
    
    private static int parseClientUploadFlag(PrivateLabel privLabel)
    {
        String uplFlag = privLabel.getStringProperty(PrivateLabel.PROP_ZoneInfo_showClientUploadZone,"");
        if (StringTools.isBlank(uplFlag)) {
            return 0;   // do not show
        } else
        if (uplFlag.equalsIgnoreCase("false")) {
            return 0;   // do not show
        } else
        if (uplFlag.equalsIgnoreCase("true") || uplFlag.equalsIgnoreCase("checkbox")) {
            return 1;   // show checkbox only
        } else
        if (uplFlag.equalsIgnoreCase("id")) {
            return 2;   // show id field only
        } else {
            return 0;
        }
    }

    private static ComboMap GetColorComboMap(I18N i18n)
    {
        ComboMap cc = new ComboMap();
        cc.add(""                                   ,i18n.getString("ZoneInfo.color.default","Default"));
        cc.add(ColorTools.BLACK.toString(true)      ,i18n.getString("ZoneInfo.color.black"  ,"Black"  ));
        cc.add(ColorTools.BROWN.toString(true)      ,i18n.getString("ZoneInfo.color.brown"  ,"Brown"  ));
        cc.add(ColorTools.RED.toString(true)        ,i18n.getString("ZoneInfo.color.red"    ,"Red"    ));
        cc.add(ColorTools.ORANGE.toString(true)     ,i18n.getString("ZoneInfo.color.orange" ,"Orange" ));
        cc.add(ColorTools.YELLOW.toString(true)     ,i18n.getString("ZoneInfo.color.yellow" ,"Yellow" ));
        cc.add(ColorTools.GREEN.toString(true)      ,i18n.getString("ZoneInfo.color.green"  ,"Green"  ));
        cc.add(ColorTools.BLUE.toString(true)       ,i18n.getString("ZoneInfo.color.blue"   ,"Blue"   ));
        cc.add(ColorTools.PURPLE.toString(true)     ,i18n.getString("ZoneInfo.color.purple" ,"Purple" ));
        cc.add(ColorTools.DARK_GRAY.toString(true)  ,i18n.getString("ZoneInfo.color.gray"   ,"Gray"   ));
        cc.add(ColorTools.WHITE.toString(true)      ,i18n.getString("ZoneInfo.color.white"  ,"White"  ));
        cc.add(ColorTools.CYAN.toString(true)       ,i18n.getString("ZoneInfo.color.cyan"   ,"Cyan"   ));
        cc.add(ColorTools.PINK.toString(true)       ,i18n.getString("ZoneInfo.color.pink"   ,"Pink"   ));
        return cc;
    }

    private static int getGeozoneSupportedPointCount(RequestProperties reqState, int type)
    {
        PrivateLabel privLabel  = reqState.getPrivateLabel(); // never null
        MapProvider mapProvider = reqState.getMapProvider();  // may be null
        int pointCount = (mapProvider != null)? mapProvider.getGeozoneSupportedPointCount(type) : 0;
        int maxCount   = privLabel.getIntProperty(PrivateLabel.PROP_ZoneInfo_maximumDisplayedVertices, -1);
        if ((maxCount > 0) && (pointCount > maxCount)) {
            pointCount = maxCount;
        }
        return pointCount;
    }

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final I18N    i18n     = privLabel.getI18N(ZoneInfo.class);
        final Locale  locale   = reqState.getLocale();
        final Account currAcct = reqState.getCurrentAccount(); // never null
        final User    currUser = reqState.getCurrentUser(); // may be null
        final String  pageName = this.getPageName();
        final boolean showOverlapPriority  = Geozone.supportsPriority() && privLabel.getBooleanProperty(PrivateLabel.PROP_ZoneInfo_showOverlapPriority,false);
        final boolean showSpeedLimit       = Geozone.supportsSpeedLimitKPH() && privLabel.getBooleanProperty(PrivateLabel.PROP_ZoneInfo_showSpeedLimit,false);
        final boolean showArriveDepartZone = Device.hasRuleFactory() || privLabel.getBooleanProperty(PrivateLabel.PROP_ZoneInfo_showArriveDepartZone,false);
        final boolean showAutoNotify       = showArriveDepartZone && privLabel.getBooleanProperty(PrivateLabel.PROP_ZoneInfo_showAutoNotify,false);
        final int     showClientUploadZone = parseClientUploadFlag(privLabel);
        String  m = pageMsg;
        boolean error = false;

        /* list of geozones */
        String zoneList[] = null;
        try {
            zoneList = Geozone.getGeozoneIDsForAccount(currAcct.getAccountID());
        } catch (DBException dbe) {
            zoneList = new String[0];
        }

        /* selected geozone */
        String selZoneID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_ZONE_SELECT, "");
        if (StringTools.isBlank(selZoneID)) {
            if ((zoneList.length > 0) && (zoneList[0] != null)) {
                selZoneID = zoneList[0];
            } else {
                selZoneID = "";
            }
            //Print.logWarn("No Zone selected, choosing first zone: %s", selZoneID);
        }
        if (zoneList.length == 0) {
            zoneList = new String[] { selZoneID };
        }
        
        /* Geozone db */
        Geozone selZone = null;
        try {
            selZone = !selZoneID.equals("")? Geozone.getGeozone(currAcct,selZoneID,DEFAULT_SORT_ID,false) : null;
        } catch (DBException dbe) {
            // ignore
        }

        /* ACL */
        boolean allowNew    = privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete = allowNew;
        boolean allowEdit   = allowNew || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView   = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* command */
        String zoneCmd      = reqState.getCommandName();
        boolean listZones   = false;
        boolean updateZone  = zoneCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectZone  = zoneCmd.equals(COMMAND_INFO_SELECT);
        boolean newZone     = zoneCmd.equals(COMMAND_INFO_NEW);
        boolean deleteZone  = false;
        boolean editZone    = false;
        boolean viewZone    = false;
        
        /* submit buttons */
        String submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* MapProvider support */
        final MapProvider mapProvider = reqState.getMapProvider(); // check below to make sure this is not null
        final boolean mapSupportsCursorLocation = ((mapProvider != null) && mapProvider.isFeatureSupported(MapProvider.FEATURE_LATLON_DISPLAY));
        final boolean mapSupportsDistanceRuler  = ((mapProvider != null) && mapProvider.isFeatureSupported(MapProvider.FEATURE_DISTANCE_RULER));
        final boolean mapSupportsGeozones       = ((mapProvider != null) && mapProvider.isFeatureSupported(MapProvider.FEATURE_GEOZONES));

        /* sub-command */
        String newZoneID   = null;
        int    newZoneType = Geozone.GeozoneType.POINT_RADIUS.getIntValue(); // default
        if (newZone) {
            if (!allowNew) {
                // not authorized to create new Geozones
                Print.logInfo("Not authorized to create a new Geozone ...");
                newZone = false;
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newZoneID   = AttributeTools.getRequestString(httpReq,PARM_NEW_ID,"").trim().toLowerCase();
                newZoneType = AttributeTools.getRequestInt(httpReq,PARM_NEW_TYPE, newZoneType);
                if (StringTools.isBlank(newZoneID)) {
                    m = i18n.getString("ZoneInfo.enterNewZone","Please enter a new Geozone name.");
                    error = true;
                    newZone = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_ZoneInfo_validateNewIDs,*/newZoneID)) {
                    m = i18n.getString("ZoneInfo.invalidIDChar","ID contains invalid characters");
                    error = true;
                    newZone = false;
                }
            }
        } else
        if (updateZone) {
            if (!allowEdit) {
                // not authorized to update Geozones
                updateZone = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("ZoneInfo.change","Change"))) {
                updateZone = false;
            } else
            if (selZone == null) {
                // should not occur
                m = i18n.getString("ZoneInfo.unableToUpdate","Unable to update Geozone, ID not found");
                error = true;
                updateZone = false;
            }
        } else
        if (selectZone) {
            if (SubmitMatch(submitDelete,i18n.getString("ZoneInfo.delete","Delete"))) {
                if (allowDelete) {
                    deleteZone = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("ZoneInfo.edit","Edit"))) {
                if (allowEdit) {
                    if (selZone == null) {
                        m = i18n.getString("ZoneInfo.pleaseSelectGeozone","Please select a Geozone");
                        error = true;
                        listZones = true;
                    } else {
                        editZone = true;
                        viewZone = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("ZoneInfo.view","View"))) {
                if (allowView) {
                    if (selZone == null) {
                        m = i18n.getString("ZoneInfo.pleaseSelectGeozone","Please select a Geozone");
                        error = true;
                        listZones = true;
                    } else {
                        viewZone = true;
                    }
                }
            } else {
                listZones = true;
            }
        } else {
            listZones = true;
        }

        /* delete Geozone? */
        if (deleteZone) {
            if (selZone == null) {
                m = i18n.getString("ZoneInfo.pleaseSelectGeozone","Please select a Geozone");
                error = true;
            } else {
                try {
                    Geozone.Key zoneKey = (Geozone.Key)selZone.getRecordKey();
                    Print.logWarn("Deleting Geozone: " + zoneKey);
                    zoneKey.delete(true); // will also delete dependencies
                    selZoneID = "";
                    selZone = null;
                    zoneList = Geozone.getGeozoneIDsForAccount(currAcct.getAccountID());
                    if ((zoneList != null) && (zoneList.length > 0)) {
                        selZoneID = zoneList[0];
                        try {
                            selZone = !selZoneID.equals("")?Geozone.getGeozone(currAcct,selZoneID,DEFAULT_SORT_ID,false):null;
                        } catch (DBException dbe) {
                            // ignore
                        }
                    }
                } catch (DBException dbe) {
                    Print.logException("Deleting Geozone", dbe);
                    m = i18n.getString("ZoneInfo.errorDelete","Internal error deleting Geozone");
                    error = true;
                }
            }
            listZones = true;
        }

        /* new Geozone? */
        if (newZone) {
            boolean createZoneOK = true;
            //Print.logInfo("Creating new Geozone: %s", newZoneID);
            for (int u = 0; u < zoneList.length; u++) {
                if (newZoneID.equalsIgnoreCase(zoneList[u])) {
                    m = i18n.getString("ZoneInfo.alreadyExists","This Geozone already exists");
                    error = true;
                    createZoneOK = false;
                    break;
                }
            }
            if (createZoneOK) {
                try {
                    Geozone zone = Geozone.getGeozone(currAcct, newZoneID, DEFAULT_SORT_ID, true); // create
                    zone.setZoneType(newZoneType);
                    zone.setDefaultRadius(); // based on zone type
                    zone.save(); // needs to be saved to be created
                    zoneList = Geozone.getGeozoneIDsForAccount(currAcct.getAccountID());
                    selZone = zone;
                    selZoneID = selZone.getGeozoneID();
                    m = i18n.getString("ZoneInfo.createdZone","New Geozone has been created");
                } catch (DBException dbe) {
                    Print.logException("Error Creating Geozone", dbe);
                    m = i18n.getString("ZoneInfo.errorCreate","Internal error creating Geozone");
                    error = true;
                }
            }
            listZones = true;
        }

        /* change/update the Geozone info? */
        if (updateZone) {
            int     zonePriority   = StringTools.parseInt(   AttributeTools.getRequestString(request,PARM_PRIORITY,null),0);
            double  zoneSpeedLimit = StringTools.parseDouble(AttributeTools.getRequestString(request,PARM_SPEED_LIMIT,null),0.0);
            boolean zoneRevGeocode = !StringTools.isBlank(   AttributeTools.getRequestString(request,PARM_REV_GEOCODE,null));
            boolean zoneArrNotify  = !StringTools.isBlank(   AttributeTools.getRequestString(request,PARM_ARRIVE_NOTIFY,null));
            boolean zoneDepNotify  = !StringTools.isBlank(   AttributeTools.getRequestString(request,PARM_DEPART_NOTIFY,null));
            boolean zoneAutoNotify = !StringTools.isBlank(   AttributeTools.getRequestString(request,PARM_AUTO_NOTIFY,null));
            boolean zoneClientUpld = !StringTools.isBlank(   AttributeTools.getRequestString(request,PARM_CLIENT_UPLOAD,null));
            int     zoneClientID   = StringTools.parseInt(   AttributeTools.getRequestString(request,PARM_CLIENT_ID,null),0);
            long    zoneRadius     = StringTools.parseLong(  AttributeTools.getRequestString(request,PARM_ZONE_RADIUS,null),100L);
            String  zoneColor      = AttributeTools.getRequestString(request,PARM_ZONE_COLOR,null);
            String  zoneDesc       = AttributeTools.getRequestString(request,PARM_ZONE_DESC,"");
            //Print.logInfo("Updating Zone: %s - %s", selZoneID, zoneDesc);
            try {
                if (selZone != null) {
                    boolean saveOK = true;
                    // Overlap priority
                    if (showOverlapPriority) {
                        selZone.setPriority(zonePriority);
                    }
                    // Speed limit
                    if (showSpeedLimit) {
                        Account.SpeedUnits speedUnit = Account.getSpeedUnits(currAcct); // not null
                        double kph = speedUnit.convertToKPH(zoneSpeedLimit);
                        selZone.setSpeedLimitKPH(kph);
                    }
                    // ReverseGeocode
                    selZone.setReverseGeocode(zoneRevGeocode);
                    // Arrive/Depart notification
                    if (showArriveDepartZone) {
                        selZone.setArrivalZone(zoneArrNotify);
                        selZone.setDepartureZone(zoneDepNotify);
                        if (showAutoNotify) {
                            selZone.setAutoNotify(zoneAutoNotify);
                        }
                    }
                    // Client upload zone
                    if (showClientUploadZone != 0) {
                        if (zoneClientID > 0) {
                            selZone.setClientUpload(true);
                            selZone.setClientID(zoneClientID);
                        } else
                        if (zoneClientUpld) {
                            selZone.setClientUpload(true);
                            selZone.setClientID(1);
                        } else {
                            selZone.setClientUpload(false);
                            selZone.setClientID(0);
                        }
                    }
                    // Radius (meters)
                    if (zoneRadius > 0L) {
                        selZone.setRadius((int)zoneRadius);
                    }
                    // description
                    if (!StringTools.isBlank(zoneColor)) {
                        selZone.setShapeColor(zoneColor);
                    }
                    // GeoPoints
                    selZone.clearGeoPoints();
                  //int pointCount = (mapProvider != null)? mapProvider.getGeozoneSupportedPointCount(selZone.getZoneType()) : 0;
                    int pointCount = ZoneInfo.getGeozoneSupportedPointCount(reqState, selZone.getZoneType());
                    for (int z = 0, p = 0; z < pointCount; z++) {
                        double zoneLat = StringTools.parseDouble(AttributeTools.getRequestString(request,PARM_ZONE_LATITUDE (z),null),0.0);
                        double zoneLon = StringTools.parseDouble(AttributeTools.getRequestString(request,PARM_ZONE_LONGITUDE(z),null),0.0);
                        if (GeoPoint.isValid(zoneLat,zoneLon)) {
                            selZone.setGeoPoint(p++, zoneLat, zoneLon);
                        }
                    }
                    // description
                    if (!StringTools.isBlank(zoneDesc)) {
                        selZone.setDescription(zoneDesc);
                    }
                    // save
                    if (saveOK) {
                        selZone.save();
                        m = i18n.getString("ZoneInfo.zoneUpdated","Geozone information updated");
                    } else {
                        // error occurred, should stay on this page
                        editZone = true;
                    }
                } else {
                    m = i18n.getString("ZoneInfo.noZones","There are currently no defined Geozones for this Account.");
                }
            } catch (Throwable t) {
                Print.logException("Updating Geozone", t);
                m = i18n.getString("ZoneInfo.errorUpdate","Internal error updating Geozone");
                error = true;
            }
            listZones = true;
        }

        /* final vars */
        final String      _selZoneID   = selZoneID;
        final Geozone     _selZone     = selZone;
        final String      _zoneList[]  = zoneList;
        final boolean     _allowEdit   = allowEdit;
        final boolean     _allowView   = allowView;
        final boolean     _allowNew    = allowNew;
        final boolean     _allowDelete = allowDelete;
        final boolean     _editZone    = _allowEdit && editZone;
        final boolean     _viewZone    = _editZone || viewZone;
        final boolean     _listZones   = listZones || (!_editZone && !_viewZone);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = ZoneInfo.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "ZoneInfo.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
                if (!_listZones && mapSupportsGeozones) {

                    // MapProvider JavaScript
                    if (mapProvider != null) {
                        mapProvider.writeJavaScript(out, reqState);
                    }

                    /* start JavaScript */
                    JavaScriptTools.writeStartJavaScript(out);

                    // Geozone Javascript
                    double radiusMeters = DEFAULT_ZONE_RADIUS;
                    int zoneTypeInt = Geozone.GeozoneType.POINT_RADIUS.getIntValue(); // default
                    String zoneColor = "";
                    if (_selZone != null) {
                        zoneTypeInt  = _selZone.getZoneType();
                        zoneColor    = _selZone.getShapeColor();
                        radiusMeters = _selZone.getRadiusMeters(MIN_RADIUS_METERS,MAX_RADIUS_METERS);
                    }
                    MapDimension mapDim = (mapProvider != null)? mapProvider.getZoneDimension() : new MapDimension(0,0);
                    out.println("// Geozone vars");
                    out.println("jsvGeozoneMode = true;");
                    out.println("MAP_WIDTH  = " + mapDim.getWidth()  + ";");
                    out.println("MAP_HEIGHT = " + mapDim.getHeight() + ";");

                    JavaScriptTools.writeJSVar(out, "DEFAULT_ZONE_RADIUS", DEFAULT_ZONE_RADIUS);
                    JavaScriptTools.writeJSVar(out, "jsvZoneEditable"    , _editZone);
                    JavaScriptTools.writeJSVar(out, "jsvShowVertices"    , true);
                    JavaScriptTools.writeJSVar(out, "jsvZoneType"        , zoneTypeInt);
                    JavaScriptTools.writeJSVar(out, "jsvZoneRadiusMeters", radiusMeters);
                    JavaScriptTools.writeJSVar(out, "jsvZoneColor"       , zoneColor);

                  //int pointCount = (mapProvider != null)? mapProvider.getGeozoneSupportedPointCount(zoneTypeInt) : 0;
                    int pointCount = ZoneInfo.getGeozoneSupportedPointCount(reqState, zoneTypeInt);
                    out.write("// Geozone points\n");
                    JavaScriptTools.writeJSVar(out, "jsvZoneCount"       , pointCount);
                    JavaScriptTools.writeJSVar(out, "jsvZoneIndex"       , DEFAULT_POINT_INDEX);
                    out.write("var jsvZoneList = new Array(\n"); // consistent with JSMapPoint
                    for (int z = 0; z < pointCount; z++) {
                        GeoPoint gp = (_selZone != null)? _selZone.getGeoPoint(z) : null;
                        if (gp == null) { gp = GeoPoint.INVALID_GEOPOINT; }
                        out.write("    { lat:" + gp.getLatitude() + ", lon:" + gp.getLongitude() + " }");
                        if ((z+1) < pointCount) { out.write(","); }
                        out.write("\n");
                    }
                    out.write("    );\n");

                    /* end JavaScript */
                    JavaScriptTools.writeEndJavaScript(out);

                    /* Geozone.js */
                    JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("Geozone.js"), request);

                }
            }
        };

        /* Content */
        final boolean mapControlsOnLeft = 
            ListTools.containsIgnoreCase(CONTROLS_ON_LEFT,privLabel.getStringProperty(PrivateLabel.PROP_ZoneInfo_mapControlLocation,""));
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = ZoneInfo.this.getPageName();

                // frame header
              //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = ZoneInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String selectURL  = ZoneInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String newURL     = ZoneInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());

                if (_listZones) {
                    
                    // Geozone selection table (Select, Geozone ID, Zone Name)
                    String frameTitle = _allowEdit? 
                        i18n.getString("ZoneInfo.list.viewEditZone","View/Edit Geozone Information") : 
                        i18n.getString("ZoneInfo.list.viewZone","View Geozone Information");
                    out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                    out.write("<hr>\n");

                    // Geozone selection table (Select, Zone ID, Zone Name)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+FilterText(i18n.getString("ZoneInfo.list.selectZone","Select a Geozone"))+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_ZONE_SELECT+"' method='post' action='"+selectURL+"' target='_self'>"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"'>"+FilterText(i18n.getString("ZoneInfo.list.select","Select"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.zoneID","Geozone ID"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.description","Description\n(Address)"))+"</th>\n");
                    if (showOverlapPriority) {
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.overlapPriority","Overlap\nPriority"))+"</th>\n");
                    }
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.zoneType","Zone\nType"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.revGeocode","Reverse\nGeocode"))+"</th>\n");
                    if (showArriveDepartZone) {
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.arriveZone","Arrival\nZone"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.departZone","Departure\nZone"))+"</th>\n");
                    }
                    if (showClientUploadZone == 1) {
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.clientUpload","Client\nUpload"))+"</th>\n");
                    } else
                    if (showClientUploadZone == 2) {
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.clientUploadID","Client\nUpload ID"))+"</th>\n");
                    }
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.radiusMeters","Radius\n(meters)"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"'>"+FilterText(i18n.getString("ZoneInfo.list.centerPoint","Center\nLatitude/Longitude"))+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    
                    /* geozone list */
                    out.write(" <tbody>\n");
                    int pointRadiusType = Geozone.GeozoneType.POINT_RADIUS.getIntValue();
                    int polygonType     = Geozone.GeozoneType.POLYGON.getIntValue();
                    int corridorType    = Geozone.GeozoneType.SWEPT_POINT_RADIUS.getIntValue();
                    for (int z = 0, r = 0; z < _zoneList.length; z++) {
                        
                        /* get Geozone */
                        Geozone zone = null;
                        try {
                            zone = Geozone.getGeozone(currAcct, _zoneList[z], DEFAULT_SORT_ID, false);
                        } catch (DBException dbe) {
                            // error
                        }
                        if (zone == null) {
                            continue; // skip 
                        }

                        /* geozone vars */
                        int    zoneTypeInt = zone.getZoneType();
                        String zoneID      = FilterText(zone.getGeozoneID());
                        String zoneDesc    = FilterText(zone.getDescription());
                        String zoneTypeStr = FilterText(zone.getZoneTypeDescription(locale));
                        String zoneRevGeo  = FilterText(ComboOption.getYesNoText(locale,zone.getReverseGeocode()));
                        String zoneRadius  = zone.hasRadius()? String.valueOf(zone.getRadius()) : "--";
                        GeoPoint centerPt  = zone.getGeoPoint(DEFAULT_POINT_INDEX); // may be null if invalid
                        if (centerPt == null) { centerPt = new GeoPoint(0.0, 0.0); }
                        String zoneCenter  = centerPt.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null) + " "+GeoPoint.PointSeparator+" " + centerPt.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null);
                        String checked     = _selZoneID.equals(zone.getGeozoneID())? "checked" : "";
                        String styleClass  = ((r++ & 1) == 0)? CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD : CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN;
                        
                      //int pointCount     = (mapProvider != null)? mapProvider.getGeozoneSupportedPointCount(zoneTypeInt) : 0;
                        int pointCount     = ZoneInfo.getGeozoneSupportedPointCount(reqState, zoneTypeInt);
                        String typeColor   = (pointCount > 0)? "black" : "red";

                        out.write("  <tr class='" + styleClass + "'>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+z+"'>");
                        if (pointCount <= 0) {
                            out.write("&nbsp;"); // not supported
                        } else
                        if ((zoneTypeInt == pointRadiusType) || (zoneTypeInt == polygonType) || (zoneTypeInt == corridorType)) {
                            out.write("<input type='radio' name='"+PARM_ZONE_SELECT+"' id='"+zoneID+"' value='"+zoneID+"' "+checked+">");
                        } else {
                            out.write("&nbsp;"); // unrecognized type
                        }
                        out.write(      "</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+zoneID+"'>"+zoneID+"</label></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneDesc+"</td>\n");
                        if (showOverlapPriority) {
                            String zonePriority = FilterText(String.valueOf(zone.getPriority()));
                            out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zonePriority+"</td>\n");
                        }
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap style='color:"+typeColor+"'>"+zoneTypeStr+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneRevGeo+"</td>\n");
                        if (showArriveDepartZone) {
                            String zoneArrNtfy  = FilterText(ComboOption.getYesNoText(locale,zone.getArrivalZone()));
                            String zoneDepNtfy  = FilterText(ComboOption.getYesNoText(locale,zone.getDepartureZone()));
                            out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneArrNtfy+"</td>\n");
                            out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneDepNtfy+"</td>\n");
                        }
                        if (showClientUploadZone == 1) {
                            String zoneUpload   = FilterText(ComboOption.getYesNoText(locale,zone.getClientUpload()||(zone.getClientID() > 0)));
                            out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneUpload+"</td>\n");
                        } else
                        if (showClientUploadZone == 2) {
                            String zoneUpldID   = (zone.getClientID() > 0)? String.valueOf(zone.getClientID()) : "--";
                            out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneUpldID+"</td>\n");
                        }
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneRadius+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+zoneCenter+"</td>\n");
                        out.write("  </tr>\n");

                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("ZoneInfo.list.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("ZoneInfo.list.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) { 
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("ZoneInfo.list.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");

                    /* new Geozone */
                    if (_allowNew) {
                        out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+FilterText(i18n.getString("ZoneInfo.list.createNewZone","Create a new Geozone"))+":</h1>\n");
                        out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                        out.write("<form name='"+FORM_ZONE_NEW+"' method='post' action='"+newURL+"' target='_self'>"); // target='_top'
                        out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                        out.write(FilterText(i18n.getString("ZoneInfo.list.zoneID","Geozone ID"))+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_ID+"' value='' size='32' maxlength='32'>");
                      //int polyPointCount = (mapProvider != null)? mapProvider.getGeozoneSupportedPointCount(polygonType ) : 0;
                      //int corrPointCount = (mapProvider != null)? mapProvider.getGeozoneSupportedPointCount(corridorType) : 0;
                        int polyPointCount = ZoneInfo.getGeozoneSupportedPointCount(reqState,polygonType );
                        int corrPointCount = ZoneInfo.getGeozoneSupportedPointCount(reqState,corridorType);
                        if ((polyPointCount > 0) || (corrPointCount > 0)) {
                            ComboMap zoneTypeList = new ComboMap();
                            out.write("&nbsp;");
                            zoneTypeList.add(String.valueOf(pointRadiusType) , Geozone.GeozoneType.POINT_RADIUS.toString(locale));
                            if (polyPointCount > 0) {
                                zoneTypeList.add(String.valueOf(polygonType) , Geozone.GeozoneType.POLYGON.toString(locale));
                            }
                            if (corrPointCount > 0) {
                                zoneTypeList.add(String.valueOf(corridorType), Geozone.GeozoneType.SWEPT_POINT_RADIUS.toString(locale));
                            }
                            out.print(Form_ComboBox(PARM_NEW_TYPE,PARM_NEW_TYPE,true,zoneTypeList,"","", -1));
                        } else {
                            // only POINT_RADIUS supported
                        }
                        out.write("<br>\n");
                        out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("ZoneInfo.list.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                        out.write("</form>\n");
                        out.write("</div>\n");
                        out.write("<hr>\n");
                    }

                } else {
                    // view/edit
                    int selZoneType = (_selZone != null)? _selZone.getZoneType() : Geozone.GeozoneType.POINT_RADIUS.getIntValue();
                    
                    // begin form
                    out.println("<form name='"+FORM_ZONE_EDIT+"' method='post' action='"+editURL+"' target='_self'>"); // target='_top'
                    
                    // Geozone view/edit form
                    out.write("<table cellspacing='0' cellpadding='0' border='0'><tr>\n");
                    out.write("<td nowrap>");
                    String frameTitle = _editZone? 
                        i18n.getString("ZoneInfo.map.editZone","Edit Geozone") : 
                        i18n.getString("ZoneInfo.map.viewZone","View Geozone");
                    out.print  ("<span style='font-size:9pt; font-weight:bold;'>"+frameTitle+" &nbsp;</span>");
                    out.print  (Form_TextField(PARM_ZONE_SELECT, false, _selZoneID, 16, 20));
                    out.write("</td>");
                    out.write("<td nowrap style=\"width:100%; text-align:right;\">");
                    //out.println("<span style='width:100%;'>&nbsp;</span>");  <-- causes IE to NOT display the following description
                    String i18nAddressTooltip = i18n.getString("ZoneInfo.map.description.tooltip", "This description is used for custom reverse-geocoding");
                    out.print  ("<span class='zoneDescription' style='width:100%;' title=\""+i18nAddressTooltip+"\">");
                    out.print  ("<b>"+i18n.getString("ZoneInfo.map.description","Description (Address)")+"</b>:&nbsp;");
                    out.print  (Form_TextField(PARM_ZONE_DESC, _editZone, (_selZone!=null)?_selZone.getDescription():"", 30, 64));
                    out.println("</span>");
                    out.write("</td>");
                    out.write("</tr></table>");

                    //out.println("<br/>");
                    out.println("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>");

                    out.println("<table border='0' cellpadding='0' cellspacing='0' style='padding-top:3px'>"); // {
                    out.println("<tr>");

                    /* map (controls on right) */
                    MapDimension mapDim = (mapProvider != null)? mapProvider.getZoneDimension() : new MapDimension(0,0);
                    if (!mapControlsOnLeft) {
                        if (mapSupportsGeozones) {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-right:5px;'>");
                            out.println("<!-- Begin Map -->");
                            mapProvider.writeMapCell(out, reqState, mapDim);
                            out.println("<!-- End Map -->");
                            out.println("</td>");
                        } else {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-right:5px; border: 1px solid black;'>");
                            out.println("<!-- Geozones not yet supported for this MapProvider -->");
                            out.println("<center>");
                            out.println("<span style='font-size:12pt;'>");
                            out.println(i18n.getString("ZoneInfo.map.notSupported","Geozone map not yet supported for this MapProvider"));
                            out.println("&nbsp;</span>");
                            out.println("</center>");
                            out.println("</td>");
                        }
                    }

                    /* Geozone fields */
                    out.println("<td valign='top' style='border-top: solid #CCCCCC 1px;'>");

                    // overlap priority
                    if (showOverlapPriority) {
                        String i18nPriorityTooltip = i18n.getString("ZoneInfo.map.overlapPriority.tooltip", "Priority used when multiple Geozones overlap");
                        out.println("<div class='zonePrioritySelect' title=\""+i18nPriorityTooltip+"\">");
                        int pri = (_selZone != null)? _selZone.getPriority() : 0;
                        if (pri < 0) {
                            pri = 0;
                        } else
                        if (pri >= OVERLAP_PRIORITY.length) {
                            pri = OVERLAP_PRIORITY.length - 1;
                        }
                        ComboMap priCombo = new ComboMap(OVERLAP_PRIORITY);
                        String priSel = OVERLAP_PRIORITY[pri];
                        out.println("<b><label for='"+PARM_PRIORITY+"'>"+i18n.getString("ZoneInfo.map.overlapPriority","Overlap Priority")+": </label></b>");
                        out.println(Form_ComboBox(PARM_PRIORITY, PARM_PRIORITY, _editZone, priCombo, priSel, null, 6));
                        out.println("</div>");
                    }
                    
                    // reverse-geocode zone
                    String i18nRevGeoTooltip = i18n.getString("ZoneInfo.map.reverseGeocode.tooltip", "Select to use this zone for custom reverse-geocoding");
                    out.println("<div class='zoneCheckSelect' title=\""+i18nRevGeoTooltip+"\">");
                    out.println(Form_CheckBox(PARM_REV_GEOCODE, PARM_REV_GEOCODE, _editZone, ((_selZone!=null) && _selZone.getReverseGeocode()),null,null));
                    out.println("<b><label for='"+PARM_REV_GEOCODE+"'>"+i18n.getString("ZoneInfo.map.reverseGeocode","Reverse Geocode")+"</label></b>");
                    out.println("</div>");

                    // arrival zone
                    if (showArriveDepartZone) {
                        String i18nArriveTooltip = i18n.getString("ZoneInfo.map.arrivalZone.tooltip", "Select to use this zone for 'Arrival' checking");
                        out.println("<div class='zoneCheckSelect' title=\""+i18nArriveTooltip+"\">");
                        out.println(Form_CheckBox(PARM_ARRIVE_NOTIFY, PARM_ARRIVE_NOTIFY, _editZone, ((_selZone!=null) && _selZone.getArrivalZone()),null,null));
                        out.println("<b><label for='"+PARM_ARRIVE_NOTIFY+"'>"+i18n.getString("ZoneInfo.map.arrivalZone","Arrival Zone")+"</label></b>");
                        out.println("</div>");
                    }

                    // departure zone
                    if (showArriveDepartZone) {
                        String i18nDepartTooltip = i18n.getString("ZoneInfo.map.departureZone.tooltip", "Select to use this zone for 'Departure' checking");
                        out.println("<div class='zoneCheckSelect' title=\""+i18nDepartTooltip+"\">");
                        out.println(Form_CheckBox(PARM_DEPART_NOTIFY, PARM_DEPART_NOTIFY, _editZone, ((_selZone!=null) && _selZone.getDepartureZone()),null,null));
                        out.println("<b><label for='"+PARM_DEPART_NOTIFY+"'>"+i18n.getString("ZoneInfo.map.departureZone","Departure Zone")+"</label></b>");
                        out.println("</div>");
                    }

                    // auto notify
                    if (showArriveDepartZone && showAutoNotify) {
                        String i18nAutoTooltip = i18n.getString("ZoneInfo.map.autoNotify.tooltip", "Select to automatically send notification on arrive/depart");
                        out.println("<div class='zoneCheckSelect' title=\""+i18nAutoTooltip+"\">");
                        out.println(Form_CheckBox(PARM_AUTO_NOTIFY, PARM_AUTO_NOTIFY, _editZone, ((_selZone!=null) && _selZone.getAutoNotify()),null,null));
                        out.println("<b><label for='"+PARM_AUTO_NOTIFY+"'>"+i18n.getString("ZoneInfo.map.autoNotify","Auto Notify")+"</label></b>");
                        out.println("</div>");
                    }

                    // Client Upload ID
                    if (showClientUploadZone != 0) {
                        String i18nUploadTooltip = i18n.getString("ZoneInfo.map.clientUpload.tooltip", "Select to use for client-side geofence");
                        out.println("<div class='zoneCheckSelect' title=\""+i18nUploadTooltip+"\">");
                        if (showClientUploadZone == 1) {
                            out.println(Form_CheckBox(PARM_CLIENT_UPLOAD, PARM_CLIENT_UPLOAD, _editZone, ((_selZone!=null) && _selZone.getClientUpload()),null,null));
                            out.println("<b><label for='"+PARM_CLIENT_UPLOAD+"'>"+i18n.getString("ZoneInfo.map.clientUpload","Client Upload")+":</label></b>&nbsp;");
                        } else
                        if (showClientUploadZone == 2) {
                            out.println("<b>"+i18n.getString("ZoneInfo.map.clientUploadID","Client Upload ID")+":</b>&nbsp;");
                            out.println(Form_TextField(PARM_CLIENT_ID, PARM_CLIENT_ID, _editZone, (_selZone!=null)?String.valueOf(_selZone.getClientID()):"", 5, 5));
                        }
                        out.println("</div>");
                    }
                    
                    // geozone points section
                    out.println("<hr>");
                    
                    /* notes */
                    if (_editZone && mapSupportsGeozones) {
                        out.println("<div class='zoneNotesBasic'>");
                        //out.println("<i>"+i18nx.getString("ZoneInfo.map.notes.basic", "The Geozone loc/size may be changed here, click 'RESET' to update.")+"</i>");
                        out.println("<i>"+i18n.getString("ZoneInfo.map.notes.basic", "Geozone attributes:")+"</i>");
                        out.println("</div>");
                    }

                    /* shape color */
                    if (privLabel.getBooleanProperty(PrivateLabel.PROP_ZoneInfo_showShapeColor,false)) {
                        ComboMap colorCombo = GetColorComboMap(i18n);
                        String color = (_selZone != null)? _selZone.getShapeColor() : "";
                        String onchange = _editZone? "javascript:jsvZoneColor=document."+FORM_ZONE_EDIT+"."+PARM_ZONE_COLOR+".value;_zoneReset();" : null;
                        out.println("<div class='zoneColorSelect' title=\""+""+"\">");
                        out.println("<b><label for='"+PARM_ZONE_COLOR+"'>"+i18n.getString("ZoneInfo.map.shapeColor","Zone Color")+": </label></b>");
                        out.println(Form_ComboBox(PARM_ZONE_COLOR, PARM_ZONE_COLOR, _editZone, colorCombo, color, onchange, 10));
                        out.println("</div>");
                    }

                    /* radius */
                    Geozone.GeozoneType gzt = Geozone.getGeozoneType(_selZone);
                    if (gzt.hasRadius()) {
                        String i18nRadiusTooltip = i18n.getString("ZoneInfo.map.radius.tooltip", "Radius may be between {0} and {1} meters",
                            String.valueOf((long)MIN_RADIUS_METERS), String.valueOf((long)MAX_RADIUS_METERS));
                        out.println("<div class='zoneRadius' title=\""+i18nRadiusTooltip+"\">");
                        out.print  ("<b>"+i18n.getString("ZoneInfo.map.radiusMeters","Radius (meters)")+":</b>&nbsp;");
                        out.println(Form_TextField(MapProvider.ID_ZONE_RADIUS_M, PARM_ZONE_RADIUS, _editZone, (_selZone!=null)?String.valueOf(_selZone.getRadius()):"", 7, 7));
                        out.println("</div>");
                    } else {
                        out.println("<input type='hidden' id='"+MapProvider.ID_ZONE_RADIUS_M+"' name='"+PARM_ZONE_RADIUS+"' value='0'/>");
                    }

                    out.println("<div class='zoneLatLon'>");
                    out.println("<b>"+i18n.getString("ZoneInfo.map.latLon","Lat/Lon")+"</b>:&nbsp;&nbsp;");
                    if (_editZone && mapSupportsGeozones) {
                        String i18nResetBtn = i18n.getString("ZoneInfo.map.reset","Reset Map");
                        String i18nResetTooltip = i18n.getString("ZoneInfo.map.reset.tooltip", "Click to update the map with the specified radius/latitude/longitude");
                        out.print("<input class='formButton' type='button' name='reset' value='"+i18nResetBtn+"' title=\""+i18nResetTooltip+"\" onclick=\"javascript:_zoneReset();\">");
                    }
                    out.println("<br>");
                  //int pointCount = (mapProvider != null)? mapProvider.getGeozoneSupportedPointCount(selZoneType) : 0;
                    int pointCount = ZoneInfo.getGeozoneSupportedPointCount(reqState, selZoneType);
                    for (int z = 0; z < pointCount; z++) {
                        String latStr = (_selZone != null)? String.valueOf(_selZone.getLatitude(z) ) : "";
                        String lonStr = (_selZone != null)? String.valueOf(_selZone.getLongitude(z)) : "";
                        // id='"+PARM_ZONE_INDEX+"'
                        if (pointCount > 1) {
                            String chk = (z == 0)? " checked" : "";
                            out.println("<input type='radio'  name='"+PARM_ZONE_INDEX+"' value='" + z + "' "+chk+" onclick=\"javascript:_zonePointSelectionChanged("+z+")\"/>&nbsp;");
                            //                                                                                     onchange=
                        } else {
                            out.println("<input type='hidden' name='"+PARM_ZONE_INDEX+"' value='" + z + "'/>");
                        }
                        String latCSS = _editZone? "zoneLatLonText" : "zoneLatLonText_ro";
                        String lonCSS = _editZone? "zoneLatLonText" : "zoneLatLonText_ro";
                        out.println(Form_TextField(MapProviderAdapter.ID_ZONE_LATITUDE (z), PARM_ZONE_LATITUDE (z), _editZone, latStr, null,  8,  9, latCSS));
                        out.println(Form_TextField(MapProviderAdapter.ID_ZONE_LONGITUDE(z), PARM_ZONE_LONGITUDE(z), _editZone, lonStr, null,  9, 10, lonCSS));
                        if ((z+1) < pointCount) { out.println("<br>"); }
                    }
                    // Speed limit
                    if (mapSupportsGeozones && showSpeedLimit) {
                        Account.SpeedUnits speedUnit = Account.getSpeedUnits(currAcct); // not null
                        double kphLimit = (_selZone!=null)? _selZone.getSpeedLimitKPH() : 0.0;
                        double speedLimit = speedUnit.convertFromKPH(kphLimit);
                        out.print("<hr>\n");
                        out.println("<div class='zoneSpeedLimit' title=\""+""+"\">");
                        out.println("<b>"+i18n.getString("ZoneInfo.speedLimit","Speed Limit ({0})",speedUnit.toString(locale))+":</b>");
                        out.println(Form_TextField(PARM_SPEED_LIMIT, PARM_SPEED_LIMIT, _editZone, String.valueOf(speedLimit), 5, 5));
                        out.println("</div>");
                    }
                    // "Center On Zip/Address"
                    if (_editZone && mapSupportsGeozones) {
                        // "ZipCode" button
                        if (privLabel.getBooleanProperty(PrivateLabel.PROP_ZoneInfo_enableGeocode,false)) {
                            GeocodeProvider gcp = privLabel.getGeocodeProvider();
                            String i18nZipBtn = "";
                            if ((gcp == null) || gcp.getName().startsWith("geonames")) {
                                i18nZipBtn = i18n.getString("ZoneInfo.map.geocodeZip","Center On City/ZipCode");
                            } else {
                                i18nZipBtn = i18n.getString("ZoneInfo.map.geocodeAddress","Center On Address", gcp.getName());
                            }
                            String i18nZipTooltip = i18n.getString("ZoneInfo.map.geocode.tooltip", "Click to reset Geozone to spcified Address/ZipCode");
                            String rgZipCode_text = "rgZipCode";
                            out.print("<hr>\n");
                          //out.print("<br>");
                            out.print("<input class='formButton' type='button' name='tozip' value='"+i18nZipBtn+"' title=\""+i18nZipTooltip+"\" onclick=\"javascript:_zoneGotoAddr(jsmGetIDValue('"+rgZipCode_text+"'),'US');\">");
                            out.print("<br>");
                            out.println(Form_TextField(rgZipCode_text, rgZipCode_text, _editZone, "",  27, 60));
                        }
                    }
                    out.println("</div>");

                    out.println("<hr>");
                    out.println("<div class='zoneInstructions'>");
                    out.println("<b>"+i18n.getString("ZoneInfo.map.notes.header","Geozone Notes/Instructions")+":</b><br>");
                    if (_editZone && mapSupportsGeozones) {
                        String instr[] = mapProvider.getGeozoneInstructions(selZoneType, locale);
                        if ((instr != null) && (instr.length > 0)) {
                            for (int i = 0; i < instr.length; i++) {
                                if (!StringTools.isBlank(instr[i])) { 
                                    out.println("- " + FilterText(instr[i]) + "<br>"); 
                                }
                            }
                        }
                    }
                    out.println("- " + i18n.getString("ZoneInfo.map.notes.lengthInMeters", "Distances are always in meters.") + "<br>");

                    out.println("<hr>");
                    if (mapSupportsCursorLocation || mapSupportsDistanceRuler) {
                        if (mapSupportsCursorLocation) {
                            out.println("<b>"+i18n.getString("ZoneInfo.map.cursorLoc","Cursor")+"</b>:");
                            out.println("<span id='"+MapProvider.ID_LAT_LON_DISPLAY +"' style='margin-left:6px; margin-bottom:3px;'>0.00000, 0.00000</span>");
                        }
                        if (mapSupportsDistanceRuler) {
                            out.println("<b>"+i18n.getString("ZoneInfo.map.distanceRuler","Distance")+"</b>:");
                            out.println("<span id='"+MapProvider.ID_DISTANCE_DISPLAY+"' style='margin-left:6px;'>0 "+GeoPoint.DistanceUnits.METERS.toString(locale)+"</span>");
                        }
                        out.println("<hr>");
                    }

                    out.println("</div>");

                    out.write("<div width='100%'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editZone) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("ZoneInfo.map.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("ZoneInfo.map.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("ZoneInfo.map.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    }
                    out.write("</div>\n");

                    out.println("<div width='100%' height='100%'>");
                    out.println("&nbsp;");
                    out.println("</div>");

                    out.println("</td>");

                    /* map (controls on left) */
                    if (mapControlsOnLeft) {
                        if (mapSupportsGeozones) {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-left:5px;'>");
                            out.println("<!-- Begin Map -->");
                            mapProvider.writeMapCell(out, reqState, mapDim);
                            out.println("<!-- End Map -->");
                            out.println("</td>");
                        } else {
                            out.println("<td style='width:"+mapDim.getWidth()+"px; height:"+mapDim.getHeight()+"px; padding-left:5px; border: 1px solid black;'>");
                            out.println("<!-- Geozones not yet supported for this MapProvider -->");
                            out.println("<center>");
                            out.println("<span style='font-size:12pt;'>");
                            out.println(i18n.getString("ZoneInfo.map.notSupported","Geozone map not yet supported for this MapProvider"));
                            out.println("&nbsp;</span>");
                            out.println("</center>");
                            out.println("</td>");
                        }
                    }

                    /* end of form */
                    out.println("</tr>");
                    out.println("</table>"); // }
                    out.println("</form>");

                }

            }
        };

        /* map load? */
        String mapOnLoad   = _listZones? "" : "javascript:_zoneMapOnLoad();";
        String mapOnUnload = _listZones? "" : "javascript:_zoneMapOnUnload();";

        /* write frame */
        String onload = error? (mapOnLoad + JS_alert(false,m)) : mapOnLoad;
        CommonServlet.writePageFrame(
            reqState,
            onload,mapOnUnload,         // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }
    
    // ------------------------------------------------------------------------
}
