/*
 * $Id: MemorySourceJavaFileObject.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
