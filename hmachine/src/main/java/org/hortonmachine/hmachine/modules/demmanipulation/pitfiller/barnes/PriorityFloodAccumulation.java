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
package org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes;

import java.util.List;

import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTile;

/**
 * Tiled D8 flow accumulation (total contributing area, in number of cells).
 *
 * <p>Follows the idea of: Barnes, R., 2017. Parallel non-divergent flow accumulation for
 * trillion cell digital elevation models on desktops or clusters. Environmental Modelling
 * & Software 92, 202–212. https://doi.org/10.1016/j.envsoft.2017.02.022</p>
 *
 * <p>The value of a cell is the cell itself plus all the cells upstream of it, as in
 * {@code OmsTca}. It works in three phases:</p>
 * <ol>
 *  <li><b>tiles in parallel</b> ({@link #analyze(HMRasterTile, int[])}): the accumulation inside the
 *      tile, in topological order. For every perimeter cell it is recorded where its flow leaves the
 *      tile and, for the cells where it leaves, how much leaves.</li>
 *  <li><b>sequential</b> ({@link #solve()}): the amounts leaving the tiles are passed downstream
 *      from tile to tile, in topological order of the leaving cells, giving what enters every
 *      perimeter cell from the neighbour tiles.</li>
 *  <li><b>tiles in parallel</b> ({@link #accumulate(HMRasterTile, int[])}): the accumulation inside
 *      the tile again, with the entering amounts added.</li>
 * </ol>
 *
 * <p>The flow is given per tile as HortonMachine flow codes (1 to 8, 10 for outlets). Codes
 * &lt;= 0 are novalues. Flow pointing outside the raster or into novalues ends there.</p>
 * 
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author. Verified against a sequential Priority-Flood,
 * OmsPitfiller, OmsFloDirection, OmsTca and OmsExtractNetwork.</p>
 */
public class PriorityFloodAccumulation {
    /**
     * Neighbour offsets in the order of the HortonMachine flow codes 1 to 8.
     */
    private static final int[] DCOLS = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DROWS = {0, -1, -1, -1, 0, 1, 1, 1};
    private static final int OUTLET = 10;

    private final List<HMRasterTile> tiles;
    private final int tileCols;
    private final int tileSize;
    private final int cols;
    private final int rows;

    private final TilePerimeter[] perimeters;
    /**
     * The amounts entering each perimeter cell from other tiles, indexed by node (tile offset + perimeter index).
     */
    private long[] enteringAmounts;

    /**
     * The perimeter data of a tile.
     */
    private static class TilePerimeter {
        int nodeOffset;
        /**
         * Per perimeter cell: <code>false</code> for novalues.
         */
        boolean[] valid;
        /**
         * Per perimeter cell: the perimeter cell where its flow leaves the tile, or -1.
         */
        int[] leavesAt;
        /**
         * Per perimeter cell: the flow code if the cell flows out of the tile, else 0.
         */
        int[] leavingCodes;
        /**
         * Per perimeter cell that flows out of the tile: the accumulation inside the tile.
         */
        long[] leavingAmounts;
    }

    /**
     * @param tiles the tiles of the raster grid, in row-major order.
     * @param tileCols the number of tile columns.
     * @param tileSize the size of the side of the tiles.
     * @param cols the cols of the raster.
     * @param rows the rows of the raster.
     */
    public PriorityFloodAccumulation( List<HMRasterTile> tiles, int tileCols, int tileSize, int cols, int rows ) {
        this.tiles = tiles;
        this.tileCols = tileCols;
        this.tileSize = tileSize;
        this.cols = cols;
        this.rows = rows;
        this.perimeters = new TilePerimeter[tiles.size()];
    }

