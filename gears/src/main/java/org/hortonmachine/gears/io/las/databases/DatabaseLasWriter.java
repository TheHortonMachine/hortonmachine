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
package org.hortonmachine.gears.io.las.databases;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.gears.utils.time.EggClock;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

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

@Description("Inserts las files into a spatialite database.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, lidar, spatialite")
@Label(HMConstants.LESTO + "/utilities")
@Name("spatialitelaswriter")
@Status(5)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DatabaseLasWriter extends HMModel {

    @Description("The folder containing the las files to index.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description("The output database path.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inDatabasePath;

    @Description("The database to use")
    @UI("combo:" + "SPATIALITE" + "," + "H2GIS")
    @In
    public String pDbType = EDb.SPATIALITE.name();

    @Description("The optional image mosaic of ortophoto to take the color from (has to be 3-band).")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inOrtophoto;

    @Description("The optional code defining the target coordinate reference system. This is needed only if the file has no prj file. If set, it will be used over the prj file.")
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The size of the cells into which to split the las file for indexing (in units defined by the projection).")
    @In
    public double pCellsize = 3;

    @Description("Number of data summary levels to add.")
    @In
    public int pLevels = 2;

    @Description("The size multiplication factor to use for subsequent levels.")
    @In
    public int pFactor = 5;

    @Description("Flag to define if empty cells should also be written into the database.")
    @In
    public boolean doEmptyCells = false;

    @Description("Flag to define if the spatial index creation should be ignored (will not create levels).")
    @In
    public boolean doAvoidIndex = false;

    @Description("Optional las list names to process only those (inside las folder).")
    @In
    public List<String> inLasNames;

    private CoordinateReferenceSystem crs;

    private int srid = -9999;

    private double ortoXRes;
    private double ortoYRes;

    private ImageMosaicReader ortoReader;

    public boolean doVerbose = true;
    private static final String INTERRUPTED_BY_USER = "Interrupted by user.";

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, inDatabasePath);

        if (pCellsize <= 0) {
            throw new ModelsIllegalargumentException("The cell size parameter needs to be > 0.", this);
        }

        if (!new File(inFolder).exists()) {
            throw new ModelsIllegalargumentException("The inFolder parameter has to be valid.", this);
        }

        if (doAvoidIndex) {
            pLevels = 0;
        }

        if (inOrtophoto != null) {
            File ortoFile = new File(inOrtophoto);
            if (ortoFile.exists()) {
                URL imageMosaicUrl = ortoFile.toURI().toURL();
                final AbstractGridFormat imageMosaicFormat = (AbstractGridFormat) GridFormatFinder.findFormat(imageMosaicUrl);
                ortoReader = (ImageMosaicReader) imageMosaicFormat.getReader(imageMosaicUrl);
                File propertiesFile = FileUtilities.substituteExtention(ortoFile, "properties");
                HashMap<String, String> propertiesMap = FileUtilities.readFileToHashMap(propertiesFile.getAbsolutePath(), null,
                        false);

                String xyREs = propertiesMap.get("Levels");
                String[] split = xyREs.split(",");
                ortoXRes = Double.parseDouble(split[0]);
                ortoYRes = Double.parseDouble(split[1]);
            }
        }

        EDb edb = EDb.SPATIALITE;
        if (pDbType.equals(EDb.H2GIS.name())) {
            edb = EDb.H2GIS;
        }
        // try (ASpatialDb spatialiteDb = new GTSpatialiteThreadsafeDb()) {
        try (ASpatialDb spatialiteDb = edb.getSpatialDb()) {
            boolean existed = spatialiteDb.open(inDatabasePath);
            if (!existed) {
                pm.beginTask("Create new spatialite database...", IHMProgressMonitor.UNKNOWN);
                spatialiteDb.initSpatialMetadata(null);
                pm.done();
            }

            try {
                if (pCode != null) {
                    crs = CrsUtilities.getCrsFromEpsg(pCode, null);
                } else {
                    File folderFile = new File(inFolder);
                    File[] prjFiles = folderFile.listFiles(new FilenameFilter(){
                        @Override
                        public boolean accept( File dir, String name ) {
                            return name.toLowerCase().endsWith(".prj");
                        }
                    });

                    if (prjFiles != null && prjFiles.length > 0) {
                        crs = CrsUtilities.readProjectionFile(prjFiles[0].getAbsolutePath(), null);
                        pCode = CrsUtilities.getCodeFromCrs(crs);
                    }
                }
                if (pCode != null) {
                    pCode = pCode.replaceFirst("EPSG:", "");
                    srid = Integer.parseInt(pCode);
                }
            } catch (Exception e1) {
                throw new ModelsIllegalargumentException(
                        "An error occurred while reading the projection definition: " + e1.getLocalizedMessage(), this);
            }

            if (srid == -9999) {
                srid = 4326;
                pm.errorMessage("No crs has been defined. Setting it to 4326 by default.");
            }

            LasSourcesTable.createTable(spatialiteDb, srid, doAvoidIndex);
            LasCellsTable.createTable(spatialiteDb, srid, doAvoidIndex);

            pm.message("Las files to be added to the index:");
            List<File> filesList;
            if (inLasNames == null) {
                OmsFileIterator iter = new OmsFileIterator();
                iter.inFolder = inFolder;
                iter.fileFilter = new FileFilter(){
                    public boolean accept( File file ) {
                        String name = file.getName();
                        boolean isLas = name.toLowerCase().endsWith(".las") && !name.toLowerCase().endsWith("indexed.las");
                        if (isLas) {
                            pm.message("   " + name);
                        }
                        return isLas;
                    }
                };
                iter.process();

                filesList = iter.filesList;
            } else {
                filesList = new ArrayList<>();
                for( String lasName : inLasNames ) {
                    File lasFile = new File(inFolder + File.separator + lasName);
                    filesList.add(lasFile);
                }
            }
            List<LasSource> lasSources = LasSourcesTable.getLasSources(spatialiteDb);
            List<String> existingLasSourcesNames = new ArrayList<String>();
            for( LasSource lasSource : lasSources ) {
                existingLasSourcesNames.add(lasSource.name);
            }
            for( File lasFile : filesList ) {
                if (pm.isCanceled()) {
                    return;
                }
                String lasName = FileUtilities.getNameWithoutExtention(lasFile);
                if (existingLasSourcesNames.contains(lasName)) {
                    pm.errorMessage("Not inserting already existing file in database: " + lasName);
                    continue;
                }
                ReferencedEnvelope3D envelope;
                try (ALasReader reader = ALasReader.getReader(lasFile, crs)) {
                    reader.open();
                    ILasHeader header = reader.getHeader();
                    envelope = header.getDataEnvelope();
                }
                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(envelope);

                GridCoverage2D ortoGC = null;
                if (ortoReader != null) {
                    double west = envelope.getMinX();
                    double east = envelope.getMaxX();
                    double south = envelope.getMinY();
                    double north = envelope.getMaxY();
                    GeneralParameterValue[] readGeneralParameterValues = CoverageUtilities
                            .createGridGeometryGeneralParameter(ortoXRes, ortoYRes, north, south, east, west, crs);
                    ortoGC = ortoReader.read(readGeneralParameterValues);
                }

                long id = LasSourcesTable.insertLasSource(spatialiteDb, srid, pLevels, pCellsize, pFactor, polygon, lasName,
                        envelope.getMinZ(), envelope.getMaxZ(), 0, 0);
                processFile(spatialiteDb, lasFile, id, ortoGC);
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void processFile( final ASpatialDb spatialiteDb, File file, long sourceID, GridCoverage2D ortoGC ) throws Exception {
        String name = file.getName();
        pm.message("Processing file: " + name);

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

            double[] xRanges = NumericsUtilities.range2Bins(west, east, pCellsize, false);
            double[] yRanges = NumericsUtilities.range2Bins(south, north, pCellsize, false);
            int cols = xRanges.length - 1;
            int rows = yRanges.length - 1;
            int tilesCount = cols * rows;

            pm.message("Splitting " + name + " into " + tilesCount + " tiles.");
            GridGeometry2D gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west, cols, rows,
                    reader.getHeader().getCrs());

            List<LasRecord>[][] dotOnMatrixXY = new ArrayList[cols][rows];
            LasCell[][] lasCellsOnMatrixXY = new LasCell[cols][rows];
            if (doVerbose)
                pm.beginTask("Sorting points for " + name, (int) recordsCount);
            long readCount = 0;

            short minIntens = Short.MAX_VALUE;
            short maxIntens = -Short.MAX_VALUE;
            while( reader.hasNextPoint() ) {
                LasRecord dot = reader.getNextPoint();
                minIntens = (short) Math.min(minIntens, dot.intensity);
                maxIntens = (short) Math.max(maxIntens, dot.intensity);
                DirectPosition wPoint = new DirectPosition2D(dot.x, dot.y);
                GridCoordinates2D gridCoord = gridGeometry.worldToGrid(wPoint);
                int x = gridCoord.x;
                if (x < 0)
                    x = 0;
                if (x > cols - 1)
                    x = cols - 1;
                int y = gridCoord.y;
                if (y < 0)
                    y = 0;
                if (y > rows - 1)
                    y = rows - 1;
                if (dotOnMatrixXY[x][y] == null) {
                    dotOnMatrixXY[x][y] = new ArrayList<>();
                }
                dotOnMatrixXY[x][y].add(dot);
                if (doVerbose)
                    pm.worked(1);
                readCount++;
            }
            if (doVerbose)
                pm.done();
            if (readCount != recordsCount) {
                throw new RuntimeException("Didn't read all the data...");
            }

            if (pm.isCanceled()) {
                throw new RuntimeException(INTERRUPTED_BY_USER);
            }

            LasSourcesTable.updateMinMaxIntensity(spatialiteDb, sourceID, minIntens, maxIntens);

            ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
            List<LasCell> cellsList = new ArrayList<>();
            final Point2D.Double pos = new Point2D.Double();
            final int[] ortoValues = new int[3];
            if (doVerbose)
                pm.beginTask("Write las data...", cols * rows);
            else
                pm.message("Write las data...");
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    List<LasRecord> dotsList = dotOnMatrixXY[c][r];

                    Coordinate coord = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
                    Envelope env = new Envelope(coord);
                    env.expandBy(pCellsize / 2.0, pCellsize / 2.0);
                    Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(env);

                    if (dotsList == null || dotsList.size() == 0) {
                        if (doEmptyCells) {
                            final LasCell lasCell = new LasCell();
                            lasCell.polygon = polygon;
                            lasCell.sourceId = sourceID;

                            lasCell.pointsCount = 0;
                            lasCell.avgElev = -9999.0;
                            lasCell.minElev = -9999.0;
                            lasCell.maxElev = -9999.0;
                            lasCell.xyzs = new byte[0];
                            lasCell.avgIntensity = (short) -999;
                            lasCell.minIntensity = (short) -999;
                            lasCell.maxIntensity = (short) -999;
                            lasCell.intensitiesClassifications = new byte[0];
                            lasCell.returns = new byte[0];
                            lasCell.minGpsTime = -9999.0;
                            lasCell.maxGpsTime = -9999.0;
                            lasCell.gpsTimes = new byte[0];
                            lasCell.colors = new byte[0];

                            cellsList.add(lasCell);
                        }
                        continue;
                    }
                    int pointCount = dotsList.size();

                    double avgElev = 0.0;
                    double minElev = Double.POSITIVE_INFINITY;
                    double maxElev = Double.NEGATIVE_INFINITY;
                    byte[] position = new byte[8 * 3 * pointCount];
                    ByteBuffer positionBuffer = ByteBuffer.wrap(position);

                    double avgIntensity = 0.0;

                    short minIntensity = 30000;
                    short maxIntensity = -1;
                    byte[] intensClass = new byte[2 * 2 * pointCount];
                    ByteBuffer intensClassBuffer = ByteBuffer.wrap(intensClass);
                    byte[] returns = new byte[2 * 2 * pointCount];
                    ByteBuffer returnsBuffer = ByteBuffer.wrap(returns);
                    double minGpsTime = Double.POSITIVE_INFINITY;
                    double maxGpsTime = Double.NEGATIVE_INFINITY;
                    byte[] gpsTimes = new byte[8 * pointCount];
                    ByteBuffer gpsTimesBuffer = ByteBuffer.wrap(gpsTimes);
                    byte[] colors = new byte[2 * 3 * pointCount];
                    ByteBuffer colorsBuffer = ByteBuffer.wrap(colors);

                    int count = 0;

                    for( LasRecord dot : dotsList ) {
                        avgElev += dot.z;
                        minElev = min(dot.z, minElev);
                        maxElev = max(dot.z, maxElev);
                        positionBuffer.putDouble(dot.x);
                        positionBuffer.putDouble(dot.y);
                        positionBuffer.putDouble(dot.z);

                        avgIntensity += dot.intensity;
                        minIntensity = (short) min(dot.intensity, minIntensity);
                        maxIntensity = (short) max(dot.intensity, maxIntensity);
                        intensClassBuffer.putShort(dot.intensity);
                        intensClassBuffer.putShort(dot.classification);

                        returnsBuffer.putShort(dot.returnNumber);
                        returnsBuffer.putShort(dot.numberOfReturns);

                        minGpsTime = min(dot.gpsTime, minGpsTime);
                        maxGpsTime = max(dot.gpsTime, maxGpsTime);

                        gpsTimesBuffer.putDouble(dot.gpsTime);

                        if (ortoGC != null) {
                            pos.setLocation(dot.x, dot.y);
                            try {
                                ortoGC.evaluate(pos, ortoValues);
                                colorsBuffer.putShort((short) ortoValues[0]);
                                colorsBuffer.putShort((short) ortoValues[1]);
                                colorsBuffer.putShort((short) ortoValues[2]);
                            } catch (PointOutsideCoverageException poce) {
                                // insert white
                                colorsBuffer.putShort((short) 255);
                                colorsBuffer.putShort((short) 255);
                                colorsBuffer.putShort((short) 255);
                            }

                        } else if (dot.color != null) {
                            colorsBuffer.putShort(dot.color[0]);
                            colorsBuffer.putShort(dot.color[1]);
                            colorsBuffer.putShort(dot.color[2]);
                        }

                        count++;
                    }
                    avgElev /= count;
                    avgIntensity /= count;

                    final LasCell lasCell = new LasCell();
                    lasCell.polygon = polygon;
                    lasCell.sourceId = sourceID;

                    lasCell.pointsCount = pointCount;
                    lasCell.avgElev = avgElev;
                    lasCell.minElev = minElev;
                    lasCell.maxElev = maxElev;
                    lasCell.xyzs = position;
                    lasCell.avgIntensity = (short) Math.round(avgIntensity);
                    lasCell.minIntensity = minIntensity;
                    lasCell.maxIntensity = maxIntensity;
                    lasCell.intensitiesClassifications = intensClass;
                    lasCell.returns = returns;
                    lasCell.minGpsTime = minGpsTime;
                    lasCell.maxGpsTime = maxGpsTime;
                    lasCell.gpsTimes = gpsTimes;
                    lasCell.colors = colors;

                    cellsList.add(lasCell);
                    lasCellsOnMatrixXY[c][r] = lasCell;

                    if (cellsList.size() > 100000) {
                        // add data to db in a thread to fasten up things
                        final List<LasCell> processCells = cellsList;
                        singleThreadExecutor.execute(new Runnable(){
                            public void run() {
                                try {
                                    LasCellsTable.insertLasCells(spatialiteDb, srid, processCells);
                                    pm.worked(processCells.size());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        cellsList = new ArrayList<>();
                    }
                }
                if (pm.isCanceled()) {
                    throw new RuntimeException(INTERRUPTED_BY_USER);
                }
            }
            if (cellsList.size() > 0) {
                // add data to db in a thread to fasten up things
                final List<LasCell> processCells = cellsList;
                singleThreadExecutor.execute(new Runnable(){
                    public void run() {
                        try {
                            LasCellsTable.insertLasCells(spatialiteDb, srid, processCells);
                            if (doVerbose)
                                pm.worked(processCells.size());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                cellsList = new ArrayList<>();
            }

            try {
                singleThreadExecutor.shutdown();
                singleThreadExecutor.awaitTermination(30, TimeUnit.DAYS);
                singleThreadExecutor.shutdownNow();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            if (doVerbose)
                pm.done();
            else
                pm.message("Done.");

            if (pLevels > 0) {
                for( int level = 1; level <= pLevels; level++ ) {
                    if (pm.isCanceled()) {
                        throw new RuntimeException(INTERRUPTED_BY_USER);
                    }
                    LasLevelsTable.createTable(spatialiteDb, srid, level, doAvoidIndex);
                    if (level == 1) {
                        insertFirstLevel(spatialiteDb, sourceID, north, south, east, west, level);
                    } else {
                        insertLevel(spatialiteDb, sourceID, north, south, east, west, level);
                    }
                }
            }

        }

    }

    private void insertFirstLevel( final ASpatialDb spatialiteDb, long sourceID, double north, double south, double east,
            double west, int level ) throws Exception, SQLException {
        List<LasLevel> levelsList = new ArrayList<>();
        double levelCellsize = pCellsize * level * pFactor;
        double[] xRangesLevel = NumericsUtilities.range2Bins(west, east, levelCellsize, false);
        double[] yRangesLevel = NumericsUtilities.range2Bins(south, north, levelCellsize, false);
        int size = (xRangesLevel.length - 1) * (yRangesLevel.length - 1);
        if (doVerbose)
            pm.beginTask("Creating level " + level + " with " + size + " tiles...", xRangesLevel.length - 1);
        else
            pm.message("Creating level " + level + " with " + size + " tiles...");
        for( int x = 0; x < xRangesLevel.length - 1; x++ ) {
            double xmin = xRangesLevel[x];
            double xmax = xRangesLevel[x + 1];
            for( int y = 0; y < yRangesLevel.length - 1; y++ ) {
                double ymin = yRangesLevel[y];
                double ymax = yRangesLevel[y + 1];
                Envelope levelEnv = new Envelope(xmin, xmax, ymin, ymax);
                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(levelEnv);

                List<LasCell> lasCells = LasCellsTable.getLasCells(spatialiteDb, polygon, true, true, false, false, false);

                double avgElev = 0.0;
                double minElev = Double.POSITIVE_INFINITY;
                double maxElev = Double.NEGATIVE_INFINITY;
                short avgIntensity = 0;
                short minIntensity = 30000;
                short maxIntensity = -1;

                int count = 0;
                for( LasCell cell : lasCells ) {
                    avgElev += cell.avgElev;
                    minElev = min(cell.minElev, minElev);
                    maxElev = max(cell.maxElev, maxElev);

                    avgIntensity += cell.avgIntensity;
                    minIntensity = (short) min(cell.minIntensity, minIntensity);
                    maxIntensity = (short) max(cell.maxIntensity, maxIntensity);

                    count++;
                }
                if (count == 0) {
                    continue;
                }
                avgElev /= count;
                avgIntensity /= count;

                LasLevel lasLevel = new LasLevel();
                lasLevel.polygon = polygon;
                lasLevel.level = level;
                lasLevel.avgElev = avgElev;
                lasLevel.minElev = minElev;
                lasLevel.maxElev = maxElev;
                lasLevel.avgIntensity = avgIntensity;
                lasLevel.minIntensity = minIntensity;
                lasLevel.maxIntensity = maxIntensity;
                lasLevel.sourceId = sourceID;

                levelsList.add(lasLevel);

                if (levelsList.size() > 10000) {
                    LasLevelsTable.insertLasLevels(spatialiteDb, srid, levelsList);
                    levelsList = new ArrayList<>();
                }
            }
            if (doVerbose)
                pm.worked(1);
        }
        if (levelsList.size() > 0) {
            LasLevelsTable.insertLasLevels(spatialiteDb, srid, levelsList);
        }
        if (doVerbose)
            pm.done();
        else
            pm.message("Done.");
    }

    private void insertLevel( final ASpatialDb spatialiteDb, long sourceID, double north, double south, double east, double west,
            int level ) throws Exception, SQLException {
        int previousLevelNum = level - 1;
        List<LasLevel> levelsList = new ArrayList<>();
        double levelCellsize = pCellsize * level * pFactor;
        double[] xRangesLevel = NumericsUtilities.range2Bins(west, east, levelCellsize, false);
        double[] yRangesLevel = NumericsUtilities.range2Bins(south, north, levelCellsize, false);
        int size = (xRangesLevel.length - 1) * (yRangesLevel.length - 1);
        if (doVerbose)
            pm.beginTask("Creating level " + level + " with " + size + " tiles...", xRangesLevel.length - 1);
        else
            pm.message("Creating level " + level + " with " + size + " tiles...");
        for( int x = 0; x < xRangesLevel.length - 1; x++ ) {
            double xmin = xRangesLevel[x];
            double xmax = xRangesLevel[x + 1];
            for( int y = 0; y < yRangesLevel.length - 1; y++ ) {
                double ymin = yRangesLevel[y];
                double ymax = yRangesLevel[y + 1];
                Envelope levelEnv = new Envelope(xmin, xmax, ymin, ymax);
                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(levelEnv);

                List<LasLevel> lasLevels = LasLevelsTable.getLasLevels(spatialiteDb, previousLevelNum, levelEnv);

                double avgElev = 0.0;
                double minElev = Double.POSITIVE_INFINITY;
                double maxElev = Double.NEGATIVE_INFINITY;
                short avgIntensity = 0;
                short minIntensity = 30000;
                short maxIntensity = -1;

                int count = 0;
                for( LasLevel lasLevel : lasLevels ) {
                    avgElev += lasLevel.avgElev;
                    minElev = min(lasLevel.minElev, minElev);
                    maxElev = max(lasLevel.maxElev, maxElev);

                    avgIntensity += lasLevel.avgIntensity;
                    minIntensity = (short) min(lasLevel.minIntensity, minIntensity);
                    maxIntensity = (short) max(lasLevel.maxIntensity, maxIntensity);

                    count++;
                }
                if (count == 0) {
                    continue;
                }
                avgElev /= count;
                avgIntensity /= count;

                LasLevel lasLevel = new LasLevel();
                lasLevel.polygon = polygon;
                lasLevel.level = level;
                lasLevel.avgElev = avgElev;
                lasLevel.minElev = minElev;
                lasLevel.maxElev = maxElev;
                lasLevel.avgIntensity = avgIntensity;
                lasLevel.minIntensity = minIntensity;
                lasLevel.maxIntensity = maxIntensity;
                lasLevel.sourceId = sourceID;

                levelsList.add(lasLevel);

                if (levelsList.size() > 10000) {
                    LasLevelsTable.insertLasLevels(spatialiteDb, srid, levelsList);
                    levelsList = new ArrayList<>();
                }
            }
            if (doVerbose)
                pm.worked(1);
        }
        if (levelsList.size() > 0) {
            LasLevelsTable.insertLasLevels(spatialiteDb, srid, levelsList);
        }
        if (doVerbose)
            pm.done();
        else
            pm.message("Done.");
    }

    @Finalize
    public void close() throws Exception {
    }

    public static void main( String[] args ) throws Exception {
        EggClock egg = new EggClock("DATABASE**************** ", "");
        egg.startAndPrint(System.out);
        DatabaseLasWriter w = new DatabaseLasWriter();
        w = new DatabaseLasWriter();
        w.pDbType = EDb.SPATIALITE.name();
        w.pLevels = 4;
        w.pCellsize = 1;
        w.inFolder = "/media/hydrologis/Samsung_T3/UNIBZ/monticolo_tls";
        w.inDatabasePath = "/media/hydrologis/Samsung_T3/UNIBZ/monticolo_tls/monticolo2018_point_cloud_02.sqlite";
        w.process();

        egg.printTimePassedInSeconds(System.out);

    }

}
