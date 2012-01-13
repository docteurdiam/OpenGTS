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
// - This reverse-geocoding uses the services provided by 'GeoNames.org' [http://www.geonames.org]
// - Geonames also provides a commercial-use reverse-geocoding service that guarantees a faster 
//   and more reliable response time.  More information is available at the following link:
//      http://www.geonames.org/commercial-webservices.html
//   Or contact them at the following email address:
//      services@geonames.org
//   (Make sure you let them know you are using it with 'OpenGTS'!)
// ----------------------------------------------------------------------------
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/05/25  Martin D. Flynn
//     -Ignore US zip code if length is less than 5 characters
//  2007/07/14  Martin D. Flynn
//     -Added test for subdivision
//  2007/09/16  Martin D. Flynn
//     -Added postal code lookup.
//     -Added 'style', 'radius', and 'maxRows' parameters to URL
//  2008/03/28  Martin D. Flynn
//     -Modified to support UTF-8 character encoding
//     -Added 'getPlaceNameReverseGeocode' method
//  2009/01/28  Martin D. Flynn
//     -Added properties: 'host, 'failoverHost', 'username', 'token'.  This
//      can now handle Geoname's commercial reverse-geocoding web service.
//  2009/05/01  Martin D. Flynn
//     -Now can support Geonames NAVTEQ/TeleAtlas service.
//     -Added "speedCategory"/"speedRestriction" tag support.
//     -Added ability to override url 'service'.
//     -Now checks both "placename" and "cityName" for city.
//     -Also checks "adminName1" for state name/abbr if "adminCode1" not found.
//  2009/08/07  Martin D. Flynn
//     -"isFastOperation" now returns true if RG is performed locally.
//  2009/09/23  Martin D. Flynn
//     -Added postal-code geocoding "getPostalCodeLocation(...)"
//     -Truncate TAG_name to 40 characters, or less (it was 120 in one location
//      in Toronto Canada).
//  2010/04/11  Martin D. Flynn
//     -Default to "US" if country code is blank.
//  2010/05/24  Martin D. Flynn
//     -Modified to handle Tiger data service
//     -Modified to attempt to eliminate redundant appended commas
//  2011/05/13  Martin D. Flynn
//     -Added "primaryService" property.
//     -Added "placeFailover" property.
//  2011/08/21  Martin D. Flynn
//     -Added status message display when reverse-geocoding fails
// ----------------------------------------------------------------------------
package org.opengts.geocoder.geonames;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.geocoder.*;

