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
package org.jgrasstools.lesto.modules.filter;
import java.io.File;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("A module that normalizes a value inside the las")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, normalize")
@Label(JGTConstants.LAS + "/filter")
@Name("lasnorm")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasIntensityNormalizer extends JGTModel {
    @Description("A las file to normalize.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas;

    @Description("Normalization factor.")
    @In
    public double pFactor = 1;

    @Description("Output las file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outLas;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, outLas);

        CoordinateReferenceSystem crs = null;
        File lasFile = new File(inLas);
        ALasReader reader = ALasReader.getReader(lasFile, crs);
        reader.open();
        ILasHeader header = reader.getHeader();
        long recordsNum = header.getRecordsCount();

        File outFile = new File(outLas);
        ALasWriter writer = new LasWriter(outFile, crs);
        pm.beginTask("Normalizing...", (int) recordsNum);
        reader = ALasReader.getReader(lasFile, crs);
        reader.open();
        writer.open();
        while( reader.hasNextPoint() ) {
            LasRecord readNextLasDot = reader.getNextPoint();
            double newValue = readNextLasDot.intensity * pFactor;
            readNextLasDot.intensity = (short) newValue;
            writer.addPoint(readNextLasDot);
            pm.worked(1);
        }
        reader.close();
        writer.close();
        pm.done();
    }
}
