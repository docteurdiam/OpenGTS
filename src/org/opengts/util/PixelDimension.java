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
//  2008/08/08  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

import org.opengts.util.*;

/**
*** A container for pixel width and height
**/

public class PixelDimension
    implements Cloneable
{

    // ------------------------------------------------------------------------

    private int width  = 0;
    private int height = 0;
    
    /**
    *** Constructor
    *** @param w The width of the pixel
    *** @param h The height of the pixel
    **/
    public PixelDimension(int w, int h)
    {
        this.setWidth( w);
        this.setHeight(h);
    }
    
    /**
    *** Copy constructor
    *** @param pd The PixelDimension to copy
    **/
    public PixelDimension(PixelDimension pd)
    {
        this.setWidth( (pd != null)? pd.getWidth()  : 0);
        this.setHeight((pd != null)? pd.getHeight() : 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a copy of this PixelDimension
    *** @return A copy of this PixelDimension object
    **/
    public Object clone()
    {
        return new PixelDimension(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the witdh of the pixel
    *** @param w The width of the pixel
    **/
    public void setWidth(int w)
    {
        this.width = w;
    }

    /**
    *** Gets the width of the pixel
    *** @return The width of the pixel
    **/
    public int getWidth()
    {
        return this.width;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the height of the pixel
    *** @param h The height of the pixel
    **/
    public void setHeight(int h)
    {
        this.height = h;
    }

    /**
    *** Gets the height of the pixel
    *** @return The height of the pixel
    **/
    public int getHeight()
    {
        return this.height;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns ture if the pixel dimensions are valid
    *** @return True if the pixel dimensions are valid
    **/
    public boolean isValid()
    {
        return (this.width > 0) && (this.height > 0);
    }

    // ------------------------------------------------------------------------

}
