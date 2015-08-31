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

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

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

@Description("A module to shift las files by a given distance (given in units of the data crs).")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, move")
@Label(JGTConstants.LESTO + "/utilities")
@Name("lasmover")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasMover extends JGTModel {
    @Description("The las file to move.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description("The easting translation to apply.")
    @In
    public double pDeltaX = 0;

    @Description("The northing translation to apply.")
    @In
    public double pDeltaY = 0;

    @Description("The elevation translation to apply.")
    @In
    public double pDeltaZ = 0;

    @Description("The moved las file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        File inLas = new File(inFile);
        try (ALasReader reader = ALasReader.getReader(inLas, null)) {
            reader.open();
            ILasHeader header = reader.getHeader();
            long recordsNum = header.getRecordsCount();
            double[] xyzScale = header.getXYZScale();
            double[] xyzOffset = header.getXYZOffset();
            ReferencedEnvelope3D env = header.getDataEnvelope();

            File outLas = new File(outFile);
            try (ALasWriter writer = ALasWriter.getWriter(outLas, env.getCoordinateReferenceSystem())) {

                writer.setOffset(xyzOffset[0], xyzOffset[1], xyzOffset[2]);
                writer.setScales(xyzScale[0], xyzScale[1], xyzScale[2]);
                writer.setBounds(env.getMinX() + pDeltaX, env.getMaxX() + pDeltaX, env.getMinY() + pDeltaY,
                        env.getMaxY() + pDeltaY, env.getMinZ() + pDeltaZ, env.getMaxZ() + pDeltaZ);
                writer.open();

                pm.beginTask("Moving las...", (int) recordsNum);
                while( reader.hasNextPoint() ) {
                    LasRecord dot = reader.getNextPoint();
                    dot.x += pDeltaX;
                    dot.y += pDeltaY;
                    dot.z += pDeltaZ;
                    writer.addPoint(dot);
                    pm.worked(1);
                }
                pm.done();
            }
        }
    }

}
