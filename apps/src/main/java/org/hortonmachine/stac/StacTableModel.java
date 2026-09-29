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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.table.AbstractTableModel;

/**
 * A simple table model backed by a list of row objects, with columns defined through getters.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
@SuppressWarnings("serial")
public class StacTableModel<T> extends AbstractTableModel {
    private final List<String> names = new ArrayList<>();
    private final List<Class< ? >> classes = new ArrayList<>();
    private final List<Function<T, Object>> getters = new ArrayList<>();
    private final List<BiConsumer<T, Object>> setters = new ArrayList<>();
    private List<T> rows = new ArrayList<>();

    /**
     * Add a read only column.
     */
    public StacTableModel<T> col( String name, Class< ? > type, Function<T, Object> getter ) {
        return col(name, type, getter, null);
    }

    /**
     * Add a column, editable if a setter is supplied.
     */
    public StacTableModel<T> col( String name, Class< ? > type, Function<T, Object> getter, BiConsumer<T, Object> setter ) {
        names.add(name);
        classes.add(type);
        getters.add(getter);
        setters.add(setter);
        return this;
    }

    public void setRows( List<T> rows ) {
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public List<T> getRows() {
        return rows;
    }

    public T getRow( int modelIndex ) {
        return rows.get(modelIndex);
    }

    public int indexOf( T row ) {
        return rows.indexOf(row);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return names.size();
    }

    @Override
    public String getColumnName( int column ) {
        return names.get(column);
    }

    @Override
    public Class< ? > getColumnClass( int columnIndex ) {
        return classes.get(columnIndex);
    }

    @Override
    public boolean isCellEditable( int rowIndex, int columnIndex ) {
        return setters.get(columnIndex) != null;
    }

    @Override
    public Object getValueAt( int rowIndex, int columnIndex ) {
        return getters.get(columnIndex).apply(rows.get(rowIndex));
    }

    @Override
    public void setValueAt( Object aValue, int rowIndex, int columnIndex ) {
        BiConsumer<T, Object> setter = setters.get(columnIndex);
        if (setter != null) {
            setter.accept(rows.get(rowIndex), aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
