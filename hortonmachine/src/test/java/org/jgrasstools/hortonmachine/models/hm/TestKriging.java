package org.jgrasstools.hortonmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorWriter;
import org.jgrasstools.hortonmachine.modules.statistics.kriging.Kriging;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
/**
 * Test the kriging model.
 * 
 * @author daniele andreis
 *
 */
public class TestKriging extends HMTestCase {

    private File stazioniFile;
    private File puntiFile;
    private File krigingRainFile;
    private String interpolatedRainPath;
    private File krigingRain2File;
    private File krigingRain3File;

    @Override
    protected void setUp() throws Exception {

        URL stazioniUrl = this.getClass().getClassLoader().getResource("rainstations.shp");
        stazioniFile = new File(stazioniUrl.toURI());
        URL puntiUrl = this.getClass().getClassLoader().getResource("basins_passirio_width0.shp");

        puntiFile = new File(puntiUrl.toURI());
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("rain_test.csv");
        krigingRainFile = new File(krigingRainUrl.toURI());

        URL krigingRain2Url = this.getClass().getClassLoader().getResource("rain_test2A.csv");
        krigingRain2File = new File(krigingRain2Url.toURI());

        URL krigingRain3Url = this.getClass().getClassLoader().getResource("rain_test3A.csv");
        krigingRain3File = new File(krigingRain3Url.toURI());

        File interpolatedRainFile = new File(krigingRainFile.getParentFile(), "kriging_interpolated.csv");
        interpolatedRainPath = interpolatedRainFile.getAbsolutePath();
        interpolatedRainPath = interpolatedRainPath.replaceFirst("target", "src" + File.separator + File.separator + "test");
        interpolatedRainPath = interpolatedRainPath.replaceFirst("test-classes", "resources");

        super.setUp();
    }

    // public void testKriging() throws Exception {
    //
    // ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
    // stationsReader.file = stazioniFile.getAbsolutePath();
    // stationsReader.readFeatureCollection();
    // SimpleFeatureCollection stationsFC = stationsReader.geodata;
    //
    // ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
    // interpolatedPointsReader.file = puntiFile.getAbsolutePath();
    // interpolatedPointsReader.readFeatureCollection();
    // SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
    //
    // TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
    // reader.file = krigingRainFile.getAbsolutePath();
    // reader.idfield = "ID";
    // reader.tStart = "2000-01-01 00:00";
    // reader.tTimestep = 60;
    // // reader.tEnd = "2000-01-01 00:00";
    // reader.fileNovalue = "-9999";
    //
    // reader.initProcess();
    //
    // Kriging kriging = new Kriging();
    // kriging.pm = pm;
    //
    // kriging.inStations = stationsFC;
    // kriging.fStationsid = "ID_PUNTI_M";
    //
    // kriging.inInterpolate = interpolatedPointsFC;
    // kriging.fInterpolateid = "netnum";
    //
    // // it doesn't execute the model with log value.
    // kriging.doLogarithmic = false;
    // /*
    // * Set up the model in order to use the variogram with an explicit integral scale and
    // variance.
    // */
    // kriging.pVariance = 3.5;
    // kriging.pIntegralscale = new double[]{10000, 10000, 100};
    // /*
    // * Set up the model in order to run with a FeatureCollection as point to interpolated. In this
    // case only 2D.
    // */
    // kriging.pMode = 0;
    //
    // TimeSeriesIteratorWriter writer = new TimeSeriesIteratorWriter();
    // writer.file = interpolatedRainPath;
    //
    // writer.tStart = reader.tStart;
    // writer.tTimestep = reader.tTimestep;
    //
    // while( reader.doProcess ) {
    // reader.nextRecord();
    // HashMap<Integer, double[]> id2ValueMap = reader.outData;
    // kriging.inData = id2ValueMap;
    // kriging.executeKriging();
    // /*
    // * Extract the result.
    // */
    // HashMap<Integer, double[]> result = kriging.outData;
    // writer.inData = result;
    // writer.writeNextLine();
    // }
    //
    // reader.close();
    // writer.close();
    // }

