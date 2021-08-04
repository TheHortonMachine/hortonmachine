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
package org.hortonmachine.hmachine.modules.demmanipulation.splitsubbasin;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_inHack_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_outNetnum_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_outSubbasins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_pHackorder_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

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
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

@Description(OMSSPLITSUBBASINS_DESCRIPTION)
@Author(name = OMSSPLITSUBBASINS_AUTHORNAMES, contact = OMSSPLITSUBBASINS_AUTHORCONTACTS)
@Keywords(OMSSPLITSUBBASINS_KEYWORDS)
@Label(OMSSPLITSUBBASINS_LABEL)
@Name(OMSSPLITSUBBASINS_NAME)
@Status(OMSSPLITSUBBASINS_STATUS)
@License(OMSSPLITSUBBASINS_LICENSE)
public class OmsSplitSubbasins extends HMModel {
    @Description(OMSSPLITSUBBASINS_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSSPLITSUBBASINS_inHack_DESCRIPTION)
    @In
    public GridCoverage2D inHack = null;

    @Description(OMSSPLITSUBBASINS_pHackorder_DESCRIPTION)
    @In
    public Double pHackorder = null;

    @Description(OMSSPLITSUBBASINS_outNetnum_DESCRIPTION)
    @Out
    public GridCoverage2D outNetnum = null;

    @Description(OMSSPLITSUBBASINS_outSubbasins_DESCRIPTION)
    @Out
    public GridCoverage2D outSubbasins = null;

    private int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

    private int nCols;
    private int nRows;
    private double hackOrder;

    @Execute
    public void process() {
        if (!concatOr(outSubbasins == null, doReset)) {
            return;
        }
        checkNull(inFlow, inHack, pHackorder);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        nCols = regionMap.getCols();
        nRows = regionMap.getRows();

        hackOrder = pHackorder;

        int flowNovalue = HMConstants.getIntNovalue(inFlow);
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        RenderedImage hacksRI = inHack.getRenderedImage();
        WritableRaster hackWR = CoverageUtilities.renderedImage2WritableRaster(hacksRI, true);

        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        WritableRandomIter hacksIter = RandomIterFactory.createWritable(hackWR, null);

        double outNv = HMConstants.doubleNovalue;
        WritableRaster netImage = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, outNv);
        WritableRandomIter netIter = RandomIterFactory.createWritable(netImage, null);
        net(hacksIter, netIter);

        WritableRaster netNumberWR = netNumber(flowIter, hacksIter, netIter);
        WritableRandomIter netNumberIter = RandomIterFactory.createWritable(netNumberWR, null);
        WritableRaster subbasinWR = ModelsEngine.extractSubbasins(flowIter, flowNovalue, netIter, netNumberIter, nRows, nCols,
                pm);

        outNetnum = CoverageUtilities.buildCoverageWithNovalue("netnum", netNumberWR, regionMap, //$NON-NLS-1$
                inFlow.getCoordinateReferenceSystem(), outNv);
        outSubbasins = CoverageUtilities.buildCoverageWithNovalue("subbasins", subbasinWR, regionMap, //$NON-NLS-1$
                inFlow.getCoordinateReferenceSystem(), outNv);
    }
    /**
     * Return the map of the network with only the river of the choosen order.
     * 
     * @param hacksIter the hack stream map.
     * @param netIter the network map to build on the required hack orders.
     * @return the map of the network with the choosen order.
     */
    private void net( WritableRandomIter hacksIter, WritableRandomIter netIter ) {
        // calculates the max order of basin (max hackstream value)
        pm.beginTask("Extraction of rivers of chosen order...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                double value = hacksIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value)) {
                    /*
                     * if the hack value is in the asked range 
                     * => keep it as net
                     */
                    if (value <= hackOrder) {
                        netIter.setSample(c, r, 0, 2);
                    } else {
                        hacksIter.setSample(c, r, 0, HMConstants.doubleNovalue);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    private WritableRaster netNumber( RandomIter flowIter, RandomIter hacksIter, RandomIter netIter ) {
        int drainingPixelNum = 0;
        int[] flowColRow = new int[2];

        WritableRaster netNumberingImage = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, 0.0);
        WritableRandomIter netNumberRandomIter = RandomIterFactory.createWritable(netNumberingImage, null);

        int n = 0;
        pm.beginTask("Numbering network...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                flowColRow[0] = c;
                flowColRow[1] = r;
                if (!isNovalue(netIter.getSampleDouble(c, r, 0)) && flowIter.getSampleDouble(c, r, 0) != 10.0
                        && NumericsUtilities.dEq(netNumberRandomIter.getSampleDouble(c, r, 0), 0.0)) {

                    boolean isSource = true;
                    for( int k = 1; k <= 8; k++ ) {
                        boolean isDraining = flowIter.getSampleDouble(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0],
                                0) == dir[k][2];
                        boolean isOnNet = !isNovalue(
                                netIter.getSampleDouble(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0], 0));
                        if (isDraining && isOnNet) {
                            isSource = false;
                            break;
                        }
                    }

                    /*
                     * if it is source pixel, go down
                     */
                    if (isSource) {
                        n++;
                        netNumberRandomIter.setSample(c, r, 0, n);
                        if (!ModelsEngine.go_downstream(flowColRow, flowIter.getSampleDouble(flowColRow[0], flowColRow[1], 0)))
                            throw new ModelsIllegalargumentException("go_downstream failure...", this, pm);
                        /*
                         * while it is on the network, go downstream
                         */
                        while( !isNovalue(flowIter.getSampleDouble(flowColRow[0], flowColRow[1], 0))
                                && netNumberRandomIter.getSampleDouble(flowColRow[0], flowColRow[1], 0) == 0 ) {
                            /*
                             * calculate how many pixels drain into the current pixel.
                             */
                            drainingPixelNum = 0;
                            for( int k = 1; k <= 8; k++ ) {
                                if (!isNovalue(netIter.getSampleDouble(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0], 0))
                                        && flowIter.getSampleDouble(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0],
                                                0) == dir[k][2]) {
                                    drainingPixelNum++;
                                }
                            }

                            if (drainingPixelNum > 1) {
                                n++;
                            }
                            netNumberRandomIter.setSample(flowColRow[0], flowColRow[1], 0, n);

                            if (!ModelsEngine.go_downstream(flowColRow,
                                    flowIter.getSampleDouble(flowColRow[0], flowColRow[1], 0)))
                                throw new ModelsIllegalargumentException("go_downstream failure...", this, pm);
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
