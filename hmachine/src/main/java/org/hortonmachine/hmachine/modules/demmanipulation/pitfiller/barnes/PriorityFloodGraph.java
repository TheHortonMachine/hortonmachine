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

import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes.PriorityFloodTile.OCEAN;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.barnes.PriorityFloodTile.CellHeap;

/**
 * The global spill graph of the parallel Priority-Flood by Barnes (2016).
 *
 * <p>It joins the label graphs of all the tiles along the tile seams and solves, for each
 * label, the lowest elevation at which it can spill into the ocean.</p>
 * 
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author. Verified against a sequential Priority-Flood,
 * OmsPitfiller, OmsFloDirection, OmsTca and OmsExtractNetwork.</p>
 */
public class PriorityFloodGraph {

    private final PriorityFloodTile[] tiles;
    private final int tileCols;
    private final int tileRows;
    private final int tileSize;

    private int[] from = new int[1024];
    private int[] to = new int[1024];
    private double[] elevation = new double[1024];
    private int size = 0;

    /**
     * The seam edges of the current tile, deduplicated keeping the lowest elevation.
     */
    private final Map<Long, Double> seamEdges = new HashMap<>();

    /**
     * @param tiles the flooded tiles, in row-major order.
     * @param tileCols the number of tile columns.
     * @param tileRows the number of tile rows.
     * @param tileSize the size of the side of the tiles.
     */
    public PriorityFloodGraph( PriorityFloodTile[] tiles, int tileCols, int tileRows, int tileSize ) {
        this.tiles = tiles;
        this.tileCols = tileCols;
        this.tileRows = tileRows;
        this.tileSize = tileSize;
    }

    /**
     * Solve the spill elevations.
     *
     * @param releasePerimeters if <code>true</code>, the perimeter data of the tiles are released
     *          once used. They are needed later to compute flow directions.
     * @return the spill elevations, indexed by global label.
     */
    public double[] solve( boolean releasePerimeters ) {
        // global labels: 0 is the ocean, then each tile gets its own range
        int labelsCount = 1;
        for( PriorityFloodTile tile : tiles ) {
            tile.labelOffset = labelsCount - 1;
            labelsCount += tile.labelsCount;
        }

        for( PriorityFloodTile tile : tiles ) {
            for( int e = 0; e < tile.edgeLabels1.length; e++ ) {
                add(tile.toGlobal(tile.edgeLabels1[e]), tile.toGlobal(tile.edgeLabels2[e]), tile.edgeElevs[e]);
            }
        }
        visitSeams(tiles, tileCols, tileRows, this::joinCells, () -> {
            for( Map.Entry<Long, Double> entry : seamEdges.entrySet() ) {
                long key = entry.getKey();
                add((int) (key >>> 32), (int) key, entry.getValue());
            }
            seamEdges.clear();
        });
        for( PriorityFloodTile tile : tiles ) {
            tile.releaseEdges();
            if (releasePerimeters) {
                tile.releaseGraphData();
            }
        }

        /*
         * minimax Dijkstra from the ocean: the spill elevation of a label is the lowest
         * possible maximum edge elevation on a path to the ocean.
         */
        int[] adjacencyStart = new int[labelsCount + 1];
        for( int e = 0; e < size; e++ ) {
            adjacencyStart[from[e] + 1]++;
            adjacencyStart[to[e] + 1]++;
        }
        for( int l = 0; l < labelsCount; l++ ) {
            adjacencyStart[l + 1] += adjacencyStart[l];
        }
        int[] adjacencyLabels = new int[adjacencyStart[labelsCount]];
        double[] adjacencyElevs = new double[adjacencyStart[labelsCount]];
        int[] fill = Arrays.copyOf(adjacencyStart, labelsCount);
        for( int e = 0; e < size; e++ ) {
            adjacencyLabels[fill[from[e]]] = to[e];
            adjacencyElevs[fill[from[e]]++] = elevation[e];
            adjacencyLabels[fill[to[e]]] = from[e];
            adjacencyElevs[fill[to[e]]++] = elevation[e];
        }
        from = to = null;
        elevation = null;

        double[] spill = new double[labelsCount];
        Arrays.fill(spill, Double.POSITIVE_INFINITY);
        spill[OCEAN] = Double.NEGATIVE_INFINITY;
        boolean[] done = new boolean[labelsCount];
        CellHeap heap = new CellHeap(1024);
        heap.push(OCEAN, spill[OCEAN]);
        while( !heap.isEmpty() ) {
            int label = heap.pop();
            if (done[label]) {
                continue;
            }
            done[label] = true;
            for( int a = adjacencyStart[label]; a < adjacencyStart[label + 1]; a++ ) {
                int other = adjacencyLabels[a];
                double candidate = Math.max(spill[label], adjacencyElevs[a]);
                if (candidate < spill[other]) {
                    spill[other] = candidate;
                    heap.push(other, candidate);
                }
            }
        }
        return spill;
    }

