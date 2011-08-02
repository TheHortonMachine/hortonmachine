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
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_CELERITY_FACTOR;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_EPSILON;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_ESP1;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_EXPONENT;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_GAMMA;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TDTP;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TPMAX;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TPMIN;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.HOUR2MIN;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.METER2CM;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.MINUTE2SEC;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.functions.R_F;
import org.jgrasstools.gears.utils.math.rootfinding.RootFindingFunctions;
import org.jgrasstools.gears.utils.sorting.QuickSortAlgorithm;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility;

/**
 * This class is an utility to calculate a sewer network.
 * 
 * <p>
 * This class contains 1 public methods which allows the client to calculate the
 * depth. It is used as an Utility to calculate pipe values. And 2 methods to
 * verify the net.
 * </p>
 * 
 * @author Daniele Andreis, Riccardo Rigon, David Tamanini,
 * 
 */
public class NetworkBuilder implements Network {
    /*
     * Monitor.
     */
    private final IJGTProgressMonitor pm;
    /*
     * Dati relativi alla rete
     */
    private final Pipe[] networkPipes;
    /*
     * celerita-.
     */
    private final double celerityfactor;
    /*
     * Coefficient of the pluviometric curve of possibility.
     */
    private final double a;
    /*
     * Tolleranza nella ricerca delle radici.
     */
    private final double n;
    /*
     * Commercial diameters of a pipe.
     */
    private double[][] diameters;
    /*
     * a string where store the warning messages.
     */
    private final StringBuilder strBuilder;
    /*
     * Time step used to calculate the discharge in a sewer pipe (to search the
     * Maximum discharge when <i>t</i> and <i>tp</i> change).
     */
    private final double tDTp;
    /*
     * Minimum Rain Time step to calculate the maximum discharge.
     */
    private final double tpMin;
    /*
     * Maximum Rain Time step to calculate the maximum discharge.
     */
    private final double tpMax;
    /*
     * Accuracy to use to calculate the discharge.
     */
    private final double epsilon;
    /*
     * Exponent of the basin extension. Used to calculate the average access
     * time to the network.
     */
    private final double exponent;
    /*
     * Exponent of the influx coefficient to calculate the average residence
     * time in the network, k.
     */
    private final double esp;
    /*
     * Exponent of the average ponderal slope of a basin to calculate the
     * average access time to the network for area units.
     */
    private final double gamma;
    /*
     * Minimum discharge in a pipe.
     */
    private double minDischarge;
    /*
     * The input feature collection of the model trentoP.
     */
    private final SimpleFeatureCollection inPipesFC;

    private final HortonMessageHandler msg = HortonMessageHandler.getInstance();

    public static class Builder {

        // Parametri obbligatori
        private final IJGTProgressMonitor pm;
        private final double n;
        private final double a;
        private final List<double[]> diameters;
        private final StringBuilder strBuilder;

        /*
         * Dati relativi alla rete
         */
        private final Pipe[] networkPipe;
        private final SimpleFeatureCollection inPipeFC;
        private double tDTp = DEFAULT_TDTP;
        private double celerityfactor1 = DEFAULT_CELERITY_FACTOR;
        private double tpMin = DEFAULT_TPMIN;
        private double tpMax = DEFAULT_TPMAX;
        private double pEpsilon = DEFAULT_EPSILON;
        private double pExponent = DEFAULT_EXPONENT;
        private double pEsp1 = DEFAULT_ESP1;
        private double pGamma = DEFAULT_GAMMA;

        /**
         * Initialize the object with needed fields.
         * 
         * @param pm
         * @param msg
         */
        public Builder( IJGTProgressMonitor pm, Pipe[] networkPipe, double n, double a, List<double[]> diameters,
                SimpleFeatureCollection inPipeFC, StringBuilder strBuilder ) {
            this.pm = pm;
            this.n = n;
            this.a = a;
            this.diameters = diameters;
            this.networkPipe = networkPipe;
            this.strBuilder = strBuilder;
            this.inPipeFC = inPipeFC;

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

        public NetworkBuilder build() {
            return new NetworkBuilder(this);
        }

        public Builder tDTp( double tDtP ) {
            this.tDTp = tDtP;
            return this;
        }

        public Builder tpMin( double tpMin ) {
            this.tpMin = tpMin;
            return this;
        }

        public Builder tpMax( double tpMax ) {
            this.tpMax = tpMax;
            return this;
        }

        public Builder pEpsilon( double epsilon ) {
            this.pEpsilon = epsilon;
            return this;
        }

        public Builder pExponent( double exponent ) {
            this.pExponent = exponent;
            return this;
        }

        public Builder pEsp1( double esp ) {
            this.pEsp1 = esp;
            return this;
        }

        public Builder pGamma( double gamma ) {
            this.pGamma = gamma;
            return this;
        }
    }

