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
//  2007/11/28  Martin D. Flynn
//     -Integrated use of 'ReportColumn'
//  2009/09/23  CLifton Flynn, Martin D. Flynn
//     -Added SOAP xml support
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class HeaderColumnTemplate
{

    // ------------------------------------------------------------------------

    public static final String  STYLE_CLASS_SORT    = "rptHdrCol_sort";
    public static final String  STYLE_CLASS_NOSORT  = "rptHdrCol_nosort";

    // ------------------------------------------------------------------------
    
    private DataColumnTemplate  dataColumn      = null;
    //private String     fldName      = "";
    //private String     title        = null;
        
    private int                 rowSpan         = 1;
    private int                 colSpan         = 1;

    // ------------------------------------------------------------------------
    
    public HeaderColumnTemplate(DataColumnTemplate dtaCol)
    {
        this(dtaCol, 1, 1);
    }

    public HeaderColumnTemplate(DataColumnTemplate dtaCol, int rowSpan, int colSpan)
    {
        this.dataColumn = dtaCol;
        //this.setFieldName(dtaCol.getKeyName());
        this.setRowSpan(rowSpan);
        this.setColSpan(colSpan);
    }

    // ------------------------------------------------------------------------

    //public HeaderColumnTemplate setFieldName(String fldName)
    //{
    //    this.fldName = fldName;
    //    return this;
    //}
    
    public String getFieldName()
    {
        return this.dataColumn.getKeyName();
    }
    
    // ------------------------------------------------------------------------

    //public HeaderColumnTemplate setTitle(String title) 
    //{
    //    this.title = (title != null)? title : "";
    //    return this;
    //}

    //public String getTitle(ReportData rd) 
    //{
    //    //if (this.dataColumn != null) {
    //        return this.dataColumn.getTitle(rd);
    //    //} else {
    //    //    return (this.title != null)? this.title : "";
    //    //}
    //}

    public String getTitle(ReportData report, ReportColumn column)
    {
        Locale locale    = report.getLocale();
        String dftTitle  = this.dataColumn.getTitle(report,column);
        String rptTitle  = (column != null)? column.getTitle(locale,dftTitle) : dftTitle;
        return report.expandHeaderText(rptTitle);
    }
 
    // ------------------------------------------------------------------------

    public HeaderColumnTemplate setRowSpan(int rowSpan)
    {
        this.rowSpan = rowSpan;
        return this;
    }
    
    public int getRowSpan()
    {
        return this.rowSpan;
    }
    
    public boolean hasRowSpan()
    {
        return (this.rowSpan > 1);
    }

    // ------------------------------------------------------------------------

    public HeaderColumnTemplate setColSpan(int colSpan)
    {
        this.colSpan = colSpan;
        return this;
    }
    
    public int getColSpan()
    {
        return this.colSpan;
    }

    public boolean hasColSpan()
    {
        return (this.colSpan > 1);
    }

    // ------------------------------------------------------------------------

    public void writeHTML(PrintWriter out, int level, ReportData report, ReportColumn column) 
    {
        String pfx1 = StringTools.replicateString(" ", level * ReportTable.INDENT);
        boolean colSortable = (column != null) && column.isSortable();

        out.print(pfx1);
        out.print("<th");
        out.print(" id=\"" + this.getFieldName() + "\"");
        out.print(" class=\"" + (colSortable?STYLE_CLASS_SORT:STYLE_CLASS_NOSORT) + "\"");
        out.print(" nowrap");
        if (this.hasRowSpan()) { out.print(" rowSpan=\"" + this.getRowSpan() + "\""); }
        if (this.hasColSpan()) { out.print(" colSpan=\"" + this.getColSpan() + "\""); }
        out.print(">");
        
        String htmlTitle = ReportTable.FilterText(this.getTitle(report, column));
        out.print(htmlTitle);
        
        out.print("</th>\n");

    }

    // ------------------------------------------------------------------------

    public void writeXML(PrintWriter out, int level, ReportData report, ReportColumn column) 
    {
        boolean isSoapRequest = report.isSoapRequest();
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);
        boolean colSortable = (column != null) && column.isSortable();

        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"HeaderColumn",
            XMLTools.ATTR("id",this.getFieldName()) +
            XMLTools.ATTR("class",colSortable?STYLE_CLASS_SORT:STYLE_CLASS_NOSORT) +
            (this.hasRowSpan()?XMLTools.ATTR("rowspan",this.getRowSpan()):"") +
            (this.hasColSpan()?XMLTools.ATTR("colspan",this.getColSpan()):""),
            false,false));
        out.print(ReportTable.XmlFilter(isSoapRequest,this.getTitle(report,column)));
        out.print(XMLTools.endTAG(isSoapRequest,"HeaderColumn",true));

    }

    // ------------------------------------------------------------------------

    public void writeXLS(ReportSpreadsheet rptSS, int level, ReportData report, ReportColumn column) 
    {
        String colTitle = this.getTitle(report, column);
        rptSS.addHeaderColumn(colTitle, 20/*charWidth*/);
    }

    // ------------------------------------------------------------------------

}
