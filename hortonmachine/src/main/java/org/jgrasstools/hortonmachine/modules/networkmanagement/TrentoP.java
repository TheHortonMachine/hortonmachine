package org.jgrasstools.hortonmachine.modules.networkmanagement;

import static java.lang.Math.pow;

import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Range;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.hortonmachine.modules.networkmanagement.core.SewerData;

@Description("The Trento P model for networks management.")
@Author(name = "Andrea Antonello, Franceschi Silvia, Riccardo Rigon, David Tamanini", contact = "www.hydrologis.com")
@Keywords("Network")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TrentoP {
    @Description("Coefficient a of the designed rainfall.")
    @Unit("mm/hour^n")
    @In
    public double pA = -1;

    @Description("Coefficient n of the designed rainfall.")
    @In
    public double pN = -1;

    @Description("The minimum shear stress.")
    @Unit("Pa")
    @In
    public double pTau = -1;

    @Description("The maximum filling ratio.")
    @In
    public double pG = -1;

    @Description("The network vertical alignment option (0,1).")
    @In
    public int pAlign = 0;

    @Description("")
    @Unit("m")
    @In
    public double pScavo = 1.20; // [m]

    @Description("Number of pipes allowed to join.")
    @In
    public double pMaxjunctions = 4;

    @Description("Number of bisection tried to obtain the root by the bisection method.")
    @In
    public double pJmax = 40;

    @Description("Accuracy used by the bisection method.")
    @In
    public double pAccuracy = 0.005;

    @Description("Time step at which the maximum discharge is evaluated.")
    @Unit("min")
    @In
    public double pDtp = 0.15;

    @Description("Minimum time of rain allowed in maximum discharge search.")
    @Unit("min")
    @In
    public double pTpmin = 1;

    @Description("Maximum time of rain allowed in maximum discharge search.")
    @Unit("min")
    @In
    public double pTpmax = 30;

    @Description("Accuracy in maximum discharge search")
    @In
    public double pEpsilon = 0.001;

    @Description("Minimum depth fraction allowed.")
    @In
    public double pMing = 0.01;

    @Description("Minimum discharge allowed in a pipe.")
    @Unit("l/s")
    @In
    public double pMinq = 1.0;

    @Description("For circular pipe cross section the maximum angle theta allowed when pG=0.8.")
    @Unit("rad")
    @In
    public double pMaxtheta = 4.43;

    @Description("Factor that multiplies velocity in pipes to obtain celerity.")
    @Range(max = 1.5, min = 1.0)
    @In
    public double pCelerityfactor = 1.5;

    @Description("Exponent used to calculate mean residence times outside pipes.")
    @Range(max = 0.5, min = 0.3)
    @In
    public double pExponent = 0.38;

    @Description("Accuracy in r value search.")
    @In
    public double pTolerance = 0.001;

    @Description("Base-height ratio for the rectangular and trapezoidal pipe sections.")
    @In
    public double pQ = 0.5; // c

    @Description("Exponent used to calculate mean residence times outside pipes.")
    @Range(max = 0.5, min = 0.3)
    @In
    public double pGamma = 0.35;

    @Description("Exponent used to calculate mean residence times outside pipes.")
    @Range(max = 0.5, min = 0.3)
    @In
    public double pB = 0.4; // esp_1

    @Description("The sewer geometry data.")
    @In
    public List<SewerData> inSewerdata;

    @Description("Diameters.")
    @In
    public double[][] inDiameters;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    public void process() {
        pA = pA / pow(60.0, pN); /* [mm/hour^n] -> [mm/min^n] */

        int sewerDataSize = inSewerdata.size();
        double[][] results = new double[sewerDataSize][25];
        double[] stateIndexes = new double[sewerDataSize];
        double[] recivingStateIndexes = new double[sewerDataSize];
        for( int i = 0; i < sewerDataSize; i++ ) {
            SewerData sewerData = inSewerdata.get(i);
            results[i][0] = sewerData.stateNum;
            results[i][1] = sewerData.stateDrain;
            results[i][2] = sewerData.stateDrainArea;
            results[i][3] = sewerData.pipeLength;
            results[i][4] = sewerData.initElev;
            results[i][5] = sewerData.finalElev;
            results[i][6] = sewerData.runoffCoeff;
            results[i][7] = sewerData.alpha;
            results[i][8] = sewerData.gsCoeff;
            results[i][9] = sewerData.minSope;
            results[i][10] = sewerData.pipeType;
            results[i][11] = sewerData.avgStateSlope;
            stateIndexes[i] = sewerData.stateNum;
            recivingStateIndexes[i] = sewerData.stateDrain;
        }

        // verify geometry data
        if (!checkData(results))
            throw new ModelsIllegalargumentException("No outlet was found in this network.", this);

        double[] magnitude = new double[sewerDataSize];
        double[][] geometry = new double[sewerDataSize][4];

        geo_sewer(stateIndexes, recivingStateIndexes, magnitude, results, inDiameters, geometry, pA, pN, pTau, pG, pAlign);
    }

    private void geo_sewer( double[] stateIndexes, double[] recivingStateIndexes, double[] magnitude, double[][] results,
            double[][] inDiameters2, double[][] geometry, double pA2, double pN2, double pTau2, double pG2, int pALign2 ) {


        long i;
        long j;
        long k;
        long l; /*! \param l Tratto che si sta progettando. */
        long qup; /*! \param qup Indice del tratto a cui corrisponde il diametro massimo, quando si analizza un sottobacino. */
        double totalarea; /*! \param totalarea \f$[m^{2}]\f$ Estensione del sottobacono con chiusura nel tratto l che si sta progettando. */
        double dtp; /*  \param dtp \f$[min]\f$ Passo con cui variare t e tp, nella ricerca della portata massima di progetto. */
        double tump; /*! \param tump \f$[l/s]\f$ Contributo alla portata del solo stato l di chiusura del subnetwork che si sta analizzando. */
        double maxd; /*! \param maxd \f$[cm]\f$ Tiene traccia dei diametri utilizzati e fa in modo che procedendo verso valle non vi siano restringimenti. */
        double oldr; /*! \param oldr r di tentativo, dove r = tp / k. */
        double r; /*! \param r Valore finale di r ( tp / k ). */
        double No; /*! \param No Vale L / ( k * c ). */
        double inf; /*! \param inf Estremo inferiore dell'intervallo che contiene la radice ricercata, ossia la r* che massimizza la portata. */
        double sup; /*! \param sup Estremo superiore dell'intervallo che contiene la r*, che massimizza la portata. */
        double oldQ; /*! \param oldQ \f$[l/s]\f$  Portata di tentativo. */
        long section; /*! \param section Tipo di sezione: 1=circolare, 2=rettangolare, 3=trapezia. */
        DOUBLEMATRIX *net; /*! \param net Puntatore a matrice che per ciascun area non di testa, contiene i dati geometrici degli stati a monte, che direttamente o indirettamente, drenano in esso*/
        DOUBLEVECTOR *localdelay; /*! \param localdelay Puntatore a vettore che contiene i ritardi locali dell'onda. */

        /*QUA CI VANNO LE 10 FORMULE USATE*/
        /*! \param type Tipo di simulazione*/
        /*! \param logstream Puntatore a file di output*/
        /*! \param one Puntatore a vettore che contiene gli indici degli stati. */
        /*! \param two Puntatore a vettore che contiene inizialmente gli indici degli stati riceventi. */
        /*! \param magnitude Puntatore a vettore che contiene la magnitude delle varie aree. */
        /*! \param results Puntatore alla matrice principale di Trento_p, che contiene i dati di input e alla fine risultati ottenuti. */
        /*! \param diametrs Puntatore alla matrice contenete diametri e spesori commerciali. */
        /*! \param geometry Puntatore a matrice che contiene i dati geometrici dei vari sottobacini. */
        /*! \param a \f$[mm \; min^{n}]\f$ Coefficiente della curva di possibilita pluviometrica. */
        /*! \param n Esponente della curva di possibilita pluviometrica. */
        /*! \param tau \f$[Pa]\f$ Sforzo tangenziale al fondo che garantisca l'autopulizia della rete. */
        /*! \param g Grado di riempimento da considerare nella progettazione della rete. */

        long index, index1;
        //  char *i3 = "Rain.txt", *i4 = "Network.txt", *o1 = "Q.txt", *program = "TRENTO_p";
        char *i3, *i4, *o1, *program = "TRENTO_p";
        double dt, time, tmin, tmax;
        //  short test
        DOUBLEVECTOR *c_delays;
        DOUBLEMATRIX *raindata, *netdata, *Qout, *pipes;

        //  printf("DO YOU WANT TO RUN THE CALIBRATION MODULE?(1=Y/0=N)\n");/*!!!!!!!*/
        //  test = get_parameter(WORKING_DIRECTORY, program);

        // if (test != 1 && test != 0)
        //      t_error("Please type 1 or 0");


        if (test == 1) {

            printf("ENTER THE TIME STEP dt [min]:\n");
            dt = get_parameter(WORKING_DIRECTORY, program);
            if (dt <= 0)
                t_error("dt must be greater than 0");

            printf("ENTER THE RAINFALL FILE NAME:\n");
            i3 = get_filename(WORKING_DIRECTORY, program);

            printf("ENTER THE NETWORK FILE NAME:\n");
            i4 = get_filename(WORKING_DIRECTORY, program);

            printf("ENTER THE DISCHARGE-OUTPUT FILE NAME:\n");
            o1 = get_filename(WORKING_DIRECTORY, program);

            irain = t_fopen(i3, "r");
            index = read_index(irain, NOPRINT);
            raindata = read_doublematrix(irain, "a", NOPRINT);
            t_fclose(irain);

            tmin = raindata->element[1][1];
            tmax = approximate_2_multiple(TMAX, dt);

            inetwork = t_fopen(i4, "r");
            index1 = read_index(inetwork, NOPRINT);
            pipes = read_doublematrix(inetwork, "a", NOPRINT);

            t_fclose(inetwork);

            netdata = new_doublematrix(pipes->nrh, 9);

            for (i = 1; i <= pipes->nrh; ++i) {
                netdata->element[i][1] = pipes->element[i][1]; /* Pipe number */
                netdata->element[i][2] = results->element[i][2]; /* Pipe in which drains */
                netdata->element[i][3] = results->element[i][3]; /* Area [ha] */
                netdata->element[i][4] = results->element[i][4]; /* L [m] */
                netdata->element[i][5] = results->element[i][7]; /* phi [] */
                netdata->element[i][6] = (HOUR2MIN * results->element[i][8] * pow(
                        results->element[i][3] / METER2CM, EXPONENT)) / (pow(
                        results->element[i][7], esp_1) * pow(
                        results->element[i][12], GAMMA)); /* k [min] nuova formula */
                //      netdata->element[i][6]= HOUR2MIN*results->element[i][8]*pow(results->element[i][3]/METER2CM,EXPONENT); /* k [min] */
                netdata->element[i][7] = results->element[i][9]; /* ks [m^1/3/s] */
                netdata->element[i][8] = pipes->element[i][2]; /* Pipe diameter [cm] */
                netdata->element[i][9] = pipes->element[i][3]; /* Pipe slope [%] */
            }

            free_doublematrix(pipes);

            //          print_doublematrix_elements(netdata,30);
            //          scanf("%c",&ch);


            Q = t_fopen(o1, "w");

            Qout = new_doublematrix(tmax / dt, (netdata->nrh + 1));
            initialize_doublematrix(Qout, 0);

            c_delays = new_doublevector(netdata->nrh);
            initialize_doublevector(c_delays, 0);

            for (i = 1, time = tmin; i <= Qout->nrh; time += dt, ++i) {
                Qout->element[i][1] = time;
            }
        }

        /**************************************************************************************/

        localdelay = new_doublevector(magnitude->nh);
        initialize_doublevector(localdelay, 0);

        /*
         * ++++++
         */
        pipe_magnitude(magnitude, two);/*Calcola la magnitude di ciascun stato*/
        /*
         * ++++++
         */

        for (i = 1; i <= two->nh; i++) /*al vettore two vengono assegnati gli elementi di magnitude */
        {
            two->element[i] = magnitude->element[i];
        }

        sort2realvectors(magnitude, one);/*Ordina gli elementi del vettore magnitude in ordine crescente, e posiziona nello stesso ordine gli elementi di one*/

        /*
         * print_doublevector_elements(magnitude,60);
         * print_doublevector_elements(one,60);
         */

        /* -----  INIZIO DIMENSIONAMENTO DELLE AREE DI TESTA  ----- */

        k = 1;
        l = one->element[k]; /*tratto che si sta analizzando o progettando*/
        section = results->element[l][11]; /*Tipo di sezione: 1=circolare, 2=rettangolare, 3=trapezia*/

        while (magnitude->element[k] == 1) /* Magnitude = 1 Area di testa */{
            printf("\nWORKING ON HEAD LINK no. %d\n", l);

            if (results->element[l][8] >= 0)/*Serve per tener conto della forma, piu o meno allungata, delle aree drenanti*/
            {
                results->element[l][15] = (HOUR2MIN * results->element[l][8] * pow(
                        results->element[l][3] / METER2CM, EXPONENT)) / (pow(
                        results->element[l][7], esp_1) * pow(
                        results->element[l][12], GAMMA)); /*La formula 1.7 (k=alfa * S^beta /(ksi^b * s^GAMMA) k tempo di residenza [min] */
            } else {
                results->element[l][15] = -results->element[l][3]
                        * results->element[l][8] / results->element[l][4];/*Considero solo l'acqua che drena dalla strada k=alfa * S/L * i^GAMMA [min] */
            }

            maxd = 0;

            results->element[l][18] = 1.0; /* Velocita media di primo tentativo nel tratto da progettare [m/s] */
            oldr = 1.0; /* r di primo tentativo [adimensional] */

            inf = 0.1; /* Estremo inferiore da adottare nella ricerca della r* che massimizza la portata. */

            /* -----  INIZIO CICLO FOR PER LA PROGETTAZIONE DEL TRATTO PARTENDO DA UNA r e celerita DI PRIMO TENTATIVO  ----- */

            for (j = 1; j <= JMAX; j++) {
                No = results->element[l][4] / (MINUTE2SEC * results->element[l][15]
                        * CELERITYFACTOR * results->element[l][18]); /* L/ku Calcolato in funzione della velocita di primo tentativo. No sara ricalcolato finche non si avra una convergenza di r. */

                sup = 2 * No + 5; /* Estremo superiore da adottare nella ricerca della r*. */

                /*
                 * ++++++
                 */
                r = rtbis(r_f, inf, sup, n, TOLERANCE, No, l, l);
                //  printf("j=%d, r=%f, oldr=%f \n",j,r,oldr);
                //              printf("r_f=%f, inf=%f, sup=%f, n=%f, No=%f, l=%d \n",r_f,inf,sup,n,No,l);

                /*
                 * ++++++
                 */
                /*
                 * printf("\nj=%d celerity=%f N=%f oldr=%f r=%f\n",j,results->element[l][15],No,oldr,r);
                 */
                results->element[l][16] = r * results->element[l][15]; /* tp* [min] che da origine alla massima portata, calcolato come r*k  */
                results->element[l][14] = results->element[l][7] * a * pow(
                        results->element[l][16], n - 1) * (1 + MINUTE2SEC
                        * CELERITYFACTOR * results->element[l][18]
                        * results->element[l][16] / results->element[l][4] - 1 / No
                        * log(exp(No) + exp(r) - 1)) * 166.6666667; /* coefficiente udometrico calcolato con la formula 2.17 u [l/s*ha] */
                results->element[l][13] = results->element[l][14]
                        * results->element[l][3]; /* Portata Q[l/s] */
                /*
                 * ++++++
                 */
                switch (section) {
                case 1:
                    design_pipe_1(results, diameters, tau, g, l, maxd);
                    break;
                case 2:
                    design_pipe_2(results, tau, g, l, maxd, c);
                    break;
                case 3:
                    design_pipe_3(results, tau, g, l, maxd, c);
                    break;
                default:
                    design_pipe_1(results, diameters, tau, g, l, maxd);
                    break;
                }

                /*
                 * ++++++
                 */

                if (fabs((r - oldr) / oldr) <= TOLERANCE) /* La r e stata determinata per via iterativa con la precisione richiesta, allora esce dal ciclo for */
                {
                    j = 0; /*la j che tiene conto del numero di iterazioni viene settata a 0 */
                    break;
                }

                oldr = r; /*non si e arrivati alla convergenza di r, quindi si usa la nuova r per un' ulteriore iterazione */
            }

            /* ----- FINE CICLO FOR PER CONVERGENZA DI r  ----- */

            if (j != 0) {
                //  printf("r=%f, oldr=%f \n",r,oldr);
                t_error("Error::r not converged \n"); /*Si e usciti dal precedente ciclo for perche si e superato il numero massimo di iterazioni JMAX ammesse senza arrivare alla convergenza di*/
            }

            results->element[l][17] = results->element[l][15] * log(exp(No)
                    + exp(r) - 1); /* t* [min] tempo in cui si verifica la massima tra le portata massime all'uscita del tratta appena progettato */
            localdelay->element[l] = (results->element[l][4]) / (CELERITYFACTOR
                    * MINUTE2SEC * results->element[l][18]); /* L/u [min] ritardo locale dell'onda di piena */
            geometry->element[l][1] = l; /* Indice dello stato.*/
            geometry->element[l][2] = results->element[l][3]; /*Ac [ha] superfice servita*/
            geometry->element[l][3] = results->element[l][4]; /* Mean length of upstream net [m] (=length of pipe)*/

            /***************************************************************************/
            if (test == 1) {
                ups_calibration(l, c_delays, raindata, netdata, Qout, dt, tmax);
            }
            /***************************************************************************/

            k++; /*Passo allo stato successivo*/

            if (k <= magnitude->nh) {
                l = one->element[k]; /*Il prossimo tratto da progettare, ovviamente se avra magnitude=1 */
            } else {
                break;
            }
        }

        /* -----  FINE CICLO WHILE PER LA PROGETTAZIONE DELLE AREE DI TESTA  ----- */

        dtp = DTP;

        if (dtp > 0.5) {
            dtp = 0.5;/* passo temporale con cui valutare le portate quando si ricerca la portata massima di progetto [min] */
            printf("\nWarning::integration time step set to 0.5 minutes\n");
        }

        /* -----  INIZIO CICLO WHILE PER LA PROGETTAZIONE DELLE AREE NON DI TESTA  ----- */

        while (k <= magnitude->nh) /* Magnitude > 1 AREE NON DI TESTA */{
            printf("\nWORKING ON LINK no. %d\n", l);
            net = new_doublematrix(magnitude->element[k] - 1, 9); /*Crea una matrice net[k-1][9], dove k-1 e pari al numero di stati, che direttamente o indirettamente, drenano nello stato*/
            initialize_doublematrix(net, 0); /*La inizializzo a 0 */

            if (results->element[l][8] >= 0)/*Serve per tener conto della forma, piu o meno allungata, delle aree drenanti*/
            {
                results->element[l][15] = (HOUR2MIN * results->element[l][8] * pow(
                        results->element[l][3] / METER2CM, EXPONENT)) / (pow(
                        results->element[l][7], esp_1) * pow(
                        results->element[l][12], GAMMA)); /*La formula 1.7 (k=alfa * S^beta * i^GAMMA) k tempo di residenza [min] */
            } else {
                results->element[l][15] = -results->element[l][3]
                        * results->element[l][8] / results->element[l][4]; /* k tempo di residenza [min] */
            }

            /*
             * ++++++
             */
            totalarea = scan_network(k, l, results, one, net, &qup); /*Restituisce l'area del subnetwork che si chiude in l*/
            /*
             * ++++++
             */

            maxd = results->element[qup][20]; /*Diametro massimo riscontrato nel subnetwork analizzato*/

            results->element[l][18] = 1.0; /* Velocita media di primo tentativo [m/s]*/
            results->element[l][13] = MINDISCHARGE; /* Portata di primo tentativp [l/s] */

            /*  -----  INIZIO CICLO DO WHILE (progettare fino alla convergenza della Q)  ----- */
            do {
                oldQ = results->element[l][13];
                results->element[l][13] = 0;
                localdelay->element[l] = results->element[l][4] / (CELERITYFACTOR
                        * MINUTE2SEC * results->element[l][18]); /* L/u [min] */

                for (i = 1; i <= net->nrh; i++) /* Aggiorna i ritardi nella matrice net, includendo il ritardo relativo allo stato che si sta progettando. Questo perche la celerita nell'ultimo tratto non e nota a priori, ma verra calcolata iteraivamente. */
                {
                    net->element[i][3] += localdelay->element[l];
                }

                /*
                 * ++++++
                 */
                tump = discharge(logstream, l, dtp, a, n, results, net, localdelay); /*Restituisce il contributo alla portata dello stato che si sta progettando*/
                /*
                 * ++++++
                 */

                /*
                 * for(y=1;y<=net->nrh;y++)
                 * { tump+=net->element[y][4]; }
                 * printf("Total discharge Q=%f\n",tump);
                 */

                for (i = 1; i <= net->nrh; i++)/* Risetta i ritardi, toglieno il ritardo relativo allo stato che si sta progettando*/
                {
                    net->element[i][3] -= localdelay->element[l];
                }

                /*
                 * ++++++
                 */
                switch (section) {
                case 1:
                    design_pipe_1(results, diameters, tau, g, l, maxd);
                    break;
                case 2:
                    design_pipe_2(results, tau, g, l, maxd, c);
                    break;
                case 3:
                    design_pipe_3(results, tau, g, l, maxd, c);
                    break;
                default:
                    design_pipe_1(results, diameters, tau, g, l, maxd);
                    break;
                }
                /*
                 * ++++++
                 */

                /*
                 * printf("\nPipe no. %d, Q=%f, Celerity=%f, TPMIN=%f, TPMAX=%f, tp*=%f, t*=%f\n", l,results->element[l][10],results->element[l][15],TPMIN,TPMAX,results->element[l][13],results->element[l][14]);
                 * scanf("%c",&ch);
                 */
            } while (abs(oldQ - results->element[l][13]) / oldQ > EPSILON); /* finchè si arriva alla convergenza della portata Q */

            /*****************************************/
            /*      if (type < 1){

             refine_discharge(logstream, l, diameters, results, net, localdelay, two, a, n, dtp, qup, tau, ms, g);
             }


             */
            /*****************************************/

            results->element[l][14] = results->element[l][13] / totalarea; /* Coefficiente udometrico u [l/(s*ha)] */
            geometry->element[l][1] = l; /* ID del tratto */
            geometry->element[l][2] = totalarea; /* Ac [ha] */
            geometry->element[l][3] = mean_doublematrix_column(net, 2); /* Mean length of upstream net [m] */
            geometry->element[l][4] = variance_doublematrix_column(net, 2,
                    geometry->element[l][3]); /* Variance of lengths of upstream net [m^2] */

            /***************************************************************************/
            if (test == 1) {
                downs_calibration(l, c_delays, net, raindata, netdata, Qout, dt,
                        tmax);
            }
            /***************************************************************************/

            k++; /*Passo allo stato successivo*/

            if (k <= magnitude->nh) /*se non sono arrivato alla fine*/{
                l = one->element[k];/* Prossimo stato da progettare*/
            } else {
                free_doublematrix(net);
                break;
            }

            free_doublematrix(net);
        }

        /******************************************************************************/
        if (test == 1) {
            fprintf(Q,
                    "/**\nThis file contains the hydrographs at the end of pipes\n*//*\n");
            /*
             * write_selected_doublematrix2_elements(Q,Qout,1,43,Qout->nrh,Qout->nch,50);
             */
            write_doublematrix2_elements(Q, Qout, 50);
            t_fclose(Q);
        }
        /******************************************************************************/

        /*
         * ++++++++++++++++++++++++
         */
        if (align == 0) {
            reset_depths_0(results, two);/*allinemento dei peli liberi, agendo sulla profondita dei tubi*/
        } else {
            reset_depths_1(results, two);/*allinemento dei peli liberi: introduzione salti di fondo*/
        }
        /*
         * ++++++++++++++++++++++++
         */

        free_doublevector(localdelay);

        return 1;


    }
    private boolean checkData( double[][] results ) {
        boolean isOk = false;
        for( int i = 0; i < inSewerdata.size(); i++ ) {
            SewerData sewerData = inSewerdata.get(i);
            /* 
             * Controlla se lo stato analizzato e quello finale.
             */
            if (sewerData.stateDrain == 0) {
                isOk = true;
            }

            /*
             * Controlla che non ci siano errori 
             * nei dati geometrici della rete
             */
            if (sewerData.stateDrain > inSewerdata.size()) {
                throw new ModelsIllegalargumentException("Wrong pipe number in geometry matrix", this);
            }
            /* 
             * ogni stato o sottobacino e contraddistinto da 
             * un numero crescente che va da 1 a n=numero di stati;
             * n e  anche pari a data->nrh. 
             * La prima colonna della matrice riporta l'elenco degli stati, 
             * mentre la seconda colonna ci dice dove ciascuno
             * stato va a drenare.(NON E AMMESSO CHE LO STESSO STATO DRENI SU 
             * PIU DI UNO!!) Questa if serve 
             * per verificare che non siano presenti condotte non dichiarate, 
             * ovvero piu realisticamente
             * che non ci sia un'errore di numerazione o battitura. In altri 
             * termini lo stato analizzato non
             * puo drenare in uno stato al di fuori di quelli esplicitamente 
             * dichiarati o dell'uscita, contradistinta con ID 0 
             */
            int kj = i;
            int count = 0; /*Terra conto degli stati attraversati dall'acqua che inizia a scorrere a partire dallo stato analizzato*/

            while( inSewerdata.get(kj).stateDrain != 0 )/*Seguo il percorso dell'acqua a partire dallo stato corrente*/
            {
                kj = inSewerdata.get(kj).stateDrain;

                if (kj > inSewerdata.size()) /*L'acqua non puo finire in uno stato che con sia tra quelli esplicitamente definiti, in altre parole il percorso dell'acqua non puo essere al di fuori dell'inseme dei dercorsi possibili*/{
                    pm.errorMessage("Wrong number " + kj + " in first two columns.");
                    pm.errorMessage("Numbers must be smaller than the number of rows");
                    throw new ModelsIllegalargumentException("Incorrect geometry matrix in input", this);
                }

                count++;
                if (count > results.length)
                    throw new ModelsIllegalargumentException("Incorrect geometry matrix in input", this);
                /* La variabile count mi consente di uscire dal ciclo while, nel caso non ci fosse [kj][2]=0,
                 ossia un'uscita. Infatti partendo da uno stato qualsiasi il numero degli stati attraversati
                 prima di raggiungere l'uscita non puo essere superiore al numero degli stati effettivamente
                 presenti. Quando questo accade vuol dire che l'acqua e in un loop  chiuso */
            }

        }
        return isOk;
    }
}
