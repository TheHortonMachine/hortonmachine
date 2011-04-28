/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

import oms3.ComponentException;
import java.io.File;
import ngmf.util.DateDirectoryOutput;
import ngmf.util.NumDirectoryOutput;
import ngmf.util.OutputStragegy;
import ngmf.util.SimpleDirectoryOutput;

import static oms3.SimConst.*;

/**
 *
 * @author od
 */
public class OutputDescriptor implements Buildable {

    int scheme = SIMPLE;
    String dir = System.getProperty("user.dir");

    public void setDir(String dir) {
        if (!new File(dir).exists()) {
            throw new ComponentException("File does not exists " + dir);
        }
        if (!new File(dir).isDirectory()) {
            throw new ComponentException("Not a directory " + dir);
        }
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }

    public void setScheme(int scheme) {
        if ((scheme == SIMPLE) || (scheme == NUMBERED) || (scheme == TIME)) {
            this.scheme = scheme;
        } else {
            throw new ComponentException("Invalid output strategy scheme.");
        }
    }

    public OutputStragegy getOutputStrategy(String simName) {
        OutputStragegy st = null;
        if (scheme == SIMPLE) {
            st = new SimpleDirectoryOutput(new File(getDir()), simName);
        } else if (scheme == NUMBERED) {
            st = new NumDirectoryOutput(new File(getDir()), simName);
        } else if (scheme == TIME) {
            st = new DateDirectoryOutput(new File(getDir()));
        }
        return st;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }
}
