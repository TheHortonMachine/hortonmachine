package org.hortonmachine.gears.modules;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.stac.HMStacCollection;
import org.hortonmachine.gears.io.stac.HMStacManager;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.MergeMode;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.RegionMap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import java.util.Calendar;
import java.util.Date;

public class TestStacQuery extends HMTestCase {

    protected void setUp() throws Exception {

    }

    // This example shows the data of different queries are being added to each other.
    // In this context that operation makes no sense, as this query is a landcover raster where each
    // value has a meaning (1 - water, 2 - trees...)
    // My question is if the HortonMachine would be able to apply different actions to the rasters
    // obtained from the assets
    // e.g. average the results, use the most common value, etc.
    // @SuppressWarnings("deprecation")
    // public void testCompareTwoQueriesOnTheSameRegion() throws Exception {
    //     // This query has a single item
    //     var start = new Date(2020 - 1900, Calendar.JANUARY, 2);
    //     var end = new Date(2020 - 1900, Calendar.JANUARY, 3);
    //     HMRaster rasterOne = getRaster(start, end);

    //     // This query has three items
    //     start = new Date(2017 - 1900, Calendar.JANUARY, 0);
    //     end = new Date(2020 - 1900, Calendar.JANUARY, 0);
    //     HMRaster rasterThree = getRaster(start, end);
    //     GridCoverage2D coverage = rasterThree.buildCoverage();

    //     int totalCells = 0;
    //     int numberOfEqualCells = 0;
    //     for( int i = 0; i < rasterOne.getCols(); i++ ) {
    //         for( int j = 0; j < rasterOne.getCols(); j++ ) {
    //             Coordinate coor = new Coordinate(i, j);
    //             System.out.println("(" + i + "," + j + "):" + rasterOne.getValue(i, j) + ":" + rasterThree.getValue(i, j));
    //             totalCells++;
    //             if (rasterOne.getValue(i, j) == rasterThree.getValue(i, j) / 3) {
    //                 numberOfEqualCells++;
    //             }
    //         }
    //     }
    //     System.out.println("Percentage of coincidence: " + numberOfEqualCells * 100.0 / totalCells + "%");
    // }

    private static HMRaster getRaster( Date start, Date end ) throws Exception {
        var pm = new DummyProgressMonitor();

        String catalogURL = "https://planetarycomputer.microsoft.com/api/stac/v1";
        try (HMStacManager stacManager = new HMStacManager(catalogURL, pm)) {
            stacManager.open();
            HMStacCollection collection = stacManager.getCollectionById("io-lulc-9-class");

            WKTReader r = new WKTReader();
            Geometry polygon = r
                    .read("POLYGON ((-3.0521 42.8042, -3.0521 43.3908, -1.2943 43.3908, -1.2943 42.8042, -3.0521 42.8042))");
            collection.setGeometryFilter(polygon);
            collection.setTimestampFilter(start, end);

            var items = collection.searchItems();

            RegionMap latLongRegionMap = RegionMap.fromEnvelopeAndGrid(polygon.getEnvelopeInternal(), 30, 30);

            HMRaster raster = collection.readRasterBandOnRegion(latLongRegionMap, "data", items, true, MergeMode.AVG, pm);
            
            return raster;
        }
    }

}
