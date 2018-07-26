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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.debristriggers;

import static java.lang.Math.pow;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_inTca_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_outTriggers_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_pGradthres_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_pTcathres_DESCRIPTION;

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
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient;

@Description(OMSDEBRISTRIGGERCNR_DESCRIPTION)
@Author(name = OMSDEBRISTRIGGERCNR_AUTHORNAMES, contact = OMSDEBRISTRIGGERCNR_AUTHORCONTACTS)
@Keywords(OMSDEBRISTRIGGERCNR_KEYWORDS)
@Label(OMSDEBRISTRIGGERCNR_LABEL)
@Name(OMSDEBRISTRIGGERCNR_NAME)
@Status(OMSDEBRISTRIGGERCNR_STATUS)
@License(OMSDEBRISTRIGGERCNR_LICENSE)
public class OmsDebrisTriggerCnr extends HMModel {

    @Description(OMSDEBRISTRIGGERCNR_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSDEBRISTRIGGERCNR_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSDEBRISTRIGGERCNR_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSDEBRISTRIGGERCNR_pTcathres_DESCRIPTION)
    @Unit("km2")
    @In
    public double pTcathres = 10;

    @Description(OMSDEBRISTRIGGERCNR_pGradthres_DESCRIPTION)
    @Unit("degree")
    @In
    public double pGradthres = 38;

    @Description(OMSDEBRISTRIGGERCNR_outTriggers_DESCRIPTION)
    @Out
    public GridCoverage2D outTriggers = null;

    @Execute
    public void process() throws Exception {
        checkNull(inElev, inNet, inTca);

        // calculate gradient map degrees
        OmsGradient gradient = new OmsGradient();
        gradient.inElev = inElev;
        gradient.pMode = Variables.FINITE_DIFFERENCES;
        gradient.doDegrees = true;
        gradient.pm = pm;
        gradient.process();
        GridCoverage2D gradientCoverageDeg = gradient.outSlope;

        // calculate gradient map %
        gradient = new OmsGradient();
        gradient.inElev = inElev;
        gradient.pMode = Variables.FINITE_DIFFERENCES;
        gradient.doDegrees = false;
        gradient.pm = pm;
        gradient.process();
        GridCoverage2D gradientCoverageTan = gradient.outSlope;

        // ritaglio della mappa di gradient lungo il reticolo
        // idrografico ed estrazione delle sole celle con
        // * pendenza minore di 38 gradi
        // * area cumulata minore di 10 km2

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

        RenderedImage netRI = inNet.getRenderedImage();
        RandomIter netIter = RandomIterFactory.create(netRI, null);

        RenderedImage tcaRI = inTca.getRenderedImage();
        RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);

        RenderedImage gradientDegRI = gradientCoverageDeg.getRenderedImage();
        RandomIter gradientDegIter = RandomIterFactory.create(gradientDegRI, null);

        RenderedImage gradientTanRI = gradientCoverageTan.getRenderedImage();
        RandomIter gradientTanIter = RandomIterFactory.create(gradientTanRI, null);

        WritableRaster outputWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        WritableRandomIter outputIter = RandomIterFactory.createWritable(outputWR, null);

        pm.beginTask("Extracting trigger points...", cols);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double net = netIter.getSampleDouble(c, r, 0);

                // all only along the network
                if (!isNovalue(net)) {
                    double tca = tcaIter.getSampleDouble(c, r, 0);

                    // tca in km2 along the net
                    double tcaKm2 = tca * xres * yres / 1000000;

                    // gradient in degrees along the net
                    double gradientDeg = gradientDegIter.getSampleDouble(c, r, 0);

                    // gradient in tan along the net
                    double gradientTan = gradientTanIter.getSampleDouble(c, r, 0);

                    /*
                     * calculate the trigger threshold:
                     * 
                     *  S = 0.32 * A^-0.2
                     *  where:
                     *   S = gradient in m/m
                     *   A = tca in km2
                     */
                    double triggerThreshold = 0.32 * pow(tcaKm2, -0.2);

                    if (gradientTan > triggerThreshold //
                            && gradientDeg < pGradthres //
                            && tcaKm2 < pTcathres) {
                        // we have a trigger point
                        outputIter.setSample(c, r, 0, triggerThreshold);
                    }

                }

            }
            pm.worked(1);
        }
        pm.done();

        outTriggers = CoverageUtilities.buildCoverage("triggers", outputWR, regionMap, inElev.getCoordinateReferenceSystem());
    }

}
