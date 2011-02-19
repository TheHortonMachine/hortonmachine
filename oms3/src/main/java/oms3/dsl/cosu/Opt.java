/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.cosu;

import oms3.dsl.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author od
 */
public class Opt implements Buildable {

    String simulated;
    String observed;
    List<ObjFunc> ofs = new ArrayList<ObjFunc>();

    public void setSimulated(String simulated) {
        this.simulated = simulated;
    }

    public String getSimulated() {
        if (simulated == null) {
            throw new IllegalArgumentException("Missing 'simulated' argument");
        }
        if (simulated.equals(observed)) {
            throw new IllegalArgumentException("'observed' == 'simulated'");
        }
        return simulated;
    }

    public void setObserved(String observed) {
        this.observed = observed;
    }

    public String getObserved() {
        if (observed == null) {
            throw new IllegalArgumentException("Missing 'observed' argument");
        }
        if (simulated.equals(observed)) {
            throw new IllegalArgumentException("'observed' == 'simulated'");
        }
        return observed;
    }

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("of")) {
            ObjFunc of = new ObjFunc();
            ofs.add(of);
            return of;
        }
        throw new IllegalArgumentException(name.toString());
    }

    ///////////////////////////////////////// rt
    boolean isInc() {
        if (ofs.size() == 0) {
            throw new IllegalArgumentException("No Objective function(s) defined. ");
        }
        boolean inc = ofs.get(0).getOF().positiveDirection();
        for (ObjFunc of : ofs) {
            if (of.getOF().positiveDirection() != inc) {
                throw new IllegalArgumentException("Objective function(s) optimization direction mismatch!");
            }
        }
        return inc;
    }

    double getMultiOFValue(double[] obs, double[] sim) {
        if (ofs.size() == 0) {
            throw new IllegalArgumentException("No Objective function(s) defined. ");
        }
        double val = 0.0;
        double weight = 0.0;
        for (ObjFunc of : ofs) {
            weight += of.getWeight();
            val += of.getOF().calculate(obs, sim, -90.0) * of.getWeight();
        }
        if (weight != 1.0) {
            throw new IllegalArgumentException("sum of of weights != 1.0");
        }
        return val;
    }

    void adjustWeights() {
        int noOf = ofs.size();
        for (ObjFunc of : ofs) {
            if (Double.isNaN(of.getWeight())) {
                of.setWeight((double) 1 / noOf);
            }
        }
    }
}
