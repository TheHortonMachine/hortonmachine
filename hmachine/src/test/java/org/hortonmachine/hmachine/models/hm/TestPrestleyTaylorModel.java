package org.hortonmachine.hmachine.models.hm;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import org.geotools.util.URLs;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPresteyTaylorEtpModel;
import org.hortonmachine.hmachine.utils.HMTestCase;

/**
 * Test PrestleyTaylorModel.
 * 
 */
@SuppressWarnings("nls")
public class TestPrestleyTaylorModel extends HMTestCase {

    public void testFaoEtpDaily() throws Exception {

        String startDate = "2005-05-02 00:00";
        String endDate = "2005-05-02 000:00";
        int timeStepMinutes = 60;
        String fId = "ID";

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        URL TempUrl = this.getClass().getClassLoader().getResource("PT_in_temp.csv");

        URL netradiationUrl = this.getClass().getClassLoader().getResource("PT_in_netrad.csv");

        URL pressureUrl = this.getClass().getClassLoader().getResource("PT_in_atmpress.csv");

        OmsTimeSeriesIteratorReader tempReader = getTimeseriesReader(TempUrl, fId, startDate, endDate, timeStepMinutes);
        OmsTimeSeriesIteratorReader pressReader = getTimeseriesReader(pressureUrl, fId, startDate, endDate, timeStepMinutes);
        OmsTimeSeriesIteratorReader netradReader = getTimeseriesReader(netradiationUrl, fId, startDate, endDate, timeStepMinutes);

        OmsPresteyTaylorEtpModel PTEtp = new OmsPresteyTaylorEtpModel();

        while( tempReader.doProcess ) {
            tempReader.nextRecord();

            HashMap<Integer, double[]> id2ValueMap = tempReader.outData;
            PTEtp.inTemp = id2ValueMap;

            PTEtp.tCurrent = tempReader.tCurrent;

            pressReader.nextRecord();
            id2ValueMap = pressReader.outData;
            PTEtp.inPressure = id2ValueMap;

            PTEtp.defaultPressure = 101.3;

            netradReader.nextRecord();
            id2ValueMap = netradReader.outData;
            PTEtp.inNetradiation = id2ValueMap;

            PTEtp.pAlpha = 1.06;
            PTEtp.pGmorn = 0.35;
            PTEtp.pGnight = 0.75;
            PTEtp.doHourly = true;
            PTEtp.pm = pm;
            PTEtp.process();

            HashMap<Integer, double[]> outEtp = PTEtp.outPTEtp;

            double value = outEtp.get(1221)[0];
            System.out.println(value);
            assertTrue(NumericsUtilities.dEq(value, -0.01375, 0.001));
        }

        TempUrl = this.getClass().getClassLoader().getResource("PT_in_temp_day.csv");

        netradiationUrl = this.getClass().getClassLoader().getResource("PT_in_netrad_day.csv");

        pressureUrl = this.getClass().getClassLoader().getResource("PT_in_atmpress_day.csv");

        tempReader = getTimeseriesReader(TempUrl, fId, startDate, endDate, timeStepMinutes);
        pressReader = getTimeseriesReader(pressureUrl, fId, startDate, endDate, timeStepMinutes);
        netradReader = getTimeseriesReader(netradiationUrl, fId, startDate, endDate, timeStepMinutes);

        PTEtp = new OmsPresteyTaylorEtpModel();

        while( tempReader.doProcess ) {
            tempReader.nextRecord();

            HashMap<Integer, double[]> id2ValueMap = tempReader.outData;
            PTEtp.inTemp = id2ValueMap;

            PTEtp.tCurrent = tempReader.tCurrent;

            pressReader.nextRecord();
            id2ValueMap = pressReader.outData;
            PTEtp.inPressure = id2ValueMap;

            PTEtp.defaultPressure = 101.3;

            netradReader.nextRecord();
            id2ValueMap = netradReader.outData;
            PTEtp.inNetradiation = id2ValueMap;

            PTEtp.pAlpha = 1.1;

            PTEtp.doHourly = false;
            PTEtp.pm = pm;
            PTEtp.process();

            HashMap<Integer, double[]> outEtp = PTEtp.outPTEtp;

            double value = outEtp.get(1221)[0];
            System.out.println(value);
            assertTrue(NumericsUtilities.dEq(value, 2.9937, 0.1));
        }

    }

    private OmsTimeSeriesIteratorReader getTimeseriesReader( URL url, String id, String startDate, String endDate,
            int timeStepMinutes ) throws URISyntaxException {
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = URLs.urlToFile(url).getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = startDate;
        reader.tTimestep = timeStepMinutes;
        reader.tEnd = endDate;
        reader.fileNovalue = "-9999";
        reader.initProcess();
        return reader;
    }

}
