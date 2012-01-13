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
//  2008/10/16  Martin D. Flynn
//     -Initial release
//  2009/10/02  Martin D. Flynn
//     -Added 'Search' option
//  2009/11/10  Martin D. Flynn
//     -Fixed search selection bug
//     -Change 'Enter/Return' behavior to select first item (previously would
//      only select the firt item if only 1 item was left in the list).
//  2011/10/03  Martin D. Flynn
//     -Do not search ID if ID is not displayed (see "deviceSearch")
// ----------------------------------------------------------------------------

/* these must match the class definitions in "DeviceChooser.css" */
var ID_CHOOSER_VIEW             = "devChooserView";
var ID_SEARCH_FORM              = "devSearchForm";
var ID_SEARCH_TEXT              = "devSearchText";
var ID_DIV_TABLE                = "DeviceTableList";
var ID_DEVSELECT_TABLE          = "devSelectIDTable";
var CLASS_TABLE_COLUMN_SORTABLE = "sortableX"; // requires 'sorttable.js'
var CLASS_DEVSELECT_DIV_VISIBLE = "devSelectorDiv";
var CLASS_DEVSELECT_DIV_HIDDEN  = "devSelectorDiv_hidden";
var CLASS_DEVSELECT_ROW_HEADER  = "devSelectorRowHeader";
var CLASS_DEVSELECT_COL_HEADER  = "devSelectorColHeader";
var CLASS_DEVSELECT_ROW_DATA    = "devSelectorRowData";
var CLASS_DEVSELECT_ROW_HIDDEN  = "devSelectorRow_hidden";
var CLASS_DEVSELECT_COL_DATA    = "devSelectorColData";
var CLASS_SEARCH_INPUT          = "deviceChooserInput";

var IDPOS_NONE                  = 0;
var IDPOS_FIRST                 = 1;
var IDPOS_LAST                  = 2;

var WIDTH_ID                    = 80;
var WIDTH_DESC                  = 180;

var SEARCH_TEXT_SIZE            = 18;

var PREDEFINED_CHOOSER_HTML     = true;

// ----------------------------------------------------------------------------
// external variable definitions

//var DEVICE_LIST_URL             = 
//var DeviceChooserIDPosition     = 2    // 0=false, 1=first, 2=last
//var DeviceChooserEnableSearch   = true
//var DeviceChooserMatchContains  = true

//var DEVICE_TEXT_ID              = "ID"
//var DEVICE_TEXT_Description     = "Description"
//var DEVICE_TEXT_Search          = "Search"

// ----------------------------------------------------------------------------

var deviceSelectorVisible       = false;
var deviceSelectorView          = null;
var deviceOld_onmousedown       = null;

// ----------------------------------------------------------------------------

var chooserRelativeElementID   = null;
var chooserRelativeElementDesc = null;
var chooserDeviceList          = null;
var chooserFirstDeviceNdx      = -1;

