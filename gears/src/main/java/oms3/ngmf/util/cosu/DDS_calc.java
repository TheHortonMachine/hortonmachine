/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu;

public class DDS_calc {

    public static void main(String[] args) {
        //test 1
        double par_All[] = {0.5000, 0.5000, 0.5000, 0.5000};
        double IndexOfSelectParameters[] = {1, 2, 3, 4};
        double dL_Selected[] = {0, 0, 0, 0};
        double dU_Selected[] = {1, 1, 1, 1};
        double IPS[] = {0, 0, 0, 0};

        double Para_Perturb = 0.2000; // ??
        int Max_runs = 500;
        int TotalP = 4;
        double Total_par = 4;
        double InitialSolution = 1;  // ??
        double RestartMechanism = 0;

        //test 2
        //double par_All[] = { 500,500,500,500,500,500,500,500,500,500 };
        //double IndexOfSelectParameters[] = { 1,2,3,4,5,6,7,8,9,10 };
        //double dL_Selected[] = { -600,-600,-600,-600,-600,-600,-600,-600,-600,-600 };
        //double dU_Selected[] = { 600,600,600,600,600,600,600,600,600,600 };
        //double IPS[] = { 0,0,0,0,0,0,0,0,0,0 };

        //double Max_runs = 2000.000;
        //double TotalP = 10.000;
        //double Total_par = 10;

        double P[] = new double[Max_runs];
        double Ftest, Fbest = 0, RI_tempt, RI_interval, RI_UB_counter, RI_LB_counter;
        int icall = 0, RI = 0, swap = 0, ISP_count = 0, count_select = 0;

        for (int i = 0; i <= Total_par - 1; i++) { 			// Only update selected parameters
            if (ISP_count <= TotalP - 1) {
                if (i == IndexOfSelectParameters[ISP_count] - 1) {
                    ISP_count = ISP_count + 1;
                    IPS[count_select] = par_All[i];
                    count_select = count_select + 1;
                }
            }
        }

        ISP_count = 0; 							// Initial stored points counting index
        count_select = 0;

        int Ini_runs = (int) Math.ceil(0.005 * Max_runs); 	// Initial model runs for DDS procedU_Selectedre
        int Tol_runs = Max_runs - Ini_runs;          // Actual total model runs of DDS loop
        double stest[] = new double[TotalP];            // Matrix allocation of proposed para set
        double sbest[] = new double[TotalP];

        if (RestartMechanism == 0) {
            if (InitialSolution == 0) { // Case 1
                for (int j = 0; j <= Ini_runs - 1; j++) {
                    for (int i = 0; i <= TotalP - 1; i++) {
                        stest[i] = dL_Selected[i] + Math.random() * (dU_Selected[i] - dL_Selected[i]);
                    }

                    for (int k = 0; k <= Total_par - 1; k++) {	// Only update selected parameters
                        if (ISP_count <= TotalP - 1) {
                            if (k == IndexOfSelectParameters[ISP_count] - 1) {
                                ISP_count++;
                                par_All[k] = stest[count_select];
                                count_select++;
                            }
                        }
                    }
                    icall = j;
                    Ftest = model_objfcn(par_All);

                    // display
                    System.out.println("Call number = " + j + "  &&  OF = " + Ftest);
                    if (j == 1) {
                        sbest = stest.clone();           		// Current best para set
                        Fbest = Ftest;           		// Current best objective value
                    } else if (j >= 1) {
                        if (Ftest <= Fbest) {
                            sbest = stest.clone();
                            Fbest = Ftest;
                        }
                    }
                }
            } else if (InitialSolution == 1) { 			// Case 2
                for (int k = 0; k <= Total_par - 1; k++) {	// Only update selected parameters
                    if (ISP_count <= TotalP - 1) {
                        if (k == IndexOfSelectParameters[ISP_count] - 1) {
                            ISP_count++;
                            par_All[k] = IPS[count_select];
                            count_select++;
                        }
                    }
                }
                sbest = IPS.clone();
                icall = 1;
                Fbest = model_objfcn(par_All);

                // display
                System.out.println("Call number = " + 1 + "  &&  OF = " + Fbest);
            }
        }

        // II. DDS Updating Procedure
        for (int i = 0; i <= Tol_runs - 1; i++) {

            // Probability of decision variables being selected
            P[i] = 1 - Math.log(i+1)/Math.log(Tol_runs);

            // Perturbation criteria
            // 1.randomly identified in total population
            // 2.certain number of points in each DV set

            int dvn_counter = 0; 					// counter of how many DV selected for perturbation
            stest = sbest.clone();

            for (int j = 0; j <= TotalP - 1; j++) {
                double RandomV = Math.random();
                if (RandomV < P[i]) {
                    stest[j] = perturbation(stest[j], dU_Selected[j], dL_Selected[j], Para_Perturb);
                    dvn_counter++;
                }
            }

            if (dvn_counter == 0) {					// When P(i) is low, perturb at least one DV
                RI_tempt = Math.random();
                RI_interval = 1 / TotalP;
                RI_UB_counter = RI_interval;
                RI_LB_counter = 0;
                for (int j = 0; j <= TotalP - 1; j++) {
                    if (RI_tempt <= RI_UB_counter && RI_tempt >= RI_LB_counter) {
                        RI = j;
                    }
                    RI_UB_counter = RI_UB_counter + RI_interval;
                    RI_LB_counter = RI_LB_counter + RI_interval;
                }
                //RI = (int)(Math.random() * TotalP); 	// One random index of DV
                stest[RI] = perturbation(stest[RI], dU_Selected[RI], dL_Selected[RI], Para_Perturb);
            }

            ISP_count = 0; 							// Initial stored points counting index
            count_select = 0;
            for (int k = 0; k <= Total_par - 1; k++) {		// Only update selected parameters
                if (ISP_count <= TotalP - 1) {
                    if (k == IndexOfSelectParameters[ISP_count] - 1) {
                        ISP_count++;
                        par_All[k] = stest[count_select];
                        count_select++;
                    }
                }
            }
            icall++;
            Ftest = model_objfcn(par_All);

            // display the latest result value, not the current best!
            System.out.println("Call number = " + icall + "  &&  OF = " + Ftest);

            if (Ftest <= Fbest) {
                sbest = stest.clone();
                Fbest = Ftest;
                swap++;
            }
        }
    }

