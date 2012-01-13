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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;

public abstract class DBDataRowAdapter
    implements DBDataRow, CSSRowClass
{

    // ------------------------------------------------------------------------
    
    private ReportData  reportData = null;

    public DBDataRowAdapter()
    {
        //
    }

    public DBDataRowAdapter(ReportData rd)
    {
        this.reportData = rd;
    }

    // ------------------------------------------------------------------------

    public boolean hasCssClass()
    {
        return !StringTools.isBlank(this.getCssClass());
    }

    public String getCssClass()
    {
        Object rowObj = this.getRowObject();
        if ((rowObj instanceof CSSRowClass) && ((CSSRowClass)rowObj).hasCssClass()) {
            return ((CSSRowClass)rowObj).getCssClass();
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    public ReportData getReportData()
    {
        return this.reportData;
    }
    
    public ReportColumn[] getReportColumns()
    {
        ReportData rd = this.getReportData();
        return (rd != null)? rd.getReportColumns() : null;
    }
    
    public DataRowTemplate getDataRowTemplate()
    {
        ReportData rd = this.getReportData();
        return (rd != null)? rd.getDataRowTemplate() : null;
    }

    // ------------------------------------------------------------------------

    public abstract Object getRowObject();
    
    public abstract Object getDBValue(String fldName, int rowNdx, ReportColumn rptCol);

    // ------------------------------------------------------------------------

    public RowType getRowType()
    {
        DataRowTemplate drt = this.getDataRowTemplate();
        if (drt != null) {
            return drt.getRowType(this.getRowObject());
        } else {
            return RowType.DETAIL;
        }
    }
    
    // ------------------------------------------------------------------------

}
