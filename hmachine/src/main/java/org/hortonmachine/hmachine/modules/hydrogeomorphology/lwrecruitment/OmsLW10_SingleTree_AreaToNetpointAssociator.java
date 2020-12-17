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

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureExtender;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

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

@Description(OmsLW10_SingleTree_AreaToNetpointAssociator.DESCRIPTION)
@Author(name = OmsLW10_SingleTree_AreaToNetpointAssociator.AUTHORS, contact = OmsLW10_SingleTree_AreaToNetpointAssociator.CONTACTS)
@Keywords(OmsLW10_SingleTree_AreaToNetpointAssociator.KEYWORDS)
@Label(OmsLW10_SingleTree_AreaToNetpointAssociator.LABEL)
@Name("_" + OmsLW10_SingleTree_AreaToNetpointAssociator.NAME)
@Status(OmsLW10_SingleTree_AreaToNetpointAssociator.STATUS)
@License(OmsLW10_SingleTree_AreaToNetpointAssociator.LICENSE)
public class OmsLW10_SingleTree_AreaToNetpointAssociator extends HMModel {

    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description(inTreePoints_DESCR)
    @In
    public SimpleFeatureCollection inTreePoints = null;

    @Description(inInundationArea_DESCR)
    @In
    public SimpleFeatureCollection inInundationArea = null;

    @Description(inFlow_DESCR)
    @In
    public GridCoverage2D inFlow = null;

    @Description(inTca_DESCR)
    @In
    public GridCoverage2D inTca = null;

    @Description(inNet_DESCR)
    @In
    public GridCoverage2D inNet = null;

    @Description(inConnectivity_DESCR)
    @In
    public GridCoverage2D inConnectivity = null;

    @Description(pConnectivityThreshold_DESCR)
    @In
    public double pConnectivityThreshold = 4.0;
    
    @Description(pAllometricCoeff2ndOrder_DESCR)
    @In
    public double pAllometricCoeff2ndOrder = 0.0096;
    
    @Description(pAllometricCoeff1stOrder_DESCR)
    @In
    public double pAllometricCoeff1stOrder = 1.298;
    
    @Description(pAllometricCoeffVolume_DESCR)
    @In
    public double pAllometricCoeffVolume = 0.0000368048;

    @Description(pRepresentingHeightDbhPercentile_DESCR)
    @In
    public int pRepresentingHeightDbhPercentile = 50;
    
    @Description(pTreeTaper_DESCR)
    @Unit("cm/m")
    @In
    public double pTreeTaper = 3.0;
    
    @Description(pFlexibleDiameterLimit_DESCR)
    @Unit("cm")
    @In
    public double pFlexibleDiameterLimit = 5.0;
    
    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description(outNetnum_DESCR)
    @Out
    public GridCoverage2D outNetnum = null;

    @Description(outTreePoints_DESCR)
    @Out
    public SimpleFeatureCollection outTreePoints = null;

    @Description(outBasins_DESCR)
    @Out
    public GridCoverage2D outBasins = null;

