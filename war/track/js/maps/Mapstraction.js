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
//   JSSetCenter(JSMapPoint center)
//   JSDrawPushpins(JSMapPushpin pushPin[], int recenterMode, int replay)
//   JSDrawPOI(JSMapPushpin pushPin[])
//   JSDrawRoute(JSMapPoint points[], String color)
//   JSDrawGeozone(int type, double radius, JSMapPoint points[], String color, int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin, boolean center)
//   JSUnload()
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Anthony George, Peter Jonas, Martin D. Flynn
//     -Extracted from obsolete "org/opengts/war/maps/ms/Mapstraction.java"
//  2008/07/27  Martin D. Flynn
//     -Modified 'JSMap.prototype._addPushpin' to set the proper 'iconAnchor' value
//  2008/08/08  Martin D. Flynn
//     -Added limited support for Geozones ("openlayers" only)
//  2009/08/23  Martin D. Flynn
//     -Added color argument to JSDrawRoute
//     -Added option for drawing multiple points per device on fleet map
// ----------------------------------------------------------------------------

/**
*** JSMap constructor
**/
function JSMap(element)
{

    /* init */
    this.olZoneFeature = null;

    /* map */
    var provider = (PROP_provider)? PROP_provider : 'openlayers';
    this.mapstraction = new Mapstraction(element, provider);
    var map = this.mapstraction.maps[provider];
    var self = this;

    /* 'mousemove' to update latitude/longitude */
    var locDisp = jsmGetLatLonDisplayElement();
    if (locDisp != null) {
        try {
            switch (provider) {
                case 'openlayers':
                    map.events.register('mousemove', map, function (e) {
                        var lonLat = map.getLonLatFromViewPortPx(e.xy);
                        var lon = lonLat.lon * (180.0 / 20037508.34);
                        var lat = lonLat.lat * (180.0 / 20037508.34);
                        lat = (180.0 / Math.PI) * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - (Math.PI / 2.0));
                        jsmSetLatLonDisplay(lat, lon);
                        jsmapElem.style.cursor = 'crosshair';
                    });
                    break;
                case 'microsoft':
                    map.AttachEvent('onmousemove', function (e) {
                        var latLon = map.PixelToLatLong(new VEPixel(e.mapX,e.mapY));
                        jsmSetLatLonDisplay(latLon.Latitude, latLon.Longitude);
                        jsmapElem.style.cursor = 'crosshair';
                    });
                    break;
                case 'google':
                    GEvent.addListener(map, 'mousemove', function (point) {
                        jsmSetLatLonDisplay(point.lat(), point.lng());
                        jsmapElem.style.cursor = 'crosshair';
                    });
                    break;
            }
        } catch (e) {
            //alert("Error: " + e);
        }
        jsmSetLatLonDisplay(0.0, 0.0);
    }

    /* OpenLayers draw layer */
    switch (provider) {
        case 'openlayers':
            this.olDrawLayer = new OpenLayers.Layer.Vector('drawLayer');
            map.addLayer(this.olDrawLayer);
            break;
    }

};

// ----------------------------------------------------------------------------

/**
*** Unload/release resources
**/
JSMap.prototype.JSUnload = function()
{
    var provider = (PROP_provider)? PROP_provider : '?';
    switch (provider) {
        case 'google':
            GUnload();
            break;
    }
};

// ----------------------------------------------------------------------------

/**
*** Clear all pushpins and drawn lines
**/
JSMap.prototype.JSClearLayers = function()
{
    try { this.mapstraction.removeAllMarkers();   } catch(e) {}
    try { this.mapstraction.removeAllPolylines(); } catch(e) {}
    
    /* OpenLayers clear */
    var provider = (PROP_provider)? PROP_provider : '?';
    switch (provider) {
        case 'openlayers':
            if ((this.olDrawLayer != null) && (this.olZoneFeature != null)) {
                try { this.olDrawLayer.removeFeatures(this.olZoneFeature); } catch (e) {}
            }
            break;
    }
    
    /* bounds */
    this.centerBounds = null;

};

// ----------------------------------------------------------------------------

/**
*** Sets the center of the map
**/
JSMap.prototype.JSSetCenter = function(center)
{
    try {
        this.mapstraction.setCenter(new LatLonPoint(center.lat, center.lon));
    } catch (e) {
        //
    }
};

