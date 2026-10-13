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
package org.hortonmachine.lesto.modules.utilities;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

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
import oms3.annotations.Unit;

@Description("Converts the scan points of a FARO terrestrial laser scanner, exported as xyz text by the SCENE software, to a las file.")
@Documentation(LasFromFaroTlsXyz.DOCUMENTATION)
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, tls, convert")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasfromfarotlsxyz")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasFromFaroTlsXyz extends HMModel {
    @Description("The xyz file to convert, with the columns row, column, x and y from the scanner, elevation, red, green and blue.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inFile;

    @Description("The easting of the scanner in the projected reference system of the output, added to the x of the points.")
    @In
    public Double pLongitude;

    @Description("The northing of the scanner in the projected reference system of the output, added to the y of the points.")
    @In
    public Double pLatitude;

    @Description("The maximum horizontal distance from the scanner: the farther points are left out.")
    @Unit("m")
    @In
    public Double pMaxDistance;

    @Description("The code defining the data coordinate reference system.")
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The converted las output file.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLas;

    public static final String DOCUMENTATION = """
            The input is the export of the scan points of SCENE, a text file with one point per line and eight columns separated by spaces: the row and column of the point in the scan, its x and y from the scanner in meters, its elevation, and its red, green and blue color from 0 to 255. Any other layout stops the conversion.

            The x and y are relative to the scanner, so they are moved by the easting and northing given for it, in the projected reference system of the output. The elevation is kept as it is, so the scan must be referenced to the right elevation already. With the maximum distance, the points farther than it from the scanner on the plane are left out, as the far points of a scan are sparse and noisy.

            The las file has the point format 3, with the colors of the scan. The scan has no returns, classes, intensities or times: the points are written as single returns, never classified (class 0), with intensity 0 and time 0.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inFile, outLas, pCode, pLatitude, pLongitude);
        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs(pCode, true);

        double latitude = pLatitude;
        double longitude = pLongitude;

        List<LasRecord> readRecords = new ArrayList<LasRecord>();

        File inXyzFile = new File(inFile);

        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;

        double maxDistance = Double.POSITIVE_INFINITY;
        if (pMaxDistance != null) {
            maxDistance = pMaxDistance;
        }

        /*
         * Lines of the export of the scan points, of type:
         * rownum, colnum, xDeltaMeters, yDeltaMeters, Elev, R, G, B
         */
        int ignoredCount = 0;
        pm.beginTask("Reading xyz and creating bounds...", IHMProgressMonitor.UNKNOWN);
        try (BufferedReader xyzReader = new BufferedReader(new FileReader(inXyzFile))) {
            String line;
            int count = 0;
            while( (line = xyzReader.readLine()) != null ) {
                if (count++ % 1000000 == 0) {
                    pm.message("processed lines: " + (count - 1) + " of which ignored: " + ignoredCount);
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                String[] lineSplit = line.split("\\s+");

                if (lineSplit.length != 8) {
                    String msg = "Wrong data format. The data are supposed to be exported from the SCENE software as scan points.\nThe data format in the XYZ file is: rowNum, colNum, xDeltaMeters, yDeltaMeters, elev, R, G, B";
                    throw new ModelsIllegalargumentException(msg, this);
                }

                int index = 2;
                double deltaX = Double.parseDouble(lineSplit[index++]);
                double deltaY = Double.parseDouble(lineSplit[index++]);

                double distanceFromCenter = NumericsUtilities.pythagoras(deltaX, deltaY);
                if (distanceFromCenter > maxDistance) {
                    ignoredCount++;
                    continue;
                }

                double x = longitude + deltaX;
                double y = latitude + deltaY;

                double elev = Double.parseDouble(lineSplit[index++]);
                int r = Integer.parseInt(lineSplit[index++]);
                int g = Integer.parseInt(lineSplit[index++]);
                int b = Integer.parseInt(lineSplit[index++]);
                xMin = min(xMin, x);
                yMin = min(yMin, y);
                zMin = min(zMin, elev);
                xMax = max(xMax, x);
                yMax = max(yMax, y);
                zMax = max(zMax, elev);

                LasRecord dot = new LasRecord();
                dot.x = x;
                dot.y = y;
                dot.z = elev;
                dot.color = new short[]{(short) r, (short) g, (short) b};
                // single returns, never classified and with no intensity
                dot.returnNumber = 1;
                dot.numberOfReturns = 1;
                dot.classification = 0;
                dot.intensity = 0;
                // the scan points have no acquisition time
                dot.gpsTime = 0;
                readRecords.add(dot);
            }
        }
        pm.done();

        pm.message("Keeping point: " + readRecords.size());

        File outFile = new File(outLas);
        try (ALasWriter writer = ALasWriter.getWriter(outFile, crs)) {
            writer.setBounds(xMin, xMax, yMin, yMax, zMin, zMax);
            writer.setPointFormat(3);
            writer.open();
            pm.beginTask("Writing las...", readRecords.size());
            for( LasRecord dot : readRecords ) {
                writer.addPoint(dot);
                pm.worked(1);
            }
            pm.done();
        }
    }
}