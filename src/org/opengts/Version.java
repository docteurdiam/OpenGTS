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
//  2007/01/25 v1.2.0  [Initial release]
//  2007/02/08 v1.2.1,  2007/02/11 v1.3.0,  2007/02/16 v1.3.1,  2007/02/18 v1.3.2
//  2007/02/26 v1.3.3,  2007/02/28 v1.3.4,  2007/03/11 v1.3.5,  2007/03/16 v1.3.6,
//  2007/03/25 v1.3.7,  2007/03/30 v1.4.0,  2007/04/15 v1.4.1,  2007/04/17 v1.4.2,
//  2007/05/06 v1.4.3,  2007/05/20 v1.4.4,  2007/05/25 v1.4.5,  2007/06/03 v1.4.6,
//  2007/06/13 v1.4.7,  2007/06/14 v1.4.8,  2007/06/30 v1.5.0,  2007/07/14 v1.5.1,
//  2007/07/27 v1.5.2,  2007/08/09 v1.5.3,  2007/09/16 v1.6.0,  2007/11/28 v1.6.1,
//  2007/12/13 v1.6.2,  2008/01/10 v1.6.3,  2008/02/04 v1.6.4,  2008/02/07 v1.6.5,
//  2008/02/11 v1.6.6,  2008/02/17 v1.6.7,  2008/02/21 v1.6.8,  2008/02/27 v1.6.9,
//  2008/03/12 v1.6.10, 2008/03/28 v1.7.0,  2008/04/11 v1.7.1,  2008/05/14 v1.8.0,
//  2008/05/20 v1.8.1,  2008/05/22 v1.8.2,  2008/06/20 v1.8.3,  2008/07/08 v1.8.4,
//  2008/07/21 v1.9.0,  2008/07/27 v1.9.1,  2008/08/08 v1.9.2,  2008/08/15 v1.9.3,
//  2008/08/17 v1.9.4,  2008/08/20 v1.9.5,  2008/08/24 v1.9.6,  2008/09/01 v1.9.7,
//  2008/09/12 v1.9.8,  2008/09/19 v2.0.1,  2008/10/16 v2.0.2,  2008/12/01 v2.0.3,
//  2009/01/01 v2.0.4,  2009/02/01 v2.0.5,  2009/02/20 v2.0.6,  2009/04/02 v2.0.7,
//  2009/05/01 v2.0.8,  2009/05/24 v2.0.9,  2009/05/27 v2.1.0,  2009/06/01 v2.1.1,
//  2009/07/01 v2.1.2,  2009/08/09 v2.1.3,  2009/08/23 v2.1.4,  2009/08/25 v2.1.4.1,
//  2009/09/23 v2.1.5,  2009/10/05 v2.1.6,  2009/11/01 v2.1.7,  2009/11/10 v2.1.8,
//  2009/12/16 v2.1.9,  2010/01/29 v2.2.0,  2010/04/11 v2.2.1,  2010/04/25 v2.2.2,
//  2010/05/24 v2.2.3,  2010/06/17 v2.2.4,  2010/07/04 v2.2.5,  2010/07/18 v2.2.6,
//  2010/09/10 v2.2.7,  2010/10/25 v2.2.8,  2010/11/29 v2.2.9,  2011/01/28 v2.3.0,
//  2011/03/08 v2.3.1,  2011/04/01 v2.3.2,  2011/05/15 v2.3.3,  2011/06/16 v2.3.4,
//  2011/07/01 v2.3.5,  2011/07/15 v2.3.6,  2011/08/21 v2.3.7
//  2011/10/10 v2.3.8
// ----------------------------------------------------------------------------
package org.opengts;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.opengts.util.*;

/**
*** This class provides runtime version information to the OpenGTS modules.
**/

public class Version
{

    // ------------------------------------------------------------------------

    // "public" as of 2.3.4-B06
    public  static final String COPYRIGHT = "Copyright 2007-2011, GeoTelematic Solutions, Inc.";

    // ------------------------------------------------------------------------

    // This string is parsed via 'grep' & 'sed' scripts and thus 
    // ONLY the version value specified within the quotes should change.
    private static final String VERSION = "2.3.8";

    // This public constant should only be accessed externally by the 'GTSAdmin' application.
    public  static final String GTS_ENTERPRISE_PREFIX = "E";
    public  static final String COMPILED_VERSION = 
        ////  GTS_ENTERPRISE_PREFIX + // "Enterprise"
        VERSION;

    // last compile time
    private static final long COMPILE_TIMESTAMP = CompileTime.COMPILE_TIMESTAMP;

    // ------------------------------------------------------------------------

    // package release time (modified by command-line 'sed' script to insert actual epoch time)
    private static final long PACKAGE_TIMESTAMP = 1318276562L;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initializes the RTConfig constant 'version' property.
    **/
    public static void initVersionProperty()
    {
        RTProperties constantProps = RTConfig.getRuntimeConstantProperties();
        if (!constantProps.hasProperty(RTKey.VERSION)) {
            constantProps.setProperty(RTKey.VERSION, Version.getVersion());
            //System.out.println("Set Version = " + Version.getVersion());
        } else {
            // already initialized, no need to reinitialize
        }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the copyright notice
    *** @return The copyright notice
    **/
    public static String getCopyright()
    {
        return COPYRIGHT;
    }

    /** 
    *** Returns the compiled version String
    *** @return The version String
    **/
    public static String getVersion()
    {
        return COMPILED_VERSION;
    }

    /** 
    *** Returns the compiled package release timestamp
    *** @return The package release timestamp
    **/
    public static long getPackageTimestamp()
    {
        return PACKAGE_TIMESTAMP;
    }

    /** 
    *** Returns the last compiled timestamp
    *** @return The last compiled timestamp
    **/
    public static long getCompileTimestamp()
    {
        return COMPILE_TIMESTAMP;
    }
    
    /**
    *** Returns the last compiled time date string
    *** @return The last compiled time date string
    **/
    public static String getCompileTime()
    {
        return (new DateTime(COMPILE_TIMESTAMP)).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this Version
    *** @return A String representation of this Version
    **/
    public static String getInfo()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(Version.COPYRIGHT).append("\n");
        sb.append("Version: ").append(Version.getVersion()).append("\n");
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss zzz", Locale.US);
        if (PACKAGE_TIMESTAMP > 0L) {
            String dfmt = dateFmt.format(new Date(PACKAGE_TIMESTAMP * 1000L));
            sb.append("Package: [" + PACKAGE_TIMESTAMP + "] " + dfmt + "\n");
        }
        if (COMPILE_TIMESTAMP > 0L) {
            String dfmt = dateFmt.format(new Date(COMPILE_TIMESTAMP * 1000L));
            sb.append("Compile: [" + COMPILE_TIMESTAMP + "] " + dfmt + "\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Main entry point to display the current version
    *** @param argv The command-line args
    **/
    public static void main(String argv[])
    {
        if ((argv.length > 0) && argv[0].equals("-info")) {
            // Version: 2.0.5
            // Compile: [1211915119] 2008/05/27 19:36:38 GMT
            // Package: [1211915119] 2008/05/27 19:36:38 GMT
            System.out.println(Version.getInfo());
        } else
        if ((argv.length > 0) && argv[0].equals("-package")) {
            // "1211915119"
            System.out.println(Version.getPackageTimestamp());
        } else
        if ((argv.length > 0) && argv[0].equals("-compile")) {
            // "1211915119"
            System.out.println(Version.getCompileTimestamp());
        } else {
            // "1.8.3"
            System.out.println(Version.getVersion());
        }
    }

    // ------------------------------------------------------------------------

}
