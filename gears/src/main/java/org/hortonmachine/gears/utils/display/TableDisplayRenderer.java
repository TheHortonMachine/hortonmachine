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
package org.hortonmachine.gears.utils.display;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.FormatHelper;

/**
 * Renders maps, non empty collections and matrixes as tables.
 *
 * <p>Collections of collections give one row per inner collection.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TableDisplayRenderer implements IDisplayRenderer {

    @Override
    public boolean canRender( Object value ) {
        return value instanceof Map || (value instanceof Collection && !((Collection< ? >) value).isEmpty())
                || value instanceof double[][] || value instanceof float[][] || value instanceof int[][];
    }

    @Override
    public List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception {
        DisplayTable table;
        if (value instanceof Map) {
            table = mapTable((Map< ? , ? >) value, "Key", options.maxTableRows());
        } else if (value instanceof Collection) {
            table = collectionTable((Collection< ? >) value, options.maxTableRows());
        } else {
            table = collectionTable(matrixRows(value), options.maxTableRows());
        }
        return List.of(table);
    }

    /**
     * Make a two columns table of a map.
     *
     * @param map the map.
     * @param keyHeader the name of the key column.
     * @param maxRows the maximum number of rows.
     * @return the table.
     */
    public static DisplayTable mapTable( Map< ? , ? > map, String keyHeader, int maxRows ) {
        List<List<String>> rows = new ArrayList<>();
        for( Map.Entry< ? , ? > entry : map.entrySet() ) {
            if (rows.size() >= maxRows) {
                break;
            }
            rows.add(List.of(String.valueOf(entry.getKey()), FormatHelper.inspect(entry.getValue())));
        }
        return new DisplayTable(List.of(keyHeader, "Value"), rows, map.size());
    }

    /**
     * Make a table of a collection.
     *
     * <p>Items that are collections are split in columns, other items go in the first column.</p>
     *
     * @param collection the collection.
     * @param maxRows the maximum number of rows.
     * @return the table.
     */
    public static DisplayTable collectionTable( Collection< ? > collection, int maxRows ) {
        int columns = 1;
        boolean nested = false;
        for( Object item : collection ) {
            if (item instanceof Collection) {
                nested = true;
                columns = Math.max(columns, ((Collection< ? >) item).size());
            }
        }
        List<List<String>> rows = new ArrayList<>();
        for( Object item : collection ) {
            if (rows.size() >= maxRows) {
                break;
            }
            List<String> row = new ArrayList<>();
            if (item instanceof Collection) {
                for( Object value : (Collection< ? >) item ) {
                    row.add(String.valueOf(value));
                }
            } else {
                row.add(String.valueOf(item));
            }
            while( row.size() < columns ) {
                row.add("");
            }
            rows.add(row);
        }
        List<String> header = new ArrayList<>();
        for( int c = 0; c < columns; c++ ) {
            header.add(nested ? "Value " + (c + 1) : "Value");
        }
        return new DisplayTable(header, rows, collection.size());
    }

    private static List<List<Object>> matrixRows( Object matrix ) {
        List<List<Object>> rows = new ArrayList<>();
        if (matrix instanceof double[][]) {
            for( double[] row : (double[][]) matrix ) {
                List<Object> values = new ArrayList<>();
                for( double v : row ) {
                    values.add(v);
                }
                rows.add(values);
            }
        } else if (matrix instanceof float[][]) {
            for( float[] row : (float[][]) matrix ) {
                List<Object> values = new ArrayList<>();
                for( float v : row ) {
                    values.add(v);
                }
                rows.add(values);
            }
        } else {
            for( int[] row : (int[][]) matrix ) {
                List<Object> values = new ArrayList<>();
                for( int v : row ) {
                    values.add(v);
                }
                rows.add(values);
            }
        }
        return rows;
    }
}
