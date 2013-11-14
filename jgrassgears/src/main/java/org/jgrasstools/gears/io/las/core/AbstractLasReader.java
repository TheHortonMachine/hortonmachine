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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.v_1_0.LasReader_1_0;
import org.jgrasstools.gears.utils.ByteUtilities;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Class to read las data records in different versions.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class AbstractLasReader {

    private final byte[] doubleDataArray = new byte[8];
    private final ByteBuffer doubleBb = ByteBuffer.wrap(doubleDataArray);
    private final byte[] longDataArray = new byte[4];
    private final ByteBuffer longBb = ByteBuffer.wrap(longDataArray);
    private final byte[] shortDataArray = new byte[2];
    private final ByteBuffer shortBb = ByteBuffer.wrap(shortDataArray);
    private final byte[] singleDataArray = new byte[1];
    private final ByteBuffer singleBb = ByteBuffer.wrap(singleDataArray);

    public static String dateTimeFormatterYYYYMMDD_string = "yyyy-MM-dd";
    public static DateTimeFormatter dateTimeFormatterYYYYMMDD = DateTimeFormat.forPattern(dateTimeFormatterYYYYMMDD_string);

    protected double xScale;
    protected double yScale;
    protected double zScale;
    protected double xOffset;
    protected double yOffset;
    protected double zOffset;
    protected final File lasFile;
    protected FileChannel fc;
    protected FileInputStream fis;
    protected long offset;
    protected long records;
    protected short recordLength;
    protected String header;
    protected boolean isOpen;
    protected CoordinateReferenceSystem crs;

    public AbstractLasReader( File lasFile, CoordinateReferenceSystem crs ) {
        this.lasFile = lasFile;
        this.crs = crs;
        doubleBb.order(ByteOrder.LITTLE_ENDIAN);
        longBb.order(ByteOrder.LITTLE_ENDIAN);
        shortBb.order(ByteOrder.LITTLE_ENDIAN);
    }

    public File getLasFile() {
        return lasFile;
    }

    /**
     * Get a Las reader from the file based on the file version.
     * 
     * @param lasFile the file to get the reader for.
     * @return the reader or <code>null</code> if no reader can be found.
     * @throws Exception
     */
    public static AbstractLasReader getReader( File lasFile ) throws Exception {
        CoordinateReferenceSystem readCrs = CrsUtilities.readProjectionFile(lasFile.getAbsolutePath(), "las");
        return getReader(lasFile, readCrs);
    }

    /**
     * Get a Las reader from the file based on the file version.
     * 
     * @param lasFile the file to get the reader for.
     * @param crs the {@link CoordinateReferenceSystem} for the file.
     * @return the reader or <code>null</code> if no reader can be found.
     * @throws IOException
     */
    public static AbstractLasReader getReader( File lasFile, CoordinateReferenceSystem crs ) throws IOException {
        String version = getLasFileVersion(lasFile);
        AbstractLasReader lasRecordReader = null;
        if (version.equals("1.0")) {
            lasRecordReader = new LasReader_1_0(lasFile, crs);
        } else {
            System.err.println("Found unsopported las file version. Trying to use the current 1.0 las reader anyways.");
            lasRecordReader = new LasReader_1_0(lasFile, crs);
        }
        return lasRecordReader;
    }

    /**
     * Read just the version bytes from a las file.
     * 
     * <p>This can be handy is one needs to choose version reader.
     * 
     * @param lasFile the las file to check.
     * @return the version string as "major.minor" .
     * @throws IOException
     */
    public static String getLasFileVersion( File lasFile ) throws IOException {
        FileInputStream fis = null;
        FileChannel fc = null;
        try {
            fis = new FileInputStream(lasFile);
            fc = fis.getChannel();
            // Version Major
            fis.skip(24);
            int versionMajor = fis.read();
            // Version Minor
            int versionMinor = fis.read();
            String version = versionMajor + "." + versionMinor; //$NON-NLS-1$
            return version;
        } finally {
            fc.close();
            fis.close();
        }
    }

    protected void checkOpen() {
        if (!isOpen) {
            try {
                open();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void open() throws Exception {
        fis = new FileInputStream(lasFile);
        fc = fis.getChannel();

        parseHeader();
        isOpen = true;
    }

    public void close() throws Exception {
        if (fc != null && fc.isOpen())
            fc.close();
        if (fis != null)
            fis.close();
        isOpen = false;
    }

    public abstract long getRecordsCount();

    protected abstract void parseHeader() throws Exception;

    public abstract ILasHeader getHeader();

    public ReferencedEnvelope3D getEnvelope() {
        checkOpen();
        return getHeader().getDataEnvelope();
    }

    /**
     * Check if there are still data available.
     * 
     * @return <code>true</code> if there are still data to read.
     */
    public abstract boolean hasNextLasDot();

    /**
     * Read the next record into a {@link LasRecord} object.
     * 
     * @return the read object or <code>null</code> if none available.
     * @throws IOException 
     */
    public abstract LasRecord readNextLasDot() throws IOException;

    /**
     * Skip a given amount of records.
     * 
     * <p>This just moves the reader to the right position after the records.
     * 
     * @param recordsToSkip the number of records to skip.
     * @throws IOException
     */
    public void skipRecords( long recordsToSkip ) throws IOException {
        long bytesToSkip = recordLength * recordsToSkip;
        fc.position(fc.position() + bytesToSkip);
    }

    protected String getString( int size ) throws IOException {
        byte[] bytesStr = new byte[size];
        ByteBuffer singleBb = ByteBuffer.wrap(bytesStr);
        fc.read(singleBb);
        String signature = new String(bytesStr);
        return signature;
    }

    protected long getLong4Bytes() throws IOException {
        longBb.clear();
        fc.read(longBb);
        long arr2long = ByteUtilities.byteArrayToLongLE(longDataArray);
        return arr2long;
    }

    protected double getDouble8Bytes() throws IOException {
        doubleBb.clear();
        fc.read(doubleBb);
        double arr2Double = doubleBb.getDouble(0);
        return arr2Double;
    }

    protected short getShort2Bytes() throws IOException {
        shortBb.clear();
        fc.read(shortBb);
        short arr2short = shortBb.getShort(0);
        return arr2short;
    }

    protected byte get() throws IOException {
        singleBb.clear();
        fc.read(singleBb);
        return singleBb.get(0);
    }

    protected void skip( int bytesTpSkip ) throws IOException {
        fc.position(fc.position() + bytesTpSkip);
    }

    protected int getReturnNumber( byte b ) {
        int rn = 0;
        for( int i = 0; i < 3; i++ ) {
            if (isSet(b, i)) {
                rn = (int) (rn + Math.pow(2.0, i));
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
    protected int getGpsTimeType( byte b ) {
        return isSet(b, 0) ? 1 : 0;
    }

    protected int getNumberOfReturns( byte b ) {
        int nor = 0;
        for( int i = 3; i < 6; i++ ) {
            if (isSet(b, i)) {
                nor = (int) (nor + Math.pow(2.0, i - 3));
            }
        }
        return nor;
    }

    private boolean isSet( byte b, int n ) { // true if bit n is set in byte b
        return (b & (1 << n)) != 0;
    }
}
