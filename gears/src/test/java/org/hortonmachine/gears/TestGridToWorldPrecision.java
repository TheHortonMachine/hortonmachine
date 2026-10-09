/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears;

import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller;
import org.hortonmachine.gears.modules.v.vectorize.OmsPointsVectorizer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * The center of the cells of a fine grid at the coordinates of a projected crs, as northings of
 * millions of meters, has to be computed in double precision.
 */
public class TestGridToWorldPrecision extends HMTestCase {

    private static final double WEST = 725717.98;
    private static final double NORTH = 5205795.28;
    private static final double RES = 0.1;

    public void testCellCenterInDoublePrecision() throws Exception {
        GridGeometry2D gridGeometry = fineGrid().getGridGeometry();
        for( int row = 0; row < 100; row += 7 ) {
            for( int col = 0; col < 100; col += 7 ) {
                Coordinate center = CoverageUtilities.coordinateFromColRow(col, row, gridGeometry);
                assertEquals(WEST + (col + 0.5) * RES, center.x, 1E-6);
                assertEquals(NORTH - (row + 0.5) * RES, center.y, 1E-6);
            }
        }
    }

    /**
     * Documents the reason of the double precision helpers: GeoTools transforms the integer grid
     * point into a float point.
     */
    public void testGeotoolsGridToWorldIsFloat() throws Exception {
        GridGeometry2D gridGeometry = fineGrid().getGridGeometry();
        double maxError = 0;
        for( int row = 0; row < 100; row++ ) {
            double[] world = gridGeometry.gridToWorld(new GridCoordinates2D(0, row)).getCoordinate();
            maxError = Math.max(maxError, Math.abs(world[1] - (NORTH - (row + 0.5) * RES)));
        }
        assertTrue("float precision at a northing of 5 million: " + maxError, maxError > 0.01);
    }

    public void testPointsVectorizerOnCellCenters() throws Exception {
        OmsPointsVectorizer vectorizer = new OmsPointsVectorizer();
        vectorizer.pm = pm;
        vectorizer.inRaster = fineGrid();
        vectorizer.process();

        assertEquals(100 * 100, vectorizer.outVector.size());
        try (SimpleFeatureIterator iterator = vectorizer.outVector.features()) {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                Coordinate point = ((Geometry) feature.getDefaultGeometry()).getCoordinate();
                // the value is the row*1000+col of the cell of the point
                int value = ((Number) feature.getAttribute("value")).intValue();
                int row = value / 1000;
                int col = value % 1000;
                assertEquals(WEST + (col + 0.5) * RES, point.x, 1E-6);
                assertEquals(NORTH - (row + 0.5) * RES, point.y, 1E-6);
            }
        }
    }

    /**
     * A hole in a plane rising to the north is filled with the plane: its neighbours must be at
     * their exact positions.
     */
    public void testHoleFillerOnFineGrid() throws Exception {
        double[][] data = new double[100][100];
        for( int row = 0; row < 100; row++ ) {
            for( int col = 0; col < 100; col++ ) {
                data[row][col] = 1000.0 + (100 - row) * 10.0;
            }
        }
        data[50][50] = HMConstants.doubleNovalue;
        OmsHoleFiller filler = new OmsHoleFiller();
        filler.pm = pm;
        filler.inRaster = fineGrid(data);
        filler.pMode = Variables.IDW;
        filler.pBuffer = 1.0;
        filler.process();

        RandomIter iter = CoverageUtilities.getRandomIterator(filler.outRaster);
        assertEquals(1500.0, iter.getSampleDouble(50, 50, 0), 1E-6);
        iter.done();
    }

    private static GridCoverage2D fineGrid() {
        double[][] data = new double[100][100];
        for( int row = 0; row < 100; row++ ) {
            for( int col = 0; col < 100; col++ ) {
                data[row][col] = row * 1000 + col;
            }
        }
        return fineGrid(data);
    }

    private static GridCoverage2D fineGrid( double[][] data ) {
        return CoverageUtilities.buildCoverage("fine", data, CoverageUtilities.makeRegionParamsMap(NORTH, NORTH - 100 * RES, WEST,
                WEST + 100 * RES, RES, RES, 100, 100), HMTestMaps.getCrs(), true);
    }
}
