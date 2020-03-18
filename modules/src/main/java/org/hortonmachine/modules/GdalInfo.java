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
import org.hortonmachine.modules.docker.GdalDockerModel;

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

@Description("GdalInfo command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("gdal, docker")
@Label(HMConstants.GDAL)
@Name("_gdalinfo")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GdalInfo extends GdalDockerModel {
    @Description("The gdal file to check.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    @Description("Show the supported formats and exit.")
    @In
    public boolean doShowFormats = false;

    @Execute
    public void process() throws Exception {
        String error = checkDockerInstall();
        if (error == null) {
            try {
                if (doShowFormats || inPath == null) {
                    String cmd = "gdalinfo --formats";
                    startContainer(null);
                    execCommand(cmd);
                } else {
                    checkFileExists(inPath);
                    File file = new File(inPath);
                    String workspace = file.getParentFile().getAbsolutePath();
                    String cmd = "gdalinfo " + file.getName();
                    startContainer(workspace);
                    execCommand(cmd);
                }
            } finally {
                closeClient();
            }
        } else {
            pm.errorMessage(error);
        }
    }

    public static void main( String[] args ) throws Exception {
        GdalInfo i = new GdalInfo();
        i.inPath = "/Users/hydrologis/data/DTM_calvello/aspect.asc";
        i.doShowFormats = true;
        i.process();
    }
}