    /**
     * Set the parameter throughout the Builder,
     * 
     */
    private NetworkBuilder( Builder builder ) {
        this.pm = builder.pm;
        this.celerityfactor = builder.celerityfactor1;
        this.n = builder.n;
        this.strBuilder = builder.strBuilder;
        this.tpMin = builder.tpMin;
        this.tpMax = builder.tpMax;
        this.epsilon = builder.pEpsilon;
        this.exponent = builder.pExponent;
        this.esp = builder.pEsp1;
        this.gamma = builder.pGamma;
        this.tDTp = builder.tDTp;
        this.a = builder.a;
        this.inPipesFC = builder.inPipeFC;
        if (builder.diameters != null) {
            setDiameters(builder.diameters);
        }

        if (builder.networkPipe != null) {

            this.networkPipes = builder.networkPipe;
        } else {
            pm.errorMessage("networkPipe is null");
            throw new IllegalArgumentException("networkPipe is null");
        }

    }

    private void setDiameters( List<double[]> diametersList ) {
        diameters = new double[diametersList.size()][2];
        Iterator<double[]> iter = diametersList.iterator();
        int i = 0;
        while( iter.hasNext() ) {
            diameters[i] = iter.next();
            i++;
        }
    }

    /**
     * Builder for the Calibration class.
     */
    /**
     * 
     * Align the free surface.
     * 
     * <p>
     * There is two different modalities:
     * <ol>
     * <li>if the parameter align is 0 then the result is obtained by the change
     * of the depth.
     * <li>if the parameter align is 1 the result is obtained throughout the
     * introduction of the bottom step.
     * </ol>
     * 
     * @param align is a switch that allow to select the metod to allign the free surface.
     * @param networkPipes is a matrix which contains the networks value.
     * @param two array which contains the ID of the pipe where other pipes drains.
     * @param maxJunction maximum number of junction in a node.
     */
    private void resetDepths( double[] two ) {

        if (networkPipes[0].getAlign() == 0) {
            // align is 0 then the result is obtained by the change
            // of the depth.
            resetDepths0(two);
        } else {
            // align is 1 the result is obtained throughout the
            // introduction of the bottom step.
            resetDepths1(two);
        }
    }

    /**
     * Aallineamento altimetrico del pelo libero.
     * 
     * 
     * @param networkPipes   is a matrix which contains the networks value.
     * @param two vettore che contiene la magnitude delle varie aree.
     * @param maxJunction
     */
    private void resetDepths0( double[] two ) {

        int length = networkPipes.length;
        int[][] upstreampipes = new int[length][networkPipes[0].getMaxJunction() + 1];

        for( int i = 0; i < length; i++ ) {
            upstreampipes[i][0] = 1;
        }

        int[] controlstrip = new int[length];
        int count;
        for( int i = 0; i < length; i++ ) {
            count = networkPipes[i].getIdPipeWhereDrain();
            if (count != 0) {
                upstreampipes[count - 1][0] += 1;
                upstreampipes[count - 1][upstreampipes[count - 1][0] - 1] = i;
            }
        }

        for( int i = 0; i < length; i++ ) {
            if (two[i] == 1) {
                int k = i;

                while( networkPipes[k].getIdPipeWhereDrain() != 0 ) {
                    int oldk = k;
                    k = networkPipes[k].getIdPipeWhereDrain() - 1;

                    if (networkPipes[oldk].finalFreesurface < networkPipes[k].initialFreesurface) {
                        double delta = -networkPipes[oldk].finalFreesurface + networkPipes[k].initialFreesurface;

                        networkPipes[k].depthInitialPipe -= delta;
                        networkPipes[k].depthFinalPipe -= delta;
                        networkPipes[k].initialFreesurface -= delta;
                        networkPipes[k].finalFreesurface -= delta;
                    } else if (networkPipes[oldk].finalFreesurface > networkPipes[k].initialFreesurface) {
                        /*
                         * do nothing
                         */
                    } else {
                        break;
                    }
                }

            }
        }

        int parents = 0;
        int childs = 0;

        for( int i = 0; i < length; i++ ) {
            if (networkPipes[i].getIdPipeWhereDrain() == 0) {
                controlstrip[childs] = i;
                childs++;
            }

        }

        int gchilds = childs;

        do {
            for( int i = parents; i < childs; i++ ) {

                for( int j = 1; j < upstreampipes[controlstrip[i]][0]; j++ ) {
                    controlstrip[gchilds] = upstreampipes[controlstrip[i]][j];
                    double delta = networkPipes[controlstrip[gchilds]].finalFreesurface
                            - networkPipes[controlstrip[i]].initialFreesurface;
                    networkPipes[controlstrip[gchilds]].depthInitialPipe -= delta;
                    networkPipes[controlstrip[gchilds]].depthFinalPipe -= delta;
                    networkPipes[controlstrip[gchilds]].initialFreesurface -= delta;
                    networkPipes[controlstrip[gchilds]].finalFreesurface -= delta;
                    gchilds++;
                }
            }

            parents = childs;
            childs = gchilds;

        } while( gchilds < length );

    }

    /**
     * Allineamento altimetrico della rete con l'introduzione di salti di fondo.
     * 
     * <p>
     * Segue un criterio idoneo alla minimizzazione dei costi di scavo.
     * </p>
     * <p>
     * Percorre la rete da monte a valle, ogni volta che il livello del pelo
     * libero sale, aumenta laprofondita di scavo del tubo a valle in modo da
     * allineare i peli liberi. Quindi l'allineamento viene eseguito solamente
     * andando da monte a valle.
     * </p>
     * 
     * @param networkPipes is a matrix which contains the networks value.
     * @param two  Puntatore al vettore che contiene la magnitude delle varie aree.
     */

