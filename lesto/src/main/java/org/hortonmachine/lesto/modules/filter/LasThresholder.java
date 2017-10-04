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

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
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

@Description("A module that applies threshold and filters on a value inside the las")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, threshold, filter")
@Label(HMConstants.LESTO + "/filter")
@Name("lasthreshold")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasThresholder extends HMModel {
    @Description("A las file to filter.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas;

    @Description("Lower threshold.")
    @In
    public Double pLower;

    @Description("Upper threshold.")
    @In
    public Double pUpper;

    @Description("The value to analyze.")
    @UI("combo:" + LasUtils.INTENSITY + "," + LasUtils.ELEVATION)
    @In
    public String pType = LasUtils.INTENSITY;

    @Description("Output las file.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLas;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, outLas);

        boolean doIntensity = false;
        if (pType.equals(LasUtils.INTENSITY)) {
            doIntensity = true;
        }

        CoordinateReferenceSystem crs = null;
        File lasFile = new File(inLas);
        File outFile = new File(outLas);

        try (ALasReader reader = ALasReader.getReader(lasFile, crs);//
                ALasWriter writer = ALasWriter.getWriter(outFile, crs);) {
            reader.open();
            ILasHeader header = reader.getHeader();
            long recordsNum = header.getRecordsCount();

            writer.setBounds(header);
            writer.open();

            pm.beginTask("Filtering", (int) recordsNum);
            double min = Double.NEGATIVE_INFINITY;
            if (pLower != null) {
                min = pLower;
            }
            double max = Double.POSITIVE_INFINITY;
            if (pUpper != null) {
                max = pUpper;
            }
            while( reader.hasNextPoint() ) {
                LasRecord readNextLasDot = reader.getNextPoint();
                double value = readNextLasDot.z;
                if (doIntensity) {
                    value = readNextLasDot.intensity;
                }
                if (value < min) {
                    pm.worked(1);
                    continue;
                }
                if (value > max) {
                    pm.worked(1);
                    continue;
                }
                writer.addPoint(readNextLasDot);
                pm.worked(1);
            }
        }
        pm.done();
    }

}
