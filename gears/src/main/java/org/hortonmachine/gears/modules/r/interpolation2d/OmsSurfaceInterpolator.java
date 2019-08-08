/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.modules.r.interpolation2d;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_F_CAT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_IN_GRID_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_IN_MASK_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_P_BUFFER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_P_MAX_THREADS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_P_MODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.IDW;
import static org.hortonmachine.gears.libs.modules.Variables.TPS;

import java.awt.image.WritableRaster;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.ISurfaceInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.TPSInterpolator;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

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
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OMSSURFACEINTERPOLATOR_DESCRIPTION)
@Documentation(OMSSURFACEINTERPOLATOR_DOCUMENTATION)
@Author(name = OMSSURFACEINTERPOLATOR_AUTHORNAMES, contact = OMSSURFACEINTERPOLATOR_AUTHORCONTACTS)
@Keywords(OMSSURFACEINTERPOLATOR_KEYWORDS)
@Label(OMSSURFACEINTERPOLATOR_LABEL)
@Name(OMSSURFACEINTERPOLATOR_NAME)
@Status(OMSSURFACEINTERPOLATOR_STATUS)
@License(OMSSURFACEINTERPOLATOR_LICENSE)
public class OmsSurfaceInterpolator extends HMModel {

    @Description(OMSSURFACEINTERPOLATOR_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSSURFACEINTERPOLATOR_IN_GRID_DESCRIPTION)
    @In
    public GridCoverage2D inGrid = null;

    @Description(OMSSURFACEINTERPOLATOR_IN_MASK_DESCRIPTION)
    @In
    public GridCoverage2D inMask = null;

    @Description(OMSSURFACEINTERPOLATOR_F_CAT_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSSURFACEINTERPOLATOR_P_MODE_DESCRIPTION)
    @UI("combo:" + TPS + "," + IDW)
    @In
    public String pMode = TPS;

    @Description(OMSSURFACEINTERPOLATOR_P_BUFFER_DESCRIPTION)
    @Unit("m")
    @In
    public double pBuffer = 4.0;

    @Description(OMSSURFACEINTERPOLATOR_P_MAX_THREADS_DESCRIPTION)
    @In
    public int pMaxThreads = getDefaultThreadsNum();

    @Description(OMSSURFACEINTERPOLATOR_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    private ISurfaceInterpolator interpolator;

    private STRtree coordinatesSpatialTree;

    private GridGeometry2D gridGeometry;

    @Execute
    public void process() throws Exception {
        checkNull(inGrid);

        gridGeometry = inGrid.getGridGeometry();
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inGrid);
        final int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        coordinatesSpatialTree = new STRtree();
        if (inVector != null) {
            checkNull(fCat);
            GeometryDescriptor geometryDescriptor = inVector.getSchema().getGeometryDescriptor();
            if (!EGeometryType.isPoint(geometryDescriptor)) {
                throw new ModelsIllegalargumentException("The geometry has to be a point geometry.", this, pm);
            }
            SimpleFeatureIterator featureIterator = inVector.features();
            Coordinate[] coordinates = new Coordinate[inVector.size()];

            int index = 0;
            pm.beginTask("Indexing control points...", coordinates.length);
            while( featureIterator.hasNext() ) {
                SimpleFeature feature = featureIterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                coordinates[index] = geometry.getCoordinate();
                double value = ((Number) feature.getAttribute(fCat)).doubleValue();
                coordinates[index].z = value;

                Envelope env = new Envelope(coordinates[index]);
                coordinatesSpatialTree.insert(env, coordinates[index]);

                pm.worked(1);
            }
            pm.done();
            pm.message("Indexed control points: " + coordinates.length);
        } else {
            // create it from grid
            pm.beginTask("Indexing control points...", cols);
            RandomIter inIter = CoverageUtilities.getRandomIterator(inGrid);
            int count = 0;
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = inIter.getSampleDouble(c, r, 0);
                    if (!HMConstants.isNovalue(value)) {
                        Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
                        coordinate.z = value;
                        Envelope env = new Envelope(coordinate);
                        coordinatesSpatialTree.insert(env, coordinate);
                        count++;
                    }
                }
                pm.worked(1);
            }
            pm.done();
            pm.message("Indexed control points (from input grid): " + count);
        }
        coordinatesSpatialTree.build();

        if (pMode.equals(IDW)) {
            interpolator = new IDWInterpolator(pBuffer);
        } else {
            interpolator = new TPSInterpolator(pBuffer);
        }

        WritableRaster interpolatedWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        final WritableRandomIter interpolatedIter = RandomIterFactory.createWritable(interpolatedWR, null);

        boolean doMultiThread = pMaxThreads > 1;

        ExecutorService fixedThreadPool = null;
        if (doMultiThread)
            fixedThreadPool = Executors.newFixedThreadPool(pMaxThreads);

        pm.beginTask("Performing interpolation...", rows);

        final double[] eval = new double[1];
        for( int r = 0; r < rows; r++ ) {
            final int row = r;
            if (doMultiThread) {
                Runnable runner = new Runnable(){
                    public void run() {
                        processing(cols, coordinatesSpatialTree, interpolatedIter, eval, row);
                    }
                };
                fixedThreadPool.execute(runner);
            } else {
                processing(cols, coordinatesSpatialTree, interpolatedIter, eval, row);
            }
        }

        if (doMultiThread) {
            try {
                fixedThreadPool.shutdown();
                fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
                fixedThreadPool.shutdownNow();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("interpolatedraster", interpolatedWR, regionMap,
                inGrid.getCoordinateReferenceSystem());

    }
    private void processing( final int cols, final STRtree tree, final WritableRandomIter interpolatedIter, final double[] eval,
            final int row ) {
        try {
            for( int c = 0; c < cols; c++ ) {
                final DirectPosition gridToWorld = gridGeometry.gridToWorld(new GridCoordinates2D(c, row));
                // System.out.println(row + "/" + c);
                boolean doProcess = true;
                if (inMask != null) {
                    inMask.evaluate(gridToWorld, eval);
                    if (isNovalue(eval[0])) {
                        doProcess = false;
                    }
                }

                if (doProcess) {
                    final Coordinate currentCoord = new Coordinate();
                    final double[] coord = gridToWorld.getCoordinate();
                    currentCoord.x = coord[0];
                    currentCoord.y = coord[1];

                    final Envelope env = new Envelope(currentCoord.x - pBuffer, currentCoord.x + pBuffer,
                            currentCoord.y - pBuffer, currentCoord.y + pBuffer);

                    @SuppressWarnings("unchecked")
                    final List<Coordinate> result = tree.query(env);
                    // System.out.println(row + "/" + c + " = " + result.size());

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
}
