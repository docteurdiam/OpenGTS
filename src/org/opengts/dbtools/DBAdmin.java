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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/09  Martin D. Flynn
//     -Integrated 'DBException'
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/02/25  Martin D. Flynn
//     -Added ability to dump/load CSV files
//  2007/06/13  Martin D. Flynn
//     -Added 'displayWarnings' flag to 'validateTableColumns(...)'
//     -Table column validations now defaults to suppressing warnings.  To display
//      warnings, the "-warn" command-line option must be specified.
//  2007/06/30  Martin D. Flynn
//     -If argument "-db=xx" has been specified, it must match the DB name specified
//      by RTKey.DB_NAME.
//     -Add optional "where" argument for use by "dump" command.
//  2007/07/13  Martin D. Flynn
//     -Added table name option to 'validateTableColumns(...)'
//     -Added table 'reload' option.
//  2007/07/27  Martin D. Flynn
//     -Check for empty filename specified on "-load"
//     -Added 'validateMask' to 'validateTableColumns' method
//     -Added table validation feature to add/change missing columns
//  2007/08/09  Martin D. Flynn
//     -Added table validation feature to show defined/actual columns
//  2007/09/16  Martin D. Flynn
//     -Added support for dumping all or multiple files.
//     -Print additional column information when using 's' flag with "-tables" option.
//  2008/02/27  Martin D. Flynn
//     -Made 'getTableFactoryMap()' public
//  2008/03/28  Martin D. Flynn
//     -Commented table 'reload' option (it was a bit dangerous)
//     -Data '-load' will not overwrite existing data by default.  The option 
//      '-overwrite' is now required to overwrite existing data.
//  2008/04/11  Martin D. Flynn
//     -Added '-schema' command-line option for printing the current database table
//      schema to stdout.
//  2008/06/20  Martin D. Flynn
//     -Added method 'addTableFactory(String,boolean)'
//  2009/09/23  Martin D. Flynn
//     -Enable VALIDATE_CREATE_TABLE[t] option if VALIDATE_ADD_COLUMNS[c] specified 
//      on "-tables" command-line argument.
//  2011/03/08  Martin D. Flynn
//     -Added "validateTableColumns" check for invalid primary/alternate keys
//  2011/04/01  Martin D. Flynn
//     -Added support for specifying multiple table names on the "-load" option.
//  2011/06/16  Martin D. Flynn
//     -Added command-line option to update a specific table only.
//     -Change DBFactory 'f.isOptional()' method call to '!f.isRequired()'
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBAdmin</code> provides database administrative functionality
**/

// ------------------------------------------------------------------------
// Initalizing user/password/database:
//   % su root
//   % mysql -u root [-p]
//   Remove anonymous users:
//     mysql> DELETE FROM mysql.user WHERE User='';
//   Remove 'root' access from anywhere but 'localhost':
//     mysql> DELETE FROM mysql.user WHERE User='root' AND Host!='localhost';
//   Create 'dbtest' database:
//     mysql> CREATE DATABASE dbtest;
//   Grant priviledges:
//     mysql> 
//     mysql> GRANT ALL ON dbtest.* TO userroot@"%" IDENTIFIED BY 'passwd' WITH GRANT OPTION;
//   Flush priviledge changes:
//     mysql> FLUSH PRIVILEGES;
//
// Troubleshooting:
//   If you get this kind of error message, even when using what you believe to be
//   the proper passwords:
//     ERROR 1045 (28000): Access denied for user 'root'@'localhost'
//   Then the following may help:
//   References:
//     http://dev.mysql.com/doc/mysql/en/access-denied.html
//     http://dev.mysql.com/doc/mysql/en/resetting-permissions.html
//     http://dev.mysql.com/doc/mysql/en/privilege-system.html
//   Database initialization
//     % mysql_install_db --user mysql
//   Reset 'root' password:
//     % /usr/sbin/mysqld --skip-grant-tables --user=root &
//     % mysql -u root
//     mysql> UPDATE mysql.user SET Password=PASSWORD('rootpass') WHERE User='root';
//     mysql> FLUSH PRIVILEGES;
//
// Create 'dbtest' database:
//   % bin/DBAdmin -createdb
// Create 'dbtest' tables:
//   % bin/DBAdmin -tables
//
// Querying 'dbtest' tables:
//   % mysql [-h <host>] -u userroot -p [dbtest]
//   Enter Password: passwd
//   mysql> [use dbtest]
//
// ------------------------------------------------------------------------

public class DBAdmin
{

    // ------------------------------------------------------------------------

    private static final boolean SKIP_OPTIONAL_TABLES   = false;
    
    private static final String  DUMP_ALL               = "all";

    // ------------------------------------------------------------------------

