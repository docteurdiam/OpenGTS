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
//  2008/01/10  Martin D. Flynn
//     -Initial release
//  2008/04/11  Martin D. Flynn
//     -Added "font-style:" methods
//  2009/10/02  Martin D. Flynn
//     -Added "setSortKey"/"getSortKey" methods
//  2011/06/16  Martin D. Flynn
//     -Added "getStyleString()" method
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.db.*;
import org.opengts.util.*;

public class ColumnValue
{

    // ------------------------------------------------------------------------

    public static final String LINK_TARGET_NONE             = "";
    public static final String LINK_TARGET_BLANK            = "_blank";
    public static final String LINK_TARGET_TOP              = "_top";
    public static final String LINK_TARGET_DEFAULT          = LINK_TARGET_NONE;

    public static final String TEXT_DECORATION_UNDERLINE    = "underline";
    public static final String TEXT_DECORATION_OVERLINE     = "overline";
    public static final String TEXT_DECORATION_BLINK        = "blink";
   
    public static final String FONT_WEIGHT_BOLD             = "bold";
    
    public static final String FONT_STYLE_ITALIC            = "italic";

    // ------------------------------------------------------------------------
    
    /* value */
    private String value            = null;
    private String sortKey          = null;

    /* css class */
    private String cssClass         = null;
    
    /* style */
    private String foreground       = null;
    private String background       = null;
    private String fontWeight       = null;
    private String fontStyle        = null;
    private String textDecoration   = null;

    /* image */
    private String imageURL         = null;

    /* link */
    private String linkURL          = null;
    private String linkTarget       = null;     // "_top", "_blank", ...
   
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public ColumnValue()
    {
        this.clear();
    }

    /**
    *** Copy Constructor
    *** @param value  The report column String value
    **/
    public ColumnValue(ColumnValue other)
    {
        this();
        if (other != null) {
            this.value          = other.value;
            this.sortKey        = other.sortKey;
            this.cssClass       = other.cssClass;
            this.foreground     = other.foreground;
            this.background     = other.background;
            this.fontWeight     = other.fontWeight;
            this.fontStyle      = other.fontStyle;
            this.textDecoration = other.textDecoration;
            this.imageURL       = other.imageURL;
            this.linkURL        = other.linkURL;
            this.linkTarget     = other.linkTarget;
        }
    }

    /**
    *** Constructor
    *** @param value  The report column String value
    **/
    public ColumnValue(String value)
    {
        this();
        this.setValue(value);
    }

    /**
    *** Constructor
    *** @param url  The report column String value
    **/
    public ColumnValue(URIArg url)
    {
        this();
        this.setImageURL(url);
    }

