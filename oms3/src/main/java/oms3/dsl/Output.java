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
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
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

    class V {

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
                return Conversions.formatISO(cal.getTime());
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
            return val == null ? "null" : value().getClass().getSimpleName();
        }
    }
    String file;
    String time;
    final Map<String, V> vars = new LinkedHashMap<String, V>();
    //
    Calendar cal;
    PrintWriter w;
    long lasttime = 0;
    boolean printTypes = true;
    boolean printHeader = true;
    String fformat = "%10.3f";
    String dformat = "%10d";

    public void setFformat(String format) {
        if (!format.startsWith("%")) {
            fformat = '%' + format;
        } else {
            fformat = format;
        }
    }

    public void setDformat(String format) {
        if (!format.startsWith("%")) {
            dformat = '%' + format;
        } else {
            dformat = format;
        }
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setVars(String varlist) {
        StringTokenizer t = new StringTokenizer(varlist, ";,:");
        while (t.hasMoreTokens()) {
            String var = t.nextToken().trim();
            String[] l = Conversions.parseArrayElement(var);
            vars.put(l[0], new V(var, l[0], Util.arraysDims(l)));
        }
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }

    public void setup(Object comp, File dir, final String header) throws IOException {
        lasttime = 0;
        printTypes = true;
        printHeader = true;
        cal = null;
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (time == null) {
            throw new IllegalArgumentException("property 'time' not set.");
        }
        if (vars.size() == 0) {
            throw new IllegalArgumentException("property 'vars' not set.");
        }

        if (comp instanceof Compound) {
            Compound c = (Compound) comp;

            if (file != null) {
//                if (new File(dir, file).exists()) {
//                    throw new IllegalArgumentException("Duplicate: " + file);
//                }
                w = new PrintWriter(new FileWriter(new File(dir, file), false));
            } else {
                w = new PrintWriter(new OutputStreamWriter(System.out));
            }

            c.addListener(new Listener() {

                @Override
                public void notice(Type T, EventObject E) {
                    if (T == Type.OUT) {
                        DataflowEvent e = (DataflowEvent) E;
                        String fieldName = e.getAccess().getField().getName();
                        if (fieldName.equals(time)) {
                            if (cal == null) {
                                cal = (Calendar) e.getValue();
                                lasttime = cal.getTimeInMillis();
                            }
                            return;
                        } else if (vars.containsKey(fieldName)) {
                            synchronized (vars) {
//                                System.out.println(fieldName + " " + cal.getTimeInMillis() + " " + e.getValue());
                                if (cal != null && cal.getTimeInMillis() > lasttime) {
                                    if (printHeader) {
                                        printHeader(header);
                                    }
                                    if (printTypes) {
                                        printTypes();
                                    }
                                    lasttime = cal.getTimeInMillis();
                                    printRow();
                                }
                                V v = vars.get(fieldName);
                                v.val = e.getValue();
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
        w.println(" " + DataIO.DATE_FORMAT + ", " + Conversions.ISO().toPattern());
        String dig = System.getProperty("oms3.digest");
        if (dig != null) {
            w.println(" " + DataIO.KEY_DIGEST + "," + dig);
        }
        w.print("@H, " + time);
        for (V v : vars.values()) {
            w.print(", " + v.token());
        }
        w.println();
        printHeader = false;
    }

    void printTypes() {
        w.print(" " + DataIO.KEY_TYPE + ", " + DataIO.VAL_DATE);
        for (V v : vars.values()) {
            w.print(", " + v.type());
        }
        w.println();
        printTypes = false;
    }

    void printRow() {
        w.print("," + Conversions.formatISO(new Date(lasttime)));
        for (V v : vars.values()) {
            w.print(", " + v.valueString());
            v.val = null;
        }
        w.println();
    }

    public void done() throws IOException {
        printRow();
        w.flush();
        if (file != null) {
            w.close();
        }
    }
}