function deviceShowChooserList(elemNameID, elemNameDesc, list)
{

    /* initial clear */
    chooserRelativeElementID   = null;
    chooserRelativeElementDesc = null;
    chooserDeviceList          = null;
    chooserFirstDeviceNdx      = -1;

    /* already displayed? close ... */
    if (deviceSelectorVisible) {
        deviceCloseChooser();
        return;
    }

    /* get destination ID field */
    var locElem = document.getElementById(elemNameID);
    if ((locElem != null) && (locElem.type != "hidden")) {
        // elemNameID
    } else {
        locElem = document.getElementById(elemNameDesc);
        if (locElem != null) {
            // elemNameDesc
        } else {
            return; // not found
        }
    }

    /* save global vars */
    chooserRelativeElementID   = elemNameID;
    chooserRelativeElementDesc = elemNameDesc;
    chooserDeviceList          = list;

    /* location of Chooser */
    var absLoc  = getElementPosition(locElem);
    var absSiz  = getElementSize(locElem);
    var posTop  = absLoc.left;
    var posLeft = absLoc.top + absSiz.height + 2;
    
    if (PREDEFINED_CHOOSER_HTML) {
        
        deviceSelectorView = document.getElementById(ID_CHOOSER_VIEW);
        if (deviceSelectorView != null) {
            deviceSelectorView.className      = CLASS_DEVSELECT_DIV_VISIBLE;
            deviceSelectorView.style.left     = posTop  + 'px';
            deviceSelectorView.style.top      = posLeft + 'px';
            deviceSelectorView.style.position = 'absolute';
            deviceSelectorView.style.cursor   = 'default';
            deviceSelectorView.style.zIndex   = 30000;
            deviceSelectorVisible             = true;
        } else {
            //deviceSelectorView = createDivBox(CLASS_DEVSELECT_DIV_VISIBLE, absLoc.left, absLoc.top + absSiz.height + 2, -1, -1);
        }

    } else {

        /* create div */
        deviceSelectorView = createDivBox(CLASS_DEVSELECT_DIV_VISIBLE, posTop, posLeft, -1, -1);

        /* start html */
        var html = "";

        /* include search */
        if (DeviceChooserEnableSearch) {
            html += "<form id='"+ID_SEARCH_FORM+"' name='"+ID_SEARCH_FORM+"' method='get' action=\"javascript:true;\" target='_top' style='padding-left:5px; background-color:#dddddd;'>";
            html += "<b>"+DEVICE_TEXT_Search+": </b>";
            html += "<input id='"+ID_SEARCH_TEXT+"' name='"+ID_SEARCH_TEXT+"' class='"+CLASS_SEARCH_INPUT+"' type='text' value='' size='"+SEARCH_TEXT_SIZE+"' onkeypress=\"return searchKeyPressed(event);\" onkeyup=\"return deviceSearch();\"/>";
            html += "</form>\n";
        }
        
        /* table */
        html += "<div id='"+ID_DIV_TABLE+"'>\n";
        html += deviceGetTableHTML(list, "");
        html += "</div>\n";
            
        /* make selection table visible */
        deviceSelectorView.innerHTML = html;
        document.body.appendChild(deviceSelectorView);
        deviceSelectorVisible = true;
        
        // make table that we just added sortable
        var tableID = document.getElementById(ID_DEVSELECT_TABLE);
        if (sorttable && tableID) {
            //sorttable.makeSortable(tableID);
        }

    }

    /* override 'onmousedown' */
    deviceOld_onmousedown = document.onmousedown;
    document.onmousedown = function(e) {
        if (!e) var e = window.event;
        if (!e) { return false; }
        var targ = e.target? e.target : e.srcElement? e.srcElement : null;
        if (targ && (targ.nodeType == 3)) { targ = targ.parentNode; } // Safari bug?
        if (targ == locElem) {
            return false;
        } else {
            for (;targ && (targ.nodeName != "BODY"); targ = targ.parentNode) {
                if (targ == deviceSelectorView) { return false; }
            }
            deviceCloseChooser();
            return true;
        }
    };
        
    /* focus on search text area */
    if (DeviceChooserEnableSearch) {
        //document.devSearchForm.devSearchText.focus();
        if (devChooserSearchTextElem) {
            //alert("1) Focusing on Search Text: " + ID_SEARCH_TEXT);
            focusOnSearchText(true);
        } else {
            var searchTextElem = document.getElementById(ID_SEARCH_TEXT);
            if (searchTextElem) {
                //alert("2) Focusing on Search Text: " + ID_SEARCH_TEXT);
                devChooserSearchTextElem = searchTextElem;
                focusOnSearchText(true);
            } else {
                searchTextElem = document.getElementByName(ID_SEARCH_TEXT);
                if (searchTextElem) {
                    //alert("3) Focusing on Search Text: " + ID_SEARCH_TEXT);
                    devChooserSearchTextElem = searchTextElem;
                    focusOnSearchText(true);
                } else {
                    alert("Search Text ID not found: " + ID_SEARCH_TEXT);
                }
            }
        }
    }

}

function focusOnSearchText(callback)
{
    if (DeviceChooserEnableSearch && devChooserSearchTextElem) {
        devChooserSearchTextElem.focus();
        devChooserSearchTextElem.select();
        if (callback) {
            //setTimeout("focusOnSearchText(false);", 1000);
        } else {
            //alert("Focused on search text input  ...");
        }
    }
}