    /**
     * Run the kriging models.
     * 
     * <p>
     * This is the case 2 station have the same coordinates but different value to interpolate (throw exception).
     * </p>
     * @throws Exception 
     * @throws Exception
     */
    public void testKriging1() throws Exception {

        ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
        stationsReader.file = stazioniFile.getAbsolutePath();
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;

        ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
        interpolatedPointsReader.file = puntiFile.getAbsolutePath();
        interpolatedPointsReader.readFeatureCollection();
        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;

        TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
        reader.file = krigingRainFile.getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";

        reader.initProcess();

        Kriging kriging = new Kriging();
        kriging.pm = pm;

        kriging.inStations = stationsFC;
        kriging.fStationsid = "ID_PUNTI_M";

        kriging.inInterpolate = interpolatedPointsFC;
        kriging.fInterpolateid = "netnum";

        // it doesn't execute the model with log value.
        kriging.doLogarithmic = false;
        /*
         * Set up the model in order to use the variogram with an explicit integral scale and variance.
         */
        kriging.pA = 1000;
        kriging.pS = 0.68;
        kriging.pNug = 0;

        /*
         * Set up the model in order to run with a FeatureCollection as point to interpolated. In this case only 2D.
         */
        kriging.pMode = 0;
        kriging.defaultVariogramMode = 1;
        TimeSeriesIteratorWriter writer = new TimeSeriesIteratorWriter();
        writer.file = interpolatedRainPath;

        writer.tStart = reader.tStart;
        writer.tTimestep = reader.tTimestep;

        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            kriging.inData = id2ValueMap;
            try {
                kriging.executeKriging();
                fail();
            } catch (Exception e) {
                // TODO: handle exception
            }

        }

