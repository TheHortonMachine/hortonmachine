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
package org.hortonmachine.gears.modules.r.edgedetection;

import static org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector.BIBLIOGRAPHY_CANNY_1986;
import static org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector.OMSCANNY_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector.OMSCANNY_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector.OMSCANNY_KEYWORDS;
import static org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector.OMSCANNY_STATUS;

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
import oms3.annotations.Unit;

@Description(OMSCANNY_DESCRIPTION)
@Documentation(OMSCANNY_DOCUMENTATION)
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords(OMSCANNY_KEYWORDS)
@Label(HMConstants.RASTERPROCESSING)
@Name("omscanny")
@Status(OMSCANNY_STATUS)
@License(HMConstants.GPL3_LICENSE)
@Bibliography(BIBLIOGRAPHY_CANNY_1986)
public class OmsCannyEdgeDetector extends HMModel {

    @Description(OMSCANNY_inMap_DESCRIPTION)
    @In
    public GridCoverage2D inMap = null;

    @Description(OMSCANNY_pLowthres_DESCRIPTION)
    @In
    public Double pLowthres = 0.1;

    @Description(OMSCANNY_pHighthres_DESCRIPTION)
    @In
    public Double pHighthres = 0.2;

    @Description(OMSCANNY_pRadiusgauss_DESCRIPTION)
    @Unit("cells")
    @In
    public Double pRadiusgauss = 2.0;

    @Description(OMSCANNY_pWidthgauss_DESCRIPTION)
    @Unit("cells")
    @In
    public Integer pWidthgauss = 16;

    @Description(OMSCANNY_outMap_DESCRIPTION)
    @Out
    public GridCoverage2D outMap = null;

    public static final String BIBLIOGRAPHY_CANNY_1986 = "Canny, J. (1986). A computational approach to edge detection. IEEE Transactions on Pattern Analysis and Machine Intelligence, 8(6), 679-698.";
    public static final String OMSCANNY_DESCRIPTION = "Finds the edges of a raster with the Canny method: the thin lines where the values change most rapidly, as the breaks of slope on a slope map or the borders of the fields on an image. The edge cells get 1, all the others no-data.";
    public static final String OMSCANNY_DOCUMENTATION = """
            The raster is first smoothed with a gaussian kernel, of standard deviation pRadiusgauss cells, to remove the noise. On the smoothed values the gradient, the rate of change, is computed with the Sobel operator. Only the cells where the gradient is larger than in the two neighbours across the edge, along the direction of the gradient, are kept, so that the edges are one cell thin.

            Two thresholds, fractions of the largest gradient of the map, then select the edges: the cells above pHighthres start an edge, which continues through the neighbouring cells above pLowthres. In this way the edges are not broken where the gradient weakens for a while, and the isolated weak changes are left out. Lower thresholds find more, and weaker, edges.

            The values are used as they are, so the module works on any single band raster: on an elevation model the edges follow the steepest slopes; for the breaks of slope, as the edges of the terraces or the scarps, it is better to run it on the slope map. The no-data cells are left out of the smoothing and the cells next to them get no gradient, so the border of the data is not an edge.
            """;
    public static final String OMSCANNY_KEYWORDS = "Edge detection, Canny, Raster";
    public static final int OMSCANNY_STATUS = 40;
    public static final String OMSCANNY_inMap_DESCRIPTION = "The map on which to find the edges.";
    public static final String OMSCANNY_pLowthres_DESCRIPTION = "The low threshold, as a fraction of the largest gradient of the map: the edges continue through the cells above it.";
    public static final String OMSCANNY_pHighthres_DESCRIPTION = "The high threshold, as a fraction of the largest gradient of the map: the edges start from the cells above it.";
    public static final String OMSCANNY_pRadiusgauss_DESCRIPTION = "The standard deviation of the gaussian smoothing, in cells: larger values remove more noise and keep only the larger edges.";
    public static final String OMSCANNY_pWidthgauss_DESCRIPTION = "The largest radius of the gaussian kernel, in cells: the kernel stops earlier where its weights become negligible.";
    public static final String OMSCANNY_outMap_DESCRIPTION = "The map of the edges: 1 on the edges, no-data elsewhere.";

