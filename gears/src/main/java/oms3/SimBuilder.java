/*
 * $Id: SimBuilder.java dba0d06e37a0 2015-04-14 odavid@colostate.edu $
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
package oms3;

import groovy.lang.Closure;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.ServiceLoader;
import oms3.dsl.AbstractSimulation;
import oms3.dsl.Buildable;
import oms3.dsl.GenericBuilderSupport;

/**
 * SimBuilder class for all oms simulation DSLs
 *
 * @author od
 */
public class SimBuilder extends GenericBuilderSupport {

    /**
     * Calibration type: individual parameter values are used for calibration
     */
    public static final String INDIVIDUAL = "individual";
    /**
     * Calibration type: parameter values are binary
     */
    public final static String BINARY = "binary";
    public final static String RAWPARAM = "rawparam";
    // TimeSteps
    public static final String RAW = "raw";
    public static final String TIME_STEP = "user_time";
    public static final String DAILY_MEAN = "daily_mean";
    public static final String MONTHLY_MEAN = "monthly_mean";
    public static final String MEAN_MONTHLY = "mean_monthly";
    public static final String ANNUAL_MEAN = "annual_mean";
    public static final String PERIOD_MEAN = "period_mean";
    public static final String PERIOD_MEDIAN = "period_median";
    public static final String PERIOD_STANDARD_DEVIATION = "period_standard_deviation";
    public static final String PERIOD_MININUM = "period_mininum";
    public static final String PERIOD_MAXIMUM = "period_maximum";
// Summary
    public static final String DAILY = "daily";
    public static final String WEEKLY = "weekly";
    public static final String MONTHLY = "monthly";
    public static final String YEARLY = "yearly";
    //
    public static final String MEAN = "mean ";
    public static final String MIN = "min ";
    public static final String MAX = "max ";
    public static final String COUNT = "count ";
    public static final String RANGE = "range ";
    public static final String MEDIAN = "median ";
    public static final String STDDEV = "stddev ";
    public static final String VAR = "variance ";
    public static final String MEANDEV = "meandev ";
    public static final String SUM = "sum ";
    public static final String PROD = "product ";
    public static final String Q1 = "q1 ";
    public static final String Q2 = "q2 ";
    public static final String Q3 = "q3 ";
    public static final String LAG1 = "lag1 ";
// Output options
    public static final int SIMPLE = 0;     // Simple output: outdir + simname
    public static final int NUMBERED = 1;   // Numbered output: outdir + simname + next#
    public static final int TIME = 2;       // Time stamp output: outdir + simname + time
// Efficiencies & OF
    /**
     * absolute difference
     */
    public static final String ABSDIF = "absdif ";
    /**
     * log of absolute difference
     */
    public static final String ABSDIFLOG = "absdiflog ";
    /**
     * Average volume error
     */
    public static final String AVE = "ave ";
    /**
     * Index of Agreement
     */
    public static final String IOA = "ioa1 ";
    /**
     * Index of Agreement pow 2
     */
    public static final String IOA2 = "ioa2 ";
    /**
     * Nash-Sutcliffe
     */
    public static final String NS = "ns1 ";
    /**
     * Nash-Sutcliffe log
     */
    public static final String NSLOG = "ns1log ";
    /**
     * Nash-Sutcliffe log pow 2
     */
    public static final String NS2LOG = "ns2log ";
    /**
     * Pearson correlation
     */
    public static final String PMCC = "pmcc ";
    /**
     * Normalized BIAS
     */
    public static final String NBIAS = "nbias ";
    /**
     * Root mean square error
     */
    public static final String RMSE = "rmse ";
    /**
     * mean square error
     */
    public static final String MSE = "mse ";
    /**
     * transformed root mean square error
     */
    public static final String TRMSE = "trmse ";
    /**
     */
    public static final String FLF = "flf ";
    /**
     */
    public static final String FHF = "fhf ";
    /**
     * Kling and Gupta
     */
    public static final String KGE = "kge ";
//    
// efficiencies
    /**
     * PBIAS
     */
    public static final String PBIAS = "pbias ";
    public static final String R2 = "r2 ";
    public static final String GRAD = "grad ";
    public static final String WR2 = "wr2 ";
    public static final String DSGRAD = "dsgrad ";
    /**
     * Runoff coefficient error
     */
    public static final String ROCE = "roce ";
//    
// graphs & plots
    /**
     * Stacked plot
     */
    public static final String STACKED = "stacked";
    /**
     * combined plot
     */
    public static final String COMBINED = "combined";
    /**
     * multi plot
     *
     */
    public static final String MULTI = "multi";

