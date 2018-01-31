package oms3.ngmf.util.cosu;

import java.io.PrintWriter;
import java.util.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Arrays.*;

/**
 *
 * @author Christian Fischer, based on the original MatLab sources
 * from SAHRA Tuscon Arizona
 */
public class SCE {

    public String parameterIDs;
    public String boundaries;
    public String effMethodName;
    public double[] prediction;
    public double[] observation;
    public int MaximizeEff;
    public int NumberOfComplexes;
    public int maxn;
    public int kstop;
    public double pcento;
    public double peps;
    public boolean enable;
    public String sceFileName;
    double[] parameters;
    String[] parameterNames;
    double[] lowBound;
    double[] upBound;
    int currentCount;
    Random generator = new Random();
    PrintWriter writer;

    static class SCE_Comparator implements Comparator<double[]> {

        int col = 0;
        int order = 1;

        SCE_Comparator(int col, boolean decreasing_order) {
            this.col = col;
            if (decreasing_order) {
                order = -1;
            } else {
                order = 1;
            }
        }

        @Override
        public int compare(double[] b1, double[] b2) {
            if (b1[col] < b2[col]) {
                return -1 * order;
            } else if (b1[col] == b2[col]) {
                return 0 * order;
            } else {
                return 1 * order;
            }
        }
    }
    int N; //parameter dimension
    int p; //number of complexes
    int s; //population size
    int m; //complex size; floor(s/q)
    int icall = 0;

    public void init() {
//            //initialising output file
//            writer = new GenericDataWriter(getModel().getWorkspaceDirectory().getPath() + "/" + sceFileName.getValue());
//            writer.addComment("SCE output");
//            for (int p = 0; p < this.parameterNames.length; p++) {
//                writer.addColumn(this.parameterNames[p]);
//            }
//            writer.addColumn(this.effMethodName.getValue());
//            writer.addColumn("model runs");
//            writer.writeHeader();
//            writer.flush();
    }

    public double custom_rand() {
        return generator.nextDouble();
    }

    private double[] randomSampler() {
        int paras = parameterNames.length;
        double[] sample = new double[paras];
        for (int i = 0; i < paras; i++) {
            double d = custom_rand();
            sample[i] = (lowBound[i] + d * (upBound[i] - lowBound[i]));
        }
        return sample;
    }

    private boolean IsSampleValid(double[] sample) {
        int paras = parameterNames.length;
        for (int i = 0; i < paras; i++) {
            if (sample[i] < lowBound[i] || sample[i] > upBound[i]) {
                return false;
            }
        }
        return true;
    }

    public double funct(double x[]) {
        for (int j = 0; j < parameters.length; j++) {
            parameters[j] = x[j];
        }

        //model run
//        singleRun();
        currentCount++;

        //getting rid of pairs which contain missing data values
        double[] preArr = prediction;
        double[] obsArr = observation;

        Vector<Double> obsVector = new Vector<Double>();
        Vector<Double> preVector = new Vector<Double>();
        for (int i = 0; i < preArr.length; i++) {
            //consider valid values only
            if (preArr[i] > -9999 && obsArr[i] > -9999) {
                obsVector.add(obsArr[i]);
                preVector.add(preArr[i]);
            }
        }
        int dataCount = obsVector.size();
        obsArr = new double[dataCount];
        preArr = new double[dataCount];

        //converting Vectors to arrays
        for (int i = 0; i < dataCount; i++) {
            obsArr[i] = obsVector.get(i).doubleValue();
            preArr[i] = preVector.get(i).doubleValue();
        }

        //efficiency calculation
        if (effMethodName.equals("e2")) {
            return (-1 * Efficiencies.nashSutcliffe(preArr, obsArr, 2));
        } else if (effMethodName.equals("e1")) {
            return (-1 * Efficiencies.nashSutcliffe(preArr, obsArr, 1));
        } else if (effMethodName.equals("le2")) {
            return (-1 * Efficiencies.nashSutcliffeLog(obsArr, preArr, 2));
        } else if (effMethodName.equals("pbias")) {
            return (Math.abs(Efficiencies.pbias(obsArr, preArr)));
        } else {
            return -9999;
        }
    }

