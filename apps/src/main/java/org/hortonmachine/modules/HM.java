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
package org.hortonmachine.modules;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.geopackage.GeopackageDb;
import org.hortonmachine.dbs.h2gis.H2GisDb;
import org.hortonmachine.dbs.postgis.PostgisDb;
import org.hortonmachine.dbs.spatialite.hm.SpatialiteThreadsafeDb;
import org.hortonmachine.dbs.spatialite.hm.SqliteDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.chart.CategoryHistogram;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.math.regressions.LogTrendLine;
import org.hortonmachine.gears.utils.math.regressions.PolyTrendLine;
import org.hortonmachine.gears.utils.math.regressions.RegressionLine;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.HMMapframe;
import org.hortonmachine.gui.utils.OmsMatrixCharter;
import org.jfree.chart.ChartPanel;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Coordinate;

import geoscript.style.Style;
import geoscript.style.io.SLDReader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Scripting support class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("rawtypes")
public class HM {

    public static String methods() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("Generic tools").append("\n");
        sb.append("-------------").append("\n");
        sb.append("Load an external geoscript file:").append("\n");
        sb.append("\tload( String scriptPath )").append("\n");
        sb.append("Convert an milliseconds epoch timestamp to string:").append("\n");
        sb.append("\tString ts2str( long millis )").append("\n");
        sb.append("Convert an iso timestamp string to milliseconds:").append("\n");
        sb.append("\tlong str2ts( String isoString )").append("\n");
        sb.append("Adds a prj file to all the files inside a folder:").append("\n");
        sb.append("\taddPrjs( String folder, int epsg )").append("\n");

        sb.append("\n");
        sb.append("Chart tools").append("\n");
        sb.append("-----------").append("\n");
        sb.append("Chart a matrix of numeric data:").append("\n");
        sb.append(
                "\tchartMatrix( String title, String xLabel, String yLabel, double[][] data, List<String> series, List<String> colors, boolean doLegend )")
                .append("\n");
        sb.append("Chart a category histogram:").append("\n");
        sb.append("\thistogram( Map<String, Object> options, List<String> categories, List<Number> values )").append("\n");
        sb.append("Chart a numeric histogram:").append("\n");
        sb.append("\thistogram( Map<String, Object> options, List<List<Number>> values )").append("\n");
        sb.append("Chart series of points:").append("\n");
        sb.append("\tscatterPlot( List<List<List<Number>>> data )").append("\n");
        sb.append("Chart series of points with rendering options:").append("\n");
        sb.append("\tscatterPlot( Map<String, Object> options, List<List<List<Number>>> data )").append("\n");
        sb.append("Calculate a log regression:").append("\n");
        sb.append("\tRegressionLine logRegression( List<List<Double>> data, List<List<Double>> result )").append("\n");
        sb.append("Calculate a polynomial regression:").append("\n");
        sb.append("\tRegressionLine polynomialRegression( Number degree, List<List<Double>> data, List<List<Double>> result )")
                .append("\n");

        sb.append("\n");
        sb.append("Databases tools").append("\n");
        sb.append("---------------").append("\n");
        sb.append("Connect to PostGIS:").append("\n");
        sb.append("\tASpatialDb connectPostgis( String host, int port, String database, String user, String pwd )").append("\n");
        sb.append("Connect to Spatialite:").append("\n");
        sb.append("\tASpatialDb connectSpatialite( String databasePath )").append("\n");
        sb.append("Connect to Geopackage:").append("\n");
        sb.append("\tASpatialDb connectGeopackage( String databasePath )").append("\n");
        sb.append("Connect to Sqlite:").append("\n");
        sb.append("\tSqliteDb connectSqlite( String databasePath )").append("\n");
        sb.append("Connect to H2GIS:").append("\n");
        sb.append("\tASpatialDb connectH2GIS( String databasePath, String user, String pwd )").append("\n");

        sb.append("\n");
        sb.append("Rendering tools").append("\n");
        sb.append("---------------").append("\n");
        sb.append("Open a simple spatial viewer on a folder of data:").append("\n");
        sb.append("\tshowFolder( String folderPath )").append("\n");
        sb.append("Print the available raster colortables:").append("\n");
        sb.append("\tString printColorTables()").append("\n");
        sb.append("Create the style object for a given colortable:").append("\n");
        sb.append("\tStyle styleForColorTable( String tableName, double min, double max, double opacity )").append("\n");

