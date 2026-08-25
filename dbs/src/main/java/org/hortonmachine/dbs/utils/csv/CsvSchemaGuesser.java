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

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Guesses a {@link CsvColumnSchema} per column of a CSV file, from its header
 * and a sample of its first rows: reads the header for column names, then for
 * each column tries, in order, {@code INTEGER}, {@code LONG}, {@code DOUBLE},
 * a fixed set of common {@code DATE} patterns, falling back to {@code TEXT} -
 * the first that parses every non-blank sampled value for that column wins.
 *
 * <p>
 * The guess is only a starting point: callers are expected to let the user
 * review and, especially for the numeric/date choices, correct it (e.g. a
 * column that looks {@code INTEGER} in the sample might carry decimals or an
 * out-of-pattern date further down the file).
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CsvSchemaGuesser {
    private CsvSchemaGuesser() {
    }

    /** Common date/time patterns tried, in order, when guessing a DATE column. */
    private static final String[] DATE_PATTERNS = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd", "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm", "dd/MM/yyyy", "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm", "MM/dd/yyyy"};

    public static final int DEFAULT_SAMPLE_ROWS = 50;

    /**
     * @param csvFile the CSV file to guess the schema of.
     * @param delimiter the field delimiter, see {@link CsvUtils#detectDelimiter(String)}.
     * @param sampleRows how many data rows (after the header) to sample.
     * @return one {@link CsvColumnSchema} per CSV column, in file order.
     */
    public static List<CsvColumnSchema> guessSchema( File csvFile, char delimiter, int sampleRows ) throws Exception {
        List<String> header = CsvUtils.readHeader(csvFile, delimiter);
        List<List<String>> sampleData = CsvUtils.readSampleRows(csvFile, delimiter, sampleRows);

        List<CsvColumnSchema> schema = new ArrayList<>();
        for( int col = 0; col < header.size(); col++ ) {
            List<String> values = new ArrayList<>();
            for( List<String> row : sampleData ) {
                if (col < row.size() && !row.get(col).isEmpty()) {
                    values.add(row.get(col));
                }
            }
            schema.add(guessColumn(header.get(col), values));
        }
        return schema;
    }

    private static CsvColumnSchema guessColumn( String name, List<String> sampleValues ) {
        if (sampleValues.isEmpty()) {
            return new CsvColumnSchema(name, CsvColumnType.TEXT);
        }
        if (allMatch(sampleValues, CsvSchemaGuesser::isInteger)) {
            return new CsvColumnSchema(name, CsvColumnType.INTEGER);
        }
        if (allMatch(sampleValues, CsvSchemaGuesser::isLong)) {
            return new CsvColumnSchema(name, CsvColumnType.LONG);
        }
        if (allMatch(sampleValues, CsvSchemaGuesser::isDouble)) {
            return new CsvColumnSchema(name, CsvColumnType.DOUBLE);
        }
        for( String pattern : DATE_PATTERNS ) {
            if (allMatch(sampleValues, value -> matchesDatePattern(value, pattern))) {
                return new CsvColumnSchema(name, CsvColumnType.DATE, pattern);
            }
        }
        return new CsvColumnSchema(name, CsvColumnType.TEXT);
    }

    /**
     * Checks whether {@code value} parses fully against {@code pattern} (a
     * {@link SimpleDateFormat} pattern) - use this to validate a user-edited date
     * pattern (e.g. against preview rows) before relying on it for an actual
     * import, since a wrong-case pattern (Java's format is case-sensitive - {@code
     * M} is month, {@code m} is minute; {@code Y} is week-year, not {@code y}
     * year; {@code D} is day-of-year, not {@code d} day-of-month) can otherwise
     * fail deep into a batch insert instead of up front.
     */
    public static boolean matchesPattern( String value, String pattern ) {
        return matchesDatePattern(value, pattern);
    }

    private static boolean allMatch( List<String> values, java.util.function.Predicate<String> predicate ) {
        for( String value : values ) {
            if (!predicate.test(value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInteger( String value ) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isLong( String value ) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDouble( String value ) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchesDatePattern( String value, String pattern ) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        // epochs for timeseries are always meant to be UTC
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        format.parse(value, pos);
        return pos.getIndex() == value.length() && pos.getErrorIndex() == -1;
    }
}
