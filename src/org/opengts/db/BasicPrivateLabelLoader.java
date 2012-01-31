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
//  2007/09/16  Martin D. Flynn
//     -Extracted from 'PrivateLabel.java'.
//     -WAR specific properties moved to 'org.opengts.war.tools.PrivateLabelLoader'
//  2008/02/07  Martin D. Flynn
//     -Updated to comply with modified 'private.dtd'
//     -Update to support localizing text found in 'private.xml'
//  2008/02/21  Martin D. Flynn
//     -Check explicitly for 'Track' war servlet before attempting to use the
//      PrivateLabelLoader.  This eliminates the series of displayed warnings when
//      loading a non-Track servlet (such as 'mologogo.war' or 'events.war', etc);
//  2008/05/14  Martin D. Flynn
//     -Added 'Property' subtag to 'GeocodeProvider'/'ReverseGeocodeProvider' tags
//  2008/08/15  Martin D. Flynn
//     -Added ACL tag 'maximum' (was 'access') and 'default' attirbutes.
//  2008/08/24  Martin D. Flynn
//     -Added TAG_DefaultLoginUser
//  2008/12/01  Martin D. Flynn
//     -Moved Domain 'Property' tags into parent 'Properties'.
//  2009/01/28  Martin D. Flynn
//     -Moved file inclusion from "<Domain include...>" to "<Include file=...>".  
//      Property tags may be specified which can be evaluated within the included file.
//  2009/05/24  Martin D. Flynn
//     -Add I18N tag Strings to BasicPrivateLabel "setI18NTextProperty"
//  2009/07/01  Martin D. Flynn
//     -Removed PageDecoration tags
//  2009/08/23  Martin D. Flynn
//     -Added MapProvider "Legend" and "IconSelector" tags.
//     -Ignore certain tags if not loading within a 'track.war' environment.
//  2010/04/11  Martin D. Flynn
//     -Added support for hiding the "Password" field on the login page
//  2010/04/25  Martin D. Flynn
//     -On "Domain" tag, non-blank "name" attribute is now required.
//  2010/11/29  Martin D. Flynn
//     -Added domain name lookup when the URL contains a subdomain.
//  2011/07/01  Martin D. Flynn
//     -Added support for MobileLocationProvider
//  2011/08/21  Martin D. Flynn
//     -Added ATTR_iconHotspot to replace ATTR_iconOffset
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

import org.w3c.dom.*;

import org.opengts.util.*;

import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.geocoder.*;
import org.opengts.cellid.*;

public class BasicPrivateLabelLoader
{
    
    // ------------------------------------------------------------------------

    /* allow including other Domain xml files */
    public  static boolean ALLOW_DOMAIN_INCLUDE                 = true;
    private static int     DomainIncludeRecursionLevel          = 0;
    
    /* always attempt to load PrivateLabelLoader before reverting to default BasicPrivateLabelLoader */
    public  static boolean ALWAYS_LOAD_WAR_PRIVATELABEL         = true;
    
    // ------------------------------------------------------------------------
    
    public  static final String CLASS_Track                     = DBConfig.PACKAGE_WAR_ + "track.Track";
    public  static final String CLASS_Service                   = DBConfig.PACKAGE_EXTRA_WAR_ + "service.Service";
    public  static final String CLASS_Celltrac                  = DBConfig.PACKAGE_OPT_WAR_ + "celltrac.Celltrac";

    public  static final String CLASS_PrivateLabelLoader        = DBConfig.PACKAGE_WAR_ + "tools.PrivateLabelLoader";

    // ------------------------------------------------------------------------
    
    public  static final String PRIVATE_LABEL_XML               = "private.xml";

    // ------------------------------------------------------------------------

    public  static final String TAG_LogMessage                  = "LogMessage";

    public  static final String TAG_PrivateLabels               = "PrivateLabels";

    public  static final String TAG_SupportedLocales            = "SupportedLocales";
    public  static final String TAG_Locale                      = "Locale";
    public  static final String TAG_TimeZones                   = "TimeZones";
    public  static final String TAG_Domain                      = "Domain";
    public  static final String TAG_Include                     = "Include";
    public  static final String TAG_BaseURL                     = "BaseURL";

    public  static final String TAG_Alias                       = "Alias";
    public  static final String TAG_DefaultLoginAccount         = "DefaultLoginAccount";
    public  static final String TAG_DefaultLoginUser            = "DefaultLoginUser";
    public  static final String TAG_PageTitle                   = "PageTitle";              // i18n
    public  static final String TAG_Copyright                   = "Copyright";
    public  static final String TAG_DateFormat                  = "DateFormat";
    public  static final String TAG_TimeFormat                  = "TimeFormat";
    public  static final String TAG_MapProvider                 = "MapProvider";
    public  static final String TAG_Legend                      = "Legend";
    public  static final String TAG_IconSelector                = "IconSelector";

    public  static final String TAG_ReverseGeocodeProvider      = "ReverseGeocodeProvider";
    public  static final String TAG_GeocodeProvider             = "GeocodeProvider";
    public  static final String TAG_MobileLocationProvider      = "MobileLocationProvider";

    public  static final String TAG_I18N                        = "I18N";
    public  static final String TAG_String                      = "String";

    public  static final String TAG_Properties                  = "Properties";
    public  static final String TAG_PropertyGroup               = "PropertyGroup";
    public  static final String TAG_Property                    = "Property";

    public  static final String TAG_Pushpins                    = "Pushpins";
    public  static final String TAG_Pushpin                     = "Pushpin";

    public  static final String TAG_EMailAddresses              = "EMailAddresses";
    public  static final String TAG_EMailAddress                = "EMailAddress";

    public  static final String TAG_StatusCodes                 = "StatusCodes";
    public  static final String TAG_StatusCode                  = "StatusCode";

    public  static final String TAG_Acls                        = "Acls";
    public  static final String TAG_Acl                         = "Acl";                    // i18n
    
    public  static final String TAG_JSPEntries                  = "JSPEntries";
    public  static final String TAG_JSP                         = "JSP";

    public  static final String TAG_WebPages                    = "WebPages";
    public  static final String TAG_MenuGroup                   = "MenuGroup";
    public  static final String TAG_Title                       = "Title";                  // i18n
    public  static final String TAG_Description                 = "Description";            // i18n
    public  static final String TAG_Page                        = "Page";
    public  static final String TAG_Link                        = "Link";
    
    public  static final String TAG_NavigationDescription       = "NavigationDescription";
    public  static final String TAG_NavigationTab               = "NavigationTab";
    public  static final String TAG_MenuDescription             = "MenuDescription";
    public  static final String TAG_MenuHelp                    = "MenuHelp";
    public  static final String TAG_IconImage                   = "IconImage";
    public  static final String TAG_ButtonImage                 = "ButtonImage";
    public  static final String TAG_ButtonImageAlt              = "ButtonImageAlt";
    public  static final String TAG_AclName                     = "AclName";

    public  static final String TAG_Reports                     = "Reports";
    public  static final String TAG_Report                      = "Report";
    public  static final String TAG_Options                     = "Options";
    public  static final String TAG_Select                      = "Select";

    public  static final String TAG_EventNotificationEMail      = "EventNotificationEMail";
    public  static final String TAG_Subject                     = "Subject";                // i18n
    public  static final String TAG_Body                        = "Body";                   // i18n
    
    public  static final String TAG_PointsOfInterest            = "PointsOfInterest";
    public  static final String TAG_POI                         = "POI";
    
    public  static final String TAG_MapShapes                   = "MapShapes";
    public  static final String TAG_Shape                       = "Shape";
    public  static final String TAG_Points                      = "Points";

    // -----------------

    public  static final String ATTR_i18nPackage                = "i18nPackage";
    public  static final String ATTR_enabled                    = "enabled";
    public  static final String ATTR_dir                        = "dir";
    public  static final String ATTR_altDir                     = "altDir";
    public  static final String ATTR_file                       = "file";
    public  static final String ATTR_host                       = "host";
    public  static final String ATTR_restricted                 = "restricted";
    public  static final String ATTR_allowLogin                 = "allowLogin";
    public  static final String ATTR_accountLogin               = "accountLogin";           // true
    public  static final String ATTR_userLogin                  = "userLogin";              // true
    public  static final String ATTR_emailLogin                 = "emailLogin";             // false
    public  static final String ATTR_showPassword               = "showPassword";           // true
    public  static final String ATTR_class                      = "class";
    public  static final String ATTR_jsp                        = "jsp";
    public  static final String ATTR_jspFile                    = "jspFile";
    public  static final String ATTR_jspName                    = "jspName";
    public  static final String ATTR_cssDir                     = "cssDir";
    public  static final String ATTR_iconDir                    = "iconDir";
    public  static final String ATTR_buttonDir                  = "buttonDir";
    public  static final String ATTR_url                        = "url";
    public  static final String ATTR_target                     = "target";
    public  static final String ATTR_demo                       = "demo";
    public  static final String ATTR_id                         = "id";
    public  static final String ATTR_name                       = "name";
    public  static final String ATTR_code                       = "code";
    public  static final String ATTR_clear                      = "clear";      // "clearFirst", "clearBefore"
    public  static final String ATTR_iconName                   = "iconName";
    public  static final String ATTR_domainName                 = "domainName";
    public  static final String ATTR_access                     = "access";
    public  static final String ATTR_default                    = "default";
    public  static final String ATTR_maximum                    = "maximum";
    public  static final String ATTR_values                     = "values";
    public  static final String ATTR_value                      = "value";
    public  static final String ATTR_hidden                     = "hidden";
    public  static final String ATTR_aclName                    = "aclName";
    public  static final String ATTR_optional                   = "optional";
    public  static final String ATTR_ignoreDuplicates           = "ignoreDuplicates";
    public  static final String ATTR_navigation                 = "navigation";
    public  static final String ATTR_description                = "description";
    public  static final String ATTR_desc                       = "desc";
    public  static final String ATTR_help                       = "help";
    public  static final String ATTR_sort                       = "sort";
    public  static final String ATTR_type                       = "type";
    public  static final String ATTR_keyPrefix                  = "keyPrefix";
    public  static final String ATTR_key                        = "key";
    public  static final String ATTR_rtPropPrefix               = "rtPropPrefix";
    public  static final String ATTR_rtKey                      = "rtKey";
    public  static final String ATTR_loggedIn                   = "loggedIn";
    public  static final String ATTR_locale                     = "locale";
    public  static final String ATTR_from                       = "from";
    public  static final String ATTR_useAsDefault               = "useAsDefault";
    public  static final String ATTR_i18n                       = "i18n";
    public  static final String ATTR_active                     = "active";
    public  static final String ATTR_menuBar                    = "menuBar";
    public  static final String ATTR_topMenu                    = "topMenu";
    public  static final String ATTR_baseURL                    = "baseURL";
    public  static final String ATTR_only                       = "only";
    public  static final String ATTR_sysAdminOnly               = "sysAdminOnly"; // Report
    public  static final String ATTR_geocode                    = "geocode";

    public  static final String ATTR_icon                       = "icon";
    public  static final String ATTR_button                     = "button";
    public  static final String ATTR_altButton                  = "altButton";
    public  static final String ATTR_image                      = "image";
    public  static final String ATTR_eval                       = "eval";
    public  static final String ATTR_alias                      = "alias";
    public  static final String ATTR_iconSize                   = "iconSize";
    public  static final String ATTR_iconHotspot                = "iconHotspot";
    public  static final String ATTR_iconAnchor                 = "iconAnchor";  // same as iconHotspot
    public  static final String ATTR_iconOffset                 = "iconOffset";  // same as iconHotspot
    public  static final String ATTR_shadow                     = "shadow";
    public  static final String ATTR_shadowSize                 = "shadowSize";
    public  static final String ATTR_back                       = "back";
    public  static final String ATTR_backSize                   = "backSize";
    public  static final String ATTR_backOffset                 = "backOffset";

    public  static final String ATTR_ruleFactoryName            = "ruleFactoryName";
    
    public  static final String ATTR_radius                     = "radius";
    public  static final String ATTR_color                      = "color";
    public  static final String ATTR_zoom                       = "zoom";
    
