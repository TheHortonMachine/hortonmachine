/*
 * $Id: Files.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import oms3.ComponentException;

/**
 *
 * @author od
 */
public class Files {

    public static String readFully(String name) {
        StringBuilder b = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(new FileReader(name));
            String line;
            while ((line = r.readLine()) != null) {
                b.append(line).append('\n');
            }
            r.close();
        } catch (IOException E) {
            throw new ComponentException(E.getMessage());
        }
        return b.toString();
    }
}
