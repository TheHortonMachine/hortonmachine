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
package org.hortonmachine.hmachine.modules.geomorphology.ab;
import static org.hortonmachine.gears.libs.modules.HMConstants.GEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_DOCUMENTATION;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_KEYWORDS;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_LABEL;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_LICENSE;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_NAME;
import static org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb.OMSAB_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

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

@Description(OMSAB_DESCRIPTION)
@Documentation(OMSAB_DOCUMENTATION)
@Author(name = OMSAB_AUTHORNAMES, contact = OMSAB_AUTHORCONTACTS)
@Keywords(OMSAB_KEYWORDS)
@Label(OMSAB_LABEL)
@Name(OMSAB_NAME)
@Status(OMSAB_STATUS)
@License(OMSAB_LICENSE)
public class OmsAb extends GridMultiProcessing {
    @Description(OMSAB_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSAB_inPlan_DESCRIPTION)
    @In
    public GridCoverage2D inPlan = null;

    @Description(OMSAB_outAb_DESCRIPTION)
    @Out
    public GridCoverage2D outAb = null;

    @Description(OMSAB_outB_DESCRIPTION)
    @Out
    public GridCoverage2D outB = null;

    public static final String OMSAB_DESCRIPTION = "Calculates the draining area per length unit.";
    public static final String OMSAB_DOCUMENTATION = "OmsAb.html";
    public static final String OMSAB_KEYWORDS = "Geomorphology, OmsTca, OmsCurvatures, OmsDrainDir, OmsFlowDirections";
    public static final String OMSAB_LABEL = GEOMORPHOLOGY;
    public static final String OMSAB_NAME = "ab";
    public static final int OMSAB_STATUS = 40;
    public static final String OMSAB_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSAB_AUTHORNAMES = "Andrea Antonello, Erica Ghesla, Rigon Riccardo, Andrea Cozzini, Silvano Pisoni";
    public static final String OMSAB_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSAB_inTca_DESCRIPTION = "The map of the total contributing area.";
    public static final String OMSAB_inPlan_DESCRIPTION = "The map of the planar curvatures.";
    public static final String OMSAB_outAb_DESCRIPTION = "The map of area per length.";
    public static final String OMSAB_outB_DESCRIPTION = "The map of contour line.";

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

        WritableRaster alungWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, null);
        WritableRandomIter alungIter = RandomIterFactory.createWritable(alungWR, null);
        WritableRaster bWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, null);
        WritableRandomIter bIter = RandomIterFactory.createWritable(bWR, null);

        try {
            pm.beginTask(msg.message("ab.calculating"), nRows * nCols);
            processGrid(nCols, nRows, ( c, r ) -> {
                if (pm.isCanceled()) {
                    return;
                }

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
                alungIter.setSample(c, r, 0, tcaIter.getSample(c, r, 0) * xRes * xRes / bIter.getSampleDouble(c, r, 0));
                if (isNovalue(planSample)) {
                    alungIter.setSample(c, r, 0, doubleNovalue);
                    bIter.setSample(c, r, 0, doubleNovalue);
                }

                pm.worked(1);
            });
            pm.done();
        } finally {
            tcaIter.done();
            planIter.done();
            alungIter.done();
            bIter.done();
        }
        outAb = CoverageUtilities.buildCoverage("alung", alungWR, regionMap, inTca.getCoordinateReferenceSystem());
        outB = CoverageUtilities.buildCoverage("b", bWR, regionMap, inTca.getCoordinateReferenceSystem());
    }
}
