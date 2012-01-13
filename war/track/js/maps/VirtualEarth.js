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
//   JSDrawRoute(JSMapPoint points[], String color)
//   JSDrawShape(String type, double radius, JSMapPoint points[], String color, boolean zoomTo)
//   JSDrawGeozone(int type, double radius, JSMapPoint points[], String color, int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin, boolean center)
//   JSUnload()
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/07/27  Martin D. Flynn
//     -Adjusted displayed pushpin marker position based on size/offset.
//  2008/08/08  Martin D. Flynn
//     -Added support for Geozones
//  2008/08/24  Martin D. Flynn
//     -Added 'replay' support.
//  2008/09/01  Martin D. Flynn
//     -Fixed Geozone selection on Safari
//     -Modified Geozome mouse behavior (click to recenter, shift-drag to resize).
//  2008/10/16  Martin D. Flynn
//     -Added recentering on pushpin when selecting a 'detail point' and the pushpin
//      is currently not displayed on the map.
//  2009/08/23  Martin D. Flynn
//     -Added color argument to JSDrawRoute
//     -Added option for drawing multiple points per device on fleet map
// ----------------------------------------------------------------------------

var VEPushpin_XOfs      = 13;
var VEPushpin_YOfs      = 13;

var DRAG_NONE           = 0x00;
var DRAG_RULER          = 0x01;
var DRAG_GEOZONE        = 0x10;
var DRAG_GEOZONE_CENTER = 0x11;
var DRAG_GEOZONE_RADIUS = 0x12;

/**
*** JSMap constructor
**/
function JSMap(element)
{

    /* map */
    try {
        this.virtEarthMap = new VEMap(element.id);
        this.virtEarthMap.LoadMap();  // (VELatLong,zoomLevel,VEMapStyle,isStatic);
    } catch (e) {
        alert("Error loading VE map:\n" + e);
        if (this.virtEarthMap == null) { return; }
    }

    /* map style */
    this._setDefaultMapStyle();

    /* map attributes */
  //this.virtEarthMap.SetDashboardSize(VEDashboardSize.Small);
  //this.virtEarthMap.SetMapStyle(VEMapStyle.Aerial);   // VEMapStyle.Birdseye, VEMapStyle.Road, VEMapStyle.Hybrid
  //element.stype.position = 'relative';
    this.virtEarthMap.SetMouseWheelZoomToCenter(SCROLL_WHEEL_ZOOM);
    element.style.cursor = "crosshair";

    /* MSIE? */
    this.userAgent_MSIE = /MSIE/.test(navigator.userAgent);

    /* last mousedown X/Y */
    this.lastX = 0;
    this.lastY = 0;

    /* draw layers */
    this.virtEarthGeozoneLayer = this.virtEarthMap;
    //this.virtEarthGeozoneTempLayer = new VEShapeLayer();
    //this.virtEarthMap.AddShapeLayer(this.virtEarthGeozoneTempLayer);
    this.virtEarthShapeLayer = new VEShapeLayer();
    this.virtEarthMap.AddShapeLayer(this.virtEarthShapeLayer);
    this.virtEarthRulerLayer = new VEShapeLayer();
    this.virtEarthMap.AddShapeLayer(this.virtEarthRulerLayer);
    
    /* popup info box */
    this.visiblePopupInfoBox = null;
    
    /* drawn shapes */
    this.drawShapes = [];

    /* replay vars */
    this.replayTimer = null;
    this.replayIndex = 0;
    this.replayInterval = (REPLAY_INTERVAL < 100)? 100 : REPLAY_INTERVAL;
    this.replayInProgress = false;
    this.replayPushpins = [];

    /* drag vars */
    this.dragType = DRAG_NONE;
    this.dragRulerLatLon = null;
    this.dragMarker = null;
    this.dragZoneOffsetLat = 0.0;
    this.dragZoneOffsetLon = 0.0;
    this.geozoneCenter = null;
    this.geozoneShape  = null;
    this.geozonePoints = null;      // JSMapPoint[]
    this.primaryIndex  = -1;
    this.primaryCenter = null;      // JSMapPoint

    /* Lat/Lon display */
    this.latLonDisplay = jsmGetLatLonDisplayElement();
    jsmSetLatLonDisplay(0,0);

    /* mouse event handlers */
    try {
        var self = this;
        this.virtEarthMap.AttachEvent("onmousedown" , function (e) { return self._event_OnMouseDown(e); });
        this.virtEarthMap.AttachEvent("onmousemove" , function (e) { return self._event_OnMouseMove(e); });
        this.virtEarthMap.AttachEvent("onmouseup"   , function (e) { return self._event_OnMouseUp(e);   });
        this.virtEarthMap.AttachEvent("onmouseover" , function (e) { return self._event_OnMouseOver(e); });
        this.virtEarthMap.AttachEvent("onmouseout"  , function (e) { return self._event_OnMouseOut(e);  });
        this.virtEarthMap.AttachEvent("onclick"     , function (e) { return self._event_OnClick(e);     });
    } catch (e) {
        alert("Error setting mouse events:\n" + e);
    }

};

/**
*** Set map style
**/
JSMap.prototype._setDefaultMapStyle = function()
{
    if ((DEFAULT_VIEW == "aerial") || (DEFAULT_VIEW == "satellite")) {
        this.virtEarthMap.SetMapStyle(VEMapStyle.Aerial);
    } else
    if (DEFAULT_VIEW == "hybrid") {
        this.virtEarthMap.SetMapStyle(VEMapStyle.Hybrid);
    } else
    if (DEFAULT_VIEW == "birdseye") {
        this.virtEarthMap.SetMapStyle(VEMapStyle.Birdseye);
    } else {
        this.virtEarthMap.SetMapStyle(VEMapStyle.Road);
    }
}

// ----------------------------------------------------------------------------

/**
*** Unload/release resources
**/
JSMap.prototype.JSUnload = function()
{
    //
};

// ----------------------------------------------------------------------------

/**
*** Clear all pushpins and drawn lines
**/
JSMap.prototype.JSClearLayers = function()
{
    try { this.virtEarthMap.DeleteAllShapes();        } catch (e) {}
    try { this.virtEarthMap.DeleteAllPolylines();     } catch (e) {}
    this._removeShapes();
    this._clearReplay();
    this.centerBoundsPoints = [];
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
            this.virtEarthMap.HideInfoBox(); // hide any displayed InfoBox
            jsmHighlightDetailRow(-1, false);
            this._startReplayTimer(replay, 100);
            return REPLAY_RUNNING;
        } else {
            // replaying "RUNNING" ... pausing replay
            this._stopReplayTimer();
            return REPLAY_PAUSED;
        }
    }
}

