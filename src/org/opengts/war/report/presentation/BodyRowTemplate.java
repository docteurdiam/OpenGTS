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
//  2009/09/23  Clifton Flynn, Martin D. Flynn
//     -Added SOAP xml support
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class BodyRowTemplate
{

    // ------------------------------------------------------------------------

    private ReportTable reportTable = null;

    public BodyRowTemplate(ReportTable rptTable)
    {
        super();
        this.reportTable = rptTable;
    }

    // ------------------------------------------------------------------------

    public void writeHTML(PrintWriter out, int level, int rowIndex, boolean isTotal, DBDataRow dr) 
        throws ReportException
    {
        // rowIndex starts at '0'.  < 0 indicates totals rowdata

        /* CSS class reference */
        String tdCssClass = null;
        String trCssClass;
        if (dr.hasCssClass()) {
            // custom CSS for TR and TD
            trCssClass = dr.getCssClass();
            tdCssClass = dr.getCssClass();
        } else
        if (isTotal) {
            trCssClass = (rowIndex <= 0)? ReportLayout.CSS_CLASS_TOTAL : ReportLayout.CSS_CLASS_TOTAL_2;
        } else
        if ((rowIndex & 1) == 0) {
            trCssClass = ReportLayout.CSS_CLASS_ODD;
        } else {
            trCssClass = ReportLayout.CSS_CLASS_EVEN;
        }

        /* data row */
        ReportColumn rptCols[] = dr.getReportColumns();
        if (ListTools.isEmpty(rptCols)) {
            throw new ReportException("No report columns defined");
        }

        /* table row start */
        out.print("<tr class=\"" + trCssClass + "\">\n");

        /* rows */
        DataRowTemplate drt = dr.getDataRowTemplate();
        for (int i = 0; i < rptCols.length; i++) {

            /* extract column name/arg */
            String colName = rptCols[i].getKey();
            int    colSpan = rptCols[i].getColSpan();

            /* get field value */
            DataColumnTemplate dct = drt.getColumnTemplate(colName);
            if (dct != null) {
                BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                String fldName = bct.getFieldName(); // same as column name
                Object fldVal  = dr.getDBValue(fldName, rowIndex, rptCols[i]);
                bct.writeHTML(out, level+1, rowIndex, isTotal, tdCssClass, colSpan, fldVal);
            } else {
                //Print.logError("BodyColumnTemplate not found: " + rptCols[i]);
            }

        }

        /* table row end */
        out.print("</tr>\n");
    }

    // ------------------------------------------------------------------------

    public void writeXML(PrintWriter out, int level, int rowIndex, boolean isTotal, DBDataRow dr) 
        throws ReportException
    {
        ReportData rd = dr.getReportData();
        boolean isSoapRequest = (rd != null)? rd.isSoapRequest() : false;
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);
        // rowIndex starts at '0'

        /* CSS class reference */
        String tdCssClass = null;
        String trCssClass;
        if (dr.hasCssClass()) {
            // custom CSS for TR and TD
            trCssClass = dr.getCssClass();
            tdCssClass = dr.getCssClass();
        } else
        if (isTotal) {
            trCssClass = (rowIndex <= 0)? ReportLayout.CSS_CLASS_TOTAL : ReportLayout.CSS_CLASS_TOTAL_2;
        } else
        if ((rowIndex & 1) == 0) {
            trCssClass = ReportLayout.CSS_CLASS_ODD;
        } else {
            trCssClass = ReportLayout.CSS_CLASS_EVEN;
        }

        /* data row */
        ReportColumn rptCols[] = dr.getReportColumns();
        if (ListTools.isEmpty(rptCols)) {
            throw new ReportException("No report columns defined");
        }

        /* table row start */
        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"BodyRow",
            XMLTools.ATTR("class",trCssClass),
            false,true));

        /* rows */
        DataRowTemplate drt = dr.getDataRowTemplate();
        for (int i = 0; i < rptCols.length; i++) {

            /* extract column name/arg */
            String colName = rptCols[i].getKey();
            int    colSpan = rptCols[i].getColSpan();

            /* get field value */
            DataColumnTemplate dct = drt.getColumnTemplate(colName);
            if (dct != null) {
                BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                String fldName = bct.getFieldName(); // same as column name
                Object fldVal  = dr.getDBValue(fldName, rowIndex, rptCols[i]);
                bct.writeXML(out, level+1, rowIndex, isTotal, tdCssClass, colSpan, fldVal, isSoapRequest);
            } else {
                //Print.logError("BodyColumnTemplate not found: " + rptCols[i]);
            }

        }

        /* table row end */
        out.print(PFX1);
        out.print(XMLTools.endTAG(isSoapRequest,"BodyRow",true));

    }

    // ------------------------------------------------------------------------

    public void writeCSV(PrintWriter out, int level, int rowIndex, boolean isTotal, DBDataRow dr) 
        throws ReportException
    {
        DataRowTemplate drt = dr.getDataRowTemplate();
        ReportColumn rptCols[] = dr.getReportColumns();
        for (int i = 0; i < rptCols.length; i++) {

            /* extract column name/arg */
            String colName = rptCols[i].getKey();

            /* get field value */
            DataColumnTemplate dct = drt.getColumnTemplate(colName);
            if (dct != null) {
                if (i > 0) {
                    out.print(","); // CSV_SEPARATOR
                }
                BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                String fldName = bct.getFieldName();
                Object fldVal  = dr.getDBValue(fldName, rowIndex, rptCols[i]);
                String valStr  = (fldVal != null)? fldVal.toString() : "";
                bct.writeCSV(out, level+1, valStr);
            }

        }
        out.print("\n");
    }

    // ------------------------------------------------------------------------

    public void writeXLS(ReportSpreadsheet rptRSS, int level, int rowIndex, DBDataRow dr) 
        throws ReportException
    {

        /* report columns */
        ReportColumn rptCols[] = dr.getReportColumns();
        if (ListTools.isEmpty(rptCols)) {
            throw new ReportException("No report columns defined");
        }

        /* write columns */
        DBDataRow.RowType rowType = dr.getRowType();
        DataRowTemplate drt = dr.getDataRowTemplate(); // ie. "FieldLayout.FieldDataRow"
        for (int i = 0; i < rptCols.length; i++) {

            /* extract column name/arg */
            String colName = rptCols[i].getKey();
            int    colSpan = rptCols[i].getColSpan();

            /* get field value */
            DataColumnTemplate dct = drt.getColumnTemplate(colName);
            if (dct != null) {
                BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                String fldName = bct.getFieldName(); // same as column name
                Object fldVal  = dr.getDBValue(fldName, rowIndex, rptCols[i]);
                bct.writeXLS(rptRSS, level+1, rowType, fldVal);
            } else {
                //Print.logError("BodyColumnTemplate not found: " + rptCols[i]);
            }

        }
        
        /* done with this row */
        rptRSS.incrementRowIndex();

    }

    // ------------------------------------------------------------------------

}
