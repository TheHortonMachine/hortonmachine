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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTile;

/**
 * A tile of the parallel Priority-Flood by Barnes (2016) and its labelling flood.
 *
 * <p>The tile keeps only what is needed to solve the global spill graph: the labels and
 * elevations of its perimeter and the spill edges between its labels. The full labels and
 * tile-filled elevations are returned by {@link #flood(double[])} and can be either retained
 * or recomputed later, since the flood is deterministic.</p>
 *
 * <p>Novalues are expected as {@link Double#NaN} in the tile values.</p>
 * 
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author. Verified against a sequential Priority-Flood,
 * OmsPitfiller, OmsFloDirection, OmsTca and OmsExtractNetwork.</p>
 */
public class PriorityFloodTile {
    /**
     * The local and global label of the "ocean", i.e. everything outside the valid data.
     */
    static final int OCEAN = 0;

    private static final int[] DCOLS = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DROWS = {0, -1, -1, -1, 0, 1, 1, 1};

    final HMRasterTile tile;
    public final int c0;
    public final int r0;
    public final int w;
    public final int h;

    /**
     * The global cols and rows of the DEM.
     */
    private final int cols;
    private final int rows;

    /**
     * <code>true</code> if the tile contains only novalues.
     */
    boolean isEmpty;
    /**
     * <code>true</code> once the tile has been flooded the first time.
     */
    private boolean isFlooded;
    /**
     * The number of valid cells of the tile.
     */
    long validCount;
    int labelsCount;
    int labelOffset;

    int[] edgeLabels1;
    int[] edgeLabels2;
    double[] edgeElevs;

    int[] topLabels;
    int[] bottomLabels;
    int[] leftLabels;
    int[] rightLabels;
    double[] topElevs;
    double[] bottomElevs;
    double[] leftElevs;
    double[] rightElevs;

    /**
     * The result of the flood of a tile.
     */
    public static class FloodResult {
        /**
         * The tile-filled elevations, NaN for novalues.
         */
        public double[] elevs;
        /**
         * The local labels, {@link PriorityFloodTile#OCEAN} for novalues.
         */
        public int[] labels;
    }

    /**
     * @param tile the tile of the raster grid.
     * @param cols the cols of the whole raster.
     * @param rows the rows of the whole raster.
     */
    public PriorityFloodTile( HMRasterTile tile, int cols, int rows ) {
        this.tile = tile;
        this.c0 = tile.getCol();
        this.r0 = tile.getRow();
        this.w = tile.getWidth();
        this.h = tile.getHeight();
        this.cols = cols;
        this.rows = rows;
    }

