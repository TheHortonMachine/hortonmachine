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
package org.hortonmachine.gears.modules.v.sourcesdirection;

import static java.lang.Double.NaN;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_IN_COVERAGE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_IN_SOURCES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_OUT_SOURCES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_P_RES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTDIRECTIONCALCULATOR_STATUS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.gridToWorld;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

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

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureExtender;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.sorting.QuickSortAlgorithmObjects;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Description(OMSPOINTDIRECTIONCALCULATOR_DESCRIPTION)
@Documentation(OMSPOINTDIRECTIONCALCULATOR_DOCUMENTATION)
@Author(name = OMSPOINTDIRECTIONCALCULATOR_AUTHORNAMES, contact = OMSPOINTDIRECTIONCALCULATOR_AUTHORCONTACTS)
@Keywords(OMSPOINTDIRECTIONCALCULATOR_KEYWORDS)
@Label(OMSPOINTDIRECTIONCALCULATOR_LABEL)
@Name(OMSPOINTDIRECTIONCALCULATOR_NAME)
@Status(OMSPOINTDIRECTIONCALCULATOR_STATUS)
@License(OMSPOINTDIRECTIONCALCULATOR_LICENSE)
public class OmsPointDirectionCalculator extends HMModel {

    @Description(OMSPOINTDIRECTIONCALCULATOR_IN_SOURCES_DESCRIPTION)
    @In
    public SimpleFeatureCollection inSources;

    @Description(OMSPOINTDIRECTIONCALCULATOR_P_RES_DESCRIPTION)
    @In
    public double pRes = NaN;

    @Description(OMSPOINTDIRECTIONCALCULATOR_IN_COVERAGE_DESCRIPTION)
    @In
    public GridCoverage2D inCoverage = null;

