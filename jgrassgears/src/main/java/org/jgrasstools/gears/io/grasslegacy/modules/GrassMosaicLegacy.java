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
package org.jgrasstools.gears.io.grasslegacy.modules;

import java.io.File;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.io.grasslegacy.GrassLegacyWriter;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Module for GRASS raster patching.")
@Documentation("GrassMosaicLegacy.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Mosaic, Raster")
@Name("grassmosaic")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class GrassMosaicLegacy extends JGTModel {

    @Description("The list of files that have to be patched (used if inGeodata is null).")
    @In
    public List<File> inFiles;

    @Description("The output file resolution in meters.")
    @In
    public Double pRes = null;

    @Description("The optional requested boundary coordinates as array of [n, s, w, e].")
    @In
    public double[] pBounds = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The GRASS file path to which to write to.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outGrassFile = null;

    private Envelope2D requestedEnvelope;

    @Execute
    public void process() throws Exception {

        if (inFiles == null) {
            throw new ModelsIllegalargumentException("No input data have been provided.", this);
        }

        if (pRes == null) {
            throw new ModelsIllegalargumentException("The definition of the output resolution is mandatory.", this);
        }

        if (inFiles != null && inFiles.size() < 2) {
            throw new ModelsIllegalargumentException("The patching module needs at least two maps to be patched.", this);
        }

        if (pBounds != null) {
            DirectPosition2D first = new DirectPosition2D(pBounds[2], pBounds[1]);
            DirectPosition2D second = new DirectPosition2D(pBounds[3], pBounds[0]);
            requestedEnvelope = new Envelope2D(first, second);
        }

        double n = Double.MIN_VALUE;
        double s = Double.MAX_VALUE;
        double e = Double.MIN_VALUE;
        double w = Double.MAX_VALUE;

        CoordinateReferenceSystem crs = null;
        pm.beginTask("Calculating final bounds...", inFiles.size());
        for( File coverageFile : inFiles ) {
            RasterReader reader = new RasterReader();
            reader.file = coverageFile.getAbsolutePath();
            reader.doEnvelope = true;
            reader.process();
            GeneralEnvelope gEnv = reader.originalEnvelope;
            ReferencedEnvelope worldEnv = new ReferencedEnvelope(gEnv);
            if (crs == null) {
                crs = gEnv.getCoordinateReferenceSystem();
            }
            if (requestedEnvelope != null) {
                if (!requestedEnvelope.intersects(gEnv.toRectangle2D())) {
                    // if a constraint envelope was supplied, only handle what is inside
                    pm.worked(1);
                    continue;
                }
            }

            double minWX = worldEnv.getMinX();
            double minWY = worldEnv.getMinY();
            double maxWX = worldEnv.getMaxX();
            double maxWY = worldEnv.getMaxY();
            if (minWX < w)
                w = minWX;
            if (minWY < s)
                s = minWY;
            if (maxWX > e)
                e = maxWX;
            if (maxWY > n)
                n = maxWY;
            pm.worked(1);
        }
        pm.done();

        Window writeWindow = new Window(w, e, s, n, pRes, pRes);

        GridGeometry2D writeGridGeometry = CoverageUtilities.gridGeometryFromRegionValues(n, s, e, w, writeWindow.getCols(),
                writeWindow.getRows(), crs);

        int rows = writeWindow.getRows();
        int cols = writeWindow.getCols();

        long megabytes = (rows * (long) cols) * 8 / 1024l / 1024l;

        pm.message("Will allocate " + (rows * (long) cols) + " cells, equal to about " + megabytes);

        double[][] outputData = new double[rows][cols];
        for( int i = 0; i < outputData.length; i++ ) {
            for( int j = 0; j < outputData[0].length; j++ ) {
                outputData[i][j] = JGTConstants.doubleNovalue;
            }
        }
        pm.message("Memory allocated.");

        int index = 1;
        for( File coverageFile : inFiles ) {
            GridCoverage2D coverage = RasterReader.readRaster(coverageFile.getAbsolutePath());
            Envelope2D env = coverage.getEnvelope2D();
            GridGeometry2D gridGeometry = coverage.getGridGeometry();
            if (requestedEnvelope != null) {
                if (!requestedEnvelope.intersects(env)) {
                    // if a constraint envelope was supplied, only handle what is inside
                    continue;
                }
            }

            double[] value = new double[1];
            GridEnvelope2D gridRange2D = gridGeometry.getGridRange2D();
            int grX = gridRange2D.x;
            int grY = gridRange2D.y;
            int grW = gridRange2D.width;
            int grH = gridRange2D.height;
            int grXEnd = grX + grW;
            int grYEnd = grY + grH;
            pm.beginTask("Patch map " + index++, grW); //$NON-NLS-1$
            for( int i = grX; i < grXEnd; i++ ) {
                for( int j = grY; j < grYEnd; j++ ) {
                    GridCoordinates2D gCoord = new GridCoordinates2D(i, j);
                    coverage.evaluate(gCoord, value);
                    DirectPosition directPosition = gridGeometry.gridToWorld(gCoord);
                    GridCoordinates2D gridCoord = writeGridGeometry.worldToGrid(directPosition);

                    if (!Double.isNaN(value[0])) {
                        if (Double.isNaN(outputData[gridCoord.y][gridCoord.x])) {
                            outputData[gridCoord.y][gridCoord.x] = value[0];
                        }
                    }
                }
                pm.worked(1);
            }
            pm.done();
        }

        pm.message("Writing mosaic map.");
        GrassLegacyWriter writer = new GrassLegacyWriter();
        writer.geodata = outputData;
        writer.file = outGrassFile;
        writer.inWindow = writeWindow;
        writer.pm = pm;
        writer.writeRaster();

    }
}
