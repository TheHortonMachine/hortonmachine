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
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.core.laszip4j.LaszipReader;
import org.hortonmachine.gears.io.las.core.v_1_0.LasReaderBuffered;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.utils.HMTestCase;
public class TestLasIO extends HMTestCase {

    private static String lasWriteFileName = "las/1.1_1.las";

    protected void setUp() throws Exception {
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
//        String name = "/media/hydrologis/Samsung_T3/UNIBZ/monticolo2019/Coverage_SolarTirol_05.laz";
//        File lasFile = new File(name);
        String name = "las/1.2-with-color.laz";
        URL lasUrl = this.getClass().getClassLoader().getResource(name);
        File lasFile = new File(lasUrl.toURI());
        int expectedCount = 1065;

        ALasReader reader = Las.getReader(lasFile, null);
        reader.open();
        ILasHeader libLasHeader = reader.getHeader();

        if (reader.hasNextPoint()) {
            LasRecord nextPoint = reader.getNextPoint();
            short r = nextPoint.color[0];
            short g = nextPoint.color[1];
            short b = nextPoint.color[2];

            assertEquals(68, r);
            assertEquals(77, g);
            assertEquals(88, b);
        }

        long recordsCount = libLasHeader.getRecordsCount();
        assertEquals(expectedCount, recordsCount);
        reader.close();
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

        LasReaderBuffered lasReader = new LasReaderBuffered(lasFile, null);
        lasReader.open();
        ILasHeader lasHeader = lasReader.getHeader();
        assertEquals(hasColor, lasHeader.hasRGB());

        assertEquals(expectedCount, lasHeader.getRecordsCount());

        long count = 0;
        while( lasReader.hasNextPoint() ) {
            lasReader.getNextPoint();
            count++;
            if (count == 3) {
                break;
            }
        }

        if (lasHeader.getRecordsCount() > 1) {
            lasReader.seek(0);
            lasReader.getPointAt(1);
        }

        lasReader.close();

    }

    private void checkFileContent( File lasFile ) throws URISyntaxException, Exception, IOException {
        try (LasReaderBuffered lasReaderBuffered = new LasReaderBuffered(lasFile, null);
                LaszipReader lasReaderEachPoint = new LaszipReader(lasFile, null);) {
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
