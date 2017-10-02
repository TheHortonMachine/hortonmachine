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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureExtender;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.hortonmachine.hmachine.modules.network.networkattributes.NetworkChannel;
import org.opengis.feature.simple.SimpleFeature;

@Description(OmsLW11_NetworkPropagator.DESCRIPTION)
@Author(name = OmsLW11_NetworkPropagator.AUTHORS, contact = OmsLW11_NetworkPropagator.CONTACTS)
@Label(OmsLW11_NetworkPropagator.LABEL)
@Keywords(OmsLW11_NetworkPropagator.KEYWORDS)
@Name("_" + OmsLW11_NetworkPropagator.NAME)
@Status(OmsLW11_NetworkPropagator.STATUS)
@License(OmsLW11_NetworkPropagator.LICENSE)
public class OmsLW11_NetworkPropagator extends HMModel implements LWFields {

    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description(ratioLogsLengthChannelWidthHillslope_DESCR)
    @In
    public double pRatioLogsLengthChannelWidthHillslope = 0.8;

    @Description(ratioLogsLengthChannelWidthChannel_DESCR)
    @In
    public double pRatioLogsLengthChannelWidthChannel = 1.0;

    @Description(ratioLogsDiameterWaterDepth_DESCR)
    @In
    public double pRatioLogsDiameterWaterDepth = 0.8;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    // VARS DOC START
    public static final String outNetPoints_DESCR = "The output network points layer with the critical sections labelled in the attribute table.";
    public static final String inNetPoints_DESCR = "The input network points layer with the additional attributes vegetation height and timber volume.";
    public static final String ratioLogsLengthChannelWidthHillslope_DESCR = "The ratio between the lenght of the logs and the maximum channel width for the vegetation coming from the hillslopes (vegetation characteristics of the current section).";
    public static final String ratioLogsLengthChannelWidthChannel_DESCR = "The ratio between the lenght of the logs and the maximum channel width for the vegetation coming from upstream (vegetation characteristics from upstream not blocked).";
    public static final String ratioLogsDiameterWaterDepth_DESCR = "The ratio between the diameter of the logs and the water depth corresponding to maximum channel widht.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw11_networkpropagator";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "critical, wood";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Label the critical section for the transit of the wood and calculate the cumulated volume of biomass in each blocking section.";
    // VARS DOC END

    /*
     * specify the names of the attributes fields
     */
    private final String FIELD_LINKID = LINKID;
    private final String FIELD_DBH = VEG_DBH;

