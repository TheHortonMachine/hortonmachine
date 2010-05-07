/*
CUENCAS is a River Network Oriented GIS
Copyright (C) 2005  Ricardo Mantilla

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/*
 * NetworkEquations.java
 *
 * Created on November 11, 2001, 10:26 AM
 */

package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.duffy;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core.DischargeContributor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core.HillSlope;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core.PfafstetterNumber;
/**
 * The duffy model.
 * 
 * This class implements the set of non-linear ordinary differential equations
 * used to simulate flows along the river network. The function is writen as a
 * {@link IBasicFunction.util.ordDiffEqSolver.BasicFunction} that is used by the
 * {@link hydroScalingAPI.util.ordDiffEqSolver.RungeKuttaFelberg}
 * 
 * @author Peter Furey
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class DuffyModel implements IBasicFunction {
    private Calendar myCalendar = Calendar.getInstance();
    private double qd, qs, Q_trib, Qs_trib, K_Q;
    private double flowdepth, hydrad, mannings_n;
    // public double pcoe;

    private double satsurf, mst, qdh, qds, inf, re, qe1, qe2;

    private double THRESHOLD_AREA = 500000; // 0.1Km2
    /*
     * HydroloGIS addons
     */
    public static final int ROUTING_CHEZY_NONEXPL = 2;
    public static final int ROUTING_CHEZY = 3;
    public static final int ROUTING_MANNING = 4;
    private static final double MSTMAX = 1;
    private int routingType = ROUTING_CHEZY;
    private List<HillSlope> orderedHillslopes = null;
    private boolean doLog = false;
    private final IJGTProgressMonitor pm;

    private boolean doPrint = false;
    private List<DischargeContributor> dischargeContributorList = new ArrayList<DischargeContributor>();
    private HashMap<Integer, DischargeDistributor> hillslopeId2DischargeDistributor;

    /**
     * Duffy model function.
     * 
     * @param linksList
     *            the list of all the network links, expressed through their
     *            Pfafstetter numbers
     * @param closureHillslope
     *            the most downstream hillslope in the analyzed basin
     * @param routingType
     *            the type of routing to be used
     * @param pm
     * @param deltaTinMinutes 
     * @param doLog
     */
    public DuffyModel( List<HillSlope> orderedHillslopes, int routingType, IJGTProgressMonitor pm,
            boolean doLog ) {
        this.orderedHillslopes = orderedHillslopes;
        this.routingType = routingType;
        this.pm = pm;
        this.doLog = doLog;

    }

    /**
     * Duffy function evaluation.
     * @param input
     *            initial condition values for every link. The structure is:
     *            <br>
     *            <ul>
     *            <li>link1 initial discharge</li>
     *            <li>link2 initial discharge</li>
     *            <li>link3 initial discharge</li>
     *            <li>...</li>
     *            <li>linkn initial discharge</li>
     *            <li>link1 subsurface and baseflow</li>
     *            <li>link2 subsurface and baseflow</li>
     *            <li>...</li>
     *            <li>linkn subsurface and baseflow</li>
     *            <li>link1 water storage in non saturated zone of the hillslope</li>
     *            <li>link2 water storage in non saturated zone of the hillslope</li>
     *            <li>...</li>
     *            <li>linkn water storage in non saturated zone of the hillslope</li>
     *            <li>link1 water storage in saturated zone of the hillslope</li>
     *            <li>link2 water storage in saturated zone of the hillslope</li>
     *            <li>...</li>
     *            <li>linkn water storage in saturated zone of the hillslope</li>
     *            </ul>
     * @param rainArray
     *            the array of precipitation (in mm/h) for each hillslope
     *            centroid (to be ordered in a consistent way with the
     *            linksList)
     * @param timeinMinutes
     *            the time
     * 
     * @return
     */
    public double[] eval( double currentTimeInMinutes, double[] input, double[] rainArray,
            double[] radiationArray, double[] netshortArray, double[] temperatureArray,
            double[] humidityArray, double[] windspeedArray, double[] pressureArray,
            double[] snowWaterEquivalentArray, boolean isAtFinalSubtimestep ) {
        // the input's length is twice the number of links... the first half
        // corresponds to links
        // discharge and the second to hillslopes storage
        // System.out.println(input.length);

        // define the month
        long currentTimeInMillis = (long) (currentTimeInMinutes * 60.0 * 1000.0);
        myCalendar.setTimeInMillis(currentTimeInMillis);
        int currentMonth = myCalendar.get(Calendar.MONTH) + 1; // jan=1, dec=12

        int linksNum = orderedHillslopes.size(); // linksConectionStruct.headsArray.length;
        // double mstold = 0.0;

        double[] output = new double[input.length];

        for( int i = linksNum - 1; i >= 0; i-- ) {
            // start from the last pieces
            HillSlope currentHillslope = orderedHillslopes.get(i);
            /*
             * NOTE: Initial conditions are ... input[i] for link discharge
             * input[i+nLi] for link base flow input[i+2*nLi] for unsaturated
             * hillslope S1 input[i+3*nLi] for saturated hillslope S2 . input[]
             * is updated for each time step in DiffEqSolver.RKF .
             */

            double prec_mphr = rainArray[i] / 1000.0; // input precipitation is in mm/h

            double area_m2 = currentHillslope.getHillslopeArea();
            // automatically in m2 from the features
            /*
             * Added some check for phisic consistency of the parameters
             */
            // if (input[i + 3 * linksNum] != input[i + 3 * linksNum]) {
            // System.out.println();
            // }
            double minsupdischarge = currentHillslope.parameters.getqqsupmin()
                    * currentHillslope.getUpstreamArea(null) / 1E6;
            if (input[i] < minsupdischarge) {
                input[i] = minsupdischarge;
                // System.out
                // .println(
                // "Current superficial discharge is less than the minimum value, setted to it for the basin "
                // + currentHillslope.getHillslopeId());
            }
            double minsubdischarge = currentHillslope.parameters.getqqsubmin()
                    * currentHillslope.getUpstreamArea(null) / 1E6;
            if (input[i + linksNum] < minsubdischarge) {
                input[i + linksNum] = minsubdischarge;
                // System.out
                // .println(
                // "Current subsuperficial discharge is less than the minimum value, setted to it for the basin "
                // + currentHillslope.getHillslopeId());
            }
            if (input[i + 2 * linksNum] < currentHillslope.parameters.getS1residual()) {
                input[i + 2 * linksNum] = currentHillslope.parameters.getS1residual();
                // System.out
                // .println(
                // "Current S1 parameter is less than the minimum value, setted to it for the basin "
                // + currentHillslope.getHillslopeId());
            }
            if (input[i + 3 * linksNum] < currentHillslope.parameters.getS2residual()) {
                input[i + 3 * linksNum] = currentHillslope.parameters.getS2residual();
                // System.out
                // .println(
                // "Current S2 parameter is less than the minimum value, setted to it for the basin "
                // + currentHillslope.getHillslopeId());
            }

            /* HILLSLOPE FLUX CONDITIONS */
            satsurf = currentHillslope.parameters.getS2Param() * (input[i + 3 * linksNum]); // dimless
            // double areasat = satsurf * area_m2;
            mst = (input[i + 2 * linksNum])
                    / (currentHillslope.parameters.getS2max() - (input[i + 3 * linksNum])); // dimless
            if (Double.isInfinite(mst)) {
                mst = MSTMAX;
            }

            // if ((mst - mstold) > 0.01) {
            // System.out.println("mst " + mst + "mstold " + mstold);
            // mstold = mst;
            // }
            // Ku = hillSlopesInfo.Ks(currentHillslope)
            // * (Math.pow(mst, hillSlopesInfo.MstExp(currentHillslope))); //
            // mphr

            /* HILLSLOPE S1-SURFACE FLUX VALUES */
            if (prec_mphr < currentHillslope.parameters.getKs()) {
                inf = (1.0 - satsurf) * area_m2 * prec_mphr; // m3phr
                qdh = 0.0; // m3phr
            } else {
                inf = (1.0 - satsurf) * area_m2 * currentHillslope.parameters.getKs(); // m3phr
                qdh = (1.0 - satsurf) * area_m2 * (prec_mphr - currentHillslope.parameters.getKs()); // m3phr
            }

            Double eTrate = currentHillslope.parameters.getETrate();
            if (eTrate == null && currentHillslope.hasVegetation()) {
                qe1 = currentHillslope.parameters.calculateEvapoTranspiration(currentMonth,
                        radiationArray[i], pressureArray[i], temperatureArray[i], netshortArray[i],
                        humidityArray[i], windspeedArray[i], input[i + 2 * linksNum],
                        snowWaterEquivalentArray[i]);
            } else {
                if (input[i + 2 * linksNum] > currentHillslope.parameters.getS1residual()) {
                    qe1 = eTrate * area_m2 * (1.0 - satsurf) * mst; // m3phr
                } else {
                    qe1 = 0.0;
                }
            }

            /* HILLSLOPE S1-S2 FLUX VALUE */
            // re = 1100.0
            // * (input[i + 2 * linksNum] / currentHillslope.parameters.getS2max())
            // + 300.0
            // * ((input[i + 2 * linksNum] / currentHillslope.parameters.getS2max()) + 5)
            // * Math.pow((input[i + 3 * linksNum] / currentHillslope.parameters.getS2max()),
            // 2.0);
            re = currentHillslope.parameters.getKs() * area_m2 * (1.0 - satsurf)
                    * (Math.pow(mst, currentHillslope.parameters.getMstExp())); // m3phr

            /* HILLSLOPE S2-SURFACE FLUX VALUES */
            qds = satsurf * area_m2 * prec_mphr; // m3phr

            if (eTrate == null && currentHillslope.hasVegetation()) {
                qe2 = currentHillslope.parameters.calculateEvapoTranspiration(currentMonth,
                        radiationArray[i], pressureArray[i], temperatureArray[i], netshortArray[i],
                        humidityArray[i], windspeedArray[i], input[i + 3 * linksNum],
                        snowWaterEquivalentArray[i]);
            } else {
                qe2 = eTrate * area_m2 * satsurf; // m3phr,
            }

            qs = currentHillslope.parameters.getRecParam() * (input[i + 3 * linksNum]); // m3phr

            /* HILLSLOPE DIRECT RUNOFF (TOTAL) FLUXES */
            // System.out.println("qdh = " + qdh);
            // System.out.println("qds = " + qds);
            qd = qdh + qds; // m3phr

            if (Double.isNaN(qs) || Double.isNaN(qd)) {
                if (Double.isNaN(qs)) {
                    throw new ModelsIllegalargumentException(
                            "Subsuperficial discharge for the hillslope "
                                    + currentHillslope.getHillslopeId() + " " + i + " is NaN", this
                                    .getClass().getSimpleName());
                } else {
                    throw new ModelsIllegalargumentException("Timestep " + currentTimeInMinutes
                            + "Superficial discharge for the hillslope "
                            + currentHillslope.getHillslopeId() + " " + i + " is NaN"
                            + "\nValue of qdh " + qdh + "\nValue of qds " + qds
                            + "\nPrecipitation " + prec_mphr + "\nSatsurf " + satsurf, this
                            .getClass().getSimpleName());
                }
            }

            if (isAtFinalSubtimestep) {
                pm.message("timeinmin = " + currentTimeInMinutes + "\tbacino: " + i + "\tqdh = "
                        + qdh + "\tqds = " + qds + "\tre = " + re + "\tqs = " + qs + "\tmst = "
                        + mst + "\tinf = " + inf + "\tqe1 = " + qe1 + "\tqe2 = " + qe2);
            }
            /*
             * if the area is > 0.1 km2, we consider the delay effect
             * of the hillslope.
             */
            if (area_m2 > THRESHOLD_AREA) {
                // distribute the discharge
                int hillslopeId = currentHillslope.getHillslopeId();
                DischargeDistributor dischargeDistributor = hillslopeId2DischargeDistributor
                        .get(hillslopeId);
                qs = dischargeDistributor.calculateSubsuperficialDischarge(qs, satsurf,
                        currentTimeInMillis);
                qd = dischargeDistributor.calculateSuperficialDischarge(qd, satsurf,
                        currentTimeInMillis);
            }

            /* LINK FLUX ( Q ) */
            /*
             * Below, i=link#, j=id of connecting links, Array[i][j]=link# for
             * connecting link
             */
            /* LINK FLUX ( Q SUBSURFACE, BASE FLOW ) */
            /*
             * Below, i=link#, j=id of connecting links, Array[i][j]=link# for
             * connecting link
             */
            Q_trib = 0.0D;
            Qs_trib = 0.0D;

            List<HillSlope> connectedUpstreamHillSlopes = currentHillslope
                    .getConnectedUpstreamElements();

            if (connectedUpstreamHillSlopes != null) {
                for( HillSlope hillSlope : connectedUpstreamHillSlopes ) {
                    PfafstetterNumber pNum = hillSlope.getPfafstetterNumber();
                    int index = orderedHillslopes.indexOf(hillSlope);
                    boolean doCalculate = true;
                    for( DischargeContributor dContributor : dischargeContributorList ) {
                        Double contributedDischarge = dContributor.getDischarge(pNum.toString(),
                                input[index]);
                        if (!isNovalue(contributedDischarge)) {
                            if (doLog && doPrint) {
                                pm.message("----> For hillslope "
                                        + currentHillslope.getPfafstetterNumber()
                                        + " using hydrometer/dams data in pfafstetter: "
                                        + pNum.toString() + "(meaning added "
                                        + contributedDischarge + " instead of " + input[index]
                                        + ")");
                            }
                            double dischargeRatio = 0.3;// input[index] / (input[index] +
                            // input[index + linksNum]);
                            Q_trib = dischargeRatio * contributedDischarge; // units m^3/s
                            Qs_trib = contributedDischarge - Q_trib; // units m^3/s
                            doCalculate = false;
                        }
                    }
                    if (doCalculate) {
                        // at the same position we can query the input array
                        Q_trib += input[index]; // units m^3/s
                        Qs_trib += input[index + linksNum]; // units m^3/s
                    }
                }

            }

            double linkWidth = currentHillslope.getLinkWidth(8.66, 0.6, 0.0);
            double linkLength = currentHillslope.getLinkLength();
            double linkSlope = currentHillslope.getLinkSlope();
            double chezLawExpon = -1. / 3.;
            double chezLawCoeff = 200. / Math.pow(0.000357911, chezLawExpon);
            double linkChezy = currentHillslope.getLinkChezi(chezLawCoeff, chezLawExpon);

            /* ROUTING RATE (K_Q) and CHANNEL VELOCITY (vc) */
            // System.out.println(routingtype);
            switch( routingType ) {
            case 2: /* No Chezi explicitly */
                K_Q = 8.796 * Math.pow(input[i], 1 / 3.) * Math.pow(linkWidth, -1 / 3.)
                        * Math.pow(linkLength, -1) * Math.pow(linkSlope, 2 / 9.); // units
                // 1/s*/
                break;

            case 3: /* Chezi explicit */
                // System.out.println("Chezy");
                K_Q = 3 / 2. * Math.pow(input[i], 1. / 3.) * Math.pow(linkChezy, 2. / 3.)
                        * Math.pow(linkWidth, -1. / 3.) * Math.pow(linkLength, -1)
                        * Math.pow(linkSlope, 1. / 3.); // units 1/s
                break;

            case 4: /* Mannings equation */
                flowdepth = (1. / 3.) * Math.pow(input[i], 1. / 3.); // depth
                // m,
                // input m^3/s;
                // general
                // observed
                // relation for
                // gc from
                // molnar and
                // ramirez 1998
                hydrad = (flowdepth * linkWidth) / (2.f * flowdepth + linkWidth); // m
                mannings_n = 1; // 0.030f; // mannings n suggested by Jason via his
                // observations at
                // Whitewater for high flows. Low flows will have higher
                // n ... up to 2x more.
                K_Q = (Math.pow(hydrad, 2. / 3.) * Math.pow(linkSlope, 1 / 2.) / mannings_n) // m/s
                        // ;
                        // this
                        // term
                        // is v
                        // from
                        // mannings
                        // eqn
                        * Math.pow(linkLength, -1); // 1/s
                break;

            }

            /*
             * if (i == 62) { System.out.println(" WD ratio ="+
             * linksHydraulicInfo.Width(i)/flowdepth); System.out.println("
             * Mannings v (m/s) =" +
             * (Math.pow(hydrad,2./3.)*Math.pow(linksHydraulicInfo.Slope(i),1/2.)/mannings_n) );
             * System.out.println(" K_Q =" +
             * (Math.pow(hydrad,2./3.)*Math.pow(linksHydraulicInfo.Slope(i),1/2.)/mannings_n)
             * *Math.pow(linksHydraulicInfo.Length(i),-1) ); }
             */
            if (input[i] == 0.0D)
                K_Q = 1e-10;

            if (Double.isNaN(qs) || Double.isNaN(qd)) {
                pm
                        .errorMessage("Problems in basin: " + currentHillslope.getHillslopeId() + " " + i); //$NON-NLS-1$ //$NON-NLS-2$
                if (area_m2 < THRESHOLD_AREA) {
                    qd = 0.0;
                    qs = 0.0;
                    inf = 0.0;
                    qe1 = 0.0;
                    qe2 = 0.0;
                    re = 0.0;
                    System.out.println("All the contributes are setted to zero.");
                }
            }

            /* OUTPUT */
            if (area_m2 > THRESHOLD_AREA) {
                // LINK dQ/dt; big () term is m^3/s, 60*K_Q is 1/min
                output[i] = 60.0D * K_Q * ((1.0D / 3600.) * qd + Q_trib - input[i]);
                // 60.0 * K_Q * (Q_trib - input[i]) + (1.0 / 3600.0) * qd / deltaTinMinutes;
                // LINK dQs/dt -> (m^3/s)/min
                output[i + linksNum] = 60.0 * K_Q * (Qs_trib - input[i + linksNum]) + 60.0 * K_Q
                        * (1.0 / 3600.) * (qs);
                // HILLSLOPE dS1/dt -> m3/min
                output[i + (2 * linksNum)] = (1.0 / 60.0) * (inf - re - qe1);
                // HILLSLOPE dS2/dt -> m3/min
                output[i + (3 * linksNum)] = (1.0 / 60.0) * (re - qs - qe2);
            } else {
                output[i] = 60.0D * K_Q * ((1.0D / 3600.) * qd + Q_trib - input[i]);
                output[i + linksNum] = 60.0D * K_Q
                        * ((1.0D / 3600.) * (qs) + Qs_trib - input[i + linksNum]);
                output[i + (2 * linksNum)] = (1.0D / 60.0) * (inf - re - qe1);
                if (output[i + (2 * linksNum)] != output[i + (2 * linksNum)]
                        || output[i + (2 * linksNum)] == 0.0) {
                    throw new ModelsIllegalargumentException(
                            "Invalid value of S1, please check the parameters."
                                    + output[i + (2 * linksNum)], this.getClass().getSimpleName());
                }
                output[i + (3 * linksNum)] = (1.0D / 60.0) * (re - qs - qe2);
            }
            if (output[i + (3 * linksNum)] != output[i + (3 * linksNum)]
                    || output[i + (2 * linksNum)] == 0.) {
                throw new ModelsIllegalargumentException(
                        "Invalid value of S2, please check the parameters.", this.getClass()
                                .getSimpleName());
            }

        }
        doPrint = false;

        return output;
    }
    public void addDischargeContributor( DischargeContributor dischargeContributor ) {
        dischargeContributorList.add(dischargeContributor);
    }

    public void addDischargeDistributor(
            HashMap<Integer, DischargeDistributor> hillslopeId2DischargeDistributor ) {
        this.hillslopeId2DischargeDistributor = hillslopeId2DischargeDistributor;
    }
}
