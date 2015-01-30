/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.lesto.modules.flightlines;
import java.io.File;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.chart.CategoryHistogram;
import org.jgrasstools.gears.utils.chart.PlotFrame;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.time.UtcTimeUtilities;
import org.joda.time.DateTime;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("A module that splits las files in its flightlines")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, split, flightlines")
@Label(JGTConstants.LESTO + "/flightlines")
@Name("lasflightlines")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class FlightLinesExtractor extends JGTModel {
    public static final String WEEK_SECONDS_TIME = "Week.seconds time";
    public static final String ADJUSTED_STANDARD_GPS_TIME = "Adjusted Standard GPS Time";

    @Description("A las file to split.")
    @UI(JGTConstants.FILEIN_UI_HINT)
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
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
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
        TreeMap<String, Integer> histogramMap = new TreeMap<String, Integer>();
        while( reader.hasNextPoint() ) {
            LasRecord readNextLasDot = reader.getNextPoint();
            DateTime gpsTimeToDateTime = LasUtils.gpsTimeToDateTime(readNextLasDot.gpsTime, gpsTimeType);
            String dateSeconds = UtcTimeUtilities.toStringWithSeconds(gpsTimeToDateTime);
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

        Set<Entry<String, Integer>> entrySet = histogramMap.entrySet();
        pm.beginTask("Defining time markers...", entrySet.size() * 2);
        String[] cat = new String[entrySet.size()];
        double[] values = new double[entrySet.size()];
        int i = 0;
        for( Entry<String, Integer> entry : entrySet ) {
            cat[i] = entry.getKey();
            values[i] = entry.getValue();

            System.out.println(cat[i] + " - " + values[i]);
            i++;
            pm.worked(1);
        }

        // find the time markers that split the flight lines
        TreeSet<Long> lineMarkers = new TreeSet<Long>();
        // lineMarkers.add(UtcTimeUtilities.fromStringWithSeconds(cat[0]).getMillis());
        DateTime currentDate = null;
        for( int j = 1; j < cat.length; j++ ) {
            String prev = cat[j - 1];
            String current = cat[j];

            DateTime prevDate = UtcTimeUtilities.fromStringWithSeconds(prev);
            currentDate = UtcTimeUtilities.fromStringWithSeconds(current);

            long millis = currentDate.getMillis() - prevDate.getMillis();
            long seconds = millis / 1000;
            if (seconds > 30) { // flightline split
                lineMarkers.add(prevDate.getMillis());
                pm.message("Adding time marker at: " + prevDate);
            }
            pm.worked(1);
        }
        pm.done();
        // add last value
        lineMarkers.add(currentDate.getMillis());

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
        int gpsTimeType2 = header2.getGpsTimeType();
        while( reader.hasNextPoint() ) {
            LasRecord readNextLasDot = reader.getNextPoint();
            DateTime gpsTimeToDateTime = LasUtils.gpsTimeToDateTime(readNextLasDot.gpsTime, gpsTimeType2);

            // round to seconds
            long millis = (long) (gpsTimeToDateTime.getMillis() / 1000.0);
            millis = millis * 1000;

            for( int j = 0; j < markersArray.length; j++ ) {
                if (millis <= markersArray[j]) {
                    if (writers[j] == null) {
                        File file = new File(outFolderFile, nameWithoutExtention + "_" + j + ".las");
                        writers[j] = new LasWriter(file, crs);
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
            CategoryHistogram hi = new CategoryHistogram(cat, values);
            
            PlotFrame frame = new PlotFrame(hi);
            frame.plot();
        }
    }

}
