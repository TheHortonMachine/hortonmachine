/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.modules.network.hackstream;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.createDoubleWritableRaster;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

/**
 * <p>
 * The openmi compliant representation of the hackstream model. HackStream
 * arranges a channel net starting from the identification of the branch
 * according to Hack. The main stream is of order 1 and its tributaries of order
 * 2 and so on, the sub-tributaries are of order 3 and so on.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the map containing the drainage directions (-flow)</LI>
 * <LI>the map containing the contributing areas (-tca)</LI>
 * <LI>the map containing the network (-net)</LI>
 * <LI>the map containing the Hack lengths (-hack)</LI>
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the map of the order according the Hack lengths (-hacks)</LI>
 * </OL>
 * <P></DD> Usage: mode 0: h.hackstream --mode 0 --igrass-flow flow --igrass-tca
 * tca --igrass-hackl hackl --igrass-net net --ograss-hacks hacks
 * </p>
 * <p>
 * Usage: mode 1: h.hackstream --mode 1 --igrass-flow flow --igrass-num num
 * --ograss-hacks hacks
 * </p>
 * <p>
 * Note: Such order correponds in some ways to the Horton numeration. It is
 * necessary that the output pixels present a drainage direction value equal to
 * 10. If there is not such identification of the mouth points, the program does
 * not function correctly.
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Rigon
 *         Riccardo
 */

