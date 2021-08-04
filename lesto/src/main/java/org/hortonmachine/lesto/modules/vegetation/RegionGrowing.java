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
package org.hortonmachine.lesto.modules.vegetation;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Description("A region growing module")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("region growing, raster")
@Label(HMConstants.LESTO + "/vegetation")
@Name("regiongrowing")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class RegionGrowing extends HMModel {
    @Description("The vector of maxima points")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMaxima;

    @Description("The dsm raster")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDsm;

    @Description("The dtm raster. If not supplied it will be put to a plane placed in 0.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

    @Description("The maximum crown radius.")
    @Unit("m")
    @In
    public double pRadius = 5.0;

    @Description("The minimum tree height considered.")
    @Unit("m")
    @In
    public double pHeight = 5.0;

    @Description("The minimum height percentage considered from the top.")
    @Unit("%")
    @In
    public double pPtop = 75;

    @Description("Use the elevation to index instead of a sequence.")
    @In
    public boolean doElev = false;

    @Description("The regions raster")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    private WritableRandomIter outIter;

    public void process() throws Exception {
        checkNull(inMaxima, inDsm, outRaster);

        GridCoverage2D inDsmGC = getRaster(inDsm);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDsmGC);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        GridGeometry2D gridGeometry = inDsmGC.getGridGeometry();

        double dsmNv = HMConstants.getNovalue(inDsmGC);
        double dtmNv;

        RandomIter dsmIter = CoverageUtilities.getRandomIterator(inDsmGC);
        RandomIter dtmIter;
        if (inDtm != null) {
            GridCoverage2D inDtmGC = getRaster(inDtm);
            dtmIter = CoverageUtilities.getRandomIterator(inDtmGC);
            dtmNv = HMConstants.getNovalue(inDtmGC);
        } else {
            WritableRaster dtmWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, 0.0);
            dtmIter = RandomIterFactory.createWritable(dtmWR, null);
            dtmNv = HMConstants.doubleNovalue;
        }

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        outIter = RandomIterFactory.createWritable(outWR, null);

        SimpleFeatureCollection inMaximaFC = getVector(inMaxima);
        List<SimpleFeature> maximaList = FeatureUtilities.featureCollectionToList(inMaximaFC);

        pm.beginTask("Processing region growing...", maximaList.size());
        int index = 0;
        for( SimpleFeature maximaFeature : maximaList ) {
            Coordinate coordinate = ((Geometry) maximaFeature.getDefaultGeometry()).getCoordinate();
            int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, null);

            GridNode startDsmNode = new GridNode(dsmIter, cols, rows, xRes, yRes, colRow[0], colRow[1], dsmNv);
            GridNode startDtmNode = new GridNode(dtmIter, cols, rows, xRes, yRes, colRow[0], colRow[1], dtmNv);
            growRegion(startDsmNode, startDtmNode, index, startDsmNode, startDtmNode);

            index++;
            pm.worked(1);
        }
        pm.done();

        dtmIter.done();
        outIter.done();

        GridCoverage2D outRasterGC = CoverageUtilities.buildCoverage("filtered", outWR, regionMap,
                inDsmGC.getCoordinateReferenceSystem());
        dumpRaster(outRasterGC, outRaster);
    }

    private void growRegion( final GridNode topDsmNode, final GridNode topDtmNode, final int index, GridNode dsmNode,
            GridNode dtmNode ) {
        if (dsmNode.isValid() && dtmNode.isValid()) {
            double topDsmElevation = topDsmNode.elevation;
            double topDtmElevation = topDtmNode.elevation;

            // do region growing
            if (doElev) {
                dsmNode.setDoubleValueInMap(outIter, topDsmElevation);
            } else {
                dsmNode.setValueInMap(outIter, index);
            }

            double currentDsmElevation = dsmNode.elevation;
            // double currentDtmElevation = dtmNode.elevation;

            // check surrounding
            List<GridNode> surroundingDsmNodes = dsmNode.getSurroundingNodes();
            List<GridNode> surroundingDtmNodes = dtmNode.getSurroundingNodes();
            for( int k = 0; k < surroundingDsmNodes.size(); k++ ) {
                GridNode surroundingDsmNode = surroundingDsmNodes.get(k);
                GridNode surroundingDtmNode = surroundingDtmNodes.get(k);
                if (surroundingDsmNode == null || surroundingDtmNode == null || !surroundingDsmNode.isValid()
                        || !surroundingDtmNode.isValid()) {
                    continue;
                }

                int col = surroundingDsmNode.col;
                int row = surroundingDsmNode.row;
                double outValue = outIter.getSampleDouble(col, row, 0);
                if (!isNovalue(outValue)) {
                    // someone already passed there
                    continue;
                }

                double surroundingDsmElevation = surroundingDsmNode.elevation;
                double surroundingDtmElevation = surroundingDtmNode.elevation;
                if (surroundingDsmElevation < currentDsmElevation) {
                    // do checks
                    // height is lower than 5 meters
                    double deltaElevation = surroundingDsmElevation - surroundingDtmElevation;
                    if (deltaElevation < pHeight) {
                        continue;
                    } else
                    // height is lower than 75% of the top
                    if (deltaElevation < pPtop / 100.0 * (topDsmElevation - topDtmElevation)) {
                        continue;
                    } else
                    // distance from
                    if (topDsmNode.getDistance(surroundingDsmNode) > pRadius) {
                        continue;
                    }

                    // mark it
                    if (doElev) {
                        surroundingDsmNode.setDoubleValueInMap(outIter, topDsmElevation);
                    } else {
                        surroundingDsmNode.setValueInMap(outIter, index);
                    }
                    growRegion(topDsmNode, topDtmNode, index, surroundingDsmNode, surroundingDtmNode);
                }
            }
        }
    }

    // public static void main( String[] args ) throws Exception {
    // OmsRegionGrowing g = new OmsRegionGrowing();
    // String dsm = "t1_dsmdtm_diff";
    // String dtm = "dtm";
    // String out = "t1_dsmdtm_diff_maxima_growing5";
    // String inmaximaShp =
    // "watersheds_maxima_popescu.shp";
    // String outRegionsShp = "region_shapes.shp";
    // g.inMaxima = getVector(inmaximaShp);
    // g.inDsm = getRaster(dsm);
    // // g.inDtm = getRaster(dtm);
    // g.pRadius = 5.0;
    // g.pHeight = 5.0;
    // g.pPtop = 75.0;
    // g.process();
    // dumpRaster(g.outRaster, out);
    //
    // OmsBasinShape bs = new OmsBasinShape();
    // bs.inBasins = g.outRaster;
    // bs.process();
    // SimpleFeatureCollection outBasins = bs.outBasins;
    // dumpVector(outBasins, outRegionsShp);
    // }

}
