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
package org.jgrasstools.gears.modules;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.liblas.LiblasReader;
import org.jgrasstools.gears.io.las.core.v_1_0.LasReader;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.utils.HMTestCase;
@SuppressWarnings("nls")
public class TestLasIO extends HMTestCase {
    public void testLasReader() throws Exception {

        // local native libs for test
        File libFolder = new File("/home/moovida/development/liblas-git/makefiles/bin/Release/");
        if (libFolder.exists()) {
            LiblasReader.loadNativeLibrary(libFolder.getAbsolutePath(), "las_c");
        }

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

    private void processFile( String name, long expectedCount, boolean hasColor ) throws URISyntaxException, Exception,
            IOException {
        URL lasUrl = this.getClass().getClassLoader().getResource(name);
        File lasFile = new File(lasUrl.toURI());
        boolean doNative = LiblasReader.loadNativeLibrary(null, null);
        LiblasReader libLasReader = null;
        ILasHeader libLasHeader = null;
        if (doNative) {
            libLasReader = new LiblasReader(lasFile, null);
            libLasReader.open();
            libLasHeader = libLasReader.getHeader();
            assertEquals(hasColor, libLasHeader.hasRGB());
        }

        LasReader lasReader = new LasReader(lasFile, null);
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
}
