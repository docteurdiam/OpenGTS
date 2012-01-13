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
//     -Moved from JavaScriptTools.java
//  2008/07/08  Martin D. Flynn
//     -Updated String "trim()"
//  2008/10/16  Martin D. Flynn
//     -Added 'numParseInt'
//  2009/07/01  Martin D. Flynn
//     -Added 'createXMLDocument'
//  2009/11/01  Martin D. Flynn
//     -Added function 'escapeText'
//  2009/12/06  Martin D. Flynn
//     -Fixed 'getKeyCode' (thanks to Pierluigi Bucolo)
//  2010/04/11  Martin D. Flynn
//     -Included "getXMLHttpRequest" (moved from 'jsmap.js')
//     -Added "rgbLighter", "rgbDarker"
//  2011/03/08  Martin D. Flynn
//     -Added String 'contains'
//  2011/08/22  Martin D. Flynn
//     -Added unicode encode/decode functions
// ----------------------------------------------------------------------------

// ----------------------------------------------------------------------------
// --- String prototypes

// String.trim()
String.prototype.trim = function() { 
    // http://www.nicknettleton.com/zine/javascript/trim-a-string-in-javascript
    return this.replace(/^\s+|\s+$/g,''); 
};

// String.startsWith(value)
String.prototype.startsWith = function(s) { 
    try {
        
        // blank string
        if (!s || (s == "")) {
            return true;
        } else
        if (s.length > this.length) {
            return false;
        }
        
        // http://www.tek-tips.com/faqs.cfm?fid=6620
        //return (this.match("^" + s) == s); 
        
        //http://underthefold.com/underthefeed/?id=23
        //var re = new RegExp('^' + s, '');
        //return s.match(re)? true : false;

        // substring check
        return (s === this.substr(0, s.length));
        
    } catch (e) {
        return false;
    }
};

// String.endsWith(value)
String.prototype.endsWith = function(s) { 
    try {

        // blank string
        if (!s || (s == "")) {
            return true;
        } else
        if (s.length > this.length) {
            return false;
        }

        // http://www.tek-tips.com/faqs.cfm?fid=6620
        //return (this.match(s + "$") == s); 
        
        //http://underthefold.com/underthefeed/?id=23
        //var re = new RegExp(s + "$", "g");
        //return s.match(re)? true : false;

        // substring check
        var len = this.length;
        return (s === this.substr(len - s.length, len));

    } catch (e) {
        return false;
    }
};

// String.contains(substring)
String.prototype.contains = function(s) { 
    try {

        // blank string
        if (!s || (s == "")) {
            return (this.length == 0); // true iff this string is also empty
        } else
        if (s.length > this.length) {
            return false;
        }

        // indexOf check
        return (this.indexOf(s) >= 0);

    } catch (e) {
        return false;
    }
};

// ----------------------------------------------------------------------------
// --- String parsing/formatting

/* string default value, if blank */
function strDefault(s, dft)
{
    if (s == null) {
        return dft;
    } else
    if (s.trim() == "") {
        return dft;
    } else {
        return s;
    }
};

/* encode URI characters */
function strEncode(s)
{
    if (s) {
        return encodeURIComponent(s);   // escape(s);
    } else {
        return "";
    }
};

/* encode URI characters */
function strDecode(s)
{
    if (s) {
        return decodeURIComponent(s);   // unescape(s);
    } else {
        return "";
    }
};

/* escape special HTML characters */
function escapeText(s)
{
    if (!s) { return ""; }
    var n = "";
    for (var i = 0; i < s.length; i++) {
        var c = s.charAt(i);
        if (c == '<') {
            n += '&lt;';
        } else
        if (c == '>') {
            n += '&gt;';
        } else
        if (c == '&') {
            n += '&amp;';
        } else
        if (c == '\'') {
            n += '&#39;'; // '&apos;'; <-- doesn't work on IE
        } else {
            n += c;
        }
    }
    return n; // 
};

