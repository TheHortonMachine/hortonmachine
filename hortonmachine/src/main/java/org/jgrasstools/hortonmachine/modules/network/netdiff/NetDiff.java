package org.jgrasstools.hortonmachine.modules.network.netdiff;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Calculates the difference between the value of a quantity in one point and the value of the same quantity in another point across a basin")
@Author(name = "Daniele Andreis, Erica Ghesla, Antonello Andrea, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo")
@Label(JGTConstants.NETWORK)
@Documentation("NetDiff.html")
@Keywords("Network, Pitfiller, DrainDir, FlowDirections")
@Name("netdiff")
@Status(Status.CERTIFIED)
@License("GPL3")
public class NetDiff extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of the stream.")
    @In
    public GridCoverage2D inStream = null;

    @Description("The map of to evaluate the difference.")
    @In
    public GridCoverage2D inRaster = null;

    @Description("The map of difference.")
    @Out
    public GridCoverage2D outDiff = null;
    
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();
    
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    
    @Execute
    public void process() {
        if (!concatOr(outDiff == null, doReset)) {
            return;
        }
        checkNull(inFlow, inStream);

        WritableRaster diffWR = netdif();
        if (diffWR == null) {
            return;
        } else {
            HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
            outDiff = CoverageUtilities.buildCoverage("netdiff", diffWR, regionMap, inFlow.getCoordinateReferenceSystem());

        }
    }

    /**
     * Calculates the difference map.
     * 
     * @return
     */
    private WritableRaster netdif() {
        // get rows and cols from the active region
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        int[] flow = new int[2];
        int[] oldflow = new int[2];

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        RandomIter streamIter = CoverageUtilities.getRandomIterator(inStream);
        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
        int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

        // create new matrix
        double[][] segna = new double[cols][rows];

        pm.beginTask(msg.message("working") + "h.netdif", 3 * rows);
        // First step: It marks with 1 the points which are at the upstream
        // beginning
        // of a link or stream
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                // looks for the source
                if (ModelsEngine.isSourcePixel(flowIter, flow[0], flow[1])) {
                    segna[i][j] = 1;
                } else if (!isNovalue(flowIter.getSampleDouble(i, j, 0)) && flowIter.getSampleDouble(i, j, 0) != 10.0) {
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == dir[k][2]) {
                            if (streamIter.getSampleDouble(flow[0] + dir[k][1], flow[1] + dir[k][0], 0) == streamIter
                                    .getSampleDouble(i, j, 0)) {
                                segna[i][j] = 0;
                                break;
                            } else {
                                segna[i][j] = 1;
                            }
                        }
                    }
                }
            }
            pm.worked(1);
        }
        WritableRaster diffImage = CoverageUtilities.createDoubleWritableRaster(cols, rows,
                null, inFlow.getRenderedImage().getSampleModel(), null);
        WritableRandomIter diffIter = RandomIterFactory.createWritable(diffImage, null);
        // Second step: It calculate the difference among the first and the last
        // point of a link
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (segna[i][j] > 0) {
                    flow[0] = i;
                    flow[1] = j;
                    oldflow[0] = i;
                    oldflow[1] = j;
                    if (!isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))) {
                        // call go_downstream in FluidUtils
                        ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0));
                        while( segna[flow[0]][flow[1]] < 1
                                && !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && streamIter.getSampleDouble(flow[0], flow[1], 0) == streamIter.getSampleDouble(i,
                                        j, 0) ) {
                            oldflow[0] = flow[0];
                            oldflow[1] = flow[1];
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                        }
                        diffIter.setSample(
                                i,
                                j,
                                0,
                                Math.abs(rasterIter.getSampleDouble(i, j, 0)
                                        - rasterIter.getSampleDouble(oldflow[0], oldflow[1], 0)));
                        // Assign to any point inside the link the value of the
                        // difference
                        flow[0] = i;
                        flow[1] = j;
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return null;
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                                && streamIter.getSampleDouble(flow[0], flow[1], 0) == streamIter.getSampleDouble(i,
                                        j, 0) ) {
                            diffIter.setSample(flow[0], flow[1], 0, diffIter.getSampleDouble(i, j, 0));
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return null;
                        }
                        if (flowIter.getSampleDouble(flow[0], flow[1], 0) == 10
                                && streamIter.getSampleDouble(flow[0], flow[1], 0) == streamIter.getSampleDouble(i,
                                        j, 0)) {
                            diffIter.setSample(flow[0], flow[1], 0, diffIter.getSampleDouble(i, j, 0));
                        }
                    }
                }
            }
            pm.worked(1);
        }
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (isNovalue(streamIter.getSampleDouble(i, j, 0))) {
                    diffIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        diffIter.done();
        flowIter.done();
        rasterIter.done();
        streamIter.done();
        return diffImage;
    }
}
