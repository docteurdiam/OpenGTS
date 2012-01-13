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
//  2009/01/28  Martin D. Flynn
//     -Initial release (adapted from various web examples)
// ----------------------------------------------------------------------------
package org.opengts.war.track;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

public class CharacterEncodingFilter 
    implements Filter 
{

    private FilterConfig filterConfig = null;
    private String       preferredEncoding = null;
    private boolean      overrideRequest = false;

    /**
    *** Initializes filter 
    *** @param filterConfig  The FilterConfig
    **/
    public void init(FilterConfig filterConfig) 
    {
        this.filterConfig = filterConfig;
        this.preferredEncoding = filterConfig.getInitParameter("characterEncoding");
        if (StringTools.isBlank(this.preferredEncoding)) {
            this.preferredEncoding = StringTools.getCharacterEncoding(); // default to platform encoding
        }
        this.overrideRequest = StringTools.parseBoolean(filterConfig.getInitParameter("overrideRequestEncoding"),false);
        //Print.logInfo("Character Encoding: " + this.preferredEncoding);
        //Print.logInfo("Override Request  : " + this.overrideRequest);
    }

    /**
    *** Allows this filter to set attributes
    *** @param req  The ServletRequest
    *** @param res  The ServletResponse
    *** @param next The next filter in the chain
    **/
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain next)
        throws IOException, ServletException 
    {

        //CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML(), this.preferredEncoding);
        res.setCharacterEncoding(this.preferredEncoding);
        if (this.overrideRequest || (req.getCharacterEncoding() == null)) {
            //Print.logInfo("Setting Request Character Encoding: %s", this.preferredEncoding);
            req.setCharacterEncoding(this.preferredEncoding);
        }

        /* call other filters */
        next.doFilter(req, res);

    }

    /**
    *** Destroys filter 
    **/
    public void destroy() 
    {
        this.filterConfig = null;
        this.preferredEncoding = null;
    }

}
