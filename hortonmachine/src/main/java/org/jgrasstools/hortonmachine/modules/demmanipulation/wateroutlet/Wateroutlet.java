/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.i18n.MessageHandler;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IHMProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
/**
 * <p>
 * The openmi compliant representation of the wateroutlet model. Generates a watershed basin from a
 * drainage direction map and a set of coordinates representing the outlet point of watershed.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the map of the drainage directions (-flow)</LI>
 * <LI>the coordinates of the water outlet (-north, -east)</LI>
 * </OL>
 * </DD>
 * <DT><STRONG>Returns:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the basin extracted mask (-basin)</LI>
 * <LI>a choosen map cutten on the basin mask (the name assigned is input.mask) (-trim)</LI>
 * </OL>
 * <P>
 * </DD>
 * Usage: h.wateroutlet --igrass-map map --igrass-flow flow --ograss-basin basin --ograss-trim trim
 * --north north --east east
 * </p>
 * <p>
 * Note: The most important thing in this module is to choose a good water outlet.
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini Andrea, Franceschi
 *         Silvia, Pisoni Silvano, Rigon Riccardo; Originally by Charles Ehlschlaeger, U.S. Army
 *         Construction Engineering Research Laboratory.
 */
public class Wateroutlet extends JGTModel {
    @Description("The northern coordinate of the watershed outlet.")
    @In
    public double pNorth = -1.0;

    @Description("The eastern coordinate of the watershed outlet.")
    @In
    public double pEast = -1.0;

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("The extracted basin mask.")
    @Out
    public GridCoverage2D outBasin = null;

    private MessageHandler msg = MessageHandler.getInstance();

    private int total;

    private int[] pt_seg = new int[1];
    private int[] ba_seg = new int[1];
    private final int RAMSEGBITS = 4;
    private final int DOUBLEBITS = 8; /* 2 * ramsegbits */
    private final int SEGLENLESS = 15; /* 2 ^ ramsegbits - 1 */

    // /private double[][] flowData = null;

    private double[] drain_ptrs = null;

    private double[] bas_ptrs = null;

    private int ncols;

    private int nrows;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outBasin == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities
                .getRegionParamsFromGridCoverage(inFlow);
        ncols = regionMap.get(CoverageUtilities.COLS).intValue();
        nrows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);
        double north = regionMap.get(CoverageUtilities.NORTH);
        double west = regionMap.get(CoverageUtilities.WEST);

        if (pNorth == -1 || pEast == -1) {
            throw new ModelsIllegalargumentException("No outlet coordinates were supplied.", this
                    .getClass().getSimpleName());
        }
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        WritableRaster basinWR = CoverageUtilities.createDoubleWritableRaster(ncols, nrows, null,
                null, null);
        WritableRandomIter basinIter = RandomIterFactory.createWritable(basinWR, null);

        total = nrows * ncols;
        drain_ptrs = new double[size_array(pt_seg, nrows, ncols)];
        // bas = (CELL *) G_calloc (size_array (&ba_seg, nrows, ncols),
        // sizeof(CELL));
        bas_ptrs = new double[size_array(ba_seg, nrows, ncols)];

        pm.beginTask(msg.message("wateroutlet.extracting"), 2 * nrows);
        for( int r = 0; r < nrows; r++ ) {
            for( int c = 0; c < ncols; c++ ) {
                // adapt to the grass drainagedirection format "grass
                // flow=(fluidturtle flow-1)"
                if (isNovalue(flowIter.getSampleDouble(c, r, 0))
                        || flowIter.getSampleDouble(c, r, 0) == 0) {
                    flowIter.setSample(c, r, 0, -1.0);
                } else if (flowIter.getSampleDouble(c, r, 0) == 1.0) {
                    flowIter.setSample(c, r, 0, 8.0);
                } else if (!isNovalue(flowIter.getSampleDouble(c, r, 0))) {
                    flowIter.setSample(c, r, 0, flowIter.getSample(c, r, 0) - 1);

                }
                if (flowIter.getSampleDouble(c, r, 0) == 0.0) {
                    total--;
                }
                drain_ptrs[seg_index(pt_seg, r, c)] = flowIter.getSample(c, r, 0);
                // out.println("DRAIN_PTRS = " +
                // drain_ptrs[seg_index(pt_seg, r, c)]);

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
                if (isNovalue(flowIter.getSampleDouble(c, r, 0))
                        || basinIter.getSampleDouble(c, r, 0) == 0.0) {
                    basinIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outBasin = CoverageUtilities.buildCoverage("basin", basinWR, regionMap, inFlow
                .getCoordinateReferenceSystem());
    }

    private int size_array( int[] ram_seg, int nrows, int ncols ) {
        int size, segs_in_col;

        segs_in_col = ((nrows - 1) >> RAMSEGBITS) + 1;
        ram_seg[0] = ((ncols - 1) >> RAMSEGBITS) + 1;
        size = ((((nrows - 1) >> RAMSEGBITS) + 1) << RAMSEGBITS)
                * ((((ncols - 1) >> RAMSEGBITS) + 1) << RAMSEGBITS);
        size -= ((segs_in_col << RAMSEGBITS) - nrows) << RAMSEGBITS;
        size -= (ram_seg[0] << RAMSEGBITS) - ncols;
        return (size);
    }

    private int seg_index( int[] s, int r, int c ) {
        int value = ((((r) >> RAMSEGBITS) * (s[0]) + (((c) >> RAMSEGBITS)) << DOUBLEBITS)
                + (((r) & SEGLENLESS) << RAMSEGBITS) + ((c) & SEGLENLESS));

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

                        if ((value == draindir[rr][cc])
                                && (bas_ptrs[seg_index(ba_seg, r, c)] == 0.0)) {
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
