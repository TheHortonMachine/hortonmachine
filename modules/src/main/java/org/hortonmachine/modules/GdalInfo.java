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
import static org.hortonmachine.gears.libs.modules.HMConstants.DOCKER;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.processes.CommandExecutor;
import org.hortonmachine.gears.utils.processes.SystemoutProcessListener;
import org.hortonmachine.modules.docker.GdalDockerModel;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ExecCreation;

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
@Label("Gdal")
@Name("_gdalinfo")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GdalInfo extends GdalDockerModel {
    @Description("The workspace inside which the processed files reside.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inWorkspace = null;

    @Description("The gdal file to check, relative to the workspace folder.")
    @In
    public String inPath = null;

    @Description("Show the supported formats and exit.")
    @In
    public boolean pShowFormats = false;

    @Execute
    public void process() throws Exception {
        try {
            String id = startContainer();

            String cmd = "gdalinfo " + inPath;
            if(pShowFormats) {
                cmd = "gdalinfo --formats";
            }
            execCommand(id, cmd);

        } finally {
            closeClient();
        }
    }
}
