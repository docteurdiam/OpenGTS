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
//  2008/02/21  Martin D. Flynn
//     -Added overridable method for providing displayable help
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.db.*;
import org.opengts.util.*;

import org.opengts.war.report.*;

public abstract class DataColumnTemplate
{

    // ------------------------------------------------------------------------

    private String                  keyName = "";
    private String                  dftArg = null;
    
    private String                  colTitle = "";

    public DataColumnTemplate(String key) 
    {
        this(key, null, null);
    }
    
    private DataColumnTemplate(String key, String arg, String title) 
    {
        this.keyName  = (key != null)? key.toLowerCase() : "";
        this.dftArg   = (arg != null)? arg : "";
        this.colTitle = (title != null)? title : "";
    }

    // ------------------------------------------------------------------------

    /* return column name */
    public String getKeyName() 
    {
        return this.keyName;
    }
    
    /* return default argument */
    // The default argument is currently ignored
    public String getDefaultArg()
    {
        return this.dftArg;
    }

    // ------------------------------------------------------------------------

    /* return displayable help for this data column */
    public String getHelp()
    {
        return "";
    }

    // ------------------------------------------------------------------------

    /* return value for this column from the specified row object */
    // 'rowNdx' is the index of the _displayed_ row (first row is '0')
    // 'rconst' is the ReportData object for this report
    // 'rowObj' is the row object (typically an instance of EventData)
    public abstract Object getColumnValue(int rowNdx, ReportData rptData, ReportColumn rptCol, Object rowObj);
    
    // ------------------------------------------------------------------------

    /* return column title */
    public String getTitle(ReportData rptData, ReportColumn rptCol) 
    {
        // may be overridden
        return this.colTitle;
    }

    // ------------------------------------------------------------------------

    public boolean equals(Object other)
    {
        if (other instanceof DataColumnTemplate) {
            DataColumnTemplate dc = (DataColumnTemplate)other;
            return this.getKeyName().equals(dc.getKeyName());
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

}
