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
//  2007/03/04  Martin D. Flynn
//     -Initial release
//  2007/03/25  Martin D. Flynn
//     -Changed return type to 'int' to allow use as a record selector.
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBRecordHandler</code> is the interface for a callback SQL record handler.
**/

public interface DBRecordHandler<RT extends DBRecord>
{
   
    // ------------------------------------------------------------------------

    public static final int DBRH_STOP = 0; // stop DBRecord selection loop
    public static final int DBRH_SKIP = 1; // skip this record and continue
    public static final int DBRH_SAVE = 2; // save this record and continue

    // ------------------------------------------------------------------------
    
    /**
    *** Callback handler for DBRecords retrieved from a database select
    *** @param rcd  The DBRecord
    *** @return  The implementation method should return 'DBRH_STOP' to stop
    ***          the DBRecord selection/processing loop, or 'DBRH_SKIP' to skip
    ***          the current record, or 'DBRH_SAVE' to save the current record
    ***          to be return by the select method in an array of DBRecord's.
    **/
    public int handleDBRecord(RT rcd)
        throws DBException;
    
}
