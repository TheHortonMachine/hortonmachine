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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.lesto.modules.utilities.LasElevationHandler;
import org.hortonmachine.lesto.modules.utilities.LasFromFaroTlsXyz;
import org.hortonmachine.lesto.modules.utilities.LasMover;
import org.hortonmachine.lesto.modules.utilities.LasOverviewCreator;
import org.hortonmachine.lesto.modules.utilities.LasPointDensityExtractor;
import org.hortonmachine.lesto.modules.utilities.LasSlicer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import junit.framework.TestCase;

/**
 * Tests the lidar utility modules on synthetic surveys.
 */
public class TestLestoUtilities extends TestCase {

    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    private static final double GROUND = 100.0;

    private CoordinateReferenceSystem crs;
    private File folder;

    @Override
    protected void setUp() throws Exception {
        crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632", true);
        folder = File.createTempFile("hm_lesto_utilities_", "");
        folder.delete();
        folder.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(folder);
    }

    /**
     * The normalized points are their heights over the dtm, the ones outside of it are left out,
     * and the header has the bounds of the heights.
     */
    public void testElevationHandler() throws Exception {
        List<LasRecord> points = new ArrayList<>();
        points.add(point(2.5, 2.5, GROUND + 3.0, 1));
        points.add(point(7.5, 7.5, GROUND + 12.0, 1));
        // outside of the dtm of 10 m
        points.add(point(15.5, 5.5, GROUND + 5.0, 1));
        File las = writeLas("survey.las", points);
        File dtm = writeDem("dtm.tif", 10, 1.0, GROUND);

        LasElevationHandler handler = new LasElevationHandler();
        handler.inFile = las.getAbsolutePath();
        handler.inDtm = dtm.getAbsolutePath();
        handler.outFile = new File(folder, "heights.las").getAbsolutePath();
        handler.process();

        List<LasRecord> heights = readLas(new File(handler.outFile));
        assertEquals(2, heights.size());
        assertEquals(3.0, heights.get(0).z, 0.001);
        assertEquals(12.0, heights.get(1).z, 0.001);
        ReferencedEnvelope3D envelope = envelope(new File(handler.outFile));
        assertEquals(3.0, envelope.getMinZ(), 0.001);
        assertEquals(12.0, envelope.getMaxZ(), 0.001);

        String heightsFile = handler.outFile;
        handler = new LasElevationHandler();
        handler.inFile = heightsFile;
        handler.inDtm = dtm.getAbsolutePath();
        handler.doAdd = true;
        handler.outFile = new File(folder, "back.las").getAbsolutePath();
        handler.process();
        assertEquals(GROUND + 12.0, readLas(new File(handler.outFile)).get(1).z, 0.001);
    }

    /**
     * The points and the bounds move by the deltas, also by a distance larger than the range of
     * the scaled integers from the offset of the input.
     */
    public void testMover() throws Exception {
        List<LasRecord> points = new ArrayList<>();
        points.add(point(2.5, 3.5, GROUND, 1));
        points.add(point(7.5, 1.5, GROUND + 10, 1));
        File las = writeLas("survey.las", points);

        LasMover mover = new LasMover();
        mover.inFile = las.getAbsolutePath();
        // more than the 21474 km of the integers with the scale of 0.01
        mover.pDeltaX = 30000000.0;
        mover.pDeltaY = 5.0;
        mover.pDeltaZ = -0.35;
        mover.outFile = new File(folder, "moved.las").getAbsolutePath();
        mover.process();

        List<LasRecord> moved = readLas(new File(mover.outFile));
        assertEquals(30000002.5, moved.get(0).x, 0.001);
        assertEquals(8.5, moved.get(0).y, 0.001);
        assertEquals(GROUND - 0.35, moved.get(0).z, 0.001);
        ReferencedEnvelope3D envelope = envelope(new File(mover.outFile));
        assertEquals(30000002.5, envelope.getMinX(), 0.001);
        assertEquals(30000007.5, envelope.getMaxX(), 0.001);
        assertEquals(GROUND + 9.65, envelope.getMaxZ(), 0.001);
    }

