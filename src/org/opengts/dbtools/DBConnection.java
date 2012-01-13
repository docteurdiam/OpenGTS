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
//     -Integrate DBException
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/09/16  Martin D. Flynn
//     -Added "Row-by-Row" option on the 'createStatement' method per MySQL "feature"
//      work-around for large result-sets.
//      [see "http://forums.mysql.com/read.php?39,152636,153012#msg-153012"]
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.io.*;

import java.sql.*; // DriverManager
import javax.sql.DataSource;

import org.opengts.util.*;

/**
*** <code>DBConnection</code> provides connections to the SQL server
**/

public class DBConnection
{

    // ------------------------------------------------------------------------

    private static final String USER_NONE         = "none";
    
    // ------------------------------------------------------------------------
    // Debug mode: show all executed SQL

    private static boolean DefaultShowExecutedSQL = false;
    private static boolean ShowExecutedSQL        = DefaultShowExecutedSQL;
    private static int     ShowExecutedSQL_level  = 0;
    static {
        DefaultShowExecutedSQL = RTConfig.getBoolean(RTKey.DB_SHOW_SQL);
        setShowExecutedSQL(DefaultShowExecutedSQL);
    }

    /**
    *** Sets a flag indicating that all SQL statements should be displayed prior to execution
    *** @param showSQL  True to display SQL statements, false otherwise
    **/
    public static void setShowExecutedSQL(boolean showSQL)
    {
        ShowExecutedSQL = showSQL;
    }

    /**
    *** Returns true if SQL statements should be displayed
    *** @return True if SQL statements should be displayed
    **/
    public static boolean getShowExecutedSQL()
    {
        return ShowExecutedSQL;
    }

    /**
    *** Pushes displaying SQL statement on a stack
    **/
    public static void pushShowExecutedSQL()
    {
        ShowExecutedSQL_level++;
        setShowExecutedSQL(true);
    }

    /**
    *** Pops the display of SQL statements state
    **/
    public static void popShowExecutedSQL()
    {
        if (ShowExecutedSQL_level > 0L) {
            ShowExecutedSQL_level--;
        }
        if (ShowExecutedSQL_level == 0L) {
            setShowExecutedSQL(DefaultShowExecutedSQL);
        }
    }

    // ------------------------------------------------------------------------

    // Always use DriverManager
    // If true, DriverManager will always be used.
    // If false, DriverManager will only be used if DBConnection.DBDataSource is null.
    private static boolean      USE_DRIVER_MANAGER      = false;
    
    // Pool DBConnections (if false, each thread will get its own DBConnection)
    private static boolean      DBCONNECTION_POOL       = false;

    // 'true' will cause table locking problems
    // (see DBProvider.isTableLockingEnabled)
    public  static boolean      ALWAYS_NEW_CONNECTION   = false;

    /* this connection timeout must be less than what is configured in the database server */
    private static long         CONNECTION_TIMEOUT      = 6L * 3600L; // 6 hours

    // ------------------------------------------------------------------------

    private static String       LastSQLExecuted         = null;

    // ------------------------------------------------------------------------

    private static DataSource   DBDataSource            = null;

