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
package org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet;

import static org.hortonmachine.gears.libs.modules.HMConstants.DEMMANIPULATION;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.shortNovalue;

import java.awt.image.WritableRaster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

@Description(OmsExtractBasin.OMSEXTRACTBASIN_DESCRIPTION)
@Author(name = OmsExtractBasin.OMSEXTRACTBASIN_AUTHORNAMES, contact = OmsExtractBasin.OMSEXTRACTBASIN_AUTHORCONTACTS)
@Keywords(OmsExtractBasin.OMSEXTRACTBASIN_KEYWORDS)
@Label(OmsExtractBasin.OMSEXTRACTBASIN_LABEL)
@Name(OmsExtractBasin.OMSEXTRACTBASIN_NAME)
@Status(OmsExtractBasin.OMSEXTRACTBASIN_STATUS)
@License(OmsExtractBasin.OMSEXTRACTBASIN_LICENSE)
public class OmsExtractBasin extends HMModel {

    @Description(OMSEXTRACTBASIN_pNorth_DESCRIPTION)
    @UI(HMConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description(OMSEXTRACTBASIN_pEast_DESCRIPTION)
    @UI(HMConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description(OMSEXTRACTBASIN_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow;

    @Description(OMSEXTRACTBASIN_inNetwork_DESCRIPTION)
    @In
    public SimpleFeatureCollection inNetwork;

    @Description(OMSEXTRACTBASIN_pSnapbuffer_DESCRIPTION)
    @In
    public double pSnapbuffer = 200;

    @Description(OMSEXTRACTBASIN_doVector_DESCRIPTION)
    @In
    public boolean doVector = true;

    @Description(OMSEXTRACTBASIN_doSmoothing_DESCRIPTION)
    @In
    public boolean doSmoothing = false;

    @Description(OMSEXTRACTBASIN_outArea_DESCRIPTION)
    @Out
    public double outArea = 0;

    @Description(OMSEXTRACTBASIN_outBasin_DESCRIPTION)
    @Out
    public GridCoverage2D outBasin = null;

    @Description(OMSEXTRACTBASIN_outOutlet_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outOutlet = null;

    @Description(OMSEXTRACTBASIN_outVectorBasin_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVectorBasin = null;

    public static final String OMSEXTRACTBASIN_DESCRIPTION = "Extract a basin from a map of flowdirections.";
    public static final String OMSEXTRACTBASIN_DOCUMENTATION = "";
    public static final String OMSEXTRACTBASIN_KEYWORDS = "Dem manipulation, Basin, OmsFlowDirections";
    public static final String OMSEXTRACTBASIN_LABEL = DEMMANIPULATION;
    public static final String OMSEXTRACTBASIN_NAME = "extractbasin";
    public static final int OMSEXTRACTBASIN_STATUS = 5;
    public static final String OMSEXTRACTBASIN_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSEXTRACTBASIN_AUTHORNAMES = "Andrea Antonello, Silvia Franceschi";
    public static final String OMSEXTRACTBASIN_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSEXTRACTBASIN_pNorth_DESCRIPTION = "The northern coordinate of the watershed outlet.";
    public static final String OMSEXTRACTBASIN_pEast_DESCRIPTION = "The eastern coordinate of the watershed outlet.";
    public static final String OMSEXTRACTBASIN_inFlow_DESCRIPTION = "The map of flowdirections.";
    public static final String OMSEXTRACTBASIN_inNetwork_DESCRIPTION = "A user supplied network map. If available, the outlet point is snapped to it before extracting the basin.";
    public static final String OMSEXTRACTBASIN_pSnapbuffer_DESCRIPTION = "A buffer to consider for network snapping.";
    public static final String OMSEXTRACTBASIN_doVector_DESCRIPTION = "Flag to enable vector basin extraction.";
    public static final String OMSEXTRACTBASIN_doSmoothing_DESCRIPTION = "Flag to enable vector basin smoothing.";
    public static final String OMSEXTRACTBASIN_outArea_DESCRIPTION = "The area of the extracted basin.";
    public static final String OMSEXTRACTBASIN_outBasin_DESCRIPTION = "The extracted basin mask.";
    public static final String OMSEXTRACTBASIN_outOutlet_DESCRIPTION = "The optional outlet point vector map.";
    public static final String OMSEXTRACTBASIN_outVectorBasin_DESCRIPTION = "The optional extracted basin vector map.";

    public static final String FIELD_BASINAREA = "basinarea";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int ncols;
    private int nrows;

    private CoordinateReferenceSystem crs;
    private GeometryFactory gf = GeometryUtilities.gf();
    private boolean alreadyWarned = false;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outBasin == null, doReset)) {
            return;
        }
        checkNull(inFlow);

        crs = inFlow.getCoordinateReferenceSystem();

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
            throw new ModelsIllegalargumentException("No outlet coordinates were supplied.", this.getClass().getSimpleName(), pm);
        }
        if (pNorth > north || pNorth < south || pEast > east || pEast < west) {
            throw new ModelsIllegalargumentException("The outlet point lies outside the map region.",
                    this.getClass().getSimpleName(), pm);
        }

        Coordinate snapOutlet = snapOutlet();
        if (snapOutlet != null) {
            pEast = snapOutlet.x;
            pNorth = snapOutlet.y;
        }
        int novalue = HMConstants.getIntNovalue(inFlow);
        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        WritableRaster basinWR = CoverageUtilities.createWritableRaster(ncols, nrows, Short.class, null, shortNovalue);
        WritableRandomIter basinIter = RandomIterFactory.createWritable(basinWR, null);

        try {
            Coordinate outlet = new Coordinate(pEast, pNorth);

            int[] outletColRow = CoverageUtilities.colRowFromCoordinate(outlet, inFlow.getGridGeometry(), null);

            int outletFlow = flowIter.getSample(outletColRow[0], outletColRow[1], 0);
            if (isNovalue(outletFlow)) {
                throw new IllegalArgumentException("The chosen outlet point doesn't have a valid value.");
            }

            FlowNode runningNode = new FlowNode(flowIter, ncols, nrows, outletColRow[0], outletColRow[1], novalue);
            runningNode.setIntValueInMap(basinIter, 1);
            outArea++;

            ConcurrentLinkedQueue<FlowNode> enteringNodes = new ConcurrentLinkedQueue<>(runningNode.getEnteringNodes());
            pm.beginTask(msg.message("wateroutlet.extracting"), -1);
            while( enteringNodes.size() > 0 ) {
                if (pm.isCanceled()) {
                    return;
                }

                ConcurrentLinkedQueue<FlowNode> newEnteringNodes = new ConcurrentLinkedQueue<>();
                enteringNodes.parallelStream().forEach(flowNode -> {
                    if (pm.isCanceled()) {
                        return;
                    }
                    if (!alreadyWarned && flowNode.touchesBound()) {
                        pm.errorMessage(MessageFormat.format(
                                "WARNING: touched boundaries in col/row = {0}/{1}. You might consider to review your processing region.",
                                flowNode.col, flowNode.row));
                        alreadyWarned = true;
                    }
                    flowNode.setIntValueInMap(basinIter, 1);
                    outArea++;

                    List<FlowNode> newEntering = flowNode.getEnteringNodes();
                    newEnteringNodes.addAll(newEntering);
                });
                enteringNodes = newEnteringNodes;
            }
            pm.done();

            outArea = outArea * xRes * yRes;
            outBasin = CoverageUtilities.buildCoverageWithNovalue("basin", basinWR, regionMap, crs, shortNovalue);

            extractVectorBasin();
        } finally {
            flowIter.done();
            basinIter.done();
        }
    }

    private void extractVectorBasin() throws Exception {
        if (!doVector) {
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("outsideValues", Arrays.asList(HMConstants.doubleNovalue));
        Collection<Polygon> polygons = FeatureUtilities.doVectorize(outBasin, params);

        Polygon rightPolygon = null;
        double maxArea = Double.NEGATIVE_INFINITY;
        for( Polygon polygon : polygons ) {
            double area = polygon.getArea();
            if (area > maxArea) {
                rightPolygon = polygon;
                maxArea = area;
            }
        }

        rightPolygon = smoothVectorBasin(rightPolygon);

        outVectorBasin = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("basins");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("area", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        Object[] values = new Object[]{rightPolygon, rightPolygon.getArea()};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        ((DefaultFeatureCollection) outVectorBasin).add(feature);
    }

    private Polygon smoothVectorBasin( Polygon polygon ) throws Exception {
        if (!doSmoothing) {
            return polygon;
        }

        // final PolygonSmoother polygonSmoother = new PolygonSmoother();
        pm.beginTask("Smoothing polygons...", IHMProgressMonitor.UNKNOWN);

        try {
            LineString lineString = gf.createLineString(polygon.getCoordinates());

            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            newCollection.add(FeatureUtilities.toDummyFeature(lineString, null));

            OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
            smoother.inVector = newCollection;
            smoother.pLookahead = 5;
            smoother.pSlide = 0.9;
            // smoother.pDensify = 0.9;
            smoother.process();
            SimpleFeatureCollection outFeatures = smoother.outVector;

            MultiLineString newGeom = (MultiLineString) outFeatures.features().next().getDefaultGeometry();
            polygon = gf.createPolygon(gf.createLinearRing(newGeom.getCoordinates()), null);
        } catch (Exception e) {
            pm.errorMessage("Warning, unable to smooth the basin. Continue with original layer.");
        }

        pm.done();

        return polygon;
    }

    private Coordinate snapOutlet() throws IOException {
        if (inNetwork != null) {
            pm.beginTask("Snapping to network...", inNetwork.size());
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
            makeOutletFC(snappedOutletPoint);
            return minDistCoordinate;
        } else {
            // use real outlet
            Point snappedOutletPoint = gf.createPoint(new Coordinate(pEast, pNorth));
            makeOutletFC(snappedOutletPoint);
        }
        return null;
    }

    private void makeOutletFC( Point snappedOutletPoint ) {
        outOutlet = new DefaultFeatureCollection();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("outlet");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add(FIELD_BASINAREA, Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{snappedOutletPoint, -9999.0};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        ((DefaultFeatureCollection) outOutlet).add(feature);
    }

}
