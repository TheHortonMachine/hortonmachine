/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author od
 */
public class NumDirectoryOutput extends OutputStragegy {

    static private DecimalFormat df = new DecimalFormat("0000");

    private static class FF implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            try {
                Integer.parseInt(name);
                return true;
            } catch (NumberFormatException E) {
                return false;
            }
        }
    }

    public NumDirectoryOutput(File base, String simName) {
        super(new File(base, simName));
    }

    @Override
    public File nextOutputFolder() {
        if (!basedir.exists()) {
            basedir.mkdirs();
        }
        File[] files = basedir.listFiles(new FF());
        int no = 0;
        if (files != null && files.length > 0) {
            Arrays.sort(files, new Comparator<File>() {

                @Override
                public int compare(File o1, File o2) {
                    return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
                }
            });
            no = Integer.parseInt(files[files.length - 1].getName()) + 1;
        }

        String filename = df.format(no);
        File nextFolder = new File(basedir, filename);
        return nextFolder;
    }
}
