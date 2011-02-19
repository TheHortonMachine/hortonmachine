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
     * @param fileManager the file manager to read classes from.
     * @throws NullPointerException if <code>fileManager</code>
     * 			is null.
     */
    public JavaFileManagerClassLoader(JavaFileManager fileManager) {
        super();
        if (fileManager == null) {
            throw new NullPointerException("fileManager == null");
        }
        this.fileManager = fileManager;
    }

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
//            String tmpDir = System.getProperty("java.io.tmpdir");
//            File cacheFolder = new File(tmpDir, "cache"); //new File("/tmp/cache")
//            cacheFolder.mkdir();
//            File cFile = new File(cacheFolder, name + ".class");
//            if (cFile.exists()) {
//                byte[] classData = read(cFile);
//                return defineClass(name, classData, 0, classData.length);
//            }
            JavaFileObject classFile = fileManager.getJavaFileForInput(StandardLocation.CLASS_OUTPUT, name, Kind.CLASS);
            if (classFile != null) {
                byte[] classData = readClassData(classFile);
//                write(cFile, classData);
                return defineClass(name, classData, 0, classData.length);
            } else {
                return super.findClass(name);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
    
//    private void write(File file, byte[] content) throws IOException {
//        ByteBuffer buf = ByteBuffer.wrap(content);
//        WritableByteChannel channel = new FileOutputStream(file).getChannel();
//        channel.write(buf);
//        channel.close();
//    }
//
//    private byte[] read(File file) throws IOException {
//        ReadableByteChannel channel = new FileInputStream(file).getChannel();
//        ByteBuffer buf = ByteBuffer.allocate((int) file.length());
//        channel.read(buf);
//        channel.close();
//        return buf.array();
//    }

    /**
     * Reads all class file data into a byte array from the given file
     * object.
     *
     * @param classFile the class file to read.
     * @return the class data.
     * @throws IOException if an I/O error occurs.
     */
    private byte[] readClassData(final JavaFileObject classFile) throws IOException {
        InputStream classStream = classFile.openInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n = classStream.read(buf);
        while (n > 0) {
            bos.write(buf, 0, n);
            n = classStream.read(buf);
        }
        return (bos.toByteArray());
    }
}