/**
*** Draw the specified pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
*** @param recenter  True to cause the map to re-center on the drawn pushpins
**/
JSMap.prototype.JSDrawPushpins = function(pushPins, recenterMode, replay)
{
    try {
        
        if ((pushPins != null) && (pushPins.length > 0)) {

            /* place pushpins */
            var pointCount = 0;
            for (var i = 0; i < pushPins.length; i++) {

                /* pushpin */
                var pp = pushPins[i];
                if ((pp.lat == 0.0) && (pp.lon == 0.0)) {
                    continue;
                }

                /* bounds */
                if (this.centerBounds == null) {
                    this.centerBounds = new BoundingBox(pp.lat, pp.lon, pp.lat, pp.lon);
                } else {
                    this.centerBounds.extend(new LatLonPoint(pp.lat, pp.lon));
                }
                pointCount++;

                /* marker */
                this._addPushpin(pp);

            }

            /* center */
            if (recenterMode > 0) {
                if (pointCount <= 0) {
                    var centerPt   = new LatLonPoint(DEFAULT_CENTER.lat, DEFAULT_CENTER.lon);
                    var zoomFactor = DEFAULT_ZOOM;
                    if (zoomFactor > 0) {
                        this.mapstraction.setCenterAndZoom(centerPt, zoomFactor); 
                    } else {
                        this.mapstraction.setCenter(centerPt); 
                    }
                } else
                if (recenterMode == RECENTER_LAST) { // center on last point
                    var pp         = pushPins[pushPins.length - 1];
                    var centerPt   = new LatLonPoint(pp.lat, pp.lon);
                    this.mapstraction.setCenter(centerPt); 
                } else
                if (recenterMode == RECENTER_PAN) { // pan to last point
                    var pp         = pushPins[pushPins.length - 1];
                    var centerPt   = new LatLonPoint(pp.lat, pp.lon);
                    this.mapstraction.setCenter(centerPt); 
                } else {
                    var centerPt   = new LatLonPoint(
                        (this.centerBounds.ne.lat+this.centerBounds.sw.lat)/2.0, 
                        (this.centerBounds.ne.lon+this.centerBounds.sw.lon)/2.0);
                    var zoomFactor = null;
                    try {
                        // broken for OpenLayers
                        zoomFactor = this.mapstraction.getZoomLevelForBoundingBox(this.centerBounds);
                    } catch (e) {
                        zoomFactor = PUSHPIN_ZOOM;
                    }
                    if (zoomFactor > 0) {
                        this.mapstraction.setCenterAndZoom(centerPt, zoomFactor); 
                    } else {
                        this.mapstraction.setCenter(centerPt); 
                    }
                }
            } else {
                //alert("Not Centering! " + pointCount);
            }

        } else
        if (recenterMode > 0) {
            var centerPt   = new LatLonPoint(DEFAULT_CENTER.lat, DEFAULT_CENTER.lon);
            var zoomFactor = DEFAULT_ZOOM;
            if (zoomFactor > 0) {
                this.mapstraction.setCenterAndZoom(centerPt, zoomFactor); 
            } else {
                this.mapstraction.setCenter(centerPt); 
            }
        }
        
    } catch (e) {
        //alert("Error: [JSDrawPushpins] " + e);
    }
};

/**
*** Draw the specified PointsOfInterest pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
**/
JSMap.prototype.JSDrawPOI = function(pushPins)
{
    if ((pushPins != null) && (pushPins.length > 0)) {
        for (var i = 0; i < pushPins.length; i++) {

            /* pushpin */
            var pp = pushPins[i];
            if ((pp.lat == 0.0) && (pp.lon == 0.0)) {
                continue;
            }

            /* marker */
            this._addPushpin(pp);

        }
    }
}

/**
*** Adds a single pushpin to the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._addPushpin = function(pp)
{
    try {
        var pin = new Marker(new LatLonPoint(pp.lat, pp.lon));
        pin.setInfoBubble(pp.html);
        pin.setLabel(pp.label);
        if (SHOW_CUSTOM_ICON) { 
            // doesn't work well on anything but OpenLayers
            var iconAnchor = [ -pp.iconHotspot[0],  -pp.iconHotspot[1] ];
            pin.setIcon(pp.iconUrl, pp.iconSize, iconAnchor); 
        } else {
            // default icon will be used 
        }
        this.mapstraction.addMarker(pin); 
    } catch (e) {
        //
    }
};

// ----------------------------------------------------------------------------

/**
*** This method should cause the info-bubble popup for the specified pushpin to display
*** @param pushPin   The JSMapPushpin object which popup its info-bubble
**/
JSMap.prototype.JSShowPushpin = function(pushPin, center)
{
    //
};

// ----------------------------------------------------------------------------

/**
*** Draws a line between the specified points on the map.
*** @param points   An array of JSMapPoint objects
**/
JSMap.prototype.JSDrawRoute = function(points, color)
{

    /* not supported in MSIE */
    if (/MSIE/.test(navigator.userAgent)) {
        return;
    }

    /* convert points */
    var latlon = [];
    for (var i = 0; i < points.length; i++) {
        latlon.push(new LatLonPoint(points[i].lat,points[i].lon));
    }
    var polyline = new Polyline(latlon);
    
    /* line style */
    // Note: Local 'mapstraction.js' modified to handle closed/open 'Polyline's.
    polyline.setClosed(false);
    polyline.setColor(color);
    polyline.setFillColor(color);
    polyline.setOpacity(1);
    polyline.setWidth(2);
    
    /* draw line */
    this.mapstraction.addPolyline(polyline,true);
    
};

