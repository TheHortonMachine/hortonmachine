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
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Calculates the projection on the plane of the distance of each pixel from the outlet.")
@Documentation("OmsDistanceToOutlet.html")
@Author(name = "Andreis Daniele, Erica Ghesla, Antonello Andrea, Cozzini Andrea, PisoniSilvano, Rigon Riccardo")
@Keywords("Geomorphology, OmsDrainDir")
@Label(JGTConstants.NETWORK)
@Name("d2o")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class OmsDistanceToOutlet extends JGTModel {
    @Description("The map of depitted elevation, if it's null the models work in 2d mode.")
    @In
    public GridCoverage2D inPit = null;
    
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;
    
    @Description("Processing mode, 0= simple mode in meter, 1 = topological distance.")
    @In
    public int pMode;
    
    @Description("The map of the distance to the outlet.")
    @Out
    public GridCoverage2D outDistance = null;

    HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outDistance == null, doReset)) {
            return;

        }
        checkInParameters();

        RandomIter pitIter = null;
        if (inPit != null) {
            pitIter = CoverageUtilities.getRandomIterator(inPit);
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
            ModelsEngine.topologicalOutletdistance(flowIter, pitIter, distanceIter, regionMap, pm);
        }

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    distanceIter.setSample(i, j, 0, JGTConstants.doubleNovalue);
                }
            }
        }
        outDistance = CoverageUtilities.buildCoverage("distanceToOutlet", distanceWR, regionMap,
                inFlow.getCoordinateReferenceSystem());

    }

    /*
     * Verify the input parameters.
     */
    private void checkInParameters() {
        // TODO Auto-generated method stub
        checkNull(inFlow);
        if (pMode < 0 || pMode > 1) {
            String message = msg.message("distancetooutlet.modeOutRange");
            pm.errorMessage(message);
            throw new IllegalArgumentException(message);
        }

    }

}
