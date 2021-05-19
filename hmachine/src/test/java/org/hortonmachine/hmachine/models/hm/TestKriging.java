package org.hortonmachine.hmachine.models.hm;
//
//import java.awt.geom.Point2D;
//import java.io.File;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Set;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.coverage.grid.GridGeometry2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.geotools.filter.text.cql2.CQL;
//import org.geotools.referencing.CRS;
//import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
//import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
//import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
//import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
//import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
//import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
//import org.hortonmachine.hmachine.modules.statistics.kriging.OmsKriging;
//import org.hortonmachine.hmachine.utils.HMTestCase;
//import org.hortonmachine.hmachine.utils.HMTestMaps;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.filter.Filter;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.Geometry;
//
///**
// * Test the kriging model.
// * 
// * @author daniele andreis
// * 
// */
//public class TestKriging extends HMTestCase {
//
//    private File stazioniFile;
//    private File puntiFile;
//    private File krigingRainFile;
//    private String interpolatedRainPath;
//    private File krigingRain2File;
//    private File krigingRain3File;
//    private File krigingRain4File;
//    private File stazioniGridFile;
//
//    @Override
//    protected void setUp() throws Exception {
//
//        URL stazioniUrl = this.getClass().getClassLoader().getResource("rainstations.shp");
//        stazioniFile = new File(stazioniUrl.toURI());
//
//        URL puntiUrl = this.getClass().getClassLoader().getResource("basins_passirio_width0.shp");
//        puntiFile = new File(puntiUrl.toURI());
//
//        URL krigingRainUrl = this.getClass().getClassLoader().getResource("rain_test.csv");
//        krigingRainFile = new File(krigingRainUrl.toURI());
//
//        URL krigingRain2Url = this.getClass().getClassLoader().getResource("rain_test2A.csv");
//        krigingRain2File = new File(krigingRain2Url.toURI());
//
//        URL krigingRain3Url = this.getClass().getClassLoader().getResource("rain_test3A.csv");
//        krigingRain3File = new File(krigingRain3Url.toURI());
//
//        URL stazioniGridUrl = this.getClass().getClassLoader().getResource("rainstationgrid.shp");
//        stazioniGridFile = new File(stazioniGridUrl.toURI());
//
//        URL krigingRain4Url = this.getClass().getClassLoader().getResource("rain_test_grid.csv");
//        krigingRain4File = new File(krigingRain4Url.toURI());
//
//        File interpolatedRainFile = new File(krigingRainFile.getParentFile(), "kriging_interpolated.csv");
//        interpolatedRainPath = interpolatedRainFile.getAbsolutePath();
//        // interpolatedRainPath = interpolatedRainPath.replaceFirst("target",
//        // "src" + File.separator + File.separator + "test");
//        interpolatedRainPath = interpolatedRainPath.replaceFirst("target", "src" + File.separator + "test");
//
//        interpolatedRainPath = interpolatedRainPath.replaceFirst("test-classes", "resources");
//
//        super.setUp();
//    }
//
//    @SuppressWarnings("nls")
//    public void testKriging() throws Exception {
//        //
//        String stationIdField = "ID_PUNTI_M";
//
//        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
//        stationsReader.file = stazioniGridFile.getAbsolutePath();
//        stationsReader.readFeatureCollection();
//        SimpleFeatureCollection stationsFC = stationsReader.geodata;
//
//        // OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
//        // interpolatedPointsReader.file = puntiFile.getAbsolutePath();
//        // interpolatedPointsReader.readFeatureCollection();
//
//        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
//        reader.file = krigingRain4File.getAbsolutePath();
//        reader.idfield = "ID";
//        reader.tStart = "2000-01-01 00:00";
//        reader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        reader.fileNovalue = "-9999";
//        reader.initProcess();
//
//        OmsKriging kriging = new OmsKriging();
//        kriging.pm = pm;
//
//        GridGeometry2D gridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(5204514.51713, 5141634.51713,
//                686136.82243, 601576.82243, 2114, 1572, HMTestMaps.getCrs());
//        kriging.inInterpolationGrid = gridGeometry2D;
//
//        kriging.inStations = stationsFC;
//        kriging.fStationsid = stationIdField;
//
//        // kriging.inInterpolate = interpolatedPointsFC;
//        kriging.fInterpolateid = "netnum";
//
//        // it doesn't execute the model with log value.
//        kriging.doLogarithmic = false;
//        /*
//         * Set up the model in order to use the variogram with an explicit
//         * integral scale and variance.
//         */
//        // kriging.pVariance = 3.5;
//        // kriging.pIntegralscale = new double[]{10000, 10000, 100};
//        kriging.defaultVariogramMode = 1;
//        kriging.pA = 123537.0;
//        kriging.pNug = 0.0;
//        kriging.pS = 1.678383;
//        /*
//         * Set up the model in order to run with a FeatureCollection as point to
//         * interpolated. In this case only 2D.
//         */
//        kriging.pMode = 1;
//        kriging.pSemivariogramType = 1;
//
//        // OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
//        // writer.file = interpolatedRainPath;
//        //
//        // writer.tStart = reader.tStart;
//        // writer.tTimestep = reader.tTimestep;
//        while( reader.doProcess ) {
//            reader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = reader.outFolder;
//            kriging.inData = id2ValueMap;
//            kriging.executeKriging();
//            /*
//             * Extract the result.
//             */
//
//            double[] values = id2ValueMap.get(1331);
//            Filter filter = CQL.toFilter(stationIdField + " = 1331");
//            SimpleFeatureCollection subCollection = stationsFC.subCollection(filter);
//            assertTrue(subCollection.size() == 1);
//
//            SimpleFeature station = subCollection.features().next();
//            Geometry geometry = (Geometry) station.getDefaultGeometry();
//            Coordinate stationCoordinate = geometry.getCoordinate();
//
//            GridCoverage2D krigingRaster = kriging.outGrid;
//            double[] expected = krigingRaster.evaluate(new Point2D.Double(stationCoordinate.x, stationCoordinate.y),
//                    (double[]) null);
//
//            assertEquals(expected[0], values[0], 0.01);
//
//            // HashMap<Integer, double[]> result = kriging.outFolder;
//            // Set<Integer> pointsToInterpolateResult = result.keySet();
//            // Iterator<Integer> iteratorTest = pointsToInterpolateResult
//            // .iterator();
//
//            int iii = 0;
//            // while (iteratorTest.hasNext() && iii<12) {
//            // double expected;
//            // if (j == 0) {
//            // expected = 0.3390869;
//            // } else if (j == 1) {
//            // expected = 0.2556174;
//            // } else if (j == 2) {
//            // expected = 0.2428944;
//            // } else if (j == 3) {
//            // expected = 0.2613782;
//            // } else if (j == 4) {
//            // expected = 0.3112850;
//            // } else if (j == 5) {
//            // expected = 0.2983679;
//            // } else if (j == 6) {
//            // expected = 0.3470377;
//            // } else if (j == 7) {
//            // expected = 0.3874065;
//            // } else if (j == 8) {
//            // expected = 0.2820323;
//            // } else if (j == 9) {
//            // expected = 0.1945515;
//            // } else if (j == 10) {
//            // expected = 0.1698022;
//            // } else if (j == 11) {
//            // expected = 0.2405134;
//            // } else if (j == 12) {
//            // expected = 0.2829313;
//            // } else {
//            // expected = 1.0;
//            // }
//            //
//
//            //
//            // int id = iteratorTest.next();
//            // double[] actual = result.get(id);
//            // iii+=1;
//            //
//            // //assertEquals(expected, actual[0], 0.001);
//            // j=j+1;
//            // }
//            // iii=0;
//            // j=0;
//            // writer.inData = result;
//            // writer.writeNextLine();
//
//        }
//
//        reader.close();
//        // writer.close();
//    }
//    //
//    //
//
//    // ///////////////////////////////////////////////////////////////////////////////////////////
//    // /////////////////////////////////TEST 1
//    // PASSA////////////////////////////////////////////////////
//    // /////////////////////////////////////////////////////////////////////////////////////////
//    public void testKriging1() throws Exception {
//
//        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
//        stationsReader.file = stazioniFile.getAbsolutePath();
//        stationsReader.readFeatureCollection();
//        SimpleFeatureCollection stationsFC = stationsReader.geodata;
//
//        OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
//        interpolatedPointsReader.file = puntiFile.getAbsolutePath();
//        interpolatedPointsReader.readFeatureCollection();
//        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
//
//        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
//        reader.file = krigingRainFile.getAbsolutePath();
//        reader.idfield = "ID";
//        reader.tStart = "2000-01-01 00:00";
//        reader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        reader.fileNovalue = "-9999";
//
//        reader.initProcess();
//
//        OmsKriging kriging = new OmsKriging();
//        kriging.pm = pm;
//
//        kriging.inStations = stationsFC;
//        kriging.fStationsid = "ID_PUNTI_M";
//
//        kriging.inInterpolate = interpolatedPointsFC;
//        kriging.fInterpolateid = "netnum";
//
//        // it doesn't execute the model with log value.
//        kriging.doLogarithmic = false;
//        /*
//        * Set up the model in order to use the variogram with an explicit
//        * integral scale and variance.
//        */
//        // kriging.pVariance = 3.5;
//        // kriging.pIntegralscale = new double[]{10000, 10000, 100};
//        kriging.defaultVariogramMode = 1;
//        kriging.pA = 123537.0;
//        kriging.pNug = 0.0;
//        kriging.pS = 1.678383;
//        /*
//        * Set up the model in order to run with a FeatureCollection as point to
//        * interpolated. In this case only 2D.
//        */
//        kriging.pMode = 0;
//        kriging.pSemivariogramType = 1;
//
//        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
//        writer.file = interpolatedRainPath;
//
//        writer.tStart = reader.tStart;
//        writer.tTimestep = reader.tTimestep;
//        int j = 0;
//        while( reader.doProcess ) {
//            reader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = reader.outFolder;
//            kriging.inData = id2ValueMap;
//            kriging.executeKriging();
//            /*
//            * Extract the result.
//            */
//            HashMap<Integer, double[]> result = kriging.outFolder;
//            Set<Integer> pointsToInterpolateResult = result.keySet();
//            Iterator<Integer> iteratorTest = pointsToInterpolateResult.iterator();
//
//            int iii = 0;
//
//            while( iteratorTest.hasNext() && iii < 12 ) {
//                double expected;
//                if (j == 0) {
//                    expected = 0.3390869;
//                } else if (j == 1) {
//                    expected = 0.2556174;
//                } else if (j == 2) {
//                    expected = 0.2428944;
//                } else if (j == 3) {
//                    expected = 0.2613782;
//                } else if (j == 4) {
//                    expected = 0.3112850;
//                } else if (j == 5) {
//                    expected = 0.2983679;
//                } else if (j == 6) {
//                    expected = 0.3470377;
//                } else if (j == 7) {
//                    expected = 0.3874065;
//                } else if (j == 8) {
//                    expected = 0.2820323;
//                } else if (j == 9) {
//                    expected = 0.1945515;
//                } else if (j == 10) {
//                    expected = 0.1698022;
//                } else if (j == 11) {
//                    expected = 0.2405134;
//                } else if (j == 12) {
//                    expected = 0.2829313;
//                } else {
//                    expected = 1.0;
//                }
//
//                int id = iteratorTest.next();
//                double[] actual = result.get(id);
//                iii += 1;
//
//                assertEquals(expected, actual[0], 0.001);
//                j = j + 1;
//            }
//            iii = 0;
//            j = 0;
//            writer.inData = result;
//            writer.writeNextLine();
//
//        }
//
//        reader.close();
//        writer.close();
//    }
//
//    // ///////////////////////////////////////////////////////////////////////////////////////////
//    // /////////////////////////////////FINE TEST
//    // 1PASSA////////////////////////////////////////////////////
//    // /////////////////////////////////////////////////////////////////////////////////////////
//    //
//    //
//    //
//    // ///////////////////////////////////////////////////////////////////////////////////////////
//    // /////////////////////////////////TEST 2
//    // PASSA////////////////////////////////////////////////////
//    // /////////////////////////////////////////////////////////////////////////////////////////
//
//    /**
//    * Run the kriging models.
//    *
//    * <p>
//    * This is the case which all the station have the same value.
//    * </p>
//    * @throws Exception
//    * @throws Exception
//    */
//    public void testKriging2() throws Exception {
//        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
//        stationsReader.file = stazioniFile.getAbsolutePath();
//        stationsReader.readFeatureCollection();
//        SimpleFeatureCollection stationsFC = stationsReader.geodata;
//
//        OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
//        interpolatedPointsReader.file = puntiFile.getAbsolutePath();
//        interpolatedPointsReader.readFeatureCollection();
//        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
//
//        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
//        reader.file = krigingRain2File.getAbsolutePath();
//        reader.idfield = "ID";
//        reader.tStart = "2000-01-01 00:00";
//        reader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        reader.fileNovalue = "-9999";
//
//        reader.initProcess();
//
//        OmsKriging kriging = new OmsKriging();
//        kriging.pm = pm;
//
//        kriging.inStations = stationsFC;
//        kriging.fStationsid = "ID_PUNTI_M";
//
//        kriging.inInterpolate = interpolatedPointsFC;
//        kriging.fInterpolateid = "netnum";
//
//        // it doesn't execute the model with log value.
//        kriging.doLogarithmic = false;
//        /*
//        * Set up the model in order to use the variogram with an explicit integral scale and
//        variance.
//        */
//        kriging.pVariance = 0.5;
//        kriging.pIntegralscale = new double[]{10000, 10000, 100};
//        /*
//        * Set up the model in order to run with a FeatureCollection as point to interpolated. In this
//        case only 2D.
//        */
//        kriging.pMode = 0;
//
//        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
//        writer.file = interpolatedRainPath;
//
//        writer.tStart = reader.tStart;
//        writer.tTimestep = reader.tTimestep;
//
//        while( reader.doProcess ) {
//            reader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = reader.outFolder;
//            kriging.inData = id2ValueMap;
//            kriging.executeKriging();
//            /*
//            * Extract the result.
//            */
//            HashMap<Integer, double[]> result = kriging.outFolder;
//            Set<Integer> pointsToInterpolateResult = result.keySet();
//            Iterator<Integer> iterator = pointsToInterpolateResult.iterator();
//            while( iterator.hasNext() ) {
//                int id = iterator.next();
//                double[] actual = result.get(id);
//                assertEquals(1.0, actual[0], 0);
//            }
//            writer.inData = result;
//            writer.writeNextLine();
//        }
//
//        reader.close();
//        writer.close();
//    }
//    // /////////////////////////////////////////////////////////////////////////////////////////
//    // ///////////////////////////////FINE TEST 2
//    // PASSA////////////////////////////////////////////////////
//    // ///////////////////////////////////////////////////////////////////////////////////////
//
//    // /////////////////////////////////////////////////////////////////////////////////////////
//    // /////////////////////////////// TEST 3
//    // PASSA////////////////////////////////////////////////////
//    // ///////////////////////////////////////////////////////////////////////////////////////
//    // /**
//    // * Run the kriging models.
//    // *
//    // * <p>
//    // * This is the case that defaultMode=0.
//    // * </p>
//    // * @throws Exception
//    // * @throws Exception
//    // */
//    public void testKriging4() throws Exception {
//        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
//        stationsReader.file = stazioniFile.getAbsolutePath();
//        stationsReader.readFeatureCollection();
//        SimpleFeatureCollection stationsFC = stationsReader.geodata;
//
//        OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
//        interpolatedPointsReader.file = puntiFile.getAbsolutePath();
//        interpolatedPointsReader.readFeatureCollection();
//        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
//
//        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
//        reader.file = krigingRainFile.getAbsolutePath();
//        reader.idfield = "ID";
//        reader.tStart = "2000-01-01 00:00";
//        reader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        reader.fileNovalue = "-9999";
//
//        reader.initProcess();
//
//        OmsKriging kriging = new OmsKriging();
//        kriging.pm = pm;
//
//        kriging.inStations = stationsFC;
//        kriging.fStationsid = "ID_PUNTI_M";
//
//        kriging.inInterpolate = interpolatedPointsFC;
//        kriging.fInterpolateid = "netnum";
//
//        // it doesn't execute the model with log value.
//        kriging.doLogarithmic = false;
//        /*
//        * Set up the model in order to use the variogram with an explicit integral scale and
//        variance.
//        */
//        kriging.pVariance = 3.5;
//        kriging.pIntegralscale = new double[]{10000, 10000, 100};
//        /*
//        * Set up the model in order to run with a FeatureCollection as point to interpolated. In this
//        case only 2D.
//        */
//        kriging.pMode = 0;
//
//        kriging.doIncludezero = false;
//        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
//        writer.file = interpolatedRainPath;
//
//        writer.tStart = reader.tStart;
//        writer.tTimestep = reader.tTimestep;
//
//        while( reader.doProcess ) {
//            reader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = reader.outFolder;
//            kriging.inData = id2ValueMap;
//            kriging.executeKriging();
//            /*
//            * Extract the result.
//            */
//            HashMap<Integer, double[]> result = kriging.outFolder;
//            double[][] test = HMTestMaps.outKriging4;
//            for( int i = 0; i < test.length; i++ ) {
//                double actual = result.get((int) test[i][0])[0];
//                double expected = test[i][1];
//                assertEquals(expected, actual, 0.01);
//            }
//
//            writer.inData = result;
//            writer.writeNextLine();
//        }
//
//        reader.close();
//        writer.close();
//    }
//    // /////////////////////////////////////////////////////////////////////////////////////////
//    // ///////////////////////////////FINE TEST 3
//    // PASSA////////////////////////////////////////////////////
//    // ///////////////////////////////////////////////////////////////////////////////////////
//
//    // /////////////////////////////////////////////////////////////////////////////////////////
//    // /////////////////////////////// TEST 4
//    // PASSA////////////////////////////////////////////////////
//    // ///////////////////////////////////////////////////////////////////////////////////////
//    /**
//    * Run the kriging models.
//    *
//    * <p>
//    * This is the case which there is only one station.
//    * </p>
//    * @throws Exception
//    * @throws Exception
//    */
//    public void testKriging5() throws Exception {
//        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
//        stationsReader.file = stazioniFile.getAbsolutePath();
//        stationsReader.readFeatureCollection();
//        SimpleFeatureCollection stationsFC = stationsReader.geodata;
//
//        OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
//        interpolatedPointsReader.file = puntiFile.getAbsolutePath();
//        interpolatedPointsReader.readFeatureCollection();
//        SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
//
//        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
//        reader.file = krigingRain3File.getAbsolutePath();
//        reader.idfield = "ID";
//        reader.tStart = "2000-01-01 00:00";
//        reader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        reader.fileNovalue = "-9999";
//
//        reader.initProcess();
//
//        OmsKriging kriging = new OmsKriging();
//        kriging.pm = pm;
//
//        kriging.inStations = stationsFC;
//        kriging.fStationsid = "ID_PUNTI_M";
//
//        kriging.inInterpolate = interpolatedPointsFC;
//        kriging.fInterpolateid = "netnum";
//
//        // it doesn't execute the model with log value.
//        kriging.doLogarithmic = false;
//        /*
//        * Set up the model in order to use the variogram with an explicit integral scale and
//        variance.
//        */
//        kriging.pVariance = 0.5;
//        kriging.pIntegralscale = new double[]{10000, 10000, 100};
//        /*
//        * Set up the model in order to run with a FeatureCollection as point to interpolated. In this
//        case only 2D.
//        */
//        kriging.pMode = 0;
//
//        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
//        writer.file = interpolatedRainPath;
//
//        writer.tStart = reader.tStart;
//        writer.tTimestep = reader.tTimestep;
//        int j = 0;
//        while( reader.doProcess ) {
//            reader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = reader.outFolder;
//            kriging.inData = id2ValueMap;
//            kriging.executeKriging();
//            /*
//            * Extract the result.
//            */
//            HashMap<Integer, double[]> result = kriging.outFolder;
//            Set<Integer> pointsToInterpolateResult = result.keySet();
//            Iterator<Integer> iteratorTest = pointsToInterpolateResult.iterator();
//            double expected;
//            if (j == 0) {
//                expected = 10.0;
//            } else if (j == 1) {
//                expected = 15;
//            } else if (j == 2) {
//                expected = 1;
//            } else if (j == 3) {
//                expected = 2;
//            } else if (j == 4) {
//                expected = 2;
//            } else if (j == 5) {
//                expected = 0;
//            } else if (j == 6) {
//                expected = 0;
//            } else if (j == 7) {
//                expected = 23;
//            } else if (j == 8) {
//                expected = 50;
//            } else if (j == 9) {
//                expected = 70;
//            } else if (j == 10) {
//                expected = 30;
//            } else if (j == 11) {
//                expected = 10;
//            } else if (j == 12) {
//                expected = 2;
//            } else {
//                expected = 1.0;
//            }
//
//            while( iteratorTest.hasNext() ) {
//                int id = iteratorTest.next();
//                double[] actual = result.get(id);
//
//                assertEquals(expected, actual[0], 0);
//            }
//            writer.inData = result;
//            writer.writeNextLine();
//            j++;
//        }
//
//        reader.close();
//        writer.close();
//    }
//    // ///////////////////////////////////////////////////////////////////////////////////////
//    // /////////////////////////////FINE TEST 4
//    // PASSA////////////////////////////////////////////////////
//    // /////////////////////////////////////////////////////////////////////////////////////
//    @Override
//    protected void tearDown() throws Exception {
//        File remove = new File(interpolatedRainPath);
//        if (remove.exists()) {
//            if (!remove.delete()) {
//                remove.deleteOnExit();
//            }
//        }
//
//        super.tearDown();
//    }
//
//}
