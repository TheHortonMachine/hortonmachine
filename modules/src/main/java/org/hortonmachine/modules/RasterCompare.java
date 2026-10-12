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
package org.hortonmachine.modules;

import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_AUTHORCONTACTS;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_AUTHORNAMES;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_DESCRIPTION;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_DOCUMENTATION;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_KEYWORDS;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_LABEL;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_LICENSE;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_NAME;
import static org.hortonmachine.modules.RasterCompare.RASTERCOMPARE_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

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

@Description(RASTERCOMPARE_DESCRIPTION)
@Documentation(RASTERCOMPARE_DOCUMENTATION)
@Author(name = RASTERCOMPARE_AUTHORNAMES, contact = RASTERCOMPARE_AUTHORCONTACTS)
@Keywords(RASTERCOMPARE_KEYWORDS)
@Label(RASTERCOMPARE_LABEL)
@Name("_" + RASTERCOMPARE_NAME)
@Status(RASTERCOMPARE_STATUS)
@License(RASTERCOMPARE_LICENSE)
public class RasterCompare extends HMModel {

    @Description(RASTERCOMPARE_IN_RASTER1_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster1;

    @Description(RASTERCOMPARE_IN_RASTER2_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster2;

    @Description(RASTERCOMPARE_P_TOLERANCE_DESCRIPTION)
    @In
    public double pTolerance = 0.0;

    @Description(RASTERCOMPARE_P_MAX_REPORTED_DESCRIPTION)
    @In
    public int pMaxReported = 10;

    @Description(RASTERCOMPARE_OUT_SAME_GRID_DESCRIPTION)
    @Out
    public Boolean outSameGrid = null;

    @Description(RASTERCOMPARE_OUT_DIFFERENT_DESCRIPTION)
    @Out
    public Long outDifferent = null;

    @Description(RASTERCOMPARE_OUT_MAX_DIFFERENCE_DESCRIPTION)
    @Out
    public Double outMaxDifference = null;

    public static final String RASTERCOMPARE_DESCRIPTION = "Compares two rasters, their grids and their values cell by cell.";
    public static final String RASTERCOMPARE_DOCUMENTATION = """
            The report lists the differences of the coordinate reference systems, of the bounds, of the \
            resolutions and of the columns and rows. If the two grids are the same, the values are \
            compared cell by cell: it counts the cells with equal values, with different values, and \
            the cells that are novalue in only one of the rasters, which count as different. Of the \
            values that differ it gives the max and the mean absolute difference, and the first \
            different cells, with their column, row and values.

            The tolerance sets when two values are equal: with 0 they need to be exactly the same, with \
            0.01 the differences up to one centimeter of two elevation models are not counted. The outputs carry the result to a script, as a test that a \
            computation still gives the reference raster.
            """;
    public static final String RASTERCOMPARE_KEYWORDS = "Compare, Difference, Raster";
    public static final String RASTERCOMPARE_LABEL = HMConstants.RASTERPROCESSING;
    public static final String RASTERCOMPARE_NAME = "rcompare";
    public static final int RASTERCOMPARE_STATUS = 40;
    public static final String RASTERCOMPARE_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String RASTERCOMPARE_AUTHORNAMES = "Andrea Antonello";
    public static final String RASTERCOMPARE_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String RASTERCOMPARE_IN_RASTER1_DESCRIPTION = "The first raster.";
    public static final String RASTERCOMPARE_IN_RASTER2_DESCRIPTION = "The second raster.";
    public static final String RASTERCOMPARE_P_TOLERANCE_DESCRIPTION = "The largest difference of two values seen as equal (default 0).";
    public static final String RASTERCOMPARE_P_MAX_REPORTED_DESCRIPTION = "The number of different cells listed in the report (default 10).";
    public static final String RASTERCOMPARE_OUT_SAME_GRID_DESCRIPTION = "True if the rasters have the same grid, and so their values were compared.";
    public static final String RASTERCOMPARE_OUT_DIFFERENT_DESCRIPTION = "The number of cells with different values, novalue in one raster only included.";
    public static final String RASTERCOMPARE_OUT_MAX_DIFFERENCE_DESCRIPTION = "The max absolute difference of the cells valid in both rasters.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster1, inRaster2);

        try (HMRaster raster1 = HMRaster.fromGridCoverage(getRaster(inRaster1));
                HMRaster raster2 = HMRaster.fromGridCoverage(getRaster(inRaster2))) {
            RegionMap regionMap1 = raster1.getRegionMap();
            RegionMap regionMap2 = raster2.getRegionMap();

            StringBuilder sb = new StringBuilder();
            sb.append("\nGRID GEOMETRY\n");
            sb.append("================================\n");
            if (!HMCrsRegistry.crsEquals(raster1.getCrs(), raster2.getCrs())) {
                sb.append("The coordinate reference systems are different.\n");
            }
            boolean sameColsRows = regionMap1.equalsColsRows(regionMap2);
            // the bounds can differ by the rounding of the files, much less than a cell
            double boundsTolerance = Math.min(regionMap1.getXres(), regionMap1.getYres()) / 100.0;
            boolean sameBounds = Math.abs(regionMap1.getWest() - regionMap2.getWest()) < boundsTolerance
                    && Math.abs(regionMap1.getEast() - regionMap2.getEast()) < boundsTolerance
                    && Math.abs(regionMap1.getSouth() - regionMap2.getSouth()) < boundsTolerance
                    && Math.abs(regionMap1.getNorth() - regionMap2.getNorth()) < boundsTolerance;
            outSameGrid = sameColsRows && sameBounds;
            if (outSameGrid) {
                sb.append("The rasters have the same bounds and resolution.\n");
            } else {
                appendDelta(sb, "west", regionMap1.getWest(), regionMap2.getWest());
                appendDelta(sb, "east", regionMap1.getEast(), regionMap2.getEast());
                appendDelta(sb, "south", regionMap1.getSouth(), regionMap2.getSouth());
                appendDelta(sb, "north", regionMap1.getNorth(), regionMap2.getNorth());
                appendDelta(sb, "x resolution", regionMap1.getXres(), regionMap2.getXres());
                appendDelta(sb, "y resolution", regionMap1.getYres(), regionMap2.getYres());
                appendDelta(sb, "cols", regionMap1.getCols(), regionMap2.getCols());
                appendDelta(sb, "rows", regionMap1.getRows(), regionMap2.getRows());
                sb.append("The grids are different, the values are not compared.\n");
                pm.message(sb.toString());
                return;
            }

            sb.append("\nVALUES\n");
            sb.append("================================\n");
            long[] counts = new long[4]; // equal, different, novalue in one, novalue in both
            double[] differences = new double[2]; // max, sum
            StringBuilder cellsSb = new StringBuilder();
            raster1.process(pm, "Comparing values...", ( col, row, value1, cols, rows ) -> {
                double value2 = raster2.getValue(col, row);
                boolean isNovalue1 = raster1.isNovalue(value1);
                boolean isNovalue2 = raster2.isNovalue(value2);
                if (isNovalue1 && isNovalue2) {
                    counts[3]++;
                    return;
                }
                boolean isDifferent;
                if (isNovalue1 || isNovalue2) {
                    counts[2]++;
                    isDifferent = true;
                } else {
                    double difference = Math.abs(value1 - value2);
                    isDifferent = pTolerance > 0 ? difference > pTolerance : value1 != value2;
                    if (isDifferent) {
                        counts[1]++;
                        differences[0] = Math.max(differences[0], difference);
                        differences[1] += difference;
                    } else {
                        counts[0]++;
                    }
                }
                if (isDifferent && counts[1] + counts[2] <= pMaxReported) {
                    cellsSb.append("\tcol ").append(col).append(", row ").append(row).append(": ")
                            .append(isNovalue1 ? "novalue" : value1).append(" - ").append(isNovalue2 ? "novalue" : value2)
                            .append("\n");
                }
            });

            outDifferent = counts[1] + counts[2];
            outMaxDifference = differences[0];
            sb.append("Equal cells: ").append(counts[0]).append("\n");
            sb.append("Different cells: ").append(counts[1]).append("\n");
            sb.append("Cells with novalue in only one raster: ").append(counts[2]).append("\n");
            sb.append("Cells with novalue in both rasters: ").append(counts[3]).append("\n");
            if (counts[1] > 0) {
                sb.append("Max absolute difference: ").append(differences[0]).append("\n");
                sb.append("Mean absolute difference: ").append(differences[1] / counts[1]).append("\n");
            }
            if (cellsSb.length() > 0) {
                sb.append("First different cells (raster 1 - raster 2):\n").append(cellsSb);
            }
            pm.message(sb.toString());
        }
    }

    private static void appendDelta( StringBuilder sb, String name, double value1, double value2 ) {
        if (value1 != value2) {
            sb.append("\t").append(name).append(": ").append(value1).append(" - ").append(value2).append(" = ")
                    .append(value1 - value2).append("\n");
        }
    }

}
