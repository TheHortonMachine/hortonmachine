/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.stone;

import java.util.Arrays;

/**
 * The trajectory of a single boulder of the STONE simulation.
 *
 * <p>The points are the recorded points of the trajectory, the same that
 * update the output maps. Coordinates are in the CRS of the input maps, the z is the absolute
 * elevation of the boulder.</p>
 */
public class StoneTrajectory {
    private final int sourceRow;
    private final int sourceCol;
    private final int boulder;
    private double[] xs = new double[32];
    private double[] ys = new double[32];
    private double[] zs = new double[32];
    private double[] speeds = new double[32];
    private double[] heights = new double[32];
    private int size;

    /**
     * @param sourceRow the row of the source cell.
     * @param sourceCol the col of the source cell.
     * @param boulder the number of the boulder in the source cell, from 1.
     */
    StoneTrajectory( int sourceRow, int sourceCol, int boulder ) {
        this.sourceRow = sourceRow;
        this.sourceCol = sourceCol;
        this.boulder = boulder;
    }

    void add( double x, double y, double z, double speed, double height ) {
        // the impact point ends a flight and starts the next one
        if (size > 0 && xs[size - 1] == x && ys[size - 1] == y && zs[size - 1] == z) {
            return;
        }
        if (size == xs.length) {
            int newLength = size * 2;
            xs = Arrays.copyOf(xs, newLength);
            ys = Arrays.copyOf(ys, newLength);
            zs = Arrays.copyOf(zs, newLength);
            speeds = Arrays.copyOf(speeds, newLength);
            heights = Arrays.copyOf(heights, newLength);
        }
        xs[size] = x;
        ys[size] = y;
        zs[size] = z;
        speeds[size] = speed;
        heights[size] = height;
        size++;
    }

    /**
     * Move the local coordinates of the simulation to the ones of the maps.
     */
    void translate( double dx, double dy ) {
        for( int i = 0; i < size; i++ ) {
            xs[i] += dx;
            ys[i] += dy;
        }
    }

    public int getSourceRow() {
        return sourceRow;
    }

    public int getSourceCol() {
        return sourceCol;
    }

    public int getBoulder() {
        return boulder;
    }

    /**
     * @return the number of points.
     */
    public int size() {
        return size;
    }

    public double getX( int index ) {
        return xs[index];
    }

    public double getY( int index ) {
        return ys[index];
    }

    /**
     * @return the absolute elevation of the boulder at the point.
     */
    public double getZ( int index ) {
        return zs[index];
    }

    /**
     * @return the velocity of the boulder at the point [m/s].
     */
    public double getSpeed( int index ) {
        return speeds[index];
    }

    /**
     * @return the height of the boulder over the terrain at the point [m].
     */
    public double getHeight( int index ) {
        return heights[index];
    }

    public double getMaxSpeed() {
        double max = 0;
        for( int i = 0; i < size; i++ ) {
            max = Math.max(max, speeds[i]);
        }
        return max;
    }

    public double getMaxHeight() {
        double max = 0;
        for( int i = 0; i < size; i++ ) {
            max = Math.max(max, heights[i]);
        }
        return max;
    }
}
