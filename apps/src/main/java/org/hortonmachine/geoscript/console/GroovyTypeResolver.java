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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Infers, without running it, the type of an expression in a groovy script.
 *
 * <p>
 * This is not a groovy type checker: it follows the simple cases that a script console needs, i.e.
 * chains of property accesses and method calls that start from:
 * <ul>
 * <li>a variable of the binding (the type of its current value, once the script has run);</li>
 * <li>a variable declared in the script (typed, assigned with <code>new</code>, with an
 * <code>as</code> coercion or with another chain);</li>
 * <li>a class name, resolved through the imports of the script;</li>
 * <li>a literal (string, list, map, number).</li>
 * </ul>
 * Whatever is not understood resolves to <code>null</code>, i.e. no completion.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GroovyTypeResolver {

    /** Packages that groovy imports by default. */
    public static final String[] DEFAULT_IMPORT_PACKAGES = {"java.lang", "java.util", "java.io", "java.net", "groovy.lang",
            "groovy.util"};

    /** Classes holding the methods that groovy adds to the jdk ones (GDK). */
    private static final String[] GDK_CLASSES = {"org.codehaus.groovy.runtime.DefaultGroovyMethods",
            "org.codehaus.groovy.runtime.StringGroovyMethods", "org.codehaus.groovy.runtime.ResourceGroovyMethods",
            "org.codehaus.groovy.runtime.IOGroovyMethods", "org.apache.groovy.nio.extensions.NioExtensions"};

    private static final int MAX_DEPTH = 5;

    private static final Pattern IMPORT_PATTERN = Pattern
            .compile("^\\s*import\\s+(static\\s+)?([\\w.]+?)(\\.\\*)?(?:\\s+as\\s+(\\w+))?\\s*;?\\s*$", Pattern.MULTILINE);
    private static final Pattern AS_PATTERN = Pattern.compile("^(.+)\\s+as\\s+([\\w.]+)$", Pattern.DOTALL);
    private static final Pattern LOCAL_NAMES_PATTERN = Pattern.compile(
            "(?:\\b(?:def|var|final)\\s+|\\b(?:[a-z_][\\w]*\\.)*[A-Z][\\w]*(?:<[^\\n=;()]*?>)?(?:\\[\\])?\\s+)([a-zA-Z_][\\w]*)\\s*(?:=(?!=)|;|$|\\s+in\\b|\\s*->)",
            Pattern.MULTILINE);
    private static final Set<String> NOT_VARIABLES = Set.of("def", "var", "final", "in", "as", "return", "new", "class",
            "import", "package");

    /** The type of an expression: a class (static access), an instance or a package. */
    public static class TypeRef {
        public final Class< ? > type;
        public final boolean isStatic;
        public final String packageName;

        private TypeRef( Class< ? > type, boolean isStatic, String packageName ) {
            this.type = type;
            this.isStatic = isStatic;
            this.packageName = packageName;
        }

        static TypeRef instance( Class< ? > type ) {
            return type == null || type == void.class ? null : new TypeRef(box(type), false, null);
        }
        static TypeRef staticOf( Class< ? > type ) {
            return type == null ? null : new TypeRef(type, true, null);
        }
        static TypeRef pkg( String packageName ) {
            return new TypeRef(null, false, packageName);
        }
        public boolean isPackage() {
            return packageName != null;
        }
    }

    /** The imports of a script. */
    public static class ScriptImports {
        /** simple name (or alias) -> fully qualified name. */
        public final Map<String, String> explicit = new LinkedHashMap<>();
        /** packages imported with a star. */
        public final List<String> starPackages = new ArrayList<>();
    }

    private enum Kind {
        NEW, IDENT, STRING, LIST, MAP, INTEGER, DECIMAL, INDEX
    }

    private static class Segment {
        final Kind kind;
        final String name;
        final boolean call;
        final int argCount;

        Segment( Kind kind, String name, boolean call, int argCount ) {
            this.kind = kind;
            this.name = name;
            this.call = call;
            this.argCount = argCount;
        }
    }

    private final ClassIndex index;
    private final Supplier<ClassLoader> loaderSupplier;
    private final Supplier<Map<String, Object>> variablesSupplier;
    private final Map<String, Optional<Class< ? >>> classCache = new ConcurrentHashMap<>();
    private List<Method> gdkMethods;

    /**
     * @param index the class names index.
     * @param loaderSupplier supplies the classloader of the script shell.
     * @param variablesSupplier supplies the variables of the script binding.
     */
    public GroovyTypeResolver( ClassIndex index, Supplier<ClassLoader> loaderSupplier,
            Supplier<Map<String, Object>> variablesSupplier ) {
        this.index = index;
        this.loaderSupplier = loaderSupplier;
        this.variablesSupplier = variablesSupplier;
    }

    public ClassIndex getIndex() {
        return index;
    }

    /**
     * @return a snapshot of the variables of the binding.
     */
    public Map<String, Object> getVariables() {
        try {
            return new LinkedHashMap<>(variablesSupplier.get());
        } catch (Exception e) {
            // the binding is being modified by a running script
            return Collections.emptyMap();
        }
    }

    /**
     * @param script the script.
     * @return its imports.
     */
    public ScriptImports parseImports( String script ) {
        ScriptImports imports = new ScriptImports();
        Matcher m = IMPORT_PATTERN.matcher(script);
        while( m.find() ) {
            if (m.group(1) != null) {
                continue; // static imports are not used for type resolution
            }
            String name = m.group(2);
            if (m.group(3) != null) {
                imports.starPackages.add(name);
            } else {
                String alias = m.group(4);
                String simple = alias != null ? alias : name.substring(name.lastIndexOf('.') + 1);
                imports.explicit.put(simple, name);
            }
        }
        return imports;
    }

    /**
     * Load a class without initializing it.
     *
     * @param fqn the fully qualified name.
     * @return the class or <code>null</code>.
     */
    public Class< ? > loadClass( String fqn ) {
        return classCache.computeIfAbsent(fqn, k -> {
            try {
                return Optional.of(Class.forName(k, false, loaderSupplier.get()));
            } catch (Throwable e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * Resolve a class name as the script would.
     *
     * @param name a simple or fully qualified class name.
     * @param imports the script imports.
     * @return the class or <code>null</code>.
     */
    public Class< ? > resolveClassName( String name, ScriptImports imports ) {
        if (name.contains(".")) {
            return loadClass(name);
        }
        String explicit = imports.explicit.get(name);
        if (explicit != null) {
            return loadClass(explicit);
        }
        List<String> packages = new ArrayList<>(imports.starPackages);
        Collections.addAll(packages, DEFAULT_IMPORT_PACKAGES);
        for( String pkg : packages ) {
            if (index.isReady() && !index.getClassesInPackage(pkg).contains(name)) {
                continue;
            }
            Class< ? > c = loadClass(pkg + "." + name);
            if (c != null) {
                return c;
            }
        }
        if (name.equals("BigDecimal")) {
            return BigDecimal.class;
        }
        if (name.equals("BigInteger")) {
            return BigInteger.class;
        }
        return null;
    }

    /**
     * @param simpleName a simple class name.
     * @param fqn the fully qualified name it should resolve to.
     * @param imports the script imports.
     * @return <code>true</code> if the simple name already resolves to the class.
     */
    public boolean isVisible( String simpleName, String fqn, ScriptImports imports ) {
        String explicit = imports.explicit.get(simpleName);
        if (explicit != null) {
            return explicit.equals(fqn);
        }
        String pkg = fqn.substring(0, fqn.lastIndexOf('.'));
        if (imports.starPackages.contains(pkg)) {
            return true;
        }
        for( String defaultPkg : DEFAULT_IMPORT_PACKAGES ) {
            if (defaultPkg.equals(pkg)) {
                return true;
            }
        }
        return fqn.equals("java.math.BigDecimal") || fqn.equals("java.math.BigInteger");
    }

    /**
     * Resolve the type of an expression.
     *
     * @param expression the expression (ex. <code>Format.getFormat(dtm).read()</code>).
     * @param script the script text that precedes the expression, used for declarations.
     * @param imports the script imports.
     * @return the type or <code>null</code>.
     */
    public TypeRef resolveExpression( String expression, String script, ScriptImports imports ) {
        return resolveExpression(expression, script, imports, 0);
    }

    private TypeRef resolveExpression( String expression, String script, ScriptImports imports, int depth ) {
        if (depth > MAX_DEPTH || expression == null || expression.isBlank()) {
            return null;
        }
        List<Segment> segments = parseChain(expression);
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        TypeRef current = null;
        for( int i = 0; i < segments.size(); i++ ) {
            Segment segment = segments.get(i);
            if (i == 0) {
                current = resolveFirst(segment, script, imports, depth);
            } else {
                current = resolveNext(current, segment);
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private TypeRef resolveFirst( Segment segment, String script, ScriptImports imports, int depth ) {
        switch( segment.kind ) {
        case NEW:
            return TypeRef.instance(resolveClassName(segment.name, imports));
        case STRING:
            return TypeRef.instance(String.class);
        case LIST:
            return TypeRef.instance(ArrayList.class);
        case MAP:
            return TypeRef.instance(LinkedHashMap.class);
        case INTEGER:
            return TypeRef.instance(Integer.class);
        case DECIMAL:
            return TypeRef.instance(BigDecimal.class);
        case IDENT:
            if (segment.call) {
                return null; // a method of the script itself
            }
            Object value = getVariables().get(segment.name);
            if (value != null) {
                return TypeRef.instance(value.getClass());
            }
            Class< ? > local = inferVariableType(segment.name, script, imports, depth);
            if (local != null) {
                return TypeRef.instance(local);
            }
            Class< ? > clazz = resolveClassName(segment.name, imports);
            if (clazz != null) {
                return TypeRef.staticOf(clazz);
            }
            if (index.isPackage(segment.name)) {
                return TypeRef.pkg(segment.name);
            }
            return null;
        default:
            return null;
        }
    }

    private TypeRef resolveNext( TypeRef current, Segment segment ) {
        if (segment.kind == Kind.INDEX) {
            return null; // element types are erased
        }
        if (current.isPackage()) {
            String name = current.packageName + "." + segment.name;
            Class< ? > clazz = index.getClassesInPackage(current.packageName).contains(segment.name) || !index.isReady()
                    ? loadClass(name)
                    : null;
            if (clazz != null) {
                return TypeRef.staticOf(clazz);
            }
            return index.isPackage(name) ? TypeRef.pkg(name) : null;
        }
        Class< ? > type = current.type;
        if (segment.call) {
            Method method = findMethod(type, segment.name, segment.argCount, current.isStatic);
            if (method != null) {
                return TypeRef.instance(method.getReturnType());
            }
            if (!current.isStatic) {
                Method gdk = findGdkMethod(type, segment.name, segment.argCount);
                if (gdk != null) {
                    return TypeRef.instance(gdk.getReturnType());
                }
            }
            return null;
        }
        // property access
        if (type.isArray() && segment.name.equals("length")) {
            return TypeRef.instance(int.class);
        }
        String capitalized = Character.toUpperCase(segment.name.charAt(0)) + segment.name.substring(1);
        for( Method m : type.getMethods() ) {
            if (m.getParameterCount() == 0 && Modifier.isStatic(m.getModifiers()) == current.isStatic
                    && (m.getName().equals("get" + capitalized) || m.getName().equals("is" + capitalized))) {
                return TypeRef.instance(m.getReturnType());
            }
        }
        for( Field f : type.getFields() ) {
            if (f.getName().equals(segment.name) && Modifier.isStatic(f.getModifiers()) == current.isStatic) {
                return TypeRef.instance(f.getType());
            }
        }
        return null;
    }

    private Method findMethod( Class< ? > type, String name, int argCount, boolean isStatic ) {
        Method fallback = null;
        for( Method m : type.getMethods() ) {
            if (!m.getName().equals(name) || (isStatic && !Modifier.isStatic(m.getModifiers()))) {
                continue;
            }
            if (m.getParameterCount() == argCount && m.getReturnType() != void.class) {
                return m;
            }
            if (fallback == null || fallback.getReturnType() == void.class) {
                fallback = m;
            }
        }
        return fallback;
    }

    private Method findGdkMethod( Class< ? > type, String name, int argCount ) {
        for( Method m : getGdkMethods(type) ) {
            if (m.getName().equals(name) && m.getParameterCount() == argCount + 1) {
                return m;
            }
        }
        return null;
    }

    /**
     * @param type the type of the receiver.
     * @return the GDK methods (static, the first parameter being the receiver) that apply to it.
     */
    public List<Method> getGdkMethods( Class< ? > type ) {
        if (gdkMethods == null) {
            List<Method> methods = new ArrayList<>();
            for( String gdkClass : GDK_CLASSES ) {
                Class< ? > c = loadClass(gdkClass);
                if (c == null) {
                    continue;
                }
                for( Method m : c.getMethods() ) {
                    if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() > 0
                            && m.getDeclaringClass() == c && !m.isAnnotationPresent(Deprecated.class)) {
                        methods.add(m);
                    }
                }
            }
            gdkMethods = methods;
        }
        Class< ? > boxed = box(type);
        List<Method> applicable = new ArrayList<>();
        for( Method m : gdkMethods ) {
            if (m.getParameterTypes()[0].isAssignableFrom(boxed)) {
                applicable.add(m);
            }
        }
        return applicable;
    }

    /**
     * Infer the type of a variable from its last declaration or assignment in the script.
     *
     * @param name the variable name.
     * @param script the script text that precedes the point of use.
     * @param imports the script imports.
     * @return the type or <code>null</code>.
     */
    public Class< ? > inferVariableType( String name, String script, ScriptImports imports ) {
        return inferVariableType(name, script, imports, 0);
    }

    private Class< ? > inferVariableType( String name, String script, ScriptImports imports, int depth ) {
        if (depth > MAX_DEPTH) {
            return null;
        }
        String quoted = Pattern.quote(name);
        Pattern typed = Pattern.compile("(?:^|[\\s(,;{])(?:final\\s+)?((?:[a-z_][\\w]*\\.)*[A-Z][\\w]*)(?:<[^\\n=;()]*?>)?(\\[\\])?\\s+"
                + quoted + "\\s*(?:=(?!=)|;|,|\\)|$|\\s+in\\b|\\s*->|\\s*:)", Pattern.MULTILINE);
        Pattern assigned = Pattern.compile("(?:^|[;{]|\\b(?:def|var|final))\\s*" + quoted + "\\s*=(?!=)\\s*([^\\n;]+)",
                Pattern.MULTILINE);

        int bestStart = -1;
        Class< ? > best = null;
        Matcher m = typed.matcher(script);
        while( m.find() ) {
            Class< ? > c = resolveClassName(m.group(1), imports);
            if (c != null && m.group(2) != null) {
                c = c.arrayType();
            }
            if (c != null && m.start() > bestStart) {
                bestStart = m.start();
                best = c;
            }
        }
        m = assigned.matcher(script);
        while( m.find() ) {
            if (m.start() <= bestStart) {
                continue;
            }
            String rhs = stripLineComment(m.group(1)).trim();
            Class< ? > c = null;
            Matcher asMatcher = AS_PATTERN.matcher(rhs);
            if (asMatcher.matches()) {
                c = resolveClassName(asMatcher.group(2), imports);
            } else {
                // the declaration itself must not be used to resolve its right hand side
                TypeRef ref = resolveExpression(rhs, script.substring(0, m.start()), imports, depth + 1);
                if (ref != null && !ref.isPackage() && !ref.isStatic) {
                    c = ref.type;
                }
            }
            if (c != null) {
                bestStart = m.start();
                best = c;
            }
        }
        return best;
    }

    /**
     * @param script the script text that precedes the point of use.
     * @return the names of the variables declared in the script.
     */
    public Set<String> findDeclaredVariables( String script ) {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = LOCAL_NAMES_PATTERN.matcher(script);
        while( m.find() ) {
            String name = m.group(1);
            if (!NOT_VARIABLES.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Extract the receiver of a member access, i.e. the expression that precedes a dot.
     *
     * @param text the text up to the dot (excluded).
     * @return the receiver expression or <code>null</code>.
     */
    public static String extractReceiver( String text ) {
        String t = text.stripTrailing();
        if (t.endsWith("?") || t.endsWith("*")) {
            t = t.substring(0, t.length() - 1);
        }
        int i = t.length() - 1;
        boolean afterBrace = false;
        while( i >= 0 ) {
            char c = t.charAt(i);
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                i--;
                afterBrace = false;
            } else if (c == '?' && i + 1 < t.length() && t.charAt(i + 1) == '.') {
                i--;
            } else if (c == ')' || c == ']' || c == '}') {
                int open = matchBackward(t, i);
                if (open < 0) {
                    return null;
                }
                afterBrace = c == '}';
                i = open - 1;
            } else if (Character.isWhitespace(c) && afterBrace) {
                // the closure of a call: list.collect { it * 2 }
                while( i >= 0 && Character.isWhitespace(t.charAt(i)) ) {
                    i--;
                }
                afterBrace = false;
            } else if ((c == '"' || c == '\'') && i == t.length() - 1) {
                int open = t.lastIndexOf(c, i - 1);
                if (open < 0) {
                    return null;
                }
                i = open - 1;
                break;
            } else {
                break;
            }
        }
        String receiver = t.substring(i + 1).trim();
        if (receiver.isEmpty()) {
            return null;
        }
        String before = t.substring(0, i + 1).stripTrailing();
        if (before.endsWith("new") && (before.length() == 3 || !Character.isJavaIdentifierPart(before.charAt(before.length() - 4)))) {
            receiver = "new " + receiver;
        }
        return receiver;
    }

    private static int matchBackward( String t, int closeIndex ) {
        int depth = 0;
        for( int i = closeIndex; i >= 0; i-- ) {
            char c = t.charAt(i);
            if (c == ')' || c == ']' || c == '}') {
                depth++;
            } else if (c == '(' || c == '[' || c == '{') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<Segment> parseChain( String expression ) {
        String s = expression.trim();
        int n = s.length();
        if (n == 0) {
            return null;
        }
        List<Segment> segments = new ArrayList<>();
        int i;
        char first = s.charAt(0);
        if (s.startsWith("new ")) {
            i = skipSpaces(s, 3);
            int start = i;
            while( i < n && (Character.isJavaIdentifierPart(s.charAt(i)) || s.charAt(i) == '.') ) {
                i++;
            }
            String className = s.substring(start, i);
            i = skipSpaces(s, i);
            if (i < n && s.charAt(i) == '<') {
                i = skipBalanced(s, i, '<', '>');
                if (i < 0) {
                    return null;
                }
                i = skipSpaces(s, i);
            }
            if (className.isEmpty() || i >= n || s.charAt(i) != '(') {
                return null;
            }
            i = skipBalanced(s, i, '(', ')');
            if (i < 0) {
                return null;
            }
            segments.add(new Segment(Kind.NEW, className, true, 0));
        } else if (first == '"' || first == '\'') {
            i = skipString(s, 0);
            if (i < 0) {
                return null;
            }
            segments.add(new Segment(Kind.STRING, null, false, 0));
        } else if (first == '[') {
            i = skipBalanced(s, 0, '[', ']');
            if (i < 0) {
                return null;
            }
            String inner = s.substring(1, i - 1).trim();
            boolean isMap = inner.equals(":") || hasTopLevel(inner, ':');
            segments.add(new Segment(isMap ? Kind.MAP : Kind.LIST, null, false, 0));
        } else if (Character.isDigit(first)) {
            i = 0;
            while( i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '_') ) {
                i++;
            }
            boolean decimal = false;
            if (i + 1 < n && s.charAt(i) == '.' && Character.isDigit(s.charAt(i + 1))) {
                decimal = true;
                i++;
                while( i < n && Character.isDigit(s.charAt(i)) ) {
                    i++;
                }
            }
            segments.add(new Segment(decimal ? Kind.DECIMAL : Kind.INTEGER, null, false, 0));
        } else if (Character.isJavaIdentifierStart(first)) {
            i = parseIdentifier(s, 0, segments);
            if (i < 0) {
                return null;
            }
        } else {
            return null;
        }

        while( true ) {
            i = skipSpaces(s, i);
            if (i >= n) {
                return segments;
            }
            char c = s.charAt(i);
            if (c == '[') {
                i = skipBalanced(s, i, '[', ']');
                if (i < 0) {
                    return null;
                }
                segments.add(new Segment(Kind.INDEX, null, false, 0));
                continue;
            }
            if ((c == '?' || c == '*') && i + 1 < n && s.charAt(i + 1) == '.') {
                i += 2;
            } else if (c == '.') {
                i++;
            } else {
                return null; // not a plain chain
            }
            i = skipSpaces(s, i);
            if (i >= n || !Character.isJavaIdentifierStart(s.charAt(i))) {
                return null;
            }
            i = parseIdentifier(s, i, segments);
            if (i < 0) {
                return null;
            }
        }
    }

    private static int parseIdentifier( String s, int start, List<Segment> segments ) {
        int n = s.length();
        int i = start;
        while( i < n && Character.isJavaIdentifierPart(s.charAt(i)) ) {
            i++;
        }
        String name = s.substring(start, i);
        int j = skipSpaces(s, i);
        boolean call = false;
        int argCount = 0;
        if (j < n && s.charAt(j) == '(') {
            int end = skipBalanced(s, j, '(', ')');
            if (end < 0) {
                return -1;
            }
            argCount = countArguments(s.substring(j + 1, end - 1));
            call = true;
            i = end;
            j = skipSpaces(s, i);
        }
        if (j < n && s.charAt(j) == '{') {
            int end = skipBalanced(s, j, '{', '}');
            if (end < 0) {
                return -1;
            }
            argCount++;
            call = true;
            i = end;
        }
        segments.add(new Segment(Kind.IDENT, name, call, argCount));
        return i;
    }

    private static int countArguments( String arguments ) {
        if (arguments.isBlank()) {
            return 0;
        }
        int count = 1;
        int depth = 0;
        for( int i = 0; i < arguments.length(); i++ ) {
            char c = arguments.charAt(i);
            if (c == '"' || c == '\'') {
                int end = skipString(arguments, i);
                if (end < 0) {
                    return count;
                }
                i = end - 1;
            } else if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasTopLevel( String text, char searched ) {
        int depth = 0;
        for( int i = 0; i < text.length(); i++ ) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                int end = skipString(text, i);
                if (end < 0) {
                    return false;
                }
                i = end - 1;
            } else if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == searched && depth == 0) {
                return true;
            }
        }
        return false;
    }

    /** @return the index after the closing bracket, or -1 if not closed. */
    private static int skipBalanced( String s, int openIndex, char open, char close ) {
        int depth = 0;
        for( int i = openIndex; i < s.length(); i++ ) {
            char c = s.charAt(i);
            if ((c == '"' || c == '\'') && open != '<') {
                int end = skipString(s, i);
                if (end < 0) {
                    return -1;
                }
                i = end - 1;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    /** @return the index after the closing quote, or -1 if not closed. */
    private static int skipString( String s, int quoteIndex ) {
        char quote = s.charAt(quoteIndex);
        for( int i = quoteIndex + 1; i < s.length(); i++ ) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == quote) {
                return i + 1;
            }
        }
        return -1;
    }

    private static int skipSpaces( String s, int i ) {
        while( i < s.length() && Character.isWhitespace(s.charAt(i)) ) {
            i++;
        }
        return i;
    }

    private static String stripLineComment( String text ) {
        int index = text.indexOf("//");
        return index >= 0 ? text.substring(0, index) : text;
    }

    /**
     * @param type a type.
     * @return the wrapper class for primitives, the type itself otherwise.
     */
    public static Class< ? > box( Class< ? > type ) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class)
            return Integer.class;
        if (type == long.class)
            return Long.class;
        if (type == double.class)
            return Double.class;
        if (type == float.class)
            return Float.class;
        if (type == boolean.class)
            return Boolean.class;
        if (type == short.class)
            return Short.class;
        if (type == byte.class)
            return Byte.class;
        if (type == char.class)
            return Character.class;
        return type;
    }
}
