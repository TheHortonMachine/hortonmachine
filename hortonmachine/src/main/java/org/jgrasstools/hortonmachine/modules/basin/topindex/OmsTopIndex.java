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
package org.jgrasstools.hortonmachine.modules.basin.topindex;

import static org.jgrasstools.gears.libs.modules.JGTConstants.BASIN;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_LABEL;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_LICENSE;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_NAME;
import static org.jgrasstools.hortonmachine.modules.basin.topindex.OmsTopIndex.OMSTOPINDEX_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

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

@Description(OMSTOPINDEX_DESCRIPTION)
@Author(name = OMSTOPINDEX_AUTHORNAMES, contact = OMSTOPINDEX_AUTHORCONTACTS)
@Keywords(OMSTOPINDEX_KEYWORDS)
@Label(OMSTOPINDEX_LABEL)
@Name(OMSTOPINDEX_NAME)
@Status(OMSTOPINDEX_STATUS)
@License(OMSTOPINDEX_LICENSE)
public class OmsTopIndex extends GridMultiProcessing {

    @Description(OMSTOPINDEX_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSTOPINDEX_inSlope_DESCRIPTION)
    @In
    public GridCoverage2D inSlope = null;

    @Description(OMSTOPINDEX_outTopindex_DESCRIPTION)
    @Out
    public GridCoverage2D outTopindex = null;

    public static final String OMSTOPINDEX_DESCRIPTION = "Topographic index calculator.";
    public static final String OMSTOPINDEX_DOCUMENTATION = "OmsTopIndex.html";
    public static final String OMSTOPINDEX_KEYWORDS = "Hydrology";
    public static final String OMSTOPINDEX_LABEL = BASIN;
    public static final String OMSTOPINDEX_NAME = "topindex";
    public static final int OMSTOPINDEX_STATUS = 40;
    public static final String OMSTOPINDEX_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSTOPINDEX_AUTHORNAMES = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo";
    public static final String OMSTOPINDEX_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSTOPINDEX_inTca_DESCRIPTION = "The map of the contributing area.";
    public static final String OMSTOPINDEX_inSlope_DESCRIPTION = "The map of slope.";
    public static final String OMSTOPINDEX_outTopindex_DESCRIPTION = "The map of the topographic index.";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outTopindex == null, doReset)) {
            return;
        }
        checkNull(inTca, inSlope);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inTca);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter tcaIter = CoverageUtilities.getRandomIterator(inTca);
        RandomIter slopeIter = CoverageUtilities.getRandomIterator(inSlope);

        WritableRaster topindexWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter topindexIter = RandomIterFactory.createWritable(topindexWR, null);

        pm.beginTask(msg.message("topindex.calculating"), nRows * nCols);
        processGrid(nCols, nRows, ( c, r ) -> {
            if (pm.isCanceled()) {
                return;
            }

            int tcaValue = tcaIter.getSample(c, r, 0);
            if (!isNovalue(tcaValue)) {
                if (slopeIter.getSampleDouble(c, r, 0) != 0) {
                    topindexIter.setSample(c, r, 0, Math.log(tcaValue / slopeIter.getSampleDouble(c, r, 0)));
                }
            }
            pm.worked(1);
        });
        pm.done();

        outTopindex = CoverageUtilities.buildCoverage("topindex", topindexWR, regionMap, inTca.getCoordinateReferenceSystem());
    }
}
