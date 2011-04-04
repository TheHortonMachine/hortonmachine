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
package org.jgrasstools.hortonmachine.modules.statistics.sumdownstream;

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
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Sums the values of a map downstream following the flowdirections.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Sumdownstream, Statistic")
@Label(JGTConstants.STATISTICS)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class SumDownStream extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map to sum.")
    @In
    public GridCoverage2D inToSum = null;

    @Description("The upper threshold.")
    @In
    public Double pUpperThres = null;

    @Description("The lower threshold.")
    @In
    public Double pLowerThres = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of summed values.")
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
