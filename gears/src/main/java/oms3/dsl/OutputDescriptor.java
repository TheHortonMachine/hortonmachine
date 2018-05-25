/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

import oms3.ComponentException;
import java.io.File;
import oms3.ngmf.util.DateDirectoryOutput;
import oms3.ngmf.util.NumDirectoryOutput;
import oms3.ngmf.util.OutputStragegy;
import oms3.ngmf.util.SimpleDirectoryOutput;

import static oms3.SimConst.*;

/**
 *
 * @author od
 */
public class OutputDescriptor implements Buildable {

    int scheme = SIMPLE;
    File dir = new File(System.getProperty("user.dir"));

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
        OutputStragegy st = null;
        if (scheme == SIMPLE) {
            st = new SimpleDirectoryOutput(getDir(), simName);
        } else if (scheme == NUMBERED) {
            st = new NumDirectoryOutput(getDir(), simName);
        } else if (scheme == TIME) {
            st = new DateDirectoryOutput(getDir());
        }
        return st;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }
}
