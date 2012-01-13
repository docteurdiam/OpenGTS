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
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class HeaderRowTemplate
{

    // ------------------------------------------------------------------------

    public static final String STYLE_CLASS = "rptHdrRow";

    // ------------------------------------------------------------------------

    private ReportTable reportTable = null;

    public HeaderRowTemplate(ReportTable rptTable) 
    {
        super();
        this.reportTable = rptTable;
    }

    // ------------------------------------------------------------------------

    public void writeHTML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {

        ReportColumn rptCols[] = report.getReportColumns();
        if ((rptCols == null) || (rptCols.length == 0)) {
            throw new ReportException("No report columns defined");
        }

        out.print("<tr");
        out.print(" class=\"" + STYLE_CLASS + "\"");
        out.print(">\n");

        DataRowTemplate rdp = report.getDataRowTemplate();
        for (int i = 0; i < rptCols.length; i++) {
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {
                HeaderColumnTemplate hct = this.reportTable.getHeaderColumnTemplate(dct);
                hct.writeHTML(out, level+1, report, rptCols[i]);
            } else {
                //Print.logError("Column name not found: " + rptCols[i]);
                Print.logStackTrace("Column name not found: " + rptCols[i].getKey());
            }
        }

        out.print("</tr>\n");
    }

    // ------------------------------------------------------------------------

    public void writeXML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {
        boolean isSoapRequest = report.isSoapRequest();
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);

        ReportColumn rptCols[] = report.getReportColumns();
        if (ListTools.isEmpty(rptCols)) {
            throw new ReportException("No report columns defined");
        }

        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"HeaderRow",
            XMLTools.ATTR("class",STYLE_CLASS),
            false,true));

        DataRowTemplate rdp = report.getDataRowTemplate();
        for (int i = 0; i < rptCols.length; i++) {
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {
                HeaderColumnTemplate hct = this.reportTable.getHeaderColumnTemplate(dct);
                hct.writeXML(out, level+1, report, rptCols[i]);
            } else {
                //Print.logError("Column name not found: " + rptCols[i]);
                Print.logStackTrace("Column name not found: " + rptCols[i].getKey());
            }
        }
        
        out.print(PFX1);
        out.print(XMLTools.endTAG(isSoapRequest,"HeaderRow",true));

    }

    // ------------------------------------------------------------------------

    public void writeXLS(ReportSpreadsheet rptSS, int level, ReportData report)
        throws ReportException
    {

        ReportColumn rptCols[] = report.getReportColumns();
        if ((rptCols == null) || (rptCols.length == 0)) {
            throw new ReportException("No report columns defined");
        }

        DataRowTemplate rdp = report.getDataRowTemplate();
        for (int i = 0; i < rptCols.length; i++) {
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {
                HeaderColumnTemplate hct = this.reportTable.getHeaderColumnTemplate(dct);
                hct.writeXLS(rptSS, level+1, report, rptCols[i]);
            } else {
                //Print.logError("Column name not found: " + rptCols[i]);
                Print.logStackTrace("Column name not found: " + rptCols[i].getKey());
            }
        }
        rptSS.incrementRowIndex();

    }
    
    // ------------------------------------------------------------------------

}
