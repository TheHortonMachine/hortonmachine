/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.modules.network.extractnetwork;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
/**
 * <p>
 * The openmi compliant representation of the extractnetwork model. It extracts
 * the channel net from the drainage directions.
 * </p>
 * <p>
 * Usage: mode 0: h.extractnetwork --mode 0 --igrass-flow flow --igrass-tca tca
 * --threshold threshold --ograss-net net
 * </p>
 * <p>
 * Usage: mode 1: h.extractnetwork --mode 1 --igrass-flow flow --igrass-tca tca
 * --igrass-slope slope --threshold threshold --ograss-net net
 * </p>
 * <p>
 * Usage: mode 2: h.extractnetwork --mode 2 --igrass-flow flow --igrass-tca tca
 * --igrass-classi classi --threshold threshold --ograss-net net
 * </p>
 * <p>
 * It's also possible to create a ShapeFile containing the network:
 * </p>
 * <p>
 * Usage: mode 0: h.extractnetwork --mode 0 --igrass-flow flow --igrass-tca tca
 * --threshold threshold --ograss-net net --oshapefile-netshape "filePath"
 * </p>
 * <p>
 * Usage: mode 1: h.extractnetwork --mode 1 --igrass-flow flow --igrass-tca tca
 * --igrass-slope slope --threshold threshold --ograss-net net
 * --oshapefile-netshape "filePath"
 * </p>
 * <p>
 * Usage: mode 2: h.extractnetwork --mode 2 --igrass-flow flow --igrass-tca tca
 * --igrass-classi classi --threshold threshold --ograss-net net
 * --oshapefile-netshape "filePath"
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini
 *         Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo
 */
