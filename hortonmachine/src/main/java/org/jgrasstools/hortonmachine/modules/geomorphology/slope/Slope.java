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

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Calculates the slope in each point of the map.")
@Documentation("Slope.html")
@Author(name = "Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology, Gradient")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("slope")
@Status(Status.TESTED)
@License("General Public License Version 3 (GPLv3)")
public class Slope extends JGTModel {
    /*
     * EXTERNAL VARIABLES
     */
    // input
    @Description("The depitted elevation map.")
    @In
    public GridCoverage2D inElev = null;

    @Description("The map of flowdirection.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of gradient.")
    @Out
    public GridCoverage2D outSlope = null;

    /*
     * INTERNAL VARIABLES
     */
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outSlope == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        int[][] DIR = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        RenderedImage elevationRI = inElev.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);
        RenderedImage flowRI = inFlow.getRenderedImage();
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);

        WritableRaster slopeWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        int[] point = new int[2];

        // grid contains the dimension of pixels according with flow directions
        double[] grid = new double[11];

        grid[0] = grid[9] = grid[10] = 0;
        grid[1] = grid[5] = abs(xRes);
        grid[3] = grid[7] = abs(yRes);
        grid[2] = grid[4] = grid[6] = grid[8] = sqrt(xRes * xRes + yRes * yRes);
        // Calculates the slope along the flow directions of elevation field, if
        // a pixel is on the border its value will be equal to novalue

        pm.beginTask(msg.message("slope.calculating"), nRows);
        for( int c = 0; c < nCols; c++ ) {
            for( int r = 0; r < nRows; r++ ) {
                int flowDir = (int) flowIter.getSampleDouble(c, r, 0);
                if (flowDir == 10) {
                    pm.errorMessage(msg.message("slope.outleterror"));
                }
                double value = doubleNovalue;
                if (!isNovalue(flowDir)) {
                    point[0] = c + DIR[flowDir][1];
                    point[1] = r + DIR[flowDir][0];
                    value = (elevationIter.getSampleDouble(c, r, 0) - elevationIter.getSampleDouble(point[0], point[1], 0))
                            / grid[flowDir];
                }
                slopeWR.setSample(c, r, 0, value);
            }
            pm.worked(1);
        }
        pm.done();

        outSlope = CoverageUtilities.buildCoverage("slope", slopeWR, regionMap, inElev.getCoordinateReferenceSystem());
    }

}
