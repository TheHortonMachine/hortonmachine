/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util;

import java.io.File;

/**
 *
 * @author od
 */
public class SimpleDirectoryOutput extends OutputStragegy {

    String name = "out";

    public SimpleDirectoryOutput(File base, String simName) {
        super(new File(base, simName));
    }

    @Override
    public File nextOutputFolder() {
        return new File(basedir, name);
    }

    @Override
    public File lastOutputFolder() {
         return new File(basedir, name);
    }

    @Override
    public File firstOutputFolder() {
         return new File(basedir, name);
    }

    @Override
    public File previousOutputFolder() {
         return new File(basedir, name);
    }
}