    // VARS DOC START
    public static final String outBasins_DESCR = "The output subbasins raster map.";
    public static final String outNetnum_DESCR = "The output netnumbering raster map.";
    public static final String outNetPoints_DESCR = "The output points network layer with the additional attributes vegetation height and timber volume.";
    public static final String outTreePoints_DESCR = "The output tree points layer with additional attribute of the correspondent river section where it will contribute.";
    public static final String pConnectivityThreshold_DESCR = "Threshold on connectivity map for extracting unstable connected pixels of the basins.";
    public static final String pAllometricCoeff2ndOrder_DESCR = "Coefficient of the second order term of tree height of the allometric function relating DBH to H of the trees.";
    public static final String pAllometricCoeff1stOrder_DESCR = "Coefficient of the first order term of tree height of the allometric function relating DBH to H of the trees.";
    public static final String pAllometricCoeffVolume_DESCR = "Coefficient of the first order term of tree height of the allometric function relating tree volume to DBH and H.";
    public static final String pRepresentingHeightDbhPercentile_DESCR = "Percentile of the distribution of tree heights and DBH to be used for the evaluation of the representative height and DBH contributing in each section from the hillslopes.";
    public static final String pTreeTaper_DESCR = "The tree taper to use to evaluate the effective length of the trees (rastremation index)";
    public static final String pFlexibleDiameterLimit_DESCR = "The value of the diameter limit under which the log is flexible used to evaluate the effective length of the trees.";
    public static final String inConnectivity_DESCR = "The input downslope connectivity index raster map.";
    public static final String inNet_DESCR = "The input network raster map.";
    public static final String inTca_DESCR = "The input total contributing areas raster map.";
    public static final String inFlow_DESCR = "The input flow directions raster map.";
    public static final String inInundationArea_DESCR = "The input polygon layer with the inundation areas.";
    public static final String inNetPoints_DESCR = "The input hierarchy point network layer.";
    public static final String inTreePoints_DESCR = "The input layer of single trees.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw10_singletreeareatonetpointassociator";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, bankflull, width, inundation, vegetation";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Calculate median vegetation height and total timber volume of the vegetation on unstable and connected areas of each subbasin.";
    // VARS DOC END

    private static final double HEIGHT_FOR_MEASURING_DBH = 1.3; // m
    
