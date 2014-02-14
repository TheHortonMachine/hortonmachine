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

import java.io.File;
import java.io.IOException;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.JGTVersion;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A las writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LiblasWriter extends ALasWriter {
    private static final String OPEN_METHOD_MSG = "This needs to be called before the open method.";

    private File outFile;
    private CoordinateReferenceSystem crs;
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
    private int offsetToData = 227;
    private boolean doWriteGroundElevation;
    private boolean openCalled;

    private LiblasJNALibrary WRAPPER;

    private long headerHandle;

    private int pointFormat = 0;

    private long fileHandle;

    /**
     * A las file writer.
     * 
     * @param outFile the output file.
     * @param crs the {@link CoordinateReferenceSystem crs}. If <code>null</code>, no prj file is written.
     * @throws Exception 
     */
    public LiblasWriter( File outFile, CoordinateReferenceSystem crs ) throws Exception {
        this.outFile = outFile;
        this.crs = crs;
        if (crs != null) {
            String nameWithoutExtention = FileUtilities.getNameWithoutExtention(outFile);
            prjFile = new File(outFile.getParent(), nameWithoutExtention + ".prj");
        }

        WRAPPER = LiblasWrapper.getWrapper();
    }

    @Override
    public void setScales( double xScale, double yScale, double zScale ) {
        if (openCalled) {
            throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, crs);
        }
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    @Override
    public void setOffset( double xOffset, double yOffset, double zOffset ) {
        if (openCalled) {
            throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, crs);
        }
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    // public void setPointFormat( int pointFormat ) {
    // this.pointFormat = pointFormat;
    // if (openCalled) {
    // throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, crs);
    // }
    // }

    @Override
    public void setBounds( double xMin, double xMax, double yMin, double yMax, double zMin, double zMax ) {
        if (openCalled) {
            throw new ModelsIllegalargumentException(OPEN_METHOD_MSG, crs);
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

        recordLength = header.getRecordLength();
        offsetToData = (int) header.getOffset();
    }

    @Override
    public void open() throws Exception {
        writeHeader();
        fileHandle = WRAPPER.LASWriter_Create(outFile.getAbsolutePath(), headerHandle, (byte) 1);
        openCalled = true;
    }

    private void writeHeader() throws IOException {
        headerHandle = WRAPPER.LASHeader_Create();

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
        // TODO handle global enchoding and proper gps time

        // byte[] signature = "LASF".getBytes();

        // // major
        // fos.write(1);
        // // minor
        // fos.write(0);
        // hLength = hLength + 2;

        // WRAPPER.LASHeader_SetSystemId(headerHandle, systemIdentifier);

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
        WRAPPER.LASHeader_SetSoftwareId(headerHandle, jgtVersion);

        // fos.write(flightDateJulian);

        // WRAPPER.LASHeader_SetSoftwareId(headerHandle, jgtVersion);
        // short headersize = 226;

        WRAPPER.LASHeader_SetDataOffset(headerHandle, offsetToData);

        // WRAPPER.LASHeader_SetDataRecordLength(headerHandle, recordLength);

        WRAPPER.LASHeader_SetOffset(headerHandle, xOffset, yOffset, zOffset);
        WRAPPER.LASHeader_SetScale(headerHandle, xScale, yScale, zScale);
        WRAPPER.LASHeader_SetMin(headerHandle, xMin, yMin, zMin);
        WRAPPER.LASHeader_SetMax(headerHandle, xMax, yMax, zMax);

    }

    @Override
    public synchronized void addPoint( LasRecord record ) throws IOException {
        long pointHandle = WRAPPER.LASPoint_Create(fileHandle);
        WRAPPER.LASPoint_SetHeader(pointHandle, headerHandle);
        WRAPPER.LASPoint_SetX(pointHandle, record.x); // 4
        WRAPPER.LASPoint_SetY(pointHandle, record.y); // 4
        if (!doWriteGroundElevation) {
            WRAPPER.LASPoint_SetZ(pointHandle, record.z); // 4
        } else {
            WRAPPER.LASPoint_SetZ(pointHandle, record.groundElevation); // 4
        }

        WRAPPER.LASPoint_SetIntensity(pointHandle, record.intensity); // 2
        WRAPPER.LASPoint_SetNumberOfReturns(pointHandle, record.numberOfReturns); // 2
        WRAPPER.LASPoint_SetReturnNumber(pointHandle, record.returnNumber); // 2
        WRAPPER.LASPoint_SetClassification(pointHandle, record.classification); // 1

        if (record.gpsTime > 0) {
            WRAPPER.LASPoint_SetTime(pointHandle, record.gpsTime);
            pointFormat = 1;
        }

        // if (record.color != null) {
        // pointFormat = 3;
        //
        // }

        WRAPPER.LASWriter_WritePoint(fileHandle, pointHandle);
        WRAPPER.LASPoint_Destroy(pointHandle);

        recordsNum++;
    }

    @Override
    public void close() throws Exception {

        WRAPPER.LASHeader_SetPointRecordsCount(headerHandle, recordsNum);
        WRAPPER.LASHeader_SetDataFormatId(headerHandle, (byte) pointFormat);

        WRAPPER.LASWriter_Destroy(fileHandle);

        /*
         * write crs file
         */
        if (crs != null)
            CrsUtilities.writeProjectionFile(prjFile.getAbsolutePath(), null, crs);
    }

    @Override
    public void setWriteGroundElevation( boolean doWriteGroundElevation ) {
        this.doWriteGroundElevation = doWriteGroundElevation;
    }

}
