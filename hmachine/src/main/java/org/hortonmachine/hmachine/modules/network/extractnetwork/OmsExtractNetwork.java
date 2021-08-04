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
package org.hortonmachine.hmachine.modules.network.extractnetwork;

import static java.lang.Math.pow;
import static org.hortonmachine.gears.libs.modules.FlowNode.NETVALUE;
import static org.hortonmachine.gears.libs.modules.HMConstants.NETWORK;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.shortNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.TCA;
import static org.hortonmachine.gears.libs.modules.Variables.TCA_CONVERGENT;
import static org.hortonmachine.gears.libs.modules.Variables.TCA_SLOPE;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.Node;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

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

@Description(OmsExtractNetwork.OMSEXTRACTNETWORK_DESCRIPTION)
@Author(name = OmsExtractNetwork.OMSEXTRACTNETWORK_AUTHORNAMES, contact = OmsExtractNetwork.OMSEXTRACTNETWORK_AUTHORCONTACTS)
@Keywords(OmsExtractNetwork.OMSEXTRACTNETWORK_KEYWORDS)
@Label(OmsExtractNetwork.OMSEXTRACTNETWORK_LABEL)
@Name(OmsExtractNetwork.OMSEXTRACTNETWORK_NAME)
@Status(OmsExtractNetwork.OMSEXTRACTNETWORK_STATUS)
@License(OmsExtractNetwork.OMSEXTRACTNETWORK_LICENSE)
public class OmsExtractNetwork extends GridMultiProcessing {

    @Description(OMSEXTRACTNETWORK_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSEXTRACTNETWORK_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSEXTRACTNETWORK_inSlope_DESCRIPTION)
    @In
    public GridCoverage2D inSlope = null;

    @Description(OMSEXTRACTNETWORK_inTc3_DESCRIPTION)
    @In
    public GridCoverage2D inTc3 = null;

    @Description(OMSEXTRACTNETWORK_pThres_DESCRIPTION)
    @In
    public int pThres = 0;

    @Description(OMSEXTRACTNETWORK_pMode_DESCRIPTION)
    @UI("combo:" + TCA + "," + TCA_SLOPE + "," + TCA_CONVERGENT)
    @In
    public String pMode = TCA;

    @Description(OMSEXTRACTNETWORK_pExp_DESCRIPTION)
    @In
    public double pExp = 0.5;

    @Description(OMSEXTRACTNETWORK_outNet_DESCRIPTION)
    @Out
    public GridCoverage2D outNet = null;

    public static final String OMSEXTRACTNETWORK_DESCRIPTION = "Extracts the raster network from an elevation model.";
    public static final String OMSEXTRACTNETWORK_DOCUMENTATION = "OmsExtractNetwork.html";
    public static final String OMSEXTRACTNETWORK_KEYWORDS = "Network, Vector, FlowDirectionsTC, GC, OmsDrainDir, OmsGradient, OmsSlope";
    public static final String OMSEXTRACTNETWORK_LABEL = NETWORK;
    public static final String OMSEXTRACTNETWORK_NAME = "extractnet";
    public static final int OMSEXTRACTNETWORK_STATUS = 40;
    public static final String OMSEXTRACTNETWORK_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSEXTRACTNETWORK_AUTHORNAMES = "Andrea Antonello, Franceschi Silvia, Erica Ghesla, Andrea Cozzini, Silvano Pisoni";
    public static final String OMSEXTRACTNETWORK_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSEXTRACTNETWORK_inTca_DESCRIPTION = "The map of total contributing areas.";
    public static final String OMSEXTRACTNETWORK_inFlow_DESCRIPTION = "The optional map of flowdirections (needed for case with slope or topographic classes).";
    public static final String OMSEXTRACTNETWORK_inSlope_DESCRIPTION = "The optional map of slope.";
    public static final String OMSEXTRACTNETWORK_inTc3_DESCRIPTION = "The optional map of aggregated topographic classes.";
    public static final String OMSEXTRACTNETWORK_pThres_DESCRIPTION = "The threshold on the map.";
    public static final String OMSEXTRACTNETWORK_pMode_DESCRIPTION = "The thresholding mode (default is on tca).";
    public static final String OMSEXTRACTNETWORK_pExp_DESCRIPTION = "OmsTca exponent for the mode with slope or topographic classes (default = 0.5).";
    public static final String OMSEXTRACTNETWORK_outNet_DESCRIPTION = "The extracted network raster.";