    /** The weight below which the gaussian kernel stops. */
    private static final double GAUSSIAN_CUT_OFF = 0.005;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        checkNull(inMap, pLowthres, pHighthres, pRadiusgauss, pWidthgauss);
        if (pLowthres <= 0 || pHighthres > 1 || pLowthres > pHighthres) {
            throw new ModelsIllegalargumentException(
                    "The thresholds have to be fractions with 0 < low <= high <= 1: " + pLowthres + ", " + pHighthres, this, pm);
        }
        if (pRadiusgauss <= 0 || pWidthgauss < 1) {
            throw new ModelsIllegalargumentException(
                    "The gaussian kernel needs a positive radius and width: " + pRadiusgauss + ", " + pWidthgauss, this, pm);
        }

        try (HMRaster in = HMRaster.fromGridCoverage(inMap);
                HMRaster out = new HMRasterWritableBuilder().setName("canny").setTemplate(in).build()) {
            int cols = in.getCols();
            int rows = in.getRows();
            int startCol = in.getStartCol();
            int startRow = in.getStartRow();

            double[] values = new double[cols * rows];
            boolean[] valid = new boolean[cols * rows];
            in.process(pm, "Reading the map...", ( col, row, value, c, r ) -> {
                int i = (row - startRow) * cols + (col - startCol);
                valid[i] = !in.isNovalue(value);
                values[i] = valid[i] ? value : 0;
            });
            if (pm.isCanceled()) {
                return;
            }

            pm.beginTask("Finding the edges...", -1);
            boolean[] edges = findEdges(values, valid, cols, rows, pRadiusgauss, pWidthgauss, pLowthres, pHighthres);
            pm.done();

            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    if (edges[row * cols + col]) {
                        out.setValue(startCol + col, startRow + row, 1.0);
                    }
                }
            }
            outMap = out.buildCoverage();
        }
    }

    /**
     * Find the Canny edges of a grid.
     *
     * @param values the values in row-major order.
     * @param valid the cells that have a value.
     * @param cols the number of columns.
     * @param rows the number of rows.
     * @param sigma the standard deviation of the gaussian smoothing, in cells.
     * @param maxRadius the largest radius of the gaussian kernel, in cells.
     * @param low the low threshold, as a fraction of the largest gradient.
     * @param high the high threshold, as a fraction of the largest gradient.
     * @return the edge cells, in row-major order.
     */
    public static boolean[] findEdges( double[] values, boolean[] valid, int cols, int rows, double sigma, int maxRadius,
            double low, double high ) {
        boolean[] edges = new boolean[cols * rows];
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for( int i = 0; i < values.length; i++ ) {
            if (valid[i]) {
                min = Math.min(min, values[i]);
                max = Math.max(max, values[i]);
            }
        }
        if (!(max > min)) {
            // no values or a flat map
            return edges;
        }
        // gradients below this are the rounding of the smoothing, not changes of the values
        double noise = (max - min) * 1E-9;

        double[] smoothed = smooth(values, valid, cols, rows, gaussianKernel(sigma, maxRadius));

        // Sobel gradient, only where the whole 3x3 window has values
        double[] magnitude = new double[cols * rows];
        double[] gx = new double[cols * rows];
        double[] gy = new double[cols * rows];
        double maxMagnitude = 0;
        for( int r = 1; r < rows - 1; r++ ) {
            for( int c = 1; c < cols - 1; c++ ) {
                if (!windowIsValid(valid, cols, c, r)) {
                    continue;
                }
                int i = r * cols + c;
                double nw = smoothed[i - cols - 1], n = smoothed[i - cols], ne = smoothed[i - cols + 1];
                double w = smoothed[i - 1], e = smoothed[i + 1];
                double sw = smoothed[i + cols - 1], s = smoothed[i + cols], se = smoothed[i + cols + 1];
                // x towards the columns, y towards the rows
                double dx = (ne + 2 * e + se) - (nw + 2 * w + sw);
                double dy = (sw + 2 * s + se) - (nw + 2 * n + ne);
                double m = Math.hypot(dx, dy);
                if (m > noise) {
                    gx[i] = dx;
                    gy[i] = dy;
                    magnitude[i] = m;
                    maxMagnitude = Math.max(maxMagnitude, m);
                }
            }
        }
        if (maxMagnitude == 0) {
            return edges;
        }

        // non maximum suppression: keep the cells that are a maximum across the edge
        double[] thin = new double[cols * rows];
        for( int r = 1; r < rows - 1; r++ ) {
            for( int c = 1; c < cols - 1; c++ ) {
                int i = r * cols + c;
                double m = magnitude[i];
                if (m == 0) {
                    continue;
                }
                // the direction of the gradient, folded in [0, 180) and rounded to 45 degrees
                double angle = Math.toDegrees(Math.atan2(gy[i], gx[i]));
                if (angle < 0) {
                    angle += 180;
                }
                int offset;
                if (angle < 22.5 || angle >= 157.5) {
                    offset = 1; // along the columns
                } else if (angle < 67.5) {
                    offset = cols + 1; // towards south-east
                } else if (angle < 112.5) {
                    offset = cols; // along the rows
                } else {
                    offset = cols - 1; // towards south-west
                }
                // strictly larger on one side, so that plateaus give a single line
                if (m > magnitude[i - offset] && m >= magnitude[i + offset]) {
                    thin[i] = m;
                }
            }
        }

        // hysteresis: from the strong cells, follow all the connected cells above the low threshold
        double highValue = high * maxMagnitude;
        double lowValue = low * maxMagnitude;
        int[] stack = new int[cols * rows];
        for( int start = 0; start < thin.length; start++ ) {
            if (edges[start] || thin[start] < highValue) {
                continue;
            }
            int top = 0;
            stack[top++] = start;
            edges[start] = true;
            while( top > 0 ) {
                int i = stack[--top];
                int r = i / cols;
                int c = i % cols;
                for( int dr = -1; dr <= 1; dr++ ) {
                    for( int dc = -1; dc <= 1; dc++ ) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                            continue;
                        }
                        int ni = nr * cols + nc;
                        if (!edges[ni] && thin[ni] > 0 && thin[ni] >= lowValue) {
                            edges[ni] = true;
                            stack[top++] = ni;
                        }
                    }
                }
            }
        }
        return edges;
    }

    /**
     * The one sided gaussian kernel, from the centre outwards, cut where the weights become negligible.
     */
    static double[] gaussianKernel( double sigma, int maxRadius ) {
        int radius = 0;
        while( radius < maxRadius && gaussian(radius + 1, sigma) > GAUSSIAN_CUT_OFF ) {
            radius++;
        }
        double[] kernel = new double[radius + 1];
        for( int i = 0; i <= radius; i++ ) {
            kernel[i] = gaussian(i, sigma);
        }
        return kernel;
    }

    private static double gaussian( double x, double sigma ) {
        return Math.exp(-(x * x) / (2 * sigma * sigma));
    }

    /**
     * Separable gaussian smoothing of the valid cells. Each pass is normalized by the weights of
     * the valid cells it uses, so that the no-data doesn't drag the values down.
     */
    private static double[] smooth( double[] values, boolean[] valid, int cols, int rows, double[] kernel ) {
        int radius = kernel.length - 1;
        double[] alongCols = new double[values.length];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                int i = r * cols + c;
                if (!valid[i]) {
                    continue;
                }
                double sum = 0;
                double weights = 0;
                for( int k = -radius; k <= radius; k++ ) {
                    int nc = c + k;
                    if (nc >= 0 && nc < cols && valid[i + k]) {
                        double weight = kernel[Math.abs(k)];
                        sum += weight * values[i + k];
                        weights += weight;
                    }
                }
                alongCols[i] = sum / weights;
            }
        }
        double[] smoothed = new double[values.length];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                int i = r * cols + c;
                if (!valid[i]) {
                    continue;
                }
                double sum = 0;
                double weights = 0;
                for( int k = -radius; k <= radius; k++ ) {
                    int nr = r + k;
                    int ni = i + k * cols;
                    if (nr >= 0 && nr < rows && valid[ni]) {
                        double weight = kernel[Math.abs(k)];
                        sum += weight * alongCols[ni];
                        weights += weight;
                    }
                }
                smoothed[i] = sum / weights;
            }
        }
        return smoothed;
    }

    private static boolean windowIsValid( boolean[] valid, int cols, int c, int r ) {
        for( int dr = -1; dr <= 1; dr++ ) {
            for( int dc = -1; dc <= 1; dc++ ) {
                if (!valid[(r + dr) * cols + c + dc]) {
                    return false;
                }
            }
        }
        return true;
    }

}
