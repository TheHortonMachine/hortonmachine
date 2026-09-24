/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.demmanipulation.pitfiller;

import static org.hortonmachine.gears.libs.modules.FlowNode.NETVALUE;
import static org.hortonmachine.gears.libs.modules.HMConstants.DEMMANIPULATION;

import java.awt.image.DataBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTile;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTiledWriter;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTiledWriter.TiledOutput;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes.PriorityFloodAccumulation;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes.PriorityFloodFlow;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes.PriorityFloodGraph;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes.PriorityFloodTile;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes.PriorityFloodTile.FloodResult;

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

/**
 * Parallel Priority-Flood depression filling.
 *
 * <p>Implementation of: Barnes, R., 2016. Parallel priority-flood depression filling for trillion cell
 * digital elevation models on desktops or clusters. Computers & Geosciences 96, 56–68.
 * https://doi.org/10.1016/j.cageo.2016.07.001</p>
 *
 * <p>The DEM is split into tiles and the algorithm works in three phases:</p>
 * <ol>
 *  <li><b>tiles in parallel:</b> each tile is flooded with a labelling Priority-Flood seeded from the
 *      tile perimeter and from the cells that touch novalues (or the DEM border). Every seed that is
 *      popped without a label starts a new watershed label. Where two labels touch, a spill edge
 *      with the elevation needed to pass from one to the other is recorded, as is the edge towards
 *      the "ocean" for cells that drain outside the valid data.</li>
 *  <li><b>sequential:</b> the label graphs of all tiles are joined along the tile seams and the
 *      minimum spill elevation of each label towards the ocean is solved with a minimax
 *      Dijkstra over the (small) label graph.</li>
 *  <li><b>tiles in parallel:</b> each cell is raised to the spill elevation of its label.</li>
 * </ol>
 *
 * <p>Two modes are available:</p>
 * <ul>
 *  <li><b>in memory</b> (default): coverage in, coverage out. The labels of the first phase are
 *      kept in memory for the last one.</li>
 *  <li><b>large file</b> ({@link #doLargeFile}): GeoTIFF file in, GeoTIFF file out, for DEMs that
 *      do not fit in memory. Tiles are decoded in parallel from the file, only their perimeters
 *      are kept after the first phase, and they are flooded again in the last phase ("evict"
 *      strategy of the paper) while the result is streamed to the output file. The output has
 *      the data type and novalue of the input and is tiled with {@link #pTileSize}.</li>
 * </ul>
 *
 * <p>The result is the same as a sequential Priority-Flood: depressions are filled to flat
 * surfaces (no epsilon gradient is imposed). Cells on the DEM border and cells touching
 * novalues are considered outlets and are never raised.</p>
 * 
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author. Verified against a sequential Priority-Flood,
 * OmsPitfiller, OmsFloDirection, OmsTca and OmsExtractNetwork.</p>
 */
