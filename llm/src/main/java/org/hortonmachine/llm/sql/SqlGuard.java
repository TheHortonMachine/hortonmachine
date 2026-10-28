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
package org.hortonmachine.llm.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks on generated SQL: whether it only reads, and adding a row limit to queries.
 *
 * <p>The check is lexical: comments, string literals and quoted identifiers are blanked out, then
 * the words of the statements are looked at. It is a safety net for the user confirmation, not a
 * proof: SQL that can't be recognized as a pure query is reported as not read-only.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class SqlGuard {

    private static final Set<String> READ_STARTS = new HashSet<>(Arrays.asList("SELECT", "WITH", "VALUES", "EXPLAIN"));

    /**
     * Words that make a statement write, also inside a query (a CTE with DML, SELECT INTO, functions
     * of the spatial extensions that change the metadata).
     */
    private static final Set<String> WRITE_WORDS = new HashSet<>(Arrays.asList("INSERT", "UPDATE", "DELETE", "MERGE",
            "UPSERT", "CREATE", "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE", "ATTACH", "DETACH", "VACUUM", "REINDEX", "COPY",
            "CALL", "DO", "INTO", "LOCK", "ADDGEOMETRYCOLUMN", "RECOVERGEOMETRYCOLUMN", "DISCARDGEOMETRYCOLUMN",
            "CREATESPATIALINDEX", "DISABLESPATIALINDEX", "INITSPATIALMETADATA", "DROPGEOTABLE", "DROPTABLE",
            "UPDATELAYERSTATISTICS", "ADDGEOMETRYCOLUMNS", "DROPGEOMETRYCOLUMN"));

    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

    private SqlGuard() {
    }

    /**
     * @param sql the SQL to check.
     * @return <code>true</code> if every statement of the SQL is recognized as only reading data.
     */
    public static boolean isReadOnly( String sql ) {
        List<String> statements = splitStatements(mask(sql));
        if (statements.isEmpty()) {
            return false;
        }
        for( String statement : statements ) {
            List<String> words = words(statement);
            if (words.isEmpty()) {
                return false;
            }
            String first = words.get(0);
            boolean readStart = READ_STARTS.contains(first) || (first.equals("PRAGMA") && !statement.contains("="));
            if (!readStart) {
                return false;
            }
            for( String word : words ) {
                if (WRITE_WORDS.contains(word)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param sql the SQL to check.
     * @return <code>true</code> if the SQL holds exactly one statement.
     */
    public static boolean isSingleStatement( String sql ) {
        return splitStatements(mask(sql)).size() == 1;
    }

    /**
     * Add a row limit to a query that has none.
     *
     * <p>Only a single <code>SELECT</code> or <code>WITH</code> statement without a top level
     * <code>LIMIT</code>, <code>FETCH</code> or <code>TOP</code> gets the limit; any other SQL is
     * returned as it is. All the supported databases accept <code>LIMIT n</code>.</p>
     *
     * @param sql the SQL.
     * @param limit the limit, 0 or less for none.
     * @return the SQL with the limit, or the same SQL.
     */
    public static String applyLimit( String sql, int limit ) {
        if (limit <= 0) {
            return sql;
        }
        String masked = mask(sql);
        List<String> statements = splitStatements(masked);
        if (statements.size() != 1) {
            return sql;
        }
        List<String> words = words(statements.get(0));
        if (words.isEmpty() || !(words.get(0).equals("SELECT") || words.get(0).equals("WITH"))) {
            return sql;
        }
        if (hasTopLevelWord(masked, "LIMIT", "FETCH", "TOP")) {
            return sql;
        }
        // the masked text has the same positions: cut after the last real character, dropping a final ; and comments
        int end = masked.length();
        while( end > 0 && Character.isWhitespace(masked.charAt(end - 1)) ) {
            end--;
        }
        if (end > 0 && masked.charAt(end - 1) == ';') {
            end--;
        }
        return sql.substring(0, end).stripTrailing() + "\nLIMIT " + limit;
    }

    /**
     * Blank out comments, string literals and quoted identifiers, keeping the positions.
     *
     * @param sql the SQL.
     * @return the masked SQL, as long as the original.
     */
    static String mask( String sql ) {
        char[] chars = sql.toCharArray();
        int i = 0;
        while( i < chars.length ) {
            char c = chars[i];
            char next = i + 1 < chars.length ? chars[i + 1] : 0;
            if (c == '-' && next == '-') {
                while( i < chars.length && chars[i] != '\n' ) {
                    chars[i++] = ' ';
                }
            } else if (c == '/' && next == '*') {
                int start = i;
                int close = sql.indexOf("*/", i + 2);
                int stop = close == -1 ? chars.length : close + 2;
                for( int j = start; j < stop; j++ ) {
                    if (chars[j] != '\n') {
                        chars[j] = ' ';
                    }
                }
                i = stop;
            } else if (c == '\'' || c == '"') {
                int j = i + 1;
                while( j < chars.length ) {
                    if (chars[j] == c) {
                        if (j + 1 < chars.length && chars[j + 1] == c) {
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    j++;
                }
                int stop = Math.min(j + 1, chars.length);
                for( int k = i; k < stop; k++ ) {
                    if (chars[k] != '\n') {
                        chars[k] = ' ';
                    }
                }
                i = stop;
            } else {
                i++;
            }
        }
        return new String(chars);
    }

    private static List<String> splitStatements( String masked ) {
        List<String> statements = new ArrayList<>();
        for( String part : masked.split(";") ) {
            if (!part.isBlank()) {
                statements.add(part);
            }
        }
        return statements;
    }

    private static List<String> words( String text ) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        while( matcher.find() ) {
            words.add(matcher.group().toUpperCase(Locale.ROOT));
        }
        return words;
    }

    private static boolean hasTopLevelWord( String masked, String... wanted ) {
        Set<String> wantedSet = new HashSet<>(Arrays.asList(wanted));
        int depth = 0;
        int i = 0;
        while( i < masked.length() ) {
            char c = masked.charAt(i);
            if (c == '(') {
                depth++;
                i++;
            } else if (c == ')') {
                depth--;
                i++;
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while( i < masked.length() && (Character.isLetterOrDigit(masked.charAt(i)) || masked.charAt(i) == '_') ) {
                    i++;
                }
                if (depth == 0 && wantedSet.contains(masked.substring(start, i).toUpperCase(Locale.ROOT))) {
                    return true;
                }
            } else {
                i++;
            }
        }
        return false;
    }
}
