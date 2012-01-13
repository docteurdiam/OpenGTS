// ----------------------------------------------------------------------------
// To provide some additional features for OpenLayers, portions of the OpenLayers support
// have been copied from 'mapstraction.js' [www.mapstraction.com] which includes the 
// copyright included below.
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Copyright (c) 2006-7, Tom Carden, Steve Coast, Mikel Maron, Andrew Turner, Henri Bergius
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without modification, are  
// permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice, this list of  
//   conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright notice, this list of  
//   conditions and the following disclaimer in the documentation and/or other materials 
//   provided with the distribution.
// * Neither the name of the Mapstraction nor the names of its contributors may be used to  
//   endorse or promote products derived from this software without specific prior written 
//   permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
// COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF  
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// ----------------------------------------------------------------------------
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
// UserAgents:
//  - "/MSIE/.test(navigator.userAgent)"
//  - "/Firefox/.test(navigator.userAgent)"
//  - "/Safari/.test(navigator.userAgent)"
// ----------------------------------------------------------------------------
// References:
//  - http://openlayers.org/dev/examples/
//      OpenLayers Modify Feature Example
//      Popup Matrix
//      Drawing Simple Vector Features Example
//      Wrapping the Date Line
//      OpenLayers.Handler.Point
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/07/27  Martin D. Flynn
//     -Modified 'JSMap.prototype._addPushpin' to set the proper 'iconAnchor' value
//  2008/08/08  Martin D. Flynn
//     -Added support for Geozones
//  2008/08/15  Martin D. Flynn
//     -Many changes to make this work better with MS IE.
//  2008/08/24  Martin D. Flynn
//     -Added 'replay' support.
//  2008/09/01  Martin D. Flynn
//     -Modified Geozome mouse behavior (click to recenter, shift-drag to resize).
//  2008/10/16  Martin D. Flynn
//     -Initial support for GeoServer
//  2008/12/01  Martin D. Flynn
//     -Removed the 'short' zoom control display (both the zoom-drag bar and the
//      short zoom bar were previously displayed on the map simultaneously).
//  2009/08/23  Martin D. Flynn
//     -Added color argument to JSDrawRoute
//     -Added option for drawing multiple points per device on fleet map
//  2010/04/11  Martin D. Flynn
//     -Added support for drawing polygon and corridor geozones.  However, in the
//      case of corridor geozones, additional GTS features may be required to
//      fully utilize this type of geozone.
//  2011/01/28  Martin D. Flynn
//     -Apply minimum zoom (point range) when updating a single point on the map.
//  2011/10/03  Martin D. Flynn / YanXu("stelzbock" SourceForge)
//     -Applied changes to fix memory leak (thanks to YanXu "stelzbock" SourceForge)
// ----------------------------------------------------------------------------

var DRAG_NONE               = 0x00;
var DRAG_RULER              = 0x01;
var DRAG_GEOZONE            = 0x10;
var DRAG_GEOZONE_CENTER     = 0x11;
var DRAG_GEOZONE_RADIUS     = 0x12;

// ----------------------------------------------------------------------------

function OpenLayersColorStyle(borderColor, borderOpacity, fillColor, fillOpacity)
{
    this.strokeColor   = borderColor;
    this.strokeOpacity = borderOpacity;
    this.strokeWidth   = 1;
    this.fillColor     = fillColor;
    this.fillOpacity   = fillOpacity;
};

var GEOZONE_STYLE = [
    new OpenLayersColorStyle("#CC1111", 0.80, "#11CC22", 0.28), /* primary */
    new OpenLayersColorStyle("#11CC11", 0.80, "#11CC22", 0.18)
    ];
    
function GetGeozoneStyle(isPrimary, fillColor)
{
    var s = GEOZONE_STYLE[isPrimary? 0 : 1];
    if (fillColor && (fillColor != "")) {
        return new OpenLayersColorStyle(
            s.strokeColor, s.strokeOpacity,
            fillColor, s.fillOpacity);
    } else {
        return s;
    }
};
    
//var DRAW_ZONE_POINT_RADIUS_POLYGON = false;

// ----------------------------------------------------------------------------

/**
*** JSMap constructor
**/
function JSMap(mapElement)
{

    /* custom fix for MSIE */
    this.userAgent_MSIE = /MSIE/.test(navigator.userAgent);

    /* crosshair mouse cursor */
    mapElement.style.cursor = "crosshair";

    /* OpenStreetMaps */
    if (GEOSERVER_enable) {
        this._initGeoServerCustom(mapElement);
    } else
    if (SITIMAPA_enable) {
        this._initSitiMapaCustom(mapElement);
    } else {
        this._initOpenStreetMaps(mapElement);
    }

    /* pan/zoom bar */
    //this.openLayersMap.addControl(new OpenLayers.Control.PanZoomBar());

    /* disable double-click zoom */
    // this seems to also disable the shift-drag zoom.
    var nav = new OpenLayers.Control.Navigation({ 
        defaultDblClick: function(e) {/*no-op*/} 
    });
    this.openLayersMap.addControl(nav);
    
    /* map layers */
    /*
    this.openLayersMap.addLayers([
        new OpenLayers.Layer.OSM("Cabg", "./tiles/${z}/${x}/${y}.png", {numZoomLevels: 19, displayInLayerSwitcher: true }),
        new OpenLayers.Layer.TMS( "Mapa General", "http://tile.openstreetmap.org/",
          { 
            type: 'png', getURL: osm_getTileURL, displayOutsideMaxExtent: true, attribution: 'Map data &copy; <a href="http://www.openstreetmap.org/">OpenStreetMap</a> and contributors <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>',
            opacity: 0.5, isBaseLayer: false, visibility: false, numZoomLevels:19 
          } )
        ]);
        */

    /* "ruler" layer */
    this.rulerFeatures = null;
    this.rulerLayer = new OpenLayers.Layer.Vector('rulerLayer');
    this.openLayersMap.addLayer(this.rulerLayer);

    /* POI layer */
    this.poiLayer = new OpenLayers.Layer.Markers('poiLayer');
    this.openLayersMap.addLayer(this.poiLayer);

    /* route/zone layer */
    this.routeLines    = [];        // JSMapPoint[]
    this.primaryIndex  = -1;
    this.primaryCenter = null;      // OpenLayers.LonLat
    this.geozonePoints = null;      // JSMapPoint[]
    this.dragZoneOffsetLat = 0.0;
    this.dragZoneOffsetLon = 0.0;
    this.drawFeatures = null;
    this.drawShapes = [];
    this.drawLayer = [];
    // drawLayer0
    this.drawLayer.push(new OpenLayers.Layer.Vector('drawLayer0'));
    this.openLayersMap.addLayer(this.drawLayer[this.drawLayer.length - 1]);
    // drawLayer1
    //this.drawLayer.push(new OpenLayers.Layer.Vector('drawLayer0'));
    //this.openLayersMap.addLayer(this.drawLayer[this.drawLayer.length - 1);

    /* marker/pushpin layer */
    this.markerLayer = new OpenLayers.Layer.Markers('markerLayer');
    this.openLayersMap.addLayer(this.markerLayer);
    this.visiblePopupInfoBox = null; // JSMapPushpin
	this.popups = []; // ID: 3401674

    /* replay vars */
    this.replayTimer = null;
    this.replayIndex = 0;
    this.replayInterval = (REPLAY_INTERVAL < 100)? 100 : REPLAY_INTERVAL;
    this.replayInProgress = false;
    this.replayPushpins = [];

    /* mouse event handlers */
    this.dragType = DRAG_NONE;
    this.dragRulerStart = null;
    this.dragRulerEnd = null;
    try {
        this.openLayersMap.events.registerPriority("mousemove", this, this._event_OnMouseMove );
        this.openLayersMap.events.registerPriority("mousedown", this, this._event_OnMouseDown );
        this.openLayersMap.events.registerPriority("mouseup"  , this, this._event_OnMouseUp   );
    } catch (e) {
        //alert("Error: " + e);
    }
    
    /* "click" handler to recenter geozones */
    try {
        this.openLayersMap.events.register("click", this, this._event_OnClick);
    } catch (e) {
        //alert("Error: " + e);
    }

    /* init lat/lon display */
    jsmSetLatLonDisplay(0,0);

    /* zoom event */
    this.lastMapZoom = 0;
    this.lastMapSize = new OpenLayers.Size(0,0);
    try {
        //this.openLayersMap.events.register("zoomend", this, this._event_ZoomEnd);
        this.openLayersMap.events.register("moveend", this, this._event_MoveEnd);
    } catch (e) {
        //alert("Error: " + e);
    }

};

