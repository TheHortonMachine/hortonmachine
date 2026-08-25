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
import java.util.ArrayList;
import java.util.List;

/**
 * Small, dependency-free (RFC4180-ish) CSV helpers: delimiter sniffing, a
 * quote-aware line splitter, and header/sample-rows reading - just enough for
 * schema guessing and import, without pulling in a CSV parsing library.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CsvUtils {
    private CsvUtils() {
    }

    /**
     * Sniffs the delimiter of a CSV header line by counting occurrences of each
     * candidate ({@code , ; \t}) and picking the most frequent one, defaulting to
     * comma if none appear.
     */
    public static char detectDelimiter( String headerLine ) {
        char[] candidates = {',', ';', '\t'};
        char best = ',';
        int bestCount = 0;
        for( char candidate : candidates ) {
            int count = 0;
            for( int i = 0; i < headerLine.length(); i++ ) {
                if (headerLine.charAt(i) == candidate) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Splits one CSV line on {@code delimiter}, honoring double-quoted fields
     * (which may contain the delimiter itself, and escape an embedded quote as
     * {@code ""}). Unquoted fields are trimmed; quoted ones are not.
     */
    public static List<String> splitLine( String line, char delimiter ) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for( int i = 0; i < line.length(); i++ ) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"' && field.length() == 0) {
                    inQuotes = true;
                } else if (c == delimiter) {
                    fields.add(field.toString().trim());
                    field.setLength(0);
                } else {
                    field.append(c);
                }
            }
        }
        fields.add(field.toString().trim());
        return fields;
    }

    /**
     * Reads {@code csvFile}'s raw, unsplit header line - use this to
     * {@link #detectDelimiter(String) detect the delimiter} before calling
     * {@link #readHeader(File, char)}, which needs the delimiter already known.
     * A leading {@code #} (some tools write the header as a comment line, e.g.
     * {@code "# time,value"}) is stripped, along with any whitespace right after it.
     */
    public static String readRawHeaderLine( File csvFile ) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Empty CSV file: " + csvFile);
            }
            return stripLeadingHash(headerLine);
        }
    }

    /**
     * Reads just the header line of {@code csvFile}, already split into column
     * names. A leading {@code #} on the header line is stripped, see
     * {@link #readRawHeaderLine(File)}.
     */
    public static List<String> readHeader( File csvFile, char delimiter ) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Empty CSV file: " + csvFile);
            }
            return splitLine(stripLeadingHash(headerLine), delimiter);
        }
    }

    private static String stripLeadingHash( String line ) {
        String trimmed = line.trim();
        if (trimmed.startsWith("#")) {
            return trimmed.substring(1).trim();
        }
        return line;
    }

    /**
     * Reads up to {@code maxRows} data rows (after the header) from
     * {@code csvFile}, each already split into fields, skipping blank lines.
     */
    public static List<List<String>> readSampleRows( File csvFile, char delimiter, int maxRows ) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            reader.readLine(); // header
            String line;
            while( rows.size() < maxRows && (line = reader.readLine()) != null ) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                rows.add(splitLine(line, delimiter));
            }
        }
        return rows;
    }
}
