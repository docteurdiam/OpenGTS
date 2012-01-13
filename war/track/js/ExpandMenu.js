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
// References:
// - http://javascript.internet.com/navigation/click-to-expand-menu.html
// ----------------------------------------------------------------------------
// Change History:
//  2008/10/16  Martin D. Flynn
//     -Initial Release
// ----------------------------------------------------------------------------

var IMAGE_OPENED  = "images/Opened.png";
var IMAGE_CLOSED  = "images/Closed.png";
var IMAGE_LEAF    = "images/Leaf.png";

var DEFAULT_OPEN  = true;

// ----------------------------------------------------------------------------

function ExpandMenu(elemID)
{

    this.cookieList = null;
    var tempCookieList = null;
    if (document.cookie) {
        this.cookieList = document.cookie.split(";");
        tempCookieList = new Array();
        for (i in this.cookieList) {
            tempCookieList[this.cookieList[i].split("=")[0].replace(/ /g,"")] = this.cookieList[i].split("=")[1].replace(/ /g,"");
        }
    }

    this.cookieList = (document.cookie.indexOf("state=") >= 0)? tempCookieList["state"].split(",") : new Array();
    var temp = document.getElementById(elemID);
    
    var cookieCount = 0;
    for (var o = 0; o < temp.getElementsByTagName("li").length; o++) {
        var temp2;
        if (temp.getElementsByTagName("li")[o].getElementsByTagName("ul").length > 0) {
            var doOpen = (cookieCount < this.cookieList.length)? (this.cookieList[cookieCount] == "true") : DEFAULT_OPEN;
            temp2 = document.createElement("span");
            temp2.className = "symbols";
            temp2.style.backgroundImage = doOpen? "url("+IMAGE_OPENED+")" : "url("+IMAGE_CLOSED+")";
            var self = this;
            temp2.onclick = function() {
                self._toggleMenuNode(this.parentNode);
                self._saveMenuState(temp);
            }
            temp.getElementsByTagName("li")[o].insertBefore(temp2,temp.getElementsByTagName("li")[o].firstChild)
            temp.getElementsByTagName("li")[o].getElementsByTagName("ul")[0].style.display = "none";
            if (doOpen) {
                this._toggleMenuNode(temp.getElementsByTagName("li")[o]); // open
            }
            cookieCount++;
        } else {
            temp2 = document.createElement("span");
            temp2.className = "symbols";
            temp2.style.backgroundImage = "url("+IMAGE_LEAF+")";
            temp.getElementsByTagName("li")[o].insertBefore(temp2,temp.getElementsByTagName("li")[o].firstChild);
        }
    }
    
}

ExpandMenu.prototype._toggleMenuNode = function(el)
{
    if (el.getElementsByTagName("ul")[0].style.display == "block") {
        // close
        el.getElementsByTagName("ul")[0].style.display = "none";
        el.getElementsByTagName("span")[0].style.backgroundImage = "url("+IMAGE_CLOSED+")";
    } else {
        // open
        el.getElementsByTagName("ul")[0].style.display = "block";
        el.getElementsByTagName("span")[0].style.backgroundImage = "url("+IMAGE_OPENED+")";
    }
}

ExpandMenu.prototype._saveMenuState = function(temp)
{
    this.cookieList = new Array()
    for (var q = 0; q < temp.getElementsByTagName("li").length; q++) {
        if (temp.getElementsByTagName("li")[q].childNodes.length > 0) {
            if ((temp.getElementsByTagName("li")[q].childNodes[0].nodeName == "SPAN")     && 
                (temp.getElementsByTagName("li")[q].getElementsByTagName("ul").length > 0)  ) {
                this.cookieList[this.cookieList.length] = (temp.getElementsByTagName("li")[q].getElementsByTagName("ul")[0].style.display == "block");
            }
        }
    }
    document.cookie = "state="+this.cookieList.join(",")+";expires="+new Date(new Date().getTime()+365*24*60*60*1000).toGMTString();
}