    private void resetDepths1( double[] two ) {

        /*
         * ! \param upstreampipes matrice che contiene per ogni stato in numuero
         * e gli ID degli stati che drenano in esso
         */
        int[][] upstreamPipes;

        /* creo una matrice upstreampipes[n stati][MAXJAUNCTION] */
        upstreamPipes = new int[networkPipes.length][networkPipes[0].getMaxJunction() + 1];
        /* Tutti gli elementi della prima colonna vengono posti pari a 1 */
        for( int i = 0; i < networkPipes.length; i++ ) {
            upstreamPipes[i][0] = 1;
        }
        // per ogni stato a partire dal primo.
        for( int i = 0; i < networkPipes.length; i++ ) {
            // vedo dove va a drenare.
            int count = networkPipes[i].getIdPipeWhereDrain();
            if (count != 0) {
                /*
                 * Se non si trarra dell'uscita incremento di uno la prima
                 * colonna della riga corrispondente allo statoricevente, nella
                 * matrice upstreampipes. In questo modo alla fine delciclo for
                 * per ogni stato avro nella prima colonna il numero di statiche
                 * drenano direttamente in lui
                 */

                upstreamPipes[count - 1][0] += 1;
                /*
                 * Nelle colonne successive registro l'ID degli stati che vi
                 * drenano direttamente,(nell'ordine in cui si riscontrano nella
                 * matrice networkPipes)
                 */
                upstreamPipes[count - 1][upstreamPipes[count - 1][0] - 1] = i;

            }
        }

        /* ! \param delta differenza di quota tra i peli liberi in una giunzione */
        double delta;
        for( int i = 0; i < networkPipes.length; i++ ) {
            // Se si tratta di un'area di testa
            if (two[i] == 1) {
                int k = i;
                /*
                 * Seguo il percorso dell'acqua verso valle, a partire da
                 * un'area di testa
                 */
                while( networkPipes[k].getIdPipeWhereDrain() != 0 ) {
                    int oldk = k;
                    // Stato dove drena l'area di testa
                    k = networkPipes[k].getIdPipeWhereDrain() - 1;

                    /*
                     * Controllo che procedendo verso valle la quota del pelo
                     * libero non aumenti. In caso contrario provedo
                     * all'allineamento dei peli liberi, ovviamento aumentando
                     * la profondita di scavo del tratto piu a valle, di una
                     * quantita delta pari alla differenza di quota tra i peli
                     * liberi.
                     */

                    if (networkPipes[oldk].finalFreesurface < networkPipes[k].initialFreesurface) {
                        /*
                         * Differenza di quota tra i peli liberi in una
                         * giunzione
                         */
                        delta = -networkPipes[oldk].finalFreesurface + networkPipes[k].initialFreesurface;
                        /*
                         * upstreampipes->element[k][1]=i;
                         */
                        /* Aumento la profondita di scavo al secondo nodo */
                        networkPipes[k].depthInitialPipe -= delta;
                        /* Aumento la profondita di scavo al secondo nodo */
                        networkPipes[k].depthFinalPipe -= delta;
                        /* Aggiorno la quota del pelo libero */
                        networkPipes[k].initialFreesurface -= delta;
                        /* Aggiorno la quota del pelo libero */
                        networkPipes[k].finalFreesurface -= delta;
                    } else if (networkPipes[oldk].finalFreesurface > networkPipes[k].initialFreesurface) {
                        /*
                         * controlstrip->element[i]=networkPipes->element[oldk][22
                         * ]- networkPipes->element[k][21];
                         */
                    } else {
                        /*
                         * Se il pelo libero nel tratto a vale risulta piu
                         * basso, non cambio niente
                         */
                        break;
                    }
                }
            }
        }

    }

