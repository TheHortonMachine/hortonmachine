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
package org.hortonmachine.gears.modules.r.cutout;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

@Description(OmsCutOut.OMSCUTOUT_DESCRIPTION)
@Documentation(OmsCutOut.OMSCUTOUT_DOCUMENTATION)
@Author(name = OmsCutOut.OMSCUTOUT_AUTHORNAMES, contact = OmsCutOut.OMSCUTOUT_AUTHORCONTACTS)
@Keywords(OmsCutOut.OMSCUTOUT_KEYWORDS)
@Label(OmsCutOut.OMSCUTOUT_LABEL)
@Name(OmsCutOut.OMSCUTOUT_NAME)
@Status(OmsCutOut.OMSCUTOUT_STATUS)
@License(OmsCutOut.OMSCUTOUT_LICENSE)
public class OmsCutOut extends GridMultiProcessing {

    @Description(OMSCUTOUT_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSCUTOUT_IN_MASK_DESCRIPTION)
    @In
    public GridCoverage2D inMask;

    @Description(OMSCUTOUT_P_MAX_DESCRIPTION)
    @In
    public Double pMax;

    @Description(OMSCUTOUT_P_MIN_DESCRIPTION)
    @In
    public Double pMin;

    @Description(OMSCUTOUT_DO_INVERSE_DESCRIPTION)
    @In
    public boolean doInverse = false;

    @Description(OMSCUTOUT_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    public static final String OMSCUTOUT_DESCRIPTION = "Module for raster thresholding and masking.";
    public static final String OMSCUTOUT_DOCUMENTATION = "OmsCutOut.html";
    public static final String OMSCUTOUT_KEYWORDS = "Raster, Threshold, OmsMapcalc";
    public static final String OMSCUTOUT_LABEL = RASTERPROCESSING;
    public static final String OMSCUTOUT_NAME = "cutout";
    public static final int OMSCUTOUT_STATUS = 40;
    public static final String OMSCUTOUT_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSCUTOUT_AUTHORNAMES = "Silvia Franceschi, Andrea Antonello";
    public static final String OMSCUTOUT_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSCUTOUT_IN_RASTER_DESCRIPTION = "The map that has to be processed.";
    public static final String OMSCUTOUT_IN_MASK_DESCRIPTION = "The map to use as mask.";
    public static final String OMSCUTOUT_P_MAX_DESCRIPTION = "The upper threshold value.";
    public static final String OMSCUTOUT_P_MIN_DESCRIPTION = "The lower threshold value.";
    public static final String OMSCUTOUT_DO_INVERSE_DESCRIPTION = "Switch for doing extraction of the mask area or the inverse (negative). Default is false and extract the mask area.";
    public static final String OMSCUTOUT_OUT_RASTER_DESCRIPTION = "The processed map.";

    private RandomIter maskIter;
    private boolean doMax = false;
    private boolean doMin = false;
    private double max = -1;
    private double min = -1;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        // do autoboxing only once
        if (pMax != null) {
            max = pMax;
            doMax = true;
        }
        if (pMin != null) {
            min = pMin;
            doMin = true;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RenderedImage geodataRI = inRaster.getRenderedImage();
        RandomIter geodataIter = RandomIterFactory.create(geodataRI, null);

        double maskNv = 0;
        if (inMask != null) {
            RenderedImage maskRI = inMask.getRenderedImage();
            maskNv = HMConstants.getNovalue(inMask);
            maskIter = RandomIterFactory.create(maskRI, null);
        }

        WritableRaster outWR = CoverageUtilities.renderedImage2WritableRaster(geodataRI, false);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        try {
            double _maskNv = maskNv;
            pm.beginTask("Processing map...", nRows * nCols);
            processGrid(nCols, nRows, false, ( c, r ) -> {
                if (pm.isCanceled()) {
                    return;
                }
                pm.worked(1);
                if (maskIter != null) {
                    double maskValue = maskIter.getSampleDouble(c, r, 0);
                    if (!doInverse) {
                        if (isNovalue(maskValue, _maskNv)) {
                            outIter.setSample(c, r, 0, HMConstants.doubleNovalue);
                            return;
                        }
                    } else {
                        if (!isNovalue(maskValue, _maskNv)) {
                            outIter.setSample(c, r, 0, HMConstants.doubleNovalue);
                            return;
                        }
                    }
                }
                double value = geodataIter.getSampleDouble(c, r, 0);
                if (doMax && value > max) {
                    outIter.setSample(c, r, 0, HMConstants.doubleNovalue);
                    return;
                }
                if (doMin && value < min) {
                    outIter.setSample(c, r, 0, HMConstants.doubleNovalue);
                }
            });
            pm.done();
        } finally {
            geodataIter.done();
            outIter.done();
            if (maskIter != null) {
                maskIter.done();
            }
        }

        outRaster = CoverageUtilities.buildCoverage("cutout", outWR, regionMap, inRaster.getCoordinateReferenceSystem());

    }

    /**
     * Cut a raster on a mask using the default parameters.
     * 
     * @param raster the raster to cut.
     * @param mask the mask to apply.
     * @return the cut map.
     * @throws Exception
     */
    public static GridCoverage2D cut( GridCoverage2D raster, GridCoverage2D mask ) throws Exception {
        OmsCutOut cutDrain = new OmsCutOut();
        cutDrain.inRaster = raster;
        cutDrain.inMask = mask;
        cutDrain.process();
        return cutDrain.outRaster;
    }
}
