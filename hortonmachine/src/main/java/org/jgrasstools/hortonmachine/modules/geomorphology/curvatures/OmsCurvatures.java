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
package org.jgrasstools.hortonmachine.modules.geomorphology.curvatures;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outPlan_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outProf_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outTang_DESCRIPTION;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;

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
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description(OMSCURVATURES_DESCRIPTION)
@Documentation(OMSCURVATURES_DOCUMENTATION)
@Author(name = OMSCURVATURES_AUTHORNAMES, contact = OMSCURVATURES_AUTHORCONTACTS)
@Keywords(OMSCURVATURES_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name(OMSCURVATURES_NAME)
@Status(OMSCURVATURES_STATUS)
@License(OMSCURVATURES_LICENSE)
public class OmsCurvatures extends JGTModel {
    @Description(OMSCURVATURES_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    // output
    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @Out
    public GridCoverage2D outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @Out
    public GridCoverage2D outPlan = null;

    @Description(OMSCURVATURES_outTang_DESCRIPTION)
    @Out
    public GridCoverage2D outTang = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outProf == null, doReset)) {
            return;
        }
        checkNull(inElev);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);

        WritableRaster profWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRaster planWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRaster tangWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        double plan = 0.0;
        double tang = 0.0;
        double prof = 0.0;
        double disXX = Math.pow(xRes, 2.0);
        double disYY = Math.pow(yRes, 2.0);
        /*
         * calculate curvatures
         */
        pm.beginTask(msg.message("curvatures.calculating"), nRows - 2);
        for( int r = 1; r < nRows - 1; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 1; c < nCols - 1; c++ ) {
                double elevation = elevationIter.getSampleDouble(c, r, 0);
                if (!isNovalue(elevation)) {
                    double elevRplus = elevationIter.getSampleDouble(c, r + 1, 0);
                    double elevRminus = elevationIter.getSampleDouble(c, r - 1, 0);
                    double elevCplus = elevationIter.getSampleDouble(c + 1, r, 0);
                    double elevCminus = elevationIter.getSampleDouble(c - 1, r, 0);
                    /*
                     * first derivate
                     */
                    double sxValue = 0.5 * (elevRplus - elevRminus) / xRes;
                    double syValue = 0.5 * (elevCplus - elevCminus) / yRes;
                    double p = Math.pow(sxValue, 2.0) + Math.pow(syValue, 2.0);
                    double q = p + 1;
                    if (p == 0.0) {
                        plan = 0.0;
                        tang = 0.0;
                        prof = 0.0;
                    } else {
                        double elevCplusRplus = elevationIter.getSampleDouble(c + 1, r + 1, 0);
                        double elevCplusRminus = elevationIter.getSampleDouble(c + 1, r - 1, 0);
                        double elevCminusRplus = elevationIter.getSampleDouble(c - 1, r + 1, 0);
                        double elevCminusRminus = elevationIter.getSampleDouble(c - 1, r - 1, 0);

                        double sxxValue = (elevRplus - 2 * elevation + elevRminus) / disXX;
                        double syyValue = (elevCplus - 2 * elevation + elevCminus) / disYY;
                        double sxyValue = 0.25 * ((elevCplusRplus - elevCplusRminus - elevCminusRplus + elevCminusRminus) / (xRes * yRes));

                        plan = (sxxValue * Math.pow(syValue, 2.0) - 2 * sxyValue * sxValue * syValue + syyValue
                                * Math.pow(sxValue, 2.0))
                                / (Math.pow(p, 1.5));
                        tang = (sxxValue * Math.pow(syValue, 2.0) - 2 * sxyValue * sxValue * syValue + syyValue
                                * Math.pow(sxValue, 2.0))
                                / (p * Math.pow(q, 0.5));
                        prof = (sxxValue * Math.pow(sxValue, 2.0) + 2 * sxyValue * sxValue * syValue + syyValue
                                * Math.pow(syValue, 2.0))
                                / (p * Math.pow(q, 1.5));
                    }

                    profWR.setSample(c, r, 0, prof);
                    tangWR.setSample(c, r, 0, tang);
                    planWR.setSample(c, r, 0, plan);
                }
            }
            pm.worked(1);
        }
        pm.done();

        if (isCanceled(pm)) {
            return;
        }
        outProf = CoverageUtilities.buildCoverage("prof_curvature", profWR, regionMap, inElev.getCoordinateReferenceSystem());
        outPlan = CoverageUtilities.buildCoverage("plan_curvature", planWR, regionMap, inElev.getCoordinateReferenceSystem());
        outTang = CoverageUtilities.buildCoverage("tang_curvature", tangWR, regionMap, inElev.getCoordinateReferenceSystem());
    }
}
