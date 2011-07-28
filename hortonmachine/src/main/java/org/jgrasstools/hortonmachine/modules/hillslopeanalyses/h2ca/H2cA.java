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
package org.jgrasstools.hortonmachine.modules.hillslopeanalyses.h2ca;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Select a hillslope or some of its property from the DEM")
@Documentation("H2cA.html")
@Author(name = "Andreis Daniele,  Erica Ghesla, Antonello Andrea, CozziniAndrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo")
@Keywords("Geomorphology, DrainDir")
@Label(JGTConstants.HILLSLOPE)
@Name("H2cA")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class H2cA extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;
    @Description("The map with the net.")
    @In
    public GridCoverage2D inNet = null;
    @Description("The map of the attribute to estimate.")
    @In
    public GridCoverage2D inAttribute = null;
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
    @Description("The output map of the attribute.")
    @Out
    public GridCoverage2D outAttribute = null;
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outAttribute == null, doReset)) {
            return;
        }
        checkNull(inFlow, inNet, inAttribute);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        WritableRaster attributeWR = h2ca();
        outAttribute = CoverageUtilities.buildCoverage("h2ca", attributeWR, regionMap, inFlow.getCoordinateReferenceSystem());

    }

    /**
     * Calculates the h2ca in every pixel of the map
     * 
     * @return
     */
    private WritableRaster h2ca() {
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        // get rows and cols from the active region
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RandomIter attributeIter = CoverageUtilities.getRandomIterator(inAttribute);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);
        pm.beginTask(msg.message("h2ca.workingon"), -1); //$NON-NLS-1$

        // setting novalue border...
        // FluidUtils.setNovalueBorder(netData);

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (netIter.getSampleDouble(i, j, 0) == 2)
                    flowIter.setSample(i, j, 0, 10);
            }
        }
        netIter.done();
        // create new matrix
        WritableRaster h2caWR = ModelsEngine.go2channel(flowIter, attributeIter, cols, rows, pm);
        WritableRandomIter h2caIter = RandomIterFactory.createWritable(h2caWR, null);

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    h2caIter.setSample(i, j, 0, doubleNovalue);
                }
            }
        }
        pm.done();
        h2caIter.done();
        attributeIter.done();
        flowIter.done();
        return h2caWR;
    }
}
