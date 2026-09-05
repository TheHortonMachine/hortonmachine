/*
 * $Id: NativeLibraries.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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
package oms3.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import oms3.annotations.DLL;

/**
 * Simplifies runtime binding of dynamic shared libraries to Java Interfaces.
 * 
 * @author od
 */
public class NativeLibraries {

    private NativeLibraries() {
    }

    /**
     * Maps a DLL to an interface using the @DLL annotation.
     *
     * @param <T> The interface type
     * @param intf  The interface class
     * @return the Library instance of type T
     *
     * @see oms3.annotations.DLL
     */
    public static <T extends Library> T bind(Class<T> intf) {
        DLL dll = intf.getAnnotation(DLL.class);
        if (dll == null) {
            throw new IllegalArgumentException("Missing DLL annotation.");
        }
        return bind(dll.value(), intf);
    }

    /**
     * Bind the Library 'name' to the interface 'intf', No annotation needed.
     * 
     * @param <T> generic type
     * @param name the library name, if not provided the Interface
     *             must be annotated with '@DLL'
     * @param intf the interface
     * @return The library interface bound to a DLL.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Library> T bind(String name, Class<T> intf) {

        if (!intf.isInterface()) {
            throw new IllegalArgumentException("Expected interface, but got '" + intf + "'.");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty library name.");
        }
        Native.setProtected(true);
        return (T) Native.loadLibrary(name, intf);
    }
}
