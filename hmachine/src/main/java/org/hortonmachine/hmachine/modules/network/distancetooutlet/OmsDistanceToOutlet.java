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
package org.hortonmachine.hmachine.modules.network.distancetooutlet;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_inPit_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_outDistance_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_pMode_DESCRIPTION;

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
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSDISTANCETOOUTLET_DESCRIPTION)
@Author(name = OMSDISTANCETOOUTLET_AUTHORNAMES, contact = OMSDISTANCETOOUTLET_AUTHORCONTACTS)
@Keywords(OMSDISTANCETOOUTLET_KEYWORDS)
@Label(OMSDISTANCETOOUTLET_LABEL)
@Name(OMSDISTANCETOOUTLET_NAME)
@Status(OMSDISTANCETOOUTLET_STATUS)
@License(OMSDISTANCETOOUTLET_LICENSE)
public class OmsDistanceToOutlet extends HMModel {

    @Description(OMSDISTANCETOOUTLET_inPit_DESCRIPTION)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSDISTANCETOOUTLET_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSDISTANCETOOUTLET_pMode_DESCRIPTION)
    @In
    public int pMode;

    @Description(OMSDISTANCETOOUTLET_outDistance_DESCRIPTION)
    @Out
    public GridCoverage2D outDistance = null;

    HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outDistance == null, doReset)) {
            return;

        }
        checkInParameters();

        RandomIter pitIter = null;
        if (inPit != null) {
            pitIter = CoverageUtilities.getRandomIterator(inPit);
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        WritableRaster distanceWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, 0.0);
        WritableRandomIter distanceIter = CoverageUtilities.getWritableRandomIterator(distanceWR);

        if (pMode == 1) {
            ModelsEngine.outletdistance(flowIter, distanceIter, regionMap, pm);
        } else if (pMode == 0) {
            ModelsEngine.topologicalOutletdistance(flowIter, pitIter, distanceIter, regionMap, pm);
        }

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    distanceIter.setSample(i, j, 0, HMConstants.doubleNovalue);
                }
            }
        }
        outDistance = CoverageUtilities.buildCoverage("distanceToOutlet", distanceWR, regionMap,
                inFlow.getCoordinateReferenceSystem());

    }

    /*
     * Verify the input parameters.
     */
    private void checkInParameters() {
        // TODO Auto-generated method stub
        checkNull(inFlow);
        if (pMode < 0 || pMode > 1) {
            String message = msg.message("distancetooutlet.modeOutRange");
            pm.errorMessage(message);
            throw new IllegalArgumentException(message);
        }

    }

}