    /**
     * Project the net.
     * 
     * <p>
     * it calculate some properties of each pipe, for instance the diameter, the
     * depth, the discharge.
     * </p>
     * <p>
     * The Network is a collection of {@link Pipe}, and the discharge is
     * evalutate using a pliviometric curve.
     * </p>
     * <p>
     * There is two loops:
     * <ol>
     * <li>The first on the head pipes.
     * <li>The second on the pipes which are internal.
     * </ol>
     * </p>
     */
    @Override
    public void geoSewer() throws Exception {
        /* l Tratto che si sta progettando. */
        int l;
        /*
         * Estensione del sottobacino con chiusura nel tratto l che si sta
         * progettando.
         */
        double totalarea;
        /*
         * Passo con cui variare t e tp, nella ricerca della portata massima di
         * progetto.
         */
        double dtp;
        /*
         * Tiene traccia dei diametri utilizzati e fa in modo che procedendo
         * verso valle non vi siano restringimenti.
         */
        double maxd;
        /* r di tentativo, dove r = tp / k. */
        double oldr;
        /* r Valore finale di r ( tp / k ). */
        double r = 0;
        /* No Vale L / ( k * c ). */
        double No = 0;
        /*
         * Estremo inferiore dell'intervallo che contiene la radice ricercata,
         * ossia la r* che massimizza la portata.
         */
        double inf;
        /*
         * Estremo superiore dell'intervallo che contiene la r*, che massimizza
         * la portata.
         */
        double sup;
        /* Portata di tentativo. */
        double oldQ;
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
        double tolerance = networkPipes[0].getTolerance();

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
        /* ----- INIZIO DIMENSIONAMENTO DELLE AREE DI TESTA ----- */
        /*
         * qup Indice del tratto a cui corrisponde il diametro massimo, quando
         * si analizza un sottobacino.
         */
        int[] qup = new int[1];
        int k = 0;
        // vettore che contiene i ritardi locali dell'onda.
        double[] localdelay = new double[magnitude.length];
        // tratto che si sta analizzando o progettando
        l = (int) one[k];
        pm.beginTask(msg.message("trentoP.begin"), networkPipes.length - 1);
        int jMax = networkPipes[0].getjMax();
        double c = networkPipes[0].getC();
        double g = networkPipes[0].getG();
        double tau = networkPipes[0].getTau();
        minDischarge = networkPipes[0].getMinDischarge();

        while( magnitude[k] == 1 ) {
            /*
             * Serve per tener conto della forma, piu o meno allungata, delle
             * aree drenanti
             */
            if (networkPipes[l - 1].getAverageResidenceTime() >= 0) {
                /*
                 * La formula 1.7 ( k = alfa * S ^ beta / ( ksi ^ b * s ^ GAMMA
                 * ) k tempo di residenza [ min ]
                 */
                networkPipes[l - 1].residenceTime = ((HOUR2MIN * networkPipes[l - 1].getAverageResidenceTime() * pow(
                        networkPipes[l - 1].getDrainArea() / METER2CM, exponent))

                / (pow(networkPipes[l - 1].getRunoffCoefficient(), esp) * pow(networkPipes[l - 1].getAverageSlope(), gamma)));
            } else {
                /*
                 * Considero solo l 'acqua che drena dalla strada k = alfa * S /
                 * L * i ^ GAMMA [ min ]
                 */
                networkPipes[l - 1].residenceTime = (-networkPipes[l - 1].getDrainArea()
                        * networkPipes[l - 1].getAverageResidenceTime() / networkPipes[l - 1].getLenght());
            }

            maxd = 0;
            /*
             * Velocita media di primo tentativo nel tratto da progettare [m/s]
             */
            networkPipes[l - 1].meanSpeed = (1.0);
            // r di primo tentativo [adimensional]
            oldr = 1.0;
            /*
             * Estremo inferiore da adottare nella ricerca della r* che
             * massimizza la portata.
             */
            inf = 0.1;
            /*
             * ----- INIZIO CICLO FOR PER LA PROGETTAZIONE DEL TRATTO PARTENDO
             * DA UNA r e celerita DI PRIMO TENTATIVO -----
             */
            int j;

            for( j = 0; j <= jMax; j++ ) {
                /*
                 * L / ku Calcolato in funzione della velocita di primo
                 * tentativo . No sara ricalcolato finche non si avra una
                 * convergenza di r .
                 */

                No = networkPipes[l - 1].getLenght()
                        / (MINUTE2SEC * networkPipes[l - 1].residenceTime * celerityfactor * networkPipes[l - 1].meanSpeed);

                // Estremo superiore da adottare nella ricerca della r*.

                sup = 2 * No + 5;
                /*
                 * tp* [min] che da origine alla massima portata, calcolato come
                 * r*k
                 */
                R_F rfFunction = new R_F();
                rfFunction.setParameters(n, No);
                r = RootFindingFunctions.bisectionRootFinding(rfFunction, inf, sup, tolerance, jMax, pm);
                networkPipes[l - 1].tP = (r * networkPipes[l - 1].residenceTime);
                /*
                 * coefficiente udometrico calcolato con la formula 2.17 u [ l /
                 * s * ha ]
                 */
                networkPipes[l - 1].coeffUdometrico = (networkPipes[l - 1].getRunoffCoefficient()
                        * a
                        * pow(networkPipes[l - 1].tP, n - 1)
                        * (1 + MINUTE2SEC * celerityfactor * networkPipes[l - 1].meanSpeed * networkPipes[l - 1].tP
                                / networkPipes[l - 1].getLenght() - 1 / No * log(exp(No) + exp(r) - 1)) * 166.6666667);
                /*
                 * Portata Q [ l / s ]
                 */
                networkPipes[l - 1].discharge = (networkPipes[l - 1].coeffUdometrico * networkPipes[l - 1].getDrainArea());

                networkPipes[l - 1].designPipe(diameters, tau, g, maxd, c, strBuilder);

                /*
                 * La r e stata determinata per via iterativa con la precisione
                 * richiesta , allora esce dal ciclo for
                 */
                if (abs((r - oldr) / oldr) <= tolerance) {
                    /*
                     * la j che tiene conto del numero di iterazioni viene
                     * settata a 0
                     */
                    j = 0;
                    break;
                }
                /*
                 * non si e arrivati alla convergenza di r, quindi si usa la
                 * nuova r per un' ulteriore iterazione
                 */
                oldr = r;
            }

            /* ----- FINE CICLO FOR PER CONVERGENZA DI r ----- */
            /*
             * Si e usciti dal precedente ciclo for perche si e superato il
             * numero massimo di iterazioni JMAX ammesse senza arrivare alla
             * convergenza di
             */
            if (j != 0) {
                pm.errorMessage(msg.message("trentoP.error.conv"));
            }
            /*
             * t * [ min ] tempo in cui si verifica la massima tra le portata
             * massime all 'uscita del tratta appena progettato
             */
            networkPipes[l - 1].tQmax = (networkPipes[l - 1].residenceTime * log(exp(No) + exp(r) - 1));
            /*
             * L / u [ min ] ritardo locale dell 'onda di piena
             */
            localdelay[l - 1] = (networkPipes[l - 1].getLenght()) / (celerityfactor * MINUTE2SEC * networkPipes[l - 1].meanSpeed);

            // Ac [ha] superfice servita

            networkPipes[l - 1].totalSubNetArea = networkPipes[l - 1].getDrainArea();

            // Mean length of upstream net [m] (=length of pipe)

            networkPipes[l - 1].totalSubNetLength = networkPipes[l - 1].getLenght();

            networkPipes[l - 1].meanLengthSubNet = networkPipes[l - 1].getLenght();

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
        }

        /*
         * ----- FINE CICLO WHILE PER LA PROGETTAZIONE DELLE AREE DI TESTA
         */

        dtp = tDTp;

        if (dtp > 0.5) {
            dtp = 0.5;/*
                      * passo temporale con cui valutare le portate quando si
                      * ricerca la portata massima di progetto [min]
                      */
            strBuilder.append(msg.message("trentoP.warning.timestep"));
        }

        /*
         * ----- INIZIO CICLO WHILE PER LA PROGETTAZIONE DELLE AREE NON DI TESTA
         * -----
         * 
         * Magnitude > 1 AREE NON DI TESTA
         */
        while( k < magnitude.length ) {
            /*
             * Crea una matrice net[k-1][9], dove k-1 e pari al numero di stati,
             * che direttamente o indirettamente , drenano nello stato
             */
            net = new double[(int) (magnitude[k] - 1)][9];
            /*
             * Serve per tener conto della forma, piu o meno allungata, delle
             * aree drenanti
             */
            if (networkPipes[l - 1].getAverageResidenceTime() >= 0) {
                /*
                 * La formula 1.7 ( k = alfa * S ^ beta * i ^ GAMMA ) k tempo di
                 * residenza [ min ]
                 */
                networkPipes[l - 1].residenceTime = ((HOUR2MIN * networkPipes[l - 1].getAverageResidenceTime() * pow(
                        networkPipes[l - 1].getDrainArea() / METER2CM, exponent)) / (pow(
                        networkPipes[l - 1].getRunoffCoefficient(), esp) * pow(networkPipes[l - 1].getAverageSlope(), gamma)));
            } else {

                // k tempo di residenza [ min ]

                networkPipes[l - 1].residenceTime = (-networkPipes[l - 1].getDrainArea()
                        * networkPipes[l - 1].getAverageResidenceTime() / networkPipes[l - 1].getLenght());
            }

            // Restituisce l'area del subnetwork che si chiude in l

            totalarea = scanNetwork(k, l, one, net, qup);

            // Diametro massimo riscontrato nel subnetwork analizzato

            maxd = networkPipes[qup[0] - 1].diameter;
            /*
             * Velocita media di primo tentativo [m/s]
             */
            networkPipes[l - 1].meanSpeed = (1.0);
            /*
             * Portata di primo tentativp [l/s]
             */
            networkPipes[l - 1].discharge = (minDischarge);

            /*
             * ----- INIZIO CICLO DO WHILE (progettare fino alla convergenza
             * della Q) -----
             */
            do {
                oldQ = networkPipes[l - 1].discharge;
                networkPipes[l - 1].discharge = (0);
                /*
                 * L / u [ min ]
                 */
                localdelay[l - 1] = networkPipes[l - 1].getLenght()
                        / (celerityfactor * MINUTE2SEC * networkPipes[l - 1].meanSpeed);
                /*
                 * Aggiorna i ritardi nella matrice net, includendo il ritardo
                 * relativo allo stato che si sta progettando. Questo perche la
                 * celerita nell'ultimo tratto non e nota a priori, ma verra
                 * calcolata iteraivamente.
                 */
                for( int i = 0; i < net.length; i++ ) {
                    net[i][2] += localdelay[l - 1];
                }
                /*
                 * Restituisce il contributo alla portata dello stato che si sta
                 * progettando
                 */
                discharge(l, dtp, net, localdelay);
                /*
                 * Risetta i ritardi, toglieno il ritardo relativo allo stato
                 * che si sta progettando
                 */
                for( int i = 0; i < net.length; i++ ) {
                    net[i][2] -= localdelay[l - 1];
                }

                networkPipes[l - 1].designPipe(diameters, tau, g, maxd, c, strBuilder);
                /*
                 * finche' si arriva alla convergenza della portata Q
                 */
            } while( abs(oldQ - networkPipes[l - 1].discharge) / oldQ > epsilon );

            /*
             * Coefficiente udometrico u [l/(s*ha )]
             */
            networkPipes[l - 1].coeffUdometrico = (networkPipes[l - 1].discharge / totalarea);

            /* Ac [ha] */
            networkPipes[l - 1].totalSubNetArea = totalarea;
            // Mean length of upstream net [ m ]
            networkPipes[l - 1].meanLengthSubNet = ModelsEngine.meanDoublematrixColumn(net, 1);
            /*
             * Variance of lengths of upstream net [ m ^ 2 ]
             */
            networkPipes[l - 1].varianceLengthSubNet = ModelsEngine.varianceDoublematrixColumn(net, 1,
                    networkPipes[l - 1].meanLengthSubNet);

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
        }
        resetDepths(two);

    }

