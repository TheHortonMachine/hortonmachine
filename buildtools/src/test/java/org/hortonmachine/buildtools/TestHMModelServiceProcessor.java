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
package org.hortonmachine.buildtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Compiles sample sources with the processor and checks the generated service file.
 *
 * <p>HMModel and Execute are stubbed with the same names, since buildtools doesn't depend on gears.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestHMModelServiceProcessor {

    private static final String MODEL = "org.hortonmachine.gears.libs.modules.HMModel";

    private static final String[][] SOURCES = {//
            {"org/hortonmachine/gears/libs/modules/HMModel.java", //
                    "package org.hortonmachine.gears.libs.modules; public class HMModel {}"},
            {"oms3/annotations/Execute.java", //
                    "package oms3.annotations; import java.lang.annotation.*; "
                            + "@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface Execute {}"},
            // a regular module
            {"test/GoodModule.java", //
                    "package test; import oms3.annotations.Execute; public class GoodModule extends " + MODEL
                            + " { @Execute public void process() {} }"},
            // inherits the execute method from an abstract base
            {"test/AbstractBase.java", //
                    "package test; import oms3.annotations.Execute; public abstract class AbstractBase extends " + MODEL
                            + " { @Execute public void process() {} }"},
            {"test/InheritingModule.java", //
                    "package test; public class InheritingModule extends AbstractBase {}"},
            // static nested module needs the binary name, inner classes can't be instantiated
            {"test/Outer.java", //
                    "package test; import oms3.annotations.Execute; public class Outer { "
                            + "public static class NestedModule extends " + MODEL + " { @Execute public void process() {} } "
                            + "public class InnerModule extends " + MODEL + " { @Execute public void process() {} } }"},
            // not modules
            {"test/NoExecute.java", //
                    "package test; public class NoExecute extends " + MODEL + " {}"},
            {"test/NotPublic.java", //
                    "package test; import oms3.annotations.Execute; class NotPublic extends " + MODEL
                            + " { @Execute public void process() {} }"},
            {"test/NoDefaultConstructor.java", //
                    "package test; import oms3.annotations.Execute; public class NoDefaultConstructor extends " + MODEL
                            + " { public NoDefaultConstructor( int a ) {} @Execute public void process() {} }"},
            {"test/NotAModel.java", //
                    "package test; import oms3.annotations.Execute; public class NotAModel { "
                            + "@Execute public void process() {} }"},//
    };

    private Path tmp;
    private Path srcDir;
    private Path outDir;
    private Path serviceFile;

    @Before
    public void setUp() throws IOException {
        tmp = Files.createTempDirectory("hm-processor-test");
        srcDir = tmp.resolve("src");
        outDir = tmp.resolve("classes");
        Files.createDirectories(outDir);
        serviceFile = outDir.resolve(HMModelServiceProcessor.SERVICE_FILE);
    }

    @After
    public void tearDown() throws IOException {
        try (Stream<Path> walk = Files.walk(tmp)) {
            for( Path p : walk.sorted(Collections.reverseOrder()).collect(Collectors.toList()) ) {
                Files.delete(p);
            }
        }
    }

    @Test
    public void testServiceFileContent() throws Exception {
        compile(writeSources(SOURCES));

        assertEquals(Arrays.asList("test.GoodModule", "test.InheritingModule", "test.Outer$NestedModule"),
                Files.readAllLines(serviceFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testIncrementalCompilationMergesEntries() throws Exception {
        compile(writeSources(SOURCES));
        // a stale entry of a class that doesn't exist anymore
        Files.write(serviceFile, "test.RemovedModule\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

        // compile only a changed module (now not a module anymore) and a new one, as IDEs do
        List<File> changed = writeSources(new String[][]{//
                {"test/LaterModule.java", //
                        "package test; import oms3.annotations.Execute; public class LaterModule extends " + MODEL
                                + " { @Execute public void process() {} }"},
                {"test/GoodModule.java", //
                        "package test; public abstract class GoodModule extends " + MODEL + " {}"}//
        });
        compile(changed);

        assertEquals(Arrays.asList("test.InheritingModule", "test.LaterModule", "test.Outer$NestedModule"),
                Files.readAllLines(serviceFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testNoServiceFileWithoutModels() throws Exception {
        compile(writeSources(new String[][]{{"test/Plain.java", "package test; public class Plain {}"}}));

        assertFalse(Files.exists(serviceFile));
    }

    private List<File> writeSources( String[][] sources ) throws IOException {
        List<File> files = new ArrayList<>();
        for( String[] source : sources ) {
            Path file = srcDir.resolve(source[0]);
            Files.createDirectories(file.getParent());
            Files.write(file, source[1].getBytes(StandardCharsets.UTF_8));
            files.add(file.toFile());
        }
        return files;
    }

    /**
     * Compile into the output folder, which is also on the classpath, as in incremental builds.
     */
    private void compile( List<File> sourceFiles ) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT,
                StandardCharsets.UTF_8)) {
            List<String> options = Arrays.asList("-d", outDir.toString(), "-classpath", outDir.toString());
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles));
            task.setProcessors(Arrays.asList(new HMModelServiceProcessor()));
            assertTrue(diagnostics.getDiagnostics().toString(), task.call());
        }
    }
}
