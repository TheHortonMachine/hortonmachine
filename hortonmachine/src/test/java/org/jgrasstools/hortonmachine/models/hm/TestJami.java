package org.jgrasstools.hortonmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.geotools.feature.FeatureCollection;
import org.jgrasstools.gears.io.eicalculator.EIAltimetry;
import org.jgrasstools.gears.io.eicalculator.EIAltimetryReader;
import org.jgrasstools.gears.io.id2valuearray.Id2ValueArrayWriter;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepWriterId2Value;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.statistics.jami.Jami;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
/**
 * Test jami.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestJami extends HMTestCase {

    public void testJami() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        URL altimUrl = this.getClass().getClassLoader().getResource("eicalculator_out_altimetry.csv");
        File altimetryFile = new File(altimUrl.toURI());
        File outputFile = new File(altimetryFile.getParentFile(), "jami_out_temp.csv");
        outputFile = classesTestFile2srcTestResourcesFile(outputFile);

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
        dataReader.tEnd = "2005-05-01 03:00";

        dataReader.initProcess();

        Jami jami = new Jami();
        jami.pm = pm;
        jami.inAltimetry = altimList;
        jami.fStationid = "id_punti_m";
        jami.fStationelev = "quota";
        jami.fBasinid = "netnum";
        jami.pNum = 2;
        jami.pBins = 4;
        jami.pType = JGTConstants.DTDAY;
        jami.inStations = stationsFC;
        jami.inInterpolate = basinsFC;

        Id2ValueArrayWriter writer = new Id2ValueArrayWriter();
        writer.file = outputFile.getAbsolutePath();
        writer.pSeparator = " ";
        writer.fileNovalue = "-9999.0";

        DateTimeFormatter dF = JGTConstants.utcDateFormatterYYYYMMDDHHMM;
        // 1 5 2005 - 1 6 2005
        // 30 min
        while( dataReader.doProcess ) {
            dataReader.nextRecord();
            DateTime runningDate = dF.parseDateTime(dataReader.tCurrent);
            HashMap<Integer, double[]> id2ValueMap = dataReader.data;
            jami.inMeteo = id2ValueMap;
            jami.tCurrent = runningDate.toString(dF);

            jami.process();

            HashMap<Integer, double[]> interpolationPointId2MeteoDataMap = jami.outInterpolated;

            writer.data = interpolationPointId2MeteoDataMap;
            writer.writeNextLine();

            runningDate = runningDate.plusMinutes(30);
        }

        dataReader.close();
        writer.close();

        // for now there is no writer... waiting for the apposite module

        // double[] result1221 = new double[]{9.925938910342381, 8.1, 7.561630032493953,
        // 5.86663426553045, 3.4220912542795046};
        //
        // for( int i = 0; i < result1221.length; i++ ) {
        // assertEquals(result1221[i], values1221[i], 0.0001);
        // }

    }
}
