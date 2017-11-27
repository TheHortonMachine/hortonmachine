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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.utils.DbsUtilities;

/**
 * Class representing a db level.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbLevel {
    public String dbName;

    public List<TypeLevel> typesList = new ArrayList<TypeLevel>();

    @Override
    public String toString() {
        return dbName;
    }

    /**
     * Get the {@link DbLevel} for a database.
     * 
     * @param db the database.
     * @param types the table tyeps to filter.
     * @return the {@link DbLevel}.
     * @throws Exception
     */
    public static DbLevel getDbLevel( ASpatialDb db, String... types ) throws Exception {
        DbLevel dbLevel = new DbLevel();
        String databasePath = db.getDatabasePath();
        File dbFile = new File(databasePath);

        String dbName = DbsUtilities.getNameWithoutExtention(dbFile);
        dbLevel.dbName = dbName;
        HashMap<String, List<String>> currentDatabaseTablesMap = db.getTablesMap(true);
        for( String typeName : types ) {
            TypeLevel typeLevel = new TypeLevel();
            typeLevel.typeName = typeName;
            List<String> tablesList = currentDatabaseTablesMap.get(typeName);
            for( String tableName : tablesList ) {
                TableLevel tableLevel = new TableLevel();
                tableLevel.parent = dbLevel;
                tableLevel.tableName = tableName;

                ETableType tableType = db.getTableType(tableName);
                tableLevel.tableType = tableType;

                GeometryColumn geometryColumns = null;
                try {
                    geometryColumns = db.getGeometryColumnsForTable(tableName);
                } catch (Exception e1) {
                    // ignore
                    e1.printStackTrace();
                }
                List<ForeignKey> foreignKeys = new ArrayList<>();
                try {
                    foreignKeys = db.getForeignKeys(tableName);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                List<Index> indexes = new ArrayList<>();
                try {
                    indexes = db.getIndexes(tableName);
                } catch (Exception e) {
                }

                tableLevel.isGeo = geometryColumns != null;
                List<String[]> tableInfo;
                try {
                    tableInfo = db.getTableColumns(tableName);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                for( String[] columnInfo : tableInfo ) {
                    ColumnLevel columnLevel = new ColumnLevel();
                    columnLevel.parent = tableLevel;
                    String columnName = columnInfo[0];
                    String columnType = columnInfo[1];
                    String columnPk = columnInfo[2];
                    columnLevel.columnName = columnName;
                    columnLevel.columnType = columnType;
                    columnLevel.isPK = columnPk.equals("1") ? true : false;
                    if (geometryColumns != null && columnName.equalsIgnoreCase(geometryColumns.geometryColumnName)) {
                        columnLevel.geomColumn = geometryColumns;
                    }
                    for( ForeignKey fKey : foreignKeys ) {
                        if (fKey.from.equals(columnName)) {
                            columnLevel.setFkReferences(fKey);
                        }
                    }
                    for( Index index : indexes ) {
                        if (index.columns.contains(columnName)) {
                            columnLevel.setIndex(index);
                        }
                    }
                    tableLevel.columnsList.add(columnLevel);
                }
                typeLevel.tablesList.add(tableLevel);
            }
            dbLevel.typesList.add(typeLevel);
        }

        return dbLevel;
    }

}
