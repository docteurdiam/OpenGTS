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
//  2011/08/21  Martin D. Flynn
//     -Added support for returning a JSON object repreenting the tree structure
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;

/**
*** Tree Node
**/

public class TreeNode
{

    // ------------------------------------------------------------------------

    public static final String  SLASH_SEPARATOR         = "/";
    public static final char    SLASH_SEPARATOR_CHAR    = '/';

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static interface TreeNodeHandler
    {
        public boolean startNode(TreeNode tn);
        public void    endNode(TreeNode tn);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Create named TreeNodes under specified parent
    **/
    public static TreeNode createTreePath(TreeNode parent, String name[])
    {
        return TreeNode.createTreePath(parent, name, null);
    }

    /**
    *** Create named TreeNodes under specified parent
    **/
    public static TreeNode createTreePath(TreeNode parent, String name[], Class<? extends TreeNode> treeNodeClass)
    {

        /* invalid name */
        if (ListTools.isEmpty(name)) {
            return parent;
        }

        /* no parent */
        if (parent == null) {
            Print.logStackTrace("Parent is null!");
            return null;
        }

        /* add children/grandchildren */
        TreeNode tn = parent;
        try {
            for (int n = 0; n < name.length; n++) {
                TreeNode sn = tn.getChildByName(name[n]);
                if (sn == null) {
                    sn = (treeNodeClass != null)? (TreeNode)treeNodeClass.newInstance() : new TreeNode();
                    sn.setName(name[n]);
                    tn.addChild(sn);
                }
                tn = sn;
            }
        } catch (Throwable th) { // MethodInvocationException
            return null; // error
        }

        /* return last node created */
        return tn;

    }

    /**
    *** Gets/Returns named TreeNodes under specified parent
    **/
    public static TreeNode getTreePath(TreeNode parent, String name[])
    {

        /* invalid name */
        if (ListTools.isEmpty(name)) {
            return parent;
        }

        /* no parent */
        if (parent == null) {
            Print.logStackTrace("Parent is null!");
            return null;
        }

        /* find children/grandchildren */
        TreeNode tn = parent;
        for (int n = 0; n < name.length; n++) {
            TreeNode sn = tn.getChildByName(name[n]);
            if (sn == null) {
                return null; // not found
            }
            tn = sn;
        }

        /* return last node found */
        return tn;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Flattens the specified tree
    **/
    public static java.util.List<String> flattenTree(java.util.List<String> list, String prefix, char sep, TreeNode parent)
    {

        /* list */
        if (list == null) {
            list = new Vector<String>();
        }

        /* invalid parent */
        if (parent == null) {
            return list;
        }

        /* this node */
        String name = parent.getName();
        prefix = StringTools.trim(prefix) + sep + name;
        list.add(prefix);
        Print.sysPrintln(prefix);

        /* descend tree */
        if (parent.hasChildren()) {
            for (TreeNode tn : parent.getChildren()) {
                TreeNode.flattenTree(list, prefix, sep, tn);
            }
        }
        
        /* return collection */
        return list;

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Prints the specified Tree
    **/
    public static <TN extends TreeNode> void printTree(TN parent)
    {
        if (parent != null) {
            parent.printChildren(0);
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                          name        = "";
    private String                          description = "";
    private int                             type        = 0;
    
    private TreeNode                        parent      = null;
    private java.util.List<TreeNode >       children    = null;
    
    private RTProperties                    rtProp      = null;
    private Object                          value       = null;

    /**
    *** Constructor
    **/
    public TreeNode()
    {
        super();
    }

    /**
    *** Constructor
    **/
    public TreeNode(String name)
    {
        super();
        this.setName(name);
    }

    /**
    *** Constructor
    **/
    public TreeNode(String name, java.util.List<TreeNode> children)
    {
        this(name);
        this.addChildren(children);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this TreeNode
    **/
    public void setName(String name)
    {
        this.name = StringTools.trim(name);
    }
    
    /**
    *** Gets the name of this TreeNode
    **/
    public String getName()
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the type of this TreeNode
    *** (usage defined by caller)
    **/
    public void setType(int type)
    {
        this.type = type;
    }
    
    /**
    *** Gets the type of this TreeNode
    **/
    public int getType()
    {
        return this.type;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the description of this TreeNode
    **/
    public void setDescription(String desc)
    {
        this.description = StringTools.trim(desc);
    }
    
    /**
    *** Gets the description of this TreeNode
    **/
    public String getDescription()
    {
        if (!StringTools.isBlank(this.description)) {
            return this.description;
        } else {
            return this.getName();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the collection of children nodes 
    **/
    public java.util.List<TreeNode> getChildren()
    {
        return this.children;
    }

    /**
    *** Returns true if this node has children
    **/
    public boolean hasChildren()
    {
        return !ListTools.isEmpty(this.children);
    }

    /**
    *** Returns number of children in this node
    **/
    public int size()
    {
        return ListTools.size(this.children);
    }

    /**
    *** Adds all children in the specified java.util.List
    **/
    public void addChildren(java.util.List<TreeNode> children)
    {
        if (children != null) {
            for (TreeNode tn : children) {
                this.addChild(tn);
            }
        }
    }

    /**
    *** Adds the specified TreeNode as a child to this node
    **/
    public TreeNode addChild(TreeNode node)
    {
        if (node == null) {
            // quietly ignore
            return null;
        } else
        if (this.isAncestor(node)) {
            Print.logStackTrace("Attempting to add an ancestor to this node");
            return null;
        } else {
            if (this.children == null) {
                this.children = new Vector<TreeNode>();
            }
            this.children.add(node);
            node.setParent(this);
            return node;
        }
    }

    /**
    *** Finds/Returns the named child
    *** Does not check grandchildren.
    **/
    public TreeNode getChildAt(int ndx)
    {
        java.util.List<TreeNode> chList = this.getChildren();
        if ((chList != null) && (ndx >= 0) && (ndx < chList.size())) {
            return chList.get(ndx);
        } else {
            return null;
        }
    }

    /**
    *** Finds/Returns the named child
    *** Does not check grandchildren.
    **/
    public TreeNode getChildByName(String name)
    {

        /* invalid name */
        if (StringTools.isBlank(name)) {
            return null;
        }

        /* descend children */
        if (this.hasChildren()) {
            for (TreeNode tn : this.getChildren()) {
                if (tn.getName().equals(name)) {
                    return tn;
                }
            }
        }

        /* not found */
        return null;

    }

    /**
    *** Finds/Returns the named child
    *** Does not check grandchildren.
    **/
    public TreeNode getChildByPath(String name[])
    {

        /* invalid name */
        if (ListTools.isEmpty(name)) {
            return null;
        }

        /* find child */
        TreeNode tn = this.getChildByName(name[0]);
        for (int n = 1; (tn != null) && (n < name.length); n++) {
            tn = tn.getChildByName(name[n]);
        }
        return tn; // may be null;

    }

    /**
    *** Finds/Returns the child matching the specified value
    *** Does not check grandchildren.
    **/
    public TreeNode getChildByValue(Object val)
    {

        /* invalid name */
        if (val == null) {
            return null;
        }

        /* descend children */
        if (this.hasChildren()) {
            for (TreeNode tn : this.getChildren()) {
                Object v = tn.getObject();
                if ((v != null) && val.equals(v)) {
                    return tn;
                }
            }
        }

        /* not found */
        return null;

    }
    
    /**
    *** Remove the specified child
    **/
    public boolean removeChild(TreeNode tn)
    {
        if ((this.children != null) && this.children.contains(tn)) {
            tn.setParent(null);
            this.children.remove(tn);
            return true;
        } else {
            return false;
        }
    }
    
    /**
    *** Remove this node from it's parent
    **/
    public boolean removeFromParent()
    {
        TreeNode parent = this.getParent();
        if (parent != null) {
            parent.removeChild(this);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if this node has a 'next' sibling
    **/
    public boolean hasNextSibling()
    {
        return (this.getNextSibling() != null);
    }

    /**
    *** Return the 'next' sibling node of this node
    **/
    public TreeNode getNextSibling()
    {
        TreeNode parent = this.getParent();
        if (parent == null) {
            return null; // 'this' is the root node (no siblings)
        }
        java.util.List<TreeNode> siblings = parent.getChildren();
        if (siblings != null) {
            int sz = siblings.size();
            for (int n = 0; n < sz; n++) {
                if (siblings.get(n) == this) {
                    return ((n + 1) < sz)? siblings.get(n + 1) : null;
                }
            }
        }
        return null; // will not occur
    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if this node has a 'previous' sibling
    **/
    public boolean hasPreviousSibling()
    {
        return (this.getPreviousSibling() != null);
    }

    /**
    *** Return the 'previous' sibling node of this node
    **/
    public TreeNode getPreviousSibling()
    {
        TreeNode parent = this.getParent();
        if (parent == null) {
            return null; // 'this' is the root node (no siblings)
        }
        java.util.List<TreeNode> siblings = parent.getChildren();
        if (siblings != null) {
            int sz = siblings.size();
            for (int n = 0; n < sz; n++) {
                if (siblings.get(n) == this) {
                    return ((n - 1) >= 0)? siblings.get(n - 1) : null;
                }
            }
        }
        return null; // will not occur
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the parent of this TreeNode
    **/
    protected void setParent(TreeNode node)
    {
        this.parent = node;
    }

    /**
    *** Gets the parent of this TreeNode
    **/
    protected TreeNode getParent()
    {
        return this.parent;
    }

    /**
    *** Returns true if this TreeNode has a parent
    **/
    public boolean hasParent()
    {
        return (this.parent != null);
    }

    /**
    *** Returns true if this node does not have a parent
    **/
    public boolean isRootNode()
    {
        return (this.parent == null);
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns true if the specified node is the same as this node,
    *** or is an ancestor
    **/
    public boolean isAncestor(TreeNode node)
    {

        /* invalid node */
        if (node == null) {
            return false;
        }

        /* ascend tree */
        TreeNode tn = this;
        while (tn != null) {
            if (node == tn) {
                return true;
            }
            tn = tn.getParent();
        }

        /* not an ancestor */
        return false;

    }
    
    /** 
    *** Returns true if the specified node is an offspring of this node
    **/
    public boolean isOffspring(TreeNode node)
    {

        /* invalid node */
        if (node == null) {
            return false;
        }

        /* descend children */
        if (this.hasChildren()) {
            for (TreeNode tn : this.getChildren()) {
                if (tn.isOffspring(node)) {
                    return true;
                }
            }
        }
        
        /* not an offspring */
        return false;
        
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Calculates/Returns the level of this node (root node is level '0')
    **/
    public int getLevel()
    {
        return this.getLevel(null);
    }
    
    /**
    *** Calculates/Returns the level of this node (root node is level '0')
    **/
    public int getLevel(TreeNode parent)
    {
        int L = 0;
        for (TreeNode tn = this.getParent(); (tn != null) && (tn != parent); L++) {
            tn = tn.getParent();
        }
        return L;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the path of this TreeNode
    **/
    public TreeNode[] getPath()
    {
        return this.getPath(null);
    }

    /**
    *** Returns the path of this TreeNode
    **/
    public TreeNode[] getPath(TreeNode parent)
    {
        java.util.List<TreeNode> path = new Vector<TreeNode>();

        /* ascend tree */
        TreeNode tn = this;
        while ((tn != null) && (tn != parent)) {
            path.add(tn);
            tn = tn.getParent();
        }

        /* reverse */
        int pathLen = path.size();
        TreeNode pathStr[] = new TreeNode[pathLen];
        for (int i = 0; i < pathLen; i++) {
            pathStr[i] = path.get((pathLen - 1) - i);
        }
        return pathStr;

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the path name of this TreeNode
    **/
    public String[] getPathNames()
    {
        return this.getPathNames(null);
    }
    
    /**
    *** Returns the path name of this TreeNode
    **/
    public String[] getPathNames(TreeNode parent)
    {
        TreeNode tn[] = this.getPath(parent);
        if (ListTools.isEmpty(tn)) {
            return new String[0];
        } else {
            String pn[] = new String[tn.length];
            for (int i = 0; i < pn.length; i++) {
                pn[i] = tn[i].getName();
            }
            return pn;
        }
    }

    /**
    *** Returns the path name of this TreeNode
    **/
    public String getPathName(String sep)
    {
        return this.getPathName(null, sep);
    }
    
    /**
    *** Returns the path name of this TreeNode
    **/
    public String getPathName(TreeNode parent, String sep)
    {
        String s = (sep != null)? sep : SLASH_SEPARATOR;
        StringBuffer sb = new StringBuffer();
        for (TreeNode tn : this.getPath(parent)) {
            sb.append(s).append(tn.getName());
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the path name of this TreeNode
    **/
    public String[] getPathDescriptions()
    {
        return this.getPathDescriptions(null);
    }

    /**
    *** Returns the path name of this TreeNode
    **/
    public String[] getPathDescriptions(TreeNode parent)
    {
        TreeNode tn[] = this.getPath(parent);
        if (ListTools.isEmpty(tn)) {
            return new String[0];
        } else {
            String pn[] = new String[tn.length];
            for (int i = 0; i < pn.length; i++) {
                String d = tn[i].getDescription();
                pn[i] = !StringTools.isBlank(d)? d : tn[i].getName();
            }
            return pn;
        }
    }

    /**
    *** Returns the path name of this TreeNode
    **/
    public String getPathDescription(String sep)
    {
        return this.getPathDescription(null, sep);
    }

    /**
    *** Returns the path name of this TreeNode
    **/
    public String getPathDescription(TreeNode parent, String sep)
    {
        String s = (sep != null)? sep : SLASH_SEPARATOR;
        StringBuffer sb = new StringBuffer();
        for (TreeNode tn : this.getPath(parent)) {
            sb.append(s);
            String d = tn.getDescription();
            sb.append(!StringTools.isBlank(d)? d : tn.getName());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Prefix traversal of TreeNodes
    **/
    public TreeNode traverseTree(TreeNodeHandler tnh)
    {

        /* null node */
        if (tnh == null) {
            return null;
        }

        /* call-back */
        if (tnh.startNode(this)) {
            // returned true, done traversing
            return this;
        }

        /* check for children */
        if (this.hasChildren()) {
            for (TreeNode tn : this.getChildren()) {
                TreeNode foundNode = tn.traverseTree(tnh);
                if (foundNode != null) { 
                    return foundNode;
                }
            }
        }

        /* done with traversal (for this branch) */
        tnh.endNode(this);
        return null;

    }

    /**
    *** Return first node with matching name
    **/
    public TreeNode findChildByName(final String name)
    {
        return this.traverseTree(new TreeNodeHandler() {
            public boolean startNode(TreeNode tn) {
                return ((tn != null) && tn.getName().equals(name));
            }
            public void endNode(TreeNode tn) {}
        });
    }

    /**
    *** Return first node with matching property key
    **/
    public TreeNode findChildByProperty(final String key, final Object value)
    {

        /* key not specified */
        if (StringTools.isBlank(key)) {
            return null;
        }

        /* traverse and return */
        return this.traverseTree(new TreeNodeHandler() {
            public boolean startNode(TreeNode tn) {
                if (tn == null) {
                    return false;
                } else
                if (!tn.hasProperty(key)) {
                    return false;
                }
                Object v = tn.getProperty(key);
                if (v == value) {
                    return true;
                } else
                if ((v != null) && v.equals(value)) {
                    return true;
                } else {
                    return false;
                }
            }
            public void endNode(TreeNode tn) {}
        });
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Returns a JSON array of this nodes children (or null if there are no children)
    *** @return A JSON array of this nodes children (or null if there are no children)
    **/
    public JSON._Array getJsonChildrenArray()
    {
        JSON._Array children = null;
        if (this.hasChildren()) {
            children = new JSON._Array();
            for (TreeNode tn : this.getChildren()) {
                children.addValue(tn.getJsonObject());
            }
        }
        return children;
    }

    /**
    *** Returns a JSON object for this TreeNode
    *** @return A JSON object for this TreeNode
    **/
    public JSON._Object getJsonObject()
    {
        return this._setJsonNodeValues(new JSON._Object(), 
            this.getName(), this.getDescription(), 
            this.getJsonChildrenArray());
    }

    public static final String JSON_Key_name        = "name";
    public static final String JSON_Key_description = "description";
    public static final String JSON_Key_children    = "children";

    /**
    *** Overridable method for setting node JSON values
    *** @param jsonObj  The JSON object for this not (never null)
    *** @param name     The default node name
    *** @param desc     The default node description
    *** @param children A JSON array of children JSON nodes (may be null)
    *** @return The jsonObj passed to this method
    **/
    protected JSON._Object _setJsonNodeValues(JSON._Object jsonObj, 
        String name, String desc, 
        JSON._Array children)
    {
        jsonObj.addKeyValue(JSON_Key_name        , name);
        jsonObj.addKeyValue(JSON_Key_description , desc);
        if (children != null) {
            jsonObj.addKeyValue(JSON_Key_children, children);
        }
        return jsonObj;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the String representation of this TreeNode
    **/
    public String toString()
    {
        return this.getPathName(SLASH_SEPARATOR);
    }

    /**
    *** Gets the String representation of this TreeNode
    **/
    public String toString(String sep)
    {
        return this.getPathName(sep);
    }

    /**
    *** Prints the children nodes 
    **/
    public void printChildren()
    {
        this.printChildren(-1);
    }

    /**
    *** Prints the children nodes 
    **/
    public void printChildren(int lvl)
    {
        int    level  = (lvl >= 0)? lvl : this.getLevel();
        String sep    = SLASH_SEPARATOR;
        String indent = StringTools.replicateString("    ", level);

        /* description */
        String desc   = this.getDescription();
        if (StringTools.isBlank(desc)) { desc = this.getName(); }

        /* print */
        StringBuffer sb = new StringBuffer();
        sb.append(indent).append(desc);
        if (this.hasChildren()) {
            sb.append(":");
            Print.sysPrintln(sb.toString());
            for (TreeNode tn : this.getChildren()) {
                tn.printChildren(level + 1);
            }
        } else {
            Print.sysPrintln(sb.toString());
        }

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the 'other' node is the same as 'this' node
    **/
    public boolean equals(Object other)
    {

        /* not event the same type? */
        if (!(other instanceof TreeNode)) {
            return false;
        }

        /* is same node? */
        if (other == this) {
            return true;
        }

        /* "similar" nodes don't count */
        return false;
 
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Get node properties
    *** @return Node properties
    **/
    public RTProperties getProperties()
    {
        if (this.rtProp == null) {
            this.rtProp = new RTProperties();
        }
        return this.rtProp;
    }
    
    /**
    *** Returns true if properties have been defined for this node
    *** @return True if properties have been defined for this node
    **/
    public boolean hasProperties()
    {
        return (this.rtProp != null) && !this.rtProp.isEmpty();
    }

    /**
    *** Returns true if the specified property key is defined in this node
    *** @param key  The property key
    *** @return True if the specified property key is defined in this node
    **/
    public boolean hasProperty(String key)
    {
        return (this.rtProp != null)? this.rtProp.hasProperty(key) : false;
    }

    /**
    *** Gets the specified node property value
    *** @param key  The property key
    *** @param dft  The default return value
    *** @return The property value
    **/
    public Object getProperty(String key, Object dft)
    {
        return (this.rtProp != null)? this.rtProp.getProperty(key,dft) : null;
    }

    /**
    *** Gets the specified node property value
    *** @param key  The property key
    *** @return The property value
    **/
    public Object getProperty(String key)
    {
        return this.getProperty(key, null);
    }

    /**
    *** Sets the specified node property value
    *** @param key  The property key
    *** @param val  The property value
    **/
    public void setProperty(String key, Object val)
    {
        this.getProperties().setProperty(key, val);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets a node leaf object
    **/
    public void setObject(Object val)
    {
        this.value = val;
    }
    
    /**
    *** Gets the node leaf object
    **/
    public Object getObject()
    {
        return this.value;
    }
    
    /**
    *** Returns true if this node has an object value
    **/
    public boolean hasObject()
    {
        return (this.value != null);
    }
    
    /**
    *** Returns true if this node has an object value
    **/
    public boolean isLeaf()
    {
        return (this.value != null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        String list[] = new String[] {
            "AA/BA/CA/DA/EA",
            "AA/BA/CA/DA/EB",
            "AA/BA/CA/DB/EA",
            "AA/BA/CA/DB/EB",
            "AA/BA/CA/DB/EC",
            "AA/BA/CA/DC/EA",
            "AA/BA/CB/DA/EA",
            "AA/BA/CB/DA/EB",
            "AA/BA/CB/DB/EA",
            "AA/BA/CB/DB/EB",
            "AA/BA/CB/DB/EC",
            "AA/BA/CB/DC/EA",
            "AA/BB/CA/DA/EA",
            "AA/BB/CA/DA/EB",
            "AA/BB/CA/DB/EA",
            "AA/BB/CA/DB/EB",
            "AA/BB/CA/DB/EC",
            "AA/BB/CA/DC/EA",
            "AA/BB/CB/DA/EA",
            "AA/BB/CB/DA/EB",
            "AA/BB/CB/DB/EA",
            "AA/BB/CB/DB/EB",
            "AA/BB/CB/DB/EC",
            "AA/BB/CB/DC/EA",
        };

        TreeNode root = new TreeNode("root");
        for (String a : list) {
            String aa[] = StringTools.split(a, SLASH_SEPARATOR_CHAR);
            TreeNode.createTreePath(root, aa);
        }

        printTree(root);

        Vector<String> flatList = new Vector<String>();
        flattenTree(flatList, "", '-', root);

        JSON._Object jsonObj = root.getJsonObject();
        Print.sysPrintln("JSON:\n" + jsonObj);

    }
    
}
