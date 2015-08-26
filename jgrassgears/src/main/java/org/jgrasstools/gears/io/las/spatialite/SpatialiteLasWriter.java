/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.las.spatialite;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.modules.utils.fileiterator.OmsFileIterator;
import org.jgrasstools.gears.spatialite.SpatialiteDb;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

@Description("Inserts las files into a spatialite database.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, lidar, spatialite")
@Label(JGTConstants.LESTO + "/utilities")
@Name("spatialitelaswriter")
@Status(5)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class SpatialiteLasWriter extends JGTModel {

    @Description("The folder containing the las files to index.")
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description("The spatialite database.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSpatialite;

    @Description("The optional code defining the target coordinate reference system. This is needed only if the file has no prj file. If set, it will be used over the prj file.")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The size of the cells into which to split the las file for indexing (in units defined by the projection).")
    @In
    public double pCellsize = 3;

    private CoordinateReferenceSystem crs;

    private int srid = -9999;

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, inSpatialite);

        if (pCellsize <= 0) {
            throw new ModelsIllegalargumentException("The cell size parameter needs to be > 0.", this);
        }

        if (!new File(inFolder).exists()) {
            throw new ModelsIllegalargumentException("The inFolder parameter has to be valid.", this);
        }

        File spatialiteFile = new File(inSpatialite);
        try (SpatialiteDb spatialiteDb = new SpatialiteDb()) {
            spatialiteDb.open(inSpatialite);
            if (!spatialiteFile.exists()) {
                pm.beginTask("Create new spatialite database...", IJGTProgressMonitor.UNKNOWN);
                spatialiteDb.initSpatialMetadata(null);
                pm.done();
            }

            try {
                if (pCode != null) {
                    crs = CRS.decode(pCode);
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
                throw new ModelsIllegalargumentException("An error occurred while reading the projection definition: "
                        + e1.getLocalizedMessage(), this);
            }

            if (srid == -9999) {
                srid = 4326;
                pm.errorMessage("No crs has been defined. Setting it to 4326 by default.");
            }

            LasSourcesTable.createTable(spatialiteDb, srid);
            LasCellsTable.createTable(spatialiteDb, srid);

            pm.message("Las files to be added to the index:");
            OmsFileIterator iter = new OmsFileIterator();
            iter.inFolder = inFolder;
            iter.fileFilter = new FileFilter(){
                public boolean accept( File file ) {
                    String name = file.getName();
                    boolean isLas = name.toLowerCase().endsWith(".las");
                    if (isLas) {
                        pm.message("   " + name);
                    }
                    return isLas;
                }
            };
            iter.process();

            List<File> filesList = iter.filesList;
            List<File> filesListToAdd = new ArrayList<File>();
            List<Long> sourceIds = new ArrayList<Long>();
            List<LasSource> lasSources = LasSourcesTable.getLasSources(spatialiteDb);
            List<String> names = new ArrayList<String>();
            for( LasSource lasSource : lasSources ) {
                names.add(lasSource.name);
            }
            pm.beginTask("Creating sources index...", filesList.size());
            for( File lasFile : filesList ) {
                String lasName = FileUtilities.getNameWithoutExtention(lasFile);
                if (names.contains(lasName)) {
                    pm.errorMessage("Not inserting already existing file in database: " + lasName);
                    continue;
                }
                filesListToAdd.add(lasFile);
                try (ALasReader reader = ALasReader.getReader(lasFile, crs)) {
                    reader.open();
                    ILasHeader header = reader.getHeader();
                    ReferencedEnvelope3D envelope = header.getDataEnvelope();

                    Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(envelope);

                    long id = LasSourcesTable.insertLasSource(spatialiteDb, srid, polygon, lasName, envelope.getMinZ(),
                            envelope.getMaxZ());
                    sourceIds.add(id);
                }
                pm.worked(1);
            }
            pm.done();

            for( int i = 0; i < filesListToAdd.size(); i++ ) {
                File file = filesListToAdd.get(i);
                long sourceId = sourceIds.get(i);
                processFile(spatialiteDb, file, sourceId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processFile( final SpatialiteDb spatialiteDb, File file, long sourceID ) throws Exception {
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

            double[] xRanges = NumericsUtilities.range2Bins(west, east, pCellsize, true);
            double[] yRanges = NumericsUtilities.range2Bins(south, north, pCellsize, true);
            int cols = xRanges.length - 1;
            int rows = yRanges.length - 1;
            int tilesCount = cols * rows;

            pm.message("Splitting " + name + " into " + tilesCount + " tiles.");
            GridGeometry2D gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west, cols, rows,
                    reader.getHeader().getCrs());

            List<LasRecord>[][] dotOnMatrix = new ArrayList[cols][rows];
            pm.beginTask("Sorting points for " + name, (int) recordsCount);
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
                pm.worked(1);
            }
            pm.done();

            ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
            pm.beginTask("Write las data...", cols * rows);
            for( int c = 0; c < cols; c++ ) {
                for( int r = 0; r < rows; r++ ) {
                    List<LasRecord> dotsList = dotOnMatrix[c][r];
                    if (dotsList == null || dotsList.size() == 0) {
                        continue;
                    }
                    int pointCount = dotsList.size();
                    Coordinate coord = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
                    Envelope env = new Envelope(coord);
                    env.expandBy(pCellsize / 2.0, pCellsize / 2.0);
                    Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(env);

                    double avgElev = 0.0;
                    double minElev = Double.POSITIVE_INFINITY;
                    double maxElev = Double.NEGATIVE_INFINITY;
                    byte[] position = new byte[8 * 3 * pointCount];
                    ByteBuffer positionBuffer = ByteBuffer.wrap(position);
                    
                    // TODO check, maybe here a median would be more appropriate 
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

                        if (dot.color != null) {
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
                    lasCell.xzys = position;
                    lasCell.avgIntensity = (short) Math.round(avgIntensity);
                    lasCell.minIntensity = minIntensity;
                    lasCell.maxIntensity = maxIntensity;
                    lasCell.intensitiesClassifications = intensClass;
                    lasCell.returns = returns;
                    lasCell.minGpsTime = minGpsTime;
                    lasCell.maxGpsTime = maxGpsTime;
                    lasCell.gpsTimes = gpsTimes;
                    lasCell.colors = colors;

                    // add data to db in a thread to fasten up things
                    singleThreadExecutor.execute(new Runnable(){
                        public void run() {
                            try {
                                LasCellsTable.insertLasCell(spatialiteDb, srid, lasCell);
                                pm.worked(1);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
            try {
                singleThreadExecutor.shutdown();
                singleThreadExecutor.awaitTermination(30, TimeUnit.DAYS);
                singleThreadExecutor.shutdownNow();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            pm.done();
        }

    }

    @Finalize
    public void close() throws Exception {
    }

    public static void main( String[] args ) throws Exception {
        SpatialiteLasWriter w = new SpatialiteLasWriter();
        w.inFolder = "I:/UNIBZ/aurina_spatialite/las";
        w.inSpatialite = "I:/UNIBZ/aurina_spatialite/las_aurina.sqlite";
        w.pCellsize = 5;
        w.process();
    }
}