// ----------------------------------------------------------------------------

/* init OpenLayers with OpenStreetMaps */
JSMap.prototype._initOpenStreetMaps = function(mapElement)
{

    /* bounds */
    var bounds = new OpenLayers.Bounds(
        -20037508.34, -20037508.34,
         20037508.34,  20037508.34
    );

    /* see "http://wiki.openstreetmap.org/index.php/OpenLayers_Simple_Example" */
    this.openLayersMap = new OpenLayers.Map(mapElement.id, 
        {
            maxExtent:     bounds, 
            maxResolution: 156543, 
            numZoomLevels: 18, 
            units:         "meters", 
            projection:    "EPSG:41001",
            controls: [
              //new OpenLayers.Control.MousePosition(),
              //new OpenLayers.Control.OverviewMap(),
              //new OpenLayers.Control.ScaleLine(),
              //new OpenLayers.Control()
                new OpenLayers.Control.PanZoomBar()
            ]
        }
    );

    this.openLayersMap.addLayer(new OpenLayers.Layer.TMS(
        "OSM Mapnik", 
        [    
            "http://a.tile.openstreetmap.org/",
            "http://b.tile.openstreetmap.org/",
            "http://c.tile.openstreetmap.org/"
        ], 
        {
            type: 'png', 
            getURL: function (bounds) {
                var res = this.map.getResolution();
                var x = Math.round((bounds.left - this.maxExtent.left) / (res * this.tileSize.w));
                var y = Math.round((this.maxExtent.top - bounds.top) / (res * this.tileSize.h));
                var z = this.map.getZoom();
                var limit = Math.pow(2, z);    
                if ((y < 0) || (y >= limit)) {
                    return null;
                } else {
                    x = ((x % limit) + limit) % limit;
                    var path = z + "/" + x + "/" + y + "." + this.type; 
                    var url = this.url;
                    if (url instanceof Array) {
                        url = this.selectUrl(path, url);
                    }
                    return url + path;
                }
            }, 
            displayOutsideMaxExtent: true
        }
    ));

    this.openLayersMap.addLayer(new OpenLayers.Layer.TMS(
        "OSM", 
        [    
            "http://a.tah.openstreetmap.org/Tiles/tile.php/",
            "http://b.tah.openstreetmap.org/Tiles/tile.php/",
            "http://c.tah.openstreetmap.org/Tiles/tile.php/"
        ], 
        {
            type: 'png', 
            getURL: function (bounds) {
                var res = this.map.getResolution();
                var x = Math.round((bounds.left - this.maxExtent.left) / (res * this.tileSize.w));
                var y = Math.round((this.maxExtent.top - bounds.top) / (res * this.tileSize.h));
                var z = this.map.getZoom();
                var limit = Math.pow(2, z);    
                if ((y < 0) || (y >= limit)) {
                    return null;
                } else {
                    x = ((x % limit) + limit) % limit;
                    var path = z + "/" + x + "/" + y + "." + this.type; 
                    var url = this.url;
                    if (url instanceof Array) {
                        url = this.selectUrl(path, url);
                    }
                    return url + path;
                }
            }, 
            displayOutsideMaxExtent: true
        }
    ));

    /* convert OpenLayers point to JSMapPoint */
    JSMap.prototype._toJSMapPointLatLon = function(olLat, olLon) {
        var lon = olLon * (180.0 / 20037508.34);
        var lat = olLat * (180.0 / 20037508.34);
        lat = (180.0 / Math.PI) * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - (Math.PI / 2.0));
        return new JSMapPoint(lat,lon);
    };

    /* convert OpenLayers point to JSMapPoint */
    JSMap.prototype._toJSMapPoint = function(point) {
        return this._toJSMapPointLatLon(point.lat, point.lon);
    };

    /* convert JSMapPoint to OpenLayers point */
    JSMap.prototype._toOpenLayerPointLatLon = function(lat, lon) {
        var ollon = lon * (20037508.34 / 180.0);
        var ollat = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        ollat *= 20037508.34 / 180.0;
        return new OpenLayers.LonLat(ollon, ollat);
    };

    /* convert JSMapPoint to OpenLayers point */
    JSMap.prototype._toOpenLayerPoint = function(point) {
        return this._toOpenLayerPointLatLon(point.lat, point.lon);
    };

    /* return map object */
    return this.openLayersMap;

};

// ----------------------------------------------------------------------------

/* init OpenLayers with GeoServer */
JSMap.prototype._initGeoServerCustom = function(mapElement)
{
    var GEOSERVER_TITLE = GEOSERVER_title;
    var GEOSERVER_URL   = GEOSERVER_url;            // "http://localhost:8085/geoserver/wms";
    var MAX_RESOLUTION  = GEOSERVER_maxResolution;  // 0.0007907421875;
    var MAP_WIDTH       = GEOSERVER_size.width;     // "431";
    var MAP_HEIGHT      = GEOSERVER_size.height;    // "550";
    var MAP_STYLES      = "";
    var MAP_BOUNDS      = new OpenLayers.Bounds(GEOSERVER_bounds.left, GEOSERVER_bounds.bottom, GEOSERVER_bounds.right, GEOSERVER_bounds.top); // -74.047185, 40.679648, -73.907005, 40.882078);
    var TILE_ORIGIN     = MAP_BOUNDS.left + "," + MAP_BOUNDS.bottom; // "-74.047185,40.679648"
    var PROJECTION      = GEOSERVER_projection;     // "EPSG:4326";
    var TILE_FORMAT     = "image/png";
    var TILE_STATE      = "true";                   // may need to be "false" ???
    var DATA_LAYERS     = GEOSERVER_layers;         // "tiger-ny";
    var COORD_UNITS     = (GEOSERVER_units != "")? GEOSERVER_units : "degrees";
    var LAYER_TYPE      = GEOSERVER_layerType;

    /* map */
    this.openLayersMap = new OpenLayers.Map(
        mapElement.id, 
        {
            maxExtent:      MAP_BOUNDS,
            maxResolution:  MAX_RESOLUTION,
            projection:     PROJECTION,
            units:          COORD_UNITS,
            controls: [
              //new OpenLayers.Control.MousePosition(),
              //new OpenLayers.Control.OverviewMap(),
              //new OpenLayers.Control.ScaleLine(),
                new OpenLayers.Control.PanZoomBar()
            ]
        }
    );
    
    /* simple layer */
    if (LAYER_TYPE == "simple") {
        this.openLayersMap.addLayer(new OpenLayers.Layer.WMS(
            GEOSERVER_TITLE, 
            GEOSERVER_URL,
            {
                layers:     DATA_LAYERS
            }
        ));
    }

    /* tiled layer */
    if (LAYER_TYPE == "tiled") {
        this.openLayersMap.addLayer(new OpenLayers.Layer.WMS(
            GEOSERVER_TITLE, // + " (tiled)",
            GEOSERVER_URL,
            {
                width:       MAP_WIDTH,
                height:      MAP_HEIGHT,
                styles:      MAP_STYLES,
                layers:      DATA_LAYERS,
                srs:         PROJECTION,
                format:      TILE_FORMAT,
                tiled:       TILE_STATE,
                tilesOrigin: TILE_ORIGIN
            },
            { 
                buffer: 0 
            } 
        ));
    }

    /* untiled layer */
    if (LAYER_TYPE == "untiled") {
        this.openLayersMap.addLayer(new OpenLayers.Layer.WMS(
            GEOSERVER_TITLE, // + " (untiled)",
            GEOSERVER_URL,
            {
                width:  MAP_WIDTH,
                styles: MAP_STYLES,
                height: MAP_HEIGHT,
                layers: DATA_LAYERS,
                srs:    PROJECTION,
                format: TILE_FORMAT
            },
            { 
                singleTile: true, 
                ratio:      1 
            } 
        ));
    }
    
    /* initial zoom */
    this.openLayersMap.zoomToExtent(MAP_BOUNDS);

    /* convert OpenLayers point to JSMapPoint */
    JSMap.prototype._toJSMapPoint = function(point) {
        var lon = point.lon;
        var lat = point.lat;
        return new JSMapPoint(lat,lon);
    };

    /* convert JSMapPoint to OpenLayers point */
    JSMap.prototype._toOpenLayerPointLatLon = function(lat, lon) {
        var ollon = lon;
        var ollat = lat;
        return new OpenLayers.LonLat(ollon, ollat);
    };

    /* convert JSMapPoint to OpenLayers point */
    JSMap.prototype._toOpenLayerPoint = function(point) {
        return this._toOpenLayerPointLatLon(point.lat, point.lon);
    };

    /* return map object */
    return this.openLayersMap;

};