/**
*** Start the replay timer
**/
JSMap.prototype._startReplayTimer = function(replay, interval)
{
    if (this.replayInProgress) {
        this.replayTimer = setTimeout("jsmap._replayPushpins("+replay+")", interval);
    }
    jsmSetReplayState(REPLAY_RUNNING);
}

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
}

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
}

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
}

// ----------------------------------------------------------------------------

/**
*** Sets the center of the map
**/
JSMap.prototype.JSSetCenter = function(center, zoom)
{
    var vpt = new VELatLong(center.lat, center.lon);
    // I'd prefer that this "recentering" NOT be animated!
    var animate = this.virtEarthMap.vemapcontrol.IsAnimationEnabled();
    this.virtEarthMap.vemapcontrol.SetAnimationEnabled(false);
    if (!zoom || (zoom == 0)) {
        this.virtEarthMap.SetCenter(vpt);
    } else
    if (zoom > 0) {
        this.virtEarthMap.SetCenterAndZoom(vpt, zoom);
    } else {
        var vb = new JSBounds();
        vb.extend(center);
        zoom = this._calcBestZoom(vb);
        //alert("Zoom = " + zoom);
        this.virtEarthMap.SetCenterAndZoom(vpt, zoom);
    }
    this.virtEarthMap.vemapcontrol.SetAnimationEnabled(animate);
};

/**
*** Draw the specified pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
*** @param recenter  True to cause the map to re-center on the drawn pushpins
*** @param replay    Replay mode
**/
JSMap.prototype.JSDrawPushpins = function(pushPins, recenterMode, replay)
{

    /* clear replay (may be redundant, but repeated just to make sure) */
    this._clearReplay();
    this.virtEarthMap.HideInfoBox(); // hide any displayed InfoBox
    var mapStyle = this.virtEarthMap.GetMapStyle();

    /* drawn pushpins */
    var drawPushpins = [];

    /* recenter map on points */
    var points = [];
    var pointCount = 0;
    if ((pushPins != null) && (pushPins.length > 0)) {
        // extend bounding box around displayed pushpins
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            if ((pp.lat != 0.0) || (pp.lon != 0.0)) {
                pointCount++;
                var veLatLon = new VELatLong(pp.lat,pp.lon);
                this.centerBoundsPoints.push(veLatLon);
                points.push(veLatLon);
                drawPushpins.push(pp);
            }
        }
    }
    if (recenterMode > 0) {
        // Recenter modes:
        //  0 = none (leave map positioned as-is)
        //  1 = center on last point only (no zoom)
        //  2 = center and zoom
        try {
            if (pointCount <= 0) {
                var centerPt   = new VELatLong(DEFAULT_CENTER.lat, DEFAULT_CENTER.lon);
                var zoomFactor = DEFAULT_ZOOM;
                this._setCenter(centerPt, zoomFactor);
            } else
            if (recenterMode == RECENTER_LAST) { // center on last point
                var centerPt   = points[points.length - 1];
                var zoomFactor = -1; // default VirtualEarth zoom
                this._setCenter(centerPt, zoomFactor);
            } else
            if (recenterMode == RECENTER_PAN) { // pan to last point
                var centerPt   = points[points.length - 1];
                var zoomFactor = -1; // default VirtualEarth zoom
                this._setCenter(centerPt, zoomFactor);
            } else {
                if (mapStyle == VEMapStyle.Birdseye) {
                    this._setDefaultMapStyle();
                }
                this.virtEarthMap.SetMapView(this.centerBoundsPoints);
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

    /* Birdseye pushpin accuracy */
    //this.virtEarthMap.SetShapesAccuracy(VEShapeAccuracy.Pushpin);

    /* Birdseye scene */
    //var lastPP = drawPushpins[drawPushpins.length - 1];
    //this.virtEarthMap.SetBirdseyeScene(new VELatLong(lastPP.lat, lastPP.lon));

    /* any errors? */
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
    
    /* hide infobox */
    this.virtEarthMap.HideInfoBox(); // hide any displayed InfoBox
    
    /* draw pushpins now */
    if ((pushPins != null) && (pushPins.length > 0)) {
        var pushpinErr = null;
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i];
            try {
                this._addPushpin(pp);
            } catch (e) {
                if (pushpinErr == null) { pushpinErr = e; }
            }
        }
    }

    /* any errors? */
    if (pushpinErr != null) {
        alert("Error: adding pushpins:\n" + pushpinErr);
    }

}

/**
*** Sets the center/zoom of the map
**/
JSMap.prototype._setCenter = function(center, zoom)
{
    if (zoom && (zoom > 0)) {
        this.virtEarthMap.SetCenterAndZoom(center, zoom);
    } else {
        this.virtEarthMap.SetCenter(center);
    }
};

