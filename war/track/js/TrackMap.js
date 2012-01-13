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
//  2008/08/17  Martin D. Flynn
//     -Moved from TrackMap.java
//  2008/09/19  Martin D. Flynn
//     -Added support for updating map with "Last" point.
//     -Added support for starting "AutoUpdate" on map load.
//  2009/04/02  Martin D. Flynn
//     -Added 'trackMapEventLimit' to allow event 'limit' overrides.
//  2009/11/01  Martin D. Flynn
//     -If "AutoUpdateOnLoad" is true, clicking "Update" now just resets/restarts
//      the auto-update timer.
//  2009/11/10  Martin D. Flynn
//     -Added condition to only restart auto-update if "AutoUpdateOnLoad" is true
//      AND auto-update is currently active/in-process.
// ----------------------------------------------------------------------------

var ID_DEVICE_ID        = "deviceSelector";
var ID_DEVICE_DESCR     = "deviceDescription";
var ID_GOTO_ADDRESS     = "gotoAddressFld";

var PAGE_ZONEGEOCODE    = "ZONEGEOCODE";
var TAG_geocode         = "geocode";
var TAG_geonames        = "geonames";
var TAG_code            = "code";
var TAG_geoname         = "geoname";
var TAG_lat             = "lat";
var TAG_lng             = "lng";
var TAG_lon             = "lon";

// ----------------------------------------------------------------------------

var trackMapLimitOverride = -1;

/* return the event 'limit' for non-fleet maps */
function trackMapEventLimit()
{
    return IS_FLEET? -1 : trackMapLimitOverride;
}

// ----------------------------------------------------------------------------

/* this is executed when the page is loaded */
function trackMapOnLoad()
{

    /* display calendars */
    if (mapCal_fr) {
        mapCal_fr.setCollapsible(CalendarCollapsible, CalendarFade, CalendarDivBox);
        mapCal_to.setCollapsible(CalendarCollapsible, CalendarFade, CalendarDivBox);
        calWriteCalendars(mapCal_fr, mapCal_to);
        if ((CalendarDateOnLoad == "last") && jsvLastEventYMD && (jsvLastEventEpoch > 0)) {
            mapCal_to.setDate(jsvLastEventYMD.YYYY, jsvLastEventYMD.MM, jsvLastEventYMD.DD);
            mapCal_fr.setDate(jsvLastEventYMD.YYYY, jsvLastEventYMD.MM, jsvLastEventYMD.DD);
        }
    } else {
        mapCal_to.writeCalendar();
        if ((CalendarDateOnLoad == "last") && jsvTodayYMD) {
            mapCal_to.setDate(jsvTodayYMD.YYYY, jsvTodayYMD.MM, jsvTodayYMD.DD);
        }
    }

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
        var limit = ((MapUpdateOnLoad == "last") && !IS_FLEET)? 1 : trackMapEventLimit();
        trackMapUpdateMap(limit, trackMapLimitType(), jsmRecenterZoomMode(RECENTER_ZOOM), 0);
    }

}

/* this is executed when the page is unloaded */
function trackMapOnUnload()
{
    mapProviderUnload();
}

// ----------------------------------------------------------------------------

/* this is executed when a new device is selected */
function trackMapSelectDevice()
{ // 'PARM_PAGE=map' && PARM_DEVICE are defined in FORM_NEW_DEVICE
    document.SelectDeviceForm.date_fr.value = mapCal_fr? mapCal_fr.getArgDateTime() : ""; // PARM_RANGE_FR
    document.SelectDeviceForm.date_to.value = mapCal_to.getArgDateTime(); // PARM_RANGE_TO
    document.SelectDeviceForm.date_tz.value = calGetTimeZone(); // PARM_TIMEZONE
    document.SelectDeviceForm.submit();
}

// ----------------------------------------------------------------------------

/* this is executed when a new region is selected */
function trackMapSelectRegion()
{

    /* remove current shapes */
    jsmDrawShape("!");

    /* find zoomRegion */
    var zoomRegion = null;
    if (trackZoomRegionShapes) {
        var zrName = document.SelectDeviceForm.regionSelector.value;
        if ((zrName != "") && (zrName != "normal")) {
            for (var i = 0; i < trackZoomRegionShapes.length; i++) {
                if (zrName == trackZoomRegionShapes[i].name) {
                    zoomRegion = trackZoomRegionShapes[i];
                    break;
                }
            }
        }
    }
    if (zoomRegion == null) {
        // selected zoom region not found
        jsmSetFixedZoom(false);
        trackMapClickedUpdateAll();
        return;
    }
    
    /* attributes */
    var color = zoomRegion.color;
    var type  = zoomRegion.type;   // numParseFloat(fld[0], 0);
    var radM  = zoomRegion.radius; // numParseFloat(fld[1], 0);

    // ZoomRegion value:
    // <type>,<radius>,<lat>/<lon>,<lat>/<lon>
    var fld = zoomRegion.points.split(',');
    //if (fld.length < 3) { return; }
    //var type  = numParseFloat(fld[0], 0);
    //var radM  = numParseFloat(fld[1], 0);
    var jspt  = [];
    for (var i = 0; i < fld.length; i++) {
        var LL = fld[i].split('/');
        if (LL.length < 2) { continue; }
        var lat = numParseFloat(LL[0], 0);
        var lon = numParseFloat(LL[1], 0);
        if (((lat != 0) || (lon != 0))) {
            jspt.push(new JSMapPoint(lat,lon));
        }
    }
    if (jspt.length == 0) {
        jsmSetFixedZoom(false);
        trackMapClickedUpdateAll();
        return;
    }
    
    /* legacy zoom mode */
    if ((type == "zoom") || (type == "!zoom")) {
        jsmSetCenter(jspt[0].lat, jspt[0].lon, radM);
        jsmSetFixedZoom(true);
        return;
    }

    /* draw shape */
    if (jsmDrawShape(type, radM, jspt, color, true)) {
        jsmSetFixedZoom(true);
        return;
    }
    
    /* not supported */
    jsmSetFixedZoom(false);

}

