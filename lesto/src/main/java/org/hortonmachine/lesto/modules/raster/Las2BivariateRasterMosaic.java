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
package org.hortonmachine.lesto.modules.raster;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.Point;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.WritableRandomIter;

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
import oms3.annotations.Unit;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RRQRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ThreadedRunnable;
import org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

@Description("Module that converts a las data into a set of mosaic rasters using a bivariate function.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, lidar, raster, bivariate, mosaic")
@Label(HMConstants.LESTO + "/raster")
@Name("lasfolder2bivariaterastermosaic")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class Las2BivariateRasterMosaic extends HMModel {

    @Description("Las files folder main index file path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description("A region of interest. If not supplied the whole dataset is used.")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inRoi;

    @Description("The tilesize of the subrasters.")
    @Unit("m")
    @In
    public double pTilesize = 5000;

    @Description("New resolution used for the tiles.")
    @Unit("m")
    @In
    public double pRes = 1;

    @Description("Buffer of influence for points interpolation in number of cells.")
    @In
    public int pBuffer = 2;

    @Description("The impulse to use (if empty everything is used).")
    @In
    public Integer pImpulse = 1;

    @Description("Number of threads to use.")
    @In
    public int pThreads = 1;

    @Description("If true, intensity is used instead of elevation.")
    @In
    public boolean doIntensity = false;

    @Description("Minimum number of points to consider the resulting cell valid.")
    @In
    public int pMinpoints = 6;

    @Description("The output folder.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFolder = null;

    private static final double NOINTENSITY = -9999.0;
    private volatile double minValue = Double.POSITIVE_INFINITY;
    private volatile double maxValue = Double.NEGATIVE_INFINITY;

    private CoordinateReferenceSystem crs;

    @SuppressWarnings("rawtypes")
    @Execute
    public void process() throws Exception {
        checkNull(inLas, outFolder);

        final File outFolderFile = new File(outFolder);

        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLas), null, 0, null)) {
            lasData.open();
            if (pImpulse != null) {
                lasData.setImpulsesConstraint(new double[]{pImpulse});
            }

            ReferencedEnvelope roiEnvelope = lasData.getOverallEnvelope();
            crs = roiEnvelope.getCoordinateReferenceSystem();
            if (crs == null) {
                if (inRoi == null) {
                    throw new ModelsIllegalargumentException("The lasfolder file needs to have a prj definition.", this);
                }
            }
            if (inRoi != null) {
                SimpleFeatureCollection inRoiFC = getVector(inRoi);
                roiEnvelope = inRoiFC.getBounds();
            }

            double overallW = roiEnvelope.getMinX();
            double overallE = roiEnvelope.getMaxX();
            double overallS = roiEnvelope.getMinY();
            double overallN = roiEnvelope.getMaxY();

            double[] xBins = NumericsUtilities.range2Bins(overallW, overallE, pTilesize, true);
            double[] yBins = NumericsUtilities.range2Bins(overallS, overallN, pTilesize, true);

            int tilesCols = xBins.length - 1;
            int tilesRows = yBins.length - 1;
            pm.message("Splitting into tiles: " + tilesCols + " x " + tilesRows);
            pm.beginTask("Interpolating tiles...", tilesCols * tilesRows);
            ThreadedRunnable< ? > runnable = null;
            if (pThreads > 1) {
                runnable = new ThreadedRunnable(pThreads, pm);
            }
            for( int x = 0; x < xBins.length - 1; x++ ) {
                for( int y = 0; y < yBins.length - 1; y++ ) {
                    final double w = xBins[x];
                    final double e = xBins[x + 1];
                    final double s = yBins[y];
                    final double n = yBins[y + 1];
                    final int _x = x;
                    final int _y = y;
                    if (runnable != null) {
                        runnable.executeRunnable(new Runnable(){
                            public void run() {
                                try {
                                    processTile(outFolderFile, crs, lasData, _x, _y, w, e, s, n);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                pm.worked(1);
                            }
                        });
                    } else {
                        try {
                            processTile(outFolderFile, crs, lasData, _x, _y, w, e, s, n);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        pm.worked(1);
                    }
                }
            }
            if (runnable != null) {
                runnable.waitAndClose();
            }
            pm.done();

        }

        OmsImageMosaicCreator im = new OmsImageMosaicCreator();
        im.inFolder = outFolder;
        im.process();

        String name = outFolderFile.getName();
        String style = RasterStyleUtilities.styleToString(
                RasterStyleUtilities.createStyleForColortable(EColorTables.extrainbow.name(), minValue, maxValue, null, 1.0));
        File styleFile = new File(outFolderFile, name + ".sld");
        FileUtilities.writeFile(style, styleFile);

    }

    @SuppressWarnings({"unchecked"})
    private void processTile( File outFolderFile, CoordinateReferenceSystem crs, ALasDataManager lasData, int x, int y, double w,
            double e, double s, double n ) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("tile_");
        sb.append(x);
        sb.append("_");
        sb.append(y);
        final String tileName = sb.toString();
        sb.append(".tiff");
        File outTileFile = new File(outFolderFile, sb.toString());
        if (outTileFile.exists()) {
            pm.errorMessage("Not overwriting existing tile: " + outTileFile.getName());
            return;
        }

        final int cols = (int) round((e - w) / pRes);
        final int rows = (int) round((n - s) / pRes);
        double xRes = (e - w) / cols;
        double yRes = (n - s) / rows;
        RegionMap regionMap = CoverageUtilities.makeRegionParamsMap(n, s, w, e, xRes, yRes, cols, rows);
        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        GridCoverage2D outputCoverage = CoverageUtilities.buildCoverage("data", outWR, regionMap, crs);

        // pm.message("Reading laspoints for: " + tileName);
        Envelope env = new Envelope(w, e, s, n);
        /*
         * enlarge by buffer value, to solve border issues 
         * 
         * -> outside points are read to aid interpolation at borders
         * -> this forces to keep two gridgeometries active, the 
         *    resulting coverage + the buffered one
         */
        double deltaX = xRes * pBuffer;
        double deltaY = yRes * pBuffer;
        env.expandBy(deltaX, deltaY);
        Polygon roiPolygon = GeometryUtilities.createPolygonFromEnvelope(env);
        List<LasRecord> tileLasPoints = lasData.getPointsInGeometry(roiPolygon, true);

        if (tileLasPoints.size() > 100) {
            final GridGeometry2D gridGeometry = outputCoverage.getGridGeometry();

            int newCols = cols + 2 * pBuffer;
            int newRows = rows + 2 * pBuffer;
            GridGeometry2D bufferedGridGeometry = CoverageUtilities.gridGeometryFromRegionValues(env.getMaxY(), env.getMinY(),
                    env.getMaxX(), env.getMinX(), newCols, newRows, crs);

            ArrayList<LasRecord>[][] lasMatrix = new ArrayList[newCols][newRows];
            for( int r = 0; r < newRows; r++ ) {
                for( int c = 0; c < newCols; c++ ) {
                    ArrayList<LasRecord> item = new ArrayList<LasRecord>();
                    lasMatrix[c][r] = item;
                }
            }

            // Splitting las into cells
            final Point point = new Point();
            for( LasRecord dot : tileLasPoints ) {
                if (doIntensity) {
                    if (dot.intensity == NOINTENSITY) {
                        continue;
                    }
                    minValue = min(dot.intensity, minValue);
                    maxValue = max(dot.intensity, maxValue);
                } else {
                    minValue = min(dot.z, minValue);
                    maxValue = max(dot.z, maxValue);
                }
                CoverageUtilities.colRowFromCoordinate(new Coordinate(dot.x, dot.y), bufferedGridGeometry, point);
                lasMatrix[point.x][point.y].add(dot);
            }

            WritableRandomIter outWIter = null;
            try {
                outWIter = CoverageUtilities.getWritableRandomIterator(outWR);
                for( int c = pBuffer; c < newCols - pBuffer; c++ ) {
                    if (c % 100 == 0) {
                        StringBuilder sb1 = new StringBuilder();
                        sb1.append(tileName);
                        sb1.append(": ");
                        sb1.append(c);
                        sb1.append(" of ");
                        sb1.append(newCols);
                        pm.message(sb1.toString());
                    }
                    for( int r = pBuffer; r < newRows - pBuffer; r++ ) {
                        Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, bufferedGridGeometry);
                        List<LasRecord> currentPoints = new ArrayList<LasRecord>();
                        for( int tmpC = c - pBuffer; tmpC <= c + pBuffer; tmpC++ ) {
                            for( int tmpR = r - pBuffer; tmpR <= r + pBuffer; tmpR++ ) {
                                currentPoints.addAll(lasMatrix[tmpC][tmpR]);
                            }
                        }
                        int size = currentPoints.size();
                        if (size >= pMinpoints) {
                            // need at least as many samples as parameters
                            try {
                                double[] parameters = calculateParameters(currentPoints);
                                double interpolatedValue = getInterpolatedValue(parameters, coordinate.x, coordinate.y);
                                // limit by min/max
                                if (interpolatedValue < minValue) {
                                    interpolatedValue = minValue;
                                }
                                if (interpolatedValue > maxValue) {
                                    interpolatedValue = maxValue;
                                }

                                CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, point);
                                outWIter.setSample(point.x, point.y, 0, interpolatedValue);
                            } catch (SingularMatrixException ex) {
                                // we ignore the singular matrix leaving the cell undefined
                            }
                        }
                    }
                }
            } finally {
                if (outWIter != null)
                    outWIter.done();
            }

            dumpRaster(outputCoverage, outTileFile.getAbsolutePath());
        } else {
            pm.message(tileName + " ignored because of no points there.");
        }
    }

    private double[] calculateParameters( final List<LasRecord> pointsInGeometry ) {
        int pointsNum = pointsInGeometry.size();

        final double[][] xyMatrix = new double[pointsNum][6];
        final double[] valueArray = new double[pointsNum];
        for( int i = 0; i < pointsNum; i++ ) {
            LasRecord dot = pointsInGeometry.get(i);
            xyMatrix[i][0] = dot.x * dot.x; // x^2
            xyMatrix[i][1] = dot.y * dot.y; // y^2
            xyMatrix[i][2] = dot.x * dot.y; // xy
            xyMatrix[i][3] = dot.x; // x
            xyMatrix[i][4] = dot.y; // y
            xyMatrix[i][5] = 1;
            if (doIntensity) {
                valueArray[i] = dot.intensity;
            } else {
                valueArray[i] = dot.z;
            }
        }

        RealMatrix A = MatrixUtils.createRealMatrix(xyMatrix);
        RealVector z = MatrixUtils.createRealVector(valueArray);

        DecompositionSolver solver = new RRQRDecomposition(A).getSolver();
        RealVector solution = solver.solve(z);

        // start values for a, b, c, d, e, f, all set to 0.0
        final double[] parameters = solution.toArray();
        return parameters;
    }

    private double getInterpolatedValue( double[] parameters, double x, double y ) {
        double z = parameters[0] * x * x + //
                parameters[1] * y * y + //
                parameters[2] * x * y + //
                parameters[3] * x + //
                parameters[4] * y + //
                parameters[5];
        return z;
    }

}
