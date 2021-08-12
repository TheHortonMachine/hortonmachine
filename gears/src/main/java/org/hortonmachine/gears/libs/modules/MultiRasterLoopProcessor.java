package org.hortonmachine.gears.libs.modules;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

public class MultiRasterLoopProcessor {

    private IHMProgressMonitor pm;
    private String taskName;

    public MultiRasterLoopProcessor( String taskName, IHMProgressMonitor pm ) {
        this.taskName = taskName;
        this.pm = pm;
    }

    public GridCoverage2D loop( IDataLoopFunction function, Double noValue, GridCoverage2D... rasters ) {
        GridCoverage2D refRaster = null;
        for( GridCoverage2D raster : rasters ) {
            if (raster != null) {
                refRaster = raster;
                break;
            }
        }
        if (refRaster == null) {
            throw new IllegalArgumentException("At least one raster needs to be non null.");
        }
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(rasters[0]);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();
        double nv = HMConstants.doubleNovalue;
        if (noValue != null) {
            nv = noValue;
        }

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, nv);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        RandomIter[] iters = new RandomIter[rasters.length];
        for( int i = 0; i < iters.length; i++ ) {
            if (rasters[i] != null) {
                iters[i] = CoverageUtilities.getRandomIterator(rasters[i]);
            }
        }

        try {
            pm.beginTask(taskName, rows);
            double[] values = new double[rasters.length];
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return null;
                }
                for( int c = 0; c < cols; c++ ) {
                    for( int i = 0; i < iters.length; i++ ) {
                        if (iters[i] != null) {
                            values[i] = iters[i].getSampleDouble(c, r, 0);
                        } else {
                            values[i] = nv;
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
                if (randomIter != null) {
                    randomIter.done();
                }
            }
            outIter.done();
        }

        return CoverageUtilities.buildCoverageWithNovalue("raster", outWR, regionMap, rasters[0].getCoordinateReferenceSystem(),
                noValue);
    }

}
