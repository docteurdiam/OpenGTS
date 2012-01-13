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
//  2009/11/10  Martin D. Flynn
//     -Initial Creation
// ----------------------------------------------------------------------------

var MAX_COMMAND_ARGS   = 10;
var COMMAND_ARG_PREFIX = "rct_";

// Onload 
function devCommandOnLoad() {
    devCommandRadioChanged();
}

// Command radio button selection changed
function devCommandRadioChanged() {
   //try {
      if (document.DeviceCommandForm.cmdRadioSel.length) {
         var rc = document.DeviceCommandForm.cmdRadioSel.length;
         //alert("Radio selection changed ... " + rc);
         for (var i = 0; i < rc; i++) {
            var cmdName = document.DeviceCommandForm.cmdRadioSel[i].value;
            var cmdChkd = document.DeviceCommandForm.cmdRadioSel[i].checked;
            //alert("Command: " + i + " " + cmdName);
            for (var a = 0; a < MAX_COMMAND_ARGS; a++) {
                var cmdOptn = document.getElementById(COMMAND_ARG_PREFIX + cmdName + '_' + a);
                if (!cmdOptn) { continue; }
                //alert("Radio selection changed ... " + i + " " + cmdChkd);
                if (cmdChkd) {
                   cmdOptn.disabled  = false;
                   cmdOptn.className = "textInput";
                } else {
                   cmdOptn.disabled  = true;
                   cmdOptn.className = "textReadOnly";
                }
            }
         }
      }
   //} catch (e) {
      //
   //}
}
