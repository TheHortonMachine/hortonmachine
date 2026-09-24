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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * D8 flow directions on the depressionless surface produced by the tiled Priority-Flood.
 *
 * <p>The directions follow the HortonMachine schema (1 = E, 2 = NE, 3 = N, 4 = NW, 5 = W,
 * 6 = SW, 7 = S, 8 = SE, 10 = outlet) and are assigned as:</p>
 * <ul>
 *  <li>steepest descent, for cells that have a strictly lower neighbour;</li>
 *  <li>outlet, for cells that touch novalues or the DEM border and have no lower neighbour;</li>
 *  <li>for flat cells (all the filled depressions and the natural flats), towards the neighbour
 *      from which a breadth first search over the flat, started at its already resolved cells,
 *      reached them. On a depressionless surface every flat has such cells, so everything drains.</li>
 * </ul>
 *
 * <p>Flats can cross tile seams. Each tile first splits its flat cells in connected pieces and
 * tells which ones drain inside the tile ({@link #analyze(PriorityFloodTile, double[])}). Then a
 * breadth first search over the pieces across the seams ({@link #solveFlatExits()}) gives each
 * piece that does not drain inside its tile an exit cell towards a piece that does.
 * The resulting tree has no cycles, so the flow of every cell ends in an outlet.</p>
 *
 * <p>The surface of a tile and its 1 cell halo are the tile-filled elevations raised to the
 * spill elevations. The halo comes from the perimeter data of the neighbour tiles, which then
 * have to be kept after the spill graph is solved.</p>
 *
 * <p>Novalues: as in the rest of the tiled Priority-Flood, novalues are {@link Double#NaN} in the
 * elevation arrays. The caller converts the novalue of the raster to NaN when reading (with
 * {@code HMRaster#isNovalue}) and {@link #INVALID} to the novalue of the flow raster when writing.</p>
 * 
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author. Verified against a sequential Priority-Flood,
 * OmsPitfiller, OmsFloDirection, OmsTca and OmsExtractNetwork.</p>
 */
public class PriorityFloodFlow {
    static final int OUTLET = 10;
    static final int UNRESOLVED = 0;
    static final int INVALID = -1;

    /**
     * Perimeter status of a novalue cell. A status >= 0 is the local id of a flat piece.
     */
    private static final int P_INVALID = -2;
    /**
     * Perimeter status of a cell with a flow direction.
     */
    private static final int P_RESOLVED = -1;

    /**
     * Neighbour offsets in the order of the HortonMachine flow codes 1 to 8.
     */
    private static final int[] DCOLS = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DROWS = {0, -1, -1, -1, 0, 1, 1, 1};

    private final PriorityFloodTile[] tiles;
    private final int tileCols;
    private final int tileRows;
    private final int tileSize;
    private final int cols;
    private final int rows;
    private final double[] spillElevations;
    private final double[] distances = new double[8];

    private final TileFlats[] tilesFlats;

    /**
     * The flats of a tile.
     */
    private static class TileFlats {
        int piecesCount;
        boolean[] drainsInTile;
        int[] topStatus;
        int[] bottomStatus;
        int[] leftStatus;
        int[] rightStatus;
        int pieceOffset;
        /**
         * Per piece: the tile cell (r * w + c) that exits the tile, or -1.
         */
        int[] exitCells;
        /**
         * Per piece: the flow code of the exit cell.
         */
        int[] exitCodes;
    }

    /**
     * The surface with halo of a tile, its flow codes and flat pieces.
     */
    private static class TileSurface {
        int hw;
        double[] surface;
        int[] codes;
        int[] pieces;
        int piecesCount;
    }

    public PriorityFloodFlow( PriorityFloodTile[] tiles, int tileCols, int tileRows, int tileSize, int cols, int rows,
            double[] spillElevations, double xRes, double yRes ) {
        this.tiles = tiles;
        this.tileCols = tileCols;
        this.tileRows = tileRows;
        this.tileSize = tileSize;
        this.cols = cols;
        this.rows = rows;
        this.spillElevations = spillElevations;
        this.tilesFlats = new TileFlats[tiles.length];
        double diagonal = Math.sqrt(xRes * xRes + yRes * yRes);
        for( int k = 0; k < 8; k++ ) {
            distances[k] = DCOLS[k] != 0 && DROWS[k] != 0 ? diagonal : (DCOLS[k] != 0 ? xRes : yRes);
        }
    }

    /**
     * First pass on a tile: find its flat pieces, which ones drain inside the tile, and the
     * status of its perimeter cells. Can be called concurrently on different tiles.
     *
     * @param tile the tile.
     * @param finalElevs the final (raised) elevations of the tile, NaN for novalues.
     */
    public void analyze( PriorityFloodTile tile, double[] finalElevs ) {
        TileSurface s = computeSurface(tile, finalElevs);
        int w = tile.w;
        int h = tile.h;
        int hw = s.hw;
        TileFlats flats = new TileFlats();
        flats.piecesCount = s.piecesCount;
        flats.drainsInTile = new boolean[s.piecesCount];
        for( int r = 1; r <= h; r++ ) {
            for( int c = 1; c <= w; c++ ) {
                int i = r * hw + c;
                int piece = s.pieces[i];
                if (piece < 0 || flats.drainsInTile[piece]) {
                    continue;
                }
                for( int k = 0; k < 8; k++ ) {
                    int n = i + DROWS[k] * hw + DCOLS[k];
                    if (isInside(n, hw, w, h) && isResolved(s.codes[n]) && s.surface[n] == s.surface[i]) {
                        flats.drainsInTile[piece] = true;
                        break;
                    }
                }
            }
        }
        flats.topStatus = new int[w];
        flats.bottomStatus = new int[w];
        for( int c = 0; c < w; c++ ) {
            flats.topStatus[c] = status(s, hw + c + 1);
            flats.bottomStatus[c] = status(s, h * hw + c + 1);
        }
        flats.leftStatus = new int[h];
        flats.rightStatus = new int[h];
        for( int r = 0; r < h; r++ ) {
            flats.leftStatus[r] = status(s, (r + 1) * hw + 1);
            flats.rightStatus[r] = status(s, (r + 1) * hw + w);
        }
        flats.exitCells = new int[s.piecesCount];
        flats.exitCodes = new int[s.piecesCount];
        Arrays.fill(flats.exitCells, -1);
        tilesFlats[tile.tile.getIndex()] = flats;
    }

    /**
     * Second (sequential) pass: give every flat piece that does not drain inside its tile
     * an exit cell across the seams, towards a piece that does.
     *
     * @return the number of flat pieces that could not be drained (0 on a depressionless surface).
     */
    public int solveFlatExits() {
        int piecesCount = 0;
        for( int t = 0; t < tiles.length; t++ ) {
            TileFlats flats = getFlats(t);
            flats.pieceOffset = piecesCount;
            piecesCount += flats.piecesCount;
        }
        boolean[] done = new boolean[piecesCount];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for( int t = 0; t < tiles.length; t++ ) {
            TileFlats flats = getFlats(t);
            for( int p = 0; p < flats.piecesCount; p++ ) {
                if (flats.drainsInTile[p]) {
                    done[flats.pieceOffset + p] = true;
                    queue.add(flats.pieceOffset + p);
                }
            }
        }

        // contacts across the seams between cells of the same elevation
        List<int[]> pieceContacts = new ArrayList<>();
        PriorityFloodGraph.visitSeams(tiles, tileCols, tileRows, ( col1, row1, col2, row2 ) -> {
            double elev1 = getTile(col1, row1).getFinalPerimeterElev(col1, row1, spillElevations);
            double elev2 = getTile(col2, row2).getFinalPerimeterElev(col2, row2, spillElevations);
            if (Double.isNaN(elev1) || Double.isNaN(elev2) || elev1 != elev2) {
                return;
            }
            int status1 = getPerimeterStatus(col1, row1);
            int status2 = getPerimeterStatus(col2, row2);
            if (status1 >= 0 && status2 == P_RESOLVED) {
                drainTo(done, queue, col1, row1, col2, row2);
            } else if (status2 >= 0 && status1 == P_RESOLVED) {
                drainTo(done, queue, col2, row2, col1, row1);
            } else if (status1 >= 0 && status2 >= 0) {
                pieceContacts.add(new int[]{col1, row1, col2, row2});
            }
        }, null);

        // adjacency of the pieces, each entry as: cell of the other piece, touching cell of this piece
        int[] start = new int[piecesCount + 1];
        for( int[] contact : pieceContacts ) {
            start[globalPiece(contact[0], contact[1]) + 1]++;
            start[globalPiece(contact[2], contact[3]) + 1]++;
        }
        for( int p = 0; p < piecesCount; p++ ) {
            start[p + 1] += start[p];
        }
        int[] fill = Arrays.copyOf(start, piecesCount);
        int[][] adjacency = new int[start[piecesCount]][];
        for( int[] contact : pieceContacts ) {
            adjacency[fill[globalPiece(contact[0], contact[1])]++] = new int[]{contact[2], contact[3], contact[0], contact[1]};
            adjacency[fill[globalPiece(contact[2], contact[3])]++] = contact;
        }

        while( !queue.isEmpty() ) {
            int piece = queue.poll();
            for( int a = start[piece]; a < start[piece + 1]; a++ ) {
                int[] contact = adjacency[a];
                int other = globalPiece(contact[0], contact[1]);
                if (!done[other]) {
                    done[other] = true;
                    setExit(contact[0], contact[1], contact[2], contact[3]);
                    queue.add(other);
                }
            }
        }

        int undrained = 0;
        for( boolean d : done ) {
            if (!d) {
                undrained++;
            }
        }
        return undrained;
    }

    /**
     * Last pass on a tile: compute its flow directions. Can be called concurrently on different tiles.
     *
     * @param tile the tile.
     * @param finalElevs the final (raised) elevations of the tile, NaN for novalues.
     * @return the flow codes of the tile in row-major order, {@link #INVALID} for novalues.
     *          Cells of undrained flats (only on a surface with depressions) are left {@link #UNRESOLVED}.
     */
    public int[] computeFlow( PriorityFloodTile tile, double[] finalElevs ) {
        TileSurface s = computeSurface(tile, finalElevs);
        int w = tile.w;
        int h = tile.h;
        int hw = s.hw;
        TileFlats flats = tilesFlats[tile.tile.getIndex()];

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for( int r = 1; r <= h; r++ ) {
            for( int c = 1; c <= w; c++ ) {
                int i = r * hw + c;
                if (isResolved(s.codes[i])) {
                    queue.add(i);
                }
            }
        }
        if (flats != null) {
            for( int p = 0; p < flats.piecesCount; p++ ) {
                if (!flats.drainsInTile[p] && flats.exitCells[p] >= 0) {
                    int cell = flats.exitCells[p];
                    int i = (cell / w + 1) * hw + cell % w + 1;
                    s.codes[i] = flats.exitCodes[p];
                    queue.add(i);
                }
            }
        }
        while( !queue.isEmpty() ) {
            int i = queue.poll();
            for( int k = 0; k < 8; k++ ) {
                int n = i + DROWS[k] * hw + DCOLS[k];
                if (isInside(n, hw, w, h) && s.codes[n] == UNRESOLVED && s.surface[n] == s.surface[i]) {
                    // the flow of n goes back to i, the opposite direction of k
                    s.codes[n] = (k + 4) % 8 + 1;
                    queue.add(n);
                }
            }
        }

        int[] result = new int[w * h];
        for( int r = 0; r < h; r++ ) {
            for( int c = 0; c < w; c++ ) {
                result[r * w + c] = s.codes[(r + 1) * hw + c + 1];
            }
        }
        return result;
    }

    /**
     * Build the surface with halo of a tile, assign the directions of the non flat cells
     * and find the flat pieces.
     */
    private TileSurface computeSurface( PriorityFloodTile tile, double[] finalElevs ) {
        int w = tile.w;
        int h = tile.h;
        int hw = w + 2;
        int hh = h + 2;
        TileSurface s = new TileSurface();
        s.hw = hw;
        s.surface = new double[hw * hh];
        Arrays.fill(s.surface, Double.NaN);
        for( int r = 0; r < h; r++ ) {
            System.arraycopy(finalElevs, r * w, s.surface, (r + 1) * hw + 1, w);
        }
        for( int r = 0; r < hh; r++ ) {
            for( int c = 0; c < hw; c++ ) {
                if (r != 0 && c != 0 && r != hh - 1 && c != hw - 1) {
                    continue;
                }
                int col = tile.c0 + c - 1;
                int row = tile.r0 + r - 1;
                if (col >= 0 && row >= 0 && col < cols && row < rows) {
                    s.surface[r * hw + c] = getTile(col, row).getFinalPerimeterElev(col, row, spillElevations);
                }
            }
        }

        s.codes = new int[hw * hh];
        Arrays.fill(s.codes, INVALID);
        for( int r = 1; r <= h; r++ ) {
            for( int c = 1; c <= w; c++ ) {
                int i = r * hw + c;
                double elev = s.surface[i];
                if (Double.isNaN(elev)) {
                    continue;
                }
                int best = -1;
                double maxSlope = 0;
                boolean touchesInvalid = false;
                for( int k = 0; k < 8; k++ ) {
                    double other = s.surface[i + DROWS[k] * hw + DCOLS[k]];
                    if (Double.isNaN(other)) {
                        touchesInvalid = true;
                        continue;
                    }
                    double slope = (elev - other) / distances[k];
                    if (slope > maxSlope) {
                        maxSlope = slope;
                        best = k;
                    }
                }
                if (best >= 0) {
                    s.codes[i] = best + 1;
                } else if (touchesInvalid) {
                    s.codes[i] = OUTLET;
                } else {
                    s.codes[i] = UNRESOLVED;
                }
            }
        }

        // connected pieces of flat cells of the same elevation
        s.pieces = new int[hw * hh];
        Arrays.fill(s.pieces, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for( int r = 1; r <= h; r++ ) {
            for( int c = 1; c <= w; c++ ) {
                int i = r * hw + c;
                if (s.codes[i] != UNRESOLVED || s.pieces[i] >= 0) {
                    continue;
                }
                int piece = s.piecesCount++;
                s.pieces[i] = piece;
                queue.add(i);
                while( !queue.isEmpty() ) {
                    int current = queue.poll();
                    for( int k = 0; k < 8; k++ ) {
                        int n = current + DROWS[k] * hw + DCOLS[k];
                        if (isInside(n, hw, w, h) && s.codes[n] == UNRESOLVED && s.pieces[n] < 0
                                && s.surface[n] == s.surface[current]) {
                            s.pieces[n] = piece;
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return s;
    }

    private static int status( TileSurface s, int i ) {
        if (s.codes[i] == INVALID) {
            return P_INVALID;
        }
        if (s.codes[i] == UNRESOLVED) {
            return s.pieces[i];
        }
        return P_RESOLVED;
    }

    private static boolean isResolved( int code ) {
        return code > 0;
    }

    private static boolean isInside( int i, int hw, int w, int h ) {
        int r = i / hw;
        int c = i % hw;
        return r >= 1 && c >= 1 && r <= h && c <= w;
    }

    private PriorityFloodTile getTile( int col, int row ) {
        return tiles[(row / tileSize) * tileCols + col / tileSize];
    }

    private TileFlats getFlats( int tileIndex ) {
        TileFlats flats = tilesFlats[tileIndex];
        if (flats == null) {
            // tiles of only novalues are never analyzed
            flats = new TileFlats();
            flats.drainsInTile = new boolean[0];
            tilesFlats[tileIndex] = flats;
        }
        return flats;
    }

    private int getPerimeterStatus( int col, int row ) {
        PriorityFloodTile tile = getTile(col, row);
        TileFlats flats = tilesFlats[tile.tile.getIndex()];
        if (flats == null || flats.topStatus == null) {
            return P_INVALID;
        }
        if (row == tile.r0) {
            return flats.topStatus[col - tile.c0];
        } else if (row == tile.r0 + tile.h - 1) {
            return flats.bottomStatus[col - tile.c0];
        } else if (col == tile.c0) {
            return flats.leftStatus[row - tile.r0];
        } else {
            return flats.rightStatus[row - tile.r0];
        }
    }

    private int globalPiece( int col, int row ) {
        PriorityFloodTile tile = getTile(col, row);
        return tilesFlats[tile.tile.getIndex()].pieceOffset + getPerimeterStatus(col, row);
    }

    private void drainTo( boolean[] done, ArrayDeque<Integer> queue, int col, int row, int toCol, int toRow ) {
        int piece = globalPiece(col, row);
        if (!done[piece]) {
            done[piece] = true;
            setExit(col, row, toCol, toRow);
            queue.add(piece);
        }
    }

    /**
     * Set the exit of the piece of a perimeter cell, flowing into an adjacent cell of another tile.
     */
    private void setExit( int col, int row, int toCol, int toRow ) {
        PriorityFloodTile tile = getTile(col, row);
        TileFlats flats = tilesFlats[tile.tile.getIndex()];
        int localPiece = getPerimeterStatus(col, row);
        flats.exitCells[localPiece] = (row - tile.r0) * tile.w + col - tile.c0;
        int dc = toCol - col;
        int dr = toRow - row;
        for( int k = 0; k < 8; k++ ) {
            if (DCOLS[k] == dc && DROWS[k] == dr) {
                flats.exitCodes[localPiece] = k + 1;
                return;
            }
        }
        throw new IllegalStateException("Cells are not adjacent: " + col + "/" + row + " - " + toCol + "/" + toRow);
    }
}
