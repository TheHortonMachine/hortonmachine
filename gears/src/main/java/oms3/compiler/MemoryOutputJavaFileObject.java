package oms3.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * A file object that retains contents in memory and does not write
 * out to disk.
 * 
 * @author prunge
 */
public class MemoryOutputJavaFileObject extends SimpleJavaFileObject {

    private ByteArrayOutputStream outputStream;

    /**
     * Constructs a <code>MemoryOutputJavaFileObject</code>.
     *
     * @param uri the URI of the output file.
     * @param kind the file type.
     */
    public MemoryOutputJavaFileObject(URI uri, Kind kind) {
        super(uri, kind);
    }

    /**
     * Opens an output stream to write to the file.  This writes to
     * memory.  This clears any existing output in the file.
     *
     * @return an output stream.
     * 
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public OutputStream openOutputStream() throws IOException {
        outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    /**
     * Opens an input stream to the file data.  If the file has never
     * been written the input stream will contain no data (i.e. length=0).
     *
     * @return an input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public InputStream openInputStream() throws IOException {
        if (outputStream != null) {
            return new ByteArrayInputStream(outputStream.toByteArray());
        } else {
            return new ByteArrayInputStream(new byte[0]);
        }
    }
}
