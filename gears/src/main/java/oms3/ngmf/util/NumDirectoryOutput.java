/*
 * $Id: NumDirectoryOutput.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
        ensureEmptyIfExist(nextFolder);
        return nextFolder;
    }
}
