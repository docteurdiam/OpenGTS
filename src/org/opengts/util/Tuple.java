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
//  This class provides Tuples
// ----------------------------------------------------------------------------
// Change History:
//  2009/06/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** <code>Tuple</code> a wrapper class for Tuples
**/

public class Tuple
{

    // ------------------------------------------------------------------------

    /**
    *** A wrapper for a single item
    **/
    public static class Single<AA>
    {
        public AA  a = null;
        public Single(AA a) {
            this.a = a;
        }
        public String toString() {
            return (this.a != null)? this.a.toString() : "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** A wrapper for a pair of items
    **/
    public static class Pair<AA,BB>
    {
        public AA  a = null;
        public BB  b = null;
        public Pair(AA a, BB b) {
            this.a = a;
            this.b = b;
        }
        public String toString() {
            return (this.a != null)? this.a.toString() : "";
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** A wrapper for a triple of items
    **/
    public static class Triple<AA,BB,CC>
    {
        public AA  a = null;
        public BB  b = null;
        public CC  c = null;
        public Triple(AA a, BB b, CC c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
        public String toString() {
            return (this.a != null)? this.a.toString() : "";
        }
    }

    // ------------------------------------------------------------------------

}