// ----------------------------------------------------------------------------
// --- Unicode

function parseHexChar(hex)
{
    var cp = numParseHex(hex,-1);
    if (cp == -1) { 
        return ""; 
    } else
    if (cp <= 0xFFFF) {
        return String.fromCharCode(cp);
    } else
    if (cp <= 0x10FFFF) { // (d <= 0x10FFFF)
        cp -= 0x10000; // 
        var D1 = 0xD800 | ((cp >> 10) & 0x03FF);
        var D2 = 0xDC00 | (cp & 0x03FF);
        return String.fromCharCode(D1, D2);
    } else {
        return "?";
    }
};

function decodeUnicode(s)
{
    s = s.replace(/\\U([A-Fa-f0-9]{8})/g,function(m,h){return parseHexChar(h)}); // 4 byte chars
    s = s.replace(/\\u([A-Fa-f0-9]{4})/g,function(m,h){return parseHexChar(h)}); // 2 byte chars
    return s;
};

// ----------------------------------------------------------------------------
// --- UTF8

function decodeUTF8(s)
{
    return decodeURIComponent(escape(s));
};

function encodeUTF8(s)
{
    return unescape(encodeURIComponent(s));
};

// ----------------------------------------------------------------------------
// --- Number parsing/formatting

function _trimZeros(val)
{
    if (typeof val == "string") {
        while (val.startsWith(" ")) { val = val.substring(1); }
        while ((val.length > 1) && val.startsWith("0")) { val = val.substring(1); }
    }
    return val;
}

/* parse 'hex' int value */
function numParseHex(val, dft) 
{
    var num = parseInt(val,16);
    if (isNaN(num)) { num = dft; }
    return num;
};

/* parse 'decimal' int value */
function numParseInt(val, dft) 
{
    var num = parseInt(_trimZeros(val));
    if (isNaN(num)) { num = dft; }
    return num;
};

/* parse float value */
function numParseFloat(val, dft) 
{
    var num = parseFloat(_trimZeros(val));
    if (isNaN(num)) { num = dft; }
    return num;
};

/* format floating-point value with specified number of decimal points */
// an unsophisticated numeric formatter
function numFormatFloat(val, dec) 
{
    var num = numParseFloat(val,0);
    if (dec > 0) {
        var neg = (num >= 0)? '' : '-';
        num = Math.abs(num);
        var d;
        for (d = 0; d < dec; d++) { num *= 10; }
        num = parseInt(num + 0.5);
        var str = new String(num);
        while (str.length <= dec) { str = '0' + str; }
        str = str.substring(0, str.length - dec) + '.' + str.substring(str.length - dec);
        return neg + str;
    } else {
        num = parseInt((num >= 0)? (num + 0.5) : (num - 0.5));
        return new String(num);
    }
};

// ----------------------------------------------------------------------------
// --- latitude/logiutude distance calculations

var EARTH_RADIUS_KM     = 6371.0088;
var EARTH_RADIUS_METERS = EARTH_RADIUS_KM * 1000.0;

/* is valid latitude/longitude */
function geoIsValid(lat, lon)
{
    if (!lat && !lon) {
        return false;
    } else
    if ((lat > 0.0001) || (lat < -0.0001)) {
        return true;
    } else
    if ((lon > 0.0001) || (lon < -0.0001)) {
        return true;
    } else {
        return false;
    }
}

/* Square */
function geoSQ(val) 
{
    return val * val;
};

/* degrees to radians */
function geoRadians(deg) 
{
    return deg * (Math.PI / 180.0);
};

/* radians to degrees */
function geoDegrees(rad) 
{
    return rad * (180.0 / Math.PI);
};

