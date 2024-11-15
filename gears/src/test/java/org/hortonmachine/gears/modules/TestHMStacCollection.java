package org.hortonmachine.gears.modules;

import org.hortonmachine.gears.io.stac.HMStacCollection;
import org.hortonmachine.gears.io.stac.HMStacItem;
import org.hortonmachine.gears.io.stac.HMStacManager;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.HMTestCase;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertThrows;

public class TestHMStacCollection extends HMTestCase {
    private GeometryFactory gf = new GeometryFactory();
    private DummyProgressMonitor pm = new DummyProgressMonitor();
    private HMStacManager manager;

    private Coordinate[] createBbox(double xmin, double ymin, double xmax, double ymax) {
        Coordinate[] bboxCoordinates = new Coordinate[5];
        bboxCoordinates[0] = new Coordinate(xmin, ymin);
        bboxCoordinates[1] = new Coordinate(xmin, ymax);
        bboxCoordinates[2] = new Coordinate(xmax, ymax);
        bboxCoordinates[3] = new Coordinate(xmax, ymin);
        bboxCoordinates[4] = new Coordinate(xmin, ymin);
        return bboxCoordinates;
    }

    protected void setUp() throws Exception {
        super.setUp();

        String catalogUrl = "https://planetarycomputer.microsoft.com/api/stac/v1";
        manager = new HMStacManager(catalogUrl, pm);
        manager.open();
    }

    public void testSearchCatalogWithValidSpatioTemporalFilters() throws Exception {
        HMStacCollection collection = manager.getCollectionById("io-lulc-annual-v02");
        Instant start = Instant.ofEpochMilli(1514764800000L); // 2018-01-01
        Instant end = start.plus(Duration.ofDays(10));
        Polygon bboxPolygon = gf.createPolygon(createBbox(0.0, 0.0, 10.0, 10.0));

        List<HMStacItem> items = collection.setTimestampFilter(Date.from(start), Date.from(end))
                .setGeometryFilter(bboxPolygon)
                .searchItems();

        assertTrue(!items.isEmpty());
    }

    public void testSearchCatalogWithInvalidTemporalFilters() throws Exception {
        HMStacCollection collection = manager.getCollectionById("io-lulc-annual-v02");
        Instant start = Instant.ofEpochMilli(946688400000L); // 2000-01-01, a year with no data
        Instant end = start.plus(Duration.ofDays(10));
        Polygon bboxPolygon = gf.createPolygon(createBbox(0.0, 0.0, 10.0, 10.0));

        List<HMStacItem> items = collection.setTimestampFilter(Date.from(start), Date.from(end))
                .setGeometryFilter(bboxPolygon)
                .searchItems();

        assertTrue(items.isEmpty());
    }

    public void testSearchCatalogWithInvalidSpatialFilters() throws Exception {
        HMStacCollection collection = manager.getCollectionById("io-lulc-annual-v02");
        Instant start = Instant.ofEpochMilli(1514764800000L); // 2018-01-01
        Instant end = start.plus(Duration.ofDays(10));
        Polygon bboxPolygon = gf.createPolygon(createBbox(190.0, 90.0, 200.0, 100.0)); // out of bounds

        List<HMStacItem> items = collection.setTimestampFilter(Date.from(start), Date.from(end))
                .setGeometryFilter(bboxPolygon)
                .searchItems();

        assertTrue(items.isEmpty());
    }

    public void testSearchCatalogUsingBbox() throws Exception {
        HMStacCollection collection = manager.getCollectionById("io-lulc-annual-v02");
        Instant start = Instant.ofEpochMilli(1514764800000L); // 2018-01-01
        Instant end = start.plus(Duration.ofDays(10));
        double[] bbox = {0.0, 0.0, 10.0, 10.0};

        List<HMStacItem> items = collection.setTimestampFilter(Date.from(start), Date.from(end))
                .setBboxFilter(bbox)
                .searchItems();

        assertTrue(!items.isEmpty());
    }

    public void testSearchThrowsExceptionUsingBothBboxAndIntersect() throws Exception {
        HMStacCollection collection = manager.getCollectionById("io-lulc-annual-v02");
        Instant start = Instant.ofEpochMilli(1514764800000L); // 2018-01-01
        Instant end = start.plus(Duration.ofDays(10));
        Polygon bboxPolygon = gf.createPolygon(createBbox(0.0, 0.0, 10.0, 10.0));
        double[] bbox = {0.0, 0.0, 10.0, 10.0};

        assertThrows(IllegalStateException.class, () -> collection.setTimestampFilter(Date.from(start), Date.from(end))
                .setGeometryFilter(bboxPolygon)
                .setBboxFilter(bbox)
                .searchItems()
        );
    }
}
