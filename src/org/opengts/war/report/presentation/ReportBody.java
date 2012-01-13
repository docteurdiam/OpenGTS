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
//  2007/06/30  Martin D. Flynn
//     -Added total row ('getTotalsDataIterator')
//  2007/12/13  Martin D. Flynn
//     -Added indication of partial data (ie. when 'limit' has been exceeded)
//  2007/01/10  Martin D. Flynn
//     -Fixed 'partial' indication when limit is '-1'
//  2009/05/01  Martin D. Flynn
//     -Removed "Totals" line from CSV generated output
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class ReportBody
{

    // ------------------------------------------------------------------------

    private ReportTable         reportTable = null;

    private BodyRowTemplate     bodyRow     = null;

    private int                 rcdCount    = 0;
    private boolean             isPartial   = false;

    // ------------------------------------------------------------------------

    public ReportBody(ReportTable rptTable) 
    {
        this(rptTable, null);
    }

    protected ReportBody(ReportTable rptTable, BodyRowTemplate br) 
    {
        this.reportTable = rptTable;
        this.bodyRow     = (br != null)? br : new BodyRowTemplate(this.reportTable);
    }

    // ------------------------------------------------------------------------

    public int getRecordCount()
    {
        return this.rcdCount;
    }

    public boolean isPartial()
    {
        return this.isPartial;
    }

    // ------------------------------------------------------------------------

    private boolean _overLimit(ReportData rd)
    {

        /* check report limit */
        long rptLimit = rd.getReportLimit();
        long rptCount = this.rcdCount;
        if ((rptLimit > 0L) && (rptCount >= rptLimit)) {
            Print.logInfo("Partial report data (RecordCount): " + rptCount + " >= " + rptLimit);
            return true;
        }
        //Print.logInfo("RecordCount: %d/%d", rptCount, rptLimit);

        /* check selection limit (applicable only for EventData queries) */
        long selLimit = rd.getSelectionLimit();
        if (selLimit > 10L) { // don't count small selection limit (other "1 >= 1" will cause a partial indication)
            long selCount = rd.getMaximumEventDataCount();
            if ((selLimit > 0L) && (selCount >= selLimit)) {
                Print.logInfo("Partial report data (maxSelectionCount): " + selCount + " >= " + selLimit);
                return true;
            }
            //Print.logInfo("SelectionCount: %d/%d", selCount, selLimit);
        }

        /* not over limit */
        return false;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writeHTML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {
        
        /* resulting state */
        this.isPartial = false;
        this.rcdCount = 0;

        /* HTML table body begin */
        out.print("<tbody>\n");

        /* report body */
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            for (this.rcdCount = 0; data.hasNext(); this.rcdCount++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    this.bodyRow.writeHTML(out, level+1, this.rcdCount, false/*totals*/, dr);
                }
            }
            this.isPartial = this._overLimit(report);
        }

        /* report totals */
        DBDataIterator totals = report.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            for (int r = 0; totals.hasNext(); r++) {
                DBDataRow dr = totals.next();
                if (dr != null) {
                    this.bodyRow.writeHTML(out, level+1, r, true/*totals*/, dr);
                }
            }
        }

        /* HTML table body end */
        out.print("</tbody>\n");
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writeXML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {
        boolean isSoapRequest = report.isSoapRequest();
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);

        /* resulting state */
        this.isPartial = false;
        this.rcdCount = 0;

        /* HTML table body begin */
        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"ReportBody","",false,true));

        /* report body */
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            for (this.rcdCount = 0; data.hasNext(); this.rcdCount++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    this.bodyRow.writeXML(out, level+1, this.rcdCount, false/*totals*/, dr);
                }
            }
            this.isPartial = this._overLimit(report);
        }

        /* report totals */
        DBDataIterator totals = report.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            for (int r = 0; totals.hasNext(); r++) {
                DBDataRow dr = totals.next();
                if (dr != null) {
                    this.bodyRow.writeXML(out, level+1, r, true/*totals*/, dr);
                }
            }
        }

        /* HTML table body end */
        out.print(PFX1);
        out.print(XMLTools.endTAG(isSoapRequest,"ReportBody",true));

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final boolean INCLUDE_CSV_TOTALS = false;

    public void writeCSV(PrintWriter out, int level, ReportData report) 
        throws ReportException
    {

        /* report body */
        this.isPartial = false;
        this.rcdCount = 0;
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            for (this.rcdCount = 0; data.hasNext(); this.rcdCount++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    this.bodyRow.writeCSV(out, level+1, this.rcdCount, false/*totals*/, dr);
                }
            }
            this.isPartial = this._overLimit(report);
        }

        /* report totals */
        if (INCLUDE_CSV_TOTALS) {
            DBDataIterator totals = report.getTotalsDataIterator();
            if (totals != null) {
                for (int r = 0; totals.hasNext(); r++) {
                    DBDataRow dr = totals.next();
                    if (dr != null) {
                        this.bodyRow.writeCSV(out, level+1, r, true/*totals*/, dr);
                    }
                }
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writeXLS(ReportSpreadsheet rptSS, int level, ReportData report) 
        throws ReportException
    {

        /* resulting state */
        this.isPartial = false;
        this.rcdCount = 0;

        /* report body */
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            for (this.rcdCount = 0; data.hasNext(); this.rcdCount++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    this.bodyRow.writeXLS(rptSS, level+1, this.rcdCount, dr);
                }
            }
            this.isPartial = this._overLimit(report);
        }

        /* report totals */
        DBDataIterator totals = report.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            for (int r = 0; totals.hasNext(); r++) {
                DBDataRow dr = totals.next();
                if (dr != null) {
                    this.bodyRow.writeXLS(rptSS, level+1, r, dr);
                }
            }
        }

    }

    /**
    *** Sends the first line of the report to the callback method
    *** @param out     The stream to which the output is written
    *** @param level   The recursion level
    *** @param rptData The Report attributes
    **/
    public void writeCallback(OutputProvider out, int level, ReportData rd) 
        throws ReportException
    {

        /* resulting state */
        this.isPartial = false;
        this.rcdCount = 0;

        /* get callback method */
        ReportCallback rptCB = rd.getReportCallback();
        if (rptCB == null) {
            return;
        }

        /* report body */
        DBDataIterator data = rd.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            this.rcdCount  = rptCB.reportBody(out, level+1, data);
            this.isPartial = this._overLimit(rd);
        }

        /* report totals */
        DBDataIterator totals = rd.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            rptCB.reportTotals(out, level+1, totals);
        }

    }

    // ------------------------------------------------------------------------

}
