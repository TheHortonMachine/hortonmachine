/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.io.grasslegacy.modules;

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.io.grasslegacy.GrassLegacyWriter;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Module for raster patching")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Mosaic, Raster")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class GrassMosaicLegacy extends JGTModel {

    @Description("The list of files that have to be patched (used if inGeodata is null).")
    @In
    public List<File> inGeodataFiles;

    @Description("The interpolation type to use: nearest neightbour (0), bilinear (1), bicubic (2)")
    @In
    public int pInterpolation = 0;

    @Description("The output file resolution in meters.")
    @In
    public Double pRes = null;

    @Description("The optional requested boundary coordinates as array of [n, s, w, e].")
    @In
    public double[] pBounds = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The grass file path to which to write to.")
    @In
    public String outFile = null;

    private Envelope2D requestedEnvelope;

    @Execute
    public void process() throws Exception {

        if (inGeodataFiles == null) {
            throw new ModelsIllegalargumentException("No input data have been provided.", this);
        }

        if (pRes == null) {
            throw new ModelsIllegalargumentException("The definition of the output resolution is mandatory.", this);
        }

        if (inGeodataFiles != null && inGeodataFiles.size() < 2) {
            throw new ModelsIllegalargumentException("The patching module needs at least two maps to be patched.", this);
        }

        GridGeometry2D referenceGridGeometry = null;

        if (pBounds != null) {
            DirectPosition2D first = new DirectPosition2D(pBounds[2], pBounds[1]);
            DirectPosition2D second = new DirectPosition2D(pBounds[3], pBounds[0]);
            requestedEnvelope = new Envelope2D(first, second);
        }

        double n = Double.MIN_VALUE;
        double s = Double.MAX_VALUE;
        double e = Double.MIN_VALUE;
        double w = Double.MAX_VALUE;

        pm.beginTask("Calculating final bounds...", inGeodataFiles.size());
        for( File coverageFile : inGeodataFiles ) {
            GridCoverage2D coverage = RasterReader.readCoverage(coverageFile.getAbsolutePath());
            if (referenceGridGeometry == null) {
                // take the first as reference
                referenceGridGeometry = coverage.getGridGeometry();
            }

            Envelope2D worldEnv = coverage.getEnvelope2D();
            if (requestedEnvelope != null) {
                if (!requestedEnvelope.intersects(worldEnv)) {
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
        for( File coverageFile : inGeodataFiles ) {
            GridCoverage2D coverage = RasterReader.readCoverage(coverageFile.getAbsolutePath());
            Envelope2D env = coverage.getEnvelope2D();
            if (requestedEnvelope != null) {
                if (!requestedEnvelope.intersects(env)) {
                    // if a constraint envelope was supplied, only handle what is inside
                    continue;
                }
            }

            RenderedImage renderedImage = coverage.getRenderedImage();
            RandomIter randomIter = RandomIterFactory.create(renderedImage, null);

            double east = env.getMaxX();
            double south = env.getMinY();
            double west = env.getMinX();
            double north = env.getMaxY();

            Window tmpWindow = new Window(west, east, south, north, pRes, pRes);

            double startRow = (writeWindow.getNorth() - tmpWindow.getNorth()) / pRes;
            double startCol = (tmpWindow.getWest() - writeWindow.getWest()) / pRes;

            pm.beginTask("Patch map " + index++, tmpWindow.getRows()); //$NON-NLS-1$
            double[] value = new double[1];
            for( int row = 0; row < tmpWindow.getRows(); row++ ) {
                double northing = north - row * pRes;
                for( int col = 0; col < tmpWindow.getCols(); col++ ) {
                    double easting = west + col * pRes;
                    coverage.evaluate(new Point2D.Double(easting, northing), value);

                    if (!Double.isNaN(value[0])) {
                        if (Double.isNaN(outputData[(int) (row + startRow)][(int) (col + startCol)])) {
                            outputData[(int) (row + startRow)][(int) (col + startCol)] = value[0];
                        }
                    }
                }
                pm.worked(1);
            }
            pm.done();
            randomIter.done();
        }

        pm.message("Writing mosaic map.");
        GrassLegacyWriter writer = new GrassLegacyWriter();
        writer.geodata = outputData;
        writer.file = outFile;
        writer.inWindow = writeWindow;
        writer.pm = pm;
        writer.writeRaster();

    }
}
