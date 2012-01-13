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
//      Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;
import java.net.*;

public interface ExcelAPI
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final int     CellStyle_ALIGN_LEFT        =  1; // (int)CellStyle.ALIGN_LEFT;
    public static final int     CellStyle_ALIGN_CENTER      =  2; // (int)CellStyle.ALIGN_CENTER;
    public static final int     CellStyle_ALIGN_RIGHT       =  3; // (int)CellStyle.ALIGN_RIGHT;
    public static final int     CellStyle_VERTICAL_TOP      =  0; // (int)CellStyle.VERTICAL_TOP;
    public static final int     CellStyle_VERTICAL_CENTER   =  1; // (int)CellStyle.VERTICAL_CENTER;
    public static final int     CellStyle_VERTICAL_BOTTOM   =  2; // (int)CellStyle.VERTICAL_BOTTOM;
    public static final int     CellStyle_SOLID_FOREGROUND  =  1; // (int)CellStyle.SOLID_FOREGROUND;
    public static final int     CellStyle_BORDER_NONE       =  0; // (int)CellStyle.BORDER_NONE;
    public static final int     CellStyle_BORDER_THIN       =  1; // (int)CellStyle.BORDER_THIN;
    public static final int     CellStyle_BORDER_THICK      =  5; // (int)CellStyle.BORDER_THICK;
   
    public static final int     Font_BOLDWEIGHT_BOLD        = 700; // (int)Font.BOLDWEIGHT_BOLD;
    public static final int     Font_BOLDWEIGHT_NORMAL      = 400; // (int)Font.BOLDWEIGHT_NORMAL;

    public static final int     Font_POINT_9                =  9;
    public static final int     Font_POINT_10               = 10;
    public static final int     Font_POINT_11               = 11;
    public static final int     Font_POINT_12               = 12;
    public static final int     Font_POINT_14               = 14;
    public static final int     Font_POINT_16               = 16;
    public static final int     Font_POINT_18               = 18;
    public static final int     Font_POINT_20               = 20;
 
    public static final boolean Text_WRAP                   = true;
    public static final boolean Text_NOWRAP                 = false;
    
    // ------------------------------------------------------------------------

    public void init(boolean xlsx, String name);
    
    public void setTitle(int rowIndex, String title, int colSpan);
    public void setSubtitle(int rowIndex, String title, int colSpan);
    
    public void addHeaderColumn(int rowIndex, int colIndex, String colTitle, int charWidth);
    
    public void addBodyColumn(int rowIndex, int colIndex, Object value);
    public void addSubtotalColumn(int rowIndex, int colIndex, Object value);
    public void addTotalColumn(int rowIndex, int colIndex, Object value);
    
    public boolean write(File dir);
    public boolean write(OutputStream out);
    
    // ------------------------------------------------------------------------

}
