package org.jgrasstools.hortonmachine.modules.calibrations;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

public class ParticleSwarming extends JGTModel {

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("")
    @In
    public int Warmup;

    @Description("Potential ETP vector.")
    @In
    public double[] Extra_PET;

    @Description("Precipitations vector.")
    @In
    public double[] Extra_Precip;

    @Description("The limit of the time series considered in computation.")
    @In
    public double Extra_MaxT = doubleNovalue;

    @Description("Define the measured streamflow data.")
    @In
    public double[] Measurement_MeasData;

    @Description("Area of the basin.")
    @In
    public double Area;

    @Description("The first value of mesured discharge")
    @In
    @Unit("m3/s")
    public double Q0 = doubleNovalue;

    @Description("kmax")
    @In
    public int kmax;

    @Description("ModelName")
    @In
    public String ModelName = null;

    @Description("Give the parameter ranges (minimum values).")
    @In
    public double[] ParRange_minn;

    @Description("Give the parameter ranges (maximum values).")
    @In
    public double[] ParRange_maxn;

    @Description("p")
    @In
    public int p;
    @Description("parameters")
    @In
    public int parameters;

    @Description("gbest")
    @Out
    public double outOpt;

    @Description("optimal parameters")
    @Out
    public double[] outOptSet;

    public int int_Extra_MaxT;
    public double[] costvect;
    public double[][] p_best;
    public double[] g_best;
    public double g_best_value;
    public double[] p_best_value;
    public double[] hist_g_best_value;
    public double observed[];
    public double modelled[];

    @Execute
    public void process() throws Exception {
        int_Extra_MaxT = (int) Extra_MaxT;

        double xX[][] = new double[parameters][p];

        xX = UNIFORM(ParRange_minn, ParRange_maxn, p);
        double x[][] = ReflectBounds(xX, ParRange_maxn, ParRange_minn);
        // for (int i = 0; i < x.length; i++) {
        // for (int j = 0; j < x[0].length;j++){
        // System.out.print(x[i][j]+ " ");
        // }
        // System.out.println();
        //
        // }
        double VelRange_minn[] = new double[ParRange_minn.length];

        double VelRange_max[] = new double[ParRange_minn.length];
        // ipotizzo che la velocita iniziale sia inizializzata compresa tra 1/100 dei valori max e
        // min dei parametri
        for( int i = 0; i < ParRange_maxn.length; i++ ) {
            VelRange_minn[i] = ParRange_minn[i];
            VelRange_max[i] = ParRange_maxn[i];
        }

        double vel[][] = new double[parameters][p];
        vel = UNIFORM(VelRange_minn, VelRange_max, p);

        // calculate the cost of each particle
        double[] costvectold = ComputeCostFunction(x);

        int kkk = 0;
        p_best = x;
        p_best_value = costvectold;
        double min = Math.abs(costvectold[0]);
        int posmin = 0;
        for( int i = 1; i < costvectold.length; i++ ) {
            if (Math.abs(costvectold[i]) < min) {
                min = Math.abs(costvectold[i]);
                posmin = i;
            }
        }
        g_best = new double[x[0].length];
        g_best_value = min;
        for( int i = 0; i < x[0].length; i++ ) {
            g_best[i] = x[posmin][i];
        }

        hist_g_best_value = new double[kmax];
        boolean fermati = false;
        while( kkk < (kmax - 1) || !fermati ) {
            double[][] x_old = x;
            double[][] velnew = Compute_velocity(x_old, vel);
            vel = velnew;
            x = Compute_particle(x_old, velnew);
            costvect = ComputeCostFunction(x);
            p_best = Compute_pBest(x, costvect);
            g_best = Compute_gBest(x, costvect);

            hist_g_best_value[kkk] = g_best_value;

            if (kkk > 500) {
                int sum = 0;
                for( int c = 0; c < 50; c++ ) {
                    if (Math.abs(hist_g_best_value[kkk - c] - hist_g_best_value[kkk - c - 1]) < 0.001) {
                        sum = sum + 1;
                    }
                }

                if (sum > 30) {
                    fermati = true;
                    break;
                }
            }
            if (kkk > kmax - 2) {
                break;
            }

            costvectold = costvect;
            // System.out.println("ite="+kkk);
            // System.out.println("fermati="+fermati);
            kkk++;

        }
        // for(int i=0;i<g_best.length;i++){
        // System.out.println(g_best[i]);
        //
        // }
        // System.out.println(g_best_value);

        outOpt = g_best_value;
        outOptSet = g_best;
    }

    public boolean StoppingCondition( double vett[], double s ) {

        boolean result = false;
        int cnt = 0;
        for( int i = 0; i < vett.length; i++ ) {
            if (vett[i] < s) {
                cnt += 1;
            }
        }
        if (cnt == vett.length) {
            result = true;
        }

        return result;
    }

