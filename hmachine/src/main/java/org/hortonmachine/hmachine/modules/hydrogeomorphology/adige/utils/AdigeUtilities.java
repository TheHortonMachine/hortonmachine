/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.utils;

import static org.hortonmachine.hmachine.modules.network.networkattributes.NetworkChannel.NETNUMNAME;
import static org.hortonmachine.hmachine.modules.network.networkattributes.NetworkChannel.PFAFNAME;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.HillSlope;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AdigeUtilities {

    /**
     * Generates {@link HillSlope}s from the informations gathered in the provided feature layers.
     * 
     * @param netFeatureCollection the network features
     * @param hillslopeFeatureCollection the hillslope features
     * @param out a printstream for logging
     * @return the list of ordered hillslopes, starting from the most downstream one
     * @throws Exception
     */
    public static List<IHillSlope> generateHillSlopes( FeatureCollection<SimpleFeatureType, SimpleFeature> netFeatureCollection,
            FeatureCollection<SimpleFeatureType, SimpleFeature> hillslopeFeatureCollection, IHMProgressMonitor out )
            throws Exception {

        out.message("Analizing the network layer...");
        List<SimpleFeature> netFeaturesList = new ArrayList<SimpleFeature>();
        List<Integer> netIdsList = new ArrayList<Integer>();
        ArrayList<PfafstetterNumber> netPfaffsList = new ArrayList<PfafstetterNumber>();
        FeatureIterator<SimpleFeature> netFeatureIterator = netFeatureCollection.features();
        PfafstetterNumber mostDownStreamPNumber = null;
        SimpleFeature mostDownStreamNetFeature = null;
        Integer mostDownStreamLinkId = -1;
        while( netFeatureIterator.hasNext() ) {
            SimpleFeature netFeature = (SimpleFeature) netFeatureIterator.next();
            String attribute = (String) netFeature.getAttribute(PFAFNAME);
            PfafstetterNumber current = new PfafstetterNumber(attribute);
            int tmpId = ((Number) netFeature.getAttribute(NETNUMNAME)).intValue();
            if (mostDownStreamPNumber == null) {
                mostDownStreamPNumber = current;
            } else {
                if (current.isDownStreamOf(mostDownStreamPNumber)) {
                    mostDownStreamLinkId = tmpId;
                    mostDownStreamNetFeature = netFeature;
                    mostDownStreamPNumber = current;
                }
            }
            netFeaturesList.add(netFeature);
            netIdsList.add(tmpId);
            netPfaffsList.add(current);
        }
        netFeatureIterator.close();

        /*
         * search subbasins
         */
        out.message("Analyzing the hillslopes layer...");

        List<SimpleFeature> hillslopeFeaturesList = new ArrayList<SimpleFeature>();
        List<Integer> hillslopeIdsList = new ArrayList<Integer>();
        FeatureIterator<SimpleFeature> hillslopeIterator = hillslopeFeatureCollection.features();
        SimpleFeature mostDownstreamHillslopeFeature = null;
        while( hillslopeIterator.hasNext() ) {
            SimpleFeature f = hillslopeIterator.next();
            int linkAttribute = ((Number) f.getAttribute(NETNUMNAME)).intValue();
            if (mostDownStreamLinkId == linkAttribute) {
                mostDownstreamHillslopeFeature = f;
            }
            hillslopeIdsList.add(linkAttribute);
            hillslopeFeaturesList.add(f);
        }
        /*
         * create all the hillslopes and connect them with their net feature and other hillslopes
         */
        out.message("Linking together network and hillslopes layers...");
        ArrayList<IHillSlope> hillslopeElements = new ArrayList<IHillSlope>();
        IHillSlope mostDownstreamHillslope = null;
        if (mostDownStreamPNumber.isEndPiece()) {
            Integer basinId = hillslopeIdsList.get(0);
            IHillSlope tmpHslp = new HillSlope(mostDownStreamNetFeature, mostDownstreamHillslopeFeature, mostDownStreamPNumber,
                    basinId.intValue());
            hillslopeElements.add(tmpHslp);
            mostDownstreamHillslope = tmpHslp;
        } else {
            /*
             * almost there, now get from the basins list the ones with that netNums
             */
            ArrayList<SimpleFeature> selectedNetFeatureList = new ArrayList<SimpleFeature>();
            ArrayList<Integer> selectedNetId = new ArrayList<Integer>();
            for( int i = 0; i < hillslopeFeaturesList.size(); i++ ) {
                SimpleFeature basinFeature = hillslopeFeaturesList.get(i);
                Integer link = hillslopeIdsList.get(i);
                for( int j = 0; j < netFeaturesList.size(); j++ ) {
                    Integer netNum = netIdsList.get(j);
                    if (netNum.equals(link)) {
                        SimpleFeature netFeature = netFeaturesList.get(j);
                        IHillSlope tmpHslp = new HillSlope(netFeature, basinFeature, netPfaffsList.get(j), netNum.intValue());
                        hillslopeElements.add(tmpHslp);
                        selectedNetFeatureList.add(netFeature);
                        selectedNetId.add(netNum);
                        break;
                    }
                }
            }

            mostDownStreamPNumber = null;
            Integer mostDownStreamNetId = null;
            for( SimpleFeature feature : selectedNetFeatureList ) {
                String attribute = (String) feature.getAttribute(PFAFNAME);
                PfafstetterNumber current = new PfafstetterNumber(attribute);
                Integer tmpId = ((Number) feature.getAttribute(NETNUMNAME)).intValue();
                if (mostDownStreamPNumber == null) {
                    mostDownStreamPNumber = current;
                } else {
                    if (current.isDownStreamOf(mostDownStreamPNumber)) {
                        mostDownStreamNetId = tmpId;
                        mostDownStreamPNumber = current;
                    }
                }
            }

            for( int i = 0; i < hillslopeElements.size(); i++ ) {
                Integer hId = hillslopeIdsList.get(i);
                if (hId.equals(mostDownStreamNetId)) {
                    mostDownstreamHillslope = hillslopeElements.get(i);
                    break;
                }
            }
            if (hillslopeElements.size() == 1) {
                mostDownstreamHillslope = hillslopeElements.get(0);
            }

        }

        if (mostDownstreamHillslope == null)
            throw new RuntimeException();
        HillSlope.connectElements(hillslopeElements);

        List<IHillSlope> orderedHillslopes = new ArrayList<IHillSlope>();
        mostDownstreamHillslope.getAllUpstreamElements(orderedHillslopes, null);

        return orderedHillslopes;

    }

    /**
     * Method to do the routing of a  discharge along the  link of {@link IHillSlope}.
     * 
     * @param discharge the discharge to be transported.
     * @param hillslope the current hillslope. 
     * @param routingType the routing type to use:
     *                  <ul>
     *                      <li>2 = No Chezi explicitly</li>
     *                      <li>3 = Chezi explicitly</li>
     *                      <li>4 = Manning equation</li>
     *                  </ul>
     * @return the routing cuencas coefficient.
     */
    public static double doRouting( double discharge, IHillSlope hillslope, int routingType ) {
        double linkWidth = hillslope.getLinkWidth(8.66, 0.6, 0.0);
        double linkLength = hillslope.getLinkLength();
        double linkSlope = hillslope.getLinkSlope();
        double chezLawExpon = -1. / 3.;
        double chezLawCoeff = 200. / Math.pow(0.000357911, chezLawExpon);
        double linkChezy = hillslope.getLinkChezi(chezLawCoeff, chezLawExpon);

        double K_Q = 0;

        /* ROUTING RATE (K_Q) and CHANNEL VELOCITY (vc) */
        // System.out.println(routingtype);
        switch( routingType ) {
        case 2: /* No Chezi explicitly */
            K_Q = 8.796 * Math.pow(discharge, 1 / 3.) * Math.pow(linkWidth, -1 / 3.) * Math.pow(linkLength, -1)
                    * Math.pow(linkSlope, 2 / 9.); // units
            // 1/s*/
            break;

        case 3: /* Chezi explicit */
            // System.out.println("Chezy");
            K_Q = 3 / 2. * Math.pow(discharge, 1. / 3.) * Math.pow(linkChezy, 2. / 3.) * Math.pow(linkWidth, -1. / 3.)
                    * Math.pow(linkLength, -1) * Math.pow(linkSlope, 1. / 3.); // units 1/s
            break;

        case 4: /* Mannings equation */
            double flowdepth = (1. / 3.) * Math.pow(discharge, 1. / 3.); // depth
            // m,
            // input m^3/s;
            // general
            // observed
            // relation for
            // gc from
            // molnar and
            // ramirez 1998
            double hydrad = (flowdepth * linkWidth) / (2.f * flowdepth + linkWidth); // m
            double mannings_n = 1; // 0.030f; // mannings n suggested by Jason via his
            // observations at
            // Whitewater for high flows. Low flows will have higher
            // n ... up to 2x more.
            K_Q = (Math.pow(hydrad, 2. / 3.) * Math.pow(linkSlope, 1 / 2.) / mannings_n) // m/s
                    // ;
                    // this
                    // term
                    // is v
                    // from
                    // mannings
                    // eqn
                    * Math.pow(linkLength, -1); // 1/s
            break;

        }

        return K_Q;
    }

}
