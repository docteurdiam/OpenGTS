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
// References:
//  http://www.irt.org/articles/js128/index.htm
//  http://www.sourcesnippets.com/javascript-open-window.html
// ----------------------------------------------------------------------------
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/05/06  Martin D. Flynn
//     -Fixed 'writeJS' to not write 'null' records.
//  2007/05/20  Martin D. Flynn
//     -Added 'writeLatLonToolsJS' method.
//  2007/12/13  Martin D. Flynn
//     -Added 'writeElementLocSizeJS' method.
//  2008/02/21  Martin D. Flynn
//     -Moved various JavaScript functions to 'utils.js'
//  2008/02/27  Martin D. Flynn
//     -Added ability to specify the 'utils.js' location in the 'webapp.conf' file.
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.JspWriter;

import org.opengts.util.*;
import org.opengts.db.*;

public class JavaScriptTools
{

    // ------------------------------------------------------------------------
    // Return specified JS file reference relative to "./js" directory
    
    public static String qualifyJSFileRef(String jsFileRef)
    {
        if (jsFileRef == null) {
            return null;
        } else
        if (jsFileRef.startsWith("http:") || jsFileRef.startsWith("https:")) {
            return jsFileRef;
        } else
        if (jsFileRef.startsWith("file:")) {
            return jsFileRef;
        } else
        if (jsFileRef.startsWith("/") || jsFileRef.startsWith(".")) {
            return jsFileRef;
        } else {
            String js_dir = RTConfig.getString(DBConfig.PROP_track_js_directory, "./js");
            return js_dir + "/" + jsFileRef;
        }
    }

    // ------------------------------------------------------------------------
    // Write JS "var"

    public static void writeJSVar(PrintWriter out, String varName, Object value)
        throws IOException
    {
        JavaScriptTools.writeJSVar(out, varName, value, true);
    }

    public static void writeJSVar(PrintWriter out, String varName, Object value, boolean quoteString)
        throws IOException
    {
        String var = varName; // StringTools.leftAlign(varName,20);
        out.write("var "+var+" = ");
        if (value == null) {
            out.write("null");
        } else 
        if ((value instanceof Number) || (value instanceof Boolean)) {
            out.write(value.toString());
        } else 
        if (value instanceof Object[]) {
            Object obj[] = (Object[])value;
            out.write("[");
            //out.write("\n");
            for (int i = 0; i < obj.length; i++) {
                Object v = obj[i];
                out.write(" ");
                if ((v instanceof Number) || (v instanceof Boolean)) {
                    out.write(v.toString());
                } else {
                    String val = StringTools.replace(v.toString(),"\"","\\\"");
                    val = StringTools.replace(val,"\n","\\n");
                    out.write("\""+val+"\"");
                }
                if ((i + 1) < obj.length) {
                    out.write(",");
                    //out.write("\n");
                }
            }
            out.write("]");
        } else
        if (!quoteString) {
            out.write(value.toString());
        } else {
            String val = StringTools.replace(value.toString(),"\"","\\\"");
            val = StringTools.replace(val,"\n","\\n");
            out.write("\""+val+"\"");
        }
        out.write(";\n");
    }

    // ------------------------------------------------------------------------
    // Write 'XXXXXX.js' included files

    public static void writeJSInclude(PrintWriter out, String jsFileRef, HttpServletRequest request)
        throws IOException
    {
        JavaScriptTools.writeJSInclude(out, jsFileRef, AttributeTools.GetSessionSequence(request));
    }

    public static void writeJSInclude(PrintWriter out, String jsFileRef, int seq)
        throws IOException
    {
        String src = jsFileRef;
        if ((seq > 0) && jsFileRef.endsWith(".js")) {
            src += "?seq=" + seq;
        }
        out.write("<script src=\""+src+"\" type=\"text/javascript\"></script>\n");
    }

    public static void writeJSIncludes(PrintWriter out, String jsFileRef[], HttpServletRequest request)
        throws IOException
    {
        JavaScriptTools.writeJSIncludes(out, jsFileRef, AttributeTools.GetSessionSequence(request));
    }

    public static void writeJSIncludes(PrintWriter out, String jsFileRefs[], int seq) 
        throws IOException
    {
        if (jsFileRefs != null) {
            for (int i = 0; i < jsFileRefs.length; i++) {
                String js = StringTools.trim(jsFileRefs[i]);
                if (!StringTools.isBlank(js)) {
                    JavaScriptTools.writeJSInclude(out, js, seq);
                }
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public static void writeJSInclude(JspWriter out, String jsFileRef, HttpServletRequest request)
        throws IOException
    {
        JavaScriptTools.writeJSInclude(out, jsFileRef, AttributeTools.GetSessionSequence(request));
    }

    public static void writeJSInclude(JspWriter out, String jsFileRef, int seq)
        throws IOException
    {
        String src = jsFileRef;
        if ((seq > 0) && jsFileRef.endsWith(".js")) {
            src += "?seq=" + seq;
        }
        out.write("<script src=\""+src+"\" type=\"text/javascript\"></script>\n");
    }

    public static void writeJSIncludes(JspWriter out, String jsFileRef[], HttpServletRequest request)
        throws IOException
    {
        JavaScriptTools.writeJSIncludes(out, jsFileRef, AttributeTools.GetSessionSequence(request));
    }

    public static void writeJSIncludes(JspWriter out, String jsFileRefs[], int seq) 
        throws IOException
    {
        if (jsFileRefs != null) {
            for (int i = 0; i < jsFileRefs.length; i++) {
                String js = StringTools.trim(jsFileRefs[i]);
                if (!StringTools.isBlank(js)) {
                    JavaScriptTools.writeJSInclude(out, js, seq);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Write 'utils.js' 

    public static void writeUtilsJS(PrintWriter out, HttpServletRequest request)
        throws IOException
    {
        JavaScriptTools.writeUtilsJS(out, AttributeTools.GetSessionSequence(request));
    }

    public static void writeUtilsJS(PrintWriter out, int seq)
        throws IOException
    {
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("utils.js"), seq);
    }

    public static void writeUtilsJS(JspWriter out, HttpServletRequest request)
        throws IOException
    {
        JavaScriptTools.writeUtilsJS(out, AttributeTools.GetSessionSequence(request));
    }

    public static void writeUtilsJS(JspWriter out, int seq)
        throws IOException
    {
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("utils.js"), seq);
    }

    // ------------------------------------------------------------------------
    // Start/End JavaScript section 
    
    public static void writeStartJavaScript(PrintWriter out)
    {
        out.println("<script type=\"text/javascript\">");
      //out.println("//<![CDATA[");
        out.println("<!--");
    }
    
    public static void writeEndJavaScript(PrintWriter out)
    {
      //out.println("//]]>");
        out.println("//-->");
        out.println("</script>");
    }
        
    // ------------------------------------------------------------------------
    // helper to write out JavaScript lines

    public static void writeJS(PrintWriter out, String js[])
        throws IOException
    {
        for (int i = 0; i < js.length; i++) {
            if (js[i] != null) {
                out.println(js[i]);
            }
        }
    }

    public static void writeJS(PrintWriter out, String js)
        throws IOException
    {
        if (js != null) {
            out.println(js);
        }
    }
    
}
