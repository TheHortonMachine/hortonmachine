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

import java.io.File;
import java.io.IOException;

import org.jgrasstools.gears.io.las.core.AbstractLasReader;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.joda.time.DateTime;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Las record reader for las spec 1.0. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasReader_1_0 extends AbstractLasReader {

    private long readRecords = 0;

    private double xMax;
    private double xMin;
    private double yMax;
    private double yMin;
    private double zMax;
    private double zMin;

    private LasHeader_1_0 header;

    public LasReader_1_0( File lasFile, CoordinateReferenceSystem crs ) {
        super(lasFile, crs);
    }

    public void setOverrideGpsTimeType( int type ) {
        getHeader();
        header.gpsTimeType = type;
    }

    @SuppressWarnings("nls")
    protected void parseHeader() throws Exception {

        try {
            header = new LasHeader_1_0(crs);

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNextLasDot() {
        if (readRecords < records) {
            return true;
        }
        return false;
    }

    @Override
    public LasRecord readNextLasDot() throws IOException {
        int read = 0;
        // x
        long x = getLong4Bytes();
        // y
        long y = getLong4Bytes();
        // z
        long z = getLong4Bytes();
        double xd = x * xScale + xOffset;
        double yd = y * yScale + yOffset;
        double zd = z * zScale + zOffset;

        read = read + 12;
        // intensity
        short intensity = getShort2Bytes();
        read = read + 2;
        // return number
        byte b = get();
        int returnNumber = getReturnNumber(b);
        // number of returns (given pulse)
        int numberOfReturns = getNumberOfReturns(b);
        read = read + 1;
        // classification
        byte classification = get();
        read = read + 1;
        // skip:
        // scan angle rank (1 byte)
        // file marker (1 byte)
        // Point Source ID (2 byte)
        skip(4);
        read = read + 4;

        LasRecord dot = new LasRecord();
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
        int skip = recordLength - read;
        skip(skip);

        readRecords++;
        return dot;
    }

    /**
     * Reads a dot at a given address.
     * 
     * <p>the file position is set back to the one before reading.</p>
     * 
     * @param address the file address of the record to read.
     * @return the read record.
     * @throws IOException
     */
    public LasRecord readLasDotAtAddress( long address ) throws IOException {
        // long oldPosition = fc.position();
        fc.position(address);

        int read = 0;
        long x = getLong4Bytes();
        long y = getLong4Bytes();
        long z = getLong4Bytes();
        double xd = x * xScale + xOffset;
        double yd = y * yScale + yOffset;
        double zd = z * zScale + zOffset;

        read = read + 12;
        short intensity = getShort2Bytes();
        read = read + 2;
        byte b = get();
        int returnNumber = getReturnNumber(b);
        int numberOfReturns = getNumberOfReturns(b);
        read = read + 1;
        byte classification = get();
        read = read + 1;

        // skip:
        // scan angle rank (1 byte)
        // file marker (1 byte)
        // Point Source ID (2 byte)
        skip(4);
        read = read + 4;

        LasRecord dot = new LasRecord();
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
        // int skip = recordLength - read;
        // skip(skip);

        return dot;
    }

    /**
     * Reads the position and the record address in the file of the next point.
     * 
     * @return the array containing [x, y, address].
     * @throws IOException
     */
    public double[] readNextLasXYAddress() throws IOException {
        long position = fc.position();
        int read = 0;
        long x = getLong4Bytes();
        long y = getLong4Bytes();
        double xd = x * xScale + xOffset;
        double yd = y * yScale + yOffset;
        read = read + 8;

        int skip = recordLength - read;
        skip(skip);

        readRecords++;
        return new double[]{xd, yd, position};
    }

    @Override
    public long getRecordsCount() {
        checkOpen();
        return records;
    }

    @Override
    public ILasHeader getHeader() {
        checkOpen();
        return header;
    }

    public DateTime getRecordDateTime( LasRecord record ) {
        return LasUtils.gpsTimeToDateTime(record.gpsTime, header.gpsTimeType);
    }

}