var searchTableHeaderHtml = null;
function deviceGetTableHTML(list, searchVal)
{
    var idWidth = WIDTH_ID;
    var dsWidth = WIDTH_DESC;
    searchVal = searchVal.toLowerCase();

    /* table header */
    if (searchTableHeaderHtml == null) {
        var h = "";
        // begin table HTML
        h += "<table id='"+ID_DEVSELECT_TABLE+"' class='"+CLASS_TABLE_COLUMN_SORTABLE+"' cellspacing='0' cellpadding='0' border='1'>\n";
        // table header
        h += "<thead>\n";
        h += "<tr class='"+CLASS_DEVSELECT_ROW_HEADER+"'>";
        if (DeviceChooserIDPosition == IDPOS_NONE) {
            h += "<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>";
        } else 
        if (DeviceChooserIDPosition == IDPOS_LAST) {
            h += "<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>";
            h += "<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+DEVICE_TEXT_ID+"</th>";
        } else {
            h += "<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+DEVICE_TEXT_ID+"</th>";
            h += "<th nowrap class='"+CLASS_DEVSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+DEVICE_TEXT_Description+"</th>";
        }
        h += "</tr>\n";
        h += "</thead>\n";
        searchTableHeaderHtml = h;
    }
    var html = searchTableHeaderHtml;
    
    /* pre-build TD html */
    var TD_idCell = "<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:deviceSelected(";
    var TD_dsCell = "<td nowrap class='"+CLASS_DEVSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected(";

    /* table body */
    html += "<tbody>\n";
    chooserFirstDeviceNdx = -1;
    if (!list) { list = []; }
    for (var d = 0; d < list.length; d++) {

        /* omit items not matched */
        if ((searchVal != "") && !list[d].desc.toLowerCase().startsWith(searchVal)) { 
            continue; 
        }
        var idVal = list[d].id;
        var dsVal = escapeText(list[d].desc);
        
        /* save first item */
        if (chooserFirstDeviceNdx < 0) {
            chooserFirstDeviceNdx = d;
        }

        /* save matched item */
        var selNdx = d;

        /* write html */
        html += "<tr class='" + CLASS_DEVSELECT_ROW_DATA + "'>";
        if (DeviceChooserIDPosition == IDPOS_NONE) {
            html += TD_dsCell + selNdx + ")\">" + dsVal + "</td>";
        } else 
        if (DeviceChooserIDPosition == IDPOS_LAST) {
            html += TD_dsCell + selNdx + ")\">" + dsVal + "</td>";
            html += TD_idCell + selNdx + ")\">" + idVal + "</td>";
        } else {
            html += TD_idCell + selNdx + ")\">" + idVal + "</td>";
            html += TD_dsCell + selNdx + ")\">" + dsVal + "</td>";
        }
        html += "</tr>";

    }
    html += "</tbody>\n";

    html += "</table>\n";
    return html;

}

// ----------------------------------------------------------------------------

/* invoked on 'onkeypress' */
function searchKeyPressed(event)
{
    var isCR = isEnterKeyPressed(event);
    if (isCR && (chooserFirstDeviceNdx >= 0)) {
        deviceSelected(chooserFirstDeviceNdx);
        return false;
    }
    return !isCR;
}

function isSearchMatch(searchVal, idVal, dsVal)
{
    if (!searchVal || (searchVal == "")) {
        return true;
    } else
    if (DeviceChooserMatchContains) {
        if (dsVal && dsVal.contains(searchVal)) {
            return true;
        } else
        if (idVal && idVal.contains(searchVal)) {
            return true;
        }
    } else {
        if (dsVal && dsVal.startsWith(searchVal)) {
            return true;
        } else
        if (idVal && idVal.startsWith(searchVal)) {
            return true;
        }
    }
    return false;
}

function deviceSearch(event)
{
    if (!DeviceChooserEnableSearch) { return false; }
    //var searchVal = document.devSearchForm.devSearchText.value.toLowerCase();
    var searchTextElem = document.getElementById(ID_SEARCH_TEXT);
    if (!searchTextElem) { searchTextElem = document.getElementByName(ID_SEARCH_TEXT); }
    var searchVal = (searchTextElem)? searchTextElem.value.toLowerCase() : "";
    var tableDiv = document.getElementById(ID_DIV_TABLE);
    if (tableDiv) {

        /* DOM search through table */
        chooserFirstDeviceNdx = -1;
        var tableID = document.getElementById(ID_DEVSELECT_TABLE);
        if (PREDEFINED_CHOOSER_HTML) {
            
            if (tableID) {
                for (var i = 0; i < tableID.rows.length; i++) {
                    var row    = tableID.rows[i];
                    var idVal  = (DeviceChooserIDPosition != IDPOS_NONE)? row.getAttribute("idVal") : null;
                    var dsVal  = row.getAttribute("dsVal"); // description
                    if (dsVal == null) {
                        // skip these records (first record is the header)
                    } else
                    if (isSearchMatch(searchVal,idVal,dsVal)) {
                        if (chooserFirstDeviceNdx < 0) {
                            chooserFirstDeviceNdx = numParseInt(row.getAttribute("selNdx"),i-1);
                        }
                        //if (row.className != CLASS_DEVSELECT_ROW_DATA) {
                            row.className =  CLASS_DEVSELECT_ROW_DATA;
                        //}
                        // setting the 'style' directly does not produce the desired results.
                        //row.style.visibility = "visible";
                        //row.style.display    = "inline";
                    } else {
                        //if (row.className != CLASS_DEVSELECT_ROW_HIDDEN) {
                            row.className =  CLASS_DEVSELECT_ROW_HIDDEN;
                        //}
                        // setting the 'style' directly does not produce the desired results.
                        //row.style.visibility = "collapse";
                        //row.style.display    = "none";
                    }
                }
            } else {
                alert("Table not found: " + ID_DEVSELECT_TABLE);
            }
            
        } else {
    
            /* html */
            var tableHtml = deviceGetTableHTML(chooserDeviceList, searchVal);
            tableDiv.innerHTML = tableHtml;
        
        }

    }
    return !isEnterKeyPressed(event);
}

// ----------------------------------------------------------------------------

function deviceCloseChooser()
{
    if (deviceSelectorVisible) {
        if (PREDEFINED_CHOOSER_HTML) {
            deviceSelectorView.className = CLASS_DEVSELECT_DIV_HIDDEN;
        } else {
            document.body.removeChild(deviceSelectorView);
            deviceSelectorView = null;
        }
        document.onmousedown = deviceOld_onmousedown;
        deviceSelectorVisible = false;
    }
}

// ----------------------------------------------------------------------------

function deviceLoadList(elemNameID, elemNameDesc, deviceListURL) 
{
    try {
        var req = jsmGetXMLHttpRequest();
        if (req) {
            req.open("GET", deviceListURL, true);
            //req.setRequestHeader("CACHE-CONTROL", "NO-CACHE");
            //req.setRequestHeader("PRAGMA", "NO-CACHE");
            req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    var data = req.responseText;
                    deviceParseList(elemNameID, elemNameDesc, data);
                } else
                if (req.readyState == 1) {
                    // alert('Loading points from URL: [' + req.readyState + ']\n' + mapURL);
                } else {
                    // alert('Problem loading URL? [' + req.readyState + ']\n' + mapURL);
                }
            }
            req.send(null);
        } else {
            alert("Error [deviceLoadList]:\n" + deviceURL);
        }
    } catch (e) {
        alert("Error [deviceLoadList]:\n" + e);
    }
};

