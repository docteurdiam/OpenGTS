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
//  2007/11/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public abstract class EntityManager
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Entity type

    public enum EntityType implements EnumTools.StringLocale, EnumTools.IntValue {
        TRAILER     (    0, "TRAILER"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.trailer"  ,"Trailer"    )), // default
        DRIVER      (  100, "DRIVER"    , I18N.getString(EntityManager.class,"EntityManager.EntityType.driver"   ,"Driver"     )),
        PERSON      (  200, "PERSON"    , I18N.getString(EntityManager.class,"EntityManager.EntityType.person"   ,"Person"     )),
        ANIMAL      (  300, "ANIMAL"    , I18N.getString(EntityManager.class,"EntityManager.EntityType.animal"   ,"Animal"     )),
        CONTAINER   (  400, "CONTAINER" , I18N.getString(EntityManager.class,"EntityManager.EntityType.container","Container"  )),
        PACKAGE     (  500, "PACKAGE"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.package"  ,"Package"    )),
        TOOL        (  600, "TOOL"      , I18N.getString(EntityManager.class,"EntityManager.EntityType.tool"     ,"Tool"       )),
        EQUIPMENT   (  700, "EQUIPMENT" , I18N.getString(EntityManager.class,"EntityManager.EntityType.equipment","Equipment"  )),
        RFID_00     (  900, "RFID_00"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_0"   ,"RFID Type 0")),
        RFID_01     (  901, "RFID_01"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_1"   ,"RFID Type 1")),
        RFID_02     (  902, "RFID_02"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_2"   ,"RFID Type 2")),
        RFID_03     (  903, "RFID_03"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_3"   ,"RFID Type 3")),
        RFID_04     (  904, "RFID_04"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_4"   ,"RFID Type 4"));
        // ---
        private int         vv = 0;
        private String      nn = "";
        private I18N.Text   aa = null;
        EntityType(int v, String n, I18N.Text a)    { vv = v; nn = n; aa = a; }
        public String  getName()                    { return nn; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isTrailer()                  { return this.equals(TRAILER); }
        public boolean isDriver()                   { return this.equals(DRIVER); }
        public boolean isType(int type)             { return this.getIntValue() == type; }
    }

    public static EntityType getDefaultEntityType()
    {
        return EnumTools.getDefault(EntityType.class);
    }

    public static EntityType getEntityType(EntityType et)
    {
        return (et != null)? et : EntityManager.getDefaultEntityType();
    }

    public static EntityType getEntityTypeFromName(String et)
    {
        if (!StringTools.isBlank(et)) {
            if (et.equalsIgnoreCase("TRAILER"  )) { return EntityType.TRAILER;   } 
            if (et.equalsIgnoreCase("DRIVER"   )) { return EntityType.DRIVER;    } 
            if (et.equalsIgnoreCase("PERSON"   )) { return EntityType.PERSON;    } 
            if (et.equalsIgnoreCase("ANIMAL"   )) { return EntityType.ANIMAL;    } 
            if (et.equalsIgnoreCase("CONTAINER")) { return EntityType.CONTAINER; } 
            if (et.equalsIgnoreCase("PACKAGE"  )) { return EntityType.PACKAGE;   } 
            if (et.equalsIgnoreCase("TOOL"     )) { return EntityType.TOOL;      } 
            if (et.equalsIgnoreCase("EQUIPMENT")) { return EntityType.EQUIPMENT; } 
            if (et.equalsIgnoreCase("EQUIP"    )) { return EntityType.EQUIPMENT; } 
            if (et.equalsIgnoreCase("RFID"     )) { return EntityType.RFID_00;   } 
            if (et.equalsIgnoreCase("RFID_00"  )) { return EntityType.RFID_00;   } 
            if (et.equalsIgnoreCase("RFID_0"   )) { return EntityType.RFID_00;   } 
            if (et.equalsIgnoreCase("RFID_01"  )) { return EntityType.RFID_01;   } 
            if (et.equalsIgnoreCase("RFID_1"   )) { return EntityType.RFID_01;   } 
            if (et.equalsIgnoreCase("RFID_02"  )) { return EntityType.RFID_02;   } 
            if (et.equalsIgnoreCase("RFID_2"   )) { return EntityType.RFID_02;   } 
            if (et.equalsIgnoreCase("RFID_03"  )) { return EntityType.RFID_03;   } 
            if (et.equalsIgnoreCase("RFID_3"   )) { return EntityType.RFID_03;   } 
            if (et.equalsIgnoreCase("RFID_04"  )) { return EntityType.RFID_04;   }
            if (et.equalsIgnoreCase("RFID_4"   )) { return EntityType.RFID_04;   }
        }
        return EntityManager.getDefaultEntityType();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public abstract boolean insertEntityChange(EventData event);
    
    public abstract String[] getAttachedEntityDescriptions(String accountID, String deviceID, long entityType) 
        throws DBException;

    public abstract String getEntityDescription(String accountID, String entityID, long entityType);
    
    // ------------------------------------------------------------------------

}
