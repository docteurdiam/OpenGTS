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
//  2011/07/15  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

import org.opengts.war.report.presentation.HeaderColumnTemplate;

public class ReportCallback
{

    // ------------------------------------------------------------------------

    private ReportData               report         = null;
    private ReportColumn             rptCols[]      = null;

    private String                   rptTitle       = null;
    private String                   rptSubtitle    = null;

    private String                   headerDesc[]   = null;

    private java.util.List<Object[]> bodyRows       = null;
    private java.util.List<Object[]> totalRows      = null;

    public ReportCallback() 
    {
        super();
    }

    public ReportCallback(ReportData report) 
        throws ReportException
    {
        this();
        this.setReport(report);
    }

    // ------------------------------------------------------------------------

    public void setReport(ReportData report)
        throws ReportException
    {

        /* set report */
        this.report = report;
        if (this.report == null) {
            throw new ReportException("Report is null");
        }

        /* cache report columns */
        this.rptCols = null; // reset
        this.getReportColumns();

    }
    
    public ReportColumn[] getReportColumns()
        throws ReportException
    {
        
        /* already have columns */
        if (this.rptCols != null) {
            return this.rptCols;
        }

        /* report */
        if (this.report == null) {
            throw new ReportException("Report is null");
        }

        /* get report columns */
        this.rptCols = this.report.getReportColumns();
        if (ListTools.isEmpty(this.rptCols)) {
            throw new ReportException("No report columns defined");
        }
        return this.rptCols;

    }

    // ------------------------------------------------------------------------

    public void reportStart(OutputProvider out, int level) 
        throws ReportException
    {

        /* report */
        if (this.report == null) {
            throw new ReportException("Report is null");
        }

        /* get titles */
        this.rptTitle    = this.report.getReportTitle();
        this.rptSubtitle = this.report.getReportSubtitle();

    }
    
    public String getReportTitle()
    {
        return this.rptTitle;
    }
    
    public String getReportSubtitle()
    {
        return this.rptSubtitle;
    }

    // ------------------------------------------------------------------------

    public void reportHeader(OutputProvider out, int level, Object headerCol[]) 
        throws ReportException
    {

        /* report */
        if (this.report == null) {
            throw new ReportException("Report is null");
        }
        
        /* invalid header columns? */
        if (ListTools.size(headerCol) != this.rptCols.length) {
            throw new ReportException("Invalid number of header columns");
        }

        /* populate column header descriptions */
        this.headerDesc = new String[this.rptCols.length];
        for (int i = 0; i < this.rptCols.length; i++) {
            if (headerCol[i] instanceof String) {
                this.headerDesc[i] = (String)headerCol[i];
            } else
            if (headerCol[i] instanceof HeaderColumnTemplate) {
                HeaderColumnTemplate hct = (HeaderColumnTemplate)headerCol[i];
                this.headerDesc[i] = hct.getTitle(this.report, this.rptCols[i]);
            } else {
                Print.logWarn("Unrecognized header column class: " + StringTools.className(headerCol[i]));
                this.headerDesc[i] = "#" + i;
            }
        }
        
    }
    
    public boolean hasHeaderColumnDescriptions()
    {
        return !ListTools.isEmpty(this.headerDesc);
    }
    
    public String[] getHeaderColumnDescriptions()
    {
        return this.headerDesc;
    }

    // ------------------------------------------------------------------------

    public int reportBody(OutputProvider out, int level, DBDataIterator data) 
        throws ReportException
    {

        /* report */
        if (this.report == null) {
            throw new ReportException("Report is null");
        }
        
        /* populate report body */
        this.bodyRows = new Vector<Object[]>();
        for (;data.hasNext();) {
            DBDataRow dr = data.next();
            if (dr == null) {
                continue;
            }
            DataRowTemplate drt = dr.getDataRowTemplate();

            /* populate row data */
            Object rowVal[] = new Object[this.rptCols.length];
            for (int i = 0; i < this.rptCols.length; i++) {
    
                /* extract column name/arg */
                String colName = this.rptCols[i].getKey();
                int    colSpan = this.rptCols[i].getColSpan(); // assume "1"
    
                /* get field value */
                DataColumnTemplate dct = drt.getColumnTemplate(colName);
                if (dct != null) {
                    //BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                    String fldName = colName; // bct.getFieldName(); // same as column name
                    Object fldVal  = dr.getDBValue(fldName, this.bodyRows.size(), this.rptCols[i]);
                    rowVal[i] = fldVal;
                } else {
                    //Print.logError("DataColumnTemplate not found: " + rptCols[i]);
                    rowVal[i] = "";
                }
    
            }
            
            /* save row */
            this.bodyRows.add(rowVal);

        }
        
        /* return number of rows saved */
        return this.bodyRows.size();
        
    }
    
