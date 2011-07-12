/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.hortonmachine.modules.networktools.trento_p.net;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.CUBICMETER2LITER;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_CELERITY_FACTOR;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TMAX;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.EIGHTOVERTHREE;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.METER2CM;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.MINUTE2SEC;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.TWOOVERTHREE;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.TWO_THIRTEENOVERTHREE;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.sorting.QuickSortAlgorithm;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility;
import org.joda.time.DateTime;

/**
 * 
 * Class which contains methods to verify a sewer network.
 * 
 * <p>
 * 
 * It contains a geosewere method which verify the net..
 * 
 * </p>
 * 
 * @author Daniele Andreis, Riccardo Rigon, David Tamanini
 * 
 */
public class NetworkCalibration implements Network {
    DateTime first = null;

    /*
     * Intervallo temporale.
     */
    private Integer dt;
    /*
     * Monitor.
     */
    private final IJGTProgressMonitor pm;
    /*
     * Gestore messaggi.
     */
    private final HortonMessageHandler msg = HortonMessageHandler.getInstance();
    /*
     * Dati di pioggia.
     */
    private double[][] rainData;

    private final HashMap<DateTime, double[]> inRain;

    /*
     * Dati relativi alla rete
     */
    private final Pipe[] networkPipes;
    /*
     * celerita-.
     */
    private final double celerityfactor1;
    /*
     * The time of discharge.
     */
    private double[][] timeDischarge;
    /*
     * The
     */
    private final int tMax;
    /*
     * A string where to put the warning messages.
     */
    private final StringBuilder strBuilder;
    /*
     * The discharg for each pipe and for each time.
     */
    private HashMap<DateTime, HashMap<Integer, double[]>> discharge;
    /**
     * Builder for the Calibration class.
     */
    public static class Builder {

        // Parametri obbligatori
        private final IJGTProgressMonitor pm;
        // intervallo temporale.
        private final Integer dt;
        // max number of time step.
        private int tMax = (int) DEFAULT_TMAX;

        private final StringBuilder strBuilder;
        /*
         * The discharg for each pipe and for each time.
         */
        private final HashMap<DateTime, HashMap<Integer, double[]>> discharge;
        /*
         * Dati di pioggia.
         */

        private final HashMap<DateTime, double[]> inRain;
        /*
         * Dati relativi alla rete
         */
        private final Pipe[] networkPipe;

        // Precisione con cui vengono cercate alcune soluzioni col metodo delle
        // bisezioni.
        private double celerityfactor1 = DEFAULT_CELERITY_FACTOR;

        /**
         * Initialize the object with the needed parameters
         * 
         * 
         * @param pm
         * @param msg
         * @param networkPipe
         *            the array of pipes.
         * @param dt
         *            time step.
         * @param inRain
         *            the rain data.
         * @param outDischarge
         *            the output, discharge.
         * @param strBuilder
         *            a tring used to store the warnings.
         */
        public Builder( IJGTProgressMonitor pm, Pipe[] networkPipe, Integer dt, HashMap<DateTime, double[]> inRain,
                HashMap<DateTime, HashMap<Integer, double[]>> outDischarge, StringBuilder strBuilder ) {
            this.pm = pm;
            this.networkPipe = networkPipe;
            this.inRain = inRain;
            this.dt = dt;
            this.discharge = outDischarge;
            this.strBuilder = strBuilder;
        }

        /**
         * Set the max celerity factor.
         * 
         * @param max
         *            celerityFactor.
         */
        public Builder celerityFactor( double celerityFactor ) {
            this.celerityfactor1 = celerityFactor;
            return this;
        }

        /**
         * Set the max number of time step.
         * 
         * @param tMax
         *            .
         */
        public Builder tMax( int tMax ) {
            this.tMax = tMax;
            return this;
        }

        public NetworkCalibration build() {
            return new NetworkCalibration(this);
        }

    }

