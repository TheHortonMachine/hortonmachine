/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu;

import java.util.Random;

/**
 *	Dynamically dimensioned Search (DDS) version 1.1 algorithm by Bryan Tolson
 *	Fortran version (original was coded in Matlab)
 *	Coded in Nov 2005 by Bryan Tolson
 *
 *	DDS is an n-dimensional continuous global optimization algorithm.  It is coded as a
 *	minimizer but built into the code is a hidden transformation to make it capable of
 *	solving a maximization problem.  In a maximization problem, the algorithm minimizes
 *	the negative of the objective function F (-1*F).  User specifies in inputs
 *	whether it is a max or a min problem.
 *
 * REFERENCE FOR THIS ALGORITHM:
 * Tolson, B. A., and C. A. Shoemaker (2007), Dynamically dimensioned search algorithm
 * for computationally efficient watershed model calibration, Water Resour. Res., 43,
 * W01413, doi:10.1029/2005WR004723.
 *
 * @author od (java translation)
 */
public class DDS {

    int user_seed;
    int maxiter;
    String runname, ini_name;
    String[] DVnames;
    double r_val;
    int num_dec = 0;
    double[] s_min = null;
    double[] s_max = null;
    double[] ini_soln = null;
    double[] initials = null;

//	DDS Output declarations
    double[] Ftests;
    double[] Fbests;
    double[] sbest;
    double[] stest = null;
    double[][] stests = null;
    double Fbest, to_max;
    int[] f_count;
    Random rand = new Random();

    void dds() {

        int ini_fevals = Math.max(5, (int) Math.round(0.005 * maxiter));
        int ileft = maxiter - ini_fevals;

        double fvalue = 0;
        double Ftest;

        for (int i = 0; i < ini_fevals; i++) {
            // sample an initial solution candidate (uniform random sampling):
            for (int j = 0; j < num_dec; j++) {
                double ranval = rand.nextDouble();
                stest[j] = s_min[j] + ranval * (s_max[j] - s_min[j]);
            }
            // Evaluate solution and return objective function value (fvalue), for example see grie10.f
            fvalue = obj_func(stest);
            Ftest = to_max * fvalue;  // to_max is 1.0 for MIN problems, -1 for MAX problems
            if (i == 1) {
                // Fbest must be initialized
                // track best solution found so far and corresponding obj function value
                Fbest = Ftest;
                sbest = stest;
            }
            if (Ftest <= Fbest) {
                // update current (best) solution
                Fbest = Ftest;
                sbest = stest;
            }

            // accumulate DDS initialization outputs
            f_count[i] = i;
            Ftests[i] = to_max * Ftest;  // candidate solution objective function value (untransformed)
            Fbests[i] = to_max * Fbest;  // best current solution objective function value (untransformed)
            stests[i] = stest;       // candidate solution (decision variable values)

//		write(*,*) f_count(i), to_max*Fbest ! *** user uncomment if desired ***
        }

        for (int i = 0; i < ileft; i++) {

            double Pn = 1.0 - Math.log((double) i+1) / Math.log((double) ileft); // probability each DV selected
            int dvn_count = 0; // counter for how many DVs selected for perturbation

            stest = sbest;  // define stest initially as best current solution

            for (int j = 0; j < num_dec; j++) {
                double ranval = rand.nextDouble();
                if (ranval < Pn) {
                    dvn_count++;
                    // call 1-D perturbation function to get new DV value (new_value)
                    double new_value = neigh_value(sbest[j], s_min[j], s_max[j], r_val);
                    // note that r_val is the value of the DDS r perturbation size parameter (0.2 by default)
                    stest[j] = new_value; //change relevant DV value in stest
                }
            }
            if (dvn_count == 0) {
                // no DVs selected at random, so select ONE
                double ranval = rand.nextDouble();
                int dv = (int) Math.ceil(num_dec * ranval);
                // call 1-D perturbation function to get new DV value (new_value)
                double new_value = neigh_value(sbest[dv], s_min[dv], s_max[dv], r_val);
                stest[dv] = new_value; //change relevant DV value in stest
            }

            //	Evaluate obj function value (fvalue) for stest, for example see grie10.f:
            fvalue = obj_func(stest);
            Ftest = to_max * fvalue;  // to_max handles min (=1) and max (=-1) problems,

            if (Ftest <= Fbest) { // update current best solution
                Fbest = Ftest;
                sbest = stest;
            }
            // accumulate DDS search history
            int ind1 = i + ini_fevals; // proper index for storage
            f_count[ind1] = ind1;
            Ftests[ind1] = to_max * Ftest;
            Fbests[ind1] = to_max * Fbest;
            stests[ind1] = stest;
        }
    }