    public boolean hasReportBodyRows()
    {
        return !ListTools.isEmpty(this.bodyRows);
    }

    public java.util.List<Object[]> getReportBodyRows()
    {
        return this.bodyRows;
    }

    // ------------------------------------------------------------------------

    public int reportTotals(OutputProvider out, int level, DBDataIterator data) 
        throws ReportException
    {

        /* report */
        if (this.report == null) {
            throw new ReportException("Report is null");
        }
        
        /* populate report total */
        this.totalRows = new Vector<Object[]>();
        for (;data.hasNext();) {
            DBDataRow dr = data.next();
            if (dr == null) {
                continue;
            }
            DataRowTemplate drt = dr.getDataRowTemplate();

            /* populate row data */
            Object rowVal[] = new Object[this.rptCols.length];
            for (int i = 0; i < this.rptCols.length; i++) {
    
                /* extract column name/arg */
                String colName = this.rptCols[i].getKey();
                int    colSpan = this.rptCols[i].getColSpan(); // assume "1"
    
                /* get field value */
                DataColumnTemplate dct = drt.getColumnTemplate(colName);
                if (dct != null) {
                    //BodyColumnTemplate bct = this.reportTable.getBodyColumnTemplate(dct);
                    String fldName = colName; // bct.getFieldName(); // same as column name
                    Object fldVal  = dr.getDBValue(fldName, this.totalRows.size(), this.rptCols[i]);
                    rowVal[i] = fldVal;
                } else {
                    //Print.logError("DataColumnTemplate not found: " + rptCols[i]);
                    rowVal[i] = "";
                }
    
            }
            
            /* save row */
            this.totalRows.add(rowVal);

        }
        
        /* return number of rows saved */
        return this.totalRows.size();
        
    }
    
    public boolean hasReportTotalRows()
    {
        return !ListTools.isEmpty(this.totalRows);
    }

    public java.util.List<Object[]> getReportTotalRows()
    {
        return this.totalRows;
    }

    // ------------------------------------------------------------------------

    public void reportEnd(OutputProvider out, int level) 
        throws ReportException
    {
        //
    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Report Title   : " + this.getReportTitle() + "\n");
        sb.append("Report Subtitle: " + this.getReportSubtitle() + "\n");
        sb.append("Report Headers : ");
        String hdr[] = this.getHeaderColumnDescriptions();
        if (ListTools.isEmpty(hdr)) {
            sb.append("null\n");
        } else {
            for (int i = 0; i < hdr.length; i++) {
                if (i > 0) { sb.append(","); }
                sb.append(StringTools.replace(StringTools.trim(hdr[i]),"\n","\\n"));
            }
            sb.append("\n");
        }
        java.util.List<Object[]> bodyRows = this.getReportBodyRows();
        if (!ListTools.isEmpty(bodyRows)) {
            sb.append("Report Body    : \n");
            for (Object rowData[] : bodyRows) {
                for (int i = 0; i < rowData.length; i++) {
                    if (i > 0) { sb.append(","); }
                    sb.append(StringTools.replace(StringTools.trim(rowData[i]),"\n","\\n"));
                }
                sb.append("\n");
            }
        }
        Collection<Object[]> totalRows = this.getReportTotalRows();
        if (!ListTools.isEmpty(totalRows)) {
            sb.append("Report Totals  : \n");
            for (Object rowData[] : totalRows) {
                for (int i = 0; i < rowData.length; i++) {
                    if (i > 0) { sb.append(","); }
                    sb.append(StringTools.replace(StringTools.trim(rowData[i]),"\n","\\n"));
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
}
