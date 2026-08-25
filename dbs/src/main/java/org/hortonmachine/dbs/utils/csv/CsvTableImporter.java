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
package org.hortonmachine.dbs.utils.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * Creates a table from a CSV file's guessed schema, or imports a CSV's rows
 * into an already existing table, matching CSV columns to table columns by
 * name.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CsvTableImporter {
    private CsvTableImporter() {
    }

    public static final int BATCH_SIZE = 500;

    /**
     * Creates {@code newTableName} with one column per entry of {@code schema}
     * (in order, using each column's {@link CsvColumnType#toSqlType(ADatabaseSyntaxHelper)}),
     * then imports every row of {@code csvFile} into it.
     *
     * @param primaryKey how to set up the new table's primary key, or {@code null} for
     *          {@link CsvPrimaryKeyOption#none()}. For {@link CsvPrimaryKeyOption.Mode#GENERATED},
     *          the generated column is added to the table but, having no CSV counterpart, is left
     *          for the database to fill in on insert.
     */
    public static void createTableFromCsv( ADb db, SqlName newTableName, File csvFile, char delimiter,
            List<CsvColumnSchema> schema, CsvPrimaryKeyOption primaryKey ) throws Exception {
        if (primaryKey == null) {
            primaryKey = CsvPrimaryKeyOption.none();
        }
        ADatabaseSyntaxHelper syntaxHelper = db.getType().getDatabaseSyntaxHelper();
        List<String> fieldData = new ArrayList<>();
        List<String> targetColumnNames = new ArrayList<>();

        if (primaryKey.mode == CsvPrimaryKeyOption.Mode.GENERATED) {
            fieldData.add(primaryKey.generatedColumnName + " " + syntaxHelper.LONG_PRIMARYKEY_AUTOINCREMENT());
        }
        for( CsvColumnSchema col : schema ) {
            String fieldDef = col.name + " " + col.type.toSqlType(syntaxHelper);
            if (primaryKey.mode == CsvPrimaryKeyOption.Mode.EXISTING_COLUMN && col.name.equals(primaryKey.existingColumnName)) {
                fieldDef += " " + syntaxHelper.PRIMARYKEY();
            }
            fieldData.add(fieldDef);
            targetColumnNames.add(col.name);
        }
        if (primaryKey.mode == CsvPrimaryKeyOption.Mode.EXISTING_COLUMN) {
            // check the whole file up front: a duplicate found mid-import (e.g. after several
            // batches already committed) is a lot messier to diagnose and recover from than one
            // caught before anything is created.
            validatePrimaryKeyUniqueness(csvFile, delimiter, schema, primaryKey.existingColumnName);
        }
        db.createTable(newTableName, fieldData.toArray(new String[0]));
        importRows(db, newTableName, csvFile, delimiter, schema, targetColumnNames);
    }

    /**
     * The outcome of matching a CSV's columns against an existing table's
     * columns by name (case-insensitive), before actually importing anything -
     * so a caller can show the mismatches to the user and let them confirm.
     */
    public static class ColumnMatch {
        /** CSV column names that also exist in the table: these get imported. */
        public final List<String> matched = new ArrayList<>();
        /** CSV column names with no matching table column: these are ignored. */
        public final List<String> unmatchedCsvColumns = new ArrayList<>();
        /** Table column names with no matching CSV column: these are left untouched. */
        public final List<String> unmatchedTableColumns = new ArrayList<>();
    }

    /**
     * Matches {@code csvSchema}'s column names against {@code tableColumnNames} case-insensitively,
     * without importing anything - use this to show the user what will and won't be imported before
     * calling {@link #importCsvIntoTable(ADb, SqlName, File, char, List, ColumnMatch)}.
     */
    public static ColumnMatch matchColumns( List<CsvColumnSchema> csvSchema, List<String> tableColumnNames ) {
        ColumnMatch match = new ColumnMatch();
        List<String> remainingTableColumns = new ArrayList<>(tableColumnNames);
        for( CsvColumnSchema csvCol : csvSchema ) {
            String found = null;
            for( String tableCol : remainingTableColumns ) {
                if (tableCol.equalsIgnoreCase(csvCol.name)) {
                    found = tableCol;
                    break;
                }
            }
            if (found != null) {
                match.matched.add(csvCol.name);
                remainingTableColumns.remove(found);
            } else {
                match.unmatchedCsvColumns.add(csvCol.name);
            }
        }
        match.unmatchedTableColumns.addAll(remainingTableColumns);
        return match;
    }

    /**
     * Imports every row of {@code csvFile} into the already existing {@code tableName}, inserting
     * only the columns in {@code match.matched} (see {@link #matchColumns(List, List)}).
     */
    public static void importCsvIntoTable( ADb db, SqlName tableName, File csvFile, char delimiter,
            List<CsvColumnSchema> csvSchema, ColumnMatch match ) throws Exception {
        List<CsvColumnSchema> matchedSchema = new ArrayList<>();
        List<String> matchedColumnNames = new ArrayList<>();
        for( CsvColumnSchema csvCol : csvSchema ) {
            if (match.matched.contains(csvCol.name)) {
                matchedSchema.add(csvCol);
                matchedColumnNames.add(csvCol.name);
            }
        }
        importRows(db, tableName, csvFile, delimiter, matchedSchema, matchedColumnNames);
    }

    /**
     * Reads {@code csvFile} (skipping its header), extracting only the columns present in
     * {@code csvSchema} (in that order) and inserting them into {@code targetColumnNames} (same
     * order, 1:1), batching every {@link #BATCH_SIZE} rows in a single transaction.
     */
    private static void importRows( ADb db, SqlName tableName, File csvFile, char delimiter,
            List<CsvColumnSchema> csvSchema, List<String> targetColumnNames ) throws Exception {
        if (csvSchema.isEmpty()) {
            return;
        }
        // map from the csv's full column order to the (schema, targetName) pairs to extract
        List<String> allCsvColumns = CsvUtils.readHeader(csvFile, delimiter);
        Map<Integer, CsvColumnSchema> csvIndexToSchema = new LinkedHashMap<>();
        Map<Integer, String> csvIndexToTargetName = new LinkedHashMap<>();
        for( int i = 0; i < csvSchema.size(); i++ ) {
            String csvName = csvSchema.get(i).name;
            int csvIndex = allCsvColumns.indexOf(csvName);
            if (csvIndex == -1) {
                throw new IllegalArgumentException("CSV column not found in file header: " + csvName);
            }
            csvIndexToSchema.put(csvIndex, csvSchema.get(i));
            csvIndexToTargetName.put(csvIndex, targetColumnNames.get(i));
        }

        String cols = String.join(",", targetColumnNames);
        String questionMarks = targetColumnNames.stream().map(c -> "?").collect(Collectors.joining(","));
        String insertSql = "INSERT INTO " + tableName.fixedDoubleName + " (" + cols + ") VALUES (" + questionMarks + ")";

        db.execOnConnection(connection -> {
            connection.setAutoCommit(false);
            boolean committed = false;
            try (IHMPreparedStatement pStmt = connection.prepareStatement(insertSql);
                    BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                reader.readLine(); // header, already consumed above
                String line;
                int pendingBatch = 0;
                while( (line = reader.readLine()) != null ) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    List<String> fields = CsvUtils.splitLine(line, delimiter);
                    int paramIndex = 1;
                    for( Integer csvIndex : csvIndexToSchema.keySet() ) {
                        String rawValue = csvIndex < fields.size() ? fields.get(csvIndex) : null;
                        setParam(pStmt, paramIndex++, csvIndexToSchema.get(csvIndex), rawValue);
                    }
                    pStmt.addBatch();
                    pendingBatch++;
                    if (pendingBatch >= BATCH_SIZE) {
                        pStmt.executeBatch();
                        pendingBatch = 0;
                    }
                }
                if (pendingBatch > 0) {
                    pStmt.executeBatch();
                }
                connection.commit();
                committed = true;
            } finally {
                // a failed import must not leave partial rows behind - those would collide with
                // themselves on a retry into the same table. Flipping autoCommit back on with a
                // pending transaction can otherwise commit it implicitly instead of discarding it.
                if (!committed) {
                    connection.rollback();
                }
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    /**
     * Reads the whole of {@code csvFile}'s {@code pkColumnName} column (parsed per its schema
     * entry) and throws if any two rows produce the same value - catching a duplicate-primary-key
     * mistake (most often a wrong date pattern, e.g. 12-hour {@code hh} instead of 24-hour
     * {@code HH}, silently folding afternoon hours onto their morning counterparts) before
     * anything is created or inserted, rather than mid-import.
     */
    private static void validatePrimaryKeyUniqueness( File csvFile, char delimiter, List<CsvColumnSchema> schema,
            String pkColumnName ) throws Exception {
        CsvColumnSchema pkSchema = null;
        for( CsvColumnSchema col : schema ) {
            if (col.name.equals(pkColumnName)) {
                pkSchema = col;
                break;
            }
        }
        if (pkSchema == null) {
            return;
        }
        List<String> allCsvColumns = CsvUtils.readHeader(csvFile, delimiter);
        int csvIndex = allCsvColumns.indexOf(pkColumnName);
        if (csvIndex == -1) {
            return;
        }
        Map<Object, Integer> seenValueToLineNumber = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            reader.readLine(); // header
            String line;
            int lineNumber = 1;
            while( (line = reader.readLine()) != null ) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> fields = CsvUtils.splitLine(line, delimiter);
                if (csvIndex >= fields.size() || fields.get(csvIndex).isEmpty()) {
                    continue;
                }
                String rawValue = fields.get(csvIndex);
                Object parsedValue = parseTypedValue(pkSchema, rawValue);
                Integer previousLine = seenValueToLineNumber.put(parsedValue, lineNumber);
                if (previousLine != null) {
                    throw new IllegalArgumentException(
                            "Column \"" + pkColumnName + "\" can't be used as primary key: value \"" + rawValue
                                    + "\" appears on both line " + previousLine + " and line " + lineNumber
                                    + (pkSchema.type == CsvColumnType.DATE
                                            ? ". If this is a duplicate that shouldn't be there, double check the date pattern \""
                                                    + pkSchema.datePattern + "\" (e.g. 12-hour \"hh\" instead of 24-hour \"HH\" "
                                                    + "would fold afternoon and morning hours together)."
                                            : "."));
                }
            }
        }
    }

    private static void setParam( IHMPreparedStatement pStmt, int index, CsvColumnSchema column, String rawValue )
            throws Exception {
        if (rawValue == null || rawValue.isEmpty()) {
            pStmt.setObject(index, null);
            return;
        }
        Object value = parseTypedValue(column, rawValue);
        if (value instanceof Integer) {
            pStmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            pStmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            pStmt.setDouble(index, (Double) value);
        } else {
            pStmt.setString(index, (String) value);
        }
    }

    private static Object parseTypedValue( CsvColumnSchema column, String rawValue ) throws Exception {
        switch( column.type ) {
        case INTEGER:
            return Integer.parseInt(rawValue);
        case LONG:
            return Long.parseLong(rawValue);
        case DOUBLE:
            return Double.parseDouble(rawValue);
        case DATE:
            SimpleDateFormat format = new SimpleDateFormat(column.datePattern);
            // epochs for timeseries are always meant to be UTC
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            format.setLenient(false);
            return format.parse(rawValue).getTime();
        case TEXT:
        default:
            return rawValue;
        }
    }
}
