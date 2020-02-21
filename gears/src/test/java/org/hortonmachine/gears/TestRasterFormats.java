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
package org.hortonmachine.gears;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Generic tests.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterFormats extends HMTestCase {

    public File getFile( String relPath ) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(relPath);
        File file = new File(url.toURI());
        return file;
    }

    public void testAsc() throws Exception {
        File file = getFile("formats/asc/dem.asc");
        GridCoverage2D raster = OmsRasterReader.readRaster(file.getAbsolutePath());
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        double novalue = CoverageUtilities.getNovalue(raster);

        assertEquals(1404, regionMap.getCols());
        assertEquals(1400, regionMap.getRows());
        assertEquals(22.5, regionMap.getXres());
        assertEquals(969870.0, regionMap.getWest());
        assertEquals(642840.0, regionMap.getSouth());

        assertEquals(-9999.0, novalue);

        file = getFile("formats/asc/SWAN_NURC_LigurianSeaL07_HSIGN.asc");
        raster = OmsRasterReader.readRaster(file.getAbsolutePath());
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        novalue = CoverageUtilities.getNovalue(raster);

        assertEquals(278, regionMap.getCols());
        assertEquals(144, regionMap.getRows());
        assertEquals(0.008999999478566561, regionMap.getXres(), DELTA);
        assertEquals(8.118000030517578, regionMap.getWest() + regionMap.getXres() / 2, DELTA);
        assertEquals(43.191001892089844, regionMap.getSouth() + regionMap.getXres() / 2, DELTA);

        assertEquals(-9.0, novalue);

        file = getFile("formats/asc/095b_dem_90m.asc");
        raster = OmsRasterReader.readRaster(file.getAbsolutePath());
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        novalue = CoverageUtilities.getNovalue(raster);

        assertEquals(351, regionMap.getCols());
        assertEquals(350, regionMap.getRows());
        assertEquals(90, regionMap.getXres(), DELTA);
        assertEquals(969870, regionMap.getWest(), DELTA);
        assertEquals(642840, regionMap.getSouth(), DELTA);

        assertEquals(-9999.0, novalue);
    }

    public void testTiff() throws Exception {
        File file = getFile("formats/tiff/test.tif");
        GridCoverage2D raster = OmsRasterReader.readRaster(file.getAbsolutePath());
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        Double novalue = CoverageUtilities.getNovalue(raster);
        assertNull(novalue);

        assertEquals(30, regionMap.getCols());
        assertEquals(26, regionMap.getRows());
        assertEquals(7874.0, regionMap.getXres());
        assertEquals(8120.769230769229580, regionMap.getYres(), DELTA);
        assertEquals(688054.25, regionMap.getWest());
        assertEquals(5472037.36448598, regionMap.getSouth(), DELTA);

        CoordinateReferenceSystem crs = raster.getCoordinateReferenceSystem();
        String epsg = CrsUtilities.getCodeFromCrs(crs);
        assertEquals("EPSG:26921", epsg);

    }

}
