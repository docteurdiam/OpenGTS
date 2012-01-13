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
// Description:
//  This module constructs a table dependency tree
// ----------------------------------------------------------------------------
// Change History:
//  2008/02/27  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.math.*;

import org.opengts.util.*;

/**
*** <code>DBFactoryTree</code> provides an SQL table dependency tree.  Instances
*** of <code>DBFactoryTree</code> are nodes in this tree.
**/

public class DBFactoryTree
{

    // ------------------------------------------------------------------------

    private int                             level        = 0;
    private DBFactory<? extends DBRecord>   dbFactory    = null;
    private DBFactoryTree                   parentNode   = null;
    
    private java.util.List<DBFactoryTree>   childList    = null;
    private DBFactoryTree                   childArray[] = null;
    
    private DBRecord                        dbRecord     = null;
    
    /**
    *** Constructor
    **/
    private DBFactoryTree() 
    {
        // root node
        this(0,null,null);
    }
        
    /**
    *** Constructor
    *** @param level   Tree level 
    *** @param parent  Tree parent node
    *** @param dbFact  The DBFactory for this node
    **/
    private DBFactoryTree(int level, DBFactoryTree parent, DBFactory<? extends DBRecord> dbFact) 
    {
        this.level      = level;
        this.parentNode = parent;
        this.dbFactory  = dbFact;
        this.childList  = null;
        this.childArray = null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the tree level 
    *** @return The tree level
    **/
    public int getLevel() 
    {
        return this.level;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the parent DBFactoryTree node
    *** @param parent The parent node
    **/
    public void setParentNode(DBFactoryTree parent)
    {
        this.parentNode = parent;
    }
    
    /**
    *** Gets the parent DBFactoryTree node
    *** @return The parent DBFactory node
    **/
    public DBFactoryTree getParentNode()
    {
        return this.parentNode;
    }

    /**
    *** Returns true if the parent of this node has a defined DBRecord
    *** @return True if the parent node has a defined DBRecord
    **/
    public boolean hasParentDBRecord()
    {
        if (this.parentNode != null) {
            return this.parentNode.hasDBRecord();
        } else {
            // If the parent node is null, then this is the root, and we will
            // return true by default
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this node has a defined DBFactory
    *** @return True if this node has a defined DBFactory
    **/
    public boolean hasDBFactory()
    {
        return (this.dbFactory != null);
    }

    /**
    *** Gets the defined DBFactory for this node
    *** @return The DBFactory for this node
    **/
    public DBFactory<? extends DBRecord> getDBFactory() 
    {
        return this.dbFactory;
    }

    /**
    *** Returns true if the DBFactory table is editable
    *** @return True if the DBFactory table is editable
    **/
    public boolean isEditable()
    {
        return (this.dbFactory != null)? this.dbFactory.isEditable() : false;
    }

    /**
    *** Gets the fields for the DBFactory of this node
    *** @return The DBFields for this node
    **/
    public DBField[] getFields() 
    {
        return (this.dbFactory != null)? this.dbFactory.getFields() : null;
    }

    /**
    *** Gets the primary key fields for the DBFactory of this node
    *** @return The primary key fields for this node
    **/
    public DBField[] getKeyFields() 
    {
        return (this.dbFactory != null)? this.dbFactory.getKeyFields() : null;
    }

    /**
    *** Returns the primary key field names for the DBFactory of this node
    *** @return The primary key field names for this node
    **/
    public String[] getKeyNames() 
    {
        return (this.dbFactory != null)? this.dbFactory.getKeyNames() : null;
    }

    /**
    *** Gets the DBField for the specified field name 
    *** @param name The name of the field to retrieve
    *** @return The DBField for the specified name
    **/
    public DBField getField(String name) 
    {
        return (this.dbFactory != null)? this.dbFactory.getField(name) : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a DBRecord is defined for this node
    *** @return True is a DBRecord is defined for this node
    **/
    public boolean hasDBRecord()
    {
        return (this.dbRecord != null);
    }
    
    /**
    *** Gets the DBRecord for this node
    *** @return The DBRecord for this node
    **/
    public DBRecord getDBRecord()
    {
        return this.dbRecord;
    }
    
    /** 
    *** Sets the DBRecord for this node
    *** @param record  The DBRecord to set
    *** @return True if the DBRecord was successfully set, false otherwise
    **/
    public boolean setDBRecord(DBRecord<?> record)
    {
        
        /* pre-checks */
        this.dbRecord = null;
        if (record == null) {
            return false;
        } else
        if (!this.hasDBFactory()) {
            // TODO: we probably could just retrieve the DBFactory from the DBRecord
            Print.logError("DBFactory is not defined!");
            return false;
        }
        
        /* check DBFactory */
        DBFactory<?> rcdFact = DBRecord.getFactory(record);
        if (!this.getDBFactory().equals(rcdFact)) {
            Print.logError("Invalid DBFactory for specified DBRecord!");
            return false;
        }
        
        /* set DBRecord */
        this.dbRecord = record;
        return true;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the table name for the defined DBFactory
    *** @return The table name
    **/
    public String getUntranslatedTableName()
    {
        return (this.dbFactory != null)? this.dbFactory.getUntranslatedTableName() : "";
    }

    /**
    *** Gets the table name for the DBFactory (if defined)
    *** @return The DBFactory table name, or "root" if the DBFactory is not defined, and the tree level is '0'
    **/
    public String getName()
    {
        if (this.dbFactory != null) {
            return this.dbFactory.getUntranslatedTableName();
        } else
        if (this.level == 0) {
            return "<root>";
        } else {
            return "?unknown?";
        }
    }
    
    /**
    *** Gets the String representation of this class (typically the DBFactory table name)
    *** @return The String representation of this class
    **/
    public String toString() 
    {
        return this.getName();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Adds the specified node as a child of this node
    *** @param dbFactNode  The node to add
    **/
    public void addChild(DBFactoryTree dbFactNode)
    {
        if (this.childList == null) {
            this.childList = new Vector<DBFactoryTree>();
        }
        this.childList.add(dbFactNode);
        this.childArray = null;
    }
    
    /**
    *** Returns true if this node has children
    *** @return True if this node has children
    **/
    public boolean hasChildren()
    {
        return (this.childList != null);
    }

    /**
    *** Gets an array of children for this node
    *** @return An array of children for this node, or null if this node has no children
    **/
    public DBFactoryTree[] getChildren() 
    {
        if ((this.childList != null) && (this.childArray == null)) {
            this.childArray = new DBFactoryTree[this.childList.size()];
            this.childList.toArray(this.childArray);
        }
        return this.childArray;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns an OrderedMap of defined primary keys
    *** @return map of defined primary keys
    **/
    public Map<DBField,Object> getKeyMap()
    {
        Map<DBField,Object> keyMap = new OrderedMap<DBField,Object>(); // ordered key list
        if (this.dbFactory != null) {
            DBField keyField[] = this.dbFactory.getKeyFields();
            for (int i = 0; i < keyField.length; i++) {
                String key = keyField[i].getName();
                Object val = null;
                if (this.dbRecord != null) {
                    // all key values will be defined
                    val = this.dbRecord.getFieldValue(key);
                } else {
                    // use parent keys
                    DBFactoryTree parent = this.getParentNode();
                    for (; (parent != null) && (val == null); parent = parent.getParentNode()) {
                        DBRecord dbr = parent.getDBRecord();
                        if (dbr == null) {
                            // stop at the first undefined ancestor
                            break;
                        } else
                        if (dbr.hasField(key)) {
                            // try getting key value from ancestor
                            DBField parFld = dbr.getField(key);
                            if ((parFld != null) && parFld.isPrimaryKey()) {
                                // primary key fields only
                                val = dbr.getFieldValue(key);
                            }
                        }
                    }
                }
                // save key DBField and value
                if (val != null) {
                    keyMap.put(keyField[i], val);
                } else {
                    keyMap.put(keyField[i], null);
                }
            }
        }
        return keyMap;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Traverses the DBFactory dependency tree, creating a DBFactoryTree
    *** @param level  The current tree level
    *** @param dbFact The current DBFactory to add
    *** @param parentNode  The parent node to which a new DBFactoryNode child will be added
    *** @param addedTables A set of table names added to the current DBFactoryTree
    ***/
    private static void _traverseDBFactoryTree(int level, 
        DBFactory<? extends DBRecord> dbFact, DBFactoryTree parentNode, Set<String> addedTables)
    {

        /* no DBFactory? */
        if (dbFact == null) {
            Print.logError("Null DBFactory!");
            return;
        }
        String utableName = dbFact.getUntranslatedTableName();

        /* already added? */
        if (addedTables.contains(utableName)) {
            return;
        }
        addedTables.add(utableName);

        /* add this node */
        //Print.logInfo(StringTools.replicateString("  ",level) + dbFact.getUntranslatedTableName());
        DBFactoryTree dbFactNode = new DBFactoryTree(level, parentNode, dbFact);
        parentNode.addChild(dbFactNode);

        /* find dependent children */
        DBFactory<? extends DBRecord> childFact[] = dbFact.getChildFactories();
        for (int i = 0; i < childFact.length; i++) {
            int index = childFact[i].getParentTables().indexOf(utableName);
            if (level == index) {
                DBFactoryTree._traverseDBFactoryTree(level + 1, childFact[i], dbFactNode, addedTables);
            } else
            if (!addedTables.contains(childFact[i].getUntranslatedTableName())) {
                Print.logWarn("Skipping table in heiarchy: " + utableName + " ==> " + childFact[i].getUntranslatedTableName());
            }
        }

    }

    /**
    *** Returns an array of root DBFactoryTree nodes based on the specified top-level DBFactory.
    *** @param initialFactory  The root DBFactory which is traversed to create the DBFactoryTree.  If null
    ***                        all root DBFactories will be traversed.
    *** @return An array of rot DBFactoryTree nodes.  If the 'initialFactory' is null, the returned
    ***         DBFactoryTree array will have at most 1 element.
    **/
    @SuppressWarnings("unchecked")
    public static DBFactoryTree[] getDBFactoryTree(DBFactory<? extends DBRecord> initialFactory)
    {
        if (initialFactory != null) {
            return DBFactoryTree.getDBFactoryTree(new DBFactory[] { initialFactory }); // "unchecked conversion"
        } else {
            return DBFactoryTree.getDBFactoryTree((DBFactory<? extends DBRecord>[])null);
        }
    }
    
    /**
    *** Returns an array of root DBFactoryTree nodes based on the specified top-level DBFactory.
    *** @param initialFactories  The root DBFactories which are traversed to create the DBFactoryTree.  If null
    ***                        all root DBFactories will be traversed.
    *** @return An array of rot DBFactoryTree nodes.
    **/
    public static DBFactoryTree[] getDBFactoryTree(DBFactory<? extends DBRecord> initialFactories[])
    {
        Set<String> addedTables = new HashSet<String>();
        DBFactoryTree rootNode = new DBFactoryTree();
        
        /* start with initial factories */
        if (initialFactories != null) {
            for (int i = 0; i < initialFactories.length; i++) {
                DBFactory<? extends DBRecord> dbf = initialFactories[i];
                DBFactoryTree._traverseDBFactoryTree(0, dbf, rootNode, addedTables);
            }
        }
        
        /* traverse remaining DBFactories, if any */
        OrderedMap<String,DBFactory<? extends DBRecord>> dbFactMap = 
            new OrderedMap<String,DBFactory<? extends DBRecord>>(DBAdmin.getTableFactoryMap());
        for (Iterator<String> i = dbFactMap.keyIterator(); i.hasNext();) {
            String tn = i.next();
            DBFactory<? extends DBRecord> dbFact = (DBFactory<? extends DBRecord>)dbFactMap.get(tn);
            DBFactoryTree._traverseDBFactoryTree(0, dbFact, rootNode, addedTables);
        }
        
        /* return list of root children */
        DBFactoryTree roots[] = rootNode.getChildren();
        if (roots != null) {
            // clear our temporary root node from the table factory roots
            for (int i = 0; i < roots.length; i++) {
                roots[i].setParentNode(null);
            }
        }
        return roots;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
