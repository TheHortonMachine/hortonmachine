/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gears.io.disktree;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;

import org.locationtech.jts.JTSVersion;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * Writer for the quadtree disk index.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @see IDiskTree
 */
public class DiskTreeWriter implements IDiskTree {

    private final String path;

    /**
     * Constructor.
     * 
     * @param path the path to which the index will be written.
     */
    public DiskTreeWriter( String path ) {
        this.path = path;
    }

    /**
     * Writes an array of {@link Geometry}s to the disk.
     * 
     * @param geometries the array of geoms to write.
     * @throws IOException
     */
    public void writeGeometries( Geometry[] geometries ) throws IOException {
        File file = new File(path);
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");

            int major = JTSVersion.MAJOR;
            int minor = JTSVersion.MINOR;

            raf.writeChars("jts");
            raf.writeInt(major);
            raf.writeInt(minor);

            long geometriesStart = INDEX_ADDRESS_POSITION + INDEX_ADDRESS_SIZE + INDEX_LENGTH_SIZE;
            raf.seek(geometriesStart);
            System.out.println("geometriesStart: " + geometriesStart);

            long fileIndex = geometriesStart;
            STRtree tree = new STRtree(geometries.length);
            for( int i = 0; i < geometries.length; i++ ) {
                Geometry geometry = geometries[i];

                if (geometry.isEmpty()) {
                    continue;
                }
                Envelope envelope = geometry.getEnvelopeInternal();

                byte[] geomBytes = serialize(geometry);
                raf.write(geomBytes);

                /*
                 * the tree contains the envelope of a geometry
                 * and the array with the exact position in the file where the 
                 * geometry bytes start + the length. 
                 */
                tree.insert(envelope, new long[]{fileIndex, geomBytes.length});

                fileIndex = fileIndex + geomBytes.length;
                System.out.println("geom: " + i + " finished at: " + fileIndex);
            }

            raf.seek(INDEX_ADDRESS_POSITION);
            raf.writeLong(fileIndex);

            System.out.println("INDEX_ADDRESS_POSITION: " + fileIndex);

            /*
             * serialize index
             */
            byte[] treeBytes = serialize(tree);

            long treeSize = treeBytes.length;
            raf.seek(fileIndex);
            raf.write(treeBytes, 0, (int) treeSize);

            System.out.println("treeSize: " + treeSize);

            raf.seek(INDEX_ADDRESS_POSITION + INDEX_ADDRESS_SIZE);
            raf.writeLong(treeSize);

            long length = raf.length();
            System.out.println(length);

        } finally {
            System.out.println("close");
            if (raf != null)
                raf.close();
        }
    }

    private byte[] serialize( Object obj ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        out.close();
        return bos.toByteArray();
    }
}
