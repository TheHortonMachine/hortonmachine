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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;

/**
 * Las reader based on laszip4j. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LaszipReader extends ALasReader {

    private File lasFile;
    private CoordinateReferenceSystem crs;
    private LaszipHeader header;
    private LASReader reader;
    private Iterator<LASPoint> pointsIterator;
    private double[] xyzOffset;
    private double[] xyzScale;
    private boolean isOpen;

    public LaszipReader( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        this.lasFile = lasFile;
        this.crs = crs;
    }

    @Override
    public File getLasFile() {
        return lasFile;
    }

    @Override
    public void open() throws Exception {
        reader = new LASReader(lasFile);
        LASHeader laszipHeader = reader.getHeader();
        header = new LaszipHeader(laszipHeader, crs);
        xyzOffset = header.getXYZOffset();
        xyzScale = header.getXYZScale();
        isOpen = true;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void setOverrideGpsTimeType( int type ) {
    }

    @Override
    public boolean hasNextPoint() throws IOException {
        if (pointsIterator == null) {
            Iterable<LASPoint> pointsIterable = reader.getPoints();
            pointsIterator = pointsIterable.iterator();
        }
        return pointsIterator.hasNext();
    }

    @Override
    public LasRecord getNextPoint() throws IOException {
        LASPoint next = pointsIterator.next();
        LasRecord lr = new LasRecord();
        lr.x = next.getX() * xyzScale[0] + xyzOffset[0];
        lr.y = next.getY() * xyzScale[1] + xyzOffset[1];
        lr.z = next.getZ() * xyzScale[2] + xyzOffset[2];
        lr.intensity = (short) next.getIntensity();
        lr.returnNumber = next.getReturnNumber();
        lr.numberOfReturns = next.getNumberOfReturns();
        lr.classification = next.getClassification();
        lr.gpsTime = next.getGPSTime();
        lr.color = new short[]{(short) next.getRed(), (short) next.getGreen(), (short) next.getBlue()};

        return lr;
    }

    public LasRecord getPointAtAddress( long address ) throws IOException {
        throw new RuntimeException("Not supported in laszip reader");
    }

    @Override
    public LasRecord getPointAt( long pointNumber ) throws IOException {
        throw new RuntimeException("Not supported in laszip reader");
    }

    @Override
    public double[] readNextLasXYZAddress() throws IOException {
        throw new RuntimeException("Not supported in laszip reader");
    }

    public void seek( long pointNumber ) throws IOException {
        throw new RuntimeException("Not supported in laszip reader");
    }

    @Override
    public ILasHeader getHeader() {
        checkOpen();
        return header;
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
    public void rewind() throws IOException {
        pointsIterator = null;
    }

}
