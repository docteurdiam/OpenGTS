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
// Description:
//  This class performs post installation checks on the OpenGTS installation.
// ----------------------------------------------------------------------------
// Change History:
//  2008/02/17  Martin D. Flynn
//     -Initial release
//  2008/05/14  Martin D. Flynn
//     -Added additional 'private.xml' checks
//     -Additional changes to support Java 6.
//  2008/09/12  Martin D. Flynn
//     -Displays RuleFactory and PingDispatcher, if configured.
//  2008/12/01  Martin D. Flynn
//     -Added check for Cygwin directory symbolic links
//     -Display additional private-label Domain attributes
//  2009/01/01  Martin D. Flynn
//     -Added display of SMTP information
//  2009/01/28  Martin D. Flynn
//     -Added compile time
//     -Added character encoding information
//  2009/02/20  Martin D. Flynn
//     -Added check for initial SMTP host:port connection (3 second timeout)
//  2009/04/02  Martin D. Flynn
//     -Added check for "root" db username
//     -Added "Service Account" header.
//  2009/05/01  Martin D. Flynn
//     -Skip SMTP socket test if no SMTP host specified
//     -Added JAVA_HOME check for JRE
//     -Added CATALINA_HOME check for proper Tomcat installation
//     -Added check for 'readability' for various required library jars.
//     -Added check for running checkInstall as 'root'.
//  2009/05/24  Martin D. Flynn
//     -Added check for executable Tomcat startup/shutdown files.
//  2009/05/27  Martin D. Flynn
//     -Now insists on Java 6+
//  2009/06/01  Martin D. Flynn
//     -Removed check for 'activation.jar' (already present in Java 6)
//     -Added check for JavaMail, and SendMailArgs
//     -Perform additional checks on the comparison of JAVA_HOME vs PATH
//     -Attempt to compare private.xml with deployed track.war private.xml
//  2009/07/01  Martin D. Flynn
//     -Added cmd-line option ("localStrings") for LocalStrings_*.properties 
//      validation (validateLocalStrings).  Checks for invalid unicode-escaped
//      characters, and non-'ISO-8859-1' characters.
//     -Added ability to send a test email ("sendTestEmailTo").
//  2009/11/10  Martin D. Flynn
//     -Added check for 'private.xml' property "reportMenu.enableReportEmail".
//  2009/12/16  Martin D. Flynn
//     -Added list of defined reports.
//     -Added summary listing of warnings
//  2011/01/28  Martin D. Flynn
//     -Added symbolic link recommendations
//  2011/04/01  Martin D. Flynn
//     -Added check for non-readable files in Tomcat directory
//     -Added check for non-read/writable files in Log directory
//  2011/05/13  Martin D. Flynn
//     -Added check for MySQL "max-connections".
//  2011/06/16  Martin D. Flynn
//     -Added runtime config option to skip counting records
//  2011/07/01  Martin D. Flynn
//     -Added MobileLocationProvider information
//  2011/10/03  Martin D. Flynn
//     -Changed "max-connections" check in "my.cnf" to look for 'lastIndexOF("=")'
//      rather than 'indexOf("=")'.
// ----------------------------------------------------------------------------
package org.opengts.tools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import java.awt.Font;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.CompileTime;
import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.geocoder.ReverseGeocodeProvider;
import org.opengts.geocoder.GeocodeProvider;
import org.opengts.cellid.MobileLocationProvider;

import org.opengts.war.tools.*;

import org.opengts.war.report.ReportFactory;

public class CheckInstall
{

    // ------------------------------------------------------------------------

    private static final String  ARG_privateLabelDetail         = "privateLabelDetail";

    private static final String  PFX                            = "    ";

    private static final String  TRACK_CLASS_DIR                = "./build/track/WEB-INF/classes";

    private static final int     MAX_WIDTH                      = 85;

    // ------------------------------------------------------------------------

    private static final String  PROP_skipDefaultEMailChecks[]  = new String[] { "checkInstall.skipDefaultEMailChecks", "skipDefaultEMailChecks", "skipEMailChecks" };
    private static final String  PROP_skipDefaultMapChecks[]    = new String[] { "checkInstall.skipDefaultMapChecks"  , "skipDefaultMapChecks"  , "skipMapChecks"   };
    private static final String  PROP_skipDBRecordCount[]       = new String[] { "checkInstall.skipDBRecordCount"     , "skipDBRecordCount"     , "noRecordCount"   };

    // ------------------------------------------------------------------------

    private static final String  ENVIRON_GTS_HOME               = DBConfig.env_GTS_HOME;
    private static final String  ENVIRON_GTS_CONF               = "GTS_CONF";
    private static final String  ENVIRON_GTS_CHARSET            = "GTS_CHARSET";
    private static final String  ENVIRON_JAVA_HOME              = "JAVA_HOME";
    private static final String  ENVIRON_ANT_HOME               = "ANT_HOME";
    private static final String  ENVIRON_CATALINA_HOME          = "CATALINA_HOME";
    private static final String  ENVIRON_MYSQL_HOME             = "MYSQL_HOME";
    private static final String  ENVIRON_CLASSPATH              = "CLASSPATH";
    private static final String  ENVIRON_PATH                   = "PATH";

    // ------------------------------------------------------------------------

    private static final String  REASON_DIR_NOT_EXIST           = "Java '<File>.isDirectory()' returned false";
    private static final String  REASON_FILE_NOT_EXIST          = "Java '<File>.isFile()' returned false";
    private static final String  REASON_SYSTEM_ERROR            = "Possible internal system error";

    private static final String  FIX_JAVA_VERSION               = "Please install Sun Microsystems Java version 1.6 (ie. 'Java 6')";
    private static final String  FIX_VALID_DIRECTORY            = "Please specify a valid directory path";
    private static final String  FIX_VALID_FILE                 = "Please specify a valid file path";
    private static final String  FIX_PREVIOUS_ERRORS            = "Fix previous errors, then re-run this installation check.";

    private static java.util.List<String[]> _errors = new Vector<String[]>();
    private static java.util.List<String[]> getErrors()
    {
        return _errors;
    }
    private static void clearErrors()
    {
        getErrors().clear();
    }
    private static void addError(String error, String reason, String fix, boolean fatal)
    {
        if (fatal) {
            getErrors().add(new String[] { error, reason, fix });
        } else {
            getErrors().add(new String[] { error, reason, fix, "false" });
        }
    }
    private static void addError(String error, String reason, String fix)
    {
        addError(error, reason, fix, true);
    }

    // ------------------------------------------------------------------------

    private static int                      warnCount = 0;
    private static java.util.List<String>   warnList  = new Vector<String>();
    
    private static int warnCount()
    {
        return warnCount;
    }
    
    private static java.util.List<String> getWarnings()
    {
        return warnList;
    }

    private static int countWarning(String msg)
    {
        int wc = ++warnCount;
        warnList.add(wc + ") " + msg);
        return wc;
    }

    // ------------------------------------------------------------------------
    
    public interface OutputHandler
    {
        public void checkInstallOutput(String m);
    }
    
    private static OutputHandler outputHandler = null;
    
    /* set output delegate */
    public static void setOutputHandler(final OutputHandler output)
    {
        if (output == null) {
            CheckInstall.outputHandler = null;
            BasicPrivateLabelLoader.setOutputHandler(null);
        } else {
            CheckInstall.outputHandler = output;
            BasicPrivateLabelLoader.setOutputHandler(new BasicPrivateLabelLoader.OutputHandler() {
                public void privateLabelOutput(String s) {
                    output.checkInstallOutput(s);
                }
            });
        }
    }

    /* output line to stdout */
    private static void println(String s)
    {
        if (outputHandler != null) {
            outputHandler.checkInstallOutput(s);
        } else {
            Print.sysPrintln(s);
        }
    }

    private static void wrapPrintln(String s, char sep)
    {

        /* extract prefixing spaces */
        int pfxNdx = 0;
        while (Character.isWhitespace(s.charAt(pfxNdx))) { pfxNdx++; }
        String prefix = s.substring(0, pfxNdx) + "  ";

        /* wrap */
        while (s.length() > MAX_WIDTH) {
            int ch = MAX_WIDTH;
            while ((ch > 0) && (s.charAt(ch) != sep)) { ch--; }
            if (ch > 0) {
                println(s.substring(0,ch+1));
                s = prefix + s.substring(ch+1).trim();
            } else {
                break;
            }
        }

        /* final line */
        if (s.length() > 0) {
            println(s);
        }

    }

    // ------------------------------------------------------------------------

    /* print a variable/key and it's value */
    private static void printVariable(String name, Object val, Object note)
    {
        int tab = 22, len = 2 + tab + 5;
        String nameFmt = "  " + StringTools.leftAlign(name,tab) + " ==> ";
        String v = (val  != null)? val.toString()  : "";
        String n = (note != null)? note.toString() : "";
        if (StringTools.isBlank(n)) {
            println(nameFmt + v);
        } else
        if (StringTools.isBlank(v)) {
            println(nameFmt + n);
        } else
        if ((nameFmt + v + "  " + n).length() < MAX_WIDTH) {
            println(nameFmt + v + "  " + n);
        } else {
            println(nameFmt + v);
            println(StringTools.replicateString(" ",len) + n);
        }
    }

    // ------------------------------------------------------------------------

    /* return the canonical directory for the specified environment variable */
    private static File getEnvironmentFile(String name, boolean isDirectory, boolean errorIfMissing)
    {
        
        /* get value */
        String val = null;
        try {
            val = System.getenv(name);
            if (StringTools.isBlank(val)) {
                if (errorIfMissing) {
                    printVariable(name, "", "(ERROR: not defined)");
                    addError("Environment variable '"+name+"' is not defined.", 
                             null,
                             "Please define the specified environment variable");
                } else {
                    printVariable(name, "", "(NOTE: not defined)");
                }
                return null;
            }
            if ((val.indexOf("\"") >= 0) || (val.indexOf("\'") >= 0)) {
                //val = StringTools.stripChars(val, '\"');
                printVariable(name, val, "(ERROR: contains quotes)");
                addError("Directory specification '"+name+"' contains quote characters.", 
                         null,
                         "Remove quotes from directory specification");
                return null;
            }
        } catch (Error err) {
            printVariable(name, "", "(ERROR: error retrieving environment variable)");
            addError("Error retrieving environment variable '"+name+"'.", 
                     "Possible invalid version of Java installed",
                     FIX_JAVA_VERSION);
            return null;
        }

        /* check for existance */
        File dir = new File(val);
        if (isDirectory) {
            if (!dir.isDirectory()) {
                File dirLnk = new File(val + ".lnk");
                if (File.separator.equals("\\") && dirLnk.isFile()) {
                    printVariable(name, val, "(ERROR: possible Cygwin symbolic link)");
                    addError("Environment variable '"+name+"' specifies a Cygwin symbolic link.", 
                             "Directory appears to be a Cygwin symbolic link",
                             "Please change environment value to a DOS absolute/canonical path");
                    return null;
                } else {
                    printVariable(name, val, "(ERROR: invalid directory)");
                    addError("Environment variable '"+name+"' specifies an invalid directory.", 
                             REASON_DIR_NOT_EXIST,
                             FIX_VALID_DIRECTORY);
                    return null;
                }
            }
        } else {
            if (!dir.isFile()) {
                printVariable(name, val, "(ERROR: invalid file)");
                addError("Environment variable '"+name+"' specifies an invalid file.", 
                         REASON_FILE_NOT_EXIST,
                         FIX_VALID_FILE);
                return null;
            }
        }

        /* canonical directory */
        try {
            dir = dir.getCanonicalFile();
        } catch (IOException ioe) {
            printVariable(name, val, "(ERROR: canonical error)");
            addError("Error retrieving canonical directory for environment variable '"+name+"'.", 
                     REASON_SYSTEM_ERROR,
                     null);
            return null;
        }

        /* return directory */
        return dir;
        
    }
    
    // ------------------------------------------------------------------------

    /* print all defined system properties */
    private static void printSystemProperties()
    {
        Properties props = System.getProperties();
        for (Enumeration n = props.propertyNames(); n.hasMoreElements();) {
            String key = (String)n.nextElement();
            String val = props.getProperty(key);
            println(key + " ==> " + val);
        }
    }

