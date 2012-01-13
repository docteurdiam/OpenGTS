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
//  2010/09/09  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.Account;
import org.opengts.db.tables.Device;

public abstract class HierarchyRecord<RT extends DBRecord>
    extends AccountRecord<RT>
{

    // ------------------------------------------------------------------------
    
    public static final String HIERARCHY_SEPARATOR      = "/";
    public static final char   HIERARCHY_SEPARATOR_CHAR = '/';
    
    public static final String TYPE_HIER_KEY            = DBField.TYPE_STRING(512);
    public static final String TYPE_ITEM_ID             = DBField.TYPE_STRING(50);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* field definition */
    public static final String FLD_hierarchyKey         = "hierarchyKey";
    public static final String FLD_level                = "level";
    /*
    public static final String FLD_itemID               = "itemID";
    protected static DBField FieldInfo[] = {
        // Group fields
        newField_accountID(true),
        new DBField(FLD_hierarchyKey    , String.class  , TYPE_HIER_KEY, "Hierarchy Key" , "key=true"),
        new DBField(FLD_itemID          , String.class  , TYPE_ITEM_ID , "Item ID"       , "key=true"),
        // Common fields
        newField_displayName(),
        newField_description(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };
    */
    
    // ----

    /* create a new "hierarchyKey" key field definition */
    protected static DBField newField_hierarchyKey(boolean key)
    {
        return HierarchyRecord.newField_hierarchyKey(key, null);
    }

    /* create a new "hierarchyKey" key field definition */
    protected static DBField newField_hierarchyKey(boolean key, String xAttr)
    {
        String attr = (key?"key=true":"edit=2") + (StringTools.isBlank(xAttr)?"":(" " + xAttr));
        return new DBField(FLD_hierarchyKey, String.class, TYPE_HIER_KEY, "Hierarchy Key", attr);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static abstract class HierarchyKey<RT extends DBRecord>
        extends AccountKey<RT>
    {
        public HierarchyKey() {
            super();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Bean instance */
    public HierarchyRecord()
    {
        super();
    }

    /* database record */
    public HierarchyRecord(HierarchyKey<RT> key)
    {
        super(key);
    }
         
    // ------------------------------------------------------------------------

    /* hierarchy Key */
    public String getHierarchyKey()
    {
        String v = (String)this.getFieldValue(FLD_hierarchyKey);
        return (v != null)? v : "";
    }
    
    public void setHierarchyKey(String v)
    {
        this.setFieldValue(FLD_hierarchyKey, ((v != null)? v : ""));
    }

    /* get hierarhy */
    public String[] getHierarchy()
    {
        String hk = this.getHierarchyKey();
        return StringTools.split(hk, HIERARCHY_SEPARATOR_CHAR);
    }

    /* set hierarchy */
    public void setHierarchy(String h[]) 
    {
        String hk = StringTools.join(h, HIERARCHY_SEPARATOR_CHAR);
        this.setHierarchyKey(hk);
    }

    // ------------------------------------------------------------------------

    public int getLevel()
    {
        Integer v = (Integer)this.getFieldValue(FLD_level);
        return (v != null)? v.intValue() : 0;
    }
    
    public void setLevel(int v)
    {
        this.setFieldValue(FLD_level, v);
    }

    // ------------------------------------------------------------------------

    /* Item ID */
    public abstract String getItemID();
    public abstract void setItemID(String v);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static <RT extends HierarchyRecord> TreeNode/*<RT>*/ readHiearchy(DBFactory<RT> dbFact, Account acct)
        throws DBException
    {

        /* no account specified? */
        if (acct == null) {
            return null;
        }
        String acctID = acct.getAccountID();

        /* TreeNode record handler */
        final TreeNode/*<RT>*/ parent = new TreeNode/*<RT>*/(acctID);
        final DBRecordHandler<RT> hierRH = new DBRecordHandler<RT>() {
            public int handleDBRecord(RT rcd) throws DBException {
                RT hr = rcd;

                /* tree path */
                String hierKey = hr.getHierarchyKey();
                TreeNode/*<RT>*/ itemParent = parent;
                if (!StringTools.isBlank(hierKey)) {
                    String hier[] = StringTools.split(hierKey, HIERARCHY_SEPARATOR_CHAR);
                    itemParent = TreeNode.createTreePath(itemParent, hier);
                    itemParent.setDescription(hr.getDescription());
                }

                /* leaf */
                String itemID = hr.getItemID();
                if (!StringTools.isBlank(itemID)) {
                    TreeNode/*<RT>*/ leaf = new TreeNode/*<RT>*/(itemID);
                    leaf.setDescription(hr.getDescription());
                    leaf.setObject(hr);
                    itemParent.addChild(leaf);
                }

                /* skip */
                return DBRecordHandler.DBRH_SKIP;

            }
        };
        
        /* Selector */
        // DBSelect: SELECT * FROM HiearchyTable WHERE (accountID='acct') ORDER BY heirarchyKey
        DBSelect<RT> hsel = new DBSelect<RT>(dbFact);
        DBWhere hwh = hsel.createDBWhere();
        hsel.setWhere(hwh.WHERE(
            hwh.EQ(HierarchyRecord.FLD_accountID, acctID)
        ));
        hsel.setOrderByFields(HierarchyRecord.FLD_hierarchyKey);

        /* read devices for account */
        Statement stmt = null;
        ResultSet rs = null;
        try {
            DBRecord.select(hsel, hierRH); // select:DBSelect
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
        }

        /* return parent TreeNode */
        return parent;
        
    }

}
