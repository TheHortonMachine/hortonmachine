/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.libs.modules;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.intNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.i18n.GearsMessageHandler;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

/**
 * A class containing several methods used by the modules.
 *
 * <p>The methods are not static for usage in multithreading environment.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Erica Ghesla
 * @author Daniele Andreis
 */
@SuppressWarnings("unchecked")
public class ModelsEngine {
    /**
     * @deprecated this should not be used. Use the {@link FlowNode} concept instead.
     */
    public static int[][] DIR = ModelsSupporter.DIR;

    private static int[][] dirIn = ModelsSupporter.DIR_WITHFLOW_ENTERING;

    private static GearsMessageHandler msg = GearsMessageHandler.getInstance();

    public static PixelInCell DEFAULTPIXELANCHOR = PixelInCell.CELL_CENTER;

    /**
     * Moves one pixel downstream.
     *
     * @param colRow
     *            the array containing the column and row of the current pixel.
     *            It will be modified here to represent the next downstream
     *            pixel.
     * @param flowdirection
     *            the current flowdirection number.
     * @return true if everything went well.
     */
    public static boolean go_downstream( int[] colRow, double flowdirection ) {

        int n = (int) flowdirection;
        if (n == 10) {
            return true;
        } else if (n < 1 || n > 9) {
            return false;
        } else {
            colRow[1] += DIR[n][0];
            colRow[0] += DIR[n][1];
            return true;
        }
    }

