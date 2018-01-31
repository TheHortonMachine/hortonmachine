/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        List<File> f = new ArrayList<File>();
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
        List<File> f = new ArrayList<File>();
        for (String s : l) {
            File sf = new File(s);
            if (sf.isDirectory() && sf.exists()) {
               f.add(sf);
            }
        }
        return f;
    }
}
