package org.jgrasstools.hortonmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.eicalculator.EIAltimetry;
import org.jgrasstools.gears.io.eicalculator.EIAltimetryReader;
import org.jgrasstools.gears.io.eicalculator.EIAreas;
import org.jgrasstools.gears.io.eicalculator.EIAreasReader;
import org.jgrasstools.gears.io.generic.Id2ValueArrayWriter;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.statistics.jami.Jami;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
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

        URL areasUrl = this.getClass().getClassLoader().getResource("eicalculator_out_areas.csv");
        File areasFile = new File(areasUrl.toURI());

        File outputFileForEtp = new File(altimetryFile.getParentFile(), "etp_in_temp.csv");
        outputFileForEtp = classesTestFile2srcTestResourcesFile(outputFileForEtp);

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

        EIAreasReader areas = new EIAreasReader();
        areas.file = areasFile.getAbsolutePath();
        areas.pSeparator = "\\s+";
        areas.pm = pm;
        areas.read();
        List<EIAreas> areasList = areas.outAreas;
        areas.close();

        ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
        stationsReader.file = new File(stationsUrl.toURI()).getAbsolutePath();
        stationsReader.readFeatureCollection();
        SimpleFeatureCollection stationsFC = stationsReader.geodata;

        ShapefileFeatureReader basinsReader = new ShapefileFeatureReader();
        basinsReader.file = new File(basinsUrl.toURI()).getAbsolutePath();
        basinsReader.readFeatureCollection();
        SimpleFeatureCollection basinsFC = basinsReader.geodata;

        TimeSeriesIteratorReader dataReader = new TimeSeriesIteratorReader();
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
        jami.inAreas = areasList;
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

        TimeSeriesIteratorWriter tsWriter = new TimeSeriesIteratorWriter();
        tsWriter.file = outputFileForEtp.getAbsolutePath();
        tsWriter.tStart = dataReader.tStart;
        tsWriter.tTimestep = dataReader.tTimestep;

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

            HashMap<Integer, double[]> interpolationPointId2MeteoDataMapBands = jami.outInterpolatedBand;
            HashMap<Integer, double[]> interpolationPointId2MeteoDataMap = jami.outInterpolated;

            // Set<Entry<Integer, double[]>> entrySet =
            // interpolationPointId2MeteoDataMapBands.entrySet();
            // for( Entry<Integer, double[]> entry : entrySet ) {
            // Integer basinId = entry.getKey();
            // double[] valuePerBand = entry.getValue();
            // double value = interpolationPointId2MeteoDataMap.get(basinId)[0];
            // System.out.println("basin: " + basinId);
            // System.out.print("per band: ");
            // for( double bandValue : valuePerBand ) {
            // System.out.print(" " + bandValue);
            // }
            // System.out.println("per band: ");
            // System.out.println("interpolated on basin: " + value);
            // System.out.println("****************************");
            // }

            writer.data = interpolationPointId2MeteoDataMapBands;
            writer.writeNextLine();

            tsWriter.inData = interpolationPointId2MeteoDataMap;
            tsWriter.writeNextLine();

            runningDate = runningDate.plusMinutes(30);
        }

        dataReader.close();
        writer.close();
        tsWriter.close();

        // for now there is no writer... waiting for the apposite module

        // double[] result1221 = new double[]{9.925938910342381, 8.1, 7.561630032493953,
        // 5.86663426553045, 3.4220912542795046};
        //
        // for( int i = 0; i < result1221.length; i++ ) {
        // assertEquals(result1221[i], values1221[i], 0.0001);
        // }

    }
}
