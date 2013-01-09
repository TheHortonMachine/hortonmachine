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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_inToSum_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_outSummed_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_pLowerThres_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_pUpperThres_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;

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
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description(OMSSUMDOWNSTREAM_DESCRIPTION)
@Author(name = OMSSUMDOWNSTREAM_AUTHORNAMES, contact = OMSSUMDOWNSTREAM_AUTHORCONTACTS)
@Keywords(OMSSUMDOWNSTREAM_KEYWORDS)
@Label(OMSSUMDOWNSTREAM_LABEL)
@Name(OMSSUMDOWNSTREAM_NAME)
@Status(OMSSUMDOWNSTREAM_STATUS)
@License(OMSSUMDOWNSTREAM_LICENSE)
public class OmsSumDownStream extends JGTModel {

    @Description(OMSSUMDOWNSTREAM_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSSUMDOWNSTREAM_inToSum_DESCRIPTION)
    @In
    public GridCoverage2D inToSum = null;

    @Description(OMSSUMDOWNSTREAM_pUpperThres_DESCRIPTION)
    @In
    public Double pUpperThres = null;

    @Description(OMSSUMDOWNSTREAM_pLowerThres_DESCRIPTION)
    @In
    public Double pLowerThres = null;

    @Description(OMSSUMDOWNSTREAM_outSummed_DESCRIPTION)
    @Out
    public GridCoverage2D outSummed = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outSummed == null, doReset)) {
            return;
        }

        checkNull(inFlow, inToSum);

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        RandomIter toSumIter = CoverageUtilities.getRandomIterator(inToSum);

        int[] colsRows = CoverageUtilities.getRegionColsRows(inFlow);

        WritableRaster summedWR = ModelsEngine.sumDownstream(flowIter, toSumIter, colsRows[0], colsRows[1], pUpperThres,
                pLowerThres, pm);

        flowIter.done();
        toSumIter.done();

        HashMap<String, Double> params = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        outSummed = CoverageUtilities.buildCoverage("summeddownstream", summedWR, params, inFlow.getCoordinateReferenceSystem()); //$NON-NLS-1$
    }

}
