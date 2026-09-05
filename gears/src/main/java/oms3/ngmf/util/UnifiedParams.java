/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.Access;
import oms3.ComponentAccess;
import oms3.ComponentException;
import oms3.Conversions;
import oms3.annotations.Bound;
import oms3.annotations.Range;
import oms3.io.CSProperties;
import oms3.io.DataIO;
import oms3.util.Annotations;

/**
 *
 * @author od
 */
public class UnifiedParams {

    private CSProperties params;

    public UnifiedParams(CSProperties params) {
        this.params = params;
    }

    public UnifiedParams() {
        params = DataIO.properties();
    }

    public UnifiedParams(Map<String, Object> p) {
        params = DataIO.properties(p);
    }

    public void add(CSProperties p) {
        params.putAll(p);
    }

    // this is for external overrides
    public void add(Map<String, Object> p) {
        params.putAll(p);
    }

    public CSProperties getParams() {
        return params;
    }

    public String getInternalKey(String pname) {
        // direct access
        if (params.containsKey(pname)) {
            return pname;
        }
        // check for transformed tables
        List<String> p = new ArrayList<String>();
        String tableCol = "$" + pname;
        for (String key : params.keySet()) {
            if (key.endsWith(tableCol)) {
                p.add(key);
            }
        }
        if (p.isEmpty()) {
            return null;
        }
        if (p.size() > 1) {
            throw new RuntimeException("Multiple keys found for parameter: " + pname + " " + p.toString());
        }
        return p.get(0);
    }

    public Object getParamValue(String pname) {
        String key = getInternalKey(pname);
        if (key == null) {
            return null;
//            throw new IllegalArgumentException("No such parameter " + pname);
        }
        // check for direct keys
        Object val = params.get(key);
        if (val == null) {
            throw new RuntimeException("Null value for parameter: " + pname);
        }
        return val;
    }

    public void putParamValue(String pname, Object pvalue) {
        String key = getInternalKey(pname);
        if (key == null) {
            throw new IllegalArgumentException("No such parameter " + pname);
        }
        params.put(key, pvalue);
    }

    public void putNewParamValue(String pname, Object pvalue) {
        params.put(pname, pvalue);
    }

    public Object toValue(String name, Object vals) {
        Object orig = getParamValue(name);
        if (orig.toString().indexOf('{') > -1) {
            // this is an array (hopefully 1dim)
            return Conversions.convert(vals, String.class);
        } else {
            double[] dvals = (double[]) vals;
            return Double.toString(dvals[0]);
        }
    }

    public Object toValueI(String name, int[] vals) {
        Object orig = getParamValue(name);
        if (orig.toString().indexOf('{') > -1) {
            // this is an array (hopefully 1dim)
            return Conversions.convert(vals, String.class);
        } else {
            return Integer.toString(vals[0]);
        }
    }

    synchronized public boolean setInputData(Object comp, Logger log) {
        PrintWriter w = null;
        File file = null;
        boolean success = true;

        ComponentAccess cp = new ComponentAccess(comp);
        for (Access in : cp.inputs()) {
            String fieldName = in.getField().getName();
            Object inpValue = getParamValue(fieldName);
            if (inpValue != null) {
                Class fieldType = in.getField().getType();
                // allow files and dates provided as strings and
                // convert them
                try {
                    // pass in the raw parameter
                    if (inpValue.equals("rawparam") && fieldType.getName().equals("oms3.io.CSProperties")) {
                        in.setFieldValue(params);
                        if (log.isLoggable(Level.CONFIG)) {
                            log.config("@In " + comp.getClass().getName() + "@" + fieldName + " <- '" + inpValue + "'");
                        }
                        continue;
                    }
//                    System.out.println("inputValue " + inpValue);
//                    System.out.println("inputValue cl " + inpValue.getClass());
//                    System.out.println("fieldType " + fieldType.getCanonicalName());
//                    System.out.println();
                    inpValue = conv(inpValue, fieldType);
                    // check the range if possible.
                    if (Number.class.isAssignableFrom(fieldType) || fieldType == double.class || fieldType == float.class || fieldType == int.class) {
                        Range range = in.getField().getAnnotation(Range.class);
                        if (range != null) {
                            double v = ((Number) inpValue).doubleValue();
                            if (!Annotations.inRange(range, v)) {
                                if (log.isLoggable(Level.WARNING)) {
                                    log.warning("Value '" + v + "' not in Range: " + range);
                                }
                            }
                        }
                    }
                    in.setFieldValue(inpValue);
                    if (log.isLoggable(Level.CONFIG)) {
                        log.config("@In " + comp.getClass().getName() + "@" + fieldName + " <- '" + inpValue + "'");
                    }
                } catch (Exception ex) {
                    throw new ComponentException("Failed setting '" + fieldName + "' type " + in.getField().getType().getCanonicalName() + " <- " + ex.getMessage());
                }
                continue;
            } else {
                if (System.getProperty("oms.check_params") != null) {
                    try {
                        if (w == null) {
                            file = new File(System.getProperty("oms3.work", System.getProperty("user.dir")), "missing_params.csv");
                            w = new PrintWriter(new FileWriter(file));
                            w.println("# Missing parameter, copy those entries into one of your parameter files.");
                        }
                        String val = null;
                        Bound b = null;
                        Object o = in.getFieldValue();
                        if (o != null) {
                            val = o.toString();
                        } else {
                            b = in.getField().getAnnotation(Bound.class);
                            if (b != null) {
                                try {
                                    Object v = getParamValue(b.value());
                                    if (v == null) {
                                        v = new Integer(0);
                                    }
                                    int dim = Integer.parseInt(v.toString());
                                    int[] d = new int[dim];
                                    val = Conversions.convert(d, String.class);
                                } catch (NumberFormatException E) {
                                    val = "?";
                                }
                            } else {
                                val = "?";
                            }
                        }
                        w.println("@P, " + fieldName + ",  \"" + val + "\"");
                        if (b != null) {
                            w.println(" bound, " + b.value());
                        }
                    } catch (Exception E) {
                        throw new RuntimeException(E);
                    }
                }
                if (log.isLoggable(Level.WARNING)) {
                    log.warning("No Input for '" + fieldName + "'");
                }
            }
        }
        if (w != null) {
            w.close();
            System.out.println("Missing parameter [" + file + "]");
            success = false;
        }
        return success;
    }

    private static Object conv(Object inpValue, Class<?> fieldType) {
        Class inpType = inpValue.getClass();
        if (fieldType.isAssignableFrom(inpType)) {
            return inpValue;
        }
//        if (inpType == String.class && fieldType != String.class) {
//            inpValue = Conversions.convert((String) inpValue, fieldType);
//        } else if (inpType == BigDecimal.class && fieldType != BigDecimal.class) {
//            inpValue = Conversions.convert(inpValue.toString(), fieldType);
//        } else if (inpValue instanceof CharSequence) {
//            inpValue = Conversions.convert(inpValue.toString(), fieldType);
//        } else 

        // this is for GStringimpl
        if (inpValue instanceof CharSequence) {
            inpValue = Conversions.convert(inpValue.toString(), fieldType);
        } else {
            inpValue = Conversions.convert(inpValue, fieldType);
        }
        return inpValue;
    }
}
