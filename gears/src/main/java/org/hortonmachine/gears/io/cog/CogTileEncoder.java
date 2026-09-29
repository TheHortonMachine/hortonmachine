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
package org.hortonmachine.gears.io.cog;

import java.awt.image.DataBuffer;
import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

/**
 * Encoding of a single TIFF tile: packing of the samples in little endian order,
 * optional predictor and lossless compression (Deflate or LZW).
 *
 * <p>Instances are not thread safe, use one per thread.</p>
 */
class CogTileEncoder {
    /**
     * TIFF compression codes.
     */
    static final int COMPRESSION_LZW = 5;
    static final int COMPRESSION_DEFLATE = 8;

    /**
     * TIFF predictor codes.
     */
    static final int PREDICTOR_NONE = 1;
    static final int PREDICTOR_HORIZONTAL = 2;
    static final int PREDICTOR_FLOATING_POINT = 3;

    private final int dataType;
    private final int tileSize;
    private final int bytesPerSample;
    private final int compression;
    private final int predictor;
    private final double fillValue;
    private final Deflater deflater;
    private final byte[] rowBuffer;

    /**
     * @param dataType the {@link DataBuffer} data type of the samples.
     * @param tileSize the side of the square tiles.
     * @param compression {@link #COMPRESSION_DEFLATE} or {@link #COMPRESSION_LZW}.
     * @param deflateLevel the deflate level, 1 to 9.
     * @param predictor the TIFF predictor code.
     * @param fillValue the value written for NaN samples of integer data types.
     */
    CogTileEncoder( int dataType, int tileSize, int compression, int deflateLevel, int predictor, double fillValue ) {
        this.dataType = dataType;
        this.tileSize = tileSize;
        this.bytesPerSample = bytesPerSample(dataType);
        this.compression = compression;
        this.predictor = predictor;
        this.fillValue = fillValue;
        this.deflater = compression == COMPRESSION_DEFLATE ? new Deflater(deflateLevel) : null;
        this.rowBuffer = new byte[tileSize * bytesPerSample];
    }

    /**
     * @return the size in bytes of a sample of the data type.
     */
    static int bytesPerSample( int dataType ) {
        switch( dataType ) {
        case DataBuffer.TYPE_BYTE:
            return 1;
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            return 2;
        case DataBuffer.TYPE_INT:
        case DataBuffer.TYPE_FLOAT:
            return 4;
        case DataBuffer.TYPE_DOUBLE:
            return 8;
        default:
            throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }
    }

    /**
     * @return <code>true</code> for floating point data types.
     */
    static boolean isFloatingPoint( int dataType ) {
        return dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE;
    }

    /**
     * Encode a tile.
     *
     * @param values the tileSize*tileSize values in row-major order, NaN for novalues.
     * @return the compressed bytes of the tile.
     */
    byte[] encode( double[] values ) {
        byte[] raw = new byte[tileSize * tileSize * bytesPerSample];
        int rowBytes = tileSize * bytesPerSample;
        for( int r = 0; r < tileSize; r++ ) {
            int offset = r * rowBytes;
            packRow(values, r * tileSize, raw, offset);
            if (predictor == PREDICTOR_HORIZONTAL) {
                horizontalDifferencing(raw, offset);
            } else if (predictor == PREDICTOR_FLOATING_POINT) {
                floatingPointDifferencing(raw, offset);
            }
        }
        if (compression == COMPRESSION_LZW) {
            return lzw(raw);
        }
        return deflate(raw);
    }

