/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.cosu;

import java.util.ArrayList;
import java.util.List;

public class Morris {

    /**
     * @param args
     */
    public static void main(String[] args) {

        int k = 4; // No. of factors
        int p = 6; // No. of levels per factor (even)
        int t = 8; // No. of trajectories
        double[][] range_array = {{0, 4}, {2, 3}, {4, 5}, {1, 5}};
        Matrix range = new Matrix(range_array);

        // Create samples for Morris method
        double x[] = new double[k];
        double y_f[] = new double[k];
        double y_s[] = new double[k];
        double El_Ef[][] = new double[t][k];
        double Mat_array[][] = new double[k + 1][k];
        double w_array[][] = new double[k + 1][1];
        Matrix Mat = new Matrix(Mat_array);
        Matrix w = new Matrix(w_array);

        Matrix[] Mat_store = new Matrix[t];
        for (int i = 0; i < t; i++) {
            Mat_store[i] = new Matrix(Mat_array);
        }

        List<Double> values = new ArrayList<Double>();
        int u = 0, a = 0, r = 0;

        double inc = (double) 1 / (p - 1);
        double delta = p * inc / 2;
        double temp = 0;
        do {
            values.add(temp);
            temp = temp + inc;
        } while (temp <= 1);

        do {
            for (int j = 0; j < k; j++) {
                Mat.data[0][j] = values.get((int) (Math.floor(p * Math.random())));
            }
            int q[] = permutation(k);
            for (int j = 0; j < k; j++) {
                for (int i = 0; i < Mat.N; i++) {
                    Mat.data[j + 1][i] = Mat.data[j][i];
                }
                double mat_cal = Mat.data[j + 1][q[j]] + delta;
                Mat.data[j+1][q[j]] = (mat_cal > 1) ? (mat_cal - 2 * delta) : mat_cal;
            }

            // Check to not repeat trajectories
            int add = 1;
            if (a > 0) {
                for (int i = 0; i < a; i++) {
                    if (Mat.eq(Mat_store[i]) == true) {
                        add--;
                    }
                }
            }
            if (add == 1) {
                Mat_store[a] = Mat.copy();
                a++;
            }
        } while (a < t);

        // scale trajectories to real factor ranges
        double scale[] = new double[range.M];
        for (int i = 0; i < range.M; i++) {
            scale[i] = range.data[i][1] - range.data[i][0];
        }

        for (int i = 0; i < w.M; i++) {
            for (int j = 0; j < w.N; j++) {
                w.data[i][j] = 1;
            }
        }

        Matrix w_temp = new Matrix(new double[w.M][range.M]);
        Matrix w_temp2 = new Matrix(new double[w.M][scale.length]);

        for (int i = 0; i < w.M; i++) {
            for (int j = 0; j < range.M; j++) {
                w_temp.data[i][j] = w.data[i][0] * range.data[j][0];
                w_temp2.data[i][j] = w.data[i][0] * scale[j];
            }
        }

        for (int i = 0; i <= t - 1; i++) {
            Mat_store[i] = w_temp.plus(Mat_store[i].timesbyelement(w_temp2));
        }

        // Calculation Mean & Standard deviation of elementary effect
        for (int i = 0; i < t; i++) {
            for (int j = 0; j < k; j++) {
                for (int l = 0; l <= Mat_store[i].N - 1; l++) {
                    x[l] = Mat_store[i].data[j][l];
                }
                y_f[j] = equation(x);

                for (int l = 0; l < Mat_store[i].N; l++) {
                    x[l] = Mat_store[i].data[j + 1][l];
                }
                y_s[j] = equation(x);
                for (int l = 0; l < Mat_store[i].N; l++) {
                    if (Mat_store[i].data[j][l] != Mat_store[i].data[j + 1][l]) {
                        u = l;
                        break;
                    }
                }
                El_Ef[r][u] = (y_s[j] - y_f[j]) / delta;
            }
            r++;
        }

        double Mean[] = new double[k];
        double Std[] = new double[k];
        for (int l = 0; l < k; l++) {
            double tmp = 0;
            for (int i = 0; i <El_Ef.length; i++) {
                tmp += Math.abs(El_Ef[i][l]);
            }
            Mean[l] = tmp / El_Ef.length;
            double squareSum = 0;
            for (int i = 0; i < El_Ef.length; i++) {
                squareSum += Math.pow(Math.abs(El_Ef[i][l]) - Mean[l], 2);
            }
            Std[l] = Math.sqrt(squareSum / (El_Ef.length - 1));
        }

        System.out.println("Mean");
        for (int i = 0; i < Mean.length; i++) {
            System.out.println(" Factor " + i+1 + " = " + Mean[i]);
        }
        System.out.println();
        System.out.println("STD");
        for (int i = 0; i < Std.length; i++) {
            System.out.println(" Factor " + i+1 + " = " + Std[i]);
        }
    }

    static double equation(double x[]) {
        return x[0] * 2 * x[2] * 4 + Math.pow(x[1], 3) * x[2] * 4 + x[2] * 4 + x[3];
    }

    static private int[] permutation(int N) {
        int[] a = new int[N];
        // insert integers 0..N-1
        for (int i = 0; i < N; i++) {
            a[i] = i;
        }

        // shuffle
        for (int i = 0; i < N; i++) {
            int r = (int) (Math.random() * (i + 1));     // int between 0 and i
            int swap = a[r];
            a[r] = a[i];
            a[i] = swap;
        }
        return a;
    }
}