    static {

        /* DBConnection pooling? */
        DBCONNECTION_POOL = RTConfig.getBoolean(RTKey.DB_DBCONNECTION_POOL);
        if (DBCONNECTION_POOL) {
            Print.logDebug("DBConnection pooling enabled");
        } else {
            Print.logDebug("DBConnection per-thread enabled");
        }

        /* DataSource configuration */
        // Possible DataSource classes:
        //   org.apache.commons.dbcp.BasicDataSource
        //   org.apache.tomcat.dbcp.dbcp.BasicDataSource
        // References:
        //   http://commons.apache.org/dbcp/
        //   http://commons.apache.org/pool/
        //   http://svn.apache.org/viewvc/commons/proper/dbcp/trunk/doc/
        String dataSrcClass = RTConfig.getString(RTKey.DB_DATASOURCE_CLASS);
        if (!StringTools.isBlank(dataSrcClass)) {
            Print.logInfo("Attempting to load DataSource: " + dataSrcClass);

            /* get DataSource class */
            Class bdsClass = null;
            if (dataSrcClass.equalsIgnoreCase("true") || dataSrcClass.equalsIgnoreCase("default")) {
                String bdsClassName[] = new String[] {
                    "org.apache.tomcat.dbcp.dbcp.BasicDataSource",  // Tomcat (try first)
                    "org.apache.commons.dbcp.BasicDataSource",      // http://commons.apache.org/dbcp/
                };
                for (int i = 0; i < bdsClassName.length; i++) {
                    try {
                        bdsClass = Class.forName(bdsClassName[i]);
                        break; // found it
                    } catch (Throwable th) {
                        if (th.getMessage().indexOf("GenericObjectPool") >= 0) {
                            // only applicable to "org.apache.commons.dbcp.BasicDataSource"
                            Print.logException("Apache 'Pool' Component missing! (see http://commons.apache.org/pool/)", th);
                        } else
                        if (RTConfig.isDebugMode()) {
                            //Print.logException("Error loading '"+bdsClassName[i]+"'", th);
                            Print.logInfo("Error loading '"+bdsClassName[i]+"'");
                            Print.logInfo(th.toString());
                        }
                    }
                }
            } else {
                try {
                    bdsClass = Class.forName(dataSrcClass);
                } catch (Throwable th) {
                    bdsClass = null;
                }
            }

            /* init DataSource */
            if (bdsClass != null) {
                String driverName = DBProvider.getProvider().getDrivers()[0]; // first driver only
                String urlStr     = DBProvider.getDBUri(true);
                String user       = DBProvider.getDBUsername();
                String password   = DBProvider.getDBPassword();
                int    maxActive  = RTConfig.getInt( RTKey.DB_DATASOURCE_MAX_ACTIVE,   100);
                int    maxIdle    = RTConfig.getInt( RTKey.DB_DATASOURCE_MAX_IDLE  ,    30);
                long   maxWait    = RTConfig.getLong(RTKey.DB_DATASOURCE_MAX_WAIT  , 10000L);
                try {
                    DataSource ds = (DataSource)bdsClass.newInstance();
                    (new MethodAction(ds, "setDriverClassName", String.class )).invoke(driverName);
                    (new MethodAction(ds, "setUrl"            , String.class )).invoke(urlStr    );
                    (new MethodAction(ds, "setUsername"       , String.class )).invoke(user      );
                    (new MethodAction(ds, "setPassword"       , String.class )).invoke(password  );
                    (new MethodAction(ds, "setMaxActive"      , Integer.TYPE )).invoke(new Integer(maxActive));
                    (new MethodAction(ds, "setMaxIdle"        , Integer.TYPE )).invoke(new Integer(maxIdle));
                    (new MethodAction(ds, "setMaxWait"        , Long.TYPE    )).invoke(new Long(maxWait));
                    DBConnection.DBDataSource = ds;
                } catch (Throwable th) {
                    Print.logWarn("Unable to load DataSource: " + th);
                    DBConnection.DBDataSource = null;
                }
            } else {
                Print.logWarn("DataSource class not found: " + dataSrcClass);
            }

        }

        /* display message */
        if (DBConnection.DBDataSource != null) {
            Print.logInfo("Loaded DataSource: %s ...", StringTools.className(DBConnection.DBDataSource));
        } else {
            Print.logDebug("DriverManager will be used for new connections");
            Print.logDebug(RTKey.DB_PROVIDER + " = " + RTConfig.getString(RTKey.DB_PROVIDER));
            Print.logDebug(RTKey.DB_URL      + " = " + RTConfig.getString(RTKey.DB_URL     ));
            Print.logDebug(RTKey.DB_URL_DB   + " = " + RTConfig.getString(RTKey.DB_URL_DB  ));
            Print.logDebug("DBProvider.getDBUri  = " + DBProvider.getDBUri(true));
        }

    }

    // ------------------------------------------------------------------------
    // Thread-Safety: Currently, each thread gets a new dedicated connection map