    /**
     * Visitor of the pairs of adjacent cells across the tile seams.
     */
    @FunctionalInterface
    interface SeamVisitor {
        void visit( int col1, int row1, int col2, int row2 );
    }

    /**
     * Visit every pair of 8-connected cells that lie in different tiles, once.
     *
     * @param tiles the tiles in row-major order.
     * @param tileCols the number of tile columns.
     * @param tileRows the number of tile rows.
     * @param visitor the visitor of the pairs.
     * @param afterTile optional action run after the pairs of each tile.
     */
    static void visitSeams( PriorityFloodTile[] tiles, int tileCols, int tileRows, SeamVisitor visitor, Runnable afterTile ) {
        for( int tr = 0; tr < tileRows; tr++ ) {
            for( int tc = 0; tc < tileCols; tc++ ) {
                PriorityFloodTile tile = tiles[tr * tileCols + tc];
                int lastCol = tile.c0 + tile.w - 1;
                int lastRow = tile.r0 + tile.h - 1;
                if (tc + 1 < tileCols) {
                    // right neighbour
                    for( int row = tile.r0; row <= lastRow; row++ ) {
                        for( int dr = -1; dr <= 1; dr++ ) {
                            int otherRow = row + dr;
                            if (otherRow >= tile.r0 && otherRow <= lastRow) {
                                visitor.visit(lastCol, row, lastCol + 1, otherRow);
                            }
                        }
                    }
                }
                if (tr + 1 < tileRows) {
                    // bottom neighbour
                    for( int col = tile.c0; col <= lastCol; col++ ) {
                        for( int dc = -1; dc <= 1; dc++ ) {
                            int otherCol = col + dc;
                            if (otherCol >= tile.c0 && otherCol <= lastCol) {
                                visitor.visit(col, lastRow, otherCol, lastRow + 1);
                            }
                        }
                    }
                    if (tc + 1 < tileCols) {
                        // bottom right corner
                        visitor.visit(lastCol, lastRow, lastCol + 1, lastRow + 1);
                    }
                    if (tc > 0) {
                        // bottom left corner
                        visitor.visit(tile.c0, lastRow, tile.c0 - 1, lastRow + 1);
                    }
                }
                if (afterTile != null) {
                    afterTile.run();
                }
            }
        }
    }

    private void joinCells( int col1, int row1, int col2, int row2 ) {
        PriorityFloodTile tile1 = getTile(col1, row1);
        PriorityFloodTile tile2 = getTile(col2, row2);
        int label1 = tile1.toGlobal(tile1.getPerimeterLabel(col1, row1));
        int label2 = tile2.toGlobal(tile2.getPerimeterLabel(col2, row2));
        if (label1 == label2) {
            return;
        }
        if (label1 == OCEAN) {
            // the valid cell touches a novalue of the other tile, so it is an outlet
            addSeamEdge(label2, OCEAN, tile2.getPerimeterElev(col2, row2));
        } else if (label2 == OCEAN) {
            addSeamEdge(label1, OCEAN, tile1.getPerimeterElev(col1, row1));
        } else {
            addSeamEdge(label1, label2, Math.max(tile1.getPerimeterElev(col1, row1), tile2.getPerimeterElev(col2, row2)));
        }
    }

    private void addSeamEdge( int label1, int label2, double elev ) {
        long key = ((long) label1 << 32) | (label2 & 0xFFFFFFFFL);
        Double existing = seamEdges.get(key);
        if (existing == null || elev < existing) {
            seamEdges.put(key, elev);
        }
    }

    private PriorityFloodTile getTile( int col, int row ) {
        return tiles[(row / tileSize) * tileCols + col / tileSize];
    }

    private void add( int label1, int label2, double elev ) {
        if (size == from.length) {
            int newCapacity = from.length * 2;
            from = Arrays.copyOf(from, newCapacity);
            to = Arrays.copyOf(to, newCapacity);
            elevation = Arrays.copyOf(elevation, newCapacity);
        }
        from[size] = label1;
        to[size] = label2;
        elevation[size] = elev;
        size++;
    }
}
