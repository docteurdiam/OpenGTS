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
//  2007/03/11  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class PageDecorationsAdaptor
    implements PageDecorations
{

    // ------------------------------------------------------------------------

    private PageDecorations dftPageDecorations  = null;

    private String          jspURI              = null;

    private String          pageStyle           = null;
    private String          pageHeader          = null;
    private String          pageNavigate        = null;
    private String          pageFooter          = null;
    private String          pageLeft            = null;
    private String          pageRight           = null;

    // ------------------------------------------------------------------------

    public PageDecorationsAdaptor()
    {
    }

    public PageDecorationsAdaptor(PageDecorations dftPageDecor)
    {
        this();
        this.setDefaultPageDecorations(dftPageDecor);
    }

    // ------------------------------------------------------------------------

    public void setDefaultPageDecorations(PageDecorations dftPageDecor)
    {
        this.dftPageDecorations = dftPageDecor;
    }
    
    public PageDecorations getDefaultPageDecorations()
    {
        return this.dftPageDecorations;
    }

    // ------------------------------------------------------------------------

    /* returns true if this PageDecorations instance is backed by a JSP page */
    public boolean hasJspURI()
    {
        return !StringTools.isBlank(this.jspURI);
    }
    
    /* sets the backing JSP page URI */
    public void setJspURI(String jspURI)
    {
        this.jspURI = !StringTools.isBlank(jspURI)? jspURI : null;
    }

    /* returns backing JSP page URI, or null if there is no JSP page backing */
    public String getJspURI()
    {
        return this.jspURI;
    }

    // ------------------------------------------------------------------------

    protected String trimStringRecords(String s)
    {
        if (s != null) {
            String sa[] = StringTools.parseString(s,"\r\n");
            for (int i = 0; i < sa.length; i++) {
                sa[i] = sa[i].trim();
            }
            s = StringTools.join(sa,'\n').trim();
        }
        return s;
    }
    
    // ------------------------------------------------------------------------

    /* set the page style html */
    public void setPageStyle(String style)
    {
        this.pageStyle = this.trimStringRecords(style);
    }

    /* return the page style html */
    public String getPageStyle()
    {
        return this.pageStyle;
    }

    /* write the page header to the specified output stream */
    public void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        String s = this.getPageStyle();
        if (s != null) {
            out.write(s);
            if (!s.endsWith("\n")) {
                out.write("\n");
            }
        } else {
            this.getDefaultPageDecorations().writeStyle(out, reqState);
        }
    }

    // ------------------------------------------------------------------------

    /* set the page header html */
    public void setPageHeader(String header)
    {
        this.pageHeader = this.trimStringRecords(header);
    }

    /* return the page header html */
    public String getPageHeader()
    {
        return this.pageHeader;
    }

    /* write the page header to the specified output stream */
    public void writeHeader(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        String s = this.getPageHeader();
        if (s != null) {
            out.write(StringTools.replaceKeys(s,reqState));
        } else {
            this.getDefaultPageDecorations().writeHeader(out, reqState);
        }
    }

    // ------------------------------------------------------------------------

    /* set the page navigation html */
    public void setPageNavigation(String navigate)
    {
        this.pageNavigate = this.trimStringRecords(navigate);
    }

    /* return the page header html */
    public String getPageNavigation()
    {
        return this.pageNavigate;
    }

    /* write the page header to the specified output stream */
    public void writeNavigation(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        String s = this.getPageNavigation();
        if (s != null) {
            out.write(StringTools.replaceKeys(s,reqState));
        } else {
            this.getDefaultPageDecorations().writeNavigation(out, reqState);
        }
    }

    // ------------------------------------------------------------------------

    /* set the page footer html */
    public void setPageFooter(String footer)
    {
        this.pageFooter = this.trimStringRecords(footer);
    }

    /* return the page header html */
    public String getPageFooter()
    {
        return this.pageFooter;
    }

    /* write the page footer to the specified output stream */
    public void writeFooter(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        String s = this.getPageFooter();
        if (s != null) {
            out.write(StringTools.replaceKeys(s,reqState));
        } else {
            this.getDefaultPageDecorations().writeFooter(out, reqState);
        }
    }

    // ------------------------------------------------------------------------

    /* set the left banner html */
    public void setPageLeft(String left)
    {
        this.pageLeft = this.trimStringRecords(left);
    }

    /* return the page header html */
    public String getPageLeft()
    {
        return this.pageLeft;
    }

    /* write the left page banner to the specified output stream */
    public void writeLeft(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        String s = this.getPageLeft();
        if (s != null) {
            out.write(StringTools.replaceKeys(s,reqState));
        } else {
            this.getDefaultPageDecorations().writeLeft(out, reqState);
        }
    }

    // ------------------------------------------------------------------------

    /* set the right banner html */
    public void setPageRight(String right)
    {
        this.pageRight = this.trimStringRecords(right);
    }

    /* return the page header html */
    public String getPageRight()
    {
        return this.pageRight;
    }

    /* write the right page banner to the specified output stream */
    public void writeRight(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        String s = this.getPageRight();
        if (s != null) {
            out.write(StringTools.replaceKeys(s,reqState));
        } else {
            this.getDefaultPageDecorations().writeRight(out, reqState);
        }
    }

    // ------------------------------------------------------------------------

}
