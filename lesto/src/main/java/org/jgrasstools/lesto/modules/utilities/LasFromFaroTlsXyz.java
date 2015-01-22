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
package org.jgrasstools.lesto.modules.utilities;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

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

import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Converts XYZ data exported from FARO TLS to las.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, tls, convert")
@Label(JGTConstants.LESTO + "/utilities")
@Name("lasfromfarotlsxyz")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasFromFaroTlsXyz extends JGTModel {
    @Description("The xyz file to convert.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description("The reference longitude in a metric projection.")
    @In
    public double pLongitude;

    @Description("The reference latitude in a metric projection.")
    @In
    public double pLatitude;

    @Description("The code defining the data coordinate reference system.")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The converted las output file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outLas;

    @Execute
    public void process() throws Exception {
        checkNull(inFile, outLas, pCode);
        CoordinateReferenceSystem crs = CRS.decode(pCode);

        List<LasRecord> readRecords = new ArrayList<LasRecord>();

        File inXyzFile = new File(inFile);

        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;

        /*
         * Lines are of type:
         * -7.41840000 0.39450000 1190.87950000 254 254 254
         * xDeltaMeters, yDeltaMeters, Elev, R, G, B 
         */
        pm.beginTask("Reading xyz and creating bounds...", IJGTProgressMonitor.UNKNOWN);
        try (BufferedReader xyzReader = new BufferedReader(new FileReader(inXyzFile))) {
            String line;
            while( (line = xyzReader.readLine()) != null ) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                String[] lineSplit = line.split("\\s+");

                double deltaX = Double.parseDouble(lineSplit[0]);
                double deltaY = Double.parseDouble(lineSplit[1]);
                double x = pLongitude + deltaX;
                double y = pLatitude + deltaY;

                double elev = Double.parseDouble(lineSplit[2]);
                int r = Integer.parseInt(lineSplit[3]);
                int g = Integer.parseInt(lineSplit[4]);
                int b = Integer.parseInt(lineSplit[5]);
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
                dot.gpsTime = 0;
                readRecords.add(dot);
            }
        }
        pm.done();

        File outFile = new File(outLas);
        try (ALasWriter writer = new LasWriter(outFile, crs)) {
            writer.setBounds(xMin, xMax, yMin, yMax, zMin, zMax);
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
        LasFromFaroTlsXyz x = new LasFromFaroTlsXyz();
        x.inFile = "D:/TMP/faro/capriana_nuvola_punti_progetto_lowres.xyz";
        x.pLongitude = 681269.8905;
        x.pLatitude = 5127117.2439;
        x.pCode = "EPSG:32632";
        x.outLas = "D:/TMP/faro/capriana_lowres.las";
        x.process();
    }
}