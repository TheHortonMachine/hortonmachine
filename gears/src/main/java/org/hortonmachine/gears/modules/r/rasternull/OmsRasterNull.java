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
package org.hortonmachine.gears.modules.r.rasternull;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_LABEL;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_LICENSE;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_NAME;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_STATUS;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

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

@Description(OMSRASTERNULL_DESCRIPTION)
@Documentation(OMSRASTERNULL_DOCUMENTATION)
@Author(name = OMSRASTERNULL_AUTHORNAMES, contact = OMSRASTERNULL_AUTHORCONTACTS)
@Keywords(OMSRASTERNULL_KEYWORDS)
@Label(OMSRASTERNULL_LABEL)
@Name(OMSRASTERNULL_NAME)
@Status(OMSRASTERNULL_STATUS)
@License(OMSRASTERNULL_LICENSE)
public class OmsRasterNull extends HMModel {

    @Description(OMSRASTERNULL_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERNULL_P_VALUE_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSRASTERNULL_doInverse_DESCRIPTION)
    @In
    public boolean doInverse = false;

    @Description(OMSRASTERNULL_P_NULL_DESCRIPTION)
    @In
    public Double pNull = null;

    @Description(OMSRASTERNULL_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERNULL_DESCRIPTION = "Sets the cells of a given value of a raster to novalue, or all the other cells.";
    public static final String OMSRASTERNULL_DOCUMENTATION = """
            The typical use is a value that means no data but isn't declared as novalue, as the 0 of \
            the frame of an orthophoto or the -32768 of some elevation models. The inverse switch keeps \
            only the cells of the value and sets all the others to novalue, which gives the mask of a \
            class from a raster of classes.

            The value is compared at float precision, so 1.1 finds also the cells of a float raster, \
            that stores it as 1.100000023841858: the values nearer than about one ten millionth of the \
            value are taken as equal. To null a range of values RangeLookup or Mapcalc are the tools. The cells that are novalue in \
            the input stay novalue. A null value given sets the nulled cells to that value instead of \
            the novalue, for example 0 to make a 1/0 raster with the inverse.
            """;
    public static final String OMSRASTERNULL_KEYWORDS = "Null, Novalue, Mask, Raster";
    public static final String OMSRASTERNULL_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERNULL_NAME = "rnull";
    public static final int OMSRASTERNULL_STATUS = 40;
    public static final String OMSRASTERNULL_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERNULL_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERNULL_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERNULL_IN_RASTER_DESCRIPTION = "The raster to modify.";
    public static final String OMSRASTERNULL_P_VALUE_DESCRIPTION = "The value to set to null.";
    public static final String OMSRASTERNULL_doInverse_DESCRIPTION = "If true, keeps the cells of the value and sets all the others to null.";
    public static final String OMSRASTERNULL_P_NULL_DESCRIPTION = "An optional value to give to the nulled cells instead of the novalue.";
    public static final String OMSRASTERNULL_OUT_RASTER_DESCRIPTION = "The new raster.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pValue);

        double replaceValue = pValue;
        // the values of float rasters are found also when they come as doubles
        float replaceFloat = (float) replaceValue;
        try (HMRaster inData = HMRaster.fromGridCoverage(inRaster);
                HMRaster outData = new HMRasterWritableBuilder().setName("nulled").setTemplate(inRaster).build()) {
            double nullValue = pNull != null ? pNull : outData.getNovalue();

            inData.process(pm, "Nulling data...", ( col, row, value, cols, rows ) -> {
                if (inData.isNovalue(value)) {
                    // the output starts as novalue
                    return;
                }
                boolean isTheValue = (float) value == replaceFloat || NumericsUtilities.dEq(value, replaceValue);
                if (isTheValue != doInverse) {
                    value = nullValue;
                }
                outData.setValue(col, row, value);
            });
            outRaster = outData.buildCoverage();
        }
    }

}
