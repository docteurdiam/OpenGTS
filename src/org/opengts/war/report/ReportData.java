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
//     -Added support for rule selectors
//     -Updated to use 'DeviceList'
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
//  2007/06/13  Martin D. Flynn
//     -Renamed 'DeviceList' to 'ReportDeviceList'
//  2007/06/30  Martin D. Flynn
//     -Added 'getTotalsDataIterator'
//  2007/11/28  Martin D. Flynn
//     -Integrated use of 'ReportColumn'
//  2008/02/21  Martin D. Flynn
//     -Modified '_getEventData' to set the Device on retrieved EventData records
//  2009/01/01  Martin D. Flynn
//     -Added 'setOrderAscending' to allow descending order EventData reports.
//  2009/11/01  Martin D. Flynn
//     -Added ReportOption support
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.PrivateLabel;
import org.opengts.war.tools.RequestProperties;
import org.opengts.war.tools.MapDimension;
import org.opengts.war.tools.OutputProvider;

import org.opengts.war.report.ReportFactory;
import org.opengts.war.report.ReportColumn;

public abstract class ReportData
{

    // ------------------------------------------------------------------------

    //public static final long RECORD_LIMIT           = 800L;
    
    // ------------------------------------------------------------------------

    public static final String FORMAT_MAP           = "map";
    public static final String FORMAT_KML           = "kml";
    public static final String FORMAT_GRAPH         = "graph";

    // ------------------------------------------------------------------------

    private static final String DFT_REPORT_NAME     = "generic.report";
    
    private static final String DFT_REPORT_TITLE    = "Generic Report";
    private static final String DFT_REPORT_SUBTITLE = "${deviceDesc} [${deviceId}]\n${dateRange}";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final ReportColumn EMPTY_COLUMNS[] = new ReportColumn[0];

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /*
    public static ReportOptionsProvider getReportOptionsProvider()
    {
        return new ReportOptionsProvider() {
            public OrderedMap<String,ReportOption> getReportOptionMap(ReportFactory rptFact, RequestProperties reqState) {
                //PrivateLabel privLabel = reqState.getPrivateLabel();
                //I18N i18n = privLabel.getI18N(ReportData.class);
                //OrderedMap<String,ReportOption> map = new OrderedMap<String,ReportOption>();
                //map.put("test1", new ReportOption("test1", i18n.getString("ReportData.option.1","This is Option 1"), null));
                //map.put("test2", new ReportOption("test2", i18n.getString("ReportData.option.2","This is Option 2"), null));
                //return map;
                return null;
            }
        };
    }
    */

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String              reportName          = DFT_REPORT_NAME;
    private String              reportTitle         = DFT_REPORT_TITLE;
    private String              reportSubtitle      = DFT_REPORT_SUBTITLE;

    private ReportEntry         rptEntry            = null;
    private ReportFactory       rptFactory          = null;
    
    private PrivateLabel        privLabel           = null;
    private RequestProperties   reqState            = null;
    private Account             account             = null;
    private User                user                = null;
    
    private String              preferredFormat     = "";
    
    private ReportDeviceList    deviceList          = null;

    private int                 eventDataCount      = 0;
    private int                 maxEventDataCount   = 0;

    private ReportConstraints   rptConstraints      = null;
    
    private ReportOption        reportOption        = null;
    private RTProperties        reportProperties    = null;

    private ReportColumn        rptColumns[]        = EMPTY_COLUMNS;
    
    private URIArg              refreshURL          = null;
    private URIArg              autoReportURL       = null;
    private URIArg              graphURL            = null;
    private URIArg              mapURL              = null;
    private URIArg              kmlURL              = null;

    private String              iconSelector        = null;
    
    private ReportCallback      rptCallback         = null;

    // ------------------------------------------------------------------------

    /* OBSOLETE: create an instance of a report */
    public ReportData(ReportFactory rptFact, RequestProperties reqState, Account acct, User user, ReportDeviceList devList)
        throws ReportException
    {
        this.rptFactory = rptFact;      // never null
        this.reqState   = reqState;     // never null
        this.privLabel  = this.reqState.getPrivateLabel();
        this.account    = acct;
        this.user       = user;
        this.deviceList = devList;
    }

