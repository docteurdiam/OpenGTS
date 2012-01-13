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
//  2007/03/30  Martin D. Flynn
//     -Added access control
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.PixelDimension;
import org.opengts.util.URIArg;
import org.opengts.util.RTProperties;

public interface WebPage
{

    // ------------------------------------------------------------------------
    // Sortable table constants (used by 'sorttable.js')

    public static final String SORTTABLE_SORTKEY            = "sorttable_customkey";
    public static final String SORTTABLE_CSS_CLASS          = "sortable";
    public static final String SORTTABLE_CSS_NOSORT         = "nosort"; // MDF modified, was "sorttable_nosort";

    // ------------------------------------------------------------------------

    public boolean getIsEnabled();
    
    // ------------------------------------------------------------------------

    public String getBaseURI();
    public URIArg getPageURI();

    // ------------------------------------------------------------------------

    public RTProperties getProperties();

    // ------------------------------------------------------------------------

    /* return the page name */
    public String getPageName();
    
    /* override the default page name */
    // the interface implementation may reject this override
    public void setPageName(String pageName);

    // ------------------------------------------------------------------------

    /* return the JSP path which should be used to display this page */
    public String getJspURI();

    // ------------------------------------------------------------------------

    /* return the page 'target=' */
    public String getTarget();
    
    /* get window pixel dimension (may return null) */
    public PixelDimension getWindowDimension();

    // ------------------------------------------------------------------------

    /* return the desired page navigation */
    public String getPageNavigationHTML(RequestProperties reqState);
    
    // ------------------------------------------------------------------------

    /* return true if a valid log is required for the display of this page */
    public boolean isLoginRequired();
    
    // ------------------------------------------------------------------------

    /* return true if the page indicates that it is ok to display (pending other restrictions) */
    public boolean isOkToDisplay(RequestProperties reqState);

    // ------------------------------------------------------------------------

    /* return the ACL name for this page */
    public String getAclName();
    public String getAclName(String subAcl);

    // ------------------------------------------------------------------------

    /* true if this page iis for the system admin only */
    public boolean systemAdminOnly();

    // ------------------------------------------------------------------------

    /* return the menu group (if any) */
    public MenuGroup getMenuGroup(RequestProperties reqState);

    /* return the menu name for menu navigation */
    public String getMenuName(RequestProperties reqState);
    
    /* return the menu icon/button image URI */
    public String getMenuIconImage();
    public String getMenuButtonImage();
    public String getMenuButtonAltImage();

    /* return the menu description for the specified menu */
    public String getMenuDescription(RequestProperties reqState, String parentMenuName);
    
    /* return the menu help for the specified menu */
    public String getMenuHelp(RequestProperties reqState, String parentMenuName);
 
    // ------------------------------------------------------------------------

    /* return the 'logged-in' navigation description */
    public String getNavigationDescription(RequestProperties reqState);

    /* return the 'logged-in' navigation description */
    public String getNavigationTab(RequestProperties reqState);

    // ------------------------------------------------------------------------

    /* encode/return the URL to the WebPage */
    public String encodePageURL(RequestProperties reqState);
    public String encodePageURL(RequestProperties reqState, String command);
    public String encodePageURL(RequestProperties reqState, String command, String cmdArg);

    // ------------------------------------------------------------------------

    /* write the contents for this page to the 'response' output */
    // connection state is available in 'reqState'
    public void writePage(RequestProperties reqState, String pageMsg)
        throws IOException;
    
    // ------------------------------------------------------------------------

}
