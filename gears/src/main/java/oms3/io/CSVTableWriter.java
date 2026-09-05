/*
 * $Id: CSVTableWriter.java a4500310d299 2013-03-13 odavid@colostate.edu $
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
package oms3.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author od
 */
public class CSVTableWriter {

    PrintWriter w;

    public CSVTableWriter(Writer s, String name, String[][] meta) {
        w = new PrintWriter(s);
        w.println("@T," + name);
        if (meta != null) {
            for (String[] key : meta) {
                w.println(" " + key[0] + ", " + key[1]);
            }
        }
    }

    public CSVTableWriter(Writer s, String name, Map<String, String> meta) {
        w = new PrintWriter(s);
        w.println("@T," + name);
        if (meta != null) {
            for (String key : meta.keySet()) {
                w.println(" " + key + ", " + meta.get(key));
            }
        }
    }

    public CSVTableWriter(OutputStream s, String name, String[][] meta) {
        this(new OutputStreamWriter(s), name, meta);
    }

    public CSVTableWriter(File file, String name) throws IOException {
        this(new FileWriter(file), name, (String[][]) null);
    }

    public void writeHeader(String... col) {
        writeHeader((String[][]) null, col);
    }

    public void writeHeader(Map<String, String[]> meta, List<String> col) {
        writeHeader(meta, col.toArray(new String[0]));
    }
    
    public void writeHeader(Map<String, String[]> meta, String... col) {
        w.print("@H");
        writeRow((Object[]) col);
        if (meta != null) {
            for (String key : meta.keySet()) {
                w.print(" " + key);
                writeRow((Object[]) meta.get(key));
            }
        }
    }

    public void writeHeader(String[][] meta, String... col) {
        w.print("@H");
        writeRow((Object[]) col);
        if (meta != null) {
            for (String[] key : meta) {
                w.print(" " + key[0]);
                for (int i = 1; i < key.length; i++) {
                    w.print(", " + key[i]);
                }
                w.println();
            }
        }
    }
    
    public void writeRow(Object... val) {
        for (Object v : val) {
            w.print(",");
            w.print(v == null ? "" : v);
        }
        w.println();
    }

    // skip the first column
    public void writeRow(Object[] val, int startCol) {
        for (int i = startCol; i < val.length; i++) {
            w.print(",");
            w.print(val[i] == null ? "" : val[i]);
        }
        w.println();
    }
    
    public void writeRow(List<?> val) {
        for (Object v : val) {
            w.print(",");
            w.print(v == null ? "" : v);
        }
        w.println();
    }
    
    public void writeRows(List<String[]> val) {
        for (String[] v : val) {
            writeRow((Object[]) v);
        }
    }

    public void writeRows(List<String[]> val, int startCol) {
        for (String[] v : val) {
            writeRow((Object[]) v, startCol);
        }
    }

    public void close() {
        w.flush();
    }

    public static void main(String[] args) {
        CSVTableWriter w = new CSVTableWriter(System.out, "Olaf", new String[][]{
                    {"unit", "mm"},
                    {"key", "value1"}
                });
        w.writeHeader(new String[][]{
                    {"unit", "mm", "name", "val"},
                    {"format", "mm", "fff", "ffff"}
                }, "temp", "olaf", "precip");
        w.writeRow(1.3, "olaf", 5.23);
        w.writeRow(1.3, "olaf", 5.23);
        w.writeRow(1.3, "olaf", 5.23);
        w.close();
    }
}
