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
package org.jgrasstools.modules;

import static java.lang.Math.pow;
import static org.jgrasstools.gears.libs.modules.FlowNode.NETVALUE;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.libs.modules.Variables.TCA;
import static org.jgrasstools.gears.libs.modules.Variables.TCA_CONVERGENT;
import static org.jgrasstools.gears.libs.modules.Variables.TCA_SLOPE;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Extracts the raster network from an elevation model.")
@Documentation("OmsExtractNetwork.html")
@Author(name = "Andrea Antonello, Franceschi Silvia, Erica Ghesla, Andrea Cozzini, Silvano Pisoni", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Network, Vector, FlowDirectionsTC, GC, OmsDrainDir, OmsGradient, OmsSlope")
@Label(JGTConstants.NETWORK)
@Name("_extractnet")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class OmsExtractNetwork extends JGTModel {

    @Description("The map of total contributing areas.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The optional map of flowdirections (needed for case with slope or topographic classes).")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The optional map of slope.")
    @In
    public GridCoverage2D inSlope = null;

    @Description("The optional map of aggregated topographic classes.")
    @In
    public GridCoverage2D inTc3 = null;

    @Description("The threshold on the map.")
    @In
    public double pThres = 0;

    @Description("The thresholding mode (default is on tca).")
    @UI("combo:" + TCA + "," + TCA_SLOPE + "," + TCA_CONVERGENT)
    @In
    public String pMode = TCA;

    @Description("OmsTca exponent for the mode with slope or topographic classes (default = 0.5).")
    @In
    public double pExp = 0.5;

    @Description("The extracted network raster.")
    @Out
    public GridCoverage2D outNet = null;

    /*
     * INTERNAL VARIABLES
     */
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int cols;
    private int rows;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inTca);
        if (!concatOr(outNet == null, doReset)) {
            return;
        }
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.getCols();
        rows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        RenderedImage tcaRI = inTca.getRenderedImage();

        WritableRaster networkWR = null;
        if (pMode.equals(TCA)) {
            checkNull(flowRI, tcaRI);
            networkWR = extractNetTcaThreshold(flowRI, tcaRI);
        } else if (pMode.equals(TCA_SLOPE)) {
            checkNull(inSlope);
            RenderedImage slopeRI = inSlope.getRenderedImage();
            networkWR = extractNetMode1(flowRI, tcaRI, slopeRI);
        } else if (pMode.equals(TCA_CONVERGENT)) {
            checkNull(inSlope, inTc3);
            RenderedImage classRI = inTc3.getRenderedImage();
            RenderedImage slopeRI = inSlope.getRenderedImage();
            networkWR = extractNetMode2(flowRI, tcaRI, classRI, slopeRI);
        }
        if (isCanceled(pm)) {
            return;
        }
        outNet = CoverageUtilities.buildCoverage("network", networkWR, regionMap, inFlow.getCoordinateReferenceSystem());

    }
    /**
     * this method calculates the network using a threshold value on the
     * contributing areas or on magnitudo
     */
    private WritableRaster extractNetTcaThreshold( RenderedImage flowRI, RenderedImage tcaRI ) {
        // create new RasterData for the network matrix
        RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);
        WritableRaster netWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter netIter = RandomIterFactory.createWritable(netWR, null);

        pm.beginTask(msg.message("extractnetwork.extracting"), rows); //$NON-NLS-1$
        for( int r = 0; r < rows; r++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int c = 0; c < cols; c++ ) {
                double tcaValue = tcaIter.getSampleDouble(c, r, 0);
                if (!isNovalue(tcaValue)) {
                    if (tcaValue >= pThres) { // FIXME needs power here?
                        netIter.setSample(c, r, 0, NETVALUE);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        return netWR;
    }

    /**
     * this method calculates the network imposing a threshold value on the
     * product of two quantities, for example the contributing area and the
     * slope.
     */
    private WritableRaster extractNetMode1( RenderedImage flowRI, RenderedImage tcaRI, RenderedImage slopeRI ) {

        RandomIter flowRandomIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaRandomIter = RandomIterFactory.create(tcaRI, null);
        RandomIter slopeRandomIter = RandomIterFactory.create(slopeRI, null);

        // create new RasterData for the network matrix
        WritableRaster networkWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null,
                JGTConstants.doubleNovalue);
        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(networkWR, null);

        pm.beginTask(msg.message("extractnetwork.extracting"), rows); //$NON-NLS-1$
        for( int r = 0; r < rows; r++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int c = 0; c < cols; c++ ) {
                double tcaValue = tcaRandomIter.getSampleDouble(c, r, 0);
                double slopeValue = slopeRandomIter.getSampleDouble(c, r, 0);
                if (!isNovalue(tcaValue) && !isNovalue(slopeValue)) {
                    tcaValue = pow(tcaValue, pExp);

                    if (tcaValue * slopeValue >= pThres) {
                        netRandomIter.setSample(c, r, 0, NETVALUE);
                        FlowNode flowNode = new FlowNode(flowRandomIter, cols, rows, c, r);
                        FlowNode runningNode = flowNode;
                        while( (runningNode = runningNode.goDownstream()) != null ) {
                            int rCol = runningNode.col;
                            int rRow = runningNode.row;
                            double tmpNetValue = netRandomIter.getSampleDouble(rCol, rRow, 0);
                            if (!isNovalue(tmpNetValue)) {
                                break;
                            }
                            if (runningNode.isMarkedAsOutlet()) {
                                netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                break;
                            } else if (runningNode.touchesBound()) {
                                FlowNode goDownstream = runningNode.goDownstream();
                                if (goDownstream == null || !goDownstream.isValid()) {
                                    netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                    break;
                                }
                            }
                            netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                        }
                    }
                } else {
                    netRandomIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
        return networkWR;
    }

    /**
     * this method the network is extracted by considering only concave points
     * as being part of the channel network.
     */
    private WritableRaster extractNetMode2( RenderedImage flowRI, RenderedImage tcaRI, RenderedImage classRI,
            RenderedImage slopeRI ) {
        RandomIter flowRandomIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaRandomIter = RandomIterFactory.create(tcaRI, null);
        RandomIter classRandomIter = RandomIterFactory.create(classRI, null);
        RandomIter slopeRandomIter = RandomIterFactory.create(slopeRI, null);
        WritableRaster netImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);

        // try the operation!!

        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(netImage, null);

        pm.beginTask(msg.message("extractnetwork.extracting"), rows); //$NON-NLS-1$
        for( int r = 0; r < rows; r++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int c = 0; c < cols; c++ ) {
                double tcaValue = tcaRandomIter.getSampleDouble(c, r, 0);
                double slopeValue = slopeRandomIter.getSampleDouble(c, r, 0);
                if (!isNovalue(tcaValue) && !isNovalue(slopeValue)) {
                    tcaValue = pow(tcaValue, pExp) * slopeValue;
                    if (tcaValue >= pThres && classRandomIter.getSample(c, r, 0) == 15.0) {
                        netRandomIter.setSample(c, r, 0, NETVALUE);
                        FlowNode flowNode = new FlowNode(flowRandomIter, cols, rows, c, r);
                        FlowNode runningNode = flowNode;
                        while( (runningNode = runningNode.goDownstream()) != null ) {
                            int rCol = runningNode.col;
                            int rRow = runningNode.row;
                            double tmpNetValue = netRandomIter.getSampleDouble(rCol, rRow, 0);
                            if (!isNovalue(tmpNetValue)) {
                                break;
                            }
                            if (runningNode.isMarkedAsOutlet()) {
                                netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                break;
                            } else if (runningNode.touchesBound()) {
                                FlowNode goDownstream = runningNode.goDownstream();
                                if (goDownstream == null || !goDownstream.isValid()) {
                                    netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                    break;
                                }
                            }
                            netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        return netImage;
    }

}
