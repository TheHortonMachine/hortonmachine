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
package org.hortonmachine.hmachine.modules.geomorphology.curvatures;

import static org.hortonmachine.gears.libs.modules.HMConstants.GEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_DOCUMENTATION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_KEYWORDS;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_LABEL;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_LICENSE;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_NAME;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

import oms3.annotations.Author;
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

@Description(OMSCURVATURES_DESCRIPTION)
@Documentation(OMSCURVATURES_DOCUMENTATION)
@Author(name = OMSCURVATURES_AUTHORNAMES, contact = OMSCURVATURES_AUTHORCONTACTS)
@Keywords(OMSCURVATURES_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name(OMSCURVATURES_NAME)
@Status(OMSCURVATURES_STATUS)
@License(OMSCURVATURES_LICENSE)
public class OmsCurvatures extends GridMultiProcessing {
    @Description(OMSCURVATURES_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    // output
    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @Out
    public GridCoverage2D outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @Out
    public GridCoverage2D outPlan = null;

    @Description(OMSCURVATURES_outTang_DESCRIPTION)
    @Out
    public GridCoverage2D outTang = null;

    public static final String OMSCURVATURES_DESCRIPTION = "It estimates the longitudinal, normal and planar curvatures.";
    public static final String OMSCURVATURES_DOCUMENTATION = "OmsCurvatures.html";
    public static final String OMSCURVATURES_KEYWORDS = "Geomorphology";
    public static final String OMSCURVATURES_LABEL = GEOMORPHOLOGY;
    public static final String OMSCURVATURES_NAME = "curvatures";
    public static final int OMSCURVATURES_STATUS = 40;
    public static final String OMSCURVATURES_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSCURVATURES_AUTHORNAMES = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo";
    public static final String OMSCURVATURES_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSCURVATURES_inElev_DESCRIPTION = "The map of the digital elevation model (DEM or pit).";
    public static final String OMSCURVATURES_outProf_DESCRIPTION = "The map of profile curvatures.";
    public static final String OMSCURVATURES_outPlan_DESCRIPTION = "The map of planar curvatures.";
    public static final String OMSCURVATURES_outTang_DESCRIPTION = "The map of tangential curvatures.";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outProf == null, doReset)) {
            return;
        }
        checkNull(inElev);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        double novalue = HMConstants.getNovalue(inElev);
        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);

        WritableRaster profWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, novalue);
        WritableRaster planWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, novalue);
        WritableRaster tangWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, novalue);

        final double[] planTangProf = new double[3];


        try {
            /*
                 * calculate curvatures
                 */
            pm.beginTask(msg.message("curvatures.calculating"), (nRows - 2) * (nCols - 2));
            processGrid(nCols, nRows, true, ( c, r ) -> {
                if (pm.isCanceled()) {
                    return;
                }
                GridNode node = new GridNode(elevationIter, nCols, nRows, xRes, yRes, c, r, novalue);
                if (node.isValid() && !node.touchesNovalue() && !node.touchesBound()) {
                    calculateCurvatures2(node, planTangProf);
                    planWR.setSample(c, r, 0, planTangProf[0]);
                    tangWR.setSample(c, r, 0, planTangProf[1]);
                    profWR.setSample(c, r, 0, planTangProf[2]);
                }
                pm.worked(1);
            });
            pm.done();
        } finally {
            elevationIter.done();
        }
        if (pm.isCanceled()) {
            return;
        }
        outProf = CoverageUtilities.buildCoverageWithNovalue("prof_curvature", profWR, regionMap,
                inElev.getCoordinateReferenceSystem(), novalue);
        outPlan = CoverageUtilities.buildCoverageWithNovalue("plan_curvature", planWR, regionMap,
                inElev.getCoordinateReferenceSystem(), novalue);
        outTang = CoverageUtilities.buildCoverageWithNovalue("tang_curvature", tangWR, regionMap,
                inElev.getCoordinateReferenceSystem(), novalue);
    }

    /**
     * Calculate curvatures for a single cell.
     * 
     * @param elevationIter the elevation map.
     * @param planTangProf the array into which to insert the resulting [plan, tang, prof] curvatures.
     * @param c the column the process.
     * @param r the row the process.
     * @param xRes 
     * @param yRes
     * @param disXX the diagonal size of the cell, x component.
     * @param disYY the diagonal size of the cell, y component.
     */
    public static void calculateCurvatures( RandomIter elevationIter, final double[] planTangProf, int c, int r, double xRes,
            double yRes, double disXX, double disYY ) {

        double elevation = elevationIter.getSampleDouble(c, r, 0);
        if (!isNovalue(elevation)) {
            double elevRplus = elevationIter.getSampleDouble(c, r + 1, 0);
            double elevRminus = elevationIter.getSampleDouble(c, r - 1, 0);
            double elevCplus = elevationIter.getSampleDouble(c + 1, r, 0);
            double elevCminus = elevationIter.getSampleDouble(c - 1, r, 0);
            /*
             * first derivate
             */
            double sxValue = 0.5 * (elevRplus - elevRminus) / xRes;
            double syValue = 0.5 * (elevCplus - elevCminus) / yRes;
            double p = Math.pow(sxValue, 2.0) + Math.pow(syValue, 2.0);
            double q = p + 1;
            if (p == 0.0) {
                planTangProf[0] = 0.0;
                planTangProf[1] = 0.0;
                planTangProf[2] = 0.0;
            } else {
                double elevCplusRplus = elevationIter.getSampleDouble(c + 1, r + 1, 0);
                double elevCplusRminus = elevationIter.getSampleDouble(c + 1, r - 1, 0);
                double elevCminusRplus = elevationIter.getSampleDouble(c - 1, r + 1, 0);
                double elevCminusRminus = elevationIter.getSampleDouble(c - 1, r - 1, 0);

                double sxxValue = (elevRplus - 2 * elevation + elevRminus) / disXX;
                double syyValue = (elevCplus - 2 * elevation + elevCminus) / disYY;
                double sxyValue = 0.25
                        * ((elevCplusRplus - elevCplusRminus - elevCminusRplus + elevCminusRminus) / (xRes * yRes));

                planTangProf[0] = (sxxValue * Math.pow(syValue, 2.0) - 2 * sxyValue * sxValue * syValue
                        + syyValue * Math.pow(sxValue, 2.0)) / (Math.pow(p, 1.5));
                planTangProf[1] = (sxxValue * Math.pow(syValue, 2.0) - 2 * sxyValue * sxValue * syValue
                        + syyValue * Math.pow(sxValue, 2.0)) / (p * Math.pow(q, 0.5));
                planTangProf[2] = (sxxValue * Math.pow(sxValue, 2.0) + 2 * sxyValue * sxValue * syValue
                        + syyValue * Math.pow(syValue, 2.0)) / (p * Math.pow(q, 1.5));
            }
        } else {
            planTangProf[0] = doubleNovalue;
            planTangProf[1] = doubleNovalue;
            planTangProf[2] = doubleNovalue;
        }
    }

    public static void calculateCurvatures2( GridNode node, final double[] planTangProf ) {
        double disXX = Math.pow(node.xRes, 2.0);
        double disYY = Math.pow(node.yRes, 2.0);
        double elevation = node.elevation;
        double elevRplus = node.getSouthElev();
        double elevRminus = node.getNorthElev();
        double elevCplus = node.getEastElev();
        double elevCminus = node.getWestElev();
        /*
         * first derivate
         */
        double sxValue = 0.5 * (elevRplus - elevRminus) / node.xRes;
        double syValue = 0.5 * (elevCplus - elevCminus) / node.yRes;
        double p = Math.pow(sxValue, 2.0) + Math.pow(syValue, 2.0);
        double q = p + 1;
        if (p == 0.0) {
            planTangProf[0] = 0.0;
            planTangProf[1] = 0.0;
            planTangProf[2] = 0.0;
        } else {
            double elevCplusRplus = node.getSEElev();
            double elevCplusRminus = node.getENElev();
            double elevCminusRplus = node.getWSElev();
            double elevCminusRminus = node.getNWElev();

            double sxxValue = (elevRplus - 2 * elevation + elevRminus) / disXX;
            double syyValue = (elevCplus - 2 * elevation + elevCminus) / disYY;
            double sxyValue = 0.25
                    * ((elevCplusRplus - elevCplusRminus - elevCminusRplus + elevCminusRminus) / (node.xRes * node.yRes));

            planTangProf[0] = (sxxValue * Math.pow(syValue, 2.0) - 2 * sxyValue * sxValue * syValue
                    + syyValue * Math.pow(sxValue, 2.0)) / (Math.pow(p, 1.5));
            planTangProf[1] = (sxxValue * Math.pow(syValue, 2.0) - 2 * sxyValue * sxValue * syValue
                    + syyValue * Math.pow(sxValue, 2.0)) / (p * Math.pow(q, 0.5));
            planTangProf[2] = (sxxValue * Math.pow(sxValue, 2.0) + 2 * sxyValue * sxValue * syValue
                    + syyValue * Math.pow(syValue, 2.0)) / (p * Math.pow(q, 1.5));
        }
    }
}
