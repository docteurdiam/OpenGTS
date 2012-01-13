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
//  2007/03/30  Martin D. Flynn
//     -Initial release
//  2007/07/14  Martin D. Flynn
//     -Moved to "org.opengts.db"
//  2008/05/20  Martin D. Flynn
//     -Made the access level an enumerated type.
//  2008/10/16  Martin D. Flynn
//     -Changed AccessLevel descriptions
//     -Added support for valid AccessLevel values.
//  2010/07/18  Martin D. Flynn
//     -Added check to 'getDescription' to replace "${dcsDesc}" with the ACL
//      DCS description (only on ACLs starting with "acl.dcs.").
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class AclEntry
    implements Cloneable
{
    
    // ------------------------------------------------------------------------

    /* used for replacing the DCS name with the customized description */
    private static final String ACL_DCS_        = "acl.dcs.";

    /* used for authorizing "Service" commands */
    public  static final String ACL_SERVICE_    = "acl.service.";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public enum AccessLevel implements EnumTools.StringLocale, EnumTools.IntValue, EnumTools.IsDefault {
        UNDEFINED   (-1, I18N.getString(AclEntry.class,"AclEntry.access.undefined","Undefined" )),
        NONE        ( 0, I18N.getString(AclEntry.class,"AclEntry.access.none"     ,"None"      )),
        READ        ( 1, I18N.getString(AclEntry.class,"AclEntry.access.readView" ,"Read/View" )),
        WRITE       ( 2, I18N.getString(AclEntry.class,"AclEntry.access.writeEdit","Write/Edit")),
        ALL         ( 3, I18N.getString(AclEntry.class,"AclEntry.access.newDelete","New/Delete"));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        AccessLevel(int v, I18N.Text a)             { vv = v; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(NONE); }
        public boolean okRead()                     { return (this.getIntValue() >= READ.getIntValue()); }
        public boolean okWrite()                    { return (this.getIntValue() >= WRITE.getIntValue()); }
        public boolean okAll()                      { return (this.getIntValue() >= ALL.getIntValue()); }
        public boolean isUndefined()                { return this.equals(UNDEFINED); }
    };
    
    public static AccessLevel getAccessLevel(int accessLevel)
    {
        return EnumTools.getValueOf(AccessLevel.class, accessLevel);
    }
    
    public static AccessLevel getAccessLevel(String accessLevel, Locale locale)
    {
        return EnumTools.getValueOf(AccessLevel.class, accessLevel, locale);
    }
    
    public static AccessLevel parseAccessLevel(String accStr, AccessLevel dft)
    {
        int accInt = StringTools.parseInt(accStr, (dft != null)? dft.getIntValue() : AccessLevel.READ.getIntValue());
        if (accInt > AccessLevel.ALL.getIntValue()) { 
            accInt = AccessLevel.ALL.getIntValue(); 
        } else
        if (accInt < AccessLevel.NONE.getIntValue()) { 
            accInt = AccessLevel.NONE.getIntValue(); 
        }
        return EnumTools.getValueOf(AccessLevel.class,accInt); 
    }

    // ------------------------------------------------------------------------

    private static final AccessLevel ACCESS_VALUES_NONE[] = new AccessLevel[] {
        AccessLevel.NONE, 
    };

    private static final AccessLevel ACCESS_VALUES_READ[] = new AccessLevel[] {
        AccessLevel.NONE, 
        AccessLevel.READ, 
    };

    private static final AccessLevel ACCESS_VALUES_WRITE[] = new AccessLevel[] {
        AccessLevel.NONE, 
        AccessLevel.READ, 
        AccessLevel.WRITE, 
    };
    
    private static final AccessLevel ACCESS_VALUES_ALL[] = new AccessLevel[] {
        AccessLevel.NONE, 
        AccessLevel.READ, 
        AccessLevel.WRITE, 
        AccessLevel.ALL,
    };

    public static AccessLevel[] GetValueListForMaximumAccessLevel(AccessLevel maxAcc)
    {
        if (maxAcc != null) {
            return ACCESS_VALUES_ALL;
        } else {
            switch (maxAcc) {
                case NONE   : return ACCESS_VALUES_NONE;
                case READ   : return ACCESS_VALUES_READ;
                case WRITE  : return ACCESS_VALUES_WRITE;
                default     : return ACCESS_VALUES_ALL;
            }
        }
    }

    // ------------------------------------------------------------------------

    public static boolean okRead(AccessLevel level)
    {
        return (level != null) && AclEntry.okRead(level.getIntValue());
    }

    public static boolean okRead(int level)
    {
        return (level >= AccessLevel.READ.getIntValue());
    }

    public static boolean okWrite(AccessLevel level)
    {
        return (level != null) && AclEntry.okWrite(level.getIntValue());
    }

    public static boolean okWrite(int level)
    {
        return (level >= AccessLevel.WRITE.getIntValue());
    }

    public static boolean okAll(AccessLevel level)
    {
        return (level != null) && AclEntry.okAll(level.getIntValue());
    }

    public static boolean okAll(int level)
    {
        return (level >= AccessLevel.ALL.getIntValue());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public  static final String SUBACL_SEPARATOR = ":";

    public static String CreateAclName(String aclName, String subAcl)
    {
        if (StringTools.isBlank(aclName)) {
            return "";
        } else
        if (StringTools.isBlank(subAcl)) {
            return aclName;
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append(aclName);
            sb.append(AclEntry.SUBACL_SEPARATOR);
            sb.append(subAcl);
            return sb.toString();
        }
    }

    public static String[] ParseAclName(String aclName)
    {
        if (StringTools.isBlank(aclName)) {
            return new String[] { "", "" };
        } else {
            int p = aclName.indexOf(AclEntry.SUBACL_SEPARATOR);
            if (p < 0) {
                return new String[] { aclName, "" };
            } else {
                return new String[] { aclName.substring(0,p), aclName.substring(p+1) };
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String      aclName     = "";
    private I18N.Text   aclDesc     = null;
    private AccessLevel values[]    = ACCESS_VALUES_ALL;
    private AccessLevel maxLevel    = AccessLevel.ALL;
    private AccessLevel dftLevel    = AccessLevel.NONE;
    private boolean     isHidden    = false;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param name   The name associated with the AclEntry
    *** @param desc   The description of the AclEntry
    *** @param values The valid access level values
    **/
    public AclEntry(String name, I18N.Text desc, AccessLevel values[], AccessLevel dftLvl)
    {

        /* name/description */
        this.aclName  = (name != null)? name : "";
        this.aclDesc  = desc;

        /* values */
        this.values   = !ListTools.isEmpty(values)? values : ACCESS_VALUES_ALL;
        this.maxLevel = this.values[this.values.length - 1];

        /* default level */
        this.setDefaultAccessLevel(dftLvl);

    }

    /**
    *** Constructor
    *** @param name   The name associated with the AclEntry
    *** @param desc   The description of the AclEntry
    *** @param maxLvl The maximum access level
    **/
    public AclEntry(String name, I18N.Text desc, AccessLevel maxLvl, AccessLevel dftLvl)
    {

        /* name/description */
        this.aclName  = (name != null)? name : "";
        this.aclDesc  = desc;

        /* values */
        this.maxLevel = (maxLvl != null)? maxLvl : AccessLevel.ALL;
        this.values   = GetValueListForMaximumAccessLevel(this.maxLevel);

        /* default level */
        this.setDefaultAccessLevel(dftLvl);

    }

    /**
    *** Copy Constructor
    *** @param aclEntry The other AclEntry instance to copy
    **/
    public AclEntry(AclEntry aclEntry)
    {
        if (aclEntry != null) {
            this.aclName  = (aclEntry.aclName != null)? aclEntry.aclName : "";
            this.aclDesc  = aclEntry.aclDesc;
            this.values   = aclEntry.values;
            this.maxLevel = aclEntry.maxLevel;
            this.dftLevel = aclEntry.dftLevel;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of this AclEntry
    *** @return The name of this AclEntry
    **/
    public String getName()
    { // ie. "acl.dsc.enfora:LocateNow"
        return this.aclName;
    }

    /**
    *** Gets the description of this AclEntry
    *** @return The description of this AclEntry
    **/
    public String getDescription(Locale loc)
    {
        if (this.aclDesc == null) {
            return "";
        } else {
            String aclDesc = this.aclDesc.toString(loc);
            if (this.aclName.startsWith(ACL_DCS_)) {
                // a bit of hackery to replace the key "${dcsDesc}" with the custom DCS "server" description
                // ie. <Acl name="acl.dcs.enfora:LocateNow">${dcsDesc} Locate Now</Acl>
                String k_dcsDesc = "${dcsDesc}";
                int p = aclDesc.indexOf(k_dcsDesc);
                if (p >= 0) {
                    int c = this.aclName.indexOf(AclEntry.SUBACL_SEPARATOR); // find ":"
                    String dcs_n = (c > 0)?
                        this.aclName.substring(ACL_DCS_.length(),c) :
                        this.aclName.substring(ACL_DCS_.length())   ; // extract "server"
                    DCServerConfig dcs_c = DCServerFactory.getServerConfig(dcs_n);
                    String dcs_d = (dcs_c != null)? dcs_c.getDescription() : dcs_n.toUpperCase(); // DCS description
                    aclDesc = aclDesc.substring(0,p) + dcs_d + aclDesc.substring(p + k_dcsDesc.length()); // insert into ACL description
                }
            }
            return aclDesc;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the 'hidden' state of this AclEntry
    *** @param hidden  True to set this AclEntry hidden
    **/
    public void setHidden(boolean hidden)
    {
        this.isHidden = hidden;
    }
    
    /**
    *** Returns the 'hidden' state of this AclEntry
    *** @return The 'hidden' state of this AclEntry
    **/
    public boolean isHidden()
    {
        return this.isHidden;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the access level values for this AclEntry
    *** @return The access level values for this AclEntry (does not return null)
    **/
    public AccessLevel[] getAccessLevelValues()
    {
        if (this.values != null) {
            return this.values;
        } else
        if (this.maxLevel != null) {
            return AclEntry.GetValueListForMaximumAccessLevel(this.maxLevel);
        } else {
            return ACCESS_VALUES_ALL;
        }
    }

    /**
    *** Gets the maximum access level for this AclEntry
    *** @return The maximum access level for this AclEntry (does not return null)
    **/
    public AccessLevel getMaximumAccessLevel()
    {
        if (this.maxLevel != null) {
            return this.maxLevel;
        } else
        if (!ListTools.isEmpty(this.values)) {
            return this.values[this.values.length - 1];
        } else {
            return AccessLevel.ALL;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default access level for this AclEntry
    *** @param dft  The default access level for this AclEntry
    **/
    public void setDefaultAccessLevel(AccessLevel dft)
    {
        if (dft != null) {
            this.dftLevel = dft;
            AccessLevel maxLvl = this.getMaximumAccessLevel();
            if (this.dftLevel.getIntValue() > maxLvl.getIntValue()) {
                this.dftLevel = maxLvl;
            }
        } else {
            this.dftLevel = this.getMaximumAccessLevel();
        }
    }

    /**
    *** Gets the default access level for this AclEntry
    *** @return The default access level for this AclEntry (does not return null)
    **/
    public AccessLevel getDefaultAccessLevel()
    {
        return (this.dftLevel != null)? this.dftLevel : AccessLevel.NONE;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns a String representation of this AclEntry
    *** @return A String representation of this AclEntry
    **/
    public String toString()
    {
        return this.getName();
    }
    
    /**
    *** Returns true if the specified Object is equal to this AclEntry
    *** @param other  The other Object to compare for equals
    *** @return True if the Objects are equivalent, false otherwise
    **/
    public boolean equals(Object other)
    {
        if (other instanceof AclEntry) {
            return this.toString().equals(other.toString());
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns a clone of this AclEntry
    *** @return A clone of this AclEntry
    **/
    public Object clone()
    {
        return new AclEntry(this);
    }
    
    // ------------------------------------------------------------------------

}
