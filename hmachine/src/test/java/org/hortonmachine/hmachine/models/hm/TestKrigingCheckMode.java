package org.hortonmachine.hmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.OmsKrigingCheckMode;
import org.junit.Test;

/**
 * @author Andrea Antonello
 *
 */
public class TestKrigingCheckMode {

    private String getRes( String name ) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(name);
        File file = new File(url.toURI());
        return file.getAbsolutePath();
    }

    @Test
    public void testKrigings() throws Exception {

        String stationsPath = "kriging/stations.shp";

        String variogramPref = "kriging/kriging_interpolated_1_orario_";
        String variogramPost = ".csv";
        String[] variograms = {//
//                "bessel", //
                "exponential", //
//                "gaussian", //
//                "linear", //
//                "spherical",//
        };

        String input = "kriging/temp_ordinate_QC_1.csv";

        String startDate = "2008-01-01 00:00";
        String endDate = "2008-01-31 23:00";
        int timestep = 60;

        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
        stationsReader.file = getRes(stationsPath);
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;

        for( String variogramType : variograms ) {
            OmsTimeSeriesIteratorReader inputReader = new OmsTimeSeriesIteratorReader();
            inputReader.file = getRes(input);
            inputReader.idfield = "ID";
            inputReader.tStart = startDate;
            inputReader.tEnd = endDate;
            inputReader.tTimestep = timestep;
            inputReader.fileNovalue = "-9999";
            inputReader.initProcess();

            OmsKrigingCheckMode kriging = new OmsKrigingCheckMode();
            kriging.pm = new DummyProgressMonitor();
            kriging.inStations = stationsFC;
            kriging.fStationsid = "id";
            kriging.fStationsZ = "quota";
            kriging.inNumCloserStations = 10;

            kriging.pSemivariogramType = variogramType;

            kriging.range = 41315.76341306812;
            kriging.nugget = 5.982206872;
            kriging.sill = 3.33;
            // kriging.maxdist = 1000; // TODO check
            kriging.doIncludezero = false;

            OmsTimeSeriesIteratorReader expectedOutputReader = new OmsTimeSeriesIteratorReader();
            expectedOutputReader.file = getRes(variogramPref + variogramType + variogramPost);
            expectedOutputReader.idfield = "ID";
            expectedOutputReader.tStart = startDate;
            expectedOutputReader.tEnd = endDate;
            expectedOutputReader.tTimestep = timestep;
            expectedOutputReader.fileNovalue = "-9999";
            expectedOutputReader.initProcess();

            int count = 0;
            while( inputReader.doProcess ) {
                inputReader.nextRecord();
                HashMap<Integer, double[]> id2ValueMap = inputReader.outData;
                kriging.inData = id2ValueMap;
                kriging.executeKriging();
                HashMap<Integer, double[]> result = kriging.outData;

                expectedOutputReader.nextRecord();
                HashMap<Integer, double[]> checkData = expectedOutputReader.outData;
//
                for( Entry<Integer, double[]> entry : result.entrySet() ) {
                    Integer key = entry.getKey();
                    double[] values = entry.getValue();

                    double[] checkValues = checkData.get(key);
                }

            }
            inputReader.close();
//            expectedOutputReader.close();
        }

    }

}
