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
package org.hortonmachine.gears.modules;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.core.liblas.LiblasHeader;
import org.hortonmachine.gears.io.las.core.liblas.LiblasJNALibrary;
import org.hortonmachine.gears.io.las.core.liblas.LiblasReader;
import org.hortonmachine.gears.io.las.core.liblas.LiblasWrapper;
import org.hortonmachine.gears.io.las.core.liblas.LiblasWriter;
import org.hortonmachine.gears.io.las.core.v_1_0.LasReaderBuffered;
import org.hortonmachine.gears.io.las.core.v_1_0.LasReaderEachPoint;
import org.hortonmachine.gears.io.las.core.v_1_0.LasWriterBuffered;
import org.hortonmachine.gears.io.las.core.v_1_0.LasWriterEachPoint;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.utils.HMTestCase;
@SuppressWarnings("nls")
public class TestLasIO extends HMTestCase {

    private static boolean doNative = false;
    private static boolean tellNative = true;
    private static String lasWriteFileName = "las/1.1_1.las";

    protected void setUp() throws Exception {
        // local native libs for test
        File libFolder = new File("/usr/local/lib/");
        if (libFolder.exists()) {
            String error = LiblasWrapper.loadNativeLibrary(libFolder.getAbsolutePath(), "las_c");
            if (error == null) {
                doNative = true;
            }
        } else {
            LiblasJNALibrary wrapper = LiblasWrapper.getWrapper();
            if (wrapper != null) {
                doNative = true;
            }
        }

        if (doNative && tellNative) {
            System.out.println("Doing tests with native support.");
            tellNative = false;
        }
    }

    public void testLasReader() throws Exception {

        String name = "las/1.0_0.las";
        long expectedCount = 1;
        processFile(name, expectedCount, false);
        name = "las/1.0_1.las";
        expectedCount = 1;
        processFile(name, expectedCount, false);
        name = "las/1.1_0.las";
        expectedCount = 1;
        processFile(name, expectedCount, false);
        name = "las/1.1_1.las";
        expectedCount = 1;
        processFile(name, expectedCount, false);
        name = "las/1.2_0.las";
        expectedCount = 1;
        processFile(name, expectedCount, false);
        name = "las/1.2_1.las";
        expectedCount = 1;
        processFile(name, expectedCount, false);
        name = "las/1.2_2.las";
        expectedCount = 1;
        processFile(name, expectedCount, true);
        name = "las/1.2_3.las";
        expectedCount = 1;
        processFile(name, expectedCount, true);
        name = "las/1.2-with-color.las";
        expectedCount = 1065;
        processFile(name, expectedCount, true);
    }

    public void testLasContentReader() throws Exception {

        String name = "las/1.0_0.las";
        checkFileContent(fileFromName(name));
        name = "las/1.0_1.las";
        checkFileContent(fileFromName(name));
        name = "las/1.1_0.las";
        checkFileContent(fileFromName(name));
        name = "las/1.1_1.las";
        checkFileContent(fileFromName(name));
        name = "las/1.2_0.las";
        checkFileContent(fileFromName(name));
        name = "las/1.2_1.las";
        checkFileContent(fileFromName(name));
        name = "las/1.2_2.las";
        checkFileContent(fileFromName(name));
        name = "las/1.2_3.las";
        checkFileContent(fileFromName(name));
        name = "las/1.2-with-color.las";
        checkFileContent(fileFromName(name));

    }

    public void testLazReader() throws Exception {
        if (doNative) {
            String name = "las/1.2-with-color.laz";
            int expectedCount = 1065;

            URL lasUrl = this.getClass().getClassLoader().getResource(name);
            File lasFile = new File(lasUrl.toURI());
            LiblasReader libLasReader = new LiblasReader(lasFile, null);
            libLasReader.open();
            LiblasHeader libLasHeader = libLasReader.getHeader();

            long recordsCount = libLasHeader.getRecordsCount();
            if (recordsCount != 0) {
                // we have laz support
                assertEquals(expectedCount, recordsCount);
            } else {
                System.out.println("No laz support");
            }
            libLasReader.close();
        }

    }

    public void testLasWriterNative() throws Exception {
        if (doNative) {
            URL lasUrl = this.getClass().getClassLoader().getResource(lasWriteFileName);
            File lasFile = new File(lasUrl.toURI());

            File liblasTmp = File.createTempFile("liblasreader", ".las");
            LiblasReader libLasReader = new LiblasReader(lasFile, null);
            libLasReader.open();
            LiblasHeader libLasHeader = libLasReader.getHeader();

            LiblasWriter liblasWriter = new LiblasWriter(liblasTmp, DefaultGeographicCRS.WGS84);
            liblasWriter.setBounds(libLasHeader);
            liblasWriter.open();
            while( libLasReader.hasNextPoint() ) {
                liblasWriter.addPoint(libLasReader.getNextPoint());
            }
            liblasWriter.close();

            LiblasReader tmpLiblasReader = new LiblasReader(liblasTmp, null);
            tmpLiblasReader.open();
            ILasHeader tmpLiblasHeader = tmpLiblasReader.getHeader();
            checkHeader(libLasHeader, tmpLiblasHeader);
            LasRecord tmpLiblasDot = tmpLiblasReader.getPointAt(0);
            LasRecord liblasDot = libLasReader.getPointAt(0);
            assertTrue(LasUtils.lasRecordEqual(tmpLiblasDot, liblasDot));
            tmpLiblasReader.close();
            libLasReader.close();

            liblasTmp.deleteOnExit();
        }
    }

