/*
 * $Id$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
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
 * @author Olaf David (olaf.david@ars.usda.gov)
 * @version $Id$ 
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
 * @author Olaf David
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

