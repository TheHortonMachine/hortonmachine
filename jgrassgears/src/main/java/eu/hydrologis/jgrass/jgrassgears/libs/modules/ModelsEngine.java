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
package eu.hydrologis.jgrass.jgrassgears.libs.modules;

import static eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants.isNovalue;
import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;
import javax.vecmath.Point4d;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import eu.hydrologis.jgrass.jgrassgears.i18n.MessageHandler;
import eu.hydrologis.jgrass.jgrassgears.libs.exceptions.ModelsIllegalargumentException;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.IHMProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

public class ModelsEngine {
    private static int[][] DIR = ModelsSupporter.DIR;

    private MessageHandler msg = MessageHandler.getInstance();

    public static PixelInCell DEFAULTPIXELANCHOR = PixelInCell.CELL_CENTER;

    /**
     * Moves one pixel downstream.
     * 
     * @param rowCol
     *            the array containing the row and column of the current pixel.
     *            It will be modified here to represent the next downstream
     *            pixel.
     * @param flowdirection
     *            the current flowdirection number.
     * @return true if everything went well.
     */
    public boolean go_downstream( int[] rowCol, double flowdirection ) {

        int n = (int) flowdirection;
        if (n == 10) {
            return true;
        } else if (n < 1 || n > 9) {
            return false;
        } else {
            rowCol[1] += DIR[n][0];
            rowCol[0] += DIR[n][1];
            return true;
        }
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
    public FeatureCollection<SimpleFeatureType, SimpleFeature> net2ShapeOnly(
            RenderedImage flowImage, WritableRaster netNumImage, GridGeometry2D gridGeometry,
            List<Integer> nstream, IHMProgressMonitor pm ) throws IOException, TransformException {

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
                                double[] worldPosition = gridGeometry.gridToWorld(
                                        new GridCoordinates2D(flow[0], flow[1])).getCoordinate();
                                Coordinate coordSource = new Coordinate(worldPosition[0],
                                        worldPosition[1]);
                                // creates new Object Coordinate... SOURCE
                                // POINT...
                                // adds the points to the CoordinateList
                                coordlist.add(coordSource);
                                if (!go_downstream(flow, m1RandomIter.getSampleDouble(flow[0],
                                        flow[1], 0)))
                                    return null;
                                // it extracts the other points of the
                                // channel... it
                                // continues until the next node...
                                while( !isNovalue(m1RandomIter.getSampleDouble(flow[0], flow[1], 0))
                                        && m1RandomIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                        && netNumRandomIter.getSampleDouble(flow[0], flow[1], 0) == num
                                        && !isNovalue(netNumRandomIter.getSampleDouble(flow[0],
                                                flow[1], 0)) ) {
                                    worldPosition = gridGeometry.gridToWorld(
                                            new GridCoordinates2D(flow[0], flow[1]))
                                            .getCoordinate();
                                    Coordinate coordPoint = new Coordinate(worldPosition[0],
                                            worldPosition[1]);
                                    // creates new Object Coordinate... CHANNEL
                                    // POINT...
                                    // adds new points to CoordinateList
                                    coordlist.add(coordPoint);
                                    flow_p[0] = flow[0];
                                    flow_p[1] = flow[1];
                                    if (!go_downstream(flow, m1RandomIter.getSampleDouble(flow[0],
                                            flow[1], 0)))
                                        return null;
                                }
                                worldPosition = gridGeometry.gridToWorld(
                                        new GridCoordinates2D(flow[0], flow[1])).getCoordinate();
                                Coordinate coordNode = new Coordinate(worldPosition[0],
                                        worldPosition[1]);
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
        // set the name
        b.setName("network"); //$NON-NLS-1$
        // add a geometry property
        b.add("the_geom", LineString.class); //$NON-NLS-1$
        // build the type
        SimpleFeatureType type = b.buildFeatureType();
        // create the feature
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = FeatureCollections
                .newCollection();
        int index = 0;
        for( LineString lineString : newGeometryVectorLine ) {
            Object[] values = new Object[]{lineString};
            // add the values
            builder.addAll(values);
            // build the feature with provided ID
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + index); //$NON-NLS-1$
            index++;
            featureCollection.add(feature);
        }
        return featureCollection;
    }

