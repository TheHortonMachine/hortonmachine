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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_LABEL;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_outPlan_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_outProf_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate.BIBLIOGRAPHY_WILSON_2007;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate.BIBLIOGRAPHY_WOOD_1996;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate.OMSCURVATURESBIVARIATE_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate.OMSCURVATURESBIVARIATE_DOCUMENTATION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate.OMSCURVATURESBIVARIATE_KEYWORDS;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate.OMSCURVATURESBIVARIATE_STATUS;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RRQRDecomposition;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;

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
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OMSCURVATURESBIVARIATE_DESCRIPTION)
@Documentation(OMSCURVATURESBIVARIATE_DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSCURVATURESBIVARIATE_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name("oms_curvaturesbivariate")
@Status(OMSCURVATURESBIVARIATE_STATUS)
@License(OMSHYDRO_LICENSE)
@Bibliography({BIBLIOGRAPHY_WOOD_1996, BIBLIOGRAPHY_WILSON_2007})
public class OmsCurvaturesBivariate extends HMModel {
    @Description(OMSCURVATURESBIVARIATE_inElev_DESCRIPTION)
    @Unit("m")
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSCURVATURESBIVARIATE_pCells_DESCRIPTION)
    @Unit("cells")
    @In
    public int pCells = 3;

    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @Unit("1/m")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @Unit("1/m")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outPlan = null;

    @Description(OMSCURVATURESBIVARIATE_outSlope_DESCRIPTION)
    @Unit("degree")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outSlope = null;

    @Description(OMSCURVATURESBIVARIATE_outAspect_DESCRIPTION)
    @Unit("degree")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outAspect = null;

    public static final String BIBLIOGRAPHY_WOOD_1996 = "Wood, J. (1996). The geomorphological characterisation of digital elevation models. PhD thesis, University of Leicester.";
    public static final String BIBLIOGRAPHY_WILSON_2007 = "Wilson, M. F. J., O'Connell, B., Brown, C., Guinan, J. C., Grehan, A. J. (2007). Multiscale terrain analysis of multibeam bathymetry data for habitat mapping on the continental slope. Marine Geodesy, 30(1-2), 3-35.";
    public static final String OMSCURVATURESBIVARIATE_DESCRIPTION = "Calculates the profile and planar curvatures, the slope and the aspect of the terrain from a quadratic surface fitted by least squares to a window of pCells x pCells cells around each cell. Larger windows describe the forms at a larger scale. The cells closer to the border of the map or of the no-data than half the window are left as no-data.";
    public static final String OMSCURVATURESBIVARIATE_DOCUMENTATION = """
            For each cell, the surface z = ax² + by² + cxy + dx + ey + f is fitted by least squares to the elevations of the window, with the coordinates in map units, centred on the cell, x towards east and y towards north. The derivatives of the surface at the centre give the slope, from the gradient (d, e), the aspect, the direction opposite to the gradient, and the curvatures, from the second derivatives 2a, 2b and c, as in Wood (1996).

            The curvatures have the same units and signs of the Curvatures module: 1/m, positive where the profile is concave and where the contour lines converge. On a 3x3 window the fitted derivatives are those of the finite differences of Evans, so the results are close to Curvatures; larger windows smooth the small forms and the noise of the elevation, and bring out the forms of the size of the window, which is the reason to use this module.

            On the flat cells, where the gradient is zero, the curvatures and the slope are 0 and the aspect, undefined, is no-data.
            """;
    public static final String OMSCURVATURESBIVARIATE_KEYWORDS = "Geomorphology, Curvature, Bivariate, Slope, Aspect, Multiscale";
    public static final int OMSCURVATURESBIVARIATE_STATUS = 40;
    public static final String OMSCURVATURESBIVARIATE_inElev_DESCRIPTION = "The map of the elevation.";
    public static final String OMSCURVATURESBIVARIATE_pCells_DESCRIPTION = "The size of the window, in cells: an odd number, at least 3.";
    public static final String OMSCURVATURESBIVARIATE_outSlope_DESCRIPTION = "The map of the slope, in degrees.";
    public static final String OMSCURVATURESBIVARIATE_outAspect_DESCRIPTION = "The map of the aspect, in degrees clockwise from north: 0 for the slopes facing north, 90 east, 180 south, 270 west. No-data on the flat cells.";

    /** Below this squared gradient the cell is flat. */
    private static final double FLAT_GRADIENT2 = 1E-20;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outProf == null, doReset)) {
            return;
        }
        checkNull(inElev);
        if (pCells < 3 || pCells % 2 == 0) {
            throw new ModelsIllegalargumentException("The window size has to be an odd number of cells, at least 3: " + pCells,
                    this, pm);
        }

        try (HMRaster elev = HMRaster.fromGridCoverage(inElev);
                HMRaster prof = new HMRasterWritableBuilder().setName("prof_curvature").setTemplate(elev).build();
                HMRaster plan = new HMRasterWritableBuilder().setName("plan_curvature").setTemplate(elev).build();
                HMRaster slope = new HMRasterWritableBuilder().setName("slope").setTemplate(elev).build();
                HMRaster aspect = new HMRasterWritableBuilder().setName("aspect").setTemplate(elev).build()) {

            double[][] fit = fitMatrix(pCells, elev.getXRes(), elev.getYRes());
            int half = pCells / 2;
            int size = pCells * pCells;
            double[] window = new double[size];
            double[] results = new double[4];

            elev.process(pm, "Fitting the bivariate quadratic surfaces...", ( col, row, value, cols, rows ) -> {
                if (elev.isNovalue(value)) {
                    return;
                }
                int i = 0;
                for( int r = row - half; r <= row + half; r++ ) {
                    for( int c = col - half; c <= col + half; c++ ) {
                        double v = elev.getValue(c, r);
                        if (elev.isNovalue(v)) {
                            // outside of the map or no-data in the window
                            return;
                        }
                        window[i++] = v;
                    }
                }
                calculate(fit, window, results);
                prof.setValue(col, row, results[0]);
                plan.setValue(col, row, results[1]);
                slope.setValue(col, row, results[2]);
                if (!Double.isNaN(results[3])) {
                    aspect.setValue(col, row, results[3]);
                }
            });
            if (pm.isCanceled()) {
                return;
            }

            outProf = prof.buildCoverage();
            outPlan = plan.buildCoverage();
            outSlope = slope.buildCoverage();
            outAspect = aspect.buildCoverage();
        }
    }

    /**
     * Build the matrix that gives the parameters of the least squares quadratic surface from the
     * values of a window.
     *
     * <p>The surface is <code>z = ax² + by² + cxy + dx + ey + f</code>, with x towards east and y
     * towards north, in map units, from the centre of the window. As the coordinates are the same
     * for every window, the pseudo-inverse of the design matrix is computed once.</p>
     *
     * @param windowSize the odd size of the window, in cells.
     * @param xRes the cell size along x.
     * @param yRes the cell size along y.
     * @return the 6 x windowSize² matrix that, multiplied by the window values in row-major order
     *          from the north-west corner, gives [a, b, c, d, e, f].
     */
    public static double[][] fitMatrix( int windowSize, double xRes, double yRes ) {
        int half = windowSize / 2;
        double[][] design = new double[windowSize * windowSize][6];
        int i = 0;
        for( int r = -half; r <= half; r++ ) {
            for( int c = -half; c <= half; c++ ) {
                double x = c * xRes;
                // rows grow towards south
                double y = -r * yRes;
                design[i][0] = x * x;
                design[i][1] = y * y;
                design[i][2] = x * y;
                design[i][3] = x;
                design[i][4] = y;
                design[i][5] = 1;
                i++;
            }
        }
        // least squares solution for each unit vector: the columns of the pseudo-inverse
        return new RRQRDecomposition(MatrixUtils.createRealMatrix(design)).getSolver()
                .solve(MatrixUtils.createRealIdentityMatrix(design.length)).getData();
    }

    /**
     * Calculate the curvatures, slope and aspect at the centre of a window.
     *
     * @param fit the matrix of {@link #fitMatrix(int, double, double)}.
     * @param window the window values in row-major order from the north-west corner.
     * @param results the array to fill with [profile curvature, planar curvature, slope in degrees,
     *          aspect in degrees clockwise from north or NaN on flat cells].
     */
    public static void calculate( double[][] fit, double[] window, double[] results ) {
        double a = dot(fit[0], window);
        double b = dot(fit[1], window);
        double c = dot(fit[2], window);
        double d = dot(fit[3], window);
        double e = dot(fit[4], window);

        // first derivatives zx = d, zy = e; second derivatives zxx = 2a, zyy = 2b, zxy = c
        double p = d * d + e * e;
        if (p < FLAT_GRADIENT2) {
            results[0] = 0;
            results[1] = 0;
            results[2] = 0;
            results[3] = Double.NaN;
            return;
        }
        // positive where the slope decreases downhill
        results[0] = (2 * a * d * d + 2 * c * d * e + 2 * b * e * e) / (p * Math.pow(1 + p, 1.5));
        // positive where the contour lines converge
        results[1] = (2 * a * e * e - 2 * c * d * e + 2 * b * d * d) / Math.pow(p, 1.5);
        results[2] = Math.toDegrees(Math.atan(Math.sqrt(p)));
        // the slope faces the direction opposite to the gradient
        double azimuth = Math.toDegrees(Math.atan2(-d, -e));
        if (azimuth < 0) {
            azimuth += 360;
        }
        results[3] = azimuth;
    }

    private static double dot( double[] v1, double[] v2 ) {
        double sum = 0;
        for( int i = 0; i < v1.length; i++ ) {
            sum += v1[i] * v2[i];
        }
        return sum;
    }

}