    public void sort(double x[][], double xf[]) {
        if (x.length == 0) {
            return;
        }
        int n = x[0].length;
        double t[][] = new double[x.length][n + 1];
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < n; j++) {
                t[i][j] = x[i][j];
            }
            t[i][n] = xf[i];
        }
        SCE_Comparator comparator = new SCE_Comparator(n, false);
        Arrays.sort(t, comparator);
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < n; j++) {
                x[i][j] = t[i][j];
            }
            xf[i] = t[i][n];
        }
    }

    public void sort(int x[]) {
        Arrays.sort(x);
    }

    public double normalizedgeometricRange(double x[][], double bound[]) {
        if (x.length == 0) {
            return 0;
        }
        int n = x[0].length;
        double min[] = new double[n];
        double max[] = new double[n];

        double mean = 0;
        for (int i = 0; i < n; i++) {
            min[i] = Double.POSITIVE_INFINITY;
            max[i] = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < x.length; j++) {
                if (x[j][i] < min[i]) {
                    min[i] = x[j][i];
                }
                if (x[j][i] > max[i]) {
                    max[i] = x[j][i];
                }
            }
            mean += Math.log(max[i] - min[i]) / bound[i];
        }
        mean /= n;
        return Math.exp(mean);
    }

    public double[] std(double x[][]) {
        if (x.length <= 1) {
            return null;
        }

        int n = x[0].length;
        double mean[] = new double[n];
        double var[] = new double[n];
        for (int i = 0; i < n; i++) {
            mean[i] = 0;
            for (int j = 0; j < x.length; j++) {
                mean[i] += x[j][i];
            }
            mean[i] /= n;
        }
        for (int i = 0; i < n; i++) {
            var[i] = 0;
            for (int j = 0; j < x.length; j++) {
                var[i] += (mean[i] - x[j][i]) * (mean[i] - x[j][i]);
            }
            var[i] = Math.sqrt(var[i]) / (n - 1);
        }
        return var;
    }

    public int find(int lcs[], int startindex, int endindex, int value) {
        for (int i = startindex; i < endindex; i++) {
            if (lcs[i] == value) {
                return i;
            }
        }
        return -1;
    }

    //s forms the simplex
    //sf function values of simplex
    //bl lower bound,
    // bu upper bound
    public double[] cceua(double s[][], double sf[], double bl[], double bu[]) {
        int nps = s.length;
        int nopt = s[0].length;

        int n = nps;
        int m = nopt;

        double alpha = 1.0;
        double beta = 0.5;

        // Assign the best and worst points:
        double sb[] = new double[nopt];
        double sw[] = new double[nopt];
        double fb = sf[0];
        double fw = sf[n - 1];

        for (int i = 0; i < nopt; i++) {
            sb[i] = s[0][i];
            sw[i] = s[n - 1][i];
        }

        // Compute the centroid of the simplex excluding the worst point:
        double ce[] = new double[nopt];
        for (int i = 0; i < nopt; i++) {
            ce[i] = 0;
            for (int j = 0; j < n - 1; j++) {
                ce[i] += s[j][i];
            }
            ce[i] /= (n - 1);
        }

        // Attempt a reflection point
        double snew[] = new double[nopt];
        for (int i = 0; i < nopt; i++) {
            snew[i] = ce[i] + alpha * (ce[i] - sw[i]);
        }

        // Check if is outside the bounds:
        int ibound = 0;
        for (int i = 0; i < nopt; i++) {
            if ((snew[i] - bl[i]) < 0) {
                ibound = 1;
            }
            if ((bu[i] - snew[i]) < 0) {
                ibound = 2;
            }
        }

        if (ibound >= 1) {
            snew = randomSampler();
        }

        double fnew = funct(snew);

        // Reflection failed; now attempt a contraction point:
        if (fnew > fw) {
            for (int i = 0; i < nopt; i++) {
                snew[i] = sw[i] + beta * (ce[i] - sw[i]);
            }
            fnew = funct(snew);
        }
        // Both reflection and contraction have failed, attempt a random point;
        if (fnew > fw) {
            snew = randomSampler();
            fnew = funct(snew);
        }

        double result[] = new double[nopt + 1];
        for (int i = 0; i < nopt; i++) {
            result[i] = snew[i];
        }
        result[nopt] = fnew;
        return result;
    }

    public double[] sceua(double[] x0, double[] bl, double[] bu, int maxn,
            int kstop, double pcento, double peps, int ngs, int iseed, int iniflg) {
        int nopt = x0.length;
        int npg = 2 * nopt + 1;
        int nps = nopt + 1;
        int nspl = npg;
        int mings = ngs;
        int npt = npg * ngs;

        double bound[] = new double[nopt];
        for (int i = 0; i < nopt; i++) {
            bound[i] = bu[i] - bl[i];
        }

        // Create an initial population to fill array x(npt,nopt):
        //this.generator.setSeed(iseed);
        double x[][] = new double[npt][nopt];
        for (int i = 0; i < npt; i++) {
            x[i] = randomSampler();
        }

        if (iniflg == 1) {
            x[0] = x0;
        }

        int nloop = 0;

        double xf[] = new double[npt];
        for (int i = 0; i < npt; i++) {
            xf[i] = funct(x[i]);
        }
        double f0 = xf[0];

        // Sort the population in order of increasing function values;
        sort(x, xf);

        // Record the best and worst points;
        double bestx[] = new double[nopt];
        double worstx[] = new double[nopt];
        double bestf, worstf;
        for (int i = 0; i < nopt; i++) {
            bestx[i] = x[0][i];
            worstx[i] = x[npt - 1][i];
        }
        bestf = xf[0];
        worstf = xf[npt - 1];

        // Compute the standard deviation for each parameter
        double xnstd[] = std(x);

        // Computes the normalized geometric range of the parameters
        double gnrng = normalizedgeometricRange(x, bound); //exp(mean(log((max(x)-min(x))./bound)));

        System.out.println("The Inital Loop: 0");
        System.out.println("BestF: " + bestf);
        System.out.print("BestX");

        //writer.writeLine("The Inital Loop: 0");
        //writer.writeLine("BestF: " + bestf);
        //writer.writeLine("BestX");
        for (int i = 0; i < nopt; i++) {
            System.out.print("\t\t" + bestx[i]);
        }
        for (int i = 0; i < nopt; i++) {
//                writer.addData(bestx[i]);
            }
//            writer.addData(bestf);
//            writer.addData(this.currentCount);
//            writer.writeData();
//            writer.flush();

        System.out.println("");
        System.out.println("WorstF: " + worstf);
        System.out.print("WorstX");

        //writer.writeLine("");
        //writer.writeLine("WorstF: " + worstf);
        //writer.writeLine("WorstX");
        for (int i = 0; i < nopt; i++) {
            System.out.print("\t\t" + worstx[i]);
        //    writer.write("\t\t" + worstx[i]);
        }
        System.out.println("");
        //writer.writeLine("");
        //writer.flush();
        //Check for convergency;
        if (icall >= maxn) {
            System.out.println("*** OPTIMIZATION SEARCH TERMINATED BECAUSE THE LIMIT");
            System.out.println("ON THE MAXIMUM NUMBER OF TRIALS" + maxn);
            System.out.println("HAS BEEN EXCEEDED.  SEARCH WAS STOPPED AT TRIAL NUMBER:" + icall);
            System.out.println("OF THE INITIAL LOOP!");

            writer.println("*** OPTIMIZATION SEARCH TERMINATED BECAUSE THE LIMIT");
            writer.println("ON THE MAXIMUM NUMBER OF TRIALS" + maxn);
            writer.println("HAS BEEN EXCEEDED.  SEARCH WAS STOPPED AT TRIAL NUMBER:" + icall);
            writer.println("OF THE INITIAL LOOP!");
            writer.flush();
        }

        if (gnrng < peps) {
            writer.println("THE POPULATION HAS CONVERGED TO A PRESPECIFIED SMALL PARAMETER SPACE");
            System.out.println("THE POPULATION HAS CONVERGED TO A PRESPECIFIED SMALL PARAMETER SPACE");
            writer.flush();
        }

        // Begin evolution loops:
        nloop = 0;
        double criter[] = new double[kstop];
        double criter_change = 100000;

        while (icall < maxn && gnrng > peps && criter_change > pcento) {
            nloop++;

            // Loop on complexes (sub-populations);
            for (int igs = 0; igs < ngs; igs++) {
                // Partition the population into complexes (sub-populations);
                int k1[] = new int[npg];
                int k2[] = new int[npg];
                for (int i = 0; i < npg; i++) {
                    k1[i] = i;
                    k2[i] = k1[i] * ngs + igs;
                }
                double cx[][] = new double[npg][nopt];
                double cf[] = new double[npg];
                for (int i = 0; i < npg; i++) {
                    for (int j = 0; j < nopt; j++) {
                        cx[k1[i]][j] = x[k2[i]][j];
                    }
                    cf[k1[i]] = xf[k2[i]];
                }

                //Evolve sub-population igs for nspl steps:
                for (int loop = 0; loop < nspl; loop++) {
                    // Select simplex by sampling the complex according to a linear
                    // probability distribution
                    int lcs[] = new int[nps];
                    lcs[0] = 0;
                    for (int k3 = 1; k3 < nps; k3++) {
                        int lpos = 0;
                        for (int iter = 0; iter < 1000; iter++) {
                            lpos = (int) Math.floor(npg + 0.5 - Math.sqrt((npg + 0.5) *
                                    (npg + 0.5) - npg * (npg + 1) * custom_rand()));
                            //wirklich noetig??
                            int idx = find(lcs, 0, k3, lpos);
                            if (idx == -1) {
                                break;
                            }
                        }
                        lcs[k3] = lpos;
                    }
                    sort(lcs);

                    // Construct the simplex:
                    double s[][] = new double[nps][nopt];
                    double sf[] = new double[nps];
                    for (int i = 0; i < nps; i++) {
                        for (int j = 0; j < nopt; j++) {
                            s[i][j] = cx[lcs[i]][j];
                        }
                        sf[i] = cf[lcs[i]];
                    }

                    double snew[] = new double[nopt];
                    double fnew;
                    double xnew[] = cceua(s, sf, bl, bu);
                    //icall aktualisieren!!!
                    icall++; // ????

                    for (int i = 0; i < nopt; i++) {
                        snew[i] = xnew[i];
                    }
                    fnew = xnew[nopt];

                    // Replace the worst point in Simplex with the new point:
                    s[nps - 1] = snew;
                    sf[nps - 1] = fnew;

                    //Replace the simplex into the complex;
                    for (int i = 0; i < nps; i++) {
                        for (int j = 0; j < nopt; j++) {
                            cx[lcs[i]][j] = s[i][j];
                        }
                        cf[lcs[i]] = sf[i];
                    }
                    // Sort the complex;
                    sort(cx, cf);
                } // End of Inner Loop for Competitive Evolution of Simplexes
                // Replace the complex back into the population;
                for (int i = 0; i < npg; i++) {
                    for (int j = 0; j < nopt; j++) {
                        x[k2[i]][j] = cx[k1[i]][j];
                    }
                    xf[k2[i]] = cf[k1[i]];
                }
            }  // End of Loop on Complex Evolution;
            // Shuffled the complexes;
            sort(x, xf);

            // Record the best and worst points;
            for (int i = 0; i < nopt; i++) {
                bestx[i] = x[0][i];
                worstx[i] = x[nopt - 1][i];
            }
            bestf = xf[0];
            worstf = xf[npt - 1];

            //Compute the standard deviation for each parameter
            xnstd = std(x);
            gnrng = normalizedgeometricRange(x, bound);

            System.out.println("Evolution Loop:" + nloop + " - Trial - " + icall);
            System.out.println("BESTF:" + bestf);
            System.out.print("BESTX:");

            //writer.writeLine("Evolution Loop:" + nloop + " - Trial - " + icall);
            //writer.writeLine("BESTF:" + bestf);
            //writer.writeLine("BESTX:");
            for (int i = 0; i < nopt; i++) {
                System.out.print("\t" + bestx[i]);
            //  writer.write("\t" + bestx[i]);
            }
            for (int i = 0; i < nopt; i++) {
                //System.out.print("\t\t" + bestx[i]);
//                    writer.addData(bestx[i]);
                }
//                writer.addData(bestf);
//                writer.addData(this.currentCount);
//                writer.writeData();
//                writer.flush();

            System.out.println("\nWORSTF:" + worstf);
            System.out.print("WORSTX:");

            //writer.writeLine("\nWORSTF:" + worstf);
            //writer.writeLine("WORSTX:");
            for (int i = 0; i < nopt; i++) {
                System.out.print("\t" + worstx[i]);
            //writer.write("\t" + worstx[i]);
            }
            System.out.println("");
            //writer.flush();

            // Check for convergency;
            if (icall >= maxn) {
                System.out.println("*** OPTIMIZATION SEARCH TERMINATED BECAUSE THE LIMIT");
                System.out.println("ON THE MAXIMUM NUMBER OF TRIALS " + maxn + " HAS BEEN EXCEEDED!");

                writer.println("*** OPTIMIZATION SEARCH TERMINATED BECAUSE THE LIMIT");
                writer.println("ON THE MAXIMUM NUMBER OF TRIALS " + maxn + " HAS BEEN EXCEEDED!");
                writer.flush();
            }
            if (gnrng < peps) {
                System.out.println("THE POPULATION HAS CONVERGED TO A PRESPECIFIED SMALL PARAMETER SPACE");
                writer.println("THE POPULATION HAS CONVERGED TO A PRESPECIFIED SMALL PARAMETER SPACE");
                writer.flush();
            }

            for (int i = 0; i < kstop - 1; i++) {
                criter[i] = criter[i + 1];
            }
            criter[kstop - 1] = bestf;
            if (nloop >= kstop) {
                criter_change = Math.abs(criter[0] - criter[kstop - 1]) * 100.0;
                double criter_mean = 0;
                for (int i = 0; i < kstop; i++) {
                    criter_mean += Math.abs(criter[i]);
                }
                criter_mean /= kstop;
                criter_change /= criter_mean;

                if (criter_change < pcento) {
                    System.out.println("THE BEST POINT HAS IMPROVED IN LAST " + kstop + " LOOPS BY");
                    System.out.println("LESS THAN THE THRESHOLD " + pcento + "%");
                    System.out.println("CONVERGENCY HAS ACHIEVED BASED ON OBJECTIVE FUNCTION CRITERIA!!!");

                    writer.println("THE BEST POINT HAS IMPROVED IN LAST " + kstop + " LOOPS BY");
                    writer.println("LESS THAN THE THRESHOLD " + pcento + "%");
                    writer.println("CONVERGENCY HAS ACHIEVED BASED ON OBJECTIVE FUNCTION CRITERIA!!!");
                    writer.flush();
                }
            }
        }
        System.out.println("SEARCH WAS STOPPED AT TRIAL NUMBER: " + icall);
        System.out.println("NORMALIZED GEOMETRIC RANGE = " + gnrng);
        System.out.println("THE BEST POINT HAS IMPROVED IN LAST " + kstop + " LOOPS BY " + criter_change + "%");

        writer.println("SEARCH WAS STOPPED AT TRIAL NUMBER: " + icall);
        writer.println("NORMALIZED GEOMETRIC RANGE = " + gnrng);
        writer.println("THE BEST POINT HAS IMPROVED IN LAST " + kstop + " LOOPS BY " + criter_change + "%");
        writer.flush();
        double[] retVal = new double[nopt + 1];
        for (int i = 0; i < nopt; i++) {
            retVal[i] = bestx[i];
        }
        retVal[nopt] = bestf;
        return retVal;
    }

    public void run() {
        maxn = 10000;
        kstop = 10;
        pcento = 0.01;
        peps = 0.00001;

        int iseed = 10;
        int iniflg = 0;

        System.out.println("Pcento: " + pcento);

        double bestpoint[], bestx[], bestf;
        double x0[] = randomSampler();

        //double x0[] = {-1.295,2.659,1.1,0.1649};

        bestpoint = sceua(x0, lowBound, upBound, maxn, kstop, pcento, peps, NumberOfComplexes, iseed, iniflg);

        bestx = new double[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            bestx[i] = bestpoint[i];
        }
        bestf = bestpoint[parameters.length];
    }
}