    /**
     * Phase 1: accumulate inside a tile and store its perimeter data. Can be called concurrently
     * on different tiles. Tiles that are never analyzed are considered all novalues.
     *
     * @param tile the tile.
     * @param codes the flow codes of the tile in row-major order.
     */
    public void analyze( HMRasterTile tile, int[] codes ) {
        int w = tile.getWidth();
        int h = tile.getHeight();
        long[] amounts = accumulateInTile(tile, codes, null);
        int[] order = lastOrder.get();

        // where the flow of each cell leaves the tile: cell index, or -1 if it ends inside
        int[] leavesAt = new int[w * h];
        for( int k = order.length - 1; k >= 0; k-- ) {
            int i = order[k];
            if (i < 0) {
                continue;
            }
            int target = downstream(tile, codes, i);
            if (target == LEAVES_TILE) {
                leavesAt[i] = i;
            } else if (target == ENDS) {
                leavesAt[i] = -1;
            } else {
                leavesAt[i] = leavesAt[target];
            }
        }

        TilePerimeter perimeter = new TilePerimeter();
        int count = perimeterCount(w, h);
        perimeter.valid = new boolean[count];
        perimeter.leavesAt = new int[count];
        perimeter.leavingCodes = new int[count];
        perimeter.leavingAmounts = new long[count];
        for( int r = 0; r < h; r++ ) {
            boolean fullRow = r == 0 || r == h - 1;
            for( int c = 0; c < w; c += fullRow ? 1 : Math.max(1, w - 1) ) {
                int i = r * w + c;
                int p = perimeterIndex(w, h, c, r);
                if (codes[i] <= 0) {
                    perimeter.leavesAt[p] = -1;
                    continue;
                }
                perimeter.valid[p] = true;
                int exit = leavesAt[i];
                perimeter.leavesAt[p] = exit < 0 ? -1 : perimeterIndex(w, h, exit % w, exit / w);
                if (exit == i) {
                    perimeter.leavingCodes[p] = codes[i];
                    perimeter.leavingAmounts[p] = amounts[i];
                }
            }
        }
        perimeters[tile.getIndex()] = perimeter;
    }

    /**
     * Phase 2: pass the amounts leaving the tiles downstream, tile to tile.
     *
     * @throws IllegalStateException if the flow has loops across the tiles.
     */
    public void solve() {
        int nodes = 0;
        for( int t = 0; t < perimeters.length; t++ ) {
            if (perimeters[t] != null) {
                perimeters[t].nodeOffset = nodes;
                nodes += perimeters[t].leavesAt.length;
            }
        }
        enteringAmounts = new long[nodes];
        long[] totals = new long[nodes];
        int[] nextLeaving = new int[nodes];
        int[] enteringNode = new int[nodes];
        int[] inDegree = new int[nodes];
        java.util.Arrays.fill(nextLeaving, -1);
        java.util.Arrays.fill(enteringNode, -1);

        // links from every leaving cell to the cell it enters and to where the flow leaves that tile
        int leavingCount = 0;
        for( int t = 0; t < perimeters.length; t++ ) {
            TilePerimeter perimeter = perimeters[t];
            if (perimeter == null) {
                continue;
            }
            HMRasterTile tile = tiles.get(t);
            int w = tile.getWidth();
            int h = tile.getHeight();
            for( int r = 0; r < h; r++ ) {
                boolean fullRow = r == 0 || r == h - 1;
                for( int c = 0; c < w; c += fullRow ? 1 : Math.max(1, w - 1) ) {
                    int p = perimeterIndex(w, h, c, r);
                    int code = perimeter.leavingCodes[p];
                    if (code <= 0) {
                        continue;
                    }
                    int node = perimeter.nodeOffset + p;
                    leavingCount++;
                    totals[node] = perimeter.leavingAmounts[p];
                    int toCol = tile.getCol() + c + DCOLS[code - 1];
                    int toRow = tile.getRow() + r + DROWS[code - 1];
                    if (toCol < 0 || toRow < 0 || toCol >= cols || toRow >= rows) {
                        continue;
                    }
                    HMRasterTile toTile = getTile(toCol, toRow);
                    TilePerimeter toPerimeter = perimeters[toTile.getIndex()];
                    if (toPerimeter == null) {
                        continue;
                    }
                    int toP = perimeterIndex(toTile.getWidth(), toTile.getHeight(), toCol - toTile.getCol(),
                            toRow - toTile.getRow());
                    if (!toPerimeter.valid[toP]) {
                        // flows into a novalue, it ends there
                        continue;
                    }
                    enteringNode[node] = toPerimeter.nodeOffset + toP;
                    int leavesAt = toPerimeter.leavesAt[toP];
                    if (leavesAt >= 0) {
                        nextLeaving[node] = toPerimeter.nodeOffset + leavesAt;
                        inDegree[nextLeaving[node]]++;
                    }
                }
            }
        }

        // topological order over the leaving cells
        int[] queue = new int[leavingCount];
        int head = 0;
        int tail = 0;
        for( int t = 0; t < perimeters.length; t++ ) {
            TilePerimeter perimeter = perimeters[t];
            if (perimeter == null) {
                continue;
            }
            for( int p = 0; p < perimeter.leavingCodes.length; p++ ) {
                int node = perimeter.nodeOffset + p;
                if (perimeter.leavingCodes[p] > 0 && inDegree[node] == 0) {
                    queue[tail++] = node;
                }
            }
        }
        while( head < tail ) {
            int node = queue[head++];
            if (enteringNode[node] >= 0) {
                enteringAmounts[enteringNode[node]] += totals[node];
            }
            int next = nextLeaving[node];
            if (next >= 0) {
                totals[next] += totals[node];
                if (--inDegree[next] == 0) {
                    queue[tail++] = next;
                }
            }
        }
        if (tail != leavingCount) {
            throw new IllegalStateException("The flow directions have loops across the tiles.");
        }
        for( TilePerimeter perimeter : perimeters ) {
            if (perimeter != null) {
                perimeter.leavingAmounts = null;
            }
        }
    }

