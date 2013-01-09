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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMARKOUTLETS_outFlow_DESCRIPTION;

import java.awt.image.WritableRaster;

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
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description(OMSMARKOUTLETS_DESCRIPTION)
@Author(name = OMSMARKOUTLETS_AUTHORNAMES, contact = OMSMARKOUTLETS_AUTHORCONTACTS)
@Keywords(OMSMARKOUTLETS_KEYWORDS)
@Label(OMSMARKOUTLETS_LABEL)
@Name(OMSMARKOUTLETS_NAME)
@Status(OMSMARKOUTLETS_STATUS)
@License(OMSMARKOUTLETS_LICENSE)
public class OmsMarkoutlets extends JGTModel {
    @Description(OMSMARKOUTLETS_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSMARKOUTLETS_outFlow_DESCRIPTION)
    @Out
    public GridCoverage2D outFlow = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }
        checkNull(inFlow);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        WritableRaster mflowWR = CoverageUtilities.renderedImage2WritableRaster(inFlow.getRenderedImage(), false);
        WritableRandomIter mflowIter = RandomIterFactory.createWritable(mflowWR, null);

        pm.beginTask(msg.message("markoutlets.working"), nRows); //$NON-NLS-1$

        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                FlowNode flowNode = new FlowNode(mflowIter, nCols, nRows, c, r);
                if (flowNode.isValid() && flowNode.isHeadingOutside()) {
                    flowNode.setValueInMap(mflowIter, FlowNode.OUTLET);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outFlow = CoverageUtilities.buildCoverage("markoutlets", mflowWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }

}
