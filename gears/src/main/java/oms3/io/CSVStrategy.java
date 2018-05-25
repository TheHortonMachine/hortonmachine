/*
 * $Id:$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
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
