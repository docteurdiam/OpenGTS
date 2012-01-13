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
//
// report definition:
//   ReportHeader rh = new ReportHeader(
//       new HeaderRowTemplate("#DDDDDD", new HeaderColumnTemplate[] {
//           new HeaderColumnTemplate("Col A",40, 2, 1),
//           new HeaderColumnTemplate("Temperature",200,1,3),
//           new HeaderColumnTemplate("Col Z",40, 2, 1),
//       }),
//       new HeaderRowTemplate("#DDDDDD", new HeaderColumnTemplate[] {
//           new HeaderColumnTemplate("A"),
//           new HeaderColumnTemplate("B").setAlignLeft(),
//           new HeaderColumnTemplate("C"),
//       }));
//
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2009/09/23  Clifton Flynn, Martin D. Flynn
//     -Added SOAP xml support
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class ReportHeader
{

    // ------------------------------------------------------------------------

    private static final boolean USE_CSV_COLUMN_HEADER_DESCRIPTIONS      = false;
    private static final String  PROP_csvColumnHeaderDescriptions        = "csvColumnHeaderDescriptions";

    private static final boolean USE_XLS_COLUMN_HEADER_DESCRIPTIONS      = true;
    private static final String  PROP_xlsColumnHeaderDescriptions        = "xlsColumnHeaderDescriptions";

    private static final boolean USE_CALLBACK_COLUMN_HEADER_DESCRIPTIONS = true;
    private static final String  PROP_callbackColumnHeaderDescriptions   = "callbackColumnHeaderDescriptions";

    // ------------------------------------------------------------------------

    private ReportTable         reportTable = null;
    private HeaderRowTemplate   headerRow[] = null;

    // ------------------------------------------------------------------------

    public ReportHeader(ReportTable rptTable) 
    {
        this(rptTable, new HeaderRowTemplate(rptTable));
    }

    protected ReportHeader(ReportTable rptTable, HeaderRowTemplate... hr) 
    {
        this.reportTable = rptTable;
        this.headerRow   = (hr != null)? hr : new HeaderRowTemplate[0];
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writeHTML(PrintWriter out, int level, ReportData rptData) 
        throws ReportException
    {

        out.print("<thead>\n");
        for (int i = 0; i < this.headerRow.length; i++) {
            this.headerRow[i].writeHTML(out, level+1, rptData);
        }
        out.print("</thead>\n");

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Writes the header portion of the XML file containing the column descriptions
    *** @param out     The stream to which the output is written
    *** @param level   The recursion level
    *** @param rptData The Report attributes
    **/
    public void writeXML(PrintWriter out, int level, ReportData rptData) 
        throws ReportException
    {
        boolean isSoapRequest = rptData.isSoapRequest();
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);

        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"ReportHeader","",false,true));
        for (int i = 0; i < this.headerRow.length; i++) {
            this.headerRow[i].writeXML(out, level+1, rptData);
        }
        out.print(PFX1);
        out.print(XMLTools.endTAG(isSoapRequest,"ReportHeader",true));

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Writes the first line of the CSV file containing the column descriptions
    *** @param out     The stream to which the output is written
    *** @param level   The recursion level
    *** @param rptData The Report attributes
    **/
    public void writeCSV(PrintWriter out, int level, ReportData rptData) 
        throws ReportException
    {

        ReportColumn rptCols[] = rptData.getReportColumns();
        if (ListTools.isEmpty(rptCols)) {
            throw new ReportException("No report columns defined");
        }

        boolean useColDesc = rptData.getProperties().getBoolean(PROP_csvColumnHeaderDescriptions,USE_CSV_COLUMN_HEADER_DESCRIPTIONS);
        DataRowTemplate rdp = rptData.getDataRowTemplate();
        for (int i = 0; i < rptCols.length; i++) {
            
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {

                /* next field */
                if (i > 0) {
                    out.print(","); // CSV_SEPARATOR
                }

                /* column title */
                BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                String colTitle = null;
                if (useColDesc) {
                    HeaderColumnTemplate hct = this.reportTable.getHeaderColumnTemplate(dct);
                    colTitle = StringTools.replace(hct.getTitle(rptData, rptCols[i]),"\n"," ");
                } else {
                    colTitle = bct.getFieldName();
                }

                /* display column header title */
                bct.writeCSV(out, level+1, colTitle);

            } else {

                // TODO:?

            }
            
        }
        out.print("\n");

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Writes the headert rows of the XLS file containing the column descriptions
    *** @param rptSS   The spreadsheet instance to which the header is written
    *** @param level   The recursion level
    *** @param rptData The Report attributes
    **/
    public void writeXLS(ReportSpreadsheet rptSS, int level, ReportData rptData) 
        throws ReportException
    {
        for (int i = 0; i < this.headerRow.length; i++) {
            this.headerRow[i].writeXLS(rptSS, level+1, rptData);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sends the first line of the report to the callback method
    *** @param out     The stream to which the output is written
    *** @param level   The recursion level
    *** @param rptData The Report attributes
    **/
    public void writeCallback(OutputProvider out, int level, ReportData rd) 
        throws ReportException
    {

        /* get callback method */
        ReportCallback rptCB = rd.getReportCallback();
        if (rptCB == null) {
            return;
        }

        /* column descriptions vs. names */
        boolean useColDesc = rd.getProperties().getBoolean(PROP_callbackColumnHeaderDescriptions,USE_CALLBACK_COLUMN_HEADER_DESCRIPTIONS);

        /* report columns */
        ReportColumn rptCols[] = rd.getReportColumns();
        if (ListTools.isEmpty(rptCols)) {
            throw new ReportException("No report columns defined");
        }

        /* assemble HeaderColumnTemplate array */
        Object colHeader[] = new Object[rptCols.length]; // either "String" or "HeaderColumnTemplate"
        DataRowTemplate rdp = rd.getDataRowTemplate();
        for (int i = 0; i < rptCols.length; i++) {
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {
                BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                if (useColDesc) {
                    colHeader[i] = this.reportTable.getHeaderColumnTemplate(dct);
                } else {
                    colHeader[i] = bct.getFieldName();
                }
            } else {
                colHeader[i] = colName;
            }
        }

        /* call callback */
        rptCB.reportHeader(out, level, colHeader);

    }

    // ------------------------------------------------------------------------

}
