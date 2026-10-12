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
package org.hortonmachine.gears.modules.r.rasternull;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.AVERAGING;
import static org.hortonmachine.gears.libs.modules.Variables.CATEGORIES;
import static org.hortonmachine.gears.libs.modules.Variables.IDW;
import static org.hortonmachine.gears.libs.modules.Variables.LDW;
import static org.hortonmachine.gears.libs.modules.Variables.TPS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_LABEL;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_LICENSE;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_NAME;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_STATUS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.modules.r.interpolation2d.core.AveragingInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.ISurfaceInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.LinearDWInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.TPSInterpolator;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

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

@Description(OMSRASTERNULLFILLER_DESCRIPTION)
@Documentation(OMSRASTERNULLFILLER_DOCUMENTATION)
@Author(name = OMSRASTERNULLFILLER_AUTHORNAMES, contact = OMSRASTERNULLFILLER_AUTHORCONTACTS)
@Keywords(OMSRASTERNULLFILLER_KEYWORDS)
@Label(OMSRASTERNULLFILLER_LABEL)
@Name(OMSRASTERNULLFILLER_NAME)
@Status(OMSRASTERNULLFILLER_STATUS)
@License(OMSRASTERNULLFILLER_LICENSE)
@SuppressWarnings("unchecked")
public class OmsRasterMissingValuesFiller extends HMModel {

    @Description(OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERNULLFILLER_IN_RASTERMASK_DESCRIPTION)
    @In
    public GridCoverage2D inRasterMask;

