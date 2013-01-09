/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules.r.rastervaluerounder;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_pPattern_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description(OMSRASTERVALUEROUNDER_DESCRIPTION)
@Documentation(OMSRASTERVALUEROUNDER_DOCUMENTATION)
@Author(name = OMSRASTERVALUEROUNDER_AUTHORNAMES, contact = OMSRASTERVALUEROUNDER_AUTHORCONTACTS)
@Keywords(OMSRASTERVALUEROUNDER_KEYWORDS)
@Label(OMSRASTERVALUEROUNDER_LABEL)
@Name(OMSRASTERVALUEROUNDER_NAME)
@Status(OMSRASTERVALUEROUNDER_STATUS)
@License(OMSRASTERVALUEROUNDER_LICENSE)
public class OmsRasterValueRounder extends JGTModel {

    @Description(OMSRASTERVALUEROUNDER_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERVALUEROUNDER_pPattern_DESCRIPTION)
    @In
    public String pPattern = null;

    @Description(OMSRASTERVALUEROUNDER_outRaster_DESCRIPTION)
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

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        RandomIter inRasterIter = CoverageUtilities.getRandomIterator(inRaster);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        pm.beginTask("Rounding data...", cols);
        for( int c = 0; c < cols; c++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int r = 0; r < rows; r++ ) {
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
