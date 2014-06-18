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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.features.FilterUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;

@Description("Correct the bankfull width in the section of the channels point where a bridge or a check dam is found, set the attribute with the origin of the with to the corresponding value.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, point, bankflull, width")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW05_BridgesDamsWidthAdder")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW05_BridgesDamsWidthAdder extends JGTModel implements LWFields {

    @Description("The input hierarchy point network layer.")
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description("The input point layer of with the bridges to consider for channel width.")
    @In
    public SimpleFeatureCollection inBridges = null;

    @Description("The input point layer of with the check dams to consider for channel width.")
    @In
    public SimpleFeatureCollection inDams = null;

    @Description("The output points network layer with the bankfull width updated with the information of bridges and dams width.")
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description("The output layer containing the points of the structures containing no information of the width.")
    @Out
    public SimpleFeatureCollection outProblemBridges = null;

    /*
     * meters within a dam is assumed to be on the network
     */
    private double briglieOnNetDistance = 15.0;

    /*
     * meters within a bridge is assumed to be on the network
     */
    private double pontiOnNetDistance = 15.0;

    @Execute
    public void process() throws Exception {

        /*
         * of the input layer with dams consider only the check dams to 
         * contract the channel, check dams have TYPE 230 and 237
         */
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
            envelopeExpanded.expandBy(briglieOnNetDistance);

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
                nearestPoint.setAttribute(WIDTH, 0.1);
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
            double length = (Double) pontiFeature.getAttribute(BRIDGE_LENGTH);
            if (length == 0.0) {
                ((DefaultFeatureCollection) outProblemBridges).add(pontiFeature);
                continue;
            }

            Envelope envelopeInternal = pontiGeometry.getEnvelopeInternal();
            Envelope envelopeExpanded = new Envelope(envelopeInternal);
            envelopeExpanded.expandBy(pontiOnNetDistance);

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
        String inNetPoints = "D:/lavori_tmp/gsoc/netpoints_width.shp";
        String inBridges = "D:/lavori_tmp/gsoc/basedata/W_Brcke_2008.shp";
        String inDams = "D:/lavori_tmp/gsoc/basedata/W_Querwerke_2008.shp";

        String outNetPoints = "D:/lavori_tmp/gsoc/netpoints_width_bridgesdams.shp";
        String outProblemStructure = "D:/lavori_tmp/gsoc/bridgesdams_problems.shp";

        LW05_BridgesDamsWidthAdder bridgesDamsWidthAdder = new LW05_BridgesDamsWidthAdder();
        bridgesDamsWidthAdder.inNetPoints = OmsVectorReader.readVector(inNetPoints);
        bridgesDamsWidthAdder.inBridges = OmsVectorReader.readVector(inBridges);
        bridgesDamsWidthAdder.inDams = OmsVectorReader.readVector(inDams);

        bridgesDamsWidthAdder.process();

        SimpleFeatureCollection outNetPointFC = bridgesDamsWidthAdder.outNetPoints;
        SimpleFeatureCollection outProblemBridgesFC = bridgesDamsWidthAdder.outProblemBridges;

        OmsVectorWriter.writeVector(outNetPoints, outNetPointFC);

        /*
         * writes the output layer with bridges without the length attribute only if there
         * are some in the area of interest
         */
        int size = outProblemBridgesFC.size();
        System.out.println("Ponti a l == 0: " + size);
        if (size > 0) {
            OmsVectorWriter.writeVector(outProblemStructure, outProblemBridgesFC);
        }

    }

}