    /**
     * Pack a row of samples as little endian bytes.
     */
    private void packRow( double[] values, int from, byte[] raw, int offset ) {
        for( int c = 0; c < tileSize; c++ ) {
            double value = values[from + c];
            if (Double.isNaN(value) && !isFloatingPoint(dataType)) {
                // integers can't store NaN, floating point data keep it as it is
                value = fillValue;
            }
            int o = offset + c * bytesPerSample;
            switch( dataType ) {
            case DataBuffer.TYPE_BYTE:
                raw[o] = (byte) (int) value;
                break;
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT: {
                int v = (int) value;
                raw[o] = (byte) v;
                raw[o + 1] = (byte) (v >>> 8);
                break;
            }
            case DataBuffer.TYPE_INT:
                putInt(raw, o, (int) value);
                break;
            case DataBuffer.TYPE_FLOAT:
                putInt(raw, o, Float.floatToRawIntBits((float) value));
                break;
            case DataBuffer.TYPE_DOUBLE: {
                long v = Double.doubleToRawLongBits(value);
                putInt(raw, o, (int) v);
                putInt(raw, o + 4, (int) (v >>> 32));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
            }
        }
    }

    private static void putInt( byte[] b, int o, int v ) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >>> 8);
        b[o + 2] = (byte) (v >>> 16);
        b[o + 3] = (byte) (v >>> 24);
    }

    /**
     * TIFF predictor 2 on a row of little endian integer samples, on the values of the sample size.
     */
    private void horizontalDifferencing( byte[] raw, int offset ) {
        switch( bytesPerSample ) {
        case 1:
            for( int c = tileSize - 1; c > 0; c-- ) {
                raw[offset + c] = (byte) (raw[offset + c] - raw[offset + c - 1]);
            }
            break;
        case 2:
            for( int c = tileSize - 1; c > 0; c-- ) {
                int o = offset + 2 * c;
                int v = (raw[o] & 0xff) | ((raw[o + 1] & 0xff) << 8);
                int p = (raw[o - 2] & 0xff) | ((raw[o - 1] & 0xff) << 8);
                int d = v - p;
                raw[o] = (byte) d;
                raw[o + 1] = (byte) (d >>> 8);
            }
            break;
        case 4:
            for( int c = tileSize - 1; c > 0; c-- ) {
                int o = offset + 4 * c;
                putInt(raw, o, getInt(raw, o) - getInt(raw, o - 4));
            }
            break;
        default:
            throw new IllegalArgumentException("Horizontal predictor not supported for " + bytesPerSample + " bytes samples.");
        }
    }

    private static int getInt( byte[] b, int o ) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8) | ((b[o + 2] & 0xff) << 16) | ((b[o + 3] & 0xff) << 24);
    }

    /**
     * TIFF predictor 3 (as libtiff fpDiff) on a row of little endian floating point samples: the bytes
     * are regrouped by significance, most significant first, then differenced byte by byte.
     */
    private void floatingPointDifferencing( byte[] raw, int offset ) {
        int count = tileSize * bytesPerSample;
        System.arraycopy(raw, offset, rowBuffer, 0, count);
        for( int c = 0; c < tileSize; c++ ) {
            for( int b = 0; b < bytesPerSample; b++ ) {
                raw[offset + (bytesPerSample - b - 1) * tileSize + c] = rowBuffer[bytesPerSample * c + b];
            }
        }
        for( int i = count - 1; i > 0; i-- ) {
            raw[offset + i] = (byte) (raw[offset + i] - raw[offset + i - 1]);
        }
    }

    private byte[] deflate( byte[] raw ) {
        deflater.reset();
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, raw.length / 4));
        byte[] buffer = new byte[65536];
        while( !deflater.finished() ) {
            int n = deflater.deflate(buffer);
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static final int LZW_CLEAR = 256;
    private static final int LZW_EOI = 257;
    private static final int LZW_FIRST = 258;
    private static final int LZW_TABLE_FULL = 4094;

    /**
     * The LZW string table: child code of (prefix code, byte), valid only if stamped with the current generation.
     */
    private int[] lzwChildren;
    private int[] lzwStamps;
    private int lzwGeneration = 1;

    /**
     * TIFF LZW, as the libtiff encoder: MSB first codes, starting with a clear code, "early change"
     * of the code width and a clear code when the table is full.
     */
    private byte[] lzw( byte[] raw ) {
        if (lzwChildren == null) {
            lzwChildren = new int[4096 * 256];
            lzwStamps = new int[4096 * 256];
        }
        BitWriter out = new BitWriter(raw.length / 2 + 16);
        int nbits = 9;
        int maxcode = 511;
        int freeEnt = LZW_FIRST;
        lzwGeneration++;
        out.write(LZW_CLEAR, nbits);
        if (raw.length > 0) {
            int ent = raw[0] & 0xff;
            for( int i = 1; i < raw.length; i++ ) {
                int c = raw[i] & 0xff;
                int key = (ent << 8) | c;
                if (lzwStamps[key] == lzwGeneration) {
                    ent = lzwChildren[key];
                    continue;
                }
                out.write(ent, nbits);
                ent = c;
                lzwStamps[key] = lzwGeneration;
                lzwChildren[key] = freeEnt;
                freeEnt++;
                if (freeEnt == LZW_TABLE_FULL) {
                    out.write(LZW_CLEAR, nbits);
                    lzwGeneration++;
                    freeEnt = LZW_FIRST;
                    nbits = 9;
                    maxcode = 511;
                } else if (freeEnt > maxcode) {
                    nbits++;
                    maxcode = (1 << nbits) - 1;
                }
            }
            out.write(ent, nbits);
            freeEnt++;
            if (freeEnt == LZW_TABLE_FULL) {
                out.write(LZW_CLEAR, nbits);
                nbits = 9;
            } else if (freeEnt > maxcode) {
                nbits++;
            }
        }
        out.write(LZW_EOI, nbits);
        return out.toByteArray();
    }

    /**
     * Writer of codes, most significant bit first.
     */
    private static class BitWriter {
        private byte[] data;
        private int size = 0;
        private long buffer = 0;
        private int bits = 0;

        BitWriter( int capacity ) {
            data = new byte[Math.max(16, capacity)];
        }

        void write( int code, int nbits ) {
            buffer = (buffer << nbits) | code;
            bits += nbits;
            while( bits >= 8 ) {
                bits -= 8;
                put((byte) (buffer >>> bits));
            }
        }

        private void put( byte b ) {
            if (size == data.length) {
                data = java.util.Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = b;
        }

        byte[] toByteArray() {
            if (bits > 0) {
                put((byte) (buffer << (8 - bits)));
                bits = 0;
            }
            return java.util.Arrays.copyOf(data, size);
        }
    }
}
