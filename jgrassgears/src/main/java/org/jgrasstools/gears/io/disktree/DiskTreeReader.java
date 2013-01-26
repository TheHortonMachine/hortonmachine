/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.disktree;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;

import com.vividsolutions.jts.JTSVersion;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Reader for the Sort-Tile-Recursive disk index.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @see IDiskTree
 */
public class DiskTreeReader implements IDiskTree {

    private final String path;

    private STRtree indexObj;

    private RandomAccessFile raf = null;

    /**
     * Constructor.
     * 
     * @param path the path from which to read.
     */
    public DiskTreeReader( String path ) {
        this.path = path;
    }

    /**
     * Reads the {@link STRtree} object from the file.
     * 
     * @return the quadtree, holding envelops and geometry positions in the file.
     * @throws Exception
     */
    public STRtree readIndex() throws Exception {

        File file = new File(path);
        raf = new RandomAccessFile(file, "r");

        raf.seek(6l);
        checkVersions();

        long position = INDEX_ADDRESS_POSITION;
        raf.seek(position);
        long indexAddress = raf.readLong();
        position = INDEX_ADDRESS_POSITION + INDEX_ADDRESS_SIZE;
        raf.seek(position);
        long indexSize = raf.readLong();

        raf.seek(indexAddress);
        byte[] indexBytes = new byte[(int) indexSize];
        int read = raf.read(indexBytes);
        if (read != indexSize) {
            throw new IOException();
        }

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(indexBytes));
        indexObj = (STRtree) in.readObject();
        return indexObj;
    }

    private void checkVersions() throws IOException {
        int fileMajor = raf.readInt();
        int fileMinor = raf.readInt();

        int currMajor = JTSVersion.MAJOR;
        int currMinor = JTSVersion.MINOR;

        if (fileMajor != currMajor || fileMinor != currMinor) {
            System.out
                    .println("Warning, the current used JTS version differs from the one used to create the file. Unexpected results may occurr.");
        }
    }

    /**
     * Reads a single geomtry, using the info from the quadtree read in {@link #readIndex()}.
     * 
     * @param position the position of the geom to read.
     * @param size the size of the geom to read.
     * @return the read geometry.
     * @throws Exception
     */
    public Geometry pickGeometry( long position, long size ) throws Exception {
        byte[] geomBytes = new byte[(int) size];
        raf.seek(position);
        raf.read(geomBytes);

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(geomBytes));
        Geometry geometry = (Geometry) in.readObject();
        return geometry;
    }

    /**
     * Closes the filehandle.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        raf.close();
    }

    // public static void main( String[] args ) throws Exception {
    //
    //
    // DiskTreeReader writer = new DiskTreeReader("/home/moovida/TMP/index.bin");
    // Quadtree indexObj = writer.readIndex();
    //
    // List queryAll = indexObj.queryAll();
    //
    // for( Object object : queryAll ) {
    // if (object instanceof long[]) {
    // long[] posSize = (long[]) object;
    // Geometry geom = writer.pickGeometry(posSize[0], posSize[1]);
    // System.out.println(geom.toText());
    // }
    // }
    //
    // }
}
