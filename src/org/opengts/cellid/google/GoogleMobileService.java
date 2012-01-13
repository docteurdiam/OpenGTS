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
// Description: (EXPERIMENTAL!  Still under development!)
//  Tools for obtaining information from the Google Mobile Service API
// ----------------------------------------------------------------------------
// Change History:
//  2010/05/24  Martin D. Flynn
//     -Initial release
//  2011/06/16  Martin D. Flynn
//     -Moved from org.opengts.google
// ----------------------------------------------------------------------------
package org.opengts.cellid.google;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.cellid.*;

import org.opengts.cellid.CellTower;

/**
*** Tools for obtaining information from the Google Mobile Service API
**/

public class GoogleMobileService
    extends MobileLocationProviderAdapter
    implements MobileLocationProvider
{

    // ------------------------------------------------------------------------

    private static final String  MOBILE_SERVICE_URI             = "http://www.google.com/glm/mmap";
    
    private static final String  VERSION                        = "0.1.2";

    // ------------------------------------------------------------------------

    private static final String  PROP_timeoutMS                 = "timeoutMS";
   
    // ------------------------------------------------------------------------

    private static final long    DefaultServiceTimeout          = 5000L; // milliseconds

    // ------------------------------------------------------------------------

    /**
    *** Encode request array
    **/
    private static byte[] encodeRequest(CellTower cti)
    {

        /* invalid cell-tower info */
        if (cti == null) {
            return null;
        }

        /* extract cell info */
        int mcc = cti.getMobileCountryCode();
        int mnc = cti.getMobileNetworkCode();
        int cid = cti.getCellTowerID();
        int lac = cti.getLocationAreaCode();

        /* return response */
        return GoogleMobileService.encodeRequest(mcc, mnc, cid, lac);

    }
    
    /**
    *** Encode request array
    **/
    private static byte[] encodeRequest(int mcc, int mnc, int cid, int lac)
    {
        
        /* UMTS CellID? */
        boolean isUMTS = (cid > 65535);
        int cidLen = (cid > 65536)? 5 : 3;

        /* encode */
        Payload p = new Payload();
        p.writeUInt(0x000E,2);      //  0: 2
        p.writeZeroFill(8);         //  2: 8
        p.writeZeroFill(2);         // 10: 2
        p.writeZeroFill(2);         // 12: 2
        p.writeZeroFill(2);         // 14: 2
        p.writeUInt(0x1B,1);        // 16: 1
        p.writeUInt(mnc,4);         // 17: 4
        p.writeUInt(mcc,4);         // 21: 4
        p.writeZeroFill(3);         // 25: 3
        p.writeUInt(cidLen,1);      // 28: 1
        p.writeZeroFill(2);         // 29: 2
        p.writeUInt(cid,4);         // 31: 4
        p.writeUInt(lac,4);         // 35: 4
        p.writeUInt(mnc,4);         // 39: 4
        p.writeUInt(mcc,4);         // 43: 4
        p.writeUInt(0xFF,1);        // 47: 1
        p.writeUInt(0xFF,1);        // 48: 1
        p.writeUInt(0xFF,1);        // 49: 1
        p.writeUInt(0xFF,1);        // 50: 1
        p.writeZeroFill(4);         // 51: 4

        /* return */
        byte b[] = p.getBytes();
        Print.logInfo("Request length: " + b.length);
        return b;

    }

    // ------------------------------------------------------------------------

    /**
    *** Decode location information
    **/
    private static GeoPoint decodeLocation(byte data[])
    {
        
        /* response length */
        if (data == null) {
            Print.logWarn("Response is null");
            return null;
        }
        Print.logInfo("Response length: " + data.length);
        Print.logInfo("Response: " + StringTools.toHexString(data));
        Print.logInfo("Response: " + StringTools.toStringValue(data,'.'));

        /* invalid response */
        if (data.length < 7) {
            return null;
        }

        /* parse */
        Payload p = new Payload(data);
        long   optCode1   = p.readULong(2,0L);
        long   optCode2   = p.readULong(1,0L);
        long   success    = p.readULong(4,0L);
        if (success != 0) {
            Print.logWarn("Google Mobile Service Error: " + success);
            //return null;
        }
        
        /* return GeoPoint */
        double latitude   = p.readLong(4,0L) / 1000000.0;
        double longitude  = p.readLong(4,0L) / 1000000.0;
        return new GeoPoint(latitude, longitude);

    }
    
    // ------------------------------------------------------------------------
    
    /* get CellTower location */
    public static MobileLocation _getMobileLocation(
        int mcc, int mnc, int cid, int lac,
        long timeout)
    {
        if ((mcc > 0) || (mnc > 0)) {
            try {
                byte req[] = GoogleMobileService.encodeRequest(mcc, mnc, cid, lac);
                byte rsp[] = HTMLTools.readPage_POST(MOBILE_SERVICE_URI, HTMLTools.MIME_BINARY(), req, (int)timeout);
                GeoPoint gp = GoogleMobileService.decodeLocation(rsp);
                return new MobileLocation(gp);
            } catch (Throwable th) {
                Print.logException("Google Mobile API Error",th);
                return null;
            }
        } else {
            return null;
        }
    }
    
    /* get CellTower location */
    public static MobileLocation _getMobileLocation(
        CellTower servCT, 
        CellTower nborCT[], // ignored
        long timeoutMS)
    {

        /* invalid cell-tower info */
        if (servCT == null) {
            return null;
        }

        /* extract cell info */
        int mcc = servCT.getMobileCountryCode();
        int mnc = servCT.getMobileNetworkCode();
        int cid = servCT.getCellTowerID();
        int lac = servCT.getLocationAreaCode();

        /* return response */
        return GoogleMobileService._getMobileLocation(
            mcc, mnc, cid, lac,
            timeoutMS);

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // MobileLocationProvider interface
    
    public GoogleMobileService(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    public MobileLocation getMobileLocation(CellTower servCT, CellTower nborCT[]) 
    {
        long tmoMS = this.getProperties().getLong(PROP_timeoutMS, DefaultServiceTimeout);
        return GoogleMobileService._getMobileLocation(servCT, nborCT, tmoMS);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // GMAPI: -mcc=240 -mnc=4   -lac=1341   -cid=67780667     [ 58.39586/ 15.56200]
    // GMAPI: -mcc=208 -mnc=20  -lac=277    -cid=23800        [ 48.72228/  0.26520]
    // GMAPI: -mcc=505 -mnc=1   -lac=60301  -cid=61652        [-31.98531/115.93802]
    // ------------------------------------------------------------------------
    // http://cellid.labs.ericsson.net/json/lookup?cellid=67780667&mnc=4&mcc=240&lac=1341&key=
    // http://www.opencellid.org/cell/get?key=myapikey&mnc=4&mcc=240&lac=1341&cellid=67780667   [57.72080/13.34455]

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        int mcc = RTConfig.getInt("mcc",-1);
        int mnc = RTConfig.getInt("mnc",-1);
        int cid = RTConfig.getInt("cid",-1);
        int lac = RTConfig.getInt("lac",-1);
        CellTower cti = new CellTower(mcc,mnc,-1,cid,lac,-1,-1);
        MobileLocation ml = GoogleMobileService._getMobileLocation(cti, null, -1L);
        Print.logInfo("Mobile Location: " + ml);
    }

}
