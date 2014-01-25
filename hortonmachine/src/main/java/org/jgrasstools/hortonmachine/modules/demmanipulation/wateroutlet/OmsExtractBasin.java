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
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_doSmoothing_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_doVector_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_inNetwork_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outArea_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outBasin_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outOutlet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outVectorBasin_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pEast_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pNorth_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pSnapbuffer_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pValue_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.NullProgressListener;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.modules.v.smoothing.OmsLineSmootherMcMaster;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

@Description(OMSEXTRACTBASIN_DESCRIPTION)
@Author(name = OMSEXTRACTBASIN_AUTHORNAMES, contact = OMSEXTRACTBASIN_AUTHORCONTACTS)
@Keywords(OMSEXTRACTBASIN_KEYWORDS)
@Label(OMSEXTRACTBASIN_LABEL)
@Name(OMSEXTRACTBASIN_NAME)
@Status(OMSEXTRACTBASIN_STATUS)
@License(OMSEXTRACTBASIN_LICENSE)
public class OmsExtractBasin extends JGTModel {
    @Description(OMSEXTRACTBASIN_pNorth_DESCRIPTION)
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description(OMSEXTRACTBASIN_pEast_DESCRIPTION)
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description(OMSEXTRACTBASIN_pValue_DESCRIPTION)
    @In
    public double pValue = 1.0;

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

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int ncols;

    private int nrows;

    private CoordinateReferenceSystem crs;

    private GeometryFactory gf = GeometryUtilities.gf();

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
            throw new ModelsIllegalargumentException("No outlet coordinates were supplied.", this.getClass().getSimpleName());
        }
        if (pNorth > north || pNorth < south || pEast > east || pEast < west) {
            throw new ModelsIllegalargumentException("The outlet point lies outside the map region.", this.getClass()
                    .getSimpleName());
        }

        Coordinate snapOutlet = snapOutlet();
        if (snapOutlet != null) {
            pEast = snapOutlet.x;
            pNorth = snapOutlet.y;
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
        runningNode.setValueInMap(basinIter, pValue);
        outArea++;
        List<FlowNode> enteringNodes = runningNode.getEnteringNodes();

        boolean alreadyWarned = false;
        pm.beginTask(msg.message("wateroutlet.extracting"), -1);
        while( enteringNodes.size() > 0 ) {
            if (pm.isCanceled()) {
                return;
            }
            List<FlowNode> newEnteringNodes = new ArrayList<FlowNode>();
            for( FlowNode flowNode : enteringNodes ) {
                if (!alreadyWarned && flowNode.touchesBound()) {
                    pm.errorMessage(MessageFormat
                            .format("WARNING: touched boundaries in col/row = {0}/{1}. You might consider to review your processing region.",
                                    flowNode.col, flowNode.row));
                    alreadyWarned = true;
                }
                flowNode.setValueInMap(basinIter, pValue);
                outArea++;

                List<FlowNode> newEntering = flowNode.getEnteringNodes();
                if (newEntering.size() > 0)
                    newEnteringNodes.addAll(newEntering);
            }
            enteringNodes = newEnteringNodes;
        }
        pm.done();

        outArea = outArea * xRes * yRes;
        outBasin = CoverageUtilities.buildCoverage("basin", basinWR, regionMap, crs);

        extractVectorBasin();
    }

    private void extractVectorBasin() throws Exception {
        if (!doVector) {
            return;
        }

        Collection<Polygon> polygons = FeatureUtilities.doVectorize(outBasin, null);

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
        pm.beginTask("Smoothing polygons...", IJGTProgressMonitor.UNKNOWN);

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
        b.setName("typename");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("basinarea", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{snappedOutletPoint, -9999.0};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        ((DefaultFeatureCollection) outOutlet).add(feature);
    }

    public static void main( String[] args ) throws Exception {
        String grassdb = "/home/moovida/Dropbox/hydrologis/lavori/2012_03_27_finland_forestry/data/grassdata/finland/testset/cell/";
        String shapeBase = "/home/moovida/Dropbox/hydrologis/lavori/2012_03_27_finland_forestry/data/GISdata/04_164/";

        String inFlowPath = grassdb + "carved_flow";
        String inNetworkPath = shapeBase + "vv_04_164.shp";
        String outBasinPath = grassdb + "carved_eb";
        String outOutletPath = shapeBase + "eb_outlet.shp";
        String outVectorBasinPath = shapeBase + "eb_basin_smoothed.shp";

        OmsExtractBasin eb = new OmsExtractBasin();
        eb.pNorth = 6862353.979338094;
        eb.pEast = 3520253.4090277995;
        eb.pValue = 1.0;
        eb.inFlow = OmsRasterReader.readRaster(inFlowPath);
        eb.inNetwork = OmsVectorReader.readVector(inNetworkPath);
        eb.pSnapbuffer = 200.0;
        eb.doVector = true;
        eb.doSmoothing = true;
        eb.process();

        OmsRasterWriter.writeRaster(outBasinPath, eb.outBasin);
        OmsVectorWriter.writeVector(outVectorBasinPath, eb.outVectorBasin);
        OmsVectorWriter.writeVector(outOutletPath, eb.outOutlet);
    }

}
