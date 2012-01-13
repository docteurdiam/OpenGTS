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
//  2011/07/15  Martin D. Flynn
//     -Initial release (cloned from AccumulatorLong)
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

/**
*** Accumulator Integer container.
*** Typically used in conditions where it is desireable to pass an accumulator
*** to an inner-class and have the value accessible from outside the inner-class.
*** (If the accumulator is to be used from different threads in a multi-threaded
*** environment, use "AtomicInteger" instead).
**/

public class AccumulatorInteger
{

    // ------------------------------------------------------------------------

    private int     accum   = 0;

    /**
    *** Constructor
    **/
    public AccumulatorInteger()
    {
        this(0);
    }

    /**
    *** Constructor
    *** @param val  Initial value
    **/
    public AccumulatorInteger(int val)
    {
        this.accum = val;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the value of the accumulator
    *** @param v  The new value
    **/
    public void set(int v)
    {
        this.accum = v;
    }

    /**
    *** Gets the value of the accumulator
    *** @return The current value
    **/
    public int get()
    {
        return this.accum;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified value to the accumulator
    *** @param v  The value to add
    **/
    public void add(int v)
    {
        this.accum += v;
    }

    /**
    *** Subtracts the specified value from the accumulator
    *** @param v  The value to subtract
    **/
    public void subtract(int v)
    {
        this.accum -= v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Increment the value of the accumulator by 1
    **/
    public void increment()
    {
        this.accum++;
    }

    /**
    *** Decrement the value of the accumulator by 1
    **/
    public void decrement(int v)
    {
        this.accum--;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Increment, and return the next value
    *** @return The next value
    **/
    public int next()
    {
        this.increment();
        return this.get();
    }

    /** 
    *** Add the specified value, and return the result
    *** @param v The value to add
    *** @return The next value
    **/
    public int next(int v)
    {
        this.add(v);
        return this.get();
    }

    // ------------------------------------------------------------------------

}

