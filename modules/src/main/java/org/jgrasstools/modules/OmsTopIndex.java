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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_inSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_inTca_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTOPINDEX_outTopindex_DESCRIPTION;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
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
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description(OMSTOPINDEX_DESCRIPTION)
@Author(name = OMSTOPINDEX_AUTHORNAMES, contact = OMSTOPINDEX_AUTHORCONTACTS)
@Keywords(OMSTOPINDEX_KEYWORDS)
@Label(OMSTOPINDEX_LABEL)
@Name(OMSTOPINDEX_NAME)
@Status(OMSTOPINDEX_STATUS)
@License(OMSTOPINDEX_LICENSE)
public class OmsTopIndex extends JGTModel {

    @Description(OMSTOPINDEX_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSTOPINDEX_inSlope_DESCRIPTION)
    @In
    public GridCoverage2D inSlope = null;

    @Description(OMSTOPINDEX_outTopindex_DESCRIPTION)
    @Out
    public GridCoverage2D outTopindex = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outTopindex == null, doReset)) {
            return;
        }
        checkNull(inTca, inSlope);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inTca);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter tcaIter = CoverageUtilities.getRandomIterator(inTca);
        RandomIter slopeIter = CoverageUtilities.getRandomIterator(inSlope);

        WritableRaster topindexWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter topindexIter = RandomIterFactory.createWritable(topindexWR, null);

        pm.beginTask(msg.message("topindex.calculating"), nRows);
        for( int r = 0; r < nRows; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 0; c < nCols; c++ ) {
                double tcaValue = tcaIter.getSampleDouble(c, r, 0);
                if (!isNovalue(tcaValue)) {
                    if (slopeIter.getSampleDouble(c, r, 0) != 0) {
                        topindexIter.setSample(c, r, 0, Math.log(tcaValue / slopeIter.getSampleDouble(c, r, 0)));
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        outTopindex = CoverageUtilities.buildCoverage("topindex", topindexWR, regionMap, inTca.getCoordinateReferenceSystem());
    }
}
