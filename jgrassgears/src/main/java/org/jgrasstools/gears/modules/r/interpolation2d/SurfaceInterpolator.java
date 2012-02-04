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
package org.jgrasstools.gears.modules.r.interpolation2d;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.WritableRaster;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.jgrasstools.gears.modules.r.interpolation2d.core.ISurfaceInterpolator;
import org.jgrasstools.gears.modules.r.interpolation2d.core.TPSInterpolator;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;

@Description("Thin Plate Spline Interpolator")
@Author(name = "Jan Jezek, Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Interpolation, Raster, Spline")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class SurfaceInterpolator extends JGTModel {

    @Description("The input vector map of points.")
    @In
    public SimpleFeatureCollection inVector;

    @Description("The grid on which to interpolate.")
    @In
    public GridGeometry2D inGrid = null;

    @Description("A mask raster map. Values will be computed only where the mask has values.")
    @In
    public GridCoverage2D inMask = null;

    @Description("Field from which to take the category value.")
    @In
    public String fCat;

    @Description("Interpolation mode (0 = TPS, 1 = IDW).")
    @In
    public int pMode = 0;

    @Description("The buffer to use for interpolation (default is 4).")
    @Unit("m")
    @In
    public double pBuffer = 4.0;

    @Description("Max threads to use (default 5).")
    @In
    public Integer pMaxThreads = 5;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The interpolated raster.")
    @Out
    public GridCoverage2D outRaster = null;

    private ISurfaceInterpolator interpolator;

    @Execute
    public void process() throws Exception {
        checkNull(inVector, inGrid, fCat);

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(inGrid);
        final int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        SimpleFeatureIterator featureIterator = inVector.features();
        Coordinate[] coordinates = new Coordinate[inVector.size()];
        final STRtree tree = new STRtree(coordinates.length);

        int index = 0;
        pm.beginTask("Indexing control points...", coordinates.length);
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            coordinates[index] = geometry.getCoordinate();
            double value = ((Number) feature.getAttribute(fCat)).doubleValue();
            coordinates[index].z = value;

            Envelope env = new Envelope(coordinates[index]);
            tree.insert(env, coordinates[index]);

            pm.worked(1);
        }
        tree.build();
        pm.done();

        pm.message("Indexed control points: " + coordinates.length);

        if (pMode == 1) {
            interpolator = new IDWInterpolator(pBuffer);
        } else {
            interpolator = new TPSInterpolator(pBuffer);
        }

        WritableRaster interpolatedWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null,
                JGTConstants.doubleNovalue);
        final WritableRandomIter interpolatedIter = RandomIterFactory.createWritable(interpolatedWR, null);

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(pMaxThreads);

        pm.beginTask("Performing interpolation...", rows);

        final double[] eval = new double[1];
        for( int r = 0; r < rows; r++ ) {
            final int row = r;
            Runnable runner = new Runnable(){
                public void run() {
                    try {
                        for( int c = 0; c < cols; c++ ) {
                            final DirectPosition gridToWorld = inGrid.gridToWorld(new GridCoordinates2D(c, row));
                            // System.out.println(row + "/" + c);
                            boolean doProcess = true;
                            if (inMask != null) {
                                inMask.evaluate(gridToWorld, eval);
                                if (isNovalue(eval[0])) {
                                    doProcess = false;
                                }
                            }

                            final Coordinate currentCoord = new Coordinate();
                            if (doProcess) {
                                final double[] coord = gridToWorld.getCoordinate();
                                currentCoord.x = coord[0];
                                currentCoord.y = coord[1];

                                final Envelope env = new Envelope(currentCoord.x - pBuffer, currentCoord.x + pBuffer,
                                        currentCoord.y - pBuffer, currentCoord.y + pBuffer);

                                @SuppressWarnings("unchecked")
                                final List<Coordinate> result = tree.query(env);
                                // System.out.println(row + "/" + c + "  = " + result.size());

                                // we need at least 3 points
                                if (result.size() < 4) {
                                    continue;
                                }

                                final double value = interpolator.getValue(result.toArray(new Coordinate[0]), currentCoord);
                                synchronized (interpolatedIter) {
                                    interpolatedIter.setSample(c, row, 0, value);
                                }
                            }

                        }
                        pm.worked(1);
                    } catch (TransformException e) {
                        e.printStackTrace();
                    }
                }
            };
            fixedThreadPool.execute(runner);
        }
        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("interpolatedraster", interpolatedWR, regionMap, inVector.getSchema()
                .getCoordinateReferenceSystem());

    }
}
