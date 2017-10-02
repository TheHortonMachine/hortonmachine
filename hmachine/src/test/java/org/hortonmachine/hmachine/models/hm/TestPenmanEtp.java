package org.hortonmachine.hmachine.models.hm;
//package org.hortonmachine.hmachine.models.hm;
//
//import java.io.File;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.util.HashMap;
//
//import org.hortonmachine.gears.JGrassGears;
//import org.hortonmachine.gears.io.adige.VegetationLibraryReader;
//import org.hortonmachine.gears.io.adige.VegetationLibraryRecord;
//import org.hortonmachine.gears.io.timedependent.TimeseriesByStepReaderId2Value;
//import org.hortonmachine.gears.io.timedependent.TimeseriesByStepWriterId2Value;
//import org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPenmanEtp;
//import org.hortonmachine.hmachine.utils.HMTestCase;
///**
// * Test Penman Evapotranspiration..
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//@SuppressWarnings("nls")
//public class TestPenmanEtp extends HMTestCase {
//
//    public void testPenmanEtp() throws Exception {
//
//        // PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        // URL rainUrl = this.getClass().getClassLoader().getResource("etp_in_data_rain.csv");
//
//        URL tempUrl = this.getClass().getClassLoader().getResource("etp_in_data_temperature.csv");
//        URL windUrl = this.getClass().getClassLoader().getResource("etp_in_data_windspeed.csv");
//        URL pressureUrl = this.getClass().getClassLoader().getResource("etp_in_data_pressure.csv");
//        URL humidityUrl = this.getClass().getClassLoader().getResource("etp_in_data_humidity.csv");
//
//        URL vegetationUrl = JGrassGears.class.getClassLoader().getResource("vegetation.csv");
//
//        URL netradiationUrl = this.getClass().getClassLoader().getResource("etp_in_netradiation.csv");
//        URL shortwaveradiationUrl = this.getClass().getClassLoader().getResource("etp_in_shortwaveradiation.csv");
//        URL sweUrl = this.getClass().getClassLoader().getResource("etp_in_swe.csv");
//
//        File tempFile = new File(tempUrl.toURI());
//        File outputFile = new File(tempFile.getParentFile(), "etp_out.csv");
//        outputFile = classesTestFile2srcTestResourcesFile(outputFile);
//
//        TimeseriesByStepReaderId2Value tempReader = getReader(tempUrl);
//        TimeseriesByStepReaderId2Value windReader = getReader(windUrl);
//        TimeseriesByStepReaderId2Value pressReader = getReader(pressureUrl);
//        TimeseriesByStepReaderId2Value humReader = getReader(humidityUrl);
//        TimeseriesByStepReaderId2Value netradReader = getReader(netradiationUrl);
//        TimeseriesByStepReaderId2Value shortradReader = getReader(shortwaveradiationUrl);
//        TimeseriesByStepReaderId2Value sweReader = getReader(sweUrl);
//
//        VegetationLibraryReader vegetationReader = new VegetationLibraryReader();
//        vegetationReader.file = new File(vegetationUrl.toURI()).getAbsolutePath();;
//        vegetationReader.read();
//        HashMap<Integer, VegetationLibraryRecord> vegetationData = vegetationReader.data;
//        vegetationReader.close();
//
//        OmsPenmanEtp penmanEtp = new OmsPenmanEtp();
//        penmanEtp.inVegetation = vegetationData;
//
//        TimeseriesByStepWriterId2Value etpWriter = new TimeseriesByStepWriterId2Value();
//        etpWriter.file = outputFile.getAbsolutePath();
//        etpWriter.tStart = tempReader.tStart;
//        etpWriter.tTimestep = tempReader.tTimestep;
//
//        while( tempReader.doProcess ) {
//            tempReader.nextRecord();
//
//            penmanEtp.tCurrent = tempReader.tCurrent;
//
//            tempReader.nextRecord();
//            HashMap<Integer, double[]> id2ValueMap = tempReader.data;
//            penmanEtp.inTemp = id2ValueMap;
//
//            windReader.nextRecord();
//            id2ValueMap = windReader.data;
//            penmanEtp.inWind = id2ValueMap;
//
//            pressReader.nextRecord();
//            id2ValueMap = pressReader.data;
//            penmanEtp.inPressure = id2ValueMap;
//
//            humReader.nextRecord();
//            id2ValueMap = humReader.data;
//            penmanEtp.inRh = id2ValueMap;
//
//            netradReader.nextRecord();
//            id2ValueMap = netradReader.data;
//            penmanEtp.inNetradiation = id2ValueMap;
//
//            shortradReader.nextRecord();
//            id2ValueMap = shortradReader.data;
//            penmanEtp.inShortradiation = id2ValueMap;
//
//            sweReader.nextRecord();
//            id2ValueMap = sweReader.data;
//            penmanEtp.inSwe = id2ValueMap;
//
//            penmanEtp.penman();
//
//            HashMap<Integer, double[]> outEtp = penmanEtp.outEtp;
//
//            etpWriter.data = outEtp;
//            etpWriter.writeNextLine();
//        }
//
//        tempReader.close();
//        windReader.close();
//        pressReader.close();
//        humReader.close();
//        netradReader.close();
//        shortradReader.close();
//        sweReader.close();
//
//        etpWriter.close();
//
//    }
//
//    private TimeseriesByStepReaderId2Value getReader( URL fileUrl ) throws URISyntaxException {
//        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
//        reader.file = new File(fileUrl.toURI()).getAbsolutePath();
//        reader.idfield = "ID";
//        reader.tStart = "2000-01-01 00:00";
//        reader.tTimestep = 60;
//        // reader.tEnd = "2000-01-01 00:00";
//        reader.fileNovalue = "-9999";
//        reader.initProcess();
//        return reader;
//    }
//
//}