        reader.close();
    }

    /**
     * Run the kriging models.
     * 
     * <p>
     * This is the case which all the station have the same value.
     * </p>
     * @throws Exception 
     * @throws Exception
     */
    public void testKriging2() throws Exception {
        ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
        stationsReader.file = stazioniFile.getAbsolutePath();
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;

        ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
        interpolatedPointsReader.file = puntiFile.getAbsolutePath();
        interpolatedPointsReader.readFeatureCollection();
        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;

        TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
        reader.file = krigingRain2File.getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        // reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";

        reader.initProcess();

        Kriging kriging = new Kriging();
        kriging.pm = pm;

        kriging.inStations = stationsFC;
        kriging.fStationsid = "ID_PUNTI_M";

        kriging.inInterpolate = interpolatedPointsFC;
        kriging.fInterpolateid = "netnum";

        // it doesn't execute the model with log value.
        kriging.doLogarithmic = false;
        /*
         * Set up the model in order to use the variogram with an explicit integral scale and variance.
         */
        kriging.pVariance = 0.5;
        kriging.pIntegralscale = new double[]{10000, 10000, 100};
        /*
         * Set up the model in order to run with a FeatureCollection as point to interpolated. In this case only 2D.
         */
        kriging.pMode = 0;

        TimeSeriesIteratorWriter writer = new TimeSeriesIteratorWriter();
        writer.file = interpolatedRainPath;

        writer.tStart = reader.tStart;
        writer.tTimestep = reader.tTimestep;

        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            kriging.inData = id2ValueMap;
            kriging.executeKriging();
            /*
             * Extract the result.
             */
            HashMap<Integer, double[]> result = kriging.outData;
            Set<Integer> pointsToInterpolateResult = result.keySet();
            Iterator<Integer> iterator = pointsToInterpolateResult.iterator();
            while( iterator.hasNext() ) {
                int id = iterator.next();
                double[] actual = result.get(id);
                assertEquals(1.0, actual[0], 0);
            }
            writer.inData = result;
            writer.writeNextLine();
        }

        reader.close();
        writer.close();
    }

    // /**
    // * Run the kriging models.
    // *
    // * <p>
    // * This is the case which the defaultMode=1.
    // * </p>
    // * @throws Exception
    // * @throws Exception
    // */
    // public void testKriging3() throws Exception {
    // ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
    // stationsReader.file = stazioniFile.getAbsolutePath();
    // stationsReader.readFeatureCollection();
    // SimpleFeatureCollection stationsFC = stationsReader.geodata;
    //
    // ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
    // interpolatedPointsReader.file = puntiFile.getAbsolutePath();
    // interpolatedPointsReader.readFeatureCollection();
    // SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
    //
    // TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
    // reader.file = krigingRainFile.getAbsolutePath();
    // reader.idfield = "ID";
    // reader.tStart = "2000-01-01 00:00";
    // reader.tTimestep = 60;
    // // reader.tEnd = "2000-01-01 00:00";
    // reader.fileNovalue = "-9999";
    //
    // reader.initProcess();
    //
    // Kriging kriging = new Kriging();
    // kriging.pm = pm;
    //
    // kriging.inStations = stationsFC;
    // kriging.fStationsid = "ID_PUNTI_M";
    //
    // kriging.inInterpolate = interpolatedPointsFC;
    // kriging.fInterpolateid = "netnum";
    //
    // // it doesn't execute the model with log value.
    // kriging.doLogarithmic = false;
    // /*
    // * Set up the model in order to use the variogram with an explicit integral scale and
    // variance.
    // */
    // kriging.pA = 1000;
    // kriging.pS = 0.68;
    // kriging.pNug = 0;
    //
    // /*
    // * Set up the model in order to run with a FeatureCollection as point to interpolated. In this
    // case only 2D.
    // */
    // kriging.pMode = 0;
    // kriging.defaultVariogramMode = 1;
    // TimeSeriesIteratorWriter writer = new TimeSeriesIteratorWriter();
    // writer.file = interpolatedRainPath;
    //
    // writer.tStart = reader.tStart;
    // writer.tTimestep = reader.tTimestep;
    //
    // while( reader.doProcess ) {
    // reader.nextRecord();
    // HashMap<Integer, double[]> id2ValueMap = reader.outData;
    // kriging.inData = id2ValueMap;
    // kriging.executeKriging();
    // /*
    // * Extract the result.
    // */
    // HashMap<Integer, double[]> result = kriging.outData;
    // double[][] test = HMTestMaps.outKriging3;
    // for( int i = 0; i < test.length; i++ ) {
    // assertEquals(test[i][1], result.get((int) test[i][0])[0], 0.01);
    // }
    //
    // writer.inData = result;
    // writer.writeNextLine();
    // }
    //
    // reader.close();
    // writer.close();
    // }
    //
    // /**
    // * Run the kriging models.
    // *
    // * <p>
    // * This is the case that defaultMode=0.
    // * </p>
    // * @throws Exception
    // * @throws Exception
    // */
    // public void testKriging4() throws Exception {
    // ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
    // stationsReader.file = stazioniFile.getAbsolutePath();
    // stationsReader.readFeatureCollection();
    // SimpleFeatureCollection stationsFC = stationsReader.geodata;
    //
    // ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
    // interpolatedPointsReader.file = puntiFile.getAbsolutePath();
    // interpolatedPointsReader.readFeatureCollection();
    // SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
    //
    // TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
    // reader.file = krigingRainFile.getAbsolutePath();
    // reader.idfield = "ID";
    // reader.tStart = "2000-01-01 00:00";
    // reader.tTimestep = 60;
    // // reader.tEnd = "2000-01-01 00:00";
    // reader.fileNovalue = "-9999";
    //
    // reader.initProcess();
    //
    // Kriging kriging = new Kriging();
    // kriging.pm = pm;
    //
    // kriging.inStations = stationsFC;
    // kriging.fStationsid = "ID_PUNTI_M";
    //
    // kriging.inInterpolate = interpolatedPointsFC;
    // kriging.fInterpolateid = "netnum";
    //
    // // it doesn't execute the model with log value.
    // kriging.doLogarithmic = false;
    // /*
    // * Set up the model in order to use the variogram with an explicit integral scale and
    // variance.
    // */
    // kriging.pVariance = 3.5;
    // kriging.pIntegralscale = new double[]{10000, 10000, 100};
    // /*
    // * Set up the model in order to run with a FeatureCollection as point to interpolated. In this
    // case only 2D.
    // */
    // kriging.pMode = 0;
    //
    // TimeSeriesIteratorWriter writer = new TimeSeriesIteratorWriter();
    // writer.file = interpolatedRainPath;
    //
    // writer.tStart = reader.tStart;
    // writer.tTimestep = reader.tTimestep;
    //
    // while( reader.doProcess ) {
    // reader.nextRecord();
    // HashMap<Integer, double[]> id2ValueMap = reader.outData;
    // kriging.inData = id2ValueMap;
    // kriging.executeKriging();
    // /*
    // * Extract the result.
    // */
    // HashMap<Integer, double[]> result = kriging.outData;
    // double[][] test = HMTestMaps.outKriging4;
    // for( int i = 0; i < test.length; i++ ) {
    // double actual = result.get((int) test[i][0])[0];
    // double expected = test[i][1];
    // assertEquals(expected, actual, 0.01);
    // }
    //
    // writer.inData = result;
    // writer.writeNextLine();
    // }
    //
    // reader.close();
    // writer.close();
    // }
    //
    // /**
    // * Run the kriging models.
    // *
    // * <p>
    // * This is the case which there is only one station.
    // * </p>
    // * @throws Exception
    // * @throws Exception
    // */
    // public void testKriging5() throws Exception {
    // ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
    // stationsReader.file = stazioniFile.getAbsolutePath();
    // stationsReader.readFeatureCollection();
    // SimpleFeatureCollection stationsFC = stationsReader.geodata;
    // ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
    // interpolatedPointsReader.file = puntiFile.getAbsolutePath();
    // interpolatedPointsReader.readFeatureCollection();
    // SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
    //
    // TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
    // reader.file = krigingRain3File.getAbsolutePath();
    // reader.idfield = "ID";
    // reader.tStart = "2000-01-01 00:00";
    // reader.tTimestep = 60;
    // // reader.tEnd = "2000-01-01 00:00";
    // reader.fileNovalue = "-9999";
    //
    // reader.initProcess();
    //
    // Kriging kriging = new Kriging();
    // kriging.pm = pm;
    //
    // kriging.inStations = stationsFC;
    // kriging.fStationsid = "ID_PUNTI_M";
    //
    // kriging.inInterpolate = interpolatedPointsFC;
    // kriging.fInterpolateid = "id";
    //
    // // it doesn't execute the model with log value.
    // kriging.doLogarithmic = false;
    // /*
    // * Set up the model in order to use the variogram with an explicit integral scale and
    // variance.
    // */
    // kriging.pVariance = 0.5;
    // kriging.pIntegralscale = new double[]{10000, 10000, 100};
    // /*
    // * Set up the model in order to run with a FeatureCollection as point to interpolated. In this
    // case only 2D.
    // */
    // kriging.pMode = 0;
    //
    // TimeSeriesIteratorWriter writer = new TimeSeriesIteratorWriter();
    // writer.file = interpolatedRainPath;
    //
    // writer.tStart = reader.tStart;
    // writer.tTimestep = reader.tTimestep;
    // int j = 0;
    // while( reader.doProcess ) {
    // reader.nextRecord();
    // HashMap<Integer, double[]> id2ValueMap = reader.outData;
    // kriging.inData = id2ValueMap;
    // kriging.executeKriging();
    // /*
    // * Extract the result.
    // */
    // HashMap<Integer, double[]> result = kriging.outData;
    // Set<Integer> pointsToInterpolateResult = result.keySet();
    // Iterator<Integer> iteratorTest = pointsToInterpolateResult.iterator();
    // double expected;
    // if (j == 0) {
    // expected = 0.0;
    // } else if (j == 1) {
    // expected = Double.NaN;
    // } else {
    // expected = 1.0;
    // }
    // while( iteratorTest.hasNext() ) {
    // int id = iteratorTest.next();
    // double[] actual = result.get(id);
    //
    // assertEquals(expected, actual[0], 0);
    // }
    // writer.inData = result;
    // writer.writeNextLine();
    // j++;
    // }
    //
    // reader.close();
    // writer.close();
    // }

    @Override
    protected void tearDown() throws Exception {
        File remove = new File(interpolatedRainPath);
        if (remove.exists()) {
            if (!remove.delete()) {
                remove.deleteOnExit();
            }
        }

        super.tearDown();
    }

}
