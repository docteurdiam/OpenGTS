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
// Required funtions defined by this module:
//   new JSMap(String mapID)
//   JSClearLayers()
//   JSSetCenter(JSMapPoint center [, int zoom])
//   JSDrawPushpins(JSMapPushpin pushPin[], int recenterMode, int replay)
//   JSDrawPOI(JSMapPushpin pushPin[])
//   JSDrawRoute(JSMapPoint points[], String color)
//   JSDrawShape(String type, double radius, JSMapPoint points[], String color, boolean zoomTo)
//   JSDrawGeozone(int type, double radius, JSMapPoint points[], String color, int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin, boolean center)
//   JSPauseReplay(int replay)
//   JSUnload() 
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/08/08  Martin D. Flynn
//     -Added support for Geozones
//  2008/09/01  Martin D. Flynn
//     -Added replay and geozone recenter support
//  2009/08/23  Martin D. Flynn
//     -Added color argument to JSDrawRoute
//     -Added option for drawing multiple points per device on fleet map
//  2009/09/23  Martin D. Flynn
//     -Added support for displaying multipoint geozones (single point at a time)
//  2009/11/01  Juan Carlos Argueta
//     -Added route-arrows (see ROUTE_LINE_ARROWS)
//  2010/11/29  Martin D. Flynn
//     -Removed pushpins from non-editable polygon geozones
//  2011/01/28  Martin D. Flynn
//     -Apply minimum zoom (point range) when updating a single point on the map.
//     -Added support for setting map type from "map.view" property.
//  2011/06/16  Martin D. Flynn
//     -Miscellaneous cleanup/changes.
// ----------------------------------------------------------------------------

var DRAG_NONE               = 0;
var DRAG_RULER              = 1;
var DRAG_GEOZONE_CENTER     = 2;
var DRAG_GEOZONE_RADIUS     = 3;

var USE_DEFAULT_CONTROLS    = true;

// ----------------------------------------------------------------------------

/**
*** Create GMap(...)
**/
function jsNewGMap(element)
{
    var mapStyle = { 
        draggableCursor: "auto", 
        draggingCursor: "move" 
    };
    return new GMap2(element, mapStyle);
};

/**
*** Create GLatLng(...)
**/
function jsNewGLatLng(lat, lon)
{
    return new GLatLng(lat, lon);
};

/**
*** Create GLatLngBounds()
**/
function jsNewGLatLngBounds()
{
    return new GLatLngBounds();
};

/**
*** Create GSize()
**/
function jsNewGSize(W, H) 
{
    return new GSize(W, H);
};

/**
*** Create GPoint()
**/
function jsNewGPoint(X, Y) 
{
    return new GPoint(X, Y);
};

/**
*** Create GPolyline()
**/
function jsNewGPolyline(latLonList, 
    borderColor, borderWidth, borderOpacity)
{
    return new GPolyline(latLonList, borderColor, borderWidth, borderOpacity);
};

/**
*** Create GPolygon()
**/
function jsNewGPolygon(latLonList, 
    borderColor, borderWidth, borderOpacity, 
    fillColor, fillOpacity)
{
    return new GPolygon(latLonList, borderColor, borderWidth, borderOpacity, fillColor, fillOpacity);
};

/**
*** Create Pushpin Marker()
**/
function jsNewImageMarker(point, 
    image, iconSize, iconAnchor, shadow, shadowSize, infoWindowAnchor,
    draggable) 
{
    var icon = new GIcon();
    if (image )           { icon.image            = image;            }
    if (iconSize)         { icon.iconSize         = iconSize;         }
    if (iconAnchor)       { icon.iconAnchor       = iconAnchor;       }
    if (shadow)           { icon.shadow           = shadow;           }
    if (shadowSize)       { icon.shadowSize       = shadowSize;       }
    if (infoWindowAnchor) { icon.infoWindowAnchor = infoWindowAnchor; }
    var marker = new GMarker(point, { 
        icon: icon, 
        draggable: draggable 
        });
    return marker;
};

// ----------------------------------------------------------------------------

