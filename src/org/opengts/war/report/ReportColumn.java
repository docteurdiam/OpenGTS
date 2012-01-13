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
//  2007/11/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.report.*;

public class ReportColumn
{
    
    // ------------------------------------------------------------------------

    private String    colKey            = "";
    private String    colArg            = "";
    private I18N.Text colTitle          = null;
    private int       colSpan           = 1;
    
    private boolean   sortable          = true;
    
    private String    blankFiller       = "";

    public ReportColumn(String key, String arg, I18N.Text title)
    {
        this.colKey   = (key != null)? key : "";
        this.colArg   = (arg != null)? arg : "";
        this.colTitle = ((title != null) && !title.toString().equals(""))? title : null;
    }
    
    public ReportColumn(String key, String arg, int colSpan, I18N.Text title)
    {
        this(key, arg, title);
        this.colSpan = colSpan;
    }

    // ------------------------------------------------------------------------

    /* return column name(key) */
    public String getName()
    {
        return this.colKey;
    }

    /* return column key */
    public String getKey()
    {
        return this.colKey;
    }
    
    /* return column argument(s) */
    public String getArg()
    {
        return this.colArg; // should never be null
    }
    
    /* return column span */
    public int getColSpan()
    {
        return this.colSpan;
    }
    
    // ------------------------------------------------------------------------

    /* return column title */
    public String getTitle(Locale loc, String dft)
    {
        return (this.colTitle != null)? this.colTitle.toString(loc) : dft;
    }

    // ------------------------------------------------------------------------

    public void setSortable(boolean sortable)
    {
        this.sortable = sortable;
    }
    
    public boolean isSortable()
    {
        return this.sortable;
    }
    
    // ------------------------------------------------------------------------

    public void setBlankFiller(String filler)
    {
        this.blankFiller = (filler != null)? filler : "";
    }

    public String getBlankFiller()
    {
        return this.blankFiller;
    }
    
    // ------------------------------------------------------------------------

    /* debug: convert to string */
    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getKey()).append(":").append(this.getArg());
        String title = this.getTitle(null,"");
        if (title != null) {
            sb.append(" - ").append(title);
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

}
