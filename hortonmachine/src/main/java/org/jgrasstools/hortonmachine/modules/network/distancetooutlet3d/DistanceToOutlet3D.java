package org.jgrasstools.hortonmachine.modules.network.distancetooutlet3d;
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
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Distance to outlet 3D calculates the distance of every pixel within the basin, considering also the vertical coordinate ")
@Documentation("DistanceToOutlet3D.html")
@Author(name = "Andreis Daniele, Erica Ghesla, Rigon Riccardo")
@Keywords("Geomorphology, DrainDir, Pitfiller")
@Label(JGTConstants.NETWORK)
@Name("distanceToOutlet3D")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class DistanceToOutlet3D extends JGTModel {
    @Description("The map of depitted elevation.")
    @In
    public GridCoverage2D inPit = null;
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of the distance to the outlet.")
    @Out
    public GridCoverage2D outDistance = null;
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        // create the needed data
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RandomIter pitIter = CoverageUtilities.getRandomIterator(inPit);
        WritableRaster distanceWR = d2o3d(pitIter, flowIter);
        if (distanceWR != null) {
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
            outDistance = CoverageUtilities.buildCoverage("distanceToOutlet3d", distanceWR, regionMap,
                    inFlow.getCoordinateReferenceSystem());
        } else {
            outDistance=null;
            pm.errorMessage(msg.message("distanceToOutlet3D.error"));
        }

    }

    /**
     * Calculates the distance to the outlet, in 3d and in every pixel of the map
     * 
     * 
     * @param pitIter an iterator on the elevation raster.
     * @param flowIter an iterator on the 
     * @return a WritableRaster that contains the distance to the outlet.
     */
    private WritableRaster d2o3d( RandomIter pitIter, RandomIter flowIter ) {

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        // get rows and cols from the active region
        double dx = regionMap.get(CoverageUtilities.XRES).doubleValue();
        double dy = regionMap.get(CoverageUtilities.YRES).doubleValue();

        WritableRaster distanceWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        WritableRandomIter distanceIter = CoverageUtilities.getWritableRandomIterator(distanceWR);

        int[] flow = new int[2];
        int[] flow_p = new int[2];

        // create new matrix

        double oldir = 0.0, dz = 0.0, count = 0.0;

        double[] grid = new double[11];

        // grid contains the dimension of pixels according with flow directions
        grid[0] = grid[9] = grid[10] = 0;
        grid[1] = grid[5] = Math.abs(dx);
        grid[3] = grid[7] = Math.abs(dy);
        grid[2] = grid[4] = grid[6] = grid[8] = Math.sqrt(dx * dx + dy * dy);

        pm.beginTask(msg.message("distanceToOutlet3D.workingon"), rows);
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                // looks for the source
                flow[0] = i;
                flow[1] = j;
                if (isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))) {
                    distanceIter.setSample(flow[0], flow[1], 0, doubleNovalue);

                } else {

                    if (ModelsEngine.isSourcePixel(flowIter, flow[0], flow[1])) {
                        count = 0;
                        oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        flow_p[0] = flow[0];
                        flow_p[1] = flow[1];
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && distanceIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {
                            dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0) - pitIter.getSampleDouble(flow[0], flow[1], 0);
                            count += Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2));
                            oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                            flow_p[0] = flow[0];
                            flow_p[1] = flow[1];
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                        }
                        if (distanceIter.getSampleDouble(flow[0], flow[1], 0) > 0) {
                            dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0) - pitIter.getSampleDouble(flow[0], flow[1], 0);
                            count += Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2))
                                    + distanceIter.getSampleDouble(flow[0], flow[1], 0);
                            distanceIter.setSample(i, j, 0, count);
                        } else if (flowIter.getSampleDouble(flow[0], flow[1], 0) > 9) {
                            distanceIter.setSample(flow[0], flow[1], 0, 0);
                            dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0) - pitIter.getSampleDouble(flow[0], flow[1], 0);
                            count += Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2));
                            distanceIter.setSample(i, j, 0, count);
                        }

                        flow[0] = i;
                        flow[1] = j;
                        oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        flow_p[0] = flow[0];
                        flow_p[1] = flow[1];
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && distanceIter.getSampleDouble(flow[0], flow[1], 0) <= 0 ) {
                            dz = pitIter.getSampleDouble(flow_p[0], flow_p[1], 0) - pitIter.getSampleDouble(flow[0], flow[1], 0);
                            count -= Math.sqrt(Math.pow(grid[(int) oldir], 2) + Math.pow(dz, 2));
                            distanceIter.setSample(flow[0], flow[1], 0, count);
                            if (distanceIter.getSampleDouble(flow[0], flow[1], 0) < 0)
                                distanceIter.setSample(flow[0], flow[1], 0, 0);
                            oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                            flow_p[0] = flow[0];
                            flow_p[1] = flow[1];
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        flowIter.done();
        pitIter.done();
        distanceIter.done();
        return distanceWR;

    }

}