/**
*** JSMap constructor
**/
function JSMap(element)
{
    //if (navigator.platform.match(/linux|bsd/i)) { _mSvgEnabled = _mSvgForced = true; }

    /* map */
    this.gmapGoogleMap = jsNewGMap(element);
    if (USE_DEFAULT_CONTROLS) {
        this.gmapGoogleMap.setUIToDefault();
        //var customUI = this.gmapGoogleMap.getDefaultUI();
        ////customUI.controls.scalecontrol = false;
        //this.gmapGoogleMap.setUI(customUI);
    } else {
        //this.gmapGoogleMap.addMapType(G_PHYSICAL_MAP);
        //this.gmapGoogleMap.addMapType(G_SATELLITE_3D_MAP); // provided by "Harold Julian M"
        //var hierarchy = new GHierarchicalMapTypeControl();
        //hierarchy.addRelationship(G_SATELLITE_MAP, G_HYBRID_MAP, "Labels", true);
        //this.gmapGoogleMap.addControl(hierarchy);
        this.gmapGoogleMap.addControl(new GMapTypeControl(1));
        this.gmapGoogleMap.addControl(new GSmallMapControl());
    }

    /* map style */
    this._setDefaultMapStyle();

    /* scroll wheel zoom */
    this.gmapGoogleMap.disableDoubleClickZoom();
    if (SCROLL_WHEEL_ZOOM) { 
        this.gmapGoogleMap.enableScrollWheelZoom(); 
    }

    element.style.cursor = "crosshair"; // may not be effective
    var self = this;
    
    /* misc vars */
    this.visiblePopupInfoBox = null;

    /* replay vars */
    this.replayTimer = null;
    this.replayIndex = 0;
    this.replayInterval = (REPLAY_INTERVAL < 100)? 100 : REPLAY_INTERVAL;
    this.replayInProgress = false;
    this.replayPushpins = [];

    /* zone vars */
    this.geozoneCenter = null;  // JSMapPoint

    /* drawn shapes */
    this.drawShapes = [];

    /* 'mousemove' to update latitude/longitude */
    var locDisp = document.getElementById(ID_LAT_LON_DISPLAY);
    if (locDisp != null) {
        GEvent.addListener(this.gmapGoogleMap, "mousemove", function (point) {
            jsmSetLatLonDisplay(point.lat(),point.lng());
            jsmapElem.style.cursor = "crosshair";
        });
        jsmSetLatLonDisplay(0,0);
    }
    
    /* "click" */
    GEvent.addListener(this.gmapGoogleMap, "click", function (overlay, point) {
        if (point) {
            var LL = new JSMapPoint(point.lat(), point.lng());
            if (jsvGeozoneMode && jsvZoneEditable) {
                // recenter geozone
                if (jsvZoneType == ZONE_POINT_RADIUS) {
                    var CC = (this.geozoneCenter != null)? this.geozoneCenter : new JSMapPoint(0.0,0.0);
                    if (jsvZoneRadiusMeters <= 0.0              ) { jsvZoneRadiusMeters = DEFAULT_ZONE_RADIUS; }
                    if (jsvZoneRadiusMeters >  MAX_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MAX_ZONE_RADIUS_M;   }
                    if (jsvZoneRadiusMeters <  MIN_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MIN_ZONE_RADIUS_M;   }
                    if (geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon) > jsvZoneRadiusMeters) {
                        jsmSetPointZoneValue(LL.lat, LL.lon, jsvZoneRadiusMeters);
                        mapProviderParseZones(jsvZoneList);
                    }
                } else
                if (jsvZoneType == ZONE_POLYGON) {
                    // count number of valid points
                    var count = 0;
                    for (var z = 0; z < jsvZoneList.length; z++) {
                        if ((jsvZoneList[z].lat != 0.0) || (jsvZoneList[z].lon != 0.0)) {
                            count++;
                        }
                    }
                    if (count == 0) {
                        // no valid points - create default polygon
                        var radiusM = 450;
                        var crLat   = geoRadians(point.lat());  // radians
                        var crLon   = geoRadians(point.lng());  // radians
                        for (x = 0; x < jsvZoneList.length; x++) {
                            var deg   = x * (360.0 / jsvZoneList.length);
                            var radM  = radiusM / EARTH_RADIUS_METERS;
                            if ((deg == 0.0) || ((deg > 170.0) && (deg<  190.0))) { radM *= 0.8; }
                            var xrad  = geoRadians(deg); // radians
                            var rrLat = Math.asin(Math.sin(crLat) * Math.cos(radM) + Math.cos(crLat) * Math.sin(radM) * Math.cos(xrad));
                            var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(radM) * Math.cos(crLat), Math.cos(radM)-Math.sin(crLat) * Math.sin(rrLat));
                            _jsmSetPointZoneValue(x, geoDegrees(rrLat), geoDegrees(rrLon), 0);
                        }
                    } else {
                        // move valid points to new location
                        var bounds = jsNewGLatLngBounds();
                        for (var x = 0; x < jsvZoneList.length; x++) {
                            var pt = jsvZoneList[x];
                            if ((pt.lat != 0.0) || (pt.lon != 0.0)) {
                                bounds.extend(jsNewGLatLng(pt.lat, pt.lon));
                            }
                        }
                        var center   = bounds.getCenter(); // GLatLng
                        var deltaLat = point.lat() - center.lat();
                        var deltaLon = point.lng() - center.lng();
                        for (var x = 0; x < jsvZoneList.length; x++) {
                            var pt = jsvZoneList[x];
                            if ((pt.lat != 0.0) || (pt.lon != 0.0)) {
                                _jsmSetPointZoneValue(x, (pt.lat + deltaLat), (pt.lon + deltaLon), 0);
                            }
                        }
                    }
                    // parse points
                    mapProviderParseZones(jsvZoneList);
                    this.geozoneCenter = LL;
                } else
                if (jsvZoneType == ZONE_SWEPT_POINT_RADIUS) {
                    var CC = (this.geozoneCenter != null)? this.geozoneCenter : new JSMapPoint(0.0,0.0);
                    if (jsvZoneRadiusMeters <= 0.0              ) { jsvZoneRadiusMeters = DEFAULT_ZONE_RADIUS; }
                    if (jsvZoneRadiusMeters >  MAX_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MAX_ZONE_RADIUS_M;   }
                    if (jsvZoneRadiusMeters <  MIN_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MIN_ZONE_RADIUS_M;   }
                    // count number of valid points
                    var count = 0;
                    var maxDistKM = 0.0;
                    var lastPT = null;
                    for (var z = 0; z < jsvZoneList.length; z++) {
                        if ((jsvZoneList[z].lat != 0.0) || (jsvZoneList[z].lon != 0.0)) {
                            count++;
                            if (lastPT != null) {
                                var dkm = geoDistanceMeters(lastPT.lat, lastPT.lon, jsvZoneList[z].lat, jsvZoneList[z].lon);
                                if (dkm > maxDistKM) {
                                    maxDistKM = dkm;
                                }
                            } else {
                                lastPT = jsvZoneList[z]; // first valid point
                            }
                        }
                    }
                    //
                    if (geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon) > jsvZoneRadiusMeters) {
                        jsmSetPointZoneValue(LL.lat, LL.lon, jsvZoneRadiusMeters);
                        mapProviderParseZones(jsvZoneList);
                        this.geozoneCenter = LL;
                    }
                }
            }
        }
    });

    /* right-click-drag to display 'ruler' */
    this.dragRulerLatLon = null;
    this.rulerOverlay = null;
    var distDisp = document.getElementById(ID_DISTANCE_DISPLAY);
    if (distDisp != null) {
        /*
        GEvent.addListener(this.gmapGoogleMap, 'mousedown', function (e) { // "dragstart", "dragend"
            // how do I tell that the control-key has been pressed?
            if (e.ctrlKey) {
                if (self.rulerOverLay != null) {
                    self.gmapGoogleMap.removeOverlay(self.rulerOverlay);
                    self.rulerOverlay = null;
                }
                jsmSetDistanceDisplay(0);
                this.dragRulerLatLon = new JSMapPoint(point.lat(),point.lng());
            }
        });
        GEvent.addListener(this.gmapGoogleMap, 'mousemove', function (point) {
            if (self.rulerOverLay != null) {
                self.gmapGoogleMap.removeOverlay(self.rulerOverlay);
                self.rulerOverlay = null;
            }
            var ruler = [];
            ruler.push(jsNewGLatLng(this.dragRulerLatLon.lat,this.dragRulerLatLon.lon));
            ruler.push(jsNewGLatLng(point.lat(),point.lng()));
            self.rulerOverlay = jsNewGPolyline(latlon, '#FF6422', 2);
            self.gmapGoogleMap.addOverlay(jsNewGPolyline(latlon, '#FF2222', 2));
        });
        GEvent.addListener(this.gmapGoogleMap, 'mouseup', function (e) {
            self.dragRulerLatLon = null;
        });
        */
    }

};

/**
*** Set map style
**/
JSMap.prototype._setDefaultMapStyle = function()
{
    if ((DEFAULT_VIEW == "aerial") || (DEFAULT_VIEW == "satellite")) {
        this.gmapGoogleMap.setMapType(G_SATELLITE_MAP); // G_AERIAL_MAP 
    } else
    if (DEFAULT_VIEW == "hybrid") {
        this.gmapGoogleMap.setMapType(G_HYBRID_MAP); // G_AERIAL_HYBRID_MAP 
    } else {
        // normal, road
        this.gmapGoogleMap.setMapType(G_NORMAL_MAP);
    }
}

// ----------------------------------------------------------------------------

/**
*** Unload/release resources
**/
JSMap.prototype.JSUnload = function()
{
    GUnload();
};

// ----------------------------------------------------------------------------

/**
*** Clear all pushpins and drawn lines
**/
JSMap.prototype.JSClearLayers = function()
{

    /* clear all overlays */
    try { this.gmapGoogleMap.clearOverlays(); } catch (e) {}

    /* reset state */
    this._clearReplay();
    this.centerBounds = jsNewGLatLngBounds();

    /* redraw shapes? */
    /* (not sure why I'm redrawing the shapes here - redrawing these shapes causes
    ** multiple geozones to be displayed on top of each other (darkening the image)
    if (this.drawShapes) {
        for (var s = 0; s < this.drawShapes.length; s++) {
            this.gmapGoogleMap.addOverlay(this.drawShapes[s]);
        }
    }
    */

};

// ----------------------------------------------------------------------------

