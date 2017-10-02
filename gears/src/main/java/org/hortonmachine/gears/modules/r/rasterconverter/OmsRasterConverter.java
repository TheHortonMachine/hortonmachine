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
package org.hortonmachine.gears.modules.r.rasterconverter;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_DOUBLE;
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_FLOAT;
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_INT;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
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
import oms3.annotations.UI;

@Description(OmsRasterConverter.OMSRASTERCONVERTER_DESCRIPTION)
@Documentation(OmsRasterConverter.OMSRASTERCONVERTER_DOCUMENTATION)
@Author(name = OmsRasterConverter.OMSRASTERCONVERTER_AUTHORNAMES, contact = OmsRasterConverter.OMSRASTERCONVERTER_AUTHORCONTACTS)
@Keywords(OmsRasterConverter.OMSRASTERCONVERTER_KEYWORDS)
@Label(OmsRasterConverter.OMSRASTERCONVERTER_LABEL)
@Name(OmsRasterConverter.OMSRASTERCONVERTER_NAME)
@Status(OmsRasterConverter.OMSRASTERCONVERTER_STATUS)
@License(OmsRasterConverter.OMSRASTERCONVERTER_LICENSE)
public class OmsRasterConverter extends HMModel {

    @Description(OMSRASTERCONVERTER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERCONVERTER_OUT_RASTER_TYPE)
    @UI("combo:" + TYPE_INT + "," + TYPE_FLOAT + "," + TYPE_DOUBLE)
    @In
    public String pOutType = "";

    @Description(OMSRASTERCONVERTER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERCONVERTER_DESCRIPTION = "Raster conversion module.";
    public static final String OMSRASTERCONVERTER_DOCUMENTATION = "OmsRasterConverter.html";
    public static final String OMSRASTERCONVERTER_KEYWORDS = "IO, Coverage, Raster, Convert, OmsRasterReader";
    public static final String OMSRASTERCONVERTER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERCONVERTER_NAME = "oms_rconvert";
    public static final int OMSRASTERCONVERTER_STATUS = 40;
    public static final String OMSRASTERCONVERTER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERCONVERTER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERCONVERTER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERCONVERTER_IN_RASTER_DESCRIPTION = "The input raster.";
    public static final String OMSRASTERCONVERTER_OUT_RASTER_DESCRIPTION = "The output raster.";
    public static final String OMSRASTERCONVERTER_OUT_RASTER_TYPE = "An optional type for the conversion.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);
        if (pOutType == null || pOutType.length() == 0) {
            outRaster = inRaster;
        } else {
            // convert type
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
            int nCols = regionMap.getCols();
            int nRows = regionMap.getRows();

            RandomIter inIter = CoverageUtilities.getRandomIterator(inRaster);
            WritableRaster outWritableRaster = null;
            WritableRandomIter outIter = null;
            try {
                switch( pOutType ) {
                case TYPE_INT:
                    outWritableRaster = CoverageUtilities.createWritableRaster(nCols, nRows, Integer.class, null, null);
                    outIter = CoverageUtilities.getWritableRandomIterator(outWritableRaster);
                    pm.beginTask("Converting...", nRows);
                    for( int r = 0; r < nRows; r++ ) {
                        if (pm.isCanceled()) {
                            return;
                        }
                        for( int c = 0; c < nCols; c++ ) {
                            int value = inIter.getSample(c, r, 0);
                            outIter.setSample(c, r, 0, value);

                        }
                        pm.worked(1);
                    }
                    pm.done();

                    break;
                case TYPE_FLOAT:
                    outWritableRaster = CoverageUtilities.createWritableRaster(nCols, nRows, Float.class, null, null);
                    outIter = CoverageUtilities.getWritableRandomIterator(outWritableRaster);
                    try {
                        pm.beginTask("Converting...", nRows);
                        for( int r = 0; r < nRows; r++ ) {
                            if (pm.isCanceled()) {
                                return;
                            }
                            for( int c = 0; c < nCols; c++ ) {
                                float value = inIter.getSampleFloat(c, r, 0);
                                outIter.setSample(c, r, 0, value);
                            }
                            pm.worked(1);
                        }
                        pm.done();
                    } finally {
                        outIter.done();
                    }
                    break;

                case TYPE_DOUBLE:
                default:
                    outWritableRaster = CoverageUtilities.createWritableRaster(nCols, nRows, Double.class, null, null);
                    outIter = CoverageUtilities.getWritableRandomIterator(outWritableRaster);
                    try {
                        pm.beginTask("Converting...", nRows);
                        for( int r = 0; r < nRows; r++ ) {
                            if (pm.isCanceled()) {
                                return;
                            }
                            for( int c = 0; c < nCols; c++ ) {
                                double value = inIter.getSampleDouble(c, r, 0);
                                outIter.setSample(c, r, 0, value);
                            }
                            pm.worked(1);
                        }
                        pm.done();
                    } finally {
                        outIter.done();
                    }
                    break;
                }

                outRaster = CoverageUtilities.buildCoverage("converted", outWritableRaster, regionMap,
                        inRaster.getCoordinateReferenceSystem());
            } finally {
                outIter.done();
                inIter.done();
            }
        }
    }

}
