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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.duffy;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.hortonmachine.gears.io.adige.AdigeBoundaryCondition;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.IAdigeEngine;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.OmsAdige;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.HillSlopeDuffy;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IDischargeContributor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope;
import org.joda.time.DateTime;

/**
 * The Duffy engine for the {@link OmsAdige} framework.
 *  
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class DuffyAdigeEngine implements IAdigeEngine {

    private DuffyModel duffyEvaluator;
    private RungeKuttaFelberg rainRunoffRaining;
    private final DuffyInputs inDuffyInput;
    private final HashMap<Integer, Integer> index2Basinid;
    private final HashMap<String, Integer> pfaff2Index;
    private final List<String> pfaffsList;
    private int hillsSlopeNum;
    private final HashMap<Integer, double[]> outDischarge;
    private final HashMap<Integer, double[]> outSubDischarge;
    private final List<IHillSlope> orderedHillslopes;
    private final DateTime startTimestamp;
    private final int tTimestep;
    private final DateTime endTimestamp;

    /**
     * Create the Duffy engine.
     * 
     * @param orderedHillslopes
     * @param inDuffyInput
     * @param pm
     * @param doLog
     * @param initialConditions
     * @param basinid2Index
     * @param index2Basinid
     * @param pfaffsList
     * @param pfaff2Index
     * @param outDischarge the {@link Map} in which the model will fill in the 
     *              superficial discharge in each basin for every timestep. Note that 
     *              values will be overwritten.
     * @param outSubDischarge the {@link Map} in which the model will fill in the 
     *              subsuperficial discharge in each basin for every timestep. Note that 
     *              values will be overwritten.
     * @param tTimestep 
     * @param endTimestamp 
     * @param startTimestamp 
     */
    public DuffyAdigeEngine( List<IHillSlope> orderedHillslopes, DuffyInputs inDuffyInput, IHMProgressMonitor pm, boolean doLog,
            double[] initialConditions, HashMap<Integer, Integer> basinid2Index, HashMap<Integer, Integer> index2Basinid,
            List<String> pfaffsList, HashMap<String, Integer> pfaff2Index, HashMap<Integer, double[]> outDischarge,
            HashMap<Integer, double[]> outSubDischarge, DateTime startTimestamp, DateTime endTimestamp, int tTimestep ) {
        this.orderedHillslopes = orderedHillslopes;
        this.inDuffyInput = inDuffyInput;
        this.index2Basinid = index2Basinid;
        this.pfaffsList = pfaffsList;
        this.pfaff2Index = pfaff2Index;
        this.outDischarge = outDischarge;
        this.outSubDischarge = outSubDischarge;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.tTimestep = tTimestep;

        inDuffyInput.outS1 = new HashMap<Integer, double[]>();
        inDuffyInput.outS2 = new HashMap<Integer, double[]>();

        duffyEvaluator = new DuffyModel(orderedHillslopes, inDuffyInput.pRouting, pm, doLog);

        hillsSlopeNum = orderedHillslopes.size();

        createDistributors();

        /*
         * read the initial conditions. 
         */
        if (inDuffyInput.inInitialconditions != null) {
            Set<Entry<Integer, AdigeBoundaryCondition>> entries = inDuffyInput.inInitialconditions.entrySet();
            for( Entry<Integer, AdigeBoundaryCondition> entry : entries ) {
                Integer hillslopeId = entry.getKey();
                Integer index = basinid2Index.get(hillslopeId);
                if (index == null)
                    continue;
                AdigeBoundaryCondition condition = entry.getValue();
                initialConditions[index] = condition.getDischarge();
                initialConditions[index + hillsSlopeNum] = condition.getDischargeSub();
                initialConditions[index + 2 * hillsSlopeNum] = condition.getS1();
                initialConditions[index + 3 * hillsSlopeNum] = condition.getS2();
            }
        } else {
            double startSubsuperficialDischargeFraction = 1.0 - inDuffyInput.pStartSuperficialDischargeFraction;
            for( int i = 0; i < orderedHillslopes.size(); i++ ) {
                HillSlopeDuffy currentHillslope = (HillSlopeDuffy) orderedHillslopes.get(i);
                // initialize with a default discharge per unit of drainage area in km2
                double hillslopeTotalDischarge = currentHillslope.getUpstreamArea(null) / 1000000.0
                        * inDuffyInput.pDischargePerUnitArea;
                initialConditions[i] = inDuffyInput.pStartSuperficialDischargeFraction * hillslopeTotalDischarge;
                // initial subsuperficial flow is setted at a percentage of the total
                // discharge
                initialConditions[i + hillsSlopeNum] = startSubsuperficialDischargeFraction * hillslopeTotalDischarge;
                // initial water content in the saturated hillslope volume is set to
                // have:
                // saturation surface at the 10% of the total area
                double maxSaturatedVolume = currentHillslope.getParameters().getS2max();
                // initial water content in the non saturated hillslope volume is set to
                initialConditions[i + 2 * hillsSlopeNum] = inDuffyInput.pMaxSatVolumeS1 * maxSaturatedVolume;
                initialConditions[i + 3 * hillsSlopeNum] = inDuffyInput.pMaxSatVolumeS2 * maxSaturatedVolume;
            }
        }

        // print of the initial conditions values, just for check
        if (doLog) {
            pm.message("bacino\tQ\tQs\tS1\tS2");
            for( int i = 0; i < hillsSlopeNum; i++ ) {
                int currentBasinId = index2Basinid.get(i);
                pm.message(currentBasinId + "\t" + initialConditions[i] + "\t" + initialConditions[i + hillsSlopeNum] + "\t"
                        + initialConditions[i + 2 * hillsSlopeNum] + "\t" + initialConditions[i + 3 * hillsSlopeNum]);
            }
        }

        rainRunoffRaining = new RungeKuttaFelberg(duffyEvaluator, 1e-2, 10 / 60., pm, doLog);

    }

    public void addDischargeContributor( IDischargeContributor dischargeContributor ) {
        duffyEvaluator.addDischargeContributor(dischargeContributor);
    }

    public void addDischargeDistributor( HashMap<Integer, ADischargeDistributor> hillslopeId2DischargeDistributor ) {
        duffyEvaluator.addDischargeDistributor(hillslopeId2DischargeDistributor);
    }

    public double[] solve( DateTime currentTimstamp, int modelTimestepInMinutes, double internalTimestepInMinutes,
            double[] previousSolution, double[] rainArray, double[] etpArray ) throws IOException {
        rainRunoffRaining.solve(currentTimstamp, modelTimestepInMinutes, internalTimestepInMinutes, previousSolution, rainArray,
                etpArray);
        double[] finalCond = rainRunoffRaining.getFinalCond();

        if (inDuffyInput.doBoundary)
            inDuffyInput.outFinalconditions = new HashMap<Integer, AdigeBoundaryCondition>();

        Set<Entry<String, Integer>> entrySet = pfaff2Index.entrySet();
        for( Entry<String, Integer> entry : entrySet ) {
            String pfaf = entry.getKey();
            Integer index = entry.getValue();
            Integer basinId = index2Basinid.get(index);
            double[] discharge = {finalCond[index]};
            double[] subdischarge = {finalCond[index + hillsSlopeNum]};
            double[] s1 = {finalCond[index + 2 * hillsSlopeNum]};
            double[] s2 = {finalCond[index + 3 * hillsSlopeNum]};

            if (pfaffsList.contains(pfaf)) {
                outDischarge.put(basinId, discharge);
                outSubDischarge.put(basinId, subdischarge);
                inDuffyInput.outS1.put(basinId, s1);
                inDuffyInput.outS2.put(basinId, s2);
            }
            if (inDuffyInput.doBoundary) {
                AdigeBoundaryCondition bc = new AdigeBoundaryCondition();
                bc.setBasinId(basinId);
                bc.setDischarge(discharge[0]);
                bc.setDischargeSub(subdischarge[0]);
                bc.setS1(s1[0]);
                bc.setS2(s2[0]);
                inDuffyInput.outFinalconditions.put(basinId, bc);
            }
        }

        return finalCond;
    }

    public HashMap<Integer, double[]> getDischarge() {
        return outDischarge;
    }

    public HashMap<Integer, double[]> getSubDischarge() {
        return outSubDischarge;
    }

    private void createDistributors() {
        HashMap<Integer, ADischargeDistributor> hillslopeId2DischargeDistributor = new HashMap<Integer, ADischargeDistributor>();
        for( IHillSlope hillSlope : orderedHillslopes ) {
            int hillslopeId = hillSlope.getHillslopeId();
            HashMap<Integer, Double> params = fillParameters(hillSlope);
            System.out.println("Bacino: " + hillslopeId);
            hillslopeId2DischargeDistributor.put(
                    hillslopeId,
                    ADischargeDistributor.createDischargeDistributor(ADischargeDistributor.DISTRIBUTOR_TYPE_NASH,
                            startTimestamp.getMillis(), endTimestamp.getMillis(), (long) tTimestep * 60L * 1000L, params));
        }
        addDischargeDistributor(hillslopeId2DischargeDistributor);
    }

    private HashMap<Integer, Double> fillParameters( IHillSlope hillSlope ) {
        HashMap<Integer, Double> params = new HashMap<Integer, Double>();
        // Double attribute = (Double)
        // hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_10);
        Double attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fAvg_sup_10)).doubleValue();

        params.put(ADischargeDistributor.PARAMS_AVG_SUP_10, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_30);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fAvg_sup_30)).doubleValue();
        params.put(ADischargeDistributor.PARAMS_AVG_SUP_30, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_60);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fAvg_sup_60)).doubleValue();
        params.put(ADischargeDistributor.PARAMS_AVG_SUP_60, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_10);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fVar_sup_10)).doubleValue();
        params.put(ADischargeDistributor.PARAMS_VAR_SUP_10, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_30);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fVar_sup_30)).doubleValue();
        params.put(ADischargeDistributor.PARAMS_VAR_SUP_30, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_60);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fVar_sup_60)).doubleValue();
        params.put(ADischargeDistributor.PARAMS_VAR_SUP_60, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUB);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fAvg_sub)).doubleValue();
        params.put(ADischargeDistributor.PARAMS_AVG_SUB, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUB);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(inDuffyInput.fVar_sub)).doubleValue();
        params.put(ADischargeDistributor.PARAMS_VAR_SUB, attribute);
        params.put(ADischargeDistributor.PARAMS_V_SUP, inDuffyInput.pV_sup);
        params.put(ADischargeDistributor.PARAMS_V_SUB, inDuffyInput.pV_sub);
        return params;
    }
}
