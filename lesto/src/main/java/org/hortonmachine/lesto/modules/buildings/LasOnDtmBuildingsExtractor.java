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
package org.hortonmachine.lesto.modules.buildings;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.modules.r.morpher.OmsMorpher;
import org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster;
import org.hortonmachine.gears.modules.v.vectorize.OmsVectorizer;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

@Description("A simple buildings extractor module")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("las, buildings")
@Label(HMConstants.LESTO + "/buildings")
@Name("lasondembuildingsextractor")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasOnDtmBuildingsExtractor extends HMModel {
    @Description("The las file")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas;

    @Description("The dtm raster")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

    @Description("The dtm threshold to use.")
    @Unit("m")
    @In
    public double pDtmThres = 1.8;

    @Description("The minimum building area allowed.")
    @Unit("m2")
    @In
    public double pMinArea = 25.0;

    @Description("The raster resolution to force. If null, the dtm resolution will be used.")
    @Unit("m")
    @In
    public Double pRasterResolution;

    @Description("If true, some smoothing and cleanups will be performed.")
    @In
    public boolean doSmoothing;

    @Description("The densification resolution to use for smoothing.")
    @Unit("m")
    @In
    public double pDensifyResolution = 0.5;

    @Description("The negative buildings buffer used to check internal points without border problems.")
    @Unit("m")
    @In
    public double pBuildingsBuffer = -0.5;

    @Description("The output vector buildings.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBuildings;

    @Description("The optional cleaned-up output vector buildings.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outCleanBuildings;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, inDtm, outBuildings);

        SimpleFeatureCollection buildingsFC = null;
        GridCoverage2D inDtmGC = null;
        try (ALasDataManager lasHandler = ALasDataManager.getDataManager(new File(inLas), inDtmGC, 0, null)) {
            lasHandler.open();

            ReferencedEnvelope3D e = lasHandler.getEnvelope3D();

            if (pRasterResolution != null) {
                OmsRasterReader reader = new OmsRasterReader();
                reader.file = inDtm;
                reader.pNorth = e.getMaxY();
                reader.pSouth = e.getMinY();
                reader.pWest = e.getMinX();
                reader.pEast = e.getMaxX();
                reader.pXres = pRasterResolution;
                reader.pYres = pRasterResolution;
                reader.process();
                inDtmGC = reader.outRaster;
            } else {
                inDtmGC = getRaster(inDtm);
                RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtmGC);
                pRasterResolution = regionMap.getXres();
            }

            Envelope2D envelope2d = inDtmGC.getEnvelope2D();
            Polygon regionPolygon = FeatureUtilities.envelopeToPolygon(envelope2d);

            WritableRaster[] buildingsHolder = new WritableRaster[1];
            GridCoverage2D newBuildingsRaster = CoverageUtilities.createCoverageFromTemplate(inDtmGC, 1.0, buildingsHolder);
            GridGeometry2D gridGeometry = newBuildingsRaster.getGridGeometry();

            java.awt.Point p = new java.awt.Point();
            IHMProgressMonitor pm = new LogProgressMonitor();
            pm.beginTask("Reading points...", IHMProgressMonitor.UNKNOWN);
            List<LasRecord> pointsInGeometry = lasHandler.getPointsInGeometry(regionPolygon, true);
            pm.done();
            pm.beginTask("Buildings filtering...", (int) pointsInGeometry.size());
            for( LasRecord dot : pointsInGeometry ) {
                Coordinate c = new Coordinate(dot.x, dot.y);
                if (!envelope2d.contains(dot.x, dot.y)) {
                    continue;
                }

                double dtmValue = CoverageUtilities.getValue(inDtmGC, c);
                double height = abs(dot.z - dtmValue);
                CoverageUtilities.colRowFromCoordinate(c, gridGeometry, p);
                if (height < pDtmThres) {
                    buildingsHolder[0].setSample(p.x, p.y, 0, doubleNovalue);
                }
                pm.worked(1);
            }
            pm.done();

            OmsMorpher morpher = new OmsMorpher();
            morpher.pm = pm;
            morpher.inMap = newBuildingsRaster;
            morpher.pMode = Variables.OPEN;
            morpher.process();
            newBuildingsRaster = morpher.outMap;

            OmsVectorizer buildingsVectorizer = new OmsVectorizer();
            buildingsVectorizer.pm = pm;
            buildingsVectorizer.inRaster = newBuildingsRaster;
            buildingsVectorizer.doRemoveHoles = true;
            double minAreaInCells = pMinArea / pRasterResolution / pRasterResolution;
            buildingsVectorizer.pThres = minAreaInCells;
            buildingsVectorizer.fDefault = "rast";
            buildingsVectorizer.process();

            buildingsFC = buildingsVectorizer.outVector;
            dumpVector(buildingsFC, outBuildings);
            if (doSmoothing && outCleanBuildings != null) {
                // smooth
                SimpleFeatureCollection smoothedFC = smoothBuildings(pDensifyResolution, buildingsFC);

                // remove trees
                DefaultFeatureCollection removedAndSmoothedFC = removeNonBuildings(lasHandler, smoothedFC, inDtmGC,
                        pBuildingsBuffer);

                dumpVector(removedAndSmoothedFC, outCleanBuildings);
            }
        }

    }

    private DefaultFeatureCollection removeNonBuildings( ALasDataManager lasHandler, SimpleFeatureCollection buildingsFC,
            GridCoverage2D dem, double buildingsBuffer ) throws Exception {
        final List<SimpleFeature> buildingsList = FeatureUtilities.featureCollectionToList(buildingsFC);

        final List<SimpleFeature> checkedBuildings = new ArrayList<SimpleFeature>();
        pm.beginTask("Removing buildings...", buildingsList.size());
        for( int i = 0; i < buildingsList.size(); i++ ) {
            SimpleFeature building = buildingsList.get(i);
            Geometry buildingGeom = (Geometry) building.getDefaultGeometry();

            Geometry bufferedGeom = buildingGeom.buffer(buildingsBuffer);
            List<LasRecord> points = lasHandler.getPointsInGeometry(bufferedGeom, false);
            int percOfOnes = checkReturnNum(points, bufferedGeom);
            if (percOfOnes >= 96) {
                checkedBuildings.add(building);
            }
            pm.worked(1);
        }
        pm.done();

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.addAll(checkedBuildings);
        return fc;
    }

    private int checkReturnNum( List<LasRecord> points, Geometry bufferedGeom ) {

        int count = 0;
        for( LasRecord l : points ) {
            // System.out.print(l.returnNumber);
            if (l.returnNumber == 1) {
                count++;
            }
        }

        double perc = count * 100.0 / points.size();
        return (int) round(perc);
    }

    private SimpleFeatureCollection smoothBuildings( double densifyResolution, SimpleFeatureCollection buildingsFC )
            throws Exception {

        final List<SimpleFeature> buildingsList = FeatureUtilities.featureCollectionToList(buildingsFC);

        List<Polygon> newPolygons = new ArrayList<Polygon>();
        pm.beginTask("Smoothing buildings...", buildingsList.size());
        for( final SimpleFeature building : buildingsList ) {
            Geometry buildingGeom = (Geometry) building.getDefaultGeometry();

            buildingGeom = Densifier.densify(buildingGeom, densifyResolution);
            Coordinate[] coordinates = buildingGeom.getCoordinates();
            List<Coordinate> newCoords = new ArrayList<Coordinate>();
            for( int i = 0; i < coordinates.length - 1; i++ ) {
                Coordinate from = coordinates[i];
                Coordinate to = coordinates[i + 1];

                LineSegment l = new LineSegment(from, to);
                Coordinate newCoord = l.pointAlong(0.5);
                newCoords.add(newCoord);
            }

            newCoords.add(newCoords.get(0));

            Polygon newPolygon = gf.createPolygon(newCoords.toArray(new Coordinate[0]));

            // newPolygon = (Polygon) DouglasPeuckerSimplifier.simplify(newPolygon, 0.1);
            newPolygons.add(newPolygon);
            pm.worked(1);
        }
        pm.done();

        SimpleFeatureCollection fc = FeatureUtilities.featureCollectionFromGeometry(buildingsFC.getBounds()
                .getCoordinateReferenceSystem(), newPolygons.toArray(GeometryUtilities.TYPE_POLYGON));

        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        smoother.pm = pm;
        smoother.pLimit = 10;
        smoother.inVector = fc;
        smoother.pLookahead = 5;
        // smoother.pSlide = 1;
        // smoother.pDensify = 0.2;
        // smoother.pSimplify = 0.1;
        smoother.process();

        fc = smoother.outVector;
        List<Geometry> geomsList = FeatureUtilities.featureCollectionToGeometriesList(fc, true, null);
        List<Geometry> newGeomsList = new ArrayList<Geometry>();
        for( Geometry geometry : geomsList ) {
            Polygon polygon = gf.createPolygon(geometry.getCoordinates());
            polygon = (Polygon) TopologyPreservingSimplifier.simplify(polygon, 0.4);
            newGeomsList.add(polygon);
        }
        fc = FeatureUtilities.featureCollectionFromGeometry(buildingsFC.getBounds().getCoordinateReferenceSystem(),
                newGeomsList.toArray(GeometryUtilities.TYPE_POLYGON));
        return fc;
    }
}
