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
import oms3.annotations.Unit;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.features.FilterUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

@Description(OmsLW05_BridgesDamsWidthAdder.DESCRIPTION)
@Author(name = OmsLW05_BridgesDamsWidthAdder.AUTHORS, contact = OmsLW05_BridgesDamsWidthAdder.CONTACTS)
@Keywords(OmsLW05_BridgesDamsWidthAdder.KEYWORDS)
@Label(OmsLW05_BridgesDamsWidthAdder.LABEL)
@Name("_" + OmsLW05_BridgesDamsWidthAdder.NAME)
@Status(OmsLW05_BridgesDamsWidthAdder.STATUS)
@License(OmsLW05_BridgesDamsWidthAdder.LICENSE)
public class OmsLW05_BridgesDamsWidthAdder extends HMModel implements LWFields {
    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description(inBridges_DESCR)
    @In
    public SimpleFeatureCollection inBridges = null;

    @Description(inDams_DESCR)
    @In
    public SimpleFeatureCollection inDams = null;

    @Description(pDamsOnNetDistance_DESCR)
    @Unit("m")
    @In
    public double pDamsOnNetDistance = 15.0;

    @Description(pBridgesOnNetDistance_DESCR)
    @Unit("m")
    @In
    public double pBridgesOnNetDistance = 15.0;

    @Description(pFixedDamsWidth_DESCR)
    @Unit("m")
    @In
    public double pFixedDamsWidth = 0.1;

    @Description(bridgeLenghtField_DESCR)
    @In
    public String fBridgeLenght = "LENGHT";

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description(outProblemBridges_DESCR)
    @Out
    public SimpleFeatureCollection outProblemBridges = null;

    // VARS DOC START
    public static final String outProblemBridges_DESCR = "The output layer containing the points of the structures containing no information of the width.";
    public static final String outNetPoints_DESCR = "The output points network layer with the bankfull width updated with the information of bridges and dams width.";
    public static final String bridgeLenghtField_DESCR = "Name of the attribute field of the bridges layer to use as width of the channel under the bridge.";
    public static final String pFixedDamsWidth_DESCR = "Fixed value of the width to assign to the sections where there is a check dam.";
    public static final String pBridgesOnNetDistance_DESCR = "Distance within which a bridge is assumed to be on the network.";
    public static final String pDamsOnNetDistance_DESCR = "Distance within which a dam is assumed to be on the network.";
    public static final String inDams_DESCR = "The input point layer with the check dams to consider to modify channel width.";
    public static final String inBridges_DESCR = "The input point layer with the bridges to consider to modify channel width.";
    public static final String inNetPoints_DESCR = "The input hierarchy point network layer.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw05_bridgesdamswidthadder";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, point, bankflull, width";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Correct the bankfull width of the sections of the channels where a bridge or a check dam is found, set the attribute with the origin of the width to the corresponding value.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {

        /*
         * of the input layer with dams consider only the check dams to 
         * contract the channel, check dams have TYPE 230 and 237
         */
        //TODO: remove this filter??
        Filter briglieFilter = FilterUtilities.getCQLFilter("TYPE > 230 AND TYPE < 237");

        /*
         * reads the network
         */
        List<SimpleFeature> netList = FeatureUtilities.featureCollectionToList(inNetPoints);

        STRtree netTree = new STRtree(netList.size());
        for( SimpleFeature netFeature : netList ) {
            Geometry geometry = (Geometry) netFeature.getDefaultGeometry();
            netTree.insert(geometry.getEnvelopeInternal(), netFeature);
        }

