package org.jgrasstools.hortonmachine.modules.hillslopeanalyses.h2cd3d;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

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
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
@Description("It calculates for each hillslope pixel its distance from the river networks, following the steepest descent (i.e. the drainage directions), considering also the vertical coordinate .")
@Documentation("H2cD3D.html")
@Author(name = "Andreis Daniele, Erica Ghesla, Rigon Riccardo")
@Keywords("Hillslope, Outlet, Distance, DrainDir, Net")
@Label(JGTConstants.HILLSLOPE)
@Name("h2cd3d")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class H2cD3D extends JGTModel {
    @Description("The map of flowdirections")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The optional map of the elevation used for 3d mode in pMode = 1.")
    @In
    public GridCoverage2D inElev = null;
    @Description("The map of the distance to the net.")
    @Out
    public GridCoverage2D outH2cD3D = null;
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    @Execute
    public void process() {
        if (!concatOr(outH2cD3D == null, doReset)) {
            return;
        }
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);

        WritableRaster h2cD3DWR = h2cd3d();
        if (h2cD3DWR != null) {
            outH2cD3D = CoverageUtilities.buildCoverage("distanceToOutlet", h2cD3DWR, regionMap,
                    inFlow.getCoordinateReferenceSystem());
        }

    }

    /**
     * Calculates the h2cd3d in every pixel of the map
     * 
     * @return
     */
    private WritableRaster h2cd3d() {
        // get rows and cols from the active region
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double dx = regionMap.get(CoverageUtilities.XRES).doubleValue();
        double dy = regionMap.get(CoverageUtilities.YRES).doubleValue();
        int[] flow = new int[2];
        int[] flow_p = new int[2];

        double oldir = 0.0, dz = 0.0, count = 0.0;

        double[] grid = new double[11];

        // grid contains the dimension of pixels according with flow directions
        grid[0] = grid[9] = grid[10] = 0;
        grid[1] = grid[5] = Math.abs(dx);
        grid[3] = grid[7] = Math.abs(dy);
        grid[2] = grid[4] = grid[6] = grid[8] = Math.sqrt(dx * dx + dy * dy);

        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);
        // setting novalue border...
        RandomIter pitRandomIter = CoverageUtilities.getRandomIterator(inElev);
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);

        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (netIter.getSampleDouble(i, j, 0) == 2)
                    flowIter.setSample(i, j, 0, 10);
            }
        }
        netIter.done();

        WritableRaster h2cdWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, flowWR.getSampleModel(), null);
        WritableRandomIter h2cdRandomIter = RandomIterFactory.createWritable(h2cdWR, null);

        pm.beginTask(msg.message("h2cd3d.workingon"), rows - 2);
        for( int j = 1; j < rows - 1; j++ ) {
            for( int i = 1; i < cols - 1; i++ ) {
                flow[0] = i;
                flow[1] = j;
                // looks for the source
                if (ModelsEngine.isSourcePixel(flowIter, flow[0], flow[1])) {
                    count = 0;
                    oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                    flow_p[0] = flow[0];
                    flow_p[1] = flow[1];
                    if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                        return null;
                    // calculates the distance from the river networks
                    while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                            && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                            && h2cdRandomIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {
                        dz = pitRandomIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                - pitRandomIter.getSampleDouble(flow[0], flow[1], 0);
                        count += Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2));
                        oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        flow_p[0] = flow[0];
                        flow_p[1] = flow[1];
                        // calls go_downstream in FluidUtils
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                    }
                    if (h2cdRandomIter.getSampleDouble(flow[0], flow[1], 0) > 0) {
                        dz = pitRandomIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                - pitRandomIter.getSampleDouble(flow[0], flow[1], 0);
                        count += Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2))
                                + h2cdRandomIter.getSampleDouble(flow[0], flow[1], 0);;
                        h2cdRandomIter.setSample(i, j, 0, count);
                    } else if (flowIter.getSampleDouble(flow[0], flow[1], 0) > 9) {
                        h2cdRandomIter.setSample(flow[0], flow[1], 0, 0);
                        dz = pitRandomIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                - pitRandomIter.getSampleDouble(flow[0], flow[1], 0);
                        count += Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2));
                        h2cdRandomIter.setSample(i, j, 0, count);
                    }
                    flow[0] = i;
                    flow[1] = j;
                    oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                    flow_p[0] = flow[0];
                    flow_p[1] = flow[1];
                    // calls go_downstream in FluidUtils
                    if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                        return null;
                    while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                            && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                            && h2cdRandomIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {
                        dz = pitRandomIter.getSampleDouble(flow_p[0], flow_p[1], 0)
                                - pitRandomIter.getSampleDouble(flow[0], flow[1], 0);
                        count -= Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2));
                        h2cdRandomIter.setSample(flow[0], flow[1], 0, count);
                        if (h2cdRandomIter.getSampleDouble(flow[0], flow[1], 0) < 0)
                            h2cdRandomIter.setSample(flow[0], flow[1], 0, 0);
                        oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        flow_p[0] = flow[0];
                        flow_p[1] = flow[1];
                        // calls go_downstream in FluidUtils
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    h2cdRandomIter.setSample(i, j, 0, doubleNovalue);
                }
            }
        }
        return h2cdWR;
    }

}
