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
//  2008/05/20  Martin D. Flynn
//     -Initial release
//  2009/05/01  Martin D. Flynn
//     -Added support for specifying replacement variables in encoded URLs.
//  2009/09/23  Martin D. Flynn
//     -Added 'getTarget'
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;

import org.opengts.war.tools.*;

public class WebPageURL
    extends WebPageAdaptor
    implements StringTools.ValueFilter
{
    
    // ------------------------------------------------------------------------

    private static int LinkCount = 0;

    // ------------------------------------------------------------------------

    private String  urlString   = null;
    private String  urlTarget   = "_blank";

    public WebPageURL()
    {
        this.setBaseURI("");
        this.setPageName("link_" + (++LinkCount));
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------
    
    public String getURL()
    {
        return (this.urlString != null)? this.urlString : "";
    }

    public void setURL(String url)
    {
        this.urlString = (url != null)? url : "";
    }

    // ------------------------------------------------------------------------

    public String getTarget()
    {
        return !StringTools.isBlank(this.urlTarget)? this.urlTarget : "_blank";
    }
    
    public void setTarget(String targ)
    {
        this.urlTarget = !StringTools.isBlank(targ)? targ : "_blank";
    }
    
    // ------------------------------------------------------------------------
    
    // StringTools.ValueFilter interface
    public String getFilteredValue(String value)
    {
        return URIArg.encodeArg(new StringBuffer(), value, false).toString();
    }

    public String encodePageURL(RequestProperties reqState)
    {
        return StringTools.replaceKeys(this.getURL(), reqState, this/*StringTools.ValueFilter*/);
    }

    public String encodePageURL(RequestProperties reqState, String command)
    {
        return this.encodePageURL(reqState);
    }

    //public String encodePageURL(RequestProperties reqState, String command, String cmdArg)
    //{
    //    return this.encodePageURL(reqState);
    //}

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "url";
    }

    // ------------------------------------------------------------------------

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        String urlDesc = super.getMenuDescription(reqState, parentMenuName);
        if (!StringTools.isBlank(urlDesc)) {
            return urlDesc;
        } else {
            return this.encodePageURL(reqState);
        }
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        // no-op
    }

    // ------------------------------------------------------------------------

}
