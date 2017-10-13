/* JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.hymod;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.IAdigeEngine;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.OmsAdige;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IDischargeContributor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.joda.time.DateTime;

/**
 * The Hymod engine for the {@link OmsAdige} framework.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Giuseppe Formetta
 */
public class HymodAdigeEngine implements IAdigeEngine {

    private final HymodInputs hymodInputs;
    private final List<IHillSlope> orderedHillslopes;
    // private double[] initialConditions;
    private double[] xSlow = null;
    private double[] xLoss = null;
    private double[][] xQuick = null;
    private final HashMap<Integer, double[]> outDischarge;
    private final HashMap<Integer, double[]> outSubDischarge;
    private final HashMap<Integer, double[]> outDischargeInternal;
    private final HashMap<Integer, Integer> index2Basinid;
    private List<IDischargeContributor> dischargeContributorList = new ArrayList<IDischargeContributor>();
    private final boolean doPrint;
    private final boolean doLog;
    private final IHMProgressMonitor pm;
    private final List<String> pfaffsList;
    private double[] coeffs;
    int conta = 0;

    public HymodAdigeEngine( HymodInputs hymodInputs, List<IHillSlope> orderedHillslopes,
            HashMap<Integer, Integer> index2Basinid, HashMap<Integer, double[]> outDischarge,
            HashMap<Integer, double[]> outSubDischarge, List<String> pfaffsList, boolean doLog, boolean doPrint,
            IHMProgressMonitor pm ) {
        this.hymodInputs = hymodInputs;
        this.orderedHillslopes = orderedHillslopes;
        this.index2Basinid = index2Basinid;
        this.outDischarge = outDischarge;
        this.outSubDischarge = outSubDischarge;
        this.pfaffsList = pfaffsList;
        this.doLog = doLog;
        this.doPrint = doPrint;
        this.pm = pm;
        outDischargeInternal = new HashMap<Integer, double[]>();
    }

    public void addDischargeContributor( IDischargeContributor dischargeContributor ) {
        dischargeContributorList.add(dischargeContributor);
    }

    public HashMap<Integer, double[]> getDischarge() {
        return outDischarge;
    }

    public HashMap<Integer, double[]> getSubDischarge() {
        return outSubDischarge;
    }

