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
package org.hortonmachine.cli;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hortonmachine.Version;
import org.hortonmachine.cli.ModuleDescriptor.DataType;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMParameterKind;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * The HortonMachine command line: lists, describes and runs the modules.
 *
 * <pre>
 * hm-cli list [filter]
 * hm-cli help &lt;Module&gt;
 * hm-cli describe [Module]
 * hm-cli run &lt;Module&gt; [--parameter=value ...] [--params=file.json] [--debug]
 * hm-cli version
 * </pre>
 *
 * <p>Exit codes: 0 on success, 1 if the module failed, 2 for wrong usage.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class HmCli {
    public static final int EXIT_OK = 0;
    public static final int EXIT_FAILURE = 1;
    public static final int EXIT_USAGE = 2;

    /** Version of the format of the describe command, to change when it changes incompatibly. */
    public static final int DESCRIBE_FORMAT = 1;

    public static final String OUTPUT_PREFIX = "Output: ";

    private static final int WIDTH = 100;

    private final PrintStream out;
    private final PrintStream err;

    /**
     * A wrong usage, reported with its message and exit code {@link #EXIT_USAGE}.
     */
    private static class UsageException extends Exception {
        private static final long serialVersionUID = 1L;

        UsageException( String message ) {
            super(message);
        }
    }

    public HmCli( PrintStream out, PrintStream err ) {
        this.out = out;
        this.err = err;
    }

    public static void main( String[] args ) {
        checkGdalQuietly();
        System.exit(new HmCli(System.out, System.err).run(args));
    }

    /**
     * The GDAL bindings print an error when the native GDAL is not installed, which is the normal case
     * and confuses the users and the applications reading the errors: do the check once, silently.
     */
    private static void checkGdalQuietly() {
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(OutputStream.nullOutputStream()));
            Class.forName("it.geosolutions.imageio.gdalframework.GDALUtilities").getMethod("isGDALAvailable").invoke(null);
        } catch (Throwable e) {
            // GDAL support not available
        } finally {
            System.setErr(originalErr);
        }
    }

    /**
     * Run a command.
     *
     * @param args the command and its arguments.
     * @return the exit code.
     */
    public int run( String[] args ) {
        if (args.length == 0) {
            printUsage();
            return EXIT_USAGE;
        }
        List<String> arguments = new ArrayList<>(List.of(args));
        String command = arguments.remove(0);
        try {
            switch( command ) {
            case "list":
                return list(arguments.isEmpty() ? null : arguments.get(0));
            case "help":
                if (arguments.isEmpty()) {
                    printUsage();
                    return EXIT_OK;
                }
                return help(arguments.get(0));
            case "describe":
                arguments.remove("--json");
                return describe(arguments.isEmpty() ? null : arguments.get(0));
            case "run":
                if (arguments.isEmpty()) {
                    throw new UsageException("Missing the name of the module to run.");
                }
                return runModule(arguments.remove(0), arguments);
            case "version":
            case "--version":
                out.println("HortonMachine " + Version.getVersion());
                return EXIT_OK;
            case "--help":
            case "-h":
                printUsage();
                return EXIT_OK;
            default:
                throw new UsageException("Unknown command: " + command);
            }
        } catch (UsageException e) {
            err.println("Error: " + e.getMessage());
            err.println("Run 'hm-cli --help' for the usage.");
            return EXIT_USAGE;
        } catch (Exception e) {
            err.println("Error: " + e.getMessage());
            return EXIT_FAILURE;
        }
    }

    private void printUsage() {
        out.println("HortonMachine command line " + Version.getVersion());
        out.println();
        out.println("Usage:");
        out.println("  hm-cli list [filter]         list the modules, optionally only those matching the filter");
        out.println("  hm-cli help <Module>         describe a module and its parameters");
        out.println("  hm-cli describe [Module]     describe all the modules, or one, as JSON");
        out.println("  hm-cli run <Module> [--parameter=value ...] [--params=file.json] [--debug]");
        out.println("                               run a module; --debug shows the full error traces");
        out.println("  hm-cli version               show the version");
        out.println();
        out.println("Exit codes: 0 on success, 1 if the module failed, 2 for wrong usage.");
    }

    private int list( String filter ) {
        String lowerFilter = filter != null ? filter.toLowerCase(Locale.ROOT) : null;
        List<ModuleDescriptor> modules = new ArrayList<>();
        int nameWidth = 0;
        for( Class< ? extends HMModel> moduleClass : ModuleDescriptor.getAvailableModules().values() ) {
            ModuleDescriptor module = ModuleDescriptor.of(moduleClass);
            if (lowerFilter != null && !matches(module, lowerFilter)) {
                continue;
            }
            modules.add(module);
            nameWidth = Math.max(nameWidth, module.getName().length());
        }
        for( ModuleDescriptor module : modules ) {
            out.println(pad(module.getName(), nameWidth + 2) + module.getFolder());
        }
        return EXIT_OK;
    }

    private static boolean matches( ModuleDescriptor module, String lowerFilter ) {
        if (module.getName().toLowerCase(Locale.ROOT).contains(lowerFilter)
                || module.getFolder().toLowerCase(Locale.ROOT).contains(lowerFilter)) {
            return true;
        }
        for( String keyword : module.getKeywords() ) {
            if (keyword.toLowerCase(Locale.ROOT).contains(lowerFilter)) {
                return true;
            }
        }
        return false;
    }

    private int help( String moduleName ) throws UsageException {
        ModuleDescriptor module = ModuleDescriptor.of(findModule(moduleName));
        out.println(module.getName() + (module.getFolder().isEmpty() ? "" : " (" + module.getFolder() + ")")
                + (module.getStatus().equals("experimental") ? " - experimental" : ""));
        out.println();
        printWrapped(module.getDescription(), "");
        if (!module.getAuthors().isEmpty()) {
            out.println();
            printWrapped("Authors: " + String.join(", ", module.getAuthors()), "");
        }
        out.println();
        out.println("Usage: hm-cli run " + module.getName() + " [--parameter=value ...]");

        for( HMParameterKind kind : HMParameterKind.values() ) {
            List<Parameter> parameters = new ArrayList<>();
            for( Parameter parameter : module.getParameters() ) {
                if (parameter.kind == kind && !parameter.isComputed) {
                    parameters.add(parameter);
                }
            }
            if (!parameters.isEmpty()) {
                out.println();
                out.println(kind.getTitle() + ":");
                for( Parameter parameter : parameters ) {
                    String value = parameter.dataType == DataType.BOOLEAN ? "[=true|false]" : "=<" + parameter.dataType.getName() + ">";
                    out.println("  --" + parameter.name + value);
                    printWrapped(parameterDetails(parameter), "      ");
                }
            }
        }
        List<Parameter> computed = new ArrayList<>();
        for( Parameter parameter : module.getParameters() ) {
            if (parameter.isComputed) {
                computed.add(parameter);
            }
        }
        if (!computed.isEmpty()) {
            out.println();
            out.println("Computed values, printed at the end of the run:");
            for( Parameter parameter : computed ) {
                out.println("  " + parameter.name);
                printWrapped(parameterDetails(parameter), "      ");
            }
        }
        return EXIT_OK;
    }

    private static String parameterDetails( Parameter parameter ) {
        StringBuilder sb = new StringBuilder(parameter.description);
        if (parameter.unit != null) {
            sb.append(" Unit: ").append(parameter.unit).append(".");
        }
        if (parameter.min != null || parameter.max != null) {
            sb.append(" Range: ").append(parameter.min != null ? ModuleDescriptor.formatNumber(parameter.min) : "").append(" to ")
                    .append(parameter.max != null ? ModuleDescriptor.formatNumber(parameter.max) : "").append(".");
        }
        if (parameter.dataType == DataType.NUMBERS) {
            sb.append(" A list of numbers separated by commas.");
        }
        if (!parameter.choices.isEmpty()) {
            sb.append(" One of: ").append(String.join(", ", parameter.choices)).append(".");
        }
        if (parameter.defaultValue != null && !parameter.defaultValue.getClass().isArray()) {
            sb.append(" Default: ").append(parameter.defaultValue).append(".");
        }
        return sb.toString().trim();
    }

    private int describe( String moduleName ) throws Exception {
        List<Class< ? extends HMModel>> classes = new ArrayList<>();
        if (moduleName != null) {
            classes.add(findModule(moduleName));
        } else {
            classes.addAll(ModuleDescriptor.getAvailableModules().values());
        }
        List<Map<String, Object>> modules = new ArrayList<>();
        for( Class< ? extends HMModel> moduleClass : classes ) {
            modules.add(toJson(ModuleDescriptor.of(moduleClass)));
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", DESCRIBE_FORMAT);
        root.put("version", Version.getVersion());
        root.put("modules", modules);
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        out.println(mapper.writeValueAsString(root));
        return EXIT_OK;
    }

    private static Map<String, Object> toJson( ModuleDescriptor module ) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", module.getName());
        map.put("class", module.getModuleClass().getName());
        map.put("folder", module.getFolder());
        map.put("description", module.getDescription());
        map.put("status", module.getStatus());
        map.put("keywords", module.getKeywords());
        map.put("authors", module.getAuthors());
        List<Map<String, Object>> parameters = new ArrayList<>();
        for( Parameter parameter : module.getParameters() ) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", parameter.name);
            p.put("kind", parameter.kind.name().toLowerCase(Locale.ROOT));
            p.put("type", parameter.dataType.getName());
            p.put("computed", parameter.isComputed);
            p.put("description", parameter.description);
            putIfNotNull(p, "unit", parameter.unit);
            putIfNotNull(p, "min", parameter.min);
            putIfNotNull(p, "max", parameter.max);
            if (!parameter.choices.isEmpty()) {
                p.put("choices", parameter.choices);
            }
            putIfNotNull(p, "extension", parameter.extension);
            if (parameter.defaultValue instanceof Number || parameter.defaultValue instanceof Boolean) {
                p.put("default", parameter.defaultValue);
            } else if (parameter.defaultValue != null && !parameter.defaultValue.getClass().isArray()) {
                p.put("default", parameter.defaultValue.toString());
            }
            parameters.add(p);
        }
        map.put("parameters", parameters);
        return map;
    }

    private static void putIfNotNull( Map<String, Object> map, String key, Object value ) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private int runModule( String moduleName, List<String> arguments ) throws Exception {
        Class< ? extends HMModel> moduleClass = findModule(moduleName);
        ModuleDescriptor module = ModuleDescriptor.of(moduleClass);

        boolean debug = arguments.remove("--debug");
        Map<String, String> values = parseArguments(arguments, module);

        long start = System.currentTimeMillis();
        Map<String, Object> outputs;
        try {
            outputs = new ModuleRunner(module).run(values, new CliProgressMonitor(out, err));
        } catch (Throwable e) {
            err.println("Error: the module " + module.getName() + " failed: " + e.getMessage());
            if (debug) {
                e.printStackTrace(err);
            } else {
                err.println("Run again with --debug to see the full error trace.");
            }
            return EXIT_FAILURE;
        }

        for( Map.Entry<String, Object> entry : outputs.entrySet() ) {
            Object value = entry.getValue();
            if (!value.getClass().isArray() || Array.getLength(value) < 1000) {
                out.println(OUTPUT_PREFIX + entry.getKey() + " = " + ModuleRunner.valueToString(value));
            }
        }
        out.println(String.format(Locale.ROOT, "Done in %.1f s.", (System.currentTimeMillis() - start) / 1000.0));
        return EXIT_OK;
    }

    /**
     * Read the --parameter=value arguments and the --params=file.json file, checking the values.
     *
     * @return the values by parameter name, those of the arguments overriding those of the file.
     */
    private Map<String, String> parseArguments( List<String> arguments, ModuleDescriptor module ) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        for( String argument : arguments ) {
            if (!argument.startsWith("--")) {
                throw new UsageException("Unexpected argument: " + argument + ". Parameters are given as --parameter=value.");
            }
            String name = argument.substring(2);
            String value = null;
            int equalsIndex = name.indexOf('=');
            if (equalsIndex >= 0) {
                value = name.substring(equalsIndex + 1);
                name = name.substring(0, equalsIndex);
            }
            if (name.equals("params")) {
                if (value == null) {
                    throw new UsageException("The parameters file is given as --params=file.json.");
                }
                for( Map.Entry<String, String> entry : readParamsFile(new File(value)).entrySet() ) {
                    values.putIfAbsent(entry.getKey(), entry.getValue());
                }
                continue;
            }
            if (value == null) {
                Parameter parameter = module.getParameter(name);
                if (parameter == null || parameter.dataType != DataType.BOOLEAN) {
                    throw new UsageException("Missing the value of --" + name + ", give it as --" + name + "=value.");
                }
                // a boolean flag
                value = "true";
            }
            values.put(name, value);
        }

        Map<String, String> errors = module.check(values);
        if (!errors.isEmpty()) {
            throw new UsageException(String.join(System.lineSeparator() + "Error: ", errors.values()));
        }
        return values;
    }

    private Map<String, String> readParamsFile( File file ) throws Exception {
        if (!file.isFile()) {
            throw new UsageException("The parameters file doesn't exist: " + file);
        }
        JsonNode root = new ObjectMapper().readTree(file);
        if (!root.isObject()) {
            throw new UsageException("The parameters file must contain a JSON object of parameter names and values.");
        }
        Map<String, String> values = new LinkedHashMap<>();
        for( Map.Entry<String, JsonNode> field : root.properties() ) {
            JsonNode value = field.getValue();
            if (value.isNull()) {
                continue;
            }
            if (value.isContainerNode()) {
                throw new UsageException("The value of " + field.getKey() + " in the parameters file is not a single value.");
            }
            values.put(field.getKey(), value.asText());
        }
        return values;
    }

    private Class< ? extends HMModel> findModule( String name ) throws UsageException {
        Map<String, Class< ? extends HMModel>> modules = ModuleDescriptor.getAvailableModules();
        Class< ? extends HMModel> moduleClass = modules.get(name);
        if (moduleClass != null) {
            return moduleClass;
        }
        List<String> similar = new ArrayList<>();
        for( String moduleName : modules.keySet() ) {
            if (moduleName.equalsIgnoreCase(name)) {
                return modules.get(moduleName);
            }
            if (moduleName.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                similar.add(moduleName);
            }
        }
        throw new UsageException("Unknown module: " + name
                + (similar.isEmpty() ? ". Run 'hm-cli list' to see the modules." : ". Did you mean: " + String.join(", ", similar) + "?"));
    }

    private static String pad( String text, int width ) {
        StringBuilder sb = new StringBuilder(text);
        while( sb.length() < width ) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private void printWrapped( String text, String indent ) {
        if (text == null || text.isBlank()) {
            return;
        }
        StringBuilder line = new StringBuilder(indent);
        for( String word : text.split("\\s+") ) {
            if (line.length() > indent.length() && line.length() + 1 + word.length() > WIDTH) {
                out.println(line);
                line = new StringBuilder(indent);
            }
            if (line.length() > indent.length()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > indent.length()) {
            out.println(line);
        }
    }
}
