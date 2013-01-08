//package org.jgrasstools.gears.modules;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.util.List;
//
//import org.jgrasstools.gears.io.eicalculator.EIAltimetry;
//import org.jgrasstools.gears.io.eicalculator.OmsEIAltimetryReader;
//import org.jgrasstools.gears.io.eicalculator.OmsEIAltimetryWriter;
//import org.jgrasstools.gears.io.eicalculator.EIAreas;
//import org.jgrasstools.gears.io.eicalculator.OmsEIAreasReader;
//import org.jgrasstools.gears.io.eicalculator.OmsEIAreasWriter;
//import org.jgrasstools.gears.io.eicalculator.EIEnergy;
//import org.jgrasstools.gears.io.eicalculator.OmsEIEnergyReader;
//import org.jgrasstools.gears.io.eicalculator.OmsEIEnergyWriter;
//import org.jgrasstools.gears.utils.HMTestCase;
///**
// * Test EiCalculatorInputOutput .
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEiCalculatorInputOutput extends HMTestCase {
//
//    public void testEiCalculatorInputOutput() throws Exception {
//        URL altimUrl = this.getClass().getClassLoader().getResource("eicalculator_out_altimetry.csv");
//        OmsEIAltimetryReader altimReader = new OmsEIAltimetryReader();
//        altimReader.file = new File(altimUrl.toURI()).getAbsolutePath();
//        altimReader.pSeparator = "\\s+";
//        altimReader.read();
//        altimReader.close();
//        List<EIAltimetry> altimetry = altimReader.outAltimetry;
//        assertTrue(altimetry.size() == 2940);
//
//        // write file
//        File tmpFile = File.createTempFile("altim", ".csv");
//        String altimTmpFile = tmpFile.getAbsolutePath();
//        OmsEIAltimetryWriter altimWriter = new OmsEIAltimetryWriter();
//        altimWriter.file = altimTmpFile;
//        altimWriter.inAltimetry = altimetry;
//        altimWriter.pSeparator = " ";
//        altimWriter.write();
//        altimWriter.close();
//        OmsEIAltimetryReader altimTmpReader = new OmsEIAltimetryReader();
//        altimTmpReader.file = altimTmpFile;
//        altimTmpReader.pSeparator = "\\s+";
//        altimTmpReader.read();
//        altimTmpReader.close();
//        List<EIAltimetry> altimetryTmp = altimTmpReader.outAltimetry;
//        for( int i = 0; i < altimetry.size(); i++ ) {
//            assertEquals(altimetry.get(i).basinId, altimetryTmp.get(i).basinId);
//        }
//        if (!tmpFile.delete())
//            throw new IOException();
//
//        /*
//         * areas
//         */
//        URL areaUrl = this.getClass().getClassLoader().getResource("eicalculator_out_areas.csv");
//        OmsEIAreasReader areaReader = new OmsEIAreasReader();
//        areaReader.file = new File(areaUrl.toURI()).getAbsolutePath();
//        areaReader.pSeparator = "\\s+";
//        areaReader.read();
//        areaReader.close();
//        List<EIAreas> areas = areaReader.outAreas;
//        assertTrue(areas.size() == 14700);
//
//        // write file
//        File tmpFile1 = File.createTempFile("altim", ".csv");
//        String areasTmpFile = tmpFile1.getAbsolutePath();
//        OmsEIAreasWriter areasWriter = new OmsEIAreasWriter();
//        areasWriter.file = areasTmpFile;
//        areasWriter.inAreas = areas;
//        areasWriter.pSeparator = " ";
//        areasWriter.write();
//        areasWriter.close();
//        OmsEIAreasReader areasTmpReader = new OmsEIAreasReader();
//        areasTmpReader.file = areasTmpFile;
//        areasTmpReader.pSeparator = "\\s+";
//        areasTmpReader.read();
//        areasTmpReader.close();
//        List<EIAreas> areasTmp = areasTmpReader.outAreas;
//        for( int i = 0; i < areas.size(); i++ ) {
//            assertEquals(areas.get(i).basinId, areasTmp.get(i).basinId);
//        }
//        if (tmpFile1.delete())
//            throw new IOException();
//
//        /*
//         * energy
//         */
//        URL energyUrl = this.getClass().getClassLoader().getResource("eicalculator_out_energy.csv");
//        OmsEIEnergyReader energyReader = new OmsEIEnergyReader();
//        energyReader.file = new File(energyUrl.toURI()).getAbsolutePath();
//        energyReader.pSeparator = "\\s+";
//        energyReader.read();
//        energyReader.close();
//        List<EIEnergy> energy = energyReader.outEnergy;
//        assertTrue(energy.size() == 17640);
//
//        // write file
//        File tmpFile2 = File.createTempFile("altim", ".csv");
//        String energyTmpFile = tmpFile2.getAbsolutePath();
//        OmsEIEnergyWriter energyWriter = new OmsEIEnergyWriter();
//        energyWriter.file = energyTmpFile;
//        energyWriter.inEnergy = energy;
//        energyWriter.pSeparator = " ";
//        energyWriter.write();
//        energyWriter.close();
//        OmsEIEnergyReader energyTmpReader = new OmsEIEnergyReader();
//        energyTmpReader.file = energyTmpFile;
//        energyTmpReader.pSeparator = "\\s+";
//        energyTmpReader.read();
//        energyTmpReader.close();
//        List<EIEnergy> energyTmp = energyTmpReader.outEnergy;
//        for( int i = 0; i < energy.size(); i++ ) {
//            assertEquals(energy.get(i).basinId, energyTmp.get(i).basinId);
//        }
//        if (!tmpFile2.delete())
//            throw new IOException();
//
//    }
//}