    public double[] solve( DateTime currentTimstamp, int tTimestep, double internalTimestepInMinutes, double[] initialConditions,
            double[] rainArray, double[] etpArray ) throws IOException {

        if (initialConditions != null) {

            for( int i = orderedHillslopes.size() - 1; i >= 0; i-- ) {
                xLoss[i] = initialConditions[i];
                xSlow[i] = initialConditions[i + orderedHillslopes.size()];
                xQuick[0][i] = initialConditions[i + 2 * orderedHillslopes.size()];
                xQuick[1][i] = initialConditions[i + 3 * orderedHillslopes.size()];
                xQuick[2][i] = initialConditions[i + 4 * orderedHillslopes.size()];

            }
        }

        if (initialConditions == null) {
            xSlow = new double[orderedHillslopes.size()];
            coeffs = new double[orderedHillslopes.size()];
            xQuick = new double[3][orderedHillslopes.size()];
            xLoss = new double[orderedHillslopes.size()];
            initialConditions = new double[orderedHillslopes.size() * 5];
            for( int i = orderedHillslopes.size() - 1; i >= 0; i-- ) {
                IHillSlope hillSlope = orderedHillslopes.get(i);
                double areaKm2 = hillSlope.getHillslopeArea() / 1E6;
                // System.out.println(areaKm2);

                coeffs[i] = (Math.pow(10, 9)) * tTimestep * 60 / (areaKm2 * (Math.pow(10, 12)));
                xSlow[i] = 0 * hymodInputs.pQ0 * coeffs[i] / hymodInputs.pRs;

            }
        }

        for( int i = orderedHillslopes.size() - 1; i >= 0; i-- ) {
            IHillSlope hillSlope = orderedHillslopes.get(i);

            // /////////////FISSATO PER CHECK///////////////
            // hymodInputs.pAlpha=0.323;
            // hymodInputs.pCmax=999.0;
            // hymodInputs.pB=0.515;
            // hymodInputs.pRq=0.135;
            // hymodInputs.pRs=0.0091;
            // /////////////FISSATO PER CHECK///////////////

            double rain = rainArray[i];
            double etp = etpArray[i];
            // funziona
            // if (rain == -999 || etp ==-999) {
            // rain=0;etp=0;
            // }
            // modificato
            // System.out.println("rain= "+rain+" etp= "+etp);
            // if (isNovalue(rain) || isNovalue(etp)) {
            // rain=0;
            // etp=0;
            // }
            //
            //
            // /*
            // * sum together the discharge contributed by the current
            // * hillslope plus the contributions coming from upstream
            // */
            //
            // PfafstetterNumber pfaf = hillSlope.getPfafstetterNumber();
            // if (pfaffsList.contains(pfaf.toString())) {
            // outDischarge.put(basinId, new double[] { -999 });
            // outSubDischarge.put(basinId, new double[] { -999 });
            // }
            //
            // outDischargeInternal.put(basinId,
            // new double[] { -999 });
            // System.out.println(basinId+" rain= "+rain+" etp="+etp+ "outDischargeInternal="+
            // outDischargeInternal.get(basinId));
            // // if (i == 2) {
            // // //
            // // System.out.println(conta+" basinDischarge"+(-999)
            // // +" xloss="+xLoss[i]);
            // // conta++;
            // // }

            // } else {

            double[] out_excess = excess(xLoss[i], rain, etp);
            double UT1 = out_excess[0];
            double UT2 = out_excess[1];
            xLoss[i] = out_excess[2];

            double UQ = hymodInputs.pAlpha * UT2 + UT1;
            double US = (1.0 - hymodInputs.pAlpha) * UT2;

            double inflow = US;
            double[] out_linres1 = linres(xSlow[i], inflow, hymodInputs.pRs, 1);

            xSlow[i] = out_linres1[0];
            double outflow1 = out_linres1[1];
            double QS = outflow1;
            inflow = UQ;
            double outflow2 = 0;

            for( int k = 0; k < 3; k++ ) {
                double[] out_linres2 = linres(xQuick[k][i], inflow, hymodInputs.pRq, 1);
                xQuick[k][i] = out_linres2[0];
                outflow2 = out_linres2[1];
                inflow = outflow2;
            }

            Integer basinId = index2Basinid.get(i);
            double basinDischarge = (QS + outflow2) / coeffs[i];
            double basinSubDischarge = QS / coeffs[i];

            double allContributionsDischarge = handleContributors(hillSlope, basinDischarge);

            /*
             * sum together the discharge contributed by the current
             * hillslope plus the contributions coming from upstream
             */
            basinDischarge = basinDischarge + allContributionsDischarge;

            PfafstetterNumber pfaf = hillSlope.getPfafstetterNumber();
            if (pfaffsList.contains(pfaf.toString())) {
                outDischarge.put(basinId, new double[]{basinDischarge});
                outSubDischarge.put(basinId, new double[]{basinSubDischarge});
            }
            initialConditions[i] = xLoss[i];
            initialConditions[i + orderedHillslopes.size()] = xSlow[i];
            initialConditions[i + 2 * orderedHillslopes.size()] = xQuick[0][i];
            initialConditions[i + 3 * orderedHillslopes.size()] = xQuick[1][i];
            initialConditions[i + 4 * orderedHillslopes.size()] = xQuick[2][i];

            outDischargeInternal.put(basinId, new double[]{basinDischarge});
            // System.out.println(basinId+" rain= "+rain+" etp="+etp+ "outDischargeInternal="+
            // outDischargeInternal.get(basinId));
            // if (i == 61) {
            // System.out.println("rain= "+rain+" etp= "+etp+" basinId= "+basinId+
            // " basinDischarge"+basinDischarge+" allcontributions= "+allContributionsDischarge+" xloss= "+xLoss[i]);
            // }
            // if (i == 61) {
            // //
            // System.out.println(conta+"rain= "+rain+" etp= "+etp+" basinDischarge"+(basinDischarge-allContributionsDischarge)
            // +" xloss="+xLoss[i]);
            // conta++;
            // }
            // }
        }

        // System.out.println("out=" + outDischarge.get(basinId)[0] + " x_slow="
        // + xSlow[0] + "rain=" + rainArray[0] + " etp="
        // + etpArray[0]);
        return initialConditions;
    }

