package eu.hydrologis.jgrass.hortonmachine.models.hm;

import static eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants.TEMPERATURE;
import static eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants.utcDateFormatterYYYYMMDDHHMM;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.geotools.feature.FeatureCollection;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.hydrologis.jgrass.hortonmachine.modules.statistics.jami.Jami;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.jgrassgears.io.eicalculator.EIAltimetry;
import eu.hydrologis.jgrass.jgrassgears.io.eicalculator.EIAltimetryReader;
import eu.hydrologis.jgrass.jgrassgears.io.shapefile.ShapefileFeatureReader;
import eu.hydrologis.jgrass.jgrassgears.io.timedependent.TimeseriesByStepReaderId2Value;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
/**
 * Test jami.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestJami extends HMTestCase {

    public void testJami() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        URL altimUrl = this.getClass().getClassLoader().getResource(
                "eicalculator_out_altimetry.csv");
        File altimetryFile = new File(altimUrl.toURI());
        URL stationsUrl = this.getClass().getClassLoader().getResource("jami_in_stations.shp");
        URL stationdataUrl = this.getClass().getClassLoader().getResource("jami_new_temp.csv");
        File stationDataFile = new File(stationdataUrl.toURI());
        URL basinsUrl = this.getClass().getClassLoader().getResource("jami_in_basins.shp");

        EIAltimetryReader altim = new EIAltimetryReader();
        altim.file = altimetryFile.getAbsolutePath();
        altim.pSeparator = "\\s+";
        altim.pm = pm;
        altim.read();
        List<EIAltimetry> altimList = altim.outAltimetry;
        altim.close();

        ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
        stationsReader.file = new File(stationsUrl.toURI()).getAbsolutePath();
        stationsReader.readFeatureCollection();
        FeatureCollection<SimpleFeatureType, SimpleFeature> stationsFC = stationsReader.geodata;

        ShapefileFeatureReader basinsReader = new ShapefileFeatureReader();
        basinsReader.file = new File(basinsUrl.toURI()).getAbsolutePath();
        basinsReader.readFeatureCollection();
        FeatureCollection<SimpleFeatureType, SimpleFeature> basinsFC = basinsReader.geodata;

        TimeseriesByStepReaderId2Value dataReader = new TimeseriesByStepReaderId2Value();
        dataReader.file = stationDataFile.getAbsolutePath();
        dataReader.fileNovalue = "-9999";
        dataReader.idfield = "ID";
        dataReader.tStart = "2005-05-01 00:00";
        dataReader.tTimestep = 60;
        dataReader.tEnd = "2000-01-01 03:00";

        dataReader.startTicking();

        Jami jami = new Jami();
        jami.pm = pm;
        jami.inAltimetry = altimList;
        jami.fStationid = "id_punti_m";
        jami.fStationelev = "quota";
        jami.fBasinid = "netnum";
        jami.pNum = 2;
        jami.pBins = 4;
        jami.pType = TEMPERATURE;
        jami.inStations = stationsFC;
        jami.inInterpolate = basinsFC;

        DateTimeFormatter dF = utcDateFormatterYYYYMMDDHHMM;
        // 1 5 2005 - 1 6 2005
        // 30 min

        double[] values1221 = null;
        while( dataReader.isTicking ) {
            dataReader.nextRecord();
            DateTime runningDate = dF.parseDateTime(dataReader.tCurrent);
            HashMap<Integer, double[]> id2ValueMap = dataReader.data;
            jami.inMeteo = id2ValueMap;
            jami.tCurrent = runningDate.toString(dF);

            jami.process();

            HashMap<Integer, double[]> interpolationPointId2MeteoDataMap = jami.outInterpolated;
            values1221 = interpolationPointId2MeteoDataMap.get(1221);

            runningDate = runningDate.plusMinutes(30);

            for( int i = 0; i < values1221.length; i++ ) {
                System.out.println(values1221[i]);
            }

        }
        dataReader.close();

        // for now there is no writer... waiting for the apposite module

        // double[] result1221 = new double[]{9.925938910342381, 8.1, 7.561630032493953,
        // 5.86663426553045, 3.4220912542795046};
        //
        // for( int i = 0; i < result1221.length; i++ ) {
        // assertEquals(result1221[i], values1221[i], 0.0001);
        // }

    }
}
