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
//  2006/06/30  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;

import org.opengts.util.*;

import org.opengts.geocoder.*;

public class USState
{

    // ------------------------------------------------------------------------

    private static HashMap<String,StateInfo> globalStateMap = new HashMap<String,StateInfo>();
    
    /**
    *** StateInfo class
    **/
    private static class StateInfo
    {

        private String code     = null;
        private String name     = null;
        private String abbrev   = null;

        public StateInfo(String code, String name, String abbrev) {
            this.code   = code;
            this.name   = name;
            this.abbrev = abbrev;
        }
        
        public String getCode() {
            return this.code;
        }
        
        public String getAbbreviation() {
            return this.abbrev;
        }
        
        public String getName() {
            return this.name;
        }
        
    }
    
    // ------------------------------------------------------------------------

    private static StateInfo stateMapArray[] = new StateInfo[] {
        new StateInfo("AL", "Alabama"           , "Ala."  ),
        new StateInfo("AK", "Alaska"            , "Alaska"),
        new StateInfo("AS", "American"          , "Samoa" ),
        new StateInfo("AZ", "Arizona"           , "Ariz." ),
        new StateInfo("AR", "Arkansas"          , "Ark."  ),
        new StateInfo("CA", "California"        , "Calif."),
        new StateInfo("CO", "Colorado"          , "Colo." ),
        new StateInfo("CT", "Connecticut"       , "Conn." ),
        new StateInfo("DE", "Delaware"          , "Del."  ),
        new StateInfo("DC", "Dist. of Columbia" , "D.C."  ),
        new StateInfo("FL", "Florida"           , "Fla."  ),
        new StateInfo("GA", "Georgia"           , "Ga."   ),
        new StateInfo("GU", "Guam"              , "Guam"  ),
        new StateInfo("HI", "Hawaii"            , "Hawaii"),
        new StateInfo("ID", "Idaho"             , "Idaho" ),
        new StateInfo("IL", "Illinois"          , "Ill."  ),
        new StateInfo("IN", "Indiana"           , "Ind."  ),
        new StateInfo("IA", "Iowa"              , "Iowa"  ),
        new StateInfo("KS", "Kansas"            , "Kans." ),
        new StateInfo("KY", "Kentucky"          , "Ky."   ),
        new StateInfo("LA", "Louisiana"         , "La."   ),
        new StateInfo("ME", "Maine"             , "Maine" ),
        new StateInfo("MD", "Maryland"          , "Md."   ),
        new StateInfo("MH", "Marshall Islands"  , "MH"    ),
        new StateInfo("MA", "Massachusetts"     , "Mass." ),
        new StateInfo("MI", "Michigan"          , "Mich." ),
        new StateInfo("FM", "Micronesia"        , "FM"    ),
        new StateInfo("MN", "Minnesota"         , "Minn." ),
        new StateInfo("MS", "Mississippi"       , "Miss." ),
        new StateInfo("MO", "Missouri"          , "Mo."   ),
        new StateInfo("MT", "Montana"           , "Mont." ),
        new StateInfo("NE", "Nebraska"          , "Nebr." ),
        new StateInfo("NV", "Nevada"            , "Nev."  ),
        new StateInfo("NH", "New Hampshire"     , "N.H."  ),
        new StateInfo("NJ", "New Jersey"        , "N.J."  ),
        new StateInfo("NM", "New Mexico"        , "N.M."  ),
        new StateInfo("NY", "New York"          , "N.Y."  ),
        new StateInfo("NC", "North Carolina"    , "N.C."  ),
        new StateInfo("ND", "North Dakota"      , "N.D."  ),
        new StateInfo("MP", "Northern Marianas" , "MP"    ),
        new StateInfo("OH", "Ohio"              , "Ohio"  ),
        new StateInfo("OK", "Oklahoma"          , "Okla." ),
        new StateInfo("OR", "Oregon"            , "Ore."  ),
        new StateInfo("PW", "Palau"             , "PW"    ),
        new StateInfo("PA", "Pennsylvania"      , "Pa."   ),
        new StateInfo("PR", "Puerto Rico"       , "P.R."  ),
        new StateInfo("RI", "Rhode Island"      , "R.I."  ),
        new StateInfo("SC", "South Carolina"    , "S.C."  ),
        new StateInfo("SD", "South Dakota"      , "S.D."  ),
        new StateInfo("TN", "Tennessee"         , "Tenn." ),
        new StateInfo("TX", "Texas"             , "Tex."  ),
        new StateInfo("UT", "Utah"              , "Utah"  ),
        new StateInfo("VT", "Vermont"           , "Vt."   ),
        new StateInfo("VA", "Virginia"          , "Va."   ),
        new StateInfo("VI", "Virgin Islands"    , "V.I."  ),
        new StateInfo("WA", "Washington"        , "Wash." ),
        new StateInfo("WV", "West Virginia"     , "W.Va." ),
        new StateInfo("WI", "Wisconsin"         , "Wis."  ),
        new StateInfo("WY", "Wyoming"           , "Wyo."  ),
    };
    
    static {
        for (int i = 0; i < stateMapArray.length; i++) {
            String st = stateMapArray[i].getCode();
            globalStateMap.put(st, stateMapArray[i]);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the state name for the specified state code
    *** @param code  The state code
    *** @return The state name, or an empty String if the state code was not found
    **/
    public static String getStateName(String code)
    {
        if (code != null) {
            if (code.startsWith(ReverseGeocode.COUNTRY_US_)) { 
                code = code.substring(ReverseGeocode.COUNTRY_US_.length()); 
            }
            StateInfo si = globalStateMap.get(code);
            return (si != null)? si.getName() : "";
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the state code for the specified state name
    *** @param name  The state name
    *** @return The state code, or an empty String if the state name was not found
    **/
    public static String getStateCode(String name)
    {
        if (name != null) {
            for (Iterator<String> i = globalStateMap.keySet().iterator(); i.hasNext();) {
                String code = i.next();
                StateInfo si = globalStateMap.get(code);
                if (si.getName().equalsIgnoreCase(name)) {
                    return code;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the state abbreviation for the specified state code
    *** @param code  The state code
    *** @return The state abbreviation, or an empty String if the state code was not found
    **/
    public static String getStateAbbreviation(String code)
    {
        if (code != null) {
            if (code.startsWith(ReverseGeocode.COUNTRY_US_)) { 
                code = code.substring(ReverseGeocode.COUNTRY_US_.length()); 
            }
            StateInfo si = globalStateMap.get(code);
            return (si != null)? si.getAbbreviation() : "";
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

}