/**
*** Pause/Resume replay
**/
JSMap.prototype.JSPauseReplay = function(replay)
{
    /* stop replay? */
    if (!replay || (replay <= 0) || !this.replayInProgress) {
        // stopping replay
        this._clearReplay();
        return REPLAY_STOPPED;
    } else {
        // replay currently in progress
        if (this.replayTimer == null) {
            // replay is "PAUSED" ... resuming replay
            this._hidePushpinPopup(this.visiblePopupInfoBox);
            jsmHighlightDetailRow(-1, false);
            this._startReplayTimer(replay, 100);
            return REPLAY_RUNNING;
        } else {
            // replaying "RUNNING" ... pausing replay
            this._stopReplayTimer();
            return REPLAY_PAUSED;
        }
    }
};

/**
*** Start the replay timer
**/
JSMap.prototype._startReplayTimer = function(replay, interval)
{
    if (this.replayInProgress) {
        this.replayTimer = setTimeout("jsmap._replayPushpins("+replay+")", interval);
    }
    jsmSetReplayState(REPLAY_RUNNING);
};

/**
*** Stop the current replay timer
**/
JSMap.prototype._stopReplayTimer = function()
{
    if (this.replayTimer != null) { 
        clearTimeout(this.replayTimer); 
        this.replayTimer = null;
    }
    jsmSetReplayState(this.replayInProgress? REPLAY_PAUSED : REPLAY_STOPPED);
};

/**
*** Clear any current replay in process
**/
JSMap.prototype._clearReplay = function()
{
    this.replayPushpins = [];
    this.replayInProgress = false;
    this._stopReplayTimer();
    this.replayIndex = 0;
    jsmHighlightDetailRow(-1, false);
};

/**
*** Gets the current replay state
**/
JSMap.prototype._getReplayState = function()
{
    if (this.replayInProgress) {
        if (this.replayTimer == null) {
            return REPLAY_PAUSED;
        } else {
            return REPLAY_RUNNING;
        }
    } else {
        return REPLAY_STOPPED;
    }
};

// ----------------------------------------------------------------------------

/**
*** Sets the center of the map
**/
JSMap.prototype.JSSetCenter = function(center, zoom)
{
    var gpt = jsNewGLatLng(center.lat, center.lon);
    if (!zoom || (zoom == 0)) {
        this.gmapGoogleMap.setCenter(gpt);
    } else
    if (zoom > 0) {
        this.gmapGoogleMap.setCenter(gpt, zoom);
    } else {
        var gb = jsNewGLatLngBounds();
        gb.extend(gpt);
        var zoom = this.gmapGoogleMap.getBoundsZoomLevel(gb);
        this.gmapGoogleMap.setCenter(gpt, zoom);
    }
};

/**
*** Draw the specified pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
*** @param recenter  True to cause the map to re-center on the drawn pushpins
**/
JSMap.prototype.JSDrawPushpins = function(pushPins, recenterMode, replay)
{

    /* clear replay (may be redundant, but repeated just to make sure) */
    this._clearReplay();

    /* make sure we have a bounding box instance */
    if (!this.centerBounds) {
        this.centerBounds = jsNewGLatLngBounds();
    }

    /* drawn pushpins */
    var drawPushpins = [];

    /* recenter map on points */
    var pointCount = 0;
    if ((pushPins != null) && (pushPins.length > 0)) {
        // extend bounding box around displayed pushpins
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            if ((pp.lat != 0.0) || (pp.lon != 0.0)) {
                pointCount++;
                this.centerBounds.extend(jsNewGLatLng(pp.lat, pp.lon));
                drawPushpins.push(pp);
            }
        }
        // MinimumMapBounds: make sure points span a minimum distance top to bottom
        var rangeRadiusM = 400; // TODO: should make this a configurable options
        var cenPt = this.centerBounds.getCenter();
        var topPt = geoRadiusPoint(cenPt.lat(), cenPt.lng(), rangeRadiusM,   0.0); // top
        this.centerBounds.extend(jsNewGLatLng(topPt.lat,topPt.lon));
        var botPt = geoRadiusPoint(cenPt.lat(), cenPt.lng(), rangeRadiusM, 180.0); // bottom
        this.centerBounds.extend(jsNewGLatLng(botPt.lat,botPt.lon));
    }
    if (recenterMode > 0) {
        try {
            if (pointCount <= 0) {
                var centerPt   = jsNewGLatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lon);
                var zoomFactor = DEFAULT_ZOOM;
                this.gmapGoogleMap.setCenter(centerPt, zoomFactor);
            } else 
            if (recenterMode == RECENTER_LAST) { // center on last point
                var pp         = drawPushpins[drawPushpins.length - 1];
                var centerPt   = jsNewGLatLng(pp.lat, pp.lon);
                this.gmapGoogleMap.setCenter(centerPt);
            } else 
            if (recenterMode == RECENTER_PAN) { // pan to last point
                var pp         = drawPushpins[drawPushpins.length - 1];
                var centerPt   = jsNewGLatLng(pp.lat, pp.lon);
                this.gmapGoogleMap.setCenter(centerPt);
            } else {
                var centerPt   = this.centerBounds.getCenter();
                var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(this.centerBounds);
                this.gmapGoogleMap.setCenter(centerPt, zoomFactor);
            }
        } catch (e) {
            //alert("Error: [JSDrawPushpins] " + e);
            return;
        }
    }
    if (pointCount <= 0) {
        return;
    }

    /* replay pushpins? */
    if (replay && (replay >= 1)) {
        this.replayIndex = 0;
        this.replayInProgress = true;
        this.replayPushpins = drawPushpins;
        this._startReplayTimer(replay, 100);
        return;
    }

    /* draw pushpins now */
    var pushpinErr = null;
    for (var i = 0; i < drawPushpins.length; i++) {
        var pp = drawPushpins[i];
        try {
            this._addPushpin(pp);
        } catch (e) {
            if (pushpinErr == null) { pushpinErr = e; }
        }
    }
    if (pushpinErr != null) {
        alert("Error: adding pushpins:\n" + pushpinErr);
    }

};

/**
*** Draw the specified PointsOfInterest pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
**/
JSMap.prototype.JSDrawPOI = function(pushPins)
{

    /* draw pushpins now */
    if ((pushPins != null) && (pushPins.length > 0)) {
        var pushpinErr = null;
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i];
            if ((pp.lat == 0.0) && (pp.lon == 0.0)) {
                continue;
            }
            try {
                this._addPushpin(pp);
            } catch (e) {
                if (pushpinErr == null) { pushpinErr = e; }
            }
        }
        if (pushpinErr != null) {
            alert("Error: adding pushpins:\n" + pushpinErr);
        }
    }

};

