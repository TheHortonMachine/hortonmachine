/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu;

import java.util.Arrays;

/**
 *
 * @author joy,od
 */
public class Fast {

    public static void main(String[] args) {

        // FAST operation and external model parameters
        int N = 2500;    	        // number of samples
        double M = 4;      		// number of terms in the partial variances summation

        int npar = 4;   		// number of parameters
        
        double LB[] = {1, 0, 0, 0};
        double UB[] = {2, 1, 1, 1};

        // Add warning if N is less than minimum required realizations
        if (N < 2000) {
            throw new RuntimeException("The number of realizations is less than 2000.");
        }
        
        double wi = Math.floor(N / (2 * M));
        double m2 = Math.floor(wi / (2 * M));
        double r = Math.floor((m2) / npar);
        double[] w = new double[npar];

        if (r < 1) {
            for (int i = 0; i <= npar - 1; i++) {
                w[i] = 1;
            }
        } else {
            double t = Math.floor(m2 / npar);
            w[0] = 1; 
            for (int i = 1; i <= npar - 1; i++) {
                w[i] = 1 + i * t;
            }
        }

        int k1 = 0;
        double[][] w2 = new double[npar][w.length];
        for (int i = 0; i <= npar - 1; i++) {
            for (int j = 0; j <= w.length - 1; j++) {
                if (j == k1) {
                    w2[i][j] = wi;
                } else {
                    w2[i][j] = w[j];
                }
            }
            k1++;
        }

        double inc = 2 * Math.PI / N;
        double[] s = new double[N];
        s[0] = -Math.PI;
        for (int i = 1; i < s.length; i++) {
            s[i] = s[i - 1] + inc;
        }

        double[][] x = new double[N][npar];
        double[] y = new double[N];
        double[] V = new double[npar];
        double[] VT = new double[npar];
        double[] Ak = new double[(int) Math.floor((N - 1) / 2)];
        double[] Bk = new double[(int) Math.floor((N - 1) / 2)];
        double[] S_par = new double[npar];
        double[] Vex = new double[npar];
        double[] Sex_par = new double[npar];

//        System.out.println((int) Math.floor((N - 1) / 2));
//        System.out.println((N - 1) / 2);

        for (int h = 0; h <= npar - 1; h++) {
            // Compute realizations
            for (int j = 0; j <= N - 1; j++) {
                for (int i = 0; i <= npar - 1; i++) {
                    double p = 0.5 + Math.asin(Math.sin(w2[h][i] * s[j])) / Math.PI;
                    p = (double) Math.round(p * 10000) / 10000;
                    x[j][i] = p * (UB[i] - LB[i]) + LB[i];
                }
                y[j] = run_model(x[j][0], x[j][1], x[j][2], x[j][3]);
            }
            // Compute total variance
            V[h] = 0;
            for (int k = 1; k <= ((N - 1) / 2); k++) {
                double A = 0, B = 0;
                for (int j = 0; j <= N - 1; j++) {
                    if (j == 0) {
                        A = y[j] * Math.cos(s[j] * k);
                        B = y[j] * Math.sin(s[j] * k);
                    } else {
                        A += y[j] * Math.cos(s[j] * k); //#ok<AGROW>
                        B += y[j] * Math.sin(s[j] * k); //#ok<AGROW>
                    }
                    A = (double) Math.round(A * 10000) / 10000;
                    B = (double) Math.round(B * 10000) / 10000;
                }
                Ak[k - 1] = A * 2 / N;
                Bk[k - 1] = B * 2 / N;
                V[h] = V[h] + Math.pow((A * 2 / N), 2) + Math.pow((B * 2 / N), 2);
                V[h] = (double) Math.round(V[h] * 10000) / 10000;
            }
            VT[h] = V[h] / 2;
            //Compute partial variance
            V[h] = 0;
            for (int q = 1; q <= M; q++) {
                V[h] = V[h] + Math.pow(Ak[(int) (q * w2[h][h]) - 1], 2)
                        + Math.pow(Bk[(int) (q * w2[h][h]) - 1], 2);
                V[h] = (double) Math.round(V[h] * 10000) / 10000;
            }
            V[h] = V[h] / 2;
            S_par[h] = V[h] / VT[h];
            S_par[h] = (double) Math.round(S_par[h] * 1000000) / 1000000;

            //Compute Extended partial variance
            Vex[h] = 0;
            for (int q = 1; q <= M; q++) {
                for (int c = 0; c <= npar - 1; c++) {
                    if (c != h) {
                        Vex[h] = Vex[h] + Math.pow(Ak[(int) (q * w2[h][c]) - 1], 2)
                                        + Math.pow(Bk[(int) (q * w2[h][c]) - 1], 2);
                        Vex[h] = (double) Math.round(Vex[h] * 10000) / 10000;
                    }
                }
            }
            Vex[h] /= 2;
            Sex_par[h] = (1 - Vex[h] / VT[h]);
            Sex_par[h] = (double) Math.round(Sex_par[h] * 1000000) / 1000000;
        }

        //print out S values
        System.out.println("S  =" + Arrays.toString(S_par));
        System.out.println("ST =" + Arrays.toString(Sex_par));
    }

    static double run_model(double x1, double x2, double x3, double x4) {
        double y = x1 * x2 + x3 + x4;
        return y;
    }
}
