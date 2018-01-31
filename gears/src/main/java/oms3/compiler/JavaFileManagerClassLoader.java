package oms3.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.JavaFileObject.Kind;

/**
 * A class loader that loads classes generated from a Java file manager.
 * This can be used in conjunction with the compiler API to compile and run
 * classes on the fly.
 */
public class JavaFileManagerClassLoader extends ClassLoader {

    JavaFileManager fileManager;

    /**
     * Constructs a <code>ClassDataClassLoader</code>.
     *
     * @param fileManager the file manager to read classes from.
     * @param parent the parent classloader to delegate to if a class
     * 			is not found in the file manager.
     *
     * @throws NullPointerException if <code>fileManager</code>
     * 			is null.
     */
    public JavaFileManagerClassLoader(JavaFileManager fileManager, final ClassLoader parent) {
        super(parent);
        if (fileManager == null) {
            throw new NullPointerException("fileManager");
        }
        this.fileManager = fileManager;
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            JavaFileObject classFile = fileManager.getJavaFileForInput(StandardLocation.CLASS_OUTPUT, name, Kind.CLASS);
            if (classFile != null) {
                byte[] classData = readClassData(classFile);
                return defineClass(name, classData, 0, classData.length);
            } else {
                return super.findClass(name);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
    
    /**
     * Reads all class file data into a byte array from the given file
     * object.
     *
     * @param classFile the class file to read.
     * @return the class data.
     * @throws IOException if an I/O error occurs.
     */
    private byte[] readClassData(JavaFileObject classFile) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        InputStream classStream = classFile.openInputStream();
        int n = classStream.read(buf);
        while (n > 0) {
            bos.write(buf, 0, n);
            n = classStream.read(buf);
        }
        classStream.close();
        return bos.toByteArray();
    }
}