    @Description(OMSRASTERNULLFILLER_IN_VECTORMASK_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVectorMask;

    @Description(OMSRASTERNULLFILLER_pMinDistance_DESCRIPTION)
    @In
    public int pMinDistance = 0;

    @Description(OMSRASTERNULLFILLER_pMaxDistance_DESCRIPTION)
    @In
    public int pMaxDistance = 10;

    @Description(OMSRASTERNULLFILLER_doUseOnlyBorderValues_DESCRIPTION)
    @In
    public boolean doUseOnlyBorderValues = false;

    @Description(OMSRASTERNULLFILLER_P_MODE_DESCRIPTION)
    @UI("combo:" + IDW + "," + LDW + "," + TPS + "," + AVERAGING + "," + CATEGORIES)
    @In
    public String pMode = IDW;

    @Description(OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERNULLFILLER_DESCRIPTION = "Fills the missing values (holes) of a raster by interpolation of the valid cells around them.";
    public static final String OMSRASTERNULLFILLER_DOCUMENTATION = """
            Every novalue cell is filled from the valid cells within the max distance, measured in \
            cells from center to center, so a circle of radius pMaxDistance. The cells at the min \
            distance or nearer are not used: a min distance of 1 or 2 skips the edge cells of the holes, which \
            in lidar or photogrammetric models are often wrong. With the only border values switch, just \
            the valid cells touching a hole (in the 8 directions) are used, the ones on the rim of the \
            holes: the interior of the valid area doesn't weigh on the fill, and on large rasters it is \
            faster.

            A cell is filled only if at least 4 valid cells are found, otherwise it stays novalue: the \
            holes wider than about twice the max distance keep a novalue core. The filled cells are not \
            used to fill the others, so the result doesn't depend on the order of the cells.

            The modes are inverse distance weighting (IDW, weights 1/d²), linear distance weighting \
            (LDW), thin plate spline (TPS), the plain average (AVERAGING) and, for rasters of classes, \
            the most frequent value (CATEGORIES), that never creates values that are not in the input.

            Every novalue cell is a hole, also the ones outside the area of the data, as the border of a \
            basin or the frame of an orthophoto: without a mask the valid area grows outwards by the max \
            distance. A raster mask (on the grid of the input) or a vector mask of polygons limits the \
            fill to its valid cells or to its area.
            """;
    public static final String OMSRASTERNULLFILLER_KEYWORDS = "Null, Missing values, Holes, Interpolation, Raster";
    public static final String OMSRASTERNULLFILLER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERNULLFILLER_NAME = "rmissingfiller";
    public static final int OMSRASTERNULLFILLER_STATUS = 40;
    public static final String OMSRASTERNULLFILLER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERNULLFILLER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERNULLFILLER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION = "The raster in which to fill missing values.";
    public static final String OMSRASTERNULLFILLER_IN_RASTERMASK_DESCRIPTION = "An optional raster mask, on the grid of the input: only the holes on its valid cells are filled.";
    public static final String OMSRASTERNULLFILLER_IN_VECTORMASK_DESCRIPTION = "An optional vector mask of polygons: only the holes inside them are filled.";
    public static final String OMSRASTERNULLFILLER_pMinDistance_DESCRIPTION = "The cells at this distance or nearer are not used for the interpolation (in cells).";
    public static final String OMSRASTERNULLFILLER_pMaxDistance_DESCRIPTION = "The max distance of the cells used for the interpolation (in cells).";
    public static final String OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION = "The filled raster.";
    public static final String OMSRASTERNULLFILLER_P_MODE_DESCRIPTION = "The interpolation mode.";
    public static final String OMSRASTERNULLFILLER_doUseOnlyBorderValues_DESCRIPTION = "Use only the valid cells touching the holes, instead of all the valid cells within the max distance.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);
        ISurfaceInterpolator interpolator = null;
        switch( pMode ) {
        case TPS:
            interpolator = new TPSInterpolator();
            pm.message("Interpolating with Thin Plate Spline.");
            break;
        case AVERAGING:
            interpolator = new AveragingInterpolator();
            pm.message("Interpolating by simple averaging.");
            break;
        case CATEGORIES:
            interpolator = new CategoriesInterpolator();
            pm.message("Interpolating of categories done by most frequent value.");
            break;
        case LDW:
            interpolator = new LinearDWInterpolator();
            pm.message("Interpolating with Linear Distance Weight function.");
            break;
//        case BIVARIATE:
//            interpolator = new BivariateInterpolator(pValidCellsBuffer);
//            pm.message("Interpolating with Bivariate function.");
//            break;
        case IDW:
        default:
            interpolator = new IDWInterpolator();
            pm.message("Interpolating with Inverse Distance Weight function.");
            break;
        }

        try (HMRaster inData = HMRaster.fromGridCoverage(inRaster);
                HMRaster outData = new HMRasterWritableBuilder().setName("nulled").setTemplate(inRaster).setCopyValues(true)
                        .build();) {

            HMRaster mask = null;
            if (inRasterMask != null) {
                mask = HMRaster.fromGridCoverage(inRasterMask);
                if (mask.getCols() != inData.getCols() || mask.getRows() != inData.getRows()) {
                    throw new ModelsIllegalargumentException("The raster mask needs to have the grid of the input raster.",
                            this, pm);
                }
            }

            PreparedGeometry vectorMask = null;
            if (inVectorMask != null) {
                List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(inVectorMask, false, null);
                Geometry union = CascadedPolygonUnion.union(geoms);
                vectorMask = PreparedGeometryFactory.prepare(union);
            }

            List<Coordinate> novaluePoints = new ArrayList<>();
            inData.process(pm, "Identifying holes...", ( col, row, value, tcols, trows ) -> {
                if (inData.isNovalue(value)) {
                    novaluePoints.add(new Coordinate(col, row));
                }
            });
            pm.message("Found holes:" + novaluePoints.size());

            if (doUseOnlyBorderValues) {
                processUsingOnlyBorderValues(interpolator, inData, mask, vectorMask, outData, novaluePoints);
            } else {
                processUsingAllValues(interpolator, inData, mask, vectorMask, outData, novaluePoints);
            }
        }
    }

    private void processUsingAllValues( ISurfaceInterpolator interpolator, HMRaster inData, HMRaster mask,
            PreparedGeometry vectorMask, HMRaster outData, List<Coordinate> novaluePoints ) throws IOException {
        // now find touching points with values
        pm.beginTask("Filling holes...", novaluePoints.size());
        for( Coordinate noValuePoint : novaluePoints ) {
            // the holes are kept in grid space, as col/row coordinates, to interpolate on distances in cells
            int col = (int) noValuePoint.x;
            int row = (int) noValuePoint.y;
            if (!isInMasks(inData, mask, vectorMask, col, row)) {
                pm.worked(1);
                continue;
            }
            List<Coordinate> result = inData.getSurroundingCells(col, row, pMaxDistance, true);
            result = result.stream().filter(c -> !inData.isNovalue(c.z) && c.distance(noValuePoint) > pMinDistance)
                    .collect(Collectors.toList());
            if (result.size() > 3) {
                try {
                    double value = interpolator.getValue(result, noValuePoint);
                    outData.setValue(col, row, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            pm.worked(1);
        }
        pm.done();

        outRaster = outData.buildCoverage();
    }

    private void processUsingOnlyBorderValues( ISurfaceInterpolator interpolator, HMRaster inData, HMRaster mask,
            PreparedGeometry vectorMask, HMRaster outData, List<Coordinate> novaluePoints ) throws IOException {
        // now find touching points with values
        STRtree touchingPointsTreetree = new STRtree();
        TreeSet<String> checkSet = new TreeSet<>();
        for( Coordinate noValuePoint : novaluePoints ) {
            Direction[] orderedDirs = Direction.getOrderedDirs();
            for( int i = 0; i < orderedDirs.length; i++ ) {
                Direction direction = orderedDirs[i];
                int newCol = (int) (noValuePoint.x + direction.col);
                int newRow = (int) (noValuePoint.y + direction.row);

                boolean added = checkSet.add(newCol + "_" + newRow);
                if (added && inData.isContained(newCol, newRow)) {
                    double value = inData.getValue(newCol, newRow);
                    if (!inData.isNovalue(value)) {
                        Coordinate coordinate = new Coordinate(newCol, newRow, value);
                        Point p = gf.createPoint(coordinate);
                        touchingPointsTreetree.insert(p.getEnvelopeInternal(), coordinate);
                    }
                }
            }
        }
        touchingPointsTreetree.build();

        pm.beginTask("Filling holes...", novaluePoints.size());
        for( Coordinate noValuePoint : novaluePoints ) {
            int col = (int) noValuePoint.x;
            int row = (int) noValuePoint.y;
            if (!isInMasks(inData, mask, vectorMask, col, row)) {
                pm.worked(1);
                continue;
            }
            // get points with values in range
            Envelope env = new Envelope(new Coordinate(noValuePoint.x, noValuePoint.y));
            env.expandBy(pMaxDistance);
            List<Coordinate> result = touchingPointsTreetree.query(env);
            result = result.stream().filter(c -> isValid(c.distance(noValuePoint))).collect(Collectors.toList());

            if (result.size() > 3) {
                try {
                    double value = interpolator.getValue(result, noValuePoint);
                    outData.setValue(col, row, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            pm.worked(1);
        }
        pm.done();

        outRaster = outData.buildCoverage();
    }

    /**
     * @return <code>true</code> if the cell is on a valid cell of the raster mask and inside the
     *         vector mask, the latter tested on the world position of the cell center.
     */
    private boolean isInMasks( HMRaster inData, HMRaster mask, PreparedGeometry vectorMask, int col, int row ) {
        if (mask != null && mask.isNovalue(mask.getValue(col, row))) {
            return false;
        }
        if (vectorMask != null) {
            Point cellCenter = gf.createPoint(inData.getWorld(col, row));
            return vectorMask.intersects(cellCenter);
        }
        return true;
    }

    private boolean isValid( double distance ) {
        return distance <= pMaxDistance && distance > pMinDistance;
    }

    static public class CategoriesInterpolator implements ISurfaceInterpolator {

        public double getValue( Coordinate[] controlPoints, Coordinate interpolated ) {
            if (controlPoints.length == 0) {
                return HMConstants.doubleNovalue;
            }

            Stream<Coordinate> stream = Stream.of(controlPoints);
            Entry<Double, Long> entry = stream.map(c -> c.z)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get();

            return entry.getKey();
        }

        public double getValue( List<Coordinate> controlPoints, Coordinate interpolated ) {
            if (controlPoints.isEmpty()) {
                return HMConstants.doubleNovalue;
            }

            Entry<Double, Long> entry = controlPoints.stream().map(c -> c.z)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get();

            return entry.getKey();
        }

    }

}
