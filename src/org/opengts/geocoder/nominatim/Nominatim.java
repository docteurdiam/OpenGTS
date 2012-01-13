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
// Notes:
//  - http://wiki.openstreetmap.org/wiki/Nominatim
//  - http://open.mapquestapi.com/nominatim/
//  - http://nominatim.openstreetmap.org
//  - OpenStreetMap "Nominative Usage Policy" can be found at the following link: 
//      http://wiki.openstreetmap.org/wiki/Nominatim_usage_policy
// ----------------------------------------------------------------------------
// Example
//  - http://nominatim.openstreetmap.org/reverse?format=xml&lat=46.17330&lon=21.29370&zoom=18&addressdetails=1
//  - http://open.mapquestapi.com/nominatim/v1/reverse?format=xml&lat=46.17330&lon=21.29370&zoom=18&addressdetails=1
//   <?xml version="1.0" encoding="UTF-8" ?>
//   <reversegeocode timestamp='Sat, 08 Jan 11 01:43:35 -0500' 
//      attribution='Data Copyright OpenStreetMap Contributors, Some Rights Reserved. CC-BY-SA 2.0.' 
//      querystring='format=xml&amp;lat=46.17330&amp;lon=21.29370&amp;zoom=18&amp;addressdetails=1'>
//      <result place_id="25016501" osm_type="way" osm_id="17508617">P?durii, Arad, 310365, Romania</result>
//      <addressparts>
//          <tram>P?durii</tram>
//          <road>P?durii</road>
//          <residential>Arad</residential>
//          <city>Arad</city>
//          <postcode>310365</postcode>
//          <country>Romania</country>
//          <country_code>ro</country_code>
//      </addressparts>
//   </reversegeocode>
// ----------------------------------------------------------------------------
// Change History:
//  2011/01/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.geocoder.nominatim;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.opengts.util.*;
import org.opengts.geocoder.*;

