package oms3.compiler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
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
    List<String> compilerOptions = new ArrayList<String>();
    //
    private static Compiler instance;

    public static synchronized Compiler singleton(URLClassLoader parent) {
        if (instance == null) {
            instance = new Compiler(parent);
        }
        return instance;
    }

    private Compiler(URLClassLoader parent) {
        //If not running on JDK, compiler will be null
        if (compiler == null) {
            throw new Error("Compiler not available.  This may happen if "
                    + "running on JRE instead of JDK.  Please use a full JDK 1.6."
                    + "javax.tools.ToolProvider.getSystemJavaCompiler() returned null.");
        }
        fileManager = new MemoryOutputJavaFileManager(compiler.getStandardFileManager(null, null, null));
        loader = new JavaFileManagerClassLoader(fileManager, parent);

        // create a classpath for the compiler
        StringBuilder b = new StringBuilder();
        URL[] cp = parent.getURLs();
        for (int i = 0; i < cp.length; i++) {
            b.append(File.pathSeparatorChar);
            b.append(cp[i].getFile());
        }
        // set compiler's classpath to be same as the runtime's
        compilerOptions.addAll(Arrays.asList("-cp", System.getProperty("java.class.path") + b.toString()));
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
            c = compileSource0(name, code);
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
     * Compiles multiple sources file and loads the classes.
     *
     * @param sourceFiles the source files to compile.
     * @param parentLoader the parent class loader to use when loading classes.
     *
     * @return a map of compiled classes.  This maps class names to
     * 			Class objects.
     * @throws Exception 
     *
     */
    private Class<?> compileSource0(String className, String sourceCode) throws Exception {
        List<MemorySourceJavaFileObject> compUnits = new ArrayList<MemorySourceJavaFileObject>(1);
        compUnits.add(new MemorySourceJavaFileObject(className + ".java", sourceCode));
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<JavaFileObject>();
        Boolean result = compiler.getTask(null, fileManager, diag, compilerOptions, null, compUnits).call();
        if (result.equals(Boolean.FALSE)) {
            throw new RuntimeException(diag.getDiagnostics().toString());
        }

        try {
            String classDotName = className.replace('/', '.');
            return Class.forName(classDotName, true, loader);
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }
}
