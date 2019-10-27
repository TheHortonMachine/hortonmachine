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
package org.hortonmachine.gears.modules.r.filter;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.WritableRaster;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

/** This plugin-Filter provides a selective mean (averaging) filter.
* In contrast to the standard mean filter, it preserves edges better
* and is less sensitive to outliers.
* Based on Lee's sigma filter algorithm and a plugin by Tony Collins.
*   J.S. Lee, Digital image noise smoothing and the sigma filter, in:
*   Computer Vision, Graphics and Image Processing, vol. 24, 255-269 (1983).
* The "Outlier Aware" option is a modification of Lee's algorithm introduced
* by Tony Collins.
*
* The filter smoothens an image by taking an average over the
* neighboring pixels, but only includes those pixels that have a
* value not deviating from the current pixel by more than a given
* range. The range is defined by the standard deviation of the pixel
* values within the neighborhood ("Use pixels within ... sigmas").
* If the number of pixels in this range is too low (less than "Minimum
* pixel fraction"), averaging over all neighboring pixels is performed.
* With the "Outlier Aware" option, averaging over all neighboring
* pixels excludes the center pixel. Thus, outliers having a value
* very different from the surrounding are not included in the average,
* i.e., completely eliminated.
*
* For preserving the edges, values of "Use pixels within" between
* 1 and 2 sigmas are recommended. With high values, the filter will behave
* more like a traditional averaging filter, i.e. smoothen the edges.
* Typical values of the minimum pixel fraction are around 0.2, with higher
* values resulting in more noise supression, but smoother edges.
*
* If preserving the edges is not desired, "Use pixels within" 2-3 sigmas
* and a minimum pixel fraction around 0.8-0.9, together with the "Outlier
* Aware" option will smoothen the image, similar to a traditional filter,
* but without being influenced by outliers strongly deviating from the
* surrounding pixels (hot pixels, dead pixels etc.).
*
*
* Code by Michael Schmid, 2007-10-25
*/
@Description("A sigma selective mean (averaging) filter ")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("raster, filter, sigma")
@Label(HMConstants.RASTERPROCESSING)
@Name("selectivesigmafilter")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsSigmaFilterPlus extends HMModel {

    @Description("The input raster")
    @In
    public GridCoverage2D inGeodata;

    @Description("The filter window radius in cells.")
    @In
    public double pRadius = 2.0;

    @Description("Pixel value range in sigmas")
    @In
    public double pSigmaWidth = 2.0;

    @Description("The output raster")
    @Out
    public GridCoverage2D outGeodata;

    private double minPixFraction = 0.2; // The fraction of pixels that need to be inside the
                                         // range for selective smoothing
    private boolean outlierAware = true; // Whether outliers will be excluded from averaging
    private int nPasses = 1; // The number of passes (color channels * stack slices)
    protected int kRadius; // kernel radius. Size is (2*kRadius+1)^2
    protected int kNPoints; // number of points in the kernel
    protected int[] lineRadius; // the length of each kernel line is 2*lineRadius+1

    public static void main( String[] args ) throws Exception {
        String inPath = "/home/hydrologis/TMP/R3GIS/data/planetscope_20191018_092049_0f15_sat0f15_epsg32634.tiff";
        String outPath = "/home/hydrologis/TMP/R3GIS/data/planetscope_20191018_092049_0f15_sat0f15_epsg32634_sigma.tiff";

        OmsSigmaFilterPlus sf = new OmsSigmaFilterPlus();
        sf.inGeodata = OmsRasterReader.readRaster(inPath);
        sf.pRadius = 5.0;
        sf.pSigmaWidth = 2.5;
        sf.process();
        GridCoverage2D outSigma = sf.outGeodata;
        OmsRasterWriter.writeRaster(outPath, outSigma);

    }

    @Execute
    public void process() throws Exception {
        checkNull(inGeodata);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inGeodata);

        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double[] pixels = CoverageUtilities.renderedImage2DoubleArray(inGeodata.getRenderedImage(), 3);

        /* 
         *  Create a circular kernel of a given radius. Radius = 0.5 includes the 4 neighbors of the
         *  pixel in the center, radius = 1 corresponds to a 3x3 kernel size.
         *  The output is written to class variables kNPoints (number of points inside the kernel) and
         *  lineRadius, which is an array giving the radius of each line. Line length is 2*lineRadius+1.
         */
        if (pRadius >= 1.5 && pRadius < 1.75) // this code creates the same sizes as the previous
            // RankFilters
            pRadius = 1.75;
        else if (pRadius >= 2.5 && pRadius < 2.85)
            pRadius = 2.85;
        int r2 = (int) (pRadius * pRadius) + 1;
        kRadius = (int) (Math.sqrt(r2 + 1e-10));
        lineRadius = new int[2 * kRadius + 1];
        lineRadius[kRadius] = kRadius;
        kNPoints = 2 * kRadius + 1;
        for( int y = 1; y <= kRadius; y++ ) {
            int dx = (int) (Math.sqrt(r2 - y * y + 1e-10));
            lineRadius[kRadius + y] = dx;
            lineRadius[kRadius - y] = dx;
            kNPoints += 4 * dx + 2;
        }

        // process passes
        for( int pass = 0; pass < nPasses; pass++ ) {
            int minPixNumber = (int) (kNPoints * minPixFraction + 0.999999); // min pixels in sigma
            // range
            pixels = doFiltering(pixels, cols, rows, kRadius, lineRadius, pSigmaWidth, minPixNumber, outlierAware);
        }

        WritableRaster outWR = CoverageUtilities.doubleArray2WritableRaster(pixels, cols, rows);
        outGeodata = CoverageUtilities.buildCoverage("sigma", outWR, regionMap, inGeodata.getCoordinateReferenceSystem());
    }

    public double[] doFiltering( double[] pixels, int cols, int rows, int kRadius, int[] lineRadius, double sigmaWidth,
            int minPixNumber, boolean outlierAware ) {
        int xmin = -kRadius;
        int xEnd = cols;
        int xmax = xEnd + kRadius;
        int kSize = 2 * kRadius + 1;
        int cacheWidth = xmax - xmin;
        int xminInside = xmin > 0 ? xmin : 0;
        int xmaxInside = xmax < cols ? xmax : cols;
        int widthInside = xmaxInside - xminInside;
        boolean smallKernel = kRadius < 2;
        double[] cache = new double[cacheWidth * kSize]; // a stripe of the image with
                                                         // height=2*kRadius+1
        for( int y = 0 - kRadius, iCache = 0; y < rows + kRadius; y++ )
            for( int x = xmin; x < xmax; x++, iCache++ ) // fill the cache for filtering the first
                                                         // line
                cache[iCache] = pixels[(x < 0 ? 0 : x >= cols ? cols - 1 : x) + cols * (y < 0 ? 0 : y >= rows ? rows - 1 : y)];
        int nextLineInCache = 2 * kRadius; // where the next line should be written to
        double[] sums = new double[2];
        pm.beginTask("Processing...", rows);
        for( int y = 0; y < rows; y++ ) {
            int ynext = y + kRadius; // C O P Y N E W L I N E into cache
            if (ynext >= rows)
                ynext = rows - 1;
            double leftpxl = pixels[cols * ynext]; // edge pixels of the line replace out-of-image
                                                   // pixels
            double rightpxl = pixels[cols - 1 + cols * ynext];
            int iCache = cacheWidth * nextLineInCache;// where in the cache we have to copy to
            for( int x = xmin; x < 0; x++, iCache++ )
                cache[iCache] = leftpxl;
            System.arraycopy(pixels, xminInside + cols * ynext, cache, iCache, widthInside);
            iCache += widthInside;
            for( int x = cols; x < xmax; x++, iCache++ )
                cache[iCache] = rightpxl;
            nextLineInCache = (nextLineInCache + 1) % kSize;
            boolean fullCalculation = true; // F I L T E R the line
            for( int x = 0, p = x + y * cols, xCache0 = kRadius; x < xEnd; x++, p++, xCache0++ ) {
                double value = pixels[p]; // the current pixel
                if (fullCalculation) {
                    fullCalculation = smallKernel; // for small kernel, always use the full area,
                                                   // not incremental algorithm
                    getAreaSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
                } else
                    addSideSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
                double mean = sums[0] / kNPoints; // sum[0] is the sum over the pixels, sum[1] the
                                                  // sum over the squares
                double variance = sums[1] / kNPoints - mean * mean;

                double sigmaRange = sigmaWidth * Math.sqrt(variance);
                double sigmaBottom = value - sigmaRange;
                double sigmaTop = value + sigmaRange;
                double sum = 0;
                int count = 0;
                for( int y1 = 0; y1 < kSize; y1++ ) { // for y1 within the cache stripe
                    for( int x1 = xCache0 - lineRadius[y1], iCache1 = y1 * cacheWidth + x1; x1 <= xCache0
                            + lineRadius[y1]; x1++, iCache1++ ) {
                        double v = cache[iCache1]; // a point within the kernel
                        if ((v >= sigmaBottom) && (v <= sigmaTop)) {
                            sum += v;
                            count++;
                        }
                    }
                }
                // if there are too few pixels in the kernel that are within sigma range, the
                // mean of the entire kernel is taken.
                if (count >= minPixNumber)
                    pixels[p] = (double) (sum / count);
                else {
                    if (outlierAware)
                        pixels[p] = (double) ((sums[0] - value) / (kNPoints - 1)); // assumes that
                                                                                   // the current
                                                                                   // pixel is an
                                                                                   // outlier
                    else
                        pixels[p] = (double) mean;
                }
            } // for x
            int newLineRadius0 = lineRadius[kSize - 1]; // shift kernel lineRadii one line
            System.arraycopy(lineRadius, 0, lineRadius, 1, kSize - 1);
            lineRadius[0] = newLineRadius0;

            pm.worked(1);
        }
        pm.done();
        return pixels;// for y
    }

    /** Get sum of values and values squared within the kernel area.
     *  xCache0 points to cache element equivalent to current x coordinate.
     *  Output is written to array sums[0] = sum; sums[1] = sum of squares */
    private void getAreaSums( double[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize, double[] sums ) {
        double sum = 0, sum2 = 0;
        for( int y = 0; y < kSize; y++ ) { // y within the cache stripe
            for( int x = xCache0 - lineRadius[y], iCache = y * cacheWidth + x; x <= xCache0 + lineRadius[y]; x++, iCache++ ) {
                double v = cache[iCache];
                sum += v;
                sum2 += v * v;
            }
        }
        sums[0] = sum;
        sums[1] = sum2;
        return;
    }

    /** Add all values and values squared at the right border inside minus at the left border outside the kernal area.
     *  Output is added or subtracted to/from array sums[0] += sum; sums[1] += sum of squares  when at 
     *  the right border, minus when at the left border */
    private void addSideSums( double[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize, double[] sums ) {
        double sum = 0, sum2 = 0;
        for( int y = 0; y < kSize; y++ ) { // y within the cache stripe
            int iCache0 = y * cacheWidth + xCache0;
            double v = cache[iCache0 + lineRadius[y]];
            sum += v;
            sum2 += v * v;
            v = cache[iCache0 - lineRadius[y] - 1];
            sum -= v;
            sum2 -= v * v;
        }
        sums[0] += sum;
        sums[1] += sum2;
        return;
    }

}
