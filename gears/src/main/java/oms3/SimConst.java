/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * All constants to be used for creating simulations.
 *
 * @author od
 */
public class SimConst {

    public static String now(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(new Date());
    }

    private SimConst() {
    }

    /** Calibration type: individual parameter values are used for calibration */
    public static final String INDIVIDUAL = "individual";
    /** Calibration type: parameter values are binary */
    public final static String BINARY = "binary";

    // TimeSteps
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
    public static final int SIMPLE = 0;   // Simple output: outdir + simname
    public static final int NUMBERED = 1; // Numbered output: outdir + simname + next#
    public static final int TIME = 2; // Time stamp output: outdir + simname + time

// Efficiencies
    /**absolute difference */
    public static final String ABSDIF = "absdif ";
    /** log of absolute difference */
    public static final String LOGABSDIF = "logabsdif ";
    /** Nash-Suttcliffe  */
    public static final String NS = "ns1 ";
    /** Nash-Sutcliffe log */
    public static final String LOGNS = "logns1 ";
    /** Nash-Sutcliffe log pow 2 */
    public static final String LOGNS2 = "logns2 ";
    /** Index of Agreement */
    public static final String IOA = "ioa1 ";
    /** Index of Agreement pow 2 */
    public static final String IOA2 = "ioa2 ";
    public static final String R2 = "r2 ";
    public static final String GRAD = "grad ";
    public static final String WR2 = "wr2 ";
    public static final String DSGRAD = "dsgrad ";
    /** Average volume error */
    public static final String AVE = "ave ";
    /** Root mean square error */
    public static final String RMSE = "rmse ";
    /** BIAS */
    public static final String PBIAS = "pbias ";
    /** Pearson correlation */
    public static final String PMCC = "pmcc ";
    /** transformed root mean square error */
    public static final String TRMSE = "trmse ";
    /** Runoff coefficient error */
    public static final String ROCE = "roce ";


    public static final String STACKED = "stacked";
    public static final String COMBINED = "combined";
    public static final String MULTI = "multi";


}
