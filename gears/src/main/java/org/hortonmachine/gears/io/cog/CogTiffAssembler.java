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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Writes the final Cloud Optimized GeoTIFF from the compressed tiles of all the levels.
 *
 * <p>The layout is the one of the GDAL COG driver:</p>
 * <pre>
 * header | GDAL structural metadata ("ghost area") | IFD 0 (full resolution) | IFD 1 ... IFD n (overviews)
 *        | tiles of level n (smallest) | ... | tiles of level 0 (full resolution)
 * </pre>
 * <p>Each tile is preceded by its size as 4 bytes and followed by its last 4 bytes repeated.
 * All the positions are computed before writing, since the tile sizes are known.</p>
 */
class CogTiffAssembler {
    // TIFF field types
    static final int TYPE_ASCII = 2;
    static final int TYPE_SHORT = 3;
    static final int TYPE_LONG = 4;
    static final int TYPE_DOUBLE = 12;
    static final int TYPE_LONG8 = 16;

    static final int TAG_TILE_OFFSETS = 324;
    static final int TAG_TILE_BYTE_COUNTS = 325;

    private static final String GHOST_OPTIONS = "LAYOUT=IFDS_BEFORE_DATA\n" + //
            "BLOCK_ORDER=ROW_MAJOR\n" + //
            "BLOCK_LEADER=SIZE_AS_UINT4\n" + //
            "BLOCK_TRAILER=LAST_4_BYTES_REPEATED\n" + //
            "KNOWN_INCOMPATIBLE_EDITION=NO\n ";

    /**
     * A TIFF field, with its values already encoded in little endian order.
     */
    static class Field {
        final int tag;
        final int type;
        final long count;
        final byte[] data;

        Field( int tag, int type, long count, byte[] data ) {
            this.tag = tag;
            this.type = type;
            this.count = count;
            this.data = data;
        }

        static Field shorts( int tag, int... values ) {
            ByteBuffer b = ByteBuffer.allocate(values.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            for( int v : values ) {
                b.putShort((short) v);
            }
            return new Field(tag, TYPE_SHORT, values.length, b.array());
        }

        static Field longs( int tag, long... values ) {
            ByteBuffer b = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            for( long v : values ) {
                b.putInt((int) v);
            }
            return new Field(tag, TYPE_LONG, values.length, b.array());
        }

        static Field long8s( int tag, long... values ) {
            ByteBuffer b = ByteBuffer.allocate(values.length * 8).order(ByteOrder.LITTLE_ENDIAN);
            for( long v : values ) {
                b.putLong(v);
            }
            return new Field(tag, TYPE_LONG8, values.length, b.array());
        }

        static Field doubles( int tag, double... values ) {
            ByteBuffer b = ByteBuffer.allocate(values.length * 8).order(ByteOrder.LITTLE_ENDIAN);
            for( double v : values ) {
                b.putDouble(v);
            }
            return new Field(tag, TYPE_DOUBLE, values.length, b.array());
        }

        static Field ascii( int tag, String value ) {
            byte[] chars = value.getBytes(StandardCharsets.US_ASCII);
            byte[] data = new byte[chars.length + 1];
            System.arraycopy(chars, 0, data, 0, chars.length);
            return new Field(tag, TYPE_ASCII, data.length, data);
        }
    }

    /**
     * The description of a level (image) of the COG.
     */
    static class Level {
        final int tilesCount;
        /**
         * The fields of the image, without tile offsets and byte counts.
         */
        final List<Field> fields;

        Level( int tilesCount, List<Field> fields ) {
            this.tilesCount = tilesCount;
            this.fields = fields;
        }
    }

    private final List<Level> levels;
    private final CogTileStore store;

    CogTiffAssembler( List<Level> levels, CogTileStore store ) {
        this.levels = levels;
        this.store = store;
    }

    /**
     * @return the size the file would have, to decide if BigTIFF is needed.
     */
    long computeSize( boolean bigTiff ) {
        return plan(bigTiff).fileSize;
    }

