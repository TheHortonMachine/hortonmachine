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
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_BYTE;
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_DOUBLE;
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_FLOAT;
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_INT;
import static org.hortonmachine.gears.libs.modules.Variables.TYPE_SHORT;

import java.awt.image.WritableRaster;

import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
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
    @UI("combo:" + TYPE_BYTE + "," + TYPE_SHORT + "," + TYPE_INT + "," + TYPE_FLOAT + "," + TYPE_DOUBLE)
    @In
    public String pOutType = "";

    @Description(OMSRASTERCONVERTER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERCONVERTER_DESCRIPTION = "Converts a raster to another data type, byte, short, integer, float or double, keeping its novalues.";
    public static final String OMSRASTERCONVERTER_DOCUMENTATION = """
            The values are rounded to the nearest integer for the byte, short and integer types, \
            and to about 7 significant digits for the float one, that so halves the size of a \
            double raster. The short type, from -32768 to 32767, takes half the space of the \
            integer one and suits the elevations in meters; the byte type, from 0 to 254, a \
            quarter, and suits the classes and the masks. The novalue of the input, whatever it \
            is, becomes the -9999 of the HortonMachine, or 255 for the byte type, as the values \
            that don't fit in the type. A cell whose value is the novalue, as a -9999 in an \
            integer raster, is read as novalue too.

            Without a type the raster is only read and written again, which converts its format, \
            as an ASCII grid to a GeoTIFF.

            The bounds, the resolution or the rows and columns, all optional, read only a part of \
            the raster or resample it, with the nearest cell, as the raster reader does.
            """;
    public static final String OMSRASTERCONVERTER_KEYWORDS = "Raster, Conversion, Type, Integer, Float";
    public static final String OMSRASTERCONVERTER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERCONVERTER_NAME = "oms_rconvert";
    public static final int OMSRASTERCONVERTER_STATUS = 40;
    public static final String OMSRASTERCONVERTER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERCONVERTER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERCONVERTER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERCONVERTER_IN_RASTER_DESCRIPTION = "The raster to convert.";
    public static final String OMSRASTERCONVERTER_OUT_RASTER_DESCRIPTION = "The converted raster.";
    public static final String OMSRASTERCONVERTER_OUT_RASTER_TYPE = "The data type of the output: byte, short, integer, float or double. If not given, the type doesn't change.";

    /**
     * The novalue of the byte rasters, that are unsigned: the -9999 of the other types doesn't fit.
     */
    public static final int OMSRASTERCONVERTER_BYTE_NOVALUE = 255;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);
        if (pOutType == null || pOutType.length() == 0) {
            outRaster = inRaster;
            return;
        }

        Class< ? > type;
        double outNovalue;
        // the integer types round the values and keep only the ones in their range
        boolean isInteger = false;
        long min = 0;
        long max = 0;
        switch( pOutType ) {
        case TYPE_BYTE:
            type = Byte.class;
            outNovalue = OMSRASTERCONVERTER_BYTE_NOVALUE;
            isInteger = true;
            min = 0;
            max = OMSRASTERCONVERTER_BYTE_NOVALUE - 1;
            break;
        case TYPE_SHORT:
            type = Short.class;
            outNovalue = HMConstants.shortNovalue;
            isInteger = true;
            min = Short.MIN_VALUE;
            max = Short.MAX_VALUE;
            break;
        case TYPE_INT:
            type = Integer.class;
            outNovalue = HMConstants.intNovalue;
            isInteger = true;
            min = Integer.MIN_VALUE;
            max = Integer.MAX_VALUE;
            break;
        case TYPE_FLOAT:
            type = Float.class;
            outNovalue = HMConstants.floatNovalue;
            break;
        case TYPE_DOUBLE:
            type = Double.class;
            outNovalue = HMConstants.doubleNovalue;
            break;
        default:
            throw new ModelsIllegalargumentException("Unknown type: " + pOutType, this, pm);
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double inNovalue = HMConstants.getNovalue(inRaster);

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, type, null, outNovalue);
        RandomIter inIter = CoverageUtilities.getRandomIterator(inRaster);
        try {
            pm.beginTask("Converting...", rows);
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {
                    double value = inIter.getSampleDouble(c, r, 0);
                    if (HMConstants.isNovalue(value, inNovalue)) {
                        continue;
                    }
                    if (isInteger) {
                        long rounded = Math.round(value);
                        if (rounded < min || rounded > max) {
                            continue;
                        }
                        outWR.setSample(c, r, 0, (int) rounded);
                    } else if (type == Float.class) {
                        outWR.setSample(c, r, 0, (float) value);
                    } else {
                        outWR.setSample(c, r, 0, value);
                    }
                }
                pm.worked(1);
            }
            pm.done();
        } finally {
            inIter.done();
        }

        outRaster = CoverageUtilities.buildCoverageWithNovalue("converted", outWR, regionMap,
                inRaster.getCoordinateReferenceSystem(), outNovalue);
    }

}