/**
*** Adds a single pushpin to the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._addPushpin = function(pp)
{
    try {

        /* point */
        var pt = jsNewGLatLng(pp.lat, pp.lon);

        /* background marker */
        if (pp.bgUrl) {
            var bgMarker = jsNewImageMarker(pt,
                pp.bgUrl,                                                           // image
                pp.bgSize?   jsNewGSize(pp.bgSize[0],pp.bgSize[1])      : null,     // iconSize
                pp.bgOffset? jsNewGPoint(pp.bgOffset[0],pp.bgOffset[1]) : null,     // iconAnchor
                null,                                                               // shadow
                null,                                                               // shadowSize
                jsNewGPoint(5, 1),                                                  // infoWindowAnchor
                false);                                                             // draggable
            this.gmapGoogleMap.addOverlay(bgMarker);
            pp.bgMarker = bgMarker;
        }

        /* pushpin marker */
        var ppMarker = jsNewImageMarker(pt,
            pp.iconUrl,                                                                // image
            pp.iconSize?    jsNewGSize(pp.iconSize[0],pp.iconSize[1])        : null,   // iconSize
            pp.iconHotspot? jsNewGPoint(pp.iconHotspot[0],pp.iconHotspot[1]) : null,   // iconAnchor
            pp.shadowUrl,                                                              // shadow
            pp.shadowSize?  jsNewGSize(pp.shadowSize[0],pp.shadowSize[1])    : null,   // shadowSize
            jsNewGPoint(5, 1),                                                         // infoWindowAnchor
            false);                                                                    // draggable
        GEvent.addListener(ppMarker, 'click', function() { ppMarker.openInfoWindowHtml(pp.html); });
        this.gmapGoogleMap.addOverlay(ppMarker);
        pp.marker = ppMarker;

    } catch(e) {
        //
    }
};

/**
*** Replays the list of pushpins on the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._replayPushpins = function(replay)
{

    /* advance to next valid point */
    while (true) {
        if (this.replayIndex >= this.replayPushpins.length) {
            this._clearReplay();
            jsmHighlightDetailRow(-1, false);
            return; // stop
        }
        var pp = this.replayPushpins[this.replayIndex]; // JSMapPushpin
        if ((pp.lat != 0.0) || (pp.lon != 0.0)) {
            break; // valid point
        }
        this.replayIndex++;
    }

    /* add pushpin */
    try {
        var lastNdx = this.replayIndex - 1;
        var pp = this.replayPushpins[this.replayIndex++]; // JSMapPushpin
        pp.hoverPopup = true;
        if (REPLAY_SINGLE && (lastNdx >= 0)) {
            var lastPP = this.replayPushpins[lastNdx]; // JSMapPushpin
            if (lastPP.marker) {
                this.gmapGoogleMap.removeOverlay(lastPP.marker);
            }
            if (lastPP.bgMarker) {
                this.gmapGoogleMap.removeOverlay(lastPP.bgMarker);
            }
        }
        this._addPushpin(pp);
        if (replay && (replay >= 2)) {
            this._showPushpinPopup(pp);
        } else {
            jsmHighlightDetailRow(pp.rcdNdx, true);
        }
        this._startReplayTimer(replay, this.replayInterval);
    } catch (e) {
        alert("Replay error: " + e);
    }

};

// ----------------------------------------------------------------------------

/**
*** This method should cause the info-bubble popup for the specified pushpin to display
*** @param pushPin   The JSMapPushpin object which popup its info-bubble
**/
JSMap.prototype.JSShowPushpin = function(pp, center)
{
    if (pp) {
        if (center) {
            this.JSSetCenter(new JSMapPoint(pp.lat, pp.lon));
        }
        this._showPushpinPopup(pp);
    }
};

JSMap.prototype._showPushpinPopup = function(pp)
{
    this._hidePushpinPopup(this.visiblePopupInfoBox);
    if (pp) {
        try {
            GEvent.trigger(pp.marker,"click");
        } catch (e) {
            // ignore
        }
        this.visiblePopupInfoBox = pp;
        jsmHighlightDetailRow(pp.rcdNdx, true);
    }
};

JSMap.prototype._hidePushpinPopup = function(pp)
{
    //GEvent.trigger(pp.marker,"click");
    if (pp) {
        jsmHighlightDetailRow(pp.rcdNdx, false);
    }
    if (this.visiblePopupInfoBox) {
        jsmHighlightDetailRow(this.visiblePopupInfoBox.rcdNdx, false);
        this.visiblePopupInfoBox = null;
    }
};

// ----------------------------------------------------------------------------

/**
*** Draws a line between the specified points on the map.
*** @param points   An array of JSMapPoint objects
**/
JSMap.prototype.JSDrawRoute = function(points, color)
{
    var latlon = [];
    for (var i = 0; i < points.length; i++) {
        latlon.push(jsNewGLatLng(points[i].lat,points[i].lon));
    }
    this.gmapGoogleMap.addOverlay(jsNewGPolyline(latlon, color, 2, 1.0)); // "#003399"
    if (ROUTE_LINE_ARROWS) {
        this.midArrows(latlon);
    }
};

//  [Juan Carlos Argueta] retReturns the bearing in degrees between two points.
JSMap.prototype.bearing = function(from, to) {
    // ----- Returns the bearing in degrees between two points. -----
    // ----- North = 0, East = 90, South = 180, West = 270.
    // ----- var degreesPerRadian = 180.0 / Math.PI;

    // ----- Convert to radians.
    var lat1 = from.latRadians();
    var lon1 = from.lngRadians();
    var lat2 = to.latRadians();
    var lon2 = to.lngRadians();

    // -----Compute the angle.
    var angle = - Math.atan2( Math.sin( lon1 - lon2 ) * Math.cos( lat2 ), Math.cos( lat1 ) * Math.sin( lat2 ) - Math.sin( lat1 ) * Math.cos( lat2 ) * Math.cos( lon1 - lon2 ) );
    if (angle < 0.0) { angle  += Math.PI * 2.0; }

    // ----- And convert result to degrees.
    angle = angle * (180.0 / Math.PI);
    angle = angle.toFixed(1);

    return angle;
};
       
// [Juan Carlos Argueta]  A function to create the arrow head at the end of the polyline ===
//  http://www.google.com/intl/en_ALL/mapfiles/dir_0.png
//  http://www.google.com/intl/en_ALL/mapfiles/dir_3.png
//  http://www.google.com/intl/en_ALL/mapfiles/dir_6.png
//  ...
JSMap.prototype.arrowHead = function(points) {	  
    // ----- obtain the bearing between the last two points
    if (!points || (points.length < 2)) { return; }
    var p1 = points[points.length-1];
    var p2 = points[points.length-2];
    // ----- round heading to a multiple of 3 and cast out 120s
    var dir = this.bearing(p2,p1);
    dir = Math.round(dir/3) * 3;
    while (dir >= 120) { dir -= 120; }
    // ----- use the corresponding triangle marker 
    var arrowMarker = jsNewImageMarker(p1,
        "http://www.google.com/intl/en_ALL/mapfiles/dir_"+dir+".png",   // image
        jsNewGSize(14,14),                                              // iconSize
        jsNewGPoint(7,7),                                               // iconAnchor
        null,                                                           // shadow
        jsNewGSize(1,1),                                                // shadowSize
        jsNewGPoint(0,0),                                               // infoWindowAnchor
        false                                                           // draggable
        );
    // ----- add arrow marker
    this.gmapGoogleMap.addOverlay(arrowMarker);
}
      
// [Juan Carlos Argueta]  A function to put arrow heads at intermediate points
JSMap.prototype.midArrows = function(points) {		  
    if (!points || (points.length < 2)) { return; }
    for (var i = 1; i < points.length - 1; i++) {  
        var p1 = points[i-1];
        var p2 = points[i+1];
        // ----- round it to a multiple of 3 and cast out 120s
        var dir = this.bearing(p1,p2);
        dir = Math.round(dir/3) * 3;
        while (dir >= 120) { dir -= 120; }
        // ----- use the corresponding triangle marker 
        var arrowMarker = jsNewImageMarker(points[i],
            "http://www.google.com/intl/en_ALL/mapfiles/dir_"+dir+".png",   // image
            jsNewGSize(14,14),                                              // iconSize
            jsNewGPoint(7,7),                                               // iconAnchor
            null,                                                           // shadow
            jsNewGSize(1,1),                                                // shadowSize
            jsNewGPoint(0,0),                                               // infoWindowAnchor
            false                                                           // draggable
            );
        // ----- add arrow marker
        this.gmapGoogleMap.addOverlay(arrowMarker);
    }
}

