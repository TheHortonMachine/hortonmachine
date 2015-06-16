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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras;

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.AUTHORNAMES;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder.STATUS;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.riversections.ARiverSectionsExtractor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.riversections.RiverPoint;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

@Description(DESCRIPTION)
@Author(name = AUTHORNAMES, contact = AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class OmsHecrasInputBuilder extends JGTModel {
    @Description(inRiver_DESCRIPTION)
    @In
    public SimpleFeatureCollection inRiverPoints = null;

    @Description(inSections_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSections = null;

    @Description(inSectionPoints_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSectionPoints = null;

    @Description(pTitle_DESCRIPTION)
    @In
    public String pTitle = "DEFAULTID";

    @Description(outHecras_DESCRIPTION)
    @In
    @UI(JGTConstants.FILEIN_UI_HINT)
    public String outHecras = null;

    public static final String DESCRIPTION = "Module that prepares input data for Hecras.";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "Hecras, Raster, Vector, Hydraulic";
    public static final String LABEL = JGTConstants.HYDROGEOMORPHOLOGY;
    public static final String NAME = "inhecras";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "Andrea Antonello, Silvia Franceschi";
    public static final String AUTHORCONTACTS = "www.hydrologis.com";
    public static final String inRiver_DESCRIPTION = "The river points with elevation.";
    public static final String inSections_DESCRIPTION = "The section lines.";
    public static final String inSectionPoints_DESCRIPTION = "The section points (with the elevation in the attribute table).";
    public static final String pTitle_DESCRIPTION = "The id of the river/simulation.";
    public static final String pSectionsIntervalDistance_DESCRIPTION = "The sections interval distance.";
    public static final String pSectionsWidth_DESCRIPTION = "The section width.";
    public static final String pBridgeBuffer_DESCRIPTION = "The bridge buffer.";
    public static final String fBridgeWidth_DESCRIPTION = "The bridge width.";
    public static final String outHecras_DESCRIPTION = "The path to the generated hecras.";

    @Execute
    public void process() throws Exception {
        checkNull(inRiverPoints, inSectionPoints, inSections);

        List<SimpleFeature> riverFeatures = FeatureUtilities.featureCollectionToList(inRiverPoints);
        TreeMap<Integer, SimpleFeature> orderedRiverPoints = new TreeMap<>();
        for( SimpleFeature riverFeature : riverFeatures ) {
            int sectionId = ((Number) riverFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID)).intValue();
            orderedRiverPoints.put(sectionId, riverFeature);
        }
        int count = 0;
        Coordinate[] riverCoords = new Coordinate[orderedRiverPoints.size()];
        for( Entry<Integer, SimpleFeature> riverEntry : orderedRiverPoints.entrySet() ) {
            SimpleFeature riverPoint = riverEntry.getValue();
            Coordinate coordinate = ((Geometry) riverPoint.getDefaultGeometry()).getCoordinate();
            double elev = ((Number) riverPoint.getAttribute(ARiverSectionsExtractor.FIELD_ELEVATION)).doubleValue();
            riverCoords[count++] = new Coordinate(coordinate.x, coordinate.y, elev);
        }
        LineString riverGeometry = gf.createLineString(riverCoords);

        List<SimpleFeature> sectionFeatures = FeatureUtilities.featureCollectionToList(inSections);
        List<SimpleFeature> sectionPointFeatures = FeatureUtilities.featureCollectionToList(inSectionPoints);

        TreeMap<Integer, SimpleFeature> orderedSections = new TreeMap<>();
        for( SimpleFeature sectionFeature : sectionFeatures ) {
            int sectionId = ((Number) sectionFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID)).intValue();
            orderedSections.put(sectionId, sectionFeature);
        }

        HashMap<Integer, TreeMap<Integer, SimpleFeature>> sectionId2PointId2PointMap = new HashMap<>();
        for( SimpleFeature pointFeature : sectionPointFeatures ) {
            int sectionId = ((Number) pointFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID)).intValue();
            TreeMap<Integer, SimpleFeature> pointId2PointMap = sectionId2PointId2PointMap.get(sectionId);
            if (pointId2PointMap == null) {
                pointId2PointMap = new TreeMap<>();
                sectionId2PointId2PointMap.put(sectionId, pointId2PointMap);
            }
            int pointId = ((Number) pointFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTIONPOINT_INDEX)).intValue();
            pointId2PointMap.put(pointId, pointFeature);
        }

        List<RiverPoint> orderedNetworkPoints = new ArrayList<>();// sectionsExtractor.getOrderedNetworkPoints();
        for( Entry<Integer, SimpleFeature> sectionEntry : orderedSections.entrySet() ) {
            Integer sectionId = sectionEntry.getKey();
            SimpleFeature sectionFeature = sectionEntry.getValue();
            double progressive = ((Number) sectionFeature.getAttribute(ARiverSectionsExtractor.FIELD_PROGRESSIVE)).doubleValue();
            Geometry sectionLine = (Geometry) sectionFeature.getDefaultGeometry();

            Geometry intersectionPoint = riverGeometry.intersection(sectionLine);
            if (intersectionPoint == null) {
                throw new ModelsRuntimeException("All sections have to intersect the river line.", this);
            }

            TreeMap<Integer, SimpleFeature> sectionPoints = sectionId2PointId2PointMap.get(sectionId);
            Coordinate[] sectionCoords = new Coordinate[sectionPoints.size()];
            count = 0;
            for( Entry<Integer, SimpleFeature> pointEntry : sectionPoints.entrySet() ) {
                SimpleFeature sectionPoint = pointEntry.getValue();
                sectionCoords[count++] = ((Geometry) sectionPoint.getDefaultGeometry()).getCoordinate();
            }
            LineString sectionLineWithPoints = gf.createLineString(sectionCoords);
            RiverPoint rp = new RiverPoint(intersectionPoint.getCoordinate(), progressive, sectionLineWithPoints);
            orderedNetworkPoints.add(rp);
        }
        int sectionsCount = orderedNetworkPoints.size();

        for( Entry<Integer, SimpleFeature> riverEntry : orderedRiverPoints.entrySet() ) {
            SimpleFeature riverPoint = riverEntry.getValue();
            Coordinate coordinate = ((Geometry) riverPoint.getDefaultGeometry()).getCoordinate();
            double progressive = ((Number) riverPoint.getAttribute(ARiverSectionsExtractor.FIELD_PROGRESSIVE)).doubleValue();
            RiverPoint rp = new RiverPoint(coordinate, progressive, null);
            orderedNetworkPoints.add(rp);
        }

        Collections.sort(orderedNetworkPoints);

        StringBuilder outBuf = new StringBuilder();

        outBuf.append("# Header must contain a record to identify the ");
        outBuf.append("unit system used in the imported data set\r\n");
        outBuf.append("BEGIN HEADER:\r\n");
        outBuf.append("# Number of reaches\r\n");
        outBuf.append("NUMBER OF REACHES: 1\r\n");
        outBuf.append("# Number of cross sections\r\n");
        outBuf.append("NUMBER OF CROSS-SECTIONS:\r\n" + sectionsCount + "\r\n");
        outBuf.append("# Unit system used\r\n");
        outBuf.append("UNITS: METRIC\r\n");
        outBuf.append("END HEADER:\r\n");
        outBuf.append("\r\n");

        outBuf.append("BEGIN STREAM NETWORK:\r\n");
        outBuf.append("# List of all endpoint of the multiline that represents the river\r\n");
        outBuf.append("ENDPOINT:\t" + riverCoords[0].x + "," + riverCoords[0].y + "," + riverCoords[0].z + ",\t1\r\n");
        outBuf.append("ENDPOINT:\t" + riverCoords[(riverCoords.length - 1)].x + "," + riverCoords[(riverCoords.length - 1)].y
                + "," + riverCoords[(riverCoords.length - 1)].z + ",\t2\r\n");

        outBuf.append("# Description of the river reach\r\n");
        outBuf.append("REACH:\r\n");
        outBuf.append("STREAM ID: " + pTitle + "\r\n");
        outBuf.append("REACH ID: headwaters\r\n");

        outBuf.append("# Upsteam endpoint\r\n");
        outBuf.append("FROM POINT: 1\r\n");
        outBuf.append("# Downsteam endpoint\r\n");
        outBuf.append("TO POINT: 2\r\n");
        outBuf.append("\r\n");
        outBuf.append("# Coordinates and floating point station");
        outBuf.append(" value to draw the river network:\r\n");
        outBuf.append("\r\n");
        outBuf.append("CENTERLINE:\r\n");

        int orderedNetworkPointsSize = orderedNetworkPoints.size();
        for( int i = 0; i < orderedNetworkPointsSize; ++i ) {
            // mind, the reach points and the sections need to walk in reverse order!
            int iRev = orderedNetworkPointsSize - 1 - i;
            RiverPoint networkPoint = orderedNetworkPoints.get(i);
            if (networkPoint.hasSection) {
                continue;
            }
            Coordinate tmpCoord = networkPoint.point;
            outBuf.append(tmpCoord.x + ",\t" + tmpCoord.y + ",\t" + tmpCoord.z + ",\t" + iRev + "\r\n");
        }

        outBuf.append("END:\r\n");
        outBuf.append("\r\n");
        outBuf.append("END STREAM NETWORK:\r\n");
        outBuf.append("BEGIN CROSS-SECTIONS:\r\n");
        outBuf.append("\r\n");

        // get only sections with their indexes
        List<RiverPoint> sectionPoints = new ArrayList<RiverPoint>();
        List<Integer> sectionIndexes = new ArrayList<Integer>();
        for( int i = 0; i < orderedNetworkPointsSize; i++ ) {
            int iRev = orderedNetworkPointsSize - 1 - i;
            RiverPoint currentNetworkPoint = orderedNetworkPoints.get(i);
            if (currentNetworkPoint.hasSection) {
                sectionPoints.add(currentNetworkPoint);
                sectionIndexes.add(iRev);
                currentNetworkPoint.setSectionId(iRev);
            }
        }

        pm.beginTask("Building cross-sections geometry...", sectionPoints.size());
        for( int i = 0; i < sectionPoints.size(); i++ ) {
            RiverPoint currentNetworkPoint = sectionPoints.get(i);
            RiverPoint nextNetworkPoint = currentNetworkPoint;
            if (i + 1 != sectionPoints.size()) {
                nextNetworkPoint = sectionPoints.get(i + 1);
            }

            Integer currentNetworkPointIndex = sectionIndexes.get(i);

            outBuf.append("\r\n");
            outBuf.append("# New Cross Section\r\n");
            outBuf.append("# Cross section must include records identifying the stream, reach and station value of cross section\r\n");

            outBuf.append("CROSS-SECTION:\r\n");
            outBuf.append("STREAM ID: " + pTitle + "\r\n");
            outBuf.append("REACH ID: headwaters\r\n");

            outBuf.append("STATION: " + currentNetworkPointIndex + "\r\n");

            List<Double> bankPositions = currentNetworkPoint.bankPositions;
            outBuf.append("BANK POSITIONS:\t" + bankPositions.get(0) + ",\t" + bankPositions.get(1) + "\r\n");

            Coordinate[] coordinates = currentNetworkPoint.sectionGeometry.getCoordinates();
            Coordinate firstCoordinateOfSection = coordinates[0];
            Coordinate lastCoordinateOfSection = coordinates[coordinates.length - 1];
            Coordinate[] nextCoordinates = nextNetworkPoint.sectionGeometry.getCoordinates();
            Coordinate firstCoordinateOfNextSection = nextCoordinates[0];
            Coordinate lastCoordinateOfNextSection = nextCoordinates[nextCoordinates.length - 1];

            outBuf.append("REACH LENGTHS: " + firstCoordinateOfSection.distance(firstCoordinateOfNextSection) + ",\t"
                    + currentNetworkPoint.point.distance(nextNetworkPoint.point) + ",\t"
                    + lastCoordinateOfSection.distance(lastCoordinateOfNextSection) + "\r\n");
            outBuf.append("NVALUES: \r\n");
            outBuf.append("0.00,\t0.0333\r\n");
            if (bankPositions.size() == 4) {
                outBuf.append("LEVEE POSITIONS:\r\n");
                outBuf.append("1," + bankPositions.get(0) + "," + bankPositions.get(2) + "\r\n");
                outBuf.append("2," + bankPositions.get(1) + "," + bankPositions.get(3) + "\r\n");
            }
            outBuf.append("CUT LINE: \r\n");
            outBuf.append(firstCoordinateOfSection.x + ",\t" + firstCoordinateOfSection.y + "\r\n");
            outBuf.append(lastCoordinateOfSection.x + ",\t" + lastCoordinateOfSection.y + "\r\n");

            outBuf.append("\r\n");
            outBuf.append("SURFACE LINE: \r\n");
            for( int j = 0; j < coordinates.length; ++j ) {
                outBuf.append(coordinates[j].x + ",\t" + coordinates[j].y + ",\t" + coordinates[j].z + "\r\n");
            }
            outBuf.append("END:\r\n");

            pm.worked(1);
        }
        pm.done();
        outBuf.append("\r\n");
        outBuf.append("END CROSS-SECTIONS:\r\n");

        String outString = outBuf.toString();
        FileUtilities.writeFile(outString, new File(outHecras));

    }
    
    public static void main( String[] args ) throws Exception {
        String base = "";

        OmsHecrasInputBuilder h = new OmsHecrasInputBuilder();
        h.pTitle = "Testsim";
        h.inRiverPoints = OmsVectorReader.readVector(base + "riverpoints_test2.shp");
        h.inSections = OmsVectorReader.readVector(base + "sections_test2.shp");
        h.inSectionPoints = OmsVectorReader.readVector(base + "sectionpoints_test2.shp");
        h.outHecras = base + "hecras.txt";
        h.process();
        
        
    }
}
