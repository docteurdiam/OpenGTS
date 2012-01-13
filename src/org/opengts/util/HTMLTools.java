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
//  HTML Tools
// ----------------------------------------------------------------------------
// Change History:
//  2006/05/15  Martin D. Flynn
//     -Initial release
//  2008/02/27  Martin D. Flynn
//     -Added methods 'getMimeTypeFromExtension' and 'getMimeTypeFromData'
//  2008/03/28  Martin D. Flynn
//     -Added method 'inputStream_GET'.
//  2008/12/16  Martin D. Flynn
//     -Added 'timeoutMS' option to 'inputStream_GET' method
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

/**
*** Various HTML and HTTP utilities
**/

public class HTMLTools
{

    public  static final String PROP_HTMLTools_MIME_        = "HTMLTools.MIME.";
    
    // ------------------------------------------------------------------------
    // HTML tag constants

    public  static final String SP                          = StringTools.HTML_SP;
    public  static final String LT                          = StringTools.HTML_LT;
    public  static final String GT                          = StringTools.HTML_GT;
    public  static final String AMP                         = StringTools.HTML_AMP;
    public  static final String QUOTE                       = StringTools.HTML_QUOTE;
    public  static final String BR                          = StringTools.HTML_BR;
    public  static final String HR                          = StringTools.HTML_HR;

    public  static final String HTML                        = "<html>";

    public  static final String REQUEST_GET                 = "GET";
    public  static final String REQUEST_POST                = "POST";

    // ------------------------------------------------------------------------
    // Magic numbers:

