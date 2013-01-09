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
package org.jgrasstools.modules;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_fBridgeWidth_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inBridges_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inHecras_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inRiver_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inSections_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_outSectionPoints_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_outSections_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pBridgeBuffer_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pSectionsIntervalDistance_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pSectionsWidth_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pTitle_DESCRIPTION;

import java.awt.geom.Point2D;
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
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.HecrasSectionsExtractor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.HecrasSectionsFromDtmExtractor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.HecrasSectionsFromFeaturesExtractor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.NetworkPoint;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

@Description(OMSHECRASINPUTBUILDER_DESCRIPTION)
@Author(name = OMSHECRASINPUTBUILDER_AUTHORNAMES, contact = OMSHECRASINPUTBUILDER_AUTHORCONTACTS)
@Keywords(OMSHECRASINPUTBUILDER_KEYWORDS)
@Label(OMSHECRASINPUTBUILDER_LABEL)
@Name(OMSHECRASINPUTBUILDER_NAME)
@Status(OMSHECRASINPUTBUILDER_STATUS)
@License(OMSHECRASINPUTBUILDER_LICENSE)
public class OmsHecrasInputBuilder extends JGTModel {

    @Description(OMSHECRASINPUTBUILDER_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSHECRASINPUTBUILDER_inRiver_DESCRIPTION)
    @In
    public SimpleFeatureCollection inRiver = null;

    @Description(OMSHECRASINPUTBUILDER_inBridges_DESCRIPTION)
    @In
    public SimpleFeatureCollection inBridges = null;

    @Description(OMSHECRASINPUTBUILDER_inSections_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSections = null;

    @Description(OMSHECRASINPUTBUILDER_pTitle_DESCRIPTION)
    @In
    public String pTitle = "DEFAULTID";

    @Description(OMSHECRASINPUTBUILDER_pSectionsIntervalDistance_DESCRIPTION)
    @In
    public double pSectionsIntervalDistance = 0.0D;

    @Description(OMSHECRASINPUTBUILDER_pSectionsWidth_DESCRIPTION)
    @In
    public double pSectionsWidth = 0.0D;

    @Description(OMSHECRASINPUTBUILDER_pBridgeBuffer_DESCRIPTION)
    @In
    public double pBridgeBuffer = 0.0D;

    @Description(OMSHECRASINPUTBUILDER_fBridgeWidth_DESCRIPTION)
    @In
    public String fBridgeWidth;

    @Description(OMSHECRASINPUTBUILDER_inHecras_DESCRIPTION)
    @In
    @UI(JGTConstants.FILEIN_UI_HINT)
    public String inHecras = null;

    @Description(OMSHECRASINPUTBUILDER_outSections_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outSections = null;

    @Description(OMSHECRASINPUTBUILDER_outSectionPoints_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outSectionPoints = null;

    private GeometryFactory gf;

    @Execute
    public void process() throws Exception {
        checkNull(inElev, inRiver);

        gf = GeometryUtilities.gf();

        List<SimpleFeature> riverFeatures = FeatureUtilities.featureCollectionToList(inRiver);
        SimpleFeature riverFeature = riverFeatures.get(0);

        /*
         * TODO support for custom sections
         */
        // String asciiSectionsFolder = null;

        /*
         * END: TODO support for custom sections
         */

        Geometry geometry = (Geometry) riverFeature.getDefaultGeometry();
        Coordinate[] riverCoordinates = geometry.getCoordinates();

        pm.beginTask("Building reach geometry...", riverCoordinates.length);
        Point2D.Double point = new Point2D.Double();
        double[] extracted = new double[1];
        for( int i = 0; i < riverCoordinates.length; i++ ) {
            point.setLocation(riverCoordinates[i].x, riverCoordinates[i].y);
            inElev.evaluate(point, extracted);

            riverCoordinates[i] = new Coordinate(riverCoordinates[i].x, riverCoordinates[i].y, extracted[0]);
            pm.worked(1);
        }
        pm.done();

        LineString riverGeometry3d = gf.createLineString(riverCoordinates);

        List<FeatureMate> bridgesList = FeatureUtilities.featureCollectionToMatesList(inBridges);

        HecrasSectionsExtractor sectionsExtractor;
        if (inSections == null) {
            sectionsExtractor = new HecrasSectionsFromDtmExtractor(riverGeometry3d, inElev, pSectionsIntervalDistance,
                    pSectionsWidth, bridgesList, fBridgeWidth, pBridgeBuffer, pm);
        } else {
            List<FeatureMate> sectionsList = FeatureUtilities.featureCollectionToMatesList(inSections);
            sectionsExtractor = new HecrasSectionsFromFeaturesExtractor(riverGeometry3d, inElev, sectionsList, pm);
        }

        List<NetworkPoint> orderedNetworkPoints = sectionsExtractor.getOrderedNetworkPoints();

        StringBuilder outBuf = new StringBuilder();

        outBuf.append("# Header must contain a record to identify the ");
        outBuf.append("unit system used in the imported data set\r\n");
        outBuf.append("BEGIN HEADER:\r\n");
        outBuf.append("# Number of reaches\r\n");
        outBuf.append("NUMBER OF REACHES: 1\r\n");
        outBuf.append("# Number of cross sections\r\n");
        int sectionsCount = sectionsExtractor.getSectionsNum();
        outBuf.append("NUMBER OF CROSS-SECTIONS:\r\n" + sectionsCount + "\r\n");
        outBuf.append("# Unit system used\r\n");
        outBuf.append("UNITS: METRIC\r\n");
        outBuf.append("END HEADER:\r\n");
        outBuf.append("\r\n");

        outBuf.append("BEGIN STREAM NETWORK:\r\n");
        outBuf.append("# List of all endpoint of the multiline that represents the river\r\n");
        outBuf.append("ENDPOINT:\t" + riverCoordinates[0].x + "," + riverCoordinates[0].y + "," + riverCoordinates[0].z
                + ",\t1\r\n");
        outBuf.append("ENDPOINT:\t" + riverCoordinates[(riverCoordinates.length - 1)].x + ","
                + riverCoordinates[(riverCoordinates.length - 1)].y + "," + riverCoordinates[(riverCoordinates.length - 1)].z
                + ",\t2\r\n");

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
            NetworkPoint networkPoint = orderedNetworkPoints.get(i);
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
        List<NetworkPoint> sectionPoints = new ArrayList<NetworkPoint>();
        List<Integer> sectionIndexes = new ArrayList<Integer>();
        for( int i = 0; i < orderedNetworkPointsSize; i++ ) {
            int iRev = orderedNetworkPointsSize - 1 - i;
            NetworkPoint currentNetworkPoint = orderedNetworkPoints.get(i);
            if (currentNetworkPoint.hasSection) {
                sectionPoints.add(currentNetworkPoint);
                sectionIndexes.add(iRev);
                currentNetworkPoint.setSectionId(iRev);
            }
        }

        pm.beginTask("Building cross-sections geometry...", sectionPoints.size());
        for( int i = 0; i < sectionPoints.size(); i++ ) {
            NetworkPoint currentNetworkPoint = sectionPoints.get(i);
            NetworkPoint nextNetworkPoint = currentNetworkPoint;
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
        FileUtilities.writeFile(outString, new File(inHecras));

        outSections = sectionsExtractor.getSectionsCollection();
        outSectionPoints = sectionsExtractor.getSectionPointsCollection();
    }
}
