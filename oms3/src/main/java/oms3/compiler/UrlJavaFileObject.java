package oms3.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.tools.SimpleJavaFileObject;

/**
 * A Java file object that reads from a URL.
 * 
 * @author prunge
 */
public class UrlJavaFileObject extends SimpleJavaFileObject {

    URL url;
    String binaryName;

    /**
     * Constructs a <code>URLJavaFileObject</code>.
     *
     * @param name the file name.
     * @param url the URL of the file.
     * @param kind the kind of file.
     * @param binaryName the binary name of the file.
     *
     * @throws URISyntaxException if an error occurs converting <code>name</code>
     * 			to a URI.
     */
    public UrlJavaFileObject(String name, URL url, Kind kind, String binaryName)
            throws URISyntaxException {
        super(new URI(name), kind);
        this.url = url;
        this.binaryName = binaryName;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return url.openStream();
    }

    public String getBinaryName() {
        return binaryName;
    }
}
