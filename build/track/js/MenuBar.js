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
//  2008/02/21  Martin D. Flynn
//     -Moved from MenuBar.java
//  2008/02/27  Martin D. Flynn
//     -Added ability to specify the 'MenuBar.js' location in the 'webapp.conf' file.
//  2008/08/15  Martin D. Flynn
//     -Minor change to adjust position of submenu on Safari.
//  2008/12/01  Martin D. Flynn
//     -Menu now stays open as long as the mouse is still over the menu tab, or submenu.
// ----------------------------------------------------------------------------
// External dependencies:
//  - 'mnubarCreateSubMenu' must be defined externally.
//    'mnubarShowMenu' will call 'mnubarCreateSubMenu' to create the sub-menu with appropriate links.
// ----------------------------------------------------------------------------

var MenuLongTimeoutMS  = 1000;
var MenuShortTimeoutMS =  500;

var mbSubMenuObj       = null;
var mbSelectedMenu     = null;
var mbDestroyMenuTimer = null;

/* stop the 'destroy-menubar' timer */
function mnubarClearDestroyMenuTimer() 
{
    if (mbDestroyMenuTimer != null) {
        clearTimeout(mbDestroyMenuTimer);
        mbDestroyMenuTimer = null;
    }
}

/* start the 'destroy-menubar' timer */
function mnubarSetDestroyMenuTimer() 
{
    mnubarClearDestroyMenuTimer();
    mbDestroyMenuTimer = setTimeout('mnubarMenuTimeout()',MenuLongTimeoutMS);
}

/* destroy current submenu on timeout */
function mnubarMenuTimeout() 
{
    if (((mbSelectedMenu != null) && mbSelectedMenu.isMouseInside) || 
        ((mbSubMenuObj   != null) && mbSubMenuObj.isMouseInside  )   ) {
        mnubarClearDestroyMenuTimer();
        mbDestroyMenuTimer = setTimeout('mnubarMenuTimeout()',MenuShortTimeoutMS);
    } else {
        mnubarDestroyMenu();
    }
}

/* destroy current submenu */
function mnubarDestroyMenu() 
{
    if (mbSubMenuObj != null) {
        document.body.removeChild(mbSubMenuObj);
        mbSubMenuObj = null;
    }
    if (mbSelectedMenu != null) {
        mbSelectedMenu.className = 'menuBarUnsW';
        mbSelectedMenu = null;
    }
    mnubarClearDestroyMenuTimer();
}

/* show submenu */
function mnubarShowMenu(menuId) 
{
    mnubarDestroyMenu();
    var menuObj = document.getElementById(menuId);
    if (menuObj) {
        mbSelectedMenu = menuObj;
        mbSelectedMenu.className = 'menuBarSelW';
        var myMenu = mnubarCreateSubMenu(mbSelectedMenu);
        if (myMenu) {
            myMenu.style.visibility = 'visible';
        }
    }
    mnubarSetDestroyMenuTimer();
}

/* mouseOver menu tab */
function mnubarMouseOverTab(menuId,openMenu) 
{
    var menuObj = document.getElementById(menuId);
    if (menuObj) {
        menuObj.isMouseInside = true;
        if (openMenu) {
            mnubarShowMenu(menuId);
        }
    }
}

/* mouseOut menu tab */
function mnubarMouseOutTab(menuId) 
{
    var menuObj = document.getElementById(menuId);
    if (menuObj) {
        menuObj.isMouseInside = false;
    }
}

/* toggle submenu (show if hidden, destroy if visible) */
function mnubarToggleMenu(menuId) 
{
    var menuObj = document.getElementById(menuId);
    if (!(menuObj)) {
        mnubarDestroyMenu();
        return;
    }
    if (menuObj == mbSelectedMenu) {
        mnubarDestroyMenu();
        return;
    }
    mnubarShowMenu(menuId);
}

/* create submenu frame */
// this creates the object placed in 'mbSubMenuObj'
function mnubarCreateMenuFrame(absLoc, absSiz, height) 
{
    var isSafari           = /Safari/.test(navigator.userAgent);
    var menuObj            = document.createElement('div');
    menuObj.id             = 'subMenu';
    menuObj.name           = 'subMenu';
    menuObj.style.width    = absSiz.width + 'px';
    menuObj.style.left     = absLoc.left + 'px';
    menuObj.style.top      = (absLoc.top + absSiz.height - (isSafari? 0 : 0)) + 'px'; // was (isSafari?6:0)
    menuObj.style.height   = height + 'px';
    menuObj.style.position = 'absolute';
    menuObj.style.cursor   = 'default';
    menuObj.style.zIndex   = 30000;
    menuObj.isMouseInside  = false;
    menuObj.onmouseover    = function() { menuObj.isMouseInside = true; }
    menuObj.onmouseout     = function() { menuObj.isMouseInside = false; }
    return menuObj;
}

// ---
