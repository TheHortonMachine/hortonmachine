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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMModelRegistry;
import org.hortonmachine.gears.libs.modules.HMParameterKind;

import oms3.annotations.Documentation;
import oms3.annotations.Label;

/**
 * The processing modules a script can use, described for the model.
 *
 * <p>They are the modules of the {@value #MODULES_PACKAGE} package, which work on files, as in the
 * Spatial Toolbox and the command line, but also the ones hidden there (as Mapcalc), since a
 * script can use them. The model gets a short {@link #index(Collection) index} of all and the full
 * {@link #reference(ModuleDescriptor) reference} of the ones {@link #select(String, int) relevant}
 * to the request: describing all of them would not fit the context of a small model.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ModuleCatalog {

    /**
     * The package of the modules that work on files.
     */
    public static final String MODULES_PACKAGE = "org.hortonmachine.modules";

    /** The length the descriptions are cut to in the index. */
    private static final int INDEX_DESCRIPTION_LENGTH = 90;
    /** The length the documentation notes are cut to in the references. */
    private static final int NOTES_LENGTH = 400;

    /** Words that say nothing about which module is meant. */
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "from", "into", "then", "that", "this",
            "all", "each", "out", "get", "make", "create", "write", "read", "use", "using", "file", "files", "map", "maps",
            "script", "want", "need", "how", "can", "what", "which", "some", "its", "their", "them", "show", "also");

    private static ModuleCatalog defaultCatalog;

    private final Map<String, ModuleDescriptor> name2Module = new TreeMap<>();
    private final DataKinds kinds;

    /**
     * @return the catalog of the modules on the classpath, built at the first call.
     */
    public static synchronized ModuleCatalog getDefault() {
        if (defaultCatalog == null) {
            List<ModuleDescriptor> modules = new ArrayList<>();
            for( Class< ? extends HMModel> moduleClass : HMModelRegistry.getModelClasses() ) {
                if (!isScriptModule(moduleClass)) {
                    continue;
                }
                try {
                    modules.add(ModuleDescriptor.of(moduleClass));
                } catch (Throwable e) {
                    // a broken module must not break the assistant
                    Logger.INSTANCE.insertError("ModuleCatalog", "Can't describe the module " + moduleClass.getName(), e);
                }
            }
            defaultCatalog = new ModuleCatalog(modules);
        }
        return defaultCatalog;
    }

    /**
     * @param modules the modules of the catalog.
     */
    public ModuleCatalog( Collection<ModuleDescriptor> modules ) {
        for( ModuleDescriptor module : modules ) {
            name2Module.putIfAbsent(module.getName(), module);
        }
        kinds = DataKinds.load(name2Module.values());
    }

    /**
     * @return the kinds of data the modules read and write.
     */
    public DataKinds getKinds() {
        return kinds;
    }

    /**
     * Check if a module is for scripts: as {@link ModuleDescriptor#isUserModule(Class)}, but also if
     * it is hidden in the toolbox.
     *
     * @param moduleClass the class of the module.
     * @return <code>true</code> if the module is in the catalog.
     */
    public static boolean isScriptModule( Class< ? extends HMModel> moduleClass ) {
        if (!moduleClass.getName().equals(MODULES_PACKAGE + "." + moduleClass.getSimpleName())) {
            return false;
        }
        if (moduleClass.getSimpleName().startsWith("Oms") || moduleClass.getEnclosingClass() != null) {
            return false;
        }
        Label label = moduleClass.getAnnotation(Label.class);
        return label == null || (!label.value().isBlank() && !ModuleDescriptor.INTERNAL_CATEGORIES.contains(label.value()));
    }

    /**
     * @return the modules, sorted by name.
     */
    public Collection<ModuleDescriptor> getModules() {
        return name2Module.values();
    }

    /**
     * @param name the name of a module, as Pitfiller.
     * @return the module, or <code>null</code> if it is not in the catalog.
     */
    public ModuleDescriptor get( String name ) {
        return name2Module.get(name);
    }

    /**
     * Find the modules most relevant to a request, by the words they share with it: in the
     * name, the keywords, the folder and the description, in decreasing weight.
     *
     * @param request the request, or any text about what is needed.
     * @param max the maximum number of modules.
     * @return the modules, the most relevant first; only the ones sharing something with the request.
     */
    public List<ModuleDescriptor> select( String request, int max ) {
        Set<String> words = words(request);
        String lowerRequest = request.toLowerCase(Locale.ROOT);
        Map<ModuleDescriptor, Integer> scores = new LinkedHashMap<>();
        for( ModuleDescriptor module : name2Module.values() ) {
            int score = 0;
            String name = module.getName().toLowerCase(Locale.ROOT);
            if (lowerRequest.matches("(?s).*\\b" + name + "\\b.*")) {
                score += 20;
            }
            Set<String> keywordWords = new HashSet<>();
            for( String keyword : module.getKeywords() ) {
                keywordWords.addAll(words(keyword));
            }
            Set<String> folderWords = words(module.getFolder());
            Set<String> descriptionWords = words(module.getDescription());
            for( String word : words ) {
                if (matches(word, name)) {
                    score += 6;
                }
                if (containsMatch(keywordWords, word)) {
                    score += 3;
                }
                if (containsMatch(folderWords, word)) {
                    score += 1;
                }
                if (containsMatch(descriptionWords, word)) {
                    score += 1;
                }
            }
            if (score > 0) {
                scores.put(module, score);
            }
        }
        List<ModuleDescriptor> selected = new ArrayList<>(scores.keySet());
        selected.sort(Comparator.comparing(( ModuleDescriptor m ) -> -scores.get(m)).thenComparing(ModuleDescriptor::getName));
        return selected.subList(0, Math.min(max, selected.size()));
    }

    /**
     * @return the lower case words of the text, of at least 3 letters and without the {@link #STOP_WORDS}.
     */
    static Set<String> words( String text ) {
        Set<String> words = new HashSet<>();
        for( String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+") ) {
            if (word.length() >= 3 && !STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    private static boolean containsMatch( Set<String> words, String word ) {
        for( String other : words ) {
            if (matches(word, other)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Two words match if they are equal, share a beginning of at least 5 letters or one is the
     * other with up to 2 more letters, so that the forms of a word (pit, pits; contour, contours;
     * extract, extraction) match without a stemmer, but not net and network.
     */
    static boolean matches( String word, String other ) {
        if (word.equals(other)) {
            return true;
        }
        int common = 0;
        int length = Math.min(word.length(), other.length());
        while( common < length && word.charAt(common) == other.charAt(common) ) {
            common++;
        }
        return common >= 5 || (common >= 3 && common == length && Math.abs(word.length() - other.length()) <= 2);
    }

    /**
     * A short index of modules, one line each, grouped by folder.
     *
     * @param modules the modules.
     * @return the index.
     */
    public static String index( Collection<ModuleDescriptor> modules ) {
        Map<String, List<ModuleDescriptor>> folder2Modules = new TreeMap<>();
        for( ModuleDescriptor module : modules ) {
            String folder = module.getFolder().isBlank() ? "Other" : module.getFolder();
            folder2Modules.computeIfAbsent(folder, k -> new ArrayList<>()).add(module);
        }
        StringBuilder sb = new StringBuilder();
        for( Map.Entry<String, List<ModuleDescriptor>> entry : folder2Modules.entrySet() ) {
            sb.append(entry.getKey()).append(":\n");
            for( ModuleDescriptor module : entry.getValue() ) {
                sb.append("- ").append(module.getName()).append(": ").append(firstSentence(module.getDescription())).append("\n");
            }
        }
        return sb.toString();
    }

    private static String firstSentence( String text ) {
        int end = text.indexOf(". ");
        String sentence = end > 0 ? text.substring(0, end + 1) : text;
        return sentence.length() > INDEX_DESCRIPTION_LENGTH ? sentence.substring(0, INDEX_DESCRIPTION_LENGTH).trim() + "..."
                : sentence;
    }

    /**
     * The full reference of a module: description, notes, parameters with the kinds of their
     * data and what makes the inputs, and how to run it.
     *
     * @param module the module.
     * @return the reference.
     */
    public String reference( ModuleDescriptor module ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Module ").append(module.getName());
        if (!module.getFolder().isBlank()) {
            sb.append(" (").append(module.getFolder()).append(")");
        }
        sb.append(": ").append(module.getDescription()).append("\n");
        Documentation documentation = module.getModuleClass().getAnnotation(Documentation.class);
        if (documentation != null && !documentation.value().isBlank()) {
            String notes = documentation.value().replaceAll("\\s+", " ").trim();
            if (notes.length() > NOTES_LENGTH) {
                notes = notes.substring(0, NOTES_LENGTH).trim() + "...";
            }
            sb.append("Notes: ").append(notes).append("\n");
        }
        sb.append("Fields:\n");
        List<Parameter> results = new ArrayList<>();
        for( Parameter parameter : module.getParameters() ) {
            if (parameter.isComputed) {
                results.add(parameter);
                continue;
            }
            sb.append("- ").append(parameter.name).append(" (").append(describeKind(parameter)).append(", ")
                    .append(parameter.field.getType().getSimpleName());
            if (parameter.unit != null) {
                sb.append(", unit ").append(parameter.unit);
            }
            if (parameter.min != null || parameter.max != null) {
                sb.append(", range ").append(parameter.min != null ? format(parameter.min) : "").append("..")
                        .append(parameter.max != null ? format(parameter.max) : "");
            }
            if (!parameter.choices.isEmpty()) {
                sb.append(", one of ").append(String.join("|", parameter.choices));
            }
            if (parameter.defaultValue != null) {
                sb.append(", default ").append(formatValue(parameter.defaultValue));
            }
            sb.append(")");
            if (!parameter.description.isBlank()) {
                sb.append(": ").append(parameter.description);
            }
            String hint = kinds.hint(parameter);
            if (hint != null) {
                sb.append(" ").append(hint);
            }
            sb.append("\n");
        }
        for( Parameter parameter : results ) {
            sb.append("- ").append(parameter.name).append(" (result, ").append(parameter.field.getType().getSimpleName())
                    .append(", read it after process())");
            if (!parameter.description.isBlank()) {
                sb.append(": ").append(parameter.description);
            }
            sb.append("\n");
        }
        sb.append("Usage:\n").append(usage(module));
        return sb.toString();
    }

    private static String describeKind( Parameter parameter ) {
        if (parameter.kind == HMParameterKind.PARAMETER) {
            return "parameter";
        }
        String kind = parameter.kind == HMParameterKind.INPUT ? "input" : "output";
        return parameter.isPath() ? kind + " " + parameter.dataType.getName() + " path" : kind;
    }

    /**
     * @return the lines that set the inputs, outputs and the parameters without default of a module
     *          and run it, leaving out the ones described as optional.
     */
    static String usage( ModuleDescriptor module ) {
        String name = module.getName();
        String variable = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        StringBuilder sb = new StringBuilder();
        sb.append("def ").append(variable).append(" = new ").append(name).append("()\n");
        for( Parameter parameter : module.getParameters() ) {
            if (parameter.isComputed) {
                continue;
            }
            if (parameter.kind == HMParameterKind.PARAMETER && parameter.defaultValue != null) {
                continue;
            }
            if (parameter.description.toLowerCase(Locale.ROOT).contains("optional")) {
                continue;
            }
            sb.append(variable).append(".").append(parameter.name).append(" = ").append(placeholder(parameter)).append("\n");
        }
        sb.append(variable).append(".process()\n");
        return sb.toString();
    }

    private static String placeholder( Parameter parameter ) {
        Class< ? > type = parameter.field.getType();
        if (type == String.class) {
            return parameter.choices.isEmpty() ? "\"...\"" : "\"" + parameter.choices.get(0) + "\"";
        } else if (type == boolean.class || type == Boolean.class) {
            return "false";
        } else if (type.isEnum() && !parameter.choices.isEmpty()) {
            return type.getSimpleName() + "." + parameter.choices.get(0);
        }
        return "...";
    }

    private static String format( double value ) {
        return value == Math.rint(value) && Math.abs(value) < 1E15 ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static String formatValue( Object value ) {
        if (value instanceof Double || value instanceof Float) {
            return format(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value.getClass().isArray()) {
            return "an array";
        }
        return String.valueOf(value);
    }
}
