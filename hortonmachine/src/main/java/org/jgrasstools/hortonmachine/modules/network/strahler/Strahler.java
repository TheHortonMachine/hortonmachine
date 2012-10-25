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
package org.jgrasstools.hortonmachine.modules.network.strahler;

import static java.lang.Math.max;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Calculates the Strahler order on a basin.")
@Author(name = "Erica Ghesla, Antonello Andrea, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Network, Strahler")
@Label(JGTConstants.NETWORK)
@Name("strahler")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class Strahler extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The map of the strahler order.")
    @Out
    public GridCoverage2D outStrahler = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outStrahler == null, doReset)) {
            return;
        }

        checkNull(inFlow);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        RandomIter flowIter;

        int[] flow = new int[2];
        // it memorize the number of pixel which are draining into the pixel examine.
        int contr = 0;
        int counter = 0, io, jo, s = 0;

        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        int[] vett_contr;
        double max;
        if (inNet != null) {
            WritableRandomIter tmpIter = RandomIterFactory.createWritable(flowWR, null);
            RenderedImage netRI = inNet.getRenderedImage();
            RandomIter netRandomIter = RandomIterFactory.create(netRI, null);
            for( int r = 0; r < nRows; r++ ) {
                for( int c = 0; c < nCols; c++ ) {
                    if (isNovalue(netRandomIter.getSampleDouble(c, r, 0))) {
                        tmpIter.setSample(c, r, 0, JGTConstants.doubleNovalue);
                    }
                }
            }
            netRandomIter.done();
            flowIter = tmpIter;
        } else {
            flowIter = RandomIterFactory.create(flowWR, null);
        }

        /*
         * initialize the iterator for the map and create the output image
         */
        WritableRaster strahlerWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null,
                JGTConstants.doubleNovalue);
        WritableRandomIter strahlerIter = RandomIterFactory.createWritable(strahlerWR, null);
        // start to calculate the output map.

        pm.beginTask("Calculating Strahler order...", nRows * 2);
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                flow[0] = c;
                flow[1] = r;
                /* 
                 * verify if the pixel is a source. If it is then set the value to 1.
                 */
                if (ModelsEngine.isSourcePixel(flowIter, flow[0], flow[1])) {
                    strahlerIter.setSample(flow[0], flow[1], 0, 1.0);

                    /*
                     * start wandering downstream 
                     */
                    if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                        throw new ModelsIllegalargumentException(
                                "Unable to go further downstream at: " + flow[0] + "/" + flow[1], this);
                    /*
                     * as long as it isn't an outlet point and both flow and net have valid value,
                     * loop downstream.
                     */
                    while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                            && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                        contr = 0;
                        /*
                         * Verify if the pixel have a number of pixel which are drained into greater than 1.
                         */
                        vett_contr = new int[10];
                        for( int k = 1; k <= 8; k++ ) {
                            int col = flow[0] + dir[k][1];
                            int row = flow[1] + dir[k][0];
                            double direction = flowIter.getSampleDouble(col, row, 0);
                            if (isNovalue(direction))
                                continue;
                            int tmpDir = dir[k][2];
                            if (direction == tmpDir) {
                                contr += 1;
                                vett_contr[contr] = k;
                            }
                        }

                        if (contr > 1)
                        /*
                         * If the number of pixel which are going in this pixel is major than 1
                         * then verify which has the larger strahler number. 
                         */
                        {
                            max = 0;
                            for( int ii = 1; ii <= contr; ii++ ) {
                                s = vett_contr[ii];
                                io = flow[0] + dir[s][1];
                                jo = flow[1] + dir[s][0];
                                double strahler = strahlerIter.getSampleDouble(io, jo, 0);
                                max = max(max, strahler);
                            }
                            counter = 0;
                            for( int ii = 1; ii <= contr; ii++ ) {
                                s = vett_contr[ii];
                                io = flow[0] + dir[s][1];
                                jo = flow[1] + dir[s][0];
                                if (max == strahlerIter.getSampleDouble(io, jo, 0))
                                    counter += 1;
                            }
                            /*
                             * if counter is greater than 1 then the strahler order is
                             * equal to the previous plus 1, otherwise is equal to the previus.
                             */
                            if (counter > 1) {
                                strahlerIter.setSample(flow[0], flow[1], 0, max + 1);
                            }
                            if (counter == 1) {
                                strahlerIter.setSample(flow[0], flow[1], 0, max);
                            }
                        } else
                        /*
                         * If there is only one drained pixel then the order is equal to this pixel.
                         */
                        {
                            s = vett_contr[1];
                            io = flow[0] + dir[s][1];
                            jo = flow[1] + dir[s][0];
                            max = strahlerIter.getSampleDouble(io, jo, 0);
                            strahlerIter.setSample(flow[0], flow[1], 0, max);
                        }
                        max = strahlerIter.getSampleDouble(flow[0], flow[1], 0);

                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            throw new ModelsIllegalargumentException("Unable to go further downstream at: " + flow[0] + "/"
                                    + flow[1], this);
                        /*
                         * Go to the next pixel, if the order of previuos pixel is lesser than the next value then break and keep the old value.
                         */
                        if (strahlerIter.getSampleDouble(flow[0], flow[1], 0) > max)
                            break;
                    }
                }
            }
            pm.worked(1);
        }

        for( int j = 0; j < nRows; j++ ) {
            for( int i = 0; i < nCols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                vett_contr = new int[10];
                contr = 0;
                /*
                 * calcolo l'ordine anche per il pixel di uscita: primo passo calcolo il numero dei
                 * pixel drenenti
                 */
                if (flowIter.getSampleDouble(flow[0], flow[1], 0) == 10) {
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == dir[k][2]) {
                            contr += 1;
                            vett_contr[contr] = k;
                        }
                    }
                    /* calcolo il max valore di strahler */
                    max = 0;
                    for( int ii = 1; ii <= contr; ii++ ) {
                        s = vett_contr[ii];
                        io = flow[0] + dir[s][1];
                        jo = flow[1] + dir[s][0];
                        if (max < strahlerIter.getSampleDouble(io, jo, 0))
                            max = strahlerIter.getSampleDouble(io, jo, 0);
                    }
                    /*
                     * calcolo quanti pixel (conta) presentano massimo valore di strahler
                     */
                    counter = 0;
                    for( int ii = 1; ii <= contr; ii++ ) {
                        s = vett_contr[ii];
                        io = flow[0] + dir[s][1];
                        jo = flow[1] + dir[s][0];
                        if (max == strahlerIter.getSampleDouble(io, jo, 0))
                            counter += 1;
                    }
                    /*
                     * se conta e' maggiore di 1 si aumenta il numero di strahler di 1
                     */
                    if (counter > 1)
                        strahlerIter.setSample(flow[0], flow[1], 0, max + 1);
                    if (counter == 1)
                        strahlerIter.setSample(flow[0], flow[1], 0, max);
                }
            }
            pm.worked(1);
        }
        pm.done();
        for( int j = 0; j < nRows; j++ ) {
            for( int i = 0; i < nCols; i++ ) {
                if (strahlerIter.getSampleDouble(i, j, 0) == 0.0)
                    strahlerIter.setSample(i, j, 0, JGTConstants.doubleNovalue);
            }
        }
        strahlerIter.done();
        flowIter.done();

        outStrahler = CoverageUtilities.buildCoverage("strahler", strahlerWR, regionMap, inFlow.getCoordinateReferenceSystem());

    }

}
