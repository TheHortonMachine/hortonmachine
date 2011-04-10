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
package org.jgrasstools.hortonmachine.modules.geomorphology.tca;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.text.MessageFormat;
import java.util.HashMap;

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
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Contributing areas represent the areas (in number of pixels) afferent to each point.")
@Documentation("Tca.html")
@Author(name = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology, DrainDir, Tca3D, Ab, Multitca")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("tca")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Tca extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of total contributing areas.")
    @Out
    public GridCoverage2D outTca = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int cols;
    private int rows;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outTca == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);

        pm.message(msg.message("tca.initializematrix"));

        // Initialize new RasterData and set value
        WritableRaster tcaWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);

        area(flowWR, tcaWR);
        outTca = CoverageUtilities.buildCoverage("tca", tcaWR, regionMap, inFlow.getCoordinateReferenceSystem());

    }

    private void area( WritableRaster flowImage, WritableRaster tcaImage ) throws ModelsIOException {

        RandomIter flowIter = RandomIterFactory.create(flowImage, null);
        WritableRandomIter tcaIter = RandomIterFactory.createWritable(tcaImage, null);

        pm.beginTask(msg.message("tca.workingon"), rows);

        int[][] dirs = ModelsSupporter.DIR;

        for( int i = 0; i < rows; i++ ) {
            for( int j = 0; j < cols; j++ ) {
                // get the directions of the current pixel.
                double flowValue = flowIter.getSampleDouble(j, i, 0);
                if (isNovalue(flowValue)) {
                    tcaIter.setSample(j, i, 0, doubleNovalue);
                } else {
                    int rRow = i;
                    int rCol = j;
                    double tcaValue = tcaIter.getSampleDouble(rCol, rRow, 0);
                    while( flowValue < 9 && !isNovalue(flowValue) && flowValue != 0 ) {
                        // it update the value of tca in the next pixel.
                        tcaIter.setSample(rCol, rRow, 0, tcaValue + 1);

                        // it move to the next pixel.
                        int newRow = rRow + dirs[(int) flowValue][0];
                        int newCol = rCol + dirs[(int) flowValue][1];
                        // get the new value of drainage direction.
                        int nextFlow = (int) flowIter.getSampleDouble(newCol, newRow, 0);
                        /*  
                         * 
                         * verify that the next pixel doesn't drain in the previous, otherwise there
                         * is an infinite loop.
                         */
                        if (nextFlow < 9 && (!isNovalue(nextFlow) || nextFlow != 0)) {
                            int r = newRow + dirs[nextFlow][0];
                            int c = newCol + dirs[nextFlow][1];
                            if (r == rRow && c == rCol) {
                                throw new ModelsIOException(MessageFormat.format(
                                        "Detected loop between rows/cols = {0}/{1} and {2}/{3}", rRow, rCol, newRow, newCol),
                                        this);
                            }
                        }
                        // update the indexes.
                        rRow = newRow;
                        rCol = newCol;
                        // memorize the value of the pixel in order to add it to the new pixel.
                        tcaValue = tcaIter.getSampleDouble(rCol, rRow, 0);

                        // extract the new value of drainage direction.
                        flowValue = (int) flowIter.getSampleDouble(rCol, rRow, 0);
                    }
                    if (flowValue == 10) {
                        tcaIter.setSample(rCol, rRow, 0, tcaValue + 1);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        flowIter.done();
        tcaIter.done();
    }
}