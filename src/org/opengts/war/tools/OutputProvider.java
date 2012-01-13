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
//  2011/01/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class OutputProvider
{
    
    // ------------------------------------------------------------------------
    
    private Object       provider           = null;

    private OutputStream stream             = null;
    private PrintWriter  writer             = null;

    public OutputProvider()
    {
        // Null output provider
    }

    public OutputProvider(Object provider)
    {
        // "HttpServletResponse"
        this.provider = provider;
    }

    public OutputProvider(OutputStream stream)
    {
        this.stream = stream;
    }
    
    public OutputProvider(PrintWriter writer)
    {
        this.writer = writer;
    }
    
    // ------------------------------------------------------------------------
    
    public boolean hasOutputStream()
    {
        return (this.stream != null);
    }

    public OutputStream getOutputStream()
        throws IOException, IllegalStateException
    {
        if (this.stream != null) {
            return this.stream;
        } else 
        if (this.writer != null) {
            // can't create OutputStream from PrintWriter 
            throw new IllegalStateException("'stream' already defined");
        } else 
        if (this.provider != null) {
            try {
                MethodAction ma = new MethodAction(this.provider, "getOutputStream");
                this.stream = (OutputStream)ma.invoke();
                return this.stream;
            } catch (IOException ioe) {
                throw ioe;
            } catch (Throwable th) {
                // ignore
            }
        }
        return null;
    }
   
    // ------------------------------------------------------------------------
    
    public boolean hasWriter()
    {
        return (this.writer != null);
    }

    public PrintWriter getWriter()
        throws IOException, IllegalStateException
    {
        if (this.writer != null) {
            return this.writer;
        } else 
        if (this.stream != null) {
            // create PrintWriter from OutputStream
            //throw new IllegalStateException("'writer' already defined");
            this.writer = new PrintWriter(this.stream);
            return this.writer;
        } else
        if (this.provider != null) {
            try {
                MethodAction ma = new MethodAction(this.provider, "getWriter");
                this.writer = (PrintWriter)ma.invoke();
                return this.writer;
            } catch (IOException ioe) {
                throw ioe;
            } catch (Throwable th) {
                // ignore
            }
        }
        return null;
    }
    
    // ------------------------------------------------------------------------

}