    /**
     * Calcola la portata alla chiusura del un sottobacino che si sta
     * analizzando.
     * <p>
     * Valuta la portata alla chiusura di un subnetwork, ossia nel tratto che si
     * sta dimensionando,tenendo conto dei ritardi caratteistici di ciascun
     * stato contibuente e variando <i>t</i> e <i>tp</i> in un opportuno
     * intervallo. In questo modo determina, indirettamente la coppia t* e tp*
     * che massimizza la portata.
     * <p>
     * 
     * @param l Tratto che si sta dimensionando.
     * @param dtp [min] Passo temporale con cui valutare la portata, per cercare quella massima.
     * @param netPuntatore alla matrice che contiene tutti i dati delsubnetwork che si sta anallizzando: lunghezze, ritardi, ID, diametri ecc..
     * @param localdelay Puntatore al vettore dei ritardi nella rete.
     */

    private double discharge( int l, double dtp, double[][] net, double[] localdelay ) throws IOException {
        int indx;
        /*
         * [l/s]\f Portata nel tratto da progettare.
         */
        double Q = 0;
        /* [min] Tempo reale */
        double t = 0;
        /* [min] Tempo di pioggia */
        double tp;
        /*
         * [min]Estremo inferiore dell'intervallo in cui cercare la massima tra
         * le portate massime
         */
        double tmin = 0;
        /*
         * [min]\ Estremo superiore dell'intervallo in cui cercare la massima
         * tra le portate massime
         */
        double tmax = 0;
        /*
         * [min] t* che massimizza la portata del singolo stato
         */
        double tpeak;
        /* [min] */
        double tt = 0;
        /* [min] */
        double ttp = 0;
        /*
         * [l/s] Contributo alla portata del solo stato che si sta progettando.
         */
        double deltaQ;

        for( tp = tpMin; tp <= tpMax; tp += dtp ) {

            double[] tmpTime = new double[2];
            tmpTime[0] = tmin;
            tmpTime[1] = tmax;
            /*
             * Fornisce l'intervallo in cui ricercare la portata massima.
             */
            minMaxT(net, localdelay, tp, tmpTime);
            tmax = tmpTime[1];
            tmin = tmpTime[0];
            /*
             * t * [ min ] che massimizza la portata dello stato che si sta
             * progettando
             */
            tpeak = networkPipes[l - 1].residenceTime
                    * log(exp(tp / networkPipes[l - 1].residenceTime)
                            + exp(localdelay[l - 1] / networkPipes[l - 1].residenceTime) - 1);

            if (tmin > tpeak) {
                /*
                 * Aggiorna l'estremo inferiore dell'intervallo in cui si cerca
                 * la Qmax.
                 */
                tmin = ModelsEngine.approximate2Multiple(tpeak, tDTp);
            }
            for( t = tmin; t <= tmax; t += dtp ) {
                Q = 0;
                /*
                 * Per ogni stato del subnetwork
                 */
                for( int i = 0; i < net.length; i++ ) {
                    indx = (int) net[i][0];
                    /*
                     * Contributo dello stato indx , alla formazione della
                     * portata in l.
                     */
                    net[i][6] = dischargeFunction(indx, tp, t, net[i][2], localdelay);
                    /*
                     * Somma delle portate dei singoli stati contribuenti
                     */
                    Q += net[i][6];
                }
                /*
                 * Contributo dello stato che si sta progettando .
                 */
                deltaQ = dischargeFunction(l, tp, t, 0, localdelay);
                /*
                 * Portata totale all'uscita del tratto l a tempo t, tempo di
                 * pioggia tp in [l/s]
                 */
                Q += deltaQ;
                /* Aggiorna Q, t* e tp* */
                if (Q > networkPipes[l - 1].discharge) {
                    networkPipes[l - 1].discharge = (Q);
                    networkPipes[l - 1].tQmax = (t);
                    networkPipes[l - 1].tP = (tp);

                    for( int i = 0; i < net.length; i++ ) {
                        /*
                         * Contributo alla portata, valutata alla chiusura del
                         * subnetwork corrente
                         */
                        net[i][3] = net[i][6];

                        if (net[i][6] <= minDischarge) {
                            tt = t;
                            ttp = tp;
                        }
                    }

                }

            }

        }

        /*
         * Nelle righe seguenti calcolo il contributo alla portata di ciascun
         * stato al tempo t* per un tempo di pioggia pari a tp*. Se il
         * contributo e inferiore alla portata minima prefissata, allora il
         * programma stampa un messaggio di avviso
         */
        for( int i = 0; i < net.length; i++ ) {
            indx = (int) net[i][0];

            net[i][3] = dischargeFunction(indx, networkPipes[l - 1].tP, networkPipes[l - 1].tQmax, net[i][2], localdelay);

            if (net[i][3] <= minDischarge) {

                strBuilder.append("Minimum direct discharge in pipe no." + net[i][0] + " at time t=" + t + " for tp=" + tt
                        + " delay=" + ttp + " from outlet at " + net[i][2] + "\n");
            }
        }
        /*
         * Contributo dello stato di chiusura
         */
        deltaQ = dischargeFunction(l, networkPipes[l - 1].tP, networkPipes[l - 1].tQmax, 0, localdelay);

        return deltaQ;

    }

