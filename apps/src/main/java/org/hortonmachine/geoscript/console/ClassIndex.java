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

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.hortonmachine.dbs.log.Logger;

/**
 * An index of the class names available on the classpath and in the jdk, used by the code completion.
 *
 * <p>
 * The index is built in a background thread: until it is ready, queries simply return less.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ClassIndex {

    /** simple name -> fully qualified names. */
    private final NavigableMap<String, Set<String>> simpleToFqn = new ConcurrentSkipListMap<>();
    /** package -> simple names. */
    private final NavigableMap<String, Set<String>> packageToSimple = new ConcurrentSkipListMap<>();
    private volatile boolean ready = false;

    /**
     * Start indexing in a daemon thread.
     */
    public void buildAsync() {
        Thread thread = new Thread(this::build, "hm-console-class-index");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Index the classpath and the jdk.
     */
    public void build() {
        String classPath = System.getProperty("java.class.path", "");
        for( String entry : classPath.split(File.pathSeparator) ) {
            if (entry.isBlank()) {
                continue;
            }
            File file = new File(entry);
            try {
                if (file.isDirectory()) {
                    indexFolder(file.toPath());
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    indexJar(file);
                }
            } catch (Exception e) {
                // a broken entry should not stop the indexing
            }
        }
        indexJdk();
        ready = true;
    }

    private void indexFolder( Path folder ) throws Exception {
        try (Stream<Path> files = Files.walk(folder)) {
            files.filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> addClassFile(folder.relativize(p).toString().replace(File.separatorChar, '/')));
        }
    }

    private void indexJar( File jar ) throws Exception {
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration< ? extends ZipEntry> entries = zip.entries();
            while( entries.hasMoreElements() ) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class") && !name.startsWith("META-INF")) {
                    addClassFile(name);
                }
            }
        }
    }

    private void indexJdk() {
        try {
            FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
            Path modules = jrt.getPath("/modules");
            try (Stream<Path> moduleFolders = Files.list(modules)) {
                for( Path moduleFolder : (Iterable<Path>) moduleFolders::iterator ) {
                    try (Stream<Path> files = Files.walk(moduleFolder)) {
                        files.map(p -> moduleFolder.relativize(p).toString())
                                .filter(n -> n.endsWith(".class") && (n.startsWith("java/") || n.startsWith("javax/")))
                                .forEach(this::addClassFile);
                    }
                }
            }
        } catch (Exception e) {
            Logger.INSTANCE.insertWarning("", "Unable to index the jdk classes: " + e.getMessage());
        }
    }

    private void addClassFile( String path ) {
        String name = path.substring(0, path.length() - ".class".length());
        if (name.contains("$") || name.endsWith("module-info") || name.endsWith("package-info")) {
            return;
        }
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash < 0) {
            return;
        }
        String simpleName = name.substring(lastSlash + 1);
        if (simpleName.isEmpty() || !Character.isUpperCase(simpleName.charAt(0))) {
            return;
        }
        String packageName = name.substring(0, lastSlash).replace('/', '.');
        String fqn = packageName + "." + simpleName;
        simpleToFqn.computeIfAbsent(simpleName, k -> new ConcurrentSkipListSet<>()).add(fqn);
        packageToSimple.computeIfAbsent(packageName, k -> new ConcurrentSkipListSet<>()).add(simpleName);
    }

    /**
     * @return <code>true</code> once the index is complete.
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * @param prefix the start of the simple class name (case sensitive).
     * @return the simple names starting with the prefix and their fully qualified names.
     */
    public Map<String, Set<String>> getClassesStartingWith( String prefix ) {
        return simpleToFqn.subMap(prefix, true, prefix + Character.MAX_VALUE, false);
    }

    /**
     * @param simpleName the simple class name.
     * @return the fully qualified names of the classes with that simple name.
     */
    public Set<String> getFqns( String simpleName ) {
        Set<String> fqns = simpleToFqn.get(simpleName);
        return fqns != null ? fqns : Collections.emptySet();
    }

    /**
     * @param packageName the package.
     * @return the simple names of the classes in the package.
     */
    public Set<String> getClassesInPackage( String packageName ) {
        Set<String> classes = packageToSimple.get(packageName);
        return classes != null ? classes : Collections.emptySet();
    }

    /**
     * @param packageName the package.
     * @return <code>true</code> if the package, or one of its subpackages, contains classes.
     */
    public boolean isPackage( String packageName ) {
        String key = packageToSimple.ceilingKey(packageName);
        return key != null && (key.equals(packageName) || key.startsWith(packageName + "."));
    }

    /**
     * @param parentPackage the parent package, or the empty string for the top level packages.
     * @return the names (last part only) of the direct subpackages.
     */
    public List<String> getSubPackages( String parentPackage ) {
        String prefix = parentPackage.isEmpty() ? "" : parentPackage + ".";
        NavigableSet<String> names = new ConcurrentSkipListSet<>();
        for( String pkg : packageToSimple.subMap(prefix, true, prefix + Character.MAX_VALUE, false).keySet() ) {
            String rest = pkg.substring(prefix.length());
            int dot = rest.indexOf('.');
            names.add(dot < 0 ? rest : rest.substring(0, dot));
        }
        return new ArrayList<>(names);
    }
}
