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
package org.hortonmachine.gears.modules.r.houghes;

import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.geom.Point2D;

import javax.media.jai.iterator.RandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ThreadedRunnable;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Description(OmsHoughCirclesRaster.DESCRIPTIO)
@Author(name = OmsHoughCirclesRaster.AUTHORS, contact = "")
@Keywords(OmsHoughCirclesRaster.KEYWORDS)
@Label(HMConstants.RASTERPROCESSING)
@Name(OmsHoughCirclesRaster.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsHoughCirclesRaster extends HMModel {

    @Description(inRaster_DESCR)
    @In
    public GridCoverage2D inRaster;

    @Description(pMinRadius_DESCR)
    @Unit("m")
    @In
    public Double pMinRadius;

    @Description(pMaxRadius_DESCR)
    @Unit("m")
    @In
    public Double pMaxRadius;

    @Description(pRadiusIncrement_DESCR)
    @Unit("m")
    @In
    public Double pRadiusIncrement;

    @Description(pMaxCircleCount_DESCR)
    @In
    public int pMaxCircleCount = 50;

    @Description("Optional novalue in case of non physical data")
    @In
    public Integer pColorNv = 255;

    @Description(outCircles_DESCR)
    @In
    public SimpleFeatureCollection outCircles;

    // VARS DESCR START
    public static final String NAME = "houghcirclesraster";
    public static final String KEYWORDS = "Hough, circle";
    public static final String AUTHORS = "Hemerson Pistori (pistori@ec.ucdb.br) and Eduardo Rocha Costa, Mark A. Schulze (http://www.markschulze.net/), Andrea Antonello (www.hydrologis.com)";
    public static final String DESCRIPTIO = "Hough Transform implementation.";

    public static final String outCircles_DESCR = "The output circles.";
    public static final String pMaxCircleCount_DESCR = "The maximum circle count to look for.";
    public static final String pRadiusIncrement_DESCR = "The radius increment to use.";
    public static final String pMaxRadius_DESCR = "The maximum radius to look for.";
    public static final String pMinRadius_DESCR = "The minimum radius to look for.";
    public static final String inRaster_DESCR = "The input raster.";
    // VARS DESCR END

    private int radiusMinPixel; // Find circles with radius grater or equal radiusMin
    private int radiusMaxPixel; // Find circles with radius less or equal radiusMax
    private int radiusIncPixel; // Increment used to go from radiusMin to radiusMax
    private int maxCircles; // Numbers of circles to be found

    private byte imageValues[]; // Raw image (returned by ip.getPixels())
    private int width; // Hough Space width (depends on image width)
    private int height; // Hough Space heigh (depends on image height)
    private int depth; // Hough Space depth (depends on radius interval)
    private int offset; // Image Width
    private int offx; // ROI x offset
    private int offy; // ROI y offset
    private int lut[][][]; // LookUp Table for rsin e rcos values

    private double xRes;
    private double referenceImageValue = HMConstants.doubleNovalue;

    private boolean useColorNv = false;
    private int colorNv;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pMinRadius, pMaxRadius, pRadiusIncrement);

        if (pColorNv != null) {
            colorNv = pColorNv;
            useColorNv = true;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        offx = 0;
        offy = 0;
        width = regionMap.getCols();
        height = regionMap.getRows();
        offset = width;
        xRes = regionMap.getXres();

        radiusMinPixel = (int) round(width * pMinRadius / (regionMap.getEast() - regionMap.getWest()));
        radiusMaxPixel = (int) round(width * pMaxRadius / (regionMap.getEast() - regionMap.getWest()));;
        radiusIncPixel = (int) round(width * pRadiusIncrement / (regionMap.getEast() - regionMap.getWest()));
        if (radiusIncPixel < 1) {
            radiusIncPixel = 1;
        }

        maxCircles = pMaxCircleCount;
        depth = ((radiusMaxPixel - radiusMinPixel) / radiusIncPixel) + 1;

        Geometry[] circles = getCircles();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("houghcircles");
        b.setCRS(inRaster.getCoordinateReferenceSystem());
        b.add("the_geom", Polygon.class);
        b.add("value", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        DefaultFeatureCollection outFC = new DefaultFeatureCollection();
        for( Geometry geometry : circles ) {
            Object[] values = new Object[]{geometry, referenceImageValue};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            outFC.add(feature);
        }

        outCircles = outFC;
    }

    public Geometry[] getCircles() throws Exception {

        RandomIter renderedImageIterator = CoverageUtilities.getRandomIterator(inRaster);
        imageValues = new byte[width * height];

        int count = 0;
        for( int r = 0; r < height; r++ ) {
            for( int c = 0; c < width; c++ ) {
                if (useColorNv) {
                    int sample = renderedImageIterator.getSample(c, r, 0);
                    imageValues[count++] = (sample == colorNv) ? (byte) 0 : (byte) 1;
                } else {
                    double sample = renderedImageIterator.getSampleDouble(c, r, 0);
                    if (HMConstants.isNovalue(sample)) {
                        imageValues[count++] = (byte) 0;
                    } else {
                        referenceImageValue = sample;
                        imageValues[count++] = (byte) 1;
                    }
                }
            }
        }
        renderedImageIterator.done();

        double[][][] houghValues = houghTransform();
        Coordinate[] centerPoints = getCenterPoints(houghValues, maxCircles);
        Geometry[] geoms = new Geometry[centerPoints.length];
        GridGeometry2D gridGeometry = inRaster.getGridGeometry();
        for( int i = 0; i < centerPoints.length; i++ ) {
            Coordinate c = centerPoints[i];
            Point2D world = CoverageUtilities.gridToWorld(gridGeometry, (int) c.x, (int) c.y);
            double radius = c.z * xRes;
            Coordinate w = new Coordinate(world.getX(), world.getY(), radius);
            Point point = gf.createPoint(w);
            Geometry circle = point.buffer(radius);
            geoms[i] = circle;
        }

        return geoms;
    }

    /** 
     *  The parametric equation for a circle centered at (a,b) with
     *   radius r is:
     *
     *   a = x - r*cos(theta)
     *   b = y - r*sin(theta)
     *
     *   In order to speed calculations, we first construct a lookup
     *   table (lut) containing the rcos(theta) and rsin(theta) values, for
     *   theta varying from 0 to 2*PI with increments equal to
     *   1/8*r. As of now, a fixed increment is being used for all
     *   different radius (1/8*radiusMin). This should be corrected in
     *   the future.
     *
     *   Return value = Number of angles for each radius
     *  
     */
    private int buildLookUpTable() {
        int i = 0;
        int incDen = Math.round(8F * radiusMinPixel); // increment denominator
        lut = new int[2][incDen][depth];
        for( int radius = radiusMinPixel; radius <= radiusMaxPixel; radius = radius + radiusIncPixel ) {
            i = 0;
            for( int incNun = 0; incNun < incDen; incNun++ ) {
                double angle = (2 * Math.PI * (double) incNun) / (double) incDen;
                int indexR = (radius - radiusMinPixel) / radiusIncPixel;
                int rcos = (int) Math.round((double) radius * Math.cos(angle));
                int rsin = (int) Math.round((double) radius * Math.sin(angle));
                if ((i == 0) | (rcos != lut[0][i][indexR]) & (rsin != lut[1][i][indexR])) {
                    lut[0][i][indexR] = rcos;
                    lut[1][i][indexR] = rsin;
                    i++;
                }
            }
        }
        return i;
    }

    private double[][][] houghTransform() {
        int lutSize = buildLookUpTable();
        double[][][] houghValues = new double[width][height][depth];
        int k = width - 1;
        int l = height - 1;

        pm.beginTask("Hough transform...", l);
        for( int y = 1; y < l; y++ ) {
            if (pm.isCanceled()) {
                throw new ModelsRuntimeException("Module interrupted.", this);
            }
            for( int x = 1; x < k; x++ ) {
                for( int radius = radiusMinPixel; radius <= radiusMaxPixel; radius = radius + radiusIncPixel ) {
                    if (imageValues[(x + offx) + (y + offy) * offset] != 0) {// Edge pixel found
                        int indexR = (radius - radiusMinPixel) / radiusIncPixel;
                        for( int i = 0; i < lutSize; i++ ) {
                            int a = x + lut[1][i][indexR];
                            int b = y + lut[0][i][indexR];
                            if ((b >= 0) & (b < height) & (a >= 0) & (a < width)) {
                                houghValues[a][b][indexR] += 1;
                            }
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        return houghValues;

    }

    /** 
     * Search for a fixed number of circles.
     * 
     * @param houghValues the hough values.
     * @param maxCircles The number of circles that should be found.  
     * @return the center coordinates.
     */
    private Coordinate[] getCenterPoints( double[][][] houghValues, int maxCircles ) {

        Coordinate[] centerPoints = new Coordinate[maxCircles];
        int xMax = 0;
        int yMax = 0;
        int rMax = 0;

        pm.beginTask("Search for circles...", maxCircles);
        for( int c = 0; c < maxCircles; c++ ) {
            double counterMax = -1;
            for( int radius = radiusMinPixel; radius <= radiusMaxPixel; radius = radius + radiusIncPixel ) {

                int indexR = (radius - radiusMinPixel) / radiusIncPixel;
                for( int y = 0; y < height; y++ ) {
                    for( int x = 0; x < width; x++ ) {
                        if (houghValues[x][y][indexR] > counterMax) {
                            counterMax = houghValues[x][y][indexR];
                            xMax = x;
                            yMax = y;
                            rMax = radius;
                        }
                    }

                }
            }
            centerPoints[c] = new Coordinate(xMax, yMax, rMax);
            clearNeighbours(houghValues, xMax, yMax, rMax);
            pm.worked(1);
        }
        pm.done();
        return centerPoints;
    }

    /** 
     * Clear, from the Hough Space, all the counter that are near (radius/2) a previously found circle C.
     *  
     * @param x The x coordinate of the circle C found.
     * @param x The y coordinate of the circle C found.
     * @param x The radius of the circle C found.
     */
    private void clearNeighbours( double[][][] houghValues, int x, int y, int radius ) {
        // The following code just clean the points around the center of the circle found.
        double halfRadius = radius / 2.0F;
        double halfSquared = halfRadius * halfRadius;

        int y1 = (int) Math.floor((double) y - halfRadius);
        int y2 = (int) Math.ceil((double) y + halfRadius) + 1;
        int x1 = (int) Math.floor((double) x - halfRadius);
        int x2 = (int) Math.ceil((double) x + halfRadius) + 1;

        if (y1 < 0)
            y1 = 0;
        if (y2 > height)
            y2 = height;
        if (x1 < 0)
            x1 = 0;
        if (x2 > width)
            x2 = width;

        for( int r = radiusMinPixel; r <= radiusMaxPixel; r = r + radiusIncPixel ) {
            int indexR = (r - radiusMinPixel) / radiusIncPixel;
            for( int i = y1; i < y2; i++ ) {
                for( int j = x1; j < x2; j++ ) {
                    if (Math.pow(j - x, 2D) + Math.pow(i - y, 2D) < halfSquared) {
                        houghValues[j][i][indexR] = 0.0D;
                    }
                }
            }
        }

    }

    public static void main( String[] args ) throws Exception {

        ThreadedRunnable< ? > runner = new ThreadedRunnable(getDefaultThreadsNum(), null);

        int[] i = {2};// , 4, 6, 8, 10};
        for( int index : i ) {
            final int _index = index;
            runner.executeRunnable(new Runnable(){
                public void run() {
                    try {
                        String inRaster = "/home/hydrologis/data/rilievo_tls/avgres/las/vertical_slices/slice_" + _index
                                + ".0.asc";
                        String outShp = "/home/hydrologis/data/rilievo_tls/avgres/las/vertical_slices/slice_vector_" + _index
                                + ".0.shp";

                        GridCoverage2D src = OmsRasterReader.readRaster(inRaster);
                        OmsHoughCirclesRaster h = new OmsHoughCirclesRaster();
                        h.inRaster = src;
                        h.pMinRadius = 0.1;
                        h.pMaxRadius = 0.5;
                        h.pRadiusIncrement = 0.01;
                        h.pMaxCircleCount = 500;
                        h.process();

                        OmsVectorWriter.writeVector(outShp, h.outCircles);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        runner.waitAndClose();
    }
}
