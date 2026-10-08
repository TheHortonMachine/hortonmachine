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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.StreamingRenderer;
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
import org.hortonmachine.gears.io.wcs.Wcs;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.TransformationUtils;
import org.hortonmachine.gears.utils.chart.CategoryHistogram;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.chart.TimeSeries;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.RasterCellInfo;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gears.utils.crs.ProjectionInfo;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.images.ImageGenerator;
import org.hortonmachine.gears.utils.images.ImageUtilities;
import org.hortonmachine.gears.utils.math.regressions.LogTrendLine;
import org.hortonmachine.gears.utils.math.regressions.PolyTrendLine;
import org.hortonmachine.gears.utils.math.regressions.RegressionLine;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.HMMapframe;
import org.hortonmachine.gui.utils.OmsMatrixCharter;
import org.hortonmachine.gui.utils.RasterInfoLayer;
import org.hortonmachine.modules.FileIterator;
import org.hortonmachine.modules.RasterSummary;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

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
        sb.append("Chart a simple category histogram:").append("\n");
        sb.append("\thistogram( Map<String, Object> options, List<String> categories, List<Number> values )").append("\n");
        sb.append("Chart a category histogram with multiple series:").append("\n");
        sb.append(
                "\thistogram( Map<String, Object> options, List<String> seriesNames, List<String> categories, List<List<Number>> values )")
                .append("\n");
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
        sb.append("Create the QGIS style file for a given colortable and raster file:").append("\n");
        sb.append("\tmakeQgisStyleForRaster( String tableName, String rasterPath, int labelDecimals )").append("\n");
        sb.append("Create the SLD style file for a given colortable and raster file:").append("\n");
        sb.append("\tmakeSldStyleForRaster( String tableName, String rasterPath )").append("\n");
        sb.append("Create an image with raster info around a given cell (or world position):").append("\n");
        sb.append("\tBufferedImage toImage( String path, cellX, cellY, int bufferCells, String dtm, int width, int height )")
                .append("\n");
        sb.append("Create an image of a list of geometries (format can be geoscript, jts or wkt geometry:").append("\n");
        sb.append("\tBufferedImage toImage( List geomsList )").append("\n");
        sb.append("Create an image from a string, if interpretable (currently image file path and wkt geometry are supported):")
                .append("\n");
        sb.append("\tBufferedImage toImage( String value)").append("\n");

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
        sb.append("Open WCS service:").append("\n");
        sb.append("\twcs( String url )").append("\n");
        sb.append("\twcs( String url, String version )").append("\n");

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

    public static String allMethods() throws Exception {
        Method[] m = HM.class.getDeclaredMethods();
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("All Methods").append("\n");
        sb.append("-------------").append("\n");
        for( Method method : m ) {
            String methodName = method.getName();
            int modifiers = method.getModifiers();
            if (!Modifier.isPrivate(modifiers) && !methodName.startsWith("lambda") && !methodName.startsWith("$")) {
                sb.append(methodName);
                sb.append(" ( ");
                Parameter[] parameters = method.getParameters();
                for( Parameter parameter : parameters ) {
                    Class< ? > type = parameter.getType();
                    sb.append(type.getSimpleName()).append(" ").append(parameter.getName() + ", ");
                }
                sb.append(" )\n");
            }
        }
        return sb.toString();
    }

    /**
     * Reads a vector file and returns a list of SimpleFeature objects.
     * 
     * @param path the path to the vector file
     * @return a list of SimpleFeature objects representing the features in the vector file
     * @throws Exception if an error occurs while reading the vector file
     */
    public static List<SimpleFeature> readVector( String path ) throws Exception {
        if (path == null || path.trim().length() == 0)
            return null;
        return FeatureUtilities.featureCollectionToList(readVectorFc(path));
    }

    /**
     * Reads a vector file and returns a SimpleFeatureCollection.
     * 
     * @param path the path to the vector file
     * @return a SimpleFeatureCollection representing the features in the vector file
     * @throws Exception if an error occurs while reading the vector file
     */
	public static SimpleFeatureCollection readVectorFc(String path) throws Exception {
		return OmsVectorReader.readVector(path);
	}


    /**
     * Reads a vector file and returns its envelope as a ReferencedEnvelope.
     * 
     * @param path the path to the vector file
     * @return a ReferencedEnvelope representing the envelope of the vector file
     * @throws Exception if an error occurs while reading the vector file
     */
    public static ReferencedEnvelope readEnvelope( String path ) throws Exception {
        if (path == null || path.trim().length() == 0)
            return null;
        FileDataStore dataStore = FileDataStoreFinder.getDataStore(new File(path));
        if( dataStore != null ){
            return OmsVectorReader.readEnvelope(path);
        }else{
            // try raster
            return OmsRasterReader.readEnvelope(path);    
        }
    }

    /**
     * Reads a raster file and returns it as a GridCoverage2D.
     * 
     * @param source the path to the raster file
     * @return a GridCoverage2D representing the raster data
     * @throws Exception if an error occurs while reading the raster file
     */
    public static GridCoverage2D readRaster( String source ) throws Exception {
        if (source == null || source.trim().length() == 0)
            return null;

        OmsRasterReader reader = new OmsRasterReader();
        reader.file = source;
        reader.pm = new DummyProgressMonitor();
        reader.process();
        GridCoverage2D geodata = reader.outRaster;
        return geodata;
    }
    
    /**
     * Reads a raster file and returns it as an HMRaster.
     * 
     * @param source the path to the raster file
     * @return an HMRaster representing the raster data
     * @throws Exception if an error occurs while reading the raster file
     */
    public static HMRaster readHMRaster(String source) throws Exception {
    	if (source == null || source.trim().length() == 0)
            return null;
    	return HMRaster.fromFile(source);
    }
    
    /**
     * Dumps an HMRaster to a file.
     * 
     * @param raster the HMRaster to be dumped
     * @param source the path to the output file
     * @throws Exception if an error occurs while writing the raster to the file
     */
    public static void dumpHMRaster( HMRaster raster, String source ) throws Exception {
		if (raster == null || source == null)
			return;
		raster.writeToFile(source);
	}

    /**
     * Dumps a GridCoverage2D raster to a file.
     * 
     * @param raster the GridCoverage2D raster to be dumped
     * @param source the path to the output file
     * @throws Exception if an error occurs while writing the raster to the file
     */
    public static void dumpRaster( GridCoverage2D raster, String source ) throws Exception {
        if (raster == null || source == null)
            return;
        OmsRasterWriter.writeRaster(source, raster);
    }

    /**
     * Dumps a SimpleFeatureCollection vector to a file.
     * 
     * @param vector the SimpleFeatureCollection vector to be dumped
     * @param source the path to the output file
     * @param suggestedSrid the suggested SRID for the output file (can be null)
     * @throws Exception if an error occurs while writing the vector to the file
     */
    public static void dumpVector( SimpleFeatureCollection vector, String source, Integer suggestedSrid ) throws Exception {
    	if (vector == null || source == null)
    		return;
    	OmsVectorWriter.writeVector(source, vector, suggestedSrid);    	
    }
    
    /**
     * Dumps a SimpleFeatureCollection vector to a file.
     * 
     * @param vector the SimpleFeatureCollection vector to be dumped
     * @param source the path to the output file
     * @throws Exception if an error occurs while writing the vector to the file
     */
    public static void dumpVector( SimpleFeatureCollection vector, String source ) throws Exception {
    	dumpVector(vector, source, null);
    }

    /**
     * Returns a string listing all registered raster formats for reading and writing.
     * 
     * @return a string listing all registered raster formats
     */
	public static String getRegisteredRasterFormats() {
		StringBuilder sb = new StringBuilder();
		ServiceLoader<ImageReaderSpi> loader = ServiceLoader.load(ImageReaderSpi.class);
		TreeSet<String> extSet = new TreeSet<String>();
		Iterator<ImageReaderSpi> iterator = loader.iterator();
		sb.append("Registered reader formats:").append("\n");
		while (iterator.hasNext()) {
			try {
				ImageReaderSpi imageReaderSpi = (ImageReaderSpi) iterator.next();
				String[] fileSuffixes = imageReaderSpi.getFileSuffixes();
				if (fileSuffixes.length > 0) {
					String[] formatNames = imageReaderSpi.getFormatNames();
//		            String vendorName = imageReaderSpi.getVendorName();
//		            sb.append(vendorName).append("\n");
					for (int i = 0; i < formatNames.length; i++) {
						if (extSet.add(formatNames[i].toLowerCase())) {
							sb.append(formatNames[i]).append(" ");
						}
					}
					String suff = Arrays.toString(fileSuffixes);
					if (extSet.add(suff.toLowerCase())) {
						sb.append("\t").append(suff).append("\n");
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}
		ServiceLoader<ImageWriterSpi> writerLoader = ServiceLoader.load(ImageWriterSpi.class);
		Iterator<ImageWriterSpi> writerIterator = writerLoader.iterator();
		sb.append("\nRegistered writer formats:").append("\n");
		extSet = new TreeSet<String>();
		while (writerIterator.hasNext()) {
			try {
				ImageWriterSpi imageWriterSpi = (ImageWriterSpi) writerIterator.next();
				String[] fileSuffixes = imageWriterSpi.getFileSuffixes();
				if (fileSuffixes.length > 0) {
					String[] formatNames = imageWriterSpi.getFormatNames();
					// String vendorName = imageWriterSpi.getVendorName();
					// sb.append(vendorName).append("\n");
					for (int i = 0; i < formatNames.length; i++) {
						if (extSet.add(formatNames[i].toLowerCase())) {
							sb.append(formatNames[i]).append(" ");
						}
					}
					String suff = Arrays.toString(fileSuffixes);
					if (extSet.add(suff.toLowerCase())) {
						sb.append("\t").append(suff).append("\n");
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}

		return sb.toString();
	}

    /**
     * Returns a spatial index (STRtree) for a list of objects with their corresponding envelopes.
     * 
     * @param objects a list of objects, each represented as a list containing an Envelope and the object itself
     * @return an STRtree spatial index containing the objects
     */
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

    /**
     * Open a map viewer on a given folder.
     * 
     * @param folderPath the folder to load.
     */
    public static void showFolder( String folderPath ) {
        HMMapframe.openFiles(new File[]{new File(folderPath)});
    }

    /**
     * Load a groovy script to be run with the current script.
     * 
     * @param scriptPath the path to the script.
     * @return the script loaded.
     * @throws Exception
     */
    public static Script load( String scriptPath ) throws Exception {
        GroovyShell gsh = new GroovyShell();
        return gsh.parse(new File(scriptPath));
    }

    /**
     * Creates a chart matrix from a list of lists of numbers.
     * 
     * @param title the title of the chart
     * @param xLabel the label for the x-axis
     * @param yLabel the label for the y-axis
     * @param data the data for the chart as a list of lists of numbers
     * @param series the series names
     * @param colors the colors for the series
     * @param doLegend whether to display the legend
     */
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

    /**
     * Creates a chart matrix from a 2D array of numbers.
     * 
     * @param title the title of the chart
     * @param xLabel the label for the x-axis
     * @param yLabel the label for the y-axis
     * @param data the data for the chart as a 2D array of numbers
     * @param series the series names
     * @param colors the colors for the series
     * @param doLegend whether to display the legend
     */
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

    /**
     * Creates a time series chart from the given options, series names, times, and values.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param seriesNames the names of the series
     * @param times the times for each series as a list of lists of numbers
     * @param values the values for each series as a list of lists of numbers
     */
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

    /**
     * Creates a time series chart from the given options, series names, and a list of times and values.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param seriesNames the names of the series
     * @param timesValuesList a list of lists of times and values for each series, where each inner list contains pairs of [time, value]
     */
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

    /**
     * Creates a time series chart from the given list of times and values.
     * 
     * @param timesValuesList a list of lists of times and values for each series, where each inner list contains pairs of [time, value]
     */
    public static void timeseries( List<List<List<Number>>> timesValuesList ) {
        timeseries(null, timesValuesList);
    }

    /**
     * Creates a time series chart from the given options and a list of times and values.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param timesValuesList a list of lists of times and values for each series, where each inner list contains pairs of [time, value]
     */
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

    /**
     * Creates a histogram chart from the given options and values.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param categories the categories for the histogram
     * @param values the values for each category
     */
    public static void histogram( Map<String, ?> options, List<String> categories, List<? extends Number> values ) {
        histogram(options, Arrays.asList(""), categories, values);
    }

    /**
     * Creates a histogram chart from the given options, series names, categories, and values.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param series the names of the series
     * @param categories the categories for the histogram
     * @param values the values for each category
     */
    @SuppressWarnings("unchecked")
    public static void histogram( Map<String, ?> options, List<String> series, List<String> categories,
            List<? extends Number> values ) {
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

        List<double[]> data = new ArrayList<>();

        Object object = values.get(0);
        if (object instanceof List) {
            for( int i = 0; i < values.size(); i++ ) {
                List<Number> numList = (List) values.get(i);

                double[] valuesDouble = new double[numList.size()];
                for( int j = 0; j < valuesDouble.length; j++ ) {
                    Number num = (Number) numList.get(j);
                    valuesDouble[j] = num.doubleValue();
                }
                data.add(valuesDouble);
            }
        } else {
            // it has to be number
            double[] valuesDouble = new double[values.size()];
            for( int i = 0; i < valuesDouble.length; i++ ) {
                Number num = (Number) values.get(i);
                valuesDouble[i] = num.doubleValue();
            }
            data.add(valuesDouble);
        }

        CategoryHistogram categoryHistogram = new CategoryHistogram(title, series,
                categories.toArray(new String[categories.size()]), data);
        categoryHistogram.setXLabel(xLabel);
        categoryHistogram.setYLabel(yLabel);
        ChartPanel chartPanel = new ChartPanel(categoryHistogram.getChart(), true);
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);

    }

    /**
     * Creates a histogram chart from the given options and values.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param values a list of lists of numbers, where each inner list contains pairs of [category, value]
     */
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
        CategoryHistogram categoryHistogram = new CategoryHistogram(title, Arrays.asList(""), categories,
                Arrays.asList(valuesDouble));
        categoryHistogram.setXLabel(xLabel);
        categoryHistogram.setYLabel(yLabel);
        ChartPanel chartPanel = new ChartPanel(categoryHistogram.getChart(), true);
        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);

    }

    /**
     * Plots a list of geoscript geometries.
     * 
     * @param geomsList a list of geoscript geometries to plot
     */
    public static void plotGeometries( List<geoscript.geom.Geometry> geomsList ) {
        plotJtsGeometries(null, geomsList.stream().map(gg -> gg.getG()).collect(Collectors.toList()));
    }
    /**
     * Plots a list of geoscript geometries with the given options.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param geomsList a list of geoscript geometries to plot
     */
    public static void plotGeometries( Map<String, Object> options, List<geoscript.geom.Geometry> geomsList ) {
        plotJtsGeometries(options, geomsList.stream().map(gg -> gg.getG()).collect(Collectors.toList()));
    }

    /**
     * Plots a list of JTS geometries.
     * 
     * @param geomsList a list of JTS geometries to plot
     */
    public static void plotJtsGeometries(List<Geometry> geomsList ) {
		plotJtsGeometries(null, geomsList);
    }
    
    /**
     * Plots a list of JTS geometries with the given options.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param geomsList a list of JTS geometries to plot
     */
    public static void plotJtsGeometries( Map<String, ?> options, List<Geometry> geomsList ) {
        JFreeChart chart = makeJtsGeometriesChart(options, geomsList);
        ChartPanel chartPanel = new ChartPanel(chart, true);
        chartPanel.setRangeZoomable(true);
        chartPanel.setDomainZoomable(true);

        int width = 1200;
        int height = 1200;

        if (options != null) {
            Object object = options.get("size");
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
        }

        Dimension preferredSize = new Dimension(width, height);
        chartPanel.setPreferredSize(preferredSize);

        GuiUtilities.openDialogWithPanel(chartPanel, "Simple Geometry Plot", preferredSize, false);
    }

    /**
     * Plots a list of SimpleFeature geometries.
     * 
     * @param featuresList a list of SimpleFeature geometries to plot
     */
    public static void plotFeaturesGeometries(List<SimpleFeature> featuresList) {
        plotFeaturesGeometries(null, featuresList);
    }

    /**
     * Plots a list of SimpleFeature geometries with the given options.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param featuresList a list of SimpleFeature geometries to plot
     */
    public static void plotFeaturesGeometries(Map<String, ?> options, List<SimpleFeature> featuresList) {
        List<Geometry> geomsList = featuresList.stream().map(f -> (Geometry) f.getDefaultGeometry()).collect(Collectors.toList());
        plotJtsGeometries(options, geomsList);
    }

    /**
     * Creates a chart for a list of geoscript geometries.
     * 
     * @param geomsList a list of geoscript geometries to plot
     * @return a JFreeChart representing the geometries
     */
    public static JFreeChart makeGeometriesChart( List<geoscript.geom.Geometry> geomsList ) {
        return makeJtsGeometriesChart(null, geomsList.stream().map(gg -> gg.getG()).collect(Collectors.toList()));
    }

    /**
     * Creates a chart for a list of geoscript geometries with the given options.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param geomsList a list of geoscript geometries to plot
     * @return a JFreeChart representing the geometries
     */
    public static JFreeChart makeGeometriesChart( Map<String, Object> options, List<geoscript.geom.Geometry> geomsList ) {
        return makeJtsGeometriesChart(options, geomsList.stream().map(gg -> gg.getG()).collect(Collectors.toList()));
    }
    /**
     * Creates a chart for a list of JTS geometries with the given options.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, width, and height
     * @param geomsList a list of JTS geometries to plot
     * @return a JFreeChart representing the geometries
     */
    public static JFreeChart makeJtsGeometriesChart( Map<String, ?> options, List<Geometry> geomsList ) {
        String title = "";
        String xLabel = "x";
        String yLabel = "y";

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

        renderer.setSeriesPaint(0, ColorUtilities.makeTransparent(Color.red, 0));

        return chart;
    }

    
    /**
     * Creates a scatter plot with the given data.
     * 
     * @param data a list of series, each containing a list of points, each point being a list of numbers [x, y]
     */
    public static void scatterPlot( List<? extends List<? extends List<? extends Number>>> data) {
        scatterPlot(null, data);
    }

    /**
     * Creates a scatter plot with the given options and data.
     * 
     * @param options a map of chart options, including title, xlabel, ylabel, series, colors, dolines, doshapes, width, and height
     * @param data a list of series, each containing a list of points, each point being a list of numbers [x, y]
     */
    @SuppressWarnings("unchecked")
    public static void scatterPlot(Map<String,?> options, List<? extends List<? extends List<? extends Number>>> data) {
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
            } else if (object instanceof String) {
				colors = Arrays.asList((String) object);
			}
            object = options.get("dolines");
            if (object instanceof List) {
                doLines = (List) object;
            } else if (object instanceof Boolean) {
				doLines = Arrays.asList((Boolean) object);
            }
            object = options.get("doshapes");
            if (object instanceof List) {
                doShapes = (List) object;
            } else if (object instanceof Boolean) {
				doShapes = Arrays.asList((Boolean) object);
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
        for( List<? extends List<? extends Number>> seriesData : data ) {
            String name = "data " + (index + 1);
            if (series != null)
                name = series.get(index);
            double[] x = new double[seriesData.size()];
            double[] y = new double[seriesData.size()];
            for( int i = 0; i < seriesData.size(); i++ ) {
                List<? extends Number> pair = seriesData.get(i);
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

    /**
     * Performs a logarithmic regression on the given data.
     * 
     * @param data a list of points, each point being a list of numbers [x, y]
     * @param result a list to store the resulting regression points
     * @return the regression line representing the logarithmic fit
     */
    public static RegressionLine logRegression( List<List<Double>> data, List<List<Double>> result ) {
        RegressionLine t = new LogTrendLine();
        return processregression(data, result, t);
    }

    /**
     * Performs a polynomial regression on the given data.
     * 
     * @param degree the degree of the polynomial
     * @param data a list of points, each point being a list of numbers [x, y]
     * @param result a list to store the resulting regression points
     * @return the regression line representing the polynomial fit
     */
    public static RegressionLine polynomialRegression( Number degree, List<List<Double>> data, List<List<Double>> result ) {
        RegressionLine t = new PolyTrendLine(degree.intValue());
        return processregression(data, result, t);
    }

    /**
     * Processes the regression for the given data using the specified regression function.
     * 
     * @param data a list of points, each point being a list of numbers [x, y]
     * @param result a list to store the resulting regression points
     * @param function the regression function to use
     * @return the regression line after processing
     */
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

    /**
     * Connects to a PostGIS database with the given connection parameters.
     * 
     * @param host the database host
     * @param port the database port
     * @param database the database name
     * @param user the username for authentication
     * @param pwd the password for authentication
     * @return the connected PostGIS spatial database
     * @throws Exception if the connection fails
     */
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

    /**
     * Connects to a Spatialite database with the given database path.
     * 
     * @param databasePath the path to the Spatialite database file
     * @return the connected Spatialite spatial database
     * @throws Exception if the connection fails
     */
    public static ASpatialDb connectSpatialite( String databasePath ) throws Exception {
        SpatialiteThreadsafeDb spatialDb = (SpatialiteThreadsafeDb) EDb.SPATIALITE.getSpatialDb();
        spatialDb.setMakePooled(false);
        if (!spatialDb.open(databasePath)) {
            spatialDb.initSpatialMetadata(null);
        }
        return spatialDb;
    }

    /**
     * Connects to a GeoPackage database with the given database path.
     * 
     * @param databasePath the path to the GeoPackage database file
     * @return the connected GeoPackage spatial database
     * @throws Exception if the connection fails
     */
    public static GeopackageCommonDb connectGeopackage( String databasePath ) throws Exception {
        GeopackageCommonDb spatialDb = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb();
        spatialDb.setMakePooled(false);
        spatialDb.open(databasePath);
        return spatialDb;
    }

    /**
     * Connects to a SQLite database with the given database path.
     * 
     * @param databasePath the path to the SQLite database file
     * @return the connected SQLite database
     * @throws Exception if the connection fails
     */
    public static SqliteDb connectSqlite( String databasePath ) throws Exception {
        SqliteDb spatialDb = (SqliteDb) EDb.SQLITE.getDb();
        spatialDb.setMakePooled(false);
        spatialDb.open(databasePath);
        return spatialDb;
    }

    /**
     * Connects to an H2GIS database with the given connection parameters.
     * 
     * @param databasePath the path to the H2GIS database file
     * @param user the username for authentication
     * @param pwd the password for authentication
     * @return the connected H2GIS spatial database
     * @throws Exception if the connection fails
     */
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

    /**
     * Executes a query on the given database and returns the result as a list of maps.
     * 
     * @param db the database to query
     * @param sql the SQL query to execute
     * @return a list of maps representing the query result, where each map corresponds to a row
     * @throws Exception if the query fails
     */
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

    /**
     * Executes an SQL statement (insert, update, or delete) on the given database.
     * 
     * @param db the database to execute the statement on
     * @param sql the SQL statement to execute
     * @return the number of affected rows
     * @throws Exception if the execution fails
     */
    public static int execute( ADb db, String sql ) throws Exception {
        return db.executeInsertUpdateDeleteSql(sql);
    }

    /**
     * Executes a batch of SQL statements on the given database.
     * 
     * @param db the database to execute the statements on
     * @param sqlList the list of SQL statements to execute
     * @param updateMessageConsumer a consumer to receive update messages (can be null)
     * @throws Exception if the execution fails
     */
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
                            updateMessageConsumer.accept("Executed: " + count);
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

    /**
     * Prints the available color tables.
     * 
     * @return a comma-separated string of color table names
     */
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

    /**
     * Calculate the orthodromic distance between two geographic coordinates specified by longitude and latitude.
     * 
     * @param lon1 the longitude of the first coordinate.
     * @param lat1 the latitude of the first coordinate.
     * @param lon2 the longitude of the second coordinate.
     * @param lat2 the latitude of the second coordinate.
     * @return the distance in meters.
     */
    public static double distanceLL( double lon1, double lat1, double lon2, double lat2 ) {
        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        gc.setStartingGeographicPoint(lon1, lat1);
        gc.setDestinationGeographicPoint(lon2, lat2);
        double distance = gc.getOrthodromicDistance();
        return distance;
    }

    /**
     * Creates a style for a color table with the specified parameters.
     * 
     * @param tableName the name of the color table.
     * @param min the minimum value for the style.
     * @param max the maximum value for the style.
     * @param opacity the opacity of the style.
     * @return the created style.
     * @throws Exception if the style creation fails.
     */
    public static Style styleForColorTable( String tableName, double min, double max, double opacity ) throws Exception {
        return new SLDReader().read(
                RasterStyleUtilities.styleToString(RasterStyleUtilities.createStyleForColortable(tableName, min, max, opacity)));
    }
    
    /**
     * Creates a GeoTools style for a color table with the specified parameters.
     * 
     * @param tableName the name of the color table.
     * @param min the minimum value for the style.
     * @param max the maximum value for the style.
     * @param opacity the opacity of the style.
     * @return the created GeoTools style.
     * @throws Exception if the style creation fails.
     */
    public static org.geotools.api.style.Style styleForColorTableGT( String tableName, double min, double max, double opacity ) throws Exception {
    	var s = styleForColorTable(tableName, min, max, opacity);
    	return s.getGtStyle();
    }

    /**
     * Creates an SLD style file for a raster with the specified color table and opacity.
     * 
     * @param tableName the name of the color table.
     * @param rasterPath the path to the raster file.
     * @throws Exception if the style creation fails.
     */
    public static void makeSldStyleForRaster( String tableName, String rasterPath ) throws Exception {
        makeSldStyleForRaster(tableName, rasterPath, 1);
    }

    /**
     * Creates an SLD style file for a raster with the specified color table and opacity.
     * 
     * @param tableName the name of the color table.
     * @param rasterPath the path to the raster file.
     * @param opacity the opacity of the style.
     * @throws Exception if the style creation fails.
     */
    public static void makeSldStyleForRaster( String tableName, String rasterPath, double opacity ) throws Exception {
    	HMRaster raster = HMRaster.fromFile(new File(rasterPath));
    	double[] minMax = raster.getStatistics();
    	if (minMax == null) {
    		return;
    	}
        String style = RasterStyleUtilities
                .styleToString(RasterStyleUtilities.createStyleForColortable(tableName, minMax[0], minMax[1], opacity));
        File styleFile = FileUtilities.substituteExtention(new File(rasterPath), "sld");
        FileUtilities.writeFile(style, styleFile);
    }

    /**
     * Creates a QGIS style file for a raster with the specified color table and label decimals.
     * 
     * @param tableName the name of the color table.
     * @param rasterPath the path to the raster file.
     * @throws Exception if the style creation fails.
     */
    public static void makeQgisStyleForRaster( String tableName, String rasterPath ) throws Exception {
        makeQgisStyleForRaster(tableName, rasterPath, 2);
    }

    /**
     * Creates a QGIS style file for a raster with the specified color table and label decimals.
     * 
     * @param tableName the name of the color table.
     * @param rasterPath the path to the raster file.
     * @param labelDecimals the number of decimal places for the labels.
     * @throws Exception if the style creation fails.
     */
    public static void makeQgisStyleForRaster( String tableName, String rasterPath, int labelDecimals ) throws Exception {
        HMRaster raster = HMRaster.fromFile(new File(rasterPath));
		double[] minMax = raster.getStatistics();
        double min = minMax[0];
        double max = minMax[1];

        String style = RasterStyleUtilities.createQGISRasterStyle(tableName, min, max, null, labelDecimals);
        File styleFile = FileUtilities.substituteExtention(new File(rasterPath), "qml");
        FileUtilities.writeFile(style, styleFile);
    }

    /**
     * Converts a timestamp in milliseconds to a formatted string.
     * 
     * @param millis the timestamp in milliseconds.
     * @return the formatted string representation of the timestamp.
     */
    public static String ts2str( long millis ) {
        return new DateTime(millis).toString(HMConstants.utcDateFormatterYYYYMMDDHHMMSS);
    }

    /**
     * Converts a formatted string representation of a timestamp to milliseconds.
     * 
     * @param isoString the formatted string representation of the timestamp.
     * @return the timestamp in milliseconds.
     */
    public static long str2ts( String isoString ) {
        return HMConstants.utcDateFormatterYYYYMMDDHHMMSS.parseDateTime(isoString).getMillis();
    }

    /**
     * Shows an input dialog with a message and a default input value.
     * 
     * @param message the message to display.
     * @param defaultInput the default input value.
     * @return the user's input.
     */
    public static String showInputDialog( String message, String defaultInput ) {
        String answer = GuiUtilities.showInputDialog(null, message, defaultInput);
        return answer;
    }
    
    /**
     * Shows a yes/no dialog with a message.
     * 
     * @param message the message to display.
     * @return true if the user selects "Yes", false otherwise.
     */
    public static boolean showYesNoDialog( String message ) {
        return GuiUtilities.showYesNoDialog(null, message);
    }
    
    /**
     * Shows a combo dialog with a title, message, and selectable values.
     * 
     * @param title the title of the dialog.
     * @param message the message to display.
     * @param values the array of selectable values.
     * @param selectedValue the initially selected value.
     * @return the value selected by the user.
     */
    public static String showComboDialog( String title, String message, String[] values, String selectedValue ) {
        String result = GuiUtilities.showComboDialog(null, message, title, values, selectedValue);
        return result;
    }

    /**
     * Shows an information message dialog with the specified message.
     * 
     * @param message the message to display.
     */
    public static void showInfoMessage( String message ) {
        GuiUtilities.showInfoMessage(null, null, message);
    }

    /**
     * Shows a warning message dialog with the specified message.
     * 
     * @param message the message to display.
     */
    public static void showWarningMessage( String message ) {
        GuiUtilities.showWarningMessage(null, null, message);
    }

    /**
     * Shows an error message dialog with the specified message.
     * 
     * @param message the message to display.
     */
    public static void showErrorMessage( String message ) {
        GuiUtilities.showErrorMessage(null, null, message);
    }

    /**
     * Adds a projection to the specified folder with the given EPSG code.
     * 
     * @param folder the folder to add the projection to.
     * @param epsg the EPSG code of the projection.
     * @throws Exception if an error occurs while adding the projection.
     */
    public static void addPrjs( String folder, int epsg ) throws Exception {
        FileIterator.addPrj(folder, "EPSG:" + epsg);
    }

    /**
     * Creates a color interpolator with the specified color table, minimum and maximum values, and optional alpha.
     * 
     * @param colortable the color table to use.
     * @param min the minimum value.
     * @param max the maximum value.
     * @param alpha the optional alpha value.
     * @return the created color interpolator.
     */
    public static ColorInterpolator getColorInterpolator( String colortable, double min, double max, Integer alpha ) {
        ColorInterpolator ci = new ColorInterpolator(colortable, min, max, alpha);
        return ci;
    }

    /**
     * Retrieves cell information for the specified column, row, buffer, and raster paths.
     * 
     * @param col the column index.
     * @param row the row index.
     * @param buffer the buffer size.
     * @param rasterPaths the paths to the raster files.
     * @return the retrieved raster cell information.
     * @throws Exception if an error occurs while reading the rasters.
     */
    public static RasterCellInfo getCellInfo( int col, int row, int buffer, String... rasterPaths ) throws Exception {
        GridCoverage2D[] rasters = new GridCoverage2D[rasterPaths.length];
        int i = 0;
        for( String path : rasterPaths ) {
            rasters[i++] = readRaster(path);
        }
        RasterCellInfo ri = new RasterCellInfo(col, row, rasters);
        ri.setBufferCells(buffer);
        return ri;
    }

    /**
     * Retrieves cell information for the specified longitude, latitude, buffer, and raster paths.
     * 
     * @param lon the longitude coordinate.
     * @param lat the latitude coordinate.
     * @param buffer the buffer size.
     * @param rasterPaths the paths to the raster files.
     * @return the retrieved raster cell information.
     * @throws Exception if an error occurs while reading the rasters.
     */
    public static RasterCellInfo getCellInfo( double lon, double lat, int buffer, String... rasterPaths ) throws Exception {
        GridCoverage2D[] rasters = new GridCoverage2D[rasterPaths.length];
        int i = 0;
        for( String path : rasterPaths ) {
            rasters[i++] = readRaster(path);
        }
        RasterCellInfo ri = new RasterCellInfo(lon, lat, rasters);
        ri.setBufferCells(buffer);
        return ri;
    }

    // ANYTHING THAT CAN BE CONVERTED TO IMAGE
    /**
     * Converts the specified file or geometry WKT to a BufferedImage with default width and height.
     * 
     * @param fileOrGeometryWkt the file path or geometry WKT to convert.
     * @return the resulting BufferedImage.
     */
    public static BufferedImage toImage( String fileOrGeometryWkt ) {
        return toImage(fileOrGeometryWkt, 1000, 800);
    }
    
    /**
     * Converts the specified GridCoverage2D map to a BufferedImage.
     * 
     * @param map the GridCoverage2D map to convert.
     * @return the resulting BufferedImage.
     */
    public static BufferedImage toImage(GridCoverage2D map) {
        // StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

        // // HM.styleForColorTable(getRegisteredRasterFormats(), 0, 0, 0);
        // // RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        // // Style rasterStyle = SLD.wrapSymbolizers(sym);

        // StyleBuilder sB = new StyleBuilder(sf);
        // RasterSymbolizer rasterSym = sf.createRasterSymbolizer();

        // ColorMap colorMap = sf.createColorMap();

        // RenderedImage renderedImage = map.getRenderedImage();

        GridEnvelope2D gridRange = map.getGridGeometry().getGridRange2D();
        int width = gridRange.width;
        int height = gridRange.height;

        // Create an empty BufferedImage
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Create an ImageLayout
        ImageLayout layout = new ImageLayout();
        layout.setMinX(gridRange.x);
        layout.setMinY(gridRange.y);
        layout.setWidth(width);
        layout.setHeight(height);

        // Render the GridCoverage2D to the BufferedImage
        map.getRenderedImage().copyData(bufferedImage.getRaster());
        

        return bufferedImage;
    }

    public static BufferedImage toImage( SimpleFeatureCollection featureCollection ) {
        try {
            List<Geometry> geometries = new ArrayList<>();
            try (SimpleFeatureIterator it = featureCollection.features()) {
                while (it.hasNext()) {
                    SimpleFeature feature = it.next();
                    Object geom = feature.getDefaultGeometry();
                    if (geom instanceof Geometry) {
                        geometries.add((Geometry) geom);
                    }
                }
            }
            JFreeChart chart = makeJtsGeometriesChart(null, geometries);
            return chart.createBufferedImage(1000, 800);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    
    /**
     * Converts the specified file or geometry WKT to a BufferedImage with the given width and height.
     * 
     * @param fileOrGeometryWkt the file path or geometry WKT to convert.
     * @param width the width of the resulting image.
     * @param height the height of the resulting image.
     * @return the resulting BufferedImage.
     */
    public static BufferedImage toImage( String fileOrGeometryWkt, int width, int height ) {
        try {
            File file = new File(fileOrGeometryWkt);
            if (file.exists()) {
                if(file.getName().toLowerCase().endsWith(".tif")){
                    makeSldStyleForRaster("rainbow", file.getAbsolutePath());
                    GridCoverage2D raster = readRaster( file.getAbsolutePath());
                    RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
                    Envelope envelope = region.toEnvelope();
                    ReferencedEnvelope refEnv = new ReferencedEnvelope(envelope, raster.getCoordinateReferenceSystem());    

                    final ImageGenerator imgGen = new ImageGenerator(null, raster.getCoordinateReferenceSystem());
                    imgGen.addCoveragePath(file.getAbsolutePath());
                    imgGen.setLayers();
                    BufferedImage imageWithCheck = imgGen.getImageWithCheck(refEnv, width, height, 0, null);
                    return imageWithCheck;
                }else{
                	return ImageUtilities.scaleImage(ImageIO.read(file), height);
                }
            } else {
                // try with wkt geometry
                Geometry geometry = new WKTReader().read(fileOrGeometryWkt);
                JFreeChart chart = makeJtsGeometriesChart(null, Arrays.asList(geometry));
                return chart.createBufferedImage(width, height);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts the specified list of geometries to a BufferedImage.
     * 
     * @param geomsList the list of geometries to convert.
     * @return the resulting BufferedImage.
     */
    @SuppressWarnings("unchecked")
    public static BufferedImage toImage( List< ? > geomsList ) {
        Object object = geomsList.get(0);
        if (object instanceof geoscript.geom.Geometry) {
            JFreeChart chart = makeJtsGeometriesChart(null,
                    ((List<geoscript.geom.Geometry>) geomsList).stream().map(gg -> gg.getG()).collect(Collectors.toList()));
            return chart.createBufferedImage(600, 400);
        } else if (object instanceof Geometry) {
            JFreeChart chart = makeJtsGeometriesChart(null, ((List<Geometry>) geomsList));
            return chart.createBufferedImage(600, 400);
        } else if (object instanceof String) {
            // suppose wkt
            JFreeChart chart = makeJtsGeometriesChart(null, ((List<String>) geomsList).stream().map(wkt -> {
                try {
                    return new WKTReader().read(wkt);
                } catch (ParseException e) {
                    throw new ModelsIllegalargumentException("Unable to interpret objects as wkt geometries.", "HM");
                }
            }).collect(Collectors.toList()));
            return chart.createBufferedImage(600, 400);
        } else {
            throw new ModelsIllegalargumentException("Unable to interpret list of objects.", "HM");
        }
    }

    /**
     * Converts the specified list of geoscript geometries to a BufferedImage with the given options.
     * 
     * @param options the options for the image conversion, such as size, width, and height.
     * @param geomsList the list of geoscript geometries to convert.
     * @return the resulting BufferedImage.
     */
    public static BufferedImage toImage( Map<String, Object> options, List<geoscript.geom.Geometry> geomsList ) {
        int width = 600;
        int height = 400;

        if (options != null) {
            Object object = options.get("size");
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
        }
        return makeJtsGeometriesChart(options, geomsList.stream().map(gg -> gg.getG()).collect(Collectors.toList()))
                .createBufferedImage(width, height);
    }

    /**
     * Converts the specified cell coordinates and raster information to a BufferedImage with the given parameters.
     * 
     * @param cellX the X coordinate of the cell.
     * @param cellY the Y coordinate of the cell.
     * @param bufferCells the number of buffer cells around the specified cell.
     * @param mapPath the path to the raster map.
     * @param dtm the digital terrain model (DTM) path.
     * @param width the width of the resulting image.
     * @param height the height of the resulting image.
     * @return the resulting BufferedImage.
     * @throws Exception if an error occurs while reading the raster or creating the image.
     */
    public static BufferedImage toImage( int cellX, int cellY, int bufferCells, String mapPath, String dtm, int width,
            int height ) throws Exception {
        GridCoverage2D map = OmsRasterReader.readRaster(mapPath);
        return createRasterImageWithInfoAroundCell(mapPath, map, cellX, cellY, bufferCells, width, height, dtm);
    }

    /**
     * Converts the specified cell coordinates and raster information to a BufferedImage with the given parameters, without a DTM.
     * 
     * @param cellX the X coordinate of the cell.
     * @param cellY the Y coordinate of the cell.
     * @param bufferCells the number of buffer cells around the specified cell.
     * @param mapPath the path to the raster map.
     * @param width the width of the resulting image.
     * @param height the height of the resulting image.
     * @return the resulting BufferedImage.
     * @throws Exception if an error occurs while reading the raster or creating the image.
     */
    public static BufferedImage toImage( int cellX, int cellY, int bufferCells, String mapPath, int width, int height )
            throws Exception {
        return toImage(cellX, cellY, bufferCells, mapPath, null, width, height);
    }

    /**
     * Converts the specified world coordinates and raster information to a BufferedImage with the given parameters.
     * 
     * @param worldX the X coordinate in the world.
     * @param worldY the Y coordinate in the world.
     * @param bufferCells the number of buffer cells around the specified cell.
     * @param mapPath the path to the raster map.
     * @param dtm the digital terrain model (DTM) path.
     * @param width the width of the resulting image.
     * @param height the height of the resulting image.
     * @return the resulting BufferedImage.
     * @throws Exception if an error occurs while reading the raster or creating the image.
     */
    public static BufferedImage toImage( double worldX, double worldY, int bufferCells, String mapPath, String dtm, int width,
            int height ) throws Exception {
        GridCoverage2D map = OmsRasterReader.readRaster(mapPath);
        int[] colRow = CoverageUtilities.colRowFromCoordinate(new Coordinate(worldX, worldY), map.getGridGeometry(), null);
        return createRasterImageWithInfoAroundCell(mapPath, map, colRow[0], colRow[1], bufferCells, width, height, dtm);
    }

    /**
     * Converts the specified world coordinates and raster information to a BufferedImage with the given parameters, without a DTM.
     * 
     * @param worldX the X coordinate in the world.
     * @param worldY the Y coordinate in the world.
     * @param bufferCells the number of buffer cells around the specified cell.
     * @param mapPath the path to the raster map.
     * @param width the width of the resulting image.
     * @param height the height of the resulting image.
     * @return the resulting BufferedImage.
     * @throws Exception if an error occurs while reading the raster or creating the image.
     */
    public static BufferedImage toImage( double worldX, double worldY, int bufferCells, String mapPath, int width, int height )
            throws Exception {
        return toImage(worldX, worldY, bufferCells, mapPath, null, width, height);
    }

    
    private static BufferedImage createRasterImageWithInfoAroundCell( String mapPath, GridCoverage2D map, int col, int row,
            int bufferCells, int width, int height, String dtm ) throws Exception {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(map);
        GridGeometry2D gg = map.getGridGeometry();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();
        Coordinate centerCoordinate = CoverageUtilities.coordinateFromColRow(col, row, gg);
        double west = centerCoordinate.x - bufferCells * xres - xres / 2.0;
        double east = centerCoordinate.x + bufferCells * xres + xres / 2.0;
        double south = centerCoordinate.y - bufferCells * yres - yres / 2.0;
        double north = centerCoordinate.y + bufferCells * yres + yres / 2.0;

        File styleFile = FileUtilities.substituteExtention(new File(mapPath), "sld");
        org.geotools.api.style.Style style;
        if (styleFile.exists()) {
            style = SldUtilities.getStyleFromFile(styleFile);
        } else {
            style = SldUtilities.getStyleFromRasterFile(styleFile);
        }
        GridCoverageLayer layer = new GridCoverageLayer(map, style);

        ReferencedEnvelope envelope = new ReferencedEnvelope(west, east, south, north, map.getCoordinateReferenceSystem());

        RasterInfoLayer infoLayer = new RasterInfoLayer();
        infoLayer.setRasterLayer(layer);

        List<Layer> layers = Arrays.asList(layer);
        BufferedImage image = drawLayers(layers, envelope, width, height, 0);

        AffineTransform worldToScreen = TransformationUtils.getWorldToPixel(envelope,
                new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        drawInfo(g2d, worldToScreen, envelope, map, dtm);
        g2d.dispose();

        return image;
    }

    private static void drawInfo( Graphics2D g2d, AffineTransform worldToScreen, ReferencedEnvelope envelope,
            GridCoverage2D raster, String dtm ) throws Exception {
        try {
            String DEFAULT_NUMFORMAT = "0.#";

            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
            GridGeometry2D gg = raster.getGridGeometry();
            double xres = regionMap.getXres();
            double yres = regionMap.getYres();

            double w = envelope.getMinX();
            double s = envelope.getMinY();
            double e = envelope.getMaxX();
            double n = envelope.getMaxY();

            double halfX = regionMap.getXres() / 2;
            double halfY = regionMap.getYres() / 2;
            int cols = regionMap.getCols();
            int rows = regionMap.getRows();

            GridCoordinates2D llPix = gg.worldToGrid(new Position2D(w, s));
            GridCoordinates2D urPix = gg.worldToGrid(new Position2D(e, n));

            int fromC = (int) Math.floor(llPix.getX());
            int toC = (int) Math.ceil(urPix.getX()) + 1;
            int fromR = (int) Math.floor(urPix.getY());
            int toR = (int) Math.ceil(llPix.getY()) + 1;

            boolean doDtmInfo = false;
            RandomIter dtmIter = null;
            GridCoverage2D dtmGc;
            if (dtm != null) {
                doDtmInfo = true;
                dtmGc = OmsRasterReader.readRaster(dtm);
                dtmIter = CoverageUtilities.getRandomIterator(dtmGc);
            }

            int[] centerColRow = CoverageUtilities.colRowFromCoordinate(envelope.centre(), gg, null);

            DecimalFormat f = new DecimalFormat(DEFAULT_NUMFORMAT);

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            for( int r = fromR; r < toR; r++ ) {
                if (r < 0 || r >= rows)
                    continue;
                for( int c = fromC; c < toC; c++ ) {
                    if (c < 0 || c >= cols)
                        continue;

                    boolean isCenter = centerColRow[0] == c && centerColRow[1] == r;

                    Coordinate cellCenterCoord = CoverageUtilities.coordinateFromColRow(c, r, gg);
                    Point2D ll = worldToScreen.transform(new Point2D.Double(cellCenterCoord.x - halfX, cellCenterCoord.y - halfY),
                            null);
                    Point2D ul = worldToScreen.transform(new Point2D.Double(cellCenterCoord.x - halfX, cellCenterCoord.y + halfY),
                            null);
                    Point2D ur = worldToScreen.transform(new Point2D.Double(cellCenterCoord.x + halfX, cellCenterCoord.y + halfY),
                            null);
                    Point2D lr = worldToScreen.transform(new Point2D.Double(cellCenterCoord.x + halfX, cellCenterCoord.y - halfY),
                            null);

                    GeneralPath path = new GeneralPath();
                    path.moveTo(ll.getX(), ll.getY());
                    path.lineTo(ul.getX(), ul.getY());
                    path.lineTo(ur.getX(), ur.getY());
                    path.lineTo(lr.getX(), lr.getY());

                    double value = CoverageUtilities.getValue(raster, c, r);
                    String valueFormatted = f.format(value);
                    if (isCenter) {
                        g2d.setColor(Color.red);
                        g2d.setStroke(new BasicStroke(3));
                    } else if (HMConstants.isNovalue(value)) {
                        g2d.setColor(Color.gray);
                        g2d.setStroke(new BasicStroke(1));
                        valueFormatted = "nv";
                    } else {
                        g2d.setColor(Color.black);
                        g2d.setStroke(new BasicStroke(1));
                    }
                    g2d.draw(path);

                    // draw dtm info
                    if (doDtmInfo && !HMConstants.isNovalue(value)) {
                        GridNode node = new GridNode(dtmIter, cols, rows, xres, yres, c, r, null);
                        int dot = 8;
                        Point2D pCenter = worldToScreen.transform(new Point2D.Double(cellCenterCoord.x, cellCenterCoord.y), null);
                        if (node.isPit()) {
                            g2d.setColor(Color.red);
                            g2d.fillOval((int) pCenter.getX() - dot / 2, (int) pCenter.getY() - dot / 2, dot, dot);
                        } else {
                            g2d.setColor(Color.black);
                            g2d.setStroke(new BasicStroke(3));
                            int d = (int) ((ur.getX() - ll.getX()) * 0.2);
                            int flow = node.getFlow();
                            if (!HMConstants.isNovalue(flow)) {
                                GeneralPath fPath = null;
                                Direction dir = Direction.forFlow(flow);
                                switch( dir ) {
                                case E:
                                    g2d.fillOval((int) ll.getX() + d - dot / 2, (int) pCenter.getY() - dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(ll.getX() + d, pCenter.getY());
                                    fPath.lineTo(ur.getX() - d, pCenter.getY());
                                    g2d.draw(fPath);
                                    break;
                                case W:
                                    g2d.fillOval((int) ur.getX() - d - dot / 2, (int) pCenter.getY() - dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(ur.getX() - d, pCenter.getY());
                                    fPath.lineTo(ll.getX() + d, pCenter.getY());
                                    g2d.draw(fPath);
                                    break;
                                case S:
                                    g2d.fillOval((int) pCenter.getX() - dot / 2, (int) ur.getY() + d + dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(pCenter.getX(), ur.getY() + d);
                                    fPath.lineTo(pCenter.getX(), ll.getY() - d);
                                    g2d.draw(fPath);
                                    break;
                                case N:
                                    g2d.fillOval((int) pCenter.getX() - dot / 2, (int) ll.getY() - d - dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(pCenter.getX(), ll.getY() - d);
                                    fPath.lineTo(pCenter.getX(), ur.getY() + d);
                                    g2d.draw(fPath);
                                    break;
                                case SE:
                                    g2d.fillOval((int) ul.getX() + d - dot / 2, (int) ul.getY() + d - dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(ul.getX() + d, ul.getY() + d);
                                    fPath.lineTo(lr.getX() - d, lr.getY() - d);
                                    g2d.draw(fPath);
                                    break;
                                case WS:
                                    g2d.fillOval((int) ur.getX() - d - dot / 2, (int) ur.getY() + d - dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(ur.getX() - d, ur.getY() + d);
                                    fPath.lineTo(ll.getX() + d, ll.getY() - d);
                                    g2d.draw(fPath);
                                    break;
                                case NW:
                                    g2d.fillOval((int) lr.getX() - d - dot / 2, (int) lr.getY() - d - dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(lr.getX() - d, lr.getY() - d);
                                    fPath.lineTo(ul.getX() + d, ul.getY() + d);
                                    g2d.draw(fPath);
                                    break;
                                case EN:
                                    g2d.fillOval((int) ll.getX() + d - dot / 2, (int) ll.getY() - d - dot / 2, dot, dot);
                                    fPath = new GeneralPath();
                                    fPath.moveTo(ll.getX() + d, ll.getY() - d);
                                    fPath.lineTo(ur.getX() - d, ur.getY() + d);
                                    g2d.draw(fPath);
                                    break;

                                default:
                                    break;
                                }
                            }
                        }
                    }

                    // draw string info
                    int cellW = (int) (ur.getX() - ul.getX());
                    int cellH = (int) (ll.getY() - ul.getY());

                    int fontSize = cellH / 5;
                    if (fontSize > 14) {
                        fontSize = 14;
                    }

                    Font newFont = new Font("default", Font.BOLD, fontSize);
                    g2d.setFont(newFont);
                    g2d.setColor(Color.BLACK);
                    FontMetrics fontMetrics = g2d.getFontMetrics();

                    String text1 = "col:" + c;
                    String text2 = "row:" + r;
                    String text3 = "value:" + valueFormatted;

                    if (!drawStrings(g2d, ll, ul, cellW, cellH, fontMetrics, text1, text2, text3)) {
                        text1 = "c:" + c;
                        text2 = "r:" + r;
                        text3 = "v:" + valueFormatted;

                        if (!drawStrings(g2d, ll, ul, cellW, cellH, fontMetrics, text1, text2, text3)) {
                            text1 = "" + c;
                            text2 = "" + r;
                            text3 = "" + valueFormatted;
                            if (!drawStrings(g2d, ll, ul, cellW, cellH, fontMetrics, text1, text2, text3)) {
                                drawStrings(g2d, ll, ul, cellW, cellH, fontMetrics, text1, text2, null);
                            }

                        }
                    }
                }
            }

        } catch (InvalidGridGeometryException | TransformException e1) {
            e1.printStackTrace();
        }
    }

    private static boolean drawStrings( Graphics2D g2d, Point2D p1, Point2D p2, int cellW, int cellH, FontMetrics fontMetrics,
            String text1, String text2, String text3 ) {
        Rectangle2D stringBounds = fontMetrics.getStringBounds(text1, g2d);
        Rectangle2D stringBounds2 = fontMetrics.getStringBounds(text2, g2d);
        if (stringBounds2.getWidth() > stringBounds.getWidth()) {
            stringBounds = stringBounds2;
        }
        if (text3 != null) {
            Rectangle2D stringBounds3 = fontMetrics.getStringBounds(text3, g2d);
            if (stringBounds3.getWidth() > stringBounds.getWidth()) {
                stringBounds = stringBounds3;
            }
        }
        int stringW = (int) stringBounds.getWidth();
        int stringH = (int) stringBounds.getHeight();

        if (stringW <= cellW * 0.8 && stringH * 3 <= cellH) {
            int posX = (int) (p1.getX() + (cellW - stringW) / 2);
            int posY = (int) (p2.getY() + stringH);

            FontRenderContext frc = g2d.getFontRenderContext();
            Font font = g2d.getFont();
            GlyphVector gv = font.createGlyphVector(frc, text1);
            Shape o = gv.getOutline(posX, posY);
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.setColor(Color.white);
            g2d.draw(o);
            g2d.setColor(Color.black);
            g2d.drawString(text1, posX, posY);

            g2d.setColor(Color.white);
            gv = font.createGlyphVector(frc, text2);
            o = gv.getOutline(posX, posY + stringH);
            g2d.draw(o);
            g2d.setColor(Color.black);
            g2d.drawString(text2, posX, posY + stringH);
            if (text3 != null) {
                g2d.setColor(Color.white);
                gv = font.createGlyphVector(frc, text3);
                o = gv.getOutline(posX, posY + 2 * stringH);
                g2d.draw(o);
                g2d.setColor(Color.black);
                g2d.drawString(text3, posX, posY + 2 * stringH);
            }
            return true;
        }
        return false;
    }

    /**
     * Draw the map on an image creating a new MapContent.
     * 
     * @param bounds the area of interest.
     * @param imageWidth the width of the image to produce.
     * @param imageHeight the height of the image to produce.
     * @param buffer the buffer to add around the map bounds in map units. 
     * @return the image.
     */
    private static BufferedImage drawLayers( List<Layer> layers, ReferencedEnvelope ref, int imageWidth, int imageHeight,
            double buffer ) {
        MapContent content = new MapContent();
        content.setTitle("dump");

        content.getViewport().setCoordinateReferenceSystem(ref.getCoordinateReferenceSystem());
        content.getViewport().setBounds(ref);

        for( Layer layer : layers ) {
            content.addLayer(layer);
        }

        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(content);

        if (buffer > 0.0) {
            ref = new ReferencedEnvelope(ref);
            ref.expandBy(buffer, buffer);
        }

        double envW = ref.getWidth();
        double envH = ref.getHeight();

        if (envW < envH) {
            double newEnvW = envH * (double) imageWidth / (double) imageHeight;
            double delta = newEnvW - envW;
            ref.expandBy(delta / 2, 0);
        } else {
            double newEnvH = envW * (double) imageHeight / (double) imageWidth;
            double delta = newEnvH - envH;
            ref.expandBy(0, delta / 2.0);
        }

        Rectangle imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
        BufferedImage dumpImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dumpImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        renderer.paint(g2d, imageBounds, ref);

        return dumpImage;
    }

    /**
     * Creates a Wcs object for the specified URL and version.
     * 
     * @param url the URL of the WCS service.
     * @param version the version of the WCS service.
     * @return the Wcs object.
     * @throws Exception if an error occurs while creating the Wcs object.
     */
    public static Wcs wcs(String url, String version) throws Exception {
        Wcs wcs = new Wcs(url, version);
        return wcs;
    }

    /**
     * Creates a Wcs object for the specified URL with the default version.
     * 
     * @param url the URL of the WCS service.
     * @return the Wcs object.
     * @throws Exception if an error occurs while creating the Wcs object.
     */
    public static Wcs wcs(String url) throws Exception {
        Wcs wcs = new Wcs(url);
        return wcs;
    }
    /**
     * Sends an HTTP GET request to the specified URL and returns the JSON response as a string.
     * 
     * @param url the URL to send the request to.
     * @return the JSON response as a string.
     * @throws Exception if an error occurs while sending the request or receiving the response.
     */
    public static String getJsonResponse(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Hortonmachine/geotools (info@hortonmachine.org)")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
			throw new RuntimeException("Failed to get response ("+response.statusCode()+") from url: " + url + " with error: " + response.body());
		}

        return response.body();
    }

    /**
     * Converts a JSON string to a corresponding Java object (Map or List).
     * 
     * @param json the JSON string to convert.
     * @return the corresponding Java object (Map or List).
     * @throws IllegalArgumentException if the JSON is invalid or not a Map or List.
     */
    public static Object jsonToObject(String json) {
        char first = json.stripLeading().charAt(0);

        return switch (first) {
            case '[' -> new JSONArray(json).toList();
            case '{' -> new JSONObject(json).toMap();
            default -> throw new IllegalArgumentException("Invalid JSON");
        };
    }

    /**
     * Converts a Java object (Map or List) to a JSON string.
     * 
     * @param object the Java object to convert.
     * @return the JSON string representation of the object.
     * @throws IllegalArgumentException if the object is not a Map or List.
     */
    public static String objectToJson(Object object) {
		if (object instanceof Map) {
			return new JSONObject((Map<?, ?>) object).toString();
		} else if (object instanceof List) {
			return new JSONArray((List<?>) object).toString();
		} else {
			throw new IllegalArgumentException("Object must be a Map or List");
		}
	}
    
    /**
     * Retrieves the projection information for the specified EPSG code.
     * 
     * @param epsgCode the EPSG code of the projection.
     * @return the ProjectionInfo object representing the projection.
     * @throws Exception if an error occurs while retrieving the projection information.
     */
    public static ProjectionInfo getProjectionInfo(String epsgCode) throws Exception {
		return new ProjectionInfo(epsgCode);
	}
    
    /**
     * Creates a CRS transformer for converting coordinates between two EPSG codes.
     * 
     * @param fromEpsg the source EPSG code.
     * @param toEpsg the target EPSG code.
     * @return the HMCrsTransformer object for coordinate transformation.
     * @throws Exception if an error occurs while creating the CRS transformer.
     */
    public static HMCrsTransformer getCrsTransformer(String fromEpsg, String toEpsg) throws Exception {
		return new HMCrsTransformer(fromEpsg, toEpsg);
	}

    /**
     * Retrieves the CRS registry instance.
     * 
     * @return the HMCrsRegistry instance.
     */
    public static HMCrsRegistry getCrsRegistry() {
    	return HMCrsRegistry.INSTANCE;
    }

}
