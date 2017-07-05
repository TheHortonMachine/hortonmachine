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
package org.jgrasstools.gears.modules.r.rasternull;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_IN_RASTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_OUT_RASTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_P_NULL_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERNULL_P_VALUE_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.WritableRaster;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;

@Description(OMSRASTERNULL_DESCRIPTION)
@Documentation(OMSRASTERNULL_DOCUMENTATION)
@Author(name = OMSRASTERNULL_AUTHORNAMES, contact = OMSRASTERNULL_AUTHORCONTACTS)
@Keywords(OMSRASTERNULL_KEYWORDS)
@Label(OMSRASTERNULL_LABEL)
@Name(OMSRASTERNULL_NAME)
@Status(OMSRASTERNULL_STATUS)
@License(OMSRASTERNULL_LICENSE)
public class OmsRasterNull extends JGTModel {

    @Description(OMSRASTERNULL_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERNULL_P_VALUE_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description("If true, sets everything else to null.")
    @In
    public boolean doInverse = false;

    @Description(OMSRASTERNULL_P_NULL_DESCRIPTION)
    @In
    public Double pNull = null;

    @Description(OMSRASTERNULL_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pValue);

        double replaceValue = pValue;
        double nullValue = JGTConstants.doubleNovalue;
        if (pNull != null) {
            nullValue = pNull;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
        RandomIter inRasterIter = CoverageUtilities.getRandomIterator(inRaster);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        pm.beginTask("Nulling data...", cols);
        for( int c = 0; c < cols; c++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int r = 0; r < rows; r++ ) {
                double value = inRasterIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value)) {
                    if (doInverse) {
                        if (!NumericsUtilities.dEq(value, replaceValue)) {
                            value = doubleNovalue;
                        }
                    } else {
                        if (NumericsUtilities.dEq(value, replaceValue)) {
                            value = nullValue;
                        }
                    }
                    outIter.setSample(c, r, 0, value);
                } else {
                    outIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        inRasterIter.done();
        outIter.done();

        outRaster = CoverageUtilities.buildCoverage("nulled", outWR, regionMap, inRaster.getCoordinateReferenceSystem());
    }

}
