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
package org.hortonmachine.gears.libs.modules;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralEnvelope;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public abstract class HMModelIM extends HMModel {

    private List<ImageMosaicReader> readers = new ArrayList<ImageMosaicReader>();

    protected List<RandomIter> inRasterIterators = new ArrayList<RandomIter>();
    protected List<Double> inRasterNovalues = new ArrayList<Double>();
    protected List<GridCoverage2D> inRasters = new ArrayList<GridCoverage2D>();
    protected List<WritableRandomIter> outRasterIterators = new ArrayList<WritableRandomIter>();
    protected List<GridCoverage2D> outGridCoverages = new ArrayList<GridCoverage2D>();
    private List<File> outRasterFiles = new ArrayList<File>();

    protected String locationField;
    protected double xRes;
    protected double yRes;
    protected CoordinateReferenceSystem crs;
    protected double[] llCorner;
    protected double[] urCorner;
    protected List<Geometry> boundsGeometries;

    protected int cellBuffer = 0;

    protected GridGeometry2D readGridGeometry;
    private boolean isSingleInX = true;
    private boolean isSingleInY = true;

    protected void addSource( File imageMosaicSource ) throws Exception {

        URL imageMosaicUrl = imageMosaicSource.toURI().toURL();
        final AbstractGridFormat imageMosaicFormat = (AbstractGridFormat) GridFormatFinder.findFormat(imageMosaicUrl);
        final ImageMosaicReader imReader = (ImageMosaicReader) imageMosaicFormat.getReader(imageMosaicUrl);
        // ImageMosaicReader imReader = new ImageMosaicReader(imageMosaicSource);

        if (readers.size() == 0) {
            File propertiesFile = FileUtilities.substituteExtention(imageMosaicSource, "properties");
            HashMap<String, String> propertiesMap = FileUtilities.readFileToHashMap(propertiesFile.getAbsolutePath(), null,
                    false);

            String xyREs = propertiesMap.get("Levels");
            String[] split = xyREs.split(",");
            xRes = Double.parseDouble(split[0]);
            yRes = Double.parseDouble(split[1]);

            locationField = propertiesMap.get("LocationAttribute");
            crs = imReader.getCoordinateReferenceSystem();

            GeneralEnvelope originalEnvelope = imReader.getOriginalEnvelope();
            llCorner = originalEnvelope.getLowerCorner().getCoordinate();
            urCorner = originalEnvelope.getUpperCorner().getCoordinate();

            SimpleFeatureCollection vectorBounds = OmsVectorReader.readVector(imageMosaicSource.getAbsolutePath());
            boundsGeometries = FeatureUtilities.featureCollectionToGeometriesList(vectorBounds, true, locationField);

            Envelope singleTileEnv = boundsGeometries.get(0).getEnvelopeInternal();
            Envelope allTilesEnv = new Envelope();
            for( Geometry boundsGeometry : boundsGeometries ) {
                allTilesEnv.expandToInclude(boundsGeometry.getEnvelopeInternal());
            }
            if (allTilesEnv.getWidth() > singleTileEnv.getWidth()) {
                isSingleInX = false;
            }
            if (allTilesEnv.getHeight() > singleTileEnv.getHeight()) {
                isSingleInY = false;
            }

        }
        readers.add(imReader);
    }

    protected void addDestination( File outputFile ) throws IOException {
        outRasterFiles.add(outputFile);
    }

    protected void addDestination( File outputFile, int position ) throws IOException {
        if (outRasterFiles.size() < position + 1) {
            for( int i = 0; i < position + 1; i++ ) {
                outRasterFiles.add(new File("dummy"));
            }
        }
        outRasterFiles.set(position, outputFile);
    }

    /**
     * Get the {@link GridCoverage2D} in a certain region.
     * 
     * <p>It might be usefull for faster access to the underlying data to
     * extract the raster before creatin the iterator, instead of iterating 
     * directly.
     * <p>This can be done on the returned gridcoverage through:
     * <code>
     * Raster readRaster = readGC.getRenderedImage().getData();
     * RandomIter readIter = RandomIterFactory.create(readRaster, null);
     * </code>  
     * 
     * @param readerNum the number of the reader to use.
     * @param north
     * @param south
     * @param east
     * @param west
     * @return the gridcoverage inside the supplied bounds.
     * @throws Exception
     */
    protected GridCoverage2D getGridCoverage( int readerNum, double north, double south, double east, double west )
            throws Exception {
        GeneralParameterValue[] readGeneralParameterValues = CoverageUtilities.createGridGeometryGeneralParameter(xRes, yRes,
                north, south, east, west, crs);

        ImageMosaicReader reader = readers.get(readerNum);
        GridCoverage2D readGC = reader.read(readGeneralParameterValues);
        return readGC;
    }

    protected GridCoverage2D getGridCoverage( int readerNum, Envelope envelope ) throws Exception {
        return getGridCoverage(readerNum, envelope.getMaxY(), envelope.getMinY(), envelope.getMaxX(), envelope.getMinX());
    }

    protected void processByTileCells() throws Exception {
        int size = boundsGeometries.size();
        int count = 0;
        // pm.beginTask("Processing tiles...", size);
        for( Geometry boundGeometry : boundsGeometries ) {
            count++;
            try {
                pm.message("Processing tile " + boundGeometry.getUserData() + "(" + count + " of " + size + ")");
                pm.message("\t\t->geom: " + boundGeometry.getEnvelopeInternal());
                pm.message("\t\t->reading with cell buffer: " + cellBuffer);
                pm.message("\t\t->reading with x/y resolution: " + xRes + "/" + yRes);
                processGeometryByTileCell(count, boundGeometry);
            } catch (Exception e) {
                pm.errorMessage("Problems found for tile: " + boundGeometry.getUserData());
                throw e;
            }
            // pm.worked(1);
        }
        // pm.done();

    }

    private void processGeometryByTileCell( int count, Geometry boundGeometry )
            throws IOException, TransformException, Exception {
        Envelope writeEnv = boundGeometry.getEnvelopeInternal();

        double writeEast = writeEnv.getMaxX();
        double writeWest = writeEnv.getMinX();
        double writeNorth = writeEnv.getMaxY();
        double writeSouth = writeEnv.getMinY();
        int writeCols = (int) ((writeEast - writeWest) / xRes);
        int writeRows = (int) ((writeNorth - writeSouth) / yRes);

        Envelope readEnv = new Envelope(writeEnv);
        readEnv.expandBy(cellBuffer * xRes, cellBuffer * yRes);

        double readEast = readEnv.getMaxX();
        double readWest = readEnv.getMinX();
        double readNorth = readEnv.getMaxY();
        double readSouth = readEnv.getMinY();
        // int readCols = (int) ((readEast - readWest) / xRes);
        // int readRows = (int) ((readNorth - readSouth) / yRes);

        /*
         * clear lists of in and out data local to the loop
         */
        outGridCoverages.clear();
        inRasterIterators.clear();
        inRasters.clear();
        outRasterIterators.clear();

        GridGeometry2D writeGridGeometry = CoverageUtilities.gridGeometryFromRegionValues(writeNorth, writeSouth, writeEast,
                writeWest, writeCols, writeRows, crs);

        for( File outRasterFile : outRasterFiles ) {
            File parentFile = outRasterFile.getParentFile();
            if (parentFile != null && parentFile.exists()) {
                WritableRaster outWR = CoverageUtilities.createWritableRaster(writeCols, writeRows, null, null,
                        HMConstants.doubleNovalue);
                RegionMap writeParams = CoverageUtilities.gridGeometry2RegionParamsMap(writeGridGeometry);
                GridCoverage2D writeGC = CoverageUtilities.buildCoverage(outRasterFile.getName(), outWR, writeParams, crs);
                outGridCoverages.add(writeGC);
                WritableRandomIter outDataIter = CoverageUtilities.getWritableRandomIterator(outWR);
                outRasterIterators.add(outDataIter);
            } else {
                outGridCoverages.add(null);
                outRasterIterators.add(null);
            }
        }

        readGridGeometry = null;
        GeneralParameterValue[] readGeneralParameterValues = CoverageUtilities.createGridGeometryGeneralParameter(xRes, yRes,
                readNorth, readSouth, readEast, readWest, crs);

        int index = 0;
        for( ImageMosaicReader reader : readers ) {
            try {
                GridCoverage2D readGC = reader.read(readGeneralParameterValues);
                readGridGeometry = readGC.getGridGeometry();
                // read raster at once, since a randomiter is way slower when wrapping borders
                Raster readRaster = readGC.getRenderedImage().getData();
                RandomIter readIter = RandomIterFactory.create(readRaster, null);
                inRasterIterators.add(readIter);
                inRasters.add(readGC);
                inRasterNovalues.add(HMConstants.getNovalue(readGC));
                index++;
            } catch (Exception e) {
                StringBuilder errSb = new StringBuilder();
                errSb.append("ERROR: could not read coverage for parameters: \n");
                errSb.append(readGeneralParameterValues[0]);
                errSb.append("ERROR: with reader N." + index + ": " + Arrays.toString(reader.getGridCoverageNames()));
                errSb.append("\nERROR: " + e.getLocalizedMessage());
                pm.errorMessage(errSb.toString());
                freeIterators();
                // e.printStackTrace();
                // return;
                throw new IOException("Problems reading Mosaic!");
            }
        }

        // Envelope allBoundsEnv = new Envelope(new Coordinate(llCorner[0], llCorner[1]), new
        // Coordinate(urCorner[0], urCorner[1]));
        // MathTransform crsToGrid = readGridGeometry.getCRSToGrid2D(PixelOrientation.LOWER_RIGHT);
        //
        // Envelope transformed = JTS.transform(allBoundsEnv, crsToGrid);
        // int minX = (int) round(transformed.getMinX());
        // int maxY = (int) round(transformed.getMaxY()); // y grid is inverse
        // int maxX = (int) round(transformed.getMaxX());
        // int minY = (int) round(transformed.getMinY());

        GridCoordinates2D llGrid = readGridGeometry.worldToGrid(new DirectPosition2D(llCorner[0], llCorner[1]));
        GridCoordinates2D urGrid = readGridGeometry.worldToGrid(new DirectPosition2D(urCorner[0], urCorner[1]));
        int minX = llGrid.x;
        int maxY = llGrid.y; // y grid is inverse
        int maxX = urGrid.x;
        int minY = urGrid.y;

        // is there a gridrange shift?
        GridEnvelope2D gridRange2D = readGridGeometry.getGridRange2D();
        int readRows = gridRange2D.height;
        minY = minY + gridRange2D.y;
        // TODO check this out properly
        if (isSingleInY) {
            maxY = maxY - gridRange2D.y;
        } else {
            maxY = maxY + gridRange2D.y;
        }
        int readCols = gridRange2D.width;
        minX = minX + gridRange2D.x;
        if (isSingleInX) {
            maxX = maxX - gridRange2D.x;
        } else {
            maxX = maxX + gridRange2D.x;
        }

        try {
            final GridCoordinates2D gridCoordinates2D = new GridCoordinates2D();
            for( int writeRow = 0; writeRow < writeRows; writeRow++ ) {
                for( int writeCol = 0; writeCol < writeCols; writeCol++ ) {
                    gridCoordinates2D.x = writeCol;
                    gridCoordinates2D.y = writeRow;
                    DirectPosition writeGridToWorld = writeGridGeometry.gridToWorld(gridCoordinates2D);
                    GridCoordinates2D worldToReadGrid = readGridGeometry.worldToGrid(writeGridToWorld);
                    int readCol = worldToReadGrid.x;
                    int readRow = worldToReadGrid.y;

                    if (readCol + cellBuffer > maxX || readCol - cellBuffer < minX || //
                            readRow + cellBuffer > maxY || readRow - cellBuffer < minY) {
                        continue;
                    }

                    processCell(readCol, readRow, writeCol, writeRow, readCols, readRows, writeCols, writeRows);
                }
            }

        } finally {
            freeIterators();
        }

        for( int i = 0; i < outRasterFiles.size(); i++ ) {
            File outputFile = outRasterFiles.get(i);
            GridCoverage2D writeGC = outGridCoverages.get(i);
            if (writeGC != null) {
                File outParentFolder = outputFile.getParentFile();
                if (outParentFolder == null || !outParentFolder.exists()) {
                    continue;
                }
                String outBaseName = FileUtilities.getNameWithoutExtention(outputFile);
                File outTileFile = new File(outParentFolder, outBaseName + "_" + count + ".tiff");
                OmsRasterWriter writer = new OmsRasterWriter();
                writer.pm = new DummyProgressMonitor();
                writer.inRaster = writeGC;
                writer.file = outTileFile.getAbsolutePath();
                writer.process();
            }
        }

    }

    private void freeIterators() {
        for( RandomIter inRasterIterator : inRasterIterators ) {
            if (inRasterIterator != null)
                inRasterIterator.done();
        }
        for( RandomIter outRasterIterator : outRasterIterators ) {
            if (outRasterIterator != null)
                outRasterIterator.done();
        }
    }

    protected void makeMosaic() throws Exception {
        for( int i = 0; i < outRasterFiles.size(); i++ ) {
            File outputFile = outRasterFiles.get(i);
            if (outputFile != null) {
                File outParentFolder = outputFile.getParentFile();
                if (outParentFolder == null || !outParentFolder.exists()) {
                    continue;
                }
                OmsImageMosaicCreator im = new OmsImageMosaicCreator();
                im.inFolder = outParentFolder.getAbsolutePath();
                im.process();
            }
        }
    }

    protected void makeStyle( EColorTables colorTable, double min, double max ) throws Exception {
        for( int i = 0; i < outRasterFiles.size(); i++ ) {
            File outputFile = outRasterFiles.get(i);
            if (outputFile != null) {
                File outParentFolder = outputFile.getParentFile();
                if (outParentFolder == null || !outParentFolder.exists()) {
                    continue;
                }
                if (colorTable == null)
                    colorTable = EColorTables.extrainbow;
                String name = outParentFolder.getName();
                String style = RasterStyleUtilities
                        .styleToString(RasterStyleUtilities.createStyleForColortable(colorTable.name(), min, max, null, 1.0));
                File styleFile = new File(outParentFolder, name + ".sld");
                FileUtilities.writeFile(style, styleFile);
            }
        }
    }

    /**
     * Disposes resources.
     */
    protected void dispose() {
        for( ImageMosaicReader reader : readers ) {
            reader.dispose();
        }
    }

    /**
     * Process one cell.
     * 
     * <p>This is used when {@link #processByTileCells()} is called.
     * 
     * @param readCol the column of the cell to read.
     * @param readRow  the row of the cell to read.
     * @param writeCol the column of the cell to write.
     * @param writeRow the row of the cell to write.
     * @param readCols the total columns of the current handled read tile.
     * @param readRows the total rows of the current handled read tile.
     * @param writeCols the total columns of the current handled written tile.
     * @param writeRows the total rows of the current handled written tile.
     */
    protected abstract void processCell( int readCol, int readRow, int writeCol, int writeRow, int readCols, int readRows,
            int writeCols, int writeRows );

}
