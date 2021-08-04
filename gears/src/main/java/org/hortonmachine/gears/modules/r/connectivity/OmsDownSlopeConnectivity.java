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
package org.hortonmachine.gears.modules.r.connectivity;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.ConstantRandomIter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
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

@Description(OmsDownSlopeConnectivity.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsDownSlopeConnectivity.KEYWORDS)
@Label(OmsDownSlopeConnectivity.LABEL)
@Name(OmsDownSlopeConnectivity.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@Bibliography(OmsDownSlopeConnectivity.BIBLIO)
public class OmsDownSlopeConnectivity extends HMModel {

    @Description(inFlow_DESCR)
    @In
    public GridCoverage2D inFlow;

    @Description(inNet_DESCR)
    @In
    public GridCoverage2D inNet;

    @Description(inSlope_DESCR)
    @Unit("m/m")
    @In
    public GridCoverage2D inSlope;

    @Description(inWeights_DESCR)
    @In
    public GridCoverage2D inWeights;

    @Description(pWeight_DESCR)
    @In
    public Double pWeight;

    @Description(outConnectivity_DESCR)
    @Out
    public GridCoverage2D outConnectivity = null;

    // VARS DOC START
    public static final String outConnectivity_DESCR = "The connectivity map.";
    public static final String pWeight_DESCR = "The optional constant value of weights.";
    public static final String inWeights_DESCR = "The optional map of weights.";
    public static final String inSlope_DESCR = "The map of slope.";
    public static final String inNet_DESCR = "The network map.";
    public static final String inFlow_DESCR = "The map of flowdirections.";
    public static final String KEYWORDS = "connectivity, raster";
    public static final String BIBLIO = "Geomorphometric assessment of spatial sediment connectivity in small Alpine catchments. - Cavalli et al. 2012";
    public static final String NAME = "downslopeconnectivity";
    public static final String DESCRIPTION = "Module for the calculation of the downslope connectivity.";
    public static final String LABEL = HMConstants.HILLSLOPE;
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inNet, inSlope);

        if (pWeight == null && inWeights == null) {
            throw new ModelsIllegalargumentException("At lest one weight definition needs to be supplied.", this);
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);
        RandomIter slopeIter = CoverageUtilities.getRandomIterator(inSlope);

        int flowNv = HMConstants.getIntNovalue(inFlow);
        double netNv = HMConstants.getNovalue(inNet);

        RandomIter weightsIter;
        if (inWeights != null) {
            weightsIter = CoverageUtilities.getRandomIterator(inWeights);
        } else {
            weightsIter = new ConstantRandomIter(pWeight);
        }

        WritableRaster[] connectivityRasterHolder = new WritableRaster[1];
        outConnectivity = CoverageUtilities.createCoverageFromTemplate(inFlow, HMConstants.doubleNovalue,
                connectivityRasterHolder);
        WritableRandomIter connectivityIter = CoverageUtilities.getWritableRandomIterator(connectivityRasterHolder[0]);

        pm.beginTask("Calculate downslope connectivity...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = 0; c < nCols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, nCols, nRows, c, r, flowNv);
                if (!flowNode.isValid()) {
                    continue;
                }

                GridNode netNode = new GridNode(netIter, nCols, nRows, xres, yres, c, r, netNv);

                double connectivitySum = 0;
                while( flowNode.isValid() && !netNode.isValid() ) {
                    FlowNode nextFlowNode = flowNode.goDownstream();
                    if (nextFlowNode == null) {
                        throw new ModelsRuntimeException(
                                "Could not properly navigate the flowdirections. Are you using an extracted basin?", this);
                    }
                    int col = flowNode.col;
                    int nextCol = nextFlowNode.col;
                    int row = flowNode.row;
                    int nextRow = nextFlowNode.row;
                    double distance = sqrt(pow((nextCol - col) * xres, 2.0) + pow((nextRow - row) * yres, 2.0));

                    double weight = weightsIter.getSampleDouble(flowNode.col, flowNode.row, 0);
                    double slope = slopeIter.getSampleDouble(flowNode.col, flowNode.row, 0);
                    if (slope == 0.0) {
                        slope = 0.005;
                    }
                    double Di = distance / weight / slope;
                    connectivitySum = connectivitySum + Di;
                    flowNode = nextFlowNode;
                    netNode = new GridNode(netIter, nCols, nRows, xres, yres, nextFlowNode.col, nextFlowNode.row, netNv);
                }
                connectivityIter.setSample(c, r, 0, connectivitySum);
            }
            pm.worked(1);
        }
        pm.done();
    }

}
