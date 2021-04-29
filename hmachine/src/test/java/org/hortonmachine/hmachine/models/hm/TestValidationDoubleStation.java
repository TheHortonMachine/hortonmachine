package org.hortonmachine.hmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.old.OmsValidateDoubleStation;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
/**
 * Test the ValidationDoubleStation model.
 * 
 * @author daniele andreis
 *
 */
public class TestValidationDoubleStation extends HMTestCase {

    public void testValidationDoubleStation() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        URL stazioniUrl = this.getClass().getClassLoader().getResource("rainstations.shp");
        File stazioniFile = new File(stazioniUrl.toURI());

        URL krigingRainUrl = this.getClass().getClassLoader().getResource("rain_test1.csv");
        File krigingRainFile = new File(krigingRainUrl.toURI());

        OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
        stationsReader.file = stazioniFile.getAbsolutePath();
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;

        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = krigingRainFile.getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        // reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";

        reader.initProcess();

        OmsValidateDoubleStation validatStation = new OmsValidateDoubleStation();
        validatStation.pm = pm;

        validatStation.inStations = stationsFC;
        validatStation.fStationsid = "ID_PUNTI_M";
        validatStation.doMean = true;

        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            validatStation.inData = id2ValueMap;
            validatStation.verifyDoubleStation();
            /*
             * Extract the result.
             */
            HashMap<Integer, double[]> result = validatStation.outData;
            double[][] test = HMTestMaps.outValidation;

            Set<Integer> resultSet = result.keySet();
            Iterator<Integer> idIterator = resultSet.iterator();
            int j = 1;
            while( idIterator.hasNext() ) {
                int id = idIterator.next();
                double actual = result.get(id)[0];
                boolean done = false;
                for( int i = 0; i < test.length; i++ ) {
                    if (test[i][0] == id) {
                        assertEquals(" " + i + " " + j, test[i][j], actual, 0.01);
                        done=true;
                    }
                }
                if(!done){
                    fail();
                }
                
                
            }
            j++;
        }

        reader.close();

    }

}
