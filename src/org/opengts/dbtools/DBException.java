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
//  2006/04/08  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBException</code> is the general exception thrown for various encountered
*** SQL database errors.
**/

public class DBException
    extends Exception
{

    private static String CreateDBxceptionMessage(String msg, Throwable cause)
    {
        if (cause instanceof SQLException) {
            int errCode = ((SQLException)cause).getErrorCode();
            if (errCode > 0) {
                return msg + " [SQLErr=" + errCode + "]";
            }
        }
        return msg;
    }
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param msg  The message associated with this exception
    **/
    public DBException(String msg)
    {
        super(msg);
    }

    /**
    *** Constructor
    *** @param msg    The message associated with this exception
    *** @param cause  The reason for this exception
    **/
    public DBException(String msg, Throwable cause)
    {
        super(CreateDBxceptionMessage(msg,cause), cause);
    }
    
    // ----------------------------------------------------------------------------

    /**
    *** Returns true if the cause of this exception is an SQLException
    *** @return True if the cause of this exception is an SQLException
    **/
    public boolean isSQLException()
    {
        return (this.getCause() instanceof SQLException);
    }
    
    // ----------------------------------------------------------------------------

    /**
    *** Prints a description of this exception to the logging output
    **/
    public void printException()
    {
        Throwable cause = this.getCause();
        if (cause instanceof SQLException) {
            Print.logSQLError(1, this.getMessage(), (SQLException)cause);
        } else {
            Print.logException(this.getMessage(), this);
        }
    }
    
    // ----------------------------------------------------------------------------

    /**
    *** Returns a String representation of this exception
    *** @return A String representation of this exception
    **/
    public String toString()
    {
        Throwable cause = this.getCause();
        if (cause != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(super.toString());
            sb.append(" [").append(cause.toString()).append("]");
            return sb.toString();
        } else {
            return super.toString();
        }
    }
    
    // ----------------------------------------------------------------------------

    /*
    public void printStackTrace()
    {
        Throwable cause = this.getCause();
        if (cause instanceof SQLException) {
            Print.logStackTrace(cause);
        } else {
            super.printStackTrace();
        }
    }
    */
    
}