    /**
     * Phase 3: the final accumulation of a tile. Can be called concurrently on different tiles.
     *
     * @param tile the tile.
     * @param codes the flow codes of the tile in row-major order.
     * @return the number of cells draining through each cell, -1 for novalues.
     */
    public long[] accumulate( HMRasterTile tile, int[] codes ) {
        TilePerimeter perimeter = perimeters[tile.getIndex()];
        long[] entering = null;
        if (perimeter != null) {
            int w = tile.getWidth();
            int h = tile.getHeight();
            entering = new long[w * h];
            for( int r = 0; r < h; r++ ) {
                boolean fullRow = r == 0 || r == h - 1;
                for( int c = 0; c < w; c += fullRow ? 1 : Math.max(1, w - 1) ) {
                    entering[r * w + c] = enteringAmounts[perimeter.nodeOffset + perimeterIndex(w, h, c, r)];
                }
            }
        }
        return accumulateInTile(tile, codes, entering);
    }

    private static final int LEAVES_TILE = -2;
    private static final int ENDS = -1;

    /**
     * The processing order of the last {@link #accumulateInTile(HMRasterTile, int[], long[])} of the thread.
     */
    private final ThreadLocal<int[]> lastOrder = new ThreadLocal<>();

    /**
     * Accumulate inside a tile in topological order (Kahn), from the cells with nothing flowing in.
     */
    private long[] accumulateInTile( HMRasterTile tile, int[] codes, long[] entering ) {
        int w = tile.getWidth();
        int h = tile.getHeight();
        int n = w * h;
        int[] inDegree = new int[n];
        int valid = 0;
        for( int i = 0; i < n; i++ ) {
            if (codes[i] <= 0) {
                continue;
            }
            valid++;
            int target = downstream(tile, codes, i);
            if (target >= 0) {
                inDegree[target]++;
            }
        }
        long[] amounts = new long[n];
        int[] order = new int[n];
        java.util.Arrays.fill(order, -1);
        int head = 0;
        int tail = 0;
        for( int i = 0; i < n; i++ ) {
            if (codes[i] <= 0) {
                amounts[i] = -1;
                continue;
            }
            amounts[i] = 1 + (entering == null ? 0 : entering[i]);
            if (inDegree[i] == 0) {
                order[tail++] = i;
            }
        }
        while( head < tail ) {
            int i = order[head++];
            int target = downstream(tile, codes, i);
            if (target >= 0) {
                amounts[target] += amounts[i];
                if (--inDegree[target] == 0) {
                    order[tail++] = target;
                }
            }
        }
        if (tail != valid) {
            throw new IllegalStateException("The flow directions have loops in the tile " + tile);
        }
        lastOrder.set(order);
        return amounts;
    }

    /**
     * @return the downstream cell inside the tile, {@link #LEAVES_TILE} if the flow goes to another
     *          tile or {@link #ENDS} if it ends (outlet, novalue or outside the raster).
     */
    private int downstream( HMRasterTile tile, int[] codes, int i ) {
        int code = codes[i];
        if (code <= 0 || code == OUTLET || code > 8) {
            return ENDS;
        }
        int w = tile.getWidth();
        int c = i % w + DCOLS[code - 1];
        int r = i / w + DROWS[code - 1];
        if (c >= 0 && r >= 0 && c < w && r < tile.getHeight()) {
            int target = r * w + c;
            return codes[target] > 0 ? target : ENDS;
        }
        int col = tile.getCol() + c;
        int row = tile.getRow() + r;
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return ENDS;
        }
        return LEAVES_TILE;
    }

    private HMRasterTile getTile( int col, int row ) {
        return tiles.get((row / tileSize) * tileCols + col / tileSize);
    }

    private static int perimeterCount( int w, int h ) {
        if (h == 1) {
            return w;
        }
        return 2 * w + (h - 2) * (w == 1 ? 1 : 2);
    }

    private static int perimeterIndex( int w, int h, int c, int r ) {
        if (r == 0) {
            return c;
        }
        if (r == h - 1) {
            return w + c;
        }
        if (c == 0) {
            return 2 * w + (r - 1);
        }
        if (c == w - 1) {
            return 2 * w + (h - 2) + (r - 1);
        }
        throw new IllegalArgumentException("Not a perimeter cell: " + c + "/" + r);
    }
}
