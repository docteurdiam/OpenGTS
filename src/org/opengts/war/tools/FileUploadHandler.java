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
package org.opengts.war.tools;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;

public interface FileUploadHandler
{

    /**
    *** Handle a File Upload request
    *** @param context              The "context" of the File Upload
    *** @param name                 The MIME name
    *** @param contentType          The MIME "content-type" value
    *** @param contentDisposition   The MIME "content-disposition" value
    *** @param fileName             The MIME upload file name
    *** @param fileBytes            The MIME upload file bytes
    *** @return The response String
    **/
    public String handleFileUpload(
        String context, RequestProperties reqState, 
        String name, String contentType, String contentDisposition,
        String fileName, byte fileBytes[]);

}