    /**
     * TODO port to be independent from JGrassRegion
     * 
     * It create the shapefile of channel network
     * 
     * @param flowIterator
     * @param netNumIterator
     * @param nstream
     * @param active
     * @param out
     * @return
     * @throws IOException
     */
    // public List<MultiLineString> net2ShapeGeometries( RandomIter flowIterator,
    // RandomIter netNumIterator, int[] nstream, JGrassRegion active, PrintStream out )
    // throws IOException {
    //
    // // get rows and cols from the active region
    // int activecols = active.getCols();
    // int activerows = active.getRows();
    // int[] flow = new int[2];
    // int[] flow_p = new int[2];
    //
    // CoordinateList coordlist = new CoordinateList();
    //
    // // GEOMETRY
    //
    // // creates a vector of geometry
    // List<MultiLineString> newGeometryVectorLine = new ArrayList<MultiLineString>();
    // GeometryFactory newfactory = new GeometryFactory();
    //
    // /* name of new geometry (polyline) */
    // PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(out, out);
    // pm.beginTask(msg.message("utils.extracting_network_geometries"), nstream[0]);
    // for( int num = 1; num <= nstream[0]; num++ ) {
    // for( int y = 0; y < activerows; y++ ) {
    // for( int x = 0; x < activecols; x++ ) {
    // flow[0] = x;
    // flow[1] = y;
    // // looks for the source
    // if (netNumIterator.getSampleDouble(x, y, 0) == num) {
    // // if the point is a source it starts to extract the
    // // channel...
    // if (sourcesNet(flowIterator, flow, num, netNumIterator)) {
    // flow_p[0] = flow[0];
    // flow_p[1] = flow[1];
    // Coordinate coordSource = JGrassUtilities.rowColToCenterCoordinates(
    // active, flow[1], flow[0]);
    // // creates new Object Coordinate... SOURCE POINT...
    // // adds the points to the CoordinateList
    // coordlist.add(coordSource);
    // if (!go_downstream(flow, flowIterator.getSampleDouble(flow[0], flow[1],
    // 0)))
    // return null;
    // // it extracts the other points of the channel... it
    // // continues until the next node...
    // while( !isNovalue(flowIterator.getSampleDouble(flow[0], flow[1], 0))
    // && flowIterator.getSampleDouble(flow[0], flow[1], 0) != 10.0
    // && netNumIterator.getSampleDouble(flow[0], flow[1], 0) == num
    // && !isNovalue(netNumIterator.getSampleDouble(flow[0], flow[1],
    // 0)) ) {
    // Coordinate coordPoint = JGrassUtilities.rowColToCenterCoordinates(
    // active, flow[1], flow[0]);
    // // creates new Object Coordinate... CHANNEL
    // // POINT...
    // // adds new points to CoordinateList
    // coordlist.add(coordPoint);
    // flow_p[0] = flow[0];
    // flow_p[1] = flow[1];
    // if (!go_downstream(flow, flowIterator.getSampleDouble(flow[0],
    // flow[1], 0)))
    // return null;
    // }
    // Coordinate coordNode = JGrassUtilities.rowColToCenterCoordinates(
    // active, flow[1], flow[0]);
    // // creates new Object Coordinate... NODE POINT...
    // // adds new points to CoordinateList
    // coordlist.add(coordNode);
    // }
    // }
    // }
    // }
    // // when the channel is complete creates one new geometry (new
    // // channel of the network)
    // // adds the new geometry to the vector of geometry
    // // if (!coordlist.isEmpty()) {
    // newGeometryVectorLine.add(newfactory.createMultiLineString(new LineString[]{newfactory
    // .createLineString(coordlist.toCoordinateArray())}));
    // // } else {
    // // if (out != null)
    // // out.println("Found an empty geometry at " + num);
    // // }
    // // it removes every element of coordlist
    // coordlist.clear();
    // pm.worked(1);
    // }
    // pm.done();
    // return newGeometryVectorLine;
    // }

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
    public boolean sourcesNet( RandomIter flowIterator, int[] colRow, int num, RandomIter netNum ) {
        int[][] dir = {{0, 0, 0}, {1, 0, 5}, {1, -1, 6}, {0, -1, 7}, {-1, -1, 8}, {-1, 0, 1},
                {-1, 1, 2}, {0, 1, 3}, {1, 1, 4}};

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
    public double[] vectorizeDoubleMatrix( RenderedImage input ) {
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
     * @param N
     * @param num_max
     * @param out
     * @return
     */
    public double split2realvectors( double[] U, double[] T, SplitVectors theSplit, int N,
            int num_max, IHMProgressMonitor pm ) {

        double delta = 0, min, max;
        int i, count, count1, minposition = 0, maxposition, bin_vuoti;
        int[] bins;
        int head = 0;

        // minposition = 0; //pippo 1;
        // maxposition = U.length - 1; //pippo U.length;
        bins = new int[U.length];

        if (N <= 1) {
            count1 = 1;
            count = 1;

            int index = 0;
            while( count < U.length ) {

                while( U[count] == U[count - 1] && count <= U.length ) {
                    count++;
                }

                index++;
                bins[index] = count - count1;
                head++;
                count1 = count;
                count++;
                if (head > num_max)
                    throw new ModelsIllegalargumentException(
                            "The number of bin eccedes the maximum number allowed.", "MODEL");
            }

        } else if (N > 1) {

            minposition = 0; // here Ricci had made it be two (i.e. 1 for no
            // fluidturtles) WHY?!?!?
            max = U[U.length - 1]; // pippo
            while( isNovalue(U[minposition]) ) {
                minposition++;
            }
            min = U[minposition];
            maxposition = U.length - 1;

            delta = (max - min) / (N - 1);

            int index = 0;

            count1 = minposition; // the novalues are already left aside
            count = minposition;
            bin_vuoti = 0;

            while( count < maxposition ) {

                if (U[count] < min + 0.5 * delta) {
                    while( U[count] < min + 0.5 * delta && count < maxposition ) {
                        count++;
                    }

                    bins[index] = count - count1; // number of values
                    // contained in the bin
                    index++; // starts from position 1!!!

                    head++;
                    count1 = count;
                    count++;

                } else {
                    bin_vuoti++;
                }
                min += delta;
            }

            if (bin_vuoti != 0) {
                pm.message(bin_vuoti + " empty bins where found");
            }

        }

        if (head < 1) {
            throw new ModelsIllegalargumentException("Something wrong happened in binning", "MODEL");
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

            int index = minposition;
            for( int j = 0; j < head; j++ ) {
                for( int k = 0; k < theSplit.splitIndex[j]; k++ ) {
                    theSplit.splitValues1[j][k] = U[index];
                    theSplit.splitValues2[j][k] = T[index];
                    index++;
                }
            }
        }

        if (N < 2)
            delta = 0;

        return delta;
    }

    public double doubleNMoment( double[] m, int nh, double mean, double NN, IHMProgressMonitor pm ) {
        double moment = 0.0, n;

        n = 0;

        if (NN == 1.0) {

            for( int i = 0; i < nh; i++ ) {

                if (!isNovalue(m[i])) {
                    moment += m[i];
                    n++;
                }

            }

            if (n >= 1) {
                moment /= n;
            } else {
                pm.errorMessage("No valid data were processed, setting moment value to zero.");
                moment = 0.0;
            }

        } else if (NN == 2.0) {
            for( int i = 0; i < nh; i++ ) {
                if (!isNovalue(m[i])) {
                    moment += (m[i]) * (m[i]);
                    n++;
                }

            }
            if (n >= 1) {
                moment = (moment / n - mean * mean);
            } else {
                pm.errorMessage("No valid data were processed, setting moment value to zero.");
                moment = 0.0;
            }

        } else {
            for( int i = 0; i < nh; i++ ) {
                if (!isNovalue(m[i])) {
                    moment += pow((m[i] - mean), NN);
                    n++;
                }

            }
            if (n >= 1) {
                moment /= n;

            } else {
                pm.errorMessage("No valid data were processed, setting moment value to zero.");
                moment = 0.0;
            }

        }

        return moment;
    }

    /**
     * this method numerating every stream
     */
    public WritableRaster netNumbering( List<Integer> nstream, RandomIter flowIter,
            RandomIter networkIter, int width, int height, IHMProgressMonitor pm ) {
        int[] flow = new int[2];
        int gg = 0, n = 0, f;
        WritableRaster netnumWR = CoverageUtilities.createDoubleWritableRaster(width, height, null,
                null, null);
        WritableRandomIter netnumIter = RandomIterFactory.createWritable(netnumWR, null);

        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        /* numerating every stream */
        MessageHandler msg = MessageHandler.getInstance();
        pm.beginTask(msg.message("utils.numbering_stream"), height);
        for( int j = 0; j < height; j++ ) {
            for( int i = 0; i < width; i++ ) {
                flow[0] = i;
                flow[1] = j;
                if (!isNovalue(networkIter.getSampleDouble(i, j, 0))
                        && flowIter.getSampleDouble(i, j, 0) != 10.0
                        && netnumIter.getSampleDouble(i, j, 0) == 0.0) {
                    f = 0;
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == dir[k][2]
                                && !isNovalue(networkIter.getSampleDouble(flow[0] + dir[k][1],
                                        flow[1] + dir[k][0], 0))) {
                            break;
                        } else
                            f++;
                    }
                    // if the pixel is a source...
                    if (f == 8) {
                        n++;
                        nstream.add(n);
                        netnumIter.setSample(i, j, 0, n);
                        if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && netnumIter.getSampleDouble(flow[0], flow[1], 0) == 0 ) {
                            gg = 0;
                            for( int k = 1; k <= 8; k++ ) {
                                if (!isNovalue(networkIter.getSampleDouble(flow[0] + dir[k][1],
                                        flow[1] + dir[k][0], 0))
                                        && flowIter.getSampleDouble(flow[0] + dir[k][1], flow[1]
                                                + dir[k][0], 0) == dir[k][2]) {
                                    gg++;
                                }
                            }
                            if (gg >= 2) {
                                n++;
                                nstream.add(n);
                                netnumIter.setSample(flow[0], flow[1], 0, n);
                            } else {
                                netnumIter.setSample(flow[0], flow[1], 0, n);
                            }
                            if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        return netnumWR;
    }

    /**
     * this method numerating every stream and subdivide the stream when tca is
     * greater than a threshold
     */
    public WritableRaster netNumberingWithTca( List<Integer> nstream, RandomIter mRandomIter,
            RandomIter netRandomIter, RandomIter tcaRandomIter, int cols, int rows, double tcaTh,
            IHMProgressMonitor pm ) {
        int[] flow = new int[2];
        int gg = 0, n = 0, f;

        WritableRaster outImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null,
                null, null);
        WritableRandomIter oMatrixRandomIter = RandomIterFactory.createWritable(outImage, null);

        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        double tcaValue = 0;

        pm.beginTask(msg.message("utils.numbering_stream"), rows);
        /* numerating every stream */
        for( int j = 0; j < rows; j++ ) {
            // ShowPercent.getPercent(copt, i, rows - 1, 1);
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                if (!isNovalue(netRandomIter.getSampleDouble(i, j, 0))
                        && mRandomIter.getSampleDouble(i, j, 0) != 10.0
                        && oMatrixRandomIter.getSampleDouble(i, j, 0) == 0.0) {
                    f = 0;
                    for( int k = 1; k <= 8; k++ ) {
                        if (mRandomIter
                                .getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == dir[k][2]
                                && !isNovalue(netRandomIter.getSampleDouble(flow[0] + dir[k][1],
                                        flow[1] + dir[k][0], 0))) {
                            break;
                        } else
                            f++;
                    }
                    // if the pixel is a source...
                    if (f == 8) {
                        n++;
                        nstream.add(n);
                        tcaValue = tcaRandomIter.getSampleDouble(i, j, 0);
                        oMatrixRandomIter.setSample(i, j, 0, n);
                        if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        while( !isNovalue(mRandomIter.getSampleDouble(flow[0], flow[1], 0))
                                && oMatrixRandomIter.getSampleDouble(flow[0], flow[1], 0) == 0 ) {
                            gg = 0;
                            for( int k = 1; k <= 8; k++ ) {
                                if (!isNovalue(netRandomIter.getSampleDouble(flow[0] + dir[k][1],
                                        flow[1] + dir[k][0], 0))
                                        && mRandomIter.getSampleDouble(flow[0] + dir[k][1], flow[1]
                                                + dir[k][0], 0) == dir[k][2]) {
                                    gg++;
                                }
                            }
                            if (gg >= 2) {
                                // it is a node
                                n++;
                                nstream.add(n);
                                oMatrixRandomIter.setSample(flow[0], flow[1], 0, n);
                                tcaValue = tcaRandomIter.getSampleDouble(flow[0], flow[1], 0);
                            } else if (tcaRandomIter.getSampleDouble(flow[0], flow[1], 0)
                                    - tcaValue > tcaTh) {
                                // tca greater than threshold
                                n++;
                                nstream.add(n);
                                oMatrixRandomIter.setSample(flow[0], flow[1], 0, n);
                                tcaValue = tcaRandomIter.getSampleDouble(flow[0], flow[1], 0);
                            } else {
                                // normal point
                                oMatrixRandomIter.setSample(flow[0], flow[1], 0, n);
                            }
                            if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1],
                                    0)))
                                return null;
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        return outImage;
    }

    /**
     * this method numerating every stream dividing the channels in fixed points
     * @throws TransformException 
     * @throws InvalidGridGeometryException 
     */
    public WritableRaster netNumberingWithPoints( List<Integer> nstream, RandomIter mRandomIter,
            RandomIter netRandomIter, int rows, int cols,
            List<HashMap<String, ? >> attributePoints, List<Geometry> geomVect,
            GridGeometry2D gridGeometry, IHMProgressMonitor pm )
            throws InvalidGridGeometryException, TransformException {
        int[] flow = new int[2];
        int gg = 0, n = 0, f;
        WritableRaster outImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null,
                null, null);
        WritableRandomIter oMatrixRandomIter = RandomIterFactory.createWritable(outImage, null);

        Rectangle2D regionBox = gridGeometry.getEnvelope2D().getBounds2D();

        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        List<Point4d> points = new ArrayList<Point4d>();
        // new rectangle for active region
        Number nodoId;
        int l = 0;
        int numGeometry = 0;
        // insert the points in a Vector of points
        for( Geometry pointV : geomVect ) {
            for( int i = 0; i < pointV.getNumGeometries(); i++ ) {
                GridCoordinates2D gridCoordinate = gridGeometry.worldToGrid(new DirectPosition2D(
                        pointV.getCoordinates()[0].x, pointV.getCoordinates()[0].y));
                nodoId = (Number) attributePoints.get(numGeometry).get("RETE_ID");
                if (nodoId == null) {
                    throw new ModelsIllegalargumentException("Field RETE_ID not found", "");
                }
                if (nodoId.intValue() != -1
                        && regionBox.contains(new Point2D.Double(pointV.getCoordinates()[0].x,
                                pointV.getCoordinates()[0].y))) {
                    points.add(new Point4d(gridCoordinate.x, gridCoordinate.y,
                            nodoId.doubleValue(), 0));
                    l++;
                }
            }
            numGeometry++;
        }
        // if the points isn't on the channel net, move the point
        int p = 0;
        for( Point4d point4d : points ) {
            if (netRandomIter.getSampleDouble((int) point4d.x, (int) point4d.y, 0) != point4d.z) {
                for( int i = 1; i < 9; i++ ) {
                    int indexI = (int) point4d.x + dir[i][0];
                    int indexJ = (int) point4d.y + dir[i][1];
                    if (netRandomIter.getSampleDouble(indexJ, indexI, 0) == point4d.z) {
                        point4d.x = indexI;
                        point4d.y = indexJ;
                    }
                }
            }
        }
        for( Point4d point4d : points ) {
            if (netRandomIter.getSampleDouble((int) point4d.y, (int) point4d.x, 0) == point4d.z) {
                p++;
            }
        }

        pm.beginTask(msg.message("utils.numbering_stream"), rows);
        /* Selects every node and go downstream */
        for( int j = 0; j < rows; j++ ) {
            // ShowPercent.getPercent(copt, i, rows - 1, 1);
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                if (!isNovalue(netRandomIter.getSampleDouble(i, j, 0))
                        && mRandomIter.getSampleDouble(i, j, 0) != 10.0
                        && oMatrixRandomIter.getSampleDouble(i, j, 0) == 0.0) {
                    f = 0;
                    // look for the source...
                    for( int k = 1; k <= 8; k++ ) {
                        if (mRandomIter
                                .getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == dir[k][2]
                                && !isNovalue(netRandomIter.getSampleDouble(flow[0] + dir[k][0],
                                        flow[1] + dir[k][1], 0))) {
                            break;
                        } else
                            f++;
                    }
                    // if the pixel is a source...starts to assigne a number to
                    // every stream
                    if (f == 8) {
                        n++;
                        nstream.add(n);
                        oMatrixRandomIter.setSample(i, j, 0, n);
                        if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        for( Point4d point4d : points ) {
                            if (point4d.x == flow[1] && point4d.y == flow[0]) {
                                n++;
                                nstream.add(n);
                                point4d.w = n - 1;
                                /*
                                 * omatrix.getSampleDouble(i,j) = n; if
                                 * (!FluidUtils.go_downstream(flow,
                                 * m.getSampleDouble(flow[0],flow[1]), copt)) ;
                                 */
                            }
                        }
                        while( !isNovalue(mRandomIter.getSampleDouble(flow[0], flow[1], 0))
                                && oMatrixRandomIter.getSampleDouble(flow[0], flow[1], 0) == 0
                                && mRandomIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                            gg = 0;
                            for( int k = 1; k <= 8; k++ ) {
                                if (!isNovalue(netRandomIter.getSampleDouble(flow[0] + dir[k][0],
                                        flow[1] + dir[k][1], 0))
                                        && mRandomIter.getSampleDouble(flow[0] + dir[k][0], flow[1]
                                                + dir[k][1], 0) == dir[k][2]) {
                                    gg++;
                                }
                            }
                            if (gg >= 2) {
                                n++;
                                nstream.add(n);
                                oMatrixRandomIter.setSample(flow[0], flow[1], 0, n);
                            } else {
                                oMatrixRandomIter.setSample(flow[0], flow[1], 0, n);
                            }
                            if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1],
                                    0)))
                                return null;
                            for( Point4d point4d : points ) {
                                if (point4d.x == flow[1] && point4d.y == flow[0]) {
                                    n++;
                                    nstream.add(n);
                                    point4d.w = n - 1;
                                }
                            }
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        return outImage;
    }

    /**
     * this method numerating every stream dividing the channels in fixed points
     * @param out 
     * @throws TransformException 
     * @throws InvalidGridGeometryException 
     */
    public WritableRaster netNumberingWithPointsAndTca( List<Integer> nstream,
            RandomIter mRandomIter, RandomIter netRandomIter, RandomIter tcaRandomIter,
            double tcaTh, int rows, int cols, List<HashMap<String, ? >> attributePoints,
            List<Geometry> geomVect, GridGeometry2D gridGeometry, IHMProgressMonitor pm )
            throws InvalidGridGeometryException, TransformException {
        int[] flow = new int[2];
        int gg = 0, n = 0, f;

        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        double tcaValue = 0;

        List<Point4d> points = new ArrayList<Point4d>();
        // new rectangle for active region
        Rectangle2D regionBox = gridGeometry.getEnvelope2D().getBounds2D();
        Number nodoId;
        int l = 0;
        int numGeometry = 0;
        // insert the points in a Vector of points
        for( Geometry pointV : geomVect ) {
            for( int i = 0; i < pointV.getNumGeometries(); i++ ) {
                GridCoordinates2D gridCoordinate = gridGeometry.worldToGrid(new DirectPosition2D(
                        pointV.getCoordinates()[0].x, pointV.getCoordinates()[0].y));
                nodoId = (Number) attributePoints.get(numGeometry).get("RETE_ID");
                if (nodoId == null) {
                    throw new ModelsIllegalargumentException("Field RETE_ID not found", "");
                }
                if (nodoId.intValue() != -1
                        && regionBox.contains(new Point2D.Double(pointV.getCoordinates()[0].x,
                                pointV.getCoordinates()[0].y))) {
                    points.add(new Point4d(gridCoordinate.x, gridCoordinate.y,
                            nodoId.doubleValue(), 0));
                    l++;
                }
            }
            numGeometry++;
        }
        // if the points isn't on the channel net, move the point
        int p = 0;

        WritableRaster outImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null,
                null, null);
        WritableRandomIter oRandomIter = RandomIterFactory.createWritable(outImage, null);

        for( Point4d point4d : points ) {
            if (netRandomIter.getSampleDouble((int) point4d.x, (int) point4d.y, 0) != point4d.z) {
                for( int i = 1; i < 9; i++ ) {
                    int indexI = (int) point4d.x + dir[i][0];
                    int indexJ = (int) point4d.y + dir[i][1];
                    if (netRandomIter.getSampleDouble(indexJ, indexI, 0) == point4d.z) {
                        point4d.x = indexI;
                        point4d.y = indexJ;
                    }
                }
            }
        }
        for( Point4d point4d : points ) {
            if (netRandomIter.getSampleDouble((int) point4d.y, (int) point4d.x, 0) == point4d.z) {
                p++;
            }
        }

        pm.beginTask(msg.message("utils.numbering_stream"), rows);
        /* Selects every node and go downstream */
        for( int j = 0; j < rows; j++ ) {
            // ShowPercent.getPercent(copt, i, rows - 1, 1);
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                if (!isNovalue(netRandomIter.getSampleDouble(i, j, 0))
                        && mRandomIter.getSampleDouble(i, j, 0) != 10.0
                        && oRandomIter.getSampleDouble(i, j, 0) == 0.0) {
                    f = 0;
                    // look for the source...
                    for( int k = 1; k <= 8; k++ ) {
                        if (mRandomIter
                                .getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == dir[k][2]
                                && !isNovalue(netRandomIter.getSampleDouble(flow[0] + dir[k][1],
                                        flow[1] + dir[k][0], 0))) {
                            break;
                        } else
                            f++;
                    }
                    // if the pixel is a source...starts to assigne a number to
                    // every stream
                    if (f == 8) {
                        n++;
                        nstream.add(n);
                        oRandomIter.setSample(i, j, 0, n);
                        if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        for( Point4d point4d : points ) {
                            if (point4d.x == flow[1] && point4d.y == flow[0]) {
                                n++;
                                nstream.add(n);
                                point4d.w = n - 1;
                                /*
                                 * omatrix.getValueAt(i,j) = n; if
                                 * (!FluidUtils.go_downstream(flow,
                                 * m.getValueAt(flow[0],flow[1]), copt)) ;
                                 */
                            }
                        }
                        while( !isNovalue(mRandomIter.getSampleDouble(flow[0], flow[1], 0))
                                && oRandomIter.getSampleDouble(flow[0], flow[1], 0) == 0
                                && mRandomIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                            gg = 0;
                            for( int k = 1; k <= 8; k++ ) {
                                if (!isNovalue(netRandomIter.getSampleDouble(flow[0] + dir[k][1],
                                        flow[1] + dir[k][0], 0))
                                        && mRandomIter.getSampleDouble(flow[0] + dir[k][1], flow[1]
                                                + dir[k][0], 0) == dir[k][2]) {
                                    gg++;
                                }
                            }
                            if (gg >= 2) {
                                // it is a node
                                n++;
                                oRandomIter.setSample(flow[0], flow[1], 0, n);
                                tcaValue = tcaRandomIter.getSampleDouble(flow[0], flow[1], 0);
                            } else if (tcaRandomIter.getSampleDouble(flow[0], flow[1], 0)
                                    - tcaValue > tcaTh) {
                                // tca greater than threshold
                                n++;
                                nstream.add(n);
                                oRandomIter.setSample(flow[0], flow[1], 0, n);
                                tcaValue = tcaRandomIter.getSampleDouble(flow[0], flow[1], 0);
                            } else {
                                // normal point
                                oRandomIter.setSample(flow[0], flow[1], 0, n);
                            }
                            if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1],
                                    0)))
                                return null;
                            for( Point4d point4d : points ) {
                                if (point4d.x == flow[1] && point4d.y == flow[0]) {
                                    n++;
                                    nstream.add(n);
                                    point4d.w = n - 1;
                                }
                            }
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        oRandomIter.done();
        tcaRandomIter.done();
        mRandomIter.done();
        netRandomIter.done();
        return outImage;
    }

    /**
     * Extract subbasin of a raster.
     * 
     * 
     * @param flowRandomIter
     *            usually is the flow map.
     * @param cols 
     * @param rows 
     * @param net
     *            the network map.
     * @param netNumber
     *            the netnumbering map.
     * @return the map of subbasin
     */
    public WritableRaster extractSubbasins( WritableRandomIter flowRandomIter,
            RandomIter netRandomIter, WritableRandomIter netNumberRandomIter, int rows, int cols,
            IHMProgressMonitor pm ) {

        for( int l = 0; l < rows; l++ ) {
            for( int k = 0; k < cols; k++ ) {
                if (!isNovalue(netRandomIter.getSampleDouble(k, l, 0)))
                    flowRandomIter.setSample(k, l, 0, 10);
            }
        }

        WritableRaster subbImage = go2channel(flowRandomIter, netNumberRandomIter, cols, rows, pm);

        WritableRandomIter subbRandomIter = RandomIterFactory.createWritable(subbImage, null);

        for( int l = 0; l < rows; l++ ) {
            for( int k = 0; k < cols; k++ ) {
                if (!isNovalue(netRandomIter.getSampleDouble(k, l, 0)))
                    subbRandomIter.setSample(k, l, 0, netNumberRandomIter.getSampleDouble(k, l, 0));
                if (netNumberRandomIter.getSampleDouble(k, l, 0) == 0)
                    netNumberRandomIter.setSample(k, l, 0, HMConstants.doubleNovalue);
                if (subbRandomIter.getSampleDouble(k, l, 0) == 0)
                    subbRandomIter.setSample(k, l, 0, HMConstants.doubleNovalue);
            }
        }

        return subbImage;
    }

    /**
     * @param flowImage
     * @param att
     * @param dist
     */
    public WritableRaster go2channel( RandomIter mRandomIter, RandomIter attRandomIter, int cols,
            int rows, IHMProgressMonitor pm ) {
        int[] flow = new int[2];
        double value = 0.0;

        WritableRaster dist = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null,
                null);
        WritableRandomIter distIter = RandomIterFactory.createWritable(dist, null);

        pm.beginTask("Calculating the distance along the flowstream...", rows - 2);
        for( int j = 1; j < rows - 1; j++ ) {
            for( int i = 1; i < cols - 1; i++ ) {
                flow[0] = i;
                flow[1] = j;

                // Rectangle aroundSample = new Rectangle(i - 1, j - 1, 3, 3);
                // Raster aroundRaster = flowImage.getData(aroundSample);

                if (!isNovalue(mRandomIter.getSampleDouble(flow[0], flow[1], 0))
                        && isSourcePixel(mRandomIter, flow[0], flow[1])) {
                    while( !isNovalue(mRandomIter.getSampleDouble(flow[0], flow[1], 0))
                            && mRandomIter.getSampleDouble(flow[0], flow[1], 0) != 10.0 ) {
                        if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                    }

                    if (isNovalue(mRandomIter.getSampleDouble(flow[0], flow[1], 0))) {
                        throw new ModelsIllegalargumentException(
                                "No proper outlets were found in the flow file", "");
                    } else if (mRandomIter.getSampleDouble(flow[0], flow[1], 0) == 10) {
                        value = attRandomIter.getSampleDouble(flow[0], flow[1], 0);
                    }
                    flow[0] = i;
                    flow[1] = j;
                    distIter.setSample(i, j, 0, value);
                    while( !isNovalue(mRandomIter.getSampleDouble(flow[0], flow[1], 0))
                            && mRandomIter.getSampleDouble(flow[0], flow[1], 0) != 10.0 ) {
                        distIter.setSample(flow[0], flow[1], 0, value);
                        if (!go_downstream(flow, mRandomIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        return dist;
    }

    /**
     * Verifies if the point is a source pixel in the supplied flow raster.
     * 
     * @param flowRaster 
     * @param colRow the col and row of the point to check.
     * @return
     */
    public boolean isSourcePixel( RandomIter flowRaster, int col, int row ) {
        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;
        if (flowRaster.getSampleDouble(col, row, 0) < 9.0
                && flowRaster.getSampleDouble(col, row, 0) > 0.0) {

            for( int k = 1; k <= 8; k++ ) {
                if (flowRaster.getSampleDouble(col + dir[k][1], row + dir[k][0], 0) == dir[k][2]) {
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
    public double width_interpolate( double[][] data, double x, int nx, int ny ) {

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
            throw new RuntimeException("Error in the interpolation algorithm");
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
    public double henderson( double[][] data, int tp ) {

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
                    y = muno * x + a;
                    if (x >= data[(j - 1)][0] && x <= data[j][0] && x - tp >= data[(i - 1)][0]
                            && x - tp <= data[i][0]) {

                        ydue = width_interpolate(data, x - tp, 0, 1);
                        n++;

                        s_uno = width_interpolate(data, x - tp, 0, 2);

                        s_due = width_interpolate(data, x, 0, 2);

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
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1) + 24.01409822 / (x + 2)
                - 1.231739516 / (x + 3) + 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
        double gamma = exp(tmp + log(ser * sqrt(2 * PI)));
        return gamma;
    }
}
