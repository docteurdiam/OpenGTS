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
//  2007/01/25  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.JspWriter;

import org.opengts.util.*;

public class HTMLOutput
{
    
    // ------------------------------------------------------------------------

    /* NO-OP html instance */
    public static final HTMLOutput NOOP = new HTMLOutput();
    
    // ------------------------------------------------------------------------

    private String      tableClass      = null;
    private String      cellClass       = null;
    private String      tableMessage    = null;

    /**
    *** Constructor
    **/
    public HTMLOutput() 
    {
        // override
        // tableClass, cellClass, tableMessage will be null
    }

    /**
    *** Constructor
    *** @param cssClassNames  The default table/cell css class names
    *** @param tableMessage   The table message to display
    **/
    public HTMLOutput(String cssClassNames[], String tableMessage) 
    {
        this.tableClass   = ((cssClassNames != null) && (cssClassNames.length >= 1))? cssClassNames[0] : "defaultTableClass"; // not null
        this.cellClass    = ((cssClassNames != null) && (cssClassNames.length >= 2))? cssClassNames[1] : "defaultCellClass";  // not null
        this.tableMessage = tableMessage; // may be null
    }

    // ------------------------------------------------------------------------
    // methods used for "Content" display
    
    /**
    *** Returns the CSS class for the table element
    *** @return The CSS class for the table element
    **/
    public String getTableClass()
    {
        return this.tableClass;
    }
    
    /**
    *** Returns the CSS class for the table cell element (TD)
    *** @return The CSS class for the table cell element (TD)
    **/
    public String getCellClass()
    {
        return this.cellClass;
    }

    /**
    *** Returns the table message
    *** @return The table message
    **/
    public String getTableMessage()
    {
        return this.tableMessage;
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes data to the specified PrintWriter.  This method is intended to be overridden.
    *** The default implementation does nothing.
    *** @param out  The PrintWriter
    **/
    public void write(PrintWriter out) 
        throws IOException
    {
        // override
    }

    /**
    *** Writes data to the specified JspWriter.  This method may be overridden to provide 
    *** Custom behavior.  The default implementation calls "write(PrintWriter)".
    *** @param out  The JspWriter
    **/
    public void write(JspWriter out) 
        throws IOException 
    {
        // override
        this.write(new PrintWriter(out, out.isAutoFlush()));
    }

    // ------------------------------------------------------------------------

}