    @Execute
    public void process() throws Exception {

        GridGeometry2D gridGeometry = inFlow.getGridGeometry();
        GeometryFactory gf = GeometryUtilities.gf();

        /*
         * extract the inundated area from the polygon
         */
        PreparedGeometry preparedFloodingArea = getFloodingArea(inInundationArea);

        List<FeatureMate> treesList = FeatureUtilities.featureCollectionToMatesList(inTreePoints);

        /*
         * extract basins calling netnumbering with in input all the network points
         */
        OmsNetNumbering omsnetnumbering = new OmsNetNumbering();
        omsnetnumbering.inFlow = inFlow;
        omsnetnumbering.inNet = inNet;
        omsnetnumbering.inTca = inTca;
        omsnetnumbering.inPoints = inNetPoints;
        omsnetnumbering.pm = pm;
        omsnetnumbering.process();
        outNetnum = omsnetnumbering.outNetnum;
        outBasins = omsnetnumbering.outBasins;

        RandomIter netnumBasinsIter = CoverageUtilities.getRandomIterator(outBasins);
        RandomIter connectivityIter = CoverageUtilities.getRandomIterator(inConnectivity);

        HashMap<Integer, DescriptiveStatistics> heightBasin2ValueMap = new HashMap<Integer, DescriptiveStatistics>();
        HashMap<Integer, DescriptiveStatistics> dbhBasin2ValueMap = new HashMap<Integer, DescriptiveStatistics>();
        HashMap<Integer, DescriptiveStatistics> standBasin2ValueMap = new HashMap<Integer, DescriptiveStatistics>();

        FeatureExtender treesExtender = new FeatureExtender(inTreePoints.getSchema(), new String[]{LWFields.LINKID},
                new Class[]{Integer.class});

        List<SimpleFeature> treePointsList = new ArrayList<>();

        pm.beginTask("Calculating vegetation stats.", treesList.size());
        for( FeatureMate treeFeature : treesList ) {
            Coordinate treeCoordinate = treeFeature.getGeometry().getCoordinate();
            Point treePoint = gf.createPoint(treeCoordinate);
            double treeHeight = treeFeature.getAttribute(LWFields.FIELD_ELEV, Double.class);

            int[] colRow = CoverageUtilities.colRowFromCoordinate(treeCoordinate, gridGeometry, null);
            int c = colRow[0];
            int r = colRow[1];

            double netnumDouble = netnumBasinsIter.getSampleDouble(c, r, 0);
            if (!isNovalue(netnumDouble)) {
                Integer netNum = (int) netnumDouble;
                double connectivityDouble = connectivityIter.getSampleDouble(c, r, 0);
                /*
                 * check if the point is connected to the network:
                 * - connectivity index less than the threshold
                 * - point is inside the inundated area
                 * and fill the hashmaps with the correspondent positions.
                 */
                if (connectivityDouble < pConnectivityThreshold || preparedFloodingArea.intersects(treePoint)) {
                    double[] volume = calculateVolume(treeHeight);
                    double dbhDouble = volume[0];
                    double standDouble = volume[1];
                    
                    //TODO: here use the taper to evaluate the effective length
                    double lengthLimitBeforeFlexibility = (dbhDouble - pFlexibleDiameterLimit) / pTreeTaper;
                    double usefulHeightForPropagationDownstream = lengthLimitBeforeFlexibility + HEIGHT_FOR_MEASURING_DBH;
                    if (treeHeight >= usefulHeightForPropagationDownstream) {
                        treeHeight = usefulHeightForPropagationDownstream;
                    }    
                    DescriptiveStatistics summaryHeightStatistics = heightBasin2ValueMap.get(netNum);
                    DescriptiveStatistics summaryDbhStatistics = dbhBasin2ValueMap.get(netNum);
                    DescriptiveStatistics summaryStandStatistics = standBasin2ValueMap.get(netNum);
                    if (summaryHeightStatistics == null) {
                        summaryHeightStatistics = new DescriptiveStatistics();
                        summaryDbhStatistics = new DescriptiveStatistics();
                        summaryStandStatistics = new DescriptiveStatistics();
                        heightBasin2ValueMap.put(netNum, summaryHeightStatistics);
                        dbhBasin2ValueMap.put(netNum, summaryDbhStatistics);
                        standBasin2ValueMap.put(netNum, summaryStandStatistics);
                    }
                    summaryHeightStatistics.addValue(treeHeight);
                    summaryDbhStatistics.addValue(dbhDouble);
                    summaryStandStatistics.addValue(standDouble);

                    // for now we put the basin netnum as id, later we substitute it with the linkid
                    // (here it is not known yet)
                    SimpleFeature newTreeFeature = treesExtender.extendFeature(treeFeature.getFeature(), new Object[]{netNum});
                    treePointsList.add(newTreeFeature);
                }
            }
            pm.worked(1);
        }
        pm.done();

        /*
         * create the structure for the output attributes and insert the summary statistics
         * as attributes
         */
        FeatureExtender netPointsExtender = new FeatureExtender(inNetPoints.getSchema(),
                new String[]{LWFields.VEG_VOL, LWFields.VEG_H, LWFields.VEG_DBH}, new Class[]{Double.class, Double.class, Double.class});
        List<SimpleFeature> inNetworkPointsList = FeatureUtilities.featureCollectionToList(inNetPoints);
        DefaultFeatureCollection finalNetworkPointsFC = new DefaultFeatureCollection();
        final java.awt.Point point = new java.awt.Point();
        HashMap<Integer, Integer> netnum2LinkidMap = new HashMap<>();
        for( SimpleFeature inPointFeature : inNetworkPointsList ) {
            Integer id = (Integer) inPointFeature.getAttribute(LWFields.LINKID);

            Geometry geometry = (Geometry) inPointFeature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();
            CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, point);
            int netnum = netnumBasinsIter.getSample(point.x, point.y, 0);

            netnum2LinkidMap.put(netnum, id);

            DescriptiveStatistics summaryHeightStatistics = heightBasin2ValueMap.get(netnum);
            double medianHeight = 0.0;
            if (summaryHeightStatistics != null) {
                medianHeight = summaryHeightStatistics.getPercentile(pRepresentingHeightDbhPercentile);
            }
            
            DescriptiveStatistics summaryDbhStatistics = dbhBasin2ValueMap.get(netnum);
            double medianDbh = 0.0;
            if (summaryDbhStatistics != null) {
                medianDbh = summaryDbhStatistics.getPercentile(pRepresentingHeightDbhPercentile);
            }

            DescriptiveStatistics summaryStandStatistics = standBasin2ValueMap.get(netnum);
            double sumStand = 0.0;
            if (summaryStandStatistics != null) {
                sumStand = summaryStandStatistics.getSum();
            }

            SimpleFeature newPointFeature = netPointsExtender.extendFeature(inPointFeature, new Object[]{sumStand, medianHeight, medianDbh});
            finalNetworkPointsFC.add(newPointFeature);
        }
        outNetPoints = finalNetworkPointsFC;

