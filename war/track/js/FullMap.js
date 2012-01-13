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
//  2011/10/03  Martin D. Flynn
//     -Cloned from TrackMap.js
// ----------------------------------------------------------------------------

var PAGE_ZONEGEOCODE    = "ZONEGEOCODE";
var TAG_geocode         = "geocode";
var TAG_geonames        = "geonames";
var TAG_code            = "code";
var TAG_geoname         = "geoname";
var TAG_lat             = "lat";
var TAG_lng             = "lng";
var TAG_lon             = "lon";

// ----------------------------------------------------------------------------

var _selectedDG = null;

function fmSetSelectedDG(item)
{
    if (IS_FLEET) {
        _selectedDG = item;
    } else {
        _selectedDG = item;
    }
};
function fmGetSelectedDG()
{
    return IS_FLEET? _selectedDG : _selectedDG;
};

// ----------------------------------------------------------------------------

var _dateFrom = "";
function fmSetDateFrom(df)
{
    _dateFrom = df;
};
function fmGetDateFrom()
{
    return _dateFrom;
};

// ----------------------------------------------------------------------------

var _dateTo = "";
function fmSetDateTo(dt)
{
    _dateTo = dt;
};
function fmGetDateTo()
{
    return _dateTo;
};

// ----------------------------------------------------------------------------

var _timezone = "";
function fmSetTimeZone(tz)
{
    _timezone = tz;
};
function fmGetTimeZone()
{
    return _timezone;
};

// ----------------------------------------------------------------------------

var fmLimitOverride = -1;

/* return the event 'limit' for non-fleet maps */
function fmEventLimit()
{
    return IS_FLEET? -1 : fmLimitOverride;
};

// ----------------------------------------------------------------------------

/* this is executed when the page is loaded */
function fmOnLoad()
{

    /* init AutoUpdate button text */
    var btn = document.getElementById(ID_MAP_AUTOUPDATE_BTN);
    if (btn != null) { btn.value = AutoUpdateEnable? TEXT_autoUpdateStart : "??"; }

    /* update map */
    if (AutoUpdateOnLoad) {
        // start AutoUpdate (first RecenterMode should be RECENTER_ZOOM)
        var savedRecenterMode = AutoUpdateRecenterMode;
        AutoUpdateRecenterMode = jsmRecenterZoomMode(RECENTER_ZOOM);
        //trackMapClickedAutoUpdate();
        startAutoUpdateMapTimer();
        AutoUpdateRecenterMode = savedRecenterMode;
    } else {
        // update map points
        var limit = ((MapUpdateOnLoad == "last") && !IS_FLEET)? 1 : fmEventLimit();
        fmUpdateMap(limit, fmLimitType(), jsmRecenterZoomMode(RECENTER_ZOOM), 0);
    }

};

/* this is executed when the page is unloaded */
function fmOnUnload()
{
    mapProviderUnload();
};

// ----------------------------------------------------------------------------

/* this is executed when the "Update Map"/"Auto Update" is pressed */
function fmSendDeviceCommand(devID, devCMD) 
{
    if (!IS_FLEET && mapDevicePing && DEVICE_PING_URL && (DEVICE_PING_URL != "")) { 
        var dev = devID; // PARM_DEVICE
        var rfr = ""; // date from
        var rto = ""; // date to
        var tmz = ""; // timezone
        var url = DEVICE_PING_URL + 
            "&_uniq=" + Math.random() +  // necessary to make the URL unique
            "&" + PARM_DEVICE_GROUP + "=" + strEncode(dev) +    
            "&" + PARM_DEVICE_COMMAND + "=" + strEncode(devCMD?devCMD:"");
        mapDevicePing(url); // valid iff 'jsmap.js' is in use!
    }
};

// ----------------------------------------------------------------------------

/* get limit type first/last */
function fmLimitType()
{
    return LimitType;
};

/* this is executed when the "Update All" is clicked */
function fmClickedUpdateAll() // all
{
    if (AutoUpdateOnLoad && (AutoUpdateMapTimer != null)) {
        startAutoUpdateMapTimer();
    } else {
        stopAutoUpdateMapTimer();
        fmUpdateMap(fmEventLimit(), fmLimitType(), jsmRecenterZoomMode(RECENTER_ZOOM), 0);
    }
};

