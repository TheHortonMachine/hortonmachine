/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.lesto.modules.flightlines;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.GpsTimeConverter;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.chart.CategoryHistogram;
import org.hortonmachine.gears.utils.chart.PlotFrame;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.joda.time.DateTime;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("A module that splits las files in its flightlines")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, split, flightlines")
@Label(HMConstants.LESTO + "/flightlines")
@Name("lasflightlines")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class FlightLinesExtractor extends HMModel {
    public static final String WEEK_SECONDS_TIME = "Week.seconds time";
    public static final String ADJUSTED_STANDARD_GPS_TIME = "Adjusted Standard GPS Time";

    @Description("A las file to split.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas;

    @Description("The gps time type.")
    @UI("combo:" + ADJUSTED_STANDARD_GPS_TIME + "," + WEEK_SECONDS_TIME)
    @In
    public String pGpsTimeType = ADJUSTED_STANDARD_GPS_TIME;

    @Description("Plot time markers.")
    @In
    public boolean doPlot = true;

    @Description("Output folder.")
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @In
    public String outFolder;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, outFolder);
        checkFileExists(inLas, outFolder);

        int timeType = 1;
        if (pGpsTimeType.equals(ADJUSTED_STANDARD_GPS_TIME)) {
            timeType = 1;
        }
        if (pGpsTimeType.equals(WEEK_SECONDS_TIME)) {
            timeType = 0;
        }

        File lasFile = new File(inLas);
        ALasReader reader = ALasReader.getReader(lasFile, null);
        reader.open();
        reader.setOverrideGpsTimeType(timeType);
        ILasHeader header = reader.getHeader();
        int gpsTimeType = header.getGpsTimeType();
        pm.message(header.toString());

        CoordinateReferenceSystem crs = header.getCrs();
        if (crs == null) {
            throw new ModelsIllegalargumentException("No crs available for the input data.", this);
        }

        pm.beginTask("Creating histogram...", (int) header.getRecordsCount());
        TreeMap<Long, Integer> histogramMap = new TreeMap<Long, Integer>();
        while( reader.hasNextPoint() ) {
            LasRecord readNextLasDot = reader.getNextPoint();
            long dateSeconds = getDateSeconds(gpsTimeType, readNextLasDot);
            Integer count = histogramMap.get(dateSeconds);
            if (count == null) {
                histogramMap.put(dateSeconds, 0);
            } else {
                histogramMap.put(dateSeconds, count + 1);
            }
            pm.worked(1);
        }
        reader.close();
        pm.done();

        Set<Entry<Long, Integer>> entrySet = histogramMap.entrySet();
        pm.beginTask("Defining time markers...", entrySet.size() * 2);
        long[] cat = new long[entrySet.size()];
        double[] values = new double[entrySet.size()];
        int i = 0;
        for( Entry<Long, Integer> entry : entrySet ) {
            cat[i] = entry.getKey();
            values[i] = entry.getValue();
            // System.out.println(cat[i] + " - " + values[i]);
            i++;
            pm.worked(1);
        }

        // find the time markers that split the flight lines
        TreeSet<Long> lineMarkers = new TreeSet<Long>();
        // lineMarkers.add(UtcTimeUtilities.fromStringWithSeconds(cat[0]).getMillis());
        long lastSeconds = 0;
        for( int j = 1; j < cat.length; j++ ) {
            long prevSeconds = cat[j - 1];
            long currentSeconds = cat[j];
            lastSeconds = currentSeconds;

            long seconds = currentSeconds - prevSeconds;
            if (seconds > 30) { // flightline split
                lineMarkers.add(prevSeconds);
                pm.message("Adding time marker at: " + prevSeconds);
            }
            pm.worked(1);
        }
        pm.done();
        // add last value
        lineMarkers.add(lastSeconds);

        long[] markersArray = new long[lineMarkers.size()];
        int index = 0;
        for( Long marker : lineMarkers ) {
            markersArray[index] = marker;
            index++;
        }

        File outFolderFile = new File(outFolder);
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(lasFile);

        pm.beginTask("Splitting flightlines...", (int) header.getRecordsCount());
        // now read them all and split them into files following the markers
        ALasWriter[] writers = new ALasWriter[100];
        reader = ALasReader.getReader(lasFile, crs);
        reader.setOverrideGpsTimeType(timeType);
        ILasHeader header2 = reader.getHeader();
        while( reader.hasNextPoint() ) {
            LasRecord readNextLasDot = reader.getNextPoint();
            long dateSeconds = getDateSeconds(gpsTimeType, readNextLasDot);

            for( int j = 0; j < markersArray.length; j++ ) {
                if (dateSeconds <= markersArray[j]) {
                    if (writers[j] == null) {
                        File file = new File(outFolderFile, nameWithoutExtention + "_" + j + ".las");
                        writers[j] = ALasWriter.getWriter(file, crs);
                        writers[j].setBounds(header2);
                        writers[j].open();
                    }
                    writers[j].addPoint(readNextLasDot);
                    break;
                }
            }
            pm.worked(1);
        }
        reader.close();
        pm.done();

        for( int j = 0; j < writers.length; j++ ) {
            ALasWriter writer = writers[j];
            if (writer != null)
                writer.close();
        }

        if (doPlot) {
            String[] catStr = new String[cat.length];
            for( int j = 0; j < cat.length; j++ ) {
                catStr[j] = String.valueOf(cat[j]);
            }
            CategoryHistogram hi = new CategoryHistogram(catStr, values);

            PlotFrame frame = new PlotFrame(hi);
            frame.plot();
        }
    }

    private long getDateSeconds( int gpsTimeType, LasRecord readNextLasDot ) {
        long dateSeconds;
        if (gpsTimeType == 0) {
            DateTime dt = GpsTimeConverter.gpsWeekTime2DateTime(readNextLasDot.gpsTime);
            dateSeconds = dt.getMillis() / 1000;
        } else {
            DateTime gpsTimeToDateTime = LasUtils.adjustedStandardGpsTime2DateTime(readNextLasDot.gpsTime);
            dateSeconds = gpsTimeToDateTime.getMillis() / 1000;
        }
        return dateSeconds;
    }

    public static void main( String[] args ) throws Exception {

        Files.list(Paths.get("/media/hydrologis/LATEMAR/lavori_tmp/2016_10_geologico/test_flightlines/57_Class_LAS/"))
                .filter(p -> p.getFileName().toString().endsWith(".las")).forEach(path -> {
                    FlightLinesExtractor eh = new FlightLinesExtractor();
                    eh.inLas = path.toString();
                    eh.doPlot = false;
                    eh.pGpsTimeType = WEEK_SECONDS_TIME;
                    eh.outFolder = "/media/hydrologis/LATEMAR/lavori_tmp/2016_10_geologico/test_flightlines/57_Class_LAS/flightlines";
                    try {
                        eh.process();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

    }

}
