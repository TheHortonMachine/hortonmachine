package org.jgrasstools.hortonmachine.modules.geomorphology.gc;

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
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
@Description("Subdivides the sites of a basin in 11 topographic classes.")
@Documentation("GC.html")
@Author(name = "Daniele Andreis,Erica Ghesla, Cozzini Andrea, Rigon Riccardo")
@Keywords("Geomorphology, Tc, Slope, ExtractNetwork")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("gc")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Gc extends JGTModel {
    @Description("The map of the slope")
    @In
    public GridCoverage2D inSlope = null;

    @Description("The map of with the network")
    @In
    public GridCoverage2D inNetwork = null;

    @Description("The map of with the Thopological classes cp9")
    @In
    public GridCoverage2D inCp9 = null;

    @Description("The gradient formula mode (0 = finite differences, 1 = horn, 2 = evans).")
    @In
    public int pTh = 0;
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map with the geomorphological classes")
    @Out
    public GridCoverage2D outClasses = null;

    @Description("The map with the geomorphological classes")
    @Out
    public GridCoverage2D outAggregateClasses = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    /**
     * The region map for the simulation.
     */
    private HashMap<String, Double> regionMap = null;

    @Execute
    public void process() {
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inSlope);
        if (regionMap != null) {
            checkNull(inSlope, inNetwork, inCp9);
            WritableRaster[] gcClasses = createGCRaster();
            checkNull(gcClasses[0], gcClasses[1]);
            outClasses = CoverageUtilities.buildCoverage("gcClasses", gcClasses[0], regionMap,
                    inSlope.getCoordinateReferenceSystem());
            outAggregateClasses = CoverageUtilities.buildCoverage("gcAggregateClasses", gcClasses[1], regionMap,
                    inSlope.getCoordinateReferenceSystem());
        } else {
            throw new IllegalArgumentException();
        }

    }

    private WritableRaster[] createGCRaster() {
        // get rows and cols from the active region

        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RandomIter slopeIter = CoverageUtilities.getRandomIterator(inSlope);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNetwork);
        RandomIter cp9Iter = CoverageUtilities.getRandomIterator(inCp9);

        WritableRaster cpClassWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);
        WritableRandomIter cpClassIter = RandomIterFactory.createWritable(cpClassWR, null);

        WritableRaster cpAggClassWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);
        WritableRandomIter cpAggClassIter = RandomIterFactory.createWritable(cpAggClassWR, null);
        // calculate ...

        pm.beginTask(msg.message("working") + "gc... (1/2)", rows);
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                // individuates the pixel with a slope greater than the
                // threshold
                if (slopeIter.getSampleDouble(i, j, 0) >= pTh) {
                    cpClassIter.setSample(i, j, 0, 110);
                }
                // individuates the network
                else if (netIter.getSampleDouble(i, j, 0) == 2) {
                    cpClassIter.setSample(i, j, 0, 100);
                } else {
                    cpClassIter.setSample(i, j, 0, cp9Iter.getSampleDouble(i, j, 0));
                }
                if (isNovalue(slopeIter.getSampleDouble(i, j, 0))) {
                    cpClassIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        netIter = null;
        slopeIter = null;

        pm.beginTask(msg.message("working") + "gc... (2/2)", rows);
        // aggregation of these classes:
        // 15 ? non-channeled valley sites (classes 70, 90, 30 )
        // 25 ? planar sites (class 10)
        // 35 ? channel sites (class 100)
        // 45 ? hillslope sites (classes 20, 40, 50, 60, 80)
        // 55 ? ravine sites (slope > critic value) (class 110).
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (cpClassIter.getSample(i, j, 0) == 70 || cpClassIter.getSampleDouble(i, j, 0) == 90
                        || cpClassIter.getSampleDouble(i, j, 0) == 30) {
                    cpAggClassIter.setSample(i, j, 0, 15);
                } else if (cpClassIter.getSampleDouble(i, j, 0) == 10) {
                    cpAggClassIter.setSample(i, j, 0, 25);
                } else if (cpClassIter.getSampleDouble(i, j, 0) == 100) {
                    cpAggClassIter.setSample(i, j, 0, 35);
                } else if (cpClassIter.getSampleDouble(i, j, 0) == 110) {
                    cpAggClassIter.setSample(i, j, 0, 55);
                } else if (!isNovalue(cpClassIter.getSampleDouble(i, j, 0))) {
                    cpAggClassIter.setSample(i, j, 0, 45);
                } else if (isNovalue(cpClassIter.getSampleDouble(i, j, 0))) {
                    cpAggClassIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
        return new WritableRaster[]{cpClassWR, cpAggClassWR};

    }

}
