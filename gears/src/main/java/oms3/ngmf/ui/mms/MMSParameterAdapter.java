/*
 * $Id: MMSParameterAdapter.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
package oms3.ngmf.ui.mms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/** MMS parameter adapter.
 */
public class MMSParameterAdapter {

    /** Creates a new instance of MMSParameterAdapter */
    public MMSParameterAdapter() {
    }

    public static class MmsParamInfo {

        String description;
        String version;
        Properties omsParams;
        Properties meta = new Properties();

        public void store(OutputStream out) throws IOException {
            PrintStream p = new PrintStream(out);
            TreeMap tm = new TreeMap(omsParams);

            int maxlen = -1;
            for (Iterator it = tm.keySet().iterator(); it.hasNext();) {
                String elem = (String) it.next();
                maxlen = Math.max(elem.length(), maxlen);
            }
            p.println("@S, Parameter");
            p.println(" Descr, \"" + description + "\"");
            p.println(" Version, \"" + version + "\"");
            p.println(" CreatedAt, \"" + new Date() + "\"");
            for (Iterator it = tm.keySet().iterator(); it.hasNext();) {
                String elem = (String) it.next();
                p.println("@P, " + format(elem, maxlen) + "\"" + tm.get(elem) + "\"");
                p.println(meta.get(elem) == null ? "" : meta.get(elem));
            }
            p.close();
        }

        private String format(String inp, int fillLength) {
            StringBuffer b = new StringBuffer();
            for (int i = 0; i < fillLength - inp.length(); i++) {
                b.append(' ');
            }
            return inp + "," + b.toString();
        }
    }

    public static MmsParamInfo map(File mmsParamFile) throws IOException {
        MmsParamInfo info = new MmsParamInfo();
        info.omsParams = new Properties();

        MmsParamsReader mmsReader = new MmsParamsReader(mmsParamFile.toString());
        ParameterSet ps = mmsReader.read();

        info.description = ps.getDescription();
        info.version = ps.getVersion();

        Map m = ps.getDims();
        for (Iterator i = m.values().iterator(); i.hasNext();) {
            Dimension param = (Dimension) i.next();
            info.omsParams.put(param.getName(), Integer.toString(param.getSize()));
            info.meta.put(param.getName(), " role, dimension");
        }

        m = ps.getParams();
        for (Iterator i = m.values().iterator(); i.hasNext();) {
            Parameter param = (Parameter) i.next();
            Object val = param.getVals();
            StringBuffer b = new StringBuffer();
            if (!param.getDimension(0).getName().equals("one")) {
                b.append("{");
            }
            if (val.getClass() == int[].class) {
                int v[] = (int[]) val;
                if (param.getNumDim() == 2) {
                    Dimension d1 = param.getDimension(0);
                    Dimension d2 = param.getDimension(1);
                    info.meta.put(param.getName(), " bound, " + d1.getName() + ", " + d2.getName());

                    int idx = 0;
                    if (d1.getSize() * d2.getSize() == 0) {
                        b.append("{ }");
                    } else {
                        for (int j = 0; j < d2.getSize(); j++) {
                            b.append("{");
                            for (int j1 = 0; j1 < d1.getSize(); j1++) {
                                b.append(v[idx++] + (j1 < d1.getSize() -1 ? ", " : ""));
                            }
                            b.append("}"+ (j < d2.getSize() -1 ? ", " : ""));
                        }
                    }
                } else {
                    if (!param.getDimension(0).getName().equals("one")) {
                        for (int j = 0; j < v.length; j++) {
                            b.append(v[j] + (j < v.length -1 ? ", " : ""));
                        }
                        info.meta.put(param.getName(), " bound, " + param.getDimension(0).getName());
                    } else {
                        b.append(v[0]);
                    }
                }
            } else if (val.getClass() == double[].class) {
                double v[] = (double[]) val;
                if (param.getNumDim() == 2) {
//                    System.out.println(param.getName() + " " + param.getSize() + " " + param.getWidth() + " " + param.getNumDim() + " "
//                            + param.getType() + " " + param.getDimension(0) + " " + param.getDimension(1));
                    Dimension d1 = param.getDimension(0);
                    Dimension d2 = param.getDimension(1);
                    info.meta.put(param.getName(), " bound, " + d1.getName() + ", " + d2.getName());
//                    System.out.println("    d1: " + d1.getSize());
//                    System.out.println("    d2: " + d2.getSize());
                    int idx = 0;
                    if (d1.getSize() * d2.getSize() == 0) {
                        b.append("{ }");
                    } else {
                        for (int j = 0; j < d2.getSize(); j++) {
                            b.append("{");
                            for (int j1 = 0; j1 < d1.getSize(); j1++) {
                                b.append(v[idx++] + (j1 < d1.getSize() -1 ? ", " : ""));
                            }
                            b.append("}" + (j < d2.getSize() -1 ? ", " : ""));
                        }
                    }
                } else {
                    if (!param.getDimension(0).getName().equals("one")) {
                        for (int j = 0; j < v.length; j++) {
                            b.append(v[j] + (j < v.length -1 ? ", " : ""));
                        }
                         info.meta.put(param.getName(), " bound, " + param.getDimension(0).getName());
                    } else {
                        b.append(v[0]);
                    }
                }
            }
            if (!param.getDimension(0).getName().equals("one")) {
                b.append("}");
            }
//            System.out.println(param.getName() + " " + param.getNumDim() + " " + param.getSize());
            String result;

            // HACK, snarea_curve is a 1D array in MMS but a 2D array in OMS.
            // it is being used as 2D array in both systems.
            if (param.getName().equals("snarea_curve")) {
                result = "{" + b.toString() + "}";
            } else {
                result = b.toString();
            }

            info.omsParams.put(param.getName(), result);
        }
        return info;
    }

    public static void main(String args[]) throws Exception {
        MmsParamInfo info = map(new File("c:/omswork/prms/data/efcarson.params"));
        info.omsParams.store(new FileOutputStream(new File("c:/omswork/prms/data/efcarson1.pps")),
                info.description + " (" + info.version + ")");
    }
}
