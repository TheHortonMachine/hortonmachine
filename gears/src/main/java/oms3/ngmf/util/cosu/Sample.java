/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu;

/**
 *
 * @author od
 */
class Sample {

    double[] x;
    double fx;

    Sample() {
    }

    Sample(double[] x, double fx) {
        this.fx = fx;
        if (x == null) {
            return;
        }
        this.x = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            this.x[i] = x[i];
        }
    }

    @Override
    public Sample clone() {
        Sample cpy = new Sample();
        cpy.x = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            cpy.x[i] = x[i];
        }
        cpy.fx = fx;
        return cpy;
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < x.length; i++) {
            s += x[i] + "\t";
        }
        return s += fx;
    }
}