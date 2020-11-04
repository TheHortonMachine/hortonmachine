//package org.hortonmachine.hmachine.models.hm;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
//import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
//import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP;
//import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject;
//import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectNeededParameterCodes;
//import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectOptionalParameterCodes;
//import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectTimeParameterCodes;
//import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
//import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.DiametersReader;
//import org.hortonmachine.hmachine.utils.HMTestCase;
//import org.hortonmachine.hmachine.utils.HMTestMapstrentoP;
//import org.joda.time.DateTime;
//
///**
// * A test case for the trentoP-java model.
// * 
// * @author Daniele Andreis
// * 
// */
//public class TestTrentoP extends HMTestCase {
//
//    private final static double TOLL = 0.009;
//
//    private final static double[] INTERNAL_PARAMETERS = {1.2, 4, 40, 0.005, 0.15, 1, 30, 0.001, 0.01, 1.0, 4.43, 1.5, 0.38, 0.001,
//            1, 0.2, 0.4};
//
//    private final static double[] INTERNAL_PARAMETERS2 = {1.3, 4, 50, 0.001, 0.20, 1, 40, 0.001, 0.01, 1.0, 4.43, 1.5, 0.38,
//            0.001, 1, 0.2, 0.4};
//    /**
//     * This is a block of parameter that usually are used in the simulation of
//     * {@link OmsTrentoP}.
//     */
//    private static double a = 60.4;
//    private static double n = 0.61;
//    private static double tau = 2.5;
//    private static double g = 0.8;
//    private static Integer align = 0;
//
//    /**
//     * Test project 1.
//     * 
//     * Check the result in the project mode, for the file Mulinu.geo.
//     * 
//     * @throws Exception
//     */
//    public void testProject1() throws Exception {
//        double[][] result = null;
//        double[] globalparameters = INTERNAL_PARAMETERS;
//
//        HashMap<String, Number> parametersMap = new HashMap<String, Number>();
//        parametersMap.put(ProjectOptionalParameterCodes.MIN_DEPTH.getKey(), globalparameters[0]);
//        parametersMap.put(ProjectOptionalParameterCodes.MAX_JUNCTION.getKey(), globalparameters[1]);
//        parametersMap.put(ProjectOptionalParameterCodes.JMAX.getKey(), globalparameters[2]);
//        parametersMap.put(ProjectOptionalParameterCodes.ACCURACY.getKey(), globalparameters[3]);
//
//        parametersMap.put(ProjectTimeParameterCodes.STEP.getKey(), globalparameters[4]);
//        parametersMap.put(ProjectTimeParameterCodes.MINIMUM_TIME.getKey(), globalparameters[5]);
//        parametersMap.put(ProjectTimeParameterCodes.MAXIMUM_TIME.getKey(), globalparameters[6]);
//
//        parametersMap.put(ProjectOptionalParameterCodes.EPS.getKey(), globalparameters[7]);
//        parametersMap.put(ProjectOptionalParameterCodes.MIN_FILL_DEGREE.getKey(), globalparameters[8]);
//        parametersMap.put(ProjectOptionalParameterCodes.MIN_DISCHARGE.getKey(), globalparameters[9]);
//        parametersMap.put(ProjectOptionalParameterCodes.MAX_FILL_DEGREE.getKey(), globalparameters[10]);
//        parametersMap.put(ProjectOptionalParameterCodes.CELERITY_FACTOR.getKey(), globalparameters[11]);
//        parametersMap.put(ProjectOptionalParameterCodes.EXPONENT.getKey(), globalparameters[12]);
//        parametersMap.put(ProjectOptionalParameterCodes.TOLERANCE.getKey(), globalparameters[13]);
//        parametersMap.put(ProjectOptionalParameterCodes.C.getKey(), globalparameters[14]);
//        parametersMap.put(ProjectOptionalParameterCodes.GAMMA.getKey(), globalparameters[15]);
//        parametersMap.put(ProjectOptionalParameterCodes.INFLUX_EXP.getKey(), globalparameters[16]);
//
//        parametersMap.put(ProjectNeededParameterCodes.ALIGN.getKey(), align);
//        parametersMap.put(ProjectNeededParameterCodes.G.getKey(), g);
//        parametersMap.put(ProjectNeededParameterCodes.TAU.getKey(), tau);
//        parametersMap.put(ProjectNeededParameterCodes.A.getKey(), a);
//        parametersMap.put(ProjectNeededParameterCodes.N.getKey(), n);
//
//
//        OmsTrentoPProject trento_P = new OmsTrentoPProject();
//        URL diametersUrl = this.getClass().getClassLoader().getResource("diameters.csv");
//        DiametersReader diametersreader = new DiametersReader();
//        diametersreader.file = new File(diametersUrl.toURI()).getAbsolutePath();
//        diametersreader.pCols = 2;
//        diametersreader.pSeparator = "\\s+";
//        diametersreader.fileNovalue = "-9999.0";
//        diametersreader.readFile();
//        List<double[]> pipe = diametersreader.data;
//        // set parameters;
////        trento_P.pMode = 0; // project
////        trento_P.pA = a;
////        trento_P.pN = n;
////        trento_P.pTau = tau;
////        trento_P.pG = g;
////        trento_P.pAlign = align;
////        trento_P.pMinimumDepth = globalparameters[0];
////        trento_P.pMaxJunction = (int) globalparameters[1];
////        trento_P.pJMax = (int) globalparameters[2];
////        trento_P.tDTp = globalparameters[4];
////        trento_P.tpMin = globalparameters[5];
////        trento_P.tpMax = globalparameters[6];
////        trento_P.pEpsilon = globalparameters[7];
////        trento_P.pMinG = globalparameters[8];
////        trento_P.pMinDischarge = globalparameters[9];
////        trento_P.pMaxTheta = globalparameters[10];
////        trento_P.pCelerityFactor = globalparameters[11];
////        trento_P.pExponent = globalparameters[12];
////        trento_P.pTolerance = globalparameters[13];
////        trento_P.pC = globalparameters[14];
////        trento_P.pGamma = globalparameters[15];
////        trento_P.pEspInflux = globalparameters[16];
//        trento_P.inDiameters = pipe;
//        trento_P.pOutPipe = 16;
//        URL net = this.getClass().getClassLoader().getResource("TestTrentoP1.shp");
//        File netFile = new File(net.toURI());
//        OmsShapefileFeatureReader netReader = new OmsShapefileFeatureReader();
//        netReader.file = netFile.getAbsolutePath();
//        netReader.readFeatureCollection();
//        SimpleFeatureCollection netFC = netReader.geodata;
//        // set global parameters
//        // verify
//        // SimpleFeatureCollection netFC=Utility.readShp(netFile);
//        trento_P.inPipes = netFC;
//        trento_P.process();
//        result = trento_P.getResults();
//        checkMatrixEqual(result, HMTestMapstrentoP.project1, TOLL);
//
//    }
//
//    /**
//    * Test project 1, rectangular.
//    *
//    * Check the result in the project mode, for the file Mulinu.geo.
//    *
//    * @throws Exception
//    */
//    public void testProject1Rect() throws Exception {
//        double[][] result = null;
//        double[] globalparameters = INTERNAL_PARAMETERS;
//        OmsTrentoP trento_P = new OmsTrentoP();
//        // set parameters;
//        URL diametersUrl = this.getClass().getClassLoader().getResource("diameters.csv");
//        DiametersReader diametersreader = new DiametersReader();
//        diametersreader.file = new File(diametersUrl.toURI()).getAbsolutePath();
//        diametersreader.pCols = 2;
//        diametersreader.pSeparator = "\\s+";
//        diametersreader.fileNovalue = "-9999.0";
//        diametersreader.readFile();
//        List<double[]> pipe = diametersreader.data;
//        trento_P.pMode = 0; // project
//        trento_P.pA = a;
//        trento_P.pN = n;
//        trento_P.pTau = tau;
//        trento_P.pG = g;
//        trento_P.pAlign = align;
//        trento_P.pMinimumDepth = globalparameters[0];
//        trento_P.pMaxJunction = (int) globalparameters[1];
//        trento_P.pJMax = (int) globalparameters[2];
//        trento_P.pAccuracy = globalparameters[3];
//        trento_P.tDTp = globalparameters[4];
//        trento_P.tpMin = globalparameters[5];
//        trento_P.tpMax = globalparameters[6];
//        trento_P.pEpsilon = globalparameters[7];
//        trento_P.pMinG = globalparameters[8];
//        trento_P.pMinDischarge = globalparameters[9];
//        trento_P.pMaxTheta = globalparameters[10];
//        trento_P.pCelerityFactor = globalparameters[11];
//        trento_P.pExponent = globalparameters[12];
//        trento_P.pTolerance = globalparameters[13];
//        trento_P.pC = globalparameters[14];
//        trento_P.pGamma = globalparameters[15];
//        trento_P.pEspInflux = globalparameters[16];
//        trento_P.inDiameters = pipe;
//        trento_P.pOutPipe = 16;
//        URL net = this.getClass().getClassLoader().getResource("TestTrentoP1Rect.shp");
//        File netFile = new File(net.toURI());
//        OmsShapefileFeatureReader netReader = new OmsShapefileFeatureReader();
//        netReader.file = netFile.getAbsolutePath();
//        netReader.readFeatureCollection();
//        SimpleFeatureCollection netFC = netReader.geodata;
//        // set global parameters
//        // verify
//        // SimpleFeatureCollection netFC=Utility.readShp(netFile);
//        trento_P.inPipes = netFC;
//        trento_P.process();
//        result = trento_P.getResults();
//        checkMatrixEqual(result, HMTestMapstrentoP.project1Rectangular, TOLL);
//
//    }
//    /**
//    * Test project 1, rectangular.
//    *
//    * Check the result in the project mode, for the file Mulinu.geo.
//    *
//    * @throws Exception
//    */
//    public void testProject1Trap() throws Exception {
//        double[][] result = null;
//        double[] globalparameters = INTERNAL_PARAMETERS;
//        OmsTrentoP trento_P = new OmsTrentoP();
//        // set parameters;
//        URL diametersUrl = this.getClass().getClassLoader().getResource("diameters.csv");
//        DiametersReader diametersreader = new DiametersReader();
//        diametersreader.file = new File(diametersUrl.toURI()).getAbsolutePath();
//        diametersreader.pCols = 2;
//        diametersreader.pSeparator = "\\s+";
//        diametersreader.fileNovalue = "-9999.0";
//        diametersreader.readFile();
//        List<double[]> pipe = diametersreader.data;
//        trento_P.pMode = 0; // project
//        trento_P.pA = a;
//        trento_P.pN = n;
//        trento_P.pTau = tau;
//        trento_P.pG = g;
//        trento_P.pAlign = align;
//        trento_P.pMinimumDepth = globalparameters[0];
//        trento_P.pMaxJunction = (int) globalparameters[1];
//        trento_P.pJMax = (int) globalparameters[2];
//        trento_P.pAccuracy = globalparameters[3];
//        trento_P.tDTp = globalparameters[4];
//        trento_P.tpMin = globalparameters[5];
//        trento_P.tpMax = globalparameters[6];
//        trento_P.pEpsilon = globalparameters[7];
//        trento_P.pMinG = globalparameters[8];
//        trento_P.pMinDischarge = globalparameters[9];
//        trento_P.pMaxTheta = globalparameters[10];
//        trento_P.pCelerityFactor = globalparameters[11];
//        trento_P.pExponent = globalparameters[12];
//        trento_P.pTolerance = globalparameters[13];
//        trento_P.pC = globalparameters[14];
//        trento_P.pGamma = globalparameters[15];
//        trento_P.pEspInflux = globalparameters[16];
//        trento_P.inDiameters = pipe;
//        trento_P.pOutPipe = 16;
//        URL net = this.getClass().getClassLoader().getResource("TestTrentoP1Trap.shp");
//        File netFile = new File(net.toURI());
//        OmsShapefileFeatureReader netReader = new OmsShapefileFeatureReader();
//        netReader.file = netFile.getAbsolutePath();
//        netReader.readFeatureCollection();
//        SimpleFeatureCollection netFC = netReader.geodata;
//        // set global parameters
//        // verify
//        // SimpleFeatureCollection netFC=Utility.readShp(netFile);
//        trento_P.inPipes = netFC;
//        trento_P.process();
//        result = trento_P.getResults();
//        checkMatrixEqual(result, HMTestMapstrentoP.project1Trapezio, TOLL);
//
//    }
//
//    /**
//    * Test project 1 with align set to 1.
//    *
//    * Check the result in the project mode, for the file Mulinu.geo.
//    *
//    * @throws Exception
//    */
//    public void testProject1Align1() throws Exception {
//        double[][] result = null;
//        double[] globalparameters = INTERNAL_PARAMETERS;
//        URL diametersUrl = this.getClass().getClassLoader().getResource("diameters.csv");
//        DiametersReader diametersreader = new DiametersReader();
//        diametersreader.file = new File(diametersUrl.toURI()).getAbsolutePath();
//        diametersreader.pCols = 2;
//        diametersreader.pSeparator = "\\s+";
//        diametersreader.fileNovalue = "-9999.0";
//        diametersreader.readFile();
//        List<double[]> pipe = diametersreader.data;
//        OmsTrentoP trento_P = new OmsTrentoP();
//        // set parameters;
//        trento_P.pMode = 0; // project
//        trento_P.pA = a;
//        trento_P.pN = n;
//        trento_P.pTau = tau;
//        trento_P.pG = g;
//        trento_P.pAlign = 1;
//        trento_P.pMinimumDepth = globalparameters[0];
//        trento_P.pMaxJunction = (int) globalparameters[1];
//        trento_P.pJMax = (int) globalparameters[2];
//        trento_P.pAccuracy = globalparameters[3];
//        trento_P.tDTp = globalparameters[4];
//        trento_P.tpMin = globalparameters[5];
//        trento_P.tpMax = globalparameters[6];
//        trento_P.pEpsilon = globalparameters[7];
//        trento_P.pMinG = globalparameters[8];
//        trento_P.pMinDischarge = globalparameters[9];
//        trento_P.pMaxTheta = globalparameters[10];
//        trento_P.pCelerityFactor = globalparameters[11];
//        trento_P.pExponent = globalparameters[12];
//        trento_P.pTolerance = globalparameters[13];
//        trento_P.pC = globalparameters[14];
//        trento_P.pGamma = globalparameters[15];
//        trento_P.pEspInflux = globalparameters[16];
//        trento_P.inDiameters = pipe;
//        trento_P.pOutPipe = 16;
//        URL net = this.getClass().getClassLoader().getResource("TestTrentoP1.shp");
//        File netFile = new File(net.toURI());
//        OmsShapefileFeatureReader netReader = new OmsShapefileFeatureReader();
//        netReader.file = netFile.getAbsolutePath();
//        netReader.readFeatureCollection();
//        SimpleFeatureCollection netFC = netReader.geodata;
//        // set global parameters
//        // verify
//        // SimpleFeatureCollection netFC=Utility.readShp(netFile);
//        trento_P.inPipes = netFC;
//        trento_P.process();
//        result = trento_P.getResults();
//        checkMatrixEqual(result, HMTestMapstrentoP.project1align1, TOLL);
//
//    }
//
//    /**
//    *
//    * Check if the models work well in verify mode..
//    *
//    * @throws Exception
//    *
//    * @throws IOException
//    */
//    public void testVerify1() throws Exception {
//
//        URL rainUrl = this.getClass().getClassLoader().getResource("rain_trentop.csv"); //$NON-NLS-1$
//        File rainFile = new File(rainUrl.toURI());
//
//        double[][] result = null;
//        double[] globalparameters = INTERNAL_PARAMETERS;
//
//        OmsTrentoP trento_P = new OmsTrentoP();
//        // set parameters;
//        trento_P.pMode = 1; // verify
//        trento_P.pMaxJunction = (int) globalparameters[1];
//        trento_P.pJMax = (int) globalparameters[2];
//        trento_P.pAccuracy = globalparameters[3];
//        trento_P.tpMax = globalparameters[6];
//        trento_P.pEpsilon = globalparameters[7];
//        trento_P.pMaxTheta = 6.28;
//        trento_P.pCelerityFactor = Constants.DEFAULT_CELERITY_FACTOR;
//        trento_P.pExponent = globalparameters[12];
//        trento_P.pTolerance = 0.01;
//        trento_P.pGamma = globalparameters[15];
//        trento_P.pEspInflux = globalparameters[16];
//        trento_P.pOutPipe = 16;
//        OmsTimeSeriesReader rainReader = new OmsTimeSeriesReader();
//        rainReader.fileNovalue = "-9999";
//        rainReader.file = rainFile.getAbsolutePath();
//        rainReader.read();
//        rainReader.close();
//        trento_P.inRain = rainReader.outData;
//        // set global parameters.
//        URL net = this.getClass().getClassLoader().getResource("TestTrentoP1Verifica.shp");
//        File netFile = new File(net.toURI());
//        OmsShapefileFeatureReader netReader = new OmsShapefileFeatureReader();
//        netReader.file = netFile.getAbsolutePath();
//        netReader.readFeatureCollection();
//        SimpleFeatureCollection netFC = netReader.geodata;
//        // set global parameters
//        // verify
//        trento_P.inPipes = netFC;
//        trento_P.process();
//        result = hashToMatrix(trento_P.outDischarge, trento_P.inRain, trento_P.getResults().length);
//        checkMatrixEqual(result, HMTestMapstrentoP.verify1, TOLL);
//
//    }
//
//    // /**
//    // * Test project 2.
//    // *
//    // * Check the result in the project mode, for the file Mulinu.geo.
//    // *
//    // * @throws Exception
//    // */
//    // public void testProject2() throws Exception {
//    // double[][] result = null;
//    // double[] globalparameters2 = INTERNAL_PARAMETERS2;
//    // OmsTrentoP trento_P = new OmsTrentoP();
//    // // set parameters;
//    // URL diametersUrl = this.getClass().getClassLoader().getResource("diameters.csv");
//    // DiametersReader diametersreader = new DiametersReader();
//    // diametersreader.file = new File(diametersUrl.toURI()).getAbsolutePath();
//    // diametersreader.pCols = 2;
//    // diametersreader.pSeparator = "\\s+";
//    // diametersreader.fileNovalue = "-9999.0";
//    // diametersreader.readFile();
//    // List<double[]> pipe = diametersreader.data;
//    // trento_P.pMode = 0; // project
//    // trento_P.pA = 60.5;
//    // trento_P.pN = 0.64;
//    // trento_P.pTau = 2.6;
//    // trento_P.pG = g;
//    // trento_P.pAlign = 1;
//    // trento_P.pMinimumDepth = globalparameters2[0];
//    // trento_P.pMaxJunction = (int) globalparameters2[1];
//    // trento_P.pJMax = (int) globalparameters2[2];
//    // trento_P.pAccuracy = globalparameters2[3];
//    // trento_P.tDTp = globalparameters2[4];
//    // trento_P.tpMin = globalparameters2[5];
//    // trento_P.tpMax = globalparameters2[6];
//    // trento_P.pEpsilon = globalparameters2[7];
//    // trento_P.pMinG = globalparameters2[8];
//    // trento_P.pMinDischarge = globalparameters2[9];
//    // trento_P.pMaxTheta = globalparameters2[10];
//    // trento_P.pCelerityFactor = globalparameters2[11];
//    // trento_P.pExponent = globalparameters2[12];
//    // trento_P.pTolerance = globalparameters2[13];
//    // trento_P.pC = globalparameters2[14];
//    // trento_P.pGamma = globalparameters2[15];
//    // trento_P.pEspInflux = globalparameters2[16];
//    // trento_P.inDiameters = pipe;
//    // trento_P.pOutPipe = 38;
//    // URL net = this.getClass().getClassLoader().getResource("TestTrentoP2Verifica.shp");
//    // File netFile = new File(net.toURI());
//    // OmsShapefileFeatureReader netReader = new OmsShapefileFeatureReader();
//    // netReader.file = netFile.getAbsolutePath();
//    // netReader.readFeatureCollection();
//    // SimpleFeatureCollection netFC = netReader.geodata;
//    // // set global parameters
//    // // verify
//    // // SimpleFeatureCollection netFC=Utility.readShp(netFile);
//    // trento_P.inPipes = netFC;
//    // trento_P.process();
//    // result = trento_P.getResults();
//    // checkMatrixEqual(result, HMTestMapstrentoP.projectTrentoP2, TOLL);
//    //
//    // }
//    private double[][] hashToMatrix( HashMap<DateTime, HashMap<Integer, double[]>> outDischarge,
//            HashMap<DateTime, double[]> inRain, int nStation ) {
//        // create the rains array from the input.
//        Set<Entry<DateTime, HashMap<Integer, double[]>>> dischargeSet = outDischarge.entrySet();
//        DateTime first = null;
//        DateTime second = null;
//        int l = outDischarge.size();
//
//        double[][] rainData = new double[l][nStation + 1];
//        int index = 0;
//        int dt = 0;
//        int n = outDischarge.size() - 1;
//        for( Entry<DateTime, HashMap<Integer, double[]>> dischargeRecord : dischargeSet ) {
//
//            DateTime dateTime = dischargeRecord.getKey();
//            HashMap<Integer, double[]> values = dischargeRecord.getValue();
//            if (first == null) {
//                first = dateTime;
//                rainData[index][0] = 1;
//                Set<Integer> tmp = values.keySet();
//                int i = 0;
//                for( Integer f : tmp ) {
//                    rainData[index][i + 1] = values.get(f)[0];
//                    i++;
//                }
//
//            } else if (second == null) {
//                second = dateTime;
//                dt = Math.abs(second.getMinuteOfDay() - first.getMinuteOfDay());
//                rainData[index][0] = rainData[index - 1][0] + dt;
//                Set<Integer> tmp = values.keySet();
//                int i = 0;
//                for( Integer f : tmp ) {
//                    rainData[index][i + 1] = values.get(f)[0];
//                    i++;
//                }
//
//            } else {
//                rainData[index][0] = rainData[index - 1][0] + dt;
//                int i = 0;
//                Set<Integer> tmp = values.keySet();
//                for( Integer f : tmp ) {
//                    rainData[index][i + 1] = values.get(f)[0];
//                    i++;
//                }
//            }
//            index++;
//        }
//        return rainData;
//    }
//}
