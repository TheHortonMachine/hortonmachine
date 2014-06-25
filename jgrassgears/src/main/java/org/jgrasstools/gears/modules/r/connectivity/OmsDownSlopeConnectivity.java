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
package org.jgrasstools.gears.modules.r.connectivity;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.ConstantRandomIter;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description(OMSCUTOUT_DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("connectivity, raster")
@Label(JGTConstants.HILLSLOPE)
@Name("downslopeconnectivity")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsDownSlopeConnectivity extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow;

    @Description("The network map.")
    @In
    public GridCoverage2D inNet;

    @Description("The map of slope.")
    @Unit("m/m")
    @In
    public GridCoverage2D inSlope;

    @Description("The optional map of weights.")
    @In
    public GridCoverage2D inWeights;

    @Description("The optional constant value of weights.")
    @In
    public Double pWeight;

    @Description("The connectivity map.")
    @Out
    public GridCoverage2D outConnectivity = null;

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

        RandomIter weightsIter;
        if (inWeights != null) {
            weightsIter = CoverageUtilities.getRandomIterator(inWeights);
        } else {
            weightsIter = new ConstantRandomIter(pWeight);
        }

        WritableRaster[] connectivityRasterHolder = new WritableRaster[1];
        outConnectivity = CoverageUtilities.createCoverageFromTemplate(inFlow, JGTConstants.doubleNovalue,
                connectivityRasterHolder);
        WritableRandomIter connectivityIter = CoverageUtilities.getWritableRandomIterator(connectivityRasterHolder[0]);

        pm.beginTask("Calculate downslope connectivity...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 0; c < nCols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, nCols, nRows, c, r);
                if (!flowNode.isValid()) {
                    continue;
                }

                GridNode netNode = new GridNode(netIter, nCols, nRows, xres, yres, c, r);

                double connectivitySum = 0;
                while( flowNode.isValid() && !netNode.isValid() ) {
                    FlowNode nextFlowNode = flowNode.goDownstream();
                    double distance = sqrt(pow((nextFlowNode.col - flowNode.col) * xres, 2.0)
                            + pow((nextFlowNode.row - flowNode.row) * yres, 2.0));

                    double weight = weightsIter.getSampleDouble(flowNode.col, flowNode.row, 0);
                    double slope = slopeIter.getSampleDouble(flowNode.col, flowNode.row, 0);
                    if (slope == 0.0) {
                        slope = 0.005;
                    }
                    double Di = distance / weight / slope;
                    connectivitySum = connectivitySum + Di;
                    flowNode = nextFlowNode;
                    netNode = new GridNode(netIter, nCols, nRows, xres, yres, nextFlowNode.col, nextFlowNode.row);
                }
                connectivityIter.setSample(c, r, 0, connectivitySum);
            }
            pm.worked(1);
        }
        pm.done();
    }

    public static void main( String[] args ) throws Exception {
        String flowPath = "/home/moovida/gsoc/basin_flow.asc";
        String netPath = "/home/moovida/gsoc/basin_net.asc";
        String slopePath = "/home/moovida/gsoc/basin_slope.asc";
        String connectPath = "/home/moovida/gsoc/basin_connectivity.asc";

        OmsDownSlopeConnectivity c = new OmsDownSlopeConnectivity();
        c.inFlow = OmsRasterReader.readRaster(flowPath);
        c.inNet = OmsRasterReader.readRaster(netPath);
        c.inSlope = OmsRasterReader.readRaster(slopePath);
        c.pWeight = 100.0;
        c.process();

        OmsRasterWriter.writeRaster(connectPath, c.outConnectivity);
    }

}
