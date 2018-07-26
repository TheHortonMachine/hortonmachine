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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_P_PATTERN_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;
import java.text.DecimalFormat;

import javax.media.jai.iterator.RandomIter;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

    private DecimalFormat formatter = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        checkNull(inRaster, pPattern);

        formatter = new DecimalFormat(pPattern);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
        RandomIter inRasterIter = CoverageUtilities.getRandomIterator(inRaster);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        pm.beginTask("Rounding data...", rows);
        for( int r = 0; r < rows; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 0; c < cols; c++ ) {
                double value = inRasterIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value)) {
                    String formatted = formatter.format(value);
                    value = Double.parseDouble(formatted);
                    outIter.setSample(c, r, 0, value);
                } else {
                    outIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outIter.done();

        outRaster = CoverageUtilities.buildCoverage("rounded", outWR, regionMap, inRaster.getCoordinateReferenceSystem());
    }

}
