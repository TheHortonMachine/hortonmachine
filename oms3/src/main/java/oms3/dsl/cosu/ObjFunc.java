/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.cosu;

import oms3.dsl.*;
import ngmf.util.cosu.luca.of.*;
import oms3.ObjectiveFunction;
import ngmf.util.cosu.luca.of.PearsonsCorrelation;
import oms3.SimConst;

/**
 *
 * @author od
 */
public class ObjFunc implements Buildable {

    double weight = Double.NaN;
    String timestep = SimConst.DAILY;
    ObjectiveFunction of;

    public void setMethod(String method) {
        if (method.equals(SimConst.NS)) {
            of = new NashSutcliffe();
        } else if (method.equals(SimConst.RMSE)) {
            of = new NormalizedRMSE();
        } else if (method.equals(SimConst.ABSDIF)) {
            of = new AbsoluteDifference();
        } else if (method.equals(SimConst.LOGABSDIF)) {
            of = new AbsoluteDifferenceLog();
        } else if (method.equals(SimConst.PMCC)) {
            of = new PearsonsCorrelation();
        } else {
            try {
                // load this as a class name from default classpath
                Class<?> c = Class.forName(method);
                of = (ObjectiveFunction) c.newInstance();
            } catch (Exception E) {
                throw new IllegalArgumentException("No such method: " + method);
            }
        }
    }

    String getTimestep() {
        return timestep;
    }

    public void setTimestep(String timestep) {
        if (!timestep.equals(SimConst.DAILY)) {
            throw new IllegalArgumentException("Illegal timestep: " + timestep);
        }
        this.timestep = timestep;
    }

    public void setWeight(double weight) {
        if (weight <= 0 || weight > 1) {
            throw new IllegalArgumentException("of weight out of range: " + weight);
        }
        this.weight = weight;
    }

    double getWeight() {
        return weight;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }

    //////////////////////
    ObjectiveFunction getOF() {
        if (of == null) {
            throw new IllegalArgumentException("No Objective function method defined.");
        }
        return of;
    }
}
