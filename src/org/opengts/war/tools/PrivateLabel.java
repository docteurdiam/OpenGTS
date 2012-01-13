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
// The features this class provides are highly configurable through the external
// XML file 'private.xml'.  However, this code may also be modified to provide
// special custom features for the GPS tracking page.
// ----------------------------------------------------------------------------
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/03/11  Martin D. Flynn
//     -Changed XML to place PageHeader/PageFooter/PageLeft/PageRight inside a
//      PageDecorations tag.
//     -Implemented two flavors of page decorations, one which is displayed when
//      no user is logged in, another which is displayed when a user is logged in.
//     -Added ReportFactory support
//  2007/03/30  Martin D. Flynn
//     -Added 'User' login support
//     -Added access control support
//  2007/05/06  Martin D. Flynn
//     -Added support for 'Page' tags 'menuText', 'menuHelp', and 'navText'
//  2007/05/20  Martin D. Flynn
//     -Added 'properties' attribute to 'MapProvider tag.
//     -Removed 'Geocoder' tag (use GeocodeProvider/ReverseGeocodeProvider instead)
//  2007/05/25  Martin D. Flynn
//     -Added 'restricted' attribute
//  2007/06/03  Martin D. Flynn
//     -Added 'locale' attribute (for I18N support)
//     -Removed 'menuText', 'menuHelp', 'navText' attributes (replaced by i18n)
//  2007/06/30  Martin D. Flynn
//     -Added host 'alias' support method 'addHostAlias(...)'
//     -Added support for overriding the default map dimensions.
//  2007/07/27  Martin D. Flynn
//     -'MapProvider' properties now supports ';' property separator.
//  2007/09/16  Martin D. Flynn
//     -XML loading moved to PrivateLabelLoader.java
//  2008/09/12  Martin D. Flynn
//     -Move "Domain" property keys to this module.
//  2009/05/24  Martin D. Flynn
//     -Moved all property definitions from here to 'BasicPrivateLabel.java'
//  2009/05/24  Martin D. Flynn
//     -Added convenience method "getPushpinIconIndex" for converting a pushpin
//      ID to the MapProvider pushpin icon index.
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.Color;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.geocoder.*;

import org.opengts.war.report.ReportException;
import org.opengts.war.report.ReportFactory;
import org.opengts.war.report.ReportEntry;