/**
*** Adds a single pushpin to the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._addPushpin = function(pp)
{

    pp.map = this.virtEarthMap;

    /* background marker */
    if (pp.bgUrl) {
        var bgMarker = new VEShape(VEShapeType.Pushpin, new VELatLong(pp.lat, pp.lon));
        bgMarker.SetTitle(" ");
        bgMarker.SetDescription("");
        bgMarker.pp = pp;
        var bgURL  = pp.bgUrl;
        var bgXOfs = VEPushpin_XOfs - pp.bgOffset[0];
        var bgYOfs = VEPushpin_YOfs - pp.bgOffset[1];
        var bgHTML = "<div style='position:relative; top:"+bgYOfs+"px; left:"+bgXOfs+"px'><img src='"+bgURL+"'></div>";
        bgMarker.SetCustomIcon(bgHTML);
        this.virtEarthMap.AddShape(bgMarker);
        pp.bgMarker = bgMarker;
    }

    /* pushpin marker */
    // Offset to hotspot
    var ppMarker = new VEShape(VEShapeType.Pushpin, new VELatLong(pp.lat, pp.lon));
    //ppMarker.SetTitle('<small>[#' + ndx + '] <b>' + dev + ' : ' + code + '</b></small>');",
    // http://msdn.microsoft.com/en-us/library/bb412441.aspx
    ppMarker.SetTitle(" ");
    ppMarker.SetDescription(pp.html);
    ppMarker.pp = pp;
    // Using 'margins' here positions the push-pin properly, but may also cause IE to clip the image
    // var imgURL = "<div style='margin-left:"+ppXOfs+"px; margin-top:"+ppYOfs+"px;'><img src='"+ppURL+"'></div>";
    var ppURL  = pp.iconUrl;
    var ppXOfs = VEPushpin_XOfs - pp.iconHotspot[0];
    var ppYOfs = VEPushpin_YOfs - pp.iconHotspot[1];
    var ppHTML = "<div style='position:relative; top:"+ppYOfs+"px; left:"+ppXOfs+"px'><img src='"+ppURL+"'></div>";
    ppMarker.SetCustomIcon(ppHTML);
    // var iconSpec = new VECustomIconSpecification(null,imgURL,'Test',null,false,true,false,10,null,null,null,null);
    // ppMarker.SetCustomIcon(iconSpec);  // 3D
    // TODO: "mouseover" events?

    /* marker */
    this.virtEarthMap.AddShape(ppMarker);
    pp.marker = ppMarker;

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
                this.virtEarthMap.DeleteShape(lastPP.marker);
            }
            if (lastPP.bgMarker) {
                this.virtEarthMap.DeleteShape(lastPP.bgMarker);
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
        // ignore
    }

}

// ----------------------------------------------------------------------------

/**
*** This method should cause the info-bubble popup for the specified pushpin to display
*** @param pushPin   The JSMapPushpin object which popup its info-bubble
**/
JSMap.prototype.JSShowPushpin = function(pp, center)
{
    if (pp) {
        if (pp.popupShown) {
            this._hidePushpinPopup(pp);
        } else {
            if (center || !this._isPointOnMap(pp.lat,pp.lon,5,5,5,5)) {
                this.JSSetCenter(new JSMapPoint(pp.lat, pp.lon));
            }
            this._showPushpinPopup(pp);
        }
    }
};

JSMap.prototype._isPointOnMap = function(lat, lon, margTop, margLeft, margBott, margRght)
{
    if ((MAP_HEIGHT > 0) && (MAP_WIDTH > 0)) {
        var top   = 0                 + margTop; // this.virtEarthMap.GetTop();
        var left  = 0                 + margLeft; // this.virtEarthMap.GetLeft();
        var bott  = top  + MAP_HEIGHT - margBott;
        var rght  = left + MAP_WIDTH  - margRght;
        var TL    = this.virtEarthMap.PixelToLatLong(new VEPixel(left, top ));
        var BR    = this.virtEarthMap.PixelToLatLong(new VEPixel(rght, bott));
        //alert("TopLeft="+TL.Latitude+"/"+TL.Longitude+", BottomRight="+BR.Latitude+"/"+BR.Longitude);
        if ((lat > TL.Latitude) || (lat < BR.Latitude)) {
            return false;
        } else
        if ((lon < TL.Longitude) || (lon > BR.Longitude)) {
            return false;
        } else {
            return true;
        }
    } else {
        return true;
    }
}

JSMap.prototype._showPushpinPopup = function(pp)
{
    this._hidePushpinPopup(this.visiblePopupInfoBox);
    if (pp && !pp.popupShown && pp.map) {
        pp.map.ShowInfoBox(pp.marker);
        pp.popupShown = true;
        this.visiblePopupInfoBox = pp;
        jsmHighlightDetailRow(pp.rcdNdx, true);
    } else {
        this.visiblePopupInfoBox = null;
    }
}

JSMap.prototype._hidePushpinPopup = function(pp)
{
    if (pp && pp.popupShown) {
        pp.map.HideInfoBox(pp.marker);
        pp.popupShown = false;
        jsmHighlightDetailRow(pp.rcdNdx, false);
    }
}

// ----------------------------------------------------------------------------

/**
*** Draws a line between the specified points on the map.
*** @param points   An array of JSMapPoint objects
**/
var routLineNdx = 0;
JSMap.prototype.JSDrawRoute = function(points, color)
{
    if (points && (points.length >= 2)) {
        var latlon = [];
        for (var i = 0; i < points.length; i++) {
            latlon.push(new VELatLong(points[i].lat, points[i].lon));
        }
        try {
            var name = "routeLine" + (routLineNdx++);
            var rgb  = rgbVal(color); // ie. Convert "#FF2222" to { R:255, G:34, B:34 }
            var veColor = new VEColor(rgb.R,rgb.G,rgb.B,1);
            this.virtEarthMap.AddPolyline(new VEPolyline(name, latlon, veColor, 2));
        } catch (e) {
            alert("Error creating route:\n" + e);
        }
    }
};

// ----------------------------------------------------------------------------

/**
*** Remove previously drawn shapes 
**/
JSMap.prototype._removeShapes = function()
{
    this.virtEarthShapeLayer.DeleteAllShapes();
    this.drawShapes = [];
}

