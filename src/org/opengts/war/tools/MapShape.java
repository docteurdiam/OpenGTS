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
//  2009/11/01  Martin D. Flynn
//     -Initial release (EXPERIMENTAL)
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;

public class MapShape
{

    // ------------------------------------------------------------------------
    
    public static final ShapeType       DEFAULT_SHAPE_TYPE  = ShapeType.CIRCLE;
    public static final ColorTools.RGB  DEFAULT_COLOR       = ColorTools.BLUE;

    // ------------------------------------------------------------------------

    /**
    *** Shape Types 
    **/
    public enum ShapeType implements EnumTools.IntValue {
        CIRCLE    (  0, "circle"   , I18N.getString(MapShape.class,"MapShape.type.circle"   ,"Circle"   )), // default
        RECTANGLE (  1, "rectangle", I18N.getString(MapShape.class,"MapShape.type.rectangle","Rectangle")),
        CORRIDOR  (  2, "corridor" , I18N.getString(MapShape.class,"MapShape.type.corridor" ,"Corridor" )),
        POLYGON   (  3, "polygon"  , I18N.getString(MapShape.class,"MapShape.type.polygon"  ,"Polygon"  )),
        LINE      ( 10, "line"     , I18N.getString(MapShape.class,"MapShape.type.line"     ,"Line"     )),
        CENTER    ( 98, "center"   , I18N.getString(MapShape.class,"MapShape.type.center"   ,"Center"   )),
        ZOOM      ( 99, "zoom"     , I18N.getString(MapShape.class,"MapShape.type.zoom"     ,"Zoom"     ));
        // ---
        private int         vv = 0;
        private String      nn = null;
        private I18N.Text   aa = null;
        ShapeType(int v, String n, I18N.Text a)     { vv = v; nn = n; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return nn; }
        public String  getDescription(Locale loc)   { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(CIRCLE); }
    };
    
    public static ShapeType getShapeType(String type, ShapeType dft)
    {
        return EnumTools.getValueOf(ShapeType.class, type, dft);
    }

    public static ShapeType getShapeType(String type)
    {
        return EnumTools.getValueOf(ShapeType.class, type, DEFAULT_SHAPE_TYPE);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                      name            = "";
    private String                      desc            = "";
    private ShapeType                   shapeType       = DEFAULT_SHAPE_TYPE;
    private double                      radiusMeters    = 0.0;
    private java.util.List<GeoPoint>    geoPoints       = null;
    private ColorTools.RGB              color           = DEFAULT_COLOR;
    private boolean                     zoomTo          = false;
    
    public MapShape(String name, ShapeType type, double radiusM, GeoPoint gp[])
    {
        this.setName(name);
        this.setType(type);
        this.setRadiusMeters(radiusM);
        this.setPoints(gp);
    }

    public MapShape(String name, ShapeType type, double radiusM, java.util.List<GeoPoint> gp)
    {
        this.setName(name);
        this.setType(type);
        this.setRadiusMeters(radiusM);
        this.setPoints(gp);
    }

    // ------------------------------------------------------------------------

    public void setName(String name)
    {
        this.name = StringTools.trim(name);
    }

    public String getName()
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    public void setDescription(String desc)
    {
        this.desc = StringTools.trim(desc);
    }

    public String getDescription()
    {
        return !StringTools.isBlank(this.desc)? this.desc : this.getName();
    }

    // ------------------------------------------------------------------------
    
    public void setType(ShapeType type)
    {
        this.shapeType = (type != null)? type : DEFAULT_SHAPE_TYPE;
    }

    public ShapeType getType()
    {
        return this.shapeType;
    }

    // ------------------------------------------------------------------------
    
    public void setRadiusMeters(double radius)
    {
        this.radiusMeters = radius;
    }

    public double getRadiusMeters()
    {
        return this.radiusMeters;
    }

    // ------------------------------------------------------------------------

    public void setPoints(GeoPoint gp[])
    {
        this.geoPoints = new Vector<GeoPoint>();
        ListTools.toList(gp, this.geoPoints);
    }
    
    public void setPoints(java.util.List<GeoPoint> gp)
    {
        this.geoPoints = new Vector<GeoPoint>();
        ListTools.toList(gp, this.geoPoints);
    }

    public java.util.List<GeoPoint> getPoints()
    {
        return this.geoPoints;
    }

    public String getPointsString()
    {
        return StringTools.join(this.getPoints(),",");
    }

    // ------------------------------------------------------------------------

    public String getColorString()
    {
        return this.getColor().toString(true);
    }

    public ColorTools.RGB getColor()
    {
        return (this.color != null)? this.color : DEFAULT_COLOR;
    }
    
    public void setColor(ColorTools.RGB color)
    {
        this.color = (color != null)? color : DEFAULT_COLOR;
    }
    
    public void setColor(String color)
    {
        this.setColor(ColorTools.parseColor(color,DEFAULT_COLOR));
    }

    // ------------------------------------------------------------------------

    public boolean isZoomTo()
    {
        return this.zoomTo;
    }

    public boolean getZoomTo()
    {
        return this.zoomTo;
    }

    public void setZoomTo(boolean zoom)
    {
        this.zoomTo = zoom;
    }
    
    // ------------------------------------------------------------------------

    public String toString()
    {
        return this.getName();
    }
    
    public String toLegacyZoomRegion()
    {
         StringBuffer sb = new StringBuffer();
         if (this.isZoomTo()) {
            
            /* type */
            switch (this.getType()) {
                case CIRCLE    : sb.append( "0,"); break;
                case RECTANGLE : sb.append( "1,"); break;
                case CORRIDOR  : sb.append( "2,"); break;
                case POLYGON   : sb.append( "3,"); break;
                case LINE      : sb.append( "9,"); break;
                case CENTER    : sb.append("-1,"); break;
            }
            
            /* radius */
            sb.append(Math.round(this.getRadiusMeters())).append(",");
            
            /* points */
            sb.append(this.getPointsString());
            
         }
        
        /* return */
        return sb.toString();
        
    }
    
}
