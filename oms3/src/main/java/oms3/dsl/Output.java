/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Logger;
import oms3.*;
import oms3.Notification.*;
import oms3.io.DataIO;

/**
 *
 * @author od
 */
public class Output implements Buildable {

    protected static final Logger log = Logger.getLogger("oms3.sim");

    private class V {

        String token;
        String name;
        int[] idx;
        Object val;

        V(String token, String name, int[] idx) {
            this.token = token;
            this.name = name;
            this.idx = idx;
        }

        Object value() {
            return (idx == null) ? val : Util.accessArray(name, val, idx);
        }

        String token() {
            if (idx == null && val.getClass().isArray()) {
                int len = Array.getLength(val);
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < len - 1; i++) {
                    b.append(token + "[" + i + "],");
                }
                b.append(token + "[" + (len - 1) + "]");
                return b.toString();
            }
            return token;
        }

        String valueString() {
            Object v = value();
            if (v == null) {
                throw new IllegalArgumentException("Missing output: " + token);
            }
            if (v.getClass() == Double.class) {
                return String.format(Locale.US, fformat, v);
            } else if (v instanceof Calendar) {
                return dfmt.format(((Calendar) v).getTime());
            } else if (v instanceof double[]) {
                return dblfmt((double[]) v);
            }
            return v.toString();
        }

        String dblfmt(double[] d) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < d.length - 1; i++) {
                b.append(String.format(Locale.US, fformat, d[i]));
                b.append(',');
            }
            b.append(String.format(Locale.US, fformat, d[d.length - 1]));
            return b.toString();
        }

        String type() {
            if (idx == null && val.getClass().isArray()) {
                String t = val.getClass().getComponentType().getSimpleName();
                int len = Array.getLength(val);
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < len - 1; i++) {
                    b.append(t + ",");
                }
                b.append(t);
                return b.toString();
            }
            if (val != null && value() instanceof Calendar) {
                return "Date";
            }
            return val == null ? "null" : value().getClass().getSimpleName();
        }
    }
    final List<V> vars = new ArrayList<V>();
    Set<String> d = new TreeSet<String>();
    //
    String file;
    //
    PrintWriter w;
    boolean printHeader = true;
    //
    String fformat = "%7.3f";
    String dformat = "%10d";
    SimpleDateFormat dfmt = Conversions.ISO();

    public void setDateformat(String dateformat) {
        dfmt = new SimpleDateFormat(dateformat);
    }

    public void setFloatformat(String format) {
        fformat = (!format.startsWith("%")) ? ('%' + format) : format;
    }

    public void setDecimalformat(String format) {
        dformat = (!format.startsWith("%")) ? ('%' + format) : format;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setVars(String varlist) {
        StringTokenizer t = new StringTokenizer(varlist, ";,:");
        while (t.hasMoreTokens()) {
            String var = t.nextToken().trim();
            String[] l = Conversions.parseArrayElement(var);
            vars.add(new V(var, l[0], Util.arraysDims(l)));
            d.add(l[0]);
        }
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }

    public void setup(Object comp, File dir, final String header) throws IOException {
        printHeader = true;
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (vars.isEmpty()) {
            throw new IllegalArgumentException("no variables to output.");
        }

        if (comp instanceof Compound) {
            Compound c = (Compound) comp;

            if (file != null) {
                w = new PrintWriter(new FileWriter(new File(dir, file), false));
            } else {
                w = new PrintWriter(new OutputStreamWriter(System.out));
            }

            c.addListener(new Listener() {

                int count = 0;
                int vars_size = vars.size();

                @Override
                public void notice(Type T, EventObject E) {
                    if (T == Type.OUT) {
                        DataflowEvent e = (DataflowEvent) E;
                        String fieldName = e.getAccess().getField().getName();
                        if (d.contains(fieldName)) {
                            synchronized (vars) {
                                for (V v : vars) {
                                    if (v.name.equals(fieldName)) {
                                        v.val = e.getValue();
                                        count++;
                                    }
                                }
                                if (count == vars_size) {
                                    if (printHeader) {
                                        printHeader(header);
                                    }
                                    printRow();
                                    count = 0;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    void printHeader(String header) {
        w.println("@T, \"" + header + "\"");
        w.println(" " + DataIO.KEY_CREATED_AT + ", \"" + new Date() + "\"");
        w.println(" " + DataIO.DATE_FORMAT + ", " + dfmt.toPattern());
        String dig = System.getProperty("oms3.digest");
        if (dig != null) {
            w.println(" " + DataIO.KEY_DIGEST + "," + dig);
        }
        w.print("@H");
        for (V v : vars) {
            w.print(", " + v.token());
        }
        w.println();
        w.print(" " + DataIO.KEY_TYPE);
        for (V v : vars) {
            w.print(", " + v.type());
        }
        w.println();
        printHeader = false;
    }

    void printRow() {
        for (V v : vars) {
            w.print(", " + v.valueString());
        }
        w.println();
    }

    public void done() throws IOException {
        w.flush();
        if (file != null) {
            w.close();
        }
    }
}
