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

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.BICUBIC;
import static org.hortonmachine.gears.libs.modules.Variables.BILINEAR;
import static org.hortonmachine.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_KEYWORDS;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_LABEL;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_LICENSE;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_NAME;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_STATUS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.EAST;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.WEST;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    @Description(OMSMOSAIC_IN_COVERAGES_DESCRIPTION)
    @In
    public List<GridCoverage2D> inCoverages;

    @Description(OMSMOSAIC_P_INTERPOLATION_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSMOSAIC_doFindSmallesresolution)
    @In
    public boolean doFindSmallesresolution = false;

    @Description(OMSMOSAIC_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    public static final String OMSMOSAIC_DESCRIPTION = "Module for raster patching.";
    public static final String OMSMOSAIC_DOCUMENTATION = "OmsMosaic.html";
    public static final String OMSMOSAIC_KEYWORDS = "OmsMosaic, Raster";
    public static final String OMSMOSAIC_LABEL = RASTERPROCESSING;
    public static final String OMSMOSAIC_NAME = "mosaic";
    public static final int OMSMOSAIC_STATUS = 40;
    public static final String OMSMOSAIC_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSMOSAIC_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSMOSAIC_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSMOSAIC_IN_FILES_DESCRIPTION = "An optional list of map files that have to be patched.";
    public static final String OMSMOSAIC_IN_COVERAGES_DESCRIPTION = "An optional list of rasters that have to be patched.";
    public static final String OMSMOSAIC_P_INTERPOLATION_DESCRIPTION = "The interpolation type to use";
    public static final String OMSMOSAIC_doFindSmallesresolution = "Force the reading of all coverages to find the smalles resolution for the final patched raster.";
    public static final String OMSMOSAIC_OUT_RASTER_DESCRIPTION = "The patched map.";

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        if (inFiles == null && inCoverages == null) {
            throw new ModelsIllegalargumentException("No input data have been provided.", this, pm);
        }
        int count = 0;
        List<FileOrCoverage> dataList = new ArrayList<>();
        if (inFiles != null) {
            for( File file : inFiles ) {
                dataList.add(new FileOrCoverage(file));
                count++;
            }
        }
        if (inCoverages != null) {
            for( GridCoverage2D coverage : inCoverages ) {
                dataList.add(new FileOrCoverage(coverage));
                count++;
            }
        }

        if (dataList.size() < 2) {
            throw new ModelsIllegalargumentException("The patching module needs at least two maps to be patched.", this, pm);
        }

        GridGeometry2D referenceGridGeometry = null;

//        double n = Double.MIN_VALUE;
//        double s = Double.MAX_VALUE;
//        double e = Double.MIN_VALUE;
//        double w = Double.MAX_VALUE;
//        int np = Integer.MIN_VALUE;
//        int sp = Integer.MAX_VALUE;
//        int ep = Integer.MIN_VALUE;
//        int wp = Integer.MAX_VALUE;

        pm.beginTask("Calculating final bounds...", count);
        double novalue = 0;
        double xRes = 0;
        double yRes = 0;
        Envelope totalEnvelope = new Envelope();
        for( FileOrCoverage data : dataList ) {
            Envelope worldEnv;
            if (referenceGridGeometry == null) {
                GridCoverage2D coverage = data.getCoverage();

                novalue = HMConstants.getNovalue(coverage);

                worldEnv = FeatureUtilities.envelopeToPolygon(coverage.getEnvelope2D()).getEnvelopeInternal();
                // take the first as reference
                crs = coverage.getCoordinateReferenceSystem();
                referenceGridGeometry = coverage.getGridGeometry();

                RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);
                xRes = regionMap.getXres();
                yRes = regionMap.getYres();

                pm.message("Using crs: " + CrsUtilities.getCodeFromCrs(crs));
            } else {
                worldEnv = FeatureUtilities.envelopeToPolygon(data.getEnvelope()).getEnvelopeInternal();
            }

            totalEnvelope.expandToInclude(worldEnv);

//            GridEnvelope2D pixelEnv = referenceGridGeometry.worldToGrid(worldEnv);
//
//            int minPX = (int) pixelEnv.getMinX();
//            int minPY = (int) pixelEnv.getMinY();
//            int maxPX = (int) pixelEnv.getMaxX();
//            int maxPY = (int) pixelEnv.getMaxY();
//            if (minPX < wp)
//                wp = minPX;
//            if (minPY < sp)
//                sp = minPY;
//            if (maxPX > ep)
//                ep = maxPX;
//            if (maxPY > np)
//                np = maxPY;
//
//            double minWX = worldEnv.getMinX();
//            double minWY = worldEnv.getMinY();
//            double maxWX = worldEnv.getMaxX();
//            double maxWY = worldEnv.getMaxY();
//            if (minWX < w)
//                w = minWX;
//            if (minWY < s)
//                s = minWY;
//            if (maxWX > e)
//                e = maxWX;
//            if (maxWY > n)
//                n = maxWY;
            pm.worked(1);
        }
        pm.done();

        int endWidth = (int) (totalEnvelope.getWidth() / xRes);
        int endHeight = (int) (totalEnvelope.getHeight() / yRes);
