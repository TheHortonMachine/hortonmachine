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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Las record reader for las spec 1.0. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasReaderBuffered extends ALasReader {
    private final byte[] doubleDataArray = new byte[8];
    private final ByteBuffer doubleBb = ByteBuffer.wrap(doubleDataArray);
    private final byte[] longDataArray = new byte[4];
    private final ByteBuffer longBb = ByteBuffer.wrap(longDataArray);
    private final byte[] shortDataArray = new byte[2];
    private final ByteBuffer shortBb = ByteBuffer.wrap(shortDataArray);
    private final byte[] singleDataArray = new byte[1];
    private final ByteBuffer singleBb = ByteBuffer.wrap(singleDataArray);

    private byte[] readingDataArray = null;
    private ByteBuffer bufferedReadingBb = null;

    private double xScale;
    private double yScale;
    private double zScale;
    private double xOffset;
    private double yOffset;
    private double zOffset;
    private final File lasFile;
    private FileChannel fc;
    private FileInputStream fis;
    private long offset;
    private long records;
    private short recordLength;
    private boolean isOpen;
    private CoordinateReferenceSystem crs;

    private long readRecords = 0;

    private double xMax;
    private double xMin;
    private double yMax;
    private double yMin;
    private double zMax;
    private double zMin;

    private LasHeader header;
    private int bufferSizeInPointsNum;
    private int readBufferSize;

    public LasReaderBuffered( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        this(lasFile, 100000, crs);
    }

    public LasReaderBuffered( File lasFile, int bufferSizeInPointsNum, CoordinateReferenceSystem crs ) throws Exception {
        this.lasFile = lasFile;
        this.bufferSizeInPointsNum = bufferSizeInPointsNum;

        if (crs != null) {
            this.crs = crs;
        } else {
            try {
                this.crs = CrsUtilities.readProjectionFile(lasFile.getAbsolutePath(), "las");
            } catch (Exception e) {
                // ignore
            }
        }
        doubleBb.order(ByteOrder.LITTLE_ENDIAN);
        longBb.order(ByteOrder.LITTLE_ENDIAN);
        shortBb.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public File getLasFile() {
        return lasFile;
    }

    private void checkOpen() {
        if (!isOpen) {
            try {
                open();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void open() throws Exception {
        if (fc != null)
            fc.close();
        if (fis != null)
            fis.close();
        fis = new FileInputStream(lasFile);
        fc = fis.getChannel();

        readingDataArray = null;
        bufferedReadingBb = null;
        xScale = 0;
        yScale = 0;
        zScale = 0;
        xOffset = 0;
        yOffset = 0;
        zOffset = 0;
        offset = 0;
        records = 0;
        recordLength = 0;
        readRecords = 0;
        xMax = 0;
        xMin = 0;
        yMax = 0;
        yMin = 0;
        zMax = 0;
        zMin = 0;
        readBufferSize = 0;

        parseHeader();
        isOpen = true;
    }

    @Override
    public void close() throws Exception {
        if (fc != null && fc.isOpen())
            fc.close();
        if (fis != null)
            fis.close();
        isOpen = false;
    }

    @Override
    public void setOverrideGpsTimeType( int type ) {
        getHeader();
        header.gpsTimeType = type;
    }

    @SuppressWarnings("nls")
    private void parseHeader() throws Exception {

        try {
            header = new LasHeader(crs);

            String signature = getString(4);
            header.signature = signature;

            short fileSourceId = getShort2Bytes();
            header.fileSourceId = fileSourceId;

            // reserved (optional)
            byte globalEnchodingBitFirstHalf = get();
            // byte globalEnchodingBitSecondHalf = get();
            skip(1);
            int gpsTimeType = getGpsTimeType(globalEnchodingBitFirstHalf);
            header.gpsTimeType = gpsTimeType;

            long projectIdGuidData1 = getLong4Bytes();
            header.projectIdGuidData1 = projectIdGuidData1;
            short projectIdGuidData2 = getShort2Bytes();
            header.projectIdGuidData2 = projectIdGuidData2;
            short projectIdGuidData3 = getShort2Bytes();
            header.projectIdGuidData3 = projectIdGuidData3;
            String projectIdGuidData4 = getString(8);
            header.projectIdGuidData4 = projectIdGuidData4;

            byte versionMajor = get();
            byte versionMinor = get();
            header.versionMajor = versionMajor;
            header.versionMinor = versionMinor;

            String systemIdentifier = getString(32);
            header.systemIdentifier = systemIdentifier;

            String generatingSoftware = getString(32);
            header.generatingSoftware = generatingSoftware;

            short dayOfYear = getShort2Bytes();
            header.dayOfYear = dayOfYear;

            short year = getShort2Bytes();
            header.year = year;

            short headerSize = getShort2Bytes();
            header.headerSize = headerSize;

            offset = getLong4Bytes();
            header.offset = offset;

            long variableLengthRecordNum = getLong4Bytes();
            header.variableLengthRecordNum = variableLengthRecordNum;

            byte pointDataFormat = get();
            header.pointDataFormat = pointDataFormat;

            recordLength = getShort2Bytes();
            header.recordLength = recordLength;

            records = getLong4Bytes();
            header.records = records;

            fc.position(fc.position() + 20); // skip

            xScale = getDouble8Bytes();
            header.xScale = xScale;
            yScale = getDouble8Bytes();
            header.yScale = yScale;
            zScale = getDouble8Bytes();
            header.zScale = zScale;
            xOffset = getDouble8Bytes();
            header.xOffset = xOffset;
            yOffset = getDouble8Bytes();
            header.yOffset = yOffset;
            zOffset = getDouble8Bytes();
            header.zOffset = zOffset;
            xMax = getDouble8Bytes();
            header.xMax = xMax;
            xMin = getDouble8Bytes();
            header.xMin = xMin;
            yMax = getDouble8Bytes();
            header.yMax = yMax;
            yMin = getDouble8Bytes();
            header.yMin = yMin;
            zMax = getDouble8Bytes();
            header.zMax = zMax;
            zMin = getDouble8Bytes();
            header.zMin = zMin;

            /*
             * move to the data position
             */
            fc.position(offset);

            readBufferSize = bufferSizeInPointsNum * recordLength;
            readingDataArray = new byte[readBufferSize];
            bufferedReadingBb = ByteBuffer.wrap(readingDataArray);
            bufferedReadingBb.order(ByteOrder.LITTLE_ENDIAN);

            // read the first set of data
            readDataBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int readDataBuffer() throws IOException {
        bufferedReadingBb.clear();
        int read = fc.read(bufferedReadingBb);
        bufferedReadingBb.position(0);
        return read;
    }

    @Override
    public boolean hasNextPoint() throws IOException {
        if (readRecords < records) {
            if (!bufferedReadingBb.hasRemaining()) {
                readDataBuffer();
            }
            return true;
        }
        return false;
    }

    @Override
    public LasRecord getNextPoint() throws IOException {
        int read = 0;
        final long x = bufferedReadingBb.getInt();
        final long y = bufferedReadingBb.getInt();
        final long z = bufferedReadingBb.getInt();
        final double xd = x * xScale + xOffset;
        final double yd = y * yScale + yOffset;
        final double zd = z * zScale + zOffset;

        read = read + 12;
        final short intensity = bufferedReadingBb.getShort();
        read = read + 2;
        final byte b = bufferedReadingBb.get();
        final short returnNumber = getReturnNumber(b);
        final short numberOfReturns = getNumberOfReturns(b);
        read = read + 1;
        final byte classification = bufferedReadingBb.get();
        read = read + 1;

        // skip:
        // scan angle rank (1 byte)
        // file marker (1 byte)
        // Point Source ID (2 byte)

        // to skip 4 bytes read an int -> was skip(4);
        bufferedReadingBb.getInt();
        read = read + 4;

        final LasRecord dot = new LasRecord();
        dot.x = xd;
        dot.y = yd;
        dot.z = zd;
        dot.intensity = intensity;
        dot.classification = classification;
        dot.returnNumber = returnNumber;
        dot.numberOfReturns = numberOfReturns;
        if (header.pointDataFormat == 1) {
            dot.gpsTime = bufferedReadingBb.getDouble();
            read = read + 8;
        } else if (header.pointDataFormat == 2) {
            dot.color[0] = bufferedReadingBb.getShort();
            dot.color[1] = bufferedReadingBb.getShort();
            dot.color[2] = bufferedReadingBb.getShort();
            read = read + 6;
        } else if (header.pointDataFormat == 3) {
            dot.gpsTime = bufferedReadingBb.getDouble();
            dot.color[0] = bufferedReadingBb.getShort();
            dot.color[1] = bufferedReadingBb.getShort();
            dot.color[2] = bufferedReadingBb.getShort();
            read = read + 14;
        }

        int skip = recordLength - read;
        if (skip > 0)
            bufferedReadingBb.position(bufferedReadingBb.position() + skip);

        readRecords++;
        return dot;
    }

    public LasRecord getPointAtAddress( long address ) throws IOException {
        fc.position(address);
        return getPoint();
    }

    @Override
    public LasRecord getPointAt( long pointNumber ) throws IOException {
        fc.position(offset + pointNumber * recordLength);
        return getPoint();
    }

    private LasRecord getPoint() throws IOException {
        int read = 0;
        final long x = getLong4Bytes();
        final long y = getLong4Bytes();
        final long z = getLong4Bytes();
        final double xd = x * xScale + xOffset;
        final double yd = y * yScale + yOffset;
        final double zd = z * zScale + zOffset;

        read = read + 12;
        final short intensity = getShort2Bytes();
        read = read + 2;
        final byte b = get();
        final short returnNumber = getReturnNumber(b);
        final short numberOfReturns = getNumberOfReturns(b);
        read = read + 1;
        final byte classification = get();
        read = read + 1;

        // skip:
        // scan angle rank (1 byte)
        // file marker (1 byte)
        // Point Source ID (2 byte)
        skip(4);
        read = read + 4;

        final LasRecord dot = new LasRecord();
        dot.x = xd;
        dot.y = yd;
        dot.z = zd;
        dot.intensity = intensity;
        dot.classification = classification;
        dot.returnNumber = returnNumber;
        dot.numberOfReturns = numberOfReturns;
        if (header.pointDataFormat == 1) {
            dot.gpsTime = getDouble8Bytes();
            read = read + 8;
        } else if (header.pointDataFormat == 2) {
            dot.color[0] = getShort2Bytes();
            dot.color[1] = getShort2Bytes();
            dot.color[2] = getShort2Bytes();
            read = read + 6;
        } else if (header.pointDataFormat == 3) {
            dot.gpsTime = getDouble8Bytes();
            dot.color[0] = getShort2Bytes();
            dot.color[1] = getShort2Bytes();
            dot.color[2] = getShort2Bytes();
            read = read + 14;
        }
        return dot;
    }

    @Override
    public double[] readNextLasXYZAddress() throws IOException {
        long position = fc.position();
        int read = 0;
        long x = getLong4Bytes();
        long y = getLong4Bytes();
        long z = getLong4Bytes();
        double xd = x * xScale + xOffset;
        double yd = y * yScale + yOffset;
        double zd = z * zScale + zOffset;

        read = read + 12;

        int skip = recordLength - read;
        skip(skip);

        readRecords++;
        return new double[]{xd, yd, zd, position};
    }

    public void seek( long pointNumber ) throws IOException {
        // long bytesToSkip = recordLength * newPosition;
        // fc.position(fc.position() + bytesToSkip);
        fc.position(offset + pointNumber * recordLength);
        readRecords = pointNumber;
    }

    @Override
    public ILasHeader getHeader() {
        checkOpen();
        return header;
    }

    private String getString( int size ) throws IOException {
        byte[] bytesStr = new byte[size];
        ByteBuffer singleBb = ByteBuffer.wrap(bytesStr);
        fc.read(singleBb);
        String signature = new String(bytesStr);
        return signature;
    }

    private long getLong4Bytes() throws IOException {
        longBb.clear();
        fc.read(longBb);
        long arr2long = longBb.getInt(0);
        return arr2long;
    }

    private double getDouble8Bytes() throws IOException {
        doubleBb.clear();
        fc.read(doubleBb);
        double arr2Double = doubleBb.getDouble(0);
        return arr2Double;
    }

    private short getShort2Bytes() throws IOException {
        shortBb.clear();
        fc.read(shortBb);
        short arr2short = shortBb.getShort(0);
        return arr2short;
    }

    private byte get() throws IOException {
        singleBb.clear();
        fc.read(singleBb);
        return singleBb.get(0);
    }

    private void skip( int bytesTpSkip ) throws IOException {
        fc.position(fc.position() + bytesTpSkip);
    }

    private short getReturnNumber( byte b ) {
        short rn = 0;
        for( int i = 0; i < 3; i++ ) {
            if (isSet(b, i)) {
                rn = (short) (rn + Math.pow(2.0, i));
            }
        }
        return rn;
    }

    /**
     * Checks the gps time type.
     * 
     * <p>
     * <ul>
     * <li>0 (not set) = GPS time in the point record fields is GPS Week Time</li>
     * <li>1 (set) = GPS time is standard GPS time (satellite gps time) minus 1E9 (Adjusted standard GPS time)</li>
     * </ul>
     * 
     * @param b the global enchoding byte.
     * @return 0 or 1;
     */
    private int getGpsTimeType( byte b ) {
        return isSet(b, 0) ? 1 : 0;
    }

    private short getNumberOfReturns( byte b ) {
        short nor = 0;
        for( int i = 3; i < 6; i++ ) {
            if (isSet(b, i)) {
                nor = (short) (nor + Math.pow(2.0, i - 3));
            }
        }
        return nor;
    }

    private boolean isSet( byte b, int n ) { // true if bit n is set in byte b
        return (b & (1 << n)) != 0;
    }

    @Override
    public void rewind() throws IOException {
        try {
            close();
            open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