        sb.append("\n");
        sb.append("Distance tools").append("\n");
        sb.append("--------------").append("\n");
        sb.append("Calculate the distance between two lat/long WGS84 coordinates:").append("\n");
        sb.append("\tdouble distanceLL( Coordinate c1, Coordinate c2 )").append("\n");
        sb.append("Calculate the distance between two lat/long WGS84 coordinates:").append("\n");
        sb.append("\tdouble distanceLL( double lon1, double lat1, double lon2, double lat2 )").append("\n");

        sb.append("\n");
        sb.append("Dialogs").append("\n");
        sb.append("-------").append("\n");

        sb.append("Dialog to prompt for user input:").append("\n");
        sb.append("\tString showInputDialog( String message, String defaultInput )").append("\n");
        sb.append("Dialog to prompt user for yes or no:").append("\n");
        sb.append("\tboolean showYesNoDialog( String message )").append("\n");
        sb.append("Combo box dialog for multiple choice:").append("\n");
        sb.append("\tString showComboDialog( String title, String message, String[] values, String selectedValue )").append("\n");
        sb.append("Info message dialog:").append("\n");
        sb.append("\tshowInfoMessage( String message )").append("\n");
        sb.append("Warning message dialog:").append("\n");
        sb.append("\tshowWarningMessage( String message )").append("\n");
        sb.append("Error message dialog:").append("\n");
        sb.append("\tshowErrorMessage( String message )").append("\n");

