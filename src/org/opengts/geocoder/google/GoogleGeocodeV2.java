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
//  2008/12/01  Martin D. Flynn
//     -Initial release
//  2009/09/23  Martin D. Flynn
//     -Added "&oe=utf8" to reverse-geocode request url
//  2009/11/01  Martin D. Flynn
//     -Added check for error code "620" (ie. limit exceeded)
//  2009/12/16
//     -Added "reverseGeocodeURL", "geocodeURL" properties.
//     -Added support for client-id (ie. "&client=gme-...")
//     -Added support for Geocoding
// ----------------------------------------------------------------------------
package org.opengts.geocoder.google;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.geocoder.*;

public class GoogleGeocodeV2
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider, GeocodeProvider
{

    // ------------------------------------------------------------------------
    // References:
    //   - http://googlegeodevelopers.blogspot.com/2008/10/geocoding-in-reverse.html
    //   - http://blog.charlvn.za.net/2008/10/google-reverse-geocoding.html
    //   - http://code.google.com/apis/maps/documentation/services.html#Geocoding_Direct
    //   - http://code.google.com/apis/maps/documentation/geocoding/index.html
    //
    // Nearest Address: V2 API
    //   - http://maps.google.com/maps/geo?output=xml&ll=40.479581,-117.773438&key=(GoogleKey)
    //     Returns an XML file:
    //     <kml>
    //      <Response>
    //        <name>40.479581,-117.773438</name>
    //        <Status>
    //          <code>200</code>
    //          <request>geocode</request>
    //        </Status>
    //        <Placemark id="p1">
    //          <address>Apache, NV 89418, USA</address>
    //          <AddressDetails Accuracy="5">
    //            <Country>
    //              <CountryNameCode>US</CountryNameCode>
    //              <CountryName>USA</CountryName>
    //              <AdministrativeArea>
    //                <AdministrativeAreaName>NV</AdministrativeAreaName>
    //                <Locality>
    //                  <LocalityName>Apache</LocalityName>
    //                  <PostalCode>
    //                    <PostalCodeNumber>89418</PostalCodeNumber>
    //                  </PostalCode>
    //                </Locality>
    //              </AdministrativeArea>
    //            </Country>
    //          </AddressDetails>
    //          <Point>
    //            <coordinates>-118.0186087,40.6177933,0</coordinates>
    //          </Point>
    //        </Placemark>
    //      </Response>
    //     </kml>
    //
    // Exceeding reverse-geocode limit: V2 API
    //     <?xml version="1.0" encoding="UTF-8" ?>
    //     <kml xmlns="http://earth.google.com/kml/2.0">
    //       <Response>
    //         <name>40.479581,-117.773438</name>
    //         <Status>
    //           <code>620</code>
    //           <request>geocode</request>
    //         </Status>
    //       </Response>
    //     </kml>
    //
    // ------------------------------------------------------------------------
    // V3 API:
    //   http://maps.googleapis.com/maps/api/geocode/xml?oe=utf8&latlng=37.78340,-122.40246&sensor=false
    //   <?xml version="1.0" encoding="UTF-8"?>
    //   <GeocodeResponse>
    //      <status>OK</status>
    //      <result>
    //          <type>street_address</type>
    //          <formatted_address>747-799 Howard St, San Francisco, CA 94103, USA</formatted_address>
    //          <address_component>
    //              <long_name>747-799</long_name>
    //              <short_name>747-799</short_name>
    //              <type>street_number</type>
    //          </address_component>
    //          <address_component>
    //              <long_name>Howard St</long_name>
    //              <short_name>Howard St</short_name>
    //              <type>route</type>
    //          </address_component>
    //          <address_component>
    //              <long_name>San Francisco</long_name>
    //              <short_name>SF</short_name>
    //              <type>locality</type>
    //              <type>political</type>
    //          </address_component>
    //          <address_component>
    //              <long_name>San Francisco</long_name>
    //              <short_name>San Francisco</short_name>
    //              <type>administrative_area_level_3</type>
    //              <type>political</type>
    //          </address_component>
    //          <address_component>
    //              <long_name>San Francisco</long_name>
    //              <short_name>San Francisco</short_name>
    //              <type>administrative_area_level_2</type>
    //              <type>political</type>
    //          </address_component>
    //          <address_component>
    //              <long_name>California</long_name>
    //              <short_name>CA</short_name>
    //              <type>administrative_area_level_1</type>
    //              <type>political</type>
    //          </address_component>
    //          <address_component>
    //              <long_name>United States</long_name>
    //              <short_name>US</short_name>
    //              <type>country</type>
    //              <type>political</type>
    //          </address_component>
    //          <address_component>
    //              <long_name>94103</long_name>
    //              <short_name>94103</short_name>
    //              <type>postal_code</type>
    //          </address_component>
    //          <geometry>
    //              <!-- ... -->
    //          </geometry>
    //      </result>
    //      <result>
    //          <!-- ... -->
    //      </result>
    //      <!-- ... -->
    //  </GeocodeResponse>
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // V2 API
    
    /* V2 Tags */
    protected static final String TAG_kml                       = "kml";           // main tag
    protected static final String TAG_Response                  = "Response";  
    protected static final String TAG_name                      = "name";
    protected static final String TAG_Status                    = "Status";  
    protected static final String TAG_code                      = "code";
    protected static final String TAG_request                   = "request";
    protected static final String TAG_Placemark                 = "Placemark";
    protected static final String TAG_address                   = "address";
    protected static final String TAG_AddressDetails            = "AddressDetails";
    protected static final String TAG_Country                   = "Country";
    protected static final String TAG_CountryNameCode           = "CountryNameCode";
    protected static final String TAG_CountryName               = "CountryName";
    protected static final String TAG_AdministrativeArea        = "AdministrativeArea";
    protected static final String TAG_AdministrativeAreaName    = "AdministrativeAreaName";
    protected static final String TAG_Locality                  = "Locality";
    protected static final String TAG_LocalityName              = "LocalityName";
    protected static final String TAG_PostalCode                = "PostalCode";
    protected static final String TAG_PostalCodeNumber          = "PostalCodeNumber";
    protected static final String TAG_Point                     = "Point";
    protected static final String TAG_coordinates               = "coordinates";

    /* V2 Attributes */
    protected static final String ATTR_id                       = "id";
    protected static final String ATTR_Accuracy                 = "Accuracy";

    /* V2 URLs */
    protected static final String URL_ReverseGeocode_           = "http://maps.google.com/maps/geo?";
    protected static final String URL_Geocode_                  = "http://maps.google.com/maps/geo?";
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static final String PROP_reverseGeocodeURL        = "reverseGeocodeURL";
    protected static final String PROP_geocodeURL               = "geocodeURL";
    protected static final String PROP_sensor                   = "sensor";
    protected static final String PROP_channel                  = "channel";
    protected static final String PROP_countryCodeBias          = "countryCodeBias"; // http://en.wikipedia.org/wiki/CcTLD
    protected static final String PROP_signatureKey             = "signatureKey";

    // ------------------------------------------------------------------------

    protected static final int    TIMEOUT_ReverseGeocode        = 2500; // milliseconds
    protected static final int    TIMEOUT_Geocode               = 5000; // milliseconds
    
    protected static final String DEFAULT_COUNTRY               = "US"; // http://en.wikipedia.org/wiki/CcTLD
    
    protected static final String CLIENT_ID_PREFIX              = "gme-";

    // ------------------------------------------------------------------------

    // address has to be within this distance to qualify (cannot be greater than 5.0 kilometers)
    protected static final double MAX_ADDRESS_DISTANCE_KM       = 1.1; 
   // protected static final double MAX_ADDRESS_DISTANCE_KM = 4.5;

    // ------------------------------------------------------------------------

    protected static final String ENCODING_UTF8                 = StringTools.CharEncoding_UTF_8;
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private GoogleSig    signature = null;
    
    public GoogleGeocodeV2(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
        if (!StringTools.isBlank(key) && key.startsWith(CLIENT_ID_PREFIX)) {
            String sigKey = this.getProperties().getString(PROP_signatureKey,"");
            if (!StringTools.isBlank(sigKey)) {
                this.signature = new GoogleSig(sigKey);
            }
        }
    }

    // ------------------------------------------------------------------------
    
    public boolean isFastOperation()
    {
        // this is a slow operation
        return super.isFastOperation();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Geocode timeout
    **/
    protected int getGeocodeTimeout()
    {
        return TIMEOUT_Geocode;
    }

    /**
    *** Returns the ReverseGeocode timeout
    **/
    protected int getReverseGeocodeTimeout()
    {
        return TIMEOUT_ReverseGeocode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return reverse-geocode */
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr)
    {
        ReverseGeocode rg = this.getAddressReverseGeocode(gp, localeStr);
        return rg;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* nearest address URI */
    protected String getAddressReverseGeocodeURI()
    {
        return URL_ReverseGeocode_;
    }

    /* encode GeoPoint into nearest address URI */
    protected String getAddressReverseGeocodeURL(GeoPoint gp, String localStr)
    {
        StringBuffer sb = new StringBuffer();

        /* predefined URL */
        String rgURL = this.getProperties().getString(PROP_reverseGeocodeURL,null);
        if (!StringTools.isBlank(rgURL)) {
            sb.append(rgURL);
            sb.append("&ll=").append(gp.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null)).append(",").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));
            String defURL = sb.toString();
            if (this.signature == null) {
                return defURL;
            } else {
                String urlStr = this.signature.signURL(defURL);
                return (urlStr != null)? urlStr : defURL;
            }
        }

        /* assemble URL */
        sb.append(this.getAddressReverseGeocodeURI());
        sb.append("output=xml");
        sb.append("&oe=utf8");

        /* latitude/longitude */
        sb.append("&ll=").append(gp.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null)).append(",").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));

        /* sensor */
        String sensor = this.getProperties().getString(PROP_sensor, null);
        if (!StringTools.isBlank(sensor)) {
            sb.append("&sensor=").append(sensor);
        }

        /* key */
        String channel = this.getProperties().getString(PROP_channel, null);
        String auth    = this.getAuthorization();
        if (StringTools.isBlank(auth) || auth.startsWith("*")) {
            // invalid key
        } else
        if (auth.startsWith(CLIENT_ID_PREFIX)) {
            sb.append("&client=").append(auth);
            if (StringTools.isBlank(channel)) {
                channel = DBConfig.getServiceAccountID(null);
            }
        } else {
            sb.append("&key=").append(auth);
        }

        /* channel */
        if (!StringTools.isBlank(channel)) {
            sb.append("&channel=").append(channel);
        }

        /* localization ("&hl=") */
        if (!StringTools.isBlank(localStr)) {
            sb.append("&hl=").append(localStr);
        }

        /* return url */
        String defURL = sb.toString();
        if (this.signature == null) {
            return defURL;
        } else {
            String urlStr = this.signature.signURL(defURL);
            return (urlStr != null)? urlStr : defURL;
        }

    }

    /* return reverse-geocode using nearest address */
    public ReverseGeocode getAddressReverseGeocode(GeoPoint gp, String localeStr)
    {

        /* URL */
        String url = this.getAddressReverseGeocodeURL(gp, localeStr);
        Print.logDebug("Google RG URL: " + url);
        //byte xmlBytes[] = HTMLTools.readPage(url);
        
        /* create XML document */
        Document xmlDoc = GetXMLDocument(url, this.getReverseGeocodeTimeout());
        if (xmlDoc == null) {
            return null;
        }
        
        /* vars */
        String errCode = null;
        String address = null;

        /* parse "xml" */
        Element kml = xmlDoc.getDocumentElement();
        if (kml.getTagName().equalsIgnoreCase(TAG_kml)) {
            NodeList ResponseList = XMLTools.getChildElements(kml,TAG_Response);
            for (int g = 0; (g < ResponseList.getLength()); g++) {
                Element response = (Element)ResponseList.item(g);
                NodeList responseNodes = response.getChildNodes();
                for (int n = 0; n < responseNodes.getLength(); n++) {
                    Node responseNode = responseNodes.item(n);
                    if (!(responseNode instanceof Element)) { continue; }
                    Element responseElem = (Element)responseNode;
                    String responseNodeName = responseElem.getNodeName();
                    if (responseNodeName.equalsIgnoreCase(TAG_name)) {
                        // <name>40.479581,-117.773438</name>
                    } else
                    if (responseNodeName.equalsIgnoreCase(TAG_Status)) {
                        // <Status> ... </Status>
                        NodeList statusNodes = responseElem.getChildNodes();
                        for (int st = 0; st < statusNodes.getLength(); st++) {
                            Node statusNode = statusNodes.item(st);
                            if (!(statusNode instanceof Element)) { continue; }
                            Element statusElem = (Element)statusNode;
                            String statusNodeName = statusElem.getNodeName();
                            if (statusNodeName.equalsIgnoreCase(TAG_code)) {
                                errCode = StringTools.trim(GoogleGeocodeV2.GetNodeText(statusElem));  // expect "200"
                                break; // we only care about the 'code'
                            }
                        }
                    } else
                    if (responseNodeName.equalsIgnoreCase(TAG_Placemark)) {
                        // <Placemark> ... </Placemark>
                        String id = responseElem.getAttribute(ATTR_id);
                        if ((id != null) && id.equals("p1")) {
                            NodeList placemarkNodes = responseElem.getChildNodes();
                            for (int pm = 0; pm < placemarkNodes.getLength(); pm++) {
                                Node placemarkNode = placemarkNodes.item(pm);
                                if (!(placemarkNode instanceof Element)) { continue; }
                                Element placemarkElem = (Element)placemarkNode;
                                String placemarkNodeName = placemarkElem.getNodeName();
                                if (placemarkNodeName.equalsIgnoreCase(TAG_address)) {
                                    address = GoogleGeocodeV2.GetNodeText(placemarkElem);
                                    break; // we only care about the 'address'
                                }
                            }
                        } else {
                            //Print.logInfo("Skipping Placemark ID = %s", id);
                        }
                    }
                }
            }
        }
        
        /* create address */
        if (!StringTools.isBlank(address)) {
            // address found 
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(address);
            return rg;
        }
        
        /* check for Google reverse-geocode limit exceeded */
        if ((errCode != null) && errCode.equals("620")) {
            Print.logError("!!!! Google Reverse-Geocode Limit Exceeded [Error 620] !!!!");
        }

        /* no reverse-geocode available */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* nearest address URI */
    protected String getGeoPointGeocodeURI()
    {
        return URL_Geocode_;
    }

    /* encode GeoPoint into nearest address URI */
    protected String getGeoPointGeocodeURL(String address, String country)
    {
        StringBuffer sb = new StringBuffer();
        
        /* country */
        if (StringTools.isBlank(country)) { 
            country = this.getProperties().getString(PROP_countryCodeBias, DEFAULT_COUNTRY);
        }

        /* predefined URL */
        String gcURL = this.getProperties().getString(PROP_geocodeURL,null);
        if (!StringTools.isBlank(gcURL)) {
            sb.append(gcURL);
            sb.append("&q=").append(URIArg.encodeArg(address));
            if (!StringTools.isBlank(country)) {
                // country code bias: http://en.wikipedia.org/wiki/CcTLD
                sb.append("&gl=").append(country);
            }
            return sb.toString();
        }
        
        /* assemble URL */
        sb.append(this.getGeoPointGeocodeURI());
        sb.append("output=xml");
        sb.append("&oe=utf8");

        /* address/country */
        sb.append("&q=").append(URIArg.encodeArg(address));
        if (!StringTools.isBlank(country)) {
            sb.append("&gl=").append(country);
        }

        /* sensor */
        String sensor= this.getProperties().getString(PROP_sensor, null);
        if (!StringTools.isBlank(sensor)) {
            sb.append("&sensor=").append(sensor);
        }

        /* channel */
        String channel = this.getProperties().getString(PROP_channel, null);
        if (!StringTools.isBlank(channel)) {
            sb.append("&channel=").append(channel);
        }

        /* key */
        String auth = this.getAuthorization();
        if (StringTools.isBlank(auth) || auth.startsWith("*")) {
            // invalid key
        } else
        if (auth.startsWith(CLIENT_ID_PREFIX)) {
            sb.append("&client=").append(auth);
        } else {
            sb.append("&key=").append(auth);
        }

        /* return url */
        String defURL = sb.toString();
        if (this.signature == null) {
            return defURL;
        } else {
            String urlStr = this.signature.signURL(defURL);
            return (urlStr != null)? urlStr : defURL;
        }

    }

    /* return geocode */
    // http://code.google.com/apis/maps/documentation/geocoding/index.html
    public GeoPoint getGeocode(String address, String country)
    {

        /* URL */
        String url = this.getGeoPointGeocodeURL(address, country);
        Print.logDebug("Google GC URL: " + url);
        
        /* create XML document */
        Document xmlDoc = GetXMLDocument(url, this.getGeocodeTimeout());
        if (xmlDoc == null) {
            return null;
        }
        
        /* vars */
        String errCode = null;
        GeoPoint geoPoint = null;

        /* parse "xml" */
        Element kml = xmlDoc.getDocumentElement();
        if (kml.getTagName().equalsIgnoreCase(TAG_kml)) {
            NodeList ResponseList = XMLTools.getChildElements(kml,TAG_Response);
            for (int g = 0; (g < ResponseList.getLength()); g++) {
                Element response = (Element)ResponseList.item(g);
                NodeList responseNodes = response.getChildNodes();
                for (int n = 0; n < responseNodes.getLength(); n++) {
                    Node responseNode = responseNodes.item(n);
                    if (!(responseNode instanceof Element)) { continue; }
                    Element responseElem = (Element)responseNode;
                    String responseNodeName = responseElem.getNodeName();
                    if (responseNodeName.equalsIgnoreCase(TAG_name)) {
                        // <name>1600 Amphitheatre Parkway, Mountain View, CA</name>
                    } else
                    if (responseNodeName.equalsIgnoreCase(TAG_Status)) {
                        // <Status> ... </Status>
                        NodeList statusNodes = responseElem.getChildNodes();
                        for (int st = 0; st < statusNodes.getLength(); st++) {
                            Node statusNode = statusNodes.item(st);
                            if (!(statusNode instanceof Element)) { continue; }
                            Element statusElem = (Element)statusNode;
                            String statusNodeName = statusElem.getNodeName();
                            if (statusNodeName.equalsIgnoreCase(TAG_code)) {
                                errCode = StringTools.trim(GoogleGeocodeV2.GetNodeText(statusElem));  // expect "200"
                                break; // we only care about the 'code'
                            }
                        }
                    } else
                    if (responseNodeName.equalsIgnoreCase(TAG_Placemark)) {
                        // <Placemark> ... </Placemark>
                        String id = responseElem.getAttribute(ATTR_id);
                        if ((id != null) && id.equals("p1")) {
                            NodeList placemarkNodes = responseElem.getChildNodes();
                            for (int pm = 0; (geoPoint == null) && (pm < placemarkNodes.getLength()); pm++) {
                                Node placemarkNode = placemarkNodes.item(pm);
                                if (!(placemarkNode instanceof Element)) { continue; }
                                Element placemarkElem = (Element)placemarkNode;
                                String placemarkNodeName = placemarkElem.getNodeName();
                                if (placemarkNodeName.equalsIgnoreCase(TAG_Point)) {
                                    NodeList pointNodes = placemarkElem.getChildNodes();
                                    for (int ptn = 0; (geoPoint == null) && (ptn < pointNodes.getLength()); ptn++) {
                                        Node pointNode = pointNodes.item(ptn);
                                        if (!(pointNode instanceof Element)) { continue; }
                                        Element pointElem = (Element)pointNode;
                                        String pointNodeName = pointElem.getNodeName();
                                        if (pointNodeName.equalsIgnoreCase(TAG_coordinates)) {
                                            String ll[] = StringTools.split(GoogleGeocodeV2.GetNodeText(pointElem),',');
                                            if (ll.length >= 2) {
                                                double lon = StringTools.parseDouble(ll[0],0.0); // longitude is first
                                                double lat = StringTools.parseDouble(ll[1],0.0); 
                                                if (GeoPoint.isValid(lat,lon)) {
                                                    geoPoint = new GeoPoint(lat,lon);
                                                    break; // we only care about the 'GeoPoint'
                                                }
                                            }
                                        }
                                    }                                    
                                }
                            }
                        } else {
                            //Print.logInfo("Skipping Placemark ID = %s", id);
                        }
                    }
                }
            }
        }

        /* create address */
        if (geoPoint != null) {
            // GeoPoint found 
            return geoPoint;
        }
        
        /* check for Google reverse-geocode limit exceeded */
        if ((errCode != null) && errCode.equals("620")) {
            Print.logError("!!!! Google Reverse-Geocode Limit Exceeded [Error 620] !!!!");
        }

        /* no reverse-geocode available */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static Document GetXMLDocument(String url, int timeoutMS)
    {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream input = HTMLTools.inputStream_GET(url, timeoutMS);
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

    /* return the value of the XML text node (never null) */
    protected static String GetNodeText(Node root)
    {
        StringBuffer sb = new StringBuffer();
        if (root != null) {
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) { // CDATA Section
                    sb.append(n.getNodeValue());
                } else
                if (n.getNodeType() == Node.TEXT_NODE) {
                    sb.append(n.getNodeValue());
                } else {
                    //Print.logWarn("Unrecognized node type: " + n.getNodeType());
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_ACCOUNT[]       = new String[] { "account", "a"  };
    private static final String ARG_GEOCODE[]       = new String[] { "geocode", "gc" };
    private static final String ARG_REVGEOCODE[]    = new String[] { "revgeo" , "rg" };
    
    private static String FilterID(String id)
    {
        if (id == null) {
            return null;
        } else {
            StringBuffer newID = new StringBuffer();
            int st = 0;
            for (int i = 0; i < id.length(); i++) {
                char ch = Character.toLowerCase(id.charAt(i));
                if (Character.isLetterOrDigit(ch)) {
                    newID.append(ch);
                    st = 1;
                } else
                if (st == 1) {
                    newID.append("_");
                    st = 0;
                } else {
                    // ignore char
                }
            }
            while ((newID.length() > 0) && (newID.charAt(newID.length() - 1) == '_')) {
                newID.setLength(newID.length() - 1);
            }
            return newID.toString();
        }
    }

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(ENCODING_UTF8);
        String accountID = RTConfig.getString(ARG_ACCOUNT,"demo");
        GoogleGeocodeV2 gn = new GoogleGeocodeV2("google", null, null);

        /* reverse geocode */
        if (RTConfig.hasProperty(ARG_REVGEOCODE)) {
            GeoPoint gp = new GeoPoint(RTConfig.getString(ARG_REVGEOCODE,null));
            if (!gp.isValid()) {
                Print.logInfo("Invalid GeoPoint specified");
                System.exit(1);
            }
            Print.logInfo("Reverse-Geocoding GeoPoint: " + gp);
            Print.sysPrintln("RevGeocode = " + gn.getReverseGeocode(gp,null/*localeStr*/));
            // Note: Even though the values are printed in UTF-8 character encoding, the
            // characters may not appear to be property displayed if the console display
            // does not support UTF-8.
            System.exit(0);
        }

        /* no options */
        Print.sysPrintln("No options specified");
        System.exit(1);

    }

}