    /**
     *	Purpose is to generate a neighboring decision variable value for a single
     *	decision variable value being perturbed by the DDS optimization algorithm.
     *	New DV value respects the upper and lower DV bounds.
     *	Coded by Bryan Tolson, Nov 2005.

     * I/O variable definitions:
     *	x_cur - current decision variable (DV) value
     *	x_min - min DV value
     *	x_max - max DV value
     *	r  - the neighborhood perturbation factor
     *	new_value - new DV variable value (within specified min and max)
     */
    private double neigh_value(double x_cur, double x_min, double x_max, double r) {

        double ranval, zvalue, new_value;
        double work3, work2 = 0, work1 = 0;

        double x_range = x_max - x_min;

// ------------ generate a standard normal random variate (zvalue) -------------------

        // perturb current value with normal random variable
//	CALL DRNNOA(1,zvalue)  // generates a standard normal random deviate
        // ISML Stat Library 2 routine - Acceptance/rejection

//	Below returns a standard Gaussian random number based upon Numerical recipes gasdev and
//	Marsagalia-Bray Algorithm
        work3 = 2.0;

        while (work3 >= 1.0 || work3 == 0.0) {
            ranval = rand.nextDouble();
            work1 = 2.0 * ranval - 1.0;
            ranval = rand.nextDouble();
            work2 = 2.0 * ranval - 1.0;
            work3 = work1 * work1 + work2 * work2;
        }
        // was dlog() !!!
        work3 = Math.pow((-2.0 * Math.log(work3)) / work3, 0.5);  // natural log

        // pick one of two deviates at random (don't worry about trying to use both):
        ranval = rand.nextDouble();
        if (ranval < 0.5) {
            zvalue = work1 * work3;
        } else {
            zvalue = work2 * work3;
        }

// ------------ done standard normal random variate generation -----------------
        // calculate new decision variable value:
        new_value = x_cur + zvalue * r * x_range;

        //  check new value is within DV bounds.  If not, bounds are reflecting.
        if (new_value < x_min) {
            new_value = x_min + (x_min - new_value);
            if (new_value > x_max) {
                // if reflection goes past x_max { value should be x_min since
                // without reflection the approach goes way past lower bound.
                // This keeps x close to lower bound when x_cur is close to lower bound
                //  Practically speaking, this should never happen with r values <0.3
                new_value = x_min;
            }
        } else if (new_value > x_max) {
            new_value = x_max - (new_value - x_max);
            if (new_value < x_min) {
                // if reflection goes past x_min { value should be x_max for same reasons as above
                new_value = x_max;
            }
        }
        return new_value;
    }

    /*
     *  This is the Griewank Function (2-D or 10-D)
     *  Bound: X(i)=[-600,600], for i=1,2,...,10
     *  Global minimum: 0, at origin

     *	Coded originally by Q Duan.  Editted for incorporation into Fortran DDS algorithm by
     *	Bryan Tolson, Nov 2005.

     * DDS users should make their objective functions follow this framework
     * user function arguments must be the same as above for the Griewank function

     * I/O Variable definitions:
     *	x_values	an array of decision variable values (size nopt)
     *	return		the value of the objective function with x_values as input
     */
    private double obj_func(double[] x_values) {
        int nopt = x_values.length;
        double d = (nopt == 2) ? 200.0 : 4000.0;
        double u1 = 0.0;
        double u2 = 1.0;
        for (int i = 0; i < nopt; i++) {
            u1 += (x_values[i] * x_values[i]) / d;
            u2 *= Math.cos(x_values[i] / Math.sqrt((double) i+1));
        }
        return u1 - u2 + 1;
    }
}