/* return distance (in radians) between points */
function geoDistanceRadians(lat1, lon1, lat2, lon2) 
{
    var rlat1 = geoRadians(lat1);
    var rlon1 = geoRadians(lon1);
    var rlat2 = geoRadians(lat2);
    var rlon2 = geoRadians(lon2);
    var dtlat = rlat2 - rlat1;
    var dtlon = rlon2 - rlon1;
    var a     = geoSQ(Math.sin(dtlat/2.0)) + (Math.cos(rlat1) * Math.cos(rlat2) * geoSQ(Math.sin(dtlon/2.0)));
    var rad   = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    return rad;
};

/* return distance (in meters) between points */
function geoDistanceMeters(lat1, lon1, lat2, lon2) 
{
    return geoDistanceRadians(lat1,lon1,lat2,lon2) * EARTH_RADIUS_METERS;
};

/* return heading (degrees) from first point to second point */
function geoHeading(lat1, lon1, lat2, lon2)
{
    var rlat1 = geoRadians(lat1);
    var rlon1 = geoRadians(lon1);
    var rlat2 = geoRadians(lat2);
    var rlon2 = geoRadians(lon2);
    var rDist = geoDistanceRadians(lat1, lon1, lat2, lon2);
    var rad   = Math.acos((Math.sin(rlat2) - (Math.sin(rlat1) * Math.cos(rDist))) / (Math.sin(rDist) * Math.cos(rlat1)));
    if (Math.sin(rlon2 - rlon1) < 0) { rad = (2.0 * Math.PI) - rad; }
    var deg   = geoDegrees(rad);
    return deg;
};

function geoRadiusPoint(lat, lon, radiusM, heading)
{
    while (heading <    0.0) { heading += 360.0; }
    while (heading >= 360.0) { heading -= 360.0; }
    var crLat = geoRadians(lat);          // radians
    var crLon = geoRadians(lon);          // radians
    var d     = radiusM / EARTH_RADIUS_METERS;
    var xrad  = geoRadians(heading);            // radians
    var rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
    var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
    return { lat: geoDegrees(rrLat), lon: geoDegrees(rrLon) }; // same as JSMapPoint
};

// ----------------------------------------------------------------------------
// --- cookie utilities

var cookieTag = 'OpenGTS';

/* set cookie */
function setCookie(key, val) 
{
    var d;
    if (val == null) {
        d = (new Date(0)).toGMTString();
        val = '';
    } else {
        var expireMin = 15;
        d = new Date((new Date()).getTime() + (expireMin * 60000));
    }
    document.cookie = cookieTag+ '.' + key + '=' + strEncode(val) + ';; expires=' + d;
};

/* get cookie */
function getCookie(key, dft) 
{
    var c = document.cookie, k = cookieTag+ '.' + key + '=', p = c.indexOf(k);
    if (p >= 0) {
        var pe = c.indexOf(';',p);
        if (pe < 0) { pe = c.length; }
        return strDecode(c.substring(p + k.length, pe));
    } else {
        return dft;
    }
};

// ----------------------------------------------------------------------------
// --- Parse request URL

/* return argument for specified key in request string */
function getQueryArg(argName) 
{
    var mainURL = window.location.search;
    var argStr = mainURL.split('?');
    if (argStr.length > 1) {
        var args = argStr[1].split('&');
        for (i in args) {
            var keyVal = args[i].split('=');
            if (argName == keyVal[0]) {
                //document.write('Found Key: ' + keyVal[0] + ' == ' + keyVal[1] + '<br>');
                return keyVal[1];
            }
        }
    }
    return null;
};

// ----------------------------------------------------------------------------
// --- Element location/size

/* return relative position of specified element */
function getElementPosition(elem) 
{
    var ofsLeft = 0;
    var ofsTop  = 0;
    var ofsElem = elem;
    while (ofsElem) {
        ofsLeft += ofsElem.offsetLeft; // - ofsElem.scrollLeft;
        ofsTop  += ofsElem.offsetTop;  // - ofsElem.scrollTop;
        ofsElem  = ofsElem.offsetParent;
    }
    if ((navigator.userAgent.indexOf('Mac') >= 0) && (typeof document.body.leftMargin != 'undefined')) {
        ofsLeft += document.body.leftMargin;
        ofsTop  += document.body.topMargin;
    }
    return { left:ofsLeft, top:ofsTop };
};

