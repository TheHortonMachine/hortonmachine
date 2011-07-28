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

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Extract the watershed for a defined outlet.")
@Documentation("Wateroutlet.html")
@Author(name = "Andrea Antonello, US Army Construction Engineering Research Laboratory", contact = "http://www.hydrologis.com")
@Keywords("Dem manipulation, Geomorphology, FlowDirections")
@Label(JGTConstants.DEMMANIPULATION)
@Name("wateroutlet")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Wateroutlet extends JGTModel {
    @Description("The northern coordinate of the watershed outlet.")
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description("The eastern coordinate of the watershed outlet.")
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The extracted basin mask.")
    @Out
    public GridCoverage2D outBasin = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int total;

    private int[] pt_seg = new int[1];
    private int[] ba_seg = new int[1];
    private static final int RAMSEGBITS = 4;
    private static final int DOUBLEBITS = 8; /* 2 * ramsegbits */
    private static final int SEGLENLESS = 15; /* 2 ^ ramsegbits - 1 */

    private double[] drain_ptrs = null;

    private double[] bas_ptrs = null;

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

        WritableRaster basinWR = CoverageUtilities.createDoubleWritableRaster(ncols, nrows, null, null, null);
        WritableRandomIter basinIter = RandomIterFactory.createWritable(basinWR, null);

        total = nrows * ncols;
        drain_ptrs = new double[size_array(pt_seg, nrows, ncols)];
        // bas = (CELL *) G_calloc (size_array (&ba_seg, nrows, ncols),
        // sizeof(CELL));
        bas_ptrs = new double[size_array(ba_seg, nrows, ncols)];

        pm.beginTask(msg.message("wateroutlet.extracting"), 2 * nrows);
        for( int r = 0; r < nrows; r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = 0; c < ncols; c++ ) {
                // adapt to the grass drainagedirection format "grass
                // flow=(fluidturtle flow-1)"
                double flowValue = flowIter.getSampleDouble(c, r, 0);
                if (isNovalue(flowValue) || flowValue == 0) {
                    flowIter.setSample(c, r, 0, -1.0);
                } else if (flowValue == 1.0) {
                    flowIter.setSample(c, r, 0, 8.0);
                } else if (!isNovalue(flowValue)) {
                    flowIter.setSample(c, r, 0, flowValue - 1);
                }
                if (flowValue == 0.0) {
                    total--;
                }
                drain_ptrs[seg_index(pt_seg, r, c)] = flowIter.getSample(c, r, 0);
            }
            pm.worked(1);
        }

        int row = (int) ((north - pNorth) / yRes);
        int col = (int) ((pEast - west) / xRes);

        if (row >= 0 && col >= 0 && row < nrows && col < ncols)
            overland_cells(row, col);

        for( int r = 0; r < nrows; r++ ) {
            for( int c = 0; c < ncols; c++ ) {
                basinIter.setSample(c, r, 0, bas_ptrs[seg_index(ba_seg, r, c)]);
                if (isNovalue(flowIter.getSampleDouble(c, r, 0)) || basinIter.getSampleDouble(c, r, 0) == 0.0) {
                    basinIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outBasin = CoverageUtilities.buildCoverage("basin", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }

    private int size_array( int[] ram_seg, int nrows, int ncols ) {
        int size, segs_in_col;

        segs_in_col = ((nrows - 1) >> RAMSEGBITS) + 1;
        ram_seg[0] = ((ncols - 1) >> RAMSEGBITS) + 1;
        size = ((((nrows - 1) >> RAMSEGBITS) + 1) << RAMSEGBITS) * ((((ncols - 1) >> RAMSEGBITS) + 1) << RAMSEGBITS);
        size -= ((segs_in_col << RAMSEGBITS) - nrows) << RAMSEGBITS;
        size -= (ram_seg[0] << RAMSEGBITS) - ncols;
        return (size);
    }

    private int seg_index( int[] s, int r, int c ) {
        int value = ((((r) >> RAMSEGBITS) * (s[0]) + (((c) >> RAMSEGBITS)) << DOUBLEBITS) + (((r) & SEGLENLESS) << RAMSEGBITS) + ((c) & SEGLENLESS));

        return value;
    }

    private void overland_cells( int row, int col ) {
        int r, rr, c, cc, num_cells, size_more;
        double value;
        double[][] draindir = {{7, 6, 5}, {8, -17, 4}, {1, 2, 3}};

        if (nrows > ncols) {
            size_more = nrows;
        } else {
            size_more = ncols;
        }

        // OneCell[] Acells = new OneCell[size_more];
        int[] AcellsR = new int[nrows * ncols];
        int[] AcellsC = new int[nrows * ncols];
        // OneCell Acells = new OneCell(nrows * ncols);

        // Acells = (ONE_CELL *) G_malloc (size_more * sizeof(ONE_CELL));
        num_cells = 1;
        AcellsR[0] = row;
        AcellsC[0] = col;
        while( num_cells != 0 ) {
            num_cells--;
            // out.println(" num_cell = " + num_cells);
            row = AcellsR[num_cells];
            col = AcellsC[num_cells];
            bas_ptrs[seg_index(ba_seg, row, col)] = 1.0;
            for( r = row - 1, rr = 0; r <= row + 1; r++, rr++ ) {
                for( c = col - 1, cc = 0; c <= col + 1; c++, cc++ ) {
                    if (r >= 0 && c >= 0 && r < nrows && c < ncols) {
                        value = drain_ptrs[seg_index(pt_seg, r, c)];

                        /*
                         * out.println("value == drain -> " + value + " == " + draindir[rr][cc] + " &&
                         * bas_ptrs == 0.0 -> " + bas_ptrs[seg_index(ba_seg, r, c)] + " == 0.0");
                         */

                        if ((value == draindir[rr][cc]) && (bas_ptrs[seg_index(ba_seg, r, c)] == 0.0)) {
                            if (num_cells == size_more) {
                                System.out.println("AAAAAAAAAAAARRRRRRRRRRRGGGGGGGGGGGHHHHHHHH");
                            }
                            AcellsR[num_cells] = r;
                            AcellsC[num_cells++] = c;
                        }
                    }
                }
            }
        }
    }
}
