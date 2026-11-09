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
package org.hortonmachine.gears.io.las.utils;

import java.util.Arrays;

import org.hortonmachine.gears.io.las.core.LasRecord;

/**
 * Many las points kept as columns of primitive arrays, one per attribute, instead of a
 * {@link LasRecord} object per point.
 *
 * <p>A point takes 48 bytes, against about 100 of a {@link LasRecord} in a list, which matters
 * with tens of millions of points. The point density of the records is not kept.</p>
 *
 * <p>The arrays are public for fast access, valid up to {@link #size()}, and can be longer.</p>
 */
public class LasPointColumns {

    public double[] x;
    public double[] y;
    public double[] z;
    public short[] intensity;
    public byte[] returnNumber;
    public byte[] numberOfReturns;
    public short[] classification;
    public double[] gpsTime;
    /** The colors, as in the las file: 8 or 16 bits. */
    public short[] red;
    public short[] green;
    public short[] blue;
    /** The height over the ground, as in {@link LasRecord#groundElevation}, NaN if not known. */
    public float[] groundHeight;

    private int size = 0;

    /**
     * @param initialCapacity the number of points expected, the arrays grow if needed.
     */
    public LasPointColumns( int initialCapacity ) {
        allocate(Math.max(16, initialCapacity));
    }

    private void allocate( int capacity ) {
        x = new double[capacity];
        y = new double[capacity];
        z = new double[capacity];
        intensity = new short[capacity];
        returnNumber = new byte[capacity];
        numberOfReturns = new byte[capacity];
        classification = new short[capacity];
        gpsTime = new double[capacity];
        red = new short[capacity];
        green = new short[capacity];
        blue = new short[capacity];
        groundHeight = new float[capacity];
    }

    /**
     * @return the number of points.
     */
    public int size() {
        return size;
    }

    /**
     * @return the number of points the arrays can hold without growing.
     */
    public int capacity() {
        return x.length;
    }

    /**
     * Add a point.
     *
     * @param record the point.
     */
    public void add( LasRecord record ) {
        ensureCapacity(size + 1);
        int i = size;
        x[i] = record.x;
        y[i] = record.y;
        z[i] = record.z;
        intensity[i] = record.intensity;
        returnNumber[i] = (byte) record.returnNumber;
        numberOfReturns[i] = (byte) record.numberOfReturns;
        classification[i] = record.classification;
        gpsTime[i] = record.gpsTime;
        if (record.color != null) {
            red[i] = record.color[0];
            green[i] = record.color[1];
            blue[i] = record.color[2];
        }
        groundHeight[i] = (float) record.groundElevation;
        size++;
    }

    /**
     * Add the point of another set of columns.
     *
     * @param other the other columns.
     * @param index the index of the point in the other columns.
     */
    public void add( LasPointColumns other, int index ) {
        ensureCapacity(size + 1);
        int i = size;
        x[i] = other.x[index];
        y[i] = other.y[index];
        z[i] = other.z[index];
        intensity[i] = other.intensity[index];
        returnNumber[i] = other.returnNumber[index];
        numberOfReturns[i] = other.numberOfReturns[index];
        classification[i] = other.classification[index];
        gpsTime[i] = other.gpsTime[index];
        red[i] = other.red[index];
        green[i] = other.green[index];
        blue[i] = other.blue[index];
        groundHeight[i] = other.groundHeight[index];
        size++;
    }

    /**
     * @param index the index of the point.
     * @return a new record with the values of the point.
     */
    public LasRecord get( int index ) {
        LasRecord record = new LasRecord();
        record.x = x[index];
        record.y = y[index];
        record.z = z[index];
        record.intensity = intensity[index];
        record.returnNumber = returnNumber[index];
        record.numberOfReturns = numberOfReturns[index];
        record.classification = classification[index];
        record.gpsTime = gpsTime[index];
        record.color = new short[]{red[index], green[index], blue[index]};
        record.groundElevation = groundHeight[index];
        return record;
    }

    /**
     * Free the memory of the arrays beyond the points, if it is more than a tenth.
     */
    public void trimToSize() {
        if (size < capacity() * 0.9) {
            resize(Math.max(16, size));
        }
    }

    private void ensureCapacity( int needed ) {
        if (needed > capacity()) {
            long newCapacity = Math.max(needed, capacity() + (capacity() >> 1));
            resize((int) Math.min(Integer.MAX_VALUE - 8, newCapacity));
        }
    }

    private void resize( int capacity ) {
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        z = Arrays.copyOf(z, capacity);
        intensity = Arrays.copyOf(intensity, capacity);
        returnNumber = Arrays.copyOf(returnNumber, capacity);
        numberOfReturns = Arrays.copyOf(numberOfReturns, capacity);
        classification = Arrays.copyOf(classification, capacity);
        gpsTime = Arrays.copyOf(gpsTime, capacity);
        red = Arrays.copyOf(red, capacity);
        green = Arrays.copyOf(green, capacity);
        blue = Arrays.copyOf(blue, capacity);
        groundHeight = Arrays.copyOf(groundHeight, capacity);
    }
}
