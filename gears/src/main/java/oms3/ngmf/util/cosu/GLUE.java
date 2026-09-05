/*
 * $Id: GLUE.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
package oms3.ngmf.util.cosu;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import oms3.dsl.Param;
import oms3.dsl.Util;
import oms3.io.CSProperties;

/**
 *
 * @author od
 */
public class GLUE {

    static class OptParam {

        double mean;
        double range;
        double[] prop_dev = null;
        Param parameter;

        public OptParam(Param p) {
            parameter = p;
            double min = p.getLower();
            double max = p.getUpper();

            // Compute the parameter mean.
//            double[] val_f = p.getVal_f();
            double[] val = Util.getVals(p);
            mean = 0.0;
            for (int i = 0; i < val.length; i++) {
                mean += val[i];
            }
            mean = mean / val.length;
            range = max - min;

            /*  Compute the proportional deviation from the mean for each
             *  individual parameter value.
             */
            prop_dev = new double[val.length];
            for (int i = 0; i < prop_dev.length; i++) {
                prop_dev[i] = val[i] / mean;
            }
        }

        public void setMean(double new_mean) {
            mean = new_mean;
        }

        public Param getParam() {
            return parameter;
        }

        public double getRange() {
            return range;
        }

        public double[] getDev() {
            return prop_dev;
        }
    }

    List<OptParam> opt_params = new ArrayList<OptParam>();
    Random random = new Random();

    public GLUE(CSProperties params) {
//    public GLUE(List<Param> params) {
//        for (Param param : params) {
//            opt_params.add(new OptParam(param));
//        }
    }

    public void newParamSet() {
        for (OptParam op : opt_params) {
            Param p = op.getParam();
            double new_mean = random.nextDouble() * op.getRange() + p.getLower();
            op.setMean(new_mean);

            // Calculate new parameter values based on the random mean.
            double[] pro_dev = op.getDev();
            double[] new_val = new double[pro_dev.length];
            double min = p.getLower();
            double max = p.getUpper();

            for (int j = 0; j < new_val.length; j++) {
                new_val[j] = pro_dev[j] * new_mean;
                if (new_val[j] > max) {
                    new_val[j] = max;
                }
                if (new_val[j] < min) {
                    new_val[j] = min;
                }
            }
            //  Update the Parameter objects with the new values
            Util.setVals(new_val, p);
        }
    }
  

//    public static void main(String[] args) {
//        Double d = new Double(4);
//        double[] da = new double[] { d };
//        System.out.println(da[0]);
//    }

}