    /**
     * The cells count the points of each return, also those on the north and east bounds and
     * those with a return number out of 1 to 5, that count only in the total.
     */
    public void testPointDensity() throws Exception {
        List<LasRecord> points = new ArrayList<>();
        points.add(point(0.0, 0.0, GROUND, 1));
        points.add(point(1.0, 1.0, GROUND, 2));
        points.add(point(1.5, 1.5, GROUND, 2));
        // on the north east corner of the bounds
        points.add(point(4.0, 4.0, GROUND, 1));
        // returns not counted by return
        points.add(point(3.5, 3.5, GROUND, 0));
        points.add(point(3.5, 3.5, GROUND, 7));
        File las = writeLas("survey.las", points);

        LasPointDensityExtractor density = new LasPointDensityExtractor();
        density.inFile = las.getAbsolutePath();
        density.pGridStep = 2.0;
        density.outFile = new File(folder, "density.shp").getAbsolutePath();
        density.process();

        Map<String, SimpleFeature> cells = new HashMap<>();
        SimpleFeatureCollection fc = OmsVectorReader.readVector(density.outFile);
        try (SimpleFeatureIterator it = fc.features()) {
            while( it.hasNext() ) {
                SimpleFeature f = it.next();
                Envelope env = ((Geometry) f.getDefaultGeometry()).getEnvelopeInternal();
                cells.put((int) env.getMinX() + "/" + (int) env.getMinY(), f);
            }
        }
        assertEquals("only the cells with points", 2, cells.size());
        SimpleFeature southWest = cells.get("0/0");
        assertEquals(3, ((Number) southWest.getAttribute("total")).intValue());
        assertEquals(1, ((Number) southWest.getAttribute("imp1")).intValue());
        assertEquals(2, ((Number) southWest.getAttribute("imp2")).intValue());
        assertEquals(0.75, ((Number) southWest.getAttribute("density")).doubleValue(), 0.0001);
        SimpleFeature northEast = cells.get("2/2");
        assertEquals(3, ((Number) northEast.getAttribute("total")).intValue());
        assertEquals(1, ((Number) northEast.getAttribute("imp1")).intValue());
        assertEquals(0, ((Number) northEast.getAttribute("imp5")).intValue());
    }

    /**
     * The overview has the extent, the name and the number of points of each file of the folder.
     */
    public void testOverview() throws Exception {
        File lasFolder = new File(folder, "pieces");
        lasFolder.mkdirs();
        writeLas("pieces/a.las", Arrays.asList(point(0.5, 0.5, GROUND, 1), point(4.5, 2.5, GROUND, 1)));
        writeLas("pieces/b.las", Arrays.asList(point(10.5, 0.5, GROUND, 1), point(12.5, 6.5, GROUND, 1),
                point(11.5, 3.5, GROUND, 1)));

        LasOverviewCreator overview = new LasOverviewCreator();
        overview.inLas = lasFolder.getAbsolutePath();
        overview.outOverview = new File(folder, "overview.shp").getAbsolutePath();
        overview.process();

        SimpleFeatureCollection fc = OmsVectorReader.readVector(overview.outOverview);
        assertEquals(2, fc.size());
        try (SimpleFeatureIterator it = fc.features()) {
            while( it.hasNext() ) {
                SimpleFeature f = it.next();
                Envelope env = ((Geometry) f.getDefaultGeometry()).getEnvelopeInternal();
                if (f.getAttribute("file").equals("a.las")) {
                    assertEquals(2, ((Number) f.getAttribute("points")).intValue());
                    assertEquals(new Envelope(0.5, 4.5, 0.5, 2.5), env);
                } else {
                    assertEquals("b.las", f.getAttribute("file"));
                    assertEquals(3, ((Number) f.getAttribute("points")).intValue());
                    assertEquals(new Envelope(10.5, 12.5, 0.5, 6.5), env);
                }
            }
        }
    }