public class Nominatim
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider
{
   
    // ------------------------------------------------------------------------
    // TAGs
    
    protected static final String TAG_reversegeocode            = "reversegeocode";     // main tag
    protected static final String TAG_result                    = "result";             // full address
    protected static final String TAG_addressparts              = "addressparts";       // address components
    protected static final String TAG_house                     = "house";              // 
    protected static final String TAG_tram                      = "tram";               // same as road?
    protected static final String TAG_road                      = "road";               // 
    protected static final String TAG_residential               = "residential";        // same as city?
    protected static final String TAG_village                   = "village";            // 
    protected static final String TAG_town                      = "town";               // alternate for city?
    protected static final String TAG_city                      = "city";               // 
    protected static final String TAG_county                    = "county";             // 
    protected static final String TAG_postcode                  = "postcode";           // 
    protected static final String TAG_hamlet                    = "hamlet";             // Wherefor art thou?
    protected static final String TAG_state                     = "state";              // 
    protected static final String TAG_state_district            = "state_district";     // 
    protected static final String TAG_country                   = "country";            // Country name
    protected static final String TAG_country_code              = "country_code";       // Country code
    
    protected static final String ATTR_osm_type                 = "osm_type";

    // ------------------------------------------------------------------------

    protected static final String PROP_reverseURL               = "reverseURL";      // String: "http://localhost:8081/reverse?"
    protected static final String PROP_hostName                 = "host";            // String: "localhost:8081"
    protected static final String PROP_zoom                     = "zoom";            // String: "18"
    protected static final String PROP_addressdetails           = "addressdetails";  // String: "1"
    protected static final String PROP_email                    = "email";           // String: "joe@example.com"

    protected static       String HOST_OPENSTREETMAP            = "nominatim.openstreetmap.org";
    protected static       String HOST_MAPQUEST                 = "open.mapquestapi.com";
    protected static       String HOST_PRIMARY                  = HOST_MAPQUEST;

    // ------------------------------------------------------------------------

    protected static final String ENCODING_UTF8                 = StringTools.CharEncoding_UTF_8;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param name    The name assigned to this ReverseGeocodeProvider
    *** @param key     The optional authorization key
    *** @param rtProps The properties associated with this ReverseGeocodeProvider
    **/
    public Nominatim(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if locally resolved, false otherwise.
    *** (ie. remote address resolution takes more than 20ms to complete)
    *** @return true if locally resolved, false otherwise.
    **/
    public boolean isFastOperation() 
    {
        // this is a slow operation
        return super.isFastOperation();
    }

    /**
    *** Returns a ReverseGeocode instance for the specified GeoPoint
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr) 
    {
        ReverseGeocode rg = this.getAddressReverseGeocode(gp, localeStr);
        return rg;
    }

    /* return subdivision */
    public String getSubdivision(GeoPoint gp) 
    {
        throw new UnsupportedOperationException("Not supported");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a ReverseGeocode instance containing address information
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    private ReverseGeocode getAddressReverseGeocode(GeoPoint gp, String localeStr) 
    {

        /* URL */
        String url = this.getAddressReverseGeocodeURL(gp);
        Print.logInfo("Address URL: " + url);

        /* create XML document */
        Document xmlDoc = GetXMLDocument(url);
        if (xmlDoc == null) {
            return null;
        }

        /* create ReverseGeocode response */
        Element reversegeocode = xmlDoc.getDocumentElement();
        if (!reversegeocode.getTagName().equalsIgnoreCase(TAG_reversegeocode)) {
            return null;
        }
        
        /* init */
        String address_val      = null;     // null address
        String house_val        = null;     // house number
        String road_val         = null;     // street name
        String city_val         = null;     // city name
        String county_val       = null;     // county name
        String state_val        = null;     // state/province
        String postcode_val     = null;     // postal code
        String country_name_val = null;     // country name
        String country_code_val = null;     // country code

        // full address
        NodeList resultList = XMLTools.getChildElements(reversegeocode,TAG_result);
        for (int r = 0; r < resultList.getLength(); r++) {
            Element result = (Element)resultList.item(r);
            //String osmType = XMLTools.getAttribute(result, ATTR_osm_type, null, false);
            address_val = XMLTools.getNodeText(result," ",false);
            break; // only the first element
        }

        // address components
        NodeList addresspartsList = XMLTools.getChildElements(reversegeocode,TAG_addressparts);
        for (int a = 0; (a < addresspartsList.getLength()); a++) {
            Element addressparts = (Element)addresspartsList.item(a);
            NodeList addresspartsChildren = addressparts.getChildNodes();
            for (int ac = 0; ac < addresspartsChildren.getLength(); ac++) {
                Node child = addresspartsChildren.item(ac);
                if (!(child instanceof Element)) { continue; }
                Element elem = (Element)child;
                String elemName = elem.getNodeName();
                if (elemName.equalsIgnoreCase(TAG_house)) {
                    house_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_tram)) {
                    // ignore
                } else
                if (elemName.equalsIgnoreCase(TAG_road)) {
                    road_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_residential)) {
                    // ignore
                } else
                if (elemName.equalsIgnoreCase(TAG_village)) {
                    // ignore
                } else
                if (elemName.equalsIgnoreCase(TAG_town)) {
                    if (StringTools.isBlank(city_val)) {
                        city_val = XMLTools.getNodeText(elem," ",false);
                    }
                } else
                if (elemName.equalsIgnoreCase(TAG_city)) {
                    city_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_county)) {
                    county_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_postcode)) {
                    postcode_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_state)) {
                    state_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_country)) {
                    country_name_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_country_code)) {
                    country_code_val = StringTools.trim(XMLTools.getNodeText(elem," ",false)).toUpperCase();
                } else {
                    // elemName unrecognized
                }
            }
            break; // only the first element
        }

        /* populate ReverseGeocode instance */
        ReverseGeocode rg = new ReverseGeocode();
        StringBuffer addr = new StringBuffer();
        // house number /road
        if (!StringTools.isBlank(house_val)) {
            addr.append(house_val);
            if (!StringTools.isBlank(road_val)) {
                addr.append(" ");
                addr.append(road_val);
                rg.setStreetAddress(house_val + " " + road_val);
            } else {
                rg.setStreetAddress(house_val);
            }
        } else
        if (!StringTools.isBlank(road_val)) {
            addr.append(road_val);
            rg.setStreetAddress(road_val);
        }
        // city/county
        if (!StringTools.isBlank(city_val)) {
            if (addr.length() > 0) { addr.append(", "); }
            addr.append(city_val);
            rg.setCity(city_val);
        }
        if (!StringTools.isBlank(county_val)) {
            if (StringTools.isBlank(city_val)) {
                // "city" not provided, at least include the "county"
                if (addr.length() > 0) { addr.append(", "); }
                addr.append("[").append(county_val).append("]");
            }
            //rg.setCounty(county_val);
        }
        // state/province
        if (!StringTools.isBlank(state_val)) {
            if (addr.length() > 0) { addr.append(", "); }
            addr.append(state_val);
            rg.setStateProvince(state_val);
            if (!StringTools.isBlank(postcode_val)) {
                addr.append(" ").append(postcode_val);
                rg.setPostalCode(postcode_val);
            }
        } else {
            if (!StringTools.isBlank(postcode_val)) {
                if (addr.length() > 0) { addr.append(", "); }
                addr.append(postcode_val);
                rg.setPostalCode(postcode_val);
            }
        }
        // country
        if (!StringTools.isBlank(country_code_val)) {
            if (country_code_val.equalsIgnoreCase("US")) {
                //if (addr.length() > 0) { addr.append(", "); }
                //addr.append("USA");
            } else
            if (!StringTools.isBlank(country_name_val)) {
                if (addr.length() > 0) { addr.append(", "); }
                addr.append(country_name_val);
            } else {
                if (addr.length() > 0) { addr.append(", "); }
                addr.append(country_code_val);
            }
            rg.setCountryCode(country_code_val);
        }
        // full address
        rg.setFullAddress(addr.toString());

        return rg;
    
    }

    private String getEmail()
    {
        RTProperties rtp = this.getProperties();
        String email = rtp.getString(PROP_email,null);
        if (StringTools.isBlank(email)) {
            email = SendMail.getUserFromEmailAddress();
        }
        return email;
    }

    private String getAddressReverseGeocodeURL(GeoPoint gp) 
    {
        //  - http://nominatim.openstreetmap.org/reverse?format=xml&addressdetails=1&zoom=18&lat=46.17330&lon=21.29370
        StringBuffer sb = new StringBuffer();
        RTProperties rtp = this.getProperties();
        String url = rtp.getString(PROP_reverseURL, null);
        if (!StringTools.isBlank(url)) {
            sb.append(url);
        } else {
            String host = rtp.getString(PROP_hostName, HOST_PRIMARY);
            sb.append("http://");
            sb.append(host);
            if (host.indexOf("mapquest") >= 0) {
                sb.append("/nominatim/v1/reverse?");
            } else {
                sb.append("/reverse?");
            }
        }
        sb.append("format=xml&");
        sb.append("limit=1&");
        //sb.append("osm_type=W&");
        sb.append("addressdetails=").append(rtp.getString(PROP_addressdetails,"1")).append("&"); // 0,1
        sb.append("zoom=").append(rtp.getString(PROP_zoom,"18")).append("&"); // 0..18
        sb.append("email=").append(this.getEmail()).append("&"); // required, per usage policy
        sb.append("lat=").append(gp.getLatitudeString( GeoPoint.SFORMAT_DEC_5,null)).append("&");
        sb.append("lon=").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));
        return sb.toString();
    }

    private Document GetXMLDocument(String url) 
    {
         try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream input = HTMLTools.inputStream_GET(url, 5000);
            InputStreamReader reader = new InputStreamReader(input, ENCODING_UTF8);
            InputSource inSrc = new InputSource(reader);
            inSrc.setEncoding(ENCODING_UTF8);
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
    // ------------------------------------------------------------------------

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(ENCODING_UTF8);

        /* host */
        String host = RTConfig.getString("host",null);
        if (!StringTools.isBlank(host)) {
            HOST_PRIMARY = host;
        }

        /* GeoPoint */
        GeoPoint gp = new GeoPoint(RTConfig.getString("gp",null));
        if (!gp.isValid()) {
            Print.logInfo("Invalid GeoPoint specified");
            System.exit(1);
        }
        Print.logInfo("Reverse-Geocoding GeoPoint: " + gp);

        /* Reverse Geocoding */
        Nominatim gn = new Nominatim("nominatim", null, RTConfig.getCommandLineProperties());
        Print.sysPrintln("RevGeocode = " + gn.getReverseGeocode(gp,null/*localeStr*/));

    }

}
