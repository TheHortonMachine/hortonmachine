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
package org.hortonmachine.gears.libs.modules.hmraster;

import java.util.ArrayList;
import java.util.List;

/**
 * A rectangular tile of a raster grid.
 *
 * <p>Tiles are created by {@link #createGrid(int, int, int)}: they cover the raster in
 * row-major order, all of them of the same size except the last column and row, which are
 * clipped to the raster bounds.</p>
 */
public final class HMRasterTile {
    private final int index;
    private final int tileCol;
    private final int tileRow;
    private final int col;
    private final int row;
    private final int width;
    private final int height;

    private HMRasterTile( int index, int tileCol, int tileRow, int col, int row, int width, int height ) {
        this.index = index;
        this.tileCol = tileCol;
        this.tileRow = tileRow;
        this.col = col;
        this.row = row;
        this.width = width;
        this.height = height;
    }

    /**
     * Create the tiles covering a raster grid.
     *
     * @param cols the cols of the raster.
     * @param rows the rows of the raster.
     * @param tileSize the size of the side of the tiles.
     * @return the tiles in row-major order.
     */
    public static List<HMRasterTile> createGrid( int cols, int rows, int tileSize ) {
        if (tileSize < 1) {
            throw new IllegalArgumentException("The tile size has to be positive.");
        }
        int tileCols = getTilesCount(cols, tileSize);
        int tileRows = getTilesCount(rows, tileSize);
        List<HMRasterTile> tiles = new ArrayList<>(tileCols * tileRows);
        for( int tr = 0; tr < tileRows; tr++ ) {
            for( int tc = 0; tc < tileCols; tc++ ) {
                int col = tc * tileSize;
                int row = tr * tileSize;
                tiles.add(new HMRasterTile(tiles.size(), tc, tr, col, row, Math.min(tileSize, cols - col),
                        Math.min(tileSize, rows - row)));
            }
        }
        return tiles;
    }

    /**
     * @return the number of tiles needed to cover a size.
     */
    public static int getTilesCount( int size, int tileSize ) {
        return (size + tileSize - 1) / tileSize;
    }

    /**
     * @return the index of the tile in the row-major tiles list.
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the column of the tile in the tiles grid.
     */
    public int getTileCol() {
        return tileCol;
    }

    /**
     * @return the row of the tile in the tiles grid.
     */
    public int getTileRow() {
        return tileRow;
    }

    /**
     * @return the first raster col of the tile.
     */
    public int getCol() {
        return col;
    }

    /**
     * @return the first raster row of the tile.
     */
    public int getRow() {
        return row;
    }

    /**
     * @return the number of cols of the tile.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the number of rows of the tile.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return <code>true</code> if the raster cell is inside the tile.
     */
    public boolean contains( int col, int row ) {
        return col >= this.col && row >= this.row && col < this.col + width && row < this.row + height;
    }

    @Override
    public String toString() {
        return "HMRasterTile[" + index + ": col=" + col + ", row=" + row + ", " + width + "x" + height + "]";
    }
}
