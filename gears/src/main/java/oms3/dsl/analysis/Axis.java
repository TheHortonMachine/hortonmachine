/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.analysis;

import oms3.dsl.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.ngmf.ui.graph.ValueSet;
import oms3.ngmf.util.OutputStragegy;

/**
 *
 * @author od
 */
public class Axis implements Buildable, ValueSet {

    String file;
    String table;
    String column;

    String name;
    
    boolean shape = false;
    boolean line = true;

    public void setLine(boolean line) {
        this.line = line;
    }

    public void setShape(boolean shape) {
        this.shape = shape;
    }

    @Override
    public boolean isLine() {
        return line;
    }

    @Override
    public boolean isShape() {
        return shape;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setTable(String table) {
        this.table = table;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }

    // for xaxis
    Date[] getDates(File st, String simName) throws IOException {
        CSTable t = table(st);
        return DataIO.getColumnDateValues(t, column);
    }

    @Override
    public Double[] getDoubles(File st, String simName) throws IOException {
        CSTable ty = table(st);
        return DataIO.getColumnDoubleValues(ty, column);
    }

    private CSTable table(File st) throws IOException {
        File f = new File(file);
        if (!(f.isAbsolute() && f.exists())) {
            if (file.startsWith("%")) {
                f = OutputStragegy.resolve(new File(st, file));
            } else {
                f = OutputStragegy.resolve(file);
            }
        }
        return DataIO.table(f, table);
    }
    
    @Override
    public String getName() {
        return name == null ? column : name;
    }
}
