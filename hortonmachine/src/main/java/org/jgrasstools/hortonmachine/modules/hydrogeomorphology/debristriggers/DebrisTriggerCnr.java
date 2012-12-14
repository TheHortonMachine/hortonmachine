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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.debristriggers;

import static java.lang.Math.pow;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.gradient.Gradient;

@Description("Module for extraction of debris trigger points along the network following the CNR methodology.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Debris, Trigger, Raster")
@Name("debristrigger")
@Label(JGTConstants.HYDROGEOMORPHOLOGY)
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class DebrisTriggerCnr extends JGTModel {

    @Description("The map of elevation.")
    @In
    public GridCoverage2D inElev = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The map of tca.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The tca threshold to use (default = 10 km2).")
    @Unit("km2")
    @In
    public double pTcathres = 10;

    @Description("The gradient threshold to use (default = 38 deg).")
    @Unit("degree")
    @In
    public double pGradthres = 38;

    @Description("The trigger map.")
    @Out
    public GridCoverage2D outTriggers = null;

    @Execute
    public void process() throws Exception {
        checkNull(inElev, inNet, inTca);

        // calculate gradient map degrees
        Gradient gradient = new Gradient();
        gradient.inElev = inElev;
        gradient.pMode = 0;
        gradient.doDegrees = true;
        gradient.pm = pm;
        gradient.process();
        GridCoverage2D gradientCoverageDeg = gradient.outSlope;

        // calculate gradient map %
        gradient = new Gradient();
        gradient.inElev = inElev;
        gradient.pMode = 0;
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

        WritableRaster outputWR = CoverageUtilities
                .createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter outputIter = RandomIterFactory.createWritable(outputWR, null);

        pm.beginTask("Extracting trigger points...", cols);
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
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
