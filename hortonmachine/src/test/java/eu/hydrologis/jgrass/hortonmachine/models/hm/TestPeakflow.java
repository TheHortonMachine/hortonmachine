//package eu.hydrologis.jgrass.hortonmachine.models.hm;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.joda.time.DateTime;
//
//import eu.hydrologis.jgrass.hortonmachine.io.arcgrid.ArcgridCoverageReader;
//import eu.hydrologis.jgrass.hortonmachine.io.timeseries.TimeseriesReaderArray;
//import eu.hydrologis.jgrass.hortonmachine.io.timeseries.TimeseriesWriterArray;
//import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.peakflow.Peakflow;
//import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
//
///**
// * Test the {@link Peakflow} module.
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
//        Peakflow peakflow = new Peakflow();
//        peakflow.pm = pm;
//        peakflow.topindexCoverage = topindexCoverage;
//        peakflow.supRescaledCoverage = supRescaledCoverage;
//        peakflow.subRescaledCoverage = subRescaledCoverage;
//        peakflow.a = 43.91;
//        peakflow.n = 0.48;
//        peakflow.rainfallList = null;
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
//        Peakflow peakflow = new Peakflow();
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