    /**
     * Labelling Priority-Flood of the tile.
     *
     * <p>The flood is seeded from the tile perimeter and from the cells that touch novalues
     * or the DEM border inside the tile. Every seed popped without a label starts a new label.
     * Where two labels touch, a spill edge is recorded. Cells that drain outside the valid data
     * get an edge to the ocean. Perimeter cells that touch novalues of a neighbour tile are
     * connected to the ocean when the tiles are joined, since the tile does not see them.</p>
     *
     * <p>On the first call the perimeter and edges of the tile are stored. Any later call
     * (for example to recompute the labels) has to produce the same labels count.</p>
     *
     * @param values the tile values in row-major order, NaN for novalues. They are not modified.
     * @return the tile-filled elevations and the labels.
     */
    public FloodResult flood( double[] values ) {
        // work on a grid with a 1 cell halo, so that neighbours never need bound checks
        int hw = w + 2;
        int hh = h + 2;
        double[] elev = new double[hw * hh];
        boolean[] valid = new boolean[hw * hh];
        long valids = 0;
        for( int r = 0; r < h; r++ ) {
            for( int c = 0; c < w; c++ ) {
                double value = values[r * w + c];
                if (!Double.isNaN(value)) {
                    int i = (r + 1) * hw + c + 1;
                    elev[i] = value;
                    valid[i] = true;
                    valids++;
                }
            }
        }
        boolean hasValid = valids > 0;
        validCount = valids;
        FloodResult result = new FloodResult();
        if (!hasValid) {
            isEmpty = true;
            result.elevs = values.clone();
            result.labels = new int[w * h];
            if (!isFlooded) {
                isFlooded = true;
                edgeLabels1 = new int[0];
                edgeLabels2 = new int[0];
                edgeElevs = new double[0];
            }
            return result;
        }

        int[] neighbourOffsets = new int[8];
        for( int k = 0; k < 8; k++ ) {
            neighbourOffsets[k] = DROWS[k] * hw + DCOLS[k];
        }

        // halo cells inside the DEM belong to other tiles: here they count as valid, the join handles them
        boolean[] haloValid = valid.clone();
        for( int r = 0; r < hh; r++ ) {
            for( int c = 0; c < hw; c++ ) {
                if (r == 0 || c == 0 || r == hh - 1 || c == hw - 1) {
                    int col = c0 + c - 1;
                    int row = r0 + r - 1;
                    haloValid[r * hw + c] = col >= 0 && row >= 0 && col < cols && row < rows;
                }
            }
        }

        int[] labels = new int[hw * hh];
        boolean[] queued = new boolean[hw * hh];
        boolean[] outlet = new boolean[hw * hh];
        CellHeap heap = new CellHeap(4 * (w + h));
        IntFifo pitQueue = new IntFifo(1024);

        for( int r = 1; r <= h; r++ ) {
            for( int c = 1; c <= w; c++ ) {
                int i = r * hw + c;
                if (!valid[i]) {
                    continue;
                }
                boolean isOutlet = false;
                for( int k = 0; k < 8; k++ ) {
                    if (!haloValid[i + neighbourOffsets[k]]) {
                        isOutlet = true;
                        break;
                    }
                }
                boolean onPerimeter = r == 1 || c == 1 || r == h || c == w;
                if (isOutlet || onPerimeter) {
                    outlet[i] = isOutlet;
                    queued[i] = true;
                    heap.push(i, elev[i]);
                }
            }
        }

        int nextLabel = 0;
        Map<Long, Double> edges = new HashMap<>();
        while( !pitQueue.isEmpty() || !heap.isEmpty() ) {
            int current;
            if (!pitQueue.isEmpty()) {
                current = pitQueue.pop();
            } else {
                current = heap.pop();
            }
            if (labels[current] == 0) {
                labels[current] = ++nextLabel;
            }
            int currentLabel = labels[current];
            double currentElev = elev[current];
            if (outlet[current]) {
                addEdge(edges, currentLabel, OCEAN, currentElev);
            }

            for( int k = 0; k < 8; k++ ) {
                int n = current + neighbourOffsets[k];
                if (!valid[n]) {
                    // novalue or halo
                    continue;
                }
                if (labels[n] != 0) {
                    if (labels[n] != currentLabel) {
                        addEdge(edges, currentLabel, labels[n], Math.max(currentElev, elev[n]));
                    }
                    continue;
                }
                labels[n] = currentLabel;
                if (queued[n]) {
                    // a seed still waiting in the heap, it is never raised
                    continue;
                }
                queued[n] = true;
                if (elev[n] <= currentElev) {
                    elev[n] = currentElev;
                    pitQueue.push(n);
                } else {
                    heap.push(n, elev[n]);
                }
            }
        }

        result.elevs = new double[w * h];
        result.labels = new int[w * h];
        for( int r = 0; r < h; r++ ) {
            for( int c = 0; c < w; c++ ) {
                int i = (r + 1) * hw + c + 1;
                result.labels[r * w + c] = labels[i];
                result.elevs[r * w + c] = valid[i] ? elev[i] : Double.NaN;
            }
        }

        if (!isFlooded) {
            isFlooded = true;
            labelsCount = nextLabel;
            storeEdges(edges);
            storePerimeter(result);
        } else if (labelsCount != nextLabel) {
            throw new IllegalStateException("The flood of tile " + c0 + "/" + r0 + " is not reproducible: " + labelsCount
                    + " labels before, " + nextLabel + " now.");
        }
        return result;
    }

    private void storeEdges( Map<Long, Double> edges ) {
        edgeLabels1 = new int[edges.size()];
        edgeLabels2 = new int[edges.size()];
        edgeElevs = new double[edges.size()];
        int index = 0;
        for( Map.Entry<Long, Double> entry : edges.entrySet() ) {
            long key = entry.getKey();
            edgeLabels1[index] = (int) (key >>> 32);
            edgeLabels2[index] = (int) key;
            edgeElevs[index] = entry.getValue();
            index++;
        }
    }

    private void storePerimeter( FloodResult result ) {
        // perimeter cells are seeds, their elevation is never changed by the tile flood
        topLabels = new int[w];
        bottomLabels = new int[w];
        topElevs = new double[w];
        bottomElevs = new double[w];
        for( int c = 0; c < w; c++ ) {
            topLabels[c] = result.labels[c];
            topElevs[c] = result.elevs[c];
            bottomLabels[c] = result.labels[(h - 1) * w + c];
            bottomElevs[c] = result.elevs[(h - 1) * w + c];
        }
        leftLabels = new int[h];
        rightLabels = new int[h];
        leftElevs = new double[h];
        rightElevs = new double[h];
        for( int r = 0; r < h; r++ ) {
            leftLabels[r] = result.labels[r * w];
            leftElevs[r] = result.elevs[r * w];
            rightLabels[r] = result.labels[r * w + w - 1];
            rightElevs[r] = result.elevs[r * w + w - 1];
        }
    }

    /**
     * @return the local label of a perimeter cell of the tile.
     */
    int getPerimeterLabel( int col, int row ) {
        if (isEmpty) {
            return OCEAN;
        }
        if (row == r0) {
            return topLabels[col - c0];
        } else if (row == r0 + h - 1) {
            return bottomLabels[col - c0];
        } else if (col == c0) {
            return leftLabels[row - r0];
        } else {
            return rightLabels[row - r0];
        }
    }

    /**
     * @return the elevation of a perimeter cell of the tile.
     */
    double getPerimeterElev( int col, int row ) {
        if (row == r0) {
            return topElevs[col - c0];
        } else if (row == r0 + h - 1) {
            return bottomElevs[col - c0];
        } else if (col == c0) {
            return leftElevs[row - r0];
        } else {
            return rightElevs[row - r0];
        }
    }