    public  static final byte   MAGIC_GIF_87a[]             = new byte[]{(byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38,(byte)0x37,(byte)0x61}; // "GIF87a"
    public  static final byte   MAGIC_GIF_89a[]             = new byte[]{(byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38,(byte)0x39,(byte)0x61}; // "GIF89a"
    public  static final byte   MAGIC_JPEG[]                = new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF,(byte)0xE0};
    public  static final byte   MAGIC_PNG[]                 = new byte[]{(byte)0x89,(byte)0x50,(byte)0x4E,(byte)0x47,(byte)0x0D,(byte)0x0A,(byte)0x1A,(byte)0x0A};
    public  static final byte   MAGIC_TIFF_II[]             = new byte[]{(byte)0x49,(byte)0x49,(byte)0x2A,(byte)0x00};
    public  static final byte   MAGIC_TIFF_MM[]             = new byte[]{(byte)0x4D,(byte)0x4D,(byte)0x00,(byte)0x2A};
    public  static final byte   MAGIC_BMP[]                 = new byte[]{(byte)0x42,(byte)0x4D};
    public  static final byte   MAGIC_JAVA_CLASS[]          = new byte[]{(byte)0xCA,(byte)0xFE,(byte)0xBA,(byte)0xBE};

    // ------------------------------------------------------------------------

    public  static final String CHARSET_UTF8                = "charset=UTF-8";

    // ------------------------------------------------------------------------

    public  static final String HEADER_CONTENT_TYPE         = "Content-Type";
    public  static final String HEADER_CONTENT_DISPOSITION  = "Content-Disposition";
    public  static final String HEADER_CONTENT_LENGTH       = "Content-Length";
    public  static final String HEADER_USER_AGENT           = "User-Agent";
    public  static final String HEADER_REFERER              = "Referer";
    public  static final String HEADER_HOST                 = "Host";
    public  static final String HEADER_SOAPACTION           = "SOAPAction";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String PROP_User_Agent             = "User-Agent";

    public  static final String DEFAULT_USER_AGENT          = "HTMLTools/1.3";

    private static       String HTTPUserAgent               = null;

    /**
    *** Sets the HTTP "User-Agent" value to use for HTTP requests.
    *** @param userAgent  The user-agent
    **/
    public static void setHttpUserAgent(String userAgent)
    {
        HTTPUserAgent = userAgent;
    }

    /**
    *** Gets the HTTP "User-Agent" value to use for HTTP requests.
    *** @return  The user-agent
    **/
    public static String getHttpUserAgent()
    {
        if (!StringTools.isBlank(HTTPUserAgent)) {
            return HTTPUserAgent;
        } else {
            return RTConfig.getString(RTKey.HTTP_USER_AGENT, DEFAULT_USER_AGENT);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String CONTENT_FORM_MULTIPART   = "multipart/form-data";
    public static final String CONTENT_FORM_URLENCODED  = "application/x-www-form-urlencoded";

    /**
    *** Returns true if the content type is a multipart form
    *** @param ct     The content type
    *** @return True if "multipart/form-data"
    **/
    public static boolean isContentMultipartForm(String ct)
    {
        return (ct != null)? ct.toLowerCase().startsWith(CONTENT_FORM_MULTIPART) : false;
    }
    
    /**
    *** Gets the "boundary=" value
    *** @param ct     The content type
    *** @return The "boundary=" value
    **/
    public static String getContentMultipartBoundary(String ct)
    {
        int p = (ct != null)? ct.indexOf("boundary=") : -1;
        if (p < 0) {
            return null;
        } else {
            String b = ct.substring(p + 9);
            if (b.startsWith("\"")) {
                int q = b.lastIndexOf("\"");
                b = (q > 0)? b.substring(1,q) : b.substring(1);
            }
            return b;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String CONTENT_TYPE_PLAIN      = "text/plain";
    public  static final String CONTENT_TYPE_TEXT       = CONTENT_TYPE_PLAIN;
    public  static final String CONTENT_TYPE_XML        = "text/xml";
    public  static final String CONTENT_TYPE_HTML       = "text/html";
    public  static final String CONTENT_TYPE_GIF        = "image/gif";  // "GIF87a", "GIF89a"
    public  static final String CONTENT_TYPE_JPEG       = "image/jpeg"; // 0xFF,0xD8,0xFF,0xE0
    public  static final String CONTENT_TYPE_PNG        = "image/png";  // 0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A
    public  static final String CONTENT_TYPE_TIFF       = "image/tiff";
    public  static final String CONTENT_TYPE_BMP        = "image/bmp";  
    public  static final String CONTENT_TYPE_OCTET      = "application/octet-stream";
    public  static final String CONTENT_TYPE_BINARY     = "application/binary";
    public  static final String CONTENT_TYPE_DOC        = "application/msword";
    public  static final String CONTENT_TYPE_XLS        = "application/vnd.ms-excel";
    public  static final String CONTENT_TYPE_XLSX       = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public  static final String CONTENT_TYPE_CSV_OLD    = "text/comma-separated-values";
    public  static final String CONTENT_TYPE_CSV        = "text/csv";   // RFC-4180 [http://tools.ietf.org/html/rfc4180]
    public  static final String CONTENT_TYPE_PDF        = "application/pdf";
    public  static final String CONTENT_TYPE_PS         = "application/postscript";
    public  static final String CONTENT_TYPE_ZIP        = "application/x-zip-compressed";
    public  static final String CONTENT_TYPE_JAD        = "text/vnd.sun.j2me.app-descriptor";
    public  static final String CONTENT_TYPE_JAR        = "application/java-archive";
    public  static final String CONTENT_TYPE_KML_XML    = "application/vnd.google-earth.kml+xml kml";
    public  static final String CONTENT_TYPE_KML        = "application/vnd.google-earth.kml+xml";
    public  static final String CONTENT_TYPE_KMZ        = "application/vnd.google-earth.kmz";
    public  static final String CONTENT_TYPE_JSON       = "application/jsonrequest";

    /**
    *** Returns "zip" MIME type
    *** @return "zip" MIME type
    **/
    public static String MIME_ZIP()
    {
        return HTMLTools.getMimeType("zip", HTMLTools.CONTENT_TYPE_ZIP);
    }

    /**
    *** Returns "plain" MIME type
    *** @return "plain" MIME type
    **/
    public static String MIME_PLAIN()
    {
        return HTMLTools.getMimeType("plain", HTMLTools.CONTENT_TYPE_PLAIN);
    }

    /**
    *** Returns "html" MIME type
    *** @return "html" MIME type
    **/
    public static String MIME_HTML()
    {
        return HTMLTools.getMimeType("html", HTMLTools.CONTENT_TYPE_HTML);
    }

    /**
    *** Returns "xml" MIME type
    *** @return "xml" MIME type
    **/
    public static String MIME_XML()
    {
        return HTMLTools.getMimeType("xml", HTMLTools.CONTENT_TYPE_XML);
    }

    /**
    *** Returns "xml" MIME type, with added character set
    *** @return "xml" MIME type, with added character set
    **/
    public static String MIME_XML(String charSet)
    {
        return HTMLTools.getMimeTypeCharset(MIME_XML(), charSet);
    }

    /**
    *** Returns "csv" MIME type
    *** @return "csv" MIME type
    **/
    public static String MIME_CSV()
    {
        return HTMLTools.getMimeType("csv", HTMLTools.CONTENT_TYPE_CSV);
    }

    /**
    *** Returns "xls" MIME type
    *** @return "xls" MIME type
    **/
    public static String MIME_XLS()
    {
        return HTMLTools.getMimeType("xls", HTMLTools.CONTENT_TYPE_XLS);
    }

    /**
    *** Returns "xlsx" MIME type
    *** @return "xlsx" MIME type
    **/
    public static String MIME_XLSX()
    {
        return HTMLTools.getMimeType("xlsx", HTMLTools.CONTENT_TYPE_XLSX);
    }

    /**
    *** Returns "kml" MIME type
    *** @return "kml" MIME type
    **/
    public static String MIME_KML()
    {
        return HTMLTools.getMimeType("kml", HTMLTools.CONTENT_TYPE_KML);
    }

    /**
    *** Returns "json" MIME type
    *** @return "json" MIME type
    **/
    public static String MIME_JSON()
    {
        return HTMLTools.getMimeType("json", HTMLTools.CONTENT_TYPE_JSON);
    }

    /**
    *** Returns "binary" MIME type
    *** @return "binary" MIME type
    **/
    public static String MIME_BINARY()
    {
        return HTMLTools.getMimeType("binary", HTMLTools.CONTENT_TYPE_BINARY);
    }

    /**
    *** Returns "png" MIME type
    *** @return "png" MIME type
    **/
    public static String MIME_PNG()
    {
        return HTMLTools.getMimeType("png", HTMLTools.CONTENT_TYPE_PNG);
    }

    /**
    *** Returns "gif" MIME type
    *** @return "gif" MIME type
    **/
    public static String MIME_GIF()
    {
        return HTMLTools.getMimeType("gif", HTMLTools.CONTENT_TYPE_GIF);
    }

    /**
    *** Returns "jpeg" MIME type
    *** @return "jpeg" MIME type
    **/
    public static String MIME_JPEG()
    {
        return HTMLTools.getMimeType("jpeg", HTMLTools.CONTENT_TYPE_JPEG);
    }

    /**
    *** Returns "tiff" MIME type
    *** @return "tiff" MIME type
    **/
    public static String MIME_TIFF()
    {
        return HTMLTools.getMimeType("tiff", HTMLTools.CONTENT_TYPE_TIFF);
    }

    /**
    *** Returns "bmp" MIME type
    *** @return "bmp" MIME type
    **/
    public static String MIME_BMP()
    {
        return HTMLTools.getMimeType("bmp", HTMLTools.CONTENT_TYPE_BMP);
    }

    /**
    *** Returns the requested mime type
    *** @param name     The name of the MIME type property key
    *** @param dftType  The default MIME type to return
    *** @return The MIME type
    **/
    public static String getMimeType(String name, String dftType)
    {
        String mimeKey = PROP_HTMLTools_MIME_ + name;
        String mimeType = RTConfig.getString(mimeKey, null);
        if (!StringTools.isBlank(mimeType)) {
            return mimeType;
        } else
        if (!StringTools.isBlank(dftType)) {
            return dftType;
        } else {
            return HTMLTools.getMimeTypeFromExtension(name, dftType);
        }
    }
 
    /**
    *** Appends the default "charset" to mime type
    *** @param type     The Mime type
    *** @return The combined mime-type/charset
    **/
    public static String getMimeTypeCharset(String type)
    {
        return HTMLTools.getMimeTypeCharset(type, StringTools.getCharacterEncoding());
    }

    /**
    *** Appends the specified "charset" to mime type
    *** @param type     The Mime type
    *** @param charSet  The 'charset'
    *** @return The combined mime-type/charset
    **/
    public static String getMimeTypeCharset(String type, String charSet)
    {
        if (StringTools.isBlank(type) || StringTools.isBlank(charSet)) {
            return type;
        } else {
            return type + "; charset=" + charSet;
        }
    }

    /**
    *** Returns the MIME type for the specified extension
    *** @param extn  The extension
    *** @return The MIME type (default to CONTENT_TYPE_OCTET if the extension is not recognized)
    **/
    public static String getMimeTypeFromExtension(String extn)
    {
        return getMimeTypeFromExtension(extn, CONTENT_TYPE_OCTET);
    }

    /**
    *** Returns the MIME type for the specified extension
    *** @param extn  The extension
    *** @param dft   The default MIME type to return if the extension is not recognized
    *** @return The MIME type
    **/
    public static String getMimeTypeFromExtension(String extn, String dft)
    {

        /* default only */
        if (extn == null) {
            return dft;
        }

        /* image files */
        if (extn.equalsIgnoreCase("gif")) {
            return CONTENT_TYPE_GIF;
        } else
        if (extn.equalsIgnoreCase("jpeg") || extn.equalsIgnoreCase("jpg")) {
            return CONTENT_TYPE_JPEG;
        } else
        if (extn.equalsIgnoreCase("png")) {
            return CONTENT_TYPE_PNG;
        }
        
        /* plain */
        if (extn.equalsIgnoreCase("js")) {
            return CONTENT_TYPE_PLAIN;
        } else
        if (extn.equalsIgnoreCase("plain") || extn.equalsIgnoreCase("txt") || extn.equalsIgnoreCase("text")) {
            return CONTENT_TYPE_PLAIN;
        } else
        if (extn.equalsIgnoreCase("out") || extn.equalsIgnoreCase("conf")) {
            return CONTENT_TYPE_PLAIN;
        } 
        
        /* XML types */
        if (extn.equalsIgnoreCase("xml") || extn.equalsIgnoreCase("dtd")) {
            return CONTENT_TYPE_XML;
        } else
        if (extn.equalsIgnoreCase("xls")) {
            return CONTENT_TYPE_XLS;
        } else
        if (extn.equalsIgnoreCase("kml")) {
            return CONTENT_TYPE_KML;
        } else
        if (extn.equalsIgnoreCase("kmz")) {
            return CONTENT_TYPE_KMZ;
        } else
        if (extn.equalsIgnoreCase("json")) {
            return CONTENT_TYPE_JSON;
        }
        
        /* document types */
        if (extn.equalsIgnoreCase("html") || extn.equalsIgnoreCase("htm")) {
            return CONTENT_TYPE_HTML;
        } else
        if (extn.equalsIgnoreCase("pdf")) {
            return CONTENT_TYPE_PDF;
        } else
        if (extn.equalsIgnoreCase("ps")) {
            return CONTENT_TYPE_PS;
        } else
        if (extn.equalsIgnoreCase("doc")) {
            return CONTENT_TYPE_DOC;
        } else
        if (extn.equalsIgnoreCase("csv")) {
            return CONTENT_TYPE_CSV;
        } else
        if (extn.equalsIgnoreCase("xls")) {
            return CONTENT_TYPE_XLS;
        } else
        if (extn.equalsIgnoreCase("xlsx")) {
            return CONTENT_TYPE_XLSX;
        }

        /* archive types */
        if (extn.equalsIgnoreCase("zip")) {
            return CONTENT_TYPE_ZIP;
        } else
        if (extn.equalsIgnoreCase("jad")) {
            return CONTENT_TYPE_JAD;
        } else
        if (extn.equalsIgnoreCase("jar")) {
            return CONTENT_TYPE_JAR;
        } else
        if (extn.equalsIgnoreCase("binary") || extn.equalsIgnoreCase("bin")) {
            return CONTENT_TYPE_BINARY;
        } else
        if (extn.equalsIgnoreCase("octet")) {
            return CONTENT_TYPE_OCTET;
        }
        
        /* not found, return default */
        return dft;

    }

    /**
    *** Return the MIME type based on the data Magic Number
    *** @param data The data buffer to test for specific Magin-Numbers
    *** @return The MIME type (default to CONTENT_TYPE_OCTET if data is not recognized)
    **/
    public static String getMimeTypeFromData(byte data[])
    {
        return getMimeTypeFromData(data, CONTENT_TYPE_OCTET);
    }

    /**
    *** Return the MIME type based on the data Magic Number
    *** @param data  The data buffer to test for specific Magin-Numbers
    *** @param dft   The default MIME type to return if the data is not recognized
    *** @return The MIME type
    **/
    public static String getMimeTypeFromData(byte data[], String dft)
    {
        
        /* invalid data? */
        if (data == null) {
            return dft;
        }

        /* GIF */
        if (StringTools.compareEquals(data,MAGIC_GIF_87a,-1)) {
            return CONTENT_TYPE_GIF;
        } else
        if (StringTools.compareEquals(data,MAGIC_GIF_89a,-1)) {
            return CONTENT_TYPE_GIF;
        }

        /* JPEG */
        if (StringTools.compareEquals(data,MAGIC_JPEG,-1)) { // ([6..10]=="JFIF")
            return CONTENT_TYPE_JPEG;
        }

        /* PNG */
        if (StringTools.compareEquals(data,MAGIC_PNG,-1)) {
            return CONTENT_TYPE_PNG;
        }

        /* TIFF */
        if (StringTools.compareEquals(data,MAGIC_TIFF_II,-1)) {
            return CONTENT_TYPE_TIFF;
        } else
        if (StringTools.compareEquals(data,MAGIC_TIFF_MM,-1)) {
            return CONTENT_TYPE_TIFF;
        }

        /* BMP */
        if (StringTools.compareEquals(data,MAGIC_BMP,-1)) {
            return CONTENT_TYPE_BMP;
        }

        /* default */
        return dft;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final TagBlock TAG_HTML           = new TagBlock("<html>", "</html>");
    public static final TagBlock TAG_H1             = new TagBlock("<h1>", "</h1>");
    public static final TagBlock TAG_H2             = new TagBlock("<h2>", "</h2>");
    public static final TagBlock TAG_H3             = new TagBlock("<h3>", "</h3>");
    public static final TagBlock TAG_CENTER         = new TagBlock("<center>", "</center>");
    public static final TagBlock TAG_BLOCKQUOTE     = new TagBlock("<blockquote>", "</blockquote>");
    public static final TagBlock TAG_RIGHT          = new TagBlock("<div align=right>", "</div>");
    public static final TagBlock TAG_BOLD           = new TagBlock("<B>", "</B>");
    public static final TagBlock TAG_ITALIC         = new TagBlock("<I>", "</I>");
    public static final TagBlock TAG_MONOSPACE      = new TagBlock("<tt>", "</tt>");
    public static final TagBlock TAG_MONORIGHT      = new TagBlock(TAG_MONOSPACE, TAG_RIGHT);
    public static final TagBlock TAG_MONOCENTER     = new TagBlock(TAG_MONOSPACE, TAG_CENTER);
    public static final TagBlock TAG_RED            = TAG_COLOR("#FF0000");
    public static final TagBlock TAG_REDBOLD        = new TagBlock(TAG_BOLD, TAG_RED);
    public static final TagBlock TAG_GREEN          = TAG_COLOR("#007700");
    public static final TagBlock TAG_GREENBOLD      = new TagBlock(TAG_BOLD, TAG_GREEN);
    public static final TagBlock TAG_BLUE           = TAG_COLOR("#0000FF");
    public static final TagBlock TAG_BLUEBOLD       = new TagBlock(TAG_BOLD, TAG_BLUE);
    public static final TagBlock TAG_SMALLFONT      = new TagBlock("<font size=-1>", "</font>");
    public static final TagBlock TAG_SMALLCENTER    = new TagBlock(TAG_SMALLFONT, TAG_CENTER);
    public static final TagBlock TAG_NUMBERLIST     = new TagBlock("<ol>", "</ol>");
    public static final TagBlock TAG_BULLETLIST     = new TagBlock("<ul>", "</ul>");
    public static final TagBlock TAG_LISTITEM       = new TagBlock("<li>", "</li>");

    /**
    *** Returns a TagBlock 'font' wrapper with the specified color
    *** @param c  The font color
    *** @return The TagBlock 'font' wrapper
    **/
    public static TagBlock TAG_COLOR(Color c)
    {
        return TAG_COLOR(ColorTools.toHexString(c));
    }

    /**
    *** Returns a TagBlock 'font' wrapper with the specified color String
    *** @param cs  The font color String
    *** @return The TagBlock 'font' wrapper
    **/
    public static TagBlock TAG_COLOR(String cs)
    {
        if ((cs != null) && !cs.startsWith("#")) { cs = "#" + cs; }
        return new TagBlock("<font color=\"" + cs + "\">", "</font>");
    }

    /**
    *** Returns a TagBlock 'span' wrapper with the specified title/tooltip
    *** @param tip  The title/tooltip
    *** @return The TagBlock 'span' wrapper
    **/
    public static TagBlock TAG_TOOLTIP(String tip)
    {
        String t = StringTools.quoteString(tip);
        return new TagBlock("<span title=" + t + ">", "</span>");
    }

    /**
    *** Returns a TagBlock 'a href=' wrapper with the specified url
    *** @param url  The URL
    *** @return The TagBlock 'a' wrapper
    **/
    public static TagBlock TAG_LINK(String url)
    {
        return new TagBlock("<a href=\"" + url + "\">", "</a>");
    }

    /**
    *** Returns a TagBlock 'a href=' wrapper with the specified url
    *** @param url  The URL
    *** @param newWindow True to specify "target=_blank"
    *** @return The TagBlock 'a' wrapper
    **/
    public static TagBlock TAG_LINK(String url, boolean newWindow)
    {
        StringBuffer a = new StringBuffer();
        a.append("<a href=\"").append(url).append("\"");
        if (newWindow) { a.append(" target='_blank'"); }
        a.append(">");
        return new TagBlock(a.toString(), "</a>");
    }

    // ------------------------------------------------------------------------

    /* encode HTML parameter */
    private static final String ENC_CHARS = " %=<>&'\"";

    /**
    *** Encode specific characters in the specified text object
    *** @param text  The text Object
    *** @return The encoded String
    **/
    public static String encodeParameter(Object text)
    {
        String s = text.toString();

        /* encode special characters */
        char ch[] = new char[s.length()];
        s.getChars(0, s.length(), ch, 0);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ch.length; i++) {
            if (ENC_CHARS.indexOf(ch[i]) >= 0) {
                int x = (int)ch[i] + 0x100;
                sb.append("%" + Integer.toHexString(x).substring(1).toUpperCase());
            } else {
                sb.append(ch[i]);
            }
        }

        return sb.toString();
    }

    /**
    *** Decode URL encoded hex values from the specified String
    *** @param text  The encoded text
    *** @return The decoded String
    **/
    public static String decodeParameter(String text)
    {
        StringBuffer sb = new StringBuffer();
        if (!StringTools.isBlank(text)) {
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if ((ch == '%') && ((i + 2) < text.length())) {
                    int h1 = StringTools.hexIndex(text.charAt(i+1));
                    int h2 = StringTools.hexIndex(text.charAt(i+2)); 
                    if ((h1 >= 0) && (h2 >= 0)) {
                        sb.append((char)((h1 * 16) + h2));
                        i += 2;
                        continue;
                    }
                }
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Encode the specified color into a String value
    *** @param color  The Color object
    *** @return The String representation of the color
    **/
    public static String color(Color color)
    {
        int ci = 0x000000;
        ci |= (color.getRed()   << 16) & 0xFF0000;
        ci |= (color.getGreen() <<  8) & 0x00FF00;
        ci |= (color.getBlue()  <<  0) & 0x0000FF;
        String cs = Integer.toHexString(ci | 0x1000000).substring(1).toUpperCase();
        return "#" + cs;
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates/Returns a String representing a link to a URL with the specified text description
    *** and attributes.
    *** @param ref        The referenced URL link ("target='_blank'" will be applied)
    *** @param text       The link text (html filtering will be applied)
    *** @return The assembled link ('a' tag)
    **/
    public static String createLink(String ref, Object text)
    {
        return HTMLTools.createLink(ref, true, text, true);
    }

    /**
    *** Creates/Returns a String representing a link to a URL with the specified text description
    *** and attributes.
    *** @param ref        The referenced URL link
    *** @param newWindow  If true, "target='_blank'" will be specified
    *** @param text       The link text (html filtering will be applied)
    *** @return The assembled link ('a' tag)
    **/
    public static String createLink(String ref, boolean newWindow, Object text)
    {
        return createLink(ref, newWindow, text, true);
    }

    /**
    *** Creates/Returns a String representing a link to a URL with the specified text description
    *** and attributes.
    *** @param ref        The referenced URL link
    *** @param newWindow  If true, "target='_blank'" will be specified
    *** @param text       The link text
    *** @param filterText True to apply HTML filtering to the specified text
    *** @return The assembled link ('a' tag)
    **/
    public static String createLink(String ref, boolean newWindow, Object text, boolean filterText)
    {
        String t = (text != null)? text.toString() : "";
        StringBuffer sb = new StringBuffer();
        sb.append("<a href='").append(ref).append("'");
        if (newWindow) { sb.append(" target='_blank'"); }
        sb.append(">");
        sb.append(filterText? StringTools.htmlFilterText(t) : t);
        sb.append("</a>");
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a 'span' wrapper with the specified 'font' specification
    *** @param family  The font family
    *** @param style   The font style ('normal', 'italic', 'oblique')
    *** @param variant The font variant ('normal', 'small-caps')
    *** @param weight  The font weight ('normal', 'bold', 'bolder', 'lighter', etc)
    *** @param ptsize  The font point size
    *** @return The 'font' tag specification
    ***/
    public static String font(String family, String style, String variant, String weight, int ptsize)
    {
        // Reference: http://www.w3.org/TR/CSS21/fonts.html
        StringBuffer sb = new StringBuffer();
        sb.append("<span style='");
        if ((family != null) && !family.equals("")) {
            // "Arial,Helvetica,san-serif"
            sb.append("font-family:").append(family).append(";");
        }
        if ((style != null) && !style.equals("")) {
            // "normal", "italic", "oblique"
            sb.append("font-style:").append(style).append(";");
        }
        if ((variant != null) && !variant.equals("")) {
            // "normal", "small-caps"
            sb.append("font-variant:").append(variant).append(";");
        }
        if ((weight != null) && !weight.equals("")) {
            // "normal"(400), "bold"(700), "bolder", "lighter", "100".."900"
            sb.append("font-weight:").append(weight).append(";");
        }
        if (ptsize > 0) {
            sb.append("font-size:").append(ptsize).append("pt;");
        }
        sb.append("'>");
        return sb.toString();
    }

    /**
    *** Closes a 'span' tag used to specify 'font' style.
    **/
    public static String _font()
    {
        return "</span>";
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representing a start-of-JavaScript tag
    ***/
    public static String startJavaScript()
    {
        return "<script language='JavaScript'><!--";
    }

    /**
    *** Returns a String representing an end-of-JavaScript tag
    ***/
    public static String endJavaScript()
    {
        return "// --></script>";
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a 'meta' tag with a 'refresh' option
    *** @param delay  Delay before next update 
    *** @param url    The URL to jump to after the delay
    *** @return  The String representing the assembled 'meta' tag
    ***/
    public static String autoRefresh(int delay, String url)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<meta http-equiv='Refresh' ");
        sb.append("content='").append(delay).append(";URL=").append(url).append("'>");
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sends a POST to the specified URL, then reads and returns the response
    *** @param pageURLStr   The URL to which the POST is sent
    *** @param contentType  The MIME type of the POST data sent to the server
    *** @param postData     The data sent to the server
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    ***/
    public static byte[] readPage_POST(String pageURLStr, String contentType, byte postData[], int timeoutMS)
        throws IOException
    {
        URL pageURL = new URL(pageURLStr);
        return readPage_POST(pageURL, contentType, postData, timeoutMS);
    }

    /**
    *** Sends a POST to the specified URL, then reads and returns the response
    *** @param pageURL      The URL to which the POST is sent
    *** @param contentType  The MIME type of the POST data sent to the server
    *** @param postData     The data sent to the server
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    ***/
    public static byte[] readPage_POST(URL pageURL, String contentType, byte postData[], int timeoutMS)
        throws IOException
    {
        if (!StringTools.isBlank(contentType)) {
            Properties hp = new Properties();
            hp.setProperty(HEADER_CONTENT_TYPE, contentType);
            return readPage_POST(pageURL, hp, postData, timeoutMS);
        } else {
            return readPage_POST(pageURL, (Properties)null, postData, timeoutMS);
        }
    }

    /**
    *** Sends a POST to the specified URL, then reads and returns the response
    *** @param pageURLStr   The URL to which the POST is sent
    *** @param headerProps  The POST header properties
    *** @param postData     The data sent to the server
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    ***/
    public static byte[] readPage_POST(String pageURLStr, Properties headerProps, byte postData[], int timeoutMS)
        throws IOException
    {
        URL pageURL = new URL(pageURLStr);
        return readPage_POST(pageURL, headerProps, postData, timeoutMS);
    }
    
    /**
    *** Sends a POST to the specified URL, then reads and returns the response
    *** @param pageURL      The URL to which the POST is sent
    *** @param headerProps  The POST header properties
    *** @param postData     The data sent to the server
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    ***/
    public static byte[] readPage_POST(URL pageURL, Properties headerProps, byte postData[], int timeoutMS)
        throws IOException
    {

        /* valid url? */
        if (pageURL == null) {
            return null;
        }

        /* send POST */
        byte data[] = null;
        HttpURLConnection httpConnect = null;
        OutputStream postOutput = null;
        InputStream  postInput = null;
        try {
            //Print.logInfo("POST v1.2");

            /* init connection */
            httpConnect = (HttpURLConnection)(pageURL.openConnection());
            httpConnect.setRequestMethod(REQUEST_POST);
            httpConnect.setAllowUserInteraction(false);
            httpConnect.setDoInput(true);
            httpConnect.setDoOutput(true);
            httpConnect.setUseCaches(false);
            httpConnect.setRequestProperty(PROP_User_Agent, HTMLTools.getHttpUserAgent());
            if (timeoutMS >= 0) {
                httpConnect.setConnectTimeout(timeoutMS);
            }

            /* header properties */
            if (headerProps != null) {
                for (Enumeration<?> pe = headerProps.propertyNames(); pe.hasMoreElements();) {
                    String hk = (String)pe.nextElement();
                    String hv = headerProps.getProperty(hk);
                    httpConnect.setRequestProperty(hk, hv);
                }
            }

            /* write data */
            if (postData != null) {
                httpConnect.setRequestProperty(HEADER_CONTENT_LENGTH, String.valueOf(postData.length));
                postOutput = httpConnect.getOutputStream();
                postOutput.write(postData);
                postOutput.flush();
            }

            /* connect */
            httpConnect.connect(); // possible NoRouteToHostException, etc.

            /* read data */
            //int contentLen = httpConnect.getContentLength();
            //Print.logInfo("Read ContentLength: " + contentLen);
            //if ((contentLen > 0) || (contentLen == -1)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            postInput = new BufferedInputStream(httpConnect.getInputStream());
            FileTools.copyStreams(postInput, output);
            data = output.toByteArray();
            //} else {
            //    data = new byte[0];
            //}

        } finally {

            /* close */
            if (postOutput  != null) { postOutput.close(); }
            if (postInput   != null) { postInput.close(); }
            if (httpConnect != null) { httpConnect.disconnect(); }

        }

        /* return data */
        return data;

    }

    // ------------------------------------------------------------------------

    /**
    *** Sends a GET to the specified URL, then reads and returns the response
    *** @param pageURLStr   The URL to which the GET is sent
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    ***/
    public static byte[] readPage_GET(String pageURLStr, int timeoutMS)
        throws Throwable
    {
        URL pageURL = new URL(pageURLStr);
        return readPage_GET(pageURL, timeoutMS);
    }

    /**
    *** Sends a GET to the specified URL, then reads and returns the response
    *** @param pageURL      The URL to which the GET is sent
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    ***/
    public static byte[] readPage_GET(URL pageURL, int timeoutMS)
        throws Throwable
    {
        byte data[] = null;
        HttpURLConnection httpConnect = null;
        try {            
           
            /* init connection */
            httpConnect = (HttpURLConnection)(pageURL.openConnection());
            httpConnect.setAllowUserInteraction(false);
            httpConnect.setRequestMethod(REQUEST_GET);
            httpConnect.setRequestProperty(PROP_User_Agent, HTMLTools.getHttpUserAgent());
            if (timeoutMS >= 0) {
                httpConnect.setConnectTimeout(timeoutMS);
            }

            /* connect */
            //Print.logDebug("Connecting ...");
            httpConnect.connect(); // possible NoRouteToHostException, etc.
            //Print.logDebug("Connected.");
            
            /* read data */
            int contentLen = httpConnect.getContentLength();
            if ((contentLen > 0) || (contentLen == -1)) {
                data = FileTools.readStream(new BufferedInputStream(httpConnect.getInputStream()));
            } else {
                data = new byte[0];
            }
            
        } finally {
            if (httpConnect != null) { httpConnect.disconnect(); }
        }
        return data;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sends a GET to the specified URL, then reads and returns the response.  This method
    *** will not throw any Exceptions.  Instead, the returned response is null if any errors
    *** are encountered.
    *** @param pageURLStr   The URL to which the GET is sent
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    ***/
    public static byte[] readPage_GET_LogError(String pageURLStr, int timeoutMS)
    {
        try {
            URL pageURL = new URL(pageURLStr);
            return readPage_GET_LogError(pageURL, timeoutMS);
        } catch (MalformedURLException mue) {
            Print.logError(mue.toString()); // DNS?
            return null;
        }
    }

    /**
    *** Sends a GET to the specified URL, then reads and returns the response.  This method
    *** will not throw any Exceptions.  Instead, the returned response is null if any errors
    *** are encountered.
    *** @param pageURL      The URL to which the GET is sent
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The response from the server
    ***/
    public static byte[] readPage_GET_LogError(URL pageURL, int timeoutMS)
    {
        try {
            return HTMLTools.readPage_GET(pageURL, timeoutMS);
        } catch (UnknownHostException uhe) {
            Print.logError(uhe.toString()); // DNS?
        } catch (NoRouteToHostException nrthe) {
            Print.logError(nrthe.toString()); // DNS?
        } catch (ConnectException ce) {
            Print.logError(ce.toString()); // timed out?
        } catch (SocketException se) {
            Print.logError(se.toString()); // DNS?
        } catch (FileNotFoundException fnfe) {
            Print.logError(fnfe.toString()); // wrong URL?
        } catch (Throwable t) {
            Print.logStackTrace("Read page", t);
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an InputStream for reading the contents of the specified URL
    *** @param pageURLStr   The URL
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The InputStream
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    **/
    public static InputStream inputStream_GET(String pageURLStr, int timeoutMS)
        throws IOException
    {
        URL pageURL = new URL(pageURLStr);
        return inputStream_GET(pageURL, timeoutMS);
    }

    /**
    *** Returns an InputStream for reading the contents of the specified URL
    *** @param pageURL      The URL
    *** @param timeoutMS    Connection timeout in milliseconds (<=0 for indefinite timeout)
    *** @return The InputStream
    *** @throws NoRouteToHostException if the remote host could not be reached
    *** @throws IOException if an I/O error occurs
    **/
    public static InputStream inputStream_GET(URL pageURL, int timeoutMS)
        throws IOException
    {
           
        /* init connection */
        final HttpURLConnection httpConnect = (HttpURLConnection)(pageURL.openConnection());
        httpConnect.setAllowUserInteraction(false);
        httpConnect.setRequestMethod(REQUEST_GET);
        httpConnect.setRequestProperty(PROP_User_Agent, HTMLTools.getHttpUserAgent());
        if (timeoutMS >= 0) {
            httpConnect.setConnectTimeout(timeoutMS);
        }

        /* connect */
        httpConnect.connect(); // possible NoRouteToHostException, etc.

        /* read data */
        int contentLen = httpConnect.getContentLength();
        if ((contentLen > 0) || (contentLen == -1)) {
            return new BufferedInputStream(httpConnect.getInputStream()) {
                public void close() throws IOException {
                    //Print.logInfo("Closing HTTP stream ...");
                    httpConnect.disconnect();
                }
            };
        } else {
            httpConnect.disconnect();
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom TagBlock class
    **/
    public static class TagBlock
    {
        private String   startTag   = null;
        private String   endTag     = null;
        private TagBlock tagGroup[] = null;
        public TagBlock(String startTag, String endTag) {
            this.startTag = startTag;
            this.endTag   = endTag;
        }
        public TagBlock(TagBlock group[]) {
            this((String)null, (String)null);
            this.tagGroup = group;
        }
        public TagBlock(TagBlock tb1, TagBlock tb2) {
            this(new TagBlock[] { tb1, tb2 });
        }
        public TagBlock(TagBlock tb1, TagBlock tb2, TagBlock tb3) {
            this(new TagBlock[] { tb1, tb2, tb3 });
        }
        public String getStartTag() {
            StringBuffer sb = new StringBuffer();
            if (this.tagGroup != null) {
                for (int i = 0; i < this.tagGroup.length; i++) {
                    if (this.tagGroup[i] != null) {
                        sb.append(this.tagGroup[i].getStartTag());
                    }
                }
            }
            if (this.startTag != null) {
                sb.append(this.startTag);
            }
            return sb.toString();
        }
        public String getEndTag() {
            StringBuffer sb = new StringBuffer();
            if (this.endTag != null) {
                sb.append(this.endTag);
            }
            if (this.tagGroup != null) {
                for (int i = (this.tagGroup.length - 1); i >= 0; i--) {
                    if (this.tagGroup[i] != null) {
                        sb.append(this.tagGroup[i].getEndTag());
                    }
                }
            }
            return sb.toString();
        }
        public StringBuffer wrap(Object text, boolean htmlFilter, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            if (text != null) {
                String v = text.toString();
                sb.append(this.getStartTag());
                sb.append(htmlFilter? StringTools.htmlFilterText(v) : v);
                sb.append(this.getEndTag());
            }
            return sb;
        }
        public String wrap(Object text, boolean htmlFilter) {
            return this.wrap(text, htmlFilter, new StringBuffer()).toString();
        }
        public String wrap(Object text) {
            return this.wrap(text, false);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom tag wrapper class
    **/
    public static class TagWrap
    {
        private Object text = null;
        private TagBlock tags[] = null;
        public TagWrap(Object text, TagBlock tags[]) {
            this.text = text;
            this.tags = tags;
        }
        public TagWrap(Object text, TagBlock tag) {
            this(text, new TagBlock[] { tag });
        }
        public String toString(boolean html) {
            if (this.text != null) {
                if (html) {
                    String v = (this.text instanceof TagWrap)? ((TagWrap)this.text).toString(html) : StringTools.htmlFilterText(this.text);
                    if ((this.tags != null) && (this.tags.length > 0)) {
                        for (int i = 0; i < this.tags.length; i++) {
                            if (this.tags[i] != null) { v = this.tags[i].wrap(v); }
                        }
                    }
                    return v;
                } else {
                    return this.text.toString();
                }
            } else {
                return "";
            }
        }
        public String toString() {
            return this.toString(false);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates an IField/ILayer section.
    *** @param srcURL  The source URL displayed within the frame
    *** @param sb      The destination StringBuffer
    *** @return The StringBuffer where the created IFrame/ILayer section is placed
    **/
    public static StringBuffer createIFrameTemplate(String srcURL, StringBuffer sb)
    {
        return HTMLTools.createIFrameTemplate(-1, -1, srcURL, sb);
    }

    /**
    *** Creates an IField/ILayer section.
    *** @param srcURL  The source URL displayed within the frame
    *** @return The String containing the IFrame/ILayer section
    **/
    public static String createIFrameTemplate(String srcURL)
    {
        return HTMLTools.createIFrameTemplate(-1, -1, srcURL, null).toString();
    }

    /**
    *** Creates an IField/ILayer section.
    *** @param w       The width of the IFrame
    *** @param h       The height of the IFrame
    *** @param srcURL  The source URL displayed within the frame
    *** @return The String containing the IFrame/ILayer section
    **/
    public static String createIFrameTemplate(int w, int h, String srcURL)
    {
        return HTMLTools.createIFrameTemplate(w, h, srcURL, null).toString();
    }

    /**
    *** Creates an IField/ILayer section.
    *** @param w       The width of the IFrame
    *** @param h       The height of the IFrame
    *** @param srcURL  The source URL displayed within the frame
    *** @param sb      The destination StringBuffer
    *** @return The StringBuffer where the created IFrame/ILayer section is placed
    **/
    public static StringBuffer createIFrameTemplate(int w, int h, String srcURL, StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        String id = "id.iframe";
        String ws = (w > 0)? String.valueOf(w) : "100%";
        String hs = (h > 0)? String.valueOf(h) : "100%";
        //String style = "STYLE='{visibility=visible;width=" + w + "px;height=" + h + "px}' ";

        sb.append("<IFRAME WIDTH='"+ws+"' HEIGHT='"+hs+"' FRAMEBORDER='0' ID='"+id+"' SRC='"+srcURL+"'>\n");
        sb.append("<ILAYER WIDTH='"+ws+"' HEIGHT='"+hs+"' FRAMEBORDER='0' ID='"+id+"' SRC='"+srcURL+"'>\n");
        sb.append("InternetExplorer or Netscape is required to view this page\n");
        sb.append("</ILAYER>\n");
        sb.append("</IFRAME>\n");

        return sb;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_GET[]       = new String[] { "get"      };
    private static final String ARG_POST[]      = new String[] { "post"     };
    private static final String ARG_POSTDATA[]  = new String[] { "postData" };
    private static final String ARG_DECODE[]    = new String[] { "decode", "dec" };

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        if (RTConfig.hasProperty(ARG_DECODE)) {
            String s = RTConfig.getString(ARG_DECODE,"");
            Print.sysPrintln("Decoded: " + HTMLTools.decodeParameter(s));
            System.exit(0);
        }

        if (RTConfig.hasProperty(ARG_GET)) {
            try {
                URIArg uri = new URIArg(RTConfig.getString(ARG_GET,""));
                byte data[] = readPage_GET(uri.toString(), -1);
                String s = StringTools.toStringValue(data);
                Print.sysPrintln("Response:\n");
                Print.sysPrintln(s);
                System.exit(0);
            } catch (Throwable th) {
                Print.logException("Error", th);
                System.exit(99);
            }
        }

        if (RTConfig.hasProperty(ARG_POST)) {
            try {
                String postURL    = RTConfig.getString(ARG_POST,"");
                URIArg uri        = new URIArg(postURL);
                String postDataS  = RTConfig.getString(ARG_POSTDATA,uri.getArgString());
                Print.sysPrintln("Post URL : " + postURL);
                Print.sysPrintln("Post Data: " + postDataS);
                byte   postData[] = postDataS.startsWith("0x")? 
                    StringTools.parseHex(postDataS,new byte[0]) : postDataS.getBytes();
                byte   respData[] = readPage_POST(uri.toString()/*getURI()*/, CONTENT_TYPE_PLAIN, postData, -1);
                String resp = StringTools.toStringValue(respData);
                Print.sysPrintln("Response :\n");
                Print.sysPrintln(resp);
                System.exit(0);
            } catch (Throwable th) {
                Print.logException("Error", th);
                System.exit(99);
            }
        }

        Print.logWarn("Missing options ...");
        
    }

}
