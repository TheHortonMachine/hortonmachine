package org.hortonmachine.hmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.OmsKrigingCheckMode;
import org.hortonmachine.hmachine.modules.statistics.kriging.OmsKrigingVectorMode;
import org.junit.Test;

/**
 * @author Andrea Antonello
 *
 */
public class TestKrigingVectorMode {

    private String getRes( String name ) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(name);
        File file = new File(url.toURI());
        return file.getAbsolutePath();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testKrigings() throws Exception {

        String stationsPath = "kriging/vectormode/stations.shp";
        String interpolationPath = "kriging/vectormode/pointWhereInterp.shp";
        String input = "kriging/vectormode/P.csv";

        String startDate = "2014-10-01 12:00";
        String endDate = "2014-10-03 12:00";
        int timestep = 60 * 24;

        SimpleFeatureCollection stationsFC = OmsShapefileFeatureReader.readShapefile(getRes(stationsPath));
        SimpleFeatureCollection interpolatedFC = OmsShapefileFeatureReader.readShapefile(getRes(interpolationPath));

        OmsTimeSeriesIteratorReader inputReader = new OmsTimeSeriesIteratorReader();
        inputReader.file = getRes(input);
        inputReader.idfield = "ID";
        inputReader.tStart = startDate;
        inputReader.tEnd = endDate;
        inputReader.tTimestep = timestep;
        inputReader.fileNovalue = "-9999";
        inputReader.initProcess();

        OmsKrigingVectorMode kriging = new OmsKrigingVectorMode();
        kriging.pm = new DummyProgressMonitor();
        kriging.inStations = stationsFC;
        kriging.inInterpolate = interpolatedFC;
        kriging.fInterpolateid = "ID";
        kriging.fPointZ = "ELEVATI";
        kriging.fStationsid = "ID";
        kriging.fStationsZ = "ELEVATI";
        kriging.inNumCloserStations = 11;

        kriging.pSemivariogramType = "exponential";

        kriging.range = 10000;
        kriging.nugget = 1;
        kriging.sill = 10;
        // kriging.maxdist = 1000; // TODO check
        kriging.doIncludezero = false;

        HashMap<String, HashMap<Integer, Double>> expectedDate2IdValueMap = new HashMap<>();

        HashMap<Integer, Double> id2DataMap = new HashMap<>();
        id2DataMap.put(465, 3.0);
        id2DataMap.put(713, 10.0);
        id2DataMap.put(739, 3.0);
        expectedDate2IdValueMap.put("2014-10-01 12:00", id2DataMap);
        id2DataMap = new HashMap<>();
        id2DataMap.put(465, 10.0);
        id2DataMap.put(713, 15.0);
        id2DataMap.put(739, 15.0);
        expectedDate2IdValueMap.put("2014-10-02 12:00", id2DataMap);
        id2DataMap = new HashMap<>();
        id2DataMap.put(465, 5.0);
        id2DataMap.put(713, 5.0);
        id2DataMap.put(739, 13.0);
        expectedDate2IdValueMap.put("2014-10-03 12:00", id2DataMap);

        while( inputReader.doProcess ) {
            inputReader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = inputReader.outData;
            kriging.inData = id2ValueMap;
            kriging.executeKriging();
            HashMap<Integer, double[]> result = kriging.outData;

            HashMap<Integer, Double> id2Data = expectedDate2IdValueMap.get(inputReader.tCurrent);

            System.out.println("Timestep: " + inputReader.tCurrent);
            for( Entry<Integer, double[]> entry : result.entrySet() ) {
                Integer key = entry.getKey();
                double[] values = entry.getValue();

                double checkValue = id2Data.get(key);
                
                System.out.println("\tStation: " + key + " -> " + values[0] + " VS. " + checkValue);

            }

        }
        inputReader.close();
//            expectedOutputReader.close();

    }

}
