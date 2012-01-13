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
//  This prints a list of System properties
// ----------------------------------------------------------------------------
// Change History:
//  2009/05/24  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.tools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

//import javax.mail.*;
//import javax.mail.internet.*;
//import javax.activation.*;

public class PrintProperties
{

    public static void main(String argv[])
    {
        Properties props = System.getProperties();
        for (Enumeration n = props.propertyNames(); n.hasMoreElements();) {
            String key = (String)n.nextElement();
            String val = props.getProperty(key);
            while (key.length() < 24) { key += " "; }
            System.out.println(key + " ==> " + val);
        }
    }

}
