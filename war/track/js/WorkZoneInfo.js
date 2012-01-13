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
//  2010/09/09  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------

var COMBO_SELECT_CLASS = "";

//var MAX_LEVEL = 5;            // <-- array size
var POLYGON_INDEX = MAX_LEVEL;  // <-- index

var workZonePath = new Array();
for (var i = 0; i < MAX_LEVEL; i++) { 
    workZonePath.push(workZoneSelected[i]); 
}
var workZonePolygon = "";

var wzIsChanging = 0;
var workZoneKey = "";

var workZoneTree_empty = new Array(
    { name:"--", desc:"--", poly:null, children:null }
);

// -------------------------------------

/* return the event 'limit' for non-fleet maps */
function wzLevelComboHTML(selClass, level, selID, selName, optList, optSelect)
{
    // <select class='adminComboBox' name='z_zone' onchange='' disabled>
    //    <option value='key' selected>Description</option>
    //    <option value='key'>Description</option>
    // </select>
    var sel = "";

    /* start tag */
    sel += "<select";
    if (selClass != "") { sel += " class='" + selClass + "'"; }
    if (selName  != "") { sel += " name='"  + selName  + "'"; }
    if (selID    != "") { sel += " id='"    + selID    + "'"; }
    sel += " onchange=\"javascript:wzComboChanged("+level+")\">\n";

    /* list */
    var listLen = (optList)? optList.length : 0;
    for (var i = 0; i < listLen; i++) {
        var opt  = optList[i]; // "name", "desc", "poly"
        var name = opt.name;
        var desc = opt.desc;
        var poly = null; // opt.poly;
        if ((poly != null) && (poly.length > 0)) {
            for (var p = 0; p < poly.length; p++) {
                var pname = name + ","  + poly[p];
                var pdesc = desc + " [" + poly[p] + "]";
                sel += "  <option value='"+pname+"'";
                if (pname == optSelect) { sel += " selected"; }
                sel += ">";
                sel += pdesc;
                sel += "</options>\n";
            }
        } else {
            sel += "  <option value='"+name+"'";
            if (name == optSelect) { sel += " selected"; }
            sel += ">";
            sel += desc;
            sel += "</options>\n";
        }
    }

    /* end tag */
    sel += "</select>\n";

    return sel;
};

function wzUpdateLevelCombo(level, nodeParent, itemSelected)
{
    var itemArray = nodeParent.children;
    var htmlID  = "hLevel_"  + level;
    var comboID = "cbLevel_" + level;
    var tdElem = document.getElementById(htmlID);
    if (tdElem != null) {
        tdElem.innerHTML = wzLevelComboHTML(COMBO_SELECT_CLASS, level, comboID, comboID, itemArray, itemSelected);
    }
};

// -------------------------------------

/* return the event 'limit' for non-fleet maps */
function wzPolygonComboHTML(selClass, selID, selName, optList, optSelect)
{
    // <select class='adminComboBox' name='z_zone' onchange='' disabled>
    //    <option value='key' selected>Description</option>
    //    <option value='key'>Description</option>
    // </select>
    var sel = "";

    /* start tag */
    sel += "<select";
    if (selClass != "") { sel += " class='" + selClass + "'"; }
    if (selName  != "") { sel += " name='"  + selName  + "'"; }
    if (selID    != "") { sel += " id='"    + selID    + "'"; }
    sel += ">\n";

    /* list */
    var listLen = (optList)? optList.length : 0;
    for (var i = 0; i < listLen; i++) {
        var name = optList[i];
        var desc = optList[i];
        var poly = null; // opt.poly;
        sel += "  <option value='"+name+"'";
        if (name == optSelect) { sel += " selected"; }
        sel += ">";
        sel += desc;
        sel += "</options>\n";
    }

    /* end tag */
    sel += "</select>\n";

    return sel;
};

function wzUpdatePolygonCombo(polyArray, itemSelected)
{
    var itemArray = polyArray;
    var htmlID  = "hLevel_"  + POLYGON_INDEX;
    var comboID = "cbLevel_" + POLYGON_INDEX;
    var tdElem = document.getElementById(htmlID);
    if (tdElem != null) {
        tdElem.innerHTML = wzPolygonComboHTML(COMBO_SELECT_CLASS, comboID, comboID, itemArray, itemSelected);
    }
};