    /**
     * valuta il contributo di ciascun stato, alla formazione della portata in
     * corrispondenza della chiusura del subnetwork che si sta analizzando
     * <p>
     * Nel modello ad afflusso distribuito l'idrogrmma e ottenuto integrando
     * lungo la lunghezza del tubo ilcontribuito alla portata di un elemento del
     * tubo di lunghezza dx posto a distanza x dall'uscita del tubo stesso. Nel
     * calcolo di questo integrale si distingue tra tra due casi:
     * <ol>
     * <li>caso 1) tp>L/c e
     * <li>caso 2) tp<L/c.
     * </ol>
     * </p>
     * <p>
     * In entrambi i casi l'idrogramma che si ottiene e identico ed e formato da
     * quattro rami. Quello checambia sono gli intervalli dei vari rami, e
     * l'esppressione matematica degli stessi. La discharge_function() mi
     * restituisce il valoredella portata al tempo t e tempo di pioggia tp,
     * usando l'espressione giusta a seconda del ramo del'idrogramma di piena in
     * cui ci si trova. NB: che l'idrogramma di piena considerato tiene conto
     * della semplice traslazione cinematica che l'onda di piena, formatasi in
     * uno stato devesubire prima di raggiungere l'uscita del subnetwork che si
     * sta considerando.
     * </p>
     * 
     * @param indx ID stato contribuente, di cui si vuol calcolare la portata.
     * @param tp [min] tempo di pioggia.
     * @param t [min] tempo reale.
     * @param delay [min] ritardo con cui l'onda arriva all'uscita che si sta dimensionando.
     * @param localdelay vettore dei ritardi nella rete.
     * @return portata.
     */

