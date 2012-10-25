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
package org.jgrasstools.hortonmachine.modules.basin.topindex;

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
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Topographic index calculator.")
@Documentation("TopIndex.html")
@Author(name = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Hydrology")
@Label(JGTConstants.BASIN)
@Name("topindex")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class TopIndex extends JGTModel {

    @Description("The map of the contributing area.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The map of slope.")
    @In
    public GridCoverage2D inSlope = null;

    @Description("The map of the topographic index.")
    @Out
    public GridCoverage2D outTopindex = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outTopindex == null, doReset)) {
            return;
        }
        checkNull(inTca, inSlope);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inTca);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage tcaRI = inTca.getRenderedImage();
        RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);
        RenderedImage slopeRI = inSlope.getRenderedImage();
        RandomIter slopeIter = RandomIterFactory.create(slopeRI, null);

        WritableRaster topindexWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, null);
        WritableRandomIter topindexIter = RandomIterFactory.createWritable(topindexWR, null);

        pm.beginTask(msg.message("topindex.calculating"), nRows);
        for( int j = 0; j < nRows; j++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int i = 0; i < nCols; i++ ) {
                if (isNovalue(tcaIter.getSampleDouble(i, j, 0))) {
                    topindexIter.setSample(i, j, 0, doubleNovalue);
                } else {
                    if (slopeIter.getSampleDouble(i, j, 0) != 0) {
                        topindexIter.setSample(i, j, 0,
                                Math.log(tcaIter.getSampleDouble(i, j, 0) / slopeIter.getSampleDouble(i, j, 0)));
                    } else {
                        topindexIter.setSample(i, j, 0, doubleNovalue);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        outTopindex = CoverageUtilities.buildCoverage("topindex", topindexWR, regionMap, inTca.getCoordinateReferenceSystem());
    }
}
