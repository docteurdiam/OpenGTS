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
//  2011/08/21  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class HtmlWrapper
    extends WebPageAdaptor
    implements Constants //, JSONRequestHandler
{

    // ------------------------------------------------------------------------

    public static final String   PROP_loginRequired             = "loginRequired";
    public static final String   PROP_htmlFile                  = "htmlFile";
    public static final String   PROP_jsonRequestHandlerClass   = "jsonRequestHandlerClass";
    public static final String   PROP_fileUploadHandlerClass    = "fileUploadHandlerClass";
    public static final String   PROP_fileDownloadHandlerClass  = "fileDownloadHandlerClass";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public HtmlWrapper()
    {
        super.setBaseURI(RequestProperties.TRACK_BASE_URI());
        super.setPageName(PAGE_HTML_WRAP);  // override in private.xml
        super.setPageNavigation(null);      // override below
        super.setLoginRequired(false);      // override below
    }

    // ------------------------------------------------------------------------

    public boolean isLoginRequired()
    {
        return this.getProperties().getBoolean(PROP_loginRequired,super.isLoginRequired());
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean            jsonRequestHandler_init = false;
    private JSONRequestHandler jsonRequestHandler      = null;
    
    private void setJSONRequestHandler(JSONRequestHandler jrh)
    {
        this.jsonRequestHandler_init = true;
        this.jsonRequestHandler = jrh;
    }
    
    private JSONRequestHandler getJSONRequestHandler()
    {
        if (!this.jsonRequestHandler_init) {
            this.jsonRequestHandler_init = true;
            String jsonHandlerClassName = this.getProperties().getString(PROP_jsonRequestHandlerClass,null);
            if (!StringTools.isBlank(jsonHandlerClassName)) {
                try {
                    Class jsonHandlerClass = Class.forName(jsonHandlerClassName);
                    this.jsonRequestHandler = (JSONRequestHandler)jsonHandlerClass.newInstance();
                    Print.logInfo("Loaded JSONRequestHandler ["+this.getPageName()+"]: " + StringTools.className(this.jsonRequestHandler));
                } catch (Throwable th) {
                    Print.logException("Unable to create JSONRequestHandler ["+this.getPageName()+"]", th);
                }
            } else {
                Print.logInfo("No JSONRequestHandler defined for page ["+this.getPageName()+"]");
            }
        }
        return this.jsonRequestHandler; // may be null
    }

    public JSON handleJSONRequest(String context, RequestProperties reqState, JSON jsonObj)
    {

        /* get handler */
        JSONRequestHandler jrh = this.getJSONRequestHandler();
        if (jrh == null) {
            return null;
        }

        /* call handler */
        try {
            return jrh.handleJSONRequest(context, reqState, jsonObj);
        } catch (Throwable th) {
            Print.logException("JSON Request Handler error", th);
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean            fileUploadHandler_init  = false;
    private FileUploadHandler  fileUploadHandler       = null;
    
    private void setFileUploadHandler(FileUploadHandler fuh)
    {
        this.fileUploadHandler_init = true;
        this.fileUploadHandler = fuh;
    }
    
    private FileUploadHandler getFileUploadHandler()
    {
        if (!this.fileUploadHandler_init) {
            this.fileUploadHandler_init = true;
            String fileHandlerClassName = this.getProperties().getString(PROP_fileUploadHandlerClass,null);
            if (!StringTools.isBlank(fileHandlerClassName)) {
                try {
                    Class fileHandlerClass = Class.forName(fileHandlerClassName);
                    this.fileUploadHandler = (FileUploadHandler)fileHandlerClass.newInstance();
                    Print.logInfo("Loaded FileUploadHandler ["+this.getPageName()+"]: " + StringTools.className(this.fileUploadHandler));
                } catch (Throwable th) {
                    Print.logException("Unable to create FileUploadHandler ["+this.getPageName()+"]", th);
                }
            } else {
                Print.logInfo("No FileUploadHandler defined for page ["+this.getPageName()+"]");
            }
        }
        return this.fileUploadHandler; // may be null
    }

    public String handleFileUpload(
        String context, RequestProperties reqState, 
        RTProperties mp)
    {

        /* get handler */
        FileUploadHandler fuh = this.getFileUploadHandler();
        if (fuh == null) {
            return null;
        }

        /* call handler */
        try {

            /* found Multipart-Mime attributes? */
            if (mp == null) {
                Print.logError("Upload: No Multipart-Mime attributes!!!");
                return fuh.handleFileUpload(
                    context, reqState,
                    null/*mimeName*/, null/*mimeContType*/, null/*mimeContDisp*/, 
                    null/*mimeFile*/, null/*mimeBytes*/);
            }

            /* get upload file(s) */
            for (Object mpKey : mp.getPropertyKeys()) {

                /* get MimePart */
                Object upld = mp.getProperty(mpKey, null);
                if (!(upld instanceof AttributeTools.MimePart)) {
                    continue;
                }
                AttributeTools.MimePart uploadMime = (AttributeTools.MimePart)upld;

                /* call handler to upload file */
                // only one file upload allowed
                String mimeName     = uploadMime.getString(AttributeTools.MIMEPART_NAME,"");
                String mimeContType = uploadMime.getString("content-type",null);
                String mimeContDisp = uploadMime.getString("content-disposition",null);
                String mimeFile     = uploadMime.getString(AttributeTools.MIMEPART_FILENAME,"");
                byte   mimeBytes[]  = uploadMime.getByteArray(AttributeTools.MIMEPART_BYTES,new byte[0]);
                if (!StringTools.isBlank(mimeFile) && !ListTools.isEmpty(mimeBytes)) {
                    return fuh.handleFileUpload(
                        context, reqState,
                        mimeName, mimeContType, mimeContDisp, 
                        mimeFile, mimeBytes);
                }

            }

            /* nothing found */
            Print.logError("No File upload attributes!!!");
            return fuh.handleFileUpload(
                context, reqState,
                null/*mimeName*/, null/*mimeContType*/, null/*mimeContDisp*/, 
                null/*mimeFile*/, null/*mimeBytes*/);

        } catch (Throwable th) {
            Print.logException("File Upload Handler error", th);
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean             fileDownloadHandler_init  = false;
    private FileDownloadHandler fileDownloadHandler       = null;
    
    private void setFileDownloadHandler(FileDownloadHandler fuh)
    {
        this.fileDownloadHandler_init = true;
        this.fileDownloadHandler = fuh;
    }
    
    private FileDownloadHandler getFileDownloadHandler()
    {
        if (!this.fileDownloadHandler_init) {
            this.fileDownloadHandler_init = true;
            String fileHandlerClassName = this.getProperties().getString(PROP_fileDownloadHandlerClass,null);
            if (!StringTools.isBlank(fileHandlerClassName)) {
                try {
                    Class fileHandlerClass = Class.forName(fileHandlerClassName);
                    this.fileDownloadHandler = (FileDownloadHandler)fileHandlerClass.newInstance();
                    Print.logInfo("Loaded FileDownloadHandler ["+this.getPageName()+"]: " + StringTools.className(this.fileDownloadHandler));
                } catch (Throwable th) {
                    Print.logException("Unable to create FileDownloadHandler ["+this.getPageName()+"]", th);
                }
            } else {
                Print.logInfo("No FileDownloadHandler defined for page ["+this.getPageName()+"]");
            }
        }
        return this.fileDownloadHandler; // may be null
    }

    public boolean handleFileDownload(
        String context, RequestProperties reqState)
    {

        /* get handler */
        FileDownloadHandler fdh = this.getFileDownloadHandler();
        if (fdh == null) {
            return false;
        }

        /* call handler */
        try {
            return fdh.handleFileDownload(context, reqState);
        } catch (Throwable th) {
            Print.logException("JSON Request Handler error", th);
            return false;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        Track.setDisplayRequest(true);

        /* get html file name */
        String cmd = reqState.getCommandName(); // "html", "json"
        String arg = reqState.getCommandArg();

        /* JSON */
        if (cmd.equalsIgnoreCase("json")) {
            HttpServletRequest  request  = reqState.getHttpServletRequest();
            HttpServletResponse response = reqState.getHttpServletResponse();
            // parse request
            JSON jsonReq = null;
            try {
                byte jsonBytes[] = FileTools.readStream(request.getInputStream());
                String jsonStr = StringTools.toStringValue(jsonBytes).trim();
                Print.logInfo("JSON request:\n" + jsonStr);
                jsonReq = new JSON(jsonStr);
            } catch (Throwable th) { // JSON.JSONParseException, etc
                Print.logException("JSON request exception", th);
            }
            // get response
            String context = !StringTools.isBlank(arg)? arg : this.getPageName();
            JSON  jsonResp = this.handleJSONRequest(context, reqState, jsonReq);
            if (jsonResp != null) {
                //CommonServlet.setResponseContentType(response, HTMLTools.MIME_JSON());
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                PrintWriter out = response.getWriter();
                String jsonRespStr = (jsonResp != null)? jsonResp.toString() : "";
                Print.logInfo("JSON response:\n" + jsonRespStr);
                out.println(jsonRespStr);
                out.close();
            } else {
                // we assume that the JSON request handler had other intentions
            }
            return; 
        }

        /* Upload */
        if (cmd.equalsIgnoreCase("upload")) {
            HttpServletRequest  request  = reqState.getHttpServletRequest();
            HttpServletResponse response = reqState.getHttpServletResponse();
            // get response
            String context  = !StringTools.isBlank(arg)? arg : this.getPageName();
            String fileResp = this.handleFileUpload(
                context, reqState,
                AttributeTools.getMultipartProperties(request));
            if (fileResp != null) {
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                PrintWriter out = response.getWriter();
                Print.logInfo("File upload response:\n" + fileResp);
                out.println(fileResp);
                out.close();
                return;
            }
        }

        /* Download */
        if (cmd.equalsIgnoreCase("download") || 
            cmd.equalsIgnoreCase("file")       ) {
            HttpServletRequest  request  = reqState.getHttpServletRequest();
            HttpServletResponse response = reqState.getHttpServletResponse();
            // get response
            String context  = !StringTools.isBlank(arg)? arg : this.getPageName();
            boolean downloadResp = this.handleFileDownload(
                context, reqState);
            if (downloadResp) {
                return;
            }
        }

        /* get html file */
        String htmlFileName = this.getProperties().getString(PROP_htmlFile,null);
        //Print.logInfo("'htmlFile' ==> " + htmlFileName);
        if (StringTools.isBlank(htmlFileName) || 
            (htmlFileName.equalsIgnoreCase("arg") && !StringTools.isBlank(arg))) {
            htmlFileName = arg;
        }

        /* get HTML page */
        final String htmlString;
        File rootDir = RTConfig.getServletContextPath(); // may return null
        if (rootDir == null) {
            htmlString = "";
            Print.logInfo("HTML file directory not found");
        } else
        if (StringTools.isBlank(htmlFileName)) {
            htmlString = "";
            Print.logInfo("HTML file is blank");
        } else {
            File htmlAbsPath = new File(rootDir, htmlFileName);
            if (htmlAbsPath.isFile()) {
                htmlString = StringTools.toStringValue(FileTools.readFile(htmlAbsPath));
                //Print.logInfo("HTML file found: " + htmlAbsPath);
            } else {
                htmlString = "";
                Print.logInfo("HTML file not found: " + htmlAbsPath);
            }
        }

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            new HTMLOutput() {
                public void write(PrintWriter out) throws IOException {
                    out.write(htmlString);
                }
            }
        );

    }

    // ------------------------------------------------------------------------

}