// ----------------------------------------------------------------------------

/* init OpenLayers with SitiMapa */
JSMap.prototype._initSitiMapaCustom = function(mapElement)
{

    /* external SitiMapa init */
    try { init(); } catch (e) { }

    /* map */
    this.openLayersMap = mapa; // "mapa" externally defined (TODO: make this more general)
    if (!this.openLayersMap) {
        alert("Map not defined!");
    }

    /* convert OpenLayers point to JSMapPoint */
    JSMap.prototype._toJSMapPointLatLon = function(olLat, olLon) {
        var lon = olLon * (180.0 / 20037508.34);
        var lat = olLat * (180.0 / 20037508.34);
        lat = (180.0 / Math.PI) * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - (Math.PI / 2.0));
        return new JSMapPoint(lat,lon);
    };

    /* convert OpenLayers point to JSMapPoint */
    JSMap.prototype._toJSMapPoint = function(point) {
        return this._toJSMapPointLatLon(point.lat, point.lon);
    };

    /* convert JSMapPoint to OpenLayers point */
    JSMap.prototype._toOpenLayerPointLatLon = function(lat, lon) {
        var ollon = lon * (20037508.34 / 180.0);
        var ollat = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        ollat *= 20037508.34 / 180.0;
        return new OpenLayers.LonLat(ollon, ollat);
    };

    /* convert JSMapPoint to OpenLayers point */
    JSMap.prototype._toOpenLayerPoint = function(point) {
        return this._toOpenLayerPointLatLon(point.lat, point.lon);
    };

    /* return map object */
    return this.openLayersMap;

};

// ----------------------------------------------------------------------------

/**
*** Unload/release resources
**/
JSMap.prototype.JSUnload = function()
{
    // nothing to do?
};

// ----------------------------------------------------------------------------

/**
*** Clear all pushpins and drawn lines
**/
JSMap.prototype.JSClearLayers = function()
{
    this._clearPoiLayer();
    this._clearMarkerLayer();
    this._clearDrawLayer();
    this._clearRulerLayer(true);
    this.centerBounds = new OpenLayers.Bounds();
    this.routeLines = [];
};

/**
*** Clear the POI layer
**/
JSMap.prototype._clearPoiLayer = function()
{
    if (this.poiLayer !== null) {
        try { this.poiLayer.clearMarkers(); } catch (e) {}
        try { this.openLayersMap.removeLayer(this.poiLayer); } catch (e) {}
    }
};

