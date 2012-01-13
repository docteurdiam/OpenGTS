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
//  2007/01/10  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report.field;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class EventCountReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Summary report
    // 1 'count' record per device
    // ------------------------------------------------------------------------

    private java.util.List<FieldData>   rowData = null;

    // ------------------------------------------------------------------------

    /**
    *** Event Count Report Constructor
    *** @param rptEntry The ReportEntry
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public EventCountReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }
        //if (this.getDeviceCount() < 1) {
        //    throw new ReportException("At least 1 Device must be specified");
        //}
        // report on all authorized devices
        //this.getReportDeviceList().addAllAuthorizedDevices();
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {
        //ReportConstraints rc = this.getReportConstraints();
        //Print.logInfo("LimitType=" + rc.getSelectionLimitType() + ", Limit=" + rc.getSelectionLimit());
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return FieldLayout.getReportLayout();
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {
        
        /* init */
        this.rowData = new Vector<FieldData>();
        
        /* loop through devices */
        String devID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {
                Device device  = devList.getDevice(devID);
                if (device != null) {
                    long rcdCount = this.countEventData(device);
                    FieldData fd = new FieldData();
                    fd.setDevice(device);
                    fd.setString(FieldLayout.DATA_DEVICE_ID, devID);
                    fd.setLong(  FieldLayout.DATA_COUNT    , rcdCount);
                    this.rowData.add(fd);
                } else {
                    // should never occur
                    Print.logError("Returned DeviceList 'Device' is null: " + devID);
                }
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData count for Device: " + devID);
            }
        }

        /* return data iterator */
        FieldData.sortByDeviceDescription(this.rowData);
        return new ListDataIterator(this.rowData);
        
    }

    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        return null;
    }

    // ------------------------------------------------------------------------

}
