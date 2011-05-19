package oms3.dsl;

import oms3.SimConst;


/**
 * 
 * @author od
 */
public class Param implements Buildable {

    String name;
    Object value;

    double lower = Double.NaN;
    double upper = Double.NaN;

    String strategy = SimConst.MEAN;

    public Param(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        if (!strategy.equals(SimConst.MEAN)) {
            throw new IllegalArgumentException("MEAN only supported.");
        }
        this.strategy = strategy;
    }

    public double getLower() {
        return lower;
    }
    public double getUpper() {
        return upper;
    }

    public void setLower(double lower) {
        this.lower = lower;
    }

    public void setUpper(double upper) {
        this.upper = upper;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    // called by groovy on xxx(abc:v) value
    public void call(Object value) {
        this.value = value;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }
}
