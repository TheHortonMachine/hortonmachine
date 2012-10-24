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
package org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet;

import static org.jgrasstools.gears.libs.modules.JGTConstants.*;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

import com.vividsolutions.jts.geom.Coordinate;

@Description("Extract a basin from a map of flowdirections.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "http://www.hydrologis.com")
@Keywords("Dem manipulation, Basin, FlowDirections")
@Label(JGTConstants.DEMMANIPULATION)
@Name("extractbasin")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class ExtractBasin extends JGTModel {
    @Description("The northern coordinate of the watershed outlet.")
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description("The eastern coordinate of the watershed outlet.")
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description("The value for the map (defaults to 1).")
    @In
    public double pValue = 1.0;

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow;

    @Description("The area of the extracted basin.")
    @Out
    public double outArea = 0;

    @Description("The extracted basin mask.")
    @Out
    public GridCoverage2D outBasin = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int ncols;

    private int nrows;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outBasin == null, doReset)) {
            return;
        }
        checkNull(inFlow);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        ncols = regionMap.getCols();
        nrows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();
        double north = regionMap.getNorth();
        double west = regionMap.getWest();
        double south = regionMap.getSouth();
        double east = regionMap.getEast();

        if (pNorth == -1 || pEast == -1) {
            throw new ModelsIllegalargumentException("No outlet coordinates were supplied.", this.getClass().getSimpleName());
        }
        if (pNorth > north || pNorth < south || pEast > east || pEast < west) {
            throw new ModelsIllegalargumentException("The outlet point lies outside the map region.", this.getClass()
                    .getSimpleName());
        }
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        WritableRaster basinWR = CoverageUtilities.createDoubleWritableRaster(ncols, nrows, null, null, doubleNovalue);
        WritableRandomIter basinIter = RandomIterFactory.createWritable(basinWR, null);

        Coordinate outlet = new Coordinate(pEast, pNorth);

        int[] outletColRow = CoverageUtilities.colRowFromCoordinate(outlet, inFlow.getGridGeometry(), null);

        double outletFlow = flowIter.getSampleDouble(outletColRow[0], outletColRow[1], 0);
        if (isNovalue(outletFlow)) {
            throw new IllegalArgumentException("The chosen outlet point doesn't have a valid value.");
        }

        FlowNode runningNode = new FlowNode(flowIter, ncols, nrows, outletColRow[0], outletColRow[1]);
        List<FlowNode> enteringNodes = runningNode.getEnteringNodes();

        pm.beginTask(msg.message("wateroutlet.extracting"), -1);
        while( enteringNodes.size() > 0 ) {
            if (pm.isCanceled()) {
                return;
            }
            List<FlowNode> newEnteringNodes = new ArrayList<FlowNode>();
            for( FlowNode flowNode : enteringNodes ) {
                flowNode.mark(basinIter, pValue);
                outArea++;

                List<FlowNode> newEntering = flowNode.getEnteringNodes();
                if (newEntering.size() > 0)
                    newEnteringNodes.addAll(newEntering);
            }
            enteringNodes = newEnteringNodes;
        }
        pm.done();

        outArea = outArea * xRes * yRes;
        outBasin = CoverageUtilities.buildCoverage("basin", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }

}
