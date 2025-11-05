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
package org.hortonmachine.hmachine.modules.geomorphology.viewshed;

import static org.hortonmachine.gears.libs.modules.HMConstants.GEOMORPHOLOGY;

import java.awt.image.WritableRaster;
import java.util.List;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.feature.simple.SimpleFeature;

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
import oms3.annotations.Unit;

@Description(OmsViewshed.DESCRIPTION)
@Documentation(OmsViewshed.DOC)
@Author(name = OmsViewshed.AUTHOR, contact = OmsViewshed.CONTACT)
@Keywords(OmsViewshed.KEYWORDS)
@Label(OmsViewshed.LABEL)
@Name(OmsViewshed.KEYWORDS)
@Status(OmsViewshed.STATUS)
@License(OmsViewshed.LICENSE)
public class OmsViewshed extends HMModel {

    private static final String DEFAULT_HEIGHT_FIELD = "elev";

    @Description(DESCR_inRaster)
    @In
    public GridCoverage2D inRaster = null;

    @Description(DESCR_inViewPoints)
    @In
    public SimpleFeatureCollection inViewPoints = null;

    @Description(DESCR_pField)
    @In
    public String pField = DEFAULT_HEIGHT_FIELD;

    @Description(DESCR_pHeight)
    @Unit("m")
    @In
    public double pHeight = 2.0;

    @Description(DESCR_outViewshed)
    @Out
    public GridCoverage2D outViewshed = null;

    /**
     * If a listener is added here, every viewpoint will also be calculated separately, using the same raster. 
     */
    public ViewpointProcessingListener singleViewpointProcessListener = null;

    public static final String DOC = "Calculate a viewshed raster, with values based on the visibility by the supplied view points.";
    public static final String DESCR_outViewshed = "Output viewshed raster.";
    public static final String DESCR_pHeight = "Default height above the elevation model to use if no station's height field is available.";
    public static final String DESCR_pField = "Name of the field containing the station's height above the elevation model";
    public static final String DESCR_inViewPoints = "Input viewpoints collection.";
    public static final String DESCR_inRaster = "Input elevation raster.";
    public static final String LICENSE = HMConstants.GPL3_LICENSE;
    public static final String LABEL = GEOMORPHOLOGY;
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String KEYWORDS = "viewshed";
    public static final String CONTACT = "jlindsay@uoguelph.ca";
    public static final String AUTHOR = "Dr. John Lindsay";
    public static final String DESCRIPTION = "Viewshed module";

    public static interface ViewpointProcessingListener {

        void processViewPoint( Coordinate viewpoint3D, WritableRaster reusableViewshed );
    }

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        double novalue = HMConstants.getNovalue(inRaster);

        RandomIter inIter = CoverageUtilities.getRandomIterator(inRaster);

