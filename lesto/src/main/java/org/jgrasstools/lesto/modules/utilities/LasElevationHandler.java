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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

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

@Description("A module that allows to normalize the las over an elevation model (or vice versa).")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, dem")
@Label(JGTConstants.LESTO + "/utilities")
@Name("laselevationhandler")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasElevationHandler extends JGTModel {
    @Description("The las file to handle.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description("A dtm raster to use for the ellevation normlization.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDtm;

    @Description("If set to true, the dtm value is added to the las elevation, instead of subtracted.")
    @In
    public boolean doAdd = false;

    @Description("The nomalized las file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    @Execute
    public void process() throws Exception {
        checkNull(inFile, inDtm);

        GridCoverage2D dtm = getRaster(inDtm);

        File inLas = new File(inFile);
        try (ALasReader reader = ALasReader.getReader(inLas, null)) {
            reader.open();
            ILasHeader header = reader.getHeader();
            long recordsNum = header.getRecordsCount();
            ReferencedEnvelope3D env = header.getDataEnvelope();

            File outLas = new File(outFile);
            try (ALasWriter writer = ALasWriter.getWriter(outLas, env.getCoordinateReferenceSystem())) {
                writer.setBounds(header);
                writer.open();

                pm.beginTask("Normalizing las...", (int) recordsNum);
                while( reader.hasNextPoint() ) {
                    LasRecord dot = reader.getNextPoint();
                    double dtmValue = CoverageUtilities.getValue(dtm, dot.x, dot.y);
                    if (JGTConstants.isNovalue(dtmValue)) {
                        dtmValue = 0;
                    }
                    if (doAdd) {
                        dot.z += dtmValue;
                    } else {
                        dot.z -= dtmValue;
                    }
                    writer.addPoint(dot);
                    pm.worked(1);
                }
                pm.done();
            }
        }
    }

    public static void main( String[] args ) throws Exception {

        Files.list(Paths.get("/home/hydrologis/TMP/geologico/iotest/")).filter(p -> p.getFileName().toString().endsWith(".las"))
                .forEach(path -> {
                    LasElevationHandler eh = new LasElevationHandler();
                    eh.inFile = path.toString();
                    eh.inDtm = "/home/hydrologis/TMP/geologico/iotest/DTMclip.tif";
                    eh.outFile = path.toString() + "_norm.las";
                    try {
                        eh.process();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

    }

}
