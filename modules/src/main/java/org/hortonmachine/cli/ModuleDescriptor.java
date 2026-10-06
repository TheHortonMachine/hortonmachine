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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMModelRegistry;
import org.hortonmachine.gears.libs.modules.HMParameterKind;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.Out;
import oms3.annotations.Range;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

/**
 * The description of a module and its parameters, read from its annotations,
 * as needed to run it from the command line or to describe it to other applications.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ModuleDescriptor {

    /**
     * The categories of the readers and writers used only inside other modules.
     */
    public static final Set<String> INTERNAL_CATEGORIES = Set.of(HMConstants.GRIDGEOMETRYREADER, HMConstants.RASTERREADER,
            HMConstants.RASTERWRITER, HMConstants.FEATUREREADER, HMConstants.FEATUREWRITER, HMConstants.GENERICREADER,
            HMConstants.GENERICWRITER, HMConstants.HASHMAP_READER, HMConstants.HASHMAP_WRITER, HMConstants.LIST_READER,
            HMConstants.LIST_WRITER);

    /** Fields of the modules framework, not real parameters. */
    private static final List<String> SKIPPED_FIELDS = Arrays.asList("pm", "doProcess", "doReset");

    private static final Map<Integer, String> STATUS_NAMES = new LinkedHashMap<>();
    static {
        STATUS_NAMES.put(Status.EXPERIMENTAL, "experimental");
        STATUS_NAMES.put(Status.DRAFT, "draft");
        STATUS_NAMES.put(Status.TESTED, "tested");
        STATUS_NAMES.put(Status.VALIDATED, "validated");
        STATUS_NAMES.put(Status.CERTIFIED, "certified");
    }

    /**
     * The type of data of a parameter, as needed to choose an input widget or convert a value.
     */
    public enum DataType {
        RASTER("raster"), VECTOR("vector"), LAS("las"), CSV("csv"), FILE("file"), FOLDER("folder"), CRS("crs"),
        CHOICE("choice"), TEXT("text"), STRING("string"), NUMBER("number"), INTEGER("integer"), BOOLEAN("boolean"),
        /** A list of numbers, for one dimensional arrays. */
        NUMBERS("numbers"), OTHER("other");

        private final String name;

        private DataType( String name ) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * A parameter of a module.
     */
    public static class Parameter {
        public final Field field;
        public final String name;
        public final HMParameterKind kind;
        public final DataType dataType;
        public final String description;
        public final String unit;
        /** The minimum and maximum allowed, or <code>null</code> if not bounded. */
        public final Double min, max;
        /** The default value, or <code>null</code>; arrays are not empty. */
        public final Object defaultValue;
        /** The allowed values of a choice, else empty. */
        public final List<String> choices;
        /** The extension of the files, as gpkg or csv, if known, else <code>null</code>. */
        public final String extension;
        /** If the value is written by the module (<code>@Out</code>) and not set by the user. */
        public final boolean isComputed;

        Parameter( Field field, HMParameterKind kind, DataType dataType, String description, String unit, Double min,
                Double max, Object defaultValue, List<String> choices, String extension, boolean isComputed ) {
            this.field = field;
            this.name = field.getName();
            this.kind = kind;
            this.dataType = dataType;
            this.description = description;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.choices = choices;
            this.extension = extension;
            this.isComputed = isComputed;
        }

        /**
         * @return <code>true</code> if the parameter is a path to a file or folder.
         */
        public boolean isPath() {
            switch( dataType ) {
            case RASTER:
            case VECTOR:
            case LAS:
            case CSV:
            case FILE:
            case FOLDER:
                return true;
            default:
                return false;
            }
        }
    }

    private final Class< ? extends HMModel> moduleClass;
    private final String folder;
    private final String description;
    private final String status;
    private final List<String> keywords;
    private final List<String> authors;
    private final List<Parameter> parameters = new ArrayList<>();

    private ModuleDescriptor( Class< ? extends HMModel> moduleClass ) {
        this.moduleClass = moduleClass;

        Label label = moduleClass.getAnnotation(Label.class);
        folder = label != null ? label.value().replaceFirst("^HortonMachine/", "").trim() : "";
        Description descriptionAnn = moduleClass.getAnnotation(Description.class);
        description = descriptionAnn != null ? clean(descriptionAnn.value()) : "";
        Status statusAnn = moduleClass.getAnnotation(Status.class);
        status = statusAnn != null ? STATUS_NAMES.getOrDefault(statusAnn.value(), String.valueOf(statusAnn.value())) : "";
        Keywords keywordsAnn = moduleClass.getAnnotation(Keywords.class);
        List<String> keywordsList = new ArrayList<>();
        if (keywordsAnn != null) {
            for( String keyword : keywordsAnn.value().split(",") ) {
                if (!keyword.isBlank()) {
                    keywordsList.add(keyword.trim());
                }
            }
        }
        keywords = Collections.unmodifiableList(keywordsList);
        Author authorAnn = moduleClass.getAnnotation(Author.class);
        List<String> authorsList = new ArrayList<>();
        if (authorAnn != null) {
            for( String author : authorAnn.name().split(",") ) {
                if (!author.isBlank()) {
                    authorsList.add(author.trim());
                }
            }
        }
        authors = Collections.unmodifiableList(authorsList);

        Object instance = null;
        try {
            instance = moduleClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // without an instance the default values are not available
        }
        Class< ? > omsClass = HMModelRegistry.getModelClass("Oms" + moduleClass.getSimpleName());
        for( Field field : getFields(moduleClass) ) {
            boolean isIn = field.isAnnotationPresent(In.class);
            boolean isOut = field.isAnnotationPresent(Out.class);
            if ((!isIn && !isOut) || SKIPPED_FIELDS.contains(field.getName())) {
                continue;
            }
            UI ui = field.getAnnotation(UI.class);
            String uiHint = ui != null ? ui.value() : "";
            if (uiHint.contains(HMConstants.HIDE_UI_HINT)) {
                continue;
            }
            Description fieldDescription = field.getAnnotation(Description.class);
            Unit unit = field.getAnnotation(Unit.class);
            Range range = field.getAnnotation(Range.class);
            Double min = range != null && range.min() != Double.MIN_VALUE ? range.min() : null;
            Double max = range != null && range.max() != Double.MAX_VALUE ? range.max() : null;
            HMParameterKind kind = HMParameterKind.of(isIn, isOut, uiHint);
            parameters.add(new Parameter(field, kind, dataType(field, uiHint, omsClass), //
                    fieldDescription != null ? clean(fieldDescription.value()) : "", //
                    unit != null && !unit.value().isBlank() ? unit.value().trim() : null, //
                    min, max, //
                    isIn ? defaultValue(field, instance) : null, //
                    choices(field, uiHint), //
                    extension(uiHint), //
                    isOut && !isIn));
        }
    }

    /**
     * Describe a module.
     *
     * @param moduleClass the class of the module.
     * @return the description.
     */
    public static ModuleDescriptor of( Class< ? extends HMModel> moduleClass ) {
        return new ModuleDescriptor(moduleClass);
    }

    /**
     * Check if a module is meant for the users, as in the Spatial Toolbox: not an Oms module 
     * (which works on data in memory and has a file based counterpart), not hidden and 
     * not one of the readers and writers used only inside other modules.
     *
     * @param moduleClass the class of the module.
     * @return <code>true</code> if the module is shown to the users.
     */
    public static boolean isUserModule( Class< ? extends HMModel> moduleClass ) {
        if (moduleClass.getSimpleName().startsWith("Oms") || moduleClass.getEnclosingClass() != null) {
            return false;
        }
        UI ui = moduleClass.getAnnotation(UI.class);
        if (ui != null && ui.value().contains(HMConstants.HIDE_UI_HINT)) {
            return false;
        }
        Label label = moduleClass.getAnnotation(Label.class);
        if (label != null && (label.value().isBlank() || INTERNAL_CATEGORIES.contains(label.value()))) {
            return false;
        }
        return true;
    }

    /**
     * Check if a module is available to the users, in the Spatial Toolbox, the command line and the QGIS plugin:
     * a {@link #isUserModule(Class) module for the users} whose outputs, if any, can be delivered 
     * (see {@link #hasDeliverableOutput()}).
     *
     * @param moduleClass the class of the module.
     * @return <code>true</code> if the module is available.
     */
    public static boolean isAvailable( Class< ? extends HMModel> moduleClass ) {
        return isUserModule(moduleClass) && of(moduleClass).hasDeliverableOutput();
    }

    /**
     * Get the {@link #isAvailable(Class) available modules}.
     * 
     * <p>If modules of different packages have the same name, the first in class name order is used.
     *
     * @return the module classes by name, sorted by name.
     */
    public static Map<String, Class< ? extends HMModel>> getAvailableModules() {
        Map<String, Class< ? extends HMModel>> name2Class = new TreeMap<>();
        for( Class< ? extends HMModel> modelClass : HMModelRegistry.getModelClasses() ) {
            if (!name2Class.containsKey(modelClass.getSimpleName()) && isAvailable(modelClass)) {
                name2Class.put(modelClass.getSimpleName(), modelClass);
            }
        }
        return name2Class;
    }

    /**
     * @return <code>false</code> if the module has outputs, but none the command line can deliver, 
     *         as data in memory. Modules without outputs, that write into an input folder or 
     *         database or only print, are fine.
     */
    public boolean hasDeliverableOutput() {
        boolean hasOutputs = false;
        for( Parameter parameter : parameters ) {
            if (parameter.kind != HMParameterKind.OUTPUT) {
                continue;
            }
            hasOutputs = true;
            if ((parameter.isPath() && !parameter.isComputed)
                    || (parameter.isComputed && isPrintable(parameter.field.getType()))) {
                return true;
            }
        }
        return !hasOutputs;
    }

    private static boolean isPrintable( Class< ? > type ) {
        if (type.isArray()) {
            return isPrintable(type.getComponentType());
        }
        return type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type) || type == Boolean.class;
    }

    public Class< ? extends HMModel> getModuleClass() {
        return moduleClass;
    }

    public String getName() {
        return moduleClass.getSimpleName();
    }

    /**
     * @return the folder of the module in the toolbox, ex. Raster Processing or Dem Manipulation.
     */
    public String getFolder() {
        return folder;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return the status, as experimental, draft, tested, validated or certified, or empty if not set.
     */
    public String getStatus() {
        return status;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * @return the names of the authors, in the order of the annotation.
     */
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * @return the parameters, in declaration order.
     */
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * @return the parameter with the given name, or <code>null</code>.
     */
    public Parameter getParameter( String name ) {
        for( Parameter parameter : parameters ) {
            if (parameter.name.equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    /**
     * @return the public fields of the class, from the topmost superclass down, each in declaration order.
     *         A field redeclared in a subclass hides the inherited one, keeping its position.
     */
    private static List<Field> getFields( Class< ? > moduleClass ) {
        List<Class< ? >> hierarchy = new ArrayList<>();
        for( Class< ? > c = moduleClass; c != null && c != Object.class; c = c.getSuperclass() ) {
            hierarchy.add(0, c);
        }
        Map<String, Field> fields = new LinkedHashMap<>();
        for( Class< ? > c : hierarchy ) {
            for( Field field : c.getDeclaredFields() ) {
                if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    fields.put(field.getName(), field);
                }
            }
        }
        return new ArrayList<>(fields.values());
    }

    private static DataType dataType( Field field, String uiHint, Class< ? > omsClass ) {
        if (uiHint.contains(HMConstants.FILEIN_UI_HINT_RASTER)) {
            return DataType.RASTER;
        } else if (uiHint.contains(HMConstants.FILEIN_UI_HINT_VECTOR)) {
            return DataType.VECTOR;
        } else if (uiHint.contains(HMConstants.FILEIN_UI_HINT_LAS)) {
            return DataType.LAS;
        } else if (uiHint.contains(HMConstants.FILEIN_UI_HINT_CSV)) {
            return DataType.CSV;
        } else if (uiHint.contains(HMConstants.FOLDEROUT_UI_HINT) || uiHint.contains(HMConstants.FOLDERIN_UI_HINT)) {
            return DataType.FOLDER;
        } else if (uiHint.contains(HMConstants.FILEOUT_UI_HINT)) {
            // the wrapped Oms module tells which kind of data is written
            if (omsClass != null) {
                try {
                    Class< ? > omsType = omsClass.getField(field.getName()).getType();
                    if (GridCoverage2D.class.isAssignableFrom(omsType)) {
                        return DataType.RASTER;
                    } else if (SimpleFeatureCollection.class.isAssignableFrom(omsType)) {
                        return DataType.VECTOR;
                    }
                } catch (NoSuchFieldException e) {
                    // not available
                }
            }
            return DataType.FILE;
        } else if (uiHint.contains(HMConstants.FILEIN_UI_HINT_GENERIC)) {
            return DataType.FILE;
        } else if (uiHint.contains(HMConstants.CRS_UI_HINT)) {
            return DataType.CRS;
        } else if (choices(field, uiHint).size() > 0) {
            return DataType.CHOICE;
        } else if (uiHint.contains(HMConstants.MULTILINE_UI_HINT)) {
            return DataType.TEXT;
        }
        Class< ? > type = field.getType();
        if (type == String.class) {
            return DataType.STRING;
        } else if (type == boolean.class || type == Boolean.class) {
            return DataType.BOOLEAN;
        } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return DataType.NUMBER;
        } else if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == short.class || type == Short.class) {
            return DataType.INTEGER;
        } else if (type.isArray() && (type.getComponentType().isPrimitive() || Number.class.isAssignableFrom(type.getComponentType()))
                && type.getComponentType() != boolean.class && type.getComponentType() != char.class) {
            return DataType.NUMBERS;
        }
        return DataType.OTHER;
    }

    /**
     * @return the allowed values of an enum field or of a combo UI hint (combo:value1,value2,...).
     */
    private static List<String> choices( Field field, String uiHint ) {
        if (field.getType().isEnum()) {
            List<String> values = new ArrayList<>();
            for( Object constant : field.getType().getEnumConstants() ) {
                values.add(((Enum< ? >) constant).name());
            }
            return Collections.unmodifiableList(values);
        }
        for( String hint : uiHint.split(";") ) {
            hint = hint.trim();
            if (hint.startsWith(HMConstants.COMBO_UI_HINT + ":")) {
                List<String> values = new ArrayList<>();
                for( String value : hint.substring(HMConstants.COMBO_UI_HINT.length() + 1).split(",") ) {
                    values.add(value.trim());
                }
                return Collections.unmodifiableList(values);
            }
        }
        return Collections.emptyList();
    }

    /**
     * @return the extension of the files of a specific file UI hint, as infile_gpkg, or <code>null</code>.
     */
    private static String extension( String uiHint ) {
        String[][] hint2Extension = {{HMConstants.FILEIN_UI_HINT_GPKG, "gpkg"}, {HMConstants.FILEIN_UI_HINT_CSV, "csv"},
                {HMConstants.FILEIN_UI_HINT_LAS, "las"}, {HMConstants.FILEIN_UI_HINT_JSON, "json"},
                {HMConstants.FILEIN_UI_HINT_DBF, "dbf"}, {HMConstants.FILEIN_UI_HINT_GPAP, "gpap"}};
        for( String[] pair : hint2Extension ) {
            if (uiHint.contains(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }

    private static Object defaultValue( Field field, Object instance ) {
        if (instance == null) {
            return null;
        }
        try {
            Object value = field.get(instance);
            if (value == null || (value.getClass().isArray() && Array.getLength(value) == 0)) {
                return null;
            }
            if (value instanceof String && ((String) value).isEmpty()) {
                return null;
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    private static String clean( String text ) {
        return text.replace("\r", "").replace("\n", " ").trim();
    }
}
