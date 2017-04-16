/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RunMultiBandFileDumper implements IRunBehavior {
    
    private int band;
    private String pOutFolder = null;
    private String filePrefix = null;
    private RandomIter multiBandMapIter = null;
    private int cols;
    private int rows;
    private IJGTProgressMonitor pm;
    private RegionMap regionMap;
    private CoordinateReferenceSystem crs;

    public RunMultiBandFileDumper(int band, int cols, int rows, String filePrefix, String pOutFolder, RandomIter multiBandMapIter, RegionMap regionMap, CoordinateReferenceSystem crs, IJGTProgressMonitor pm){
        this.band = band;
        this.cols = cols;
        this.rows = rows;
        this.pm = pm;
        this.pOutFolder = pOutFolder;
        this.filePrefix = filePrefix;
        this.multiBandMapIter = multiBandMapIter;
        this.regionMap = regionMap;
        this.crs = crs;
    }
    
    @Override
    public void runLaunch() {
        
        WritableRaster outputBand_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter outputBand_Iter = RandomIterFactory.createWritable(outputBand_WR, null);

        if(pm != null){pm.beginTask("Starting to copy Coverages into Rasters...", rows);}
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = multiBandMapIter.getSampleDouble(c, r, band);
                if (!Double.isNaN(value))
                    outputBand_Iter.setSample(c, r, 0, value);
            }
        }               
        
        GridCoverage2D outBand = CoverageUtilities.buildCoverage(filePrefix + (band + 1) + "h", outputBand_WR, regionMap, crs);
        
        try {
            OmsRasterWriter.writeRaster(pOutFolder + outBand.getName() + ".asc", outBand);
        } catch (Exception e) {
            e.printStackTrace();
        }            
    } 
}
