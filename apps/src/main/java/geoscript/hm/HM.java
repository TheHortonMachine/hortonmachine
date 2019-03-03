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
package geoscript.hm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.interpolation.LeastSquaresInterpolator;
import org.hortonmachine.gears.utils.math.interpolation.PolynomialInterpolator;
import org.hortonmachine.gears.utils.sorting.OddEvenSortAlgorithm;
import org.hortonmachine.gui.utils.HMMapframe;
import org.hortonmachine.gui.utils.OmsMatrixCharter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Scripting support class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("rawtypes")
public class HM {
    public static void showFolder( String folderPath ) {
        HMMapframe.openFolder(new File(folderPath));
    }

    public static void chartMatrix( String title, String xLabel, String yLabel, double[][] data, List<String> series,
            List<String> colors, boolean doLegend ) {
        OmsMatrixCharter charter = new OmsMatrixCharter();
        charter.doChart = true;
        charter.doDump = false;
        charter.doLegend = doLegend;
        charter.doHorizontal = false;
        charter.pHeight = 900;
        charter.pWidth = 1200;
        charter.pType = 0;
        charter.inData = data;
        charter.inTitle = title;
        charter.inSubTitle = "";
        String[] labels = {xLabel, yLabel};
        charter.inLabels = labels;
        charter.inSeries = series.toArray(new String[0]);
        if (colors != null)
            charter.inColors = colors.stream().collect(Collectors.joining(";"));
        try {
            charter.chart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LineString regressionLS( double[][] dataset ) {
        return regressionLS(toCoordList(dataset));
    }

    private static List<Coordinate> toCoordList( double[][] dataset ) {
        List<Coordinate> cDataset = new ArrayList<>();
        for( double[] ds : dataset ) {
            cDataset.add(new Coordinate(ds[0], ds[1]));
        }
        return cDataset;
    }

    public static LineString regressionLS( List< ? > dataset ) {
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for( Object cObj : dataset ) {
            if (cObj instanceof Coordinate) {
                Coordinate c = (Coordinate) cObj;
                x.add(c.x);
                y.add(c.y);
                maxX = Math.max(maxX, c.x);
                minX = Math.min(minX, c.x);
            } else if (cObj instanceof double[]) {
                double[] c = (double[]) cObj;
                x.add(c[0]);
                y.add(c[1]);
                maxX = Math.max(maxX, c[0]);
                minX = Math.min(minX, c[0]);
            } else if (cObj instanceof List) {
                List c = (List) cObj;
                double xv = ((Number) c.get(0)).doubleValue();
                double yv = ((Number) c.get(1)).doubleValue();
                x.add(xv);
                y.add(yv);
                maxX = Math.max(maxX, xv);
                minX = Math.min(minX, xv);
            }
        }
        LeastSquaresInterpolator lsInt = new LeastSquaresInterpolator(x, y);
        double y1 = lsInt.getInterpolated(minX);
        double y2 = lsInt.getInterpolated(maxX);
        return GeometryUtilities.gf().createLineString(new Coordinate[]{new Coordinate(minX, y1), new Coordinate(maxX, y2)});
    }

    public static LineString interpolationPoly( double[][] dataset, double interval ) {
        return interpolationPoly(toCoordList(dataset), interval);
    }

    public static LineString interpolationPoly( List< ? > dataset, double interval ) {
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for( Object cObj : dataset ) {
            if (cObj instanceof Coordinate) {
                Coordinate c = (Coordinate) cObj;
                x.add(c.x);
                y.add(c.y);
                maxX = Math.max(maxX, c.x);
                minX = Math.min(minX, c.x);
            } else if (cObj instanceof double[]) {
                double[] c = (double[]) cObj;
                x.add(c[0]);
                y.add(c[1]);
                maxX = Math.max(maxX, c[0]);
                minX = Math.min(minX, c[0]);
            } else if (cObj instanceof List) {
                List c = (List) cObj;
                double xv = ((Number) c.get(0)).doubleValue();
                double yv = ((Number) c.get(1)).doubleValue();
                x.add(xv);
                y.add(yv);
                maxX = Math.max(maxX, xv);
                minX = Math.min(minX, xv);
            }
        }

        OddEvenSortAlgorithm.oddEvenSort(x, y);

        PolynomialInterpolator polyInt = new PolynomialInterpolator(x, y);

        List<Coordinate> coords = new ArrayList<>();
        double runningX = minX;
        while( runningX <= maxX ) {
            double interpY = polyInt.getInterpolated(runningX);
            Coordinate c = new Coordinate(runningX, interpY);
            coords.add(c);
            runningX += interval;
        }
        return GeometryUtilities.gf().createLineString(coords.toArray(new Coordinate[coords.size()]));
    }

}