public class GeoNames
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider, SubdivisionProvider, GeocodeProvider
{

    // ------------------------------------------------------------------------
    //
    // References:
    //   - http://geonames.wordpress.com/2006/06/25/tiger-line-reverse-geocoder/
    //   - http://www.geonames.org/maps/reverse-geocoder.html
    //
    // Blog:
    //   - http://geonames.wordpress.com/
    //
    // Creating an account:
    //   - http://www.geonames.org/login
    //
    // Purchasing premium service:
    //   - http://www.geonames.org/products/reverse-geocoding.html 
    //
    // Nearest Address:
    //   - http://ws.geonames.org/findNearestAddress?lat=37.459445&lng=-122.179234
    //     Returns an XML file:
    //      <geonames>
    //          <address>
    //              <street>Marcussen Dr</street>
    //              <streetNumber>1147</streetNumber>
    //              <lat>37.45963896437795</lat>
    //              <lng>-122.17913013350552</lng>
    //              <distance>0.02</distance>
    //              <postalcode>94025</postalcode>
    //              <placename>Menlo Park</placename>
    //              <adminName2>San Mateo</adminName2>
    //              <adminCode1>CA</adminCode1>
    //              <adminName1>California</adminName1>
    //              <countryCode>US</countryCode>
    //          </address>
    //      </geonames>
    //
    // Country Code:
    //   - http://ws.geonames.org/countrycode?lat=47.03&lng=10.2
    //     Returns the country code:
    //       AT
    //
    // Country Subdivision:
    //   - http://ws.geonames.org/countrySubdivision?lat=39.03&lng=-121.0
    //     Returns an XML file:
    //       <geonames>
    //           <countrySubdivision>
    //               <countryCode>US</countryCode>
    //               <countryName>United States</countryName>
    //               <adminCode1>CA</adminCode1>
    //               <adminName1>California</adminName1>
    //           </countrySubdivision>
    //       </geonames>
    //
    // Elevation:
    //   - http://ws.geonames.org/gtopo30?lat=47.01&lng=10.2
    //     Returns elevation in meters:
    //       2632
    //
    // Nearby Placename:
    //   - http://ws.geonames.org/findNearbyPlaceName?lat=47.3&lng=9&style=FULL&radius==1.0
    //     Returns an XML file: 
    //      <geonames>
    //        <geoname>
    //          <name>Atzm'nnig</name>
    //          <lat>47.287633</lat>
    //          <lng>8.988454</lng>
    //          <geonameId>6559633</geonameId>
    //          <countryCode>CH</countryCode>
    //          <countryName>Switzerland</countryName>
    //          <fcl>P</fcl>
    //          <fcode>PPL</fcode>
    //          <fclName>city, village,...</fclName>
    //          <fcodeName>populated place</fcodeName>
    //          <population/>
    //          <alternateNames/>
    //          <elevation>0</elevation>
    //          <adminCode1>SG</adminCode1>
    //          <adminName1>Sankt Gallen</adminName1>
    //          <adminCode2/>
    //          <adminName2/>
    //          <distance>1.6275754724230265</distance>
    //        </geoname>
    //      </geonames>
    //
    // Nearby Streets:
    //   - http://ws.geonames.org/findNearbyStreetsOSM?lat=38.94&lng=-121.05
    //      <geonames>
    //        <streetSegment>
    //          <line>
    //            ...
    //          </line>
    //          <distance>0.1</distance>
    //          <name>I-80</name>
    //          <highway>motorway</highway>
    //          <oneway>true</oneway>
    //          <ref>I 80</ref>
    //        </streetSegment>
    //        ...
    //      </geonames>
    //
    // Country Info:
    //   - http://ws.geonames.org/countryInfo?
    //     Returns an XML file:
    //
    // Geocoding:
    //   - http://ws.geonames.org/postalCodeSearch?postalcode=95603&country=US&style=full
    //   - http://ws.geonames.org/postalCodeSearch?placename=95603&country=US&style=full
    //   - http://ws.geonames.org/search?name=San%20Francisco&adminCode1=CA&country=US&type=xml&style=full&featureCode=PPL&featureCode=PPLA&featureCode=PPLC&featureCode=PPLG&featureCode=PPLG&featureCode=PPLL&featureCode=PPLS
    //   (Country codes are ISO-3166)
    //
    // ------------------------------------------------------------------------
    
    protected static final String TAG_geonames                  = "geonames";               // main tag
    protected static final String TAG_geoname                   = "geoname";            
    protected static final String TAG_streetSegment             = "streetSegment";          // "findNearbyStreetsOSM"
    protected static final String TAG_address                   = "address";
    protected static final String TAG_code                      = "code";
    protected static final String TAG_countrySubdivision        = "countrySubdivision";
    protected static final String TAG_street                    = "street";                 // Street name
    protected static final String TAG_streetNumber              = "streetNumber";           // Street number
    protected static final String TAG_lat                       = "lat";                    // Latitude
    protected static final String TAG_lng                       = "lng";                    // Longitude
    protected static final String TAG_distance                  = "distance";               // delta distance
    protected static final String TAG_postalcode                = "postalcode";             // Zip code
    protected static final String TAG_postalCode                = "postalCode";             // Zip code
    protected static final String TAG_name                      = "name";                   // code:City
    protected static final String TAG_placename                 = "placename";              // address:City
    protected static final String TAG_cityName                  = "cityName";               // address:City
    protected static final String TAG_adminName2                = "adminName2";             // County
    protected static final String TAG_adminCode1                = "adminCode1";             // State abbrev (eg. "CA")
    protected static final String TAG_adminName1                = "adminName1";             // State name (eg. "California")
    protected static final String TAG_countryCode               = "countryCode";            // Country abbrev (eg. "US")
    protected static final String TAG_countryName               = "countryName";            // Country name (eg. "United States")

    protected static final String TAG_speedCategory             = "speedCategory";          // Speed Category (1..8)
    protected static final String TAG_speedRestriction          = "speedRestriction";       // Speed Restriction (1..8)
    
    protected static final String TAG_isTollRoad                = "isTollRoad";             // is Toll-Road
    
    protected static final String TAG_status                    = "status";                 // status message

    protected static final String ATTR_message                  = "message";                 // status message

    // ------------------------------------------------------------------------

    protected static final String PROP_radiusKM                 = "radiusKM";               // Double: 1.0 <= radiusKM <= 5.0
    protected static final String PROP_hostName                 = "host";                   // String: "ws.geonames.org"
    protected static final String PROP_failoverHostName         = "failoverHost";           // String: ""
    
    protected static final String PROP_primaryService           = "primaryService";         // String: findNearestAddress|findNearbyPostalCodes|findNearbyStreetsOSM|findNearbyPlaceName
    protected static final String PROP_addressFailover          = "addressFailover";        // Boolean: false
    protected static final String PROP_postalFailover           = "postalFailover";         // Boolean: false
    protected static final String PROP_streetFailover           = "streetFailover";         // Boolean: false
    protected static final String PROP_placeFailover            = "placeFailover";          // Boolean: false
    
    protected static final String PROP_username                 = "username";               // String: ""
    protected static final String PROP_token                    = "token";                  // String: ""
    protected static final String PROP_service_                 = "service.";               // String: "findNearestAddress" (rename)

    // ------------------------------------------------------------------------

    protected static       String HOST_PRIMARY                  = "ws.geonames.org";
    protected static       String HOST_PRIMARY2                 = "ws.geonames.net";
    protected static       String HOST_FAILOVER                 = null;

    protected static final String SERVICE_findNearestAddress    = "findNearestAddress";     // 1 credit
    protected static final String SERVICE_findNearbyPlaceName   = "findNearbyPlaceName";    // 4 credits
    protected static final String SERVICE_countrySubdivision    = "countrySubdivision";     // 1 credit?
    protected static final String SERVICE_findNearbyPostalCodes = "findNearbyPostalCodes";  // 2 credits
    protected static final String SERVICE_findNearbyStreetsOSM  = "findNearbyStreetsOSM";   // ?

    // ------------------------------------------------------------------------

    protected static final int    TIMEOUT_ReverseGeocode        = 2500; // milliseconds
    protected static final int    TIMEOUT_Geocode               = 5000; // milliseconds

    // ------------------------------------------------------------------------

    // address has to be within this distance to qualify (cannot be greater than 5.0 kilometers)
    protected static final double MAX_ADDRESS_DISTANCE_KM       = 1.0;  // required 1.0 for free Geonames
    // protected static final double MAX_ADDRESS_DISTANCE_KM     = 4.5;

    // ------------------------------------------------------------------------

    protected static final String ENCODING_UTF8                 = StringTools.CharEncoding_UTF_8;

    // ------------------------------------------------------------------------

    private static double speedCategoryMap[] = new double[] {
        999.00,    // 0 <= 999/999 (should not occur)
        999.00,    // 1 <= 999/999
        129.37,    // 2 <= 130/80   80.38
        101.50,    // 3,<= 100/64   63.07
         88.45,    // 4 <=  90/54   54.96
         67.18,    // 5 <=  70/40   41.74
         49.14,    // 6 <=  50/30   30.53
         31.09,    // 7 <=  30/20   19.31
         09.02,    // 8 <=  10/ 5    5.60
    };

    private static double speedCategoryMapUS[][] = new double[][] {
        // km/h     mph
        { 999.00, 999.00                                },   // 0 <= 999/999 (should not occur)
        { 999.00, 999.00                                },   // 1 <= 999/999
        { 130.00,  80.00 * GeoPoint.KILOMETERS_PER_MILE },   // 2 <= 130/80  
        { 100.00,  65.00 * GeoPoint.KILOMETERS_PER_MILE },   // 3,<= 100/64  
        {  90.00,  55.00 * GeoPoint.KILOMETERS_PER_MILE },   // 4 <=  90/54  
        {  70.00,  40.00 * GeoPoint.KILOMETERS_PER_MILE },   // 5 <=  70/40  
        {  50.00,  30.00 * GeoPoint.KILOMETERS_PER_MILE },   // 6 <=  50/30  
        {  30.00,  20.00 * GeoPoint.KILOMETERS_PER_MILE },   // 7 <=  30/20  
        {  10.00,   5.00 * GeoPoint.KILOMETERS_PER_MILE },   // 8 <=  10/ 5  
    };

    private static double getSpeedCategoryKPH(int n, boolean isUS)
    {
        int x = isUS? 1 : 0;
        if (n <= 0) {
            return speedCategoryMapUS[0][x];
        } else
        if (n >= 8) {
            return speedCategoryMapUS[8][x];
        } else {
            return speedCategoryMapUS[n][x];
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean IsUSCountry(String country)
    {
      //return (country != null) && country.equalsIgnoreCase(ReverseGeocode.COUNTRY_US);
        return (country == null) || country.equalsIgnoreCase(ReverseGeocode.COUNTRY_US);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String getPostalCodeGeocodeURL(String postalCode, String country)
    {
        // http://ws.geonames.org/postalCodeSearch?postalcode=95603&country=US&style=long&maxRows=5
        if (!StringTools.isBlank(postalCode)) {
            StringBuffer url = new StringBuffer();
            url.append("http://ws.geonames.org/postalCodeSearch?");
            url.append("postalcode=").append(postalCode);
            if (!StringTools.isBlank(country)) {
                url.append("&country=").append(country);
            }
            url.append("&style=long");
            url.append("&maxRows=5");
            return url.toString();
        } else {
            return null;
        }
    }
    
    public static GeoPoint getPostalCodeLocation(String postalCode, String country, int timeoutMS)
    {
        // <geonames>
        //    <totalResultsCount>1</totalResultsCount>
        //    <code>
        //       <postalcode>95603</postalcode>
        //       <name>Auburn</name>
        //       <countryCode>US</countryCode>
        //       <lat>38.9115</lat>
        //       <lng>-121.08</lng>
        //       <adminCode1>CA</adminCode1>
        //       <adminName1>California</adminName1>
        //       <adminCode2>061</adminCode2>
        //       <adminName2>Placer</adminName2>
        //       <adminCode3/>
        //       <adminName3/>
        //    </code>
        // </geonames>
        
        /* URL */
        String url = getPostalCodeGeocodeURL(postalCode, country);
        if (StringTools.isBlank(url)) {
            return null;
        }
        
        /* get XML document */
        Document xmlDoc = GeoNames.GetXMLDocument(url, timeoutMS);
        if (xmlDoc == null) {
            return null;
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {

            /* code */
            NodeList codeList = XMLTools.getChildElements(geonames,TAG_code);
            for (int a = 0; a < codeList.getLength(); a++) {
                Element code = (Element)codeList.item(a);
                Map<String,String> codeProps = GetElementValueMap(code);
                double lat = StringTools.parseDouble(codeProps.get(TAG_lat),-999.0);
                double lon = StringTools.parseDouble(codeProps.get(TAG_lng),-999.0);
                if (GeoPoint.isValid(lat,lon)) {
                    return new GeoPoint(lat,lon);
                }
            }

            /* status message */
            NodeList statusList = XMLTools.getChildElements(geonames,TAG_status);
            for (int s = 0; s < statusList.getLength(); s++) {
                Element status = (Element)statusList.item(s);
                String message = XMLTools.getAttribute(status,ATTR_message,null,false);
                if (!StringTools.isBlank(message)) {
                    Print.logWarn("Geonames Status: " + message);
                }
            }

        }
        
        /* not found */
        return null;

    }

    public static String getPostalCodeLocation_xml(String postalCode, String country, int timeoutMS)
    {

        /* URL */
        String url = getPostalCodeGeocodeURL(postalCode, country);
        if (StringTools.isBlank(url)) {
            return null;
        }
        
        /* get XML String */
        try {
            byte xml[] = HTMLTools.readPage_GET(url.toString(), timeoutMS);
            return StringTools.toStringValue(xml);
        } catch (Throwable th) {
            Print.logError("GeoNames URL: " + url);
            Print.logError("GeoName postalCode retrieval error: " + th);
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String getCityGeocodeURL(String city, String state, String country)
    {
        // http://ws.geonames.org/search?name=San%20Francisco&adminCode1=CA&country=US&type=xml&style=full&featureCode=PPL&featureCode=PPLA&featureCode=PPLC&featureCode=PPLG&featureCode=PPLG&featureCode=PPLL&featureCode=PPLS
        if (!StringTools.isBlank(city)) {
            StringBuffer url = new StringBuffer();
            url.append("http://ws.geonames.org/search?");
            url.append("name=").append(URIArg.encodeArg(city));
            if (!StringTools.isBlank(state)) {
                url.append("&adminCode1=").append(state);
            }
            if (!StringTools.isBlank(country)) {
                url.append("&country=").append(country);
            }
            url.append("&type=xml");
            url.append("&style=full");
            url.append("&maxRows=3");
            url.append("&featureCode=PPL");
            url.append("&featureCode=PPLA");
            url.append("&featureCode=PPLC");
            url.append("&featureCode=PPLG");
            url.append("&featureCode=PPLG");
            url.append("&featureCode=PPLL");
            url.append("&featureCode=PPLS");
            return url.toString();
        } else {
            return null;
        }
    }
    
    public static GeoPoint getCityLocation(String address, String state, String country, int timeoutMS)
    {
        // <geonames style="FULL">
        //   <totalResultsCount>3</totalResultsCount>
        //   <geoname>
        //     <name>San Francisco</name>
        //     <lat>37.7749295</lat>
        //     <lng>-122.4194155</lng>
        //     <geonameId>5391959</geonameId>
        //     <countryCode>US</countryCode>
        //     <countryName>United States</countryName>
        //     <fcl>P</fcl>
        //     <fcode>PPL</fcode>
        //     <fclName>city, village,...</fclName>
        //     <fcodeName>populated place</fcodeName>
        //     <population>732072</population>
        //     <alternateNames>Frisco,Kapalakiko,Lungsod ng San Francisco,QSF,San Francisco,San Franciscu,San Franciskas,San Francisko,San Fransisco,San Frantzisko,San-Francisko,Sanctus Franciscus,Sanfrancisko,??? ?????????,??? ?????????,???-?????????,???-?????????,?? ????????,??? ?????????,??? ?????????,?????????????,????????????,???-?????????,????????,???,??????</alternateNames>
        //     <elevation>16</elevation>
        //     <continentCode>NA</continentCode>
        //     <adminCode1>CA</adminCode1>
        //     <adminName1>California</adminName1>
        //     <adminCode2>075</adminCode2>
        //     <adminName2>San Francisco County</adminName2>
        //     <alternateName lang="tl">Lungsod ng San Francisco</alternateName>
        //     <alternateName lang="ast">San Francisco</alternateName>
        //     <alternateName lang="nl">San Francisco</alternateName>
        //     <timezone dstOffset="-7.0" gmtOffset="-8.0">America/Los_Angeles</timezone>
        //     <score>1.0</score>
        //   </geoname>
        // </geonames>
        
        /* URL */
        String url = getCityGeocodeURL(address, state, country);
        if (StringTools.isBlank(url)) {
            return null;
        }
        
        /* get XML document */
        Document xmlDoc = GeoNames.GetXMLDocument(url, timeoutMS);
        if (xmlDoc == null) {
            return null;
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {
            NodeList codeList = XMLTools.getChildElements(geonames,TAG_code);
            for (int a = 0; a < codeList.getLength(); a++) {
                Element code = (Element)codeList.item(a);
                Map<String,String> codeProps = GetElementValueMap(code);
                double lat = StringTools.parseDouble(codeProps.get(TAG_lat),-999.0);
                double lon = StringTools.parseDouble(codeProps.get(TAG_lng),-999.0);
                if (GeoPoint.isValid(lat,lon)) {
                    return new GeoPoint(lat,lon);
                }
            }
        }

        /* status message */
        NodeList statusList = XMLTools.getChildElements(geonames,TAG_status);
        for (int s = 0; s < statusList.getLength(); s++) {
            Element status = (Element)statusList.item(s);
            String message = XMLTools.getAttribute(status,ATTR_message,null,false);
            if (!StringTools.isBlank(message)) {
                Print.logWarn("Geonames Status: " + message);
            }
        }

        /* not found */
        return null;

    }

    public static String getCityLocation_xml(String city, String state, String country, int timeoutMS)
    {
        //Print.logInfo("City="+city + ", State="+state + ", Country="+country);

        /* URL */
        String url = getCityGeocodeURL(city, state, country);
        if (StringTools.isBlank(url)) {
            return null;
        }
        //Print.logInfo("URL="+url);

        /* get XML String */
        try {
            byte xml[] = HTMLTools.readPage_GET(url.toString(), timeoutMS);
            String xmlStr = StringTools.toStringValue(xml);
            //Print.logInfo("Query XML:\n" + xmlStr);
            return xmlStr;
        } catch (Throwable th) {
            Print.logError("GeoNames URL: " + url);
            Print.logError("GeoName city retrieval error: " + th);
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Wrapper class for creating the Geonames response XML document
    **/
    private abstract class GeonamesXML
    {
        private String serviceName;
        public GeonamesXML(String serviceName) {
            RTProperties rtp = GeoNames.this.getProperties();
            this.serviceName = rtp.getString(PROP_service_ + serviceName, serviceName);
        }
        protected String getServiceName() {
            return this.serviceName;
        }
        protected abstract String getURL(boolean primary, GeoPoint gp);
        public Document getXMLDocument(GeoPoint gp, int timeoutMS) {
            Document xmlDoc = null;
            String url = this.getURL(true, gp);
            if (url != null) {
                Print.logInfo("Primary URL: " + url);
                xmlDoc = GeoNames.GetXMLDocument(url, timeoutMS); // primary
                if (xmlDoc == null) {
                    url = this.getURL(false, gp);
                    if (url != null) {
                        Print.logInfo("Failover URL: " + url);
                        xmlDoc = GeoNames.GetXMLDocument(url, timeoutMS); // failover
                    }
                }
            }
            return xmlDoc;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Construct authorization key
    **/
    private static String _createAuthKey(RTProperties rtProps)
    {
        if (rtProps != null) {
            String username = rtProps.getString(PROP_username, "");
            String token    = rtProps.getString(PROP_token   , "");
            if (StringTools.isBlank(token)) {
                return username; // may still be blank
            } else {
                return username + ":" + token;
            }
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param name    The name assigned to this ReverseGeocodeProvider
    *** @param key     The optional authorization key
    *** @param rtProps The properties associated with this ReverseGeocodeProvider
    **/
    public GeoNames(String name, String key, RTProperties rtProps)
    {
        super(name, null, rtProps);
        this.init_findNearbyPlaceName();
        this.init_findNearestAddress();
        this.init_findNearbyPostalCodes();
        this.init_findNearbyStreetsOSM();
        this.init_countrySubdivision();
        if (!StringTools.isBlank(key) && !key.startsWith("***") && !this.hasUsername()) {
            // if 'username' is blank, then parse/assign username from key
            int p = key.indexOf(":");
            this.getProperties().setString(PROP_username, ((p >= 0)? key.substring(0,p) : key));
            this.getProperties().setString(PROP_token   , ((p >= 0)? key.substring(p+1) : "" ));
        }
        super.setAuthorization(_createAuthKey(rtProps));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if locally resolved, false otherwise.
    *** (ie. remote address resolution takes more than 20ms to complete)
    *** @return true if locally resolved, false otherwise.
    **/
    public boolean isFastOperation()
    {
        String host = this.getHostname(true);
        if (host.startsWith("localhost") || host.startsWith("127.0.0.1")) {
            // resolved locally, assume fast
            return true;
        } else {
            // this may be a slow operation
            return super.isFastOperation();
        }
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

    /**
    *** Returns true if a username has been defined
    *** @return True if a username has been defined
    **/
    public boolean hasUsername()
    {
        String userName = this.getProperties().getString(PROP_username,null);
        return StringTools.isBlank(userName);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a ReverseGeocode instance for the specified GeoPoint
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr)
    {
        RTProperties rtProp = this.getProperties();
        ReverseGeocode rg = null;

        /* primary service type */
        String primaryService = rtProp.getString(PROP_primaryService,null);
        if (StringTools.isBlank(primaryService) || primaryService.equalsIgnoreCase("findNearestAddress")) {
            // findNearestAddress
            rg = this.getAddressReverseGeocode(gp, localeStr);
        } else
        if (primaryService.equalsIgnoreCase("findNearbyPostalCodes")) {
            // findNearbyPostalCodes
            rg = this.getPostalReverseGeocode(gp);
        } else
        if (primaryService.equalsIgnoreCase("findNearbyStreetsOSM")) {
            // findNearbyStreetsOSM
            rg = this.getNearbyStreetNameReverseGeocode(gp);
        } else
        if (primaryService.equalsIgnoreCase("findNearbyPlaceName")) {
            rg = this.getPlaceNameReverseGeocode(gp);
        } else {
            // findNearestAddress
            rg = this.getAddressReverseGeocode(gp, localeStr);
        }
        if (rg != null) {
            return rg;
        }

        /* findNearestAddress */
        String addressFailover = rtProp.getString(PROP_addressFailover,"false");
        if (StringTools.parseBoolean(addressFailover,false)) {
            rg = this.getAddressReverseGeocode(gp, localeStr);
            if (rg != null) {
                return rg;
            }
        }

        /* findNearbyPostalCodes failover */
        String postalFailover = rtProp.getString(PROP_postalFailover,"false");
        if (StringTools.parseBoolean(postalFailover,false)) {
            rg = this.getPostalReverseGeocode(gp);
            if (rg != null) {
                return rg;
            }
        }

        /* findNearbyStreetsOSM failover */
        String streetFailover = rtProp.getString(PROP_streetFailover,"false");
        if (StringTools.parseBoolean(streetFailover,false)) {
            rg = this.getNearbyStreetNameReverseGeocode(gp);
            if (rg != null) {
                return rg;
            }
        }

        /* findNearbyPlaceName failover */
        String placeFailover = rtProp.getString(PROP_placeFailover,"false");
        if (StringTools.parseBoolean(placeFailover,false)) {
            rg = this.getPlaceNameReverseGeocode(gp);
            if (rg != null) {
                return rg;
            }
        }

        /* not found */
        return null;
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* GeocodeProvider interface */
    public GeoPoint getGeocode(String address, String country)
    {
        
        /* no address? */
        if (StringTools.isBlank(address)) {
            return null;
        }
        
        /* default to "US" */
        if (StringTools.isBlank(country)) {
            country = "US";
        }

        /* get XML */
        String gpXML = "";
        if (StringTools.isNumeric(address)) {
            // all numeric, US zip code only
            String zip = address;
            gpXML = GeoNames.getPostalCodeLocation_xml(zip, country, this.getGeocodeTimeout());
        } else {
            // city, state
            String a[] = StringTools.split(address,',');
            if (ListTools.isEmpty(a)) {
                gpXML = "";
            } else
            if (a.length >= 2) {
                String state = a[a.length - 1];
                String city  = a[a.length - 2];
                gpXML = GeoNames.getCityLocation_xml(city, state, country, this.getGeocodeTimeout());
            } else {
                String state = "";
                String city  = a[0];
                gpXML = GeoNames.getCityLocation_xml(city, state, country, this.getGeocodeTimeout());
            }
        }

        /* no XML? */
        if (StringTools.isBlank(gpXML)) {
            return null;
        }

        /* parse XML */
        Document xmlDoc = XMLTools.getDocument(gpXML);
        if (xmlDoc == null) {
            return null;
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (!geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {
            Print.logWarn("Invalid Geonames tag found: " + geonames.getTagName());
            return null;
        }

        /* code/geoname nodes */
        NodeList codeList = XMLTools.getChildElements(geonames,TAG_code);
        if ((codeList == null) || (codeList.getLength() == 0)) {
            codeList = XMLTools.getChildElements(geonames,TAG_geoname);
            if ((codeList == null) || (codeList.getLength() == 0)) {
                Print.logWarn("Geonames code/geoname sub-tag not found");
                // TODO: display status message?
                return null;
            }
        }

        /* check first node only */
        double lat = 0.0;
        double lng = 0.0;
        NodeList latLngList = codeList.item(0).getChildNodes();
        for (int n = 0; n < latLngList.getLength(); n++) {
            Node node = latLngList.item(n);
            if (node instanceof Element) {
                String name = node.getNodeName();
                if (StringTools.isBlank(name)) {
                    // skip
                } else
                if (name.equals(TAG_lat)) {
                    lat = StringTools.parseDouble(GeoNames.GetNodeText((Element)node),-999.0);
                } else
                if (name.equals(TAG_lng)) {
                    lng = StringTools.parseDouble(GeoNames.GetNodeText((Element)node),-999.0);
                }
            }
        }
        
        /* return GeoPoint */
        if (GeoPoint.isValid(lat,lng)) {
            return new GeoPoint(lat,lng);
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the hostname
    *** @param primary  True to return the primary host, else failover host
    *** @return The hostname
    **/
    protected String getHostname(boolean primary)
    {
        RTProperties rtp = this.getProperties();
        String host = primary?
            rtp.getString(PROP_hostName        , HOST_PRIMARY ) :
            rtp.getString(PROP_failoverHostName, HOST_FAILOVER);
        if ((host != null) && host.equalsIgnoreCase("default")) {
            // return default host
            return this.hasUsername()? HOST_PRIMARY2 : HOST_PRIMARY;
        } else {
            // return explicit host
            return host;
        }
    }

    /**
    *** Returns the URL for the specified Geonames service
    *** @param primary  True for primary server, false for failover server
    *** @param service  The Geonames service
    *** @return The Geonames service URL
    **/
    protected StringBuffer getGeonamesURL(boolean primary, String service, GeoPoint gp)
    {
        String host = this.getHostname(primary);
        if (!StringTools.isBlank(host) && !StringTools.isBlank(service)) {
            RTProperties rtp = this.getProperties();
            StringBuffer sb = new StringBuffer();

            // URL
            sb.append("http://").append(host).append("/").append(service);
            if (service.endsWith("?")) {
                // continue
            } else
            if (service.endsWith("&")) {
                // continue
            } else
            if (service.indexOf("?") < 0) {
                // ie "geonames/findNearestAddress"
                sb.append("?");
            } else {
                // is "geonames/geonames?srv=findNearbyAddress"
                sb.append("&");
            }

            // optional username
            String username = rtp.getString(PROP_username, null);
            if (!StringTools.isBlank(username)) {
                sb.append("username=").append(username).append("&");
                // optional token
                String token = rtp.getString(PROP_token, null);
                if (!StringTools.isBlank(token)) {
                    sb.append("token=").append(token).append("&");
                }
            }

            // latitude/longitude
            sb.append("lat=" ).append(gp.getLatitudeString( GeoPoint.SFORMAT_DEC_5,null));
            sb.append("&lng=").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));

            // return URL
            return sb;

        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private GeonamesXML xmlDoc_findNearbyPlaceName = null;

    /**
    *** Initializes the 'findNearbyPlaceName' XML document generator.
    *** Must be called from the constructor.
    **/
    protected void init_findNearbyPlaceName()
    {
        this.xmlDoc_findNearbyPlaceName = new GeonamesXML(SERVICE_findNearbyPlaceName) {
            public String getURL(boolean primary, GeoPoint gp) {
                StringBuffer sb = GeoNames.this.getGeonamesURL(primary, this.getServiceName(), gp);
                if (sb != null) {
                    sb.append("&style=").append("FULL");
                    return sb.toString();
                } else {
                    return null;
                }
            }
        };
    }

    /** 
    *** Returns a PlaceName ReverseGeocode instance
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    public ReverseGeocode getPlaceNameReverseGeocode(GeoPoint gp)
    {

        /* XML Document */
        Document xmlDoc = this.xmlDoc_findNearbyPlaceName.getXMLDocument(gp, this.getReverseGeocodeTimeout());
        if (xmlDoc == null) {
            return null; // still no data
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {

            /* parse addresses */
            NodeList placeList = XMLTools.getChildElements(geonames,TAG_geoname);
            for (int a = 0; a < placeList.getLength(); a++) {
                Element place = (Element)placeList.item(a);
                Map<String,String> placeProps = GetElementValueMap(place);
                ReverseGeocode rg = this.createPlaceName(placeProps);
                if (rg != null) {
                    return rg;
                }
            }

            /* status message */
            NodeList statusList = XMLTools.getChildElements(geonames,TAG_status);
            for (int s = 0; s < statusList.getLength(); s++) {
                Element status = (Element)statusList.item(s);
                String message = XMLTools.getAttribute(status,ATTR_message,null,false);
                if (!StringTools.isBlank(message)) {
                    Print.logWarn("Geonames Status: " + message);
                }
            }

        }

        return null;

    }

    /**
    *** Creates a ReverseGeocode instance from the mapped values
    **/
    private ReverseGeocode createPlaceName(Map<String,String> placeProps)
    {
        StringBuffer sb = new StringBuffer();

        /* extract place name */ 
        int maxNameLen = 40;
        String place = placeProps.get(TAG_name);
        if (!StringTools.isBlank(place)) {
            //Print.logInfo("Placename: " + place);
            if (place.length() > maxNameLen) {
                int p = place.indexOf("("); //
                int t = ((p > 0) && (p < maxNameLen))? p : maxNameLen;
                place = place.substring(0,t).trim();
            }
            sb.append(place);
        }

        /* country code */
        String countryCode = placeProps.get(TAG_countryCode);
        String countryName = placeProps.get(TAG_countryName);
        boolean isUS = GeoNames.IsUSCountry(countryCode);
        if (isUS) {
            if (sb.length() > 0) { sb.append(", "); }
            sb.append("USA");
        } else
        if ((countryName != null) && !countryName.equals("")) {
            if (sb.length() > 0) { sb.append(", "); }
            sb.append(countryName);
        } else
        if ((countryCode != null) && !countryCode.equals("")) {
            if (sb.length() > 0) { sb.append(", "); }
            sb.append(countryCode);
        }

        /* return ReverseGeocode */
        String addr = sb.toString().trim();
        if (!addr.equals("")) {
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(addr);
            rg.setCountryCode(countryCode);
            //Print.logInfo("Found address: " + rg);
            return rg;
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private GeonamesXML xmlDoc_findNearestAddress = null;

    /**
    *** Initializes the 'findNearestAddress' XML document generator.
    *** Must be called from the constructor.
    **/
    protected void init_findNearestAddress()
    {
        this.xmlDoc_findNearestAddress = new GeonamesXML(SERVICE_findNearestAddress) {
            public String getURL(boolean primary, GeoPoint gp) {
                StringBuffer sb = GeoNames.this.getGeonamesURL(primary, this.getServiceName(), gp);
                if (sb != null) {
                    sb.append("&style=").append("FULL");
                    sb.append("&radius=").append(GeoNames.this.getMaximumSearchRadius()); // must be <= 5.0 km
                    return sb.toString();
                } else {
                    return null;
                }
            }
        };
    }

    /**
    *** Returns the maximum search radius, in kilometers
    *** @return The maximum search radius, in kilometers
    **/
    protected double getMaximumSearchRadius()
    {
        double radKM = GeoNames.this.getProperties().getDouble(PROP_radiusKM, MAX_ADDRESS_DISTANCE_KM);
        return (radKM < 1.0)? 1.0 : ((radKM > 5.0)? 5.0 : radKM);
    }

    /**
    *** Returns a ReverseGeocode instance containing address information
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    public ReverseGeocode getAddressReverseGeocode(GeoPoint gp, String localeStr)
    {

        /* XML Document */
        Document xmlDoc = this.xmlDoc_findNearestAddress.getXMLDocument(gp, this.getReverseGeocodeTimeout());
        if (xmlDoc == null) {
            return null; // no data
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {

            /* parse addresses */
            double radiusKM = this.getMaximumSearchRadius();
            NodeList addressList = XMLTools.getChildElements(geonames,TAG_address);
            for (int a = 0; a < addressList.getLength(); a++) {
                Element address = (Element)addressList.item(a);
                Map<String,String> addrProps = GetElementValueMap(address);
                ReverseGeocode rg = this.createAddress(addrProps, radiusKM);
                if (rg != null) {
                    return rg;
                }
            }

            /* status message */
            NodeList statusList = XMLTools.getChildElements(geonames,TAG_status);
            for (int s = 0; s < statusList.getLength(); s++) {
                Element status = (Element)statusList.item(s);
                String message = XMLTools.getAttribute(status,ATTR_message,null,false);
                if (!StringTools.isBlank(message)) {
                    Print.logWarn("Geonames Status: " + message);
                }
            }

        }

        return null;
        
    }

    /**
    *** Creates a ReverseGeocode instance from the specified string properties map
    **/
    private ReverseGeocode createAddress(Map<String,String> addrProps, double maxAddressDistanceKM)
    {
        String p = null;
        StringBuffer sb = new StringBuffer();
        String country = addrProps.get(TAG_countryCode);
        boolean isUS = GeoNames.IsUSCountry(country);

        /* extract street address */ 
        double distanceKM = StringTools.parseDouble(addrProps.get(TAG_distance),-1.0);
        if (distanceKM <= maxAddressDistanceKM) {
            this.appendAddressKey(sb, addrProps, TAG_streetNumber, false);  // street number
            this.appendAddressKey(sb, addrProps, TAG_street      , true);   // street
        }
        String streetAddr = sb.toString();

        /* extract city */ 
        String cityTag = addrProps.containsKey(TAG_cityName)? TAG_cityName : TAG_placename;
        String city  = this.appendAddressKey(sb, addrProps, cityTag , true);   // city

        /* extract state */
        String stateTag = addrProps.containsKey(TAG_adminCode1)? TAG_adminCode1 : TAG_adminName1;
        String state = StringTools.trim(addrProps.get(stateTag));
        if (isUS && (state.length() > 2)) {
            String stateAbbr = USState.getStateCode(state);
            if (!StringTools.isBlank(stateAbbr)) {
                state = stateAbbr.toUpperCase();
            } else {
                state = StringTools.setFirstUpperCase(state);
            }
        }
        this.appendAddressValue(sb, state, false, false);  // state abbrev

        /* postal code */
        String postalTag = addrProps.containsKey(TAG_postalCode)? TAG_postalCode : TAG_postalcode;
        String postalCode = addrProps.get(postalTag);
        if (isUS && (postalCode != null) && (postalCode.length() < 5)) { postalCode = null; }
        if (!StringTools.isBlank(postalCode)) {
            if (sb.length() > 0) { sb.append(" "); }
            sb.append(postalCode);
        }

        /* country abbrev (only if not "US") */
        if (!isUS && !StringTools.isBlank(country)) {
            if (sb.length() > 0) { sb.append(" "); }
            sb.append(country);
        }

        /* speed info */
        double speedCat   = GeoNames.getSpeedCategoryKPH(StringTools.parseInt(addrProps.get(TAG_speedCategory),0),isUS);
        double speedLimit = StringTools.parseDouble(addrProps.get(TAG_speedRestriction), 0.0);
        //if (speedCat   >= 998.0) { speedCat   = 0.0; }
        //if (speedLimit >= 998.0) { speedLimit = 0.0; }
        if (isUS) { 
            // "speedRestriction" is in MPH in the US, converto to KPH
            speedLimit *= GeoPoint.KILOMETERS_PER_MILE; 
        }
        //Print.logInfo("Speed %f / %f", speedCat, speedLimit);
        
        /* toll-road */
        boolean isTollRoad = StringTools.parseBoolean(addrProps.get(TAG_isTollRoad),false);

        /* return ReverseGeocode */
        String addr = sb.toString().trim();
        if (!addr.equals("")) {
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(addr);
            rg.setStreetAddress(streetAddr);
            rg.setCity(city);
            rg.setStateProvince(state);
            rg.setPostalCode(postalCode);
            rg.setCountryCode(country);
            if (isUS && !StringTools.isBlank(state)) {
                rg.setSubdivision(ReverseGeocode.COUNTRY_US_ + state);
            }
            if (speedLimit > 0.0) {
                rg.setSpeedLimitKPH(speedLimit);
            } else {
                rg.setSpeedLimitKPH(speedCat);
            }
            rg.setIsTollRoad(isTollRoad);
            //Print.logInfo("Found address: " + rg);
            return rg;
        } else {
            return null;
        }

    }

    /** 
    *** Appends the specified key element from the map to the specified StringBuffer
    *** @param sb  The StringBuffer
    *** @param addrProps  The address key/value map
    *** @param key The address key
    *** @param suffixComma  If true, append a suffixing comma
    *** @return The value of the specified key
    **/
    private String appendAddressKey(StringBuffer sb, Map addrProps, String key, boolean suffixComma)
    {
        String elem = (String)addrProps.get(key);
        return this.appendAddressValue(sb, elem, true, suffixComma);
    }

    /** 
    *** Appends the specified key element from the map to the specified StringBuffer
    *** @param sb  The StringBuffer
    *** @param addrProps  The address key/value map
    *** @param key The address key
    *** @param suffixComma  If true, append a suffixing comma
    *** @return The value of the specified key
    **/
    private String appendAddressValue(StringBuffer sb, String elem, boolean upshiftFirstOnly, boolean suffixComma)
    {
        if (!StringTools.isBlank(elem)) {
            // prepend space separator?
            if (sb.length() > 0) {
                sb.append(" ");
            }
            // Capitalize value?
            if (upshiftFirstOnly) {
                elem = StringTools.setFirstUpperCase(elem);
            }
            // append value
            sb.append(elem); // will not be blank here
            // append trailing comma?
            if (suffixComma && (sb.length() > 0) && (sb.charAt(sb.length()-1) != ',')) {
                // does have a value, but does not already have a trailing comma
                sb.append(",");
            }
        }
        return elem;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private GeonamesXML xmlDoc_findNearbyStreetsOSM = null;
    
    /**
    *** Initializes the 'findNearbyStreetsOSM' XML document generator.
    *** Must be called from the constructor.
    **/
    protected void init_findNearbyStreetsOSM()
    {
        this.xmlDoc_findNearbyStreetsOSM = new GeonamesXML(SERVICE_findNearbyStreetsOSM) {
            public String getURL(boolean primary, GeoPoint gp) {
                StringBuffer sb = GeoNames.this.getGeonamesURL(primary, this.getServiceName(), gp);
                if (sb != null) {
                    sb.append("&style=").append("FULL");
                    sb.append("&maxRows=").append("1");
                    return sb.toString();
                } else {
                    return null;
                }
            }
        };
    }

    /* return reverse-geocode using nearby street name (street number omitted) */
    public ReverseGeocode getNearbyStreetNameReverseGeocode(GeoPoint gp)
    {

        /* XML Document */
        Document xmlDoc = this.xmlDoc_findNearbyStreetsOSM.getXMLDocument(gp, this.getReverseGeocodeTimeout());
        if (xmlDoc == null) {
            return null; // no data
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {

            /* parse name */
            NodeList streetList = XMLTools.getChildElements(geonames,TAG_streetSegment);
            for (int a = 0; a < streetList.getLength(); a++) {
                Element street = (Element)streetList.item(a);
                Map<String,String> streetProps = GetElementValueMap(street);
                ReverseGeocode rg = this.createStreetName(streetProps);
                if (rg != null) {
                    return rg;
                }
            }

            /* status message */
            NodeList statusList = XMLTools.getChildElements(geonames,TAG_status);
            for (int s = 0; s < statusList.getLength(); s++) {
                Element status = (Element)statusList.item(s);
                String message = XMLTools.getAttribute(status,ATTR_message,null,false);
                if (!StringTools.isBlank(message)) {
                    Print.logWarn("Geonames Status: " + message);
                }
            }

        }

        return null;
        

    }

    private ReverseGeocode createStreetName(Map<String,String> codeProps)
    {
        StringBuffer sb = new StringBuffer();
        String streetName = codeProps.get(TAG_name);

        /* return ReverseGeocode */
        if (!streetName.equals("")) {
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(streetName);
            rg.setStreetAddress(streetName);
            //Print.logInfo("Found address: " + rg);
            return rg;
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private GeonamesXML xmlDoc_findNearbyPostalCodes = null;

    /**
    *** Initializes the 'findNearbyPostalCodes' XML document generator.
    *** Must be called from the constructor.
    **/
    protected void init_findNearbyPostalCodes()
    {
        this.xmlDoc_findNearbyPostalCodes = new GeonamesXML(SERVICE_findNearbyPostalCodes) {
            public String getURL(boolean primary, GeoPoint gp) {
                StringBuffer sb = GeoNames.this.getGeonamesURL(primary, this.getServiceName(), gp);
                if (sb != null) {
                    sb.append("&style=").append("FULL");
                    sb.append("&maxRows=").append("1");
                    return sb.toString();
                } else {
                    return null;
                }
            }
        };
    }

    /* return reverse-geocode using postal code (street address omitted) */
    public ReverseGeocode getPostalReverseGeocode(GeoPoint gp)
    {

        /* XML Document */
        Document xmlDoc = this.xmlDoc_findNearbyPostalCodes.getXMLDocument(gp, this.getReverseGeocodeTimeout());
        if (xmlDoc == null) {
            return null; // no data
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {

            /* parse addresses */
            NodeList codeList = XMLTools.getChildElements(geonames,TAG_code);
            for (int a = 0; a < codeList.getLength(); a++) {
                Element code = (Element)codeList.item(a);
                Map<String,String> codeProps = GetElementValueMap(code);
                ReverseGeocode rg = this.createPostalCode(codeProps);
                if (rg != null) {
                    return rg;
                }
            }

            /* status message */
            NodeList statusList = XMLTools.getChildElements(geonames,TAG_status);
            for (int s = 0; s < statusList.getLength(); s++) {
                Element status = (Element)statusList.item(s);
                String message = XMLTools.getAttribute(status,ATTR_message,null,false);
                if (!StringTools.isBlank(message)) {
                    Print.logWarn("Geonames Status: " + message);
                }
            }

        }

        return null;
        

    }

    private ReverseGeocode createPostalCode(Map<String,String> codeProps)
    {
        String p = null;
        StringBuffer sb = new StringBuffer();
        String country = codeProps.get(TAG_countryCode);
        boolean isUS = GeoNames.IsUSCountry(country);
        String streetAddr = null; // not available in PostalCode lookup

        /* extract/format address */ 
        String city  = this.appendPostalCodeElement(sb, codeProps, TAG_name        , 40, true);   // city
        String state = this.appendPostalCodeElement(sb, codeProps, TAG_adminCode1  , -1, false);  // state abbrev
        
        /* postal code */
        String postalTag  = codeProps.containsKey(TAG_postalCode)? TAG_postalCode : TAG_postalcode;
        String postalCode = codeProps.get(postalTag);
        if (isUS && (postalCode != null) && (postalCode.length() < 5)) { postalCode = null; }
        if ((postalCode != null) && !postalCode.equals("")) {
            if (sb.length() > 0) { sb.append(" "); }
            sb.append(postalCode);
        }

        /* country abbrev (only if not "US") */
        if (!isUS && !StringTools.isBlank(country)) {
            if (sb.length() > 0) { sb.append(" "); }
            sb.append(country);
        }

        /* return ReverseGeocode */
        String addr = sb.toString().trim();
        if (!addr.equals("")) {
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(addr);
            rg.setStreetAddress(streetAddr);
            rg.setCity(city);
            rg.setStateProvince(state);
            rg.setPostalCode(postalCode);
            rg.setCountryCode(country);
            if (isUS && !StringTools.isBlank(state)) {
                rg.setSubdivision(ReverseGeocode.COUNTRY_US_ + state);
            }
            //Print.logInfo("Found address: " + rg);
            return rg;
        } else {
            return null;
        }

    }
    
    private String appendPostalCodeElement(StringBuffer sb, Map codeProps, String key, int maxLen, boolean suffixComma)
    {
        String elem = (String)codeProps.get(key);
        if (!StringTools.isBlank(elem)) {
            // trim postal code
            int elemLen = elem.length();
            if ((maxLen > 0) && (elemLen > maxLen)) {
                int p = elem.indexOf("("); // stop at first left-pren
                int t = ((p > 0) && (p < maxLen))? p : maxLen;
                elem = elem.substring(0,t).trim();
                elemLen = elem.length();
            }
            if (elemLen > 0) {
                // space separator?
                if (sb.length() > 0) { 
                    sb.append(" "); 
                }
                // element value
                sb.append(elem);
                // trailing comma?
                if (suffixComma && (sb.length() > 0) && (sb.charAt(sb.length()-1) != ',')) {
                    // does have a value, but does not already have a trailing comma
                    sb.append(","); 
                }
            }
        }
        return elem;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private GeonamesXML xmlDoc_countrySubdivision = null;

    /**
    *** Initializes the 'countrySubdivision' XML document generator.
    *** Must be called from the constructor.
    **/
    protected void init_countrySubdivision()
    {
        this.xmlDoc_countrySubdivision = new GeonamesXML(SERVICE_countrySubdivision) {
            public String getURL(boolean primary, GeoPoint gp) {
                StringBuffer sb = GeoNames.this.getGeonamesURL(primary, this.getServiceName(), gp);
                if (sb != null) {
                    return sb.toString();
                } else {
                    return null;
                }
            }
        };
    }

    /* return subdivision */
    public String getSubdivision(GeoPoint gp)
    {

        /* XML Document */
        Document xmlDoc = this.xmlDoc_countrySubdivision.getXMLDocument(gp, this.getReverseGeocodeTimeout());
        if (xmlDoc == null) {
            return null; // still no data
        }

        /* parse "geonames" */
        Element geonames = xmlDoc.getDocumentElement();
        if (geonames.getTagName().equalsIgnoreCase(TAG_geonames)) {

            /* get/parse subdivisions node */
            String subDivTAG = TAG_countrySubdivision;
            NodeList subdivList = XMLTools.getChildElements(geonames,subDivTAG);
            if (subdivList != null) {
                for (int a = 0; a < subdivList.getLength(); a++) {
                    Element subdivElem = (Element)subdivList.item(a);
                    Map<String,String> subdivMap = GetElementValueMap(subdivElem);
                    String country = subdivMap.get(TAG_countryCode);
                    String subdiv  = subdivMap.get(TAG_adminCode1);
                    if ((country != null) && (subdiv != null)) {
                        String state = country + ReverseGeocode.SUBDIVISION_SEPARATOR + subdiv;
                        return state.toUpperCase();
                    }
                }
            } else {
                Print.logError("Geonames SubDivision Tag not found: " + subDivTAG);
            }

            /* status message */
            NodeList statusList = XMLTools.getChildElements(geonames,TAG_status);
            for (int s = 0; s < statusList.getLength(); s++) {
                Element status = (Element)statusList.item(s);
                String message = XMLTools.getAttribute(status,ATTR_message,null,false);
                if (!StringTools.isBlank(message)) {
                    Print.logWarn("Geonames Status: " + message);
                }
            }

        }

        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Reads the XML response from the specified URL location, returning an XML Document
    *** @param url  The Geonames URL
    *** @return An XML Document, or null if the XML Document could not be read
    **/
    protected static Document GetXMLDocument(String url, int timeoutMS)
    {
        try {
            //Print.logInfo("HTTP User-Agent: " + HTMLTools.getHttpUserAgent());
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
    // ------------------------------------------------------------------------

    protected static Map<String,String> GetElementValueMap(Element elem)
    {
        Map<String,String> elemMap = new HashMap<String,String>();
        NodeList attrNodes = elem.getChildNodes();
        for (int n = 0; n < attrNodes.getLength(); n++) {
            Node node = attrNodes.item(n);
            if (node instanceof Element) {
                String name = node.getNodeName();
                String text = GeoNames.GetNodeText((Element)node);
                if (!StringTools.isBlank(name) && !StringTools.isBlank(text)) {
                    elemMap.put(name, text);
                }
            }
        }
        return elemMap;
    }

    // ------------------------------------------------------------------------

    /* return the value of the XML text node */
    protected static String GetNodeText(Node root)
    {
        StringBuffer sb = new StringBuffer();
        if (root != null) {
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) { // CDATA Section
                    sb.append(StringTools.trim(n.getNodeValue()));
                } else
                if (n.getNodeType() == Node.TEXT_NODE) {
                    sb.append(StringTools.trim(n.getNodeValue()));
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
    // http://ws.geonames.org/findNearbyPostalCodes?lat=43.65500&lng=-79.35585&style=FULL&maxRows=1
    // gp=43.65500/-79.35585

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(ENCODING_UTF8);
        GeoNames gn = new GeoNames("geonames", null, RTConfig.getCommandLineProperties());

        /* geocode lookup */
        if (RTConfig.hasProperty("geocode")) {
            String address = RTConfig.getString("geocode",null);
            GeoPoint gp = gn.getGeocode(address, "US");
            Print.sysPrintln("Location " + gp);
            System.exit(0);
        }

        /* zip lookup */
        if (RTConfig.hasProperty("zip")) {
            String zipCode = RTConfig.getString("zip",null);
            GeoPoint gp = GeoNames.getPostalCodeLocation(zipCode, "US", gn.getGeocodeTimeout());
            Print.sysPrintln("Location " + gp);
            System.exit(0);
        }

        /* host */
        String host = RTConfig.getString("host",null);
        if (!StringTools.isBlank(host)) {
            HOST_PRIMARY = host;
        }

        /* failover */
        String failover = RTConfig.getString("failover",null);
        if (!StringTools.isBlank(failover)) {
            HOST_FAILOVER = failover;
        }

        /* GeoPoint */
        GeoPoint gp = new GeoPoint(RTConfig.getString("gp",null));
        if (!gp.isValid()) {
            Print.logInfo("Invalid GeoPoint specified");
            System.exit(1);
        }
        Print.logInfo("Reverse-Geocoding GeoPoint: " + gp);

        /* Reverse Geocoding */
        Print.sysPrintln("RevGeocode = " + gn.getReverseGeocode(gp, null/*localeStr*/));
        Print.sysPrintln("Address    = " + gn.getAddressReverseGeocode(gp, null/*locale*/));
        Print.sysPrintln("PostalCode = " + gn.getPostalReverseGeocode(gp));
        Print.sysPrintln("StreetName = " + gn.getNearbyStreetNameReverseGeocode(gp));
        Print.sysPrintln("PlaceName  = " + gn.getPlaceNameReverseGeocode(gp));
        // Note: Even though the values are printed in UTF-8 character encoding, the
        // characters may not appear to be property displayed if the console display
        // does not support UTF-8.

    }

}