/**
*** Draws a Shape on the map at the specified location
*** @param type     The Geozone shape type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param color    shape color
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
    var rgb = rgbVal(color);  // ie. Convert "#FF2222" to { R:255, G:34, B:34 }

    /* zoom bounds */
    var mapBounds = zoomTo? new JSBounds() : null;

    /* draw shape */
    var didDrawShape = false;
    if (type == "circle") { // ZONE_POINT_RADIUS

        for (var i = 0; i < verticePts.length; i++) {
            var vp = verticePts[i]; // JSMapPoint
            if ((vp.lat == 0.0) && (vp.lon == 0.0)) { continue; }
            var center = new VELatLong(vp.lat,vp.lon);
            var circlePoints = this._getCirclePoints(center, radiusM);
            var shape = new VEShape(VEShapeType.Polygon, circlePoints);
            shape.SetTitle('<small>Circle</small>');
            shape.SetLineWidth(1);
            shape.SetDescription("Circle");
            shape.HideIcon();  // remove the pushpin at the center of the circle
            shape.SetLineColor(new VEColor(rgb.R,rgb.G,rgb.B,1.0));
            shape.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B,0.1));
            if (mapBounds) { 
                mapBounds.extend(vp);
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM,   0.0));
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM,  90.0));
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM, 180.0));
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM, 270.0));
            }
            this.drawShapes.push(shape);
            this.virtEarthShapeLayer.AddShape(shape);
            didDrawShape = true;
        }

    } else
    if (type == "rectangle") {

        if (verticePts.length >= 2) {

            /* create rectangle */
            var vp0   = verticePts[0];
            var vp1   = verticePts[1];
            var TL    = new VELatLong(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var TR    = new VELatLong(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var BL    = new VELatLong(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var BR    = new VELatLong(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var vePts = [ TL, TR, BR, BL, TL ];
            var shape = new VEShape(VEShapeType.Polygon, vePts);
            shape.SetTitle("<small>Rectangle</small>");
            shape.SetLineWidth(1);
            shape.SetDescription("Rectangle");
            shape.HideIcon();  // remove the pushpin at the center of the circle
            shape.SetLineColor(new VEColor(rgb.R,rgb.G,rgb.B,1.0));
            shape.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B,0.1));
            if (mapBounds) {
                mapBounds.extend(vp0); 
                mapBounds.extend(vp1); 
            }
            this.drawShapes.push(shape);
            this.virtEarthShapeLayer.AddShape(shape);
            didDrawShape = true;

        }
            
    } else
    if (type == "polygon") {
       
        if (verticePts.length >= 3) {

            var vePts = [];
            for (var p = 0; p < verticePts.length; p++) {
                var vePt = new VELatLong(verticePts[p].lat, verticePts[p].lon);
                vePts.push(vePt);
                if (mapBounds) { 
                    mapBounds.extend(verticePts[p]); 
                }
            }
            var poly = new VEShape(VEShapeType.Polygon, vePts);
            poly.SetTitle("<small>Polygon</small>");
            poly.SetLineWidth(1);
            poly.SetDescription("Rectangle");
            poly.HideIcon();  // remove the pushpin at the center of the circle
            poly.SetLineColor(new VEColor(rgb.R,rgb.G,rgb.B,1.0));
            poly.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B,0.1));
            this.drawShapes.push(poly);
            this.virtEarthShapeLayer.AddShape(poly);
            didDrawShape = true;

        }

    } else
    if (type == "corridor") { // ZONE_SWEPT_POINT_RADIUS

        var lastPT = null;
        for (var i = 0; i < verticePts.length; i++) {
            var vp = verticePts[i]; // JSMapPoint
            if ((vp.lat == 0.0) && (vp.lon == 0.0)) { continue; }
            var center = new VELatLong(vp.lat,vp.lon);
            var circlePoints = this._getCirclePoints(center, radiusM);
            var circleShape = new VEShape(VEShapeType.Polygon, circlePoints);
            circleShape.SetTitle('<small>Corridor</small>');
            circleShape.SetLineWidth(1);
            circleShape.SetDescription("Corridor");
            circleShape.HideIcon();  // remove the pushpin at the center of the circle
            circleShape.SetLineColor(new VEColor(rgb.R,rgb.G,rgb.B,1.0));
            circleShape.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B,0.1));
            if (mapBounds) { 
                mapBounds.extend(vp);
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM,   0.0));
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM,  90.0));
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM, 180.0));
                mapBounds.extend(this._calcRadiusPoint(vp, radiusM, 270.0));
            }
            this.drawShapes.push(circleShape);
            this.virtEarthShapeLayer.AddShape(circleShape);
            
            if (lastPT != null) {
                var ptA = lastPT;   // JSMapPoint
                var ptB = vp;       // JSMapPoint
                var hAB = geoHeading(ptA.lat, ptA.lon, ptB.lat, ptB.lon) - 90.0; // perpendicular
                var rp1 = this._calcRadiusPoint(ptA, radiusM, hAB        ); // JSMapPoint
                var rp2 = this._calcRadiusPoint(ptB, radiusM, hAB        ); // JSMapPoint
                var rp3 = this._calcRadiusPoint(ptB, radiusM, hAB + 180.0); // JSMapPoint
                var rp4 = this._calcRadiusPoint(ptA, radiusM, hAB + 180.0); // JSMapPoint
                var rectPts = [];
                rectPts.push(new VELatLong(rp1.lat, rp1.lon));
                rectPts.push(new VELatLong(rp1.lat, rp1.lon));
                rectPts.push(new VELatLong(rp1.lat, rp1.lon));
                rectPts.push(new VELatLong(rp1.lat, rp1.lon));
                var rectShape = new VEShape(VEShapeType.Polygon, rectPts);
                rectShape.SetTitle("<small>Corridor</small>");
                rectShape.SetLineWidth(1);
                rectShape.SetDescription("Corridor");
                rectShape.HideIcon();  // remove the pushpin at the center of the circle
                rectShape.SetLineColor(new VEColor(rgb.R,rgb.G,rgb.B,1.0));
                rectShape.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B,0.1));
                this.drawShapes.push(rectShape);
                this.virtEarthShapeLayer.AddShape(rectShape);
            }
            lastPT = vp;
            
            didDrawShape = true;
        }

    } else
    if (type == "center") {

        if (mapBounds) {
            for (var p = 0; p < verticePts.length; p++) {
                var jsPt = new JSMapPoint(verticePts[p].lat, verticePts[p].lon);
                mapBounds.extend(jsPt);
            }
            didDrawShape = true;
        }

    }

    /* center on shape */
    if (didDrawShape && zoomTo && mapBounds) {
        var centerPt   = mapBounds.getCenter(); // JSMapPoint
        var zoomFactor = this._calcBestZoom(mapBounds);
        this._setCenter(new VELatLong(centerPt.lat, centerPt.lon), zoomFactor);
    }

    /* shape not supported */
    return didDrawShape;

}

