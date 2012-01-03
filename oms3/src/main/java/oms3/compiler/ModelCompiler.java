/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.compiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import oms3.ComponentException;
import oms3.util.Processes;

/**
 *
 * @author od
 */
public abstract class ModelCompiler {

    public abstract Class<?> compile(Logger log, URLClassLoader loader, String name, String src) throws Exception;

    public static ModelCompiler create(String sysprop) {
        if ("javac".equals(sysprop)) {
            return new Javac();
        } else if ("external".equals(sysprop)) {
            return new External();
        } else {
            return new Memory();
        }
    }

    private static void write(File file, String s) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(s);
        out.close();
    }

    private static class External extends ModelCompiler {

        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

        @Override
        public Class<?> compile(Logger log, URLClassLoader loader, String name, String src) throws Exception {
            log.fine("Expernal compiler");
            File classDir = new File(System.getProperty("oms.prj") + File.separatorChar + "dist");
            File srcDir = new File(System.getProperty("java.io.tmpdir"));

            File javaFile = new File(srcDir, name + ".java");
            write(javaFile, src);

            StandardJavaFileManager fm = jc.getStandardFileManager(null, null, null);

            Iterable fileObjects = fm.getJavaFileObjects(javaFile);
            String[] options = new String[]{"-d", classDir.toString()};
            jc.getTask(null, null, null, Arrays.asList(options), null, fileObjects).call();

            fm.close();
            
            return loader.loadClass(name);
        }
    }

    private static class Memory extends ModelCompiler {

        @Override
        public Class<?> compile(Logger log, URLClassLoader loader, String name, String src) throws Exception {
            log.info("Memory compiler");
            oms3.compiler.Compiler tc = oms3.compiler.Compiler.singleton(loader);
            Class jc = tc.compileSource(name, src);
            return jc;
        }
    }

    private static class Javac extends ModelCompiler {
        // javac -cp "/home/od/.oms/3.1rc6/oms-all.jar:/od/projects/oms_examples/oms3.prj.csm/dist/csm.jar" /tmp/Comp_f61514ea_4e12_431c_a26b_b6b016c273df.java -d /tmp/javafiles

        @Override
        public Class<?> compile(final Logger log, URLClassLoader loader, String name, String src) throws Exception {
            log.info("Javac compiler");
            File classDir = new File(System.getProperty("oms.prj") + File.separatorChar + "dist");
            File srcDir = new File(System.getProperty("java.io.tmpdir"));
            File javaFile = new File(srcDir, name + ".java");

            write(javaFile, src);

            Processes p = new Processes(new File("javac"));
            
            p.setArguments("-cp", System.getProperty("java.class.path"), javaFile.toString(), "-d", classDir.toString());

            p.redirectOutput(new Writer() {

                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine(new String(cbuf, off, len));
                    }
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                }
            });
            //
            p.redirectError(new Writer() {

                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    if (log.isLoggable(Level.SEVERE)) {
                        log.severe(new String(cbuf, off, len));
                    }
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                }
            });

            int exitValue = p.exec();
            if (exitValue != 0) {
                throw new ComponentException("Commpile failed for " + javaFile);
            }

            if (log.isLoggable(Level.INFO)) {
                log.info("succesfully compiled -> " + javaFile + " to " + classDir);
            }

            return loader.loadClass(name);
        }
    }
}
