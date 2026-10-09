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
package org.hortonmachine.lesto;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.RandomIterFactory;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.lesto.modules.filter.LasHeightDistribution;
import org.hortonmachine.lesto.modules.filter.LasHistogram;
import org.hortonmachine.lesto.modules.filter.LasMerger;
import org.hortonmachine.lesto.modules.filter.LasSplitter;
import org.hortonmachine.lesto.modules.filter.LasThresholder;
import org.hortonmachine.lesto.modules.raster.Las2BivariateRasterMosaic;
import org.hortonmachine.lesto.modules.raster.Las2RasterInterpolator;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import junit.framework.TestCase;

/**
 * Tests the lidar filter and raster modules on synthetic surveys.
 */
public class TestLestoModules extends TestCase {

    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    private CoordinateReferenceSystem crs;
    private File folder;

    @Override
    protected void setUp() throws Exception {
        crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632", true);
        folder = File.createTempFile("hm_lesto_", "");
        folder.delete();
        folder.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(folder);
    }

    /**
     * The table of the histogram has all the points, the maximum too.
     */
    public void testHistogramTable() throws Exception {
        List<LasRecord> points = new ArrayList<>();
        for( int i = 0; i <= 100; i++ ) {
            LasRecord point = point(i % 10 + 0.5, i / 10 + 0.5, 100.0, 1);
            point.intensity = (short) (i * 3);
            points.add(point);
        }
        File las = writeLas("intensity.las", points);

        LasHistogram histogram = new LasHistogram();
        histogram.inLas = las.getAbsolutePath();
        histogram.pType = "intensity";
        histogram.pBin = 7;
        histogram.doPlot = false;
        histogram.outCsv = new File(folder, "histogram.csv").getAbsolutePath();
        histogram.process();

        List<String> lines = FileUtilities.readFileToLinesList(histogram.outCsv);
        assertEquals("intensity,count", lines.get(0));
        assertEquals(8, lines.size());
        long total = 0;
        for( String line : lines.subList(1, lines.size()) ) {
            total += Long.parseLong(line.split(",")[1]);
        }
        assertEquals(101, total);
    }

    /**
     * Split, merge back and threshold: no point is lost and the outputs keep the crs.
     */
    public void testSplitMergeThreshold() throws Exception {
        File las = writeLas("forest.las", forestPoints());

        File pieces = new File(folder, "pieces");
        pieces.mkdirs();
        LasSplitter splitter = new LasSplitter();
        splitter.inFile = las.getAbsolutePath();
        splitter.pRows = 2;
        splitter.pCols = 2;
        splitter.outFolder = pieces.getAbsolutePath();
        splitter.process();
        assertTrue(new File(pieces, "forest_4.las").exists());
        assertFalse("nothing written next to the input", new File(folder, "forest_1.las").exists());

        LasMerger merger = new LasMerger();
        merger.inFolder = pieces.getAbsolutePath();
        merger.outLas = new File(folder, "merged.las").getAbsolutePath();
        merger.process();
        assertEquals(200, countPoints(new File(merger.outLas)));
        assertTrue("the merged file has the crs", new File(folder, "merged.prj").exists());

        LasThresholder thresholder = new LasThresholder();
        thresholder.inLas = las.getAbsolutePath();
        thresholder.pType = "elevation";
        thresholder.pLower = 105.0;
        thresholder.outLas = new File(folder, "canopy.las").getAbsolutePath();
        thresholder.process();
        assertEquals(100, countPoints(new File(thresholder.outLas)));
        assertTrue("the filtered file has the crs", new File(folder, "canopy.prj").exists());
    }