// ----------------------------------------------------------------------------

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param editable True to allow editing the circle (position, radius)
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points
*** @param primNdx  Index of primary point
*** @return An object representing the Circle.
**/
JSMap.prototype.JSDrawGeozone = function(type, radiusM, points, color, primNdx)
{
    // only type 0 (pointRadius) is currently supported

    /* Geozone mode */
    jsvGeozoneMode = true;

    /* remove old geozone */
    //for (var i = 0; i < this.geozonePoints.length; i++) { this.geozonePoints[i].remove(); }
    //this.geozonePoints = [];
    this.geozoneShape = null;
    this.geozoneCenter = null;
    this.virtEarthGeozoneLayer.DeleteAllShapes();
    //this.virtEarthGeozoneTempLayer.DeleteAllShapes();

    /* save geozone points */
    this.geozonePoints = points;
    this.primaryIndex  = primNdx;

    /* no points? */
    if ((points == null) || (points.length <= 0)) {
        //alert("No Zone center!");
        return null;
    }
    
    /* zone shape color */
    this.shapeColor = (color && (color != ""))? color : "#00B400";
    this.lineWidth  = 3;
    //this.opacity    = 1.0;

    /* point-radius */
    //type = ZONE_POLYGON;
    if (type == ZONE_POINT_RADIUS) {

        /* adjust radius */
        if (isNaN(radiusM))              { radiusM = 5000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }

        /* draw points */
        var count = 0;
        var mapBounds = new JSBounds();
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if ((c.lat != 0.0) || (c.lon != 0.0)) {
                var isPrimary = (i == primNdx);
                if (jsvZoneEditable || jsvShowVertices) {
                    this._addCircleShape(new VELatLong(c.lat,c.lon), radiusM, this.shapeColor, this.lineWidth, isPrimary);
                }
                mapBounds.extend(c);
                mapBounds.extend(this._calcRadiusPoint(c, radiusM,   0.0));
                mapBounds.extend(this._calcRadiusPoint(c, radiusM,  90.0));
                mapBounds.extend(this._calcRadiusPoint(c, radiusM, 180.0));
                mapBounds.extend(this._calcRadiusPoint(c, radiusM, 270.0));
                count++;
            }
        }

        /* center on geozone */
        var centerPt = DEFAULT_CENTER; // JSMapPoint
        var zoom     = DEFAULT_ZOOM;
        if (count > 0) {
            centerPt = mapBounds.getCenter(); // JSMapPoint
            zoom     = this._calcBestZoom(mapBounds);
        }
        this._setCenter(new VELatLong(centerPt.lat, centerPt.lon), zoom);

    } else
    if (type == ZONE_POLYGON) {

        /* adjust radius */
        //if (isNaN(radiusM))              { radiusM = 5000; }
        //if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        //if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }

        var isDragging = false;

        /* draw points */
        var count = 0;
        var mapBounds = new JSBounds();
        var polyPts = []; // JSMapPoint[]
        var polyPtPrim = -1;
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if ((c.lat != 0.0) || (c.lon != 0.0)) {
                var isPrimary = (i == primNdx);
                var center    = (isPrimary && isDragging)? this.primaryCenter : c;
                if (isPrimary) {
                    this.primaryCenter = center; // OpenLayers.LonLat
                    polyPtsPrim = polyPts.length;
                }
                polyPts.push(center); // JSMapPoint
                mapBounds.extend(center);
                count++;
            }
        }
        if (polyPts.length >= 3) {
            this._addPolygonShape(polyPts, this.shapeColor, this.lineWidth);
        }

        /* center on geozone */
        var centerPt = DEFAULT_CENTER; // JSMapPoint
        var zoom     = DEFAULT_ZOOM;
        if (count > 0) {
            centerPt = mapBounds.getCenter(); // JSMapPoint
            zoom     = this._calcBestZoom(mapBounds);
        }
        this._setCenter(new VELatLong(centerPt.lat, centerPt.lon), zoom);

        /* current MPP */
        var mpp;
        if ((MAP_WIDTH > 0) && (MAP_HEIGHT > 0)) {
            mpp  = mapBounds.calculateMetersPerPixel(MAP_WIDTH, MAP_HEIGHT);
        } else {
            mpp  = mapBounds.calculateMetersPerPixel(680, 470); // TODO: read these values from the map
        }
        radiusM = 20.0 * mpp;
        jsvZoneRadiusMeters = radiusM;

        /* draw drag circles at vertices */
        for (var i = 0; i < polyPts.length; i++) {
            var c  = polyPts[i]; // OpenLayers.LonLat
            var vc = new VELatLong(c.lat,c.lon);
            var isPrimary = (i == polyPtsPrim);
            if (jsvZoneEditable || jsvShowVertices) {
                this._addCircleShape(vc, radiusM, this.shapeColor, this.lineWidth, isPrimary);
            }
            if (isPrimary) {
                this.geozoneCenter = vc;
            }
        }

    } else
    if (type == ZONE_SWEPT_POINT_RADIUS) {

        /* adjust radius */
        if (isNaN(radiusM))              { radiusM = 1000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }

        /* draw points */
        var count = 0;
        var mapBounds = new JSBounds();
        var polyPts = []; // JSMapPoint
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if ((c.lat != 0.0) || (c.lon != 0.0)) {
                var isPrimary = (i == primNdx);
                if (jsvZoneEditable || jsvShowVertices) {
                    this._addCircleShape(new VELatLong(c.lat,c.lon), radiusM, this.shapeColor, this.lineWidth, isPrimary);
                }
                polyPts.push(c); // JSMapPoint
                mapBounds.extend(c);
                mapBounds.extend(this._calcRadiusPoint(c, radiusM,   0.0));
                mapBounds.extend(this._calcRadiusPoint(c, radiusM,  90.0));
                mapBounds.extend(this._calcRadiusPoint(c, radiusM, 180.0));
                mapBounds.extend(this._calcRadiusPoint(c, radiusM, 270.0));
                count++;
            }
        }

        /* draw corridors */
        if (polyPts.length >= 2) {
            // routeline "_createRouteFeature"
            for (var i = 0; i < (polyPts.length - 1); i++) {
                var ptA = polyPts[i  ]; // JSMapPoint
                var ptB = polyPts[i+1]; // JSMapPoint
                var hAB = geoHeading(ptA.lat, ptA.lon, ptB.lat, ptB.lon) - 90.0; // perpendicular
                var rp1 = this._calcRadiusPoint(ptA, radiusM, hAB        ); // JSMapPoint
                var rp2 = this._calcRadiusPoint(ptB, radiusM, hAB        ); // JSMapPoint
                var rp3 = this._calcRadiusPoint(ptB, radiusM, hAB + 180.0); // JSMapPoint
                var rp4 = this._calcRadiusPoint(ptA, radiusM, hAB + 180.0); // JSMapPoint
                var rectPts = [ rp1, rp2, rp3, rp4 ];
                this._addPolygonShape(rectPts, this.shapeColor, this.lineWidth);
            }
        }

        /* center on geozone */
        var centerPt = DEFAULT_CENTER; // JSMapPoint
        var zoom     = DEFAULT_ZOOM;
        if (count > 0) {
            centerPt = mapBounds.getCenter(); // JSMapPoint
            zoom     = this._calcBestZoom(mapBounds);
        }
        this._setCenter(new VELatLong(centerPt.lat, centerPt.lon), zoom);

    } else {
        
        alert("Geozone type not supported: " + type);

    }
    
    return null;
};

