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
// Last know location
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
// ----------------------------------------------------------------------------
package org.opengts.war.report.event;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class EventSummaryReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Summary report
    // 1 EventData record per device
    // As-of date ('To' date only)
    // ------------------------------------------------------------------------

    /**
    *** Event Summary Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public EventSummaryReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }
        //if ((acct == null) || (this.getDeviceCount() < 1)) {
        //    throw new ReportException("At least 1 Device must be specified");
        //}
        // report on all authorized devices
        ////this.getReportDeviceList().addAllAuthorizedDevices();
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {
        ReportConstraints rc = this.getReportConstraints();
        rc.setTimeStart(-1L); // disregard 'start' time
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report supports displaying a map
    *** @return True if this report supports displaying a map, false otherwise
    **/
    public boolean getSupportsMapDisplay()
    {
        return true;
    }

    /**
    *** Returns true if this report supports displaying KML
    *** @return True if this report supports displaying KML, false otherwise
    **/
    public boolean getSupportsKmlDisplay()
    {
        return true;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return EventDataLayout.getReportLayout();
    }

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {
        EventData ed[] = this.getEventData(null);
        Arrays.sort(ed, new EventData.DeviceDescriptionComparator()); // sort by device description
        return new ArrayDataIterator(ed); // 'EventDataLayout' expects EventData[]
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
    // EventData record retrieval overrides
    
    /**
    *** Returns the limit type contraint
    *** @return The limit type
    **/
    public EventData.LimitType getSelectionLimitType()
    {
        return EventData.LimitType.LAST;
    }
    
    /**
    *** Returns the selection limit
    *** @return The selection limit
    **/
    public long getSelectionLimit()
    {
        return 1L;
    }

    // ------------------------------------------------------------------------

}
