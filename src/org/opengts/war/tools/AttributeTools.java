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
//  2007/07/14  Martin D. Flynn
//     -Added method 'getMatchingKeys(...)'
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;
import java.math.BigInteger;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

public class AttributeTools
{

    // ------------------------------------------------------------------------

    /* ignore property key case */
    private static final boolean    IGNORE_CASE             = true;

    // ------------------------------------------------------------------------

    /* the RTProperties attribute key */
    public  static final String     ATTR_RTP                = "rtp_";   // ?rtp_=dfsdfsdf
    public  static final String     ATTR_MULTIPART          = "multi_"; // "multipart/form-data"

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Session sequence ID

    private static final String     SessionSequenceKey      = "SequenceID";
    private static final Object     SessionSequenceLock     = new Object();
    private static       int        SessionSequenceValue    = 0;

    /**
    *** Saves the session sequence ID into the current session
    **/
    private static int _SaveSessionSequence(HttpServletRequest request)
    {
        int seq;
        synchronized (SessionSequenceLock) {
            SessionSequenceValue = (SessionSequenceValue + 1) & 0x7FFFFFFF;
            seq = SessionSequenceValue;
        }
        AttributeTools.setSessionInt(request, SessionSequenceKey, seq);
        return seq;
    }

    /**
    *** Returns the current session sequence ID
    **/
    public static int GetSessionSequence(HttpServletRequest request)
    {
        int seq = AttributeTools.getSessionInt(request, SessionSequenceKey, 0);
        if (seq <= 0) {
            // session sequence ID not yet defined
            seq = _SaveSessionSequence(request);
        }
        return seq;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Print headers
    **/
    public static void printHeaders(HttpServletRequest req, String title)
    {
        if (req != null) {
            Print.logInfo("Headers: " + title);
            for (Enumeration h = req.getHeaderNames(); h.hasMoreElements();) {
                String key = (String)h.nextElement();
                for (Enumeration v = req.getHeaders(key); v.hasMoreElements();) {
                    String val = (String)v.nextElement();
                    Print.logInfo(" " + key + " ==> " + val);
                }
            }
        }
    }
    
    /**
    *** Print Paramewters
    **/
    public static void printParameters(HttpServletRequest req, String title)
    {

        /* invalid request */
        if (req == null) {
            return;
        }

        /* header */
        if (title == null) {
            Print.logInfo("Parameters:");
        } else {
            Print.logInfo(title);
        }

        /* standard parameters */
        for (Enumeration e = req.getParameterNames(); e.hasMoreElements();) {
            String key = (String)e.nextElement();
            String val = req.getParameter(key);
            Print.logInfo("[standard] " + key + "=" + val);
        }

        /* RTP parameters */
        RTProperties rtp = (RTProperties)req.getAttribute(ATTR_RTP);
        if (rtp != null) {
            for (Object key : rtp.getPropertyKeys()) {
                Object val = rtp.getProperty(key,null);
                Print.logInfo("[rtp] " + key + "=" + val);
            }
        }

        /* Multipart form paramters? */
        RTProperties multi = (RTProperties)req.getAttribute(ATTR_MULTIPART);
        if (multi != null) {
            for (Object key : multi.getPropertyKeys()) {
                Object val = multi.getProperty(key,null);
                if (val instanceof MimePart) {
                    MimePart mp = (MimePart)val;
                    Print.logInfo("[multi] MimePart: " + key);
                    for (String mpKey : mp.keySet()) {
                        Object mpVal = mp.get(mpKey);
                        if (mpVal instanceof byte[]) {
                            byte b[] = (byte[])mpVal;
                            int maxLen = 20;
                            if (b.length > maxLen) {
                                StringBuffer sb = StringTools.toHexString(b,0,maxLen,null);
                                Print.logInfo("    ==> " + mpKey + "=0x" + sb + "...");
                            } else {
                                String s = StringTools.toHexString(b);
                                Print.logInfo("    ==> " + mpKey + "=0x" + s);
                            }
                        } else {
                            Print.logInfo("    ==> " + mpKey + "=" + mpVal);
                        }
                    }
                } else {
                    Print.logInfo("[multi] " + key + "=" + val);
                }
            }
        }

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final int    MAX_BINARY_SIZE      = 2 * 1024 * 1024;
    
    public static final String MIMEPART_NAME        = "mimepart.name";
    public static final String MIMEPART_FILENAME    = "mimepart.filename";
    public static final String MIMEPART_FILE        = "mimepart.file";
    public static final String MIMEPART_STRING      = "mimepart.string";
    public static final String MIMEPART_BYTES       = "mimepart.bytes";
    public static final String MIMEPART_INCOMPLETE  = "mimepart.incomplete";
    
    public static class MimePart
        extends HashMap<String,Object>
    {
        public MimePart() {
            super();
        }
        public String getString(String key, String dft) {
            Object obj = super.get(key);
            return (obj != null)? obj.toString() : dft;
        }
        public byte[] getByteArray(String key, byte dft[]) {
            Object obj = super.get(key);
            return (obj instanceof byte[])? (byte[])obj : dft;
        }
    }

    /**
    *** Parse "multipart/form-data" paramters
    **/
    public static boolean parseMultipartFormData(HttpServletRequest request)
    {
        try {
            AttributeTools._parseMultipartFormData(request, MAX_BINARY_SIZE, null);
            return true;
        } catch (IOException ioe) {
            Print.logError("Unexpected IOException: " + ioe);
            return false;
        }
    }
    
    /**
    *** Get MultiPart Mime parameters 
    **/
    public static RTProperties getMultipartProperties(HttpServletRequest request)
    {
        return (RTProperties)request.getAttribute(ATTR_MULTIPART);
    }
    
    /**
    *** Return true if multi-part form data is present
    **/
    public static boolean hasMultipartFormData(HttpServletRequest request)
    {
        String contentType   = AttributeTools.getContentType(request);
        String multiBoundary = HTMLTools.getContentMultipartBoundary(contentType);
        return !StringTools.isBlank(multiBoundary);
    }
    
    /**
    *** Parse "multipart/form-data" paramters
    **/
    public static boolean parseMultipartFormData(HttpServletRequest request, int maxLen, File fileSaveDir)
    {
        try {
            AttributeTools._parseMultipartFormData(request, maxLen, fileSaveDir);
            return true;
        } catch (IOException ioe) {
            Print.logError("Unexpected IOException: " + ioe);
            return false;
        }
    }

    /**
    *** Parse "multipart/form-data" paramters.  Sets "multi_" attribute in request containing RTProperies
    *** instance containing read parameter information. <br>
    *** Reference: http://www.ietf.org/rfc/rfc1867.txt <br>
    *** (This feature is still experimental, and may not be fully supported/implemented)
    *** @param request      The HttpServletRequest
    *** @param maxLen       The maximum number of bytes read from a MIME part ('-1' to disregard length limit)
    *** @param fileSaveDir  The file directory where file types are saved (null to only store data in memory)
    *** @throws IOException If a IO error occurs
    **/
    protected static void _parseMultipartFormData(HttpServletRequest request, int maxLen, File fileSaveDir)
        throws IOException
    {
        
        /* content length */
        // int contentLength = AttributeTools.getContentLength(request);

        /* content type */
        String contentType   = AttributeTools.getContentType(request);
        String multiBoundary = HTMLTools.getContentMultipartBoundary(contentType);
        {
            int ctSep = contentType.indexOf(";");
            if (ctSep >= 0) {
                contentType = contentType.substring(0,ctSep);
            }
        }
        
        /* parse multipart forms */
        if (!StringTools.isBlank(multiBoundary)) {
            ServletInputStream sis = request.getInputStream();

            /* multipart boundary pattern */
            multiBoundary = "--" + multiBoundary;
            byte pattern[] = multiBoundary.getBytes();
            //Print.logInfo("InputStream: boundary=" + multiBoundary);

            /* read to first boundary */
            for (;;) {
                String b = FileTools.readLineNL(sis);
                if (b.startsWith(multiBoundary)) {
                    break;
                }
            }

            /* reset boundary to include prefixing CRLF */
            multiBoundary = "\r\n" + multiBoundary;
            pattern = multiBoundary.getBytes();

            /* loop through multiparts */
            RTProperties rtProps = new RTProperties();
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            multipartLoop:
            for (;;) {

                /* read headers */
                MimePart partHeaders = new MimePart();
                for (;;) {
                    try {
                        String h = FileTools.readLineNL(sis);
                        if (StringTools.isBlank(h)) { break; }
                        int p = h.indexOf(":");
                        String key = (p >= 0)? h.substring(0,p).trim() : h.trim();
                        String val = (p >= 0)? h.substring(p+1).trim() : "";
                        partHeaders.put(key.toLowerCase(), val);
                        //Print.logInfo("Key: " + key + " ==> " + val);
                    } catch (EOFException eof) {
                        Print.logInfo("EOF while reading headers");
                        break multipartLoop;
                    }
                }

                /* mime part Content-Type */
                String partType = (String)partHeaders.get(HTMLTools.HEADER_CONTENT_TYPE.toLowerCase()); // may be null

                /* Content-Disposition */
                // Content-disposition: attachment; filename="file2.gif"
                // Content-Disposition: form-data; name="upload"; filename=""
                String partName = null;
                String partFile = null;
                String partDisp = (String)partHeaders.get(HTMLTools.HEADER_CONTENT_DISPOSITION.toLowerCase());
                if (!StringTools.isBlank(partDisp)) {
                    String cdisp[] = StringTools.split(partDisp,';');
                    String mimeType = (cdisp.length > 0)? cdisp[0] : "";
                    for (int c = 1; c < cdisp.length; c++) {
                        if (cdisp[c].startsWith("name=")) {
                            partName = StringTools.parseQuote(cdisp[c].substring(5));
                            partHeaders.put(MIMEPART_NAME, partName);
                        } else
                        if (cdisp[c].startsWith("filename=")) {
                            partFile = StringTools.parseQuote(cdisp[c].substring(9));
                            partHeaders.put(MIMEPART_FILENAME, partFile);
                        }
                    }
                }
                //Print.logInfo("Name = " + partName);
                //Print.logInfo("File = " + partFile);

                /* read data, until boundary */
                try {
                    if (!StringTools.isBlank(partFile) && FileTools.isDirectory(fileSaveDir)) {
                        File toFile = new File(fileSaveDir, partFile);
                        FileOutputStream fos = null;
                        try {
                            partHeaders.put(MIMEPART_FILE, toFile.toString());
                            FileInputStream fis = new FileInputStream(toFile);
                            int len = FileTools.copyStreams(sis, fos, pattern, maxLen, false);
                            if ((maxLen > 0) && (len >= maxLen)) {
                                partHeaders.put(MIMEPART_INCOMPLETE, "true");
                            } else {
                                //partHeaders.put(MIMEPART_INCOMPLETE, "false");
                            }
                        } catch (EOFException eofe) {
                            partHeaders.put(MIMEPART_INCOMPLETE, "true");
                            throw eofe;
                        } catch (IOException ioe) {
                            partHeaders.put(MIMEPART_INCOMPLETE, "true");
                        } finally {
                            try { fos.close(); } catch (Throwable th) {}
                        }
                        Print.logInfo("Multi Name="+partName+" ==> <file>");
                        rtProps.setProperty(partName, partHeaders);
                    } else {
                        byteOutput.reset(); // clear previous data
                        int len = FileTools.copyStreams(sis, byteOutput, pattern, maxLen, false);
                        if ((maxLen > 0) && (len >= maxLen)) {
                            partHeaders.put(MIMEPART_INCOMPLETE, "true");
                        } else {
                            //partHeaders.put(MIMEPART_INCOMPLETE, "false");
                        }
                        byte data[] = byteOutput.toByteArray();
                        if (StringTools.isBlank(partType) || partType.equals(HTMLTools.CONTENT_TYPE_PLAIN)) {
                            String partString = StringTools.toStringValue(data);
                            partHeaders.put(MIMEPART_STRING, partString);
                            rtProps.setString(partName, partString);
                            Print.logInfo("Multi Name="+partName+" ==> " + partString);
                        } else {
                            partHeaders.put(MIMEPART_BYTES, data);
                            rtProps.setProperty(partName, partHeaders);
                            Print.logInfo("Multi Name="+partName+" ==> <binary>");
                        }
                        byteOutput.reset(); // clear for garbage collection
                    }
                } catch (EOFException eof) {
                    // EOF unexpected here
                    Print.logInfo("EOF while attempting to read multipart data");
                    break;
                }

                /* read next 2 bytes after boundary */
                try {
                    String NL = FileTools.readLineNL(sis);
                    if (NL.equals("--")) {
                        // we are done
                        break;
                    }
                } catch (EOFException eof) {
                    // not expected here
                    Print.logInfo("EOF while reading final CRLF");
                    break;
                }

            } // for (;;)

            /* set request attribute */
            request.setAttribute(ATTR_MULTIPART, rtProps);

        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Returns the HttpSession from the specified ServletRequest
    *** @param req The ServletRequest
    *** @return The HttpSession extracted from the specified ServletRequest
    **/
    public static HttpSession getSession(ServletRequest req)
    {
        return (req instanceof HttpServletRequest)?
            ((HttpServletRequest)req).getSession(true) :
            null;
    }

    /* return the current HttpSessionContext */
    // deprecated: returns null/empty
    //public static HttpSessionContext getSessionContext(ServletRequest req)
    //{
    //    HttpSession session = AttributeTools.getSession(req);
    //    return (session != null)? session.getSessionContext() : null;
    //}

    // ------------------------------------------------------------------------

    /**
    *** Gets the content type for the specified Request
    *** @param req  The request
    *** @return The content type
    **/
    public static String getContentType(HttpServletRequest req)
    {
        if (req == null) {
            return "";
        } else {
            String ct1 = StringTools.trim(req.getContentType());
            String ct2 = StringTools.trim(req.getHeader("content-type"));
            return (ct1.length() >= ct2.length())? ct1 : ct2;
        }
    }

    /**
    *** Gets the content length for the specified Request
    *** @param req  The request
    *** @return The content length
    **/
    public static int getContentLength(HttpServletRequest req)
    {
        return (req != null)? req.getContentLength() : 0;
    }

    // ------------------------------------------------------------------------

    /**
    *** Looks for and decodes request argument "rtp" and adds any contained
    *** properties to the request attributes.
    *** @param req  The ServletRequest
    *** @return A copy of the decoded RTProperties
    **/
    public static RTProperties parseRTP(ServletRequest req)
    {
        if (req != null) {
            String args = req.getParameter(ATTR_RTP);
            if (!StringTools.isBlank(args)) {
                RTProperties rtp = URIArg.parseRTP(args);
                if (rtp != null) {
                    rtp.setIgnoreKeyCase(IGNORE_CASE);
                    req.setAttribute(ATTR_RTP, rtp);
                    return rtp;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    
    /** 
    *** Returns an array of keys from the list of session request parameters that
    *** match the specified partial key.
    *** @param key_  The parameter partial key
    *** @return An array of matching parameter keys
    **/
    public static String[] getMatchingKeys(ServletRequest req, String key_)
    {
        if (req == null)  {
            return null;
        } else {
            boolean allKeys = (key_ == null) || key_.equals("");
            java.util.List<String> keyList = new Vector<String>();
            for (Enumeration e = req.getParameterNames(); e.hasMoreElements();) {
                String k = (String)e.nextElement();
                if (allKeys) {
                    keyList.add(k);
                } else
                if (IGNORE_CASE? StringTools.startsWithIgnoreCase(k,key_) : k.startsWith(key_)) {
                    keyList.add(k);
                }
            }
            if (keyList.size() > 0) {
                // at least one entry was found
                return keyList.toArray(new String[keyList.size()]);
            } else {
                // no entries found
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Search for the specified key in the following location(s):
    //  1) The URL Query string

    /**
    *** Returns true if the specified key is defined in the parameter list for
    *** the specified ServletRequest.
    *** @param req  The ServletRequest
    *** @param key  The key to test
    *** @return True if the specified key is defined.
    **/
    public static boolean hasRequestAttribute(ServletRequest req, String key)
    {
        String v = getRequestString(req,key,null);
        return (v != null);
    }

    /**
    *** Returns true if the specified key is defined in the parameter list for
    *** the specified ServletRequest.
    *** @param req  The ServletRequest
    *** @param key  An array of keys to test
    *** @return True if the specified key is defined.
    **/
    public static boolean hasRequestAttribute(ServletRequest req, String key[])
    {
        String v = getRequestString(req,key,null);
        return (v != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the String value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  The key to test
    *** @param dft  The default value to return if the key is not defined
    *** @return The String value of the specified key
    **/
    public static String getRequestString(ServletRequest req, String key, String dft)
    {

        /* nothing to lookup? */
        if ((req == null) || StringTools.isBlank(key)) {
            return dft;
        }

        /* standard parameters */
        if (IGNORE_CASE) {
            for (Enumeration e = req.getParameterNames(); e.hasMoreElements();) {
                String n = (String)e.nextElement();
                if (n.equalsIgnoreCase(key)) {
                    String val = req.getParameter(n);
                    if (val != null) {
                        return val;
                    }
                }
            }
        } else {
            String val = req.getParameter(key);
            if (val != null) {
                return val;
            }
        }

        /* RTProperties? */
        RTProperties rtp = (RTProperties)req.getAttribute(ATTR_RTP);
        if (rtp != null) {
            String val = rtp.getString(key, null);
            if (val != null) {
                return val;
            }
        }

        /* Multipart MIME? */
        RTProperties multi = (RTProperties)req.getAttribute(ATTR_MULTIPART);
        if (multi != null) {
            Object val = multi.getProperty(key, null);
            if (val == null) {
                // skip
            } else
            if (val instanceof String) {
                return (String)val;
            } else {
                // (val instanceof HashMap)
                return (dft == null)? null : "";
            }
        }

        /* default */
        return dft;

    }

    /**
    *** Returns the String value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req      The ServletRequest
    *** @param keyList  The keys to test
    *** @param dft      The default value to return if the key is not defined
    *** @return The String value of the specified key
    **/
    public static String getRequestString(ServletRequest req, String keyList[], String dft)
    {

        /* nothing to lookup? */
        if ((req == null) || ListTools.isEmpty(keyList)) {
            return dft;
        }

        /* standard parameters */
        if (IGNORE_CASE) {
            for (Enumeration e = req.getParameterNames(); e.hasMoreElements();) {
                String n = (String)e.nextElement();
                for (String key : keyList) {
                    if (n.equalsIgnoreCase(key)) {
                        String val = req.getParameter(n);
                        if (val != null) {
                            return val;
                        }
                    }
                }
            }
        } else {
            for (String key : keyList) {
                String val = req.getParameter(key);
                if (val != null) {
                    return val;
                }
            }
        }

        /* RTProperties? */
        RTProperties rtp = (RTProperties)req.getAttribute(ATTR_RTP);
        if (rtp != null) {
            String val = rtp.getString(keyList, null);
            if (val != null) {
                return val;
            }
        }

        /* Multipart MIME? */
        RTProperties multi = (RTProperties)req.getAttribute(ATTR_MULTIPART);
        if (multi != null) {
            for (String key : keyList) {
                Object val = multi.getProperty(key, null);
                if (val != null) {
                    if (val instanceof String) {
                        return (String)val;
                    } else {
                        // (val instanceof HashMap) is likely true
                        return (dft == null)? null : "";
                    }
                }
            }
        }

        /* default */
        return dft;

    }

    /**
    *** Returns the String value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  An array of keys to test
    *** @param dft  The default value to return if the key is not defined
    *** @return The String value of the specified key
    **/
    /*
    public static String getRequestString(ServletRequest req, String key[], String dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                String val = getRequestString(req,key[i],null);
                if (val != null) {
                    return val;
                }
            }
            return dft;
        }
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Returns the Double value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  The key to test
    *** @param dft  The default value to return if the key is not defined, 
    ***             or cannot be converted to a Double.
    *** @return The Double value of the specified key
    **/
    public static double getRequestDouble(ServletRequest req, String key, double dft)
    {
        return StringTools.parseDouble(getRequestString(req,key,null), dft);
    }

    /**
    *** Returns the Double value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  An array of keys to test
    *** @param dft  The default value to return if the key is not defined, 
    ***             or cannot be converted to a Double.
    *** @return The Double value of the specified key
    **/
    public static double getRequestDouble(ServletRequest req, String key[], double dft)
    {
        return StringTools.parseDouble(getRequestString(req,key,null), dft);
    }

    /**
    *** Returns the Long value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  The key to test
    *** @param dft  The default value to return if the key is not defined,
    ***             or cannot be converted to a Long.
    *** @return The Long value of the specified key
    **/
    public static long getRequestLong(ServletRequest req, String key, long dft)
    {
        return StringTools.parseLong(getRequestString(req,key,null), dft);
    }

    /**
    *** Returns the Long value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  An array of keys to test
    *** @param dft  The default value to return if the key is not defined,
    ***             or cannot be converted to a Long.
    *** @return The Long value of the specified key
    **/
    public static long getRequestLong(ServletRequest req, String key[], long dft)
    {
        return StringTools.parseLong(getRequestString(req,key,null), dft);
    }

    /**
    *** Returns the Int value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  The key to test
    *** @param dft  The default value to return if the key is not defined,
    ***             or cannot be converted to a Int.
    *** @return The Int value of the specified key
    **/
    public static int getRequestInt(ServletRequest req, String key, int dft)
    {
        return StringTools.parseInt(getRequestString(req,key,null), dft);
    }

    /**
    *** Returns the Int value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  An array of keys to test
    *** @param dft  The default value to return if the key is not defined,
    ***             or cannot be converted to a Int.
    *** @return The Int value of the specified key
    **/
    public static int getRequestInt(ServletRequest req, String key[], int dft)
    {
        return StringTools.parseInt(getRequestString(req,key,null), dft);
    }

    /**
    *** Returns the Boolean value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  The key to test
    *** @param dft  The default value to return if the key is not defined,
    ***             or cannot be converted to a Boolean.
    *** @return The Boolean value of the specified key
    **/
    public static boolean getRequestBoolean(ServletRequest req, String key, boolean dft)
    {
        return StringTools.parseBoolean(getRequestString(req,key,null), dft);
    }

    /**
    *** Returns the Boolean value of the specified key from the parameter list in
    *** the specified ServletRequest
    *** @param req  The ServletRequest
    *** @param key  An array of keys to test
    *** @param dft  The default value to return if the key is not defined,
    ***             or cannot be converted to a Boolean.
    *** @return The Boolean value of the specified key
    **/
    public static boolean getRequestBoolean(ServletRequest req, String key[], boolean dft)
    {
        return StringTools.parseBoolean(getRequestString(req,key,null), dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Search for the specified key in the following location(s):
    //  1) The Session attributes

    /**
    *** Returns true if the specified attribute key is defined in the specified HttpSession
    *** @param key  The attribute key to test
    *** @return True if the attribute key is defined
    **/
    public static boolean hasSessionAttribute(HttpSession sess, String key)
    {
        Object val = getSessionAttribute(sess,key,null);
        return (val != null);
    }

    /**
    *** Returns true if the specified attribute key is defined in the specified HttpSession
    *** @param key  The attribute key to test
    *** @return True if the attribute key is defined
    **/
    public static boolean hasSessionAttribute(HttpSession sess, String key[])
    {
        Object val = getSessionAttribute(sess,key,null);
        return (val != null);
    }

    /**
    *** Returns true if the specified attribute key is defined in the specified ServletRequest
    *** @param key  The attribute key to test
    *** @return True if the attribute key is defined
    **/
    public static boolean hasSessionAttribute(ServletRequest req, String key)
    {
        return hasSessionAttribute(getSession(req), key);
    }

    /**
    *** Returns true if the specified attribute key is defined in the specified ServletRequest
    *** @param key  The attribute key to test
    *** @return True if the attribute key is defined
    **/
    public static boolean hasSessionAttribute(ServletRequest req, String key[])
    {
        return hasSessionAttribute(getSession(req), key);
    }

    // --------------------------------

    /**
    *** Gets the value for the specified attribute key from the specified HttpSession
    *** @param sess  The HttpSession
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined.
    *** @return The value of the specified attribute key
    **/
    public static Object getSessionAttribute(HttpSession sess, String key, Object dft)
        // throws java.lang.IllegalStateException
    {
        try {
            Object val = (sess != null)? sess.getAttribute(key) : null;
            return (val != null)? val : dft;
        } catch (java.lang.IllegalStateException ise) {
            // "java.lang.IllegalStateException: getAttribute: Session already invalidated"
            // throw ise;
            return dft;
        }
    }

    /**
    *** Gets the value for the specified attribute key from the specified HttpSession
    *** @param sess  The HttpSession
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined.
    *** @return The value of the specified attribute key
    **/
    public static Object getSessionAttribute(HttpSession sess, String key[], Object dft)
    {
        if ((sess == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                Object val = getSessionAttribute(sess,key[i],null);
                if (val != null) {
                    return val;
                }
            }
            return dft;
        }
    }

    /**
    *** Gets the value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined.
    *** @return The value of the specified attribute key
    **/
    public static Object getSessionAttribute(ServletRequest req, String key, Object dft)
    {
        Object val = getSessionAttribute(getSession(req), key, null);
        return (val != null)? val : dft;
    }

    /**
    *** Gets the value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined.
    *** @return The value of the specified attribute key
    **/
    public static Object getSessionAttribute(ServletRequest req, String key[], Object dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                Object val = getSessionAttribute(req,key[i],null);
                if (val != null) {
                    return val;
                }
            }
            return dft;
        }
    }

    // --------------------------------

    /**
    *** Gets the String value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined.
    *** @return The String value of the specified attribute key
    **/
    public static String getSessionString(ServletRequest req, String key, String dft)
    {
        Object val = getSessionAttribute(getSession(req), key, null);
        return (val != null)? val.toString() : dft;
    }

    /**
    *** Gets the String value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined.
    *** @return The String value of the specified attribute key
    **/
    public static String getSessionString(ServletRequest req, String key[], String dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                String val = getSessionString(req,key[i],null);
                if (val != null) {
                    return val;
                }
            }
            return dft;
        }
    }

    // --------------------------------

    /**
    *** Gets the Double value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Double.
    *** @return The Double value of the specified attribute key
    **/
    public static double getSessionDouble(ServletRequest req, String key, double dft)
    {
        Object val = getSessionAttribute(getSession(req), key, null);
        return StringTools.parseDouble(val, dft);
    }

    /**
    *** Gets the Double value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Double.
    *** @return The Double value of the specified attribute key
    **/
    public static double getSessionDouble(ServletRequest req, String key[], double dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                Object val = getSessionAttribute(req,key[i],null);
                if (val != null) {
                    return StringTools.parseDouble(val, dft);
                }
            }
            return dft;
        }
    }

    // --------------------------------

    /**
    *** Gets the Long value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Long.
    *** @return The Long value of the specified attribute key
    **/
    public static long getSessionLong(ServletRequest req, String key, long dft)
    {
        Object val = getSessionAttribute(getSession(req), key, null);
        return StringTools.parseLong(val, dft);
    }

    /**
    *** Gets the Long value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Long.
    *** @return The Long value of the specified attribute key
    **/
    public static long getSessionLong(ServletRequest req, String key[], long dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                Object val = getSessionAttribute(req,key[i],null);
                if (val != null) {
                    return StringTools.parseLong(val, dft);
                }
            }
            return dft;
        }
    }

    // --------------------------------

    /**
    *** Gets the Int value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Int.
    *** @return The Int value of the specified attribute key
    **/
    public static int getSessionInt(ServletRequest req, String key, int dft)
    {
        Object val = getSessionAttribute(getSession(req), key, null);
        return StringTools.parseInt(val, dft);
    }

    /**
    *** Gets the Int value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Int.
    *** @return The Int value of the specified attribute key
    **/
    public static int getSessionInt(ServletRequest req, String key[], int dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                Object val = getSessionAttribute(req,key[i],null);
                if (val != null) {
                    return StringTools.parseInt(val, dft);
                }
            }
            return dft;
        }
    }

    // --------------------------------

    /**
    *** Gets the Boolean value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Boolean.
    *** @return The Boolean value of the specified attribute key
    **/
    public static boolean getSessionBoolean(ServletRequest req, String key, boolean dft)
    {
        Object val = getSessionAttribute(getSession(req), key, null);
        return StringTools.parseBoolean(val, dft);
    }
    
    /**
    *** Gets the Boolean value for the specified attribute key from the specified ServletRequest
    *** @param req   The ServletRequest
    *** @param key   The key for which the attribute value will be returned
    *** @param dft   The default value returns if the key is not defined,
    ***              or cannot be converted to a Boolean.
    *** @return The Boolean value of the specified attribute key
    **/
    public static boolean getSessionBoolean(ServletRequest req, String key[], boolean dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                Object val = getSessionAttribute(req,key[i],null);
                if (val != null) {
                    return StringTools.parseBoolean(val, dft);
                }
            }
            return dft;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Search for the specified key in the following location(s):
    //  1) The URL Query string
    //  2) The Session attributes

    /**
    *** Returns the value for the specified key from the ServletRequest.  The query
    *** string is first searched for the key/value.  The session attributes are then
    *** search if the key/value is not found in the query string.
    *** @param req  The ServletRequest
    *** @param key  The key for which the value is to be returned
    *** @param dft  The default value return if the key is not defined
    *** @return The value of the speciied key
    **/
    public static Object getRequestAttribute(ServletRequest req, String key, Object dft)
    {
        // first try the query string
        Object val = getRequestString(req, key, null);
        // then try the session attributes
        if (val == null) { val = getSessionAttribute(req, key, null); }
        // all else fails, return the default
        return (val != null)? val : dft;
    }

    /**
    *** Returns the value for the specified key from the ServletRequest.  The query
    *** string is first searched for the key/value.  The session attributes are then
    *** search if the key/value is not found in the query string.
    *** @param req  The ServletRequest
    *** @param key  The key for which the value is to be returned
    *** @param dft  The default value return if the key is not defined
    *** @return The value of the speciied key
    **/
    public static Object getRequestAttribute(ServletRequest req, String key[], Object dft)
    {
        if ((req == null) || ListTools.isEmpty(key)) {
            return dft;
        } else {
            // first try the query string
            Object val = getRequestString(req, key, null);
            // then try the session attributes
            if (val == null) { val = getSessionAttribute(req, key, null); }
            // all else fails, return the default
            return (val != null)? val : dft;
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Sets the HttpSession attribute value for the specified key
    *** @param sess  The HttpSession
    *** @param key   The attribute key to set
    *** @param val   The value to set for the specified key
    **/
    public static void setSessionAttribute(HttpSession sess, String key, Object val)
    {
        if (sess != null) {
            try {
                if (val != null) {
                    sess.setAttribute(key, val);
                } else {
                    sess.removeAttribute(key);
                }
            } catch (Throwable th) { // IllegalStateException
                Print.logError("Error setting HttpSession attribute: " + th);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the ServletRequest attribute value for the specified key
    *** @param req   The ServletRequest
    *** @param key   The attribute key to set
    *** @param val   The value to set for the specified key
    **/
    public static void setSessionAttribute(ServletRequest req, String key, Object val)
    {
        setSessionAttribute(getSession(req), key, val);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the ServletRequest attribute Double value for the specified key
    *** @param req   The ServletRequest
    *** @param key   The attribute key to set
    *** @param val   The Double value to set for the specified key
    **/
    public static void setSessionDouble(ServletRequest req, String key, double val)
    {
        setSessionAttribute(getSession(req), key, new Double(val));
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the ServletRequest attribute Long value for the specified key
    *** @param req   The ServletRequest
    *** @param key   The attribute key to set
    *** @param val   The Long value to set for the specified key
    **/
    public static void setSessionLong(ServletRequest req, String key, long val)
    {
        setSessionAttribute(getSession(req), key, new Long(val));
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the ServletRequest attribute Int value for the specified key
    *** @param req   The ServletRequest
    *** @param key   The attribute key to set
    *** @param val   The Int value to set for the specified key
    **/
    public static void setSessionInt(ServletRequest req, String key, int val)
    {
        setSessionAttribute(getSession(req), key, new Integer(val));
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the ServletRequest attribute Boolean value for the specified key
    *** @param req   The ServletRequest
    *** @param key   The attribute key to set
    *** @param val   The Boolean value to set for the specified key
    **/
    public static void setSessionBoolean(ServletRequest req, String key, boolean val)
    {
        setSessionAttribute(getSession(req), key, new Boolean(val));
    }

    // ------------------------------------------------------------------------

    /**
    *** Clears all HttpSession attributes
    *** @param sess  The HttpSession 
    **/
    public static void clearSessionAttributes(HttpSession sess)
    {
        if (sess != null) {
            sess.invalidate();
        }
    }

    /**
    *** Clears all ServletRequest attributes
    *** @param req  The ServletRequest 
    **/
    public static void clearSessionAttributes(ServletRequest req)
    {
        clearSessionAttributes(getSession(req));
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
   
    private static final String ARG_DECODE[]    = new String[] { "decode" , "d"  };
    private static final String ARG_ENCODE[]    = new String[] { "encode" , "e"  };
    private static final String ARG_URL[]       = new String[] { "url"    , "u"  };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + AttributeTools.class.getName() + " {options}");
        Print.logInfo("Options:");
        Print.logInfo("  -encode=<ASCII>    Encode ASCII string to URL argument string");
        Print.logInfo("  -decode=<args>     Decode URL argument string to ASCII");
        System.exit(1);
    }

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* encode rtp URL */
        if (RTConfig.hasProperty(ARG_URL)) {
            URIArg u = new URIArg(RTConfig.getString(ARG_URL,""));
            String r = URIArg.encodeRTP(u.getArgProperties());
            Print.sysPrintln("==> " + u.getURI() + "?" + ATTR_RTP + "=" + r);
            System.exit(0);
        }

        /* no options */
        usage();
        
    }

}
