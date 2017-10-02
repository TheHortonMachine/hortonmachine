package org.hortonmachine.hmachine.models.hm;
//package org.hortonmachine.hmachine.models.hm;
//
//import java.io.File;
//import java.util.List;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.junit.Ignore;
//
//import eu.hydrologis.jgrass.hortonmachine.io.arcgrid.ArcgridCoverageReader;
//import eu.hydrologis.jgrass.hortonmachine.io.csv.CsvWriter;
//import eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants;
//import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.energyindexcalculator.EIAltimetry;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.energyindexcalculator.EIAreas;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.energyindexcalculator.EIEnergy;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.energyindexcalculator.EnergyIndexCalculator;
//import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
//
///**
// * Test {@link OmsEnergyIndexCalculator}.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEnergyIndexCalculatorRealMaps extends HMTestCase {
//
//    @Ignore 
//    public void testEnergyIndexCalculator() throws Exception {
//
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out);
//
//        String folder = "D:\\data\\eidata\\input\\";
//        File folderFile = new File(folder);
//
//        String aspect = "basin_aspect_rad.asc";
//        String nabla = "basin_nablac.asc";
//        String pit = "basin_pit.asc";
//        String slope = "basin_slope_rad.asc";
//        String subb = "basin_subb_tca1000000.asc";
//
//        ArcgridCoverageReader reader = new ArcgridCoverageReader();
//        reader.arcgridCoverageFile = new File(folderFile, aspect);
//        reader.fileNovalue = -9999.0;
//        reader.novalue = HMConstants.doubleNovalue;
//        reader.pm = pm;
//        reader.readCoverage();
//        GridCoverage2D aspectCoverage = reader.coverage;
//        reader = new ArcgridCoverageReader();
//        reader.arcgridCoverageFile = new File(folderFile, nabla);
//        reader.fileNovalue = -9999.0;
//        reader.novalue = HMConstants.doubleNovalue;
//        reader.pm = pm;
//        reader.readCoverage();
//        GridCoverage2D nablaCoverage = reader.coverage;
//        reader = new ArcgridCoverageReader();
//        reader.arcgridCoverageFile = new File(folderFile, pit);
//        reader.fileNovalue = -9999.0;
//        reader.novalue = HMConstants.doubleNovalue;
//        reader.pm = pm;
//        reader.readCoverage();
//        GridCoverage2D pitCoverage = reader.coverage;
//        reader = new ArcgridCoverageReader();
//        reader.arcgridCoverageFile = new File(folderFile, slope);
//        reader.fileNovalue = -9999.0;
//        reader.novalue = HMConstants.doubleNovalue;
//        reader.pm = pm;
//        reader.readCoverage();
//        GridCoverage2D slopeCoverage = reader.coverage;
//        reader = new ArcgridCoverageReader();
//        reader.arcgridCoverageFile = new File(folderFile, subb);
//        reader.fileNovalue = -9999.0;
//        reader.novalue = HMConstants.doubleNovalue;
//        reader.pm = pm;
//        reader.readCoverage();
//        GridCoverage2D subbCoverage = reader.coverage;
//
//        OmsEnergyIndexCalculator eiCalculator = new OmsEnergyIndexCalculator();
//        eiCalculator.aspectCoverage = aspectCoverage;
//        eiCalculator.curvaturesCoverage = nablaCoverage;
//        eiCalculator.elevationCoverage = pitCoverage;
//        eiCalculator.slopeCoverage = slopeCoverage;
//        eiCalculator.idbasinCoverage = subbCoverage;
//        eiCalculator.dtData = 1;
//        eiCalculator.numEi = 5;
//        eiCalculator.numEs = 5;
//        eiCalculator.latitude = 45;
//        eiCalculator.pm = pm;
//
//        eiCalculator.executeEnergyIndexCalculator();
//
//        List<EIAltimetry> altimetricValues = eiCalculator.altimetricValues;
//        List<EIEnergy> energeticValues = eiCalculator.energeticValues;
//        List<EIAreas> areaValues = eiCalculator.areaValues;
//
//        CsvWriter csvWriterAltim = new CsvWriter();
//        csvWriterAltim.csvfile = new File(folderFile, "altimetry.csv");
//        csvWriterAltim.open();
//        for( EIAltimetry tmp : altimetricValues ) {
//            csvWriterAltim.add(String.valueOf(tmp.basinId));
//            csvWriterAltim.add(String.valueOf(tmp.altimetricBandId));
//            csvWriterAltim.add(String.valueOf(tmp.elevationValue));
//            csvWriterAltim.add(String.valueOf(tmp.bandRange));
//            csvWriterAltim.newline();
//        }
//        csvWriterAltim.close();
//
//        CsvWriter csvWriterEnergy = new CsvWriter();
//        csvWriterEnergy.csvfile = new File(folderFile, "energy.csv");
//        csvWriterEnergy.open();
//        for( EIEnergy tmp : energeticValues ) {
//            csvWriterEnergy.add(String.valueOf(tmp.basinId));
//            csvWriterEnergy.add(String.valueOf(tmp.altimetricBandId));
//            csvWriterEnergy.add(String.valueOf(tmp.virtualMonth));
//            csvWriterEnergy.add(String.valueOf(tmp.energyValue));
//            csvWriterEnergy.newline();
//        }
//        csvWriterEnergy.close();
//        
//        CsvWriter csvWriterAreas = new CsvWriter();
//        csvWriterAreas.csvfile = new File(folderFile, "areas.csv");
//        csvWriterAreas.open();
//        for( EIAreas tmp : areaValues ) {
//            csvWriterAreas.add(String.valueOf(tmp.basinId));
//            csvWriterAreas.add(String.valueOf(tmp.altimetricBandId));
//            csvWriterAreas.add(String.valueOf(tmp.energyId));
//            csvWriterAreas.add(String.valueOf(tmp.areaValue));
//            csvWriterAreas.newline();
//        }
//        csvWriterAreas.close();
//
//    }
//
//}