/**
*** Clear the marker layer
**/
JSMap.prototype._clearMarkerLayer = function()
{
    if (this.markerLayer !== null) {

        /* clear OpenLayer markers */
		try {
		    var markers = this.markerLayer.markers;
			if (markers !== null) {
				for (var i = 0; i < markers.length; i++) {
					/* This is necessary to remove the event listeners, too */
					markers[i].destroy();
				}
			}
		} catch (e) {/*ignore*/}

		/* clear popups */
		try {
			if (this.popups !== null) {
				for (var i = 0; i < this.popups.length; i++) {
					this.popups[i].destroy();
				}
				this.popups = [];
			}
		} catch (e) {/*ignore*/}

		/* clear marker layers */
        try { this.markerLayer.clearMarkers(); } catch (e) {}
        try { this.openLayersMap.removeLayer(this.markerLayer); } catch (e) {}

    }
    this._clearReplay();
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
        if (this.replayTimer === null) {
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
*** @param replay  0=off, 1=pushpin_only, 2=pushpin&balloon
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
    if (this.replayTimer !== null) { 
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
        if (this.replayTimer === null) {
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
*** Clear the draw layer
**/
JSMap.prototype._clearRulerLayer = function(clearStart)
{
    if ((this.rulerLayer !== null) && (this.rulerFeatures !== null)) {
        try { this.rulerLayer.removeFeatures(this.rulerFeatures); } catch (e) {}
        this.rulerFeatures = null;
    }
    if (clearStart) {
        this.dragRulerStart = null;
        this.dragRulerEnd = null;
    }
};

/**
*** Draw Ruler
**/
JSMap.prototype._drawRuler = function(features)
{
    this._clearRulerLayer(false);
    this.rulerFeatures = features;
    if (this.rulerFeatures !== null) {
        if (this.rulerLayer === null) {
            this.rulerLayer = new OpenLayers.Layer.Vector('rulerLayer');
            this.openLayersMap.addLayer(this.rulerLayer);
        }
        this.rulerLayer.addFeatures(this.rulerFeatures);
        //this.rulerLayer.display(true);
        //this.rulerLayer.drawFeature(this.rulerFeatures[0]);
    }
};

/**
*** Create/return ruler feature
**/
JSMap.prototype._createRulerFeature = function(lat, lon)
{
    if ((lat !== null) && (lon !== null)) {
        var rulerStyle = {
            strokeColor:   "#FF6422",
            strokeOpacity: 1,
            strokeWidth:   2,
            fillColor:     "#FF2222",
            fillOpacity:   0.2
        };
        var rulerPoints = [];
        rulerPoints.push(this._createGeometryPoint(this._toOpenLayerPoint(lat)));
        rulerPoints.push(this._createGeometryPoint(this._toOpenLayerPoint(lon)));
        var line = new OpenLayers.Geometry.LineString(rulerPoints);
        return new OpenLayers.Feature.Vector(line, null, rulerStyle);
    } else {
        return null;
    }
};

// ----------------------------------------------------------------------------

/**
*** Clear the draw layer
**/
JSMap.prototype._clearDrawLayer = function()
{
    if (this.drawFeatures !== null) {
        try { this.drawLayer[0].removeFeatures(this.drawFeatures); } catch (e) {}
        this.drawFeatures = null;
    }
};

/**
*** Draw Feature
**/
JSMap.prototype._drawFeatures = function(clear, features)
{

    /* clear existing features */
    if (clear) {
        this._clearDrawLayer();
    }

    /* add features */
    if (features) {
        if (!this.drawFeatures) { this.drawFeatures = []; }
        for (var i = 0; i < features.length; i++) {
            this.drawFeatures.push(features[i]);
        }
    }

    /* draw features */
    if (this.drawFeatures && (this.drawFeatures.length > 0)) {
        //if (this.drawLayer === null) {
        //    this.drawLayer = new OpenLayers.Layer.Vector('drawLayer0');
        //    this.openLayersMap.addLayer(this.drawLayer);
        //}
        this.drawLayer[0].addFeatures(this.drawFeatures);
        //this.drawLayer[0].display(true);
        //this.drawLayer[0].drawFeature(this.drawFeatures[0]);
    }
    
};

// ----------------------------------------------------------------------------

/**
*** Sets the center of the map
**/
JSMap.prototype.JSSetCenter = function(center, zoom)
{
    try {
        var opt = this._toOpenLayerPoint(center);
        if (!zoom || (zoom == 0)) {
            this.openLayersMap.setCenter(opt);
        } else
        if (zoom > 0) {
            this.openLayersMap.setCenter(opt, zoom);
        } else {
            var ob = new OpenLayers.Bounds();
            ob.extend(opt);
            zoom = this.openLayersMap.getZoomForExtent(ob);
            this.openLayersMap.setCenter(opt, zoom);
        }
    } catch (e) {
        //
    }
};

/**
*** Draw the specified pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
*** @param recenter  0=no-recenter, 1=last-pushpin, 2=all-pushpins
*** @param replay    0=off, 1=pushpin_only, 2=pushpin&balloon
**/
JSMap.prototype.JSDrawPushpins = function(pushPins, recenterMode, replay)
{

    /* there are no pushpins in geozone mode */
    if (jsvGeozoneMode) {
        return;
    }

    /* reset pushpin layer */
    //this.markerLayer.display(false);
    this._hidePushpinPopup(this.visiblePopupInfoBox);
    
    /* drawn pushpins */
    var drawPushpins = [];
    
    /* make sure we have a bounding box instance */
    if (!this.centerBounds) {
        this.centerBounds = new OpenLayers.Bounds();
    }

    /* recenter map on points */
    var pointCount = 0;
    if ((pushPins !== null) && (pushPins.length > 0)) {
        // extend bounding box around displayed pushpins
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            if ((pp.lat != 0.0) || (pp.lon != 0.0)) {
                pointCount++;
                this.centerBounds.extend(this._toOpenLayerPoint(pp));
                drawPushpins.push(pp);
            }
        }
        // make sure points span a minimum distance top to bottom
        var rangeRadiusM = 400; // TODO: should make this a configurable options
        var cenPt = this._toJSMapPoint(this.centerBounds.getCenterLonLat());
        var topPt = geoRadiusPoint(cenPt.lat, cenPt.lon, rangeRadiusM,   0.0); // top
        this.centerBounds.extend(this._toOpenLayerPoint(topPt));
        var botPt = geoRadiusPoint(cenPt.lat, cenPt.lon, rangeRadiusM, 180.0); // bottom
        this.centerBounds.extend(this._toOpenLayerPoint(botPt));
    }
    if (recenterMode > 0) {
        try {
            if (pointCount <= 0) {
                var centerPt   = this._toOpenLayerPoint(DEFAULT_CENTER);
                var zoomFactor = DEFAULT_ZOOM;
                this.openLayersMap.setCenter(centerPt, zoomFactor);
            } else 
            if (recenterMode == RECENTER_LAST) { // center on last point
                var pp         = drawPushpins[drawPushpins.length - 1];
                var centerPt   = this._toOpenLayerPoint(pp);
                this.openLayersMap.setCenter(centerPt);
            } else 
            if (recenterMode == RECENTER_PAN) { // pan to last point
                var pp         = drawPushpins[drawPushpins.length - 1];
                var centerPt   = this._toOpenLayerPoint(pp);
                this.openLayersMap.setCenter(centerPt);
            } else {
                var centerPt   = this.centerBounds.getCenterLonLat();
                var zoomFactor = this.openLayersMap.getZoomForExtent(this.centerBounds);
                this.openLayersMap.setCenter(centerPt, zoomFactor);
            }
        } catch (e) {
            alert("Error: [JSDrawPushpins] (pointCount="+pointCount+", recenterMode="+recenterMode+") " + e);
            return;
        }
    }
    if (pointCount <= 0) {
        return;
    }

    /* replay pushpins? */
    if (replay && (replay >= 1)) {
        this.openLayersMap.addLayer(this.markerLayer);   // must re-add layer
        this.replayIndex = 0;
        this.replayInProgress = true;
        this.replayPushpins = drawPushpins;
        this._startReplayTimer(replay, 100);
        return;
    }

    /* draw pushpins now */
    var pushpinErr = null;
    for (var i = 0; i < drawPushpins.length; i++) {
        var pp = drawPushpins[i]; // JSMapPushpin
        try {
            pp.hoverPopup = true;
            this._addPushpin(pp, this.markerLayer);
        } catch (e) {
            if (pushpinErr === null) { pushpinErr = e; }
        }
    }
    try { 
        this.openLayersMap.addLayer(this.markerLayer);  // must re-add layer
        //this.markerLayer.display(true);
    } catch (e) {
        if (pushpinErr === null) { pushpinErr = e; }
    }
    if (pushpinErr !== null) {
        alert("Error: adding pushpins:\n" + pushpinErr);
    }

};

/**
*** Draw the specified PointsOfInterest pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
**/
JSMap.prototype.JSDrawPOI = function(pushPins)
{

    /* reset pushpin layer */
    this._clearPoiLayer();
    //this.poiLayer.display(false);
    this._hidePushpinPopup(this.visiblePopupInfoBox);

    /* draw pushpins */
    if ((pushPins !== null) && (pushPins.length > 0)) {
        var pushpinErr = null;
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            try {
                pp.hoverPopup = true;
                this._addPushpin(pp, this.poiLayer);
            } catch (e) {
                if (pushpinErr === null) { pushpinErr = e; }
            }
        }
        try { 
            this.openLayersMap.addLayer(this.poiLayer);  // must re-add layer
            //this.poiLayer.display(true);
        } catch (e) {
            if (pushpinErr === null) { pushpinErr = e; }
        }
        if (pushpinErr !== null) {
            alert("Error: adding pushpins:\n" + pushpinErr);
        }
    }

};

/**
*** Adds a single pushpin to the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._addPushpin = function(pp, layer)
{
    try {
        var self = this;

        pp.map = this.openLayersMap;

        var ppMarker = null;
        if (pp.iconUrl) {
            var iSize   = new OpenLayers.Size(pp.iconSize[0], pp.iconSize[1]);
            var iAnchor = new OpenLayers.Pixel(-pp.iconHotspot[0], -pp.iconHotspot[1]);
            var iIcon   = new OpenLayers.Icon(pp.iconUrl, iSize, iAnchor);
            ppMarker    = new OpenLayers.Marker(this._toOpenLayerPoint(pp), iIcon);
        }
        
        var bgMarker = null;
        if (pp.bgUrl) {
            var bSize   = new OpenLayers.Size(pp.bgSize[0], pp.bgSize[1]);
            var bAnchor = new OpenLayers.Pixel(-pp.bgOffset[0], -pp.bgOffset[1]);
            var bIcon   = new OpenLayers.Icon(pp.bgUrl, bSize, bAnchor);
            bgMarker    = new OpenLayers.Marker(this._toOpenLayerPoint(pp), bIcon);
        }

        if (pp.html) {
            if (SITIMAPA_enable && false) {
                var olp = this._toOpenLayerPoint(pp);
                var lat = olp.lat; // 4.6815
                var lon = olp.lon; // -74.08506
                this.openLayersMap.adicionarPunto(
                    lon, lat, pp.html,
                    ''/*title*/,0,0,
                    'OpenLayers.Feature.Vector_999',
                    true,false,false);
            } else {
                pp.popup = new OpenLayers.Popup(null,
                    this._toOpenLayerPoint(pp),
                    new OpenLayers.Size(100,100),
                    pp.html,
                    true);
                pp.popup.autoSize = true;
                if (pp.hoverPopup) {
                    ppMarker.events.register("mouseover", ppMarker, function(event) {
                        //alert("Over pushpin!");
                        self._showPushpinPopup(pp);
                    });
                    ppMarker.events.register("mouseout", ppMarker, function(event) {
                        self._hidePushpinPopup(pp);
                    });
                    //var isFleet = pp.evRcd.isFleet;
                    //var typeId = pp.evRcd.typeID;
                    //if(isFleet && isFleet==="true" && typeId) {
                    //    ppMarker.events.register("click", ppMarker, function(event) {
                    //      location.href = './Track?page=map.device&device='+typeId;
                    //    });
                    //}
                } else {
                    ppMarker.events.register("mousedown", ppMarker, function(event) {
                        if (pp.popupShown) {
                            self._hidePushpinPopup(pp);
                        } else {
                            self._showPushpinPopup(pp);
                        }
                    });
                }
				this.popups.push(pp.popup);
            }
        }
        
        if (bgMarker) {
            pp.bgMarker = bgMarker;
            layer.addMarker(bgMarker);
        }

        pp.marker = ppMarker;
        layer.addMarker(ppMarker);

        /*
        if (pp.label) {
            var labelPopup = new OpenLayers.Popup(null,
                this._toOpenLayerPoint(pp),
                new OpenLayers.Size(30,12),
                "<span>"+pp.label+"</span>",
                false);
            labelPopup.autoSize = true;
            this.openLayersMap.addPopup(labelPopup);
            labelPopup.show();
        }
        */

    } catch(e) {
        //alert("Error: " + e);
    }
};

