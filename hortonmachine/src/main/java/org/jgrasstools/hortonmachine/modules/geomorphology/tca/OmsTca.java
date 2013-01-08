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

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Calculates the contributing areas that represent the areas (in number of pixels) afferent to each point.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("Geomorphology, OmsDrainDir, Tca3D, OmsAb, Multitca")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("tca")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class OmsTca extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of total contributing areas.")
    @Out
    public GridCoverage2D outTca = null;

    @Description("The vector containing loops, if there are any.")
    @Out
    public SimpleFeatureCollection outLoop = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outTca == null, doReset)) {
            return;
        }
        checkNull(inFlow);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster tcaWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);

        RandomIter flowIter = RandomIterFactory.create(flowRI, null);
        WritableRandomIter tcaIter = RandomIterFactory.createWritable(tcaWR, null);

        pm.beginTask("Calculating tca...", rows); //$NON-NLS-1$
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, cols, rows, c, r);
                if (flowNode.isSource()) {
                    double previousTcaValue = 0.0;
                    while( flowNode != null && flowNode.isValid() ) {
                        int col = flowNode.col;
                        int row = flowNode.row;
                        double tmpTca = tcaIter.getSampleDouble(col, row, 0);
                        double newTcaValue;
                        /*
                         * cumulate only if first time passing, else
                         * just propagate 
                         */
                        if (isNovalue(tmpTca)) {
                            tmpTca = 1.0;
                            newTcaValue = tmpTca + previousTcaValue;
                            previousTcaValue = newTcaValue;
                        } else {
                            newTcaValue = tmpTca + previousTcaValue;
                        }
                        tcaIter.setSample(col, row, 0, newTcaValue);
                        flowNode = flowNode.goDownstream();
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        flowIter.done();
        tcaIter.done();

        outTca = CoverageUtilities.buildCoverage("tca", tcaWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }

}