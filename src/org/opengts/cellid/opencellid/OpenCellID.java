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
//  2011/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.cellid.opencellid;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.cellid.*;

public class OpenCellID
    extends MobileLocationProviderAdapter
    implements MobileLocationProvider
{

    // ------------------------------------------------------------------------
    //
    // References:
    //   - http://www.opencellid.org/api
    //
    // ------------------------------------------------------------------------

    private static final String  MOBILE_LOCATION_URI            = "http://www.opencellid.org/cell/get";
    
    private static final String  VERSION                        = "0.1.1";

    // ------------------------------------------------------------------------

    private static final String  PROP_timeoutMS                 = "timeoutMS";
    
    // ------------------------------------------------------------------------

    private static final long    DefaultServiceTimeout          = 5000L; // milliseconds

    // ------------------------------------------------------------------------

    private static final String  TAG_rsp                        = "rsp";
    private static final String  TAG_cell                       = "cell";

    private static final String  ATTR_stat                      = "stat";
    private static final String  ATTR_nbSamples                 = "nbSamples";
    private static final String  ATTR_mnc                       = "mnc";
    private static final String  ATTR_lac                       = "lac";
    private static final String  ATTR_cellId                    = "cellId";
    private static final String  ATTR_mcc                       = "mcc";
    private static final String  ATTR_lat                       = "lat";
    private static final String  ATTR_lon                       = "lon";
    private static final String  ATTR_range                     = "range";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Document _getMobileLocateXML(String url, long timeoutMS)
    {
        // Success Resonse:
        //   <rsp stat="ok">
        //      <cell nbSamples="57" mnc="99" lac="0" lat="50.5715642160311" lon="25.2897075399231" cellId="29513" mcc="250" range="6000"/>
        //   </rsp>
        try {
            //Print.logInfo("HTTP User-Agent: " + HTMLTools.getHttpUserAgent());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream input = HTMLTools.inputStream_GET(url, (int)timeoutMS);
            InputStreamReader reader = new InputStreamReader(input, StringTools.CharEncoding_UTF_8);
            InputSource inSrc = new InputSource(reader);
            inSrc.setEncoding(StringTools.CharEncoding_UTF_8);
            return db.parse(inSrc);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
            return null;
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
            return null;
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String _getCellLocationURL(CellTower servCT, CellTower nborCT[], String key)
    {
        // http://www.opencellid.org/cell/get?key=myapikey&mnc=1&mcc=2&lac=200&cellid=234
        
        /* no serving CellTower? */
        if (servCT == null) {
            return null;
        }

        /* skip if no key (required) */
        if (StringTools.isBlank(key)) {
            Print.logError("Required 'key' is not specified");
            return null;
        }

        /* standard */
        URIArg url = new URIArg(MOBILE_LOCATION_URI);
        url.addArg("key"    , key);
        // - Service Cell
        url.addArg("cellid" , servCT.getCellTowerID());
        url.addArg("mnc"    , servCT.getMobileNetworkCode());
        url.addArg("mcc"    , servCT.getMobileCountryCode());
        url.addArg("lac"    , servCT.getLocationAreaCode());
        return url.toString();

    }

    private static MobileLocation _getMobileLocation(
        CellTower servCT, CellTower nborCT[], 
        String key, long timeoutMS)
    {

        /* URL */
        String url = OpenCellID._getCellLocationURL(servCT, nborCT, key);
        if (StringTools.isBlank(url)) {
            return null;
        }

        /* get HTTP result */
        Document xmlDoc = OpenCellID._getMobileLocateXML(url, timeoutMS);
        if (xmlDoc == null) {
            return null;
        }

        /* parse "position" */
        //   <rsp stat="ok">
        //      <cell nbSamples="57" mnc="99" lac="0" lat="50.5715642160311" lon="25.2897075399231" cellId="29513" mcc="250" range="6000"/>
        //   </rsp>
        double latitude  = 999.0;
        double longitude = 999.0;
        double range     = 0.0;
        Element rsp = xmlDoc.getDocumentElement();
        if (rsp.getTagName().equalsIgnoreCase(TAG_rsp)) {
            String stat = StringTools.blankDefault(XMLTools.getAttribute(rsp,ATTR_stat,"",false),"ok");
            if (stat.equalsIgnoreCase("ok")) {
                NodeList nodeList = rsp.getChildNodes();
                for (int a = 0; a < nodeList.getLength(); a++) {
                    Node node = nodeList.item(a);
                    if (!(node instanceof Element)) { continue; }
                    Element elem = (Element)node;
                    String name = elem.getNodeName();
                    if (name.equalsIgnoreCase(TAG_cell)) {
                        latitude  = StringTools.parseDouble(XMLTools.getAttribute(elem,ATTR_lat  ,null,false),0.0);
                        longitude = StringTools.parseDouble(XMLTools.getAttribute(elem,ATTR_lon  ,null,false),0.0);
                        range     = StringTools.parseDouble(XMLTools.getAttribute(elem,ATTR_range,null,false),0.0);
                    } else {
                        // not not recognized
                    }
                }
            }
        }

        /* valid GeoPoint? */
        if (GeoPoint.isValid(latitude,longitude)) {
            return new MobileLocation(latitude,longitude,range);
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // MobileLocationProvider interface

    public OpenCellID(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    public MobileLocation getMobileLocation(CellTower servCT, CellTower nborCT[]) 
    {
        long tmoMS = this.getProperties().getLong(PROP_timeoutMS, DefaultServiceTimeout);
        return OpenCellID._getMobileLocation(servCT, nborCT, this.getAuthorization(), tmoMS);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(StringTools.CharEncoding_UTF_8);

        /* geocode lookup */
        CellTower ct = new CellTower();
        ct.setCellTowerID(565110);
        ct.setMobileNetworkCode(8);
        ct.setMobileCountryCode(240);
        ct.setLocationAreaCode(318);

        /* get CellTower location */
        String key = RTConfig.getString("key","");
        OpenCellID mobLoc = new OpenCellID("opencellid", key, null);
        MobileLocation ml = mobLoc.getMobileLocation(ct, null);
        Print.logInfo("Mobile Location: " + ml);

    }

}