/* return size of specified element */
function getElementSize(elem) 
{
    return { width:elem.offsetWidth, height:elem.offsetHeight };
};

// ----------------------------------------------------------------------------
// --- Create new window

/* open a resizable window and display specified URL */
function openResizableWindow(url, name, W, H) 
{
    //  "resizable=[yes|no]"
    //  "width='#',height='#'"
    //  "screenX='#',screenY='#',left='#',top='#'"
    //  "status=[yes|no]"
    //  "scrollbars=[yes|no]"
    var attr = "resizable=yes";
    attr += ",menubar=no,toolbar=no";
    if ((W > 0) && (H > 0)) {
        attr += ",width=" + W + ",height=" + H;
        var L = ((screen.width - W) / 2), T = ((screen.height - H) / 2);
        attr += ",screenX=" + L + ",screenY=" + T + ",left=" + L + ",top=" + T;
    }
    attr += ",status=yes,scrollbars=yes";
    var win = window.open(url, name, attr, false);
    if (win) {
        //if (!(typeof win.moveTo == "undefined")) { win.moveTo(L,T); }
        if (!(typeof win.focus  == "undefined")) { win.focus(); }
        return win;
    } else {
        return null;
    }
};

/* open a resizable window and display specified URL */
function openFixedWindow(url, name, W, H) 
{
    //  "resizable=[yes|no]"
    //  "width='#',height='#'"
    //  "screenX='#',screenY='#',left='#',top='#'"
    //  "status=[yes|no]"
    //  "scrollbars=[yes|no]"
    var attr = "resizable=no";
    attr += ",menubar=no,toolbar=no";
    if ((W > 0) && (H > 0)) {
        attr += ",width=" + W + ",height=" + H;
        var L = ((screen.width - W) / 2), T = ((screen.height - H) / 2);
        attr += ",screenX=" + L + ",screenY=" + T + ",left=" + L + ",top=" + T;
    }
    attr += ",status=no,scrollbars=no";
    var win = window.open(url, name, attr, false);
    if (win) {
        //if (!(typeof win.moveTo == "undefined")) { win.moveTo(L,T); }
        if (!(typeof win.focus  == "undefined")) { win.focus(); }
        return win;
    } else {
        return null;
    }
};

// ----------------------------------------------------------------------------

function openURL(url, target)
{
    // parent.main.location = url;
    // window.location.href = url;
    // document.location.href = url;
    if (!target) { target = "_top"; }
    window.open(url, target);
}

// ----------------------------------------------------------------------------
// --- HexColor value

/* return 6-digit hex value for specified RGB color */
function rgbHex(R,G,B)
{
    // "D.toString(16)" fails to produce a proper hex value if D is negative!!!!
    var D = 0x1F000000 | ((R & 0xFF) << 16) | ((G & 0xFF) << 8) | (B & 0xFF);
    var C = D.toString(16).toUpperCase();
    //alert("RGB " + R + "/" + G + "/" + B + " => " + C);
    return C.substr(2,6); // makes sure it has a leading '0' if necessary
}

/* return RGB structure from 6-digit hex value */
function rgbVal(rgb)
{
    try {
        if (rgb.startsWith("#")) { rgb = rgb.substr(1); }
        var Rx = rgb.substr(0,2);
        var Gx = rgb.substr(2,2);
        var Bx = rgb.substr(4,2);
        var rgb = eval("[ 0x"+Rx+", 0x"+Gx+", 0x"+Bx+" ]");
        return { R:rgb[0], G:rgb[1], B:rgb[2] };
    } catch (e) {
        return { R:0, G:0, B:0 };
    }
}

