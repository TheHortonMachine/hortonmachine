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
package org.hortonmachine.lesto.modules.filter;
import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.chart.CategoryHistogram;
import org.hortonmachine.gears.utils.chart.PlotFrame;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

@Description("Creates the histogram of the intensity, elevation or classification of the points of a las file.")
@Documentation(LasHistogram.DOCUMENTATION)
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, histogram ")
@Label(HMConstants.LESTO + "/filter")
@Name("lashistogram")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasHistogram extends HMModel {
    @Description("The las file to analyze.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas;

    @Description("The number of bins, of equal width between the minimum and the maximum of the value.")
    @In
    public int pBin = 100;

    @Description("If true, the histogram is shown in a chart window.")
    @In
    public boolean doPlot = true;

    @Description("The value of the points to analyze.")
    @UI("combo:" + LasUtils.INTENSITY + "," + LasUtils.ELEVATION + "," + LasUtils.CLASSIFICATION)
    @In
    public String pType = LasUtils.INTENSITY;

    @Description("The optional output csv file of the histogram: the center of each bin and its count of points.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outCsv;

    public static final String DOCUMENTATION = """
            The file is read twice: first for the range of the value, then to count the points in each bin. The bins and their counts are printed, shown in a chart window and, if the output file is given, written to a csv file with the center of each bin and its count of points.

            The histogram of the intensity helps to choose the thresholds of LasThresholder, the one of the elevation shows the vertical structure of the survey, as the layers of a forest. The classification has only a few values, as 1 unclassified, 2 ground and 3 low vegetation: use as many bins as the classes.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        boolean doIntensity = false;
        boolean doClassification = false;
        DecimalFormat formatter = new DecimalFormat("0.0");
        if (pType.equals(LasUtils.INTENSITY)) {
            doIntensity = true;
            formatter = new DecimalFormat("0");
        }
        if (pType.equals(LasUtils.CLASSIFICATION)) {
            doClassification = true;
            formatter = new DecimalFormat("0");
        }

        CoordinateReferenceSystem crs = null;
        File lasFile = new File(inLas);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int recordsCount = 0;
        try (ALasReader reader = ALasReader.getReader(lasFile, crs)) {
            reader.open();
            ILasHeader header = reader.getHeader();
            recordsCount = (int) header.getRecordsCount();
            pm.beginTask(MessageFormat.format("Calculating range of {0} points...", recordsCount), recordsCount);
            while( reader.hasNextPoint() ) {
                LasRecord readNextLasDot = reader.getNextPoint();
                double value = readNextLasDot.z;
                if (doIntensity) {
                    value = readNextLasDot.intensity;
                }
                if (doClassification) {
                    value = readNextLasDot.classification;
                }
                min = Math.min(min, value);
                max = Math.max(max, value);
                pm.worked(1);
            }
        }
        pm.done();

        pm.message("Max: " + max);
        pm.message("Min: " + min);
        if (!Double.isFinite(max) || !Double.isFinite(min)) {
            pm.errorMessage("A problem occurred while reading the data, exiting...");
            return;
        }

        double range = max - min;
        double step = range / pBin;
        double[] count = new double[pBin];
        double[] markers = new double[pBin];
        for( int i = 0; i < markers.length; i++ ) {
            markers[i] = min + step * (i + 1);
        }

        pm.beginTask("Creating histogram...", recordsCount);
        // now read them all and split them into files following the markers
        try (ALasReader reader = ALasReader.getReader(lasFile, crs)) {
            reader.open();
            while( reader.hasNextPoint() ) {
                LasRecord readNextLasDot = reader.getNextPoint();
                double value = readNextLasDot.z;
                if (doIntensity) {
                    value = readNextLasDot.intensity;
                }
                if (doClassification) {
                    value = readNextLasDot.classification;
                }
                // the maximum can be over the last marker by a rounding error: it goes in the last bin
                int bin = pBin - 1;
                for( int j = 0; j < markers.length; j++ ) {
                    if (value <= markers[j]) {
                        bin = j;
                        break;
                    }
                }
                count[bin] = count[bin] + 1;
                pm.worked(1);
            }
        }
        pm.done();

        pm.message("value, \tcount");
        String[] markersLabels = new String[pBin];
        StringBuilder csv = new StringBuilder(pType + ",count\n");
        for( int i = 0; i < markersLabels.length; i++ ) {
            double center = markers[i] - step / 2.0;
            markersLabels[i] = formatter.format(center);
            pm.message(markersLabels[i] + ",\t" + count[i]);
            csv.append(center).append(",").append((long) count[i]).append("\n");
        }
        if (outCsv != null) {
            FileUtilities.writeFile(csv.toString(), new File(outCsv));
        }

        if (doPlot) {
            CategoryHistogram hi = new CategoryHistogram(markersLabels, count);
            PlotFrame frame = new PlotFrame(hi);
            frame.plot();
        }
    }

}
