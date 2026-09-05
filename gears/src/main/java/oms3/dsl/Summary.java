/*
 * $Id: Summary.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
package oms3.dsl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import oms3.*;
import oms3.Notification.*;

import oms3.util.Statistics;
import static oms3.SimBuilder.*;

/**
 *
 * @author od
 */
public class Summary extends AbstractBuildableLeaf {

    private static final String[] opt = {MONTHLY, YEARLY, WEEKLY};
    //
    String time;
    String var;
    int idx[];
    Calendar cal;
    StringBuffer out;
    List<Number> var_l = new ArrayList<Number>();
    int field = Calendar.DAY_OF_MONTH;
    String moments = MEAN;
    // output file optional
    String file;

    public void setFile(String file) {
        this.file = file;
    }

    public void setPeriod(String period) {
        if (period.equals(WEEKLY)) {
            field = Calendar.DAY_OF_WEEK;
        } else if (period.equals(MONTHLY)) {
            field = Calendar.DAY_OF_MONTH;
        } else if (period.equals(YEARLY)) {
            field = Calendar.DAY_OF_YEAR;
        } else {
            throw new IllegalArgumentException(period);
        }
    }

    public void setMoments(String moments) {
        this.moments = moments;
    }

    public void setVar(String var) {
        String[] l = Conversions.parseArrayElement(var);
        this.var = l[0];
        idx = Util.arraysDims(l);
    }

    public void setTime(String time) {
        this.time = time;
    }

    void setup(Object comp) {
        if (comp instanceof Compound) {
            Compound c = (Compound) comp;

            out = new StringBuffer("Summary for '" + var + "' (" + opt[field - 5] + ")\n");
            out.append(String.format(Locale.US, "%19s", time) + "  ");
            for (String s : moments.split(" ")) {
                out.append(String.format(Locale.US, "%14s", s));
            }
            out.append('\n');

            c.addListener(new Listener() {

                @Override
                public void notice(Type T, EventObject E) {
                    if (T == Type.OUT) {
                        DataflowEvent e = (DataflowEvent) E;
                        if (e.getAccess().getField().getName().equals(time)) {
                            if (cal == null) {
                                cal = (Calendar) e.getValue();
                            }
                        } else if (e.getAccess().getField().getName().equals(var)) {
                            if (idx == null) {
                                var_l.add((Number) e.getValue());
                            } else {
                                var_l.add((Number) Util.accessArray(var, e.getValue(), idx));
                            }
                            if (cal == null) {
                                // TODO dangerous
                                return;
                            }
                            if (cal.get(field) == 1) {
                                double[] d = Util.convertNumber(var_l);
                                double eff = 0;
                                out.append(Conversions.formatISO(cal.getTime()));
                                out.append("  ");
                                for (String m : moments.split(" ")) {
                                    if (MAX.startsWith(m)) {
                                        eff = Statistics.max(d);
                                    } else if (MIN.startsWith(m)) {
                                        eff = Statistics.min(d);
                                    } else if (MEAN.startsWith(m)) {
                                        eff = Statistics.mean(d);
                                    } else if (COUNT.startsWith(m)) {
                                        eff = Statistics.length(d);
                                    } else if (RANGE.startsWith(m)) {
                                        eff = Statistics.range(d);
                                    } else if (MEDIAN.startsWith(m)) {
                                        eff = Statistics.median(d);
                                    } else if (STDDEV.startsWith(m)) {
                                        eff = Statistics.stddev(d);
                                    } else if (VAR.startsWith(m)) {
                                        eff = Statistics.variance(d);
                                    } else if (MEANDEV.startsWith(m)) {
                                        eff = Statistics.meandev(d);
                                    } else if (SUM.startsWith(m)) {
                                        eff = Statistics.sum(d);
                                    } else if (PROD.startsWith(m)) {
                                        eff = Statistics.product(d);
                                    } else if (Q1.startsWith(m)) {
                                        eff = Statistics.quantile(d, 0.25);
                                    } else if (Q2.startsWith(m)) {
                                        eff = Statistics.quantile(d, 0.50);
                                    } else if (Q3.startsWith(m)) {
                                        eff = Statistics.quantile(d, 0.75);
                                    } else if (LAG1.startsWith(m)) {
                                        eff = Statistics.lag1(d);
                                    } else {
                                        throw new IllegalArgumentException(m);
                                    }
                                    out.append(String.format(Locale.US, "%14.5f", eff));
                                }
                                out.append('\n');
                                var_l.clear();
                            }
//                    System.err.println(E.getAccess().getField().getName() + "/" +
//                    E.getComponent().getClass().getName() + E.getValue());
                        }
                    }
                }
            });
        }
    }

    public void printSum(File dir) throws IOException {
        PrintWriter w;
        if (file != null) {
            w = new PrintWriter(new FileWriter(new File(dir, file), true));
        } else {
            w = new PrintWriter(new OutputStreamWriter(System.out));
        }
        w.println(out.toString());
        w.flush();
        if (file != null) {
            w.close();
        }
    }
}