    /**
     * Write the COG.
     *
     * @param outFile the file to write.
     * @param bigTiff if <code>true</code>, a BigTIFF is written.
     */
    void write( File outFile, boolean bigTiff ) throws IOException {
        Plan plan = plan(bigTiff);
        if (!bigTiff && plan.fileSize > 0xFFFFFFFFL) {
            throw new IOException("The file is too large for a classic TIFF, BigTIFF is needed.");
        }
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile), 1 << 20)) {
            CountingWriter writer = new CountingWriter(out);
            // header
            ByteBuffer header = ByteBuffer.allocate(bigTiff ? 16 : 8).order(ByteOrder.LITTLE_ENDIAN);
            header.put((byte) 'I').put((byte) 'I');
            if (bigTiff) {
                header.putShort((short) 43).putShort((short) 8).putShort((short) 0).putLong(plan.ifdOffsets[0]);
            } else {
                header.putShort((short) 42).putInt((int) plan.ifdOffsets[0]);
            }
            writer.write(header.array());
            // ghost area
            writer.write(plan.ghost);
            // image file directories, each followed by its values that do not fit the entries
            for( int l = 0; l < levels.size(); l++ ) {
                writer.padTo(plan.ifdOffsets[l]);
                long next = l + 1 < levels.size() ? plan.ifdOffsets[l + 1] : 0;
                writeIfd(writer, plan.fields.get(l), plan.ifdOffsets[l], next, bigTiff);
            }
            // tiles, from the smallest level to the full resolution one
            writer.padTo(plan.dataStart);
            for( int l = levels.size() - 1; l >= 0; l-- ) {
                for( int t = 0; t < levels.get(l).tilesCount; t++ ) {
                    byte[] data = store.get(l, t);
                    writer.writeInt(data.length);
                    writer.write(data);
                    byte[] trailer = new byte[4];
                    System.arraycopy(data, Math.max(0, data.length - 4), trailer, Math.max(0, 4 - data.length),
                            Math.min(4, data.length));
                    writer.write(trailer);
                }
            }
            if (writer.position != plan.fileSize) {
                throw new IOException("Wrong COG layout: " + writer.position + " bytes written, " + plan.fileSize + " planned.");
            }
        }
    }

    private static class Plan {
        byte[] ghost;
        long[] ifdOffsets;
        List<List<Field>> fields = new ArrayList<>();
        long dataStart;
        long fileSize;
    }

    private Plan plan( boolean bigTiff ) {
        Plan plan = new Plan();
        String ghost = String.format("GDAL_STRUCTURAL_METADATA_SIZE=%06d bytes\n", GHOST_OPTIONS.length()) + GHOST_OPTIONS;
        plan.ghost = ghost.getBytes(StandardCharsets.US_ASCII);
        int headerSize = bigTiff ? 16 : 8;
        int inlineSize = bigTiff ? 8 : 4;

        // tile positions: data start is known once the directories are sized, and they don't depend on the positions
        plan.ifdOffsets = new long[levels.size()];
        long position = align(headerSize + plan.ghost.length);
        for( int l = 0; l < levels.size(); l++ ) {
            plan.ifdOffsets[l] = position;
            List<Field> fields = withTileFields(levels.get(l), new long[levels.get(l).tilesCount], bigTiff);
            position = align(position + ifdSize(fields, bigTiff, inlineSize));
        }
        plan.dataStart = position;

        long[][] tileOffsets = new long[levels.size()][];
        for( int l = levels.size() - 1; l >= 0; l-- ) {
            tileOffsets[l] = new long[levels.get(l).tilesCount];
            for( int t = 0; t < levels.get(l).tilesCount; t++ ) {
                position += 4;
                tileOffsets[l][t] = position;
                position += store.getLength(l, t) + 4;
            }
        }
        plan.fileSize = position;
        for( int l = 0; l < levels.size(); l++ ) {
            plan.fields.add(withTileFields(levels.get(l), tileOffsets[l], bigTiff));
        }
        return plan;
    }

    private List<Field> withTileFields( Level level, long[] tileOffsets, boolean bigTiff ) {
        List<Field> fields = new ArrayList<>(level.fields);
        fields.add(bigTiff ? Field.long8s(TAG_TILE_OFFSETS, tileOffsets) : Field.longs(TAG_TILE_OFFSETS, tileOffsets));
        long[] byteCounts = new long[level.tilesCount];
        int levelIndex = levels.indexOf(level);
        for( int t = 0; t < byteCounts.length; t++ ) {
            byteCounts[t] = store.getLength(levelIndex, t);
        }
        fields.add(Field.longs(TAG_TILE_BYTE_COUNTS, byteCounts));
        fields.sort(Comparator.comparingInt(f -> f.tag));
        return fields;
    }

    private static long ifdSize( List<Field> fields, boolean bigTiff, int inlineSize ) {
        long size = bigTiff ? 8 + 20L * fields.size() + 8 : 2 + 12L * fields.size() + 4;
        for( Field field : fields ) {
            if (field.data.length > inlineSize) {
                size = align(size) + field.data.length;
            }
        }
        return size;
    }

    private static long align( long position ) {
        return (position + 7) & ~7L;
    }

    private void writeIfd( CountingWriter writer, List<Field> fields, long ifdOffset, long nextIfd, boolean bigTiff )
            throws IOException {
        int inlineSize = bigTiff ? 8 : 4;
        int entrySize = bigTiff ? 20 : 12;
        long entriesSize = bigTiff ? 8 + (long) entrySize * fields.size() + 8 : 2 + (long) entrySize * fields.size() + 4;
        ByteBuffer ifd = ByteBuffer.allocate((int) entriesSize).order(ByteOrder.LITTLE_ENDIAN);
        if (bigTiff) {
            ifd.putLong(fields.size());
        } else {
            ifd.putShort((short) fields.size());
        }
        // the values that do not fit the entries follow the directory
        long valuesPosition = ifdOffset + entriesSize;
        List<Field> outOfLine = new ArrayList<>();
        List<Long> outOfLineOffsets = new ArrayList<>();
        for( Field field : fields ) {
            ifd.putShort((short) field.tag);
            ifd.putShort((short) field.type);
            if (bigTiff) {
                ifd.putLong(field.count);
            } else {
                ifd.putInt((int) field.count);
            }
            byte[] value = new byte[inlineSize];
            if (field.data.length <= inlineSize) {
                System.arraycopy(field.data, 0, value, 0, field.data.length);
            } else {
                valuesPosition = align(valuesPosition);
                ByteBuffer offset = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
                if (bigTiff) {
                    offset.putLong(valuesPosition);
                } else {
                    offset.putInt((int) valuesPosition);
                }
                outOfLine.add(field);
                outOfLineOffsets.add(valuesPosition);
                valuesPosition += field.data.length;
            }
            ifd.put(value);
        }
        if (bigTiff) {
            ifd.putLong(nextIfd);
        } else {
            ifd.putInt((int) nextIfd);
        }
        writer.write(ifd.array());
        for( int i = 0; i < outOfLine.size(); i++ ) {
            writer.padTo(outOfLineOffsets.get(i));
            writer.write(outOfLine.get(i).data);
        }
    }

    /**
     * An output stream wrapper that tracks the position.
     */
    private static class CountingWriter {
        private final OutputStream out;
        private long position = 0;

        CountingWriter( OutputStream out ) {
            this.out = out;
        }

        void write( byte[] data ) throws IOException {
            out.write(data);
            position += data.length;
        }

        void writeInt( int value ) throws IOException {
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
        }

        void padTo( long target ) throws IOException {
            if (target < position) {
                throw new IOException("Wrong COG layout: at " + position + ", expected " + target);
            }
            while( position < target ) {
                out.write(0);
                position++;
            }
        }
    }
}