    /**
     * The slices are at the multiples of the interval over the ground, named after them, and
     * have the center of the slice where their points are.
     */
    public void testSlicer() throws Exception {
        List<LasRecord> points = new ArrayList<>();
        // a ground point, left out by the threshold, and two points in different slices
        points.add(point(0.5, 0.5, GROUND + 0.1, 1));
        points.add(point(2.5, 2.5, GROUND + 5.2, 1));
        points.add(point(9.75, 9.75, GROUND + 10.3, 1));
        File las = writeLas("survey.las", points);
        File dtm = writeDem("dtm.tif", 10, 1.0, GROUND);

        LasSlicer slicer = new LasSlicer();
        slicer.inLas = las.getAbsolutePath();
        slicer.inDtm = dtm.getAbsolutePath();
        slicer.pInterval = 5.0;
        slicer.pThickness = 1.0;
        slicer.pGroundThreshold = 0.5;
        slicer.pResolution = 1.0;
        slicer.outFolder = new File(folder, "slices").getAbsolutePath();
        slicer.process();

        File[] slices = new File(slicer.outFolder).listFiles();
        Arrays.sort(slices);
        assertEquals(2, slices.length);
        assertEquals("slice_10.tif", slices[0].getName());
        assertEquals("slice_5.tif", slices[1].getName());
        try (HMRaster slice = HMRaster.fromFile(slices[1].getAbsolutePath())) {
            assertEquals(5.0, slice.getValue(new Coordinate(2.5, 2.5)), 0.0);
            assertTrue(slice.isNovalue(slice.getValue(new Coordinate(5.0, 5.0))));
        }
        try (HMRaster slice = HMRaster.fromFile(slices[0].getAbsolutePath())) {
            // the point on the north east bound of the data is in the last cell
            assertEquals(10.0, slice.getValue(slice.getCols() - 1, 0), 0.0);
            assertTrue(slice.isNovalue(slice.getValue(new Coordinate(2.5, 2.5))));
        }
    }

    /**
     * The scan points are moved by the position of the scanner, the far ones are left out, and
     * they are single returns with their colors.
     */
    public void testFaroTlsXyz() throws Exception {
        File xyz = new File(folder, "scan.xyz");
        FileUtilities.writeFile("""
                0 0 -1.5 2.0 1190.5 255 128 0
                0 1 3.0 -4.0 1191.25 10 20 30

                1 0 30.0 0.0 1200.0 0 0 0
                """, xyz);

        LasFromFaroTlsXyz converter = new LasFromFaroTlsXyz();
        converter.inFile = xyz.getAbsolutePath();
        converter.pLongitude = 681270.0;
        converter.pLatitude = 5127117.0;
        converter.pMaxDistance = 15.0;
        converter.pCode = "EPSG:32632";
        converter.outLas = new File(folder, "scan.las").getAbsolutePath();
        converter.process();

        List<LasRecord> points = readLas(new File(converter.outLas));
        assertEquals("the point 30 m away is left out", 2, points.size());
        LasRecord first = points.get(0);
        assertEquals(681268.5, first.x, 0.001);
        assertEquals(5127119.0, first.y, 0.001);
        assertEquals(1190.5, first.z, 0.001);
        assertEquals(255, first.color[0]);
        assertEquals(128, first.color[1]);
        assertEquals(0, first.color[2]);
        assertEquals(1, first.returnNumber);
        assertEquals(1, first.numberOfReturns);
        assertEquals(0, first.classification);
        assertTrue(new File(folder, "scan.prj").exists());
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

    /**
     * Writes the points with bounds that are not theirs: the writer sets the right ones.
     */
    private File writeLas( String name, List<LasRecord> points ) throws Exception {
        File lasFile = new File(folder, name);
        try (ALasWriter writer = ALasWriter.getWriter(lasFile, crs)) {
            writer.setBounds(0, 1, 0, 1, 0, 1);
            writer.open();
            for( LasRecord point : points ) {
                writer.addPoint(point);
            }
        }
        return lasFile;
    }

    private static List<LasRecord> readLas( File lasFile ) throws Exception {
        List<LasRecord> points = new ArrayList<>();
        try (ALasReader reader = ALasReader.getReader(lasFile, null)) {
            reader.open();
            while( reader.hasNextPoint() ) {
                points.add(reader.getNextPoint());
            }
        }
        return points;
    }

    private static ReferencedEnvelope3D envelope( File lasFile ) throws Exception {
        try (ALasReader reader = ALasReader.getReader(lasFile, null)) {
            reader.open();
            return reader.getHeader().getDataEnvelope();
        }
    }

    private File writeDem( String name, int cells, double res, double elevation ) throws Exception {
        double[][] data = new double[cells][cells];
        for( double[] row : data ) {
            Arrays.fill(row, elevation);
        }
        RegionMap region = RegionMap.fromBoundsAndGrid(0, cells * res, 0, cells * res, cells, cells);
        File demFile = new File(folder, name);
        OmsRasterWriter.writeRaster(demFile.getAbsolutePath(), CoverageUtilities.buildCoverage("dem", data, region, crs, true));
        return demFile;
    }
}
