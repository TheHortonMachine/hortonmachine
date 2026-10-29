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
package org.hortonmachine.geoscript.console.assistant;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.hortonmachine.HM;
import org.hortonmachine.gears.assistant.IScriptChecker;

import groovy.lang.GroovyClassLoader;

/**
 * The parts of the {@link org.hortonmachine.gears.assistant.ScriptContext} of the scripting
 * console that don't depend on its widgets: the reference of the {@link HM} helper class, the
 * variables of the binding and the compile only checker.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ConsoleAssistantContext {

    private static final String CHECK_SCRIPT_NAME = "AssistantScript.groovy";

    private static String helperReference;

    private ConsoleAssistantContext() {
    }

    /**
     * The reference of the {@link HM} helper class: the described methods of {@link HM#methods()},
     * then the signatures of the public static methods it doesn't describe.
     *
     * @return the reference, built once.
     */
    public static synchronized String helperReference() {
        if (helperReference == null) {
            String described = HM.methods();
            TreeSet<String> others = new TreeSet<>();
            for( Method method : HM.class.getDeclaredMethods() ) {
                int modifiers = method.getModifiers();
                String name = method.getName();
                if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || method.isSynthetic()
                        || name.equals("methods") || name.equals("main") || described.contains(name + "(")) {
                    continue;
                }
                others.add(signature(method));
            }
            StringBuilder sb = new StringBuilder(described.strip()).append("\n");
            if (!others.isEmpty()) {
                sb.append("\nOther methods\n-------------\n");
                for( String signature : others ) {
                    sb.append("\t").append(signature).append("\n");
                }
            }
            helperReference = sb.toString();
        }
        return helperReference;
    }

    /**
     * @return the method as <code>Type name( Type param, ... )</code>, with the parameter names
     *          if the class was compiled with them, and without the packages of the types.
     */
    static String signature( Method method ) {
        StringBuilder sb = new StringBuilder(simpleType(method.getGenericReturnType().getTypeName()));
        sb.append(" ").append(method.getName()).append("( ");
        Parameter[] parameters = method.getParameters();
        for( int i = 0; i < parameters.length; i++ ) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(simpleType(parameters[i].getParameterizedType().getTypeName()));
            if (parameters[i].isNamePresent()) {
                sb.append(" ").append(parameters[i].getName());
            }
        }
        return sb.append(" )").toString().replace("(  )", "()");
    }

    private static String simpleType( String typeName ) {
        return typeName.replaceAll("\\b[a-z][\\w]*\\.(?=[\\w.]*[A-Z])", "").replace('$', '.');
    }

    /**
     * @param bindingVariables the variables of the binding of the console.
     * @return the variables, from name to the simple name of the type of their value, sorted by
     *          name; never the values.
     */
    public static Map<String, String> variables( Map< ? , ? > bindingVariables ) {
        List<Map.Entry< ? , ? >> entries;
        try {
            entries = new ArrayList<>(bindingVariables.entrySet());
        } catch (RuntimeException e) {
            // a running script changed the binding meanwhile
            return Map.of();
        }
        entries.sort(Comparator.comparing(e -> String.valueOf(e.getKey())));
        Map<String, String> variables = new LinkedHashMap<>();
        for( Map.Entry< ? , ? > entry : entries ) {
            Object value = entry.getValue();
            variables.put(String.valueOf(entry.getKey()), value == null ? "null" : value.getClass().getSimpleName());
        }
        return variables;
    }

    /**
     * @param parent the class loader of the console, to resolve the classes the scripts use.
     * @return a checker that compiles a script as the console would, without running it.
     */
    public static IScriptChecker checker( ClassLoader parent ) {
        return script -> compile(parent, script);
    }

    /**
     * Compile a script up to the class generation, in a class loader of its own that is then
     * dropped, so that nothing of it stays in the console.
     *
     * @param parent the class loader of the console.
     * @param script the script.
     * @return the compilation errors, or <code>null</code> if it compiles.
     */
    static String compile( ClassLoader parent, String script ) {
        CompilerConfiguration config = new CompilerConfiguration();
        try (GroovyClassLoader loader = new GroovyClassLoader(parent, config)) {
            CompilationUnit unit = new CompilationUnit(config, null, loader);
            unit.addSource(CHECK_SCRIPT_NAME, script);
            unit.compile(Phases.CLASS_GENERATION);
            return null;
        } catch (MultipleCompilationErrorsException e) {
            return e.getMessage().replace("startup failed:", "").replace(CHECK_SCRIPT_NAME + ": ", "").trim();
        } catch (Exception | LinkageError e) {
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }
}
