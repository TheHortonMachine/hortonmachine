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
package org.hortonmachine.gears.io.las.core.laszip4j;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.joda.time.DateTime;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.github.mreutegg.laszip4j.LASHeader;

/**
 * Las header object for las spec 1.0. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LaszipHeader implements ILasHeader {

    private LASHeader header;
    private CoordinateReferenceSystem crs;
    private ReferencedEnvelope3D dataEnvelope;

    public LaszipHeader( LASHeader header, CoordinateReferenceSystem crs ) {
        this.header = header;
        this.crs = crs;
    }

    public String getVersion() {
        return header.getVersionMajor() + "." + header.getVersionMinor();
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public long getRecordsCount() {
        return header.getLegacyNumberOfPointRecords();
    }

    @Override
    public long getOffset() {
        return header.getOffsetToPointData();
    }

    public double[] getXYZScale() {
        return new double[]{header.getXScaleFactor(), header.getYScaleFactor(), header.getZScaleFactor()};
    }

    public double[] getXYZOffset() {
        return new double[]{header.getXOffset(), header.getYOffset(), header.getZOffset()};
    }

    public short getRecordLength() {
        return (short) header.getPointDataRecordLength();
    }

    public ReferencedEnvelope3D getDataEnvelope() {
        if (dataEnvelope == null) {
            dataEnvelope = new ReferencedEnvelope3D(header.getMinX(), header.getMaxX(), header.getMinY(), header.getMaxY(),
                    header.getMinZ(), header.getMaxZ(), getCrs());
        }
        return dataEnvelope;
    }

    public boolean hasGpsTime() {
        return true;
    }

    public int getGpsTimeType() {
        return 0;
    }

    public boolean hasRGB() {
        return true;
    }

    public byte getPointDataFormat() {
        return getPointDataFormat();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("File signature: ").append(new String(header.getFileSignature())).append("\n");
        sb.append("File source ID: ").append(header.getFileSourceID()).append("\n");
        sb.append("Project ID - data 1: ").append(header.getProjectID_GUIDData1()).append("\n");
        sb.append("Project ID - data 2: ").append(header.getProjectID_GUIDData2()).append("\n");
        sb.append("Project ID - data 3: ").append(header.getProjectID_GUIDData3()).append("\n");
        sb.append("Project ID - data 4: ").append(new String(header.getProjectID_GUIDData4())).append("\n");
        sb.append("Version: ").append(getVersion()).append("\n");
        sb.append("System identifier: ").append(new String(header.getSystemIdentifier())).append("\n");
        sb.append("Generating software: ").append(header.getGeneratingSoftware()).append("\n");
        try {
            short fileCreationYear = (short) header.getFileCreationYear();
            short fileCreationDayOfYear = (short) header.getFileCreationDayOfYear();
            String dtString = " - nv - ";
            if (fileCreationYear != 0 && fileCreationDayOfYear != 0) {
                DateTime dateTime = new DateTime();
                dateTime = dateTime.withYear(fileCreationYear).withDayOfYear(fileCreationDayOfYear);
                dtString = dateTime.toString(LasUtils.dateTimeFormatterYYYYMMDD);
            }
            sb.append("File creation date: ").append(dtString).append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.append("Header size: ").append(header.getHeaderSize()).append("\n");
        sb.append("Offset to data: ").append(getOffset()).append("\n");
        sb.append("Variable length records: ").append(header.getNumberOfVariableLengthRecords()).append("\n");
        sb.append("Point data format ID (0-99 for spec): ").append(header.getPointDataRecordFormat()).append("\n");
        sb.append("Number of point records: ").append(getRecordsCount()).append("\n");
        sb.append("Record length: ").append(getRecordLength()).append("\n");
        sb.append("Scale: [").append(header.getXScaleFactor()).append(", ").append(header.getYScaleFactor()).append(", ")
                .append(header.getZScaleFactor()).append("]\n");
        sb.append("Offset: [").append(header.getXOffset()).append(", ").append(header.getYOffset()).append(", ")
                .append(header.getZOffset()).append("]\n");
        sb.append("X Range: [").append(header.getMinX()).append(", ").append(header.getMaxX()).append("]\n");
        sb.append("Y Range: [").append(header.getMinY()).append(", ").append(header.getMaxY()).append("]\n");
        sb.append("Z Range: [").append(header.getMinZ()).append(", ").append(header.getMaxZ()).append("]\n");
        sb.append("Has gps time info: ").append(hasGpsTime()).append("\n");
        sb.append("Has color info: ").append(hasRGB()).append("\n");
        sb.append("Gps time type: ").append(getGpsTimeType()).append("\n");
        return sb.toString();
    }

    @Override
    public double[] getRawDataEnvelope() {
        return new double[]{header.getMinX(), header.getMaxX(), header.getMinY(), header.getMaxY(), header.getMinZ(),
                header.getMaxZ()};
    }

    @Override
    public String getFileSignature() {
        return header.getFileSignature();
    }

    @Override
    public char getFileSourceID() {
        return header.getFileSourceID();
    }

    @Override
    public int getProjectID_GUIDData1() {
        return header.getProjectID_GUIDData1();
    }

    @Override
    public char getProjectID_GUIDData2() {
        return header.getProjectID_GUIDData2();
    }

    @Override
    public char getProjectID_GUIDData3() {
        return header.getProjectID_GUIDData3();
    }

    @Override
    public byte[] getProjectID_GUIDData4() {
        return header.getProjectID_GUIDData4();
    }

    @Override
    public String getSystemIdentifier() {
        return header.getSystemIdentifier();
    }

    @Override
    public String getGeneratingSoftware() {
        return header.getGeneratingSoftware();
    }

    @Override
    public short getFileCreationYear() {
        return (short) header.getFileCreationYear();
    }

    @Override
    public short getFileCreationDayOfYear() {
        return (short) header.getFileCreationDayOfYear();
    }

    @Override
    public char getHeaderSize() {
        return header.getHeaderSize();
    }

    @Override
    public int getNumberOfVariableLengthRecords() {
        return header.getNumberOfVariableLengthRecords();
    }

    @Override
    public byte getPointDataRecordFormat() {
        return header.getPointDataRecordFormat();
    }

}
