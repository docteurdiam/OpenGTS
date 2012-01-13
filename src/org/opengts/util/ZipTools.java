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
//  02/19/2006  Martin D. Flynn
//      Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.net.*;
import java.security.cert.Certificate;

public class ZipTools
{
    
    /**
    *** Get Zip table of contents
    **/
    public static java.util.List<String> getTableOfContents(byte zipData[])
    {
        java.util.List<String> toc = new Vector<String>();
        ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(bais);
            for (;;) {
                ZipEntry ze = zis.getNextEntry();
                if (ze == null) { break; }
                toc.add(ze.getName());
                zis.closeEntry();
            }
        } catch (IOException ioe) {
            Print.logError("Reading Zip: " + ioe);
        } finally {
            try { zis.close();  } catch (Throwable th) {}
            try { bais.close(); } catch (Throwable th) {}
        }
        return toc;
    }
    
    /**
    *** Get file data 
    **/
    public static byte[] readEntry(byte zipData[], String name)
    {
        byte data[] = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(bais);
            for (;;) {
                ZipEntry ze = zis.getNextEntry();
                if (ze == null) {
                    break;
                } else
                if (name.equals(ze.getName())) {
                    byte d[] = new byte[10*1024];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (;;) {
                        int size = zis.read(d,0,d.length);
                        if (size < 0) { break; }
                        baos.write(d,0,size);
                    }
                    data = baos.toByteArray();
                    zis.closeEntry();
                    break;
                }
                zis.closeEntry();
            }
        } catch (IOException ioe) {
            Print.logError("Reading Zip: " + ioe);
        } finally {
            try { zis.close();  } catch (Throwable th) {}
            try { bais.close(); } catch (Throwable th) {}
        }
        return data;
    }
    
}