    /**
     * Two layers of canopy over all the plot are a double layered forest, everywhere: also on the
     * borders of tiles that do not fall on the borders of the cells of the dem.
     */
    public void testHeightDistributionDoubleLayer() throws Exception {
        // the crowns between 3 and 7 m and between 20 and 24 m over the ground, each missing in
        // a different 5% of the cells of 2 m: the layers overlap in 95% of their cells
        List<LasRecord> points = new ArrayList<>();
        for( int i = 0; i < 80; i++ ) {
            for( int j = 0; j < 80; j++ ) {
                double height = ((i * 7 + j * 3) % 17) * 0.25;
                int cell = missingCell(i / 4, j / 4);
                if (cell != 0) {
                    points.add(point(i * 0.5 + 0.25, j * 0.5 + 0.25, 103.0 + height, 1));
                }
                if (cell != 10) {
                    points.add(point(i * 0.5 + 0.25, j * 0.5 + 0.25, 120.0 + height, 2));
                }
            }
        }
        File las = writeLas("layers.las", points);
        File dem = writeDem("dem.tif", 40, 2.0, 100.0);
        // tiles of 15 m from 1 m: their borders are inside the cells of 2 m
        File tiles = writeTiles("tiles.shp", 1.0, 15.0, 2);

        LasHeightDistribution distribution = new LasHeightDistribution();
        distribution.inIndexFile = las.getAbsolutePath();
        distribution.inDem = dem.getAbsolutePath();
        distribution.inVector = tiles.getAbsolutePath();
        distribution.outCats = new File(folder, "layers.tif").getAbsolutePath();
        distribution.process();

        GridCoverage2D cats = OmsRasterReader.readRaster(distribution.outCats);
        RandomIter iter = RandomIterFactory.create(cats.getRenderedImage(), null);
        // the tiles from 1 to 31 m are snapped on the cells from 2 to 32 m
        for( int row = 4; row < 19; row++ ) {
            for( int col = 1; col < 16; col++ ) {
                // a single layer where the other is missing
                int cell = missingCell(col, 19 - row);
                double expected = cell == 0 || cell == 10 ? 1.0 : 3.0;
                assertEquals("cell " + col + "/" + row, expected, iter.getSampleDouble(col, row, 0), 0.0);
            }
        }
        iter.done();
    }

    /**
     * @return the code of the cell of 2 m: 0 and 10 mark a cell in twenty with a missing layer.
     */
    private static int missingCell( int cellX, int cellY ) {
        return (cellX + cellY * 7) % 20;
    }

    /**
     * With the normalization the raster has the height of the canopy, without it its elevation.
     */
    public void testInterpolatorNormalization() throws Exception {
        File las = writeLas("forest.las", forestPoints());
        File dem = writeDem("dem.tif", 10, 1.0, 100.0);

        for( boolean doNormalize : new boolean[]{true, false} ) {
            Las2RasterInterpolator interpolator = new Las2RasterInterpolator();
            interpolator.inIndexFile = las.getAbsolutePath();
            interpolator.inDtm = dem.getAbsolutePath();
            interpolator.doNormalize = doNormalize;
            interpolator.pThreshold = 2.0;
            interpolator.pImpulse = null;
            interpolator.outRaster = new File(folder, "canopy_" + doNormalize + ".tif").getAbsolutePath();
            interpolator.process();

            double expected = doNormalize ? 10.0 : 110.0;
            double[] minMax = minMax(OmsRasterReader.readRaster(interpolator.outRaster).getRenderedImage());
            assertEquals(expected, minMax[0], 0.01);
            assertEquals(expected, minMax[1], 0.01);
        }
    }

    /**
     * Points on the border of a buffered tile must not make the tile fail.
     */
    public void testBivariateMosaicPointsOnTileBorder() throws Exception {
        List<LasRecord> points = new ArrayList<>();
        for( int i = 0; i <= 20; i++ ) {
            for( int j = 0; j <= 20; j++ ) {
                points.add(point(i * 0.5, j * 0.5, 100.0 + i * 0.1, 1));
            }
        }
        File las = writeLas("plane.las", points);

        File mosaicFolder = new File(folder, "mosaic");
        mosaicFolder.mkdirs();
        Las2BivariateRasterMosaic mosaic = new Las2BivariateRasterMosaic();
        mosaic.inLas = las.getAbsolutePath();
        // the tiles of 5 m buffered by 2 cells of 0.5 m end at 6 m, where there are points
        mosaic.pTilesize = 5.0;
        mosaic.pRes = 0.5;
        mosaic.pBuffer = 2;
        mosaic.pMinpoints = 6;
        mosaic.outFolder = mosaicFolder.getAbsolutePath();
        mosaic.process();

        File tile = new File(mosaicFolder, "tile_0_0.tiff");
        assertTrue(tile.exists());
        // the plane z = 100 + 0.2 x, at the center of the first column of cells, x = 0.25
        RenderedImage image = OmsRasterReader.readRaster(tile.getAbsolutePath()).getRenderedImage();
        RandomIter iter = RandomIterFactory.create(image, null);
        assertEquals(100.05, iter.getSampleDouble(0, 5, 0), 0.001);
        iter.done();
    }

