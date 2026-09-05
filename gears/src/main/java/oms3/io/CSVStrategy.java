/*
 * $Id: CSVStrategy.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.io;

import java.io.Serializable;

/**
 * CSVStrategy
 * 
 * Represents the strategy for a CSV.
 */
class CSVStrategy implements Cloneable, Serializable {

    private char delimiter;
    private char encapsulator;
    private char commentStart;
    private char escape;
    private boolean ignoreLeadingWhitespaces;
    private boolean ignoreTrailingWhitespaces;
    private boolean interpretUnicodeEscapes;
    private boolean ignoreEmptyLines;

    // -2 is used to signal disabled, because it won't be confused with
    // an EOF signal (-1), and because \ufffe in UTF-16 would be
    // encoded as two chars (using surrogates) and thus there should never
    // be a collision with a real text char.
    public static char COMMENTS_DISABLED = (char) -2;
    public static char ESCAPE_DISABLED = (char) -2;
    public static CSVStrategy DEFAULT_STRATEGY = new CSVStrategy(',', '"', COMMENTS_DISABLED, ESCAPE_DISABLED, true,
            true, false, true);
    public static CSVStrategy EXCEL_STRATEGY = new CSVStrategy(',', '"', COMMENTS_DISABLED, ESCAPE_DISABLED, false,
            false, false, false);
    public static CSVStrategy TDF_STRATEGY = new CSVStrategy('\t', '"', COMMENTS_DISABLED, ESCAPE_DISABLED, true,
            true, false, true);

    public static CSVStrategy OMS_STRATEGY = new CSVStrategy(',', '"', COMMENTS_DISABLED, ESCAPE_DISABLED, true,
            true, false, true);

    /**
     * Customized CSV strategy setter.
     * 
     * @param delimiter a Char used for value separation
     * @param encapsulator a Char used as value encapsulation marker
     * @param commentStart a Char used for comment identification
     * @param ignoreLeadingWhitespace TRUE when leading whitespaces should be
     *                                ignored
     * @param interpretUnicodeEscapes TRUE when unicode escapes should be 
     *                                interpreted
     * @param ignoreEmptyLines TRUE when the parser should skip emtpy lines
     */
    CSVStrategy(
            char delimiter,
            char encapsulator,
            char commentStart,
            char escape,
            boolean ignoreLeadingWhitespace,
            boolean ignoreTrailingWhitespace,
            boolean interpretUnicodeEscapes,
            boolean ignoreEmptyLines) {
        setDelimiter(delimiter);
        setEncapsulator(encapsulator);
        setCommentStart(commentStart);
        setEscape(escape);
        setIgnoreLeadingWhitespaces(ignoreLeadingWhitespace);
        setIgnoreTrailingWhitespaces(ignoreTrailingWhitespace);
        setUnicodeEscapeInterpretation(interpretUnicodeEscapes);
        setIgnoreEmptyLines(ignoreEmptyLines);
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    public char getDelimiter() {
        return this.delimiter;
    }

    public void setEncapsulator(char encapsulator) {
        this.encapsulator = encapsulator;
    }

    public char getEncapsulator() {
        return this.encapsulator;
    }

    public void setCommentStart(char commentStart) {
        this.commentStart = commentStart;
    }

    public char getCommentStart() {
        return this.commentStart;
    }

    public boolean isCommentingDisabled() {
        return this.commentStart == COMMENTS_DISABLED;
    }

    public void setEscape(char escape) {
        this.escape = escape;
    }

    public char getEscape() {
        return this.escape;
    }

    public void setIgnoreLeadingWhitespaces(boolean ignoreLeadingWhitespaces) {
        this.ignoreLeadingWhitespaces = ignoreLeadingWhitespaces;
    }

    public boolean getIgnoreLeadingWhitespaces() {
        return this.ignoreLeadingWhitespaces;
    }

    public void setIgnoreTrailingWhitespaces(boolean ignoreTrailingWhitespaces) {
        this.ignoreTrailingWhitespaces = ignoreTrailingWhitespaces;
    }

    public boolean getIgnoreTrailingWhitespaces() {
        return this.ignoreTrailingWhitespaces;
    }

    public void setUnicodeEscapeInterpretation(boolean interpretUnicodeEscapes) {
        this.interpretUnicodeEscapes = interpretUnicodeEscapes;
    }

    public boolean getUnicodeEscapeInterpretation() {
        return this.interpretUnicodeEscapes;
    }

    public void setIgnoreEmptyLines(boolean ignoreEmptyLines) {
        this.ignoreEmptyLines = ignoreEmptyLines;
    }

    public boolean getIgnoreEmptyLines() {
        return this.ignoreEmptyLines;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);  // impossible
        }
    }
}