// ----------------------------------------------------------------------------

/**
*** Remove previously drawn shapes 
**/
JSMap.prototype._removeShapes = function()
{
    if (this.drawShapes) {
        for (var s = 0; s < this.drawShapes.length; s++) {
            this.gmapGoogleMap.removeOverlay(this.drawShapes[s]);
        }
    }
    this.drawShapes = [];
};

/**
*** Draws a Shape on the map at the specified location
*** @param type     The Geozone shape type ("line", "circle", "rectangle", "polygon", "center")
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param zoomTo   rue to zoom to drawn shape
*** @return True if shape was drawn, false otherwise
**/
JSMap.prototype.JSDrawShape = function(type, radiusM, verticePts, color, zoomTo)
{

    /* no type? */
    if (!type || (type == "") || (type == "!")) {
        this._removeShapes();
        return false;
    }

    /* clear existing shapes? */
    if (type.startsWith("!")) { 
        this._removeShapes();
        type = type.substr(1); 
    }

    /* no geopoints? */
    if (!verticePts || (verticePts.length == 0)) {
        return false;
    }

    /* color */
    if (!color || (color == "")) {
        color = "#0000FF";
    }

    /* zoom bounds */
    var mapBounds = zoomTo? jsNewGLatLngBounds() : null;

    /* draw shape */
    var didDrawShape = false;
    if (type == "circle") { // ZONE_POINT_RADIUS

        for (var p = 0; p < verticePts.length; p++) {
            var jsPt = verticePts[p]; // JSMapPoint
            
            /* calc circle points */
            var crPts = [];
            var crLat = geoRadians(jsPt.lat);  // radians
            var crLon = geoRadians(jsPt.lon);  // radians
            var d     = radiusM / EARTH_RADIUS_METERS;
            for (x = 0; x <= 360; x += 6) {         // 6 degrees (saves memory, & it still looks like a circle)
                var xrad  = geoRadians(x);          // radians
                var rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
                var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
                var gPt   = jsNewGLatLng(geoDegrees(rrLat),geoDegrees(rrLon));
                crPts.push(gPt);
                if (mapBounds) { mapBounds.extend(gPt); } // TODO: could stand to be optimized
            }
    
            /* draw circle */
            var crPoly = jsNewGPolygon(crPts, color, 2, 0.9, color, 0.1);
            this.gmapGoogleMap.addOverlay(crPoly);
            this.drawShapes.push(crPoly);
            didDrawShape = true;
            
        }

    } else
    if (type == "rectangle") { // ZONE_BOUNDED_RECT
        
        if (verticePts.length >= 2) {

            /* create rectangle */
            var vp0   = verticePts[0];
            var vp1   = verticePts[1];
            var TL    = jsNewGLatLng(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var TR    = jsNewGLatLng(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var BL    = jsNewGLatLng(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var BR    = jsNewGLatLng(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var crPts = [ TL, TR, BR, BL, TL ];
            if (mapBounds) { for (var b = 0; b < crPts.length; b++) { mapBounds.extend(crPts[b]); } }
    
            /* draw rectangle */
            var crPoly = jsNewGPolygon(crPts, color, 2, 0.9, color, 0.1);
            this.gmapGoogleMap.addOverlay(crPoly);
            this.drawShapes.push(crPoly);
            didDrawShape = true;

        }

    } else
    if (type == "polygon") { // ZONE_POLYGON
        
        if (verticePts.length >= 3) {

            /* accumulate polygon vertices */
            var crPts = [];
            for (var p = 0; p < verticePts.length; p++) {
                var gPt = jsNewGLatLng(verticePts[p].lat, verticePts[p].lon);
                crPts.push(gPt);
                if (mapBounds) { mapBounds.extend(gPt); }
            }
            crPts.push(crPts[0]); // close polygon

            /* draw polygon */
            var crPoly = jsNewGPolygon(crPts, color, 2, 0.9, color, 0.1);
            this.gmapGoogleMap.addOverlay(crPoly);
            this.drawShapes.push(crPoly);
            didDrawShape = true;

        }

    } else
    if (type == "corridor") { // ZONE_SWEPT_POINT_RADIUS

        // TODO: 
        
    } else
    if (type == "center") {

        if (mapBounds) {
            for (var p = 0; p < verticePts.length; p++) {
                var gPt = jsNewGLatLng(verticePts[p].lat, verticePts[p].lon);
                mapBounds.extend(gPt);
            }
            didDrawShape = true;
        }

    }

    /* center on shape */
    if (didDrawShape && zoomTo && mapBounds) {
        var centerPt   = mapBounds.getCenter(); // GLatLng
        var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(mapBounds);
        this.gmapGoogleMap.setCenter(centerPt, zoomFactor);
    }

    /* shape not supported */
    return didDrawShape;

};

// ----------------------------------------------------------------------------

var geozoneList = [];

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param primNdx  Index of point on which to center
*** @return An object representing the Circle.
**/
JSMap.prototype.JSDrawGeozone = function(type, radiusM, points, color, primNdx)
{
    // type:
    //   0 - ZONE_POINT_RADIUS
    //   1 - ZONE_BOUNDED_RECT
    //   2 - ZONE_SWEPT_POINT_RADIUS
    //   3 - ZONE_POLYGON
    // (type ZONE_POINT_RADIUS may only be currently supported)

    /* Geozone mode */
    jsvGeozoneMode = true;

    /* remove old geozone */
    for (var i = 0; i < /*Global*/geozoneList.length; i++) {
        /*Global*/geozoneList[i].remove();
    }
    /*Global*/geozoneList = [];
    this.geozoneCenter = null;

    /* no points? */
    if ((points == null) || (points.length <= 0)) {
        //alert("No Zone center!");
        return null;
    }

    /* draw geozone */
    var maxLat = -90.0;
    var minLat =  90.0;
    var pointCount = 0;
    var mapBounds  = jsNewGLatLngBounds();
    if (type == ZONE_POINT_RADIUS) {

        var zoneNdx = ((primNdx >= 0) && (primNdx < points.length))? primNdx : 0;
        var zoneCenter = points[zoneNdx]; // JSMapPoint
        if (isNaN(radiusM))              { radiusM = 5000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;
        this.geozoneCenter = zoneCenter;

        /* draw points */
        var prg = new PointRadiusGeozone(this.gmapGoogleMap, zoneCenter.lat, zoneCenter.lon, radiusM, jsvZoneColor, jsvZoneEditable);
        var cpt = jsNewGLatLng(zoneCenter.lat,zoneCenter.lon); // GLatLng
        mapBounds.extend(cpt);
        mapBounds.extend(prg.calcRadiusPoint(  0.0));
        mapBounds.extend(prg.calcRadiusPoint(180.0));
        /*Global*/geozoneList.push(prg);
        if ((zoneCenter.lat != 0.0) || (zoneCenter.lon != 0.0)) {
            pointCount = 1;
        }

        /* adjust minimum map bounds */
        // TODO:

        /* center on geozone */
        var centerPt   = mapBounds.getCenter(); // GLatLng
        var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(mapBounds);
        this.gmapGoogleMap.setCenter(centerPt, zoomFactor);

    } else
    if (type == ZONE_POLYGON) {

        /* draw points */
        // "radiusM" is not used
        //radiusM = 500; // may be used later for setting minimum map bounds
        var prg = new PolygonGeozone(this.gmapGoogleMap, points, jsvZoneColor, jsvZoneEditable)
        for (var i = 0; i < prg.verticeMarkers.length; i++) {
            var vm  = prg.verticeMarkers[i];
            var vpt = vm.getPoint(); // GLatLng
            if ((vpt.lat() != 0.0) || (vpt.lng() != 0.0)) {
                mapBounds.extend(vm.getPoint());
                pointCount++;
            }
        }
        /*Global*/geozoneList.push(prg);

    } else
    if (type == ZONE_SWEPT_POINT_RADIUS) {

        var zoneNdx = ((primNdx >= 0) && (primNdx < points.length))? primNdx : 0;
        var zoneCenter = points[zoneNdx]; // JSMapPoint
        if (isNaN(radiusM))              { radiusM = 1000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;
        this.geozoneCenter = zoneCenter;

        /* draw points */
        var prg = new CorridorGeozone(this.gmapGoogleMap, points, radiusM, jsvZoneColor, jsvZoneEditable);
        for (var i = 0; i < prg.verticeMarkers.length; i++) {
            var vm = prg.verticeMarkers[i];
            if (vm.isVisible) { // point-radius vertice
                var vpt = vm.getPoint(); // GLatLng
                if (vpt.lat() < minLat) { minLat = vpt.lat(); }
                if (vpt.lat() > maxLat) { maxLat = vpt.lat(); }
                var pt000 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM,   0.0);
                mapBounds.extend(jsNewGLatLng(pt000.lat,pt000.lon));
                var pt090 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM,  90.0);
                mapBounds.extend(jsNewGLatLng(pt090.lat,pt090.lon));
                var pt180 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM, 180.0);
                mapBounds.extend(jsNewGLatLng(pt180.lat,pt180.lon));
                var pt270 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM, 270.0);
                mapBounds.extend(jsNewGLatLng(pt270.lat,pt270.lon));
                pointCount++;
            }
        }
        /*Global*/geozoneList.push(prg);

        /* center on geozone */
        //var centerPt   = mapBounds.getCenter(); // GLatLng
        //var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(mapBounds);
        //this.gmapGoogleMap.setCenter(centerPt, zoomFactor);

    } else {

        alert("Geozone type not supported: " + type);
        
    }

    /* center on geozone */
    if (pointCount > 0) {

        // MinimumMapBounds: make sure points span a minimum distance top to bottom
        if (maxLat >= minLat) {
            var rangeRadiusM = radiusM * 3;
            var cenPt = mapBounds.getCenter();
            var topPt = geoRadiusPoint(maxLat, cenPt.lng(), rangeRadiusM,   0.0); // top
            mapBounds.extend(jsNewGLatLng(topPt.lat,topPt.lon));
            var botPt = geoRadiusPoint(minLat, cenPt.lng(), rangeRadiusM, 180.0); // bottom
            mapBounds.extend(jsNewGLatLng(botPt.lat,botPt.lon));
        }

        /* center on points */
        var centerPt   = mapBounds.getCenter(); // GLatLng
        var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(mapBounds);
        this.gmapGoogleMap.setCenter(centerPt, zoomFactor);

    } else {

        /* default center/zoom */
        var centerPt   = jsNewGLatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lon); // GLatLng
        var zoomFactor = DEFAULT_ZOOM;
        this.gmapGoogleMap.setCenter(centerPt, zoomFactor);

    }

    return null;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

function PointRadiusGeozone(gMap, lat, lon, radiusM, color, editable)
{
    var self = this;

    /* circle attributes */
    this.googleMap        = gMap;
    this.radiusMeters     = (radiusM <= MAX_ZONE_RADIUS_M)? Math.round(radiusM) : MAX_ZONE_RADIUS_M;
    this.radiusPoint      = null;
    this.centerMarker     = null;
    this.radiusMarker     = null;
    this.circlePolygon    = null;
    this.shapeColor       = (color && (color != ""))? color : "#0000FF";

    /* center Icon/marker */
    this.centerPoint      = jsNewGLatLng(lat, lon);
    this.centerMarker     = jsNewImageMarker(this.centerPoint,
        "http://labs.google.com/ridefinder/images/mm_20_blue.png",      // image
        jsNewGSize(12,20),                                              // iconSize
        jsNewGPoint(6,20),                                              // iconAnchor
        "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
        jsNewGSize(22,20),                                              // shadowSize
        null,                                                           // infoWindowAnchor
        editable                                                        // draggable
        );
    this.googleMap.addOverlay(this.centerMarker);

    /* editable? */
    if (editable) {

        /* center marker dragging */
        this.centerMarker.enableDragging();
        GEvent.addListener(this.centerMarker, "dragend", function() {
            var oldCP = self.centerPoint;
            var oldRP = self.radiusMarker.getPoint();
            var newRP = self.calcRadiusPoint(geoHeading(oldCP.lat(),oldCP.lng(),oldRP.lat(),oldRP.lng()));
            self.centerPoint = self.centerMarker.getPoint();
            self.radiusMarker.setPoint(newRP);
            self.drawCircle(); 
        });
    
        /* radius Icon/Marker */
        this.radiusPoint      = this.calcRadiusPoint(60.0);
        this.radiusMarker     = jsNewImageMarker(this.radiusPoint,
            "http://labs.google.com/ridefinder/images/mm_20_gray.png",      // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            true                                                            // draggable
            );
        this.googleMap.addOverlay(this.radiusMarker);

        /* radius marker dragging */
        this.radiusMarker.enableDragging();
        GEvent.addListener(this.radiusMarker, "dragend", function() {
            var oldCP = self.centerMarker.getPoint();
            var newRP = self.radiusMarker.getPoint();
            var radM  = Math.round(geoDistanceMeters(oldCP.lat(),oldCP.lng(),newRP.lat(),newRP.lng()));
            self.radiusMeters = radM;
            if (self.radiusMeters < MIN_ZONE_RADIUS_M) {
                self.radiusMeters = MIN_ZONE_RADIUS_M;
                newRP = self.calcRadiusPoint(geoHeading(oldCP.lat(),oldCP.lng(),newRP.lat(),newRP.lng()));
                self.radiusMarker.setPoint(newRP);
            } else
            if (self.radiusMeters > MAX_ZONE_RADIUS_M) {
                self.radiusMeters = MAX_ZONE_RADIUS_M;
                newRP = self.calcRadiusPoint(geoHeading(oldCP.lat(),oldCP.lng(),newRP.lat(),newRP.lng()));
                self.radiusMarker.setPoint(newRP);
            }
            jsvZoneRadiusMeters = self.radiusMeters;
            self.drawCircle(); 
        });

    }

    /* draw circle */
    this.drawCircle();

};

PointRadiusGeozone.prototype.type = function()
{
    return ZONE_POINT_RADIUS;
};

PointRadiusGeozone.prototype.calcRadiusPoint = function(heading)
{
    var cpt = this.centerMarker.getPoint();   // GLatLng [MUST be 'centerMarker.getPoint()' NOT 'centerPoint']
    var rp  = geoRadiusPoint(cpt.lat(), cpt.lng(), this.radiusMeters, heading);
    return jsNewGLatLng(rp.lat,  rp.lon);
};

PointRadiusGeozone.prototype.drawCircle = function()
{

    /* calc circle points */
    var points = [];
    var crLat  = geoRadians(this.centerPoint.lat());  // radians
    var crLon  = geoRadians(this.centerPoint.lng());  // radians
    var d      = this.radiusMeters / EARTH_RADIUS_METERS;
    for (x = 0; x <= 360; x += 6) {         // 6 degrees (saves memory, & it still looks like a circle)
        var xrad  = geoRadians(x);          // radians
        var rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
        var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
        var pt    = jsNewGLatLng(geoDegrees(rrLat),geoDegrees(rrLon));
        points.push(pt);
    }

    /* remove old circle */
    if (this.circlePolygon != null) {
        this.googleMap.removeOverlay(this.circlePolygon);
    }
    
    /* draw circle */
    var color = this.shapeColor;
    //this.circlePolygon = jsNewGPolyline(points, "#0000FF", 2, 0.9);
    this.circlePolygon = jsNewGPolygon(points, color, 2, 0.9, color, 0.1);
    this.googleMap.addOverlay(this.circlePolygon);
    
    /* set Geozone elements */
    jsmSetPointZoneValue(this.centerPoint.lat(), this.centerPoint.lng(), this.radiusMeters);

};

PointRadiusGeozone.prototype.getCenter = function()
{
    return this.centerPoint; // GLatLng
};

PointRadiusGeozone.prototype.getRadiusMeters = function()
{
    return this.radiusMeters;
};

PointRadiusGeozone.prototype.remove = function()
{
    if (this.radiusMarker != null) {
        this.googleMap.removeOverlay(this.radiusMarker);
    }
    if (this.centerMarker != null) {
        this.googleMap.removeOverlay(this.centerMarker);
    }
    this.googleMap.removeOverlay(this.circlePolygon);
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

function PolygonGeozone(gMap, points, color, editable)
{
    var self = this;

    /* circle attributes */
    this.googleMap        = gMap;
    this.verticeMarkers   = [];
    this.centerMarker     = null;
    this.shapeColor       = (color && (color != ""))? color : "#0000FF";
    this.centerBounds     = null;

    /* create vertices */
    var count  = 0;
    var bounds = jsNewGLatLngBounds();
    for (var i = 0; i < points.length; i++) {
        var p = points[i];

        /* vertice Icon/marker */
        var vertPoint       = jsNewGLatLng(p.lat, p.lon);
        var vertMarker      = jsNewImageMarker(vertPoint,
            "http://labs.google.com/ridefinder/images/mm_20_blue.png",      // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            editable                                                        // draggable
            );
        vertMarker.pointIndex = i;
        vertMarker.isEditable = editable;
        this.verticeMarkers.push(vertMarker);
        bounds.extend(vertPoint);
        if ((p.lat != 0.0) || (p.lon != 0.0)) {
            this.googleMap.addOverlay(vertMarker);
            vertMarker.isVisible = true;     // polygon vertice
            vertMarker.isValid   = true;
            count++;
        } else {
            vertMarker.isVisible = false;    // polygon vertice
            vertMarker.isValid   = false;
        }

    }

    /* editable? */
    if (editable) {

        /* enable vertice dragging */
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            this._enableVerticeDrag(this.verticeMarkers[i]);
        }

        /* center point */
        var center            = bounds.getCenter();
        this.centerMarker     = jsNewImageMarker(center,
            "http://labs.google.com/ridefinder/images/mm_20_red.png",       // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            editable                                                        // draggable
            );
        this.centerMarker.lastPoint = center;
        this.centerMarker.enableDragging();
        GEvent.addListener(this.centerMarker, "dragend", function() {
            var thisPoint = self.centerMarker.getPoint(); // GLatLng
            var lastPoint = self.centerMarker.lastPoint;  // GLatLng
            var deltaLat  = thisPoint.lat() - lastPoint.lat();
            var deltaLon  = thisPoint.lng() - lastPoint.lng();
            for (var i = 0; i < self.verticeMarkers.length; i++) {
                var vm  = self.verticeMarkers[i];
                var vpt = vm.getPoint();
                if ((vpt.lat() != 0.0) || (vpt.lng() != 0.0)) {
                    var npt = jsNewGLatLng(vpt.lat() + deltaLat, vpt.lng() + deltaLon);
                    vm.setPoint(npt);
                    _jsmSetPointZoneValue(vm.pointIndex, npt.lat(), npt.lng(), 0);
                } else {
                    _jsmSetPointZoneValue(vm.pointIndex, 0.0, 0.0, 0);
                }
            }
            self.centerMarker.lastPoint = thisPoint;
            self.drawPolygon();
        });
        if (count > 0) {
            this.googleMap.addOverlay(this.centerMarker);
            this.centerMarker.isVisible = true;     // polygon center
        } else {
            this.centerMarker.isVisible = false;    // polygon center
        }
        
    }
    
    /* draw polygon */
    this.drawPolygon();

};

PolygonGeozone.prototype.type = function()
{
    return ZONE_POLYGON;
};

PolygonGeozone.prototype._enableVerticeDrag = function(marker)
{
    var self = this;
    var finalMarker = marker;
    finalMarker.enableDragging();
    GEvent.addListener(finalMarker, "dragend", function() {
        var ndx   = finalMarker.pointIndex;
        var point = finalMarker.getPoint();
        _jsmSetPointZoneValue(ndx, point.lat(), point.lng(), 0);
        self.drawPolygon(); 
        self.centerBounds = jsNewGLatLngBounds();
        for (var i = 0; i < self.verticeMarkers.length; i++) {
            var vpt = self.verticeMarkers[i].getPoint();
            if ((vpt.lat() != 0.0) || (vpt.lng() != 0.0)) {
                self.centerBounds.extend(vpt);
            }
        }
        self.centerMarker.setPoint(self.centerBounds.getCenter());
        self.centerMarker.lastPoint = self.centerBounds.getCenter();
    });
};

PolygonGeozone.prototype.drawPolygon = function()
{

    /* remove old polygon */
    if (this.polygon != null) {
        this.googleMap.removeOverlay(this.polygon);
    }

    /* points */
    var points = [];
    var bounds = jsNewGLatLngBounds();
    if (this.verticeMarkers.length > 0) {
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            var vm  = this.verticeMarkers[i];
            var vpt = vm.getPoint();
            if (vm.isVisible) { // polygon vertice
                this.googleMap.removeOverlay(vm);
                vm.isVisible = false;  // polygon vertice
            }
            if ((vpt.lat() != 0.0) || (vpt.lng() != 0.0)) {
                if (vm.isEditable) {
                    this.googleMap.addOverlay(vm);
                    vm.isVisible = true;  // polygon vertice
                }
                points.push(vpt);
                bounds.extend(vpt);
            }
        }
        if (points.length > 0) {
            points.push(points[0]); // close polygon
        }
    }
    
    /* center marker */
    if (this.centerMarker != null) {
        var center = bounds.getCenter();
        if (points.length > 0) {
            this.centerMarker.setPoint(center);
            this.centerMarker.lastPoint = center;
            if (!this.centerMarker.isVisible) {  // polygon center
                this.googleMap.addOverlay(this.centerMarker);
                this.centerMarker.isVisible = true; // polygon center
            }
        } else {
            if (this.centerMarker.isVisible) { // polygon center
                this.googleMap.removeOverlay(this.centerMarker);
                this.centerMarker.isVisible = false; // polygon center
            }
        }
    }

    /* draw polygon */
    var color = this.shapeColor;
    this.polygon = jsNewGPolygon(points, color, 2, 0.9, color, 0.1);
    this.googleMap.addOverlay(this.polygon);

};

PolygonGeozone.prototype.remove = function()
{
    if (this.centerMarker && this.centerMarker.isVisible) { // polygon center
        this.googleMap.removeOverlay(this.centerMarker);
    }
    if (this.verticeMarkers != null) {
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            if (this.verticeMarkers[i].isVisible) { // polygon vertice
                this.googleMap.removeOverlay(this.verticeMarkers[i]);
            }
        }
    }
    if (this.polygon != null) {
        this.googleMap.removeOverlay(this.polygon);
    }
};

