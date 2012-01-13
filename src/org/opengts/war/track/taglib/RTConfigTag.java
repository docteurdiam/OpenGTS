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
//  2008/07/21  Martin D. Flynn
//     -Initial release
//  2009/05/01  Martin D. Flynn
//     -Fixed bug that unecessarily processed non-matching tag blocks
//  2009/12/16  Martin D. Flynn
//     -Added compare types "gt", "ge", "lt", "le", "in", "ni"
//  2011/01/28  Martin D. Flynn
//     -Added "andTrue", "andFalse", "orTrue", "orFalse" comparisons
// ----------------------------------------------------------------------------
package org.opengts.war.track.taglib;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.opengts.util.*;
import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class RTConfigTag 
    extends BodyTagSupport
    implements Constants, StringTools.KeyValueMap
{

    // ------------------------------------------------------------------------

  //private static final String KEY_START               = "@@{";
    private static final String KEY_START               = StringTools.KEY_START;
    private static final String KEY_END                 = StringTools.KEY_END;
    private static final String ARG_DELIM               = StringTools.ARG_DELIM;
    private static final String DFT_DELIM               = StringTools.DFT_DELIM;

    private static final String COMPARE_EQ              = "eq";         // ==
    private static final String COMPARE_NE              = "ne";         // !=
    private static final String COMPARE_GT              = "gt";         // >
    private static final String COMPARE_GE              = "ge";         // >=
    private static final String COMPARE_LT              = "lt";         // <
    private static final String COMPARE_LE              = "le";         // <=
    private static final String COMPARE_INSET           = "inset";      // in set
    private static final String COMPARE_NOTINSET        = "!inset";     // not in set
    private static final String COMPARE_DEFINED         = "defined";    // not in set

    private static final String BOOLEAN_TRUE            = "true";
    private static final String BOOLEAN_FALSE           = "false";

    // ------------------------------------------------------------------------
    // <%@ taglib uri="./Track" prefix="gts" %>
    // <gts:var ifKey="key" [compare="eq"] [value="false"]>Some html</gts:var>
    // ------------------------------------------------------------------------
    // <jsp:forward page="xxxxx"/>
    // ------------------------------------------------------------------------

    private String  ifKey           = null;
    private String  ifCompare       = null;
    private String  ifCompareType   = COMPARE_EQ;
    private boolean hasElse         = false;

    private String  andKey          = null;
    private String  andCompare      = null;
    private String  andCompareType  = COMPARE_EQ;
    
    private String  orKey           = null;
    private String  orCompare       = null;
    private String  orCompareType   = COMPARE_EQ;

    private int     isMatch         = -1; // undefined

    /**
    *** Gets the "ifKey" attribute
    *** @return The "ifKey" attribute
    **/
    public String getIfKey()
    {
        return this.getIf();
    }

    /**
    *** Gets the "if" attribute
    *** @return The "if" attribute
    **/
    public String getIf()
    {
        return this.ifKey;
    }

    /**
    *** Gets the "ifDefined" attribute
    *** @return The "ifDefined" attribute
    **/
    public String getIfDefined()
    {
        if ((this.ifCompare != null) && this.ifCompare.equalsIgnoreCase(BOOLEAN_TRUE)) {
            return this.ifKey;
        } else {
            return null;
        }
    }

    /**
    *** Gets the "ifTrue" attribute
    *** @return The "ifTrue" attribute
    **/
    public String getIfTrue()
    {
        if ((this.ifCompare != null) && this.ifCompare.equalsIgnoreCase(BOOLEAN_TRUE)) {
            return this.ifKey;
        } else {
            return null;
        }
    }

    /**
    *** Gets the "ifFalse" attribute
    *** @return The "ifFalse" attribute
    **/
    public String getIfFalse()
    {
        if ((this.ifCompare != null) && this.ifCompare.equalsIgnoreCase(BOOLEAN_FALSE)) {
            return this.ifKey;
        } else {
            return null;
        }
    }

    /**
    *** Sets the "ifKey" attribute
    *** @param k  The "ifKey" attribute value
    **/
    public void setIfKey(String k)
    {
        this.setIf(k);
    }

    /**
    *** Sets the "if" attribute
    *** @param k  The "if" attribute value
    **/
    public void setIf(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = null;
      //this.ifCompareType  = COMPARE_EQ; <-- explicitly set later
        this.hasElse        = false;
    }

    /**
    *** Sets the "ifDefined" attribute
    *** @param k  The "ifDefined" attribute value
    **/
    public void setIfDefined(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = BOOLEAN_TRUE;
        this.ifCompareType  = COMPARE_DEFINED;
        this.hasElse        = false;
    }

    /**
    *** Sets the "ifTrue" attribute
    *** @param k  The "ifTrue" attribute value
    **/
    public void setIfTrue(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = BOOLEAN_TRUE;
        this.ifCompareType  = COMPARE_EQ;
        this.hasElse        = false;
    }

    /**
    *** Sets the "ifFalse" attribute
    *** @param k  The "ifFalse" attribute value
    **/
    public void setIfFalse(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = BOOLEAN_FALSE;
        this.ifCompareType  = COMPARE_EQ;
        this.hasElse        = false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "ifElseKey" attribute
    *** @return The "ifElseKey" attribute
    **/
    public String getIfElseKey()
    {
        return this.getIf();
    }

    /**
    *** Gets the "ifElse" attribute
    *** @return The "ifElse" attribute
    **/
    public String getIfElse()
    {
        return this.getIf();
    }

    /**
    *** Sets the "ifElseKey" attribute
    *** @param k  The "ifElseKey" attribute value
    **/
    public void setIfElseKey(String k)
    {
        this.setIfElse(k);
    }

    /**
    *** Sets the "ifElseKey" attribute
    *** @param k  The "ifElseKey" attribute value
    **/
    public void setIfElse(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = null;
        this.hasElse        = true;
    }

    /**
    *** Returns true if an 'else' exists
    *** @return True is an 'else' exists
    **/
    public boolean hasElse()
    {
        return this.hasElse;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the "andKey" attribute
    *** @return The "andKey" attribute
    **/
    public String getAndKey()
    {
        return this.andKey;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the "andTrue" attribute
    *** @return The "andTrue" attribute
    **/
    public String getAndTrue()
    {
        if ((this.andCompare != null) && this.andCompare.equalsIgnoreCase(BOOLEAN_TRUE)) {
            return this.andKey;
        } else {
            return null;
        }
    }

    /**
    *** Sets the "andTrue" attribute
    *** @param k  The "andTrue" attribute value
    **/
    public void setAndTrue(String k)
    {
        this.andKey         = k;
        this.andCompare     = BOOLEAN_TRUE;
        this.andCompareType = COMPARE_EQ;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the "andFalse" attribute
    *** @return The "andFalse" attribute
    **/
    public String getAndFalse()
    {
        if ((this.andCompare != null) && this.andCompare.equalsIgnoreCase(BOOLEAN_FALSE)) {
            return this.andKey;
        } else {
            return null;
        }
    }

    /**
    *** Sets the "andFalse" attribute
    *** @param k  The "andFalse" attribute value
    **/
    public void setAndFalse(String k)
    {
        this.andKey         = k;
        this.andCompare     = BOOLEAN_FALSE;
        this.andCompareType = COMPARE_EQ;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the "orKey" attribute
    *** @return The "orKey" attribute
    **/
    public String getOrKey()
    {
        return this.orKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "orTrue" attribute
    *** @return The "orTrue" attribute
    **/
    public String getOrTrue()
    {
        if ((this.orCompare != null) && this.orCompare.equalsIgnoreCase(BOOLEAN_TRUE)) {
            return this.orKey;
        } else {
            return null;
        }
    }

    /**
    *** Sets the "orTrue" attribute
    *** @param k  The "orTrue" attribute value
    **/
    public void setOrTrue(String k)
    {
        this.orKey          = k;
        this.orCompare      = BOOLEAN_TRUE;
        this.orCompareType  = COMPARE_EQ;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the "orFalse" attribute
    *** @return The "orFalse" attribute
    **/
    public String getOrFalse()
    {
        if ((this.orCompare != null) && this.orCompare.equalsIgnoreCase(BOOLEAN_FALSE)) {
            return this.orKey;
        } else {
            return null;
        }
    }

    /**
    *** Sets the "orFalse" attribute
    *** @param k  The "orFalse" attribute value
    **/
    public void setOrFalse(String k)
    {
        this.orKey          = k;
        this.orCompare      = BOOLEAN_FALSE;
        this.orCompareType  = COMPARE_EQ;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Session attribute for the specified key
    *** @param key  The attribute key
    *** @param dft  The default value
    *** @return The value for the specified key
    **/
    public String getAttributeValue(String key, String dft)
    {
        if (!StringTools.isBlank(key)) {
            ServletRequest request = super.pageContext.getRequest();
            RequestProperties rp = (RequestProperties)request.getAttribute(PARM_REQSTATE);
            if (rp != null) {
                String v = rp._getKeyValue(key, null);
                if (v != null) {
                    return v;
                }
            }
        }
        return dft;

    }

    // "StringTools.KeyValueMap" interface
    public String getKeyValue(String key, String arg, String dft)
    {
        return this.getAttributeValue(key, dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the "compare" type
    *** @return The "compare" type
    **/
    public String getCompare(String dft)
    {
        return !StringTools.isBlank(this.ifCompareType)? this.ifCompareType : dft;
    }

    /**
    *** Gets the "compare" type
    *** @return The "compare" type
    **/
    public String getCompare()
    {
        return this.ifCompareType;
    }
    
    /**
    *** Sets the "compare" type
    *** @param comp  The "compare" type
    **/
    public void setCompare(String comp)
    {
        this.ifCompareType = comp;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the comparison value
    *** @param dft  The default returned value if the comparison value has not been defined
    *** @return The comparison value
    **/
    public String getValue(String dft)
    {
        return !StringTools.isBlank(this.ifCompare)? this.ifCompare : dft;
    }

    /**
    *** Gets the comparison value
    *** @return The comparison value
    **/
    public String getValue()
    {
        return (this.ifCompare != null)? this.ifCompare : "";
    }
    
    /**
    *** Sets the comparison value
    *** @param val  The comparison value
    **/
    public void setValue(String val)
    {
        this.ifCompare = val;
    }
    
    /**
    *** Returns true if the attribute key matches the current comparison value, based on the
    *** comparison type.
    **/
    public boolean isMatch()
    {
        
        /* already initialized? */
        // NOTE: caching the 'match' value causes problems
        //if (this.isMatch >= 0) {
        //    return (this.isMatch == 1);
        //}

        /* key 'ifKey' */
        String ifKY = this.getIfKey();
        if (StringTools.isBlank(ifKY)) {
            // key not defined (always true)
            //this.isMatch = 1;
            return true;
        }

        /* check "if" comparison */
        String  ifCT  = this.getCompare(COMPARE_EQ).toLowerCase();
        String  ifCV  = this.getValue(BOOLEAN_TRUE);            // constant (not null)
        String  ifKV  = this.getAttributeValue(ifKY, null);     // variable (may be null)
        boolean match = this._compare(ifCT, ifCV, ifKV);
        
        /* check "and" comparison */
        String anKY = this.getAndKey();
        if (match && !StringTools.isBlank(anKY)) {
            // "if" match is true, also require "and" comparison
            String  anCT  = StringTools.blankDefault(this.andCompareType,COMPARE_EQ).toLowerCase();
            String  anCV  = StringTools.blankDefault(this.andCompare, BOOLEAN_TRUE);
            String  anKV  = this.getAttributeValue(anKY, null);    // variable (may be null)
            match = this._compare(anCT, anCV, anKV);
        }
        
        /* check "or" comparison */
        String orKY = this.getOrKey();
        if (!match && !StringTools.isBlank(orKY)) {
            // "if" && "and" is false, check for "or" comparison
            String  orCT  = StringTools.blankDefault(this.orCompareType,COMPARE_EQ).toLowerCase();
            String  orCV  = StringTools.blankDefault(this.orCompare,BOOLEAN_TRUE);
            String  orKV  = this.getAttributeValue(orKY, null);    // variable (may be null)
            match = this._compare(orCT, orCV, orKV);
        }
        
        //this.isMatch = match? 1 : 0;
        return match;

    }
    
    private boolean _compare(String ct, String cv, String kv)
    {
        // ct == CompareType
        // cv == CompareValue
        // kv == KeyValue
        boolean match = false;
        if (ct.equals(COMPARE_EQ)) {
            // compare equals
            match = (kv != null)?  kv.equalsIgnoreCase(cv) : false;
        } else
        if (ct.equals(COMPARE_NE)) {
            // compare not equals
            match = (kv != null)? !kv.equalsIgnoreCase(cv) : true;
        } else
        if (kv == null) {
            match = false;
        } else
        if (ct.equals(COMPARE_GT)) {
            // compare greater-than
            match = (StringTools.parseDouble(kv,0.0) >  StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_GE)) {
            // compare greater-than-or-equals-to
            match = (StringTools.parseDouble(kv,0.0) >= StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_LT)) {
            // compare less-than
            match = (StringTools.parseDouble(kv,0.0) <  StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_LE)) {
            // compare less-than-or-equals-to
            match = (StringTools.parseDouble(kv,0.0) <= StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_INSET)) {
            // compare "in" set
            match = ListTools.contains(StringTools.split(cv,','),kv);
        } else
        if (ct.equals(COMPARE_NOTINSET)) {
            // compare "not in" set
            match = !ListTools.contains(StringTools.split(cv,','),kv);
        } else
        if (ct.equals(COMPARE_DEFINED)) {
            // compare defined
            boolean def = StringTools.parseBoolean(cv,true);
            if (!RTConfig.hasProperty(kv)) {
                match = !def; // not defined
            } else 
            if (StringTools.isBlank(RTConfig.getString(kv,null))) {
                match = !def; // has blank value
            } else {
                match = def; // defined and non-blank
            }
        } else {
            // false
            match = false;
        }
        return match;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String VAR_ELSE = "<varElse/>";
    
    private String bodyContent = null;
    private String elseContent = null;

    private void setSavedBodyContent(String body)
    {
        int p = (body != null)? body.indexOf(VAR_ELSE) : -1; // "<varElse/>"
        if (p >= 0) {
            int b = p - 1;
            for (;(b >= 0) && (body.charAt(b) == ' '); b--);
            this.bodyContent = body.substring(0,b+1);
            int e = p + VAR_ELSE.length();
            for (;(e < body.length()) && (body.charAt(e) == ' ' ); e++);
            for (;(e < body.length()) && (body.charAt(e) == '\r'); e++);
            for (;(e < body.length()) && (body.charAt(e) == '\n'); e++);
            this.elseContent = body.substring(e);
        } else {
            this.bodyContent = body;
            this.elseContent = null;
        }
        /* ---
        Print.logInfo("BodyContent:\n"+this.bodyContent);
        if (!StringTools.isBlank(this.bodyContent) && Character.isLetter(this.bodyContent.charAt(0))) {
            int e = this.bodyContent.indexOf(KEY_END);
            if (e >= 0) {
                int s = this.bodyContent.indexOf(KEY_START);
                if ((s <= -1) || (s > e)) {
                    this.bodyContent = KEY_START + this.bodyContent;
                    Print.logInfo("Repaired broken Taglib BodyContent: " + this.bodyContent);
                }
            }
        }
        --- */
    }

    private String getSavedBodyContent()
    {
        return (this.bodyContent != null)? this.bodyContent : "";
    }

    private String getSavedElseContent()
    {
        return (this.elseContent != null)? this.elseContent : "";
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Tag handler override
    *** May return:
    ***     EVAL_BODY_INCLUDE (only if BodyTag interface NOT implemented)
    ***     EVAL_BODY_TAG (only if BodyTag interface IS implemented)
    ***     EVAL_BODY_BUFFERED
    ***     SKIP_BODY 
    **/
    public int doStartTag()
        throws JspTagException
    {
        if (this.hasElse() || this.isMatch()) {
            return EVAL_BODY_BUFFERED;
        } else {
            // no-match, do not process this tag-block
            return SKIP_BODY;
        }
    }
    
    /**
    *** Tag handler override
    *** May return:
    ***     EVAL_PAGE
    ***     SKIP_PAGE
    **/
    public int doEndTag()
        throws JspTagException
    {

        try {
            if (this.isMatch()) {
                String body = StringTools.replaceKeys(this.getSavedBodyContent(), 
                    this, null/*valueFilter*/,
                    KEY_START, KEY_END, ARG_DELIM, DFT_DELIM);
                super.pageContext.getOut().write(body);
            } else
            if (this.hasElse()) {
                String body = StringTools.replaceKeys(this.getSavedElseContent(), 
                    this, null/*valueFilter*/,
                    KEY_START, KEY_END, ARG_DELIM, DFT_DELIM);
                super.pageContext.getOut().write(body);
            }
        } catch (Throwable t) {
            if (t instanceof JspTagException) {
                throw (JspTagException)t;
            } else {
                throw new JspTagException(t);
            }
        }
        
        return EVAL_PAGE;
    }

    // ------------------------------------------------------------------------
        
    public void setBodyContent(BodyContent body)
    {
        super.setBodyContent(body);
    }
    
    /**
    *** Invoked before the body of the tag is evaluated but after body content is set
    **/
    public void doInitBody()
        throws JspException
    {
        // invoked after 'setBodyContent'
        super.doInitBody();
    }
    
    /**
    *** Invoked after body content is evaluated
    **/
    public int doAfterBody()
        throws JspException
    {
        // invoked after 'doInitBody'        
        this.setSavedBodyContent(this.getBodyContent().getString());
        return SKIP_BODY; // EVAL_BODY_TAG loops
    }

    // ------------------------------------------------------------------------

    /**
    *** Release resources
    **/
    public void release()
    {
        
        this.ifKey          = null;
        this.ifCompareType  = null;
        this.ifCompare      = null;
        
        this.andKey         = null;
        this.andCompareType = null;
        this.andCompare     = null;
        
        this.orKey          = null;
        this.orCompareType  = null;
        this.orCompare      = null;

        this.bodyContent    = null;
        
    }
    
    // ------------------------------------------------------------------------

}
