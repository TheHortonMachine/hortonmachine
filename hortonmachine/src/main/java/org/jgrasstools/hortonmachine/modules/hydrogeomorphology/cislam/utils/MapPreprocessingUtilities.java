/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.Node;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.OmsCislam;

public class MapPreprocessingUtilities {

    /**
     * Runs across the passed elevation map and check corresponding points in passed Slope map.
     * When a border point is found (valid in elevation map but null in slope) a new value is computed for the border
     * and is assigned to the slope cell. More, when a valid but zero value is found in Slope map, the  is substituted
     * @param inSlope The Slope map from OmsSlope module
     * @param inPit The depitted elevation map from OmsPit module
     * @param minimumAllowedSlope the value to substitute to zero values (if left zero defaults to 0,009 [== tan(0.5 degree) ] )
     * @return outSlope the fixed Slope map ready for OmsCislam model
     * @prerequisite The two Grids must have the same RegionMap
     */
    public static GridCoverage2D slopeMapFixZeroValuesAndBorder( GridCoverage2D inSlope, GridCoverage2D inPit, double minimumAllowedSlope,
            IJGTProgressMonitor pm ) {

        if (minimumAllowedSlope <= 0) {
            minimumAllowedSlope = OmsCislam.MINIMM_ALLOWED_SLOPE;
        }

        // TODO Check region maps of the two Grids are the same

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter inPitIter = CoverageUtilities.getRandomIterator(inPit);

        WritableRaster inSlopeWR = CoverageUtilities.renderedImage2WritableRaster(inSlope.getRenderedImage(), false);
        WritableRandomIter inSlopeIter = RandomIterFactory.createWritable(inSlopeWR, null);

        pm.beginTask("Started fixing slope borders and zero values..", rows); //$NON-NLS-1$

        GridNode elevationNode;
        // Cycling into the valid region.
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {

                elevationNode = new GridNode(inPitIter, cols, rows, xRes, yRes, c, r);

                if (elevationNode.isValid()) {

                    double slopeValue = elevationNode.getValueFromMap(inSlopeIter);
                    // Check if we are on the border (slope maps out of OmsSlope module are
                    // one cell smaller than parent elevation map unless fixed)
                    if (JGTConstants.isNovalue(slopeValue)) {
                        // We are on the border where elevation is valid but slope is not:
                        // compute value for slope
                        List<GridNode> surroundingValidNodes = elevationNode.getValidSurroundingNodes();
                        double newSlope = minimumAllowedSlope;
                        for( GridNode elevNode : surroundingValidNodes ) {
                            double slope = elevationNode.getSlopeTo(elevNode);
                            if (slope > newSlope) {
                                newSlope = slope;
                            }
                        }
                        elevationNode.setValueInMap(inSlopeIter, newSlope);
                    } else if (slopeValue == 0.0) {
                        // We are inside the basin but on a 'plateau' of the elevation model
                        // Implement fixing: zero values cells are given value 0.009 [tan(0.5degree)]
                        elevationNode.setValueInMap(inSlopeIter, minimumAllowedSlope);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        // CoverageUtilities.setNovalueBorder(mflowWR);
        GridCoverage2D outFixedSlope = CoverageUtilities.buildCoverage("slope", inSlopeWR, regionMap, inPit.getCoordinateReferenceSystem());

        return outFixedSlope;
    }

    /**
     * Runs across the passed elevation map and check corresponding points in passed Slope map.
     * When a border point is found (valid in elevation map but null in slope) a new value is computed for the border
     * and is assigned to the slope cell. More, when a valid but zero value is found in Slope map, a default value of 0.009 is substituted
     * @param inPit depitted elevation map from OmsPit module
     * @param inSlope slope map from OmsSlope module
     * @return outSlope the fixed Slope map ready for OmsCislam model
     * @prerequisite The two Grids must have the same RegionMap
     */
    public static GridCoverage2D slopeMapFixZeroValuesAndBorder( GridCoverage2D inPit, GridCoverage2D inSlope, IJGTProgressMonitor pm ) {
        return slopeMapFixZeroValuesAndBorder(inSlope, inPit, OmsCislam.MINIMM_ALLOWED_SLOPE, pm);
    }

    /**
     * Runs across the passed Flow map and marks any outlet with the FlowNode.OUTLET constant (=10)
     * @param inFlow
     * @param pm 
     * @return
     */
    public static GridCoverage2D flowMapMarkOutlet( GridCoverage2D inFlow, IJGTProgressMonitor pm ) {
        // Implement fixing: mark Outlet cell with value 10
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        // double xRes = regionMap.getXres();
        // double yRes = regionMap.getYres();

        // RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);

        WritableRaster mflowWR = CoverageUtilities.renderedImage2WritableRaster(inFlow.getRenderedImage(), false);
        WritableRandomIter mflowIter = RandomIterFactory.createWritable(mflowWR, null);

        pm.beginTask("Start finding Outlets..", rows); //$NON-NLS-1$

        // Cycling into the valid region.
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                FlowNode flowNode = new FlowNode(mflowIter, cols, rows, c, r);
                if (flowNode.isValid() && flowNode.isHeadingOutside()) {
                    flowNode.setValueInMap(mflowIter, FlowNode.OUTLET);
                    pm.message("A node was marked as an Outlet");
                }
            }
            pm.worked(1);
        }
        pm.done();

        // CoverageUtilities.setNovalueBorder(mflowWR);
        GridCoverage2D outMarkedFlow = CoverageUtilities.buildCoverage("flow", mflowWR, regionMap, inFlow.getCoordinateReferenceSystem());

        return outMarkedFlow;
    }

    public static boolean isValidFlowMap( GridCoverage2D inFlow, IJGTProgressMonitor pm ) {

        int outletCount = countCellsWith10Value(inFlow, pm);

        if (outletCount == 0) {
            pm.errorMessage("No cell marked as outlets (value 10) was found. Use OmsMarkoutles model to fix this problem.");
            return false;
        } else if (outletCount > 1) {
            pm.errorMessage("More than one cell marked as outlet (value 10) was found. This Fow map may include more than one basin and is so not suitable for the OmsCislam module.");
            return false;
        } else {
            return true;
        }
    }

    public static int countCellsWith10Value( GridCoverage2D inMap, IJGTProgressMonitor pm ) {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inMap);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RandomIter inMapIter = CoverageUtilities.getRandomIterator(inMap);

        pm.beginTask("Started checking Slope map for zero values..", rows); //$NON-NLS-1$

        double celValue;
        int outletCount = 0;
        // Cycling into the valid region.
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {

                celValue = inMapIter.getSampleDouble(c, r, 0);
                if (celValue == 10.0) {
                    pm.errorMessage("A cell with value equal to 10 was found at " + c + ";" + r); //$NON-NLS-1$
                    outletCount++;
                }
                // elevationNode = new GridNode(inPitIter, cols, rows, yRes, yRes, c, r);
            }
            pm.worked(1);
        }
        pm.done();

