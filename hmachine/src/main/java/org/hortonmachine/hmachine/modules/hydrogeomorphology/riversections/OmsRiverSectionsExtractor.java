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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections;
import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.LWFields;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OmsRiverSectionsExtractor.DESCRIPTION)
@Documentation(OmsRiverSectionsExtractor.DOCUMENTATION)
@Author(name = OmsRiverSectionsExtractor.AUTHORNAMES, contact = OmsRiverSectionsExtractor.AUTHORCONTACTS)
@Keywords(OmsRiverSectionsExtractor.KEYWORDS)
@Label(OmsRiverSectionsExtractor.LABEL)
@Name(OmsRiverSectionsExtractor.NAME)
@Status(OmsRiverSectionsExtractor.STATUS)
@License(OmsRiverSectionsExtractor.LICENSE)
public class OmsRiverSectionsExtractor extends HMModel {

    @Description(inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(inRiver_DESCRIPTION)
    @In
    public SimpleFeatureCollection inRiver = null;

    @Description(inRiverPoints_DESCRIPTION)
    @In
    public SimpleFeatureCollection inRiverPoints = null;

    @Description(inBridges_DESCRIPTION)
    @In
    public SimpleFeatureCollection inBridges = null;

    @Description(inSections_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSections = null;

    @Description(pSectionsIntervalDistance_DESCRIPTION)
    @In
    public double pSectionsIntervalDistance = 10.0;

    @Description(pSectionsWidth_DESCRIPTION)
    @In
    public double pSectionsWidth = 10.0;

    @Description(pBridgeBuffer_DESCRIPTION)
    @In
    public double pBridgeBuffer = 0.0D;

    @Description(fBridgeWidth_DESCRIPTION)
    @In
    public String fBridgeWidth;

    @Description(outSections_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outSections = null;

    @Description(outSectionPoints_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outSectionPoints = null;

    @Description(outRiverPoints_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outRiverPoints = null;

    public static final String DESCRIPTION = "Extracts the cross sections of a river from an elevation model, along the line of the river: the input of the 1D hydraulic models, as SaintGeo and HEC-RAS.";
    public static final String DOCUMENTATION = """
            The line of the river has to be drawn from upstream to downstream, along the bottom of its channel. If it is made of several lines, they are merged: when they do not form a single line, the longest one is used.

            The sections are perpendicular to the line, at the given interval along it, and reach the given distance from it on each side. Their points are taken from the elevation model, one for each cell crossed. As they are perpendicular to the local direction of the line, a line that follows the cells, as the networks extracted from an elevation model, gives sections turned by 45 degrees back and forth: smooth it first. The sections must not cross each other: in the bends, they can reach at most the radius of the bend.

            Instead of the interval, the sections can be placed at given river points, with their ids and Gauckler-Strickler coefficients (in the fields of the large wood modules). Bridges, given as points with their width in a field, add a section on the bridge and one just upstream and downstream of it. Sections created before by this module can also be given, to take their points from a new elevation model.

            The three outputs describe the river for the hydraulic models: the section lines, with their id, progressive distance along the river and Gauckler-Strickler coefficient (30 if not given); the points of the sections, with their elevation; the points of the river between the sections, with their elevation.
            """;
    public static final String KEYWORDS = "Sections, Raster, Vector, Hydraulic";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "RiverSectionsExtractor";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "Andrea Antonello, Silvia Franceschi";
    public static final String AUTHORCONTACTS = "www.hydrologis.com";

    public static final String inElev_DESCRIPTION = "The elevation model.";
    public static final String inRiver_DESCRIPTION = "The line of the river, drawn from upstream along the bottom of its channel.";
    public static final String inRiverPoints_DESCRIPTION = "Optional points of the river where to place the sections, instead of at the interval.";
    public static final String inBridges_DESCRIPTION = "Optional points of the bridges, with a section on each and just upstream and downstream of it.";
    public static final String inSections_DESCRIPTION = "Optional sections created before by this module, to use instead of extracting them at the interval.";
    public static final String pSectionsIntervalDistance_DESCRIPTION = "The distance between the sections, along the river.";
    public static final String pSectionsWidth_DESCRIPTION = "The distance of the section ends from the river line, on each side: the section is twice as wide.";
    public static final String pBridgeBuffer_DESCRIPTION = "The distance within which a bridge point is considered on the river.";
    public static final String fBridgeWidth_DESCRIPTION = "The field of the width of the bridges.";
    public static final String outSections_DESCRIPTION = "The section lines.";
    public static final String outSectionPoints_DESCRIPTION = "The points of the sections, with their elevation.";
    public static final String outRiverPoints_DESCRIPTION = "The points of the river between the sections, with their elevation.";

    @Execute
    public void process() throws Exception {
        checkNull(inElev, inRiver);

        gf = GeometryUtilities.gf();

        List<SimpleFeature> riverFeatures = FeatureUtilities.featureCollectionToList(inRiver);
        
        Geometry geometry;
        if (riverFeatures.size() == 1) {            
            geometry = (Geometry) riverFeatures.get(0).getDefaultGeometry();
        } else {
            List<LineString> lines = riverFeatures.stream().map(f -> (LineString) ((Geometry) f.getDefaultGeometry()).getGeometryN(0)).collect(Collectors.toList());
            List<LineString> merged = GeometryUtilities.mergeLinestrings(lines);
            // the river is the longest of the lines that could not be merged together
            geometry = merged.stream().max(Comparator.comparingDouble(LineString::getLength)).get();
            if (merged.size() > 1) {
                pm.errorMessage("The river lines form " + merged.size() + " separate lines: only the longest one, of "
                        + Math.round(geometry.getLength()) + " m, is used.");
            }
        }
        
        Coordinate[] riverCoordinates = geometry.getCoordinates();

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        Envelope envelope = regionMap.toEnvelope();
        pm.beginTask("Building 3D reach geometry...", riverCoordinates.length);
        Point2D.Double point = new Point2D.Double();
        double[] extracted = new double[1];
        for( int i = 0; i < riverCoordinates.length; i++ ) {
            Coordinate coordinate = riverCoordinates[i];
            if (!envelope.intersects(coordinate.x, coordinate.y)) {
                pm.worked(1);
                continue;
            }
            point.setLocation(coordinate.x, coordinate.y);
            inElev.evaluate(point, extracted);

            riverCoordinates[i] = new Coordinate(coordinate.x, coordinate.y, extracted[0]);
            pm.worked(1);
        }
        pm.done();

        LineString riverGeometry3d = gf.createLineString(riverCoordinates);

        ARiverSectionsExtractor sectionsExtractor;
        if (inSections == null) {
            List<FeatureMate> bridgePoints = new ArrayList<>();
            if (inBridges != null) {
                bridgePoints = FeatureUtilities.featureCollectionToMatesList(inBridges);
            }

            if (inRiverPoints != null) {
                List<SimpleFeature> riverPointsList = FeatureUtilities.featureCollectionToList(inRiverPoints);
                Coordinate[] riverPointCoordinates = new Coordinate[riverPointsList.size()];
                int[] riverPointIds = new int[riverPointsList.size()];
                double[] riverPointKs = new double[riverPointsList.size()];

                for( int i = 0; i < riverPointIds.length; i++ ) {
                    SimpleFeature riverPointFeature = riverPointsList.get(i);
                    Coordinate riverPointCoordinate = ((Geometry) riverPointFeature.getDefaultGeometry()).getCoordinate();
                    int id = ((Number) riverPointFeature.getAttribute(LWFields.LINKID)).intValue();
                    riverPointCoordinates[i] = riverPointCoordinate;
                    riverPointIds[i] = id;
                    Object attribute = riverPointFeature.getAttribute(LWFields.GAUKLER);
                    if (attribute != null) {
                        double ks = ((Number) attribute).doubleValue();
                        riverPointKs[i] = ks;
                    } else {
                        riverPointKs[i] = 30.0;
                    }
                }
                sectionsExtractor = new RiverSectionsFromDtmExtractor(riverGeometry3d, //
                        riverPointCoordinates, riverPointIds, riverPointKs, //
                        inElev, pSectionsIntervalDistance, pSectionsWidth, bridgePoints, fBridgeWidth, pBridgeBuffer, pm);
            } else {
                sectionsExtractor = new RiverSectionsFromDtmExtractor(riverGeometry3d, //
                        inElev, pSectionsIntervalDistance, pSectionsWidth, bridgePoints, fBridgeWidth, pBridgeBuffer, pm);
            }

        } else {
            List<SimpleFeature> sectionsList = FeatureUtilities.featureCollectionToList(inSections);
            sectionsExtractor = new RiverSectionsFromFeaturesExtractor(riverGeometry3d, inElev, sectionsList, pm);
        }

        outSections = sectionsExtractor.getSectionsCollection();
        outSectionPoints = sectionsExtractor.getSectionPointsCollection();
        outRiverPoints = sectionsExtractor.getRiverPointsCollection();
    }

    public static void main( String[] args ) throws Exception {

        String base = "/storage/lavori_tmp/SILLI/ciclabile_moena/";
        
        double dist = 10.0;
        double width = 15.0;


        OmsRiverSectionsExtractor ex = new OmsRiverSectionsExtractor();
        ex.inElev = OmsRasterReader.readRaster(base + "lidarMoenaSmall.asc");
        ex.inRiver = OmsVectorReader.readVector(base + "trattoAvisioCiclabile.shp");
//        ex.inSections = OmsVectorReader.readVector(base + "sadole_sections_progetto.shp");
        ex.pSectionsWidth = width;
        ex.pSectionsIntervalDistance = dist;
//        ex.inRiverPoints = OmsVectorReader.readVector(base + "net_point_slope.shp");
        ex.process();
        SimpleFeatureCollection outSections2 = ex.outSections;
        SimpleFeatureCollection outSectionsPoints2 = ex.outSectionPoints;
        SimpleFeatureCollection outRiverPoints2 = ex.outRiverPoints;

        OmsVectorWriter.writeVector(base + "moena_ciclabile.gpkg#sections_" + ((int)dist), outSections2);
        OmsVectorWriter.writeVector(base + "moena_ciclabile.gpkg#sectionpoints_" + ((int)dist), outSectionsPoints2);
        OmsVectorWriter.writeVector(base + "moena_ciclabile.gpkg#riverpoints_" + ((int)dist), outRiverPoints2);

        // OmsRiverSectionsExtractor ex = new OmsRiverSectionsExtractor();
        // ex.inElev = OmsRasterReader.readRaster(base + "dtm_04.asc");
        // ex.inRiver = OmsVectorReader.readVector(base + "net_10000_daiano_11.shp");
        // ex.pSectionsIntervalDistance = 10;
        // ex.pSectionsWidth = 30;
        // ex.process();
        // SimpleFeatureCollection outSections2 = ex.outSections;
        // SimpleFeatureCollection outSectionsPoints2 = ex.outSectionPoints;
        // SimpleFeatureCollection outRiverPoints2 = ex.outRiverPoints;
        //
        // OmsVectorWriter.writeVector(base + "sections_test.shp", outSections2);
        // OmsVectorWriter.writeVector(base + "sectionpoints_test.shp", outSectionsPoints2);
        // OmsVectorWriter.writeVector(base + "riverpoints_test.shp", outRiverPoints2);

    }

}