public class PrivateLabel
    extends BasicPrivateLabel
{

    // ------------------------------------------------------------------------

    public  static final String  DEFAULT_CSS_DIR                = "css";
    
    public  static final String  JSPENTRY_DEFAULT               = "default";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // see "isGlobalPushpins"
    private static OrderedMap<String,PushpinIcon> GlobalPushpins = null;

    public static OrderedMap<String,PushpinIcon> GetGlobalPushpinIcons()
    {
        if (GlobalPushpins == null) {
            GlobalPushpins = new OrderedMap<String,PushpinIcon>(PushpinIcon.newDefaultPushpinIconMap());
        }
        return GlobalPushpins;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class JSPEntry
    {
        private String    name     = null;
        private I18N.Text desc     = null;
        private String    file     = null;
        private String    cssDir   = null;
        public JSPEntry(String name, I18N.Text desc, String file) {
            this.name    = name;
            this.desc    = desc;
            this.file    = file;
            this.cssDir  = null;
        }
        public String getName() {
            return this.name;
        }
        public String getDescription(Locale locale) {
            if (this.desc != null) {
                return this.desc.toString(locale);
            } else {
                return this.name;
            }
        }
        public String getFile() {
            return this.file;
        }
        public String getCSSDir() {
            return this.cssDir;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // Page decorations
    private PageDecorations                 dftPageDecorations          = null;
    private PageDecorations                 userPageDecorations         = null;

    // Displayed map provider
    private OrderedMap<String,MapProvider>  mapProvider                 = null;

    // map of PrivateLabel web-pages
    private String                          webPageJSP                  = null;
    private Map<String,WebPage>             pageMap                     = null;
    
    // CSS file map
    private String                          cssDirectory                = DEFAULT_CSS_DIR;
    private Map<String,String>              cssFileMap                  = new HashMap<String,String>();

    // map of MenuGroups
    private Map<String,MenuGroup>           menuGroupMap                = null;

    // map of PrivateLabel reports
    private Map<String,ReportEntry>         reportMap                   = null;

    // JSP File map
    private Map<String,JSPEntry>            jspMap                      = null;

    // TimeZone ComboMap
    private ComboMap                        timeZonesMap                = null;

    /**
    *** Constructor 
    **/
    private PrivateLabel()
    {
        super();
    }

    /**
    *** Constructor 
    *** @param host  host/domain name
    **/
    protected PrivateLabel(String host)
    {
        super(host);
    }

    //public PrivateLabel(String host, String title)
    //{
    //    super(host, title);
    //}

    // ------------------------------------------------------------------------
    
    /**
    *** Returns true if user page decorations have been defined
    *** @return True if user page decorations have been defined
    **/
    public boolean hasUserPageDecorations()
    {
        return (this.userPageDecorations != null);
    }

    /**
    *** Sets the user page decorations
    *** @param pd  The user page decorations
    **/
    public void setUserPageDecorations(PageDecorations pd)
    {
        this.userPageDecorations = pd;
    }

    /**
    *** Gets the user page decorations
    *** @return The user page decorations
    **/
    public PageDecorations getUserPageDecorations()
    {
        if (this.userPageDecorations != null) {
            return this.userPageDecorations;
        } else {
            return this.getDefaultPageDecorations();
        }
    }
    
    /**
    *** Sets the default page decorations
    *** @param pd  The default page decorations
    **/
    public void setDefaultPageDecorations(PageDecorations pd)
    {
        this.dftPageDecorations = pd;
    }

    /**
    *** Gets the default page decorations
    *** @return The default page decorations
    **/
    public PageDecorations getDefaultPageDecorations()
    {
        if (this.dftPageDecorations == null) {
            this.dftPageDecorations = new PageDecorationsDefault(this);
        }
        return this.dftPageDecorations;
    }

    // ------------------------------------------------------------------------
    // Currently, only one map provider option for a specific account

    /**
    *** Adds a supported MapProvider.
    *** @param mapProv  The MapProvider
    **/
    public void addMapProvider(MapProvider mapProv)
    {
        if (mapProv != null) {
            if (this.mapProvider == null) {
                this.mapProvider = new OrderedMap<String,MapProvider>();
            }
            String name = mapProv.getName().toLowerCase();
            this.mapProvider.put(name, mapProv);
        }
    }

    /**
    *** Returns the first MapProvider 
    *** @return The first MapProverer
    **/
    public MapProvider getMapProvider()
    {
        return (this.mapProvider != null)? this.mapProvider.getValue(0) : null;
    }

    /**
    *** Gets the named MapProvider
    *** @param name  The name of the MapProvider to return
    **/
    public MapProvider getMapProvider(String name)
    {
        if ((name != null) && (this.mapProvider != null)) {
            return this.mapProvider.get(name.toLowerCase());
        } else {
            return null;
        }
    }

    /**
    *** Returns the number of MapProviders
    *** @return The number of MapProviders
    **/
    public int getMapProviderCount()
    {
        return (this.mapProvider != null)? this.mapProvider.size() : 0;
    }

    /**
    *** Return the MapProvider's Pushpin index for the specified pushpin ID
    *** @param mapProviderID  The MapProvider ID (may be null)
    *** @param pushpinID      The pushpin ID
    *** @param dftIndex       The default index value (0..9 are always defined)
    *** @return The pushpin icon index
    **/
    public int getPushpinIconIndex(String mapProviderID, String pushpinID, int dftIndex)
    {
        MapProvider mp = StringTools.isBlank(mapProviderID)? 
            this.getMapProvider() : this.getMapProvider(mapProviderID);
        if (mp != null) {
            OrderedMap<String,PushpinIcon> iconMap = mp.getPushpinIconMap(null);
            OrderedSet<String> iconKeys = (OrderedSet<String>)iconMap.keySet(); 
            return EventData._getPushpinIconIndex(pushpinID, iconKeys, dftIndex); 
        } else {
            // only "black"(0) through "white"(9) supported here
            return EventData._getPushpinIconIndex(pushpinID, null, dftIndex); 
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default JSP URI
    *** @param dftJSP  The default JSP URI (may be a name, or absolute URI path entry)
    **/
    public void setWebPageJSP(String dftJSP)
    {
        this.webPageJSP = dftJSP;
    }

    /**
    *** Gets the assigned WebPage JSP URI
    *** @return The assigned WebPage JSP URI
    **/
    public String getWebPageJSP()
    {
        return this.webPageJSP;
    }

    /**
    *** Gets the default WebPage JSP URI (adjusted by RequestProperties)
    *** @return The WebPage JSP URI
    **/
    protected String _getWebPageJSP(RequestProperties reqState)
    {
        String jsp = StringTools.blankDefault(this.getWebPageJSP(), JSPENTRY_DEFAULT);
        if (StringTools.isBlank(jsp)) {
            // (will not occur) a default JSP not specified.
            return null;
        } else
        if (jsp.startsWith("/")) {
            // absolute file path specified
            return jsp;
        } else
        if (!ListTools.isEmpty(this.jspMap)) {
            // look up JSP name
            String jn = StringTools.replaceKeys(jsp, reqState, null);
            String jf = this.getJSPFile(jn, true);
            if (!StringTools.isBlank(jf)) {
                return jf;
            } else {
                // return null
                Print.logWarn("Named JSP name not found: " + jn + " [" + jsp + "]");
                return null;
            }
        } else {
            // return as-is
            return jsp;
        }
    }

    /**
    *** Gets the WebPage JSP URI
    *** @return The default JSP URI
    **/
    public String getWebPageJSP(String jsp, RequestProperties reqState)
    {
        if (StringTools.isBlank(jsp)) {
            // return default
            return this._getWebPageJSP(reqState); // may be null
        } else
        if (jsp.startsWith("/")) {
            // absolute path
            return jsp;
        } else {
            // lookup name
            String jn = StringTools.replaceKeys(jsp, reqState, null);
            String jf = this.getJSPFile(jn, false);
            if (!StringTools.isBlank(jf)) {
                return jf;
            } else {
                // name not found, get default value
                Print.logWarn("Named JSP name not found: " + jn + " [" + jsp + "]");
                return this._getWebPageJSP(reqState); // may be null
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the CSS directory 
    *** @param cssDir  The CSS directory
    **/
    public void setCssDirectory(String cssDir)
    {
        this.cssDirectory = cssDir;
    }

    /**
    *** Gets the CSS directory 
    *** @return The CSS directory
    **/
    public String getCssDirectory()
    {
        if (StringTools.isBlank(this.cssDirectory)) {
            return DEFAULT_CSS_DIR;
        } else {
            return this.cssDirectory;
        }
    }

    /** 
    *** Resolves the path to the specified CSS dir/file
    *** @param cssFileName  The CSS file name
    *** @param cssFileDir   An optional CSS file directory
    *** @return The resolved CSS file paths
    **/
    public String resolveCssFile(String cssFileName, String cssFileDir)
    {

        /* already absolute file reference */
        if (URIArg.isAbsoluteURL(cssFileName)) {
            // return as-is
            return cssFileName;
        }

        /* search for file */
        String cssRelPath = this.cssFileMap.get(cssFileName);
        if (cssRelPath == null) {
            // key does not exist
            synchronized (this.cssFileMap) {
                // try again after synchronizing
                cssRelPath = this.cssFileMap.get(cssFileName);
                if (cssRelPath == null) {
                    // still doesn't exist, now we can create the entry
                    cssRelPath = "";
        
                    /* get file */
                    String cssDir = !StringTools.isBlank(cssFileDir)? cssFileDir : this.getCssDirectory();
                    if (!StringTools.isBlank(cssDir)) {
                        if (cssDir.startsWith("/")) {
                            cssRelPath += cssDir.substring(1) + "/";
                        } else {
                            cssRelPath += cssDir + "/";
                        }
                    }
                    cssRelPath += cssFileName;
            
                    /* check for file existance */
                    File rootDir = RTConfig.getServletContextPath(); // may return null
                    if (rootDir != null) {
                        File cssAbsPath = new File(rootDir, cssRelPath);
                        if (!cssAbsPath.isFile() && !cssDir.equals(DEFAULT_CSS_DIR)) {
                            //Print.logWarn("'%s' not found, trying with 'css/...'", cssFilePath);
                            cssRelPath = DEFAULT_CSS_DIR + "/" + cssFileName;
                        }
                    }
                
                    /* save */
                    this.cssFileMap.put(cssFileName, cssRelPath);
                
                }
            }
        }
        
        return cssRelPath;

    }

    // ------------------------------------------------------------------------

    public static final String CONTEXT_FILE_OFFLINE = ".offline";

    /**
    *** Gets the Context offline message.  Returns null if the Context has not been placed offline.
    *** @return The Context offline message, or null if the Context is not offline.  Note: Global
    ***         offline may still be in effect.
    **/
    private static String _readOfflineMessage(File offlineFile, String n)
    {
        if ((offlineFile != null) && offlineFile.exists()) {
            if (offlineFile.isFile()) {
                byte msg[] = FileTools.readFile(offlineFile);
                return StringTools.trim(StringTools.toStringValue(msg)); // non-null
            } else {
                Print.logError(n + " Offline indicator exists, but is not a file: " + offlineFile);
                return "";
            }
        } else {
            return null;
        }
    }

    /**
    *** Gets the Context offline message.  Returns null if the Context has not been placed offline.
    *** @return The Context offline message, or null if the Context is not offline.  Note: Global
    ***         offline may still be in effect.
    **/
    public static String GetContextOfflineMessage()
    {
        File rd = RTConfig.getServletContextPath(); // may return null
        return (rd != null)? PrivateLabel._readOfflineMessage(new File(rd,CONTEXT_FILE_OFFLINE),"Context") : null;
    }

    /**
    *** Gets the Global offline message.  Returns null if Global offline is not in effect.
    *** @return The Global offline message, or null if Global offline is not in effect.  Note: Context
    ***         offline may still be in effect.
    **/
    public static String GetGlobalOfflineMessage()
    {
        return PrivateLabel._readOfflineMessage(RTConfig.getFile(DBConfig.PROP_track_offlineFile,null),"Global");
    }

    /**
    *** Gets the Context/Global offline message.  Returns null if the system is online.
    *** @return The Context/Global offline message, or null if the system is online.
    **/
    public static String GetOfflineMessage()
    {
        String msg = GetContextOfflineMessage();
        if (msg == null) {
            msg = GetGlobalOfflineMessage();
        }
        return msg;
    }

    /**
    *** Sets the context offline message<br>
    *** Note: This method effects only the Context offline settings.  Any global offlines setting
    *** will not be effected.
    *** @param msg  The offline message, or 'null' to place back 'online'
    **/
    public static void SetContextOfflineMessage(String msg)
    {
        File rootDir = RTConfig.getServletContextPath(); // may return null
        if (rootDir != null) {
            File filePath = new File(rootDir, CONTEXT_FILE_OFFLINE);
            if (msg == null) {
                // remove offline (ie. back online)
                if (filePath.isFile()) {
                    try {
                        boolean didDelete = filePath.delete();
                        if (didDelete) {
                            Print.logInfo("Context 'offline' file deleted: " + filePath);
                        } else {
                            Print.logError("Unable to delete context 'offline' file: " + filePath);
                        }
                    } catch (Throwable th) { // SecurityException
                        Print.logException("Unable to delete 'offline' message file " + filePath, th);
                    }
                }
            } else {
                // set offline message
                try {
                    FileTools.writeFile(msg.getBytes(), filePath, false); // overwrite
                } catch (IOException ioe) {
                    Print.logException("Unable to create/write 'offline' message file " + filePath, ioe);
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the WebPage map
    *** @param pageMap The WebPage map
    **/
    public void setWebPageMap(Map<String,WebPage> pageMap)
    {
        this.pageMap = pageMap;
    }

    /**
    *** Gets the WebPage map
    *** @return pageMap The WebPage map
    **/
    public Map<String,WebPage> getWebPageMap()
    {
        return this.pageMap;
    }

    /**
    *** Gets the named WebPage
    *** @param pageName  The name of the WebPage to return
    *** @return The named WebPage, or null if the named page does not exist
    **/
    public WebPage getWebPage(String pageName)
    {
        Map<String,WebPage> map = this.getWebPageMap();
        return (map != null)? map.get(pageName) : null;
    }

    /**
    *** Returns true if the named WebPage is defined
    *** @param pageName  The name of the WebPage to test
    *** @return True if the named WebPage is defined
    **/
    public boolean hasWebPage(String pageName)
    {
        return (this.getWebPage(pageName) != null);
    }

    /**
    *** Gets the URL to the named WebPage
    *** @param pageName  The name of the WebPage
    *** @param reqState  The RequestProperties
    *** @return The URL to the named WebPage, or null if the named page does not exist
    **/
    public String getWebPageURL(RequestProperties reqState, String pageName)
    {
        WebPage wp = this.getWebPage(pageName);
        return (wp != null)? wp.encodePageURL(reqState) : "";
    }

    /**
    *** Gets the URL to the named WebPage
    *** @param pageName  The name of the WebPage
    *** @param reqState  The RequestProperties
    *** @param command   The command to append to the URL
    *** @return The URL to the named WebPage, or null if the named page does not exist
    **/
    public String getWebPageURL(RequestProperties reqState, String pageName, String command)
    {
        WebPage wp = this.getWebPage(pageName);
        return (wp != null)? wp.encodePageURL(reqState, command) : null;
    }

    /**
    *** Gets the URL to the named WebPage
    *** @param pageName  The name of the WebPage
    *** @param reqState  The RequestProperties
    *** @param command   The command to append to the URL
    *** @param arg       The command argument to append to the URL
    *** @return The URL to the named WebPage, or null if the named page does not exist
    **/
    public String getWebPageURL(RequestProperties reqState, String pageName, String command, String arg)
    {
        WebPage wp = this.getWebPage(pageName);
        return (wp != null)? wp.encodePageURL(reqState, command, arg) : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the MenuGroup map
    *** @param menuGroupMap  The MenuGroup map
    **/
    public void setMenuGroupMap(Map<String,MenuGroup> menuGroupMap)
    {
        this.menuGroupMap = menuGroupMap;
    }
    
    /**
    *** Gets the MenuGroup map
    *** @return The MenuGroup map
    **/
    public Map<String,MenuGroup> getMenuGroupMap()
    {
        return this.menuGroupMap;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the ReportEntry map
    *** @param reportMap  The ReportEntry map
    **/
    public void setReportMap(Map<String,ReportEntry> reportMap)
    {
        this.reportMap = reportMap;
    }
    
    /**
    *** Gets the ReportEntry map
    *** @return The ReportEntry map
    **/
    public Map<String,ReportEntry> getReportMap()
    {
        return this.reportMap;
    }
    
    /**
    *** Gets the named ReportFactory
    *** @param rptName  The ReportFactory name to return
    *** @return The named ReportFactory
    **/
    public ReportEntry getReportEntry(String rptName)
    {

        /* no report name */
        if (StringTools.isBlank(rptName)) {
            return null;
        }

        /* no ReportEntry map? */
        Map<String,ReportEntry> map = this.getReportMap();
        if (map == null) {
            return null;
        }

        /* get ReportEntry */
        String rn = rptName.trim();
        return map.containsKey(rn)? map.get(rn) : map.get(rn.toLowerCase());

    }

    /**
    *** Gets the named ReportFactory
    *** @param rptName  The ReportFactory name to return
    *** @return The named ReportFactory
    **/
    public ReportFactory getReportFactory(String rptName)
    {
        ReportEntry re = this.getReportEntry(rptName);
        return (re != null)? re.getReportFactory() : null;
    }

    /**
    *** Returns true if the named ReportFactory has been defined
    *** @param rptName  The ReportFactory name to test
    *** @return True if the named ReportFactory has been defined
    **/
    public boolean hasReport(String rptName)
    {
        return (this.getReportEntry(rptName) != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the JSPEntry map
    *** @param jspMap  The JSPEntry map
    **/
    public void setJSPMap(Map<String,JSPEntry> jspMap)
    {
        this.jspMap = jspMap;
    }

    /**
    *** Gets the JSPEntry map
    *** @return  The JSPEntry map
    **/
    private Map<String,JSPEntry> _getJSPMap()
    {
        return this.jspMap;
    }
    
    /**
    *** Gets a set of JSPEntry names
    *** @return A set of JSPEntry names
    **/
    public Set<String> getJSPEntryNames()
    {
        Map<String,JSPEntry> jspMap = this._getJSPMap();
        return (jspMap != null)? jspMap.keySet() : new HashSet<String>();
    }
    
    /**
    *** Gets a map of JSPEntry names/descriptions
    *** @return A set of JSPEntry names/descriptions
    **/
    public OrderedMap<String,String> getJSPEntryDescriptions(Locale locale)
    {
        OrderedMap<String,String> descMap = new OrderedMap<String,String>();
        Map<String,JSPEntry> jspMap = this._getJSPMap();
        if (jspMap != null) {
            // add "default" first
            String dftName = "default";
            if (jspMap.containsKey(dftName)) {
                String d = jspMap.get(dftName).getDescription(locale);
                descMap.put(dftName, d);
            }
            // add remaining
            for (String n : jspMap.keySet()) {
                if (!descMap.containsKey(n)) {
                    String d = jspMap.get(n).getDescription(locale);
                    descMap.put(n, d);
                }
            }
        }
        return descMap;
    }

    /**
    *** Gets the named JSPEntry. <br>
    *** Can be overidden with property "JSPEntry.<jspName>".
    *** If the "default" JSP Entry is preconfigured, then overiding with
    *** property key "JSPEntry.default" will allow mapping other JSP enties.
    *** @param jspName  The JSPEntry name to return
    *** @return The named JSPEntry
    **/
    public JSPEntry getJSPEntry(String jspName)
    {
        //Print.logInfo("Getting JSPEntry: " + jspName);
        Map<String,JSPEntry> map = this._getJSPMap();
        if ((map != null) && !StringTools.isBlank(jspName)) {

            /* check for override/redirect name */
            String jspPropKey = LAF_JSPEntry_ + jspName; // 
            String jspn = this.getStringProperty(jspPropKey, jspName); // redirect
            //Print.logInfo("Looking for redirect: " + jspPropKey + " ==> " + jspn);

            /* get JSPEntry */
            JSPEntry je = map.get(jspn);
            if ((je == null) && !jspn.equals(jspName)) {
                // not found, recheck original specified name
                je = map.get(jspName);
            }

            /* return */
            if (je != null) {
                //Print.logInfo("Returning JSPEntry: " + je.getName());
                return je;
            }

        }
        return null;
    }
    
    /**
    *** Gets the named JSPEntry
    *** @param jspName     The JSPEntry name to return
    *** @param rtnDefault  True to return "default" JSP if jsp name is not found
    *** @return The named JSPEntry
    **/
    public String getJSPFile(String jspName, boolean rtnDefault)
    {
        JSPEntry jsp = this.getJSPEntry(jspName);
        if ((jsp == null) && rtnDefault) {
            jsp = this.getJSPEntry(JSPENTRY_DEFAULT);
        }
        return (jsp != null)? jsp.getFile() : null;
    }
    
    // ------------------------------------------------------------------------

    private java.util.List<PoiProvider> pointsOfInterest = null;

    /**
    *** Sets the global PointsOfInterest
    *** @param poiList The PointsOfInterest list
    **/
    public void setPointsOfInterest(java.util.List<PoiProvider> poiList)
    {
        this.pointsOfInterest = poiList;
    }

    /**
    *** Gets the global PointsOfInterest
    *** @return The PointsOfInterest list
    **/
    public java.util.List<PoiProvider> getPointsOfInterest()
    {
        return this.pointsOfInterest;
    }
    
    // ------------------------------------------------------------------------

    private Map<String,MapShape> mapShapes = null;

    /**
    *** Sets the global MapShapes
    *** @param shapeList The MapShape list
    **/
    public void setMapShapes(java.util.List<MapShape> shapeList)
    {
        this.mapShapes = new OrderedMap<String,MapShape>();
        if (!ListTools.isEmpty(shapeList)) {
            for (MapShape ms : shapeList) {
                this.mapShapes.put(ms.getName(), ms);
            }
        }
    }

    /**
    *** Gets the global MapShapes
    *** @return The MapShapes list
    **/
    public Map<String,MapShape> getMapShapes()
    {
        return this.mapShapes;
    }

    // ------------------------------------------------------------------------

    /**
    *** Clears TimeZone cache
    **/
    public void clearTimeZones()
    {
        super.clearTimeZones();
        this.timeZonesMap = null;
    }

    /**
    *** Gets a ComboMap of supported TimeZones
    *** @return The ComboMap of supported TimeZones
    **/
    public ComboMap getTimeZoneComboMap()
    {
        if (this.timeZonesMap == null) {
            java.util.List<String> tzList = this.getTimeZonesList();
            synchronized (tzList) {
                if (this.timeZonesMap == null) {
                    // reconstruct TimeZone Map from TimeZone List
                    this.timeZonesMap = new ComboMap();
                    for (String tz : tzList) {
                        this.timeZonesMap.add(tz,tz);
                    }
                }
            }
        }
        return this.timeZonesMap;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a ComboMap of the enumnerated values specified by the Enum class
    *** @return The ComboMap
    **/
    public <T extends Enum<T>> ComboMap getEnumComboMap(Class<T> enumClass)
    {
        // TODO: can be optimized (since the Locale will not change for this PrivateLabel)
        return new ComboMap(EnumTools.getValueMap(enumClass, this.getLocale()));
    }

    /**
    *** Gets a ComboMap of the enumnerated values specified by the Enum class
    *** @return The ComboMap
    **/
    public <T extends Enum<T>> ComboMap getEnumComboMap(Class<T> enumClass, T list[])
    {
        return new ComboMap(EnumTools.getValueMap(enumClass, list, this.getLocale()));
    }

    /**
    *** Gets a ComboOption encapsulating the specified Enum type
    *** @return The ComboOption
    **/
    public ComboOption getEnumComboOption(EnumTools.StringLocale enumType) 
    {
        return new ComboOption(enumType, this.getLocale());
    }

}
