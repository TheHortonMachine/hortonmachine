/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu;

/**
 *
 * @author od
 */
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class NelderMead {

    static class SampleComperator implements Comparator<Sample> {

        private int order = 1;

        public SampleComperator(boolean decreasing_order) {
            order = decreasing_order ? -1 : 1;
        }

        @Override
        public int compare(Sample d1, Sample d2) {
            if (d1.fx < d2.fx) {
                return -1 * order;
            } else if (d1.fx == d2.fx) {
                return 0 * order;
            } else {
                return 1 * order;
            }
        }
    }
    Sample[] initialSimplex = null;
    //number of parameters!!
    int n;
    double[] lowBound;
    double[] upBound;
    double x0[] = null;
    int mode;
    Random generator = new Random();
    int currentSampleCount;
    double effValue;
    int maxn;
    double[] parameters;

    void sort(Sample[] array) {
        Arrays.sort(array, new SampleComperator(false));
    }

    double normalizedgeometricRange(Sample x[]) {
        if (x.length == 0) {
            return 0;
        }

        double min[] = new double[n];
        double max[] = new double[n];

        double mean = 0;

        for (int i = 0; i < n; i++) {
            min[i] = Double.POSITIVE_INFINITY;
            max[i] = Double.NEGATIVE_INFINITY;

            for (int j = 0; j < x.length; j++) {
                min[i] = Math.min(x[j].x[i], min[i]);
                max[i] = Math.max(x[j].x[i], max[i]);
            }
            mean += Math.log(max[i] - min[i]);
        }
        return Math.exp(mean / n);
    }

    boolean feasible(double point[]) {
        for (int i = 0; i < point.length; i++) {
            if (point[i] < lowBound[i] || point[i] > upBound[i]) {
                return false;
            }
        }
        return true;
    }

    Sample getSample(double[] x) {
        return new Sample(x, funct(x));
    }

    public double funct(double x[]) {
        //RefreshDataHandles();
        for (int j = 0; j < parameters.length; j++) {
            try {
                parameters[j] = x[j];
            } catch (Exception e) {
                throw new RuntimeException("Error! Parameter No. " + j + " wasn^t found" + e.toString());
            }
        }
        // singleRun();

        double value = effValue;
        //sometimes its a bad idea to calculate with NaN or Infty
        double bigNumber = 10000000;

        effValue = Math.max(effValue, -bigNumber);
        effValue = Math.min(effValue, bigNumber);

        if (Double.isNaN(effValue)) {
            effValue = -bigNumber;
        }

        currentSampleCount++;

        switch (mode) {
            case Efficiencies.MINIMIZATION:
                return value;
            case Efficiencies.MAXIMIZATION:
                return -value;
            case Efficiencies.ABSMINIMIZATION:
                return Math.abs(value);
            case Efficiencies.ABSMAXIMIZATION:
                return -Math.abs(value);
            default:
                return 0.0;
        }
    }

    double[] randomSample() {
        double[] sample = new double[n];
        for (int i = 0; i < n; i++) {
            sample[i] = (lowBound[i] + generator.nextDouble() * (upBound[i] - lowBound[i]));
        }
        return sample;
    }

    public void run() {
        //first draw n+1 random points
        Sample[] simplex = new Sample[n + 1];
        if (initialSimplex != null) {
            simplex = initialSimplex;
        } else {
            for (int i = 0; i < n + 1; i++) {
                if (i == 0 && x0 != null) {
                    simplex[i] = getSample(x0);
                } else {
                    simplex[i] = getSample(randomSample());
                }
            }
        }

        int m = simplex.length;
        double alpha = 1.0;
        double gamma = 2.0;
        double rho = 0.5;
        double sigma = 0.5;
        double epsilon = 0.01;
        double max_restart_count = 5;
        int restart_counter = 0;
        int iterationcounter = 0;

        while (true) {
            if (iterationcounter++ > maxn) {
                System.out.println("*********************************************************");
                System.out.println("Maximum number of iterations reached, finished optimization");
                System.out.println("Bestpoint:" + simplex[0]);
                System.out.println("*********************************************************");
                return;
            }
            if (normalizedgeometricRange(simplex) < epsilon) {
                if (max_restart_count < ++restart_counter) {
                    System.out.println("*********************************************************");
                    System.out.println("Maximum number of restarts reached, finished optimization");
                    System.out.println("Bestpoint:" + simplex[0]);
                    System.out.println("*********************************************************");
                    return;
                }
                System.out.println("restart");
                for (int i = 1; i < m; i++) {
                    simplex[i] = getSample(randomSample());
                }
            }
            sort(simplex);

            // Compute the centroid of the simplex
            double centroid[] = new double[n];
            for (int j = 0; j < n; j++) {
                centroid[j] = 0;
                for (int i = 0; i < m - 1; i++) {
                    centroid[j] += simplex[i].x[j] * (1.0 / (m - 1.0));
                }
            }

            //reflect
            double reflection[] = new double[n];
            for (int i = 0; i < n; i++) {
                reflection[i] = centroid[i] + alpha * (centroid[i] - simplex[m - 1].x[i]);
            }
            Sample reflection_sample = null;
            if (feasible(reflection)) {
                System.out.println("reflection step");
                reflection_sample = getSample(reflection);

                if (simplex[0].fx < reflection_sample.fx && reflection_sample.fx < simplex[m - 1].fx) {
                    simplex[m - 1] = reflection_sample;
                    continue;
                }
            }

            //expand
            if (feasible(reflection) && simplex[0].fx >= reflection_sample.fx) {
                double expansion[] = new double[n];
                for (int i = 0; i < n; i++) {
                    expansion[i] = centroid[i] + gamma * (centroid[i] - simplex[m - 1].x[i]);
                }
                System.out.println("expansion step");

                Sample expansion_sample = getSample(expansion);
                if (feasible(expansion) && expansion_sample.fx < reflection_sample.fx) {
                    simplex[m - 1] = expansion_sample;
                } else {
                    simplex[m - 1] = reflection_sample;
                }
                continue;
            }

            //contraction
            if (!feasible(reflection) || simplex[m - 1].fx <= reflection_sample.fx) {
                double contraction[] = new double[n];
                for (int i = 0; i < n; i++) {
                    contraction[i] = centroid[i] + rho * (centroid[i] - simplex[m - 1].x[i]);
                }
                System.out.println("contraction step");
                //this should not happen ..
                Sample contraction_sample = null;
                if (!feasible(contraction)) {
                    System.out.println("not feasible after contraction step");
                    contraction_sample = getSample(randomSample());
                } else {
                    contraction_sample = getSample(contraction);
                }
                if (contraction_sample.fx < simplex[m - 1].fx) {
                    simplex[m - 1] = contraction_sample;
                    continue;
                }
            }

            //shrink
            for (int i = 1; i < m; i++) {
                double shrink[] = new double[n];
                for (int j = 0; j < n; j++) {
                    shrink[j] = simplex[0].x[j] + sigma * (simplex[i].x[j] - simplex[0].x[j]);
                }
                System.out.println("shrink step");
                simplex[i] = getSample(shrink);
            }
        }
    }
}
