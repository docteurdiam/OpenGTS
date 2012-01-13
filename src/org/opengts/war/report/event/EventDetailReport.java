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
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
//  2007/06/14  Martin D. Flynn
//     -Display appropriate error text when no devices have been specified for 
//      this report.
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

public class EventDetailReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Detail report
    // Multiple EventData records per device
    // 'From'/'To' date
    // ------------------------------------------------------------------------

    /**
    *** Event Detail Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public EventDetailReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);

        /* has account */
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }

        /* has at least one device */
        if (this.getDeviceCount() <= 0) {
            throw new ReportException("No Devices specified");
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report handles only a single device at a time
    *** @return True If this report handles only a single device at a time
    **/
    public boolean isSingleDeviceOnly()
    {
        return true;
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
        return new ArrayDataIterator(ed); // 'EventDataLayout' expects EventData[]
    }
 
    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        // TODO: keep track of distance traveled for report?
        return null;
    }

    // ------------------------------------------------------------------------

}