    /**
    *** Constructor
    *** @param value  The report column String value
    **/
    public ColumnValue(long value)
    {
        this(String.valueOf(value));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this column has a specific define CSS class, false otherwise
    *** @return True if this column has a specific define CSS class
    **/
    public boolean hasCssClass()
    {
        return !StringTools.isBlank(this.cssClass);
    }
    
    /**
    *** Sets the CSS class for this ColumnValue
    *** @param cssClass  The css class
    **/
    public ColumnValue setCssClass(String cssClass)
    {
        this.cssClass = StringTools.trim(cssClass);
        return this;
    }

    /**
    *** Gets the CSS class for this ColumnValue
    *** @return   The css class, or null/blank if no CSS class defined
    **/
    public String getCssClass()
    {
        return this.cssClass;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Clears all state
    **/
    public void clear()
    {
        this.value          = null;
        this.cssClass       = null;
        this.foreground     = null;
        this.background     = null;
        this.fontWeight     = null;
        this.fontStyle      = null;
        this.textDecoration = null;
        this.imageURL       = null;
        this.linkURL        = null;
        this.linkTarget     = null;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the current value
    *** @param val  The column String value
    *** @return This ColumnValue instance
    **/
    public ColumnValue setValue(String val)
    {
        this.value = val;
        return this;
    }
    
    /**
    *** Gets the current value
    *** @return This current column value as a String
    **/
    public String getValue()
    {
        return (this.value != null)? this.value : "";
    }

    /**
    *** Gets the current value
    *** @return This current column value as a String
    **/
    public String toString()
    {
        return this.getValue();
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the value that should be used for sorting.
    *** @param val  The column String value
    *** @return This ColumnValue instance
    **/
    public ColumnValue setSortKey(String val)
    {
        this.sortKey = val;
        return this;
    }
    
    /**
    *** Sets the value that should be used for sorting.
    *** @param val  The column String value
    *** @return This ColumnValue instance
    **/
    public ColumnValue setSortKey(long val)
    {
        return this.setSortKey(String.valueOf(val));
    }

    /**
    *** Sets the value that should be used for sorting.
    *** @param val  The column String value
    *** @return This ColumnValue instance
    **/
    public ColumnValue setSortKey(int val)
    {
        return this.setSortKey(String.valueOf(val));
    }

    /**
    *** Returns true if a sort key has been set for this column value
    *** @return True if a sort key has been set for this column value
    **/
    public boolean hasSortKey()
    {
        return (this.sortKey != null);
    }

    /**
    *** Gets the value that should be used for sorting.
    *** Returns null if value returned by 'getValue()' should be used for sorting
    *** @return The sort key, or null if no special sort key is required
    **/
    public String getSortKey()
    {
        return this.sortKey;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this ColumnValue contains style information
    *** @return True if this ColumnValue contains style information
    **/
    public boolean hasStyle()
    {
        return
            this.hasForegroundColor() ||
            this.hasBackgroundColor() ||
            this.hasTextDecoration()  ||
            this.hasFontWeight()      ||
            this.hasFontStyle();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "style" for this custom value.<br>
    *** Note: the prefixing <code>style=</code> and quotes are not included.
    *** @return The "style" for this custom value
    **/
    public String getStyleString()
    {

        /* return blank if no style */
        if (!this.hasStyle()) {
            return "";
        }

        /* create style */
        StringBuffer sb = new StringBuffer();
        if (this.hasForegroundColor()) {
            sb.append("color:").append(this.getForegroundColor()).append(";");
        }
        if (this.hasBackgroundColor()) {
            sb.append("background-color:").append(this.getBackgroundColor()).append(";");
        }
        if (this.hasTextDecoration()) {
            sb.append("text-decoration:").append(this.getTextDecoration()).append(";");
        }
        if (this.hasFontStyle()) {
            sb.append("font-style:").append(this.getFontStyle()).append(";");
        }
        if (this.hasFontWeight()) {
            sb.append("font-weight:").append(this.getFontWeight()).append(";");
        }

        /* return style */
        return sb.toString();

    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the text foregound color
    *** @param rgb  The ColorTools.RGB value of the text foreground color
    *** @return This ColumnValue instance
    **/
    public ColumnValue setForegroundColor(ColorTools.RGB rgb)
    {
        if (rgb != null) {
            this.setForegroundColor(rgb.toString(true));
        } else {
            this.setForegroundColor((String)null);
        }
        return this;
    }
    
    /**
    *** Sets the text foregound color
    *** @param color  The String representation of the text foreground color
    *** @return This ColumnValue instance
    **/
    public ColumnValue setForegroundColor(String color)
    {
        this.foreground = color;
        //if (!StringTools.isBlank(this.foreground) && !this.foreground.startsWith("#")) {
        //    // TODO: check to see if we need to prepend a "#"
        //}
        return this;
    }

    /**
    *** Returns true if the foreground color attribute has been defined
    *** @return True if the foreground color attribute has been defined
    **/
    public boolean hasForegroundColor()
    {
        return !StringTools.isBlank(this.foreground);
    }
    
    /**
    *** Gets the text foreground color
    *** @return The text foreground color
    **/
    public String getForegroundColor()
    {
        return this.hasForegroundColor()? this.foreground : null;
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Sets the text background color
    *** @param color  The String representation of the text background color
    *** @return This ColumnValue instance
    **/
    public ColumnValue setBackgroundColor(String color)
    {
        this.background = color;
        return this;
    }

    /**
    *** Returns true if the background color attribute has been defined
    *** @return True if the background color attribute has been defined
    **/
    public boolean hasBackgroundColor()
    {
        return ((this.background != null) && !this.background.equals(""));
    }

    /**
    *** Gets the text background color
    *** @return The text background color
    **/
    public String getBackgroundColor()
    {
        return this.hasBackgroundColor()? this.background : null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the text decoration style ("text-decoration:")
    *** @param decor text decoraton style (ie. "underline", "overline", "line-through", "blink")
    *** @return This ColumnValue instance
    **/
    public ColumnValue setTextDecoration(String decor)
    {
        this.textDecoration = decor;
        return this;
    }
    
    /**
    *** Sets the text decoration style to "underline"
    *** @return This ColumnValue instance
    **/
    public ColumnValue setTextDecorationUnderline()
    {
        return this.setTextDecoration(TEXT_DECORATION_UNDERLINE);
    }
    
    /**
    *** Sets the text decoration style to "overline"
    *** @return This ColumnValue instance
    **/
    public ColumnValue setTextDecorationOverline()
    {
        return this.setTextDecoration(TEXT_DECORATION_OVERLINE);
    }
    
    /**
    *** Sets the text decoration style to "blink"
    *** @return This ColumnValue instance
    **/
    public ColumnValue setTextDecorationBlink()
    {
        return this.setTextDecoration(TEXT_DECORATION_BLINK);
    }

    /**
    *** Returns true if the text decoration attribute has been defined
    *** @return True if the text decoration attribute has been defined
    **/
    public boolean hasTextDecoration()
    {
        return ((this.textDecoration != null) && !this.textDecoration.equals(""));
    }
    
    /**
    *** Gets the text decoration
    *** @return The text decoration
    **/
    public String getTextDecoration()
    {
        return this.hasTextDecoration()? this.textDecoration : null;
    }

    // ------------------------------------------------------------------------
 
    /**
    *** Sets the text font weight ("font-weight:")
    *** @param fw The text font weight (ie. "bold", "normal", "100".."900")
    *** @return This ColumnValue instance
    **/
    public ColumnValue setFontWeight(String fw)
    {
        this.fontWeight = fw;
        return this;
    }
    
    /**
    *** Sets the text font weight to "bold"
    *** @return This ColumnValue instance
    **/
    public ColumnValue setFontWeightBold()
    {
        return this.setFontWeight(FONT_WEIGHT_BOLD);
    }

    /**
    *** Returns true if the text font weight attribute has been defined
    *** @return True if the text font weight attribute has been defined
    **/
    public boolean hasFontWeight()
    {
        return ((this.fontWeight != null) && !this.fontWeight.equals(""));
    }

    /**
    *** Gets the text font weight
    *** @return The text font weight
    **/
    public String getFontWeight()
    {
        return this.hasFontWeight()? this.fontWeight : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the text font style ("font-style:")
    *** @param fs The text font style (ie. "normal", "italic", "oblique")
    *** @return This ColumnValue instance
    **/
    public ColumnValue setFontStyle(String fs)
    {
        this.fontStyle = fs;
        return this;
    }
    
    /**
    *** Sets the text font style to "italic"
    *** @return This ColumnValue instance
    **/
    public ColumnValue setFontStyleItalic()
    {
        return this.setFontStyle(FONT_STYLE_ITALIC);
    }

    /**
    *** Returns true if the text font style attribute has been defined
    *** @return True if the text font style attribute has been defined
    **/
    public boolean hasFontStyle()
    {
        return ((this.fontStyle != null) && !this.fontStyle.equals(""));
    }

    /**
    *** Gets the text font style
    *** @return The text font style
    **/
    public String getFontStyle()
    {
        return this.hasFontStyle()? this.fontStyle : null;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the image URL
    *** @param url The image URL
    *** @return This ColumnValue instance
    **/
    public ColumnValue setImageURL(String url)
    {
        return this.setImageURL(url, null);
    }

    /**
    *** Sets the image URL
    *** @param url The image URL
    *** @return This ColumnValue instance
    **/
    public ColumnValue setImageURL(URIArg url)
    {
        return this.setImageURL(((url != null)? url.toString() : null), null);
    }

    /**
    *** Sets the image URL
    *** @param url   The image URL
    *** @param size  The image width/height
    *** @return This ColumnValue instance
    **/
    public ColumnValue setImageURL(String url, PixelDimension size)
    {
        this.imageURL  = url;
      //this.imageSize = size;
        return this;
    }

    /**
    *** Returns true if the image URL attribute has been defined
    *** @return True if the image URL attribute has been defined
    **/
    public boolean hasImageURL()
    {
        return !StringTools.isBlank(this.imageURL);
    }
    
    /**
    *** Gets the imageURL URL
    *** @return The imageURL URL
    **/
    public String getImageURL()
    {
        return this.hasImageURL()? this.imageURL : null;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the link URL
    *** @param url The link URL
    *** @return This ColumnValue instance
    **/
    public ColumnValue setLinkURL(String url)
    {
        return this.setLinkURL(url, null);
    }
    
    /**
    *** Sets the link URL
    *** @param url The link URL
    *** @return This ColumnValue instance
    **/
    public ColumnValue setLinkURL(URIArg url)
    {
        return this.setLinkURL(url, null);
    }

    /**
    *** Sets the link URL
    *** @param url The link URL
    *** @param target  The link target ("_blank", "_top")
    *** @return This ColumnValue instance
    **/
    public ColumnValue setLinkURL(String url, String target)
    {
        this.linkURL    = url;
        this.linkTarget = target;
        return this;
    }

    /**
    *** Sets the link URL
    *** @param url The link URL
    *** @param target  The link target ("_blank", "_top", "-self")
    *** @return This ColumnValue instance
    **/
    public ColumnValue setLinkURL(URIArg url, String target)
    {
        return this.setLinkURL(((url != null)? url.toString() : null), target);
    }

    /**
    *** Returns true if the link URL attribute has been defined
    *** @return True if the link URL attribute has been defined
    **/
    public boolean hasLinkURL()
    {
        return !StringTools.isBlank(this.linkURL);
    }
    
    /**
    *** Gets the link URL
    *** @return The link URL
    **/
    public String getLinkURL()
    {
        return this.hasLinkURL()? this.linkURL : null;
    }

    /**
    *** Returns true if the link target attribute has been defined
    *** @return True if the link target attribute has been defined
    **/
    public boolean hasLinkTarget()
    {
        return ((this.linkTarget != null) && !this.linkTarget.equals(""));
    }

    /**
    *** Gets the link target
    *** @return The link target
    **/
    public String getLinkTarget()
    {
        return this.hasLinkTarget()? this.linkTarget : LINK_TARGET_DEFAULT;
    }

    // ------------------------------------------------------------------------

}