// ----------------------------------------------------------------------------

/* reset the calendar dates to the last known device date */
function _resetCalandarDates()
{
    if (!IS_FLEET && jsvLastEventYMD && (jsvLastEventEpoch > 0)) {
        if (mapCal_to) {
            var day = jsvLastEventYMD.DD + 1;  // next day
            mapCal_to.setDate(jsvLastEventYMD.YYYY, jsvLastEventYMD.MM, day);
        }
        if (mapCal_fr) { 
            var day = jsvLastEventYMD.DD;  // this day
            mapCal_fr.setDate(jsvLastEventYMD.YYYY, jsvLastEventYMD.MM, day); 
        }
    } else
    if (jsvTodayYMD) {
        if (mapCal_to) {
            var day = jsvTodayYMD.DD + 1;  // next day
            mapCal_to.setDate(jsvTodayYMD.YYYY, jsvTodayYMD.MM, day);
        }
        if (mapCal_fr) { 
            var day = jsvTodayYMD.DD;  // this day
            mapCal_fr.setDate(jsvTodayYMD.YYYY, jsvTodayYMD.MM, day); 
        }
    } else {
        // as a last resort, we could rely on the 'Date' returned by the browser
        // (which may be inaccurate), but for now just leave the calendars as-is.
    }
}

/* this is executed when the "Latest Event" is selected */
function trackMapGotoLastEventDate()
{
    _resetCalandarDates();
    trackMapClickedUpdateAll();
}

// ----------------------------------------------------------------------------

/* this is executed when the "Update Map"/"Auto Update" is pressed */
function trackMapPingDevice(devcmd) 
{
    if (!IS_FLEET && mapDevicePing && DEVICE_PING_URL && (DEVICE_PING_URL != "")) { 
        var dev = document.SelectDeviceForm.device.value; // PARM_DEVICE
        var rfr = mapCal_fr.getArgDateTime();
        var rto = mapCal_to.getArgDateTime();
        var tmz = calGetTimeZone();
        var url = DEVICE_PING_URL + 
            "&_uniq=" + Math.random() +  // necessary to make the URL unique
            "&" + PARM_RANGE_FR + "=" + strEncode(rfr) +        // necessary?
            "&" + PARM_RANGE_TO + "=" + strEncode(rto) +        // necessary?
            "&" + PARM_TIMEZONE + "=" + strEncode(tmz) +        // necessary?
            "&" + PARM_DEVICE_GROUP + "=" + strEncode(dev) +    
            "&" + PARM_DEVICE_COMMAND + "=" + strEncode(devcmd?devcmd:"");
        mapDevicePing(url); // valid iff 'jsmap.js' is in use!
    }
}

// ----------------------------------------------------------------------------

/* get limit type first/last */
function trackMapLimitType()
{
    return LimitType;
}

/* this is executed when the "Update All" is pressed */
function trackMapClickedUpdateAll() // all
{
    if (AutoUpdateOnLoad && (AutoUpdateMapTimer != null)) {
        startAutoUpdateMapTimer();
    } else {
        stopAutoUpdateMapTimer();
        trackMapUpdateMap(trackMapEventLimit(), trackMapLimitType(), jsmRecenterZoomMode(RECENTER_ZOOM), 0);
    }
}

/* this is executed when the "Update Last" is pressed */
function trackMapClickedUpdateLast() // last
{
    stopAutoUpdateMapTimer();
    trackMapUpdateMap(1, "last", jsmRecenterZoomMode(RECENTER_ZOOM), 0);
}

/* this is executed when the "Replay Map" is pressed */
function trackMapClickedReplay(showInfoBox) 
{
    //try { document.getElementById(ID_CENTER_LAST_POINT_FORM).centerLastPoint.checked = false; } catch (e) {}
    stopAutoUpdateMapTimer();
    var replay = showInfoBox? 2 : 1; // 1=don't show info box, 2=show info box (if enabled)
    var replayState = mapProviderPauseReplay(replay);
    if (replayState > 0) {
        // replay paused/resumed
    } else {
        // (re)starting replay
        trackMapUpdateMap(trackMapEventLimit(), trackMapLimitType(), jsmRecenterZoomMode(RECENTER_ZOOM), replay);
    }
}

