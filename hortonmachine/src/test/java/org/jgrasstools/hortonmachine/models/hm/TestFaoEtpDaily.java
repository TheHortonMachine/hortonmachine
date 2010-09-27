package org.jgrasstools.hortonmachine.models.hm;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepWriterId2Value;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.etp.FaoEtpDaily;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
/**
 * Test FAO daily evapotranspiration.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TestFaoEtpDaily extends HMTestCase {

    public void testFaoEtpDaily() throws Exception {

        // PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        // URL rainUrl = this.getClass().getClassLoader().getResource("etp_in_data_rain.csv");

        String startDate = "2005-05-01 00:00";
        String endDate = "2005-05-02 00:00";
        int timeStepMinutes = 1440;
        String fId = "ID";

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        String folder = "/Users/silli/development/jgrasstools-hg/jgrasstools/hortonmachine/src/test/resources/";

        // URL maxTempUrl = this.getClass().getClassLoader().getResource("faoetpday_in_tmax.csv");
        // URL minTempUrl = this.getClass().getClassLoader().getResource("faoetpday_in_tmin.csv");
        // URL windUrl = this.getClass().getClassLoader().getResource("faoetpday_in_wind.csv");
        // URL humidityUrl = this.getClass().getClassLoader().getResource("faoetpday_in_rh.csv");
        //
        // URL netradiationUrl =
        // this.getClass().getClassLoader().getResource("faoetpday_in_rad.csv");

        String maxTempUrl = folder + "faoetpday_in_tmax.csv";
        String minTempUrl = folder + "faoetpday_in_tmin.csv";
        String windUrl = folder + "faoetpday_in_wind.csv";
        String humidityUrl = folder + "faoetpday_in_rh.csv";
        String netradiationUrl = folder + "faoetpday_in_rad.csv";

        // File maxTempFile = new File(maxTempUrl.toURI());
        File outputFile = new File(folder + "faoetpday_out.csv");
        outputFile = classesTestFile2srcTestResourcesFile(outputFile);

        TimeseriesByStepReaderId2Value maxtempReader = getTimeseriesReader(maxTempUrl, fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value mintempReader = getTimeseriesReader(minTempUrl, fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value windReader = getTimeseriesReader(windUrl, fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value humReader = getTimeseriesReader(humidityUrl, fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value netradReader = getTimeseriesReader(netradiationUrl, fId, startDate, endDate, timeStepMinutes);

        FaoEtpDaily faoEtpDaily = new FaoEtpDaily();

        TimeseriesByStepWriterId2Value etpWriter = new TimeseriesByStepWriterId2Value();
        etpWriter.file = outputFile.getAbsolutePath();

        while( maxtempReader.doProcess ) {
            maxtempReader.nextRecord();

            maxtempReader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = maxtempReader.data;
            faoEtpDaily.inMaxTemp = id2ValueMap;

            mintempReader.nextRecord();
            id2ValueMap = mintempReader.data;
            faoEtpDaily.inMinTemp = id2ValueMap;

            windReader.nextRecord();
            id2ValueMap = windReader.data;
            faoEtpDaily.inWind = id2ValueMap;

            faoEtpDaily.defaultPressure = 101.3;

            humReader.nextRecord();
            id2ValueMap = humReader.data;
            faoEtpDaily.inRh = id2ValueMap;

            netradReader.nextRecord();
            id2ValueMap = netradReader.data;
            faoEtpDaily.inNetradiation = id2ValueMap;

            faoEtpDaily.pm = pm;
            faoEtpDaily.process();

            HashMap<Integer, double[]> outEtp = faoEtpDaily.outFaoEtp;

            etpWriter.tStart = startDate;
            etpWriter.tTimestep = timeStepMinutes;
            etpWriter.data = outEtp;
            etpWriter.writeNextLine();
        }

        maxtempReader.close();
        windReader.close();
        humReader.close();
        netradReader.close();

        etpWriter.close();

    }

    private TimeseriesByStepReaderId2Value getTimeseriesReader( String path, String id, String startDate, String endDate, int timeStepMinutes ) throws URISyntaxException {
        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = path;
        reader.idfield = "ID";
        reader.tStart = "2005-05-01 00:00";
        reader.tTimestep = 1440;
        reader.tEnd = "2005-05-02 00:00";
        reader.fileNovalue = "-9999";
        reader.initProcess();
        return reader;
    }

}
