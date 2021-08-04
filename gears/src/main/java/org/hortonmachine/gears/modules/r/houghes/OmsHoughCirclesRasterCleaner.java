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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.util.ArrayList;
import java.util.List;

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
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ThreadedRunnable;
import org.hortonmachine.gears.modules.r.summary.OmsZonalStats;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;

@Description(OmsHoughCirclesRasterCleaner.DESCRIPTIO)
@Author(name = OmsHoughCirclesRasterCleaner.AUTHORS, contact = "")
@Keywords(OmsHoughCirclesRasterCleaner.KEYWORDS)
@Label(HMConstants.RASTERPROCESSING)
@Name(OmsHoughCirclesRasterCleaner.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsHoughCirclesRasterCleaner extends HMModel {

    @Description(inVector_DESCR)
    @In
    public SimpleFeatureCollection inVector;

    @Description(inRaster_DESCR)
    @In
    public GridCoverage2D inRaster;

    @Description(pMaxOverlapCount_DESCR)
    @In
    public int pMaxOverlapCount = 3;

    @Description(pMaxOverlap_DESCR)
    @Unit("%")
    @In
    public Double pMaxOverlap;

    @Description(outCircles_DESCR)
    @In
    public SimpleFeatureCollection outCircles;

    // VARS DESCR START
    public static final String NAME = "houghcirclesrastercleaner";
    public static final String KEYWORDS = "Hough, circle, cleaner";
    public static final String AUTHORS = "Andrea Antonello (www.hydrologis.com)";
    public static final String DESCRIPTIO = "Module to remove circles after the HoughCirclesRaster module.";

    public static final String inVector_DESCR = "The input circles vector.";
    public static final String inRaster_DESCR = "The raster from which the circles where generated.";
    public static final String pMaxOverlap_DESCR = "The maximum permitted overlap.";
    public static final String pMaxOverlapCount_DESCR = "The maximum permitted overlap count.";
    public static final String outCircles_DESCR = "The leftover circles.";
    // VARS DESCR END

    @SuppressWarnings("unchecked")
    @Execute
    public void process() throws Exception {
        checkNull(inVector, pMaxOverlap, inRaster);

        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
        double novalue = HMConstants.getNovalue(inRaster);
        GridGeometry2D gridGeometry = inRaster.getGridGeometry();
        double[] tm_utm_tac = new double[3];

        STRtree circlesTree = FeatureUtilities.featureCollectionToSTRtree(inVector);
        List<SimpleFeature> circlesList = FeatureUtilities.featureCollectionToList(inVector);

        DefaultFeatureCollection outFC = new DefaultFeatureCollection();

        for( SimpleFeature circleFeature : circlesList ) {
            Geometry geometry = (Geometry) circleFeature.getDefaultGeometry();
            Polygon circle = (Polygon) geometry.getGeometryN(0);
            PreparedGeometry preparedCircle = PreparedGeometryFactory.prepare(circle);

            List<SimpleFeature> circlesAround = circlesTree.query(circle.getEnvelopeInternal());
            List<Geometry> intersectedCircles = new ArrayList<Geometry>();
            for( SimpleFeature circleAround : circlesAround ) {
                if (circleAround.equals(circleFeature)) {
                    continue;
                }
                Geometry circleAroundGeometry = (Geometry) circleAround.getDefaultGeometry();
                if (preparedCircle.intersects(circleAroundGeometry)) {
                    intersectedCircles.add(circleAroundGeometry);
                }
            }

            Point centroid = circle.getCentroid();
            int intersectionsCount = intersectedCircles.size();
            if (intersectionsCount != 0) {
                // check how many circles overlapped
                if (intersectionsCount > pMaxOverlapCount) {
                    continue;
                }
                // check if the circles overlap too much, i.e. cover their baricenter
                boolean intersected = false;
                for( Geometry intersectedCircle : intersectedCircles ) {
                    if (intersectedCircle.intersects(centroid)) {
                        intersected = true;
                        break;
                    }
                }
                if (intersected) {
                    continue;
                }
            }
            // check if the center has a raster value, i.e. is not empty
            double value = CoverageUtilities.getValue(inRaster, centroid.getCoordinate());
            if (!HMConstants.isNovalue(value)) {
                continue;
            }

            // check if the inner part of the circle is indeed rather empty

            // min, max, mean, var, sdev, activeCellCount, passiveCellCount
            double[] stats = OmsZonalStats.polygonStats(circle, gridGeometry, rasterIter, novalue, false, tm_utm_tac, 0, pm);
            // if we have many more active cells than passive cells, that is not a circle
            double activeCells = stats[5];
            double novalues = stats[6];
            if (activeCells * 1.5 > novalues) {
                continue;
            }

            // take it as valid circle
            outFC.add(circleFeature);
        }
        outCircles = outFC;

        rasterIter.done();
    }

    @SuppressWarnings("rawtypes")
    public static void main( String[] args ) throws Exception {

        ThreadedRunnable< ? > runner = new ThreadedRunnable(8, null);

        int[] i = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
        for( int index : i ) {
            final int _index = index;
            runner.executeRunnable(new Runnable(){
                public void run() {
                    try {
                        String inRaster = "/home/hydrologis/data/rilievo_tls/avgres/las/vertical_slices/slice_" + _index
                                + ".0.asc";
                        String inShp = "/home/hydrologis/data/rilievo_tls/avgres/las/vertical_slices/slice_vector_" + _index
                                + ".0.shp";
                        String outShp = "/home/hydrologis/data/rilievo_tls/avgres/las/vertical_slices/slice_vector_cleaned_"
                                + _index + ".0.shp";

                        GridCoverage2D src = OmsRasterReader.readRaster(inRaster);

                        OmsHoughCirclesRaster h = new OmsHoughCirclesRaster();
                        h.inRaster = src;
                        h.pMinRadius = 0.1;
                        h.pMaxRadius = 0.5;
                        h.pRadiusIncrement = 0.01;
                        h.pMaxCircleCount = 500;
                        h.process();

                        OmsVectorWriter.writeVector(inShp, h.outCircles);

                        SimpleFeatureCollection inVector = OmsVectorReader.readVector(inShp);
                        OmsHoughCirclesRasterCleaner hCleaner = new OmsHoughCirclesRasterCleaner();
                        hCleaner.inRaster = src;
                        hCleaner.inVector = inVector;
                        hCleaner.pMaxOverlap = 0.1;
                        hCleaner.pMaxOverlapCount = 5;
                        hCleaner.process();

                        OmsVectorWriter.writeVector(outShp, hCleaner.outCircles);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        runner.waitAndClose();
    }
}
