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
package org.hortonmachine.gears.modules.r.rasteronvectorresizer;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_LABEL;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_LICENSE;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_NAME;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

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

@Description(OmsRasterResizer_DESCRIPTION)
@Documentation(OmsRasterResizer_DOCUMENTATION)
@Author(name = OmsRasterResizer_AUTHORNAMES, contact = OmsRasterResizer_AUTHORCONTACTS)
@Keywords(OmsRasterResizer_KEYWORDS)
@Label(OmsRasterResizer_LABEL)
@Name(OmsRasterResizer_NAME)
@Status(OmsRasterResizer_STATUS)
@License(OmsRasterResizer_LICENSE)
public class OmsRasterResizer extends HMModel {

    @Description(OmsRasterResizer_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OmsRasterResizer_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OmsRasterResizer_IN_MASKRASTER_DESCRIPTION)
    @In
    public GridCoverage2D inMaskRaster;

    @Description(OmsRasterResizer_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OmsRasterResizer_DESCRIPTION = "Module to resize a raster on a raster or vector using the resolution of the original raster. Snapping is applied to the first contained cell, if necessary.";
    public static final String OmsRasterResizer_DOCUMENTATION = "";
    public static final String OmsRasterResizer_KEYWORDS = "Raster, Vector, Resize";
    public static final String OmsRasterResizer_LABEL = RASTERPROCESSING;
    public static final String OmsRasterResizer_NAME = "rresizer";
    public static final int OmsRasterResizer_STATUS = 5;
    public static final String OmsRasterResizer_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OmsRasterResizer_AUTHORNAMES = "Andrea Antonello";
    public static final String OmsRasterResizer_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OmsRasterResizer_IN_VECTOR_DESCRIPTION = "The optional polygon vector to use for the bounds.";
    public static final String OmsRasterResizer_IN_MASKRASTER_DESCRIPTION = "The optional raster to use as mask.";
    public static final String OmsRasterResizer_IN_RASTER_DESCRIPTION = "The raster to resize.";
    public static final String OmsRasterResizer_OUT_RASTER_DESCRIPTION = "The resized raster.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        if (inVector != null) {
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);

            ReferencedEnvelope bounds = inVector.getBounds();
            // add a one cell buffer around the vector
            double xres = regionMap.getXres();
            double yres = regionMap.getYres();
            bounds.expandBy(2 * xres, 2 * yres);
            double n = regionMap.getNorth();
            double s = regionMap.getSouth();
            double w = regionMap.getWest();
            double e = regionMap.getEast();

            double newW = bounds.getMinX();
            if (newW < w) {
                newW = w;
            }
            double newE = bounds.getMaxX();
            if (newE > e) {
                newE = e;
            }
            double newS = bounds.getMinY();
            if (newS < s) {
                newS = s;
            }
            double newN = bounds.getMaxY();
            if (newN > n) {
                newN = n;
            }

            Envelope env = new Envelope(newW, newE, newS, newN);

            RegionMap subRegion = regionMap.toSubRegion(env);

            WritableRaster outWR = CoverageUtilities.createWritableRaster(subRegion.getCols(), subRegion.getRows(), null, null,
                    null);
            WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);
            RandomIter iter = CoverageUtilities.getRandomIterator(inRaster);
            try {
                GridGeometry2D startGG = inRaster.getGridGeometry();
                GridGeometry2D destGG = CoverageUtilities.gridGeometryFromRegionParams(subRegion,
                        inRaster.getCoordinateReferenceSystem());

                int rows = subRegion.getRows();
                int cols = subRegion.getCols();
                for( int r = 0; r < rows; r++ ) {
                    for( int c = 0; c < cols; c++ ) {
                        Coordinate coord = CoverageUtilities.coordinateFromColRow(c, r, destGG);
                        int[] cr = CoverageUtilities.colRowFromCoordinate(coord, startGG, null);
                        double value = iter.getSampleDouble(cr[0], cr[1], 0);
                        outIter.setSample(c, r, 0, value);
                    }
                }

                outRaster = CoverageUtilities.buildCoverage("resized", outWR, subRegion, inRaster.getCoordinateReferenceSystem());
            } finally {
                outIter.done();
                iter.done();
            }
        } else if (inMaskRaster != null) {
            RegionMap maskRegionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inMaskRaster);

            int rows = maskRegionMap.getRows();
            int cols = maskRegionMap.getCols();
            WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
            WritableRandomIter maskOutIter = CoverageUtilities.getWritableRandomIterator(outWR);

            RandomIter outIter = CoverageUtilities.getRandomIterator(inMaskRaster);
            RandomIter inIter = CoverageUtilities.getRandomIterator(inRaster);
            RegionMap inRegionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
            Envelope inEnvelope = inRegionMap.toEnvelope();

            GridGeometry2D inGridGeometry = inRaster.getGridGeometry();
            GridGeometry2D maskGridGeometry = inMaskRaster.getGridGeometry();

            double inRasterNv = HMConstants.getNovalue(inRaster);
            double maskRasterNv = HMConstants.getNovalue(inMaskRaster);

            try {
                pm.beginTask("Resizing raster...", rows);
                for( int r = 0; r < rows; r++ ) {
                    if (pm.isCanceled()) {
                        return;
                    }
                    for( int c = 0; c < cols; c++ ) {
                        double maskValue = outIter.getSampleDouble(c, r, 0);
                        if (!HMConstants.isNovalue(maskValue, maskRasterNv)) {
                            Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, maskGridGeometry);
                            if (inEnvelope.contains(coordinate)) {
                                int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, inGridGeometry, null);
                                double value = inIter.getSampleDouble(colRow[0], colRow[1], 0);
                                maskOutIter.setSample(c, r, 0, value);
                            } else {
                                maskOutIter.setSample(c, r, 0, inRasterNv);
                            }
                        } else {
                            maskOutIter.setSample(c, r, 0, inRasterNv);
                        }
                    }
                    pm.worked(1);
                }
                pm.done();

                outRaster = CoverageUtilities.buildCoverageWithNovalue("resized", outWR, maskRegionMap,
                        inRaster.getCoordinateReferenceSystem(), inRasterNv);
            } finally {
                inIter.done();
                maskOutIter.done();
            }

        } else {
            throw new ModelsIllegalargumentException("Either an input vector or raster need to be defined.", this);
        }
    }
}
