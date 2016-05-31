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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A laslib based native las reader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LiblasReader extends ALasReader {

    private File lasFile;
    private long fileHandle;
    private long currentPointRef;
    private LiblasHeader headerHandle = null;
    private CoordinateReferenceSystem crs;
    private byte pointDataFormat;
    private long offset;
    private double xOffset = 0.0;
    private double yOffset = 0.0;
    private double zOffset = 0.0;
    private short recordLength;
    private LiblasJNALibrary WRAPPER;

    public LiblasReader( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        this.lasFile = lasFile;
        if (crs != null) {
            this.crs = crs;
        } else {
            try {
                this.crs = CrsUtilities.readProjectionFile(lasFile.getAbsolutePath(), "las");
            } catch (Exception e) {
            	try {
            		this.crs = CrsUtilities.readProjectionFile(lasFile.getAbsolutePath(), "laz");
            	} catch (Exception e1) {} // ignore crs errors
            }
        }
        WRAPPER = LiblasWrapper.getWrapper();
    }

    public void open() throws FileNotFoundException {
        if (!lasFile.exists()) {
            throw new FileNotFoundException(lasFile.getAbsolutePath());
        }
        fileHandle = WRAPPER.LASReader_Create(lasFile.getAbsolutePath());
        LiblasHeader header = getHeader();
        pointDataFormat = header.getPointDataFormat();
        offset = header.getOffset();
        recordLength = header.getRecordLength();
    }

    public LiblasHeader getHeader() {
        if (headerHandle == null) {
            headerHandle = new LiblasHeader(WRAPPER, WRAPPER.LASReader_GetHeader(fileHandle), crs);
        }
        return headerHandle;
    }

    public boolean hasNextPoint() {
        currentPointRef = WRAPPER.LASReader_GetNextPoint(fileHandle);
        return currentPointRef != 0;
    }

    public LasRecord getNextPoint() {
        return getPointAtRef(currentPointRef);
    }

    private LasRecord getPointAtRef( long ref ) {
        double x = WRAPPER.LASPoint_GetX(ref);
        double y = WRAPPER.LASPoint_GetY(ref);
        double z = WRAPPER.LASPoint_GetZ(ref);

        LasRecord dot = new LasRecord();
        dot.x = x;
        dot.y = y;
        dot.z = z;

        short intensity = WRAPPER.LASPoint_GetIntensity(ref);
        short returnNumber = WRAPPER.LASPoint_GetReturnNumber(ref);
        short numberOfReturns = WRAPPER.LASPoint_GetNumberOfReturns(ref);
        byte classification = WRAPPER.LASPoint_GetClassification(ref);

        dot.intensity = intensity;
        dot.classification = classification;
        dot.returnNumber = returnNumber;
        dot.numberOfReturns = numberOfReturns;
        if (pointDataFormat == 1) {
            dot.gpsTime = WRAPPER.LASPoint_GetTime(ref);
        } else if (pointDataFormat == 2) {
            long colorHandle = WRAPPER.LASPoint_GetColor(ref);
            dot.color[0] = WRAPPER.LASColor_GetRed(colorHandle);
            dot.color[1] = WRAPPER.LASColor_GetGreen(colorHandle);
            dot.color[2] = WRAPPER.LASColor_GetBlue(colorHandle);
        } else if (pointDataFormat == 3) {
            dot.gpsTime = WRAPPER.LASPoint_GetTime(ref);
            long colorHandle = WRAPPER.LASPoint_GetColor(ref);
            dot.color[0] = WRAPPER.LASColor_GetRed(colorHandle);
            dot.color[1] = WRAPPER.LASColor_GetGreen(colorHandle);
            dot.color[2] = WRAPPER.LASColor_GetBlue(colorHandle);
        }

        return dot;
    }

    public LasRecord getPointAt( long position ) {
        currentPointRef = WRAPPER.LASReader_GetPointAt(fileHandle, position);
        return getPointAtRef(currentPointRef);
    }

    public LasRecord getPointAtAddress( long address ) {
        long pointNum = (address - offset) / recordLength;
        currentPointRef = WRAPPER.LASReader_GetPointAt(fileHandle, pointNum);
        return getPointAtRef(currentPointRef);
    }

    public void close() {
        WRAPPER.LASReader_Destroy(fileHandle);
    }

    @Override
    public File getLasFile() {
        return lasFile;
    }

    @Override
    public double[] readNextLasXYZAddress() throws IOException {
        throw new RuntimeException("not implemented yet: readNextLasXYZAddress");
    }

    @Override
    public void seek( long pointNumber ) throws IOException {
        WRAPPER.LASReader_Seek(fileHandle, pointNumber);
        currentPointRef = WRAPPER.LASReader_GetNextPoint(fileHandle);
    }

    @Override
    public void setOverrideGpsTimeType( int type ) {

    }

}
