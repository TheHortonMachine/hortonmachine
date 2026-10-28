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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lays out SQL on several indented lines, for reading.
 *
 * <pre>
 * SELECT e.id,
 *     p.idsap
 * FROM event e
 * JOIN gpspoint g ON g.id = e.gpspoint_id
 * WHERE e.value &gt; 0
 *     AND p.status = 1
 * ORDER BY e.id
 * </pre>
 *
 * <p>Each clause starts a line; the items of the select, group by and order by lists and the
 * <code>AND</code>/<code>OR</code> of the conditions go on their own indented lines; subqueries in
 * parentheses are indented, function calls stay on one line.</p>
 *
 * <p>Only the whitespace outside of strings, quoted identifiers and comments changes, so the SQL
 * keeps its meaning; the case of the keywords and the spacing within a line are kept.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class SqlFormatter {

    private static final String INDENT = "    ";

    private static final Set<String> CLAUSES = Set.of("SELECT", "FROM", "WHERE", "HAVING", "LIMIT", "OFFSET", "UNION",
            "EXCEPT", "INTERSECT", "WITH", "VALUES", "SET", "RETURNING", "WINDOW", "INSERT", "UPDATE", "DELETE");
    private static final Set<String> JOIN_WORDS = Set.of("JOIN", "LEFT", "RIGHT", "FULL", "INNER", "CROSS", "NATURAL",
            "OUTER");
    private static final Set<String> LIST_CLAUSES = Set.of("SELECT", "GROUP", "ORDER", "SET", "RETURNING", "WITH");
    private static final Set<String> CONDITION_CLAUSES = Set.of("WHERE", "HAVING", "JOIN");

    private enum Type {
        WORD, LITERAL, LINE_COMMENT, BLOCK_COMMENT, OPEN, CLOSE, COMMA, SEMICOLON, OTHER
    }

    /**
     * A piece of SQL.
     *
     * @param type the type.
     * @param text the text.
     * @param spaceBefore <code>true</code> if there was whitespace before it in the SQL.
     */
    private record Token( Type type, String text, boolean spaceBefore ) {
        String upper() {
            return text.toUpperCase(Locale.ROOT);
        }
    }

    /**
     * The parentheses being in: the whole statement and subqueries lay out their clauses, the
     * others (function calls, lists of values) stay on their line.
     */
    private static class Frame {
        final boolean laidOut;
        final int baseIndent;
        final int closeIndent;
        String clause;

        Frame( boolean laidOut, int baseIndent, int closeIndent, String clause ) {
            this.laidOut = laidOut;
            this.baseIndent = baseIndent;
            this.closeIndent = closeIndent;
            this.clause = clause;
        }
    }

    private final StringBuilder out = new StringBuilder();
    private int lineIndent = 0;
    private boolean atLineStart = true;

    private SqlFormatter() {
    }

    /**
     * @param sql the SQL.
     * @return the SQL laid out on indented lines.
     */
    public static String format( String sql ) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        return new SqlFormatter().layout(tokenize(sql));
    }

    private String layout( List<Token> tokens ) {
        Deque<Frame> frames = new ArrayDeque<>();
        Frame frame = new Frame(true, 0, 0, null);
        boolean betweenPending = false;
        boolean breakPending = false;

        for( int i = 0; i < tokens.size(); i++ ) {
            Token token = tokens.get(i);
            Token next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
            if (breakPending) {
                newLine(frame.clause == null ? frame.baseIndent : frame.baseIndent + 1);
                breakPending = false;
            }
            switch( token.type() ) {
            case LINE_COMMENT:
                emit(token);
                breakPending = true;
                break;
            case WORD: {
                String upper = token.upper();
                boolean isFunction = next != null && next.type() == Type.OPEN && !next.spaceBefore();
                if (frame.laidOut && !isFunction) {
                    boolean twoWordClause = (upper.equals("GROUP") || upper.equals("ORDER")) && next != null
                            && next.upper().equals("BY");
                    if (CLAUSES.contains(upper) || twoWordClause) {
                        newLine(frame.baseIndent);
                        emit(token);
                        if (twoWordClause) {
                            emit(next);
                            i++;
                        }
                        frame.clause = upper;
                        break;
                    }
                    if (JOIN_WORDS.contains(upper) && startsJoin(tokens, i)) {
                        newLine(frame.baseIndent);
                        while( i < tokens.size() && tokens.get(i).type() == Type.WORD
                                && JOIN_WORDS.contains(tokens.get(i).upper()) ) {
                            emit(tokens.get(i));
                            if (tokens.get(i).upper().equals("JOIN")) {
                                break;
                            }
                            i++;
                        }
                        frame.clause = "JOIN";
                        break;
                    }
                    if (upper.equals("BETWEEN")) {
                        betweenPending = true;
                    } else if ((upper.equals("AND") || upper.equals("OR")) && CONDITION_CLAUSES.contains(frame.clause)) {
                        if (upper.equals("AND") && betweenPending) {
                            betweenPending = false;
                        } else {
                            newLine(frame.baseIndent + 1);
                        }
                    }
                }
                emit(token);
                break;
            }
            case OPEN: {
                emit(token);
                boolean subquery = next != null && next.type() == Type.WORD
                        && (next.upper().equals("SELECT") || next.upper().equals("WITH"));
                frames.push(frame);
                if (subquery) {
                    frame = new Frame(true, lineIndent + 1, lineIndent, null);
                } else {
                    frame = new Frame(false, frame.baseIndent, lineIndent, frame.clause);
                }
                break;
            }
            case CLOSE:
                if (frame.laidOut && !frames.isEmpty()) {
                    newLine(frame.closeIndent);
                }
                emit(token);
                if (!frames.isEmpty()) {
                    frame = frames.pop();
                }
                break;
            case COMMA:
                emit(token);
                if (frame.laidOut && LIST_CLAUSES.contains(frame.clause)) {
                    newLine(frame.baseIndent + 1);
                }
                break;
            case SEMICOLON:
                emit(token);
                frames.clear();
                frame = new Frame(true, 0, 0, null);
                if (next != null) {
                    // a blank line between statements
                    out.append("\n\n");
                    newLine(0);
                }
                break;
            default:
                emit(token);
                break;
            }
        }
        return out.toString().stripTrailing();
    }

    /**
     * @return <code>true</code> if the words from the index lead to a JOIN.
     */
    private static boolean startsJoin( List<Token> tokens, int index ) {
        for( int i = index; i < tokens.size(); i++ ) {
            Token token = tokens.get(i);
            if (token.type() != Type.WORD || !JOIN_WORDS.contains(token.upper())) {
                return false;
            }
            if (token.upper().equals("JOIN")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Start a new line, unless the current one is still empty, which is then only re-indented.
     */
    private void newLine( int indent ) {
        int end = out.length();
        while( end > 0 && out.charAt(end - 1) == ' ' ) {
            end--;
        }
        out.setLength(end);
        // an empty line only gets its indentation replaced
        int lineStart = out.lastIndexOf("\n") + 1;
        if (out.substring(lineStart).isBlank()) {
            out.setLength(lineStart);
        } else {
            out.append('\n');
        }
        lineIndent = Math.max(0, indent);
        out.append(INDENT.repeat(lineIndent));
        atLineStart = true;
    }

    private void emit( Token token ) {
        if (!atLineStart && token.spaceBefore()) {
            out.append(' ');
        }
        out.append(token.text());
        atLineStart = false;
    }

    private static List<Token> tokenize( String sql ) {
        List<Token> tokens = new ArrayList<>();
        int n = sql.length();
        int i = 0;
        boolean space = false;
        while( i < n ) {
            char c = sql.charAt(i);
            char next = i + 1 < n ? sql.charAt(i + 1) : 0;
            if (Character.isWhitespace(c)) {
                space = true;
                i++;
                continue;
            }
            int start = i;
            Type type;
            if (c == '-' && next == '-') {
                while( i < n && sql.charAt(i) != '\n' ) {
                    i++;
                }
                type = Type.LINE_COMMENT;
            } else if (c == '/' && next == '*') {
                int close = sql.indexOf("*/", i + 2);
                i = close == -1 ? n : close + 2;
                type = Type.BLOCK_COMMENT;
            } else if (c == '\'' || c == '"' || c == '`') {
                i++;
                while( i < n ) {
                    if (sql.charAt(i) == c) {
                        if (i + 1 < n && sql.charAt(i + 1) == c) {
                            i += 2;
                            continue;
                        }
                        break;
                    }
                    i++;
                }
                i = Math.min(i + 1, n);
                type = Type.LITERAL;
            } else if (Character.isLetterOrDigit(c) || c == '_') {
                while( i < n && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_') ) {
                    i++;
                }
                type = Type.WORD;
            } else if (c == '(') {
                i++;
                type = Type.OPEN;
            } else if (c == ')') {
                i++;
                type = Type.CLOSE;
            } else if (c == ',') {
                i++;
                type = Type.COMMA;
            } else if (c == ';') {
                i++;
                type = Type.SEMICOLON;
            } else {
                i++;
                type = Type.OTHER;
            }
            tokens.add(new Token(type, sql.substring(start, i), space));
            space = false;
        }
        return tokens;
    }
}
