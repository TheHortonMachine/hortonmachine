package oms3.dsl;

import oms3.SimConst;
import oms3.dsl.cosu.Calibration;

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

    Calibration calibration;
     
    public Param(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        if ((!strategy.equals(SimConst.MEAN)) && 
            (!strategy.equals(SimConst.INDIVIDUAL)) &&
            (!strategy.equals(SimConst.BINARY)) ) {
            throw new IllegalArgumentException(strategy + "strategy not supported.");
        }
        this.strategy = strategy;
    }
    
    public double getLower() {
        return lower;
    }
    public double getUpper() {
        return upper;
    }

    public Calibration getCalibration () {
         if (calibration == null) {
            throw new IllegalArgumentException("Missing calibration argument");
        }
        return calibration;
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
        if (name.equals("calibration")) {
            this.calibration = new Calibration();
            return calibration;
        }
        else {
            throw new IllegalArgumentException(name.toString());
        }
    }
}


