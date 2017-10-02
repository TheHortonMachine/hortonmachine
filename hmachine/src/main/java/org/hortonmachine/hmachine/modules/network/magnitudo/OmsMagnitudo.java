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
package org.hortonmachine.hmachine.modules.network.magnitudo;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.go_downstream;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.isSourcePixel;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMAGNITUDO_outMag_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSMAGNITUDO_DESCRIPTION)
@Author(name = OMSMAGNITUDO_AUTHORNAMES, contact = OMSMAGNITUDO_AUTHORCONTACTS)
@Keywords(OMSMAGNITUDO_KEYWORDS)
@Label(OMSMAGNITUDO_LABEL)
@Name(OMSMAGNITUDO_NAME)
@Status(OMSMAGNITUDO_STATUS)
@License(OMSMAGNITUDO_LICENSE)
public class OmsMagnitudo extends HMModel {

    @Description(OMSMAGNITUDO_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSMAGNITUDO_outMag_DESCRIPTION)
    @Out
    public GridCoverage2D outMag = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMag == null, doReset)) {
            return;
        }
        checkNull(inFlow);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);

        WritableRaster magWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, 0.0);
        if (magWR == null) {
            return;
        } else {
            magnitudo(flowIter, cols, rows, magWR);
            outMag = CoverageUtilities.buildCoverage("mag", magWR, regionMap, inFlow.getCoordinateReferenceSystem());

        }
    }

    public void magnitudo( RandomIter flowIter, int width, int height, WritableRaster magWR ) {

        int[] flow = new int[2];
        // get rows and cols from the active region
        int cols = width;
        int rows = height;
        RandomIter magIter = RandomIterFactory.create(magWR, null);
        pm.beginTask(msg.message("magnitudo.workingon"), rows * 2); //$NON-NLS-1$

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                // looks for the source
                if (isSourcePixel(flowIter, flow[0], flow[1])) {
                    magWR.setSample(flow[0], flow[1], 0, magIter.getSampleDouble(flow[0], flow[1], 0) + 1.0);
                    if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                        return;
                    while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                            && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                        magWR.setSample(flow[0], flow[1], 0, magIter.getSampleDouble(flow[0], flow[1], 0) + 1.0);
                        if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return;
                    }

                    if (flowIter.getSampleDouble(flow[0], flow[1], 0) == 10) {
                        magWR.setSample(flow[0], flow[1], 0, magIter.getSampleDouble(flow[0], flow[1], 0) + 1.0);
                    }
                }
            }
            pm.worked(1);
        }

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (magIter.getSampleDouble(i, j, 0) == 0.0 && flowIter.getSampleDouble(i, j, 0) == 10.0) {
                    magWR.setSample(i, j, 0, 1.0);
                } else if (magIter.getSampleDouble(i, j, 0) == 0.0 && isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    magWR.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

}