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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.riversections;
import static org.jgrasstools.gears.libs.modules.JGTConstants.HYDROGEOMORPHOLOGY;

import java.awt.geom.Point2D;
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
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

@Description(OmsRiverSectionsExtractor.DESCRIPTION)
@Author(name = OmsRiverSectionsExtractor.AUTHORNAMES, contact = OmsRiverSectionsExtractor.AUTHORCONTACTS)
@Keywords(OmsRiverSectionsExtractor.KEYWORDS)
@Label(OmsRiverSectionsExtractor.LABEL)
@Name(OmsRiverSectionsExtractor.NAME)
@Status(OmsRiverSectionsExtractor.STATUS)
@License(OmsRiverSectionsExtractor.LICENSE)
public class OmsRiverSectionsExtractor extends JGTModel {

    @Description(inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(inRiver_DESCRIPTION)
    @In
    public SimpleFeatureCollection inRiver = null;

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

    public static final String DESCRIPTION = "Module that prepares data for Hecras.";
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
    public static final String inBridges_DESCRIPTION = "The map of bridges points to consider.";
    public static final String inSections_DESCRIPTION = "The map of sections to consider. If supplied, they are used instead of extracting at a given interval. The sections need to be created with this same module.";
    public static final String pSectionsIntervalDistance_DESCRIPTION = "The sections interval distance.";
    public static final String pSectionsWidth_DESCRIPTION = "The section width.";
    public static final String pBridgeBuffer_DESCRIPTION = "The bridge buffer.";
    public static final String fBridgeWidth_DESCRIPTION = "The bridge width.";
    public static final String outSections_DESCRIPTION = "The extracted section lines.";
    public static final String outSectionPoints_DESCRIPTION = "The extracted section points (with the elevation in the attribute table).";
    public static final String outRiverPoints_DESCRIPTION = "The extracted river points (with the elevation in the attribute table).";

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
            sectionsExtractor = new RiverSectionsFromDtmExtractor(riverGeometry3d, inElev, pSectionsIntervalDistance,
                    pSectionsWidth, bridgePoints, fBridgeWidth, pBridgeBuffer, pm);
        } else {
            List<FeatureMate> sectionsList = FeatureUtilities.featureCollectionToMatesList(inSections);
            sectionsExtractor = new RiverSectionsFromFeaturesExtractor(riverGeometry3d, inElev, sectionsList, pm);
        }

        outSections = sectionsExtractor.getSectionsCollection();
        outSectionPoints = sectionsExtractor.getSectionPointsCollection();
        outRiverPoints = sectionsExtractor.getRiverPointsCollection();
    }

    public static void main( String[] args ) throws Exception {

        String base = "";

        OmsRiverSectionsExtractor ex = new OmsRiverSectionsExtractor();
        ex.inElev = OmsRasterReader.readRaster(base + "dtm_04.asc");
        ex.inRiver = OmsVectorReader.readVector(base + "net_10000_daiano_11.shp");
        ex.inSections = OmsVectorReader.readVector(base + "sections_test.shp");
        ex.pSectionsIntervalDistance = 10;
        ex.pSectionsWidth = 30;
        ex.process();
        SimpleFeatureCollection outSections2 = ex.outSections;
        SimpleFeatureCollection outSectionsPoints2 = ex.outSectionPoints;
        SimpleFeatureCollection outRiverPoints2 = ex.outRiverPoints;

        OmsVectorWriter.writeVector(base + "sections_test2.shp", outSections2);
        OmsVectorWriter.writeVector(base + "sectionpoints_test2.shp", outSectionsPoints2);
        OmsVectorWriter.writeVector(base + "riverpoints_test2.shp", outRiverPoints2);

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