/**
*** Calculate the best zoom
*** @param bounds JSBounds
**/
JSMap.prototype._calcBestZoom = function(bounds)
{
    // Derived from the zoom values specified at the following link:
    //  - http://blogs.msdn.com/virtualearth/archive/2006/02/25/map-control-zoom-levels-gt-resolution.aspx
    var mpp;
    if ((MAP_WIDTH > 0) && (MAP_HEIGHT > 0)) {
        mpp  = bounds.calculateMetersPerPixel(MAP_WIDTH, MAP_HEIGHT);
    } else {
        mpp  = bounds.calculateMetersPerPixel(680, 470); // TODO: read these values from the map
    }
    // Based on the ideal meters-per-pixel calculated by the JSBounds instance, the following converts
    // this value to the VirtualEarth Zoom#.
    var C = 0.2985821533203125000; // derived from MSVE zoom meters-per-pixel values
    // MPP  = C * 2^(19-ZOOM);   [where ZOOM is between 1 and 19, inclusive]
    // ZOOM = 19 - LOG2(MPP/C);  [where LOG2(X) == (LOGe(X)/LOGe(2))]
    var zoom = (19 - Math.round(Math.log(mpp / C) / Math.log(2.0))) - 1; // '-1' just to make sure everything fits
    if (zoom < 1) {
        return 1;
    } else
    if (zoom > 19) {
        return 19;
    } else {
        return zoom;
    }
}

/**
*** Returns an array of points (VELatLong) representing a circle polygon
*** @param center   The center point (VELatLong) of the circle
*** @param radiusM  The radius of the circle in meters
*** @return An array of points (VELatLong) representing a circle polygon
**/
JSMap.prototype._getCirclePoints = function(center, radiusM)
{
    var rLat = geoRadians(center.Latitude);   // radians
    var rLon = geoRadians(center.Longitude);  // radians
    var d    = radiusM / EARTH_RADIUS_METERS;
    var circlePoints = new Array();
    for (x = 0; x <= 360; x += 6) {       // 6 degrees (saves memory, and it still looks like a circle)
        var xrad = geoRadians(x);         // radians
        var tLat = Math.asin(Math.sin(rLat) * Math.cos(d) + Math.cos(rLat) * Math.sin(d) * Math.cos(xrad));
        var tLon = rLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(rLat), Math.cos(d) - Math.sin(rLat) * Math.sin(tLat));
        circlePoints.push(new VELatLong(geoDegrees(tLat), geoDegrees(tLon)));
    }
    return circlePoints;
};

/**
*** Returns a circle shape (VEShape)
*** @param center   The center point (VELatLong) of the circle
*** @param radiusM  The radius of the circle in meters
*** @return The circle VEShape object
**/
JSMap.prototype._addCircleShape = function(center, radiusM, color, width, isPrimary)
{

    /* Circle points */
    var circlePoints = this._getCirclePoints(center, radiusM);

    /* Circle shape */
    var shape = new VEShape(VEShapeType.Polygon, circlePoints);
    shape.SetTitle('<small><b>'+center.Latitude+' / '+center.Longitude+' '+radiusM+'</b></small>');
    shape.SetLineWidth(width);
    shape.SetDescription("Click and drag");
    shape.HideIcon();  // remove the pushpin at the center of the circle

    /* save primary shape */
    var rgb = (color && (color != ""))? rgbVal(color) : null;
    if (isPrimary) {
        if (rgb) {
            shape.SetLineColor(new VEColor(  220,    0,    0, 1.00));
            shape.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B, 0.20));
        } else {
            shape.SetLineColor(new VEColor(200,  0,  0, 1.0));
          //shape.SetFillColor(new VEColor(  0,100,150, 1.0));
        }
        //shape.SetCustomIcon("<img src='...'>");
        //shape.ShowIcon();
        this.virtEarthGeozoneLayer.AddShape(shape);
        this.geozoneShape  = shape;
        this.geozoneCenter = center; // VELatLong
    } else {
        if (rgb) {
            //shape.SetLineColor(new VEColor( 0,  90,  0, 1.0)); // #005A00
            shape.SetLineColor(new VEColor(rgb.R,rgb.G,rgb.B, 0.35));
            shape.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B, 0.20));
        } else {
            shape.SetLineColor(new VEColor( 0,  90,  0, 0.35)); // #005A00
            shape.SetFillColor(new VEColor( 0, 180,  0, 0.20)); // #00B400
        }
        this.virtEarthGeozoneLayer.AddShape(shape);
        //this.virtEarthGeozoneTempLayer.AddShape(shape);
    }

    /* Icon */
    /*
    if (jsvZoneEditable) {
        var iconURL = "http://labs.google.com/ridefinder/images/mm_20_blue.png";
        //var iconHotspot = [6, 12];
        //var xOfs = VEPushpin_XOfs - iconHotspot[0];
        //var yOfs = VEPushpin_YOfs - iconHotspot[1];
        //var imgHTML = "<div style='position: relative; top: "+yOfs+"px; left: "+xOfs+"px'><img src='"+iconURL+"'></div>";
        //shape.SetCustomIcon(imgHTML);
        //shape.SetCustomIcon(iconURL);

        var centerMarker = new VEShape(VEShapeType.Pushpin, new VELatLong(center.Latitude, center.Longitude));
        centerMarker.SetTitle(" ");
        centerMarker.SetDescription("GeoZone");
        centerMarker.SetCustomIcon(iconURL);
        this.virtEarthGeozoneLayer.AddShape(centerMarker);
        //this.virtEarthGeozoneTempLayer.AddShape(centerMarker);

    }
    */
    
    return shape;

};

JSMap.prototype._calcRadiusPoint = function(center/*JSMapPoint*/, radiusM, heading)
{
    var pt = geoRadiusPoint(center.lat, center.lon, radiusM, heading); // { lat: <>, lon: <> }
    return new JSMapPoint(pt.lat, pt.lon);
};

