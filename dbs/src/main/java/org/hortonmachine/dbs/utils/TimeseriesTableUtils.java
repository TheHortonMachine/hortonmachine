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
package org.hortonmachine.dbs.utils;

import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.datatypes.EDataType;

/**
 * Heuristic check for whether a table "looks like" a timeseries table in the
 * sense {@link DbTimeseriesIterator} reads: a temporal column (epoch
 * millis, or a native DATE/DATETIME/TIMESTAMP type) plus at least one other
 * numeric value column - the same shape written by, for example, WHETGEO-1D's
 * forcing/output tables, without requiring any particular column naming
 * convention beyond a temporal-sounding name for non-natively-typed time
 * columns.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TimeseriesTableUtils {
    private TimeseriesTableUtils() {
    }

    private static final String[] TEMPORAL_NAME_HINTS = {"time", "timestamp", "date", "ts", "epoch"};

    /**
     * @return {@code true} if {@code table} has at least one plausibly temporal
     *          column and at least one other numeric column.
     */
    public static boolean isTimeseriesTable( TableLevel table ) {
        boolean hasTemporalColumn = false;
        boolean hasValueColumn = false;
        for( ColumnLevel column : table.columnsList ) {
            if (column.geomColumn != null || column.columnType == null) {
                continue;
            }
            if (!hasTemporalColumn && isTemporalColumn(column.columnName, column.columnType)) {
                hasTemporalColumn = true;
                continue;
            }
            if (!hasValueColumn && !column.isPK && isNumericColumn(column.columnType)) {
                hasValueColumn = true;
            }
        }
        return hasTemporalColumn && hasValueColumn;
    }

    private static boolean isTemporalColumn( String columnName, String columnType ) {
        String typeUpper = columnType.toUpperCase();
        // native date/time types: not all covered by EDataType (e.g. plain "TIMESTAMP"), so
        // check the raw JDBC type name directly rather than relying on EDataType alone.
        if (typeUpper.contains("DATE") || typeUpper.contains("TIMESTAMP")) {
            return true;
        }
        EDataType dataType;
        try {
            dataType = EDataType.getType4Name(columnType);
        } catch (Exception e) {
            return false;
        }
        if (dataType != EDataType.LONG && dataType != EDataType.INTEGER) {
            return false;
        }
        String nameLower = columnName.toLowerCase();
        for( String hint : TEMPORAL_NAME_HINTS ) {
            if (nameLower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNumericColumn( String columnType ) {
        try {
            EDataType dataType = EDataType.getType4Name(columnType);
            return dataType == EDataType.DOUBLE || dataType == EDataType.FLOAT || dataType == EDataType.INTEGER
                    || dataType == EDataType.LONG;
        } catch (Exception e) {
            return false;
        }
    }
}
