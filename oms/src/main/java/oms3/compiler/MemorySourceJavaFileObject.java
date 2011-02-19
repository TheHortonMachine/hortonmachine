package oms3.compiler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.tools.SimpleJavaFileObject;

/**
 * A Java source file that exists in memory.
 * 
 * @author prunge
 */
public class MemorySourceJavaFileObject extends SimpleJavaFileObject {

    String code;

    /**
     * Constructs a <code>MemoryJavaFileObject</code>.
     *
     * @param name the name of the source file.
     * @param code the source code.
     *
     * @throws IllegalArgumentException if <code>name</code> is not valid.
     * @throws NullPointerException if any parameter is null.
     */
    public MemorySourceJavaFileObject(String name, String code) {
        super(createUriFromName(name), Kind.SOURCE);
        if (code == null) {
            throw new NullPointerException("code");
        }
        this.code = code;
    }

    /**
     * Creates a URI from a source file name.
     * @param name the source file name.
     * @return the URI.
     * @throws NullPointerException if <code>name</code>
     * 			is null.
     * @throws IllegalArgumentException if <code>name</code>
     * 			is invalid.
     */
    private static URI createUriFromName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        try {
            return new URI(name);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid name: " + name, e);
        }
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncErrors) throws IOException {
        return code;
    }
}