        WritableRaster outViewshedWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, novalue);
        WritableRandomIter outViewshedIter = CoverageUtilities.getWritableRandomIterator(outViewshedWR);
        WritableRaster tmpViewshedWR = null;
        WritableRandomIter tmpViewshedIter = null;
        if (singleViewpointProcessListener != null) {
            tmpViewshedWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, novalue);
            tmpViewshedIter = CoverageUtilities.getWritableRandomIterator(tmpViewshedWR);
        }

        WritableRaster viewAngleWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, novalue);
        WritableRandomIter viewAngleIter = CoverageUtilities.getWritableRandomIterator(viewAngleWR);
        WritableRaster maxViewAngleWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, novalue);
        WritableRandomIter maxViewAngleIter = CoverageUtilities.getWritableRandomIterator(maxViewAngleWR);

        List<SimpleFeature> viewPoints = FeatureUtilities.featureCollectionToList(inViewPoints);

        GridGeometry2D gg = inRaster.getGridGeometry();
        try {
            boolean isFirst = true;
            pm.beginTask("Processing viewpoints...", viewPoints.size());
            for( SimpleFeature feature : viewPoints ) {
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Coordinate viewPoint3D = geom.getCoordinate();
                int[] stationColRow = CoverageUtilities.colRowFromCoordinate(viewPoint3D, gg, null);
                int stationCol = stationColRow[0];
                int stationRow = stationColRow[1];
                if (stationCol - 1 < 0 || stationCol + 1 >= cols || stationRow - 1 < 0 || stationRow + 1 >= rows) {
                    pm.errorMessage("Ignoring viewpoint on border.");
                    continue;
                }

                double tmpZ = pHeight;
                if (pField != null) {
                    Object fieldObj = feature.getAttribute(pField);
                    if (fieldObj instanceof Number) {
                        Number elevNum = (Number) fieldObj;
                        tmpZ = elevNum.doubleValue();
                    } else if (isFirst) {
                        pm.errorMessage(
                                "Using default height value " + pHeight + ", since field: " + pField + " does not exist.");
                    }
                }
                viewPoint3D.z = tmpZ;

                if (singleViewpointProcessListener != null) {
                    if (!isFirst) {
                        // in this case clear the raster to hold one viewpoint result at a time
                        for( int row = 0; row < rows; row++ ) {
                            for( int col = 0; col < cols; col++ ) {
                                tmpViewshedIter.setSample(col, row, 0, novalue);
                            }
                        }
                    }
                    calculateViewshed(viewPoint3D, stationColRow, cols, rows, novalue, inIter, tmpViewshedIter, viewAngleIter,
                            maxViewAngleIter, gg);
                    singleViewpointProcessListener.processViewPoint(viewPoint3D, tmpViewshedWR);
                    for( int r = 0; r < rows; r++ ) {
                        for( int c = 0; c < cols; c++ ) {
                            double tmpValue = tmpViewshedIter.getSampleDouble(c, r, 0);
                            double finalValue = outViewshedIter.getSampleDouble(c, r, 0);
                            if (HMConstants.isNovalue(tmpValue, novalue)) {
                                tmpValue = 0;
                            }
                            if (HMConstants.isNovalue(finalValue, novalue)) {
                                finalValue = 0;
                            }
                            outViewshedIter.setSample(c, r, 0, tmpValue + finalValue);
                        }
                    }
                } else {
                    calculateViewshed(viewPoint3D, stationColRow, cols, rows, novalue, inIter, outViewshedIter, viewAngleIter,
                            maxViewAngleIter, gg);
                }

                isFirst = false;
            }
            pm.done();

        } finally {
            inIter.done();
            viewAngleIter.done();
            maxViewAngleIter.done();
            if (tmpViewshedIter != null)
                tmpViewshedIter.done();
            outViewshedIter.done();
        }

        outViewshed = CoverageUtilities.buildCoverageWithNovalue(KEYWORDS, outViewshedWR, regionMap,
                inRaster.getCoordinateReferenceSystem(), novalue);
