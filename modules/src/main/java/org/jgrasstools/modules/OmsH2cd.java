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

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

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
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("It calculates for each hillslope pixel its distance from the river networks, following the steepest descent.")
// @Documentation("OmsTc.html")
@Author(name = "Erica Ghesla, Antonello Andrea, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Hillslope, Outlet, Distance")
@Label(JGTConstants.HILLSLOPE)
@Documentation("OmsH2cd.html")
@Name("_h2cd")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class OmsH2cd extends JGTModel {

    @Description("The map of flowdirections")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The optional map of the elevation used for 3d mode in pMode = 1.")
    @In
    public GridCoverage2D inElev = null;

    @Description("The processing mode (0 = in number of pixels (default), 1 = in meters).")
    @In
    public int pMode = 0;

    @Description("The map of hillslope to channels distance.")
    @Out
    public GridCoverage2D outH2cd = null;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inNet);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RenderedImage netRI = inNet.getRenderedImage();
        RandomIter netIter = RandomIterFactory.create(netRI, null);

        RandomIter elevIter = null;
        if (inElev != null && pMode == 1) {
            RenderedImage elevRI = inElev.getRenderedImage();
            elevIter = RandomIterFactory.create(elevRI, null);
        }

        WritableRaster h2cdWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRandomIter h2cdIter = RandomIterFactory.createWritable(h2cdWR, null);

        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                double value = netIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value))
                    flowIter.setSample(c, r, 0, 10.0);
            }
        }

        if (pMode == 1) {
            ModelsEngine.topologicalOutletdistance(flowIter, elevIter, h2cdIter, regionMap, pm);
        } else {
            ModelsEngine.outletdistance(flowIter, h2cdIter, regionMap, pm);
        }

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (!isNovalue(netIter.getSampleDouble(i, j, 0)) && !isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    h2cdIter.setSample(i, j, 0, 0.0);
                } else if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    h2cdIter.setSample(i, j, 0, JGTConstants.doubleNovalue);
                }
            }
        }

        outH2cd = CoverageUtilities.buildCoverage("h2cd", h2cdWR, regionMap, inFlow.getCoordinateReferenceSystem()); //$NON-NLS-1$
    }
}
