/*
 * $Id: CSVColumn.java 347c4561653f 2014-09-10 odavid@colostate.edu $
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
package oms3.dsl.cosu;

import oms3.dsl.AbstractBuildableLeaf;

/**
 *
 * @author od
 */
public class CSVColumn extends AbstractBuildableLeaf {
    
    String file;
    String table;
    String column;
    
    // direct data reference
    String data;

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
    
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

}