    protected static Map<String,DBConnection>            dbConnectionMap  = null;
    protected static Map<String,ArrayList<DBConnection>> dbConnectionPool = null;
    protected static Collection<DBConnection>            dbConnectionList = null;

    /**
    *** Gets the named connection
    *** @param uri    The DBConnection name
    **/
    private static DBConnection _getDBConnection(String uri)
    {
        // "DBConnection.ConnectionPoolLock" lock required
        if (uri == null) {
            return null;
        } else
        if (DBCONNECTION_POOL) {
            // Pooled Connections
            if (dbConnectionPool != null) {
                ArrayList<DBConnection> dbcArray = dbConnectionPool.get(uri);
                if (dbcArray != null) {
                    for (int d = 0; d < dbcArray.size(); d++) {
                        DBConnection dbc = dbcArray.get(d);
                        if (dbc._getLockCount() <= 0) {
                            dbcArray.remove(0);
                            dbcArray.add(dbc);
                            return dbc;
                        }
                    }
                }
            }
            return null;
        } else {
            // ThreadLocal connections
            return (dbConnectionMap != null)? dbConnectionMap.get(uri) : null;
        }
    }

    /**
    *** Saves a named DBConnection
    *** @param dbc   The DBConnection
    **/
    protected static void _saveDBConnection(DBConnection dbc)
    {
        // "DBConnection.ConnectionPoolLock" lock required
        if (dbc != null) {
            String uri = dbc.getUri();
            if (DBCONNECTION_POOL) {
                // Pooled Connections
                if (dbConnectionPool == null) {
                    dbConnectionPool = new HashMap<String,ArrayList<DBConnection>>();
                }
                ArrayList<DBConnection> dbcArray = dbConnectionPool.get(uri);
                if (dbcArray == null) {
                    dbcArray = new ArrayList<DBConnection>();
                    dbConnectionPool.put(uri, dbcArray);
                }
                dbcArray.add(dbc);
            } else {
                // ThreadLocal connections
                if (dbConnectionMap == null) {
                    dbConnectionMap  = new ThreadLocalMap<String,DBConnection>();
                }
                dbConnectionMap.put(uri, dbc);
            }
            /* save list of DBConnections */
            if (dbConnectionList == null) {
                dbConnectionList = new Vector<DBConnection>();
            }
            dbConnectionList.add(dbc);
        }
    }

    /**
    *** Close all open DBConnections
    **/
    public static void closeAllConnections()
    {
        int closed = 0;
        if (dbConnectionList != null) {
            for (DBConnection dbc : dbConnectionList) {
                if ((dbc != null) && !dbc.isConnectionClosed()) {
                    dbc.closeConnection();
                    closed++;
                }
            }
        }
        if (closed > 0) {
            Print.logInfo("Closed all open DBConnections: " + closed);
        }
    }

    // ------------------------------------------------------------------------
    
    private static Object ConnectionPoolLock = new Object();

    /**
    *** Gets a DBConnection based on the specified URI 
    *** @param uri  The connection URI
    *** @param user The user name
    *** @param pass The password
    *** @return The returned DBConnection
    **/
    public static DBConnection getDBConnection(String uri, String user, String pass)
    {
        if (uri != null) {
            DBConnection dbc = null;
            String dbConnMsg = null;
            Throwable previousLock = null;
            synchronized (DBConnection.ConnectionPoolLock) {
                dbc = DBConnection._getDBConnection(uri);
                if (dbc == null) {
                    // new connection
                    String threadName = Thread.currentThread().getName();
                    dbc = new DBConnection(uri, user, pass);
                    dbc.threadName = StringTools.trim(threadName);
                    DBConnection._saveDBConnection(dbc);
                    dbConnMsg = "New Connection [" + threadName + "] " + uri;
                } else {
                    // reused connection
                    //if (DBCONNECTION_POOL) {
                    //    dbConnMsg = "Reusing Connection [" + dbc.threadName + "] " + dbc.getUri();
                    //}
                }
                previousLock = dbc._lock();
            }
            if (dbConnMsg != null) {
                Print.logDebug(dbConnMsg);
            }
            if (previousLock != null) {
                Print.logException("DBConnection connection not released!", previousLock);
            }
            return dbc;
        } else {
            return null;
        }
    }