    /*
     * INTERNAL VARIABLES
     */
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int cols;
    private int rows;

    @Execute
    public void process() throws Exception {
        checkNull(inTca);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inTca);
        cols = regionMap.getCols();
        rows = regionMap.getRows();

        RenderedImage tcaRI = inTca.getRenderedImage();

        WritableRaster networkWR = null;
        if (pMode.equals(TCA)) {
            checkNull(tcaRI);
            networkWR = extractNetTcaThreshold(tcaRI);
        } else if (pMode.equals(TCA_SLOPE)) {
            checkNull(inSlope);
            int novalue = HMConstants.getIntNovalue(inFlow);
            RenderedImage flowRI = inFlow.getRenderedImage();
            RenderedImage slopeRI = inSlope.getRenderedImage();
            networkWR = extractNetMode1(flowRI, novalue, tcaRI, slopeRI);
        } else if (pMode.equals(TCA_CONVERGENT)) {
            checkNull(inSlope, inTc3);
            int novalue = HMConstants.getIntNovalue(inFlow);
            RenderedImage flowRI = inFlow.getRenderedImage();
            RenderedImage classRI = inTc3.getRenderedImage();
            RenderedImage slopeRI = inSlope.getRenderedImage();
            networkWR = extractNetMode2(flowRI, novalue, tcaRI, classRI, slopeRI);
        } else {
            throw new ModelsIllegalargumentException("The selected mode is not valid.", this);
        }
        if (pm.isCanceled()) {
            return;
        }
        outNet = CoverageUtilities.buildCoverageWithNovalue("network", networkWR, regionMap, inTca.getCoordinateReferenceSystem(),
                shortNovalue);

    }

    /**
     * this method calculates the network using a threshold value on the
     * contributing areas or on magnitudo
     * @throws Exception 
     */
    private WritableRaster extractNetTcaThreshold( RenderedImage tcaRI ) throws Exception {
        RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);
        WritableRaster netWR = CoverageUtilities.createWritableRaster(cols, rows, Short.class, null, shortNovalue);
        WritableRandomIter netIter = RandomIterFactory.createWritable(netWR, null);

        try {
            pm.beginTask(msg.message("extractnetwork.extracting"), rows * cols); //$NON-NLS-1$
            processGrid(cols, rows, false, ( c, r ) -> {
                if (pm.isCanceled()) {
                    return;
                }
                int tcaValue = tcaIter.getSample(c, r, 0);
                if (!isNovalue(tcaValue)) {
                    if (tcaValue >= pThres) { // FIXME needs power here?
                        netIter.setSample(c, r, 0, NETVALUE);
                    }
                }
                pm.worked(1);
            });
            pm.done();
            return netWR;
        } finally {
            netIter.done();
            tcaIter.done();
        }
    }

    /**
     * this method calculates the network imposing a threshold value on the
     * product of two quantities, for example the contributing area and the
     * slope.
     * @throws Exception 
     */
    private WritableRaster extractNetMode1( RenderedImage flowRI, int novalue, RenderedImage tcaRI, RenderedImage slopeRI )
            throws Exception {

        RandomIter flowRandomIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaRandomIter = RandomIterFactory.create(tcaRI, null);
        RandomIter slopeRandomIter = RandomIterFactory.create(slopeRI, null);

        // create new RasterData for the network matrix
        WritableRaster networkWR = CoverageUtilities.createWritableRaster(cols, rows, Short.class, null, shortNovalue);
        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(networkWR, null);
        try {
            pm.beginTask(msg.message("extractnetwork.extracting"), rows * cols); //$NON-NLS-1$
            processGrid(cols, rows, false, ( c, r ) -> {
                if (pm.isCanceled()) {
                    return;
                }
                double tcaValue = tcaRandomIter.getSample(c, r, 0);
                double slopeValue = slopeRandomIter.getSampleDouble(c, r, 0);
                if (!isNovalue(tcaValue) && !isNovalue(slopeValue)) {
                    tcaValue = pow(tcaValue, pExp);

                    if (tcaValue * slopeValue >= pThres) {
                        netRandomIter.setSample(c, r, 0, NETVALUE);
                        FlowNode flowNode = new FlowNode(flowRandomIter, cols, rows, c, r, novalue);
                        FlowNode runningNode = flowNode;
                        while( (runningNode = runningNode.goDownstream()) != null ) {
                            int rCol = runningNode.col;
                            int rRow = runningNode.row;
                            int tmpNetValue = netRandomIter.getSample(rCol, rRow, 0);
                            if (!isNovalue(tmpNetValue)) {
                                break;
                            }
                            if (runningNode.isMarkedAsOutlet()) {
                                netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                break;
                            } else if (runningNode.touchesBound()) {
                                Node goDownstream = runningNode.goDownstream();
                                if (goDownstream == null || !goDownstream.isValid()) {
                                    netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                    break;
                                }
                            }
                            netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                        }
                    }
                } else {
                    netRandomIter.setSample(c, r, 0, shortNovalue);
                }
                pm.worked(1);
            });
            pm.done();
            return networkWR;
        } finally {
            flowRandomIter.done();
            tcaRandomIter.done();
            slopeRandomIter.done();
            netRandomIter.done();
        }
    }

    /**
     * this method the network is extracted by considering only concave points
     * as being part of the channel network.
     * @throws Exception 
     */
    private WritableRaster extractNetMode2( RenderedImage flowRI, int novalue, RenderedImage tcaRI, RenderedImage classRI,
            RenderedImage slopeRI ) throws Exception {
        RandomIter flowRandomIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaRandomIter = RandomIterFactory.create(tcaRI, null);
        RandomIter classRandomIter = RandomIterFactory.create(classRI, null);
        RandomIter slopeRandomIter = RandomIterFactory.create(slopeRI, null);
        WritableRaster netImage = CoverageUtilities.createWritableRaster(cols, rows, Short.class, null, shortNovalue);

        // try the operation!!

        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(netImage, null);

        try {
            pm.beginTask(msg.message("extractnetwork.extracting"), rows * cols); //$NON-NLS-1$
            processGrid(cols, rows, false, ( c, r ) -> {
                if (pm.isCanceled()) {
                    return;
                }

                double tcaValue = tcaRandomIter.getSample(c, r, 0);
                double slopeValue = slopeRandomIter.getSampleDouble(c, r, 0);
                if (!isNovalue(tcaValue) && !isNovalue(slopeValue)) {
                    tcaValue = pow(tcaValue, pExp) * slopeValue;
                    if (tcaValue >= pThres && classRandomIter.getSample(c, r, 0) == 15) {
                        netRandomIter.setSample(c, r, 0, NETVALUE);
                        FlowNode flowNode = new FlowNode(flowRandomIter, cols, rows, c, r, novalue);
                        FlowNode runningNode = flowNode;
                        while( (runningNode = runningNode.goDownstream()) != null ) {
                            int rCol = runningNode.col;
                            int rRow = runningNode.row;
                            short tmpNetValue = (short) netRandomIter.getSample(rCol, rRow, 0);
                            if (!isNovalue(tmpNetValue)) {
                                break;
                            }
                            if (runningNode.isMarkedAsOutlet()) {
                                netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                break;
                            } else if (runningNode.touchesBound()) {
                                Node goDownstream = runningNode.goDownstream();
                                if (goDownstream == null || !goDownstream.isValid()) {
                                    netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                                    break;
                                }
                            }
                            netRandomIter.setSample(rCol, rRow, 0, NETVALUE);
                        }
                    }
                }
                pm.worked(1);
            });
            pm.done();
            return netImage;
        } finally {
            flowRandomIter.done();
            tcaRandomIter.done();
            classRandomIter.done();
            slopeRandomIter.done();
            netRandomIter.done();
        }
    }

}
