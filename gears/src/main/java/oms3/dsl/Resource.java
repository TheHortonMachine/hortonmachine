/*
 * $Id: Resource.java fce87655d038 2015-01-05 odavid@colostate.edu $
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import oms3.ngmf.util.WildcardFileFilter;
import oms3.ComponentException;

/**
 *
 * @author od
 */
public class Resource {

    List<String> l = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    public void addResource(Object arg) {
        if (arg == null) {
            throw new ComponentException("No resource string  provided.");
        }
        if (arg.getClass() == String.class) {
            l.add((String) arg);
        } else if (arg instanceof Collection) {
            l.addAll((Collection) arg);
        } else {
            l.add(arg.toString());
        }
    }

    public List<String> getRecources() {
        List<String> f = new ArrayList<String>();
        for (String s : l) {
            File sf = new File(s);
            File parent = sf.getParentFile();
            if (!parent.exists()) {
                throw new IllegalArgumentException("Not found: " + parent);
            }
            File[] fi = parent.listFiles(new WildcardFileFilter(sf.getName()));
            for (File file : fi) {
               f.add(file.toString());
            }
        }
        return f;
    }

    public List<File> filterFiles(String ext) {
        List<File> f = new ArrayList<>();
        for (String s : l) {
            File sf = new File(s);
            File parent = sf.getParentFile();
            if (!parent.exists()) {
                throw new IllegalArgumentException("Not found: " + parent);
            }
            for (File file : parent.listFiles(new WildcardFileFilter(sf.getName()))) {
                //TODO put config logger here.
                if (file.getName().endsWith(ext)) {
                    f.add(file);
                }
            }
        }
        return f;
    }

    public List<File> filterDirectories() {
        List<File> f = new ArrayList<>();
        for (String s : l) {
            File sf = new File(s);
            if (sf.isDirectory() && sf.exists()) {
               f.add(sf);
            }
        }
        return f;
    }
}