/**
*** Replays the list of pushpins on the map
*** @param replay  0=off, 1=pushpin_only, 2=pushpin&balloon
**/
JSMap.prototype._replayPushpins = function(replay)
{
    
    /* no replay pushpins? */
    if (this.replayPushpins === null) {
        this._clearReplay();
        jsmHighlightDetailRow(-1, false);
        return; // stop
    }

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
            try { this.markerLayer.clearMarkers(); } catch (e) {}
        }
        this._addPushpin(pp, this.markerLayer);
        if (replay && (replay >= 2)) {
            this._showPushpinPopup(pp);
        } else {
            jsmHighlightDetailRow(pp.rcdNdx, true);
        }
        this._startReplayTimer(replay, this.replayInterval);
    } catch (e) {
        // ignore
    }

};

// ----------------------------------------------------------------------------

/**
*** This method should cause the info-bubble popup for the specified pushpin to display
*** @param pp   The JSMapPushpin object to popup its info-bubble
**/
JSMap.prototype.JSShowPushpin = function(pp, center)
{
    if (pp) {
        if (pp.popupShown) {
            this._hidePushpinPopup(pp);
        } else {
            if (center || !this._isPointOnMap(pp.lat,pp.lon,7,7,50,100)) {
                this.JSSetCenter(new JSMapPoint(pp.lat, pp.lon));
            }
            this._showPushpinPopup(pp);
        }
    }
};

JSMap.prototype._isPointOnMap = function(lat, lon, margTop, margLeft, margBott, margRght)
{
    var size  = this.openLayersMap.getSize();
    var top   = 0             + margTop;
    var left  = 0             + margLeft;
    var bott  = top  + size.h - margBott;
    var rght  = left + size.w - margRght;
    var TL    = this._toJSMapPoint(this.openLayersMap.getLonLatFromViewPortPx(new OpenLayers.Pixel(left, top )));
    var BR    = this._toJSMapPoint(this.openLayersMap.getLonLatFromViewPortPx(new OpenLayers.Pixel(rght, bott)));
    //alert("_isPointOnMap "+lat+"/"+lon+", TL:"+TL.lat+"/"+TL.lon+", BR:"+BR.lat+"/"+BR.lon);
    if ((lat > TL.lat) || (lat < BR.lat)) {
        return false;
    } else
    if ((lon < TL.lon) || (lon > BR.lon)) {
        return false;
    } else {
        return true;
    }
};

JSMap.prototype._showPushpinPopup = function(pp)
{
    this._hidePushpinPopup(this.visiblePopupInfoBox);
    if (pp && !pp.popupShown && pp.map) {
        pp.map.addPopup(pp.popup);
        pp.popup.show();
        pp.popupShown = true;
        this.visiblePopupInfoBox = pp;
        jsmHighlightDetailRow(pp.rcdNdx, true);
    } else {
        this.visiblePopupInfoBox = null;
    }
};

JSMap.prototype._hidePushpinPopup = function(pp)
{
    if (pp && pp.popupShown) {
        pp.popup.hide();
        pp.map.removePopup(pp.popup);
        pp.popupShown = false;
        jsmHighlightDetailRow(pp.rcdNdx, false);
    }
};

// ----------------------------------------------------------------------------

/**
*** Draws a line between the specified points on the map.
*** @param points   An array of JSMapPoint objects
**/
JSMap.prototype.JSDrawRoute = function(points, color)
{
    if ((points !== null) && (points.length > 0)) {
        var route = {
            points: points,
            color:  color
        };
        this.routeLines.push(route);
        var routeFeatures = [];
        for (var i = 0; i < this.routeLines.length; i++) {
            var r = this.routeLines[i];
            routeFeatures.push(this._createRouteFeature(r.points,r.color));
        }
        this._drawFeatures(true, routeFeatures);
    } else {
        //this.routeLines = [];
        //this._clearDrawLayer();
    }
};

/**
*** Create/Return route feature
**/
JSMap.prototype._createRouteFeature = function(points, color) // JSMapPoint
{
    if ((points !== null) && (points.length > 0)) {
        var routeStyle = {
            strokeColor:   color,
            strokeOpacity: 1,
            strokeWidth:   2,
            fillColor:     color,
            fillOpacity:   0.2
        };
        var rp = [];
        for (var i = 0; i < points.length; i++) {
            var olpt = this._toOpenLayerPoint(points[i]); // OpenLayers.LonLat
            rp.push(this._createGeometryPoint(olpt));
        }
        var line = new OpenLayers.Geometry.LineString(rp);
        return new OpenLayers.Feature.Vector(line, null, routeStyle);
    } else {
        return null;
    }
};

// ----------------------------------------------------------------------------

