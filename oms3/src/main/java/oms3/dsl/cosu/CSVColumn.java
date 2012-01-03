/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.cosu;

import oms3.dsl.Buildable;

/**
 *
 * @author od
 */
public class CSVColumn implements Buildable {
    
    String file;
    String table;
    String column;

    public void setFile(String file) {
        this.file = file;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getFile() {
        if (file == null) {
            throw new RuntimeException("missing file name.");
        }
        return file;
    }

    public String getTable() {
        return table;   // can be null
    }

    public String getColumn() {
        if (column == null) {
            throw new RuntimeException("missing column name.");
        }
        return column;
    }

    @Override
    public Buildable create(Object name, Object value) {
       return LEAF;
    }
}