    /* create an instance of a report */
    public ReportData(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        this.rptEntry   = rptEntry;                             // never null
        this.rptFactory = this.rptEntry.getReportFactory();     // never null
        this.reqState   = reqState;                             // never null
        this.privLabel  = this.reqState.getPrivateLabel();      // never null
        this.account    = this.reqState.getCurrentAccount();    // should not be null
        this.user       = this.reqState.getCurrentUser();       // may be null;
        this.deviceList = devList;
    }
    
    // ------------------------------------------------------------------------

    /* return the report entry which created this report */
    public ReportEntry getReportEntry()
    {
        return this.rptEntry; // may be null
    }

    /* return the report factory which ctreated this report */
    public ReportFactory getReportFactory()
    {
        return this.rptFactory; // never null
    }

    /* return the ReportFactory properties */
    public RTProperties getProperties()
    {
        if (this.reportProperties == null) {
            this.reportProperties = this.getReportFactory().getProperties(); // never null
            if (this.hasReportOption()) {
                this.reportProperties = new RTProperties(this.reportProperties);
                //this.reportProperties.printProperties("ReportData Properties:");
                //this.getReportOption().getProperties().printProperties("ReportOption Properties:");
                this.reportProperties.setProperties(this.getReportOption().getProperties());
                //this.reportProperties.printProperties("Combined Properties:");
            }
        }
        return this.reportProperties; // never null
    }

    // ------------------------------------------------------------------------
    
    /* name of this report */
    public void setReportName(String name)
    {
        this.reportName = name;
    }

    public String getReportName()
    {
        if ((this.reportName != null) && !this.reportName.equals("")) {
            return this.reportName;
        } else {
            return DFT_REPORT_NAME;
        }
    }
   
    // ------------------------------------------------------------------------

    /* type of this report */
    public String getReportType()
    {
        return this.getReportFactory().getReportType();
    }
   
    // ------------------------------------------------------------------------

    /* return report title */
    public void setReportTitle(String title)
    {
        this.reportTitle = title;
    }
    
    public String getReportTitle()
    {
        if ((this.reportTitle != null) && !this.reportTitle.equals("")) {
            return this.expandHeaderText(this.reportTitle);
        } else {
            return this.expandHeaderText(DFT_REPORT_NAME);
        }
    }
   
    // ------------------------------------------------------------------------

    /* return report sub-title */
    public void setReportSubtitle(String title)
    {
        this.reportSubtitle = title;
    }
    
    public String getReportSubtitle()
    {
        //if (!StringTools.isBlank(this.reportSubtitle)) {
            return this.expandHeaderText(this.reportSubtitle);
        //} else {
        //    return this.expandHeaderText(DFT_REPORT_SUBTITLE);
        //}
    }
    
    // ------------------------------------------------------------------------

    /* replace ${key} fields with the representative text */
    public String expandHeaderText(String text)
    {
        return ReportLayout.expandHeaderText(text, this);
    }

    // ------------------------------------------------------------------------
    // RequestProperties
    
    /* return the current RequestProperties */
    public RequestProperties getRequestProperties()
    {
        return this.reqState; // never null
    }
    
    /* return 'isSoapRequest' state */
    public boolean isSoapRequest()
    {
        return this.getRequestProperties().isSoapRequest();
    }

    /* return the TimeZone */
    public TimeZone getTimeZone()
    {
        return this.getRequestProperties().getTimeZone();
    }

    /* return the TimeZone */
    public String getTimeZoneString()
    {
        return this.getRequestProperties().getTimeZoneString(null);
    }

    // ------------------------------------------------------------------------
    // PrivateLabel
    
    /* return the current PrivateLabel */
    public PrivateLabel getPrivateLabel()
    {
        return this.privLabel;
    }

    /* return the PrivateLabel Locale */
    public Locale getLocale()
    {
        return this.getRequestProperties().getLocale();
    }

    // ------------------------------------------------------------------------