        /*
         * handles check dams
         */
        inDams = inDams.subCollection(briglieFilter);
        List<SimpleFeature> briglieList = FeatureUtilities.featureCollectionToList(inDams);
        for( SimpleFeature brigliaFeature : briglieList ) {
            Geometry brigliaGeometry = (Geometry) brigliaFeature.getDefaultGeometry();
            Envelope envelopeInternal = brigliaGeometry.getEnvelopeInternal();
            Envelope envelopeExpanded = new Envelope(envelopeInternal);
            envelopeExpanded.expandBy(pDamsOnNetDistance);

            /*
             * finds the nearest point on the network within the minDistance for Dams
             */
            List netPoints = netTree.query(envelopeExpanded);
            SimpleFeature nearestPoint = null;
            double minDistance = Double.POSITIVE_INFINITY;
            for( Object netPoint : netPoints ) {
                if (netPoint instanceof SimpleFeature) {
                    SimpleFeature netFeature = (SimpleFeature) netPoint;
                    Geometry point = (Geometry) netFeature.getDefaultGeometry();
                    double distance = brigliaGeometry.distance(point);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestPoint = netFeature;
                    }
                }
            }
            /*
             * set the attributes of the nearest point to contract the channel width
             * to be 0.1 m and origin from dams
             */
            if (nearestPoint != null) {
                // briglia filtrante strozza
                nearestPoint.setAttribute(WIDTH, pFixedDamsWidth);
                nearestPoint.setAttribute(WIDTH_FROM, WIDTH_FROM_DAMS);
            }
        }

        /*
         * handles bridges
         */

        // adds the bridges without the information of length to the output FC
        outProblemBridges = new DefaultFeatureCollection();
        List<SimpleFeature> pontiList = FeatureUtilities.featureCollectionToList(inBridges);
        for( SimpleFeature pontiFeature : pontiList ) {
            Geometry pontiGeometry = (Geometry) pontiFeature.getDefaultGeometry();

            // check if there is a regular bridge length
            double length = (Double) pontiFeature.getAttribute(fBridgeLenght);
            if (length == 0.0) {
                ((DefaultFeatureCollection) outProblemBridges).add(pontiFeature);
                continue;
            }

            Envelope envelopeInternal = pontiGeometry.getEnvelopeInternal();
            Envelope envelopeExpanded = new Envelope(envelopeInternal);
            envelopeExpanded.expandBy(pBridgesOnNetDistance);

            /*
             * finds the nearest point on the network within the minDistance for Bridges
             */
            List netPoints = netTree.query(envelopeExpanded);
            SimpleFeature nearestPoint = null;
            double minDistance = Double.POSITIVE_INFINITY;
            for( Object netPoint : netPoints ) {
                if (netPoint instanceof SimpleFeature) {
                    SimpleFeature netFeature = (SimpleFeature) netPoint;
                    Geometry point = (Geometry) netFeature.getDefaultGeometry();
                    double distance = pontiGeometry.distance(point);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestPoint = netFeature;
                    }
                }
            }
            if (nearestPoint != null) {
                // set bridge length as width
                nearestPoint.setAttribute(WIDTH, length);
                nearestPoint.setAttribute(WIDTH_FROM, WIDTH_FROM_BRIDGES);
            }
        }

        // adds the data to the output FC
        outNetPoints = new DefaultFeatureCollection();
        ((DefaultFeatureCollection) outNetPoints).addAll(netList);

    }

    public static void main( String[] args ) throws Exception {

        String base = "D:/lavori_tmp/unibz/2016_06_gsoc/data01/";

        OmsLW05_BridgesDamsWidthAdder ex = new OmsLW05_BridgesDamsWidthAdder();
        ex.inNetPoints = OmsVectorReader.readVector(base + "net_point_width.shp");
        ex.inBridges = OmsVectorReader.readVector(base + "W_Brcke_2008_extract.shp");
        ex.inDams = OmsVectorReader.readVector(base + "W_Querwerke_2008_extract.shp");
        ex.pFixedDamsWidth = 2.0;

        ex.process();
        SimpleFeatureCollection outNetPoints = ex.outNetPoints;
        
        OmsVectorWriter.writeVector(base + "net_point_width_damsbridg.shp", outNetPoints);

    }
    
}
