/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.stac;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.hortonmachine.gui.utils.GuiUtilities;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Builds browsable trees out of stac metadata (maps, lists and jackson json nodes).
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class StacMetadataTree {
    private static final int MAX_LABEL_LENGTH = 300;

    /**
     * A tree node user object keeping both the label and the full value.
     */
    public static class NodeValue {
        final String label;
        final String value;

        NodeValue( String label, String value ) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Create a tree with a popup to copy node values.
     */
    public static JTree createTree() {
        JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy value");
        copyItem.addActionListener(e -> {
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                Object uo = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                GuiUtilities.copyToClipboard(uo instanceof NodeValue nv && nv.value != null ? nv.value : String.valueOf(uo));
            }
        });
        popup.add(copyItem);
        tree.setComponentPopupMenu(popup);
        return tree;
    }

    /**
     * Set a new content in the tree, expanding the first level.
     */
    public static void setContent( JTree tree, Map<String, ? extends Object> content ) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        if (content != null) {
            // sorted for easier browsing
            for( Entry<String, ? extends Object> entry : new TreeMap<>(content).entrySet() ) {
                root.add(build(entry.getKey(), entry.getValue()));
            }
        }
        tree.setModel(new DefaultTreeModel(root));
        for( int i = 0; i < tree.getRowCount(); i++ ) {
            TreePath path = tree.getPathForRow(i);
            if (path.getPathCount() == 2)
                tree.expandPath(path);
        }
    }

    public static DefaultMutableTreeNode build( String key, Object value ) {
        if (value instanceof JsonNode node) {
            if (node.isObject()) {
                DefaultMutableTreeNode n = container(key, "{" + node.size() + "}");
                Iterator<String> names = node.fieldNames();
                while( names.hasNext() ) {
                    String name = names.next();
                    n.add(build(name, node.get(name)));
                }
                return n;
            } else if (node.isArray()) {
                DefaultMutableTreeNode n = container(key, "[" + node.size() + "]");
                for( int i = 0; i < node.size(); i++ ) {
                    n.add(build("[" + i + "]", node.get(i)));
                }
                return n;
            } else {
                return leaf(key, node.isNull() ? "null" : node.asText());
            }
        } else if (value instanceof Map< ? , ? > map) {
            DefaultMutableTreeNode n = container(key, "{" + map.size() + "}");
            for( Map.Entry< ? , ? > e : map.entrySet() ) {
                n.add(build(String.valueOf(e.getKey()), e.getValue()));
            }
            return n;
        } else if (value instanceof Iterable< ? > iterable) {
            DefaultMutableTreeNode n = container(key, "");
            int i = 0;
            for( Object o : iterable ) {
                n.add(build("[" + i++ + "]", o));
            }
            n.setUserObject(new NodeValue(key + " [" + i + "]", null));
            return n;
        } else if (value instanceof Object[] array) {
            DefaultMutableTreeNode n = container(key, "[" + array.length + "]");
            for( int i = 0; i < array.length; i++ ) {
                n.add(build("[" + i + "]", array[i]));
            }
            return n;
        } else if (value instanceof Geometry geom) {
            return leaf(key, geom.getGeometryType() + " " + geom.toText());
        }
        return leaf(key, String.valueOf(value));
    }

    private static DefaultMutableTreeNode container( String key, String sizeInfo ) {
        return new DefaultMutableTreeNode(new NodeValue(key + " " + sizeInfo, null));
    }

    private static DefaultMutableTreeNode leaf( String key, String value ) {
        String shown = value.length() > MAX_LABEL_LENGTH ? value.substring(0, MAX_LABEL_LENGTH) + "…" : value;
        shown = shown.replace('\n', ' ');
        return new DefaultMutableTreeNode(new NodeValue(key + ": " + shown, value));
    }
}