    /**
    *** Gets the default DBConnection
    **/
    public static DBConnection getDefaultConnection()
    {
        String uri = DBProvider.getDBUri(true);
        String usr = DBProvider.getDBUsername();
        String pwd = DBProvider.getDBPassword();
        return DBConnection.getDBConnection(uri, usr, pwd);
    }
    
    public static boolean isLocked(DBConnection dbc)
    {
        if (dbc != null) {
            int LC = 0;
            synchronized (DBConnection.ConnectionPoolLock) {
                LC = dbc._getLockCount();
            }
            return (LC > 0);
        } else {
            return false;
        }
    }

    /**
    *** clear "In Use"
    **/
    public static void release(DBConnection dbc)
    {
        //Print.logInfo("Releasing DBConnection ...");
        if (dbc != null) {
            boolean alreadyReleased = false;
            synchronized (DBConnection.ConnectionPoolLock) {
                alreadyReleased = dbc._release();
            }
            //if (alreadyReleased) {
            //    Print.logStackTrace("DBConnection already released!");
            //}
        } else {
            //Print.logWarn("Specified DBConnection is null");
        }
    }

    /**
    *** clear "In Use"
    **/
    public static void release(DBConnection dbc, Statement stmt, ResultSet rs)
    {
        if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
        if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
        DBConnection.release(dbc);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String      threadName          = null;
    
    private String      dbUri               = null;
    
    private String      userName            = null;
    private String      password            = null;
    
    private Connection  dbConnection        = null;
    
    private long        connectOpenCount    = 0L;
    private long        connectCloseCount   = 0L;
    private long        connectTime         = 0L;
    private long        lastUseTime         = 0L;
    private int         lockCount           = 0;
    private Throwable   lastLockTrace       = null;

    /**
    *** Constructor
    *** @param uri  The connection URI
    *** @param user The user name
    *** @param pass The password
    **/
    protected DBConnection(String uri, String user, String pass)
    {
        this.dbUri    = uri;
        this.userName = user;
        this.password = pass;
    }

    // ------------------------------------------------------------------------

    /* finalize */
    protected void finalize()
    {
        //Print.logInfo("Finalizing DBConnection ...");
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the DBConnection lock count (should be either '0' or '1')
    **/
    private int _getLockCount()
    {
        // "DBConnection.ConnectionPoolLock" lock required
        return this.lockCount;
    }

    /**
    *** Set "In Use"
    **/
    private Throwable _lock()
    {
        // "DBConnection.ConnectionPoolLock" lock required
        if (this.lockCount++ > 0) {
            // already locked
            if (DBCONNECTION_POOL) {
                // connection pooling should not be sharing connections
                return this.lastLockTrace; // return previous "_lock()" location 
            } else {
                // connection may be shared in a per-thread model
                return null;
            }
        } else {
            // new lock
            this.lastLockTrace = new Throwable("First DBConnection Lock");
            this.lastLockTrace.fillInStackTrace();
            return null;
        }
    }

    /**
    *** clear "In Use"
    **/
    private boolean _release()
    {
        // "DBConnection.ConnectionPoolLock" lock required
        if (this.lockCount > 0) {
            // release lock
            this.lockCount--;
            if (this.lockCount == 0) {
                this.lastLockTrace = null;
            }
            return false;
        } else {
            // not previously locked
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the connection URI
    *** @return The connection URI
    **/
    public String getUri()
    {
        return (this.dbUri != null)? this.dbUri : DBProvider.getDBUri(true);
    }

    /**
    *** Gets the connection user
    *** @return The connection user
    **/
    public String getUser()
    {
        if (this.userName == null) {
            return DBProvider.getDBUsername();
        } else
        if (this.userName.equals(USER_NONE)) {
            return "";
        } else {
            return this.userName;
        }
    }

    /**
    *** Gets the connection user password
    *** @return The connection user password
    **/
    public String getPassword()
    {
        return (this.password != null)? this.password : DBProvider.getDBPassword();
    }

    // ------------------------------------------------------------------------

    private static String loadedDriverName = null;

    /** 
    *** Returns a database connection 
    *** @return A database connection
    *** @throws SQLException  If an SQL error occurs
    **/
    public Connection getConnection()
        throws SQLException
    {
        if (this.isConnectionClosed()) {

            /* make sure connection is closed */
            this.closeConnection();

            /* load driver if not already loaded */
            if (loadedDriverName == null) {
                loadedDriverName = DBProvider.loadJDBCDriver();
                if ((loadedDriverName != null) && RTConfig.isDebugMode()) {
                    Print.logDebug("Loaded JDBC driver '" + loadedDriverName + "'");
                }
            }
 
            /* get DB connection */
            // May throw SQLException due to
            //    com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException: Too many connections
            // See my.cnf: 
            //    max_connections=500
            //    max_user_connections=500
            String user = this.getUser();
            if (StringTools.isBlank(user)) {
                //Print.logInfo("Obtaining connection without a user/password ...");
                if (USE_DRIVER_MANAGER || (DBConnection.DBDataSource == null)) {
                    this.dbConnection = DriverManager.getConnection(this.getUri());
                } else {
                    this.dbConnection = DBConnection.DBDataSource.getConnection();
                }
            } else {
                String pass = this.getPassword();
                //Print.logInfo("User: " + user + " Password: " + pass);
                if (USE_DRIVER_MANAGER || (DBConnection.DBDataSource == null)) {
                    this.dbConnection = DriverManager.getConnection(this.getUri(), user, pass);
                } else {
                    //this.dbConnection = DBConnection.DBDataSource.getConnection(user, pass);
                    this.dbConnection = DBConnection.DBDataSource.getConnection();
                }
            }

            /* save connection time */
            this.connectTime = DateTime.getCurrentTimeSec();
            this.connectOpenCount++;
            if (RTConfig.getBoolean(RTKey.DB_SHOW_CONNECTIONS)) {
                Print.logInfo("New Connection: [" + this.threadName + "]" + 
                    " openCount=" + this.connectOpenCount + ", closeCount=" + this.connectCloseCount);
            } else {
                Print.logDebug("New Connection: [" + this.threadName + "]" + 
                    " openCount=" + this.connectOpenCount + ", closeCount=" + this.connectCloseCount);
            }

        }

        /* save last used time */
        this.lastUseTime = DateTime.getCurrentTimeSec();

        return this.dbConnection;
    }

    /** 
    *** Closes the current database connection
    **/
    public void closeConnection()
    {
        if (this.dbConnection != null) {
            try {
                if (!this.dbConnection.isClosed()) {
                    // try normal close
                    try { 
                        this.dbConnection.close(); 
                        this.connectCloseCount++;
                    } catch (SQLException sqe) {
                        Print.logError("Error closing DBConnection: " + sqe);
                    }
                }
            } catch (SQLException sqle) {
                // force close
                try { 
                    this.dbConnection.close(); 
                    this.connectCloseCount++;
                } catch (SQLException sqe) {
                    //
                }
            }
        }
    }

    /**
    *** Returns true if the current database connection is closed
    *** @return True if the current database connection is closed
    **/
    public boolean isConnectionClosed()
    {
        try {
            if (this.dbConnection == null) {
                return true;
            } else
            if (this.dbConnection.isClosed()) {
                return true;
            } else
            if (this.isConnectionTimeout()) {
                return true;
            } else {
                return ALWAYS_NEW_CONNECTION;
            }
        } catch (SQLException sqe) {
            return true;
        }
    }
    
    /**
    *** Returns true if the last time this connection was used is beyond the timeout interval
    *** @return True if the last time this connection was used is beyond the timeout interval
    **/
    public boolean isConnectionTimeout()
    {
        long nowTime = DateTime.getCurrentTimeSec();
        return ((nowTime - this.lastUseTime) > CONNECTION_TIMEOUT);
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Create a new Statement
    *** @return A new SQL Statement
    *** @throws SQLException  If an SQL error occurs
    **/
    public Statement createStatement()
        throws SQLException
    {
        return this.createStatement(false);
    }
    
    /**
    *** Create a new Statement
    *** @param rowByRow  True to create a new Statement in row-by-row mode
    *** @return A new SQL Statement
    *** @throws SQLException  If an SQL error occurs
    **/
    public Statement createStatement(boolean rowByRow)
        throws SQLException
    {
        Connection conn = this.getConnection();
        Statement stmt = null;
        if (rowByRow) {
            // see "http://forums.mysql.com/read.php?39,152636,153012#msg-153012"
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
        } else {
            stmt = conn.createStatement();
        }
        return stmt;
    }

    // ------------------------------------------------------------------------

    /**
    *** Execute the specified SQL statement
    *** @param sql  The String SQL statement to execute
    *** @return The returned executed statement
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public Statement execute(String sql)
        throws SQLException, DBException
    {
        return this.execute(sql, false);
    }
    
    /**
    *** Execute the specified SQL statement
    *** @param sql  The String SQL statement to execute
    *** @param rowByRow  True to create a new Statement in row-by-row mode
    *** @return The returned executed statement
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public Statement execute(String sql, boolean rowByRow)
        throws SQLException, DBException
    {
        try {
            if (ShowExecutedSQL) { 
                Print.logInfo("SQL(Show): " + sql); 
            }
            return this._execute(sql, rowByRow);
        } catch (SQLException sqe) { 
            // The most likely reason for an error here is a connection timeout on the MySQL server:
            //  v3.23.54 "Communication link failure: java.io.IOException"
            //  v4.0.18  "Communication link failure ... java.io.EOFException"
            String sqlMsg = sqe.getMessage();
            int errCode   = sqe.getErrorCode();
            if ((sqlMsg.indexOf("IOException" ) >= 0) || 
                (sqlMsg.indexOf("EOFException") >= 0)   ) {
                // close connection and retry with new connection
                Print.logWarn("SQL(CommunicationLinkError) close/retry ["+errCode+"]: "+sqlMsg);
                this.closeConnection();
                return this._execute(sql, rowByRow);
            } else
            if ((errCode == DBFactory.SQLERR_SYNTAX_ERROR)  ||
                (errCode == DBFactory.SQLERR_UNKNOWN_COLUMN)  ) {
                // print sql statement for syntax errors
                Print.logError("SQL(SyntaxError): " + sql);
                throw sqe;
            } else {
                if (sqlMsg.indexOf("Data truncation") >= 0) {
                    // com.mysql.jdbc.MysqlDataTruncation: Data truncation: Data truncated for column 'X' ...
                    // This can occur when a numeric field is set to an empty string (ie. ..., myNumeric='', ...)
                    Print.logError("SQL(DataTruncation): " + sql);
                }
                throw sqe;
            }
        }
    }

    /**
    *** Execute the specified SQL statement
    *** @param sql  The String SQL statement to execute
    *** @param rowByRow  True to create a new Statement in row-by-row mode
    *** @return The returned executed statement
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    protected Statement _execute(String sql, boolean rowByRow)
        throws SQLException, DBException
    {
        try {
            LastSQLExecuted = sql;
            Statement stmt = this.createStatement(rowByRow);
            stmt.execute(sql); // eg. "SELECT * FROM <table>"
            return stmt;
        } catch (SQLException sqe) {
            if (StringTools.className(sqe).equals("com.mysql.jdbc.CommunicationsException")) {
                // MySQL: can occur if the server is not running, or server can't be found
                throw new DBException("JDBC Error", sqe);
            } else {
                //Print.logException("Executing SQL: " + sql, sqe);
                throw sqe;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Execute the specified SQL query
    *** @param sql  The String SQL statement to execute
    *** @return The returned ResultSet
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public ResultSet executeQuery(String sql)
        throws SQLException, DBException
    {
        try {
            if (ShowExecutedSQL) { 
                Print.logInfo("SQL: " + sql); 
            }
            return this._executeQuery(sql); // may throw DBException
        } catch (SQLException sqe) { // "Communication link failure: java.io.IOException"
            String sqlMsg = sqe.getMessage();
            int errCode   = sqe.getErrorCode();
            if ((sqlMsg.indexOf("IOException" ) >= 0) || 
                (sqlMsg.indexOf("EOFException") >= 0)   ) {
                this.closeConnection();
                return this._executeQuery(sql); // may throw DBException
            } else {
                throw sqe;
            }
        }
    }

    /**
    *** Execute the specified SQL query
    *** @param sql  The String SQL statem
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    *** @return The returned ResultSet
    **/
    protected ResultSet _executeQuery(String sql)
        throws SQLException, DBException
    {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            LastSQLExecuted = sql;
            stmt = this.createStatement();
            rs = stmt.executeQuery(sql);
            return rs; // close 'stmt'?
        } catch (SQLException sqe) {
            if (StringTools.className(sqe).equals("com.mysql.jdbc.CommunicationsException")) {
                // MySQL: can occur if the server is not running, or server can't be found
                throw new DBException("JDBC Error", sqe);
            } else {
                throw sqe;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Execute the specified SQL update
    *** @param sql  The String SQL statement to execute
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public void executeUpdate(String sql)
        throws SQLException, DBException
    {
        this.executeUpdate(sql, false);
    }
    
    /**
    *** Execute the specified SQL update
    *** @param sql  The String SQL statement to execute
    *** @param rtnAutoIncrVal If auto-generated fields (ie. "auto_increment")
    ***        should be returned
    *** @return The generated auto increment value or -1
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public long executeUpdate(String sql, boolean rtnAutoIncrVal)
        throws SQLException, DBException
    {
        try {
            if (ShowExecutedSQL) { 
                Print.logInfo("SQL: " + sql); 
            }
            return this._executeUpdate(sql, rtnAutoIncrVal); // may throw DBException
        } catch (IOException ioe) { // EOFException
            try {
                this.closeConnection();
                return this._executeUpdate(sql, rtnAutoIncrVal); // may throw DBException
            } catch (IOException ioe2) {
                throw new DBException("JDBC IOException", ioe2);
            }
        } catch (SQLException sqe) { // "Communication link failure: java.io.IOException"
            String sqlMsg = sqe.getMessage();
            int errCode   = sqe.getErrorCode();
            if ((sqlMsg.indexOf("IOException" ) >= 0) || 
                (sqlMsg.indexOf("EOFException") >= 0)   ) {
                this.closeConnection();
                try {
                    return this._executeUpdate(sql, rtnAutoIncrVal); // may throw SQLException, DBException
                } catch (IOException ioe2) {
                    throw new DBException("JDBC IOException", ioe2);
                }
            } else {
                //Print.logError("SQL: " + sql);
                throw sqe;
            }
        }
    }

    /**
    *** Execute the specified SQL update
    *** @param sql  The String SQL statement to execute
    *** @param rtnAutoIncrVal If auto-generated fields (ie. "auto_increment")
    ***        should be returned
    *** @return The generated auto increment value or -1
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    private long _executeUpdate(String sql, boolean rtnAutoIncrVal)
        throws SQLException, IOException, DBException
    {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            LastSQLExecuted = sql;
            stmt = this.createStatement();
            if (rtnAutoIncrVal) {
                stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    Print.logError("Expected Auto-Increment value not found!");
                    return -1L;
                }
            } else {
                stmt.executeUpdate(sql); // known to throw IOException
                return -1L;
            }
        } catch (SQLException sqe) { // catch (com.mysql.jdbc.CommunicationsException ce)
            if (StringTools.className(sqe).equals("com.mysql.jdbc.CommunicationsException")) {
                // MySQL: can occur if the server is not running, or server can't be found
                throw new DBException("JDBC Error", sqe);
            } else {
                throw sqe;
            }
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** DEBUG: Main entry point used for testing/debugging
    *** @param argv  The command-line arguments
    **/
    public static void main(String argv[])
    {
        DBProvider.getDBUsername();
        DBProvider.getDBPassword();
        DBProvider.getDBUri(true);
    }
    
}
