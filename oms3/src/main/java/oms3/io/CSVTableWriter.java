/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
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
                w.println(key[0] + ", " + key[1]);
            }
        }
    }

    public CSVTableWriter(Writer s, String name, Map<String, String> meta) {
        w = new PrintWriter(s);
        w.println("@T," + name);
        if (meta != null) {
            for (String key : meta.keySet()) {
                w.println(key + ", " + meta.get(key));
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

    public void writeHeader(Map<String, String[]> meta, String... col) {
        w.print("@H");
        writeRow((Object[]) col);
        if (meta != null) {
            for (String key : meta.keySet()) {
                w.print(key);
                writeRow((Object[]) meta.get(key));
            }
        }
    }

    public void writeHeader(String[][] meta, String... col) {
        w.print("@H");
        writeRow((Object[]) col);
        if (meta != null) {
            for (String[] key : meta) {
                w.print(key[0]);
                for (int i = 1; i < key.length; i++) {
                    w.print("," + key[i]);
                }
                w.println();
            }
        }
    }

    public void writeRow(Object... val) {
        for (Object v : val) {
            w.print(",");
            w.print(v);
        }
        w.println();
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
