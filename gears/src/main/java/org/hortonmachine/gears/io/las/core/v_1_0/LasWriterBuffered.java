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
package org.hortonmachine.gears.io.las.core.v_1_0;

import static java.lang.Math.round;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.BitSet;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.utils.ByteUtilities;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.HMVersion;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A las writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasWriterBuffered extends ALasWriter {
    private static final String OPEN_METHOD_MSG = "This needs to be called before the open method.";

    private final int bufferCapacity = 10 * 1024 * 1024; // 10 Mb allocations
    private final ByteBuffer mainBuffer = ByteBuffer.allocate(bufferCapacity);
    private int runningBytesCount = 0;

    private File outFile;
    private CoordinateReferenceSystem crs;
    private FileOutputStream fos;
    private File prjFile;
    private double xScale = 0.01;
    private double yScale = 0.01;
    private double zScale = 0.001;
    private double xOffset = 0.0;
    private double yOffset = 0.0;
    private double zOffset = 0.0;
    private double xMin = 0;
    private double yMin = 0;
    private double zMin = 0;
    private double xMax = 0;
    private double yMax = 0;
    private double zMax = 0;
    private int recordsNum = 0;

    private short recordLength = 28;
    private FileChannel fileChannel;
    private int recordsNumPosition;
    private int pointFormatPosition;
    private boolean pointFormatHasBeenSet = true;
    private int pointFormat = 3;
    private boolean doWriteGroundElevation;
    private boolean openCalled;

    private int previousReturnNumber = -999;
    private int previousNumberOfReturns = -999;
    private byte[] previousReturnBytes = null;
    private long offsetToData = 227;
    private int recordLengthPosition;
    private int gpsTimeType = 0;

    /**
     * A las file writer.
     * 
     * @param outFile the output file.
     * @param crs the {@link CoordinateReferenceSystem crs}. If <code>null</code>, no prj file is written.
     */
    public LasWriterBuffered( File outFile, CoordinateReferenceSystem crs ) {
        this.outFile = outFile;
        this.crs = crs;

        if (crs != null) {
            String nameWithoutExtention = FileUtilities.getNameWithoutExtention(outFile);
            prjFile = new File(outFile.getParent(), nameWithoutExtention + ".prj");
        }
        mainBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void setScales( double xScale, double yScale, double zScale ) {
        if (openCalled) {
            throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, this);
        }
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    @Override
    public void setOffset( double xOffset, double yOffset, double zOffset ) {
        if (openCalled) {
            throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, this);
        }
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    @Override
    public void setPointFormat( int pointFormat ) {
        this.pointFormat = pointFormat;
        pointFormatHasBeenSet = true;
    }

    @Override
    public void setBounds( double xMin, double xMax, double yMin, double yMax, double zMin, double zMax ) {
        if (openCalled) {
            throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, this);
        }
        this.xMin = xMin;
        this.yMin = yMin;
        this.zMin = zMin;
        this.xMax = xMax;
        this.yMax = yMax;
        this.zMax = zMax;
    }

    @Override
    public void setBounds( ILasHeader header ) {
        if (openCalled) {
            throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, crs);
        }
        ReferencedEnvelope3D env = header.getDataEnvelope();
        this.xMin = env.getMinX();
        this.yMin = env.getMinY();
        this.zMin = env.getMinZ();
        this.xMax = env.getMaxX();
        this.yMax = env.getMaxY();
        this.zMax = env.getMaxZ();

        double[] xyzOffset = header.getXYZOffset();
        double[] xyzScale = header.getXYZScale();
        xOffset = xyzOffset[0];
        yOffset = xyzOffset[1];
        zOffset = xyzOffset[2];
        xScale = xyzScale[0];
        yScale = xyzScale[1];
        zScale = xyzScale[2];

        offsetToData = header.getOffset();
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.gears.io.las.core.v_1_0.ALasWriter#open()
     */
    @Override
    public void open() throws Exception {
        openFile();
        writeHeader();
        openCalled = true;
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

        byte[] signature = "LASF".getBytes("ISO-8859-1");// ensure a sensible encoding is using
        mainBuffer.put(signature);
        hLength = hLength + 4;
        byte[] fileSourceId = new byte[2];
        mainBuffer.put(fileSourceId);
        hLength = hLength + 2;
        short globalEncoding;
        if (gpsTimeType == 1) {
            globalEncoding = 1;
        } else {
            globalEncoding = 0;
        }
        mainBuffer.putShort(globalEncoding);
        hLength = hLength + 2;
        byte[] guid1 = new byte[4];
        mainBuffer.put(guid1);
        hLength = hLength + 4;
        byte[] guid2 = new byte[2];
        mainBuffer.put(guid2);
        hLength = hLength + 2;
        byte[] guid3 = new byte[2];
        mainBuffer.put(guid3);
        hLength = hLength + 2;
        byte[] guid4 = new byte[8];
        mainBuffer.put(guid4);
        hLength = hLength + 8;
        // major
        mainBuffer.put((byte) 1);
        // minor
        mainBuffer.put((byte) 2);
        hLength = hLength + 2;

        byte[] systemIdentifier = new byte[32];
        mainBuffer.put(systemIdentifier);
        hLength = hLength + 32;

        String jgtVersion = "hortonmachine_" + HMVersion.CURRENT_VERSION.toString();
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
        mainBuffer.put(software);
        hLength = hLength + 32;

        byte[] flightDateJulian = new byte[2];
        mainBuffer.put(flightDateJulian);
        hLength = hLength + 2;

        byte[] year = new byte[2];
        mainBuffer.put(year);
        hLength = hLength + 2;

        short headersize = 227;
        mainBuffer.putShort(headersize);
        hLength = hLength + 2;

        mainBuffer.putInt((int) offsetToData);
        hLength = hLength + 4;

        int numVarRecords = 0;
        mainBuffer.putInt(numVarRecords);
        hLength = hLength + 4;

        // point data format
        pointFormatPosition = hLength;
        mainBuffer.put((byte) pointFormat);
        hLength = hLength + 1;

        recordLengthPosition = hLength;
        mainBuffer.putShort(recordLength);
        hLength = hLength + 2;

        recordsNumPosition = hLength;
        mainBuffer.putInt(recordsNum);
        hLength = hLength + 4;

        // num of points by return
        mainBuffer.put(new byte[20]);
        hLength = hLength + 20;

        // xscale
        mainBuffer.putDouble(xScale);
        // yscale
        mainBuffer.putDouble(yScale);
        // zscale
        mainBuffer.putDouble(zScale);
        hLength = hLength + 3 * 8;

        // xoff, yoff, zoff
        mainBuffer.putDouble(xOffset);
        mainBuffer.putDouble(yOffset);
        mainBuffer.putDouble(zOffset);
        hLength = hLength + 3 * 8;

        // x,y,z - min/max
        mainBuffer.putDouble(xMax);
        mainBuffer.putDouble(xMin);
        mainBuffer.putDouble(yMax);
        mainBuffer.putDouble(yMin);
        mainBuffer.putDouble(zMax);
        mainBuffer.putDouble(zMin);
        hLength = hLength + 6 * 8;

        // write header to output stream
        writeMainBuffer(hLength);
        fileChannel.position(offsetToData);

    }

    private void writeMainBuffer( int length ) throws IOException {
        byte[] array = mainBuffer.array();
        fos.write(array, 0, length);
        mainBuffer.clear();
        mainBuffer.position(0);
    }

    @Override
    public synchronized void addPoint( LasRecord record ) throws IOException {
        int length = 0;
        int x = (int) round((record.x - xOffset) / xScale);
        int y = (int) round((record.y - yOffset) / yScale);
        int z;
        if (!doWriteGroundElevation) {
            z = (int) round((record.z - zOffset) / zScale);
        } else {
            z = (int) round((record.groundElevation - zOffset) / zScale);
        }
        mainBuffer.putInt(x);
        mainBuffer.putInt(y);
        mainBuffer.putInt(z);
        length = length + 12;
        mainBuffer.putShort(record.intensity);
        length = length + 2;

        // 001 | 001 | 11 -> bits for return num, num of ret, scan dir flag, edge of flight line

        int returnNumber = record.returnNumber;
        int numberOfReturns = record.numberOfReturns;

        if (returnNumber != previousReturnNumber || numberOfReturns != previousNumberOfReturns) {
            BitSet bitsetRN = ByteUtilities.bitsetFromByte((byte) returnNumber);
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
            mainBuffer.put(bb[0]);
            previousReturnBytes = bb;
            previousReturnNumber = returnNumber;
            previousNumberOfReturns = numberOfReturns;
        } else {
            mainBuffer.put(previousReturnBytes[0]);
        }
        length = length + 1;

        // class
        byte c = (byte) record.classification;
        mainBuffer.put(c);
        length = length + 1;

        // scan angle rank
        mainBuffer.put((byte) 1);
        length = length + 1;
        // user data
        mainBuffer.put((byte) 0);
        length = length + 1;
        // point source id
        mainBuffer.put(new byte[2]);
        length = length + 2;

        if (pointFormatHasBeenSet) {
            switch( pointFormat ) {
            case 1:
                length = length + writeGpstime(record);
                break;
            case 2:
                length = length + writeRGB(record);
                break;
            case 3:
                length = length + writeGpstime(record);
                length = length + writeRGB(record);
                break;
            }
        } else {
            if (record.gpsTime != -1) {
                pointFormat = 1;
                length = length + writeGpstime(record);
            }
        }

        recordLength = (short) length;

        runningBytesCount += length;

        if (bufferCapacity - runningBytesCount < recordLength) {
            // write to disk and create new space
            writeMainBuffer(runningBytesCount);
            runningBytesCount = 0;
        }

        recordsNum++;
    }

    private int writeGpstime( LasRecord record ) throws IOException {
        mainBuffer.putDouble(record.gpsTime);
        return 8;
    }

    private int writeRGB( LasRecord record ) throws IOException {
        mainBuffer.putShort(record.color[0]);
        mainBuffer.putShort(record.color[1]);
        mainBuffer.putShort(record.color[2]);
        return 6;
    }

    @Override
    public void close() throws Exception {
        // write leftover points
        if (runningBytesCount > 0) {
            writeMainBuffer(runningBytesCount);
            runningBytesCount = 0;
        }

        byte[] longDataArray = new byte[4];
        ByteBuffer longBb = ByteBuffer.wrap(longDataArray);
        longBb.order(ByteOrder.LITTLE_ENDIAN);
        byte[] shortDataArray = new byte[2];
        ByteBuffer shortBb = ByteBuffer.wrap(shortDataArray);
        shortBb.order(ByteOrder.LITTLE_ENDIAN);
        longBb.putInt(recordsNum);
        byte[] array = longBb.array();
        fileChannel.position(recordsNumPosition);
        fos.write(array);
        fileChannel.position(pointFormatPosition);
        fos.write(pointFormat);
        shortBb.putShort(recordLength);
        array = shortBb.array();
        fileChannel.position(recordLengthPosition);
        fos.write(array);

        closeFile();

        /*
         * write crs file
         */
        if (crs != null)
            CrsUtilities.writeProjectionFile(prjFile.getAbsolutePath(), null, crs);
    }

    // private byte[] getLong( int num ) {
    // longBb.clear();
    // longBb.putInt(num);
    // byte[] array = longBb.array();
    // return array;
    // }
    //
    // // private byte[] getLong( double num ) {
    // // longBb.clear();
    // // longBb.putDouble(num);
    // // byte[] array = longBb.array();
    // // return array;
    // // }
    //
    // private byte[] getDouble( double num ) {
    // doubleBb.clear();
    // doubleBb.putDouble(num);
    // byte[] array = doubleBb.array();
    // return array;
    // }
    //
    // private byte[] getShort( short num ) {
    // shortBb.clear();
    // shortBb.putShort(num);
    // return shortBb.array();
    // }

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

    @Override
    public void setWriteGroundElevation( boolean doWriteGroundElevation ) {
        this.doWriteGroundElevation = doWriteGroundElevation;
    }

    @Override
    public void setGpsTimeType( int timeType ) {
        gpsTimeType = timeType;
    }

}
