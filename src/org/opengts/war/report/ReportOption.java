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
//  2007/03/30  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.db.tables.Account;

import org.opengts.war.tools.*;

public class ReportOption
{

    // ------------------------------------------------------------------------

    private String       optName  = null;
    private I18N.Text    optDesc  = null;
    private RTProperties optProps = null;

    // ------------------------------------------------------------------------

    public ReportOption(String name, I18N.Text desc, RTProperties rtProps)
    {
        this.optName  = StringTools.trim(name);
        this.optDesc  = desc;
        this.optProps = rtProps;
    }

    public ReportOption(String name, String desc, RTProperties rtProps)
    {
        this.optName  = StringTools.trim(name);
        this.optDesc  = !StringTools.isBlank(desc)? new I18N.Text(desc): null;
        this.optProps = rtProps;
    }

    public ReportOption(String name)
    {
        this(name, (I18N.Text)null, null);
    }

    // ------------------------------------------------------------------------

    public String getName()
    {
        return this.optName;
    }

    // ------------------------------------------------------------------------

    public void setDescription(I18N.Text desc)
    {
        this.optDesc = desc;
    }

    public String getDescription(Locale locale)
    {
        return (this.optDesc != null)? this.optDesc.toString(locale) : "";
    }

    public String getDescription(final Locale locale, final RequestProperties reqState)
    {
        String desc = this.getDescription(locale);

        /* current account (required) */
        final Account currAcct = (reqState != null)? reqState.getCurrentAccount() : null;
        if (currAcct == null) { // }
            return desc;
        }

        /* KeyValueMap */
        StringTools.KeyValueMap kvm = new StringTools.KeyValueMap() {
            public String getKeyValue(String key, String arg, String dft) {
                if (key.equalsIgnoreCase("kph")) {
                    double kph = StringTools.parseDouble(arg,-1.0);
                    if (kph >= 0.0) {
                        Account.SpeedUnits speedUnits = Account.getSpeedUnits(currAcct);
                        double speed = speedUnits.convertFromKPH(kph);
                        return Math.round(speed) + " " + speedUnits.toString(locale);
                    }
                    return dft;
                } else
                if (key.equalsIgnoreCase("mph")) {
                    double mph = StringTools.parseDouble(arg,-1.0);
                    if (mph >= 0.0) {
                        double kph = mph * GeoPoint.KILOMETERS_PER_MILE;
                        Account.SpeedUnits speedUnits = Account.getSpeedUnits(currAcct);
                        double speed = speedUnits.convertFromKPH(kph);
                        return Math.round(speed) + " " + speedUnits.toString(locale);
                    }
                    return dft;
                } else {
                    // try looking elsewhere for values
                    String v = reqState.getKeyValue(key, arg, null);
                    return (v != null)? v : dft;
                }
            }
        };

        /* standard argument conversion */
        if (desc.indexOf(StringTools.KEY_START) >= 0) {
            desc = StringTools.replaceKeys(desc, 
                kvm, null/*valueFilter*/, 
                StringTools.KEY_START,StringTools.KEY_END,StringTools.ARG_DELIM,StringTools.DFT_DELIM);
        }

        /* special argument conversion */
        String keyStart = "%{";
        String keyEnd   = "}";
        if (desc.indexOf(keyStart) >= 0) {
            desc = StringTools.replaceKeys(desc, 
                kvm, null/*valueFilter*/, 
                keyStart,keyEnd,StringTools.ARG_DELIM,StringTools.DFT_DELIM);
        }
        
        /* return expanded description */
        return desc;

    }

    // ------------------------------------------------------------------------

    public boolean hasProperties()
    {
        return (this.optProps != null);
    }
    
    public RTProperties getProperties()
    {
        return this.optProps;
    }
    
    public void setValue(String key, String val)
    {
        if (this.optProps == null) {
            this.optProps = new RTProperties();
        }
        this.optProps.setString(key, val);
    }
    
    public String getValue(String key)
    {
        return (this.optProps != null)? this.optProps.getString(key,null) : null;
    }

    // ------------------------------------------------------------------------

}