/**
*** Remove previously drawn shapes 
**/
JSMap.prototype._removeShapes = function()
{
    this._clearDrawLayer();
    this.drawShapes = [];
};

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

    /* zoom bounds */
    var mapBounds = zoomTo? new OpenLayers.Bounds() : null;

    /* color/style */
    var colorStyle = new OpenLayersColorStyle(color, 0.75, color, 0.08);

    /* draw shape */
    var didDrawShape = false;
    if (type == "circle") { // ZONE_POINT_RADIUS

        var circleList = [];
        for (var p = 0; p < verticePts.length; p++) {
            var jsPt    = verticePts[p]; // JSMapPoint
            var center  = this._toOpenLayerPoint(jsPt); // OpenLayers.LonLat
            var circleF = this._createCircleFeature(center, radiusM, colorStyle);
            if (mapBounds) { 
                mapBounds.extend(center);
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(jsPt, radiusM,   0.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(jsPt, radiusM,  90.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(jsPt, radiusM, 180.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(jsPt, radiusM, 270.0)));
            }
            this.drawShapes.push(circleF);
        }
        if (this.drawShapes.length > 0) {
            this._drawFeatures(false, this.drawShapes);
            didDrawShape = true;
        }
            
    } else 
    if (type == "rectangle") { // ZONE_BOUNDED_RECT

        if (verticePts.length >= 2) {

            /* create rectangle */
            var vp0   = verticePts[0];
            var vp1   = verticePts[1];
            var TL    = this._toOpenLayerPointLatLon(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var TR    = this._toOpenLayerPointLatLon(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var BL    = this._toOpenLayerPointLatLon(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var BR    = this._toOpenLayerPointLatLon(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var crPts = [ TL, TR, BR, BL, TL ];
            var poly  = this._createPolygonFeature(crPts, colorStyle);
            if (mapBounds) { for (var b = 0; b < crPts.length; b++) { mapBounds.extend(crPts[b]); } }
            this.drawShapes.push(poly);
            this._drawFeatures(false, this.drawShapes);
            didDrawShape = true;

        }
            
    } else 
    if (type == "polygon") { // ZONE_POLYGON
       
        if (verticePts.length >= 3) {

            var crPts = [];
            for (var p = 0; p < verticePts.length; p++) {
                var olPt = this._toOpenLayerPointLatLon(verticePts[p].lat, verticePts[p].lon);
                crPts.push(olPt);
                if (mapBounds) { mapBounds.extend(olPt); }
            }
            var poly  = this._createPolygonFeature(crPts, colorStyle);
            this.drawShapes.push(poly);
            this._drawFeatures(false, this.drawShapes);
            didDrawShape = true;

        }

    } else
    if (type == "corridor") { // ZONE_SWEPT_POINT_RADIUS

        // TODO: 

    } else
    if (type == "center") {

        if (mapBounds) {
            for (var p = 0; p < verticePts.length; p++) {
                var olPt = this._toOpenLayerPointLatLon(verticePts[p].lat, verticePts[p].lon);
                mapBounds.extend(olPt);
            }
            didDrawShape = true;
        }

    }

    /* center on shape */
    if (didDrawShape && zoomTo && mapBounds) {
        var centerPt   = mapBounds.getCenterLonLat(); // OpenLayers.LonLat
        var zoomFactor = this.openLayersMap.getZoomForExtent(mapBounds);
        try { this.openLayersMap.setCenter(centerPt, zoomFactor); } catch (e) { /*alert("Error[JSDrawGeozone]:"+e);*/ }
    }

    /* shape not supported */
    return didDrawShape;

};

// ----------------------------------------------------------------------------

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of JSMapPoints
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
    this._JSDrawGeozone(type, radiusM, points, color, primNdx, false)
};

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of JSMapPoints
*** @return An object representing the Circle.
**/
JSMap.prototype._JSDrawGeozone = function(type, radiusM, points, color, primNdx, isDragging)
{
    // (type ZONE_POINT_RADIUS may only be currently supported)

    /* Geozone mode */
    jsvGeozoneMode = true;

    /* remove old primary */
    if (!isDragging) { 
        this.primaryCenter = null;
        this.primaryIndex  = primNdx;
    }

    /* save geozone points */
    this.geozonePoints = points;

    /* no points? */
    if ((points === null) || (points.length <= 0)) {
        //alert("No Zone center!");
        this._clearDrawLayer();
        return null;
    }

    /* point-radius */
    if (type == ZONE_POINT_RADIUS) {

        /* adjust radius */
        if (isNaN(radiusM))              { radiusM = 5000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;

        /* draw points */
        var count = 0;
        var zoneFeatures = new Array();
        var mapBounds = new OpenLayers.Bounds();
        var polyPts = [];
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if (geoIsValid(c.lat,c.lon)) {
                var isPrimary = (i == primNdx);
                var center    = (isPrimary && isDragging)? this.primaryCenter : this._toOpenLayerPoint(c); // OpenLayers.LonLat
                var circStyle = GetGeozoneStyle(isPrimary,color);
                zoneFeatures.push(this._createCircleFeature(center, radiusM, circStyle));
                if (isPrimary && !isDragging) {
                    this.primaryCenter = center; // OpenLayers.LonLat
                }
                polyPts.push(center);
                mapBounds.extend(center);
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM,   0.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM,  90.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM, 180.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM, 270.0)));
                count++;
            }
        }
        //if (DRAW_ZONE_POINT_RADIUS_POLYGON && (polyPts.length >= 3)) {
        //    var style = new OpenLayersColorStyle("#1111CC", 0.15, "#1111CC", 0.04);
        //    zoneFeatures.push(this._createPolygonFeature(polyPts, style));
        //}

        /* center on geozone */
        if (!isDragging) {
            var centerPt   = this._toOpenLayerPoint(DEFAULT_CENTER); // OpenLayers.LonLat
            var zoomFactor = DEFAULT_ZOOM;
            if (count > 0) {
                centerPt   = mapBounds.getCenterLonLat(); // OpenLayers.LonLat
                zoomFactor = this.openLayersMap.getZoomForExtent(mapBounds);
            }
            try { this.openLayersMap.setCenter(centerPt, zoomFactor); } catch (e) { /*alert("Error[JSDrawGeozone]:"+e);*/ }
        }

        /* create zone feature */
        this._drawFeatures(true, zoneFeatures);

        /*
        var self = this;
        var m = new OpenLayers.Marker(olpt);
        this.markerLayer.addMarker(m);
        var dragMarkers = this.markerLayer;
        var dragging = false;
        m.events.register("mousedown", m, function(e) {
            dragging = true;
        });
        m.events.register("mousemove", m, function(e) {
            if (dragging) { 
                m.moveTo(self.openLayersMap.getLayerPxFromViewPortPx(e.xy)); 
            }
        });
        m.events.register("mouseup", m, function(e) {
            dragging = false;
        });
        */

        /*
        var controls = {
            drag: new OpenLayers.Control.DragMarker(dragMarkers, { 'onComplete': function() { alert('foo'); } })
        }
        for(var key in controls) {
            this.openLayersMap.addControl(controls[key]);
        }
        */

    } else
    if (type == ZONE_POLYGON) {

        /* set radius (should be about 30 pixels radius) */
        jsvZoneRadiusMeters = radiusM;

        /* draw points */
        var count = 0;
        var zoneFeatures = new Array();
        var mapBounds = new OpenLayers.Bounds();
        var polyPts = [];
        var polyPtPrim = -1;
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if ((c.lat != 0.0) || (c.lon != 0.0)) {
                var isPrimary = (i == primNdx);
                var center    = (isPrimary && isDragging)? this.primaryCenter : this._toOpenLayerPoint(c); // OpenLayers.LonLat
                if (isPrimary) {
                    this.primaryCenter = center; // OpenLayers.LonLat
                    polyPtsPrim = polyPts.length;
                }
                polyPts.push(center);
                mapBounds.extend(center);
                count++;
            }
        }
        if (polyPts.length >= 3) {
            zoneFeatures.push(this._createPolygonFeature(polyPts, GetGeozoneStyle(false,color)));
        }

        /* center on geozone */
        if (!isDragging) {
            var centerPt   = this._toOpenLayerPoint(DEFAULT_CENTER); // OpenLayers.LonLat
            var zoomFactor = DEFAULT_ZOOM;
            if (count > 0) {
                centerPt   = mapBounds.getCenterLonLat(); // OpenLayers.LonLat
                zoomFactor = this.openLayersMap.getZoomForExtent(mapBounds);
            }
            try { this.openLayersMap.setCenter(centerPt, zoomFactor); } catch (e) { /*alert("Error[JSDrawGeozone]:"+e);*/ }
        }
        
        /* current MPP */
        //var zoom = this.openLayersMap.getZoom();
        //var DPP  = this.openLayersMap.getResolution(); // degrees per pixel
        radiusM = 10.0 * this.openLayersMap.getResolution();
        //alert("Radius = "+radiusM + ", Zoom="+this.openLayersMap.getZoom() + ", Resolution="+this.openLayersMap.getResolution());
        jsvZoneRadiusMeters = radiusM;

        /* draw drag circles at vertices */
        for (var i = 0; i < polyPts.length; i++) {
            var center = polyPts[i]; // OpenLayers.LonLat
            var dragStyle = GetGeozoneStyle((i == polyPtsPrim), color);
            zoneFeatures.push(this._createCircleFeature(center, radiusM, dragStyle));
        }

        /* create zone feature */
        this._drawFeatures(true, zoneFeatures);

    } else
    if (type == ZONE_SWEPT_POINT_RADIUS) {

        /* adjust radius */
        if (isNaN(radiusM))              { radiusM = 1000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;

        /* draw vertices */
        var count = 0;
        var zoneFeatures = new Array();
        var mapBounds = new OpenLayers.Bounds();
        var polyPts = []; // OpenLayers.LonLat[]
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if ((c.lat != 0.0) || (c.lon != 0.0)) {
                var isPrimary = (i == primNdx);
                var center    = (isPrimary && isDragging)? this.primaryCenter : this._toOpenLayerPoint(c); // OpenLayers.LonLat
                var circStyle = GetGeozoneStyle(isPrimary,color);
                var circleF   = this._createCircleFeature(center, radiusM, circStyle);
                zoneFeatures.push(circleF);
                if (isPrimary && !isDragging) {
                    this.primaryCenter = center; // OpenLayers.LonLat
                }
                polyPts.push(center); // OpenLayers.LonLat
                mapBounds.extend(center);
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM,   0.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM,  90.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM, 180.0)));
                mapBounds.extend(this._toOpenLayerPoint(this._calcRadiusPoint(c, radiusM, 270.0)));
                count++;
            }
        }

        /* draw corridors */
        if (polyPts.length >= 2) {
            // routeline "_createRouteFeature"
            for (var i = 0; i < (polyPts.length - 1); i++) {
                var ptA = this._toJSMapPoint(polyPts[i  ]);
                var ptB = this._toJSMapPoint(polyPts[i+1]);
                var hAB = geoHeading(ptA.lat, ptA.lon, ptB.lat, ptB.lon) - 90.0; // perpendicular
                var rp1 = this._toOpenLayerPoint(this._calcRadiusPoint(ptA, radiusM, hAB        ));
                var rp2 = this._toOpenLayerPoint(this._calcRadiusPoint(ptB, radiusM, hAB        ));
                var rp3 = this._toOpenLayerPoint(this._calcRadiusPoint(ptB, radiusM, hAB + 180.0));
                var rp4 = this._toOpenLayerPoint(this._calcRadiusPoint(ptA, radiusM, hAB + 180.0));
                var rectPts = [ rp1, rp2, rp3, rp4 ];
                zoneFeatures.push(this._createPolygonFeature(rectPts, GetGeozoneStyle(false,color)));
            }
        }

        /* center on geozone */
        if (!isDragging) {
            var centerPt   = this._toOpenLayerPoint(DEFAULT_CENTER); // OpenLayers.LonLat
            var zoomFactor = DEFAULT_ZOOM;
            if (count > 0) {
                centerPt   = mapBounds.getCenterLonLat(); // OpenLayers.LonLat
                zoomFactor = this.openLayersMap.getZoomForExtent(mapBounds);
            }
            try { this.openLayersMap.setCenter(centerPt, zoomFactor); } catch (e) { /*alert("Error[JSDrawGeozone]:"+e);*/ }
        }

        /* create zone feature */
        this._drawFeatures(true, zoneFeatures);

    } else {
        
        alert("Geozone type not supported: " + type);
        
    }
    
    return null;
};

