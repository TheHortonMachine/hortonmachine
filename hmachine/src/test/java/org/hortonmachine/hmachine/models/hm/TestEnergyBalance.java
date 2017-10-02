package org.hortonmachine.hmachine.models.hm;
//package org.hortonmachine.hmachine.models.hm;
//
//import java.io.File;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.List;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.io.eicalculator.EIAreas;
//import org.hortonmachine.gears.io.eicalculator.OmsEIAreasReader;
//import org.hortonmachine.gears.io.eicalculator.EIEnergy;
//import org.hortonmachine.gears.io.eicalculator.OmsEIEnergyReader;
//import org.hortonmachine.gears.io.generic.OmsId2ValueArrayReader;
//import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
//import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
//import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
//import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.hortonmachine.hmachine.modules.hydrogeomorphology.energybalance.OmsEnergyBalance;
//import org.hortonmachine.hmachine.utils.HMTestCase;
///**
// * Test OmsEnergyBalance.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEnergyBalance extends HMTestCase {
//
//    public void testEnergyBalance() throws Exception {
//
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
//
//        URL areasUrl = this.getClass().getClassLoader().getResource("eicalculator_out_areas.csv");
//        URL energyUrl = this.getClass().getClassLoader().getResource("eicalculator_out_energy.csv");
//
//        URL rainUrl = this.getClass().getClassLoader().getResource("energybalance_in_data_rain.csv");
//
//        URL tempUrl = this.getClass().getClassLoader().getResource("energybalance_in_data_temperature.csv");
//        URL windUrl = this.getClass().getClassLoader().getResource("energybalance_in_data_windspeed.csv");
//        URL pressureUrl = this.getClass().getClassLoader().getResource("energybalance_in_data_pressure.csv");
//        URL humidityUrl = this.getClass().getClassLoader().getResource("energybalance_in_data_humidity.csv");
//        URL dtdayUrl = this.getClass().getClassLoader().getResource("energybalance_in_data_dtday.csv");
//        URL dtmonthUrl = this.getClass().getClassLoader().getResource("energybalance_in_data_dtmonth.csv");
//
//        URL basinsUrl = this.getClass().getClassLoader().getResource("jami_in_basins.shp");
//
//        OmsEIAreasReader areas = new OmsEIAreasReader();
//        File areaFile = new File(areasUrl.toURI());
//        areas.file = areaFile.getAbsolutePath();
//        areas.pSeparator = "\\s+";
//        areas.pm = pm;
//        areas.read();
//        List<EIAreas> areasList = areas.outAreas;
//        areas.close();
//
//        OmsEIEnergyReader energy = new OmsEIEnergyReader();
//        energy.file = new File(energyUrl.toURI()).getAbsolutePath();
//        energy.pSeparator = "\\s+";
//        energy.pm = pm;
//        energy.read();
//        List<EIEnergy> energyList = energy.outEnergy;
//        energy.close();
//
//        OmsShapefileFeatureReader basinsReader = new OmsShapefileFeatureReader();
//        basinsReader.file = new File(basinsUrl.toURI()).getAbsolutePath();
//        basinsReader.readFeatureCollection();
//        SimpleFeatureCollection basinsFC = basinsReader.geodata;
//
//        OmsTimeSeriesIteratorReader rainReader = new OmsTimeSeriesIteratorReader();
//        rainReader.file = new File(rainUrl.toURI()).getAbsolutePath();
//        rainReader.idfield = "ID";
//        rainReader.tStart = "2000-01-01 00:00";
//        rainReader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        rainReader.fileNovalue = "-9999";
//        rainReader.initProcess();
//
//        OmsId2ValueArrayReader tempReader = new OmsId2ValueArrayReader();
//        tempReader.file = new File(tempUrl.toURI()).getAbsolutePath();
//        tempReader.pCols = 5;
//        tempReader.pSeparator = "\\s+";
//        tempReader.fileNovalue = "-9999.0";
//
//        OmsId2ValueArrayReader pressureReader = new OmsId2ValueArrayReader();
//        pressureReader.file = new File(pressureUrl.toURI()).getAbsolutePath();
//        pressureReader.pCols = 5;
//        pressureReader.pSeparator = "\\s+";
//        pressureReader.fileNovalue = "-9999.0";
//
//        OmsId2ValueArrayReader humidityReader = new OmsId2ValueArrayReader();
//        humidityReader.file = new File(humidityUrl.toURI()).getAbsolutePath();
//        humidityReader.pCols = 5;
//        humidityReader.pSeparator = "\\s+";
//        humidityReader.fileNovalue = "-9999.0";
//
//        OmsId2ValueArrayReader windReader = new OmsId2ValueArrayReader();
//        windReader.file = new File(windUrl.toURI()).getAbsolutePath();
//        windReader.pCols = 5;
//        windReader.pSeparator = "\\s+";
//        windReader.fileNovalue = "-9999.0";
//
//        OmsId2ValueArrayReader dtdayReader = new OmsId2ValueArrayReader();
//        dtdayReader.file = new File(dtdayUrl.toURI()).getAbsolutePath();
//        dtdayReader.pCols = 5;
//        dtdayReader.pSeparator = "\\s+";
//        dtdayReader.fileNovalue = "-9999.0";
//
//        OmsId2ValueArrayReader dtmonthReader = new OmsId2ValueArrayReader();
//        dtmonthReader.file = new File(dtmonthUrl.toURI()).getAbsolutePath();
//        dtmonthReader.pCols = 5;
//        dtmonthReader.pSeparator = "\\s+";
//        dtmonthReader.fileNovalue = "-9999.0";
//
//        OmsEnergyBalance energyBalance = new OmsEnergyBalance();
//        energyBalance.pm = pm;
//        energyBalance.inBasins = basinsFC;
//        energyBalance.inEnergy = energyList;
//        energyBalance.inAreas = areasList;
//        energyBalance.fBasinid = "netnum";
//        energyBalance.fBasinlandcover = "uso_reclas";
//        energyBalance.pInitswe = 500;
//        energyBalance.pGlacierid = 15;
//        // energyBalance.pSnowrefv = 0.95;
//        energyBalance.tTimestep = rainReader.tTimestep;
//
//        File pnetFile = new File(areaFile.getParentFile(), "energybalance_out_pnet.csv");
//        pnetFile = classesTestFile2srcTestResourcesFile(pnetFile);
//        OmsTimeSeriesIteratorWriter pnetWriter = new OmsTimeSeriesIteratorWriter();
//        pnetWriter.file = pnetFile.getAbsolutePath();
//        pnetWriter.tStart = rainReader.tStart;
//        pnetWriter.tTimestep = rainReader.tTimestep;
//
//        while( rainReader.doProcess ) {
//            rainReader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = rainReader.outFolder;
//            energyBalance.inRain = id2ValueMap;
//
//            tempReader.readNextLine();
//            HashMap<Integer, double[]> id2ValueArrayMap = tempReader.data;
//            energyBalance.inTemp = id2ValueArrayMap;
//
//            windReader.readNextLine();
//            id2ValueArrayMap = windReader.data;
//            energyBalance.inWind = id2ValueArrayMap;
//
//            pressureReader.readNextLine();
//            id2ValueArrayMap = pressureReader.data;
//            energyBalance.inPressure = id2ValueArrayMap;
//
//            humidityReader.readNextLine();
//            id2ValueArrayMap = humidityReader.data;
//            energyBalance.inRh = id2ValueArrayMap;
//
//            dtdayReader.readNextLine();
//            id2ValueArrayMap = dtdayReader.data;
//            energyBalance.inDtday = id2ValueArrayMap;
//
//            dtmonthReader.readNextLine();
//            id2ValueArrayMap = dtmonthReader.data;
//            energyBalance.inDtmonth = id2ValueArrayMap;
//
//            energyBalance.tCurrent = rainReader.tCurrent;
//            energyBalance.process();
//
//            HashMap<Integer, double[]> pnet = energyBalance.outPnet;
//            HashMap<Integer, double[]> prain = energyBalance.outPrain;
//            HashMap<Integer, double[]> psnow = energyBalance.outPsnow;
//
//            pnetWriter.inData = prain;
//            pnetWriter.writeNextLine();
//
//        }
//        rainReader.close();
//
//        tempReader.close();
//        pressureReader.close();
//        windReader.close();
//        humidityReader.close();
//        dtdayReader.close();
//        dtmonthReader.close();
//
//        pnetWriter.close();
//
//        // double[] result1221 = new double[]{9.925938910342381, 8.1, 7.561630032493953,
//        // 5.86663426553045, 3.4220912542795046};
//        //
//        // for( int i = 0; i < result1221.length; i++ ) {
//        // assertEquals(result1221[i], values1221[i], 0.0001);
//        // }
//        
//        if (!pnetFile.delete()) {
//            pnetFile.deleteOnExit();
//        }
//
//    }
//
//    private double[] expectedPnet = new double[]{699.0, 0.0, 1065.0, 0.0, 1210.0, 0.0, 762.0, 0.0, 626.0, 0.0, 623.0, 0.0, 616.0,
//            0.0, 944.0, 0.0, 1209.0, 0.0, 1039.0, 0.0, 624.0, 0.0, 1097.0, 0.0, 1212.0, 0.0, 1263.0, 0.0, 629.0, 0.0, 1104.0,
//            0.0, 621.0, 0.0, 1179.0, 0.0, 1042.0, 0.0, 613.0, 0.0, 946.0, 0.0, 693.0, 0.0, 763.0, 0.0, 769.0, 0.0, 1292.0, 0.0,
//            618.0, 0.0, 1068.0, 0.0, 615.0, 0.0, 630.0, 0.0, 961.0, 0.0, 617.0, 0.0, 627.0, 0.0, 625.0, 0.0, 1211.0, 0.0, 1221.0,
//            0.0, 1064.0, 0.0, 619.0, 0.0, 1041.0, 0.0, 1202.0, 0.0, 841.0, 0.0, 1040.0, 0.0, 1069.0, 0.0, 1260.0, 0.0, 1242.0,
//            0.0, 622.0, 0.0, 1291.0, 0.0, 1070.0, 0.0, 620.0, 0.0, 1075.0, 0.0, 1066.0, 0.0, 628.0, 0.0, 1105.0, 0.0, 945.0, 0.0,
//            614.0, 0.0, 970.0, 0.0};
//    private double[] expectedPrain = new double[]{699.0, 0.1454180460051585, 1065.0, 0.14999999760693658, 1210.0,
//            0.23190963611777973, 762.0, 0.2972437781933401, 626.0, 0.14999999500822495, 623.0, 0.14999999564750602, 616.0,
//            0.29974445956392226, 944.0, 0.24999999116857943, 1209.0, 0.20843756700693264, 1039.0, 0.2046855716152388, 624.0,
//            0.14997390897836843, 1097.0, 0.14993486689479696, 1212.0, 0.24256385305908343, 1263.0, 0.2442336421296105, 629.0,
//            0.14999999820645707, 1104.0, 0.14999999806845538, 621.0, 0.14999999440686618, 1179.0, 0.14997921950545054, 1042.0,
//            0.15008797554452888, 613.0, 0.29764065702174186, 946.0, 0.29999999713668396, 693.0, 0.285995906054635, 763.0,
//            0.2999999966009149, 769.0, 0.14982853690272288, 1292.0, 0.14999999525968377, 618.0, 0.2999057459276908, 1068.0,
//            0.2063624744985185, 615.0, 0.29955312661787636, 630.0, 0.15003825036919352, 961.0, 0.14999999555508045, 617.0,
//            0.249999988359043, 627.0, 0.14994771192193923, 625.0, 0.1499999952331639, 1211.0, 0.25384614779184755, 1221.0,
//            0.14999999615251092, 1064.0, 0.14999999927172133, 619.0, 0.1499999985664037, 1041.0, 0.14773406320776558, 1202.0,
//            0.14997890273328196, 841.0, 0.2999479041662106, 1040.0, 0.1500000009061293, 1069.0, 0.2499999928695707, 1260.0,
//            0.14999999950137963, 1242.0, 0.15000000222012597, 622.0, 0.14999999497489233, 1291.0, 0.1499748491180227, 1070.0,
//            0.24999999222422858, 620.0, 0.14997830946474452, 1075.0, 0.14994904177153595, 1066.0, 0.15020407797302815, 628.0,
//            0.1499999975559162, 1105.0, 0.1499805447102739, 945.0, 0.24942877310354855, 614.0, 0.2760015917709732, 970.0,
//            0.22512322172103927};
//    private double[] expectedPSnow = new double[]{699.0, 0.004581945735074934, 1065.0, 0.0, 1210.0, 0.018025491089557658, 762.0,
//            0.0027562177310753466, 626.0, 0.0, 623.0, 0.0, 616.0, 0.0, 944.0, 0.0, 1209.0, 0.0415015727199312, 1039.0,
//            0.04531442069636117, 624.0, 0.0, 1097.0, 0.0, 1212.0, 0.007436140804802122, 1263.0, 0.005766353985273886, 629.0, 0.0,
//            1104.0, 0.0, 621.0, 0.0, 1179.0, 0.0, 1042.0, 0.0, 613.0, 0.0023593381768641287, 946.0, 0.0, 693.0,
//            0.013924429209936248, 763.0, 0.0, 769.0, 1.714554984036082E-4, 1292.0, 0.0, 618.0, 0.0, 1068.0, 0.04363751926076958,
//            615.0, 4.468691949822926E-4, 630.0, 0.0, 961.0, 0.0, 617.0, 0.0, 627.0, 0.0, 625.0, 0.0, 1211.0, 0.0, 1221.0, 0.0,
//            1064.0, 0.0, 619.0, 0.0, 1041.0, 0.0022773621234270292, 1202.0, 0.0, 841.0, 0.0, 1040.0, 0.0, 1069.0, 0.0, 1260.0,
//            0.0, 1242.0, 0.0, 622.0, 0.0, 1291.0, 0.0, 1070.0, 0.0, 620.0, 0.0, 1075.0, 0.0, 1066.0, 0.0, 628.0, 0.0, 1105.0,
//            0.0, 945.0, 5.712168815597725E-4, 614.0, 0.023848027492350717, 970.0, 0.02487676985069746};
//
//}
