package org.jgrasstools.hortonmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.statistics.kriging.ValidateDoubleStation;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
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

        ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
        stationsReader.file = stazioniFile.getAbsolutePath();
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;

        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = krigingRainFile.getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        // reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";

        reader.initProcess();

        ValidateDoubleStation validatStation = new ValidateDoubleStation();
        validatStation.pm = pm;

        validatStation.inStations = stationsFC;
        validatStation.fStationsid = "ID_PUNTI_M";
        validatStation.doMean = true;

        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.data;
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
