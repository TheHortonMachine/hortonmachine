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
package org.jgrasstools.gears.modules.r.mosaic;

import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.EAST;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.WEST;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Module for raster patching")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Mosaic, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Mosaic extends JGTModel {

    @Description("The list of files that have to be patched (used if inGeodata is null).")
    @In
    public List<File> inGeodataFiles;

    @Description("The interpolation type to use: nearest neightbour (0), bilinear (1), bicubic (2)")
    @In
    public int pInterpolation = 0;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The patched coverage.")
    @Out
    public GridCoverage2D outGeodata = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }

        if (inGeodataFiles == null) {
            throw new ModelsIllegalargumentException("No input data have been provided.", this);
        }

        if (inGeodataFiles != null && inGeodataFiles.size() < 2) {
            throw new ModelsIllegalargumentException("The patching module needs at least two maps to be patched.", this);
        }

        GridGeometry2D referenceGridGeometry = null;

        double n = Double.MIN_VALUE;
        double s = Double.MAX_VALUE;
        double e = Double.MIN_VALUE;
        double w = Double.MAX_VALUE;
        int np = Integer.MIN_VALUE;
        int sp = Integer.MAX_VALUE;
        int ep = Integer.MIN_VALUE;
        int wp = Integer.MAX_VALUE;

        pm.beginTask("Calculating final bounds...", inGeodataFiles.size());
        for( File coverageFile : inGeodataFiles ) {

            GridCoverage2D coverage = RasterReader.readCoverage(coverageFile.getAbsolutePath());

            if (referenceGridGeometry == null) {
                // take the first as reference
                referenceGridGeometry = coverage.getGridGeometry();
                crs = coverage.getCoordinateReferenceSystem();
            }

            Envelope2D worldEnv = coverage.getEnvelope2D();
            GridEnvelope2D pixelEnv = referenceGridGeometry.worldToGrid(worldEnv);

            int minPX = (int) pixelEnv.getMinX();
            int minPY = (int) pixelEnv.getMinY();
            int maxPX = (int) pixelEnv.getMaxX();
            int maxPY = (int) pixelEnv.getMaxY();
            if (minPX < wp)
                wp = minPX;
            if (minPY < sp)
                sp = minPY;
            if (maxPX > ep)
                ep = maxPX;
            if (maxPY > np)
                np = maxPY;

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

        int endWidth = ep - wp;
        int endHeight = np - sp;
        WritableRaster outputWR = CoverageUtilities.createDoubleWritableRaster(endWidth, endHeight, null, null,
                JGTConstants.doubleNovalue);
        WritableRandomIter outputIter = RandomIterFactory.createWritable(outputWR, null);

        int offestX = Math.abs(wp);
        int offestY = Math.abs(sp);
        int index = 1;
        for( File coverageFile : inGeodataFiles ) {
            GridCoverage2D coverage = RasterReader.readCoverage(coverageFile.getAbsolutePath());

            RenderedImage renderedImage = coverage.getRenderedImage();
            RandomIter randomIter = RandomIterFactory.create(renderedImage, null);

            Envelope2D env = coverage.getEnvelope2D();

            GridEnvelope2D repEnv = referenceGridGeometry.worldToGrid(env);

            GridGeometry2D tmpGG = coverage.getGridGeometry();
            GridEnvelope2D tmpEnv = tmpGG.worldToGrid(env);

            int startX = (int) (repEnv.getMinX() + offestX);
            int startY = (int) (repEnv.getMinY() + offestY);

            System.out.println();

            double tmpW = tmpEnv.getWidth();
            pm.beginTask("Patch map " + index++, (int) tmpW); //$NON-NLS-1$
            for( int x = 0; x < tmpW; x++ ) {
                for( int y = 0; y < tmpEnv.getHeight(); y++ ) {
                    double value = randomIter.getSampleDouble(x, y, 0);
                    outputIter.setSample(x + startX, y + startY, 0, value);
                }
                pm.worked(1);
            }
            pm.done();
            randomIter.done();
        }

        HashMap<String, Double> envelopeParams = new HashMap<String, Double>();
        envelopeParams.put(NORTH, n);
        envelopeParams.put(SOUTH, s);
        envelopeParams.put(WEST, w);
        envelopeParams.put(EAST, e);

        outGeodata = CoverageUtilities.buildCoverage("patch", outputWR, envelopeParams, crs); //$NON-NLS-1$

    }

}
