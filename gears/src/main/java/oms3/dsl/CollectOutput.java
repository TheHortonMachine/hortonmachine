/*
 * $Id$
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
package oms3.dsl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import oms3.ComponentException;

/** Model Logging configuration.
 *
 * @author od
 */
public class CollectOutput extends AbstractBuildableLeaf {

    String file;
    String to;
    boolean append = true;
    
    public void setFile(String file) {
        this.file = file;
    }

    public void setTo(String to) {
        this.to = to;
    }
    
    public void setAppend(boolean app) {
        this.append = app;
    }
    
    public void collect(String separator) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            FileWriter w = new FileWriter(to, append);
            char[] b = new char[4096];
            int len;
            w.write('\n');
            if (separator != null) {
                w.write(separator);
                w.write('\n');
            }
            while ((len = r.read(b)) != -1) {
                w.write(b, 0, len);
            }
            r.close();
            w.flush();
            w.close();
        } catch (IOException E) {
            throw new ComponentException(E.getMessage());
        }
    }
}