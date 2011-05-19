/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.util;

import java.util.Random;
import oms3.Access;
import oms3.ComponentAccess;
import oms3.annotations.Range;

/**
 *
 * @author od
 */
public class Ranges {

    /** Range Checker
     * 
     */
    public static class Check {

        Access out;
        double min;
        double max;

        public Check(Object comp, String field) {
            ComponentAccess cp = new ComponentAccess(comp);
            out = cp.output(field);
            Range range = out.getField().getAnnotation(Range.class);
            if (range == null) {
                throw new IllegalArgumentException(field);
            }
            if (range.max() < range.min()) {
                throw new RuntimeException("min>max " + range);
            }
            min = range.min();
            max = range.max();
        }

        public Check(Object comp, String field, double min, double max) {
            ComponentAccess cp = new ComponentAccess(comp);
            out = cp.output(field);
            if (max < min) {
                throw new RuntimeException("min>max ");
            }
            this.min = min;
            this.max = max;
        }

        public boolean check() {
            Object o = null;
            try {
                o = out.getFieldValue();
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof Double) {
                double d = (Double) o;
                if (!(min <= d && d <= max)) {
                    return false;
                }
            }
            return true;
        }
    }

     public static class Gen {

        Access in;
        double min;
        double max;

        public Gen(Object comp, String field, double min, double max) {
            ComponentAccess cp = new ComponentAccess(comp);
            in = cp.input(field);
            if (max < min) {
                throw new RuntimeException("min>max");
            }
            this.min = min;
            this.max = max;
        }

        public Gen(Object comp, String field) {
            ComponentAccess cp = new ComponentAccess(comp);
            in = cp.input(field);
            Range range = in.getField().getAnnotation(Range.class);
            if (range == null) {
                throw new IllegalArgumentException(field);
            }
            min = range.min();
            max = range.max();
        }

        public double next(Random r) {
            double d = r.nextDouble() * (max - min) + min;
            try {
                in.setFieldValue(new Double(d));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return d;
        }
    }
}
