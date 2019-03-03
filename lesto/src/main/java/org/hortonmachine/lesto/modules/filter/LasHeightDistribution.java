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
package org.hortonmachine.lesto.modules.filter;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasRecordGroundElevationComparator;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.filter.OmsKernelFilter;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

@Description("Module that analyzes the height distribution of a las file and categorizes the forest type.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, distribution ")
@Label(HMConstants.LESTO + "/filter")
@Name("lasheightdistribution")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasHeightDistribution extends HMModel {
    @Description("Las files folder main index file path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inIndexFile = null;

    @Description("DEM to normalize heights.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDem = null;

    @Description("Tiled region to work on.")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector = null;

    @Description("Normalization difference threshold in meters.")
    @In
    public double pThres = 2;

    @Description("Minumin percentage of overlap that defines a multilayer.")
    @In
    public double pOverlapPerc = 80.0;

    @Description("Field name for tile id.")
    @In
    public String fId = "id";

//    @Description("Optional folder to dump charts in.")
//    @UI(HMConstants.FOLDEROUT_UI_HINT)
//    @In
//    public String outChartsFolder = null;

    @Description("The output raster of forest categories.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outCats = null;

    private boolean doChart = false;

    private File outChartsFolderFile;

    @Execute
    public void process() throws Exception {
        checkNull(inIndexFile, inVector, inDem);

//        if (outChartsFolder != null) {
//            outChartsFolderFile = new File(outChartsFolder);
//            if (outChartsFolderFile.exists()) {
//                doChart = true;
//            }
//        }

        double percentageOverlap = pOverlapPerc / 100.0;
        File indexFile = new File(inIndexFile);

        SimpleFeatureCollection tilesFC = getVector(inVector);
        List<FeatureMate> tilesMates = FeatureUtilities.featureCollectionToMatesList(tilesFC);

        GridCoverage2D inDemGC = getRaster(inDem);
        CoordinateReferenceSystem crs = inDemGC.getCoordinateReferenceSystem();

        WritableRaster[] finalCoverageWRH = new WritableRaster[1];
        GridCoverage2D outCatsGC = CoverageUtilities.createCoverageFromTemplate(inDemGC, HMConstants.doubleNovalue,
                finalCoverageWRH);
        WritableRandomIter finalIter = CoverageUtilities.getWritableRandomIterator(finalCoverageWRH[0]);

        try (ALasDataManager dataManager = ALasDataManager.getDataManager(indexFile, inDemGC, pThres, null)) {
            dataManager.open();

            for( int i = 0; i < tilesMates.size(); i++ ) {
                pm.message("Processing tile: " + i + "/" + tilesMates.size());
                FeatureMate tileMate = tilesMates.get(i);
                String id = tileMate.getAttribute(fId, String.class);
                Geometry tileGeom = tileMate.getGeometry();

                Envelope geomEnvelope = tileGeom.getEnvelopeInternal();
                ReferencedEnvelope refEnvelope = new ReferencedEnvelope(geomEnvelope, crs);
                Envelope2D tileEnvelope = new Envelope2D(refEnvelope);
                WritableRaster[] tmpWrH = new WritableRaster[1];
                GridCoverage2D tmp = CoverageUtilities.createSubCoverageFromTemplate(inDemGC, tileEnvelope, doubleNovalue,
                        tmpWrH);
                RegionMap tileRegionMap = CoverageUtilities.getRegionParamsFromGridCoverage(tmp);
                GridGeometry2D tileGridGeometry = tmp.getGridGeometry();

                List<LasRecord> pointsListForTile = dataManager.getPointsInGeometry(tileGeom, true);
                // do something with the data

                if (pointsListForTile.size() == 0) {
                    pm.errorMessage("No points found in tile: " + id);
                    continue;
                }
                if (pointsListForTile.size() < 2) {
                    pm.errorMessage("Not enough points found in tile: " + id);
                    continue;
                }
                List<double[]> negativeRanges = analyseNegativeLayerRanges(id, pointsListForTile);
                List<GridCoverage2D> rangeCoverages = new ArrayList<GridCoverage2D>();

                for( double[] range : negativeRanges ) {
                    List<LasRecord> pointsInVerticalRange = ALasDataManager.getPointsInVerticalRange(pointsListForTile, range[0],
                            range[1], true);

                    WritableRaster[] wrH = new WritableRaster[1];
                    GridCoverage2D tmpCoverage = CoverageUtilities.createSubCoverageFromTemplate(inDemGC, tileEnvelope,
                            doubleNovalue, wrH);
                    rangeCoverages.add(tmpCoverage);

                    WritableRandomIter tmpIter = CoverageUtilities.getWritableRandomIterator(wrH[0]);

                    final DirectPosition2D wp = new DirectPosition2D();
                    for( LasRecord lasRecord : pointsInVerticalRange ) {
                        wp.setLocation(lasRecord.x, lasRecord.y);
                        GridCoordinates2D gp = tileGridGeometry.worldToGrid(wp);
                        double count = tmpIter.getSampleDouble(gp.x, gp.y, 0);
                        if (isNovalue(count)) {
                            count = 0;
                        }
                        tmpIter.setSample(gp.x, gp.y, 0, count + 1);
                    }

                    tmpIter.done();

                }

                /*
                 * categorize in non forest/single/double layer
                 */
                /*
                 * 1) check if the multiple layers are double or
                 * single at variable heights. 
                 */
                boolean isDoubleLayered = false;
                if (rangeCoverages.size() > 1) {
                    for( int j = 0; j < rangeCoverages.size() - 1; j++ ) {
                        GridCoverage2D cov1 = rangeCoverages.get(j);
                        GridCoverage2D cov2 = rangeCoverages.get(j + 1);
                        if (overlapForPercentage(cov1, cov2, percentageOverlap)) {
                            isDoubleLayered = true;
                            break;
                        }
                    }
                }
                /*
                 * 2) define each pixel of the end map
                 * - 0 = no forest
                 * - 1 = single layer
                 * - 2 = single with variable height
                 * - 3 = double layer
                 */
                GridGeometry2D gridGeometry = outCatsGC.getGridGeometry();
                // RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(outCats);
                double[] gridValue = new double[1];
                int cols = tileRegionMap.getCols();
                int rows = tileRegionMap.getRows();
                for( int r = 0; r < rows; r++ ) {
                    for( int c = 0; c < cols; c++ ) {
                        int value = 0;
                        GridCoordinates2D gridPosition = new GridCoordinates2D(c, r);
                        for( int j = 0; j < rangeCoverages.size(); j++ ) {
                            GridCoverage2D cov = rangeCoverages.get(j);
                            cov.evaluate(gridPosition, gridValue);

                            if (!isNovalue(gridValue[0])) {
                                value++;
                            }
                        }

                        // set final value in the grid
                        if (value > 1) {
                            // multilayer
                            if (isDoubleLayered) {
                                value = 3;
                            } else {
                                value = 2;
                            }
                        }
                        DirectPosition worldPosition = tileGridGeometry.gridToWorld(gridPosition);
                        GridCoordinates2D worldPositionCats = gridGeometry.worldToGrid(worldPosition);
                        finalIter.setSample(worldPositionCats.x, worldPositionCats.y, 0, value);
                    }
                }
            }

        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(outCatsGC);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = finalIter.getSampleDouble(c, r, 0);
                if (isNovalue(value)) {
                    finalIter.setSample(c, r, 0, 0.0);
                }
            }
        }
        finalIter.done();

        dumpRaster(outCatsGC, outCats);

    }

    private boolean overlapForPercentage( GridCoverage2D cov1, GridCoverage2D cov2, double forPercentage ) {
        RandomIter cov1Iter = CoverageUtilities.getRandomIterator(cov1);
        RandomIter cov2Iter = CoverageUtilities.getRandomIterator(cov2);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(cov1);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        int valid1 = 0;
        int valid2 = 0;
        int overlapping = 0;

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double v1 = cov1Iter.getSampleDouble(c, r, 0);
                double v2 = cov2Iter.getSampleDouble(c, r, 0);

                if (!isNovalue(v1)) {
                    valid1++;
                }
                if (!isNovalue(v2)) {
                    valid2++;
                }
                if (!isNovalue(v1) && !isNovalue(v2)) {
                    overlapping++;
                }

            }
        }

        cov1Iter.done();
        cov2Iter.done();

        if (overlapping == 0) {
            return false;
        }
        double perc1 = overlapping / valid1;
        if (perc1 > forPercentage) {
            return true;
        }
        double perc2 = overlapping / valid2;
        if (perc2 > forPercentage) {
            return true;
        }
        return false;
    }

    private List<double[]> analyseNegativeLayerRanges( String id, List<LasRecord> pointsList ) throws Exception {
        Collections.sort(pointsList, new LasRecordGroundElevationComparator());

        double[] pointsArray = new double[pointsList.size()];
        for( int i = 0; i < pointsArray.length; i++ ) {
            LasRecord lasRecord = pointsList.get(i);
            pointsArray[i] = lasRecord.groundElevation;
        }

        double binSize = 0.5;
        double[][] bins = toBins(pointsArray, binSize);
        double[] elevationsArray = bins[0];
        double[] countsArray = bins[1];

        int gaussian = 12;
        // apply padding
        double[] paddedCountsArray = doPadding(countsArray, gaussian);
        // smooth
        double[] paddedGaussianSmoothedValues = OmsKernelFilter.gaussianSmooth(paddedCountsArray, gaussian);
        // remove paddings again
        double[] gaussianSmoothedValues = new double[countsArray.length];
        for( int i = 0; i < gaussianSmoothedValues.length; i++ ) {
            gaussianSmoothedValues[i] = paddedGaussianSmoothedValues[gaussian + i];
        }

        double[] deriv2 = new double[gaussianSmoothedValues.length];
        deriv2[0] = 0;
        deriv2[deriv2.length - 1] = 0;

        for( int i = 1; i < bins[0].length - 1; i++ ) {
            double elevM1 = bins[0][i - 1];
            double elev = bins[0][i];
            // double elevP1 = bins[0][i + 1];
            double hM1 = gaussianSmoothedValues[i - 1];
            double h = gaussianSmoothedValues[i];
            double hP1 = gaussianSmoothedValues[i + 1];

            double delev = elev - elevM1;
            double derivata2 = (hP1 - 2 * h + hM1) / (delev * delev);
            deriv2[i] = derivata2;
        }

        doChart(id, bins, gaussianSmoothedValues, deriv2);

        List<int[]> negativeRangesIndexes = NumericsUtilities.getNegativeRanges(deriv2);
        List<double[]> negativeRanges = new ArrayList<double[]>();
        for( int[] index : negativeRangesIndexes ) {
            negativeRanges.add(new double[]{elevationsArray[index[0]], elevationsArray[index[1]]});
        }

        return negativeRanges;
    }

    private void doChart( String id, double[][] bins, double[] gaussianSmoothedValues, double[] deriv2 ) throws Exception {
//        if (doChart) {
//            double[][] data = new double[bins[0].length][4];
//            for( int i = 0; i < bins[0].length; i++ ) {
//                data[i][0] = bins[0][i];
//                data[i][1] = bins[1][i];
//                data[i][2] = gaussianSmoothedValues[i];
//                data[i][3] = deriv2[i];
//            }
//
//            File chartFile = new File(outChartsFolderFile, "chart_" + id + ".png");
//
//            OmsMatrixCharter charter = new OmsMatrixCharter();
//            charter.doChart = false;
//            charter.doDump = true;
//            charter.doLegend = false;
//            charter.doHorizontal = true;
//            charter.pHeight = 600;
//            charter.pWidth = 300;
//            charter.pType = 0;
//            charter.inData = data;
//            charter.inTitle = "Height distribution id = " + id;
//            charter.inSubTitle = "";
//            charter.inChartPath = chartFile.getAbsolutePath();
//
//            String[] labels = {"height [m]", "number of points"};
//            String[] series = {"original distribution", "gaussian smoothed", "second derivative"};
//            // String[] series = {"second derivative"};
//            charter.inLabels = labels;
//            charter.inSeries = series;
//            charter.inColors = "255,0,0;0,0,255;0,0,0";
//            charter.chart();
//        }
    }

    private double[] doPadding( double[] countsArray, int gaussian ) {
        /*
         * do some padding to help the smoothing by mirroring around 0
         */
        // double[] paddedCountsArray = new double[countsArray.length + 2 * gaussian];
        // for( int i = 0; i < paddedCountsArray.length; i++ ) {
        // if (i < gaussian) {
        // paddedCountsArray[i] = -countsArray[gaussian - i];
        // } else if (i >= gaussian && i < paddedCountsArray.length - gaussian) {
        // paddedCountsArray[i] = countsArray[i - gaussian];
        // } else {
        // paddedCountsArray[i] = -countsArray[i - (paddedCountsArray.length - gaussian)];
        // }
        // }

        /*
         * do some padding with 0 to help the smoothing 
         */
        double[] paddedCountsArray = new double[countsArray.length + 2 * gaussian];
        for( int i = 0; i < paddedCountsArray.length; i++ ) {
            if (i < gaussian) {
                paddedCountsArray[i] = 0;
            } else if (i >= gaussian && i < paddedCountsArray.length - gaussian) {
                paddedCountsArray[i] = countsArray[i - gaussian];
            } else {
                paddedCountsArray[i] = 0;
            }
        }
        /*
         * do some padding with the exterior values to help the smoothing 
         */
        // double[] paddedCountsArray = new double[countsArray.length + 2 * gaussian];
        // for( int i = 0; i < paddedCountsArray.length; i++ ) {
        // if (i < gaussian) {
        // paddedCountsArray[i] = countsArray[0];
        // } else if (i >= gaussian && i < paddedCountsArray.length - gaussian) {
        // paddedCountsArray[i] = countsArray[i - gaussian];
        // } else {
        // paddedCountsArray[i] = countsArray[countsArray.length - 1];
        // }
        // }
        return paddedCountsArray;
    }

    public static double[][] toBins( double[] values, double binSize ) {
        double min = values[0];
        double max = values[values.length - 1];
        int num = (int) Math.ceil((max - min) / binSize);
        if (num == 0) {
            num = 1;
        }
        double from = min;
        double to = min + binSize;
        double[][] result = new double[2][num];
        for( int i = 0; i < num; i++ ) {
            int count = 0;
            for( int j = 0; j < values.length; j++ ) {
                if (values[j] >= from && values[j] < to) {
                    count++;
                }
            }
            double centerValue = from + binSize / 2.0;
            result[0][i] = centerValue;
            result[1][i] = count;

            from = from + binSize;
            to = to + binSize;
        }
        return result;
    }

}