function deviceParseList(elemNameID, elemNameDesc, data) 
{
    // TODO: parse 'data'
    var list = new Array();
    for (var d = 1; d <= 500; d++) {
        var D = new Object();
        D.id = "device_" + d;
        D.desc = "My Device #" + d;
        list.push(D);
    }
    deviceShowChooserList(elemNameID, elemNameDesc, list)
}

// ----------------------------------------------------------------------------

function deviceSelected(x)
{
    if (x < 0) { return; }
    var tableID = document.getElementById(ID_DEVSELECT_TABLE);
    if (!tableID) { return; }
    if (x >= tableID.rows.length) { return; }
    var selRow = tableID.rows[x];
    var selNdx = numParseInt(selRow.getAttribute("selNdx"),-1);
    if (x != selNdx) {
        selRow = tableID.rows[x + 1];
        selNdx = numParseInt(selRow.getAttribute("selNdx"),-1);
        if (x != selNdx) {
            alert("Cannot find selected row: " + x);
            return;
        }
    }
    var selID   = selRow.getAttribute("idVal");
    var selDesc = selRow.getAttribute("dsVal");
    //alert("Selected ("+x+") " + selID + " - " +selDesc);

    /*
    var selItem = (chooserDeviceList && (x < chooserDeviceList.length))? chooserDeviceList[x] : null;
    var selID   = selItem.id;
    var selDesc = selItem.desc;
    */

    // set id
    var idElem  = chooserRelativeElementID? document.getElementById(chooserRelativeElementID) : null;
    if (idElem != null) { idElem.value = selID; }
    
    // set description
    var dsElem  = chooserRelativeElementDesc? document.getElementById(chooserRelativeElementDesc) : null;
    if (dsElem != null) { dsElem.value = selDesc; }

    /* device delected */
    deviceCloseChooser();
    deviceDeviceChanged();
    
}

// ----------------------------------------------------------------------------
