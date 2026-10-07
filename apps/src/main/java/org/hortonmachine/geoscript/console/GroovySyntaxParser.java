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
package org.hortonmachine.geoscript.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;

import groovy.lang.GroovyClassLoader;

/**
 * Checks the groovy syntax while typing and marks the errors in the editor.
 *
 * <p>
 * Only the syntax is checked (compilation up to the conversion phase): unknown classes or
 * variables are not errors here, since a script can get them from the binding.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GroovySyntaxParser extends AbstractParser {

    private static final Pattern UNEXPECTED_INPUT = Pattern.compile("Unexpected input: '(.*)'", Pattern.DOTALL);

    private final GroovyClassLoader loader = new GroovyClassLoader();
    private final CompilerConfiguration config = new CompilerConfiguration();

    /**
     * Parse a dummy script, since the first parse warms up the groovy parser and takes a while.
     */
    public void warmUp() {
        Thread thread = new Thread(() -> check("def a = 1"), "hm-console-parser-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public ParseResult parse( RSyntaxDocument doc, String style ) {
        DefaultParseResult result = new DefaultParseResult(this);
        Element root = doc.getDefaultRootElement();
        result.setParsedLines(0, root.getElementCount() - 1);
        long start = System.currentTimeMillis();
        try {
            String text = doc.getText(0, doc.getLength());
            SyntaxException error = check(text);
            if (error != null) {
                int line = Math.max(0, Math.min(error.getStartLine() - 1, root.getElementCount() - 1));
                Element lineElement = root.getElement(line);
                int lineStart = lineElement.getStartOffset();
                int lineEnd = lineElement.getEndOffset() - 1;
                int offset = Math.min(lineStart + Math.max(0, error.getStartColumn() - 1), lineEnd);
                int length = 1;
                if (error.getEndLine() == error.getStartLine() && error.getEndColumn() > error.getStartColumn()) {
                    length = error.getEndColumn() - error.getStartColumn();
                }
                length = Math.max(1, Math.min(length, lineEnd - offset));

                // the parser reports unexpected input where it ends: mark where it starts instead
                Matcher m = UNEXPECTED_INPUT.matcher(error.getOriginalMessage());
                if (m.matches()) {
                    String input = m.group(1).replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
                    int inputStart = input.equals("<EOF>") ? lastNonBlank(text, offset) : text.lastIndexOf(input, offset);
                    if (inputStart >= 0) {
                        line = root.getElementIndex(inputStart);
                        int inputLineEnd = root.getElement(line).getEndOffset() - 1;
                        offset = inputStart;
                        length = Math.max(1, Math.min(inputLineEnd, inputStart + input.length()) - inputStart);
                    }
                }
                result.addNotice(new DefaultParserNotice(this, error.getOriginalMessage(), line, offset, length));
            }
        } catch (BadLocationException e) {
            result.setError(e);
        }
        result.setParseTime(System.currentTimeMillis() - start);
        return result;
    }

    private static int lastNonBlank( String text, int before ) {
        for( int i = Math.min(before, text.length()) - 1; i >= 0; i-- ) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check the syntax of a script.
     *
     * @param script the script.
     * @return the first syntax error, or <code>null</code>.
     */
    public SyntaxException check( String script ) {
        CompilationUnit unit = new CompilationUnit(config, null, loader);
        unit.addSource("Script.groovy", script);
        try {
            unit.compile(Phases.CONVERSION);
        } catch (MultipleCompilationErrorsException e) {
            for( Message message : e.getErrorCollector().getErrors() ) {
                if (message instanceof SyntaxErrorMessage) {
                    return ((SyntaxErrorMessage) message).getCause();
                }
            }
        } catch (Exception e) {
            // anything else is not a syntax problem
        }
        return null;
    }
}
