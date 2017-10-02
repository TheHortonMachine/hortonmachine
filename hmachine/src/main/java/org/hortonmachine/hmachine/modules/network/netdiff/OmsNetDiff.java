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
package org.hortonmachine.hmachine.modules.network.netdiff;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_inRaster_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_inStream_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_outDiff_DESCRIPTION;

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
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSNETDIFF_DESCRIPTION)
@Author(name = OMSNETDIFF_AUTHORNAMES, contact = OMSNETDIFF_AUTHORCONTACTS)
@Keywords(OMSNETDIFF_KEYWORDS)
@Label(OMSNETDIFF_LABEL)
@Name(OMSNETDIFF_NAME)
@Status(OMSNETDIFF_STATUS)
@License(OMSNETDIFF_LICENSE)
public class OmsNetDiff extends HMModel {

    @Description(OMSNETDIFF_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSNETDIFF_inStream_DESCRIPTION)
    @In
    public GridCoverage2D inStream = null;

    @Description(OMSNETDIFF_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSNETDIFF_outDiff_DESCRIPTION)
    @Out
    public GridCoverage2D outDiff = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outDiff == null, doReset)) {
            return;
        }
        checkNull(inFlow, inStream);

        WritableRaster diffWR = netdif();
        if (diffWR == null) {
            return;
        } else {
            HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
            outDiff = CoverageUtilities.buildCoverage("netdiff", diffWR, regionMap, inFlow.getCoordinateReferenceSystem());

        }
    }

    /**
     * Calculates the difference map.
     * 
     * @return
     */
    private WritableRaster netdif() {
        // get rows and cols from the active region
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        int[] flow = new int[2];
        int[] oldflow = new int[2];

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        RandomIter streamIter = CoverageUtilities.getRandomIterator(inStream);
        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        // create new matrix
        double[][] segna = new double[cols][rows];

        pm.beginTask(msg.message("working") + "h.netdif", 3 * rows);
        // First step: It marks with 1 the points which are at the upstream
        // beginning
        // of a link or stream
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                // looks for the source
                if (ModelsEngine.isSourcePixel(flowIter, flow[0], flow[1])) {
                    segna[i][j] = 1;
                } else if (!isNovalue(flowIter.getSampleDouble(i, j, 0)) && flowIter.getSampleDouble(i, j, 0) != 10.0) {
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == dir[k][2]) {
                            if (streamIter.getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == streamIter
                                    .getSampleDouble(i, j, 0)) {
                                segna[i][j] = 0;
                                break;
                            } else {
                                segna[i][j] = 1;
                            }
                        }
                    }
                }
            }
            pm.worked(1);
        }
        WritableRaster diffImage = CoverageUtilities.createWritableRaster(cols, rows, null, inFlow.getRenderedImage()
                .getSampleModel(), null);
        WritableRandomIter diffIter = RandomIterFactory.createWritable(diffImage, null);
        // Second step: It calculate the difference among the first and the last
        // point of a link
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (segna[i][j] > 0) {
                    flow[0] = i;
                    flow[1] = j;
                    oldflow[0] = i;
                    oldflow[1] = j;
                    if (!isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))) {
                        // call go_downstream in FluidUtils
                        ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        while( segna[flow[0]][flow[1]] < 1 && !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && streamIter.getSampleDouble(flow[0], flow[1], 0) == streamIter.getSampleDouble(i, j, 0) ) {
                            oldflow[0] = flow[0];
                            oldflow[1] = flow[1];
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                        }
                        diffIter.setSample(
                                i,
                                j,
                                0,
                                Math.abs(rasterIter.getSampleDouble(i, j, 0)
                                        - rasterIter.getSampleDouble(oldflow[0], oldflow[1], 0)));
                        // Assign to any point inside the link the value of the
                        // difference
                        flow[0] = i;
                        flow[1] = j;
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && streamIter.getSampleDouble(flow[0], flow[1], 0) == streamIter.getSampleDouble(i, j, 0) ) {
                            diffIter.setSample(flow[0], flow[1], 0, diffIter.getSampleDouble(i, j, 0));
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                        }
                        if (flowIter.getSampleDouble(flow[0], flow[1], 0) == 10
                                && streamIter.getSampleDouble(flow[0], flow[1], 0) == streamIter.getSampleDouble(i, j, 0)) {
                            diffIter.setSample(flow[0], flow[1], 0, diffIter.getSampleDouble(i, j, 0));
                        }
                    }
                }
            }
            pm.worked(1);
        }
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (isNovalue(streamIter.getSampleDouble(i, j, 0))) {
                    diffIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        diffIter.done();
        flowIter.done();
        rasterIter.done();
        streamIter.done();
        return diffImage;
    }
}
