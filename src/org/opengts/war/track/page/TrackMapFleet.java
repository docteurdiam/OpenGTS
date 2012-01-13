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
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.TimeZone;
import java.util.Iterator;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class TrackMapFleet
    extends TrackMap
{
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // WebPage interface
    
    public TrackMapFleet()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_MAP_FLEET);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
        this.setFleet(true);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_TRACK_FLEET;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapFleet.class);
        String grpTitles[]     = reqState.getDeviceGroupTitles();
        return super._getMenuDescription(reqState,i18n.getString("TrackMapFleet.menuDesc","Track {0} locations on a map", grpTitles));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapFleet.class);
        String grpTitles[]     = reqState.getDeviceGroupTitles();
        return super._getMenuHelp(reqState,i18n.getString("TrackMapFleet.menuHelp","Select and Track the location of a {0} on a map", grpTitles));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapFleet.class);
        String grpTitles[]     = reqState.getDeviceGroupTitles();
        return super._getNavigationDescription(reqState,i18n.getString("TrackMapFleet.navDesc","{0}", grpTitles));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N   i18n            = privLabel.getI18N(TrackMapFleet.class);
        String grpTitles[]     = reqState.getDeviceGroupTitles();
        return super._getNavigationTab(reqState,i18n.getString("TrackMapFleet.navTab","{0} Map", grpTitles));
    }

    // ------------------------------------------------------------------------

}
