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
package org.jgrasstools.gears.io.las.core.liblas;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.joda.time.DateTime;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Las header read from liblas.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LiblasHeader implements ILasHeader {

    private LiblasJNALibrary WRAPPER;
    // private long srsHandle;

    private String signature = "";
    private short fileSourceId = 0;
    // private byte globalEnchodingBitFirstHalf = 0;

    private long projectIdGuidData1 = 0;
    private short projectIdGuidData2 = 0;
    private short projectIdGuidData3 = 0;
    private String projectIdGuidData4 = "";

    private byte versionMajor = 0;
    private byte versionMinor = 0;

    private String systemIdentifier = "";
    private String generatingSoftware = "";

    private short dayOfYear = 0;
    private short year = 0;
    private short headerSize = 0;

    private long offset = 0;

    private long variableLengthRecordNum = 0;
    private byte pointDataFormat = 0;

    private short recordLength = 0;
    private long pointRecordsCount = 0;

    private int gpsTimeType;

    private double xScale = 0.0;
    private double yScale = 0.0;
    private double zScale = 0.0;
    private double xOffset = 0.0;
    private double yOffset = 0.0;
    private double zOffset = 0.0;
    private double xMax = 0.0;
    private double xMin = 0.0;
    private double yMax = 0.0;
    private double yMin = 0.0;
    private double zMax = 0.0;
    private double zMin = 0.0;
    private CoordinateReferenceSystem crs;

    private ReferencedEnvelope3D dataEnvelope;

    public LiblasHeader( LiblasJNALibrary wrapper, long headerHandle, CoordinateReferenceSystem crs ) {
        this.WRAPPER = wrapper;
        this.crs = crs;
        // srsHandle = WRAPPER.LASHeader_GetSRS(headerHandle);

        pointRecordsCount = WRAPPER.LASHeader_GetPointRecordsCount(headerHandle);
        recordLength = WRAPPER.LASHeader_GetDataRecordLength(headerHandle);

        xMin = WRAPPER.LASHeader_GetMinX(headerHandle);
        yMin = WRAPPER.LASHeader_GetMinY(headerHandle);
        zMin = WRAPPER.LASHeader_GetMinZ(headerHandle);
        xMax = WRAPPER.LASHeader_GetMaxX(headerHandle);
        yMax = WRAPPER.LASHeader_GetMaxY(headerHandle);
        zMax = WRAPPER.LASHeader_GetMaxZ(headerHandle);

        xOffset = WRAPPER.LASHeader_GetOffsetX(headerHandle);
        yOffset = WRAPPER.LASHeader_GetOffsetY(headerHandle);
        zOffset = WRAPPER.LASHeader_GetOffsetZ(headerHandle);

        xScale = WRAPPER.LASHeader_GetScaleX(headerHandle);
        yScale = WRAPPER.LASHeader_GetScaleY(headerHandle);
        zScale = WRAPPER.LASHeader_GetScaleZ(headerHandle);

        signature = WRAPPER.LASHeader_GetFileSignature(headerHandle);
        fileSourceId = WRAPPER.LASHeader_GetFileSourceId(headerHandle);
        WRAPPER.LASHeader_GetReserved(headerHandle);
        WRAPPER.LASHeader_GetProjectId(headerHandle);
        versionMajor = WRAPPER.LASHeader_GetVersionMajor(headerHandle);
        versionMinor = WRAPPER.LASHeader_GetVersionMinor(headerHandle);
        systemIdentifier = WRAPPER.LASHeader_GetSystemId(headerHandle);
        generatingSoftware = WRAPPER.LASHeader_GetSoftwareId(headerHandle);
        dayOfYear = WRAPPER.LASHeader_GetCreationDOY(headerHandle);
        year = WRAPPER.LASHeader_GetCreationYear(headerHandle);
        headerSize = WRAPPER.LASHeader_GetHeaderSize(headerHandle);
        offset = WRAPPER.LASHeader_GetDataOffset(headerHandle);

        // WRAPPER.LASHeader_GetRecordsCount(headerHandle);
        pointDataFormat = WRAPPER.LASHeader_GetDataFormatId(headerHandle);
        // WRAPPER.LASHeader_GetPointRecordsByReturnCount(headerHandle, int returnNum );
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

    public long getOffset() {
        return offset;
    }

    /**
     * Sets the gps time type.
     * 
     * <p>
     * <ul>
     * <li>0 (not set) = GPS time in the point record fields is GPS Week Time</li>
     * <li>1 (set) = GPS time is standard GPS time (satellite gps time) minus 1E9 (Adjusted standard GPS time)</li>
     * </ul>
     * 
     * @param gpsTimeType the type to set.
     */
    public void setGpsTimeType( int gpsTimeType ) {
        this.gpsTimeType = gpsTimeType;
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
        sb.append("Number of point records: ").append(pointRecordsCount).append("\n");
        sb.append("Record length: ").append(recordLength).append("\n");
        sb.append("Scale: [").append(xScale).append(", ").append(yScale).append(", ").append(zScale).append("]\n");
        sb.append("Offset: [").append(xOffset).append(", ").append(yOffset).append(", ").append(zOffset).append("]\n");
        sb.append("X Range: [").append(xMin).append(", ").append(xMax).append("]\n");
        sb.append("Y Range: [").append(yMin).append(", ").append(yMax).append("]\n");
        sb.append("Z Range: [").append(zMin).append(", ").append(zMax).append("]\n");
        sb.append("Has gps time info: ").append(hasGpsTime()).append("\n");
        sb.append("Has color info: ").append(hasRGB()).append("\n");
        // sb.append("Gps time type: ").append(gpsTimeType).append("\n");
        return sb.toString();
    }

    @Override
    public String getVersion() {
        return versionMajor + "." + versionMinor;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    @Override
    public ReferencedEnvelope3D getDataEnvelope() {
        if (dataEnvelope == null) {
            dataEnvelope = new ReferencedEnvelope3D(xMin, xMax, yMin, yMax, zMin, zMax, getCrs());
        }
        return dataEnvelope;
    }

    @Override
    public long getRecordsCount() {
        return pointRecordsCount;
    }

    public boolean hasGpsTime() {
        return pointDataFormat == 1 || pointDataFormat == 3;
    }

    public boolean hasRGB() {
        return pointDataFormat == 2 || pointDataFormat == 3;
    }

    @Override
    public byte getPointDataFormat() {
        return pointDataFormat;
    }

    @Override
    public int getGpsTimeType() {
        return gpsTimeType;
    }
    
	@Override
	public double[] getRawDataEnvelope() {
		return new double[]{xMin, yMin, zMin, xMax, yMax, zMax};
	}
}
