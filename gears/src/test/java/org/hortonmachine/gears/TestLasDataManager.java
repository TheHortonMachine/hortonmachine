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
package org.hortonmachine.gears;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.databases.DatabaseLasWriter;
import org.hortonmachine.gears.io.las.databases.LasCell;
import org.hortonmachine.gears.io.las.databases.LasCellsTable;
import org.hortonmachine.gears.io.las.databases.LasLevel;
import org.hortonmachine.gears.io.las.databases.LasLevelsTable;
import org.hortonmachine.gears.io.las.databases.LasSource;
import org.hortonmachine.gears.io.las.databases.LasSourcesTable;
import org.hortonmachine.gears.io.las.index.LasIndexer;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

/**
 * Tests the normalization of the las points on a dem by the data manager of a single las file, and the
 * coverage helpers used by the lidar modules.
 */
public class TestLasDataManager extends HMTestCase {

    private static final double GROUND = 100.0;
    private static final double CANOPY = 110.0;

    /**
     * Every request of points, not only the first one, has to give the points over the threshold,
     * with their height over the dem.
     */
    public void testNormalizationOnEveryRequest() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D dem = flatDem(crs, 10, 1.0, GROUND);
        File lasFile = writeForestLas(crs);

        try (ALasDataManager dataManager = ALasDataManager.getDataManager(lasFile, dem, 2.0, crs)) {
            dataManager.open();
            // two tiles, each with 50 ground and 50 canopy points
            Polygon westTile = GeometryUtilities.createPolygonFromEnvelope(new Envelope(0, 5, 0, 10));
            Polygon eastTile = GeometryUtilities.createPolygonFromEnvelope(new Envelope(5, 10, 0, 10));
            for( Polygon tile : new Polygon[]{westTile, eastTile, westTile} ) {
                List<LasRecord> points = dataManager.getPointsInGeometry(tile, true);
                assertEquals("only the canopy is over the threshold", 50, points.size());
                for( LasRecord point : points ) {
                    assertEquals(CANOPY - GROUND, point.groundElevation, 0.01);
                }
            }
        } finally {
            lasFile.delete();
        }
    }

    /**
     * The header has the bounds of the written points, also when the ones set before are wrong,
     * as those of the input of a module that changes the elevations.
     */
    public void testWriterBoundsOfThePoints() throws Exception {
        File lasFile = File.createTempFile("hm_bounds_", ".las");
        try {
            try (ALasWriter writer = ALasWriter.getWriter(lasFile, HMTestMaps.getCrs())) {
                writer.setBounds(0, 1000, 0, 1000, 1900, 1950);
                writer.open();
                writer.addPoint(point(2.5, 3.5, 12.25, 1));
                writer.addPoint(point(7.5, 1.5, 0.5, 1));
            }
            try (ALasReader reader = ALasReader.getReader(lasFile, null)) {
                reader.open();
                ReferencedEnvelope3D envelope = reader.getHeader().getDataEnvelope();
                assertEquals(2.5, envelope.getMinX(), 0.001);
                assertEquals(7.5, envelope.getMaxX(), 0.001);
                assertEquals(1.5, envelope.getMinY(), 0.001);
                assertEquals(3.5, envelope.getMaxY(), 0.001);
                assertEquals(0.5, envelope.getMinZ(), 0.001);
                assertEquals(12.25, envelope.getMaxZ(), 0.001);
            }
        } finally {
            lasFile.delete();
        }
    }

    /**
     * The builder of the las features has the crs it is asked for, not the one of the first request.
     */
    public void testFeatureBuilderCrs() throws Exception {
        CoordinateReferenceSystem utm = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632", true);
        CoordinateReferenceSystem wgs84 = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        SimpleFeatureBuilder utmBuilder = LasUtils.getLasFeatureBuilder(utm);
        assertTrue(HMCrsRegistry.crsEquals(utm, utmBuilder.getFeatureType().getCoordinateReferenceSystem()));
        SimpleFeatureBuilder wgs84Builder = LasUtils.getLasFeatureBuilder(wgs84);
        assertTrue(HMCrsRegistry.crsEquals(wgs84, wgs84Builder.getFeatureType().getCoordinateReferenceSystem()));
    }

    /**
     * The index of a folder serves all the points of its files, and the overview has a polygon per
     * file. An index name without extension gets it, to be readable by the data managers.
     */
    public void testFolderIndex() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        File folder = Files.createTempDirectory("hm_lasindex_").toFile();
        try {
            File las = writeForestLas(crs);
            File prj = FileUtilities.substituteExtention(las, "prj");
            for( String name : new String[]{"forest", "forest2"} ) {
                FileUtilities.copyFile(las, new File(folder, name + ".las"));
                FileUtilities.copyFile(prj, new File(folder, name + ".prj"));
            }
            las.delete();
            prj.delete();

            LasIndexer indexer = new LasIndexer();
            indexer.inFolder = folder.getAbsolutePath();
            indexer.pIndexname = "survey";
            indexer.pCellsize = 2.0;
            indexer.doOverview = true;
            indexer.process();

            File indexFile = new File(folder, "survey" + LasIndexer.INDEX_EXTENSION);
            assertTrue(indexFile.exists());
            assertTrue(new File(folder, "forest_indexed.las").exists());
            assertTrue(new File(folder, "forest2_indexed.las").exists());
            try (ALasDataManager dataManager = ALasDataManager.getDataManager(indexFile, null, 0, crs)) {
                dataManager.open();
                Polygon all = GeometryUtilities.createPolygonFromEnvelope(new Envelope(-1, 11, -1, 11));
                assertEquals(400, dataManager.getPointsInGeometry(all, false).size());
                Polygon westTile = GeometryUtilities.createPolygonFromEnvelope(new Envelope(0, 5, 0, 10));
                assertEquals(200, dataManager.getPointsInGeometry(westTile, false).size());
            }
            SimpleFeatureCollection overview = OmsVectorReader
                    .readVector(new File(folder, "survey.shp").getAbsolutePath());
            assertEquals(2, overview.size());
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    /**
     * The database gets all the points in its cells, and the averages of the intensity of the
     * levels do not overflow when their sum is larger than a short.
     */
    public void testDatabaseLasWriter() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        File folder = Files.createTempDirectory("hm_lasdb_").toFile();
        try {
            File lasFile = new File(folder, "bright.las");
            try (ALasWriter writer = ALasWriter.getWriter(lasFile, crs)) {
                writer.setBounds(0, 10, 0, 10, GROUND, CANOPY);
                writer.open();
                for( int x = 0; x < 10; x++ ) {
                    for( int y = 0; y < 10; y++ ) {
                        LasRecord bright = point(x + 0.5, y + 0.5, CANOPY, 1);
                        bright.intensity = 20000;
                        writer.addPoint(bright);
                    }
                }
            }

            DatabaseLasWriter dbWriter = new DatabaseLasWriter();
            dbWriter.inFolder = folder.getAbsolutePath();
            dbWriter.inDatabasePath = new File(folder, "las_db").getAbsolutePath();
            dbWriter.pDbType = EDb.H2GIS.name();
            dbWriter.pCellsize = 1.0;
            dbWriter.pLevels = 2;
            dbWriter.pFactor = 2;
            dbWriter.process();

            try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
                db.open(dbWriter.inDatabasePath);
                List<LasSource> sources = LasSourcesTable.getLasSources(db);
                assertEquals(1, sources.size());
                List<LasCell> cells = LasCellsTable.getLasCellsBySource(db, sources.get(0).id, false, false, false, false,
                        false);
                int count = 0;
                for( LasCell cell : cells ) {
                    count += cell.pointsCount;
                }
                assertEquals(100, count);
                for( int level = 1; level <= 2; level++ ) {
                    List<LasLevel> levels = LasLevelsTable.getLasLevels(db, level, new Envelope(-1, 11, -1, 11));
                    assertFalse(levels.isEmpty());
                    for( LasLevel lasLevel : levels ) {
                        assertEquals("level " + level, 20000, lasLevel.avgIntensity);
                    }
                }
            }
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    public void testTemplateCoverageHasNodata() throws Exception {
        GridCoverage2D dem = flatDem(HMTestMaps.getCrs(), 10, 1.0, GROUND);
        GridCoverage2D fromTemplate = CoverageUtilities.createCoverageFromTemplate(dem, HMConstants.doubleNovalue, null);
        NoDataContainer nodata = (NoDataContainer) fromTemplate.getProperty(NoDataContainer.GC_NODATA);
        assertNotNull("the nodata is declared, so that the written file has it", nodata);
        assertEquals(HMConstants.doubleNovalue, nodata.getAsSingleValue(), 0.0);
    }

    public void testSubCoverageSizeOnSnappedRegion() throws Exception {
        GridCoverage2D dem = flatDem(HMTestMaps.getCrs(), 10, 0.1, GROUND);
        // 0.3 / 0.1 is 2.9999999999999996 in double
        GridCoverage2D sub = CoverageUtilities.createSubCoverageFromTemplate(dem, new Envelope(0, 0.3, 0, 0.3), null, null);
        assertEquals(3, sub.getRenderedImage().getWidth());
        assertEquals(3, sub.getRenderedImage().getHeight());
    }

    /**
     * A tiff read back in tiles gives an image whose data is not writable.
     */
    public void testReplaceNovalueOnReadOnlyData() throws Exception {
        GridCoverage2D dem = flatDem(HMTestMaps.getCrs(), 121, 1.0, GROUND);
        WritableRaster[] holder = new WritableRaster[1];
        GridCoverage2D coverage = CoverageUtilities.createCoverageFromTemplate(dem, 1.0, holder);
        for( int i = 0; i < 121; i++ ) {
            holder[0].setSample(i, i, 0, HMConstants.doubleNovalue);
        }
        File tiff = File.createTempFile("hm_novalue_", ".tif");
        try {
            OmsRasterWriter.writeRaster(tiff.getAbsolutePath(), coverage);
            RenderedImage image = OmsRasterReader.readRaster(tiff.getAbsolutePath()).getRenderedImage();
            assertFalse("the case of the failure", image.getData() instanceof WritableRaster);

            WritableRaster replaced = CoverageUtilities.replaceNovalue(image, 0.0);
            for( int y = 0; y < 121; y++ ) {
                for( int x = 0; x < 121; x++ ) {
                    assertEquals(x == y ? 0.0 : 1.0, replaced.getSampleDouble(x, y, 0), 0.0);
                }
            }
            assertTrue("the image is not changed", HMConstants.isNovalue(image.getData().getSampleDouble(2, 2, 0)));
        } finally {
            tiff.delete();
        }
    }

    private static GridCoverage2D flatDem( CoordinateReferenceSystem crs, int size, double res, double elevation ) {
        double[][] data = new double[size][size];
        for( double[] row : data ) {
            Arrays.fill(row, elevation);
        }
        RegionMap region = CoverageUtilities.makeRegionParamsMap(size * res, 0, 0, size * res, res, res, size, size);
        return CoverageUtilities.buildCoverage("dem", data, region, crs, true);
    }

    /**
     * A forest of 10 x 10 m: a ground point and a canopy point in the center of each meter.
     */
    private static File writeForestLas( CoordinateReferenceSystem crs ) throws Exception {
        File lasFile = File.createTempFile("hm_forest_", ".las");
        try (ALasWriter writer = ALasWriter.getWriter(lasFile, crs)) {
            writer.setBounds(0, 10, 0, 10, GROUND, CANOPY);
            writer.open();
            for( int x = 0; x < 10; x++ ) {
                for( int y = 0; y < 10; y++ ) {
                    writer.addPoint(point(x + 0.5, y + 0.5, GROUND + 0.1, 2));
                    writer.addPoint(point(x + 0.5, y + 0.5, CANOPY, 1));
                }
            }
        }
        return lasFile;
    }

    private static LasRecord point( double x, double y, double z, int returnNumber ) {
        LasRecord record = new LasRecord();
        record.x = x;
        record.y = y;
        record.z = z;
        record.intensity = 100;
        record.returnNumber = (short) returnNumber;
        record.numberOfReturns = 2;
        record.classification = 1;
        return record;
    }
}
