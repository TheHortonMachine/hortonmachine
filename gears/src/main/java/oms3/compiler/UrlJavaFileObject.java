/*
 * $Id: UrlJavaFileObject.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
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