    public  static final String ATTR_includeDefault             = "includeDefault";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static OutputHandler outputHandler = null;

    /**
    *** OutputHandler interface
    **/
    public interface OutputHandler
    {
        public void privateLabelOutput(String m);
    }
    
    /**
    *** Sets the output delegate 
    **/
    public static void setOutputHandler(OutputHandler output)
    {
        BasicPrivateLabelLoader.outputHandler = output;
    }

    /**
    *** Print message to output handler
    *** @param h Header type
    *** @param msg  Message to display
    *** @param args Message arguments (may be null
    **/
    private static void _printOutputHandler(String h, String msg, Object... args)
    {
        StringBuffer logMsg = new StringBuffer();
        if (h != null) {
            logMsg.append("[").append(h).append("] ");
        }
        if (msg != null) {
            if ((args != null) && (args.length > 0)) {
                try {
                    logMsg.append(String.format(msg,args));
                } catch (Throwable th) { 
                    // MissingFormatArgumentException, UnknownFormatConversionException
                    System.out.println("ERROR: [" + msg + "] " + th); // [OUTPUT]
                    logMsg.append(msg);
                }
            } else {
                logMsg.append(msg);
            }
            if (!msg.endsWith("\n")) { logMsg.append("\n"); }
        } else {
            logMsg.append("\n");
        }
        if (outputHandler != null) {
            outputHandler.privateLabelOutput(logMsg.toString());
        } else {
            Print.sysPrintln(logMsg.toString());
        }
    }

    /** 
    *** Display error messages
    **/
    protected static void printError(String msg, Object... args)
    {
        if (outputHandler != null) {
            if (Print.getLogLevel() >= Print.LOG_ERROR) {
                _printOutputHandler("ERROR", msg, args);
            }
        } else {
            Print.logError(msg, args);
        }
    }

    /** 
    *** Display error messages
    **/
    protected static void printWarn(String msg, Object... args)
    {
        if (outputHandler != null) {
            if (Print.getLogLevel() >= Print.LOG_WARN) {
                _printOutputHandler("WARN", msg, args);
            }
        } else {
            Print.logWarn(msg, args);
        }
    }

    /** 
    *** Display error messages
    **/
    protected static void printInfo(String msg, Object... args)
    {
        if (outputHandler != null) {
            if (Print.getLogLevel() >= Print.LOG_INFO) {
                _printOutputHandler("INFO", msg, args);
            }
        } else {
            Print.logInfo(msg, args);
        }
    }

