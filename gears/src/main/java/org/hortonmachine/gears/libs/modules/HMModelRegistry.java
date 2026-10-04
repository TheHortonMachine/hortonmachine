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
package org.hortonmachine.gears.libs.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hortonmachine.dbs.log.Logger;

/**
 * Registry of the available {@link HMModel} modules.
 *
 * <p>Modules are discovered through the SPI service files
 * <code>META-INF/services/org.hortonmachine.gears.libs.modules.HMModel</code>
 * found on the classpath. Any jar that contains such a file has its modules
 * picked up. The HortonMachine jars get their service files generated at compile
 * time by the <code>hm-buildtools</code> annotation processor, which external
 * projects can use as well.
 *
 * <p>Classes are loaded but not initialized nor instantiated, so listing the modules is cheap.
 * Entries that can't be loaded are logged and skipped, so a broken jar doesn't
 * hide the other modules.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class HMModelRegistry {

    public static final String SERVICE_FILE = "META-INF/services/" + HMModel.class.getName();

    private static List<Class< ? extends HMModel>> modelClasses;

    private HMModelRegistry() {
    }

    /**
     * Get all the available module classes, sorted by full class name.
     *
     * @return the unmodifiable list of module classes.
     */
    public static synchronized List<Class< ? extends HMModel>> getModelClasses() {
        if (modelClasses == null) {
            modelClasses = Collections.unmodifiableList(loadModelClasses(getClassLoader()));
        }
        return modelClasses;
    }

    /**
     * Get the module classes mapped by their simple name.
     *
     * <p>If different packages contain modules with the same simple name,
     * the first one in class name order is kept.
     *
     * @return the map of simple names to module classes.
     */
    public static Map<String, Class< ? extends HMModel>> getModelClassesBySimpleName() {
        Map<String, Class< ? extends HMModel>> map = new LinkedHashMap<>();
        for( Class< ? extends HMModel> modelClass : getModelClasses() ) {
            map.putIfAbsent(modelClass.getSimpleName(), modelClass);
        }
        return map;
    }

    /**
     * Get a module class by its simple name.
     *
     * @param simpleName the simple class name, ex. <code>OmsPitfiller</code>.
     * @return the module class or <code>null</code>, if none is available.
     */
    public static Class< ? extends HMModel> getModelClass( String simpleName ) {
        for( Class< ? extends HMModel> modelClass : getModelClasses() ) {
            if (modelClass.getSimpleName().equals(simpleName)) {
                return modelClass;
            }
        }
        return null;
    }

    /**
     * Load the module classes listed in the service files visible to a classloader.
     *
     * <p>This doesn't use the cache and is meant for cases in which the classpath changes.
     *
     * @param classLoader the classloader to use.
     * @return the list of module classes, sorted by full class name.
     */
    public static List<Class< ? extends HMModel>> loadModelClasses( ClassLoader classLoader ) {
        Set<String> classNames = new LinkedHashSet<>();
        try {
            Enumeration<URL> serviceFiles = classLoader.getResources(SERVICE_FILE);
            while( serviceFiles.hasMoreElements() ) {
                URL serviceFile = serviceFiles.nextElement();
                try {
                    classNames.addAll(readServiceFile(serviceFile));
                } catch (IOException e) {
                    Logger.INSTANCE.insertError("HMModelRegistry", "Unable to read " + serviceFile, e);
                }
            }
        } catch (IOException e) {
            Logger.INSTANCE.insertError("HMModelRegistry", "Unable to list the module service files", e);
        }

        List<String> sortedNames = new ArrayList<>(classNames);
        Collections.sort(sortedNames);
        List<Class< ? extends HMModel>> classes = new ArrayList<>();
        for( String className : sortedNames ) {
            try {
                Class< ? > clazz = Class.forName(className, false, classLoader);
                if (HMModel.class.isAssignableFrom(clazz)) {
                    classes.add(clazz.asSubclass(HMModel.class));
                } else {
                    Logger.INSTANCE.insertWarning("HMModelRegistry",
                            "Ignoring " + className + ", which is not a subclass of " + HMModel.class.getName());
                }
            } catch (Throwable e) {
                Logger.INSTANCE.insertError("HMModelRegistry", "Unable to load module " + className, e);
            }
        }
        return classes;
    }

    /**
     * Read the class names from a service file, ignoring comments and blank lines.
     */
    static List<String> readServiceFile( URL serviceFile ) throws IOException {
        List<String> names = new ArrayList<>();
        try (InputStream in = serviceFile.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while( (line = reader.readLine()) != null ) {
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex);
                }
                line = line.trim();
                if (!line.isEmpty()) {
                    names.add(line);
                }
            }
        }
        return names;
    }

    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = HMModelRegistry.class.getClassLoader();
        }
        return classLoader;
    }
}
