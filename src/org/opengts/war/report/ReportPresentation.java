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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.io.*;

import org.opengts.war.tools.OutputProvider;

import org.opengts.util.EnumTools;
import org.opengts.db.ReportURL;

public interface ReportPresentation
{

    // ------------------------------------------------------------------------
    // Sortable table constants (used by 'sorttable.js')

    public static final String SORTTABLE_SORTKEY            = "sorttable_customkey";
    public static final String SORTTABLE_JS                 = "sorttable/sorttable.js";
    public static final String SORTTABLE_CSS_CLASS          = "sortable";
    public static final String SORTTABLE_CSS_NOSORT         = "nosort"; // MDF modified, was "sorttable_nosort";

    // ------------------------------------------------------------------------

    public static final int    INDENT                       = 3;

    // ------------------------------------------------------------------------

    public int writeReport(String format, ReportData rd, OutputProvider out, int indentLevel)
        throws ReportException;

    public int writeReport(ReportURL.Format format, ReportData rd, OutputProvider out, int indentLevel)
        throws ReportException;

    // ------------------------------------------------------------------------

}
