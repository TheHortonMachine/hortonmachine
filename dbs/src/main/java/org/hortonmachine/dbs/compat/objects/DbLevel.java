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
import java.util.Map;
import java.util.Map.Entry;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;

/**
 * Class representing a db level.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbLevel {
    public ADb parent;
    public String dbName;

    public List<SchemaLevel> schemasList = new ArrayList<SchemaLevel>();

    @Override
    public String toString() {
        return dbName;
    }

    /**
     * Get the {@link DbLevel} for a database.
     * 
     * @param db the database.
     * @return the {@link DbLevel}.
     * @throws Exception
     */
    public static DbLevel getDbLevel( ADb db ) throws Exception {
        DbLevel dbLevel = createEmptyDbLevel(db);

        HashMap<String, HashMap<String, List<String>>> currentDatabaseSchema2Type2TablesMap = db.getTablesMap();

        for( String schemaName : currentDatabaseSchema2Type2TablesMap.keySet() ) {
            var type2TablesMap = currentDatabaseSchema2Type2TablesMap.get(schemaName);
            SchemaLevel schemaLevel = getSchemaLevel(db, dbLevel, schemaName, type2TablesMap);
            if (schemaLevel != null) {
                dbLevel.schemasList.add(schemaLevel);
            }
        }
        return dbLevel;
    }

    /**
     * Create an empty root level for a database.
     *
     * @param db the database.
     * @return the empty {@link DbLevel}.
     */
    public static DbLevel createEmptyDbLevel( ADb db ) {
        DbLevel dbLevel = new DbLevel();
        String databasePath = db.getDatabasePath();
        File dbFile = new File(databasePath);

        String dbName = DbsUtilities.getNameWithoutExtention(dbFile);
        dbLevel.dbName = dbName;
        dbLevel.parent = db;
        return dbLevel;
    }

    /**
     * Build a schema level and its table/column metadata.
     *
     * @param db the database.
     * @param dbLevel the parent database level.
     * @param schemaName the schema name.
     * @param type2TablesMap tables grouped by table type.
     * @return the schema level, or {@code null} when the schema has no table map.
     */
    public static SchemaLevel getSchemaLevel( ADb db, DbLevel dbLevel, String schemaName,
            Map<String, List<String>> type2TablesMap ) {
        if (type2TablesMap == null) {
            return null;
        }

        SchemaLevel schemaLevel = new SchemaLevel();
        schemaLevel.parent = dbLevel;
        schemaLevel.schemaName = schemaName;

        for( Entry<String, List<String>> entry : type2TablesMap.entrySet() ) {
            String typeName = entry.getKey();
            List<String> tablesMap = entry.getValue();
            TableTypeLevel tableTypeLevel = new TableTypeLevel();
            tableTypeLevel.parent = schemaLevel;
            tableTypeLevel.typeName = typeName;
            ETableType tableType = ETableType.fromType(typeName);
            for( String tableName : tablesMap ) {
                TableName tableNameObj = new TableName(tableName, schemaName, tableType);
                TableLevel tableLevel = new TableLevel();
                tableLevel.parent = tableTypeLevel;
                tableLevel.tableName = tableNameObj;
                tableLevel.tableType = tableType;
                getColumnsList(db, tableLevel);
                tableTypeLevel.tablesList.add(tableLevel);
            }
            schemaLevel.tableTypesList.add(tableTypeLevel);
        }
        return schemaLevel;
    }

    private static void getColumnsList( ADb db, TableLevel tableLevel ) {
        TableName tableName = tableLevel.tableName;
        var columnsListTmp = new ArrayList<ColumnLevel>();
        SqlName sqlName = SqlName.m(tableName.getFullName());
        GeometryColumn geometryColumns = null;
        try {
            if (db instanceof ASpatialDb) {
                geometryColumns = ((ASpatialDb) db).getGeometryColumnsForTable(sqlName);
            }
        } catch (Exception e1) {
            // ignore
            e1.printStackTrace();
        }
        tableLevel.isGeo = geometryColumns != null;
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
            return ;
        }
        for (String[] columnInfo : tableInfo) {
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
        tableLevel.columnsList = columnsListTmp;
        tableLevel.isLoaded = true;
    }



    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void handleDocument( Object parent, Map<String, Object> map, HashMap<String, GeometryColumn> spatialIndexes ) {
        for( Entry<String, Object> entry : map.entrySet() ) {
            String key = entry.getKey();
            Object value = entry.getValue().toString();
            String type = value.getClass().getSimpleName();
            LeafLevel leafLevel = new LeafLevel();
            if (value instanceof Map) {
                type = "Document";
                handleDocument(leafLevel, (Map) value, spatialIndexes);
            } else {
                type = value.toString();
            }
            leafLevel.parent = parent;
            leafLevel.columnName = key;
            leafLevel.columnType = type;
            leafLevel.isPK = key.equals("_id");
            if (parent instanceof LeafLevel) {
                ((LeafLevel) parent).leafsList.add(leafLevel);
                leafLevel.geomColumn = spatialIndexes.get(key);;
            } else if (parent instanceof ColumnLevel) {
                ((ColumnLevel) parent).leafsList.add(leafLevel);
                leafLevel.geomColumn = spatialIndexes.get(key);;
            }
        }

    }
}
