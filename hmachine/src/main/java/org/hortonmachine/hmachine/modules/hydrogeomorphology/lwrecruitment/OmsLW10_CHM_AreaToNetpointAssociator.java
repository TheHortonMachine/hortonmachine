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

import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureExtender;
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

@Description(OmsLW10_CHM_AreaToNetpointAssociator.DESCRIPTION)
@Author(name = OmsLW10_CHM_AreaToNetpointAssociator.AUTHORS, contact = OmsLW10_CHM_AreaToNetpointAssociator.CONTACTS)
@Keywords(OmsLW10_CHM_AreaToNetpointAssociator.KEYWORDS)
@Label(OmsLW10_CHM_AreaToNetpointAssociator.LABEL)
@Name("_" + OmsLW10_CHM_AreaToNetpointAssociator.NAME)
@Status(OmsLW10_CHM_AreaToNetpointAssociator.STATUS)
@License(OmsLW10_CHM_AreaToNetpointAssociator.LICENSE)
public class OmsLW10_CHM_AreaToNetpointAssociator extends HMModel {

    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

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

    @Description(inDtm_DESCR)
    @In
    public GridCoverage2D inDtm = null;

    @Description(inDsm_DESCR)
    @In
    public GridCoverage2D inDsm = null;

    @Description(inStand_DESCR)
    @In
    public GridCoverage2D inStand = null;

    @Description(inConnectivity_DESCR)
    @In
    public GridCoverage2D inConnectivity = null;

    @Description(pConnectivityThreshold_DESCR)
    @In
    public double pConnectivityThreshold = 4.0;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description(outNetnum_DESCR)
    @Out
    public GridCoverage2D outNetnum = null;

    @Description(outBasins_DESCR)
    @Out
    public GridCoverage2D outBasins = null;

    // VARS DOC START
    public static final String outBasins_DESCR = "The output subbasins raster map.";
    public static final String outNetnum_DESCR = "The output netnumbering raster map.";
    public static final String outNetPoints_DESCR = "The output points network layer with the additional attributes vegetation height and timber volume .";
    public static final String pConnectivityThreshold_DESCR = "Threshold on connectivity map for extracting unstable connected pixels of the basins.";
    public static final String inConnectivity_DESCR = "The input downslope connectivity index raster map.";
    public static final String inStand_DESCR = "The input total stand volume raster map.";
    public static final String inDsm_DESCR = "The input superficial elevation raster map.";
    public static final String inDtm_DESCR = "The input terrain elevation raster map.";
    public static final String inNet_DESCR = "The input network raster map.";
    public static final String inTca_DESCR = "The input total contributing areas raster map.";
    public static final String inFlow_DESCR = "The input flow directions raster map.";
    public static final String inInundationArea_DESCR = "The input polygon layer with the inundation areas.";
    public static final String inNetPoints_DESCR = "The input hierarchy point network layer.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw10_areatonetpointassociator";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, bankflull, width, inundation, vegetation";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Calculate median vegetation height and total timber volume of the vegetation on unstable and connected areas of each subbasin.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        GridGeometry2D gridGeometry = inFlow.getGridGeometry();
        GeometryFactory gf = GeometryUtilities.gf();

        /*
         * extract the inundated area from the polygon
         */
        PreparedGeometry preparedFloodingArea = getFloodingArea(inInundationArea);

        /*
         * extract the Canopy Height Model from DTM and DSM
         */
        GridCoverage2D chmGC = getChm(inDsm, inDtm);

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
        RandomIter chmIter = CoverageUtilities.getRandomIterator(chmGC);
        RandomIter standIter = CoverageUtilities.getRandomIterator(inStand);

        HashMap<Integer, DescriptiveStatistics> heightBasin2ValueMap = new HashMap<Integer, DescriptiveStatistics>();
        HashMap<Integer, DescriptiveStatistics> standBasin2ValueMap = new HashMap<Integer, DescriptiveStatistics>();

        pm.beginTask("Calculating vegetation stats.", cols);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double netnumDouble = netnumBasinsIter.getSampleDouble(c, r, 0);
                if (!isNovalue(netnumDouble)) {
                    Integer netNum = (int) netnumDouble;
                    Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
                    Point point = gf.createPoint(coordinate);
                    double connectivityDouble = connectivityIter.getSampleDouble(c, r, 0);
                    /*
                     * check if the point is connected to the network:
                     * - connectivity index less than the threshold
                     * - point is inside the inundated area
                     * and fill the hashmaps with the correspondent positions.
                     */
                    if (connectivityDouble < pConnectivityThreshold || preparedFloodingArea.intersects(point)) {
                        double chmDouble = chmIter.getSampleDouble(c, r, 0);
                        double standDouble = standIter.getSampleDouble(c, r, 0);
                        DescriptiveStatistics summaryHeightStatistics = heightBasin2ValueMap.get(netNum);
                        DescriptiveStatistics summaryStandStatistics = standBasin2ValueMap.get(netNum);
                        if (summaryHeightStatistics == null) {
                            summaryHeightStatistics = new DescriptiveStatistics();
                            summaryStandStatistics = new DescriptiveStatistics();
                            heightBasin2ValueMap.put(netNum, summaryHeightStatistics);
                            standBasin2ValueMap.put(netNum, summaryStandStatistics);
                        }
                        summaryHeightStatistics.addValue(chmDouble);
                        summaryStandStatistics.addValue(standDouble);
                    }
                }

            }
            pm.worked(1);
        }
        pm.done();

        /*
         * create the structure for the output attributes and insert the summary statistics
         * as attributes
         */
        FeatureExtender ext = new FeatureExtender(inNetPoints.getSchema(), new String[]{LWFields.VEG_VOL, LWFields.VEG_H},
                new Class[]{Double.class, Double.class});
        List<SimpleFeature> inNetworkPointsList = FeatureUtilities.featureCollectionToList(inNetPoints);
        DefaultFeatureCollection finalNetworkPointsFC = new DefaultFeatureCollection();
        final java.awt.Point point = new java.awt.Point();
        for( SimpleFeature inPointFeature : inNetworkPointsList ) {
            Geometry geometry = (Geometry) inPointFeature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();
            CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, point);
            int netnum = netnumBasinsIter.getSample(point.x, point.y, 0);

            DescriptiveStatistics summaryHeightStatistics = heightBasin2ValueMap.get(netnum);
            double medianHeight = 0.0;
            if (summaryHeightStatistics != null) {
                medianHeight = summaryHeightStatistics.getPercentile(50);
            }

            DescriptiveStatistics summaryStandStatistics = standBasin2ValueMap.get(netnum);
            double sumStand = 0.0;
            if (summaryStandStatistics != null) {
                sumStand = summaryStandStatistics.getSum();
            }

            SimpleFeature newPointFeature = ext.extendFeature(inPointFeature, new Object[]{sumStand, medianHeight});
            finalNetworkPointsFC.add(newPointFeature);
        }
        outNetPoints = finalNetworkPointsFC;
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

    /*
    * extract the Canopy Height Model from DTM and DSM
    */
    private GridCoverage2D getChm( GridCoverage2D inDsm, GridCoverage2D inDtm ) throws Exception {
        OmsRasterDiff rasterDiff = new OmsRasterDiff();
        rasterDiff.pm = pm;
        rasterDiff.inRaster1 = inDsm;
        rasterDiff.inRaster2 = inDtm;
        rasterDiff.pThreshold = 0.0;
        rasterDiff.doNegatives = false;
        rasterDiff.process();
        GridCoverage2D out = rasterDiff.outRaster;
        return out;
    }

}
