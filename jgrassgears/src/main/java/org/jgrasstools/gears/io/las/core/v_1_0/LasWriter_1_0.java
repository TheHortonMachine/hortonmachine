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
package org.jgrasstools.gears.io.las.core.v_1_0;

import static java.lang.Math.round;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.BitSet;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.utils.ByteUtilities;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.JGTVersion;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A las writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasWriter_1_0 {
    private final byte[] doubleDataArray = new byte[8];
    private final ByteBuffer doubleBb = ByteBuffer.wrap(doubleDataArray);
    private final byte[] longDataArray = new byte[4];
    private final ByteBuffer longBb = ByteBuffer.wrap(longDataArray);
    private final byte[] shortDataArray = new byte[2];
    private final ByteBuffer shortBb = ByteBuffer.wrap(shortDataArray);
    private File outFile;
    private CoordinateReferenceSystem crs;
    private FileOutputStream fos;
    private File prjFile;
    private double xScale = 0.01;
    private double yScale = 0.01;
    private double zScale = 0.001;
    private double xMin = 0;
    private double yMin = 0;
    private double zMin = 0;
    private double xMax = 0;
    private double yMax = 0;
    private double zMax = 0;
    private int recordsNum = 0;

    private final short recordLength = 28;
    private FileChannel fileChannel;
    private int recordsNumPosition;

    /**
     * A las file writer.
     * 
     * @param outFile the output file.
     * @param crs the {@link CoordinateReferenceSystem crs}. If <code>null</code>, no prj file is written.
     */
    public LasWriter_1_0( File outFile, CoordinateReferenceSystem crs ) {
        this.outFile = outFile;
        this.crs = crs;

        if (crs != null) {
            String nameWithoutExtention = FileUtilities.getNameWithoutExtention(outFile);
            prjFile = new File(outFile.getParent(), nameWithoutExtention + ".prj");
        }
        doubleBb.order(ByteOrder.LITTLE_ENDIAN);
        longBb.order(ByteOrder.LITTLE_ENDIAN);
        shortBb.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Possibility to define the scale for the data.
     * 
     * <p>If not set it defaults to 0.01,0.01,0.001.</p>
     * 
     * @param xScale the x scaling value.
     * @param yScale the y scaling value.
     * @param zScale the z scaling value.
     */
    public void setScales( double xScale, double yScale, double zScale ) {
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    /**
     * Possibility to set the min and max bounds.
     * 
     * <p>If not set they all default to 0.</p>
     * 
     * @param xMin
     * @param xMax
     * @param yMin
     * @param yMax
     * @param zMin
     * @param zMax
     */
    public void setBounds( double xMin, double xMax, double yMin, double yMax, double zMin, double zMax ) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.zMin = zMin;
        this.xMax = xMax;
        this.yMax = yMax;
        this.zMax = zMax;
    }
    
    /**
     * Possibility to set the min and max bounds.
     * 
     * @param header the las header (as read by the reader).
     */
    public void setBounds( ILasHeader header) {
        ReferencedEnvelope3D env = header.getDataEnvelope();
        this.xMin = env.getMinX();
        this.yMin = env.getMinY();
        this.zMin = env.getMinZ();
        this.xMax = env.getMaxX();
        this.yMax = env.getMaxY();
        this.zMax = env.getMaxZ();
    }

    /**
     * Opens the file and write the header info.
     * 
     * @throws Exception
     */
    public void open() throws Exception {
        openFile();
        writeHeader();
    }

    private void writeHeader() throws IOException {

        // File signature: LASF
        // File source ID: 0
        // Project ID - data 1: 0
        // Project ID - data 2: 0
        // Project ID - data 3: 0
        // Project ID - data 4:
        // Version: 1.0
        // System identifier:
        // File creation Day of Year: 0
        // File creation Year: 0
        // Header size: 227
        // Variable length records: 1
        // Point data format ID (0-99 for spec): 1
        // Number of point records: 20308602
        int hLength = 0;
        
        // TODO handle global enchoding and proper gps time

        byte[] signature = "LASF".getBytes();
        fos.write(signature);
        hLength = hLength + 4;
        byte[] reserved = new byte[4];
        fos.write(reserved);
        hLength = hLength + 4;
        byte[] guid1 = new byte[4];
        fos.write(guid1);
        hLength = hLength + 4;
        byte[] guid2 = new byte[2];
        fos.write(guid2);
        hLength = hLength + 2;
        byte[] guid3 = new byte[2];
        fos.write(guid3);
        hLength = hLength + 2;
        byte[] guid4 = new byte[8];
        fos.write(guid4);
        hLength = hLength + 8;
        // major
        fos.write(1);
        // minor
        fos.write(0);
        hLength = hLength + 2;

        byte[] systemIdentifier = new byte[32];
        fos.write(systemIdentifier);
        hLength = hLength + 32;

        String jgtVersion = "jgrasstools_" + JGTVersion.CURRENT_VERSION.toString();
        if (jgtVersion.length() > 32) {
            jgtVersion = jgtVersion.substring(0, 31);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(jgtVersion);
            for( int i = jgtVersion.length(); i < 32; i++ ) {
                sb.append(" ");
            }
            jgtVersion = sb.toString();
        }
        byte[] software = jgtVersion.getBytes();
        fos.write(software);
        hLength = hLength + 32;

        byte[] flightDateJulian = new byte[2];
        fos.write(flightDateJulian);
        hLength = hLength + 2;

        byte[] year = new byte[2];
        fos.write(year);
        hLength = hLength + 2;

        short headersize = 226;
        fos.write(getShort(headersize));
        hLength = hLength + 2;

        int offsetToData = 227;
        fos.write(getLong(offsetToData));
        hLength = hLength + 4;

        int numVarRecords = 0;
        fos.write(getLong(numVarRecords));
        hLength = hLength + 4;

        // point data format
        fos.write(1);
        hLength = hLength + 1;

        fos.write(getShort(recordLength));
        hLength = hLength + 2;

        recordsNumPosition = hLength;
        fos.write(getLong(recordsNum));
        hLength = hLength + 4;

        // num of points by return
        fos.write(new byte[20]);
        hLength = hLength + 20;

        // xscale
        fos.write(getDouble(xScale));
        // yscale
        fos.write(getDouble(yScale));
        // zscale
        fos.write(getDouble(zScale));
        hLength = hLength + 3 * 8;

        // xoff, yoff, zoff
        fos.write(getDouble(xMin));
        fos.write(getDouble(yMin));
        fos.write(getDouble(zMin));
        hLength = hLength + 3 * 8;

        fos.write(getDouble(xMax));
        fos.write(getDouble(xMin));
        hLength = hLength + 2 * 8;

        fos.write(getDouble(yMax));
        fos.write(getDouble(yMin));
        hLength = hLength + 2 * 8;

        fos.write(getDouble(zMax));
        fos.write(getDouble(zMin));
        hLength = hLength + 3 * 8;

    }

    /**
     * Writes a point to file.
     * 
     * @param record the point record.
     * @throws IOException
     */
    public void addPoint( LasRecord record ) throws IOException {
        int length = 0;
        int x = (int) round((record.x - xMin) / xScale);
        int y = (int) round((record.y - yMin) / yScale);
        int z = (int) round((record.z - zMin) / zScale);
        fos.write(getLong(x));
        fos.write(getLong(y));
        fos.write(getLong(z));
        length = length + 12;
        fos.write(getShort(record.intensity));
        length = length + 2;

        // 001 | 001 | 11 -> bits for return num, num of ret, scan dir flag, edge of flight line

        int returnNumber = record.returnNumber;
        BitSet bitsetRN = ByteUtilities.bitsetFromByte((byte) returnNumber);
        int numberOfReturns = record.numberOfReturns;
        BitSet bitsetNOR = ByteUtilities.bitsetFromByte((byte) numberOfReturns);
        BitSet b = new BitSet(7);
        b.set(0, bitsetRN.get(0));
        b.set(1, bitsetRN.get(1));
        b.set(2, bitsetRN.get(2));
        b.set(3, bitsetNOR.get(0));
        b.set(4, bitsetNOR.get(1));
        b.set(5, bitsetNOR.get(2));
        b.set(6, false);
        b.set(7, false);
        byte[] bb = ByteUtilities.bitSetToByteArray(b);
        fos.write(bb[0]);
        length = length + 1;

        // class
        byte c = (byte) record.classification;
        fos.write(c);
        length = length + 1;

        // scan angle rank
        fos.write(1);
        length = length + 1;
        fos.write(0);
        length = length + 1;
        fos.write(new byte[2]);
        length = length + 2;

        fos.write(getDouble(record.gpsTime));
        length = length + 8;

        if (recordLength != length) {
            throw new RuntimeException();
        }

        recordsNum++;
    }

    public void close() throws Exception {
        fileChannel.position(recordsNumPosition);
        fos.write(getLong(recordsNum));

        closeFile();

        /*
         * write crs file
         */
        if (crs != null)
            CrsUtilities.writeProjectionFile(prjFile.getAbsolutePath(), null, crs);
    }

    private byte[] getLong( int num ) {
        longBb.clear();
        longBb.putInt(num);
        byte[] array = longBb.array();
        return array;
    }

    // private byte[] getLong( double num ) {
    // longBb.clear();
    // longBb.putDouble(num);
    // byte[] array = longBb.array();
    // return array;
    // }

    private byte[] getDouble( double num ) {
        doubleBb.clear();
        doubleBb.putDouble(num);
        byte[] array = doubleBb.array();
        return array;
    }

    private byte[] getShort( short num ) {
        shortBb.clear();
        shortBb.putShort(num);
        return shortBb.array();
    }

    private void openFile() throws Exception {
        fos = new FileOutputStream(outFile);
        fileChannel = fos.getChannel();
    }

    private void closeFile() throws Exception {
        if (fileChannel != null && fileChannel.isOpen())
            fileChannel.close();
        if (fos != null)
            fos.close();
    }

}
