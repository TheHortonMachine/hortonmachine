/*
 * $Id: FlatDirectoryOutput.java e6d7b0143a21 2015-06-08 odavid@colostate.edu $
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
package oms3.ngmf.util;

import java.io.File;

/**
 * This is for server side execution of a model.
 * @author od
 */
public class FlatDirectoryOutput extends OutputStragegy {

    public FlatDirectoryOutput(File base) {
        super(base);
    }

    @Override
    public File nextOutputFolder() {
        File f = basedir;
        ensureEmptyIfExist(f);
        return f;
    }


    @Override
    public File lastOutputFolder() {
        return basedir;
    }


    @Override
    public File firstOutputFolder() {
        return basedir;
    }


    @Override
    public File previousOutputFolder() {
        return basedir;
    }
}