    /**
     * @return the global label for a local label of this tile.
     */
    int toGlobal( int localLabel ) {
        return localLabel == OCEAN ? OCEAN : labelOffset + localLabel;
    }

    /**
     * Release the spill edges, once the graph has been built.
     */
    void releaseEdges() {
        edgeLabels1 = null;
        edgeLabels2 = null;
        edgeElevs = null;
    }

    /**
     * Get the final elevation of a perimeter cell, i.e. after the raise to the spill elevations.
     * 
     * @return the elevation or NaN for novalues.
     */
    double getFinalPerimeterElev( int col, int row, double[] spillElevations ) {
        int label = getPerimeterLabel(col, row);
        if (label == OCEAN) {
            return Double.NaN;
        }
        double elev = getPerimeterElev(col, row);
        double spill = spillElevations[toGlobal(label)];
        return !Double.isInfinite(spill) && spill > elev ? spill : elev;
    }

    /**
     * Release the perimeter data, once they are no longer needed.
     */
    void releaseGraphData() {
        releaseEdges();
        topLabels = bottomLabels = leftLabels = rightLabels = null;
        topElevs = bottomElevs = leftElevs = rightElevs = null;
    }

    /**
     * @return <code>true</code> if the tile contains only novalues, known after the first flood.
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * @return the number of valid cells of the tile, known after the first flood.
     */
    public long getValidCount() {
        return validCount;
    }

    /**
     * Raise the tile-filled elevations to the spill elevation of their label.
     *
     * @param result the flood result of this tile, modified in place.
     * @param spillElevations the spill elevations indexed by global label.
     */
    public void raise( FloodResult result, double[] spillElevations ) {
        double[] elevs = result.elevs;
        int[] labels = result.labels;
        for( int i = 0; i < elevs.length; i++ ) {
            if (labels[i] == OCEAN) {
                continue;
            }
            double spill = spillElevations[toGlobal(labels[i])];
            if (!Double.isInfinite(spill) && elevs[i] < spill) {
                elevs[i] = spill;
            }
        }
    }

    private static void addEdge( Map<Long, Double> edges, int label1, int label2, double elevation ) {
        int min = Math.min(label1, label2);
        int max = Math.max(label1, label2);
        long key = ((long) min << 32) | (max & 0xFFFFFFFFL);
        Double existing = edges.get(key);
        if (existing == null || elevation < existing) {
            edges.put(key, elevation);
        }
    }

    /**
     * Binary min-heap of int ids ordered by elevation, with insertion order as tie breaker.
     */
    static class CellHeap {
        private int[] ids;
        private double[] keys;
        private long[] order;
        private int size = 0;
        private long counter = 0;

        CellHeap( int capacity ) {
            capacity = Math.max(16, capacity);
            ids = new int[capacity];
            keys = new double[capacity];
            order = new long[capacity];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void push( int id, double key ) {
            if (size == ids.length) {
                int newCapacity = ids.length * 2;
                ids = Arrays.copyOf(ids, newCapacity);
                keys = Arrays.copyOf(keys, newCapacity);
                order = Arrays.copyOf(order, newCapacity);
            }
            int i = size++;
            long o = counter++;
            while( i > 0 ) {
                int parent = (i - 1) >>> 1;
                if (!less(key, o, keys[parent], order[parent])) {
                    break;
                }
                set(i, ids[parent], keys[parent], order[parent]);
                i = parent;
            }
            set(i, id, key, o);
        }

        int pop() {
            int result = ids[0];
            size--;
            if (size > 0) {
                int id = ids[size];
                double key = keys[size];
                long o = order[size];
                int i = 0;
                while( true ) {
                    int child = 2 * i + 1;
                    if (child >= size) {
                        break;
                    }
                    if (child + 1 < size && less(keys[child + 1], order[child + 1], keys[child], order[child])) {
                        child++;
                    }
                    if (!less(keys[child], order[child], key, o)) {
                        break;
                    }
                    set(i, ids[child], keys[child], order[child]);
                    i = child;
                }
                set(i, id, key, o);
            }
            return result;
        }

        private static boolean less( double key1, long order1, double key2, long order2 ) {
            return key1 < key2 || (key1 == key2 && order1 < order2);
        }

        private void set( int i, int id, double key, long o ) {
            ids[i] = id;
            keys[i] = key;
            order[i] = o;
        }
    }

    /**
     * Growable ring buffer of ints.
     */
    private static class IntFifo {
        private int[] data;
        private int head = 0;
        private int size = 0;

        IntFifo( int capacity ) {
            data = new int[capacity];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void push( int value ) {
            if (size == data.length) {
                int[] newData = new int[data.length * 2];
                for( int i = 0; i < size; i++ ) {
                    newData[i] = data[(head + i) % data.length];
                }
                data = newData;
                head = 0;
            }
            data[(head + size) % data.length] = value;
            size++;
        }

        int pop() {
            int value = data[head];
            head = (head + 1) % data.length;
            size--;
            return value;
        }
    }
}
