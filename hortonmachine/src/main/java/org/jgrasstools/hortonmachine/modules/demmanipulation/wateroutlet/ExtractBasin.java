/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.NullProgressListener;
import org.jaitools.jts.PolygonSmoother;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorize.Vectorizer;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

@Description("Extract a basin from a map of flowdirections.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "http://www.hydrologis.com")
@Keywords("Dem manipulation, Basin, FlowDirections")
@Label(JGTConstants.DEMMANIPULATION)
@Name("extractbasin")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class ExtractBasin extends JGTModel {
    @Description("The northern coordinate of the watershed outlet.")
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description("The eastern coordinate of the watershed outlet.")
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description("The value for the map (defaults to 1).")
    @In
    public double pValue = 1.0;

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow;

    @Description("A user supplied network map. If available, the outlet point is snapped to it before extracting the basin.")
    @In
    public SimpleFeatureCollection inNetwork;

    @Description("A buffer to consider for network snapping.")
    @In
    public double pSnapbuffer = 200;

    @Description("Flag to enable vector basin smoothing.")
    @In
    public boolean doSmoothing = false;

    @Description("The area of the extracted basin.")
    @Out
    public double outArea = 0;

    @Description("The extracted basin mask.")
    @Out
    public GridCoverage2D outBasin = null;

    @Description("The optional outlet point vector map.")
    @Out
    public SimpleFeatureCollection outOutlet = null;

    @Description("The optional extracted basin vector map.")
    @Out
    public SimpleFeatureCollection outVectorBasin = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int ncols;

    private int nrows;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outBasin == null, doReset)) {
            return;
        }
        checkNull(inFlow);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        ncols = regionMap.getCols();
        nrows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();
        double north = regionMap.getNorth();
        double west = regionMap.getWest();
        double south = regionMap.getSouth();
        double east = regionMap.getEast();

        if (pNorth == -1 || pEast == -1) {
            throw new ModelsIllegalargumentException("No outlet coordinates were supplied.", this.getClass().getSimpleName());
        }
        if (pNorth > north || pNorth < south || pEast > east || pEast < west) {
            throw new ModelsIllegalargumentException("The outlet point lies outside the map region.", this.getClass()
                    .getSimpleName());
        }

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        WritableRaster basinWR = CoverageUtilities.createDoubleWritableRaster(ncols, nrows, null, null, doubleNovalue);
        WritableRandomIter basinIter = RandomIterFactory.createWritable(basinWR, null);

        Coordinate outlet = new Coordinate(pEast, pNorth);

        int[] outletColRow = CoverageUtilities.colRowFromCoordinate(outlet, inFlow.getGridGeometry(), null);

        double outletFlow = flowIter.getSampleDouble(outletColRow[0], outletColRow[1], 0);
        if (isNovalue(outletFlow)) {
            throw new IllegalArgumentException("The chosen outlet point doesn't have a valid value.");
        }

        FlowNode runningNode = new FlowNode(flowIter, ncols, nrows, outletColRow[0], outletColRow[1]);
        runningNode.mark(basinIter, pValue);
        outArea++;
        List<FlowNode> enteringNodes = runningNode.getEnteringNodes();

        pm.beginTask(msg.message("wateroutlet.extracting"), -1);
        while( enteringNodes.size() > 0 ) {
            if (pm.isCanceled()) {
                return;
            }
            List<FlowNode> newEnteringNodes = new ArrayList<FlowNode>();
            for( FlowNode flowNode : enteringNodes ) {
                flowNode.mark(basinIter, pValue);
                outArea++;

                List<FlowNode> newEntering = flowNode.getEnteringNodes();
                if (newEntering.size() > 0)
                    newEnteringNodes.addAll(newEntering);
            }
            enteringNodes = newEnteringNodes;
        }
        pm.done();

        outArea = outArea * xRes * yRes;
        outBasin = CoverageUtilities.buildCoverage("basin", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());

        snapOutlet();

        extractVectorBasin();

        smoothVectorBasin();
    }

    private void extractVectorBasin() throws Exception {
        Vectorizer vectorizer = new Vectorizer();
        vectorizer.pm = pm;
        vectorizer.inRaster = outBasin;
        vectorizer.pValue = 2.0;
        vectorizer.pThres = 1;
        vectorizer.fDefault = "rast";
        vectorizer.process();
        outVectorBasin = vectorizer.outVector;
    }

    private void smoothVectorBasin() throws IOException {
        final PolygonSmoother polygonSmoother = new PolygonSmoother();
        pm.beginTask("Smoothing polygons...", outVectorBasin.size());
        outVectorBasin.accepts(new FeatureVisitor(){
            @Override
            public void visit( Feature feature ) {
                SimpleFeature simpleFeature = (SimpleFeature) feature;
                Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
                if (geom != null) {
                    Polygon smoothedPolygon = polygonSmoother.smooth((Polygon) geom, 0.95);
                    simpleFeature.setDefaultGeometry(smoothedPolygon);
                }
                pm.worked(1);
            }
        }, new NullProgressListener());
        pm.done();
    }

    private void snapOutlet() throws IOException {
        if (inNetwork != null) {
            pm.beginTask("Snapping lines...", inNetwork.size());
            final SpatialIndex linesIndex = new STRtree();
            inNetwork.accepts(new FeatureVisitor(){
                @Override
                public void visit( Feature feature ) {
                    SimpleFeature simpleFeature = (SimpleFeature) feature;
                    pm.worked(1);
                    Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
                    if (geom != null) {
                        Envelope env = geom.getEnvelopeInternal();
                        if (!env.isNull()) {
                            env.expandBy(pSnapbuffer);
                            linesIndex.insert(env, new LocationIndexedLine(geom));
                        }
                    }
                }
            }, new NullProgressListener());
            pm.done();

            GeometryFactory gf = GeometryUtilities.gf();
            Coordinate userOutletCoordinate = new Coordinate(pEast, pNorth);
            Point userOutletPoint = gf.createPoint(userOutletCoordinate);

            @SuppressWarnings("unchecked")
            List<LocationIndexedLine> nearLines = linesIndex.query(userOutletPoint.getEnvelopeInternal());

            double minDist = Double.POSITIVE_INFINITY;
            Coordinate minDistCoordinate = null;
            for( LocationIndexedLine line : nearLines ) {
                LinearLocation here = line.project(userOutletCoordinate);
                Coordinate snappedCoordinate = line.extractPoint(here);
                double dist = snappedCoordinate.distance(userOutletCoordinate);
                if (dist < minDist) {
                    minDist = dist;
                    minDistCoordinate = snappedCoordinate;
                }
            }

            if (minDistCoordinate == null) {
                throw new RuntimeException("The outlet point could not be snapped to the network.");
            }

            Point snappedOutletPoint = gf.createPoint(minDistCoordinate);

            outOutlet = FeatureCollections.newCollection();

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("typename");
            b.setCRS(inNetwork.getSchema().getCoordinateReferenceSystem());
            b.add("the_geom", Point.class);
            b.add("basinarea", Double.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{snappedOutletPoint, outArea};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            outOutlet.add(feature);
        }
    }

}
