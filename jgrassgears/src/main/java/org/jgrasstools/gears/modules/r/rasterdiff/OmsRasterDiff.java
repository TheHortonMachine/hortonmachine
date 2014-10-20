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
package org.jgrasstools.gears.modules.r.rasterdiff;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_doNegatives_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_inRaster1_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_inRaster2_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_pThreshold_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
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

@Description(OMSRASTERDIFF_DESCRIPTION)
@Documentation(OMSRASTERDIFF_DOCUMENTATION)
@Author(name = OMSRASTERDIFF_AUTHORNAMES, contact = OMSRASTERDIFF_AUTHORCONTACTS)
@Keywords(OMSRASTERDIFF_KEYWORDS)
@Label(OMSRASTERDIFF_LABEL)
@Name(OMSRASTERDIFF_NAME)
@Status(OMSRASTERDIFF_STATUS)
@License(OMSRASTERDIFF_LICENSE)
public class OmsRasterDiff extends JGTModel {

    @Description(OMSRASTERDIFF_inRaster1_DESCRIPTION)
    @In
    public GridCoverage2D inRaster1;

    @Description(OMSRASTERDIFF_inRaster2_DESCRIPTION)
    @In
    public GridCoverage2D inRaster2;

    @Description(OMSRASTERDIFF_pThreshold_DESCRIPTION)
    @In
    public Double pThreshold;

    @Description(OMSRASTERDIFF_doNegatives_DESCRIPTION)
    @In
    public boolean doNegatives = true;

    @Description(OMSRASTERDIFF_outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster1, inRaster2);

        double thres = 0.0;
        if (pThreshold != null) {
            thres = pThreshold;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster1);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RandomIter r1Iter = CoverageUtilities.getRandomIterator(inRaster1);
        RandomIter r2Iter = CoverageUtilities.getRandomIterator(inRaster2);

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        pm.beginTask("Subtracting raster...", cols);
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                double r1 = r1Iter.getSampleDouble(c, r, 0);
                double r2 = r2Iter.getSampleDouble(c, r, 0);
                double diff;
                if (isNovalue(r1) && isNovalue(r2)) {
                    continue;
                } else if (isNovalue(r1)) {
                    diff = r2;
                } else if (isNovalue(r2)) {
                    diff = r1;
                } else {
                    diff = r1 - r2;
                }
                if (!doNegatives && diff < 0) {
                    diff = 0.0;
//                    diff = doubleNovalue;
                }
                if (pThreshold != null && diff < thres) {
                    diff = doubleNovalue;
                }
                outIter.setSample(c, r, 0, diff);
            }
            pm.worked(1);
        }
        pm.done();
        
        r1Iter.done();
        r2Iter.done();
        outIter.done();
        
        outRaster = CoverageUtilities.buildCoverage("corrected", outWR, regionMap, inRaster1.getCoordinateReferenceSystem());
    }

}
