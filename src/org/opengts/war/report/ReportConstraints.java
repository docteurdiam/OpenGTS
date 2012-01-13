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
//  2007/03/25  Martin D. Flynn
//     -Added 'Rule Selector'
//  2008/03/28  Martin D. Flynn
//     -Renamed Constraints "Limit" tag to "SelectionLimit"
//     -Added a new "ReportLimit" tag
//  2009/01/01  Martin D. Flynn
//     -Added ascending/descending order
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.track.*;
import org.opengts.war.tools.*;

public class ReportConstraints
    implements Cloneable
{

    // ------------------------------------------------------------------------

    public static final int    WHERE_SET        = 0;
    public static final int    WHERE_OR         = 1;
    public static final int    WHERE_AND        = 2;

    // ------------------------------------------------------------------------
    // NOTE: ANY VARS ADDED HERE MUST BE COPIED IN THE COPY CONSTRUCTOR!!!!

    private long                timeStart       = -1L;
    private long                timeEnd         = -1L;
    
    private boolean             validGPS        = false;
    
    private EventData.LimitType selLimitType    = EventData.LimitType.FIRST;
    private long                selLimitCount   = -1L;
    private long                rptLimitCount   = -1L;
    
    private boolean             orderAscending  = true;

    private StringBuffer        whSelect        = new StringBuffer();
    
    private String              ruleSelector    = null;
    
    private int                 statusCodes[]   = null;
    
    private String              emailAddresses  = null;
    
    // ------------------------------------------------------------------------

    /**
    *** Default constructor
    **/
    public ReportConstraints() 
    {
        super();
    }
    
    /**
    *** Constructor
    *** @param timeStart  Time 'start' constraint
    *** @param timeEnd    Time 'end' constraint
    **/
    public ReportConstraints(long timeStart, long timeEnd)
    {
        this();
        this.setTimeRange(timeStart, timeStart);
    }

    /**
    *** Constructor
    *** @param timeStart    Time 'start' constraint
    *** @param timeEnd      Time 'end' constraint
    *** @param validGPS     If true, only events with a valid GPS fix are allowed, false to allow all events
    *** @param selLimitType The Selection limit type (ie. first/last)
    *** @param selLimit     The Selection limit
    **/
    public ReportConstraints(long timeStart, long timeEnd,
        boolean validGPS,
        EventData.LimitType selLimitType, long selLimit)
    {
        this();
        this.setTimeRange(timeStart, timeStart);
        this.setValidGPSRequired(validGPS);
        this.setSelectionLimit(selLimitType, selLimit);
        this.setReportLimit(selLimit); // report limit defaults to selection limit
    }

    /**
    *** Copy Constructor
    *** @param rc  The ReportConstraints to copy (shallow copy)
    **/
    public ReportConstraints(ReportConstraints rc) 
    {
        this();
        if (rc != null) {
            this.setTimeRange(rc.getTimeStart(), rc.getTimeEnd());
            this.setValidGPSRequired(rc.getValidGPSRequired());
            this.setSelectionLimit(rc.getSelectionLimitType(), rc.getSelectionLimit());
            this.setReportLimit(rc.getReportLimit());
            this.setWhere(rc.getWhere());
            this.setRuleSelector(rc.getRuleSelector());
            this.setStatusCodes(rc.getStatusCodes());
            this.setOrderAscending(rc.getOrderAscending());
        }
    }

    // ------------------------------------------------------------------------
    // Time range

    /**
    *** Sets the time 'start' constraint
    *** @param ts  The time 'start' constraint
    **/
    public void setTimeStart(long ts)
    {
        this.timeStart = (ts > 0L)? ts : -1L;
    }
    
    /**
    *** Gets the time 'start' constraint
    *** @return  The time 'start' constraint
    **/
    public long getTimeStart()
    {
        return this.timeStart;
    }

    /**
    *** Sets the time 'end' constraint
    *** @param te  The time 'start' constraint
    **/
    public void setTimeEnd(long te)
    {
        this.timeEnd = (te > 0L)? te : -1L;
    }

    /**
    *** Gets the time 'end' constraint
    *** @return  The time 'end' constraint
    **/
    public long getTimeEnd()
    {
        return this.timeEnd;
    }

    /**
    *** Sets the time 'start'/'end' constraint
    *** @param timeStart The time 'start' constraint
    *** @param timeEnd   The time 'end' constraint
    **/
    public void setTimeRange(long timeStart, long timeEnd)
    {
        this.timeStart = timeStart;
        this.timeEnd   = timeEnd;
    }

    // ------------------------------------------------------------------------
    // Valid GPS Required

    /**
    *** Returns true if all events require a valid GPS fix
    *** @return True if all events require a valid GPS fix
    **/
    public boolean getValidGPSRequired()
    {
        return this.validGPS;
    }

    /**
    *** Sets whether a valid GPS fix is required for all events
    *** @param reqGPS True to require valid a GPS fix, false to allow all events
    **/
    public void setValidGPSRequired(boolean reqGPS)
    {
        this.validGPS = reqGPS;
    }
   
    // ------------------------------------------------------------------------
    // Ascending order

    /** 
    *** Returns true if the data records are to be in ascending order
    *** @return True if the data records are to be in ascending order
    **/
    public boolean getOrderAscending()
    {
        return this.orderAscending;
    }

    /**
    *** Sets whether data records are to be in ascending order
    *** @param ascending True to sort data records in ascending order
    **/
    public void setOrderAscending(boolean ascending)
    {
        this.orderAscending = ascending;
    }

    // ------------------------------------------------------------------------
    // Selection Limit

    /** 
    *** Gets the selection limit type
    *** @return The selection limit type
    **/
    public EventData.LimitType getSelectionLimitType()
    {
        return this.selLimitType;
    }
    
    /**
    *** Gets the selection limit
    *** @return The selection limit (-1 for no limit)
    **/
    public long getSelectionLimit()
    {
        if (this.selLimitCount == 0L) {
            this.selLimitCount = 1L;
        }
        return this.selLimitCount;
    }
    
    /**
    *** Returns true if a selection limit has been defined
    *** @return True if a selection limit has been defined, false otherwise
    **/
    public boolean hasSelectionLimit()
    {
        return (this.getSelectionLimit() > 0L);
    }
    
    /**
    *** Sets the selection limit
    *** @param limit  The selection limit, or -1 to specify no limit
    **/
    public void setSelectionLimit(long limit)
    {
        if (limit < 0L) {
            this.selLimitCount = -1L;
        } else
        if (limit == 0L) {
            this.selLimitCount = 1L;
        } else {
            this.selLimitCount = limit;
        }
    }

    /**
    *** Sets the selection limit and type
    *** @param limitType The selection limit type (may be one of EventData.LimitType.FIRST or EventData.LimitType.LAST)
    *** @param limit     The selection limit
    **/
    public void setSelectionLimit(EventData.LimitType limitType, long limit)
    {
        this.selLimitType = limitType;
        this.setSelectionLimit(limit);
    }
   
    // ------------------------------------------------------------------------
    // Report Limit

    /**
    *** Gets the report limit
    *** @return The report limit (-1 for no limit)
    **/
    public long getReportLimit()
    {
        if (this.rptLimitCount == 0L) {
            this.rptLimitCount = 1L;
        }
        return this.rptLimitCount;
    }
    
    /**
    *** Returns true if a report limit has been defined
    *** @return True if a report limit has been defined, false otherwise
    **/
    public boolean hasReportLimit()
    {
        return (this.getReportLimit() > 0L);
    }
    
    /**
    *** Sets the report limit
    *** @param limit  The report limit, or -1 to specify no limit
    **/
    public void setReportLimit(long limit)
    {
        if (limit < 0L) {
            this.rptLimitCount = -1L;
        } else
        if (limit == 0L) {
            this.rptLimitCount = 1L;
        } else {
            this.rptLimitCount = limit;
        }
        //Print.logInfo("Report record limit: %d", this.rptLimitCount);
    }

    // ------------------------------------------------------------------------
    // Additional 'WHERE' selection

    /**
    *** Gets the selection 'WHERE' clause
    *** @return The selection 'WHERE' clause
    **/
    public String getWhere()
    {
        return this.whSelect.toString();
    }
    
    /**
    *** Returns true if a selection 'WHERE' clause has been defined
    *** @return True is a selection 'WHERE' clause has been defined
    **/
    public boolean hasWhere()
    {
        return (this.whSelect.length() > 0);
    }

    /**
    *** Sets the selection 'WHERE' clause
    *** @param dbsel  The DB selection 'WHERE' clause
    **/
    public void setWhere(String dbsel)
    {
        this._appendWhere(dbsel, WHERE_SET);
    }

    /**
    *** Adds ("OR's) the specified selection clause with the current 'WHERE' selection clause
    *** @param dbsel  The selection clause to append to the current clause
    **/
    public void orWhere(String dbsel)
    {
        this._appendWhere(dbsel, WHERE_OR);
    }

    /**
    *** Adds ("AND's) the specified selection clause with the current 'WHERE' selection clause
    *** @param dbsel  The selection clause to append to the current clause
    **/
    public void andWhere(String dbsel)
    {
        this._appendWhere(dbsel, WHERE_OR);
    }

    /**
    *** Appends the specified DB selection 'WHERE' clause
    *** @param dbsel  The selection clause to append
    *** @param op     The append operation (WHERE_SET, WHERE_OR, WHERE_AND)
    **/
    protected void _appendWhere(String dbsel, int op)
    {
        // WARNING: this does not make sure this selection is valid.
        String sel = StringTools.trim(dbsel);
        //Print.logInfo("AppendWhere: " + sel);
        
        /* clear existing selection if 'SET' */
        if (op == WHERE_SET) {
            this.whSelect.setLength(0);
            if (!sel.equals("")) {
                this.whSelect.append("(");
                this.whSelect.append(sel);
                this.whSelect.append(")");
            }
            return;
        }
        
        /* append selection (if specified) */
        if (!sel.equals("")) {
            // AND/OR selection
            if (this.whSelect.length() > 0) {
                // "( <OldSelect> AND/OR ( <AdditionalSelect> ) )"
                this.whSelect.insert(0,"(");
                if (op == WHERE_OR) {
                    this.whSelect.append(" OR ");
                } else {
                    this.whSelect.append(" AND ");
                }
                this.whSelect.append("(");
                this.whSelect.append(sel);
                this.whSelect.append("))");
            } else {
                // no prior selection criteria
                this.whSelect.append("(");
                this.whSelect.append(sel);
                this.whSelect.append(")");
            }
        }

    }

    // ------------------------------------------------------------------------
    // Rule Selector

    /**
    *** Gets the rule selector constraint
    *** @return The rule selector constraint
    **/
    public String getRuleSelector()
    {
        return (this.ruleSelector != null)? this.ruleSelector : "";
    }
    
    /**
    *** Return true if a rule selector constraint has been defined
    *** @return True if a rule selector constraint has been defined
    **/
    public boolean hasRuleSelector()
    {
        return (this.ruleSelector != null) && !this.ruleSelector.equals("");
    }

    /**
    *** Sets the rule selector constraint
    *** @param ruleSel The rule selector constraint
    **/
    public void setRuleSelector(String ruleSel)
    {
        this.ruleSelector = ruleSel;
    }

    // ------------------------------------------------------------------------
    // Status codes
    
    /**
    *** Gets the status code constraints
    *** @return An array of status codes
    **/
    public int[] getStatusCodes()
    {
        return this.statusCodes;
    }
    
    /**
    *** Return true if status code constraints have been defined
    *** @return True if status code constraints have been defined
    **/
    public boolean hasStatusCodes()
    {
        return (this.statusCodes != null) && (this.statusCodes.length > 0);
    }

    /**
    *** Sets the status code constraints
    *** @param sc The status code constraints
    **/
    public void setStatusCodes(int sc[])
    {
        this.statusCodes = ((sc != null) && (sc.length > 0))? sc : null;
    }

    // ------------------------------------------------------------------------
    // Email Addresses

    /* return true if report email addresses are present */
    public boolean hasEmailAddresses()
    {
        return !StringTools.isBlank(this.emailAddresses);
    }

    /* get report email addresses (used by "Service") */
    public String getEmailAddresses()
    {
        return this.emailAddresses;
    }

    /* sets the report email adresses (used by "Service") */
    public void setEmailAddresses(String emailAddr)
    {
        this.emailAddresses = StringTools.trim(emailAddr);
    }

    // ------------------------------------------------------------------------
    // Clone

    /**
    *** Returns a clone of this ReportConstraints instance
    *** @return The cloned ReportConstrains instance
    **/
    public Object clone()
    {
        return new ReportConstraints(this);
    }
    
    // ------------------------------------------------------------------------

    /** 
    *** Returns a String representation of this ReportConstraints instance
    *** @return The String representation
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("ReportConstraints:");
        sb.append(" TimeStart=" + this.getTimeStart());
        sb.append(" TimeEnd=" + this.getTimeEnd());
        sb.append(" ValidGPSRequired=" + this.getValidGPSRequired());
        sb.append(" SelectionLimitType=" + this.getSelectionLimitType());
        sb.append(" SelectionLimit=" + this.getSelectionLimit());
        sb.append(" ReportLimit=" + this.getReportLimit());
        sb.append(" Where='" + this.getWhere() + "'");
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

}
