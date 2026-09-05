/*
 * $Id: Tree.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.ds;

import oms3.Compound;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Tree data structure.
 * 
 * @author od  
 *   
 */
public class Tree {

    Map<Compound, TreeNode> content = new HashMap<Compound, TreeNode>();
    Compound root;

    public Tree(Compound root) {
        content.put(root, new TreeNode(null));
        this.root = root;
    }

    private TreeNode node(Compound c) {
        TreeNode t = content.get(c);
        if (t == null) {
            throw new IllegalArgumentException("Not in tree " + c);
        }
        return t;
    }

    public List<Compound> children(Compound c) {
        return node(c).children;
    }

    public Compound parent(Compound c) {
        return node(c).parent;
    }

//   inorder
//   postorder
//   preorder
//   levelorder  (breadthfirst)
    
    /** Returns all compounds from the Compound argument to the root of 
     *  the tree following the path.
     * 
     * @param c the Compound to start with.
     * @return the set of Compounds in the given order.
     */
    public Iterator<Compound> toRootOrder(final Compound c) {
        return new Iterator<Compound>() {

            Compound curr;
            TreeNode n = node(c);
            Compound parent = c;

            public boolean hasNext() {
                return !n.isRoot();
            }

            public Compound next() {
                if (hasNext()) {
                    curr = parent;
                    parent = n.parent;
                    n = node(n.parent);
                    return curr;
                } else {
                    throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void add(Compound parent, Compound child) {
        TreeNode parentNode = content.get(parent);
        if (parentNode == null) {
            throw new IllegalArgumentException("no parent");
        }
        TreeNode c = content.get(child);
        if (c != null) {
            throw new IllegalArgumentException("child already in.");
        }
        content.put(child, new TreeNode(parent));
        parentNode.children.add(child);
    }

    public Compound getRoot() {
        return root;
    }

    public Collection<Compound> content() {
        return content.keySet();
    }
}

/**
 * 
 * @author od
 */
class TreeNode {

    Compound parent;
    List<Compound> children = new ArrayList<Compound>(3);

    TreeNode(Compound parent) {
        this.parent = parent;
    }

    boolean isRoot() {
        return parent == null;
    }

    boolean hasChildren() {
        return children.size() > 0;
    }
}