/* this is executed when the "Update Last" is clicked */
function fmClickedUpdateLast() // last
{
    stopAutoUpdateMapTimer();
    fmUpdateMap(1, "last", jsmRecenterZoomMode(RECENTER_ZOOM), 0);
};

/* this is executed when the "Replay Map" is pressed */
function fmClickedReplay(showInfoBox) 
{
    //try { document.getElementById(ID_CENTER_LAST_POINT_FORM).centerLastPoint.checked = false; } catch (e) {}
    stopAutoUpdateMapTimer();
    var replay = showInfoBox? 2 : 1; // 1=don't show info box, 2=show info box (if enabled)
    var replayState = mapProviderPauseReplay(replay);
    if (replayState > 0) {
        // replay paused/resumed
    } else {
        // (re)starting replay
        fmUpdateMap(fmEventLimit(), fmLimitType(), jsmRecenterZoomMode(RECENTER_ZOOM), replay);
    }
};

/* this is executed when "Update Map" is clicked */
function fmUpdateMap(limit, limitType, recenterMode, replay) 
{
    var limitFirst = false;
    var dev = fmGetSelectedDG();
    var rfr = fmGetDateFrom();
    var rto = fmGetDateTo();
    var tmz = fmGetTimeZone();
    var url = MAP_UPDATE_URL + 
        "&_uniq=" + Math.random() +  // necessary to make the URL unique
        "&" + PARM_RANGE_FR + "=" + strEncode(rfr) +
        "&" + PARM_RANGE_TO + "=" + strEncode(rto) +
        "&" + PARM_TIMEZONE + "=" + strEncode(tmz) +
        "&" + PARM_DEVICE_GROUP + "=" + strEncode(dev);
    if (limit > 0) {
        url += "&" + PARM_LIMIT + "=" + limit;
    }
    if (limitType != "") {
        url += "&" + PARM_LIMIT_TYPE + "=" + limitType;
    }
    mapProviderUpdateMap(url, recenterMode, replay);
};

// ----------------------------------------------------------------------------

/* auto-update map (toggle) */
function fmClickedAutoUpdate()
{
    if (AutoUpdateMapTimer != null) {
        stopAutoUpdateMapTimer();
    } else {
        startAutoUpdateMapTimer();
    }
};

/* sets the AutoUpdateMap button text */
var _autoUpdateButtonElem = null;
function _setAutoUpdateButtonText(text)
{
    if (_autoUpdateButtonElem == null) {
        _autoUpdateButtonElem = document.getElementById(ID_MAP_AUTOUPDATE_BTN);
    }
    if (_autoUpdateButtonElem != null) { 
        _autoUpdateButtonElem.value = text; 
    }
};

/* periodic map update timer target */
function _timerAutoUpdateMap() 
{
    if (--AutoIntervalCount <= 0) {
        var limit = ((MapUpdateOnLoad == "last") && !IS_FLEET)? 1 : fmEventLimit();
        fmUpdateMap(limit, "last", jsmRecenterZoomMode(AutoUpdateRecenterMode), 0);
        if ((AutoMaxCount > 0) && (++AutoUpdateMapCount >= AutoMaxCount)) {
            // we've reached the maximum number of allowed updates.
            stopAutoUpdateMapTimer();
            return;
        }
        AutoIntervalCount = AutoInterval; // start over
    }
    _setAutoUpdateButtonText(TEXT_autoUpdateStop + ' : ' + AutoIntervalCount);
};

/* start a map auto-update timer */
function startAutoUpdateMapTimer() 
{
    stopAutoUpdateMapTimer();
    var limit = ((MapUpdateOnLoad == "last") && !IS_FLEET)? 1 : fmEventLimit(); // single last point, or all points
    fmUpdateMap(limit, "last", jsmRecenterZoomMode(AutoUpdateRecenterMode), 0); // update map now
    AutoIntervalCount  = AutoInterval;
    AutoUpdateMapCount = 0;
    _setAutoUpdateButtonText(TEXT_autoUpdateStop);
    AutoUpdateMapTimer = setInterval('_timerAutoUpdateMap()',1000); // setTimeout
};

/* stop any running map auto-update timer */
function stopAutoUpdateMapTimer()
{
    if (AutoUpdateMapTimer != null) {
        clearInterval(AutoUpdateMapTimer); // clearTimeout
        AutoUpdateMapTimer = null;
    }
    AutoIntervalCount  = 0;
    AutoUpdateMapCount = 0;
    _setAutoUpdateButtonText(TEXT_autoUpdateStart);
};