// ----------------------------------------------------------------------------

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points
*** @return An object representing the Circle.
**/
JSMap.prototype.JSDrawGeozone = function(type, radiusM, points, color, primNdx)
{

    /* geozones currently only supported for OpenLayers */
    var provider = (PROP_provider)? PROP_provider : '?';
    if (provider != 'openlayers') {
        return null;
    }
    var map = this.mapstraction.maps[provider];

    /* Geozone mode */
    jsvGeozoneMode = true;

    /* remove old geozone */
    try {
        if (this.olZoneFeature != null) {
            this.olDrawLayer.removeFeatures(this.olZoneFeature);
        }
    } catch (e) {
        // ignore
    }
    this.geozoneCenter = null;

    /* draw geozone */
    if ((points != null) && (points.length > 0)) {
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }

        /* draw points */
        var mapBounds = new OpenLayers.Bounds();
        for (var i = 0; i < points.length; i++) {
            var c = points[i];
            var olpt = this._toOpenLayerPoint(c);
            this._addCircleShape(olpt, radiusM);
            mapBounds.extend(olpt);
            mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM,   0.0)));
            mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM, 180.0)));

        }

        /* center */
        var centerPt   = mapBounds.getCenterLonLat();
        var zoomFactor = map.getZoomForExtent(mapBounds);
        map.setCenter(centerPt, zoomFactor);

    }
    
    return null;
};

/**
*** Returns a circle shape (VEShape)
*** @param center   The center point (OpenLayers.LonLat) of the circle
*** @param radiusM  The radius of the circle in meters
*** @return The circle VEShape object
**/
JSMap.prototype._addCircleShape = function(center, radiusM)
{

    /* save center */
    this.geozoneCenter = center;  // OpenLayers.LonLat

    /* circle style */
    var circleStyle = {
        strokeColor:   "#11CC11",
        strokeOpacity: 0.8,
        strokeWidth:   1,
        fillColor:     "#11CC11",
        fillOpacity:   0.2
    };

    /* Circle shape */
    var circlePoints = this._getCirclePoints(center, radiusM);
    var circleShape = new OpenLayers.Geometry.LinearRing(circlePoints);
    this.olZoneFeature = [ new OpenLayers.Feature.Vector(circleShape, null, circleStyle) ];
    this.olDrawLayer.addFeatures(this.olZoneFeature);

};

/**
*** Returns an array of points (OpenLayers.LonLat) representing a circle polygon
*** @param center   The center point (OpenLayers.LonLat) of the circle
*** @param radiusM  The radius of the circle in meters
*** @return An array of points (OpenLayers.LonLat) representing a circle polygon
**/
JSMap.prototype._getCirclePoints = function(center, radiusM)
{
    var jspt = this._toJSMapPoint(center);
    var rLat = geoRadians(jspt.lat);   // radians
    var rLon = geoRadians(jspt.lon);   // radians
    var d    = radiusM / EARTH_RADIUS_METERS;
    var circlePoints = new Array();
    for (x = 0; x <= 360; x += 12) {
        var xrad = geoRadians(x);
        var tLat = Math.asin(Math.sin(rLat) * Math.cos(d) + Math.cos(rLat) * Math.sin(d) * Math.cos(xrad));
        var tLon = rLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(rLat), Math.cos(d) - Math.sin(rLat) * Math.sin(tLat));
        var olpt = this._toOpenLayerPointLatLon(geoDegrees(tLat),geoDegrees(tLon));
        circlePoints.push(new OpenLayers.Geometry.Point(olpt.lon, olpt.lat));

    }
    return circlePoints;
};

JSMap.prototype._calcRadiusPoint = function(center, radiusM, heading)
{
    var cpt   = center; // JSMapPoint
    var crLat = geoRadians(cpt.lat);          // radians
    var crLon = geoRadians(cpt.lon);          // radians
    var d     = radiusM / EARTH_RADIUS_METERS;
    var xrad  = geoRadians(heading);            // radians
    var rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
    var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
    return new JSMapPoint(geoDegrees(rrLat), geoDegrees(rrLon));
};

/**
*** Convert a JSMapPoint to an OpenLayers.LonLat
*** @param point  The JSMapPoint instance
*** @return An OpenLayers.LonLat instance
**/
JSMap.prototype._toOpenLayerPoint = function(point)
{
    return this._toOpenLayerPointLatLon(point.lat, point.lon);
};

/**
*** Convert a Lat/Lon to an OpenLayers.LonLat
*** @param lat  The Latitude
*** @param lat  The Longitude
*** @return An OpenLayers.LonLat instance
**/
JSMap.prototype._toOpenLayerPointLatLon = function(lat, lon)
{
    var ollon = lon * (20037508.34 / 180.0);
    var ollat = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
    ollat *= 20037508.34 / 180.0;
    return new OpenLayers.LonLat(ollon, ollat);
};

/**
*** Convert an OpenLayers.LonLat to a JSMapPoint
*** @param point  The OpenLayers.LonLat instance
*** @return A JSMapPoint instance
**/
JSMap.prototype._toJSMapPoint = function(point)
{
    var lon = point.lon * (180.0 / 20037508.34);
    var lat = point.lat * (180.0 / 20037508.34);
    lat = (180.0 / Math.PI) * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - (Math.PI / 2.0));
    return new JSMapPoint(lat,lon);
};

// ----------------------------------------------------------------------------
