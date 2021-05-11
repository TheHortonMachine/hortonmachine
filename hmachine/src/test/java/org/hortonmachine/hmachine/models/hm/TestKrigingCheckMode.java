package org.hortonmachine.hmachine.models.hm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.hmachine.modules.statistics.kriging.OmsKrigingVectorMode;
import org.junit.Test;

/**
 * @author Daniele Andreis
 *
 */
public class TestKrigingVector {

    private String getRes( String name ) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(name);
        File file = new File(url.toURI());
        return file.getAbsolutePath();
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
//    @Test
//    public void testKriging2() throws Exception {
//        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
//        stationsReader.file = getRes("rainstations.shp");
//        stationsReader.readFeatureCollection();
//        SimpleFeatureCollection stationsFC = stationsReader.geodata;
//        //
//        OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
//        interpolatedPointsReader.file = getRes("basins_passirio_width0.shp");
//        interpolatedPointsReader.readFeatureCollection();
//        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
//        //
//        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
//        reader.file = getRes("rain_test2A_allNoValue.csv");
//        reader.idfield = "ID";
//        reader.tStart = "2000-01-01 00:00";
//        reader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        reader.fileNovalue = "-9999";
//        //
//        reader.initProcess();
//        //
//        OmsKrigingVectorMode kriging = new OmsKrigingVectorMode();
//
//        //
//        kriging.inStations = stationsFC;
//        kriging.fStationsid = "ID_PUNTI_M";
//        //
//        kriging.inInterpolate = interpolatedPointsFC;
//        kriging.fInterpolateid = "netnum";
//        kriging.maxdist = 40368.0;
//
//        kriging.range = 123537.0;
//        kriging.nugget = 0.0;
//        kriging.sill = 1.678383;
//        kriging.pSemivariogramType = "linear";
//
//        //
//        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
//        writer.file = File.createTempFile("hm_test_", "kriging_interpolated_NoValue.csv").getAbsolutePath();
//        //
//        writer.tStart = reader.tStart;
//        writer.tTimestep = reader.tTimestep;
//        //
//        while( reader.doProcess ) {
//            reader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = reader.outData;
//            kriging.inData = id2ValueMap;
//            kriging.executeKriging();
//            /*
//             * Extract the result.
//             */
//
//            HashMap<Integer, double[]> result = kriging.outData;
//
//            /*
//            Set<Integer> pointsToInterpolateResult = result.keySet();
//            Iterator<Integer> iterator = pointsToInterpolateResult.iterator();
//            while( iterator.hasNext() ) {
//                int id = iterator.next();
//                double[] actual = result.get(id);
//                assertEquals(1.0, actual[0], 0);
//            }*/
//
//            writer.inData = result;
//            writer.writeNextLine();
//        }
//
//        //
//        reader.close();
//        writer.close();
//    }
    // /////////////////////////////////////////////////////////////////////////////////////////
    // ///////////////////////////////FINE TEST 2
    // PASSA////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////////
    //
    // /////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////// TEST 3
    // PASSA////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////////
    // /**
    // * Run the kriging models.
    // *
    // * <p>
    // * This is the case that defaultMode=0.
    // * </p>
    // * @throws Exception
    // * @throws Exception
    // */

    @Test
    public void testKriging4() throws Exception {
        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
        stationsReader.file = getRes("rainstations.shp");
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;
        //
        OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
        interpolatedPointsReader.file = getRes("basins_passirio_width0.shp");
        interpolatedPointsReader.readFeatureCollection();
        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
        //
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = getRes("rain_test.csv");
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        // reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";
        //
        reader.initProcess();
        //
        OmsKrigingVectorMode kriging = new OmsKrigingVectorMode();
        // kriging.pm = pm;
        //
        kriging.inStations = stationsFC;
        kriging.fStationsid = "ID_PUNTI_M";
        //
        kriging.inInterpolate = interpolatedPointsFC;
        kriging.fInterpolateid = "netnum";

        kriging.pSemivariogramType = "linear";

        kriging.range = 123537.0;
        kriging.nugget = 0.0;
        kriging.sill = 1.678383;
        kriging.maxdist = 1000;

        //
        kriging.doIncludezero = false;
        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
        writer.file = File.createTempFile("hm_test_", "kriging_interpolated_2.csv").getAbsolutePath();
        //
        writer.tStart = reader.tStart;
        writer.tTimestep = reader.tTimestep;
        //
        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            kriging.inData = id2ValueMap;
            kriging.executeKriging();
            /*
             * Extract the result.
             */
            HashMap<Integer, double[]> result = kriging.outData;

            //
            writer.inData = result;
            writer.writeNextLine();
        }
        //
        reader.close();
        writer.close();
    }

    /**
     * Run the kriging models.
     *
     * <p>
     * This is the case which there is only one station.
     * </p>
     * @throws Exception
     * @throws Exception
     */

    @Test
    public void testKriging5() throws Exception {
        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
        stationsReader.file = getRes("rainstations.shp");
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;
        //
        OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
        interpolatedPointsReader.file = getRes("basins_passirio_width0.shp");
        interpolatedPointsReader.readFeatureCollection();
        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
        //
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = getRes("rain_test3A.csv");
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        // reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";
        //
        reader.initProcess();
        //
        OmsKrigingVectorMode kriging = new OmsKrigingVectorMode();
        // kriging.pm = pm;
        //
        kriging.inStations = stationsFC;
        kriging.fStationsid = "ID_PUNTI_M";
        //
        kriging.inInterpolate = interpolatedPointsFC;
        kriging.fInterpolateid = "netnum";

        // Set up the model in order to use the variogram with an explicit integral scale and
        // variance.

        kriging.pSemivariogramType = "linear";
        kriging.range = 123537.0;
        kriging.nugget = 0.0;
        kriging.sill = 1.678383;
        // kriging.maxdist=1000;

        //
        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
        writer.file = File.createTempFile("hm_test_", "kriging_interpolated_3.csv").getAbsolutePath();
        //
        writer.tStart = reader.tStart;
        writer.tTimestep = reader.tTimestep;
        int j = 0;
        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            kriging.inData = id2ValueMap;
            kriging.executeKriging();

            // Extract the result.

            HashMap<Integer, double[]> result = kriging.outData;

            Set<Integer> pointsToInterpolateResult = result.keySet();
            Iterator<Integer> iteratorTest = pointsToInterpolateResult.iterator();
            double expected;
            if (j == 0) {
                expected = 10.0;
            } else if (j == 1) {
                expected = 15;
            } else if (j == 2) {
                expected = 1;
            } else if (j == 3) {
                expected = 2;
            } else if (j == 4) {
                expected = 2;
            } else if (j == 5) {
                expected = 0;
            } else if (j == 6) {
                expected = 0;
            } else if (j == 7) {
                expected = 23;
            } else if (j == 8) {
                expected = 50;
            } else if (j == 9) {
                expected = 70;
            } else if (j == 10) {
                expected = 30;
            } else if (j == 11) {
                expected = 10;
            } else if (j == 12) {
                expected = 2;
            } else {
                expected = 1.0;
            }
            //
            while( iteratorTest.hasNext() ) {
                int id = iteratorTest.next();
                double[] actual = result.get(id);
                //
                assertEquals(expected, actual[0], 0);
            }

            writer.inData = result;
            writer.writeNextLine();
            j++;
        }
        //
        reader.close();
        writer.close();
    }

}
