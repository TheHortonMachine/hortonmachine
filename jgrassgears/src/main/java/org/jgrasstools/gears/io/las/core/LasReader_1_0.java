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
package org.jgrasstools.gears.io.las.core;

import java.io.File;
import java.io.IOException;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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

    public LasReader_1_0( File lasFile, CoordinateReferenceSystem crs ) {
        super(lasFile, crs);
    }

    @SuppressWarnings("nls")
    protected void parseHeader() throws Exception {

        try {
            StringBuilder sb = new StringBuilder();
            // file signature (LASF)
            String signature = getString(4);
            sb.append("File signature: ").append(signature).append("\n");

            // file source ID
            short fileSourceId = getShort2Bytes();
            sb.append("File source ID: ").append(fileSourceId).append("\n");

            // reserved (optional)
            getShort2Bytes();

            // Project ID - data 1 (optional)
            long projectIdGuidData1 = getLong4Bytes();
            sb.append("Project ID - data 1: ").append(projectIdGuidData1).append("\n");

            // Project ID - data 2 (optional)
            short projectIdGuidData2 = getShort2Bytes();
            sb.append("Project ID - data 2: ").append(projectIdGuidData2).append("\n");

            // Project ID - data 3 (optional)
            short projectIdGuidData3 = getShort2Bytes();
            sb.append("Project ID - data 3: ").append(projectIdGuidData3).append("\n");

            // Project ID - data 4 (optional)
            String projectIdGuidData4 = getString(8);
            sb.append("Project ID - data 4: ").append(projectIdGuidData4).append("\n");

            // Version Major
            byte versionMajor = get();
            // Version Minor
            byte versionMinor = get();
            version = versionMajor + "." + versionMinor;
            sb.append("Version: ").append(versionMajor).append(".").append(versionMinor).append("\n");

            // System identifier
            String systemIdentifier = getString(32);
            sb.append("System identifier: ").append(systemIdentifier).append("\n");

            // generating software
            String generatingSoftware = getString(32);
            sb.append("Generating software: ").append(generatingSoftware).append("\n");

            // File creation Day of Year (optional)
            short dayOfYear = getShort2Bytes();

            // File creation Year (optional)
            short year = getShort2Bytes();

            if (dayOfYear != 0 && year != 0) {
                DateTime dateTime = new DateTime();
                dateTime = dateTime.withYear(year).withDayOfYear(dayOfYear);
                String dtString = dateTime.toString(dateTimeFormatterYYYYMMDD);
                sb.append("File creation date: ").append(dtString).append("\n");
            } else {
                sb.append("File creation Day of Year: ").append(dayOfYear).append("\n");
                sb.append("File creation Year: ").append(year).append("\n");
            }

            // header size
            short headerSize = getShort2Bytes();
            sb.append("Header size: ").append(headerSize).append("\n");

            // offset to point data
            offset = getLong4Bytes();

            // Number of variable length records
            long variableLengthRecordNum = getLong4Bytes();
            sb.append("Variable length records: ").append(variableLengthRecordNum).append("\n");

            // point data format ID (0-99 for spec)
            byte pointDataFormat = get();
            sb.append("Point data format ID (0-99 for spec): ").append(pointDataFormat).append("\n");

            recordLength = getShort2Bytes();

            // Number of point records
            records = getLong4Bytes();

            sb.append("Number of point records: ").append(records).append("\n");

            fc.position(fc.position() + 20); // skip

            xScale = getDouble8Bytes();
            yScale = getDouble8Bytes();
            zScale = getDouble8Bytes();
            xOffset = getDouble8Bytes();
            yOffset = getDouble8Bytes();
            zOffset = getDouble8Bytes();
            xMax = getDouble8Bytes();
            xMin = getDouble8Bytes();
            yMax = getDouble8Bytes();
            yMin = getDouble8Bytes();
            zMax = getDouble8Bytes();
            zMin = getDouble8Bytes();
            sb.append("X Range: [").append(xMin).append(", ").append(xMax).append("]\n");
            sb.append("Y Range: [").append(yMin).append(", ").append(yMax).append("]\n");
            sb.append("Z Range: [").append(zMin).append(", ").append(zMax).append("]\n");

            header = sb.toString();

            if (crs != null) {
                crs = DefaultGeographicCRS.WGS84;
            }
            dataEnvelope = new ReferencedEnvelope(xMin, xMax, yMin, yMax, crs);

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
        long x = getLong4Bytes();
        long y = getLong4Bytes();
        long z = getLong4Bytes();
        double xd = x * xScale + xOffset;
        double yd = y * yScale + yOffset;
        double zd = z * zScale + zOffset;

        // if (!isBetween(xd, xMin, xMax)) {
        // throw new ModelsIllegalargumentException("Data not in supposed range.", this);
        // }
        // if (!isBetween(yd, yMin, yMax)) {
        // throw new ModelsIllegalargumentException("Data not in supposed range.", this);
        // }
        // if (!isBetween(zd, zMin, zMax)) {
        // throw new ModelsIllegalargumentException("Data not in supposed range.", this);
        // }

        read = read + 12;
        short intensity = getShort2Bytes();
        read = read + 2;
        byte b = get();
        int returnNumber = getReturnNumber(b);
        int numberOfReturns = getNumberOfReturns(b);
        read = read + 1;
        byte classification = get();
        read = read + 1;

        int skip = recordLength - read;
        skip(skip);

        LasRecord dot = new LasRecord();
        dot.x = xd;
        dot.y = yd;
        dot.z = zd;
        dot.intensity = intensity;
        dot.classification = classification;
        dot.returnNumber = returnNumber;
        dot.numberOfReturns = numberOfReturns;

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

        LasRecord dot = new LasRecord();
        dot.x = xd;
        dot.y = yd;
        dot.z = zd;
        dot.intensity = intensity;
        dot.classification = classification;
        dot.returnNumber = returnNumber;
        dot.numberOfReturns = numberOfReturns;

        // fc.position(oldPosition);

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
    public String getHeader() {
        checkOpen();
        return header;
    }

}
