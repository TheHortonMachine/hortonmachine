package org.jgrasstools.gears.io.disktree;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.util.List;

import org.jgrasstools.gears.io.disktree.jtstmp.Quadtree;

import com.vividsolutions.jts.JTSVersion;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;

public class DiskTreeReader implements IDiskTree {

    private final String path;

    private Quadtree indexObj;

    private RandomAccessFile raf = null;

    public DiskTreeReader( String path ) {
        this.path = path;
    }

    public Quadtree readIndex() throws Exception {

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
        indexObj = (Quadtree) in.readObject();
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

    public Geometry pickGeometry( long position, long size ) throws Exception {
        byte[] geomBytes = new byte[(int) size];
        raf.seek(position);
        raf.read(geomBytes);
        WKBReader r = new WKBReader();
        Geometry geometry = r.read(geomBytes);
        return geometry;
    }

    public void close() throws IOException {
        raf.close();
    }

    public static void main( String[] args ) throws Exception {


        DiskTreeReader writer = new DiskTreeReader("/home/moovida/TMP/index.bin");
        Quadtree indexObj = writer.readIndex();

        List queryAll = indexObj.queryAll();

        for( Object object : queryAll ) {
            if (object instanceof long[]) {
                long[] posSize = (long[]) object;
                Geometry geom = writer.pickGeometry(posSize[0], posSize[1]);
                System.out.println(geom.toText());
            }
        }

    }
}
