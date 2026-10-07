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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.text.BadLocationException;
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
import org.hortonmachine.geoscript.console.GroovyTypeResolver.ScriptImports;
import org.hortonmachine.geoscript.console.GroovyTypeResolver.TypeRef;

import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Out;

/**
 * Code completion for groovy scripts.
 *
 * <p>
 * After a dot it proposes the members of the receiver, whose type is inferred by the
 * {@link GroovyTypeResolver}: methods, properties, public fields (with the documentation of the
 * HortonMachine modules parameters) and the methods groovy adds to the jdk (GDK). Elsewhere it
 * proposes keywords, templates, variables and class names, adding the import when a class is not
 * yet visible to the script.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GroovyCompletionProvider extends DefaultCompletionProvider {

    private static final String[] KEYWORDS = {"abstract", "as", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "continue", "def", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally",
            "float", "for", "if", "implements", "import", "in", "instanceof", "int", "interface", "long", "new", "null",
            "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "this", "throw",
            "throws", "true", "try", "var", "void", "while"};

    /** Members that only add noise in a script. */
    private static final Set<String> HIDDEN_MEMBERS = Set.of("getMetaClass", "setMetaClass", "invokeMethod", "getProperty",
            "setProperty", "wait", "notify", "notifyAll", "getClass", "metaClass", "class", "asType", "isCase", "getAt",
            "putAt", "leftShift", "rightShift", "rightShiftUnsigned", "plus", "minus", "multiply", "div", "power", "mod",
            "xor", "or", "and", "negative", "positive", "bitwiseNegate", "next", "previous", "respondsTo", "hasProperty",
            "getMetaPropertyValues", "mixin", "use", "print", "println", "printf", "sprintf", "sleep", "addShutdownHook",
            "identity", "is", "isAtLeast", "asBoolean", "asImmutable", "asSynchronized", "asUnmodifiable", "asChecked");

    private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*import\\s+(static\\s+)?[\\w.]*$");
    private static final int MAX_CLASS_COMPLETIONS = 200;

    private static final Icon METHOD_ICON = new LetterIcon('m', new Color(0x3b82f6));
    private static final Icon PROPERTY_ICON = new LetterIcon('p', new Color(0x8b5cf6));
    private static final Icon FIELD_ICON = new LetterIcon('f', new Color(0xd97706));
    private static final Icon PARAMETER_ICON = new LetterIcon('i', new Color(0x16a34a));
    private static final Icon CLASS_ICON = new LetterIcon('C', new Color(0x0d9488));
    private static final Icon PACKAGE_ICON = new LetterIcon('P', new Color(0x6b7280));
    private static final Icon VARIABLE_ICON = new LetterIcon('v', new Color(0xdb2777));
    private static final Icon GDK_ICON = new LetterIcon('g', new Color(0x64748b));

    private final GroovyTypeResolver resolver;

    /**
     * @param resolver the type resolver.
     */
    public GroovyCompletionProvider( GroovyTypeResolver resolver ) {
        this.resolver = resolver;
        setParameterizedCompletionParams('(', ", ", ')');
        setAutoActivationRules(true, ".");
        addKeywordsAndTemplates();
    }

    private void addKeywordsAndTemplates() {
        for( String keyword : KEYWORDS ) {
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
        try {
            int caret = comp.getCaretPosition();
            String textBefore = comp.getDocument().getText(0, caret);
            if (isInStringOrComment(comp, caret)) {
                return completions;
            }
            String prefix = getAlreadyEnteredText(comp);
            int dotIndex = caret - prefix.length() - 1;
            ScriptImports imports = resolver.parseImports(comp.getText());
            if (dotIndex >= 0 && textBefore.charAt(dotIndex) == '.') {
                String receiver = GroovyTypeResolver.extractReceiver(textBefore.substring(0, dotIndex));
                if (receiver != null) {
                    String scriptBefore = textBefore.substring(0, Math.max(0, dotIndex - receiver.length()));
                    TypeRef ref = resolver.resolveExpression(receiver, scriptBefore, imports);
                    if (ref != null) {
                        addMemberCompletions(ref, prefix, completions);
                    }
                }
                return completions;
            }
            String line = textBefore.substring(textBefore.lastIndexOf('\n') + 1);
            if (IMPORT_LINE.matcher(line).matches()) {
                for( String pkg : resolver.getIndex().getSubPackages("") ) {
                    if (startsWithIgnoreCase(pkg, prefix)) {
                        completions.add(packageCompletion(pkg));
                    }
                }
                return completions;
            }
            completions.addAll(super.getCompletionsImpl(comp));
            addVariableCompletions(prefix, textBefore, imports, completions);
            if (!prefix.isEmpty() && Character.isUpperCase(prefix.charAt(0))) {
                addClassCompletions(prefix, imports, completions);
            }
        } catch (BadLocationException e) {
            // return what we have
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

    private void addMemberCompletions( TypeRef ref, String prefix, List<Completion> completions ) {
        if (ref.isPackage()) {
            ClassIndex index = resolver.getIndex();
            for( String pkg : index.getSubPackages(ref.packageName) ) {
                if (startsWithIgnoreCase(pkg, prefix)) {
                    completions.add(packageCompletion(pkg));
                }
            }
            for( String simpleName : index.getClassesInPackage(ref.packageName) ) {
                if (startsWithIgnoreCase(simpleName, prefix)) {
                    completions.add(new ClassCompletion(simpleName, ref.packageName + "." + simpleName, false));
                }
            }
            return;
        }

        Class< ? > type = ref.type;
        Set<String> properties = new HashSet<>();
        for( Field field : type.getFields() ) {
            if (Modifier.isStatic(field.getModifiers()) != ref.isStatic || !startsWithIgnoreCase(field.getName(), prefix)) {
                continue;
            }
            properties.add(field.getName());
            completions.add(fieldCompletion(field, type));
        }
        for( Method method : type.getMethods() ) {
            String name = method.getName();
            if (method.isSynthetic() || method.isBridge() || name.contains("$") || HIDDEN_MEMBERS.contains(name)
                    || Modifier.isStatic(method.getModifiers()) != ref.isStatic) {
                continue;
            }
            if (!ref.isStatic) {
                String property = getPropertyName(method);
                if (property != null && !properties.contains(property) && !HIDDEN_MEMBERS.contains(property)
                        && startsWithIgnoreCase(property, prefix)) {
                    properties.add(property);
                    VariableCompletion completion = new VariableCompletion(this, property,
                            simpleName(method.getReturnType()));
                    completion.setDefinedIn(simpleName(method.getDeclaringClass()));
                    completion.setIcon(PROPERTY_ICON);
                    completion.setRelevance(method.getDeclaringClass() == type ? 60 : 40);
                    completions.add(completion);
                }
            }
            if (startsWithIgnoreCase(name, prefix)) {
                int relevance = method.getDeclaringClass() == type ? 50 : method.getDeclaringClass() == Object.class ? 5 : 30;
                completions.add(methodCompletion(name, method.getReturnType(), method.getParameters(), 0,
                        simpleName(method.getDeclaringClass()), METHOD_ICON, relevance));
            }
        }
        if (!ref.isStatic) {
            for( Method method : resolver.getGdkMethods(type) ) {
                String name = method.getName();
                if (HIDDEN_MEMBERS.contains(name) || !startsWithIgnoreCase(name, prefix)) {
                    continue;
                }
                int relevance = method.getParameterTypes()[0] == Object.class ? 3 : 10;
                completions.add(methodCompletion(name, method.getReturnType(), method.getParameters(), 1, "GDK", GDK_ICON,
                        relevance));
            }
        }
    }

    private Completion fieldCompletion( Field field, Class< ? > type ) {
        VariableCompletion completion = new VariableCompletion(this, field.getName(), simpleName(field.getType()));
        completion.setDefinedIn(simpleName(field.getDeclaringClass()));
        boolean isIn = field.isAnnotationPresent(In.class);
        boolean isOut = field.isAnnotationPresent(Out.class);
        Description description = field.getAnnotation(Description.class);
        if (description != null) {
            String role = isIn ? "input" : isOut ? "output" : "";
            completion.setShortDescription(description.value());
            completion.setSummary("<b>" + field.getName() + "</b> (" + simpleName(field.getType()) + ")"
                    + (role.isEmpty() ? "" : " - module " + role) + "<br><br>" + escape(description.value()));
        }
        completion.setIcon(isIn || isOut ? PARAMETER_ICON : FIELD_ICON);
        // the parameters of the module itself before the inherited ones (pm, doProcess...)
        int relevance = isIn ? 100 : isOut ? 90 : 45;
        if (field.getDeclaringClass() != type && (isIn || isOut)) {
            relevance -= 40;
        }
        completion.setRelevance(relevance);
        return completion;
    }

    private FunctionCompletion methodCompletion( String name, Class< ? > returnType, Parameter[] parameters, int skip,
            String definedIn, Icon icon, int relevance ) {
        FunctionCompletion completion = new FunctionCompletion(this, name, simpleName(returnType));
        List<ParameterizedCompletion.Parameter> params = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        for( int i = skip; i < parameters.length; i++ ) {
            Parameter p = parameters[i];
            String paramName = p.isNamePresent() ? p.getName() : guessParameterName(p.getType(), i - skip + 1);
            if (!usedNames.add(paramName)) {
                paramName = paramName + (i - skip + 1);
            }
            params.add(new ParameterizedCompletion.Parameter(simpleName(p.getType()), paramName, i == parameters.length - 1));
        }
        completion.setParams(params);
        completion.setDefinedIn(definedIn);
        completion.setIcon(icon);
        completion.setRelevance(relevance);
        return completion;
    }

    private void addVariableCompletions( String prefix, String textBefore, ScriptImports imports,
            List<Completion> completions ) {
        Set<String> added = new HashSet<>();
        for( Map.Entry<String, Object> entry : resolver.getVariables().entrySet() ) {
            String name = entry.getKey();
            if (name.startsWith("_") || !startsWithIgnoreCase(name, prefix)) {
                continue;
            }
            Object value = entry.getValue();
            VariableCompletion completion = new VariableCompletion(this, name,
                    value != null ? simpleName(value.getClass()) : "null");
            completion.setDefinedIn("binding");
            completion.setIcon(VARIABLE_ICON);
            completion.setRelevance(80);
            completions.add(completion);
            added.add(name);
        }
        for( String name : resolver.findDeclaredVariables(textBefore) ) {
            if (added.contains(name) || !startsWithIgnoreCase(name, prefix) || name.equals(prefix)) {
                continue;
            }
            Class< ? > type = resolver.inferVariableType(name, textBefore, imports);
            VariableCompletion completion = new VariableCompletion(this, name, type != null ? simpleName(type) : "def");
            completion.setDefinedIn("script");
            completion.setIcon(VARIABLE_ICON);
            completion.setRelevance(85);
            completions.add(completion);
        }
    }

    private void addClassCompletions( String prefix, ScriptImports imports, List<Completion> completions ) {
        List<ClassCompletion> classes = new ArrayList<>();
        for( Map.Entry<String, Set<String>> entry : resolver.getIndex().getClassesStartingWith(prefix).entrySet() ) {
            String simpleName = entry.getKey();
            for( String fqn : entry.getValue() ) {
                boolean visible = resolver.isVisible(simpleName, fqn, imports);
                ClassCompletion completion = new ClassCompletion(simpleName, fqn, !visible);
                completion.setRelevance(visible ? 70 : classRelevance(fqn));
                classes.add(completion);
            }
        }
        classes.sort(( a, b ) -> Integer.compare(b.getRelevance(), a.getRelevance()));
        completions.addAll(classes.subList(0, Math.min(MAX_CLASS_COMPLETIONS, classes.size())));
    }

    private static int classRelevance( String fqn ) {
        if (fqn.contains(".internal.") || fqn.contains(".impl.") || fqn.contains(".shaded.")) {
            return 1;
        }
        if (fqn.startsWith("org.hortonmachine.") || fqn.startsWith("geoscript.")) {
            return 40;
        }
        if (fqn.startsWith("java.") || fqn.startsWith("groovy.")) {
            return 30;
        }
        if (fqn.startsWith("org.geotools.") || fqn.startsWith("org.locationtech.jts.")) {
            return 25;
        }
        return 10;
    }

    private Completion packageCompletion( String name ) {
        BasicCompletion completion = new BasicCompletion(this, name, "package");
        completion.setIcon(PACKAGE_ICON);
        completion.setRelevance(20);
        return completion;
    }

    private static String getPropertyName( Method method ) {
        if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
            return null;
        }
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    /**
     * Compiled classes usually lack parameter names: use the type name, unless it says nothing.
     */
    private static String guessParameterName( Class< ? > type, int position ) {
        Class< ? > boxed = GroovyTypeResolver.box(type);
        if (type.isPrimitive() || boxed == String.class || Number.class.isAssignableFrom(boxed) || boxed == Boolean.class
                || boxed == Object.class) {
            return "arg" + position;
        }
        return decapitalize(simpleName(type));
    }

    private static String simpleName( Class< ? > type ) {
        return type.isArray() ? simpleName(type.getComponentType()) + "[]" : type.getSimpleName();
    }

    private static String decapitalize( String name ) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.endsWith("[]")) {
            return decapitalize(name.substring(0, name.length() - 2)) + "s";
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static boolean startsWithIgnoreCase( String text, String prefix ) {
        return text.regionMatches(true, 0, prefix, 0, prefix.length());
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
                Class< ? > clazz = resolver.loadClass(fqn);
                if (clazz != null) {
                    Description description = clazz.getAnnotation(Description.class);
                    if (description != null) {
                        sb.append("<br><br>").append(escape(description.value()));
                    }
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