// ----------------------------------------------------------------------------

function fmGotoAddress(addr)
{
    var ct = "US";

    /* get the latitude/longitude for the zip */
    var url = "./Track?page=" + PAGE_ZONEGEOCODE + "&addr=" + addr + "&country=" + ct + "&_uniq=" + Math.random();
    //alert("URL " + url);
    try {
        var req = jsmGetXMLHttpRequest();
        if (req) {
            req.open("GET", url, true);
            //req.setRequestHeader("CACHE-CONTROL", "NO-CACHE");
            //req.setRequestHeader("PRAGMA", "NO-CACHE");
            //req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    var lat = 0.0;
                    var lon = 0.0;
                    for (;;) {

                        /* get xml */
                        var xmlStr = req.responseText;
                        if (!xmlStr || (xmlStr == "")) {
                            break;
                        }

                        /* get XML doc */
                        var xmlDoc = createXMLDocument(xmlStr);
                        if (xmlDoc == null) {
                            break;
                        }

                        /* try parsing as "geocode" encasulated XML */
                        var geocode = xmlDoc.getElementsByTagName(TAG_geocode);
                        if ((geocode != null) && (geocode.length > 0)) {
                            //alert("geocode: " + xmlStr);
                            var geocodeElem = geocode[0];
                            if (geocodeElem != null) {
                                var latn = geocodeElem.getElementsByTagName(TAG_lat);
                                var lonn = geocodeElem.getElementsByTagName(TAG_lng);
                                if (!lonn || (lonn.length == 0)) { lonn = geocodeElem.getElementsByTagName(TAG_lon); }
                                if ((latn.length > 0) && (lonn.length > 0)) {
                                    lat = numParseFloat(latn[0].childNodes[0].nodeValue,0.0);
                                    lon = numParseFloat(lonn[0].childNodes[0].nodeValue,0.0);
                                    break;
                                }
                            }
                            break;
                        }

                        /* try parsing as forwarded XML from Geonames */
                        var geonames = xmlDoc.getElementsByTagName(TAG_geonames);
                        if ((geonames != null) && (geonames.length > 0)) {
                            //alert("geonames: " + xmlStr);
                            // returned XML was forwarded as-is from Geonames
                            var geonamesElem = geonames[0];
                            var codeList = null;
                            if (geonamesElem != null) {
                                codeList = geonamesElem.getElementsByTagName(TAG_code);
                                if (!codeList || (codeList.length == 0)) {
                                    codeList = geonamesElem.getElementsByTagName(TAG_geoname);
                                }
                            }
                            if (codeList != null) {
                                for (var i = 0; i < codeList.length; i++) {
                                    var code = codeList[i];
                                    var latn = code.getElementsByTagName(TAG_lat);
                                    var lonn = code.getElementsByTagName(TAG_lng);
                                    if ((latn.length > 0) && (lonn.length > 0)) {
                                        lat = numParseFloat(latn[0].childNodes[0].nodeValue,0.0);
                                        lon = numParseFloat(lonn[0].childNodes[0].nodeValue,0.0);
                                        break;
                                    }
                                }
                            }
                            break;
                        }

                        /* break */
                        //alert("unknown: " + xmlStr);
                        break;

                    }

                    /* set lat/lon */
                    if ((lat != 0.0) || (lon != 0.0)) {
                        // got lat/lon
                        //alert("Lat="+lat +", Lon="+lon);
                        //jsmSetCenter(lat,lon,PUSHPIN_ZOOM);
                        jsmDrawShape("!");
                        jsmDrawShape("circle", 200, [ new JSMapPoint(lat,lon) ], "#888855", true);
                    }

                } else
                if (req.readyState == 1) {
                    // alert('Loading GeoNames from URL: [' + req.readyState + ']\n' + url);
                } else {
                    // alert('Problem loading URL? [' + req.readyState + ']\n' + url);
                }
            }
            req.send(null);
        } else {
            alert("Error [fmGotoAddress]:\n" + url);
        }
    } catch (e) {
        alert("Error [fmGotoAddress]:\n" + e);
    }

};

// ----------------------------------------------------------------------------
