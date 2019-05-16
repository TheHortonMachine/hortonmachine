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
import org.hortonmachine.gears.io.las.index.LasIndexer;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
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
import oms3.annotations.Unit;

@Description("Converts XYZ data exported from FARO TLS to las.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, tls, convert")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasfromfarotlsxyz")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasFromFaroTlsXyz extends HMModel {
    @Description("The xyz file to convert.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inFile;

    @Description("The reference longitude in a metric projection.")
    @In
    public Double pLongitude;

    @Description("The reference latitude in a metric projection.")
    @In
    public Double pLatitude;

    @Description("The maximum allowed distance from the center.")
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

    @Execute
    public void process() throws Exception {
        checkNull(inFile, outLas, pCode, pLatitude, pLongitude);
        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg(pCode);

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
         * Lines are of type:
         * -7.41840000 0.39450000 1190.87950000 254 254 254
         * rownum, colnum, xDeltaMeters, yDeltaMeters, Elev, R, G, B 
         * 
         * Export scan points
         * 
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
                dot.gpsTime = System.currentTimeMillis();
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

    public static void main( String[] args ) throws Exception {
        // String inFile = "/home/hydrologis/data/rilievo_tls/capriana_punti_scansione_avgres.xyz";
        // double lon = 681274.363;
        // double lat = 5127118.3962;
        // String outFile = "/home/hydrologis/data/rilievo_tls/capriana_avgres.las";
        // double maxDistance = 15;

        String inFile = "/home/hydrologis/data/rilievo_tls/capriana_punti_scansione_lowres.xyz";
        double lon = 681269.8905;
        double lat = 5127117.2439;
        String outFolder = "/home/hydrologis/data/rilievo_tls/lowres/";
        String outFile = outFolder + "capriana_lowres.las";
        double maxDistance = 16;

        LasFromFaroTlsXyz x = new LasFromFaroTlsXyz();
        x.inFile = inFile;
        x.pLongitude = lon;
        x.pLatitude = lat;
        x.pMaxDistance = maxDistance;
        x.pCode = "EPSG:32632";
        x.outLas = outFile;
        x.process();

        LasIndexer in = new LasIndexer();
        in.inFolder = outFolder;
        in.pCellsize = 0.5;
        in.process();
    }
}