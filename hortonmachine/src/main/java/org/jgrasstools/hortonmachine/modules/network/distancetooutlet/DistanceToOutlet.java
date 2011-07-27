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
package org.jgrasstools.hortonmachine.modules.network.distancetooutlet;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

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
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("calculates the projection on the plane of the distance of each pixel from the outlet.")
@Documentation("DistanceToOutlet.html")
@Author(name = "Andreis Daniele, Erica Ghesla, Antonello Andrea, Cozzini Andrea, PisoniSilvano, Rigon Riccardo")
@Keywords("Geomorphology, DrainDir")
@Label(JGTConstants.NETWORK)
@Name("D2o")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class DistanceToOutlet extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;
    @Description("Processing mode, 0= simple mode in meter, 1 = topological distance.")
    @In
    public int pMode;
    @Description("The map of the distance to the outlet.")
    @Out
    public GridCoverage2D outDistance = null;
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
    @Execute
    public void process() {
        if (!concatOr(outDistance == null, doReset)) {
            return;
        }
        if (pMode < 0 || pMode > 1) {
            throw new IllegalArgumentException();
        }
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        WritableRaster distanceWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRandomIter distanceIter = CoverageUtilities.getWritableRandomIterator(distanceWR);

        if (pMode == 1) {
            ModelsEngine.outletdistance(flowIter, distanceIter, regionMap, pm);
        } else if (pMode == 0) {
            ModelsEngine.topologicalOutletdistance(flowIter,null, distanceIter, regionMap, pm);
        }
        outDistance = CoverageUtilities.buildCoverage("distanceToOutlet", distanceWR, regionMap,
                inFlow.getCoordinateReferenceSystem());

    }

}