@Description("Extracts the network from an elevation model.")
@Author(name = "Erica Ghesla, Andrea Antonello, Franceschi Silvia", contact = "www.hydrologis.com")
@Keywords("Network, Vector")
@Label(JGTConstants.NETWORK)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ExtractNetwork extends JGTModel {

    /*
     * EXTERNAL VARIABLES
     */
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;
    
    @Description("The map of total contributing areas.")
    @In
    public GridCoverage2D inTca = null;
    
    @Description("The map of slope.")
    @In
    public GridCoverage2D inSlope = null;
    
    @Description("The map of aggregated topographic classes.")
    @In
    public GridCoverage2D inTc3 = null;
    
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Role(Role.PARAMETER)
    @Description("The threshold on the map.")
    @In
    public double pThres = 0;
    
    @Role(Role.PARAMETER)
    @Description("The processing mode.")
    @In
    public int pMode = 0;
    
    @Role(Role.PARAMETER)
    @Description("switch to create a featurecollection of the network (default = false).")
    @In
    public boolean doNetfc = false;

    @Description("The extracted network map.")
    @Out
    public GridCoverage2D outNet = null;
    
    @Description("The feature collection of the network.")
    @Out
    public SimpleFeatureCollection outNetfc = null;

    /*
     * INTERNAL VARIABLES
     */
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    

    private int cols;
    private int rows;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outNet == null, doReset)) {
            return;
        }
        
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        RenderedImage tcaRI = inTca.getRenderedImage();

        WritableRaster networkWR = null;
        if (pMode == 0) {
            networkWR = extractNetMode0(flowRI, tcaRI);
        } else if (pMode == 1) {
            RenderedImage slopeRI = inSlope.getRenderedImage();
            networkWR = extractNetMode1(flowRI, tcaRI, slopeRI);
        } else if (pMode == 2) {
            RenderedImage classRI = inTc3.getRenderedImage();
            networkWR = extractNetMode2(flowRI, tcaRI, classRI);
        }
        if (isCanceled(pm)) {
            return;
        }
        outNet = CoverageUtilities.buildCoverage("network", networkWR, regionMap, inFlow.getCoordinateReferenceSystem());

        if (doNetfc) {
            if (isCanceled(pm)) {
                return;
            }
            List<Integer> nstream = new ArrayList<Integer>();
            RandomIter flowIter = RandomIterFactory.create(flowRI, null);
            RandomIter networkIter = RandomIterFactory.create(networkWR, null);

            WritableRaster netNumWR = ModelsEngine.netNumbering(nstream, flowIter, networkIter, cols, rows, pm);
            CoverageUtilities.setNovalueBorder(netNumWR);
            // calculates the shape...
            outNetfc = ModelsEngine.net2ShapeOnly(flowRI, netNumWR, inFlow.getGridGeometry(), nstream, pm);
        }
    }
    /**
     * this method calculates the network using a threshold value on the
     * contributing areas or on magnitudo
     */
    private WritableRaster extractNetMode0( RenderedImage flowRI, RenderedImage tcaRI ) {
        // create new RasterData for the network matrix
        RandomIter flowRandomIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaRandomIter = RandomIterFactory.create(tcaRI, null);
        WritableRaster netImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);

        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(netImage, null);

        int flw[] = new int[2];

        pm.beginTask(msg.message("extractnetwork.extracting"), rows);
        for( int j = 0; j < rows; j++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int i = 0; i < cols; i++ ) {

                double tcaValue = tcaRandomIter.getSampleDouble(i, j, 0);
                if (!isNovalue(tcaValue) && !isNovalue(flowRandomIter.getSampleDouble(i, j, 0))) {
                    if (tcaValue >= pThres) {
                        netRandomIter.setSample(i, j, 0, 2);
                        flw[0] = i;
                        flw[1] = j;
                        if (!ModelsEngine.go_downstream(flw, flowRandomIter.getSampleDouble(flw[0], flw[1], 0)))
                            return null;
                        while( netRandomIter.getSampleDouble(flw[0], flw[1], 0) != 2 && flowRandomIter.getSampleDouble(flw[0], flw[1], 0) < 9
                                && !isNovalue(flowRandomIter.getSampleDouble(flw[0], flw[1], 0)) ) {
                            netRandomIter.setSample(flw[0], flw[1], 0, 2);
                            if (!ModelsEngine.go_downstream(flw, flowRandomIter.getSampleDouble(flw[0], flw[1], 0)))
                                return null;
                        }

                    } else if (flowRandomIter.getSampleDouble(i, j, 0) == 10) {
                        netRandomIter.setSample(i, j, 0, 2);
                    }
                } else {
                    netRandomIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
        return netImage;
    }

    /**
     * this method calculates the network imposing a threshold value on the
     * product of two quantities, for example the contributing area and the
     * slope.
     */
    private WritableRaster extractNetMode1( RenderedImage flowRI, RenderedImage tcaRI, RenderedImage slopeRI ) {

        RandomIter flowRandomIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaRandomIter = RandomIterFactory.create(tcaRI, null);
        RandomIter slopeRandomIter = RandomIterFactory.create(slopeRI, null);

        // create new RasterData for the network matrix
        WritableRaster networkWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(networkWR, null);

        int flw[] = new int[2];

        pm.beginTask(msg.message("extractnetwork.extracting"), rows);
        for( int j = 0; j < rows; j++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int i = 0; i < cols; i++ ) {
                if (!isNovalue(tcaRandomIter.getSampleDouble(i, j, 0)) && !isNovalue(flowRandomIter.getSampleDouble(i, j, 0))) {
                    if (tcaRandomIter.getSampleDouble(i, j, 0) * slopeRandomIter.getSampleDouble(i, j, 0) >= pThres) {
                        netRandomIter.setSample(i, j, 0, 2);
                        flw[0] = i;
                        flw[1] = j;
                        if (!ModelsEngine.go_downstream(flw, flowRandomIter.getSampleDouble(flw[0], flw[1], 0)))
                            return null;
                        while( netRandomIter.getSampleDouble(flw[0], flw[1], 0) != 2 && flowRandomIter.getSampleDouble(flw[0], flw[1], 0) < 9
                                && !isNovalue(flowRandomIter.getSampleDouble(flw[0], flw[1], 0)) ) {
                            netRandomIter.setSample(flw[0], flw[1], 0, 2);
                            if (!ModelsEngine.go_downstream(flw, flowRandomIter.getSampleDouble(flw[0], flw[1], 0)))
                                return null;
                        }
                    } else if (flowRandomIter.getSampleDouble(i, j, 0) == 10) {
                        netRandomIter.setSample(i, j, 0, 2);
                    }
                } else {
                    netRandomIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
        return networkWR;
    }

    /**
     * this method the network is extracted by considering only concave points
     * as being part of the channel network.
     */
    private WritableRaster extractNetMode2( RenderedImage flowRI, RenderedImage tcaRI, RenderedImage classRI ) {
        RandomIter flowRandomIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaRandomIter = RandomIterFactory.create(tcaRI, null);
        RandomIter classRandomIter = RandomIterFactory.create(classRI, null);
        WritableRaster netImage = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);

        // try the operation!!

        WritableRandomIter netRandomIter = RandomIterFactory.createWritable(netImage, null);

        int flw[] = new int[2];

        pm.beginTask(msg.message("extractnetwork.extracting"), rows);
        for( int j = 0; j < rows; j++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int i = 0; i < cols; i++ ) {
                if (!isNovalue(tcaRandomIter.getSampleDouble(i, j, 0)) && !isNovalue(flowRandomIter.getSampleDouble(i, j, 0))) {
                    if (tcaRandomIter.getSampleDouble(i, j, 0) >= pThres && classRandomIter.getSample(i, j, 0) == 15.0) {
                        netRandomIter.setSample(i, j, 0, 2);
                        flw[0] = i;
                        flw[1] = j;
                        if (!ModelsEngine.go_downstream(flw, flowRandomIter.getSampleDouble(flw[0], flw[1], 0)))
                            return null;
                        while( netRandomIter.getSampleDouble(flw[0], flw[1], 0) != 2 && flowRandomIter.getSampleDouble(flw[0], flw[1], 0) < 9
                                && !isNovalue(flowRandomIter.getSampleDouble(flw[0], flw[1], 0)) ) {
                            netRandomIter.setSample(flw[0], flw[1], 0, 2);
                            if (!ModelsEngine.go_downstream(flw, flowRandomIter.getSampleDouble(flw[0], flw[1], 0)))
                                return null;
                        }
                    } else if (flowRandomIter.getSampleDouble(i, j, 0) == 10) {
                        netRandomIter.setSample(i, j, 0, 2);
                    }
                } else {
                    netRandomIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
        return netImage;
    }
}