// ----------------------------------------------------------------------------

/**
*** Returns a circle shape (OpenLayers.Feature.Vector)
*** @param center   The center point (OpenLayers.LonLat) of the circle
*** @param radiusM  The radius of the circle in meters
*** @return The circle OpenLayers.Feature.Vector object
**/
JSMap.prototype._createCircleFeature = function(center, radiusM, circleStyle)
{
    if ((center !== null) && (radiusM > 0)) {
      //var circleShape  = OpenLayers.Geometry.Polygon.createRegularPolygpm(center, radiusM, 30, 0);
        var circlePoints = this._getCirclePoints(center, radiusM); // OpenLayers.Geometry.Point[]
        var circleShape  = new OpenLayers.Geometry.LinearRing(circlePoints);
        return new OpenLayers.Feature.Vector(circleShape, null, circleStyle);
    } else {
        return null;
    }
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
        circlePoints.push(this._createGeometryPoint(olpt));

    }
    return circlePoints;
};

/**
*** Calculate the lat/lon on the radius of the circle in the 'heading' direction
**/
JSMap.prototype._calcRadiusPoint = function(center/*JSMapPoint*/, radiusM, heading)
{
    var pt = geoRadiusPoint(center.lat, center.lon, radiusM, heading); // { lat: <>, lon: <> }
    return new JSMapPoint(pt.lat, pt.lon);
};

// ----------------------------------------------------------------------------

/**
*** Returns a polygon shape (OpenLayers.Feature.Vector)
*** @param vertices   An array of polygon vertice points (OpenLayers.LonLat) of the circle
*** @return The polygon OpenLayers.Feature.Vector object
**/
JSMap.prototype._createPolygonFeature = function(vertices, colorStyle)
{
    if ((vertices !== null) && (vertices.length >= 3)) {
        var polyStyle = colorStyle;
        var polyPoints = [];
        for (var i = 0; i < vertices.length; i++) {
            polyPoints.push(this._createGeometryPoint(vertices[i]));
        }
        var polyShape = new OpenLayers.Geometry.LinearRing(polyPoints);
        return new OpenLayers.Feature.Vector(polyShape, null, polyStyle);
    } else {
        return null;
    }
};

// ----------------------------------------------------------------------------

/**
*** Create/Adjust feature points 
*** @param olpt The OpenLayers.LonLat point
**/
JSMap.prototype._createGeometryPoint = function(olpt) // OpenLayers.LonLat
{
    /* This seems to be fixed (as of 2008/09/28)
    if (this.userAgent_MSIE) {
        // TODO: find out why this is needed and fix it at the source.
        var px = this.openLayersMap.getViewPortPxFromLonLat(olpt);
        px.x  -= (this.openLayersMap.size.w / 2);
        olpt   = this.openLayersMap.getLonLatFromViewPortPx(px);
    }
    */
    return new OpenLayers.Geometry.Point(olpt.lon, olpt.lat);
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Mouse modifier keys: e.shiftKey, e.altKey, e.ctrlKey
// Mouse buttons: OpenLayers.Event.isLeftClick(e)

/**
*** Retrun adjusted mouse cursor hotspot
**/
JSMap.prototype._mouseLocation = function(xy)
{
    if (this.userAgent_MSIE) {
        // the "crosshair" cursor hotpsot is off by 4px
        return new OpenLayers.Pixel(xy.x - 4, xy.y - 4);
    } else {
        return xy;
    }
};

/**
*** Mouse event handler to draw circles/lines on the map 
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseDown = function(e)
{
    
    /* quick exits */
    if (!OpenLayers.Event.isLeftClick(e) || e.altKey || (e.ctrlKey && e.shiftKey)) {
        return true;
    }

    /* mouse down point */
    var LL = this._toJSMapPoint(this.openLayersMap.getLonLatFromViewPortPx(this._mouseLocation(e.xy)));
    jsmapElem.style.cursor = 'crosshair';

    /* start distance ruler drag */
    if (e.ctrlKey) {
        this.dragType = DRAG_RULER;
        this._clearRulerLayer(true);
        this.dragRulerStart = LL; // JSMapPoint
        jsmSetDistanceDisplay(0);
        OpenLayers.Event.stop(e);
        return false;
    }

    /* geozone mode */
    if (jsvGeozoneMode && jsvZoneEditable) {
        var radiusM = zoneMapGetRadius(false);
        // check primary point
        if (this.primaryCenter !== null) {
            var CC = this._toJSMapPoint(this.primaryCenter);
            if (geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon) <= radiusM) {
                if (e.shiftKey) {
                    // resize
                    this.dragType = DRAG_GEOZONE_RADIUS;
                    this._clearRulerLayer(true);
                } else {
                    // inside geozone, move
                    this.dragType = DRAG_GEOZONE_CENTER;
                    this.dragZoneOffsetLat = LL.lat - CC.lat;
                    this.dragZoneOffsetLon = LL.lon - CC.lon;
                }
                OpenLayers.Event.stop(e);
                return false;
            }
        }
        // check other points
        if (!e.shiftKey && this.geozonePoints && (this.geozonePoints.length > 0)) {
            for (var i = 0; i < this.geozonePoints.length; i++) {
                if (geoDistanceMeters(this.geozonePoints[i].lat, this.geozonePoints[i].lon, LL.lat, LL.lon) <= radiusM) {
                    this.primaryIndex  = i;
                    this.primaryCenter = this.geozonePoints[i];
                    zoneMapSetIndex(this.primaryIndex,true);
                    this._JSDrawGeozone(jsvZoneType, jsvZoneRadiusMeters, this.geozonePoints, jsvZoneColor, this.primaryIndex, false);
                    // inside geozone, move
                    CC = this._toJSMapPoint(this.primaryCenter);
                    this.dragType = DRAG_GEOZONE_CENTER;
                    this.dragZoneOffsetLat = LL.lat - CC.lat;
                    this.dragZoneOffsetLon = LL.lon - CC.lon;
                    OpenLayers.Event.stop(e);
                    return false;
                }
            }
        }
    }

    this.dragType = DRAG_NONE;
    return true;
};

/**
*** Mouse event handler to draw circles on the map 
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseUp = function(e)
{

    /* geozone mode */
    if (jsvGeozoneMode && ((this.dragType & DRAG_GEOZONE) != 0)) {
        var CC      = this._toJSMapPoint(this.primaryCenter);
        var radiusM = zoneMapGetRadius(false);
        jsmSetPointZoneValue(CC.lat, CC.lon, radiusM);
        this.dragType = DRAG_NONE;
        mapProviderParseZones(jsvZoneList);
        OpenLayers.Event.stop(e);
        return false;
    }

    /* normal mode */
    this.dragType = DRAG_NONE;
    return true;
};