    /**
     * Set the parameter throughout the Builder,
     * 
     */
    private NetworkCalibration( Builder builder ) {
        this.dt = builder.dt;
        this.pm = builder.pm;
        this.celerityfactor1 = builder.celerityfactor1;
        this.discharge = builder.discharge;
        this.tMax = builder.tMax;
        this.strBuilder = builder.strBuilder;
        if (builder.networkPipe != null) {
            this.networkPipes = builder.networkPipe;
        } else {
            pm.errorMessage("networkPipe is null");
            throw new IllegalArgumentException("networkPipe is null");
        }
        if (builder.inRain != null) {

            this.inRain = builder.inRain;
            // create the rains array from the input.

            Set<Entry<DateTime, double[]>> rainSet = inRain.entrySet();

            DateTime second = null;
            int l = rainSet.size();
            rainData = new double[l][2];
            int index = 0;
            for( Entry<DateTime, double[]> rainRecord : rainSet ) {
                DateTime dateTime = rainRecord.getKey();
                double[] values = rainRecord.getValue();
                if (first == null) {
                    first = dateTime;
                } else if (second == null && dt == null) {
                    second = dateTime;
                }

                rainData[index][0] = index + 1;
                rainData[index][1] = values[0];
                index++;
            }

            // Evaluate the time step, if it isn't passed as a parameter,as a difference between two
            // time.

            if (dt == null) {
                dt = abs(second.getMinuteOfDay() - first.getMinuteOfDay());
            }
            // if the input has the date in a wrong order.
            if (dt <= 0) {
                pm.errorMessage(msg.message("trentoP.error.t"));
                throw new IllegalArgumentException(msg.message("trentoP.error.t"));
            }

            double tmax = 0;

            tmax = ModelsEngine.approximate2Multiple(tMax, dt);
            // initialize the output.

            int nTime = (int) (tmax / dt);
            double time = 0;
            double tmin = 0;
            tmin = rainData[0][0];
            time = tmin;
            timeDischarge = new double[nTime][networkPipes.length + 1];
            for( int i = 0; i < nTime; ++i ) {
                timeDischarge[i][0] = time;
                time += dt;
            }

        } else {
            pm.errorMessage("rainData is null");
            throw new IllegalArgumentException("rainData is null");
        }
    }

    /**
     * verify of the no-head pipes.
     * 
     * <p>
     * It evaluate the discharge.
     * </p>
     * 
     * @param k
     *            ID of the pipe where evaluate the discharge.
     * @param cDelays
     *            delay matrix (for the evalutation of the flow wave).
     * @param net
     *            matrix that contains value of the network.
     */
    private void internalPipeVerify( int k, double[] cDelays, double[][] net ) {

        int num;
        double localdelay, olddelay, Qmax, B, known, theta, u;
        double[][] qPartial;

        qPartial = new double[timeDischarge.length][timeDischarge[0].length];
        calculateDelays(k, cDelays, net);
        // First attempt local delay [min]
        localdelay = 1;
        double accuracy = networkPipes[0].getAccuracy();
        int jMax = networkPipes[0].getjMax();
        double minG = networkPipes[0].getMinG();
        double maxtheta = networkPipes[0].getMaxTheta();
        double tolerance = networkPipes[0].getTolerance();

        do {
            olddelay = localdelay;
            Qmax = 0;
            // Updates delays
            for( int i = 0; i < net.length; i++ ) {
                net[i][2] += localdelay;
            };

            for( int j = 0; j < net.length; ++j ) {
                num = (int) net[j][0];
                getHydrograph(num, qPartial, olddelay, net[j][2]);

            }

            getHydrograph(k, qPartial, olddelay, 0);
            Qmax = ModelsEngine.sumDoublematrixColumns(k, qPartial, timeDischarge, 1, qPartial[0].length - 1, pm);
            if (Qmax <= 1)
                Qmax = 1;
            // Resets delays
            for( int i = 0; i < net.length; i++ ) {
                net[i][2] -= localdelay;
            }

            B = Qmax / (CUBICMETER2LITER * networkPipes[k - 1].getKs() * sqrt(networkPipes[k - 1].verifyPipeSlope / METER2CM));
            known = (B * TWO_THIRTEENOVERTHREE) / pow(networkPipes[k - 1].diameterToVerify / METER2CM, EIGHTOVERTHREE);
            theta = Utility.thisBisection(maxtheta, known, TWOOVERTHREE, minG, accuracy, jMax, pm, strBuilder);
            // Average velocity in pipe [ m / s ]
            u = Qmax * 80 / (pow(networkPipes[k - 1].diameterToVerify, 2) * (theta - sin(theta)));
            localdelay = networkPipes[k - 1].getLenght() / (celerityfactor1 * u * MINUTE2SEC);

        } while( abs(localdelay - olddelay) / olddelay >= tolerance );

        cDelays[k - 1] = localdelay;

    }

    /**
     * Calcola il ritardo della tubazione k.
     * 
     * @param k
     *            indice della tubazione.
     * @param cDelays
     *            matrice dei ritardi.
     * @param net
     *            matrice che contiene la sottorete.
     */

