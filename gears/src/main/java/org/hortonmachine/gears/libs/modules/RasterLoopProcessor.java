package org.hortonmachine.gears.libs.modules;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

public class RasterLoopProcessor {

    private IHMProgressMonitor pm;
    private String taskName;

    public RasterLoopProcessor( String taskName, IHMProgressMonitor pm ) {
        this.taskName = taskName;
        this.pm = pm;
    }

    public void process( IDataLoopFunction function, GridCoverage2D... rasters ) {


        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(rasters[0]);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        RandomIter[] iters = new RandomIter[rasters.length];
        for( int i = 0; i < iters.length; i++ ) {
            if (rasters != null) {
                iters[i] = CoverageUtilities.getRandomIterator(rasters[i]);
            }
        }

        try {
            pm.beginTask(taskName, rows);
            double[] values = new double[rasters.length];
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {

                    for( int i = 0; i < iters.length; i++ ) {
                        if (iters[i] != null) {
                            values[i] = iters[i].getSampleDouble(c, r, 0);
                        } else {
                            values[i] = Double.NaN;
                        }
                    }
                    double result = function.process(values);
                    outIter.setSample(c, r, 0, result);
                }
                pm.worked(1);
            }
            pm.done();
        } finally {
            for( RandomIter randomIter : iters ) {
                randomIter.done();
            }
            outIter.done();
        }

    }

}