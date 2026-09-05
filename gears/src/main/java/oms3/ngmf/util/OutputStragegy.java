/*
 * $Id: OutputStragegy.java 2fb4d513ea17 2014-06-04 odavid@colostate.edu $
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
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author od
 */
public abstract class OutputStragegy {

    File basedir;

    OutputStragegy(File basedir) {
        this.basedir = basedir;
    }

    public abstract File nextOutputFolder();

    public File lastOutputFolder() {
        return resolve(basedir.toString() + "/%last");
    }

    public File firstOutputFolder() {
        return resolve(basedir.toString() + "/%first");
    }

    public File previousOutputFolder() {
        return resolve(basedir.toString() + "/%previous");
    }

    public File baseFolder() {
        return basedir;
    }

    static public File resolve(File f) {
        return resolve(f.toString());
    }
    
    static public File resolve(String f) {
        if (!f.contains("%")) {
            return new File(f);
        }
        String fi = f.replace('\\', '/');
        String[] d = fi.split("/");
        File file = new File(d[0]);
        for (int i = 1; i < d.length; i++) {
            fi = d[i];
            if (fi.startsWith("%")) {
                if (!file.exists() || !file.isDirectory()) {
                    throw new IllegalArgumentException("Invalid directory: " + f + " (" + file + ")");
                }
                File[] files = file.listFiles();
                Arrays.sort(files, new Comparator<File>() {

                    @Override
                    public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                if (fi.equalsIgnoreCase("%last")) {
                    fi = files[files.length - 1].getName();
                } else if (fi.equalsIgnoreCase("%previous")) {
                    fi = files[files.length - 2].getName();
                } else if (fi.equalsIgnoreCase("%first")) {
                    fi = files[0].getName();
                } else {
                    throw new IllegalArgumentException(f + " Invalid: " + fi);
                }
            }
            file = new File(file, fi);
        }
        return file;
    }

    static public void deleteDirectory(File path, boolean keepPath) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i], false);
                } else {
                    files[i].delete();
                }
            }
        }
        if (!keepPath) {
            path.delete();
        }
    }
    
    
    static void ensureEmptyIfExist(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            deleteDirectory(dir, false);
        }
    }

    public static void main(String[] args) {
        System.out.println(resolve("C:\\od\\projects\\oms3.prj.prms2008\\output\\PRMS2008\\%previous\\out.csv"));
    }
}
