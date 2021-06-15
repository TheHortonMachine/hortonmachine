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
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_LABEL;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_LICENSE;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_NAME;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
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

@Description(OmsRasterOnVectorCutter_DESCRIPTION)
@Documentation(OmsRasterOnVectorCutter_DOCUMENTATION)
@Author(name = OmsRasterOnVectorCutter_AUTHORNAMES, contact = OmsRasterOnVectorCutter_AUTHORCONTACTS)
@Keywords(OmsRasterOnVectorCutter_KEYWORDS)
@Label(OmsRasterOnVectorCutter_LABEL)
@Name(OmsRasterOnVectorCutter_NAME)
@Status(OmsRasterOnVectorCutter_STATUS)
@License(OmsRasterOnVectorCutter_LICENSE)
public class OmsRasterOnVectorResizer extends HMModel {

    @Description(OmsRasterOnVectorCutter_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OmsRasterOnVectorCutter_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OmsRasterOnVectorCutter_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OmsRasterOnVectorCutter_DESCRIPTION = "Module to resize a raster on a vector region maintaining resolution and bounds snap.";
    public static final String OmsRasterOnVectorCutter_DOCUMENTATION = "";
    public static final String OmsRasterOnVectorCutter_KEYWORDS = "Raster, Vector, Intersect";
    public static final String OmsRasterOnVectorCutter_LABEL = RASTERPROCESSING;
    public static final String OmsRasterOnVectorCutter_NAME = "rvresizer";
    public static final int OmsRasterOnVectorCutter_STATUS = 5;
    public static final String OmsRasterOnVectorCutter_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OmsRasterOnVectorCutter_AUTHORNAMES = "Andrea Antonello";
    public static final String OmsRasterOnVectorCutter_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OmsRasterOnVectorCutter_IN_VECTOR_DESCRIPTION = "The polygon vector to use for the bounds.";
    public static final String OmsRasterOnVectorCutter_IN_RASTER_DESCRIPTION = "The raster to resize.";
    public static final String OmsRasterOnVectorCutter_OUT_RASTER_DESCRIPTION = "The output raster.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inVector);

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

        WritableRaster outWR = CoverageUtilities.createWritableRaster(subRegion.getCols(), subRegion.getRows(), null, null, null);
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
    }
}