        outTreePoints = new DefaultFeatureCollection();
        for( SimpleFeature treePointFeature : treePointsList ) {
            Integer netnum = (Integer) treePointFeature.getAttribute(LWFields.LINKID);
            Integer linkid = netnum2LinkidMap.get(netnum);
            treePointFeature.setAttribute(LWFields.LINKID, linkid);
            ((DefaultFeatureCollection) outTreePoints).add(treePointFeature);
        }

    }

    private double [] calculateVolume( double treeHeight ) {
        /*
        The volume of the trees is calculated considering two allometric functions:
        1. the relation between the height of the trees and the diameter
        2. the relation between the height and diameter and the volume of the trees
        */
        double treeDbh = pAllometricCoeff2ndOrder * Math.pow(treeHeight, 2) + pAllometricCoeff1stOrder * treeHeight;
        double treeVolume = Math.PI * Math.pow(treeDbh, 2) / 4 * treeHeight * pAllometricCoeffVolume;
        double [] treeParams = new double[] {treeDbh, treeVolume};
        return treeParams;
    }

    /*
    * extract the inundated area from the polygon
    */
    private PreparedGeometry getFloodingArea( SimpleFeatureCollection inFloodingAreas ) throws Exception {
        List<Geometry> geometriesList = FeatureUtilities.featureCollectionToGeometriesList(inFloodingAreas, true, null);
        Geometry polygonUnion = CascadedPolygonUnion.union(geometriesList);
        PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(polygonUnion);
        return preparedGeometry;
    }

    public static void main( String[] args ) throws Exception {

        String base = "D:/lavori_tmp/unibz/2016_06_gsoc/data01/";
        String base2 = "D:/lavori_tmp/unibz/2016_06_gsoc/raster/";

        OmsLW10_SingleTree_AreaToNetpointAssociator ex = new OmsLW10_SingleTree_AreaToNetpointAssociator();
        ex.inNetPoints = OmsVectorReader.readVector(base + "net_point_width_damsbridg_slope_lateral_inund.shp");
        ex.inTreePoints = OmsVectorReader.readVector(base + "T1_ps_plot.shp");
        ex.inInundationArea = OmsVectorReader.readVector(base + "inund_area2.shp");
        ex.inFlow = OmsRasterReader.readRaster(base2 + "basin_drain.asc");
        ex.inNet = OmsRasterReader.readRaster(base2 + "net_7000.asc");
        ex.inConnectivity = OmsRasterReader.readRaster(base2 + "connectivity.asc");
        ex.pConnectivityThreshold = 45.0;
        ex.pRepresentingHeightDbhPercentile = 80;

        ex.process();
        SimpleFeatureCollection outNetPoints = ex.outNetPoints;
        SimpleFeatureCollection outTreePoints = ex.outTreePoints;
        GridCoverage2D outNetNum = ex.outNetnum;
        GridCoverage2D outBasins = ex.outBasins;
        
        OmsVectorWriter.writeVector(base + "net_point_width_damsbridg_slope_lateral_inund_veg_80_rast.shp", outNetPoints);
        OmsVectorWriter.writeVector(base + "tree_points_80_rast.shp", outTreePoints);
        OmsRasterWriter.writeRaster(base + "netnum_80.asc", outNetNum);
        OmsRasterWriter.writeRaster(base + "basins_80.asc", outBasins);

    }
    
}
