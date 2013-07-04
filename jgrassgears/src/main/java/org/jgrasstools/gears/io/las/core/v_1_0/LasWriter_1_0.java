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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.utils.ByteUtilities;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.JGTVersion;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A las writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasWriter_1_0 {
    private final byte[] doubleDataArray = new byte[8];
    private final ByteBuffer doubleBb = ByteBuffer.wrap(doubleDataArray);
    private final byte[] longDataArray = new byte[4];
    private final ByteBuffer longBb = ByteBuffer.wrap(longDataArray);
    private final byte[] shortDataArray = new byte[2];
    private final ByteBuffer shortBb = ByteBuffer.wrap(shortDataArray);
    private File outFile;
    private CoordinateReferenceSystem crs;
    private FileOutputStream fos;
    private File prjFile;
    private double xScale = 0.01;
    private double yScale = 0.01;
    private double zScale = 0.001;

    public LasWriter_1_0( File outFile, CoordinateReferenceSystem crs ) {
        this.outFile = outFile;
        this.crs = crs;

        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(outFile);
        prjFile = new File(outFile.getParent(), nameWithoutExtention + ".prj");

        doubleBb.order(ByteOrder.LITTLE_ENDIAN);
        longBb.order(ByteOrder.LITTLE_ENDIAN);
        shortBb.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void setScales( double xScale, double yScale, double zScale ) {
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    public void writerecords( List<LasRecord> recordsList ) throws IOException {

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

        byte[] signature = "LASF".getBytes();
        fos.write(signature);
        byte[] reserved = new byte[4];
        fos.write(reserved);
        byte[] guid1 = new byte[4];
        fos.write(guid1);
        byte[] guid2 = new byte[2];
        fos.write(guid2);
        byte[] guid3 = new byte[2];
        fos.write(guid3);
        byte[] guid4 = new byte[8];
        fos.write(guid4);
        // major
        fos.write(1);
        // minor
        fos.write(0);
        byte[] systemIdentifier = new byte[32];
        fos.write(systemIdentifier);
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
        byte[] software = jgtVersion.getBytes();
        fos.write(software);
        byte[] flightDateJulian = new byte[2];
        fos.write(flightDateJulian);
        byte[] year = new byte[2];
        fos.write(year);

        short headersize = 226;
        fos.write(getShort(headersize));

        int offsetToData = 227;
        fos.write(getLong(offsetToData));

        int numVerRecords = 0;
        fos.write(getLong(numVerRecords));

        // point data format
        fos.write(1);

        short recordLength = 20;
        fos.write(getShort(recordLength));

        int recordsNum = recordsList.size();
        fos.write(getLong(recordsNum));

        // num of points by return
        fos.write(new byte[20]);

        // xscale
        fos.write(getDouble(xScale));
        // yscale
        fos.write(getDouble(yScale));
        // zscale
        fos.write(getDouble(zScale));

        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;

        for( LasRecord record : recordsList ) {
            xMin = min(xMin, record.x);
            yMin = min(yMin, record.y);
            zMin = min(zMin, record.z);
            xMax = max(xMax, record.x);
            yMax = max(yMax, record.y);
            zMax = max(zMax, record.z);
        }

        // xoff
        fos.write(getDouble(xMin));
        // yoff
        fos.write(getDouble(yMin));
        // zoff
        fos.write(getDouble(zMin));

        fos.write(getDouble(xMax));
        fos.write(getDouble(xMin));
        fos.write(getDouble(yMax));
        fos.write(getDouble(yMin));
        fos.write(getDouble(zMax));
        fos.write(getDouble(zMin));

        for( LasRecord record : recordsList ) {
            int length = 0;
            int x = (int) ((record.x - xMin) / xScale);
            int y = (int) ((record.y - yMin) / yScale);
            int z = (int) ((record.z - zMin) / zScale);
            fos.write(getLong(x));
            fos.write(getLong(y));
            fos.write(getLong(z));
            length = length + 12;
            fos.write(getShort(record.intensity));
            length = length + 2;

            // 001 | 001 | 11 -> bits for return num, num of ret, scan dir flag, edge of flight line

            int returnNumber = record.returnNumber;
            BitSet bitsetRN = ByteUtilities.bitsetFromByte((byte) returnNumber);
            int numberOfReturns = record.numberOfReturns;
            BitSet bitsetNOR = ByteUtilities.bitsetFromByte((byte) numberOfReturns);
            BitSet b = new BitSet(7);
            b.set(0, bitsetRN.get(0));
            b.set(1, bitsetRN.get(1));
            b.set(2, bitsetRN.get(2));
            b.set(3, bitsetNOR.get(0));
            b.set(4, bitsetNOR.get(1));
            b.set(5, bitsetNOR.get(2));
            b.set(6, false);
            b.set(7, false);
            byte[] bb = ByteUtilities.bitSetToByteArray(b);
            fos.write(bb[0]);
            length = length + 1;

            // class
            byte c = (byte) record.classification;
            fos.write(c);
            length = length + 1;

            // scan angle rank
            fos.write(1);
            length = length + 1;
            fos.write(0);
            length = length + 1;
            fos.write(new byte[2]);
            length = length + 2;
        }

        /*
         * write crs file
         */
        CrsUtilities.writeProjectionFile(prjFile.getAbsolutePath(), null, crs);

    }
    private byte[] getLong( int num ) {
        longBb.clear();
        longBb.putInt(num);
        byte[] array = longBb.array();
        return array;
    }

    // private byte[] getLong( double num ) {
    // longBb.clear();
    // longBb.putDouble(num);
    // byte[] array = longBb.array();
    // return array;
    // }

    private byte[] getDouble( double num ) {
        doubleBb.clear();
        doubleBb.putDouble(num);
        byte[] array = doubleBb.array();
        return array;
    }

    private byte[] getShort( short num ) {
        shortBb.clear();
        shortBb.putShort(num);
        return shortBb.array();
    }

    public void open() throws Exception {
        fos = new FileOutputStream(outFile);
    }

    public void close() throws Exception {
        if (fos != null)
            fos.close();
    }

    public static void main( String[] args ) throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:32632");
        // File file = new
        // File("/home/moovida/data/serviziogeologico_tn/ServizioGeologico/datigrezzi28100/Trento000091.las");
        File file = new File("/home/moovida/data-mega/las/uni_bz_49.las");
        LasReader_1_0 reader = new LasReader_1_0(file, crs);
        System.out.println(reader.getHeader());

        file = new File("/home/moovida/test.las");
        LasWriter_1_0 w = new LasWriter_1_0(file, crs);
        w.open();
        ArrayList<LasRecord> recordsList = new ArrayList<LasRecord>();
        int count = 0;
        while( reader.hasNextLasDot() ) {
            LasRecord readNextLasDot = reader.readNextLasDot();
            recordsList.add(readNextLasDot);
            System.out.println(readNextLasDot);
            if (++count > 5) {
                break;
            }
        }

        w.writerecords(recordsList);
        w.close();

        reader.close();
    }

}