// -------------------------------------

function wzGetChildByName(level, parent, childName)
{
    //alert("Attempting to find: " + childName);
    if (parent == null) {
        alert("Parent node is null/empty!");
        return null;
    } else
    if (parent.children == null) {
        alert("Parent has no children!");
        return null;
    } else {
        //alert("["+level+"] parent.children.length == " + parent.children.length);
        for (var c = 0; c < parent.children.length; c++) {
            //alert("Testing parent.children["+c+"].name: " + parent.children[c].name);
            if (parent.children[c].name == childName) {
                return parent.children[c];
            }
        }
        alert("Child node not found: " + childName);
        return null;
    } 
};

function wzSetPath(level)
{
    //alert("Path change: " + level);

    /* parent */
    var nodeParent = workZoneTree;
    if (nodeParent === null) {
        alert("RootNode not defined");
        return;
    }

    /* changing */
    if (wzIsChanging > 0) {
        return;
    }
    wzIsChanging++;

    /* polygon */
    var polyLevel_ = "cbLevel_" + POLYGON_INDEX;
    var polyElem   = document.getElementById(polyLevel_);
    if (polyElem != null) {
        workZonePolygon = polyElem.value; // default value
    } else {
        workZonePolygon = "--";
    }
    var polyChildNode = null;

    /* traverse through all levels */
    workZoneKey = "";
    var blankRemaining = false;
    for (var i = 0; i < MAX_LEVEL; i++) {

        /* level combo-pulldown */
        var cbLevel_ = "cbLevel_" + i;
        var cbElem   = document.getElementById(cbLevel_);

        /* no node pointer? */
        if (nodeParent == null) {
            wzUpdateLevelCombo(i, workZoneTree_empty, "");
            continue;
        }

        /* level ID */
        if ((level < 0) && workZoneSelected && (workZoneSelected.length > i) && (workZoneSelected[i] != "")) {
            workZonePath[i] = workZoneSelected[i];
        } else
        if (cbElem === null) {
            alert("ComboBox not found: " + cbLevel_);
            workZonePath[i] = (nodeParent.children != null)? nodeParent.children[0].name : "?";
        } else {
            if (i <= level) {
                // up to and including the one where the change was indicated
                workZonePath[i] = cbElem.value;
            } else {
                // use the first name at the current level
                if (nodeParent.children === null) {
                    workZonePath[i] = "?";
                } else {
                    var child_0 = nodeParent.children[0];
                    workZonePath[i] = child_0.name;
                }
            }
        }

        /* level */
        wzUpdateLevelCombo(i, nodeParent, workZonePath[i]);

        /* key */
        if ((i + 1) >= MAX_LEVEL) {
            // Field node
            workZoneKey += "," + workZonePath[i];
        } else {
            workZoneKey += "/" + workZonePath[i];
        }

        /* has polygon? */
        var childNode = wzGetChildByName(i, nodeParent, workZonePath[i]);
        if (childNode != null) {
            if ((childNode.poly != null) && (childNode.poly.length > 0)) {
                polyChildNode = childNode;
                if ((workZonePolygon == "--") || (workZonePolygon == "")) {
                    workZonePolygon = polyChildNode.poly[0];
                }
                nodeParent = null;
                continue;
            }
        } else {
            alert("Child node not found: " + workZonePath[i]);
        }

        /* next level parent */
        nodeParent = childNode;
        if (nodeParent === null) {
            //alert("Node not found: ["+i+"/"+level+"] " + workZonePath[i]);
        }

    }

    /* polygon */
    if (polyElem != null) {
        if (polyChildNode != null) {
            if ((workZonePolygon == "--") || (workZonePolygon == "")) {
                workZonePolygon = polyChildNode.poly[0];
            }
            wzUpdatePolygonCombo(polyChildNode.poly, workZonePolygon);
        } else {
            wzUpdatePolygonCombo([ "--" ], workZonePolygon);
        }
    }
    workZoneKey += "," + workZonePolygon;

    /* work zone key */
    document.WorkZoneInfoSelect.z_zone.value = workZoneKey;

    /* no longer changing */
    wzIsChanging--;

};

// -------------------------------------

function wzComboChanged(level)
{

    /* selected node */
    wzSetPath(level);

};

// -------------------------------------
