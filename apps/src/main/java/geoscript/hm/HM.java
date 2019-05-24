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

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.h2gis.H2GisDb;
import org.hortonmachine.dbs.postgis.PostgisDb;
import org.hortonmachine.dbs.spatialite.hm.SpatialiteThreadsafeDb;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.interpolation.LeastSquaresInterpolator;
import org.hortonmachine.gears.utils.math.interpolation.PolynomialInterpolator;
import org.hortonmachine.gears.utils.sorting.OddEvenSortAlgorithm;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.HMMapframe;
import org.hortonmachine.gui.utils.OmsMatrixCharter;
import org.jfree.chart.ChartPanel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

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

    public static Script load( String scriptPath ) throws Exception {
        GroovyShell gsh = new GroovyShell();
        return gsh.parse(new File(scriptPath));
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

    public static void scatterPlot( List<List<List<Double>>> data ) {
        scatterPlot(null, data);
    }

    @SuppressWarnings("unchecked")
    public static void scatterPlot( Map<String, Object> options, List<List<List<Double>>> data ) {
        String title = "";
        String xLabel = "x";
        String yLabel = "y";
        List<String> series = null;
        List<String> colors = null;
        boolean doLines = false;

        if (options != null) {
            Object object = options.get("title");
            if (object instanceof String) {
                title = (String) object;
            }
            object = options.get("xlabel");
            if (object instanceof String) {
                xLabel = (String) object;
            }
            object = options.get("ylabel");
            if (object instanceof String) {
                yLabel = (String) object;
            }
            object = options.get("series");
            if (object instanceof List) {
                series = (List) object;
            }
            object = options.get("colors");
            if (object instanceof List) {
                colors = (List) object;
            }
            object = options.get("dolines");
            if (object instanceof Boolean) {
                doLines = (Boolean) object;
            }
        }

        Scatter scatterChart = new Scatter(title);
        scatterChart.setShowLines(doLines);
        scatterChart.setXLabel(xLabel);
        scatterChart.setYLabel(yLabel);

        int index = 0;
        for( List<List<Double>> seriesData : data ) {
            String name = "data " + (index + 1);
            if (series != null)
                name = series.get(index);
            double[] x = new double[seriesData.size()];
            double[] y = new double[seriesData.size()];
            for( int i = 0; i < seriesData.size(); i++ ) {
                List<Double> pair = seriesData.get(i);
                x[i] = pair.get(0);
                y[i] = pair.get(1);
            }
            scatterChart.addSeries(name, x, y);
            index++;
        }

        if (colors != null) {
            List<Color> colorsList = colors.stream().map(cStr -> {
                return ColorUtilities.fromHex(cStr);
            }).collect(Collectors.toList());
            scatterChart.setColors(colorsList.toArray(new Color[0]));
        }

        ChartPanel chartPanel = new ChartPanel(scatterChart.getChart(), true);
        Dimension preferredSize = new Dimension(1600, 1000);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);
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

    public static ASpatialDb connectPostgis( String host, int port, String database, String user, String pwd ) throws Exception {
        PostgisDb spatialDb = (PostgisDb) EDb.POSTGIS.getSpatialDb();
        String dbPath = host + ":" + port + "/" + database;
        spatialDb.setMakePooled(false);
        if (user != null && pwd != null)
            spatialDb.setCredentials(user, pwd);
        spatialDb.open(dbPath);
        spatialDb.initSpatialMetadata(null);
        return spatialDb;
    }

    public static ASpatialDb connectSpatialite( String databasePath ) throws Exception {
        SpatialiteThreadsafeDb spatialDb = (SpatialiteThreadsafeDb) EDb.SPATIALITE.getSpatialDb();
        spatialDb.setMakePooled(false);
        if (!spatialDb.open(databasePath)) {
            spatialDb.initSpatialMetadata(null);
        }
        return spatialDb;
    }

    public static ASpatialDb connectH2GIS( String databasePath, String user, String pwd ) throws Exception {
        H2GisDb spatialDb = (H2GisDb) EDb.H2GIS.getSpatialDb();
        if (user != null && pwd != null) {
            spatialDb.setCredentials(user, pwd);
        }
        spatialDb.setMakePooled(false);
        if (!spatialDb.open(databasePath)) {
            spatialDb.initSpatialMetadata(null);
        }
        return spatialDb;
    }

}
