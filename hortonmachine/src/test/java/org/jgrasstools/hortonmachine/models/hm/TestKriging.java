package org.jgrasstools.hortonmachine.models.hm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.media.jai.RasterFactory;

import org.apache.commons.io.IOUtils;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.statistics.kriging.OmsKriging;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Test the kriging model.
 * 
 * @author Daniele Andreis
 * @author Rafael Almeida
 * 
 */
public class TestKriging {

	/**
	 * If enabled, will write test results into GeoTIFF files for manual
	 * inspection and ease of development, beyond automated testing.
	 */
	private final static boolean ENABLE_TEST_GEOTIFF_WRITING = true;

    @Test
    public void testKriging() throws Exception {
    	// Load test case shapefile
    	URL stazioniGridUrl = this.getClass().getClassLoader().getResource("rainstationgrid.shp");
    	File stazioniGridFile = new File(stazioniGridUrl.toURI());
    	
    	// Read the features
        String stationIdField = "ID_PUNTI_M";
        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
        stationsReader.file = stazioniGridFile.getAbsolutePath();
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;

        // Load the time series data for this test case
        URL krigingRain4Url = this.getClass().getClassLoader().getResource("rain_test_grid.csv");
        File krigingRain4File = new File(krigingRain4Url.toURI());
        
        // Setup the time series reader
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = krigingRain4File.getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        reader.fileNovalue = "-9999";
        reader.initProcess();

        // Create the kriging handler
        OmsKriging kriging = new OmsKriging();
        kriging.pm = new DummyProgressMonitor();

        // Defines the grids of points to be interpolated
        kriging.pMode = 1;
        GridGeometry2D gridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(5204514.51713, 5141634.51713,
                686136.82243, 601576.82243, 2114, 1572, HMTestMaps.getCrs());
        kriging.inInterpolationGrid = gridGeometry2D;

        // Set up the station data
        kriging.inStations = stationsFC;
        kriging.fStationsid = stationIdField;
        kriging.fInterpolateid = "netnum";

		// Disable logarithmic mode (which would do the kriging with the log of
		// data)
        kriging.doLogarithmic = false;
        
        // Set up the exponential variogram
        kriging.defaultVariogramMode = 1;
        kriging.pSemivariogramType = 1;
        kriging.pA = 123537.0;
        kriging.pNug = 0.0;
        kriging.pS = 1.678383;

        // Run the kriging for the time steps
        while( reader.doProcess ) {
        	// Run this time step
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            kriging.inData = id2ValueMap;
            kriging.process();
            
            // Get expected result
            double[] values = id2ValueMap.get(1331);
            Filter filter = CQL.toFilter(stationIdField + " = 1331");
            SimpleFeatureCollection subCollection = stationsFC.subCollection(filter);
            assertTrue(subCollection.size() == 1);

            // Get result station coordinates
            SimpleFeature station = subCollection.features().next();
            Geometry geometry = (Geometry) station.getDefaultGeometry();
            Coordinate stationCoordinate = geometry.getCoordinate();

            // Extract and check calculated result
            GridCoverage2D krigingRaster = kriging.outGrid;
            double[] expected = krigingRaster.evaluate(new Point2D.Double(stationCoordinate.x, stationCoordinate.y),
                    (double[]) null);
            assertEquals(expected[0], values[0], 0.01);
            
            // Save grid for debug purposes
            if (ENABLE_TEST_GEOTIFF_WRITING) {
            	byte[] geoTIFF = makeGeoTIFF(krigingRaster);
                FileOutputStream stream = new FileOutputStream(new File("kriging-test-jgrasstools.tif"));
                IOUtils.write(geoTIFF, stream);
            }
        }

        // Close the time series reader
        reader.close();
    }
    
    /**
	 * Creates a GeoTIFF file representing the input coverage and returns
	 * it as a byte array. It can then be persisted to disk (usually with
	 * .tif extension).
	 * 
	 * <p>The data in the image is guaranteed to be in 32 bits.
	 * @throws StriderException 
	 * 
	 * @author Rafael Almeida (@rafaelalmeida)
	 */
	private byte[] makeGeoTIFF(GridCoverage2D coverage) throws IOException {
		// Ensure the output will be a 32-bit image
		coverage = transcodeCoverageTo32Bit(coverage);
		
		// Convert the coverage to the specified format
        GridCoverageFactory gridFactory = CoverageFactoryFinder.getGridCoverageFactory(null);
        gridFactory.create(coverage.getName(), new float[1][1], coverage.getEnvelope());
		
		// Convert the result into GeoTIFF format
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		GeoTiffWriter geoTIFFWriter = new GeoTiffWriter(outputStream);
		geoTIFFWriter.getFormat();
		geoTIFFWriter.write(coverage, null);
		
		// Return the GeoTIFF as a byte array
		return outputStream.toByteArray();
	}
	
	/**
	 * Creates a GridCoverage2D equivalent to the input, but with raster data 
	 * in 32-bit IEEE 754 floats.
	 * 
	 * @author Rafael Almeida (@rafaelalmeida)
	 */
	private GridCoverage2D transcodeCoverageTo32Bit(GridCoverage2D coverage) {
		// Aliases
		int width = coverage.getRenderedImage().getWidth();
		int height = coverage.getRenderedImage().getHeight();
		
		// Prepare the raster
		SampleModel model = new ComponentSampleModel(DataBuffer.TYPE_FLOAT,
				width, height, 1, width, new int[] { 0 });
		WritableRaster raster = RasterFactory.createWritableRaster(model, null);
		
		// Populate the raster by transcoding the original coverage
		for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
            	// JLS, section 4.2.3, guarantees float primitives will always be
            	// 32-bit IEEE 754 floats.
				raster.setPixel(x, y, coverage.evaluate(new GridCoordinates2D(
						x, y), new float[1]));
            }
        }
		
		// Create and return equivalent coverage with transcoded raster
		return CoverageFactoryFinder.getGridCoverageFactory(null).create(
				coverage.getName(), raster, coverage.getEnvelope());
	}
    
}