/* return new 'lighter' RGB structure */
function rgbLighter(RGB, p)
{
    try {
        var r = parseInt(RGB.R + ((255 - RGB.R) * p) + 0.5);
        var g = parseInt(RGB.G + ((255 - RGB.G) * p) + 0.5);
        var b = parseInt(RGB.B + ((255 - RGB.B) * p) + 0.5);
        r = (r < 0)? 0 : (r > 255)? 255 : r;
        g = (g < 0)? 0 : (g > 255)? 255 : g;
        b = (b < 0)? 0 : (b > 255)? 255 : b;
        return { R:r, G:g, B:b };
    } catch (e) {
        return { R:0, G:0, B:0 };
    }
}

/* return new 'lighter' RGB structure */
function rgbDarker(RGB, p)
{
    try {
        var r = parseInt(RGB.R - (RGB.R * p) + 0.5);
        var g = parseInt(RGB.G - (RGB.G * p) + 0.5);
        var b = parseInt(RGB.B - (RGB.B * p) + 0.5);
        r = (r < 0)? 0 : (r > 255)? 255 : r;
        g = (g < 0)? 0 : (g > 255)? 255 : g;
        b = (b < 0)? 0 : (b > 255)? 255 : b;
        return { R:r, G:g, B:b };
    } catch (e) {
        return { R:0, G:0, B:0 };
    }
}

// ----------------------------------------------------------------------------
// --- Div frame

/* create 'div' frame/box */
// var myElem = document.getElementById(someID);
// var absLoc = getElementPosition(myElem);
// var absSiz = getElementSize(myElem);
// var divObj = createDivBox('myid', absLoc.left, absLoc.top + absSiz.height, absSiz.width, H);
function createDivBox(idName, X, Y, W, H) 
{
    var isSafari           = /Safari/.test(navigator.userAgent);
    var divObj             = document.createElement('div');
    divObj.id              = idName;
    divObj.name            = idName;
    divObj.className       = idName;
    divObj.style.left      = X + 'px';
    divObj.style.top       = Y + 'px'; // (Y - (isSafari? 6 : 0)) + 'px';
    if (W > 0) {
        divObj.style.width  = W + 'px';
    }
    if (H > 0) {
        divObj.style.height = H + 'px';
    }
    divObj.style.position  = 'absolute';
    divObj.style.cursor    = 'default';
    divObj.style.zIndex    = 30000;
    divObj.style.overflow  = "auto";
    return divObj;
    // divObj.innerHTML = "<html...>";
    // document.body.appendChild(divObj);
    // document.body.removeChild(divObj);

};

// ----------------------------------------------------------------------------

/* return key code */
function getKeyCode(e)
{
    var key = 0;
    try {
        key = document.layers        ? e.which   : 
              document.all           ? e.keyCode : 
              document.getElementById? (e.keyCode || e.which) : 
              0;
    } catch (err) {
        key = 0;
    }
    //alert("KeyCode = " + key);
    return key;
}

/* convert key code to String */
function getKeyString(e)
{
    var kc = getKeyCode(e);
    return String.fromCharCode(kc);
};

/* return true if this event represents an Digit key (0..9) */
function isDigitKeyPressed(e)
{
    var kc = getKeyCode(e);
    return ((kc >= 0x30) && (kc <= 0x39))? true : false;
};

/* return true if this event represents an 'Enter' key (carriage return) */
function isEnterKeyPressed(e)
{
    var kc = getKeyCode(e);
    return (kc == 13)? true : false;
};

/* ignore enter key "onKeyPress" */
function ignoreEnterKeyPress(e)
{
    if (isEnterKeyPressed(e)) { 
        e.keyCode = 0;  // appears to be required for IE
        // e.returnValue = false; 
        // e.cancel = true;
        return false; 
    } else { 
        return true; 
    }
}

// ----------------------------------------------------------------------------

