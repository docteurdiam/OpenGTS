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
//  2008/05/14  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.db.tables.*;

public interface UserInformation
{

    public String getEncodedPassword();
    public void setDecodedPassword(String enteredPass);
    public boolean checkPassword(String enteredPass);
    
    //public int getGender();
    //public void setGender(int gender);

    public String getContactName();
    public void setContactName(String v);

    public String getContactPhone();
    public void setContactPhone(String v);

    public String getContactEmail();
    public void setContactEmail(String v);

    public String getTimeZone();
    public void setTimeZone(String v);

    public long getPasswdQueryTime();
    public void setPasswdQueryTime(long v);

    public long getLastLoginTime();
    public void setLastLoginTime(long v);

}
