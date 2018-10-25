/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

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
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.ARiverSectionsExtractor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.RiverInfo;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.RiverPoint;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;

@Description(OmsSaintGeo.DESCRIPTION)
@Author(name = OmsSaintGeo.AUTHORNAMES, contact = OmsSaintGeo.AUTHORCONTACTS)
@Keywords(OmsSaintGeo.KEYWORDS)
@Label(OmsSaintGeo.LABEL)
@Name(OmsSaintGeo.NAME)
@Status(OmsSaintGeo.STATUS)
@License(OmsSaintGeo.LICENSE)
public class OmsSaintGeo extends HMModel {
    @Description(inRiver_DESCRIPTION)
    @In
    public SimpleFeatureCollection inRiverPoints = null;

    @Description(inSections_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSections = null;

    @Description(inSectionPoints_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSectionPoints = null;

    @Description(inDischarge_DESCRIPTION)
    @In
    public double[] inDischarge;

    @Description(inDownstreamLevel_DESCRIPTION)
    @In
    public double[] inDownstreamLevel;

    // @Description("Lateral discharge tribute or offtake section progressive value for each id.")
    // @In
    // public HashMap<Integer, Double> inLateralId2ProgressiveMap;

    @Description(inLateralId2DischargeMap_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inLateralId2DischargeMap;

    // @Description("Lateral immission from confluences progressive value for each id.")
    // @In
    // public HashMap<Integer, Double> inConfluenceId2ProgressiveMap;

    @Description(inConfluenceId2DischargeMap_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inConfluenceId2DischargeMap;

    @Description(pDeltaTMillis_DESCRIPTION)
    @Unit(pDeltaTMillis_UNIT)
    @In
    public long pDeltaTMillis = 5000;

    @Description(outputLevelFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputLevelFile;

    @Description(outputDischargeFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputDischargeFile;

    // VARS DOC START
    public static final String DESCRIPTION = "A simple 1D hydraulic model based on the equations of Saint Venant.";
    public static final String DOCUMENTATION = "OmsSaintGeo.html";
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
    public static final String pDeltaTMillis_UNIT = "millisec";
    public static final String outputLevelFile_DESCRIPTION = "Output file with levels.";
    public static final String outputDischargeFile_DESCRIPTION = "Output file with the quantities related to discharge.";
    public static final String pDeltaTMillis_DESCRIPTION = "Time interval.";
    public static final String inConfluenceId2DischargeMap_DESCRIPTION = "Lateral immission from confluences discharge values for each id.";
    public static final String inLateralId2DischargeMap_DESCRIPTION = "Lateral discharge tribute or offtake section discharge values for each id.";
    public static final String inDownstreamLevel_DESCRIPTION = "Input downstream level.";
    public static final String inDischarge_DESCRIPTION = "Input head discharge.";
    // VARS DOC END

    private LinearAlgebra linearAlgebra = new LinearAlgebra();
    private double DELT = -1;
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

    private HashMap<Integer, Double> linkId2LevelMap = new HashMap<>();
    private HashMap<Integer, Double> linkId2RelativeLevelMap = new HashMap<>();
    private HashMap<Integer, Double> linkId2DischargeMap = new HashMap<>();
    private HashMap<Integer, Double> linkId2VelocityMap = new HashMap<>();

    @Execute
    public void process() throws Exception {
        checkNull(inDischarge, inRiverPoints, inSectionPoints, inSections);

        /*
         * FIXME: INSERT THE BOUNDARY CONDITIONS AS INPUT PARAMETERS 
         */
        SCELTA_A_MONTE = 1;
        SCELTA_A_VALLE = 2;
        DELT = pDeltaTMillis / 1000.0;

        List<SimpleFeature> riverPointsFeatures = FeatureUtilities.featureCollectionToList(inRiverPoints);
        List<SimpleFeature> sectionFeatures = FeatureUtilities.featureCollectionToList(inSections);
        List<SimpleFeature> sectionPointsFeatures = FeatureUtilities.featureCollectionToList(inSectionPoints);
        RiverInfo riverInfo = ARiverSectionsExtractor.getRiverInfo(riverPointsFeatures, sectionFeatures, sectionPointsFeatures);
        List<RiverPoint> riverPoints = ARiverSectionsExtractor.riverInfo2RiverPoints(riverInfo);

        int sectionsCount = riverPoints.size();

        try (BufferedWriter outputLevelWriter = new BufferedWriter(new FileWriter(outputLevelFile));
                BufferedWriter outputDischargeWriter = new BufferedWriter(new FileWriter(outputDischargeFile));) {

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
                linkId2LevelMap.clear();
                linkId2RelativeLevelMap.clear();
                linkId2DischargeMap.clear();
                linkId2VelocityMap.clear();

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

                idrgeo = wettedArea(waterLevel, riverPoints);

                /* write the output file with levels */
                StringBuilder sbLevel = new StringBuilder();
                sbLevel.append("\n#timestep: " + timeIndex + "\n");
                for( int i = 0; i < sectionsCount - 1; i++ ) {
                    RiverPoint section = riverPoints.get(i);
                    Coordinate[] sectionCoordinates = section.getSectionCoordinates();
                    int sectionId = section.getSectionId();
                    sbLevel.append(sectionId).append(";");
                    sbLevel.append(section.getProgressiveDistance()).append(";");
                    sbLevel.append(waterLevel[i]).append(";");
                    double minsez = section.getMinElevation();
                    sbLevel.append(minsez).append(";");
                    int dx = section.getStartNodeIndex();
                    sbLevel.append(sectionCoordinates[dx].z).append(";");
                    int sx = section.getEndNodeIndex();
                    sbLevel.append(sectionCoordinates[sx].z).append("\n");
                    linkId2LevelMap.put(sectionId, waterLevel[i]);
                    linkId2RelativeLevelMap.put(sectionId, (waterLevel[i] - minsez));
                }
                RiverPoint section = riverPoints.get(sectionsCount - 1);
                Coordinate[] sectionCoordinates = section.getSectionCoordinates();
                int sectionId = section.getSectionId();
                sbLevel.append(sectionId).append(";");
                sbLevel.append(section.getProgressiveDistance()).append(";");
                sbLevel.append(waterLevel[sectionsCount - 1]).append(";");
                linkId2LevelMap.put(sectionId, waterLevel[sectionsCount - 1]);
                double minsez = section.getMinElevation();
                sbLevel.append(minsez).append(";");
                linkId2RelativeLevelMap.put(sectionId, (waterLevel[sectionsCount - 1] - minsez));
                int dx = section.getStartNodeIndex();
                sbLevel.append(sectionCoordinates[dx].z).append(";");
                int sx = section.getEndNodeIndex();
                sbLevel.append(sectionCoordinates[sx].z);

                outputLevelWriter.write(sbLevel.toString() + "\n");
                pm.message(sbLevel.toString());

                /* write the output file with discharge */
                StringBuilder sbDischarge = new StringBuilder();
                sbDischarge.append("\n#timestep: " + timeIndex + "\n");
                for( int i = 0; i < sectionsCount - 1; i++ ) {
                    RiverPoint sectionDischarge = riverPoints.get(i);
                    sectionId = sectionDischarge.getSectionId();
                    sbDischarge.append(sectionId).append(";");
                    sbDischarge.append(sectionDischarge.getProgressiveDistance()).append(";");
                    double froudeNumber = (Math.abs(celerity[i]) / Math.sqrt(G * (idrgeo[i][0] / idrgeo[i][3])));
                    sbDischarge.append(froudeNumber).append(";");
                    sbDischarge.append(discharge[i] < 0.0 ? 0.0 : discharge[i]).append(";");
                    sbDischarge.append(celerity[i] < 0.0 ? 0.0 : celerity[i]).append(";");
                    sbDischarge.append(idrgeo[i][0]).append("\n");
                    linkId2DischargeMap.put(sectionId, discharge[i]);
                    linkId2VelocityMap.put(sectionId, celerity[i]);
                }
                RiverPoint sectionDischarge = riverPoints.get(sectionsCount - 1);
                double froudeNumber = (Math.abs(discharge[sectionsCount - 2] / idrgeo[sectionsCount - 1][0])
                        / Math.sqrt(G * (idrgeo[sectionsCount - 1][0] / idrgeo[sectionsCount - 1][3])));
                sectionId = sectionDischarge.getSectionId();
                sbDischarge.append(sectionId).append(";");
                sbDischarge.append(sectionDischarge.getProgressiveDistance()).append(";");
                sbDischarge.append(froudeNumber).append(";");
                double lastDischarge = discharge[sectionsCount - 2] < 0.0 ? 0.0 : discharge[sectionsCount - 2];
                sbDischarge.append(lastDischarge).append(";");
                double celDischarge = discharge[sectionsCount - 2] / idrgeo[sectionsCount - 1][0];
                double lastCelerity = celDischarge < 0.0 ? 0.0 : celDischarge;
                sbDischarge.append(lastCelerity).append(";");
                sbDischarge.append(idrgeo[sectionsCount - 1][0]).append(";");
                linkId2DischargeMap.put(sectionId, lastDischarge);
                linkId2VelocityMap.put(sectionId, lastCelerity);

                outputDischargeWriter.write(sbDischarge.toString() + "\n");
                pm.message(sbDischarge.toString());

                pm.worked(1);
            }
            pm.done();
        }
    }

    public HashMap<Integer, Double> getLastLinkId2DischargeMap() {
        return linkId2DischargeMap;
    }

    public HashMap<Integer, Double> getLastLinkId2LevelMap() {
        return linkId2LevelMap;
    }

    public HashMap<Integer, Double> getLastLinkId2RelativeLevelMap() {
        return linkId2RelativeLevelMap;
    }

    public HashMap<Integer, Double> getLastLinkId2VelocityMap() {
        return linkId2VelocityMap;
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
                    pm.errorMessage("Evaluation of the steady flow not possible for the section " + i + "solution not found.");
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
     * This method calculates
     * </p>
     * <ul>
     * <li> wetted area </li>
     * <li> wetted perimeter </li>
     * <li> hydraulic radius </li>
     * <li> width of water surface </li>
     * <li> roughness </li>
     * <li> alpha coefficient of Coriolis </li>
     * </ul>
     * in each section starting from the water depth.
     * <p>
     * The method returns a matrix with:
     * <ul>
     * <li> rows: the sections </li>
     * <li> cols: the variables above. </li>
     * </ul>
     * </p>
     * The method calculates
     * <p>
     * <b>wetted area</b> calculated as the sum of the trapezoids obtained by tracing a vertical
     * line starting from each points of the sections; each trapezoid has a base
     * on the right (base_dx) and a base on the left (base_sx) and an height (altezza). 
     * <b>wetted perimeter</b> as the sum of all the wetted segments of the river ground
     * <b>hydraulic radius</b> using the definition formula
     * <b>width of water surface</b> height of the trapezoids defined to calculate the wetted area
     * <b>roughness coefficient</b> using the Engelund method: the section is divided into trapezoids
     * as for the wetted area and for each trapezoid this quantity is calculated:
     * <b>Ks(j)*Y(j)^(5/3)*B(j)</b> where:
     * </p>
     * <ul>
     * <li><b>Ks(j)</b> Gaukler-Strickler coefficient for the segment j</li>
     * <li><b>Y(j)</b> water depth for the trapezoid j obtained as the ratio between the 
     * area and the width of the water surface for each trapezoid </li>
     * <li><b>B(j)</b> is the width of the water surface of the trapezoid j</li>
     * </ul>
     * <p>
     * NOTE: the hydraulic radius of the trapezoid j is approximated with the water depth even if this
     * is recommended only with large sections.
     * </p>
     * <p>
     * The <b>roughness coefficient</b> is obtained dividing the sum of all the quantities
     * with <b>(A*RH^(2/3))</b>, where A is the total wetted area and RH the hydraulic radius
     * referred to the whole section.
     * </p>
     * <p>
     * <b>The alpha coefficient of Coriolis</b> is calculated using the same subdivision of the
     * section used for the calculation of the wetted area and for each trapezoid this quantity
     * is evaluated: 
     * <b>Ks(j)^2*A(j)^(7/3)/P(j)^(4/3)</b> where:
     * </p>
     * <ul>
     * <li><b>Ks(j)</b> the Gaukler-Strickler coefficient for the segment j</li>
     * <li><b>A(j)</b> the area of the trapezoid j</li>
     * <li><b>P(j)</b> the wetted perimeter of the trapezoid j</li>
     * </ul>
     * <p>
     * This is then calculated by dividing the sum of all the quantities for 
     * <b>(ATOT*KsTOT^2*RHTOT^(4/3))</b> where:
     * <ul>
     * <li>ATOT the area of the whole section</li>
     * <li>KsTOT the roughness total coefficient  </li>
     * <li>RHTOT the hydraulic radius of the whole section</li>
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

            dx = section.getStartNodeIndex() + 1;
            sx = section.getEndNodeIndex() - 1;

            Coordinate[] sectionCoordinates = section.getSectionCoordinates();
            List<Double> sectionProgressives = section.getSectionProgressive();
            double sectionGauklerStrickler = section.getSectionGauklerStrickler();

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
                 * case 1: only partially wetted (right side dry) 
                 */

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
                    gau_b = gau_b + (sectionGauklerStrickler * Math.pow((base_dx + base_sx) / 2.0, (5.0 / 3.0)) * altezza);
                    gau_loc = sectionGauklerStrickler;
                    /* the alpha coefficient of Coriolis */
                    area_loc = (base_dx + base_sx) * altezza / 2.0;
                    peri_loc = Math.sqrt((base_dx - base_sx) * (base_dx - base_sx) + altezza * altezza);
                    if (area_loc != 0) {
                        // use the method of Einstein-Horton?
                        alfa_num = alfa_num
                                + (gau_loc * gau_loc) * Math.pow(area_loc, (7.0 / 3.0)) / Math.pow(peri_loc, (4.0 / 3.0));
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
                    gau_b = gau_b + (sectionGauklerStrickler * Math.pow((base_dx + base_sx) / 2.0, (5.0 / 3.0)) * altezza);
                    gau_loc = sectionGauklerStrickler;

                    /* the alpha coefficient of Coriolis */
                    area_loc = (base_dx + base_sx) * altezza / 2.0;
                    peri_loc = Math.sqrt((base_dx - base_sx) * (base_dx - base_sx) + altezza * altezza);
                    if (area_loc != 0) {
                        alfa_num = alfa_num
                                + (gau_loc * gau_loc) * Math.pow(area_loc, (7.0 / 3.0)) / Math.pow(peri_loc, (4.0 / 3.0));
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
                    gau_b = gau_b + (sectionGauklerStrickler * Math.pow((base_dx + base_sx) / 2, (5.0 / 3.0)) * altezza);
                    gau_loc = sectionGauklerStrickler;

                    area_loc = (base_dx + base_sx) * altezza / 2;
                    peri_loc = Math.sqrt((base_dx - base_sx) * (base_dx - base_sx) + altezza * altezza);
                    if (area_loc != 0) {
                        alfa_num = alfa_num
                                + (gau_loc * gau_loc) * Math.pow(area_loc, (7.0 / 3.0)) / Math.pow(peri_loc, (4.0 / 3.0));
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

    /*
     * calculates the value of the water depth for the current timestep
     */
    private void new_tirante( List<RiverPoint> riverPoints, double[] tirante, double[] Q, double[] U, double[] DELXM,
            int SCELTA_A_MONTE, double qin, double qin_old, int SCELTA_A_VALLE, double tiranteout, double[] ql ) {

        double uu;
        double base, C1, C2, Ci, C_old, dx;
        double omegam, zetam;
        double minsez, mindx, umax;
        int ds, sx;
        double T1, T2, A1dx, A2dx, A1sx, A2sx;
        double l, c;
        int imax = riverPoints.size();
        double[][] geomid = new double[imax - 1][6];
        double[] U_I = new double[imax];
        double[] GAM = new double[imax];
        double[] F_Q = new double[imax - 1];
        double[] D = new double[imax];
        double[] DS = new double[imax - 1];
        double[] DI = new double[imax - 1];
        double[] B = new double[imax];
        double[] tirante_old = new double[imax];
        double[] qs = new double[imax - 1];

        // FIXME not sure it is correct
        double tirantein = 0;

        /*
         * Execute the method wettedArea
         * the variable idrgeo contains all the quantities related to water depth and section
         */
        double[][] idrgeo = wettedArea(tirante, riverPoints);
        /*
         * goemid contains all the quantities related to intermediate sections, linear interpolation
         * between the values calculated for the given sections
         */
        for( int i = 0; i < imax - 1; i++ ) {
            for( int j = 0; j < 6; j++ ) {
                geomid[i][j] = (idrgeo[i][j] + idrgeo[i + 1][j]) / 2.0;
            }
        }
        /*
         * Calculate the average velocity for the main sections U_I[] and also for the 
         * intermediate ones U[] 
         */
        U[0] = Q[0] / geomid[0][0];
        for( int i = 1; i < imax - 1; i++ ) {
            U[i] = Q[i] / geomid[i][0];
            U_I[i] = 0.5 * (U[i - 1] + U[i]);
        }
        U_I[0] = 0.5 * (U[0] + qin / (2.0 * idrgeo[0][0] - geomid[0][0]));
        /*
         * Calculate the gamma coefficient.
         */
        for( int i = 0; i < imax - 1; i++ ) {
            uu = U[i];
            GAM[i] = G * Math.abs(uu) / (Math.pow(geomid[i][2], 4.0 / 3.0) * Math.pow(geomid[i][4], 2.0));
            GAM[i] = GAM[i] + ql[i] / geomid[i][0];
        }

        /*
         * Verify to respect the Courant condition
         */

        /* Look for the minimum spatial interval and the maximum velocity */
        mindx = DELXM[0];
        umax = Math.abs(U[0]);
        for( int i = 0; i < imax - 2; i++ ) {
            dx = (DELXM[i] + DELXM[i + 1]) / 2.0;
            if (dx <= mindx)
                mindx = dx;
            if (Math.abs(U[i]) >= umax)
                umax = Math.abs(U[i]);
        }
        DELT = 0.1 * mindx / umax;

        // Does it have to be initialized?
        double qout = Q[imax - 2];
        /*
         * Apply the FQ function to solve the finite difference schema.
         */
        // if (SCELTA_A_VALLE != 3)
        // qout = Q[imax - 2];
        FQ(F_Q, Q, U_I, U, idrgeo, riverPoints, DELT, qin, qout);

        /*
         * Calculate the overflow discharge for each section
         */
        for( int i = 0; i < imax - 1; i++ ) {
            // current section
            RiverPoint section_i = riverPoints.get(i);
            // next section (downstream)
            RiverPoint section_ip = riverPoints.get(i + 1);
            Coordinate[] sectionCoordinates_i = section_i.getSectionCoordinates();
            Coordinate[] sectionCoordinates_ip = section_ip.getSectionCoordinates();
            qs[i] = 0; // overflow discharge

            // define the height of the water surface and the banks on the left side
            T1 = tirante[i];
            T2 = tirante[i + 1];
            ds = section_i.getStartNodeIndex();
            // height of the point outside the bank on the left for the current and for the next
            // section
            A1dx = sectionCoordinates_i[ds].z;
            ds = section_ip.getStartNodeIndex();
            A2dx = sectionCoordinates_ip[ds].z;
            /* calculate the outflow discharge on the right */
            if (T1 > A1dx && T2 > A2dx) {
                l = DELXM[i];
                c = (T2 - T1 - A2dx + A1dx) / l;
                qs[i] = (Cq / (2.5 * c)) * (Math.pow((T2 - A2dx), (5.0 / 2.0)) - Math.pow((T1 - A1dx), (5.0 / 2.0)));
            } else if (T1 > A1dx && T2 <= A2dx) {
                l = DELXM[i];
                c = (T2 - T1 - A2dx + A1dx) / l;
                qs[i] = (Cq / (2.5 * c)) * (-Math.pow((T1 - A1dx), (5.0 / 2.0)));
            } else if (T1 <= A1dx && T2 > A2dx) {
                l = DELXM[i];
                c = (T2 - T1 - A2dx + A1dx) / l;
                qs[i] = (Cq / (2.5 * c)) * (Math.pow((T2 - A2dx), (5.0 / 2.0)));
            }
            // define the height of the water surface and the banks on the right side
            // T1 = tirante[i];
            // T2 = tirante[i + 1];
            sx = section_i.getEndNodeIndex();
            A1sx = sectionCoordinates_i[sx].z;
            sx = section_ip.getEndNodeIndex();
            A2sx = sectionCoordinates_ip[sx].z;
            /* calculate the outflow discharge on the right */
            if (T1 > A1sx && T2 > A2sx) {
                l = DELXM[i];
                c = (T2 - T1 - A2sx + A1sx) / l;
                qs[i] = qs[i] + (Cq / (2.5 * c)) * (Math.pow((T2 - A2sx), (5.0 / 2.0)) - Math.pow((T1 - A1sx), (5.0 / 2.0)));
            } else if (T1 > A1sx && T2 <= A2sx) {
                l = DELXM[i];
                c = (T2 - T1 - A2sx + A1sx) / l;
                qs[i] = qs[i] + (Cq / (2.5 * c)) * (-Math.pow((T1 - A1sx), (5.0 / 2.0)));
            } else if (T1 <= A1sx && T2 > A2sx) {
                l = DELXM[i];
                c = (T2 - T1 - A2sx + A1sx) / l;
                qs[i] = qs[i] + (Cq / (2.5 * c)) * (Math.pow((T2 - A2sx), (5.0 / 2.0)));
            }
        }

        /*******************************************************************************************
         * Define the coefficient of the matrix and the denominate number considering the
         * upstream and downstream assigned conditions. 
         * ******************************************************************************************
         * FIRST CASE: UPSTREAM CONDITION 1 DOWNSTREAM CONDITION 1
         ******************************************************************************************/
        if (SCELTA_A_MONTE == 1 && SCELTA_A_VALLE == 1) {
            tirante[imax - 1] = tiranteout;
            /* the coefficients of the first line */
            dx = DELXM[0];
            base = (idrgeo[0][3] + geomid[0][3]) / 2.0;
            C1 = (G * DELT * geomid[0][0]) / (DELXM[0] * (1.0 + DELT * GAM[0]));
            DS[0] = -C1 / dx;
            D[0] = C1 / dx + base / DELT;
            B[0] = (base / DELT) * tirante[0] - F_Q[0] / (dx * (1.0 + DELT * GAM[0])) + qin / dx + ql[imax - 2] - qs[imax - 2];
            /* the coefficients of the second line */
            dx = (DELXM[1] + DELXM[0]) / 2.0;
            Ci = C1;
            for( int i = 1; i < imax - 2; i++ ) {
                dx = (DELXM[i] + DELXM[i - 1]) / 2.0;
                base = (geomid[i - 1][3] + geomid[i][3]) / 2.0;
                C_old = Ci;
                Ci = (G * DELT * geomid[i][0]) / (DELXM[i] * (1.0 + DELT * GAM[i]));
                D[i] = Ci / dx + C_old / dx + (base / DELT);
                DI[i - 1] = -C_old / dx;
                DS[i] = -Ci / dx;
                B[i] = (base / DELT) * tirante[i] - F_Q[i] / (dx * (1.0 + DELT * GAM[i]))
                        + F_Q[i - 1] / (dx * (1.0 + DELT * GAM[i - 1])) + ql[i] - qs[i];
            }
            /* the coefficients of the last line */
            dx = (DELXM[imax - 3] + DELXM[imax - 2]) / 2.0;
            base = (geomid[imax - 3][3] + geomid[imax - 2][3]) / 2.0;
            C_old = (G * DELT * geomid[imax - 3][0]) / (DELXM[imax - 3] * (1.0 + DELT * GAM[imax - 3]));
            Ci = (G * DELT * geomid[imax - 2][0]) / (DELXM[imax - 2] * (1.0 + DELT * GAM[imax - 2]));
            DI[imax - 3] = -C_old / dx;
            DS[imax - 2] = 0;
            D[imax - 2] = C_old / dx + Ci / dx + base / DELT;
            B[imax - 2] = base / DELT * tirante[imax - 2] + Ci / dx * tiranteout
                    - F_Q[imax - 2] / (dx * (1.0 + DELT * GAM[imax - 2])) + F_Q[imax - 3] / (dx * (1.0 + DELT * GAM[imax - 3]))
                    + ql[imax - 2] - qs[imax - 2];
            /* Move the values of the water height at time n in the vector tirante_old[] */
            for( int i = 0; i < imax; i++ )
                tirante_old[i] = tirante[i];
            /*
             * The function ris_sistema calculates the values of the water height at time n+1
             * and save them in the vector tirante[]
             */
            linearAlgebra.ris_sistema(D, DS, DI, B, tirante, imax - 1);

            /*
             * Check on the water depth: if during the elaborations the height of the water depth
             * is less than the minimum, this minimum value is assigned
             */
            for( int i = 0; i < imax; i++ ) {
                minsez = riverPoints.get(i).getMinElevation();
                if (minsez >= tirante[i])
                    tirante[i] = minsez + h_DEF;
            }
            tirante[imax - 1] = tiranteout;
            /* Calculate the discharge and the velocities at time n+1. */
            for( int i = 0; i < imax - 1; i++ ) {
                Q[i] = F_Q[i] / (1.0 + (DELT * GAM[i]))
                        - (G * DELT * geomid[i][0]) / (DELXM[i] * (1.0 + DELT * GAM[i])) * (tirante[i + 1] - tirante[i]);
            }
            dx = (DELXM[imax - 2] + DELXM[imax - 3]) / 2.0;
            base = (geomid[imax - 3][3] + geomid[imax - 2][3]) / 2.0;
            Q[imax - 2] = Q[imax - 3];
        }

        /*******************************************************************************************
         * SECOND CASE: UPSTREAM CONDITION 1 DOWNSTREAM CONDITION 2
         ******************************************************************************************/
        if (SCELTA_A_MONTE == 1 && SCELTA_A_VALLE == 2) {
            /* the coefficients of the first line */
            dx = DELXM[0];
            base = (idrgeo[0][3] + geomid[0][3]) / 2.0;
            C1 = (G * DELT * geomid[0][0]) / (DELXM[0] * (1.0 + DELT * GAM[0]));
            DS[0] = -C1 / dx;
            D[0] = C1 / dx + base / DELT;
            B[0] = (base / DELT) * tirante[0] - F_Q[0] / (dx * (1.0 + DELT * GAM[0])) + qin / dx + ql[0] - qs[0];
            /* the coefficients of the second line */
            dx = (DELXM[1] + DELXM[0]) / 2.0;
            Ci = C1;
            for( int i = 1; i < imax - 1; i++ ) {
                dx = (DELXM[i] + DELXM[i - 1]) / 2.0;
                base = (geomid[i - 1][3] + geomid[i][3]) / 2.0;
                C_old = Ci;
                Ci = (G * DELT * geomid[i][0]) / (DELXM[i] * (1.0 + DELT * GAM[i]));
                D[i] = Ci / dx + C_old / dx + (base / DELT);
                DI[i - 1] = -C_old / dx;
                DS[i] = -Ci / dx;
                B[i] = (base / DELT) * tirante[i] - F_Q[i] / (dx * (1.0 + DELT * GAM[i]))
                        + F_Q[i - 1] / (dx * (1.0 + DELT * GAM[i - 1])) + ql[i] - qs[i];
            }
            /* the coefficients of the last line */
            dx = DELXM[imax - 2];
            base = (geomid[imax - 2][3] + idrgeo[imax - 1][3]) / 2.0;
            zetam = tirante[imax - 1] - ((idrgeo[imax - 1][0] + geomid[imax - 2][0]) / 2.0) / base;

            omegam = base * Math.sqrt(G * (tirante[imax - 1] - zetam));
            C_old = (G * DELT * geomid[imax - 2][0]) / (DELXM[imax - 2] * (1.0 + DELT * GAM[imax - 2]));
            DI[imax - 2] = -C_old / dx;
            D[imax - 1] = base / DELT + C_old / dx + omegam / dx;
            B[imax - 1] = base / DELT * tirante[imax - 1] + F_Q[imax - 2] / (dx * (1.0 + DELT * GAM[imax - 2]))
                    + omegam * zetam / dx + ql[imax - 2] - qs[imax - 2];

            /* Move the values of the water height at time n in the vector tirante_old[] */
            for( int i = 0; i < imax; i++ )
                tirante_old[i] = tirante[i];
            /*
             * The function ris_sistema calculates the values of the water height at time n+1
             * and save them in the vector tirante[]
             */
            linearAlgebra.ris_sistema(D, DS, DI, B, tirante, imax);
            /* Calculate the discharge and the velocities at time n+1. */
            for( int i = 0; i < imax - 1; i++ ) {
                Q[i] = F_Q[i] / (1.0 + (DELT * GAM[i]))
                        - (G * DELT * geomid[i][0]) / (DELXM[i] * (1.0 + DELT * GAM[i])) * (tirante[i + 1] - tirante[i]);
            }
            /*
             * Check on the water depth: if during the elaborations the height of the water depth
             * is less than the minimum, this minimum value is assigned
             */
            for( int i = 0; i < imax; i++ ) {
                minsez = riverPoints.get(i).getMinElevation();
                if (minsez >= tirante[i])
                    tirante[i] = minsez + h_DEF;
            }
        }

        /*******************************************************************************************
         * THIRD CASE: UPSTREAM CONDITION 1 DOWNSTREAM CONDITION 3
         ******************************************************************************************/
        if (SCELTA_A_MONTE == 1 && SCELTA_A_VALLE == 3) {
            /* the coefficients of the first line */
            dx = DELXM[0];
            base = (idrgeo[0][3] + geomid[0][3]) / 2.0;
            C1 = (G * DELT * geomid[0][0]) / (DELXM[0] * (1.0 + DELT * GAM[0]));
            DS[0] = -C1 / dx;
            D[0] = C1 / dx + base / DELT;
            B[0] = (base / DELT) * tirante[0] - F_Q[0] / (dx * (1.0 + DELT * GAM[0])) + qin / dx + ql[0] - qs[0];
            /* the coefficients from the second to the penultimate line */
            dx = (DELXM[1] + DELXM[0]) / 2.0;
            Ci = C1;
            for( int i = 1; i < imax - 1; i++ ) {
                dx = (DELXM[i] + DELXM[i - 1]) / 2.0;
                base = (geomid[i - 1][3] + geomid[i][3]) / 2.0;
                C_old = Ci;
                Ci = (G * DELT * geomid[i][0]) / (DELXM[i] * (1.0 + DELT * GAM[i]));
                D[i] = Ci / dx + C_old / dx + (base / DELT);
                DI[i - 1] = -C_old / dx;
                DS[i] = -Ci / dx;
                B[i] = (base / DELT) * tirante[i] - F_Q[i] / (dx * (1.0 + DELT * GAM[i]))
                        + F_Q[i - 1] / (dx * (1 + DELT * GAM[i - 1])) + ql[i] - qs[i];
            }
            /* the coefficients of the last line */
            dx = DELXM[imax - 2];
            base = (geomid[imax - 2][3] + idrgeo[imax - 1][3]) / 2.0;
            C_old = (G * DELT * geomid[imax - 2][0]) / (DELXM[imax - 2] * (1.0 + DELT * GAM[imax - 2]));
            DI[imax - 2] = -C_old / dx;
            D[imax - 1] = base / DELT + C_old / dx;
            B[imax - 1] = base / DELT * tirante[imax - 1] - Q[imax - 2] / DELXM[imax - 2]
                    + F_Q[imax - 2] / (dx * (1.0 + DELT * GAM[imax - 2])) + ql[imax - 2] - qs[imax - 2];
            /* Move the values of the water height at time n in the vector tirante_old[] */
            for( int i = 0; i < imax; i++ )
                tirante_old[i] = tirante[i];
            /*
             * The function ris_sistema calculates the values of the water height at time n+1
             * and save them in the vector tirante[]
             */
            linearAlgebra.ris_sistema(D, DS, DI, B, tirante, imax - 1);
            /* Calculate the discharge and the velocities at time n+1. */
            Q[0] = qin;
            for( int i = 1; i < imax - 2; i++ ) {
                Q[i] = F_Q[i] / (1.0 + (DELT * GAM[i]))
                        - (G * DELT * geomid[i][0]) / (DELXM[i] * (1.0 + DELT * GAM[i])) * (tirante[i + 1] - tirante[i]);
            }
            Q[imax - 2] = Q[imax - 3];

            /*
             * Check on the water depth: if during the elaborations the height of the water depth
             * is less than the minimum, this minimum value is assigned
             */
            for( int i = 0; i < imax; i++ ) {
                minsez = riverPoints.get(i).getMinElevation();
                if (minsez >= tirante[i]) {
                    tirante[i] = minsez + h_DEF;
                }
                if (i == imax - 1) {
                    tirante[imax - 1] = minsez + geomid[imax - 2][0] / geomid[imax - 2][3];
                }
            }
            /*
             * TODO: add the calculation of water depth and velocity and the check on the water depth (min_value)
             */
        }

        /*******************************************************************************************
         * FOURTH CASE: UPSTREAM CONDITION 2 DOWNSTREAM CONDITION 1
         ******************************************************************************************/
        if (SCELTA_A_MONTE == 2 && SCELTA_A_VALLE == 1) {
            /* the coefficients of the first line */
            C1 = (G * DELT * geomid[0][0]) / (2.0 * DELXM[0] * DELXM[0] * (1.0 + DELT * GAM[0]));
            DS[0] = -C1;
            D[0] = 0;
            B[0] = -(idrgeo[0][3] / DELT + C1) * tirantein + (idrgeo[0][3] / DELT - C1) * tirante[0] + C1 * tirante[1]
                    - F_Q[0] / (DELXM[0] * (1.0 + DELT * GAM[0])) + (qin - Q[0] + qin_old) / DELXM[0];

            /* the coefficients of the second line */
            dx = (DELXM[0] + DELXM[1]) / 2.0;
            C1 = (G * DELT * geomid[0][0]) / (4.0 * dx * DELXM[0] * (1.0 + DELT * GAM[0]));
            C2 = (G * DELT * geomid[1][0]) / (4.0 * dx * DELXM[1] * (1.0 + DELT * GAM[1]));
            D[1] = (idrgeo[1][3] / DELT + C1 + C2);
            DS[1] = -C2;
            DI[0] = 0;
            B[1] = (idrgeo[1][3] / DELT - C1 - C2) * tirante[1] + C2 * tirante[2] + C1 * (tirantein + tirante[0])
                    - F_Q[1] / (2.0 * dx * (1.0 + DELT * GAM[1])) + F_Q[0] / (2.0 * dx * (1.0 + DELT * GAM[0]))
                    - (Q[1] - Q[0]) / (2.0 * dx);

            /* the coefficients of the third last line */
            dx = (DELXM[1] + DELXM[0]) / 2.0;
            Ci = (G * DELT * geomid[1][0]) / (4.0 * dx * DELXM[1] * (1.0 + DELT * GAM[1]));
            for( int i = 2; i < imax - 2; i++ ) {
                dx = (DELXM[i] + DELXM[i - 1]) / 2.0;
                C_old = Ci;
                Ci = (G * DELT * geomid[i][0]) / (4.0 * dx * DELXM[i] * (1.0 + DELT * GAM[i]));
                D[i] = Ci + C_old + (idrgeo[i][3] / DELT);
                DI[i - 1] = -C_old;
                DS[i] = -Ci;
                B[i] = (idrgeo[i][3] / DELT - Ci - C_old) * tirante[i] + Ci * tirante[i + 1] + C_old * tirante[i - 1]
                        - F_Q[i] / (2.0 * dx * (1.0 + DELT * GAM[i])) + F_Q[i - 1] / (2.0 * dx * (1.0 + DELT * GAM[i - 1]))
                        - (Q[i] - Q[i - 1]) / (2.0 * dx);
            }

            /* the coefficients of the penultimate line */
            dx = (DELXM[imax - 3] + DELXM[imax - 2]) / 2.0;
            C_old = (G * DELT * geomid[imax - 3][0]) / (4.0 * dx * DELXM[imax - 3] * (1.0 + DELT * GAM[imax - 3]));
            Ci = (G * DELT * geomid[imax - 2][0]) / (4.0 * dx * DELXM[imax - 2] * (1.0 + DELT * GAM[imax - 2]));
            DI[imax - 3] = -C_old;
            DS[imax - 2] = 0;
            D[imax - 2] = C_old + Ci + (idrgeo[imax - 2][3]) / DELT;
            B[imax - 2] = (-C_old - Ci + (idrgeo[imax - 2][3]) / DELT) * tirante[imax - 2] + Ci * (tiranteout + tirante[imax - 1])
                    + C_old * tirante[imax - 3] - F_Q[imax - 2] / (2.0 * dx * (1.0 + DELT * GAM[imax - 2]))
                    + F_Q[imax - 3] / (2.0 * dx * (1.0 + DELT * GAM[imax - 3])) - (Q[imax - 2] - Q[imax - 3]) / (2.0 * dx);

            /* the coefficients of the last line */
            dx = DELXM[imax - 2];
            C_old = (G * DELT * geomid[imax - 2][0]) / (2.0 * dx * dx * (1.0 + DELT * GAM[imax - 2]));
            DI[imax - 2] = -C_old;
            D[imax - 1] = 1.0 / dx;
            B[imax - 1] = -C_old * (tiranteout + tirante[imax - 1] - tirante[imax - 2])
                    - (idrgeo[imax - 1][3] / DELT) * (tiranteout - tirante[imax - 1])
                    + F_Q[imax - 2] / (dx * (1.0 + DELT * GAM[imax - 2])) - (qout - Q[imax - 2]) / dx;
        }

        /*******************************************************************************************
         * FIFTH CASE: UPSTREAM CONDITION 2 DOWNSTREAM CONDITION 2
         ******************************************************************************************/
        if (SCELTA_A_MONTE == 2 && SCELTA_A_VALLE == 2) {
            /* the coefficients of the first line */
            C1 = (G * DELT * geomid[0][0]) / (2.0 * DELXM[0] * DELXM[0] * (1.0 + DELT * GAM[0]));
            DS[0] = -C1;
            D[0] = 0;
            B[0] = -(idrgeo[0][3] / DELT + C1) * tirantein + (idrgeo[0][3] / DELT - C1) * tirante[0] + C1 * tirante[1]
                    - F_Q[0] / (DELXM[0] * (1.0 + DELT * GAM[0])) + (qin - Q[0] + qin_old) / DELXM[0];

            /* the coefficients of the second line */
            dx = (DELXM[0] + DELXM[1]) / 2.0;
            C1 = (G * DELT * geomid[0][0]) / (4.0 * dx * DELXM[0] * (1.0 + DELT * GAM[0]));
            C2 = (G * DELT * geomid[1][0]) / (4.0 * dx * DELXM[1] * (1.0 + DELT * GAM[1]));
            D[1] = (idrgeo[1][3] / DELT + C1 + C2);
            DS[1] = -C2;
            DI[0] = 0;
            B[1] = (idrgeo[1][3] / DELT - C1 - C2) * tirante[1] + C2 * tirante[2] + C1 * (tirantein + tirante[0])
                    - F_Q[1] / (2.0 * dx * (1.0 + DELT * GAM[1])) + F_Q[0] / (2.0 * dx * (1.0 + DELT * GAM[0]))
                    - (Q[1] - Q[0]) / (2.0 * dx);
            /* the coefficients from the third to the penultimate line */
            dx = (DELXM[1] + DELXM[0]) / 2.0;
            Ci = (G * DELT * geomid[0][0]) / (4.0 * dx * DELXM[0] * (1.0 + DELT * GAM[0]));
            for( int i = 1; i < imax - 1; i++ ) {
                dx = (DELXM[i] + DELXM[i - 1]) / 2.0;
                C_old = Ci;
                Ci = (G * DELT * geomid[i][0]) / (4.0 * dx * DELXM[i] * (1.0 + DELT * GAM[i]));
                D[i] = Ci + C_old + (idrgeo[i][3] / DELT);
                DI[i - 1] = -C_old;
                DS[i] = -Ci;
                B[i] = (idrgeo[i][3] / DELT - Ci - C_old) * tirante[i] + Ci * tirante[i + 1] + C_old * tirante[i - 1]
                        - F_Q[i] / (2.0 * dx * (1 + DELT * GAM[i])) + F_Q[i - 1] / (2.0 * dx * (1.0 + DELT * GAM[i - 1]))
                        - (Q[i] - Q[i - 1]) / (2.0 * dx);
            }
            /* the coefficients of the last line */
            omegam = Math.sqrt(G * idrgeo[imax - 1][0] * idrgeo[imax - 1][3]);
            zetam = tirante[imax - 1] - idrgeo[imax - 1][0] / idrgeo[imax - 1][3];
            dx = DELXM[imax - 2];
            C_old = (G * DELT * geomid[imax - 2][0]) / (2.0 * dx * dx * (1.0 + DELT * GAM[imax - 2]));
            DI[imax - 2] = -C_old;
            D[imax - 1] = idrgeo[imax - 1][3] / DELT + C_old + omegam;
            B[imax - 1] = (idrgeo[imax - 1][3] / DELT - C_old) * tirante[imax - 1] + C_old * tirante[imax - 2]
                    + F_Q[imax - 2] / (dx * (1.0 + DELT * GAM[imax - 2])) + (-qout + Q[imax - 2] + omegam * zetam) / dx;
        }

        /*******************************************************************************************
         * SIXTH CASE: UPSTREAM CONDITION 2 DOWNSTREAM CONDITION 3
         ******************************************************************************************/
        if (SCELTA_A_MONTE == 2 && SCELTA_A_VALLE == 3) {
            /* the coefficients of the first line */
            C1 = (G * DELT * geomid[0][0]) / (2.0 * DELXM[0] * DELXM[0] * (1.0 + DELT * GAM[0]));
            DS[0] = -C1;
            D[0] = 0;
            B[0] = -(idrgeo[0][3] / DELT + C1) * tirantein + (idrgeo[0][3] / DELT - C1) * tirante[0] + C1 * tirante[1]
                    - F_Q[0] / (DELXM[0] * (1.0 + DELT * GAM[0])) + (qin - Q[0] + qin_old) / DELXM[0];
            /* the coefficients of the second line */
            dx = (DELXM[0] + DELXM[1]) / 2.0;
            C1 = (G * DELT * geomid[0][0]) / (4.0 * dx * DELXM[0] * (1.0 + DELT * GAM[0]));
            C2 = (G * DELT * geomid[1][0]) / (4.0 * dx * DELXM[1] * (1.0 + DELT * GAM[1]));
            D[1] = (idrgeo[1][3] / DELT + C1 + C2);
            DS[1] = -C2;
            DI[0] = 0;
            B[1] = (idrgeo[1][3] / DELT - C1 - C2) * tirante[1] + C2 * tirante[2] + C1 * (tirantein + tirante[0])
                    - F_Q[1] / (2.0 * dx * (1.0 + DELT * GAM[1])) + F_Q[0] / (2.0 * dx * (1.0 + DELT * GAM[0]))
                    - (Q[1] - Q[0]) / (2.0 * dx);
            /* the coefficients from the third to the penultimate line */
            dx = (DELXM[1] + DELXM[0]) / 2.0;
            Ci = (G * DELT * geomid[0][0]) / (4.0 * dx * DELXM[0] * (1.0 + DELT * GAM[0]));
            for( int i = 1; i < imax - 1; i++ ) {
                dx = (DELXM[i] + DELXM[i - 1]) / 2.0;
                C_old = Ci;
                Ci = (G * DELT * geomid[i][0]) / (4.0 * dx * DELXM[i] * (1.0 + DELT * GAM[i]));
                D[i] = Ci + C_old + (idrgeo[i][3] / DELT);
                DI[i - 1] = -C_old;
                DS[i] = -Ci;
                B[i] = (idrgeo[i][3] / DELT - Ci - C_old) * tirante[i] + Ci * tirante[i + 1] + C_old * tirante[i - 1]
                        - F_Q[i] / (2.0 * dx * (1.0 + DELT * GAM[i])) + F_Q[i - 1] / (2.0 * dx * (1.0 + DELT * GAM[i - 1]))
                        - (Q[i] - Q[i - 1]) / (2.0 * dx);
            }
            /* the coefficients of the last line */
            dx = DELXM[imax - 2];
            base = (geomid[imax - 2][3] + idrgeo[imax - 1][3]) / 2.0;
            C_old = (G * DELT * geomid[imax - 2][0]) / (DELXM[imax - 2] * (1.0 + DELT * GAM[imax - 2]));
            DI[imax - 2] = -C_old / dx;
            D[imax - 1] = base / DELT + C_old / dx;
            B[imax - 1] = base / DELT * tirante[imax - 1] + F_Q[imax - 2] / (dx * (1.0 + DELT * GAM[imax - 2]))
                    - Q[imax - 2] / DELXM[imax - 2] + ql[imax - 2] - qs[imax - 2];
            /* Move the values of the water height at time n in the vector tirante_old[] */
            for( int i = 0; i < imax; i++ )
                tirante_old[i] = tirante[i];

            /*
             * The function ris_sistema calculates the values of the water height at time n+1
             * and save them in the vector tirante[]
             */
            linearAlgebra.ris_sistema(D, DS, DI, B, tirante, imax);
            /* Calculate the discharge and the velocities at time n+1. */
            /* Q[1]=qin; */
            for( int i = 0; i < imax - 1; i++ ) {
                Q[i] = F_Q[i] / (1.0 + (DELT * GAM[i]))
                        - (G * DELT * geomid[i][0]) / (DELXM[i] * (1.0 + DELT * GAM[i])) * (tirante[i + 1] - tirante[i]);
            }
            /*
             * Check on the water depth: if during the elaborations the height of the water depth
             * is less than the minimum, this minimum value is assigned
             */
            for( int i = 0; i < imax; i++ ) {
                minsez = riverPoints.get(i).getMinElevation();
                if (minsez >= tirante[i]) {
                    tirante[i] = minsez + h_DEF;
                }
                if (i == imax - 1) {
                    tirante[imax - 1] = minsez + geomid[imax - 2][0] / geomid[imax - 2][3];
                }
            }

        }
    }

    /**
     * Finite difference operator with a discretization upwind.
     * 
     * <pre>
     *  This method calculates the value of FQ for the time n and in the position i+1/2
     *  where F is the difference operatore using a discretization upwind.
     * </pre>
     * 
     * @param F_Q
     * @param Q discharge at time n;
     * @param U_I average velocity in the measured sections;
     * @param U
     * @param idrgeo containing all the values for the sections i, i+1, ... at time n;
     * @param riverPoints contains all the values related to the measured sections;
     * @param delta_T internal timestep;
     * @param qin input discharge;
     * @param qout output discharge.
     */
    private void FQ( double[] F_Q, double[] Q, double[] U_I, double[] U, double[][] idrgeo, List<RiverPoint> riverPoints,
            double delta_T, double qin, double qout ) {

        double coeff;
        /* Coefficient of Coriolis in the sections i e i+1 */
        double alfa, alfa_i, alfa_ii;
        /* Average velocity in the sections i e i+1 */
        double u, u_i, u_ii;
        /* Discharge in the sections i-0.5, i+0.5, i+1.5 */
        double q, q_i, q_ii;

        /* First element of FQ */
        /* Defines the velocities and the Coriolis coefficients */
        u_i = U[0];
        q_i = Q[0];
        alfa_i = 1;
        u_ii = U[1];
        q_ii = Q[1];
        alfa_ii = 1;
        q = qin;
        u = qin / idrgeo[0][0];
        alfa = 1;
        if ((u_i) >= 0) {
            coeff = delta_T / ((riverPoints.get(1).getProgressiveDistance() - riverPoints.get(0).getProgressiveDistance()));
            F_Q[0] = q_i - coeff * (alfa_i * u_i * q_i - alfa * u * q);
        } else {
            coeff = 2 * delta_T / ((riverPoints.get(2).getProgressiveDistance() - riverPoints.get(0).getProgressiveDistance()));
            F_Q[0] = q_i - coeff * (alfa_ii * u_ii * q_ii - alfa_i * u_i * q_i);
        }
        int imax = riverPoints.size();

        /* Intermediate elements */
        for( int i = 1; i < imax - 2; i++ ) {
            /* Defines the velocities and the Coriolis coefficients */
            u_i = U[i];
            q_i = Q[i];
            alfa_i = 1;
            u_ii = U[i + 1];
            q_ii = Q[i + 1];
            alfa_ii = 1;
            u = U[i - 1];
            q = Q[i - 1];
            alfa = 1;
            if ((u_i) >= 0) {
                coeff = 2 * delta_T
                        / ((riverPoints.get(i + 1).getProgressiveDistance() - riverPoints.get(i - 1).getProgressiveDistance()));
                F_Q[i] = q_i - coeff * (alfa_i * u_i * q_i - alfa * u * q);
            } else {
                coeff = 2 * delta_T
                        / ((riverPoints.get(i + 2).getProgressiveDistance() - riverPoints.get(i).getProgressiveDistance()));
                F_Q[i] = q_i - coeff * (alfa_ii * u_ii * q_ii - alfa_i * u_i * q_i);
            }
        }
        /* Last element */
        /* Defines the velocities and the Coriolis coefficients */
        u_i = U[imax - 2];
        q_i = Q[imax - 2];
        alfa_i = 1;
        u_ii = qout / idrgeo[imax - 1][0];
        q_ii = qout;
        alfa_ii = 1;
        coeff = delta_T
                / ((riverPoints.get(imax - 1).getProgressiveDistance() - riverPoints.get(imax - 2).getProgressiveDistance()));
        u = U[imax - 3];
        q = Q[imax - 3];
        alfa = 1;
        if ((u_i) >= 0) {
            coeff = 2 * delta_T
                    / ((riverPoints.get(imax - 1).getProgressiveDistance() - riverPoints.get(imax - 3).getProgressiveDistance()));
            F_Q[imax - 2] = q_i - coeff * (alfa_i * u_i * q_i - alfa * u * q);
        } else {
            coeff = delta_T
                    / ((riverPoints.get(imax - 1).getProgressiveDistance() - riverPoints.get(imax - 2).getProgressiveDistance()));
            F_Q[imax - 2] = q_i - coeff * (alfa_ii * u_ii * q_ii - alfa_i * u_i * q_i);
        }

    }

}