/**
*** Mouse event handler to detect lat/lon changes and draw circles/lines on the map 
*** @param e  The mouse event
**/
JSMap.prototype._event_OnMouseMove = function(e)
{
    var olpt = this.openLayersMap.getLonLatFromViewPortPx(this._mouseLocation(e.xy));
    if (!olpt) { return true; }
    var LL = this._toJSMapPoint(olpt);

    /* Latitude/Longitude change */
    jsmSetLatLonDisplay(LL.lat, LL.lon);
    jsmapElem.style.cursor = 'crosshair';

    /* distance ruler */
    if (this.dragType == DRAG_RULER) {
        this.dragRulerEnd = LL;
        var CC = this.dragRulerStart;
        jsmSetDistanceDisplay(geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon));
        this._drawRuler([ this._createRulerFeature(this.dragRulerStart, this.dragRulerEnd) ]);
        OpenLayers.Event.stop(e);
        return false;
    }

    /* geozone mode */
    if (this.dragType == DRAG_GEOZONE_RADIUS) {
        var CC = this._toJSMapPoint(this.primaryCenter);
        jsvZoneRadiusMeters = Math.round(geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon));
        if (jsvZoneRadiusMeters > MAX_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MAX_ZONE_RADIUS_M; }
        if (jsvZoneRadiusMeters < MIN_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MIN_ZONE_RADIUS_M; }
        var circleF = this._createCircleFeature(this.primaryCenter,jsvZoneRadiusMeters,GetGeozoneStyle(true,jsvZoneColor));
        if (circleF != null) {
            var features = [ circleF ];
            this._drawFeatures(true, features);
            jsmSetDistanceDisplay(jsvZoneRadiusMeters);
            //mapProviderParseZones(jsvZoneList);
        }
        OpenLayers.Event.stop(e);
        return false;
    }

    /* geozone mode */
    if (this.dragType == DRAG_GEOZONE_CENTER) {
        var CC = new JSMapPoint(LL.lat - this.dragZoneOffsetLat, LL.lon - this.dragZoneOffsetLon);
        this.primaryCenter = this._toOpenLayerPoint(CC);
        var REDRAW_GEOZONE = true;
        if (REDRAW_GEOZONE) {
            // redraw the entire Geozone
            this._JSDrawGeozone(jsvZoneType, jsvZoneRadiusMeters, this.geozonePoints, jsvZoneColor, this.primaryIndex, true);
            //mapProviderParseZones(jsvZoneList);
        } else {
            // just draw the single point-radius [zoneFeatures]
            var circleF = this._createCircleFeature(this.primaryCenter, jsvZoneRadiusMeters, GetGeozoneStyle(true,jsvZoneColor));
            if (circleF != null) {
                var features = [ circleF ];
                this._drawFeatures(true, features);
            }
        }
        OpenLayers.Event.stop(e);
        return false;
    }
    
    return true;

};

/**
*** Mouse event handler to recenter map
*** @param e  The mouse event
**/
JSMap.prototype._event_OnClick = function(e)
{

    /* geozone mode */
    if (jsvGeozoneMode && jsvZoneEditable && !e.ctrlKey && !e.shiftKey && !e.altKey) {
        var LL = this._toJSMapPoint(this.openLayersMap.getLonLatFromViewPortPx(this._mouseLocation(e.xy))); // where you clicked
        var CC = (this.primaryCenter !== null)? this._toJSMapPoint(this.primaryCenter) : new JSMapPoint(0.0,0.0); // where the primary center is
        var CCIsValid = geoIsValid(CC.lat,CC.lon);
        var CCLLDistKM = geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon);
        if (jsvZoneType == ZONE_POINT_RADIUS) {
            var radiusM = zoneMapGetRadius(false);
            // inside primary zone?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any zone?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    if (i == this.primaryIndex) { continue; }
                    var gpt = this.geozonePoints[i];
                    if (geoDistanceMeters(gpt.lat, gpt.lon, LL.lat, LL.lon) <= radiusM) {
                        return false; // inside this zone
                    }
                }
            }
            // outside geozone, recenter
            jsmSetPointZoneValue(LL.lat, LL.lon, radiusM);
            mapProviderParseZones(jsvZoneList);
            OpenLayers.Event.stop(e);
            return true;
        } else
        if (jsvZoneType == ZONE_POLYGON) {
            var radiusM = jsvZoneRadiusMeters; // vertice radius
            // inside primary vertice?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any vertice?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    if (i == this.primaryIndex) { continue; }
                    var gpt = this.geozonePoints[i];
                    if (geoDistanceMeters(gpt.lat, gpt.lon, LL.lat, LL.lon) <= radiusM) {
                        return false;
                    }
                }
            }
            // count number of valid points
            var count = 0;
            for (var x = 0; x < jsvZoneList.length; x++) {
                var pt = jsvZoneList[x];
                if (geoIsValid(pt.lat,pt.lon)) {
                    count++;
                }
            }
            if (count == 0) {
                // no valid points - create default polygon
                var radiusM = 450;
                var crLat   = geoRadians(LL.lat);  // radians
                var crLon   = geoRadians(LL.lon);  // radians
                for (x = 0; x < jsvZoneList.length; x++) {
                    var deg   = x * (360.0 / jsvZoneList.length);
                    var radM  = radiusM / EARTH_RADIUS_METERS;
                    if ((deg == 0.0) || ((deg > 170.0) && (deg < 190.0))) { radM *= 0.8; }
                    var xrad  = geoRadians(deg); // radians
                    var rrLat = Math.asin(Math.sin(crLat) * Math.cos(radM) + Math.cos(crLat) * Math.sin(radM) * Math.cos(xrad));
                    var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(radM) * Math.cos(crLat), Math.cos(radM)-Math.sin(crLat) * Math.sin(rrLat));
                    _jsmSetPointZoneValue(x, geoDegrees(rrLat), geoDegrees(rrLon), 0);
                }
            } else {
                // move valid points to new location
                var deltaLat = LL.lat - CC.lat;
                var deltaLon = LL.lon - CC.lon;
                for (var x = 0; x < jsvZoneList.length; x++) {
                    var pt = jsvZoneList[x];
                    if (geoIsValid(pt.lat,pt.lon)) {
                        _jsmSetPointZoneValue(x, (pt.lat + deltaLat), (pt.lon + deltaLon), 0);
                    }
                }
            }
            mapProviderParseZones(jsvZoneList);
            OpenLayers.Event.stop(e);
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
                    if (i == this.primaryIndex) { continue; }
                    var gpt = this.geozonePoints[i];
                    if (geoDistanceMeters(gpt.lat, gpt.lon, LL.lat, LL.lon) <= radiusM) {
                        return false;
                    }
                }
            }
            // count number of valid points
            var count = 0;
            var maxDistKM = 0.0;
            var lastPT = null;
            for (var x = 0; x < jsvZoneList.length; x++) {
                var pt = jsvZoneList[x];
                if (geoIsValid(pt.lat,pt.lon)) {
                    count++;
                    if (lastPT !== null) {
                        var dkm = geoDistanceMeters(lastPT.lat, lastPT.lon, pt.lat, pt.lon);
                        if (dkm > maxDistKM) {
                            maxDistKM = dkm;
                        }
                    } else {
                        lastPT = pt; // first valid point
                    }
                }
            }
            var maxDeltaKM = ((maxDistKM > 5000)? maxDistKM : 5000) * 1.5;
            if (!CCIsValid || (count <= 0) || (CCLLDistKM <= maxDeltaKM)) {
                jsmSetPointZoneValue(LL.lat, LL.lon, radiusM);
            }
            // reparse zone
            mapProviderParseZones(jsvZoneList);
            OpenLayers.Event.stop(e);
            return true;
        } else {
            return false;
        }
    }

};

// ----------------------------------------------------------------------------

/**
*** map zoomed
**/
JSMap.prototype._event_ZoomEnd = function()
{
    this._event_MoveEnd();
};

/**
*** map zoomed/panned
**/
JSMap.prototype._event_MoveEnd = function()
{
    if (this.userAgent_MSIE) {
        var zm = this.openLayersMap.getZoom();
        var sz = this.openLayersMap.getSize();
        if ((this.lastMapZoom != zm) || (sz.w != this.lastMapSize.w) || (sz.h != this.lastMapSize.h)) {
            // TODO: (see '_createGeometryPoint' above) This is necessary because everything seems to be 
            // shifted by MapWidth/2 pixels.
            if (jsvGeozoneMode) {
                // redraw the Geozone
                var circleF = this._createCircleFeature(this.primaryCenter, jsvZoneRadiusMeters, GetGeozoneStyle(true,jsvZoneColor));
                if (circleF != null) {
                    var features = [ circleF ];
                    this._drawFeatures(true, features);
                }
            } else
            if (this.routeLines && (this.routeLines.length > 0)) {
                // redraw the route
                var routeFeatures = [];
                for (var i = 0; i < this.routeLines.length; i++) {
                    var r = this.routeLines[i];
                    routeFeatures.push(this._createRouteFeature(r.points,r.color));
                }
                this._drawFeatures(true, routeFeatures);
            }
            if ((this.dragRulerStart !== null) && (this.dragRulerEnd !== null)) {
                // redraw the ruler
                this._drawRuler([ this._createRulerFeature(this.dragRulerStart, this.dragRulerEnd) ]);
            }
            this.lastMapZoom = zm;
            this.lastMapSize = sz;
        }
    }
};

// ----------------------------------------------------------------------------
