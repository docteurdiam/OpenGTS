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
//  2007/12/13  Martin D. Flynn
//     -Initial release
//  2008/05/20  Martin D. Flynn
//     -Addeded methods to allow hiding/showing this menu group in the menu bar
//      or top menu.
//  2008/12/01  Martin D. Flynn
//     -Added check for "SysAdmin" account
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;

import org.opengts.util.*;

import org.opengts.db.tables.Account;
import org.opengts.db.tables.User;

public class MenuGroup
{
    
    // ------------------------------------------------------------------------
    
    private String                  name        = null;
    private I18N.Text               title       = null;
    private I18N.Text               description = null;

    private java.util.List<WebPage> pageList    = null;

    private boolean                 showMenuBar = true;
    private boolean                 showTopMenu = true;

    public MenuGroup(String name, I18N.Text title, I18N.Text desc)
    {
        this.setName(name);
        this.setTitle(title);
        this.setDescription(desc);
        this.pageList = new Vector<WebPage>();
    }

    public MenuGroup(String name, I18N.Text title)
    {
        this(name, title, null);
    }

    public MenuGroup(String name)
    {
        this(name, null, null);
    }

    // ------------------------------------------------------------------------

    /* show this group as a tab in the menu bar */
    public void setShowInMenuBar(boolean show)
    {
        this.showMenuBar = show;
    }

    /* show this group as a tab in the menu bar */
    public boolean showInMenuBar()
    {
        return this.showMenuBar;
    }

    // ------------------------------------------------------------------------

    /* show this group as a tab in the menu bar */
    public void setShowInTopMenu(boolean show)
    {
        this.showTopMenu = show;
    }

    /* show this group as a tab in the menu bar */
    public boolean showInTopMenu()
    {
        return this.showTopMenu;
    }

    // ------------------------------------------------------------------------

    /* get the name of this menu group */
    public void setName(String name)
    {
        this.name = name;
    }
    
    /* set the name of this menu group */
    public String getName()
    {
        return this.name;
    }
    
    // ------------------------------------------------------------------------

    /* set the short title of this menu group */
    public void setTitle(I18N.Text title)
    {
        this.title = title;
    }
    
    /* get the short title of this menu group */
    public String getTitle(Locale loc)
    {
        return (this.title != null)? this.title.toString(loc) : "";
    }
    
    // ------------------------------------------------------------------------

    /* set the description of this menu group */
    public void setDescription(I18N.Text desc)
    {
        this.description = desc;
    }
    
    /* get the description of this menu group */
    public String getDescription(Locale loc)
    {
        String desc = (this.description != null)? this.description.toString(loc) : null;
        if ((desc != null) && !desc.equals("")) {
            return desc;
        } else {
            return this.getTitle(loc);
        }
    }

    // ------------------------------------------------------------------------

    /* add specified WebPage to this menu group */
    public void addWebPage(WebPage wp)
    {
        if (wp != null) {
            this.pageList.add(wp);
        }
    }
    
    /* get user accessible WebPages */
    public java.util.List<WebPage> getWebPageList(RequestProperties reqState)
    {
        if (reqState == null) {
            return this.pageList;
        } else {
            PrivateLabel privLabel = reqState.getPrivateLabel();
            Account account = reqState.getCurrentAccount();
            User    user    = reqState.getCurrentUser();
            java.util.List<WebPage> authPageList = new Vector<WebPage>();
            for (Iterator i = this.pageList.iterator(); i.hasNext();) {
                WebPage wp = (WebPage)i.next();
                if (wp.systemAdminOnly() && !Account.isSystemAdmin(account)) { 
                    continue; 
                } else
                if (!wp.isOkToDisplay(reqState)) {
                    continue; 
                } else
                if (privLabel.hasReadAccess(user,wp.getAclName())) {
                    authPageList.add(wp);
                }
            }
            return authPageList;
        }
    }
    
    // ------------------------------------------------------------------------

}
