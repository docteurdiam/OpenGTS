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
//  2010/07/18  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;
import java.awt.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.db.*;
import org.opengts.util.*;

/**
*** Parse XML location formats supported by Google (currently only GPX)
**/

public class ParseEventsXML
    implements ParseEvent.ParseEventHandler
{

    // ------------------------------------------------------------------------
    // GPX tags

    private static final String TAG_gpx                 = "gpx";        // top level tag
    private static final String TAG_name                = "name";       // gpx name
    private static final String TAG_desc                = "desc";       // gpx description
    private static final String TAG_number              = "number";     // gpx number
    private static final String TAG_trk                 = "trk";        // a 'track' containing segments
    private static final String TAG_trkseg              = "trkseg";     // a 'track' segment containing points
    private static final String TAG_trkpt               = "trkpt";      // a 'track' point
    private static final String TAG_ele                 = "ele";        // a point altitude
    private static final String TAG_time                = "time";       // a point time [2010-07-11T23:44:12Z]

    private static final String ATTR_version            = "version";    // version
    private static final String ATTR_creator            = "creator";    // creator
    private static final String ATTR_lat                = "lat";        // latitude
    private static final String ATTR_lon                = "lon";        // longitude

    // ------------------------------------------------------------------------
    // KML tags

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public ParseEventsXML()
    {
        super();
    }

    // ------------------------------------------------------------------------

    public boolean parseStream(InputStream xmlStream, GeoEvent.GeoEventHandler gevHandler)
        throws IOException
    {

        /* get Document */
        Document xmlDoc = XMLTools.getDocument(xmlStream, false/*checkErrors*/);
        if (xmlDoc == null) {
            // errors already displayed
            return false;
        }

        /* get top-level tag */
        Element topElem = xmlDoc.getDocumentElement();
        String topLevelTagName = topElem.getTagName();
        
        /* GPS */
        if (topElem.getTagName().equalsIgnoreCase(TAG_gpx)) {
            return this._parse_gpx(topElem, gevHandler);
        }

        /* not supported */
        Print.logError("XML format not supported: " + topLevelTagName);
        return false;

    }
    
    // ------------------------------------------------------------------------
    
    public boolean _parse_gpx(Element topElem, GeoEvent.GeoEventHandler gevHandler)
    {

        /* top level attributes */
        String version = XMLTools.getAttribute(topElem, ATTR_version, "");
        String creator = XMLTools.getAttribute(topElem, ATTR_creator, "");
        
        /* tracks */
        NodeList trkNodes = XMLTools.getChildElements(topElem, TAG_trk);
        for (int trk = 0; trk < trkNodes.getLength(); trk++) {
            Element trkTag = (Element)trkNodes.item(trk);
    
            /* name */
            Element nameTag = XMLTools.getChildElement(trkTag, TAG_name);
            String name = (nameTag != null)? XMLTools.getNodeText(nameTag,""/*repNewline*/) : "";
            Print.logInfo("Track Name: " + name);
                       
            /* description */
            Element descTag = XMLTools.getChildElement(trkTag, TAG_desc);
            String desc = (descTag != null)? XMLTools.getNodeText(descTag,""/*repNewline*/) : "";
            Print.logInfo("Track Descrption: " + desc);
         
            /* number */
            Element numberTag = XMLTools.getChildElement(trkTag, TAG_number);
            int number = (numberTag != null)? StringTools.parseInt(XMLTools.getNodeText(numberTag,""),0) : 0;
            Print.logInfo("Track Number: " + number);
    
            /* track segments */
            NodeList trksegNodes = XMLTools.getChildElements(trkTag, TAG_trkseg);
            for (int seg = 0; seg < trksegNodes.getLength(); seg++) {
                Print.logInfo("Parsing Track Segment ...");
                Element trksegTag = (Element)trksegNodes.item(seg);
                NodeList trkptNodes = XMLTools.getChildElements(trksegTag, TAG_trkpt);
                for (int pt = 0; pt < trkptNodes.getLength(); pt++) {
                    Element trkptTag = (Element)trkptNodes.item(pt);
                    double latitude  = XMLTools.getAttributeDouble(trkptTag, ATTR_lat, 0.0);
                    double longitude = XMLTools.getAttributeDouble(trkptTag, ATTR_lon, 0.0);
                    Element eleTag   = XMLTools.getChildElement(trkptTag, TAG_ele);
                    double altitudeM = (eleTag != null)? StringTools.parseDouble(XMLTools.getNodeText(eleTag,""),0.0) : 0.0;
                    Element timeTag  = XMLTools.getChildElement(trkptTag, TAG_time);
                    long   timestamp = (timeTag != null)? this._parseTime(XMLTools.getNodeText(timeTag,"")) : 0L;
                    this._handleEvent(gevHandler,
                        timestamp, StatusCodes.STATUS_LOCATION,
                        latitude, longitude, altitudeM
                        );
                }
            }
            
        }
        return true;

    }
        
    // ------------------------------------------------------------------------

    protected long _parseTime(String timeStr)
    {
        // "2010-07-11T23:44:12Z"
        try {
            DateTime dt = DateTime.parseArgumentDate(timeStr);
            return dt.getTimeSec();
        } catch (DateTime.DateParseException dpe) {
            Print.logError("Date/Time parsing format error: " + timeStr);
            return 0L;
        }
    }
        
    // ------------------------------------------------------------------------

    protected void _handleEvent(GeoEvent.GeoEventHandler gevHandler,
        long timestamp, int statusCode,
        double latitude, double longitude,
        double altitudeM)
    {
        if (gevHandler != null) {
            GeoEvent gev = new GeoEvent();
            //gev.setAccountID(this.accountID);
            //gev.setDeviceID(this.deviceID);
            gev.setTimestamp(timestamp);
            gev.setStatusCode(statusCode);
            gev.setLatitude(latitude);
            gev.setLongitude(longitude);
            gev.setAltitudeMeters(altitudeM);
            gevHandler.handleGeoEvent(gev);
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("[" + new DateTime(timestamp) + "] ");
            sb.append(StringTools.format(latitude,"0.00000"));
            sb.append("/");
            sb.append(StringTools.format(longitude,"0.00000"));
            sb.append("  ");
            sb.append(StringTools.format(altitudeM,"0") + " m");
            Print.logInfo("Point: " + sb);
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String args[])
    {
        RTConfig.setCommandLineArgs(args);
        
        File xmlFile = RTConfig.getFile("file",null);
        if (xmlFile == null) {
            Print.sysPrintln("Missing '-file' specification");
            System.exit(1);
        }
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(xmlFile);
            ParseEventsXML pgx = new ParseEventsXML();
            pgx.parseStream(fis, null);
        } catch (IOException ioe) {
            Print.logException("IO Error", ioe);
        } finally {
            if (fis != null) { try { fis.close(); } catch (Throwable th) {} }
        }
        
    }
    
}