    /**
     * A forest of 10 x 10 m: a ground point and a canopy point in the center of each meter.
     */
    private static List<LasRecord> forestPoints() {
        List<LasRecord> points = new ArrayList<>();
        for( int x = 0; x < 10; x++ ) {
            for( int y = 0; y < 10; y++ ) {
                points.add(point(x + 0.5, y + 0.5, 100.1, 2));
                points.add(point(x + 0.5, y + 0.5, 110.0, 1));
            }
        }
        return points;
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

    private File writeLas( String name, List<LasRecord> points ) throws Exception {
        double[] bounds = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE};
        for( LasRecord point : points ) {
            bounds[0] = Math.min(bounds[0], point.x);
            bounds[1] = Math.max(bounds[1], point.x);
            bounds[2] = Math.min(bounds[2], point.y);
            bounds[3] = Math.max(bounds[3], point.y);
            bounds[4] = Math.min(bounds[4], point.z);
            bounds[5] = Math.max(bounds[5], point.z);
        }
        File lasFile = new File(folder, name);
        try (ALasWriter writer = ALasWriter.getWriter(lasFile, crs)) {
            writer.setBounds(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
            writer.open();
            for( LasRecord point : points ) {
                writer.addPoint(point);
            }
        }
        return lasFile;
    }

    private File writeDem( String name, double size, double res, double elevation ) throws Exception {
        int cells = (int) Math.round(size / res);
        double[][] data = new double[cells][cells];
        for( double[] row : data ) {
            Arrays.fill(row, elevation);
        }
        RegionMap region = CoverageUtilities.makeRegionParamsMap(size, 0, 0, size, res, res, cells, cells);
        File demFile = new File(folder, name);
        OmsRasterWriter.writeRaster(demFile.getAbsolutePath(), CoverageUtilities.buildCoverage("dem", data, region, crs, true));
        return demFile;
    }

    /**
     * Square tiles with an id, starting from the given origin.
     */
    private File writeTiles( String name, double origin, double side, int number ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("tiles");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("id", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection tiles = new DefaultFeatureCollection();
        int id = 0;
        for( int i = 0; i < number; i++ ) {
            for( int j = 0; j < number; j++ ) {
                double x = origin + i * side;
                double y = origin + j * side;
                Polygon tile = GeometryUtilities.createPolygonFromEnvelope(new Envelope(x, x + side, y, y + side));
                builder.addAll(new Object[]{tile, String.valueOf(id++)});
                tiles.add(builder.buildFeature(null));
            }
        }
        File tilesFile = new File(folder, name);
        OmsVectorWriter.writeVector(tilesFile.getAbsolutePath(), tiles);
        return tilesFile;
    }

    private static long countPoints( File lasFile ) throws Exception {
        try (ALasReader reader = ALasReader.getReader(lasFile, null)) {
            reader.open();
            return reader.getHeader().getRecordsCount();
        }
    }

    private static double[] minMax( RenderedImage image ) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        RandomIter iter = RandomIterFactory.create(image, null);
        for( int y = 0; y < image.getHeight(); y++ ) {
            for( int x = 0; x < image.getWidth(); x++ ) {
                double value = iter.getSampleDouble(x, y, 0);
                if (!HMConstants.isNovalue(value)) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }
        iter.done();
        return new double[]{min, max};
    }
}
