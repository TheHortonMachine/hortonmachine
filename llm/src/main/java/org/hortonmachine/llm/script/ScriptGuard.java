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
package org.hortonmachine.llm.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;

/**
 * Checks how a script uses the modules of the {@link ModuleCatalog}, which the compiler can't:
 * Groovy compiles <code>pitfiller.inElevv = dtm</code> and fails only when it runs.
 *
 * <p>The check follows the variables that get a new module (<code>def pit = new Pitfiller()</code>)
 * and reports the fields and methods used on them that the module lacks, the results assigned as
 * if they were inputs and the modules never run. It reads the script as text, without comments and
 * strings, so it can miss uses (other variable names, closures), but what it reports is wrong.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ScriptGuard {

    private static final Pattern NEW_MODULE = Pattern.compile("\\b(\\w+)\\s*=\\s*new\\s+(?:[\\w.]+\\.)?(\\w+)\\s*\\(");
    private static final Pattern NEW_ANY = Pattern.compile("\\bnew\\s+(?:[\\w.]+\\.)?(\\w+)\\s*\\(");
    /** A member of a variable, followed by a call (with arguments or a closure) or an assignment, if any. */
    private static final Pattern MEMBER = Pattern.compile("\\b(\\w+)\\s*\\.\\s*(\\w+)\\s*(\\(|\\{|=(?!=))?");
    /** A whole line assigning a variable or a member of a variable: target, member, value. */
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "^[ \\t]*(?:(?:def|var|final|String)[ \\t]+)?(\\w+)(?:[ \\t]*\\.[ \\t]*(\\w+))?[ \\t]*=(?!=)[ \\t]*(.+?)[ \\t]*;?[ \\t]*$",
            Pattern.MULTILINE);
    private static final Pattern MODULE_OUTPUT = Pattern.compile("(\\w+)\\s*\\.\\s*(\\w+)");
    private static final Pattern IDENTIFIER = Pattern.compile("\\w+");

    private ScriptGuard() {
    }

    /**
     * The result of a check.
     *
     * @param modules the names of the modules the script creates, in order of appearance.
     * @param errors the problems found, empty if none.
     */
    public record Result( List<String> modules, List<String> errors ) {
    }

    /**
     * Check a script.
     *
     * @param script the script.
     * @param catalog the modules.
     * @return the modules used and the problems found.
     */
    public static Result check( String script, ModuleCatalog catalog ) {
        String code = stripCommentsAndStrings(script);

        Set<String> modules = new LinkedHashSet<>();
        Matcher anyMatcher = NEW_ANY.matcher(code);
        while( anyMatcher.find() ) {
            if (catalog.get(anyMatcher.group(1)) != null) {
                modules.add(anyMatcher.group(1));
            }
        }

        Map<String, ModuleDescriptor> variable2Module = new LinkedHashMap<>();
        Matcher newMatcher = NEW_MODULE.matcher(code);
        while( newMatcher.find() ) {
            ModuleDescriptor module = catalog.get(newMatcher.group(2));
            if (module != null) {
                variable2Module.put(newMatcher.group(1), module);
            } else {
                // the variable got something else later
                variable2Module.remove(newMatcher.group(1));
            }
        }

        Set<String> errors = new LinkedHashSet<>();
        Set<String> runVariables = new LinkedHashSet<>();
        Matcher memberMatcher = MEMBER.matcher(code);
        while( memberMatcher.find() ) {
            String variable = memberMatcher.group(1);
            ModuleDescriptor module = variable2Module.get(variable);
            if (module == null) {
                continue;
            }
            String member = memberMatcher.group(2);
            String after = memberMatcher.group(3);
            Class< ? > moduleClass = module.getModuleClass();
            if ("(".equals(after) || "{".equals(after)) {
                if (member.equals("process") || member.equals("with")) {
                    runVariables.add(variable);
                }
                if (!hasMethod(moduleClass, member)) {
                    errors.add("The module " + module.getName() + " has no method " + member + "(): run it with " + variable
                            + ".process().");
                }
                continue;
            }
            Field field = publicField(moduleClass, member);
            if (field == null) {
                errors.add("The module " + module.getName() + " has no field " + member + ". Its fields are: "
                        + String.join(", ", fieldNames(module)) + ".");
                continue;
            }
            Parameter parameter = module.getParameter(member);
            if (after != null && parameter != null && parameter.isComputed) {
                errors.add("The field " + member + " of " + module.getName()
                        + " is a result of the module: it can't be set, read it after " + variable + ".process().");
            }
        }
        for( Map.Entry<String, ModuleDescriptor> entry : variable2Module.entrySet() ) {
            if (!runVariables.contains(entry.getKey())) {
                errors.add("The module " + entry.getValue().getName() + " in " + entry.getKey() + " is never run: call "
                        + entry.getKey() + ".process() after setting its fields.");
            }
        }
        errors.addAll(checkKinds(strip(script, false), variable2Module, catalog.getKinds()));
        return new Result(new ArrayList<>(modules), new ArrayList<>(errors));
    }

    /**
     * Find the inputs fed with data of another kind, as <code>basin.inFlow = dem</code>.
     *
     * <p>The kind of a value is the kind of the output that writes it (<code>pit.outPit</code>, or
     * a variable or path also given to an output), else the kinds told by the words of its variable
     * and of what the variable holds (dem in <code>demPath = "/data/dem.tif"</code>). An input is
     * reported only if none of them fits it.</p>
     *
     * @param code the script without comments, with its strings.
     * @param variable2Module the variables holding modules.
     * @param kinds the kinds of data.
     * @return the problems found.
     */
    static List<String> checkKinds( String code, Map<String, ModuleDescriptor> variable2Module, DataKinds kinds ) {
        Map<String, String> variable2Value = new LinkedHashMap<>();
        Map<String, String> value2Kind = new LinkedHashMap<>();
        List<Input> inputs = new ArrayList<>();
        Matcher matcher = ASSIGNMENT.matcher(code);
        while( matcher.find() ) {
            String target = matcher.group(1);
            String member = matcher.group(2);
            String value = matcher.group(3).trim();
            if (member == null) {
                variable2Value.put(target, value);
                continue;
            }
            ModuleDescriptor module = variable2Module.get(target);
            Parameter parameter = module == null ? null : module.getParameter(member);
            if (parameter == null || !parameter.isPath() || kinds.kindOf(member) == null) {
                continue;
            }
            if (member.startsWith("out")) {
                value2Kind.put(value, kinds.kindOf(member));
            } else {
                inputs.add(new Input(module, parameter, value));
            }
        }

        List<String> errors = new ArrayList<>();
        for( Input input : inputs ) {
            String kind = kinds.kindOf(input.parameter().name);
            Set<String> valueKinds = kindsOf(input.value(), variable2Module, variable2Value, value2Kind, kinds, 0);
            if (valueKinds.isEmpty() || valueKinds.stream().anyMatch(valueKind -> kinds.accepts(kind, valueKind))) {
                continue;
            }
            String description = kinds.getDescription(kind);
            List<String> producers = kinds.producers(input.parameter());
            StringBuilder sb = new StringBuilder("The field ").append(input.parameter().name).append(" of ")
                    .append(input.module().getName()).append(" reads ")
                    .append(description != null ? description + " (kind " + kind + ")" : "the kind " + kind).append(", but gets ")
                    .append(input.value()).append(", which is ").append(String.join(" or ", valueKinds)).append(".");
            if (!producers.isEmpty()) {
                sb.append(" Give it the output of ").append(String.join(", ", producers))
                        .append(" (run that module first), or a file of the user of that kind.");
            }
            errors.add(sb.toString());
        }
        return errors;
    }

    /**
     * @return the kinds a value can be, empty if unknown.
     */
    private static Set<String> kindsOf( String value, Map<String, ModuleDescriptor> variable2Module,
            Map<String, String> variable2Value, Map<String, String> value2Kind, DataKinds kinds, int depth ) {
        Set<String> found = new LinkedHashSet<>();
        if (value2Kind.containsKey(value)) {
            found.add(value2Kind.get(value));
            return found;
        }
        Matcher output = MODULE_OUTPUT.matcher(value);
        if (output.matches()) {
            if (variable2Module.containsKey(output.group(1))) {
                String kind = kinds.kindOf(output.group(2));
                if (kind != null) {
                    found.add(kind);
                }
                return found;
            }
        }
        // what a variable holds tells more than its name
        if (IDENTIFIER.matcher(value).matches() && variable2Value.containsKey(value) && depth < 3) {
            found.addAll(kindsOf(variable2Value.get(value), variable2Module, variable2Value, value2Kind, kinds, depth + 1));
            if (!found.isEmpty()) {
                return found;
            }
        }
        found.addAll(kinds.kindsInWords(value));
        return found;
    }

    /**
     * An input path field set in the script.
     *
     * @param module the module.
     * @param parameter the field.
     * @param value what it gets, as written.
     */
    private record Input( ModuleDescriptor module, Parameter parameter, String value ) {
    }

    private static Field publicField( Class< ? > moduleClass, String name ) {
        try {
            Field field = moduleClass.getField(name);
            return Modifier.isStatic(field.getModifiers()) ? null : field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static boolean hasMethod( Class< ? > moduleClass, String name ) {
        for( Method method : moduleClass.getMethods() ) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        // the methods Groovy adds to every object, as with, each, getClass
        return name.equals("with") || name.equals("tap") || name.equals("each") || name.equals("properties")
                || name.equals("dump") || name.equals("inspect");
    }

    private static List<String> fieldNames( ModuleDescriptor module ) {
        List<String> names = new ArrayList<>();
        for( Parameter parameter : module.getParameters() ) {
            names.add(parameter.name);
        }
        return names;
    }

    /**
     * Remove the comments and the content of the strings (the quotes stay), so that only code is
     * left: <code>"pit.tif"</code> must not look like the use of a variable <code>pit</code>.
     *
     * <p>Interpolations in double quoted strings are removed with the string: the script is not
     * checked there.</p>
     *
     * @param script the script.
     * @return the code, with the same lines.
     */
    static String stripCommentsAndStrings( String script ) {
        return strip(script, true);
    }

    /**
     * @param script the script.
     * @param strings <code>true</code> to remove the content of the strings too.
     * @return the code without comments, with the same lines.
     */
    private static String strip( String script, boolean strings ) {
        StringBuilder sb = new StringBuilder(script.length());
        int i = 0;
        int n = script.length();
        while( i < n ) {
            char c = script.charAt(i);
            if (c == '/' && i + 1 < n && script.charAt(i + 1) == '/') {
                while( i < n && script.charAt(i) != '\n' ) {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && script.charAt(i + 1) == '*') {
                int end = script.indexOf("*/", i + 2);
                end = end == -1 ? n : end + 2;
                keepNewlines(sb, script, i, end);
                i = end;
            } else if (c == '"' || c == '\'') {
                String quote = script.startsWith(String.valueOf(c).repeat(3), i) ? String.valueOf(c).repeat(3) : String.valueOf(c);
                int contentStart = i + quote.length();
                int closing = closingQuote(script, contentStart, quote);
                int contentEnd = closing >= 0 ? closing : -closing - 1;
                sb.append(quote);
                if (strings) {
                    keepNewlines(sb, script, contentStart, contentEnd);
                } else {
                    sb.append(script, contentStart, contentEnd);
                }
                if (closing >= 0) {
                    sb.append(quote);
                    i = closing + quote.length();
                } else {
                    i = contentEnd;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * @return the position of the closing quote or, if the string is not closed, <code>-end - 1</code>
     *          with end where it stops: at the end of its line for single quotes, else of the script.
     */
    private static int closingQuote( String script, int from, String quote ) {
        int i = from;
        while( i < script.length() ) {
            char c = script.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (quote.length() == 1 && c == '\n') {
                return -i - 1;
            }
            if (script.startsWith(quote, i)) {
                return i;
            }
            i++;
        }
        return -Math.min(i, script.length()) - 1;
    }

    private static void keepNewlines( StringBuilder sb, String script, int from, int to ) {
        for( int i = from; i < to && i < script.length(); i++ ) {
            if (script.charAt(i) == '\n') {
                sb.append('\n');
            }
        }
    }
}
