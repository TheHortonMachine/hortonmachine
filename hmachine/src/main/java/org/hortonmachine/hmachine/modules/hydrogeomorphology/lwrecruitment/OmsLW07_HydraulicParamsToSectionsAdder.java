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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureExtender;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.ARiverSectionsExtractor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
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
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OmsLW07_HydraulicParamsToSectionsAdder.DESCRIPTION)
@Author(name = OmsLW07_HydraulicParamsToSectionsAdder.AUTHORS, contact = OmsLW07_HydraulicParamsToSectionsAdder.CONTACTS)
@Keywords(OmsLW07_HydraulicParamsToSectionsAdder.KEYWORDS)
@Label(OmsLW07_HydraulicParamsToSectionsAdder.LABEL)
@Name("_" + OmsLW07_HydraulicParamsToSectionsAdder.NAME)
@Status(OmsLW07_HydraulicParamsToSectionsAdder.STATUS)
@License(OmsLW07_HydraulicParamsToSectionsAdder.LICENSE)
public class OmsLW07_HydraulicParamsToSectionsAdder extends HMModel implements LWFields {
    @Description(inDtm_DESCR)
    @In
    public GridCoverage2D inDtm = null;

    @Description(inNet_DESCR)
    @In
    public SimpleFeatureCollection inNet = null;

    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description(inDischarge_DESCRIPTION)
    @Unit("m3/s")
    @In
    public double pDischarge;

    @Description(pDeltaTMillis_DESCRIPTION)
    @Unit(pDeltaTMillis_UNIT)
    @In
    public long pDeltaTMillis = 5000;
    
    @Description(doMaxWidening_DESCRIPTION)
    @In
    public boolean doMaxWidening = false;

