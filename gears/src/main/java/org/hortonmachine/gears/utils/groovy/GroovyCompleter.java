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
package org.hortonmachine.gears.utils.groovy;

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

import org.hortonmachine.gears.utils.groovy.GroovyTypeResolver.ScriptImports;
import org.hortonmachine.gears.utils.groovy.GroovyTypeResolver.TypeRef;

import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Out;

/**
 * Code completion for groovy scripts, without user interface, used by the scripting console and the notebooks.
 *
 * <p>
 * After a dot it proposes the members of the receiver, whose type is inferred by the
 * {@link GroovyTypeResolver}: methods, properties, public fields (with the documentation of the
 * HortonMachine modules parameters) and the methods groovy adds to the jdk (GDK). Elsewhere it
 * proposes variables and class names, telling when a class still needs to be imported.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GroovyCompleter {

    public static final String[] KEYWORDS = {"abstract", "as", "assert", "boolean", "break", "byte", "case", "catch", "char",
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

    /** The kinds of the completions. */
    public enum Kind {
        METHOD, PROPERTY, FIELD, PARAMETER, GDK, CLASS, PACKAGE, VARIABLE, KEYWORD
    }

    /**
     * A parameter of a method.
     */
    public record Param( String type, String name ) {
    }

    /**
     * A completion.
     *
     * @param kind the kind.
     * @param name the text to insert.
     * @param type the type of a variable, field or property, the return type of a method, the package of a class.
     * @param definedIn where it comes from: the class of a member, "binding" or "script" for variables.
     * @param params the parameters of a method, <code>null</code> for the others.
     * @param description the documentation of a module parameter, or <code>null</code>.
     * @param role "input" or "output" for the parameters of the modules, else empty.
     * @param relevance higher first.
     * @param fqn the fully qualified name of a class, or <code>null</code>.
     * @param needsImport <code>true</code> for a class that the script doesn't import yet.
     */
    public record Item( Kind kind, String name, String type, String definedIn, List<Param> params, String description,
            String role, int relevance, String fqn, boolean needsImport ) {

        public boolean isMethod() {
            return params != null;
        }
    }

    /**
     * The completions at a position.
     *
     * @param from the start of the text already entered, which the completion replaces.
     * @param prefix the text already entered.
     * @param items the completions.
     * @param scoped <code>true</code> after a dot or in an import, where only members or packages fit: no
     *              keywords or templates should be added.
     */
    public record Result( int from, String prefix, List<Item> items, boolean scoped ) {
    }

    private final GroovyTypeResolver resolver;

    /**
     * @param resolver the type resolver.
     */
    public GroovyCompleter( GroovyTypeResolver resolver ) {
        this.resolver = resolver;
    }

    public GroovyTypeResolver getResolver() {
        return resolver;
    }

    /**
     * Find the completions at a position of a script.
     *
     * @param script the whole script.
     * @param caret the position.
     * @param withKeywords <code>true</code> to add the keywords where no member is completed.
     * @return the completions.
     */
    public Result complete( String script, int caret, boolean withKeywords ) {
        caret = Math.max(0, Math.min(caret, script.length()));
        String textBefore = script.substring(0, caret);
        String prefix = enteredText(textBefore);
        int from = caret - prefix.length();
        List<Item> items = new ArrayList<>();
        int dotIndex = from - 1;
        ScriptImports imports = resolver.parseImports(script);
        if (dotIndex >= 0 && textBefore.charAt(dotIndex) == '.') {
            String receiver = GroovyTypeResolver.extractReceiver(textBefore.substring(0, dotIndex));
            if (receiver != null) {
                String scriptBefore = textBefore.substring(0, Math.max(0, dotIndex - receiver.length()));
                TypeRef ref = resolver.resolveExpression(receiver, scriptBefore, imports);
                if (ref != null) {
                    addMemberCompletions(ref, prefix, items);
                }
            }
            return new Result(from, prefix, items, true);
        }
        String line = textBefore.substring(textBefore.lastIndexOf('\n') + 1);
        if (IMPORT_LINE.matcher(line).matches()) {
            for( String pkg : resolver.getIndex().getSubPackages("") ) {
                if (startsWithIgnoreCase(pkg, prefix)) {
                    items.add(packageItem(pkg));
                }
            }
            return new Result(from, prefix, items, true);
        }
        if (withKeywords && !prefix.isEmpty()) {
            for( String keyword : KEYWORDS ) {
                if (startsWithIgnoreCase(keyword, prefix) && !keyword.equals(prefix)) {
                    items.add(new Item(Kind.KEYWORD, keyword, "keyword", null, null, null, "", 15, null, false));
                }
            }
        }
        addVariableCompletions(prefix, textBefore, imports, items);
        if (!prefix.isEmpty() && Character.isUpperCase(prefix.charAt(0))) {
            addClassCompletions(prefix, imports, items);
        }
        return new Result(from, prefix, items, false);
    }

    /**
     * @return the text before the caret that is part of an identifier.
     */
    public static String enteredText( String textBefore ) {
        int start = textBefore.length();
        while( start > 0 ) {
            char c = textBefore.charAt(start - 1);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                break;
            }
            start--;
        }
        return textBefore.substring(start);
    }

    /**
     * @return the description of a class, as of the modules, or <code>null</code>.
     */
    public String classDescription( String fqn ) {
        Class< ? > clazz = resolver.loadClass(fqn);
        if (clazz != null) {
            Description description = clazz.getAnnotation(Description.class);
            if (description != null) {
                return description.value();
            }
        }
        return null;
    }

    private void addMemberCompletions( TypeRef ref, String prefix, List<Item> items ) {
        if (ref.isPackage()) {
            ClassIndex index = resolver.getIndex();
            for( String pkg : index.getSubPackages(ref.packageName) ) {
                if (startsWithIgnoreCase(pkg, prefix)) {
                    items.add(packageItem(pkg));
                }
            }
            for( String simpleName : index.getClassesInPackage(ref.packageName) ) {
                if (startsWithIgnoreCase(simpleName, prefix)) {
                    items.add(classItem(simpleName, ref.packageName + "." + simpleName, false, 0));
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
            items.add(fieldItem(field, type));
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
                    items.add(new Item(Kind.PROPERTY, property, simpleName(method.getReturnType()),
                            simpleName(method.getDeclaringClass()), null, null, "", method.getDeclaringClass() == type ? 60 : 40,
                            null, false));
                }
            }
            if (startsWithIgnoreCase(name, prefix)) {
                int relevance = method.getDeclaringClass() == type ? 50 : method.getDeclaringClass() == Object.class ? 5 : 30;
                items.add(methodItem(Kind.METHOD, name, method.getReturnType(), method.getParameters(), 0,
                        simpleName(method.getDeclaringClass()), relevance));
            }
        }
        if (!ref.isStatic) {
            for( Method method : resolver.getGdkMethods(type) ) {
                String name = method.getName();
                if (HIDDEN_MEMBERS.contains(name) || !startsWithIgnoreCase(name, prefix)) {
                    continue;
                }
                int relevance = method.getParameterTypes()[0] == Object.class ? 3 : 10;
                items.add(methodItem(Kind.GDK, name, method.getReturnType(), method.getParameters(), 1, "GDK", relevance));
            }
        }
    }

    private Item fieldItem( Field field, Class< ? > type ) {
        boolean isIn = field.isAnnotationPresent(In.class);
        boolean isOut = field.isAnnotationPresent(Out.class);
        Description description = field.getAnnotation(Description.class);
        String role = isIn ? "input" : isOut ? "output" : "";
        // the parameters of the module itself before the inherited ones (pm, doProcess...)
        int relevance = isIn ? 100 : isOut ? 90 : 45;
        if (field.getDeclaringClass() != type && (isIn || isOut)) {
            relevance -= 40;
        }
        return new Item(isIn || isOut ? Kind.PARAMETER : Kind.FIELD, field.getName(), simpleName(field.getType()),
                simpleName(field.getDeclaringClass()), null, description != null ? description.value() : null, role,
                relevance, null, false);
    }

    private static Item methodItem( Kind kind, String name, Class< ? > returnType, Parameter[] parameters, int skip,
            String definedIn, int relevance ) {
        List<Param> params = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        for( int i = skip; i < parameters.length; i++ ) {
            Parameter p = parameters[i];
            String paramName = p.isNamePresent() ? p.getName() : guessParameterName(p.getType(), i - skip + 1);
            if (!usedNames.add(paramName)) {
                paramName = paramName + (i - skip + 1);
            }
            params.add(new Param(simpleName(p.getType()), paramName));
        }
        return new Item(kind, name, simpleName(returnType), definedIn, params, null, "", relevance, null, false);
    }

    private void addVariableCompletions( String prefix, String textBefore, ScriptImports imports, List<Item> items ) {
        Set<String> added = new HashSet<>();
        for( Map.Entry<String, Object> entry : resolver.getVariables().entrySet() ) {
            String name = entry.getKey();
            if (name.startsWith("_") || !startsWithIgnoreCase(name, prefix)) {
                continue;
            }
            Object value = entry.getValue();
            items.add(new Item(Kind.VARIABLE, name, value != null ? simpleName(value.getClass()) : "null", "binding", null, null,
                    "", 80, null, false));
            added.add(name);
        }
        for( String name : resolver.findDeclaredVariables(textBefore) ) {
            if (added.contains(name) || !startsWithIgnoreCase(name, prefix) || name.equals(prefix)) {
                continue;
            }
            Class< ? > type = resolver.inferVariableType(name, textBefore, imports);
            items.add(new Item(Kind.VARIABLE, name, type != null ? simpleName(type) : "def", "script", null, null, "", 85, null,
                    false));
        }
    }

    private void addClassCompletions( String prefix, ScriptImports imports, List<Item> items ) {
        List<Item> classes = new ArrayList<>();
        for( Map.Entry<String, Set<String>> entry : resolver.getIndex().getClassesStartingWith(prefix).entrySet() ) {
            String simpleName = entry.getKey();
            for( String fqn : entry.getValue() ) {
                boolean visible = resolver.isVisible(simpleName, fqn, imports);
                classes.add(classItem(simpleName, fqn, !visible, visible ? 70 : classRelevance(fqn)));
            }
        }
        classes.sort(( a, b ) -> Integer.compare(b.relevance(), a.relevance()));
        items.addAll(classes.subList(0, Math.min(MAX_CLASS_COMPLETIONS, classes.size())));
    }

    private static Item classItem( String simpleName, String fqn, boolean needsImport, int relevance ) {
        return new Item(Kind.CLASS, simpleName, fqn.substring(0, fqn.lastIndexOf('.')), null, null, null, "", relevance, fqn,
                needsImport);
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

    private static Item packageItem( String name ) {
        return new Item(Kind.PACKAGE, name, "package", null, null, null, "", 20, null, false);
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

    /**
     * @return the simple name of a type, with [] for the arrays.
     */
    public static String simpleName( Class< ? > type ) {
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
}