//        int endWidth = ep - wp;
//        int endHeight = np - sp;

        referenceGridGeometry = CoverageUtilities.gridGeometryFromRegionValues(totalEnvelope.getMaxY(), totalEnvelope.getMinY(), totalEnvelope.getMaxX(),
                totalEnvelope.getMinX(), endWidth, endHeight, crs);

        pm.message(MessageFormat.format("Output raster will have {0} cols and {1} rows.", endWidth, endHeight));
        WritableRaster outputWR = CoverageUtilities.createWritableRaster(endWidth, endHeight, null, null, novalue);
        WritableRandomIter outputIter = RandomIterFactory.createWritable(outputWR, null);

//        int offestX = Math.abs(wp);
//        int offestY = Math.abs(sp);
        int index = 1;
        for( FileOrCoverage data : dataList ) {
            GridCoverage2D coverage = data.getCoverage();
            GridGeometry2D itemGG = coverage.getGridGeometry();
            RenderedImage renderedImage = coverage.getRenderedImage();
            RandomIter randomIter = RandomIterFactory.create(renderedImage, null);
            try {
                RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);
                int rows = regionMap.getRows();
                int cols = regionMap.getCols();
                pm.beginTask("Patch map " + index++, rows); //$NON-NLS-1$
                for( int r = 0; r < rows; r++ ) {
                    for( int c = 0; c < cols; c++ ) {
                        double value = randomIter.getSampleDouble(c, r, 0);
                        Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, itemGG);
                        int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, referenceGridGeometry, null);

                        try {
                            double tmpValue = outputIter.getSampleDouble(colRow[0], colRow[1], 0);
                            if (HMConstants.isNovalue(tmpValue, novalue)) {
                                outputIter.setSample(colRow[0], colRow[1], 0, value);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    pm.worked(1);
                }
                pm.done();
            } finally {
                randomIter.done();
            }

//            Envelope2D env = coverage.getEnvelope2D();
//
//            GridEnvelope2D repEnv = referenceGridGeometry.worldToGrid(env);
//
//            GridGeometry2D tmpGG = coverage.getGridGeometry();
//            GridEnvelope2D tmpEnv = tmpGG.worldToGrid(env);
//
//            int startX = (int) (repEnv.getMinX() + offestX);
//            int startY = (int) (repEnv.getMinY() + offestY);
//
//            double tmpW = tmpEnv.getWidth();
//            double tmpH = tmpEnv.getHeight();
//            pm.beginTask("Patch map " + index++, (int) tmpW); //$NON-NLS-1$
//            for( int y = 0; y < tmpH; y++ ) {
//                for( int x = 0; x < tmpW; x++ ) {
//                    double value = randomIter.getSampleDouble(x, y, 0);
//                    outputIter.setSample(x + startX, y + startY, 0, value);
//                }
//                pm.worked(1);
//            }
//            pm.done();
//            randomIter.done();
        }

        HashMap<String, Double> envelopeParams = new HashMap<String, Double>();
        envelopeParams.put(NORTH, totalEnvelope.getMaxY());
        envelopeParams.put(SOUTH, totalEnvelope.getMinY());
        envelopeParams.put(WEST, totalEnvelope.getMinX());
        envelopeParams.put(EAST, totalEnvelope.getMaxX());

        outRaster = CoverageUtilities.buildCoverage("patch", outputWR, envelopeParams, crs); //$NON-NLS-1$

    }

    static class FileOrCoverage {
        private File file;
        private GridCoverage2D coverage;

        public FileOrCoverage( File file ) {
            this.file = file;
        }
        public Envelope2D getEnvelope() throws Exception {
            if (file != null) {
                ReferencedEnvelope env = OmsRasterReader.readEnvelope(file.getAbsolutePath());
                return new Envelope2D(env.getCoordinateReferenceSystem(), env.getMinX(), env.getMinY(), env.getWidth(),
                        env.getHeight());
            } else {
                return coverage.getEnvelope2D();
            }
        }
        public GridCoverage2D getCoverage() throws Exception {
            if (file != null) {
                return OmsRasterReader.readRaster(file.getAbsolutePath());
            }
            return coverage;
        }
        public FileOrCoverage( GridCoverage2D coverage ) {
            this.coverage = coverage;
        }
    }

}
