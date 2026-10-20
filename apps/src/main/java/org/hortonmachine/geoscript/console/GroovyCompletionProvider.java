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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.autocomplete.TemplateCompletion;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.hortonmachine.gears.utils.groovy.GroovyCompleter;
import org.hortonmachine.gears.utils.groovy.GroovyCompleter.Item;
import org.hortonmachine.gears.utils.groovy.GroovyCompleter.Param;
import org.hortonmachine.gears.utils.groovy.GroovyTypeResolver;

/**
 * Code completion for groovy scripts in the console.
 *
 * <p>
 * The completions come from the {@link GroovyCompleter}, shared with the notebooks: after a dot the
 * members of the receiver, elsewhere variables and class names, adding the import when a class is not
 * yet visible to the script. This adds the keywords and templates and shows them with icons.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GroovyCompletionProvider extends DefaultCompletionProvider {

    private static final Icon METHOD_ICON = new LetterIcon('m', new Color(0x3b82f6));
    private static final Icon PROPERTY_ICON = new LetterIcon('p', new Color(0x8b5cf6));
    private static final Icon FIELD_ICON = new LetterIcon('f', new Color(0xd97706));
    private static final Icon PARAMETER_ICON = new LetterIcon('i', new Color(0x16a34a));
    private static final Icon CLASS_ICON = new LetterIcon('C', new Color(0x0d9488));
    private static final Icon PACKAGE_ICON = new LetterIcon('P', new Color(0x6b7280));
    private static final Icon VARIABLE_ICON = new LetterIcon('v', new Color(0xdb2777));
    private static final Icon GDK_ICON = new LetterIcon('g', new Color(0x64748b));

    private final GroovyCompleter completer;

    /**
     * @param resolver the type resolver.
     */
    public GroovyCompletionProvider( GroovyTypeResolver resolver ) {
        this.completer = new GroovyCompleter(resolver);
        setParameterizedCompletionParams('(', ", ", ')');
        setAutoActivationRules(true, ".");
        addKeywordsAndTemplates();
    }

    private void addKeywordsAndTemplates() {
        for( String keyword : GroovyCompleter.KEYWORDS ) {
            addCompletion(new BasicCompletion(this, keyword, "keyword"));
        }
        addCompletion(new BasicCompletion(this, "println", "print a line to the output"));
        addCompletion(new TemplateCompletion(this, "for", "for (item in list)", "for (${item} in ${list}) {\n    ${cursor}\n}",
                "loop over a collection", null));
        addCompletion(new TemplateCompletion(this, "fori", "for (int i...)",
                "for (int ${i} = 0; ${i} < ${size}; ${i}++) {\n    ${cursor}\n}", "counted loop", null));
        addCompletion(new TemplateCompletion(this, "if", "if (condition)", "if (${condition}) {\n    ${cursor}\n}",
                "conditional block", null));
        addCompletion(new TemplateCompletion(this, "ifelse", "if (condition) else",
                "if (${condition}) {\n    ${cursor}\n} else {\n    \n}", "conditional block with else", null));
        addCompletion(new TemplateCompletion(this, "try", "try catch",
                "try {\n    ${cursor}\n} catch (Exception e) {\n    e.printStackTrace()\n}", "exception handling", null));
        addCompletion(new TemplateCompletion(this, "closure", "{ it -> }", "{ ${it} ->\n    ${cursor}\n}", "a closure", null));
    }

    @Override
    protected List<Completion> getCompletionsImpl( JTextComponent comp ) {
        List<Completion> completions = new ArrayList<>();
        int caret = comp.getCaretPosition();
        if (isInStringOrComment(comp, caret)) {
            return completions;
        }
        GroovyCompleter.Result result = completer.complete(comp.getText(), caret, false);
        if (!result.scoped()) {
            completions.addAll(super.getCompletionsImpl(comp));
        }
        for( Item item : result.items() ) {
            completions.add(toCompletion(item));
        }
        return completions;
    }

    private boolean isInStringOrComment( JTextComponent comp, int caret ) {
        if (caret == 0 || !(comp instanceof RSyntaxTextArea)) {
            return false;
        }
        Token token = ((RSyntaxTextArea) comp).modelToToken(caret - 1);
        if (token == null) {
            return false;
        }
        int type = token.getType();
        return token.isComment() || type == Token.LITERAL_STRING_DOUBLE_QUOTE || type == Token.LITERAL_CHAR
                || type == Token.LITERAL_BACKQUOTE;
    }

    private Completion toCompletion( Item item ) {
        switch( item.kind() ) {
        case PACKAGE: {
            BasicCompletion completion = new BasicCompletion(this, item.name(), "package");
            completion.setIcon(PACKAGE_ICON);
            completion.setRelevance(item.relevance());
            return completion;
        }
        case CLASS: {
            ClassCompletion completion = new ClassCompletion(item.name(), item.fqn(), item.needsImport());
            completion.setRelevance(item.relevance());
            return completion;
        }
        case METHOD:
        case GDK:
            return methodCompletion(item, item.kind() == GroovyCompleter.Kind.GDK ? GDK_ICON : METHOD_ICON);
        default: {
            VariableCompletion completion = new VariableCompletion(this, item.name(), item.type());
            completion.setDefinedIn(item.definedIn());
            if (item.description() != null) {
                completion.setShortDescription(item.description());
                completion.setSummary("<b>" + item.name() + "</b> (" + item.type() + ")"
                        + (item.role().isEmpty() ? "" : " - module " + item.role()) + "<br><br>" + escape(item.description()));
            }
            switch( item.kind() ) {
            case PROPERTY:
                completion.setIcon(PROPERTY_ICON);
                break;
            case PARAMETER:
                completion.setIcon(PARAMETER_ICON);
                break;
            case FIELD:
                completion.setIcon(FIELD_ICON);
                break;
            default:
                completion.setIcon(VARIABLE_ICON);
            }
            completion.setRelevance(item.relevance());
            return completion;
        }
        }
    }

    private FunctionCompletion methodCompletion( Item item, Icon icon ) {
        FunctionCompletion completion = new FunctionCompletion(this, item.name(), item.type());
        List<ParameterizedCompletion.Parameter> params = new ArrayList<>();
        List<Param> itemParams = item.params();
        for( int i = 0; i < itemParams.size(); i++ ) {
            Param param = itemParams.get(i);
            params.add(new ParameterizedCompletion.Parameter(param.type(), param.name(), i == itemParams.size() - 1));
        }
        completion.setParams(params);
        completion.setDefinedIn(item.definedIn());
        completion.setIcon(icon);
        completion.setRelevance(item.relevance());
        return completion;
    }

    private static String escape( String text ) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * A class name, that knows whether an import is needed once inserted.
     */
    public class ClassCompletion extends BasicCompletion {
        private final String fqn;
        private final boolean needsImport;
        private String summary;

        ClassCompletion( String simpleName, String fqn, boolean needsImport ) {
            super(GroovyCompletionProvider.this, simpleName, fqn.substring(0, fqn.lastIndexOf('.')));
            this.fqn = fqn;
            this.needsImport = needsImport;
            setIcon(CLASS_ICON);
        }

        public String getFqn() {
            return fqn;
        }

        public boolean needsImport() {
            return needsImport;
        }

        @Override
        public String getSummary() {
            if (summary == null) {
                StringBuilder sb = new StringBuilder("<b>").append(fqn).append("</b>");
                String description = completer.classDescription(fqn);
                if (description != null) {
                    sb.append("<br><br>").append(escape(description));
                }
                if (needsImport) {
                    sb.append("<br><br><i>The import will be added to the script.</i>");
                }
                summary = sb.toString();
            }
            return summary;
        }

        @Override
        public String toString() {
            return getReplacementText() + " - " + getShortDescription();
        }
    }

    /**
     * A small round icon with a letter, to tell the kinds of completions apart.
     */
    private static class LetterIcon implements Icon {
        private static final int SIZE = 14;
        private final char letter;
        private final Color color;

        LetterIcon( char letter, Color color ) {
            this.letter = letter;
            this.color = color;
        }

        @Override
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.fillOval(x, y, SIZE, SIZE);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            String text = String.valueOf(letter);
            int width = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, x + (SIZE - width) / 2f, y + SIZE - 3.5f);
            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
