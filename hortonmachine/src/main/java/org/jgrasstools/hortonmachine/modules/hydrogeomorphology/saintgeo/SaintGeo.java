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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo;

import static org.jgrasstools.gears.libs.modules.JGTConstants.HYDROGEOMORPHOLOGY;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.riversections.ARiverSectionsExtractor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.riversections.RiverInfo;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.riversections.RiverPoint;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

@Description(SaintGeo.DESCRIPTION)
@Author(name = SaintGeo.AUTHORNAMES, contact = SaintGeo.AUTHORCONTACTS)
@Keywords(SaintGeo.KEYWORDS)
@Label(SaintGeo.LABEL)
@Name(SaintGeo.NAME)
@Status(SaintGeo.STATUS)
@License(SaintGeo.LICENSE)
public class SaintGeo extends JGTModel {
    @Description(inRiver_DESCRIPTION)
    @In
    public SimpleFeatureCollection inRiverPoints = null;

    @Description(inSections_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSections = null;

    @Description(inSectionPoints_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSectionPoints = null;

    @Description("Input head discharge.")
    @In
    public double[] inDischarge;

    @Description("Input downstream level.")
    @In
    public double[] inDownstreamLevel;

    // @Description("Lateral discharge tribute or offtake section progressive value for each id.")
    // @In
    // public HashMap<Integer, Double> inLateralId2ProgressiveMap;

    @Description("Lateral discharge tribute or offtake section discharge values for each id.")
    @In
    public HashMap<Integer, double[]> inLateralId2DischargeMap;

    // @Description("Lateral immission from confluences progressive value for each id.")
    // @In
    // public HashMap<Integer, Double> inConfluenceId2ProgressiveMap;

    @Description("Lateral immission from confluences discharge values for each id.")
    @In
    public HashMap<Integer, double[]> inConfluenceId2DischargeMap;

    @Description("Time interval.")
    @Unit("millisec")
    @In
    public long pDeltaTMillis = 5000;

    @Description("Output file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outputFile;

    // VARS DOC START
    public static final String DESCRIPTION = "A simple 1D hydraulic model based on the equations of Saint Venant.";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "1D, Hydraulic";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "SaintGeo";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "Silvia Franceschi, Andrea Antonello, Riccardo Rigon, Angelo Zacchia";
    public static final String AUTHORCONTACTS = "www.hydrologis.com";

    public static final String inRiver_DESCRIPTION = "The main stream river points (with the elevation in the attribute table).";
    public static final String inSections_DESCRIPTION = "The section lines.";
    public static final String inSectionPoints_DESCRIPTION = "The section points (with the elevation in the attribute table).";
    // VARS DOC END

    private LinearAlgebra linearAlgebra = new LinearAlgebra();
    private double DELT = -1;
    // TODO verify where and how these variables are set 
    private int SCELTA_A_MONTE;
    private int SCELTA_A_VALLE;

    // CONSTANTS
    private final double TOL_mu = 0.001;
    private final int MAX_CICLI = 1000;
    private final double TOLL = 0.001;
    private final double MIN_TIR = 0.01;
    private final double h_DEF = 0.001;
    private final double G = 9.806;
    private final double Cq = 0.41;

    @Execute
    public void process() throws Exception {
        checkNull(inDischarge, inRiverPoints, inSectionPoints, inSections);

        SCELTA_A_MONTE = 1;
        SCELTA_A_VALLE = 2;
        DELT = pDeltaTMillis / 1000.0;

        List<SimpleFeature> riverPointsFeatures = FeatureUtilities.featureCollectionToList(inRiverPoints);
        List<SimpleFeature> sectionFeatures = FeatureUtilities.featureCollectionToList(inSections);
        List<SimpleFeature> sectionPointsFeatures = FeatureUtilities.featureCollectionToList(inSectionPoints);
        RiverInfo riverInfo = ARiverSectionsExtractor.getRiverInfo(riverPointsFeatures, sectionFeatures, sectionPointsFeatures);
        List<RiverPoint> riverPoints = ARiverSectionsExtractor.riverInfo2RiverPoints(riverInfo);

        int sectionsCount = riverPoints.size();

        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile))) {

            double[] waterLevel = null;
            double[] waterLevelPrevious = null;
            double[] discharge = null;
            double[] celerity = null;
            double[] ql = null;
            double[] DELXM = null;
            pm.beginTask("Calculating SaintGeo...", inDischarge.length);
            /*
             * The length of the simulation is taken from the input head discharge
             * this is also the reference for all the other input data that have to
             * cover the same interval with the same timesteps. 
             */
            for( int timeIndex = 0; timeIndex < inDischarge.length; timeIndex++ ) {
                double qHead = inDischarge[timeIndex];
                double qHeadPrevious = qHead;
                if (timeIndex > 0) {
                    qHeadPrevious = inDischarge[timeIndex - 1];
                }

                double downstreamLevel = -1;
                if (inDownstreamLevel != null) {
                    downstreamLevel = inDownstreamLevel[timeIndex];
                }

                /*
                 * lateral discharge tribute or offtake
                 */
                boolean hasLateral = false;
                if (inLateralId2DischargeMap != null) {
                    hasLateral = true;
                }

                /*
                 * lateral contributes from confluences
                 */
                boolean hasConfluences = false;
                if (inConfluenceId2DischargeMap != null) {
                    hasConfluences = true;
                }

                double[][] idrgeo;
                /*
                 * defining initial conditions
                 */
                if (waterLevel == null) {
                    waterLevel = new double[sectionsCount];
                    waterLevelPrevious = new double[sectionsCount];
                    discharge = new double[sectionsCount - 1];
                    celerity = new double[sectionsCount - 1];
                    DELXM = new double[sectionsCount - 1];
                    ql = new double[sectionsCount];

                    for( int j = 0; j < riverPoints.size(); j++ ) {
                        // initial water level
                        double minsez = riverPoints.get(j).getTalWeg().z;
                        waterLevel[j] = minsez + 0.5;
                    }

                    /*
                     *  assign the initial discharge in all the sections with the
                     *  value of the first head discharge
                     */
                   
                    for( int i = 0; i < sectionsCount - 1; i++ ) {
                        discharge[i] = qHead;
                        DELXM[i] = riverPoints.get(i + 1).getProgressiveDistance() - riverPoints.get(i).getProgressiveDistance();
                    }

                    /*
                     * Evaluation of the steady flow condition using the
                     * initial discharge (first value of the head discharge) using
                     * the Gaukler-Strickler formula
                     */
                    calculateGauklerStrickler(qHead, waterLevel, riverPoints);
                    idrgeo = wettedArea(waterLevel, riverPoints);
                    double error = 100.0;

                    /*
                     * The condition of steady flow is reached when the maximum 
                     * difference between the water levels calculated in two subsequent
                     * instants is less than the tollerance TOL_mu 
                     */
                    int conta_cicli = 0;

                    while( error >= (TOL_mu / 10.0) && conta_cicli <= 60000 ) {
                        for( int i = 0; i < sectionsCount; i++ )
                            waterLevelPrevious[i] = waterLevel[i];
                        new_tirante(riverPoints, waterLevel, discharge, celerity, DELXM, SCELTA_A_MONTE, qHead, qHeadPrevious,
                                SCELTA_A_VALLE, downstreamLevel, ql);
                        error = Math.abs(waterLevelPrevious[0] - waterLevel[0]);
                        for( int i = 1; i < sectionsCount - 2; i++ ) {
                            double new_err = Math.abs(waterLevelPrevious[i] - waterLevel[i]);
                            if (new_err >= error)
                                error = new_err;
                        }
                        conta_cicli = conta_cicli + 1;
                    }
                    pm.message("Number of cicles for the steady flow condition " + conta_cicli); 

                    /*
                     * START THE MAIN FLOW
                     */
                    conta_cicli = 0;
                    // conta_warningqin = 1;
                    // conta_warningtirout = 1;
                }

                for( int i = 0; i < sectionsCount - 1; i++ ) {
                    ql[i] = 0;
                    RiverPoint section = riverPoints.get(i);
                    /*
                     * TODO check lateral contribute
                     */
                    if (hasLateral) {
                        double[] discharges = inLateralId2DischargeMap.get(section.getSectionId());
                        if (discharges != null) {
                            double lateralDischarge = discharges[timeIndex];
                            ql[i] = ql[i] + lateralDischarge / DELXM[i];
                        }
                    } else if (hasConfluences) {
                        double[] discharges = inConfluenceId2DischargeMap.get(section.getSectionId());
                        if (discharges != null) {
                            double confluenceDischarge = discharges[timeIndex];
                            ql[i] = ql[i] + confluenceDischarge / DELXM[i];
                        }
                    }
                }

                /*
                 * calculate the new water level:
                 * new_tirante generate the tridiagonal system for the evaluation of the 
                 * new water level
                 */
                new_tirante(riverPoints, waterLevel, discharge, celerity, DELXM, SCELTA_A_MONTE, qHead, qHeadPrevious,
                        SCELTA_A_VALLE, downstreamLevel, ql);

                /* write the output file 1 second (temporary) */
                idrgeo = wettedArea(waterLevel, riverPoints);

                StringBuilder sb = new StringBuilder();
                for( int i = 0; i < sectionsCount - 1; i++ ) {
                    RiverPoint section = riverPoints.get(i);
                    Coordinate[] sectionCoordinates = section.getSectionCoordinates();
                    sb.append(section.getSectionId()).append(";");
                    sb.append(section.getProgressiveDistance()).append(";");
                    double froudeNumber = (Math.abs(celerity[i]) / Math.sqrt(G * (idrgeo[i][0] / idrgeo[i][3])));
                    sb.append(froudeNumber).append(";");
                    sb.append(discharge[i] < 0.0 ? 0.0 : discharge[i]).append(";");
                    sb.append(celerity[i] < 0.0 ? 0.0 : celerity[i]).append(";");
                    sb.append(waterLevel[i]).append(";");
                    sb.append(idrgeo[i][0]).append(";");
                    double minsez = section.getMinElevation();
                    sb.append(minsez).append(";");
                    int dx = section.getStartNodeIndex();
                    sb.append(sectionCoordinates[dx].z).append(";");
                    int sx = section.getEndNodeIndex();
                    sb.append(sectionCoordinates[sx].z).append(";");
                }
                RiverPoint section = riverPoints.get(sectionsCount - 1);
                Coordinate[] sectionCoordinates = section.getSectionCoordinates();
                double froudeNumber = (Math.abs(discharge[sectionsCount - 2] / idrgeo[sectionsCount - 1][0]) / Math.sqrt(G
                        * (idrgeo[sectionsCount - 1][0] / idrgeo[sectionsCount - 1][3])));
                sb.append(section.getSectionId()).append(";");
                sb.append(section.getProgressiveDistance()).append(";");
                sb.append(froudeNumber).append(";");
                sb.append(discharge[sectionsCount - 2] < 0.0 ? 0.0 : discharge[sectionsCount - 2]).append(";");
                double cel = discharge[sectionsCount - 2] / idrgeo[sectionsCount - 1][0];
                sb.append(cel < 0.0 ? 0.0 : cel).append(";");
                sb.append(waterLevel[sectionsCount - 1]).append(";");
                sb.append(idrgeo[sectionsCount - 1][0]).append(";");
                double minsez = section.getMinElevation();
                sb.append(minsez).append(";");
                int dx = section.getStartNodeIndex();
                sb.append(sectionCoordinates[dx].z).append(";");
                int sx = section.getEndNodeIndex();
                sb.append(sectionCoordinates[sx].z);

                outputWriter.write(sb.toString() + "\n");
                pm.message(sb.toString());
                pm.worked(1);
            }
            pm.done();
        }
    }
    /**
     * Use Gaukler-Strickler formula for the evaluation of the initial condition
     * (hypotesis of steady flow).
     * 
     * @param q: discharge
     * @param level: the level
     * @param sectionsList
     */
    private void calculateGauklerStrickler( double q, double[] level, List<RiverPoint> sectionsList ) {
        double toll, conta_cicli;
        double IF, max_tir, tir_dx, tir_sx, tir_med, val_dx, val_sx, val_med;
        double[][] idrgeo;
        /* create a complete list to proceed with the next elaborations */
        int imax = sectionsList.size();
        double[] minsez = new double[imax];
        double[] maxsez = new double[imax];

        for( int i = 0; i < imax; i++ ) {
            RiverPoint section = sectionsList.get(i);
            minsez[i] = section.getMinElevation();
            maxsez[i] = section.getMaxElevation();
            level[i] = minsez[i] + 1;
        }
        /* calculate the steady flow water level for the sections of the j-esim segment */
        for( int i = 0; i < imax; i++ ) {
            /* the slope of the bottom of the i-esim section */
            if (i == 0 || i == 1)
                IF = (minsez[i] - minsez[i + 1])
                        / (sectionsList.get(i + 1).getProgressiveDistance() - sectionsList.get(i).getProgressiveDistance());
            else if (i == imax - 1 || i == imax - 2)
                IF = (minsez[i - 1] - minsez[i])
                        / (sectionsList.get(i).getProgressiveDistance() - sectionsList.get(i - 1).getProgressiveDistance());
            else
                IF = (minsez[i - 2] - minsez[i + 2])
                        / (sectionsList.get(i + 2).getProgressiveDistance() - sectionsList.get(i - 2).getProgressiveDistance());
            if (IF <= 0)
                IF = (minsez[0] - minsez[imax - 1])
                        / (sectionsList.get(imax - 1).getProgressiveDistance() - sectionsList.get(0).getProgressiveDistance());
            /*
             * the function is calculated for the extreme values looking for the minimun value
             * at the bank
             */
            max_tir = maxsez[i];
            tir_dx = max_tir;
            tir_sx = minsez[i] + MIN_TIR;
            toll = 100;
            conta_cicli = 0;

            while( toll >= TOLL && conta_cicli <= MAX_CICLI ) {
                level[i] = tir_dx;
                idrgeo = wettedArea(level, sectionsList);
                val_dx = q - idrgeo[i][0] * idrgeo[i][4] * Math.pow(IF, (0.5)) * Math.pow(idrgeo[i][2], (2.0 / 3.0));
                level[i] = tir_sx;
                idrgeo = wettedArea(level, sectionsList);
                val_sx = q - idrgeo[i][0] * idrgeo[i][4] * Math.pow(IF, (0.5)) * Math.pow(idrgeo[i][2], (2.0 / 3.0));
                if ((val_dx * val_sx) > 0) {
                    pm.errorMessage("Evaluation of the steady flow not possible for the section "  + i + 
                            "solution not found.");
                }
                tir_med = (tir_dx + tir_sx) / 2.0;
                level[i] = tir_med;
                idrgeo = wettedArea(level, sectionsList);
                val_med = q - idrgeo[i][0] * idrgeo[i][4] * Math.pow(IF, (0.5)) * Math.pow(idrgeo[i][2], (2.0 / 3.0));
                toll = Math.abs(tir_dx - tir_sx);
                if ((val_dx * val_med) < 0)
                    tir_sx = tir_med;
                else
                    tir_dx = tir_med;
                conta_cicli = conta_cicli + 1;
            }
        }
    }