/**
*** Returns a circle shape (VEShape)
*** @param center   The center point (VELatLong) of the circle
*** @param radiusM  The radius of the circle in meters
*** @return The circle VEShape object
**/
JSMap.prototype._addPolygonShape = function(points, color, width)
{

    /* polygon points */
    var polyPoints = [];
    for (var i = 0; i < points.length; i++) {
        polyPoints.push(new VELatLong(points[i].lat, points[i].lon));
    }

    /* shape */
    var shape = new VEShape(VEShapeType.Polygon, polyPoints);
    shape.SetTitle('<small><b>Polygon Geozone</b></small>');
    shape.SetLineWidth(width);
    shape.SetDescription("Click and drag");
    shape.HideIcon();  // remove the pushpin at the center

    /* save primary shape */
    var rgb = (color && (color != ""))? rgbVal(color) : null;
    if (rgb) {
        //shape.SetLineColor(new VEColor( 0,  90,  0, 1.0)); // #005A00
        shape.SetLineColor(new VEColor(rgb.R,rgb.G,rgb.B, 0.75)); // 0.35
        shape.SetFillColor(new VEColor(rgb.R,rgb.G,rgb.B, 0.20));
    } else {
        shape.SetLineColor(new VEColor( 0,  90,  0, 0.75)); // 0.35, #005A00
        shape.SetFillColor(new VEColor( 0, 180,  0, 0.20)); // #00B400
    }
    this.virtEarthGeozoneLayer.AddShape(shape);
    this.geozoneShape = shape;

}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Mouse event handler to draw circles/lines on the map 
*** @param e  The mouse event
**/
JSMap.prototype._getGeozoneAtPoint = function(lat,lon)
{
    if ((this.geozoneCenter == null) || (this.geozoneShape == null)) {
        return null
    } else {
        var CC = this.geozoneCenter; // VELatLong
        var radiusM = zoneMapGetRadius(false);
        if (geoDistanceMeters(CC.Latitude,CC.Longitude,lat,lon) <= radiusM) {
            return this.geozoneShape;
        } else {
            return null;
        }
    }
}

/**
*** Mouse event handler to draw circles/lines on the map 
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseDown = function(e)
{
    
    /* last mousedown X/Y */
    this.lastX = e.mapX;
    this.lastY = e.mapY;
    
    /* quick exits */
    if (!e.leftMouseButton || e.altKey || (e.ctrlKey && e.shiftKey)) {
        this.dragType = DRAG_NONE;
        return false;
    }

    /* mouse down point */
    var x = e.mapX, y = e.mapY;
    var LL = this.virtEarthMap.PixelToLatLong(new VEPixel(x,y));
    jsmapElem.style.cursor = 'crosshair';

    /* distance ruler */
    if (e.ctrlKey) {
        this.dragType = DRAG_RULER;
        this.dragRulerLatLon = LL;
        this.virtEarthRulerLayer.DeleteAllShapes();
        jsmSetDistanceDisplay(0);
        return true; // e.preventDefault()
    }

    /* geozone mode */
    if (jsvGeozoneMode && jsvZoneEditable) {
        // Note: We cannot believe the value of 'e.elementID on Safari, so we do not use it here!
        var geozone = this._getGeozoneAtPoint(LL.Latitude,LL.Longitude);
        if (geozone != null) {
            this.virtEarthMap.HideInfoBox();
            this.dragMarker = geozone;
            if (e.shiftKey) {
                // resize
                this.dragType = DRAG_GEOZONE_RADIUS;
                this.virtEarthRulerLayer.DeleteAllShapes();
            } else {
                // move 
                this.dragType = DRAG_GEOZONE_CENTER;
                var CC = this.geozoneCenter;
                this.dragZoneOffsetLat = LL.Latitude  - CC.Latitude;
                this.dragZoneOffsetLon = LL.Longitude - CC.Longitude;
                this.virtEarthMap.vemapcontrol.EnableGeoCommunity(true);
            }
            return true;
        }
    }
    
    this.dragType = DRAG_NONE;
    return false;
};

/**
*** Mouse event handler to draw circles on the map 
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseUp = function(e)
{

    /* geozone mode */
    if (jsvGeozoneMode && (this.dragMarker != null)) {
        var center = this.geozoneCenter;
        this.dragMarker = null;
        jsmSetPointZoneValue(center.Latitude, center.Longitude, jsvZoneRadiusMeters);
        this.virtEarthMap.vemapcontrol.EnableGeoCommunity(false);
        this.dragType = DRAG_NONE;
        mapProviderParseZones(jsvZoneList);
        return true;
    }
        
    /* normal mode */
    this.dragType = DRAG_NONE;
    this.dragRulerLatLon = null;
    return false;

};

/**
*** Mouse event handler to detect lat/lon changes and draw circles/lines on the map 
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseMove = function(e)
{
    var X = e.mapX;
    //if (this.userAgent_MSIE) { X -= MAP_WIDTH / 2; }
    var Y = e.mapY;
    var LL = this.virtEarthMap.PixelToLatLong(new VEPixel(X, Y));

    /* update Latitude/Longitude display */
    if (this.latLonDisplay != null) {
        jsmSetLatLonDisplay(LL.Latitude,LL.Longitude);
        jsmapElem.style.cursor = 'crosshair';
    }

    /* disance ruler */
    if (this.dragType == DRAG_RULER) {
        this.virtEarthRulerLayer.DeleteAllShapes();
        var CC    = this.dragRulerLatLon;
        var distM = geoDistanceMeters(CC.Latitude, CC.Longitude, LL.Latitude, LL.Longitude);
        var line  = new VEShape(VEShapeType.Polyline, [CC,LL]);
        line.SetTitle('<small><b>'+distM+' meters</b></small>');
        //line.SetDescription('');
        line.HideIcon();
        line.SetLineWidth(2);
        line.SetLineColor(new VEColor(255,100,34,1)); // #FF6422
        line.SetFillColor(new VEColor(255, 34,34,1)); // #FF2222
        this.virtEarthRulerLayer.AddShape(line);
        jsmSetDistanceDisplay(distM);
        return true;
    }

    /* dragging the zone radius? */
    if (this.dragType == DRAG_GEOZONE_RADIUS) {
        this.virtEarthMap.HideInfoBox(this.dragMarker);
        this.virtEarthGeozoneLayer.DeleteAllShapes();
        //this.virtEarthGeozoneTempLayer.DeleteAllShapes();
        var CC = this.geozoneCenter; // VELatLong
        jsvZoneRadiusMeters = Math.round(geoDistanceMeters(CC.Latitude, CC.Longitude, LL.Latitude, LL.Longitude));
        if (jsvZoneRadiusMeters > MAX_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MAX_ZONE_RADIUS_M; }
        if (jsvZoneRadiusMeters < MIN_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MIN_ZONE_RADIUS_M; }
        this._addCircleShape(CC, jsvZoneRadiusMeters, jsvZoneColor, 2, true);
        jsmSetDistanceDisplay(jsvZoneRadiusMeters);
        this.virtEarthMap.HideInfoBox(this.dragMarker);
        return true;
    }
    
    /* dragging the zone center? */
    if (this.dragType == DRAG_GEOZONE_CENTER) {
        this.virtEarthMap.HideInfoBox(this.dragMarker);
        var circlePoints = this.dragMarker.GetPoints();
        this.geozoneCenter = new VELatLong(LL.Latitude - this.dragZoneOffsetLat, LL.Longitude - this.dragZoneOffsetLon);
        this.dragMarker.SetPoints(this._getCirclePoints(this.geozoneCenter, jsvZoneRadiusMeters));
        this.virtEarthMap.HideInfoBox(this.dragMarker);
        return true;
    }
    
    /* no-op */
    return false;

};

