/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.hymod;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.Adige;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.IAdigeEngine;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core.IDischargeContributor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core.IHillSlope;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core.PfafstetterNumber;
import org.joda.time.DateTime;

/**
 * The Hymod engine for the {@link Adige} framework.
 *  
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Giuseppe Formetta
 */
public class HymodAdigeEngine implements IAdigeEngine {

    private final HymodInputs hymodInputs;
    private final List<IHillSlope> orderedHillslopes;

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
    private final IJGTProgressMonitor pm;
    private final List<String> pfaffsList;
    private double udo;

    public HymodAdigeEngine( HymodInputs hymodInputs, List<IHillSlope> orderedHillslopes,
            HashMap<Integer, Integer> index2Basinid, HashMap<Integer, double[]> outDischarge,
            HashMap<Integer, double[]> outSubDischarge, List<String> pfaffsList, boolean doLog, boolean doPrint,
            IJGTProgressMonitor pm ) {
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

        double totalArea = orderedHillslopes.get(0).getUpstreamArea(null);
        udo = hymodInputs.pQ0 / (totalArea / 1E6);

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

    public double[] solve( DateTime currentTimstamp, int tTimestep, double internalTimestepInMinutes, double[] previousSolution,
            double[] rainArray, double[] etpArray ) throws IOException {

        if (xSlow == null) {
            xSlow = new double[orderedHillslopes.size()];
            xQuick = new double[3][orderedHillslopes.size()];
            xLoss = new double[orderedHillslopes.size()];
        }

        for( int i = orderedHillslopes.size() - 1; i >= 0; i-- ) {
            IHillSlope hillSlope = orderedHillslopes.get(i);
            double areaKm2 = hillSlope.getHillslopeArea() / 1E6;
            double coeff = (pow(10, 9)) * tTimestep * 60 / (areaKm2 * (pow(10, 12)));
            xSlow[i] = udo * areaKm2 * coeff / hymodInputs.pRs;

            double rain = rainArray[i];
            double etp = etpArray[i];

            double[] out_excess = excess(xLoss[i], rain, etp);
            double UT1 = out_excess[0];
            double UT2 = out_excess[1];
            xLoss[i] = out_excess[2];

            double UQ = hymodInputs.pAlpha * UT2 + UT1;
            double US = (1 - hymodInputs.pAlpha) * UT2;

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
            double basinDischarge = (QS + outflow2) / coeff;
            double basinSubDischarge = QS / coeff;

            basinDischarge = handleContributors(hillSlope, basinDischarge);

            PfafstetterNumber pfaf = hillSlope.getPfafstetterNumber();
            if (pfaffsList.contains(pfaf.toString())) {
                outDischarge.put(basinId, new double[]{basinDischarge});
                outSubDischarge.put(basinId, new double[]{basinSubDischarge});
            }
            outDischargeInternal.put(basinId, new double[]{basinDischarge});
        }

        return null;
    }

    private double handleContributors( IHillSlope hillSlope, double basinDischarge ) {
        List<IHillSlope> connectedUpstreamHillSlopes = hillSlope.getConnectedUpstreamElements();
        if (connectedUpstreamHillSlopes != null) {
            for( IHillSlope tmpHillSlope : connectedUpstreamHillSlopes ) {
                PfafstetterNumber pNum = tmpHillSlope.getPfafstetterNumber();
                int hillslopeId = tmpHillSlope.getHillslopeId();
                /*
                 * contributors
                 */
                for( IDischargeContributor dContributor : dischargeContributorList ) {
                    Double contributedDischarge = dContributor.getDischarge(pNum.toString());
                    if (!isNovalue(contributedDischarge)) {
                        if (doLog && doPrint) {
                            pm.message("----> For hillslope " + hillSlope.getPfafstetterNumber()
                                    + " using hydrometer/dams data in pfafstetter: " + pNum.toString() + "(meaning added "
                                    + contributedDischarge + " instead of " + basinDischarge + ")");
                        }

                        // double routedDischarge = doRouting(contributedDischarge);
                        // basinDischarge = dContributor.mergeWithDischarge(routedDischarge,
                        // basinDischarge);
                        basinDischarge = dContributor.mergeWithDischarge(contributedDischarge, basinDischarge);
                    }
                }

                /*
                 * add inflow from upstream basins
                 */
                double[] upstreamDischarge = outDischargeInternal.get(hillslopeId);
                basinDischarge = basinDischarge + upstreamDischarge[0];
                // double routedDischarge = doRouting(upstreamDischarge[0]);
                // basinDischarge = basinDischarge + routedDischarge;
            }
        }

        return basinDischarge;
    }

    // TODO
    // private double doRouting( double discharge, IHillSlope hillSlope ) {
    // double linkWidth = hillSlope.getLinkWidth(8.66, 0.6, 0.0);
    // double linkLength = hillSlope.getLinkLength();
    // double linkSlope = hillSlope.getLinkSlope();
    // return 0;
    // }

    private double[] excess( double x_losss, double Pval, double PETval ) {
        double[] o_exces = new double[3];
        double pB = hymodInputs.pB;
        double pCmax = hymodInputs.pCmax;

        double xn_prev = x_losss;
        double coeff1 = ((1 - ((pB + 1) * (xn_prev) / pCmax)));
        double exp = 1 / (pB + 1);
        double ct_prev = pCmax * (1 - pow(coeff1, exp));
        double UT1 = max((Pval - pCmax + ct_prev), 0.0);
        Pval = Pval - UT1;
        double dummy = min(((ct_prev + Pval) / pCmax), 1);
        double coeff2 = (1 - dummy);
        double exp2 = (pB + 1);
        double xn = (pCmax / (pB + 1)) * (1 - (pow(coeff2, exp2)));
        double UT2 = max(Pval - (xn - xn_prev), 0);
        double evap = min(xn, PETval);
        xn = xn - evap;
        o_exces[0] = UT1;
        o_exces[1] = UT2;
        o_exces[2] = xn;

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
