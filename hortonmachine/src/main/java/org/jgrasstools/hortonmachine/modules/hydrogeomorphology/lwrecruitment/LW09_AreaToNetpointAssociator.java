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

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import oms3.annotations.UI;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.hortonmachine.modules.network.netnumbering.OmsNetNumbering;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

@Description("Calculate median vegetation height and total timber volume of the vegetation on unstable and connected areas of each subbasin.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, bankflull, width, inundation, vegetation")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW09_AreaToNetpointAssociator")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW09_AreaToNetpointAssociator extends JGTModel implements LWFields {

    @Description("The input hierarchy point network layer.")
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description("The input polygon layer with the inundation areas.")
    @Out
    public SimpleFeatureCollection inInundationArea = null;

    @Description("The input flow directions raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inFlow = null;

    @Description("The input total contributing areas raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inTca = null;

    @Description("The input network raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inNet = null;

    @Description("The input terrain elevation raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inDtm = null;

    @Description("The input superficial elevation raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inDsm = null;
    
    @Description("The input total stand volume raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inStand = null;

    @Description("The input slope raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inSlope = null;

    @Description("The input downslope connectivity index raster map.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inConnectivity = null;

    @Description("The output points network layer with the additional attributes vegetation height and timber volume .")
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description("The output netnumbering raster map.")
    @Out
    public GridCoverage2D outNetnum = null;

    @Description("The output subbasins raster map.")
    @Out
    public GridCoverage2D outBasins = null;

    // Threshold on connectivity map for extracting unstable connected pixels of the basins.
    double connectivityThreshold = 4.0;

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
        PreparedGeometry preparedFooldingArea = getFloofindArea(inInundationArea);

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
        omsnetnumbering.pThres = 0.0;
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
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
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
                    if (connectivityDouble < connectivityThreshold || preparedFooldingArea.intersects(point)) {
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
        FeatureExtender ext = new FeatureExtender(inNetPoints.getSchema(), new String[]{LWFields.VOLUME, LWFields.MEDIAN},
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
    private PreparedGeometry getFloofindArea( SimpleFeatureCollection inFloodingAreas ) throws Exception {
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
        rasterDiff.process();
        GridCoverage2D out = rasterDiff.outRaster;
        return out;
    }

    public static void main( String[] args ) throws Exception {
        String inNetPointsShp = "D:/lavori_tmp/gsoc/netpoints_width_bridgesdams_slope_floodwidth.shp";
        String inInundatedShp = "D:/lavori_tmp/gsoc/floodpolygon_merged.shp";
        String inFlowRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_flow.asc";
        String inTcaRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_tca.asc";
        String inNetRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_netnull.asc";
        String inDtmRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_dtmfel.asc";
        String inDsmRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_dsm.asc";
        String inStandRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_stand.asc";
        String inSlopeRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_slope.asc";
        String inConnectivityRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_down_slope_con_log10.asc";

        String outNetnumRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_netnum.asc";
        String outBasinsRaster = "D:/lavori_tmp/gsoc/raster/basin_raster/basin_basins.asc";
        String outNetPointsShp = "D:/lavori_tmp/gsoc/netpoints_width_bridgesdams_slope_veg_stand.shp";

        LW09_AreaToNetpointAssociator areaToNetpointAssociator = new LW09_AreaToNetpointAssociator();
        areaToNetpointAssociator.inNetPoints = OmsVectorReader.readVector(inNetPointsShp);
        areaToNetpointAssociator.inInundationArea = OmsVectorReader.readVector(inInundatedShp);

        areaToNetpointAssociator.inFlow = OmsRasterReader.readRaster(inFlowRaster);
        areaToNetpointAssociator.inTca = OmsRasterReader.readRaster(inTcaRaster);
        areaToNetpointAssociator.inNet = OmsRasterReader.readRaster(inNetRaster);
        areaToNetpointAssociator.inDtm = OmsRasterReader.readRaster(inDtmRaster);
        areaToNetpointAssociator.inDsm = OmsRasterReader.readRaster(inDsmRaster);
        areaToNetpointAssociator.inStand = OmsRasterReader.readRaster(inStandRaster);
        areaToNetpointAssociator.inSlope = OmsRasterReader.readRaster(inSlopeRaster);
        areaToNetpointAssociator.inConnectivity = OmsRasterReader.readRaster(inConnectivityRaster);

        areaToNetpointAssociator.process();

        SimpleFeatureCollection outNetPointFC = areaToNetpointAssociator.outNetPoints;
        OmsVectorWriter.writeVector(outNetPointsShp, outNetPointFC);

        GridCoverage2D outNetnum = areaToNetpointAssociator.outNetnum;
        OmsRasterWriter.writeRaster(outNetnumRaster, outNetnum);

        GridCoverage2D outBasins = areaToNetpointAssociator.outBasins;
        OmsRasterWriter.writeRaster(outBasinsRaster, outBasins);

    }

}
