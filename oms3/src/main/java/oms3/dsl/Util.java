/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

import java.lang.reflect.Array;
import java.util.List;

/**
 *
 * @author od
 */
public class Util {

    public static double[] getVals(Param p) {
        Object o = p.getValue();
        if (o.getClass() == double[].class) {
            return (double[]) o;
        } else if (o.getClass() == Double.class) {
            return new double[]{(Double) o};
        }
        throw new IllegalArgumentException(p.toString());
    }

    public static void setVals(double[] val, Param p) {
        Object o = p.getValue();
        if (o.getClass() == double[].class) {
            p.setValue(val);
        } else if (o.getClass() == Double.class) {
            p.setValue(val[0]);
        }
        throw new IllegalArgumentException(p.toString());
    }

    public static double[] convert(List<Double> l) {
        double[] d = new double[l.size()];
        for (int i = 0; i < d.length; i++) {
            d[i] = l.get(i);
        }
        return d;
    }

    public static double[] convertNumber(List<Number> l) {
        double[] d = new double[l.size()];
        for (int i = 0; i < d.length; i++) {
            d[i] = l.get(i).doubleValue();
        }
        return d;
    }

    static Object accessArray(String name, Object val, int idx[]) {
        if (!val.getClass().isArray()) {
            throw new IllegalArgumentException("Not an array : " + name + " " + val);
        }
        for (int i : idx) {
            val = Array.get(val, i);
        }
        return val;
    }

    static public int[] arraysDims(String[] arr) {
        if (arr.length > 1) {
            int[] idx = new int[arr.length - 1];
            for (int i = 1; i < arr.length; i++) {
                idx[i - 1] = Integer.parseInt(arr[i]);
            }
            return idx;
        }
        return null;
    }
}