/* this is executed when "Update Map" is clicked */
function trackMapUpdateMap(limit, limitType, recenterMode, replay) 
{
    var limitFirst = false;
    //try { document.getElementById(ID_CENTER_LAST_POINT_FORM).centerLastPoint.checked = false; } catch (e) {}
    var dev = IS_FLEET? document.SelectDeviceForm.group.value : document.SelectDeviceForm.device.value; // PARM_GEOUP/PARM_DEVICE
    var rfr = mapCal_fr? mapCal_fr.getArgDateTime() : "";
    var rto = mapCal_to.getArgDateTime();
    var tmz = calGetTimeZone();
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
    // "Replay" button ID is ID_MAP_REPLAY_BTN
}

/* this is executed when "Update KML" is clicked */
function trackMapUpdateKML() 
{
    if (KML_UPDATE_URL && (KML_UPDATE_URL != "")) {
        var dev = IS_FLEET? document.SelectDeviceForm.group.value : document.SelectDeviceForm.device.value; // PARM_GEOUP/PARM_DEVICE
        var rfr = mapCal_fr? mapCal_fr.getArgDateTime() : "";
        var rto = mapCal_to.getArgDateTime();
        var tmz = calGetTimeZone();
        var url = KML_UPDATE_URL +  // Google KML
            "&_uniq=" + Math.random() +  // necessary to make the URL unique
            "&" + PARM_RANGE_FR + "=" + strEncode(rfr) +
            "&" + PARM_RANGE_TO + "=" + strEncode(rto) +
            "&" + PARM_TIMEZONE + "=" + strEncode(tmz) +
            "&" + PARM_DEVICE_GROUP + "=" + strEncode(dev);
        openURL(url, "_blank");
    }
}

/* this is executed when the "Last Location" is pressed */
//function trackMapClickedCenterOnLast()
//{
//    stopAutoUpdateMapTimer();
//    try { jsmCenterOnLastPushpin(document.getElementById(ID_CENTER_LAST_POINT_FORM).centerLastPoint.checked); } catch (e) {/*ignore*/}
//}

// ----------------------------------------------------------------------------

/* auto-update map (toggle) */
function trackMapClickedAutoUpdate()
{
    if (AutoUpdateMapTimer != null) {
        stopAutoUpdateMapTimer();
    } else {
        startAutoUpdateMapTimer();
    }
}

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
}

/* periodic map update timer target */
function _timerAutoUpdateMap() 
{
    if (--AutoIntervalCount <= 0) {
        _resetCalandarDates();
        var limit = ((MapUpdateOnLoad == "last") && !IS_FLEET)? 1 : trackMapEventLimit();
        trackMapUpdateMap(limit, "last", jsmRecenterZoomMode(AutoUpdateRecenterMode), 0);
        if ((AutoMaxCount > 0) && (++AutoUpdateMapCount >= AutoMaxCount)) {
            // we've reached the maximum number of allowed updates.
            stopAutoUpdateMapTimer();
            return;
        }
        AutoIntervalCount = AutoInterval; // start over
    }
    _setAutoUpdateButtonText(TEXT_autoUpdateStop + ' : ' + AutoIntervalCount);
}

/* start a map auto-update timer */
function startAutoUpdateMapTimer() 
{
    stopAutoUpdateMapTimer();
    _resetCalandarDates();
    var limit = ((MapUpdateOnLoad == "last") && !IS_FLEET)? 1 : trackMapEventLimit(); // single last point, or all points
    trackMapUpdateMap(limit, "last", jsmRecenterZoomMode(AutoUpdateRecenterMode), 0); // update map now
    AutoIntervalCount  = AutoInterval;
    AutoUpdateMapCount = 0;
    _setAutoUpdateButtonText(TEXT_autoUpdateStop);
    AutoUpdateMapTimer = setInterval('_timerAutoUpdateMap()',1000); // setTimeout
}

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
}

// ----------------------------------------------------------------------------

/* show device selector */
// only valid if DeviceChooser has been included
function trackMapShowSelector()
{
    if (deviceShowChooserList) {
        var list = (typeof TrackSelectorList != 'undefined')? TrackSelectorList : null;
        deviceShowChooserList(ID_DEVICE_ID, ID_DEVICE_DESCR, list); 
    }
}

function deviceDeviceChanged()
{
    trackMapSelectDevice();
}

// ----------------------------------------------------------------------------

function trackMapGotoAddress()
{
    var addrFld = document.getElementById(ID_GOTO_ADDRESS);
    if (addrFld == null) { return; }
    var addr = addrFld.value;
    var ct   = "US";

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
            alert("Error [trackMapGotoAddress]:\n" + url);
        }
    } catch (e) {
        alert("Error [trackMapGotoAddress]:\n" + e);
    }

}

// ----------------------------------------------------------------------------
