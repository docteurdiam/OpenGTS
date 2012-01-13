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
//  2006/02/19  Martin D. Flynn
//     -Initial release
//  2008/02/27  Martin D. Flynn
//     -Modified 'getNodeText' to include 'CDATA' sections
//     -Added 'getDocument(byte xml[])' methods
//  2009/07/01  Martin D. Flynn
//     -Modified 'getNodeText' to support a returned default value
//  2011/03/08  Martin D. Flynn
//     -Modified "getAttribute" to check for 'elem.hasAttribute(...)'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.awt.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
*** A set of tools for simplifying the reading and validating of XML documents
**/

public class XMLTools
{

    // ------------------------------------------------------------------------

    /**
    *** XMLErrorHandler class
    **/
    public static class XMLErrorHandler
        implements ErrorHandler
    {
        public XMLErrorHandler() {
        }
        private void printError(String msg, SAXParseException spe) {
            int line = spe.getLineNumber();
            int col  = spe.getColumnNumber();
            System.out.println(msg + " [" + line + ":"+ col+ "] " + spe.getMessage());
        }
        public void error(SAXParseException spe) throws SAXException {
            printError("ERROR", spe);
        }
        public void fatalError(SAXParseException spe) throws SAXException {
            printError("FATAL", spe);
        }
        public void warning(SAXParseException spe) throws SAXException {
            printError("WARN ", spe);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Start tag
    **/
    public static String startTAG(boolean isSoapReq, String tag, String attr, boolean endTag, boolean newLine)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(isSoapReq? "&lt;" : "<");
        sb.append(tag);
        if (!StringTools.isBlank(attr)) {
            if (!attr.startsWith(" ")) { sb.append(" "); }
            sb.append(attr);
        }
        if (endTag) {
            sb.append("/");
        }
        sb.append(isSoapReq? "&gt;" : ">");
        if (!isSoapReq && newLine) {
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
    *** End tag
    **/
    public static String endTAG(boolean isSoapReq, String tag, boolean newLine)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(isSoapReq? "&lt;/" : "</");
        sb.append(tag);
        sb.append(isSoapReq? "&gt;" : ">");
        if (!isSoapReq && newLine) {
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
    *** Wrap content in CDATA
    **/
    public static String CDATA(boolean isSoapReq, String content)
    {
        if ((content == null) || content.equals("")) { // do not use StringTools.isBlank
            return "";
        } else
        if (isSoapReq) {
            return "&lt;![CDATA[" + content + "]]&gt;";
        } else {
            return "<![CDATA[" + content + "]]>";
        }
    }

    /**
    *** Create prefix spaces
    */
    public static String PREFIX(boolean isSoapReq, int indent)
    {
        return isSoapReq? "" : StringTools.replicateString(" ",indent);
    }

    /**
    *** Create Attribute
    **/
    public static String ATTR(String key, Object value)
    {
        if (value != null) {
            return XMLTools.ATTR(key, value.toString());
        } else {
            return "";
        }
    }

    /**
    *** Create Attribute
    **/
    public static String ATTR(String key, String value)
    {
        if ((value != null) && !value.equals("")) {
            StringBuffer sb = new StringBuffer();
            sb.append(" ").append(key).append("=\"").append(value).append("\"");
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** Create Attribute
    **/
    public static String ATTR(String key, int value)
    {
        return XMLTools.ATTR(key, String.valueOf(value));
    }

    /**
    *** Create Attribute
    **/
    public static String ATTR(String key, long value)
    {
        return XMLTools.ATTR(key, String.valueOf(value));
    }

    /**
    *** Create Attribute
    **/
    public static String ATTR(String key, boolean value)
    {
        return XMLTools.ATTR(key, String.valueOf(value));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Load and parses a XML document from a file
    *** @param xmlFile The file to load the document from
    *** @return The pasrsed XML document
    **/
    public static Document getDocument(File xmlFile)
    {
        return XMLTools.getDocument(xmlFile, false);
    }

    /**
    *** Load and parses a XML document from a file
    *** @param xmlFile The file to load the document from
    *** @param checkErrors True if the XML document should be validated 
    ***        against it's DTD
    *** @return The pasrsed XML document
    **/
    public static Document getDocument(File xmlFile, boolean checkErrors)
    {

        /* valid file? */
        if (xmlFile == null) {
            Print.logError("XML file is null!");
            return null;
        } else
        if (!xmlFile.exists()) {
            Print.logError("XML file does not exist: " + xmlFile);
            return null;
        }

        /* create XML document */
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (checkErrors) {
                dbf.setValidating(true);
                dbf.setIgnoringElementContentWhitespace(true);
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (checkErrors) {
                db.setErrorHandler(new XMLErrorHandler());
            }
            doc = db.parse(xmlFile);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
        }

        /* return */
        return doc;

    }

    // ------------------------------------------------------------------------

    /**
    *** Load and parse a XML document from an input stream 
    *** @param input The input stream to read the document from
    *** @return The parsed XML document
    **/
    public static Document getDocument(InputStream input)
    {
        return XMLTools.getDocument(input, false);
    }

    /**
    *** Load and parse a XML document from an input stream 
    *** @param input The input stream to read the document from
    *** @param checkErrors True if the XML document should be validated 
    ***        against it's DTD
    *** @return The parsed XML document
    **/
    public static Document getDocument(InputStream input, boolean checkErrors)
    {

        /* valid stream? */
        if (input == null) {
            Print.logError("XML stream is null!");
            return null;
        }

        /* create XML document */
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (checkErrors) {
                dbf.setValidating(true);
                dbf.setIgnoringElementContentWhitespace(true);
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (checkErrors) {
                db.setErrorHandler(new XMLErrorHandler());
            }
            doc = db.parse(input);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
        }

        /* return */
        return doc;

    }

    // ------------------------------------------------------------------------

    /**
    *** Load and parse a XML document from an array of bytes 
    *** @param xml The byte array to read the document from
    *** @return The parsed XML document
    **/
    public static Document getDocument(byte xml[])
    {
        return XMLTools.getDocument(xml, false);
    }

    /**
    *** Load and parse a XML document from an array of bytes 
    *** @param xml The byte array to read the document from
    *** @param checkErrors True if the XML document should be validated 
    ***        against it's DTD
    *** @return The parsed XML document
    **/
    public static Document getDocument(byte xml[], boolean checkErrors)
    {

        /* valid xml bytes? */
        if (xml == null) {
            Print.logError("XML data is null!");
            return null;
        }

        /* return */
        return XMLTools.getDocument(new ByteArrayInputStream(xml), checkErrors);

    }

    // ------------------------------------------------------------------------

    /**
    *** Load and parse a XML document from a String
    *** @param xml The String to read the document from
    *** @return The parsed XML document
    **/
    public static Document getDocument(String xml)
    {
        return XMLTools.getDocument(StringTools.getBytes(xml), false);
    }

    /**
    *** Load and parse a XML document from a String
    *** @param xml The String to read the document from
    *** @param checkErrors True if the XML document should be validated 
    ***        against it's DTD
    *** @return The parsed XML document
    **/
    public static Document getDocument(String xml, boolean checkErrors)
    {
        return XMLTools.getDocument(StringTools.getBytes(xml), checkErrors);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Parse text from node. Combines and returns the values of all immediate child
    *** Text/CData Nodes of the given <code>root</code> Node. 
    *** This method does not return null.
    *** @param root The node to get the text of
    *** @return The parsed text
    **/
    public static String getNodeText(Node root)
    {
        return XMLTools.getNodeText(root, null, false, "");
    }

    /** 
    *** Parse text from node. Combines and returns the values of all immediate child
    *** Text/CData Nodes of the given <code>root</code> Node. 
    *** This method does not return null.
    *** @param root The node to get the text of
    *** @param repNewline The string to replace any newlines with
    *** @return The parsed text
    **/
    public static String getNodeText(Node root, String repNewline)
    {
        return XMLTools.getNodeText(root, repNewline, false, "");
    }
    
    /** 
    *** Parse text from node. Combines and returns the values of all immediate child
    *** Text/CData Nodes of the given <code>root</code> Node.
    *** This method does not return null.
    *** @param root The node to get the text of
    *** @param repNewline The string to replace any newlines with
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The parsed text
    **/
    public static String getNodeText(Node root, String repNewline, boolean resolveRT)
    {
        return XMLTools.getNodeText(root, repNewline, resolveRT, "");
    }
    
    /** 
    *** Parse text from node. Combines and returns the values of all immediate child
    *** Text/CData Nodes of the given <code>root</code> Node. This method 
    *** may return the specified default value, <code>dft</code>.
    *** @param root The node to get the text of
    *** @param repNewline The string to replace any newlines with
    *** @param resolveRT If true, resolve any runtime config variables
    *** @param dft   The default value to return if no text was found
    *** @return The parsed text
    **/
    public static String getNodeText(Node root, String repNewline, boolean resolveRT, String dft)
    {
        StringBuffer text = null;

        /* extract String */
        if (root != null) {
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) { // CDATA Section
                    if (text == null) { text = new StringBuffer(); }
                    text.append(n.getNodeValue());
                } else
                if (n.getNodeType() == Node.TEXT_NODE) {
                    if (text == null) { text = new StringBuffer(); }
                    text.append(n.getNodeValue());
                } else {
                    //Print.logWarn("Unrecognized node type: " + n.getNodeType());
                }
            }
        }

        /* return default? */
        if (text == null) {
            if (StringTools.isBlank(dft)) {
                return dft;
            } else {
                text = new StringBuffer(dft);
            }
        }

        /* remove CR, and handle NL */
        if (repNewline != null) {
            // 'repNewline' contains text which is used to replace detected '\n' charaters
            StringBuffer sb = new StringBuffer();
            String s[] = StringTools.parseString(text.toString(),"\n\r");
            for (int i = 0; i < s.length; i++) {
                String line = s[i].trim();
                if (!line.equals("")) {
                    if (sb.length() > 0) {
                        sb.append(repNewline);
                    }
                    sb.append(line);
                }
            }
            text = sb;
        }
        
        /* return resulting text */
        if (resolveRT) {
            // resolve runtime property variables
            return RTConfig.insertKeyValues(text.toString());
        } else {
            // as-is
            return text.toString();
        }

    }

    /**
    *** Parse String into an array terminated by CR, NL, or CRNL
    *** @param text  The String text to parse
    *** @return An array of String lines
    **/
    public static String[] parseLines(String text)
    {
        return StringTools.parseString(text, "\r\n");
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the value for the specified attribute key, or null if the key is not
    *** defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @return The value of the key, or null if the key is not defined.
    **/
    public static String getAttribute(Element elem, String key)
    {
        return XMLTools.getAttribute(elem, key, null, false);
    }

    /**
    *** Returns the value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default value to return if the key is not defined
    *** @return The value of the key, or the default value if the key is not defined.
    **/
    public static String getAttribute(Element elem, String key, String dft)
    {
        return XMLTools.getAttribute(elem, key, dft, false);
    }

    /**
    *** Returns the value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default value to return if the key is not defined
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The value of the key, or the default value if the key is not defined.
    **/
    public static String getAttribute(Element elem, String key, String dft, boolean resolveRT)
    {
        String rtn = dft;
        if ((elem != null) && !StringTools.isBlank(key) && elem.hasAttribute(key)) {
            String val = elem.getAttribute(key); // never returns null
            if (val != null) { // but check anyway
                rtn = val;
            }
        }
        if (resolveRT) {
            // resolve runtime property variables
            return RTConfig.insertKeyValues(rtn);
        } else {
            // as-is
            return rtn;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the boolean value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default boolean value to return if the key is not defined
    *** @return The boolean value of the key, or the default value if the key is not defined.
    **/
    public static boolean getAttributeBoolean(Element elem, String key, boolean dft)
    {
        return StringTools.parseBoolean(XMLTools.getAttribute(elem,key,null,false), dft);
    }

    /**
    *** Returns the boolean value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default boolean value to return if the key is not defined
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The boolean value of the key, or the default value if the key is not defined.
    **/
    public static boolean getAttributeBoolean(Element elem, String key, boolean dft, boolean resolveRT)
    {
        return StringTools.parseBoolean(XMLTools.getAttribute(elem,key,null,resolveRT),dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the int value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default int value to return if the key is not defined
    *** @return The int value of the key, or the default value if the key is not defined.
    **/
    public static int getAttributeInt(Element elem, String key, int dft)
    {
        return StringTools.parseInt(XMLTools.getAttribute(elem,key,null,false), dft);
    }

    /**
    *** Returns the int value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default int value to return if the key is not defined
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The int value of the key, or the default value if the key is not defined.
    **/
    public static int getAttributeInt(Element elem, String key, int dft, boolean resolveRT)
    {
        return StringTools.parseInt(XMLTools.getAttribute(elem,key,null,resolveRT),dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the long value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default long value to return if the key is not defined
    *** @return The long value of the key, or the default value if the key is not defined.
    **/
    public static long getAttributeLong(Element elem, String key, long dft)
    {
        return StringTools.parseLong(XMLTools.getAttribute(elem,key,null,false), dft);
    }

    /**
    *** Returns the long value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default long value to return if the key is not defined
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The long value of the key, or the default value if the key is not defined.
    **/
    public static long getAttributeLong(Element elem, String key, long dft, boolean resolveRT)
    {
        return StringTools.parseLong(XMLTools.getAttribute(elem,key,null,resolveRT),dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the double value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default double value to return if the key is not defined
    *** @return The double value of the key, or the default value if the key is not defined.
    **/
    public static double getAttributeDouble(Element elem, String key, double dft)
    {
        return StringTools.parseDouble(XMLTools.getAttribute(elem,key,null,false), dft);
    }

    /**
    *** Returns the double value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default double value to return if the key is not defined
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The double value of the key, or the default value if the key is not defined.
    **/
    public static double getAttributeDouble(Element elem, String key, double dft, boolean resolveRT)
    {
        return StringTools.parseDouble(XMLTools.getAttribute(elem,key,null,resolveRT),dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the first child element of the specified Node having the specified name
    *** @param root The Node to look for the child element in
    *** @param name The name of the child element
    *** @return The child element
    **/
    public static Element getChildElement(Node root, String name)
    {
        //print("Looking for " + name);
        NodeList list = root.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            //print(", checking " + n.getNodeName());
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if ((name == null) || n.getNodeName().equalsIgnoreCase(name)) {
                    //println(", found!");
                    return (Element)n;
                }
            }
        }
        //println(", not found");
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets all child elements of the specified Node having the specified name
    *** @param root The Node to look for the child element in
    *** @param name The name of the child element
    *** @return The child element array
    **/
    public static NodeList getChildElements(Node root, String name)
    {
        //  NodeList nodes = root.getElementsByTagName(name);
        final java.util.List<Node> elemList = new Vector<Node>();
        NodeList childNodes = root.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if ((name == null) || n.getNodeName().equalsIgnoreCase(name)) {
                    //Print.logInfo("Found '"+name+"', type=" + n.getNodeType());
                    elemList.add(n);
                }
            }
        }
        return new NodeList() {
            public int getLength() {
                return elemList.size();
            }
            public Node item(int index) {
                return ((index >= 0) && (index < elemList.size()))? elemList.get(index) : null;
            }
        };
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets gets the path of an element found at a relative path from 
    *** <code>root</code>
    *** @param root The root node
    *** @param nodes The path to the child element, delimited with '/'
    *** @return The destination child element, or <code>null</code>
    **/
    public static String getPathText(Element root, String nodes)
    {
        Element node = XMLTools.getPathElement(root, nodes);
        return (node != null)? XMLTools.getNodeText(node) : null;
    }

    /**
    *** Gets an element by a relative path from <code>root</code>
    *** @param root The root node
    *** @param nodes The path to the child element, delimited with '/'
    *** @return The destination child element, or <code>null</code>
    **/
    public static Element getPathElement(Element root, String nodes)
    {
        return XMLTools.getPathElement(root, new StringTokenizer(nodes, "/"));
    }

    /**
    *** Gets an element by a relative path from <code>root</code>
    *** @param root  The root node
    *** @param nodes The path to the child element, delimited with '/'
    *** @return The destination child element, or <code>null</code>
    **/
    public static Element getPathElement(Element root, StringTokenizer nodes)
    {
        if (root == null) {
            return null;
        } else
        if (nodes == null) {
            return XMLTools.getChildElement(root, null);
        } else
        if (!nodes.hasMoreTokens()) {
            return root;
        } else {
            String nextName = nodes.nextToken();
            return XMLTools.getPathElement(XMLTools.getChildElement(root, nextName), nodes);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String nodeToString(Node n)
    {
        try {
            TransformerFactory xfact = TransformerFactory.newInstance();
            try {
                xfact.setAttribute("indent-number", new Integer(2));
            } catch (Throwable th) { // IllegalArgumentException
                // ignore
            }
            Transformer xform = xfact.newTransformer();
            xform.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult out = new StreamResult(new StringWriter());
            //StreamResult out = new StreamResult(new OutputStreamWriter(new StringWriter(),"utf-8"));
            DOMSource ds = new DOMSource(n);
            xform.transform(ds, out);
            return out.getWriter().toString();
        } catch (TransformerConfigurationException tce) {
            Print.logException("Converting Node to String", tce);
            return null;
        } catch (TransformerException te) {
            Print.logException("Converting Node to String", te);
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void printNodeTree(String indent, Node n)
    {
        Object objVal = n.getNodeValue();
        String strVal = (objVal != null)? objVal.toString().trim() : "null";
        Print.logInfo(indent + "Name: " + n.getNodeName() + " ['" + strVal + "']");
        NamedNodeMap attr = n.getAttributes();
        if (attr != null) {
            for (int i = 0; i < attr.getLength(); i++) {
                XMLTools.printNodeTree(indent + "   [A] ", attr.item(i));
            }
        }
        NodeList child = n.getChildNodes();
        if (child != null) {
            for (int i = 0; i < child.getLength(); i++) {
                XMLTools.printNodeTree(indent + "    ", child.item(i));
            }
        }
    }
   
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {

        /* start with blank line */
        System.out.println("");

        /* get file from command line */
        File xmlFile = (argv.length > 0)? new File(argv[0]) : null;
        System.out.println("Loading XML file: " + xmlFile);

        /* parse/validate XML */
        Document doc = XMLTools.getDocument(xmlFile, true);
        if (doc == null) {
            System.out.println("");
            System.out.println("Fatal XML errors found!");
            System.exit(99);
        }
        
        /* print */
        String xmlStr = XMLTools.nodeToString(doc);
        System.out.println("");
        System.out.println(xmlStr);

    }

}
