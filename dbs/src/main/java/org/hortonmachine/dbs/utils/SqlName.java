package org.hortonmachine.dbs.utils;

/**
 * A SQL identifier that can optionally be schema-qualified.
 *
 * <p>The legacy API exposed multiple pre-rendered variants. Those fields are
 * still kept for compatibility, but the underlying representation is now a
 * single parsed identifier that renders each part safely and consistently.</p>
 */
public class SqlName {

    public final String name;

    /**
     * Legacy compatibility field. Historically this used single quotes, which
     * are not valid SQL identifier quotes. It now matches {@link #fixedDoubleName}.
     */
    public final String fixedName;

    /**
     * Identifier rendered with ANSI double quotes.
     */
    public final String fixedDoubleName;

    /**
     * Legacy compatibility field for bracket-quoted identifiers.
     */
    public final String bracketName;

    public final String schema;

    /**
     * Make a new instance of sqlname.
     *
     * <p>If the input contains exactly one unquoted dot, it is treated as
     * {@code schema.name}. Otherwise the whole input is treated as the name.</p>
     *
     * @param name the name to use.
     * @return the instance of SqlName.
     */
    public static SqlName m( String name ) {
        QualifiedName qualifiedName = splitQualifiedName(name);
        return new SqlName(qualifiedName.schema, qualifiedName.name);
    }

    public static SqlName of( String name ) {
        return m(name);
    }

    public static SqlName qualified( String schema, String name ) {
        return new SqlName(schema, name);
    }

    protected SqlName( String schema, String name ) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Sql name can't be null or empty.");
        }
        this.schema = normalize(schema);
        this.name = name;

        String quotedFullName = renderQualified(this.schema, this.name, '"', '"');
        this.fixedName = quotedFullName;
        this.fixedDoubleName = quotedFullName;
        this.bracketName = renderQualified(this.schema, this.name, '[', ']');
    }

    public String getFullname() {
        if (schema != null) {
            return schema + "." + name;
        }
        return name;
    }

    @Override
    public String toString() {
        return getFullname();
    }

    public String nameForIndex() {
        return getFullname().replaceAll("[^A-Za-z0-9_]+", "_");
    }

    private static String normalize( String schema ) {
        if (schema == null || schema.isBlank()) {
            return null;
        }
        return schema;
    }

    private static QualifiedName splitQualifiedName( String rawName ) {
        if (rawName == null || rawName.isEmpty()) {
            throw new IllegalArgumentException("Sql name can't be null or empty.");
        }
        String trimmed = rawName.trim();
        int firstDot = findUnquotedDot(trimmed, 0);
        if (firstDot == -1 || findUnquotedDot(trimmed, firstDot + 1) != -1) {
            return new QualifiedName(null, unquotePart(trimmed));
        }
        return new QualifiedName(unquotePart(trimmed.substring(0, firstDot)), unquotePart(trimmed.substring(firstDot + 1)));
    }

    private static int findUnquotedDot( String value, int start ) {
        boolean inDoubleQuotes = false;
        boolean inBrackets = false;
        for( int i = start; i < value.length(); i++ ) {
            char c = value.charAt(i);
            if (c == '"' && !inBrackets) {
                if (inDoubleQuotes && i + 1 < value.length() && value.charAt(i + 1) == '"') {
                    i++;
                    continue;
                }
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '[' && !inDoubleQuotes) {
                inBrackets = true;
            } else if (c == ']' && inBrackets) {
                if (i + 1 < value.length() && value.charAt(i + 1) == ']') {
                    i++;
                    continue;
                }
                inBrackets = false;
            } else if (c == '.' && !inDoubleQuotes && !inBrackets) {
                return i;
            }
        }
        return -1;
    }

    private static String unquotePart( String value ) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '[' && trimmed.charAt(trimmed.length() - 1) == ']') {
            return trimmed.substring(1, trimmed.length() - 1).replace("]]", "]");
        }
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '\'' && trimmed.charAt(trimmed.length() - 1) == '\'') {
            return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
        }
        return trimmed;
    }

    private static String renderQualified( String schema, String name, char openQuote, char closeQuote ) {
        if (schema == null) {
            return quotePart(name, openQuote, closeQuote);
        }
        return quotePart(schema, openQuote, closeQuote) + "." + quotePart(name, openQuote, closeQuote);
    }

    private static String quotePart( String part, char openQuote, char closeQuote ) {
        if (!shouldQuote(part)) {
            return part;
        }
        if (openQuote == '"' && closeQuote == '"') {
            return "\"" + part.replace("\"", "\"\"") + "\"";
        }
        if (openQuote == '[' && closeQuote == ']') {
            return "[" + part.replace("]", "]]") + "]";
        }
        return part;
    }

    private static boolean shouldQuote( String part ) {
        if (part == null || part.isEmpty()) {
            return true;
        }
        if (DbsUtilities.isReservedName(part)) {
            return true;
        }
        if (!part.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return true;
        }

        for( char c : part.toCharArray() ) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    private static class QualifiedName {
        private final String schema;
        private final String name;

        private QualifiedName( String schema, String name ) {
            this.schema = schema;
            this.name = name;
        }
    }
}