    /**
    *** Private constructor
    **/
    private DBAdmin()
    {
        //
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the DBFactory instance for the specified class.<br>
    *** This assumes that the DBRecord subclass represented by the specified class name
    *** implements the static method "getFactory()".
    *** @param className  The class name of the DBRecord subclass
    *** @param isRequired True if this class must be resolved (any errors will be displayed),
    ***                   false if it is optional (errors may be quietly ignored).
    *** @return The returned DBFactory instance, or null if unable to load the specifid DBFactory
    **/
    protected static DBFactory<? extends DBRecord> _createDBRecordFactory(String className, boolean isRequired)
    {
        try {
            //Print.logInfo("Loading DBFactory: " + className);
            DBFactory<? extends DBRecord> dbFact = 
                (DBFactory<? extends DBRecord>)(new MethodAction(className,"getFactory")).invoke();
            if (dbFact != null) {
                dbFact.setRequired(isRequired);
                return dbFact;
            } else {
                Print.logError("\"" + className + ".getFactory()\" returned NULL!");
                return null;
            }
        } catch (ClassNotFoundException cnfe) {
            if (isRequired) {
                Print.logError("Required DBFactory not found: " + className);
            } else {
                Print.logDebug("Optional DBFactory not found: " + className);
            }
            return null;
        } catch (Throwable t) { // NoClassDefFoundError, ClassCastException, MethodInvocationException, ...
            if (isRequired) {
                Print.logError("Required DBFactory Error: " + className + " [" + t + "]");
                //Print.logException("Error creating DBFactory instance: " + className, t);
            } else
            if (t instanceof NoClassDefFoundError) {
                // This load failure likely due to a missing dependency of the specified class
                String missing = t.getMessage().replace('/','.');
                Print.logDebug("Optional DBFactory not found: " + className + " [missing " + missing + "]");
                //Print.logException("NoClassDefFoundError: " + className, t);
            } else {
                Print.logError("Optional DBFactory Error: " + className + " [" + t + "]");
            }
            return null;
        }
    }

    /**
    *** Loads the DBFactory for the specified DBRecord classname, and adds it to the table name/factory map 
    *** This assumes that the DBRecord subclass represented by the specified class name
    *** implements the static method "getFactory()".
    *** @param className  The class name of the DBRecord subclass
    *** @param isRequired True if this class must be resolved, false if it is optional
    *** @return True if the DBFactory was succesfully loaded/added.
    **/
    public static boolean addTableFactory(String className, boolean isRequired)
    {
        DBFactory<? extends DBRecord> fact = DBAdmin._createDBRecordFactory(className, isRequired);
        if (fact != null) {
            DBAdmin.addTableFactory(fact);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified table to the table name/factory map 
    *** @param factory The table factory
    **/
    public static void addTableFactory(DBFactory<? extends DBRecord> factory)
    {
        if (factory == null) {
            //Print.logStackTrace("Attempting to add a null DBFactory");
        } else
        if (factory.getFieldCount() <= 0) {
            Print.logStackTrace("Ignoring DBFactory with no fields: " + factory.getUntranslatedTableName());
        } else {
            String utableNameLC = factory.getUntranslatedTableName().toLowerCase();
            if (DBAdmin.getTableFactoryMap().containsKey(utableNameLC)) {
                Print.logWarn("Overriding existing DBFactory: %s", factory.getUntranslatedTableName());
            }
            DBAdmin.getTableFactoryMap().put(utableNameLC, factory);
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Provides the opportunity for the DBAdmin to decide whether or not the specified
    *** DBField should be added/included in the DBFactory.  Typically, an external 
    *** configuration manager will provide the actual decision.
    *** @param fact     The DBFactory
    *** @param field    The DBField which is to be added to the DBFactory
    *** @return True if the DBField should be added to the DBFactory
    **/
    public static boolean includeFieldInFactory(DBFactory fact, DBField field)
    {
        // TODO:
        return true;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static OrderedMap<String,DBFactory<? extends DBRecord>> tableFactoryMap = null;
    
    /**
    *** Returns the number of registered DBFactory classes
    *** @return The number of registered DBFactory classes
    **/
    public static int getTableFactoryCount()
    {
        return (tableFactoryMap != null)? tableFactoryMap.size() : 0;
    }

    /**
    *** Returns a map of all defined table names to their table DBFactory
    *** @return The table name/factory map
    **/
    public static OrderedMap<String,DBFactory<? extends DBRecord>> getTableFactoryMap()
    {
        if (tableFactoryMap == null) {
            tableFactoryMap = new OrderedMap<String,DBFactory<? extends DBRecord>>();
        }
        return tableFactoryMap;
    }

    /**
    *** Gets all child/dependent table factories for the specified table factory
    *** @param parentFactory  The parent table factory
    *** @return The child/dependent table factories of the specified parent factory
    **/
    public static DBFactory<? extends DBRecord>[] getChildTableFactories(DBFactory<? extends DBRecord> parentFactory)
    {
        java.util.List<DBFactory<? extends DBRecord>> list = new Vector<DBFactory<? extends DBRecord>>();
        if (parentFactory != null) {
            String uparentTableName = parentFactory.getUntranslatedTableName();
            for (DBFactory<? extends DBRecord> childFact : DBAdmin.getTableFactoryMap().values()) {
                if (childFact.hasParentTable(uparentTableName)) {
                    // BTW, since tables don't specify themselves as a parent, 'hasParentTable'
                    // will return false if the child is the parent.
                    list.add(childFact);
                }
            }
        }
        return (DBFactory<? extends DBRecord>[])list.toArray(new DBFactory[list.size()]);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the table factory for the specified table name
    *** @param utableName The untranslated table name
    *** @return The table factory
    **/
    private static DBFactory<? extends DBRecord> _getTableFactory(String utableName)
    {
        if (utableName != null) {
            String utableNameLC = utableName.toLowerCase();
            return (DBFactory<? extends DBRecord>)DBAdmin.getTableFactoryMap().get(utableNameLC);
        } else {
            return null;
        }
    }

    /**
    *** Returns the table factory for the specified table name
    *** @param utableName The untranslated table name
    *** @return The table factory
    **/
    public static DBFactory<? extends DBRecord> getTableFactory(String utableName)
    {
        if (StringTools.isBlank(utableName)) {
            Print.logWarn("Table name is null/blank");
            return null;
        } else {
            DBFactory<? extends DBRecord> fact = DBAdmin._getTableFactory(utableName);
            if (fact == null) {
                Print.logWarn("Table factory class not found: " + utableName);
                Print.logStackTrace("Table factory class not found: " + utableName);
                return null;
            } else {
                return fact;
            }
        }
    }

    /**
    *** Returns true if the spcified table name has been defined
    *** @param utableName The untranslated table name
    *** @return True if the table has been defined, false otherwise
    **/
    public static boolean hasTableFactory(String utableName)
    {
        return (DBAdmin.getTableFactory(utableName) != null);
    }
    
    /**
    *** Returns the proper case of the specified table name.  
    *** For example, assuming that "Account" is a defined table name, passing "accounT" to this
    *** method will return "Account".
    *** @param utableName The untranslated table name
    *** @return The table name with the proper 'case'
    **/
    public static String getTableNameProperCase(String utableName)
    {
        DBFactory fact = DBAdmin.getTableFactory(utableName);
        return (fact != null)? fact.getUntranslatedTableName() : utableName;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // Note: These constants have no external dependencies.  
    // These may be changed, however, note that these values are "final", thus any other  
    // module that references these values will need to be recompiled.
    public static final int VALIDATE_CREATE_TABLE           = 0x00000001;   // 't'
    public static final int VALIDATE_ADD_COLUMNS            = 0x00000002;   // 'c'
    public static final int VALIDATE_ALTER_COLUMNS          = 0x00000004;   // 'a'
    public static final int VALIDATE_REBUILD_KEYS           = 0x00000008;   // 'k'
    public static final int VALIDATE_CHECK_ENCODING         = 0x00000010;   // 'u' (requires 'a' or 'w')
    public static final int VALIDATE_DISPLAY_ERRORS         = 0x00000100;   // 'e'
    public static final int VALIDATE_DISPLAY_WARNINGS       = 0x00000200;   // 'w'
    public static final int VALIDATE_SHOW_COLUMNS           = 0x00000400;   // 's'
    public static final int VALIDATE_NAMED_TABLE_ONLY       = 0x00000800;   // 'n'

    /**
    *** Validate table column for the specified table name
    *** @param validateMask  A bitmask containing the type of validation to perform
    *** @param utableName    The untranslated table name on which the validation is to be performed
    *** @return True if the validation was performed successfully, false otherwise
    **/
    private static boolean validateTableColumns(final int validateMask, String utableName)
    {
        boolean validatedColumns = true;
        int sepLen = 70;

        /* header */
        Print.sysPrintln("");
        Print.sysPrintln("Validating table columns ...");
        //Print.sysPrintln("[mask == 0x" + StringTools.toHexString(validateMask,16)+"]");
        Print.sysPrintln(StringTools.replicateString("-",sepLen));

        /* named table only */
        if (((validateMask & VALIDATE_NAMED_TABLE_ONLY) != 0) && 
            StringTools.isBlank(utableName)                     ) {
            Print.sysPrintln("  Table name not specified.");
            return false;
        }

        /* validate */
        OrderedMap<String,DBFactory<? extends DBRecord>> factMap = DBAdmin.getTableFactoryMap();
        for (Iterator<String> i = factMap.keyIterator(); i.hasNext();) {
            String tn = i.next(); // table name

            /* specific table */
            if (!StringTools.isBlank(utableName) && !utableName.equalsIgnoreCase(tn)) {
                //Print.logInfo("No match: " + tn);
                continue;
            }

            /* display table information */
            DBFactory<? extends DBRecord> f = (DBFactory<? extends DBRecord>)factMap.get(tn);
            try {
                long rc = -1L;
                StringBuffer sb = new StringBuffer();
                sb.append("  Table ");
                sb.append(StringTools.leftAlign("'" + f.getUntranslatedTableName() + "'", 25));
                if (!f.tableExists()) {
                    sb.append(StringTools.rightAlign("["+rc+"]", 9));
                    if ((validateMask & VALIDATE_CREATE_TABLE) != 0) {
                        sb.append(" Creating table ...");
                        Print.sysPrintln(sb.toString());
                        f.createTable();
                    } else {
                        sb.append(" Table does not exist!");
                        Print.sysPrintln(sb.toString());
                    }
                } else {
                    rc = DBRecord.getRecordCount(f,"");
                    sb.append(StringTools.rightAlign("["+rc+"]", 9));
                    sb.append(" Validating columns ...");
                    Print.sysPrintln(sb.toString());
                    validatedColumns = f.validateColumns(validateMask) && validatedColumns;
                }
            } catch (DBException dbe) {
                Print.sysPrintln("    ERROR: Unable to create/validate table '" + f.getUntranslatedTableName() + "'");
                dbe.printException();
            }

        }

        /* check validation results */
        Print.sysPrintln(StringTools.replicateString("-",sepLen));
        if (!validatedColumns) {
            boolean addCol      = ((validateMask & VALIDATE_ADD_COLUMNS   ) != 0);
            boolean alterTypes  = ((validateMask & VALIDATE_ALTER_COLUMNS ) != 0);
            boolean rebuildKeys = ((validateMask & VALIDATE_REBUILD_KEYS  ) != 0);
            if (addCol || alterTypes || rebuildKeys) {
                Print.sysPrintln("WARNING: Attempted repair of column validation errors");
                Print.sysPrintln("Rerun with option '-tables' to see if errors have been repaired.");
            } else {
                Print.sysPrintln("ERROR: Fix/Recheck column validation errors");
                Print.sysPrintln("Depending on the error displayed, rerun with one of the following options:");
                Print.sysPrintln("  '-tables=c'   to add missing columns");
                Print.sysPrintln("  '-tables=ca'  to also alter existing column types");
                Print.sysPrintln("  '-tables=cak' to also recreate key indexes");
            }
        } else {
            Print.sysPrintln("Column validation completed successfully.");
        }
        Print.sysPrintln(StringTools.replicateString("-",sepLen));
        Print.sysPrintln("");
        return validatedColumns;

    }

    // ------------------------------------------------------------------------

    /** 
    *** Validates the specified table configuration as defined in the SQL database
    *** @param utableName The untranslated table name to validate
    *** @param inclWarn   True to display all warnings
    **/
    private static void validateTables(String utableName, boolean inclWarn)
    {
        // 'utableName' is non-null if the caller wants to validate a specific table
        boolean tableFound = false;
        OrderedMap<String,DBFactory<? extends DBRecord>> factMap = DBAdmin.getTableFactoryMap();
        for (Iterator<String> i = factMap.keyIterator(); i.hasNext();) {
            String tn = i.next();
            DBFactory<? extends DBRecord> f = (DBFactory<? extends DBRecord>)factMap.get(tn);
            try {
                if (StringTools.isBlank(utableName) || tn.equalsIgnoreCase(utableName)) {
                    if (!f.tableExists()) {
                        Print.logError("Table does not exist '" + f.getUntranslatedTableName() + "'");
                    } else {
                        f.validateTable(inclWarn);
                        tableFound = true;
                    }
                }
            } catch (DBException dbe) {
                Print.logError("Unable to validate table '" + f.getUntranslatedTableName() + "'");
            }
        }
        if (!tableFound) {
            Print.logError("Table not found '" + utableName + "'");
        }
        Print.logInfo("");
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the specified text, wrapping it to the specified width
    *** @param text  The text to print
    *** @param width The maximum width
    **/
    private static void _printHeaderText(String text, int width)
    {
        String s[] = StringTools.parseString(text, "\n");
        for (int i = 0; i < s.length; i++) {
            while (s[i].length() > width) {
                int ch = width;
                while ((ch > 0) && !Character.isWhitespace(s[i].charAt(ch))) { ch--; }
                if (ch > 0) {
                    Print.sysPrintln(s[i].substring(0,ch+1).trim());
                    s[i] = s[i].substring(ch+1).trim();
                } else {
                    break;
                }
            }
            if (s[i].length() > 0) {
                Print.sysPrintln(s[i]);
            }
        }
    }
    
    /**
    *** Prints a db schema row
    **/
    private static void _printSchemaRow(String index, String name, String title, String sqlType, String keyType)
    {
        int widIndex   =  3;
        int widName    = 22;
        int widTitle   = 30;
        int widSqlType = 28;
        int widKeyType =  8;
        if (index   == null) { index   = StringTools.replicateString("-",widIndex  ); }
        if (name    == null) { name    = StringTools.replicateString("-",widName   ); }
        if (title   == null) { title   = StringTools.replicateString("-",widTitle  ); }
        if (sqlType == null) { sqlType = StringTools.replicateString("-",widSqlType); }
        if (keyType == null) { keyType = StringTools.replicateString("-",widKeyType); }
        StringBuffer sb = new StringBuffer();
        sb.append(" ");
        sb.append(StringTools.rightAlign(index  ,widIndex  )).append(" ");
        sb.append(StringTools.leftAlign (name   ,widName   )).append(" ");
        sb.append(StringTools.leftAlign (title  ,widTitle  )).append(" ");
        sb.append(StringTools.leftAlign (sqlType,widSqlType)).append(" ");
        sb.append(StringTools.leftAlign (keyType,widKeyType));
        Print.sysPrintln(sb.toString());
    }

    /**
    *** Print the database schema for the managed tables
    *** @param utableName The specific untranslated table name for which the schema will be printed.
    ***                   If null, the schema for all tables will be printed.
    **/
    public static void printTableSchema(int docWidth, String header[], String utableName)
    {
        // 'utableName' is non-null if the caller wants to print the schema for a specific table

        /* English locale */
        Locale locale = I18N.getLocale("en");

        /* header */
        if (!ListTools.isEmpty(header)) {
            Print.sysPrintln(StringTools.replicateString("=",docWidth+1));
            for (String h : header) {
                if (StringTools.isBlank(h)) {
                    Print.sysPrintln("");
                } else
                if (h.length() < docWidth) {
                    Print.sysPrintln(h);
                } else {
                    DBAdmin._printHeaderText(h, docWidth);
                }
            }
        }

        /* loop through tables */
        int tablesFound = 0;
        OrderedMap<String,DBFactory<? extends DBRecord>> factMap = DBAdmin.getTableFactoryMap();
        for (Iterator<String> i = factMap.keyIterator(); i.hasNext();) {
            String tn = i.next();
            if (!StringTools.isBlank(utableName) && !tn.equalsIgnoreCase(utableName)) {
                // skip this table 
                continue;
            }
            tablesFound++;

            /* table factory */
            DBFactory<? extends DBRecord> f = (DBFactory<? extends DBRecord>)factMap.get(tn);
            
            /* table exists */
            boolean tableExists = false;
            try {
                if (f.tableExists()) {
                    tableExists = true;
                } else {
                    //Print.sysPrintln("Table does not exist '" + f.getUntranslatedTableName() + "'");
                    //continue;
                }
            } catch (DBException dbe) {
                //Print.logError("Unable to print schema for table '" + f.getUntranslatedTableName() + "'");
                //continue;
            }

            /* omit optional tables? */
            boolean isRequired = f.isRequired();
            if (SKIP_OPTIONAL_TABLES && !isRequired) {
                // for now, omit all optional tables.
                continue;
            }
            
            /* attributes */
            StringBuffer attr = new StringBuffer();
            attr.append(" [");
            attr.append(isRequired?"required":"optional");
            //attr.append(tableExists?",exists" :",missing");
            attr.append("]");

            /* table header */
            Print.sysPrintln("");
            Print.sysPrintln("");
            Print.sysPrintln(StringTools.replicateString("=",docWidth+1));
            Print.sysPrintln("Table: " + f.getUntranslatedTableName() + attr);
            Print.sysPrintln("Class: " + StringTools.className(f.getRecordClass()));
            Print.sysPrintln("");
            DBAdmin._printHeaderText(f.getDescription(locale), docWidth);
            Print.sysPrintln("");

            /* field header */
            DBAdmin._printSchemaRow("##", "Column", "Description", "SQL Type", "Key");
            DBAdmin._printSchemaRow(null,     null,          null,       null,  null);

            /* fields */
            DBField colDef[] = f.getFields();
            for (int c = 0; c < colDef.length; c++) {
                String  index   = String.valueOf(c + 1);
                String  name    = colDef[c].getName();
                String  title   = colDef[c].getTitle(locale);
                String  sqlType = colDef[c].getSqlType(false);
                boolean isAuto  = colDef[c].isAutoIncrement();
                boolean isUTF8  = colDef[c].isUTF8();
                if (isAuto || isUTF8) {
                    sqlType += " [";
                    if (isAuto) {
                        sqlType += "auto"; 
                    }
                    if (isUTF8) {
                        if (isAuto) { sqlType += ","; }
                        sqlType += "utf8"; 
                    }
                    sqlType += "]";
                }
                String  keyType = colDef[c].getIndexNames();
                DBAdmin._printSchemaRow(index, name, title, sqlType, keyType);
            }

        }
        
        /* no tables found? */
        if (tablesFound <= 0) {
            Print.sysPrintln("Table(s) not found");
        }
        
        Print.sysPrintln("");
        Print.sysPrintln("");
        
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Verify that all defined tables actually exist
    *** @return True if all tables exist
    **/
    public static boolean verifyTablesExist()
    {
        boolean allOK = true;
        OrderedMap<String,DBFactory<? extends DBRecord>> factMap = DBAdmin.getTableFactoryMap();
        // look through registered tables.
        for (Iterator<String> i = factMap.keyIterator(); i.hasNext();) {
            String tn = i.next();
            DBFactory<? extends DBRecord> f = (DBFactory<? extends DBRecord>)factMap.get(tn);
            try {
                if (!f.tableExists()) {
                    // table does not exist in database
                    if (f.isRequired()) {
                        Print.logError("Required table not found: " + f.getUntranslatedTableName());
                        allOK = false;
                    } else {
                        Print.logWarn("Optional table not found: " + f.getUntranslatedTableName());
                    }
                }
            } catch (DBException dbe) {
                // Message:   Table './gts/EventData' is marked as crashed and should be repaired
                // SQLState:  HY000
                // ErrorCode: 145
                Print.logError("Error checking for table '" + f.getUntranslatedTableName() + "'");
                dbe.printException();
                allOK = false;
            }
        }
        return allOK;
    }

    // ------------------------------------------------------------------------
    
    private static String DEFAULT_DIRECTORY = File.separator + "tmp";
    
    /**
    *** Creates and returns the default dump file directory
    *** @return The default dump file directory
    **/
    public static File getDumpDirectory()
    {
        String dirName = DEFAULT_DIRECTORY + File.separator + DBProvider.getDBName();
        return new File(dirName);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** "DBAdmin.execCommands()" return status enumerated type
    **/
    public enum DBAdminExec {
        NONE,
        OK,
        EXIT,
        WARN,
        ERROR
    };

    // ------------------------------------------------------------------------

    public  static final String ARG_ROOT_USER[] = new String[] { "rootUser"  , "user" };
    public  static final String ARG_ROOT_PASS[] = new String[] { "rootPass"  , "pass" };
    public  static final String ARG_CREATE_DB[] = new String[] { "createdb"  };
    public  static final String ARG_GRANT[]     = new String[] { "grant"     };
    public  static final String ARG_DB[]        = new String[] { "db"        };
    public  static final String ARG_DIR[]       = new String[] { "dir"       };
    public  static final String ARG_LOAD[]      = new String[] { "load"      };
    public  static final String ARG_DROP[]      = new String[] { "drop"      };
    public  static final String ARG_TABLES[]    = new String[] { "tables"    };
    public  static final String ARG_TABLENAME[] = new String[] { "tableName" , "name" };
    public  static final String ARG_SCHEMA[]    = new String[] { "schema"    };
    public  static final String ARG_DUMP[]      = new String[] { "dump"      };
    public  static final String ARG_WHERE[]     = new String[] { "where"     };
    public  static final String ARG_VALIDATE[]  = new String[] { "validate"  };
    public  static final String ARG_LAST[]      = new String[] { "last"      };
    public  static final String ARG_OVERWRITE[] = new String[] { "overwrite" };
    public  static final String ARG_BEAN[]      = new String[] { "bean"      };
  //public  static final String ARG_HIBXML[]    = new String[] { "hibxml"    };
  //public  static final String ARG_RELOAD[]    = new String[] { "reload"    };

    /**
    *** Execute command defined in the runtime-config properties (RTConfig)
    *** @return One of the following DBAdminExec status codes: ERROR to represent an error condition, 
    ***         NONE to represent that no command was executed, EXIT to represent that a command was 
    ***         executed and that the caller of this method should exit, OK to represent that a command
    ***         was executed, but the caller should continue.
    **/
    @SuppressWarnings("unchecked")
    public static DBAdminExec execCommands() // return
    {
        RTConfig.setBoolean(RTKey.LOG_EMAIL_EXCEPTIONS, false);
        int execCmd = 0;
        // -createdb -rootUser=<Root_User> -rootPass=<Root_Pass>
        // -grant -rootUser=<Root_User> -rootPass=<Root_Pass> -user=<Grant_User> -pass=<Grant_Pass>
        // -tables
        // -dump=<table> -dir=<Destination_Dir>
        // -load=<table> -dir=<Source_Dir>
        // -drop=<table>

        /* if specified, the argument specified for "-db" MUST match 'DBProvider.getDBName()' */
        if (RTConfig.hasProperty(ARG_DB)) {
            String dbArg  = RTConfig.getString(ARG_DB,"");
            String dbName = DBProvider.getDBName();
            if (StringTools.isBlank(dbName)) {
                // config db not specified, use arg db
                RTConfig.setString(RTKey.DB_NAME,dbArg);
            } else
            if (!dbName.equals(dbArg)) {
                Print.logError("Argument DB ("+dbArg+") does not match config DB ("+dbName+")");
                return DBAdminExec.ERROR;
            }
        }
        
        /* creatdb */
        // bin/exe DBAdmin -createdb
        //    -rootUser=<Root_User>
        //    -rootPass=<Root_Pass>
        //    [-db.sql.name=<DataBase_Name>]
        if (RTConfig.getBoolean(ARG_CREATE_DB, false)) {
            execCmd++;
            String rootUser = RTConfig.getString(ARG_ROOT_USER, null);
            String rootPass = RTConfig.getString(ARG_ROOT_PASS, null);
            try {
                DBProvider.createDatabase(rootUser,rootPass);
            } catch (SQLException sqe) {
                String dbName = DBProvider.getDBName();
                String sqlMsg = sqe.getMessage();
                int errCode   = sqe.getErrorCode();
                if (errCode == DBFactory.SQLERR_DATABASE_EXISTS) {  // MySQL: ?
                    Print.logWarn("Database already exists: " + dbName);
                } else
                if (sqlMsg.indexOf("already exists" ) >= 0) { // PostgreSQL: ?
                    Print.logWarn("Database already exists: " + dbName);
                } else {
                    Print.logError("SQLException message: " + sqlMsg);
                    Print.logSQLError("DB create error [" + DBProvider.getDBUri(false) + "]", sqe);
                    return DBAdminExec.ERROR;
                }
            } catch (DBException dbe) {
                Print.logException("DB create error [" + DBProvider.getDBUri(false) + "]", dbe);
            }
        }

        /* grant */
        // bin/exe DBAdmin -grant
        //    -rootUser=<Root_User>
        //    -rootPass=<Root_Pass>
        //    [-db.sql.user=<Grant_User>]
        //    [-db.sql.pass=<Grant_Pass>]
        //    [-db.sql.name=<DataBase_Name>]
        if (RTConfig.getBoolean(ARG_GRANT, false)) {
            execCmd++;
            String rootUser = RTConfig.getString(ARG_ROOT_USER, null);
            String rootPass = RTConfig.getString(ARG_ROOT_PASS, null);
            try {
                DBProvider.grantDBUser(rootUser, rootPass);
            } catch (SQLException sqe) {
                Print.logSQLError("DB grant error [" + DBProvider.getDBUri(false) + "]", sqe);
                return DBAdminExec.ERROR;
            } catch (DBException dbe) {
                Print.logException("DB grant error [" + DBProvider.getDBUri(false) + "]", dbe);
            }
        }
        
        // -----------------------------------
        // Options beyond this point
        //   -dump
        //   -drop
        //   -load
        //   -tables
        //   -validate
        //   -last
        //   -bean
        //   -hibxml [experimental]

        /* connect to db */
        try {
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDefaultConnection();
                dbc.getConnection();
            } finally {
                DBConnection.release(dbc);
            }
        } catch (SQLException sqe) {
            String uri = DBProvider.getDBUri(true); // includes dbName
            Print.logSQLError("Connection error [" + uri + "]", sqe);
            return DBAdminExec.ERROR;
        }

        /* input/output directory */
        File dumpDir = RTConfig.getFile(ARG_DIR, getDumpDirectory());
        File loadDir = RTConfig.getFile(ARG_DIR, getDumpDirectory());
        //Print.logInfo("Output dir: " + dumpDir);
        
        /* reload */
        /*
        // bin/exe DBAdmin -reload=<table> -dir=<Temp_Dir>
        String reloadTable = RTConfig.getString(ARG_RELOAD, null);
        if (reloadTable != null) {
            execCmd++;
            if (FileTools.hasFileSeparator(reloadTable) || (reloadTable.indexOf(".") >= 0)) {
                Print.logError("'-reload' must specify a simple table name");
                return DBAdminExec.ERROR;
            }
            // db factory
            DBFactory fact = DBAdmin.getTableFactory(reloadTable);
            if (fact == null) {
                Print.logError("No DBFactory for table: " + reloadTable);
                return DBAdminExec.ERROR;
            }
            // header
            Print.logInfo("Reloading table: " + reloadTable);
            // starting record count
            long startingRecordCount = 0L;
            try {
                startingRecordCount = DBRecord.getRecordCount(fact,"");
                Print.logInfo("  Starting record count = " + startingRecordCount);
            } catch (Throwable th) { // SQLException, DBException
                Print.logException("Error retrieve initial table record count: " + reloadTable, th);
                // table is still intact
                return DBAdminExec.ERROR;
            }
            // dump
            Print.logInfo("  Dumping ...");
            File reloadFile = new File(dumpDir, reloadTable + DBFactory._DUMP_EXT_TXT);
            try {
                fact.dumpTable(reloadFile, null);
            } catch (DBException dbe) {
                Print.logException("Error dumping table: " + reloadTable, dbe);
                // table is still intact
                return DBAdminExec.ERROR;
            }
            // drop
            Print.logInfo("  Dropping ...");
            try {
                fact.dropTable();
                if (fact.tableExists()) { 
                    Print.logError("Table still exists: " + reloadTable);
                    // table is still intact
                    return DBAdminExec.WARN;
                }
            } catch (DBException dbe) {
                Print.logError("Error dropping table: " + reloadTable);
                dbe.printException();
                // table is hopefully still intact
                return DBAdminExec.ERROR;
            }
            // create
            Print.logInfo("  Creating ...");
            try {
                fact.createTable();
            } catch (DBException dbe) {
                Print.logException("Error creating table: " + reloadTable, dbe);
                // table data is lost!
                return DBAdminExec.ERROR;
            }
            // load
            Print.logInfo("  Loading ...");
            try {
                long count = fact.loadTable(reloadFile, false);
                //Print.logInfo("(Loaded " + count + " records from file '" + reloadFile + "' into table '" + reloadTable + "')");
            } catch (DBException dbe) {
                Print.logException("Error loading table: " + reloadTable, dbe);
                // table data is lost!
                return DBAdminExec.ERROR;
            }
            // validate
            Print.logInfo("  Validating ...");
            fact.validateColumns(); // display warnings
            // list table record count
            long endingRecordCount = 0L;
            try {
                endingRecordCount = DBRecord.getRecordCount(fact,"");
                Print.logInfo("  Ending record count = " + endingRecordCount);
            } catch (Throwable th) { // SQLException, DBException
                Print.logException("Error retrieving table record count: " + reloadTable, th);
                return DBAdminExec.ERROR;
            }
            // check against starting record count
            if (startingRecordCount != endingRecordCount) {
                Print.logError("Old record count ["+startingRecordCount+"] does not match new record count");
            }
            // return
            return DBAdminExec.EXIT; // go no further
        }
        */

        /* pre-check 'load' file */
        // bin/exe DBAdmin -load=<Table> -dir=<Source_Dir> -overwrite
        File   loadTableFiles[] = null;
        String loadTableNames[] = null;
        if (RTConfig.hasProperty(ARG_LOAD)) {
            execCmd++;
            String loadTableArg = RTConfig.getString(ARG_LOAD, null);
            loadTableNames = StringTools.split(loadTableArg, ',');
            if (StringTools.isBlank(loadTableArg) || ListTools.isEmpty(loadTableNames)) {
                Print.logError("'Load' file(s) not specified");
                return DBAdminExec.ERROR;
            }
            loadTableFiles = new File[loadTableNames.length];
            for (int t = 0; t < loadTableNames.length; t++) {
                if (FileTools.hasFileSeparator(loadTableNames[t])) {
                    // IE. "-load=./Geozone[.csv]"
                    if (loadTableNames.length > 1) {
                        Print.logError("Multiple load files may not contain directory specifications.");
                        return DBAdminExec.ERROR;
                    }
                    File file = new File(loadTableNames[t]);
                    loadTableNames[t] = file.getName();
                    loadDir = file.getParentFile(); // override any directory specified with '-dir=<dir>'
                }
                int p = loadTableNames[t].lastIndexOf('.');
                if (p < 0) {
                    // IE. "Geozone"
                    loadTableFiles[t] = new File(loadDir, loadTableNames[t] + DBFactory._LOAD_EXT_TXT);
                    if (!loadTableFiles[t].isFile()) { 
                        loadTableFiles[t] = new File(loadDir, loadTableNames[t] + DBFactory._LOAD_EXT_SQL);
                        if (!loadTableFiles[t].isFile()) {
                            String f = loadDir + File.separator + loadTableNames[t] + ".[txt|sql]";
                            Print.logWarn("'Load' file not found: " + f);
                            return DBAdminExec.WARN;
                        }
                    }
                } else {
                    // IE. "Geozone.csv"
                    String ext = loadTableNames[t].substring(p); // ==> ".csv"
                    if (ext.equals(DBFactory._LOAD_EXT_CSV) || 
                        ext.equals(DBFactory._LOAD_EXT_TXT) || 
                        ext.equals(DBFactory._LOAD_EXT_SQL)   ) {
                        loadTableFiles[t] = new File(loadDir, loadTableNames[t]);
                        if (!loadTableFiles[t].isFile()) {
                            Print.logWarn("'Load' file not found: " + loadTableFiles[t]);
                            return DBAdminExec.WARN;
                        }
                    } else {
                        Print.logError("Invalid 'load' file extension [expected txt|sql|csv]");
                        return DBAdminExec.ERROR;
                    }
                    loadTableNames[t] = loadTableNames[t].substring(0,p);
                }
            }
        }
        // loadTableNames, loadTableFiles, [loadDir] now defined ...

        /* dump: dump table to flatfile */
        // bin/exe DBAdmin -dump=<Table> -dir=<Destination_Dir>
        String dumpTable = RTConfig.getString(ARG_DUMP,null);
        if (dumpTable != null) {
            execCmd++;
            String dftExt = DBFactory._DUMP_EXT_TXT;
            // tables to dump
            java.util.List<String> tableList = new Vector<String>();
            if (dumpTable.equalsIgnoreCase(DUMP_ALL)) {
                // -dump=all
                ListTools.toList(DBAdmin.getTableFactoryMap().keySet(), tableList);
                dftExt = DBFactory._DUMP_EXT_TXT;
            } else
            if (dumpTable.equalsIgnoreCase(DUMP_ALL + DBFactory._DUMP_EXT_CSV)) {
                // -dump=all.csv
                ListTools.toList(DBAdmin.getTableFactoryMap().keySet(), tableList);
                dftExt = DBFactory._DUMP_EXT_CSV;
            } else
            if (dumpTable.equalsIgnoreCase(DUMP_ALL + DBFactory._DUMP_EXT_TXT)) {
                // -dump=all.txt
                ListTools.toList(DBAdmin.getTableFactoryMap().keySet(), tableList);
                dftExt = DBFactory._DUMP_EXT_TXT;
            } else
            if (dumpTable.equalsIgnoreCase(DUMP_ALL + DBFactory._DUMP_EXT_XML)) {
                // -dump=all.xml
                ListTools.toList(DBAdmin.getTableFactoryMap().keySet(), tableList);
                dftExt = DBFactory._DUMP_EXT_XML;
            } else {
                // -dump=<table>[,<table>[,<table>]]
                ListTools.toList(StringTools.split(dumpTable,','), tableList);
                dftExt = DBFactory._DUMP_EXT_TXT;
            }
            // dump tables
            for (Iterator<String> i = tableList.iterator(); i.hasNext(); ) {
                DBFactory dbfact = null;
                // extract table name
                String utableName = i.next(); // untranslated
                File toDir = dumpDir;
                if (FileTools.hasFileSeparator(utableName)) {
                    // IE. "-dump=./Geozone[.csv]"
                    File file = new File(utableName);
                    utableName = file.getName();
                    toDir = file.getParentFile(); // override any directory specified with '-dir=<dir>'
                }
                // create the dump file name
                File dumpFile = null;
                int p = utableName.lastIndexOf('.');
                if (p < 0) {
                    // IE. "Geozone"
                    utableName = DBAdmin.getTableNameProperCase(utableName);
                    dumpFile = new File(toDir, utableName + dftExt);
                } else {
                    // IE. "Geozone.csv"
                    String dotExt = utableName.substring(p);
                    utableName = DBAdmin.getTableNameProperCase(utableName.substring(0,p)); 
                    if (dotExt.equals(DBFactory._DUMP_EXT_CSV) || 
                        dotExt.equals(DBFactory._DUMP_EXT_TXT) || 
                        dotExt.equals(DBFactory._DUMP_EXT_XML)   ) {
                        dumpFile = new File(toDir, utableName + dotExt);
                    } else {
                        Print.logError("Invalid 'dump' file extension [expected txt|csv|xml]: " + dotExt);
                        return DBAdminExec.ERROR;
                    }
                }
                // 'where' clause
                String where = null;
                if (RTConfig.hasProperty(ARG_WHERE)) {
                    where = RTConfig.getString(ARG_WHERE,"");
                    Print.logInfo("Dumping '" + utableName + "' to file: " + dumpFile + " => " + where);
                } else {
                    where = null; // all 
                    Print.logInfo("Dumping '" + utableName + "' to file: " + dumpFile);
                }
                // DBFactory for table
                DBFactory<? extends DBRecord> fact = DBAdmin.getTableFactory(utableName);
                if (fact != null) {
                    try {
                        DBSelect dbSel = new DBSelect(fact,where); // "unchecked call"
                        fact.dumpTable(dumpFile, dbSel); // "unchecked conversion"
                    } catch (DBException dbe) {
                        Print.logException("Error dumping table: " + utableName, dbe);
                        return DBAdminExec.ERROR;
                    }
                } else {
                    Print.logError("No DBFactory for table: " + utableName);
                    return DBAdminExec.ERROR;
                }
            }
        }

        /* drop: drop tables */
        // bin/exe DBAdmin -drop=<Table>
        if (RTConfig.hasProperty(ARG_DROP)) {
            execCmd++;
            String dropTbl = RTConfig.getString(ARG_DROP,null);
            if (!StringTools.isBlank(dropTbl)) {
                Print.logInfo("Deleting table: " + dropTbl);
                DBFactory dbf = DBAdmin.getTableFactory(dropTbl);
                if (dbf != null) {
                    try {
                        dbf.dropTable();
                        Print.logInfo("Table dropped: " + dropTbl);
                    } catch (DBException dbe) {
                        Print.logError("Unable to drop table");
                        dbe.printException();
                    }
                } else {
                    Print.logError("No DBFactory for table: " + dropTbl);
                }
            } else {
                Print.logError("Missing table name");
            }
        }

        /* load: load table data from flatfile */
        // bin/exe DBAdmin -dir=<Source_Dir> -load=<Table>
        if (!ListTools.isEmpty(loadTableFiles)) {
            execCmd++;
            for (int t = 0; t < loadTableFiles.length; t++) {
                if (loadTableFiles[t].isFile()) {
                    DBFactory fact = DBAdmin.getTableFactory(loadTableNames[t]);
                    if (fact != null) {
                        boolean overwriteExisting = RTConfig.getBoolean(ARG_OVERWRITE,false);
                        try {
                            Print.logInfo("-----------------------------------");
                            if (!fact.tableExists()) { 
                                Print.logInfo("Creating table '" + loadTableNames[t] + "'");
                                fact.createTable();
                            }
                            Print.logInfo("Loading table '" + loadTableNames[t] + "' from file: " + loadTableFiles[t]);
                            if (overwriteExisting) {
                                Print.logWarn("**** Existing data WILL be overwritten! ****");
                            } else {
                                Print.logWarn("---- Existing data will NOT be overwritten! ----");
                            }
                            long count = fact.loadTable(loadTableFiles[t], overwriteExisting);
                            Print.logInfo("(Loaded " + count + " records from file '" + loadTableFiles[t] + "' into table '" + loadTableNames[t] + "')");
                        } catch (DBException dbe) {
                            Print.logException("Error creating/loading table: " + loadTableNames[t], dbe);
                            return DBAdminExec.ERROR;
                        }
                    } else {
                        Print.logError("No DBFactory for table '" + loadTableNames[t] + "'");
                        return DBAdminExec.ERROR;
                    }
                } else {
                    Print.logError("File not found: " + loadDir + File.separator + loadTableNames[t] + ".[dump|sql|txt|csv]");
                    return DBAdminExec.ERROR;
                }
            }
        }

        /* tables: clear/create tables */
        // bin/exe DBAdmin -tables=[opt]  ["tcaws"] (Table, Column, Warn)
        //RTConfig.getCommandLineProperties().printProperties("DBAdmin: check for '-tables'");
        if (RTConfig.hasProperty(ARG_TABLES)) {
            execCmd++;
            String utableName = RTConfig.getString(ARG_TABLENAME,null);
            String options = RTConfig.getString(ARG_TABLES,"").trim().toLowerCase();
            if (StringTools.isBlank(options)) { options = "t"; }
            int mask = VALIDATE_DISPLAY_ERRORS;
            if (options.indexOf("t") >= 0) { mask |= VALIDATE_CREATE_TABLE;     }
            if (options.indexOf("c") >= 0) { mask |= VALIDATE_ADD_COLUMNS | VALIDATE_CREATE_TABLE; }
            if (options.indexOf("a") >= 0) { mask |= VALIDATE_ALTER_COLUMNS;    }
            if (options.indexOf("k") >= 0) { mask |= VALIDATE_REBUILD_KEYS;     }
            if (options.indexOf("u") >= 0) { mask |= VALIDATE_CHECK_ENCODING;   }
            if (options.indexOf("e") >= 0) { mask |= VALIDATE_DISPLAY_ERRORS;   }
            if (options.indexOf("w") >= 0) { mask |= VALIDATE_DISPLAY_WARNINGS; }
            if (options.indexOf("s") >= 0) { mask |= VALIDATE_SHOW_COLUMNS;     }
            if (options.indexOf("n") >= 0) { mask |= VALIDATE_NAMED_TABLE_ONLY; }
            boolean validateOK = DBAdmin.validateTableColumns(mask, utableName);
            if (!validateOK) {
                return DBAdminExec.ERROR;
            }
        }

        /* validate: validate tables */
        // bin/exe DBAdmin -validate=[/]<table>
        if (RTConfig.hasProperty(ARG_VALIDATE)) {
            execCmd++;
            String validateTable = RTConfig.getString(ARG_VALIDATE,null);
            if (validateTable != null) {
                boolean inclWarn = true;
                if (validateTable.startsWith("/")) {
                    inclWarn = false;
                    validateTable = validateTable.substring(1);
                }
                Print.logInfo("Validating tables");
                DBAdmin.validateTables(validateTable, inclWarn);
            }
        }
 
        /* last: show last update time */
        // bin/exe DBAdmin -last
        if (RTConfig.hasProperty(ARG_LAST)) {
            execCmd++;
            //String last = RTConfig.getString(ARG_LAST, null);
            Print.logInfo("Table last update time:");
            OrderedMap<String,DBFactory<? extends DBRecord>> factMap = DBAdmin.getTableFactoryMap();
            for (Iterator<String> i = factMap.keyIterator(); i.hasNext();) {
                String tn = i.next();
                DBFactory<? extends DBRecord> f = (DBFactory<? extends DBRecord>)factMap.get(tn);
                try {
                    long lut = DBRecord.getLastUpdateTime(f);
                    if (lut < 0L) {
                        Print.logInfo("  Last Table Update: " +tn + " - Not Available");
                    } else
                    if (lut == 0L) {
                        Print.logInfo("  Last Table Update: " + tn + " - No Data");
                    } else {
                        Print.logInfo("  Last Table Update: " + tn + " - " + (new DateTime(lut)));
                        //Print.logInfo("   => " + DBRecord.getRecordsSince(f, lut)[0]);
                    }
                } catch (DBException dbe) {
                    Print.logError("  Last Table Update: " +tn + " - DB Error [" + dbe + "]");
                }
            }
        }

        /* schema: print table schema */
        // bin/exe DBAdmin -schema[=<table>]
        if (RTConfig.hasProperty(ARG_SCHEMA)) {
            execCmd++;
            String schemaTable = RTConfig.getString(ARG_SCHEMA,null);
            DBAdmin.printTableSchema(95, null, schemaTable);
            //return DBAdminExec.EXIT; // go no further
        }

        /* bean: validate bean access methods for specified table */
        // bin/exe DBAdmin -bean=table
        if (RTConfig.hasProperty(ARG_BEAN)) {
            execCmd++;
            String table = RTConfig.getString(ARG_BEAN,null);
            if (table != null) {
                OrderedMap<String,DBFactory<? extends DBRecord>> factMap = DBAdmin.getTableFactoryMap();
                DBFactory<? extends DBRecord> f = DBAdmin._getTableFactory(table);
                if (f != null) {
                    f.validateTableBeanMethods();
                } else {
                    Print.logError("Table not found: " + table);
                }
            }
        }
        
        /* hibxml: print Hibernate XML for specified table */
        // bin/exe DBAdmin -hibxml=table
        // [This option is currently experimental]
        /*
        if (RTConfig.hasProperty(ARG_HIBXML)) {
            execCmd++;
            String table = RTConfig.getString(ARG_HIBXML, null);
            if (table != null) {
                OrderedMap<String,DBFactory<? extends DBRecord>> factMap = DBAdmin.getTableFactoryMap();
                DBFactory<? extends DBRecord> f = DBAdmin._getTableFactory(table);
                if (f != null) {
                    f.createHibernateXML();
                } else {
                    Print.logError("Table not found: " + table);
                }
            }
        }
        */
        
        return (execCmd > 0)? DBAdminExec.OK : DBAdminExec.NONE;

    }

    /**
    *** Main entry point
    *** @param argv  The command-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        DBAdminExec rtn = DBAdmin.execCommands();
        if (rtn.equals(DBAdminExec.ERROR)) {
            System.exit(2);
        } else
        if (rtn.equals(DBAdminExec.WARN)) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------------

}
