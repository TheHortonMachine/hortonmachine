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
package org.hortonmachine.dbs.compat.objects;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * Class representing a db table level.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TableLevel {
    public TableTypeLevel parent;
    public String tableName;
    public boolean isGeo = false;
    public boolean isLoaded = false;

    private List<ColumnLevel> columnsList = null;

    public ColumnLevel getFirstGeometryColumn() {
        if (isGeo) {
            for (ColumnLevel columnLevel : columnsList) {
                if (columnLevel.geomColumn != null) {
                    return columnLevel;
                }
            }
        }
        return null;
    }

    public boolean hasFks() {
        for (ColumnLevel columnLevel : columnsList) {
            if (columnLevel.references != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return tableName;
    }

    public List<ColumnLevel> getColumnsList(ADb db) {
        if (columnsList == null) {
            var columnsListTmp = new ArrayList<ColumnLevel>();
            SqlName sqlName = SqlName.m(tableName);
            GeometryColumn geometryColumns = null;
            try {
                if (db instanceof ASpatialDb) {
                    geometryColumns = ((ASpatialDb) db).getGeometryColumnsForTable(sqlName);
                }
            } catch (Exception e1) {
                // ignore
                e1.printStackTrace();
            }
            List<ForeignKey> foreignKeys = new ArrayList<>();
            try {
                foreignKeys = db.getForeignKeys(sqlName);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<Index> indexes = new ArrayList<>();
            try {
                indexes = db.getIndexes(sqlName);
            } catch (Exception e) {
            }

            List<String[]> tableInfo;
            try {
                tableInfo = db.getTableColumns(sqlName);
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<ColumnLevel>();
            }
            for (String[] columnInfo : tableInfo) {
                ColumnLevel columnLevel = new ColumnLevel();
                columnLevel.parent = this;
                String columnName = columnInfo[0];
                String columnType = columnInfo[1];
                String columnPk = columnInfo[2];
                columnLevel.columnName = columnName;
                columnLevel.columnType = columnType;
                columnLevel.isPK = columnPk.equals("1") ? true : false;
                if (geometryColumns != null && columnName.equalsIgnoreCase(geometryColumns.geometryColumnName)) {
                    columnLevel.geomColumn = geometryColumns;
                    if (columnType.equals("USER-DEFINED")) {
                        EGeometryType guessedType = geometryColumns.geometryType;// TODO check
                                                                                 // EGeometryType.fromSpatialiteCode(geometryColumns.geometryType);
                        if (guessedType != null) {
                            columnLevel.columnType = guessedType.name();
                        }
                    }
                }
                for (ForeignKey fKey : foreignKeys) {
                    if (fKey.from.equals(columnName)) {
                        columnLevel.setFkReferences(fKey);
                    }
                }
                for (Index index : indexes) {
                    if (index.columns.contains(columnName)) {
                        columnLevel.setIndex(index);
                    }
                }
                columnsListTmp.add(columnLevel);
            }
            columnsList = columnsListTmp;
        }
        return columnsList;
    }
}
