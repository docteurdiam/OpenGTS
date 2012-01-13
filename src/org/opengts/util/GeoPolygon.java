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
// Note:
//  This class holds a polygon based on GeoPoints, and will perform polygon
//  inclusion testing as if the points were in a flat 2D plane.  The accuracy 
//  of 2D calculations based on GeoPoints will descrease and the distance 
//  between the points increases.
// References:
//  http://softsurfer.com/Archive/algorithm_0103/algorithm_0103.htm
//  http://postgis.refractions.net/documentation/javadoc/org/postgis/Polygon.html
//  http://technet.microsoft.com/en-us/library/bb964739.aspx
// ----------------------------------------------------------------------------
// Change History:
//  2007/07/27  Martin D. Flynn
//     -Initial release
//  2009/09/23  Martin D. Flynn
//     -Fixed 'isPointInside' to close the polygon before performing a point
//      inclusion test.
//  2010/09/09  Martin D. Flynn
//     -Added support for negative rings
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

import org.opengts.util.*;

/**
*** A container for a polygon composed of GeoPoints
**/

public class GeoPolygon
    implements Cloneable
{
    
    // ------------------------------------------------------------------------
    
    // States:
    //  0   starting/ending state
    // 10   look for "(" of outer ring
    // 11   look for latitude of outer ring
    // 12   have outer latitude, look for longitude
    // 13   have outer longitude, look for next latitude or ')'
    // 20   look for "(" of inner ring
    // 21   look for latitude of inner ring
    // 22   have inner latitude, look for longitude
    // 23   have inner longitude, look for next latitude or ')'

    public static GeoPolygon parseGeoPolygon(String polyStr)
    {
        // ( ( lat/lon, lat/lon ) ( negLat/negLon, negLat/negLon ) )
        GeoPolygon outerRing = null;
        GeoPolygon innerRing = null;
        Vector<GeoPolygon> innerRings = new Vector<GeoPolygon>();
        
        boolean topLevelParent = false;
        int state = 0, level = 0, ringCount = 0;
        double latitude = 360.0, longitude = 360.0;
        
        StringTokenizer st = new StringTokenizer(polyStr, "()/ ,", true);
        for (;st.hasMoreTokens();) {

            /* next token */
            String token = st.nextToken();
            if (token.equals(" ")) {
                continue; // ignore spaces
            } else
            if (token.equals("/")) {
                continue; // ignore separator
            }

            /* parenthesis level up */
            if (token.equals("(")) {
                int oldState = state;
                if (state == 0) {
                     // start looking for "(" of outerRing
                    state = 10;
                } else
                if (state == 10) {
                     // found "(" of outerRing start parsing lat/lon
                    topLevelParent = true;
                    outerRing = new GeoPolygon();
                    innerRing = null;
                    state = 11;
                } else
                if (state == 20) {
                     // found "(" of innerRing start parsing lat/lon
                    innerRing = new GeoPolygon();
                    if (innerRings == null) { innerRings = new Vector<GeoPolygon>(); }
                    innerRings.add(innerRing);
                    state = 21;
                } else {
                    Print.logError("Invalid Polygon state: " + state);
                    return null;
                }
                //Print.logInfo("Found '(': " + oldState + " ==> " + state);
                continue;
            }
            
            /* parenthesis level down */
            if (token.equals(")")) {
                int oldState = state;
                if (state == 10) {
                    // empty polygon definition (error condition caught later)
                    state = -1; // done
                } else
                if (state == 11) {
                    // found an empty outerRing "()", or an empty lat/log specification
                    state = 20;
                } else
                if (state == 13) {
                    // finished parsing last outerRing lat/lon, start looking for "(" of innerRing
                    state = 20; // start looking for "(inner)"
                } else
                if (state == 20) {
                    // we were looking for "(" of the innerRing, but didn't find it
                    if (!topLevelParent) {
                        Print.logWarn("Found unexpected final ')'");
                    }
                    state = -1; // were done (nothing else is expected)
                } else
                if (state == 21) {
                    // found an empty innerRing "()" - ignore
                    state = 20;
                } else
                if (state == 23) {
                    // finished parsing last innerRing lat/lon, start looking for another "(" of innerRing
                    state = 20; // start looking for another "(inner)"
                } else {
                    Print.logError("Invalid Polygon state: " + state);
                    return null;
                }
                //Print.logInfo("Found ')': " + oldState + " ==> " + state);
                continue;
            }

            // end of latitude/longitude,
            if (token.equals(",")) {
                int oldState = state;
                if (state == 11) {
                    // empty latitude/longitude - ignore
                    Print.logWarn("Empty Outer latitude/longitude specification");
                    state = 11;
                } else
                if (state == 13) {
                    // have outerRing latitude/longitude, look for next latitude or ')'
                    state = 11;
                } else
                if (state == 23) {
                    // have inner latitude/longitude, look for next latitude or ')'
                    state = 21;
                } else {
                    Print.logError("Invalid Polygon state: " + state);
                    return null;
                }
                //Print.logInfo("Found ',': " + oldState + " ==> " + state);
                continue;
            }
            
            /* latitude */
            if (state == 10) {
                // special case where the polygon is specified as just "( outer )"
                char ch = token.charAt(0);
                if ((ch == '-') || (ch == '+') || Character.isDigit(ch)) {
                    outerRing = new GeoPolygon();
                    innerRing = null;
                    latitude = StringTools.parseDouble(token,360.0);
                    state = 12;
                    continue;
                    // the final ')' will be missing
                } else {
                    Print.logError("Invalid Polygon state: " + state);
                    return null;
                }
            } else
            if (state == 11) {
                latitude = StringTools.parseDouble(token,360.0);
                state = 12;
                continue;
            } else 
            if (state == 21) {
                latitude = StringTools.parseDouble(token,360.0);
                state = 22;
                continue;
            }

            /* longitude */
            if (state == 12) {
                longitude = StringTools.parseDouble(token,360.0);
                GeoPoint gp = new GeoPoint(latitude, longitude);
                if (gp.isValid()) {
                    outerRing.addGeoPoint(gp);
                    //Print.logInfo("Added point to outer ring: " + gp + " [" + outerRing.getSize() + "]");
                } else {
                    Print.logError("Invalid Outer GeoPoint: " + gp);
                }
                state = 13;
                continue;
            } else
            if (state == 22) {
                longitude = StringTools.parseDouble(token,360.0);
                GeoPoint gp = new GeoPoint(latitude, longitude);
                if (gp.isValid()) {
                    innerRing.addGeoPoint(gp);
                } else {
                    Print.logError("Invalid Inner GeoPoint: " + gp);
                }
                state = 23;
                continue;
            }

            // invalid state
            Print.logError("Invalid Polygon state: " + state);
            return null;
            
        }
        
        /* final state check */
        if (state == 20) {
            // missing final ')'
            state = -1;
        } else
        if (state == -1) {
            // normal completion
        } else {
            Print.logError("Invalid Polygon state: " + state);
            return null;
        }

        /* check outer ring */
        if ((outerRing == null) || outerRing.isEmpty()) {
            Print.logError("Empty GeoPolygon");
            return null;
        }
        
        /* add negative polygons */
        if (innerRings != null) {
            for (GeoPolygon negPoly : innerRings) {
                if (!negPoly.isEmpty()) {
                    outerRing.addNegativeRing(negPoly);
                }
            }
        }
        
        /* return outer polygon */
        return outerRing;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String              name        = null; // optional
    private GeoPoint            boundary[]  = null;
    
    private Vector<GeoPolygon>  negRings    = null;

    /**
    *** Empty constructor
    **/
    private GeoPolygon()
    {
        super();
    }

    /**
    *** Point constructor
    *** @param gp A list of GeoPoints
    **/
    public GeoPolygon(GeoPoint... gp)
    {
        super();
        this.boundary = gp;
    }

    /**
    *** Point constructor
    *** @param gpp A list of GeoPointProviders
    **/
    public GeoPolygon(GeoPointProvider... gpp)
    {
        super();
        if (gpp != null) {
            this.boundary = new GeoPoint[gpp.length];
            for (int i = 0; i < this.boundary.length; i++) {
                this.boundary[i] = gpp[i].getGeoPoint();
            }
        } else {
            this.boundary = null;
        }
    }

    /**
    *** Point constructor
    *** @param gpList A list of GeoPoints
    **/
    public GeoPolygon(java.util.List<GeoPoint> gpList)
    {
        super();
        this.boundary = ListTools.toArray(gpList, GeoPoint.class);
    }

    /**
    *** Point constructor
    *** @param gp An array of lattitude/longitude pairs
    **/
    public GeoPolygon(float gp[][])
    {
        super();
        this.boundary = new GeoPoint[gp.length];
        for (int i = 0; i < gp.length; i++) {
            this.boundary[i] = new GeoPoint((double)gp[i][0],(double)gp[i][1]);
        }
    }

    /**
    *** Point constructor
    *** @param gp An array of lattitude/longitude pairs
    **/
    public GeoPolygon(double gp[][])
    {
        super();
        this.boundary = new GeoPoint[gp.length];
        for (int i = 0; i < gp.length; i++) {
            this.boundary[i] = new GeoPoint(gp[i][0],gp[i][1]);
        }
    }

    /**
    *** Name/point constructor
    *** @param name The name of the polygon
    *** @param gp A list of GeoPoints
    **/
    public GeoPolygon(String name, GeoPoint... gp)
    {
        this(gp);
        this.name = name;
        // closed
    }

    /**
    *** Name/point constructor
    *** @param name The name of the polygon
    *** @param gpList A list of GeoPoints
    **/
    public GeoPolygon(String name, java.util.List<GeoPoint> gpList)
    {
        this(gpList);
        this.name = name;
        // closed
    }

    /**
    *** Name/point constructor
    *** @param name The name of the polygon
    *** @param gp An array of lattitude/longitude pairs
    **/
    public GeoPolygon(String name, float gp[][])
    {
        this(gp);
        this.name = name;
        // closed
    }

    /**
    *** Name/point constructor
    *** @param name The name of the polygon
    *** @param gp An array of lattitude/longitude pairs
    **/
    public GeoPolygon(String name, double gp[][])
    {
        this(gp);
        this.name = name;
        // closed
    }

    /**
    *** Copy constructor
    **/
    public GeoPolygon(GeoPolygon other)
    {
        super();
        if (other != null) {
            this.name     = other.getName();
            this.boundary = other.getGeoPoints();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of the polygon
    *** @return The name of the polygon
    **/
    public String getName()
    {
        return this.name;
    }
    
    /**
    *** Sets the name of the polygon
    *** @param name The name of the polygon
    **/
    public void setName(String name)
    {
        this.name = name;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Return true if this polygon is empty 
    *** @return True if the polygon is empty
    **/
    public boolean isEmpty()
    {
        return ListTools.isEmpty(this.boundary);
    }

    /**
    *** Return true if this polygon is valid 
    *** @return True if the polygon is valid
    **/
    public boolean isValid()
    {
        if (this.boundary == null) {
            return false;
        } else
        if (GeoPolygon.isClosed(this.boundary)) {
            return (this.boundary.length >= 4);
        } else {
            return (this.boundary.length >= 3);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the array of GeoPoints from the specified GeoPolygon
    *** @param geoPoly  The GeoPolygon
    *** @return The list of contained GeoPoints
    **/
    public static GeoPoint[] getGeoPoints(GeoPolygon geoPoly)
    {
        return (geoPoly != null)? geoPoly.getGeoPoints() : null;
    }

    /**
    *** Return the array of points that make up the polygon
    *** @return The array of points that make up the polygon
    **/
    public GeoPoint[] getGeoPoints()
    {
        return this.boundary;
    }

    /**
    *** Return the GeoPoint at the specified index
    *** @param gpi  The GeoPoint index
    *** @return The GeoPoint at the specified index
    **/
    public GeoPoint getGeoPoint(int gpi)
    {
        if (this.boundary == null) {
            return null;
        } else
        if ((gpi < 0) || (gpi >= this.boundary.length)) {
            return null;
        } else {
            return this.boundary[gpi];
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Insert point into polygon
    *** @param gp The point to insert into the polygon
    *** @param ndx The index to insert the point at
    **/
    public void insertGeoPoint(GeoPoint gp, int ndx)
    {
        if (gp == null) {
            // ignore
        } else
        if (!gp.isValid()) {
            Print.logError("Invalid GeoPoint: " + gp);
        } else
        if (this.boundary == null) {
            // ignore index
            this.boundary = new GeoPoint[] { gp };
        } else {
            this.boundary = ListTools.insert(this.boundary, gp, ndx);
        }
    }
    
    /**
    *** Add point to polygon
    *** @param gp The point to add to the polygon
    **/
    public void addGeoPoint(GeoPoint gp)
    {
        if (gp == null) {
            // ignore
        } else
        if (!gp.isValid()) {
            Print.logError("Invalid GeoPoint: " + gp);
        } else
        if (this.boundary == null) {
            this.boundary = new GeoPoint[] { gp };
        } else {
            this.boundary = ListTools.insert(this.boundary, gp, -1);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified points represent a closed polygon
    *** @param gp The set of points representing a polygon
    *** @return True if the represented polygon is closed
    **/
    public static boolean isClosed(GeoPoint gp[])
    {
        if (gp == null) {
            // no points
            return false;
        } else
        if (gp.length < 3) {
            // below minimum points for a closed polygon
            return false;
        } else {
            // first point equals last point?
            GeoPoint gp0 = gp[0];
            GeoPoint gpN = gp[gp.length - 1];
            return gp0.equals(gpN);
        }
    }

    /**
    *** Returns true if this polygon is closed
    *** @return True if this polygon is closed
    **/
    public boolean isClosed()
    {
        return GeoPolygon.isClosed(this.boundary);
    }

    /**
    *** Closes the polygon represented by the list of points
    *** @return A closed polygon
    **/
    public static GeoPoint[] closePolygon(GeoPoint gp[])
    {
        if (ListTools.isEmpty(gp)) {
            // null/empty, return as-is
            return gp;
        } else
        if (gp.length < 3) {
            // invalid number of points, return as-is
            return gp;
        } else {
            GeoPoint gp0 = gp[0];
            GeoPoint gpN = gp[gp.length - 1];
            if (gp0.equals(gpN)) {
                // already closed
                return gp;
            } else {
                // close and return new array
                return ListTools.add(gp, gp0);
            }
        }
    }

    /**
    *** Closes this GeoPolygon (making sure last point is equal to first point)
    *** @return True if able to close GeoPolygon, false otherwise
    **/
    public boolean closePolygon()
    {
        if ((this.boundary != null) && (this.boundary.length >= 3)) {
            this.boundary = GeoPolygon.closePolygon(this.boundary);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the GeoBounds of this GeoPolygon
    *** @return The GeoBounds
    **/
    public GeoBounds getGeoBounds()
    {
        return new GeoBounds(this.getGeoPoints());
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the number of points in the polygon
    *** @return The number of points in the polygon
    **/
    public int getSize()
    {
        return (this.boundary != null)? this.boundary.length : 0;
    }

    /**
    *** @see #getSize()
    **/
    public int size()
    {
        return this.getSize();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified point is inside the polygon, 
    *** same as <code>isPointInside()</code>
    *** @param gp The point to check if is inside the polygon
    *** @return True if the specified point is inside the polygon
    *** @see #isPointInside(GeoPoint gp)
    **/
    public boolean containsPoint(GeoPoint gp)
    {
        return GeoPolygon.isPointInside(gp, this.getGeoPoints());
    }

    /**
    *** Returns true if the specified point is inside the polygon
    *** @param gp The point to check if is inside the polygon
    *** @return True if the specified point is inside the polygon
    **/
    public boolean isPointInside(GeoPoint gp)
    {
        return GeoPolygon.isPointInside(gp, this.getGeoPoints());
    }

    /**
    *** Returns true if the specified point is inside the polygon formed by
    *** a specified list of points, same as <code>isPointInside()</code>  <br>
    *** NOTE: The list of points MUST represent a <strong>closed</strong> polygon.
    *** @param gp The point to check if is inside the polygon
    *** @return True if the specified point is inside the polygon
    *** @see #isPointInside(GeoPoint gp, GeoPoint... pp)
    **/
    public static boolean containsPoint(GeoPoint gp, GeoPoint... pp)
    {
        return GeoPolygon.isPointInside(gp, pp);
    }

    /**
    *** Returns true if the specified point is inside the polygon formed by
    *** a specified list of points <br>
    *** NOTE: The list of points MUST represent a <strong>closed</strong> polygon.
    *** @param gp The point to check if is inside the polygon
    *** @return True if the specified point is inside the polygon
    **/
    public static boolean isPointInside(GeoPoint gp, GeoPoint... pp)
    {

        /* quick argument validation */
        if ((gp == null) || (pp == null)) {
            return false;
        }

        /* close polygon (make sure last point is same as first) */
        pp = GeoPolygon.closePolygon(pp);

        // Uses "Winding Number" algorithm
        // Notes: 
        //  - This is a very simple algorithm that compares the number of downward vectors
        //    with the number of upward vectors surrounding a specified point.  
        //  - This algorithm was designed for a 2D plane and will fail for a curved surface
        //    where the distance between points is great.
        // Observations:
        //  - It appears that the state borders may have been defined by simple X/Y cooridinated
        //    based on latitude/longitude values.  The simple cases are states bordered by 
        //    constant longitudes or latitudes.
        int wn = 0;                                             // the winding number counter
        for (int i = 0; i < pp.length - 1; i++) {               // edge from V[i] to V[i+1]
            if (pp[i].getY() <= gp.getY()) {                    // start y <= P.y
                if (pp[i+1].getY() > gp.getY()) {               // an upward crossing
                    if (GeoPolygon._isLeft(pp[i],pp[i+1],gp) > 0.0) {  // P left of edge
                        ++wn;                                   // have a valid up intersect
                    }
                }
            } else {                                            // start y > P.y (no test needed)
                if (pp[i+1].getY() <= gp.getY()) {              // a downward crossing
                    if (GeoPolygon._isLeft(pp[i],pp[i+1],gp) < 0.0) {  // P right of edge
                        --wn;                                   // have a valid down intersect
                    }
                }
            }
        }
        return (wn == 0)? false : true; // wn==0 if point is OUTSIDE

    }
    
    /** 
    *** Tests if the point, <code>gpC</code>, is Left|On|Right of an infinite line 
    *** formed by <code>gp0</code> and <code>gp1</code>
    *** @param gp0 First point forming the line
    *** @param gp1 Second point forming the line
    *** @return <ul>
    ****        <li> >0 for gpC left of the line through gp0 and P1</li>
    ***         <li> =0 for gpC on the line</li>
    ***         <li> <0 for gpC right of the line</li>
    ****        </ul>
    *** @see "The January 2001 Algorithm 'Area of 2D and 3D Triangles and Polygons'"
    **/
    private static double _isLeft(GeoPoint gp0, GeoPoint gp1, GeoPoint gpC)
    {
        double val = (gp1.getX() - gp0.getX()) * (gpC.getY() - gp0.getY()) -
                     (gpC.getX() - gp0.getX()) * (gp1.getY() - gp0.getY());
        return val;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // http://local.wasp.uwa.edu.au/~pbourke/geometry/clockwise/index.html
    // http://en.wikipedia.org/wiki/Curve_orientation#Orientation_of_a_simple_polygon
    // http://www.alienryderflex.com/polygon_area/

    /**
    *** Returns true if the points in this polygon are oriented clockwise.
    *** Undeterminate if polygon is invalid
    *** @return True if the polygon is oriented clockwise
    **/
    public boolean isClockwise()
    {
        return GeoPolygon.isClockwise(this.getGeoPoints());
    }

    /**
    *** Returns true if the points in this polygon are oriented clockwise.
    *** Indeterminate if polygon is invalid
    *** @param pp  The GeoPoints comprising the polygon
    *** @return True if the polygon is oriented clockwise, false otherwise
    **/
    public static boolean isClockwise(GeoPoint... pp)
    {

        /* invalid Polygon */
        if ((pp == null) || (pp.length < 3)) {
            return false;
        }

        /* close polygon */
        pp = GeoPolygon.closePolygon(pp);

        /* calculate area 'sign' */
        double area = 0.0; // we don't care about the magnitude of this value, only the sign
        for (int i = 0; i < pp.length; i++) {
            int j = (i + 1) % pp.length;
            area += (pp[i].getLongitude() + pp[j].getLongitude()) * (pp[i].getLatitude() - pp[j].getLatitude());
        }

        /* return */
        return (area >= 0.0)? true : false;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Shallow copy
    **/
    public Object clone()
    {
        return new GeoPolygon(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this class.<br>
    *** Currently, this simply includes the number of points this polygon contains
    *** IE. 234(5,10,6)
    *** @return A String representation of this class.
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.size());
        //sb.append(this.isClockwise()?":CW":":CCW");
        if (!ListTools.isEmpty(this.negRings)) {
            sb.append("(");
            for (int i = 0; i < this.negRings.size(); i++) {
                GeoPolygon gp = this.negRings.get(i);
                if (i > 0) { sb.append(","); }
                sb.append(gp.toString());
            }
            sb.append(")");
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Adds an inner polygon which is defines an area which is not part of
    *** the outer polygon.<br>
    *** No validation is performed to ensure that the specified polygon is a 
    *** propper inner polygon.
    *** @param geoPolyList  The inner negative rings
    **/
    public void addNegativeRings(Collection<GeoPolygon> geoPolyList)
    {
        if (!ListTools.isEmpty(geoPolyList)) {
            if (this.negRings == null) {
                this.negRings = new Vector<GeoPolygon>();
            }
            this.negRings.addAll(geoPolyList);
        }
    }

    /**
    *** Adds an inner polygon which is defines an area which is not part of
    *** the outer polygon.<br>
    *** No validation is performed to ensure that the specified polygon is a 
    *** propper inner polygon.
    *** @param geoPoly  The inner negative ring
    **/
    public void addNegativeRing(GeoPolygon geoPoly)
    {
        if (geoPoly != null) {
            if (this.negRings == null) {
                this.negRings = new Vector<GeoPolygon>();
            }
            this.negRings.add(geoPoly);
        }
    }
    
    /**
    *** Gets the list of inner negative rings
    *** @return The list of inner negative rings
    **/
    public java.util.List<GeoPolygon>getNegativeRings()
    {
        return this.negRings;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static GeozoneChecker geozoneCheck = null;

    /**
    *** Gets a GeozoneChecker implemtation
    *** @return A GeozoneChecker implementation
    **/
    public static GeozoneChecker getGeozoneChecker()
    {
        if (geozoneCheck == null) {
            geozoneCheck = new GeozoneChecker() {
                public boolean containsPoint(GeoPoint gpTest, GeoPoint gpList[], double radiusKM) {
                    return GeoPolygon.isPointInside(gpTest, gpList);
                }
            };
        }
        return geozoneCheck;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
