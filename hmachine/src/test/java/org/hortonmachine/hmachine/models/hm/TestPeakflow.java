package org.hortonmachine.hmachine.models.hm;
//package org.hortonmachine.hmachine.models.hm;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.hortonmachine.gears.io.arcgrid.ArcgridCoverageReader;
//import org.hortonmachine.gears.io.timeseries.TimeseriesReaderArray;
//import org.hortonmachine.gears.io.timeseries.TimeseriesWriterArray;
//import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow;
//import org.hortonmachine.hmachine.utils.HMTestCase;
//import org.joda.time.DateTime;
//
///**
// * Test the {@link OmsPeakflow} module.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestPeakflow extends HMTestCase {
//    public void testStatisticPeakflow() throws Exception {
//
//        File folder = new File("C:\\TMP\\peakflow\\");
//
//        File topFile = new File(folder, "mycismon_topindex.asc");
//        GridCoverage2D topindexCoverage = getCoverage(topFile);
//
//        File supFile = new File(folder, "mycismon_resc10.asc");
//        GridCoverage2D supRescaledCoverage = getCoverage(supFile);
//
//        File subFile = new File(folder, "mycismon_resc100.asc");
//        GridCoverage2D subRescaledCoverage = getCoverage(subFile);
//
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        OmsPeakflow peakflow = new OmsPeakflow();
//        peakflow.pm = pm;
//        peakflow.inTopindex = topindexCoverage;
//        peakflow.inRescaledsup = supRescaledCoverage;
//        peakflow.inRescaledsub = subRescaledCoverage;
//        peakflow.pA = 43.91;
//        peakflow.pN = 0.48;
//        peakflow.inRainfall = null;
//        peakflow.rainfallTimestampList = null;
//        peakflow.saturationThreshold = 30;
//        peakflow.diffusion = 1000;
//        peakflow.channelCelerity = 2;
//
//        peakflow.executePeakflow();
//
//        List<DateTime> peakflowTimestampList = peakflow.peakflowTimestampList;
//        List<double[]> peakflowOutputList = peakflow.peakflowOutputList;
//
//        File outFile = new File(folder, "peakflow_discharge.csv");
//        TimeseriesWriterArray writer = new TimeseriesWriterArray();
//        writer.csvfilePath = outFile.getAbsolutePath();
//        writer.doWriteDates = false;
//        writer.tableName = "discharge";
//        writer.timestampList = peakflowTimestampList;
//        writer.valuesArrayList = peakflowOutputList;
//
//        writer.open();
//        writer.write();
//        writer.close();
//    }
//
//    public void testRealRainPeakflow() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        File folder = new File("C:\\TMP\\peakflow\\");
//
//        File topFile = new File(folder, "mycismon_topindex.asc");
//        GridCoverage2D topindexCoverage = getCoverage(topFile);
//
//        File supFile = new File(folder, "mycismon_resc10.asc");
//        GridCoverage2D supRescaledCoverage = getCoverage(supFile);
//
//        File subFile = new File(folder, "mycismon_resc100.asc");
//        GridCoverage2D subRescaledCoverage = getCoverage(subFile);
//
//        File rainFile = new File(folder, "rain_peak.csv");
//        TimeseriesReaderArray rainReader = new TimeseriesReaderArray();
//        rainReader.fileNovalue = "-9999";
//        rainReader.csvfilePath = rainFile.getAbsolutePath();
//        rainReader.open();
//        rainReader.read();
//        rainReader.close();
//
//        List<DateTime> timestampsList = rainReader.timestampsList;
//        List<double[]> recordsList = rainReader.recordsList;
//
//        OmsPeakflow peakflow = new OmsPeakflow();
//        peakflow.pm = pm;
//        peakflow.topindexCoverage = topindexCoverage;
//        peakflow.supRescaledCoverage = supRescaledCoverage;
//        peakflow.subRescaledCoverage = subRescaledCoverage;
//        peakflow.rainfallList = recordsList;
//        peakflow.rainfallTimestampList = timestampsList;
//        peakflow.saturationThreshold = 30;
//        peakflow.diffusion = 1000;
//        peakflow.oututstepArg = 300;
//        peakflow.channelCelerity = 2;
//
//        peakflow.executePeakflow();
//
//        List<DateTime> peakflowTimestampList = peakflow.peakflowTimestampList;
//        List<double[]> peakflowOutputList = peakflow.peakflowOutputList;
//
//        File outFile = new File(folder, "peakflow_discharge_real.csv");
//        TimeseriesWriterArray writer = new TimeseriesWriterArray();
//        writer.csvfilePath = outFile.getAbsolutePath();
//        // writer.doWriteDates = false;
//        writer.tableName = "discharge";
//        writer.timestampList = peakflowTimestampList;
//        writer.valuesArrayList = peakflowOutputList;
//
//        writer.open();
//        writer.write();
//        writer.close();
//    }
//
//    private GridCoverage2D getCoverage( File file ) throws IOException {
//        ArcgridCoverageReader reader = new ArcgridCoverageReader();
//        reader.arcgridCoveragePath = file.getAbsolutePath();
//        reader.fileNovalue = -9999.0;
//        reader.readCoverage();
//        return reader.coverage;
//    }
//
//}
