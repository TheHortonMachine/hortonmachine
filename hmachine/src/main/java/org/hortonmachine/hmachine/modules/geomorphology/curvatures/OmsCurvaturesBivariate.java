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

import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_LABEL;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_outPlan_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_outProf_DESCRIPTION;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RRQRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Estimates the longitudinal, normal and planar curvatures by means of a bivariate quadratic representation of the terrain.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("curvatures, bivariate, slope, aspect")
@Label(OMSCURVATURES_LABEL)
@Name("oms_curvaturesbivariate")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@Bibliography("Multiscale Terrain Analysis of Multibeam Bathymetry Data for Habitat Mapping on the Continental Slope, Wilson M., 2007")
public class OmsCurvaturesBivariate extends HMModel {
    @Description("The map of the digital elevation model (DEM or pit).")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public GridCoverage2D inElev = null;

    @Description("The size of the analysis window in odd cells number.")
    @In
    public int pCells = 3;

    // output
    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outPlan = null;

    @Description("The map of slope.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outSlope = null;

    @Description("The map of aspect")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outAspect = null;

    private double novalue;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outProf == null, doReset)) {
            return;
        }
        checkNull(inElev);
        novalue = HMConstants.getNovalue(inElev);

        if (pCells < 3) {
            pCells = 3;
        }
        if (pCells % 2 == 0) {
            pCells = pCells + 1;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);

        WritableRaster profWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, novalue);
        WritableRaster planWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, novalue);
        WritableRaster slopeWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, novalue);
        WritableRaster aspectWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, novalue);

        final double[] planProfSlopeAspect = new double[4];
        double disXX = Math.pow(xRes, 2.0);
        double disYY = Math.pow(yRes, 2.0);
        /*
         * calculate curvatures
         */
        pm.beginTask("Processing...", nRows - 2);
        for( int r = 1; r < nRows - 1; r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = 1; c < nCols - 1; c++ ) {
                calculateCurvatures(elevationIter, novalue, planProfSlopeAspect, nCols, nRows, c, r, xRes, yRes, disXX, disYY,
                        pCells);
                planWR.setSample(c, r, 0, planProfSlopeAspect[0]);
                profWR.setSample(c, r, 0, planProfSlopeAspect[1]);
                slopeWR.setSample(c, r, 0, planProfSlopeAspect[2]);
                aspectWR.setSample(c, r, 0, planProfSlopeAspect[3]);
            }
            pm.worked(1);
        }
        pm.done();

        if (pm.isCanceled()) {
            return;
        }
        CoordinateReferenceSystem crs = inElev.getCoordinateReferenceSystem();
        outProf = CoverageUtilities.buildCoverageWithNovalue("prof_curvature", profWR, regionMap, crs, novalue);
        outPlan = CoverageUtilities.buildCoverageWithNovalue("plan_curvature", planWR, regionMap, crs, novalue);
        outSlope = CoverageUtilities.buildCoverageWithNovalue("slope", slopeWR, regionMap, crs, novalue);
        outAspect = CoverageUtilities.buildCoverageWithNovalue("aspect", aspectWR, regionMap, crs, novalue);
    }

    /**
     * Calculate curvatures for a single cell.
     * 
     * @param elevationIter the elevation map.
     * @param planTangProf the array into which to insert the resulting [plan, tang, prof] curvatures.
     * @param col the column to process.
     * @param row the row to process.
     * @param ncols the columns of the raster.
     * @param nrows the rows of the raster.
     * @param xRes 
     * @param yRes
     * @param disXX the diagonal size of the cell, x component.
     * @param disYY the diagonal size of the cell, y component.
     */
    public static void calculateCurvatures( RandomIter elevationIter, double novalue, final double[] planTangProf, int ncols,
            int nrows, int col, int row, double xRes, double yRes, double disXX, double disYY, int windowSize ) {

        GridNode node = new GridNode(elevationIter, ncols, nrows, xRes, yRes, col, row, novalue);
        double[][] window = node.getWindow(windowSize, false);
        if (!hasNovalues(window)) {
            double[] parameters = calculateParameters(window);
            double a = parameters[0];
            double b = parameters[1];
            double c = parameters[2];
            double d = parameters[3];
            double e = parameters[4];

            double slope = atan(sqrt(d * d + e * e));
            slope = toDegrees(slope);
            double aspect = atan(e / d);
            aspect = toDegrees(aspect);

            double profcNumerator = -200.0 * (a * d * d + b * e * e + c * d * e);
            double profcDenominator = (e * e + d * d) * pow((1 + e * e + d * d), 1.5);
            double profc = profcNumerator / profcDenominator;

            double plancNumerator = -200.0 * (b * d * d + a * e * e + c * d * e);
            double plancDenominator = pow((e * e + d * d), 1.5);
            double planc = plancNumerator / plancDenominator;

            planTangProf[0] = planc;
            planTangProf[1] = profc;
            planTangProf[2] = slope;
            planTangProf[3] = aspect;
        } else {
            planTangProf[0] = doubleNovalue;
            planTangProf[1] = doubleNovalue;
            planTangProf[2] = doubleNovalue;
            planTangProf[3] = doubleNovalue;
        }
    }

    private static boolean hasNovalues( double[][] window ) {
        for( int i = 0; i < window.length; i++ ) {
            for( int j = 0; j < window[0].length; j++ ) {
                if (HMConstants.isNovalue(window[i][j])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculates the parameters of a bivariate quadratic equation.
     * 
     * @param elevationValues the window of points to use.
     * @return the parameters of the bivariate quadratic equation as [a, b, c, d, e, f]
     */
    private static double[] calculateParameters( final double[][] elevationValues ) {
        int rows = elevationValues.length;
        int cols = elevationValues[0].length;
        int pointsNum = rows * cols;

        final double[][] xyMatrix = new double[pointsNum][6];
        final double[] valueArray = new double[pointsNum];

        // TODO check on resolution
        int index = 0;
        for( int y = 0; y < rows; y++ ) {
            for( int x = 0; x < cols; x++ ) {
                xyMatrix[index][0] = x * x; // x^2
                xyMatrix[index][1] = y * y; // y^2
                xyMatrix[index][2] = x * y; // xy
                xyMatrix[index][3] = x; // x
                xyMatrix[index][4] = y; // y
                xyMatrix[index][5] = 1;
                valueArray[index] = elevationValues[y][x];
                index++;
            }
        }

        RealMatrix A = MatrixUtils.createRealMatrix(xyMatrix);
        RealVector z = MatrixUtils.createRealVector(valueArray);

        DecompositionSolver solver = new RRQRDecomposition(A).getSolver();
        RealVector solution = solver.solve(z);

        // start values for a, b, c, d, e, f, all set to 0.0
        final double[] parameters = solution.toArray();
        return parameters;
    }

}