    public void testLasWriter() throws Exception {
        URL lasUrl = this.getClass().getClassLoader().getResource(lasWriteFileName);
        File lasFile = new File(lasUrl.toURI());

        LasReaderBuffered lasReader = new LasReaderBuffered(lasFile, null);
        lasReader.open();
        ILasHeader lasHeader = lasReader.getHeader();

        /*
         * write tmp files
         */
        File lasTmp = File.createTempFile("lasreader", ".las");

        ALasWriter lasWriter = ALasWriter.getWriter(lasTmp, DefaultGeographicCRS.WGS84);
        lasWriter.setBounds(lasHeader);
        lasWriter.open();
        while( lasReader.hasNextPoint() ) {
            lasWriter.addPoint(lasReader.getNextPoint());
        }
        lasWriter.close();

        LasReaderBuffered tmpLasReader = new LasReaderBuffered(lasTmp, null);
        tmpLasReader.open();
        ILasHeader tmpLasHeader = tmpLasReader.getHeader();
        checkHeader(lasHeader, tmpLasHeader);
        LasRecord tmpLasDot = tmpLasReader.getPointAt(0);
        LasRecord lasDot = lasReader.getPointAt(0);
        assertTrue(LasUtils.lasRecordEqual(tmpLasDot, lasDot));
        tmpLasReader.close();
        lasReader.close();

        lasTmp.deleteOnExit();
    }

    private void processFile( String name, long expectedCount, boolean hasColor )
            throws URISyntaxException, Exception, IOException {
        URL lasUrl = this.getClass().getClassLoader().getResource(name);
        File lasFile = new File(lasUrl.toURI());
        LiblasReader libLasReader = null;
        ILasHeader libLasHeader = null;
        if (doNative) {
            libLasReader = new LiblasReader(lasFile, null);
            libLasReader.open();
            libLasHeader = libLasReader.getHeader();
            assertEquals(hasColor, libLasHeader.hasRGB());
        }

        LasReaderBuffered lasReader = new LasReaderBuffered(lasFile, null);
        lasReader.open();
        ILasHeader lasHeader = lasReader.getHeader();
        assertEquals(hasColor, lasHeader.hasRGB());

        assertEquals(expectedCount, lasHeader.getRecordsCount());

        if (doNative) {
            assertTrue(libLasHeader.getRecordsCount() == lasHeader.getRecordsCount());
        }

        long count = 0;
        while( lasReader.hasNextPoint() ) {
            LasRecord lasDot = lasReader.getNextPoint();
            if (doNative) {
                libLasReader.hasNextPoint();
                LasRecord liblasDot = libLasReader.getNextPoint();
                assertTrue(LasUtils.lasRecordEqual(lasDot, liblasDot));
            }

            count++;
            if (count == 3) {
                break;
            }
        }

        if (lasHeader.getRecordsCount() > 1) {
            if (doNative) {
                libLasReader.seek(0);
            }
            lasReader.seek(0);

            LasRecord lasDot = lasReader.getNextPoint();
            if (doNative) {
                LasRecord liblasDot = libLasReader.getNextPoint();
                assertTrue(LasUtils.lasRecordEqual(lasDot, liblasDot));
            }

            lasDot = lasReader.getPointAt(1);
            if (doNative) {
                LasRecord liblasDot = libLasReader.getPointAt(1);
                assertTrue(LasUtils.lasRecordEqual(lasDot, liblasDot));
            }

        }

        lasReader.close();
        if (doNative) {
            libLasReader.close();
        }

    }

    private void checkFileContent( File lasFile ) throws URISyntaxException, Exception, IOException {
        try (LasReaderBuffered lasReaderBuffered = new LasReaderBuffered(lasFile, null);
                LasReaderEachPoint lasReaderEachPoint = new LasReaderEachPoint(lasFile, null);) {
            lasReaderBuffered.open();
            lasReaderEachPoint.open();

            long count = 0;
            while( lasReaderEachPoint.hasNextPoint() ) {
                assertTrue(lasReaderBuffered.hasNextPoint());

                LasRecord lasBuf = lasReaderBuffered.getNextPoint();
                LasRecord lasEach = lasReaderEachPoint.getNextPoint();
                boolean areEqual = LasUtils.lasRecordEqual(lasBuf, lasEach);
                if (!areEqual) {
                    System.err.println(count);
                }
                assertTrue(areEqual);
                count++;
            }

            // System.out.println("Read points: " + count);
        }

    }

    private File fileFromName( String name ) throws URISyntaxException {
        URL lasUrl = this.getClass().getClassLoader().getResource(name);
        File lasFile = new File(lasUrl.toURI());
        return lasFile;
    }

    private void checkHeader( ILasHeader header, ILasHeader tmpHeader ) {
        assertEquals(header.getOffset(), tmpHeader.getOffset());
        // assertEquals(header.getRecordLength(), tmpHeader.getRecordLength());
        assertEquals(header.getRecordsCount(), tmpHeader.getRecordsCount());
        ReferencedEnvelope3D lasEnv = header.getDataEnvelope();
        ReferencedEnvelope3D tmpLasEnv = tmpHeader.getDataEnvelope();
        assertEquals(lasEnv.getMinX(), tmpLasEnv.getMinX(), DELTA);
        assertEquals(lasEnv.getMinY(), tmpLasEnv.getMinY(), DELTA);
        assertEquals(lasEnv.getMinZ(), tmpLasEnv.getMinZ(), DELTA);
        assertEquals(lasEnv.getMaxX(), tmpLasEnv.getMaxX(), DELTA);
        assertEquals(lasEnv.getMaxY(), tmpLasEnv.getMaxY(), DELTA);
        assertEquals(lasEnv.getMaxZ(), tmpLasEnv.getMaxZ(), DELTA);
    }

}