    private double handleContributors( IHillSlope hillSlope, final double basinDischarge ) {
        double summedContributions = 0;

        List<IHillSlope> connectedUpstreamHillSlopes = hillSlope.getConnectedUpstreamElements();
        if (connectedUpstreamHillSlopes != null) {
            for( IHillSlope tmpHillSlope : connectedUpstreamHillSlopes ) {
                PfafstetterNumber pNum = tmpHillSlope.getPfafstetterNumber();
                int hillslopeId = tmpHillSlope.getHillslopeId();

                /*
                 * get the inflow from upstream basins
                 */
                double upstreamDischarge = outDischargeInternal.get(hillslopeId)[0];

                /*
                 * handle the contributors
                 */
                for( IDischargeContributor dContributor : dischargeContributorList ) {
                    Double contributedDischarge = dContributor.getDischarge(pNum.toString());
                    if (!isNovalue(contributedDischarge)) {
                        if (doLog && doPrint) {
                            pm.message("----> For hillslope " + hillSlope.getPfafstetterNumber()
                                    + " using hydrometer/dams data in pfafstetter: " + pNum.toString() + "(meaning added "
                                    + contributedDischarge + " instead of " + upstreamDischarge + ")");
                        }

                        /*
                         * here the contributor will give its contribution,
                         * which depends on the type of contributor. For example
                         * a Hydrometer will completely substitute the
                         * calculated discharge of the current hillslope
                         * (tmpHillSlope) with the measure supplied by the
                         * Hydrometer.
                         */

                        // funziona
                        // if (contributedDischarge != -9999) {
                        // upstreamDischarge = dContributor
                        // .mergeWithDischarge(contributedDischarge,
                        // upstreamDischarge);
                        // }

                        // modificato
                        if (!isNovalue(contributedDischarge)) {
                            upstreamDischarge = dContributor.mergeWithDischarge(contributedDischarge, upstreamDischarge);
                        }

                    }
                }
                double routedDischarge = doRouting(upstreamDischarge, basinDischarge, tmpHillSlope);

                summedContributions = summedContributions + routedDischarge;
            }
        }

        return summedContributions;
    }

    // TODO make this real
    private double doRouting( double discharge, final double basinDischarge, IHillSlope hillslope ) {
        //
        // double K_Q = AdigeUtilities.doRouting(discharge, hillslope, 2);

        return discharge;
    }

    private double[] excess( double x_losss, double Pval, double PETval ) {
        double[] o_exces = new double[3];
        double pB = hymodInputs.pB;
        double pCmax = hymodInputs.pCmax;

        double xn_prev = x_losss;
        double coeff1 = ((1.0 - ((pB + 1.0) * (xn_prev) / pCmax)));
        // if(Math.abs(coeff1)<1E-10){coeff1=0;}
        double exp = 1.0 / (pB + 1.0);
        double ct_prev = pCmax * (1.0 - pow(coeff1, exp));
        double UT1 = max((Pval - pCmax + ct_prev), 0.0);
        Pval = Pval - UT1;
        double dummy = min(((ct_prev + Pval) / pCmax), 1.0);
        double coeff2 = (1.0 - dummy);
        double exp2 = (pB + 1.0);
        double xn = (pCmax / (pB + 1.0)) * (1.0 - (pow(coeff2, exp2)));
        double UT2 = max(Pval - (xn - xn_prev), 0);
        double evap = min(xn, PETval);

        xn = xn - evap;

        o_exces[0] = UT1;
        o_exces[1] = UT2;
        o_exces[2] = xn;
        if (xn != xn || UT1 != UT1 || UT2 != UT2) {
            System.out.println("FERMATI");

        }

        return o_exces;

    }

    private double[] linres( double x_sloww, double infloww, double RR, double dt ) {

        double[] o_linres = new double[2];

        double x_sloww_prev = x_sloww;
        double x_slow_new = (infloww * dt) + x_sloww_prev * (1 - RR * dt);
        double outfloww = x_slow_new * RR;
        o_linres[0] = x_slow_new;
        o_linres[1] = outfloww;
        return o_linres;

    }

}
