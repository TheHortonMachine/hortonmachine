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

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.joda.time.DateTime;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Las header object for las spec 1.0. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasHeader implements ILasHeader {

    /**
     *  file signature (LASF)
     */
    String signature;
    /**
     *  file source ID
     */
    short fileSourceId;

    /**
     * The gps time type.
     * 
     * <p>
     * <ul>
     * <li>0 (not set) = GPS time in the point record fields is GPS Week Time</li>
     * <li>1 (set) = GPS time is standard GPS time (satellite gps time) minus 1E9 (Adjusted standard GPS time)</li>
     * </ul>
     */
    int gpsTimeType;

    /**
     *  Project ID - data 1 (optional)
     */
    long projectIdGuidData1;
    /**
     *  Project ID - data 2 (optional)
     */
    short projectIdGuidData2;
    /**
     *  Project ID - data 3 (optional)
     */
    short projectIdGuidData3;
    /**
     *  Project ID - data 4 (optional)
     */
    String projectIdGuidData4;
    /**
     *  Version Major
     */
    byte versionMajor;
    /**
     *  Version Minor
     */
    byte versionMinor;
    /**
     *  System identifier
     */
    String systemIdentifier;
    /**
     *  generating software
     */
    String generatingSoftware;
    /**
     *  File creation Day of Year (optional)
     */
    short dayOfYear;
    /**
     *  File creation Year (optional)
     */
    short year;
    /**
     *  header size
     */
    short headerSize;
    /**
     *  offset to point data
     */
    long offset;
    /**
     *  Number of variable length records
     */
    long variableLengthRecordNum;
    /**
     *  point data format ID (0-99 for spec)
     */
    byte pointDataFormat;
    /**
     *  Record length
     */
    short recordLength;
    /**
     *  Number of point records
     */
    long records;
    double xScale;
    double yScale;
    double zScale;
    double xOffset;
    double yOffset;
    double zOffset;
    double xMin;
    double yMin;
    double zMin;
    double xMax;
    double yMax;
    double zMax;

    private ReferencedEnvelope3D dataEnvelope;
    private CoordinateReferenceSystem crs;

    public LasHeader( CoordinateReferenceSystem crs ) {
        this.crs = crs;
    }

    public String getVersion() {
        return versionMajor + "." + versionMinor;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public long getRecordsCount() {
        return records;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    public double[] getXYZScale() {
        return new double[]{xScale, yScale, zScale};
    }

    public double[] getXYZOffset() {
        return new double[]{xOffset, yOffset, zOffset};
    }

    public short getRecordLength() {
        return recordLength;
    }

    public ReferencedEnvelope3D getDataEnvelope() {
        if (dataEnvelope == null) {
            dataEnvelope = new ReferencedEnvelope3D(xMin, xMax, yMin, yMax, zMin, zMax, getCrs());
        }
        return dataEnvelope;
    }

    public boolean hasGpsTime() {
        return pointDataFormat == 1 || pointDataFormat == 3;
    }

    public int getGpsTimeType() {
        return gpsTimeType;
    }

    public boolean hasRGB() {
        return pointDataFormat == 2 || pointDataFormat == 3;
    }

    public byte getPointDataFormat() {
        return pointDataFormat;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("File signature: ").append(signature).append("\n");
        sb.append("File source ID: ").append(fileSourceId).append("\n");
        sb.append("Project ID - data 1: ").append(projectIdGuidData1).append("\n");
        sb.append("Project ID - data 2: ").append(projectIdGuidData2).append("\n");
        sb.append("Project ID - data 3: ").append(projectIdGuidData3).append("\n");
        sb.append("Project ID - data 4: ").append(projectIdGuidData4).append("\n");
        sb.append("Version: ").append(versionMajor).append(".").append(versionMinor).append("\n");
        sb.append("System identifier: ").append(systemIdentifier).append("\n");
        sb.append("Generating software: ").append(generatingSoftware).append("\n");
        if (dayOfYear != 0 && year != 0) {
            DateTime dateTime = new DateTime();
            dateTime = dateTime.withYear(year).withDayOfYear(dayOfYear);
            String dtString = dateTime.toString(LasUtils.dateTimeFormatterYYYYMMDD);
            sb.append("File creation date: ").append(dtString).append("\n");
        } else {
            sb.append("File creation Day of Year: ").append(dayOfYear).append("\n");
            sb.append("File creation Year: ").append(year).append("\n");
        }
        sb.append("Header size: ").append(headerSize).append("\n");
        sb.append("Offset to data: ").append(offset).append("\n");
        sb.append("Variable length records: ").append(variableLengthRecordNum).append("\n");
        sb.append("Point data format ID (0-99 for spec): ").append(pointDataFormat).append("\n");
        sb.append("Number of point records: ").append(records).append("\n");
        sb.append("Record length: ").append(recordLength).append("\n");
        sb.append("Scale: [").append(xScale).append(", ").append(yScale).append(", ").append(zScale).append("]\n");
        sb.append("Offset: [").append(xOffset).append(", ").append(yOffset).append(", ").append(zOffset).append("]\n");
        sb.append("X Range: [").append(xMin).append(", ").append(xMax).append("]\n");
        sb.append("Y Range: [").append(yMin).append(", ").append(yMax).append("]\n");
        sb.append("Z Range: [").append(zMin).append(", ").append(zMax).append("]\n");
        sb.append("Has gps time info: ").append(hasGpsTime()).append("\n");
        sb.append("Has color info: ").append(hasRGB()).append("\n");
        sb.append("Gps time type: ").append(gpsTimeType).append("\n");
        return sb.toString();
    }

    @Override
    public double[] getRawDataEnvelope() {
        return new double[]{xMin, yMin, zMin, xMax, yMax, zMax};
    }

    @Override
    public String getFileSignature() {
        return signature;
    }

    @Override
    public char getFileSourceID() {
        return (char) fileSourceId;
    }

    @Override
    public int getProjectID_GUIDData1() {
        return (int) projectIdGuidData1;
    }

    @Override
    public char getProjectID_GUIDData2() {
        return (char) projectIdGuidData2;
    }

    @Override
    public char getProjectID_GUIDData3() {
        return (char) projectIdGuidData3;
    }

    @Override
    public byte[] getProjectID_GUIDData4() {
        return projectIdGuidData4.getBytes();
    }

    @Override
    public String getSystemIdentifier() {
        return systemIdentifier;
    }

    @Override
    public String getGeneratingSoftware() {
        return generatingSoftware;
    }

    @Override
    public short getFileCreationYear() {
        return year;
    }

    @Override
    public short getFileCreationDayOfYear() {
        return dayOfYear;
    }

    @Override
    public char getHeaderSize() {
        return (char) headerSize;
    }

    @Override
    public int getNumberOfVariableLengthRecords() {
        return (int) variableLengthRecordNum;
    }

    @Override
    public byte getPointDataRecordFormat() {
        return pointDataFormat;
    }

}
