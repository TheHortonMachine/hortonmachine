/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
//            if (!file.exists()) {
//                throw new IllegalArgumentException(f);
//            }
        }
        return file;
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static void main(String[] args) {
        System.out.println(resolve("C:\\od\\projects\\oms3.prj.prms2008\\output\\PRMS2008\\%previous\\out.csv"));
    }
}
