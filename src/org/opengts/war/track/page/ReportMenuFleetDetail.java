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
//  2010/06/17  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.TimeZone;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.track.*;

public class ReportMenuFleetDetail
    extends ReportMenu
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Reports: "fleet.summary"
    //  - Last known Device location
    //  - Last known Entity location

    public ReportMenuFleetDetail()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_MENU_RPT_GRPDETAIL);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
        this.setReportType(ReportFactory.REPORT_TYPE_FLEET_DETAIL);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_REPORTS_GRPDETAIL;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(ReportMenuFleetDetail.class);
        String       devTitles[] = reqState.getDeviceTitles();
        String       grpTitles[] = reqState.getDeviceGroupTitles();
        return super._getMenuDescription(reqState,i18n.getString("ReportMenuFleetDetail.menuDesc","{0} Detail Reports", grpTitles));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(ReportMenuFleetDetail.class);
        String       devTitles[] = reqState.getDeviceTitles();
        String       grpTitles[] = reqState.getDeviceGroupTitles();
        return super._getMenuHelp(reqState,i18n.getString("ReportMenuFleetDetail.menuHelp","Display various {0} detail reports", grpTitles));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(ReportMenuFleetDetail.class);
        String       devTitles[] = reqState.getDeviceTitles();
        String       grpTitles[] = reqState.getDeviceGroupTitles();
        return super._getNavigationDescription(reqState,i18n.getString("ReportMenuFleetDetail.navDesc","{0} Detail", grpTitles));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(ReportMenuFleetDetail.class);
        String       devTitles[] = reqState.getDeviceTitles();
        String       grpTitles[] = reqState.getDeviceGroupTitles();
        return super._getNavigationTab(reqState,i18n.getString("ReportMenuFleetDetail.navTab","{0} Detail", grpTitles));
    }

    // ------------------------------------------------------------------------

}
