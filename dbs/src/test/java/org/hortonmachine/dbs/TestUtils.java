package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomReader;
import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomWriter;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.MercatorUtils;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

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
    public void testTiles() throws Exception {
        int tx = 0;
        int ty = 0;
        int tz = 0;

        Envelope env4326 = MercatorUtils.tileBounds4326(tx, ty, tz);
        Envelope env3857 = MercatorUtils.tileBounds3857(tx, ty, tz);

        Coordinate ll3857 = new Coordinate(env3857.getMinX(), env3857.getMinY());
        Coordinate ur3857 = new Coordinate(env3857.getMaxX(), env3857.getMaxY());

        Coordinate ll4326transf = MercatorUtils.convert3857To4326(ll3857);
        Coordinate ur4326transf = MercatorUtils.convert3857To4326(ur3857);
        Coordinate ll4326 = new Coordinate(env4326.getMinX(), env4326.getMinY());
        Coordinate ur4326 = new Coordinate(env4326.getMaxX(), env4326.getMaxY());

        double tolerance = 0.0000001;
        assertTrue(ll4326transf.equals2D(ll4326, tolerance));
        assertTrue(ur4326transf.equals2D(ur4326, tolerance));
    }

    @Test
    public void testTiles2() throws Exception {
        int zoomLevel = 8;
        double tolerance = 0.0000001;

        Coordinate c1 = new Coordinate(1289079.2130195359, 5900910.886700573);
        Coordinate c2 = new Coordinate(1298888.6991376826, 5909656.474824957);
        Envelope env3857 = new Envelope(c1, c2);
        Envelope env4326 = MercatorUtils.convert3857To4326(env3857);

        Coordinate centre = env4326.centre();
        assertTrue(centre.equals2D(new Coordinate(11.62405565150858, 46.77412159831822), tolerance));

        int[] tileNumberFrom3857 = MercatorUtils.getTileNumberFrom3857(env3857.centre(), zoomLevel);
        assertEquals(136, tileNumberFrom3857[1]);
        assertEquals(90, tileNumberFrom3857[2]);
        assertEquals(8, tileNumberFrom3857[0]);

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

    @Test
    public void testDbUtils() throws Exception {
        String name = "123name";
        String fixedName = DbsUtilities.fixTableName(name);
        assertEquals("'" + name + "'", fixedName);

        name = "name with space";
        fixedName = DbsUtilities.fixTableName(name);
        assertEquals("'" + name + "'", fixedName);
    }

    @Test
    public void testWkb() throws Exception {
        Coordinate c1 = new Coordinate(1, 2, 3);
        Coordinate c2 = new Coordinate(10, 20, 30);
        GeometryFactory gf = new GeometryFactory();
        LineString lineString = gf.createLineString(new Coordinate[]{c1, c2});
        lineString.setSRID(4326);

        int outputDimension = 3;
        WKBWriter w = new WKBWriter(outputDimension);
        byte[] bytes = w.write(lineString);
        
        byte[] bytesGpkg =  new GeoPkgGeomWriter(outputDimension).write(lineString);

        WKBReader r = new WKBReader(gf);
        Geometry geometry = r.read(bytes);
        
        Geometry geometryGpkg = new GeoPkgGeomReader(bytesGpkg).get();
        
        assertTrue(geometry.equals(geometryGpkg));

    }

}
