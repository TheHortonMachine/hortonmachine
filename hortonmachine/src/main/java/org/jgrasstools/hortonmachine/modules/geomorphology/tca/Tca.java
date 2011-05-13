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
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Calculates the contributing areas that represent the areas (in number of pixels) afferent to each point.")
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

        pm.message(msg.message("tca.initializematrix"));

        // Initialize new RasterData and set value
        WritableRaster oneValuesWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 1.0);
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);
        WritableRandomIter oneValuesIter = RandomIterFactory.createWritable(oneValuesWR, null);
        WritableRaster tcaWR = ModelsEngine.sumDownstream(flowIter, oneValuesIter, cols, rows, null, null, pm);
        outTca = CoverageUtilities.buildCoverage("tca", tcaWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }

}