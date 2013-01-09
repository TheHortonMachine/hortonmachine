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
package org.jgrasstools.hortonmachine.modules.geomorphology.slope;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_doHandleNegativeSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_outSlope_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.Direction;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description(OMSSLOPE_DESCRIPTION)
@Documentation(OMSSLOPE_DOCUMENTATION)
@Author(name = OMSSLOPE_AUTHORNAMES, contact = OMSSLOPE_AUTHORCONTACTS)
@Keywords(OMSSLOPE_KEYWORDS)
@Label(OMSSLOPE_LABEL)
@Name(OMSSLOPE_NAME)
@Status(OMSSLOPE_STATUS)
@License(OMSSLOPE_LICENSE)
public class OmsSlope extends JGTModel {
    @Description(OMSSLOPE_inPit_DESCRIPTION)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSSLOPE_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSSLOPE_doHandleNegativeSlope_DESCRIPTION)
    @In
    public boolean doHandleNegativeSlope;

    @Description(OMSSLOPE_outSlope_DESCRIPTION)
    @Out
    public GridCoverage2D outSlope = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outSlope == null, doReset)) {
            return;
        }
        checkNull(inPit, inFlow);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage elevationRI = inPit.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);
        RenderedImage flowRI = inFlow.getRenderedImage();
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);

        WritableRaster slopeWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("slope.calculating"), nCols);
        for( int c = 0; c < nCols; c++ ) {
            for( int r = 0; r < nRows; r++ ) {
                double flowValue = flowIter.getSampleDouble(c, r, 0);
                GridNode node = new GridNode(elevationIter, nCols, nRows, xRes, yRes, c, r);
                double value = calculateSlope(node, flowValue);
                if (doHandleNegativeSlope && value < 0) {
                    value = Double.MIN_VALUE;
                }
                slopeWR.setSample(c, r, 0, value);
            }
            pm.worked(1);
        }
        pm.done();

        outSlope = CoverageUtilities.buildCoverage("slope", slopeWR, regionMap, inPit.getCoordinateReferenceSystem());
    }

    /**
     * Calculates the slope of a given flowdirection value in currentCol and currentRow.
     * 
     * @param node the current {@link GridNode}.
     * @param flowValue the value of the flowdirection.
     * @return
     */
    public static double calculateSlope( GridNode node, double flowValue ) {
        double value = doubleNovalue;
        if (!isNovalue(flowValue)) {
            int flowDir = (int) flowValue;
            if (flowDir != 10) {
                Direction direction = Direction.forFlow(flowDir);
                double distance = direction.getDistance(node.xRes, node.yRes);
                double currentElevation = node.elevation;
                double nextElevation = node.getElevationAt(direction);
                value = (currentElevation - nextElevation) / distance;
            }
        }
        return value;
    }

}
