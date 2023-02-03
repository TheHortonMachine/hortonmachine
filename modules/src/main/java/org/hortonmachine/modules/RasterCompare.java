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

import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_AUTHORCONTACTS;
import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_AUTHORNAMES;
import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_DESCRIPTION;
import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_KEYWORDS;
import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_LABEL;
import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_LICENSE;
import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_NAME;
import static org.hortonmachine.modules.RasterCompare.OMSRASTERSUMMARY_STATUS;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.RasterCellInfo;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSRASTERSUMMARY_DESCRIPTION)
@Author(name = OMSRASTERSUMMARY_AUTHORNAMES, contact = OMSRASTERSUMMARY_AUTHORCONTACTS)
@Keywords(OMSRASTERSUMMARY_KEYWORDS)
@Label(OMSRASTERSUMMARY_LABEL)
@Name("_" + OMSRASTERSUMMARY_NAME)
@Status(OMSRASTERSUMMARY_STATUS)
@License(OMSRASTERSUMMARY_LICENSE)
public class RasterCompare extends HMModel {

    @Description("Raster 1")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster1;

    @Description("Raster 2")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster2;

    @Description("Test some random pixels")
    @In
    public boolean doPixelTest = false;

    @Description("Test pixels bins")
    @In
    public int pTestPixelsBins = 10;

    public static final String OMSRASTERSUMMARY_DESCRIPTION = "Compare two rasters";
    public static final String OMSRASTERSUMMARY_KEYWORDS = "Compare, Raster";
    public static final String OMSRASTERSUMMARY_LABEL = HMConstants.RASTERPROCESSING;
    public static final String OMSRASTERSUMMARY_NAME = "rcompare";
    public static final int OMSRASTERSUMMARY_STATUS = 40;
    public static final String OMSRASTERSUMMARY_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERSUMMARY_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERSUMMARY_AUTHORCONTACTS = "http://www.hydrologis.com";

    @Execute
    public void process() throws Exception {

        HMRaster raster1 = HMRaster.fromGridCoverage(getRaster(inRaster1));
        HMRaster raster2 = HMRaster.fromGridCoverage(getRaster(inRaster2));

        RegionMap regionMap1 = raster1.getRegionMap();
        RegionMap regionMap2 = raster2.getRegionMap();

        StringBuilder sb = new StringBuilder();

        sb.append("\nGRIDGEOMETRY ANALYSIS").append("\n");
        sb.append("================================").append("\n");

        if (regionMap1.equals(regionMap2)) {
            sb.append("Rasters have same bounds and resolution.").append("\n");
        } else if (regionMap1.equalsBounds(regionMap2)) {
            sb.append("Rasters have same bounds but different resolution.").append("\n");
        } else {
            sb.append("Bounds delta:").append("\n");
            sb.append("\twest: " + regionMap1.getWest() + "-" + regionMap2.getWest() + "="
                    + (regionMap1.getWest() - regionMap2.getWest())).append("\n");
            sb.append("\teast: " + regionMap1.getEast() + "-" + regionMap2.getEast() + "="
                    + (regionMap1.getEast() - regionMap2.getEast())).append("\n");
            sb.append("\tsouth: " + regionMap1.getSouth() + "-" + regionMap2.getSouth() + "="
                    + (regionMap1.getSouth() - regionMap2.getSouth())).append("\n");
            sb.append("\tnorth: " + regionMap1.getNorth() + "-" + regionMap2.getNorth() + "="
                    + (regionMap1.getNorth() - regionMap2.getNorth())).append("\n");
        
        }

        if (!regionMap1.equalsResolution(regionMap2)) {
            sb.append("\tResolution Delta X: " + regionMap1.getXres() + "-" + regionMap2.getXres() + "="
                    + (regionMap1.getXres() - regionMap2.getXres())).append("\n");
            sb.append("\tResolution Delta Y: " + regionMap1.getYres() + "-" + regionMap2.getYres() + "="
                    + (regionMap1.getYres() - regionMap2.getYres())).append("\n");
        }
        if (!regionMap1.equalsColsRows(regionMap2)) {
            sb.append("\tCols Delta: " + regionMap1.getCols() + "-" + regionMap2.getCols() + "="
                    + (regionMap1.getCols() - regionMap2.getCols())).append("\n");
            sb.append("\tRows Delta: " + regionMap1.getRows() + "-" + regionMap2.getRows() + "="
                    + (regionMap1.getRows() - regionMap2.getRows())).append("\n");
        }

        if (doPixelTest) {
            int cols = regionMap1.getCols();
            int rows = regionMap1.getRows();

            double[] xRange = NumericsUtilities.range2Bins(0, cols, pTestPixelsBins);
            double[] yRange = NumericsUtilities.range2Bins(0, rows, pTestPixelsBins);

            sb.append("\nPIXEL ANALYSIS").append("\n");
            sb.append("================================").append("\n");
            int equalCount  = 0;
            int nonEqualCount  = 0;
            for( double y : yRange ) {
                for( double x : xRange ) {
                    if (raster1.isContained((int) x, (int) y) && raster2.isContained((int) x, (int) y)) {
                        RasterCellInfo ri = new RasterCellInfo((int) x, (int) y,
                                new GridCoverage2D[]{raster1.buildCoverage(), raster2.buildCoverage()});
                        ri.setBufferCells(2);
                        if (ri.allEqual() || (raster1.isNovalue(ri.getValues()[0]) && raster2.isNovalue(ri.getValues()[1]))) {
//                            double v = ri.getValues()[0];
//                            String vStr = String.valueOf(v);
//                            if (raster1.isNovalue(v)) {
//                                vStr = "novalue";
//                            }
//                            sb.append("VALUES IN ROW/COL = " + y + "/" + x + " are all the same: " + vStr + "\n");
                            equalCount++;
                        } else {
                            sb.append("VALUES IN ROW/COL = " + y + "/" + x + ":\n");
                            sb.append(ri.toString()).append("\n");
                            nonEqualCount++;
                        }
                    }
                }
            }
            sb.append("===========================================\n");
            sb.append("EQUAL PIXELS FOUND IN TEST: " + equalCount).append("\n");
            sb.append("NONEQUAL PIXELS FOUND IN TEST: " + nonEqualCount).append("\n");
        }

        pm.message(sb.toString());

    }

}
