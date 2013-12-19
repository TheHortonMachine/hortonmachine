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
package org.jgrasstools.gears.libs.modules;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralEnvelope;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.modules.r.imagemosaic.OmsImageMosaicCreator;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.colors.ColorTables;
import org.jgrasstools.gears.utils.colors.RasterStyleUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public abstract class JGTModelIM extends JGTModel {

    private List<ImageMosaicReader> readers = new ArrayList<ImageMosaicReader>();

    protected List<Raster> inRasters = new ArrayList<Raster>();
    protected WritableRandomIter outDataIter;

    protected String locationField;
    protected double xRes;
    protected double yRes;
    protected CoordinateReferenceSystem crs;
    protected double[] llCorner;
    protected double[] urCorner;
    protected List<Geometry> boundsGeometries;

    protected int cellBuffer = 0;
    private File outParentFolder;
    private String outBaseName;

    protected void addSource( File imageMosaicSource ) throws IOException {
        ImageMosaicReader imReader = new ImageMosaicReader(imageMosaicSource);
        if (readers.size() == 0) {
            File propertiesFile = FileUtilities.substituteExtention(imageMosaicSource, "properties");
            HashMap<String, String> propertiesMap = FileUtilities.readFileToHasMap(propertiesFile.getAbsolutePath(), null, false);

            String xyREs = propertiesMap.get("Levels");
            String[] split = xyREs.split(",");
            xRes = Double.parseDouble(split[0]);
            yRes = Double.parseDouble(split[1]);

            locationField = propertiesMap.get("LocationAttribute");
            crs = imReader.getCrs();

            GeneralEnvelope originalEnvelope = imReader.getOriginalEnvelope();
            llCorner = originalEnvelope.getLowerCorner().getCoordinate();
            urCorner = originalEnvelope.getUpperCorner().getCoordinate();

            SimpleFeatureCollection vectorBounds = OmsVectorReader.readVector(imageMosaicSource.getAbsolutePath());
            boundsGeometries = FeatureUtilities.featureCollectionToGeometriesList(vectorBounds, true, locationField);
        }
        readers.add(imReader);
    }

    protected void setOutput( String outputPath ) {
        File outputFile = new File(outputPath);
        outParentFolder = outputFile.getParentFile();
        outBaseName = FileUtilities.getNameWithoutExtention(outputFile);
    }

    protected void processTiles() throws Exception {
        int size = boundsGeometries.size();
        int count = 0;
        pm.beginTask("Processing...", size);
        for( Geometry boundGeometry : boundsGeometries ) {
            count++;
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

            GridGeometry2D writeGridGeometry = CoverageUtilities.gridGeometryFromRegionValues(writeNorth, writeSouth, writeEast,
                    writeWest, writeCols, writeRows, crs);
            WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(writeCols, writeRows, null, null,
                    JGTConstants.doubleNovalue);
            RegionMap writeParams = CoverageUtilities.gridGeometry2RegionParamsMap(writeGridGeometry);
            GridCoverage2D writeGC = CoverageUtilities.buildCoverage("tile", outWR, writeParams, crs);

            GridGeometry2D readGridGeometry = null;
            GeneralParameterValue[] readGeneralParameterValues = CoverageUtilities.createGridGeometryGeneralParameter(xRes, yRes,
                    readNorth, readSouth, readEast, readWest, crs);

            inRasters.clear();
            for( ImageMosaicReader reader : readers ) {
                GridCoverage2D readGC = reader.read(readGeneralParameterValues);
                readGridGeometry = readGC.getGridGeometry();
                // read raster at once, since a randomiter is way slower
                Raster readRaster = readGC.getRenderedImage().getData();
                inRasters.add(readRaster);
            }

            GridCoordinates2D llGrid = readGridGeometry.worldToGrid(new DirectPosition2D(llCorner[0], llCorner[1]));
            GridCoordinates2D urGrid = readGridGeometry.worldToGrid(new DirectPosition2D(urCorner[0], urCorner[1]));
            int minX = llGrid.x;
            int maxY = llGrid.y; // y grid is inverse
            int maxX = urGrid.x;
            int minY = urGrid.y;
            // is there a gridrange shift?
            GridEnvelope2D gridRange2D = readGridGeometry.getGridRange2D();
            minY = minY + gridRange2D.y;
            maxY = maxY + gridRange2D.y;
            minX = minX + gridRange2D.x;
            maxX = maxX + gridRange2D.x;

            outDataIter = CoverageUtilities.getWritableRandomIterator(outWR);

            for( int writeCol = 0; writeCol < writeCols; writeCol++ ) {
                for( int writeRow = 0; writeRow < writeRows; writeRow++ ) {
                    DirectPosition writeGridToWorld = writeGridGeometry.gridToWorld(new GridCoordinates2D(writeCol, writeRow));
                    GridCoordinates2D worldToReadGrid = readGridGeometry.worldToGrid(writeGridToWorld);
                    int readCol = worldToReadGrid.x;
                    int readRow = worldToReadGrid.y;

                    if (readCol + cellBuffer > maxX || readCol - cellBuffer < minX || //
                            readRow + cellBuffer > maxY || readRow - cellBuffer < minY) {
                        continue;
                    }

                    processCell(readCol, readRow, writeCol, writeRow);
                }
            }

            File outTileFile = new File(outParentFolder, outBaseName + "_" + count + ".tiff");
            OmsRasterWriter writer = new OmsRasterWriter();
            writer.pm = new DummyProgressMonitor();
            writer.inRaster = writeGC;
            writer.file = outTileFile.getAbsolutePath();
            writer.process();
            pm.worked(1);
        }
        pm.done();

    }

    protected void makeMosaicWithStyle( ColorTables colorTable, double min, double max ) {
        if (colorTable == null)
            colorTable = ColorTables.extrainbow;
        try {
            OmsImageMosaicCreator im = new OmsImageMosaicCreator();
            im.inFolder = outParentFolder.getAbsolutePath();
            im.process();
            String name = outParentFolder.getName();
            String style = RasterStyleUtilities.createStyleForColortable(colorTable.name(), min, max, null, 1.0);
            File styleFile = new File(outParentFolder, name + ".sld");
            FileUtilities.writeFile(style, styleFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void processCell( int readCol, int readRow, int writeCol, int writeRow );

}
