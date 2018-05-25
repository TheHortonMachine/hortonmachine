package oms3.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.JavaFileObject.Kind;

/**
 * A java file manager that stores output in memory, delegating all other
 * functions to another file manager.
 * 
 */
public class MemoryOutputJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    /**
     * Maps class names to file objects.
     */
    Map<String, MemoryOutputJavaFileObject> outputMap = new HashMap<String, MemoryOutputJavaFileObject>();
    List<URL> classPathUrls = new ArrayList<URL>();

    /**
     * Constructs a <code>MemoryOutputJavaFileManager</code>.
     *
     * @param fileManager the underlying file manager to use.
     */
    public MemoryOutputJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    /**
     * Adds a URL that classes may be loaded from.  All classes from this
     * URL will be added to the classpath.
     *
     * @param url the URL to add.
     *
     * @throws NullPointerException if <code>url</code> is null.
     */
    public void addClassPathUrl(URL url) {
        if (url == null) {
            throw new NullPointerException("url == null");
        }
        classPathUrls.add(url);
    }

    /**
     * Returns the base URL of the specified class.
     * <p>
     *
     * For example, if <code>java.lang.String</code> exists at
     * http://base.net/parent/java/lang/String.class, the base URL
     * is http://base.net/parent/.
     *
     * @param clazz the class.
     *
     * @return a base URL where the class is located.
     *
     * @throws IllegalArgumentException if a URL cannot be obtained.
     */
    public static URL baseUrlOfClass(Class<?> clazz) {
        try {
            String name = clazz.getName();
            URL url = clazz.getResource("/" + name.replace('.', '/') + ".class");
            int curPos = 0;
            do {
                curPos = name.indexOf('.', curPos + 1);
                if (curPos >= 0) {
                    url = new URL(url, "..");
                }
            } while (curPos >= 0);
            return url;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL for class " + clazz.getName(), e);
        }
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling)
            throws IOException {
        if (kind != Kind.CLASS) {
            throw new IOException("Only class output supported, kind=" + kind);
        }
        try {
            MemoryOutputJavaFileObject output = new MemoryOutputJavaFileObject(new URI(className), kind);
            outputMap.put(className, output);
            return output;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location,
            String className, Kind kind) throws IOException {
        JavaFileObject result;
        if (StandardLocation.CLASS_OUTPUT == location && Kind.CLASS == kind) {
            result = outputMap.get(className);
            if (result == null) {
                result = super.getJavaFileForInput(location, className, kind);
            }
        } else {
            result = super.getJavaFileForInput(location, className, kind);
        }
        return result;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof UrlJavaFileObject) {
            UrlJavaFileObject urlFile = (UrlJavaFileObject) file;
            return urlFile.getBinaryName();
        } else {
            return super.inferBinaryName(location, file);
        }
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
            Set<Kind> kinds, boolean recurse) throws IOException {
        //Special handling for Privateer classes when building with Maven
        //Maven does not set the classpath but instead uses a custom
        //classloader to load test classes which means the compiler
        //tool cannot normally see standard Privateer classes so
        //we put in a workaround here
        if (StandardLocation.CLASS_PATH == location && kinds.contains(Kind.CLASS)) {
            List<JavaFileObject> results = new ArrayList<JavaFileObject>();
            Iterable<JavaFileObject> superResults = super.list(location, packageName, kinds, recurse);
            for (JavaFileObject superResult : superResults) {
                results.add(superResult);
            }
            //Now process classpath URLs
            for (URL curClassPathUrl : classPathUrls) {
                String directory = packageName.replace('.', '/') + '/';
                URL loadUrl = new URL(curClassPathUrl, directory);
                try {
                    List<JavaFileObject> additionalClasses = listClassesFromUrl(loadUrl, packageName);
                    results.addAll(additionalClasses);
                } catch (IOException e) {
                    //This happens if the file does not exist
                    //Move onto next one
                }
            }
            return results;
        } else {
            Iterable<JavaFileObject> results = super.list(location, packageName, kinds, recurse);
            return results;
        }
    }

    /**
     * Lists all files at a specified URL.
     *
     * @param base the URL.
     * @param packageName the package name of classes to list.
     *
     * @return a list of class files.
     *
     * @throws IOException if an I/O error occurs.
     */
    protected List<JavaFileObject> listClassesFromUrl(URL base, String packageName) throws IOException {
        //TODO this will only work with file:// not jar://

        if (base == null) {
            throw new NullPointerException("base == null");
        }

        List<JavaFileObject> list = new ArrayList<JavaFileObject>();

        URLConnection connection = base.openConnection();
        connection.connect();
        String encoding = connection.getContentEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
        try {
            String curLine;
            do {
                curLine = reader.readLine();
                if (curLine != null && curLine.endsWith(".class")) {
                    try {
                        String curSimpleName = curLine.substring(0, curLine.length() - ".class".length());
                        String binaryName;
                        if (packageName == null) {
                            binaryName = curSimpleName;
                        } else {
                            binaryName = packageName + "." + curSimpleName;
                        }
                        list.add(new UrlJavaFileObject(curLine, new URL(base, curLine), Kind.CLASS, binaryName));
                    } catch (URISyntaxException e) {
                        throw new IOException("Error parsing URL " + curLine + ".", e);
                    }
                }
            } while (curLine != null);
        } finally {
            reader.close();
        }
        return list;
    }
}
