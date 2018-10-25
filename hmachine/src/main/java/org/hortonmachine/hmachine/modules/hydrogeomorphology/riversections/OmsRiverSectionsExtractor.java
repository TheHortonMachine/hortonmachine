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
import java.util.List;

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
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

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

@Description(OmsRiverSectionsExtractor.DESCRIPTION)
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

    public static final String DESCRIPTION = "Module that extract sections starting from a DTM and a main line (stream river, breakline,, ...).";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "Sections, Raster, Vector, Hydraulic";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "RiverSectionsExtractor";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "Andrea Antonello, Silvia Franceschi";
    public static final String AUTHORCONTACTS = "www.hydrologis.com";

    public static final String inElev_DESCRIPTION = "The map of elevation.";
    public static final String inRiver_DESCRIPTION = "The map of the river.";
    public static final String inRiverPoints_DESCRIPTION = "The optional map of the river points to extract.";
    public static final String inBridges_DESCRIPTION = "The map of bridges points to consider.";
    public static final String inSections_DESCRIPTION = "The map of sections to consider. If supplied, they are used instead of extracting at a given interval. The sections need to be created with this same module.";
    public static final String pSectionsIntervalDistance_DESCRIPTION = "The sections interval distance.";
    public static final String pSectionsWidth_DESCRIPTION = "The section width.";
    public static final String pBridgeBuffer_DESCRIPTION = "The bridge buffer.";
    public static final String fBridgeWidth_DESCRIPTION = "The bridge width.";
    public static final String outSections_DESCRIPTION = "The extracted section lines.";
    public static final String outSectionPoints_DESCRIPTION = "The extracted section points (with the elevation in the attribute table).";
    public static final String outRiverPoints_DESCRIPTION = "The extracted main stream points (with the elevation in the attribute table).";

    @Execute
    public void process() throws Exception {
        checkNull(inElev, inRiver);

        gf = GeometryUtilities.gf();

        List<SimpleFeature> riverFeatures = FeatureUtilities.featureCollectionToList(inRiver);
        SimpleFeature riverFeature = riverFeatures.get(0);

        Geometry geometry = (Geometry) riverFeature.getDefaultGeometry();
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

        String base = "D:\\lavori_tmp\\2018_idraulico_sadole\\sadole1D\\2018_04_11_data\\progetto_10\\";
        String baseRaster = "D:\\Dropbox\\hydrologis\\lavori\\2018_projects\\02_idraulico_sadole\\dati_trent2D\\stato_progetto\\";


        OmsRiverSectionsExtractor ex = new OmsRiverSectionsExtractor();
        ex.inElev = OmsRasterReader.readRaster(baseRaster + "dtm_sadole_proj_09.asc");
        ex.inRiver = OmsVectorReader.readVector(base + "net_final_slope_03.shp");
//        ex.inSections = OmsVectorReader.readVector(base + "sadole_sections_progetto.shp");
        ex.pSectionsWidth = 25.0;
        ex.pSectionsIntervalDistance = 10.0;
//        ex.inRiverPoints = OmsVectorReader.readVector(base + "net_point_slope.shp");
        ex.process();
        SimpleFeatureCollection outSections2 = ex.outSections;
        SimpleFeatureCollection outSectionsPoints2 = ex.outSectionPoints;
        SimpleFeatureCollection outRiverPoints2 = ex.outRiverPoints;

        OmsVectorWriter.writeVector(base + "sadole_sections_prj_10.shp", outSections2);
        OmsVectorWriter.writeVector(base + "sadole_sectionpoints_prj_10.shp", outSectionsPoints2);
        OmsVectorWriter.writeVector(base + "sadole_riverpoints_prj_10.shp", outRiverPoints2);

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