package oms3.compiler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * 
 */
public final class Compiler {

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    MemoryOutputJavaFileManager fileManager;
    ClassLoader loader;
    Map<String, Class<?>> cache = new HashMap<String, Class<?>>();
    //
    private static Compiler instance;

    public static synchronized Compiler singleton() {
        if (instance == null) {
            instance = new Compiler();
        }
        return instance;
    }

    private Compiler() {
        //If not running on JDK, compiler will be null
        if (compiler == null) {
            throw new Error("Compiler not available.  This may happen if " +
                    "running on JRE instead of JDK.  Please use a full JDK 1.6." +
                    "javax.tools.ToolProvider.getSystemJavaCompiler() returned null.");
        }
        fileManager = new MemoryOutputJavaFileManager(compiler.getStandardFileManager(null, null, null));
//        try {
//            fileManager.addClassPathUrl(new URL("file:/od/projects/ngmf.all/lib/oms-all.jar"));
//        } catch (MalformedURLException ex) {
//            throw new RuntimeException(ex);
//        }
        for (URL url : getClassPathUrls()) {
            fileManager.addClassPathUrl(url);
        }
        loader = new JavaFileManagerClassLoader(fileManager, getDefaultClassLoader());
    }

    /**
     * Compiles a single source file and loads the class with a
     * default class loader.  The default class loader is the one used
     * to load the test case class.
     *
     * @param name the name of the class to compile.
     * @param code the source code of the class.
     *
     * @return the compiled class.
     */
    public synchronized Class<?> compileSource(String name, String code) throws Exception {
        Class<?> c = cache.get(name);
        if (c == null) {
            c = compileSources(name, code);
            cache.put(name, c);
        }
        return c;
    }

    public synchronized Class<?> getCompiledClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException ex) {
                return cache.get(name);
            }
        }
    }

    /**
     * Returns the default classloader to use when compiling sources
     * and no classloader is explicitly defined.  Defaults to the
     * thread context classloader.
     *
     * @return a classloader.
     */
    ClassLoader getDefaultClassLoader() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = getClass().getClassLoader();
        }
        return parent;
    }

    /**
     * Compiles multiple sources file and loads the classes.
     *
     * @param sourceFiles the source files to compile.
     * @param parentLoader the parent classloader to use when loading classes.
     *
     * @return a map of compiled classes.  This maps class names to
     * 			Class objects.
     * @throws Exception 
     *
     */
    Class<?> compileSources(String className, String sourceCode) throws Exception {
        List<MemorySourceJavaFileObject> compUnits = new ArrayList<MemorySourceJavaFileObject>(1);
        compUnits.add(new MemorySourceJavaFileObject(className + ".java", sourceCode));

        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<JavaFileObject>();
        Boolean result = compiler.getTask(null, fileManager, diag, null, null, compUnits).call();
        if (!Boolean.TRUE.equals(result)) {
            throw new RuntimeException(diag.getDiagnostics().toString());
        }

        try {
            String classDotName = className.replace('/', '.');
            Class<?> clazz = Class.forName(classDotName, true, loader);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }

    /**
     * Returns a list of base URLs to add to the classpath when
     * compiling.
     * <p>
     *
     * This method returns an empty list.  Subclasses may override to use
     * a different classpath.
     *
     * @return a list of classpath URLs.
     */
    protected List<URL> getClassPathUrls() {
        return Collections.emptyList();
    }
}