    /**
     * <p>
     * Questa funzione calcola
     * </p>
     * <ul>
     * <li> l'area bagnata </li>
     * <li> il perimetro bagnato </li>
     * <li> il raggio idraulico </li>
     * <li> la larghezza della superficie libera </li>
     * <li> la scabrezza efficace </li>
     * <li> il coeff. alfa di Coriolis </li>
     * </ul>
     * in ogni sezione noto il tirante.
     * <p>
     * La funzione restituisce una matrice che ha come elementi di ogni colonna della i-esima
     * riga le grandezze precedenti relative alla i-esima sezione nello stesso ordine in cui sono
     * state elencate.
     * </p>
     * <p>
     * La funzione ha come argomenti:
     * </p>
     * <ul>
     * <li>il vettore dei tiranti</li>
     * <li>il vettore che contiene le sezioni di calcolo.</li>
     * </ul>
     * <p>
     * <b>L'area bagnata</b> e' calcolata come somma dei trapezi che si ottengono tracciando da
     * ogni punto di stazione delle suddivioni verticali; ogni trapezio e' definito da una base
     * destra (base_dx) una base sinistra (base_sx) e un'altezza (altezza).
     * </p>
     * <p>
     * <b>Il perimetro bagnato</b> e' calcolato come somma dei tratti bagnati del fondo alveo.
     * </p>
     * <p>
     * <b>Il raggio idraulico</b> e' calcolato direttamente come da definizione.
     * </p>
     * <p>
     * <b>La larghezza della superficie libera</b> coincide con l'altezza dei trapezi definiti per
     * calcolare l'area bagnata.
     * </p>
     * <p>
     * <b>Il coefficiente di scabrezza efficace</b> e' calcolato con il metodo di Egelund.
     * Suddivisa la sezione come per il calcolo dell'area bagnata per ogni trapezio si calcola la
     * quantita' <b>Ks(j)*Y(j)^(5/3)*B(j)</b> dove:
     * </p>
     * <ul>
     * <li><b>Ks(j)</b> e' il coeff. di Gaukler-Strickler per il tratto j-esimo</li>
     * <li><b>Y(j)</b> e' l'altezza idrica nel trapezio j-esimo che si ottiene come rapporto fra
     * l'area del trapezio j-esimo e la relativa larghezza sulla superficie libera</li>
     * <li><b>B(j)</b> e' la larghezza della superficie libera relativa al trapezio j-esimo</li>
     * </ul>
     * <p>
     * n.b. il raggio idraulico del trapezio j-esimo si ritiene approsimabile con l'altezza idrica,
     * cio' e' lecito solo nell'ipotesi di sezione larga
     * </p>
     * <p>
     * <b>Il coefficiente di scabrezza efficace</b> si ottiene dividendo la somma di tutte le
     * quantita' per <b>(A*RH^(2/3))</b>, dove A e' l'area bagnata complessiva e RH e' il raggio
     * idraulico riferito all'intera sezione.</li>
     * </p>
     * <p>
     * <b>Il coefficiente alfa di Coriolis</b> e' calcolato con il seguente metodo: suddivisa la
     * sezione come per il calcolo dell'area bagnata per ogni trapezio si calcola la quantita'
     * <b>Ks(j)^2*A(j)^(7/3)/P(j)^(4/3)</b> dove:
     * </p>
     * <ul>
     * <li><b>Ks(j)</b> e' il coeff. di G-S per il tratto j-esimo</li>
     * <li><b>A(j)</b> e' l'area del trapezio j-esimo</li>
     * <li><b>P(j)</b> e' il contorno bagnato relativo al trapezio j-esimo</li>
     * </ul>
     * <p>
     * <b>Il coefficiente di Coriolis</b> si ottiene dividendo la somma di tutte le quantita' per
     * <b>(ATOT*KsTOT^2*RHTOT^(4/3))</b> dove:
     * <ul>
     * <li>ATOT l'area dell'intera sezione</li>
     * <li>KsTOT il coefficiente di scabrezza efficace </li>
     * <li>RHTOT il raggio idraulico della sezione</li>
     * </ul>
     * </p>
     * 
     * @param waterLevel il vettore dei tiranti
     * @param riverPoints il vettore che contiene le sezioni di calcolo
     * @return una matrice che ha come elementi di ogni colonna della i-esima riga le
     *         grandezze: 
     *          - wetted area
     *          - wetted perimeter
     *          - hydraulic radius
     *          - roughness
     *          - alpha coefficient of Corilis
     *         relative alla i-esima sezione.
     */
    private double[][] wettedArea( double[] waterLevel, List<RiverPoint> riverPoints ) {
        /* right and left limits of the main channel */
        double dx, sx; 
        double area_b, base_dx, base_sx, altezza;
        double peri_b;
        double larghe_b;
        double gau_b;
        double alfa_num, alfa_den;
        /* *_loc refer to the local slice (trapezoidal) of the area of the section */
        double area_loc, peri_loc, gau_loc;

        int imax = riverPoints.size();
        double[][] tirase = new double[imax][6];

        /*
         * Calculate: wetted area, wetted perimeter, width of the water surface, the roughness
         * and the alpha coefficient of Coriolis in the section i
         */
        for( int i = 0; i < imax; i++ ) {

            RiverPoint section = riverPoints.get(i);
            area_b = 0;
            peri_b = 0;
            larghe_b = 0;
            gau_b = 0;
            gau_loc = 0;
            alfa_num = 0;
            alfa_den = 0;

            dx = section.getStartNodeIndex();
            sx = section.getEndNodeIndex();

            Coordinate[] sectionCoordinates = section.getSectionCoordinates();
            List<Double> sectionProgressives = section.getSectionProgressive();
            List<Double> sectionGauklerStrickler = section.getSectionGauklerStrickler();

            for( int j = (int) dx - 1; j < sx - 1; j++ ) {
                /* Check the segments between the stations j and j+1 of the current section
                 
                // if (section.getYAt(j) >= tirante[i] && section.getYAt(j + 1) >= tirante[i]) {
                // area_b = area_b + 0;
                // peri_b = peri_b + 0;
                // larghe_b = larghe_b + 0;
                // gau_b = gau_b + 0;
                // alfa_num = alfa_num + 0;
                // alfa_den = alfa_den + 0;
                // }
                /* 
                 * case 1: only partially wetted (right side dry) */

                if (sectionCoordinates[j].z >= waterLevel[i] && sectionCoordinates[j + 1].z < waterLevel[i]) {
                    /* the area of the triangle */
                    base_dx = 0;
                    base_sx = waterLevel[i] - sectionCoordinates[j + 1].z;
                    altezza = base_sx * (sectionProgressives.get(j + 1) - sectionProgressives.get(j))
                            / (sectionCoordinates[j].z - sectionCoordinates[j + 1].z);
                    area_b = area_b + (base_dx + base_sx) * altezza / 2.0;
                    /* the part of the bottom that is wetted */
                    peri_b = peri_b + Math.sqrt(Math.abs(base_dx - base_sx) * Math.abs(base_dx - base_sx) + altezza * altezza);
                    /* the width of the water surface */
                    larghe_b = larghe_b + altezza;
                    /* the roughness coefficient */
                    gau_b = gau_b + (sectionGauklerStrickler.get(j) * Math.pow((base_dx + base_sx) / 2.0, (5.0 / 3.0)) * altezza);
                    gau_loc = sectionGauklerStrickler.get(j);
                    /* the alpha coefficient of Coriolis */
                    area_loc = (base_dx + base_sx) * altezza / 2.0;
                    peri_loc = Math.sqrt((base_dx - base_sx) * (base_dx - base_sx) + altezza * altezza);
                    if (area_loc != 0) {
                        // use the method of Einstein-Horton?
                        alfa_num = alfa_num + (gau_loc * gau_loc) * Math.pow(area_loc, (7.0 / 3.0))
                                / Math.pow(peri_loc, (4.0 / 3.0));
                    }
                    alfa_den = alfa_den + gau_loc * area_loc * Math.pow(peri_loc, (2.0 / 3.0));

                    /* no one of these values can be null */
                    /*
                     * if ( base_dx<0 || base_sx<0 || altezza<0 ) { print("POSIZIONE " + i " -
                     * TIRANTE " + waterLevel[i] + "Error in the evaluation of the wetted area"};
                     */
                }
                /* case 2: only partially wetted (left side dry) */

                if (sectionCoordinates[j + 1].z >= waterLevel[i] && sectionCoordinates[j].z < waterLevel[i]) {
                    /* the area of the triangle */
                    base_sx = 0;
                    base_dx = waterLevel[i] - sectionCoordinates[j].z;
                    altezza = base_dx * (sectionProgressives.get(j + 1) - sectionProgressives.get(j))
                            / (sectionCoordinates[j + 1].z - sectionCoordinates[j].z);
                    area_b = area_b + (base_dx + base_sx) * altezza / 2.0;
                    /* the part of the bottom that is wetted */
                    peri_b = peri_b + Math.sqrt(Math.abs(base_dx - base_sx) * Math.abs(base_dx - base_sx) + altezza * altezza);
                    /* the width of the water surface */
                    larghe_b = larghe_b + altezza;
                    /* the roughness coefficient */
                    gau_b = gau_b + (sectionGauklerStrickler.get(j) * Math.pow((base_dx + base_sx) / 2.0, (5.0 / 3.0)) * altezza);
                    gau_loc = sectionGauklerStrickler.get(j);

                    /* the alpha coefficient of Coriolis */
                    area_loc = (base_dx + base_sx) * altezza / 2.0;
                    peri_loc = Math.sqrt((base_dx - base_sx) * (base_dx - base_sx) + altezza * altezza);
                    if (area_loc != 0) {
                        alfa_num = alfa_num + (gau_loc * gau_loc) * Math.pow(area_loc, (7.0 / 3.0))
                                / Math.pow(peri_loc, (4.0 / 3.0));
                    }
                    alfa_den = alfa_den + gau_loc * area_loc * Math.pow(peri_loc, (2.0 / 3.0));
                  
                    /* no one of these values can be null */
                    /*
                     * if ( base_dx<0 || base_sx<0 || altezza<0 ) { print("POSIZIONE " + i " -
                     * TIRANTE " + waterLevel[i] + "Error in the evaluation of the wetted area"};
                     */
                    
                }
                /* case 3: completely wetted */
                
                if (sectionCoordinates[j + 1].z < waterLevel[i] && sectionCoordinates[j].z < waterLevel[i]) {
                    base_dx = waterLevel[i] - sectionCoordinates[j].z;
                    base_sx = waterLevel[i] - sectionCoordinates[j + 1].z;
                    altezza = sectionProgressives.get(j + 1) - sectionProgressives.get(j);
                    area_b = area_b + (base_dx + base_sx) * altezza / 2.0;

                    peri_b = peri_b + Math.sqrt(Math.abs(base_dx - base_sx) * Math.abs(base_dx - base_sx) + altezza * altezza);
                    larghe_b = larghe_b + altezza;
                    gau_b = gau_b + (sectionGauklerStrickler.get(j) * Math.pow((base_dx + base_sx) / 2, (5.0 / 3.0)) * altezza);
                    gau_loc = sectionGauklerStrickler.get(j);

                    area_loc = (base_dx + base_sx) * altezza / 2;
                    peri_loc = Math.sqrt((base_dx - base_sx) * (base_dx - base_sx) + altezza * altezza);
                    if (area_loc != 0) {
                        alfa_num = alfa_num + (gau_loc * gau_loc) * Math.pow(area_loc, (7.0 / 3.0))
                                / Math.pow(peri_loc, (4.0 / 3.0));
                    } else {
                        alfa_num = alfa_num + 0;
                    }
                    alfa_den = alfa_den + gau_loc * area_loc * Math.pow(peri_loc, (2.0 / 3.0));
                   
                    /* no one of these values can be null */
                    /*
                     * if ( base_dx<0 || base_sx<0 || altezza<0 ) { print("POSIZIONE " + i " -
                     * TIRANTE " + waterLevel[i] + "Error in the evaluation of the wetted area"};
                     */
                }
            }
            /*
             * Fill the final matrix tirase
             */
            /* wetted area */
            tirase[i][0] = area_b;
            /* wetted perimeter */
            tirase[i][1] = peri_b;
            /* hydraulic radius */
            tirase[i][2] = area_b / peri_b;
            /* width of the water surface */
            tirase[i][3] = larghe_b;
            /* roughness coefficient */
            gau_b = gau_b / (area_b * Math.pow((area_b / peri_b), (2.0 / 3.0)));

            tirase[i][4] = gau_b;
            /* alpha coefficient of Coriolis */
            tirase[i][5] = alfa_num / (area_b * Math.pow(gau_b, 2) * Math.pow((area_b / peri_b), (4.0 / 3.0)));
            /* tirase[i][6]=1; */
            /* tirase[i][6]=area_b*alfa_num/(alfa_den*alfa_den); */
        }

        return tirase;
    }

    private void new_tirante( List<RiverPoint> riverPoints, double[] tirante, double[] Q, double[] U, double[] DELXM,
            int SCELTA_A_MONTE, double qin, double qin_old, int SCELTA_A_VALLE, double tiranteout, double[] ql ) {
        
        /*
         * TODO calculate the solution to the Saint Venant equations
         */
    }
    

}