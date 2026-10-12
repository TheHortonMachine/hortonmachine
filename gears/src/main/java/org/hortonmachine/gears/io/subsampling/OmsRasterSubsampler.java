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
package org.hortonmachine.gears.io.subsampling;

import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_AUTHORNAMES;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_DESCRIPTION;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_DOCUMENTATION;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_KEYWORDS;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_LABEL;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_LICENSE;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_NAME;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.AVG;
import static org.hortonmachine.gears.libs.modules.Variables.MAX;
import static org.hortonmachine.gears.libs.modules.Variables.MIN;
import static org.hortonmachine.gears.libs.modules.Variables.SUM;

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;

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
import oms3.annotations.UI;

@Description(OMSRASTERSUBSAMPLER_DESCRIPTION)
@Documentation(OMSRASTERSUBSAMPLER_DOCUMENTATION)
@Author(name = OMSRASTERSUBSAMPLER_AUTHORNAMES, contact = OMSRASTERSUBSAMPLER_AUTHORCONTACTS)
@Keywords(OMSRASTERSUBSAMPLER_KEYWORDS)
@Label(OMSRASTERSUBSAMPLER_LABEL)
@Name(OMSRASTERSUBSAMPLER_NAME)
@Status(OMSRASTERSUBSAMPLER_STATUS)
@License(OMSRASTERSUBSAMPLER_LICENSE)
public class OmsRasterSubsampler extends HMModel {

    @Description(OMSRASTERSUBSAMPLER_FILE_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSRASTERSUBSAMPLER_P_FACTOR_DESCRIPTION)
    @In
    public int pFactor = 2;

    @Description(OMSRASTERSUBSAMPLER_P_MODE_DESCRIPTION)
    @UI("combo:" + AVG + "," + MIN + "," + MAX + "," + SUM + "," + OmsRasterSubsampler.MAJORITY + ","
            + OmsRasterSubsampler.NEAREST)
    @In
    public String pMode = AVG;

    @Description(OMSRASTERSUBSAMPLER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    public static final String MAJORITY = "majority";
    public static final String NEAREST = "nearest";

    public static final String OMSRASTERSUBSAMPLER_DESCRIPTION = "Aggregates a raster to a coarser one, of cells of factor x factor input cells.";
    public static final String OMSRASTERSUBSAMPLER_DOCUMENTATION = """
            Every output cell covers a block of factor x factor input cells and gets the average, the \
            min, the max, the sum or the most frequent value of its valid cells: the novalues are left \
            out, and a block without valid cells is novalue. The resolution is exactly the input one \
            times the factor and the grid starts at the north-west corner of the input: the last \
            columns and rows that don't fill a whole block are left out, so the east and south borders \
            can move inwards by less than one output cell.

            The average suits the continuous variables, as an elevation model taken to a coarser \
            resolution, the sum the counts and the amounts per cell, the most frequent value (on ties \
            the smallest) the classes, as a land cover. On a block with novalues the sum is of the valid \
            cells only. The nearest mode takes the value of the input cell at the center of the block \
            (for an even factor, the one south-east of the center), novalue included: it is the \
            nearest neighbour resampling of RasterResolutionResampler, on the grid of the blocks.
            """;
    public static final String OMSRASTERSUBSAMPLER_KEYWORDS = "Raster, Subsampling, Aggregation, Resolution";
    public static final String OMSRASTERSUBSAMPLER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERSUBSAMPLER_NAME = "rastersubsampler";
    public static final int OMSRASTERSUBSAMPLER_STATUS = 40;
    public static final String OMSRASTERSUBSAMPLER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERSUBSAMPLER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERSUBSAMPLER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERSUBSAMPLER_FILE_DESCRIPTION = "The raster to aggregate.";
    public static final String OMSRASTERSUBSAMPLER_P_FACTOR_DESCRIPTION = "The number of input cells per side of an output cell (default is 2).";
    public static final String OMSRASTERSUBSAMPLER_P_MODE_DESCRIPTION = "The aggregation of the cells of a block (default is avg).";
    public static final String OMSRASTERSUBSAMPLER_OUT_RASTER_DESCRIPTION = "The aggregated raster.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pMode);

        if (!pMode.equals(AVG) && !pMode.equals(MIN) && !pMode.equals(MAX) && !pMode.equals(SUM) && !pMode.equals(MAJORITY)
                && !pMode.equals(NEAREST)) {
            throw new ModelsIllegalargumentException("Unknown mode: " + pMode, this, pm);
        }

        try (HMRaster raster = HMRaster.fromGridCoverage(inRaster)) {
            int cols = raster.getCols();
            int rows = raster.getRows();
            if (pFactor < 1 || pFactor > cols || pFactor > rows) {
                throw new ModelsIllegalargumentException(
                        "The factor needs to be at least 1 and not larger than the columns and rows of the raster.", this, pm);
            }
            int newCols = cols / pFactor;
            int newRows = rows / pFactor;

            RegionMap regionMap = raster.getRegionMap();
            double west = regionMap.getWest();
            double north = regionMap.getNorth();
            double newEast = west + newCols * regionMap.getXres() * pFactor;
            double newSouth = north - newRows * regionMap.getYres() * pFactor;
            RegionMap newRegionMap = RegionMap.fromBoundsAndGrid(west, newEast, newSouth, north, newCols, newRows);

            double novalue = raster.getNovalue();
            try (HMRaster outHMRaster = new HMRaster.HMRasterWritableBuilder().setCrs(raster.getCrs()).setName("subsampled")
                    .setRegion(newRegionMap).setNoValue(novalue).build()) {
                pm.beginTask("Aggregating cells...", newRows);
                for( int r = 0; r < newRows; r++ ) {
                    if (isCanceled(pm)) {
                        return;
                    }
                    for( int c = 0; c < newCols; c++ ) {
                        outHMRaster.setValue(c, r, aggregate(raster, c * pFactor, r * pFactor, novalue));
                    }
                    pm.worked(1);
                }
                pm.done();

                outRaster = outHMRaster.buildCoverage();
            }
        }
    }

    private double aggregate( HMRaster raster, int fromCol, int fromRow, double novalue ) {
        if (pMode.equals(NEAREST)) {
            return raster.getValue(fromCol + pFactor / 2, fromRow + pFactor / 2);
        }

        int count = 0;
        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        Map<Double, Integer> frequencies = pMode.equals(MAJORITY) ? new HashMap<>() : null;
        for( int r = fromRow; r < fromRow + pFactor; r++ ) {
            for( int c = fromCol; c < fromCol + pFactor; c++ ) {
                double value = raster.getValue(c, r);
                if (raster.isNovalue(value)) {
                    continue;
                }
                count++;
                sum += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
                if (frequencies != null) {
                    frequencies.merge(value, 1, Integer::sum);
                }
            }
        }
        if (count == 0) {
            return novalue;
        }
        switch( pMode ) {
        case MIN:
            return min;
        case MAX:
            return max;
        case SUM:
            return sum;
        case MAJORITY:
            double mostFrequent = novalue;
            int maxFrequency = 0;
            for( Map.Entry<Double, Integer> entry : frequencies.entrySet() ) {
                int frequency = entry.getValue();
                if (frequency > maxFrequency || (frequency == maxFrequency && entry.getKey() < mostFrequent)) {
                    mostFrequent = entry.getKey();
                    maxFrequency = frequency;
                }
            }
            return mostFrequent;
        case AVG:
        default:
            return sum / count;
        }
    }

}
