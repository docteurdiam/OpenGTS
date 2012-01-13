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
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/03/30  Martin D. Flynn
//     -Added access control
//  2007/06/03  Martin D. Flynn
//     -Removed 'setMenuDescription', 'setMenuHelp', setNavigationDescription' methods
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2009/09/23  Martin D. Flynn
//     -Added 'setNavigationTab(...)'
//  2011/04/01  Martin D. Flynn
//     -Fixed 'onclick' issue in Form_TextField
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.Version;
import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.ReportPresentation;

public abstract class WebPageAdaptor
    implements WebPage
{

    // ------------------------------------------------------------------------
    // Redefine global parameter constants

    public  static final String  PARM_PAGE                              = CommonServlet.PARM_PAGE;
    public  static final String  PARM_COMMAND                           = CommonServlet.PARM_COMMAND;
    public  static final String  PARM_ARGUMENT                          = CommonServlet.PARM_ARGUMENT;
    public  static final String  PARM_CONTENT                           = CommonServlet.PARM_CONTENT;
    
    public  static final String  SUBACL_SEPARATOR                       = ":";

    // ------------------------------------------------------------------------

    public  static final String  SORTTABLE_SORTKEY                      = ReportPresentation.SORTTABLE_SORTKEY;
    public  static final String  SORTTABLE_JS                           = ReportPresentation.SORTTABLE_JS;

    private static final boolean COMBOBOX_SHOW_DISABLED_IF_NOT_ENABLED  = true;
    
    // ------------------------------------------------------------------------

    /**
    *** Return true if the specified ID contains valid characters
    *** @param reqState  The session RequestProperties instance.
    *** @param propKey   The Domain 'private.xml' property indicating whether validation should occur.
    *** @param id        The id to validate
    *** @return True if the id is valid, false otherwise
    **/
    public static boolean isValidID(RequestProperties reqState, /*String propKey,*/ String id)
    {
        if (StringTools.isBlank(id)) {
            return false;
        } else {
            return id.equals(WebPageAdaptor.getFilteredID(reqState,/*propKey,*/id));
        }
    }

    /**
    *** Returns the specified id, with invalid characters filtered out.
    *** @param reqState  The session RequestProperties instance.
    *** @param propKey   The Domain 'private.xml' property indicating whether id filtering/validation 
    ***                  should occur.  Leading/trailing spaces will always be filtered out, regardless
    ***                  of the value of the property.
    *** @param id        The id to filter/validate.
    *** @return The filtered id
    **/
    public static String getFilteredID(RequestProperties reqState, /*String propKey,*/ String id)
    {
        if (id == null) {
            return "";
        } else {
            PrivateLabel privLabel = (reqState != null)? reqState.getPrivateLabel() : null;
            if ((privLabel == null) || privLabel.globalValidateIDs()) { //privLabel.getBooleanProperty(propKey,true)
                return StringTools.trim(AccountRecord.getFilteredID(id,false/*nullOnError*/,true/*lowerCase*/));
            } else {
                return StringTools.trim(id);
            }
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Return true if the specified ID contains valid characters
    *** @param privLabel The PrivateLabel instance.
    *** @param propKey   The Domain 'private.xml' property indicating whether validation should occur.
    *** @param id        The id to validate
    *** @return True if the id is valid, false otherwise
    **/
    public static boolean isValidID(PrivateLabel privLabel, /*String propKey,*/ String id)
    {
        if (StringTools.isBlank(id)) {
            return false;
        } else {
            return id.equals(WebPageAdaptor.getFilteredID(privLabel,/*propKey,*/id));
        }
    }

    /**
    *** Returns the specified id, with invalid characters filtered out.
    *** @param privLabel The PrivateLabel instance.
    *** @param propKey   The Domain 'private.xml' property indicating whether id filtering/validation 
    ***                  should occur.  Leading/trailing spaces will always be filtered out, regardless
    ***                  of the value of the property.
    *** @param id        The id to filter/validate.
    *** @return The filtered id
    **/
    public static String getFilteredID(PrivateLabel privLabel, /*String propKey,*/ String id)
    {
        if (id == null) {
            return "";
        } else
        if ((privLabel == null) || privLabel.globalValidateIDs()) { // privLabel.getBooleanProperty(propKey,true)) {
            return StringTools.trim(AccountRecord.getFilteredID(id,false/*nullOnError*/,true/*lowerCase*/));
        } else {
            return StringTools.trim(id);
        }
    }

    // ------------------------------------------------------------------------
    // helpers to encode URLs (for browsers with disabled cookies)

    public static String EncodeURL(RequestProperties reqState, URIArg url)
    {
        if (url == null) {
            return "";
        } else {
            return EncodeURL(reqState, url.toString());
        }
    }

    public static String EncodeURL(RequestProperties reqState, String urlStr)
    {
        if (StringTools.isBlank(urlStr)) {
            return "";
        } else
        if ((reqState == null) || reqState.getCookiesRequired()) {
            // Cookies required (must be enabled), no encoding necessary
            return urlStr;
        } else {
            // Cookies not required (may be disabled), encode session-id into URL
            HttpServletResponse response = reqState.getHttpServletResponse();
            return response.encodeURL(urlStr);
            // see also java.net.URLEncoder.encode(url, charSet)
        }
    }

    // ------------------------------------------------------------------------
    // helpers to create URLs
    // (these probably should be in "RequestProperties.java")

    public static URIArg MakeURL(String baseURI)
    {
        return MakeURL(baseURI, null, null, null);
    }

    public static URIArg MakeURL(String baseURI, String page)
    {
        return MakeURL(baseURI, page, null, null);
    }

    public static URIArg MakeURL(String baseURI, String page, String cmd)
    {
        return MakeURL(baseURI, page, cmd, null);
    }
    
    public static URIArg MakeURL(String baseURI, String page, String cmd, String arg)
    {
        URIArg uri = new URIArg(baseURI);
        if (page != null) { uri.addArg(PARM_PAGE    ,page); }
        if (cmd  != null) { uri.addArg(PARM_COMMAND ,cmd ); }
        if (arg  != null) { uri.addArg(PARM_ARGUMENT,arg ); }
        return uri;
    }
    
    public static String EncodeMakeURL(RequestProperties reqState, String baseURI)
    {
        return EncodeMakeURL(reqState, baseURI, null, null, null);
    }

    //public static String EncodeMakeURL(RequestProperties reqState, String baseURI, String page)
    //{
    //    return EncodeMakeURL(reqState, baseURI, page, null, null);
    //}

    //public static String EncodeMakeURL(RequestProperties reqState, String baseURI, String page, String cmd)
    //{
    //    return EncodeMakeURL(reqState, baseURI, page, cmd, null);
    //}

    public static String EncodeMakeURL(RequestProperties reqState, String baseURI, String page, String cmd, String arg)
    {
        URIArg uri = MakeURL(baseURI,page,cmd,arg);
        if (reqState.getPageFrameContentOnly()) { uri.addArg(PARM_CONTENT,1); }
        return EncodeURL(reqState,uri);
    }

    // ------------------------------------------------------------------------
    // helpers to create Notes

    public static String FormRow_TextNote(String description, String note)
    {
        StringBuffer sb = new StringBuffer();
        /* start row */
        sb.append("<tr>");
        /* description */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' valign='top' nowrap>");
        sb.append(StringTools.blankDefault(description,StringTools.HTML_SP));
        sb.append("</td>");
        /* note */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
        sb.append(StringTools.blankDefault(StringTools.replace(StringTools.trim(note),"\n",StringTools.HTML_BR),StringTools.HTML_SP));
        sb.append("</td>");
        /* end row */
        sb.append("</tr>");
        /* return result */
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // helpers to create separators

    public static String FormRow_Separator(String description)
    {
        String d = !StringTools.isBlank(description)? description : "&nbsp;";
        StringBuffer sb = new StringBuffer();
        /* start row */
        sb.append("<tr>");
        /* description */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' valign='top' nowrap>"+d+"</td>");
        /* separator */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'><hr></td>");
        /* end row */
        sb.append("</tr>");
        /* return result */
        return sb.toString();
    }

    public static String FormRow_Separator()
    {
        StringBuffer sb = new StringBuffer();
        /* start row */
        sb.append("<tr>");
        /* separator */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"'><hr></td>");
        /* separator */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'><hr></td>");
        /* end row */
        sb.append("</tr>");
        /* return result */
        return sb.toString();
    }

    public static String FormRow_SubSeparator()
    {
        StringBuffer sb = new StringBuffer();
        /* start row */
        sb.append("<tr>");
        /* separator */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"'><hr style='border:0; height:1px; color:#BBBBBB; background-color:#BBBBBB;'></td>");
        /* separator */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>&nbsp;</td>");
        /* end row */
        sb.append("</tr>");
        /* return result */
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // helpers to create Form TextFields

    private static String escapeValue(String val)
    {
        String v = StringTools.htmlFilterValue(val); // does not do single quotes (TODO - actually it does now)
        return v; // StringTools.replace(v, "'", StringTools.HTML_APOS);
    }

    public static String Form_TextField(
        String name, 
        boolean editable, 
        String value, int size, int maxLen)
    {
        return Form_TextField(null/*id*/, name, editable, 
            value, null/*onclick*/, 
            size, maxLen, 
            null/*cssClass*/);
    }

    public static String Form_TextField(
        String id, String name, 
        boolean editable, 
        String value,
        int size, int maxLen)
    {
        return Form_TextField(id, name, editable, 
            value, null/*onclick*/, 
            size, maxLen, 
            null/*cssClass*/);
    }

    public static String Form_TextField(
        String id, String name, 
        boolean editable, 
        String value, String onclick,
        int size, int maxLen)
    {
        return Form_TextField(id, name, editable, 
            value, onclick, // fixed 2011/03/16
            size, maxLen, 
            null/*cssClass*/);
    }
    
    public static String Form_TextField(
        String id, String name, 
        boolean editable, 
        String value, String onclick,
        int size, int maxLen, 
        String cssClass)
    {
        StringBuffer sb = new StringBuffer();
        if (maxLen < size) { maxLen = size; }
        /* input class */
        String inputClass = null;
        if (!StringTools.isBlank(cssClass)) {
            inputClass = cssClass;
        } else
        if (editable && !StringTools.isBlank(onclick)) {
            inputClass = CommonServlet.CSS_TEXT_ONCLICK;
        } else
        if (editable) {
            inputClass = CommonServlet.CSS_TEXT_INPUT;
        } else {
            inputClass = CommonServlet.CSS_TEXT_READONLY;
        }
        /* form input */
        sb.append("<input");
        if (!StringTools.isBlank(id)) {
            sb.append(" id='"+id+"'");
        }
        sb.append(" class='" + inputClass + "'");
        sb.append(" type='text'");
        sb.append(" name='" + name + "'");
        sb.append(" value='" + escapeValue(value) + "'");
        if (!editable) { 
            sb.append(" readonly"); 
        }
        if ((size > 0) && (maxLen > 0)) {
            sb.append(" size='" + size + "'");
            sb.append(" maxlength='" + maxLen + "'");
        }
        if (editable) {
            // The follow attempts to consume the "Enter" key and prevent it
            // from submitting the form.
            // - The following still causes Internet Explorer (IE) to submit the form!!  
            //   String js = "return !isEnterKeyPressed(event);";
            // - These seems to work OK on IE (and do no harm on Firefox, etc)
            //   String js = "if (isEnterKeyPressed(event)) { event.returnValue=false; event.cancel = true; return false; } else { return true; }";
            //   String js = "if (isEnterKeyPressed(event)) { event.keyCode=0; return false; } else { return true; }";
            String js = "return ignoreEnterKeyPress(event);";
            //sb.append(" onKeyDown=\""+js+"\"");
            sb.append(" onKeyPress=\""+js+"\"");
        }
        if (editable && !StringTools.isBlank(onclick)) {
            sb.append(" onclick=\""+onclick+"\"");
        }
        sb.append(">");
        /* return result */
        return sb.toString();
    }

    public static String FormRow_TextField(
        String key, boolean editable, 
        String description, String value, 
        int size, int maxLen)
    {
        return FormRow_TextField(key, editable, description, value, size, maxLen, null/*trailingText*/);
    }

    public static String FormRow_TextField(
        String key, boolean editable,
        String description, String value, 
        int size, int maxLen, 
        String trailingHtml)
    {
        return FormRow_TextField(null/*id*/, key, editable, description, value, null/*onclick*/, size, maxLen, trailingHtml);
    }
    
    public static String FormRow_TextField(
        String id, String key, boolean editable, 
        String description, String value, 
        String onclick, 
        int size, int maxLen, 
        String trailingHtml)
    {
        StringBuffer sb = new StringBuffer();
        if (maxLen < size) { maxLen = size; }
        /* start row */
        sb.append("<tr>");
        /* description */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+description+"</td>");
        /* form input */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
        sb.append(Form_TextField(id, key, editable, value, onclick, size, maxLen));
        if (!StringTools.isBlank(trailingHtml)) {
            sb.append(" ").append(trailingHtml);
        }
        sb.append("</td>");
        /* end row */
        sb.append("</tr>");
        /* return result */
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // helpers to create Form TextArea

    public static String Form_TextArea(String name, boolean editable, 
        String value, int rows, int cols)
    {
        return Form_TextArea(null, name, editable, value, rows, cols);
    }

    public static String Form_TextArea(String id, String name, boolean editable, 
        String value, int rows, int cols)
    {
        StringBuffer sb = new StringBuffer();
        String inputClass = editable? CommonServlet.CSS_TEXTAREA_INPUT : CommonServlet.CSS_TEXTAREA_READONLY;
        sb.append("<textArea");
        if (!StringTools.isBlank(id)) {
            sb.append(" id='"+id+"'");
        }
        sb.append(" class='" + inputClass + "'");
        sb.append(" name='" + name + "'");
        if (!editable) {
            sb.append(" readonly"); 
        }
        sb.append(" rows='" + rows + "'");
        sb.append(" cols='" + cols + "'");
        if (editable) { 
            //sb.append(" onkeypress=\"return !isEnterKeyPressed(event);\"");
        }
        sb.append(" nowrap>");
        sb.append(value);
        sb.append("</textArea>");
        /* return result */
        return sb.toString();
    }

    public static String FormRow_TextArea(String key, boolean editable, String desc, String value, int rows, int cols)
    {
        return FormRow_TextArea(null, key, editable, desc, value, rows, cols);
    }

    public static String FormRow_TextArea(String id, String key, boolean editable, String desc, String value, int rows, int cols)
    {
        StringBuffer sb = new StringBuffer();
        /* start row */
        sb.append("<tr>");
        /* description */
        if (StringTools.isBlank(desc)) { 
            desc = "&nbsp;"; 
        }
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' valign='top' nowrap>"+desc+"</td>");
        /* form input */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_TEXTAREA+"'>");
        sb.append(Form_TextArea(id, key, editable, value, rows, cols));
        sb.append("</td>");
        /* end row */
        sb.append("</tr>");
        /* return result */
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // helpers to create Form ComboBoxes

    public static String Form_ComboBox(String id, String key, boolean editable, ComboMap map, String selKey, String onchange, int size)
    {
        ComboOption opt = ComboMap.getComboOption(map, selKey);
        return Form_ComboBox(id, key, editable, map, opt, onchange, size);
    }

    public static String Form_ComboBox(String id, String key, boolean editable, ComboMap map, ComboOption selKey, String onchange, int size)
    {

        if (editable || COMBOBOX_SHOW_DISABLED_IF_NOT_ENABLED) {
            
            /* accumulate html */
            StringBuffer sb = new StringBuffer();

            /* begin form select */
            sb.append("<select");
            if (!StringTools.isBlank(id)) {
                sb.append(" id='" + id + "'");
            }
            sb.append(" class='"+ CommonServlet.CSS_ADMIN_COMBO_BOX + "'");
            sb.append(" name='" + key + "'");
            if (size > 0) {
                sb.append(" style='width:"+(size*8)+"px;'");
            }
            if (editable) {
                if (!StringTools.isBlank(onchange)) {
                    sb.append(" onchange=\"" + onchange + "\"");
                }
            } else {
                sb.append(" disabled");
            }
            sb.append(">");

            /* normalize selection */
            if (selKey == null) {
                selKey = new ComboOption("");
            }
    
            /* selectable options */
            if (ListTools.isEmpty(map)) { 
                // 'list' should never be empty
                Print.logWarn("ComboBox 'list' is empty: key=" + key);
                sb.append("<option value='"+selKey.getKey()+"' selected>" + FilterText(selKey.getDescription()) + "</option>");
            } else {
                for (String comboKey : map.keySet()) {
                    String comboDesc = map.get(comboKey); // Localized description
                    if (StringTools.isBlank(comboDesc)) { comboDesc = comboKey; }
                    String isSel = selKey.isMatch(comboKey)? " selected" : "";
                    sb.append("<option value='" + comboKey + "' " + isSel + ">" + FilterText(comboDesc) + "</option>");
                }
            }
    
            /* end form select */
            sb.append("</select>");
        
            /* return result */
            return sb.toString();

        } else {
            
            String dispValue = (selKey != null)? selKey.getDescription() : StringTools.trim(map.get(""));
            return Form_TextField(id, key, false, dispValue, null, size, size);
            
        }

    }

    public static String Form_ComboBox(String id, String key, boolean editable, ComboMap map, String selKey, String onchange)
    {
        ComboOption opt = ComboMap.getComboOption(map, selKey);
        return Form_ComboBox(id, key, editable, map, opt, onchange, -1);
    }

    public static String Form_ComboBox(String id, String key, boolean editable, ComboMap map, ComboOption sel, String onchange)
    {
        return Form_ComboBox(id, key, editable, map, sel, onchange, -1);
    }

    public static String FormRow_ComboBox(String key, boolean editable, String desc, String selKey, ComboMap map, String onchange, int size)
    {
        ComboOption opt = ComboMap.getComboOption(map, selKey);
        return FormRow_ComboBox(null, key, editable, desc, opt, map, onchange, size, null);
    }

    public static String FormRow_ComboBox(String key, boolean editable, String desc, ComboOption sel, ComboMap map, String onchange, int size)
    {
        return FormRow_ComboBox(null, key, editable, desc, sel, map, onchange, size, null);
    }

    public static String FormRow_ComboBox(String key, boolean editable, String desc, String selKey, ComboMap map, String onchange, int size, String trailingHtml)
    {
        ComboOption opt = ComboMap.getComboOption(map, selKey);
        return FormRow_ComboBox(null, key, editable, desc, opt, map, onchange, size, trailingHtml);
    }

    public static String FormRow_ComboBox(String id, String key, boolean editable, String desc, String selKey, ComboMap map, String onchange, int size, String trailingHtml)
    {
        ComboOption opt = ComboMap.getComboOption(map, selKey);
        return FormRow_ComboBox(id, key, editable, desc, opt, map, onchange, size, trailingHtml);
    }

    public static String FormRow_ComboBox(String key, boolean editable, String desc, ComboOption sel, ComboMap map, String onchange, int size, String trailingHtml)
    {
        return FormRow_ComboBox(null, key, editable, desc, sel, map, onchange, size, trailingHtml);
    }

    public static String FormRow_ComboBox(String id, String key, boolean editable, String desc, ComboOption sel, ComboMap map, String onchange, int size, String trailingHtml)
    {
        
        /* editable */
        if (editable || COMBOBOX_SHOW_DISABLED_IF_NOT_ENABLED) {
            
            StringBuffer sb = new StringBuffer();
            /* start row */
            sb.append("<tr>");
            if (StringTools.isBlank(desc)) { 
                desc = "&nbsp;"; 
            }
            sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+desc+"</td>");
            /* form input */
            sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
            sb.append(Form_ComboBox(id, key, editable, map, sel, onchange, size));
            if (!StringTools.isBlank(trailingHtml)) {
                sb.append(" ").append(trailingHtml);
            }
            sb.append("</td>");
            /* end row */
            sb.append("</tr>");
            /* return result */
            return sb.toString();
            
        } else {

            // read-only text field
            if (sel != null) {
                String d = sel.getDescription();
                return FormRow_TextField(key, false, desc, d, size, size, trailingHtml);
            } else {
                String dispValue = StringTools.trim(map.get(""));
                return FormRow_TextField(key, false, desc, dispValue, size, size, trailingHtml);
            }

        }
        
    }

    // ------------------------------------------------------------------------
    // helpers to create Form CheckBoxes
    // IMPORTANT NOTE:
    //  - Checked checkboxes will appear with value == "on".
    //  - Unchecked checkboxes WILL NOT APPEAR IN THE ARGUMENT LIST!

    public static String Form_CheckBox(String id, String name, boolean editable, boolean checked, String tooltip, String onchange)
    {
        StringBuffer sb = new StringBuffer();
        /* form input */
        sb.append("<input type='checkbox' style='margin: 0px 0px 0px 0px;'");
        if (!StringTools.isBlank(id)) {
            sb.append(" id='" + id + "'");
        }
        if (!StringTools.isBlank(name)) {
            sb.append(" name='" + name + "'");
        }
        if (!editable) {
            sb.append(" disabled");
        }
        if (checked) {
            sb.append(" checked");
        }
        if (!StringTools.isBlank(onchange)) {
            //sb.append(" onchange=\"" + onchange + "\"");   <-- does not work on IE.
            sb.append(" onclick=\"" + onchange + "\"");
        }
        if (!StringTools.isBlank(tooltip)) {
            sb.append(" title=\""+tooltip+"\"");
        }
        sb.append(">");
        /* return result */
        return sb.toString();
    }
    
    public static String FormRow_CheckBox(String desc, String id, String name, boolean editable, boolean checked, String tooltip, String onchange)
    {
        StringBuffer sb = new StringBuffer();
        /* start row */
        sb.append("<tr>");
        /* description */
        if (StringTools.isBlank(desc)) { 
            desc = "&nbsp;"; 
        }
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+desc+"</td>");
        /* form input */
        sb.append("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
        sb.append(Form_CheckBox(id, name, editable, checked, tooltip, onchange));
        sb.append("</td>");
        /* end row */
        sb.append("</tr>");
        /* return result */
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    protected static String JS_alert(boolean inclJS, String msg)
    {
        if (!StringTools.isBlank(msg)) {
            StringBuffer sb = new StringBuffer();
            if (inclJS) { sb.append("javascript:"); }
            String m = StringTools.replace(msg, "\"", "'"); // filter out quotes
            m = StringTools.replace(m, "\n"               , "\\n"); // encode newline
            m = StringTools.replace(m, StringTools.HTML_br, "\\n"); // encode newline
            m = StringTools.replace(m, StringTools.HTML_BR, "\\n"); // encode newline
            sb.append("alert(&quot;").append(m).append("&quot;);");
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------

    protected static String Onclick_ConfirmDelete(Locale locale)
    {
        I18N i18n = I18N.getI18N(WebPageAdaptor.class, locale);
        String confirmDel = i18n.getString("WebPageAdapter.confirmDelete",
            "Are you sure you want to delete the selected item?");
        return "onclick=\"return confirm('"+confirmDel+"');\"";
    }

    protected static String Onclick_ConfirmSelected(Locale locale, String itemOperation)
    {
        I18N i18n = I18N.getI18N(WebPageAdaptor.class, locale);
        String confirmOp = i18n.getString("WebPageAdapter.confirmSelected",
            "Are you sure you want to {0} the selected item?", StringTools.trim(itemOperation));
        return "onclick=\"return confirm('"+confirmOp+"');\"";
    }

    protected static String Onclick_ConfirmOperation(Locale locale, String operation)
    {
        I18N i18n = I18N.getI18N(WebPageAdaptor.class, locale);
        String confirmOp = i18n.getString("WebPageAdapter.confirmOperation",
            "Are you sure you want to {0}?", StringTools.trim(operation));
        return "onclick=\"return confirm('"+confirmOp+"');\"";
    }

    // ------------------------------------------------------------------------

    /* replace blank strings with html "&nbsp;" */
    public static String FilterText(String s)
    {
        // Don't use StringTools.isBlank(...) - spaces are significant
        return ((s==null)||s.equals(""))? StringTools.HTML_SP : StringTools.htmlFilterText(s);
    }

    /* replace blank strings with html "&nbsp;" */
    public static String FilterValue(String s)
    {
        return StringTools.isBlank(s)? "" : StringTools.htmlFilterValue(s);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private PrivateLabel    privateLabel    = null;

    private String          baseURI         = null;

    private String          jspURI          = null;
    
    private String          cssDir          = "css";

    private MenuGroup       menuGroup       = null;
    private String          iconImageURI    = null;
    private String          btnImageURI     = null;
    private String          btnImageALT     = null;

    private String          pageName        = null;
    private String          pageNavList[]   = null;
    private String          pageNavHTML     = null;

    private String          aclName         = null;

    private boolean         loginRequired   = true;
    private int             sortIndex       = -1;
    
    private I18N.Text       menuDesc        = null;
    private I18N.Text       menuHelp        = null;
    private I18N.Text       navDesc         = null;
    private I18N.Text       navTab          = null;

    private RTProperties    pageProperties  = null;

    public WebPageAdaptor()
    {
        super();
        //Example initialization:
        // this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        // this.setPageName(PAGE_XXXXXXXX);
        // this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        // this.setLoginRequired(true);
    }
    
    /* post initialization */
    protected void postInit()
    {
        // override
    }

    // ------------------------------------------------------------------------

    public boolean getIsEnabled()
    {
        // override to change behavior
        return true;
    }

    // ------------------------------------------------------------------------

    public String getBaseURI()
    {
        return this.baseURI;
    }
    
    public void setBaseURI(String baseURI)
    {
        this.baseURI = baseURI;
    }
    
    public URIArg getPageURI()
    {
        return this.getPageURI(null, null);
    }
    
    public URIArg getPageURI(String command)
    {
        return this.getPageURI(command, null);
    }
    
    public URIArg getPageURI(String command, String cmdArg)
    {
        return MakeURL(this.getBaseURI(), this.getPageName(), command, cmdArg);
    }

    // ------------------------------------------------------------------------

    public String getPageName()
    {
        return this.pageName;
    }
    
    public void setPageName(String pageName)
    {
        this.pageName = pageName;
    }

    // ------------------------------------------------------------------------

    public PrivateLabel getPrivateLabel()
    {
        return this.privateLabel;
    }
    
    public void setPrivateLabel(PrivateLabel privLabel)
    {
        this.privateLabel = privLabel;
    }

    // ------------------------------------------------------------------------

    public String getJspURI()
    {
        return this.jspURI;
    }

    public void setJspURI(String jspURI)
    {
        this.jspURI = jspURI;
    }

    // ------------------------------------------------------------------------

    public String getTarget()
    {
        return "_self";
    }
    
    public PixelDimension getWindowDimension()
    {
        return null;
    }

    // ------------------------------------------------------------------------

    public String getCssDirectory()
    {
        return this.cssDir;
    }
    
    public void setCssDirectory(String cssDir)
    {
        this.cssDir = cssDir;
    }

    // ------------------------------------------------------------------------

    public String getPageNavigationHTML(RequestProperties reqState)
    {
        return this.getPageNavigationHTML(reqState, true);
    }
    
    public String getPageNavigationHTML(RequestProperties reqState, boolean reInit)
    {
        if ((reInit || (this.pageNavHTML == null)) && this.hasPageNavigation()) {
            String pageNavNames[] = this.getPageNavigation();
            PrivateLabel privLbl = this.getPrivateLabel();
            if (privLbl == null) {
                Print.logWarn("Page Navigation PrivateLabel undefined: " + this.getPageName());
                this.pageNavHTML = "";
            } else {
                // <a href="/track/Track?page=menu.top">Main Menu</a> | 
                // <a href="/track/Track?page=login">Logout</a>&nbsp;&nbsp;
                StringBuffer sb = new StringBuffer();
                for (int i = pageNavNames.length - 1; i >= 0; i--) {
                    String pageName = pageNavNames[i];
                    WebPage page = privLbl.getWebPage(pageName);
                    if (page != null) {
                        if (sb.length() > 0) { sb.append(" | "); }
                        String uri  = WebPageAdaptor.EncodeURL(reqState, page.getPageURI());
                        String desc = page.getNavigationDescription(reqState);
                        sb.append("<a href='"+uri+"'>").append(desc).append("</a>");
                    } else {
                        String vers   = Version.getVersion();
                        String plName = privLbl.getName();
                        Print.logWarn("Page not found: " + pageName + " [v="+vers+", pl="+plName+"]");
                        //Print.logStackTrace("Page not found: " + pageName + " [v="+vers+", pl="+plName+"]");
                    }
                }
                this.pageNavHTML = sb.toString();
            }
            //Print.logStackTrace("Setting Navigation HTML: " + this.pageNavHTML);
        } else {
            //Print.logStackTrace("Returning Previous Navigation HTML: " + this.pageNavHTML);
        }
        return this.pageNavHTML;
    }

    // ------------------------------------------------------------------------

    public void setPageNavigation(String pageNav[])
    {
        this.pageNavList = pageNav;
    }
    
    public boolean hasPageNavigation() 
    {
        return !ListTools.isEmpty(this.getPageNavigation());
    }   

    public String[] getPageNavigation()
    {
        if (this.pageNavList == null) { // check for 'null'
            this.setPageNavigation(new String[] { this.getPageName() });
        }
        return this.pageNavList;
    }

    // ------------------------------------------------------------------------

    public void setLoginRequired(boolean required)
    {
        this.loginRequired = required;
    }

    public boolean isLoginRequired()
    {
        return this.loginRequired;
    }

    // ------------------------------------------------------------------------

    public boolean isOkToDisplay(RequestProperties reqState)
    {
        return true;
    }

    // ------------------------------------------------------------------------

    public void setAclName(String name)
    {
        if (StringTools.isBlank(name) || name.equals("*")) {
            this.aclName = null;
        } else {
            this.aclName = name;
        }
    }

    public String getAclName()
    {
        return this.aclName;
    }

    public String getAclName(String subAcl)
    {
        return AclEntry.CreateAclName(this.getAclName(), subAcl);
    }

    public String[] getChildAclList()
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /* true if this page iis for the system admin only */
    public boolean systemAdminOnly()
    {
        return false;
    }

    // ------------------------------------------------------------------------

    public void setMenuGroup(MenuGroup mg)
    {
        this.menuGroup = mg;
    }
    
    public MenuGroup getMenuGroup(RequestProperties reqState)
    {
        return this.menuGroup;
    }

    // ------------------------------------------------------------------------

    public String encodePageURL(RequestProperties reqState)
    {
        return this.encodePageURL(reqState, null, null);
    }

    public String encodePageURL(RequestProperties reqState, String command)
    {
        return this.encodePageURL(reqState, command, null);
    }

    public String encodePageURL(RequestProperties reqState, String command, String cmdArg)
    {
        URIArg uri = this.getPageURI(command, cmdArg);
        if (reqState.getPageFrameContentOnly()) { uri.addArg(PARM_CONTENT,1); }
        return EncodeURL(reqState,uri);
    }

    // ------------------------------------------------------------------------

    public abstract String getMenuName(RequestProperties reqState);

    // ------------------------------------------------------------------------

    public String getMenuIconImage()
    {
        return this.iconImageURI;
    }
    
    public void setMenuIconImage(String iconURI)
    {
        if (StringTools.isBlank(iconURI) || iconURI.startsWith("!")) {
            this.iconImageURI = null;
        } else {
            this.iconImageURI = iconURI;
        }
    }

    // ------------------------------------------------------------------------

    public String getMenuButtonImage()
    {
        return this.btnImageURI;
    }
    
    public void setMenuButtonImage(String btnURI)
    {
        if (StringTools.isBlank(btnURI) || btnURI.startsWith("!")) {
            this.btnImageURI = null;
        } else {
            this.btnImageURI = btnURI;
        }
    }

    // ------------------------------------------------------------------------

    public String getMenuButtonAltImage()
    {
        return this.btnImageALT;
    }
    
    public void setMenuButtonAltImage(String btnURI)
    {
        if (StringTools.isBlank(btnURI) || btnURI.startsWith("!")) {
            this.btnImageALT = null;
        } else {
            this.btnImageALT = btnURI;
        }
    }

    // ------------------------------------------------------------------------

    public void setMenuDescription(I18N.Text menuDesc)
    {
        this.menuDesc = menuDesc;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        if (this.menuDesc != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.menuDesc.toString(locale);
        } else {
            return "";
        }
    }
    
    protected String _getMenuDescription(RequestProperties reqState, String dft)
    {
        if (this.menuDesc != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.menuDesc.toString(locale);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public void setMenuHelp(I18N.Text menuHelp)
    {
        this.menuHelp = menuHelp;
    }

    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        if (this.menuHelp != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.menuHelp.toString(locale);
        } else {
            return "";
        }
    }
    
    protected String _getMenuHelp(RequestProperties reqState, String dft)
    {
        if (this.menuHelp != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.menuHelp.toString(locale);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public void setNavigationDescription(I18N.Text navDesc)
    {
        this.navDesc = navDesc;
    }

    public String getNavigationDescription(RequestProperties reqState)
    {
        if (this.navDesc != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.navDesc.toString(locale);
        } else {
            return "";
        }
    }
    
    protected String _getNavigationDescription(RequestProperties reqState, String dft)
    {
        if (this.navDesc != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.navDesc.toString(locale);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public void setNavigationTab(I18N.Text navTab)
    {
        this.navTab = navTab;
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        if (this.navTab != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.navTab.toString(locale);
        } else {
            return this.getNavigationDescription(reqState);
        }
    }
   
    protected String _getNavigationTab(RequestProperties reqState, String dft)
    {
        if (this.navTab != null) {
            Locale locale = (reqState != null)? reqState.getLocale() : null;
            return this.navTab.toString(locale);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public void setProperties(RTProperties rtp)
    {
        this.pageProperties = rtp;
        //if (this.pageProperties != null) {
        //    this.pageProperties.logProperties(this.getPageName() + " Page Properties");
        //}
    }
    
    public RTProperties getProperties()
    {
        if (this.pageProperties == null) {
            this.pageProperties = new RTProperties();
            this.pageProperties.setPropertySeparatorChar(';');
        }
        return this.pageProperties;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public abstract void writePage(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException;

    // ------------------------------------------------------------------------

    public static void writeCssLink(
        PrintWriter out, 
        final RequestProperties reqState, 
        String cssFileName, String cssFileDir)
        throws IOException
    {
        PrivateLabel privLabel = (reqState != null)? reqState.getPrivateLabel() : RequestProperties.NullPrivateLabel;
        boolean isAbsolute = URIArg.isAbsoluteURL(cssFileName);

        /* check for including expanded CSS files */
        if (!isAbsolute && (reqState != null) && reqState.getEncodeEMailHTML()) {

            /* read contents of locally defined file */
            File rootDir = RTConfig.getServletContextPath(); // may return null
            if (rootDir != null) {
                String cssDir = !StringTools.isBlank(cssFileDir)? cssFileDir : StringTools.trim(privLabel.getCssDirectory());
                if (cssDir.startsWith("/")) {
                    cssDir = cssDir.substring(1);
                }
                String cssRelPath = cssDir + "/" + cssFileName;
                File cssAbsPath = new File(rootDir, cssRelPath);
                if (!cssAbsPath.isFile()) {
                    cssRelPath = PrivateLabel.DEFAULT_CSS_DIR + "/" + cssFileName;
                    cssAbsPath = new File(rootDir, cssRelPath);
                }
                if (cssAbsPath.isFile()) {
                    String css = StringTools.toStringValue(FileTools.readFile(cssAbsPath));
                    if (!StringTools.isBlank(css)) {
                        out.write("<!-- Inline CSS file: " + cssFileName + " -->\n");
                        out.write("<style type='text/css'>\n");
                        out.write(css);
                        out.write("\n");
                        out.write("</style>\n");
                        return;
                    } else {
                        // file is blank?
                    }
                } else {
                    // file not found
                    Print.logWarn("CSS file not found: " + cssAbsPath);
                }
            } else {
                // context path root directory not found
            }
            // continue below

        }
        
        /* default CSS link */
        out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
        if (isAbsolute) {
            out.write(cssFileName);
        } else {
            out.write(privLabel.resolveCssFile(cssFileName,cssFileDir));
        }
        out.write("\"/>\n");
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected boolean SubmitMatch(String value, String expect)
    {
        if (StringTools.isBlank(value)) {
            // no value specified
            return false;
        } else
        if (value.equals(expect)) {
            // value matches expectation exactly
            return true;
        } else
        if (StringTools.isBlank(expect)) {
            // test for non-blank value only
            return true;
        } else {
            // value is non-blank, as-is expectation
            return true; // only return true for unique 'submit' names
        }
    }

}
