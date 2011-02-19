/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.analysis;

import oms3.dsl.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import ngmf.util.OutputStragegy;
import oms3.io.CSTable;
import oms3.io.DataIO;

/**
 *
 * @author od
 */
public class Axis implements Buildable, ValueSet {

    String file;
    String table;
    String column;

    String name;

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
    Date[] getDates(OutputStragegy st, String simName) throws IOException {
        CSTable t = table(st);
        return DataIO.getColumnDateValues(t, column);
    }

    @Override
    public Double[] getDoubles(OutputStragegy st, String simName) throws IOException {
        CSTable ty = table(st);
        return DataIO.getColumnDoubleValues(ty, column);
    }

    private CSTable table(OutputStragegy st) throws IOException {
        File f = new File(file);
        if (!(f.isAbsolute() && f.exists())) {
            if (file.startsWith("%")) {
                f = OutputStragegy.resolve(new File(st.baseFolder(), file));
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
