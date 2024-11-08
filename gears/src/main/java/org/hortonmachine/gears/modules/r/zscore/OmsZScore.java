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
package org.hortonmachine.gears.modules.r.zscore;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_KEYWORDS;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_LABEL;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_LICENSE;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_NAME;
import static org.hortonmachine.gears.modules.r.zscore.OmsZScore.OMSZSCORE_STATUS;

import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

@Description(OMSZSCORE_DESCRIPTION)
@Documentation(OMSZSCORE_DOCUMENTATION)
@Author(name = OMSZSCORE_AUTHORNAMES, contact = OMSZSCORE_AUTHORCONTACTS)
@Keywords(OMSZSCORE_KEYWORDS)
@Label(OMSZSCORE_LABEL)
@Name(OMSZSCORE_NAME)
@Status(OMSZSCORE_STATUS)
@License(OMSZSCORE_LICENSE)
public class OmsZScore extends HMModel {

    public static final String OMSZSCORE_DESCRIPTION = "Module to do zscore analysis on defined windows.";
    public static final String OMSZSCORE_DOCUMENTATION = "";
    public static final String OMSZSCORE_KEYWORDS = "IO, Coverage, Raster, ZScore, Interpolation";
    public static final String OMSZSCORE_LABEL = RASTERPROCESSING;
    public static final String OMSZSCORE_NAME = "zscore";
    public static final int OMSZSCORE_STATUS = 5;
    public static final String OMSZSCORE_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSZSCORE_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSZSCORE_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSZSCORE_IN_GEODATA_DESCRIPTION = "The input coverage.";
    public static final String OMSZSCORE_P_WINSIZE_DESCRIPTION = "The windows size in cells.";
    public static final String OMSZSCORE_P_THRESHOLD_DESCRIPTION = "The score threshold.";
    public static final String OMSZSCORE_DO_HOLES_DESCRIPTION = "Make holes in the original map.";
    public static final String OMSZSCORE_OUT_GEODATA_DESCRIPTION = "The output coverage.";

    @Description(OMSZSCORE_IN_GEODATA_DESCRIPTION)
    @In
    public GridCoverage2D inGeodata;

    @Description(OMSZSCORE_P_WINSIZE_DESCRIPTION)
    @In
    public int pSize = 15;

    @Description(OMSZSCORE_P_THRESHOLD_DESCRIPTION)
    @In
    public double pThreshold = -2;

    @Description(OMSZSCORE_P_THRESHOLD_DESCRIPTION)
    @In
    public boolean doHoles = false;

    @Description(OMSZSCORE_OUT_GEODATA_DESCRIPTION)
    @Out
    public GridCoverage2D outGeodata;

    @Execute
    public void process() throws Exception {
        checkNull(inGeodata);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inGeodata);

        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        HMRaster inRaster = HMRaster.fromGridCoverage(inGeodata);
        HMRaster outRaster;
        if (doHoles) {
            outRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inGeodata).setCopyValues(true).build();
        } else {
            outRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inGeodata).setInitialValue(0.0).build();
        }

//        inRaster.processByRow(pm, "Processing...", (row, ncols,  nrows) -> {
//            for( int c = 0; c < cols; c++ ) {
//                GridNode cell = new GridNode(inRaster, c, row);
//                if (cell.isValid()) {
//                    List<GridNode> windowNodes = cell.getWindow(pSize);
//                    double[] neighbors = windowNodes.stream().filter(n -> n.isValid()).map(n -> n.elevation)
//                            .mapToDouble(Double::doubleValue).toArray();
//                    double zScore = calculateZScore(cell.elevation, neighbors);
//
//                    if (zScore > pThreshold) {
//                        zScore = 0;
//                    }
//                    outRaster.setValue(c, row, zScore);
//                }
//            }
//        }, true);

        double novalue = inRaster.getNovalue();
        pm.beginTask("Processing...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                GridNode cell = new GridNode(inRaster, c, r);
                if (cell.isValid()) {

                    List<GridNode> windowNodes = cell.getWindow(pSize);
                    double[] neighbors = windowNodes.stream().filter(n -> n.isValid()).map(n -> n.elevation)
                            .mapToDouble(Double::doubleValue).toArray();
                    double zScore = calculateZScore(cell.elevation, neighbors);

                    if (zScore > pThreshold) {
                        zScore = 0;
                    }
                    if(doHoles) {
                        if(zScore != 0.0) {
                            outRaster.setValue(c, r, novalue);
                        }
                    } else {
                        outRaster.setValue(c, r, zScore);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        outGeodata = outRaster.buildCoverage();

    }

    // Function to calculate the mean of an array of values
    public static double mean( double[] values ) {
        double sum = 0.0;
        for( double value : values ) {
            sum += value;
        }
        return sum / values.length;
    }

    // Function to calculate the standard deviation of an array of values
    public static double standardDeviation( double[] values, double mean ) {
        double sum = 0.0;
        for( double value : values ) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum / values.length);
    }

    // Function to calculate the Z-score for the central pixel in a window
    public static double calculateZScore( double centerValue, double[] neighborhood ) {
        double mean = mean(neighborhood);
        double stdDev = standardDeviation(neighborhood, mean);

        // Avoid division by zero in case of constant neighborhood values
        if (stdDev == 0) {
            return 0;
        }

        return (centerValue - mean) / stdDev;
    }


    public static void main( String[] args ) throws Exception {
        OmsZScore z = new OmsZScore();
        z.inGeodata = OmsRasterReader
                .readRaster("/storage/lavori_tmp/GEOLOGICO_TN/20240318_dati_per_Silvia/1_errori_dtm_dbm_fiumi/fiume_sarca.tif");
        z.pSize = 20;
        z.pThreshold = -1.5;
        z.doHoles = true;
        z.process();
        GridCoverage2D outGC = z.outGeodata;
        OmsRasterWriter.writeRaster("/storage/lavori_tmp/GEOLOGICO_TN/20240318_dati_per_Silvia/1_errori_dtm_dbm_fiumi/zscore_holes_1.5.tif",
                outGC);
        
        
    }

}
