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
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Select a hillslope or some of its property from the DEM")
@Documentation("H2cA.html")
@Author(name = "Andreis Daniele,  Erica Ghesla, Antonello Andrea, CozziniAndrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo")
@Keywords("Geomorphology, DrainDir")
@Label(JGTConstants.HILLSLOPE)
@Name("H2cA")
@Status(Status.CERTIFIED)
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

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RandomIter attributeIter = CoverageUtilities.getRandomIterator(inAttribute);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);

        pm.beginTask("Marking the network...", rows); //$NON-NLS-1$
        /*
         * mark network as outlet, in order to easier stop on the net 
         * while going downstream
         */
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (netIter.getSampleDouble(i, j, 0) == FlowNode.NETVALUE)
                    flowIter.setSample(i, j, 0, FlowNode.OUTLET);
            }
            pm.worked(1);
        }
        pm.done();
        netIter.done();

        WritableRaster h2caWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);
        WritableRandomIter h2caIter = RandomIterFactory.createWritable(h2caWR, null);

        ModelsEngine.markHillSlopeWithLinkValue(flowIter, attributeIter, h2caIter, cols, rows, pm);

        h2caIter.done();
        attributeIter.done();
        flowIter.done();

        outAttribute = CoverageUtilities.buildCoverage("h2ca", h2caWR, regionMap, inFlow.getCoordinateReferenceSystem());

    }

}
