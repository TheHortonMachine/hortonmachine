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
package org.jgrasstools.hortonmachine.modules.geomorphology.ab;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Calculates the draining area per length unit.")
@Documentation("OmsAb.html")
@Author(name = "Andrea Antonello, Erica Ghesla, Rigon Riccardo, Andrea Cozzini, Silvano Pisoni", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology, OmsTca, OmsCurvatures, OmsDrainDir, OmsFlowDirections")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("ab")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class OmsAb extends JGTModel {
    @Description("The map of the total contributing area.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The map of the planar curvatures.")
    @In
    public GridCoverage2D inPlan = null;

    @Description("The map of area per length.")
    @Out
    public GridCoverage2D outAb = null;

    @Description("The map of contour line.")
    @Out
    public GridCoverage2D outB = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAb == null, doReset)) {
            return;
        }
        checkNull(inTca, inPlan);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inTca);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();
        double xRes = regionMap.getXres();

        RandomIter tcaIter = CoverageUtilities.getRandomIterator(inTca);
        RandomIter planIter = CoverageUtilities.getRandomIterator(inPlan);

        WritableRaster alungWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, null);
        WritableRandomIter alungIter = RandomIterFactory.createWritable(alungWR, null);
        WritableRaster bWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, null);
        WritableRandomIter bIter = RandomIterFactory.createWritable(bWR, null);

        pm.beginTask(msg.message("ab.calculating"), nRows);
        for( int r = 0; r < nRows; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 0; c < nCols; c++ ) {
                double planSample = planIter.getSampleDouble(c, r, 0);
                if (!isNovalue(planSample) && planSample != 0.0) {
                    if (xRes > 1 / planSample && planSample >= 0.0) {
                        bIter.setSample(c, r, 0, 0.1 * xRes);
                    } else if (xRes > Math.abs(1 / planSample) && planSample < 0.0) {
                        bIter.setSample(c, r, 0, xRes + 0.9 * xRes);
                    } else {
                        double bSample = 2 * Math.asin(xRes / (2 * (1 / planSample))) * (1 / planSample - xRes);
                        bIter.setSample(c, r, 0, bSample);
                        if (planSample >= 0.0 && bSample < 0.1 * xRes) {
                            bIter.setSample(c, r, 0, 0.1 * xRes);
                        }
                        if (planSample < 0.0 && bSample > (xRes + 0.9 * xRes)) {
                            bIter.setSample(c, r, 0, xRes + 0.9 * xRes);
                        }
                    }
                }
                if (planSample == 0.0) {
                    bIter.setSample(c, r, 0, xRes);
                }
                alungIter.setSample(c, r, 0, tcaIter.getSampleDouble(c, r, 0) * xRes * xRes / bIter.getSampleDouble(c, r, 0));
                if (isNovalue(planSample)) {
                    alungIter.setSample(c, r, 0, doubleNovalue);
                    bIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outAb = CoverageUtilities.buildCoverage("alung", alungWR, regionMap, inTca.getCoordinateReferenceSystem());
        outB = CoverageUtilities.buildCoverage("b", bWR, regionMap, inTca.getCoordinateReferenceSystem());
    }
}