    /**
     * Moves one pixel upstream.
     *
     * @param p
     * @param flowRandomIter
     * @param tcaRandomIter
     * @param lRandomIter
     * @param param
     */
    public static void go_upstream_a( int[] p, RandomIter flowRandomIter, RandomIter tcaRandomIter, RandomIter lRandomIter,
            int[] param ) {
        double area = 0, lenght = 0;
        int[] point = new int[2];
        int kk = 0, count = 0;

        point[0] = p[0];
        point[1] = p[1];
        // check how many pixels are draining in the considered pixel and select
        // the pixel with maximun tca
        for( int k = 1; k <= 8; k++ ) {
            if (flowRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0) == dirIn[k][2]) {
                // counts how many pixels are draining in the considere
                count++;
                if (tcaRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0) >= area) {
                    // if two pixels has the same tca select the pixel with the
                    // maximum vale of hacklength
                    if (tcaRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0) == area) {
                        if (lRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0) > lenght) {
                            kk = k;
                            area = tcaRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0);
                            lenght = lRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0);
                            point[0] = p[0] + dirIn[k][1];
                            point[1] = p[1] + dirIn[k][0];
                        }
                    } else {
                        kk = k;
                        area = tcaRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0);
                        lenght = lRandomIter.getSampleDouble(p[0] + dirIn[k][1], p[1] + dirIn[k][0], 0);
                        point[0] = p[0] + dirIn[k][1];
                        point[1] = p[1] + dirIn[k][0];
                    }
                }
            }

        }
        p[0] = point[0];
        p[1] = point[1];
        param[0] = kk;
        param[1] = count;
    }

    /**
     * Moves one pixel upstream following the supplied network. TODO Daniele doc
     *
     * @param colRow
     * @param flowIterator
     * @param netnumIterator
     * @param param
     */
    public static void goUpStreamOnNetFixed( int[] colRow, RandomIter flowIterator, RandomIter netnumIterator, int[] param ) {

        int kk = 0, count = 0;
        int[] point = new int[2];

        for( int k = 1; k <= 8; k++ ) {
            if (flowIterator.getSampleDouble(colRow[0] + dirIn[k][1], colRow[1] + dirIn[k][0], 0) == dirIn[k][2]) {
                count++;
                if (netnumIterator.getSampleDouble(colRow[0] + dirIn[k][1], colRow[1] + dirIn[k][0], 0) == netnumIterator
                        .getSampleDouble(colRow[0], colRow[1], 0)) {
                    kk = k;
                    point[0] = colRow[0] + dirIn[k][1];
                    point[1] = colRow[1] + dirIn[k][0];
                }
            }
        }
        if (kk == 0) {
            for( int k = 1; k <= 8; k++ ) {
                if (flowIterator.getSampleDouble(colRow[0] + dirIn[k][1], colRow[1] + dirIn[k][0], 0) == dirIn[k][2]) {
                    kk = k;
                    point[0] = colRow[0] + dirIn[k][1];
                    point[1] = colRow[1] + dirIn[k][0];
                }
            }
        }
        colRow[0] = point[0];
        colRow[1] = point[1];
        param[0] = kk;
        param[1] = count;
    }

    /**
     * It create the shape-file of channel network
     *
     * @param flowImage the map of flow direction.
     * @param netNumImage the map of netnumbering.
     * @param gridGeometry the {@link GridGeometry2D} of the flow coverage.
     * @param nstream
     * @param pm the progress monitor.
     * @return the extracted features.
     * @throws IOException
     * @throws TransformException
     */
    public static SimpleFeatureCollection net2ShapeOnly( RenderedImage flowImage, WritableRaster netNumImage,
            GridGeometry2D gridGeometry, List<Integer> nstream, IHMProgressMonitor pm ) throws IOException, TransformException {

        int activecols = flowImage.getWidth();
        int activerows = flowImage.getHeight();

        int[] flow = new int[2];
        int[] flow_p = new int[2];

        CoordinateList coordlist = new CoordinateList();
        RandomIter m1RandomIter = RandomIterFactory.create(flowImage, null);
        RandomIter netNumRandomIter = RandomIterFactory.create(netNumImage, null);
        // GEOMETRY
        // creates new LineSting array
        LineString[] newGeometry = new LineString[nstream.size()];
        // creates a vector of geometry
        List<LineString> newGeometryVectorLine = new ArrayList<LineString>();
        GeometryFactory newfactory = new GeometryFactory();

        pm.beginTask(msg.message("utils.extracting_network_geometries"), nstream.size());
        for( int num = 1; num <= nstream.size(); num++ ) {
            for( int y = 0; y < activerows; y++ ) {
                for( int x = 0; x < activecols; x++ ) {
                    if (!isNovalue(m1RandomIter.getSampleDouble(x, y, 0))) {
                        flow[0] = x;
                        flow[1] = y;
                        // looks for the source
                        if (netNumRandomIter.getSampleDouble(x, y, 0) == num) {
                            // if the point is a source it starts to extract the
                            // channel...
                            if (sourcesNet(m1RandomIter, flow, num, netNumRandomIter)) {
                                flow_p[0] = flow[0];
                                flow_p[1] = flow[1];
                                double[] worldPosition = gridGeometry.gridToWorld(new GridCoordinates2D(flow[0], flow[1]))
                                        .getCoordinate();
                                Coordinate coordSource = new Coordinate(worldPosition[0], worldPosition[1]);
                                // creates new Object Coordinate... SOURCE
                                // POINT...
                                // adds the points to the CoordinateList
                                coordlist.add(coordSource);
                                if (!go_downstream(flow, m1RandomIter.getSampleDouble(flow[0], flow[1], 0)))
                                    return null;
                                // it extracts the other points of the
                                // channel... it
                                // continues until the next node...
                                while( !isNovalue(m1RandomIter.getSampleDouble(flow[0], flow[1], 0))
                                        && m1RandomIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                        && netNumRandomIter.getSampleDouble(flow[0], flow[1], 0) == num
                                        && !isNovalue(netNumRandomIter.getSampleDouble(flow[0], flow[1], 0)) ) {
                                    worldPosition = gridGeometry.gridToWorld(new GridCoordinates2D(flow[0], flow[1]))
                                            .getCoordinate();
                                    Coordinate coordPoint = new Coordinate(worldPosition[0], worldPosition[1]);
                                    // creates new Object Coordinate... CHANNEL
                                    // POINT...
                                    // adds new points to CoordinateList
                                    coordlist.add(coordPoint);
                                    flow_p[0] = flow[0];
                                    flow_p[1] = flow[1];
                                    if (!go_downstream(flow, m1RandomIter.getSampleDouble(flow[0], flow[1], 0)))
                                        return null;
                                }
                                worldPosition = gridGeometry.gridToWorld(new GridCoordinates2D(flow[0], flow[1])).getCoordinate();
                                Coordinate coordNode = new Coordinate(worldPosition[0], worldPosition[1]);
                                // creates new Object Coordinate... NODE
                                // POINT...
                                // adds new points to CoordinateList
                                coordlist.add(coordNode);
                            }
                        }
                    }
                }
            }
            // when the channel is complete creates one new geometry (new
            // channel of the network)
            newGeometry[num - 1] = newfactory.createLineString(coordlist.toCoordinateArray());
            // adds the new geometry to the vector of geometry
            newGeometryVectorLine.add(newGeometry[num - 1]);
            // it removes every element of coordlist
            coordlist.clear();
            pm.worked(1);
        }
        pm.done();

        // create the feature type
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("network"); //$NON-NLS-1$
        b.setCRS(gridGeometry.getCoordinateReferenceSystem());
        b.add("the_geom", LineString.class); //$NON-NLS-1$
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        SimpleFeatureCollection featureCollection = new DefaultFeatureCollection();
        int index = 0;
        for( LineString lineString : newGeometryVectorLine ) {
            Object[] values = new Object[]{lineString};
            // add the values
            builder.addAll(values);
            // build the feature with provided ID
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + index); //$NON-NLS-1$
            index++;
            ((DefaultFeatureCollection) featureCollection).add(feature);
        }
        return featureCollection;
    }

    /**
     * Create the {@link MultiLineString line} geometries of channel network
     *
     * @param flowIter the flow map.
     * @param netNumIter the netnumbering map.
     * @param nstream
     * @param pm the progress monitor.
     * @param gridGeometry the gridgeometry.
     * @return the geometries of the network.
     * @throws IOException
     * @throws TransformException
     */
    public static List<MultiLineString> net2ShapeGeometries( WritableRandomIter flowIter, RandomIter netNumIter, int[] nstream,
            GridGeometry2D gridGeometry, IHMProgressMonitor pm ) throws IOException, TransformException {

        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int rows = gridRange.height;
        int cols = gridRange.width;

        // screen2world
        MathTransform2D gridToCRS2D = gridGeometry.getGridToCRS2D();

        // get rows and cols from the active region
        int[] flow = new int[2];
        int[] flow_p = new int[2];

        CoordinateList coordlist = new CoordinateList();

        // creates a vector of geometry
        List<MultiLineString> newGeometryVectorLine = new ArrayList<MultiLineString>();
        GeometryFactory newfactory = new GeometryFactory();

        /* name of new geometry (polyline) */
        pm.beginTask("Extracting the network geometries...", nstream[0]);
        for( int num = 1; num <= nstream[0]; num++ ) {
            for( int y = 0; y < rows; y++ ) {
                for( int x = 0; x < cols; x++ ) {
                    flow[0] = x;
                    flow[1] = y;
                    // looks for the source
                    if (netNumIter.getSampleDouble(x, y, 0) == num) {
                        // if the point is a source it starts to extract the
                        // channel...
                        if (sourcesNet(flowIter, flow, num, netNumIter)) {
                            flow_p[0] = flow[0];
                            flow_p[1] = flow[1];

                            Point2D worldPosition = new Point2D.Double(flow[0], flow[1]);
                            gridToCRS2D.transform(worldPosition, worldPosition);
                            Coordinate coordSource = new Coordinate(worldPosition.getX(), worldPosition.getY());

                            // creates new Object Coordinate... SOURCE POINT...
                            // adds the points to the CoordinateList
                            coordlist.add(coordSource);
                            if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                            // it extracts the other points of the channel... it
                            // continues until the next node...
                            while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                    && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                    && netNumIter.getSampleDouble(flow[0], flow[1], 0) == num
                                    && !isNovalue(netNumIter.getSampleDouble(flow[0], flow[1], 0)) ) {

                                worldPosition = new Point2D.Double(flow[0], flow[1]);
                                gridToCRS2D.transform(worldPosition, worldPosition);
                                Coordinate coordPoint = new Coordinate(worldPosition.getX(), worldPosition.getY());

                                // creates new Object Coordinate... CHANNEL
                                // POINT...
                                // adds new points to CoordinateList
                                coordlist.add(coordPoint);
                                flow_p[0] = flow[0];
                                flow_p[1] = flow[1];
                                if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                    return null;
                            }
                            worldPosition = new Point2D.Double(flow[0], flow[1]);
                            gridToCRS2D.transform(worldPosition, worldPosition);
                            Coordinate coordNode = new Coordinate(worldPosition.getX(), worldPosition.getY());
                            // creates new Object Coordinate... NODE POINT...
                            // adds new points to CoordinateList
                            coordlist.add(coordNode);
                        }
                    }
                }
            }
            // when the channel is complete creates one new geometry (new
            // channel of the network)
            // adds the new geometry to the vector of geometry
            // if (!coordlist.isEmpty()) {
            newGeometryVectorLine.add(newfactory
                    .createMultiLineString(new LineString[]{newfactory.createLineString(coordlist.toCoordinateArray())}));
            // } else {
            // if (out != null)
            // out.println("Found an empty geometry at " + num);
            // }
            // it removes every element of coordlist
            coordlist.clear();
            pm.worked(1);
        }
        pm.done();
        return newGeometryVectorLine;
    }

    /**
     * Controls if the considered point is a source in the network map.
     *
     * @param flowIterator
     *            {@link RandomIter iterator} of flowdirections map
     * @param colRow the col and row of the point to check.
     * @param num
     *            channel number
     * @param netNum
     *            {@link RandomIter iterator} of the netnumbering map.
     * @return
     */
    public static boolean sourcesNet( RandomIter flowIterator, int[] colRow, int num, RandomIter netNum ) {
        int[][] dir = {{0, 0, 0}, {1, 0, 5}, {1, -1, 6}, {0, -1, 7}, {-1, -1, 8}, {-1, 0, 1}, {-1, 1, 2}, {0, 1, 3}, {1, 1, 4}};

        if (flowIterator.getSampleDouble(colRow[0], colRow[1], 0) <= 10.0
                && flowIterator.getSampleDouble(colRow[0], colRow[1], 0) > 0.0) {
            for( int k = 1; k <= 8; k++ ) {
                if (flowIterator.getSampleDouble(colRow[0] + dir[k][0], colRow[1] + dir[k][1], 0) == dir[k][2]
                        && netNum.getSampleDouble(colRow[0] + dir[k][0], colRow[1] + dir[k][1], 0) == num) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * Takes a input raster and vectorializes it.
     *
     * @param input
     * @return
     */
    public static double[] vectorizeDoubleMatrix( RenderedImage input ) {
        double[] U = new double[input.getWidth() * input.getHeight()];
        RandomIter inputRandomIter = RandomIterFactory.create(input, null);

        int j = 0;
        for( int i = 0; i < input.getHeight() * input.getWidth(); i = i + input.getWidth() ) {
            double tmp[] = new double[input.getWidth()];
            for( int k = 0; k < input.getWidth(); k++ ) {
                tmp[k] = inputRandomIter.getSampleDouble(k, j, 0);
            }

            System.arraycopy(tmp, 0, U, i, input.getWidth());
            j++;
        }

        return U;
    }

    /**
     * TODO Daniele doc
     *
     * @param U
     * @param T
     * @param theSplit
     * @param binNum
     * @param num_max
     * @return
     */
    public static double split2realvectors( double[] U, double[] T, SplitVectors theSplit, int binNum, int num_max,
            IHMProgressMonitor pm ) {

        double binStep = 0, minValue = 0, maxValue;
        int i, count = 0, previousCount, minPosition = 0, maxPosition = 0, emptyBins;
        int[] bins;
        int head = 0;

        bins = new int[U.length];

        if (binNum <= 1) {
            previousCount = 1;
            count = 1;

            int index = 0;
            while( count < U.length ) {

                // was while( count <= U.length && U[count] == U[count - 1] ) {
                while( count < U.length && U[count] == U[count - 1] ) {
                    count++;
                }

                index++;
                bins[index] = count - previousCount;
                head++;
                previousCount = count;
                count++;
                if (head > num_max)
                    throw new ModelsIllegalargumentException("The number of bin exceeds the maximum number allowed.", "MODEL",
                            pm);
            }

        } else if (binNum > 1) {

            minPosition = 0;
            maxValue = U[U.length - 1];
            while( minPosition < U.length && isNovalue(U[minPosition]) ) {
                minPosition++;
            }
            if (minPosition == U.length) {
                // force the situation of non calculation
                binStep = 0;
            } else {
                minValue = U[minPosition];
                maxPosition = U.length - 1;

                binStep = (maxValue - minValue) / (binNum - 1);
            }

            if (binStep != 0) {
                int binIndex = 0;

                previousCount = minPosition; // the novalues are already left aside
                count = minPosition;
                emptyBins = 0;

                double runningCenter = minValue + binStep / 2.0;

                for( int n = 0; n < binNum - 1; n++ ) {
                    double upperLimitOfBin;
                    if (n == binNum - 2) {
                        upperLimitOfBin = maxValue;
                    } else {
                        upperLimitOfBin = runningCenter + binStep / 2.0;
                    }
                    if (U[count] <= upperLimitOfBin) {
                        double value = U[count];
                        while( value <= upperLimitOfBin ) {
                            count++;
                            if (count > maxPosition) {
                                break;
                            }
                            value = U[count];
                        }

                        bins[binIndex] = count - previousCount;
                        // contained in the bin
                        binIndex++;

                        head++;
                        previousCount = count;
                        // count++;

                    } else {
                        emptyBins++;
                    }
                    runningCenter += binStep;
                }

                if (emptyBins != 0) {
                    pm.message(emptyBins + " empty bins where found");
                }
            } else {
                for( double tmpValue : U ) {
                    if (!isNovalue(tmpValue)) {
                        count++;
                    }
                }
                bins[0] = count;
                head = count;
            }

        }

        if (head < 1) {
            throw new ModelsIllegalargumentException("Something wrong happened in binning", "MODEL", pm);
        } else {
            theSplit.initIndex(head);

            int maxnumberinbin = 0;
            for( i = 0; i < head; i++ ) {
                theSplit.splitIndex[i] = bins[i];
                if (bins[i] > maxnumberinbin)
                    maxnumberinbin = bins[i];
            }

            /*
             * now a list of the values inside the bins are put into the
             * matrixes, therefore we need as many rows as bins and a column
             * number high enough to hold the major number of values hold inside
             * a bin.
             */
            theSplit.initValues(head, maxnumberinbin);

            int index = minPosition;
            for( int j = 0; j < head; j++ ) {
                for( int k = 0; k < theSplit.splitIndex[j]; k++ ) {
                    theSplit.splitValues1[j][k] = U[index];
                    theSplit.splitValues2[j][k] = T[index];
                    index++;
                }
            }
        }

        if (binNum < 2)
            binStep = 0;

        return binStep;
    }

    /**
     * Calculates the nth moment of a set of values.
     * 
     * @param values the array of values.
     * @param validValues the number of valid values in the array.
     * @param mean the mean to use.
     * @param momentOrder the moment order to calculate.
     * @param pm the monitor.
     * @return the nth moment value.
     */
    public static double calculateNthMoment( double[] values, int validValues, double mean, double momentOrder,
            IHMProgressMonitor pm ) {
        double moment = 0.0;
        double n = 0.0;

        if (momentOrder == 1.0) {
            for( int i = 0; i < validValues; i++ ) {
                if (!isNovalue(values[i])) {
                    moment += values[i];
                    n++;
                }
            }
            if (n >= 1) {
                moment /= n;
            }
        } else if (momentOrder == 2.0) {
            // FIXME this needs to be checked, variance doesn't give negative values
            for( int i = 0; i < validValues; i++ ) {
                if (!isNovalue(values[i])) {
                    moment += (values[i]) * (values[i]);
                    n++;
                }
            }
            if (n >= 1) {
                moment = (moment / n - mean * mean);
            }
        } else {
            for( int i = 0; i < validValues; i++ ) {
                if (!isNovalue(values[i])) {
                    moment += pow((values[i] - mean), momentOrder);
                    n++;
                }
            }
            if (n >= 1) {
                moment /= n;
            }
        }

        if (n == 0) {
            pm.errorMessage("No valid data were processed, setting moment value to zero.");
            moment = 0.0;
        }

        return moment;
    }

    /**
     * Calculate the map of netnumbering.
     *
     * @param flowGC the map of flowdirection.
     * @param netGC the map of network.
     * @param tcaGC the optional map of tca.
     * @param tcaThreshold the threshold on the tca.
     * @param pointsFC optional feature collection of points in which to split the net.
     * @param pm the monitor.
     * @return the raster of netnumbering.
     * @throws Exception
     */
    public static WritableRaster netNumbering( GridCoverage2D flowGC, GridCoverage2D netGC, GridCoverage2D tcaGC,
            List<Geometry> points, List<NetLink> netLinksList, IHMProgressMonitor pm ) throws Exception {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(flowGC);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        WritableRaster netnumWR = CoverageUtilities.createWritableRaster(cols, rows, Integer.class, null, null);
        WritableRandomIter netnumIter = RandomIterFactory.createWritable(netnumWR, null);

        int flowNv = HMConstants.getIntNovalue(flowGC);
        double netNv = HMConstants.getNovalue(netGC);

        RandomIter flowIter = CoverageUtilities.getRandomIterator(flowGC);
        RandomIter netIter = CoverageUtilities.getRandomIterator(netGC);
        RandomIter tcaIter = null;
        if (tcaGC != null)
            tcaIter = CoverageUtilities.getRandomIterator(tcaGC);
        try {
            /*
             * split nodes are points that create new numbering:
             * - first points upstream on net
             * - confluences
             * - supplied points
             */
            List<FlowNode> splitNodes = new ArrayList<>();
            List<String> fixedNodesColRows = new ArrayList<>();
            // SUPPLIED POINTS
            if (points != null) {
                Envelope envelope = regionMap.toEnvelope();
                GridGeometry2D gridGeometry = flowGC.getGridGeometry();
                // snap points on net if necessary
                for( Geometry point : points ) {
                    Coordinate pointCoordinate = point.getCoordinate();
                    if (envelope.contains(pointCoordinate)) {
                        GridCoordinates2D gridCoordinate = gridGeometry
                                .worldToGrid(new DirectPosition2D(pointCoordinate.x, pointCoordinate.y));

                        GridNode netNode = new GridNode(netIter, cols, rows, -1, -1, gridCoordinate.x, gridCoordinate.y, netNv);
                        FlowNode flowNode = new FlowNode(flowIter, cols, rows, gridCoordinate.x, gridCoordinate.y, flowNv);
                        while( !netNode.isValid() ) {
                            flowNode = flowNode.goDownstream();
                            if (flowNode == null)
                                break;
                            netNode = new GridNode(netIter, cols, rows, -1, -1, flowNode.col, flowNode.row, netNv);
                        }
                        if (flowNode != null) {
                            /*
                             * now we need to go one more down. This is necessary, since 
                             * in the later processing the netnumber channel is extracted 
                             * going downstream from the splitnodes, so the supplied point 
                             * would end up being the most upstream point of the basin.
                             * Therefore we move one down, one downstream of teh point 
                             * we want to be the basin outlet.
                             */
                            FlowNode flowNodeTmp = flowNode.goDownstream();
                            if (flowNodeTmp != null) {
                                netNode = new GridNode(netIter, cols, rows, -1, -1, flowNodeTmp.col, flowNodeTmp.row, netNv);
                                if (netNode.isValid) {
                                    splitNodes.add(flowNodeTmp);
                                    fixedNodesColRows.add(flowNode.col + "_" + flowNode.row);
                                }
                            }
                        }
                    }
                }
            }

            // FIND CONFLUENCES AND NETWORK STARTING POINTS (MOST UPSTREAM)
            pm.beginTask("Find confluences...", rows);
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    GridNode netNode = new GridNode(netIter, cols, rows, -1, -1, c, r, netNv);
                    if (netNode.isValid()) {
                        List<GridNode> validSurroundingNodes = netNode.getValidSurroundingNodes();
                        FlowNode currentflowNode = new FlowNode(flowIter, cols, rows, c, r, flowNv);
                        int enteringCount = 0;
                        for( GridNode gridNode : validSurroundingNodes ) {
                            FlowNode tmpNode = new FlowNode(flowIter, cols, rows, gridNode.col, gridNode.row, flowNv);
                            List<FlowNode> enteringNodes = currentflowNode.getEnteringNodes();
                            if (enteringNodes.contains(tmpNode)) {
                                enteringCount++;
                            }
                        }
                        if (enteringCount != 1) {
                            splitNodes.add(currentflowNode);
                        }
                    }
                }
                pm.worked(1);
            }
            pm.done();
            pm.message("Found split points: " + splitNodes.size());

            int channel = 1;
            pm.beginTask("Numbering network...", splitNodes.size());
            for( int i = 0; i < splitNodes.size(); i++ ) {
                FlowNode splitNode = splitNodes.get(i);
                int startTca = splitNode.getIntValueFromMap(tcaIter);

                setNetNumWithCheck(netnumIter, channel, splitNode);

                FlowNode lastNode = null;
                FlowNode nextNode = splitNode.goDownstream();
                int endTca;
                if (nextNode == null || splitNodes.contains(nextNode)) {
                    // it is a one pixel basin
                    endTca = startTca;
                    lastNode = splitNode;

                } else {
                    endTca = intNovalue;
                    if (nextNode != null) {
                        do {
                            lastNode = nextNode;
                            endTca = nextNode.getIntValueFromMap(tcaIter);

                            setNetNumWithCheck(netnumIter, channel, nextNode);
                            nextNode = nextNode.goDownstream();
                        } while( nextNode != null && !splitNodes.contains(nextNode) );
                    }
                }

                int upCol = splitNode.col;
                int upRow = splitNode.row;
                int downCol = lastNode.col;
                int downRow = lastNode.row;

                int downLinkCol = -1;
                int downLinkRow = -1;
                if (nextNode != null) {
                    downLinkCol = nextNode.col;
                    downLinkRow = nextNode.row;
                }
                boolean isFixedNode = false;
                if (fixedNodesColRows.contains(lastNode.col + "_" + lastNode.row)) {
                    isFixedNode = true;
                    // System.out.println("Found fixed: " + lastNode);
                }

                NetLink link = new NetLink(channel, upCol, upRow, downCol, downRow, downLinkCol, downLinkRow, isFixedNode);
                link.setTca(endTca);
                netLinksList.add(link);

                channel++;
                pm.worked(1);
            }
            pm.done();
        } finally {
            netnumIter.done();
            flowIter.done();
            netIter.done();
            if (tcaIter != null) {
                tcaIter.done();
            }
        }
        return netnumWR;
    }

    private static void setNetNumWithCheck( WritableRandomIter netnumIter, int channel, FlowNode node ) {
        int sample = netnumIter.getSample(node.col, node.row, 0);
        if (HMConstants.isNovalue(sample)) {
            throw new ModelsIllegalargumentException("Can't over write existing netnum " + sample + " with " + channel,
                    "ModelsEngine#setNetNumWithCheck");
        }
        node.setIntValueInMap(netnumIter, channel);
    }

    /**
     * Extract the subbasins of a raster map.
     *
     * @param flowIter the map of flowdirections.
     * @param netIter the network map.
     * @param netNumberIter the netnumber map.
     * @param rows rows of the region.
     * @param cols columns of the region.
     * @param pm
     * @return the map of extracted subbasins.
     */
    public static WritableRaster extractSubbasins( WritableRandomIter flowIter, int flowNovalue, RandomIter netIter,
            WritableRandomIter netNumberIter, int rows, int cols, IHMProgressMonitor pm ) {

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                if (!isNovalue(netIter.getSampleDouble(c, r, 0)))
                    flowIter.setSample(c, r, 0, FlowNode.OUTLET);
            }
        }

        WritableRaster subbasinWR = CoverageUtilities.createWritableRaster(cols, rows, Integer.class, null, null);
        WritableRandomIter subbasinIter = RandomIterFactory.createWritable(subbasinWR, null);

        markHillSlopeWithLinkValue(flowIter, flowNovalue, netNumberIter, subbasinIter, cols, rows, pm);

        try {
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    int netValue = netIter.getSample(c, r, 0);
                    int netNumberValue = netNumberIter.getSample(c, r, 0);
                    if (!isNovalue(netValue)) {
                        subbasinIter.setSample(c, r, 0, netNumberValue);
                    }
                    if (NumericsUtilities.dEq(netNumberValue, 0)) {
                        netNumberIter.setSample(c, r, 0, HMConstants.intNovalue);
                    }
                    int subbValue = subbasinIter.getSample(c, r, 0);
                    if (NumericsUtilities.dEq(subbValue, 0))
                        subbasinIter.setSample(c, r, 0, HMConstants.intNovalue);
                }
            }
        } finally {
            subbasinIter.done();
        }

        return subbasinWR;
    }

    /**
     * Marks a map on the hillslope with the values on the channel of an attribute map.
     *
     * @param flowIter map of flow direction with the network cells
     *                  all marked as {@link FlowNode#NETVALUE}. This is very important!
     * @param attributeIter map of attributes.
     * @param markedIter the map to be marked.
     * @param cols region cols.
     * @param rows region rows.
     * @param pm monitor.
     */
    public static void markHillSlopeWithLinkValue( RandomIter flowIter, int flowNovalue, RandomIter attributeIter,
            WritableRandomIter markedIter, int cols, int rows, IHMProgressMonitor pm ) {
        pm.beginTask("Marking the hillslopes with the channel value...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, cols, rows, c, r, flowNovalue);
                if (flowNode.isHeadingOutside()) {
                    // ignore single cells on borders that exit anyway
                    continue;
                }

                if (flowNode.isMarkedAsOutlet()) {
                    double attributeValue = flowNode.getDoubleValueFromMap(attributeIter);
                    flowNode.setDoubleValueInMap(markedIter, attributeValue);
                    continue;
                }
                if (flowNode.isValid() && flowNode.isSource()) {
                    /*
                     * run down to the net to find the
                     * attribute map content on the net 
                     */
                    double attributeValue = doubleNovalue;
                    FlowNode runningNode = flowNode.goDownstream();
                    int runningRow = -1;
                    int runningCol = -1;
                    while( runningNode != null && runningNode.isValid() ) {
                        runningRow = runningNode.row;
                        runningCol = runningNode.col;
                        if (runningNode.isMarkedAsOutlet()) {
                            attributeValue = runningNode.getDoubleValueFromMap(attributeIter);
                            break;
                        }
                        runningNode = runningNode.goDownstream();
                    }
                    if (!isNovalue(attributeValue)) {
                        // run down marking the hills
                        runningNode = flowNode;
                        while( runningNode != null && runningNode.isValid() ) {
                            runningNode.setDoubleValueInMap(markedIter, attributeValue);
                            if (runningNode.isMarkedAsOutlet()) {
                                break;
                            }
                            runningNode = runningNode.goDownstream();
                        }
                    } else {
                        throw new ModelsIllegalargumentException(
                                "Could not find a value of the attributes map in the channel after point: " + runningCol + "/"
                                        + runningRow + ". Are you sure that everything leads to a channel or outlet?",
                                "MODELSENGINE", pm);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Verifies if the point is a source pixel in the supplied flow raster.
     *
     * @param flowIter the {@link RandomIter iterator} of the flowdirections.
     * @param col the col of the point to check.
     * @param row the row of the point to check.
     * @return true if the point identified by col and row is a source pixel.
     */
    public static boolean isSourcePixel( RandomIter flowIter, int col, int row ) {
        double flowDirection = flowIter.getSampleDouble(col, row, 0);
        if (flowDirection < 9.0 && flowDirection > 0.0) {
            for( int k = 1; k <= 8; k++ ) {
                if (flowIter.getSampleDouble(col + dirIn[k][1], row + dirIn[k][0], 0) == dirIn[k][2]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the flow direction value for a given point as indexes (i, j) of the dir matrix.
     *
     * @param i
     * @param j
     * @return
     */
    public static int getFlowDirection( int i, int j ) {
        int flow = -1;
        for( int k = 1; k < 9; k++ ) {
            if (ModelsSupporter.DIR[k][0] == i && ModelsSupporter.DIR[k][1] == j) {
                flow = k;
            }
        }
        return flow;
    }

    /**
     * Linear interpolation between two values
     *
     * @param data
     *            - matrix of values to interpolate
     * @param x
     *            - value to interpolate
     * @param nx
     *            - column of data in which you find the x values
     * @param ny
     *            - column of data in which you find the y values
     * @return
     */
    public static double widthInterpolate( double[][] data, double x, int nx, int ny ) {

        int rows = data.length;
        double xuno = 0, xdue = 0, yuno = 0, ydue = 0, y = 0;

        // if 0, interpolate between 0 and the first value of data
        if (x >= 0 && x < data[0][nx]) {
            xuno = 0;
            xdue = data[0][nx];
            yuno = 0;
            ydue = data[0][ny];
            y = ((ydue - yuno) / (xdue - xuno)) * (x - xuno) + yuno;
        }

        // if it is less than 0 and bigger than the maximum, throw error
        if (x > data[(rows - 1)][nx] || x < 0) {
            throw new RuntimeException(MessageFormat.format(
                    "Error in the interpolation algorithm: entering with x = {0} (min = 0.0 max = {1}", x, data[(rows - 1)][nx]));
        }

        /* trovo i valori limite entro i quali effettuo l'interpolazione lineare */
        for( int i = 0; i < rows - 1; i++ ) {

            if (x > data[i][nx] && x <= data[(i + 1)][nx]) {
                xuno = data[i][nx];
                xdue = data[(i + 1)][nx];
                yuno = data[i][ny];
                ydue = data[(i + 1)][ny];
                y = ((ydue - yuno) / (xdue - xuno)) * (x - xuno) + yuno;

            }
        }

        return y;
    }

    /**
     * Interpolates the width function in a given tp.
     *
     * @param data
     * @param tp
     * @return
     */
    public static double henderson( double[][] data, int tp ) {

        int rows = data.length;

        int j = 1, n = 0;
        double dt = 0, muno, mdue, a, b, x, y, ydue, s_uno, s_due, smax = 0, tstar;

        for( int i = 1; i < rows; i++ ) {

            if (data[i][0] + tp <= data[(rows - 1)][0]) {
                /**
                 * ***trovo parametri geometrici del segmento di retta y=muno
                 * x+a******
                 */

                muno = (data[i][1] - data[(i - 1)][1]) / (data[i][0] - data[(i - 1)][0]);
                a = data[i][1] - (data[i][0] + tp) * muno;

                /**
                 * ***trovo i valori di x per l'intersezione tra y=(muno x+tp)+a
                 * e y=mdue x+b ******
                 */
                for( j = 1; j <= (rows - 1); j++ ) {
                    mdue = (data[j][1] - data[(j - 1)][1]) / (data[j][0] - data[(j - 1)][0]);

                    b = data[j][1] - data[j][0] * mdue;
                    x = (a - b) / (mdue - muno);
                    y = mdue * x + b;
                    if (x >= data[(j - 1)][0] && x <= data[j][0] && x - tp >= data[(i - 1)][0] && x - tp <= data[i][0]) {

                        ydue = widthInterpolate(data, x - tp, 0, 1);
                        n++;

                        s_uno = widthInterpolate(data, x - tp, 0, 2);

                        s_due = widthInterpolate(data, x, 0, 2);

                        if (s_due - s_uno > smax) {
                            smax = s_due - s_uno;
                            dt = x - tp;
                            tstar = x;

                        }
                    }
                }

            }
        }
        return dt;

    }

    /**
     * The Gamma function.
     *
     * @param x
     * @return the calculated gamma function.
     */
    public static double gamma( double x ) {
        double tmp = (x - 0.5) * log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1) + 24.01409822 / (x + 2) - 1.231739516 / (x + 3)
                + 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
        double gamma = exp(tmp + log(ser * sqrt(2 * PI)));
        return gamma;
    }

    /**
     * Calculates the sum of the values of a specified quantity from every point to the outlet.
     *
     * <p>During the calculation the drainage directions are followed.</p>
     *
     * @param flowIter the map of flowdirections.
     * @param mapToSumIter the map for which to sum downstream.
     * @param width the width of the resulting map.
     * @param height the height of the resulting map.
     * @param upperThreshold the upper threshold, values above that are excluded.
     * @param lowerThreshold the lower threshold, values below that are excluded.
     * @param pm the monitor.
     * @return The map of downstream summed values.
     */
    public static WritableRaster sumDownstream( RandomIter flowIter, RandomIter mapToSumIter, int width, int height,
            Double upperThreshold, Double lowerThreshold, IHMProgressMonitor pm ) {
        final int[] point = new int[2];
        WritableRaster summedMapWR = CoverageUtilities.createWritableRaster(width, height, null, null, null);
        WritableRandomIter summedMapIter = RandomIterFactory.createWritable(summedMapWR, null);

        double uThres = Double.POSITIVE_INFINITY;
        if (upperThreshold != null) {
            uThres = upperThreshold;
        }
        double lThres = Double.NEGATIVE_INFINITY;
        if (lowerThreshold != null) {
            lThres = lowerThreshold;
        }

        pm.beginTask("Calculating downstream sum...", height);
        for( int r = 0; r < height; r++ ) {
            for( int c = 0; c < width; c++ ) {
                double mapToSumValue = mapToSumIter.getSampleDouble(c, r, 0);
                if (!isNovalue(flowIter.getSampleDouble(c, r, 0)) && //
                        mapToSumValue < uThres && //
                        mapToSumValue > lThres //
                ) {
                    point[0] = c;
                    point[1] = r;
                    while( flowIter.getSampleDouble(point[0], point[1], 0) < 9 && //
                            !isNovalue(flowIter.getSampleDouble(point[0], point[1], 0))
                            && (checkRange(mapToSumIter.getSampleDouble(point[0], point[1], 0), uThres, lThres)) ) {

                        double sumValue = summedMapIter.getSampleDouble(point[0], point[1], 0)
                                + mapToSumIter.getSampleDouble(c, r, 0);

                        summedMapIter.setSample(point[0], point[1], 0, sumValue);

                        // FlowNode flowNode = new FlowNode(flowIter, width, height, point[0],
                        // point[1]);
                        // FlowNode downStreamNode = flowNode.goDownstream();
                        // if (downStreamNode != null) {
                        // if (!downStreamNode.isMarkedAsOutlet()) {
                        // point[0] = downStreamNode.col;
                        // point[1] = downStreamNode.row;
                        // }
                        // } else {
                        // return null;
                        // }
                        if (!go_downstream(point, flowIter.getSampleDouble(point[0], point[1], 0)))
                            return null;
                    }

                    if (!isNovalue(flowIter.getSampleDouble(point[0], point[1], 0))) {
                        double summedMapValue = summedMapIter.getSampleDouble(point[0], point[1], 0);
                        if (!isNovalue(summedMapValue)) {
                            double sumValue = summedMapValue + mapToSumIter.getSampleDouble(c, r, 0);
                            summedMapIter.setSample(point[0], point[1], 0, sumValue);
                        }
                    }
                } else {
                    summedMapIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        return summedMapWR;
    }

    private static boolean checkRange( double value, double upper, double lower ) {
        if (value < upper && value > lower) {
            return true;
        }
        return false;
    }

    /**
     * Calculating the inverse of the sun vector.
     *
     * @param sunVector
     * @return
     */
    public static double[] calcInverseSunVector( double[] sunVector ) {
        double m = Math.max(Math.abs(sunVector[0]), Math.abs(sunVector[1]));
        return new double[]{-sunVector[0] / m, -sunVector[1] / m, -sunVector[2] / m};
    }

    /**
     * Calculating the normal to the sun vector.
     *
     * @param sunVector
     * @return
     */
    public static double[] calcNormalSunVector( double[] sunVector ) {
        double[] normalSunVector = new double[3];
        normalSunVector[2] = Math.sqrt(Math.pow(sunVector[0], 2) + Math.pow(sunVector[1], 2));
        normalSunVector[0] = -sunVector[0] * sunVector[2] / normalSunVector[2];
        normalSunVector[1] = -sunVector[1] * sunVector[2] / normalSunVector[2];
        return normalSunVector;
    }

    /**
     * Compute the dot product.
     *
     * @param a
     *            is a vector.
     * @param b
     *            is a vector.
     * @return the dot product of a and b.
     */
    public static double scalarProduct( double[] a, double[] b ) {
        double c = 0;
        for( int i = 0; i < a.length; i++ ) {
            c = c + a[i] * b[i];
        }
        return c;
    }
    /**
    * Evaluate the shadow map calling the shadow method.
    *
    * @param h
    *            the height of the raster.
    * @param w
    *            the width of the raster.
    * @param sunVector
    * @param inverseSunVector
    * @param normalSunVector
    * @param demWR
    *            the elevation map.
    * @param dx
    *            the resolution of the elevation map.
    * @return the shadow map.
    */
    public static WritableRaster calculateFactor( int h, int w, double[] sunVector, double[] inverseSunVector,
            double[] normalSunVector, WritableRaster demWR, double dx ) {

        double casx = 1e6 * sunVector[0];
        double casy = 1e6 * sunVector[1];
        int f_i = 0;
        int f_j = 0;

        if (casx <= 0) {
            f_i = 0;
        } else {
            f_i = w - 1;
        }

        if (casy <= 0) {
            f_j = 0;
        } else {
            f_j = h - 1;
        }

        WritableRaster sOmbraWR = CoverageUtilities.createWritableRaster(w, h, null, null, 1.0);
        int j = f_j;
        for( int i = 0; i < sOmbraWR.getWidth(); i++ ) {
            shadow(i, j, sOmbraWR, demWR, dx, normalSunVector, inverseSunVector);
        }
        int i = f_i;
        for( int k = 0; k < sOmbraWR.getHeight(); k++ ) {
            shadow(i, k, sOmbraWR, demWR, dx, normalSunVector, inverseSunVector);
        }
        return sOmbraWR;
    }

    /**
     * Evaluate the shadow map.
     *
     * @param i
     *            the x axis index.
     * @param j
     *            the y axis index.
     * @param tmpWR
     *            the output shadow map.
     * @param demWR
     *            the elevation map.
     * @param res
     *            the resolution of the elevation map.
     * @param normalSunVector
     * @param inverseSunVector
     * @return
     */
    private static WritableRaster shadow( int i, int j, WritableRaster tmpWR, WritableRaster demWR, double res,
            double[] normalSunVector, double[] inverseSunVector ) {
        int n = 0;
        double zcompare = -Double.MAX_VALUE;
        double dx = (inverseSunVector[0] * n);
        double dy = (inverseSunVector[1] * n);
        int nCols = tmpWR.getWidth();
        int nRows = tmpWR.getHeight();
        int idx = (int) Math.round(i + dx);
        int jdy = (int) Math.round(j + dy);
        double vectorToOrigin[] = new double[3];
        while( idx >= 0 && idx <= nCols - 1 && jdy >= 0 && jdy <= nRows - 1 ) {
            vectorToOrigin[0] = dx * res;
            vectorToOrigin[1] = dy * res;

            int tmpY = (int) (j + dy);
            if (tmpY < 0) {
                tmpY = 0;
            } else if (tmpY > nRows) {
                tmpY = nRows - 1;
            }
            int tmpX = (int) (i + dx);
            if (tmpX < 0) {
                tmpX = 0;
            } else if (tmpY > nCols) {
                tmpX = nCols - 1;
            }
            vectorToOrigin[2] = demWR.getSampleDouble(idx, jdy, 0);
            // vectorToOrigin[2] = (pitRandomIter.getSampleDouble(idx, jdy, 0) +
            // pitRandomIter
            // .getSampleDouble(tmpX, tmpY, 0)) / 2;
            double zprojection = scalarProduct(vectorToOrigin, normalSunVector);
            if ((zprojection < zcompare)) {
                tmpWR.setSample(idx, jdy, 0, 0);
            } else {
                zcompare = zprojection;
            }
            n = n + 1;
            dy = (inverseSunVector[1] * n);
            dx = (inverseSunVector[0] * n);
            idx = (int) Math.round(i + dx);
            jdy = (int) Math.round(j + dy);
        }
        return tmpWR;
    }

    /**
     * Verify if the current station (i) is already into the arrays.
     *
     * @deprecated This is no longer used.
     * @param xStation the x coordinate of the stations
     * @param yStation the y coordinate of the stations
     * @param zStation the z coordinate of the stations
     * @param hStation the h value of the stations
     * @param xTmp
     * @param yTmp
     * @param zTmp
     * @param hTmp
     * @param i the current index
     * @param doMean if the h value of a double station have different value then do the mean.
     * @return true if there is already this station.
     * @throws Exception
     */
    public static boolean verifyDoubleStation( double[] xStation, double[] yStation, double[] zStation, double[] hStation,
            double xTmp, double yTmp, double zTmp, double hTmp, int i, boolean doMean ) throws Exception {

        for( int j = 0; j < i - 1; j++ ) {

            if (dEq(xTmp, xStation[j]) && dEq(yTmp, yStation[j]) && dEq(zTmp, zStation[j]) && dEq(hTmp, hStation[j])) {
                if (!doMean) {
                    throw new IllegalArgumentException(msg.message("verifyStation.equalsStation1") + xTmp + "/" + yTmp);
                }
                return true;
            } else if (dEq(xTmp, xStation[j]) && dEq(yTmp, yStation[j]) && dEq(zTmp, zStation[j])) {
                if (!doMean) {
                    throw new IllegalArgumentException(msg.message("verifyStation.equalsStation2") + xTmp + "/" + yTmp);
                }
                if (!isNovalue(hStation[j]) && !isNovalue(hTmp)) {
                    hStation[j] = (hStation[j] + hTmp) / 2;
                } else {
                    hStation[j] = doubleNovalue;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Return the mean of a column of a matrix.
     *
     * @param matrix matrix of the value to calculate.
     * @param column index of the column to calculate the variance.
     * @return mean.
     */
    public static double meanDoublematrixColumn( double[][] matrix, int column ) {

        double mean;

        mean = 0;

        int length = matrix.length;
        for( int i = 0; i < length; i++ ) {
            mean += matrix[i][column];
        }

        return mean / length;

    }

    /**
     * Return the variance of a column of a matrix.
     *
     * @param matrix matrix of the value to calculate.
     * @param column index of the column to calculate the variance.
     * @param mean the mean value of the column.
     * @return variance.
     */
    public static double varianceDoublematrixColumn( double[][] matrix, int column, double mean )

    {

        double variance;

        variance = 0;

        for( int i = 0; i < matrix.length; i++ ) {
            variance += (matrix[i][column] - mean) * (matrix[i][column] - mean);
        }

        return variance / matrix.length;

    }

    /**
     * Sum columns.
     *
     * <p>
     * Store in a matrix (at index coulumn), the sum of the column of another
     * matrix. It's necessary to specify the initial and final index of the
     * coluns to sum.
     * </p>
     *
     *
     * @param coolIndex index of the matrix2 where to put the result.
     * @param matrixToSum contains the value to sum.
     * @param resultMatrix where to put the result.
     * @param firstRowIndex initial index of the colum to sum.
     * @param lastRowIndex final index of the colum to sum.
     * @return maximum value of the colum index of the matrix2.
     */
    public static double sumDoublematrixColumns( int coolIndex, double[][] matrixToSum, double[][] resultMatrix,
            int firstRowIndex, int lastRowIndex, IHMProgressMonitor pm ) {

        double maximum;

        maximum = 0;

        if (matrixToSum.length != resultMatrix.length) {
            pm.errorMessage(msg.message("trentoP.error.matrix")); //$NON-NLS-1$
            throw new ArithmeticException(msg.message("trentoP.error.matrix")); //$NON-NLS-1$
        }
        if (firstRowIndex < 0 || lastRowIndex < firstRowIndex) {
            pm.errorMessage(msg.message("trentoP.error.nCol")); //$NON-NLS-1$
            throw new ArithmeticException(msg.message("trentoP.error.nCol")); //$NON-NLS-1$
        }
        for( int i = 0; i < matrixToSum.length; ++i ) {
            resultMatrix[i][coolIndex] = 0; /* Initializes element */

            for( int j = firstRowIndex; j <= lastRowIndex; ++j ) {
                resultMatrix[i][coolIndex] += matrixToSum[i][j];
            }

            if (resultMatrix[i][coolIndex] >= maximum) /* Saves maximum value */
            {
                maximum = resultMatrix[i][coolIndex];
            }
        }

        return maximum;

    }

    /**
     * Calculates the distance of every pixel of the basin from the outlet (in meter),
     * calculated along the drainage directions
     *
     * @param flowIter the flow map.
     * @param pitIter the pit map (if available distance is calculated in 3d).
     * @param distanceToOutIter the resulting outlet distance map.
     * @param region the region parameters.
     * @param pm the monitor.
     */
    public static void topologicalOutletdistance( RandomIter flowIter, RandomIter pitIter, WritableRandomIter distanceToOutIter,
            RegionMap region, IHMProgressMonitor pm ) {
        int activeCols = region.getCols();
        int activeRows = region.getRows();
        double dx = region.getXres();
        double dy = region.getYres();
        int[] flow = new int[2];
        int[] flow_p = new int[2];
        double oldir = 0.0;
        double[] grid = new double[11];
        double count = 0.0;

        grid[0] = grid[9] = grid[10] = 0;
        grid[1] = grid[5] = abs(dx);
        grid[3] = grid[7] = abs(dy);
        grid[2] = grid[4] = grid[6] = grid[8] = sqrt(dx * dx + dy * dy);

        pm.beginTask("Calculating topological outlet distance...", activeRows);
        for( int r = 0; r < activeRows; r++ ) {
            for( int c = 0; c < activeCols; c++ ) {
                flow[0] = c;
                flow[1] = r;
                if (isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))) {
                    distanceToOutIter.setSample(flow[0], flow[1], 0, doubleNovalue);
                } else {
                    if (isSourcePixel(flowIter, flow[0], flow[1])) {
                        count = 0;
                        oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        flow_p[0] = flow[0];
                        flow_p[1] = flow[1];
                        go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && distanceToOutIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {

                            if (pitIter != null) {
                                double dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                        - pitIter.getSampleDouble(flow[0], flow[1], 0);
                                count += sqrt(pow(grid[(int) oldir], 2) + pow(dz, 2));
                            } else {
                                count += grid[(int) oldir];
                            }
                            oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                            flow_p[0] = flow[0];
                            flow_p[1] = flow[1];
                            go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        }
                        if (distanceToOutIter.getSampleDouble(flow[0], flow[1], 0) > 0) {
                            if (pitIter != null) {
                                double dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                        - pitIter.getSampleDouble(flow[0], flow[1], 0);
                                count += sqrt(pow(grid[(int) oldir], 2) + pow(dz, 2))
                                        + distanceToOutIter.getSampleDouble(flow[0], flow[1], 0);
                            } else {
                                count += grid[(int) oldir] + distanceToOutIter.getSampleDouble(flow[0], flow[1], 0);
                            }
                            distanceToOutIter.setSample(c, r, 0, count);
                        } else if (flowIter.getSampleDouble(flow[0], flow[1], 0) > 9) {
                            distanceToOutIter.setSample(flow[0], flow[1], 0, 0);

                            if (pitIter != null) {
                                double dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                        - pitIter.getSampleDouble(flow[0], flow[1], 0);
                                count += sqrt(pow(grid[(int) oldir], 2) + pow(dz, 2));
                            } else {
                                count += grid[(int) oldir];
                            }
                            distanceToOutIter.setSample(c, r, 0, count);
                        }

                        flow[0] = c;
                        flow[1] = r;
                        oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        flow_p[0] = flow[0];
                        flow_p[1] = flow[1];
                        go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && distanceToOutIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {
                            if (pitIter != null) {
                                double dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                        - pitIter.getSampleDouble(flow[0], flow[1], 0);
                                count -= sqrt(pow(grid[(int) oldir], 2) + pow(dz, 2));
                            } else {
                                count -= grid[(int) oldir];
                            }
                            if (count < 0) {
                                distanceToOutIter.setSample(flow[0], flow[1], 0, 0);
                            } else {
                                distanceToOutIter.setSample(flow[0], flow[1], 0, count);
                            }
                            oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                            flow_p[0] = flow[0];
                            flow_p[1] = flow[1];

                            go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Calculates the distance of every pixel of the basin from the outlet (in map units),
     * calculated along the drainage directions
     *
     * @param flowIter the flow map.
     * @param distanceToOutIter the resulting outlet distance map.
     * @param region the region parameters.
     * @param pm the monitor.
     */
    public static void outletdistance( RandomIter flowIter, WritableRandomIter distanceToOutIter, RegionMap region,
            IHMProgressMonitor pm ) {
        int cols = region.getCols();
        int rows = region.getRows();
        int[] flow = new int[2];
        double count = 0.0;

        pm.beginTask("Calculating outlet distance...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                flow[0] = c;
                flow[1] = r;
                if (isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))) {
                    distanceToOutIter.setSample(flow[0], flow[1], 0, doubleNovalue);
                } else {
                    flow[0] = c;
                    flow[1] = r;
                    if (isSourcePixel(flowIter, flow[0], flow[1])) {
                        count = 0;
                        go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && distanceToOutIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {
                            count += 1;
                            go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        }
                        if (distanceToOutIter.getSampleDouble(flow[0], flow[1], 0) > 0) {
                            count += 1 + distanceToOutIter.getSampleDouble(flow[0], flow[1], 0);
                            distanceToOutIter.setSample(c, r, 0, count);
                        } else if (flowIter.getSampleDouble(flow[0], flow[1], 0) > 9) {
                            distanceToOutIter.setSample(flow[0], flow[1], 0, 0);
                            count += 1;
                            distanceToOutIter.setSample(c, r, 0, count);
                        }

                        flow[0] = c;
                        flow[1] = r;
                        go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && distanceToOutIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {
                            count -= 1;
                            distanceToOutIter.setSample(flow[0], flow[1], 0, count);
                            go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Approximate a value to a multiple of a divisor value.
     *
     * <p>
     * It evaluates a multiple of the divisor (which is the nearest number to valueToApproximate).
     * </p>
     *
     * @param valueToApproximate value to approximate
     * @param divisor the unit value, it's the divisor number.
     * @return the largest value less than or equal to valueToApproximate and divisible to divisor.
     */
    public static double approximate2Multiple( double valueToApproximate, double divisor ) {
        return valueToApproximate - abs(valueToApproximate % divisor);
    }
}
