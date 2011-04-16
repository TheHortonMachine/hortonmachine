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
package org.jgrasstools.hortonmachine.modules.demmanipulation.markoutlets;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.libs.modules.ModelsEngine.go_downstream;
import static org.jgrasstools.gears.libs.modules.ModelsEngine.isSourcePixel;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Marks all the outlets of the considered region on the drainage directions map with the conventional value 10.")
@Documentation("Markoutlets.html")
@Author(name = "Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Outlets, Dem, Raster, FlowDirections, DrainDir")
@Label(JGTConstants.DEMMANIPULATION)
@Name("markoutlets")
@Status(Status.TESTED)
@License("General Public License Version 3 (GPLv3)")
public class Markoutlets extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of the flowdirections with outlet marked.")
    @Out
    public GridCoverage2D outFlow = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        CoverageUtilities.setNovalueBorder(flowWR);
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);

        WritableRaster mflowWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("markoutlets.working"), 2 * nRows); //$NON-NLS-1$

        int[] punto = new int[2];
        int[] oldpunto = new int[2];
        for( int i = 0; i < nRows; i++ ) {
            for( int j = 0; j < nCols; j++ ) {
                double value = flowIter.getSampleDouble(j, i, 0);
                if (!isNovalue(value)) {
                    mflowWR.setSample(j, i, 0, value);
                } else {
                    mflowWR.setSample(j, i, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }

        for( int i = 0; i < nRows; i++ ) {
            for( int j = 0; j < nCols; j++ ) {
                punto[0] = j;
                punto[1] = i;

                double flowSample = flowIter.getSampleDouble(punto[0], punto[1], 0);

                if (isSourcePixel(flowIter, punto[0], punto[1])) {
                    oldpunto[0] = punto[0];
                    oldpunto[1] = punto[1];

                    while( flowSample < 9.0 && (!isNovalue(flowSample)) ) {
                        oldpunto[0] = punto[0];
                        oldpunto[1] = punto[1];
                        if (!go_downstream(punto, flowSample)) {
                            return;
                        }
                        flowSample = flowIter.getSampleDouble(punto[0], punto[1], 0);

                    }
                    if (flowSample != 10.0)
                        mflowWR.setSample(oldpunto[0], oldpunto[1], 0, 10.0);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outFlow = CoverageUtilities.buildCoverage("markoutlets", mflowWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }

}
