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
package org.hortonmachine.gears.modules.r.summary;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.ACTCELLS;
import static org.hortonmachine.gears.libs.modules.Variables.AVG;
import static org.hortonmachine.gears.libs.modules.Variables.AVGABSDEV;
import static org.hortonmachine.gears.libs.modules.Variables.INVCELLS;
import static org.hortonmachine.gears.libs.modules.Variables.MAX;
import static org.hortonmachine.gears.libs.modules.Variables.MIN;
import static org.hortonmachine.gears.libs.modules.Variables.SDEV;
import static org.hortonmachine.gears.libs.modules.Variables.VAR;
import static org.hortonmachine.gears.libs.modules.Variables.SUM;

import java.awt.geom.AffineTransform;
import java.text.MessageFormat;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

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

@Description("Calculate zonal stats.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("zonalstats")
@Label(HMConstants.RASTERPROCESSING)
@Name("zonalstats")
@Status(Status.EXPERIMENTAL)
@License(OMSHYDRO_LICENSE)
public class OmsZonalStats extends HMModel {

    @Description("The raster map to process.")
    @In
    public GridCoverage2D inRaster = null;

    @Description("The polygons map on which to do the stats.")
    @In
    public SimpleFeatureCollection inVector = null;

    @Description("Percentage of minimum active cells to have a valid stat.")
    @In
    public double pPercentageThres = 20.0;

    @Description("Total meanvalue (also produced by this module) for the calculation of the mean absolute deviation.")
    @In
    public Double pTotalMean = null;

    @Description("The input polygons with the added stats values.")
    @Out
    public SimpleFeatureCollection outVector;

    /**
     * The array holding:
     * <ul>
     *  <li>totalMean</li>
     *  <li>userTotalMean</li>
     *  <li>totalActiveCells</li>
     * </ul>
     * if {@link #pTotalMean} is != <code>null</code>.
     */
    double[] tm_usertm_tactivecells = new double[3];

    @Execute
    public void process() throws Exception {
        checkNull(inVector, inRaster);

        boolean hasUserTotalMean = false;
        if (pTotalMean != null) {
            hasUserTotalMean = true;
            tm_usertm_tactivecells[1] = pTotalMean;
        }

        ReferencedEnvelope bounds = inVector.getBounds();
        CoordinateReferenceSystem crs = bounds.getCoordinateReferenceSystem();

        SimpleFeatureBuilder featureBuilder = createFeatureBuilder(crs, hasUserTotalMean);

        outVector = new DefaultFeatureCollection();
        List<Geometry> geometriesList = FeatureUtilities.featureCollectionToGeometriesList(inVector, true, null);

        // pm.message("" + readEnvelope);
        GridGeometry2D gridGeometry = inRaster.getGridGeometry();
        RandomIter readIter = CoverageUtilities.getRandomIterator(inRaster);
        double novalue = HMConstants.getNovalue(inRaster);
        pm.beginTask("Processing polygons...", geometriesList.size());
        for( Geometry geometry : geometriesList ) {
            double[] polygonStats = polygonStats(geometry, gridGeometry, readIter, novalue, hasUserTotalMean,
                    tm_usertm_tactivecells, pPercentageThres, pm);
            if (polygonStats == null) {
                continue;
            }

            Object[] values;
            if (!hasUserTotalMean) {
                values = new Object[]{geometry, //
                        polygonStats[0], //
                        polygonStats[1], //
                        polygonStats[2], //
                        polygonStats[3], //
                        polygonStats[4], //
                        polygonStats[5], //
                        (int) polygonStats[6], //
                        (int) polygonStats[7] //
                };
            } else {
                values = new Object[]{geometry, //
                        polygonStats[0], //
                        polygonStats[1], //
                        polygonStats[2], //
                        polygonStats[3], //
                        polygonStats[4], //
                        polygonStats[5], //
                        polygonStats[6], //
                        (int) polygonStats[7], //
                        (int) polygonStats[8] //
                };
            }

            featureBuilder.addAll(values);
            SimpleFeature feature = featureBuilder.buildFeature(null);
            ((DefaultFeatureCollection) outVector).add(feature);
            pm.worked(1);
        }
        pm.done();

        if (!hasUserTotalMean) {
            tm_usertm_tactivecells[0] = tm_usertm_tactivecells[0] / tm_usertm_tactivecells[2];
            pm.message("Total mean: " + tm_usertm_tactivecells[0]);
        }
    }

    /**
     * @param geometry
     * @param gridGeometry
     * @param inIter
     * @param tm_utm_tac the array holding:
     *                  <ul>
     *                      <li>totalMean</li>
     *                      <li>userTotalMean</li>
     *                      <li>totalActiveCells</li>
     *                  </ul>
     * @param percentageThres
     * @param monitor an optional monitor. If <code>null</code> an exception
     *          is thrown in case of cusps, else an errormessage is given, but
     *          processing will go on ignoring the geometry.
     * @return
     * @throws Exception
     */
    public static double[] polygonStats( Geometry geometry, GridGeometry2D gridGeometry, RandomIter inIter, double novalue,
            boolean hasUserTotalMean, double[] tm_utm_tac, double percentageThres, IHMProgressMonitor monitor ) throws Exception {
        GeometryFactory gf = GeometryUtilities.gf();
        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int rows = gridRange.height;
        int cols = gridRange.width;
        int startX = gridRange.x;
        int startY = gridRange.y;
        AffineTransform gridToCRS = (AffineTransform) gridGeometry.getGridToCRS();
        double xRes = XAffineTransform.getScaleX0(gridToCRS);
        double yRes = XAffineTransform.getScaleY0(gridToCRS);

        final double delta = xRes / 4.0;
        Envelope env = geometry.getEnvelopeInternal();
        env.expandBy(xRes, yRes);
        double geometryUpperBound = env.getMaxY();
        double envArea = env.getWidth() * env.getHeight();
        int maxCells = (int) ceil(envArea / (xRes * yRes));

        int activeCellCount = 0;
        int passiveCellCount = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0.0;

        MathTransform gridToCRS2 = gridGeometry.getGridToCRS();

        double[] values = new double[maxCells];

        LineString previousLine = null;
        final Coordinate worldWestCoord = new Coordinate();
        final Coordinate worldEastCoord = new Coordinate();
        for( int r = startY; r < startY + rows; r++ ) {
            // do scan line to fill the polygon
            Coordinate gridWestCoord = new Coordinate(startX, r);
            JTS.transform(gridWestCoord, worldWestCoord, gridToCRS2);
            if (geometryUpperBound < worldWestCoord.y) {
                continue;
            }
            Coordinate gridEastCoord = new Coordinate(startX + cols - 1, r);
            JTS.transform(gridEastCoord, worldEastCoord, gridToCRS2);
            LineString line = gf.createLineString(new Coordinate[]{worldWestCoord, worldEastCoord});
            if (previousLine != null && previousLine.equals(line)) {
                previousLine = line;
                continue;
            }
            if (geometry.intersects(line)) {
                Geometry internalLines = geometry.intersection(line);
                int lineNums = internalLines.getNumGeometries();
                for( int l = 0; l < lineNums; l++ ) {
                    Coordinate[] coords = internalLines.getGeometryN(l).getCoordinates();
                    if (coords.length == 2) {
                        for( int j = 0; j < coords.length; j = j + 2 ) {
                            Coordinate startC = new Coordinate(coords[j].x + delta, coords[j].y);
                            Coordinate endC = new Coordinate(coords[j + 1].x - delta, coords[j + 1].y);

                            DirectPosition2D startDP;
                            DirectPosition2D endDP;
                            if (startC.x < endC.x) {
                                startDP = new DirectPosition2D(startC.x, startC.x);
                                endDP = new DirectPosition2D(endC.x, endC.x);
                            } else {
                                startDP = new DirectPosition2D(endC.x, endC.x);
                                endDP = new DirectPosition2D(startC.x, startC.x);
                            }
                            GridCoordinates2D startGridCoord = gridGeometry.worldToGrid(startDP);
                            GridCoordinates2D endGridCoord = gridGeometry.worldToGrid(endDP);

                            /*
                             * the part in between has to be filled
                             */
                            for( int k = startGridCoord.x; k <= endGridCoord.x; k++ ) {
                                double v = inIter.getSampleDouble(k, r, 0);
                                if (isNovalue(v, novalue)) {
                                    passiveCellCount++;
                                    continue;
                                }
                                min = Math.min(min, v);
                                max = Math.max(max, v);
                                sum += v;
                                values[activeCellCount] = v;
                                activeCellCount++;

                                if (!hasUserTotalMean) {
                                    tm_utm_tac[0] = tm_utm_tac[0] + v;
                                    tm_utm_tac[2] = tm_utm_tac[2] + 1;
                                }
                            }
                        }
                    } else {
                        if (coords.length == 1) {
                            String message = MessageFormat.format("Found a cusp in: {0}/{1}", coords[0].x, coords[0].y);
                            if (monitor != null) {
                                monitor.errorMessage(message);
                            } else {
                                throw new ModelsIOException(message, "ZonalStats");
                            }
                        } else {
                            throw new ModelsIOException(MessageFormat
                                    .format("Found intersection with more than 2 points in: {0}/{1}", coords[0].x, coords[0].y),
                                    "ZonalStats");
                        }
                    }
                }

            }

            previousLine = line;
        }

        int all = activeCellCount + passiveCellCount;
        double ratio = 100.0 * activeCellCount / all;
        if (ratio < percentageThres) {
            return null;
        }

        double mean = mean(values, activeCellCount);
        double sdev = standardDeviation(values, mean, activeCellCount);
        double var = variance(values, mean, activeCellCount);

        double[] result;
        if (hasUserTotalMean) {
            double meanAbsoluteDeviation = meanAbsoluteDeviation(values, activeCellCount, tm_utm_tac[1]);
            result = new double[]{min, max, mean, var, sdev, sum, meanAbsoluteDeviation, activeCellCount, passiveCellCount};
        } else {
            result = new double[]{min, max, mean, var, sdev, sum, activeCellCount, passiveCellCount};
        }
        return result;
    }

    public static SimpleFeatureBuilder createFeatureBuilder( CoordinateReferenceSystem crs, boolean hasUserTotalMean ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("stats");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add(MIN, Double.class);
        b.add(MAX, Double.class);
        b.add(AVG, Double.class);
        b.add(VAR, Double.class);
        b.add(SDEV, Double.class);
        b.add(SUM, Double.class);
        if (hasUserTotalMean)
            b.add(AVGABSDEV, Double.class);
        b.add(ACTCELLS, Integer.class);
        b.add(INVCELLS, Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        return new SimpleFeatureBuilder(type);
    }

    private static double meanAbsoluteDeviation( double[] values, int count, double userTotalMean ) {
        double mean = 0;
        for( int i = 0; i < count; i++ ) {
            mean = mean + abs(values[i] - userTotalMean);
        }
        return mean / count;
    }

    private static double mean( double[] values, int count ) {
        double mean = 0;
        for( int i = 0; i < count; i++ ) {
            mean += values[i];
        }
        return mean / count;
    }

    private static double standardDeviation( double[] values, double mean, int count ) {
        double sd = variance(values, mean, count);
        sd = sqrt(sd);
        return sd;
    }

    private static double variance( double[] values, double mean, int count ) {
        double variance = 0;
        for( int i = 0; i < count; i++ ) {
            variance = variance + pow(values[i] - mean, 2.0);
        }
        variance = variance / (count);
        return variance;
    }

    protected void processCell( int readCol, int readRow, int writeCol, int writeRow, int readCols, int readRows, int writeCols,
            int writeRows ) {
        // not used in this case
    }
}
