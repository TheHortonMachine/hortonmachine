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
import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

@Description("Normalizes the points of a las file over an elevation model, turning their elevations into heights over the ground, or the other way around.")
@Documentation(LasElevationHandler.DOCUMENTATION)
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, dem")
@Label(HMConstants.LESTO + "/utilities")
@Name("laselevationhandler")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasElevationHandler extends HMModel {
    @Description("The las file to handle.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inFile;

    @Description("The elevation model of the ground.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

    @Description("If set to true, the dtm value is added to the las elevation, instead of subtracted.")
    @In
    public boolean doAdd = false;

    @Description("The normalized las file.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    public static final String DOCUMENTATION = """
            The elevation of the cell of the elevation model that contains each point, without interpolation, is subtracted from the elevation of the point, which becomes its height over the ground, or added to it, to bring the heights of a normalized file back to elevations.

            The points outside of the elevation model, or on its cells without value, are left out, and their number is reported. All the other attributes of the points are kept; the header gets the bounds of the new values.

            A normalized file is the input of the modules that work on the heights over the ground without an elevation model, as a canopy height model interpolated from the points.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inFile, inDtm, outFile);

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

                int outsideCount = 0;
                pm.beginTask("Normalizing las...", (int) recordsNum);
                while( reader.hasNextPoint() ) {
                    pm.worked(1);
                    LasRecord dot = reader.getNextPoint();
                    double dtmValue = CoverageUtilities.getValue(dtm, dot.x, dot.y);
                    if (HMConstants.isNovalue(dtmValue)) {
                        // outside of the dtm or on its novalues: no elevation to use
                        outsideCount++;
                        continue;
                    }
                    if (doAdd) {
                        dot.z += dtmValue;
                    } else {
                        dot.z -= dtmValue;
                    }
                    writer.addPoint(dot);
                }
                pm.done();
                if (outsideCount > 0) {
                    pm.message("Points left out, with no dtm value: " + outsideCount);
                }
            }
        }
    }


}
