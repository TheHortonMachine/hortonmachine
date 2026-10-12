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
package org.hortonmachine.gears.modules.r.rastervaluerounder;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_LABEL;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_LICENSE;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_NAME;
import static org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder.OMSRASTERVALUEROUNDER_STATUS;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;

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

@Description(OMSRASTERVALUEROUNDER_DESCRIPTION)
@Documentation(OMSRASTERVALUEROUNDER_DOCUMENTATION)
@Author(name = OMSRASTERVALUEROUNDER_AUTHORNAMES, contact = OMSRASTERVALUEROUNDER_AUTHORCONTACTS)
@Keywords(OMSRASTERVALUEROUNDER_KEYWORDS)
@Label(OMSRASTERVALUEROUNDER_LABEL)
@Name(OMSRASTERVALUEROUNDER_NAME)
@Status(OMSRASTERVALUEROUNDER_STATUS)
@License(OMSRASTERVALUEROUNDER_LICENSE)
public class OmsRasterValueRounder extends HMModel {

    @Description(OMSRASTERVALUEROUNDER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERVALUEROUNDER_P_PATTERN_DESCRIPTION)
    @In
    public String pPattern = null;

    @Description(OMSRASTERVALUEROUNDER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERVALUEROUNDER_DESCRIPTION = "Rounds the values of a raster to a number of decimals.";
    public static final String OMSRASTERVALUEROUNDER_DOCUMENTATION = """
            The pattern is a Java DecimalFormat pattern, with the dot as decimal separator whatever the \
            language of the system: 0.00 rounds to two decimals, # to the integer. The values are rounded \
            half up, so 2.5 becomes 3 and 2.25 with 0.0 becomes 2.3. The novalues stay novalue.

            The rounded values are doubles, nearest to the rounded decimals: 6.35 is not exact in \
            binary. The rounding removes the noise of the last decimals, as in the output of an \
            interpolation, and makes the files compress better; to round to steps that are not \
            decimals, as to the half meter, Mapcalc does it with round(value * 2) / 2.
            """;
    public static final String OMSRASTERVALUEROUNDER_KEYWORDS = "Round, Raster";
    public static final String OMSRASTERVALUEROUNDER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERVALUEROUNDER_NAME = "rround";
    public static final int OMSRASTERVALUEROUNDER_STATUS = 40;
    public static final String OMSRASTERVALUEROUNDER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERVALUEROUNDER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERVALUEROUNDER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERVALUEROUNDER_IN_RASTER_DESCRIPTION = "The raster of which to round the values.";
    public static final String OMSRASTERVALUEROUNDER_P_PATTERN_DESCRIPTION = "The rounding pattern, as 0.00 for two decimals.";
    public static final String OMSRASTERVALUEROUNDER_OUT_RASTER_DESCRIPTION = "The rounded raster.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }
        checkNull(inRaster, pPattern);

        DecimalFormat formatter = new DecimalFormat(pPattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        formatter.setGroupingUsed(false);

        try (HMRaster inData = HMRaster.fromGridCoverage(inRaster);
                HMRaster outData = new HMRasterWritableBuilder().setName("rounded").setTemplate(inRaster).build()) {
            inData.process(pm, "Rounding data...", ( col, row, value, cols, rows ) -> {
                if (!inData.isNovalue(value)) {
                    outData.setValue(col, row, formatter.parse(formatter.format(value)).doubleValue());
                }
            });
            outRaster = outData.buildCoverage();
        }
    }

}
