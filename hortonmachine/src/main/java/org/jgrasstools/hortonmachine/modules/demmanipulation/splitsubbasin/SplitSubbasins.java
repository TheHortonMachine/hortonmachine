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
package org.jgrasstools.hortonmachine.modules.demmanipulation.splitsubbasin;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("A tool for" + " labeling the subbasins of a basin. Given the Hacks number of the channel"
        + " network, the subbasin up to a selected order are labeled. If Hack order 2 was"
        + " selected, the subbasins of Hack order 1 and 2 and the network of the same" + " order are extracted.")
@Author(name = "Erica Ghesla, Rigon Riccardo, Antonello Andrea, Franceschi Silvia, Rigon Riccardo", contact = "http://www.hydrologis.com")
@Keywords("Subbasins, Dem, Raster")
@Label(JGTConstants.DEMMANIPULATION)
@Status(Status.EXPERIMENTAL)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class SplitSubbasins extends JGTModel {
    @Description("The map of flow direction.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of hack.")
    @In
    public GridCoverage2D inHack = null;

    @Description("The map of tca.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The threshold.")
    @In
    public double pThres = 0.0;

    @Description("The hack order.")
    @In
    public Double pHackorder = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of numbered network.")
    @Out
    public GridCoverage2D outNetnum = null;

    @Description("The map of subbasins.")
    @Out
    public GridCoverage2D outSubbasins = null;

    private int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

    @Execute
    public void process() {
        if (!concatOr(outSubbasins == null, doReset)) {
            return;
        }
        checkNull(inFlow, inHack, inTca, pHackorder);

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        RenderedImage hacksRI = inHack.getRenderedImage();
        RenderedImage tcaRI = inTca.getRenderedImage();

        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RandomIter hacksIter = RandomIterFactory.create(hacksRI, null);
        RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);

        WritableRaster netWR = net(hacksIter, tcaIter, pHackorder, pThres, nRows, nCols);
        RandomIter netIter = RandomIterFactory.create(netWR, null);

        WritableRaster netNumberWR = netNumber(flowIter, hacksIter, tcaIter, netIter, pThres, nRows, nCols);
        WritableRandomIter netNumberIter = RandomIterFactory.createWritable(netNumberWR, null);
        WritableRaster subbasinWR = ModelsEngine.extractSubbasins(flowIter, netIter, netNumberIter, nRows, nCols, pm);

        outNetnum = CoverageUtilities.buildCoverage("netnum", netNumberWR, regionMap, inFlow.getCoordinateReferenceSystem()); //$NON-NLS-1$
        outSubbasins = CoverageUtilities.buildCoverage("subbasins", subbasinWR, regionMap, inFlow.getCoordinateReferenceSystem()); //$NON-NLS-1$
    }

    /**
     * Return the map of the network with only the river of the choosen order.
     * 
     * @param hacksIter
     *            the hack stream map.
     * @param tcaIter
     *            the total contribouting area.
     * @param hackOrder
     * @param thresholdValue
     * @param rows
     * @param cols
     * @return the map of the network with the choosen order.
     */
    public WritableRaster net( RandomIter hacksIter, RandomIter tcaIter, double hackOrder, double thresholdValue, int rows,
            int cols ) {
        // calculates the max order of basin (max hackstream value)
        double maxHacksValue = 0.0;
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                double value = hacksIter.getSampleDouble(i, j, 0);
                if (!isNovalue(value)) {
                    if (value > maxHacksValue) {
                        maxHacksValue = value;
                    }
                }
            }
        }
        if (hackOrder > maxHacksValue) {
            throw new ModelsIllegalargumentException("Error on max hackstream", this);
        }

        /*
         * Calculate the new network choosing the stream of n order and area
         * greater than the threshold.
         */
        // create the net map with the value choose.
        WritableRaster netImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(netImage, null);
        pm.beginTask("Extraction of rivers of chosen order...", rows);
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                double hackValue = hacksIter.getSampleDouble(i, j, 0);
                if (!isNovalue(hackValue)) {
                    // calculates the network selecting the streams of 1, 2,..,n
                    // order
                    if (hackValue <= hackOrder) {
                        if (tcaIter.getSampleDouble(i, j, 0) > thresholdValue) {
                            netRandomIter.setSample(i, j, 0, 2);
                        } else {
                            netRandomIter.setSample(i, j, 0, JGTConstants.doubleNovalue);
                        }
                    } else {
                        netRandomIter.setSample(i, j, 0, JGTConstants.doubleNovalue);
                    }
                } else {
                    netRandomIter.setSample(i, j, 0, JGTConstants.doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
        return netImage;
    }

    public WritableRaster netNumber( RandomIter flowIter, RandomIter hacksIter, RandomIter tcaIter, RandomIter netIter,
            double thresholdValue, int rows, int cols ) {
        int gg = 0, n = 0, f;
        int[] flow = new int[2];
        double area = 0.0;
        double[] tcavalue = new double[2];

        WritableRaster netNumberingImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        WritableRandomIter netNumberRandomIter = RandomIterFactory.createWritable(netNumberingImage, null);
        List<Integer> nstream = new ArrayList<Integer>();

        pm.beginTask("Numbering network...", rows);
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                if (netIter.getSampleDouble(i, j, 0) == 2 && flowIter.getSampleDouble(i, j, 0) != 10.0
                        && netNumberRandomIter.getSampleDouble(i, j, 0) == 0.0) {
                    f = 0;
                    // looks for the source
                    for( int k = 1; k <= 8; k++ ) {
                        /* test if neighbor drains in the cell */
                        if (flowIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == dir[k][2]
                                && netIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == 2) {
                            break;
                        } else
                            f++;
                    }
                    // se f=8 nessun pixel appartenete alla rete drena nel pixel
                    // considerato quindi
                    // questo e' sorgente
                    if (f == 8) {
                        n++;
                        nstream.add(n);
                        netNumberRandomIter.setSample(i, j, 0, n);
                        tcavalue[0] = tcaIter.getSampleDouble(i, j, 0);
                        tcavalue[1] = 0.0;
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            throw new ModelsIllegalargumentException("Godownstream failure...", this);
                        // while it is into the network.
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && netNumberRandomIter.getSampleDouble(flow[0], flow[1], 0) == 0 ) {
                            gg = 0;
                            for( int k = 1; k <= 8; k++ ) {
                                // calculate how much pixel drining into the
                                // pixel.
                                if (netIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == 2
                                        && flowIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == dir[k][2]) {
                                    gg++;
                                }
                            }
                            if (gg >= 2) {
                                // the value of the upstream area of the node.
                                for( int k = 1; k <= 8; k++ ) {
                                    if (flowIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == dir[k][2]
                                            && hacksIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == 1) {
                                        if (tcaIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) + tcavalue[1]
                                                - tcavalue[0] > thresholdValue) {
                                            tcavalue[1] = 0;
                                            n++;
                                            nstream.add(n);
                                        }
                                        area = tcaIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) + tcavalue[1];
                                    }
                                }
                            }
                            /*
                             * if there is 2 pixel which are draining in the
                             * same node then increase the order of this pixels.
                             */
                            if (gg >= 2 && (area - tcavalue[0]) > thresholdValue) {
                                // n++;
                                netNumberRandomIter.setSample(flow[0], flow[1], 0, n);
                                tcavalue[0] = tcaIter.getSampleDouble(flow[0], flow[1], 0);
                            }

                            /*
                             * If the pixel is a node and is inside a main
                             * channel (hacks ==1) and the tca which drain in
                             * the previous tract is less than th then I keep
                             * the previuos value.
                             */

                            else if (gg >= 2 && (area - tcavalue[0]) < thresholdValue
                                    && hacksIter.getSampleDouble(flow[0], flow[1], 0) == 1) {
                                netNumberRandomIter.setSample(flow[0], flow[1], 0, n);
                                tcavalue[1] = area - tcavalue[0];
                                tcavalue[0] = tcaIter.getSampleDouble(flow[0], flow[1], 0);
                            }
                            // otherwise cointinuing with the previous number.
                            else {
                                netNumberRandomIter.setSample(flow[0], flow[1], 0, n);
                            }
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                throw new ModelsIllegalargumentException("Godownstream failure...", this);
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        return netNumberingImage;
    }

}
