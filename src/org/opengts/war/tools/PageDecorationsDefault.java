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

public class PageDecorationsDefault
    implements PageDecorations
{

    // ------------------------------------------------------------------------

    private PrivateLabel    privateLabel        = null;

    private String          pageStyle           = null;
    private String          pageHeader          = null;
    private String          pageFooter          = null;

    // ------------------------------------------------------------------------

    public PageDecorationsDefault(PrivateLabel privLbl)
    {
        this.privateLabel = privLbl; // may be null
    }

    // ------------------------------------------------------------------------

    public void setDefaultPageDecorations(PageDecorations dftPageDecor)
    {
        // ignore
    }

    // ------------------------------------------------------------------------

    /* returns true if this PageDecorations instance is backed by a JSP page */
    public boolean hasJspURI()
    {
        return false;
    }
    
    /* sets the backing JSP page URI */
    public void setJspURI(String jspURI)
    {
        // ignore
    }

    /* returns backing JSP page URI, or null if there is no JSP page backing */
    public String getJspURI()
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /* set the page style html */
    public void setPageStyle(String style)
    {
        // ignore
    }

    /* return the page style html */
    public String getPageStyle()
    {
        if (this.pageStyle == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("<style type='text/css'>\n");
            sb.append("  a:hover { color:#00CC00; }\n");
            sb.append("  h1 { font-family:Arial; font-size:16pt; white-space:pre; }\n");
            sb.append("  h2 { font-family:Arial; font-size:14pt; white-space:pre; }\n");
            sb.append("  h3 { font-family:Arial; font-size:12pt; white-space:pre; }\n");
            sb.append("  h4 { font-family:Arial; font-size:10pt; white-space:pre; }\n");
            sb.append("  form { margin-top:0px; margin-bottom:0px; }\n");
            sb.append("  body { font-size:8pt; font-family:verdena,sans-serif; }\n");
            sb.append("  td { font-size:8pt; font-family:verdena,sans-serif; }\n");
            sb.append("  input { font-size:8pt; font-family:verdena,sans-serif; }\n");
            sb.append("  input:focus { background-color: #FFFFC9; }\n");
            sb.append("  select { font-size:7pt; font-family:verdena,sans-serif; }\n");
            sb.append("  select:focus { background-color: #FFFFC9; }\n");        
            sb.append("  textarea { font-size:8pt; font-family:verdena,sans-serif; }\n");
            sb.append("  textarea:focus { background-color: #FFFFC9; }\n");        
            sb.append("  ."+CommonServlet.CSS_TEXT_INPUT+" { border-width:2px; border-style:inset; border-color:#DDDDDD #EEEEEE #EEEEEE #DDDDDD; padding-left:2px; background-color:#FFFFFF; }\n");
            sb.append("  ."+CommonServlet.CSS_TEXT_READONLY+" { border-width:2px; border-style:inset; border-color:#DDDDDD #EEEEEE #EEEEEE #DDDDDD; padding-left:2px; background-color:#E7E7E7; }\n");
            sb.append("  ."+CommonServlet.CSS_CONTENT_FRAME[1]+" { padding:5px; width:300px; border-style:double; border-color:#555555; background-color:white; }\n");
            sb.append("  ."+CommonServlet.CSS_CONTENT_MESSAGE+" { padding-top:5px; font-style:oblique; text-align:center; }\n");
            sb.append("</style>\n");
            this.pageStyle = sb.toString();
        } 
        return this.pageStyle;
    }

    /* write the page header to the specified output stream */
    public void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        out.println(this.getPageStyle());
    }

    // ------------------------------------------------------------------------

    /* set the page header html */
    public void setPageHeader(String header)
    {
        // ignore
    }

    /* return the page header html */
    protected String getPageHeader()
    {
        if (this.pageHeader == null) {
            String title = (this.privateLabel != null)? this.privateLabel.getPageTitle() : "";
            StringBuffer sb = new StringBuffer();
            sb.append("<center>");
            sb.append("<span style='font-size:14pt;'><b>" + title + "</b></span>");
            sb.append("<hr>");
            sb.append("</center>");
            this.pageHeader = sb.toString();
        }
        return this.pageHeader;
    }

    /* write the page header to the specified output stream */
    public void writeHeader(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        out.write(this.getPageHeader());
    }

    // ------------------------------------------------------------------------

    /* set the page navigation html */
    public void setPageNavigation(String navigate)
    {
        // ignore
    }

    /* return the page header html */
    protected String getPageNavigation()
    {
        return "";
    }

    /* write the page header to the specified output stream */
    public void writeNavigation(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        // ignore
    }

    // ------------------------------------------------------------------------

    /* set the page footer html */
    public void setPageFooter(String footer)
    {
        // ignore
    }

    /* return the page footer html */
    protected String getPageFooter()
    {
        if (this.pageFooter == null) {
            String copyright = (this.privateLabel != null)? this.privateLabel.getCopyright() : "";
            StringBuffer sb = new StringBuffer();
            sb.append("<center>");
            sb.append("<hr><span style='font-size:7pt;'>" + copyright + "</span>");
            sb.append("</center>");
            this.pageFooter = sb.toString();
        }
        return this.pageFooter;
    }

    /* write the page footer to the specified output stream */
    public void writeFooter(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        out.write(this.getPageFooter());
    }

    // ------------------------------------------------------------------------

    /* set the left banner html */
    public void setPageLeft(String left)
    {
        // ignore
    }

    /* return the left banner html */
    protected String getPageLeft()
    {
        return "";
    }

    /* write the left page banner to the specified output stream */
    public void writeLeft(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        // ignore
    }

    // ------------------------------------------------------------------------

    /* set the right banner html */
    public void setPageRight(String right)
    {
        // ignore
    }

    /* return the page header html */
    public String getPageRight()
    {
        return "";
    }

    /* write the right page banner to the specified output stream */
    public void writeRight(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        // ignore
    }

    // ------------------------------------------------------------------------

}