        return outletCount;
    }

    public static boolean isValidSlopeMap( GridCoverage2D inSlope, IJGTProgressMonitor pm ) {

        int errorCount = countZeroValueCells(inSlope, pm);

        if (errorCount != 0) {
            pm.errorMessage("One or more cells with slope value equal to zero have been found: fix this issue to proceed");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the number of map cells having a zero-value
     * @param inMap the map to check for zero-value cells
     * @param pm
     * @return int the number of zero-value cells in the inspected map
     */
    public static int countZeroValueCells( GridCoverage2D inMap, IJGTProgressMonitor pm ) {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inMap);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RandomIter inMapIter = CoverageUtilities.getRandomIterator(inMap);

        pm.beginTask("Started checking Slope map for zero values..", rows); //$NON-NLS-1$

        double nodeSample;
        int errorCount = 0;
        // Cycling into the valid region.
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {

                nodeSample = inMapIter.getSampleDouble(c, r, 0);
                if (nodeSample == 0.0) {
                    pm.errorMessage("A cell with Slope value equal to zero was found at " + c + ";" + r); //$NON-NLS-1$
                    errorCount++;
                }
                // elevationNode = new GridNode(inPitIter, cols, rows, yRes, yRes, c, r);
            }
            pm.worked(1);
        }
        pm.done();

        return errorCount;
    }

    /**
     * Runs across the passed Flow map and checks for missing border cells to rebuild. Tools like OmsFlowDirections do not calculate
     * flow values for cells that lack one or more of the eight surrounding cells and this causes the outcome flow map not to fully cover
     * the corresponding basin
     * @param inFlow The to-be-chacked flow map
     * @param inPit The depitted DEM map
     * @param pm Parent's progress monitor
     * @return
     */
    public static GridCoverage2D flowMapRebuildBorder( GridCoverage2D inFlow, GridCoverage2D inPit, IJGTProgressMonitor pm ) {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter inPitIter = CoverageUtilities.getRandomIterator(inPit);

        WritableRaster mflowWR = CoverageUtilities.renderedImage2WritableRaster(inFlow.getRenderedImage(), false);
        WritableRandomIter mflowIter = RandomIterFactory.createWritable(mflowWR, null);

        pm.beginTask("Started checking for missing border cell in flow map..", rows); //$NON-NLS-1$

        GridNode pitnode;
        FlowNode flownode;
        List<GridNode> pitNodes;
        
        // Cycling into the valid region.
        int r = 0;
        int c = 0;
        for( r = 0; r < rows; r++ ) {
            for( c = 0; c < cols; c++ ) {

                pitnode = new GridNode(inPitIter, cols, rows, xRes, yRes, c, r);
                flownode = new FlowNode(mflowIter, cols, rows, c, r);
                
                // Select valid DEM cells that lack a correspondence in flow map
                if (pitnode.isValid() && !flownode.isValid()) {

                    // We are on a border cell: let's compute its Flow value
                    pitNodes = pitnode.getSurroundingNodes();
                    int flowvalue = 0;
                    double delta = 0.0;
                    for( int i = 0; i < 8; i++ ) {

                        // Skip null cells
                        if (pitNodes.get(i) != null) {
                            if (pitnode.getSlopeTo(pitNodes.get(i)) >= delta) {
                                delta = pitnode.getSlopeTo(pitNodes.get(i));
                                flowvalue = i + 1;
                            }
                        }

                    }

                    // If flowvalue cannot be determined, the cell should be an outlet
                    if (flowvalue == 0) {
                        pm.message("An outlet was found in the flow map: use OmsMarkoutlets model to mark it");
                        List<GridNode> surrNodes = pitnode.getSurroundingNodes();
                        for( int j = 0; j < 8; j++ ) {
                            // Find the first node around the outlet which is not in the map (so is invalid)
                            // and assign to the outlet his flow direction (so outside the basin).
                            // This will allow the outlet to be seen as outward flowing by other tools
                            if(surrNodes.get(j)==null){
                                flowvalue = j+1;
                            }
                        }
                    }
                    pitnode.setValueInMap(mflowIter, flowvalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        GridCoverage2D outFlow = CoverageUtilities.buildCoverage("flow", mflowWR, regionMap, inFlow.getCoordinateReferenceSystem());

        return outFlow;
    }

}
