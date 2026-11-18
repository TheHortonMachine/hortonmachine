/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.utils.time;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.time.DatedFilesScanner.DatedFile;
import org.hortonmachine.gears.utils.time.DatedFilesScanner.ScanResult;

/**
 * Test {@link DatedFilesScanner}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestDatedFilesScanner extends HMTestCase {

    private File root;

    @Override
    protected void setUp() throws Exception {
        root = Files.createTempDirectory("hm-dated-files").toFile();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(root);
    }

    private File touch( String relativePath ) throws Exception {
        File file = new File(root, relativePath);
        file.getParentFile().mkdirs();
        assertTrue(file.createNewFile());
        return file;
    }

    public void testMonthlyNamesWithSidecars() throws Exception {
        for( int month = 1; month <= 12; month++ ) {
            String name = String.format("prism_ppt_us_25m_2025%02d", month);
            touch(name + ".tif");
            touch(name + ".tif.aux.xml");
            touch(name + ".prj");
            touch(name + ".xml");
        }
        ScanResult result = DatedFilesScanner.scan(root, false, DatedFilesScanner.RASTER_EXTENSIONS, null);
        assertEquals("yyyyMM", result.pattern());
        assertEquals(12, result.files().size());
        assertTrue(result.undated().isEmpty());
        assertTrue(result.duplicates().isEmpty());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), result.files().get(0).dateTime());
        assertEquals(LocalDateTime.of(2025, 12, 1, 0, 0), result.files().get(11).dateTime());
        // the "25" of "25m" is not a year
        assertFalse(result.hasTime());
    }

    public void testDailyNamesInSubfolderWithTmpFolder() throws Exception {
        touch("2_0/20180107.tif");
        touch("2_0/20180105.tif");
        touch("2_0/20230615.tif");
        File kept = touch("2_0/20180330.tif");
        // a leftover download of the same day, deeper
        File deeper = touch("2_0/20180330_download_tmp/20180330.tif");
        touch("2_0/readme.tif");

        ScanResult flat = DatedFilesScanner.scan(root, false, DatedFilesScanner.RASTER_EXTENSIONS, null);
        assertTrue(flat.files().isEmpty());
        assertNull(flat.pattern());

        ScanResult result = DatedFilesScanner.scan(root, true, DatedFilesScanner.RASTER_EXTENSIONS, null);
        assertEquals("yyyyMMdd", result.pattern());
        List<DatedFile> files = result.files();
        assertEquals(4, files.size());
        assertEquals(LocalDateTime.of(2018, 1, 5, 0, 0), files.get(0).dateTime());
        assertEquals(LocalDateTime.of(2023, 6, 15, 0, 0), files.get(3).dateTime());
        assertEquals(kept, files.get(2).file());
        assertEquals(1, result.duplicates().size());
        assertEquals(deeper, result.duplicates().get(0).file());
        assertEquals(1, result.undated().size());
        assertEquals("readme.tif", result.undated().get(0).getName());
    }

    public void testDashedDatesPreferTheDay() throws Exception {
        touch("t2m_2020-03-01.tif");
        touch("t2m_2020-03-02.tif");
        ScanResult result = DatedFilesScanner.scan(root, false, DatedFilesScanner.RASTER_EXTENSIONS, null);
        // yyyy-MM and yyyy also match, with fewer dates
        assertEquals("yyyy-MM-dd", result.pattern());
        assertEquals(2, result.files().size());
    }

    public void testDayOfYearNeedsAPattern() throws Exception {
        touch("ndvi_2018_045.tif");
        touch("ndvi_2018_046.tif");
        touch("ndvi_2018_047.tif");
        ScanResult auto = DatedFilesScanner.scan(root, false, DatedFilesScanner.RASTER_EXTENSIONS, null);
        // only the year is found: one date for three files
        assertEquals("yyyy", auto.pattern());
        assertEquals(1, auto.files().size());
        assertEquals(2, auto.duplicates().size());

        ScanResult result = DatedFilesScanner.scan(root, false, DatedFilesScanner.RASTER_EXTENSIONS, "yyyy_DDD");
        assertEquals(3, result.files().size());
        assertEquals(LocalDateTime.of(2018, 2, 14, 0, 0), result.files().get(0).dateTime());
        assertTrue(result.duplicates().isEmpty());
    }

    public void testPatternOnThePath() throws Exception {
        touch("2018/045.tif");
        touch("2018/046.tif");
        touch("2019/001.tif");
        ScanResult result = DatedFilesScanner.scan(root, true, DatedFilesScanner.RASTER_EXTENSIONS, "yyyy/DDD");
        assertEquals(3, result.files().size());
        assertEquals(LocalDateTime.of(2019, 1, 1, 0, 0), result.files().get(2).dateTime());
    }

    public void testHoursAndInvalidDates() throws Exception {
        touch("era5_2020010106.tif");
        touch("era5_2020010112.tif");
        touch("era5_2020133112.tif");
        ScanResult result = DatedFilesScanner.scan(root, false, DatedFilesScanner.RASTER_EXTENSIONS, "yyyyMMddHH");
        assertTrue(result.hasTime());
        assertEquals(2, result.files().size());
        assertEquals(LocalDateTime.of(2020, 1, 1, 12, 0), result.files().get(1).dateTime());
        // month 13
        assertEquals(1, result.undated().size());
    }

    public void testValidation() throws Exception {
        assertNull(DatedFilesScanner.validate("yyyy_DDD"));
        assertNull(DatedFilesScanner.validate("yyyy/MM/dd"));
        assertNotNull(DatedFilesScanner.validate("MMdd"));
        assertNotNull(DatedFilesScanner.validate("yyMMdd"));
        assertNotNull(DatedFilesScanner.validate("yyyyMMDDD"));
        assertNotNull(DatedFilesScanner.validate("yyyyQQ"));
        assertNotNull(DatedFilesScanner.validate("yyyy-yyyy"));
        assertNotNull(DatedFilesScanner.validate(""));
    }
}
