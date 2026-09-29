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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 * A temporary file collecting the compressed tiles of all the levels, in any order,
 * so that the final COG can be assembled once all the tile sizes are known.
 */
class CogTileStore implements AutoCloseable {
    private final File file;
    private final FileChannel channel;
    private long size = 0;
    private final long[][] offsets;
    private final int[][] lengths;

    /**
     * @param file the temporary file.
     * @param tilesPerLevel the number of tiles of each level.
     */
    CogTileStore( File file, int[] tilesPerLevel ) throws IOException {
        this.file = file;
        this.channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
        offsets = new long[tilesPerLevel.length][];
        lengths = new int[tilesPerLevel.length][];
        for( int l = 0; l < tilesPerLevel.length; l++ ) {
            offsets[l] = new long[tilesPerLevel[l]];
            lengths[l] = new int[tilesPerLevel[l]];
        }
    }

    /**
     * Store a compressed tile. Can be called concurrently.
     */
    void put( int level, int tileIndex, byte[] data ) throws IOException {
        long position;
        synchronized (this) {
            position = size;
            size += data.length;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long at = position;
        while( buffer.hasRemaining() ) {
            at += channel.write(buffer, at);
        }
        offsets[level][tileIndex] = position;
        lengths[level][tileIndex] = data.length;
    }

    int getLength( int level, int tileIndex ) {
        return lengths[level][tileIndex];
    }

    /**
     * @return the total size of the stored tiles.
     */
    synchronized long getSize() {
        return size;
    }

    /**
     * Read back a stored tile.
     */
    byte[] get( int level, int tileIndex ) throws IOException {
        byte[] data = new byte[lengths[level][tileIndex]];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long at = offsets[level][tileIndex];
        while( buffer.hasRemaining() ) {
            int n = channel.read(buffer, at);
            if (n < 0) {
                throw new IOException("Unexpected end of the temporary tiles file.");
            }
            at += n;
        }
        return data;
    }

    @Override
    public void close() throws IOException {
        channel.close();
        file.delete();
    }
}