    // ------------------------------------------------------------------------

    /* return true if class is a proprietary GTS class */
    private static boolean isGtsClass(String className)
    {
        if (className.startsWith("org.opengts.rule.")) {
            return true; // possible
        } else
        if (className.startsWith("org.opengts.opt.")) {
            return true; // possible
        } else
        if (className.startsWith("org.opengts.priv.")) {
            return true; // unlikely
        } else {
            return false;
        }
    }

    private static String ClassName(Object clazz)
    {
        return ClassName(StringTools.className(clazz));
    }

    private static String ClassName(String className)
    {
        if (isGtsClass(className)) {
            return "GTS:" + className;
        } else {
            return className;
        }
    }

    // ------------------------------------------------------------------------

    private static File getLikelyWindowsJDK(File path)
    {
        File jdkDirPath = null;
        if (OSTools.isWindows()) {
            File dir = (path != null)? path : new File("C:/Program Files/Java");
            String fileList[] = ListTools.sort(dir.list()); // ie. jdk1.6.0_14
            for (int i = 0; i < fileList.length;  i++) {
                if (fileList[i].startsWith("jdk")) {
                    jdkDirPath = new File(dir, fileList[i]);
                    //println(PFX+"Found JDK dir: '" + jdkDirPath + "'");
                }
            }
        }
        return jdkDirPath;
    }

    // ------------------------------------------------------------------------

    private static final String LS_FILE_PFX     = "  ";
    private static final String LS_ERROR_PFX    = "    ==> ERROR: ";

    private static void validateLocalStrings(File dir)
    {
        Print.sysPrintln("Verifying 'LocalStrings_XX.properties' files ...");
        if (dir == null) {
            Print.sysPrintln(LS_ERROR_PFX + "Specified file/directory does not exist: null");
        } else
        if (dir.isFile()) {
            int count = _validateLocalStrings(new File[] { dir }, null);
            if (count <= 0) {
                Print.sysPrintln(LS_ERROR_PFX + "Not a 'LocalStrings_XX.properties' file");
            }
       } else
        if (dir.isDirectory()) {
            Print.sysPrintln("Directory: " + dir);
            int count = _validateLocalStrings(new File[] { dir }, null);
            if (count <= 0) {
                Print.sysPrintln(LS_ERROR_PFX + "No LocalStrings files found");
            }
        } else {
            Print.sysPrintln(LS_ERROR_PFX + "File/Directory does not exist: " + dir);
        }
    }

    private static int _validateLocalStrings(File files[], java.util.List<File> badPropFiles)
    {
        int count = 0;
        boolean verbose = (badPropFiles == null);

        /* look for LocalStrings_XX.properties in list */
        for (int i = 0; i < files.length; i++) {
            if ((files[i] == null) || !files[i].isFile()) { continue; }
            String n = files[i].getName();
            if (n.startsWith("LocalStrings_") && n.endsWith(".properties")) {
                count++;
                if (verbose) {
                    Print.sysPrintln(LS_FILE_PFX + files[i] + " ...");
                }
                // check for invalid unicode-escaped chars
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(files[i]);
                    Properties props = new Properties();
                    props.load(fis); // "ISO-8859-1" only
                } catch (Throwable th) {
                    if (badPropFiles != null) {
                        badPropFiles.add(files[i]);
                    } else
                    if (verbose) {
                        Print.sysPrintln(LS_ERROR_PFX + th.getMessage());
                    }
                } finally {
                    try { fis.close(); } catch (Throwable th) {/*ignore*/}
                }
                // check for invalid chars
                try {
                    byte data[] = FileTools.readFile(files[i]);
                    if (data != null) {
                        int line = 1;
                        StringBuffer lineBuff = new StringBuffer();
                        boolean badChar = false;
                        for (int b = 0; b < data.length; b++) {
                            int ch = (int)data[b] & 0xFF;
                            if (ch == '\n') {
                                if (badChar) {
                                    // display error at end of line
                                    Print.sysPrintln(LS_ERROR_PFX + "Invalid characters at line #" + line);
                                }
                                badChar = false;
                                lineBuff.setLength(0);
                                line++;
                            } else
                            if (ch == '\r') {
                                // allowed space characters
                            } else
                            if (ch == '\t') {
                                // allowed space characters
                                lineBuff.append((char)ch);
                            } else
                            if ((ch >= ' ') && (ch <= '~')) {
                                // allowed ascii characters
                                lineBuff.append((char)ch);
                            } else {
                                // invalid chars
                                badChar = true;
                            }
                        }
                    }
                } catch (Throwable th) {
                    if (badPropFiles != null) {
                        badPropFiles.add(files[i]);
                    } else
                    if (verbose) {
                        Print.sysPrintln(LS_ERROR_PFX + th.getMessage());
                    }
                } finally {
                    try { fis.close(); } catch (Throwable th) {/*ignore*/}
                }
            }
        }

        /* drop into subdirectories */
        for (int i = 0; i < files.length; i++) {
            if ((files[i] == null) || !files[i].isDirectory()) { continue; }
            File subFiles[] = ListTools.sort(files[i].listFiles(),null);
            count += _validateLocalStrings(subFiles, badPropFiles);
        }

        /* return number of LocalStrings_XX.properties files found */
        return count;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Command-Line Options:
    //  -localStrings=<dir>
    //      Recursively descends through the specified directory validating all 
    //      "LocalStrings_XX.properties" files found.  Useful when creating or
    //      modifying your own localized language files.
    //  -sendTestEmailTo=<addr>
    //      If specified, this will indicate to CheckInstall that it should also
    //      attempt to send a test email to the specified email address.
    
    private static final String ARG_LOCAL_STRINGS[]     = new String[] { "localStrings"   , "ls"       };
    private static final String ARG_SEND_TEST_EMAIL[]   = new String[] { "sendTestEmailTo", "sendMail" };