    private double dischargeFunction( int indx, double tp, double t, double delay, double[] localdelay ) {
        short s = 0;
        /*
         * estremo superiore del primo ramo dell'idrogramma di piena, nel caso
         * di afflusso distribuito.
         */
        double ext1;
        /*
         * estremo superiore del secondo ramo dell'idrogramma di piena, nel caso
         * di afflusso distribuito.
         */
        double ext2;

        if (tp > localdelay[indx - 1]) {
            ext1 = localdelay[indx - 1];
            ext2 = tp;
            s = 0;
        } else {
            ext1 = tp;
            ext2 = localdelay[indx - 1];
            s = 1;
        }

        if (t <= delay) {
            /*
             * [l/s] Portata Q minima ammessa nei tubi. Ossia il contributo
             * dello stato non e ancora arrivato alla chiusura del sottobacino
             * corrente
             */
            return minDischarge;
        }

        double d = 166.666667;
        if (t > delay && t <= delay + ext1) {
            // Q1 [ l / s ]
            return networkPipes[indx - 1].getDrainArea()
                    * networkPipes[indx - 1].getRunoffCoefficient()
                    * a
                    * pow(tp, n - 1)
                    * ((t - delay) / localdelay[indx - 1] + (networkPipes[indx - 1].residenceTime / localdelay[indx - 1])
                            * (exp(-(t - delay) / networkPipes[indx - 1].residenceTime) - 1)) * d;
        }

        if (t > delay + ext1 && t <= delay + ext2) {
            if (s == 0) {
                // Q2 [ l / s ]
                return networkPipes[indx - 1].getDrainArea()
                        * networkPipes[indx - 1].getRunoffCoefficient()
                        * a
                        * pow(tp, n - 1)
                        * (1 + (networkPipes[indx - 1].residenceTime / localdelay[indx - 1])
                                * (1 - exp(localdelay[indx - 1] / networkPipes[indx - 1].residenceTime))
                                * exp(-(t - delay) / networkPipes[indx - 1].residenceTime)) * d;
            } else {
                // Q2 [ l / s ]
                return networkPipes[indx - 1].getDrainArea()
                        * networkPipes[indx - 1].getRunoffCoefficient()
                        * a
                        * pow(tp, n - 1)
                        * ((tp / localdelay[indx - 1]) + (networkPipes[indx - 1].residenceTime / localdelay[indx - 1])
                                * (1 - exp(tp / networkPipes[indx - 1].residenceTime))
                                * exp(-(t - delay) / networkPipes[indx - 1].residenceTime)) * d;
            }
        }

        if (t > delay + ext2 && t <= delay + ext1 + ext2) {
            // Q3 [ l / s ]
            return networkPipes[indx - 1].getDrainArea()
                    * networkPipes[indx - 1].getRunoffCoefficient()
                    * a
                    * pow(tp, n - 1)
                    * (1 - (t - delay - tp) / localdelay[indx - 1] + (networkPipes[indx - 1].residenceTime / localdelay[indx - 1])
                            * (1 + (1 - exp(localdelay[indx - 1] / networkPipes[indx - 1].residenceTime) - exp(tp
                                    / networkPipes[indx - 1].residenceTime))
                                    * exp(-(t - delay) / networkPipes[indx - 1].residenceTime))) * d;
        }

        if (t > delay + ext1 + ext2) {
            /* Q4 [l/s] */
            return networkPipes[indx - 1].getDrainArea()
                    * networkPipes[indx - 1].getRunoffCoefficient()
                    * a
                    * pow(tp, n - 1)
                    * ((networkPipes[indx - 1].residenceTime / localdelay[indx - 1])
                            * (1 - exp(tp / networkPipes[indx - 1].residenceTime))
                            * (1 - exp(localdelay[indx - 1] / networkPipes[indx - 1].residenceTime)) * exp(-(t - delay)
                            / networkPipes[indx - 1].residenceTime)) * d;
        }

        return 1;

    }

