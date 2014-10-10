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
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.MapCalculationFunctions.MapOfCumulatedValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class MapDumpingFunctions {

    public static void dumpMapsOfPsiMultiHours( GridCoverage2D mapsPsiMultiHours, int y, int h, String pOutFolder, IJGTProgressMonitor pm  ) {

        pOutFolder = fixOutputFolderString(pOutFolder);
        //String tempDir = System.getProperty("java.io.tmpdir");
        if(pm != null){pm.message("Dumping Psi maps to " + pOutFolder);}
        
        GridCoverage2D[] outHourlyPsiMapArray = MapCalculationFunctions.fromMultibandCoverageToCoverageArray(mapsPsiMultiHours);
        
        for( int i = 0; i < outHourlyPsiMapArray.length; i++ ) {

            try {
                OmsRasterWriter.writeRaster(pOutFolder + "Psi_" + y + "y_" + h + "h_" + (i + 1) + "hsim.asc", outHourlyPsiMapArray[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    
    public static void dumpMapsOfTwt( HashMap<Integer[], GridCoverage2D> mapsHashMap, String pOutFolder, IJGTProgressMonitor pm ) {

        pOutFolder = fixOutputFolderString(pOutFolder);
        
        for( Integer[] keyArr : mapsHashMap.keySet() ) {

            if(pm != null){pm.message("Dumping Twt maps to " + pOutFolder);}
            try {
                OmsRasterWriter.writeRaster(pOutFolder + "Twt_" + keyArr[1] + "y_" + keyArr[0] + "h.asc", mapsHashMap.get(keyArr));
                pm.message("Stats for Twt map " + keyArr[1] + "y_" + keyArr[0] + " " + MapCalculationFunctions.getCoverageStatsAsString(mapsHashMap.get(keyArr)));
            } catch (Exception e) {
                if(pm != null){pm.errorMessage("An error was encounterd while dumping Twt maps to file in dir " + pOutFolder);}
                e.printStackTrace();
            }

        }

    }
    
    public static void dumpMapsOfPsiMultiHoursToTempDir( GridCoverage2D[] mapsPsiMultiHours, String pOutFolder, IJGTProgressMonitor pm ) {

        pOutFolder = fixOutputFolderString(pOutFolder);
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(mapsPsiMultiHours[0]);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps

        int maps = mapsPsiMultiHours.length;

        WritableRaster psiHourlyMapWR;
        WritableRandomIter psiHourlyMapIter;
        RandomIter mapPsiInitIter;
        for( int i = 0; i < maps; i++ ) {

            mapPsiInitIter = RandomIterFactory.create(mapsPsiMultiHours[i].getRenderedImage(), null);
            psiHourlyMapWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
            psiHourlyMapIter = RandomIterFactory.createWritable(psiHourlyMapWR, null);

            if(pm != null){pm.beginTask("Starting to copy Coverages into Rasters...", rows);}
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = mapPsiInitIter.getSample(c, r, 0);
                    if (!Double.isNaN(value))
                        psiHourlyMapIter.setSample(c, r, 0, value);
                }
            }

            if(pm != null){pm.message("Dumping Psi map to " + pOutFolder);}

            GridCoverage2D outHourlyPsiMap = CoverageUtilities.buildCoverage("psi", psiHourlyMapWR, regionMap,
                    mapsPsiMultiHours[0].getCoordinateReferenceSystem());
            try {
                OmsRasterWriter.writeRaster(pOutFolder + "Psi_" + (i + 1) + "h.asc", outHourlyPsiMap);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Takes a GridCoverage2d with several bands and dumps each one as a single ESRI Ascii Grid
     * inside the provided the destination folder.
     * @param multiband_Coverage
     * @param pOutFolder The string path of the destination folder. MUST be provided linux-style using forward slashes "/" also in WINDOWS and MUST end with a backslash.
     * @param pm The IJGTProgressMonitor to notify of the task. Can be null.
     * @param message The custom message to display while dumping. Ignored if 'pmi is null
     */
    public static void dumpMultiBandCoverage( GridCoverage2D multiband_Coverage,
            String pOutFolder, IJGTProgressMonitor pm, String message ) {
        
        if(pm != null){pm.message(message + pOutFolder);}
        
        pOutFolder = fixOutputFolderString(pOutFolder);
        
        int availableThreads = Runtime.getRuntime().availableProcessors();
        
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(availableThreads);
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(multiband_Coverage);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        
        CoordinateReferenceSystem crs = multiband_Coverage.getCoordinateReferenceSystem();

        int bands = multiband_Coverage.getNumSampleDimensions();

        for( int band = 0; band < bands; band++ ) {

            RandomIter multiBandMapIter = RandomIterFactory.create(multiband_Coverage.getRenderedImage(), null);
            
            String filePrefix = multiband_Coverage.getName().toString();
            
            IRunBehavior rb = new RunMultiBandFileDumper(band, cols, rows, filePrefix, pOutFolder, multiBandMapIter, regionMap, crs, pm);
            
            fixedThreadPool.execute(new FileDumperRunner(rb));
        }
        
        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }
    

    /**
     * Takes a GridCoverage2d with several bands and dumps the requested one to a single ESRI Ascii Grid
     * inside the provided the destination folder.
     * @param multiband_Coverage
     * @param bandToDump int The index of the band to dump: 24th band has index 23.
     * @param pOutFolder String The string path of the destination folder. MUST be provided linux-style using forward slashes "/" also in WINDOWS and MUST end with a backslash.
     * @param pm The IJGTProgressMonitor to notify of the task. Can be null.
     * @param message String The custom message to display while dumping. Ignored if 'pmi is null
     */
    public static void dumpMultiBandCoverage( GridCoverage2D multiband_Coverage, int bandToDump, String pOutFolder,
            IJGTProgressMonitor pm, String message ) {

        if(pm != null){pm.message(message + pOutFolder);}
        
        pOutFolder = fixOutputFolderString(pOutFolder);
        
        int availableThreads = Runtime.getRuntime().availableProcessors();
        
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(availableThreads);
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(multiband_Coverage);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        
        CoordinateReferenceSystem crs = multiband_Coverage.getCoordinateReferenceSystem();

        int bands = multiband_Coverage.getNumSampleDimensions();

        if( bandToDump < bands ) {

            RandomIter multiBandMapIter = RandomIterFactory.create(multiband_Coverage.getRenderedImage(), null);
            
            String filePrefix = multiband_Coverage.getName().toString();
            
            IRunBehavior rb = new RunMultiBandFileDumper(bandToDump, cols, rows, filePrefix, pOutFolder, multiBandMapIter, regionMap, crs, pm);
            
            fixedThreadPool.execute(new FileDumperRunner(rb));
            
        } else {
            
            if(pm != null){pm.errorMessage("Could not dump band to file: the requested band does not exist in the provided Coverage!");}
             
        }
        
        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * Wrapper for dumpSingleBandCoverage providing a default message
     * @param coverage
     * @param pOutFolder
     * @param pm
     */
    public static void dumpSingleBandCoverage( GridCoverage2D coverage, String pOutFolder, IJGTProgressMonitor pm  ) {
    	
    	String message = "Dumping map to ";
    	dumpSingleBandCoverage(coverage, pOutFolder, pm, message);
    }

    /**
     * Takes a single banded GridCoverage2d and dumps it to a single ESRI Ascii grid inside the provided
     * destination folder. 
     * @param coverage
     * @param pOutFolder String The string path of the destination folder. MUST be provided linux-style using forward slashes "/" also in WINDOWS and MUST end with a backslash.
     * @param pm The IJGTProgressMonitor to notify of the task. Can be null.
     * @param message 
     */
    public static void dumpSingleBandCoverage( GridCoverage2D coverage, String pOutFolder, IJGTProgressMonitor pm, String message  ) {
        
        if(pm != null){pm.message(message + pOutFolder);}
        pOutFolder = fixOutputFolderString(pOutFolder);
        CharSequence name = coverage.getName();
        try {
            OmsRasterWriter.writeRaster(pOutFolder+name+".asc", coverage);
        } catch (Exception e) {
            if(pm != null){pm.errorMessage("Problems encountered dumping map " + name + " to " + pOutFolder +"! File was not saved.");}
            e.printStackTrace();
        }
        
    }

    /**
     *  
     * @param mapsOfCumulatedParameters HashMap<MapOfCumulatedValue, GridCoverage2D>
     * @param pOutFolder String The string path of the destination folder. MUST be provided linux-style using forward slashes "/" also in WINDOWS and MUST end with a backslash.
     * @param pm
     */
    public static void dumpCumulatedParametersToFiles( HashMap<MapOfCumulatedValue, GridCoverage2D> mapsOfCumulatedParameters, String pOutFolder, IJGTProgressMonitor pm  ) {
              
        if(pm != null){pm.message("Dumping cumulated parameters maps to " + pOutFolder);}
        
        pOutFolder = fixOutputFolderString(pOutFolder);
        
        int availableThreads = Runtime.getRuntime().availableProcessors();
        
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(availableThreads);
        
        for(Enum e : MapCalculationFunctions.MapOfCumulatedValue.values()){
            
            GridCoverage2D coverage = mapsOfCumulatedParameters.get(e);
            String path = pOutFolder+e.toString()+".asc";
            
            IRunBehavior rb = new RunBasicFileDumper(path, coverage);
            
            fixedThreadPool.execute(new FileDumperRunner(rb));
                     
        }
        
        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes a string representing a folder path and converts it to linux-stile by
     * replacing backslash with forward slashes and adding a trailing forward slash if missing.
     * @param String pOutFolder
     * @return String a path using forward slashes and ending with a "/"
     */
    public static String fixOutputFolderString(String pOutFolder){
    	
    	pOutFolder = pOutFolder.replace("\\", "/");
    	
    	if(!pOutFolder.endsWith("/")){
    		pOutFolder += "/";
    	}
    
    	return pOutFolder;
    }
    
    
}