    /* set map icon selector */
    public void setMapIconSelector(String iconSel)
    {
        this.iconSelector = iconSel;
    }
    
    /* return default icon selector (may return null) */
    public String getMapIconSelector()
    {
        return this.iconSelector;
    }

    // ------------------------------------------------------------------------
    // Account 

    /* return the account */
    public Account getAccount()
    {
        return this.account;
    }
    
    /* return the ID for the account */
    public String getAccountID()
    {
        Account a = this.getAccount();
        return (a != null)? a.getAccountID() : "";
    }

    // ------------------------------------------------------------------------
    // User 

    /* return the user */
    public User getUser()
    {
        return this.user;
    }
    
    /* return the ID for the account */
    public String getUserID()
    {
        User u = this.getUser();
        return (u != null)? u.getUserID() : "";
    }

    // ------------------------------------------------------------------------
    // preferred format 

    /* gets the preferred format */
    public String getPreferredFormat()
    {
        return StringTools.trim(this.preferredFormat);
    }

    /* sets the preferred format */
    public void setPreferredFormat(String format)
    {
        this.preferredFormat = StringTools.trim(format);
    }

    // ------------------------------------------------------------------------
    // single device report

    /**
    *** Returns true if this report handles only a single device at a time
    *** @return True If this report handles only a single device at a time
    **/
    public boolean isSingleDeviceOnly()
    {
        return false;
    }

    // ------------------------------------------------------------------------
    // Devices

    /* set the device list */
    protected void setReportDeviceList(ReportDeviceList devList)
        throws ReportException
    {
        this.deviceList = devList;
    }

    /* return the device list */
    public ReportDeviceList getReportDeviceList()
    {
        if (this.deviceList == null) {
            this.deviceList = new ReportDeviceList(this.getAccount(),this.getUser());
            // sort by device description!
        }
        return this.deviceList;
    }

    /* return the number of devices in the list */
    public int getDeviceCount()
    {
        if (this.deviceList == null) {
            return 0;
        } else {
            return this.deviceList.size();
        }
    }

    /* return the first device id */
    public String getFirstDeviceID()
    {
        return this.getReportDeviceList().getFirstDeviceID();
    }
    
    /* return the Device record for the specified deviceID */
    public Device getDevice(String deviceID)
        throws DBException
    {
        ReportDeviceList devList = this.getReportDeviceList();
        return devList.getDevice(deviceID);
    }

    // ------------------------------------------------------------------------
    // report columns

    /* set report columns */
    public void setReportColumns(ReportColumn columns[])
    {
        this.rptColumns = (columns != null)? columns : EMPTY_COLUMNS;
    }

    /* return report columns */
    public ReportColumn[] getReportColumns()
    {
        return this.rptColumns;
    }

    /* return report columns */
    public int getColumnCount()
    {
        return this.rptColumns.length;
    }

    /* return true if this report has the named column */
    public boolean hasReportColumn(String name)
    {
        if (!StringTools.isBlank(name)) {
            for (ReportColumn rc : this.rptColumns) {
                if (rc.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ReportOption
    
    public boolean hasReportOption()
    {
        return (this.reportOption != null);
    }
    
    public ReportOption getReportOption()
    {
        return this.reportOption;
    }
    
    public void setReportOption(ReportOption rptOpt)
    {
        this.reportOption = rptOpt;
        this.reportProperties = null;
    }

    // ------------------------------------------------------------------------
    // set constraints used for retrieving EventData records
    
    /**
    *** Sets the ReportConstraints for this report
    *** @param rc  The ReportConstraints
    **/
    public void setReportConstraints(ReportConstraints rc)
    {
        // This is a clone of the ReportConstraints found in the report factory
        // This ReportConstraints object is owned only by this specific report and may
        // be modified if necessary.
        this.rptConstraints = rc;
    }
    
    /**
    *** Gets the ReportConstraints for this report
    *** @return The ReportConstraints
    **/
    public ReportConstraints getReportConstraints()
    {
        if (this.rptConstraints == null) {
            // this should never occur, but return a default report constraints
            this.rptConstraints = new ReportConstraints();  // should never occur!
        }
        return this.rptConstraints;
    }

    // ------------------------------------------------------------------------
    // The following allows the specific report to override any of the defined constraints

    /**
    *** Returns the 'rule' selector constraint
    *** @return The 'rule' selector constraint
    **/
    public String getRuleSelector()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getRuleSelector();
    }

    /**
    *** Returns the 'WHERE' selector constraint
    *** @return The 'WHERE' selector constraint
    **/
    public String getWhereSelector()
    {
        ReportConstraints rc = this.getReportConstraints();
        String wh = rc.getWhere();
        if (this.hasReportOption()) {
            ReportOption ro = this.getReportOption();
            wh = StringTools.replaceKeys(wh, ro.getProperties());
        }
        return wh;
    }

    /** 
    *** Returns the selection limit type constraint
    *** @return The selection limit type constraint
    **/
    public EventData.LimitType getSelectionLimitType()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getSelectionLimitType();
    }
    
    /** 
    *** Returns the selection limit constraint.
    *** @return The selection limit constraint
    **/
    public long getSelectionLimit()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getSelectionLimit();
    }

    /** 
    *** Returns the report limit constraint.
    *** @return The report limit constraint
    **/
    public long getReportLimit()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getReportLimit();
    }