    /**
     * Determina l'intervallo in cui cercare la massima tra le portate massime.
     * <p>
     * Calcola il tempo in cui si verificano i picchi delle .singole aree
     * contribuenti, coi rispettivi ritardi.Questo allo scopo di valutare i due
     * valori estremi, che saranno l'intervallo in cui cercare i massimo
     * assoluto.
     * </p>
     * 
     * @param net matrice che contiene i dati della rete (per aree di testa).
     * @param localdelayritardo locale delle tubazioni.
     * @param tp
     * @param  t
     */

    private void minMaxT( double[][] net, double[] localdelay, double tp, double[] t ) {
        /*
         * Indice di un'area contribuente, parte del subnetwork che si sta
         * analizzando
         */
        int num;
        /* [min] la t* che massimizza la portata */
        double tpeak;
        t[1] = 0.0;
        t[0] = 1000000.0;

        for( int i = 0; i < net.length; i++ ) {

            // ID stato appartenente al subnetwork che si sta analizzando

            num = (int) net[i][0] - 1;
            /*
             * t * [ min ] che massimizza la portata all 'uscita dello stato in
             * cui si e formata , considerando un 'afflusso distribuito .
             */
            tpeak = networkPipes[num].residenceTime
                    * log(exp(tp / networkPipes[num].residenceTime) + exp(localdelay[num] / networkPipes[num].residenceTime) - 1);

            if ((tpeak + net[i][2]) < t[0]) {
                t[0] = ModelsEngine.approximate2Multiple((tpeak + net[i][2]), tDTp);
            }

            if ((tpeak + net[i][2] + tDTp) > t[1]) {
                t[1] = ModelsEngine.approximate2Multiple((tpeak + net[i][2] + tDTp), tDTp);
            }
        }

    }

    /**
     * Compila la mantrice net con tutte i dati del sottobacino con chiusura nel
     * tratto che si sta analizzando, e restituisce la sua superfice
     * 
     * @param k tratto analizzato del sottobacino chiuso in l.
     * @param l chiusura del bacino.
     * @param one indice dei versanti.
     * @param net sottobacino che si chiude in l.
     * @param qup contiene l-ndice con il diametro massimo.
     * @return total drainage area
     */

    private double scanNetwork( int k, int l, double[] one, double[][] net, int[] qup ) {

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

        // Diametro o altezza massima dei tratti pi� a monte.

        double maxdiam;

        int r = 0;
        int i = 0;
        /*
         * In one gli stati sono in ordine di magmitude crescente. Per ogni
         * stato di magnitude inferiore a quella del tratto l che si sta
         * progettando.
         */
        for( int j = 0; j < k; j++ ) {
            maxdiam = 0;
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
                    // Diametro precedentemente calcolato.
                    net[r][8] = networkPipes[i - 1].diameter;
                    /*
                     * procedendo verso valle il diametri adottati possono solo
                     * crescere
                     */
                    if (maxdiam < networkPipes[i - 1].diameter) {
                        maxdiam = networkPipes[i - 1].diameter;

                        // tratto a cui corrisponde il diametro massimo

                        (qup[0]) = i;
                    }
                    /*
                     * area di tutti gli stati a monte che direttamente o
                     * indirettamente drenano in l
                     */
                    totalarea += networkPipes[i - 1].getDrainArea();
                    r++;

                    break;
                }

                // se invece lo stato in cui drena e un'altro
                /*
                 * calcolo il ritardo che l 'onda di piena formatasi in uno
                 * stato accumula prima di raggiungere l 'ingresso del tubo che
                 * si sta progettando . Logicamente il ritardo che si calcola
                 * non ha alcuna utilita se l 'acqua raggiunge un 'uscita senza
                 * attraversare lo stato che so sta progettando
                 */
                t += networkPipes[ind - 1].getLenght() / (celerityfactor * MINUTE2SEC * networkPipes[ind - 1].meanSpeed);
                /*
                 * accumula le lunghezze percorse dall'acqua per arrivare al
                 * tratto da progettare
                 */
                length += networkPipes[ind - 1].getLenght();
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
