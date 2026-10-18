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
package org.hortonmachine.server.stac;

import java.io.File;
import java.nio.file.Files;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.SampleData;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.web.MediaTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import com.fasterxml.jackson.databind.JsonNode;

import junit.framework.TestCase;

/**
 * Tests of the description of the files as assets of the STAC items.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestStacFiles extends TestCase {
    private File workspace;
    private File elevation;

    @Override
    protected void setUp() throws Exception {
        workspace = Files.createTempDirectory("hm-stac-test").toFile();
        WorkspacePaths paths = new WorkspacePaths(workspace);
        paths.createFolders();
        SampleData.writeElevation(paths);
        elevation = new File(workspace, SampleData.ELEVATION);
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(workspace);
    }

    private static void assertStatistics( JsonNode asset ) {
        JsonNode statistics = asset.at("/raster:bands/0/statistics");
        assertEquals(5.0, statistics.get("minimum").asDouble(), 1e-9);
        assertEquals(10.0, statistics.get("maximum").asDouble(), 1e-9);
        assertEquals(9.4, statistics.get("mean").asDouble(), 1e-9);
        assertEquals(100.0, statistics.get("valid_percent").asDouble(), 1e-9);
    }

    public void testRaster() throws Exception {
        assertFalse(StacFiles.isCog(elevation));
        StacFiles.Description description = StacFiles.describe(elevation, "elevation.tif");
        JsonNode asset = description.asset;
        assertEquals(MediaTypes.GEOTIFF, asset.get("type").asText());
        assertEquals(elevation.length(), asset.get("file:size").asLong());
        assertEquals("EPSG:32632", asset.get("proj:code").asText());
        assertEquals("[5,5]", asset.get("proj:shape").toString());
        assertEquals("[10.0,0.0,0.0,0.0,-10.0,50.0]", asset.get("proj:transform").toString());
        assertEquals("[0.0,0.0,50.0,50.0]", asset.get("proj:bbox").toString());
        assertEquals(10.0, asset.at("/raster:bands/0/spatial_resolution").asDouble(), 1e-9);
        assertStatistics(asset);

        // the footprint is in WGS84: UTM 32N at the equator, 3 degrees west of its central meridian
        Envelope envelope = description.footprint.getEnvelopeInternal();
        assertEquals(4.51, envelope.getMinX(), 0.01);
        assertEquals(0.0, envelope.getMinY(), 0.001);
        assertTrue(envelope.getWidth() < 0.001 && envelope.getHeight() < 0.001);
    }

    public void testCog() throws Exception {
        StacJobs.toCog(elevation);
        assertTrue(StacFiles.isCog(elevation));
        assertFalse(new File(elevation.getAbsolutePath() + ".cog.tmp").exists());
        // the statistics are read from the COG, not computed
        RasterStats stats = RasterStats.read(elevation);
        assertNotNull(stats);
        assertEquals(5.0, stats.minimum, 1e-9);
        assertEquals(9.4, stats.mean, 1e-9);
        JsonNode asset = StacFiles.describe(elevation, "elevation.tif").asset;
        assertEquals(StacFiles.COG, asset.get("type").asText());
        assertEquals("EPSG:32632", asset.get("proj:code").asText());
        assertStatistics(asset);
    }

    public void testStatisticsSidecar() throws Exception {
        File aux = new File(elevation.getAbsolutePath() + ".aux.xml");
        FileUtilities.writeFile("<PAMDataset>\n  <PAMRasterBand band=\"1\">\n    <Metadata>\n"
                + "      <MDI key=\"STATISTICS_MAXIMUM\">100</MDI>\n      <MDI key=\"STATISTICS_MEAN\">50</MDI>\n"
                + "      <MDI key=\"STATISTICS_MINIMUM\">1</MDI>\n      <MDI key=\"STATISTICS_STDDEV\">3</MDI>\n"
                + "    </Metadata>\n  </PAMRasterBand>\n  <PAMRasterBand band=\"2\">\n    <Metadata>\n"
                + "      <MDI key=\"STATISTICS_MINIMUM\">-7</MDI>\n    </Metadata>\n  </PAMRasterBand>\n</PAMDataset>\n", aux);
        RasterStats stats = RasterStats.read(elevation);
        assertEquals(1.0, stats.minimum, 1e-9);
        assertEquals(100.0, stats.maximum, 1e-9);
        assertNull(stats.validPercent);
        assertNull(RasterStats.readAsciiTag(aux, 42112));
    }

    public void testShapefileAndGeopackage() throws Exception {
        GeometryFactory factory = new GeometryFactory();
        SimpleFeatureCollection points = FeatureUtilities.featureCollectionFromGeometry(
                HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"), factory.createPoint(new Coordinate(500000, 5000000)),
                factory.createPoint(new Coordinate(510000, 5010000)), factory.createPoint(new Coordinate(505000, 5005000)));
        for( String name : new String[]{"points.shp", "points.gpkg"} ) {
            File file = new File(workspace, "data/" + name);
            OmsVectorWriter.writeVector(file.getAbsolutePath() + (name.endsWith(".gpkg") ? "#points" : ""), points);
            StacFiles.Description description = StacFiles.describe(file, name);
            JsonNode asset = description.asset;
            assertEquals(name, "EPSG:32632", asset.get("proj:code").asText());
            assertEquals(name, 3, asset.at("/hm:layers/0/count").asInt());
            assertEquals(name, 500000.0, asset.at("/proj:bbox/0").asDouble(), 1e-6);
            assertEquals(name, 5010000.0, asset.at("/proj:bbox/3").asDouble(), 1e-6);
            // UTM 32N central meridian at 9 degrees, about 45 north
            Envelope envelope = description.footprint.getEnvelopeInternal();
            assertEquals(name, 9.0, envelope.getMinX(), 0.01);
            assertEquals(name, 45.15, envelope.getMinY(), 0.05);
        }
        File shapefile = new File(workspace, "data/points.shp");
        assertTrue(StacFiles.isShapefileSidecar(new File(workspace, "data/points.dbf")));
        assertFalse(StacFiles.isShapefileSidecar(new File(workspace, "data/points.gpkg")));
        JsonNode asset = StacFiles.describe(shapefile, "points.shp").asset;
        assertTrue(asset.get("hm:sidecars").toString(), asset.get("hm:sidecars").toString().contains("points.dbf"));
        long size = 0;
        for( File file : new File(workspace, "data").listFiles() ) {
            if (file.getName().startsWith("points.") && !file.getName().endsWith(".gpkg")) {
                size += file.length();
            }
        }
        assertEquals(size, asset.get("file:size").asLong());
    }

    public void testVectorsAndOthers() throws Exception {
        File geojson = new File(workspace, "data/points.geojson");
        FileUtilities.writeFile("{\"type\": \"FeatureCollection\", \"features\": ["
                + "{\"type\": \"Feature\", \"properties\": {\"name\": \"a\"}, \"geometry\": {\"type\": \"Point\", \"coordinates\": [11.0, 46.0]}},"
                + "{\"type\": \"Feature\", \"properties\": {\"name\": \"b\"}, \"geometry\": {\"type\": \"Point\", \"coordinates\": [11.5, 46.5]}}"
                + "]}", geojson);
        StacFiles.Description description = StacFiles.describe(geojson, "points.geojson");
        assertEquals("application/geo+json", description.asset.get("type").asText());
        assertEquals(description.asset.toString(), 2, description.asset.at("/hm:layers/0/count").asInt());
        Envelope envelope = description.footprint.getEnvelopeInternal();
        assertEquals(11.0, envelope.getMinX(), 1e-9);
        assertEquals(46.5, envelope.getMaxY(), 1e-9);

        File text = new File(workspace, "data/notes.txt");
        FileUtilities.writeFile("not geographic", text);
        description = StacFiles.describe(text, "notes.txt");
        assertNull(description.footprint);
        assertEquals("text/plain", description.asset.get("type").asText());
        assertEquals(text.length(), description.asset.get("file:size").asLong());

        // a broken raster is still an asset
        File broken = new File(workspace, "data/broken.tif");
        FileUtilities.writeFile("not a tiff", broken);
        description = StacFiles.describe(broken, "broken.tif");
        assertNull(description.footprint);
        assertFalse(description.asset.has("proj:code"));
    }
}