    /** 
    *** Returns the time start constraint
    *** @return The time start constraint
    **/
    public long getTimeStart()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getTimeStart();
    }

    /** 
    *** Returns the time end constraint
    *** @return The time end constraint
    **/
    public long getTimeEnd()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getTimeEnd();
    }
    
    /** 
    *** Returns the "valid GPS required" constraint
    *** @return The "valid GPS required" constraint
    **/
    public boolean getValidGPSRequired()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getValidGPSRequired();
    }

    /** 
    *** Returns the status codes constraint
    *** @return The status codes constraint
    **/
    public int[] getStatusCodes()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getStatusCodes();
    }

    /** 
    *** Returns true if the data records are to be in ascending order
    *** @return True if the data records are to be in ascending order
    **/
    public boolean getOrderAscending()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getOrderAscending();
    }

    // ------------------------------------------------------------------------
    // ReportCallback
    
    /**
    *** Gets the ReportCallback instance (if specified)
    *** @return The ReportCallback instance, or null if not set
    **/
    public ReportCallback getReportCallback()
    {
        return this.rptCallback;
    }
    
    /**
    *** Sets the ReportCallback instance
    *** @param rptCB The ReportCallback instance
    **/
    public void setReportCallback(ReportCallback rptCB)
        throws ReportException
    {
        this.rptCallback = rptCB;
        if (this.rptCallback != null) {
            this.rptCallback.setReport(this);
        }
    }

    // ------------------------------------------------------------------------
    // EventData record retrieval

    /**
    *** Returns an array EventData records based on the predefined ReportDeviceList and constraints
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] getEventData(DBRecordHandler<EventData> rcdHandler)
    {
        long limit = this.getReportLimit(); // report record limit
        //Print.logInfo("ReportLimit: " + limit);
        ReportDeviceList devList = this.getReportDeviceList();
        java.util.List<EventData> edList = new Vector<EventData>();
        this.maxEventDataCount = 0;
        for (Iterator i = devList.iterator(); i.hasNext();) {
            String devID = (String)i.next();
            this.eventDataCount = 0;

            /* have we reached our limit? */
            if ((limit >= 0L) && (edList.size() >= limit)) {
                break;
            }
            // there is room for at least one more record

            /* get device records */
            try {
                Device device  = devList.getDevice(devID);
                EventData ed[] = this._getEventData(device, rcdHandler);
                if (limit < 0L) {
                    // no limit: add all of new EventData records to list
                    ListTools.toList(ed, edList);
                } else {
                    int maxRcds = (int)limit - edList.size(); // > 0
                    if (ed.length <= maxRcds) {
                        // under limit: add all of new EventData records to list
                        ListTools.toList(ed, edList);
                    } else {
                        // clip to limit
                        ListTools.toList(ed, 0, maxRcds, edList);
                    }
                }
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData for Device: " + devID);
            }

            /* maximum selected EventData records */
            if (this.eventDataCount > this.maxEventDataCount) {
                this.maxEventDataCount = this.eventDataCount;
            }

        }
        return edList.toArray(new EventData[edList.size()]);
    }

    /**
    *** Returns an array EventData records for the specified Device
    *** @param deviceDB     The Device for which EventData records will be selected
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] getEventData(Device deviceDB, DBRecordHandler<EventData> rcdHandler)
    {
        this.eventDataCount = 0;
        EventData ed[] = this._getEventData(deviceDB, rcdHandler);
        this.maxEventDataCount = this.eventDataCount;
        return ed;
    }

    /**
    *** Returns an array EventData records for the specified Device
    *** @param device       The Device for which EventData records will be selected
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] _getEventData(final Device deviceDB, final DBRecordHandler<EventData> rcdHandler)
    {

        /* Device */
        if (deviceDB == null) {
            return EventData.EMPTY_ARRAY;
        }

        /* Account */
        String accountID = this.getAccountID();
         //Print.logInfo("Getting EventData for " + accountID + "/" + deviceID);

        /* EventData rule selector (RuleFactory support required) */
        final String ruleSelector = this.getRuleSelector();
        final RuleFactory ruleFact;
        if (!StringTools.isBlank(ruleSelector)) {
            ruleFact = Device.getRuleFactory();
            if (ruleFact == null) {
                Print.logWarn("RuleSelector not supported");
            }
        } else {
            ruleFact = null;
        }

        /* create record handler */
        DBRecordHandler<EventData> evRcdHandler = new DBRecordHandler<EventData>() {
            public int handleDBRecord(EventData rcd) throws DBException {
                ReportData.this.eventDataCount++;
                EventData ev = rcd;
                ev.setDevice(deviceDB);
                boolean isMatch = (ruleFact != null)? ruleFact.isSelectorMatch(ruleSelector, ev) : true;
                if (!isMatch) {
                    // not a match 
                    return DBRH_SKIP;
                } else
                if (rcdHandler == null) {
                    // match, no default record handler 
                    return DBRH_SAVE;
                } else {
                    // match, send to default record handler
                    return rcdHandler.handleDBRecord(rcd);
                }
            }
        };

        /* get events */
        EventData ed[] = null;
        try {
            ed = EventData.getRangeEvents(
                accountID, deviceDB.getDeviceID(),
                this.getTimeStart(), this.getTimeEnd(),
                this.getStatusCodes(),
                this.getValidGPSRequired(),
                this.getSelectionLimitType(), this.getSelectionLimit(), this.getOrderAscending(),
                this.getWhereSelector(),
                evRcdHandler);
        } catch (DBException dbe) {
            Print.logException("Unable to obtain EventData records", dbe);
        }
        
        /* return events */
        if (ed == null) {
            return EventData.EMPTY_ARRAY;
        } else {
            // set device in each retrieved event
            for (int i = 0; i < ed.length; i++) {
                ed[i].setDevice(deviceDB);
            }
            return ed;
        }
        
    }

    /* return the actual counted EventData records from the last query (including all devices) */
    private long getEventDataCount()
    {
        return (long)this.eventDataCount;
    }

    /* return the largest counted EventData records from the last query for a single device */
    public long getMaximumEventDataCount()
    {
        return (long)this.maxEventDataCount;
    }

    /* return the count of EventData records based on the EventData constraints */
    protected long countEventData(Device deviceDB)
    {
        return this._countEventData(deviceDB);
    }

    /* return the count of EventData records based on the EventData constraints */
    protected long _countEventData(Device deviceDB)
    {

        /* Device */
        if (deviceDB == null) {
            return 0L;
        }

        /* Account */
        String accountID = this.getAccountID();
         //Print.logInfo("Getting EventData for " + accountID + "/" + deviceID);

        /* EventData rule selector */
        // (not supported)
        final String ruleSelector = this.getRuleSelector();
        if ((ruleSelector != null) && !ruleSelector.equals("")) {
            Print.logWarn("RuleSelector not supported when obtaining EventData record counts!");
        }

        /* get events */
        long recordCount = 0L;
        try {
            recordCount = EventData.countRangeEvents(
                accountID, deviceDB.getDeviceID(),
                this.getTimeStart(), this.getTimeEnd(),
                this.getStatusCodes(),
                this.getValidGPSRequired(),
                this.getSelectionLimitType(), this.getSelectionLimit(),
                this.getWhereSelector());
        } catch (DBException dbe) {
            Print.logException("Unable to obtain EventData record count", dbe);
        }
        
        /* return events */
        return recordCount;
        
    }

    // ------------------------------------------------------------------------
    // Auto Report URL

    public void setAutoReportURL(URIArg autoReportURL)
    {
        this.autoReportURL = autoReportURL;
    }

    public URIArg getAutoReportURL()
    {
        return this.autoReportURL;
    }

    // ------------------------------------------------------------------------
    // Graph URL
    
    /**
    *** Returns true if this report supports displaying a graph
    *** @return True if this report supports displaying a graph, false otherwise
    **/
    public boolean getSupportsGraphDisplay()
    {
        // override in subclass
        return false;
    }

    public void setGraphURL(URIArg graphURL)
    {
        if (this.getSupportsGraphDisplay()) {
            this.graphURL = graphURL;
        }
    }

    public URIArg getGraphURL()
    {
        return this.getSupportsGraphDisplay()? this.graphURL : null;
    }
    
    public String getGraphLinkDescription()
    {
        return null;
    }

    public MapDimension getGraphWindowSize()
    {
        return new MapDimension(730,440);
    }

    // ------------------------------------------------------------------------
    // Map URL

    /**
    *** Returns true if this report supports displaying a map
    *** @return True if this report supports displaying a map, false otherwise
    **/
    public boolean getSupportsMapDisplay()
    {
        // override in subclass
        return false;
    }

    public void setMapURL(URIArg mapURL)
    {
        if (this.getSupportsMapDisplay()) {
            this.mapURL = mapURL;
            //Print.logInfo("Map URL: " + this.mapURL);
        }
    }

    public URIArg getMapURL()
    {
        return this.getSupportsMapDisplay()? this.mapURL : null;
    }

    public String getMapLinkDescription()
    {
        return null;
    }

    public MapDimension getMapWindowSize()
    {
        return new MapDimension(700,500);
    }

    // ------------------------------------------------------------------------
    // KML URL

    /**
    *** Returns true if this report supports displaying KML
    *** @return True if this report supports displaying KML, false otherwise
    **/
    public boolean getSupportsKmlDisplay()
    {
        // override in subclass
        return false;
    }

    public void setKmlURL(URIArg kmlURL)
    {
        if (this.getSupportsKmlDisplay()) {
            this.kmlURL = kmlURL;
            //Print.logInfo("KML URL: " + this.kmlURL);
        }
    }

    public URIArg getKmlURL()
    {
        return this.getSupportsKmlDisplay()? this.kmlURL : null;
    }

    public String getKmlLinkDescription()
    {
        return null;
    }

    // ------------------------------------------------------------------------
    // Refresh URL

    public void setRefreshURL(URIArg refreshURL)
    {
        this.refreshURL = refreshURL;
        //Print.logInfo("Refresh URL: " + this.refreshURL);
    }
    
    public URIArg getRefreshURL()
    {
        return this.refreshURL;
    }

    // ------------------------------------------------------------------------
    // Start report
    
    /**
    *** This method is called after all other ReportConstraints have been set.
    *** The report has this opportunity to make any changes to the ReportConstraints
    *** before the report is actually generated
    **/
    public void postInitialize()
    {
        // last oportunity for the report to configure itself before actually writing out data
        // To prevent requireing that the subclass call "super.postInitialize()" it is
        // strongly recommended that this placeholder method always be empty.
    }

    // ------------------------------------------------------------------------
    // ReportLayout

    public abstract ReportLayout getReportLayout();

    // ------------------------------------------------------------------------
    // DataRow

    public DataRowTemplate getDataRowTemplate()
    {
        return this.getReportLayout().getDataRowTemplate();
    }

    // ------------------------------------------------------------------------

    /* write table to PrintWriter */
    public void writeReportStyle(String format, OutputProvider out)
        throws ReportException
    {
        String fmt = StringTools.blankDefault(format, this.getPreferredFormat());
        this.getReportLayout().writeReportStyle(fmt, this, out, 0);
    }

    /* write table to PrintWriter */
    public int writeReport(String format, OutputProvider out)
        throws ReportException
    {
        String fmt = StringTools.blankDefault(format, this.getPreferredFormat());
        return this.getReportLayout().writeReport(fmt, this, out, 0);
    }

    /* write table to PrintWriter */
    public int writeReport(String format, OutputProvider out, int indentLevel)
        throws ReportException
    {
        String fmt = StringTools.blankDefault(format, this.getPreferredFormat());
        return this.getReportLayout().writeReport(fmt, this, out, indentLevel);
    }

    // ------------------------------------------------------------------------
    // DBDataIterator

    // The subclass of this object must implement this method.
    // For simple EventData record data, this method could simply return:
    //   new ArrayDataIterator(this.getEventData());
    public abstract DBDataIterator getBodyDataIterator();
    
    // The subclass of this object must implement this method.
    // For simple EventData record data, this method may simply return null.
    public abstract DBDataIterator getTotalsDataIterator();

    /* this is an implementation of DBDataIterator that iterates through an array of row objects */
    public class ArrayDataIterator
        implements DBDataIterator
    {
        private int recordIndex = -1;
        private Object    data[]   = null;
        private Object    dataObj  = null;
        private DBDataRow dataRow  = null;
        
        public ArrayDataIterator(Object data[]) {
            this.data = data;
            this.recordIndex = -1;
            this.dataRow = new DBDataRowAdapter(ReportData.this) {
                public Object getRowObject() {
                    return ArrayDataIterator.this.dataObj;
                }
                public Object getDBValue(String name, int rowNdx, ReportColumn rptCol) {
                    Object obj = ArrayDataIterator.this.dataObj;
                    if (obj != null) {
                        DataRowTemplate drt = ReportData.this.getDataRowTemplate();
                        return drt.getFieldValue(name, rowNdx, ReportData.this, rptCol, obj);
                    } else {
                        return "";
                    }
                }
            };
        }
        
        public Object[] getArray() {
            return this.data;
        }

        public boolean hasNext() {
            return (this.data != null) && ((this.recordIndex + 1) < this.data.length);
        }

        public DBDataRow next() {
            if (this.hasNext()) {
                this.recordIndex++;
                this.dataObj = this.data[this.recordIndex];
                return this.dataRow;
            } else {
                this.dataObj = null;
                return null;
            }
        }
        
    }

    /* this is an implementation of DBDataIterator that iterates through an array of row objects */
    protected class ListDataIterator
        implements DBDataIterator
    {
        private Iterator  dataIter = null;
        private Object    dataObj  = null;
        private DBDataRow dataRow  = null;
        
        public ListDataIterator(java.util.List data) {
            this.dataIter = (data != null)? data.iterator() : null;
            this.dataRow = new DBDataRowAdapter(ReportData.this) {
                public Object getRowObject() {
                    return ListDataIterator.this.dataObj;
                }
                public Object getDBValue(String name, int rowNdx, ReportColumn rptCol) {
                    Object obj = ListDataIterator.this.dataObj;
                    if (obj != null) {
                        DataRowTemplate rdp = ReportData.this.getDataRowTemplate();
                        return rdp.getFieldValue(name, rowNdx, ReportData.this, rptCol, obj);
                    } else {
                        return "";
                    }
                }
            };
        }
        
        public boolean hasNext() {
            return (this.dataIter != null) && this.dataIter.hasNext();
        }
        
        public DBDataRow next() {
            if (this.hasNext()) {
                this.dataObj = this.dataIter.next();
                return this.dataRow;
            } else {
                this.dataObj = null;
                return null;
            }
        }

    }

    // ------------------------------------------------------------------------
    
}
