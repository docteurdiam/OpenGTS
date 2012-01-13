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
// - This reverse-geocoding uses the services provided by 'TinyGeocoder' [http://www.TinyGeocoder.com]
// ----------------------------------------------------------------------------
// Change History:
//  2010/01/29  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.geocoder.tinygeocoder;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.geocoder.*;

public class TinyGeocoder
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider, GeocodeProvider
{

    // ------------------------------------------------------------------------
    //
    // References:
    //   - http://www.tinygeocoder.com
    //
    // Reverse-Geocode: (NOTE: the reverse-geocoder is often overloaded and fails to return a result)
    //   - http://tinygeocoder.com/create-api.php?g=37.775196,-122.419204
    //     Returns:
    //        Market St & Van Ness Ave, United States
    //
    // Geocode:
    //   - http://tinygeocoder.com/create-api.php?q=San+Francisco
    //     Returns:
    //        37.775196,-122.419204
    //
    // ------------------------------------------------------------------------

    protected static       String HOST_PRIMARY                  = "tinygeocoder.com";

    // ------------------------------------------------------------------------

    protected static final int    TIMEOUT_ReverseGeocode        = 2500; // milliseconds
    protected static final int    TIMEOUT_Geocode               = 5000; // milliseconds

    // ------------------------------------------------------------------------

    protected static final String ENCODING_UTF8                 = StringTools.CharEncoding_UTF_8;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String _getPageResponse(String url, int timeoutMS)
    {
        String response = null;
        try {
            Print.logInfo("URL: " + url);
            byte respB[] = HTMLTools.readPage_GET(url, timeoutMS);
            if ((respB != null) && (respB.length > 0)) {
                response = StringTools.toStringValue(respB).trim();
                Print.logInfo("Response: " + response);
                if (StringTools.isBlank(response)) {
                    response = null;
                } else
                if (response.startsWith("620 :")) {
                    // "Result: 620 : Bummer, we've had too many queries to handle. ..."
                    response = null;
                }
            }
        } catch (Throwable th) {
            response = null;
        }
        return response;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String _getGeocodeURL(String address)
    {
        // http://tinygeocoder.com/create-api.php?q=San+Francisco
        if (!StringTools.isBlank(address)) {
            StringBuffer url = new StringBuffer();
            url.append("http://"+HOST_PRIMARY+"/create-api.php?");
            url.append("q=");
            URIArg.encodeArg(url,address);
            return url.toString();
        } else {
            return null;
        }
    }
    
    private static GeoPoint _getGeocode(String address, int timeoutMS)
    {
        
        /* URL */
        String url = _getGeocodeURL(address);
        if (StringTools.isBlank(url)) {
            return null;
        }
        
        /* get HTTP result */
        String result = _getPageResponse(url, timeoutMS);
        if (StringTools.isBlank(result)) {
            return null;
        }

        /* parse "<lat>,<lon>" */
        GeoPoint gp = new GeoPoint(result.trim(),',');
        return gp.isValid()? gp : null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String _getReverseGeocodeURL(GeoPoint gp)
    {
        // http://tinygeocoder.com/create-api.php?g=37.775196,-122.419204
        if ((gp != null) && gp.isValid()) {
            StringBuffer url = new StringBuffer();
            url.append("http://"+HOST_PRIMARY+"/create-api.php?");
            url.append("g=").append(gp.toString(','));
            return url.toString();
        } else {
            return null;
        }
    }
    
    private static String _getReverseGeocode(GeoPoint gp, int timeoutMS)
    {

        /* URL */
        String url = _getReverseGeocodeURL(gp);
        if (StringTools.isBlank(url)) {
            return null;
        }

        /* get HTTP result */
        String result = _getPageResponse(url, timeoutMS);
        if (StringTools.isBlank(result)) {
            return "";
        }

        /* parse address */
        return result.trim();

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
    public TinyGeocoder(String name, String key, RTProperties rtProps)
    {
        super(name, null, rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if locally resolved, false otherwise.
    *** (ie. remote address resolution takes more than 20ms to complete)
    *** @return true if locally resolved, false otherwise.
    **/
    public boolean isFastOperation()
    {
        String host = HOST_PRIMARY;
        if (host.startsWith("localhost") || host.startsWith("127.0.0.1")) {
            // resolved locally, assume fast
            return true;
        } else {
            // this is a slow operation
            return super.isFastOperation();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the ReverseGeocode timeout
    **/
    protected int getReverseGeocodeTimeout()
    {
        return TIMEOUT_ReverseGeocode;
    }

    /**
    *** Returns a ReverseGeocode instance for the specified GeoPoint
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localStr)
    {
        
        /* no GeoPoint? */
        if (gp == null) {
            return null;
        }
        
        /* return address */
        String address = TinyGeocoder._getReverseGeocode(gp, this.getReverseGeocodeTimeout());
        if (!StringTools.isBlank(address)) {
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(address);
            return rg;
        } else {
            return null;
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the Geocode timeout
    **/
    protected int getGeocodeTimeout()
    {
        return TIMEOUT_Geocode;
    }

    /* GeocodeProvider interface */
    public GeoPoint getGeocode(String address, String country)
    {
        
        /* no address? */
        if (StringTools.isBlank(address)) {
            return null;
        }
        
        /* lookup GeoPoint */
        return TinyGeocoder._getGeocode(address, this.getGeocodeTimeout());

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
        TinyGeocoder tg = new TinyGeocoder("tinygeocoder", null, RTConfig.getCommandLineProperties());

        /* geocode lookup */
        if (RTConfig.hasProperty("geocode")) {
            String address = RTConfig.getString("geocode",null);
            GeoPoint gp = tg.getGeocode(address, "US");
            Print.sysPrintln("Location " + gp);
            System.exit(0);
        }

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
        Print.sysPrintln("RevGeocode = " + tg.getReverseGeocode(gp, null/*localeStr*/));

    }

}
