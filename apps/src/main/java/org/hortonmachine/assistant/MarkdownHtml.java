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
package org.hortonmachine.assistant;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the markdown of model answers into the simple HTML a Swing <code>JEditorPane</code> shows,
 * with SQL code blocks highlighted.
 *
 * <p>Only the part of markdown models use is handled: headings, paragraphs, bullet and numbered
 * lists, fenced code blocks, inline code, bold, italic and links. Text is escaped first, so the
 * answers can't inject HTML. Colors are inline, since the style sheets of Swing's HTML support are
 * shared by all the panes of the application. Nothing is left that can't wrap (no <code>pre</code>),
 * so the HTML fits a pane of any width.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class MarkdownHtml {

    /**
     * The colors of a light or dark look and feel.
     *
     * @param codeBackground background of code blocks.
     * @param keyword SQL keywords.
     * @param string SQL strings.
     * @param number SQL numbers.
     * @param comment SQL comments.
     * @param inlineCode inline code.
     */
    public record Palette( String codeBackground, String keyword, String string, String number, String comment,
            String inlineCode ) {
    }

    public static final Palette LIGHT = new Palette("#eef1f5", "#0b4fa8", "#2e7d32", "#a0522d", "#8a8a8a", "#8a2c0d");
    public static final Palette DARK = new Palette("#2b2f36", "#7fb2ff", "#8fd18f", "#e0a070", "#8a8a8a", "#e8a27c");

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^[-*+]\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^(\\d+)[.)]\\s+(.*)$");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<![*\\w])\\*(?![\\s*])(.+?)(?<!\\s)\\*(?!\\*)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^)\\s]+)\\)");
    private static final Pattern SQL_START = Pattern.compile(
            "^\\s*(SELECT|WITH|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|PRAGMA|EXPLAIN)\\b", Pattern.CASE_INSENSITIVE);

    private static Set<String> sqlKeywords;

    private MarkdownHtml() {
    }

    /**
     * @param markdown the markdown text.
     * @param palette the colors to use.
     * @return the HTML fragment, to put inside a body.
     */
    public static String toHtml( String markdown, Palette palette ) {
        StringBuilder out = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();
        StringBuilder code = null;
        String codeLanguage = null;
        String listTag = null;

        for( String line : markdown.replace("\r", "").split("\n", -1) ) {
            String trimmed = line.trim();
            if (code != null) {
                if (trimmed.startsWith("```")) {
                    out.append(codeBlock(code.toString(), codeLanguage, palette));
                    code = null;
                } else {
                    code.append(line).append("\n");
                }
                continue;
            }
            if (trimmed.startsWith("```")) {
                flushParagraph(out, paragraph);
                listTag = closeList(out, listTag);
                code = new StringBuilder();
                codeLanguage = trimmed.substring(3).trim().toLowerCase(Locale.ROOT);
                continue;
            }
            if (trimmed.isEmpty()) {
                flushParagraph(out, paragraph);
                listTag = closeList(out, listTag);
                continue;
            }
            Matcher heading = HEADING.matcher(trimmed);
            Matcher bullet = BULLET.matcher(trimmed);
            Matcher numbered = NUMBERED.matcher(trimmed);
            if (heading.matches()) {
                flushParagraph(out, paragraph);
                listTag = closeList(out, listTag);
                // relative to the text size, the absolute html sizes come out smaller than the text
                String size = heading.group(1).length() <= 2 ? "+2" : "+1";
                out.append("<p><font size=\"").append(size).append("\"><b>").append(inline(heading.group(2), palette))
                        .append("</b></font></p>");
            } else if (bullet.matches() || numbered.matches()) {
                flushParagraph(out, paragraph);
                String tag = bullet.matches() ? "ul" : "ol";
                if (!tag.equals(listTag)) {
                    closeList(out, listTag);
                    out.append("<").append(tag);
                    if (numbered.matches()) {
                        // models leave blank lines or code between the items, which split the list:
                        // each part starts at the number written in the answer
                        out.append(" start=\"").append(numbered.group(1)).append("\"");
                    }
                    out.append(">");
                    listTag = tag;
                }
                String item = bullet.matches() ? bullet.group(1) : numbered.group(2);
                out.append("<li>").append(inline(item, palette)).append("</li>");
            } else {
                listTag = closeList(out, listTag);
                if (paragraph.length() > 0) {
                    paragraph.append("<br>");
                }
                paragraph.append(inline(trimmed, palette));
            }
        }
        // while an answer streams, a code block can still be open
        if (code != null) {
            out.append(codeBlock(code.toString(), codeLanguage, palette));
        }
        flushParagraph(out, paragraph);
        closeList(out, listTag);
        return out.toString();
    }

    private static void flushParagraph( StringBuilder out, StringBuilder paragraph ) {
        if (paragraph.length() > 0) {
            out.append("<p>").append(paragraph).append("</p>");
            paragraph.setLength(0);
        }
    }

    private static String closeList( StringBuilder out, String listTag ) {
        if (listTag != null) {
            out.append("</").append(listTag).append(">");
        }
        return null;
    }

    /**
     * @param code the code.
     * @param language the language of the fence, can be empty.
     * @param palette the colors.
     * @return the code block, SQL highlighted.
     */
    public static String codeBlock( String code, String language, Palette palette ) {
        String text = code.endsWith("\n") ? code.substring(0, code.length() - 1) : code;
        boolean sql = "sql".equals(language) || ((language == null || language.isEmpty()) && SQL_START.matcher(text).find());
        String body = sql ? highlightSql(text, palette) : escape(text);
        return "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"6\"><tr><td bgcolor=\"" + palette.codeBackground()
                + "\"><font face=\"Monospaced\">" + keepLines(body) + "</font></td></tr></table>";
    }

    /**
     * Keep the lines and the indentation of code without <code>pre</code>, whose lines never wrap
     * and would widen the whole pane: line breaks become <code>br</code>, leading and repeated
     * spaces non-breaking ones, single spaces stay places where long lines can wrap.
     */
    private static String keepLines( String html ) {
        StringBuilder sb = new StringBuilder();
        String[] lines = html.replace("\t", "    ").split("\n", -1);
        for( int i = 0; i < lines.length; i++ ) {
            String line = lines[i];
            int indent = 0;
            while( indent < line.length() && line.charAt(indent) == ' ' ) {
                indent++;
            }
            sb.append("&nbsp;".repeat(indent)).append(line.substring(indent).replace("  ", " &nbsp;"));
            if (i < lines.length - 1) {
                sb.append("<br>");
            }
        }
        return sb.toString();
    }

    private static String inline( String text, Palette palette ) {
        StringBuilder sb = new StringBuilder();
        String[] parts = text.split("`", -1);
        for( int i = 0; i < parts.length; i++ ) {
            boolean isCode = i % 2 == 1 && i < parts.length - 1;
            if (isCode) {
                sb.append("<code><font color=\"").append(palette.inlineCode()).append("\">").append(escape(parts[i]))
                        .append("</font></code>");
            } else {
                String part = escape(i % 2 == 1 ? "`" + parts[i] : parts[i]);
                part = LINK.matcher(part).replaceAll("<a href=\"$2\">$1</a>");
                part = BOLD.matcher(part).replaceAll("<b>$1</b>");
                part = ITALIC.matcher(part).replaceAll("<i>$1</i>");
                sb.append(part);
            }
        }
        return sb.toString();
    }

    /**
     * @param sql the SQL.
     * @param palette the colors.
     * @return the escaped SQL with keywords, strings, numbers and comments colored.
     */
    public static String highlightSql( String sql, Palette palette ) {
        Set<String> keywords = getSqlKeywords();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = sql.length();
        while( i < n ) {
            char c = sql.charAt(i);
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int end = sql.indexOf('\n', i);
                end = end == -1 ? n : end;
                sb.append(colored(sql.substring(i, end), palette.comment()));
                i = end;
            } else if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                end = end == -1 ? n : end + 2;
                sb.append(colored(sql.substring(i, end), palette.comment()));
                i = end;
            } else if (c == '\'') {
                int j = i + 1;
                while( j < n ) {
                    if (sql.charAt(j) == '\'') {
                        if (j + 1 < n && sql.charAt(j + 1) == '\'') {
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    j++;
                }
                int end = Math.min(j + 1, n);
                sb.append(colored(sql.substring(i, end), palette.string()));
                i = end;
            } else if (Character.isDigit(c)) {
                int j = i;
                while( j < n && (Character.isDigit(sql.charAt(j)) || sql.charAt(j) == '.') ) {
                    j++;
                }
                sb.append(colored(sql.substring(i, j), palette.number()));
                i = j;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i;
                while( j < n && (Character.isLetterOrDigit(sql.charAt(j)) || sql.charAt(j) == '_') ) {
                    j++;
                }
                String word = sql.substring(i, j);
                if (keywords.contains(word.toUpperCase(Locale.ROOT))) {
                    sb.append("<b>").append(colored(word, palette.keyword())).append("</b>");
                } else {
                    sb.append(escape(word));
                }
                i = j;
            } else {
                sb.append(escape(String.valueOf(c)));
                i++;
            }
        }
        return sb.toString();
    }

    private static String colored( String text, String color ) {
        return "<font color=\"" + color + "\">" + escape(text) + "</font>";
    }

    /**
     * @param text the text.
     * @return the text with the HTML special characters escaped.
     */
    public static String escape( String text ) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * @return the SQL keywords the SQL editor of the database viewer highlights too.
     */
    private static synchronized Set<String> getSqlKeywords() {
        if (sqlKeywords == null) {
            sqlKeywords = new HashSet<>();
            try (InputStream in = MarkdownHtml.class.getResourceAsStream("/hm_sql_keywords.txt")) {
                if (in != null) {
                    try (Scanner scanner = new Scanner(in, "UTF-8")) {
                        while( scanner.hasNextLine() ) {
                            String keyword = scanner.nextLine().trim().toUpperCase(Locale.ROOT);
                            if (!keyword.isEmpty()) {
                                sqlKeywords.add(keyword);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // no highlighting then
            }
        }
        return sqlKeywords;
    }
}
