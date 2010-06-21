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
package org.jgrasstools.hortonmachine.modules.geomorphology.tca;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.text.MessageFormat;
import java.util.HashMap;

import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.i18n.MessageHandler;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("The OMS3 component representation of the tca model. The upslope catchment (or simply contributing) areas represent the planar projection of the areas afferent to a point in the basin. Once the drainage directions have been defined, it is possible to calculate, for each site, the total drainage area afferent to it, indicated as TCA (Total Contributing Area).")
@Author(name = "Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo")
@Bibliography("Take this from the Horton Manual")
@Status(Status.DRAFT)
@License("GPL3")
public class Tca extends JGTModel {
    /*
    * EXTERNAL VARIABLES
    */
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of total contributing areas.")
    @Out
    public GridCoverage2D outTca = null;

    /*
     * INTERNAL VARIABLES
     */
    private MessageHandler msg = MessageHandler.getInstance();

    private static final double NaN = doubleNovalue;

    private int cols;
    private int rows;
    private double xRes;
    private double yRes;

    /**
     * Calculates total contributing areas
     * 
     * @throws Exception
     */
    @Execute
    public void process() throws Exception {
        if (!concatOr(outTca == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);

        pm.message(msg.message("tca.initializematrix"));

        // Initialize new RasterData and set value
        WritableRaster tcaWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        // it contains the analyzed cells
        WritableRaster analyzeWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);

        RandomIter tcaIter = RandomIterFactory.create(tcaWR, null);
        RandomIter flowIter = RandomIterFactory.create(flowWR, null);
        area(flowWR, tcaWR);
        outTca = CoverageUtilities.buildCoverage("tca", tcaWR, regionMap, inFlow.getCoordinateReferenceSystem());

    }

    private void area( WritableRaster flowImage, WritableRaster tcaImage ) {

        int row, col;
        RandomIter flowIter = RandomIterFactory.create(flowImage, null);
        WritableRandomIter tcaIter = RandomIterFactory.createWritable(tcaImage, null);

        pm.beginTask(msg.message("tca.workingon"), rows);

        int[][] dirs = ModelsSupporter.DIR;

        for( int i = 0; i < rows; i++ ) {
            for( int j = 0; j < cols; j++ ) {
                // get the girections of the current pixel.
                int flowValue = (int) flowIter.getSampleDouble(j, i, 0);
                /*
                 * I have put flowValue == 0 because the cast to an int value of a NaN is 0, and 0
                 * is an invalid value for the drainage direction.
                 */

                if (isNovalue(flowValue) || flowValue == 0) {
                    tcaIter.setSample(j, i, 0, NaN);
                } else {
                    int rRow = i;
                    int rCol = j;
                    double tcaValue = tcaIter.getSampleDouble(rCol, rRow, 0);
                    while( flowValue < 9 && !isNovalue(flowValue) && flowValue != 0 ) {
                        // it update the value of tca in the next pixel.
                        tcaIter.setSample(rCol, rRow, 0, tcaValue + 1);

                        // it move to the next pixel.
                        int newRow = rRow + dirs[flowValue][0];
                        int newCol = rCol + dirs[flowValue][1];
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
                            // if (r == rRow && c == rCol) {
                            // throw new ModelsIOException(MessageFormat.format(
                            // "Detected loop between rows/cols = {0}/{1} and {2}/{3}", rRow, rCol,
                            // newRow, newCol),
                            // this);
                            // }
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