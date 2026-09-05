/*
 * $Id: Util.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
