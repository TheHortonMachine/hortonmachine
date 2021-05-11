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
package org.hortonmachine.modules;
import java.io.File;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.modules.docker.PdalDockerModel;

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

@Description("PdalInfo command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("pdal, docker")
@Label(HMConstants.PDAL)
@Name("_pdalinfo")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class PdalInfo extends PdalDockerModel {
    @Description("The pdal file to check.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    @Description("Print metadata.")
    @In
    public boolean doMetadata = true;

    @Description("Print schema.")
    @In
    public boolean doSchema = false;

    @Description("Print statistics on all points (reads the dataset).")
    @In
    public boolean doStats = false;

    @Description("A range of points information to print out.")
    @In
    public String pPointsRange = "1-3";

    @Execute
    public void process() throws Exception {
        checkFileExists(inPath);
        String error = checkDockerInstall();
        if (error == null) {
            try {
                File file = new File(inPath);
                String workspace = file.getParentFile().getAbsolutePath();
                String cmd = "pdal info " + file.getName();

                if (doMetadata) {
                    cmd += " --metadata";
                }
                if (doSchema) {
                    cmd += " --schema";
                }
                if (doStats) {
                    cmd += " --stats";
                }

                if (pPointsRange.trim().length() > 0) {
                    cmd += " -p " + pPointsRange;
                }
                pm.message(cmd);
                startContainer(workspace);
                execCommand(cmd);
            } finally {
                closeClient();
            }
        } else {
            pm.errorMessage(error);
        }
    }

    public static void main( String[] args ) throws Exception {
        PdalInfo i = new PdalInfo();
        i.inPath = "/Users/hydrologis/data/las/EXAMPLE_river.las";
        i.doMetadata = true;
        i.doSchema = true;
        i.pPointsRange = "1-2";
        i.process();
    }
}
