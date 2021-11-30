package org.hortonmachine.hmachine.models.hm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.OmsKrigingVectorMode;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.VariogramFunction;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.VariogramFunctionFitter;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.ITheoreticalVariogram;
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

    @Test
    public void testVariogramFitter() throws Exception {
        double[] distances = {1, 2, 3, 1, 2, 3};
        double[] values = {1, 1.5, 2, 2, 2.5, 3};

        VariogramFunction function = new VariogramFunction(ITheoreticalVariogram.LINEAR);

        List<double[]> data = new ArrayList<>();
        for( int i = 0; i < values.length; i++ ) {
            data.add(new double[] {distances[i], values[i]});
        }
        VariogramFunctionFitter fitter = new VariogramFunctionFitter(function, 1, 3, 4);
        double[] fit = fitter.fit(data);

        assertEquals(1.9769933586233943, fit[0], 0.0000001);
        assertEquals(3.9539830033063583, fit[1], 0.0000001);
        assertEquals(1.0000000000000002, fit[2], 0.0000001);
    }

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
        id2DataMap.put(465, 3.989371127704894);
        id2DataMap.put(713, 5.129653798910705);
        id2DataMap.put(739, 4.773522198797728);
        expectedDate2IdValueMap.put("2014-10-01 12:00", id2DataMap);
        id2DataMap = new HashMap<>();
        id2DataMap.put(465, 11.787708461388936);
        id2DataMap.put(713, 11.402312659753031);
        id2DataMap.put(739, 12.532726275127615);
        expectedDate2IdValueMap.put("2014-10-02 12:00", id2DataMap);
        id2DataMap = new HashMap<>();
        id2DataMap.put(465, 7.548860810813379);
        id2DataMap.put(713, 5.20679546816802);
        id2DataMap.put(739, 8.932202995718598);
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

                assertEquals(checkValue, values[0], 0.000001);
//                System.out.println("\tStation: " + key + " -> " + values[0] + " VS. " + checkValue);
            }
        }
        inputReader.close();
    }

}
