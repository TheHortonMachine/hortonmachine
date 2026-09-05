/*
 * $Id: OutputDescriptor.java 610927885082 2015-05-29 odavid@colostate.edu $
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

import oms3.ComponentException;
import java.io.File;
import oms3.ngmf.util.DateDirectoryOutput;
import oms3.ngmf.util.FlatDirectoryOutput;
import oms3.ngmf.util.NumDirectoryOutput;
import oms3.ngmf.util.OutputStragegy;
import oms3.ngmf.util.SimpleDirectoryOutput;

import static oms3.SimBuilder.*;

/**
 *
 * @author od
 */
public class OutputDescriptor extends AbstractBuildableLeaf {

    int scheme = SIMPLE;
    File dir;

    public OutputDescriptor() {
        dir = new File(System.getProperty("user.dir"), "output");
        if (!dir.exists()) {
            dir = new File(System.getProperty("user.dir"));
        }
    }

    public void setDir(String d) {
        dir = new File(d);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory " + dir);
        }
    }

    File getDir() {
        return dir;
    }

    public void setScheme(int scheme) {
        if ((scheme == SIMPLE) || (scheme == NUMBERED) || (scheme == TIME)) {
            this.scheme = scheme;
        } else {
            throw new IllegalArgumentException("Invalid output strategy scheme.");
        }
    }

    public OutputStragegy getOutputStrategy(String simName) {
        if (Boolean.getBoolean("oms.csip.server")) {
            return new FlatDirectoryOutput(dir);
        }
        if (scheme == SIMPLE) {
            return  new SimpleDirectoryOutput(getDir(), simName);
        } else if (scheme == NUMBERED) {
            return new NumDirectoryOutput(getDir(), simName);
        } else if (scheme == TIME) {
            return new DateDirectoryOutput(getDir());
        }
        return null;
    }
}
