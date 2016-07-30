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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.hortonmachine.modules.network.PfafstetterNumber;
import org.jgrasstools.hortonmachine.modules.network.networkattributes.NetworkChannel;
import org.opengis.feature.simple.SimpleFeature;

@Description(OmsLW11_NetworkPropagator.DESCRIPTION)
@Author(name = OmsLW11_NetworkPropagator.AUTHORS, contact = OmsLW11_NetworkPropagator.CONTACTS)
@Label(OmsLW11_NetworkPropagator.LABEL)
@Keywords(OmsLW11_NetworkPropagator.KEYWORDS)
@Name("_" + OmsLW11_NetworkPropagator.NAME)
@Status(OmsLW11_NetworkPropagator.STATUS)
@License(OmsLW11_NetworkPropagator.LICENSE)
public class OmsLW11_NetworkPropagator extends JGTModel implements LWFields {

    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description("ratioLogsLengthChannelWidthHillslope")
    @In
    public double ratioLogsLengthChannelWidthHillslope = 0.8;

    @Description("ratioLogsLengthChannelWidthChannel")
    @In
    public double ratioLogsLengthChannelWidthChannel = 1.0;

    @Description("ratioLogsDiameterWaterDepth")
    @In
    public double ratioLogsDiameterWaterDepth = 0.8;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    // VARS DOC START
    public static final String outNetPoints_DESCR = "The output network points layer with the critical sections labelled in the attribute table.";
    public static final String inNetPoints_DESCR = "The input network points layer with the additional attributes vegetation height and timber volume.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw10_networkpropagator";
    public static final String LABEL = JGTConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "critical, wood";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Label the critical section for the transit of the wood and calculate the cumulated volume of biomass in each blocking section.";
    // VARS DOC END

    /*
     * specify the names of the attributes fields
     */
    private final String FIELD_LINKID = LINKID;

    @Execute
    public void process() throws Exception {

        /*
         * store the network points in a collection and map the Pfafstetter codes together with
         * the features ID in an  hashmap.
         */
        List<SimpleFeature> networkFeatures = FeatureUtilities.featureCollectionToList(inNetPoints);
        List<PfafstetterNumber> pfafstetterNumberList = new ArrayList<PfafstetterNumber>();
        HashMap<String, TreeMap<Integer, SimpleFeature>> pfafstetterNumber2FeaturesMap = new HashMap<String, TreeMap<Integer, SimpleFeature>>();

        for( SimpleFeature networkFeature : networkFeatures ) {
            Object pfaffObject = networkFeature.getAttribute(NetworkChannel.PFAFNAME);
            if (pfaffObject instanceof String) {
                String pfaffString = (String) pfaffObject;
                PfafstetterNumber pfaf = new PfafstetterNumber(pfaffString);
                if (!pfafstetterNumberList.contains(pfaf)) {
                    pfafstetterNumberList.add(pfaf);
                }
                TreeMap<Integer, SimpleFeature> featureTreeMap = pfafstetterNumber2FeaturesMap.get(pfaffString);
                if (featureTreeMap == null) {
                    featureTreeMap = new TreeMap<Integer, SimpleFeature>();
                    pfafstetterNumber2FeaturesMap.put(pfaffString, featureTreeMap);
                }
                Object linkidObj = networkFeature.getAttribute(FIELD_LINKID);
                if (linkidObj instanceof Integer) {
                    Integer linkId = (Integer) linkidObj;
                    featureTreeMap.put(linkId, networkFeature);
                }
            }
        }

        // sort the list of Pfafstetter to be ready to navigate the network
        Collections.sort(pfafstetterNumberList);

        /*
         * prepare the output feature collection as an extention of the input with 3 
         * additional attributes for critical sections
         */
        FeatureExtender ext = new FeatureExtender(inNetPoints.getSchema(),
                new String[]{FIELD_ISCRITIC_LOCAL, FIELD_ISCRITIC_GLOBAL, FIELD_CRITIC_SOURCE},
                new Class[]{Integer.class, Integer.class, String.class});
        DefaultFeatureCollection outputFC = new DefaultFeatureCollection();
        /*
         * consider each link and navigate downstream each
         */
        double maxUpstreamHeight = -1;

        // create the variables to use in the cycle
        List<PfafstetterNumber> lastUpStreamPfafstetters = new ArrayList<PfafstetterNumber>();
        List<Double> lastUpStreamMaxHeights = new ArrayList<Double>();
        List<String> lastUpStreamCriticSource = new ArrayList<String>();
        /*
         * start the main cycle with the elaborations to identify the critical sections
         */
        pm.beginTask("Processing network...", pfafstetterNumberList.size());
        for( PfafstetterNumber pfafstetterNumber : pfafstetterNumberList ) {
            TreeMap<Integer, SimpleFeature> featuresMap = pfafstetterNumber2FeaturesMap.get(pfafstetterNumber.toString());

            String criticSource = null;
            for( int i = 0; i < lastUpStreamPfafstetters.size(); i++ ) {
                PfafstetterNumber lastUpStreamPfafstetter = lastUpStreamPfafstetters.get(i);
                if (pfafstetterNumber.isDownStreamOf(lastUpStreamPfafstetter)) {
                    /*
                     * if the other is directly upstream, check its max height and label 
                     * the critical section
                     */
                    double lastUpstreamHeight = lastUpStreamMaxHeights.get(i);
                    if (lastUpstreamHeight > maxUpstreamHeight) {
                        maxUpstreamHeight = lastUpstreamHeight;
                        criticSource = lastUpStreamCriticSource.get(i);
                    }
                }
            }

            for( SimpleFeature feature : featuresMap.values() ) {
                String linkid = feature.getAttribute(FIELD_LINKID).toString();
                double width = (Double) feature.getAttribute(WIDTH2);
                double height = (Double) feature.getAttribute(VEG_H);
                double diameter = (Double) feature.getAttribute(VEG_DBH);
                double waterDepth = (Double) feature.getAttribute(FIELD_WATER_LEVEL2);

                if (height > maxUpstreamHeight) {
                    maxUpstreamHeight = height;
                    criticSource = pfafstetterNumber + "-" + linkid;
                }

                /*
                 * label the critical sections
                 */
                // critical from local parameters veg_h > width
                int isCriticLocal = 0;
                int isCriticGlobal = 0;
                if (height / width > ratioLogsLengthChannelWidthHillslope) {
                    isCriticLocal = 1;
                }
                // critical on vegetation coming from upstream
                if (maxUpstreamHeight / width > ratioLogsLengthChannelWidthChannel) {
                    isCriticGlobal = 1;
                    maxUpstreamHeight = -1;
                }
                

                // update the field with the origin of critical sections
                if (criticSource == null)
                    criticSource = "";
                String tmpCriticSource = criticSource;
                if (isCriticGlobal == 0) {
                    tmpCriticSource = "";
                }
                SimpleFeature newFeature = ext.extendFeature(feature,
                        new Object[]{isCriticLocal, isCriticGlobal, tmpCriticSource});
                outputFC.add(newFeature);
            }
            // add the point to the list for the next step
            lastUpStreamPfafstetters.add(pfafstetterNumber);
            lastUpStreamMaxHeights.add(maxUpstreamHeight);
            lastUpStreamCriticSource.add(criticSource);

            pm.worked(1);
        }
        pm.done();

        outNetPoints = outputFC;
    }

}
