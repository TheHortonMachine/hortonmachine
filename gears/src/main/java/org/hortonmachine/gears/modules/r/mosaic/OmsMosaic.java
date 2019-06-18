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
package org.hortonmachine.gears.modules.r.mosaic;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_IN_FILES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_P_INTERPOLATION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMOSAIC_STATUS;
import static org.hortonmachine.gears.libs.modules.Variables.BICUBIC;
import static org.hortonmachine.gears.libs.modules.Variables.BILINEAR;
import static org.hortonmachine.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.EAST;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.WEST;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSMOSAIC_DESCRIPTION)
@Documentation(OMSMOSAIC_DOCUMENTATION)
@Author(name = OMSMOSAIC_AUTHORNAMES, contact = OMSMOSAIC_AUTHORCONTACTS)
@Keywords(OMSMOSAIC_KEYWORDS)
@Label(OMSMOSAIC_LABEL)
@Name(OMSMOSAIC_NAME)
@Status(OMSMOSAIC_STATUS)
@License(OMSMOSAIC_LICENSE)
public class OmsMosaic extends HMModel {

    @Description(OMSMOSAIC_IN_FILES_DESCRIPTION)
    @In
    public List<File> inFiles;

    @Description(OMSMOSAIC_P_INTERPOLATION_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSMOSAIC_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        if (inFiles == null) {
            throw new ModelsIllegalargumentException("No input data have been provided.", this, pm);
        }

        if (inFiles != null && inFiles.size() < 2) {
            throw new ModelsIllegalargumentException("The patching module needs at least two maps to be patched.", this, pm);
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

        pm.beginTask("Calculating final bounds...", inFiles.size());
        for( File coverageFile : inFiles ) {
            GridCoverage2D coverage = OmsRasterReader.readRaster(coverageFile.getAbsolutePath());
            // pm.message(MessageFormat.format("Reading map: {0} with crs: {1}",
            // coverageFile.getAbsolutePath(),
            // CrsUtilities.getCodeFromCrs(crs)));

            if (referenceGridGeometry == null) {
                // take the first as reference
                crs = coverage.getCoordinateReferenceSystem();
                referenceGridGeometry = coverage.getGridGeometry();

                pm.message("Using crs: " + CrsUtilities.getCodeFromCrs(crs));
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

        pm.message(MessageFormat.format("Output raster will have {0} cols and {1} rows.", endWidth, endHeight));
        WritableRaster outputWR = CoverageUtilities.createWritableRaster(endWidth, endHeight, null, null,
                HMConstants.doubleNovalue);
        WritableRandomIter outputIter = RandomIterFactory.createWritable(outputWR, null);

        int offestX = Math.abs(wp);
        int offestY = Math.abs(sp);
        int index = 1;
        for( File coverageFile : inFiles ) {
            GridCoverage2D coverage = OmsRasterReader.readRaster(coverageFile.getAbsolutePath());

            RenderedImage renderedImage = coverage.getRenderedImage();
            RandomIter randomIter = RandomIterFactory.create(renderedImage, null);

            Envelope2D env = coverage.getEnvelope2D();

            GridEnvelope2D repEnv = referenceGridGeometry.worldToGrid(env);

            GridGeometry2D tmpGG = coverage.getGridGeometry();
            GridEnvelope2D tmpEnv = tmpGG.worldToGrid(env);

            int startX = (int) (repEnv.getMinX() + offestX);
            int startY = (int) (repEnv.getMinY() + offestY);

            double tmpW = tmpEnv.getWidth();
            double tmpH = tmpEnv.getHeight();
            pm.beginTask("Patch map " + index++, (int) tmpW); //$NON-NLS-1$
            for( int y = 0; y < tmpH; y++ ) {
                for( int x = 0; x < tmpW; x++ ) {
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

        outRaster = CoverageUtilities.buildCoverage("patch", outputWR, envelopeParams, crs); //$NON-NLS-1$

    }

}
