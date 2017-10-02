package org.hortonmachine.modules;

import java.util.Arrays;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.modules.Raster;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test {@link Raster}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRaster extends HMTestCase {

    public void testRaster() throws Exception {
        RegionMap e = HMTestMaps.envelopeParams;

        Raster r1 = new Raster(e.getCols(), e.getRows(), e.getXres(), e.getWest(), e.getNorth(), "EPSG:32632");

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> eP = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, eP, crs, true);

        Raster r2 = new Raster(elevationCoverage);

        Raster r3 = new Raster(r2);

        for( int r = 0; r < elevationData.length; r++ ) {
            for( int c = 0; c < elevationData[0].length; c++ ) {
                r1.setValueAt(c, r, elevationData[r][c]);
                r3.setValueAt(c, r, elevationData[r][c]);
            }
        }

        check(5, 6, r1, r2, r3);
        check(3, 4, r1, r2, r3);
        check(5, 3, r1, r2, r3);
        check(4, 4, r1, r2, r3);

    }

    public void testBounds() {
        RegionMap e = HMTestMaps.envelopeParams;
        Raster r1 = new Raster(e.getCols(), e.getRows(), e.getXres(), e.getWest(), e.getNorth(), "EPSG:32632");

        int[] gridAt1 = r1.gridAt(e.getWest(), e.getNorth());
        assertEquals(0, gridAt1[0]);
        assertEquals(0, gridAt1[1]);
        int[] gridAt2 = r1.gridAt(e.getWest() - e.getXres(), e.getNorth());
        assertNull(gridAt2);

        double[] positionAt1 = r1.positionAt(e.getCols() - 1, e.getRows() - 1);
        assertEquals(e.getEast() - e.getXres() / 2.0, positionAt1[0], DELTA);
        assertEquals(e.getSouth() + e.getYres() / 2.0, positionAt1[1], DELTA);

        double[] positionAt2 = r1.positionAt(-1, -1);
        assertNull(positionAt2);
    }

    private void check( int col, int row, Raster r1, Raster r2, Raster r3 ) {
        double[] surrounding1 = r1.surrounding(col, row);
        double[] surrounding2 = r2.surrounding(col, row);
        double[] surrounding3 = r3.surrounding(col, row);
        assertTrue(Arrays.equals(surrounding2, surrounding3));
        assertTrue(Arrays.equals(surrounding1, surrounding3));
    }
}