    /* main entry point */
    public static void main(String argv[])
    {
        RTConfig.setWebApp(true);
        BasicPrivateLabelLoader.setTrackServlet_debugOnly();
        DBConfig.cmdLineInit(argv, true);
        Print.setLogLevel(Print.LOG_WARN, false, false);
        boolean isEnterprise = DBConfig.hasExtraPackage();
        boolean isWindows = OSTools.isWindows();
        StringBuffer recommendations = new StringBuffer();

        /* special check for 'LocalStrings_XX.properties' validation */
        if (RTConfig.hasProperty(ARG_LOCAL_STRINGS)) {
            File dir = RTConfig.getFile(ARG_LOCAL_STRINGS,null);
            validateLocalStrings(dir);
            System.exit(0);
        }
        
        /* check for sending a test email message */
        String sendTestEmailTo = RTConfig.getString(ARG_SEND_TEST_EMAIL,null);

        /* environment vars */
        File env_GTS_HOME      = null;  // $GTS_HOME
        File env_GTS_CONF      = null;  // $GTS_CONF
        File env_JAVA_HOME     = null;  // $JAVA_HOME
        File env_ANT_HOME      = null;  // $ANT_HOME
        File env_CATALINA_HOME = null;  // $CATALINA_HOME

        /* clear errors */
        clearErrors();

        /* begin */
        println("");
        int sepWidth = MAX_WIDTH;
        String eqSep = StringTools.replicateString("=",sepWidth);

        /* print all system properties? */
        if (RTConfig.hasProperty("props")) {
            printSystemProperties();
            System.exit(0);
        }

        /* separator */
        println(eqSep);

        /* ServiceAccount ID/Name */
        if (RTConfig.hasProperty(DBConfig.PROP_ServiceAccount_ID)) {
            String srvID   = DBConfig.getServiceAccountID("?");
            String srvName = DBConfig.getServiceAccountName("?");
            println("Service Account: [" + srvID + "] " + srvName);
            println(eqSep);
        }

        /* Java vendor/version */
        println("");
        println(isEnterprise? "GTS Enterprise:" : "OpenGTS:");
        {
            // Version
            printVariable("(Version)", DBConfig.getVersion(), (isEnterprise?"(enterprise)":""));
            // Compiletime
            printVariable("(Compiled Time)", (new DateTime(CompileTime.COMPILE_TIMESTAMP)).toString(), "");
            // Current time
            printVariable("(Current Time)", (new DateTime()).toString(), "");
            // Current user
            String userName = System.getProperty("user.name","?");
            if (userName.equalsIgnoreCase("root")) {
                printVariable("(Current User)", userName, "(ERROR: should not be 'root')");
                addError("This application is being run as superuser 'root'.",
                         "This application should be run under a user other than 'root'.",
                         "Change to a different user when running GTS/OpenGTS.");
            } else {
                printVariable("(Current User)", userName, "");
            }
            // ServiceAccount.ID
            String saIDKey = DBConfig.PROP_ServiceAccount_ID;
            printVariable(saIDKey, RTConfig.getString(saIDKey,"?"), "");
            // ServiceAccount.Name
            String saNameKey = DBConfig.PROP_ServiceAccount_Name;
            printVariable(saNameKey, RTConfig.getString(saNameKey,"?"), "");
            // ServiceAccount.Type
            String saTypeKey = DBConfig.PROP_ServiceAccount_Attr;
            if (RTConfig.hasProperty(saTypeKey)) {
            printVariable(saTypeKey, RTConfig.getString(saTypeKey,"?"), "");
            }
            // ServiceAccount.Key
            String saKeyKey = DBConfig.PROP_ServiceAccount_Key;
            if (RTConfig.hasProperty(saKeyKey)) {
            printVariable(saKeyKey, RTConfig.getString(saKeyKey,"?"), "");
            }
        }

        /* System info */
        println("");
        println("System Information:");
        {
            // os.arch
            String osArchKey = "os.arch";
            printVariable(osArchKey, System.getProperty(osArchKey,"?"), "");
            // os.name
            String osNameKey = "os.name";
            printVariable(osNameKey, System.getProperty(osNameKey,"?"), "");
            // os.version
            String osVersKey = "os.version";
            printVariable(osVersKey, System.getProperty(osVersKey,"?"), "");
            // "/etc/issue"
            //   Fedora release 12 (Constantine)
            //   Kernel \r on an \m (\l)
            File issueFile = new File("/etc/issue");
            if (issueFile.isFile()) {
                String issue = StringTools.toStringValue(FileTools.readFile(issueFile));
                String I[]   = StringTools.parseString(issue,"\r\n"); 
                printVariable(issueFile.toString(), I[0], "");
            } else {
                printVariable(issueFile.toString(), "(not present)", "");
            }
            // "/usr/bin/free | grep Mem:"
            long memMeg = 0L;
            try {
                File linuxFreeCmd = new File("/usr/bin/free");
                if (linuxFreeCmd.isFile()) {
                    Process ppidExec = Runtime.getRuntime().exec("/usr/bin/free -m | grep -v Mem:");
                    BufferedReader ppidReader = new BufferedReader(new InputStreamReader(ppidExec.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    for (;;) {
                        String line = ppidReader.readLine();
                        if (line == null) { break; }
                        sb.append(StringTools.trim(line));
                    }
                    String M[] = StringTools.parseString(sb.toString()," \r\n"); 
                    memMeg = StringTools.parseLong(M[1],0L);
                    int exitVal = ppidExec.waitFor();
                    ppidReader.close();
                }
            } catch (Throwable th) {
                memMeg = -1L;
            }
            if (memMeg > 0L) {
                printVariable("Total Memory", StringTools.format((double)memMeg,"0.0")+" mb", "");
            } else
            if (memMeg < 0L) {
                printVariable("Total Memory", "(unable to obtain)", "");
            } else {
                printVariable("Total Memory", "(not available)", "");
            }
        }

        /* Java vendor/version */
        boolean isJava6plus = false;
        File javaInstallDir = null;
        File mostLikelyWinJDK = null;
        println("");
        println("Java Version (the JRE running this program):");
        {
            // Check Java vendor
            String javaVendKey = "java.vendor";
            String javaVendVal = System.getProperty(javaVendKey);                  // "Sun Microsystems Inc."
            if ((javaVendVal == null) || 
	            ((javaVendVal.indexOf("Sun Microsystems") < 0) && 
	             (javaVendVal.indexOf("Apple")            < 0)    )) {
                // On the Mac (OS X), this String may be "Apple Inc.", which appears to work fine.
                printVariable("(Vendor)", javaVendKey, "(ERROR: not a Sun Microsystems version!)");
                addError("This is not a 'Sun Microsystems, Inc' version of Java.",
                         "Sun Microsystems Java not installed, or not referenced in executable path",
                         FIX_JAVA_VERSION);
            } else {
                printVariable("(Vendor)", javaVendVal, "");
            }
            // Display Java version
            //String javaVersKey = "java.version";
            //String javaVersVal = System.getProperty(javaVersKey);    // "1.5.0_06"
            //printVariable(javaVersKey, javaVersVal, "");
            // Check specification version
            String javaSpecKey = "java.specification.version";
            String javaSpecVal = StringTools.trim(System.getProperty(javaSpecKey)); // "1.6" / "1.7"
            if (javaSpecVal.startsWith("1.5")) {
                printVariable("(Version)", javaSpecVal, "(ERROR: requires 1.6+ to run properly)");
                addError("This Java version may no longer be supported ("+javaSpecVal+").",
                         "Supported version of Java is not installed, or is not referenced in executable path",
                         FIX_JAVA_VERSION);
            } else
            if (javaSpecVal.startsWith("1.6")) {
                printVariable("(Version)", javaSpecVal, ""); // recommended version
                isJava6plus = true;
            } else
            if (javaSpecVal.startsWith("1.7")) {
                int WC = countWarning("Not fully tested with Java 1.7");
                printVariable("(Version)", javaSpecVal, "(WARNING["+WC+"]: not yet fully tested with 1.7)");
                isJava6plus = true;
            } else {
                printVariable("(Version)", javaSpecVal, "(ERROR: invalid version)");
                addError("This Java version is not supported ("+javaSpecVal+").",
                         "Supported version of Java is not installed, or is not referenced in executable path",
                         FIX_JAVA_VERSION);
            }
            // Check installation directory (System property "java.home")
            String javaHomeKey = "java.home";
            String javaHomeVal = System.getProperty(javaHomeKey,"");
            try {
                File javaHomeDir = !javaHomeVal.equals("")? (new File(javaHomeVal)).getCanonicalFile() : null;
                if (javaHomeDir != null) {
                    javaInstallDir = javaHomeDir.getName().equals("jre")? javaHomeDir.getParentFile() : javaHomeDir;
                    String javaInstallDirStr = javaInstallDir.toString(); // + "jre"; // <-- testing
                    boolean isJavaPathJRE = (StringTools.indexOfIgnoreCase(javaInstallDirStr, "jre") >= 0);
                    if (isJavaPathJRE) {
                        printVariable("(Install dir)", javaInstallDir, "(ERROR: 'PATH' points to the JRE, rather than the JDK)");
                        //String envPATH = StringTools.blankDefault(System.getenv(ENVIRON_PATH),"?");
                        //wrapPrintln(PFX+ENVIRON_PATH+"="+envPATH, File.pathSeparatorChar);
                        if (isWindows) {
                            mostLikelyWinJDK = getLikelyWindowsJDK(javaInstallDir.getParentFile());
                            if (mostLikelyWinJDK != null) {
                                String JavaHome = System.getenv(ENVIRON_JAVA_HOME);
                                if  ((JavaHome != null) && JavaHome.equals(mostLikelyWinJDK.toString())) {
                                    println(PFX+"('PATH' should be prefixed with '%JAVA_HOME%\\bin')");
                                } else {
                                    println(PFX+"('PATH' should likely be prefixed with '" + mostLikelyWinJDK + "\\bin')");
                                }
                            }
                        }
                        addError("The 'PATH' environment variable points to the JRE, rather than the JDK.",
                                 "The 'PATH' environment variable points to the JRE (Java Runtime Environment), rather than " + 
                                 "the JDK (Java Developer Kit).  The JDK already contains the JRE, so a separate JRE insallation " +
                                 " is not necessary.",
                                 "Set the 'PATH' environment variable to point to the JDK installation bin directory.");
                    } else {
                        printVariable("(Install dir)", javaInstallDir.toString(), "");
                    }
                } else {
                    javaInstallDir = null;
                }
            } catch (IOException ioe) {
                javaInstallDir = null;
            }
            if (javaInstallDir == null) {
                printVariable(javaHomeKey, javaHomeVal, "(ERROR: unable to determine Java installation dir)");
                addError("Unable to resolve the Java installation directory from '"+javaHomeVal+"'.",
                         "Error encountered while attempting to determine the Java installation directory",
                         null);
            }
            // Check java.awt.headless
            String javaHeadKey = "java.awt.headless";
            String javaHeadVal = System.getProperty(javaHeadKey,"false");
            printVariable(javaHeadKey, javaHeadVal, "");
            // Font check
            try {
                Font font = new Font(PushpinIcon.DEFAULT_TEXT_FONT, Font.PLAIN, 10);
                printVariable("(Has Fonts)", "true", "");
            } catch (Throwable th) {
                int WC = countWarning("Unable to load Fonts");
                printVariable("(Has Fonts)", "false", "(WARNING["+WC+"]: unable to load fonts)");
            }
            // MD5 check
            try {
                java.security.MessageDigest.getInstance("MD5");
                printVariable("(Supports MD5)", "true", "");
            } catch (java.security.NoSuchAlgorithmException nsae) {
                printVariable("(Supports MD5)", "false", "");
            }
        }

        /* environment directories */
        println("");
        println("Environment variable paths (canonical):");
        {
            // GTS_HOME
            env_GTS_HOME = getEnvironmentFile(ENVIRON_GTS_HOME, true, true);
            if (env_GTS_HOME != null) {
                String userDirPath = System.getProperty("user.dir","");
                try {
                    File userDir = !userDirPath.equals("")? (new File(userDirPath)).getCanonicalFile() : null;
                    if (!env_GTS_HOME.equals(userDir)) {
                        printVariable(ENVIRON_GTS_HOME, env_GTS_HOME, "(ERROR: does not match the current directory)");
                        addError("'GTS_HOME' does not match the current directory '"+userDir+"'.",
                                 "This installation check must be executed from directory '"+env_GTS_HOME+"'",
                                 "Change the environment variable 'GTS_HOME', or cd to '"+env_GTS_HOME+"'.");
                    } else {
                        printVariable(ENVIRON_GTS_HOME, env_GTS_HOME, "");
                    }
                } catch (IOException ioe) {
                    printVariable(ENVIRON_GTS_HOME, env_GTS_HOME, "(ERROR: unable to determine current directory)");
                    addError("Unable to resolve the current directory from '"+userDirPath+"'.",
                             "Error encountered while attempting to determine current directory",
                             null);
                }
            }
            // GTS_CONF
            env_GTS_CONF = getEnvironmentFile(ENVIRON_GTS_CONF, false, false);
            if (env_GTS_CONF != null) {
                // TODO: check to make sure that 'env_GTS_HOME' is the parent of 'env_GTS_CONF'
                printVariable(ENVIRON_GTS_CONF, env_GTS_CONF, "");
            }
            // JAVA_HOME
            env_JAVA_HOME = getEnvironmentFile(ENVIRON_JAVA_HOME, true, true);        // "/opt/sun-jdk-1.5.0.06"
            if (env_JAVA_HOME != null) {
                String env_JAVA_HOME_name = env_JAVA_HOME.getName();
                boolean isJavaEnvJRE = (StringTools.indexOfIgnoreCase(env_JAVA_HOME_name, "jre") >= 0);
                if (isJavaEnvJRE) {
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "(ERROR: points to the JRE, rather than the JDK)");
                    if (isWindows) {
                        File likelyJDK = (mostLikelyWinJDK != null)? mostLikelyWinJDK : getLikelyWindowsJDK(null);
                        if (likelyJDK != null) {
                            println(PFX+"('JAVA_HOME' should likely be set to '" + likelyJDK + "')");
                        }
                    }
                    addError("'JAVA_HOME' points to the JRE, rather than the JDK.",
                             "The 'JAVA_HOME' environment variable points to the JRE (Java Runtime Environment), rather than " + 
                             "the JDK (Java Developer Kit).  The JDK already contains the JRE, so a separate JRE insallation " +
                             " is not necessary.",
                             "Set JAVA_HOME to point to the JDK installation directory.");
                } else
                if (javaInstallDir == null) {
                    int WC = countWarning("Cannot compare JAVA_HOME to Java Install directory");
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "(WARNING["+WC+"]: could not compare to Java install dir)");
                } else
                if (!javaInstallDir.equals(env_JAVA_HOME)) {
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "(ERROR: does not match Java install dir)");
                    addError("'JAVA_HOME' does not match the Java installation 'PATH' directory '"+javaInstallDir+"'.",
                             "The version of Java referenced in the executable 'PATH' environment variable does not match 'JAVA_HOME'.",
                             "Make sure both the 'JAVA_HOME' and 'PATH' environment variables point to the same installed JDK.");
                } else {
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "");
                }
            } else {
                if (isWindows) {
                    File likelyJDK = (mostLikelyWinJDK != null)? mostLikelyWinJDK : getLikelyWindowsJDK(null);
                    if (likelyJDK != null) {
                        println(PFX+"('JAVA_HOME' should likely be set to '" + likelyJDK + "')");
                    }
                }
            }
            // ANT_HOME
            env_ANT_HOME = getEnvironmentFile(ENVIRON_ANT_HOME, true, false);
            if (env_ANT_HOME != null) {
                printVariable(ENVIRON_ANT_HOME, env_ANT_HOME, "");
            }
            // CATALINA_HOME
            env_CATALINA_HOME = getEnvironmentFile(ENVIRON_CATALINA_HOME, true, true);    // "/opt/jakarta-tomcat-5.0.28"
            if (env_CATALINA_HOME == null) {
                // error already printed
                //printVariable(ENVIRON_CATALINA_HOME, "", "(Note: not defined)");
            } else
            if (!env_CATALINA_HOME.isDirectory()) {
                int WC = countWarning("'CATALINA_HOME' does not specify a directory");
                printVariable(ENVIRON_CATALINA_HOME, env_CATALINA_HOME, "(WARNING["+WC+"]: does not exist)");
                env_CATALINA_HOME = null;
            } else {
                printVariable(ENVIRON_CATALINA_HOME, env_CATALINA_HOME, "");
                // check for non-executable files in "$CATALINA_HOME/bin"
                if (!isWindows && isJava6plus) {
                    String ext = isWindows? ".bat" : ".sh";
                    String sh[] = new String[] { "startup", "shutdown", "catalina" };
                    File tomcatBin = new File(env_CATALINA_HOME, "bin");
                    int notExecutableCount = 0;
                    /* (Java 6+) not supported on Java 5 */
                    try {
                        for (int i = 0; i < sh.length; i++) {
                            File shFile = new File(tomcatBin, sh[i]+ext);
                            MethodAction canExecMeth = new MethodAction(shFile, "canExecute");
                            boolean canExec = ((Boolean)canExecMeth.invoke()).booleanValue();
                            if (!canExec) {
                                notExecutableCount++;
                                break;
                            }
                        }
                    } catch (Throwable th) { // NoSuchMethodException
                        int WC = countWarning("Unable to check for exectuable Tomcat scripts");
                        println(PFX+"WARNING["+WC+"]: Unable to check for executable Tomcat scripts: " + th);
                    }
                    if (notExecutableCount > 0) {
                        println(PFX+"ERROR: Tomcat '$CATALINA_HOME/bin' directory contains non-executable '"+ext+"' files!");
                        addError("Tomcat contains non-executable '"+ext+"' files",
                                 "Some Tomcat '"+ext+"' commands do not have the 'execute' permission bit set.",
                                 "Run 'chmod a+x $CATALINA_HOME"+File.separator+"*"+ext+"' to set the execute bit");
                    }
                }
                // check for non-readable files in "$CATALINA_HOME/"
                if (!isWindows && isJava6plus) {
                    /* (Java 6+) not supported on Java 5 */
                    final AccumulatorLong accumCantRead = new AccumulatorLong(0L);
                    final long maxListedFiles = 4L;
                    try {
                        FileTools.traverseAllFiles(env_CATALINA_HOME, new FileFilter() {
                            public boolean accept(File f) {
                                if (!FileTools.canRead(f)) {
                                    accumCantRead.increment();
                                    if (accumCantRead.get() < maxListedFiles) {
                                        println(PFX+"ERROR: Cannot read - " + f);
                                    } else 
                                    if (accumCantRead.get() == maxListedFiles) {
                                        println(PFX+"ERROR: ... (additional non-readable files omitted) ...");
                                    }
                                } else {
                                    // OK
                                }
                                return true;
                            }
                        });
                    } catch (Throwable th) { // NoSuchMethodException
                        Print.logException("Readable files error", th);
                        int WC = countWarning("Unable to check for (non-)readable Tomcat files");
                        println(PFX+"WARNING["+WC+"]: Unable to check for (non-)readable Tomcat files: " + th);
                    }
                    if (accumCantRead.get() > 0L) {
                        println(PFX+"ERROR: Tomcat '$CATALINA_HOME/' directory contains non-readable files!");
                        addError("Tomcat contains non-readable files",
                                 "Some Tomcat files do not have the 'read' permission bit set or are owned by a different user.",
                                 "Run 'chmod' to set the read bit, or 'chown' to change to the proper user.");
                    }
                }
                // check for "$CATALINA_HOME/[common/]lib/servlet-api.jar" file
                File servletApiJarFile1 = new File(new File(new File(env_CATALINA_HOME,"common"),"lib"),"servlet-api.jar");
                boolean foundServletApiJar1 = servletApiJarFile1.isFile();
                if (!foundServletApiJar1) {
                    File servletApiJarFile2 = new File(new File(env_CATALINA_HOME,"lib"),"servlet-api.jar");
                    boolean foundServletApiJar2 = servletApiJarFile2.isFile();
                    if (!foundServletApiJar2) {
                        String saj = (isWindows? "%CATALINA_HOME%\\common\\lib\\" : "$CATALINA_HOME/common/lib/") + servletApiJarFile1.getName();
                        println(PFX+"ERROR: Tomcat '"+saj+"' file not found!");
                        addError("Tomcat '"+saj+"' file not found",
                                 "CATALINA_HOME is likely pointing to an invalid Tomcat installation",
                                 "Check directory referenced by CATALINA_HOME");
                    }
                }
            }
            // MYSQL_HOME
            //File envMysqlHome  = getEnvironmentFile(ENVIRON_MYSQL_HOME, true, false);
            //if (envMysqlHome != null) {
            //    printVariable(ENVIRON_MYSQL_HOME, envMysqlHome, "");
            //}
        }

        /* "$JAVA_HOME/jre/lib/ext" jars */
        println("");
        println("Extended library Jar files: 'java.ext.dirs'");
        String javaExtDirs[] = StringTools.split(System.getProperty("java.ext.dirs",""),File.pathSeparatorChar);
        if ((javaExtDirs == null) || (javaExtDirs.length == 0)) {
            println(PFX+"ERROR: System property 'java.ext.dirs' is null/empty!");
            addError("Extended library jar directory property 'java.ext.dirs' is null/empty.",
                     "'java.ext.dirs' is not defined",
                     null);
        } else {
            String reqJars[] = new String[] { /*"activation.jar",*/ "mail.jar", "mysql-connector-java-*" };
            for (int xd = 0; xd < javaExtDirs.length; xd++) {
                File prpExtLibHome = null;
                String fileList[] = null;
                try {
                    prpExtLibHome = (new File(javaExtDirs[xd])).getCanonicalFile();
                    fileList = prpExtLibHome.list();
                    if (fileList == null) { fileList = new String[0]; }
                } catch (IOException ioe) {
                    println(PFX+"ERROR: Unable to resolve extended library jar directory: " + javaExtDirs[xd]);
                    println(PFX+" [" + ioe.getMessage() + "]");
                    addError("Unable to resolve Java extended library directory.",
                             "Error resolving the System property 'java.ext.dirs' directory: "+javaExtDirs[xd],
                             null);
                    break;
                }
                printVariable("(Ext dir)", prpExtLibHome, "");
                for (int j = 0; j < reqJars.length; j++) {
                    if (reqJars[j] == null) { continue; }
                    String foundJarName = null;
                    for (int i = 0; i < fileList.length; i++) {
                        if (!StringTools.endsWithIgnoreCase(fileList[i],".jar")) { continue; }
                        if (reqJars[j].endsWith("*")) {
                            String pattern = reqJars[j].substring(0, reqJars[j].length() - 1); // remove trailing '*'
                            if (StringTools.startsWithIgnoreCase(fileList[i],pattern)) {
                                foundJarName = fileList[i];
                                break;
                            }
                        } else
                        if (fileList[i].equalsIgnoreCase(reqJars[j])) {
                            foundJarName = fileList[i];
                            break;
                        }
                    }
                    if (foundJarName != null) {
                        File foundJar = new File(prpExtLibHome, foundJarName);
                        if (foundJar.canRead()) {
                            printVariable(reqJars[j], "Found '" + foundJarName + "'", "");
                        } else {
                            printVariable(reqJars[j], "Found '" + foundJarName + "'", "(ERROR: not readable!)");
                            addError("Jar file '"+reqJars[j]+"' is not readable by this application.",
                                     "The jar file permissions may restrict the ability to read this file.",
                                     "Make sure this jar file permissions is set to world-readable.");
                        }
                        reqJars[j] = null;
                    }
                }
            }
            for (int j = 0; j < reqJars.length; j++) {
                if (reqJars[j] != null) {
                    printVariable(reqJars[j], "", "(ERROR: not found!)");
                    addError("Jar file '"+reqJars[j]+"' was not found.",
                             "The jar file is not installed in the extended library directory",
                             "Please install the jar file in the extended library directory");
                }
            }
        }

        /* Runtime configuration */
        println("");
        println("Runtime Configuration:");
        File configDir = RTConfig.getLoadedConfigDir();
        // 'default.conf'
        File defaultConfigFile = null;
        try { 
            defaultConfigFile = FileTools.toFile(RTConfig.getLoadedConfigURL()); 
        } catch (Throwable th) {
            int WC = countWarning("Error converting URL to File: " + RTConfig.getLoadedConfigURL());
            println(PFX+"WARNING["+WC+"]: Unable to convert URL to File: " + RTConfig.getLoadedConfigURL());
        }
        if (defaultConfigFile == null) {
            printVariable("(Default cfg dir)" , (configDir != null)? configDir.toString() : "(ERROR: not found!)", "");
            printVariable("(Default cfg file)", "", "(ERROR: not found!)");
            addError("Runtime configuration file not found.",
                     "Possible missing configuration file, or not found in CLASSPATH.",
                     "Please include configuration file directory in CLASSPATH.");
        } else
        if (configDir == null) {
            printVariable("(Default cfg dir)" , "", "(ERROR: not found!)");
            printVariable("(Default cfg file)", defaultConfigFile, "");
            addError("Runtime configuration directory not found.",
                     "Possible CLASSPATH and/or GTS_HOME configuration issue.",
                     "Please repair CLASSPATH and/or GTS_HOME configuration.");
        } else {
            printVariable("(Default cfg dir)" , configDir, "");
            printVariable("(Default cfg file)", defaultConfigFile, "");
        }
        // default properties
        File defaultFile = defaultConfigFile; // (configDir != null)? new File(configDir,"default.conf") : null;
        RTProperties defaultProps = (defaultFile != null)? new RTProperties(defaultFile) : null;
        // 'webapp.conf'
        File webappFile = (configDir != null)? new File(configDir,"webapp.conf") : null;
        RTProperties webappProps = null;
        if ((webappFile == null) || !webappFile.isFile()) {
            printVariable("(WebApp cfg URL)", "", "(ERROR: not found!)");
            addError("WebApp configuration file not found.",
                     "Possible missing configuration file, or not found in CLASSPATH.",
                     "Please include configuration file directory in CLASSPATH.");
        } else {
            //String webappURL = null;
            //try {
            //    webappURL = FileTools.toURL(webappFile).toString();
            //} catch (MalformedURLException mue) {
            //    webappURL = webappFile.toString();
            //}
            try { 
                webappProps = new RTProperties();
                webappProps.setKeyReplacementMode(RTProperties.KEY_REPLACEMENT_LOCAL);
                webappProps.setConfigLogMessagesEnabled(false);
                webappProps.setProperties(webappFile, true);
                printVariable("(WebApp cfg file)", webappFile, "");
            } catch (IOException ioe) {
                webappProps = null; // did not load
                Print.logError("Unable to load config file: " + webappFile + " [" + ioe + "]");
                printVariable("(WebApp cfg file)", webappFile, "(ERROR: unable to load!)");
                addError("Unable to load WebApp configuration file.",
                         "Possible invalid/unreadable configuration file.",
                         "Please check that configuration exists and is readable.");
            }
        }
        // log directory
        {
            File logDir = RTConfig.getFile(RTKey.LOG_DIR,null);
            if ((logDir == null) || StringTools.isBlank(logDir.toString())) {
                printVariable(RTKey.LOG_DIR, "", "(ERROR: not specified!)");
                addError("The '"+RTKey.LOG_DIR+"' appears to be missing from the runtime configuration.",
                         "Missing '"+RTKey.LOG_DIR+"' specification in 'default.conf' (or included files).",
                         "Please include '"+RTKey.LOG_DIR+"' specification in 'default.conf' (or included files).");
            } else
            if (!logDir.isDirectory()) {
                printVariable(RTKey.LOG_DIR, logDir, "(ERROR: does not exist!)");
                addError("The specified '"+RTKey.LOG_DIR+"' directory does not exist.",
                         "The specified '"+RTKey.LOG_DIR+"' directory does not exist.",
                         "Please make sure '"+RTKey.LOG_DIR+"' specifies an existing directory.");
            } else {
                printVariable(RTKey.LOG_DIR, logDir, "");
                final AccumulatorLong accumCantReadWrite = new AccumulatorLong(0L);
                final long maxListedFiles = 4L;
                try {
                    FileTools.traverseAllFiles(logDir, new FileFilter() {
                        public boolean accept(File f) {
                            if (!FileTools.canRead(f) || !FileTools.canWrite(f)) {
                                accumCantReadWrite.increment();
                                if (accumCantReadWrite.get() < maxListedFiles) {
                                    println(PFX+"ERROR: Cannot read/write - " + f);
                                } else 
                                if (accumCantReadWrite.get() == maxListedFiles) {
                                    println(PFX+"ERROR: ... (additional non-read/writable files omitted) ...");
                                }
                            } else {
                                // OK
                            }
                            return true;
                        }
                    });
                } catch (Throwable th) { // NoSuchMethodException
                    Print.logException("Read/Writable files error", th);
                    int WC = countWarning("Unable to check for (non-)read/writable Log files");
                    println(PFX+"WARNING["+WC+"]: Unable to check for (non-)read/writable Log files: " + th);
                }
                if (accumCantReadWrite.get() > 0L) {
                    println(PFX+"ERROR: Log directory contains non-read/writable files!");
                    addError("Log directory contains non-read/writable files",
                             "Some Log files do not have the 'read/write' permission bits set or are owned by a different user.",
                             "Run 'chmod' to set the read/write bits, or 'chown' to change to the proper user.");
                }
            }
        }
        // DBPrivider
        String dbProv = "";
        {
            dbProv = RTConfig.getString(RTKey.DB_PROVIDER,"");
            if (StringTools.isBlank(dbProv)) {
                printVariable(RTKey.DB_PROVIDER, "", "(ERROR: not specified!)");
                addError("The DB provider has not been specified.",
                         "Missing '"+RTKey.DB_PROVIDER+"' specification in 'default.conf' (or included files).",
                         "Please include '"+RTKey.DB_PROVIDER+"' specification in 'default.conf' (or included files).");
            } else
            if ((webappProps != null) && !dbProv.equals(webappProps.getString(RTKey.DB_PROVIDER,""))) {
                printVariable(RTKey.DB_PROVIDER, dbProv, "(ERROR: does not match 'webapp.conf'!)");
                addError("The DB provider in 'default.conf' does not match specification in 'webapp.conf'.",
                         "Invalid '"+RTKey.DB_PROVIDER+"' specification in 'webapp.conf'.",
                         "Please include proper '"+RTKey.DB_PROVIDER+"' specification in 'webapp.conf'.");
            } else {
                printVariable(RTKey.DB_PROVIDER, dbProv, "");
            }
        }
        // DB Host
        {
            String dftHost = (defaultProps != null)? defaultProps.getString(RTKey.DB_HOST,"") : "";
            String dbHost  = RTConfig.getString(RTKey.DB_HOST,"");
            if (StringTools.isBlank(dftHost)) {
                printVariable(RTKey.DB_HOST, "", "(ERROR: not specified!)");
                addError("The DB host has not been specified.",
                         "Missing '"+RTKey.DB_HOST+"' specification in 'default.conf'.",
                         "Please include '"+RTKey.DB_HOST+"' specification in 'default.conf'.");
            } else
            if (!dftHost.equals(dbHost)) {
                int WC = countWarning("DB host does not match host in 'default.conf'");
                printVariable(RTKey.DB_HOST, dftHost, "(WARNING["+WC+"]: does not match default host ["+dbHost+"])");
            } else
            if ((webappProps != null) && !dftHost.equals(webappProps.getString(RTKey.DB_HOST,""))) {
                int WC = countWarning("DB host does not match host in 'webapp.conf'");
                printVariable(RTKey.DB_HOST, dftHost, "(WARNING["+WC+"]: does not match 'webapp.conf')");
            } else
            if (!dftHost.equals("localhost")) {
                int WC = countWarning("DB host does not match 'localhost'");
                printVariable(RTKey.DB_HOST, dftHost, "(WARNING["+WC+"]: is not 'localhost')");
            } else {
                printVariable(RTKey.DB_HOST, dftHost, "");
            }
            //printVariable(RTKey.DB_PORT, String.valueOf(DBProvider.getDBPort()), "");
        }
        // DB Name
        {
            String dbName = RTConfig.getString(RTKey.DB_NAME,"");
            if (StringTools.isBlank(dbName)) {
                printVariable(RTKey.DB_NAME, "", "(ERROR: not specified!)");
                addError("The DB name has not been specified.",
                         "Missing '"+RTKey.DB_NAME+"' specification in 'default.conf'.",
                         "Please include '"+RTKey.DB_NAME+"' specification in 'default.conf'.");
            } else
            if ((webappProps != null) && !dbName.equals(webappProps.getString(RTKey.DB_NAME,""))) {
                String waName = webappProps.getString(RTKey.DB_NAME,"");
                printVariable(RTKey.DB_NAME, dbName, "(ERROR: does not match 'webapp.conf'!)");
                addError("The DB name in 'default.conf' does not match specification in 'webapp.conf'.",
                         "Invalid '"+RTKey.DB_NAME+"' specification in 'webapp.conf' ["+waName+"].",
                         "Please include proper '"+RTKey.DB_NAME+"' specification in 'webapp.conf'.");
            } else {
                printVariable(RTKey.DB_NAME, dbName, "");
            }
        }
        // DB User
        {
            String dbUser = RTConfig.getString(RTKey.DB_USER,"");
            if (StringTools.isBlank(dbUser)) {
                printVariable(RTKey.DB_USER, "", "(ERROR: not specified!)");
                addError("The DB user has not been specified.",
                         "Missing '"+RTKey.DB_USER+"' specification in 'default.conf' (or included files).",
                         "Please include '"+RTKey.DB_USER+"' specification in 'default.conf' (or included files).");
            } else
            if ((webappProps != null) && !dbUser.equals(webappProps.getString(RTKey.DB_USER,""))) {
                String waUser = webappProps.getString(RTKey.DB_USER,"");
                printVariable(RTKey.DB_USER, dbUser, "(ERROR: does not match 'webapp.conf'!)");
                addError("The DB user in 'default.conf' does not match specification in 'webapp.conf'.",
                         "Invalid '"+RTKey.DB_USER+"' specification in 'webapp.conf'.",
                         "Please include proper '"+RTKey.DB_USER+"' specification in 'webapp.conf' ["+waUser+"].");
            } else
            if (dbUser.equals("root")) {
                int WC = countWarning("DB user should not be 'root'");
                printVariable(RTKey.DB_USER, dbUser, "(WARNING["+WC+"]: should not be 'root')");
            } else {
                printVariable(RTKey.DB_USER, dbUser, "");
            }
        }
        // DB utf8
        {
            boolean dbUTF8 = RTConfig.getBoolean(RTKey.DB_UTF8,false);
            printVariable(RTKey.DB_UTF8, String.valueOf(dbUTF8), "");
        }
        // DB url
        {
            printVariable(RTKey.DB_URL   , RTConfig.getString(RTKey.DB_URL   ,""), "");
            printVariable(RTKey.DB_URL_DB, RTConfig.getString(RTKey.DB_URL_DB,""), "");
        }
        // MySQL
        if (StringTools.containsIgnoreCase(dbProv,"mysql")) {
            // (MySQL MaxConnections) ==>
            int recommendedMaxConn = 200;
            String maxConnTitle = "(MySQL MaxConnections)";
            File myCnfFile = new File("/etc/my.cnf"); // Linux
            if (myCnfFile.isFile()) {
                // format could be one of the following:
                //    max-connections=500
                //    set-variable=max_connections=500
                java.util.List<String> maxConn = FileTools.findPatternInFile(myCnfFile,"max-connections",true);
                if (ListTools.size(maxConn) > 0) {
                    String maxConnLine = maxConn.get(0);
                    int eqSepPos = maxConnLine.lastIndexOf("="); // last occurance of "="
                    if (eqSepPos < 0) {
                        printVariable(maxConnTitle, "unknown", "(unable to find specified max size)");
                    } else {
                        String maxStr = maxConnLine.substring(eqSepPos+1).trim();
                        int    maxInt = StringTools.parseInt(maxStr,-1);
                        if (maxInt < 0) {
                            printVariable(maxConnTitle, "unknown", "(unable to parse specified max size)");
                        } else
                        if (maxInt < recommendedMaxConn) {
                            printVariable(maxConnTitle, maxStr, "(Recommend at least "+recommendedMaxConn+")");
                            recommendations.append("- Recommend setting MySQL 'max-connections' to at least "+recommendedMaxConn+":\n");
                            recommendations.append("     see \"http://www.opengts.org/FAQ.html#faq_mysqlConn\"\n");
                        } else {
                            printVariable(maxConnTitle, maxStr, "");
                        }
                    }
                } else {
                    printVariable(maxConnTitle, "default", "(Recommend setting to at least "+recommendedMaxConn+")");
                    recommendations.append("- Recommend setting MySQL 'max-connections' to at least "+recommendedMaxConn+".\n");
                    recommendations.append("     see \"http://www.opengts.org/FAQ.html#faq_mysqlConn\"\n");
                }
            } else {
                printVariable(maxConnTitle, "unknown", "('"+myCnfFile+"' not found)");
            }
        }
        // StartupInit class
        {
            String startupInitClass = RTConfig.getString(DBConfig.PROP_StartupInit_class,"");
            String waStartupInitClass = (webappProps != null)? webappProps.getString(DBConfig.PROP_StartupInit_class,"") : "";
            if (StringTools.isBlank(startupInitClass) && StringTools.isBlank(waStartupInitClass)) {
                printVariable(DBConfig.PROP_StartupInit_class, "(default)", "");
            } else {
                String initClass = !StringTools.isBlank(startupInitClass)? startupInitClass : waStartupInitClass;
                printVariable(DBConfig.PROP_StartupInit_class, ClassName(initClass), "");
                Object startupInit = null;
                if (!startupInitClass.equals(waStartupInitClass)) {
                    println(PFX+"ERROR: 'webapp.conf' does not match 'default.conf'!");
                    addError("webapp.conf '"+DBConfig.PROP_StartupInit_class+"' does not match default.conf",
                             null,
                             "Change 'webapp.conf' to match 'default.conf'.");
                }
                try {
                    Class cfgClass = Class.forName(initClass);
                    startupInit = cfgClass.newInstance();
                } catch (ClassNotFoundException cnfe) {
                    println(PFX+"ERROR: Class not found!");
                    addError("Unable to load class '"+initClass+".",
                             "Class '"+initClass+"' was not found.",
                             "Fix class definition.");
                } catch (Throwable th) { // NoSuchMethodException, etc
                    println(PFX+"ERROR: Unable to load instance!");
                    addError("Unable to load class '"+initClass+".",
                             "Due to error '" + th.toString() + "'",
                             "Fix class definition.");
                }
            }
        }
        // RuleFactory
        {
            RuleFactory ruleFact = Device.getRuleFactory();
            if (ruleFact != null) {
                long compileTime = 0L;
                try {
                    MethodAction ma = new MethodAction(ruleFact,"getCompileTime");
                    Long ct = (Long)ma.invoke();
                    compileTime = (ct != null)? ct.longValue() : 0L;
                } catch (Throwable th) {
                    compileTime = 0L;
                }
                StringBuffer v = new StringBuffer();
                v.append("[").append(ruleFact.getName()).append(" ").append(ruleFact.getVersion());
                if (compileTime > 0L) {
                    v.append(" ").append((new DateTime(compileTime)).gmtFormat("yyyy/MM/dd HH:mm:ss"));
                }
                v.append("] ").append(ClassName(ruleFact));
                printVariable("(RuleFactory)", v.toString(), "");
                try {
                    // check required runtime support compments
                    MethodAction ma = new MethodAction(ruleFact,"checkRuntime");
                    Boolean rt = (Boolean)ma.invoke();
                    if ((rt != null) && !rt.booleanValue()) {
                        // RuleFactory has indicate an error
                        recommendations.append("- Recommend checking RuleFactory runtime support components.\n");
                    }
                } catch (Throwable th) {
                    // ignore
                    //Print.logException("RuleFactory",th);
                }
            } else {
                printVariable("(RuleFactory)", "(not installed)", "");
            }
        }
        // PingDispatcher
        {
            PingDispatcher pingDisp = Device.getPingDispatcher();
            if (pingDisp != null) {
                printVariable("(PingDispatcher)", ClassName(pingDisp), "");
            } else {
                //printVariable("(PingDispatcher)", "(not installed)", "");
            }
        }
        // SMTP
        int emailIsFunctional = -1; // -1=no, 0=maybe, 1=yes
        boolean hasSMTPHost = true;
        {
            String  none     = "<none>";
            String  smtpHost = (defaultProps != null)? defaultProps.getString( RTKey.SMTP_SERVER_HOST, none ) : none;
            int     smtpPort = (defaultProps != null)? defaultProps.getInt(    RTKey.SMTP_SERVER_PORT, 25   ) : 25;
            String  smtpUser = (defaultProps != null)? defaultProps.getString( RTKey.SMTP_SERVER_USER, none ) : none;
            boolean smtpSSL  = (defaultProps != null)? defaultProps.getBoolean(RTKey.SMTP_ENABLE_SSL , false) : false;
            hasSMTPHost = !StringTools.isBlank(smtpHost) && !smtpHost.equals(none);
            printVariable("(SMTP)", smtpHost + ":" + smtpPort, "[user=" + smtpUser + ", ssl=" + smtpSSL + "]");
            if (!hasSMTPHost) {
                printVariable("(SMTP Connection)", "", "SMTP service disabled (no host specified)");
            } else {
                // Socket connection to SMTP service
                boolean SMTP_port_ok = false;
                Socket socket = null;
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(smtpHost, smtpPort), 3000); // 3 seconds
                    printVariable("(SMTP Connection)", "Successful connection (does not guarantee service)", "");
                    SMTP_port_ok = true;
                } catch (SocketTimeoutException ste) {
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: connection timeout)");
                    addError("Unable to connect to the SMTP host:port '"+smtpHost+":"+smtpPort+"'.",
                             "Possible slow connection, or possible invalid SMTP host:port specification.",
                             "Please check proper SMTP specification, and re-run CheckInstall.", 
                             false);
                } catch (ConnectException ce) {
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: connection refused)");
                    addError("Unable to connect to the SMTP host:port '"+smtpHost+":"+smtpPort+"'.",
                             "Invalid SMTP host:port specified.",
                             "Please set valid SMTP host:port specification.",
                             false);
                } catch (UnknownHostException uhe) {
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: unknown host)");
                    addError("Unable to connect to the SMTP host '"+smtpHost+"'.",
                             "Invalid SMTP host specified in 'default.conf' (or included files).",
                             "Please set valid SMTP host specification.",
                             false);
                } catch (Throwable th) {
                    Print.logException("SMTP server connect error",th);
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: unexpected error)");
                    addError("Unable to connect to the SMTP host:port '"+smtpHost+":"+smtpPort+"'.",
                             "Unexpected error received [" + th + "]",
                             "Please fix and re-run this CheckInstall.",
                             false);
                } finally {
                    try { if (socket != null) { socket.close(); } } catch (Throwable th) {/*ignore*/}
                    socket = null;
                }
                // JavaMail check
                String SMTP_session = "javax.mail.Session";
                boolean found_JavaMail = false;
                try {
                    Class.forName(SMTP_session);
                    //printVariable("(JavaMail)", "JavaMail present (found '"+SMTP_session+"')", "");
                    found_JavaMail = true;
                } catch (Throwable th) { // ClassNotFoundException
                    //printVariable("(JavaMail)", "", "ERROR: Unable to locate '" + SMTP_session + "'");
                    println(PFX+"ERROR: JavaMail not installed, unable to locate '" + SMTP_session + "'");
                    addError("Unable to locate JavaMail support (class '"+SMTP_session+"').",
                             "'mail.jar' may not be installed in a Java extended library directory.",
                             "Please install JavaMail, and re-run CheckInstall.");
                    found_JavaMail = false;
                }
                // SendMailArgs check (only if JavaMail was found)
                boolean SendMail_functional = false;
                if (found_JavaMail) {
                    try {
                        Class.forName(SendMail.SendMailArgs_className);
                        SendMail_functional = true;
                    } catch (Throwable th) { // ClassNotFoundException
                        println(PFX+"ERROR: JavaMail may not have been installed at compile time!");
                        addError("JavaMail was not installed at the time this code was compiled.",
                                 "'mail.jar' was not installed in a Java extended library directory.",
                                 "Please install JavaMail, recompile, and re-run CheckInstall.");
                    }
                }
                emailIsFunctional = !SendMail_functional? -1 : !SMTP_port_ok? 0 : 1;
                if (!StringTools.isBlank(sendTestEmailTo) && SMTP_port_ok && SendMail_functional) {
                    String toAddr   = sendTestEmailTo;
                    String fromAddr = SendMail.getUserFromEmailAddress();
                    if (StringTools.isBlank(fromAddr)) {
                        println(PFX+"ERROR: Unable to send email ('"+RTKey.SMTP_SERVER_USER_EMAIL+"' not defined)");
                        addError("Unable to send a test email.",
                                 "Property '"+RTKey.SMTP_SERVER_USER_EMAIL+"' has not been defined in the runtime config file.",
                                 "Please initialize this property to a valid 'from' email address, and re-run CheckInstall.");
                    } else
                    if (StringTools.isBlank(toAddr) || toAddr.endsWith("example.com")) {
                        println(PFX+"ERROR: Unable to send email (Invalid 'To' address)");
                        addError("Unable to send a test email.",
                                 "Invalid 'To' address specified.",
                                 "Please specify a valid 'To' address, and re-run CheckInstall.");
                    } else {
                        String subj = "CheckInstall test email ["+DBConfig.getVersion()+"]";
                        String body = "CheckInstall test email send successfully.";
                        println(PFX+"Attempting to send test email to '"+toAddr+"' ...");
                        SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
                        if (SendMail.send(fromAddr,toAddr,subj,body,null)) {
                            println(PFX+"... Test email successfully sent:");
                            println(PFX+"    From   : " + fromAddr);
                            println(PFX+"    To     : " + toAddr);
                            println(PFX+"    Subject: " + subj);
                            println(PFX+"    Body   : " + body);
                        } else {
                            println(PFX+"ERROR: Unable to send email ('SendMail' failed)");
                            addError("Unable to send a test email.",
                                     "'SendMail' failed (see previous errors).",
                                     "Please fix displayed errors and re-run CheckInstall.");
                        }
                    }
                }
            }
        }

        /* character encodings */
        println("");
        println("Character Encodings:");
        // Check Character Encoding
        {
            printVariable("(Default Encoding)", StringTools.getCharacterEncoding(), "");
        }
        // "file.encoding"
        {
            String propEncoding = "file.encoding";
            String fileEncoding = System.getProperty(propEncoding,null);
            if (fileEncoding != null) {
                printVariable(propEncoding, fileEncoding, "");
            } else {
                printVariable(propEncoding, "(not specified?)", "");
            }
        }
        // GTS_CHARSET
        {
            String envGtsCharset = System.getenv(ENVIRON_GTS_CHARSET);
            if (envGtsCharset != null) {
                try {
                    byte b[] = "hello".getBytes(envGtsCharset); // may throw exception
                    printVariable(ENVIRON_GTS_CHARSET, envGtsCharset, "");
                } catch (UnsupportedEncodingException uce) {
                    printVariable(ENVIRON_GTS_CHARSET, envGtsCharset, "(ERROR: invalid character encoding)");
                    addError("'"+ENVIRON_GTS_CHARSET+"' specifies an invalid character encoding.",
                             "Character encoding specified by '"+ENVIRON_GTS_CHARSET+"' is invalid",
                             FIX_PREVIOUS_ERRORS);
                }
            } else {
                //
            }
        }
        // DBProvider
        {
            String dbCharset = null;
            try {
                dbCharset = DBProvider.getDefaultCharacterSet();
            } catch (Throwable th) {
                // ignore
            }
            String dbcs = !StringTools.isBlank(dbCharset)? dbCharset : "?";
            printVariable("DBProvider:"+DBProvider.getProviderName(), dbcs, "");
        }

        /* Tables */
        println("");
        println("Tables ["+DBProvider.getDBUri(true)+"]");
        boolean skipTableChecks = false;
        if (defaultConfigFile != null) {
            String driver = DBProvider.loadJDBCDriver();
            if (driver == null) {
                println(PFX+"ERROR: JDBC driver not found or cannot be loaded!");
                addError("JDBC driver not found, or cannot be loaded.",
                         "The database JDBC driver has not been installed, or cannot be loaded.",
                         "Please install the appropriate JDBC driver with world-readable permissions.");
                // a missing JDBC driver would cause db access errors, skip tables
                skipTableChecks = true;
            } else {
                OrderedMap factMap = DBAdmin.getTableFactoryMap();
                for (Iterator i = factMap.keyIterator(); i.hasNext();) {
                    String tn = (String)i.next();
                    DBFactory<? extends DBRecord> f = (DBFactory<? extends DBRecord>)factMap.get(tn);
                    try {
                        if (!f.tableExists()) {
                            printVariable(f.getUntranslatedTableName(), "", "(ERROR: table does not exist!)");
                            addError("Table '"+f.getUntranslatedTableName()+"' does not exist.",
                                     "Database may not have been initialized.",
                                     "Please initialize the database.");
                        } else
                        if (!f.validateColumns(0x0000)) {
                            printVariable(f.getUntranslatedTableName(), "", "(ERROR: column validation failed!)");
                            addError("Table '"+f.getUntranslatedTableName()+"' failed column validation.",
                                     "Table may be missing columns, or have columns which have changed types.",
                                     "Run 'bin/dbAdmin.pl -tables' (or 'bin/dbconfig.bat -tables') for details.");
                        } else {
                            if (RTConfig.getBoolean(PROP_skipDBRecordCount,false)) {
                                printVariable(f.getUntranslatedTableName(), "Exists", "");
                            } else {
                                long rcdCnt = DBRecord.getRecordCount(f);
                                printVariable(f.getUntranslatedTableName(), "RecordCount "+rcdCnt, "");
                            }
                        }
                    } catch (DBException dbe) {
                        if (dbe.isSQLException()) {
                            SQLException sqle = (SQLException)dbe.getCause();
                            String sqlMsg = sqle.getMessage().toLowerCase();
                            if (sqlMsg.indexOf("access denied") >= 0) {
                                printVariable(tn, "", "(ERROR: SQL database access denied!)");
                                addError("Database access denied.",
                                         "Possible invalid user/password, or database name, specified in runtime config file",
                                         "Please specify a valid database name/user/password in the runtime config file");
                                //dbe.printException();
                            } else
                            if (sqlMsg.indexOf("communications link failure") >= 0) {
                                printVariable(tn, "", "(ERROR: SQL database connection failure!)");
                                addError("Database connection failure.",
                                         "Database may not be running on expected port",
                                         "Please start database service on expected port");
                                //dbe.printException();
                            } else
                            if (sqlMsg.indexOf("no suitable driver") >= 0) {
                                printVariable(tn, "", "(ERROR: Invalid JDBC driver!)");
                                addError("JDBC driver not found, or invalid.",
                                         "The JDBC driver is not installed, or is invalid for the specified database provider",
                                         "Please install the appropriate JDBC driver for the specified database provider");
                                //dbe.printException();
                            } else {
                                printVariable(tn, "", "(ERROR: SQL exception!)");
                                addError("SQL database exception while checking table '"+f.getUntranslatedTableName()+"' existance.",
                                         "Refer to above stacktrace for a detailed description",
                                         null);
                                dbe.printException();
                            }
                        } else {
                            printVariable(tn, "", "(ERROR: database exception!)");
                            addError("Database exception while checking table '"+f.getUntranslatedTableName()+"' existance.",
                                     "Refer to above stacktrace for a detailed description",
                                     null);
                            dbe.printException();
                        }
                        // the previous errors would be repeated for all tables, skip remaining tables
                        skipTableChecks = true;
                        break;
                    }
                }
            }
        } else {
            // The runtime config contains DB access information, if it isn't available, skip the table checks
            skipTableChecks = true;
        }
        if (skipTableChecks) {
            println(PFX+"ERROR: Skipping table checks due to previous errors");
            addError("Database table checks not performed.",
                     "Table checks ignored due to previous errors",
                     FIX_PREVIOUS_ERRORS);
        }

        /* [Basic]PrivateLabel (reports.xml) */
        println("");
        println("reports.xml:");
        // 'reports.xml' file path
        {
            File reportsXMLFile = ReportFactory._getReportXMLFile();
            if ((reportsXMLFile == null) || !reportsXMLFile.isFile()) {
                printVariable("(XML file)", "", "(ERROR: XML file not found)");
                addError("'reports.xml' file not found.",
                         "Unable to locate 'reports.xml' file.",
                         "Make sure that the 'reports.xml' file is available, then re-run this installation check");
            } else
            if (ReportFactory.hasParsingWarnings()) {
                printVariable("(XML file)", reportsXMLFile.toString(), "(ERROR: Has parsing errors)");
                addError("'reports.xml' has parsing errors.",
                         "The 'reports.xml' lokely has invalid XML syntax or other parsing errors.",
                         "Fix errors in 'reports.xml', then re-run this installation check");
            } else {
                printVariable("(XML file)", reportsXMLFile.toString(), "");
            }
            Collection<ReportFactory> rptFactList = ReportFactory.getReportFactories();
            if (ListTools.isEmpty(rptFactList)) {
                printVariable("(Report count)", "0", "");
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    int WC = countWarning("'reports.xml' does not define any reports");
                    println(PFX+"WARNING["+WC+"]: 'reports.xml' does not define any reports.");
                } else {
                    int WC = countWarning("'reports.xml' might not define any reports");
                    println(PFX+"WARNING["+WC+"]: 'reports.xml' might not define any reports.");
                }
            } else {
                printVariable("(Report count)", String.valueOf(rptFactList.size()), "");
                for (ReportFactory rf : rptFactList) {
                    String rptName  = rf.getReportName();
                    String rptTitle = rf.getReportTitle(null, "");
                    printVariable(" "+rptName, rptTitle, "");
                }
            }
        }

        /* [Basic]PrivateLabel (private.xml) */
        println("");
        println("private.xml:");
        {
            // 'private.xml' file path
            File privLblXMLFile = BasicPrivateLabelLoader.getPrivateXMLFile();
            if ((privLblXMLFile == null) || !privLblXMLFile.isFile()) {
                printVariable("(XML file)", "", "(ERROR: XML file not found)");
                addError("'private.xml' file not found.",
                         "Unable to locate 'private.xml' file.",
                         "Make sure that the 'private.xml' file is available, then re-run this installation check");
            } else {
                printVariable("(XML file)", privLblXMLFile.toString(), "");
                if (env_CATALINA_HOME != null) {
                    String trackXMLName = "/webapps/track/WEB-INF/private.xml".replace('/',File.separatorChar);
                    File   trackXMLFile = new File(env_CATALINA_HOME, trackXMLName);
                    if (trackXMLFile.isFile()) {
                        byte T[] = FileTools.readFile(trackXMLFile);
                        byte G[] = FileTools.readFile(privLblXMLFile);
                        if ((T != null) && (G != null)) {
                            int diff = StringTools.compare(T, G, G.length);
                            if (diff != 0) {
                                String CH = isWindows? "%CATALINA_HOME%" : "$CATALINA_HOME";
                                int WC = countWarning("'private.xml' file does not match deployed version");
                                println(PFX+"WARNING["+WC+"]: does not match "+CH+trackXMLName);
                            }
                        }
                    }
                }
            }
            // BasicPrivateLabelLoader subclass
            Class loaderClass = BasicPrivateLabelLoader.getInstanceClass();
            printVariable("(Class)", ClassName(loaderClass), "");
            boolean isPrivateLabelLoader = false;
            try {
                isPrivateLabelLoader = PrivateLabelLoader.class.isAssignableFrom(loaderClass);
            } catch (Throwable th) { // NoClassDefFoundError
                isPrivateLabelLoader = false;
            }
            if (!isPrivateLabelLoader) {
                if (env_CATALINA_HOME == null) {
                    println(PFX+"ERROR: CATALINA_HOME not defined, unable to perform Servlet level validation.");
                    addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                             "CATALINA_HOME has not been defined",
                             "Define CATALINA_HOME");
                } else {
                    try {
                        Class.forName(BasicPrivateLabelLoader.CLASS_Track);
                        try {
                            Class.forName(BasicPrivateLabelLoader.CLASS_PrivateLabelLoader);
                            println(PFX+"ERROR: Unexpected 'PrivateLabelLoader' error, unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "Some unexpected error has occurred while loading 'PrivateLabelLoader'",
                                     null);
                        } catch (Throwable th) { // ClassNotFoundException
                            println(PFX+"ERROR: Unable to load 'PrivateLabelLoader.class', unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "Error loading 'PrivateLabelLoader': " + th,
                                     null);
                        }
                    } catch (NoClassDefFoundError ncdfe) {
                        String errMsg = StringTools.trim(ncdfe.getMessage());
                        if (errMsg.startsWith("javax/servlet")) {
                            println(PFX+"ERROR: Invalid CATALINA_HOME definition, unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "CATALINA_HOME is likely pointing to an invalid Tomcat installation",
                                     "Check directory referenced by CATALINA_HOME");
                        } else {
                            println(PFX+"ERROR: Required class not found, unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "Unable to load class " + errMsg,
                                     "Check directory referenced by CATALINA_HOME");
                        }
                    } catch (Throwable th) {
                        println(PFX+"ERROR: Unable to load 'Track.class', unable to perform Servlet level validation.");
                        addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                 "Error loading 'Track': " + th,
                                 null);
                    }
                }
            }
            // load 'private.xml'
            String privateXML = "private.xml";
            ReportFactory.setIgnoreMissingReports(false);
            BasicPrivateLabelLoader.loadPrivateLabelXML();
            if (BasicPrivateLabelLoader.hasParsingErrors()) {
                println(PFX+"ERROR: Errors were encountered while parsing '"+privateXML+"'.");
                addError("Full '"+privateXML+"' checks were not performed.",
                         "Errors were encountered while parsing '"+privateXML+"'",
                         FIX_PREVIOUS_ERRORS);
            } else {
                // track servlet?
                if (!BasicPrivateLabelLoader.isTrackServlet()) {
                    println(PFX+"ERROR: '"+privateXML+"' not fully loaded (possible classpath issue?)");
                    addError("Full '"+privateXML+"' checks may not be performed due to possible classpath issues.",
                             "Possible incorrect command execution directory, or missing '"+TRACK_CLASS_DIR+"' directory.  " + 
                                 "This condition may cause false errors/warnings to be reported",
                             "Make sure '"+TRACK_CLASS_DIR+"' exists, then re-run this installation check from the " +
                                 "OpenGTS installation directory.");
                }
                OrderedSet<BasicPrivateLabel> privLabelSet = new OrderedSet<BasicPrivateLabel>(true);
                // has warnings?
                if (BasicPrivateLabelLoader.hasParsingWarnings()) {
                    int WC = countWarning("Warnings were encountered while parsing '"+privateXML+"'");
                    println(PFX+"WARNING["+WC+"]: Warnings were encountered while parsing '"+privateXML+"'");
                }
                // default Domain?
                BasicPrivateLabel defaultPrivLabel = BasicPrivateLabelLoader.getDefaultPrivateLabel();
                if (defaultPrivLabel != null) {
                    privLabelSet.add(defaultPrivLabel); // default first
                } else {
                    int WC = countWarning("'"+privateXML+"' does not define a default 'Domain'");
                    println(PFX+"WARNING["+WC+"]: '"+privateXML+"' does not define a default 'Domain'.");
                }
                // populate a set of BasicPrivateLabel's to test
                Collection<String> privLabelNames = BasicPrivateLabelLoader.getPrivateLabelNames();
                for (String privLabelName : privLabelNames) {
                    BasicPrivateLabel privLabel = BasicPrivateLabelLoader.getPrivateLabel(privLabelName);
                    if (privLabel != null) {
                        if (!privLabelSet.contains(privLabel)) {
                            privLabelSet.add(privLabel);
                        }
                    } else {
                        println(PFX+"ERROR: Unexpected error PrivateLabelName not found: " + privLabelName);
                        addError("Unexpected error PrivateLabelName not found: " + privLabelName,
                                 "Errors were encountered while parsing 'private.xml'",
                                 FIX_PREVIOUS_ERRORS);
                    }
                }
                // number of BasicPrivateLabels
                printVariable("(Domain count)", String.valueOf(privLabelSet.size()), "");
                int domainNdx = 1;
                for (BasicPrivateLabel privLabel : privLabelSet) {
                    String name  = privLabel.getDomainName();
                    String host  = privLabel.getHostName();
                    String alias = StringTools.join(privLabel.getHostAliasNames(),", ");
                    boolean isDefault = name.equals("default");
                    boolean skipDefaultEMailChecks = isDefault && RTConfig.getBoolean(PROP_skipDefaultEMailChecks,false);
                    boolean skipDefaultMapChecks   = isDefault && RTConfig.getBoolean(PROP_skipDefaultMapChecks,false);
                    StringBuffer nameInfo = new StringBuffer();
                    nameInfo.append(privLabel.getLocale().toString());
                    if (privLabel.getAccountLogin()) { 
                        nameInfo.append(", accountLogin"); 
                        String da = privLabel.getDefaultLoginAccount();
                        if (!StringTools.isBlank(da)) {
                            nameInfo.append("[\"").append(da).append("\"]");
                        }
                    }
                    if (privLabel.getUserLogin()) { 
                        nameInfo.append(", userLogin");
                        String du = privLabel.getDefaultLoginUser();
                        if (!StringTools.isBlank(du)) {
                            nameInfo.append("[\"").append(du).append("\"]");
                        }
                    }
                    if (privLabel.getAllowEmailLogin()) { 
                        nameInfo.append(", emailLogin"); 
                    }
                    if (privLabel.getEnableDemo()) { 
                        nameInfo.append(", demo");       
                    }
                    if (privLabel.isRestricted()) { 
                        nameInfo.append(", restricted"); 
                    }
                    printVariable((domainNdx++) + ") "+name, nameInfo.toString(), "");
                    printVariable("   (host)" , " "+host , "");
                    if (!StringTools.isBlank(alias)) {
                        printVariable("   (alias)", " "+alias, "");
                    }
                    if (!skipDefaultEMailChecks) {
                        // EMail check
                        String email[] = privLabel.getEMailAddresses();
                        int emailErrors = 0;
                        for (int e = 0; e < email.length; e++) {
                            if (email[e].endsWith(SendMail.EXAMPLE_DOT_COM)) {
                                if (hasSMTPHost) {
                                    println(PFX+"ERROR: EMail address has not been customized: "+email[e]);
                                    if (emailErrors == 0) {
                                        addError("EMail addresses for Domain '"+name+"' have not been customized.",
                                                 null,
                                                 "Customize EMail address, then re-run this installation check.");
                                    }
                                } else {
                                    int WC = countWarning("EMail address has not been customized: " + email[e]);
                                    println(PFX+"WARNING["+WC+"]: EMail address has not been customized: " + email[e]);
                                }
                                emailErrors++;
                            }
                        }
                        if (privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_enableReportEmail,true)) {
                            String frEmail = privLabel.getEventNotificationFrom();
                            if (emailIsFunctional == -1) {
                                // error
                                //addError("Property 'reportMenu.enableReportEmail' enabled, but SMTP has not been configured.",
                                //         null,
                                //         "Configure outbound SMTP, then re-run this installation check.");
                                int WC = countWarning("Report email defined, but SMTP not configured");
                                println(PFX+"WARNING["+WC+"]: Property 'reportMenu.enableReportEmail' defined, but SMTP has not been configured.");
                            } else
                            if (StringTools.isBlank(frEmail)) {
                                // error
                                //addError("Property 'reportMenu.enableReportEmail' defined, but no 'From' configured.",
                                //         null,
                                //         "Configure outbound SMTP, then re-run this installation check.");
                                int WC = countWarning("Report email defined, but no 'From' address configured");
                                println(PFX+"WARNING:["+WC+"] Property 'reportMenu.enableReportEmail' defined, but no 'From' configured.");
                            } else
                            if (emailIsFunctional == 0) {
                                // warning
                                int WC = countWarning("Report email defined, but SMTP port not accessible");
                                println(PFX+"WARNING["+WC+"]: Property 'reportMenu.enableReportEmail' defined, but SMTP port not accessible.");
                            } else {
                                // ok
                            }
                        }
                    }
                    // ACL check
                    /*
                    String aclList[] = AclEntry.ACL_RESERVED_LIST;
                    int aclErrors = 0;
                    for (int a = 0; a < aclList.length; a++) {
                        if (!privLabel.hasAclEntry(aclList[a])) {
                            println(PFX+"ERROR: Missing reserved ACL entry: "+aclList[a]);
                            if (aclErrors++ == 0) {
                                addError("ACL list for Domain '"+name+"' is missing a reserved entry.",
                                         null,
                                         "Define reserved ACL entry, then re-run this installation check.");
                            }
                        }
                    }
                    */
                    // MapProvider check
                    try {
                        if (privLabel instanceof PrivateLabel) {
                            PrivateLabel pl = (PrivateLabel)privLabel;
                            // MapProvider check
                            MapProvider mp = pl.getMapProvider();
                            if (mp == null) {
                                if (BasicPrivateLabelLoader.isTrackServlet()) {
                                    println(PFX+"ERROR: No active MapProvider defined ["+name+"]");
                                    addError("Domain '"+name+"' is missing an active MapProvider declaration.",
                                             null,
                                             "Add a MapProvider declaration to this Domain, then re-run this installation check");
                                } else {
                                    int WC = countWarning("Make sure Domain '"+name+"' has an active MapProvider");
                                    println(PFX+"WARNING["+WC+"]: Make sure this Domain has an active MapProvider declaration.");
                                }
                            } else {
                                String mpName = mp.getName();
                                //printVariable("(MapProvider)", " "+ClassName(mp), "");
                                printVariable("   (map provider)", " "+mp.toString(), "");
                                if (!skipDefaultMapChecks && (mp instanceof MapProviderAdapter) && !((MapProviderAdapter)mp).validate()) {
                                    println(PFX+"ERROR: MapProvider '"+mpName+"' returned a validation error");
                                    addError("MapProvider '"+mpName+"' returned a validation error for Domain '"+name+"'",
                                             null,
                                             FIX_PREVIOUS_ERRORS);
                                }
                            }
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // ReverseGeogoceProvider check
                    try {
                        ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
                        if (rgp != null) {
                            printVariable("   (reverse-geocoder)", " "+rgp.toString(), "");
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // GeocodeProvider check
                    try {
                        GeocodeProvider gcp = privLabel.getGeocodeProvider();
                        if (gcp != null) {
                            printVariable("   (geocoder)", " "+gcp.toString(), "");
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // MobileLocationProvider check
                    try {
                        MobileLocationProvider mlp = privLabel.getMobileLocationProvider();
                        if (mlp != null) {
                            printVariable("   (mobile location)", " "+mlp.toString(), "");
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // "privateLabelDetail"
                    if (RTConfig.getBoolean(ARG_privateLabelDetail,false)) {
                        privLabel.pushRTProperties();
                        printVariable("   "+RTKey.HTTP_USER_AGENT, " "+RTConfig.getString(RTKey.HTTP_USER_AGENT,"<default>"), "");
                        privLabel.popRTProperties();
                    }
                    // DefaultLoginAccount
                    String dftAcctID = privLabel.getDefaultLoginAccount();
                    boolean dftAcctExists = false;
                    if (!StringTools.isBlank(dftAcctID)) {
                        try {
                            dftAcctExists = Account.exists(dftAcctID);
                        } catch (DBException dbe) {
                            int WC = countWarning("DB Error when checking DefaultLoginAccount '"+dftAcctID+"' existence");
                            println(PFX+"WARNING["+WC+"]: DB Error determining if 'DefaultLoginAccount' exists.");
                            dftAcctExists = true;
                        }
                    }
                    if (!privLabel.getAccountLogin() && !privLabel.getAllowEmailLogin()) {
                        // 'User' login only (no accountId, and no emailAddress)
                        if (dftAcctExists) {
                            // normal state, all is ok
                        } else
                        if (StringTools.isBlank(dftAcctID)) {
                            println(PFX+"ERROR: 'accountLogin' is false, and DefaultLoginAccount is blank.");
                            addError("'accountLogin' is false, and DefaultLoginAccount is blank.",
                                     "'accountLogin' is false, 'emailLogin' is false, and no account-id has been " +
                                     "specified on the 'DefaultLoginAccount' tag",
                                     FIX_PREVIOUS_ERRORS);
                        } else {
                            println(PFX+"ERROR: 'accountLogin' is false, and account '"+dftAcctID+"' does not exist.");
                            addError("accountLogin='false', and account '"+dftAcctID+"' does not exist.",
                                     "accountLogin='false', emailLogin='false', and account-id '"+dftAcctID+"' " +
                                     "specified on the 'DefaultLoginAccount' tag does not exist",
                                     FIX_PREVIOUS_ERRORS);
                        }
                    } else
                    if (!privLabel.getAccountLogin() && privLabel.getAllowEmailLogin() && !StringTools.isBlank(dftAcctID)) {
                        // EMailAddress login allowed and a DefaultLoginAccount has been specified
                        println(PFX+"ERROR: DefaultLoginAccount specified when emailLogin='true'.");
                        addError("DefaultLoginAccount specified when emailLogin='true' and accountLogin='false'",
                                 "emailLogin='true', accountLogin='false', and a non-blank account-id has been " +
                                 "specified on the 'DefaultLoginAccount' tag",
                                 FIX_PREVIOUS_ERRORS);
                    }
                    if (!dftAcctExists && !StringTools.isBlank(dftAcctID)) {
                        // DefaultLoginAccount has been specified, which does not exist
                        int WC = countWarning("DefaultLoginAccount '"+dftAcctID+"' does not exist");
                        println(PFX+"WARNING["+WC+"]: DefaultLoginAccount '"+dftAcctID+"' does not exist");
                    }
                }
            }
        }

        /* Initialized servers */
        println("");
        println("Device Communication Servers (registered):");
        {
            Set<String> dcUndefSet = ListTools.toSet(DCServerFactory.getUndefinedServerList());
            java.util.List<DCServerConfig> dcServerList = DCServerFactory.getServerConfigList(true);
            if (dcServerList.isEmpty()) {
                printVariable("   (none)", "", "");
            } else {
                int ndx = 1;
                String gtsHomeStr = (env_GTS_HOME != null)? env_GTS_HOME.toString() : "";
                for (DCServerConfig dcs : dcServerList) {
                    String name = dcs.getName();
                    if (dcs.serverJarExists()) {
                        String ndxStr   = StringTools.padLeft(String.valueOf(ndx++), ' ', 2);
                        File jarPath[]  = dcs.getRunningJarPath();
                        boolean running = !ListTools.isEmpty(jarPath);
                        String dcsDesc  = dcs.getDescription();
                        String dcsPorts = dcs.getPortsString();
                        printVariable(ndxStr+") " + name, "[" + dcsPorts + "] " + dcsDesc + (running?" (running)":""), "");
                        if (running) {
                            for (int d = 0; d < jarPath.length; d++) {
                                String jarPathStr = jarPath[d].toString();
                                printVariable("     (running)", " " + jarPathStr, "");
                                if (!StringTools.isBlank(gtsHomeStr) && !jarPathStr.startsWith(gtsHomeStr)) {
                                    int WC = countWarning("DCServer jar path is not in the current GTS_HOME path: " + name);
                                    println(PFX+"WARNING["+WC+"]: DCServer jar path is not in the current GTS_HOME path" );
                                    //Print.sysPrintln("GTS_HOME: " + gtsHomeStr);
                                    //Print.sysPrintln("JAR Path: " + jarPathStr);
                                }
                                File logFile = DCServerConfig.getLogFilePath(jarPath[d]);
                                boolean logExists = ((logFile != null) && logFile.isFile());
                                if (logExists) {
                                    printVariable("     (logfile)", " " + logFile.toString(), "");
                                }
                            }
                        }
                    } else
                    if (dcs.isJarOptional()) {
                        String ndxStr   = StringTools.padLeft(String.valueOf(ndx++), ' ', 2);
                        String dcsDesc  = dcs.getDescription();
                        String dcsPorts = dcs.getPortsString();
                        printVariable(ndxStr+") " + name, "[" + dcsPorts + "] " + dcsDesc + " (no jar)", "");
                    } else
                    if (dcUndefSet.contains(name)) {
                        String ndxStr = StringTools.padLeft(String.valueOf(ndx++), ' ', 2);
                        printVariable(ndxStr+") " + name, dcs.toString(false), "");
                        int WC = countWarning("Referenced server jar does not exist: " + name);
                        println(PFX+"WARNING["+WC+"]: Server jar referenced in runtime config not found: "+name);
                    } else {
                        // ignore these
                    }
                }
            }
        }

        /* Recommended symbolic links */
        File usrLocalDir = new File("/usr/local");
        if (usrLocalDir.isDirectory()) {
            println("");
            println("Recommended symbolic links:");
            // - "/usr/local/gts" to $GTS_HOME
            {
                File link     = new File(usrLocalDir, "gts");
                File target   = FileTools.getRealFile(link);
                File expect   = env_GTS_HOME;
                String envVar = "$GTS_HOME";
                if (target == null) {
                    printVariable(link.toString(), "?", "");
                    int WC = countWarning("Symbolic Link does not exist: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link does not exist: "+link.toString());
                    recommendations.append("- Recommend creating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!target.equals(expect)) {
                    printVariable(link.toString(), target.toString(), "");
                    int WC = countWarning("Symbolic Link is not up to date: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link is not up to date: "+link.toString());
                    recommendations.append("- Recommend recreating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     rm " + link + "\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else {
                    printVariable(link.toString(), target.toString(), "");
                }
            }
            // - "/usr/local/java" to $JAVA_HOME
            {
                File link     = new File(usrLocalDir, "java");
                File target   = FileTools.getRealFile(link);
                File expect   = env_JAVA_HOME;
                String envVar = "$JAVA_HOME";
                if (target == null) {
                    printVariable(link.toString(), "?", "");
                    int WC = countWarning("Symbolic Link does not exist: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link does not exist: "+link.toString());
                    recommendations.append("- Recommend creating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!target.equals(expect)) {
                    printVariable(link.toString(), target.toString(), "");
                    int WC = countWarning("Symbolic Link is not up to date: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link is not up to date: "+link.toString());
                    recommendations.append("- Recommend recreating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     rm " + link + "\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else {
                    printVariable(link.toString(), target.toString(), "");
                }
            }
            // - "/usr/local/tomcat" to $CATALINA_HOME
            {
                // - "/usr/local/tomcat" to $CATALINA_HOME
                File link     = new File(usrLocalDir, "tomcat");
                File target   = FileTools.getRealFile(link);
                File expect   = env_CATALINA_HOME;
                String envVar = "$CATALINA_HOME";
                if (target == null) {
                    printVariable(link.toString(), "?", "");
                    int WC = countWarning("Symbolic Link does not exist: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link does not exist: "+link.toString());
                    recommendations.append("- Recommend creating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!target.equals(expect)) {
                    printVariable(link.toString(), target.toString(), "");
                    int WC = countWarning("Symbolic Link is not up to date: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link is not up to date: "+link.toString());
                    recommendations.append("- Recommend recreating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     rm " + link + "\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else {
                    printVariable(link.toString(), target.toString(), "");
                }
            }
        }
        
        /* separator */
        println("");
        println(eqSep);

        /* display summary of errors */
        int rtnCode = 0;
        println("");
        if (!getErrors().isEmpty()) {
            println("** Found " + getErrors().size() + " Error(s)!");
            println(StringTools.replicateString("*",sepWidth));
            int ndx = 1;
            for (Iterator<String[]> i = getErrors().iterator(); i.hasNext();) {
                String err[] = i.next();
                println((ndx++) + ") " + err[0]);
                if (!StringTools.isBlank(err[1])) {
                    wrapPrintln("   [Reason: " + err[1] + "]", ' ');
                }
                if ((err.length > 2) && !StringTools.isBlank(err[2])) {
                    wrapPrintln("   [Fix: " + err[2] + "]", ' ');
                }
                if ((err.length <= 3) || !err[3].equals("false")) {
                    rtnCode = 1;
                }
            }
            println(StringTools.replicateString("*",sepWidth));
        } else {
            println("No errors reported");
        }

        /* display warning count */
        println("");
        if (warnCount > 0) {
            println("-- Found " + warnCount + " Warning(s):");
            for (Iterator<String> i = getWarnings().iterator(); i.hasNext();) {
                String warnMsg = i.next();
                wrapPrintln(warnMsg, ' ');
            }
        } else {
            println("No warnings reported");
        }
        
        /* display recommendations */
        if (recommendations.length() > 0) {
            println("");
            println("-- Recommendations:");
            println(recommendations.toString().trim());
        }

        /* done */
        println("");
        println(eqSep);
        println(eqSep);
        System.exit(rtnCode);
        

    }
    
    private static StringBuffer getLinkRecommendation(
        StringBuffer sb, AccumulatorLong index, 
        File link, File target, File expected, String envVarName)
    {
        if (link == null) {
            // ignore this recommendation
        } else
        if (target == null) {
            int n = (int)index.next();
            sb.append(n + ") Symbolic link: "+link+" (does not exist)\n");
            sb.append("   Recommend creating symbolic link to point to "+envVarName+":\n");
            sb.append("     ln -s " + expected + " " + link + "\n");
        } else
        if (!target.equals(expected)) {
            int n = (int)index.next();
            sb.append(n + ") Symbolic link: "+link+" (exists, but does not point to the current installation)\n");
            sb.append("   Recommend recreating symbolic link to point to "+envVarName+":\n");
            sb.append("     rm " + link + "\n");
            sb.append("     ln -s " + expected + " " + link + "\n");
        } else {
            int n = (int)index.next();
            sb.append(n + ") Symbolic link: "+link+" (exists, and is up to date)\n");
        }
        return sb;
    }
    
    // ------------------------------------------------------------------------
    
}