    public double[][] UNIFORM( double[] xmin, double[] xmax, int nsample ) {
        // Latin Hypercube sampling
        // double[][] LHSresult=new double [1][1];
        int nvar = xmin.length;
        double[][] s = new double[nsample][nvar];

        double[][] ran = new double[nsample][nvar];
        for( int row = 0; row < ran.length; row++ ) {

            for( int col = 0; col < ran[0].length; col++ ) {

                s[row][col] = (xmax[col] - xmin[col]) * Math.random() + xmin[col];
            }
        }

        return s;

    }

    public double[] ComputeCostFunction( double xx[][] ) throws Exception {
        double[] res = new double[xx.length];

        if (ModelName.equals("Banana")) {
            for( int numpart = 0; numpart < xx.length; numpart++ ) {
                double xuno = xx[numpart][0];
                double xdue = xx[numpart][1];
                res[numpart] = 100 * (xdue - xuno * xuno) * (xdue - xuno * xuno) + (1 - xuno) * (1 - xuno);
            }
        }

        if (ModelName.equals("Eggcrate")) {
            for( int numpart = 0; numpart < xx.length; numpart++ ) {
                double xuno = xx[numpart][0];
                double xdue = xx[numpart][1];
                res[numpart] = xuno * xuno + xdue * xdue + 25
                        * (Math.sin(xuno) * Math.sin(xuno) + Math.sin(xdue) * Math.sin(xdue));
            }
        }

        return res;

    }

    public double[] Compute_gBest( double xx[][], double[] vettcostnew ) {
        double re[] = g_best;
        int pos = 0;
        double min = g_best_value;
        for( int i = 0; i < vettcostnew.length; i++ ) {
            if (Math.abs(vettcostnew[i]) <= min) {
                g_best_value = Math.abs(vettcostnew[i]);
                min = Math.abs(vettcostnew[i]);
                pos = i;
                for( int ii = 0; ii < xx[0].length; ii++ ) {
                    re[ii] = xx[pos][ii];
                }
            }
        }

        // System.out.println("minimo="+g_best_value);
        return re;
    }

    public double[][] Compute_particle( double pos[][], double[][] vel ) {

        double xnew[][] = new double[pos.length][pos[0].length];
        for( int i = 0; i < vel.length; i++ ) {
            for( int j = 0; j < vel[0].length; j++ ) {
                xnew[i][j] = pos[i][j] + vel[i][j];
            }

        }
        double[][] xneww = ReflectBounds(xnew, ParRange_maxn, ParRange_minn);
        return xneww;
    }

    public double[][] Compute_velocity( double pos[][], double[][] vel ) {

        double velnew[][] = new double[pos.length][pos[0].length];
        for( int i = 0; i < vel.length; i++ ) {
            for( int j = 0; j < vel[0].length; j++ ) {
                double c1 = 1.5;
                double r1 = Math.random();
                double c2 = 2.5;
                double r2 = Math.random();
                double inertia = 0.5;
                velnew[i][j] = inertia * vel[i][j] + c1 * r1 * (p_best[i][j] - pos[i][j]) + c2 * r2 * (g_best[j] - pos[i][j]);
            }

        }
        return velnew;
    }

    public double[][] Compute_pBest( double currentpos[][], double[] currentbest ) {
        double pos_best[][] = p_best;
        for( int i = 0; i < currentbest.length; i++ ) {
            // per tutti
            if (Math.abs(currentbest[i]) < Math.abs(p_best_value[i])) {
                // per nash
                // if(Math.abs(currentbest[i])>Math.abs(p_best_value[i])){
                p_best_value[i] = Math.abs(currentbest[i]);
                for( int j = 0; j < currentpos[0].length; j++ ) {
                    pos_best[i][j] = currentpos[i][j];
                }
            }
        }
        return pos_best;
    }

    public double[][] ReflectBounds( double[][] neww, double[] ParRange_maxnn, double[] ParRange_minnn ) {
        // ParRange_maxnn=new double []{15000,50,0.99,0.3,0.9};
        // ParRange_minnn=new double []{1,0.01,0.01,0.0001,0.01};
        // Checks the bounds of the parameters
        // First determine the size of new
        // int nmbOfIndivs=neww.length;
        // int Dim=neww[0].length;
        double[][] y = neww;
        for( int row = 0; row < neww.length; row++ ) {
            for( int col = 0; col < neww[0].length; col++ ) {
                if (y[row][col] < ParRange_minnn[col]) {
                    y[row][col] = 2 * ParRange_minnn[col] - y[row][col];
                }
                if (y[row][col] > ParRange_maxnn[col]) {
                    y[row][col] = 2 * ParRange_maxnn[col] - y[row][col];
                }
            }
        }
        for( int row = 0; row < neww.length; row++ ) {
            for( int col = 0; col < neww[0].length; col++ ) {
                if (y[row][col] < ParRange_minn[col]) {
                    y[row][col] = ParRange_minn[col] + Math.random() * (ParRange_maxn[col] - ParRange_minn[col]);
                }
                if (y[row][col] > ParRange_maxn[col]) {
                    y[row][col] = ParRange_minn[col] + Math.random() * (ParRange_maxn[col] - ParRange_minn[col]);
                }
            }
        }

        return y;
    }

}