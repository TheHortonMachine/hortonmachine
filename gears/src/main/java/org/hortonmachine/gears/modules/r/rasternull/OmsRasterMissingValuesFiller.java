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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.interpolation2d.core.AveragingInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.ISurfaceInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.TPSInterpolator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
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

@Description(OMSRASTERNULLFILLER_DESCRIPTION)
@Documentation(OMSRASTERNULLFILLER_DOCUMENTATION)
@Author(name = OMSRASTERNULLFILLER_AUTHORNAMES, contact = OMSRASTERNULLFILLER_AUTHORCONTACTS)
@Keywords(OMSRASTERNULLFILLER_KEYWORDS)
@Label(OMSRASTERNULLFILLER_LABEL)
@Name(OMSRASTERNULLFILLER_NAME)
@Status(OMSRASTERNULLFILLER_STATUS)
@License(OMSRASTERNULLFILLER_LICENSE)
public class OmsRasterMissingValuesFiller extends HMModel {

    @Description(OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERNULLFILLER_pValidCellsBuffer_DESCRIPTION)
    @In
    public int pValidCellsBuffer = 10;

    @Description(OMSRASTERNULLFILLER_P_MODE_DESCRIPTION)
    @UI("combo:" + IDW + "," + TPS + "," + AVERAGING + "," + CATEGORIES)
    @In
    public String pMode = IDW;

    @Description(OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERNULLFILLER_DESCRIPTION = "Modules that fills missing values in a raster by interpolation of border values.";
    public static final String OMSRASTERNULLFILLER_DOCUMENTATION = "";
    public static final String OMSRASTERNULLFILLER_KEYWORDS = "Null, Missing values, Raster";
    public static final String OMSRASTERNULLFILLER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERNULLFILLER_NAME = "rmissingfiller";
    public static final int OMSRASTERNULLFILLER_STATUS = 40;
    public static final String OMSRASTERNULLFILLER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERNULLFILLER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERNULLFILLER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION = "The raster to modify.";
    public static final String OMSRASTERNULLFILLER_pValidCellsBuffer_DESCRIPTION = "Number of max cells in distance to consider for the interpolation.";
    public static final String OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION = "The new raster.";
    public static final String OMSRASTERNULLFILLER_P_MODE_DESCRIPTION = "Interpolation mode.";

    @SuppressWarnings("unchecked")
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
                HMRaster outData = HMRaster.writableFromTemplate("nulled", inRaster, true)) {
            List<Coordinate> novaluePoints = new ArrayList<>();
            inData.process(pm, "Identifing holes...", ( col, row, value, tcols, trows ) -> {
                if (inData.isNovalue(value)) {
                    novaluePoints.add(new Coordinate(col, row));
                }
            });

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
                // get points with values in range
                Envelope env = new Envelope(new Coordinate(noValuePoint.x, noValuePoint.y));
                env.expandBy(pValidCellsBuffer);
                List<Coordinate> result = touchingPointsTreetree.query(env);
                result = result.stream().filter(c -> c.distance(noValuePoint) < pValidCellsBuffer).collect(Collectors.toList());
                if (result.size() > 3) {
                    try {
                        double value = interpolator.getValue(result, noValuePoint);

                        outData.setValue((int) noValuePoint.x, (int) noValuePoint.y, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                pm.worked(1);
            }
            pm.done();

            outRaster = outData.buildCoverage();
        }
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

        @Override
        public double getBuffer() {
            return 0;
        }

    }
}