        return sb.toString();
    }

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

    public static void histogram( Map<String, Object> options, List<String> categories, List<Number> values ) {
        String title = "";
        String xLabel = "x";
        String yLabel = "y";
        int width = 1600;
        int height = 1000;
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
            object = options.get("width");
            if (object instanceof Number) {
                width = ((Number) object).intValue();
            }
            object = options.get("height");
            if (object instanceof Number) {
                height = ((Number) object).intValue();
            }
        }

        double[] valuesDouble = new double[values.size()];
        for( int i = 0; i < valuesDouble.length; i++ ) {
            valuesDouble[i] = values.get(i).doubleValue();
        }
        CategoryHistogram categoryHistogram = new CategoryHistogram(title, categories.toArray(new String[categories.size()]),
                valuesDouble);
        categoryHistogram.setXLabel(xLabel);
        categoryHistogram.setYLabel(yLabel);
        ChartPanel chartPanel = new ChartPanel(categoryHistogram.getChart(), true);
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);

    }

    public static void histogram( Map<String, Object> options, List<List<Number>> values ) {
        String title = "";
        String xLabel = "x";
        String yLabel = "y";
        int width = 1600;
        int height = 1000;
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
            object = options.get("width");
            if (object instanceof Number) {
                width = ((Number) object).intValue();
            }
            object = options.get("height");
            if (object instanceof Number) {
                height = ((Number) object).intValue();
            }
        }

        String[] categories = new String[values.size()];
        double[] valuesDouble = new double[values.size()];
        for( int i = 0; i < valuesDouble.length; i++ ) {
            List<Number> pair = values.get(i);
            categories[i] = pair.get(0).toString();
            valuesDouble[i] = pair.get(1).doubleValue();
        }
        CategoryHistogram categoryHistogram = new CategoryHistogram(title, categories, valuesDouble);
        categoryHistogram.setXLabel(xLabel);
        categoryHistogram.setYLabel(yLabel);
        ChartPanel chartPanel = new ChartPanel(categoryHistogram.getChart(), true);
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);

    }

    public static void scatterPlot( List<List<List<Number>>> data ) {
        scatterPlot(null, data);
    }

    @SuppressWarnings("unchecked")
    public static void scatterPlot( Map<String, Object> options, List<List<List<Number>>> data ) {
        String title = "";
        String xLabel = "x";
        String yLabel = "y";
        List<String> series = null;
        List<String> colors = null;
        List<Boolean> doLines = null;
        List<Boolean> doShapes = null;
        int width = 1600;
        int height = 1000;

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
            if (object instanceof List) {
                doLines = (List) object;
            }
            object = options.get("doshapes");
            if (object instanceof List) {
                doShapes = (List) object;
            }
            object = options.get("width");
            if (object instanceof Number) {
                width = ((Number) object).intValue();
            }
            object = options.get("height");
            if (object instanceof Number) {
                height = ((Number) object).intValue();
            }
        }

        Scatter scatterChart = new Scatter(title);
        if (doLines != null)
            scatterChart.setShowLines(doLines);
        if (doShapes != null)
            scatterChart.setShowShapes(doShapes);
        scatterChart.setXLabel(xLabel);
        scatterChart.setYLabel(yLabel);

        int index = 0;
        for( List<List<Number>> seriesData : data ) {
            String name = "data " + (index + 1);
            if (series != null)
                name = series.get(index);
            double[] x = new double[seriesData.size()];
            double[] y = new double[seriesData.size()];
            for( int i = 0; i < seriesData.size(); i++ ) {
                List<Number> pair = seriesData.get(i);
                x[i] = pair.get(0).doubleValue();
                y[i] = pair.get(1).doubleValue();
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
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);
    }

    public static RegressionLine logRegression( List<List<Double>> data, List<List<Double>> result ) {
        RegressionLine t = new LogTrendLine();
        return processregression(data, result, t);
    }

    public static RegressionLine polynomialRegression( Number degree, List<List<Double>> data, List<List<Double>> result ) {
        RegressionLine t = new PolyTrendLine(degree.intValue());
        return processregression(data, result, t);
    }

    private static RegressionLine processregression( List<List<Double>> data, List<List<Double>> result,
            RegressionLine function ) {
        double[] x = new double[data.size()];
        double[] y = new double[data.size()];
        for( int i = 0; i < data.size(); i++ ) {
            List<Double> pair = data.get(i);
            x[i] = pair.get(0);
            y[i] = pair.get(1);
        }
        function.setValues(y, x);

        for( int i = 0; i < x.length; i++ ) {
            double predY = function.predict(x[i]);
            result.add(Arrays.asList(x[i], predY));
        }
        return function;
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

    public static GeopackageDb connectGeopackage( String databasePath ) throws Exception {
        GeopackageDb spatialDb = (GeopackageDb) EDb.GEOPACKAGE.getSpatialDb();
        spatialDb.setMakePooled(false);
        spatialDb.open(databasePath);
        return spatialDb;
    }

    public static SqliteDb connectSqlite( String databasePath ) throws Exception {
        SqliteDb spatialDb = (SqliteDb) EDb.SQLITE.getDb();
        spatialDb.setMakePooled(false);
        spatialDb.open(databasePath);
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

    public static String printColorTables() {
        StringBuilder sb = new StringBuilder();
        for( EColorTables table : EColorTables.values() ) {
            sb.append(",").append(table.name());
        }
        return sb.substring(1);
    }

    /**
     * Calculate the orthodromic distance between two coordinates.
     * 
     * @param c1 the first coordinate.
     * @param c2 the second coordinate.
     * @return the distance in meters.
     */
    public static double distanceLL( Coordinate c1, Coordinate c2 ) {
        return distanceLL(c1.x, c1.y, c2.x, c2.y);
    }

    public static double distanceLL( double lon1, double lat1, double lon2, double lat2 ) {
        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        gc.setStartingGeographicPoint(lon1, lat1);
        gc.setDestinationGeographicPoint(lon2, lat2);
        double distance = gc.getOrthodromicDistance();
        return distance;
    }

    public static Style styleForColorTable( String tableName, double min, double max, double opacity ) throws Exception {
        return new SLDReader().read(
                RasterStyleUtilities.styleToString(RasterStyleUtilities.createStyleForColortable(tableName, min, max, opacity)));
    }

    public static String ts2str( long millis ) {
        return new DateTime(millis).toString(HMConstants.utcDateFormatterYYYYMMDDHHMMSS);
    }

    public static long str2ts( String isoString ) {
        return HMConstants.utcDateFormatterYYYYMMDDHHMMSS.parseDateTime(isoString).getMillis();
    }

    public static String showInputDialog( String message, String defaultInput ) {
        String answer = GuiUtilities.showInputDialog(null, message, defaultInput);
        return answer;
    }

    public static boolean showYesNoDialog( String message ) {
        return GuiUtilities.showYesNoDialog(null, message);
    }

    public static String showComboDialog( String title, String message, String[] values, String selectedValue ) {
        String result = GuiUtilities.showComboDialog(null, message, title, values, selectedValue);
        return result;
    }

    public static void showInfoMessage( String message ) {
        GuiUtilities.showInfoMessage(null, null, message);
    }

    public static void showWarningMessage( String message ) {
        GuiUtilities.showWarningMessage(null, null, message);
    }

    public static void showErrorMessage( String message ) {
        GuiUtilities.showErrorMessage(null, null, message);
    }

    public static void addPrjs( String folder, int epsg ) throws Exception {
        FileIterator.addPrj(folder, "EPSG:" + epsg);
    }

}