    private void calculateDelays( int k, double[] cDelays, double[][] net )

    {

        double t;
        int ind, r = 1;
        for( int j = 0; j < net.length; ++j ) {
            t = 0;
            r = 1;
            ind = (int) net[j][0];
            /*
             * Area k is not included in delays
             */
            while( networkPipes[ind - 1].getIdPipeWhereDrain() != k ) {
                ind = networkPipes[ind - 1].getIdPipeWhereDrain();
                t += cDelays[ind - 1];
                r++;
            }

            if (r > networkPipes.length) {
                pm.errorMessage(msg.message("trentoP.error.incorrectmatrix"));
                throw new ArithmeticException(msg.message("trentoP.error.incorrectmatrix"));
            }
            net[j][2] = t;
        }

    }

    /**
     * Restituisce l-idrogramma.
     * 
     * @param k
     *            tratto di tubazione.
     * @param Qpartial
     *            matrice delle portate temporanee necessarie per costriure
     *            l'idrogramma.
     * @param localdelay
     *            ritardo della tubazione k.
     * @param delay
     *            ritardo temporale.
     */
    private double getHydrograph( int k, double[][] Qpartial, double localdelay, double delay )

    {

        double Qmax = 0;
        double tmin = rainData[0][0]; /* [min] */
        int j = 0;
        double t = tmin;
        double Q;
        double rain;
        for( t = tmin, j = 0; t <= tMax; t += dt, ++j ) {
            Q = 0;

            for( int i = 0; i <= (rainData.length) - 1; ++i ) {

                // [ l / s ]

                rain = rainData[i][1] * networkPipes[k - 1].getDrainArea() * networkPipes[k - 1].getRunoffCoefficient()
                        * 166.666667;

                if (t <= i * dt) {
                    Q += 0;
                } else if (t <= (i + 1) * dt) {
                    Q += rain * pFunction(k, t - i * dt, localdelay, delay);
                } else {
                    Q += rain * (pFunction(k, t - i * dt, localdelay, delay) - pFunction(k, t - (i + 1) * dt, localdelay, delay));
                }
            }

            Qpartial[j][k] = Q;
            if (Q > 0.0) {
                try {
                    networkPipes[k - 1].verifyEmptyDegree(strBuilder, Q);
                } catch (ArithmeticException e) {
                    strBuilder.append(msg.message("trentoP.warning.emptydegree") + networkPipes[k - 1].getId());
                }
            }
            if (Q >= Qmax) {
                Qmax = Q;
            }

        }
        return Qmax;

    }

    /**
     * 
     * Verify of the head pipes.
     * 
     * <p>
     * It evaluate the discharge.
     * </p>
     * 
     * @param k
     *            ID of the pipe where evaluate the discharge.
     * @param cDelays
     *            delay matrix (for the evalutation of the flow wave).
     */
    private void headPipeVerify( int k, double[] cDelays ) {

        double olddelay = 0;
        double Qmax = 0;
        double B = 0;
        double known = 0;
        double theta = 0;
        double u = 0;
        /* First attempt local delay [min] */
        double localdelay = 1;
        double accuracy = networkPipes[0].getAccuracy();
        int jMax = networkPipes[0].getjMax();
        double minG = networkPipes[0].getMinG();
        double maxtheta = networkPipes[0].getMaxTheta();
        double tolerance = networkPipes[0].getTolerance();
        do {
            olddelay = localdelay;
            Qmax = getHydrograph(k, timeDischarge, olddelay, 0);
            if (Qmax <= 1) {
                Qmax = 1;
            }
            B = Qmax
                    / (CUBICMETER2LITER * networkPipes[k - 1].getKs() * Math.sqrt(networkPipes[k - 1].verifyPipeSlope / METER2CM));
            known = (B * TWO_THIRTEENOVERTHREE) / Math.pow(networkPipes[k - 1].diameterToVerify / METER2CM, EIGHTOVERTHREE);
            theta = Utility.thisBisection(maxtheta, known, TWOOVERTHREE, minG, accuracy, jMax, pm, strBuilder);
            double tmp1 = 0;
            double tmp2 = 0;
            if (k - 1 >= 0) {
                tmp1 = networkPipes[k - 1].diameterToVerify;
                tmp2 = networkPipes[k - 1].getLenght();
            }

            // Average velocity in pipe [ m / s ]

            u = Qmax * 80 / (Math.pow(tmp1, 2) * (theta - Math.sin(theta)));
            localdelay = tmp2 / (celerityfactor1 * u * MINUTE2SEC);

        } while( Math.abs(localdelay - olddelay) / olddelay >= tolerance );

        cDelays[k - 1] = localdelay;

    }