/**
*** Mouse event handler to recenter geozone
*** @param e  The mouse event
**/
JSMap.prototype._event_OnClick = function(e)
{

    /* quick exits */
    if (!e.leftMouseButton || e.altKey || e.ctrlKey || e.shiftKey) {
        // ignore 'shifts'
        return false;
    } else
    if ((e.mapX != this.lastX) || (e.mapY != this.lastY)) {
        // some 'dragging' has occurred
        return false;
    }

    /* geozone only */
    if (jsvGeozoneMode && jsvZoneEditable) {
        // recenter
        var x = e.mapX, y = e.mapY;
        var LL = this.virtEarthMap.PixelToLatLong(new VEPixel(x,y)); // VELatLong
        var CC = this.geozoneCenter? this.geozoneCenter : new VELatLong(0,0); // VELatLong
        var CCIsValid = ((CC.Latitude != 0.0) || (CC.Longitude != 0.0));
        var CCLLDistKM = geoDistanceMeters(CC.Latitude, CC.Longitude, LL.Latitude, LL.Longitude);
        if (jsvZoneType == ZONE_POINT_RADIUS) {
            var radiusM = zoneMapGetRadius(false);
            // inside primary zone?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any zone?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    //if (i == this.primaryIndex) { continue; }
                    var cc = this.geozonePoints[i]; // JSMapPoint
                    if (geoDistanceMeters(cc.lat, cc.lon, LL.Latitude, LL.Longitude) <= radiusM) {
                        return false;
                    }
                }
            }
            // outside geozone, recenter
            jsmSetPointZoneValue(LL.Latitude, LL.Longitude, jsvZoneRadiusMeters);
            mapProviderParseZones(jsvZoneList);
            return true;
        } else
        if (jsvZoneType == ZONE_POLYGON) {
            var radiusM = jsvZoneRadiusMeters;
            // inside primary zone?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any zone?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    //if (i == this.primaryIndex) { continue; }
                    var cc = this.geozonePoints[i];
                    if (geoDistanceMeters(cc.lat, cc.lon, LL.Latitude, LL.Longitude) <= radiusM) {
                        return false;
                    }
                }
            }
            // count number of valid points
            var count = 0;
            for (var z = 0; z < jsvZoneList.length; z++) {
                if ((jsvZoneList[z].lat != 0.0) || (jsvZoneList[z].lon != 0.0)) {
                    count++;
                }
            }
            if (count == 0) {
                // no valid points - create default polygon
                var radMeter = 450;
                var crLat    = geoRadians(LL.Latitude);   // radians
                var crLon    = geoRadians(LL.Longitude);  // radians
                for (x = 0; x < jsvZoneList.length; x++) {
                    var deg   = x * (360.0 / jsvZoneList.length);
                    var radM  = radMeter / EARTH_RADIUS_METERS;
                    if ((deg == 0.0) || ((deg > 170.0) && (deg<  190.0))) { radM *= 0.8; }
                    var xrad  = geoRadians(deg); // radians
                    var rrLat = Math.asin(Math.sin(crLat) * Math.cos(radM) + Math.cos(crLat) * Math.sin(radM) * Math.cos(xrad));
                    var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(radM) * Math.cos(crLat), Math.cos(radM)-Math.sin(crLat) * Math.sin(rrLat));
                    _jsmSetPointZoneValue(x, geoDegrees(rrLat), geoDegrees(rrLon), 0);
                }
            } else {
                // move valid points to new location
                var deltaLat = LL.Latitude  - CC.Latitude;
                var deltaLon = LL.Longitude - CC.Longitude;
                for (var x = 0; x < jsvZoneList.length; x++) {
                    var pt = jsvZoneList[x];
                    if ((pt.lat != 0.0) || (pt.lon != 0.0)) {
                        _jsmSetPointZoneValue(x, (pt.lat + deltaLat), (pt.lon + deltaLon), 0);
                    }
                }
            }
            // reparse zone
            mapProviderParseZones(jsvZoneList);
            return true;
        } else
        if (jsvZoneType == ZONE_SWEPT_POINT_RADIUS) {
            var radiusM = jsvZoneRadiusMeters;
            // inside primary zone?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any zone?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    //if (i == this.primaryIndex) { continue; }
                    var cc = this.geozonePoints[i];
                    if (geoDistanceMeters(cc.lat, cc.lon, LL.Latitude, LL.Longitude) <= radiusM) {
                        return false;
                    }
                }
            }
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
            var maxDeltaKM = ((maxDistKM > 5000)? maxDistKM : 5000) * 1.5;
            if (!CCIsValid || (count <= 0) || (CCLLDistKM <= maxDeltaKM)) {
                jsmSetPointZoneValue(LL.Latitude, LL.Longitude, radiusM);
            }
            // reparse zone
            mapProviderParseZones(jsvZoneList);
            return true;
        }
    }

    /* no-op */
    return false;

}; // _event_OnClick

// ----------------------------------------------------------------------------

/**
*** MouseOver event handler
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseOver = function(e)
{

    /* no-op */
    return false;

}

/**
*** MouseOut event handler
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseOut = function(e)
{

    /* no-op */
    return false;

}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
