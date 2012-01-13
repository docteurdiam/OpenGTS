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
//  2009/11/10  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------

// ----------------------------------------------------------------------------

var CLASS_ICONSELECT_DIV        = "PushpinChooser";
var CLASS_ICONSELECT_TABLE      = "PushpinChooser";
var CLASS_ICONSELECT_TR         = "PushpinChooser";
var CLASS_ICONSELECT_TD_ICON    = "PushpinChooser_icon";
var CLASS_ICONSELECT_TD_EVAL    = "PushpinChooser_eval";
var CLASS_ICONSELECT_SPAN       = "PushpinChooser";

// ----------------------------------------------------------------------------

var pushpinElementID        = null;
var pushpinChooserView      = null;
var pushpinOld_onmousedown  = null;

// ----------------------------------------------------------------------------

/* callback when pushpin is selected from chooser */
function ppcPushpinSelected(ndx)
{
    if (pushpinElementID && (ndx >= 0) && (ndx < ppcPushpinChooserList.length)) {
        var selPP = ppcPushpinChooserList[ndx];
        var textElem = document.getElementById(pushpinElementID);
        if (textElem) { textElem.value = selPP.name; }
    }
    ppcCloseIconChooser();
}

// ----------------------------------------------------------------------------

/* close the pushpin chooser */
function ppcCloseIconChooser()
{
    if (pushpinChooserView != null) {
        document.body.removeChild(pushpinChooserView);
        pushpinChooserView = null;
        document.onmousedown = pushpinOld_onmousedown;
    }
};

// ----------------------------------------------------------------------------

/* toggle display the pushpin chooser */
function ppcShowPushpinChooser(elemNameID)
{

    /* already displayed? close ... */
    if (pushpinChooserView != null) {
        ppcCloseIconChooser();
        return;
    }

    /* get destination ID field */
    var locElem = document.getElementById(elemNameID);
    if ((locElem != null) && (locElem.type != "hidden")) {
        // elemNameID
    } else {
        return false; // not found
    }
    pushpinElementID = elemNameID;

    /* location of IconChooser */
    var absLoc = getElementPosition(locElem);
    var absSiz = getElementSize(locElem);
    pushpinChooserView = createDivBox(CLASS_ICONSELECT_DIV, absLoc.left, absLoc.top + absSiz.height + 2, 0, 0);

    /* begin table HTML */
    var html = "";
    html += "<table class='"+CLASS_ICONSELECT_TABLE+"' cellspacing='0' cellpadding='0' border='1'>\n";
    html += "<tbody>\n";
    
    /* single table row */
    var maxIconsPerRow = 6;
    var columnCount = 0;
    html += "<tr class='"+CLASS_ICONSELECT_TR+"'>";
    for (var i = 0; i < ppcPushpinChooserList.length; i++) {
        var pp   = ppcPushpinChooserList[i];
        var name = pp.name;
        var desc = (pp.desc != "")? pp.desc : name;
        var W    = pp.width;
        var H    = pp.height;
        var img  = pp.image;
        if (name == "") { name = "&nbsp;"; }
        var tdClass = pp.isEval? CLASS_ICONSELECT_TD_EVAL : CLASS_ICONSELECT_TD_ICON;
        html += "<td nowrap class='"+tdClass+"' width='"+W+"' height='"+H+"' onclick=\"javascript:ppcPushpinSelected("+i+")\">";
        if (img == "?") {
            html += "<b><font size='+2'>?</font></b>";
        } else
        if (img != "")  {
            html += "<img width='"+W+"' height='"+H+"' src='"+img+"'>";
        } else {
            //
        }
        html += "<br>";
        html += "<span class='"+CLASS_ICONSELECT_SPAN+"'>"+desc+"</span>";
        html += "</td>\n";
        columnCount++;
        if ((maxIconsPerRow > 0) && (columnCount >= maxIconsPerRow)) {
            html += "<td class='"+tdClass+"' width='7'>&nbsp;</td>\n";
            html += "</tr>\n";
            html += "<tr class='"+CLASS_ICONSELECT_TR+"'>";
            columnCount = 0;
        }
    }
    html += "</tr>\n";
    
    /* end table */
    html += "</tbody>\n";
    html += "</table>\n";

    /* make selection table visible */
    pushpinChooserView.innerHTML = html;
    document.body.appendChild(pushpinChooserView);
    pushpinOld_onmousedown = document.onmousedown;
    document.onmousedown = function(e) {
        if (!e) var e = window.event;
        if (!e) { return false; }
        var targ = e.target? e.target : e.srcElement? e.srcElement : null;
        if (targ && (targ.nodeType == 3)) { targ = targ.parentNode; } // Safari bug?
        if (targ == locElem) {
            return false;
        } else {
            for (;targ && (targ.nodeName != "BODY"); targ = targ.parentNode) {
                if (targ == pushpinChooserView) { return false; }
            }
            ppcCloseIconChooser();
            return true;
        }
    };

};

// ----------------------------------------------------------------------------
