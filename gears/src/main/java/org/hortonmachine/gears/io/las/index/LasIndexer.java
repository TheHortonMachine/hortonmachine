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
package org.hortonmachine.gears.io.las.index;

import static java.lang.Math.round;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.index.strtree.STRtreeJGT;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Polygon;

@Description("Creates indexes for Las files.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, lidar")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasindexer")
@Status(5)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class LasIndexer extends HMModel {

    public static final String INDEX_LASFOLDER = "index.lasfolder";

    @Description("The folder containing the las files to index.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description("The name for the main index file.")
    @In
    public String pIndexname = INDEX_LASFOLDER;

    @Description("The optional code defining the target coordinate reference system. This is needed only if the file has no prj file. If set, it will be used over the prj file.")
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The size of the cells into which to split the las file for indexing (in units defined by the projection).")
    @In
    public double pCellsize = 5;

    @Description("Create overview shapefile (this creates a convexhull of the points).")
    @In
    public boolean doOverview = false;

    @Description("The number of threads to use for the process.")
    @In
    public int pThreads = 1;

    private CoordinateReferenceSystem crs;
    private ConcurrentLinkedQueue<Polygon> envelopesQueue;

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, pIndexname);

        if (pCellsize <= 0) {
            throw new ModelsIllegalargumentException("The cell size parameter needs to be > 0.", this);
        }

        if (!new File(inFolder).exists()) {
            throw new ModelsIllegalargumentException("The inFolder parameter has to be valid.", this);
        }

        try {
            if (pCode != null)
                crs = CrsUtilities.getCrsFromEpsg(pCode, null);
        } catch (Exception e1) {
            throw new ModelsIllegalargumentException(
                    "An error occurred while reading the projection definition: " + e1.getLocalizedMessage(), this);
        }

        pm.message("Las files to be added to the index:");
        OmsFileIterator iter = new OmsFileIterator();
        iter.inFolder = inFolder;
        iter.fileFilter = new FileFilter(){
            public boolean accept( File file ) {
                String name = file.getName();
                if (name.endsWith("indexed.las")) {
                    return false;
                }
                boolean isLas = name.toLowerCase().endsWith(".las");
                if (isLas) {
                    pm.message("   " + name);
                }
                return isLas;
            }
        };
        iter.process();

        List<File> filesList = iter.filesList;
        pm.beginTask("Creating readers index...", filesList.size());
        STRtreeJGT mainTree = new STRtreeJGT();
        for( File file : filesList ) {
            try (ALasReader reader = ALasReader.getReader(file, crs)) {
                reader.open();
                ILasHeader header = reader.getHeader();
                if (crs == null) {
                    crs = header.getCrs();
                }
                ReferencedEnvelope3D envelope = header.getDataEnvelope();
                File newLasFile = getNewLasFile(file);
                mainTree.insert(envelope, newLasFile.getName());
            }
            pm.worked(1);
        }
        pm.done();

        File mainIndex = new File(inFolder, pIndexname);
        byte[] mainIndexBytes = serialize(mainTree);
        dumpBytes(mainIndex, mainIndexBytes);

        // write prj file
        CrsUtilities.writeProjectionFile(mainIndex.getAbsolutePath(), "lasfolder", crs);

        /*
         * now the single files
         */
        if (doOverview)
            envelopesQueue = new ConcurrentLinkedQueue<>();
        if (pThreads > 1) {
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(pThreads);
            for( final File file : filesList ) {
                Runnable runner = new Runnable(){
                    public void run() {
                        try {
                            processFile(file, true);
                        } catch (Exception e) {
                            pm.errorMessage("Problems indexing file: " + file.getName());
                            e.printStackTrace();
                        }
                    }
                };
                fixedThreadPool.execute(runner);
            }
            try {
                fixedThreadPool.shutdown();
                fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
                fixedThreadPool.shutdownNow();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            for( final File file : filesList ) {
                processFile(file, false);
            }
        }

        if (doOverview) {
            File overviewFile = FileUtilities.substituteExtention(mainIndex, "shp");
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("overview");
            b.setCRS(crs);
            b.add("the_geom", Polygon.class);
            b.add("file", String.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            DefaultFeatureCollection overviewFC = new DefaultFeatureCollection();

            for( Polygon polygon : envelopesQueue ) {
                Object[] values = new Object[]{polygon, polygon.getUserData()};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                overviewFC.add(feature);
            }
            dumpVector(overviewFC, overviewFile.getAbsolutePath());
        }
    }

    @SuppressWarnings("unchecked")
    private void processFile( File file, boolean isMultiThreaded ) throws Exception {
        String name = file.getName();
        File newLasFile = getNewLasFile(file);
        File indexFile = getNetIndexFile(file);
        if (indexFile.exists() && newLasFile.exists()) {
            pm.message("Index existing already for file: " + name);
            return;
        }
        if (indexFile.exists() || newLasFile.exists()) {
            indexFile.delete();
            newLasFile.delete();
        }
        pm.message("Processing file: " + name);

        /*
         * create also a bounds geometry.
         * The geometry is calculated form the most external 
         * points in the 4 directions.
         */
        CoordinateList pointsList = new CoordinateList();

        try (ALasReader reader = ALasReader.getReader(file, crs)) {
            reader.open();
            ILasHeader header = reader.getHeader();
            long recordsCount = header.getRecordsCount();
            if (recordsCount == 0) {
                pm.errorMessage("No points found in: " + name);
                return;
            }
            ReferencedEnvelope3D envelope = header.getDataEnvelope();
            ReferencedEnvelope env2d = new ReferencedEnvelope(envelope);
            Envelope2D e = new Envelope2D(env2d);

            double north = e.getMaxY();
            double south = e.getMinY();
            double east = e.getMaxX();
            double west = e.getMinX();
            int cols = (int) round(e.getWidth() / pCellsize);
            int rows = (int) round(e.getHeight() / pCellsize);
            double xRes = e.getWidth() / cols;
            double yRes = e.getHeight() / rows;

            /*
             * expand of half resolution an recalculate to avoid problems
             * with points on the boundary 
             */
            north = north + yRes / 2.0;
            south = south - yRes / 2.0;
            west = west - xRes / 2.0;
            east = east + xRes / 2.0;
            double width = east - west;
            double height = north - south;
            cols = (int) round(width / pCellsize);
            rows = (int) round(height / pCellsize);
            xRes = width / cols;
            yRes = height / rows;

            pm.message("Splitting " + name + " into tiles of " + (float) xRes + " x " + (float) yRes + ".");
            GridGeometry2D gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west, cols, rows,
                    reader.getHeader().getCrs());

            List<LasRecord>[][] dotOnMatrix = new ArrayList[cols][rows];
            if (!isMultiThreaded) {
                pm.beginTask("Sorting points for " + name, (int) recordsCount);
            } else {
                pm.message("Sorting points for " + name + "...");
            }
            while( reader.hasNextPoint() ) {
                LasRecord dot = reader.getNextPoint();
                DirectPosition wPoint = new DirectPosition2D(dot.x, dot.y);
                GridCoordinates2D gridCoord = gridGeometry.worldToGrid(wPoint);
                int x = gridCoord.x;
                int y = gridCoord.y;
                if (dotOnMatrix[x][y] == null) {
                    dotOnMatrix[x][y] = new ArrayList<>();
                }
                dotOnMatrix[x][y].add(dot);
                if (doOverview) {
                    pointsList.add(new Coordinate(dot.x, dot.y));
                }
                if (!isMultiThreaded)
                    pm.worked(1);
            }
            if (!isMultiThreaded)
                pm.done();

            /*
             * now write indexed file plus index
             */
            try (ALasWriter writer = ALasWriter.getWriter(newLasFile, reader.getHeader().getCrs())) {
                writer.setBounds(reader.getHeader());
                writer.open();

                int addedTiles = 0;
                STRtreeJGT tree = new STRtreeJGT();
                if (!isMultiThreaded) {
                    pm.beginTask("Write and index new las...", cols);
                } else {
                    pm.message("Write and index new las...");
                }
                long pointCount = 0;
                for( int r = 0; r < rows; r++ ) {
                    for( int c = 0; c < cols; c++ ) {
                        List<LasRecord> dotsList = dotOnMatrix[c][r];
                        if (dotsList == null || dotsList.size() == 0) {
                            continue;
                        }
                        Coordinate coord = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
                        Envelope env = new Envelope(coord);
                        env.expandBy(xRes / 2.0, yRes / 2.0);
                        long tmpCount = pointCount;
                        double avgElevValue = 0.0;
                        double avgIntensityValue = 0.0;
                        int count = 0;

                        for( LasRecord dot : dotsList ) {
                            writer.addPoint(dot);
                            pointCount++;
                            avgElevValue += dot.z;
                            avgIntensityValue += dot.intensity;
                            count++;
                        }
                        avgElevValue /= count;
                        avgIntensityValue /= count;
                        tree.insert(env, new double[]{tmpCount, pointCount, avgElevValue, avgIntensityValue});
                        addedTiles++;
                    }
                    if (!isMultiThreaded)
                        pm.worked(1);
                }
                if (!isMultiThreaded)
                    pm.done();

                byte[] serialized = serialize(tree);
                dumpBytes(indexFile, serialized);

                pm.message("Tiles added for " + name + ": " + addedTiles);
            }
        }
        if (doOverview) {
            pm.message("Create overview for " + name);
            MultiPoint multiPoint = gf.createMultiPoint(pointsList.toCoordinateArray());
            Geometry polygon = multiPoint.convexHull();
            polygon.setUserData(name);
            envelopesQueue.add((Polygon) polygon);
        }

    }

    private File getNetIndexFile( File file ) {
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(file);
        File indexFile = new File(file.getParentFile(), nameWithoutExtention + "_indexed.lasfix");
        return indexFile;
    }

    private File getNewLasFile( File file ) {
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(file);
        File newLasFile = new File(file.getParentFile(), nameWithoutExtention + "_indexed.las");
        return newLasFile;
    }

    public static Polygon envelopeToPolygon( Envelope envelope ) {
        double w = envelope.getMinX();
        double e = envelope.getMaxX();
        double s = envelope.getMinY();
        double n = envelope.getMaxY();

        Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(w, n);
        coords[1] = new Coordinate(e, n);
        coords[2] = new Coordinate(e, s);
        coords[3] = new Coordinate(w, s);
        coords[4] = new Coordinate(w, n);

        GeometryFactory gf = GeometryUtilities.gf();
        LinearRing linearRing = gf.createLinearRing(coords);
        Polygon polygon = gf.createPolygon(linearRing, null);
        return polygon;
    }

    @Finalize
    public void close() throws Exception {
    }

    private static byte[] serialize( Object obj ) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.close();
            return bos.toByteArray();
        }
    }

    private static void dumpBytes( File file, byte[] bytes ) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.write(bytes);
        }
    }

}
