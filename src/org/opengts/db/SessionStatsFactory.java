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
//  2007/01/10  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public interface SessionStatsFactory
{

    // ------------------------------------------------------------------------

    public  static long IP_OVERHEAD             =  20L; // per packet (included below)
    public  static long UDP_OVERHEAD            =   8L + IP_OVERHEAD; // 28 bytes per packet
    public  static long TCP_OVERHEAD            =  24L + IP_OVERHEAD; // 44 bytes per packet
    public  static long TCP_SESSION_OVERHEAD    = 240L; // per TCP session

    // ------------------------------------------------------------------------

    /* add session statistic */
    public void addSessionStatistic(Device device, long timestamp, 
        String ipAddr, boolean isDuplex,
        long bytesRead, long bytesWritten, long eventsRecv)
        throws DBException;
        
    /* return bytes read/written */
    public long[] getByteCounts(Device device, long timeStart, long timeEnd)
        throws DBException;

    /* return number of tcp/udp connections made */
    public long[] getConnectionCounts(Device device, long timeStart, long timeEnd) throws DBException;

    // ------------------------------------------------------------------------

}
