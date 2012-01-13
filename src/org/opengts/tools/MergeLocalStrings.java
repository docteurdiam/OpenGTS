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
//  Merge new Localization strings from 'LocalStrings_en.properties' into existing
//  'LocalStrings_XX.properties' files.
// Notes:
//  - This modules and merges all 'LocalStrings_XX.properties' in memory before saving
//    any of the merged files.  If any errors are encountered, the merge and save are 
//    aborted.  This also means that if the number of LocalStrings files is large, this
//    module may run out of memory, requiring that the java option '-Xmx<memory>' be
//    used to boost the amount of memory available to the Java process (ie. '-Xmx256m')
// Example Usage:
//  To find (but not save) all LocalString_XX.properties files which need to be merged:
//    > java -classpath <classpath> org.opengts.tools.MergeLocalStrings -scan=src
//  To merge and save all LocalString_XX.properties files:
//    > java -classpath <classpath> org.opengts.tools.MergeLocalStrings -merge=src
// ----------------------------------------------------------------------------
// Change History:
//  2008/02/17  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.tools;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class MergeLocalStrings
{
    
    // ------------------------------------------------------------------------

    private static final String LOCALSTRINGS_EN_PROPERTIES  = "LocalStrings_en.properties";
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* this class holds a single 'LocalStrings_XX.properties' instance for merging */
    protected class MergeItem
    {
        private File    xxFile = null;
        private String  xxFileString_old    = null;
        private String  xxFileString_new    = null;
        public MergeItem(File xx, String enFileName, String mergeProps[]) throws IOException {
            this.xxFile = xx;
            
            /* read old file */
            byte xxFileData_old[]  = FileTools.readFile(this.xxFile);
            this.xxFileString_old  = (xxFileData_old != null)? StringTools.toStringValue(xxFileData_old) : null;
    
            /* load "LocalStrings_XX.properties" */
            Properties xxProps = new Properties();
            if (this.xxFile.exists()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(xxFile);
                    xxProps.load(fis);
                } catch (IOException ioe) {
                    Print.logError("Locale read error: " + ioe);
                    throw ioe;
                } finally {
                    try { fis.close(); } catch (Throwable th) {}
                }
            } else {
                // new file 
            }
    
            /* copy array, replace filenames */
            // TODO: could stand some optimization
            String xxFileStrArry[] = new String[mergeProps.length];
            for (int i = 0; i < xxFileStrArry.length; i++) {
                xxFileStrArry[i] = mergeProps[i];
                int p = xxFileStrArry[i].indexOf(enFileName);
                if (p >= 0) {
                    String e = xxFileStrArry[i];
                    xxFileStrArry[i] = e.substring(0,p) + this.xxFile.getName() + e.substring(p + enFileName.length());
                }
            }

            /* merge */
            // TODO: could stand some optimization
            for (Enumeration e = xxProps.propertyNames(); e.hasMoreElements();) {
                String propKey = (String)e.nextElement();
                String propVal = StringTools.replace(xxProps.getProperty(propKey), "\n", "\\n");
                if (propVal != null) {
                    String enk = "#" + propKey + "=";
                    for (int i = 0; i < xxFileStrArry.length; i++) {
                        if (xxFileStrArry[i].startsWith(enk)) {
                            xxFileStrArry[i] = propKey + "=" + propVal;
                        }
                    }
                }
            }

            /* new file contents */
            this.xxFileString_new = StringTools.join(xxFileStrArry,'\n') + "\n";

        }
        public File getFile() {
            return this.xxFile;
        }
        public boolean hasChanged() {
            if (this.xxFileString_new == null) {
                return false;
            } else
            if ((this.xxFileString_old != null) && this.xxFileString_old.equals(this.xxFileString_new)) {
                return false;
            } else {
                return true;
            }
        }
        public boolean save() throws IOException {

            /* do we have data to save? */
            if (this.xxFileString_new == null) {
                throw new IOException("No new 'LocalStrings_XX.properties' data to save");
            }
    
            /* has file changed? */
            if (!this.hasChanged()) {
                //Print.logInfo("New file contents is same as old file contents: " + this.xxFile);
                return false;
            }
    
            /* rename old */
            File xxFile_old = new File(this.xxFile.toString() + ".old");
            if (xxFile_old.exists()) {
                throw new IOException("Old 'LocalStrings_XX.properties' already exists: " + xxFile_old);
            }
            if (!this.xxFile.renameTo(xxFile_old)) {
                throw new IOException("Unable to rename 'LocalStrings_XX.properties' file: " + this.xxFile);
            }
            if (!xxFile_old.exists()) {
                throw new IOException("Renamed, but 'LocalStrings_XX.properties.old' does not exist: " + xxFile_old);
            }
            if (this.xxFile.exists()) {
                throw new IOException("Renamed, but 'LocalStrings_XX.properties' still exist: " + this.xxFile);
            }

            /* save new */
            try {
                byte xxFileData_new[] = this.xxFileString_new.getBytes();
                FileTools.writeFile(xxFileData_new, this.xxFile);
            } catch (IOException ioe) {
                throw ioe;
            }
            
            /* success */
            Print.logInfo("Saved: " + this.xxFile);
            return true;
    
        }
        public String toString() {
            return (this.xxFileString_new != null)? this.xxFileString_new : "";
        }
    }
        
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private File      locale_en_props   = null;
    
    private String    enFileStrArry[]   = null;
    private MergeItem mergeItems[]      = null;

    public MergeLocalStrings(File enFile, File xxFile[])
        throws IOException
    {

        /* "LocalStrings_en.properties" exists? */
        this.locale_en_props = enFile;
        if ((this.locale_en_props == null) || !this.locale_en_props.isFile()) {
            // EN locale file not specified, or does not exist
            throw new FileNotFoundException("LocalStrings_en.properties not found: " + this.locale_en_props);
        }
        
        /* "LocalStrings_en.properties" filename */
        String enFilePath = this.locale_en_props.getParent();
        String enFileName = this.locale_en_props.getName();
        if ((enFilePath == null) || !enFileName.equals(LOCALSTRINGS_EN_PROPERTIES)) {
            // EN locale filename not "LocalStrings_en.properties"
            throw new IOException("Invalid 'LocalStrings_en.properties' filename: " + this.locale_en_props);
        }

        /* Invalid "LocalStrings_XX.properties" file specified? */
        if ((xxFile == null) || (xxFile.length == 0)) {
            // XX locale files not specified
            throw new FileNotFoundException("'LocalStrings_XX.properties' files not specified");
        }

        /* read "LocalStrings_en.properties" */
        byte enFileData[] = FileTools.readFile(this.locale_en_props);
        if (enFileData == null) {
            // unable to read EN locale file
            throw new IOException("Invalid 'LocalStrings_en.properties' file: " + this.locale_en_props);
        }
        this.enFileStrArry = StringTools.split(StringTools.toStringValue(enFileData),'\n');

        /* Invalid "LocalStrings_XX.properties" file specified? */
        this.mergeItems = new MergeItem[xxFile.length];
        for (int i = 0; i < xxFile.length; i++) {
        
            /* Invalid "LocalStrings_XX.properties" file specified? */
            if (xxFile[i] == null) {
                // XX locale file not specified
                throw new FileNotFoundException("Invalid 'LocalStrings_XX.properties' file: " + xxFile[i]);
            }

            /* "LocalStrings_XX.properties" filename */
            String xxFilePath = xxFile[i].getParent();
            String xxFileName = xxFile[i].getName();
            if ((xxFilePath == null) || !xxFileName.startsWith("LocalStrings_") || !xxFileName.endsWith(".properties")) {
                // invalid file name
                throw new IOException("Invalid 'LocalStrings_XX.properties' filename: " + xxFile[i]);
            }
            
            /* old file exists? */
            File xxFile_old = new File(xxFile[i].toString() + ".old");
            if (xxFile_old.exists()) {
                throw new IOException("Old 'LocalStrings_XX.properties' already exists: " + xxFile_old);
            }

            /* same path? */
            if (!enFilePath.equals(xxFilePath)) {
                // paths do not match
                throw new IOException("Invalid 'LocalStrings_XX.properties' filename: " + xxFile[i]);
            }
            
            /* merge */
            this.mergeItems[i] = new MergeItem(xxFile[i], enFileName, this.enFileStrArry);

        }

    }
     
    // ------------------------------------------------------------------------

    /* return true if something has changed */
    public boolean hasChanged()
    {
        if (this.mergeItems != null) {
            for (int i = 0; i < this.mergeItems.length; i++) {
                if (this.mergeItems[i].hasChanged()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // ------------------------------------------------------------------------
    
    /* save */
    public boolean save()
        throws IOException
    {
        
        /* nothing to save */
        if (this.mergeItems == null) {
            throw new IOException("Nothing to save");
        }
        
        /* nothing has changed? */
        if (!this.hasChanged()) {
            return false;
        }
        
        /* loop and save */
        for (int i = 0; i < this.mergeItems.length; i++) {
            this.mergeItems[i].save();
        }
        
        /* saved */
        return true;

    }
    
    // ------------------------------------------------------------------------

    /* string representation */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("English: ");
        sb.append(this.locale_en_props);
        if (this.mergeItems != null) {
            for (int i = 0; i < this.mergeItems.length; i++) {
                sb.append("\n");
                MergeItem mi = this.mergeItems[i];
                sb.append("  --> ");
                sb.append(mi.getFile());
                if (mi.hasChanged()) {
                    sb.append(" [changed]");
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* filter which accepts directories and "LocalStrings_*.properties" files */
    private static FileFilter localStringsFilter = null;
    protected static FileFilter getLocalStringsFilter()
    {
        if (localStringsFilter == null) {
            localStringsFilter = new FileFilter() {
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    } else
                    if (file.isFile()) {
                        String name = file.getName();
                        return (name.startsWith("LocalStrings_") && name.endsWith(".properties"));
                    } else {
                        return false;
                    }
                }
            };
        }
        return localStringsFilter;
    }

    /* recursively decend directory to find all "LocalStrings_*.properties" files */
    protected static boolean _findMergeItems(File subDir, java.util.List<MergeLocalStrings> mergeList)
        throws IOException
    {
        boolean ok = true;
        
        /* is a directory? */
        if (!subDir.isDirectory()) {
            return ok;
        }
        
        /* get files in this directory */
        FileFilter filter = getLocalStringsFilter();
        File filesInSubdir[] = subDir.listFiles(filter);
        if ((filesInSubdir == null) || (filesInSubdir.length <= 0)) {
            return ok;
        }
        
        /* parse files/dirs */
        boolean foundProps   = false;
        File enFile          = null;
        java.util.List<File> files = new Vector<File>();
        java.util.List<File> dirs  = new Vector<File>();
        for (int i = 0; i < filesInSubdir.length; i++) {
            File f = filesInSubdir[i];
            if (f.isFile()) {
                foundProps = true;
                if (f.getName().equals(LOCALSTRINGS_EN_PROPERTIES)) {
                    enFile = f;
                } else {
                    files.add(f);
                }
            } else
            if (f.isDirectory()) {
                dirs.add(f);
            }
        }

        /* display discovered LocalStrings files */
        if (foundProps) {
            if (enFile != null) {
                File xxFile[] = files.toArray(new File[files.size()]);
                mergeList.add(new MergeLocalStrings(enFile,xxFile));
            } else {
                ok = false;
                Print.sysPrintln("*** MISSING: " + LOCALSTRINGS_EN_PROPERTIES);
                for (Iterator f = files.iterator(); f.hasNext();) {
                    File xxFile = (File)f.next();
                    Print.sysPrintln("   ==> " + xxFile + "  [skipped]");
                }
            }
        }

        /* recurse/decend into sub-directories */
        for (Iterator d = dirs.iterator(); d.hasNext();) {
            if (!_findMergeItems((File)d.next(),mergeList)) {
                ok = false;
            }
        }
        
        /* return ok? */
        return ok;

    }
    
    /* return a list of all discovered merged "LocalStrings_*.properties" files */
    protected static java.util.List/*<MergeLocalStrings>*/ findMergeItems(File subDir)
        throws IOException
    {
        java.util.List<MergeLocalStrings> mergeItems = new Vector<MergeLocalStrings>();
        if (_findMergeItems(subDir,mergeItems)) {
            return mergeItems;
        } else {
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main entry point
    
    private static final String ARG_SCAN[]      = new String[] { "scan" };
    private static final String ARG_MERGE[]     = new String[] { "merge" };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        
        /* scan for "LocalStrings_en.properties" */
        if (RTConfig.hasProperty(ARG_SCAN)) {
            try {
                File scanDir = RTConfig.getFile(ARG_SCAN,new File("."));
                java.util.List list = findMergeItems(scanDir);
                if (list != null) {
                    Print.sysPrintln("");
                    for (Iterator i = list.iterator(); i.hasNext();) {
                        MergeLocalStrings mls = (MergeLocalStrings)i.next();
                        Print.sysPrintln(mls.toString());
                        Print.sysPrintln("");
                    }
                }
            } catch (IOException ioe) {
                Print.sysPrintln("");
                Print.sysPrintln("Scan error: " + ioe.getMessage());
            }
            System.exit(0);
        }
        
        /* merge/save "LocalStrings_en.properties" */
        if (RTConfig.hasProperty(ARG_MERGE)) {
            try {
                File scanDir = RTConfig.getFile(ARG_MERGE,new File("."));
                java.util.List list = findMergeItems(scanDir);
                if (list != null) {
                    for (Iterator i = list.iterator(); i.hasNext();) {
                        MergeLocalStrings mls = (MergeLocalStrings)i.next();
                        Print.sysPrintln(mls.toString());
                        mls.save();
                        Print.sysPrintln("");
                    }
                }
            } catch (IOException ioe) {
                Print.sysPrintln("");
                Print.sysPrintln("Merge error: " + ioe.getMessage());
            }
            System.exit(0);
        }
        
        /* usage */
        Print.sysPrintln("Missing options:");
        Print.sysPrintln("  -scan=<sourceDir>      Display list of LocalStrings_XX.properties files");
        Print.sysPrintln("  -merge=<sourceDir>     Merged/save LocalStrings_XX.properties files");
        Print.sysPrintln("Notes:");
        Print.sysPrintln("  1) Scan/Merge process will terminate if any errors are encountered");

    }

}
