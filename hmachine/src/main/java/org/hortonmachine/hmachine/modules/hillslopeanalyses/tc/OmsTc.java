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
package org.hortonmachine.hmachine.modules.hillslopeanalyses.tc;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_inProf_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_inTan_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_outTc3_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_outTc9_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_pProfthres_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_pTanthres_DESCRIPTION;

import java.awt.image.RenderedImage;
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
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSTC_DESCRIPTION)
@Author(name = OMSTC_AUTHORNAMES, contact = OMSTC_AUTHORCONTACTS)
@Keywords(OMSTC_KEYWORDS)
@Label(OMSTC_LABEL)
@Name(OMSTC_NAME)
@Status(OMSTC_STATUS)
@License(OMSTC_LICENSE)
public class OmsTc extends HMModel {

    @Description(OMSTC_inProf_DESCRIPTION)
    @In
    public GridCoverage2D inProf = null;

    @Description(OMSTC_inTan_DESCRIPTION)
    @In
    public GridCoverage2D inTan = null;

    @Description(OMSTC_pProfthres_DESCRIPTION)
    @In
    public double pProfthres = 0.0;

    @Description(OMSTC_pTanthres_DESCRIPTION)
    @In
    public double pTanthres = 0.0;

    @Description(OMSTC_outTc9_DESCRIPTION)
    @Out
    public GridCoverage2D outTc9 = null;

    @Description(OMSTC_outTc3_DESCRIPTION)
    @Out
    public GridCoverage2D outTc3 = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outTc3 == null, outTc9 == null, doReset)) {
            return;
        }

        checkNull(inProf, inTan);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inProf);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        WritableRaster tc3WR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);
        WritableRaster tc9WR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);
        WritableRandomIter tc3Iter = RandomIterFactory.createWritable(tc3WR, null);
        WritableRandomIter tc9Iter = RandomIterFactory.createWritable(tc9WR, null);
        
        RandomIter profIter = CoverageUtilities.getRandomIterator(inProf);
        RandomIter tangIter = CoverageUtilities.getRandomIterator(inTan);

        // calculate ...
        pm.beginTask(msg.message("working") + "tc9...", rows); //$NON-NLS-1$ //$NON-NLS-2$
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                double tangValue = tangIter.getSampleDouble(i, j, 0);
                if (isNovalue(tangValue)) {
                    tc9Iter.setSample(i, j, 0, HMConstants.doubleNovalue);
                } else {
                    double profValue = profIter.getSampleDouble(i, j, 0);
                    if (Math.abs(tangValue) <= pTanthres) {
                        if (Math.abs(profValue) <= pProfthres) {
                            tc9Iter.setSample(i, j, 0, 10);
                        } else if (profValue < -pProfthres) {
                            tc9Iter.setSample(i, j, 0, 20);
                        } else if (profValue > pProfthres) {
                            tc9Iter.setSample(i, j, 0, 30);
                        }
                    } else if (tangValue < -pTanthres) {
                        if (Math.abs(profValue) <= pProfthres) {
                            tc9Iter.setSample(i, j, 0, 40);
                        } else if (profValue < -pProfthres) {
                            tc9Iter.setSample(i, j, 0, 50);
                        } else if (profValue > pProfthres) {
                            tc9Iter.setSample(i, j, 0, 60);
                        }
                    } else if (tangValue > pTanthres) {
                        if (Math.abs(profValue) <= pProfthres) {
                            tc9Iter.setSample(i, j, 0, 70);
                        } else if (profValue < -pProfthres) {
                            tc9Iter.setSample(i, j, 0, 80);
                        } else if (profValue > pProfthres) {
                            tc9Iter.setSample(i, j, 0, 90);
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        
        profIter.done();
        tangIter.done();

        pm.beginTask(msg.message("working") + "tc3...", rows); //$NON-NLS-1$ //$NON-NLS-2$
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                double cp9Value = tc9Iter.getSampleDouble(i, j, 0);
                if (!isNovalue(cp9Value)) {
                    if (cp9Value == 70 || cp9Value == 90 || cp9Value == 30) {
                        tc3Iter.setSample(i, j, 0, 15);
                    } else if (cp9Value == 10) {
                        tc3Iter.setSample(i, j, 0, 25);
                    } else {
                        tc3Iter.setSample(i, j, 0, 35);
                    }
                } else {
                    tc3Iter.setSample(i, j, 0, cp9Value);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outTc3 = CoverageUtilities.buildCoverage("tc3", tc3WR, regionMap, inProf.getCoordinateReferenceSystem()); //$NON-NLS-1$
        outTc9 = CoverageUtilities.buildCoverage("tc9", tc9WR, regionMap, inProf.getCoordinateReferenceSystem()); //$NON-NLS-1$

    }
}
