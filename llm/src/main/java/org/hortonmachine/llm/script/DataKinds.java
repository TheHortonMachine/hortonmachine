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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.libs.modules.HMParameterKind;

/**
 * The kinds of data the path fields of the modules read and write, to tell the model how the
 * modules chain and to check that a script feeds each input with the right kind.
 *
 * <p>A field <code>inXxx</code> reads, and <code>outXxx</code> writes, data of the kind
 * <code>Xxx</code>: the producers of an input are the outputs of the same kind and data type.
 * What the names can't say (descriptions, aliases, accepted kinds, preferred producers, the words
 * that tell a kind in a file name) is in the curated {@value #RESOURCE}, which grows with the
 * modules.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class DataKinds {

    /** The curated kinds, next to this class. */
    public static final String RESOURCE = "data-kinds.properties";

    /** The producers listed for an input; the overview lists all. */
    private static final int MAX_PRODUCERS = 5;

    private static final Pattern IN_OUT = Pattern.compile("^(in|out)([A-Z]\\w*)$");
    private static final Pattern GENERIC_NUMBERED = Pattern.compile(".*\\d$");
    private static final Pattern WORD_SPLIT = Pattern.compile("(?<=[a-z])(?=[A-Z])|[^A-Za-z0-9]+");

    /**
     * A curated kind.
     *
     * @param name the kind, as in the field names.
     * @param description what the data is, or <code>null</code>.
     * @param accepts the other kinds an input of this kind takes.
     * @param preferred the modules to propose first, in order (the key producers of the file).
     */
    private record Kind( String name, String description, List<String> accepts, List<String> preferred ) {
    }

    private final Map<String, Kind> kinds = new LinkedHashMap<>();
    private final Map<String, String> alias2Kind = new HashMap<>();
    private final Map<String, String> word2Kind = new HashMap<>();
    private final Set<String> generic = new HashSet<>();
    /** The producers by kind and data type, as Module.outField, in the order to propose them. */
    private final Map<String, List<String>> producers = new HashMap<>();
    /** The producers by kind, of any data type, for the overview. */
    private final Map<String, Set<String>> kindProducers = new LinkedHashMap<>();

    /**
     * @param modules the modules.
     * @return the kinds of the modules, with the curated {@value #RESOURCE}.
     */
    public static DataKinds load( Collection<ModuleDescriptor> modules ) {
        Properties properties = new Properties();
        try (InputStream stream = DataKinds.class.getResourceAsStream(RESOURCE)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
        } catch (Exception e) {
            // without the curated kinds the producers are still found from the names
            Logger.INSTANCE.insertError("DataKinds", "Can't read " + RESOURCE, e);
        }
        return new DataKinds(properties, modules);
    }

    /**
     * @param curated the curated kinds, in the format of {@value #RESOURCE}.
     * @param modules the modules.
     */
    public DataKinds( Properties curated, Collection<ModuleDescriptor> modules ) {
        generic.addAll(list(curated.getProperty("generic")));
        Set<String> names = new LinkedHashSet<>();
        for( String key : curated.stringPropertyNames() ) {
            int dot = key.indexOf('.');
            if (dot > 0) {
                names.add(key.substring(0, dot));
            }
        }
        for( String name : names ) {
            String description = curated.getProperty(name + ".description");
            kinds.put(name, new Kind(name, description == null || description.isBlank() ? null : description.trim(),
                    list(curated.getProperty(name + ".accepts")), list(curated.getProperty(name + ".producers"))));
            for( String alias : list(curated.getProperty(name + ".aliases")) ) {
                alias2Kind.put(alias, name);
            }
            for( String word : list(curated.getProperty(name + ".words")) ) {
                word2Kind.put(word.toLowerCase(Locale.ROOT), name);
            }
        }

        Map<String, List<String[]>> found = new HashMap<>();
        for( ModuleDescriptor module : modules ) {
            for( Parameter parameter : module.getParameters() ) {
                if (parameter.kind != HMParameterKind.OUTPUT || !parameter.isPath()) {
                    continue;
                }
                String kind = kindOf(parameter.name);
                if (kind != null) {
                    found.computeIfAbsent(key(kind, parameter), k -> new ArrayList<>())
                            .add(new String[]{module.getName(), module.getName() + "." + parameter.name});
                    kindProducers.computeIfAbsent(kind, k -> new LinkedHashSet<>());
                }
            }
        }
        for( Map.Entry<String, List<String[]>> entry : found.entrySet() ) {
            String kind = entry.getKey().substring(0, entry.getKey().indexOf('|'));
            List<String> preferred = kinds.containsKey(kind) ? kinds.get(kind).preferred() : List.of();
            List<String[]> list = entry.getValue();
            list.sort(Comparator.comparing(( String[] p ) -> {
                int index = preferred.indexOf(p[0]);
                return index < 0 ? preferred.size() : index;
            }).thenComparing(p -> p[1]));
            List<String> fields = new ArrayList<>();
            for( String[] producer : list ) {
                fields.add(producer[1]);
            }
            producers.put(entry.getKey(), fields);
        }
        // the overview lists the producers of any data type, in the same order
        for( Map.Entry<String, Set<String>> entry : kindProducers.entrySet() ) {
            for( Map.Entry<String, List<String>> producer : producers.entrySet() ) {
                if (producer.getKey().startsWith(entry.getKey() + "|")) {
                    entry.getValue().addAll(producer.getValue());
                }
            }
        }
    }

    private static String key( String kind, Parameter parameter ) {
        return kind + "|" + parameter.dataType.getName();
    }

    private static List<String> list( String value ) {
        List<String> list = new ArrayList<>();
        if (value != null) {
            for( String item : value.split(",") ) {
                if (!item.isBlank()) {
                    list.add(item.trim());
                }
            }
        }
        return list;
    }

    /**
     * @param fieldName the name of a field, as inFlow.
     * @return the kind of its data, as Flow, or <code>null</code> if the name says no kind or a generic one.
     */
    public String kindOf( String fieldName ) {
        Matcher matcher = IN_OUT.matcher(fieldName);
        if (!matcher.matches()) {
            return null;
        }
        String kind = alias2Kind.getOrDefault(matcher.group(2), matcher.group(2));
        if (generic.contains(kind) || GENERIC_NUMBERED.matcher(kind).matches()) {
            return null;
        }
        return kind;
    }

    /**
     * @param kind a kind.
     * @return its curated description, or <code>null</code>.
     */
    public String getDescription( String kind ) {
        Kind curated = kinds.get(kind);
        return curated == null ? null : curated.description();
    }

    /**
     * @param inputKind the kind of an input.
     * @param valueKind the kind of what it gets.
     * @return <code>true</code> if the input takes it.
     */
    public boolean accepts( String inputKind, String valueKind ) {
        if (inputKind.equals(valueKind)) {
            return true;
        }
        Kind curated = kinds.get(inputKind);
        return curated != null && curated.accepts().contains(valueKind);
    }

    /**
     * @param input an input path field.
     * @return the outputs that make what it reads, as Module.outField, the preferred first, also
     *          of the kinds it accepts; empty if none or not an input of a kind.
     */
    public List<String> producers( Parameter input ) {
        List<String> list = new ArrayList<>();
        String kind = kindOf(input.name);
        if (kind == null || input.kind != HMParameterKind.INPUT || !input.isPath()) {
            return list;
        }
        list.addAll(producers.getOrDefault(key(kind, input), List.of()));
        Kind curated = kinds.get(kind);
        if (curated != null) {
            for( String accepted : curated.accepts() ) {
                list.addAll(producers.getOrDefault(key(accepted, input), List.of()));
            }
        }
        return list;
    }

    /**
     * The hint added to a field in the module reference: the kind and, for inputs, what makes it.
     *
     * @param parameter the field.
     * @return the hint, as <code>[kind Flow, from FlowDirections.outFlow, DrainDir.outFlow]</code>, or
     *          <code>null</code> if there is nothing to say.
     */
    public String hint( Parameter parameter ) {
        if (!parameter.isPath()) {
            return null;
        }
        String kind = kindOf(parameter.name);
        if (kind == null) {
            return null;
        }
        boolean described = getDescription(kind) != null;
        if (parameter.kind == HMParameterKind.OUTPUT) {
            return described ? "[kind " + kind + "]" : null;
        }
        List<String> list = producers(parameter);
        if (list.isEmpty()) {
            return described ? "[kind " + kind + ", a file of the user]" : null;
        }
        String from = String.join(", ", list.subList(0, Math.min(MAX_PRODUCERS, list.size())))
                + (list.size() > MAX_PRODUCERS ? ", ..." : "");
        // no module makes the kind itself, only the ones it accepts: the user has it, as a dem
        boolean made = producers.containsKey(key(kind, parameter));
        return "[kind " + kind + (made ? ", from " : ", a file of the user, or from ") + from + "]";
    }

    /**
     * The described kinds with the modules that make them, to show how the modules chain.
     *
     * @return the overview, one line per kind, empty if no kind is described.
     */
    public String overview() {
        StringBuilder sb = new StringBuilder();
        for( Kind kind : kinds.values() ) {
            if (kind.description() == null) {
                continue;
            }
            sb.append("- ").append(kind.name()).append(": ").append(kind.description());
            Set<String> made = kindProducers.getOrDefault(kind.name(), Set.of());
            sb.append(made.isEmpty() ? "." : ". Made by " + String.join(", ", made) + ".");
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * @param text a variable name, a path or an expression.
     * @return the kinds its words tell, as dem in demPath or in /data/dem.tif; empty if none.
     */
    public Set<String> kindsInWords( String text ) {
        Set<String> found = new LinkedHashSet<>();
        for( String word : WORD_SPLIT.split(text) ) {
            String kind = word2Kind.get(word.toLowerCase(Locale.ROOT));
            if (kind != null) {
                found.add(kind);
            }
        }
        return found;
    }
}