    static double perturbation(double stest, double dUtempt, double dLtempt, double Para_Perturb) {
        double k1 = 0;
        double k2 = 0;
        double k5;

        // Method 1:
        // Standard Gaussian random number based upon Numerical recipes gasdev and
        // Marsaglia-Bray Algorithm
        double k3 = 2.0;
        while (k3 >= 1.0 || k3 == 0.0) {
            k1 = 2.0 * Math.random() - 1.0;
            k2 = 2.0 * Math.random() - 1.0;
            k3 = k1 * k1 + k2 * k2;
        }
        
        k3 = Math.pow((-2.0 * Math.log(k3) / k3), 0.5);
        double k4 = Math.random();
        if (k4 < 0.5) {
            k5 = k1 * k3;
        } else {
            k5 = k2 * k3;
        }

        // DDS perturbation parameter
        double s_new = stest + Para_Perturb * (dUtempt - dLtempt) * k5;

        // Generate normally distributed random number by MATLAB function
        // s_new = stest + k6 * (dUtempt-dLtempt) * normrnd(0,1) ;
        // Check if s_new is overshooting the feasible bound
        if (s_new > dUtempt) {            	// Upper bound management
            s_new = dUtempt - (s_new - dUtempt);
            if (s_new < dLtempt) {
                s_new = dUtempt;
            }
        } else if (s_new < dLtempt) {      	// Lower bound management
            s_new = dLtempt + (dLtempt - s_new);
            if (s_new > dUtempt) {
                s_new = dLtempt;
            }
        }

        //System.out.println(k5);
        return s_new;
    }

    public static double model_objfcn(double x[]) {
        double x1 = x[0];
        double x2 = x[1];
        double x3 = x[2];
        double x4 = x[3];
        double y = x1 + (2 * x2) + Math.pow(x3, 2) + (2 * Math.pow(x4, 2));
        return y;
    }
}