    @Description(OMSPOINTDIRECTIONCALCULATOR_OUT_SOURCES_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outSources;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outSources == null, doReset)) {
            return;
        }

        FeatureIterator<SimpleFeature> inFeatureIterator = inSources.features();

        outSources = new DefaultFeatureCollection();

        FeatureExtender fExt = new FeatureExtender(inSources.getSchema(), new String[]{"azimuth", "availpixels", "c11", "c12",
                "c13", "c21", "c22", "c23", "c31", "c32", "c33"}, new Class< ? >[]{Double.class, Integer.class, Double.class,
                Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class});

        // resample dem to required resolution
        double[] res = resFromCoverage(inCoverage);
        if (res[0] != pRes) {
            double scaleX = res[0] / pRes;
            double scaleY = res[1] / pRes;
            System.out.println(res[0] + "/" + res[1] + "/" + scaleX + "/" + scaleY);
            inCoverage = (GridCoverage2D) Operations.DEFAULT.subsampleAverage(inCoverage, scaleX, scaleY);
        }
        Envelope2D env = inCoverage.getEnvelope2D();
        GridGeometry2D gridGeometry = inCoverage.getGridGeometry();

        int size = inSources.size();
        pm.beginTask("Extracting azimuth...", size);
        while( inFeatureIterator.hasNext() ) {
            pm.worked(1);
            SimpleFeature feature = inFeatureIterator.next();

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();

            if (!env.contains(coordinate.x, coordinate.y)) {
                continue;
            }
            // source is in this dem, process it

            GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
            int cols = gridRange.width;
            int rows = gridRange.height;

            GridCoordinates2D centerGC = gridGeometry.worldToGrid(new DirectPosition2D(coordinate.x, coordinate.y));

            /*
             * c11 | c12 | c13
             * ---------------
             * c21 | cen | c23
             * ---------------
             * c31 | c32 | c33
             * 
             * where c23 = row 2, col 3
             */
            GridCoordinates2D c11 = new GridCoordinates2D(centerGC.x - 1, centerGC.y - 1);
            GridCoordinates2D c12 = new GridCoordinates2D(centerGC.x, centerGC.y - 1);
            GridCoordinates2D c13 = new GridCoordinates2D(centerGC.x + 1, centerGC.y - 1);

            GridCoordinates2D c21 = new GridCoordinates2D(centerGC.x - 1, centerGC.y);
            GridCoordinates2D c23 = new GridCoordinates2D(centerGC.x + 1, centerGC.y);

            GridCoordinates2D c31 = new GridCoordinates2D(centerGC.x - 1, centerGC.y + 1);
            GridCoordinates2D c32 = new GridCoordinates2D(centerGC.x, centerGC.y + 1);
            GridCoordinates2D c33 = new GridCoordinates2D(centerGC.x + 1, centerGC.y + 1);

            double[] center = inCoverage.evaluate((GridCoordinates2D) centerGC, (double[]) null);
            double dz11 = -10000;
            double dz12 = -10000;
            double dz13 = -10000;
            double dz21 = -10000;
            double dz23 = -10000;
            double dz31 = -10000;
            double dz32 = -10000;
            double dz33 = -10000;

            int pixelNum = 0;
            boolean oneIsNull = false;
            double[] v11 = getPixelValue(inCoverage, cols, rows, c11);
            if (v11 != null) {
                pixelNum++;
                dz11 = (center[0] - v11[0]) / sqrt(2);
            } else {
                oneIsNull = true;
            }
            double[] v12 = getPixelValue(inCoverage, cols, rows, c12);
            if (v12 != null) {
                pixelNum++;
                dz12 = (center[0] - v12[0]);
            } else {
                oneIsNull = true;
            }
            double[] v13 = getPixelValue(inCoverage, cols, rows, c13);
            if (v13 != null) {
                pixelNum++;
                dz13 = (center[0] - v13[0]) / sqrt(2);
            } else {
                oneIsNull = true;
            }
            double[] v21 = getPixelValue(inCoverage, cols, rows, c21);
            if (v21 != null) {
                pixelNum++;
                dz21 = (center[0] - v21[0]);
            } else {
                oneIsNull = true;
            }
            double[] v23 = getPixelValue(inCoverage, cols, rows, c23);
            if (v23 != null) {
                pixelNum++;
                dz23 = (center[0] - v23[0]);
            } else {
                oneIsNull = true;
            }
            double[] v31 = getPixelValue(inCoverage, cols, rows, c31);
            if (v31 != null) {
                pixelNum++;
                dz31 = (center[0] - v31[0]) / sqrt(2);
            } else {
                oneIsNull = true;
            }
            double[] v32 = getPixelValue(inCoverage, cols, rows, c32);
            if (v32 != null) {
                pixelNum++;
                dz32 = (center[0] - v32[0]);
            } else {
                oneIsNull = true;
            }
            double[] v33 = getPixelValue(inCoverage, cols, rows, c33);
            if (v33 != null) {
                pixelNum++;
                dz33 = (center[0] - v33[0]) / sqrt(2);
            } else {
                oneIsNull = true;
            }

            GridCoordinates2D[] cArray = new GridCoordinates2D[]{c31, c32, c33, c21, c23, c11, c12, c13};
            double[] tArray = new double[]{dz31, dz32, dz33, dz21, dz23, dz11, dz12, dz13};

            QuickSortAlgorithmObjects qSobj = new QuickSortAlgorithmObjects(null);
            qSobj.sort(tArray, cArray);

            GridCoordinates2D steepestCoord = cArray[cArray.length - 1];

            Point2D steepestWorldCoord = gridToWorld(gridGeometry, steepestCoord.x, steepestCoord.y);
            double[] c = new double[]{steepestWorldCoord.getX(), steepestWorldCoord.getY()};
            Point2D centerCoordOnGrid = gridToWorld(gridGeometry, centerGC.x, centerGC.y);
            double[] cent = new double[]{centerCoordOnGrid.getX(), centerCoordOnGrid.getY()};

            double azimuth = -9999.0;
            if (!oneIsNull) {
                azimuth = GeometryUtilities.azimuth(new Coordinate(cent[0], cent[1]), new Coordinate(c[0], c[1]));
            }

            SimpleFeature azimuthFeature = fExt.extendFeature(feature, new Object[]{azimuth, pixelNum, getValue(v11),
                    getValue(v12), getValue(v13), getValue(v21), getValue(center), getValue(v23), getValue(v31), getValue(v32),
                    getValue(v33)});
            ((DefaultFeatureCollection) outSources).add(azimuthFeature);

        }
        pm.done();
    }

    private double getValue( double[] array ) {
        return array != null ? array[0] : -9999.0;
    }

    private double[] getPixelValue( GridCoverage2D dem, int cols, int rows, GridCoordinates2D gridCoordinate ) {
        if (gridCoordinate.x >= 0 && gridCoordinate.x < cols && gridCoordinate.y >= 0 && gridCoordinate.y < rows) {
            double[] value = dem.evaluate((GridCoordinates2D) gridCoordinate, (double[]) null);
            return value;
        }
        return null;
    }

    private double[] resFromCoverage( GridCoverage2D dem ) {
        GridGeometry2D gridGeometry = dem.getGridGeometry();
        AffineTransform gridToCRS = (AffineTransform) gridGeometry.getGridToCRS();
        double[] res = new double[]{XAffineTransform.getScaleX0(gridToCRS), XAffineTransform.getScaleY0(gridToCRS)};
        return res;
    }
}