    @Description(outputLevelFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputLevelFile;

    @Description(outputDischargeFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputDischargeFile;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description(outTransSect_DESCR)
    @Out
    public SimpleFeatureCollection outTransSect = null;

    // VARS DOC START
    public static final String inNetPoints_DESCR = "The input hierarchy point network layer.";
    public static final String inDtm_DESCR = "The input terrain elevation raster map.";
    public static final String inNet_DESCR = "The input hierarchy network layer";
    public static final String inDischarge_DESCRIPTION = "The input discharge value";
    public static final String pDeltaTMillis_UNIT = "millisec";
    public static final String pDeltaTMillis_DESCRIPTION = "Time interval.";
    public static final String doMaxWidening_DESCRIPTION = "Boolean factor to define if the 1D model has to run with bankfull width or with maximum widening width.";
    public static final String outputLevelFile_DESCRIPTION = "Output file with levels.";
    public static final String outputDischargeFile_DESCRIPTION = "Output file with the quantities related to discharge.";
    public static final String outTransSect_DESCR = "The output line shapefile with the extracted transversal sections.";

    public static final String outNetPoints_DESCR = "Adds to the layer of section points the hydraulic parameters of the trasversal section.";

    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw07_hydraulicparamstosectionsadder";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "hydraulic, network, vector, point, bankflull, width";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Adds to the layer of section points the hydraulic parameters of the transversal section.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {

        OmsRiverSectionsExtractor ex = new OmsRiverSectionsExtractor();
        ex.inElev = inDtm;
        ex.inRiver = inNet;
        ex.inRiverPoints = inNetPoints;
        ex.process();
        SimpleFeatureCollection outSections = ex.outSections;
        SimpleFeatureCollection outSectionsPoints = ex.outSectionPoints;
        SimpleFeatureCollection outRiverPoints = ex.outRiverPoints;

        
        String widthField = LWFields.WIDTH;
        String ksField = LWFields.GAUKLER;
        String fieldWaterLevel = LWFields.FIELD_WATER_LEVEL;
        String fieldDischarge = LWFields.FIELD_DISCHARGE;
        String fieldWaterVelocity = LWFields.FIELD_WATER_VELOCITY;
        if (doMaxWidening) {
            widthField = LWFields.WIDTH2;
            fieldWaterLevel = LWFields.FIELD_WATER_LEVEL2;
            fieldDischarge = LWFields.FIELD_DISCHARGE2;
            fieldWaterVelocity = LWFields.FIELD_WATER_VELOCITY2;
        }

        // add pfafstetter to the sections
        List<SimpleFeature> netPointsFC = FeatureUtilities.featureCollectionToList(inNetPoints);
        HashMap<Integer, String> id2PfafMap = new HashMap<>();
        HashMap<Integer, Double> id2WidthMap = new HashMap<>();
        for( SimpleFeature netPointFeature : netPointsFC ) {
            Integer id = (Integer) netPointFeature.getAttribute(LWFields.LINKID);
            String pfaff = (String) netPointFeature.getAttribute(LWFields.PFAF);
            Double width = (Double) netPointFeature.getAttribute(widthField);
            id2PfafMap.put(id, pfaff);
            id2WidthMap.put(id, width);
        }
        outTransSect = new DefaultFeatureCollection();
        FeatureExtender sectionsExtender = new FeatureExtender(outSections.getSchema(),
                new String[]{LWFields.LINKID, LWFields.PFAF}, new Class[]{Integer.class, String.class});
        List<SimpleFeature> sectionsList = FeatureUtilities.featureCollectionToList(outSections);
        for( SimpleFeature sectionFeature : sectionsList ) {
            Integer id = (Integer) sectionFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID);
            String pfaf = id2PfafMap.get(id);
            SimpleFeature newFeature = sectionsExtender.extendFeature(sectionFeature, new Object[]{id, pfaf});

            Geometry origGeom = (Geometry) newFeature.getDefaultGeometry();

            double l = origGeom.getLength();
            double w = id2WidthMap.get(id);

            double x = (l - w) / 2.0;
            double factor = x / l;

            Coordinate[] coordinates = origGeom.getCoordinates();
            LineSegment ls = new LineSegment(coordinates[0], coordinates[coordinates.length - 1]);
            Coordinate c1 = ls.pointAlong(factor);
            Coordinate c2 = ls.pointAlong(1 - factor);

            LineString newLine = GeometryUtilities.gf().createLineString(new Coordinate[]{c1, c2});
            newFeature.setDefaultGeometry(newLine);
            ((DefaultFeatureCollection) outTransSect).add(newFeature);
        }
        // outTransSect

        // run hydraulic model
        OmsSaintGeo saintGeo = new OmsSaintGeo();
        saintGeo.inRiverPoints = outRiverPoints;
        saintGeo.inSectionPoints = outSectionsPoints;
        saintGeo.inSections = outTransSect;
        saintGeo.inDischarge = new double[]{pDischarge};
        saintGeo.pDeltaTMillis = pDeltaTMillis;
        saintGeo.outputLevelFile = outputLevelFile;
        saintGeo.outputDischargeFile = outputDischargeFile;
        saintGeo.process();

        HashMap<Integer, Double> lastLinkId2RelativeLevelMap = saintGeo.getLastLinkId2RelativeLevelMap();
        HashMap<Integer, Double> lastLinkId2DischargeMap = saintGeo.getLastLinkId2DischargeMap();
        HashMap<Integer, Double> lastLinkId2VelocityMap = saintGeo.getLastLinkId2VelocityMap();

        FeatureExtender ext = new FeatureExtender(inNetPoints.getSchema(),
                new String[]{fieldWaterLevel, fieldDischarge, fieldWaterVelocity},
                new Class[]{Double.class, Double.class, Double.class});

        outNetPoints = new DefaultFeatureCollection();
        for( SimpleFeature netPointFeature : netPointsFC ) {
            Integer id = (Integer) netPointFeature.getAttribute(LWFields.LINKID);
            Double level = lastLinkId2RelativeLevelMap.get(id);
            Double discharge = lastLinkId2DischargeMap.get(id);
            Double velocity = lastLinkId2VelocityMap.get(id);

            SimpleFeature newFeature = ext.extendFeature(netPointFeature, new Object[]{level, discharge, velocity});
            ((DefaultFeatureCollection) outNetPoints).add(newFeature);
        }
    }

    public static void main( String[] args ) throws Exception {

        String base = "D:/lavori_tmp/unibz/2016_06_gsoc/data01/";
        String baseRaster = "D:/lavori_tmp/unibz/2016_06_gsoc/raster/";

        OmsLW07_HydraulicParamsToSectionsAdder ex = new OmsLW07_HydraulicParamsToSectionsAdder();
        ex.inDtm = OmsRasterReader.readRaster(baseRaster + "dtmfel.asc");
        ex.inNet = OmsVectorReader.readVector(base + "extracted_net.shp");
        ex.inNetPoints = OmsVectorReader.readVector(base + "net_point_width_damsbridg_slope_lateral_inund_veg_80_rast.shp");
//        ex.inNetPoints = OmsVectorReader.readVector(base + "net_point_width_damsbridg_slope.shp");
        ex.pDischarge = 3.0;
        ex.outputLevelFile = base + "levels_lateral2.csv";
        ex.outputDischargeFile = base + "discharge_lateral2.csv";
        ex.doMaxWidening = true;

        ex.process();
        SimpleFeatureCollection outNetPoints = ex.outNetPoints;
        SimpleFeatureCollection outTransSect = ex.outTransSect;

        OmsVectorWriter.writeVector(base + "extracted_bankfullsections_lateral3.shp", outTransSect);
        OmsVectorWriter.writeVector(base + "net_point_width_damsbridg_slope_lateral_inund_veg_80_rast_lateral3.shp", outNetPoints);

    }

}
