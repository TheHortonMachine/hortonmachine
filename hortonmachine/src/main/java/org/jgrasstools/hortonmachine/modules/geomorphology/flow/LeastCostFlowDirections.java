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
package org.jgrasstools.hortonmachine.modules.geomorphology.flow;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.WritableRaster;
import java.util.TreeSet;

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
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Calculates the drainage directions following the least cost method.")
// @Documentation("FlowDirections.html")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Geomorphology, Flowdirections, Least cost")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("flowlc")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LeastCostFlowDirections extends JGTModel {
    @Description("The depitted elevation map.")
    @In
    public GridCoverage2D inElev = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of flowdirections.")
    @Out
    public GridCoverage2D outFlow = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }
        checkNull(inElev);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);
        
        TreeSet<GridNode> orderedNodes = new TreeSet<GridNode>(null);

        for( int c = 0; c < cols; c++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int r = 0; r < rows; r++ ) {
                GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, c, r);
                
                
                double value = elevationIter.getSampleDouble( c,r, 0);
                if (!isNovalue(value)) {
                    pitIter.setSample(r, c, 0, value);
                } else {
                    pitIter.setSample(r, c, 0, PITNOVALUE);
                }
            }
        }

        // outFlow = CoverageUtilities.buildCoverage("flowdirections", transposedFlow, regionMap,
        // inDem.getCoordinateReferenceSystem(), true);
    }

}