@Description(OmsPitfillerBarnes.OMSPITFILLERBARNES_DESCRIPTION)
@Author(name = OmsPitfillerBarnes.OMSPITFILLERBARNES_AUTHORNAMES, contact = OmsPitfillerBarnes.OMSPITFILLERBARNES_AUTHORCONTACTS)
@Keywords(OmsPitfillerBarnes.OMSPITFILLERBARNES_KEYWORDS)
@Label(OmsPitfillerBarnes.OMSPITFILLERBARNES_LABEL)
@Name(OmsPitfillerBarnes.OMSPITFILLERBARNES_NAME)
@Status(OmsPitfillerBarnes.OMSPITFILLERBARNES_STATUS)
@License(OmsPitfillerBarnes.OMSPITFILLERBARNES_LICENSE)
public class OmsPitfillerBarnes extends HMModel {
    @Description(OMSPITFILLERBARNES_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev;

    @Description(OMSPITFILLERBARNES_doLargeFile_DESCRIPTION)
    @In
    public boolean doLargeFile = false;

    @Description(OMSPITFILLERBARNES_inElevFile_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElevFile;

    @Description(OMSPITFILLERBARNES_outPitFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPitFile;

    @Description(OMSPITFILLERBARNES_doFlow_DESCRIPTION)
    @In
    public boolean doFlow = false;

    @Description(OMSPITFILLERBARNES_outFlowFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFlowFile;

    @Description(OMSPITFILLERBARNES_doTca_DESCRIPTION)
    @In
    public boolean doTca = false;

    @Description(OMSPITFILLERBARNES_outTcaFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTcaFile;

    @Description(OMSPITFILLERBARNES_doNet_DESCRIPTION)
    @In
    public boolean doNet = false;

    @Description(OMSPITFILLERBARNES_pThres_DESCRIPTION)
    @In
    public long pThres = 0;

    @Description(OMSPITFILLERBARNES_outNetFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetFile;

    @Description(OMSPITFILLERBARNES_pTileSize_DESCRIPTION)
    @In
    public int pTileSize = 1024;

    @Description(OMSPITFILLERBARNES_pThreads_DESCRIPTION)
    @In
    public int pThreads = getDefaultThreadsNum();

    @Description(OMSPITFILLERBARNES_outPit_DESCRIPTION)
    @Out
    public GridCoverage2D outPit = null;

    @Description(OMSPITFILLERBARNES_outFlow_DESCRIPTION)
    @Out
    public GridCoverage2D outFlow = null;

    @Description(OMSPITFILLERBARNES_outTca_DESCRIPTION)
    @Out
    public GridCoverage2D outTca = null;

    @Description(OMSPITFILLERBARNES_outNet_DESCRIPTION)
    @Out
    public GridCoverage2D outNet = null;

    public static final String OMSPITFILLERBARNES_DESCRIPTION = "Fills the depressions of a DEM using the parallel Priority-Flood algorithm by Barnes (2016).";
    public static final String OMSPITFILLERBARNES_DOCUMENTATION = "";
    public static final String OMSPITFILLERBARNES_KEYWORDS = "Dem manipulation, Geomorphology, Pitfiller, Priority-Flood";
    public static final String OMSPITFILLERBARNES_LABEL = DEMMANIPULATION;
    public static final String OMSPITFILLERBARNES_NAME = "pitfillerbarnes";
    public static final int OMSPITFILLERBARNES_STATUS = Status.EXPERIMENTAL;
    public static final String OMSPITFILLERBARNES_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSPITFILLERBARNES_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSPITFILLERBARNES_AUTHORCONTACTS = "https://g-ant.eu";
    public static final String OMSPITFILLERBARNES_inElev_DESCRIPTION = "The map of digital elevation model (DEM), used in memory mode.";
    public static final String OMSPITFILLERBARNES_doLargeFile_DESCRIPTION = "Work file to file for DEMs that do not fit in memory, using inElevFile and outPitFile (default is false).";
    public static final String OMSPITFILLERBARNES_inElevFile_DESCRIPTION = "The GeoTIFF file of the DEM, used in large file mode.";
    public static final String OMSPITFILLERBARNES_outPitFile_DESCRIPTION = "The output GeoTIFF file of the depitted DEM, used in large file mode.";
    public static final String OMSPITFILLERBARNES_pTileSize_DESCRIPTION = "The size in cells of the side of the tiles processed in parallel, in large file mode also the tile size of the output, a multiple of 16 (default is 1024).";
    public static final String OMSPITFILLERBARNES_pThreads_DESCRIPTION = "The number of threads to use (defaults to the number of available processors).";
    public static final String OMSPITFILLERBARNES_outPit_DESCRIPTION = "The depitted elevation map, set in memory mode.";
    public static final String OMSPITFILLERBARNES_doFlow_DESCRIPTION = "Also compute the D8 flow directions of the depitted map, flats included (default is false).";
    public static final String OMSPITFILLERBARNES_outFlowFile_DESCRIPTION = "The output GeoTIFF file of the flow directions, used in large file mode with doFlow.";
    public static final String OMSPITFILLERBARNES_outFlow_DESCRIPTION = "The map of D8 flow directions (10 for outlets), set in memory mode with doFlow.";
    public static final String OMSPITFILLERBARNES_doTca_DESCRIPTION = "Also compute the total contributing areas (in cells) of the D8 flow directions (default is false). They are integers when the number of valid cells allows it, else doubles.";
    public static final String OMSPITFILLERBARNES_outTcaFile_DESCRIPTION = "The output GeoTIFF file of the total contributing areas, used in large file mode with doTca.";
    public static final String OMSPITFILLERBARNES_outTca_DESCRIPTION = "The map of total contributing areas, in cells, set in memory mode with doTca.";
    public static final String OMSPITFILLERBARNES_doNet_DESCRIPTION = "Also extract the network as the cells with a total contributing area of at least pThres cells (default is false).";
    public static final String OMSPITFILLERBARNES_pThres_DESCRIPTION = "The threshold on the total contributing area, in cells, for the network extraction.";
    public static final String OMSPITFILLERBARNES_outNetFile_DESCRIPTION = "The output GeoTIFF file of the network, used in large file mode with doNet.";
    public static final String OMSPITFILLERBARNES_outNet_DESCRIPTION = "The map of the network (network cells have value 2, the others novalue), set in memory mode with doNet.";

    /**
     * The novalue of the network map.
     */
    public static final short NET_NOVALUE = HMConstants.shortNovalue;

    /**
     * The novalue of the flow directions map.
     */
    public static final short FLOW_NOVALUE = HMConstants.shortNovalue;

    private HMRaster inRaster;
    private int cols;
    private int rows;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outPit == null, doReset)) {
            return;
        }
        if (doNet && pThres < 1) {
            throw new ModelsIllegalargumentException("The network threshold has to be at least 1 cell.", this, pm);
        }
        if (doLargeFile) {
            checkNull(inElevFile, outPitFile);
            if (doFlow) {
                checkNull(outFlowFile);
            }
            if (doTca) {
                checkNull(outTcaFile);
            }
            if (doNet) {
                checkNull(outNetFile);
            }
            List<File> files = new ArrayList<>();
            for( String path : new String[]{inElevFile, outPitFile, doFlow ? outFlowFile : null, doTca ? outTcaFile : null,
                    doNet ? outNetFile : null} ) {
                if (path != null) {
                    File file = new File(path).getAbsoluteFile();
                    if (files.contains(file)) {
                        throw new ModelsIllegalargumentException("Input and output files have to be all different.", this,
                                pm);
                    }
                    files.add(file);
                }
            }
            if (pTileSize < 16 || pTileSize % 16 != 0) {
                throw new ModelsIllegalargumentException("In large file mode the tile size has to be a multiple of 16.", this,
                        pm);
            }
            inRaster = HMRaster.fromFileWindowed(inElevFile);
        } else {
            checkNull(inElev);
            if (pTileSize < 2) {
                throw new ModelsIllegalargumentException("The tile size has to be at least 2.", this, pm);
            }
            inRaster = HMRaster.fromGridCoverage(inElev);
        }
        int threads = Math.max(1, pThreads);

        try {
            cols = inRaster.getCols();
            rows = inRaster.getRows();
            List<HMRasterTile> rasterTiles = inRaster.getTiles(pTileSize);
            int tileCols = HMRasterTile.getTilesCount(cols, pTileSize);
            int tileRows = HMRasterTile.getTilesCount(rows, pTileSize);
            PriorityFloodTile[] tiles = new PriorityFloodTile[rasterTiles.size()];
            for( HMRasterTile rasterTile : rasterTiles ) {
                tiles[rasterTile.getIndex()] = new PriorityFloodTile(rasterTile, cols, rows);
            }

            if (doLargeFile) {
                processLargeFile(rasterTiles, tiles, tileCols, tileRows, threads);
            } else {
                processInMemory(rasterTiles, tiles, tileCols, tileRows, threads);
            }
        } finally {
            inRaster.close();
        }
    }

    private void processInMemory( List<HMRasterTile> rasterTiles, PriorityFloodTile[] tiles, int tileCols, int tileRows,
            int threads ) throws Exception {
        HMRaster outRaster = new HMRaster.HMRasterWritableBuilder().setName("pitfiller").setTemplate(inRaster).build();
        // the HMRaster iterators are not thread safe, so the access to the output goes through a lock
        Object outLock = new Object();
        try {
            // phase 1: flood the tiles, keeping labels and tile-filled elevations
            int[][] tilesLabels = new int[tiles.length][];
            HMRaster.processTiles(pm, "Flooding " + tiles.length + " tiles...", rasterTiles, threads, rasterTile -> {
                PriorityFloodTile tile = tiles[rasterTile.getIndex()];
                FloodResult result = tile.flood(readTile(tile));
                synchronized (outLock) {
                    writeTile(outRaster, tile, result.elevs);
                }
                tilesLabels[rasterTile.getIndex()] = result.labels;
            });
            if (pm.isCanceled()) {
                return;
            }

            double[] spillElevations = solveSpillElevations(tiles, tileCols, tileRows);
            if (pm.isCanceled()) {
                return;
            }

            // phase 3: raise the cells of each tile to the spill elevation of their label
            HMRaster.processTiles(pm, "Raising the depressions to their spill elevation...", nonEmpty(rasterTiles, tiles),
                    threads, rasterTile -> {
                        PriorityFloodTile tile = tiles[rasterTile.getIndex()];
                        FloodResult result = new FloodResult();
                        synchronized (outLock) {
                            result.elevs = outRaster.getValues(tile.c0, tile.r0, tile.w, tile.h, null);
                        }
                        result.labels = tilesLabels[rasterTile.getIndex()];
                        tile.raise(result, spillElevations);
                        synchronized (outLock) {
                            writeTile(outRaster, tile, result.elevs);
                        }
                        tilesLabels[rasterTile.getIndex()] = null;
                    });
            if (pm.isCanceled()) {
                return;
            }

            if (doFlow || doTca || doNet) {
                computeFlowInMemory(rasterTiles, tiles, tileCols, tileRows, threads, spillElevations, outRaster, outLock);
            }
            outPit = outRaster.buildCoverage();
        } finally {
            outRaster.close();
        }
    }

    private void computeFlowInMemory( List<HMRasterTile> rasterTiles, PriorityFloodTile[] tiles, int tileCols, int tileRows,
            int threads, double[] spillElevations, HMRaster outRaster, Object outLock ) throws Exception {
        PriorityFloodFlow flow = new PriorityFloodFlow(tiles, tileCols, tileRows, pTileSize, cols, rows, spillElevations,
                inRaster.getXRes(), inRaster.getYRes());
        List<HMRasterTile> nonEmptyTiles = nonEmpty(rasterTiles, tiles);
        HMRaster.processTiles(pm, "Analyzing the flats...", nonEmptyTiles, threads, rasterTile -> {
            PriorityFloodTile tile = tiles[rasterTile.getIndex()];
            flow.analyze(tile, readFinalTile(outRaster, outLock, tile));
        });
        solveFlatExits(flow);
        if (pm.isCanceled()) {
            return;
        }

        if (doFlow) {
            HMRaster flowRaster = new HMRaster.HMRasterWritableBuilder().setName("flow").setTemplate(inRaster)
                    .setDoShort(true).setNoValue(FLOW_NOVALUE).build();
            Object flowLock = new Object();
            try {
                HMRaster.processTiles(pm, "Computing the flow directions...", nonEmptyTiles, threads, rasterTile -> {
                    PriorityFloodTile tile = tiles[rasterTile.getIndex()];
                    int[] codes = flow.computeFlow(tile, readFinalTile(outRaster, outLock, tile));
                    synchronized (flowLock) {
                        for( int r = 0; r < tile.h; r++ ) {
                            for( int c = 0; c < tile.w; c++ ) {
                                int code = codes[r * tile.w + c];
                                if (code > 0) {
                                    flowRaster.setValue(tile.c0 + c, tile.r0 + r, (short) code);
                                }
                            }
                        }
                    }
                });
                outFlow = flowRaster.buildCoverage();
            } finally {
                flowRaster.close();
            }
        }

        if (doTca || doNet) {
            PriorityFloodAccumulation accumulation = new PriorityFloodAccumulation(rasterTiles, tileCols, pTileSize, cols,
                    rows);
            HMRaster.processTiles(pm, "Accumulating the flow in the tiles...", nonEmptyTiles, threads, rasterTile -> {
                PriorityFloodTile tile = tiles[rasterTile.getIndex()];
                accumulation.analyze(rasterTile, flow.computeFlow(tile, readFinalTile(outRaster, outLock, tile)));
            });
            solveAccumulation(accumulation);

            HMRaster tcaRaster = null;
            boolean asInteger = false;
            if (doTca) {
                asInteger = isTcaInteger(tiles);
                HMRaster.HMRasterWritableBuilder builder = new HMRaster.HMRasterWritableBuilder().setName("tca")
                        .setTemplate(inRaster);
                if (asInteger) {
                    builder.setDoInteger(true).setNoValue(HMConstants.intNovalue);
                } else {
                    builder.setNoValue(HMConstants.doubleNovalue);
                }
                tcaRaster = builder.build();
            }
            HMRaster netRaster = null;
            if (doNet) {
                netRaster = new HMRaster.HMRasterWritableBuilder().setName("network").setTemplate(inRaster).setDoShort(true)
                        .setNoValue(NET_NOVALUE).build();
            }
            HMRaster _tcaRaster = tcaRaster;
            HMRaster _netRaster = netRaster;
            boolean _asInteger = asInteger;
            Object rastersLock = new Object();
            try {
                HMRaster.processTiles(pm, "Computing the total contributing areas...", nonEmptyTiles, threads, rasterTile -> {
                    PriorityFloodTile tile = tiles[rasterTile.getIndex()];
                    int[] codes = flow.computeFlow(tile, readFinalTile(outRaster, outLock, tile));
                    long[] tca = accumulation.accumulate(rasterTile, codes);
                    synchronized (rastersLock) {
                        for( int r = 0; r < tile.h; r++ ) {
                            for( int c = 0; c < tile.w; c++ ) {
                                long value = tca[r * tile.w + c];
                                if (value <= 0) {
                                    continue;
                                }
                                int col = tile.c0 + c;
                                int row = tile.r0 + r;
                                if (_tcaRaster != null) {
                                    if (_asInteger) {
                                        _tcaRaster.setValue(col, row, (int) value);
                                    } else {
                                        _tcaRaster.setValue(col, row, (double) value);
                                    }
                                }
                                if (_netRaster != null && value >= pThres) {
                                    _netRaster.setValue(col, row, (short) NETVALUE);
                                }
                            }
                        }
                    }
                });
                if (tcaRaster != null) {
                    outTca = tcaRaster.buildCoverage();
                }
                if (netRaster != null) {
                    outNet = netRaster.buildCoverage();
                }
            } finally {
                if (tcaRaster != null) {
                    tcaRaster.close();
                }
                if (netRaster != null) {
                    netRaster.close();
                }
            }
        }
    }

    /**
     * The contributing area of a cell can't be more than the valid cells, if they fit an int, the tca can be int.
     */
    private boolean isTcaInteger( PriorityFloodTile[] tiles ) {
        long validCells = 0;
        for( PriorityFloodTile tile : tiles ) {
            validCells += tile.getValidCount();
        }
        boolean asInteger = validCells <= Integer.MAX_VALUE;
        pm.message("Valid cells: " + validCells + ", total contributing areas written as "
                + (asInteger ? "integers." : "doubles."));
        return asInteger;
    }

    private void solveAccumulation( PriorityFloodAccumulation accumulation ) {
        pm.beginTask("Passing the flow accumulation across the tiles...", IHMProgressMonitor.UNKNOWN);
        try {
            accumulation.solve();
        } finally {
            pm.done();
        }
    }

    /**
     * Read the final elevations of a tile from the in memory output, with novalues as NaN.
     */
    private static double[] readFinalTile( HMRaster outRaster, Object outLock, PriorityFloodTile tile ) {
        double[] values;
        synchronized (outLock) {
            values = outRaster.getValues(tile.c0, tile.r0, tile.w, tile.h, null);
        }
        for( int i = 0; i < values.length; i++ ) {
            if (outRaster.isNovalue(values[i])) {
                values[i] = Double.NaN;
            }
        }
        return values;
    }

    private void solveFlatExits( PriorityFloodFlow flow ) {
        pm.beginTask("Draining the flats across the tiles...", IHMProgressMonitor.UNKNOWN);
        try {
            int undrained = flow.solveFlatExits();
            if (undrained > 0) {
                pm.errorMessage("Flats without an outlet (they are left with novalues in the flow map): " + undrained);
            }
        } finally {
            pm.done();
        }
    }

    private void processLargeFile( List<HMRasterTile> rasterTiles, PriorityFloodTile[] tiles, int tileCols, int tileRows,
            int threads ) throws Exception {
        // phase 1: flood the tiles, keeping only perimeters and edges
        HMRaster.processTiles(pm, "Flooding " + tiles.length + " tiles...", rasterTiles, threads, rasterTile -> {
            PriorityFloodTile tile = tiles[rasterTile.getIndex()];
            tile.flood(readTile(tile));
        });
        if (pm.isCanceled()) {
            return;
        }
        pm.message("Tiles containing only novalues: " + (tiles.length - nonEmpty(rasterTiles, tiles).size()) + " of "
                + tiles.length);

        double[] spillElevations = solveSpillElevations(tiles, tileCols, tileRows);
        if (pm.isCanceled()) {
            return;
        }

        if (!doFlow && !doTca && !doNet) {
            // phase 3: flood the tiles again and raise them while streaming them to the file
            HMRasterTiledWriter.writeGeotiff(pm, outPitFile, inRaster, pTileSize, threads, rasterTile -> {
                PriorityFloodTile tile = tiles[rasterTile.getIndex()];
                return tile.isEmpty() ? null : computeFinalTile(tile, spillElevations);
            });
            return;
        }

        // the flats need a pass on the final surface before the flow can be computed
        PriorityFloodFlow flow = new PriorityFloodFlow(tiles, tileCols, tileRows, pTileSize, cols, rows, spillElevations,
                inRaster.getXRes(), inRaster.getYRes());
        HMRaster.processTiles(pm, "Analyzing the flats...", nonEmpty(rasterTiles, tiles), threads, rasterTile -> {
            PriorityFloodTile tile = tiles[rasterTile.getIndex()];
            flow.analyze(tile, computeFinalTile(tile, spillElevations));
        });
        if (pm.isCanceled()) {
            return;
        }
        solveFlatExits(flow);
        if (pm.isCanceled()) {
            return;
        }

        // the accumulation needs a pass on the final flow before it can be computed
        PriorityFloodAccumulation accumulation = null;
        boolean tcaAsInteger = false;
        if (doTca || doNet) {
            PriorityFloodAccumulation _accumulation = new PriorityFloodAccumulation(rasterTiles, tileCols, pTileSize, cols,
                    rows);
            HMRaster.processTiles(pm, "Accumulating the flow in the tiles...", nonEmpty(rasterTiles, tiles), threads,
                    rasterTile -> {
                        PriorityFloodTile tile = tiles[rasterTile.getIndex()];
                        _accumulation.analyze(rasterTile, flow.computeFlow(tile, computeFinalTile(tile, spillElevations)));
                    });
            if (pm.isCanceled()) {
                return;
            }
            solveAccumulation(_accumulation);
            accumulation = _accumulation;
            if (doTca) {
                tcaAsInteger = isTcaInteger(tiles);
            }
        }

        // phase 3: flood the tiles again, raise them, compute flow, tca and network, streaming all to their files
        List<TiledOutput> outputs = new ArrayList<>();
        outputs.add(new TiledOutput(outPitFile, inRaster.getRenderedImage().getSampleModel().getDataType(),
                inRaster.getNovalue()));
        if (doFlow) {
            outputs.add(new TiledOutput(outFlowFile, DataBuffer.TYPE_SHORT, FLOW_NOVALUE));
        }
        if (doTca) {
            outputs.add(tcaAsInteger
                    ? new TiledOutput(outTcaFile, DataBuffer.TYPE_INT, HMConstants.intNovalue)
                    : new TiledOutput(outTcaFile, DataBuffer.TYPE_DOUBLE, HMConstants.doubleNovalue));
        }
        if (doNet) {
            outputs.add(new TiledOutput(outNetFile, DataBuffer.TYPE_SHORT, NET_NOVALUE));
        }
        PriorityFloodAccumulation _accumulation = accumulation;
        HMRasterTiledWriter.writeGeotiffs(pm, inRaster, pTileSize, threads, outputs, rasterTile -> {
            PriorityFloodTile tile = tiles[rasterTile.getIndex()];
            if (tile.isEmpty()) {
                return null;
            }
            double[] elevs = computeFinalTile(tile, spillElevations);
            int[] codes = flow.computeFlow(tile, elevs);
            double[][] values = new double[outputs.size()][];
            int index = 0;
            values[index++] = elevs;
            if (doFlow) {
                double[] flowValues = new double[codes.length];
                for( int i = 0; i < codes.length; i++ ) {
                    flowValues[i] = codes[i] > 0 ? codes[i] : Double.NaN;
                }
                values[index++] = flowValues;
            }
            if (doTca || doNet) {
                long[] tca = _accumulation.accumulate(rasterTile, codes);
                if (doTca) {
                    double[] tcaValues = new double[tca.length];
                    for( int i = 0; i < tca.length; i++ ) {
                        tcaValues[i] = tca[i] > 0 ? tca[i] : Double.NaN;
                    }
                    values[index++] = tcaValues;
                }
                if (doNet) {
                    double[] netValues = new double[tca.length];
                    for( int i = 0; i < tca.length; i++ ) {
                        netValues[i] = tca[i] > 0 && tca[i] >= pThres ? NETVALUE : Double.NaN;
                    }
                    values[index++] = netValues;
                }
            }
            return values;
        });
    }

    /**
     * Flood a tile again and raise it to the spill elevations.
     */
    private double[] computeFinalTile( PriorityFloodTile tile, double[] spillElevations ) {
        FloodResult result = tile.flood(readTile(tile));
        tile.raise(result, spillElevations);
        return result.elevs;
    }

    private double[] solveSpillElevations( PriorityFloodTile[] tiles, int tileCols, int tileRows ) {
        pm.beginTask("Solving the spill elevations of the watersheds graph...", IHMProgressMonitor.UNKNOWN);
        try {
            // the flow needs the perimeters of the tiles to build the halo of the tiles
            return new PriorityFloodGraph(tiles, tileCols, tileRows, pTileSize).solve(!doFlow && !doTca && !doNet);
        } finally {
            pm.done();
        }
    }

    private static List<HMRasterTile> nonEmpty( List<HMRasterTile> rasterTiles, PriorityFloodTile[] tiles ) {
        List<HMRasterTile> nonEmpty = new ArrayList<>();
        for( HMRasterTile rasterTile : rasterTiles ) {
            if (!tiles[rasterTile.getIndex()].isEmpty()) {
                nonEmpty.add(rasterTile);
            }
        }
        return nonEmpty;
    }

    /**
     * Read the values of a tile, with novalues as NaN.
     */
    private double[] readTile( PriorityFloodTile tile ) {
        double[] values = inRaster.getValues(tile.c0, tile.r0, tile.w, tile.h, null);
        for( int i = 0; i < values.length; i++ ) {
            if (inRaster.isNovalue(values[i])) {
                values[i] = Double.NaN;
            }
        }
        return values;
    }

    /**
     * Write the valid values of a tile to the output raster.
     */
    private static void writeTile( HMRaster outRaster, PriorityFloodTile tile, double[] elevs ) throws Exception {
        for( int r = 0; r < tile.h; r++ ) {
            for( int c = 0; c < tile.w; c++ ) {
                double value = elevs[r * tile.w + c];
                if (!Double.isNaN(value)) {
                    outRaster.setValue(tile.c0 + c, tile.r0 + r, value);
                }
            }
        }
    }
}
