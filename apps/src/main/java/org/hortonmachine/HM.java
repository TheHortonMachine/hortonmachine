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
package org.hortonmachine;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.h2gis.H2GisDb;
import org.hortonmachine.dbs.postgis.PostgisDb;
import org.hortonmachine.dbs.spatialite.hm.SpatialiteThreadsafeDb;
import org.hortonmachine.dbs.spatialite.hm.SqliteDb;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.chart.CategoryHistogram;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.chart.TimeSeries;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.math.regressions.LogTrendLine;
import org.hortonmachine.gears.utils.math.regressions.PolyTrendLine;
import org.hortonmachine.gears.utils.math.regressions.RegressionLine;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.HMMapframe;
import org.hortonmachine.gui.utils.OmsMatrixCharter;
import org.hortonmachine.modules.FileIterator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.AbstractXYAnnotation;
import org.jfree.chart.annotations.XYImageAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYPolygonAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.opengis.feature.simple.SimpleFeature;

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
        sb.append("Get colortable colorinterpolator to use the with getColorFor( double value ):").append("\n");
        sb.append("\tgetColorInterpolator( String colortable, double min, double max, Integer alpha )").append("\n");

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
        sb.append("\thistogram( Map<String, Object> options, List<List<Number>> pairsValuesList )").append("\n");
        sb.append("Chart a time series:").append("\n");
        sb.append("\ttimeseries( Map<String, Object> options, List<List<List<Number>>> pairsValuesLists )").append("\n");
        sb.append("Chart a time series:").append("\n");
        sb.append("\ttimeseries( List<List<List<Number>>> pairsValuesLists )").append("\n");
        sb.append("Chart series of points:").append("\n");
        sb.append("\tscatterPlot( List<List<List<Number>>> pairsValuesLists )").append("\n");
        sb.append("Chart series of points with rendering options:").append("\n");
        sb.append("\tscatterPlot( Map<String, Object> options, List<List<List<Number>>> pairsValuesLists )").append("\n");
        sb.append("Chart a list of geometries with optional rendering options:").append("\n");
        sb.append("\tplotGeometries( Map<String, Object> options, List<geoscript.geom.Geometry> geomsList )").append("\n");
        sb.append("Calculate a log regression (use result.predict( x ) to get y):").append("\n");
        sb.append("\tRegressionLine logRegression( List<List<Double>> data, List<List<Double>> result )").append("\n");
        sb.append("Calculate a polynomial regression (use result.predict( x ) to get y):").append("\n");
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
        sb.append("Run query:").append("\n");
        sb.append("\tList<HashMap<String, Object>> query( ADb db, String sql )").append("\n");
        sb.append("Execute insert/update/delete:").append("\n");
        sb.append("\tint execute( ADb db , String sql)").append("\n");
        sb.append("Execute batched insert/update/delete:").append("\n");
        sb.append("\texecuteBatched( ADb db, List<String> sqlList, Consumer<String> updateMessageConsumer )").append("\n");

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
        sb.append("Spatial tools").append("\n");
        sb.append("--------------").append("\n");
        sb.append("Calculate the distance between two lat/long WGS84 coordinates:").append("\n");
        sb.append("\tdouble distanceLL( Coordinate c1, Coordinate c2 )").append("\n");
        sb.append("Calculate the distance between two lat/long WGS84 coordinates:").append("\n");
        sb.append("\tdouble distanceLL( double lon1, double lat1, double lon2, double lat2 )").append("\n");
        sb.append("Create a spatial index from a list of [Envelope, Object] pairs:").append("\n");
        sb.append("\tSTRtree getSpatialIndex( List<List<Object>> objects )").append("\n");
        sb.append("Read a vector the geotools way as featurecollection:").append("\n");
        sb.append("\tSimpleFeatureCollection readVector( String path )").append("\n");
        sb.append("Read a vector's envelope the geotools way as tReferencedEnvelope:").append("\n");
        sb.append("\tReferencedEnvelope readEnvelope( String path )").append("\n");
        sb.append("Read a raster the geotools way as GridCoverage2D:").append("\n");
        sb.append("\tGridCoverage2D readRaster( String source )").append("\n");
        sb.append("Write a geotools raster to file:").append("\n");
        sb.append("\tdumpRaster( GridCoverage2D raster, String source )").append("\n");
        sb.append("Write a geotools vector to file:").append("\n");
        sb.append("\tdumpVector( SimpleFeatureCollection vector, String source )").append("\n");

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

    public static List<SimpleFeature> readVector( String path ) throws Exception {
        if (path == null || path.trim().length() == 0)
            return null;
        return FeatureUtilities.featureCollectionToList(OmsVectorReader.readVector(path));
    }

    public static ReferencedEnvelope readEnvelope( String path ) throws Exception {
        if (path == null || path.trim().length() == 0)
            return null;
        return OmsVectorReader.readEnvelope(path);
    }

    public static GridCoverage2D readRaster( String source ) throws Exception {
        if (source == null || source.trim().length() == 0)
            return null;
        return OmsRasterReader.readRaster(source);
    }

    public static void dumpRaster( GridCoverage2D raster, String source ) throws Exception {
        if (raster == null || source == null)
            return;
        OmsRasterWriter.writeRaster(source, raster);
    }

    public static void dumpVector( SimpleFeatureCollection vector, String source ) throws Exception {
        if (vector == null || source == null)
            return;
        OmsVectorWriter.writeVector(source, vector);
    }

    public static STRtree getSpatialIndex( List<List<Object>> objects ) {
        STRtree tree = new STRtree();
        for( Object envAndObject : objects ) {
            if (envAndObject instanceof List) {
                Envelope env = (Envelope) ((List) envAndObject).get(0);
                Object obj = ((List) envAndObject).get(1);
                tree.insert(env, obj);
            }
        }
        return tree;
    }

    public static void showFolder( String folderPath ) {
        HMMapframe.openFiles(new File[]{new File(folderPath)});
    }

    public static Script load( String scriptPath ) throws Exception {
        GroovyShell gsh = new GroovyShell();
        return gsh.parse(new File(scriptPath));
    }

    public static void chartMatrix( String title, String xLabel, String yLabel, List<List<Number>> data, List<String> series,
            List<String> colors, boolean doLegend ) {
        double[][] dataMatrix = new double[data.size()][data.get(0).size()];
        for( int i = 0; i < data.size(); i++ ) {
            List<Number> row = data.get(i);
            for( int j = 0; j < row.size(); j++ ) {
                dataMatrix[i][j] = row.get(j).doubleValue();
            }
        }
        chartMatrix(title, xLabel, yLabel, dataMatrix, series, colors, doLegend);
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

    public static void timeseries( Map<String, Object> options, List<String> seriesNames, List<List<Number>> times,
            List<List<Number>> values ) {
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

        List<double[]> allValuesList = new ArrayList<>();
        for( List<Number> list : values ) {
            double[] valuesDouble = new double[list.size()];
            for( int i = 0; i < valuesDouble.length; i++ ) {
                valuesDouble[i] = list.get(i).doubleValue();
            }
            allValuesList.add(valuesDouble);
        }
        List<long[]> allTimesList = new ArrayList<>();
        for( List<Number> list : times ) {
            long[] timesLong = new long[list.size()];
            for( int i = 0; i < timesLong.length; i++ ) {
                timesLong[i] = list.get(i).longValue();
            }
            allTimesList.add(timesLong);
        }

        TimeSeries timeseriesChart = new TimeSeries(title, seriesNames, allTimesList, allValuesList);
        timeseriesChart.setXLabel(xLabel);
        timeseriesChart.setYLabel(yLabel);

        ChartPanel chartPanel = new ChartPanel(timeseriesChart.getChart(), true);
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);
    }

    public static void timeseries( Map<String, Object> options, List<String> seriesNames,
            List<List<List<Number>>> timesValuesList ) {
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

        List<double[]> allValuesList = new ArrayList<>();
        List<long[]> allTimesList = new ArrayList<>();
        for( List<List<Number>> list : timesValuesList ) {
            double[] valuesDouble = new double[list.size()];
            long[] timesLong = new long[list.size()];
            for( int i = 0; i < valuesDouble.length; i++ ) {
                List<Number> timeValue = list.get(i);
                valuesDouble[i] = timeValue.get(1).doubleValue();
                timesLong[i] = timeValue.get(0).longValue();
            }
            allValuesList.add(valuesDouble);
            allTimesList.add(timesLong);
        }

        TimeSeries timeseriesChart = new TimeSeries(title, seriesNames, allTimesList, allValuesList);
        timeseriesChart.setXLabel(xLabel);
        timeseriesChart.setYLabel(yLabel);

        ChartPanel chartPanel = new ChartPanel(timeseriesChart.getChart(), true);
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);
    }
    public static void timeseries( List<List<List<Number>>> timesValuesList ) {
        timeseries(null, timesValuesList);
    }

    @SuppressWarnings("unchecked")
    public static void timeseries( Map<String, Object> options, List<List<List<Number>>> timesValuesList ) {
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
            object = options.get("width");
            if (object instanceof Number) {
                width = ((Number) object).intValue();
            }
            object = options.get("height");
            if (object instanceof Number) {
                height = ((Number) object).intValue();
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
        }

        List<double[]> allValuesList = new ArrayList<>();
        List<long[]> allTimesList = new ArrayList<>();
        for( List<List<Number>> list : timesValuesList ) {
            double[] valuesDouble = new double[list.size()];
            long[] timesLong = new long[list.size()];
            for( int i = 0; i < valuesDouble.length; i++ ) {
                List<Number> timeValue = list.get(i);
                valuesDouble[i] = timeValue.get(1).doubleValue();
                timesLong[i] = timeValue.get(0).longValue();
            }
            allValuesList.add(valuesDouble);
            allTimesList.add(timesLong);
        }

        TimeSeries timeseriesChart = new TimeSeries(title, series, allTimesList, allValuesList);
        timeseriesChart.setXLabel(xLabel);
        timeseriesChart.setYLabel(yLabel);
        if (doLines != null)
            timeseriesChart.setShowLines(doLines);
        if (doShapes != null)
            timeseriesChart.setShowShapes(doShapes);

        if (colors != null) {
            List<Color> colorsList = colors.stream().map(cStr -> {
                return ColorUtilities.fromHex(cStr);
            }).collect(Collectors.toList());
            timeseriesChart.setColors(colorsList.toArray(new Color[0]));
        }

        ChartPanel chartPanel = new ChartPanel(timeseriesChart.getChart(), true);
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);
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

    public static void plotGeometries( List<geoscript.geom.Geometry> geomsList ) {
        plotJtsGeometries(null, geomsList.stream().map(gg -> gg.getG()).collect(Collectors.toList()));
    }

    public static void plotGeometries( Map<String, Object> options, List<geoscript.geom.Geometry> geomsList ) {
        plotJtsGeometries(options, geomsList.stream().map(gg -> gg.getG()).collect(Collectors.toList()));
    }

    public static void plotJtsGeometries( Map<String, Object> options, List<Geometry> geomsList ) {
        String title = "";
        String xLabel = "x";
        String yLabel = "y";
        int width = 1200;
        int height = 800;
        int strokeWidth = 2;
        boolean drawCoords = true;

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
            object = options.get("size");
            if (object instanceof List) {
                List size = (List) object;
                try {
                    if (size.size() == 2) {
                        width = ((Number) size.get(0)).intValue();
                        height = ((Number) size.get(1)).intValue();
                    }
                } catch (Exception e) {
                    // ignore and use default
                }
            }
            object = options.get("width");
            if (object instanceof Number) {
                width = ((Number) object).intValue();
            }
            object = options.get("height");
            if (object instanceof Number) {
                height = ((Number) object).intValue();
            }
            object = options.get("strokeWidth");
            if (object instanceof Number) {
                strokeWidth = ((Number) object).intValue();
            }
            object = options.get("drawCoords");
            if (object instanceof Boolean) {
                drawCoords = (boolean) object;
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries s = new XYSeries("");
        dataset.addSeries(s);
        JFreeChart chart = ChartFactory.createScatterPlot(title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL, false, true,
                false);
        XYPlot plot = (XYPlot) chart.getPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        String[] hexes = {"#e41a1c", "#377eb8", "#4daf4a", "#984ea3", "#ff7f00", "#a65628", "#f781bf", "#999999", "#ffff33"};
        List<Color> tableColors = Arrays.asList(hexes).stream().map(cs -> ColorUtilities.fromHex(cs))
                .collect(Collectors.toList());
        int colorIndex = 0;
        int maxColor = tableColors.size();

        Envelope env = new Envelope();
        List<List<List<Number>>> data = new ArrayList<>();
        for( Geometry geometry : geomsList ) {
            Envelope envelope = geometry.getEnvelopeInternal();
            env.expandToInclude(envelope);
            List<AbstractXYAnnotation> annots = new ArrayList<>();
            for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
                if (colorIndex == maxColor) {
                    colorIndex = 0;
                }
                Color color = tableColors.get(colorIndex);
                Color colorTransp = ColorUtilities.makeTransparent(color, 60);
                Color transp = ColorUtilities.makeTransparent(Color.white, 0);
                int dotImageWidth = strokeWidth * 4;
                if (dotImageWidth < 10) {
                    dotImageWidth = 10;
                }
                BufferedImage bi = new BufferedImage(dotImageWidth, dotImageWidth, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = (Graphics2D) bi.getGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(strokeWidth));
                g2d.fillRect(strokeWidth, strokeWidth, dotImageWidth - 2 * strokeWidth, dotImageWidth - 2 * strokeWidth);
                g2d.dispose();

                Geometry geometryN = geometry.getGeometryN(i);
                List<List<Number>> geomDataList = new ArrayList<>();
                data.add(geomDataList);

                if (EGeometryType.isPoint(geometryN)) {
                    Coordinate c = geometryN.getCoordinates()[0];
                    XYImageAnnotation node = new XYImageAnnotation(c.x, c.y, bi);
                    node.setToolTipText(geometry.toString());
                    annots.add(node);
                } else if (EGeometryType.isLine(geometryN)) {
                    Coordinate[] coordinates = geometryN.getCoordinates();
                    for( int j = 0; j < coordinates.length - 1; j++ ) {
                        Coordinate c1 = coordinates[j];
                        Coordinate c2 = coordinates[j + 1];
                        XYLineAnnotation lineAnn = new XYLineAnnotation(c1.x, c1.y, c2.x, c2.y, new BasicStroke(strokeWidth),
                                color);
                        lineAnn.setToolTipText(geometryN.toString());
                        annots.add(0, lineAnn);
                        if (drawCoords) {
                            if (j == 0) {
                                XYImageAnnotation node = new XYImageAnnotation(c1.x, c1.y, bi);
                                annots.add(node);
                            }
                            XYImageAnnotation node = new XYImageAnnotation(c2.x, c2.y, bi);
                            annots.add(node);
                        }
                    }
                } else if (EGeometryType.isPolygon(geometryN)) {
                    Polygon polygon = (Polygon) geometryN;

                    DelaunayTriangulationBuilder b = new DelaunayTriangulationBuilder();
                    b.setSites(polygon);
                    Geometry triangles = b.getTriangles(new GeometryFactory());

                    for( int j = 0; j < triangles.getNumGeometries(); j++ ) {
                        Geometry g = triangles.getGeometryN(j);
                        Point interiorPoint = g.getInteriorPoint();
                        if (interiorPoint.intersects(polygon)) {
                            Coordinate[] cs = g.getCoordinates();
                            double[] cd = new double[cs.length * 2];
                            int indexInt = 0;
                            for( Coordinate c : cs ) {
                                cd[indexInt++] = c.x;
                                cd[indexInt++] = c.y;
                            }
                            XYPolygonAnnotation a = new XYPolygonAnnotation(cd, null, null, colorTransp);
                            a.setToolTipText(geometryN.toString());
                            annots.add(0, a);
                        }
                    }

                    LineString exteriorRing = polygon.getExteriorRing();
                    Coordinate[] coordinates = exteriorRing.getCoordinates();
                    double[] coords = new double[coordinates.length * 2];
                    int index = 0;
                    for( Coordinate c : coordinates ) {
                        coords[index++] = c.x;
                        coords[index++] = c.y;
                        if (drawCoords) {
                            XYImageAnnotation node = new XYImageAnnotation(c.x, c.y, bi);
                            annots.add(node);
                        }
                    }

                    for( int j = 0; j < polygon.getNumInteriorRing(); j++ ) {
                        LineString interiorRingN = polygon.getInteriorRingN(j);
                        Coordinate[] coordinatesInt = interiorRingN.getCoordinates();
                        double[] coordsInt = new double[coordinatesInt.length * 2];
                        int indexInt = 0;
                        for( Coordinate c : coordinatesInt ) {
                            coordsInt[indexInt++] = c.x;
                            coordsInt[indexInt++] = c.y;
                            if (drawCoords) {
                                XYImageAnnotation node = new XYImageAnnotation(c.x, c.y, bi);
                                annots.add(node);
                            }
                        }
                        XYPolygonAnnotation aInt = new XYPolygonAnnotation(coordsInt, new BasicStroke(strokeWidth), color,
                                transp);
                        annots.add(0, aInt);
                    }
                    XYPolygonAnnotation a = new XYPolygonAnnotation(coords, new BasicStroke(strokeWidth), color, transp);
                    annots.add(0, a);
                }

                for( AbstractXYAnnotation ann : annots ) {
                    renderer.addAnnotation(ann);
                }
                colorIndex++;
            }
        }

        double deltaX = env.getWidth() * 0.1;
        double deltaY = env.getHeight() * 0.1;
        s.add(env.getMinX() - deltaX, env.getMinY() - deltaY);
        s.add(env.getMaxX() + deltaX, env.getMaxY() + deltaY);

        ChartPanel chartPanel = new ChartPanel(chart, true);
        chartPanel.setRangeZoomable(true);
        chartPanel.setDomainZoomable(true);

        renderer.setSeriesPaint(0, ColorUtilities.makeTransparent(Color.red, 0));

        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "Simple Geometry Plot", preferredSize, false);
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

    public static GeopackageCommonDb connectGeopackage( String databasePath ) throws Exception {
        GeopackageCommonDb spatialDb = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb();
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

    public static List<HashMap<String, Object>> query( ADb db, String sql ) throws Exception {
        QueryResult result = db.getTableRecordsMapFromRawSql(sql, -1);
        List<String> names = result.names;
        List<Object[]> data = result.data;
        List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

        for( Object[] objects : data ) {
            HashMap<String, Object> map = new LinkedHashMap<String, Object>();
            for( int i = 0; i < objects.length; i++ ) {
                Object object = objects[i];
                String name = names.get(i);
                map.put(name, object);
            }
            list.add(map);
        }
        return list;
    }

    public static int execute( ADb db, String sql ) throws Exception {
        return db.executeInsertUpdateDeleteSql(sql);
    }

    public static void executeBatched( ADb db, List<String> sqlList, Consumer<String> updateMessageConsumer ) throws Exception {
        db.execOnConnection(conn -> {
            int count = 0;
            int lastBatchCount = 0;
            try (IHMStatement stmt = conn.createStatement()) {
                for( String sql : sqlList ) {
                    stmt.addBatch(sql);
                    count++;
                    if (count % 10000 == 0) {
                        stmt.executeBatch();
                        lastBatchCount = count;
                        if (updateMessageConsumer != null) {
                            updateMessageConsumer.accept("Updated pipes: " + count);
                        }
                    }
                }
                if (lastBatchCount != count) {
                    stmt.executeBatch();
                }
            }

            return null;
        });

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

    public static ColorInterpolator getColorInterpolator( String colortable, double min, double max, Integer alpha ) {
        ColorInterpolator ci = new ColorInterpolator(colortable, min, max, alpha);
        return ci;
    }

}