    /**
     * 
     * restituisce la funzione p.
     * 
     * @param k
     *            tratto di tubazione esaminata.
     * @param t
     *            tempo esaminato.
     * @param localdelay
     *            ritardo nella tubazione k.
     * @param delay
     *            ritardo totale.
     * @return il valore della funxione p
     */
    private double pFunction( int k, double t, double localdelay, double delay ) {

        double P = 0;

        if (t < 0) {
            pm.errorMessage(msg.message("trentoP.error.negativeP"));
            throw new ArithmeticException(msg.message("trentoP.error.negativeP"));

        }
        if (t < delay) {
            P = 0;
        } else if (t <= (delay + localdelay)) {
            P = (t - delay) / localdelay + networkPipes[k - 1].k / localdelay
                    * (Math.exp(-(t - delay) / networkPipes[k - 1].k) - 1);
        } else {
            P = 1 + networkPipes[k - 1].k / localdelay * Math.exp(-(t - delay) / networkPipes[k - 1].k)
                    * (1 - Math.exp(localdelay / networkPipes[k - 1].k));
        }

        return P;

    }

    /**
     * Estimate the discharge for each time and for each pipes.
     * 
     * <p>
     * It work throgout 2 loop:
     * <ol>
     * *
     * <li>The first on the head pipes.
     * <li>The second on the pipes which are internal.
     * 
     * </ol>
     * </p>
     */
    @Override
    public void geoSewer() throws Exception {

        /* l Tratto che si sta progettando. */
        int l;
        /*
         * matrice che per ciascun area non di testa, contiene i dati geometrici
         * degli stati a monte, che direttamente o indirettamente, drenano in
         * esso
         */
        double[][] net;
        /*
         * contiene la magnitude dei vari stati.
         */
        double[] magnitude = new double[networkPipes.length];
        /*
         * vettore che contiene l'indice dei versanti.
         */
        double[] one = new double[networkPipes.length];
        /*
         * vettore che contiene gli stati riceventi, compresa almeno un'uscita
         */
        double[] two = new double[networkPipes.length];

        // initialize the discharge array

        for( int i = 0; i < networkPipes.length; i++ ) {
            /* Indice degli stati */
            one[i] = networkPipes[i].getId();
            /* Indice degli stati riceventi, compresa almeno un'uscita */
            two[i] = networkPipes[i].getIdPipeWhereDrain();
        }
        /* Calcola la magnitude di ciascun stato */
        Utility.pipeMagnitude(magnitude, two, pm);/*
                                                  * Calcola la magnitude di
                                                  * ciascun stato
                                                  */

        /* al vettore two vengono assegnati gli elementi di magnitude */
        for( int i = 0; i < two.length; i++ ) /*
                                              * al vettore two vengono assegnati
                                              * gli elementi di magnitude
                                              */
        {
            two[i] = magnitude[i];
        }

        /*
         * Ordina gli elementi del vettore magnitude in ordine crescente, e
         * posiziona nello stesso ordine gli elementi di one
         */
        QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
        t.sort(magnitude, one);

        int k = 0;
        // tratto che si sta analizzando o progettando
        l = (int) one[k];
        pm.beginTask(msg.message("trentoP.begin"), networkPipes.length - 1);

        double[] cDelays = new double[networkPipes.length];
        while( magnitude[k] == 1 ) {
            try {
                headPipeVerify(l, cDelays);

                // Passo allo stato successivo
                k++;

                if (k < magnitude.length) {
                    /*
                     * Il prossimo tratto da progettare, ovviamente se avra
                     * magnitude=1
                     */
                    l = (int) one[k];
                } else {
                    break;
                }
                pm.worked(1);
            } catch (ArithmeticException e) {
                strBuilder.append(msg.message("trentoP.warning.emptydegree") + networkPipes[k - 1].getId());
                break;

            }
        }

        /*
         * ----- INIZIO CICLO WHILE PER LA PROGETTAZIONE DELLE AREE NON DI TESTA
         * -----
         * 
         * Magnitude > 1 AREE NON DI TESTA
         */
        while( k < magnitude.length ) {
            try {
                net = new double[(int) (magnitude[k] - 1)][9];
                scanNetwork(k, l, one, net);
                internalPipeVerify(l, cDelays, net);

                /* Passo allo stato successivo */
                k++;
                /* se non sono arrivato alla fine */
                if (k < magnitude.length) {
                    /* Prossimo stato da progettare */
                    l = (int) one[k];
                } else {
                    break;
                }
                pm.worked(1);
            } catch (ArithmeticException e) {
                strBuilder.append(msg.message("trentoP.warning.emptydegree") + networkPipes[k - 1].getId());
                break;
            }
        }
        getDischarge();
    }
    private void getDischarge() {
        int nTime = timeDischarge.length;
        int length = timeDischarge[0].length;
        HashMap<Integer, double[]> tmpHM = new LinkedHashMap<Integer, double[]>();
        for( int i = 1; i < length; i++ ) {
            tmpHM.put(networkPipes[i - 1].getId(), new double[]{timeDischarge[0][i]});
        }
        discharge.put(first, tmpHM);
        DateTime tmp = first;
        for( int i = 1; i < nTime; ++i ) {
            tmp = tmp.plusMinutes(dt);
            tmpHM = new LinkedHashMap<Integer, double[]>();
            for( int j = 1; j < length; j++ ) {
                tmpHM.put(networkPipes[j - 1].getId(), new double[]{timeDischarge[i][j]});
            }
            discharge.put(tmp, tmpHM);
        }
    }
    /**
     * Compila la mantrice net con tutte i dati del sottobacino con chiusura nel
     * tratto che si sta analizzando, e restituisce la sua superfice
     * 
     * @param k
     *            tratto analizzato del sottobacino chiuso in l.
     * @param l
     *            chiusura del bacino.
     * @param one
     *            indice dei versanti.
     * @param net
     *            sottobacino che si chiude in l.
     */