@Description("HackStream arranges a channel net starting from the identification of the branch according to Hack..")
@Author(name = "Erica Ghesla, Andrea Antonello, Franceschi Silvia, Riccardo Rigon", contact = "www.hydrologis.com")
@Keywords("Network, Hack")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class HackStream extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of tca.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The map of hack lengths.")
    @In
    public GridCoverage2D inHacklength = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The map of the netnum.")
    @In
    public GridCoverage2D inNetnum = null;

    @Description("The processing mode.")
    @In
    public double pMode = 0;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of hackstream.")
    @Out
    public GridCoverage2D outHackstream = null;

    int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    

    private int nCols;

    private int nRows;

    private HashMap<String, Double> regionMap;

    @Execute
    public void process() {
        if (!concatOr(outHackstream == null, doReset)) {
            return;
        }

        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        // create new matrix
        WritableRaster segnaWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        WritableRandomIter segnaIter = RandomIterFactory.createWritable(segnaWR, null);

        int count = 0;
        if (pMode == 0) {

            RenderedImage tcaRI = inTca.getRenderedImage();
            RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);

            RenderedImage netRI = inNet.getRenderedImage();
            RandomIter netIter = RandomIterFactory.create(netRI, null);

            RenderedImage hacklengthRI = inHacklength.getRenderedImage();
            RandomIter hacklengthIter = RandomIterFactory.create(hacklengthRI, null);

            for( int j = 0; j < nRows; j++ ) {
                for( int i = 0; i < nCols; i++ ) {
                    if (isNovalue(netIter.getSampleDouble(i, j, 0)))
                        flowIter.setSample(i, j, 0, doubleNovalue);
                    if (flowIter.getSampleDouble(i, j, 0) == 10)
                        count++;
                }
            }
            if (count == 0) {
                throw new ModelsRuntimeException("Please run the h.markoutlets command before.", this);
            }

            hackstream(flowIter, tcaIter, hacklengthIter, segnaIter);

        } else if (pMode == 1) {
            RenderedImage netnumRI = inNetnum.getRenderedImage();
            RandomIter netnumIter = RandomIterFactory.create(netnumRI, null);

            hackstreamNetFixed(flowIter, netnumIter, segnaIter);

        }

    }

    /**
     * gives the channel numeration of the hydrographic network according to
     * Hack’s numeration.
     * 
     * @param m
     *            is the flow data
     * @param tca
     * @param hackl
     * @param hacks
     * @return
     */
    public void hackstream( WritableRandomIter flowIter, RandomIter tcaIter, RandomIter hacklengthIter,
            WritableRandomIter segnaIter ) {
        int contr = 0;
        int count = 0, kk = 0;

        int[] flow = new int[2], param = new int[2];
        int[] flow_p = new int[2];
        int[] punto = new int[2];

        WritableRaster hackstreamWR = createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter hackstreamIter = RandomIterFactory.createWritable(hackstreamWR, null);

        int iterations = 1;
        do {
            // verify if there is an output point in the segna matrix, this
            // matrix is a copy of the
            // flow matrix, but in the while cycle modify it, and add the point
            // with value equal to
            // 10 (fork) and delete the point already calculated.
            pm.beginTask(msg.message("workingiter") + iterations++, nRows);
            for( int j = 0; j < nRows; j++ ) {
                for( int i = 0; i < nCols; i++ ) {
                    contr = 0;
                    if (segnaIter.getSampleDouble(i, j, 0) == 10) {
                        flow[0] = i;
                        flow[1] = j;
                        contr = 1;
                        // it s really an output point (the segna matrix can be
                        // modified into the
                        // loop).
                        if (flowIter.getSampleDouble(i, j, 0) == 10) {
                            // the output value is setted as 1 in the hack
                            // matrix.
                            hackstreamIter.setSample(i, j, 0, 1);
                        } else if (flowIter.getSampleDouble(i, j, 0) != 10 || !isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                            // after the call to go_downstream the flow is the
                            // next pixel in the channel.
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                throw new ModelsRuntimeException("An error occurred in go_downstream.", this);
                            // this if is true if there is a fork (segna==10 but
                            // m!=10) so add one to the hack number.
                            if (!isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                hackstreamIter.setSample(i, j, 0, hackstreamIter.getSampleDouble(flow[0], flow[1], 0) + 1);
                        }
                        // memorize where the cycle was
                        punto[0] = i;
                        punto[1] = j;
                        break;
                    }
                }
                pm.worked(1);
                if (contr == 1)
                    break;
            }
            pm.done();
            flow[0] = punto[0];
            flow[1] = punto[1];
            if (contr == 1) {
                flow_p[0] = flow[0];
                flow_p[1] = flow[1];
                kk = 0;
                // the flow point is changed in order to follow the drainage
                // direction.
                ModelsEngine.go_upstream_a(flow, flowIter, tcaIter, hacklengthIter, param);
                // the direction
                kk = param[0];
                // number of pixel which drainage into this pixel, N.B. in a
                // channel only one pixel
                // drain into the next (?), otherwise there is a fork
                count = param[1];

                double tmp = 0;
                if (count > 0) {
                    tmp = hackstreamIter.getSampleDouble(punto[0], punto[1], 0);
                    hackstreamIter.setSample(flow[0], flow[1], 0, tmp);
                }
                if (count > 1) {
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(punto[0] + dir[k][1], punto[1] + dir[k][0], 0) == dir[k][2] && k != kk) {
                            segnaIter.setSample(punto[0] + dir[k][1], punto[1] + dir[k][0], 0, 10);
                        }
                    }
                }
                while( count > 0 ) {
                    /* segna altro pixel */
                    flow_p[0] = flow[0];
                    flow_p[1] = flow[1];
                    kk = 0;
                    ModelsEngine.go_upstream_a(flow, flowIter, tcaIter, hacklengthIter, param);
                    kk = param[0];
                    count = param[1];
                    double temp = hackstreamIter.getSampleDouble(punto[0], punto[1], 0);
                    hackstreamIter.setSample(flow[0], flow[1], 0, temp);
                    if (count > 1) {
                        // attribuisco ai nodi che incontro direzione di
                        // drenaggio 10
                        for( int k = 1; k <= 8; k++ ) {
                            if (flowIter.getSample(flow_p[0] + dir[k][1], flow_p[1] + dir[k][0], 0) == dir[k][2] && k != kk) {
                                segnaIter.setSample(flow_p[0] + dir[k][1], flow_p[1] + dir[k][0], 0, 10);
                            }
                        }
                    }
                }
                segnaIter.setSample(punto[0], punto[1], 0, 5);
            }
        } while( contr == 1 );

        outHackstream = CoverageUtilities.buildCoverage("hackstream", hackstreamWR, regionMap, inFlow
                .getCoordinateReferenceSystem());

    }

    /**
     * Gives the channel enumeration of the hydrographic network according to
     * Hack’s enumeration using a fixed network.
     * 
     * @param flowData
     * @param netnum
     * @param hacks
     * @return
     */
    public void hackstreamNetFixed( WritableRandomIter flowIter, RandomIter netnumIter, WritableRandomIter segnaIter ) {
        int contr = 0;
        int count = 0, kk = 0;

        int[] flow = new int[2], param = new int[2];
        int[] flow_p = new int[2];
        int[] punto = new int[2];

        WritableRaster hackstreamWR = createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter hackstreamIter = RandomIterFactory.createWritable(hackstreamWR, null);

        int iterations = 1;
        do {
            pm.beginTask(msg.message("workingiter") + iterations++, nRows); //$NON-NLS-1$
            for( int j = 0; j < nRows; j++ ) {
                for( int i = 0; i < nCols; i++ ) {
                    contr = 0;
                    // check of the drainage directions in the new matrix
                    // lool for the outlet
                    if (segnaIter.getSampleDouble(i, j, 0) == 10) {
                        // marked outlet with its line and column
                        flow[0] = i;
                        flow[1] = j;
                        contr = 1;
                        if (flowIter.getSampleDouble(i, j, 0) == 10) {
                            // set the hack order to 1 corresponding to the
                            // marked outlet
                            hackstreamIter.setSample(i, j, 0, 1);
                            // why these checks if segnaRandomIter is a copy of
                            // flowRandomIter?
                        } else if (flowIter.getSampleDouble(i, j, 0) != 10 && !isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                throw new ModelsRuntimeException("An error occurred in go_downstream.", this);
                            double tmp = 0;
                            if (!isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                tmp = hackstreamIter.getSampleDouble(flow[0], flow[1], 0) + 1;
                            hackstreamIter.setSample(i, j, 0, tmp);
                        }
                        // set punto as flow with row and column number of the
                        // outlet
                        punto[0] = i;
                        punto[1] = j;
                        /*
                         * if (copt != null && copt.isInterrupted()) return
                         * false;
                         */
                        break;
                    }
                }
                // why this check??
                pm.worked(1);
                if (contr == 1)
                    break;
            }
            pm.done();

            flow[0] = punto[0];
            flow[1] = punto[1];
            if (contr == 1) {
                flow_p[0] = flow[0];
                flow_p[1] = flow[1];
                kk = 0;
                ModelsEngine.goUpStreamOnNetFixed(flow, flowIter, netnumIter, param);
                kk = param[0];
                count = param[1];
                double tmp = 0;
                if (count > 0)
                    tmp = hackstreamIter.getSampleDouble(punto[0], punto[1], 0);
                hackstreamIter.setSample(flow[0], flow[1], 0, tmp);
                if (count > 1) {
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(punto[0] + dir[k][1], punto[1] + dir[k][0], 0) == dir[k][2]) {
                            segnaIter.setSample(punto[0] + dir[k][1], punto[1] + dir[k][0], 0, 10);
                        }
                    }
                }
                while( count > 0 && !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0)) ) {
                    /* segnaRandomIter altro pixel */
                    flow_p[0] = flow[0];
                    flow_p[1] = flow[1];
                    kk = 0;
                    ModelsEngine.goUpStreamOnNetFixed(flow, flowIter, netnumIter, param);
                    kk = param[0];
                    count = param[1];
                    tmp = hackstreamIter.getSample(punto[0], punto[1], 0);
                    hackstreamIter.setSample(flow[0], flow[1], 0, tmp);
                    if (count > 1) {
                        // attribuisco ai nodi che incontro direzione di
                        // drenaggio 10
                        for( int k = 1; k <= 8; k++ ) {
                            if (flowIter.getSample(flow_p[0] + dir[k][1], flow_p[1] + dir[k][0], 0) == dir[k][2] && k != kk) {
                                segnaIter.setSample(flow_p[0] + dir[k][1], flow_p[1] + dir[k][0], 0, 10);
                            }
                        }
                    }
                }
                segnaIter.setSample(punto[0], punto[1], 0, 5);
            }
        } while( contr == 1 );

        segnaIter.done();
        int channel;
        double hacksValue = 0;
        int channelLength = 0;
        pm.beginTask("Calculating map...", nRows);
        for( int j = 0; j < nRows; j++ ) {
            for( int i = 0; i < nCols; i++ ) {
                if (!isNovalue(netnumIter.getSample(i, j, 0)) && hackstreamIter.getSampleDouble(i, j, 0) < 0) {
                    channelLength = 1;
                    channel = (int) netnumIter.getSampleDouble(i, j, 0);
                    for( int l = 0; l < nRows; l++ ) {
                        for( int n = 0; n < nCols; n++ ) {
                            if (netnumIter.getSampleDouble(n, l, 0) == channel) {
                                flow[0] = n;
                                flow[1] = l;
                                if (ModelsEngine.sourcesNet(flowIter, flow, channel, netnumIter)) {
                                    punto[0] = flow[0];
                                    punto[1] = flow[1];
                                    ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                                    while( netnumIter.getSampleDouble(flow[0], flow[1], 0) == channel ) {
                                        flow_p[0] = flow[0];
                                        flow_p[1] = flow[1];
                                        ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                                        channelLength++;
                                    }
                                    double tmp = 0.;
                                    tmp = hackstreamIter.getSampleDouble(flow[0], flow[1], 0) + 1;
                                    hackstreamIter.setSample(punto[0], punto[1], 0, tmp);
                                    hacksValue = hackstreamIter.getSampleDouble(punto[0], punto[1], 0);
                                    flow_p[0] = punto[0];
                                    flow_p[1] = punto[1];
                                    int length = 1;
                                    while( netnumIter.getSampleDouble(flow_p[0], flow_p[1], 0) == channel
                                            && length <= channelLength ) {
                                        flow_p[0] = punto[0];
                                        flow_p[1] = punto[1];
                                        hackstreamIter.setSample(punto[0], punto[1], 0, hacksValue);
                                        ModelsEngine.go_downstream(punto, flowIter.getSampleDouble(punto[0], punto[1], 0));
                                        length++;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        outHackstream = CoverageUtilities.buildCoverage("hackstream", hackstreamWR, regionMap, inFlow
                .getCoordinateReferenceSystem());

    }

}
