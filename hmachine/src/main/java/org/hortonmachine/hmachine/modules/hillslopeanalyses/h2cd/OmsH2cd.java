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
package org.hortonmachine.hmachine.modules.hillslopeanalyses.h2cd;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_outH2cd_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CD_pMode_DESCRIPTION;

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

@Description(OMSH2CD_DESCRIPTION)
@Author(name = OMSH2CD_AUTHORNAMES, contact = OMSH2CD_AUTHORCONTACTS)
@Keywords(OMSH2CD_KEYWORDS)
@Label(OMSH2CD_LABEL)
@Name(OMSH2CD_NAME)
@Status(OMSH2CD_STATUS)
@License(OMSH2CD_LICENSE)
public class OmsH2cd extends HMModel {

    @Description(OMSH2CD_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSH2CD_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSH2CD_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSH2CD_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSH2CD_outH2cd_DESCRIPTION)
    @Out
    public GridCoverage2D outH2cd = null;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inNet);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RenderedImage netRI = inNet.getRenderedImage();
        RandomIter netIter = RandomIterFactory.create(netRI, null);

        RandomIter elevIter = null;
        if (inElev != null && pMode == 1) {
            RenderedImage elevRI = inElev.getRenderedImage();
            elevIter = RandomIterFactory.create(elevRI, null);
        }

        WritableRaster h2cdWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, 0.0);
        WritableRandomIter h2cdIter = RandomIterFactory.createWritable(h2cdWR, null);

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = netIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value))
                    flowIter.setSample(c, r, 0, 10.0);
            }
        }

        if (pMode == 1) {
            ModelsEngine.topologicalOutletdistance(flowIter, elevIter, h2cdIter, regionMap, pm);
        } else {
            ModelsEngine.outletdistance(flowIter, h2cdIter, regionMap, pm);
        }

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (!isNovalue(netIter.getSampleDouble(i, j, 0)) && !isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    h2cdIter.setSample(i, j, 0, 0.0);
                } else if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    h2cdIter.setSample(i, j, 0, HMConstants.doubleNovalue);
                }
            }
        }

        outH2cd = CoverageUtilities.buildCoverage("h2cd", h2cdWR, regionMap, inFlow.getCoordinateReferenceSystem()); //$NON-NLS-1$
    }
}
