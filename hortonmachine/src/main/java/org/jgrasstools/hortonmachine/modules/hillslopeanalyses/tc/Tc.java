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
package org.jgrasstools.hortonmachine.modules.hillslopeanalyses.tc;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Subdivides the sites of a basin in the 9 topographic classes identified by the longitudinal and transversal curvatures. ")
@Documentation("Tc.html")
@Author(name = "Erica Ghesla, Antonello Andrea, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Hillslope, Curvatures")
@Label(JGTConstants.HILLSLOPE)
@Name("tc")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Tc extends JGTModel {

    @Description("The longitudinal curvatures raster.")
    @In
    public GridCoverage2D inProf = null;

    @Description("The normal curvatures raster.")
    @In
    public GridCoverage2D inTan = null;

    @Description("The threshold value for the longitudinal curvatures.")
    @In
    public double pProfthres = 0.0;

    @Description("The threshold value for the normal curvaturess.")
    @In
    public double pTanthres = 0.0;

    @Description("The map of 9 topographic classes.")
    @Out
    public GridCoverage2D outTc9 = null;

    @Description("The map of 3 aggregated fundamental topographic classes.")
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

        RenderedImage profRI = inProf.getRenderedImage();
        RandomIter profRandomIter = RandomIterFactory.create(profRI, null);
        RenderedImage tanRI = inTan.getRenderedImage();
        RandomIter tangRandomIter = RandomIterFactory.create(tanRI, null);

        WritableRaster tc3WR = CoverageUtilities.createDoubleWritableRaster(profRI.getWidth(), profRI.getHeight(), null,
                profRI.getSampleModel(), null);
        WritableRandomIter tc3Iter = RandomIterFactory.createWritable(tc3WR, null);
        WritableRaster tc9WR = CoverageUtilities.createDoubleWritableRaster(profRI.getWidth(), profRI.getHeight(), null,
                profRI.getSampleModel(), null);
        WritableRandomIter tc9Iter = RandomIterFactory.createWritable(tc9WR, null);

        // calculate ...
        pm.beginTask(msg.message("working") + "tc9...", rows); //$NON-NLS-1$ //$NON-NLS-2$
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                double tangValue = tangRandomIter.getSampleDouble(i, j, 0);
                if (isNovalue(tangValue)) {
                    tc9Iter.setSample(i, j, 0, JGTConstants.doubleNovalue);
                } else {
                    double profValue = profRandomIter.getSampleDouble(i, j, 0);
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
