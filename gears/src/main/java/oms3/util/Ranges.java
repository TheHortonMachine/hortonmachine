/*
 * $Id: Ranges.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