PointRadiusGeozone.prototype.getCenter = function()
{
    return this.centerMarker.getPoint(); // GLatLng
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

function CorridorGeozone(gMap, points, radiusM, color, editable)
{
    var self = this;

    /* circle attributes */
    this.googleMap        = gMap;
    this.radiusMeters     = (radiusM <= MAX_ZONE_RADIUS_M)? Math.round(radiusM) : MAX_ZONE_RADIUS_M;
    this.verticeMarkers   = [];
    this.shapeColor       = (color && (color != ""))? color : "#0000FF";
    this.corridor         = [];

    /* create vertices */
    var count  = 0;
    var bounds = jsNewGLatLngBounds();
    for (var i = 0; i < points.length; i++) {
        var p = points[i]; // JSMapPoint
        if ((p.lat == 0.0) && (p.lon == 0.0)) { continue; }

        /* vertice Icon/marker */
        var vertPoint       = jsNewGLatLng(p.lat, p.lon);
        var vertMarker      = jsNewImageMarker(vertPoint,
            "http://labs.google.com/ridefinder/images/mm_20_blue.png",      // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            editable                                                        // draggable
            );
        vertMarker.pointIndex = i;
        this.verticeMarkers.push(vertMarker);
        bounds.extend(vertPoint);
        if ((p.lat != 0.0) || (p.lon != 0.0)) {
            this.googleMap.addOverlay(vertMarker);
            vertMarker.isVisible = true; // corridor vertice
            vertMarker.isValid   = true;
            count++;
        } else {
            vertMarker.isVisible = false; // corridor vertice
            vertMarker.isValid   = false;
        }

    }

    /* editable? */
    if (editable) {

        /* enable vertice dragging */
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            this._enableVerticeDrag(this.verticeMarkers[i]);
        }
        
    }
    
    /* draw corridor */
    this.drawCorridor();

};

