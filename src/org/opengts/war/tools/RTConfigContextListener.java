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
// References:
//  - http://livedocs.macromedia.com/jrun/4/Programmers_Guide/servletlifecycleevents3.htm
// ----------------------------------------------------------------------------
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2009/01/28  Martin D. Flynn
//     -Added support for monitoring HttpSessions
//     -Servlet context properties now initialized via DBConfig.servletInit(...)
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

import org.opengts.Version;
import org.opengts.db.*;
import org.opengts.dbtools.DBConnection;

/**
*** This class initializes and loads the servlet configuration properties into the Runtime configuration
*** class RTConfig.  A reference to this class is typically placed in the Servlet 'web.xml' file as follows:<br>
*** <pre>
***   &lt;listener&gt;
***       &lt;listener-class&gt;org.opengts.war.tools.RTConfigContextListener&lt;/listener-class&gt;
***   &lt;/listener&gt;
*** </pre>
**/

public class RTConfigContextListener
    implements ServletContextListener, HttpSessionListener
{

    // ------------------------------------------------------------------------

    /* must patch "org.opengts.war.track.Constants.*" */
    public static final String PARM_ACCOUNT = "account";
    public static final String PARM_USER    = "user";

    // ------------------------------------------------------------------------

    public static final String PROP_DebugMode[] = new String[] { 
        "debugMode", 
        "debug" 
    };

    public static final String PROP_DBConfig_init[] = new String[] { 
        "DBConfig.init", 
        "DBConfig.servletInit" 
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public interface HttpSessionFilter
    {
        public boolean countSession(HttpSession session);
    }

    /**
    *** Returns the current number of sessions
    *** @param sc  The ServletContext
    *** @return The current number of sessions
    **/
    public static int GetSessionCount(ServletContext sc)
    {
        return RTConfigContextListener.GetSessionCount(sc, null);
    }

    /**
    *** Returns the current number of sessions matching the specified filter
    *** @param sc       The ServletContext
    *** @param filter   The HttpSession filter (total session counter returned if filter is null)
    *** @return The current number of sessions matching the specified filter
    **/
    public static int GetSessionCount(ServletContext sc, HttpSessionFilter filter)
    {
        RTConfigContextListener rccl = (sc != null)? (RTConfigContextListener)sc.getAttribute("RTConfigContextListener") : null;
        if (rccl != null) {
            return rccl.getSessionCount(filter);
        } else {
            Print.logWarn("RTConfigContextListener not found!");
            return -1;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int                             sessionCount = 0;
    private Map<String,HttpSession>         sessionMap   = Collections.synchronizedMap(new HashMap<String,HttpSession>());

    // ------------------------------------------------------------------------
    // ServletContextListener interace

    /**
    *** This method is called by the Servlet container when the Servlet context is initialized
    *** @param sce  A ServletContextEvent instance
    **/
    public void contextInitialized(ServletContextEvent sce)
    {
        ServletContext srvCtx = sce.getServletContext();
        // ServletContext also provides the following logging methods:
        //   log(String msg);
        //   log(String msg, Throwable th);
        
        // Note: Log output occurring in this method will appear in the Tomcat 'catalina.out'
        // file, regardless of the 'log.file.enable' specified state.

        /* initialize Servlet context constant properties */
        RTProperties constantProps = RTConfig.getRuntimeConstantProperties();

        /* context name */
        String srvCtxName = StringTools.trim(srvCtx.getServletContextName());
        if (!StringTools.isBlank(srvCtxName)) {
            Print.logInfo("Context Name: " + srvCtxName);
            if (srvCtxName.startsWith("dcs.") || srvCtxName.startsWith("dcs:")) {
                String dcsName = srvCtxName.substring(4).trim();
                constantProps.setProperty(RTKey.CONTEXT_NAME, dcsName);
                DCServerFactory.SetSpecificDCServerName(dcsName);
            } else
            if (srvCtxName.startsWith("dcs_") || srvCtxName.startsWith("dcs-")) {
                constantProps.setProperty(RTKey.CONTEXT_NAME, srvCtxName);
                String dcsName = srvCtxName.substring(4).trim();
                DCServerFactory.SetSpecificDCServerName(dcsName);
            } else {
                constantProps.setProperty(RTKey.CONTEXT_NAME, srvCtxName);
            }
            // the context name is typically referenced in 'webapps.conf' to set the log file name
        } else {
            Print.logWarn("Context Name not defined");
        }

        /* context path */
        String srvCtxPath = StringTools.trim(srvCtx.getRealPath(""));
        if (!StringTools.isBlank(srvCtxPath)) {
            Print.logInfo("Context Path: " + srvCtxPath);
            constantProps.setProperty(RTKey.CONTEXT_PATH, srvCtxPath);
        } else {
            Print.logWarn("Context Path not defined: " + srvCtxName);
        }

        /* initialize Servlet context properties */
        boolean dbConfigInit = true;
        Properties srvCtxProps = new Properties();
        Enumeration parmEnum = srvCtx.getInitParameterNames();
        for (;parmEnum.hasMoreElements();) {
            String key = parmEnum.nextElement().toString();
            String val = srvCtx.getInitParameter(key);
            if (val != null) {
                Print.logInfo("Adding Servlet property: " + key + " ==> " + val);
                srvCtxProps.setProperty(key, val);
                if (ListTools.contains(PROP_DebugMode,key)) {
                    boolean debugMode = StringTools.parseBoolean(val,true);
                    RTConfig.setDebugMode(debugMode);
                    if (debugMode) {
                        Print.setLogLevel(Print.LOG_ALL);               // log everything
                        Print.setLogHeaderLevel(Print.LOG_ALL);         // include log header on everything
                    } else {
                        // leave as-is
                    }
                } else
                if (ListTools.contains(PROP_DBConfig_init,key)) {
                    // override to disable "DBConfig.servletInit(...)" ...
                    dbConfigInit = StringTools.parseBoolean(val,true);
                }
            }
        }
        if (dbConfigInit) {
            DBConfig.servletInit(srvCtxProps);
        } else {
            Print.logInfo("Skipping 'DBConfig.servletInit' ...");
            RTConfig.setServletContextProperties(srvCtxProps);
            if (RTConfig.isDebugMode()) {
                Print.setLogLevel(Print.LOG_ALL);                       // log everything
                Print.setLogHeaderLevel(Print.LOG_ALL);                 // include log header on everything
            }
        }

        /* save this RTConfigContextListener in the ServletContext */
        srvCtx.setAttribute("RTConfigContextListener", this);

        /* java.awt.headless */
        String headless = System.getProperty("java.awt.headless","false");
        Print.logInfo("java.awt.headless=" + headless);

        /* display where the log output is being sent to */
        File logFile = Print.getLogFile();
        if (logFile != null) {
            Print.sysPrintln("["+srvCtxName+"] Logging to file: " + logFile);
        } else {
            Print.sysPrintln("["+srvCtxName+"] Logging to default location");
        }

    }

    /**
    *** This method is called by the Servlet container when the Servlet context is destroyed
    *** @param sce  A ServletContextEvent instance
    **/
    public void contextDestroyed(ServletContextEvent sce)
    {
        ServletContext srvCtx = sce.getServletContext();
        String srvCtxName = srvCtx.getServletContextName();
        //RTConfig.clearServletContextProperties(null);
        DBConnection.closeAllConnections();
        Print.logInfo("... Servlet Context destroyed: " + srvCtxName);
        Print.logInfo("-----------------------------------------------");
    }

    // ------------------------------------------------------------------------
    // HttpSessionListener interace

    /**
    *** This method is called by the Servlet container when a HttpSession is created
    *** @param se  The HttpSessionEvent
    **/
    public void sessionCreated(HttpSessionEvent se) 
    {
        HttpSession    hs  = se.getSession();
        String         hid = hs.getId();
        synchronized (this.sessionMap) {
            this.sessionCount++; // may be just a login screen
            this.sessionMap.put(hid, hs);
            //Print.logInfo("Created - SessionCount: " + this.sessionCount);
        }
    }

    /**
    *** This method is called by the Servlet container when a HttpSession is destroyed
    *** @param se  The HttpSessionEvent
    **/
    public void sessionDestroyed(HttpSessionEvent se) 
    {
        HttpSession    hs  = se.getSession();
        String         hid = hs.getId();
        synchronized (this.sessionMap) {
            if (this.sessionCount > 0) { this.sessionCount--; }
            this.sessionMap.remove(hid);
            //Print.logInfo("Destroyed - SessionCount: " + this.sessionCount);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a list of current HttpSession instances
    *** @return A list of HttpSession instances
    **/
    public Collection<HttpSession> getSessions()
    {
        Collection<HttpSession> list = null;
        synchronized (this.sessionMap) {
            list = new Vector<HttpSession>(this.sessionMap.values());
        }
        return list;
    }
    
    /**
    *** Returns the current number of open sessions
    *** @param filter  The HttpSession filter
    *** @return The current number of open sessions
    **/
    public int getSessionCount(HttpSessionFilter filter)
    {
        int count = 0;
        synchronized (this.sessionMap) {
            if (filter == null) {
                count = this.sessionCount;
            } else {
                //Print.logInfo("Session map size: %d", this.sessionMap.size());
              //for (HttpSession hs : this.sessionMap.values()) [
                for (String id : this.sessionMap.keySet()) {
                    HttpSession hs = this.sessionMap.get(id);
                    if ((hs != null) && filter.countSession(hs)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------------

}

    