    public static Object sim_run(LinkedHashMap props, Closure closure) throws Exception {
        return runSimulation("sim", props, closure);
    }

    public static Object ps_run(LinkedHashMap props, Closure closure) throws Exception {
        return runSimulation("ps", props, closure);
    }

    public static Object esp_run(LinkedHashMap props, Closure closure) throws Exception {
        return runSimulation("esp", props, closure);
    }

    public static Object luca_run(LinkedHashMap props, Closure closure) throws Exception {
        return runSimulation("luca", props, closure);
    }

    public static Object fast_run(LinkedHashMap props, Closure closure) throws Exception {
        return runSimulation("fast", props, closure);
    }

    /*
     * Groovy equvalent Object sim(java.util.LinkedHashMap name, closure) { def
     * sb = new oms3.SimBuilder(logging:'OFF'); def s = sb.sim(name, closure);
     * s.run() return s; }
     */
    private static Object runSimulation(String name, LinkedHashMap props, Closure closure) throws Exception {
        SimBuilder sb = new oms3.SimBuilder();
//        if (props.get("logging") != null) {
//            sb.setLogging(String.valueOf(props.get("logging")));
//            props.remove("logging");
//        }
        Object simu = sb.invokeMethod(name, new Object[]{props, closure});
        Object model = ((AbstractSimulation) simu).run();
        ((AbstractSimulation) simu).setModelComponent(model);
        return simu;
    }

    static private ServiceLoader<DSLProvider> dslServices = ServiceLoader.load(DSLProvider.class);

    @Override
    protected Class<? extends Buildable> lookupTopLevel(Object n1) {
        String cl = null;
        String name = n1.toString();
        
        // check the SPI first, this way DSLs can be overwritten
        for (DSLProvider dslService : dslServices) {
            cl = dslService.getClassName(name);
            if (cl != null) {
                break;
            }
        }
        
        // check the local DSLs
        if (cl == null) {
            if ("sim".equals(name)) {
                cl = "oms3.dsl.Sim";
            } else if ("esp".equals(name)) {
                cl = "oms3.dsl.esp.Esp";
            } else if ("luca".equals(name)) {
                cl = "oms3.dsl.cosu.Luca";
            } else if ("fast".equals(name)) {
                cl = "oms3.dsl.cosu.Fast";
            } else if ("dds".equals(name)) {
                cl = "oms3.dsl.cosu.DDS";
            } else if ("ps".equals(name)) {
                cl = "oms3.dsl.cosu.ParticleSwarm";
            } else if ("glue".equals(name)) {
                cl = "oms3.dsl.cosu.Glue";
            } else if ("tests".equals(name)) {
                cl = "oms3.dsl.Tests";
            } else if ("chart".equals(name)) {
                cl = "oms3.dsl.analysis.Chart";
            }
        }

        if (cl == null) {
            throw new ComponentException("unknown element '" + name + "'");
        }

        try {
            return (Class<? extends Buildable>) Class.forName(cl);
        } catch (ClassNotFoundException ex) {
            throw new Error("DSL Not found '" + ex.getMessage() + "'");
        }
    }

    public static SimBuilder getInstance() {
        return new SimBuilder();
    }

    public static String now(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(new Date());
    }
}
