/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.dsl;

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
            throw new IllegalArgumentException("does not exists " + dir);
        }
        if (!new File(dir).isDirectory()) {
            throw new IllegalArgumentException("not a directory " + dir);
        }
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }

    public void setScheme(int scheme) {
        this.scheme = scheme;
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