//        dumpRaster(CoverageUtilities.buildCoverageWithNovalue("viewangle", viewAngleWR, regionMap,
//                inRaster.getCoordinateReferenceSystem(), novalue), "/home/hydrologis/data/DTM_calvello/viewangle.asc");
//        dumpRaster(
//                CoverageUtilities.buildCoverageWithNovalue("maxviewangle", maxViewAngleWR, regionMap,
//                        inRaster.getCoordinateReferenceSystem(), novalue),
//                "/home/hydrologis/data/DTM_calvello/viewangle_max.asc");
    }

    private void calculateViewshed( Coordinate viewPoint3D, int[] stationColRow, int cols, int rows, double novalue,
            RandomIter inIter, WritableRandomIter outViewshedIter, WritableRandomIter viewAngleIter,
            WritableRandomIter maxViewAngleIter, GridGeometry2D gg ) {
        double value = CoverageUtilities.getValue(inRaster, viewPoint3D.x, viewPoint3D.y);
        if (!HMConstants.isNovalue(value, novalue)) {
            pm.message("Working on viewpoint: " + viewPoint3D);
            double stationX = viewPoint3D.x;
            double stationY = viewPoint3D.y;
            double stationZ = value + viewPoint3D.z;

            int stationCol = stationColRow[0];
            int stationRow = stationColRow[1];

            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    double z = inIter.getSampleDouble(col, row, 0);
                    if (!HMConstants.isNovalue(z, novalue)) {
                        Coordinate worldC = CoverageUtilities.coordinateFromColRow(col, row, gg);
                        double x = worldC.x;
                        double y = worldC.y;
                        double dZ = z - stationZ;
                        double dist = Math.sqrt((x - stationX) * (x - stationX) + (y - stationY) * (y - stationY));
                        if (dist != 0.0) {
                            double viewAngleValue = dZ / dist * 1000;
                            viewAngleIter.setSample(col, row, 0, viewAngleValue);
                        }
                    } else {
                        viewAngleIter.setSample(col, row, 0, novalue);
                    }
                }
            }

            // perform the simple scan lines.
            for( int row = stationRow - 1; row <= stationRow + 1; row++ ) {
                for( int col = stationCol - 1; col <= stationCol + 1; col++ ) {
                    maxViewAngleIter.setSample(col, row, 0, viewAngleIter.getSampleDouble(col, row, 0));
                }
            }

            double maxVA = viewAngleIter.getSampleDouble(stationCol, stationRow - 1, 0);
            for( int row = stationRow - 2; row >= 0; row-- ) {
                double z = viewAngleIter.getSampleDouble(stationCol, row, 0);
                if (!HMConstants.isNovalue(z, novalue)) {
                    if (z > maxVA) {
                        maxVA = z;
                    }
                    maxViewAngleIter.setSample(stationCol, row, 0, maxVA);
                }
            }

            maxVA = viewAngleIter.getSampleDouble(stationCol, stationRow + 1, 0);
            for( int row = stationRow + 2; row < rows; row++ ) {
                double z = viewAngleIter.getSampleDouble(stationCol, row, 0);
                if (!HMConstants.isNovalue(z, novalue)) {
                    if (z > maxVA) {
                        maxVA = z;
                    }
                    maxViewAngleIter.setSample(stationCol, row, 0, maxVA);
                }
            }

            maxVA = viewAngleIter.getSampleDouble(stationCol + 1, stationRow, 0);
            for( int col = stationCol + 2; col < cols - 1; col++ ) {
                double z = viewAngleIter.getSampleDouble(col, stationRow, 0);
                if (!HMConstants.isNovalue(z, novalue)) {
                    if (z > maxVA) {
                        maxVA = z;
                    }
                    maxViewAngleIter.setSample(col, stationRow, 0, maxVA);
                }
            }

            maxVA = viewAngleIter.getSampleDouble(stationCol - 1, stationRow, 0);
            for( int col = stationCol - 2; col >= 0; col-- ) {
                double z = viewAngleIter.getSampleDouble(col, stationRow, 0);
                if (!HMConstants.isNovalue(z, novalue)) {
                    if (z > maxVA) {
                        maxVA = z;
                    }
                    maxViewAngleIter.setSample(col, stationRow, 0, maxVA);
                }
            }

            // solve the first triangular facet
            int vertCount = 0;
            for( int row = stationRow - 2; row >= 0; row-- ) {
                vertCount++;
                int horizCount = 0;
                for( int col = stationCol + 1; col <= stationCol + vertCount; col++ ) {
                    if (col >= 0 && col < cols) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col - 1, row + 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col, row + 1, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col - 1, row + 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the second triangular facet
            vertCount = 1;
            for( int row = stationRow - 2; row >= 0; row-- ) {
                vertCount++;
                int horizCount = 0;
                for( int col = stationCol - 1; col >= stationCol - vertCount; col-- ) {
                    if (col >= 0 && col < cols) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col + 1, row + 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col, row + 1, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col + 1, row + 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the third triangular facet
            vertCount = 1;
            for( int row = stationRow + 2; row < rows; row++ ) {
                vertCount++;
                int horizCount = 0;
                for( int col = stationCol - 1; col >= stationCol - vertCount; col-- ) {
                    if (col >= 0 && col < cols) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col + 1, row - 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col, row - 1, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col + 1, row - 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the fourth triangular facet
            vertCount = 1;
            for( int row = stationRow + 2; row < rows; row++ ) {
                vertCount++;
                int horizCount = 0;
                for( int col = stationCol + 1; col <= stationCol + vertCount; col++ ) {
                    if (col >= 0 && col < cols) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col - 1, row - 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col, row - 1, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col - 1, row - 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the fifth triangular facet
            vertCount = 1;
            for( int col = stationCol + 2; col < cols; col++ ) {
                vertCount++;
                int horizCount = 0;
                for( int row = stationRow - 1; row >= stationRow - vertCount; row-- ) {
                    if (row >= 0 && row < rows) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col - 1, row + 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col - 1, row, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col - 1, row + 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the sixth triangular facet
            vertCount = 1;
            for( int col = stationCol + 2; col < cols; col++ ) {
                vertCount++;
                int horizCount = 0;
                for( int row = stationRow + 1; row <= stationRow + vertCount; row++ ) {
                    if (row >= 0 && row < rows) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col - 1, row - 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col - 1, row, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col - 1, row - 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the seventh triangular facet
            vertCount = 1;
            for( int col = stationCol - 2; col >= 0; col-- ) {
                vertCount++;
                int horizCount = 0;
                for( int row = stationRow + 1; row <= stationRow + vertCount; row++ ) {
                    if (row >= 0 && row < rows) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col + 1, row - 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col + 1, row, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col + 1, row - 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the eight triangular facet
            vertCount = 1;
            for( int col = stationCol - 2; col >= 0; col-- ) {
                vertCount++;
                int horizCount = 0;
                for( int row = stationRow - 1; row >= stationRow - vertCount; row-- ) {
                    if (row >= 0 && row < rows) {
                        double va = viewAngleIter.getSampleDouble(col, row, 0);
                        if (!HMConstants.isNovalue(va, novalue)) {
                            horizCount++;
                            double tva;
                            if (horizCount != vertCount) {
                                double t1 = maxViewAngleIter.getSampleDouble(col + 1, row + 1, 0);
                                double t2 = maxViewAngleIter.getSampleDouble(col + 1, row, 0);
                                tva = t2 + horizCount / vertCount * (t1 - t2);
                            } else {
                                tva = maxViewAngleIter.getSampleDouble(col + 1, row + 1, 0);
                            }
                            if (tva > va) {
                                maxViewAngleIter.setSample(col, row, 0, tva);
                            } else {
                                maxViewAngleIter.setSample(col, row, 0, va);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    double viewAngle = viewAngleIter.getSampleDouble(col, row, 0);
                    double maxViewAngle = maxViewAngleIter.getSampleDouble(col, row, 0);

                    if (maxViewAngle <= viewAngle && !HMConstants.isNovalue(viewAngle, novalue)) {
                        double viewshed = outViewshedIter.getSampleDouble(col, row, 0);
                        if (HMConstants.isNovalue(viewshed, novalue)) {
                            viewshed = 0;
                        }
                        outViewshedIter.setSample(col, row, 0, viewshed + 1);
//                            } else if (HMConstants.isNovalue(viewAngle, novalue)) {
//                                outViewshedIter.setSample(col, row, 0, novalue);
                    }
                }
            }

            pm.worked(1);
        } else {
            pm.errorMessage("Ignoring viewpoint " + viewPoint3D + " since no elevation value available.");
            pm.worked(1);
        }
    }

}
