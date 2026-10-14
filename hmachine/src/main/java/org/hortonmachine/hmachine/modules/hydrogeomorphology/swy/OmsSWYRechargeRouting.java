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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.swy;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description(OmsSWYRechargeRouting.DESCRIPTION)
@Documentation(OmsSWYRechargeRouting.DOCUMENTATION)
@Bibliography({OmsSWYQuickflow.BIBLIOGRAPHY_INVEST_SWY, OmsSWYQuickflow.BIBLIOGRAPHY_HAMEL_ET_AL_2020})
@Author(name = OmsSWYRechargeRouting.AUTHORNAMES, contact = OmsSWYRechargeRouting.AUTHORCONTACTS)
@Keywords(OmsSWYRechargeRouting.KEYWORDS)
@Label(OmsSWYRechargeRouting.LABEL)
@Name(OmsSWYRechargeRouting.NAME)
@Status(OmsSWYRechargeRouting.STATUS)
@License(OmsSWYRechargeRouting.LICENSE)
public class OmsSWYRechargeRouting extends HMModel {
    @Description(inPet_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inPet = null;

    @Description(inPrecipitation_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inPrecipitation;

    @Description(inFlowdirections_DESCRIPTION)
    @In
    public GridCoverage2D inFlowdirections = null;

    /**
     * @deprecated not used, stream cells follow the same water balance as the other cells.
     */
    @Deprecated
    @Description(inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(inQuickflow_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inQuickflow = null;

    @Description(pAlpha_DESCRIPTION)
    @In
    public double pAlpha = 1.0;

    @Description(pBeta_DESCRIPTION)
    @In
    public double pBeta = 1.0;

    @Description(pGamma_DESCRIPTION)
    @In
    public double pGamma = 1.0;

    @Description(inPetList_DESCRIPTION)
    @Unit("mm")
    @In
    public List<GridCoverage2D> inPetList = null;

    @Description(inPrecipitationList_DESCRIPTION)
    @Unit("mm")
    @In
    public List<GridCoverage2D> inPrecipitationList = null;

    @Description(inQuickflowList_DESCRIPTION)
    @Unit("mm")
    @In
    public List<GridCoverage2D> inQuickflowList = null;

    @Description(pAlphaList_DESCRIPTION)
    @In
    public double[] pAlphaList = null;

    @Description(outAet_DESCRIPTION)
    @Unit("mm")
    @Out
    public GridCoverage2D outAet = null;

    @Description(outRecharge_DESCRIPTION)
    @Unit("mm")
    @Out
    /**
     * Local recharge per cell: what’s left from rainfall after AET and quickflow (R = P - AET - QF).
     *
     * INVEST's L, i.e. L in cell i and month m
     */
    public GridCoverage2D outRecharge = null;

    @Description(outUpslopeSubsidy_DESCRIPTION)
    @Unit("mm")
    @Out
    /**
	 * Recharge available from the upstream cells that drain into this cell.
	 *
	 * INVEST's L_sum_avail, i.e. L_sum_avail in cell i and month m
	 */
    public GridCoverage2D outUpslopeSubsidy = null;

    @Description(outAvailableRecharge_DESCRIPTION)
    @Unit("mm")
    @Out
    /**
	 * Part of the local recharge that is available to downslope cells, i.e. L_avail = min(gamma * L, L).
	 *
	 * INVEST's L_avail, i.e. L_avail in cell i and month m
	 */
    public GridCoverage2D outAvailableRecharge = null;


    // VARS DOC START
    public static final String DESCRIPTION = "The local recharge and actual evapotranspiration of the InVEST Seasonal Water Yield model, routing the recharge available to the downslope cells along the flowdirections.";
    public static final String DOCUMENTATION = """
            Each cell evapotranspires its precipitation minus quickflow, plus a part of the recharge of the upslope \
            cells, up to the potential evapotranspiration. What is left is the local recharge, negative where the \
            upslope water is evapotranspired, as on the streams, where all the precipitation is quickflow. The \
            fraction gamma of the recharge is available to the downslope cells, of which the fraction beta can be \
            evapotranspired, divided among the intervals by alpha.

            The module has two modes. With the lists (or folders) of the intervals, the recharge is the one of the \
            whole period, as in InVEST, where the 12 months of a year make an annual recharge, and alpha is by \
            default 1/12. With the single interval inputs, the recharge of the interval is routed by itself, \
            without water carried over from the other intervals: the results then differ from those of InVEST, \
            even if summed over the year.

            Novalues in the precipitation, quickflow and potential evapotranspiration are considered as 0.
            """;
    public static final String KEYWORDS = "recharge";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "SWYRechargeRouting";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String inFlowdirections_DESCRIPTION = "The map of flowdirections (D8).";
    public static final String inNet_DESCRIPTION = "DEPRECATED, not needed and ignored: as in InVEST, stream cells follow the same water balance as the other cells.";
    public static final String inQuickflow_DESCRIPTION = "The map of the quickflow of the interval (single interval mode).";
    public static final String outAvailableRecharge_DESCRIPTION = "The map of the part of the local recharge that is available to downslope cells (L_avail = min(gamma * L, L)).";
    public static final String outRecharge_DESCRIPTION = "The map of the local recharge (L), what is left of the precipitation after quickflow and actual evapotranspiration.";
    public static final String outAet_DESCRIPTION = "The map of the actual evapotranspiration.";
    public static final String outUpslopeSubsidy_DESCRIPTION = "The map of the upslope subsidy (the recharge available from upstream cells, L_sum_avail).";
    public static final String inPrecipitation_DESCRIPTION = "The map of the precipitation of the interval (single interval mode).";
    public static final String inPet_DESCRIPTION = "The map of the potential evapotranspiration of the interval (single interval mode).";
    public static final String pAlpha_DESCRIPTION = "The fraction of the upslope subsidy that is available in the interval (single interval mode).";
    public static final String pBeta_DESCRIPTION = "Spatial availability parameter: the fraction of the upgradient subsidy that is available for downgradient evapotranspiration, it is based on local topography and geology";
    public static final String pGamma_DESCRIPTION = "Fraction of pixel recharge that is available to downgradient pixels, represents what extent local recharge enters a local groundwater system and might be used again as oppose to entering a deeper groundwater system";
    public static final String inPetList_DESCRIPTION = "The potential evapotranspiration of each interval (ex. the 12 months of a year). If set, the recharge is calculated over the whole period, as in InVEST, and the single interval inputs are not used.";
    public static final String inPrecipitationList_DESCRIPTION = "The precipitation of each interval (ex. the 12 months of a year), used together with the pet list.";
    public static final String inQuickflowList_DESCRIPTION = "The quickflow of each interval (ex. the 12 months of a year), used together with the pet list.";
    public static final String pAlphaList_DESCRIPTION = "The fraction of the upslope subsidy of the whole period that is available in each interval, used together with the pet list (if not set, 1/number of intervals, i.e. 1/12 for months as in InVEST).";
    public static final String inPetFolder_DESCRIPTION = "The folder with the potential evapotranspiration of each interval (ex. the 12 months of a year), named as the pet prefix followed by the number of the interval. If set, the recharge is calculated over the whole period, as in InVEST, and the single interval inputs are not used.";
    public static final String pPetPrefix_DESCRIPTION = "The name prefix of the potential evapotranspiration rasters (ex. pet_ for pet_1.tif ... pet_12.tif).";
    public static final String inPrecipitationFolder_DESCRIPTION = "The folder with the precipitation of each interval, used together with the pet folder.";
    public static final String pPrecipitationPrefix_DESCRIPTION = "The name prefix of the precipitation rasters (ex. prcp_ for prcp_1.tif ... prcp_12.tif).";
    public static final String inQuickflowFolder_DESCRIPTION = "The folder with the quickflow of each interval, used together with the pet folder.";
    public static final String pQuickflowPrefix_DESCRIPTION = "The name prefix of the quickflow rasters (ex. qf_ for qf_1.tif ... qf_12.tif).";
    public static final String pAlphas_DESCRIPTION = "The comma separated fractions of the upslope subsidy available in each interval, in the order of the interval numbers, used together with the pet folder (if not set, 1/number of intervals, i.e. 1/12 for months as in InVEST).";

    public static final String pRainfall_UNIT = "mm";
    // VARS DOC END

    // one entry per interval: the precipitation minus the quickflow, and the pet, in memory
    private HMRaster[] waterRasters;
    private HMRaster[] petRasters;
    private double[] alphas;

    @Execute
    public void process() throws Exception {
        /*
         * Implements recharge and evapotranspiration routing from the InVEST Seasonal Water Yield (SWY) model.
         * Computes actual evapotranspiration (AET), local recharge, routed available recharge, and cumulative upslope subsidy."
         */
        checkNull(inFlowdirections);
        if (inNet != null) {
            pm.message("WARNING: the network map (inNet) is deprecated and ignored, it is not needed for the recharge routing.");
        }

        List<GridCoverage2D> petList;
        List<GridCoverage2D> rainList;
        List<GridCoverage2D> quickflowList;
        if (inPetList != null) {
            // whole period mode, as in InVEST
            checkNull(inPrecipitationList, inQuickflowList);
            petList = inPetList;
            rainList = inPrecipitationList;
            quickflowList = inQuickflowList;
            int intervals = petList.size();
            if (intervals == 0 || rainList.size() != intervals || quickflowList.size() != intervals) {
                throw new ModelsIllegalargumentException(
                        "The pet, precipitation and quickflow lists need to have the same, non zero, number of rasters.", this,
                        pm);
            }
            if (pAlphaList != null) {
                if (pAlphaList.length != intervals) {
                    throw new ModelsIllegalargumentException("The alpha list needs to have one value per interval.", this, pm);
                }
                alphas = pAlphaList;
            } else {
                alphas = new double[intervals];
                Arrays.fill(alphas, 1.0 / intervals);
            }
        } else {
            // single interval mode
            checkNull(inPet, inPrecipitation, inQuickflow);
            petList = List.of(inPet);
            rainList = List.of(inPrecipitation);
            quickflowList = List.of(inQuickflow);
            alphas = new double[]{pAlpha};
        }

        RegionMap regionMap = RegionMap.fromGridGeometry(inFlowdirections.getGridGeometry());
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        double outNv = HMConstants.doubleNovalue;

        WritableRaster outL_WR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outL_Iter = CoverageUtilities.getWritableRandomIterator(outL_WR);
        WritableRaster outL_Available_WR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outL_Available_Iter = CoverageUtilities.getWritableRandomIterator(outL_Available_WR);
        WritableRaster outAetWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outAetIter = CoverageUtilities.getWritableRandomIterator(outAetWR);

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlowdirections);
        int flowNv = HMConstants.getIntNovalue(inFlowdirections);
        /*
         * the inputs are read once, row by row, into rasters in memory: the routing visits the cells
         * along the flow paths, and the random access to many rasters read from file would decode
         * their tiles again and again
         */
        int intervals = alphas.length;
        waterRasters = new HMRaster[intervals];
        petRasters = new HMRaster[intervals];
        for( int i = 0; i < intervals; i++ ) {
            readInterval(i, rainList.get(i), quickflowList.get(i), petList.get(i));
        }

        List<FlowNode> sourceCells = new ArrayList<>();
        try {
            pm.beginTask("Collect source cells...", rows);
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {
                    FlowNode node = new FlowNode(flowIter, cols, rows, c, r, flowNv);
                    if (node.isSource()) {
                        sourceCells.add(node);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            // NaN marks the cells that have not been processed yet
            double[][] lSum_Matrix = new double[rows][cols];
            for( int r = 0; r < lSum_Matrix.length; r++ ) {
                for( int c = 0; c < lSum_Matrix[0].length; c++ ) {
                    lSum_Matrix[r][c] = Double.NaN;
                }
            }

            pm.beginTask("Routing recharge...", sourceCells.size());
            for( FlowNode sourceCell : sourceCells ) {
                if (pm.isCanceled()) {
                    return;
                }
                // source cells have no upslope subsidy
                lSum_Matrix[sourceCell.row][sourceCell.col] = 0;
                processCell(sourceCell, 0, outL_Iter, outL_Available_Iter, outAetIter);

                // go downstream
                FlowNode cell = sourceCell.goDownstream();

                Set<FlowNode> seen = new HashSet<>();

                while( cell != null ) {
                    List<FlowNode> upstreamCells = cell.getEnteringNodes();
                    // check if all upstream have a value
                    if (!canProcess(lSum_Matrix, upstreamCells)) {
                        break;
                    }

                    double lAvailableUpstream = 0.0;
                    double lSumAvailableUpstream = 0.0;
                    for( FlowNode upstreamCell : upstreamCells ) {
                        lSumAvailableUpstream += lSum_Matrix[upstreamCell.row][upstreamCell.col];
                        lAvailableUpstream += upstreamCell.getValueFromMap(outL_Available_Iter);
                    }
                    // as in InVEST, with D8 the sum is normalized by the number of upslope cells
                    double lSumAvailableCurrentCell = (lSumAvailableUpstream + lAvailableUpstream) / upstreamCells.size();
                    lSum_Matrix[cell.row][cell.col] = lSumAvailableCurrentCell;
                    processCell(cell, lSumAvailableCurrentCell, outL_Iter, outL_Available_Iter, outAetIter);

                    cell = cell.goDownstream();

                    if (cell != null) {
                        if (seen.contains(cell)) {
                            cell = null;
                        } else {
                            seen.add(cell);
                        }
                    }
                }
                pm.worked(1);
            }
            pm.done();

            for( int r = 0; r < lSum_Matrix.length; r++ ) {
                for( int c = 0; c < lSum_Matrix[0].length; c++ ) {
                    if (Double.isNaN(lSum_Matrix[r][c])) {
                        lSum_Matrix[r][c] = outNv;
                    }
                }
            }

            outRecharge = CoverageUtilities.buildCoverageWithNovalue("recharge", outL_WR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outAvailableRecharge = CoverageUtilities.buildCoverageWithNovalue("availablerecharge", outL_Available_WR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outAet = CoverageUtilities.buildCoverageWithNovalue("aet", outAetWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outUpslopeSubsidy = CoverageUtilities.buildCoverageWithNovalue("lsum", lSum_Matrix, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), true, outNv);

        } finally {
            flowIter.done();
            for( int i = 0; i < intervals; i++ ) {
                if (waterRasters[i] != null) {
                    waterRasters[i].close();
                }
                if (petRasters[i] != null) {
                    petRasters[i].close();
                }
            }
            waterRasters = null;
            petRasters = null;

            outL_Iter.done();
            outL_Available_Iter.done();
            outAetIter.done();

        }

    }

    /**
     * Calculate aet, local recharge and available recharge of a cell (InVEST equations 4-8).
     *
     * <p>The aet is summed over the intervals, each of which gets its alpha fraction
     * of the upslope subsidy.</p>
     *
     * <p>As in InVEST, novalues in precipitation, quickflow and pet are considered as 0
     * and stream cells follow the same water balance as the other cells. Since on streams
     * the quickflow equals the precipitation, there the recharge is negative: streams
     * evapotranspire the upslope subsidy.</p>
     *
     * @param cell the cell to process.
     * @param lSumAvailable the upslope subsidy of the cell.
     */
    private void processCell( FlowNode cell, double lSumAvailable, WritableRandomIter outL_Iter,
            WritableRandomIter outL_Available_Iter, WritableRandomIter outAetIter ) {
        double waterSum = 0;
        double aet = 0;
        for( int i = 0; i < alphas.length; i++ ) {
            double water = waterRasters[i].getValue(cell.col, cell.row);
            waterSum += water;
            aet += Math.min(petRasters[i].getValue(cell.col, cell.row), water + alphas[i] * pBeta * lSumAvailable);
        }
        double l = waterSum - aet;
        double lAvailable = Math.min(pGamma * l, l);

        cell.setDoubleValueInMap(outL_Available_Iter, lAvailable);
        cell.setDoubleValueInMap(outL_Iter, l);
        cell.setDoubleValueInMap(outAetIter, aet);
    }

    /**
     * Read the precipitation minus the quickflow and the potential evapotranspiration of an interval
     * into rasters in memory.
     *
     * <p>As in InVEST, novalues are considered as 0.</p>
     */
    private void readInterval( int interval, GridCoverage2D rain, GridCoverage2D quickflow, GridCoverage2D pet )
            throws Exception {
        try (HMRaster rainRaster = HMRaster.fromGridCoverage(rain);
                HMRaster quickflowRaster = HMRaster.fromGridCoverage(quickflow);
                HMRaster petRaster = HMRaster.fromGridCoverage(pet)) {
            HMRaster water = new HMRaster.HMRasterWritableBuilder().setName("water_" + interval).setTemplate(rainRaster)
                    .build();
            HMRaster et = new HMRaster.HMRasterWritableBuilder().setName("pet_" + interval).setTemplate(rainRaster).build();
            waterRasters[interval] = water;
            petRasters[interval] = et;
            rainRaster.process(pm, "Reading the inputs of interval " + (interval + 1) + "...", ( col, row, value, cols, rows ) -> {
                double p = valueOrZero(value, rainRaster.getNovalue());
                double qf = valueOrZero(quickflowRaster.getValue(col, row), quickflowRaster.getNovalue());
                water.setValue(col, row, p - qf);
                et.setValue(col, row, valueOrZero(petRaster.getValue(col, row), petRaster.getNovalue()));
            });
        }
    }

    /**
     * Collect the rasters of the intervals in a folder: the files named as the prefix followed
     * by the number of the interval, as prcp_1.tif ... prcp_12.tif.
     *
     * @param folder the folder with the rasters.
     * @param prefix the name prefix of the rasters.
     * @return the rasters by interval number, in increasing order.
     * @throws ModelsIllegalargumentException if the folder is not readable or two rasters have the same number.
     */
    public static TreeMap<Integer, File> getIntervalRasters( File folder, String prefix ) {
        File[] files = folder.listFiles();
        if (files == null) {
            throw new ModelsIllegalargumentException("The folder can't be read: " + folder, OmsSWYRechargeRouting.class);
        }
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "(\\d+)\\.[^.]+");
        TreeMap<Integer, File> rasters = new TreeMap<>();
        for( File file : files ) {
            if (!HMConstants.isRaster(file)) {
                continue;
            }
            Matcher matcher = pattern.matcher(file.getName());
            if (matcher.matches()) {
                int interval = Integer.parseInt(matcher.group(1));
                File other = rasters.put(interval, file);
                if (other != null) {
                    throw new ModelsIllegalargumentException("Two rasters for the interval " + interval + ": " + other.getName()
                            + " and " + file.getName(), OmsSWYRechargeRouting.class);
                }
            }
        }
        return rasters;
    }

    private static double valueOrZero( double value, double novalue ) {
        if (HMConstants.isNovalue(value, novalue)) {
            return 0;
        }
        return value;
    }

    private boolean canProcess( double[][] lSumMatrix, List<FlowNode> upstreamCells ) {
        for( FlowNode upstreamCell : upstreamCells ) {
            if (Double.isNaN(lSumMatrix[upstreamCell.row][upstreamCell.col])) {
                // stop, we still need the other upstream values
                return false;
            }
        }
        return true;
    }

}