CorridorGeozone.prototype.type = function()
{
    return ZONE_SWEPT_POINT_RADIUS;
};

CorridorGeozone.prototype._enableVerticeDrag = function(marker)
{
    var self = this;
    var finalMarker = marker;
    finalMarker.enableDragging();
    GEvent.addListener(finalMarker, "dragend", function() {
        var ndx   = finalMarker.pointIndex;
        var point = finalMarker.getPoint(); // GLatLng
        _jsmSetPointZoneValue(ndx, point.lat(), point.lng(), self.radiusMeters);
        self.drawCorridor(); 
        /*
        self.centerBounds = jsNewGLatLngBounds();
        for (var i = 0; i < self.verticeMarkers.length; i++) {
            var vpt = self.verticeMarkers[i].getPoint();
            if ((vpt.lat() != 0.0) || (vpt.lng() != 0.0)) {
                self.centerBounds.extend(vpt);
            }
        }
        */
        //self.centerMarker.setPoint(self.centerBounds.getCenter());
        //self.centerMarker.lastPoint = self.centerBounds.getCenter();
    });
};

CorridorGeozone.prototype.drawCorridor = function()
{

    /* remove old corridor */
    if (this.corridor != null) {
        for (var i = 0; i < this.corridor.length; i++) {
            this.googleMap.removeOverlay(this.corridor[i])
        };
    }
    this.corridor = [];

    /* vertices */
    var points = [];
    var bounds = jsNewGLatLngBounds();
    if (this.verticeMarkers.length > 0) {
        var lastPT = null;
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            var vm     = this.verticeMarkers[i];
            var vpt    = vm.getPoint(); // GLatLng
            
            /* draw vertice circle */
            var crLat  = geoRadians(vpt.lat());  // radians
            var crLon  = geoRadians(vpt.lng());  // radians
            var d      = this.radiusMeters / EARTH_RADIUS_METERS;
            var circlePts = [];
            for (x = 0; x <= 360; x += 6) {         // 6 degrees (saves memory, & it still looks like a circle)
                var xrad  = geoRadians(x);          // radians
                var rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
                var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
                var pt    = jsNewGLatLng(geoDegrees(rrLat),geoDegrees(rrLon));
                circlePts.push(pt);
            }
            var circlePoly = jsNewGPolygon(circlePts, this.shapeColor, 1, 0.9, this.shapeColor, 0.1);
            this.corridor.push(circlePoly);
            this.googleMap.addOverlay(circlePoly);
            bounds.extend(vpt);
            
            /* draw connecting corridor */
            if (lastPT != null) {
                var ptA = lastPT; // GLatLng
                var ptB = vpt;    // GLatLng
                var hAB = geoHeading(ptA.lat(), ptA.lng(), ptB.lat(), ptB.lng()) - 90.0; // perpendicular
                var rp1 = geoRadiusPoint(ptA.lat(), ptA.lng(), this.radiusMeters, hAB        ); // JSMapPoint
                var rp2 = geoRadiusPoint(ptB.lat(), ptB.lng(), this.radiusMeters, hAB        ); // JSMapPoint
                var rp3 = geoRadiusPoint(ptB.lat(), ptB.lng(), this.radiusMeters, hAB + 180.0); // JSMapPoint
                var rp4 = geoRadiusPoint(ptA.lat(), ptA.lng(), this.radiusMeters, hAB + 180.0); // JSMapPoint
                var rectPts = [];
                rectPts.push(jsNewGLatLng(rp1.lat,rp1.lon));
                rectPts.push(jsNewGLatLng(rp2.lat,rp2.lon));
                rectPts.push(jsNewGLatLng(rp3.lat,rp3.lon));
                rectPts.push(jsNewGLatLng(rp4.lat,rp4.lon));
                rectPts.push(jsNewGLatLng(rp1.lat,rp1.lon));
                var rectPoly = jsNewGPolygon(rectPts, this.shapeColor, 1, 0.9, this.shapeColor, 0.1);
                this.corridor.push(rectPoly);
                this.googleMap.addOverlay(rectPoly);
            }
            lastPT = vpt; // GLatLng
            
        }
    }

};

CorridorGeozone.prototype.remove = function()
{
    if (this.verticeMarkers != null) {
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            if (this.verticeMarkers[i].isVisible) { // corridor vertice
                this.googleMap.removeOverlay(this.verticeMarkers[i]);
                this.verticeMarkers[i].isVisible = false;
            }
        }
    }
    if (this.corridor != null) {
        for (var i = 0; i < this.corridor.length; i++) {
            this.googleMap.removeOverlay(this.corridor[i]);
        }
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
