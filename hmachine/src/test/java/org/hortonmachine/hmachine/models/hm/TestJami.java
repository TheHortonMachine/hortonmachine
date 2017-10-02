package org.hortonmachine.hmachine.models.hm;
//package org.hortonmachine.hmachine.models.hm;
//
//import java.io.File;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.io.eicalculator.EIAltimetry;
//import org.hortonmachine.gears.io.eicalculator.OmsEIAltimetryReader;
//import org.hortonmachine.gears.io.eicalculator.EIAreas;
//import org.hortonmachine.gears.io.eicalculator.OmsEIAreasReader;
//import org.hortonmachine.gears.io.generic.OmsId2ValueArrayWriter;
//import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
//import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
//import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
//import org.hortonmachine.gears.libs.modules.HMConstants;
//import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.hortonmachine.hmachine.modules.statistics.jami.OmsJami;
//import org.hortonmachine.hmachine.utils.HMTestCase;
//import org.joda.time.DateTime;
//import org.joda.time.format.DateTimeFormatter;
///**
// * Test jami.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestJami extends HMTestCase {
//
//    public void testJami() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
//
//        URL altimUrl = this.getClass().getClassLoader().getResource("eicalculator_out_altimetry.csv");
//        File altimetryFile = new File(altimUrl.toURI());
//
//        URL areasUrl = this.getClass().getClassLoader().getResource("eicalculator_out_areas.csv");
//        File areasFile = new File(areasUrl.toURI());
//
//        File outputFileForEtp = new File(altimetryFile.getParentFile(), "etp_in_temp.csv");
//        outputFileForEtp = classesTestFile2srcTestResourcesFile(outputFileForEtp);
//
//        File outputFile = new File(altimetryFile.getParentFile(), "jami_out_temp.csv");
//        outputFile = classesTestFile2srcTestResourcesFile(outputFile);
//
//        URL stationsUrl = this.getClass().getClassLoader().getResource("jami_in_stations.shp");
//        URL stationdataUrl = this.getClass().getClassLoader().getResource("jami_new_temp.csv");
//        File stationDataFile = new File(stationdataUrl.toURI());
//        URL basinsUrl = this.getClass().getClassLoader().getResource("jami_in_basins.shp");
//
//        OmsEIAltimetryReader altim = new OmsEIAltimetryReader();
//        altim.file = altimetryFile.getAbsolutePath();
//        altim.pSeparator = "\\s+";
//        altim.pm = pm;
//        altim.read();
//        List<EIAltimetry> altimList = altim.outAltimetry;
//        altim.close();
//
//        OmsEIAreasReader areas = new OmsEIAreasReader();
//        areas.file = areasFile.getAbsolutePath();
//        areas.pSeparator = "\\s+";
//        areas.pm = pm;
//        areas.read();
//        List<EIAreas> areasList = areas.outAreas;
//        areas.close();
//
//        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
//        stationsReader.file = new File(stationsUrl.toURI()).getAbsolutePath();
//        stationsReader.readFeatureCollection();
//        SimpleFeatureCollection stationsFC = stationsReader.geodata;
//
//        OmsShapefileFeatureReader basinsReader = new OmsShapefileFeatureReader();
//        basinsReader.file = new File(basinsUrl.toURI()).getAbsolutePath();
//        basinsReader.readFeatureCollection();
//        SimpleFeatureCollection basinsFC = basinsReader.geodata;
//
//        OmsTimeSeriesIteratorReader dataReader = new OmsTimeSeriesIteratorReader();
//        dataReader.file = stationDataFile.getAbsolutePath();
//        dataReader.fileNovalue = "-9999";
//        dataReader.idfield = "ID";
//        dataReader.tStart = "2005-05-01 00:00";
//        dataReader.tTimestep = 60;
//        dataReader.tEnd = "2005-05-01 03:00";
//
//        dataReader.initProcess();
//
//        OmsJami jami = new OmsJami();
//        jami.pm = pm;
//        jami.inAltimetry = altimList;
//        jami.inAreas = areasList;
//        jami.fStationid = "id_punti_m";
//        jami.fStationelev = "quota";
//        jami.fBasinid = "netnum";
//        jami.pNum = 2;
//        jami.pBins = 4;
//        jami.pType = HMConstants.DTDAY;
//        jami.inStations = stationsFC;
//        jami.inInterpolate = basinsFC;
//
//        OmsId2ValueArrayWriter writer = new OmsId2ValueArrayWriter();
//        writer.file = outputFile.getAbsolutePath();
//        writer.pSeparator = " ";
//        writer.fileNovalue = "-9999.0";
//
//        OmsTimeSeriesIteratorWriter tsWriter = new OmsTimeSeriesIteratorWriter();
//        tsWriter.file = outputFileForEtp.getAbsolutePath();
//        tsWriter.tStart = dataReader.tStart;
//        tsWriter.tTimestep = dataReader.tTimestep;
//
//        DateTimeFormatter dF = HMConstants.utcDateFormatterYYYYMMDDHHMM;
//        // 1 5 2005 - 1 6 2005
//        // 30 min
//        while( dataReader.doProcess ) {
//            dataReader.nextRecord();
//            DateTime runningDate = dF.parseDateTime(dataReader.tCurrent);
//            HashMap<Integer, double[]> id2ValueMap = dataReader.outFolder;
//            jami.inMeteo = id2ValueMap;
//            jami.tCurrent = runningDate.toString(dF);
//
//            jami.process();
//
//            HashMap<Integer, double[]> interpolationPointId2MeteoDataMapBands = jami.outInterpolatedBand;
//            HashMap<Integer, double[]> interpolationPointId2MeteoDataMap = jami.outInterpolated;
//
//            // Set<Entry<Integer, double[]>> entrySet =
//            // interpolationPointId2MeteoDataMapBands.entrySet();
//            // for( Entry<Integer, double[]> entry : entrySet ) {
//            // Integer basinId = entry.getKey();
//            // double[] valuePerBand = entry.getValue();
//            // double value = interpolationPointId2MeteoDataMap.get(basinId)[0];
//            // System.out.println("basin: " + basinId);
//            // System.out.print("per band: ");
//            // for( double bandValue : valuePerBand ) {
//            // System.out.print(" " + bandValue);
//            // }
//            // System.out.println("per band: ");
//            // System.out.println("interpolated on basin: " + value);
//            // System.out.println("****************************");
//            // }
//
//            writer.data = interpolationPointId2MeteoDataMapBands;
//            writer.writeNextLine();
//
//            tsWriter.inData = interpolationPointId2MeteoDataMap;
//            tsWriter.writeNextLine();
//
//            runningDate = runningDate.plusMinutes(30);
//        }
//
//        dataReader.close();
//        writer.close();
//        tsWriter.close();
//
//        // for now there is no writer... waiting for the apposite module
//
//        // double[] result1221 = new double[]{9.925938910342381, 8.1, 7.561630032493953,
//        // 5.86663426553045, 3.4220912542795046};
//        //
//        // for( int i = 0; i < result1221.length; i++ ) {
//        // assertEquals(result1221[i], values1221[i], 0.0001);
//        // }
//        if (!outputFileForEtp.delete()) {
//            outputFileForEtp.deleteOnExit();
//        }
//    }
//}
