/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author od
 */
public class DateDirectoryOutput extends OutputStragegy {
   
    DateFormat daf = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");

    public DateDirectoryOutput(File basedir) {
        super(basedir);
    }

    @Override
    public File nextOutputFolder() {
        if (!basedir.exists()) {
            basedir.mkdirs();
        }
        String filename = daf.format(new Date());
        File nextFolder = new File(basedir, filename);
        return nextFolder;
    }
}