    @Execute
    public void process() throws Exception {

        /*
         * store the network points in a collection and map the Pfafstetter codes together with
         * the features ID in an  hashmap.
         */
        List<SimpleFeature> networkFeatures = FeatureUtilities.featureCollectionToList(inNetPoints);
        List<PfafstetterNumber> pfafstetterNumberList = new ArrayList<PfafstetterNumber>();
        HashMap<String, TreeMap<Integer, SimpleFeature>> pfafstetterNumber2FeaturesMap = new HashMap<String, TreeMap<Integer, SimpleFeature>>();

        // TODO add the check if the DBH field is available or not for the check on diameter!!
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
        FeatureExtender ext = null;
          DefaultFeatureCollection outputFC = new DefaultFeatureCollection();
        /*
         * consider each link and navigate downstream each
         */
        double maxUpstreamHeight = -1;
        double maxUpstreamDiameter = -1;

        // create the variables to use in the cycle
        List<PfafstetterNumber> lastUpStreamPfafstetters = new ArrayList<PfafstetterNumber>();
        List<Double> lastUpStreamMaxHeights = new ArrayList<Double>();
        List<Double> lastUpStreamMaxDiameters = new ArrayList<Double>();
        List<String> lastUpStreamCriticSourceForLength = new ArrayList<String>();
        List<String> lastUpStreamCriticSourceForDiameter = new ArrayList<String>();
        /*
         * start the main cycle with the elaborations to identify the critical sections
         */
        pm.beginTask("Processing network...", pfafstetterNumberList.size());
        for( PfafstetterNumber pfafstetterNumber : pfafstetterNumberList ) {
            TreeMap<Integer, SimpleFeature> featuresMap = pfafstetterNumber2FeaturesMap.get(pfafstetterNumber.toString());

            String criticSourceForHeigth = null;
            String criticSourceForDiameter = null;
            for( int i = 0; i < lastUpStreamPfafstetters.size(); i++ ) {
                PfafstetterNumber lastUpStreamPfafstetter = lastUpStreamPfafstetters.get(i);
                if (pfafstetterNumber.isDownStreamOf(lastUpStreamPfafstetter)) {
                    /*
                     * if the other is directly upstream, check its max height and label 
                     * the critical section
                     */
                    double lastUpstreamHeight = lastUpStreamMaxHeights.get(i);
                    double lastUpstreamDiameter = lastUpStreamMaxDiameters.get(i);
                    if (lastUpstreamHeight > maxUpstreamHeight) {
                        maxUpstreamHeight = lastUpstreamHeight;
                        criticSourceForHeigth = lastUpStreamCriticSourceForLength.get(i);
                    }
                    if (lastUpstreamDiameter > maxUpstreamDiameter) {
                        maxUpstreamDiameter = lastUpstreamDiameter;
                        criticSourceForDiameter = lastUpStreamCriticSourceForDiameter.get(i);
                    }
                }
            }

            for( SimpleFeature feature : featuresMap.values() ) {
                String linkid = feature.getAttribute(FIELD_LINKID).toString();
                double width = (Double) feature.getAttribute(WIDTH2);
                double height = (Double) feature.getAttribute(VEG_H);
                Object diameterObj = feature.getAttribute(VEG_DBH);
                double diameter;
                if (diameterObj instanceof Double) {
                    diameter = (Double) diameterObj;
                } else {
                    diameter = -1;
                }
                Object waterDepthObj = feature.getAttribute(FIELD_WATER_LEVEL2);
                double waterDepth;
                if (waterDepthObj instanceof Double) {
                    waterDepth = (Double) waterDepthObj;
                } else {
                    waterDepth = -1;
                }
                
                if (ext ==null) {
                    if (diameter<0) {
                        ext = new FeatureExtender(inNetPoints.getSchema(),
                                new String[]{FIELD_ISCRITIC_LOCAL_FOR_HEIGHT, FIELD_ISCRITIC_GLOBAL_FOR_HEIGHT, FIELD_CRITIC_SOURCE_FOR_HEIGHT},
                                new Class[]{Integer.class, Integer.class, String.class});
                    }else{
                        ext = new FeatureExtender(inNetPoints.getSchema(),
                                new String[]{FIELD_ISCRITIC_LOCAL_FOR_HEIGHT, FIELD_ISCRITIC_GLOBAL_FOR_HEIGHT, FIELD_CRITIC_SOURCE_FOR_HEIGHT,
                                        FIELD_ISCRITIC_LOCAL_FOR_DIAMETER, FIELD_ISCRITIC_GLOBAL_FOR_DIAMETER, FIELD_CRITIC_SOURCE_FOR_DIAMETER},
                                new Class[]{Integer.class, Integer.class, String.class, Integer.class, Integer.class, String.class});
                    
                    }
                }

                if (height > maxUpstreamHeight) {
                    maxUpstreamHeight = height;
                    criticSourceForHeigth = pfafstetterNumber + "-" + linkid;
                }

                /*
                 * label the critical sections
                 */
                // critical from local parameters veg_h > width
                int isCriticLocalForLogHeight = 0;
                int isCriticGlobalForLogHeight = 0;

                if (height / width > pRatioLogsLengthChannelWidthHillslope) {
                    isCriticLocalForLogHeight = 1;
                }

                // critical on vegetation coming from upstream
                if (maxUpstreamHeight / width > pRatioLogsLengthChannelWidthChannel) {
                    isCriticGlobalForLogHeight = 1;
                    maxUpstreamHeight = -1;
                }

                // update the field with the origin of critical sections
                if (criticSourceForHeigth == null)
                    criticSourceForHeigth = "";
                String tmpCriticSourceForHeight = criticSourceForHeigth;
                if (isCriticGlobalForLogHeight == 0) {
                    tmpCriticSourceForHeight = "";
                }

                /*
                 * check on the ratio between the diameter of the logs and the channel depth will be
                 * done only if the dbh field in the input net points layer is available
                 */
                if (diameter > 0.0) {
                    if (diameter > maxUpstreamDiameter) {
                        maxUpstreamDiameter = diameter;
                        criticSourceForDiameter = pfafstetterNumber + "-" + linkid;
                    }

                    int isCriticLocalForLogDiameter = 0;
                    int isCriticGlobalForLogDiameter = 0;

                    // critical from local parameters veg_d > waterdepth
                    if (diameter / waterDepth > pRatioLogsDiameterWaterDepth) {
                        isCriticLocalForLogDiameter = 1;
                    }
                    // critical on vegetation coming from upstream
                    if (maxUpstreamDiameter / waterDepth > pRatioLogsDiameterWaterDepth) {
                        isCriticGlobalForLogDiameter = 1;
                        maxUpstreamDiameter = -1;
                    }

                    if (criticSourceForDiameter == null)
                        criticSourceForDiameter = "";
                    String tmpCriticSourceForDiameter = criticSourceForDiameter;
                    if (isCriticGlobalForLogDiameter == 0) {
                        tmpCriticSourceForDiameter = "";
                    }

                    SimpleFeature newFeature = ext
                            .extendFeature(feature,
                                    new Object[]{isCriticLocalForLogHeight, isCriticGlobalForLogHeight, tmpCriticSourceForHeight,
                                            isCriticLocalForLogDiameter, isCriticGlobalForLogDiameter,
                                            tmpCriticSourceForDiameter});
                    outputFC.add(newFeature);

                    lastUpStreamMaxDiameters.add(maxUpstreamDiameter);
                    lastUpStreamCriticSourceForDiameter.add(criticSourceForDiameter);

                } else {
                    // no other checks will be done for critical sections
                    SimpleFeature newFeature = ext.extendFeature(feature,
                            new Object[]{isCriticLocalForLogHeight, isCriticGlobalForLogHeight, tmpCriticSourceForHeight});
                    outputFC.add(newFeature);
                }

            }
            // add the point to the list for the next step
            lastUpStreamPfafstetters.add(pfafstetterNumber);
            lastUpStreamMaxHeights.add(maxUpstreamHeight);
            lastUpStreamCriticSourceForLength.add(criticSourceForHeigth);

            pm.worked(1);
        }
        pm.done();

        outNetPoints = outputFC;
    }

    public static void main( String[] args ) throws Exception {

        String base = "D:/lavori_tmp/unibz/2016_06_gsoc/data01/";

        OmsLW11_NetworkPropagator ex = new OmsLW11_NetworkPropagator();
        ex.inNetPoints = OmsVectorReader
                .readVector(base + "net_point_width_damsbridg_slope_lateral_inund_veg_80_rast_lateral3_nodbh.shp");
        ex.pRatioLogsDiameterWaterDepth = 0.7;
        ex.pRatioLogsLengthChannelWidthChannel = 0.8;
        ex.pRatioLogsLengthChannelWidthHillslope = 0.9;

        ex.process();
        SimpleFeatureCollection outNetPoints = ex.outNetPoints;

        OmsVectorWriter.writeVector(base + "net_point_width_damsbridg_slope_lateral_inund_veg_80_rast_lateral3_crit_nodbh.shp",
                outNetPoints);

    }

}