/**
*** Returns an AJAX request object
**/
function getXMLHttpRequest() 
{
    var req = null;
    // native XMLHttpRequest version
    if (window.XMLHttpRequest) {
        try {
            req = new XMLHttpRequest();
        } catch(e) {
            req = null;
        }
    } else
    // IE/Win ActiveX version
    if (window.ActiveXObject) {
        try {
            req = new ActiveXObject('Msxml2.XMLHTTP');
        } catch(e) {
            try {
                req = new ActiveXObject('Microsoft.XMLHTTP');
            } catch(e) {
                req = null;
            }
        }
    } else {
        req = null;
    }
    return req;
};

// ----------------------------------------------------------------------------

/* create XML Parser */
function createXMLDocument(xmlText)
{
    try {
        var xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
        xmlDoc.async = "false";
        xmlDoc.loadXML(xmlText);
        return xmlDoc; 
    } catch (e) {
        try {
            var parser = new DOMParser();
            var xmlDoc = parser.parseFromString(xmlText,"text/xml");
            return xmlDoc;
        } catch (x) {
            alert("Error loading XML: " + e.message);
            return null;
        }
    }
};

/* get node attribute */
function getXMLNodeAttribute(node, key, dft)
{
    var attrItem = node.getNamedItem(key);
    return (attrItem != null)? attrItem.nodeValue : dft;
};

// ----------------------------------------------------------------------------

/* gets the checked radio value */
function getCheckedRadioValue(radioObj)
{
    try {
        if (radioObj) {
            var radioLen = radioObj.length;
            if (radioLen == undefined) {
                if (radioObj.checked) {
                    return radioObj.value;
                }
            } else {
                for (var i = 0; i < radioLen; i++) {
                    if (radioObj[i].checked) {
                        return radioObj[i].value;
                    }
                }
            }
        }
    } catch (e) {
        // ignore error
    }
    return "";
};

/* gets the checked radio value */
function setCheckedRadioValue(radioObj, value)
{
    try {
        if (radioObj) {
            var radioLen = radioObj.length;
            if (radioLen == undefined) {
                radioObj.checked = (radioObj.value == value.toString());
                //alert("Value set TRUE(1): " + radioObj.value + " =? " + value.toString());
                return true;
            }
            for (var i = 0; i < radioLen; i++) {
                if (radioObj[i].value == value.toString()) {
                    radioObj[i].checked = true;
                    //alert("Value set TRUE(2): " + radioObj[i].value + " =? " + value.toString());
                    return true;
                }
            }
            //alert("Value not found: " + value.toString());
        } else {
            //alert("Radio object is null");
        }
    } catch (e) {
        // ignore error
        //alert("setCheckedRadioValue error: " + e);
    }
    return false;
};

// ----------------------------------------------------------------------------

/**
*** Returns an icon URL based on the specified battery level
*** Battery level should be specified as a %, either 0.0<%<1.0, or 1<=%<=100
*** @param e  The 'MapEventRecord' object
**/
function imageGetBatterLevelURL(p)
{
    if (p <= 0) {
        return "images/Batt000.png";
    }
    if (p < 1.0) { p *= 100.00; } // convert 0.25 to 25.0
    if (p < 26) {
        return "images/Batt025.png";
    } else
    if (p < 51) {
        return "images/Batt050.png";
    } else
    if (p < 71) {
        return "images/Batt070.png";
    } else
    if (p < 91) {
        return "images/Batt090.png";
    } else {
        return "images/Batt100.png";
    }
};

// ----------------------------------------------------------------------------

/**
*** Attempt to play a sound
*** @param spanElemName  The name of the SPAN field into which the sound HTML is written
*** @param soundURL      The sound file URL
*** @param loop          True to repeat the sound file
**/
function playSound(spanElemName, soundURL, loop)
{
    // <span id="spanElemName"></span>
    var spanElem = document.getElementById(spanElemName);
    if (spanElem != null) {
        spanElem.innerHTML = "<embed src='"+soundURL+"' hidden='true' autostart='true' loop='"+loop+"'>";
    }
    // Also:
    //  - http://www.scriptwell.net/howtoplaysound.htm
}

// ---