    private double scanNetwork( int k, int l, double[] one, double[][] net ) {

        int ind;
        /*
         * t Ritardo accumulato dall'onda prima di raggiungere il tratto si sta
         * dimensionando.
         */
        double t;
        /*
         * Distanza percorsa dall'acqua dall'area dove e' caduta per raggiungere
         * l'ingresso del tratto che si sta dimensionando.
         */
        double length;
        /*
         * Superfice del sottobacino con chiusura nel tratto che si sta
         * analizzando.
         */
        double totalarea = 0;

        int r = 0;
        int i = 0;
        /*
         * In one gli stati sono in ordine di magmitude crescente. Per ogni
         * stato di magnitude inferiore a quella del tratto l che si sta
         * progettando.
         */
        for( int j = 0; j < k; j++ ) {

            /* La portata e valutata all'uscita di ciascun tratto */
            t = 0;
            /*
             * ID dello lo stato di magnitude inferiore a quello del tratto che
             * si sta progettando.
             */
            i = (int) one[j];
            ind = i;
            // la lunghezza del tubo precedentemente progettato
            length = networkPipes[ind - 1].getLenght();

            // seguo il percorso dell'acqua finch� non si incontra l'uscita.
            while( networkPipes[ind - 1].getIdPipeWhereDrain() != 0 ) {

                // lo stato dove drena a sua volta.
                ind = networkPipes[ind - 1].getIdPipeWhereDrain();
                /*
                 * se lo stato drena direttamente in quello che si sta
                 * progettando
                 */
                if (ind == l) {
                    /*
                     * ID dello stato che drena in l, piu o meno direttamente.
                     */
                    net[r][0] = i;
                    /*
                     * lunghezza del percorsa dall'acqua prima di raggiungere lo
                     * stato l che si sta progettando
                     */
                    net[r][1] = length + networkPipes[l - 1].getLenght();

                    /*
                     * Ritardo accumulato dall'onda di piena formatasi in uno
                     * degli stati a monte, prima di raggiungere il tratto l che
                     * si sta progettando
                     */
                    net[r][2] = t;

                    /*
                     * area di tutti gli stati a monte che direttamente o
                     * indirettamente drenano in l
                     */
                    totalarea += networkPipes[i - 1].getDrainArea();
                    r++;

                    break;
                }

            }
            /*
             * viene incrementato solo se l'area drena in l quindi non puo'
             * superare net->nrh
             */
            if (r > net.length)
                break;
        }

        // area degli stati a monte che drenano in l, l compreso

        totalarea += networkPipes[l - 1].getDrainArea();

        return totalarea;

    }
}
