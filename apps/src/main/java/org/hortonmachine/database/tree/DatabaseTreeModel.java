/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.database.tree;

import java.util.EventListener;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.LeafLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.compat.objects.TypeLevel;

/**
 * Database tree model.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class DatabaseTreeModel implements TreeModel {

    private DbLevel root;
    private EventListenerList listenerList = new EventListenerList();
    /**
    * Constructs an empty tree.
    */
    public DatabaseTreeModel() {
        root = null;
    }

    /**
    * Sets the root to a given variable.
    * @param v the variable that is being described by this tree
    */
    public void setRoot( DbLevel v ) {
        DbLevel oldRoot = v;
        root = v;
        fireTreeStructureChanged(oldRoot);
    }

    public Object getRoot() {
        return root;
    }

    @SuppressWarnings("rawtypes")
    public int getChildCount( Object parent ) {
        if (parent instanceof DbLevel) {
            DbLevel dbLevel = (DbLevel) parent;
            return dbLevel.typesList.size();
        } else if (parent instanceof TypeLevel) {
            TypeLevel typeLevel = (TypeLevel) parent;
            return typeLevel.tablesList.size();
        } else if (parent instanceof TableLevel) {
            TableLevel tableLevel = (TableLevel) parent;
            return tableLevel.columnsList.size();
        } else if (parent instanceof ColumnLevel) {
            ColumnLevel columnLevel = (ColumnLevel) parent;
            return columnLevel.leafsList.size();
        } else if (parent instanceof LeafLevel) {
            LeafLevel leafLevel = (LeafLevel) parent;
            return leafLevel.leafsList.size();
        } else if (parent instanceof List) {
            List list = (List) parent;
            return list.size();
        }
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public Object getChild( Object parent, int index ) {
        if (parent instanceof DbLevel) {
            DbLevel dbLevel = (DbLevel) parent;
            return dbLevel.typesList.get(index);
        } else if (parent instanceof TypeLevel) {
            TypeLevel typeLevel = (TypeLevel) parent;
            return typeLevel.tablesList.get(index);
        } else if (parent instanceof TableLevel) {
            TableLevel tableLevel = (TableLevel) parent;
            return tableLevel.columnsList.get(index);
        } else if (parent instanceof ColumnLevel) {
            ColumnLevel columnLevel = (ColumnLevel) parent;
            return columnLevel.leafsList.get(index);
        } else if (parent instanceof LeafLevel) {
            LeafLevel leafLevel = (LeafLevel) parent;
            return leafLevel.leafsList.get(index);
        } else if (parent instanceof List) {
            List list = (List) parent;
            Object item = list.get(index);
            return item;
        }
        return null;
    }

    public int getIndexOfChild( Object parent, Object child ) {
        int n = getChildCount(parent);
        for( int i = 0; i < n; i++ )
            if (getChild(parent, i).equals(child))
                return i;
        return -1;
    }

    public boolean isLeaf( Object node ) {
        return getChildCount(node) == 0;
    }

    public void valueForPathChanged( TreePath path, Object newValue ) {
    }

    public void addTreeModelListener( TreeModelListener l ) {
        listenerList.add(TreeModelListener.class, l);
    }

    public void removeTreeModelListener( TreeModelListener l ) {
        listenerList.remove(TreeModelListener.class, l);
    }

    protected void fireTreeStructureChanged( Object oldRoot ) {
        TreeModelEvent event = new TreeModelEvent(this, new Object[]{oldRoot});
        EventListener[] listeners = listenerList.getListeners(TreeModelListener.class);
        for( int i = 0; i < listeners.length; i++ )
            ((TreeModelListener) listeners[i]).treeStructureChanged(event);
    }

}