package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.hortonmachine.dbs.utils.MercatorUtils;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

/**
 * Tests for utilities
 */
public class TestUtils {

    @Test
    public void testMercatorConversion() throws Exception {
        double lon = -3.594;
        double lat = 56.029;
        double x = -400082.25;
        double y = 7564190.90;

        test(lon, lat, x, y);

        lon = 56.029;
        lat = -3.594;
        x = 6237119.75;
        y = -400344.88;

        test(lon, lat, x, y);

        lon = 3.594;
        lat = -56.029;
        x = 400082.25;
        y = -7564190.90;

        test(lon, lat, x, y);

        lon = -56.029;
        lat = 3.594;
        x = -6237119.75;
        y = 400344.88;

        test(lon, lat, x, y);
    }

    @Test
    public void testTilesDownscaling() throws Exception {
        int zoom = 7;
        int x = 62;
        int y = 39;
        int newZoom = 8;

        List<String> expectedTiles = new ArrayList<>();
        expectedTiles.add("124_78");
        expectedTiles.add("125_78");
        expectedTiles.add("124_79");
        expectedTiles.add("125_79");
        List<int[]> tilesAtHigherZoom = MercatorUtils.getTilesAtHigherZoom(x, y, zoom, newZoom, 256);
        assertEquals(4, tilesAtHigherZoom.size());

        int index = 0;
        for( int[] zxy : tilesAtHigherZoom ) {
            String id = zxy[1] + "_" + zxy[2];
            assertEquals(expectedTiles.get(index++), id);
        }

        newZoom = 9;

        expectedTiles.clear();

        expectedTiles.add("248_156");
        expectedTiles.add("249_156");
        expectedTiles.add("250_156");
        expectedTiles.add("251_156");

        expectedTiles.add("248_157");
        expectedTiles.add("249_157");
        expectedTiles.add("250_157");
        expectedTiles.add("251_157");

        expectedTiles.add("248_158");
        expectedTiles.add("249_158");
        expectedTiles.add("250_158");
        expectedTiles.add("251_158");

        expectedTiles.add("248_159");
        expectedTiles.add("249_159");
        expectedTiles.add("250_159");
        expectedTiles.add("251_159");

        tilesAtHigherZoom = MercatorUtils.getTilesAtHigherZoom(x, y, zoom, newZoom, 256);
        assertEquals(16, tilesAtHigherZoom.size());

        index = 0;
        for( int[] zxy : tilesAtHigherZoom ) {
            String id = zxy[1] + "_" + zxy[2];
            assertEquals(expectedTiles.get(index++), id);
        }
    }

    private void test( double lon, double lat, double x, double y ) {
        Coordinate coord3857 = MercatorUtils.convert4326To3857(new Coordinate(lon, lat));

        double delta = 0.01;
        assertEquals(x, coord3857.x, delta);
        assertEquals(y, coord3857.y, delta);

        Coordinate coord4326 = MercatorUtils.convert3857To4326(new Coordinate(x, y));
        assertEquals(lon, coord4326.x, delta);
        assertEquals(lat, coord4326.y, delta);
    }

}