    /** 
    *** Display error messages
    **/
    protected static void printDebug(String msg, Object... args)
    {
        if (outputHandler != null) {
            if (Print.getLogLevel() >= Print.LOG_DEBUG) {
                _printOutputHandler("DEBUG", msg, args);
            }
        } else {
            Print.logDebug(msg, args);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    protected static boolean    _isTrackServlet = false;

    public static void setTrackServlet_debugOnly()
    {
        BasicPrivateLabelLoader.setTrackServlet();
    }

    private static void setTrackServlet()
    {
        BasicPrivateLabelLoader._isTrackServlet = true;
        BasicPrivateLabel.setTrackServlet_loaderOnly();
    }

    public static boolean isTrackServlet()
    {
        return BasicPrivateLabelLoader._isTrackServlet;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static BasicPrivateLabelLoader    privateLabelLoader = null;

    /**
    *** Returns a singleton instance of BasicPrivateLabelLoader
    *** @return A singleton instance of BasicPrivateLabelLoader
    **/
    protected static BasicPrivateLabelLoader _getInstance()
    {
        if (BasicPrivateLabelLoader.privateLabelLoader == null) {

            /* check for 'Track' servlet */
            if (!BasicPrivateLabelLoader.isTrackServlet()) {
                if (!RTConfig.isWebApp()) {
                    //BasicPrivateLabelLoader.isTrackServlet = false;   <== already false
                } else {
                    String cn[] = new String[] { CLASS_Track, CLASS_Service, CLASS_Celltrac };
                    for (int i = 0; i < cn.length; i++) {
                        try {
                            //Print.logInfo("Check for class: " + cn[i]);
                            Class.forName(cn[i]);
                            BasicPrivateLabelLoader.setTrackServlet(); // true
                            break;
                        } catch (Throwable th1) {
                            // try again
                            //Print.logInfo("Class not found: " + cn[i]);
                        }
                    }
                    if (!BasicPrivateLabelLoader.isTrackServlet()) {
                        printInfo("Not a 'Track/Service' servlet");
                    }
                }
            }

            /* attempt to use PrivateLabelLoader for the 'Track'/'Service' servlet */
            if (ALWAYS_LOAD_WAR_PRIVATELABEL || BasicPrivateLabelLoader.isTrackServlet()) {
                try {
                    Class pllClass = Class.forName(CLASS_PrivateLabelLoader);
                    BasicPrivateLabelLoader.privateLabelLoader = (BasicPrivateLabelLoader)pllClass.newInstance();
                } catch (Throwable th) { // ClassNotFoundException, InstantiationException, IllegalAccessException
                    if (BasicPrivateLabelLoader.isTrackServlet()) {
                        printError("PrivateLabelLoader not found, using BasicPrivateLabelLoader");
                        Print.logException(CLASS_PrivateLabelLoader+" not found, using BasicPrivateLabelLoader", th);
                    } else {
                        printDebug("Loading default BasicPrivateLabelLoader");
                    }
                    BasicPrivateLabelLoader.privateLabelLoader = new BasicPrivateLabelLoader();
                }
            } else {
                BasicPrivateLabelLoader.privateLabelLoader = new BasicPrivateLabelLoader();
            }
            printDebug("PrivateLabelLoader class: " + StringTools.className(BasicPrivateLabelLoader.privateLabelLoader));

        }
        return BasicPrivateLabelLoader.privateLabelLoader;
    }

    /**
    *** Returns the BasicPrivateLabelLoader class (may be a subclass)
    *** @return The BasicPrivateLabelLoader class (may be a subclass)
    **/
    public static Class getInstanceClass()
    {
        return _getInstance().getClass();
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns the the 'private.xml' File (from which the BasicPrivateLabel will be loaded)
    *** @return The 'private.xml' File
    **/
    public static File getPrivateXMLFile()
    {
        return _getInstance()._getPrivateXMLFile();
    }

    /**
    *** Loads the 'private.xml' file
    *** @return The number of 'Domain' tags found in the 'private.xml' file
    **/
    public static int loadPrivateLabelXML()
    {
        // returns number of domains parsed
        return _getInstance()._resetLoadDefaultXML();
    }

    /**
    *** Loads the 'private.xml' file
    *** @return The number of 'Domain' tags found in the 'private.xml' file
    **/
    public static int loadPrivateLabelXML(File xmlFile)
    {
        // returns number of domains parsed
        return _getInstance()._resetLoadXML(xmlFile);
    }

    // ------------------------------------------------------------------------

    private static ThreadLocal<BasicPrivateLabel> threadPrivateLabel = new ThreadLocal<BasicPrivateLabel>();
    
    /**
    *** Assigns the specified BasicPrivateLabel instance to the current Thread
    *** @param bpl  The BasicPrivateLabel instance to assign to the current Thread
    **/
    public static void setThreadPrivateLabel(BasicPrivateLabel bpl)
    {
        threadPrivateLabel.set(bpl);
    }
    
    /**
    *** Gets the current BasicPrivateLabel (or subclass) instance assigned to the current Thread
    *** @return The current BasicPrivateLabel instance assigned to the current Thread
    **/
    public static BasicPrivateLabel getThreadPrivateLabel()
    {
        Object bpl = threadPrivateLabel.get();
        return (bpl instanceof BasicPrivateLabel)? (BasicPrivateLabel)bpl : null;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns true if active ('active' is true, or equals 'name')
    *** @param active  'active' attribute
    *** @param name    'name' attribute
    *** @return True if active
    **/
    protected static boolean _isAttributeActive(String active, String name)
    {
        if (StringTools.isBlank(active)) {
            // blank is false
            return false;
        } else
        if (StringTools.isBoolean(active,true)) {
            // explicit boolean value
            return StringTools.parseBoolean(active,false);
        } else {
            // equals 'name' attribute
            return active.equalsIgnoreCase(name);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private HashMap<String,BasicPrivateLabel>   privateLabelMap         = null;
    private BasicPrivateLabel                   defaultPrivateLabel     = null; // new BasicPrivateLabel(DEFAULT_HOST);
    private boolean                             hasParsingWarnings      = false;
    private boolean                             hasParsingErrors        = false;

    /**
    *** Constructor
    **/
    public BasicPrivateLabelLoader()
    {
        super();
    }

    /**
    *** Creates a new BasicPrivalLabel instance.  Subclasses must override this method to return
    *** their own instance of the BasicPrivalLabel subclass.
    *** @param hostName  The host name assigned to the BasicPrivateLabel instance
    *** @return The BasicPrivateLabel instance.
    **/
    protected BasicPrivateLabel createPrivateLabel(File xmlFile, String hostName)
    {
        return new BasicPrivateLabel(hostName);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 'private.xml' file which will be loaded
    *** @return The 'private.xml' file to load
    **/
    protected File _getPrivateXMLFile()
    {
        File cfgFile = RTConfig.getLoadedConfigFile();
        if (cfgFile == null) {
            printError("Unable to locate parent config file directory");
            this._setHasParsingErrors(cfgFile);
            return null;
        }
        File xmlFile = new File(cfgFile.getParentFile(), PRIVATE_LABEL_XML);
        return xmlFile;
    }
    
    /**
    *** Loads/Reloads the 'private.xml' file
    *** @return The number of domains loaded
    **/
    protected int _resetLoadDefaultXML()
    {

        /* get location of 'private.xml' file */
        File xmlFile = this._getPrivateXMLFile();

        /* load */
        return this._resetLoadXML(xmlFile);

    }

    /**
    *** Loads/Reloads the specified XML file.
    *** @param xmlFile  The XML file to load
    *** @return The number of domains loaded
    **/
    protected int _resetLoadXML(File xmlFile)
    {

        /* reset existing map */
        boolean isReload = (this.privateLabelMap != null);
        this.privateLabelMap  = null;
        this.defaultPrivateLabel = null;
        this.hasParsingErrors = false;

        /* override properties */
        RTProperties dftProps = null;
        RTProperties ovrProps = new RTProperties();
        //ovrProps.printProperties("Config file props");

        /* load XML file */
        //printDebug("Reloading ["+StringTools.className(this)+"]");
        int count = this._loadXML(xmlFile, 
            dftProps, ovrProps, 
            null/*dftPushpinMap*/, null/*dftLegend*/, 
            false/*ignoreDuplicates*/);
        if (count == 0) {
            printError("Error loading: " + xmlFile);
            this._setHasParsingErrors(xmlFile);
        } else
        if (this.defaultPrivateLabel == null) {
            printWarn("No default Domain has been defined (ie. host=\""+BasicPrivateLabel.DEFAULT_HOST+"\"): " + xmlFile);
            this._setHasParsingWarnings(xmlFile);
        } else
        if (isReload) {
            printDebug("Reloaded: " + xmlFile);
        } else {
            printDebug("Loaded: " + xmlFile);
        }
        return count;

    }

    /**
    *** Loads/Reloads the specified XML file.  The list of domains is NOT reset.
    *** @param xmlFile  The XML file to load
    *** @return The number of domains loaded
    **/
    protected int _loadXML(File xmlFile, 
        RTProperties dftProps, RTProperties ovrProps, 
        OrderedMap<String,Object> dftPushpinMap,
        OrderedMap<String,String> dftLegend,
        boolean ignoreDuplicates)
    {
        printDebug("Loading PrivateLabel xml file: " + xmlFile);

        /* get XML document */
        Document xmlDoc = (xmlFile != null)? XMLTools.getDocument(xmlFile) : null;
        if (xmlDoc == null) {
            printError("Unable to create XML Document from file: " + xmlFile);
            this._setHasParsingErrors(xmlFile);
            return 0;
        }

        /* get top-level tag */
        Element privLabels = xmlDoc.getDocumentElement();
        if (!privLabels.getTagName().equalsIgnoreCase(TAG_PrivateLabels)) {
            printError("Missing '"+TAG_PrivateLabels+"' tag");
            this._setHasParsingErrors(xmlFile);
            return 0;
        }

        /* I18N package name */
        String i18nPkgName = XMLTools.getAttribute(privLabels,ATTR_i18nPackage,null,false);
        if (StringTools.isBlank(i18nPkgName)) {
            i18nPkgName = BasicPrivateLabelLoader.class.getPackage().getName();
        }

        /* SupportedLocales: parse default <SupportedLocales> (if present) */
        OrderedMap<String,I18N.Text> locales = this.parseTag_SupportedLocales(xmlFile,i18nPkgName,privLabels);
        if (!ListTools.isEmpty(locales)) {
            BasicPrivateLabel.SetSupportedLocalesMap(locales);
        }

        /* Timezones: parse default <TimeZones> (if present) */
        OrderedSet<String> timeZones = this.parseTag_Timezones(privLabels);
        if (timeZones == null) { 
            timeZones = new OrderedSet<String>(); 
        }

        /* Properties: parse default/override <Properties> (if present) */
        RTProperties globalProps = new RTProperties(dftProps);
        NodeList propsNodes = XMLTools.getChildElements(privLabels,TAG_Properties);
        for (int p = 0; p < propsNodes.getLength(); p++) {
            Element props = (Element)propsNodes.item(p);
            this.parseTag_Properties(xmlFile, globalProps, props, 
                null/*keyPrefix*/, null/*rtPrefix*/, true); // allow default RTConfig override
            // ".conf" property definitions should override the properties specified here
        }
        globalProps.setProperties(ovrProps);
        globalProps.removeProperties(RTConfig.getConfigFileProperties());
        //globalProps.printProperties("Global Properties");

        /* PushPins: parse default <PushPins> (if present) */
        boolean isGlobalPushpins = (dftPushpinMap == null);
        dftPushpinMap = new OrderedMap<String,Object>(dftPushpinMap);
        NodeList ppNodes = XMLTools.getChildElements(privLabels,TAG_Pushpins);
        for (int p = 0; p < ppNodes.getLength(); p++) {
            Element ppn = (Element)ppNodes.item(p);
            OrderedMap<String,Object> ppMap = this.parseTAG_Pushpins(xmlFile, null, ppn, null, isGlobalPushpins);
            if (!ListTools.isEmpty(ppMap)) {
                dftPushpinMap.putAll(ppMap);
            }
        }

        /* IconSelector [device|fleet]: parse default <IconSelector> (if present) */
        if (BasicPrivateLabelLoader.isTrackServlet()) {
            NodeList iconSelNodes = XMLTools.getChildElements(privLabels,TAG_IconSelector);
            for (int n = 0; n < iconSelNodes.getLength(); n++) {
                Element isn = (Element)iconSelNodes.item(n);
                // 
            }
        }

        /* Legend [device|fleet]: parse default <Legend> (if present) */
        // not advised at global level because the Domain 'Locale' is not available at this point
        dftLegend = new OrderedMap<String,String>(dftLegend);
        if (BasicPrivateLabelLoader.isTrackServlet()) {
            NodeList legendNodes = XMLTools.getChildElements(privLabels, TAG_Legend);
            for (int n = 0; n < legendNodes.getLength(); n++) {
                Element attrElem = (Element)legendNodes.item(n);
                String  legType  = XMLTools.getAttribute(attrElem,ATTR_type,"",false);
                String  legend   = StringTools.replace(XMLTools.getNodeText(attrElem,"\n",true),"\\n","\n").trim();
                if (!StringTools.isBlank(legend)) {
                    // explicit HTML specified
                    dftLegend.put(legType, legend);
                } else {
                    String refName = "<Global>" + xmlFile.getName();
                    legend = this.parseLegendHTML(refName, null/*Locale*/, dftPushpinMap, legType, attrElem);
                    if (!StringTools.isBlank(legend)) {
                        dftLegend.put(legType, legend);
                    } else {
                        legend = this.parseLegendHTML(refName, null/*Locale*/, dftPushpinMap, legType, null);
                        if (!StringTools.isBlank(legend)) {
                            dftLegend.put(legType, legend);
                        }
                    }
                }
                //Print.logInfo("Default Legend: "+legType+"\n" + legend);
            }
        }

        /* domain count */
        int count = 0;

        /* Domain: parse specific domains */
        NodeList domainList = XMLTools.getChildElements(privLabels,TAG_Domain);
        for (int d = 0; d < domainList.getLength(); d++) {
            Element domain = (Element)domainList.item(d);

            /* parse 'Domain' tag */
            this.parseTag_Domain(xmlFile, i18nPkgName, domain, 
                null/*backstopProps*/, globalProps/*overrideProps*/, 
                timeZones, 
                dftPushpinMap,
                dftLegend,
                ignoreDuplicates);

            /* count domains */
            count++;

        }

        /* Include: parse includes */
        //if (ALLOW_DOMAIN_INCLUDE)
        NodeList includeList = XMLTools.getChildElements(privLabels,TAG_Include);
        if ((includeList.getLength() > 0) && (DomainIncludeRecursionLevel > 0)) {
            printError("Included files cannot contain an 'Include' tag: " + xmlFile);
            this._setHasParsingErrors(xmlFile);
        } else {
            DomainIncludeRecursionLevel++;
            for (int i = 0; i < includeList.getLength(); i++) {
                Element include  = (Element)includeList.item(i);
                boolean optional = XMLTools.getAttributeBoolean(include,ATTR_optional,false,false);
                boolean ignDups  = ignoreDuplicates || XMLTools.getAttributeBoolean(include,ATTR_ignoreDuplicates,false,false);

                /* include file */
                String includeFile = XMLTools.getAttribute(include,ATTR_file,null,false);
                if (StringTools.isBlank(includeFile)) {
                    printError("Include 'file' not specified: " + xmlFile);
                    this._setHasParsingErrors(xmlFile);
                    continue;
                }

                /* include only default domain? */
                if (!ALLOW_DOMAIN_INCLUDE && !includeFile.equals("private_release.xml")) {
                    printInfo("Skipping include: " + includeFile);
                    continue;
                }
            
                /* include properties */
                RTProperties inclProps = new RTProperties(globalProps);
                NodeList propList = include.getChildNodes();
                for (int e = 0; e < propList.getLength(); e++) {
                    Node propNode = propList.item(e);
                    if (!(propNode instanceof Element)) {
                        continue;
                    }
                    Element propElem = (Element)propNode;
                    String propName = propNode.getNodeName();
                    if (propName.equalsIgnoreCase(TAG_Property)) { // "Include" sub
                        String key = XMLTools.getAttribute(propElem,ATTR_key,null,false); // property key
                        if (!StringTools.isBlank(key)) {
                            inclProps.setProperty(key, XMLTools.getNodeText(propElem,"\\n",true));
                        } else {
                            printWarn("Undefined property key ignored.");
                            this._setHasParsingWarnings(xmlFile);
                        }
                    } else
                    if (propName.equalsIgnoreCase(TAG_LogMessage)) {
                        this.parseTag_LogMessage(xmlFile, null, propElem);
                    } else {
                        printError("Invalid tag name: " + propName + " [expecting "+TAG_Property+"]");
                        this._setHasParsingErrors(xmlFile);
                    }
                }
                inclProps.setProperties(ovrProps); // apply override properties again
                inclProps.removeProperties(RTConfig.getConfigFileProperties());

                /* include dir */
                String includeDir = XMLTools.getAttribute(include,ATTR_dir,null,false);
                File inclDir = !StringTools.isBlank(includeDir)? new File(includeDir) : null;

                /* alternate include dir */
                String altIncludeDir = XMLTools.getAttribute(include,ATTR_altDir,null,false);
                File altInclDir = !StringTools.isBlank(altIncludeDir)? new File(altIncludeDir) : null;

                /* XML parent dir */
                File parentDir = xmlFile.getParentFile();
                if (parentDir != null) {
                    try {
                        File dir = parentDir.getCanonicalFile();
                        parentDir = dir;
                    } catch (Throwable th) {
                        // 
                    }
                }
    
                /* locate file */
                java.util.List<String> filesChecked = new Vector<String>();
                File inclFile = null;
                // 1) <XMLParentDir>/<IncludeDir>/<IncludeFile>
                if ((inclFile == null) && (parentDir != null) && (inclDir != null)) {
                    File dir  = new File(parentDir, inclDir.toString());
                    File file = new File(dir, includeFile);
                    filesChecked.add(file.toString());
                    if (file.isFile()) {
                        inclFile = file;
                        if (RTConfig.isWebApp()) {
                            Print.logDebug("[Relative] Include: " + inclFile);
                        }
                    }
                }
                // 2) <XMLParentDir>/<AltIncludeDir>/<IncludeFile>
                if ((inclFile == null) && (parentDir != null) && (altInclDir != null)) {
                    File dir  = new File(parentDir, altInclDir.toString());
                    File file = new File(dir, includeFile);
                    filesChecked.add(file.toString());
                    if (file.isFile()) {
                        inclFile = file;
                        if (RTConfig.isWebApp()) {
                            Print.logDebug("[Relative] Include: " + inclFile);
                        }
                    }
                }
                // 3) <XMLParentDir>/<IncludeFile>
                if ((inclFile == null) && (parentDir != null)) {
                    File file = new File(parentDir, includeFile);
                    filesChecked.add(file.toString());
                    if (file.isFile()) {
                        inclFile = file;
                        if (RTConfig.isWebApp()) {
                            Print.logDebug("[Relative] Include: " + inclFile);
                        }
                    }
                }
                // 4) <AbsoluteIncludeDir>/<IncludeFile> (absolute dir/file specification)
                if ((inclFile == null) && (inclDir != null)) {
                    File file = new File(inclDir, includeFile);
                    filesChecked.add(file.toString());
                    if (file.isFile()) {
                        inclFile = file;
                        if (RTConfig.isWebApp()) {
                            Print.logDebug("[Absolute] Include: " + inclFile);
                        }
                    }
                }
                // 5) <AbsoluteIncludeDir>/<IncludeFile> (absolute dir/file specification)
                if ((inclFile == null) && (altInclDir != null)) {
                    File file = new File(altInclDir, includeFile);
                    filesChecked.add(file.toString());
                    if (file.isFile()) {
                        inclFile = file;
                        if (RTConfig.isWebApp()) {
                            Print.logDebug("[Absolute] Include: " + inclFile);
                        }
                    }
                }
                // 6) <AbsoluteIncludeFile> as-is (absolute file specification)
                if ((inclFile == null) && (parentDir != null)) {
                    File file = new File(includeFile);
                    filesChecked.add(file.toString());
                    if (file.isFile()) {
                        inclFile = file;
                        if (RTConfig.isWebApp()) {
                            Print.logDebug("[Absolute] Include: " + inclFile);
                        }
                    }
                }
   
                /* Include */
                if ((inclFile != null) && inclFile.isFile()) {
                    try {
                        String inclFilePath = inclFile.getCanonicalPath();
                        if (inclFilePath.equals(xmlFile.getCanonicalPath())) {
                            printWarn("Recursive Include ignored: " + inclFile.getCanonicalPath());
                            this._setHasParsingWarnings(xmlFile);
                        } else {
                            //inclProps.printProperties("Include Props for file: " + inclFile);
                            int cnt = this._loadXML(inclFile,
                                null/*dftProps*/, inclProps/*ovrProps*/, 
                                dftPushpinMap, dftLegend, 
                                ignDups); // recursive load
                            count += cnt;
                        }
                    } catch (Throwable th) {
                        printError("Error while including file: " + inclFile);
                        Print.logException("Error while including file: " + inclFile, th);
                    }
                } else {
                    if (!optional) {
                        printWarn("Include file not found: " + includeFile);
                        this._setHasParsingWarnings(xmlFile);
                        //for (String file : filesChecked) {
                        //    printWarn("Domain include file does not exist: " + file);
                        //}
                    }
                }
    
            }
            DomainIncludeRecursionLevel--;
        }

        return count;

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'SupportedLocales' tag
    *** @param elemNode  The 'SupportedLocales' parent node
    *** @return An OrderSet of parsed SupportedLocales
    **/
    protected OrderedMap<String,I18N.Text> parseTag_SupportedLocales(
        File xmlFile, String i18nPkgName, 
        Element elemNode)
    {
        //Print.logInfo("Parsing SupportedLocales ...");
        OrderedMap<String,I18N.Text> supportedLocales = null;
        NodeList supportedLocalesNodes = XMLTools.getChildElements(elemNode,TAG_SupportedLocales);
        for (int sl = 0; sl < supportedLocalesNodes.getLength(); sl++) {
            Element  slTag = (Element)supportedLocalesNodes.item(sl);
            NodeList localeNodes = XMLTools.getChildElements(slTag, TAG_Locale);
            for (int lo = 0; lo < localeNodes.getLength(); lo++) {
                Element   locTag  = (Element)localeNodes.item(lo);
                String    locID   = XMLTools.getAttribute(locTag, ATTR_id  , null, false);
                String    i18nKey = XMLTools.getAttribute(locTag, ATTR_i18n, null, false);
                String    locName = StringTools.trim(XMLTools.getNodeText(locTag, " ", false));
                I18N.Text locText = BasicPrivateLabelLoader.parseI18N(xmlFile, i18nPkgName, i18nKey, locName);
                if (supportedLocales == null) { supportedLocales = new OrderedMap<String,I18N.Text>(); }
                supportedLocales.put(locID, locText);
                //Print.logInfo("Added Locale: " + locID + " - " + locName);
            }
        }
        return supportedLocales;
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'Timezones' tag
    *** @param elemNode  The 'Timezones' parent node
    *** @return An OrderSet of parsed Timezones
    **/
    protected OrderedSet<String> parseTag_Timezones(Element elemNode)
    {
        OrderedSet<String> timeZones = null;
        NodeList timeZonesNodes = XMLTools.getChildElements(elemNode,TAG_TimeZones);
        for (int tzl = 0; tzl < timeZonesNodes.getLength(); tzl++) {
            Element tmzsTag = (Element)timeZonesNodes.item(tzl);
            String timeZoneIDs = XMLTools.getNodeText(tmzsTag,null,false);
            String tmz[] = StringTools.parseString(timeZoneIDs, " \t\r\n");
            for (int i = 0; i < tmz.length; i++) {
                String t = tmz[i].trim();
                if (!t.equals("")) {
                    if (timeZones == null) {
                        timeZones = new OrderedSet<String>();
                    }
                    timeZones.add(t);
                }
            }
        }
        return timeZones;
    }

    /**
    *** Parse 'Domain' tag
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param domain       The 'Domain' node
    *** @param timeZones    The set of previously parse Timezones
    **/
    protected void parseTag_Domain(
        File xmlFile, String i18nPkgName, 
        Element domain, 
        RTProperties backstopProps, RTProperties overrideProps,
        OrderedSet<String> timeZones,
        OrderedMap<String,Object> dftPushpinMap,
        OrderedMap<String,String> dftLegend,
        boolean ignoreDuplicates)
    {

        /* override properties */
        boolean popOverrideProps = false;
        if (overrideProps != null) {
            RTConfig.pushTemporaryProperties(overrideProps);
            popOverrideProps = true;
        }

        /* "Domain" attributes */
        String  domainName   = XMLTools.getAttribute(       domain, ATTR_name        , null , true );
        String  hostName     = XMLTools.getAttribute(       domain, ATTR_host        , null , true );
        String  className    = XMLTools.getAttribute(       domain, ATTR_class       , null , false);
        boolean acctLogin    = XMLTools.getAttributeBoolean(domain, ATTR_accountLogin, true , true );
        boolean userLogin    = XMLTools.getAttributeBoolean(domain, ATTR_userLogin   , true , true );
        boolean emailLogin   = XMLTools.getAttributeBoolean(domain, ATTR_emailLogin  , false, true );
        boolean showPassword = XMLTools.getAttributeBoolean(domain, ATTR_showPassword, true , true );
        boolean isDemo       = XMLTools.getAttributeBoolean(domain, ATTR_demo        , false, true );
        boolean allowLogin   = XMLTools.getAttributeBoolean(domain, ATTR_allowLogin  , true , true );
        boolean restricted   = XMLTools.getAttributeBoolean(domain, ATTR_restricted  , false, true );
        String  localeStr    = XMLTools.getAttribute(       domain, ATTR_locale      , null , true );
        
        /* no 'name'? */
        if (StringTools.isBlank(domainName)) {
            printError("Domain 'name' attribute not specified");
            this._setHasParsingErrors(xmlFile);
        }

        /* i18n RTProperties Strings */
        RTProperties i18nStr = new RTProperties();
        RTConfig.pushTemporaryProperties(i18nStr);
        i18nStr.setString("Domain.name"  ,domainName);
        i18nStr.setString("Domain.host"  ,hostName);
        i18nStr.setString("Domain.locale",localeStr);

        /* Domain overridden timezones? */
        OrderedSet<String> domainTMZ = this.parseTag_Timezones(domain);
        if (domainTMZ == null) {
            domainTMZ = timeZones;
        }

        /* local copy of default pushpins */
        dftPushpinMap = new OrderedMap<String,Object>(dftPushpinMap);

        /* local copy of default legend */
        dftLegend = new OrderedMap<String,String>(dftLegend);

        /* init BasicPrivateLabel */
        BasicPrivateLabel pl = this.createPrivateLabel(xmlFile, className, hostName);
        pl.setDomainName(domainName);     // "name" of Domain
        pl.setAccountLogin(acctLogin);
        pl.setUserLogin(userLogin);
        pl.setAllowEmailLogin(emailLogin);
        pl.setShowPassword(showPassword);
        pl.setEnableDemo(isDemo);
        pl.setRestricted(restricted);
        pl.setLocaleString(localeStr);
        pl.setTimeZones(domainTMZ);
        //printDebug("Loading 'Domain': " + pl.getDomainName());

        /* set default backstop properties */
        pl.setRTProperties(backstopProps);
        pl.pushRTProperties(); // makes PrivateLabel properties available within subsequent RTConfig calls
        //pl.printProperties("Default Properties");

        /* disable login? */
        if (!allowLogin) {
            pl.setProperty(ATTR_allowLogin, Boolean.FALSE);
        }

        /* loop through 'Domain' nodes */
        NodeList attrList = domain.getChildNodes();
        for (int c = 0; c < attrList.getLength(); c++) {

            /* get Node (only interested in 'Element's) */
            Node attrNode = attrList.item(c);
            if (!(attrNode instanceof Element)) {
                continue;
            }

            /* parse node */
            String attrName = attrNode.getNodeName();
            Element attrElem = (Element)attrNode;
            if (attrName.equalsIgnoreCase(TAG_BaseURL)) {
                this.parseTag_BaseURL(xmlFile, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_Alias)) {
                this.parseTag_Alias(xmlFile, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_DefaultLoginAccount)) {
                pl.setDefaultLoginAccount(XMLTools.getNodeText(attrElem,"",true));
            } else
            if (attrName.equalsIgnoreCase(TAG_DefaultLoginUser)) {
                pl.setDefaultLoginUser(XMLTools.getNodeText(attrElem,"",true));
            } else
            if (attrName.equalsIgnoreCase(TAG_PageTitle)) {
                String i18nKey = XMLTools.getAttribute(attrElem,ATTR_i18n,null,false);
                String ttlDft  = XMLTools.getNodeText(attrElem," ",true);
                I18N.Text pageTitle = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,ttlDft);
                this._validateI18NText(xmlFile, pageTitle);
                pl.setPageTitle(pageTitle);
            } else
            if (attrName.equalsIgnoreCase(TAG_DateFormat)) {
                String dateFmt = XMLTools.getNodeText(attrElem," ",true);
                if (StringTools.isBlank(dateFmt)) {
                    dateFmt = BasicPrivateLabel.getDefaultDateFormat();
                }
                pl.setDateFormat(dateFmt);
            } else
            if (attrName.equalsIgnoreCase(TAG_TimeFormat)) {
                String timeFmt = XMLTools.getNodeText(attrElem," ",true);
                if (StringTools.isBlank(timeFmt)) {
                    timeFmt = BasicPrivateLabel.getDefaultTimeFormat();
                }
                pl.setTimeFormat(timeFmt);
            } else
            if (attrName.equalsIgnoreCase(TAG_Copyright)) {
                pl.setCopyright(XMLTools.getNodeText(attrElem," ",true));
            } else
            if (attrName.equalsIgnoreCase(TAG_ReverseGeocodeProvider)) {
                this.parseTag_ReverseGeocodeProvider(xmlFile, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_GeocodeProvider)) {
                this.parseTag_GeocodeProvider(xmlFile, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_MobileLocationProvider)) {
                this.parseTag_MobileLocationProvider(xmlFile, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_I18N)) {
                this.parseTag_I18N(xmlFile, i18nPkgName, pl, attrElem, i18nStr);
            } else
            if (attrName.equalsIgnoreCase(TAG_Properties)) {
                this.parseTag_Properties(xmlFile, pl, attrElem, 
                    null/*keyPrefix*/, null/*rtPrefix*/, false); // disallow default RTConfig override
                pl.setRTProperties(overrideProps); // override properties
                //pl.printProperties("Domain Properties");
            } else
            if (attrName.equalsIgnoreCase(TAG_EMailAddresses)) {
                this.parseTag_EMailAddresses(xmlFile, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_StatusCodes)) {
                this.parseTag_StatusCodes(xmlFile, i18nPkgName, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_Acls)) {
                this.parseTag_Acls(xmlFile, i18nPkgName, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_EventNotificationEMail)) {
                this.parseTag_EventNotificationEMail(xmlFile, i18nPkgName, pl, attrElem);
            } else
            if (attrName.equalsIgnoreCase(TAG_Pushpins)) {
                OrderedMap<String,Object> ppMap = this.parseTAG_Pushpins(xmlFile, pl, attrElem, null, false);
                if (!ListTools.isEmpty(ppMap)) {
                    dftPushpinMap.putAll(ppMap);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_Legend)) {
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    String  legType    = XMLTools.getAttribute(attrElem, ATTR_type, "", false);
                    boolean useDefault = XMLTools.getAttributeBoolean(attrElem, ATTR_includeDefault, false, false);
                    String  legendHtml = null;
                    if (useDefault) {
                        // check for legend in '.conf'
                        if (dftLegend.containsKey(legType)) {
                            legendHtml = dftLegend.get(legType);
                        } else {
                            String refName = "<Domain>" + xmlFile.getName() + ":" + domainName;
                            legendHtml = this.parseLegendHTML(refName, pl.getLocale(), dftPushpinMap, legType, null);
                        }
                    }
                    if (StringTools.isBlank(legendHtml)) {
                        legendHtml = StringTools.replace(XMLTools.getNodeText(attrElem,"\n",true),"\\n","\n").trim();
                        if (StringTools.isBlank(legendHtml)) {
                            String refName = "<Domain>" + xmlFile.getName() + ":" + domainName;
                            legendHtml = this.parseLegendHTML(refName, pl.getLocale(), dftPushpinMap, legType, attrElem);
                        }
                    }
                    if (!StringTools.isBlank(legendHtml)) {
                        dftLegend.put(legType, legendHtml);
                    }
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_MapProvider)) {
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    this.parseTag_MapProvider(xmlFile, i18nPkgName, pl, attrElem, dftPushpinMap, dftLegend);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_JSPEntries)) {
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    this.parseTag_JSPEntries(xmlFile, i18nPkgName, pl, attrElem);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_WebPages)) {
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    this.parseTag_WebPages(xmlFile, i18nPkgName, pl, attrElem);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_Reports)) {
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    this.parseTag_Reports(xmlFile, i18nPkgName, pl, attrElem); // <String,ReportEntry>
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_PointsOfInterest)) {
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    this.parseTag_PointsOfInterest(xmlFile, i18nPkgName, pl, attrElem);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_MapShapes)) {
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    this.parseTag_MapShapes(xmlFile, i18nPkgName, pl, attrElem);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, attrElem);
            } else {
                printError("Invalid/Unrecognized tag name: " + attrName);
                this._setHasParsingErrors(xmlFile);
            }

        }

        /* pop temporary properties */
        pl.popRTProperties();

        /* save this PrivateLabel */
        this._addPrivateLabel(xmlFile, pl, ignoreDuplicates);
        
        /* pop I18N properties */
        RTConfig.popTemporaryProperties(i18nStr);
        
        /* pop override properties */
        if (popOverrideProps) {
            RTConfig.popTemporaryProperties(overrideProps);
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified host is valid
    *** @param host  the host name
    *** @return True if the specified host is valid
    **/
    protected boolean isValidHostname(String host)
    {
        String h = StringTools.trim(host);
        if (StringTools.isBlank(h)) {
            return false;
        } else
        if (h.equals("example.com")) {
            return false;
        } else
        if (h.endsWith(".example.com")) {
            return false;
        } else {
            // we don't currently check for invalid characters in the hostname
            return true;
        }
    }

    /**
    *** Parse 'BaseURL' tag
    *** @param pl        The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem  The 'BaseURL' node
    **/
    protected void parseTag_BaseURL(File xmlFile, BasicPrivateLabel pl, Element attrElem)
    {
        String baseURL = XMLTools.getNodeText(attrElem,"",true);
        if (!StringTools.isBlank(baseURL)) {
            if (!pl.hasDefaultBaseURL()) {
                pl.setDefaultBaseURL(baseURL);
            } else {
                printWarn("Default BaseURL already defined [ignoring " + baseURL + "]");
                this._setHasParsingWarnings(xmlFile);
            }
        }
    }

    /**
    *** Parse 'Alias' tag
    *** @param pl        The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem  The 'Alias' node
    **/
    protected void parseTag_Alias(File xmlFile, BasicPrivateLabel pl, Element attrElem)
    {
        String aliasHost = StringTools.trim(XMLTools.getAttribute(attrElem,ATTR_host,"",true)).toLowerCase();
        if (this.isValidHostname(aliasHost)) {
            String aliasDesc = XMLTools.getNodeText(attrElem,"",true).toLowerCase();
            pl.addHostAlias(aliasHost, aliasDesc);
        } else {
            // Excluding "", "example.com", "zzzz.example.com", ...
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'I18N' tag
    *** @param pl        The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem  The 'Properties' node
    **/
    protected void parseTag_I18N(File xmlFile, String i18nPkgName, BasicPrivateLabel pl, Element attrElem, RTProperties i18nStr)
    {
        NodeList propList = attrElem.getChildNodes();
        for (int e = 0; e < propList.getLength(); e++) {

            /* get Node (only interested in 'Element's) */
            Node propNode = propList.item(e);
            if (!(propNode instanceof Element)) {
                continue;
            }

            /* parse node */
            Element propElem = (Element)propNode;
            String propName = propNode.getNodeName();
            if (propName.equalsIgnoreCase(TAG_String)) {
                this.parseTag_String(xmlFile, i18nPkgName, pl, propElem, i18nStr);
            } else {
                printError("Invalid tag name: " + propName + " [expecting "+TAG_String+"]");
                this._setHasParsingErrors(xmlFile);
            }

        }
        
    }

    /**
    *** Parse 'String' tag
    *** @param pl        The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem  The 'Property' node
    **/
    protected void parseTag_String(File xmlFile, String i18nPkgName, BasicPrivateLabel pl, Element attrElem, RTProperties i18nStr)
    {
        String i18nKey = XMLTools.getAttribute(attrElem,ATTR_i18n,null,false); // String i18n
        if (!StringTools.isBlank(i18nKey)) {
            String key = XMLTools.getAttribute(attrElem,ATTR_key,null,false);  // String key
            if (StringTools.isBlank(key)) {
                key = i18nKey;
            }
            String    dftText  = XMLTools.getNodeText(attrElem,"\\n",true);
            I18N.Text i18nText = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,dftText);
            String    text     = i18nText.toString(pl.getLocale());
            i18nStr.setString(key, text);
            pl.setI18NTextProperty(key, i18nText);
        } else {
            printWarn("Undefined String key/i18n ignored.");
            this._setHasParsingWarnings(xmlFile);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'Properties' tag
    *** @param xmlFile             The current XML file being loaded
    *** @param ps                  A handle to a PropertySetter instance
    *** @param attrElem            The Properties tag element/node
    *** @param keyPrefix           The property key prefix
    *** @param rtPrefix            The RT properties key "prefix"
    *** @param defaultRTPOverride  True to default allowing RTConfig property overrides
    **/
    protected void parseTag_Properties(File xmlFile, RTConfig.PropertySetter ps, Element attrElem, 
        String keyPrefix, String rtPrefix, boolean defaultRTPOverride)
    {

        /* property key prefix (may be blank) */
        String prefix = StringTools.trim(keyPrefix) +
            StringTools.trim(XMLTools.getAttribute(attrElem,ATTR_keyPrefix,null,true));

        /* RTConfig override key prefix */
        String rtPropPrefix = StringTools.trim(rtPrefix) +
            StringTools.blankDefault(StringTools.trim(XMLTools.getAttribute(attrElem,ATTR_rtPropPrefix,null,true)),prefix);
        if (StringTools.isBlank(rtPropPrefix)) {
            // set to allow/disallow default RTConfig overrides below
            rtPropPrefix = defaultRTPOverride? "" : null; 
        }

        /* loop through property tags */
        NodeList propList = attrElem.getChildNodes();
        for (int e = 0; e < propList.getLength(); e++) {

            /* get Node (only interested in 'Element's) */
            Node propNode = propList.item(e);
            if (!(propNode instanceof Element)) {
                continue;
            }

            /* parse node */
            Element propElem = (Element)propNode;
            String propName = propNode.getNodeName();
            if (propName.equalsIgnoreCase(TAG_PropertyGroup)) { // recursive call
                this.parseTag_Properties(xmlFile, ps, propElem, 
                    prefix, rtPropPrefix, defaultRTPOverride);
            } else
            if (propName.equalsIgnoreCase(TAG_Property)) { // "Properties" sub
                this.parseTag_Property(xmlFile, ps, propElem, 
                    prefix, rtPropPrefix);
            } else
            if (propName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, null, propElem);
            } else {
                printError("Invalid tag name: " + propName + " [expecting "+TAG_Property+"]");
                this._setHasParsingErrors(xmlFile);
            }
            
        }
        
    }

    /**
    *** Parse 'Property' tag
    *** @param xmlFile      The currentl XML file dbeing parsed.
    *** @param ps           The PropertySetter which will received the parse properties
    *** @param attrElem     The 'Property' node
    *** @param prefix       The property key prefix
    *** @param rtPropPrefix The key prefix used to lookup externally defined runtime values. 
    **/
    protected void parseTag_Property(File xmlFile, RTConfig.PropertySetter ps, Element attrElem, 
        String prefix, String rtPropPrefix)
    {

        /* key */
        String k = StringTools.trim(XMLTools.getAttribute(attrElem,ATTR_key,"",false));
        if (StringTools.isBlank(k)) {
            printWarn("Undefined property key ignored.");
            this._setHasParsingWarnings(xmlFile);
            return;
        }
        String key = StringTools.trim(prefix) + k;

        /* rtKey */
        String rtKey = null;
        if (rtPropPrefix != null) {
            String rtk = StringTools.trim(XMLTools.getAttribute(attrElem,ATTR_rtKey,null,false));
            rtKey = StringTools.trim(rtPropPrefix) + (!StringTools.isBlank(rtk)? rtk : k); // non-blank
        } else {
            // 'rtKey' remains null
        }

        /* value */
        String val = XMLTools.getNodeText(attrElem, "\\n", true);

        /* override with RTConfig props */
        if (!StringTools.isBlank(rtKey)) {
            String v = RTConfig.getString(rtKey, null);
            if (v != null) {
                val = v;
                //Print.logInfo("[%s] RTProperty '%s' ==> '%s'", xmlFile.getName(), rtKey, val);
            }
        }

        /* set property */
        //Print.logInfo("[%s] Property '%s' ==> '%s'", xmlFile.getName(), key, val);
        ps.setProperty(key, val);

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'EMailAddresses' tag
    *** @param xmlFile       The current loading xml file
    *** @param pl            The BasicPrivateLabel instance for the current 'Domain'
    *** @param listAttrElem  The 'EMailAddresses' node
    **/
    protected void parseTag_EMailAddresses(File xmlFile, BasicPrivateLabel pl, Element listAttrElem)
    {
        String domain = StringTools.trim(XMLTools.getAttribute(listAttrElem,ATTR_domainName,null,true));
        if (StringTools.isBlank(domain)) { domain = "example.com"; }
        NodeList emailAttrList = listAttrElem.getChildNodes();
        for (int e = 0; e < emailAttrList.getLength(); e++) {

            /* get Node (only interested in 'Element's) */
            Node emailAttrNode = emailAttrList.item(e);
            if (!(emailAttrNode instanceof Element)) {
                continue;
            }

            /* parse node */
            Element emailAttrElem = (Element)emailAttrNode;
            String emailAttrName = emailAttrNode.getNodeName();
            if (emailAttrName.equalsIgnoreCase(TAG_EMailAddress)) {
                String type      = XMLTools.getAttribute(emailAttrElem,ATTR_type,null,false); // may be null
                String emailAddr = XMLTools.getNodeText(emailAttrElem," ",true).trim();
                if (!StringTools.isBlank(emailAddr)) {
                    if (emailAddr.indexOf("@") < 0) { emailAddr += "@" + domain; }
                    if (this.isValidEMailAddress(emailAddr)) {
                        pl.setEMailAddress(type, emailAddr);
                    } else {
                        printError("Invalid EMail address '"+emailAddr+"'");
                        this._setHasParsingErrors(xmlFile);
                    }
                }
            } else
            if (emailAttrName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, emailAttrElem);
            } else {
                printError("Invalid tag name: " + emailAttrName + " [expecting "+TAG_EMailAddress+"]");
                this._setHasParsingErrors(xmlFile);
            }

        }
    }

    protected boolean isValidEMailAddress(String emailAddr)
    {
        if (StringTools.isBlank(emailAddr)) {
            // null, blank
            return false;
        } else
        if ((emailAddr.indexOf("@") <= 0) || emailAddr.endsWith("@")) {
            // "smith", "@example.com", "smith@"
            return false;
        } else 
        if ((emailAddr.indexOf(" ") >= 0) || (emailAddr.indexOf(",") >= 0)) {
            // "john smith@example.com", "john@example.com,smith@example.com"
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'StatusCodes' tag
    *** @param xmlFile       The current loading xml file
    *** @param pl            The BasicPrivateLabel instance for the current 'Domain'
    *** @param listAttrElem  The 'StatusCodes' node
    **/
    protected void parseTag_StatusCodes(File xmlFile, String i18nPkgName, BasicPrivateLabel pl, Element listAttrElem)
    {
        NodeList scAttrList = listAttrElem.getChildNodes();
        boolean  only       = StringTools.parseBoolean(XMLTools.getAttribute(listAttrElem,ATTR_only,null,false),false);
        int      scCount    = 0;
        for (int c = 0; c < scAttrList.getLength(); c++) {

            /* get Node (only interested in 'Element's) */
            Node scAttrNode = scAttrList.item(c);
            if (!(scAttrNode instanceof Element)) {
                continue;
            }

            /* parse node */
            Element scAttrElem = (Element)scAttrNode;
            String scAttrName = scAttrNode.getNodeName();
            if (scAttrName.equalsIgnoreCase(TAG_StatusCode)) {
                String    codeStr  = XMLTools.getAttribute(scAttrElem,ATTR_code,null,false);
                int       code     = StringTools.parseInt(codeStr,0);
                String    name     = XMLTools.getAttribute(scAttrElem,ATTR_name,null,false);
                String    iconName = XMLTools.getAttribute(scAttrElem,ATTR_iconName,null,false);
                String    i18nKey  = XMLTools.getAttribute(scAttrElem,ATTR_i18n,null,false); // String i18n
                String    dftDesc  = StringTools.trim(XMLTools.getNodeText(scAttrElem," ",true));
                I18N.Text i18nText = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,dftDesc);
                if (code > 0) {
                    this._validateI18NText(xmlFile, i18nText);
                    StatusCodes.Code sc = new StatusCodes.Code(code, name, i18nText);
                    if (!StringTools.isBlank(iconName)) {
                        sc.setIconName(iconName);
                    }
                    pl.addStatusCode(sc);
                    scCount++;
                } else {
                    printError("Code missing or Invalid: " + codeStr);
                    this._setHasParsingErrors(xmlFile);
                }
            } else
            if (scAttrName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, scAttrElem);
            } else {
                printError("Invalid tag name: " + scAttrName + " [expecting "+TAG_StatusCode+"]");
                this._setHasParsingErrors(xmlFile);
            }

        }
        pl.setStatusCodeOnly(only && (scCount > 0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'ReverseGeocodeProvider' tag
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param rgpAttrElem  The 'ReverseGeocodeProvider' node
    **/
    protected void parseTag_ReverseGeocodeProvider(File xmlFile, BasicPrivateLabel pl, Element rgpAttrElem)
    {

        /* name */
        String rpName = XMLTools.getAttribute(rgpAttrElem, ATTR_name, null, false);
        if (StringTools.isBlank(rpName)) {
            printError("ReverseGeocodeProvider 'name' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* active? */
        String active = XMLTools.getAttribute(rgpAttrElem, ATTR_active, null, true);
        if (!this._isAttributeActive(active,rpName)) {
            // inactive, quietly ignore
            return;
        }

        /* ReverseGeocodeProvider class name */
        String rpClassName = XMLTools.getAttribute(rgpAttrElem, ATTR_class, null, false);
        if (StringTools.isBlank(rpClassName)) {
            printError("ReverseGeocodeProvider 'class' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* also set as GeocodeProvider? */
        boolean usAsGeocoder = XMLTools.getAttributeBoolean(rgpAttrElem, ATTR_geocode, false, false);

        /* properties */
        String rtPropPrefix = XMLTools.getAttribute(rgpAttrElem,ATTR_rtPropPrefix,null,true);
        RTProperties rtProps = new RTProperties();
        NodeList propAttrList = rgpAttrElem.getChildNodes();
        for (int c = 0; c < propAttrList.getLength(); c++) {

            /* get Node (only interested in 'Element's) */
            Node attrNode = propAttrList.item(c);
            if (!(attrNode instanceof Element)) {
                continue;
            }

            /* parse node */
            String attrName = attrNode.getNodeName();
            Element attrElem = (Element)attrNode;
            if (attrName.equalsIgnoreCase(TAG_Property)) { // "ReverseGeocodeProvider" sub
                String key   = XMLTools.getAttribute(attrElem,ATTR_key,null,false); // property key
                String rtKey = StringTools.blankDefault(XMLTools.getAttribute(attrElem,ATTR_rtKey,null,false),key);
                if (!StringTools.isBlank(key)) {
                    String val = XMLTools.getNodeText(attrElem, "", true);
                    if (!StringTools.isBlank(rtPropPrefix)) {
                        // IE. "Domain.ReverseGeocodeProvider.host"
                        String v = RTConfig.getString(rtPropPrefix + rtKey, null);
                        if (v != null) {
                            val = v;
                        }
                    }
                    rtProps.setProperty(key, val);
                } else {
                    printWarn("Undefined property key ignored.");
                    this._setHasParsingWarnings(xmlFile);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, attrElem);
            } else {
                printError("Invalid tag name: " + attrName + " [expecting "+TAG_Property+"]");
                this._setHasParsingErrors(xmlFile);
            }

        }

        /* create instance of ReverseGeocodeProvider */
        String rpKey = XMLTools.getAttribute(rgpAttrElem,ATTR_key,null,true); // authorization key
        ReverseGeocodeProvider rgp = null;
        try {
            Class rgpClass = Class.forName(rpClassName);  // ClassNotFoundException
            MethodAction ma = new MethodAction(rgpClass, String.class, String.class, RTProperties.class);
            rgp = (ReverseGeocodeProvider)ma.invoke(rpName, rpKey, rtProps);
        } catch (ClassNotFoundException cnfe) {
            printError("ReverseGeocodeProvider class not found: " + rpClassName);
            this._setHasParsingErrors(xmlFile);
            return;
        } catch (Throwable t) { // ClassNotFoundException, ClassCastException, etc.
            printError("ReverseGeocodeProvider creation error: " + rpClassName + " [" + t);
            this._setHasParsingErrors(xmlFile);
            return;
        }
        pl.addReverseGeocodeProvider(rgp);
        
        /* set as GeocodeProvider? */
        if (usAsGeocoder) {
            if (rgp instanceof GeocodeProvider) {
                pl.addGeocodeProvider((GeocodeProvider)rgp);
            } else {
                printError("'geocode' specified, and ReverseGeocodeProvider is not a GeocodeProvider: " + rpName);
                this._setHasParsingErrors(xmlFile);
            }
        } else {
            // skip
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'GeocodeProvider' tag
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param gpAttrElem   The 'GeocodeProvider' node
    **/
    protected void parseTag_GeocodeProvider(File xmlFile, BasicPrivateLabel pl, Element gpAttrElem)
    {

        /* name */
        String gpName = XMLTools.getAttribute(gpAttrElem, ATTR_name, null, false);
        if (StringTools.isBlank(gpName)) {
            printError("GeocodeProvider 'name' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* active? */
        String active = XMLTools.getAttribute(gpAttrElem, ATTR_active, null, true);
        if (!this._isAttributeActive(active,gpName)) {
            // inactive, quietly ignore
            return;
        }

        /* GeocodeProvider class name */
        String gpClassName = XMLTools.getAttribute(gpAttrElem, ATTR_class, null, false);
        if (StringTools.isBlank(gpClassName)) {
            printError("GeocodeProvider 'class' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* properties */
        RTProperties rtProps = new RTProperties();
        NodeList propAttrList = gpAttrElem.getChildNodes();
        for (int c = 0; c < propAttrList.getLength(); c++) {

            /* get Node (only interested in 'Element's) */
            Node attrNode = propAttrList.item(c);
            if (!(attrNode instanceof Element)) {
                continue;
            }
                
            /* parse node */
            String attrName = attrNode.getNodeName();
            Element attrElem = (Element)attrNode;
            if (attrName.equalsIgnoreCase(TAG_Property)) { // "GeocodeProvider" sub
                String key = XMLTools.getAttribute(attrElem,ATTR_key,null,false); // property key
                if (!StringTools.isBlank(key)) {
                    rtProps.setProperty(key, XMLTools.getNodeText(attrElem,"",true));
                } else {
                    printWarn("Undefined property key ignored.");
                    this._setHasParsingWarnings(xmlFile);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, attrElem);
            } else {
                printError("Invalid tag name: " + attrName + " [expecting "+TAG_Property+"]");
                this._setHasParsingErrors(xmlFile);
            }

        }

        /* create instance of GeocodeProvider */
        String gpKey = XMLTools.getAttribute(gpAttrElem,ATTR_key ,null,true); // authorization key
        GeocodeProvider gp = null;
        try {
            Class gpClass = Class.forName(gpClassName);  // ClassNotFoundException
            MethodAction ma = new MethodAction(gpClass, String.class, String.class, RTProperties.class);
            gp = (GeocodeProvider)ma.invoke(gpName, gpKey, rtProps);
        } catch (ClassNotFoundException cnfe) {
            printError("GeocodeProvider class not found: " + gpClassName);
            this._setHasParsingErrors(xmlFile);
            return;
        } catch (Throwable t) { // ClassNotFoundException, ClassCastException, etc.
            printError("GeocodeProvider creation error: " + gpClassName + " [" + t);
            this._setHasParsingErrors(xmlFile);
            return;
        }
        pl.addGeocodeProvider(gp);

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'MobileLocationProvider' tag
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param gpAttrElem   The 'MobileLocationProvider' node
    **/
    protected void parseTag_MobileLocationProvider(File xmlFile, BasicPrivateLabel pl, Element mpAttrElem)
    {

        /* name */
        String mpName = XMLTools.getAttribute(mpAttrElem, ATTR_name, null, false);
        if (StringTools.isBlank(mpName)) {
            printError("MobileLocationProvider 'name' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* active? */
        String active = XMLTools.getAttribute(mpAttrElem, ATTR_active, null, true);
        if (!this._isAttributeActive(active,mpName)) {
            // inactive, quietly ignore
            return;
        }

        /* MobileLocationProvider class name */
        String mpClassName = XMLTools.getAttribute(mpAttrElem, ATTR_class, null, false);
        if (StringTools.isBlank(mpClassName)) {
            printError("MobileLocationProvider 'class' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* properties */
        RTProperties rtProps = new RTProperties();
        NodeList propAttrList = mpAttrElem.getChildNodes();
        for (int c = 0; c < propAttrList.getLength(); c++) {

            /* get Node (only interested in 'Element's) */
            Node attrNode = propAttrList.item(c);
            if (!(attrNode instanceof Element)) {
                continue;
            }
                
            /* parse node */
            String attrName = attrNode.getNodeName();
            Element attrElem = (Element)attrNode;
            if (attrName.equalsIgnoreCase(TAG_Property)) { // "MobileLocationProvider" sub
                String key = XMLTools.getAttribute(attrElem,ATTR_key,null,false); // property key
                if (!StringTools.isBlank(key)) {
                    rtProps.setProperty(key, XMLTools.getNodeText(attrElem,"",true));
                } else {
                    printWarn("Undefined property key ignored.");
                    this._setHasParsingWarnings(xmlFile);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, attrElem);
            } else {
                printError("Invalid tag name: " + attrName + " [expecting "+TAG_Property+"]");
                this._setHasParsingErrors(xmlFile);
            }

        }

        /* create instance of MobileLocationProvider */
        String mpKey = XMLTools.getAttribute(mpAttrElem,ATTR_key ,null,true); // authorization key
        MobileLocationProvider mp = null;
        try {
            Class mpClass = Class.forName(mpClassName);  // ClassNotFoundException
            MethodAction ma = new MethodAction(mpClass, String.class, String.class, RTProperties.class);
            mp = (MobileLocationProvider)ma.invoke(mpName, mpKey, rtProps);
        } catch (ClassNotFoundException cnfe) {
            printError("MobileLocationProvider class not found: " + mpClassName);
            this._setHasParsingErrors(xmlFile);
            return;
        } catch (Throwable t) { // ClassNotFoundException, ClassCastException, etc.
            printError("MobileLocationProvider creation error: " + mpClassName + " [" + t);
            this._setHasParsingErrors(xmlFile);
            return;
        }
        pl.addMobileLocationProvider(mp);

    }

    // ------------------------------------------------------------------------

    /**
    *** return the AccessLevel list for the specified argument
    **/
    private AccessLevel[] _parseAccessLevelValues(File xmlFile, String strList)
    {
        String v[] = StringTools.split(strList,',');
        if (ListTools.isEmpty(v)) {
            return AclEntry.GetValueListForMaximumAccessLevel(AccessLevel.ALL);
        } else
        if (v.length == 1) {
            return AclEntry.GetValueListForMaximumAccessLevel(AclEntry.parseAccessLevel(v[0],AccessLevel.ALL));
        } else {
            AccessLevel lastValue = null;
            java.util.List<AccessLevel> values = new Vector<AccessLevel>();
            for (int i = 0; i < v.length; i++) {
                AccessLevel acc = AclEntry.parseAccessLevel(v[i], AccessLevel.ALL);
                if ((lastValue == null) || (lastValue.getIntValue() < acc.getIntValue())) {
                    values.add(acc);
                } else {
                    printError("Invalid AccessLevel list specified: " + strList);
                    this._setHasParsingErrors(xmlFile);
                }
                lastValue = acc;
            }
            return values.toArray(new AccessLevel[values.size()]);
        }
    }

    /**
    *** Parse 'Acls' tag
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param acls         The 'Acls' node
    **/
    protected void parseTag_Acls(File xmlFile, String i18nPkgName, BasicPrivateLabel pl, Element acls)
    {

        /* default access */
        AccessLevel dftAccess = AclEntry.parseAccessLevel(XMLTools.getAttribute(acls,ATTR_default,null,false), null);

        /* parse ACL entries */
        Map<String,AclEntry> aclMap = new OrderedMap<String,AclEntry>();
        NodeList aclList = XMLTools.getChildElements(acls,TAG_Acl);
        for (int r = 0; r < aclList.getLength(); r++) {
            Element aclElem = (Element)aclList.item(r);
            String aclName  = XMLTools.getAttribute(aclElem,ATTR_name,null,false);
            if (!StringTools.isBlank(aclName)) {
                // possible ACL permissions
                String accValues     = XMLTools.getAttribute(aclElem,ATTR_values,null,false);
                if (StringTools.isBlank(accValues)) { accValues = XMLTools.getAttribute(aclElem,ATTR_maximum,null,false); }
                AccessLevel valAcc[] = this._parseAccessLevelValues(xmlFile, accValues);
                // default ACL value (Properties overridable)
                String accDefault    = RTConfig.getString(aclName,XMLTools.getAttribute(aclElem,ATTR_default,null,false));
                AccessLevel dftAcc   = AclEntry.parseAccessLevel(accDefault, dftAccess);
                // ACL description
                String i18nKey       = XMLTools.getAttribute(aclElem,ATTR_i18n,null,false);
                String descDft       = XMLTools.getNodeText(aclElem,"\\n",false);
                I18N.Text aclDesc    = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,descDft);
                this._validateI18NText(xmlFile, aclDesc);
                // Acl entry
                AclEntry ae = new AclEntry(aclName.trim(), aclDesc, valAcc, dftAcc);
                ae.setHidden(XMLTools.getAttributeBoolean(aclElem,ATTR_hidden,false,false));
                aclMap.put(aclName.trim(), ae);
            } else {
                printWarn("Domain '%s' Acl missing 'name'", pl.getName());
                this._setHasParsingWarnings(xmlFile);
            }
        }

        /* save ACL entries */
        pl.addAclMap(dftAccess, aclMap);

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse 'EventNotificationEMail' tag
    *** @param i18nPackage  The i18n resource package name for localized text
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem     The 'EventNotificationEMail' node
    **/
    protected void parseTag_EventNotificationEMail(File xmlFile, String i18nPackage, BasicPrivateLabel pl, Element attrElem)
    {

        /* From address */
        String emailFrom = XMLTools.getAttribute(attrElem, ATTR_from, null, true);
        if (!this.isValidEMailAddress(emailFrom)) {
            printError("Invalid EMail address '"+emailFrom+"'");
            this._setHasParsingErrors(xmlFile);
        }

        /* use as default subject/body? */
        boolean useAsDefault = XMLTools.getAttributeBoolean(attrElem, ATTR_useAsDefault, false, true);

        /* Subject, Body */
        I18N.Text emailSubj = null;
        I18N.Text emailBody = null;
        // nodes
        NodeList emailNodes = attrElem.getChildNodes();
        for (int n = 0; n < emailNodes.getLength(); n++) {
            Node emailNode = emailNodes.item(n);
            if (!(emailNode instanceof Element)) {
                continue;
            }
            String nodeName = emailNode.getNodeName();
            Element nodeElem = (Element)emailNode;
            if (nodeName.equalsIgnoreCase(TAG_Subject)) {
                String i18nKey = XMLTools.getAttribute(nodeElem,ATTR_i18n,null,false);
                String subjDft = XMLTools.getNodeText(nodeElem,"\\n",false);
                emailSubj = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPackage,i18nKey,subjDft);
                this._validateI18NText(xmlFile, emailSubj);
            } else
            if (nodeName.equalsIgnoreCase(TAG_Body)) {
                String i18nKey  = XMLTools.getAttribute(nodeElem,ATTR_i18n,null,false);
                String bodyText = XMLTools.getNodeText(nodeElem,null,false);
                //String body[] = StringTools.parseString(bodyText, '\n');
                String body[]   = XMLTools.parseLines(bodyText);
                for (int i = 0; i < body.length; i++) {
                    body[i] = body[i].trim();
                }
                String bodyDft = StringTools.join(body,'\n');
                //printInfo("BodyText:\n" + bodyDft);
                emailBody = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPackage,i18nKey,bodyDft);
                this._validateI18NText(xmlFile, emailBody);
            } else
            if (nodeName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, nodeElem);
            } else {
                // unrecognized tag
            }
        }

        /* save */
        pl.setEventNotificationEMail(emailFrom, emailSubj, emailBody, useAsDefault);
        // Debug log
        //printInfo("Event Notification:\n" +
        //    "From: " + emailFrom + "\n" +
        //    "Subject: " + emailSubj + "\n" +
        //    "Body: " + emailBody + "\n");

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parse 'PushPins' tag.
    *** @param xmlFile       The currentl XML file being parsed
    *** @param pl            The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem      The 'MapProvider' node
    *** @param dftPushpinMap The default pushpin map
    *** @return 'null' (must be overridden to change behavior)
    **/
    protected OrderedMap<String,Object> parseTAG_Pushpins(File xmlFile, BasicPrivateLabel pl, Element attrElem,
        OrderedMap<String,Object> dftPushpinMap, boolean isGlobalPushpins)
    {
        // override
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parse 'LogMessage' tag.
    *** @param xmlFile      The currentl XML file being parsed
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem     The 'MapProvider' node
    **/
    protected void parseTag_LogMessage(File xmlFile, BasicPrivateLabel pl, Element attrElem)
    {
        String type = StringTools.trim(XMLTools.getAttribute(attrElem,ATTR_type,"debug",true)).toLowerCase();
        String text = XMLTools.getNodeText(attrElem," ",true);
        if (!type.equals("debug") || RTConfig.isDebugMode()) {
            Print._writeLog(1,"PrivateLabel: "+xmlFile+"\n");
            Print._writeLog(1,"  LogMessage: "+text+"\n");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // The following are implemented in PrivateLabelLoader.java

    /**
    *** Parse 'MapProvider' tag.  This method is intended to be subclassed.
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem     The 'MapProvider' node
    **/
    protected void parseTag_MapProvider(File xmlFile, String i18nPkgName, BasicPrivateLabel pl, Element attrElem,
        OrderedMap<String,Object> dftPushpinMap,
        OrderedMap<String,String> dftLegend)
    {
        // WAR environment only
        //printDebug("  Skipping MapProvider tag ...");
    }
    
    /**
    *** Parse HTML Legend
    **/
    protected String parseLegendHTML(String refName, Locale locale, 
        OrderedMap pushpinMap, 
        String legendType, Element legendElem)
    {
        return null;
    }

    /**
    *** Parse 'JSPFiles' tag.  This method is intended to be subclassed.
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param jspFiles     The 'JSPFiles' node
    **/
    protected void parseTag_JSPEntries(File xmlFile, String i18nPkgName, BasicPrivateLabel pl, Element jspFiles)
    {
        // WAR environment only
        //printDebug("  Skipping JSPFiles tag ...");
    }

    /**
    *** Parse 'WebPages' tag.  This method is intended to be subclassed.
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param pl           The BasicPrivateLabel instance for the current 'Domain'
    *** @param webPages     The 'WebPages' node
    **/
    protected void parseTag_WebPages(File xmlFile, String i18nPkgName, BasicPrivateLabel pl, Element webPages)
    {
        // WAR environment only
        //printDebug("  Skipping WebPages tag ...");
    }

    /**
    *** Parse 'Reports' tag.  This method is intended to be subclassed.
    *** @param xmlFile      The current xml file being parsed.
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param bpl          The BasicPrivateLabel instance for the current 'Domain'
    *** @param reports      The 'Reports' node
    **/
    protected void parseTag_Reports(File xmlFile, String i18nPkgName, BasicPrivateLabel bpl, Element reports)
    {
        // WAR environment only
        //printDebug("  Skipping Reports tag ...");
    }

    /**
    *** Parse 'PointsOfInterest' tag.  This method is intended to be subclassed.
    *** @param xmlFile      The current xml file being parsed.
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param bpl          The BasicPrivateLabel instance for the current 'Domain'
    *** @param pois         The 'PointsOfInterest' node
    **/
    protected void parseTag_PointsOfInterest(File xmlFile, String i18nPkgName, BasicPrivateLabel bpl, Element pois)
    {
        // WAR environment only
        //printDebug("  Skipping PointsOfInterest tag ...");
    }

    /**
    *** Parse 'MapShapes' tag.  This method is intended to be subclassed.
    *** @param xmlFile      The current xml file being parsed.
    *** @param i18nPkgName  The i18n resource package name for localized text
    *** @param bpl          The BasicPrivateLabel instance for the current 'Domain'
    *** @param mapShps      The 'MapShapes' node
    **/
    protected void parseTag_MapShapes(File xmlFile, String i18nPkgName, BasicPrivateLabel bpl, Element mapShps)
    {
        // WAR environment only
        //printDebug("  Skipping MapShapes tag ...");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Extracts and returns the host name from the specified URL
    *** @param urlStr  The URL to parse for the host name
    *** @return  The parsed host name
    **/
    public static String getURLHostName(String urlStr)
    {
        if (urlStr != null) {
            try {
                URL url = new URL(urlStr);
                String host = url.getHost();
                return (host != null)? host : "";
            } catch (MalformedURLException mfue) {
                printWarn("Invalid URL format: " + urlStr);
                return "";
            }
        }
        return null;
    }

    /* return the BasicPrivateLabel instance for the specified request URL */
    //(this method omitted to avoid having to import any Servlet support classes)
    //public BasicPrivateLabel getPrivateLabel(HttpServletRequest request) 
    //{
    //    StringBuffer reqURL = request.getRequestURL();
    //    String hostName = (reqURL != null)? BasicPrivateLabelLoader.getURLHostName(reqURL.toString()) : null;
    //    return getPrivateLabel(hostName);
    //}

    /**
    *** Returns the BasicPrivateLabel instance for the specified request URL
    *** @param url  The request URL
    *** @return The BasicPrivateLabel instance
    **/
    public static BasicPrivateLabel getPrivateLabelForURL(URL url)
    {
        BasicPrivateLabelLoader bpll = BasicPrivateLabelLoader._getInstance();
        if (url != null) {

            /* extract host name (ie. "track.example.com") */
            String hostName = StringTools.trim(url.getHost());

            /* try host/path (ie. "track.example.com/custom") */
            String path = url.getPath();
            if (path.startsWith("/")) {
                int p = path.indexOf("/",1);
                String hp = hostName + ((p > 0)? path.substring(0,p) : path);
                BasicPrivateLabel bpl = bpll._getPrivateLabel(hp, null);
                if (bpl != null) {
                    // ie. "track.example.com/trackme"
                    return bpl;
                }
            }

            /* host as-is */
            BasicPrivateLabel host_bpl = bpll._getPrivateLabel(hostName, null);
            if (host_bpl != null) {
                return host_bpl;
            }

            /* remove first prefix (ie. "track.example.com" ==> "example.com") */
            int x = hostName.indexOf(".");
            if ((x >= 0) && (x < hostName.lastIndexOf("."))) {
                String accountID  = hostName.substring(0,x);
                String domainName = hostName.substring(x+1);
                BasicPrivateLabel domain_bpl = bpll._getPrivateLabel(domainName, null);
                if (domain_bpl != null) {
                    return domain_bpl;
                }
            }

        }
            
        /* return default */
        return bpll._getPrivateLabel(null);
            
    }

    /**
    *** Returns the BasicPrivateLabel instance for the specified host name
    *** @param name  The host name
    *** @return The BasicPrivateLabel instance
    **/
    public static BasicPrivateLabel getPrivateLabel(String name/*hostName*/)
    {
        return BasicPrivateLabelLoader._getInstance()._getPrivateLabel(name);
    }

    /**
    *** Returns the default BasicPrivateLabel instance 
    *** @return The default BasicPrivateLabel instance
    **/
    public static BasicPrivateLabel getDefaultPrivateLabel()
    {
        return BasicPrivateLabelLoader._getInstance()._getPrivateLabel(null);
    }

    /**
    *** Returns a String array of all private label keys/names
    *** @return A String array of all private label keys/names
    **/
    public static Collection<String> getPrivateLabelNames()
    {
        return BasicPrivateLabelLoader.getPrivateLabelNames(false);
    }

    /**
    *** Returns a String array of all private label keys/names
    *** @param nameOnly  True to return a list of PrivateLabel names only (excluding hosts/aliases).
    *** @return A String array of all private label keys/names
    **/
    public static Collection<String> getPrivateLabelNames(boolean nameOnly)
    {
        Collection<String> list = new Vector<String>();
        Map<String,BasicPrivateLabel> privLblMap = BasicPrivateLabelLoader._getInstance().getPrivateLabelMap();
        if (privLblMap != null) {
            if (nameOnly) {
                // just the domain/host name
                for (Iterator<String> i = privLblMap.keySet().iterator(); i.hasNext();) {
                    BasicPrivateLabel pbl = privLblMap.get(i.next());
                    String name = pbl.getName();
                    if (!StringTools.isBlank(name) && !list.contains(name)) {
                        list.add(name);
                    }
                }
            } else {
                // domain/host name, plus all aliases
                for (Iterator<String> i = privLblMap.keySet().iterator(); i.hasNext();) {
                    list.add(i.next());
                }
            }
        }
        return list;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this BasicPrivateLabelLoader encounted warnings while parsing 'private.xml'
    *** @return True if warnings were encounted, false otherwise
    **/
    public static boolean hasParsingWarnings()
    {
        return BasicPrivateLabelLoader._getInstance().hasParsingWarnings;
    }
    
    /**
    *** Called by this BasicPrivateLabelLoader instance (or subclass) if a parsing warning was encountered
    **/
    protected void _setHasParsingWarnings(File xmlFile)
    {
        this.hasParsingWarnings = true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this BasicPrivateLabelLoader encounted errors while parsing 'private.xml'
    *** @return True if errors were encounted, false otherwise
    **/
    public static boolean hasParsingErrors()
    {
        return BasicPrivateLabelLoader._getInstance().hasParsingErrors;
    }
    
    /**
    *** Called by this BasicPrivateLabelLoader instance (or subclass) if a parsing error was encountered
    **/
    protected void _setHasParsingErrors(File xmlFile)
    {
        this.hasParsingErrors = true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Validates the specified I18N.Text value
    *** @param text The I18N.Text value
    *** @return The specified I18N.Text value
    **/
    protected I18N.Text _validateI18NText(File xmlFile, I18N.Text text)
    {
        if (text == null) {
            printError("I18N is null ...");
            this._setHasParsingErrors(xmlFile);
        } else
        if (!text.hasKey()) {
            printError("I18N text is missing a 'key' specification");
            this._setHasParsingErrors(xmlFile);
        } else
        if (!StringTools.isValidID(text.getKey(),'.','_')) {
            printError("I18N text 'key' is invalid: " + text.getKey());
            this._setHasParsingErrors(xmlFile);
        }
        return text;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a default PrivateLabel Domain has been defined
    *** @return True if a default PrivateLabel Domain has been defined
    **/
    public static boolean hasDefaultPrivateLabel()
    {
        return (BasicPrivateLabelLoader._getInstance().defaultPrivateLabel != null);
    }

    /**
    *** Returns the BasicPrivateLabel instance for the specified host name
    *** @param hostName  The host name
    *** @return The BasicPrivateLabel instance
    **/
    private BasicPrivateLabel _getPrivateLabel(String hostName) 
    {
        return this._getPrivateLabel(hostName, this.defaultPrivateLabel);
    }

    /**
    *** Returns the BasicPrivateLabel instance for the specified (host)name
    *** @param name  The host name
    *** @return The BasicPrivateLabel instance
    **/
    private BasicPrivateLabel _getPrivateLabel(String name/*hostName*/, BasicPrivateLabel dftPrivLabel) 
    {
        // get custom private label based on domain name 
        if ((name != null) && (this.privateLabelMap != null)) {
            BasicPrivateLabel plbl = this.privateLabelMap.get(StringTools.trim(name));
            return (plbl != null)? plbl : dftPrivLabel;
        } else {
            return dftPrivLabel;
        }
    }

    /**
    *** Adds a BasicPrivateLabel to the managed private label list
    *** @param privLabel  The BasicPrivateLabel to add
    **/
    protected void _addPrivateLabel(File xmlFile, BasicPrivateLabel privLabel, boolean ignoreDuplicates)
    {
        if (privLabel != null) {

            /* allocate storage map */
            if (this.privateLabelMap == null) { 
                this.privateLabelMap = new HashMap<String,BasicPrivateLabel>(); 
            }
            HashMap<String,BasicPrivateLabel> tempPLMap = new HashMap<String,BasicPrivateLabel>(); 

            /* add BasicPrivateLabel under 'host' */
            String host = StringTools.trim(privLabel.getHostName());
            if (StringTools.isBlank(host)) {
                host = BasicPrivateLabel.DEFAULT_HOST;
                privLabel.setHostName(host);
            }
            tempPLMap.put(host, privLabel);

            /* add BasicPrivateLabel under 'name' alias */
            String domainName = privLabel.getDomainName();
            if (!StringTools.isBlank(domainName) && !tempPLMap.containsKey(domainName)) {
                tempPLMap.put(domainName, privLabel);
            }

            /* add BasicPrivateLabel under host 'domain' aliases */
            java.util.List<String> hostAliasList = privLabel.getHostAliasNames();
            if (!ListTools.isEmpty(hostAliasList)) {
                for (String hostAlias : hostAliasList) {
                    if (!StringTools.isBlank(hostAlias) && !tempPLMap.containsKey(hostAlias)) {
                        tempPLMap.put(hostAlias, privLabel);
                    }
                }
            }

            /* add all aliases to privateLabelMap (check for duplicates) */
            boolean hasDuplicates = false;
            for (String hostAlias : tempPLMap.keySet()) {
                if (this.privateLabelMap.containsKey(hostAlias)) {
                    if (ignoreDuplicates) {
                        printWarn( "Domain Host/Alias already defined: [" + xmlFile + " : " + privLabel.getName() + "] " + hostAlias);
                        this._setHasParsingWarnings(xmlFile);
                    } else {
                        printError("Domain Host/Alias already defined: [" + xmlFile + " : " + privLabel.getName() + "] " + hostAlias);
                        this._setHasParsingErrors(xmlFile);
                    }
                    hasDuplicates = true;
                }
            }
            if (!hasDuplicates) {
                // no duplicates
                if (tempPLMap.containsKey(BasicPrivateLabel.DEFAULT_HOST)) {
                    if (this.defaultPrivateLabel == null) {
                        this.defaultPrivateLabel = privLabel;
                    } else {
                        // we already have a default
                        printWarn("Default host already defined: [" + xmlFile + ":" + privLabel.getName() + "] " + host);
                        this._setHasParsingWarnings(xmlFile);
                    }
                }
                this.privateLabelMap.putAll(tempPLMap);
            }

        }
    }

    /**
    *** Gets the Map of managed BasicPrivateLabel instances
    *** @return The Map of managed BasicPrivateLabel instances
    **/
    protected Map<String,BasicPrivateLabel> getPrivateLabelMap()
    {
        return this.privateLabelMap;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Instanciates a BasicPrivateLabel instance from the specified class name
    *** @param className  The class name of the BasicPrivateLabel to instantiate
    *** @param hostName   The host name to assign to assign to the new BasicPrivateLabel
    *** @return The new BasicPrivateLabel instance
    **/
    protected BasicPrivateLabel createPrivateLabel(File xmlFile, String className, String hostName)
    {
        if (StringTools.isBlank(className)) {
            return this.createPrivateLabel(xmlFile, hostName);
        } else {
            try {
                Class labelClass = Class.forName(className);
                BasicPrivateLabel pl = (BasicPrivateLabel)labelClass.newInstance();
                if (hostName != null) {
                    pl.setHostName(hostName);
                }
                return pl;
            } catch (Throwable t) { // ClassNotFoundException, ClassCastException, etc.
                printError("BasicPrivateLabel creation error: " + className + " [" + t);
                this._setHasParsingErrors(xmlFile);
                return this.createPrivateLabel(xmlFile, hostName);
            }
        }
    }

    // ------------------------------------------------------------------------

    public static boolean               SAVE_I18N_STRINGS = false;
    public static Set<I18N.Text>        I18N_STRINGS      = null;
    public static Map<String,I18N.Text> I18N_STRINGS_MAP  = null;

    /**
    *** Create an I18N.Text wrapper with the specified key and default text
    *** @param xmlFile  The XML file in which this String was defined.
    *** @param pkgName  The package containing the "LocalStrings_XX.properties" files.
    *** @param i18nKey  The key used to look up the localized string.
    *** @param dftStr   The default text to return if the key is not found.
    *** @return The enocded I18N string
    **/
    protected static I18N.Text parseI18N(File xmlFile, String pkgName, String i18nKey, String dftStr)
    {
        return parseI18N(xmlFile, pkgName, i18nKey, dftStr, true);
    }
    
    /**
    *** Create an I18N.Text wrapper with the specified key and default text
    *** @param xmlFile  The XML file in which this String was defined.
    *** @param pkgName  The package containing the "LocalStrings_XX.properties" files.
    *** @param i18nKey  The key used to look up the localized string.
    *** @param dftStr   The default text to return if the key is not found.
    *** @param showError  If true, a stacktrace will be display if the key is invalid.
    *** @return The enocded I18N string
    **/
    protected static I18N.Text parseI18N(File xmlFile, String pkgName, String i18nKey, String dftStr, boolean showError)
    {

        /* no key/value? */
        if (StringTools.isBlank(i18nKey) && StringTools.isBlank(dftStr)) {
            // quietly ignore
            return null;
        }

        /* create/return I18N text */
        I18N.Text text = I18N.parseText(pkgName, i18nKey, dftStr.trim(), showError);
        if (SAVE_I18N_STRINGS && !StringTools.isBlank(i18nKey)) {
            // create map
            if (I18N_STRINGS == null) { 
                I18N_STRINGS     = new OrderedSet<I18N.Text>(); 
                I18N_STRINGS_MAP = new OrderedMap<String,I18N.Text>(); 
            }
            // add key/value to map
            String textKey = text.getKey();
            if (!I18N_STRINGS_MAP.containsKey(textKey)) {
                I18N_STRINGS.add(text);
                I18N_STRINGS_MAP.put(textKey, text);
                //printInfo("I18N: %s=%s", textKey, text.getDefault());
            } else {
                I18N.Text oldText = I18N_STRINGS_MAP.get(textKey);
                if (text.equals(oldText)) {
                    // duplicate key - just ignore
                } else {
                    printInfo("I18N: key already defined (different value) - " + textKey);
                }
            }
        }
        return text;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Debug/Testing entry point
    *** @param argv The command-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        RTConfig.setDebugMode(true);
        Print.setLogLevel(Print.LOG_ALL);
        Print.setLogHeaderLevel(Print.LOG_ALL);

        if (RTConfig.hasProperty("url")) {
            String urlStr = RTConfig.getString("url","");
            try {
                URL url = new URL(urlStr);
                String host = StringTools.trim(url.getHost());
                String path = url.getPath();
                Print.sysPrintln("Host=%s  Path=%s", host, path);
                if (path.startsWith("/")) {
                    int p = path.indexOf("/",1);
                    String hp = host + ((p > 0)? path.substring(0,p) : path);
                    Print.sysPrintln("HostPath=%s", hp);
                }
            } catch (Throwable th) {
                Print.logException("Bad URL", th);
            }
            System.exit(1);
        }

        File xmlFile = RTConfig.getFile("xml",null);
        if (xmlFile != null) {
            BasicPrivateLabelLoader._getInstance()._resetLoadXML(xmlFile);
        } else {
            BasicPrivateLabelLoader._getInstance()._resetLoadDefaultXML();
        }

        BasicPrivateLabel privateLabel = BasicPrivateLabelLoader.getPrivateLabel("*");
        Print.sysPrintln("Found default BasicPrivateLabel: " + (privateLabel != null));

    }

}
